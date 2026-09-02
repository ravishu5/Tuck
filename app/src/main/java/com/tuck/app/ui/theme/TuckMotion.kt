package com.tuck.app.ui.theme

import androidx.compose.ui.res.stringResource
import com.tuck.app.R

import android.provider.Settings
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext

/**
 * Motion tokens.
 *
 * Everything here is short. Animation that draws attention to itself is the difference
 * between an app that feels quick and one that feels slow, so nothing on a primary path
 * runs longer than 300ms, and all of it collapses to nothing when the user has asked the
 * system to reduce animation.
 */
object TuckMotion {
    const val QUICK_MS = 120
    const val STANDARD_MS = 220
    const val EMPHASIS_MS = 300

    /** Presses and small state changes: responsive, barely any overshoot. */
    fun <T> snappy() = spring<T>(
        dampingRatio = 0.75f,
        stiffness = Spring.StiffnessMedium
    )

    /** Entrances and reveals: a little softer. */
    fun <T> gentle() = spring<T>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    fun <T> standardTween() = tween<T>(durationMillis = STANDARD_MS)
}

/**
 * True when the user has turned animations down or off system-wide.
 *
 * Honouring this is an accessibility requirement, not a nicety: motion can trigger
 * nausea and migraine for people with vestibular disorders.
 */
@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ) == 0f
    }
}

/**
 * Shrinks slightly while pressed, so a tap feels physical rather than instantaneous.
 * The press source is returned to the caller's `clickable` so the ripple still lines up.
 */
@Composable
fun Modifier.pressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.97f
): Modifier = composed {
    val reduceMotion = rememberReduceMotion()
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && !reduceMotion) pressedScale else 1f,
        animationSpec = TuckMotion.snappy(),
        label = "pressScale"
    )
    this.scale(scale)
}
