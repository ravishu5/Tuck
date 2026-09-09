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
import com.tuck.app.domain.model.Collection
import com.tuck.app.domain.model.ContentType
import com.tuck.app.domain.model.SavedItem
import com.tuck.app.domain.repository.CollectionRepository
import com.tuck.app.domain.repository.SavedItemRepository
import com.tuck.app.processing.GalleryScreenshot
import com.tuck.app.domain.memory.rediscoverPicks
import com.tuck.app.processing.ItemProcessingWorker
import com.tuck.app.processing.QuickRecordController
import com.tuck.app.processing.ScreenshotImporter
import com.tuck.app.processing.UrlMetadataProcessor
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
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
    private val quickRecordController: com.tuck.app.processing.QuickRecordController,
    private val screenshotImporter: ScreenshotImporter,
    private val fileStorageService: FileStorageService,
    private val urlMetadataProcessor: UrlMetadataProcessor,
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

    fun setFeedViewMode(mode: com.tuck.app.domain.repository.FeedViewMode) {
        viewModelScope.launch {
            settingsRepository.setFeedViewMode(mode)
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

    /**
     * Fixed for the life of the screen, so the rediscover row keeps the same four saves while
     * the reader is looking at it instead of re-dealing whenever anything in the vault changes.
     */
    private val rediscoverSeed = System.currentTimeMillis()

    private val baseDataFlow = combine(
        savedItemRepository.getAllActiveItems(),
        collectionRepository.getAllCollections()
    ) { allItems, collections ->
        // Derived from the items already in hand. Collecting the engine's own flow ran the same
        // query and the same entity mapping a second time on every change.
        BaseHomeData(
            allItems = allItems,
            collections = collections,
            rediscoverItems = allItems.rediscoverPicks(limit = 4, seed = rediscoverSeed)
        )
    }

    init {
        // Housekeeping, not the feed. None of it is needed to draw the home screen, and all of
        // it used to run on the main thread while the first frame was trying to get out: a
        // MediaStore scan for screenshots, and a WorkManager enqueue per item missing a
        // thumbnail. Off the main thread, and behind the feed rather than in front of it.
        viewModelScope.launch(Dispatchers.Default) {
            collectionRepository.ensureDefaultCollections()

            healMissingThumbnails()

            // Last, because it is the slowest and the least urgent — the banner it feeds can
            // appear a moment after the vault does.
            checkGalleryScreenshots()
        }
    }

    /**
     * Re-queues processing for saves whose preview never arrived.
     *
     * Reads the vault once rather than watching it: this is a one-off repair at launch, and
     * collecting the live query would re-run the whole sweep on every later change.
     */
    private suspend fun healMissingThumbnails() {
        try {
            val active = savedItemRepository.getAllActiveItems().first()
            val needsThumbnail = active.filter { item ->
                (item.contentType == ContentType.URL || item.contentType == ContentType.VIDEO) &&
                    item.thumbnailPath.isNullOrBlank() &&
                    !item.originalUrl.isNullOrBlank()
            }
            if (needsThumbnail.isEmpty()) return

            // One enqueue call rather than one per item: each is a binder round trip and a
            // write, and a vault with a hundred unprocessed links paid for all of them.
            WorkManager.getInstance(context).enqueue(
                needsThumbnail.map { item ->
                    OneTimeWorkRequestBuilder<ItemProcessingWorker>()
                        .setInputData(workDataOf(ItemProcessingWorker.KEY_ITEM_ID to item.id))
                        .build()
                }
            )
        } catch (_: Exception) {
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
    }
        // The filter above walks every saved item and lowercases several of its fields, and a
        // `combine` runs its transform on whoever collects it — which for `stateIn(viewModelScope)`
        // is the main thread. So every database change re-filtered the whole vault between
        // frames, which is what made opening the app stutter and the feed appear to redraw.
        // `flowOn` moves the combine and everything upstream of it onto a worker.
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HomeUiState(isLoading = true)
        )

    fun checkGalleryScreenshots() {
        viewModelScope.launch(Dispatchers.Default) {
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

    /**
     * The recorder's own state, not a copy of it.
     *
     * Home is one of several ways into the same microphone — the Quick Settings tile and the
     * launcher shortcut are others — so it reads the shared controller rather than tracking a
     * flag of its own, and a memo started from the tile shows as recording here too.
     */
    val isRecording: StateFlow<Boolean> = quickRecordController.state
        .map { it is QuickRecordController.State.Recording }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /**
     * Whether the recording currently running was started from this screen.
     *
     * Home tears down its recording when it goes away, which is right for one it started and
     * badly wrong for one it did not: a memo begun from the Quick Settings tile is meant to
     * outlive every screen, and would otherwise be cancelled the moment the reader switched
     * tabs with the app still open.
     */
    private var startedFromHome = false

    fun startVoiceNote(): Boolean {
        if (quickRecordController.isRecording) return true
        startedFromHome = true
        viewModelScope.launch { quickRecordController.start() }
        return true
    }

    fun cancelVoiceNote() {
        startedFromHome = false
        viewModelScope.launch { quickRecordController.cancel() }
    }

    /** Cancels only a recording this screen owns. See [startedFromHome]. */
    fun cancelVoiceNoteIfOwned() {
        if (startedFromHome) cancelVoiceNote()
    }

    /** Stops recording and saves it. Returns false when nothing usable was captured. */
    fun stopAndSaveVoiceNote(onResult: (Boolean) -> Unit) {
        startedFromHome = false
        viewModelScope.launch {
            val result = quickRecordController.stopAndSave()
            onResult(result is QuickRecordController.SaveResult.Saved)
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
