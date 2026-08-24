package com.tuck.app.processing

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.tuck.app.domain.model.ContentType
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

import org.json.JSONArray
import org.json.JSONObject

data class ParsedShareContent(
    val contentType: ContentType,
    val title: String,
    val text: String? = null,
    val url: String? = null,
    val streamUris: List<Uri> = emptyList(),
    val mimeType: String? = null,
    val sourceApp: String? = null,
    val extraMetadata: Map<String, String> = emptyMap(),
    val rawPayloadJson: String? = null
)

@Singleton
class ShareParser @Inject constructor(
    @ApplicationContext private val context: Context,
    private val urlMetadataProcessor: UrlMetadataProcessor
) {
    private val urlPattern = Pattern.compile(
        "https?://[a-zA-Z0-9.-]+(?:\\.[a-zA-Z]{2,})+(?::\\d+)?(?:/[^\\s]*)?",
        Pattern.CASE_INSENSITIVE
    )

    private val geoUriPattern = Pattern.compile(
        "geo:(-?\\d+\\.?\\d*),(-?\\d+\\.?\\d*)(?:\\?q=([^\\s]+))?",
        Pattern.CASE_INSENSITIVE
    )

    fun parseIntent(intent: Intent, callerPackage: String? = null): ParsedShareContent? {
        val action = intent.action ?: return null
        val type = intent.type ?: "*/*"
        val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)
            ?: intent.getStringExtra(Intent.EXTRA_TITLE)

        val referrer = getReferrerHost(intent) ?: callerPackage
        val rawPayload = serializeRawIntent(intent, callerPackage)

        val parsed = when (action) {
            Intent.ACTION_SEND -> parseSingleSend(intent, type, subject, referrer)
            Intent.ACTION_SEND_MULTIPLE -> parseMultipleSend(intent, type, subject, referrer)
            Intent.ACTION_PROCESS_TEXT -> parseProcessText(intent, referrer)
            else -> null
        }

        return parsed?.copy(rawPayloadJson = rawPayload)
    }

    private fun parseProcessText(intent: Intent, sourceApp: String?): ParsedShareContent? {
        val text = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()?.trim()
            ?: intent.getStringExtra(Intent.EXTRA_TEXT)?.trim()
            ?: return null

        if (text.isBlank()) return null

        val urlMatcher = urlPattern.matcher(text)
        if (urlMatcher.matches()) {
            val domain = urlMetadataProcessor.extractDomain(text)
            return ParsedShareContent(
                contentType = ContentType.URL,
                title = domain.ifBlank { "Highlighted Link" },
                text = null,
                url = text,
                streamUris = emptyList(),
                mimeType = "text/uri-list",
                sourceApp = sourceApp
            )
        }

        val titlePreview = text.lines().firstOrNull { it.isNotBlank() }?.take(60) ?: "Selection"
        return ParsedShareContent(
            contentType = ContentType.TEXT,
            title = titlePreview,
            text = text,
            url = null,
            streamUris = emptyList(),
            mimeType = "text/plain",
            sourceApp = sourceApp
        )
    }

    private fun parseSingleSend(intent: Intent, type: String, subject: String?, sourceApp: String?): ParsedShareContent {
        val extraText = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim()
        val extraStream = getStreamUri(intent)

        // 1. If stream URI is present (Image, PDF, Audio, Video, Document, Contact)
        if (extraStream != null) {
            val resolvedMime = intent.type ?: context.contentResolver.getType(extraStream) ?: type
            val lowerMime = resolvedMime.lowercase()

            // Check if vCard contact stream
            if (lowerMime.contains("vcard") || extraStream.path?.endsWith(".vcf", ignoreCase = true) == true) {
                val contactInfo = parseVCardFromUri(extraStream)
                val title = subject ?: contactInfo.name ?: "Contact Card"
                return ParsedShareContent(
                    contentType = ContentType.CONTACT,
                    title = title,
                    text = contactInfo.formattedText.ifBlank { extraText },
                    url = null,
                    streamUris = listOf(extraStream),
                    mimeType = "text/x-vcard",
                    sourceApp = sourceApp,
                    extraMetadata = contactInfo.toMap()
                )
            }

            val contentType = determineContentTypeFromMime(resolvedMime)
            val title = subject ?: generateTitleForStream(extraStream, contentType)

            return ParsedShareContent(
                contentType = contentType,
                title = title,
                text = extraText,
                url = null,
                streamUris = listOf(extraStream),
                mimeType = resolvedMime,
                sourceApp = sourceApp
            )
        }

        // 2. If Text / URL / Geo is present
        if (!extraText.isNullOrBlank()) {
            // Check for vCard block in raw text
            if (extraText.contains("BEGIN:VCARD", ignoreCase = true)) {
                val contactInfo = parseVCardText(extraText)
                val title = subject ?: contactInfo.name ?: "Contact Card"
                return ParsedShareContent(
                    contentType = ContentType.CONTACT,
                    title = title,
                    text = contactInfo.formattedText,
                    url = null,
                    streamUris = emptyList(),
                    mimeType = "text/x-vcard",
                    sourceApp = sourceApp,
                    extraMetadata = contactInfo.toMap()
                )
            }

            // Check for Geo URI (e.g. geo:37.7749,-122.4194?q=San+Francisco)
            val geoMatcher = geoUriPattern.matcher(extraText)
            if (geoMatcher.find()) {
                val lat = geoMatcher.group(1) ?: "0"
                val lng = geoMatcher.group(2) ?: "0"
                val query = geoMatcher.group(3)?.replace("+", " ") ?: ""
                val title = subject?.ifBlank { null } ?: query.ifBlank { "Location: $lat, $lng" }
                return ParsedShareContent(
                    contentType = ContentType.LOCATION,
                    title = title,
                    text = "Coordinates: $lat, $lng\n${if (query.isNotBlank()) "Place: $query" else ""}".trim(),
                    url = "https://maps.google.com/?q=$lat,$lng",
                    streamUris = emptyList(),
                    mimeType = "text/plain",
                    sourceApp = sourceApp,
                    extraMetadata = mapOf("latitude" to lat, "longitude" to lng, "query" to query)
                )
            }

            // Check for Web Maps URLs
            val urlMatcher = urlPattern.matcher(extraText)
            if (urlMatcher.find()) {
                val foundUrl = urlMatcher.group()
                val remainingText = extraText.replace(foundUrl, "").trim()
                val domain = urlMetadataProcessor.extractDomain(foundUrl)

                if (domain.contains("maps.google.com") || domain.contains("maps.app.goo.gl") ||
                    domain.contains("maps.apple.com") || domain.contains("openstreetmap.org")) {
                    val title = subject?.ifBlank { null } ?: remainingText.ifBlank { "Saved Location" }
                    return ParsedShareContent(
                        contentType = ContentType.LOCATION,
                        title = title,
                        text = remainingText.takeIf { it.isNotBlank() },
                        url = foundUrl,
                        streamUris = emptyList(),
                        mimeType = "text/uri-list",
                        sourceApp = sourceApp
                    )
                }

                val inferredType = when {
                    domain.contains("instagram.com") && (foundUrl.contains("/reel/") || foundUrl.contains("/reels/") || foundUrl.contains("/tv/")) -> ContentType.VIDEO
                    domain.contains("youtube.com") || domain.contains("youtu.be") || domain.contains("tiktok.com") -> ContentType.VIDEO
                    else -> ContentType.URL
                }

                val title = subject?.ifBlank { null }
                    ?: if (remainingText.isNotBlank() && remainingText.length < 80) remainingText
                    else if (domain.contains("instagram.com") && (foundUrl.contains("/reel/") || foundUrl.contains("/reels/"))) "Instagram Reel"
                    else if (domain.contains("instagram.com")) "Instagram Post"
                    else if (domain.contains("reddit.com") || domain.contains("redd.it")) "Reddit Post"
                    else if (domain.contains("linkedin.com")) "LinkedIn Post"
                    else if (domain.contains("twitter.com") || domain.contains("x.com")) "Post on X"
                    else if (domain.contains("youtube.com") || domain.contains("youtu.be")) "YouTube Video"
                    else if (domain.contains("tiktok.com")) "TikTok Video"
                    else domain.ifBlank { "Shared Link" }

                return ParsedShareContent(
                    contentType = inferredType,
                    title = title,
                    text = remainingText.takeIf { it.isNotBlank() },
                    url = foundUrl,
                    streamUris = emptyList(),
                    mimeType = "text/uri-list",
                    sourceApp = sourceApp
                )
            } else {
                // Plain Text Note
                val title = subject?.ifBlank { null }
                    ?: extraText.lines().firstOrNull { it.isNotBlank() }?.take(60)
                    ?: "Note"

                return ParsedShareContent(
                    contentType = ContentType.TEXT,
                    title = title,
                    text = extraText,
                    url = null,
                    streamUris = emptyList(),
                    mimeType = "text/plain",
                    sourceApp = sourceApp
                )
            }
        }

        return ParsedShareContent(
            contentType = ContentType.UNKNOWN,
            title = subject ?: "Shared Item",
            text = null,
            url = null,
            streamUris = emptyList(),
            mimeType = type,
            sourceApp = sourceApp
        )
    }

    private fun parseMultipleSend(intent: Intent, type: String, subject: String?, sourceApp: String?): ParsedShareContent {
        val streamList = getStreamUris(intent)
        val isAllImages = type.startsWith("image/")

        val contentType = if (isAllImages && streamList.size > 1) {
            ContentType.MULTI_IMAGE
        } else if (isAllImages) {
            ContentType.IMAGE
        } else {
            ContentType.DOCUMENT
        }

        return ParsedShareContent(
            contentType = contentType,
            title = subject ?: "${streamList.size} Shared Files",
            text = intent.getStringExtra(Intent.EXTRA_TEXT),
            url = null,
            streamUris = streamList,
            mimeType = type,
            sourceApp = sourceApp
        )
    }

    private fun determineContentTypeFromMime(mimeType: String): ContentType {
        val lower = mimeType.lowercase()
        return when {
            lower.startsWith("image/") -> ContentType.IMAGE
            lower == "application/pdf" -> ContentType.PDF
            lower.startsWith("video/") -> ContentType.VIDEO
            lower.startsWith("audio/") -> ContentType.AUDIO
            lower.contains("vcard") -> ContentType.CONTACT
            lower.contains("text/plain") -> ContentType.TEXT
            lower.contains("text/html") -> ContentType.URL
            lower.startsWith("application/") -> ContentType.DOCUMENT
            else -> ContentType.UNKNOWN
        }
    }

    private fun generateTitleForStream(uri: Uri, contentType: ContentType): String {
        val lastPath = uri.lastPathSegment
        return if (!lastPath.isNullOrBlank() && !lastPath.contains("content:") && !lastPath.all { it.isDigit() }) {
            lastPath.substringBeforeLast(".")
        } else {
            "Shared ${contentType.displayName}"
        }
    }

    private fun getStreamUri(intent: Intent): Uri? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun getStreamUris(intent: Intent): List<Uri> {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java) ?: emptyList()
            } else {
                @Suppress("DEPRECATION")
                (intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM) as? List<*>)?.filterIsInstance<Uri>() ?: emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun getReferrerHost(intent: Intent): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_REFERRER, Uri::class.java)?.host
            } else {
                @Suppress("DEPRECATION")
                (intent.getParcelableExtra(Intent.EXTRA_REFERRER) as? Uri)?.host
            }
        } catch (e: Exception) {
            null
        }
    }

    // vCard Parser Helpers
    data class ParsedContact(
        val name: String? = null,
        val phone: String? = null,
        val email: String? = null,
        val org: String? = null,
        val title: String? = null,
        val formattedText: String = ""
    ) {
        fun toMap(): Map<String, String> {
            val map = mutableMapOf<String, String>()
            name?.let { map["name"] = it }
            phone?.let { map["phone"] = it }
            email?.let { map["email"] = it }
            org?.let { map["org"] = it }
            title?.let { map["title"] = it }
            return map
        }
    }

    private fun parseVCardFromUri(uri: Uri): ParsedContact {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val reader = BufferedReader(InputStreamReader(input))
                parseVCardText(reader.readText())
            } ?: ParsedContact()
        } catch (e: Exception) {
            ParsedContact()
        }
    }

    fun parseVCardText(vCardText: String): ParsedContact {
        var fn: String? = null
        var tel: String? = null
        var email: String? = null
        var org: String? = null
        var title: String? = null

        val lines = vCardText.lines()
        for (line in lines) {
            val trimmed = line.trim()
            when {
                trimmed.startsWith("FN:", ignoreCase = true) -> fn = trimmed.substring(3).trim()
                trimmed.startsWith("FN;", ignoreCase = true) -> fn = trimmed.substringAfter(":").trim()
                trimmed.startsWith("TEL", ignoreCase = true) -> if (tel == null) tel = trimmed.substringAfter(":").trim()
                trimmed.startsWith("EMAIL", ignoreCase = true) -> if (email == null) email = trimmed.substringAfter(":").trim()
                trimmed.startsWith("ORG:", ignoreCase = true) -> org = trimmed.substring(4).trim().replace(";", " ")
                trimmed.startsWith("TITLE:", ignoreCase = true) -> title = trimmed.substring(6).trim()
            }
        }

        val formattedParts = mutableListOf<String>()
        fn?.let { formattedParts.add("Name: $it") }
        tel?.let { formattedParts.add("Phone: $it") }
        email?.let { formattedParts.add("Email: $it") }
        org?.let { formattedParts.add("Organization: $it") }
        title?.let { formattedParts.add("Title: $it") }

        return ParsedContact(
            name = fn ?: org ?: "Contact Card",
            phone = tel,
            email = email,
            org = org,
            title = title,
            formattedText = formattedParts.joinToString("\n")
        )
    }

    private fun serializeRawIntent(intent: Intent, callerPackage: String?): String {
        return try {
            val json = kotlinx.serialization.json.buildJsonObject {
                put("action", kotlinx.serialization.json.JsonPrimitive(intent.action ?: "null"))
                put("type", kotlinx.serialization.json.JsonPrimitive(intent.type ?: "null"))
                if (callerPackage != null) {
                    put("sourceApp", kotlinx.serialization.json.JsonPrimitive(callerPackage))
                }
                val extras = intent.extras
                if (extras != null) {
                    val extrasObj = kotlinx.serialization.json.buildJsonObject {
                        for (key in extras.keySet()) {
                            val value = extras.get(key)
                            if (value != null) {
                                put(key, kotlinx.serialization.json.JsonPrimitive(value.toString()))
                            }
                        }
                    }
                    put("extras", extrasObj)
                }
            }
            json.toString()
        } catch (e: Exception) {
            "{}"
        }
    }
}
