package com.tuck.app.processing

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.PendingIntent
import com.tuck.app.data.local.db.dao.SavedItemDao
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Handles the notification actions so a reminder can be dealt with without opening the
 * app. A reminder you have to open the app to dismiss is a reminder people turn off.
 */
@AndroidEntryPoint
class ReminderActionReceiver : BroadcastReceiver() {

    @Inject lateinit var savedItemDao: SavedItemDao
    @Inject lateinit var reminderScheduler: ReminderScheduler

    companion object {
        const val ACTION_DONE = "com.tuck.app.action.REMINDER_DONE"
        const val ACTION_SNOOZE = "com.tuck.app.action.REMINDER_SNOOZE"
        const val EXTRA_ITEM_ID = "item_id"

        fun pendingIntent(context: Context, itemId: Long, action: String): PendingIntent {
            val intent = Intent(context, ReminderActionReceiver::class.java).apply {
                this.action = action
                putExtra(EXTRA_ITEM_ID, itemId)
            }
            return PendingIntent.getBroadcast(
                context,
                // Distinct request codes, or the two actions collide on one PendingIntent.
                (itemId.toInt() * 31) + action.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val itemId = intent.getLongExtra(EXTRA_ITEM_ID, -1L)
        if (itemId <= 0L) return

        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ACTION_DONE -> {
                        savedItemDao.setCompletedAt(itemId, System.currentTimeMillis())
                        savedItemDao.setRemindAt(itemId, null)
                        reminderScheduler.cancel(itemId)
                    }
                    ACTION_SNOOZE -> {
                        val next = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1)
                        savedItemDao.setRemindAt(itemId, next)
                        reminderScheduler.schedule(itemId, next)
                    }
                }
                (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                    .cancel(ReminderWorker.notificationIdFor(itemId))
            } finally {
                pending.finish()
            }
        }
    }
}
