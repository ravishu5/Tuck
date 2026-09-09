package com.tuck.app.processing

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the schedule for [MemoryNotificationWorker].
 *
 * Resurfacing is opt-in, so nothing is enqueued until the user turns it on in
 * Settings; turning it off cancels the work rather than silently no-opping.
 */
@Singleton
class MemoryNotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private companion object {
        const val WORK_NAME = "tuck_weekly_memory"
    }

    fun apply(enabled: Boolean) {
        if (enabled) schedule() else cancel()
    }

    private fun schedule() {
        val request = PeriodicWorkRequestBuilder<MemoryNotificationWorker>(7, TimeUnit.DAYS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .setInitialDelay(1, TimeUnit.DAYS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    private fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
