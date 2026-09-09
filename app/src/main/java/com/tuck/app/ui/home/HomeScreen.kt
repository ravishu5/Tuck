package com.tuck.app.ui.home

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.core.content.ContextCompat
import com.tuck.app.processing.DocumentScanner
import androidx.compose.ui.platform.LocalContext
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tuck.app.R
import com.tuck.app.domain.model.SavedItem
import com.tuck.app.ui.components.CountStat
import com.tuck.app.domain.repository.FeedViewMode
import com.tuck.app.ui.components.FeedViewModeToggle
import com.tuck.app.ui.components.TuckGridCard
import com.tuck.app.ui.components.QuickCaptureSpeedDialSheet
import com.tuck.app.ui.components.SectionLabel
import com.tuck.app.ui.components.TuckCategoryChip
import com.tuck.app.ui.components.TuckContentCard
import com.tuck.app.ui.components.TuckEmptyState
import com.tuck.app.ui.components.TuckResurfacingCard
import com.tuck.app.ui.components.TuckSectionHeader
import com.tuck.app.ui.components.TuckThemePreviewCard
import com.tuck.app.ui.theme.TuckTheme
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.rememberModalBottomSheetState
import com.tuck.app.ui.theme.color.PaletteSlot
import com.tuck.app.ui.components.TuckFab
import com.tuck.app.ui.theme.pressScale
import com.tuck.app.ui.theme.TuckGradients
import androidx.compose.material3.ripple
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.tuck.app.ui.components.CollectionTile
import com.tuck.app.ui.components.ItemQuickActionsSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSearch: () -> Unit,
    /** Opens the Collections tab; a non-null id opens that collection directly. */
    onNavigateToCollections: (Long?) -> Unit = {},
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToSettings: () -> Unit = {},
    /** Incremented by the nav bar's capture button; each change opens the quick-add sheet. */
    captureSignal: Int = 0,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var quickActionsFor by remember { mutableStateOf<com.tuck.app.domain.model.SavedItem?>(null) }
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val tuckColors = TuckTheme.colors
    val tuckShapes = TuckTheme.shapes

    var showQuickAddSheet by remember { mutableStateOf(false) }

    // Keyed on the counter rather than a Boolean so pressing capture twice works the second time.
    LaunchedEffect(captureSignal) {
        if (captureSignal > 0) showQuickAddSheet = true
    }
    var showThemeSheet by remember { mutableStateOf(false) }
    var showPasteDialog by remember { mutableStateOf(false) }
    var pasteContent by remember { mutableStateOf("") }
    var isDismissedResurface by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val isRecording by viewModel.isRecording.collectAsStateWithLifecycle()
    var voiceMessage by remember { mutableStateOf<Int?>(null) }

    fun hasAudioPermission() = ContextCompat.checkSelfPermission(
        context, Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        // Start straight away on grant, so the tap that asked is the tap that records.
        if (granted) {
            if (!viewModel.startVoiceNote()) voiceMessage = R.string.home_voice_failed
        } else {
            voiceMessage = R.string.home_voice_needs_mic
        }
    }

    LaunchedEffect(voiceMessage) {
        voiceMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            voiceMessage = null
        }
    }

    // A recording left running when the screen goes away would hold the mic open.
    DisposableEffect(Unit) {
        onDispose { if (viewModel.isRecording.value) viewModel.cancelVoiceNote() }
    }

    // One voice-note action, shared by the quick-action row and the capture sheet. The
    // sheet's "voice note" row used to be wired to an empty lambda, so the app offered a
    // recorder that did nothing when you asked for it from there.
    val toggleVoiceNote: () -> Unit = {
        when {
            isRecording -> viewModel.stopAndSaveVoiceNote { saved ->
                voiceMessage = if (saved) R.string.home_voice_saved else R.string.home_voice_too_short
            }
            hasAudioPermission() -> {
                if (!viewModel.startVoiceNote()) voiceMessage = R.string.home_voice_failed
            }
            // Asked at the point of use, never on first launch.
            else -> audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 20)
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importImagesFromUris(uris)
        }
    }

    // The Scan button used to open the photo picker, which was a promise the app did not
    // keep. Real scanning now; the picker stays as the fallback where Play Services is absent.
    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val pdf = DocumentScanner.pdfFrom(result.data)
        if (pdf != null) {
            val pages = DocumentScanner.pageCount(result.data)
            viewModel.importDocumentFromUri(
                uri = pdf,
                titleOverride = if (pages > 1) {
                    context.getString(R.string.home_scan_multi_page, pages)
                } else {
                    context.getString(R.string.title_scan)
                }
            )
        }
    }

    val docPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importDocumentFromUri(uri)
        }
    }

    val isDark = when (appSettings.theme) {
        com.tuck.app.domain.repository.AppTheme.DARK -> true
        com.tuck.app.domain.repository.AppTheme.LIGHT -> false
        com.tuck.app.domain.repository.AppTheme.SYSTEM -> isSystemInDarkTheme()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(tuckColors.background)
    ) {
        // A staggered grid rather than a column: in list mode every section spans the
        // full line and it behaves exactly as the column did, but it can also lay the
        // feed out as tiles without a second scroller nested inside this one.
        //
        // Every section body is wrapped in a Column, and must stay wrapped. A LazyColumn
        // stacks the several children of one `item {}` down the main axis; a staggered
        // grid places them all at the same offset instead, so a bare `item { Header();
        // Rail() }` draws the header on top of the rail.
        val feedViewMode = appSettings.feedViewMode
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 4.dp, bottom = 80.dp),
            verticalItemSpacing = 0.dp
        ) {
            // 1. Hero Brand Header & Search Trigger
            item(span = StaggeredGridItemSpan.FullLine) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HomeHeroHeader(
                        itemCount = uiState.items.size,
                        folderCount = uiState.collections.size,
                        isDarkMode = isDark,
                        onOpenThemePicker = { showThemeSheet = true },
                        onNavigateToSettings = onNavigateToSettings,
                        onNavigateToSearch = onNavigateToSearch
                    )
                }
            }

            // 2. Quick Action Buttons Row
            item(span = StaggeredGridItemSpan.FullLine) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HomeQuickActionRow(
                        onNote = {
                            pasteContent = ""
                            showPasteDialog = true
                        },
                        onScan = {
                            val activity = context as? Activity
                            if (activity == null) {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            } else {
                                DocumentScanner.start(
                                    activity = activity,
                                    onReady = {
                                        scannerLauncher.launch(IntentSenderRequest.Builder(it).build())
                                    },
                                    onUnavailable = {
                                        // Expected on de-Googled devices - fall back, do not scold.
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(
                                                ActivityResultContracts.PickVisualMedia.ImageOnly
                                            )
                                        )
                                    }
                                )
                            }
                        },
                        onPdf = {
                            docPickerLauncher.launch(arrayOf("application/pdf", "text/*"))
                        },
                        onVoice = toggleVoiceNote,
                        isRecording = isRecording
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }

            // 3. Screenshot Detection Banner
            if (uiState.unimportedScreenshotsCount > 0) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        ScreenshotSyncBanner(
                            count = uiState.unimportedScreenshotsCount,
                            isImporting = uiState.isImporting,
                            onImport = { viewModel.importAllGalleryScreenshots() },
                            onDismiss = { viewModel.dismissScreenshotBanner() }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }

            // 4. Resurfaced Memory / Intent Recall Card
            if (!isDismissedResurface && uiState.rediscoverItems.isNotEmpty() && uiState.selectedSource == null) {
                val itemToResurface = uiState.rediscoverItems.first()
                item(span = StaggeredGridItemSpan.FullLine) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            TuckResurfacingCard(
                                item = itemToResurface,
                                onOpen = { onNavigateToDetail(itemToResurface.id) },
                                onDismiss = { isDismissedResurface = true }
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }

            // 5. Platform Filter Chips Row
            item(span = StaggeredGridItemSpan.FullLine) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HomePlatformChipsRow(
                        selectedSource = uiState.selectedSource,
                        onSelectSource = { viewModel.selectSource(it) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // 6. Horizontal "Recently Saved" Carousel (when viewing All and items exist)
            if (uiState.selectedSource == null && uiState.items.isNotEmpty()) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        TuckSectionHeader(
                            title = stringResource(R.string.home_recently_tucked)
                        )
                        RecentlySavedRail(
                            items = uiState.items.take(6),
                            onItemClick = onNavigateToDetail,
                            onToggleFavorite = { id, isFav ->
                                viewModel.toggleFavorite(id, isFav)
                            }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            // 6b. Collections rail — the fullest first, same tiles as the Collections tab
            if (uiState.selectedSource == null && uiState.collections.any { it.itemCount > 0 }) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        TuckSectionHeader(
                            title = stringResource(R.string.home_collections),
                            actionText = stringResource(R.string.action_view_all),
                            onAction = { onNavigateToCollections(null) }
                        )
                        CollectionsRail(
                            collections = uiState.collections
                                .sortedWith(
                                    compareByDescending<com.tuck.app.domain.model.Collection> { it.itemCount }
                                        .thenBy { it.name.lowercase() }
                                )
                                .take(6),
                            onCollectionClick = { collectionId -> onNavigateToCollections(collectionId) }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            // 7. Feed Title
            item(span = StaggeredGridItemSpan.FullLine) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    TuckSectionHeader(
                        title = uiState.selectedSource?.let { stringResource(R.string.home_source_items, it) }
                                ?: stringResource(R.string.home_all_tucked_items),
                        trailing = if (uiState.items.isNotEmpty()) {
                            {
                                FeedViewModeToggle(
                                    mode = feedViewMode,
                                    onSelect = { viewModel.setFeedViewMode(it) }
                                )
                            }
                        } else {
                            null
                        }
                    )
                }
            }

            // 8. Saved Items Feed or Empty State
            if (uiState.items.isEmpty()) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        TuckEmptyState(
                            title = uiState.selectedSource?.let { stringResource(R.string.empty_no_source_items, it) }
                                ?: stringResource(R.string.empty_drawer),
                            description = uiState.selectedSource?.let {
                                stringResource(R.string.empty_no_source_body, it)
                            } ?: stringResource(R.string.empty_drawer_body),
                            actionLabel = stringResource(R.string.action_quick_save),
                            onAction = { showQuickAddSheet = true },
                            modifier = Modifier.padding(top = 24.dp)
                        )
                    }
                }
            } else {
                items(
                    items = uiState.items,
                    key = { it.id },
                    span = {
                        if (feedViewMode == FeedViewMode.GRID) {
                            StaggeredGridItemSpan.SingleLane
                        } else {
                            StaggeredGridItemSpan.FullLine
                        }
                    }
                ) { item ->
                    if (feedViewMode == FeedViewMode.GRID) {
                        Box(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                            TuckGridCard(
                                item = item,
                                onLongPress = { quickActionsFor = item },
                                onClick = { onNavigateToDetail(item.id) }
                            )
                        }
                    } else {
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                            TuckContentCard(
                                item = item,
                                onLongPress = { quickActionsFor = item },
                                onClick = { onNavigateToDetail(item.id) },
                                onToggleFavorite = {
                                    viewModel.toggleFavorite(item.id, item.isFavorite)
                                }
                            )
                        }
                    }
                }
            }
        }


    }

    quickActionsFor?.let { target ->
        ItemQuickActionsSheet(
            item = target,
            collections = uiState.collections,
            onDismiss = { quickActionsFor = null },
            onMoveTo = { collectionId ->
                viewModel.addToCollection(target.id, collectionId)
                quickActionsFor = null
            },
            onToggleDone = { viewModel.toggleCompleted(target.id, target.completedAt != null) },
            onDelete = { viewModel.moveToTrash(target.id) }
        )
    }

    // Quick Capture Speed Dial Bottom Sheet
    if (showQuickAddSheet) {
        QuickCaptureSpeedDialSheet(
            onDismiss = { showQuickAddSheet = false },
            onCaptureNote = {
                pasteContent = ""
                showPasteDialog = true
            },
            onScanOcr = {
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onImportPdf = {
                docPickerLauncher.launch(arrayOf("application/pdf", "text/*"))
            },
            onPasteLink = {
                pasteContent = ""
                showPasteDialog = true
            },
            onRecordAudio = toggleVoiceNote
        )
    }

    if (showThemeSheet) {
        QuickThemePickerSheet(
            currentTheme = appSettings.theme,
            currentFlavor = appSettings.themeFlavor,
            onSelectTheme = { viewModel.setTheme(it) },
            onSelectFlavor = { viewModel.setThemeFlavor(it) },
            onDismiss = { showThemeSheet = false }
        )
    }

    // Paste / Write Note Dialog
    if (showPasteDialog) {
        AlertDialog(
            onDismissRequest = {
                showPasteDialog = false
                pasteContent = ""
            },
            title = {
                Text(
                    text = stringResource(R.string.home_quick_save),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = tuckColors.textPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.home_paste_a_url_reddit_thread_tweet),
                        style = MaterialTheme.typography.bodySmall,
                        color = tuckColors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = pasteContent,
                        onValueChange = { pasteContent = it },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.home_https_or_type_your_thought),
                                color = tuckColors.textMuted,
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        maxLines = 5,
                        shape = tuckShapes.medium,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = tuckColors.surfaceCard,
                            unfocusedContainerColor = tuckColors.surfaceCard,
                            focusedBorderColor = tuckColors.accent,
                            unfocusedBorderColor = tuckColors.border,
                            focusedTextColor = tuckColors.textPrimary,
                            unfocusedTextColor = tuckColors.textPrimary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pasteContent.isNotBlank()) {
                            viewModel.quickAdd(pasteContent.trim(), false)
                            showPasteDialog = false
                            pasteContent = ""
                        }
                    },
                    shape = tuckShapes.small,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = tuckColors.accent,
                        contentColor = tuckColors.textOnAccent
                    ),
                    enabled = pasteContent.isNotBlank()
                ) {
                    Text(stringResource(R.string.home_tuck_away), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPasteDialog = false
                    pasteContent = ""
                }) {
                    Text(stringResource(R.string.collections_cancel), color = tuckColors.textMuted)
                }
            },
            containerColor = tuckColors.surface,
            shape = tuckShapes.large
        )
    }
}

@Composable
private fun HomeHeroHeader(
    itemCount: Int,
    folderCount: Int,
    isDarkMode: Boolean,
    onOpenThemePicker: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToSearch: () -> Unit
) {
    val tuckColors = TuckTheme.colors
    val tuckShapes = TuckTheme.shapes

    Box(modifier = Modifier.fillMaxWidth()) {
        // Atmospheric brand wash diffusing from top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .background(TuckGradients.canvasWash(tuckColors.accent, tuckColors.background))
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.home_editorial_eyebrow),
                        style = TuckTheme.typography.sectionLabel,
                        color = tuckColors.accent
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.home_my_stash),
                        style = TuckTheme.typography.displayLarge,
                        color = tuckColors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    CountStat(
                        savesCount = itemCount,
                        foldersCount = folderCount
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val themeSource = remember { MutableInteractionSource() }
                    // Theme Switcher Quick Button
                    Surface(
                        onClick = onOpenThemePicker,
                        shape = tuckShapes.medium,
                        color = tuckColors.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, tuckColors.borderSubtle),
                        modifier = Modifier
                            .size(48.dp)
                            .pressScale(themeSource)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Palette,
                                contentDescription = stringResource(R.string.home_switch_theme_palette),
                                tint = tuckColors.accent,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    val settingsSource = remember { MutableInteractionSource() }
                    // Settings Screen Button
                    Surface(
                        onClick = onNavigateToSettings,
                        shape = tuckShapes.medium,
                        color = tuckColors.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, tuckColors.borderSubtle),
                        modifier = Modifier
                            .size(48.dp)
                            .pressScale(settingsSource)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = stringResource(R.string.home_settings),
                                tint = tuckColors.textPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            val searchSource = remember { MutableInteractionSource() }
            // Large Search Field Trigger
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .pressScale(searchSource)
                    .shadow(
                        elevation = 2.dp,
                        shape = tuckShapes.pill,
                        ambientColor = tuckColors.accent.copy(alpha = 0.08f),
                        spotColor = tuckColors.accent.copy(alpha = 0.08f)
                    )
                    .clip(tuckShapes.pill)
                    .clickable(
                        interactionSource = searchSource,
                        indication = ripple(color = tuckColors.accent),
                        onClick = onNavigateToSearch
                    ),
                color = tuckColors.surface,
                shape = tuckShapes.pill,
                border = androidx.compose.foundation.BorderStroke(1.dp, tuckColors.borderSubtle)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = stringResource(R.string.components_search),
                        tint = tuckColors.accent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.home_search_anything_ocr_text_or_tags),
                        style = MaterialTheme.typography.bodyMedium,
                        color = tuckColors.textMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeQuickActionRow(
    onNote: () -> Unit,
    onScan: () -> Unit,
    onPdf: () -> Unit,
    onVoice: () -> Unit,
    isRecording: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuickActionButton(
            label = stringResource(R.string.home_note),
            sublabel = stringResource(R.string.home_quick_note),
            icon = Icons.Rounded.EditNote,
            slot = PaletteSlot.PRIMARY_CORE,
            modifier = Modifier.weight(1f),
            onClick = onNote
        )
        QuickActionButton(
            label = stringResource(R.string.home_scan),
            sublabel = stringResource(R.string.home_document),
            icon = Icons.Rounded.DocumentScanner,
            slot = PaletteSlot.SECONDARY_CORE,
            modifier = Modifier.weight(1f),
            onClick = onScan
        )
        QuickActionButton(
            label = stringResource(R.string.home_doc_pdf),
            sublabel = stringResource(R.string.home_add_file),
            icon = Icons.Rounded.UploadFile,
            slot = PaletteSlot.TERTIARY_CORE,
            modifier = Modifier.weight(1f),
            onClick = onPdf
        )
        QuickActionButton(
            label = stringResource(if (isRecording) R.string.home_stop else R.string.home_voice),
            sublabel = stringResource(if (isRecording) R.string.home_recording else R.string.home_record),
            icon = if (isRecording) Icons.Rounded.Stop else Icons.Rounded.GraphicEq,
            // The recording state gets the deep tone so a live mic is unmistakable.
            slot = if (isRecording) PaletteSlot.PRIMARY_DEEP else PaletteSlot.SECONDARY_DEEP,
            isActive = isRecording,
            modifier = Modifier.weight(1f),
            onClick = onVoice
        )
    }
}

@Composable
private fun QuickActionButton(
    label: String,
    sublabel: String,
    icon: ImageVector,
    slot: PaletteSlot,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = false
) {
    val colors = TuckTheme.colors
    val palette = colors.palette[slot]
    val interactionSource = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(22.dp)
    val iconShape = RoundedCornerShape(16.dp)

    Column(
        modifier = modifier
            .pressScale(interactionSource, pressedScale = 0.95f)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(TuckGradients.tint(palette.fill, colors.isDark), colors.surface)
                )
            )
            .border(
                1.dp,
                if (isActive) palette.fill else colors.borderSubtle,
                shape
            )
            .semantics(mergeDescendants = true) {
                if (isActive) liveRegion = LiveRegionMode.Polite
            }
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = palette.fill),
                role = Role.Button,
                onClick = onClick
            )
            .padding(vertical = 14.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // A tilted backing card gives the four tools a shared, tactile silhouette.
        // Keep the actual glyph upright and use the palette's contrast-tested foreground.
        Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier.size(44.dp).rotate(-10f)
                    .background(palette.fill.copy(alpha = 0.18f), iconShape)
            )
            Box(
                Modifier.size(42.dp)
                    .shadow(3.dp, iconShape, ambientColor = palette.fill.copy(alpha = 0.2f),
                        spotColor = palette.fill.copy(alpha = 0.2f))
                    .background(palette.fill, iconShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = palette.onFill, modifier = Modifier.size(26.dp))
            }
            if (isActive) {
                Box(
                    Modifier.align(Alignment.TopEnd).size(10.dp)
                        .background(colors.destructive, CircleShape)
                        .border(2.dp, colors.surface, CircleShape)
                )
            }
        }
        Spacer(Modifier.height(9.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = sublabel,
            style = MaterialTheme.typography.labelSmall,
            color = colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HomePlatformChipsRow(
    selectedSource: String?,
    onSelectSource: (String?) -> Unit
) {
    val platforms = listOf(
        stringResource(R.string.filter_all) to null,
        stringResource(R.string.filter_screenshots) to "Screenshots",
        "Reddit" to "Reddit",
        "YouTube" to "YouTube",
        "Twitter / X" to "Twitter",
        "LinkedIn" to "LinkedIn",
        "Instagram" to "Instagram",
        stringResource(R.string.filter_articles) to "Articles",
        stringResource(R.string.filter_education) to "Education",
        stringResource(R.string.filter_finance) to "Finance",
        stringResource(R.string.filter_programming) to "Programming",
        "Web" to "Web",
        stringResource(R.string.filter_notes) to "Notes",
        stringResource(R.string.filter_pdfs) to "PDFs"
    )

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(platforms) { (label, key) ->
            TuckCategoryChip(
                label = label,
                isSelected = selectedSource == key,
                onClick = { onSelectSource(key) }
            )
        }
    }
}

@Composable
private fun RecentlySavedRail(
    items: List<SavedItem>,
    onItemClick: (Long) -> Unit,
    onToggleFavorite: (Long, Boolean) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items, key = { "rail_${it.id}" }) { item ->
            Box(modifier = Modifier.width(260.dp)) {
                TuckContentCard(
                    item = item,
                    onClick = { onItemClick(item.id) },
                    onToggleFavorite = { onToggleFavorite(item.id, item.isFavorite) }
                )
            }
        }
    }
}

@Composable
private fun ScreenshotSyncBanner(
    count: Int,
    isImporting: Boolean,
    onImport: () -> Unit,
    onDismiss: () -> Unit
) {
    val tuckColors = TuckTheme.colors
    val tuckShapes = TuckTheme.shapes

    Card(
        shape = tuckShapes.medium,
        colors = CardDefaults.cardColors(containerColor = tuckColors.accentContainer),
        border = androidx.compose.foundation.BorderStroke(1.dp, tuckColors.accent.copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(tuckColors.accent.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PhotoLibrary,
                    contentDescription = null,
                    tint = tuckColors.accent,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pluralStringResource(R.plurals.screenshots_found, count, count),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = tuckColors.textPrimary
                )
                Text(
                    text = stringResource(R.string.home_import_index_text_with_on_device),
                    style = MaterialTheme.typography.bodySmall,
                    color = tuckColors.textSecondary
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (isImporting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.5.dp,
                    color = tuckColors.accent
                )
            } else {
                Button(
                    onClick = onImport,
                    shape = tuckShapes.small,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = tuckColors.accent)
                ) {
                    Text(stringResource(R.string.home_import), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.components_dismiss),
                        tint = tuckColors.textMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickThemePickerSheet(
    currentTheme: com.tuck.app.domain.repository.AppTheme,
    currentFlavor: com.tuck.app.domain.repository.TuckThemeFlavor,
    onSelectTheme: (com.tuck.app.domain.repository.AppTheme) -> Unit,
    onSelectFlavor: (com.tuck.app.domain.repository.TuckThemeFlavor) -> Unit,
    onDismiss: () -> Unit
) {
    val tuckColors = TuckTheme.colors
    val tuckShapes = TuckTheme.shapes
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = tuckColors.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        // Six preview cards are taller than any phone's sheet, so without this the last
        // three palettes were drawn off the bottom edge and could not be reached at all —
        // Cobalt, Plum and Harbor were effectively unselectable from here.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 36.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.home_appearance_themes),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = tuckColors.textPrimary
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.home_close), tint = tuckColors.textMuted)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            SectionLabel(text = stringResource(R.string.home_appearance_mode))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(tuckShapes.pill)
                    .background(tuckColors.surfaceCard)
                    .border(1.dp, tuckColors.borderSubtle, tuckShapes.pill)
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    Triple(stringResource(R.string.theme_system), com.tuck.app.domain.repository.AppTheme.SYSTEM, Icons.Filled.SettingsBrightness),
                    Triple(stringResource(R.string.theme_light), com.tuck.app.domain.repository.AppTheme.LIGHT, Icons.Filled.LightMode),
                    Triple(stringResource(R.string.theme_dark), com.tuck.app.domain.repository.AppTheme.DARK, Icons.Filled.DarkMode)
                ).forEach { (label, theme, icon) ->
                    val isSelected = currentTheme == theme
                    val chipSource = remember { MutableInteractionSource() }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .pressScale(chipSource, pressedScale = 0.95f)
                            .clip(tuckShapes.pill)
                            .background(if (isSelected) tuckColors.surface else Color.Transparent)
                            .then(
                                if (isSelected) Modifier.border(1.dp, tuckColors.border, tuckShapes.pill)
                                else Modifier
                            )
                            .clickable(
                                interactionSource = chipSource,
                                indication = ripple(color = tuckColors.accent),
                                onClick = { onSelectTheme(theme) }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isSelected) tuckColors.accent else tuckColors.textMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) tuckColors.textPrimary else tuckColors.textSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            SectionLabel(text = stringResource(R.string.home_theme_palette))
            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                com.tuck.app.domain.repository.TuckThemeFlavor.values().forEach { flavor ->
                    TuckThemePreviewCard(
                        flavor = flavor,
                        isSelected = currentFlavor == flavor,
                        onClick = { onSelectFlavor(flavor) }
                    )
                }
            }
        }
    }
}

/**
 * The fullest collections, in the same tiles the Collections tab uses.
 *
 * Home is where someone lands, so it should answer "what have I got" before "what did I
 * just save". Populated collections lead; the rail hides itself entirely when everything
 * is empty, rather than showing a row of nothing.
 */
@Composable
private fun CollectionsRail(
    collections: List<com.tuck.app.domain.model.Collection>,
    onCollectionClick: (Long) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(collections, key = { it.id }) { collection ->
            CollectionTile(
                name = collection.name,
                count = collection.itemCount,
                openCount = collection.openCount,
                previewPaths = collection.previewPaths,
                colorId = collection.color,
                iconHint = collection.icon,
                collectionId = collection.id,
                isLocked = collection.isLocked,
                onClick = { onCollectionClick(collection.id) },
                modifier = Modifier.width(168.dp)
            )
        }
    }
}
