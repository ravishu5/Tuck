package com.tuck.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tuck.app.data.local.db.entity.BucketListPlaceEntity
import com.tuck.app.data.local.db.entity.TripEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TravelDao {

    @Query("SELECT * FROM trips ORDER BY startDate DESC, id DESC")
    fun getAllTrips(): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE id = :id")
    suspend fun getTrip(id: Long): TripEntity?

    /**
     * Trips that have not finished yet, soonest first.
     *
     * Compared against `endDate` rather than `startDate` so a trip the traveller is currently
     * on still counts as upcoming instead of vanishing on its first morning.
     */
    @Query("SELECT * FROM trips WHERE endDate >= :now ORDER BY startDate ASC")
    fun getUpcomingTrips(now: Long): Flow<List<TripEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(trip: TripEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(trips: List<TripEntity>): List<Long>

    @Update
    suspend fun update(trip: TripEntity)

    @Delete
    suspend fun delete(trip: TripEntity)

    @Query("DELETE FROM trips WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * Trips overlapping a span, used to spot a scanned trip the vault already holds.
     *
     * Two spans overlap when each starts before the other ends — the whole test, with no
     * special case for one containing the other.
     */
    @Query(
        """
        SELECT * FROM trips
        WHERE countryCode = :countryCode
          AND startDate <= :end
          AND endDate >= :start
        """
    )
    suspend fun findOverlapping(countryCode: String, start: Long, end: Long): List<TripEntity>

    @Query("SELECT * FROM bucket_list_places ORDER BY achievedTripId IS NOT NULL, createdAt DESC")
    fun getBucketList(): Flow<List<BucketListPlaceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBucketPlace(place: BucketListPlaceEntity): Long

    @Update
    suspend fun updateBucketPlace(place: BucketListPlaceEntity)

    @Query("DELETE FROM bucket_list_places WHERE id = :id")
    suspend fun deleteBucketPlace(id: Long)
}
