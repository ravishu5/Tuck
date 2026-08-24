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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.PhotoLibrary
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tuck.app.ui.components.TuckCategoryChip
import com.tuck.app.ui.components.TuckContentCard
import com.tuck.app.ui.components.TuckEmptyState
import com.tuck.app.ui.components.TuckSectionHeader
import com.tuck.app.ui.theme.TuckTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSearch: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val tuckColors = TuckTheme.colors
    val tuckSpacing = TuckTheme.spacing
    val tuckShapes = TuckTheme.shapes

    var showQuickAddSheet by remember { mutableStateOf(false) }
    var showPasteDialog by remember { mutableStateOf(false) }
    var pasteContent by remember { mutableStateOf("") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 20)
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importImagesFromUris(uris)
        }
    }

    Scaffold(
        containerColor = tuckColors.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showQuickAddSheet = true },
                containerColor = tuckColors.accent,
                contentColor = tuckColors.textOnAccent,
                shape = CircleShape,
                modifier = Modifier.padding(bottom = 68.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Quick Stash",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(tuckColors.background)
                .statusBarsPadding()
                .padding(bottom = paddingValues.calculateBottomPadding()),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // 1. Hero Brand Header & Tagline
            item {
                HomeHeroHeader(onNavigateToSearch = onNavigateToSearch)
            }

            // 2. Platform Filter Chips Row
            item {
                HomePlatformChipsRow(
                    selectedSource = uiState.selectedSource,
                    onSelectSource = { viewModel.selectSource(it) }
                )
                Spacer(modifier = Modifier.height(10.dp))
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

            // 3.5. Rediscover from your vault (Memory Layer)
            if (uiState.rediscoverItems.isNotEmpty() && uiState.selectedSource == null) {
                item {
                    RediscoverMemorySection(
                        items = uiState.rediscoverItems,
                        onItemClick = onNavigateToDetail
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }

            // 4. Recently Tucked Section Title
            item {
                TuckSectionHeader(
                    title = if (uiState.selectedSource != null) "${uiState.selectedSource} Items" else "Recently Tucked"
                )
            }

            // 5. Saved Items Feed or Empty State
            if (uiState.items.isEmpty()) {
                item {
                    TuckEmptyState(
                        title = "Your drawer is empty.",
                        description = "Find something worth keeping and tuck it away.\nShare articles, reels, tweets, screenshots, or notes.",
                        actionLabel = "Quick Stash (+)",
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
    }

    // Quick Stash Bottom Sheet
    if (showQuickAddSheet) {
        ModalBottomSheet(
            onDismissRequest = { showQuickAddSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = tuckColors.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            QuickStashSheetContent(
                onPasteLink = {
                    showQuickAddSheet = false
                    showPasteDialog = true
                },
                onPickPhotos = {
                    showQuickAddSheet = false
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onWriteNote = {
                    showQuickAddSheet = false
                    showPasteDialog = true
                }
            )
        }
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
                    text = "Quick Stash",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = tuckColors.textPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "Paste a URL, tweet, reel, or write a quick note to tuck away:",
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
private fun HomeHeroHeader(onNavigateToSearch: () -> Unit) {
    val tuckColors = TuckTheme.colors
    val tuckShapes = TuckTheme.shapes

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = "Tuck",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            color = tuckColors.textPrimary
        )
        Text(
            text = "Everything you've tucked away.",
            style = MaterialTheme.typography.bodyMedium,
            color = tuckColors.textSecondary
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Large Beautiful Search Field Trigger
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(tuckShapes.medium)
                .clickable { onNavigateToSearch() },
            color = tuckColors.surfaceCard,
            shape = tuckShapes.medium,
            border = androidx.compose.foundation.BorderStroke(1.dp, tuckColors.border)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
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
                    text = "Search anything...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = tuckColors.textMuted
                )
            }
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
        "📸 Screenshots" to "Screenshots",
        "💼 LinkedIn" to "LinkedIn",
        "📱 Instagram" to "Instagram",
        "💬 Reddit" to "Reddit",
        "▶️ YouTube" to "YouTube",
        "🐦 X / Twitter" to "Twitter",
        "🌐 Web" to "Web",
        "📝 Notes" to "Notes",
        "📄 PDFs" to "PDFs"
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

@Composable
private fun QuickStashSheetContent(
    onPasteLink: () -> Unit,
    onPickPhotos: () -> Unit,
    onWriteNote: () -> Unit
) {
    val tuckColors = TuckTheme.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(
            text = "Tuck Away",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = tuckColors.textPrimary
        )
        Text(
            text = "Save links, screenshots, or thoughts to find later",
            style = MaterialTheme.typography.bodySmall,
            color = tuckColors.textSecondary
        )

        Spacer(modifier = Modifier.height(20.dp))

        QuickStashOptionRow(
            icon = Icons.Filled.Link,
            title = "Paste Link or Article",
            subtitle = "Saves web pages, Reddit, Instagram, YouTube",
            onClick = onPasteLink
        )

        Spacer(modifier = Modifier.height(10.dp))

        QuickStashOptionRow(
            icon = Icons.Filled.Image,
            title = "Import Screenshots / Photos",
            subtitle = "Extracts and indexes text with OCR",
            onClick = onPickPhotos
        )

        Spacer(modifier = Modifier.height(10.dp))

        QuickStashOptionRow(
            icon = Icons.Filled.Notes,
            title = "Write a Note",
            subtitle = "Keep quick thoughts, ideas, or snippets",
            onClick = onWriteNote
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun QuickStashOptionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val tuckColors = TuckTheme.colors
    val tuckShapes = TuckTheme.shapes

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(tuckShapes.medium)
            .clickable(onClick = onClick),
        shape = tuckShapes.medium,
        color = tuckColors.surfaceCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, tuckColors.border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(tuckColors.accentContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tuckColors.accent,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = tuckColors.textPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = tuckColors.textSecondary
                )
            }
        }
    }
}

@Composable
fun RediscoverMemorySection(
    items: List<com.tuck.app.domain.model.SavedItem>,
    onItemClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val tuckColors = TuckTheme.colors
    val tuckShapes = TuckTheme.shapes

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Notes,
                    contentDescription = null,
                    tint = tuckColors.accent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Rediscover from your vault",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = tuckColors.textPrimary
                )
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items, key = { "rediscover_${it.id}" }) { item ->
                Surface(
                    shape = tuckShapes.medium,
                    color = tuckColors.surfaceCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, tuckColors.border),
                    modifier = Modifier
                        .width(220.dp)
                        .clip(tuckShapes.medium)
                        .clickable { onItemClick(item.id) }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = tuckColors.textPrimary,
                            maxLines = 2,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = item.sourceDomain ?: item.contentType.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = tuckColors.accent
                        )
                    }
                }
            }
        }
    }
}

