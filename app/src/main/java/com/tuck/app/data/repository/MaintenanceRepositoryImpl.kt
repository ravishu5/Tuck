package com.tuck.app.data.repository

import com.tuck.app.data.local.db.dao.EntityDao
import com.tuck.app.data.local.db.dao.SavedItemDao
import com.tuck.app.data.local.db.dao.SavedItemFtsDao
import com.tuck.app.data.local.db.dao.SourceContentDao
import com.tuck.app.data.local.db.dao.TagDao
import com.tuck.app.data.local.db.entity.SavedItemFtsEntity
import com.tuck.app.domain.repository.MaintenanceRepository
import com.tuck.app.processing.SourcePersonExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MaintenanceRepositoryImpl @Inject constructor(
    private val sourceContentDao: SourceContentDao,
    private val entityDao: EntityDao,
    private val savedItemDao: SavedItemDao,
    private val savedItemFtsDao: SavedItemFtsDao,
    private val tagDao: TagDao,
    private val sourcePersonExtractor: SourcePersonExtractor
) : MaintenanceRepository {

    override suspend fun backfillSourcePersonEntities(): Int = withContext(Dispatchers.IO) {
        val posts = sourceContentDao.getAllPosts()
        var processedCount = 0

        for (post in posts) {
            val itemId = post.itemId
            val comments = sourceContentDao.getCommentsTreeSync(itemId)
            val commentAuthors = comments.mapNotNull { it.authorHandle }

            val newEntities = sourcePersonExtractor.extractEntities(
                savedItemId = itemId,
                platform = post.platform,
                postAuthor = post.authorHandle,
                postAuthorDisplay = post.authorDisplay,
                fallbackCommentAuthors = commentAuthors
            )

            // Remove existing source-metadata entities and insert fresh ones (idempotent)
            entityDao.deleteForSavedItemByProducer(itemId, "source-metadata")
            if (newEntities.isNotEmpty()) {
                entityDao.insertAll(newEntities)
            }

            // Refresh FTS index for this item
            val item = savedItemDao.getItemById(itemId)
            if (item != null) {
                val allEntities = entityDao.getEntitiesForItem(itemId)
                val tags = tagDao.getTagsForSavedItem(itemId)
                val ftsEntity = SavedItemFtsEntity(
                    rowid = itemId,
                    title = item.title,
                    description = item.description.orEmpty(),
                    originalUrl = item.originalUrl.orEmpty(),
                    sourceDomain = item.sourceDomain.orEmpty(),
                    originalText = item.originalText.orEmpty(),
                    extractedText = item.extractedText.orEmpty(),
                    ocrText = item.ocrText.orEmpty(),
                    tags = tags.joinToString(" ") { it.name },
                    entities = allEntities.joinToString(" ") { it.value }
                )
                savedItemFtsDao.insertOrUpdate(ftsEntity)
            }

            processedCount++
        }

        processedCount
    }
}
