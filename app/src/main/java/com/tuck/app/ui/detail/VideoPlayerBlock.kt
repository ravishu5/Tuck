package com.tuck.app.ui.detail

import android.media.MediaPlayer
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.tuck.app.R
import kotlinx.coroutines.delay

private const val SkipMs = 10_000

/**
 * Plays a video Tuck holds on disk, with controls the app owns.
 *
 * This exists because a platform embed cannot be driven. YouTube's iframe answers "video
 * unavailable" inside a WebView, Instagram serves logged-out embedders no video at all, and
 * neither exposes play, seek or position to the host app. Once the file is local, all three are
 * ordinary — and the clip keeps playing when the signed CDN link it came from expires.
 */
@Composable
fun VideoPlayerBlock(
    path: String,
    modifier: Modifier = Modifier
) {
    var player by remember(path) { mutableStateOf<MediaPlayer?>(null) }
    var view by remember(path) { mutableStateOf<VideoView?>(null) }
    var isPlaying by remember(path) { mutableStateOf(false) }
    var positionMs by remember(path) { mutableIntStateOf(0) }
    var durationMs by remember(path) { mutableIntStateOf(0) }
    var aspect by remember(path) { mutableFloatStateOf(16f / 9f) }
    // While a finger is on the bar the clock follows the finger, not the decoder.
    var scrubbingTo by remember(path) { mutableStateOf<Float?>(null) }

    // Polling rather than a listener because MediaPlayer has no position callback; a quarter of
    // a second is smooth enough to watch and cheap enough to ignore.
    LaunchedEffect(isPlaying, path) {
        while (isPlaying) {
            view?.let { if (it.isPlaying) positionMs = it.currentPosition }
            delay(250)
        }
    }

    DisposableEffect(path) {
        onDispose {
            view?.stopPlayback()
            player = null
            view = null
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspect.coerceIn(0.4f, 2.2f))
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        setVideoPath(path)
                        setOnPreparedListener { mp ->
                            player = mp
                            mp.isLooping = false
                            durationMs = duration.coerceAtLeast(0)
                            if (mp.videoWidth > 0 && mp.videoHeight > 0) {
                                aspect = mp.videoWidth.toFloat() / mp.videoHeight.toFloat()
                            }
                        }
                        setOnCompletionListener {
                            isPlaying = false
                            positionMs = durationMs
                        }
                        view = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Tapping the picture is the gesture everyone already knows.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable {
                        view?.let { v ->
                            if (v.isPlaying) {
                                v.pause(); isPlaying = false
                            } else {
                                v.start(); isPlaying = true
                            }
                        }
                    }
            )

            if (!isPlaying) {
                Surface(
                    color = Color.Black.copy(alpha = 0.45f),
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.size(62.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = stringResource(R.string.player_play),
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }
            }
        }

        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Slider(
                value = scrubbingTo ?: positionMs.toFloat(),
                onValueChange = { scrubbingTo = it },
                onValueChangeFinished = {
                    scrubbingTo?.let { target ->
                        view?.seekTo(target.toInt())
                        positionMs = target.toInt()
                    }
                    scrubbingTo = null
                },
                valueRange = 0f..(durationMs.coerceAtLeast(1).toFloat()),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatClock((scrubbingTo ?: positionMs.toFloat()).toInt()),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.width(12.dp))

                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TransportButton(Icons.Rounded.Replay10, R.string.player_back_10) {
                        view?.let {
                            val to = (it.currentPosition - SkipMs).coerceAtLeast(0)
                            it.seekTo(to); positionMs = to
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    TransportButton(
                        icon = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        labelRes = if (isPlaying) R.string.player_pause else R.string.player_play,
                        primary = true
                    ) {
                        view?.let { v ->
                            if (v.isPlaying) {
                                v.pause(); isPlaying = false
                            } else {
                                v.start(); isPlaying = true
                            }
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    TransportButton(Icons.Rounded.Forward10, R.string.player_forward_10) {
                        view?.let {
                            val to = (it.currentPosition + SkipMs).coerceAtMost(durationMs)
                            it.seekTo(to); positionMs = to
                        }
                    }
                }

                Spacer(Modifier.width(12.dp))

                Text(
                    text = formatClock(durationMs),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TransportButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    labelRes: Int,
    primary: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge,
        color = if (primary) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        modifier = Modifier.size(if (primary) 46.dp else 40.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = stringResource(labelRes),
                tint = if (primary) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(if (primary) 26.dp else 20.dp)
            )
        }
    }
}
