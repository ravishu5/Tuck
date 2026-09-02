package com.tuck.app.ui.home

import com.tuck.app.R
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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
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
    private val voiceNoteRecorder: com.tuck.app.processing.VoiceNoteRecorder,
    private val screenshotImporter: ScreenshotImporter,
    private val fileStorageService: FileStorageService,
    private val urlMetadataProcessor: UrlMetadataProcessor,
    private val relatedItemsEngine: RelatedItemsEngine,
    private val settingsRepository: com.tuck.app.domain.repository.SettingsRepository
) : ViewModel() {

    val appSettings = settingsRepository.getSettings().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = com.tuck.app.domain.repository.AppSettings()
    )

    fun setTheme(theme: com.tuck.app.domain.repository.AppTheme) {
        viewModelScope.launch {
            settingsRepository.updateTheme(theme)
        }
    }

    fun setThemeFlavor(flavor: com.tuck.app.domain.repository.TuckThemeFlavor) {
        viewModelScope.launch {
            settingsRepository.updateThemeFlavor(flavor)
        }
    }

    fun toggleTheme() {
        viewModelScope.launch {
            val current = settingsRepository.getSettings().first()
            val nextTheme = when (current.theme) {
                com.tuck.app.domain.repository.AppTheme.LIGHT -> com.tuck.app.domain.repository.AppTheme.DARK
                com.tuck.app.domain.repository.AppTheme.DARK -> com.tuck.app.domain.repository.AppTheme.LIGHT
                com.tuck.app.domain.repository.AppTheme.SYSTEM -> com.tuck.app.domain.repository.AppTheme.DARK
            }
            settingsRepository.updateTheme(nextTheme)
        }
    }

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

            // Heal any previously saved URLs missing thumbnails
            try {
                val active = savedItemRepository.getAllActiveItems().first()
                for (item in active) {
                    if ((item.contentType == ContentType.URL || item.contentType == ContentType.VIDEO) && item.thumbnailPath.isNullOrBlank() && !item.originalUrl.isNullOrBlank()) {
                        val workRequest = OneTimeWorkRequestBuilder<ItemProcessingWorker>()
                            .setInputData(workDataOf(ItemProcessingWorker.KEY_ITEM_ID to item.id))
                            .build()
                        WorkManager.getInstance(context).enqueue(workRequest)
                    }
                }
            } catch (_: Exception) {}
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
            val domain = (item.sourceDomain ?: "").lowercase()
            val url = (item.originalUrl ?: "").lowercase()
            val text = (item.originalText ?: item.extractedText ?: "").lowercase()
            val app = (item.sourceApp ?: "").lowercase()
            val collectionNames = item.collections.map { it.name.lowercase() }

            val matchesSource = when (filter.source) {
                null -> true
                "Screenshots" -> item.contentType == ContentType.IMAGE || domain.contains("screenshot") || app.contains("screenshot")
                "LinkedIn" -> domain.contains("linkedin") || domain.contains("lnkd.in") || url.contains("linkedin.com") || url.contains("lnkd.in") || app.contains("linkedin") || collectionNames.contains("linkedin")
                "Instagram" -> domain.contains("instagram") || domain.contains("instagr.am") || domain.contains("ig.me") || url.contains("instagram.com") || url.contains("instagr.am") || url.contains("ig.me") || app.contains("instagram") || collectionNames.contains("instagram")
                "Reddit" -> domain.contains("reddit") || domain.contains("redd.it") || domain.startsWith("r/") || url.contains("reddit.com") || url.contains("redd.it") || app.contains("reddit") || collectionNames.contains("reddit")
                "YouTube" -> domain.contains("youtube") || domain.contains("youtu.be") || url.contains("youtube.com") || url.contains("youtu.be") || app.contains("youtube") || collectionNames.contains("youtube")
                "Twitter" -> !domain.contains("reddit") && !url.contains("reddit.com") && !app.contains("reddit") && (domain.contains("twitter") || domain == "x.com" || domain == "t.co" || url.contains("twitter.com") || url.contains("x.com/") || url.contains("t.co/") || app.contains("twitter") || collectionNames.contains("twitter") || collectionNames.contains("twitter / x"))
                "GitHub" -> domain.contains("github") || url.contains("github.com") || app.contains("github") || collectionNames.contains("github")
                "Web" -> item.contentType == ContentType.URL && !domain.contains("instagram") && !domain.contains("reddit") && !domain.contains("linkedin") && !domain.contains("youtube") && !domain.contains("twitter") && !domain.contains("x.com") && !url.contains("reddit.com") && !url.contains("instagram.com") && !url.contains("linkedin.com") && !url.contains("twitter.com") && !url.contains("youtube.com")
                "PDFs" -> item.contentType == ContentType.PDF || collectionNames.contains("pdfs")
                "Notes" -> item.contentType == ContentType.TEXT
                "Articles" -> collectionNames.contains("articles")
                "Education" -> collectionNames.contains("education")
                "Finance" -> collectionNames.contains("finance")
                "Programming" -> collectionNames.contains("programming") || collectionNames.contains("code")
                "Research" -> collectionNames.contains("research")
                "Shopping" -> collectionNames.contains("shopping")
                "Travel" -> collectionNames.contains("travel")
                "Food & Dining" -> collectionNames.contains("food & dining") || collectionNames.contains("food")
                "Work" -> collectionNames.contains("work")
                "Personal" -> collectionNames.contains("personal")
                "Videos" -> item.contentType == ContentType.VIDEO || collectionNames.contains("videos")
                "Images" -> item.contentType == ContentType.IMAGE || collectionNames.contains("images")
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
                val titlePreview = trimmed.lines().firstOrNull { it.isNotBlank() }?.take(50) ?: context.getString(R.string.title_quick_note)
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
                        title = context.getString(R.string.title_saved_photo),
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

    // --- Voice notes -------------------------------------------------------------

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    fun startVoiceNote(): Boolean {
        val started = voiceNoteRecorder.start()
        _isRecording.value = started
        return started
    }

    fun cancelVoiceNote() {
        voiceNoteRecorder.cancel()
        _isRecording.value = false
    }

    /** Stops recording and saves it. Returns false when nothing usable was captured. */
    fun stopAndSaveVoiceNote(onResult: (Boolean) -> Unit) {
        val note = voiceNoteRecorder.stop()
        _isRecording.value = false
        if (note == null) {
            onResult(false)
            return
        }
        viewModelScope.launch {
            val seconds = (note.durationMs / 1000).coerceAtLeast(1)
            val item = SavedItem(
                contentType = ContentType.AUDIO,
                title = context.getString(R.string.title_voice_note, seconds),
                localFilePath = note.file.absolutePath,
                mimeType = "audio/mp4",
                sourceApp = "Tuck"
            )
            val id = savedItemRepository.insertItem(item)
            if (id > 0) {
                WorkManager.getInstance(context).enqueue(
                    OneTimeWorkRequestBuilder<ItemProcessingWorker>()
                        .setInputData(workDataOf(ItemProcessingWorker.KEY_ITEM_ID to id))
                        .build()
                )
            }
            onResult(id > 0)
        }
    }

    fun importDocumentFromUri(uri: Uri, titleOverride: String? = null) {
        viewModelScope.launch {
            try {
                val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
                val isPdf = mimeType.contains("pdf", ignoreCase = true) || uri.toString().endsWith(".pdf", ignoreCase = true)
                val saveResult = fileStorageService.saveStreamUri(uri, mimeType)
                val item = SavedItem(
                    contentType = if (isPdf) ContentType.PDF else ContentType.DOCUMENT,
                    title = titleOverride ?: context.getString(R.string.title_imported_document),
                    localFilePath = saveResult.localFilePath,
                    thumbnailPath = saveResult.thumbnailPath,
                    mimeType = mimeType,
                    sourceDomain = if (isPdf) "PDF" else "Document",
                    sourceApp = if (titleOverride != null) "Tuck" else null
                )
                val id = savedItemRepository.insertItem(item)
                if (id > 0) {
                    val workRequest = OneTimeWorkRequestBuilder<ItemProcessingWorker>()
                        .setInputData(workDataOf(ItemProcessingWorker.KEY_ITEM_ID to id))
                        .build()
                    WorkManager.getInstance(context).enqueue(workRequest)
                }
            } catch (e: Exception) {
                // Ignore
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

    /** Files an item into a collection from the quick-actions sheet. */
    fun addToCollection(itemId: Long, collectionId: Long) {
        viewModelScope.launch {
            savedItemRepository.addItemToCollection(itemId, collectionId)
        }
    }

    fun toggleCompleted(itemId: Long, isCurrentlyDone: Boolean) {
        viewModelScope.launch {
            savedItemRepository.setCompleted(itemId, !isCurrentlyDone)
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
