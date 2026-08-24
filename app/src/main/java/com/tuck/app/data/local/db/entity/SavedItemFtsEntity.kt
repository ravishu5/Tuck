package com.tuck.app.data.local.db.entity

/**
 * A row of the `saved_items_fts` index.
 *
 * Deliberately *not* a Room `@Entity`: the index is a raw FTS5 virtual table so it
 * can use BM25 ranking, which Room's `@Fts4` annotation cannot express. It is
 * created and maintained by [com.tuck.app.data.local.db.dao.SavedItemFtsDaoImpl].
 */
data class SavedItemFtsEntity(
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
