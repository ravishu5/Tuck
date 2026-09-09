package com.tuck.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tuck.app.data.local.db.entity.SearchHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(item: SearchHistoryEntity)

    @Query("SELECT query FROM search_history ORDER BY searchedAt DESC LIMIT :limit")
    fun getRecentQueries(limit: Int = 10): Flow<List<String>>

    @Query("DELETE FROM search_history")
    suspend fun clearHistory()
}
