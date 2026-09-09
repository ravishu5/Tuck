package com.tuck.app

import com.tuck.app.domain.model.ContentType
import com.tuck.app.domain.model.SavedItem
import com.tuck.app.processing.RuleBasedContentClassifier
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RuleBasedContentClassifierTest {

    private lateinit var classifier: RuleBasedContentClassifier

    @Before
    fun setUp() {
        classifier = RuleBasedContentClassifier()
    }

    @Test
    fun testClassifyProgrammingLink() = runBlocking {
        val item = SavedItem(
            contentType = ContentType.URL,
            title = "React Performance Optimization Guide",
            description = "Learn how to optimize React rendering performance with hooks and memoization.",
            originalUrl = "https://react.dev/learn/render-performance",
            sourceDomain = "react.dev"
        )
        val result = classifier.classify(item)
        assertEquals("Programming", result.primaryCategory)
    }

    @Test
    fun testClassifyResearchPaper() = runBlocking {
        val item = SavedItem(
            contentType = ContentType.PDF,
            title = "Attention Is All You Need",
            extractedText = "Abstract: We propose the Transformer, a novel architecture based solely on attention mechanisms.",
            originalUrl = "https://arxiv.org/abs/1706.03762",
            sourceDomain = "arxiv.org"
        )
        val result = classifier.classify(item)
        assertEquals("Research", result.primaryCategory)
    }

    @Test
    fun testClassifyRestaurantFood() = runBlocking {
        val item = SavedItem(
            contentType = ContentType.URL,
            title = "Peter Cat Restaurant Kolkata Menu",
            description = "Famous for Cheelo Kababs, continental cuisine, dinner dining in Park Street.",
            originalUrl = "https://www.zomato.com/kolkata/peter-cat-park-street-area",
            sourceDomain = "zomato.com"
        )
        val result = classifier.classify(item)
        assertEquals("Food & Dining", result.primaryCategory)
    }

    @Test
    fun testClassifyTravelHotel() = runBlocking {
        val item = SavedItem(
            contentType = ContentType.URL,
            title = "Grand Hotel Paris - Booking & Reservation",
            description = "Book your flight and luxury hotel resort stay near Eiffel Tower.",
            originalUrl = "https://www.booking.com/hotel/fr/grand-paris.html",
            sourceDomain = "booking.com"
        )
        val result = classifier.classify(item)
        assertEquals("Travel", result.primaryCategory)
    }
}
