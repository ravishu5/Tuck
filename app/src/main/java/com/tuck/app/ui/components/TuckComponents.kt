package com.tuck.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tuck.app.domain.model.ContentType
import com.tuck.app.domain.model.SavedItem
import com.tuck.app.domain.repository.TuckThemeFlavor
import com.tuck.app.ui.theme.TuckTheme
import com.tuck.app.ui.theme.getTuckColors
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

// 1. Search Bar Component
@Composable
fun TuckSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search anything...",
    focusRequester: FocusRequester? = null,
    onClear: (() -> Unit)? = null
) {
    val tuckColors = TuckTheme.colors
    val tuckShapes = TuckTheme.shapes

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = {
            Text(
                text = placeholder,
                color = tuckColors.textMuted,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = "Search",
                tint = tuckColors.accent,
                modifier = Modifier.size(20.dp)
            )
        },
        trailingIcon = {
            if (query.isNotEmpty() && onClear != null) {
                IconButton(onClick = onClear) {
                    Icon(
                        imageVector = Icons.Filled.Clear,
                        contentDescription = "Clear",
                        tint = tuckColors.textMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        singleLine = true,
        shape = tuckShapes.medium,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = tuckColors.surface,
            unfocusedContainerColor = tuckColors.surfaceCard,
            focusedBorderColor = tuckColors.accent,
            unfocusedBorderColor = tuckColors.border,
            focusedTextColor = tuckColors.textPrimary,
            unfocusedTextColor = tuckColors.textPrimary,
            cursorColor = tuckColors.accent
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        modifier = modifier
            .fillMaxWidth()
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
    )
}

// 2. Platform Brand Badge
@Composable
fun TuckPlatformBadge(
    item: SavedItem,
    modifier: Modifier = Modifier
) {
    val tuckColors = TuckTheme.colors
    val tuckShapes = TuckTheme.shapes
    val domain = (item.sourceDomain ?: "").lowercase()

    val (badgeText, badgeColor, icon) = when {
        domain.contains("linkedin") -> Triple("LinkedIn", Color(0xFF0A66C2), Icons.Filled.Language)
        domain.contains("instagram") -> Triple("Instagram", Color(0xFFE4405F), Icons.Filled.Videocam)
        domain.contains("reddit") || domain.startsWith("r/") -> Triple("Reddit", Color(0xFFFF4500), Icons.Filled.Message)
        domain.contains("youtube") || domain.contains("youtu.be") -> Triple("YouTube", Color(0xFFFF0000), Icons.Filled.PlayArrow)
        domain.contains("twitter") || domain.contains("x.com") -> Triple("X", Color(0xFF1DA1F2), Icons.Filled.Language)
        item.contentType == ContentType.IMAGE -> Triple("Screenshot", Color(0xFF10B981), Icons.Filled.Image)
        item.contentType == ContentType.PDF -> Triple("PDF", Color(0xFFEF4444), Icons.Filled.PictureAsPdf)
        item.contentType == ContentType.TEXT -> Triple("Note", Color(0xFF8B5CF6), Icons.Filled.Notes)
        else -> Triple(item.sourceDomain ?: "Web", tuckColors.accent, Icons.Filled.Language)
    }

    Surface(
        color = badgeColor.copy(alpha = 0.12f),
        shape = tuckShapes.small,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = badgeColor,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = badgeText,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = badgeColor
            )
        }
    }
}

// 3. Category Chip
@Composable
fun TuckCategoryChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: String? = null
) {
    val tuckColors = TuckTheme.colors
    val tuckShapes = TuckTheme.shapes

    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) tuckColors.accent else tuckColors.surfaceCard,
        label = "chipBg"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) tuckColors.textOnAccent else tuckColors.textPrimary,
        label = "chipText"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) tuckColors.accent else tuckColors.border,
        label = "chipBorder"
    )

    Surface(
        color = backgroundColor,
        shape = tuckShapes.pill,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        modifier = modifier
            .clip(tuckShapes.pill)
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            if (!icon.isNullOrBlank()) {
                Text(text = icon, fontSize = 13.sp)
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = textColor
            )
        }
    }
}

// 4. Category Card
@Composable
fun TuckCategoryCard(
    name: String,
    count: Int,
    icon: String?,
    isAutoGenerated: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tuckColors = TuckTheme.colors
    val tuckShapes = TuckTheme.shapes

    Card(
        shape = tuckShapes.medium,
        colors = CardDefaults.cardColors(containerColor = tuckColors.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, tuckColors.border),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(tuckColors.accentContainer),
                    contentAlignment = Alignment.Center
                ) {
                    if (!icon.isNullOrBlank()) {
                        Text(text = icon, fontSize = 18.sp)
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Folder,
                            contentDescription = null,
                            tint = tuckColors.accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = tuckColors.textPrimary
                    )
                    Text(
                        text = "$count ${if (count == 1) "item" else "items"}${if (isAutoGenerated) " • Auto" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = tuckColors.textMuted
                    )
                }
            }

            Icon(
                imageVector = Icons.Filled.ArrowForward,
                contentDescription = null,
                tint = tuckColors.textMuted,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// 5. Empty State
@Composable
fun TuckEmptyState(
    title: String,
    description: String,
    icon: ImageVector = Icons.Filled.Folder,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val tuckColors = TuckTheme.colors

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(tuckColors.accentContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tuckColors.accent,
                modifier = Modifier.size(30.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = tuckColors.textPrimary
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = tuckColors.textSecondary,
            lineHeight = 20.sp
        )

        if (!actionLabel.isNullOrBlank() && onAction != null) {
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onAction,
                shape = TuckTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = tuckColors.accent,
                    contentColor = tuckColors.textOnAccent
                )
            ) {
                Text(text = actionLabel, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// 6. Section Header
@Composable
fun TuckSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    val tuckColors = TuckTheme.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = tuckColors.textPrimary
        )

        if (!actionText.isNullOrBlank() && onAction != null) {
            Text(
                text = actionText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = tuckColors.accent,
                modifier = Modifier.clickable(onClick = onAction)
            )
        }
    }
}

// 7. Platform-Aware Content Card
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TuckContentCard(
    item: SavedItem,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
    highlightSnippet: String? = null
) {
    val tuckColors = TuckTheme.colors
    val tuckShapes = TuckTheme.shapes
    val domain = (item.sourceDomain ?: "").lowercase()

    val formattedDate = formatRelativeTime(item.createdAt)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(tuckShapes.medium)
            .clickable(onClick = onClick),
        shape = tuckShapes.medium,
        colors = CardDefaults.cardColors(containerColor = tuckColors.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, tuckColors.border)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Media Preview (YouTube / Reel / Screenshot / Image)
            val previewImage = item.thumbnailPath ?: item.localFilePath
            if (!previewImage.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .background(tuckColors.surfaceCard)
                ) {
                    val model: Any = if (previewImage.startsWith("/") || previewImage.startsWith("file://")) {
                        File(previewImage.removePrefix("file://"))
                    } else {
                        previewImage
                    }

                    AsyncImage(
                        model = model,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Video Play Icon Overlay
                    if (item.contentType == ContentType.VIDEO || domain.contains("youtube") || domain.contains("instagram")) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.6f))
                                .align(Alignment.Center),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // OCR Screenshot Badge
                    if (item.contentType == ContentType.IMAGE && !item.ocrText.isNullOrBlank()) {
                        Surface(
                            shape = tuckShapes.small,
                            color = Color.Black.copy(alpha = 0.7f),
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "📸 OCR Indexed",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Card Body Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                // Header Meta Row: Badge + Date + Favorite
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TuckPlatformBadge(item = item)

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.bodySmall,
                            color = tuckColors.textMuted
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (item.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                contentDescription = "Favorite",
                                tint = if (item.isFavorite) Color(0xFFF59E0B) else tuckColors.textMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Title
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = tuckColors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Highlighted Search Snippet OR Description
                if (!highlightSnippet.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = tuckColors.accentContainer.copy(alpha = 0.5f),
                        shape = tuckShapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = highlightSnippet,
                            style = MaterialTheme.typography.bodySmall,
                            color = tuckColors.accent,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                } else if (!item.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.description ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = tuckColors.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Collections & Comments Meta Row
                val hasCollections = item.collections.isNotEmpty()
                val hasComments = item.comments.isNotEmpty()
                if (hasCollections || hasComments) {
                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        item.collections.take(2).forEach { col ->
                            Surface(
                                shape = tuckShapes.small,
                                color = tuckColors.surfaceCard
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Folder,
                                        contentDescription = null,
                                        tint = tuckColors.accent,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = col.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = tuckColors.textSecondary
                                    )
                                }
                            }
                        }

                        if (hasComments) {
                            Surface(
                                shape = tuckShapes.small,
                                color = Color(0xFF06B6D4).copy(alpha = 0.12f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Message,
                                        contentDescription = null,
                                        tint = Color(0xFF06B6D4),
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${item.comments.size} comments",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF06B6D4)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// 8. Theme Preview Card Component
@Composable
fun TuckThemePreviewCard(
    flavor: TuckThemeFlavor,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val previewColors = getTuckColors(flavor, isDark = false)
    val shapes = TuckTheme.shapes

    Card(
        shape = shapes.large,
        colors = CardDefaults.cardColors(containerColor = previewColors.background),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.5.dp else 1.dp,
            color = if (isSelected) TuckTheme.colors.accent else previewColors.border
        ),
        modifier = modifier
            .fillMaxWidth()
            .clip(shapes.large)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Theme Title + Selected Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = flavor.displayName.uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = previewColors.textPrimary
                    )
                    Text(
                        text = flavor.tagline,
                        style = MaterialTheme.typography.bodySmall,
                        color = previewColors.textSecondary
                    )
                }

                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(TuckTheme.colors.accent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = "Selected",
                            tint = TuckTheme.colors.textOnAccent,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Miniature Search Bar
            Surface(
                shape = shapes.small,
                color = previewColors.surfaceCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, previewColors.border),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = previewColors.accent,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Search anything...",
                        style = MaterialTheme.typography.labelSmall,
                        color = previewColors.textMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Miniature Content Card + Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = shapes.small,
                    color = previewColors.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, previewColors.border),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(previewColors.accent)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Recently tucked",
                            style = MaterialTheme.typography.labelSmall,
                            color = previewColors.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Surface(
                    shape = shapes.small,
                    color = previewColors.accent,
                    modifier = Modifier.height(36.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    ) {
                        Text(
                            text = "Tuck",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = previewColors.textOnAccent
                        )
                    }
                }
            }
        }
    }
}

