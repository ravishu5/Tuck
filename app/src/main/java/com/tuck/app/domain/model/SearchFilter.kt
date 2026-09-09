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

/**
 * Which indexed field a result matched on.
 *
 * The FTS index already scores per column — see `SavedItemFtsDaoImpl.COLUMN_WEIGHTS` —
 * so the app knows *why* a row came back and simply used to throw that away. Showing it
 * is the difference between "trust the ranking" and "here is the evidence": a hit in a
 * screenshot's recognised text is a very different claim from a hit in a title.
 *
 * The order of the constants is the order the chips are shown in, strongest first.
 */
enum class MatchField {
    TITLE,
    TAG,
    ENTITY,
    DESCRIPTION,
    TEXT,
    COMMENT,
    OCR,
    URL,
    DOMAIN
}

data class SearchResult(
    val item: SavedItem,
    val matchSnippet: String? = null,
    val score: Double = 0.0,
    /** The fields this result matched on, strongest first. Empty when unknown. */
    val matchFields: List<MatchField> = emptyList()
)
