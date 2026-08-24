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

data class OcrBlockResult(
    val text: String,
    val confidence: Float = 1.0f,
    val bboxX: Float = 0f,
    val bboxY: Float = 0f,
    val bboxW: Float = 0f,
    val bboxH: Float = 0f,
    val blockIndex: Int = 0
)

data class OcrExtractionResult(
    val fullText: String?,
    val blocks: List<OcrBlockResult> = emptyList()
)

@Singleton
class ImageOcrProcessor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    suspend fun extractTextFromImageFile(imageFilePath: String): String? {
        return extractOcrBlocks(imageFilePath).fullText
    }

    suspend fun extractOcrBlocks(imageFilePath: String): OcrExtractionResult = withContext(Dispatchers.Default) {
        val file = File(imageFilePath)
        if (!file.exists() || file.length() == 0L) return@withContext OcrExtractionResult(null)

        try {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return@withContext OcrExtractionResult(null)
            val image = InputImage.fromBitmap(bitmap, 0)

            suspendCancellableCoroutine { continuation ->
                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        val fullText = visionText.text.trim().takeIf { it.isNotBlank() }
                        val blocks = visionText.textBlocks.mapIndexed { index, block ->
                            val box = block.boundingBox
                            OcrBlockResult(
                                text = block.text,
                                confidence = 1.0f,
                                bboxX = box?.left?.toFloat() ?: 0f,
                                bboxY = box?.top?.toFloat() ?: 0f,
                                bboxW = box?.width()?.toFloat() ?: 0f,
                                bboxH = box?.height()?.toFloat() ?: 0f,
                                blockIndex = index
                            )
                        }
                        bitmap.recycle()
                        if (continuation.isActive) {
                            continuation.resume(OcrExtractionResult(fullText, blocks))
                        }
                    }
                    .addOnFailureListener {
                        bitmap.recycle()
                        if (continuation.isActive) {
                            continuation.resume(OcrExtractionResult(null))
                        }
                    }
            }
        } catch (e: Exception) {
            OcrExtractionResult(null)
        }
    }
}
