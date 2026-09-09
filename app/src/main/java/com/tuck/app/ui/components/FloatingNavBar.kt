package com.tuck.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.ripple
import androidx.compose.ui.graphics.Color
import com.tuck.app.ui.theme.TuckGradients
import com.tuck.app.ui.theme.pressScale
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tuck.app.R
import com.tuck.app.ui.navigation.BottomNavScreen
import com.tuck.app.ui.theme.TuckTheme

private val BarHeight = 76.dp
private val BarSideMargin = 16.dp
private val BarBottomMargin = 14.dp
private val CaptureSize = 50.dp

/**
 * Height the content area must reserve so the floating bar never covers the last row.
 *
 * Derived from the bar's own measurements rather than written down, because a hand-written
 * figure drifts the moment the bar is restyled — and it drifts in the direction of a dead band
 * at the bottom of every screen, which is invisible to whoever changed the bar.
 *
 * The system navigation inset is *not* included: the bar is already offset by it where it is
 * placed, so counting it here would push content up by it twice.
 */
val FloatingNavBarSpace: Dp = BarHeight + BarBottomMargin + 8.dp

/**
 * Breathing room under the last row of a scrolling screen.
 *
 * Clearance for the floating bar is [FloatingNavBarSpace] and is applied once, around the whole
 * nav graph. Screens inside it were each adding their own 80-100dp on top of that, which is
 * where the empty band at the bottom of every tab came from; this is the gap a list wants
 * anyway, and nothing to do with the bar.
 */
val ListBottomGap: Dp = 16.dp

/**
 * Tracks whether the floating bar should be showing, from the scroll of whatever is beneath it.
 *
 * Direction rather than position: the bar hides once a downward scroll has accumulated past
 * [thresholdPx] and returns on the first meaningful pull upward. Reacting to raw deltas instead
 * would make it flicker on the small oscillations a finger produces while holding still.
 */
@Composable
fun rememberNavBarVisibility(thresholdPx: Float = 48f): NavBarVisibility {
    val density = LocalDensity.current
    return remember(density, thresholdPx) { NavBarVisibility(thresholdPx) }
}

class NavBarVisibility(private val thresholdPx: Float) {

    var visible by mutableStateOf(true)
        private set

    private var accumulated by mutableFloatStateOf(0f)

    /** Call when the user leaves a scrolling screen, so the bar is never stranded hidden. */
    fun reveal() {
        accumulated = 0f
        visible = true
    }

    val connection: NestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            val delta = available.y
            // Reset the moment direction changes, so a threshold is per-gesture rather than
            // something the user has to overcome after scrolling a long way the other way.
            if ((delta < 0f && accumulated > 0f) || (delta > 0f && accumulated < 0f)) {
                accumulated = 0f
            }
            accumulated += delta

            if (accumulated < -thresholdPx && visible) {
                visible = false
                accumulated = 0f
            } else if (accumulated > thresholdPx && !visible) {
                visible = true
                accumulated = 0f
            }
            return Offset.Zero
        }
    }
}

/**
 * The app's primary navigation: a floating capsule with the four destinations either side of a
 * raised capture button.
 *
 * It sits over the content rather than pushing it, so the content area reserves
 * [FloatingNavBarSpace] permanently. That keeps the layout still while the bar slides away —
 * reserving space only while it is showing would reflow the list under the reader's thumb every
 * time they changed scroll direction.
 */
@Composable
fun FloatingNavBar(
    screens: List<BottomNavScreen>,
    currentRoute: String?,
    visible: Boolean,
    onSelect: (BottomNavScreen) -> Unit,
    onCapture: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = TuckTheme.colors

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(animationSpec = tween(220)) { it } + fadeIn(tween(220)),
        exit = slideOutVertically(animationSpec = tween(180)) { it } + fadeOut(tween(180)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = BarSideMargin, end = BarSideMargin, bottom = BarBottomMargin),
            contentAlignment = Alignment.BottomCenter
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(BarHeight)
                    .shadow(
                        elevation = 16.dp,
                        shape = CircleShape,
                        ambientColor = colors.scrim.copy(alpha = 0.35f),
                        spotColor = colors.scrim.copy(alpha = 0.35f)
                    )
                    .background(colors.surfaceElevated, CircleShape)
                    .border(1.dp, colors.borderSubtle, CircleShape)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val half = (screens.size + 1) / 2

                screens.take(half).forEach { screen ->
                    NavIcon(screen, currentRoute == screen.route, Modifier.weight(1f)) { onSelect(screen) }
                }

                CaptureButton(onClick = onCapture)

                screens.drop(half).forEach { screen ->
                    NavIcon(screen, currentRoute == screen.route, Modifier.weight(1f)) { onSelect(screen) }
                }
            }
        }
    }
}

@Composable
private fun NavIcon(
    screen: BottomNavScreen,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = TuckTheme.colors
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .height(60.dp)
            .padding(horizontal = 3.dp)
            .pressScale(interactionSource, pressedScale = 0.94f)
            .clip(RoundedCornerShape(percent = 50))
            .background(if (selected) colors.accentContainer else Color.Transparent)
            .then(
                if (selected) Modifier.border(1.dp, colors.accent.copy(alpha = 0.35f), RoundedCornerShape(percent = 50))
                else Modifier
            )
            .selectable(
                selected = selected,
                role = Role.Tab,
                interactionSource = interactionSource,
                indication = ripple(color = colors.accent),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                contentDescription = null,
                tint = if (selected) colors.accent else colors.textMuted,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = stringResource(if (screen == BottomNavScreen.Collections) R.string.nav_folders else screen.titleRes),
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) colors.accent else colors.textSecondary,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CaptureButton(onClick: () -> Unit) {
    val colors = TuckTheme.colors
    val interactionSource = remember { MutableInteractionSource() }

    // Sized to sit inside the capsule with even clearance top and bottom, rather than
    // overflowing it: a button that breaks the outline reads as a rendering fault.
    Box(
        modifier = Modifier.size(CaptureSize),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(CaptureSize)
                .pressScale(interactionSource, pressedScale = 0.92f)
                .shadow(
                    elevation = 10.dp,
                    shape = CircleShape,
                    ambientColor = colors.accent.copy(alpha = 0.45f),
                    spotColor = colors.accent.copy(alpha = 0.45f)
                )
                .clip(CircleShape)
                .background(TuckGradients.accent(colors.accent, colors.isDark))
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(color = colors.textOnAccent),
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            // Subtle top sheen highlight suggesting a lit tactile gem
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(CaptureSize / 2)
                    .align(Alignment.TopCenter)
                    .background(TuckGradients.sheen(colors.textOnAccent))
            )
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = stringResource(R.string.nav_capture),
                tint = colors.textOnAccent,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
