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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

    var showCreateDialog by remember { mutableStateOf(false) }
    var newCollectionName by remember { mutableStateOf("") }
    var categorySearchQuery by remember { mutableStateOf("") }

    Scaffold(
        containerColor = tuckColors.background,
        floatingActionButton = {
            if (uiState.selectedCollection == null) {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = tuckColors.accent,
                    contentColor = tuckColors.textOnAccent,
                    shape = CircleShape,
                    modifier = Modifier.padding(bottom = 68.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "New Category",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(tuckColors.background)
                .statusBarsPadding()
                .padding(bottom = paddingValues.calculateBottomPadding())
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
                                    contentDescription = "Delete Folder",
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
                            description = "Share something to Tuck\nand choose this category.",
                            icon = Icons.Filled.Folder,
                            modifier = Modifier.padding(top = 40.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
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
                // All Categories View
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Column {
                            Text(
                                text = "Categories",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = tuckColors.textPrimary
                            )
                            Text(
                                text = "Smart auto-organized digital folders.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = tuckColors.textSecondary
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    val allCollections = uiState.autoCollections + uiState.customCollections
                    items(allCollections, key = { it.id }) { col ->
                        TuckCategoryCard(
                            name = col.name,
                            count = col.itemCount ?: 0,
                            icon = col.icon,
                            isAutoGenerated = col.isAutoGenerated,
                            onClick = { viewModel.selectCollection(col) }
                        )
                    }
                }
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
                    text = "New Category",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = tuckColors.textPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "Create a custom category for saving items:",
                        style = MaterialTheme.typography.bodySmall,
                        color = tuckColors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newCollectionName,
                        onValueChange = { newCollectionName = it },
                        placeholder = {
                            Text(
                                text = "e.g. Recipes, Work, Reading",
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
