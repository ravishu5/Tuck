package com.tuck.app.travel

import com.tuck.app.data.local.db.dao.TravelDao
import com.tuck.app.data.local.db.entity.BucketListPlaceEntity
import com.tuck.app.data.local.db.entity.TripEntity
import com.tuck.app.data.repository.TravelRepositoryImpl
import com.tuck.app.domain.model.Trip
import com.tuck.app.domain.model.TripSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * A second scan must not turn one journey into two.
 *
 * This is the whole risk of scanning incrementally: photos from the back half of a trip arrive
 * later — copied off a camera, or simply taken after the first scan ran — and the history
 * already holds the front half.
 */
class TripImportTest {

    private fun day(year: Int, month: Int, dayOfMonth: Int): Long =
        Calendar.getInstance().apply {
            set(year, month - 1, dayOfMonth, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    /** Just enough of a DAO to exercise the import logic without a database. */
    private class FakeTravelDao(seed: List<TripEntity> = emptyList()) : TravelDao {
        val trips = seed.toMutableList()
        private var nextId = (seed.maxOfOrNull { it.id } ?: 0L) + 1

        override fun getAllTrips(): Flow<List<TripEntity>> = flowOf(trips.toList())
        override suspend fun getTrip(id: Long): TripEntity? = trips.firstOrNull { it.id == id }
        override fun getUpcomingTrips(now: Long): Flow<List<TripEntity>> = flowOf(emptyList())

        override suspend fun insert(trip: TripEntity): Long {
            val id = nextId++
            trips += trip.copy(id = id)
            return id
        }

        override suspend fun insertAll(trips: List<TripEntity>): List<Long> =
            trips.map { insert(it) }

        override suspend fun update(trip: TripEntity) {
            val index = trips.indexOfFirst { it.id == trip.id }
            if (index >= 0) trips[index] = trip
        }

        override suspend fun delete(trip: TripEntity) {
            trips.removeAll { it.id == trip.id }
        }

        override suspend fun deleteById(id: Long) {
            trips.removeAll { it.id == id }
        }

        override suspend fun findOverlapping(
            countryCode: String,
            start: Long,
            end: Long
        ): List<TripEntity> = trips.filter {
            it.countryCode == countryCode && it.startDate <= end && it.endDate >= start
        }

        override fun getBucketList(): Flow<List<BucketListPlaceEntity>> = flowOf(emptyList())
        override suspend fun insertBucketPlace(place: BucketListPlaceEntity): Long = 0
        override suspend fun updateBucketPlace(place: BucketListPlaceEntity) = Unit
        override suspend fun deleteBucketPlace(id: Long) = Unit
    }

    private fun existing(
        id: Long,
        code: String,
        start: Long,
        end: Long,
        city: String? = null,
        photos: Int = 10
    ) = TripEntity(
        id = id,
        title = city ?: code,
        countryCode = code,
        countryName = code,
        city = city,
        startDate = start,
        endDate = end,
        source = TripSource.PHOTO_IMPORT.name,
        photoCount = photos
    )

    private fun incoming(
        code: String,
        start: Long,
        end: Long,
        city: String? = null,
        photos: Int = 5
    ) = Trip(
        title = city ?: code,
        countryCode = code,
        countryName = code,
        city = city,
        startDate = start,
        endDate = end,
        source = TripSource.PHOTO_IMPORT,
        photoCount = photos
    )

    @Test
    fun aTripTheHistoryDoesNotHoldIsAdded() = runTest {
        val dao = FakeTravelDao()
        val repo = TravelRepositoryImpl(dao)

        val result = repo.importTrips(
            listOf(incoming("FR", day(2026, 5, 1), day(2026, 5, 5), city = "Paris"))
        )

        assertEquals(1, result.added)
        assertEquals(0, result.extended)
        assertEquals(1, dao.trips.size)
    }

    /** The back half of a trip, found by a later scan, lengthens the trip already recorded. */
    @Test
    fun newPhotosFromTheSameTripExtendItRatherThanDuplicatingIt() = runTest {
        val dao = FakeTravelDao(
            listOf(existing(1, "FR", day(2026, 5, 1), day(2026, 5, 3), city = "Paris", photos = 20))
        )
        val repo = TravelRepositoryImpl(dao)

        val result = repo.importTrips(
            listOf(incoming("FR", day(2026, 5, 4), day(2026, 5, 8), photos = 7))
        )

        assertEquals(0, result.added)
        assertEquals(1, result.extended)
        assertEquals(1, dao.trips.size)

        val merged = dao.trips.single()
        assertEquals(day(2026, 5, 1), merged.startDate)
        assertEquals(day(2026, 5, 8), merged.endDate)
        assertEquals(27, merged.photoCount)
    }

    /** A trip the traveller named keeps its name when a later scan grows it. */
    @Test
    fun extendingDoesNotOverwriteAPlaceTheTravellerAlreadySet() = runTest {
        val dao = FakeTravelDao(
            listOf(existing(1, "FR", day(2026, 5, 1), day(2026, 5, 3), city = "Honeymoon"))
        )
        val repo = TravelRepositoryImpl(dao)

        repo.importTrips(
            listOf(incoming("FR", day(2026, 5, 3), day(2026, 5, 6), city = "Paris"))
        )

        assertEquals("Honeymoon", dao.trips.single().city)
    }

    /** Two separate visits to one country stay two trips. */
    @Test
    fun visitsMonthsApartStaySeparate() = runTest {
        val dao = FakeTravelDao(
            listOf(existing(1, "JP", day(2026, 1, 1), day(2026, 1, 10)))
        )
        val repo = TravelRepositoryImpl(dao)

        val result = repo.importTrips(
            listOf(incoming("JP", day(2026, 9, 1), day(2026, 9, 5)))
        )

        assertEquals(1, result.added)
        assertEquals(0, result.extended)
        assertEquals(2, dao.trips.size)
    }

    /** Same dates, different country: a border crossing is not the same trip. */
    @Test
    fun aDifferentCountryIsNeverTheSameTrip() = runTest {
        val dao = FakeTravelDao(
            listOf(existing(1, "IN", day(2026, 6, 8), day(2026, 6, 12)))
        )
        val repo = TravelRepositoryImpl(dao)

        val result = repo.importTrips(
            listOf(incoming("NP", day(2026, 6, 10), day(2026, 6, 10)))
        )

        assertEquals(1, result.added)
        assertEquals(2, dao.trips.size)
    }

    /**
     * Two candidates from one journey must collapse into a single trip, which only works if
     * each is checked against what the previous one just wrote.
     */
    @Test
    fun twoCandidatesFromOneJourneyBecomeOneTrip() = runTest {
        val dao = FakeTravelDao()
        val repo = TravelRepositoryImpl(dao)

        val result = repo.importTrips(
            listOf(
                incoming("IT", day(2026, 4, 1), day(2026, 4, 3), photos = 4),
                incoming("IT", day(2026, 4, 4), day(2026, 4, 6), photos = 6)
            )
        )

        assertEquals(1, result.added)
        assertEquals(1, result.extended)
        assertEquals(1, dao.trips.size)
        assertEquals(day(2026, 4, 6), dao.trips.single().endDate)
    }

    @Test
    fun importingNothingDoesNothing() = runTest {
        val dao = FakeTravelDao()
        val repo = TravelRepositoryImpl(dao)
        val result = repo.importTrips(emptyList())
        assertEquals(0, result.total)
        assertEquals(0, dao.trips.size)
    }
}
