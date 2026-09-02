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

                val canonical = doc.select("link[rel=canonical]").attr("href").trim()
                    .takeIf { it.isNotBlank() }
                    ?.let { resolve(url, it) }
                val favicon = doc.select("link[rel~=(?i)^(shortcut|icon|apple-touch-icon)]")
                    .attr("href").trim()
                    .takeIf { it.isNotBlank() }
                    ?.let { resolve(url, it) }

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
                    leadImageUrl = ogImage?.let { resolve(url, it) },
                    mediaUrls = listOfNotNull(ogImage?.let { resolve(url, it) }),
                    canonicalUrl = canonical ?: url,
                    faviconUrl = favicon
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

    /** Absolutises a page-relative href; a bad one is discarded rather than stored broken. */
    private fun resolve(base: String, href: String): String? = try {
        URI(base).resolve(href).toString()
    } catch (e: Exception) {
        href.takeIf { it.startsWith("http") }
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
