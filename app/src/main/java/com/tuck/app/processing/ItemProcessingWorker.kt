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
import com.tuck.app.processing.capture.CaptureEngine
import com.tuck.app.processing.extractors.SourceContentFetcher
import com.tuck.app.processing.extractors.isThin
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
    private val captureEngine: CaptureEngine,
    private val audioTranscriber: AudioTranscriber,
    private val imageOcrProcessor: ImageOcrProcessor,
    private val pdfProcessor: PdfProcessor,
    private val entityExtractor: EntityExtractor,
    private val sourcePersonExtractor: SourcePersonExtractor,
    private val networkPolicy: NetworkPolicy,
    private val classifier: RuleBasedContentClassifier,
    private val filingRuleEngine: FilingRuleEngine,
    private val settingsRepository: SettingsRepository
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val KEY_ITEM_ID = "key_item_id"

        private val PLACEHOLDER_TITLES = setOf(
            "Shared Link",
            "Instagram Reel",
            "Reddit Post",
            "LinkedIn Post"
        )
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
            val deepCaptureEnabled = settings?.deepCaptureEnabled ?: false
            val transcribeVoiceNotes = settings?.transcribeVoiceNotes ?: true

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
                        val cleanedUrl = urlMetadataProcessor.cleanUrl(url)
                        finalDomain = urlMetadataProcessor.extractDomain(cleanedUrl)
                            .ifBlank { finalDomain.orEmpty() }

                        // One fetch, one parser, one place platform knowledge lives.
                        val extractor = sourceExtractorRegistry.getExtractor(cleanedUrl)
                        val payload = sourceContentFetcher.fetch(cleanedUrl, extractor)
                        var extracted = try {
                            extractor.extract(cleanedUrl, payload)
                        } catch (e: Exception) {
                            null
                        }

                        // Tier 2: some platforms answer a plain fetch with a script shell, so the
                        // only way to read them is to render them. Expensive enough that it runs
                        // only when the cheap path came back empty-handed and the reader asked
                        // for it — see CAPTURE_ARCHITECTURE.md §3.
                        if (extractor.requiresRenderedHtml && deepCaptureEnabled && extracted.isThin()) {
                            val rendered = captureEngine.capture(
                                url = extractor.fetchUrl(cleanedUrl),
                                readySelector = extractor.readySelector
                            )
                            if (rendered != null) {
                                val fromRendered = try {
                                    extractor.extract(cleanedUrl, rendered)
                                } catch (e: Exception) {
                                    null
                                }
                                // Only take the render if it actually beat the cheap path.
                                if (fromRendered != null && !fromRendered.isThin()) {
                                    extracted = fromRendered
                                }
                            }
                        }

                        if (extracted != null) {
                            if (!extracted.title.isNullOrBlank() &&
                                (!extracted.bodyText.isNullOrBlank() || isPlaceholderTitle(finalTitle, finalDomain))
                            ) {
                                finalTitle = extracted.title
                            }
                            if (!extracted.description.isNullOrBlank()) {
                                finalDescription = extracted.description
                            }
                            if (!extracted.bodyText.isNullOrBlank()) {
                                finalExtractedText = extracted.bodyText
                            }
                            finalCanonicalUrl = extracted.canonicalUrl ?: cleanedUrl
                            extracted.contentType?.let { finalContentType = it }
                            if (!extracted.community.isNullOrBlank()) {
                                finalDomain = extracted.community
                            }

                            if (finalThumbnailPath.isNullOrBlank() && !extracted.leadImageUrl.isNullOrBlank()) {
                                val savedThumb = try {
                                    fileStorageService.downloadAndCacheImage(extracted.leadImageUrl)
                                } catch (e: Exception) {
                                    null
                                }
                                // Keeping the remote URL when the download fails means the card
                                // still shows something; Coil will try again at render time.
                                finalThumbnailPath = savedThumb ?: extracted.leadImageUrl
                                if (savedThumb != null) {
                                    mediaAssetDao.insert(
                                        MediaAssetEntity(
                                            itemId = itemId,
                                            role = "THUMBNAIL",
                                            localPath = savedThumb,
                                            thumbnailPath = savedThumb,
                                            mimeType = "image/jpeg",
                                            downloadState = "COMPLETE"
                                        )
                                    )
                                }
                            }

                            // A direct media file is the only way the app can actually play
                            // something: platform embeds refuse to be driven, and their signed
                            // CDN links expire within days. Only some extractors can supply one
                            // — X's syndication payload does, YouTube's never will.
                            val playableUrl = extracted.mediaUrls.firstOrNull { url ->
                                url.substringBefore('?').endsWith(".mp4", ignoreCase = true)
                            }
                            if (playableUrl != null && itemEntity.localFilePath.isNullOrBlank()) {
                                val savedVideo = try {
                                    fileStorageService.downloadAndCacheVideo(playableUrl)
                                } catch (e: Exception) {
                                    null
                                }
                                if (savedVideo != null) {
                                    savedItemDao.setLocalFilePath(itemId, savedVideo)
                                    mediaAssetDao.insert(
                                        MediaAssetEntity(
                                            itemId = itemId,
                                            role = "PRIMARY",
                                            localPath = savedVideo,
                                            thumbnailPath = finalThumbnailPath,
                                            mimeType = "video/mp4",
                                            downloadState = "COMPLETE"
                                        )
                                    )
                                    finalContentType = ContentType.VIDEO
                                }
                            }

                            if (saveCommentsEnabled || extracted.comments.isEmpty()) {
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
                                        permalink = finalCanonicalUrl ?: cleanedUrl,
                                        rawJson = extracted.rawJson,
                                        extractorVersion = extractor.platformName,
                                        fetchedAt = System.currentTimeMillis()
                                    )
                                )

                                val treeComments = if (saveCommentsEnabled) extracted.comments else emptyList()
                                if (treeComments.isNotEmpty()) {
                                    sourceContentDao.insertComments(flattenComments(treeComments, itemId))
                                    finalCommentsJson = legacyCommentsJson(treeComments)
                                }

                                extractedEntities.addAll(
                                    sourcePersonExtractor.extractEntities(
                                        savedItemId = itemId,
                                        platform = extracted.platform,
                                        postAuthor = extracted.authorHandle,
                                        postAuthorDisplay = extracted.authorDisplay,
                                        comments = treeComments
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

                ContentType.AUDIO -> {
                    // A voice note is the one capture with no text at all, so without this it is
                    // invisible to search — the app's only retrieval path since AI was cut.
                    itemEntity.localFilePath?.let { audioPath ->
                        if (transcribeVoiceNotes) {
                            val transcript = audioTranscriber.transcribe(
                                file = java.io.File(audioPath),
                                languageTag = java.util.Locale.getDefault().toLanguageTag()
                            )
                            if (!transcript.isNullOrBlank()) {
                                finalExtractedText = transcript
                                // The opening words make a better name than "Voice note", the
                                // same way a note's first line does.
                                if (isPlaceholderTitle(finalTitle, finalDomain)) {
                                    finalTitle = transcript.take(70).trim()
                                }
                                entityExtractor.extractEntities(transcript, itemId).forEach {
                                    extractedEntities.add(
                                        EntityEntity(
                                            savedItemId = itemId,
                                            type = it.type,
                                            value = it.value,
                                            normalizedValue = it.normalizedValue,
                                            producer = "speech-to-text"
                                        )
                                    )
                                }
                            }
                        }

                        mediaAssetDao.insert(
                            MediaAssetEntity(
                                itemId = itemId,
                                role = "AUDIO",
                                localPath = audioPath,
                                mimeType = itemEntity.mimeType ?: "audio/mp4",
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

            // 5. User-defined auto-filing rules.
            // Deliberately after classification so a rule can match on tags and domain,
            // and additive to whatever the classifier already chose.
            try {
                filingRuleEngine.apply(
                    domainItem.copy(
                        tags = classification.suggestedTags.map { com.tuck.app.domain.model.Tag(name = it) }
                    )
                )
            } catch (e: Exception) {
                // Filing is a convenience; never let it fail an otherwise-good save.
            }

            // 6. Index into FTS
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

            // 7. Update SavedItemEntity to READY with all enriched fields
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

    /**
     * Whether a title is still the stand-in ShareParser assigned and may be overwritten by
     * something a fetch discovered. A title the user typed is never a placeholder.
     */
    private fun isPlaceholderTitle(title: String, domain: String?): Boolean =
        title.isBlank() ||
            title == domain ||
            title in PLACEHOLDER_TITLES
}
