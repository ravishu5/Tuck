package com.tuck.app

import com.tuck.app.data.remote.AdHosts
import com.tuck.app.processing.extractors.ExtractedComment
import com.tuck.app.processing.extractors.ExtractedSourceData
import com.tuck.app.processing.extractors.GenericWebSourceExtractor
import com.tuck.app.processing.extractors.InstagramSourceExtractor
import com.tuck.app.processing.extractors.LinkedInSourceExtractor
import com.tuck.app.processing.extractors.RedditSourceExtractor
import com.tuck.app.processing.extractors.TikTokSourceExtractor
import com.tuck.app.processing.extractors.TwitterSourceExtractor
import com.tuck.app.processing.extractors.YouTubeSourceExtractor
import com.tuck.app.processing.extractors.isThin
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The decision to escalate to the Tier 2 capture engine — CAPTURE_ARCHITECTURE.md §3.
 *
 * The engine itself drives a real `WebView` and can only be exercised by an instrumented test;
 * what is checked here is everything that decides *whether* it runs, which is where a mistake
 * would be expensive rather than merely slow.
 */
class CaptureEscalationTest {

    private val instagram = InstagramSourceExtractor()

    // ------------------------------------------------------------ escalation

    @Test
    fun testAnExtractionWithNoFetchedContentIsThin() {
        // Everything here is derivable from the URL alone, so a fetch added nothing.
        val urlOnly = ExtractedSourceData(
            platform = "INSTAGRAM",
            title = "Instagram Reel",
            canonicalUrl = "https://www.instagram.com/reel/C7xyzAbC"
        )
        assertTrue(urlOnly.isThin())
        assertTrue(null.isThin())
    }

    @Test
    fun testAnyRealCapturedContentIsNotThin() {
        val withBody = ExtractedSourceData(platform = "INSTAGRAM", bodyText = "A caption")
        val withMedia = ExtractedSourceData(
            platform = "INSTAGRAM",
            leadImageUrl = "https://scontent.cdninstagram.com/v/t51/x.jpg"
        )
        val withComments = ExtractedSourceData(
            platform = "REDDIT",
            comments = listOf(ExtractedComment(id = "a", author = "someone", bodyText = "hi"))
        )

        listOf(withBody, withMedia, withComments).forEach {
            assertFalse(it.platform, it.isThin())
        }
    }

    @Test
    fun testTitleAloneDoesNotCountAsContent() {
        // A title is what the URL already told us on every platform that needs Tier 2, so
        // treating it as content would suppress every escalation that matters.
        val titleOnly = ExtractedSourceData(platform = "INSTAGRAM", title = "Instagram Post")
        assertTrue(titleOnly.isThin())
    }

    // ------------------------------------------------- who asks for rendering

    @Test
    fun testOnlyGatedPlatformsRequireRenderedHtml() {
        // Instagram serves a script shell; Reddit serves a login redirect to logged-out clients.
        // Both are reachable only by being a browser, for different reasons.
        assertTrue(instagram.requiresRenderedHtml)
        assertTrue(RedditSourceExtractor().requiresRenderedHtml)

        // The rest still have a public seam that answers a plain fetch, and paying for a render
        // on those would be pure cost.
        listOf(
            YouTubeSourceExtractor(),
            TwitterSourceExtractor(),
            TikTokSourceExtractor(),
            LinkedInSourceExtractor(),
            GenericWebSourceExtractor()
        ).forEach { assertFalse(it.platformName, it.requiresRenderedHtml) }
    }

    @Test
    fun testRedditReadySelectorMatchesItsOwnFixture() {
        // The selector has to name something the parser reads, or the engine returns early with
        // a page that has not finished rendering.
        val selector = RedditSourceExtractor().readySelector
        assertNotNull(selector)
        assertTrue(selector!!.contains(".commentarea .sitetable"))
        assertTrue(selector.contains("div.thing.link"))
    }

    @Test
    fun testInstagramDeclaresAReadySelectorMatchingItsOwnParser() {
        val selector = instagram.readySelector
        assertNotNull(selector)
        // Without a selector the engine falls back to a fixed delay and guesses; these are the
        // classes the parser actually reads, verified against the rendered page.
        assertTrue(selector!!.contains(".Caption"))
        assertTrue(selector.contains(".EmbeddedMediaImage"))
    }

    @Test
    fun testRenderedHtmlFeedsTheSameParserAsAPlainFetch() = runBlocking {
        // The engine's whole contract: hand the existing parser rendered markup and it works,
        // with no Instagram-specific knowledge inside the engine itself.
        val rendered = """
            <html><body>
              <img class="EmbeddedMediaImage" src="https://scontent.cdninstagram.com/v/t51/9_n.jpg"/>
              <div class="CaptionUsername">kotlin_dev</div>
              <div class="Caption">Rendered by the capture engine.</div>
            </body></html>
        """.trimIndent()

        val fromShell = instagram.extract("https://www.instagram.com/reel/C7xyzAbC/", "<html><body></body></html>")
        val fromRendered = instagram.extract("https://www.instagram.com/reel/C7xyzAbC/", rendered)

        assertTrue("a script shell yields nothing to escalate past", fromShell.isThin())
        assertFalse("the rendered DOM parses", fromRendered.isThin())
        assertTrue(fromRendered.bodyText!!.contains("Rendered by the capture engine."))
    }

    // --------------------------------------------------------------- blocking

    @Test
    fun testAdHostsMatchesOnLabelBoundaries() {
        assertTrue(AdHosts.blocks("doubleclick.net"))
        assertTrue(AdHosts.blocks("stats.g.doubleclick.net"))
        assertTrue(AdHosts.blocks("pagead2.googlesyndication.com"))

        assertFalse(AdHosts.blocks("doubleclick.net.evil.test"))
        assertFalse(AdHosts.blocks("notdoubleclick.net"))
        assertFalse(AdHosts.blocks(null))
    }

    @Test
    fun testPlaybackHostsAreNeverBlocked() {
        // Blocking these breaks the video rather than the advertising around it.
        listOf(
            "redirector.googlevideo.com",
            "rr3---sn-abc.googlevideo.com",
            "www.googleapis.com",
            "scontent.cdninstagram.com",
            "video.twimg.com"
        ).forEach { assertFalse(it, AdHosts.blocks(it)) }
    }
}
