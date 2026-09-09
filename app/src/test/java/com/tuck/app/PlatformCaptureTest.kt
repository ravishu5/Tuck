package com.tuck.app

import com.tuck.app.domain.model.ContentType
import com.tuck.app.processing.extractors.InstagramSourceExtractor
import com.tuck.app.processing.extractors.LinkedInSourceExtractor
import com.tuck.app.processing.extractors.TikTokSourceExtractor
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The platform extractors lifted out of `UrlMetadataProcessor` in the 1a collapse.
 *
 * Each of these used to be a branch inside a 600-line processor that opened its own connections,
 * which made them untestable. Split out, they parse a payload someone else fetched.
 */
class PlatformCaptureTest {

    private val instagram = InstagramSourceExtractor()
    private val tiktok = TikTokSourceExtractor()
    private val linkedIn = LinkedInSourceExtractor()

    // ------------------------------------------------------------- Instagram

    private val embedHtml = """
        <html><body>
          <div class="EmbedFrame">
            <img class="EmbeddedMediaImage"
                 src="https://scontent-lhr8-1.cdninstagram.com/v/t51/456_n.jpg?stp=dst-jpg&amp;_nc_ht=x"/>
            <div class="CaptionUsername"><a href="/kotlin_dev/">kotlin_dev</a></div>
            <div class="Caption">Eighteen months of GNNs in production.
            Full write-up on the blog. #android #kotlin</div>
          </div>
        </body></html>
    """.trimIndent()

    @Test
    fun testInstagramFetchUrlTargetsThePublicEmbedPage() {
        assertEquals(
            "https://www.instagram.com/p/C7xyzAbC/embed/captioned/",
            instagram.fetchUrl("https://www.instagram.com/reel/C7xyzAbC/?igsh=junk")
        )
        assertEquals(
            "https://www.instagram.com/p/C7xyzAbC/embed/captioned/",
            instagram.fetchUrl("https://www.instagram.com/p/C7xyzAbC/")
        )
    }

    @Test
    fun testInstagramFetchUrlLeavesProfileLinksAlone() {
        val profile = "https://www.instagram.com/kotlin_dev/"
        assertEquals(profile, instagram.fetchUrl(profile))
    }

    @Test
    fun testInstagramParsesCaptionAuthorAndMedia() = runBlocking {
        val result = instagram.extract("https://www.instagram.com/reel/C7xyzAbC/", embedHtml)

        assertEquals("INSTAGRAM", result.platform)
        assertEquals("@kotlin_dev", result.authorHandle)
        assertTrue(result.bodyText!!.startsWith("Eighteen months of GNNs in production."))
        assertTrue(result.title!!.startsWith("Eighteen months of GNNs"))
        // The &amp; in the embed markup must be decoded or the CDN rejects the URL.
        assertEquals(
            "https://scontent-lhr8-1.cdninstagram.com/v/t51/456_n.jpg?stp=dst-jpg&_nc_ht=x",
            result.leadImageUrl
        )
        // A reel is video even though the embed only ever hands over a poster frame.
        assertEquals(ContentType.VIDEO, result.contentType)
    }

    @Test
    fun testInstagramRejectsItsOwnChromeAsMedia() = runBlocking {
        // The sprite sheet and the app icon sit on the same CDN as post media; saving one as the
        // item's thumbnail is the failure this guards against.
        val chromeOnly = """
            <html><body>
              <img class="CoverImage" src="https://static.cdninstagram.com/rsrc.php/v3/yI/r/logo.png"/>
              <div class="Caption">A post with no reachable media</div>
            </body></html>
        """.trimIndent()

        val result = instagram.extract("https://www.instagram.com/p/C7xyzAbC/", chromeOnly)
        assertNull(result.leadImageUrl)
        assertTrue(result.mediaUrls.isEmpty())
        assertEquals("A post with no reachable media", result.bodyText)
    }

    @Test
    fun testInstagramAcceptsEveryShareHost() {
        listOf(
            "https://www.instagram.com/reel/C7xyzAbC/",
            "https://instagr.am/p/C7xyzAbC/",
            "https://ig.me/p/C7xyzAbC/"
        ).forEach { assertTrue(it, instagram.canHandle(it)) }
    }

    @Test
    fun testInstagramDecodesPercentEncodedShareUrls() {
        // Some share sheets hand over the path encoded; matching the raw string misses it.
        assertEquals(
            "https://www.instagram.com/p/C7xyzAbC/embed/captioned/",
            instagram.fetchUrl("https://www.instagram.com%2Freel%2FC7xyzAbC%2F")
        )
    }

    @Test
    fun testInstagramCapturesAudioTrackAndDirectVideo() = runBlocking {
        val withScript = """
            <html><body>
              <div class="Caption">Eighteen months of GNNs.</div>
              <script>window._d = {"audio_asset_title":"Blue Monday - New Order",
                "video_url":"https://scontent.cdninstagram.com/v/t50/clip.mp4?_nc=1"};</script>
            </body></html>
        """.trimIndent()

        val result = instagram.extract("https://www.instagram.com/reel/C7xyzAbC/", withScript)

        // The track is what identifies a reel to the person who saved it, so it belongs in the
        // indexed body, not just in metadata.
        assertTrue(result.bodyText!!.contains("Blue Monday - New Order"))
        assertTrue(result.bodyText!!.contains("Eighteen months of GNNs."))
        assertTrue(result.mediaUrls.any { it.endsWith(".mp4?_nc=1") })
    }

    @Test
    fun testInstagramStoryNamesTheAuthorFromTheUrl() = runBlocking {
        val result = instagram.extract("https://www.instagram.com/stories/kotlin_dev/123/", null)
        assertEquals("@kotlin_dev", result.authorHandle)
        assertEquals("Instagram Story by @kotlin_dev", result.title)
        assertEquals(ContentType.VIDEO, result.contentType)
    }

    @Test
    fun testInstagramPostWithoutPayloadStillTypesAsImage() = runBlocking {
        val result = instagram.extract("https://www.instagram.com/p/C7xyzAbC/", null)
        assertEquals(ContentType.IMAGE, result.contentType)
        assertEquals("Instagram Post", result.title)
    }

    // ---------------------------------------------------------------- TikTok

    @Test
    fun testTikTokFetchUrlTargetsOEmbed() {
        assertEquals(
            "https://www.tiktok.com/oembed?url=https://www.tiktok.com/@user/video/123",
            tiktok.fetchUrl("https://www.tiktok.com/@user/video/123?is_from_webapp=1")
        )
    }

    @Test
    fun testTikTokParsesOEmbedPayload() = runBlocking {
        val json = """
            {"version":"1.0","type":"video","title":"How to index 10k notes on device",
             "author_name":"kotlin_dev","author_url":"https://www.tiktok.com/@kotlin_dev",
             "thumbnail_url":"https://p16.tiktokcdn.com/obj/cover.jpeg","thumbnail_width":720}
        """.trimIndent()

        val result = tiktok.extract("https://www.tiktok.com/@user/video/123", json)

        assertEquals("TIKTOK", result.platform)
        assertEquals("How to index 10k notes on device", result.title)
        assertEquals("@kotlin_dev", result.authorHandle)
        assertEquals("https://p16.tiktokcdn.com/obj/cover.jpeg", result.leadImageUrl)
        assertEquals(ContentType.VIDEO, result.contentType)
    }

    @Test
    fun testTikTokFallsBackWithoutPayload() = runBlocking {
        val result = tiktok.extract("https://www.tiktok.com/@user/video/123", null)
        assertEquals("TikTok video", result.title)
        assertEquals(ContentType.VIDEO, result.contentType)
        assertTrue(result.mediaUrls.isEmpty())
    }

    // -------------------------------------------------------------- LinkedIn

    @Test
    fun testLinkedInParsesOpenGraph() = runBlocking {
        val html = """
            <html><head>
              <meta property="og:title" content="Ravi on LinkedIn: shipping Tuck"/>
              <meta property="og:description" content="Eighteen months of local-first Android."/>
              <meta property="og:image" content="https://media.licdn.com/dms/image/abc&amp;t=1"/>
            </head></html>
        """.trimIndent()

        val result = linkedIn.extract("https://www.linkedin.com/posts/ravi_tuck?utm_source=share", html)

        assertEquals("LINKEDIN", result.platform)
        assertEquals("Ravi on LinkedIn: shipping Tuck", result.title)
        assertEquals("Eighteen months of local-first Android.", result.bodyText)
        assertEquals("https://media.licdn.com/dms/image/abc&t=1", result.leadImageUrl)
        assertEquals("https://www.linkedin.com/posts/ravi_tuck", result.canonicalUrl)
    }

    @Test
    fun testLinkedInFallsBackWithoutPayload() = runBlocking {
        val result = linkedIn.extract("https://www.linkedin.com/posts/ravi_tuck", null)
        assertEquals("LinkedIn Post", result.title)
        assertEquals("LinkedIn", result.community)
        assertFalse(result.faviconUrl.isNullOrBlank())
    }
}
