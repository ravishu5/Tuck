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
class ReminderWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val savedItemDao: SavedItemDao
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val KEY_ITEM_ID = "key_item_id"
        const val CHANNEL_ID = "tuck_reminders"
        fun notificationIdFor(itemId: Long): Int = (itemId % Int.MAX_VALUE).toInt()
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val itemId = inputData.getLong(KEY_ITEM_ID, -1L)
        if (itemId <= 0L) return@withContext Result.failure()

        val item = savedItemDao.getItemById(itemId) ?: return@withContext Result.success()

        // Anything that happened since the reminder was set wins: acted on, trashed, or
        // rescheduled. Firing anyway would be the exact nagging that makes people
        // switch reminders off.
        if (item.isDeleted || item.completedAt != null || item.remindAt == null) {
            return@withContext Result.success()
        }

        notify(item.id, item.title.ifBlank { "A saved item" })
        Result.success()
    }

    private fun notify(itemId: Long, title: String) {
        val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                CHANNEL_ID,
                appContext.getString(R.string.notif_reminder_channel),
                NotificationManager.IMPORTANCE_DEFAULT
            )
                    .apply { description = appContext.getString(R.string.notif_reminder_channel_desc) }
            )
        }

        val open = PendingIntent.getActivity(
            appContext,
            notificationIdFor(itemId),
            Intent(appContext, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("open_item_id", itemId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(appContext.getString(R.string.notif_reminder_body))
            .setAutoCancel(true)
            .setContentIntent(open)
            .addAction(0, "Done", ReminderActionReceiver.pendingIntent(appContext, itemId, ReminderActionReceiver.ACTION_DONE))
            .addAction(0, appContext.getString(R.string.notif_snooze_a_day), ReminderActionReceiver.pendingIntent(appContext, itemId, ReminderActionReceiver.ACTION_SNOOZE))
            .build()

        manager.notify(notificationIdFor(itemId), notification)
    }
}
