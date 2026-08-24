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
import com.tuck.app.ui.theme.AccentAmber
import com.tuck.app.ui.theme.AccentRose
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ItemDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: ItemDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var showCollectionDialog by remember { mutableStateOf(false) }
    var showFullImageViewer by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

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

            // Saved Top Comments & Discussion Section
            if (item.comments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                SavedCommentsSection(
                    comments = item.comments,
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
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRose)
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
                    tint = if (item.isFavorite) AccentAmber else MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = onShare) {
                Icon(imageVector = Icons.Filled.Share, contentDescription = "Share")
            }
            IconButton(onClick = onDelete) {
                Icon(imageVector = Icons.Filled.Delete, contentDescription = "Delete", tint = AccentRose)
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
                                        color = Color.Black.copy(alpha = 0.65f),
                                        modifier = Modifier.size(54.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Filled.PlayArrow,
                                                contentDescription = "Play",
                                                tint = Color.White,
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
fun SavedCommentsSection(
    comments: List<com.tuck.app.domain.model.SavedComment>,
    onCopyComment: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val displayComments = if (expanded) comments else comments.take(3)

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
                    text = "💬 Community Comments (${comments.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (comments.size > 3) {
                    Text(
                        text = if (expanded) "Show Less" else "Show All (${comments.size})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { expanded = !expanded }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            displayComments.forEachIndexed { index, comment ->
                if (index > 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
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
                                    text = comment.author,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (comment.score != null && comment.score > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = "▲ ${comment.score}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            IconButton(
                                onClick = { onCopyComment(comment.text) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ContentCopy,
                                    contentDescription = "Copy comment",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = comment.text,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}
