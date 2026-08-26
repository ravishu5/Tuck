package com.tuck.app

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tuck.app.data.local.db.TuckDatabase
import com.tuck.app.data.local.db.dao.SavedItemFtsDaoImpl
import com.tuck.app.data.local.db.entity.SavedItemEntity
import com.tuck.app.data.local.db.entity.SourceCommentEntity
import com.tuck.app.data.local.db.entity.SourcePostEntity
import com.tuck.app.data.repository.MaintenanceRepositoryImpl
import com.tuck.app.domain.model.ContentType
import com.tuck.app.domain.model.EntityType
import com.tuck.app.domain.model.ProcessingStatus
import com.tuck.app.processing.SourcePersonExtractor
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation test running on real SQLite / Android framework.
 *
 * Verifies that PERSON entities are created from structured source metadata,
 * persist properly in the SQLite entities table with producer = "source-metadata",
 * and that backfill is strictly idempotent on device.
 */
@RunWith(AndroidJUnit4::class)
class SourcePersonEnrichmentTest {

    private lateinit var db: TuckDatabase
    private lateinit var maintenanceRepo: MaintenanceRepositoryImpl
    private val extractor = SourcePersonExtractor()

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TuckDatabase::class.java
        ).build()
        db.openHelper.writableDatabase.execSQL(SavedItemFtsDaoImpl.CREATE_TABLE)

        maintenanceRepo = MaintenanceRepositoryImpl(
            sourceContentDao = db.sourceContentDao(),
            entityDao = db.entityDao(),
            savedItemDao = db.savedItemDao(),
            savedItemFtsDao = SavedItemFtsDaoImpl(db),
            tagDao = db.tagDao(),
            sourcePersonExtractor = extractor
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun sourceMetadataEmitsPersonEntitiesWithPlatformQualifiedHandles() = runBlocking {
        val savedItemDao = db.savedItemDao()
        val sourceContentDao = db.sourceContentDao()
        val entityDao = db.entityDao()

        // 1. Insert saved item
        val itemId = savedItemDao.insert(
            SavedItemEntity(
                contentType = ContentType.URL,
                title = "Graph Neural Networks Survey",
                originalUrl = "https://reddit.com/r/MachineLearning/comments/xyz",
                canonicalUrl = "https://reddit.com/r/MachineLearning/comments/xyz",
                sourceDomain = "reddit.com",
                processingStatus = ProcessingStatus.READY,
                createdAt = 1700000000000L,
                updatedAt = 1700000000000L
            )
        )

        // 2. Insert source post and comments
        sourceContentDao.insertPost(
            SourcePostEntity(
                itemId = itemId,
                platform = "REDDIT",
                community = "r/MachineLearning",
                authorHandle = "u/ml_researcher",
                title = "Graph Neural Networks Survey",
                score = 120,
                commentCount = 2,
                fetchedAt = 1700000000000L
            )
        )

        sourceContentDao.insertComments(
            listOf(
                SourceCommentEntity(
                    itemId = itemId,
                    depth = 0,
                    path = "0001",
                    authorHandle = "u/alice_eng",
                    bodyText = "Great survey!",
                    score = 45,
                    ordinal = 1
                ),
                SourceCommentEntity(
                    itemId = itemId,
                    depth = 1,
                    path = "0001.0001",
                    authorHandle = "bob_data",
                    bodyText = "Seconded",
                    score = 12,
                    ordinal = 2
                ),
                SourceCommentEntity(
                    itemId = itemId,
                    depth = 0,
                    path = "0002",
                    authorHandle = "[deleted]",
                    bodyText = "Deleted text",
                    score = 0,
                    ordinal = 3
                )
            )
        )

        // 3. Run backfill
        val processed = maintenanceRepo.backfillSourcePersonEntities()
        assertEquals(1, processed)

        // 4. Assert entities stored in database
        val entities = entityDao.getEntitiesForItem(itemId)
        assertEquals("Post author + 2 comment authors (ignoring [deleted]) = 3 entities", 3, entities.size)

        val normalized = entities.map { it.normalizedValue }.toSet()
        assertTrue(normalized.contains("reddit:ml_researcher"))
        assertTrue(normalized.contains("reddit:alice_eng"))
        assertTrue(normalized.contains("reddit:bob_data"))

        for (e in entities) {
            assertEquals(EntityType.PERSON, e.type)
            assertEquals("source-metadata", e.producer)
        }

        // 5. Assert idempotency on second run
        val processedSecondRun = maintenanceRepo.backfillSourcePersonEntities()
        assertEquals(1, processedSecondRun)

        val entitiesAfterSecondRun = entityDao.getEntitiesForItem(itemId)
        assertEquals("Entity count remains 3 after second run", 3, entitiesAfterSecondRun.size)
        assertEquals(
            entities.map { it.normalizedValue }.sorted(),
            entitiesAfterSecondRun.map { it.normalizedValue }.sorted()
        )
    }

    @Test
    fun sameAuthorAcrossTwoSavesResolvesToOneNormalizedValue() = runBlocking {
        val savedItemDao = db.savedItemDao()
        val sourceContentDao = db.sourceContentDao()
        val entityDao = db.entityDao()

        // Item 1
        val id1 = savedItemDao.insert(
            SavedItemEntity(
                contentType = ContentType.URL,
                title = "Save 1",
                originalUrl = "https://reddit.com/r/android/1",
                sourceDomain = "reddit.com",
                processingStatus = ProcessingStatus.READY,
                createdAt = 1700000000000L,
                updatedAt = 1700000000000L
            )
        )
        sourceContentDao.insertPost(
            SourcePostEntity(
                itemId = id1,
                platform = "REDDIT",
                authorHandle = "u/ravi",
                title = "Save 1",
                score = 10,
                commentCount = 0,
                fetchedAt = 1700000000000L
            )
        )

        // Item 2
        val id2 = savedItemDao.insert(
            SavedItemEntity(
                contentType = ContentType.URL,
                title = "Save 2",
                originalUrl = "https://reddit.com/r/android/2",
                sourceDomain = "reddit.com",
                processingStatus = ProcessingStatus.READY,
                createdAt = 1700000001000L,
                updatedAt = 1700000001000L
            )
        )
        sourceContentDao.insertPost(
            SourcePostEntity(
                itemId = id2,
                platform = "REDDIT",
                authorHandle = "ravi", // Without "u/"
                title = "Save 2",
                score = 20,
                commentCount = 0,
                fetchedAt = 1700000001000L
            )
        )

        maintenanceRepo.backfillSourcePersonEntities()

        val entities1 = entityDao.getEntitiesForItem(id1)
        val entities2 = entityDao.getEntitiesForItem(id2)

        assertEquals(1, entities1.size)
        assertEquals(1, entities2.size)
        assertEquals("reddit:ravi", entities1.first().normalizedValue)
        assertEquals("reddit:ravi", entities2.first().normalizedValue)
        assertEquals(entities1.first().normalizedValue, entities2.first().normalizedValue)
    }

    @Test
    fun sameDisplayNameOnDifferentPlatformsProducesDistinctEntities() = runBlocking {
        val savedItemDao = db.savedItemDao()
        val sourceContentDao = db.sourceContentDao()
        val entityDao = db.entityDao()

        // Reddit save
        val idReddit = savedItemDao.insert(
            SavedItemEntity(
                contentType = ContentType.URL,
                title = "Reddit post",
                sourceDomain = "reddit.com",
                processingStatus = ProcessingStatus.READY,
                createdAt = 1000L,
                updatedAt = 1000L
            )
        )
        sourceContentDao.insertPost(
            SourcePostEntity(
                itemId = idReddit,
                platform = "REDDIT",
                authorHandle = "ravi",
                title = "Reddit post",
                score = 5,
                commentCount = 0,
                fetchedAt = 1000L
            )
        )

        // Twitter save
        val idTwitter = savedItemDao.insert(
            SavedItemEntity(
                contentType = ContentType.URL,
                title = "Tweet",
                sourceDomain = "x.com",
                processingStatus = ProcessingStatus.READY,
                createdAt = 2000L,
                updatedAt = 2000L
            )
        )
        sourceContentDao.insertPost(
            SourcePostEntity(
                itemId = idTwitter,
                platform = "TWITTER",
                authorHandle = "@ravi",
                title = "Tweet",
                score = 15,
                commentCount = 0,
                fetchedAt = 2000L
            )
        )

        // YouTube save
        val idYouTube = savedItemDao.insert(
            SavedItemEntity(
                contentType = ContentType.URL,
                title = "Video",
                sourceDomain = "youtube.com",
                processingStatus = ProcessingStatus.READY,
                createdAt = 3000L,
                updatedAt = 3000L
            )
        )
        sourceContentDao.insertPost(
            SourcePostEntity(
                itemId = idYouTube,
                platform = "YOUTUBE",
                authorDisplay = "ravi",
                title = "Video",
                score = 100,
                commentCount = 0,
                fetchedAt = 3000L
            )
        )

        maintenanceRepo.backfillSourcePersonEntities()

        val redditEntity = entityDao.getEntitiesForItem(idReddit).first()
        val twitterEntity = entityDao.getEntitiesForItem(idTwitter).first()
        val youtubeEntity = entityDao.getEntitiesForItem(idYouTube).first()

        assertEquals("reddit:ravi", redditEntity.normalizedValue)
        assertEquals("twitter:ravi", twitterEntity.normalizedValue)
        assertEquals("youtube:ravi", youtubeEntity.normalizedValue)

        val set = setOf(redditEntity.normalizedValue, twitterEntity.normalizedValue, youtubeEntity.normalizedValue)
        assertEquals("All 3 platforms must produce distinct normalized entities", 3, set.size)
    }
}
