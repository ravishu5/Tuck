package com.tuck.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tuck.app.domain.model.EntityType

@Entity(
    tableName = "entities",
    foreignKeys = [
        ForeignKey(
            entity = SavedItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["savedItemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["savedItemId"]),
        Index(value = ["type"]),
        Index(value = ["normalizedValue"])
    ]
)
data class EntityEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val savedItemId: Long,
    val type: EntityType,
    val value: String,
    val normalizedValue: String,
    val charStart: Int = 0,
    val charEnd: Int = 0,
    val producer: String = "rule-based"
)
