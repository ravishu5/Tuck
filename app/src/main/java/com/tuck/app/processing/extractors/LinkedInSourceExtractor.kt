package com.tuck.app.processing.extractors

import org.jsoup.Jsoup
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Extracts a LinkedIn post.
 *
 * LinkedIn gates almost everything behind a session, so OpenGraph is genuinely all that is
 * available to an unauthenticated fetch. Kept as its own extractor rather than folded into the
 * generic one so the community label and favicon stay right, and so there is somewhere obvious
 * for a Tier-2 session-backed capture to land later.
 */
@Singleton
class LinkedInSourceExtractor @Inject constructor() : SourceExtractor {

    override val platformName: String = "LINKEDIN"

    private companion object {
        const val FAVICON = "https://static.licdn.com/aero-v1/sc/h/al2o9zrvru7aqj8e1x2rzsrca"
    }

    override fun canHandle(url: String): Boolean = url.lowercase().contains("linkedin.com")

    override suspend fun extract(url: String, content: String?): ExtractedSourceData {
        val canonical = url.substringBefore('?')

        if (!content.isNullOrBlank()) {
            try {
                val doc = Jsoup.parse(content)
                val ogTitle = doc.select("meta[property=og:title]").attr("content").trim()
                val ogDesc = doc.select("meta[property=og:description]").attr("content").trim()
                val ogImage = doc.select("meta[property=og:image]").attr("content").trim()
                    .replace("&amp;", "&")

                return ExtractedSourceData(
                    platform = platformName,
                    title = ogTitle.ifBlank { "LinkedIn Post" },
                    description = ogDesc.takeIf { it.isNotBlank() },
                    bodyText = ogDesc.takeIf { it.isNotBlank() },
                    community = "LinkedIn",
                    leadImageUrl = ogImage.takeIf { it.isNotBlank() },
                    mediaUrls = listOfNotNull(ogImage.takeIf { it.isNotBlank() }),
                    canonicalUrl = canonical,
                    faviconUrl = FAVICON
                )
            } catch (e: Exception) {
                // Fall through to the bare shape below.
            }
        }

        return ExtractedSourceData(
            platform = platformName,
            title = "LinkedIn Post",
            community = "LinkedIn",
            canonicalUrl = canonical,
            faviconUrl = FAVICON
        )
    }
}
