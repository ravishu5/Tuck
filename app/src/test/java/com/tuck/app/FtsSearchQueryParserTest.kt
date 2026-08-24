package com.tuck.app

import com.tuck.app.data.local.db.dao.CollectionDao
import com.tuck.app.data.local.db.dao.EntityDao
import com.tuck.app.data.local.db.dao.SavedItemDao
import com.tuck.app.data.local.db.dao.SavedItemFtsDao
import com.tuck.app.data.local.db.dao.TagDao
import com.tuck.app.data.repository.KeywordSearchEngine
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class FtsSearchQueryParserTest {

    private lateinit var searchEngine: KeywordSearchEngine

    @Before
    fun setUp() {
        val savedItemDao = mockk<SavedItemDao>(relaxed = true)
        val savedItemFtsDao = mockk<SavedItemFtsDao>(relaxed = true)
        val entityDao = mockk<EntityDao>(relaxed = true)
        val tagDao = mockk<TagDao>(relaxed = true)
        val collectionDao = mockk<CollectionDao>(relaxed = true)

        searchEngine = KeywordSearchEngine(
            savedItemDao = savedItemDao,
            savedItemFtsDao = savedItemFtsDao,
            entityDao = entityDao,
            tagDao = tagDao,
            collectionDao = collectionDao
        )
    }

    @Test
    fun testFormatFtsQueryMultiWord() {
        val raw = "React performance"
        val formatted = searchEngine.formatFtsQuery(raw)
        assertEquals("React* performance*", formatted)
    }

    @Test
    fun testFormatFtsQueryWithPunctuation() {
        val raw = "restaurant, Kolkata!"
        val formatted = searchEngine.formatFtsQuery(raw)
        assertEquals("restaurant* Kolkata*", formatted)
    }

    @Test
    fun testFormatFtsQueryWithCurrency() {
        val raw = "headphones ₹3000"
        val formatted = searchEngine.formatFtsQuery(raw)
        assertEquals("headphones* ₹3000*", formatted)
    }

    @Test
    fun testFormatFtsQuerySingleWord() {
        val raw = "insurance"
        val formatted = searchEngine.formatFtsQuery(raw)
        assertEquals("insurance*", formatted)
    }

    @Test
    fun testFormatFtsQueryBlank() {
        val raw = "   "
        val formatted = searchEngine.formatFtsQuery(raw)
        assertEquals("", formatted)
    }
}
