package com.tuck.app

import com.tuck.app.processing.ReminderPreset
import com.tuck.app.processing.ReminderScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ReminderSchedulerTest {

    private fun at(year: Int, month: Int, day: Int, hour: Int = 14): Long =
        Calendar.getInstance().apply {
            clear(); set(year, month - 1, day, hour, 0, 0)
        }.timeInMillis

    private fun field(millis: Long, field: Int): Int =
        Calendar.getInstance().apply { timeInMillis = millis }.get(field)

    @Test
    fun laterTodayIsThreeHoursOut() {
        val now = at(2026, 8, 29)

        val resolved = ReminderScheduler.resolve(ReminderPreset.LATER_TODAY, now)

        assertEquals(now + 3 * 60 * 60 * 1000L, resolved)
    }

    @Test
    fun tomorrowMorningLandsAtNineTheFollowingDay() {
        val now = at(2026, 8, 29, hour = 22)

        val resolved = ReminderScheduler.resolve(ReminderPreset.TOMORROW_MORNING, now)

        assertEquals(9, field(resolved, Calendar.HOUR_OF_DAY))
        assertEquals(0, field(resolved, Calendar.MINUTE))
        assertEquals(30, field(resolved, Calendar.DAY_OF_MONTH))
        assertTrue("must be in the future even when set late at night", resolved > now)
    }

    @Test
    fun thisWeekendIsTheComingSaturday() {
        // 2026-08-26 is a Wednesday.
        val wednesday = at(2026, 8, 26)

        val resolved = ReminderScheduler.resolve(ReminderPreset.THIS_WEEKEND, wednesday)

        assertEquals(Calendar.SATURDAY, field(resolved, Calendar.DAY_OF_WEEK))
        assertEquals(29, field(resolved, Calendar.DAY_OF_MONTH))
    }

    @Test
    fun onSaturdayThisWeekendMeansNextSaturdayNotToday() {
        val saturday = at(2026, 8, 29)
        assertEquals(Calendar.SATURDAY, field(saturday, Calendar.DAY_OF_WEEK))

        val resolved = ReminderScheduler.resolve(ReminderPreset.THIS_WEEKEND, saturday)

        assertEquals(Calendar.SATURDAY, field(resolved, Calendar.DAY_OF_WEEK))
        assertTrue("a reminder must never resolve to the past", resolved > saturday)
        assertEquals(5, field(resolved, Calendar.DAY_OF_MONTH))
    }

    @Test
    fun nextWeekIsSevenDaysOutAtNine() {
        val now = at(2026, 8, 29)

        val resolved = ReminderScheduler.resolve(ReminderPreset.NEXT_WEEK, now)

        assertEquals(9, field(resolved, Calendar.HOUR_OF_DAY))
        assertEquals(5, field(resolved, Calendar.DAY_OF_MONTH))
    }

    @Test
    fun everyPresetResolvesToTheFuture() {
        val now = System.currentTimeMillis()

        ReminderPreset.entries.forEach { preset ->
            assertTrue(
                "$preset resolved into the past",
                ReminderScheduler.resolve(preset, now) > now
            )
        }
    }

    @Test
    fun workNamesAreUniquePerItemSoRemindersDoNotOverwriteEachOther() {
        assertEquals("tuck_reminder_7", ReminderScheduler.workNameFor(7L))
        assertTrue(ReminderScheduler.workNameFor(7L) != ReminderScheduler.workNameFor(8L))
    }
}
