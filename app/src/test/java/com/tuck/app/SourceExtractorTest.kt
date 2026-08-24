package com.tuck.app

import com.tuck.app.processing.extractors.GenericWebSourceExtractor
import com.tuck.app.processing.extractors.RedditSourceExtractor
import com.tuck.app.processing.extractors.SourceExtractorRegistry
import com.tuck.app.processing.extractors.TwitterSourceExtractor
import com.tuck.app.processing.extractors.YouTubeSourceExtractor
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceExtractorTest {

    private val redditExtractor = RedditSourceExtractor()
    private val youtubeExtractor = YouTubeSourceExtractor()
    private val twitterExtractor = TwitterSourceExtractor()
    private val genericWebExtractor = GenericWebSourceExtractor()
    private val registry = SourceExtractorRegistry(redditExtractor, youtubeExtractor, twitterExtractor, genericWebExtractor)

    private fun readFixture(name: String): String {
        val stream = javaClass.classLoader?.getResourceAsStream("fixtures/$name")
            ?: throw IllegalArgumentException("Fixture fixtures/$name not found on classpath")
        return stream.bufferedReader().use { it.readText() }
    }

    @Test
    fun testRegistrySelectsCorrectExtractor() {
        val reddit = registry.getExtractor("https://www.reddit.com/r/AndroidDev/comments/xyz/")
        assertEquals("REDDIT", reddit.platformName)

        val yt = registry.getExtractor("https://youtu.be/dQw4w9WgXcQ")
        assertEquals("YOUTUBE", yt.platformName)

        val tw = registry.getExtractor("https://x.com/AndroidDev/status/123456")
        assertEquals("TWITTER", tw.platformName)

        val web = registry.getExtractor("https://sqlite.org/features.html")
        assertEquals("WEB", web.platformName)
    }

    @Test
    fun testRedditExtractorParsesPostAndThreadedCommentsFromFixture() = runBlocking {
        val fixture = readFixture("reddit_thread.json")
        val result = redditExtractor.extract("https://reddit.com/r/MachineLearning/comments/gnn_post_1/", fixture)

        assertEquals("REDDIT", result.platform)
        assertEquals("Graph Neural Networks in Production — A 2026 Survey", result.title)
        assertEquals("ml_researcher", result.authorHandle)
        assertEquals("r/MachineLearning", result.community)
        assertEquals(542, result.score)
        assertEquals(24, result.commentCount)

        // Verify comments tree
        assertEquals(2, result.comments.size)

        val firstComment = result.comments[0]
        assertEquals("alice_eng", firstComment.author)
        assertEquals("0001", firstComment.path)
        assertEquals(0, firstComment.depth)
        assertEquals(128, firstComment.score)

        // Verify nested child comment
        assertEquals(1, firstComment.replies.size)
        val childReply = firstComment.replies[0]
        assertEquals("bob_data", childReply.author)
        assertEquals("0001.0001", childReply.path)
        assertEquals(1, childReply.depth)
        assertEquals(34, childReply.score)

        val secondComment = result.comments[1]
        assertEquals("charlie_ai", secondComment.author)
        assertEquals("0002", secondComment.path)
        assertEquals(0, secondComment.depth)
    }

    @Test
    fun testYouTubeExtractorParsesMetadataFromFixture() = runBlocking {
        val fixture = readFixture("youtube_video.html")
        val result = youtubeExtractor.extract("https://www.youtube.com/watch?v=dQw4w9WgXcQ", fixture)

        assertEquals("YOUTUBE", result.platform)
        assertEquals("Building Local-First Android Apps with Room & WorkManager", result.title)
        assertNotNull(result.leadImageUrl)
        assertTrue(result.leadImageUrl!!.contains("dQw4w9WgXcQ"))
    }

    @Test
    fun testGenericWebExtractorParsesArticleFromFixture() = runBlocking {
        val fixture = readFixture("article_sample.html")
        val result = genericWebExtractor.extract("https://sqlite.org/features.html", fixture)

        assertEquals("WEB", result.platform)
        assertEquals("Modern SQLite Features You Need to Know", result.title)
        assertEquals("Dr. Richard Hipp", result.authorDisplay)
        assertTrue(result.bodyText!!.contains("SQLite is the most widely deployed"))
    }
}
