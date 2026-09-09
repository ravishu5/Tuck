package com.tuck.app.processing

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tuck.app.data.local.storage.VaultBackupService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@HiltWorker
class VaultBackupWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted private val workerParams: WorkerParameters,
    private val vaultBackupService: VaultBackupService
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val backupDir = File(appContext.filesDir, "backups").apply { mkdirs() }
            val backupZip = File(backupDir, "scheduled_backup_${System.currentTimeMillis()}.tuck")
            vaultBackupService.exportFullVaultZip(backupZip)

            // Retain only the latest 3 scheduled backups
            val existingBackups = backupDir.listFiles()?.filter { it.extension == "tuck" }?.sortedByDescending { it.lastModified() }
            if (existingBackups != null && existingBackups.size > 3) {
                existingBackups.drop(3).forEach { it.delete() }
            }

            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}
