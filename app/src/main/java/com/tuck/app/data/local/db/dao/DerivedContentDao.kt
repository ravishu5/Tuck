package com.tuck.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tuck.app.data.local.db.entity.DerivedPointEntity
import com.tuck.app.data.local.db.entity.DerivedSummaryEntity
import com.tuck.app.data.local.db.entity.OcrBlockEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DerivedContentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSummaries(summaries: List<DerivedSummaryEntity>): List<Long>

    @Query("SELECT * FROM derived_summaries WHERE itemId = :itemId")
    fun getSummariesForItem(itemId: Long): Flow<List<DerivedSummaryEntity>>

    @Query("SELECT * FROM derived_summaries WHERE itemId = :itemId")
    suspend fun getSummariesForItemSync(itemId: Long): List<DerivedSummaryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoints(points: List<DerivedPointEntity>): List<Long>

    @Query("SELECT * FROM derived_points WHERE itemId = :itemId ORDER BY ordinal ASC")
    fun getPointsForItem(itemId: Long): Flow<List<DerivedPointEntity>>

    @Query("SELECT * FROM derived_points WHERE itemId = :itemId ORDER BY ordinal ASC")
    suspend fun getPointsForItemSync(itemId: Long): List<DerivedPointEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOcrBlocks(blocks: List<OcrBlockEntity>): List<Long>

    @Query("SELECT * FROM ocr_blocks WHERE itemId = :itemId ORDER BY blockIndex ASC")
    fun getOcrBlocksForItem(itemId: Long): Flow<List<OcrBlockEntity>>

    @Query("SELECT * FROM ocr_blocks WHERE itemId = :itemId ORDER BY blockIndex ASC")
    suspend fun getOcrBlocksForItemSync(itemId: Long): List<OcrBlockEntity>

    @Query("DELETE FROM derived_summaries WHERE itemId = :itemId")
    suspend fun deleteSummariesForItem(itemId: Long)

    @Query("DELETE FROM derived_points WHERE itemId = :itemId")
    suspend fun deletePointsForItem(itemId: Long)

    @Query("DELETE FROM ocr_blocks WHERE itemId = :itemId")
    suspend fun deleteOcrBlocksForItem(itemId: Long)
}
