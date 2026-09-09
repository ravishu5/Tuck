package com.tuck.app.quickrecord

import com.tuck.app.ui.quickrecord.formatDuration
import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class QuickRecordFormatTest {

    // --- Duration readout ---------------------------------------------------------------

    @Test
    fun durationCountsFromZero() {
        assertEquals("0:00", formatDuration(0))
        assertEquals("0:01", formatDuration(1_000))
        assertEquals("0:59", formatDuration(59_999))
    }

    @Test
    fun minutesRollOverWithPaddedSeconds() {
        assertEquals("1:00", formatDuration(60_000))
        assertEquals("2:05", formatDuration(125_000))
        assertEquals("59:59", formatDuration(3_599_000))
    }

    /** The hours field only appears once there are hours, so a short memo stays short. */
    @Test
    fun hoursOnlyAppearWhenThereAreHours() {
        assertEquals("1:00:00", formatDuration(3_600_000))
        assertEquals("2:03:04", formatDuration(7_384_000))
    }

    /** A clock that has not started yet must not read as a negative time. */
    @Test
    fun negativeElapsedReadsAsZero() {
        assertEquals("0:00", formatDuration(-5_000))
    }

    // --- Memo naming --------------------------------------------------------------------

    /**
     * Mirrors the controller's pattern. Pinned here because the format is the memo's only
     * name — nobody is asked for one — so a change to it is a change to every future title.
     */
    private val titleFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US)

    private fun at(y: Int, mo: Int, d: Int, h: Int, mi: Int, sec: Int = 0): Long =
        Calendar.getInstance().apply {
            set(y, mo - 1, d, h, mi, sec)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    @Test
    fun titleReadsAsADateAndTime() {
        assertEquals("09 Sep 2026, 12:45 PM", titleFormat.format(at(2026, 9, 9, 12, 45)))
        assertEquals("10 Sep 2026, 08:03 AM", titleFormat.format(at(2026, 9, 10, 8, 3)))
    }

    /** Midnight and noon are the two the 12-hour clock most often gets wrong. */
    @Test
    fun middayAndMidnightAreNotConfused() {
        assertEquals("01 Jan 2026, 12:00 AM", titleFormat.format(at(2026, 1, 1, 0, 0)))
        assertEquals("01 Jan 2026, 12:00 PM", titleFormat.format(at(2026, 1, 1, 12, 0)))
    }

    /**
     * Two memos in the same minute produce the same base name, which is exactly why the
     * controller has to disambiguate rather than trusting the timestamp to be unique.
     */
    @Test
    fun sameMinuteCollides() {
        val first = titleFormat.format(at(2026, 9, 9, 12, 45, 10))
        val second = titleFormat.format(at(2026, 9, 9, 12, 45, 50))
        assertEquals(first, second)
    }

    @Test
    fun differentMinutesDoNotCollide() {
        val first = titleFormat.format(at(2026, 9, 9, 12, 45))
        val second = titleFormat.format(at(2026, 9, 9, 12, 47))
        assertEquals("09 Sep 2026, 12:45 PM", first)
        assertEquals("09 Sep 2026, 12:47 PM", second)
    }
}
