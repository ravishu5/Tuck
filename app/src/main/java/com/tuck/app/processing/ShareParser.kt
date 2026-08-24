package com.tuck.app.processing

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.tuck.app.domain.model.ContentType
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

data class ParsedShareContent(
    val contentType: ContentType,
    val title: String,
    val text: String? = null,
    val url: String? = null,
    val streamUris: List<Uri> = emptyList(),
    val mimeType: String? = null,
    val sourceApp: String? = null
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

    fun parseIntent(intent: Intent, callerPackage: String? = null): ParsedShareContent? {
        val action = intent.action ?: return null
        val type = intent.type ?: "*/*"
        val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)
            ?: intent.getStringExtra(Intent.EXTRA_TITLE)

        val referrer = intent.getParcelableExtra<Uri>(Intent.EXTRA_REFERRER)?.host
            ?: callerPackage

        if (action == Intent.ACTION_SEND) {
            return parseSingleSend(intent, type, subject, referrer)
        } else if (action == Intent.ACTION_SEND_MULTIPLE) {
            return parseMultipleSend(intent, type, subject, referrer)
        }

        return null
    }

    private fun parseSingleSend(intent: Intent, type: String, subject: String?, sourceApp: String?): ParsedShareContent {
        val extraText = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim()
        val extraStream = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)

        // 1. If stream URI is present (Image, PDF, Audio, Video, Document)
        if (extraStream != null) {
            val resolvedMime = intent.type ?: context.contentResolver.getType(extraStream) ?: type
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

        // 2. If Text / URL is present
        if (!extraText.isNullOrBlank()) {
            val urlMatcher = urlPattern.matcher(extraText)
            if (urlMatcher.find()) {
                val foundUrl = urlMatcher.group()
                val remainingText = extraText.replace(foundUrl, "").trim()
                val domain = urlMetadataProcessor.extractDomain(foundUrl)

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
        val streamList = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM) ?: emptyList<Uri>()
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
}
