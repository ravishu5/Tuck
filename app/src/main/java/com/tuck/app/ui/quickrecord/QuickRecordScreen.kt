package com.tuck.app.ui.quickrecord

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tuck.app.R
import com.tuck.app.processing.QuickRecordController
import com.tuck.app.ui.theme.TuckTheme
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.roundToInt

/**
 * A timer, a level meter and two buttons.
 *
 * Everything here is deliberately absent: no lists, no navigation, no account of what else is
 * in the vault. It is shown over the lock screen, so what it does not say matters as much as
 * what it does.
 */
@Composable
fun QuickRecordScreen(
    controller: QuickRecordController,
    onStop: () -> Unit,
    onCancel: () -> Unit
) {
    // Its own theme rather than the app's: reading the saved flavour means touching settings
    // and DataStore before the microphone opens, and this screen is judged on that gap.
    TuckTheme {
        val colors = TuckTheme.colors
        val state by controller.state.collectAsState()
        val startedAt = (state as? QuickRecordController.State.Recording)?.startedAt

        var elapsedMs by remember { mutableLongStateOf(0L) }
        var level by remember { mutableFloatStateOf(0f) }

        LaunchedEffect(startedAt) {
            if (startedAt == null) return@LaunchedEffect
            while (true) {
                elapsedMs = System.currentTimeMillis() - startedAt
                level = controller.amplitude()
                delay(FRAME_MS)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.canvas),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 32.dp)
            ) {
                RecordingPulse(level = level, isRecording = startedAt != null)

                Spacer(Modifier.height(28.dp))

                Text(
                    text = formatDuration(elapsedMs),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = stringResource(
                        if (startedAt != null) R.string.quick_record_recording
                        else R.string.quick_record_starting
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.quick_record_autosave_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted
                )

                Spacer(Modifier.height(44.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(28.dp)
                ) {
                    CircleAction(
                        label = stringResource(R.string.quick_record_discard),
                        size = 60.dp,
                        background = colors.surfaceVariant,
                        tint = colors.textSecondary,
                        onClick = onCancel
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = stringResource(R.string.quick_record_discard),
                            tint = colors.textSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // The one thing this screen is for, so it is the biggest thing on it and
                    // findable by feel without reading.
                    CircleAction(
                        label = stringResource(R.string.quick_record_stop_save),
                        size = 108.dp,
                        background = colors.destructive,
                        tint = colors.onScrim,
                        onClick = onStop
                    ) {
                        Icon(
                            Icons.Filled.Stop,
                            contentDescription = stringResource(R.string.quick_record_stop_save),
                            tint = Color.White,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }
            }
        }
    }
}

/** A ring that breathes with the microphone, so it is obvious sound is arriving. */
@Composable
private fun RecordingPulse(level: Float, isRecording: Boolean) {
    val colors = TuckTheme.colors
    val target = if (isRecording) 1f + (level * PULSE_RANGE) else 1f
    val scale by animateFloatAsState(targetValue = target, label = "pulse")

    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(colors.destructive.copy(alpha = 0.16f))
        )
        Box(
            modifier = Modifier
                .size(74.dp)
                .clip(CircleShape)
                .background(colors.destructive)
        )
    }
}

@Composable
private fun CircleAction(
    label: String,
    size: androidx.compose.ui.unit.Dp,
    background: Color,
    tint: Color,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = background,
            modifier = Modifier.size(size)
        ) {
            Box(contentAlignment = Alignment.Center) { content() }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = TuckTheme.colors.textSecondary,
            fontSize = 13.sp
        )
    }
}

/** `m:ss`, growing to `h:mm:ss` only when it has to. */
internal fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}

private const val FRAME_MS = 80L
private const val PULSE_RANGE = 0.45f
