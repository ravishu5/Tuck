package com.tuck.app.ui.detail

import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
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
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.produceState
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
import com.tuck.app.processing.extractors.GoogleMapsSourceExtractor
import com.tuck.app.ui.map.MapPin
import com.tuck.app.ui.map.TuckMap

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
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp
    val fallbackHeight = mediaHeightFor(
        url = item.originalUrl,
        screenHeight = screenHeight,
        screenWidth = screenWidth
    )

    // How tall the media frame should be.
    //
    // A reel is filmed 9:16, but Instagram's embed does not serve the clip at that shape — it
    // serves a 4:5 cover crop — so reserving 16/9 of the width for it left a band of black
    // about a third of the frame tall under every reel. The shape is the platform's to decide
    // and ours to discover, in this order:
    //
    //  1. The poster Tuck already saved. It is the actual frame, on disk, readable without a
    //     network round trip, and it is what the reader sees whenever the embed does not load.
    //  2. What the embed reports once it has drawn, for items saved without a poster.
    //  3. A 4:5 placeholder, until one of the above answers.
    val posterAspect = rememberMediaAspectRatio(item.thumbnailPath ?: item.localFilePath)
    var embedAspect by remember(item.id) { mutableStateOf<Float?>(null) }
    val aspect = posterAspect ?: embedAspect

    val height = when {
        !fitsMeasuredMedia(item.originalUrl) -> fallbackHeight
        aspect != null -> (screenWidth / aspect).coerceIn(180.dp, screenHeight * 0.92f)
        else -> fallbackHeight
    }

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

    // A saved location is a place, not a page.
    //
    // It used to be a Maps embed in a WebView: a consent prompt, an "open in the app" nag, and
    // a map the reader could not really use. Tuck already parses the coordinates out of the URL
    // at save time and already draws places on a map for the travel history, so a saved pin is
    // shown the same way — the same map, offline-capable tiles, no third-party page.
    val savedPlace = rememberSavedPlace(item)
    if (savedPlace != null) {
        Box(modifier = modifier.fillMaxWidth().height(MAP_HEIGHT)) {
            TuckMap(
                pins = listOf(savedPlace),
                singlePinZoom = PLACE_ZOOM,
                modifier = Modifier.fillMaxSize()
            )
            OverlayAction(
                icon = Icons.Rounded.OpenInFull,
                labelRes = R.string.detail_open_fullscreen,
                onClick = onExpand,
                modifier = Modifier.align(Alignment.BottomEnd).zIndex(1f).padding(14.dp)
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
            // Only for media-only embeds with no poster to read instead — a YouTube watch page
            // has no single piece of media whose shape the frame should take.
            onContentAspectRatio = if (fitsMeasuredMedia(item.originalUrl) && posterAspect == null) {
                { reported -> embedAspect = reported }
            } else {
                null
            },
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
 * The coordinates of a saved location, or null when this item is not one.
 *
 * Parsed from the URL rather than fetched: Maps encodes the pin in the path, so a saved place
 * can be drawn with no network at all. A Maps link whose coordinates could not be recovered —
 * a short link never opened, say — returns null and falls back to the embed, because a map
 * centred on nowhere is worse than a web page.
 */
@Composable
private fun rememberSavedPlace(item: SavedItem): MapPin? {
    val url = item.canonicalUrl ?: item.originalUrl ?: return null
    return remember(item.id, url) {
        if (!GoogleMapsSourceExtractor.canHandleUrl(url)) return@remember null
        val place = GoogleMapsSourceExtractor.parse(url)
        val (lat, lon) = place.coordinates ?: return@remember null
        val latitude = lat.toDoubleOrNull() ?: return@remember null
        val longitude = lon.toDoubleOrNull() ?: return@remember null
        MapPin(
            id = item.id,
            latitude = latitude,
            longitude = longitude,
            title = place.name ?: item.title,
            subtitle = "$lat, $lon"
        )
    }
}

/** A saved place is one pin, so it is framed rather than filling the screen. */
private val MAP_HEIGHT = 260.dp

/** Street-ish: close enough to recognise the place, wide enough to place it. */
private const val PLACE_ZOOM = 15.0

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
/**
 * The width-over-height ratio of a saved poster, or null when there isn't one to read.
 *
 * Decoded bounds-only, so nothing larger than the image header is ever allocated, and off the
 * main thread because it touches the disk. Remote posters are skipped: only a file Tuck holds
 * can be measured without a request, and a frame that resizes after a network round trip is
 * worse than one that never moves.
 */
@Composable
private fun rememberMediaAspectRatio(path: String?): Float? {
    val local = path?.takeIf {
        it.isNotBlank() && !it.startsWith("http://") && !it.startsWith("https://")
    }
    val ratio by produceState<Float?>(initialValue = null, key1 = local) {
        val file = local?.let { File(it) }?.takeIf { it.exists() } ?: return@produceState
        value = withContext(Dispatchers.IO) {
            runCatching {
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.absolutePath, options)
                val w = options.outWidth
                val h = options.outHeight
                if (w > 0 && h > 0) w.toFloat() / h.toFloat() else null
            }.getOrNull()
        }
    }
    return ratio
}

/** Formats the platform publishes portrait: the page scrolls through these rather than in them. */
private fun isVerticalMedia(url: String?): Boolean {
    val lower = url?.lowercase().orEmpty()
    return VERTICAL_HOSTS.any { lower.contains(it) }
}

/**
 * Whether the frame should be resized to whatever the embed actually rendered.
 *
 * True for the media-only embeds, where the tallest thing on the page *is* the media. False
 * for anything that ships a page around its player — YouTube's watch view measures as the
 * player plus its comment thread, so honouring that would make the header the whole screen.
 */
private fun fitsMeasuredMedia(url: String?): Boolean = isVerticalMedia(url)

private val VERTICAL_HOSTS =
    listOf("instagram.com", "instagr.am", "ig.me", "tiktok.com", "/shorts/")

private fun mediaHeightFor(url: String?, screenHeight: Dp, screenWidth: Dp): Dp {
    val lower = url?.lowercase().orEmpty()
    val isVertical = VERTICAL_HOSTS.any { lower.contains(it) }
    val isYouTube = listOf("youtube.com", "youtu.be").any { lower.contains(it) }

    return when {
        // The placeholder a portrait clip occupies until the embed reports its real shape.
        // 4:5 rather than 9:16 because that is what these embeds serve far more often, so the
        // frame settles with the smaller correction and the page does not visibly jump.
        // Capped so a tall phone still shows something below the media.
        isVertical -> minOf(screenWidth * (5f / 4f), screenHeight * 0.72f)
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
