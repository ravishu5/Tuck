package com.tuck.app.domain.model

import java.util.Calendar

/**
 * One `key:value` filter recognised in the search box, kept so the UI can show it
 * as a removable chip.
 */
data class QueryToken(
    val raw: String,
    val key: String,
    val value: String,
    val label: String
)

data class ParsedQuery(
    /** What is left after the operators are removed - this is what goes to FTS. */
    val freeText: String = "",
    val tokens: List<QueryToken> = emptyList(),
    val contentType: ContentType? = null,
    val sourceDomain: String? = null,
    val collectionName: String? = null,
    val tag: String? = null,
    val isFavoriteOnly: Boolean = false,
    val isArchivedOnly: Boolean = false,
    val createdAfter: Long? = null,
    val createdBefore: Long? = null
)

/**
 * Parses search operators out of a plain input box: `source:reddit`, `type:pdf`,
 * `in:research`, `tag:nike`, `is:favorite`, `after:last-month`, `before:2026-01`.
 *
 * Anything unrecognised falls through to free text, so ordinary searches are
 * unaffected and a typo never swallows the query.
 */
object SearchQueryParser {

    private val TOKEN = Regex("""(\w+):("[^"]+"|\S+)""")

    fun parse(input: String, now: Long = System.currentTimeMillis()): ParsedQuery {
        var result = ParsedQuery()
        val consumed = mutableListOf<QueryToken>()

        val remainder = TOKEN.replace(input) { match ->
            val key = match.groupValues[1].lowercase()
            val value = match.groupValues[2].trim('"')
            val applied = apply(result, key, value, now)
            if (applied == null) {
                match.value // not an operator we know - leave it as free text
            } else {
                result = applied.first
                consumed.add(QueryToken(match.value, key, value, applied.second))
                ""
            }
        }

        return result.copy(
            freeText = remainder.replace(Regex("\\s+"), " ").trim(),
            tokens = consumed
        )
    }

    /** Returns the updated query and a human label, or null if the key is unknown. */
    private fun apply(
        current: ParsedQuery,
        key: String,
        value: String,
        now: Long
    ): Pair<ParsedQuery, String>? = when (key) {
        "type" -> contentTypeOf(value)?.let {
            current.copy(contentType = it) to "Type: ${it.name.lowercase()}"
        }

        "source", "domain", "site" ->
            current.copy(sourceDomain = value.lowercase()) to "Source: ${value.lowercase()}"

        "in", "collection" ->
            current.copy(collectionName = value) to "In: $value"

        "tag" ->
            current.copy(tag = value.lowercase()) to "Tag: ${value.lowercase()}"

        "is" -> when (value.lowercase()) {
            "favorite", "favourite", "starred" -> current.copy(isFavoriteOnly = true) to "Favorites"
            "archived" -> current.copy(isArchivedOnly = true) to "Archived"
            else -> null
        }

        "after", "since" -> boundaryOf(value, now)?.let {
            current.copy(createdAfter = it) to "After: $value"
        }

        "before", "until" -> boundaryOf(value, now)?.let {
            current.copy(createdBefore = it) to "Before: $value"
        }

        else -> null
    }

    private fun contentTypeOf(value: String): ContentType? = when (value.lowercase()) {
        "url", "link" -> ContentType.URL
        "image", "img", "screenshot" -> ContentType.IMAGE
        "pdf" -> ContentType.PDF
        "video" -> ContentType.VIDEO
        "note", "text" -> ContentType.TEXT
        "document", "doc", "file" -> ContentType.DOCUMENT
        else -> runCatching { ContentType.valueOf(value.uppercase()) }.getOrNull()
    }

    /** Accepts `2026-08-24`, `2026-08`, `2026`, and relative forms like `last-month`. */
    private fun boundaryOf(value: String, now: Long): Long? {
        val calendar = Calendar.getInstance().apply { timeInMillis = now }

        when (value.lowercase()) {
            "today" -> return startOfDay(calendar)
            "yesterday" -> return startOfDay(calendar.apply { add(Calendar.DAY_OF_YEAR, -1) })
            "last-week", "lastweek" -> return startOfDay(calendar.apply { add(Calendar.DAY_OF_YEAR, -7) })
            "last-month", "lastmonth" -> return startOfDay(calendar.apply { add(Calendar.MONTH, -1) })
            "last-year", "lastyear" -> return startOfDay(calendar.apply { add(Calendar.YEAR, -1) })
        }

        val parts = value.split("-")
        val year = parts.getOrNull(0)?.toIntOrNull() ?: return null
        if (year < 1970 || year > 3000) return null
        val month = parts.getOrNull(1)?.toIntOrNull() ?: 1
        val day = parts.getOrNull(2)?.toIntOrNull() ?: 1

        return Calendar.getInstance().apply {
            clear()
            set(year, (month - 1).coerceIn(0, 11), day.coerceAtLeast(1))
        }.timeInMillis
    }

    private fun startOfDay(calendar: Calendar): Long = calendar.apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
