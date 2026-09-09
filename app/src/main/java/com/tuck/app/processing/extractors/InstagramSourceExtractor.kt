package com.tuck.app.processing.extractors

import com.tuck.app.domain.model.ContentType
import org.jsoup.Jsoup
import java.net.URLDecoder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Extracts an Instagram post, reel or story card from the public embed page.
 *
 * **Known limitation, measured 2026-09-02.** `/embed/captioned/` is now client-rendered: the
 * server HTML carries no caption, no author and no media URL — only JavaScript bundles. Fetching
 * it over plain HTTP and parsing the result therefore yields nothing beyond what the URL itself
 * says (type, shortcode, canonical link, story author). Verified against a live public reel: the
 * markup this class selects on appears only after scripts run.
 *
 * The selectors below are still correct — they match the rendered DOM exactly — so this parser
 * becomes useful the moment it is handed rendered HTML instead of a server response. That is the
 * Tier 2 capture engine in CAPTURE_ARCHITECTURE.md §3, and it is the only route to Instagram
 * captions and media that does not require a session.
 *
 * The richer `api/v1/media/{pk}/info/` endpoint was investigated and rejected for the same class
 * of reason: `yt-dlp` reaches it only behind `if self._is_logged_in`, and its logged-out path
 * needs hard-coded TLS impersonation plus a rotating `doc_id`. See §4.3.
 */
@Singleton
class InstagramSourceExtractor @Inject constructor() : SourceExtractor {

    override val platformName: String = "INSTAGRAM"

    private companion object {
        const val FAVICON = "https://static.cdninstagram.com/rsrc.php/v3/yI/r/VsNE-OHk_8a.png"
        val SHORTCODE = Regex("""/(?:reel|reels|p|tv|share/reel|share/p)/([A-Za-z0-9_-]+)""")
        val STORY = Regex("""/stories/([^/?#]+)""")
        val HOSTS = listOf("instagram.com", "instagr.am", "ig.me")
        val AUDIO = listOf(
            Regex(""""audio_asset_title"\s*:\s*"([^"]+)""""),
            Regex(""""song_name"\s*:\s*"([^"]+)"""")
        )
    }

    // Measured 2026-09-02: the embed's server response carries no caption, author or media,
    // only script bundles. Everything this parser selects on exists solely after JS runs.
    override val requiresRenderedHtml: Boolean = true

    override val readySelector: String = ".Caption, .EmbeddedMediaImage, .CaptionUsername"

    override fun canHandle(url: String): Boolean =
        url.lowercase().let { lower -> HOSTS.any { lower.contains(it) } }

    override fun fetchUrl(url: String): String {
        val shortcode = shortcode(url) ?: return url
        return "https://www.instagram.com/p/$shortcode/embed/captioned/"
    }

    /** Share sheets hand over percent-encoded URLs often enough to decode before matching. */
    private fun shortcode(url: String): String? {
        val decoded = try {
            URLDecoder.decode(url, "UTF-8")
        } catch (e: Exception) {
            url
        }
        return SHORTCODE.find(decoded)?.groupValues?.get(1)
    }

    override suspend fun extract(url: String, content: String?): ExtractedSourceData {
        val isReel = listOf("/reel/", "/reels/", "/tv/", "share/reel").any { url.contains(it) }
        val isStory = url.contains("/stories/")
        val storyUser = STORY.find(url)?.groupValues?.get(1)

        var author = storyUser?.let { "@$it" }
        var caption: String? = null
        var image: String? = null
        var audioTitle: String? = null
        var videoUrl: String? = null

        if (!content.isNullOrBlank()) {
            try {
                val doc = Jsoup.parse(content)

                doc.selectFirst(
                    "img.EmbeddedMediaImage, img.CoverImage, img[src*=cdninstagram], img[src*=fbcdn]"
                )?.attr("src")?.replace("&amp;", "&")
                    ?.takeIf { isPostMedia(it) }
                    ?.let { image = it }

                doc.selectFirst(".CaptionUsername, a.username")?.text()?.trim()
                    ?.removePrefix("@")
                    ?.takeIf { it.isNotBlank() && !it.equals("instagram", ignoreCase = true) }
                    ?.let { author = "@$it" }

                doc.selectFirst(".Caption, .CaptionComments")?.text()?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?.let { caption = it }

                // The embed carries more in an escaped script blob than in its markup.
                val raw = doc.html().replace("\\/", "/").replace("\\u0026", "&")

                audioTitle = AUDIO.firstNotNullOfOrNull { it.find(raw)?.groupValues?.get(1) }
                    ?.takeIf { it.isNotBlank() }

                videoUrl = Regex(""""video_url"\s*:\s*"([^"]+)"""").find(raw)
                    ?.groupValues?.get(1)
                    ?.takeIf { it.startsWith("http") }

                if (image == null) {
                    image = listOf(
                        Regex(""""display_url"\s*:\s*"([^"]+)""""),
                        Regex(""""thumbnail_src"\s*:\s*"([^"]+)""""),
                        Regex("""https://[\w.-]+(?:\.cdninstagram\.com|\.fbcdn\.net)/[^\s"'<>\\]+""")
                    ).firstNotNullOfOrNull { pattern ->
                        pattern.find(raw)?.let { it.groupValues.lastOrNull()?.ifBlank { it.value } }
                            ?.takeIf { candidate -> isPostMedia(candidate) }
                    }
                }
            } catch (e: Exception) {
                // Fall through to whatever the URL alone can tell us.
            }
        }

        val label = when {
            isStory -> "Story"
            isReel -> "Reel"
            else -> "Post"
        }

        return ExtractedSourceData(
            platform = platformName,
            title = caption?.takeIf { it.isNotBlank() }?.let { firstLine(it) }
                ?: "Instagram $label${author?.let { " by $it" }.orEmpty()}",
            description = caption,
            // The music note carries the meaning without an English word, so the audio track
            // stays searchable whatever language the app is running in.
            bodyText = listOfNotNull(audioTitle?.let { "\uD83C\uDFB5 $it" }, caption)
                .joinToString("\n\n")
                .takeIf { it.isNotBlank() },
            authorHandle = author,
            community = "Instagram",
            leadImageUrl = image,
            mediaUrls = listOfNotNull(image, videoUrl),
            canonicalUrl = url.substringBefore('?'),
            faviconUrl = FAVICON,
            contentType = if (isReel || isStory) ContentType.VIDEO else ContentType.IMAGE
        )
    }

    /**
     * Instagram's own chrome — sprite sheets, glyphs, the app icon — lives on the same CDNs as
     * post media, so a plain host check would happily save the logo as the item's thumbnail.
     */
    private fun isPostMedia(imageUrl: String): Boolean {
        val lower = imageUrl.lowercase()
        val chrome = listOf(
            "rsrc.php", "static.cdninstagram", "favicon", "glyph",
            "app_icon", "instagram.com/static", "/static/images/"
        )
        if (chrome.any { lower.contains(it) }) return false
        return lower.contains("cdninstagram.com") || lower.contains("fbcdn.net")
    }

    private fun firstLine(caption: String): String {
        val line = caption.lineSequence().firstOrNull { it.isNotBlank() } ?: caption
        return if (line.length <= 80) line else line.take(77).trimEnd() + "…"
    }
}
