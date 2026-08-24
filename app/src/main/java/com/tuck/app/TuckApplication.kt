package com.tuck.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.tuck.app.domain.repository.SettingsRepository
import com.tuck.app.processing.MemoryNotificationScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class TuckApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var memoryNotificationScheduler: MemoryNotificationScheduler

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Re-apply the user's choice on launch so the schedule survives reboots
        // and reinstalls without ever enqueueing work they did not ask for.
        applicationScope.launch {
            val enabled = try {
                settingsRepository.getSettings().first().memoryResurfacingEnabled
            } catch (e: Exception) {
                false
            }
            memoryNotificationScheduler.apply(enabled)
        }
    }
}
