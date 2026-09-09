package com.tuck.app.processing.extractors

import com.tuck.app.domain.model.ContentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Extracts a TikTok video from the public oEmbed endpoint.
 *
 * TikTok's watch page is client-rendered and aggressively bot-checked, but oEmbed is served for
 * embedders and answers unauthenticated with the caption, author and poster frame.
 */
@Singleton
class TikTokSourceExtractor @Inject constructor() : SourceExtractor {

    override val platformName: String = "TIKTOK"

    override val payloadIsJson: Boolean = true

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    override fun canHandle(url: String): Boolean = url.lowercase().contains("tiktok.com")

    override fun fetchUrl(url: String): String =
        "https://www.tiktok.com/oembed?url=${url.substringBefore('?')}"

    override suspend fun extract(url: String, content: String?): ExtractedSourceData {
        val root = content?.takeIf { it.isNotBlank() }?.let {
            try {
                json.parseToJsonElement(it) as? JsonObject
            } catch (e: Exception) {
                null
            }
        }

        val caption = root?.string("title")
        val author = root?.string("author_name")
        val thumbnail = root?.string("thumbnail_url")

        return ExtractedSourceData(
            platform = platformName,
            title = caption?.takeIf { it.isNotBlank() }
                ?: author?.let { "TikTok video by @$it" }
                ?: "TikTok video",
            description = caption,
            bodyText = caption,
            authorHandle = author?.let { "@${it.removePrefix("@")}" },
            authorDisplay = root?.string("author_name"),
            community = "TikTok",
            leadImageUrl = thumbnail,
            mediaUrls = listOfNotNull(thumbnail),
            canonicalUrl = url.substringBefore('?'),
            contentType = ContentType.VIDEO,
            rawJson = content
        )
    }

    private fun JsonObject.string(key: String): String? =
        this[key]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() && it != "null" }
}
