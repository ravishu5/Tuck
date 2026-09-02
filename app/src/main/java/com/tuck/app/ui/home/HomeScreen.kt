package com.tuck.app.ui.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Notes
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.rememberModalBottomSheetState
import com.tuck.app.ui.theme.color.PaletteSlot

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSearch: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToSettings: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val tuckColors = TuckTheme.colors
    val tuckShapes = TuckTheme.shapes

    var showQuickAddSheet by remember { mutableStateOf(false) }
    var showThemeSheet by remember { mutableStateOf(false) }
    var showPasteDialog by remember { mutableStateOf(false) }
    var pasteContent by remember { mutableStateOf("") }
    var isDismissedResurface by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 20)
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importImagesFromUris(uris)
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
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 4.dp, bottom = 80.dp)
        ) {
            // 1. Hero Brand Header & Search Trigger
            item {
                HomeHeroHeader(
                    itemCount = uiState.items.size,
                    folderCount = uiState.collections.size,
                    isDarkMode = isDark,
                    onOpenThemePicker = { showThemeSheet = true },
                    onNavigateToSettings = onNavigateToSettings,
                    onNavigateToSearch = onNavigateToSearch
                )
            }

            // 2. Quick Action Buttons Row
            item {
                HomeQuickActionRow(
                    onNote = {
                        pasteContent = ""
                        showPasteDialog = true
                    },
                    onScan = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onPdf = {
                        docPickerLauncher.launch(arrayOf("application/pdf", "text/*"))
                    },
                    onPaste = {
                        pasteContent = ""
                        showPasteDialog = true
                    }
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            // 3. Screenshot Detection Banner
            if (uiState.unimportedScreenshotsCount > 0) {
                item {
                    ScreenshotSyncBanner(
                        count = uiState.unimportedScreenshotsCount,
                        isImporting = uiState.isImporting,
                        onImport = { viewModel.importAllGalleryScreenshots() },
                        onDismiss = { viewModel.dismissScreenshotBanner() }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            // 4. Resurfaced Memory / Intent Recall Card
            if (!isDismissedResurface && uiState.rediscoverItems.isNotEmpty() && uiState.selectedSource == null) {
                val itemToResurface = uiState.rediscoverItems.first()
                item {
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

            // 5. Platform Filter Chips Row
            item {
                HomePlatformChipsRow(
                    selectedSource = uiState.selectedSource,
                    onSelectSource = { viewModel.selectSource(it) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 6. Horizontal "Recently Saved" Carousel (when viewing All and items exist)
            if (uiState.selectedSource == null && uiState.items.isNotEmpty()) {
                item {
                    TuckSectionHeader(
                        title = "Recently Tucked",
                        actionText = "See All (${uiState.items.size})"
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

            // 7. Feed Title
            item {
                TuckSectionHeader(
                    title = if (uiState.selectedSource != null) "${uiState.selectedSource} Items" else "All Tucked Items"
                )
            }

            // 8. Saved Items Feed or Empty State
            if (uiState.items.isEmpty()) {
                item {
                    TuckEmptyState(
                        title = if (uiState.selectedSource != null) "No ${uiState.selectedSource} items found" else "Your drawer is empty.",
                        description = if (uiState.selectedSource != null) "Tuck items from ${uiState.selectedSource} to see them here." else "Save articles, screenshots, PDFs, notes, or links.\nEverything is indexed locally and private.",
                        actionLabel = "Quick Save (+)",
                        onAction = { showQuickAddSheet = true },
                        modifier = Modifier.padding(top = 24.dp)
                    )
                }
            } else {
                items(uiState.items, key = { it.id }) { item ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        TuckContentCard(
                            item = item,
                            onClick = { onNavigateToDetail(item.id) },
                            onToggleFavorite = {
                                viewModel.toggleFavorite(item.id, item.isFavorite)
                            }
                        )
                    }
                }
            }
        }

        // Floating Action Button anchored to bottom-end
        FloatingActionButton(
            onClick = { showQuickAddSheet = true },
            containerColor = tuckColors.accent,
            contentColor = tuckColors.textOnAccent,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Quick Save",
                modifier = Modifier.size(26.dp)
            )
        }
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
            onRecordAudio = {
                // Audio memo trigger
            }
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
                    text = "Quick Save",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = tuckColors.textPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "Paste a URL, Reddit thread, tweet, or write a quick note:",
                        style = MaterialTheme.typography.bodySmall,
                        color = tuckColors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = pasteContent,
                        onValueChange = { pasteContent = it },
                        placeholder = {
                            Text(
                                text = "https://... or type your thought",
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
                    Text("Tuck Away", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPasteDialog = false
                    pasteContent = ""
                }) {
                    Text("Cancel", color = tuckColors.textMuted)
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "My Stash",
                    style = TuckTheme.typography.displayLarge,
                    color = tuckColors.textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                CountStat(
                    savesCount = itemCount,
                    foldersCount = folderCount
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Theme Switcher Quick Button
                Surface(
                    onClick = onOpenThemePicker,
                    shape = tuckShapes.medium,
                    color = tuckColors.surfaceCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, tuckColors.border),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Palette,
                            contentDescription = "Switch Theme & Palette",
                            tint = tuckColors.accent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Settings Screen Button
                Surface(
                    onClick = onNavigateToSettings,
                    shape = tuckShapes.medium,
                    color = tuckColors.surfaceCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, tuckColors.border),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Settings",
                            tint = tuckColors.textPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Large Search Field Trigger
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(tuckShapes.pill)
                .clickable { onNavigateToSearch() },
            color = tuckColors.surfaceCard,
            shape = tuckShapes.pill,
            border = androidx.compose.foundation.BorderStroke(1.dp, tuckColors.border)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "Search",
                    tint = tuckColors.accent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Search anything, OCR text, or tags...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = tuckColors.textMuted
                )
            }
        }
    }
}

@Composable
private fun HomeQuickActionRow(
    onNote: () -> Unit,
    onScan: () -> Unit,
    onPdf: () -> Unit,
    onPaste: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuickActionButton(
            label = "Note",
            icon = Icons.Filled.Notes,
            tint = TuckTheme.colors.palette[PaletteSlot.PRIMARY_CORE].fill,
            modifier = Modifier.weight(1f),
            onClick = onNote
        )
        QuickActionButton(
            label = "Scan",
            icon = Icons.Filled.CameraAlt,
            tint = TuckTheme.colors.palette[PaletteSlot.SECONDARY_CORE].fill,
            modifier = Modifier.weight(1f),
            onClick = onScan
        )
        QuickActionButton(
            label = "Doc/PDF",
            icon = Icons.Filled.PictureAsPdf,
            tint = TuckTheme.colors.palette[PaletteSlot.TERTIARY_CORE].fill,
            modifier = Modifier.weight(1f),
            onClick = onPdf
        )
        QuickActionButton(
            label = "Paste",
            icon = Icons.Filled.ContentPaste,
            tint = TuckTheme.colors.palette[PaletteSlot.PRIMARY_SOFT].fill,
            modifier = Modifier.weight(1f),
            onClick = onPaste
        )
    }
}

@Composable
private fun QuickActionButton(
    label: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tuckColors = TuckTheme.colors
    val shapes = TuckTheme.shapes

    Surface(
        shape = shapes.small,
        color = tuckColors.surfaceCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, tuckColors.border),
        modifier = modifier
            .clip(shapes.small)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = tuckColors.textPrimary
            )
        }
    }
}

@Composable
private fun HomePlatformChipsRow(
    selectedSource: String?,
    onSelectSource: (String?) -> Unit
) {
    val platforms = listOf(
        "All" to null,
        "Screenshots" to "Screenshots",
        "Reddit" to "Reddit",
        "YouTube" to "YouTube",
        "Twitter / X" to "Twitter",
        "LinkedIn" to "LinkedIn",
        "Instagram" to "Instagram",
        "Articles" to "Articles",
        "Education" to "Education",
        "Finance" to "Finance",
        "Programming" to "Programming",
        "Web" to "Web",
        "Notes" to "Notes",
        "PDFs" to "PDFs"
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
                    text = "📸 Found $count Screenshots",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = tuckColors.textPrimary
                )
                Text(
                    text = "Import & index text with on-device OCR",
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
                    Text("Import", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Dismiss",
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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = tuckColors.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 36.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Appearance & Themes",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = tuckColors.textPrimary
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = tuckColors.textMuted)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            SectionLabel(text = "Appearance Mode")
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "System" to com.tuck.app.domain.repository.AppTheme.SYSTEM,
                    "Light" to com.tuck.app.domain.repository.AppTheme.LIGHT,
                    "Dark" to com.tuck.app.domain.repository.AppTheme.DARK
                ).forEach { (label, theme) ->
                    TuckCategoryChip(
                        label = label,
                        isSelected = currentTheme == theme,
                        onClick = { onSelectTheme(theme) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            SectionLabel(text = "Theme Palette")
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
