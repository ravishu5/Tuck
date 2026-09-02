package com.tuck.app.ui.collections

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import com.tuck.app.R

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tuck.app.domain.model.Collection
import com.tuck.app.ui.components.CollectionTile
import com.tuck.app.ui.components.CountStat
import com.tuck.app.ui.components.FilterPill
import com.tuck.app.ui.components.TuckCategoryCard
import com.tuck.app.ui.components.TuckContentCard
import com.tuck.app.ui.components.TuckEmptyState
import com.tuck.app.ui.components.TuckSearchBar
import com.tuck.app.ui.components.TuckSectionHeader
import com.tuck.app.ui.theme.TuckTheme
import com.tuck.app.ui.components.TuckFab
import com.tuck.app.ui.components.CollectionProgressRow
import com.tuck.app.ui.components.CollectionSectionHeader

@Composable
fun CollectionsScreen(
    onNavigateToDetail: (Long) -> Unit,
    viewModel: CollectionsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val tuckColors = TuckTheme.colors
    val tuckShapes = TuckTheme.shapes
    val context = androidx.compose.ui.platform.LocalContext.current
    val fragmentActivity = context as? androidx.fragment.app.FragmentActivity

    var showCreateDialog by remember { mutableStateOf(false) }
    var newCollectionName by remember { mutableStateOf("") }
    var selectedColorId by remember { mutableStateOf("terracotta") }
    var categorySearchQuery by remember { mutableStateOf("") }
    var isGridView by remember { mutableStateOf(true) }
    var filterTab by remember { mutableStateOf(0) } // 0: All, 1: Curated, 2: Smart, 3: Locked

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(tuckColors.background)
    ) {
        if (uiState.selectedCollection != null) {
                // Category Detail View
                val currentSelected = uiState.selectedCollection!!
                val filteredItems = uiState.collectionItems.filter { item ->
                    if (categorySearchQuery.isBlank()) true
                    else item.title.contains(categorySearchQuery, ignoreCase = true) ||
                         (item.description ?: "").contains(categorySearchQuery, ignoreCase = true)
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    // Category Top Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { viewModel.selectCollection(null) }) {
                                Icon(
                                    imageVector = Icons.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.collections_back),
                                    tint = tuckColors.textPrimary
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Column {
                                Text(
                                    text = currentSelected.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = tuckColors.textPrimary
                                )
                                Text(
                                    text = pluralStringResource(
                                        R.plurals.item_count,
                                        uiState.collectionItems.size,
                                        uiState.collectionItems.size
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = tuckColors.textMuted
                                )
                            }
                        }

                        if (!currentSelected.isAutoGenerated) {
                            IconButton(onClick = { viewModel.deleteCollection(currentSelected.id) }) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = stringResource(R.string.collections_delete_collection),
                                    tint = tuckColors.accent.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

                    // In-Category Search
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        TuckSearchBar(
                            query = categorySearchQuery,
                            onQueryChange = { categorySearchQuery = it },
                            onSearch = {},
                            placeholder = stringResource(R.string.search_in_collection, currentSelected.name),
                            onClear = { categorySearchQuery = "" }
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (filteredItems.isEmpty()) {
                        TuckEmptyState(
                            title = stringResource(R.string.empty_nothing_here_yet),
                            description = "Share something to Tuck\nand file it into ${currentSelected.name}.",
                            icon = Icons.Filled.Folder,
                            modifier = Modifier.padding(top = 40.dp)
                        )
                    } else {
                        // Organised rather than dumped: what still needs the user comes
                        // first, then what is finished, each under its own heading. A flat
                        // reverse-chronological list buries the outstanding work under
                        // whatever happened to be saved most recently.
                        val outstanding = filteredItems.filter { it.completedAt == null }
                        val finished = filteredItems.filter { it.completedAt != null }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (outstanding.isNotEmpty() && finished.isNotEmpty()) {
                                item(key = "hdr_open") {
                                    CollectionSectionHeader(
                                        label = stringResource(R.string.collections_still_to_go),
                                        count = outstanding.size
                                    )
                                }
                            }

                            items(outstanding, key = { it.id }) { item ->
                                TuckContentCard(
                                    item = item,
                                    onClick = { onNavigateToDetail(item.id) },
                                    onToggleFavorite = {
                                        viewModel.toggleFavorite(item.id, item.isFavorite)
                                    }
                                )
                            }

                            if (finished.isNotEmpty()) {
                                item(key = "hdr_done") {
                                    CollectionSectionHeader(
                                        label = stringResource(R.string.collections_done),
                                        count = finished.size
                                    )
                                }
                                items(finished, key = { it.id }) { item ->
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
            } else {
                // All Collections View
                val totalSaves = (uiState.customCollections + uiState.autoCollections).sumOf { it.itemCount }
                // Each query already returns fullest-first, but concatenating two of them
                // would restart the count at the join, so the merged list is re-sorted.
                val byFullest = compareByDescending<com.tuck.app.domain.model.Collection> { it.itemCount }
                    .thenBy { it.name.lowercase() }
                val allCollections = (uiState.customCollections + uiState.autoCollections).sortedWith(byFullest)

                val displayCollections = when (filterTab) {
                    0 -> allCollections
                    1 -> uiState.customCollections
                    2 -> uiState.autoCollections
                    3 -> allCollections.filter { it.isLocked }
                    else -> allCollections
                }

                LazyVerticalGrid(
                    // Preview tiles carry three images and a two-line caption; three across is too
                    // cramped for that, so the grid is two.
                    columns = GridCells.Fixed(if (isGridView) 2 else 1),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Header (Spans full width)
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.collections_collections),
                                    style = TuckTheme.typography.displayLarge,
                                    color = tuckColors.textPrimary
                                )
                                IconButton(
                                    onClick = { isGridView = !isGridView },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isGridView) Icons.Filled.ViewList else Icons.Filled.GridView,
                                        contentDescription = stringResource(
                            if (isGridView) R.string.action_switch_to_list
                            else R.string.action_switch_to_grid
                        ),
                                        tint = tuckColors.textSecondary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            CountStat(
                                savesCount = totalSaves,
                                foldersCount = displayCollections.size
                            )
                        }
                    }

                    // Filter Pills (Spans full width)
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            item {
                                FilterPill(
                                    label = stringResource(R.string.collections_all),
                                    isSelected = filterTab == 0,
                                    count = uiState.customCollections.size + uiState.autoCollections.size,
                                    onClick = { filterTab = 0 }
                                )
                            }
                            item {
                                FilterPill(
                                    label = stringResource(R.string.collections_curated),
                                    isSelected = filterTab == 1,
                                    count = uiState.customCollections.size,
                                    onClick = { filterTab = 1 }
                                )
                            }
                            item {
                                FilterPill(
                                    label = stringResource(R.string.collections_smart),
                                    isSelected = filterTab == 2,
                                    count = uiState.autoCollections.size,
                                    onClick = { filterTab = 2 }
                                )
                            }
                            val lockedCount = (uiState.customCollections + uiState.autoCollections).count { it.isLocked }
                            if (lockedCount > 0) {
                                item {
                                    FilterPill(
                                        label = stringResource(R.string.collections_locked),
                                        isSelected = filterTab == 3,
                                        count = lockedCount,
                                        onClick = { filterTab = 3 }
                                    )
                                }
                            }
                        }
                    }

                    if (displayCollections.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            TuckEmptyState(
                                title = stringResource(
                        if (filterTab == 1) R.string.empty_no_custom_collections
                        else R.string.empty_no_collections_found
                    ),
                                description = stringResource(
                        if (filterTab == 1) R.string.empty_create_first_collection
                        else R.string.empty_collections_appear
                    ),
                                icon = Icons.Filled.Folder,
                                modifier = Modifier.padding(top = 32.dp)
                            )
                        }
                    } else {
                        items(displayCollections, key = { it.id }) { col ->
                            if (isGridView) {
                                CollectionTile(
                                    name = col.name,
                                    count = col.itemCount,
                                    openCount = col.openCount,
                                    previewPaths = col.previewPaths,
                                    colorId = col.color,
                                    iconHint = col.icon,
                                    collectionId = col.id,
                                    isLocked = col.isLocked,
                                    onClick = {
                                        if (col.isLocked && fragmentActivity != null) {
                                            val authManager = com.tuck.app.ui.security.BiometricAuthManager(context)
                                            authManager.authenticate(
                                                activity = fragmentActivity,
                                                title = "Unlock ${col.name}",
                                                onSuccess = { viewModel.selectCollection(col) },
                                                onError = { err ->
                                                    android.widget.Toast.makeText(context, err, android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        } else {
                                            viewModel.selectCollection(col)
                                        }
                                    }
                                )
                            } else {
                                CollectionProgressRow(
                                    name = col.name,
                                    count = col.itemCount,
                                    openCount = col.openCount,
                                    colorId = col.color,
                                    iconHint = col.icon,
                                    collectionId = col.id,
                                    isLocked = col.isLocked,
                                    onClick = {
                                        if (col.isLocked && fragmentActivity != null) {
                                            val authManager = com.tuck.app.ui.security.BiometricAuthManager(context)
                                            authManager.authenticate(
                                                activity = fragmentActivity,
                                                title = "Unlock ${col.name}",
                                                onSuccess = { viewModel.selectCollection(col) },
                                                onError = { err ->
                                                    android.widget.Toast.makeText(context, err, android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        } else {
                                            viewModel.selectCollection(col)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (uiState.selectedCollection == null) {
                TuckFab(
                    onClick = { showCreateDialog = true },
                    contentDescription = stringResource(R.string.collections_new_collection),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 16.dp)
                )
            }
        }

        // Create New Category Dialog
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = {
                showCreateDialog = false
                newCollectionName = ""
                selectedColorId = "coral"
            },
            title = {
                Text(
                    text = stringResource(R.string.collections_new_collection),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = tuckColors.textPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.collections_create_a_custom_collection_for_organizing),
                        style = MaterialTheme.typography.bodySmall,
                        color = tuckColors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newCollectionName,
                        onValueChange = { newCollectionName = it },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.collections_e_g_research_tech_recipes),
                                color = tuckColors.textMuted,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        singleLine = true,
                        shape = tuckShapes.medium,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = tuckColors.surfaceCard,
                            unfocusedContainerColor = tuckColors.surfaceCard,
                            focusedBorderColor = tuckColors.accent,
                            unfocusedBorderColor = tuckColors.border,
                            focusedTextColor = tuckColors.textPrimary,
                            unfocusedTextColor = tuckColors.textPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = stringResource(R.string.collections_choose_color),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = tuckColors.textSecondary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(tuckColors.collectionPalette) { colorEntry ->
                            val isSelected = selectedColorId == colorEntry.id
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(colorEntry.background)
                                    .clickable { selectedColorId = colorEntry.id },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = stringResource(R.string.collections_selected),
                                        tint = colorEntry.foreground,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCollectionName.isNotBlank()) {
                            viewModel.createCollection(newCollectionName.trim(), selectedColorId)
                            showCreateDialog = false
                            newCollectionName = ""
                            selectedColorId = "terracotta"
                        }
                    },
                    shape = tuckShapes.small,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = tuckColors.accent,
                        contentColor = tuckColors.textOnAccent
                    ),
                    enabled = newCollectionName.isNotBlank()
                ) {
                    Text(stringResource(R.string.collections_create), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCreateDialog = false
                    newCollectionName = ""
                    selectedColorId = "terracotta"
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
private fun CollectionTabButton(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tuckColors = TuckTheme.colors
    val shapes = TuckTheme.shapes

    Surface(
        shape = shapes.pill,
        color = if (isSelected) tuckColors.surface else Color.Transparent,
        modifier = modifier
            .clip(shapes.pill)
            .clickable(onClick = onClick)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) tuckColors.accent else tuckColors.textSecondary
            )
        }
    }
}
