package com.tuck.app.data.memory

import com.tuck.app.data.local.db.dao.EntityDao
import com.tuck.app.data.local.db.dao.SavedItemDao
import com.tuck.app.data.local.db.dao.TagDao
import com.tuck.app.domain.memory.RelatedItemsEngine
import com.tuck.app.domain.model.SavedItem
import com.tuck.app.domain.repository.SavedItemRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RelatedItemsEngineImpl @Inject constructor(
    private val savedItemRepository: SavedItemRepository,
    private val savedItemDao: SavedItemDao,
    private val entityDao: EntityDao,
    private val tagDao: TagDao
) : RelatedItemsEngine {

    override fun getRediscoverItems(limit: Int): Flow<List<SavedItem>> {
        return savedItemRepository.getAllActiveItems().map { allItems ->
            val now = System.currentTimeMillis()
            val sevenDaysAgo = now - (7L * 24 * 60 * 60 * 1000)

            // Only items tucked over 7 days ago. There is deliberately no fallback to
            // recent saves: "Rediscover from your vault" offering something saved
            // moments ago makes the feature look broken, so an empty vault shows
            // nothing and the section hides itself.
            allItems
                .filter { item -> item.createdAt < sevenDaysAgo && !item.isArchived }
                .shuffled()
                .take(limit)
        }
    }

    override fun getForgottenSaves(limit: Int): Flow<List<SavedItem>> {
        return savedItemRepository.getAllActiveItems().map { allItems ->
            val now = System.currentTimeMillis()
            val thirtyDaysAgo = now - (30L * 24 * 60 * 60 * 1000)

            // Strictly items saved > 30 days ago with 0 opens
            allItems.filter { item ->
                item.createdAt < thirtyDaysAgo && item.openCount == 0 && !item.isArchived
            }.sortedBy { it.createdAt }.take(limit)
        }
    }

    private fun extractKeywords(text: String): Set<String> {
        val stopWords = setOf(
            "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for", "with",
            "about", "against", "between", "into", "through", "during", "before", "after",
            "above", "below", "from", "up", "down", "is", "are", "was", "were", "be", "been",
            "being", "have", "has", "had", "do", "does", "did", "how", "what", "who", "where",
            "when", "why", "which", "this", "that", "these", "those", "post", "video", "link",
            "shared", "item", "guide", "tutorial"
        )
        return text.lowercase()
            .split(Regex("[^a-zA-Z0-9_-]+"))
            .filter { it.length > 3 && !stopWords.contains(it) }
            .toSet()
    }
}
