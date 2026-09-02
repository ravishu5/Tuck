package com.tuck.app

import com.tuck.app.data.local.db.dao.EntityDao
import com.tuck.app.data.local.db.dao.SavedItemDao
import com.tuck.app.data.local.db.dao.TagDao
import com.tuck.app.data.memory.RelatedItemsEngineImpl
import com.tuck.app.domain.model.ContentType
import com.tuck.app.domain.model.EntityType
import com.tuck.app.domain.model.ExtractedEntity
import com.tuck.app.domain.model.SavedItem
import com.tuck.app.domain.model.Tag
import com.tuck.app.domain.repository.SavedItemRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RelatedItemsEngineTest {

    private val savedItemRepository = mockk<SavedItemRepository>(relaxed = true)
    private val savedItemDao = mockk<SavedItemDao>(relaxed = true)
    private val entityDao = mockk<EntityDao>(relaxed = true)
    private val tagDao = mockk<TagDao>(relaxed = true)

    private lateinit var engine: RelatedItemsEngineImpl

    @Before
    fun setUp() {
        engine = RelatedItemsEngineImpl(
            savedItemRepository = savedItemRepository,
            savedItemDao = savedItemDao,
            entityDao = entityDao,
            tagDao = tagDao
        )
    }

    @Test
    fun testRediscoverItemsReturnsOlderActiveSaves() = runBlocking {
        val now = System.currentTimeMillis()
        val oldItem = SavedItem(
            id = 10L,
            contentType = ContentType.URL,
            title = "Architecture of SQLite",
            createdAt = now - (45L * 24 * 60 * 60 * 1000) // 45 days ago
        )
        val freshItem = SavedItem(
            id = 11L,
            contentType = ContentType.URL,
            title = "Today News",
            createdAt = now - (1L * 60 * 60 * 1000) // 1 hour ago
        )

        every { savedItemRepository.getAllActiveItems() } returns flowOf(listOf(oldItem, freshItem))

        val result = engine.getRediscoverItems(5).first()

        assertTrue(result.isNotEmpty())
        assertTrue(result.any { it.id == 10L })
    }

    @Test
    fun testRediscoverExcludesRecentSavesEntirely() = runBlocking {
        val now = System.currentTimeMillis()
        val freshItem = SavedItem(
            id = 11L,
            contentType = ContentType.URL,
            title = "Saved moments ago",
            createdAt = now - (60L * 1000)
        )

        every { savedItemRepository.getAllActiveItems() } returns flowOf(listOf(freshItem))

        val result = engine.getRediscoverItems(5).first()

        assertTrue(
            "a vault with nothing old enough must surface nothing, not fall back to recent saves",
            result.isEmpty()
        )
    }
}
