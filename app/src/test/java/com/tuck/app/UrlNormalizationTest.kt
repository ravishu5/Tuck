package com.tuck.app

import com.tuck.app.processing.UrlMetadataProcessor
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class UrlNormalizationTest {

    private lateinit var processor: UrlMetadataProcessor

    @Before
    fun setUp() {
        processor = UrlMetadataProcessor()
    }

    @Test
    fun testExtractDomain() {
        assertEquals("reddit.com", processor.extractDomain("https://www.reddit.com/r/androiddev/comments/123"))
        assertEquals("github.com", processor.extractDomain("https://github.com/google/dagger"))
        assertEquals("instagram.com", processor.extractDomain("https://instagram.com/p/xyz123"))
        assertEquals("subdomain.example.org", processor.extractDomain("https://subdomain.example.org/path?query=1"))
    }

    @Test
    fun testCleanUrlRemovesTrackingParameters() {
        val dirtyUrl = "https://example.com/article?id=123&utm_source=twitter&utm_medium=social&utm_campaign=summer_sale&fbclid=IwAR123"
        val cleaned = processor.cleanUrl(dirtyUrl)

        assertEquals("https://example.com/article?id=123", cleaned)
    }

    @Test
    fun testCleanUrlPreservesLegitimateParameters() {
        val url = "https://www.youtube.com/watch?v=dQw4w9WgXcQ&t=42s"
        val cleaned = processor.cleanUrl(url)

        assertEquals("https://www.youtube.com/watch?v=dQw4w9WgXcQ&t=42s", cleaned)
    }
}
