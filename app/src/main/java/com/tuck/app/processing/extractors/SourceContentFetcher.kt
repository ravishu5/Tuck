package com.tuck.app.processing.extractors

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fetches the raw payload a [SourceExtractor] needs to parse.
 *
 * Kept separate from the extractors so they stay pure and unit-testable against
 * checked-in fixtures with no network access.
 */
@Singleton
class SourceContentFetcher @Inject constructor() {

    private companion object {
        const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
        const val TIMEOUT_MS = 8000
        const val MAX_CHARS = 4_000_000
        const val COMMENT_LIMIT = 500
        const val COMMENT_DEPTH = 8
    }

    suspend fun fetch(url: String, platformName: String): String? = withContext(Dispatchers.IO) {
        try {
            when (platformName) {
                "REDDIT" -> fetchJson(redditJsonUrl(url))
                else -> fetchHtml(url)
            }
        } catch (e: Exception) {
            // A failed fetch must never fail the save (Product Law 2); the caller
            // falls back to whatever metadata extraction already produced.
            null
        }
    }

    /** Reddit serves the full post plus its nested comment tree by appending `.json`. */
    private fun redditJsonUrl(url: String): String {
        val base = url.substringBefore('?').removeSuffix("/")
        val withSuffix = if (base.endsWith(".json")) base else "$base.json"
        return "$withSuffix?limit=$COMMENT_LIMIT&depth=$COMMENT_DEPTH&raw_json=1"
    }

    private fun fetchJson(url: String): String? {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "application/json")
        }
        return try {
            connection.connect()
            if (connection.responseCode !in 200..299) return null
            BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                reader.readText().take(MAX_CHARS)
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun fetchHtml(url: String): String? =
        Jsoup.connect(url)
            .userAgent(USER_AGENT)
            .timeout(TIMEOUT_MS)
            .followRedirects(true)
            .ignoreHttpErrors(true)
            .get()
            .html()
            .take(MAX_CHARS)
}
