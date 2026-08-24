package com.tuck.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tuck.app.data.local.db.entity.ItemRawPayloadEntity

@Dao
interface ItemRawPayloadDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(payload: ItemRawPayloadEntity): Long

    @Query("SELECT * FROM item_raw_payload WHERE itemId = :itemId ORDER BY receivedAt DESC LIMIT 1")
    suspend fun getPayloadForItem(itemId: Long): ItemRawPayloadEntity?

    @Query("DELETE FROM item_raw_payload WHERE itemId = :itemId")
    suspend fun deletePayloadForItem(itemId: Long)
}
