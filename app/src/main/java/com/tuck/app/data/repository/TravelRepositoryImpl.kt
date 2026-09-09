package com.tuck.app.data.repository

import com.tuck.app.data.local.db.dao.TravelDao
import com.tuck.app.data.local.db.entity.BucketListPlaceEntity
import com.tuck.app.data.local.db.entity.TripEntity
import com.tuck.app.data.travel.Continents
import com.tuck.app.domain.model.BucketListPlace
import com.tuck.app.domain.model.CountryVisit
import com.tuck.app.domain.model.Trip
import com.tuck.app.domain.model.TravelStats
import com.tuck.app.domain.model.TripImportResult
import com.tuck.app.domain.model.TripSource
import com.tuck.app.domain.repository.TravelRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TravelRepositoryImpl @Inject constructor(
    private val dao: TravelDao
) : TravelRepository {

    override fun getTrips(): Flow<List<Trip>> =
        dao.getAllTrips().map { list -> list.map { it.toDomain() } }

    override fun getUpcomingTrips(): Flow<List<Trip>> =
        dao.getUpcomingTrips(startOfToday()).map { list -> list.map { it.toDomain() } }

    override fun getCountryVisits(): Flow<List<CountryVisit>> =
        dao.getAllTrips().map { trips -> trips.map { it.toDomain() }.toCountryVisits() }

    override fun getStats(): Flow<TravelStats> =
        dao.getAllTrips().map { trips -> trips.map { it.toDomain() }.toStats() }

    override suspend fun getTrip(id: Long): Trip? = dao.getTrip(id)?.toDomain()

    override suspend fun saveTrip(trip: Trip): Long = dao.insert(trip.toEntity())

    override suspend fun importTrips(trips: List<Trip>): TripImportResult {
        if (trips.isEmpty()) return TripImportResult()

        var added = 0
        var extended = 0

        // One at a time rather than a batch insert, because each candidate has to be checked
        // against what the previous ones just wrote — two new clusters from the same journey
        // would otherwise both be inserted instead of the second extending the first.
        for (trip in trips) {
            val neighbour = dao.findOverlapping(
                countryCode = trip.countryCode.uppercase(),
                start = trip.startDate - JOIN_TOLERANCE_MS,
                end = trip.endDate + JOIN_TOLERANCE_MS
            ).firstOrNull()

            if (neighbour == null) {
                dao.insert(trip.toEntity())
                added++
            } else {
                dao.update(
                    neighbour.copy(
                        startDate = minOf(neighbour.startDate, trip.startDate),
                        endDate = maxOf(neighbour.endDate, trip.endDate),
                        photoCount = neighbour.photoCount + trip.photoCount,
                        // The city is only filled in if the existing trip had none: a trip the
                        // traveller named themselves must not be renamed by a later scan.
                        city = neighbour.city ?: trip.city,
                        region = neighbour.region ?: trip.region,
                        updatedAt = System.currentTimeMillis()
                    )
                )
                extended++
            }
        }
        return TripImportResult(added = added, extended = extended)
    }

    override suspend fun deleteTrip(id: Long) = dao.deleteById(id)

    override fun getBucketList(): Flow<List<BucketListPlace>> =
        dao.getBucketList().map { list -> list.map { it.toDomain() } }

    override suspend fun saveBucketPlace(place: BucketListPlace): Long =
        dao.insertBucketPlace(place.toEntity())

    override suspend fun deleteBucketPlace(id: Long) = dao.deleteBucketPlace(id)

    override suspend fun hasTripCovering(countryCode: String, start: Long, end: Long): Boolean =
        dao.findOverlapping(countryCode.uppercase(), start, end).isNotEmpty()
}

// --- Aggregation ---------------------------------------------------------------------
// Computed in memory rather than in SQL. The whole history is a few hundred rows even for a
// heavy traveller, and the two figures that matter — days away and countries this year — need
// interval merging and a calendar, neither of which SQLite does well.

internal fun List<Trip>.toCountryVisits(): List<CountryVisit> =
    groupBy { it.countryCode.uppercase() }
        .map { (code, trips) ->
            CountryVisit(
                countryCode = code,
                countryName = trips.first().countryName,
                // Days are merged per country for the same reason as the global figure: two
                // trips filed over one week are one week in that country.
                days = trips.map { it.startDate to it.endDate }.mergedDays(),
                tripCount = trips.size,
                cities = trips.mapNotNull { it.city?.takeIf(String::isNotBlank) }
                    .distinct()
                    .sorted(),
                regions = trips.mapNotNull { it.region?.takeIf(String::isNotBlank) }
                    .distinct()
                    .sorted(),
                firstVisit = trips.minOf { it.startDate },
                lastVisit = trips.maxOf { it.endDate }
            )
        }
        .sortedWith(compareByDescending<CountryVisit> { it.days }.thenBy { it.countryName })

internal fun List<Trip>.toStats(): TravelStats {
    if (isEmpty()) return TravelStats()

    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val thisYear = filter { it.startDate.yearOf() == currentYear }

    return TravelStats(
        countryCount = map { it.countryCode.uppercase() }.distinct().size,
        cityCount = mapNotNull { it.city?.takeIf(String::isNotBlank) }
            .map { it.lowercase() }
            .distinct()
            .size,
        // Only meaningful inside the US, so a region recorded for anywhere else is ignored
        // rather than quietly inflating the count.
        usStateCount = filter { it.countryCode.equals("US", ignoreCase = true) }
            .mapNotNull { it.region?.takeIf(String::isNotBlank) }
            .map { it.lowercase() }
            .distinct()
            .size,
        tripCount = size,
        daysTravelled = map { it.startDate to it.endDate }.mergedDays(),
        continentCount = mapNotNull { Continents.of(it.countryCode) }.distinct().size,
        longestTripDays = maxOf { it.days },
        currentYearCountryCount = thisYear.map { it.countryCode.uppercase() }.distinct().size
    )
}

/**
 * Total days covered by a set of spans, counting any day once however many spans include it.
 *
 * Spans are sorted and merged first. Summing each span's own length instead would double-count
 * every overlap, so a traveller who filed a country trip and a city trip for the same fortnight
 * would be told they had been away for a month.
 */
internal fun List<Pair<Long, Long>>.mergedDays(): Int {
    if (isEmpty()) return 0
    val sorted = sortedBy { it.first }
    var total = 0L
    var start = sorted.first().first
    var end = sorted.first().second

    for ((nextStart, nextEnd) in sorted.drop(1)) {
        // A day's gap still joins them: the two spans are contiguous, not overlapping, and
        // there is no day between them to leave out.
        if (nextStart <= end + DAY_MS) {
            if (nextEnd > end) end = nextEnd
        } else {
            total += daysInclusive(start, end)
            start = nextStart
            end = nextEnd
        }
    }
    total += daysInclusive(start, end)
    return total.toInt()
}

private fun daysInclusive(start: Long, end: Long): Long =
    (TimeUnit.MILLISECONDS.toDays(end - start) + 1).coerceAtLeast(1)

private val DAY_MS = TimeUnit.DAYS.toMillis(1)

/**
 * How far apart two spans can be and still be the same journey.
 *
 * A day, because a trip's photos are clustered by day and the last picture of one day and the
 * first of the next belong to the same holiday. Wider than this and two genuinely separate
 * weekends in the same country would be silently welded into one long trip.
 */
private val JOIN_TOLERANCE_MS = TimeUnit.DAYS.toMillis(1)

private fun Long.yearOf(): Int =
    Calendar.getInstance().also { it.timeInMillis = this }.get(Calendar.YEAR)

internal fun startOfToday(): Long = Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

// --- Mapping -------------------------------------------------------------------------

private fun TripEntity.toDomain() = Trip(
    id = id,
    title = title,
    countryCode = countryCode,
    countryName = countryName,
    city = city,
    region = region,
    latitude = latitude,
    longitude = longitude,
    startDate = startDate,
    endDate = endDate,
    notes = notes,
    source = TripSource.fromString(source),
    photoCount = photoCount,
    createdAt = createdAt,
    updatedAt = updatedAt
)

private fun Trip.toEntity() = TripEntity(
    id = id,
    title = title,
    countryCode = countryCode.uppercase(),
    countryName = countryName,
    city = city,
    region = region,
    latitude = latitude,
    longitude = longitude,
    startDate = startDate,
    endDate = endDate,
    notes = notes,
    source = source.name,
    photoCount = photoCount,
    createdAt = createdAt,
    updatedAt = System.currentTimeMillis()
)

private fun BucketListPlaceEntity.toDomain() = BucketListPlace(
    id = id,
    place = place,
    countryCode = countryCode,
    countryName = countryName,
    notes = notes,
    achievedTripId = achievedTripId,
    createdAt = createdAt
)

private fun BucketListPlace.toEntity() = BucketListPlaceEntity(
    id = id,
    place = place,
    countryCode = countryCode,
    countryName = countryName,
    notes = notes,
    achievedTripId = achievedTripId,
    createdAt = createdAt
)
