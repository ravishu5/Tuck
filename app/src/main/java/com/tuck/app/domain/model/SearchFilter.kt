package com.tuck.app.domain.model

data class SearchFilter(
    val query: String = "",
    val contentType: ContentType? = null,
    val sourceDomain: String? = null,
    val collectionId: Long? = null,
    val isFavoriteOnly: Boolean = false,
    val isArchivedOnly: Boolean = false,
    val dateRangeDays: Int? = null, // e.g. 7, 30, or null for all time
    /** Absolute bounds from the query DSL (`after:` / `before:`), in epoch millis. */
    val createdAfter: Long? = null,
    val createdBefore: Long? = null,
    /** Collection name from `in:`, resolved to an id by the repository. */
    val collectionName: String? = null,
    /** Tag from `tag:`, applied as an FTS column filter. */
    val tag: String? = null,
    val sortOrder: SortOrder = SortOrder.RELEVANCE
)

data class SearchResult(
    val item: SavedItem,
    val matchSnippet: String? = null,
    val score: Double = 0.0
)
