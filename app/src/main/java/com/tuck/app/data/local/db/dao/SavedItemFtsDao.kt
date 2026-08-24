package com.tuck.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tuck.app.data.local.db.entity.SavedItemFtsEntity

data class FtsSearchResult(
    val rowid: Long,
    val snippet: String? = null
)

@Dao
interface SavedItemFtsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(ftsEntity: SavedItemFtsEntity)

    @Query("DELETE FROM saved_items_fts WHERE rowid = :rowid")
    suspend fun delete(rowid: Long)

    @Query("""
        SELECT rowid, snippet(saved_items_fts, '<b>', '</b>', '...', -1, 30) AS snippet
        FROM saved_items_fts 
        WHERE saved_items_fts MATCH :ftsQuery
    """)
    suspend fun searchFtsWithSnippet(ftsQuery: String): List<FtsSearchResult>

    @Query("""
        SELECT rowid 
        FROM saved_items_fts 
        WHERE saved_items_fts MATCH :ftsQuery
    """)
    suspend fun searchFtsRowIds(ftsQuery: String): List<Long>
}
