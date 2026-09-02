package com.tuck.app.processing

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import com.tuck.app.data.local.storage.FileStorageService
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Records a voice note straight into Tuck.
 *
 * The fastest capture there is when hands or eyes are busy, and the one thing the share
 * sheet cannot give us - nothing else on the phone is holding a recording to share.
 *
 * AAC in an MP4 container: playable everywhere, and small enough that a long note costs
 * about a megabyte a minute.
 */
@Singleton
class VoiceNoteRecorder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fileStorageService: FileStorageService
) {
    private var recorder: MediaRecorder? = null
    private var target: File? = null
    private var startedAt: Long = 0L

    val isRecording: Boolean get() = recorder != null

    /** Returns false if the recorder could not start, leaving nothing half-created. */
    fun start(): Boolean {
        if (isRecording) return false
        val file = fileStorageService.newVoiceNoteFile()

        return try {
            val created = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(96_000)
                setAudioSamplingRate(44_100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            recorder = created
            target = file
            startedAt = System.currentTimeMillis()
            true
        } catch (e: Exception) {
            file.delete()
            recorder = null
            target = null
            false
        }
    }

    /**
     * Stops and returns the finished recording, or null if it failed or was too short to
     * be worth keeping - a mis-tap should not leave a half-second file in the vault.
     */
    fun stop(): VoiceNote? {
        val active = recorder ?: return null
        val file = target
        val durationMs = System.currentTimeMillis() - startedAt

        return try {
            active.stop()
            active.release()
            recorder = null
            target = null

            if (file == null || !file.exists() || durationMs < MIN_DURATION_MS) {
                file?.delete()
                null
            } else {
                VoiceNote(file = file, durationMs = durationMs)
            }
        } catch (e: Exception) {
            // stop() throws when nothing was captured; the partial file is unusable.
            runCatching { active.release() }
            recorder = null
            target = null
            file?.delete()
            null
        }
    }

    /** Abandons the recording and removes the partial file. */
    fun cancel() {
        val active = recorder ?: return
        runCatching { active.stop() }
        runCatching { active.release() }
        target?.delete()
        recorder = null
        target = null
    }

    data class VoiceNote(val file: File, val durationMs: Long)

    private companion object {
        const val MIN_DURATION_MS = 700L
    }
}
