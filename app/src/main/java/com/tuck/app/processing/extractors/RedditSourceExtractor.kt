package com.tuck.app.processing.extractors

import com.tuck.app.domain.model.ContentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.net.URI
import java.time.OffsetDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RedditSourceExtractor @Inject constructor() : SourceExtractor {

    override val platformName: String = "REDDIT"

    private companion object {
        const val FAVICON = "https://www.redditstatic.com/desktop2x/img/favicon/favicon-96x96.png"
        const val COMMENT_LIMIT = 500
        const val COMMENT_DEPTH = 8
    }

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    override fun canHandle(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("reddit.com") || lower.contains("redd.it")
    }

    // old.reddit.com answers with server-rendered HTML, so the payload is not JSON. The `.json`
    // listing is richer but 403s without OAuth credentials (see FUTURE_WORK.md); [extract] still
    // parses it when one arrives, so a future authenticated fetch needs no change here.
    override val payloadIsJson: Boolean = false

    /**
     * Measured 2026-09-02: old.reddit now 302s every *logged-out* request to
     * `/login/?reason=lor2`, so a plain fetch returns a login page and this parser finds no post.
     * The markup itself has not changed — the access rule has.
     *
     * That makes Reddit a session problem rather than a parsing one, which is exactly what Tier 2
     * is for: the capture engine shares the viewer's cookies, so a reader signed into Reddit
     * renders the real thread and everything below works unchanged. Logged out it degrades to
     * the same fallback as today rather than getting worse.
     */
    override val requiresRenderedHtml: Boolean = true

    override val readySelector: String = ".commentarea .sitetable, #siteTable div.thing.link"

    /**
     * Reddit's modern front end renders comments in the browser, leaving a crawler with an empty
     * shell. `old.reddit.com` still serves the whole thread as plain HTML with no JavaScript —
     * it just will not serve it to a logged-out client any more, hence [requiresRenderedHtml].
     */
    override fun fetchUrl(url: String): String {
        val host = try {
            URI(url).host?.lowercase()
        } catch (e: Exception) {
            null
        } ?: return url

        // redd.it short links only reveal their destination by following the redirect, which
        // happens inside the fetcher, so they cannot be rewritten ahead of time.
        if (host != "reddit.com" && !host.endsWith(".reddit.com")) return url

        val path = url.substringAfter(host, "").substringBefore('?').substringBefore('#')
        return "https://old.reddit.com${path.ifBlank { "/" }}?limit=$COMMENT_LIMIT&depth=$COMMENT_DEPTH"
    }

    override suspend fun extract(url: String, content: String?): ExtractedSourceData {
        if (content.isNullOrBlank()) return fallback(url)

        // Which shape arrived depends on what the fetch reached, not on configuration: a `.json`
        // listing when one is available, old.reddit HTML otherwise.
        val trimmed = content.trimStart()
        if (!trimmed.startsWith("[") && !trimmed.startsWith("{")) {
            return parseOldRedditHtml(content, url) ?: fallback(url)
        }

        try {
            val root = json.parseToJsonElement(content)
            if (root is JsonArray && root.size >= 1) {
                // Post listing (first element)
                val postListing = root[0].jsonObject["data"]?.jsonObject?.get("children")?.jsonArray?.firstOrNull()?.jsonObject?.get("data")?.jsonObject

                val title = postListing?.get("title")?.jsonPrimitive?.content ?: "Reddit Discussion"
                val body = postListing?.get("selftext")?.jsonPrimitive?.content
                val author = postListing?.get("author")?.jsonPrimitive?.content
                val subreddit = postListing?.get("subreddit")?.jsonPrimitive?.content ?: extractSubredditFromUrl(url)
                val score = postListing?.get("score")?.jsonPrimitive?.intOrNull ?: 0
                val commentCount = postListing?.get("num_comments")?.jsonPrimitive?.intOrNull ?: 0
                val createdUtc = postListing?.get("created_utc")?.jsonPrimitive?.longOrNull?.let { it * 1000 }

                // Comments listing (second element)
                val comments = mutableListOf<ExtractedComment>()
                if (root.size >= 2) {
                    val commentsArray = root[1].jsonObject["data"]?.jsonObject?.get("children")?.jsonArray
                    if (commentsArray != null) {
                        var ordinal = 0
                        for (cElem in commentsArray) {
                            val commentData = cElem.jsonObject["data"]?.jsonObject ?: continue
                            val parsed = parseRedditComment(commentData, parentPath = null, index = ++ordinal, depth = 0)
                            if (parsed != null) {
                                comments.add(parsed)
                            }
                        }
                    }
                }

                return ExtractedSourceData(
                    platform = platformName,
                    title = title,
                    description = body?.take(300),
                    bodyText = body,
                    authorHandle = author,
                    community = communityLabel(subreddit),
                    score = score,
                    commentCount = commentCount,
                    postedAt = createdUtc,
                    comments = comments,
                    rawJson = content,
                    canonicalUrl = url.substringBefore('?').removeSuffix("/"),
                    faviconUrl = FAVICON,
                    contentType = if (postListing?.get("is_video")?.jsonPrimitive?.content == "true") {
                        ContentType.VIDEO
                    } else {
                        null
                    }
                )
            }
        } catch (e: Exception) {
            // Fallback
        }

        return fallback(url)
    }

    private fun fallback(url: String) = ExtractedSourceData(
        platform = platformName,
        title = "Reddit Discussion",
        community = communityLabel(extractSubredditFromUrl(url))
    )

    // ---------------------------------------------------------------- old.reddit HTML

    /**
     * Parses a thread as `old.reddit.com` renders it. Returns null when the markup carries no
     * post, which is how a new-Reddit page or an error page arrives — the caller falls back
     * rather than archiving a shell as if it were the thread.
     */
    private fun parseOldRedditHtml(html: String, url: String): ExtractedSourceData? {
        val doc = try {
            Jsoup.parse(html)
        } catch (e: Exception) {
            return null
        }

        val post = doc.selectFirst("#siteTable div.thing.link") ?: doc.selectFirst("div.thing.link")
        val title = post?.selectFirst("a.title")?.text()?.trim()
        if (title.isNullOrBlank()) return null

        val author = post.selectFirst(".entry .author")?.text()?.trim()?.removePrefix("u/")
        val subreddit = post.attr("data-subreddit").takeIf { it.isNotBlank() }
            ?: extractSubredditFromUrl(url)
        val selfText = post.selectFirst(".expando .usertext-body .md, .usertext-body .md")
            ?.let { blockText(it) }
            ?.takeIf { it.isNotBlank() }

        val comments = doc.selectFirst(".commentarea .sitetable.nestedlisting")
            ?.let { parseCommentListing(it, parentPath = null, parentFullname = null, depth = 0) }
            .orEmpty()

        return ExtractedSourceData(
            platform = platformName,
            title = title,
            description = selfText?.take(300),
            bodyText = selfText,
            authorHandle = author,
            community = communityLabel(subreddit),
            score = post.attr("data-score").toIntOrNull() ?: scoreOf(post) ?: 0,
            // The count in the markup is the true thread size, which is larger than the slice
            // old.reddit renders inline. Reporting it honestly beats reporting what we captured.
            commentCount = post.attr("data-comments-count").toIntOrNull() ?: comments.size,
            postedAt = post.selectFirst("time[datetime]")?.let { parseHtmlTimestamp(it.attr("datetime")) },
            comments = comments,
            canonicalUrl = url.substringBefore('?').removeSuffix("/"),
            faviconUrl = FAVICON
        )
    }

    /**
     * Walks one level of the comment tree.
     *
     * Only direct children count: `select` searches all descendants, which would pull a reply
     * out of its parent and flatten the nesting the `source_comments` materialized path exists
     * to preserve.
     */
    private fun parseCommentListing(
        listing: Element,
        parentPath: String?,
        parentFullname: String?,
        depth: Int
    ): List<ExtractedComment> {
        val out = mutableListOf<ExtractedComment>()
        var ordinal = 0

        listing.children()
            // `thing morechildren` is the "load more comments" stub, not a comment.
            .filter { it.hasClass("thing") && it.hasClass("comment") }
            .forEach { thing ->
                val entry = thing.children().firstOrNull { it.hasClass("entry") } ?: return@forEach
                val body = entry.selectFirst(".usertext-body .md")?.let { blockText(it) }.orEmpty()
                val author = entry.selectFirst(".author")?.text()?.trim()?.removePrefix("u/")
                    ?: "[deleted]"
                if (body.isBlank() || body == "[deleted]" || body == "[removed]") return@forEach

                val path = buildPath(parentPath, ++ordinal)
                val fullname = thing.attr("data-fullname").takeIf { it.isNotBlank() }

                val childListing = thing.children().firstOrNull { it.hasClass("child") }
                    ?.children()?.firstOrNull { it.hasClass("sitetable") }
                val replies = childListing
                    ?.let { parseCommentListing(it, path, fullname, depth + 1) }
                    .orEmpty()

                out.add(
                    ExtractedComment(
                        id = fullname?.substringAfter('_') ?: path,
                        parentId = parentFullname,
                        author = author,
                        bodyText = body,
                        score = thing.attr("data-score").toIntOrNull() ?: scoreOf(entry) ?: 0,
                        depth = depth,
                        path = path,
                        postedAt = entry.selectFirst("time[datetime]")
                            ?.let { parseHtmlTimestamp(it.attr("datetime")) },
                        replies = replies
                    )
                )
            }

        return out
    }

    private fun buildPath(parentPath: String?, index: Int): String {
        val segment = "%04d".format(index)
        return if (parentPath == null) segment else "$parentPath.$segment"
    }

    /** `<span class="score unvoted" title="128">128 points</span>` — the title holds the number. */
    private fun scoreOf(scope: Element): Int? =
        scope.selectFirst(".score.unvoted, .score")?.attr("title")?.toIntOrNull()

    /**
     * Markdown rendered to HTML, flattened back to text with its paragraph breaks intact —
     * `Element.text()` alone would run a whole comment together into one line.
     */
    private fun blockText(md: Element): String {
        val blocks = md.select("p, li, blockquote, pre")
        val text = if (blocks.isEmpty()) {
            md.text()
        } else {
            blocks.joinToString("\n\n") { it.text() }
        }
        return text.trim()
    }

    private fun parseHtmlTimestamp(raw: String?): Long? {
        if (raw.isNullOrBlank()) return null
        return try {
            OffsetDateTime.parse(raw).toInstant().toEpochMilli()
        } catch (e: Exception) {
            null
        }
    }

    private fun parseRedditComment(data: JsonObject, parentPath: String?, index: Int, depth: Int): ExtractedComment? {
        val id = data["id"]?.jsonPrimitive?.content ?: return null
        val body = data["body"]?.jsonPrimitive?.content ?: return null
        val author = data["author"]?.jsonPrimitive?.content ?: "[deleted]"
        val score = data["score"]?.jsonPrimitive?.intOrNull ?: 0
        val createdUtc = data["created_utc"]?.jsonPrimitive?.longOrNull?.let { it * 1000 }
        val parentId = data["parent_id"]?.jsonPrimitive?.content

        val pathSegment = "%04d".format(index)
        val fullPath = if (parentPath == null) pathSegment else "$parentPath.$pathSegment"

        val childComments = mutableListOf<ExtractedComment>()
        val repliesObj = data["replies"]
        if (repliesObj is JsonObject) {
            val children = repliesObj["data"]?.jsonObject?.get("children")?.jsonArray
            if (children != null) {
                var childOrdinal = 0
                for (child in children) {
                    val childData = child.jsonObject["data"]?.jsonObject ?: continue
                    val parsedChild = parseRedditComment(childData, fullPath, ++childOrdinal, depth + 1)
                    if (parsedChild != null) {
                        childComments.add(parsedChild)
                    }
                }
            }
        }

        return ExtractedComment(
            id = id,
            parentId = parentId,
            author = author,
            bodyText = body,
            score = score,
            depth = depth,
            path = fullPath,
            postedAt = createdUtc,
            replies = childComments
        )
    }

    private fun extractSubredditFromUrl(url: String): String? {
        val regex = Regex("reddit\\.com/r/([^/]+)", RegexOption.IGNORE_CASE)
        val match = regex.find(url)
        return match?.groupValues?.get(1)
    }

    /** Normalizes a subreddit name to a single `r/` prefix. */
    private fun communityLabel(name: String?): String? =
        name?.trim()?.takeIf { it.isNotBlank() }?.let { if (it.startsWith("r/")) it else "r/$it" }
}
