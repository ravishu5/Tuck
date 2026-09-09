package com.tuck.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "derived_summaries",
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
        Index(value = ["kind"])
    ]
)
data class DerivedSummaryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val itemId: Long,
    val kind: String = "TLDR", // TLDR, ABSTRACT, WHY_SAVED
    val text: String,
    val producer: String = "rule-based",
    val modelVersion: String? = null,
    val producedAt: Long = System.currentTimeMillis(),
    val confidence: Float = 1.0f
)

@Entity(
    tableName = "derived_points",
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
        Index(value = ["kind"]),
        Index(value = ["ordinal"])
    ]
)
data class DerivedPointEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val itemId: Long,
    val kind: String = "KEY_POINT", // KEY_POINT, PRO, CON, RECOMMENDATION, WARNING, ACTION
    val text: String,
    val ordinal: Int = 0,
    val evidenceRef: String? = null,
    val producer: String = "rule-based",
    val producedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "ocr_blocks",
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
        Index(value = ["assetId"])
    ]
)
data class OcrBlockEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val assetId: Long? = null,
    val itemId: Long,
    val text: String,
    val confidence: Float = 1.0f,
    val bboxX: Float = 0f,
    val bboxY: Float = 0f,
    val bboxW: Float = 0f,
    val bboxH: Float = 0f,
    val blockIndex: Int = 0,
    val producer: String = "mlkit-ocr"
)
