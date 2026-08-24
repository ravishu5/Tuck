package com.tuck.app.processing

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tuck.app.data.local.db.dao.CollectionDao
import com.tuck.app.data.local.db.dao.EntityDao
import com.tuck.app.data.local.db.dao.SavedItemDao
import com.tuck.app.data.local.db.dao.SavedItemFtsDao
import com.tuck.app.data.local.db.dao.TagDao
import com.tuck.app.data.local.db.entity.CollectionEntity
import com.tuck.app.data.local.db.entity.EntityEntity
import com.tuck.app.data.local.db.entity.SavedItemCollectionCrossRef
import com.tuck.app.data.local.db.entity.SavedItemFtsEntity
import com.tuck.app.data.local.db.entity.SavedItemTagCrossRef
import com.tuck.app.data.local.db.entity.TagEntity
import com.tuck.app.data.local.storage.FileStorageService
import com.tuck.app.domain.model.ContentType
import com.tuck.app.domain.model.ProcessingStatus
import com.tuck.app.domain.model.SavedItem
import com.tuck.app.domain.repository.SettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

@HiltWorker
class ItemProcessingWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted private val workerParams: WorkerParameters,
    private val savedItemDao: SavedItemDao,
    private val savedItemFtsDao: SavedItemFtsDao,
    private val entityDao: EntityDao,
    private val tagDao: TagDao,
    private val collectionDao: CollectionDao,
    private val fileStorageService: FileStorageService,
    private val urlMetadataProcessor: UrlMetadataProcessor,
    private val imageOcrProcessor: ImageOcrProcessor,
    private val pdfProcessor: PdfProcessor,
    private val entityExtractor: EntityExtractor,
    private val classifier: RuleBasedContentClassifier,
    private val settingsRepository: SettingsRepository
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val KEY_ITEM_ID = "key_item_id"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val itemId = inputData.getLong(KEY_ITEM_ID, -1L)
        if (itemId <= 0L) return@withContext Result.failure()

        val itemEntity = savedItemDao.getItemById(itemId) ?: return@withContext Result.failure()

        try {
            savedItemDao.updateStatus(itemId, ProcessingStatus.PROCESSING)

            val settings = try { settingsRepository.getSettings().first() } catch (e: Exception) { null }
            val saveCommentsEnabled = settings?.saveCommentsEnabled ?: true

            var finalTitle = itemEntity.title
            var finalDescription = itemEntity.description
            var finalOcrText = itemEntity.ocrText
            var finalExtractedText = itemEntity.extractedText
            var finalCanonicalUrl = itemEntity.canonicalUrl
            var finalDomain = itemEntity.sourceDomain
            var finalThumbnailPath = itemEntity.thumbnailPath
            var finalContentType = itemEntity.contentType
            var finalCommentsJson: String? = null

            val extractedEntities = mutableListOf<EntityEntity>()

            // 1. Process based on content type
            when (itemEntity.contentType) {
                ContentType.URL, ContentType.VIDEO -> {
                    itemEntity.originalUrl?.let { url ->
                        val meta = urlMetadataProcessor.extractMetadata(url)
                        if (!meta.title.isNullOrBlank() && (finalTitle.isBlank() || finalTitle == finalDomain || finalTitle == "Shared Link" || finalTitle == "Instagram Reel" || finalTitle == "Reddit Post" || finalTitle == "LinkedIn Post")) {
                            finalTitle = meta.title
                        }
                        if (!meta.description.isNullOrBlank()) {
                            finalDescription = meta.description
                        }
                        if (!meta.fullTextContent.isNullOrBlank()) {
                            finalExtractedText = meta.fullTextContent
                        }
                        finalCanonicalUrl = meta.canonicalUrl
                        finalDomain = meta.domain.ifBlank { finalDomain }

                        if (meta.inferredContentType != ContentType.URL) {
                            finalContentType = meta.inferredContentType
                        }

                        // Save comments if enabled and present
                        if (saveCommentsEnabled && meta.comments.isNotEmpty()) {
                            val commentsArray = JSONArray()
                            for (c in meta.comments) {
                                val cObj = JSONObject()
                                cObj.put("author", c.author)
                                cObj.put("text", c.text)
                                c.score?.let { cObj.put("score", it) }
                                c.timestamp?.let { cObj.put("timestamp", it) }
                                commentsArray.put(cObj)
                            }
                            finalCommentsJson = commentsArray.toString()
                        }

                        // Download and save thumbnail locally for preview cards
                        if (finalThumbnailPath.isNullOrBlank() && !meta.ogImageUrl.isNullOrBlank()) {
                            val savedThumb = fileStorageService.downloadAndSaveThumbnail(meta.ogImageUrl)
                            finalThumbnailPath = savedThumb ?: meta.ogImageUrl
                        }

                        // Extract entities from title, description, and full text
                        val textToScan = buildString {
                            append(finalTitle).append(" ")
                            append(finalDescription.orEmpty()).append(" ")
                            append(finalExtractedText.orEmpty())
                        }
                        entityExtractor.extractEntities(textToScan, itemId).forEach {
                            extractedEntities.add(
                                EntityEntity(
                                    savedItemId = itemId,
                                    type = it.type,
                                    value = it.value,
                                    normalizedValue = it.normalizedValue
                                )
                            )
                        }
                    }
                }

                ContentType.IMAGE, ContentType.MULTI_IMAGE -> {
                    itemEntity.localFilePath?.let { imagePath ->
                        val ocr = imageOcrProcessor.extractTextFromImageFile(imagePath)
                        if (!ocr.isNullOrBlank()) {
                            finalOcrText = ocr
                            // Extract entities from OCR text
                            entityExtractor.extractEntities(ocr, itemId).forEach {
                                extractedEntities.add(
                                    EntityEntity(
                                        savedItemId = itemId,
                                        type = it.type,
                                        value = it.value,
                                        normalizedValue = it.normalizedValue
                                    )
                                )
                            }
                        }
                    }
                }

                ContentType.PDF -> {
                    itemEntity.localFilePath?.let { pdfPath ->
                        val pdfText = pdfProcessor.extractText(pdfPath)
                        if (!pdfText.isNullOrBlank()) {
                            finalExtractedText = pdfText
                            // Extract entities from PDF text
                            entityExtractor.extractEntities(pdfText, itemId).forEach {
                                extractedEntities.add(
                                    EntityEntity(
                                        savedItemId = itemId,
                                        type = it.type,
                                        value = it.value,
                                        normalizedValue = it.normalizedValue
                                    )
                                )
                            }
                        }
                    }
                }

                ContentType.TEXT -> {
                    itemEntity.originalText?.let { text ->
                        entityExtractor.extractEntities(text, itemId).forEach {
                            extractedEntities.add(
                                EntityEntity(
                                    savedItemId = itemId,
                                    type = it.type,
                                    value = it.value,
                                    normalizedValue = it.normalizedValue
                                )
                            )
                        }
                    }
                }

                else -> {
                    // Document or Other
                }
            }

            // 2. Persist Extracted Entities
            if (extractedEntities.isNotEmpty()) {
                entityDao.deleteForSavedItem(itemId)
                entityDao.insertAll(extractedEntities)
            }

            // 3. Domain item for classification
            val domainItem = SavedItem(
                id = itemId,
                contentType = finalContentType,
                title = finalTitle,
                description = finalDescription,
                originalUrl = itemEntity.originalUrl,
                canonicalUrl = finalCanonicalUrl,
                sourceDomain = finalDomain,
                originalText = itemEntity.originalText,
                extractedText = finalExtractedText,
                ocrText = finalOcrText,
                thumbnailPath = finalThumbnailPath,
                entities = extractedEntities.map {
                    com.tuck.app.domain.model.ExtractedEntity(
                        savedItemId = itemId,
                        type = it.type,
                        value = it.value,
                        normalizedValue = it.normalizedValue
                    )
                }
            )

            // 4. Automatic Classification
            val classification = classifier.classify(domainItem)

            // Link to Smart Collection
            if (classification.primaryCategory.isNotBlank()) {
                val collection = collectionDao.getByName(classification.primaryCategory)
                if (collection == null) {
                    val newColId = collectionDao.insert(
                        CollectionEntity(
                            name = classification.primaryCategory,
                            isAutoGenerated = true
                        )
                    )
                    collectionDao.insertItemCollectionCrossRef(
                        SavedItemCollectionCrossRef(savedItemId = itemId, collectionId = newColId)
                    )
                } else {
                    collectionDao.insertItemCollectionCrossRef(
                        SavedItemCollectionCrossRef(savedItemId = itemId, collectionId = collection.id)
                    )
                }
            }

            // Link Suggested Tags
            val tagsJoined = StringBuilder()
            for (tagName in classification.suggestedTags) {
                val tag = tagDao.getTagByName(tagName)
                val tagId = if (tag == null) {
                    tagDao.insertTag(TagEntity(name = tagName))
                } else {
                    tag.id
                }
                if (tagId > 0) {
                    tagDao.insertItemTagCrossRef(SavedItemTagCrossRef(savedItemId = itemId, tagId = tagId))
                }
                tagsJoined.append(tagName).append(" ")
            }

            // 5. Index into FTS
            val entitiesJoined = extractedEntities.joinToString(" ") { it.value }
            val ftsEntity = SavedItemFtsEntity(
                rowid = itemId,
                title = finalTitle,
                description = finalDescription.orEmpty(),
                originalUrl = itemEntity.originalUrl.orEmpty(),
                sourceDomain = finalDomain.orEmpty(),
                originalText = itemEntity.originalText.orEmpty(),
                extractedText = finalExtractedText.orEmpty(),
                ocrText = finalOcrText.orEmpty(),
                tags = tagsJoined.toString().trim(),
                entities = entitiesJoined
            )
            savedItemFtsDao.insertOrUpdate(ftsEntity)

            // 6. Update SavedItemEntity to READY with all enriched fields
            savedItemDao.updateProcessingResult(
                id = itemId,
                status = ProcessingStatus.READY,
                ocrText = finalOcrText,
                extractedText = finalExtractedText,
                title = finalTitle,
                description = finalDescription,
                thumbnailPath = finalThumbnailPath,
                sourceDomain = finalDomain,
                canonicalUrl = finalCanonicalUrl,
                contentType = finalContentType,
                commentsJson = finalCommentsJson
            )

            Result.success()
        } catch (e: Exception) {
            // Keep original item intact even if enrichment fails
            savedItemDao.updateStatus(itemId, ProcessingStatus.FAILED)
            Result.success()
        }
    }
}
