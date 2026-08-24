package com.tuck.app.data.local.db.dao

import com.tuck.app.data.local.db.entity.SavedItemFtsEntity

data class FtsSearchResult(
    val rowid: Long,
    val snippet: String? = null,
    /** BM25 score. Lower is a better match; nulls sort last. */
    val rank: Double = 0.0
)

interface SavedItemFtsDao {
    suspend fun insertOrUpdate(ftsEntity: SavedItemFtsEntity)
    suspend fun delete(rowid: Long)
    /** Returns matches ordered by BM25 relevance, best first. */
    suspend fun searchFtsWithSnippet(ftsQuery: String): List<FtsSearchResult>
    suspend fun searchFtsRowIds(ftsQuery: String): List<Long>
    /** Drops and repopulates the whole index from the source tables. */
    suspend fun rebuildIndex()
}
