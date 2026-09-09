package com.tuck.app.data.local.db.dao

import com.tuck.app.data.local.db.entity.SavedItemFtsEntity

data class FtsSearchResult(
    val rowid: Long,
    val snippet: String? = null,
    /** Weighted `matchinfo` score. Higher is a better match. */
    val rank: Double = 0.0,
    /**
     * Indices of the indexed columns this row actually had hits in, in the column
     * order declared by `SavedItemFtsDaoImpl.CREATE_TABLE`. The scorer already walks
     * every phrase/column pair to compute [rank]; recording which of those pairs were
     * non-zero costs nothing and is what lets the UI say why a result matched.
     */
    val matchedColumns: Set<Int> = emptySet()
)

interface SavedItemFtsDao {
    suspend fun insertOrUpdate(ftsEntity: SavedItemFtsEntity)
    suspend fun delete(rowid: Long)
    /** Returns matches ordered by BM25 relevance, best first. */
    suspend fun searchFtsWithSnippet(ftsQuery: String): List<FtsSearchResult>
    suspend fun searchFtsRowIds(ftsQuery: String): List<Long>
    /** Drops and repopulates the whole index from the source tables. */
    /** Every rowid currently in the index, to find saves that fell out of it. */
    suspend fun allIndexedRowIds(): List<Long>
    suspend fun rebuildIndex()
}
