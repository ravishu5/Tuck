package com.tuck.app.data.local.db.dao

import androidx.sqlite.db.SimpleSQLiteQuery
import java.nio.ByteBuffer
import java.nio.ByteOrder
import androidx.sqlite.db.SupportSQLiteDatabase
import com.tuck.app.data.local.db.TuckDatabase
import com.tuck.app.data.local.db.entity.SavedItemFtsEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Raw-SQL implementation of the full-text index, with relevance ranking.
 *
 * Android's bundled SQLite has no `fts5` module (verified on device: `no such
 * module: fts5`), so BM25 is unavailable and Room only offers `@Fts4`. Previously
 * that meant "sort by relevance" returned rows in rowid order - i.e. oldest first.
 *
 * This owns the virtual table directly so it can read FTS4's `matchinfo` and score
 * matches by which column they landed in: a hit in the title outranks a hit in body
 * text or OCR output.
 */
@Singleton
class SavedItemFtsDaoImpl @Inject constructor(
    private val database: TuckDatabase
) : SavedItemFtsDao {

    companion object {
        const val TABLE = "saved_items_fts"

        /** Column order here defines the weight order in [COLUMN_WEIGHTS]. */
        const val CREATE_TABLE = """
            CREATE VIRTUAL TABLE IF NOT EXISTS saved_items_fts USING fts4(
                title, description, originalUrl, sourceDomain,
                originalText, extractedText, ocrText, tags, entities,
                tokenize=porter,
                prefix='2,3,4'
            )
        """

        /**
         * Keeps the index from stranding rows when an item is hard-deleted.
         * Inserts and updates are pushed explicitly by the writer, since the
         * indexed text is assembled from several tables.
         */
        const val CREATE_DELETE_TRIGGER = """
            CREATE TRIGGER IF NOT EXISTS saved_items_fts_delete
            AFTER DELETE ON saved_items BEGIN
                DELETE FROM saved_items_fts WHERE rowid = old.id;
            END
        """

        /** title, description, url, domain, originalText, extractedText, ocr, tags, entities */
        val COLUMN_WEIGHTS = doubleArrayOf(8.0, 3.0, 1.0, 1.5, 3.0, 2.0, 2.0, 5.0, 4.0)

        private const val COLUMNS =
            "title, description, originalUrl, sourceDomain, originalText, extractedText, ocrText, tags, entities"
    }

    private val writable: SupportSQLiteDatabase
        get() = database.openHelper.writableDatabase

    override suspend fun insertOrUpdate(ftsEntity: SavedItemFtsEntity) = withContext(Dispatchers.IO) {
        // Virtual tables have no UPSERT; delete-then-insert is the documented pattern.
        writable.execSQL("DELETE FROM $TABLE WHERE rowid = ?", arrayOf<Any?>(ftsEntity.rowid))
        writable.execSQL(
            "INSERT INTO $TABLE (rowid, $COLUMNS) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            arrayOf<Any?>(
                ftsEntity.rowid,
                ftsEntity.title,
                ftsEntity.description,
                ftsEntity.originalUrl,
                ftsEntity.sourceDomain,
                ftsEntity.originalText,
                ftsEntity.extractedText,
                ftsEntity.ocrText,
                ftsEntity.tags,
                ftsEntity.entities
            )
        )
    }

    override suspend fun delete(rowid: Long) = withContext(Dispatchers.IO) {
        writable.execSQL("DELETE FROM $TABLE WHERE rowid = ?", arrayOf<Any?>(rowid))
    }

    override suspend fun searchFtsWithSnippet(ftsQuery: String): List<FtsSearchResult> =
        withContext(Dispatchers.IO) {
            val sql = """
                SELECT rowid,
                       snippet($TABLE, '<b>', '</b>', '...', -1, 30) AS snippet,
                       matchinfo($TABLE, 'pcnalx') AS info
                FROM $TABLE
                WHERE $TABLE MATCH ?
                LIMIT 500
            """.trimIndent()

            val results = mutableListOf<FtsSearchResult>()
            database.query(SimpleSQLiteQuery(sql, arrayOf<Any?>(ftsQuery))).use { cursor ->
                while (cursor.moveToNext()) {
                    results.add(
                        FtsSearchResult(
                            rowid = cursor.getLong(0),
                            snippet = if (cursor.isNull(1)) null else cursor.getString(1),
                            rank = scoreFromMatchInfo(cursor.getBlob(2))
                        )
                    )
                }
            }
            // Higher score is a better match.
            results.sortedByDescending { it.rank }
        }

    /**
     * Scores a hit from FTS4's `matchinfo(tbl, 'pcnalx')` blob, which is a packed
     * array of 32-bit ints laid out as:
     *   p                    number of phrases
     *   c                    number of columns
     *   n                    number of rows in the table
     *   a[c]                 average token count per column
     *   l[c]                 token count per column for this row
     *   x[3 * p * c]         per phrase/column: hits here, hits overall, docs matched
     *
     * Each hit contributes its column's weight, damped by how long that column is,
     * so a match in a short title beats one buried in a page of OCR text.
     */
    private fun scoreFromMatchInfo(blob: ByteArray?): Double {
        if (blob == null || blob.size < 12) return 0.0
        val ints = ByteBuffer.wrap(blob).order(ByteOrder.nativeOrder()).asIntBuffer()

        val phraseCount = ints.get(0)
        val columnCount = ints.get(1)
        if (phraseCount <= 0 || columnCount <= 0) return 0.0

        val avgOffset = 3
        val lenOffset = avgOffset + columnCount
        val hitsOffset = lenOffset + columnCount

        var score = 0.0
        for (phrase in 0 until phraseCount) {
            for (column in 0 until columnCount) {
                val index = hitsOffset + 3 * (phrase * columnCount + column)
                if (index >= ints.limit()) continue

                val hitsInThisRow = ints.get(index)
                if (hitsInThisRow == 0) continue

                val avgLength = ints.get(avgOffset + column).coerceAtLeast(1)
                val thisLength = ints.get(lenOffset + column).coerceAtLeast(1)
                val lengthDamping = avgLength.toDouble() / (avgLength + thisLength).toDouble()

                val weight = COLUMN_WEIGHTS.getOrElse(column) { 1.0 }
                score += weight * hitsInThisRow * lengthDamping
            }
        }
        return score
    }

    override suspend fun searchFtsRowIds(ftsQuery: String): List<Long> =
        searchFtsWithSnippet(ftsQuery).map { it.rowid }

    override suspend fun allIndexedRowIds(): List<Long> = withContext(Dispatchers.IO) {
        val ids = mutableListOf<Long>()
        database.query(SimpleSQLiteQuery("SELECT rowid FROM $TABLE")).use { cursor ->
            while (cursor.moveToNext()) ids.add(cursor.getLong(0))
        }
        ids
    }

    override suspend fun rebuildIndex() = withContext(Dispatchers.IO) {
        writable.execSQL("DELETE FROM $TABLE")
        writable.execSQL(backfillSql())
    }
}

/**
 * Repopulates the index from `saved_items`, folding in tags and entity values so a
 * search can match on either. Shared by the migration and by [SavedItemFtsDaoImpl.rebuildIndex].
 */
fun backfillSql(): String = """
    INSERT INTO saved_items_fts (
        rowid, title, description, originalUrl, sourceDomain,
        originalText, extractedText, ocrText, tags, entities
    )
    SELECT
        i.id,
        COALESCE(i.title, ''),
        COALESCE(i.description, ''),
        COALESCE(i.originalUrl, ''),
        COALESCE(i.sourceDomain, ''),
        COALESCE(i.originalText, ''),
        COALESCE(i.extractedText, ''),
        COALESCE(i.ocrText, ''),
        COALESCE((
            SELECT GROUP_CONCAT(t.name, ' ')
            FROM saved_item_tags sit
            JOIN tags t ON t.id = sit.tagId
            WHERE sit.savedItemId = i.id
        ), ''),
        COALESCE((
            SELECT GROUP_CONCAT(e.value, ' ')
            FROM entities e
            WHERE e.savedItemId = i.id
        ), '')
    FROM saved_items i
    WHERE i.isDeleted = 0
""".trimIndent()
