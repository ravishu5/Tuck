package com.tuck.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tuck.app.data.local.db.entity.ChecklistItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChecklistDao {
    @Query("SELECT * FROM checklist_items WHERE itemId = :itemId ORDER BY ordinal ASC, id ASC")
    fun getForItem(itemId: Long): Flow<List<ChecklistItemEntity>>

    @Query("SELECT COUNT(*) FROM checklist_items WHERE itemId = :itemId")
    suspend fun countFor(itemId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ChecklistItemEntity): Long

    @Query("UPDATE checklist_items SET isDone = :isDone WHERE id = :id")
    suspend fun setDone(id: Long, isDone: Boolean)

    @Query("UPDATE checklist_items SET text = :text WHERE id = :id")
    suspend fun setText(id: Long, text: String)

    @Delete
    suspend fun delete(item: ChecklistItemEntity)
}
