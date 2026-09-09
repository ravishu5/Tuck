package com.tuck.app.processing

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.tuck.app.data.local.db.dao.SavedItemDao
import com.tuck.app.data.local.db.entity.SavedItemEntity
import com.tuck.app.data.local.storage.FileStorageService
import com.tuck.app.domain.model.ContentType
import com.tuck.app.domain.model.ProcessingStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class GalleryScreenshot(
    val id: Long,
    val contentUri: Uri,
    val displayName: String,
    val dateAdded: Long,
    val size: Long,
    val isAlreadyImported: Boolean = false
)

@Singleton
class ScreenshotImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val savedItemDao: SavedItemDao,
    private val fileStorageService: FileStorageService,
    private val duplicateDetector: DuplicateDetector
) {

    suspend fun getRecentScreenshots(limit: Int = 50): List<GalleryScreenshot> = withContext(Dispatchers.IO) {
        val screenshots = mutableListOf<GalleryScreenshot>()
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.SIZE,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Images.Media.RELATIVE_PATH else MediaStore.Images.Media.DATA
        )

        val selection = buildString {
            append("(")
            append("${MediaStore.Images.Media.DISPLAY_NAME} LIKE '%Screenshot%'")
            append(" OR ${MediaStore.Images.Media.DISPLAY_NAME} LIKE '%screenshot%'")
            append(" OR ${MediaStore.Images.Media.DISPLAY_NAME} LIKE '%ScreenShot%'")
            append(" OR ${MediaStore.Images.Media.DISPLAY_NAME} LIKE '%Capture%'")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                append(" OR ${MediaStore.Images.Media.RELATIVE_PATH} LIKE '%Screenshots%'")
                append(" OR ${MediaStore.Images.Media.RELATIVE_PATH} LIKE '%DCIM/Screenshots%'")
            }
            append(")")
            append(" AND ${MediaStore.Images.Media.MIME_TYPE} LIKE 'image/%'")
        }

        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        try {
            val cursor: Cursor? = context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )

            cursor?.use {
                val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val dateColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                val sizeColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)

                var count = 0
                while (it.moveToNext() && count < limit) {
                    val id = it.getLong(idColumn)
                    val name = it.getString(nameColumn) ?: "Screenshot"
                    val date = it.getLong(dateColumn) * 1000L
                    val size = it.getLong(sizeColumn)
                    val contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

                    screenshots.add(
                        GalleryScreenshot(
                            id = id,
                            contentUri = contentUri,
                            displayName = name,
                            dateAdded = date,
                            size = size
                        )
                    )
                    count++
                }
            }
        } catch (e: Exception) {
            // Permission or querying exception fallback
        }

        screenshots
    }

    suspend fun importScreenshot(screenshot: GalleryScreenshot): Long = withContext(Dispatchers.IO) {
        val saveResult = fileStorageService.saveImageFromUri(screenshot.contentUri)
        val duplicate = savedItemDao.findByImageHash(saveResult.sha256)
        if (duplicate != null) {
            // Delete the copied file if it's already in the vault
            saveResult.localFilePath?.let { File(it).delete() }
            return@withContext duplicate.id
        }

        val entity = SavedItemEntity(
            contentType = ContentType.IMAGE,
            title = screenshot.displayName.substringBeforeLast('.').replace('_', ' '),
            localFilePath = saveResult.localFilePath,
            thumbnailPath = saveResult.thumbnailPath,
            imageSha256 = saveResult.sha256,
            // Previously hardcoded to image/png, which mislabelled every JPEG in the gallery.
            mimeType = saveResult.mimeType ?: "image/*",
            sourceDomain = "Gallery Screenshots",
            sourceApp = "Camera Roll / Screenshots",
            processingStatus = ProcessingStatus.PENDING,
            capturedAt = saveResult.capturedAt ?: screenshot.dateAdded.takeIf { it > 0 },
            createdAt = screenshot.dateAdded.takeIf { it > 0 } ?: System.currentTimeMillis()
        )

        savedItemDao.insert(entity)
    }

    suspend fun importAllScreenshots(screenshots: List<GalleryScreenshot>): List<Long> = withContext(Dispatchers.IO) {
        val importedIds = mutableListOf<Long>()
        for (screenshot in screenshots) {
            val id = importScreenshot(screenshot)
            if (id > 0) {
                importedIds.add(id)
            }
        }
        importedIds
    }
}
