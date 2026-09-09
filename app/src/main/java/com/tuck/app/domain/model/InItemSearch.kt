package com.tuck.app.domain.model

/** One place a query appears inside a saved item. */
data class InItemMatch(
    /** Which block of the item the hit is in, e.g. the body or a specific comment. */
    val blockId: String,
    val blockLabel: String,
    /** Index into the rendered list, so the UI can scroll to it. */
    val listIndex: Int,
    val range: IntRange
)

/** A searchable region of an item, registered by the detail screen as it builds. */
data class SearchableBlock(
    val id: String,
    val label: String,
    val listIndex: Int,
    val text: String
)

/**
 * Finds a query inside one saved item.
 *
 * Search finds the item; this finds the line. Tuck stores whole articles, pages of
 * recognised text and 300-comment threads, so "it's in here somewhere" is a real problem
 * once an item is longer than a screen - the exact request a user made of a competitor.
 */
object InItemSearch {

    /** Ignore single characters: every keystroke would otherwise match everywhere. */
    private const val MIN_QUERY_LENGTH = 2

    fun find(blocks: List<SearchableBlock>, query: String): List<InItemMatch> {
        val needle = query.trim()
        if (needle.length < MIN_QUERY_LENGTH) return emptyList()

        return blocks.flatMap { block ->
            occurrences(block.text, needle).map { range ->
                InItemMatch(
                    blockId = block.id,
                    blockLabel = block.label,
                    listIndex = block.listIndex,
                    range = range
                )
            }
        }
    }

    /** Every occurrence, case-insensitive and non-overlapping, in reading order. */
    fun occurrences(haystack: String, needle: String): List<IntRange> {
        if (needle.isEmpty() || haystack.isEmpty()) return emptyList()

        val found = mutableListOf<IntRange>()
        var index = haystack.indexOf(needle, ignoreCase = true)
        while (index >= 0) {
            found.add(index until index + needle.length)
            index = haystack.indexOf(needle, startIndex = index + needle.length, ignoreCase = true)
        }
        return found
    }

    /**
     * Wraps a match index so next/previous cycle rather than dead-ending at the edges,
     * which is what every find-in-page control does.
     */
    fun step(currentIndex: Int, total: Int, forward: Boolean): Int {
        if (total <= 0) return 0
        return if (forward) (currentIndex + 1) % total else (currentIndex - 1 + total) % total
    }
}
