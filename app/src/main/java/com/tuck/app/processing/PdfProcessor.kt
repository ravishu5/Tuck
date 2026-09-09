package com.tuck.app.processing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class PdfProcessor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val textRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    suspend fun extractText(pdfFilePath: String): String? = withContext(Dispatchers.Default) {
        val file = File(pdfFilePath)
        if (!file.exists()) return@withContext null

        // 1. Attempt raw text extraction from PDF stream
        val rawExtracted = extractRawStreamText(file)
        if (!rawExtracted.isNullOrBlank() && rawExtracted.length > 30) {
            return@withContext rawExtracted
        }

        // 2. Fallback: Render first 1-3 pages and run ML Kit OCR
        return@withContext extractTextViaPageOcr(file)
    }

    private fun extractRawStreamText(file: File): String? {
        return try {
            val content = FileInputStream(file).use { it.readBytes().toString(Charsets.ISO_8859_1) }
            val stringBuilder = StringBuilder()

            // Match text inside parentheses within Tj / TJ text operators in PDF streams
            val tjPattern = Pattern.compile("\\(([^\\)]+)\\)\\s*Tj", Pattern.DOTALL)
            val matcher = tjPattern.matcher(content)
            while (matcher.find()) {
                val match = matcher.group(1)
                if (!match.isNullOrBlank()) {
                    stringBuilder.append(cleanPdfText(match)).append(" ")
                }
            }

            // Also match array of strings in TJ operator: [(string) (string)] TJ
            val tjArrayPattern = Pattern.compile("\\[([^\\]]+)\\]\\s*TJ", Pattern.DOTALL)
            val arrayMatcher = tjArrayPattern.matcher(content)
            val innerStringPattern = Pattern.compile("\\(([^\\)]+)\\)")
            while (arrayMatcher.find()) {
                val arrayContent = arrayMatcher.group(1) ?: continue
                val innerMatcher = innerStringPattern.matcher(arrayContent)
                while (innerMatcher.find()) {
                    val part = innerMatcher.group(1)
                    if (!part.isNullOrBlank()) {
                        stringBuilder.append(cleanPdfText(part))
                    }
                }
                stringBuilder.append(" ")
            }

            val extracted = stringBuilder.toString().trim()
            if (extracted.length > 20) extracted else null
        } catch (e: Exception) {
            null
        }
    }

    private fun cleanPdfText(text: String): String {
        return text
            .replace("\\(", "(")
            .replace("\\)", ")")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\\\", "\\")
    }

    private suspend fun extractTextViaPageOcr(file: File): String? = withContext(Dispatchers.Default) {
        try {
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            val pageCount = renderer.pageCount.coerceAtMost(3) // Process up to 3 pages for preview/search
            val fullText = StringBuilder()

            for (i in 0 until pageCount) {
                val page = renderer.openPage(i)
                val width = (page.width * 1.5).toInt().coerceIn(600, 1600)
                val height = (page.height * 1.5).toInt().coerceIn(800, 2200)
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                val image = InputImage.fromBitmap(bitmap, 0)
                val pageOcr = suspendCancellableCoroutine<String?> { continuation ->
                    textRecognizer.process(image)
                        .addOnSuccessListener { visionText ->
                            bitmap.recycle()
                            continuation.resume(visionText.text)
                        }
                        .addOnFailureListener {
                            bitmap.recycle()
                            continuation.resume(null)
                        }
                }

                if (!pageOcr.isNullOrBlank()) {
                    fullText.append(pageOcr).append("\n\n")
                }
            }

            renderer.close()
            pfd.close()

            fullText.toString().trim().takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }
}
