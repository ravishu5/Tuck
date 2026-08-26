package com.tuck.app.processing

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tuck.app.data.local.db.dao.CollectionDao
import com.tuck.app.data.local.db.dao.DerivedContentDao
import com.tuck.app.data.local.db.dao.EntityDao
import com.tuck.app.data.local.db.dao.MediaAssetDao
import com.tuck.app.data.local.db.dao.SavedItemDao
import com.tuck.app.data.local.db.dao.SavedItemFtsDao
import com.tuck.app.data.local.db.dao.SourceContentDao
import com.tuck.app.data.local.db.dao.TagDao
import com.tuck.app.data.local.db.entity.CollectionEntity
import com.tuck.app.data.local.db.entity.EntityEntity
import com.tuck.app.data.local.db.entity.MediaAssetEntity
import com.tuck.app.data.local.db.entity.OcrBlockEntity
import com.tuck.app.data.local.db.entity.SavedItemCollectionCrossRef
import com.tuck.app.data.local.db.entity.SavedItemFtsEntity
import com.tuck.app.data.local.db.entity.SavedItemTagCrossRef
import com.tuck.app.data.local.db.entity.SourceCommentEntity
import com.tuck.app.data.local.db.entity.SourcePostEntity
import com.tuck.app.data.local.db.entity.TagEntity
import com.tuck.app.data.local.storage.FileStorageService
import com.tuck.app.processing.extractors.SourceContentFetcher
import com.tuck.app.processing.extractors.SourceExtractorRegistry
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
    private val mediaAssetDao: MediaAssetDao,
    private val sourceContentDao: SourceContentDao,
    private val derivedContentDao: DerivedContentDao,
    private val fileStorageService: FileStorageService,
    private val urlMetadataProcessor: UrlMetadataProcessor,
    private val sourceExtractorRegistry: SourceExtractorRegistry,
    private val sourceContentFetcher: SourceContentFetcher,
    private val imageOcrProcessor: ImageOcrProcessor,
    private val pdfProcessor: PdfProcessor,
    private val entityExtractor: EntityExtractor,
    private val sourcePersonExtractor: SourcePersonExtractor,
    private val networkPolicy: NetworkPolicy,
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
            val ocrEnabled = settings?.ocrEnabled ?: true
            val autoCategorizeEnabled = settings?.autoCategorizeEnabled ?: true
            val wifiOnlyMetadata = settings?.wifiOnlyMetadata ?: false

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
                    if (!networkPolicy.allowsRemoteFetch(wifiOnlyMetadata)) {
                        // Leave the item exactly as saved and try again when unmetered.
                        savedItemDao.updateStatus(itemId, ProcessingStatus.PENDING)
                        return@withContext Result.retry()
                    }
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

                        if (!meta.ogImageUrl.isNullOrBlank() && finalThumbnailPath.isNullOrBlank()) {
                            finalThumbnailPath = try {
                                fileStorageService.downloadAndCacheImage(meta.ogImageUrl) ?: meta.ogImageUrl
                            } catch (e: Exception) {
                                meta.ogImageUrl
                            }
                        }

                        // Structured source capture: real post + nested comment tree.
                        // Falls back to the flat comments UrlMetadataProcessor already found.
                        if (saveCommentsEnabled) {
                            val extractor = sourceExtractorRegistry.getExtractor(url)
                            val rawPayload = sourceContentFetcher.fetch(url, extractor.platformName)
                            val extracted = try {
                                extractor.extract(url, rawPayload)
                            } catch (e: Exception) {
                                null
                            }

                            val treeComments = extracted?.comments.orEmpty()

                            if (extracted != null && (treeComments.isNotEmpty() || !extracted.title.isNullOrBlank())) {
                                // Re-running the worker must not duplicate rows.
                                sourceContentDao.deleteCommentsForItem(itemId)

                                sourceContentDao.insertPost(
                                    SourcePostEntity(
                                        itemId = itemId,
                                        platform = extracted.platform,
                                        community = extracted.community,
                                        authorHandle = extracted.authorHandle,
                                        authorDisplay = extracted.authorDisplay,
                                        title = extracted.title ?: finalTitle,
                                        bodyText = extracted.bodyText,
                                        score = extracted.score,
                                        commentCount = extracted.commentCount,
                                        postedAt = extracted.postedAt,
                                        permalink = url,
                                        rawJson = extracted.rawJson,
                                        extractorVersion = extractor.platformName,
                                        fetchedAt = System.currentTimeMillis()
                                    )
                                )

                                if (treeComments.isNotEmpty()) {
                                    sourceContentDao.insertComments(flattenComments(treeComments, itemId))
                                    finalCommentsJson = legacyCommentsJson(treeComments)
                                }

                                if (!extracted.bodyText.isNullOrBlank() && finalExtractedText.isNullOrBlank()) {
                                    finalExtractedText = extracted.bodyText
                                }

                                val personEntities = sourcePersonExtractor.extractEntities(
                                    savedItemId = itemId,
                                    platform = extracted.platform,
                                    postAuthor = extracted.authorHandle ?: meta.author,
                                    postAuthorDisplay = extracted.authorDisplay ?: meta.author,
                                    comments = treeComments
                                )
                                extractedEntities.addAll(personEntities)
                            } else if (meta.comments.isNotEmpty()) {
                                val commentsArray = JSONArray()
                                val sourceComments = mutableListOf<SourceCommentEntity>()
                                var commentOrdinal = 0

                                for (c in meta.comments) {
                                    val cObj = JSONObject()
                                    cObj.put("author", c.author)
                                    cObj.put("text", c.text)
                                    c.score?.let { cObj.put("score", it) }
                                    c.timestamp?.let { cObj.put("timestamp", it) }
                                    commentsArray.put(cObj)

                                    commentOrdinal++
                                    sourceComments.add(
                                        SourceCommentEntity(
                                            itemId = itemId,
                                            depth = 0,
                                            path = "%04d".format(commentOrdinal),
                                            authorHandle = c.author,
                                            bodyText = c.text,
                                            score = c.score ?: 0,
                                            postedAt = c.timestamp ?: System.currentTimeMillis(),
                                            ordinal = commentOrdinal
                                        )
                                    )
                                }
                                finalCommentsJson = commentsArray.toString()

                                val platform = when {
                                    finalDomain?.contains("reddit") == true -> "REDDIT"
                                    finalDomain?.contains("youtube") == true || finalDomain?.contains("youtu.be") == true -> "YOUTUBE"
                                    finalDomain?.contains("instagram") == true -> "INSTAGRAM"
                                    finalDomain?.contains("twitter") == true || finalDomain?.contains("x.com") == true -> "TWITTER"
                                    else -> "WEB"
                                }
                                sourceContentDao.deleteCommentsForItem(itemId)
                                sourceContentDao.insertPost(
                                    SourcePostEntity(
                                        itemId = itemId,
                                        platform = platform,
                                        title = finalTitle,
                                        rawJson = finalCommentsJson,
                                        commentCount = meta.comments.size,
                                        fetchedAt = System.currentTimeMillis()
                                    )
                                )
                                sourceContentDao.insertComments(sourceComments)

                                val personEntities = sourcePersonExtractor.extractEntities(
                                    savedItemId = itemId,
                                    platform = platform,
                                    postAuthor = meta.author,
                                    fallbackCommentAuthors = meta.comments.map { it.author }
                                )
                                extractedEntities.addAll(personEntities)
                            } else if (!meta.author.isNullOrBlank()) {
                                val platform = when {
                                    finalDomain?.contains("reddit") == true -> "REDDIT"
                                    finalDomain?.contains("youtube") == true || finalDomain?.contains("youtu.be") == true -> "YOUTUBE"
                                    finalDomain?.contains("instagram") == true -> "INSTAGRAM"
                                    finalDomain?.contains("twitter") == true || finalDomain?.contains("x.com") == true -> "TWITTER"
                                    else -> "WEB"
                                }
                                val personEntities = sourcePersonExtractor.extractEntities(
                                    savedItemId = itemId,
                                    platform = platform,
                                    postAuthor = meta.author
                                )
                                extractedEntities.addAll(personEntities)
                            }
                        } else if (!meta.author.isNullOrBlank()) {
                            val platform = when {
                                finalDomain?.contains("reddit") == true -> "REDDIT"
                                finalDomain?.contains("youtube") == true || finalDomain?.contains("youtu.be") == true -> "YOUTUBE"
                                finalDomain?.contains("instagram") == true -> "INSTAGRAM"
                                finalDomain?.contains("twitter") == true || finalDomain?.contains("x.com") == true -> "TWITTER"
                                else -> "WEB"
                            }
                            val personEntities = sourcePersonExtractor.extractEntities(
                                savedItemId = itemId,
                                platform = platform,
                                postAuthor = meta.author
                            )
                            extractedEntities.addAll(personEntities)
                        }

                        // Download and save thumbnail locally for preview cards
                        if (finalThumbnailPath.isNullOrBlank() && !meta.ogImageUrl.isNullOrBlank()) {
                            val savedThumb = fileStorageService.downloadAndSaveThumbnail(meta.ogImageUrl)
                            finalThumbnailPath = savedThumb ?: meta.ogImageUrl
                            if (savedThumb != null) {
                                mediaAssetDao.insert(
                                    MediaAssetEntity(
                                        itemId = itemId,
                                        role = "THUMBNAIL",
                                        localPath = savedThumb,
                                        thumbnailPath = savedThumb,
                                        downloadState = "COMPLETE"
                                    )
                                )
                            }
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
                                    normalizedValue = it.normalizedValue,
                                    producer = "rule-based"
                                )
                            )
                        }
                    }
                }

                ContentType.IMAGE, ContentType.MULTI_IMAGE -> {
                    itemEntity.localFilePath?.let { imagePath ->
                        val ocrResult = if (ocrEnabled) imageOcrProcessor.extractOcrBlocks(imagePath) else null
                        if (ocrResult != null && !ocrResult.fullText.isNullOrBlank()) {
                            finalOcrText = ocrResult.fullText
                            // Save OCR blocks
                            val blocks = ocrResult.blocks.map { b ->
                                OcrBlockEntity(
                                    itemId = itemId,
                                    text = b.text,
                                    confidence = b.confidence,
                                    bboxX = b.bboxX,
                                    bboxY = b.bboxY,
                                    bboxW = b.bboxW,
                                    bboxH = b.bboxH,
                                    blockIndex = b.blockIndex,
                                    producer = "mlkit-ocr"
                                )
                            }
                            derivedContentDao.insertOcrBlocks(blocks)

                            // Extract entities from OCR text
                            entityExtractor.extractEntities(ocrResult.fullText, itemId).forEach {
                                extractedEntities.add(
                                    EntityEntity(
                                        savedItemId = itemId,
                                        type = it.type,
                                        value = it.value,
                                        normalizedValue = it.normalizedValue,
                                        producer = "mlkit-ocr"
                                    )
                                )
                            }
                        }

                        // Ensure primary media asset exists
                        mediaAssetDao.insert(
                            MediaAssetEntity(
                                itemId = itemId,
                                role = "PRIMARY",
                                localPath = imagePath,
                                thumbnailPath = itemEntity.thumbnailPath,
                                mimeType = itemEntity.mimeType ?: "image/jpeg",
                                downloadState = "COMPLETE"
                            )
                        )
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
                                        normalizedValue = it.normalizedValue,
                                        producer = "pdf-processor"
                                    )
                                )
                            }
                        }

                        mediaAssetDao.insert(
                            MediaAssetEntity(
                                itemId = itemId,
                                role = "PRIMARY",
                                localPath = pdfPath,
                                thumbnailPath = itemEntity.thumbnailPath,
                                mimeType = "application/pdf",
                                downloadState = "COMPLETE"
                            )
                        )
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
                                    normalizedValue = it.normalizedValue,
                                    producer = "rule-based"
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

            // Link to Smart Collections (all matched high-confidence categories & platform boards)
            if (autoCategorizeEnabled) {
                val categoriesToLink = mutableSetOf<String>()
                if (classification.primaryCategory.isNotBlank() && classification.primaryCategory != "Other") {
                    categoriesToLink.add(classification.primaryCategory)
                }

                // Explicit mutually exclusive platform board detection
                val lowerDomain = (finalDomain ?: "").lowercase()
                val lowerUrl = (itemEntity.originalUrl ?: "").lowercase()
                val lowerApp = (itemEntity.sourceApp ?: "").lowercase()
                val platformBoard = when {
                    lowerDomain.contains("reddit") || lowerDomain.contains("redd.it") || lowerUrl.contains("reddit.com") || lowerUrl.contains("redd.it") || lowerApp.contains("reddit") -> "Reddit"
                    lowerDomain.contains("linkedin") || lowerDomain.contains("lnkd.in") || lowerUrl.contains("linkedin.com") || lowerUrl.contains("lnkd.in") || lowerApp.contains("linkedin") -> "LinkedIn"
                    lowerDomain.contains("instagram") || lowerDomain.contains("instagr.am") || lowerDomain.contains("ig.me") || lowerUrl.contains("instagram.com") || lowerApp.contains("instagram") -> "Instagram"
                    lowerDomain.contains("youtube") || lowerDomain.contains("youtu.be") || lowerUrl.contains("youtube.com") || lowerUrl.contains("youtu.be") || lowerApp.contains("youtube") -> "YouTube"
                    lowerDomain.contains("twitter") || lowerDomain == "x.com" || lowerDomain == "t.co" || lowerUrl.contains("twitter.com") || lowerUrl.contains("x.com/") || lowerUrl.contains("t.co/") || lowerApp.contains("twitter") -> "Twitter / X"
                    lowerDomain.contains("github") || lowerUrl.contains("github.com") || lowerApp.contains("github") -> "GitHub"
                    else -> null
                }
                if (platformBoard != null) {
                    categoriesToLink.add(platformBoard)
                }

                for (categoryName in categoriesToLink) {
                    val collection = collectionDao.getByName(categoryName)
                    val targetColId = collection?.id ?: collectionDao.insert(
                        CollectionEntity(
                            name = categoryName,
                            isAutoGenerated = true
                        )
                    )
                    collectionDao.insertItemCollectionCrossRef(
                        SavedItemCollectionCrossRef(savedItemId = itemId, collectionId = targetColId)
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
            // Keep original item intact even if enrichment fails (Tuck Product Law 2)
            savedItemDao.updateStatus(itemId, ProcessingStatus.FAILED)
            Result.success()
        }
    }
}
