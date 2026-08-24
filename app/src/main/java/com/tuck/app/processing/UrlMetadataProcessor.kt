package com.tuck.app.processing

import com.tuck.app.domain.model.ContentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

data class UrlMetadata(
    val normalizedUrl: String,
    val canonicalUrl: String?,
    val domain: String,
    val title: String?,
    val description: String?,
    val ogImageUrl: String?,
    val faviconUrl: String?,
    val inferredContentType: ContentType = ContentType.URL,
    val author: String? = null,
    val fullTextContent: String? = null,
    val comments: List<com.tuck.app.domain.model.SavedComment> = emptyList()
)

@Singleton
class UrlMetadataProcessor @Inject constructor() {

    private val trackingParameters = setOf(
        "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content",
        "fbclid", "gclid", "msclkid", "mc_eid", "igshid", "igsh", "ref", "ref_src",
        "twclid", "_hsenc", "_hsmi", "yclid", "soc_src", "soc_trk", "feature"
    )

    fun cleanUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        val questionIndex = trimmed.indexOf('?')
        if (questionIndex == -1) return trimmed

        val baseUrl = trimmed.substring(0, questionIndex)
        val queryPart = trimmed.substring(questionIndex + 1)
        val fragmentIndex = queryPart.indexOf('#')
        val query = if (fragmentIndex != -1) queryPart.substring(0, fragmentIndex) else queryPart
        val fragment = if (fragmentIndex != -1) queryPart.substring(fragmentIndex) else ""

        val keptParams = query.split('&')
            .filter { it.isNotBlank() }
            .filterNot { param ->
                val paramName = param.substringBefore('=').lowercase().trim()
                trackingParameters.contains(paramName)
            }

        return if (keptParams.isEmpty()) {
            baseUrl + fragment
        } else {
            baseUrl + "?" + keptParams.joinToString("&") + fragment
        }
    }

    fun extractDomain(url: String): String {
        return try {
            val uri = URI(url)
            var host = uri.host ?: ""
            if (host.startsWith("www.")) {
                host = host.substring(4)
            }
            host
        } catch (e: Exception) {
            url.substringAfter("://").substringBefore("/").removePrefix("www.")
        }
    }

    suspend fun extractMetadata(rawUrl: String): UrlMetadata = withContext(Dispatchers.IO) {
        val cleaned = cleanUrl(rawUrl)
        val domain = extractDomain(cleaned)

        // 1. Specialized Handler: Reddit
        if (domain.contains("reddit.com") || domain.contains("redd.it")) {
            val redditMeta = fetchRedditMetadata(cleaned, domain)
            if (redditMeta != null) return@withContext redditMeta
        }

        // 2. Specialized Handler: Instagram (Reels & Posts)
        if (domain.contains("instagram.com")) {
            val instaMeta = fetchInstagramMetadata(cleaned, domain)
            if (instaMeta != null) return@withContext instaMeta
        }

        // 3. Specialized Handler: YouTube / YouTube Shorts
        if (domain.contains("youtube.com") || domain.contains("youtu.be")) {
            val ytMeta = fetchYouTubeMetadata(cleaned, domain)
            if (ytMeta != null) return@withContext ytMeta
        }

        // 4. Specialized Handler: TikTok
        if (domain.contains("tiktok.com")) {
            val tiktokMeta = fetchTikTokMetadata(cleaned, domain)
            if (tiktokMeta != null) return@withContext tiktokMeta
        }

        // 5. Specialized Handler: LinkedIn
        if (domain.contains("linkedin.com")) {
            val linkedInMeta = fetchLinkedInMetadata(cleaned, domain)
            if (linkedInMeta != null) return@withContext linkedInMeta
        }

        // 6. General Webpage Handler (OpenGraph / HTML parser)
        fetchGeneralWebpageMetadata(cleaned, domain)
    }

    private fun fetchRedditMetadata(url: String, domain: String): UrlMetadata? {
        return try {
            val cleanBase = url.substringBefore('?').removeSuffix("/")
            val jsonUrl = if (cleanBase.endsWith(".json")) cleanBase else "$cleanBase.json"

            val connection = URL(jsonUrl).openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            connection.connect()

            if (connection.responseCode in 200..299) {
                val jsonText = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                val rootArray = JSONArray(jsonText)
                if (rootArray.length() > 0) {
                    val listing = rootArray.getJSONObject(0)
                    val children = listing.getJSONObject("data").getJSONArray("children")
                    if (children.length() > 0) {
                        val postData = children.getJSONObject(0).getJSONObject("data")
                        val title = postData.optString("title").takeIf { it.isNotBlank() }
                        val selftext = postData.optString("selftext").takeIf { it.isNotBlank() }
                        val author = postData.optString("author").takeIf { it.isNotBlank() }?.let { "u/$it" }
                        val subreddit = postData.optString("subreddit_name_prefixed").takeIf { it.isNotBlank() } ?: "Reddit"
                        val isVideo = postData.optBoolean("is_video", false)

                        var previewImage: String? = null
                        val preview = postData.optJSONObject("preview")
                        if (preview != null) {
                            val images = preview.optJSONArray("images")
                            if (images != null && images.length() > 0) {
                                previewImage = images.getJSONObject(0).getJSONObject("source").optString("url")
                                    .replace("&amp;", "&")
                            }
                        }
                        if (previewImage.isNullOrBlank()) {
                            val thumb = postData.optString("thumbnail")
                            if (thumb.startsWith("http")) {
                                previewImage = thumb
                            }
                        }

                        // Extract Top Comments from second listing
                        val extractedComments = mutableListOf<com.tuck.app.domain.model.SavedComment>()
                        if (rootArray.length() > 1) {
                            val commentsData = rootArray.getJSONObject(1).optJSONObject("data")
                            val commentChildren = commentsData?.optJSONArray("children")
                            if (commentChildren != null) {
                                for (i in 0 until minOf(commentChildren.length(), 15)) {
                                    val cData = commentChildren.optJSONObject(i)?.optJSONObject("data")
                                    if (cData != null) {
                                        val body = cData.optString("body", "").trim()
                                        val cAuthor = cData.optString("author", "")
                                        val score = cData.optInt("score", 0)
                                        val createdUtc = cData.optLong("created_utc", 0L)
                                        if (body.isNotBlank() && body != "[deleted]" && body != "[removed]") {
                                            extractedComments.add(
                                                com.tuck.app.domain.model.SavedComment(
                                                    author = if (cAuthor.isNotBlank()) "u/$cAuthor" else "u/anonymous",
                                                    text = body,
                                                    score = score,
                                                    timestamp = if (createdUtc > 0) createdUtc * 1000 else null
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        val descriptionText = buildString {
                            if (!author.isNullOrBlank()) append("Posted by $author in $subreddit\n\n")
                            if (!selftext.isNullOrBlank()) append(selftext)
                        }.trim()

                        val fullText = buildString {
                            if (!selftext.isNullOrBlank()) append(selftext).append("\n\n")
                            if (extractedComments.isNotEmpty()) {
                                append("Top Community Comments:\n")
                                for (c in extractedComments) {
                                    append("${c.author}: ${c.text}\n")
                                }
                            }
                        }.trim()

                        return UrlMetadata(
                            normalizedUrl = url,
                            canonicalUrl = cleanBase,
                            domain = subreddit,
                            title = title ?: "$subreddit Post",
                            description = descriptionText.takeIf { it.isNotBlank() },
                            ogImageUrl = previewImage,
                            faviconUrl = "https://www.redditstatic.com/desktop2x/img/favicon/favicon-96x96.png",
                            inferredContentType = if (isVideo) ContentType.VIDEO else ContentType.URL,
                            author = author,
                            fullTextContent = fullText.takeIf { it.isNotBlank() },
                            comments = extractedComments
                        )
                    }
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun fetchLinkedInMetadata(url: String, domain: String): UrlMetadata? {
        return try {
            val doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .timeout(5000)
                .followRedirects(true)
                .ignoreHttpErrors(true)
                .get()

            val ogTitle = doc.select("meta[property=og:title]").attr("content").trim()
            val ogDesc = doc.select("meta[property=og:description]").attr("content").trim()
            val ogImage = doc.select("meta[property=og:image]").attr("content").trim().replace("&amp;", "&")

            val title = if (ogTitle.isNotBlank()) ogTitle else "LinkedIn Post"

            UrlMetadata(
                normalizedUrl = url,
                canonicalUrl = url.substringBefore('?'),
                domain = "LinkedIn",
                title = title,
                description = ogDesc.takeIf { it.isNotBlank() },
                ogImageUrl = ogImage.takeIf { it.isNotBlank() },
                faviconUrl = "https://static.licdn.com/aero-v1/sc/h/al2o9zrvru7aqj8e1x2rzsrca",
                inferredContentType = ContentType.URL,
                fullTextContent = ogDesc
            )
        } catch (e: Exception) {
            UrlMetadata(
                normalizedUrl = url,
                canonicalUrl = url.substringBefore('?'),
                domain = "LinkedIn",
                title = "LinkedIn Post",
                description = null,
                ogImageUrl = null,
                faviconUrl = null,
                inferredContentType = ContentType.URL
            )
        }
    }

    private fun fetchInstagramMetadata(url: String, domain: String): UrlMetadata? {
        val isReel = url.contains("/reel/") || url.contains("/reels/") || url.contains("/tv/") || url.contains("share/reel")
        val isStory = url.contains("/stories/")

        var title = if (isReel) "Instagram Reel" else if (isStory) "Instagram Story" else "Instagram Post"
        var description: String? = null
        var previewImage: String? = null
        var author: String? = null

        // Extract story username if present
        if (isStory) {
            val storyMatch = Regex("""/stories/([^/?#]+)""").find(url)
            if (storyMatch != null) {
                val username = storyMatch.groupValues[1]
                author = "@$username"
                title = "Story by @$username"
                description = "Instagram Story from $author"
            }
        }

        // Extract shortcode for Reel/Post
        val shortcodeMatch = Regex("""/(?:reel|reels|p|tv|share/reel|share/p)/([a-zA-Z0-9_-]+)""").find(url)
        val shortcode = shortcodeMatch?.groupValues?.get(1)

        // Strategy 1: Instagram Direct Media Redirect (/media/?size=l)
        if (!shortcode.isNullOrBlank()) {
            try {
                val mediaUrl = "https://www.instagram.com/p/$shortcode/media/?size=l"
                val response = Jsoup.connect(mediaUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                    .timeout(6000)
                    .followRedirects(true)
                    .ignoreHttpErrors(true)
                    .execute()

                val finalUrl = response.url().toString()
                if (finalUrl.contains("cdninstagram.com") || finalUrl.contains("fbcdn.net")) {
                    previewImage = finalUrl
                }
            } catch (e: Exception) {
                // Proceed to next strategy
            }
        }

        // Strategy 2: Instagram Embed Captioned HTML & Script unescaping
        if (!shortcode.isNullOrBlank() && (previewImage.isNullOrBlank() || description.isNullOrBlank())) {
            try {
                val embedUrl = "https://www.instagram.com/p/$shortcode/embed/captioned/"
                val embedDoc = Jsoup.connect(embedUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                    .timeout(6000)
                    .followRedirects(true)
                    .ignoreHttpErrors(true)
                    .get()

                // Extract image src from embed HTML
                val imgTag = embedDoc.select("img.EmbeddedMediaImage, img.CoverImage, img[src*=\"cdninstagram\"], img[src*=\"fbcdn\"]").first()
                if (imgTag != null) {
                    val src = imgTag.attr("src").replace("&amp;", "&")
                    if (src.isNotBlank() && previewImage.isNullOrBlank()) {
                        previewImage = src
                    }
                }

                // Extract author username
                val userTag = embedDoc.select(".CaptionUsername, a.username, a[href*=\"instagram.com/\"]").first()
                if (userTag != null && userTag.text().isNotBlank()) {
                    val rawUser = userTag.text().trim().removePrefix("@")
                    if (rawUser.isNotBlank() && !rawUser.equals("instagram", ignoreCase = true)) {
                        author = "@$rawUser"
                        if (isReel) title = "Reel by $author"
                        else if (!isStory) title = "Post by $author"
                    }
                }

                // Extract caption text
                val captionTag = embedDoc.select(".Caption, .CaptionComments, .CaptionUsername + span").first()
                if (captionTag != null && captionTag.text().isNotBlank()) {
                    val fullCaption = captionTag.text().trim()
                    description = fullCaption
                    if (!fullCaption.startsWith("@") && (title == "Instagram Reel" || title == "Instagram Post")) {
                        title = fullCaption.take(70)
                    }
                }

                // Deep regex scan on unescaped HTML scripts for display_url / cdn image
                if (previewImage.isNullOrBlank()) {
                    val rawHtml = embedDoc.html().replace("\\/", "/").replace("\\u0026", "&")
                    val displayUrlMatch = Regex(""""display_url"\s*:\s*"([^"]+)"""").find(rawHtml)
                        ?: Regex(""""thumbnail_src"\s*:\s*"([^"]+)"""").find(rawHtml)
                        ?: Regex(""""thumbnail_url"\s*:\s*"([^"]+)"""").find(rawHtml)
                        ?: Regex("""https://[a-zA-Z0-9._-]+(?:\.cdninstagram\.com|\.fbcdn\.net)/[^\s"'<>\\]+""").find(rawHtml)

                    if (displayUrlMatch != null) {
                        previewImage = displayUrlMatch.groupValues.lastOrNull() ?: displayUrlMatch.value
                    }
                }
            } catch (e: Exception) {
                // Ignore and proceed
            }
        }

        // Strategy 3: Instagram public oEmbed API
        if (!shortcode.isNullOrBlank() && (previewImage.isNullOrBlank() || description.isNullOrBlank())) {
            try {
                val oEmbedUrl = "https://api.instagram.com/oembed/?url=https://www.instagram.com/p/$shortcode/"
                val response = Jsoup.connect(oEmbedUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                    .timeout(5000)
                    .ignoreContentType(true)
                    .ignoreHttpErrors(true)
                    .execute()

                val json = JSONObject(response.body())
                if (json.has("thumbnail_url") && previewImage.isNullOrBlank()) {
                    previewImage = json.getString("thumbnail_url")
                }
                if (json.has("author_name") && author.isNullOrBlank()) {
                    author = "@${json.getString("author_name")}"
                }
                if (json.has("title") && description.isNullOrBlank()) {
                    val oTitle = json.getString("title")
                    if (oTitle.isNotBlank()) {
                        description = oTitle
                        title = oTitle.take(70)
                    }
                }
            } catch (e: Exception) {
                // Ignore
            }
        }

        // Strategy 4: Facebook / OpenGraph Crawler Headers
        if (previewImage.isNullOrBlank() || description.isNullOrBlank()) {
            try {
                val doc = Jsoup.connect(url)
                    .userAgent("facebookexternalhit/1.1 (+http://www.facebook.com/externalhit_uatext.php)")
                    .timeout(6000)
                    .followRedirects(true)
                    .ignoreHttpErrors(true)
                    .get()

                val ogTitle = doc.select("meta[property=og:title]").attr("content").trim()
                val ogDesc = doc.select("meta[property=og:description]").attr("content").trim()
                val ogImage = doc.select("meta[property=og:image], meta[property=og:image:secure_url], meta[name=twitter:image]").attr("content").trim().replace("&amp;", "&")

                if (ogImage.isNotBlank() && previewImage.isNullOrBlank()) {
                    previewImage = ogImage
                }
                if (ogDesc.isNotBlank() && description.isNullOrBlank()) {
                    description = ogDesc
                }
                if (ogTitle.isNotBlank() && (title == "Instagram Reel" || title == "Instagram Post")) {
                    title = ogTitle
                }
            } catch (e: Exception) {
                // Ignore
            }
        }

        return UrlMetadata(
            normalizedUrl = url,
            canonicalUrl = url.substringBefore('?'),
            domain = "Instagram",
            title = title,
            description = description,
            ogImageUrl = previewImage,
            faviconUrl = "https://static.cdninstagram.com/rsrc.php/v3/yI/r/VsNE-OHk_8a.png",
            inferredContentType = if (isReel || isStory) ContentType.VIDEO else ContentType.IMAGE,
            author = author,
            fullTextContent = description
        )
    }

    private fun fetchYouTubeMetadata(url: String, domain: String): UrlMetadata? {
        return try {
            val isShort = url.contains("/shorts/")
            val oembedUrl = "https://www.youtube.com/oembed?url=${url}&format=json"

            val connection = URL(oembedUrl).openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.connect()

            if (connection.responseCode in 200..299) {
                val jsonText = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                val json = JSONObject(jsonText)
                val title = json.optString("title")
                val author = json.optString("author_name")
                val thumb = json.optString("thumbnail_url")

                return UrlMetadata(
                    normalizedUrl = url,
                    canonicalUrl = url.substringBefore("&"),
                    domain = "YouTube",
                    title = title,
                    description = if (author.isNotBlank()) "Video by $author on YouTube" else null,
                    ogImageUrl = thumb.takeIf { it.isNotBlank() },
                    faviconUrl = "https://www.youtube.com/s/desktop/f1721590/img/favicon.ico",
                    inferredContentType = ContentType.VIDEO,
                    author = author
                )
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun fetchTikTokMetadata(url: String, domain: String): UrlMetadata? {
        return try {
            val oembedUrl = "https://www.tiktok.com/oembed?url=${url}"
            val connection = URL(oembedUrl).openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.connect()

            if (connection.responseCode in 200..299) {
                val jsonText = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                val json = JSONObject(jsonText)
                val title = json.optString("title")
                val author = json.optString("author_name")
                val thumb = json.optString("thumbnail_url")

                return UrlMetadata(
                    normalizedUrl = url,
                    canonicalUrl = url.substringBefore('?'),
                    domain = "TikTok",
                    title = title.ifBlank { "TikTok Video by $author" },
                    description = title,
                    ogImageUrl = thumb.takeIf { it.isNotBlank() },
                    faviconUrl = null,
                    inferredContentType = ContentType.VIDEO,
                    author = author
                )
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun fetchGeneralWebpageMetadata(url: String, domain: String): UrlMetadata {
        var canonical = url
        var title: String? = null
        var description: String? = null
        var ogImageUrl: String? = null
        var faviconUrl: String? = null

        try {
            val document = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36")
                .timeout(6000)
                .followRedirects(true)
                .ignoreHttpErrors(true)
                .get()

            title = document.select("meta[property=og:title]").attr("content").ifBlank {
                document.select("meta[name=twitter:title]").attr("content").ifBlank {
                    document.title()
                }
            }.trim().takeIf { it.isNotBlank() }

            description = document.select("meta[property=og:description]").attr("content").ifBlank {
                document.select("meta[name=twitter:description]").attr("content").ifBlank {
                    document.select("meta[name=description]").attr("content")
                }
            }.trim().takeIf { it.isNotBlank() }

            val ogImage = document.select("meta[property=og:image]").attr("content").ifBlank {
                document.select("meta[name=twitter:image]").attr("content")
            }.trim()
            if (ogImage.isNotBlank()) {
                ogImageUrl = document.baseUri().let { base ->
                    try { URI(base).resolve(ogImage).toString() } catch (e: Exception) { ogImage }
                }
            }

            val canonicalTag = document.select("link[rel=canonical]").attr("href").trim()
            if (canonicalTag.isNotBlank()) {
                canonical = try {
                    URI(url).resolve(canonicalTag).toString()
                } catch (e: Exception) {
                    canonicalTag
                }
                canonical = cleanUrl(canonical)
            }

            val iconTag = document.select("link[rel~=(?i)^(shortcut|icon|apple-touch-icon)]").attr("href").trim()
            if (iconTag.isNotBlank()) {
                faviconUrl = try {
                    URI(url).resolve(iconTag).toString()
                } catch (e: Exception) {
                    iconTag
                }
            }
        } catch (e: Exception) {
            // Safe fallback if offline or request blocked: metadata is optional
        }

        return UrlMetadata(
            normalizedUrl = url,
            canonicalUrl = canonical,
            domain = domain,
            title = title,
            description = description,
            ogImageUrl = ogImageUrl,
            faviconUrl = faviconUrl
        )
    }
}
