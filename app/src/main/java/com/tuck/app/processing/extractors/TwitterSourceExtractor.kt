package com.tuck.app.processing.extractors

import org.jsoup.Jsoup
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TwitterSourceExtractor @Inject constructor() : SourceExtractor {

    override val platformName: String = "TWITTER"

    private val handlePattern = Pattern.compile(
        "(?:twitter\\.com|x\\.com)/([a-zA-Z0-9_]{1,15})/status/(\\d+)",
        Pattern.CASE_INSENSITIVE
    )

    override fun canHandle(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("twitter.com") || lower.contains("x.com")
    }

    override suspend fun extract(url: String, content: String?): ExtractedSourceData {
        val matcher = handlePattern.matcher(url)
        val authorHandle = if (matcher.find()) "@${matcher.group(1)}" else null

        if (!content.isNullOrBlank()) {
            try {
                val doc = Jsoup.parse(content)
                val ogTitle = doc.select("meta[property=og:title]").attr("content").ifBlank { doc.title() }
                val ogDesc = doc.select("meta[property=og:description]").attr("content")
                val ogImage = doc.select("meta[property=og:image]").attr("content").takeIf { it.isNotBlank() }

                return ExtractedSourceData(
                    platform = platformName,
                    title = if (!ogTitle.isNullOrBlank()) ogTitle else "Post on X by ${authorHandle ?: "user"}",
                    description = ogDesc,
                    bodyText = ogDesc,
                    authorHandle = authorHandle,
                    leadImageUrl = ogImage,
                    mediaUrls = if (ogImage != null) listOf(ogImage) else emptyList()
                )
            } catch (e: Exception) {
                // Fall through
            }
        }

        return ExtractedSourceData(
            platform = platformName,
            title = "Post on X by ${authorHandle ?: "user"}",
            authorHandle = authorHandle
        )
    }
}
