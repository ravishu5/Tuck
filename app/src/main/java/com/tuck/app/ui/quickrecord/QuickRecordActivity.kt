package com.tuck.app.ui.quickrecord

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.tuck.app.R
import com.tuck.app.processing.QuickRecordController
import com.tuck.app.service.VoiceRecordingService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The whole Quick Memo experience: open, and you are already recording.
 *
 * Deliberately not the main app. It hosts no navigation graph, no feed, no search and no
 * view models beyond the one thing it does, because the number this screen is judged on is the
 * gap between tapping the tile and the microphone being live. Anything initialised on the way
 * is time spent not recording.
 *
 * It is also the single entry point the tile, the launcher shortcut and the notification all
 * route through, so there is one definition of what "quick record" means.
 */
@AndroidEntryPoint
class QuickRecordActivity : ComponentActivity() {

    @Inject lateinit var controller: QuickRecordController

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            beginRecording()
        } else {
            Toast.makeText(this, R.string.quick_record_needs_microphone, Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverLockScreen()

        setContent {
            QuickRecordScreen(
                controller = controller,
                onStop = ::stopAndSave,
                onCancel = ::cancelAndClose
            )
        }

        // Only on a genuinely new launch. On a recreation — a rotation, or the process coming
        // back — the recording is already running in the controller and starting again here
        // would be the second recorder this feature exists to avoid.
        if (savedInstanceState == null) ensureRecording()
    }

    /**
     * A second tile tap while recording should show the recording, not start another.
     *
     * The activity is `singleTask`, so a repeat launch arrives here rather than as a new
     * instance; [ensureRecording] is a no-op when the controller is already running.
     */
    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        ensureRecording()
    }

    private fun ensureRecording() {
        if (controller.isRecording) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) beginRecording() else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    private fun beginRecording() {
        lifecycleScope.launch {
            when (controller.start()) {
                QuickRecordController.StartResult.STARTED -> {
                    // Confirms the microphone is live without the reader having to look, which
                    // matters when the point of this screen is to start talking immediately.
                    QuickRecordHaptics.recordingStarted(this@QuickRecordActivity)
                    VoiceRecordingService.start(this@QuickRecordActivity)
                }

                QuickRecordController.StartResult.ALREADY_RECORDING -> Unit

                QuickRecordController.StartResult.FAILED -> {
                    Toast.makeText(
                        this@QuickRecordActivity,
                        R.string.quick_record_could_not_start,
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
            }
        }
    }

    private fun stopAndSave() {
        lifecycleScope.launch {
            val result = controller.stopAndSave()
            VoiceRecordingService.stop(this@QuickRecordActivity)

            val message = when (result) {
                is QuickRecordController.SaveResult.Saved -> {
                    QuickRecordHaptics.saved(this@QuickRecordActivity)
                    getString(R.string.quick_record_saved, result.title)
                }

                QuickRecordController.SaveResult.TooShort ->
                    getString(R.string.quick_record_too_short)

                is QuickRecordController.SaveResult.Failed ->
                    getString(R.string.quick_record_save_failed)

                QuickRecordController.SaveResult.NotRecording -> null
            }
            message?.let { Toast.makeText(this@QuickRecordActivity, it, Toast.LENGTH_SHORT).show() }
            finish()
        }
    }

    private fun cancelAndClose() {
        lifecycleScope.launch {
            controller.cancel()
            VoiceRecordingService.stop(this@QuickRecordActivity)
            finish()
        }
    }

    /**
     * Lets the memo be started without unlocking.
     *
     * The screen shows a timer and two buttons and nothing about what is in the vault, so
     * there is nothing here a lock screen ought to be hiding.
     */
    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
    }

    /**
     * Leaving the screen does not end the memo.
     *
     * The opposite of the main app's recorder, which cancels when its screen goes away. Here
     * backgrounding is a normal thing to do mid-recording — locking the phone and carrying on
     * talking — and the foreground service is what keeps it alive.
     */
    companion object {
        const val ACTION_QUICK_RECORD = "com.tuck.app.action.QUICK_RECORD"
    }
}
