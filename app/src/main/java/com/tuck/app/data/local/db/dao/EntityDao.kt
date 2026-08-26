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

    @Query("SELECT * FROM entities WHERE savedItemId = :savedItemId AND producer = :producer")
    suspend fun getEntitiesByProducer(savedItemId: Long, producer: String): List<EntityEntity>

    @Query("SELECT * FROM entities WHERE type = :type")
    suspend fun getEntitiesByType(type: com.tuck.app.domain.model.EntityType): List<EntityEntity>

    @Query("SELECT * FROM entities")
    suspend fun getAllEntities(): List<EntityEntity>

    @Query("DELETE FROM entities WHERE savedItemId = :savedItemId")
    suspend fun deleteForSavedItem(savedItemId: Long)

    @Query("DELETE FROM entities WHERE savedItemId = :savedItemId AND producer = :producer")
    suspend fun deleteForSavedItemByProducer(savedItemId: Long, producer: String)
}
