package com.tuck.app.ui.travel

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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.tuck.app.domain.model.Trip
import com.tuck.app.ui.theme.TuckTheme
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

/**
 * Everywhere the traveller has been, on one map.
 *
 * Tiles come from OpenStreetMap, which needs no API key and no billing account — but it does
 * fetch tiles over the network per view, so this is the one part of the travel page that is not
 * purely on-device. The trips themselves never leave: only the tile requests do, and those say
 * nothing beyond which squares of the world are on screen.
 *
 * OSM's tile policy requires a distinguishing User-Agent; osmdroid sends the package name by
 * default and would otherwise be answered with 403s.
 */
@Composable
fun TravelMap(
    trips: List<Trip>,
    /**
     * The trip the map should be looking at. Changing it flies the map there; null frames every
     * trip at once, which is the view the page opens on.
     */
    focused: Trip? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = TuckTheme.colors
    val markerColor = colors.accent.toArgb()

    val mapView = remember {
        Configuration.getInstance().apply {
            load(context, PreferenceManager.getDefaultSharedPreferences(context))
            userAgentValue = context.packageName
            osmdroidBasePath = context.cacheDir.resolve("osmdroid")
            osmdroidTileCache = context.cacheDir.resolve("osmdroid/tiles")
        }
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            // The map lives inside a scrolling page, so its own zoom buttons would sit under
            // the reader's thumb exactly where they scroll.
            zoomController.setVisibility(
                org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER
            )
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

    Box(modifier = modifier) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize(),
            update = { map ->
                map.overlays.clear()

                val points = trips.mapNotNull { trip ->
                    val lat = trip.latitude ?: return@mapNotNull null
                    val lon = trip.longitude ?: return@mapNotNull null
                    GeoPoint(lat, lon) to trip
                }

                val pin = dotMarker(map.context, markerColor)
                val highlightPin = dotMarker(map.context, markerColor, highlighted = true)
                points.forEach { (point, trip) ->
                    map.overlays.add(
                        Marker(map).apply {
                            position = point
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                            icon = if (trip.id == focused?.id) highlightPin else pin
                            title = trip.title
                            snippet = trip.countryName
                        }
                    )
                }

                val focusPoint = focused
                    ?.takeIf { it.hasCoordinates }
                    ?.let { GeoPoint(it.latitude!!, it.longitude!!) }

                when {
                    // A chosen trip wins over framing everything: the reader asked to see this
                    // one, so the map goes to it rather than to the average of all of them.
                    focusPoint != null -> {
                        map.controller.animateTo(focusPoint, FOCUS_ZOOM, FOCUS_ANIMATION_MS)
                    }

                    points.isEmpty() -> {
                        // Nothing to frame yet, so show the whole world rather than the middle
                        // of the Atlantic at street level.
                        map.controller.setZoom(1.6)
                        map.controller.setCenter(GeoPoint(20.0, 0.0))
                    }

                    points.size == 1 -> {
                        map.controller.setZoom(6.0)
                        map.controller.setCenter(points.first().first)
                    }

                    else -> {
                        val box = org.osmdroid.util.BoundingBox.fromGeoPointsSafe(
                            points.map { it.first }
                        )
                        // Posted rather than called directly: zoomToBoundingBox needs the view
                        // to have been measured, and on the first composition it has not.
                        map.post { runCatching { map.zoomToBoundingBox(box, false, MAP_PADDING_PX) } }
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

/** The chosen trip's pin, drawn larger so it is findable once the map has flown to it. */
private const val MARKER_FOCUSED_RADIUS_DP = 8f
private const val MARKER_FOCUSED_RING_DP = 2.5f

private const val MAP_PADDING_PX = 80

/** Close enough to see the town, far enough to see what it is near. */
private const val FOCUS_ZOOM = 9.0
private const val FOCUS_ANIMATION_MS = 700L
