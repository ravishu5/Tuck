package com.tuck.app.ui.quickrecord

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * The two moments Quick Memo confirms by feel rather than by sight.
 *
 * Both are cases where the reader's eyes are somewhere else — that is the whole reason the
 * feature exists — so a tick in the hand is what tells them the microphone opened and that the
 * memo is safely filed.
 */
internal object QuickRecordHaptics {

    fun recordingStarted(context: Context) = vibrate(context, START_MS)

    /** Two short taps, so "saved" is distinguishable from "started" without looking. */
    fun saved(context: Context) = vibratePattern(context, SAVED_PATTERN)

    private fun vibrate(context: Context, millis: Long) {
        val vibrator = vibrator(context) ?: return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(millis)
            }
        }
    }

    private fun vibratePattern(context: Context, pattern: LongArray) {
        val vibrator = vibrator(context) ?: return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
        }
    }

    private fun vibrator(context: Context): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)
                ?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

    private const val START_MS = 30L
    private val SAVED_PATTERN = longArrayOf(0, 25, 70, 25)
}
