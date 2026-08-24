package com.tuck.app.data.repository

import com.tuck.app.data.local.db.dao.CollectionDao
import com.tuck.app.data.local.db.dao.EntityDao
import com.tuck.app.data.local.db.dao.SavedItemDao
import com.tuck.app.data.local.db.dao.SavedItemFtsDao
import com.tuck.app.data.local.db.dao.SearchHistoryDao
import com.tuck.app.data.local.db.dao.TagDao
import com.tuck.app.data.local.db.entity.SavedItemEntity
import com.tuck.app.data.local.db.entity.SearchHistoryEntity
import com.tuck.app.domain.model.Collection
import com.tuck.app.domain.model.ExtractedEntity
import com.tuck.app.domain.model.SavedItem
import com.tuck.app.domain.model.SearchFilter
import com.tuck.app.domain.model.SearchResult
import com.tuck.app.domain.model.SortOrder
import com.tuck.app.domain.model.Tag
import com.tuck.app.domain.repository.SearchEngine
import com.tuck.app.domain.repository.SearchRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KeywordSearchEngine @Inject constructor(
    private val savedItemDao: SavedItemDao,
    private val savedItemFtsDao: SavedItemFtsDao,
    private val entityDao: EntityDao,
    private val tagDao: TagDao,
    private val collectionDao: CollectionDao
) : SearchEngine {

    override val name: String = "FTS4_Ranked_Engine"

    override suspend fun search(filter: SearchFilter): List<SearchResult> = withContext(Dispatchers.IO) {
        val query = filter.query.trim()
        if (query.isBlank()) return@withContext emptyList()

        val matchingSnippets = mutableMapOf<Long, String?>()
        val itemIdsOrdered = mutableListOf<Long>()

        // 1. Primary: FTS4 match, ordered by weighted matchinfo relevance
        val ftsQuery = buildString {
            append(formatFtsQuery(query))
            // FTS4 supports `column:token`, so a tag filter needs no extra join.
            filter.tag?.let { tag ->
                if (isNotEmpty()) append(" ")
                append("tags:").append(tag.replace(Regex("[^a-zA-Z0-9_]"), "")).append("*")
            }
        }.trim()
        if (ftsQuery.isNotBlank()) {
            try {
                val ftsResults = savedItemFtsDao.searchFtsWithSnippet(ftsQuery)
                for (res in ftsResults) {
                    if (!itemIdsOrdered.contains(res.rowid)) {
                        itemIdsOrdered.add(res.rowid)
                        matchingSnippets[res.rowid] = res.snippet
                    }
                }
            } catch (e: Exception) {
                // Ignore and proceed to fallback
            }
        }

        // 2. Fallback / Complementary: Direct SQL LIKE query on saved_items
        try {
            val likeResults = savedItemDao.searchItemsLike(query)
            for (item in likeResults) {
                if (!itemIdsOrdered.contains(item.id)) {
                    itemIdsOrdered.add(item.id)
                }
            }
        } catch (e: Exception) {
            // Ignore
        }

        if (itemIdsOrdered.isEmpty()) return@withContext emptyList()

        // Fetch full entities in preserved search relevance order
        val entitiesMap = savedItemDao.getItemsByIds(itemIdsOrdered).associateBy { it.id }
        val itemEntities = itemIdsOrdered.mapNotNull { entitiesMap[it] }

        // Filter and enrich
        val now = System.currentTimeMillis()
        val minDate = filter.dateRangeDays?.let { days ->
            now - TimeUnit.DAYS.toMillis(days.toLong())
        } ?: 0L

        val filtered = itemEntities.filter { entity ->
            if (entity.isDeleted) return@filter false
            if (filter.isArchivedOnly && !entity.isArchived) return@filter false
            if (!filter.isArchivedOnly && entity.isArchived) return@filter false
            if (filter.isFavoriteOnly && !entity.isFavorite) return@filter false
            if (filter.contentType != null && entity.contentType != filter.contentType) return@filter false
            if (filter.sourceDomain != null && entity.sourceDomain != filter.sourceDomain) return@filter false
            if (minDate > 0L && entity.createdAt < minDate) return@filter false
            filter.createdAfter?.let { if (entity.createdAt < it) return@filter false }
            filter.createdBefore?.let { if (entity.createdAt > it) return@filter false }
            true
        }

        // Apply collection filter if specified, resolving `in:<name>` to an id first
        val resolvedCollectionId = filter.collectionId
            ?: filter.collectionName?.let { name -> collectionDao.getByName(name)?.id }
        val collectionFiltered = if (resolvedCollectionId != null) {
            val targetColId = resolvedCollectionId
            filtered.filter { entity ->
                val cols = collectionDao.getCollectionsForSavedItem(entity.id)
                cols.any { it.id == targetColId }
            }
        } else {
            filtered
        }

        // Sort items
        val sorted = when (filter.sortOrder) {
            SortOrder.RELEVANCE -> collectionFiltered // Preserves the relevance ranking from the FTS query
            SortOrder.NEWEST -> collectionFiltered.sortedByDescending { it.createdAt }
            SortOrder.OLDEST -> collectionFiltered.sortedBy { it.createdAt }
            SortOrder.RECENTLY_OPENED -> collectionFiltered.sortedByDescending { it.lastOpenedAt ?: it.createdAt }
        }

        // Convert to SearchResults
        sorted.mapIndexed { index, entity ->
            val domainItem = loadDomainItem(entity)
            val score = 100.0 - (index * 2.0).coerceAtMost(90.0)
            SearchResult(
                item = domainItem,
                matchSnippet = matchingSnippets[entity.id]?.cleanSnippet(),
                score = score
            )
        }
    }

    fun formatFtsQuery(rawQuery: String): String {
        // Strip problematic FTS special operators and escape
        // `-` is deliberately NOT preserved: in FTS4 query syntax a leading hyphen is
        // the NOT operator, so "nike-air" parsed as `nike AND NOT air` and excluded
        // the very rows it should have matched.
        val cleaned = rawQuery.replace(Regex("[^a-zA-Z0-9\\s₹$€£#@_.]"), " ").trim()
        val tokens = cleaned.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return ""

        // Bareword prefix tokens: FTS4 applies `*` only to an unquoted token, not to a
        // quoted phrase. The cleaning pass above has already removed anything the
        // query parser would choke on.
        return tokens.joinToString(" ") { "$it*" }
    }

    private fun String.cleanSnippet(): String {
        return this.replace("<b>", "").replace("</b>", "").trim()
    }

    private suspend fun loadDomainItem(entity: SavedItemEntity): SavedItem {
        val entities = entityDao.getEntitiesForItem(entity.id).map {
            ExtractedEntity(
                id = it.id,
                savedItemId = it.savedItemId,
                type = it.type,
                value = it.value,
                normalizedValue = it.normalizedValue
            )
        }
        val tags = tagDao.getTagsForSavedItem(entity.id).map {
            Tag(id = it.id, name = it.name)
        }
        val collections = collectionDao.getCollectionsForSavedItem(entity.id).map {
            Collection(
                id = it.id,
                name = it.name,
                isAutoGenerated = it.isAutoGenerated,
                icon = it.icon,
                createdAt = it.createdAt
            )
        }
        return SavedItem(
            id = entity.id,
            contentType = entity.contentType,
            title = entity.title,
            description = entity.description,
            originalUrl = entity.originalUrl,
            canonicalUrl = entity.canonicalUrl,
            sourceDomain = entity.sourceDomain,
            sourceApp = entity.sourceApp,
            mimeType = entity.mimeType,
            localFilePath = entity.localFilePath,
            thumbnailPath = entity.thumbnailPath,
            originalText = entity.originalText,
            extractedText = entity.extractedText,
            ocrText = entity.ocrText,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            lastOpenedAt = entity.lastOpenedAt,
            isFavorite = entity.isFavorite,
            isArchived = entity.isArchived,
            isDeleted = entity.isDeleted,
            processingStatus = entity.processingStatus,
            entities = entities,
            tags = tags,
            collections = collections
        )
    }
}

@Singleton
class SearchRepositoryImpl @Inject constructor(
    private val keywordEngine: KeywordSearchEngine,
    private val searchHistoryDao: SearchHistoryDao
) : SearchRepository {

    override fun search(filter: SearchFilter): Flow<List<SearchResult>> = flow {
        val results = executeSearch(filter)
        emit(results)
    }.flowOn(Dispatchers.IO)

    override suspend fun executeSearch(filter: SearchFilter): List<SearchResult> = withContext(Dispatchers.IO) {
        if (filter.query.isNotBlank()) {
            saveSearchQuery(filter.query)
        }
        keywordEngine.search(filter)
    }

    override fun getRecentSearchQueries(): Flow<List<String>> {
        return searchHistoryDao.getRecentQueries(10)
    }

    override suspend fun saveSearchQuery(query: String) = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.length >= 2) {
            searchHistoryDao.insertOrUpdate(
                SearchHistoryEntity(
                    query = trimmed,
                    searchedAt = System.currentTimeMillis()
                )
            )
        }
    }

    override suspend fun clearSearchHistory() = withContext(Dispatchers.IO) {
        searchHistoryDao.clearHistory()
    }
}
