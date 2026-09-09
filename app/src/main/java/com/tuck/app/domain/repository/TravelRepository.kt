package com.tuck.app.domain.repository

import com.tuck.app.domain.model.BucketListPlace
import com.tuck.app.domain.model.CountryVisit
import com.tuck.app.domain.model.Trip
import com.tuck.app.domain.model.TravelStats
import com.tuck.app.domain.model.TripImportResult
import kotlinx.coroutines.flow.Flow

/**
 * The travel history: where the traveller has been, when, and where they mean to go next.
 *
 * Everything here lives on the device. There is no account, no sync and no server, so the
 * history is readable only on the phone that holds it.
 */
interface TravelRepository {
    fun getTrips(): Flow<List<Trip>>
    fun getUpcomingTrips(): Flow<List<Trip>>
    fun getCountryVisits(): Flow<List<CountryVisit>>
    fun getStats(): Flow<TravelStats>

    suspend fun getTrip(id: Long): Trip?
    suspend fun saveTrip(trip: Trip): Long
    suspend fun deleteTrip(id: Long)

    /**
     * Files trips found by a scan, growing an existing trip instead of adding a second one
     * whenever the two describe the same journey.
     *
     * This is what makes a second scan safe. Photos taken later on a trip the history already
     * holds — the back half of a holiday, pictures copied off a camera afterwards — arrive as a
     * candidate that touches an existing span, and the honest answer is that the trip was
     * longer than first recorded, not that it happened twice.
     */
    suspend fun importTrips(trips: List<Trip>): TripImportResult

    fun getBucketList(): Flow<List<BucketListPlace>>
    suspend fun saveBucketPlace(place: BucketListPlace): Long
    suspend fun deleteBucketPlace(id: Long)

    /** Whether the vault already holds a trip covering this country and span. */
    suspend fun hasTripCovering(countryCode: String, start: Long, end: Long): Boolean
}
