package com.tuck.app.processing

import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.OutputStream
import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Turns a saved voice note into text, entirely on the device.
 *
 * **Why the platform recogniser rather than a bundled model.** Tuck's release APK already runs
 * 43.7 MB against a 25 MB budget (FUTURE_WORK.md), and the smallest credible offline models cost
 * more than the whole budget again: Vosk's small English model is 40 MB, and whisper.cpp's tiny
 * weights are in the same range before the native runtime is counted. Android's on-device
 * recogniser costs **zero bytes**, runs without a network, and is already on the device.
 *
 * The mechanism is `EXTRA_AUDIO_SOURCE`, which takes a file descriptor instead of opening the
 * microphone, paired with `EXTRA_SEGMENTED_SESSION` so the session ends exactly when the audio
 * does. Both arrived in API 33; below that this returns null and voice notes stay untranscribed
 * rather than degrading to a cloud call, which Product Law 6 rules out anyway.
 *
 * The one documented catch is that support is optional: *"if the recognizer does not support this
 * feature, the recognizer will open the mic"*. Recording the room instead of reading the file
 * would be worse than no transcript, so [transcribe] refuses to run unless an on-device engine
 * reports itself available, and treats a silent result as a failure rather than as an answer.
 */
@Singleton
class AudioTranscriber @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private companion object {
        /** Generous: recognition streams roughly in real time, and notes can run minutes. */
        const val TIMEOUT_MS = 5 * 60_000L
        const val DECODE_TIMEOUT_US = 10_000L
    }

    /**
     * Whether this device can transcribe at all.
     *
     * False on API < 33, and on any device without an on-device engine — a de-Googled build,
     * typically. Callers should treat it as "this feature is absent", not as an error.
     */
    fun isAvailable(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            SpeechRecognizer.isOnDeviceRecognitionAvailable(context)

    /**
     * Returns the transcript of [file], or null if it could not be produced.
     *
     * Never throws: a voice note that cannot be transcribed is still a voice note, and enrichment
     * must never fail the save (Product Law 2).
     */
    suspend fun transcribe(file: File, languageTag: String): String? {
        if (!isAvailable() || !file.exists() || file.length() == 0L) return null

        return try {
            withTimeoutOrNull(TIMEOUT_MS) { runRecognition(file, languageTag) }
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun runRecognition(file: File, languageTag: String): String? {
        val format = readAudioFormat(file) ?: return null
        val pipe = ParcelFileDescriptor.createPipe()
        val readSide = pipe[0]
        val writeSide = pipe[1]

        // Decoding runs off the main thread and the pipe applies the backpressure: the writer
        // blocks whenever the recogniser is behind, so a long note never buffers in memory.
        val decodeScope = CoroutineScope(Dispatchers.IO)
        decodeScope.launch {
            ParcelFileDescriptor.AutoCloseOutputStream(writeSide).use { out ->
                try {
                    decodeToPcm(file, out)
                } catch (e: Exception) {
                    // Closing the stream ends the session; a partial transcript is still useful.
                }
            }
        }

        return withContext(Dispatchers.Main) {
            // SpeechRecognizer is bound to the thread that creates it, and wants a Looper.
            val recognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
            try {
                awaitTranscript(recognizer, readSide, format, languageTag)
            } finally {
                recognizer.destroy()
                runCatching { readSide.close() }
            }
        }
    }

    private suspend fun awaitTranscript(
        recognizer: SpeechRecognizer,
        audio: ParcelFileDescriptor,
        format: PcmFormat,
        languageTag: String
    ): String? = suspendCancellableCoroutine { continuation ->
        val collected = StringBuilder()

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE, audio)
            putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_CHANNEL_COUNT, format.channels)
            putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_ENCODING, AudioFormat.ENCODING_PCM_16BIT)
            putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_SAMPLING_RATE, format.sampleRate)
            // Without this the session would wait for a silence the file never contains; here it
            // ends precisely when the pipe closes.
            putExtra(RecognizerIntent.EXTRA_SEGMENTED_SESSION, RecognizerIntent.EXTRA_AUDIO_SOURCE)
        }


        fun finish(result: String?) {
            if (continuation.isActive) continuation.resume(result)
        }

        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                append(collected, results)
                finish(collected.toString().trim().takeIf { it.isNotBlank() })
            }

            /** Segmented sessions emit each utterance here; the final callback closes the run. */
            override fun onSegmentResults(segment: Bundle) {
                append(collected, segment)
            }

            override fun onEndOfSegmentedSession() {
                finish(collected.toString().trim().takeIf { it.isNotBlank() })
            }

            override fun onError(error: Int) {
                // ERROR_LANGUAGE_UNAVAILABLE means the language is supported but its pack has
                // not been downloaded yet. Asking for it here turns a permanent failure into a
                // one-off: the next note recorded in this language transcribes normally.
                if (error == SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE) {
                    runCatching { recognizer.triggerModelDownload(intent) }
                }
                // Anything already recognised is worth keeping, even if the tail failed.
                finish(collected.toString().trim().takeIf { it.isNotBlank() })
            }

            override fun onPartialResults(partialResults: Bundle?) = Unit
            override fun onReadyForSpeech(params: Bundle?) = Unit
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })

        continuation.invokeOnCancellation { runCatching { recognizer.cancel() } }
        recognizer.startListening(intent)
    }

    private fun append(target: StringBuilder, bundle: Bundle?) {
        val text = bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()
            ?.trim()
            ?: return
        if (text.isBlank()) return
        if (target.isNotEmpty()) target.append(' ')
        target.append(text)
    }

    private data class PcmFormat(val sampleRate: Int, val channels: Int)

    /**
     * Reads the track's real sample rate and channel count.
     *
     * The recogniser is told the audio's actual shape rather than the file being resampled to
     * some canonical rate: resampling costs CPU and loses fidelity for no benefit when the API
     * accepts the format as a parameter.
     */
    private fun readAudioFormat(file: File): PcmFormat? {
        val extractor = MediaExtractor()
        return try {
            extractor.setDataSource(file.absolutePath)
            val track = audioTrackIndex(extractor) ?: return null
            val format = extractor.getTrackFormat(track)
            PcmFormat(
                sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE),
                channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            )
        } catch (e: Exception) {
            null
        } finally {
            extractor.release()
        }
    }

    private fun audioTrackIndex(extractor: MediaExtractor): Int? {
        for (i in 0 until extractor.trackCount) {
            val mime = extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME).orEmpty()
            if (mime.startsWith("audio/")) return i
        }
        return null
    }

    /** Decodes the container's AAC into raw 16-bit PCM, which is what the recogniser reads. */
    private fun decodeToPcm(file: File, out: OutputStream) {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(file.absolutePath)
            val track = audioTrackIndex(extractor) ?: return
            extractor.selectTrack(track)

            val format = extractor.getTrackFormat(track)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: return
            codec = MediaCodec.createDecoderByType(mime).apply {
                configure(format, null, null, 0)
                start()
            }

            val info = MediaCodec.BufferInfo()
            var sawInputEnd = false
            var sawOutputEnd = false

            while (!sawOutputEnd) {
                if (!sawInputEnd) {
                    val inputIndex = codec.dequeueInputBuffer(DECODE_TIMEOUT_US)
                    if (inputIndex >= 0) {
                        val buffer = codec.getInputBuffer(inputIndex) ?: ByteBuffer.allocate(0)
                        val size = extractor.readSampleData(buffer, 0)
                        if (size < 0) {
                            codec.queueInputBuffer(
                                inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            sawInputEnd = true
                        } else {
                            codec.queueInputBuffer(inputIndex, 0, size, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                val outputIndex = codec.dequeueOutputBuffer(info, DECODE_TIMEOUT_US)
                if (outputIndex >= 0) {
                    val buffer = codec.getOutputBuffer(outputIndex)
                    if (buffer != null && info.size > 0) {
                        val chunk = ByteArray(info.size)
                        buffer.position(info.offset)
                        buffer.get(chunk, 0, info.size)
                        out.write(chunk)
                    }
                    codec.releaseOutputBuffer(outputIndex, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) sawOutputEnd = true
                } else if (outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER && sawInputEnd) {
                    // The decoder has nothing left to give and no more input is coming.
                    sawOutputEnd = true
                }
            }
            out.flush()
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            extractor.release()
        }
    }
}
