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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.tuck.app.domain.model.ContentType
import com.tuck.app.domain.model.SavedItem
import com.tuck.app.domain.repository.TuckThemeFlavor
import com.tuck.app.ui.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import com.tuck.app.ui.theme.color.PaletteSlot
import com.tuck.app.ui.theme.pressScale
import com.tuck.app.ui.theme.TuckGradients
import androidx.compose.material.icons.filled.Lock
import androidx.compose.ui.draw.shadow
import androidx.compose.material3.ripple
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.foundation.layout.fillMaxHeight

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
        domain.contains("linkedin") -> Triple("LinkedIn", BrandLinkedIn, Icons.Filled.Language)
        domain.contains("instagram") -> Triple("Instagram", BrandInstagram, Icons.Filled.Videocam)
        domain.contains("reddit") || domain.startsWith("r/") -> Triple("Reddit", BrandReddit, Icons.Filled.Message)
        domain.contains("youtube") || domain.contains("youtu.be") -> Triple("YouTube", BrandYouTube, Icons.Filled.PlayArrow)
        domain.contains("twitter") || domain.contains("x.com") -> Triple("X", BrandTwitter, Icons.Filled.Language)
        domain.contains("github") -> Triple("GitHub", BrandGitHub, Icons.Filled.Code)
        domain.contains("medium.com") -> Triple("Medium", BrandMedium, Icons.Filled.Description)
        domain.contains("substack.com") -> Triple("Substack", BrandSubstack, Icons.Filled.Bookmark)
        domain.contains("arxiv.org") -> Triple("ArXiv", BrandArXiv, Icons.Filled.Description)
        domain.contains("wikipedia.org") -> Triple("Wikipedia", BrandWikipedia, Icons.Filled.Language)
        domain.contains("amazon.") -> Triple("Amazon", BrandAmazon, Icons.Filled.ShoppingCart)
        item.contentType == ContentType.IMAGE -> Triple("Screenshot", tuckColors.palette[PaletteSlot.SECONDARY_CORE].fill, Icons.Filled.Image)
        item.contentType == ContentType.PDF -> Triple("PDF", tuckColors.palette[PaletteSlot.PRIMARY_DEEP].fill, Icons.Filled.PictureAsPdf)
        item.contentType == ContentType.TEXT -> Triple("Note", tuckColors.palette[PaletteSlot.PRIMARY_CORE].fill, Icons.Filled.Notes)
        item.contentType == ContentType.CONTACT -> Triple("Contact", tuckColors.palette[PaletteSlot.SECONDARY_SOFT].fill, Icons.Filled.Description)
        item.contentType == ContentType.LOCATION -> Triple("Location", tuckColors.palette[PaletteSlot.SECONDARY_SOFT].fill, Icons.Filled.Language)
        item.contentType == ContentType.VIDEO -> Triple("Video", tuckColors.palette[PaletteSlot.TERTIARY_DEEP].fill, Icons.Filled.PlayArrow)
        else -> Triple(item.sourceDomain ?: item.contentType.displayName, tuckColors.accent, Icons.Filled.Language)
    }

    Surface(
        color = badgeColor.copy(alpha = 0.12f),
        shape = tuckShapes.small,
        border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.25f)),
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

// 3. Collection Visual Information Model
data class CollectionVisual(
    val icon: ImageVector,
    val color: Color,
    val emoji: String
)

@Composable
fun getCollectionVisual(name: String, iconHint: String? = null, colorHint: String? = null, collectionId: Long = 0): CollectionVisual {
    val tuckColors = TuckTheme.colors
    val lower = name.lowercase().trim()
    val hint = (iconHint ?: "").lowercase().trim()
    val fallbackColor = resolveCollectionColor(colorHint, name, collectionId).background

    return when {
        lower.contains("article") || hint == "article" -> 
            CollectionVisual(Icons.AutoMirrored.Filled.Article, if (colorHint != null) fallbackColor else tuckColors.palette[PaletteSlot.SECONDARY_SOFT].fill, "📰")
        lower.contains("education") || lower.contains("course") || hint == "school" || hint == "menu_book" -> 
            CollectionVisual(Icons.Filled.School, if (colorHint != null) fallbackColor else tuckColors.palette[PaletteSlot.SECONDARY_SOFT].fill, "🎓")
        lower.contains("finance") || lower.contains("money") || lower.contains("crypto") || hint == "attach_money" -> 
            CollectionVisual(Icons.Filled.AttachMoney, if (colorHint != null) fallbackColor else tuckColors.palette[PaletteSlot.SECONDARY_CORE].fill, "💰")
        lower.contains("programming") || lower.contains("code") || lower.contains("dev") || hint == "code" -> 
            CollectionVisual(Icons.Filled.Code, if (colorHint != null) fallbackColor else tuckColors.palette[PaletteSlot.PRIMARY_DEEP].fill, "💻")
        lower.contains("research") || lower.contains("paper") || hint == "science" -> 
            CollectionVisual(Icons.Filled.AutoAwesome, if (colorHint != null) fallbackColor else tuckColors.palette[PaletteSlot.PRIMARY_CORE].fill, "🔬")
        lower.contains("shopping") || lower.contains("product") || hint == "shopping_cart" -> 
            CollectionVisual(Icons.Filled.ShoppingCart, if (colorHint != null) fallbackColor else tuckColors.palette[PaletteSlot.TERTIARY_DEEP].fill, "🛍️")
        lower.contains("travel") || lower.contains("trip") || lower.contains("flight") || hint == "flight" -> 
            CollectionVisual(Icons.Filled.Flight, if (colorHint != null) fallbackColor else tuckColors.palette[PaletteSlot.PRIMARY_SOFT].fill, "✈️")
        lower.contains("food") || lower.contains("dining") || lower.contains("recipe") || hint == "restaurant" -> 
            CollectionVisual(Icons.Filled.Restaurant, if (colorHint != null) fallbackColor else tuckColors.palette[PaletteSlot.TERTIARY_CORE].fill, "🍽️")
        lower.contains("work") || hint == "work" -> 
            CollectionVisual(Icons.Filled.Work, if (colorHint != null) fallbackColor else tuckColors.textSecondary, "💼")
        lower.contains("personal") || hint == "person" -> 
            CollectionVisual(Icons.Filled.Person, if (colorHint != null) fallbackColor else tuckColors.palette[PaletteSlot.TERTIARY_DEEP].fill, "👤")
        lower.contains("video") || hint == "videocam" -> 
            CollectionVisual(Icons.Filled.Videocam, if (colorHint != null) fallbackColor else tuckColors.palette[PaletteSlot.TERTIARY_DEEP].fill, "🎬")
        lower.contains("image") || lower.contains("photo") || hint == "image" || hint == "photo_camera" -> 
            CollectionVisual(Icons.Filled.Image, if (colorHint != null) fallbackColor else tuckColors.palette[PaletteSlot.SECONDARY_CORE].fill, "🖼️")
        lower.contains("pdf") || hint == "picture_as_pdf" -> 
            CollectionVisual(Icons.Filled.PictureAsPdf, if (colorHint != null) fallbackColor else tuckColors.palette[PaletteSlot.TERTIARY_DEEP].fill, "📄")
        lower.contains("linkedin") -> 
            CollectionVisual(Icons.Filled.Language, BrandLinkedIn, "💼")
        lower.contains("instagram") -> 
            CollectionVisual(Icons.Filled.Videocam, BrandInstagram, "📱")
        lower.contains("reddit") -> 
            CollectionVisual(Icons.AutoMirrored.Filled.Message, BrandReddit, "💬")
        lower.contains("youtube") -> 
            CollectionVisual(Icons.Filled.PlayArrow, BrandYouTube, "▶️")
        lower.contains("twitter") || lower.contains("x") -> 
            CollectionVisual(Icons.Filled.Tag, BrandTwitter, "🐦")
        lower.contains("github") -> 
            CollectionVisual(Icons.Filled.Code, BrandGitHub, "🐙")
        lower.contains("note") -> 
            CollectionVisual(Icons.AutoMirrored.Filled.Notes, tuckColors.palette[PaletteSlot.PRIMARY_CORE].fill, "📝")
        lower.contains("screenshot") -> 
            CollectionVisual(Icons.Filled.CameraAlt, tuckColors.palette[PaletteSlot.PRIMARY_DEEP].fill, "📸")
        else -> 
            CollectionVisual(Icons.Filled.Folder, fallbackColor, "📁")
    }
}

// 4. Category Chip
@Composable
fun TuckCategoryChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: String? = null,
    color: String? = null
) {
    val tuckColors = TuckTheme.colors
    val tuckShapes = TuckTheme.shapes
    val visual = getCollectionVisual(label, icon, color)

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
    val iconTint = if (isSelected) tuckColors.textOnAccent else visual.color

    Surface(
        onClick = onClick,
        color = backgroundColor,
        shape = tuckShapes.pill,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = visual.icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = textColor
            )
        }
    }
}

// 5. Category Card
@Composable
fun TuckCategoryCard(
    name: String,
    count: Int,
    icon: String?,
    isAutoGenerated: Boolean,
    isLocked: Boolean = false,
    color: String? = null,
    collectionId: Long = 0,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tuckColors = TuckTheme.colors
    val tuckShapes = TuckTheme.shapes
    val visual = getCollectionVisual(name, icon, color, collectionId)

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
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(visual.color.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = visual.icon,
                        contentDescription = null,
                        tint = visual.color,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = tuckColors.textPrimary
                        )
                        if (isLocked) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "🔒", fontSize = 12.sp)
                        }
                    }
                    Text(
                        text = "$count ${if (count == 1) "item" else "items"}${if (isAutoGenerated) " • Smart Board" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = tuckColors.textMuted
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
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

// --- R3 Component Library Additions ---

@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = TuckTheme.colors.textSecondary
) {
    Text(
        text = text.uppercase(),
        style = TuckTheme.typography.sectionLabel,
        color = color,
        modifier = modifier
    )
}

@Composable
fun CountStat(
    savesCount: Int,
    foldersCount: Int,
    modifier: Modifier = Modifier
) {
    val tuckColors = TuckTheme.colors
    Text(
        text = "$savesCount ${if (savesCount == 1) "save" else "saves"} · $foldersCount ${if (foldersCount == 1) "folder" else "folders"}",
        style = TuckTheme.typography.numericCount,
        color = tuckColors.textSecondary,
        modifier = modifier
    )
}

@Composable
fun FilterPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    count: Int? = null,
    modifier: Modifier = Modifier
) {
    val tuckColors = TuckTheme.colors
    val shapes = TuckTheme.shapes

    val bg by animateColorAsState(
        targetValue = if (isSelected) tuckColors.textPrimary else tuckColors.surfaceCard,
        label = "filterPillBg"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) tuckColors.surface else tuckColors.textPrimary,
        label = "filterPillText"
    )

    Surface(
        color = bg,
        shape = shapes.pill,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) bg else tuckColors.borderSubtle),
        modifier = modifier
            .clip(shapes.pill)
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = textColor
            )
            if (count != null) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) textColor.copy(alpha = 0.8f) else tuckColors.textMuted
                )
            }
        }
    }
}

@Composable
fun CollectionTile(
    name: String,
    count: Int,
    colorId: String? = null,
    iconHint: String? = null,
    collectionId: Long = 0,
    isSelected: Boolean = false,
    isLocked: Boolean = false,
    openCount: Int = 0,
    previewPaths: List<String> = emptyList(),
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // A collection with content is identified by that content; an empty one has nothing to
    // show, so it falls back to its colour. The grid quietens as the library fills.
    if (previewPaths.isNotEmpty() && !isLocked) {
        CollectionPreviewTile(
            name = name,
            count = count,
            openCount = openCount,
            previewPaths = previewPaths,
            colorId = colorId,
            collectionId = collectionId,
            isSelected = isSelected,
            onClick = onClick,
            modifier = modifier
        )
    } else {
        CollectionColorTile(
            name = name,
            count = count,
            openCount = openCount,
            colorId = colorId,
            iconHint = iconHint,
            collectionId = collectionId,
            isSelected = isSelected,
            isLocked = isLocked,
            onClick = onClick,
            modifier = modifier
        )
    }
}

/**
 * A populated collection: one lead image with two stacked beneath it.
 *
 * Deliberately not three equal squares - an even row reads as a contact sheet and gives
 * the eye nowhere to land. A lead plus two supporting images reads as a composition, and
 * it stays balanced whether there are two previews or three.
 */
@Composable
private fun CollectionPreviewTile(
    name: String,
    count: Int,
    openCount: Int,
    previewPaths: List<String>,
    colorId: String?,
    collectionId: Long,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tuckColors = TuckTheme.colors
    val accentEntry = resolveCollectionColor(colorId, name, collectionId)
    val interactionSource = remember { MutableInteractionSource() }
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(interactionSource)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(22.dp),
                ambientColor = tuckColors.scrim.copy(alpha = 0.35f),
                spotColor = tuckColors.scrim.copy(alpha = 0.35f)
            )
            .clip(RoundedCornerShape(22.dp))
            .background(tuckColors.surface)
            .border(1.dp, tuckColors.borderSubtle, RoundedCornerShape(22.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = accentEntry.background),
                onClick = onClick
            )
            .padding(9.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(72.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            PreviewImage(
                path = previewPaths[0],
                context = context,
                fallback = accentEntry.background,
                modifier = Modifier.weight(1.9f).fillMaxHeight()
            )

            if (previewPaths.size > 1) {
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    PreviewImage(
                        path = previewPaths[1],
                        context = context,
                        fallback = accentEntry.background,
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    )
                    if (previewPaths.size > 2) {
                        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                            PreviewImage(
                                path = previewPaths[2],
                                context = context,
                                fallback = accentEntry.background,
                                modifier = Modifier.fillMaxSize()
                            )
                            // "+N" counts what the preview is not showing, so the tile
                            // says how much more is in there.
                            val hidden = count - previewPaths.size
                            if (hidden > 0) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(7.dp))
                                        .background(tuckColors.scrim.copy(alpha = 0.55f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "+$hidden",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = tuckColors.onScrim
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(9.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(accentEntry.background)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = name,
                style = TuckTheme.typography.tileTitle,
                color = tuckColors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (isSelected) {
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Selected",
                    tint = accentEntry.background,
                    modifier = Modifier.size(15.dp)
                )
            }
        }

        Text(
            text = collectionSubtitle(count, openCount),
            style = TuckTheme.typography.numericCount,
            color = tuckColors.textMuted
        )
    }
}

@Composable
private fun PreviewImage(
    path: String,
    context: android.content.Context,
    fallback: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    val request = remember(path) {
        val model: Any = when {
            path.startsWith("content://") -> Uri.parse(path)
            path.startsWith("/") || path.startsWith("file://") -> File(path.removePrefix("file://"))
            else -> path
        }
        ImageRequest.Builder(context).data(model).crossfade(true).build()
    }
    AsyncImage(
        model = request,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .clip(RoundedCornerShape(7.dp))
            .background(fallback.copy(alpha = 0.35f))
    )
}

/** An empty or locked collection, identified by its colour rather than its contents. */
@Composable
private fun CollectionColorTile(
    name: String,
    count: Int,
    openCount: Int,
    colorId: String?,
    iconHint: String?,
    collectionId: Long,
    isSelected: Boolean,
    isLocked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tuckColors = TuckTheme.colors
    val colorEntry = resolveCollectionColor(colorId, name, collectionId)
    val visual = getCollectionVisual(name, iconHint, colorId, collectionId)
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(interactionSource)
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(22.dp),
                ambientColor = colorEntry.background.copy(alpha = 0.5f),
                spotColor = colorEntry.background.copy(alpha = 0.5f)
            )
            .clip(RoundedCornerShape(22.dp))
            .background(TuckGradients.tile(colorEntry.background, tuckColors.isDark))
            .background(TuckGradients.sheen(colorEntry.foreground))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = colorEntry.foreground),
                onClick = onClick
            )
            .padding(15.dp)
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(colorEntry.foreground)
                    .align(Alignment.TopEnd),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Selected",
                    tint = colorEntry.background,
                    modifier = Modifier.size(14.dp)
                )
            }
        } else if (isLocked) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = "Locked",
                tint = colorEntry.foreground.copy(alpha = 0.75f),
                modifier = Modifier.size(15.dp).align(Alignment.TopEnd)
            )
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colorEntry.foreground.copy(alpha = 0.20f))
                    .border(1.dp, colorEntry.foreground.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = visual.icon,
                    contentDescription = name,
                    tint = colorEntry.foreground,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = name,
                style = TuckTheme.typography.tileTitle,
                color = colorEntry.foreground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = collectionSubtitle(count, openCount),
                style = TuckTheme.typography.numericCount,
                color = colorEntry.foreground.copy(alpha = 0.72f)
            )
        }
    }
}

/**
 * "18 items · 12 to read" rather than a bare number.
 *
 * The outstanding count is the whole point of the lifecycle work: it tells the user what
 * a collection still wants from them, which a total never does.
 */
private fun collectionSubtitle(count: Int, openCount: Int): String = when {
    count == 0 -> "Empty"
    openCount == 0 -> "$count ${if (count == 1) "item" else "items"} · all done"
    openCount == count -> "$count ${if (count == 1) "item" else "items"}"
    else -> "$count items · $openCount to go"
}

@Composable
fun MatchReasonChip(
    reason: String,
    modifier: Modifier = Modifier
) {
    val tuckColors = TuckTheme.colors
    Surface(
        color = tuckColors.highlight,
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
    ) {
        Text(
            text = reason,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = tuckColors.highlightText,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun SkeletonCard(
    modifier: Modifier = Modifier
) {
    val tuckColors = TuckTheme.colors
    val shapes = TuckTheme.shapes

    Card(
        shape = shapes.medium,
        colors = CardDefaults.cardColors(containerColor = tuckColors.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, tuckColors.borderSubtle),
        modifier = modifier
            .fillMaxWidth()
            .height(96.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(shapes.small)
                    .background(tuckColors.surfaceVariant)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(14.dp)
                        .clip(shapes.small)
                        .background(tuckColors.surfaceVariant)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(12.dp)
                        .clip(shapes.small)
                        .background(tuckColors.surfaceVariant.copy(alpha = 0.6f))
                )
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
    val interactionSource = remember { MutableInteractionSource() }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(interactionSource, pressedScale = 0.985f)
            .shadow(
                elevation = 3.dp,
                shape = tuckShapes.medium,
                ambientColor = tuckColors.scrim.copy(alpha = 0.35f),
                spotColor = tuckColors.scrim.copy(alpha = 0.35f)
            )
            .clip(tuckShapes.medium)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = tuckColors.accent),
                onClick = onClick
            ),
        shape = tuckShapes.medium,
        colors = CardDefaults.cardColors(containerColor = tuckColors.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, tuckColors.borderSubtle)
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
                    val context = LocalContext.current
                    val imageRequest = remember(previewImage) {
                        val imageModel: Any = when {
                            previewImage.startsWith("content://") -> Uri.parse(previewImage)
                            previewImage.startsWith("/") || previewImage.startsWith("file://") -> File(previewImage.removePrefix("file://"))
                            else -> previewImage
                        }
                        ImageRequest.Builder(context)
                            .data(imageModel)
                            .crossfade(true)
                            .build()
                    }

                    AsyncImage(
                        model = imageRequest,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Graded scrim along the bottom edge. A flat overlay dims the whole
                    // image; a gradient keeps the picture bright where nothing sits on
                    // top of it and only darkens under the badges.
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(TuckGradients.mediaScrim(tuckColors.scrim))
                    )

                    // Video Play Icon Overlay
                    if (item.contentType == ContentType.VIDEO || domain.contains("youtube") || domain.contains("instagram")) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(tuckColors.scrim.copy(alpha = 0.6f))
                                .align(Alignment.Center),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = tuckColors.onScrim,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // OCR Screenshot Badge
                    if (item.contentType == ContentType.IMAGE && !item.ocrText.isNullOrBlank()) {
                        Surface(
                            shape = tuckShapes.small,
                            color = tuckColors.scrim.copy(alpha = 0.7f),
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "📸 OCR Indexed",
                                style = MaterialTheme.typography.labelSmall,
                                color = tuckColors.onScrim,
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
                                tint = if (item.isFavorite) tuckColors.favorite else tuckColors.textMuted,
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
                                color = tuckColors.palette[PaletteSlot.SECONDARY_CORE].fill.copy(alpha = 0.12f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Message,
                                        contentDescription = null,
                                        tint = tuckColors.palette[PaletteSlot.SECONDARY_CORE].fill,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${item.comments.size} comments",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = tuckColors.palette[PaletteSlot.SECONDARY_CORE].fill
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

            Spacer(modifier = Modifier.height(10.dp))

            // 8-Hue Multi-Hue Palette Swatch Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(shapes.small),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                previewColors.palette.entries.forEach { entry ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(12.dp)
                            .background(entry.fill)
                    )
                }
            }
        }
    }
}

// 9. Cover Mosaic 4-Cell Preview for Collections
@Composable
fun TuckCoverMosaic(
    thumbnails: List<String?>,
    modifier: Modifier = Modifier,
    backgroundColor: Color = TuckTheme.colors.surfaceCard
) {
    val shapes = TuckTheme.shapes
    val validThumbs = thumbnails.filter { !it.isNullOrBlank() }.take(4)

    Surface(
        shape = shapes.medium,
        color = backgroundColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, TuckTheme.colors.border),
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp)
    ) {
        if (validThumbs.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = Icons.Filled.Folder,
                    contentDescription = null,
                    tint = TuckTheme.colors.accent.copy(alpha = 0.5f),
                    modifier = Modifier.size(36.dp)
                )
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .padding(1.dp)
                            .clip(RoundedCornerShape(topStart = 14.dp))
                    ) {
                        MosaicCell(path = validThumbs.getOrNull(0))
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .padding(1.dp)
                            .clip(RoundedCornerShape(topEnd = 14.dp))
                    ) {
                        MosaicCell(path = validThumbs.getOrNull(1))
                    }
                }
                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .padding(1.dp)
                            .clip(RoundedCornerShape(bottomStart = 14.dp))
                    ) {
                        MosaicCell(path = validThumbs.getOrNull(2))
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .padding(1.dp)
                            .clip(RoundedCornerShape(bottomEnd = 14.dp))
                    ) {
                        MosaicCell(path = validThumbs.getOrNull(3))
                    }
                }
            }
        }
    }
}

@Composable
private fun MosaicCell(path: String?) {
    if (path.isNullOrBlank()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(TuckTheme.colors.surfaceVariant.copy(alpha = 0.5f))
        )
    } else {
        val model: Any = if (path.startsWith("/") || path.startsWith("file://")) {
            File(path.removePrefix("file://"))
        } else {
            path
        }
        AsyncImage(
            model = model,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

// 10. Resurfaced Memory / Intent Recall Card
@Composable
fun TuckResurfacingCard(
    item: SavedItem,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tuckColors = TuckTheme.colors
    val shapes = TuckTheme.shapes

    Card(
        shape = shapes.medium,
        colors = CardDefaults.cardColors(containerColor = tuckColors.surface),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, tuckColors.accent.copy(alpha = 0.4f)),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header Row: Sparkle badge + Dismiss
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = shapes.small,
                        color = tuckColors.accentContainer,
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = tuckColors.accent,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "RESURFACED MEMORY",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = tuckColors.accent,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Dismiss",
                        tint = tuckColors.textMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // User Intent Note or Resurface Prompt
            val intentText = if (!item.userNote.isNullOrBlank()) {
                "\"${item.userNote}\""
            } else {
                "You saved this ${formatRelativeTime(item.createdAt)}: \"${item.title}\""
            }

            Text(
                text = intentText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = tuckColors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Footer info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TuckPlatformBadge(item = item)

                Text(
                    text = "Tap to review ›",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = tuckColors.accent
                )
            }
        }
    }
}

// 11. Quick Capture Speed Dial Bottom Sheet
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickCaptureSpeedDialSheet(
    onDismiss: () -> Unit,
    onCaptureNote: () -> Unit,
    onScanOcr: () -> Unit,
    onImportPdf: () -> Unit,
    onPasteLink: () -> Unit,
    onRecordAudio: () -> Unit
) {
    val tuckColors = TuckTheme.colors
    val shapes = TuckTheme.shapes
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = tuckColors.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = "QUICK CAPTURE",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = tuckColors.textMuted,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            QuickCaptureOption(
                title = "Take a Quick Note",
                subtitle = "Save a formatted thought or markdown note",
                icon = Icons.Filled.Notes,
                iconTint = tuckColors.palette[PaletteSlot.PRIMARY_CORE].fill,
                onClick = {
                    onDismiss()
                    onCaptureNote()
                }
            )

            QuickCaptureOption(
                title = "Scan Document / Screenshot",
                subtitle = "Local ML Kit OCR extracts all text instantly",
                icon = Icons.Filled.CameraAlt,
                iconTint = tuckColors.palette[PaletteSlot.SECONDARY_CORE].fill,
                onClick = {
                    onDismiss()
                    onScanOcr()
                }
            )

            QuickCaptureOption(
                title = "Import PDF or Research Paper",
                subtitle = "Extract text & generate first-page thumbnail",
                icon = Icons.Filled.PictureAsPdf,
                iconTint = tuckColors.palette[PaletteSlot.TERTIARY_CORE].fill,
                onClick = {
                    onDismiss()
                    onImportPdf()
                }
            )

            QuickCaptureOption(
                title = "Paste from Clipboard",
                subtitle = "Save copied links, Reddit URLs, or text",
                icon = Icons.Filled.ContentPaste,
                iconTint = tuckColors.palette[PaletteSlot.SECONDARY_DEEP].fill,
                onClick = {
                    onDismiss()
                    onPasteLink()
                }
            )

            QuickCaptureOption(
                title = "Record Audio Voice Memo",
                subtitle = "App-private local audio note",
                icon = Icons.Filled.Mic,
                iconTint = tuckColors.palette[PaletteSlot.PRIMARY_DEEP].fill,
                onClick = {
                    onDismiss()
                    onRecordAudio()
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun QuickCaptureOption(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit
) {
    val tuckColors = TuckTheme.colors
    val shapes = TuckTheme.shapes

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.medium)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = shapes.small,
            color = iconTint.copy(alpha = 0.12f),
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = tuckColors.textPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = tuckColors.textSecondary
            )
        }

        Icon(
            imageVector = Icons.Filled.ArrowForward,
            contentDescription = null,
            tint = tuckColors.textMuted,
            modifier = Modifier.size(16.dp)
        )
    }
}


/**
 * The primary action button.
 *
 * A gradient fill and a coloured shadow, so it reads as the one lifted thing on the
 * screen rather than a flat circle, plus a haptic tick on press - the cheapest way to
 * make a tap feel like it registered.
 */
@Composable
fun TuckFab(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Filled.Add
) {
    val tuckColors = TuckTheme.colors
    val haptics = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(58.dp)
            .pressScale(interactionSource, pressedScale = 0.92f)
            .shadow(
                elevation = 14.dp,
                shape = CircleShape,
                ambientColor = tuckColors.accent.copy(alpha = 0.6f),
                spotColor = tuckColors.accent.copy(alpha = 0.6f)
            )
            .clip(CircleShape)
            .background(TuckGradients.accent(tuckColors.accent, tuckColors.isDark))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = tuckColors.textOnAccent),
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tuckColors.textOnAccent,
            modifier = Modifier.size(26.dp)
        )
    }
}

/**
 * A collection as a row, with how much of it is still outstanding.
 *
 * The bar is the lifecycle work made visible: done items over total. A filing cabinet
 * tells you how much you have; this tells you how much you still owe yourself. No
 * competitor in this category can draw it, because none of them track a done state.
 */
@Composable
fun CollectionProgressRow(
    name: String,
    count: Int,
    openCount: Int,
    colorId: String? = null,
    iconHint: String? = null,
    collectionId: Long = 0,
    isLocked: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tuckColors = TuckTheme.colors
    val entry = resolveCollectionColor(colorId, name, collectionId)
    val visual = getCollectionVisual(name, iconHint, colorId, collectionId)
    val interactionSource = remember { MutableInteractionSource() }

    val done = (count - openCount).coerceAtLeast(0)
    val fraction = if (count == 0) 0f else done.toFloat() / count.toFloat()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(interactionSource, pressedScale = 0.99f)
            .clip(RoundedCornerShape(16.dp))
            .background(tuckColors.surface)
            .border(1.dp, tuckColors.borderSubtle, RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = entry.background),
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(TuckGradients.tile(entry.background, tuckColors.isDark)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isLocked) Icons.Filled.Lock else visual.icon,
                contentDescription = null,
                tint = entry.foreground,
                modifier = Modifier.size(19.dp)
            )
        }

        Spacer(modifier = Modifier.width(13.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = TuckTheme.typography.tileTitle,
                color = tuckColors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = when {
                    count == 0 -> "Empty"
                    openCount == 0 -> "$count ${if (count == 1) "item" else "items"} · all done"
                    else -> "$count items · $done done"
                },
                style = TuckTheme.typography.numericCount,
                color = tuckColors.textMuted
            )

            if (count > 0) {
                Spacer(modifier = Modifier.height(7.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(tuckColors.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction)
                            .clip(RoundedCornerShape(2.dp))
                            .background(entry.background)
                    )
                }
            }
        }

        if (count > 0) {
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "${(fraction * 100).toInt()}%",
                style = TuckTheme.typography.numericCount,
                fontWeight = FontWeight.ExtraBold,
                color = tuckColors.textSecondary
            )
        }
    }
}

/** Small section heading used to group a collection's contents by whether they are done. */
@Composable
fun CollectionSectionHeader(
    label: String,
    count: Int,
    modifier: Modifier = Modifier
) {
    val tuckColors = TuckTheme.colors
    Row(
        modifier = modifier.fillMaxWidth().padding(top = 6.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.2.sp,
            color = tuckColors.textMuted
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$count",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = tuckColors.textMuted
        )
        Spacer(modifier = Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(tuckColors.dividerHairline)
        )
    }
}
