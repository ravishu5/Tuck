package com.tuck.app.processing.extractors

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RedditSourceExtractor @Inject constructor() : SourceExtractor {

    override val platformName: String = "REDDIT"

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    override fun canHandle(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("reddit.com") || lower.contains("redd.it")
    }

    override suspend fun extract(url: String, content: String?): ExtractedSourceData {
        if (content.isNullOrBlank()) {
            return ExtractedSourceData(
                platform = platformName,
                title = "Reddit Discussion",
                community = extractSubredditFromUrl(url)
            )
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
                    community = if (!subreddit.isNullOrBlank()) "r/$subreddit" else null,
                    score = score,
                    commentCount = commentCount,
                    postedAt = createdUtc,
                    comments = comments,
                    rawJson = content
                )
            }
        } catch (e: Exception) {
            // Fallback
        }

        return ExtractedSourceData(
            platform = platformName,
            title = "Reddit Discussion",
            community = extractSubredditFromUrl(url)
        )
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
        return match?.groupValues?.get(1)?.let { "r/$it" }
    }
}
