package com.tuck.app.ui.detail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.tuck.app.domain.model.ContentType
import com.tuck.app.domain.model.ProcessingStatus
import com.tuck.app.domain.model.SavedItem
import com.tuck.app.ui.components.ContentTypeBadge
import com.tuck.app.ui.components.EntityActionChip
import com.tuck.app.ui.components.formatRelativeTime
import com.tuck.app.ui.components.startActivitySafe
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.tuck.app.processing.ReminderPreset
import com.tuck.app.ui.theme.TuckTheme
import kotlinx.coroutines.launch
import com.tuck.app.domain.model.SearchableBlock
import com.tuck.app.domain.model.InItemSearch
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.foundation.layout.PaddingValues

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ItemDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: ((Long) -> Unit)? = null,
    viewModel: ItemDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var showCollectionDialog by remember { mutableStateOf(false) }
    var showFullImageViewer by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var findQuery by remember { mutableStateOf("") }
    var findMatchIndex by remember { mutableIntStateOf(0) }
    // Scroll offsets of the searchable regions, recorded as they lay out, so a match can
    // be scrolled to. The screen is a plain scrolling Column rather than a lazy list, so
    // there is no index to scroll to - only a pixel offset.
    val blockOffsets = remember { mutableStateMapOf<String, Int>() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) {
            onNavigateBack()
        }
    }

    val item = uiState.item

    if (item == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // Top Bar
        DetailTopBar(
            item = item,
            onBack = onNavigateBack,
            onToggleFavorite = { viewModel.toggleFavorite() },
            onShare = { shareItem(context, item) },
            onDelete = { showDeleteConfirmDialog = true }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            FindWithinItem(
                item = item,
                commentsTree = uiState.commentsTree,
                query = findQuery,
                onQueryChange = {
                    findQuery = it
                    findMatchIndex = 0
                },
                matchIndex = findMatchIndex,
                onStep = { forward, total ->
                    findMatchIndex = InItemSearch.step(findMatchIndex, total, forward)
                },
                onJumpTo = { blockId ->
                    blockOffsets[blockId]?.let { offset ->
                        coroutineScope.launch { scrollState.animateScrollTo(offset) }
                    }
                }
            )

            // Source & Date Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ContentTypeBadge(contentType = item.contentType)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.sourceDomain ?: item.sourceApp ?: item.contentType.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault()).format(Date(item.createdAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Editable Title
            if (uiState.isEditingTitle) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = uiState.editedTitle,
                        onValueChange = { viewModel.onTitleChange(it) },
                        modifier = Modifier.weight(1f),
                        label = { Text("Title") },
                        singleLine = false,
                        maxLines = 3
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = { viewModel.saveTitle() }) {
                        Icon(imageVector = Icons.Filled.Check, contentDescription = "Save", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { viewModel.cancelEditTitle() }) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = "Cancel", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = item.displayTitle,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { viewModel.startEditTitle(item.title) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit Title",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Preview Section (Live Interactive Post/Video Viewer vs Archive Snapshot)
            if (!item.originalUrl.isNullOrBlank()) {
                var selectedTab by remember { mutableIntStateOf(0) }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val tabLabels = listOf("📱 Live Interactive Post", "📄 Archive Snapshot")
                    tabLabels.forEachIndexed { index, label ->
                        val isSelected = selectedTab == index
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(9.dp))
                                .clickable { selectedTab = index },
                            color = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                            shape = RoundedCornerShape(9.dp),
                            shadowElevation = if (isSelected) 2.dp else 0.dp
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (selectedTab == 0) {
                    InPlaceMediaViewer(
                        item = item,
                        onCopyUrl = { copyToClipboard(context, "Link", it) }
                    )
                } else {
                    ItemPreviewCard(
                        item = item,
                        onOpenImage = { showFullImageViewer = true },
                        onOpenPdf = { openPdfFile(context, item.localFilePath) },
                        onOpenUrl = { openBrowserUrl(context, item.originalUrl) },
                        onCopyText = { copyToClipboard(context, "Note", item.originalText ?: item.extractedText.orEmpty()) }
                    )
                }
            } else {
                ItemPreviewCard(
                    item = item,
                    onOpenImage = { showFullImageViewer = true },
                    onOpenPdf = { openPdfFile(context, item.localFilePath) },
                    onOpenUrl = { openBrowserUrl(context, item.originalUrl) },
                    onCopyText = { copyToClipboard(context, "Note", item.originalText ?: item.extractedText.orEmpty()) }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            FollowUpSection(
                remindAt = item.remindAt,
                completedAt = item.completedAt,
                onPickPreset = { viewModel.setReminder(it) },
                onClearReminder = { viewModel.clearReminder() },
                onToggleCompleted = { viewModel.toggleCompleted() }
            )

            // Materialized Threaded Comments & Discussion Section
            if (uiState.commentsTree.isNotEmpty() || item.comments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                ThreadedCommentTreeSection(
                    commentsTree = uiState.commentsTree,
                    legacyComments = item.comments,
                    onCopyComment = { text -> copyToClipboard(context, "Comment", text) }
                )
            }

            // Detected Entities Section
            if (item.entities.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Detected Entities & Actions",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item.entities.forEach { entity ->
                        EntityActionChip(entity = entity)
                    }
                }
            }

            // Collections & Tags Section
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Collections & Tags",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(onClick = { showCollectionDialog = true }) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Manage")
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item.collections.forEach { col ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    ) {
                        Text(
                            text = col.name,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
                item.tags.forEach { tag ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "#${tag.name}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Extracted OCR / Text Content Section
            val allExtracted = item.ocrText ?: item.extractedText
            if (!allExtracted.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (!item.ocrText.isNullOrBlank()) "On-Device OCR Text" else "Extracted Content",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = { copyToClipboard(context, "Extracted Text", allExtracted) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = "Copy Text",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    )
                ) {
                    Text(
                        text = allExtracted,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            // 7. Personal Notes (User Note & Capture Note)
            var userNoteText by remember(item.userNote) { mutableStateOf(item.userNote ?: "") }
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Personal Notes",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (userNoteText != (item.userNote ?: "")) {
                    TextButton(onClick = { viewModel.saveUserNote(userNoteText) }) {
                        Text("Save Note", fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = userNoteText,
                onValueChange = { userNoteText = it },
                placeholder = { Text("Add your thoughts, key takeaways, or follow-ups...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 8,
                shape = RoundedCornerShape(12.dp)
            )

            // 8. Related Saves in Vault (Memory Engine)
            if (uiState.relatedItems.isNotEmpty()) {
                Spacer(modifier = Modifier.height(28.dp))
                Text(
                    text = "Related Saves in Your Vault",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    uiState.relatedItems.forEach { related ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToDetail?.invoke(related.id) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = related.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = related.sourceDomain ?: related.contentType.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }

    // Full screen image dialog viewer
    if (showFullImageViewer && item.localFilePath != null) {
        Dialog(onDismissRequest = { showFullImageViewer = false }) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { showFullImageViewer = false },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = File(item.localFilePath),
                    contentDescription = "Full image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Move to Trash?") },
            text = { Text("This item will be moved to Trash. You can restore it later or permanently delete it.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        viewModel.moveToTrash()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TuckTheme.colors.destructive)
                ) {
                    Text("Move to Trash")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Manage Collections Dialog
    if (showCollectionDialog) {
        AlertDialog(
            onDismissRequest = { showCollectionDialog = false },
            title = { Text("Add to Collections") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.allCollections.forEach { collection ->
                        val isMember = item.collections.any { it.id == collection.id }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.toggleCollectionMembership(collection.id, isMember)
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isMember,
                                onCheckedChange = {
                                    viewModel.toggleCollectionMembership(collection.id, isMember)
                                },
                                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = collection.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showCollectionDialog = false }) {
                    Text("Done")
                }
            }
        )
    }
}

@Composable
fun DetailTopBar(
    item: SavedItem,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onBack) {
            Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (item.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = "Favorite",
                    tint = if (item.isFavorite) TuckTheme.colors.favorite else MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = onShare) {
                Icon(imageVector = Icons.Filled.Share, contentDescription = "Share")
            }
            IconButton(onClick = onDelete) {
                Icon(imageVector = Icons.Filled.Delete, contentDescription = "Delete", tint = TuckTheme.colors.destructive)
            }
        }
    }
}

@Composable
fun ItemPreviewCard(
    item: SavedItem,
    onOpenImage: () -> Unit,
    onOpenPdf: () -> Unit,
    onOpenUrl: () -> Unit,
    onCopyText: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            when (item.contentType) {
                ContentType.IMAGE, ContentType.MULTI_IMAGE -> {
                    if (item.localFilePath != null) {
                        AsyncImage(
                            model = File(item.localFilePath),
                            contentDescription = "Image preview",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                                .clickable { onOpenImage() },
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                ContentType.PDF -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (item.thumbnailPath != null) {
                            AsyncImage(
                                model = File(item.thumbnailPath),
                                contentDescription = "PDF page 1 preview",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                        }
                        Button(
                            onClick = onOpenPdf,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Filled.PictureAsPdf, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open PDF in Viewer")
                        }
                    }
                }

                ContentType.URL, ContentType.VIDEO -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // 1. Hero Cover Thumbnail Preview
                        val previewImage = item.thumbnailPath ?: item.localFilePath
                        if (!previewImage.isNullOrBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp)
                                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { onOpenUrl() },
                                contentAlignment = Alignment.Center
                            ) {
                                val imageModel: Any = if (previewImage.startsWith("http://") || previewImage.startsWith("https://")) {
                                    previewImage
                                } else {
                                    File(previewImage)
                                }
                                AsyncImage(
                                    model = imageModel,
                                    contentDescription = "Cover preview",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )

                                // Video / Reel Play Button Overlay
                                val isVideoContent = item.contentType == ContentType.VIDEO ||
                                        item.sourceDomain?.contains("Instagram", ignoreCase = true) == true ||
                                        item.sourceDomain?.contains("YouTube", ignoreCase = true) == true ||
                                        item.sourceDomain?.contains("TikTok", ignoreCase = true) == true

                                if (isVideoContent) {
                                    Surface(
                                        shape = CircleShape,
                                        color = TuckTheme.colors.scrim.copy(alpha = 0.65f),
                                        modifier = Modifier.size(54.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Filled.PlayArrow,
                                                contentDescription = "Play",
                                                tint = TuckTheme.colors.onScrim,
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 2. Caption / Content and Action Buttons
                        Column(modifier = Modifier.padding(18.dp)) {
                            if (!item.description.isNullOrBlank()) {
                                Text(
                                    text = item.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 22.sp
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                            }

                            item.originalUrl?.let { url ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = url,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val buttonLabel = when {
                                    item.sourceDomain?.contains("LinkedIn", ignoreCase = true) == true || item.originalUrl?.contains("linkedin.com") == true -> "Open on LinkedIn"
                                    item.sourceDomain?.contains("Instagram", ignoreCase = true) == true || item.originalUrl?.contains("instagram.com") == true -> "Watch on Instagram"
                                    item.sourceDomain?.contains("Reddit", ignoreCase = true) == true || item.originalUrl?.contains("reddit.com") == true -> "Open on Reddit"
                                    item.sourceDomain?.contains("YouTube", ignoreCase = true) == true || item.originalUrl?.contains("youtube") == true || item.originalUrl?.contains("youtu.be") == true -> "Watch on YouTube"
                                    item.sourceDomain?.contains("TikTok", ignoreCase = true) == true || item.originalUrl?.contains("tiktok.com") == true -> "Watch on TikTok"
                                    item.sourceDomain?.contains("Twitter", ignoreCase = true) == true || item.originalUrl?.contains("twitter.com") == true || item.originalUrl?.contains("x.com") == true -> "Open on X"
                                    else -> "Open in Browser"
                                }

                                Button(
                                    onClick = onOpenUrl,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(imageVector = Icons.Filled.OpenInBrowser, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(buttonLabel)
                                }

                                OutlinedButton(
                                    onClick = {
                                        val textToCopy = item.originalUrl ?: item.description.orEmpty()
                                        onCopyText()
                                    },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(imageVector = Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Copy")
                                }
                            }
                        }
                    }
                }

                ContentType.TEXT -> {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = item.originalText.orEmpty(),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedButton(
                            onClick = onCopyText,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy Note")
                        }
                    }
                }

                else -> {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = item.displaySnippet,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

fun shareItem(context: Context, item: SavedItem) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        if (!item.originalUrl.isNullOrBlank()) {
            putExtra(Intent.EXTRA_TEXT, item.originalUrl)
            type = "text/plain"
        } else if (!item.originalText.isNullOrBlank()) {
            putExtra(Intent.EXTRA_TEXT, item.originalText)
            type = "text/plain"
        } else if (item.localFilePath != null) {
            val file = File(item.localFilePath)
            val uri = FileProvider.getUriForFile(context, "com.tuck.app.fileprovider", file)
            putExtra(Intent.EXTRA_STREAM, uri)
            type = item.mimeType ?: "*/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
    context.startActivitySafe(Intent.createChooser(sendIntent, "Share via"))
}

fun openPdfFile(context: Context, filePath: String?) {
    if (filePath.isNullOrBlank()) return
    try {
        val file = File(filePath)
        val uri = FileProvider.getUriForFile(context, "com.tuck.app.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "No PDF viewer app found", Toast.LENGTH_SHORT).show()
    }
}

fun openBrowserUrl(context: Context, url: String?) {
    if (url.isNullOrBlank()) return
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    context.startActivitySafe(intent)
}

fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
}

@Composable
fun ThreadedCommentTreeSection(
    commentsTree: List<com.tuck.app.data.local.db.entity.SourceCommentEntity>,
    legacyComments: List<com.tuck.app.domain.model.SavedComment>,
    onCopyComment: (String) -> Unit
) {
    val totalCount = if (commentsTree.isNotEmpty()) commentsTree.size else legacyComments.size
    var isExpanded by remember { mutableStateOf(false) }
    var collapsedPaths by remember { mutableStateOf(setOf<String>()) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "💬 Threaded Discussion ($totalCount)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (totalCount > 4) {
                    Text(
                        text = if (isExpanded) "Show Top Only" else "Show Full Tree ($totalCount)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { isExpanded = !isExpanded }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (commentsTree.isNotEmpty()) {
                val visibleComments = if (isExpanded) {
                    commentsTree.filter { comment ->
                        // Hide if any parent path is in collapsedPaths
                        val parts = comment.path.split(".")
                        var isParentCollapsed = false
                        for (i in 1 until parts.size) {
                            val subPath = parts.take(i).joinToString(".")
                            if (collapsedPaths.contains(subPath)) {
                                isParentCollapsed = true
                                break
                            }
                        }
                        !isParentCollapsed
                    }
                } else {
                    commentsTree.take(4)
                }

                visibleComments.forEachIndexed { index, comment ->
                    if (index > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    val isCollapsed = collapsedPaths.contains(comment.path)
                    val indent = (comment.depth.coerceAtMost(5) * 14).dp

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = indent)
                    ) {
                        // Depth guide vertical bar if nested
                        if (comment.depth > 0) {
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height(48.dp)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    if (comment.childCount > 0) {
                                        collapsedPaths = if (isCollapsed) {
                                            collapsedPaths - comment.path
                                        } else {
                                            collapsedPaths + comment.path
                                        }
                                    }
                                }
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = comment.authorHandle ?: "anonymous",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (comment.isOp) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                                        )

                                        if (comment.isOp) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = "OP",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.tertiary,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }

                                        if (comment.score > 0) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                            ) {
                                                Text(
                                                    text = "▲ ${comment.score}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        if (comment.childCount > 0) {
                                            Text(
                                                text = if (isCollapsed) "[+${comment.childCount}]" else "[-]",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { onCopyComment(comment.bodyText) },
                                        modifier = Modifier.size(22.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.ContentCopy,
                                            contentDescription = "Copy comment",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }

                                if (!isCollapsed) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = comment.bodyText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        lineHeight = 17.sp
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Fallback to legacy flat list if source_comments not yet populated
                val displayComments = if (isExpanded) legacyComments else legacyComments.take(3)
                displayComments.forEachIndexed { index, comment ->
                    if (index > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = comment.author,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                IconButton(
                                    onClick = { onCopyComment(comment.text) },
                                    modifier = Modifier.size(22.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ContentCopy,
                                        contentDescription = "Copy comment",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = comment.text,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Turns a save into something with a future: a reminder to bring it back, and a way to
 * mark it dealt with so it stops counting as outstanding.
 *
 * The presets are deliberately coarse rather than a date-time picker - "tomorrow
 * morning" is what people mean, and every extra tap here is a reason not to bother.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FollowUpSection(
    remindAt: Long?,
    completedAt: Long?,
    onPickPreset: (ReminderPreset) -> Unit,
    onClearReminder: () -> Unit,
    onToggleCompleted: () -> Unit
) {
    val tuckColors = TuckTheme.colors
    val isCompleted = completedAt != null
    var isPicking by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "FOLLOW UP",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.2.sp,
            color = tuckColors.textMuted
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            FilterChip(
                selected = isCompleted,
                onClick = onToggleCompleted,
                label = { Text(if (isCompleted) "Done" else "Mark done") }
            )

            Spacer(modifier = Modifier.width(8.dp))

            if (remindAt != null) {
                AssistChip(
                    onClick = onClearReminder,
                    label = {
                        Text(
                            "Reminder " + android.text.format.DateUtils.getRelativeTimeSpanString(
                                remindAt,
                                System.currentTimeMillis(),
                                android.text.format.DateUtils.MINUTE_IN_MILLIS
                            )
                        )
                    },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Clear reminder",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            } else if (!isCompleted) {
                AssistChip(
                    onClick = { isPicking = !isPicking },
                    label = { Text("Remind me") }
                )
            }
        }

        if (isPicking && remindAt == null && !isCompleted) {
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    ReminderPreset.LATER_TODAY to "Later today",
                    ReminderPreset.TOMORROW_MORNING to "Tomorrow",
                    ReminderPreset.THIS_WEEKEND to "This weekend",
                    ReminderPreset.NEXT_WEEK to "Next week"
                ).forEach { (preset, label) ->
                    AssistChip(
                        onClick = {
                            onPickPreset(preset)
                            isPicking = false
                        },
                        label = { Text(label) }
                    )
                }
            }
        }
    }
}

/**
 * Find within one saved item.
 *
 * Search finds the item; this finds the line inside it. Tuck stores whole articles, pages
 * of recognised text and long comment threads, so once an item is longer than a screen
 * "it's in here somewhere" becomes its own problem.
 *
 * Collapsed to a single row until used, because most visits to an item are not searches.
 */
@Composable
private fun FindWithinItem(
    item: com.tuck.app.domain.model.SavedItem,
    commentsTree: List<com.tuck.app.data.local.db.entity.SourceCommentEntity>,
    query: String,
    onQueryChange: (String) -> Unit,
    matchIndex: Int,
    onStep: (forward: Boolean, total: Int) -> Unit,
    onJumpTo: (blockId: String) -> Unit
) {
    val tuckColors = TuckTheme.colors
    var isOpen by remember { mutableStateOf(false) }

    val blocks: List<SearchableBlock> = remember(item, commentsTree) {
        buildList<SearchableBlock> {
            listOfNotNull(item.originalText, item.extractedText).firstOrNull()?.let {
                add(SearchableBlock(BLOCK_CONTENT, "Content", 0, it))
            }
            item.ocrText?.takeIf { it.isNotBlank() }?.let {
                add(SearchableBlock(BLOCK_OCR, "Recognised text", 1, it))
            }
            item.userNote?.takeIf { it.isNotBlank() }?.let {
                add(SearchableBlock(BLOCK_NOTES, "Your notes", 2, it))
            }
            if (commentsTree.isNotEmpty()) {
                add(
                    SearchableBlock(
                        BLOCK_COMMENTS,
                        "Comments",
                        3,
                        commentsTree.joinToString(" ") { it.bodyText }
                    )
                )
            }
        }
    }

    // Nothing to search through: an item with only a title does not need this control.
    if (blocks.isEmpty()) return

    val matches = remember(blocks, query) { InItemSearch.find(blocks, query) }
    val current = matches.getOrNull(matchIndex)

    LaunchedEffect(current) {
        current?.let { onJumpTo(it.blockId) }
    }

    if (!isOpen) {
        TextButton(onClick = { isOpen = true }, contentPadding = PaddingValues(0.dp)) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = tuckColors.textSecondary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text("Find in this item", style = MaterialTheme.typography.labelLarge, color = tuckColors.textSecondary)
        }
        return
    }

    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Find in this item") },
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = {
                    onQueryChange("")
                    isOpen = false
                }) {
                    Icon(Icons.Filled.Close, contentDescription = "Close find")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        if (query.isNotBlank()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when {
                        matches.isEmpty() -> "No matches in this item"
                        else -> "${matchIndex + 1} of ${matches.size} · in ${current?.blockLabel.orEmpty()}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (matches.isEmpty()) tuckColors.textMuted else tuckColors.textSecondary,
                    modifier = Modifier.weight(1f)
                )

                if (matches.isNotEmpty()) {
                    IconButton(onClick = { onStep(false, matches.size) }) {
                        Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Previous match")
                    }
                    IconButton(onClick = { onStep(true, matches.size) }) {
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Next match")
                    }
                }
            }
        }
    }
}

private const val BLOCK_CONTENT = "content"
private const val BLOCK_OCR = "ocr"
private const val BLOCK_NOTES = "notes"
private const val BLOCK_COMMENTS = "comments"
