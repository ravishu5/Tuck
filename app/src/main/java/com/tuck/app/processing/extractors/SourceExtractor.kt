package com.tuck.app.processing.extractors

import com.tuck.app.domain.model.ContentType

data class ExtractedComment(
    val id: String,
    val parentId: String? = null,
    val author: String,
    val bodyText: String,
    val score: Int = 0,
    val depth: Int = 0,
    val path: String = "0001",
    val postedAt: Long? = null,
    val replies: List<ExtractedComment> = emptyList()
)

data class ExtractedSourceData(
    val platform: String,
    val title: String? = null,
    val description: String? = null,
    val bodyText: String? = null,
    val authorHandle: String? = null,
    val authorDisplay: String? = null,
    val community: String? = null,
    val score: Int = 0,
    val commentCount: Int = 0,
    val postedAt: Long? = null,
    val leadImageUrl: String? = null,
    val mediaUrls: List<String> = emptyList(),
    val comments: List<ExtractedComment> = emptyList(),
    val rawJson: String? = null,
    /** The URL that identifies this item, once redirects and share wrappers are stripped. */
    val canonicalUrl: String? = null,
    val faviconUrl: String? = null,
    /** `null` leaves the type the share sheet inferred alone. */
    val contentType: ContentType? = null
)

interface SourceExtractor {
    val platformName: String
    fun canHandle(url: String): Boolean

    /** Parses [content] — the payload fetched from [fetchUrl] — into structured data. */
    suspend fun extract(url: String, content: String? = null): ExtractedSourceData

    /**
     * The URL whose payload [extract] actually wants, which is not always the URL the user
     * shared: Reddit wants old.reddit, X wants the syndication endpoint. Keeping the rewrite
     * next to the parser that depends on it means [SourceContentFetcher] stays free of
     * per-platform knowledge.
     */
    fun fetchUrl(url: String): String = url

    /** Whether [fetchUrl] returns JSON rather than HTML. Drives the Accept header and parser. */
    val payloadIsJson: Boolean get() = false

    /**
     * Whether this platform serves its content only to a browser, so a plain fetch returns a
     * shell. True routes the payload through the Tier 2 capture engine instead.
     */
    val requiresRenderedHtml: Boolean get() = false

    /**
     * A CSS selector that means "the content has arrived", used by the capture engine to stop
     * waiting. Without one it falls back to a fixed settle delay, which is slower and guesses.
     */
    val readySelector: String? get() = null
}

/**
 * Whether an extraction is worth escalating past, i.e. whether the expensive Tier 2 render is
 * worth attempting.
 *
 * "Thin" means the parser found nothing a fetch had to provide — no body, no media, no comments
 * — and so returned only what the URL itself already encoded. Testing the *result* rather than
 * the platform means the costly tier is skipped whenever the cheap one happened to work, and
 * attempted on a platform whose markup changed under us without anyone editing a flag.
 */
fun ExtractedSourceData?.isThin(): Boolean =
    this == null || (bodyText.isNullOrBlank() && leadImageUrl.isNullOrBlank() && comments.isEmpty())
