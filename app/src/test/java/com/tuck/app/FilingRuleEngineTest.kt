package com.tuck.app

import com.tuck.app.data.local.db.dao.CollectionDao
import com.tuck.app.data.local.db.dao.FilingRuleDao
import com.tuck.app.data.local.db.entity.FilingRuleEntity
import com.tuck.app.domain.model.ContentType
import com.tuck.app.domain.model.SavedItem
import com.tuck.app.processing.FilingRuleEngine
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FilingRuleEngineTest {

    private val filingRuleDao: FilingRuleDao = mockk(relaxed = true)
    private val collectionDao: CollectionDao = mockk(relaxed = true)
    private val engine = FilingRuleEngine(filingRuleDao, collectionDao)

    private fun rule(id: Long, query: String, collectionId: Long) =
        FilingRuleEntity(id = id, query = query, collectionId = collectionId)

    private val redditPost = SavedItem(
        id = 7L,
        contentType = ContentType.URL,
        title = "Best backend for Android",
        sourceDomain = "reddit.com"
    )

    @Test
    fun aMatchingRuleFilesTheItemAndRecordsTheMatch() = runBlocking {
        coEvery { filingRuleDao.getEnabledRules() } returns listOf(rule(1L, "source:reddit", 42L))

        val filed = engine.apply(redditPost)

        assertEquals(listOf(42L), filed)
        // The cross-ref stamps its own addedAt, so match on identity rather than equality.
        coVerify(exactly = 1) {
            collectionDao.insertItemCollectionCrossRef(
                match { it.savedItemId == 7L && it.collectionId == 42L }
            )
        }
        // recordMatch stamps its own time, so match the id and let the clock be anything.
        coVerify(exactly = 1) { filingRuleDao.recordMatch(1L, any()) }
    }

    @Test
    fun aNonMatchingRuleFilesNothing() = runBlocking {
        coEvery { filingRuleDao.getEnabledRules() } returns listOf(rule(1L, "source:youtube", 42L))

        assertTrue(engine.apply(redditPost).isEmpty())
        coVerify(exactly = 0) { collectionDao.insertItemCollectionCrossRef(any()) }
    }

    @Test
    fun anItemCanSatisfySeveralRulesAndLandInAllOfThem() = runBlocking {
        coEvery { filingRuleDao.getEnabledRules() } returns listOf(
            rule(1L, "source:reddit", 42L),
            rule(2L, "type:url", 43L),
            rule(3L, "source:youtube", 44L)
        )

        val filed = engine.apply(redditPost)

        assertEquals("collections are many-to-many, so rules are additive", listOf(42L, 43L), filed)
    }

    @Test
    fun aMalformedRuleDoesNotStopTheOthersFromRunning() = runBlocking {
        coEvery { filingRuleDao.getEnabledRules() } returns listOf(
            rule(1L, "\"\"\"unclosed", 42L),
            rule(2L, "source:reddit", 43L)
        )

        val filed = engine.apply(redditPost)

        assertTrue("the good rule must still fire", filed.contains(43L))
    }

    @Test
    fun anEmptyRuleNeverFilesAnything() = runBlocking {
        // Belt and braces: the UI rejects these, but a rule that matches everything is
        // the worst failure mode here, so the engine refuses them too.
        coEvery { filingRuleDao.getEnabledRules() } returns listOf(rule(1L, "   ", 42L))

        assertTrue(engine.apply(redditPost).isEmpty())
    }

    @Test
    fun noRulesMeansNoWork() = runBlocking {
        coEvery { filingRuleDao.getEnabledRules() } returns emptyList()

        assertTrue(engine.apply(redditPost).isEmpty())
        coVerify(exactly = 0) { collectionDao.insertItemCollectionCrossRef(any()) }
    }
}
