package com.tuck.app.data.remote

/**
 * Third-party ad and analytics hosts, refused before the request leaves the device.
 *
 * Shared by the in-place viewer and the headless capture engine: a page Tuck renders invisibly
 * has even less business loading trackers than one the reader is looking at, and every blocked
 * request is data and battery not spent.
 *
 * Matched on label boundaries so `doubleclick.net.example.com` is not mistaken for the real
 * thing. Deliberately absent: `googlevideo.com` and `googleapis.com`, which carry actual video
 * streams — blocking those breaks playback rather than advertising.
 */
object AdHosts {

    private val BLOCKED = setOf(
        "doubleclick.net",
        "googlesyndication.com",
        "googleadservices.com",
        "googletagservices.com",
        "googletagmanager.com",
        "google-analytics.com",
        "adservice.google.com",
        "moatads.com",
        "scorecardresearch.com",
        "adnxs.com",
        "amazon-adsystem.com",
        "criteo.com",
        "taboola.com",
        "outbrain.com",
        "connect.facebook.net",
        "analytics.tiktok.com",
        "ads.tiktok.com",
        "branch.io"
    )

    fun blocks(host: String?): Boolean {
        val lower = host?.lowercase() ?: return false
        return BLOCKED.any { lower == it || lower.endsWith(".$it") }
    }
}
