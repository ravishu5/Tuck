package com.tuck.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tuck.app.data.local.db.entity.FilingRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FilingRuleDao {
    @Query("SELECT * FROM filing_rules ORDER BY sortOrdinal ASC, id ASC")
    fun getAllRules(): Flow<List<FilingRuleEntity>>

    @Query("SELECT * FROM filing_rules WHERE isEnabled = 1 ORDER BY sortOrdinal ASC, id ASC")
    suspend fun getEnabledRules(): List<FilingRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: FilingRuleEntity): Long

    @Update
    suspend fun update(rule: FilingRuleEntity)

    @Delete
    suspend fun delete(rule: FilingRuleEntity)

    @Query("UPDATE filing_rules SET isEnabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    @Query("UPDATE filing_rules SET matchCount = matchCount + 1, lastMatchedAt = :now WHERE id = :id")
    suspend fun recordMatch(id: Long, now: Long = System.currentTimeMillis())
}
