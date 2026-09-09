package com.tuck.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A step inside a saved item.
 *
 * The lifecycle work gave a save one binary done state; a checklist gives it several, for
 * the saves that are really a small piece of work - a recipe's ingredients, the steps in a
 * tutorial, the things to compare before buying.
 */
@Entity(
    tableName = "checklist_items",
    foreignKeys = [
        ForeignKey(
            entity = SavedItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["itemId"]), Index(value = ["ordinal"])]
)
data class ChecklistItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val itemId: Long,
    val text: String,
    val isDone: Boolean = false,
    val ordinal: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
