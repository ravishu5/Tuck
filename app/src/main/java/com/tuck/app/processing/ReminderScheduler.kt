package com.tuck.app.processing

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** The preset choices offered when setting a reminder. */
enum class ReminderPreset { LATER_TODAY, TOMORROW_MORNING, THIS_WEEKEND, NEXT_WEEK }

@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        fun workNameFor(itemId: Long): String = "tuck_reminder_$itemId"

        /**
         * Resolves a preset to an absolute time.
         *
         * Deliberately coarse. Exact alarms need SCHEDULE_EXACT_ALARM on Android 12+,
         * which is a heavyweight permission to ask for so a saved article can resurface;
         * "tomorrow morning" is what people actually mean.
         */
        fun resolve(preset: ReminderPreset, now: Long = System.currentTimeMillis()): Long {
            val calendar = Calendar.getInstance().apply { timeInMillis = now }
            return when (preset) {
                ReminderPreset.LATER_TODAY -> now + TimeUnit.HOURS.toMillis(3)

                ReminderPreset.TOMORROW_MORNING -> calendar.apply {
                    add(Calendar.DAY_OF_YEAR, 1)
                    atHour(9)
                }.timeInMillis

                ReminderPreset.THIS_WEEKEND -> calendar.apply {
                    // The coming Saturday; if it is already the weekend, next Saturday.
                    do { add(Calendar.DAY_OF_YEAR, 1) } while (get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY)
                    atHour(10)
                }.timeInMillis

                ReminderPreset.NEXT_WEEK -> calendar.apply {
                    add(Calendar.DAY_OF_YEAR, 7)
                    atHour(9)
                }.timeInMillis
            }
        }

        private fun Calendar.atHour(hour: Int) {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    fun schedule(itemId: Long, remindAt: Long) {
        val delay = (remindAt - System.currentTimeMillis()).coerceAtLeast(0L)

        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(Data.Builder().putLong(ReminderWorker.KEY_ITEM_ID, itemId).build())
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            workNameFor(itemId),
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancel(itemId: Long) {
        WorkManager.getInstance(context).cancelUniqueWork(workNameFor(itemId))
    }
}
