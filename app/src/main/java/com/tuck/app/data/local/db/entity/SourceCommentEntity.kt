package com.tuck.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "source_comments",
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
        Index(value = ["parentCommentId"]),
        Index(value = ["path"]),
        Index(value = ["score"])
    ]
)
data class SourceCommentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val itemId: Long,
    val platformCommentId: String? = null,
    val parentCommentId: String? = null,
    val depth: Int = 0,
    val path: String = "0001", // Materialized path, e.g., "0001.0002"
    val authorHandle: String? = null,
    val bodyText: String,
    val bodyHtml: String? = null,
    val score: Int = 0,
    val postedAt: Long? = null,
    val isOp: Boolean = false,
    val isStickied: Boolean = false,
    val childCount: Int = 0,
    val ordinal: Int = 0
)
