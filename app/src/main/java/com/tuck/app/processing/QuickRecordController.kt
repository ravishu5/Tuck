package com.tuck.app.processing

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.tuck.app.domain.model.ContentType
import com.tuck.app.domain.model.SavedItem
import com.tuck.app.domain.repository.SavedItemRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The one place a voice memo is started, stopped and saved.
 *
 * Every way into recording — the Home button, the Quick Settings tile, the launcher shortcut,
 * the notification's stop action — goes through this. That is not tidiness for its own sake:
 * the microphone is a single resource, and two entry points each holding their own recorder is
 * how you get a tile tap that silently orphans the recording already in progress.
 *
 * It is a singleton, so its state outlives any one screen. A recording survives the Quick
 * Record activity being destroyed, which is the whole point of being able to lock the phone
 * mid-sentence.
 */
@Singleton
class QuickRecordController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val recorder: VoiceNoteRecorder,
    private val savedItemRepository: SavedItemRepository
) {

    sealed interface State {
        data object Idle : State

        /** [startedAt] is when the microphone opened, and is what names the memo. */
        data class Recording(val startedAt: Long) : State
    }

    /** Why a start did not happen, so the caller can say something true about it. */
    enum class StartResult {
        STARTED,

        /** Already recording. Deliberately not an error: a second tile tap should be a no-op. */
        ALREADY_RECORDING,

        /** The recorder would not open — no microphone, or another app holds it. */
        FAILED
    }

    sealed interface SaveResult {
        data class Saved(val itemId: Long, val title: String) : SaveResult

        /** Stopped, but there was nothing worth keeping — a mis-tap rather than a failure. */
        data object TooShort : SaveResult

        /** The audio existed and could not be filed. The file is left on disk. */
        data class Failed(val recoveredFilePath: String?) : SaveResult

        data object NotRecording : SaveResult
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    val isRecording: Boolean get() = _state.value is State.Recording

    /**
     * Serialises start/stop/cancel against each other.
     *
     * The entry points fire from different places at once — a tile tap while the activity is
     * still starting, a notification action racing the Stop button — and interleaving two of
     * these would either leak a recorder or save the same audio twice.
     */
    private val mutex = Mutex()

    /** Loudness for the waveform, 0f..1f. Cheap enough to poll on a frame timer. */
    fun amplitude(): Float = recorder.amplitude()

    suspend fun start(): StartResult = mutex.withLock {
        if (recorder.isRecording) {
            // Re-sync in case the process was restored with a live recorder but stale state.
            recorder.startedAtMillis?.let { _state.value = State.Recording(it) }
            return@withLock StartResult.ALREADY_RECORDING
        }
        if (!recorder.start()) {
            _state.value = State.Idle
            return@withLock StartResult.FAILED
        }
        _state.value = State.Recording(recorder.startedAtMillis ?: System.currentTimeMillis())
        StartResult.STARTED
    }

    /**
     * Stops, names and files the memo.
     *
     * The name is built from when the recording *began*, not from now: a memo is of the moment
     * it captured, and taking the clock at save time would file a long recording under the
     * minute it happened to finish in.
     */
    suspend fun stopAndSave(): SaveResult = mutex.withLock {
        val startedAt = (_state.value as? State.Recording)?.startedAt
        if (!recorder.isRecording) {
            _state.value = State.Idle
            return@withLock SaveResult.NotRecording
        }

        val note = recorder.stop()
        _state.value = State.Idle
        if (note == null) return@withLock SaveResult.TooShort

        val createdAt = startedAt ?: (System.currentTimeMillis() - note.durationMs)
        val title = uniqueTitleFor(createdAt)

        return@withLock try {
            val id = savedItemRepository.insertItem(
                SavedItem(
                    contentType = ContentType.AUDIO,
                    title = title,
                    localFilePath = note.file.absolutePath,
                    mimeType = "audio/mp4",
                    sourceApp = "Tuck",
                    capturedAt = createdAt
                )
            )
            if (id > 0) {
                WorkManager.getInstance(context).enqueue(
                    OneTimeWorkRequestBuilder<ItemProcessingWorker>()
                        .setInputData(workDataOf(ItemProcessingWorker.KEY_ITEM_ID to id))
                        .build()
                )
                SaveResult.Saved(itemId = id, title = title)
            } else {
                SaveResult.Failed(note.file.absolutePath)
            }
        } catch (e: Exception) {
            // The audio is real and finished. Whatever went wrong filing it, the file stays on
            // disk rather than being deleted — a recording that succeeded is never thrown away
            // because the database was busy or full.
            SaveResult.Failed(note.file.absolutePath)
        }
    }

    /** Abandons the recording and removes its file. Safe to call when nothing is recording. */
    suspend fun cancel() = mutex.withLock {
        recorder.cancel()
        _state.value = State.Idle
    }

    /**
     * `09 Sep 2026, 12:45 PM`, with a counter appended when that minute is already taken.
     *
     * Two memos started in the same minute would otherwise be indistinguishable in the list,
     * which matters precisely because the point of these is that nobody stopped to name them.
     */
    private suspend fun uniqueTitleFor(createdAt: Long): String {
        val base = TITLE_FORMAT.format(Date(createdAt))
        if (!savedItemRepository.titleExists(base)) return base
        // Two is where a person starts counting; the first one keeps its plain name.
        for (n in 2..MAX_TITLE_ATTEMPTS) {
            val candidate = "$base ($n)"
            if (!savedItemRepository.titleExists(candidate)) return candidate
        }
        // Beyond that, seconds are certain to separate them.
        return "$base (${SECONDS_FORMAT.format(Date(createdAt))})"
    }

    private companion object {
        /** Fixed to a stable locale so the pattern does not reorder between devices. */
        val TITLE_FORMAT = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val SECONDS_FORMAT = SimpleDateFormat("ss", Locale.getDefault())
        const val MAX_TITLE_ATTEMPTS = 20
    }
}
