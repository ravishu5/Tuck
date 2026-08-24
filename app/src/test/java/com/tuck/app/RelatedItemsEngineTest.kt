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
    fun testFindRelatedItemsMatchesByEntitiesTagsAndDomain() = runBlocking {
        val targetItem = SavedItem(
            id = 1L,
            contentType = ContentType.URL,
            title = "Introduction to Graph Neural Networks",
            sourceDomain = "arxiv.org",
            entities = listOf(
                ExtractedEntity(savedItemId = 1L, type = EntityType.ORGANIZATION, value = "DeepMind", normalizedValue = "deepmind"),
                ExtractedEntity(savedItemId = 1L, type = EntityType.URL, value = "https://arxiv.org/abs/123", normalizedValue = "https://arxiv.org/abs/123")
            ),
            tags = listOf(Tag(id = 1L, name = "Machine Learning"), Tag(id = 2L, name = "GNN"))
        )

        val relatedItem1 = SavedItem(
            id = 2L,
            contentType = ContentType.URL,
            title = "DeepMind GNN Architecture Research",
            sourceDomain = "arxiv.org",
            entities = listOf(
                ExtractedEntity(savedItemId = 2L, type = EntityType.ORGANIZATION, value = "DeepMind", normalizedValue = "deepmind")
            ),
            tags = listOf(Tag(id = 1L, name = "Machine Learning"))
        )

        val unrelatedItem = SavedItem(
            id = 3L,
            contentType = ContentType.TEXT,
            title = "Grocery Shopping List",
            sourceDomain = "Notes",
            entities = emptyList(),
            tags = listOf(Tag(id = 3L, name = "Shopping"))
        )

        coEvery { savedItemRepository.getItemById(1L) } returns targetItem
        every { savedItemRepository.getAllActiveItems() } returns flowOf(listOf(targetItem, relatedItem1, unrelatedItem))

        val result = engine.findRelatedItems(1L, 5).first()

        assertEquals(1, result.size)
        assertEquals(2L, result[0].id)
        assertEquals("DeepMind GNN Architecture Research", result[0].title)
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
}
