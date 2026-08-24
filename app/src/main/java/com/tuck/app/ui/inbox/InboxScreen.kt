package com.tuck.app.ui.inbox

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
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tuck.app.domain.model.SavedItem
import com.tuck.app.ui.components.EmptyState
import com.tuck.app.ui.components.TuckContentCard
import com.tuck.app.ui.theme.TuckTheme

@Composable
fun InboxScreen(
    onNavigateToDetail: (Long) -> Unit,
    viewModel: InboxViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val tuckColors = TuckTheme.colors
    val tuckSpacing = TuckTheme.spacing
    val tuckShapes = TuckTheme.shapes

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(tuckColors.background)
            .statusBarsPadding()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = tuckSpacing.m, vertical = tuckSpacing.s),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Inbox",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = tuckColors.textPrimary
                    )
                )
                Text(
                    text = if (uiState.inboxItems.isEmpty()) "All caught up" else "${uiState.inboxItems.size} items to triage",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = tuckColors.textSecondary
                    )
                )
            }

            Surface(
                shape = CircleShape,
                color = tuckColors.accentContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Inbox,
                        contentDescription = "Inbox",
                        tint = tuckColors.accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = tuckColors.accent)
            }
        } else if (uiState.inboxItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(tuckSpacing.l),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    title = "Inbox Zero",
                    description = "All saved items have been organized and reviewed. Great job!",
                    icon = Icons.Default.Check
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(tuckSpacing.m),
                verticalArrangement = Arrangement.spacedBy(tuckSpacing.m)
            ) {
                items(
                    items = uiState.inboxItems,
                    key = { it.id }
                ) { item ->
                    InboxTriageItemCard(
                        item = item,
                        onClick = { onNavigateToDetail(item.id) },
                        onToggleFavorite = { viewModel.toggleFavorite(item) },
                        onKeep = { viewModel.keepItem(item) },
                        onArchive = { viewModel.archiveItem(item) },
                        onCategorize = { viewModel.openCategorizeDialog(item) },
                        onDelete = { viewModel.deleteItem(item) }
                    )
                }
            }
        }
    }

    // Categorize Dialog
    uiState.selectedItemForCategorize?.let { selectedItem ->
        AlertDialog(
            onDismissRequest = { viewModel.closeCategorizeDialog() },
            title = {
                Text(
                    text = "Add to Collection",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = tuckColors.textPrimary
                    )
                )
            },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(260.dp)
                ) {
                    items(uiState.allCollections) { collection ->
                        Surface(
                            shape = tuckShapes.small,
                            color = tuckColors.surfaceCard,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.addToCollection(selectedItem.id, collection.id)
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = tuckColors.accent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = collection.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = tuckColors.textPrimary,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { viewModel.closeCategorizeDialog() }) {
                    Text("Cancel", color = tuckColors.textSecondary)
                }
            },
            containerColor = tuckColors.surface
        )
    }
}

@Composable
private fun InboxTriageItemCard(
    item: SavedItem,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onKeep: () -> Unit,
    onArchive: () -> Unit,
    onCategorize: () -> Unit,
    onDelete: () -> Unit
) {
    val tuckColors = TuckTheme.colors
    val tuckSpacing = TuckTheme.spacing
    val tuckShapes = TuckTheme.shapes

    Card(
        shape = tuckShapes.medium,
        colors = CardDefaults.cardColors(containerColor = tuckColors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Main card content preview
            TuckContentCard(
                item = item,
                onClick = onClick,
                onToggleFavorite = onToggleFavorite
            )

            // Triage Action Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(tuckColors.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = tuckSpacing.s, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Keep Button
                IconButton(
                    onClick = onKeep,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Keep",
                        tint = tuckColors.accent,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Categorize Button
                IconButton(
                    onClick = onCategorize,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = "Categorize",
                        tint = tuckColors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Archive Button
                IconButton(
                    onClick = onArchive,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Archive,
                        contentDescription = "Archive",
                        tint = tuckColors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Delete Button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
