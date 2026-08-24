package com.tuck.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tuck.app.data.local.db.entity.EntityEntity

@Dao
interface EntityDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<EntityEntity>)

    @Query("SELECT * FROM entities WHERE savedItemId = :savedItemId")
    suspend fun getEntitiesForItem(savedItemId: Long): List<EntityEntity>

    @Query("DELETE FROM entities WHERE savedItemId = :savedItemId")
    suspend fun deleteForSavedItem(savedItemId: Long)
}
