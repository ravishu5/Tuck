package com.tuck.app.processing

import android.content.Context
import android.graphics.BitmapFactory
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class ImageOcrProcessor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    suspend fun extractTextFromImageFile(imageFilePath: String): String? = withContext(Dispatchers.Default) {
        val file = File(imageFilePath)
        if (!file.exists() || file.length() == 0L) return@withContext null

        try {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return@withContext null
            val image = InputImage.fromBitmap(bitmap, 0)

            suspendCancellableCoroutine { continuation ->
                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        val result = visionText.text.trim().takeIf { it.isNotBlank() }
                        bitmap.recycle()
                        if (continuation.isActive) {
                            continuation.resume(result)
                        }
                    }
                    .addOnFailureListener {
                        bitmap.recycle()
                        if (continuation.isActive) {
                            continuation.resume(null)
                        }
                    }
            }
        } catch (e: Exception) {
            null
        }
    }
}
