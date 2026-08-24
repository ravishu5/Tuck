package com.tuck.app

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tuck.app.data.local.db.TuckDatabase
import com.tuck.app.data.local.db.dao.SavedItemFtsDaoImpl
import com.tuck.app.data.local.db.entity.SavedItemFtsEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises the raw FTS4 index against real SQLite. Relevance ranking is computed
 * from a `matchinfo` blob, which cannot be observed off-device at all.
 */
@RunWith(AndroidJUnit4::class)
class FtsIndexTest {

    private lateinit var db: TuckDatabase
    private lateinit var fts: SavedItemFtsDaoImpl

    private fun row(
        id: Long,
        title: String = "",
        ocr: String = "",
        body: String = "",
        tags: String = ""
    ) = SavedItemFtsEntity(
        rowid = id, title = title, description = "", originalUrl = "",
        sourceDomain = "", originalText = body, extractedText = "",
        ocrText = ocr, tags = tags, entities = ""
    )

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TuckDatabase::class.java
        ).build()
        db.openHelper.writableDatabase.execSQL(SavedItemFtsDaoImpl.CREATE_TABLE)
        fts = SavedItemFtsDaoImpl(db)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun rankingPutsTitleMatchesAboveBodyMatches() = runBlocking {
        fts.insertOrUpdate(row(1L, body = "a passing mention of kotlin somewhere in the body text"))
        fts.insertOrUpdate(row(2L, title = "Kotlin coroutines guide"))

        val results = fts.searchFtsWithSnippet("kotlin*")

        assertEquals(2, results.size)
        assertEquals("title weight must outrank body weight", 2L, results.first().rowid)
        assertTrue("higher score sorts first", results.first().rank >= results.last().rank)
    }

    @Test
    fun currencyAndPunctuationDoNotBreakQueries() = runBlocking {
        fts.insertOrUpdate(row(1L, ocr = "Nike Air Max \u20b98,999 Amazon"))
        fts.insertOrUpdate(row(2L, title = "unrelated note"))

        val outcomes = listOf("\u20b98*", "999*", "nike* air*", "Amazon*", "8*")
            .associateWith { query -> fts.searchFtsWithSnippet(query).map { it.rowid } }

        assertEquals(
            mapOf(
                // The currency symbol is part of the indexed token, not a separator.
                "\u20b98*" to listOf(1L),
                "999*" to listOf(1L),
                // What formatFtsQuery produces for "nike-air" once the hyphen is stripped.
                "nike* air*" to listOf(1L),
                "Amazon*" to listOf(1L),
                // Documented limitation: prefixes match from the start of a token, and
                // "\u20b98,999" indexes as [\u20b98, 999], so a bare "8" finds nothing.
                "8*" to emptyList<Long>()
            ),
            outcomes
        )
    }

    @Test
    fun prefixMatchingFindsPartialWords() = runBlocking {
        fts.insertOrUpdate(row(1L, title = "Photography techniques"))

        assertEquals(listOf(1L), fts.searchFtsWithSnippet("photog*").map { it.rowid })
        assertTrue(fts.searchFtsWithSnippet("zzz*").isEmpty())
    }

    @Test
    fun snippetHighlightsTheMatchedTerm() = runBlocking {
        fts.insertOrUpdate(row(1L, title = "Kotlin coroutines guide"))

        val snippet = fts.searchFtsWithSnippet("coroutines*").first().snippet
        assertTrue("snippet should mark the hit: $snippet", snippet!!.contains("<b>"))
    }

    @Test
    fun updateReplacesRatherThanDuplicating() = runBlocking {
        fts.insertOrUpdate(row(1L, title = "original title"))
        fts.insertOrUpdate(row(1L, title = "revised title"))

        assertTrue("stale text must not linger", fts.searchFtsWithSnippet("original*").isEmpty())
        assertEquals(listOf(1L), fts.searchFtsWithSnippet("revised*").map { it.rowid })
    }

    @Test
    fun deleteRemovesTheRowFromTheIndex() = runBlocking {
        fts.insertOrUpdate(row(1L, title = "disposable"))
        fts.delete(1L)

        assertTrue(fts.searchFtsWithSnippet("disposable*").isEmpty())
    }

    @Test
    fun tagsAreSearchableAndOutrankBodyText() = runBlocking {
        fts.insertOrUpdate(row(1L, body = "shopping came up in conversation"))
        fts.insertOrUpdate(row(2L, title = "Sneaker research", tags = "shopping nike"))

        val results = fts.searchFtsWithSnippet("shopping*")

        assertEquals(2, results.size)
        assertEquals("tag weight must outrank body weight", 2L, results.first().rowid)
    }
}
