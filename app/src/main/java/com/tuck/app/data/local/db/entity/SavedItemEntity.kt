package com.tuck.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tuck.app.domain.model.ContentType
import com.tuck.app.domain.model.ProcessingStatus

@Entity(
    tableName = "saved_items",
    indices = [
        Index(value = ["createdAt"]),
        Index(value = ["capturedAt"]),
        Index(value = ["remindAt"]),
        Index(value = ["completedAt"]),
        Index(value = ["contentType"]),
        Index(value = ["sourceDomain"]),
        Index(value = ["processingStatus"]),
        Index(value = ["isFavorite"]),
        Index(value = ["isPinned"]),
        Index(value = ["isDeleted"]),
        Index(value = ["canonicalUrl"]),
        Index(value = ["textHash"]),
        Index(value = ["imageSha256"]),
        Index(value = ["dedupeGroupId"])
    ]
)
data class SavedItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val contentType: ContentType,
    val title: String,
    val titleIsUserEdited: Boolean = false,
    val description: String? = null,
    val originalUrl: String? = null,
    val canonicalUrl: String? = null,
    val sourceDomain: String? = null,
    val sourceApp: String? = null,
    val mimeType: String? = null,
    val localFilePath: String? = null,
    val thumbnailPath: String? = null,
    val originalText: String? = null,
    val extractedText: String? = null,
    val ocrText: String? = null,
    val commentsJson: String? = null,
    /** When the content itself was created - EXIF/gallery date - as opposed to when Tuck saved it. */
    val capturedAt: Long? = null,
    /** When to surface this item again. Null means no reminder is set. */
    val remindAt: Long? = null,
    /** Why the reminder was set; cleared with it. */
    val remindNote: String? = null,
    /** When the user marked this acted-on. Null means it is still outstanding. */
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastOpenedAt: Long? = null,
    val openCount: Int = 0,
    val isPinned: Boolean = false,
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val processingStatus: ProcessingStatus = ProcessingStatus.PENDING,
    val captureNote: String? = null,
    val userNote: String? = null,
    val dedupeGroupId: String? = null,
    val textHash: String? = null,
    val imageSha256: String? = null
)
