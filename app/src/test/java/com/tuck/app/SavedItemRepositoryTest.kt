package com.tuck.app

import com.tuck.app.data.local.db.dao.CollectionDao
import com.tuck.app.data.local.db.dao.EntityDao
import com.tuck.app.data.local.db.dao.SavedItemDao
import com.tuck.app.data.local.db.dao.SavedItemFtsDao
import com.tuck.app.data.local.db.dao.TagDao
import com.tuck.app.data.local.db.entity.SavedItemEntity
import com.tuck.app.data.local.storage.FileStorageService
import com.tuck.app.data.repository.SavedItemRepositoryImpl
import com.tuck.app.domain.model.ContentType
import com.tuck.app.domain.model.ProcessingStatus
import com.tuck.app.domain.model.SavedItem
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class SavedItemRepositoryTest {

    private val savedItemDao: SavedItemDao = mockk(relaxed = true)
    private val savedItemFtsDao: SavedItemFtsDao = mockk(relaxed = true)
    private val entityDao: EntityDao = mockk(relaxed = true)
    private val tagDao: TagDao = mockk(relaxed = true)
    private val collectionDao: CollectionDao = mockk(relaxed = true)
    private val fileStorageService: FileStorageService = mockk(relaxed = true)

    private lateinit var repository: SavedItemRepositoryImpl

    @Before
    fun setUp() {
        repository = SavedItemRepositoryImpl(
            savedItemDao = savedItemDao,
            savedItemFtsDao = savedItemFtsDao,
            entityDao = entityDao,
            tagDao = tagDao,
            collectionDao = collectionDao,
            fileStorageService = fileStorageService
        )
    }

    @Test
    fun testInsertItemGeneratesIdAndFts() = runBlocking {
        val item = SavedItem(
            contentType = ContentType.URL,
            title = "Test Page",
            originalUrl = "https://example.com"
        )

        coEvery { savedItemDao.insert(any()) } returns 42L

        val id = repository.insertItem(item)

        assertEquals(42L, id)
        coVerify(exactly = 1) { savedItemDao.insert(any()) }
        coVerify(exactly = 1) { savedItemFtsDao.insertOrUpdate(match { it.rowid == 42L && it.title == "Test Page" }) }
    }

    @Test
    fun testGetItemByIdLoadsEntitiesAndTags() = runBlocking {
        val entity = SavedItemEntity(
            id = 10L,
            contentType = ContentType.TEXT,
            title = "My Note",
            originalText = "Sample note text",
            processingStatus = ProcessingStatus.READY
        )

        coEvery { savedItemDao.getItemById(10L) } returns entity
        coEvery { entityDao.getEntitiesForItem(10L) } returns emptyList()
        coEvery { tagDao.getTagsForSavedItem(10L) } returns emptyList()
        coEvery { collectionDao.getCollectionsForSavedItem(10L) } returns emptyList()

        val loaded = repository.getItemById(10L)

        assertNotNull(loaded)
        assertEquals(10L, loaded?.id)
        assertEquals("My Note", loaded?.title)
        assertEquals("Sample note text", loaded?.originalText)
    }

    @Test
    fun testMoveToTrashCallsDao() = runBlocking {
        repository.moveToTrash(15L)
        coVerify(exactly = 1) { savedItemDao.moveToTrash(15L, any()) }
    }

    @Test
    fun testRestoreFromTrashCallsDao() = runBlocking {
        repository.restoreFromTrash(15L)
        coVerify(exactly = 1) { savedItemDao.restoreFromTrash(15L, any()) }
    }

    @Test
    fun testSetFavoriteCallsDao() = runBlocking {
        repository.setFavorite(15L, true)
        coVerify(exactly = 1) { savedItemDao.setFavorite(15L, true, any()) }
    }
}
