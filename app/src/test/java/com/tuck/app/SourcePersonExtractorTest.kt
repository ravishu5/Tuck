package com.tuck.app

import com.tuck.app.data.local.db.dao.EntityDao
import com.tuck.app.data.local.db.dao.SavedItemDao
import com.tuck.app.data.local.db.dao.SavedItemFtsDao
import com.tuck.app.data.local.db.dao.SourceContentDao
import com.tuck.app.data.local.db.dao.TagDao
import com.tuck.app.data.local.db.entity.EntityEntity
import com.tuck.app.data.local.db.entity.SavedItemEntity
import com.tuck.app.data.local.db.entity.SourceCommentEntity
import com.tuck.app.data.local.db.entity.SourcePostEntity
import com.tuck.app.data.repository.MaintenanceRepositoryImpl
import com.tuck.app.domain.model.ContentType
import com.tuck.app.domain.model.EntityType
import com.tuck.app.domain.model.ProcessingStatus
import com.tuck.app.processing.SourcePersonExtractor
import com.tuck.app.processing.extractors.ExtractedComment
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SourcePersonExtractorTest {

    private lateinit var extractor: SourcePersonExtractor

    @Before
    fun setUp() {
        extractor = SourcePersonExtractor()
    }

    @Test
    fun testRedditHandleNormalization() {
        val cases = listOf("u/someone", "U/someone", "someone", "@someone")
        for (case in cases) {
            val result = extractor.normalizePerson("REDDIT", case)
            assertNotNull("Should not be null for $case", result)
            assertEquals("reddit:someone", result!!.second)
            assertEquals("u/someone", result.first)
        }
    }

    @Test
    fun testYouTubeChannelNormalization() {
        val withAt = extractor.normalizePerson("YOUTUBE", "@mkbhd")
        assertNotNull(withAt)
        assertEquals("youtube:mkbhd", withAt!!.second)
        assertEquals("@mkbhd", withAt.first)

        val channelName = extractor.normalizePerson("YOUTUBE", "Veritasium")
        assertNotNull(channelName)
        assertEquals("youtube:veritasium", channelName!!.second)
        assertEquals("Veritasium", channelName.first)
    }

    @Test
    fun testTwitterHandleNormalization() {
        val handle = extractor.normalizePerson("TWITTER", "@AndroidDev")
        assertNotNull(handle)
        assertEquals("twitter:androiddev", handle!!.second)
        assertEquals("@AndroidDev", handle.first)

        val handleNoAt = extractor.normalizePerson("TWITTER", "AndroidDev")
        assertNotNull(handleNoAt)
        assertEquals("twitter:androiddev", handleNoAt!!.second)
        assertEquals("@AndroidDev", handleNoAt.first)
    }

    @Test
    fun testWebAuthorNormalization() {
        val author = extractor.normalizePerson("WEB", "Dr. Richard Hipp")
        assertNotNull(author)
        assertEquals("web:dr. richard hipp", author!!.second)
        assertEquals("Dr. Richard Hipp", author.first)

        val withBy = extractor.normalizePerson("WEB", "By Jane Doe")
        assertNotNull(withBy)
        assertEquals("web:jane doe", withBy!!.second)
        assertEquals("Jane Doe", withBy.first)
    }

    @Test
    fun testIgnoresPlaceholdersAndNoise() {
        val noise = listOf("[deleted]", "[removed]", "deleted", "removed", "anonymous", "null", "", "   ")
        for (n in noise) {
            assertNull("Should ignore '$n' on Reddit", extractor.normalizePerson("REDDIT", n))
            assertNull("Should ignore '$n' on YouTube", extractor.normalizePerson("YOUTUBE", n))
            assertNull("Should ignore '$n' on Twitter", extractor.normalizePerson("TWITTER", n))
            assertNull("Should ignore '$n' on Web", extractor.normalizePerson("WEB", n))
        }
    }

    @Test
    fun testPlatformDisambiguationForSameDisplayName() {
        val name = "ravi"
        val reddit = extractor.normalizePerson("REDDIT", name)!!.second
        val twitter = extractor.normalizePerson("TWITTER", name)!!.second
        val youtube = extractor.normalizePerson("YOUTUBE", name)!!.second
        val web = extractor.normalizePerson("WEB", name)!!.second

        assertEquals("reddit:ravi", reddit)
        assertEquals("twitter:ravi", twitter)
        assertEquals("youtube:ravi", youtube)
        assertEquals("web:ravi", web)

        val set = setOf(reddit, twitter, youtube, web)
        assertEquals("All 4 platforms must produce distinct normalized values", 4, set.size)
    }

    @Test
    fun testSameAuthorAcrossTwoSavesResolvesToSameNormalizedValue() {
        val save1 = extractor.normalizePerson("REDDIT", "u/tech_enthusiast")
        val save2 = extractor.normalizePerson("REDDIT", "tech_enthusiast")

        assertNotNull(save1)
        assertNotNull(save2)
        assertEquals(save1!!.second, save2!!.second)
        assertEquals("reddit:tech_enthusiast", save1.second)
    }

    @Test
    fun testExtractEntitiesFromRedditThreadWithDeduplication() {
        val comments = listOf(
            ExtractedComment(
                id = "c1",
                author = "alice_eng",
                bodyText = "First comment",
                replies = listOf(
                    ExtractedComment(
                        id = "c1_1",
                        author = "bob_data",
                        bodyText = "Nested reply",
                        replies = listOf(
                            ExtractedComment(
                                id = "c1_1_1",
                                author = "alice_eng", // Repeated author in reply
                                bodyText = "Reply back"
                            )
                        )
                    )
                )
            ),
            ExtractedComment(
                id = "c2",
                author = "charlie_ai",
                bodyText = "Another comment"
            ),
            ExtractedComment(
                id = "c3",
                author = "[deleted]",
                bodyText = "Deleted comment"
            )
        )

        val entities = extractor.extractEntities(
            savedItemId = 42L,
            platform = "REDDIT",
            postAuthor = "ml_researcher",
            comments = comments
        )

        // Expected authors: ml_researcher (post), alice_eng, bob_data, charlie_ai (distinct)
        // [deleted] is filtered out
        // alice_eng is deduplicated
        assertEquals(4, entities.size)

        val normalized = entities.map { it.normalizedValue }
        assertTrue(normalized.contains("reddit:ml_researcher"))
        assertTrue(normalized.contains("reddit:alice_eng"))
        assertTrue(normalized.contains("reddit:bob_data"))
        assertTrue(normalized.contains("reddit:charlie_ai"))

        for (e in entities) {
            assertEquals(42L, e.savedItemId)
            assertEquals(EntityType.PERSON, e.type)
            assertEquals("source-metadata", e.producer)
        }
    }

    @Test
    fun testBackfillIdempotency() = runBlocking {
        val sourceContentDao = mockk<SourceContentDao>()
        val entityDao = mockk<EntityDao>(relaxed = true)
        val savedItemDao = mockk<SavedItemDao>()
        val savedItemFtsDao = mockk<SavedItemFtsDao>(relaxed = true)
        val tagDao = mockk<TagDao>(relaxed = true)

        val post = SourcePostEntity(
            itemId = 100L,
            platform = "REDDIT",
            authorHandle = "u/author_one",
            title = "Test Post",
            score = 10,
            commentCount = 1,
            fetchedAt = 1000L
        )
        val comment = SourceCommentEntity(
            itemId = 100L,
            depth = 0,
            path = "0001",
            authorHandle = "u/commenter_one",
            bodyText = "Comment text",
            score = 5,
            ordinal = 1
        )
        val item = SavedItemEntity(
            id = 100L,
            contentType = ContentType.URL,
            title = "Test Post",
            processingStatus = ProcessingStatus.READY,
            createdAt = 1000L,
            updatedAt = 1000L
        )

        coEvery { sourceContentDao.getAllPosts() } returns listOf(post)
        coEvery { sourceContentDao.getCommentsTreeSync(100L) } returns listOf(comment)
        coEvery { savedItemDao.getItemById(100L) } returns item
        coEvery { entityDao.getEntitiesForItem(100L) } returns emptyList()

        val capturedEntities = mutableListOf<List<EntityEntity>>()
        coEvery { entityDao.insertAll(capture(capturedEntities)) } returns Unit

        val maintenanceRepo = MaintenanceRepositoryImpl(
            sourceContentDao = sourceContentDao,
            entityDao = entityDao,
            savedItemDao = savedItemDao,
            savedItemFtsDao = savedItemFtsDao,
            tagDao = tagDao,
            sourcePersonExtractor = extractor
        )

        // Run 1
        val result1 = maintenanceRepo.backfillSourcePersonEntities()
        assertEquals(1, result1)

        // Run 2 (idempotent)
        val result2 = maintenanceRepo.backfillSourcePersonEntities()
        assertEquals(1, result2)

        // Verify that delete was called for each run
        coVerify(exactly = 2) { entityDao.deleteForSavedItemByProducer(100L, "source-metadata") }
        coVerify(exactly = 2) { entityDao.insertAll(any()) }

        assertEquals(2, capturedEntities.size)
        val firstRunRows = capturedEntities[0].map { it.normalizedValue }.sorted()
        val secondRunRows = capturedEntities[1].map { it.normalizedValue }.sorted()
        assertEquals(firstRunRows, secondRunRows)
        assertEquals(listOf("reddit:author_one", "reddit:commenter_one"), firstRunRows)
    }
}
