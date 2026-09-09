package com.tuck.app.travel

import com.tuck.app.data.repository.mergedDays
import com.tuck.app.data.repository.toCountryVisits
import com.tuck.app.data.repository.toStats
import com.tuck.app.data.travel.Continents
import com.tuck.app.data.travel.distanceKm
import com.tuck.app.domain.model.Trip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.concurrent.TimeUnit

class TravelStatsTest {

    private fun day(year: Int, month: Int, dayOfMonth: Int): Long =
        Calendar.getInstance().apply {
            set(year, month - 1, dayOfMonth, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun trip(
        country: String,
        code: String,
        start: Long,
        end: Long,
        city: String? = null,
        region: String? = null
    ) = Trip(
        title = city ?: country,
        countryCode = code,
        countryName = country,
        city = city,
        region = region,
        startDate = start,
        endDate = end
    )

    @Test
    fun tripLengthCountsBothEnds() {
        val sameDay = trip("France", "FR", day(2026, 5, 1), day(2026, 5, 1))
        assertEquals(1, sameDay.days)

        val week = trip("France", "FR", day(2026, 5, 1), day(2026, 5, 7))
        assertEquals(7, week.days)
    }

    /**
     * The figure people check first is "days away", and it is the easiest one to inflate: file
     * a country trip and a city trip over the same fortnight and a naive sum reports a month.
     */
    @Test
    fun overlappingTripsCountEachDayOnce() {
        val spans = listOf(
            day(2026, 5, 1) to day(2026, 5, 14),
            day(2026, 5, 7) to day(2026, 5, 10)
        )
        assertEquals(14, spans.mergedDays())
    }

    @Test
    fun backToBackTripsDoNotDoubleCountTheJoin() {
        val spans = listOf(
            day(2026, 5, 1) to day(2026, 5, 3),
            day(2026, 5, 4) to day(2026, 5, 6)
        )
        assertEquals(6, spans.mergedDays())
    }

    @Test
    fun separateTripsAddUp() {
        val spans = listOf(
            day(2026, 1, 1) to day(2026, 1, 3),
            day(2026, 6, 1) to day(2026, 6, 2)
        )
        assertEquals(5, spans.mergedDays())
    }

    @Test
    fun statsCountDistinctPlaces() {
        val trips = listOf(
            trip("France", "FR", day(2026, 5, 1), day(2026, 5, 7), city = "Paris"),
            trip("France", "FR", day(2026, 8, 1), day(2026, 8, 3), city = "Lyon"),
            trip("Japan", "JP", day(2026, 9, 1), day(2026, 9, 10), city = "Tokyo"),
            trip("United States", "US", day(2026, 3, 1), day(2026, 3, 5), city = "Austin", region = "Texas")
        )
        val stats = trips.toStats()

        assertEquals(3, stats.countryCount)
        assertEquals(4, stats.cityCount)
        assertEquals(1, stats.usStateCount)
        assertEquals(4, stats.tripCount)
        // Europe, Asia, North America.
        assertEquals(3, stats.continentCount)
        assertEquals(10, stats.longestTripDays)
    }

    /** A region recorded outside the US is not a US state, however it is spelled. */
    @Test
    fun regionsOutsideTheUsDoNotCountAsStates() {
        val trips = listOf(
            trip("Germany", "DE", day(2026, 5, 1), day(2026, 5, 3), region = "Bavaria"),
            trip("Australia", "AU", day(2026, 6, 1), day(2026, 6, 3), region = "Victoria")
        )
        assertEquals(0, trips.toStats().usStateCount)
    }

    @Test
    fun countryVisitsGroupAndMergeDays() {
        val trips = listOf(
            trip("France", "FR", day(2026, 5, 1), day(2026, 5, 7), city = "Paris"),
            trip("France", "FR", day(2026, 5, 5), day(2026, 5, 10), city = "Nice")
        )
        val visits = trips.toCountryVisits()

        assertEquals(1, visits.size)
        val france = visits.single()
        assertEquals("FR", france.countryCode)
        assertEquals(2, france.tripCount)
        // 1st to 10th inclusive, not 7 + 6.
        assertEquals(10, france.days)
        assertEquals(listOf("Nice", "Paris"), france.cities)
    }

    @Test
    fun emptyHistoryHasNoStats() {
        val stats = emptyList<Trip>().toStats()
        assertEquals(0, stats.countryCount)
        assertEquals(0, stats.daysTravelled)
        assertEquals(0, stats.tripCount)
    }

    /**
     * The continent table is built by a local helper whose first version shadowed
     * `MutableMap.put` and recursed into itself, so merely touching the class crashed the app.
     */
    @Test
    fun continentTableResolvesWithoutRecursing() {
        assertEquals(Continents.EUROPE, Continents.of("FR"))
        assertEquals(Continents.ASIA, Continents.of("jp"))
        assertEquals(Continents.NORTH_AMERICA, Continents.of("US"))
        assertEquals(Continents.SOUTH_AMERICA, Continents.of("BR"))
        assertEquals(Continents.OCEANIA, Continents.of("AU"))
        assertEquals(Continents.AFRICA, Continents.of("ZA"))
        assertEquals(Continents.ANTARCTICA, Continents.of("AQ"))
        assertNull(Continents.of("??"))
        assertNull(Continents.of(null))
    }

    @Test
    fun everyContinentIsRepresented() {
        val continents = listOf("FR", "JP", "US", "BR", "AU", "ZA", "AQ")
            .mapNotNull { Continents.of(it) }
            .distinct()
        assertEquals(7, continents.size)
    }

    @Test
    fun distanceIsGreatCircle() {
        // Paris to London, about 344 km.
        val km = distanceKm(48.8566, 2.3522, 51.5074, -0.1278)
        assertTrue("expected ~344km, got $km", km in 330.0..360.0)
        assertEquals(0.0, distanceKm(10.0, 10.0, 10.0, 10.0), 0.001)
    }
}
