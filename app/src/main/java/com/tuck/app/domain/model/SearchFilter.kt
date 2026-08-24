package com.tuck.app.domain.model

data class SearchFilter(
    val query: String = "",
    val contentType: ContentType? = null,
    val sourceDomain: String? = null,
    val collectionId: Long? = null,
    val isFavoriteOnly: Boolean = false,
    val isArchivedOnly: Boolean = false,
    val dateRangeDays: Int? = null, // e.g. 7, 30, or null for all time
    val sortOrder: SortOrder = SortOrder.RELEVANCE
)

data class SearchResult(
    val item: SavedItem,
    val matchSnippet: String? = null,
    val score: Double = 0.0
)
