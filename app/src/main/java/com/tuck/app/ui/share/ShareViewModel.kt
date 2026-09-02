package com.tuck.app.ui.share

import com.tuck.app.R
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.tuck.app.data.local.storage.FileStorageService
import com.tuck.app.di.IoDispatcher
import com.tuck.app.domain.model.Collection
import com.tuck.app.domain.model.ContentType
import com.tuck.app.domain.model.ProcessingStatus
import com.tuck.app.domain.model.SavedItem
import com.tuck.app.domain.repository.CollectionRepository
import com.tuck.app.domain.repository.SavedItemRepository
import com.tuck.app.processing.DuplicateDetector
import com.tuck.app.processing.ItemProcessingWorker
import com.tuck.app.processing.ParsedShareContent
import com.tuck.app.processing.ShareParser
import com.tuck.app.processing.UrlMetadataProcessor
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed interface ShareUiState {
    data object Idle : ShareUiState
    data object Saving : ShareUiState
    data class Saved(
        val savedItemId: Long,
        val title: String,
        val subtitle: String,
        val collections: List<Collection> = emptyList(),
        val selectedCollectionIds: Set<Long> = emptySet(),
        val isCustomCategoryDialogOpen: Boolean = false
    ) : ShareUiState
    data class Error(val message: String) : ShareUiState
}

@HiltViewModel
class ShareViewModel @Inject constructor(
    @ApplicationContext private val context: android.content.Context,
    private val shareParser: ShareParser,
    private val savedItemRepository: SavedItemRepository,
    private val collectionRepository: CollectionRepository,
    private val fileStorageService: FileStorageService,
    private val duplicateDetector: DuplicateDetector,
    private val urlMetadataProcessor: UrlMetadataProcessor,
    private val workManager: WorkManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow<ShareUiState>(ShareUiState.Idle)
    val uiState: StateFlow<ShareUiState> = _uiState.asStateFlow()

    fun handleIncomingIntent(intent: Intent, callerPackage: String?) {
        viewModelScope.launch {
            _uiState.value = ShareUiState.Saving

            try {
                val parsed = shareParser.parseIntent(intent, callerPackage)
                if (parsed == null) {
                    _uiState.value = ShareUiState.Error(context.getString(R.string.share_unable_to_handle))
                    return@launch
                }

                val (savedItemId, allCollections) = withContext(ioDispatcher) {
                    collectionRepository.ensureDefaultCollections()
                    val id = saveParsedContent(parsed)

                    val workRequest = OneTimeWorkRequestBuilder<ItemProcessingWorker>()
                        .setInputData(workDataOf(ItemProcessingWorker.KEY_ITEM_ID to id))
                        .setBackoffCriteria(
                            androidx.work.BackoffPolicy.EXPONENTIAL,
                            10,
                            java.util.concurrent.TimeUnit.SECONDS
                        )
                        .build()
                    workManager.enqueue(workRequest)

                    val collections = collectionRepository.getAllCollections().first()
                    Pair(id, collections)
                }

                val subtitle = buildSubtitle(parsed)

                _uiState.value = ShareUiState.Saved(
                    savedItemId = savedItemId,
                    title = parsed.title,
                    subtitle = subtitle,
                    collections = allCollections,
                    selectedCollectionIds = emptySet()
                )
            } catch (e: Exception) {
                _uiState.value = ShareUiState.Error(
                context.getString(
                    R.string.share_failed,
                    e.localizedMessage ?: context.getString(R.string.share_unknown_error)
                )
            )
            }
        }
    }

    fun toggleCollection(collectionId: Long) {
        val current = _uiState.value as? ShareUiState.Saved ?: return
        viewModelScope.launch {
            val newSelected = current.selectedCollectionIds.toMutableSet()
            if (newSelected.contains(collectionId)) {
                newSelected.remove(collectionId)
                savedItemRepository.removeItemFromCollection(current.savedItemId, collectionId)
            } else {
                newSelected.add(collectionId)
                savedItemRepository.addItemToCollection(current.savedItemId, collectionId)
            }
            _uiState.value = current.copy(selectedCollectionIds = newSelected)
        }
    }

    fun createAndAddCollection(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        val current = _uiState.value as? ShareUiState.Saved ?: return

        viewModelScope.launch {
            val colId = collectionRepository.createCollection(name = trimmed, isAutoGenerated = false)
            if (colId > 0) {
                savedItemRepository.addItemToCollection(current.savedItemId, colId)
                val updatedCollections = collectionRepository.getAllCollections().first()
                _uiState.value = current.copy(
                    collections = updatedCollections,
                    selectedCollectionIds = current.selectedCollectionIds + colId,
                    isCustomCategoryDialogOpen = false
                )
            }
        }
    }

    fun setCustomCategoryDialogOpen(isOpen: Boolean) {
        val current = _uiState.value as? ShareUiState.Saved ?: return
        _uiState.value = current.copy(isCustomCategoryDialogOpen = isOpen)
    }

    private suspend fun saveParsedContent(parsed: ParsedShareContent): Long {
        var localFilePath: String? = null
        var thumbnailPath: String? = null
        var textHash: String? = null
        var imageSha256: String? = null
        var canonicalUrl: String? = null
        var sourceDomain: String? = null
        var capturedAt: Long? = null

        // 1. Process files / images / PDFs / contacts / streams if present (copy all stream bytes immediately)
        if (parsed.streamUris.isNotEmpty()) {
            val results = fileStorageService.saveAllStreamsFromUris(parsed.streamUris, parsed.mimeType)
            if (results.isNotEmpty()) {
                val primaryResult = results.first()
                localFilePath = primaryResult.localFilePath
                thumbnailPath = primaryResult.thumbnailPath
                imageSha256 = primaryResult.sha256
                capturedAt = primaryResult.capturedAt
            }
        }

        // 2. URL canonicalization
        if (parsed.url != null) {
            canonicalUrl = duplicateDetector.getCanonicalUrl(parsed.url)
            sourceDomain = urlMetadataProcessor.extractDomain(parsed.url)
        }

        // 3. Text hashing
        if (parsed.text != null) {
            textHash = duplicateDetector.hashText(parsed.text)
        }

        val item = SavedItem(
            capturedAt = capturedAt,
            contentType = parsed.contentType,
            title = parsed.title,
            description = parsed.extraMetadata["query"] ?: parsed.extraMetadata["org"],
            originalUrl = parsed.url,
            canonicalUrl = canonicalUrl,
            sourceDomain = sourceDomain ?: if (parsed.contentType == ContentType.LOCATION) "Maps" else null,
            sourceApp = parsed.sourceApp,
            mimeType = parsed.mimeType,
            localFilePath = localFilePath,
            thumbnailPath = thumbnailPath,
            originalText = parsed.text,
            extractedText = null,
            ocrText = null,
            processingStatus = ProcessingStatus.PENDING
        )

        return savedItemRepository.insertItem(item)
    }

    private fun buildSubtitle(parsed: ParsedShareContent): String {
        val parts = mutableListOf<String>()
        if (!parsed.sourceApp.isNullOrBlank()) {
            parts.add(parsed.sourceApp.substringAfterLast("."))
        }
        parts.add(parsed.contentType.displayName)
        return parts.joinToString(" · ")
    }
}
