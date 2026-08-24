package com.tuck.app.processing.extractors

import org.jsoup.Jsoup
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GenericWebSourceExtractor @Inject constructor() : SourceExtractor {

    override val platformName: String = "WEB"

    override fun canHandle(url: String): Boolean = true

    override suspend fun extract(url: String, content: String?): ExtractedSourceData {
        val domain = extractDomain(url)

        if (!content.isNullOrBlank()) {
            try {
                val doc = Jsoup.parse(content)
                val ogTitle = doc.select("meta[property=og:title]").attr("content")
                    .ifBlank { doc.select("meta[name=twitter:title]").attr("content") }
                    .ifBlank { doc.title() }

                val ogDesc = doc.select("meta[property=og:description]").attr("content")
                    .ifBlank { doc.select("meta[name=description]").attr("content") }
                    .ifBlank { doc.select("meta[name=twitter:description]").attr("content") }

                val ogImage = doc.select("meta[property=og:image]").attr("content")
                    .ifBlank { doc.select("meta[name=twitter:image]").attr("content") }
                    .takeIf { it.isNotBlank() }

                val author = doc.select("meta[name=author]").attr("content")
                    .ifBlank { doc.select("meta[property=article:author]").attr("content") }
                    .takeIf { it.isNotBlank() }

                val siteName = doc.select("meta[property=og:site_name]").attr("content").ifBlank { domain }

                // Article body extraction fallback (main paragraph text)
                val paragraphs = doc.select("article p, main p, div[itemprop=articleBody] p, p")
                val articleText = paragraphs.take(10).joinToString("\n\n") { it.text() }.take(4000)

                return ExtractedSourceData(
                    platform = platformName,
                    title = ogTitle.ifBlank { domain },
                    description = ogDesc.ifBlank { articleText.take(300) },
                    bodyText = if (articleText.isNotBlank()) articleText else ogDesc,
                    authorDisplay = author,
                    community = siteName,
                    leadImageUrl = ogImage,
                    mediaUrls = if (ogImage != null) listOf(ogImage) else emptyList()
                )
            } catch (e: Exception) {
                // Fall through
            }
        }

        return ExtractedSourceData(
            platform = platformName,
            title = domain,
            community = domain
        )
    }

    private fun extractDomain(url: String): String {
        return try {
            val uri = URI(url)
            val host = uri.host ?: ""
            host.removePrefix("www.")
        } catch (e: Exception) {
            "web"
        }
    }
}
