package com.tuck.app

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.tuck.app.data.local.db.dao.SourceContentDao
import com.tuck.app.data.local.db.entity.SourceCommentEntity
import com.tuck.app.data.local.db.entity.SourcePostEntity
import com.tuck.app.domain.memory.RelatedItemsEngine
import com.tuck.app.domain.model.Collection
import com.tuck.app.domain.model.ContentType
import com.tuck.app.domain.model.SavedItem
import com.tuck.app.domain.repository.CollectionRepository
import com.tuck.app.domain.repository.SavedItemRepository
import com.tuck.app.ui.detail.ItemDetailViewModel
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ItemDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val savedStateHandle = SavedStateHandle(mapOf("itemId" to 42L))
    private val checklistDao: com.tuck.app.data.local.db.dao.ChecklistDao = mockk(relaxed = true) {
        // A relaxed mock returns a Flow that never emits, and combine waits for every
        // source - so without this the whole detail state would never produce a value.
        every { getForItem(any()) } returns flowOf(emptyList())
    }
    private val savedItemRepository = mockk<SavedItemRepository>(relaxed = true)
    private val collectionRepository = mockk<CollectionRepository>(relaxed = true)
    private val sourceContentDao = mockk<SourceContentDao>(relaxed = true)
    private val relatedItemsEngine = mockk<RelatedItemsEngine>(relaxed = true)

    private val sampleItem = SavedItem(
        id = 42L,
        contentType = ContentType.URL,
        title = "GNNs in 2026",
        sourceDomain = "reddit.com"
    )

    private val samplePost = SourcePostEntity(
        itemId = 42L,
        platform = "REDDIT",
        title = "GNNs in 2026",
        score = 500,
        commentCount = 2,
        fetchedAt = 1000L
    )

    private val sampleComment = SourceCommentEntity(
        id = 1L,
        itemId = 42L,
        depth = 0,
        path = "0001",
        authorHandle = "alice",
        bodyText = "Great overview of GNNs",
        score = 40,
        isOp = false,
        isStickied = false,
        childCount = 0,
        ordinal = 1
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { savedItemRepository.getItemByIdFlow(42L) } returns flowOf(sampleItem)
        every { collectionRepository.getAllCollections() } returns flowOf(listOf(Collection(id = 1L, name = "ML")))
        every { sourceContentDao.getPostFlow(42L) } returns flowOf(samplePost)
        every { sourceContentDao.getCommentsTree(42L) } returns flowOf(listOf(sampleComment))
        every { relatedItemsEngine.findRelatedItems(42L, 5) } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testViewModelLoadsItemAndSourceCommentsTree() = runTest(testDispatcher) {
        val viewModel = ItemDetailViewModel(
            savedStateHandle = savedStateHandle,
            savedItemRepository = savedItemRepository,
            checklistDao = checklistDao,
            collectionRepository = collectionRepository,
            sourceContentDao = sourceContentDao,
            relatedItemsEngine = relatedItemsEngine
        )

        viewModel.uiState.test {
            val initial = awaitItem()
            val state = if (initial.isLoading) awaitItem() else initial

            assertNotNull(state.item)
            assertEquals("GNNs in 2026", state.item?.title)
            assertEquals(1, state.commentsTree.size)
            assertEquals("alice", state.commentsTree[0].authorHandle)
            assertEquals("0001", state.commentsTree[0].path)

            cancelAndIgnoreRemainingEvents()
        }

        coVerify { savedItemRepository.markOpened(42L) }
    }
}
