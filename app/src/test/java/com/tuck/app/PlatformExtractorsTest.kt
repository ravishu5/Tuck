package com.tuck.app

import com.tuck.app.domain.model.ContentType
import com.tuck.app.domain.model.EntityType
import com.tuck.app.domain.model.SavedItem
import com.tuck.app.processing.EntityExtractor
import com.tuck.app.processing.RuleBasedContentClassifier
import com.tuck.app.processing.UrlMetadataProcessor
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlatformExtractorsTest {

    private val urlMetadataProcessor = UrlMetadataProcessor()
    private val entityExtractor = EntityExtractor()
    private val classifier = RuleBasedContentClassifier()

    @Test
    fun testUrlCleaningStripsTrackingParameters() {
        val dirtyUrl = "https://www.nytimes.com/2026/08/24/technology/ai.html?utm_source=twitter&utm_medium=social&utm_campaign=daily&fbclid=IwAR123#article-body"
        val cleaned = urlMetadataProcessor.cleanUrl(dirtyUrl)
        assertEquals("https://www.nytimes.com/2026/08/24/technology/ai.html#article-body", cleaned)

        val cleanUrl = "https://github.com/google/guava"
        assertEquals(cleanUrl, urlMetadataProcessor.cleanUrl(cleanUrl))
    }

    @Test
    fun testExtractDomainStripsWwwPrefix() {
        assertEquals("reddit.com", urlMetadataProcessor.extractDomain("https://www.reddit.com/r/Android/comments/123"))
        assertEquals("youtu.be", urlMetadataProcessor.extractDomain("https://youtu.be/dQw4w9WgXcQ"))
        assertEquals("instagram.com", urlMetadataProcessor.extractDomain("https://www.instagram.com/reel/C7xyz/"))
        assertEquals("github.com", urlMetadataProcessor.extractDomain("https://github.com/facebook/react"))
    }

    @Test
    fun testEntityExtractorRecognizesAllEntityTypes() {
        val sampleText = """
            Hey contact me at dev.support@example.org or call +1 555-234-5678.
            The conference costs $299 or ₹25,000 on 2026-09-15.
            Visit https://tuck.app for details. Follow #AndroidDev and #JetpackCompose.
        """.trimIndent()

        val entities = entityExtractor.extractEntities(sampleText, savedItemId = 1L)

        // Email
        assertTrue(entities.any { it.type == EntityType.EMAIL && it.value == "dev.support@example.org" })

        // Phone
        assertTrue(entities.any { it.type == EntityType.PHONE && it.normalizedValue.contains("5552345678") })

        // Money
        assertTrue(entities.any { it.type == EntityType.MONEY && it.value.contains("$299") })
        assertTrue(entities.any { it.type == EntityType.MONEY && it.value.contains("25,000") })

        // Date
        assertTrue(entities.any { it.type == EntityType.DATE && it.value == "2026-09-15" })

        // URL
        assertTrue(entities.any { it.type == EntityType.URL && it.value == "https://tuck.app" })

        // Hashtags
        assertTrue(entities.any { it.type == EntityType.HASHTAG && it.value.equals("#AndroidDev", ignoreCase = true) })
        assertTrue(entities.any { it.type == EntityType.HASHTAG && it.value.equals("#JetpackCompose", ignoreCase = true) })
    }

    @Test
    fun testRuleBasedClassifierAssignsSmartCollections() = runBlocking {
        // Programming item
        val codeItem = SavedItem(
            contentType = ContentType.URL,
            title = "Kotlin Flow StateFlow vs SharedFlow Guide",
            originalUrl = "https://kotlinlang.org/docs/flow.html",
            sourceDomain = "kotlinlang.org"
        )
        val codeResult = classifier.classify(codeItem)
        assertEquals("Programming", codeResult.primaryCategory)

        // Shopping item
        val shoppingItem = SavedItem(
            contentType = ContentType.URL,
            title = "Sony WH-1000XM5 Wireless Headphones on Amazon",
            originalUrl = "https://amazon.com/dp/B09XS7JWHH",
            sourceDomain = "amazon.com"
        )
        val shoppingResult = classifier.classify(shoppingItem)
        assertEquals("Shopping", shoppingResult.primaryCategory)

        // Food & Dining
        val recipeItem = SavedItem(
            contentType = ContentType.TEXT,
            title = "Classic Italian Carbonara Pasta Recipe",
            originalText = "Ingredients: Guanciale, Pecorino Romano, Eggs, Black Pepper, Spaghetti. Instructions for cooking pasta..."
        )
        val recipeResult = classifier.classify(recipeItem)
        assertEquals("Food & Dining", recipeResult.primaryCategory)
    }
}
