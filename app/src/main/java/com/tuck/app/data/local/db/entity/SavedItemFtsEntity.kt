package com.tuck.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

@Fts4
@Entity(tableName = "saved_items_fts")
data class SavedItemFtsEntity(
    @PrimaryKey
    val rowid: Long,
    val title: String,
    val description: String,
    val originalUrl: String,
    val sourceDomain: String,
    val originalText: String,
    val extractedText: String,
    val ocrText: String,
    val tags: String,
    val entities: String
)
