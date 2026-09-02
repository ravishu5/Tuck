package com.tuck.app.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.tuck.app.ui.theme.TuckTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.tuck.app.R
import com.tuck.app.domain.model.SavedItem

/**
 * The media at the top of an item, with its actions floating over it.
 *
 * A saved reel is mostly a video, so the video gets the screen and the chrome gets out of its
 * way: no title bar above it, just translucent circular controls on the media itself. That also
 * means a vertical clip is shown at something close to the shape it was filmed in, rather than
 * letterboxed into a card with a toolbar stacked on top.
 */
@Composable
fun DetailMediaHeader(
    item: SavedItem,
    onExpand: () -> Unit,
    onCopyUrl: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val height = mediaHeightFor(item.originalUrl, screenHeight)

    // A file Tuck holds is always preferable: the app can play, seek and time it, and it
    // survives the CDN link expiring. The embed is the fallback for everything else.
    val localVideo = item.localFilePath?.takeIf {
        it.endsWith(".mp4", ignoreCase = true) && java.io.File(it).exists()
    }
    if (localVideo != null) {
        Box(modifier = modifier.fillMaxWidth().background(Color.Black)) {
            VideoPlayerBlock(path = localVideo)
            OverlayAction(
                icon = Icons.Rounded.OpenInFull,
                labelRes = R.string.detail_open_fullscreen,
                onClick = onExpand,
                modifier = Modifier.align(Alignment.TopEnd).zIndex(1f).padding(10.dp)
            )
        }
        return
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            // Black behind the media so a clip that does not fill the frame is letterboxed the
            // way every video player does it, rather than against the app's paper background.
            .background(Color.Black)
    ) {
        InPlaceMediaViewer(
            item = item,
            showToolbar = false,
            heightOverride = height,
            // Vertical formats are media only, so the page scrolls through them; YouTube keeps
            // its own scrolling because its comment thread lives inside the embed.
            scrollable = !isVerticalMedia(item.originalUrl),
            onCopyUrl = onCopyUrl
        )

        OverlayAction(
            icon = Icons.Rounded.OpenInFull,
            labelRes = R.string.detail_open_fullscreen,
            onClick = onExpand,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .zIndex(1f)
                .padding(14.dp)
        )
    }
}

/**
 * The item's actions, at the top of the page.
 *
 * These used to float over the media as translucent circles. Over a bright frame they were hard
 * to read, they covered the top of whatever was saved, and they made the page's most important
 * controls depend on what the picture happened to look like. A bar owes the reader a fixed,
 * legible place to find them.
 */
@Composable
fun DetailActionBar(
    onBack: () -> Unit,
    onRemind: () -> Unit,
    onAddToCollection: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = TuckTheme.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        BarAction(Icons.Rounded.ArrowBack, R.string.collections_back, colors.textPrimary, onBack)

        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            BarAction(Icons.Outlined.Alarm, R.string.detail_remind_me, colors.textSecondary, onRemind)
            BarAction(
                Icons.Outlined.CreateNewFolder,
                R.string.detail_collections_tags,
                colors.textSecondary,
                onAddToCollection
            )
            BarAction(Icons.Rounded.Share, R.string.detail_share, colors.textSecondary, onShare)
        }
    }
}

@Composable
private fun BarAction(
    icon: ImageVector,
    labelRes: Int,
    tint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick, modifier = Modifier.size(44.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = stringResource(labelRes),
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
    }
}

/**
 * How tall the media should be.
 *
 * Vertical formats get most of the screen because that is the shape they were filmed in; a
 * landscape video in the same box would be a thin strip in a sea of black.
 */
/** Formats the platform publishes portrait: the page scrolls through these rather than in them. */
private fun isVerticalMedia(url: String?): Boolean {
    val lower = url?.lowercase().orEmpty()
    return listOf("instagram.com", "instagr.am", "ig.me", "tiktok.com", "/shorts/")
        .any { lower.contains(it) }
}

private fun mediaHeightFor(url: String?, screenHeight: Dp): Dp {
    val lower = url?.lowercase().orEmpty()
    val isVertical = listOf("instagram.com", "instagr.am", "ig.me", "tiktok.com", "/shorts/")
        .any { lower.contains(it) }
    val isYouTube = listOf("youtube.com", "youtu.be").any { lower.contains(it) }

    return when {
        isVertical -> screenHeight * 0.62f
        // The player and its comment thread, which the reader scrolls inside the embed.
        isYouTube -> screenHeight * 0.60f
        else -> 300.dp
    }
}

@Composable
private fun OverlayAction(
    icon: ImageVector,
    labelRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OverlayActionImpl(icon, labelRes, onClick, modifier)
}

/**
 * A control that has to stay legible over whatever frame happens to be behind it, which is why
 * it carries its own scrim rather than relying on the media being dark.
 */
@Composable
private fun OverlayActionImpl(
    icon: ImageVector,
    labelRes: Int,
    onClick: () -> Unit,
    modifier: Modifier
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.45f),
        contentColor = Color.White,
        modifier = modifier.size(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = stringResource(labelRes),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}


/**
 * The block directly under the media: where it came from, when it was saved, what it says.
 *
 * Deliberately plain — no card, no border, no section heading. It is the item itself rather than
 * metadata about the item, so it reads as body copy and lets the cards below carry the structure.
 */
/**
 * Everything the page says about where an item came from, in one line.
 *
 * The old header stated the source four separate times — a profile row, a platform badge, the
 * word in the title, and again as a collection chip — while the thing the reader actually saved
 * said nothing. Identity is one muted line; the content gets the emphasis.
 */
@Composable
fun DetailSourceBlock(
    item: SavedItem,
    /** The post's author handle, which lives in `source_posts` rather than on the item. */
    authorHandle: String?,
    /** The channel or subreddit the post belongs to, when there is no handle. */
    community: String?,
    savedLabel: String,
    onOpenSource: () -> Unit,
    onCopyCaption: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = TuckTheme.colors
    val body = item.description?.takeIf { it.isNotBlank() }
        ?: item.extractedText?.takeIf { it.isNotBlank() }

    // The extractor prefixes a reel's audio track with a music note, so it can be lifted back
    // out and shown as its own line rather than buried at the top of the caption.
    val audioLine = body?.lineSequence()?.firstOrNull()?.takeIf { it.startsWith("\uD83C\uDFB5") }
    val caption = if (audioLine != null) body.removePrefix(audioLine).trim() else body

    // A title like "Instagram Reel" is a type label the share sheet invented, not something the
    // reader wrote or the post says. Showing it as a headline gives the page's loudest voice to
    // its least useful words, so it is only a headline when it is genuinely a title.
    val placeholderTitle = item.title.isBlank() ||
        item.title.equals(item.sourceDomain, ignoreCase = true) ||
        GENERIC_TITLES.any { item.title.equals(it, ignoreCase = true) }
    val headline = caption ?: item.title.takeIf { !placeholderTitle }

    Column(modifier = modifier.fillMaxWidth()) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Whatever the platform calls its author: an Instagram handle, a YouTube channel,
            // a subreddit. `community` is what the extractor recorded for each, and it is a far
            // better identity than the bare domain the page used to fall back to.
            val name = authorHandle
                ?: community
                ?: item.sourceDomain.orEmpty()

            Surface(
                shape = CircleShape,
                color = colors.accentContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = name.trimStart('@').take(1).uppercase().ifBlank { "?" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.accent
                    )
                }
            }

            Spacer(Modifier.size(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = savedLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted,
                    maxLines = 1
                )
            }

            Spacer(Modifier.size(10.dp))

            Surface(
                onClick = onOpenSource,
                shape = CircleShape,
                color = colors.accentContainer,
                contentColor = colors.accent
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(
                        text = stringResource(R.string.detail_view_profile),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (audioLine != null) {
            Spacer(Modifier.size(12.dp))
            Text(
                text = audioLine,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (headline != null) {
            Spacer(Modifier.size(12.dp))
            Text(
                text = headline,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.textPrimary,
                modifier = Modifier.clickable(onClick = onCopyCaption)
            )
        }
    }
}

private val GENERIC_TITLES = listOf(
    "Instagram Reel", "Instagram Post", "Instagram Story",
    "Reddit Post", "Reddit Discussion", "LinkedIn Post",
    "YouTube Video", "TikTok video", "Shared Link", "Google Maps location"
)

/**
 * One block of the item's own information.
 *
 * The page used to be a flat run of small grey labels with nothing but whitespace between them,
 * which reads as a settings form rather than as a saved thing. Giving each block a surface and a
 * quiet title gives the page a rhythm the eye can skim.
 */
@Composable
fun DetailSection(
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = TuckTheme.colors

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = colors.surfaceCard,
        border = BorderStroke(1.dp, colors.borderSubtle),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.textSecondary
                )
                action?.invoke()
            }
            Spacer(Modifier.size(12.dp))
            content()
        }
    }
}
