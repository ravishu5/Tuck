package com.tuck.app

import com.tuck.app.domain.model.EntityType
import com.tuck.app.processing.EntityExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class EntityExtractorTest {

    private lateinit var extractor: EntityExtractor

    @Before
    fun setUp() {
        extractor = EntityExtractor()
    }

    @Test
    fun testExtractEmails() {
        val text = "Contact support at hello@tuck.app or dev.team+feedback@example.com for help."
        val entities = extractor.extractEntities(text)

        val emails = entities.filter { it.type == EntityType.EMAIL }.map { it.value }
        assertEquals(2, emails.size)
        assertTrue(emails.contains("hello@tuck.app"))
        assertTrue(emails.contains("dev.team+feedback@example.com"))
    }

    @Test
    fun testExtractPhoneNumbers() {
        val text = "Call us at +1 415-555-0199 or 9876543210 for inquiries."
        val entities = extractor.extractEntities(text)

        val phones = entities.filter { it.type == EntityType.PHONE }.map { it.value }
        assertTrue(phones.isNotEmpty())
    }

    @Test
    fun testExtractMoney() {
        val text = "Sony WH-1000XM5 headphones on sale for ₹24,990 (down from $399 or €350)."
        val entities = extractor.extractEntities(text)

        val moneyValues = entities.filter { it.type == EntityType.MONEY }.map { it.value }
        assertTrue(moneyValues.any { it.contains("24,990") || it.contains("₹") })
        assertTrue(moneyValues.any { it.contains("$399") || it.contains("399") })
    }

    @Test
    fun testExtractHashtags() {
        val text = "Building an amazing Android app! #android #kotlin #jetpackcompose"
        val entities = extractor.extractEntities(text)

        val hashtags = entities.filter { it.type == EntityType.HASHTAG }.map { it.value }
        assertEquals(3, hashtags.size)
        assertTrue(hashtags.contains("#android"))
        assertTrue(hashtags.contains("#kotlin"))
        assertTrue(hashtags.contains("#jetpackcompose"))
    }

    @Test
    fun testExtractUrls() {
        val text = "Check out the doc here: https://developer.android.com/guide and http://kotlinlang.org"
        val entities = extractor.extractEntities(text)

        val urls = entities.filter { it.type == EntityType.URL }.map { it.value }
        assertEquals(2, urls.size)
        assertTrue(urls.contains("https://developer.android.com/guide"))
        assertTrue(urls.contains("http://kotlinlang.org"))
    }
}
