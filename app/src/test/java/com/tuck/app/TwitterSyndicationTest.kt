package com.tuck.app

import com.tuck.app.processing.extractors.TwitterSourceExtractor
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * X (Twitter) capture via the public syndication endpoint — CAPTURE_ARCHITECTURE.md §4.1.
 *
 * The token vectors below were generated from the reference JavaScript
 * (`((id / 1e15) * Math.PI).toString(36).replace(/(0+|\.)/g, '')`) run on Node 22. They are the
 * contract with X's endpoint: if the base-36 port drifts by a single digit the request 404s, so
 * these are golden values rather than properties.
 */
class TwitterSyndicationTest {

    private val extractor = TwitterSourceExtractor()

    private fun readFixture(name: String): String {
        val stream = javaClass.classLoader?.getResourceAsStream("fixtures/$name")
            ?: throw IllegalArgumentException("Fixture fixtures/$name not found on classpath")
        return stream.bufferedReader().use { it.readText() }
    }

    // ---------------------------------------------------------------- token

    @Test
    fun testSyndicationTokenMatchesReferenceImplementation() {
        val vectors = mapOf(
            "1234567890123456789" to "2zqic77uqyk",
            "1795000000000000000" to "4cn5ptl6iq2",
            "1000000000000000000" to "2f9lc2ug9mm",
            "1611111111111111111" to "3wlgdgl27ih",
            "1750000000000000001" to "48psc4zagul",
            "1234567890123456" to "3vmjqguc7ab",
            "20" to "6dq1a2xwd93",
            "1" to "bhi2ay3f28n"
        )
        for ((id, expected) in vectors) {
            assertEquals("token for id $id", expected, TwitterSourceExtractor.syndicationToken(id))
        }
    }

    @Test
    fun testSyndicationTokenStripsEveryZeroRunAndThePoint() {
        // 1899999999999999999 renders as "4lt.0xr09b3f": a leading zero in the fraction and an
        // interior one, both removed, which a naive trim of only the decimal point would keep.
        assertEquals("4ltxr9b3f", TwitterSourceExtractor.syndicationToken("1899999999999999999"))
    }

    @Test
    fun testDoubleToRadixStringMatchesV8ForKnownValues() {
        assertEquals("2zq.ic77uqyk", TwitterSourceExtractor.doubleToRadixString(
            (1234567890123456789.0 / 1e15) * Math.PI, 36))
        assertEquals("4lt.0xr09b3f", TwitterSourceExtractor.doubleToRadixString(
            (1899999999999999999.0 / 1e15) * Math.PI, 36))
        // Values with no fractional part must not gain a trailing point.
        assertEquals("z", TwitterSourceExtractor.doubleToRadixString(35.0, 36))
        assertEquals("10", TwitterSourceExtractor.doubleToRadixString(36.0, 36))
        assertEquals("0", TwitterSourceExtractor.doubleToRadixString(0.0, 36))
        assertEquals("-1a", TwitterSourceExtractor.doubleToRadixString(-46.0, 36))
    }

    @Test
    fun testNonNumericStatusIdYieldsEmptyTokenRatherThanThrowing() {
        assertEquals("", TwitterSourceExtractor.syndicationToken("not-an-id"))
    }

    // ------------------------------------------------------------- fetch URL

    @Test
    fun testStatusIdParsedFromEveryShareUrlShape() {
        assertEquals("1795000000000000000",
            extractor.statusId("https://x.com/kotlin_dev/status/1795000000000000000"))
        assertEquals("1795000000000000000",
            extractor.statusId("https://twitter.com/kotlin_dev/status/1795000000000000000?s=20&t=abc"))
        // Share sheets frequently hand over the handle-less form.
        assertEquals("1795000000000000000",
            extractor.statusId("https://x.com/i/status/1795000000000000000"))
        assertNull(extractor.statusId("https://x.com/kotlin_dev"))
    }

    @Test
    fun testFetchUrlTargetsSyndicationEndpoint() {
        val url = extractor.fetchUrl("https://x.com/kotlin_dev/status/1795000000000000000")
        assertEquals(
            "https://cdn.syndication.twimg.com/tweet-result" +
                "?id=1795000000000000000&lang=en&token=4cn5ptl6iq2",
            url
        )
    }

    @Test
    fun testFetchUrlLeavesNonStatusUrlsAlone() {
        val profile = "https://x.com/kotlin_dev"
        assertEquals(profile, extractor.fetchUrl(profile))
    }

    // -------------------------------------------------------------- parsing

    @Test
    fun testExtractorParsesPostFromSyndicationFixture() = runBlocking {
        val result = extractor.extract(
            "https://x.com/kotlin_dev/status/1795000000000000000",
            readFixture("twitter_syndication.json")
        )

        assertEquals("TWITTER", result.platform)
        assertEquals("@kotlin_dev", result.authorHandle)
        assertEquals("Kotlin Dev", result.authorDisplay)
        assertEquals(1842, result.score)
        assertEquals(96, result.commentCount)

        val body = result.bodyText!!
        assertTrue(body.startsWith("Shipped an offline-first archive today."))
        // t.co link shorteners are replaced by the destination they hide.
        assertTrue(body.contains("https://developer.android.com/training/data-storage/room"))
        assertFalse(body.contains("t.co/aBcDeF1234"))
        // The t.co pointing at the post's own media is noise and is dropped entirely.
        assertFalse(body.contains("t.co/xYz987media"))
        assertTrue(body.contains("#AndroidDev"))
    }

    @Test
    fun testQuotedPostIsAppendedToBody() = runBlocking {
        val result = extractor.extract(
            "https://x.com/kotlin_dev/status/1795000000000000000",
            readFixture("twitter_syndication.json")
        )
        val body = result.bodyText!!
        assertTrue(body.contains("Quoting @ada_builds:"))
        assertTrue(body.contains("What is everyone using for on-device full-text search"))
        // HTML entities in the syndication payload are decoded, not archived raw.
        assertTrue(body.contains("& does anything beat FTS4"))
        assertFalse(body.contains("&amp;"))
    }

    @Test
    fun testMediaPrefersHighestBitrateProgressiveMp4() = runBlocking {
        val result = extractor.extract(
            "https://x.com/kotlin_dev/status/1795000000000000000",
            readFixture("twitter_syndication.json")
        )

        assertEquals(
            listOf(
                "https://pbs.twimg.com/media/GxAbCdEfGhIjKlM.jpg",
                "https://video.twimg.com/ext_tw_video/1795/pu/vid/1280x720/high.mp4"
            ),
            result.mediaUrls
        )
        assertEquals("https://pbs.twimg.com/media/GxAbCdEfGhIjKlM.jpg", result.leadImageUrl)
        // An HLS playlist is useless to an offline archive and must never be chosen.
        assertFalse(result.mediaUrls.any { it.endsWith(".m3u8") })
    }

    @Test
    fun testTitleIsTheOpeningLineOfThePost() = runBlocking {
        val result = extractor.extract(
            "https://x.com/kotlin_dev/status/1795000000000000000",
            readFixture("twitter_syndication.json")
        )
        assertNotNull(result.title)
        assertTrue(result.title!!.length <= 80)
        assertTrue(result.title!!.startsWith("Shipped an offline-first archive today."))
    }

    @Test
    fun testTimestampParsedAsUtcMillis() = runBlocking {
        val result = extractor.extract(
            "https://x.com/kotlin_dev/status/1795000000000000000",
            readFixture("twitter_syndication.json")
        )
        // 2026-08-24T09:14:33.000Z
        assertEquals(1787562873000L, result.postedAt)
    }

    @Test
    fun testRawJsonIsRetainedForLaterReprocessing() = runBlocking {
        val fixture = readFixture("twitter_syndication.json")
        val result = extractor.extract("https://x.com/kotlin_dev/status/1795000000000000000", fixture)
        assertEquals(fixture, result.rawJson)
    }

    // ------------------------------------------------------------- fallback

    @Test
    fun testErrorPayloadFallsBackToHandleOnlyRatherThanInventingAPost() = runBlocking {
        val result = extractor.extract(
            "https://x.com/kotlin_dev/status/1795000000000000000",
            """{"error":"Not found"}"""
        )
        assertEquals("Post on X by @kotlin_dev", result.title)
        assertEquals("@kotlin_dev", result.authorHandle)
        assertNull(result.bodyText)
        assertTrue(result.mediaUrls.isEmpty())
    }

    @Test
    fun testHtmlPayloadFallsBackToOpenGraph() = runBlocking {
        // A URL with no status id never reaches syndication, so the shared page is what arrives.
        val html = """
            <html><head>
              <meta property="og:title" content="Kotlin Dev (@kotlin_dev) on X">
              <meta property="og:description" content="Building offline-first Android apps.">
              <meta property="og:image" content="https://pbs.twimg.com/profile_images/1234/avatar.jpg">
            </head><body></body></html>
        """.trimIndent()

        val result = extractor.extract("https://x.com/kotlin_dev", html)
        assertEquals("Kotlin Dev (@kotlin_dev) on X", result.title)
        assertEquals("Building offline-first Android apps.", result.bodyText)
        assertEquals("https://pbs.twimg.com/profile_images/1234/avatar.jpg", result.leadImageUrl)
    }

    @Test
    fun testNoPayloadStillNamesTheAuthor() = runBlocking {
        val result = extractor.extract("https://x.com/kotlin_dev/status/1795000000000000000", null)
        assertEquals("Post on X by @kotlin_dev", result.title)
        assertEquals("@kotlin_dev", result.authorHandle)
    }
}
