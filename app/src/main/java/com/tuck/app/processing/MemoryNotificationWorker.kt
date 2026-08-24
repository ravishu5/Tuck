package com.tuck.app.processing

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tuck.app.R
import com.tuck.app.data.local.db.dao.SavedItemDao
import com.tuck.app.ui.MainActivity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class MemoryNotificationWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val savedItemDao: SavedItemDao
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val thirtyDaysAgo = now - (30L * 24 * 60 * 60 * 1000)

        val forgottenItem = savedItemDao.getAllActiveItemsList()
            .filter { it.createdAt < thirtyDaysAgo && it.openCount == 0 && !it.isArchived }
            .shuffled()
            .firstOrNull()

        if (forgottenItem != null) {
            postMemoryNotification(forgottenItem.id, forgottenItem.title ?: "Rediscover from your vault")
        }

        Result.success()
    }

    private fun postMemoryNotification(itemId: Long, itemTitle: String) {
        val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "tuck_memories"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Vault Memories & Rediscovery",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Weekly resurfacing of forgotten saved items"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(appContext, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_item_id", itemId)
        }

        val pendingIntent = PendingIntent.getActivity(
            appContext,
            itemId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(appContext, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Rediscover from your vault")
            .setContentText(itemTitle)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(1001, notification)
    }
}
