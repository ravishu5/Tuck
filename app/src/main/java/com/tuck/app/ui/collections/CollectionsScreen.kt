package com.tuck.app.ui.collections

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
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
import com.tuck.app.ui.components.TuckCategoryCard
import com.tuck.app.ui.components.TuckContentCard
import com.tuck.app.ui.components.TuckEmptyState
import com.tuck.app.ui.components.TuckSearchBar
import com.tuck.app.ui.components.TuckSectionHeader
import com.tuck.app.ui.theme.TuckTheme

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
    var categorySearchQuery by remember { mutableStateOf("") }
    var activeTab by remember { mutableStateOf(0) } // 0: Curated, 1: Smart

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
                                    contentDescription = "Back",
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
                                    text = "${uiState.collectionItems.size} items",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = tuckColors.textMuted
                                )
                            }
                        }

                        if (!currentSelected.isAutoGenerated) {
                            IconButton(onClick = { viewModel.deleteCollection(currentSelected.id) }) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "Delete Collection",
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
                            placeholder = "Search in ${currentSelected.name}...",
                            onClear = { categorySearchQuery = "" }
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (filteredItems.isEmpty()) {
                        TuckEmptyState(
                            title = "Nothing here yet.",
                            description = "Share something to Tuck\nand file it into ${currentSelected.name}.",
                            icon = Icons.Filled.Folder,
                            modifier = Modifier.padding(top = 40.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredItems, key = { it.id }) { item ->
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
            } else {
                // All Collections View
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Column {
                            Text(
                                text = "Collections",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = tuckColors.textPrimary
                            )
                            Text(
                                text = "Organized boards and smart categories.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = tuckColors.textSecondary
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // Tab Selector: Curated vs Smart
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(tuckShapes.pill)
                                .background(tuckColors.surfaceCard)
                                .padding(4.dp)
                        ) {
                            CollectionTabButton(
                                title = "Curated (${uiState.customCollections.size})",
                                isSelected = activeTab == 0,
                                onClick = { activeTab = 0 },
                                modifier = Modifier.weight(1f)
                            )
                            CollectionTabButton(
                                title = "Smart Auto (${uiState.autoCollections.size})",
                                isSelected = activeTab == 1,
                                onClick = { activeTab = 1 },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    val displayCollections = if (activeTab == 0) {
                        uiState.customCollections
                    } else {
                        uiState.autoCollections
                    }

                    if (displayCollections.isEmpty()) {
                        item {
                            TuckEmptyState(
                                title = if (activeTab == 0) "No custom collections" else "No smart categories",
                                description = if (activeTab == 0) "Create your first collection with (+)" else "Smart categories will appear automatically as you save items.",
                                icon = Icons.Filled.Folder,
                                modifier = Modifier.padding(top = 32.dp)
                            )
                        }
                    } else {
                        items(displayCollections, key = { it.id }) { col ->
                            TuckCategoryCard(
                                name = col.name,
                                count = col.itemCount ?: 0,
                                icon = col.icon,
                                isAutoGenerated = col.isAutoGenerated,
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

            if (uiState.selectedCollection == null) {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = tuckColors.accent,
                    contentColor = tuckColors.textOnAccent,
                    shape = CircleShape,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "New Collection",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Create New Category Dialog
        if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = {
                showCreateDialog = false
                newCollectionName = ""
            },
            title = {
                Text(
                    text = "New Collection",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = tuckColors.textPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "Create a custom collection for organizing your saves:",
                        style = MaterialTheme.typography.bodySmall,
                        color = tuckColors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newCollectionName,
                        onValueChange = { newCollectionName = it },
                        placeholder = {
                            Text(
                                text = "e.g. Research, Tech, Recipes",
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
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCollectionName.isNotBlank()) {
                            viewModel.createCollection(newCollectionName.trim())
                            showCreateDialog = false
                            newCollectionName = ""
                        }
                    },
                    shape = tuckShapes.small,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = tuckColors.accent,
                        contentColor = tuckColors.textOnAccent
                    ),
                    enabled = newCollectionName.isNotBlank()
                ) {
                    Text("Create", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCreateDialog = false
                    newCollectionName = ""
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
