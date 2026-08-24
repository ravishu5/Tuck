package com.tuck.app.processing.extractors

import org.jsoup.Jsoup
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YouTubeSourceExtractor @Inject constructor() : SourceExtractor {

    override val platformName: String = "YOUTUBE"

    private val videoIdPattern = Pattern.compile(
        "(?:youtu\\.be/|youtube\\.com/(?:embed/|v/|watch\\?v=|watch\\?.+&v=))([\\w-]{11})"
    )

    override fun canHandle(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("youtube.com") || lower.contains("youtu.be")
    }

    override suspend fun extract(url: String, content: String?): ExtractedSourceData {
        val videoId = extractVideoId(url)
        val defaultThumbnail = if (videoId != null) "https://img.youtube.com/vi/$videoId/hqdefault.jpg" else null

        if (!content.isNullOrBlank()) {
            try {
                val doc = Jsoup.parse(content)
                val ogTitle = doc.select("meta[property=og:title]").attr("content").ifBlank { doc.title() }
                val ogDesc = doc.select("meta[property=og:description]").attr("content")
                val ogImage = doc.select("meta[property=og:image]").attr("content").ifBlank { defaultThumbnail }
                val author = doc.select("link[itemprop=name]").attr("content")
                    .ifBlank { doc.select("meta[name=author]").attr("content") }
                    .ifBlank { doc.select("link[rel=author]").attr("href").substringAfterLast("/") }

                val cleanTitle = ogTitle.removeSuffix(" - YouTube").trim()

                return ExtractedSourceData(
                    platform = platformName,
                    title = cleanTitle.ifBlank { "YouTube Video" },
                    description = ogDesc,
                    authorDisplay = author.ifBlank { null },
                    community = if (!author.isNullOrBlank()) "@${author.removePrefix("@")}" else null,
                    leadImageUrl = ogImage,
                    mediaUrls = if (ogImage != null) listOf(ogImage) else emptyList()
                )
            } catch (e: Exception) {
                // Fall through to fallback
            }
        }

        return ExtractedSourceData(
            platform = platformName,
            title = "YouTube Video",
            leadImageUrl = defaultThumbnail,
            mediaUrls = if (defaultThumbnail != null) listOf(defaultThumbnail) else emptyList()
        )
    }

    fun extractVideoId(url: String): String? {
        val matcher = videoIdPattern.matcher(url)
        return if (matcher.find()) matcher.group(1) else null
    }
}
