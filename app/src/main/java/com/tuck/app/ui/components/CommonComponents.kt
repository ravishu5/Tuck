package com.tuck.app.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tuck.app.domain.model.ContentType
import com.tuck.app.domain.model.EntityType
import com.tuck.app.domain.model.ExtractedEntity
import com.tuck.app.domain.model.ProcessingStatus
import com.tuck.app.domain.model.SavedItem
import com.tuck.app.ui.theme.AccentAmber
import com.tuck.app.ui.theme.AccentEmerald
import com.tuck.app.ui.theme.AccentOrange
import com.tuck.app.ui.theme.AccentRose
import com.tuck.app.ui.theme.AccentSky
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun SavedItemCard(
    item: SavedItem,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
    highlightSnippet: String? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: ContentType badge, Source, Relative Time, Favorite Star
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    ContentTypeBadge(contentType = item.contentType)
                    Spacer(modifier = Modifier.width(8.dp))

                    val sourceText = item.sourceDomain ?: item.sourceApp ?: item.contentType.displayName
                    Text(
                        text = "$sourceText · ${formatRelativeTime(item.createdAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.processingStatus == ProcessingStatus.PROCESSING || item.processingStatus == ProcessingStatus.PENDING) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (item.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = if (item.isFavorite) "Unfavorite" else "Favorite",
                            tint = if (item.isFavorite) AccentAmber else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Body Row: Title + Thumbnail Preview
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = item.displayTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    val snippet = highlightSnippet ?: item.displaySnippet
                    if (snippet.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = snippet,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Thumbnail if available
                val previewImage = item.thumbnailPath ?: item.localFilePath
                if (!previewImage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        val imageModel: Any = if (previewImage.startsWith("http://") || previewImage.startsWith("https://")) {
                            previewImage
                        } else {
                            File(previewImage)
                        }
                        AsyncImage(
                            model = imageModel,
                            contentDescription = "Item preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        if (item.contentType == ContentType.VIDEO) {
                            Surface(
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.6f),
                                modifier = Modifier.size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.PlayArrow,
                                        contentDescription = "Play",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Detected Entities summary tags (URLs, Phone, Price, etc.)
            if (item.entities.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item.entities.take(3).forEach { entity ->
                        CompactEntityPill(entity = entity)
                    }
                }
            }
        }
    }
}

@Composable
fun ContentTypeBadge(contentType: ContentType) {
    val (icon, bgColor, tintColor) = when (contentType) {
        ContentType.URL -> Triple(Icons.Filled.Link, AccentSky.copy(alpha = 0.15f), AccentSky)
        ContentType.TEXT -> Triple(Icons.Filled.Notes, AccentAmber.copy(alpha = 0.15f), AccentAmber)
        ContentType.IMAGE, ContentType.MULTI_IMAGE -> Triple(Icons.Filled.Image, AccentRose.copy(alpha = 0.15f), AccentRose)
        ContentType.PDF -> Triple(Icons.Filled.PictureAsPdf, AccentOrange.copy(alpha = 0.15f), AccentOrange)
        ContentType.VIDEO -> Triple(Icons.Filled.Movie, AccentRose.copy(alpha = 0.15f), AccentRose)
        ContentType.DOCUMENT -> Triple(Icons.Filled.Description, AccentEmerald.copy(alpha = 0.15f), AccentEmerald)
        ContentType.CONTACT -> Triple(Icons.Filled.Email, AccentAmber.copy(alpha = 0.15f), AccentAmber)
        ContentType.LOCATION -> Triple(Icons.Filled.ArrowOutward, AccentEmerald.copy(alpha = 0.15f), AccentEmerald)
        else -> Triple(Icons.Filled.Language, Color.Gray.copy(alpha = 0.15f), Color.Gray)
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tintColor,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = contentType.displayName,
                style = MaterialTheme.typography.labelSmall,
                color = tintColor,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun CompactEntityPill(entity: ExtractedEntity) {
    val (icon, tint) = when (entity.type) {
        EntityType.MONEY -> Icons.Filled.AttachMoney to AccentEmerald
        EntityType.PHONE -> Icons.Filled.Call to AccentSky
        EntityType.EMAIL -> Icons.Filled.Email to AccentAmber
        EntityType.URL -> Icons.Filled.ArrowOutward to AccentSky
        EntityType.DATE -> Icons.Filled.CalendarToday to AccentOrange
        EntityType.HASHTAG -> Icons.Filled.Tag to AccentRose
        else -> Icons.Filled.Tag to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(11.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = entity.value.take(20),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun EntityActionChip(
    entity: ExtractedEntity,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val (icon, action) = when (entity.type) {
        EntityType.PHONE -> Icons.Filled.Call to {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${entity.normalizedValue}"))
            context.startActivitySafe(intent)
        }
        EntityType.EMAIL -> Icons.Filled.Email to {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${entity.normalizedValue}"))
            context.startActivitySafe(intent)
        }
        EntityType.URL -> Icons.Filled.ArrowOutward to {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(entity.value))
            context.startActivitySafe(intent)
        }
        EntityType.MONEY -> Icons.Filled.AttachMoney to null
        EntityType.DATE -> Icons.Filled.CalendarToday to null
        EntityType.HASHTAG -> Icons.Filled.Tag to null
        else -> Icons.Filled.Tag to null
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = action != null) { action?.invoke() },
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = entity.value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun EmptyState(
    title: String,
    description: String,
    icon: ImageVector = Icons.Filled.Description,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

fun formatRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val seconds = TimeUnit.MILLISECONDS.toSeconds(diff)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
    val hours = TimeUnit.MILLISECONDS.toHours(diff)
    val days = TimeUnit.MILLISECONDS.toDays(diff)

    return when {
        seconds < 60 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days == 1L -> "Yesterday"
        days < 7 -> "${days}d ago"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
    }
}

fun Context.startActivitySafe(intent: Intent) {
    try {
        startActivity(intent)
    } catch (e: Exception) {
        // Safe fallback if no app handles intent
    }
}
