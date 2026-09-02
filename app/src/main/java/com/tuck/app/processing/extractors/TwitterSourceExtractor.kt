package com.tuck.app.processing.extractors

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jsoup.Jsoup
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.floor
import kotlin.math.max

/**
 * Extracts a post from X (Twitter).
 *
 * X serves empty OpenGraph tags to unauthenticated crawlers, so parsing the shared page
 * yields nothing but the handle already visible in the URL. Instead this uses X's public
 * syndication endpoint — the one its own embed widgets call — which returns the real text,
 * author and media for a status id with no cookie, key or bearer token.
 *
 * See CAPTURE_ARCHITECTURE.md §4.1.
 */
@Singleton
class TwitterSourceExtractor @Inject constructor() : SourceExtractor {

    override val platformName: String = "TWITTER"

    override val payloadIsJson: Boolean = true

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val handlePattern = Pattern.compile(
        "(?:twitter\\.com|x\\.com)/([a-zA-Z0-9_]{1,15})/status/(\\d+)",
        Pattern.CASE_INSENSITIVE
    )

    /** Matches a bare `/status/<id>` for share URLs that omit the handle (`x.com/i/status/…`). */
    private val statusIdPattern = Pattern.compile("/status(?:es)?/(\\d+)")

    override fun canHandle(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("twitter.com") || lower.contains("x.com")
    }

    override fun fetchUrl(url: String): String {
        val id = statusId(url) ?: return url
        return "https://cdn.syndication.twimg.com/tweet-result" +
            "?id=$id&lang=en&token=${syndicationToken(id)}"
    }

    override suspend fun extract(url: String, content: String?): ExtractedSourceData {
        val authorHandle = handlePattern.matcher(url).let { if (it.find()) "@${it.group(1)}" else null }

        if (!content.isNullOrBlank()) {
            parseSyndication(content, authorHandle)?.let { return it }
            // A non-JSON body means fetchUrl fell through to the shared page (no status id in
            // the URL — a profile or a search link). OpenGraph is all that is on offer there.
            parseOpenGraph(content, authorHandle)?.let { return it }
        }

        return ExtractedSourceData(
            platform = platformName,
            title = "Post on X by ${authorHandle ?: "user"}",
            authorHandle = authorHandle
        )
    }

    private fun parseSyndication(content: String, urlHandle: String?): ExtractedSourceData? {
        val root = try {
            json.parseToJsonElement(content) as? JsonObject ?: return null
        } catch (e: Exception) {
            return null
        }
        // The endpoint answers errors with a JSON body too; a post always carries text or media.
        if (root["text"] == null && root["mediaDetails"] == null && root["photos"] == null) return null

        val user = root["user"]?.jsonObject
        val handle = user?.string("screen_name")?.let { "@$it" } ?: urlHandle
        val bodyText = renderText(root)

        val media = mediaUrls(root)
        val quoted = root["quoted_tweet"]?.jsonObject
        val fullBody = buildString {
            append(bodyText)
            if (quoted != null) {
                val quotedHandle = quoted["user"]?.jsonObject?.string("screen_name")
                val quotedText = renderText(quoted)
                if (quotedText.isNotBlank()) {
                    append("\n\nQuoting @").append(quotedHandle ?: "user").append(":\n")
                    append(quotedText)
                }
            }
        }.trim()

        return ExtractedSourceData(
            platform = platformName,
            title = titleFrom(fullBody, handle),
            description = fullBody.takeIf { it.isNotBlank() },
            bodyText = fullBody.takeIf { it.isNotBlank() },
            authorHandle = handle,
            authorDisplay = user?.string("name"),
            score = root["favorite_count"]?.jsonPrimitive?.intOrNull ?: 0,
            commentCount = root["conversation_count"]?.jsonPrimitive?.intOrNull ?: 0,
            postedAt = parseTimestamp(root.string("created_at")),
            leadImageUrl = media.firstOrNull(),
            mediaUrls = media,
            rawJson = content
        )
    }

    /**
     * Body text as the post actually reads: t.co shorteners for the post's own media are noise
     * and get dropped, while link shorteners are swapped for the destination they hide so the
     * archived copy keeps a URL that still means something years later.
     */
    private fun renderText(post: JsonObject): String {
        var text = post.string("text") ?: post.string("full_text") ?: ""
        val entities = post["entities"]?.jsonObject

        entities?.get("media")?.jsonArray?.forEach { media ->
            media.jsonObject.string("url")?.let { text = text.replace(it, "") }
        }
        entities?.get("urls")?.jsonArray?.forEach { entity ->
            val shortUrl = entity.jsonObject.string("url") ?: return@forEach
            val expanded = entity.jsonObject.string("expanded_url") ?: return@forEach
            text = text.replace(shortUrl, expanded)
        }

        // Syndication returns HTML entities in text (&amp;, &lt;, &gt;).
        return Jsoup.parse(text).text().trim()
    }

    private fun mediaUrls(post: JsonObject): List<String> {
        val details = post["mediaDetails"]?.jsonArray?.map { it.jsonObject }
            ?: post["photos"]?.jsonArray?.map { it.jsonObject }
            ?: return emptyList()

        return details.mapNotNull { media ->
            when (media.string("type")) {
                "video", "animated_gif" -> {
                    // Highest-bitrate progressive MP4; the HLS variants are useless offline.
                    media["video_info"]?.jsonObject?.get("variants")?.jsonArray
                        ?.map { it.jsonObject }
                        ?.filter { it.string("content_type") == "video/mp4" && it.string("url") != null }
                        ?.maxByOrNull { it["bitrate"]?.jsonPrimitive?.intOrNull ?: 0 }
                        ?.string("url")
                        ?: media.string("media_url_https")
                }
                else -> media.string("media_url_https") ?: media.string("url")
            }
        }.distinct()
    }

    private fun titleFrom(body: String, handle: String?): String {
        if (body.isBlank()) return "Post on X by ${handle ?: "user"}"
        val firstLine = body.lineSequence().firstOrNull { it.isNotBlank() } ?: body
        return if (firstLine.length <= 80) firstLine else firstLine.take(77).trimEnd() + "…"
    }

    private fun parseTimestamp(raw: String?): Long? {
        if (raw.isNullOrBlank()) return null
        val formats = listOf("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "yyyy-MM-dd'T'HH:mm:ss'Z'")
        for (format in formats) {
            try {
                val parser = SimpleDateFormat(format, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                return parser.parse(raw)?.time
            } catch (e: Exception) {
                // Try the next shape.
            }
        }
        return null
    }

    private fun parseOpenGraph(content: String, authorHandle: String?): ExtractedSourceData? {
        return try {
            val doc = Jsoup.parse(content)
            val ogTitle = doc.select("meta[property=og:title]").attr("content").ifBlank { doc.title() }
            val ogDesc = doc.select("meta[property=og:description]").attr("content")
            val ogImage = doc.select("meta[property=og:image]").attr("content").takeIf { it.isNotBlank() }
            if (ogTitle.isNullOrBlank() && ogDesc.isBlank()) return null

            ExtractedSourceData(
                platform = platformName,
                title = ogTitle.ifBlank { "Post on X by ${authorHandle ?: "user"}" },
                description = ogDesc.takeIf { it.isNotBlank() },
                bodyText = ogDesc.takeIf { it.isNotBlank() },
                authorHandle = authorHandle,
                leadImageUrl = ogImage,
                mediaUrls = listOfNotNull(ogImage)
            )
        } catch (e: Exception) {
            null
        }
    }

    fun statusId(url: String): String? {
        val handleMatch = handlePattern.matcher(url)
        if (handleMatch.find()) return handleMatch.group(2)
        val idMatch = statusIdPattern.matcher(url)
        return if (idMatch.find()) idMatch.group(1) else null
    }

    private fun JsonObject.string(key: String): String? =
        this[key]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() && it != "null" }

    companion object {

        /**
         * The token X's syndication endpoint expects, derived from the status id alone.
         *
         * The reference implementation is JavaScript:
         * `((id / 1e15) * Math.PI).toString(36).replace(/(0+|\.)/g, '')`
         *
         * Both halves have to match exactly or the endpoint 404s, so the division runs through
         * a [Double] (as a JS number does) and the base-36 rendering is a port of V8's
         * `DoubleToRadixCString` rather than anything from the JDK — `Double.toString` and
         * `BigDecimal` both produce a different digit string.
         */
        fun syndicationToken(statusId: String): String {
            val id = statusId.toDoubleOrNull() ?: return ""
            val value = (id / 1e15) * Math.PI
            return doubleToRadixString(value, 36).replace(Regex("(0+|\\.)"), "")
        }

        private const val DIGITS = "0123456789abcdefghijklmnopqrstuvwxyz"

        /** Smallest double whose ulp exceeds 1, above which no fractional digits exist. */
        private const val NO_FRACTIONAL_BITS = 9.007199254740992E15 // 2^53

        /**
         * Port of V8's `DoubleToRadixCString`, i.e. `Number.prototype.toString(radix)` for a
         * non-integer. The spec leaves the fractional digits implementation-defined, so
         * matching the browser means matching V8 specifically: emit digits only while they
         * remain significant against the double's own ulp, rounding half to even and carrying
         * back through digits already written.
         */
        internal fun doubleToRadixString(value: Double, radix: Int): String {
            require(radix in 2..36) { "radix out of range: $radix" }
            if (value.isNaN() || value.isInfinite()) return value.toString()
            if (value == 0.0) return "0"

            val negative = value < 0
            var v = if (negative) -value else value

            var integer = floor(v)
            var fraction = v - integer
            // Only compute fractional digits up to the input double's precision.
            var delta = max(java.lang.Double.MIN_VALUE, 0.5 * (Math.nextUp(v) - v))

            val fractionDigits = StringBuilder()
            if (fraction >= delta) {
                while (true) {
                    fraction *= radix
                    delta *= radix
                    var digit = fraction.toInt()
                    fractionDigits.append(DIGITS[digit])
                    fraction -= digit

                    if (fraction > 0.5 || (fraction == 0.5 && (digit and 1) == 1)) {
                        if (fraction + delta > 1.0) {
                            // Round up, carrying back through the digits already emitted.
                            var i = fractionDigits.length - 1
                            while (true) {
                                if (i < 0) {
                                    integer += 1.0
                                    fractionDigits.setLength(0)
                                    break
                                }
                                val c = fractionDigits[i]
                                digit = if (c > '9') c - 'a' + 10 else c - '0'
                                if (digit + 1 < radix) {
                                    fractionDigits.setCharAt(i, DIGITS[digit + 1])
                                    fractionDigits.setLength(i + 1)
                                    break
                                }
                                i--
                            }
                            break
                        }
                    }
                    if (fraction < delta) break
                }
            }

            // Low-order digit first; reversed below.
            val integerDigits = StringBuilder()
            while (integer / radix >= NO_FRACTIONAL_BITS) {
                integer /= radix
                integerDigits.append('0')
            }
            do {
                val remainder = integer % radix
                integerDigits.append(DIGITS[remainder.toInt()])
                integer = (integer - remainder) / radix
            } while (integer > 0)

            return buildString {
                if (negative) append('-')
                append(integerDigits.reverse())
                if (fractionDigits.isNotEmpty()) append('.').append(fractionDigits)
            }
        }
    }
}
