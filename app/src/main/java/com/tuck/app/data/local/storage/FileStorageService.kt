package com.tuck.app.data.local.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.exifinterface.media.ExifInterface
import com.tuck.app.domain.model.ContentType
import com.tuck.app.domain.repository.StorageUsage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
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

    private fun takePersistableUriPermissionSafely(uri: Uri) {
        if (uri.scheme == "content") {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (ignored: SecurityException) {
                // Non-persistable providers
            }
        }
    }


    /**
     * Resolves the real file extension instead of assuming one.
     *
     * Every image was previously written as `.jpg` and every video as `.mp4`, whatever
     * the bytes actually were. The bytes were always correct - the copy is a verbatim
     * stream - but a PNG named `.jpg` misleads FileProvider's mime inference, external
     * viewers, and export.
     */
    private fun resolveExtension(uri: Uri, mimeType: String?, fallback: String): String {
        MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)?.let { return it }

        displayNameOf(uri)?.substringAfterLast('.', "")?.takeIf { it.isNotBlank() && it.length <= 5 }
            ?.let { return it.lowercase() }

        return fallback
    }

    /**
     * The content resolver is authoritative for `content://`, but returns null for
     * `file://` - so the caller's hint and then the filename have to fill in, otherwise
     * every shared file falls back to a guess.
     */
    private fun resolveMime(uri: Uri, hint: String?): String? =
        context.contentResolver.getType(uri)
            ?: hint
            ?: displayNameOf(uri)?.substringAfterLast('.', "")?.takeIf { it.isNotBlank() }
                ?.let { MimeTypeMap.getSingleton().getMimeTypeFromExtension(it.lowercase()) }

    private fun displayNameOf(uri: Uri): String? = try {
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                }
        } else {
            uri.lastPathSegment
        }
    } catch (e: Exception) {
        null
    }

    /**
     * When the content was originally created, as opposed to when Tuck saved it.
     *
     * A photo taken in 2019 and shared today should not read as if it is from today -
     * losing this was the reason one competitor's users refused to delete their originals.
     */
    private fun readCapturedAt(file: File, mimeType: String?): Long? = try {
        if (mimeType?.startsWith("image/") == true) {
            val exif = ExifInterface(file.absolutePath)
            val raw = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
            raw?.let {
                java.text.SimpleDateFormat("yyyy:MM:dd HH:mm:ss", java.util.Locale.US)
                    .parse(it)?.time
            }
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }

    /** Intrinsic dimensions, so the detail screen can show real values instead of zeros. */
    private fun readDimensions(file: File, mimeType: String?): Pair<Int, Int> = try {
        if (mimeType?.startsWith("image/") == true) {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, options)
            options.outWidth.coerceAtLeast(0) to options.outHeight.coerceAtLeast(0)
        } else {
            0 to 0
        }
    } catch (e: Exception) {
        0 to 0
    }

    private fun readDurationMs(file: File, mimeType: String?): Long = try {
        if (mimeType?.startsWith("video/") == true || mimeType?.startsWith("audio/") == true) {
            android.media.MediaMetadataRetriever().use { retriever ->
                retriever.setDataSource(file.absolutePath)
                retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: 0L
            }
        } else {
            0L
        }
    } catch (e: Exception) {
        0L
    }

    private fun copyStream(uri: Uri, destinationFile: File): String {
        var sha256 = ""
        openStreamSafely(uri)?.use { input ->
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(16384)
            var bytesRead: Int
            FileOutputStream(destinationFile).use { output ->
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    digest.update(buffer, 0, bytesRead)
                }
            }
            sha256 = digest.digest().joinToString("") { "%02x".format(it) }
        } ?: throw IllegalStateException("Cannot open input stream for Uri: $uri")
        return sha256
    }

    suspend fun saveStreamUri(uri: Uri, mimeType: String? = null): StorageSaveResult = withContext(Dispatchers.IO) {
        takePersistableUriPermissionSafely(uri)
        val resolvedMime = (resolveMime(uri, mimeType) ?: "application/octet-stream").lowercase()

        when {
            resolvedMime.startsWith("image/") -> saveImageFromUri(uri, resolvedMime)
            resolvedMime == "application/pdf" -> savePdfFromUri(uri)
            else -> {
                val fallback = when {
                    resolvedMime.contains("vcard") -> "vcf"
                    resolvedMime.startsWith("video/") -> "mp4"
                    resolvedMime.startsWith("audio/") -> "mp3"
                    resolvedMime.contains("json") -> "json"
                    resolvedMime.contains("text/plain") -> "txt"
                    else -> "bin"
                }
                saveDocumentFromUri(uri, resolveExtension(uri, resolvedMime, fallback), resolvedMime)
            }
        }
    }

    suspend fun saveAllStreamsFromUris(uris: List<Uri>, mimeType: String? = null): List<StorageSaveResult> = withContext(Dispatchers.IO) {
        uris.map { uri ->
            saveStreamUri(uri, mimeType)
        }
    }

    private fun openStreamSafely(uri: Uri): InputStream? {
        return try {
            if (uri.scheme == "file" && uri.path != null) {
                val file = File(uri.path!!)
                if (file.exists() && file.canRead()) {
                    FileInputStream(file)
                } else {
                    context.contentResolver.openInputStream(uri)
                }
            } else {
                context.contentResolver.openInputStream(uri)
            }
        } catch (e: Exception) {
            try {
                if (uri.path != null) {
                    val file = File(uri.path!!)
                    if (file.exists()) FileInputStream(file) else null
                } else null
            } catch (e2: Exception) {
                null
            }
        }
    }

    suspend fun saveImageFromUri(uri: Uri, mimeHint: String? = null): StorageSaveResult = withContext(Dispatchers.IO) {
        takePersistableUriPermissionSafely(uri)
        val mime = resolveMime(uri, mimeHint) ?: "image/jpeg"
        val fileName = "img_${UUID.randomUUID()}.${resolveExtension(uri, mime, "jpg")}"
        val destinationFile = File(imagesDir, fileName)

        val sha256 = copyStream(uri, destinationFile)
        val (width, height) = readDimensions(destinationFile, mime)

        StorageSaveResult(
            localFilePath = destinationFile.absolutePath,
            thumbnailPath = generateImageThumbnail(destinationFile),
            sha256 = sha256,
            mimeType = mime,
            sizeBytes = destinationFile.length(),
            width = width,
            height = height,
            capturedAt = readCapturedAt(destinationFile, mime)
        )
    }

    suspend fun savePdfFromUri(uri: Uri): StorageSaveResult = withContext(Dispatchers.IO) {
        takePersistableUriPermissionSafely(uri)
        val destinationFile = File(pdfsDir, "doc_${UUID.randomUUID()}.pdf")

        val sha256 = copyStream(uri, destinationFile)

        StorageSaveResult(
            localFilePath = destinationFile.absolutePath,
            thumbnailPath = generatePdfThumbnail(destinationFile),
            sha256 = sha256,
            mimeType = "application/pdf",
            sizeBytes = destinationFile.length()
        )
    }

    suspend fun saveDocumentFromUri(
        uri: Uri,
        originalExtension: String = "bin",
        mimeType: String? = null
    ): StorageSaveResult = withContext(Dispatchers.IO) {
        takePersistableUriPermissionSafely(uri)
        val destinationFile = File(documentsDir, "doc_${UUID.randomUUID()}.$originalExtension")

        val sha256 = copyStream(uri, destinationFile)
        val resolvedMime = resolveMime(uri, mimeType)

        StorageSaveResult(
            localFilePath = destinationFile.absolutePath,
            thumbnailPath = null,
            sha256 = sha256,
            mimeType = resolvedMime,
            sizeBytes = destinationFile.length(),
            durationMs = readDurationMs(destinationFile, resolvedMime)
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

    suspend fun downloadAndCacheImage(imageUrl: String): String? = withContext(Dispatchers.IO) {
        if (imageUrl.isBlank()) return@withContext null
        try {
            val url = java.net.URL(imageUrl)
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.connectTimeout = 6000
            conn.readTimeout = 6000
            conn.instanceFollowRedirects = true
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile)")
            conn.connect()

            if (conn.responseCode in 200..299) {
                val fileName = "thumb_${UUID.randomUUID()}.jpg"
                val thumbFile = File(thumbnailsDir, fileName)
                conn.inputStream.use { input ->
                    FileOutputStream(thumbFile).use { output ->
                        input.copyTo(output)
                    }
                }
                if (thumbFile.exists() && thumbFile.length() > 0) {
                    thumbFile.absolutePath
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Every file Tuck has written, so the health check can spot files no database row
     * points at any more. Thumbnails are excluded: they are regenerable and are keyed to
     * their source rather than referenced directly by an item.
     */
    suspend fun listStoredFiles(): List<File> = withContext(Dispatchers.IO) {
        listOf(imagesDir, pdfsDir, documentsDir)
            .flatMap { it.listFiles()?.toList().orEmpty() }
            .filter { it.isFile }
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
        val documentsSize = getDirectorySize(documentsDir)
        val thumbsSize = getDirectorySize(thumbnailsDir)
        val cacheSize = getDirectorySize(context.cacheDir)

        StorageUsage(
            imagesSizeBytes = imagesSize,
            pdfsSizeBytes = pdfsSize,
            documentsSizeBytes = documentsSize,
            thumbnailsSizeBytes = thumbsSize,
            cacheSizeBytes = cacheSize,
            totalSizeBytes = imagesSize + pdfsSize + documentsSize + thumbsSize + cacheSize
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

    /**
     * Frees everything regenerable: cached previews and thumbnails.
     *
     * Never touches images, PDFs or documents - those are the saves. Thumbnails are
     * rebuilt on next display, so the only cost is a moment of re-decoding.
     */
    suspend fun reclaimSpace(): Long = withContext(Dispatchers.IO) {
        val before = getDirectorySize(thumbnailsDir) + getDirectorySize(context.cacheDir)
        try {
            thumbnailsDir.listFiles()?.forEach { it.delete() }
            context.cacheDir.deleteRecursively()
            context.cacheDir.mkdirs()
        } catch (ignored: Exception) {
            // Partial reclaim is still a reclaim; report what actually went.
        }
        val after = getDirectorySize(thumbnailsDir) + getDirectorySize(context.cacheDir)
        (before - after).coerceAtLeast(0L)
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
    val sha256: String,
    val mimeType: String? = null,
    val sizeBytes: Long = 0L,
    val width: Int = 0,
    val height: Int = 0,
    val durationMs: Long = 0L,
    /** Original creation time of the content, distinct from when Tuck saved it. */
    val capturedAt: Long? = null
)
