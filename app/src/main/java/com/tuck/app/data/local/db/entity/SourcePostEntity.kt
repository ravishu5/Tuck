package com.tuck.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "source_posts",
    foreignKeys = [
        ForeignKey(
            entity = SavedItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["platform"]),
        Index(value = ["community"]),
        Index(value = ["platformPostId"])
    ]
)
data class SourcePostEntity(
    @PrimaryKey
    val itemId: Long,
    val platform: String = "WEB", // REDDIT, YOUTUBE, TWITTER, INSTAGRAM, TIKTOK, LINKEDIN, WEB
    val platformPostId: String? = null,
    val community: String? = null, // subreddit, channel, handle
    val authorHandle: String? = null,
    val authorDisplay: String? = null,
    val title: String? = null,
    val bodyText: String? = null,
    val bodyHtml: String? = null,
    val score: Int = 0,
    val commentCount: Int = 0,
    val postedAt: Long? = null,
    val permalink: String? = null,
    val isNsfw: Boolean = false,
    val rawJson: String? = null,
    val extractorVersion: String? = null,
    val fetchedAt: Long = System.currentTimeMillis()
)
