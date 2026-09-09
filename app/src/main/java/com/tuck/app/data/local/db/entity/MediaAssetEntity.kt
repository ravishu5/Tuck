package com.tuck.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "media_assets",
    foreignKeys = [
        ForeignKey(
            entity = SavedItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["itemId"]),
        Index(value = ["role"]),
        Index(value = ["sha256"])
    ]
)
data class MediaAssetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val itemId: Long,
    val role: String = "PRIMARY", // PRIMARY, THUMBNAIL, GALLERY, ATTACHMENT, POSTER, AUDIO
    val localPath: String,
    val thumbnailPath: String? = null,
    val mimeType: String? = null,
    val bytes: Long = 0,
    val width: Int = 0,
    val height: Int = 0,
    val durationMs: Long = 0,
    val sha256: String? = null,
    val ordinal: Int = 0,
    val downloadState: String = "COMPLETE", // COMPLETE, DOWNLOADING, FAILED, COPYING
    val createdAt: Long = System.currentTimeMillis()
)
