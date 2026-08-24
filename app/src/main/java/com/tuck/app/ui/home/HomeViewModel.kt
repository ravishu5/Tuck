package com.tuck.app.ui.home

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.tuck.app.data.local.storage.FileStorageService
import com.tuck.app.domain.memory.RelatedItemsEngine
import com.tuck.app.domain.model.Collection
import com.tuck.app.domain.model.ContentType
import com.tuck.app.domain.model.SavedItem
import com.tuck.app.domain.repository.CollectionRepository
import com.tuck.app.domain.repository.SavedItemRepository
import com.tuck.app.processing.GalleryScreenshot
import com.tuck.app.processing.ItemProcessingWorker
import com.tuck.app.processing.ScreenshotImporter
import com.tuck.app.processing.UrlMetadataProcessor
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val items: List<SavedItem> = emptyList(),
    val collections: List<Collection> = emptyList(),
    val rediscoverItems: List<SavedItem> = emptyList(),
    val selectedCategory: String? = null,
    val selectedType: ContentType? = null,
    val selectedSource: String? = null,
    val unimportedScreenshotsCount: Int = 0,
    val isImporting: Boolean = false,
    val isLoading: Boolean = false
)

private data class FilterState(
    val category: String? = null,
    val type: ContentType? = null,
    val source: String? = null
)

private data class BaseHomeData(
    val allItems: List<SavedItem>,
    val collections: List<Collection>,
    val rediscoverItems: List<SavedItem>
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val savedItemRepository: SavedItemRepository,
    private val collectionRepository: CollectionRepository,
    private val screenshotImporter: ScreenshotImporter,
    private val fileStorageService: FileStorageService,
    private val urlMetadataProcessor: UrlMetadataProcessor,
    private val relatedItemsEngine: RelatedItemsEngine
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow<String?>(null)
    private val _selectedType = MutableStateFlow<ContentType?>(null)
    private val _selectedSource = MutableStateFlow<String?>(null)
    private val _unimportedScreenshots = MutableStateFlow<List<GalleryScreenshot>>(emptyList())
    private val _isImporting = MutableStateFlow(false)

    private val filterState = combine(
        _selectedCategory,
        _selectedType,
        _selectedSource
    ) { category, type, source ->
        FilterState(category = category, type = type, source = source)
    }

    private val baseDataFlow = combine(
        savedItemRepository.getAllActiveItems(),
        collectionRepository.getAllCollections(),
        relatedItemsEngine.getRediscoverItems(4)
    ) { allItems, collections, rediscover ->
        BaseHomeData(allItems = allItems, collections = collections, rediscoverItems = rediscover)
    }

    init {
        viewModelScope.launch {
            collectionRepository.ensureDefaultCollections()
            checkGalleryScreenshots()
        }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        baseDataFlow,
        filterState,
        _unimportedScreenshots,
        _isImporting
    ) { baseData, filter, unimported, isImporting ->
        val filtered = baseData.allItems.filter { item ->
            val matchesType = filter.type == null || item.contentType == filter.type
            val matchesCat = filter.category == null || item.collections.any { it.name.equals(filter.category, ignoreCase = true) }
            val domain = item.sourceDomain ?: ""
            val matchesSource = when (filter.source) {
                null -> true
                "Screenshots" -> item.contentType == ContentType.IMAGE || domain.contains("Screenshot", ignoreCase = true)
                "LinkedIn" -> domain.contains("LinkedIn", ignoreCase = true) || item.originalUrl?.contains("linkedin.com") == true
                "Instagram" -> domain.contains("Instagram", ignoreCase = true) || item.originalUrl?.contains("instagram.com") == true
                "Reddit" -> domain.contains("Reddit", ignoreCase = true) || item.originalUrl?.contains("reddit.com") == true || domain.startsWith("r/")
                "YouTube" -> domain.contains("YouTube", ignoreCase = true) || item.originalUrl?.contains("youtube") == true || item.originalUrl?.contains("youtu.be") == true
                "Twitter" -> domain.contains("Twitter", ignoreCase = true) || domain.contains("X.com", ignoreCase = true) || item.originalUrl?.contains("twitter.com") == true || item.originalUrl?.contains("x.com") == true
                "Web" -> item.contentType == ContentType.URL && !domain.contains("Instagram", ignoreCase = true) && !domain.contains("Reddit", ignoreCase = true) && !domain.contains("LinkedIn", ignoreCase = true)
                "PDFs" -> item.contentType == ContentType.PDF
                "Notes" -> item.contentType == ContentType.TEXT
                else -> true
            }
            matchesType && matchesCat && matchesSource
        }
        HomeUiState(
            items = filtered,
            collections = baseData.collections,
            rediscoverItems = baseData.rediscoverItems,
            selectedCategory = filter.category,
            selectedType = filter.type,
            selectedSource = filter.source,
            unimportedScreenshotsCount = unimported.size,
            isImporting = isImporting,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(isLoading = true)
    )

    fun checkGalleryScreenshots() {
        viewModelScope.launch {
            try {
                val recent = screenshotImporter.getRecentScreenshots(limit = 30)
                _unimportedScreenshots.value = recent
            } catch (e: Exception) {
                _unimportedScreenshots.value = emptyList()
            }
        }
    }

    fun importAllGalleryScreenshots() {
        viewModelScope.launch {
            val toImport = _unimportedScreenshots.value
            if (toImport.isEmpty()) return@launch


            _isImporting.value = true
            try {
                val ids = screenshotImporter.importAllScreenshots(toImport)
                _unimportedScreenshots.value = emptyList()

                // Enqueue background OCR indexing worker for each imported screenshot
                val workManager = WorkManager.getInstance(context)
                for (id in ids) {
                    val workRequest = OneTimeWorkRequestBuilder<ItemProcessingWorker>()
                        .setInputData(workDataOf(ItemProcessingWorker.KEY_ITEM_ID to id))
                        .build()
                    workManager.enqueue(workRequest)
                }
            } catch (e: Exception) {
                // Ignore
            } finally {
                _isImporting.value = false
            }
        }
    }

    fun dismissScreenshotBanner() {
        _unimportedScreenshots.value = emptyList()
    }

    fun quickAdd(content: String, isUrl: Boolean) {
        viewModelScope.launch {
            val trimmed = content.trim()
            if (trimmed.isBlank()) return@launch

            val (contentType, title, originalUrl, text) = if (isUrl || trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                val domain = urlMetadataProcessor.extractDomain(trimmed)
                Quad(ContentType.URL, domain.ifBlank { "Shared Link" }, trimmed, null)
            } else {
                val titlePreview = trimmed.lines().firstOrNull { it.isNotBlank() }?.take(50) ?: "Quick Note"
                Quad(ContentType.TEXT, titlePreview, null, trimmed)
            }

            val item = SavedItem(
                contentType = contentType,
                title = title,
                originalUrl = originalUrl,
                originalText = text,
                sourceDomain = if (contentType == ContentType.URL) urlMetadataProcessor.extractDomain(originalUrl.orEmpty()) else "Quick Note"
            )

            val id = savedItemRepository.insertItem(item)
            if (id > 0) {
                val workRequest = OneTimeWorkRequestBuilder<ItemProcessingWorker>()
                    .setInputData(workDataOf(ItemProcessingWorker.KEY_ITEM_ID to id))
                    .build()
                WorkManager.getInstance(context).enqueue(workRequest)
            }
        }
    }

    fun importImagesFromUris(uris: List<Uri>) {
        viewModelScope.launch {
            _isImporting.value = true
            try {
                val workManager = WorkManager.getInstance(context)
                for (uri in uris) {
                    val saveResult = fileStorageService.saveImageFromUri(uri)
                    val item = SavedItem(
                        contentType = ContentType.IMAGE,
                        title = "Saved Photo",
                        localFilePath = saveResult.localFilePath,
                        thumbnailPath = saveResult.thumbnailPath,
                        mimeType = "image/jpeg",
                        sourceDomain = "Gallery"
                    )
                    val id = savedItemRepository.insertItem(item)
                    if (id > 0) {
                        val workRequest = OneTimeWorkRequestBuilder<ItemProcessingWorker>()
                            .setInputData(workDataOf(ItemProcessingWorker.KEY_ITEM_ID to id))
                            .build()
                        workManager.enqueue(workRequest)
                    }
                }
            } catch (e: Exception) {
                // Ignore
            } finally {
                _isImporting.value = false
            }
        }
    }

    fun selectSource(source: String?) {
        _selectedSource.value = if (_selectedSource.value == source) null else source
        _selectedCategory.value = null
        _selectedType.value = null
    }

    fun selectCategory(category: String?) {
        _selectedCategory.value = if (_selectedCategory.value == category) null else category
        _selectedType.value = null
        _selectedSource.value = null
    }

    fun selectContentType(type: ContentType?) {
        _selectedType.value = if (_selectedType.value == type) null else type
        _selectedCategory.value = null
        _selectedSource.value = null
    }

    fun toggleFavorite(itemId: Long, current: Boolean) {
        viewModelScope.launch {
            savedItemRepository.setFavorite(itemId, !current)
        }
    }

    fun moveToTrash(itemId: Long) {
        viewModelScope.launch {
            savedItemRepository.moveToTrash(itemId)
        }
    }

    fun markOpened(itemId: Long) {
        viewModelScope.launch {
            savedItemRepository.markOpened(itemId)
        }
    }

    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
