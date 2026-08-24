package com.tuck.app.data.local.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.tuck.app.domain.model.ContentType
import com.tuck.app.domain.repository.StorageUsage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileStorageService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val imagesDir: File by lazy {
        File(context.filesDir, "saved/images").apply { mkdirs() }
    }

    private val pdfsDir: File by lazy {
        File(context.filesDir, "saved/pdfs").apply { mkdirs() }
    }

    private val documentsDir: File by lazy {
        File(context.filesDir, "saved/documents").apply { mkdirs() }
    }

    private val thumbnailsDir: File by lazy {
        File(context.filesDir, "thumbnails").apply { mkdirs() }
    }

    suspend fun saveImageFromUri(uri: Uri): StorageSaveResult = withContext(Dispatchers.IO) {
        val fileName = "img_${UUID.randomUUID()}.jpg"
        val destinationFile = File(imagesDir, fileName)

        var sha256 = ""
        context.contentResolver.openInputStream(uri)?.use { input ->
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(8192)
            var bytesRead: Int
            FileOutputStream(destinationFile).use { output ->
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    digest.update(buffer, 0, bytesRead)
                }
            }
            sha256 = digest.digest().joinToString("") { "%02x".format(it) }
        } ?: throw IllegalStateException("Cannot open input stream for Uri: $uri")

        // Generate thumbnail
        val thumbnailPath = generateImageThumbnail(destinationFile)

        StorageSaveResult(
            localFilePath = destinationFile.absolutePath,
            thumbnailPath = thumbnailPath,
            sha256 = sha256
        )
    }

    suspend fun savePdfFromUri(uri: Uri): StorageSaveResult = withContext(Dispatchers.IO) {
        val fileName = "doc_${UUID.randomUUID()}.pdf"
        val destinationFile = File(pdfsDir, fileName)

        var sha256 = ""
        context.contentResolver.openInputStream(uri)?.use { input ->
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(8192)
            var bytesRead: Int
            FileOutputStream(destinationFile).use { output ->
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    digest.update(buffer, 0, bytesRead)
                }
            }
            sha256 = digest.digest().joinToString("") { "%02x".format(it) }
        } ?: throw IllegalStateException("Cannot open input stream for Uri: $uri")

        // Generate first page PDF thumbnail
        val thumbnailPath = generatePdfThumbnail(destinationFile)

        StorageSaveResult(
            localFilePath = destinationFile.absolutePath,
            thumbnailPath = thumbnailPath,
            sha256 = sha256
        )
    }

    suspend fun saveDocumentFromUri(uri: Uri, originalExtension: String = "bin"): StorageSaveResult = withContext(Dispatchers.IO) {
        val fileName = "doc_${UUID.randomUUID()}.$originalExtension"
        val destinationFile = File(documentsDir, fileName)

        var sha256 = ""
        context.contentResolver.openInputStream(uri)?.use { input ->
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(8192)
            var bytesRead: Int
            FileOutputStream(destinationFile).use { output ->
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    digest.update(buffer, 0, bytesRead)
                }
            }
            sha256 = digest.digest().joinToString("") { "%02x".format(it) }
        } ?: throw IllegalStateException("Cannot open input stream for Uri: $uri")

        StorageSaveResult(
            localFilePath = destinationFile.absolutePath,
            thumbnailPath = null,
            sha256 = sha256
        )
    }

    suspend fun saveWebThumbnail(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        val thumbFile = File(thumbnailsDir, "thumb_${UUID.randomUUID()}.jpg")
        FileOutputStream(thumbFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
        thumbFile.absolutePath
    }

    suspend fun downloadAndSaveThumbnail(imageUrl: String): String? = withContext(Dispatchers.IO) {
        if (imageUrl.isBlank()) return@withContext null
        try {
            val cleanUrl = imageUrl.replace("&amp;", "&")
            val url = java.net.URL(cleanUrl)
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.instanceFollowRedirects = true
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            connection.setRequestProperty("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
            connection.connect()

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val bytes = connection.inputStream.use { it.readBytes() }
                if (bytes.isNotEmpty()) {
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bitmap != null) {
                        val thumbFile = File(thumbnailsDir, "thumb_${UUID.randomUUID()}.jpg")
                        FileOutputStream(thumbFile).use { out ->
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                        }
                        bitmap.recycle()
                        return@withContext thumbFile.absolutePath
                    }
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun generateImageThumbnail(sourceImage: File): String? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(sourceImage.absolutePath, options)

            val maxDimension = 512
            var inSampleSize = 1
            if (options.outHeight > maxDimension || options.outWidth > maxDimension) {
                val halfHeight = options.outHeight / 2
                val halfWidth = options.outWidth / 2
                while ((halfHeight / inSampleSize) >= maxDimension && (halfWidth / inSampleSize) >= maxDimension) {
                    inSampleSize *= 2
                }
            }

            options.inJustDecodeBounds = false
            options.inSampleSize = inSampleSize
            val decodedBitmap = BitmapFactory.decodeFile(sourceImage.absolutePath, options) ?: return null

            val thumbFile = File(thumbnailsDir, "thumb_${sourceImage.name}")
            FileOutputStream(thumbFile).use { out ->
                decodedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
            }
            decodedBitmap.recycle()
            thumbFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    private fun generatePdfThumbnail(pdfFile: File): String? {
        return try {
            val pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            if (renderer.pageCount > 0) {
                val page = renderer.openPage(0)
                val width = 400
                val height = (width.toFloat() * (page.height.toFloat() / page.width.toFloat())).toInt().coerceAtLeast(100)
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                renderer.close()
                pfd.close()

                val thumbFile = File(thumbnailsDir, "thumb_${pdfFile.nameWithoutExtension}.jpg")
                FileOutputStream(thumbFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }
                bitmap.recycle()
                thumbFile.absolutePath
            } else {
                renderer.close()
                pfd.close()
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun deleteFile(path: String?): Boolean = withContext(Dispatchers.IO) {
        if (path.isNullOrBlank()) return@withContext false
        try {
            val file = File(path)
            if (file.exists()) file.delete() else false
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getStorageUsage(): StorageUsage = withContext(Dispatchers.IO) {
        val imagesSize = getDirectorySize(imagesDir)
        val pdfsSize = getDirectorySize(pdfsDir)
        val thumbsSize = getDirectorySize(thumbnailsDir)
        val cacheSize = getDirectorySize(context.cacheDir)
        val total = imagesSize + pdfsSize + thumbsSize + cacheSize

        StorageUsage(
            imagesSizeBytes = imagesSize,
            pdfsSizeBytes = pdfsSize,
            thumbnailsSizeBytes = thumbsSize,
            cacheSizeBytes = cacheSize,
            totalSizeBytes = total
        )
    }

    suspend fun clearCache(): Boolean = withContext(Dispatchers.IO) {
        try {
            context.cacheDir.deleteRecursively()
            context.cacheDir.mkdirs()
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun getDirectorySize(dir: File): Long {
        if (!dir.exists()) return 0L
        var size = 0L
        dir.listFiles()?.forEach { file ->
            size += if (file.isDirectory) getDirectorySize(file) else file.length()
        }
        return size
    }
}

data class StorageSaveResult(
    val localFilePath: String,
    val thumbnailPath: String?,
    val sha256: String
)
