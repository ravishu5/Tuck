package com.tuck.app.domain.model

/**
 * Decides whether a single saved item satisfies a query-DSL expression.
 *
 * This is the counterpart to search: search asks the database "which items match?",
 * a filing rule asks "does *this* item match?" - so the same `source:reddit type:pdf`
 * syntax the user already types into the search box becomes the rule language, and
 * there is no second thing to learn.
 *
 * Every stated condition must hold (AND). A rule that states nothing matches nothing,
 * deliberately: a rule that silently files everything is the worst possible failure
 * here, because the user would have to unpick it by hand.
 */
object RuleMatcher {

    fun matches(query: ParsedQuery, item: SavedItem): Boolean {
        if (isEmpty(query)) return false

        query.contentType?.let { if (item.contentType != it) return false }

        query.sourceDomain?.let { wanted ->
            // Users write `source:reddit`; the stored domain is `reddit.com`.
            val haystack = listOfNotNull(item.sourceDomain, item.originalUrl, item.sourceApp)
                .joinToString(" ") { it.lowercase() }
            if (!haystack.contains(wanted.lowercase())) return false
        }

        query.tag?.let { wanted ->
            if (item.tags.none { it.name.equals(wanted, ignoreCase = true) }) return false
        }

        query.collectionName?.let { wanted ->
            if (item.collections.none { it.name.equals(wanted, ignoreCase = true) }) return false
        }

        if (query.isFavoriteOnly && !item.isFavorite) return false
        if (query.isArchivedOnly && !item.isArchived) return false

        query.createdAfter?.let { if (item.createdAt < it) return false }
        query.createdBefore?.let { if (item.createdAt > it) return false }

        if (query.freeText.isNotBlank()) {
            val haystack = searchableText(item)
            val allTermsPresent = query.freeText
                .split(Regex("\\s+"))
                .filter { it.isNotBlank() }
                .all { haystack.contains(it.lowercase()) }
            if (!allTermsPresent) return false
        }

        return true
    }

    /** True when the expression states no condition at all, and so must not match. */
    fun isEmpty(query: ParsedQuery): Boolean =
        query.freeText.isBlank() &&
            query.contentType == null &&
            query.sourceDomain == null &&
            query.tag == null &&
            query.collectionName == null &&
            !query.isFavoriteOnly &&
            !query.isArchivedOnly &&
            query.createdAfter == null &&
            query.createdBefore == null

    private fun searchableText(item: SavedItem): String = buildString {
        append(item.title).append(' ')
        append(item.description.orEmpty()).append(' ')
        append(item.originalText.orEmpty()).append(' ')
        append(item.extractedText.orEmpty()).append(' ')
        append(item.ocrText.orEmpty()).append(' ')
        append(item.originalUrl.orEmpty()).append(' ')
        append(item.sourceDomain.orEmpty()).append(' ')
        append(item.userNote.orEmpty()).append(' ')
        append(item.captureNote.orEmpty()).append(' ')
        item.tags.forEach { append(it.name).append(' ') }
        item.entities.forEach { append(it.value).append(' ') }
    }.lowercase()
}
