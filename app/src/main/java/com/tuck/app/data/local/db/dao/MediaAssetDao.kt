package com.tuck.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tuck.app.data.local.db.entity.MediaAssetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaAssetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(asset: MediaAssetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(assets: List<MediaAssetEntity>): List<Long>

    @Query("SELECT * FROM media_assets WHERE itemId = :itemId ORDER BY ordinal ASC")
    fun getAssetsForItem(itemId: Long): Flow<List<MediaAssetEntity>>

    @Query("SELECT * FROM media_assets WHERE itemId = :itemId ORDER BY ordinal ASC")
    suspend fun getAssetsForItemSync(itemId: Long): List<MediaAssetEntity>

    @Query("DELETE FROM media_assets WHERE itemId = :itemId")
    suspend fun deleteAssetsForItem(itemId: Long)

    @Query("SELECT * FROM media_assets WHERE id = :id")
    suspend fun getAssetById(id: Long): MediaAssetEntity?
}
