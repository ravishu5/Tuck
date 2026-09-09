package com.tuck.app

import com.tuck.app.processing.extractors.ExtractedComment
import com.tuck.app.processing.extractors.RedditSourceExtractor
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Reddit capture via `old.reddit.com` server-rendered HTML — CAPTURE_ARCHITECTURE.md §4.2.
 *
 * This is the path that unblocks comment capture without the OAuth credentials FUTURE_WORK.md
 * parked it on. The `.json` path is still parsed when one arrives; see [SourceExtractorTest].
 */
class OldRedditCaptureTest {

    private val extractor = RedditSourceExtractor()

    private fun readFixture(name: String): String {
        val stream = javaClass.classLoader?.getResourceAsStream("fixtures/$name")
            ?: throw IllegalArgumentException("Fixture fixtures/$name not found on classpath")
        return stream.bufferedReader().use { it.readText() }
    }

    private fun extractFixture() = runBlocking {
        extractor.extract(
            "https://www.reddit.com/r/MachineLearning/comments/gnnpost/graph_neural_networks/",
            readFixture("old_reddit_thread.html")
        )
    }

    /** Depth-first flatten, so assertions can talk about the whole tree. */
    private fun flatten(comments: List<ExtractedComment>): List<ExtractedComment> =
        comments.flatMap { listOf(it) + flatten(it.replies) }

    // ------------------------------------------------------------- fetch URL

    @Test
    fun testFetchUrlRewritesEveryRedditHostToOldReddit() {
        val expected = "https://old.reddit.com/r/MachineLearning/comments/gnnpost/" +
            "?limit=500&depth=8"

        listOf(
            "https://www.reddit.com/r/MachineLearning/comments/gnnpost/",
            "https://reddit.com/r/MachineLearning/comments/gnnpost/",
            "https://new.reddit.com/r/MachineLearning/comments/gnnpost/",
            "https://np.reddit.com/r/MachineLearning/comments/gnnpost/",
            "https://old.reddit.com/r/MachineLearning/comments/gnnpost/"
        ).forEach { url ->
            assertEquals("rewrite of $url", expected, extractor.fetchUrl(url))
        }
    }

    @Test
    fun testFetchUrlDropsTrackingQueryAndFragment() {
        assertEquals(
            "https://old.reddit.com/r/MachineLearning/comments/gnnpost/?limit=500&depth=8",
            extractor.fetchUrl(
                "https://www.reddit.com/r/MachineLearning/comments/gnnpost/?utm_source=share&context=3#t1_abc"
            )
        )
    }

    @Test
    fun testFetchUrlLeavesShortLinksAlone() {
        // redd.it only reveals its destination by following the redirect, which happens in the
        // fetcher, so there is nothing to rewrite here.
        val short = "https://redd.it/gnnpost"
        assertEquals(short, extractor.fetchUrl(short))
    }

    // ------------------------------------------------------------------ post

    @Test
    fun testPostFieldsParsedFromOldRedditHtml() {
        val result = extractFixture()

        assertEquals("REDDIT", result.platform)
        assertEquals("Graph Neural Networks in Production — A 2026 Survey", result.title)
        assertEquals("ml_researcher", result.authorHandle)
        assertEquals("r/MachineLearning", result.community)
        assertEquals(542, result.score)
        assertTrue(result.bodyText!!.startsWith("We spent eighteen months"))
        // Paragraph breaks survive; text() alone would run the selftext into one line.
        assertTrue(result.bodyText!!.contains("\n\n"))
    }

    @Test
    fun testCommentCountReportsThreadSizeNotTheCapturedSlice() {
        val result = extractFixture()
        // old.reddit renders a slice inline behind a "load more comments" stub; the true size is
        // in the markup and is what the item should claim.
        assertEquals(247, result.commentCount)
        assertTrue(flatten(result.comments).size < result.commentCount)
    }

    @Test
    fun testPostTimestampParsed() {
        // 2026-08-24T09:14:33+00:00
        assertEquals(1787562873000L, extractFixture().postedAt)
    }

    // -------------------------------------------------------------- comments

    @Test
    fun testCommentTreeKeepsItsNesting() {
        val comments = extractFixture().comments

        assertEquals(2, comments.size)

        val alice = comments[0]
        assertEquals("alice_eng", alice.author)
        assertEquals("0001", alice.path)
        assertEquals(0, alice.depth)
        assertEquals(128, alice.score)
        assertEquals("alice", alice.id)
        assertNull(alice.parentId)

        val bob = alice.replies.single()
        assertEquals("bob_data", bob.author)
        assertEquals("0001.0001", bob.path)
        assertEquals(1, bob.depth)
        assertEquals(34, bob.score)
        assertEquals("t1_alice", bob.parentId)

        val carol = bob.replies.single()
        assertEquals("alice_eng", carol.author)
        assertEquals("0001.0001.0001", carol.path)
        assertEquals(2, carol.depth)
        assertEquals("t1_bob", carol.parentId)

        val charlie = comments[1]
        assertEquals("charlie_ai", charlie.author)
        assertEquals("0002", charlie.path)
        assertEquals(0, charlie.depth)
        assertTrue(charlie.replies.isEmpty())
    }

    @Test
    fun testRepliesAreNotAlsoHoistedToTheTopLevel() {
        // The failure this guards against: `select` searches all descendants, so a careless
        // parser lifts every nested reply into the root listing and the tree goes flat.
        val comments = extractFixture().comments
        assertEquals(listOf("0001", "0002"), comments.map { it.path })
        assertEquals(4, flatten(comments).size)
    }

    @Test
    fun testCommentBodyKeepsParagraphsAndQuotedText() {
        val comments = extractFixture().comments

        val alice = comments[0]
        assertTrue(alice.bodyText.startsWith("Inference cost is exactly where we landed too."))
        assertTrue(alice.bodyText.contains("\n\nBatching helped more"))

        // A blockquote is part of what the commenter said and belongs in the archive.
        val charlie = comments[1]
        assertTrue(charlie.bodyText.contains("nobody warns you about that"))
        assertTrue(charlie.bodyText.contains("most useful sentence"))
    }

    @Test
    fun testDeletedCommentsAndLoadMoreStubsAreSkipped() {
        val all = flatten(extractFixture().comments)
        assertTrue(all.none { it.bodyText.contains("[removed]") })
        assertTrue(all.none { it.author == "[deleted]" })
        assertTrue(all.none { it.bodyText.contains("load more comments") })
    }

    @Test
    fun testCommentTimestampsParsed() {
        // 2026-08-24T10:02:00+00:00
        assertEquals(1787565720000L, extractFixture().comments[0].postedAt)
    }

    // ------------------------------------------------------------- fallbacks

    @Test
    fun testNewRedditShellFallsBackRatherThanArchivingAnEmptyThread() = runBlocking {
        val shell = "<html><body><div id=\"root\"></div></body></html>"
        val result = extractor.extract("https://www.reddit.com/r/Android/comments/x/", shell)

        assertEquals("Reddit Discussion", result.title)
        assertEquals("r/Android", result.community)
        assertTrue(result.comments.isEmpty())
    }

    @Test
    fun testJsonPayloadStillParsesAfterTheHtmlSwitch() = runBlocking {
        // The `.json` listing remains the richer source if an authenticated fetch ever returns
        // one, so switching the default fetch to HTML must not drop that path.
        val result = extractor.extract(
            "https://reddit.com/r/MachineLearning/comments/gnn_post_1/",
            readFixture("reddit_thread.json")
        )
        assertEquals("Graph Neural Networks in Production — A 2026 Survey", result.title)
        assertEquals(2, result.comments.size)
        assertEquals("0001.0001", result.comments[0].replies[0].path)
    }
}
