package com.tuck.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "item_raw_payload",
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
        Index(value = ["receivedAt"])
    ]
)
data class ItemRawPayloadEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val itemId: Long,
    val action: String? = null,
    val mimeType: String? = null,
    val text: String? = null,
    val uris: String? = null,
    val referrerPackage: String? = null,
    val receivedAt: Long = System.currentTimeMillis()
)
