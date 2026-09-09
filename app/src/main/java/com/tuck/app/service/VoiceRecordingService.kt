package com.tuck.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.tuck.app.R
import com.tuck.app.processing.QuickRecordController
import com.tuck.app.ui.quickrecord.QuickRecordActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Keeps the microphone open while nobody is looking at the screen.
 *
 * From Android 12 an app that is not in the foreground loses microphone access, so a recording
 * would die the moment the reader locked the phone or swiped away — which is exactly when a
 * voice memo is most likely still running. A foreground service is what buys the permission to
 * carry on, and its notification is where the recording stays visible and stoppable.
 *
 * It does not own the recorder. [QuickRecordController] does, so that a memo started from the
 * app and one started from the tile are the same recording, and stopping from the notification
 * takes exactly the same path as stopping from the screen.
 */
@AndroidEntryPoint
class VoiceRecordingService : Service() {

    @Inject lateinit var controller: QuickRecordController

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_AND_SAVE -> {
                scope.launch {
                    controller.stopAndSave()
                    stopSelf()
                }
                // Still promote first: a service that never calls startForeground after being
                // started as one is killed with an ANR, even if it is stopping immediately.
                promote()
                return START_NOT_STICKY
            }

            else -> promote()
        }
        // Not sticky: a recording cannot be resumed after the process dies, and restarting the
        // service without one would leave a notification for a memo nobody is recording.
        return START_NOT_STICKY
    }

    private fun promote() {
        createChannel()
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(): Notification {
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, QuickRecordActivity::class.java).apply {
                action = QuickRecordActivity.ACTION_QUICK_RECORD
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stop = PendingIntent.getService(
            this,
            1,
            Intent(this, VoiceRecordingService::class.java).setAction(ACTION_STOP_AND_SAVE),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle(getString(R.string.quick_record_notification_title))
            .setContentText(getString(R.string.quick_record_notification_text))
            .setContentIntent(open)
            .addAction(
                android.R.drawable.ic_menu_save,
                getString(R.string.quick_record_stop_save),
                stop
            )
            .setOngoing(true)
            .setSilent(true)
            // The memo's contents are not in the notification, but the fact of recording is
            // shown on the lock screen so it can always be stopped from there.
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.quick_record_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.quick_record_channel_description)
                setShowBadge(false)
            }
        )
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_STOP_AND_SAVE = "com.tuck.app.action.STOP_AND_SAVE_MEMO"
        private const val CHANNEL_ID = "tuck_voice_recording"
        private const val NOTIFICATION_ID = 4711

        fun start(context: Context) {
            val intent = Intent(context, VoiceRecordingService::class.java)
            // A microphone service may only be started while the app is allowed the microphone.
            // If the platform refuses, the recording still runs for as long as the screen is
            // up; it is the background case that is lost, and losing it is better than dying.
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }
        }

        fun stop(context: Context) {
            runCatching {
                context.stopService(Intent(context, VoiceRecordingService::class.java))
            }
        }
    }
}
