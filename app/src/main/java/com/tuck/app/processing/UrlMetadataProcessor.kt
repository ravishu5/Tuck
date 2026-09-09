package com.tuck.app.processing

import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

/**
 * URL hygiene: the parts of link handling that need no network.
 *
 * This class used to also fetch and parse pages, with its own handlers for Reddit, Instagram,
 * YouTube, TikTok and LinkedIn running alongside a second, competing set in
 * `SourceExtractorRegistry` — every saved link was fetched twice and parsed by two different
 * OpenGraph readers that could disagree. The registry is the one that survived (2026-08-24 ADR);
 * what is left here is the pure string work that has no business in an extractor.
 */
@Singleton
class UrlMetadataProcessor @Inject constructor() {

    private val trackingParameters = setOf(
        "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content",
        "fbclid", "gclid", "msclkid", "mc_eid", "igshid", "igsh", "ref", "ref_src",
        "twclid", "_hsenc", "_hsmi", "yclid", "soc_src", "soc_trk", "feature",
        "share_id", "rdt", "context", "spm", "__twitter_impression", "si"
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
}
