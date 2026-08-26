package com.tuck.app.processing

import com.tuck.app.data.local.db.entity.EntityEntity
import com.tuck.app.domain.model.EntityType
import com.tuck.app.processing.extractors.ExtractedComment
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SourcePersonExtractor @Inject constructor() {

    private val ignoredAuthors = setOf(
        "[deleted]",
        "[removed]",
        "deleted",
        "removed",
        "anonymous",
        "null",
        "undefined",
        "unknown",
        "n/a",
        "none"
    )

    /**
     * Normalizes a platform-specific author identifier to a pair of (displayValue, normalizedValue).
     *
     * The normalizedValue uses a platform prefix (`reddit:someone`, `youtube:channel`, `twitter:handle`, `web:author`)
     * so identical handles across different platforms never collide while ensuring exact identity matching
     * on the same platform.
     */
    fun normalizePerson(platform: String, rawAuthor: String?): Pair<String, String>? {
        if (rawAuthor.isNullOrBlank()) return null
        val trimmed = rawAuthor.trim()
        if (trimmed.isEmpty()) return null

        val lowerTrimmed = trimmed.lowercase()
        if (ignoredAuthors.contains(lowerTrimmed)) return null

        val normPlatform = platform.trim().uppercase()

        return when (normPlatform) {
            "REDDIT" -> {
                val clean = trimmed
                    .removePrefix("u/")
                    .removePrefix("U/")
                    .removePrefix("r/")
                    .removePrefix("R/")
                    .removePrefix("@")
                    .trim()
                if (clean.isBlank() || ignoredAuthors.contains(clean.lowercase())) return null
                val display = "u/$clean"
                val normalized = "reddit:${clean.lowercase()}"
                Pair(display, normalized)
            }
            "YOUTUBE" -> {
                val clean = trimmed.removePrefix("@").trim()
                if (clean.isBlank() || ignoredAuthors.contains(clean.lowercase())) return null
                val display = trimmed
                val normalized = "youtube:${clean.lowercase()}"
                Pair(display, normalized)
            }
            "TWITTER", "X" -> {
                val clean = trimmed.removePrefix("@").trim()
                if (clean.isBlank() || ignoredAuthors.contains(clean.lowercase())) return null
                val display = "@$clean"
                val normalized = "twitter:${clean.lowercase()}"
                Pair(display, normalized)
            }
            "WEB" -> {
                val clean = trimmed
                    .removePrefix("By ")
                    .removePrefix("by ")
                    .replace("\\s+".toRegex(), " ")
                    .trim()
                if (clean.isBlank() || ignoredAuthors.contains(clean.lowercase())) return null
                val display = clean
                val normalized = "web:${clean.lowercase()}"
                Pair(display, normalized)
            }
            else -> {
                val clean = trimmed.removePrefix("@").replace("\\s+".toRegex(), " ").trim()
                if (clean.isBlank() || ignoredAuthors.contains(clean.lowercase())) return null
                val platformKey = normPlatform.lowercase().ifBlank { "source" }
                val display = clean
                val normalized = "$platformKey:${clean.lowercase()}"
                Pair(display, normalized)
            }
        }
    }

    /**
     * Extracts deduplicated PERSON entities for post author and all comment authors.
     */
    fun extractEntities(
        savedItemId: Long,
        platform: String,
        postAuthor: String? = null,
        postAuthorDisplay: String? = null,
        comments: List<ExtractedComment> = emptyList(),
        fallbackCommentAuthors: List<String> = emptyList()
    ): List<EntityEntity> {
        val results = mutableListOf<EntityEntity>()
        val seenNormalized = mutableSetOf<String>()

        fun addIfValid(raw: String?) {
            val pair = normalizePerson(platform, raw) ?: return
            val (display, normalized) = pair
            if (seenNormalized.add(normalized)) {
                results.add(
                    EntityEntity(
                        savedItemId = savedItemId,
                        type = EntityType.PERSON,
                        value = display,
                        normalizedValue = normalized,
                        producer = "source-metadata"
                    )
                )
            }
        }

        // 1. Post author
        addIfValid(postAuthor)
        if (postAuthorDisplay != null && postAuthorDisplay != postAuthor) {
            addIfValid(postAuthorDisplay)
        }

        // 2. Structured comment authors (tree)
        fun walkComments(list: List<ExtractedComment>) {
            for (c in list) {
                addIfValid(c.author)
                if (c.replies.isNotEmpty()) {
                    walkComments(c.replies)
                }
            }
        }
        walkComments(comments)

        // 3. Fallback comment authors
        for (author in fallbackCommentAuthors) {
            addIfValid(author)
        }

        return results
    }
}
