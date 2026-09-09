package com.tuck.app.ui.map

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.preference.PreferenceManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.tuck.app.ui.theme.TuckTheme
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

/** One place on a map: a coordinate and what to call it. */
data class MapPin(
    /** Stable across recompositions, so the map knows which pin is the focused one. */
    val id: Long,
    val latitude: Double,
    val longitude: Double,
    val title: String,
    val subtitle: String? = null
)

/**
 * Places on a map.
 *
 * Tiles come from OpenStreetMap, which needs no API key and no billing account — but it does
 * fetch tiles over the network per view, so a map is the one part of Tuck that is not purely
 * on-device. What is being shown never leaves: only the tile requests do, and those say nothing
 * beyond which squares of the world are on screen.
 *
 * OSM's tile policy requires a distinguishing User-Agent; osmdroid sends the package name by
 * default and would otherwise be answered with 403s.
 *
 * Shared between the travel history, which shows every trip at once, and a single saved
 * location, which shows one — so a place looks the same wherever Tuck draws it.
 */
@Composable
fun TuckMap(
    pins: List<MapPin>,
    modifier: Modifier = Modifier,
    /**
     * The pin the map should be looking at. Changing it flies the map there; null frames every
     * pin at once.
     */
    focusedId: Long? = null,
    /** Zoom used when the map is showing one place rather than framing several. */
    singlePinZoom: Double = SINGLE_PIN_ZOOM,
    /** Lets the reader pan and pinch. Off for a small preview that sits inside a scrolling page. */
    interactive: Boolean = true
) {
    val context = LocalContext.current
    val markerColor = TuckTheme.colors.accent.toArgb()

    val mapView = remember {
        Configuration.getInstance().apply {
            load(context, PreferenceManager.getDefaultSharedPreferences(context))
            userAgentValue = context.packageName
            osmdroidBasePath = context.cacheDir.resolve("osmdroid")
            osmdroidTileCache = context.cacheDir.resolve("osmdroid/tiles")
        }
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            // The map lives inside a scrolling page, so its own zoom buttons would sit under
            // the reader's thumb exactly where they scroll.
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            isHorizontalMapRepetitionEnabled = false
            isVerticalMapRepetitionEnabled = false
            setScrollableAreaLimitLatitude(
                MapView.getTileSystem().maxLatitude,
                MapView.getTileSystem().minLatitude,
                0
            )
        }
    }

    DisposableEffect(Unit) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }

    // Clipped explicitly: a Compose Box does not clip its children, and an Android view inside
    // one happily paints past the height it was given — which put the map over the top of
    // whatever the page drew underneath it.
    Box(modifier = modifier.clipToBounds()) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize(),
            update = { map ->
                map.setMultiTouchControls(interactive)
                map.overlays.clear()

                val points = pins.map { GeoPoint(it.latitude, it.longitude) to it }
                val pin = dotMarker(map.context, markerColor)
                val highlightPin = dotMarker(map.context, markerColor, highlighted = true)

                points.forEach { (point, model) ->
                    map.overlays.add(
                        Marker(map).apply {
                            position = point
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                            icon = if (model.id == focusedId) highlightPin else pin
                            title = model.title
                            snippet = model.subtitle
                        }
                    )
                }

                val focusPoint = focusedId
                    ?.let { id -> points.firstOrNull { it.second.id == id }?.first }

                when {
                    // A chosen pin wins over framing everything: the reader asked to see this
                    // one, so the map goes to it rather than to the average of all of them.
                    focusPoint != null ->
                        map.controller.animateTo(focusPoint, FOCUS_ZOOM, FOCUS_ANIMATION_MS)

                    points.isEmpty() -> {
                        // Nothing to frame, so show the whole world rather than the middle of
                        // the Atlantic at street level.
                        map.controller.setZoom(WORLD_ZOOM)
                        map.controller.setCenter(GeoPoint(20.0, 0.0))
                    }

                    points.size == 1 -> {
                        map.controller.setZoom(singlePinZoom)
                        map.controller.setCenter(points.first().first)
                    }

                    else -> {
                        val box = BoundingBox.fromGeoPointsSafe(points.map { it.first })
                        // Posted rather than called directly: zoomToBoundingBox needs the view
                        // to have been measured, and on the first composition it has not.
                        map.post {
                            runCatching { map.zoomToBoundingBox(box, false, MAP_PADDING_PX) }
                        }
                    }
                }
                map.invalidate()
            }
        )
    }
}

/**
 * A plain filled dot in the theme's accent.
 *
 * osmdroid's stock pin is a large red teardrop that anchors at its tip and belongs to a
 * different design language entirely; a dot reads as data on a map, and re-tints itself with
 * whichever of the six palettes is active.
 */
private fun dotMarker(context: Context, color: Int, highlighted: Boolean = false): Drawable {
    val density = context.resources.displayMetrics.density
    val radius = (if (highlighted) MARKER_FOCUSED_RADIUS_DP else MARKER_RADIUS_DP) * density
    val ring = (if (highlighted) MARKER_FOCUSED_RING_DP else MARKER_RING_DP) * density
    val size = ((radius + ring) * 2).toInt()

    return object : Drawable() {
        private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        }
        private val halo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = android.graphics.Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = ring
        }

        override fun draw(canvas: Canvas) {
            val cx = bounds.exactCenterX()
            val cy = bounds.exactCenterY()
            canvas.drawCircle(cx, cy, radius, fill)
            canvas.drawCircle(cx, cy, radius, halo)
        }

        override fun setAlpha(alpha: Int) = Unit
        override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) = Unit

        @Deprecated("Deprecated in Java", ReplaceWith("android.graphics.PixelFormat.TRANSLUCENT"))
        override fun getOpacity(): Int = android.graphics.PixelFormat.TRANSLUCENT

        override fun getIntrinsicWidth(): Int = size
        override fun getIntrinsicHeight(): Int = size
    }
}

private const val MARKER_RADIUS_DP = 5f
private const val MARKER_RING_DP = 1.5f

/** The chosen pin, drawn larger so it is findable once the map has flown to it. */
private const val MARKER_FOCUSED_RADIUS_DP = 8f
private const val MARKER_FOCUSED_RING_DP = 2.5f

private const val MAP_PADDING_PX = 80
private const val WORLD_ZOOM = 1.6
private const val SINGLE_PIN_ZOOM = 6.0

/** Close enough to see the town, far enough to see what it is near. */
private const val FOCUS_ZOOM = 9.0
private const val FOCUS_ANIMATION_MS = 700L
