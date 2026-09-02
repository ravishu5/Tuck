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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
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

/** Height the content area must reserve so the floating bar never covers the last row. */
val FloatingNavBarSpace: Dp = 96.dp

private val BarHeight = 64.dp
private val BarSideMargin = 16.dp
private val BarBottomMargin = 14.dp
private val CaptureSize = 50.dp

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
                    .shadow(12.dp, CircleShape, clip = false)
                    .background(colors.surfaceElevated, CircleShape)
                    .border(1.dp, colors.borderSubtle, CircleShape)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val half = (screens.size + 1) / 2

                screens.take(half).forEach { screen ->
                    NavIcon(screen, currentRoute == screen.route) { onSelect(screen) }
                }

                CaptureButton(onClick = onCapture)

                screens.drop(half).forEach { screen ->
                    NavIcon(screen, currentRoute == screen.route) { onSelect(screen) }
                }
            }
        }
    }
}

@Composable
private fun NavIcon(
    screen: BottomNavScreen,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = TuckTheme.colors
    // The selected pill grows in rather than appearing, which reads as the same object moving.
    val pillWidth by animateDpAsState(if (selected) 52.dp else 44.dp, tween(180), label = "navPill")

    Box(
        modifier = Modifier
            .size(width = pillWidth, height = 40.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(if (selected) colors.accentContainer else colors.surfaceElevated)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
            contentDescription = stringResource(screen.titleRes),
            tint = if (selected) colors.accent else colors.textMuted,
            modifier = Modifier.size(23.dp)
        )
    }
}

@Composable
private fun CaptureButton(onClick: () -> Unit) {
    val colors = TuckTheme.colors

    // Sized to sit inside the capsule with even clearance top and bottom, rather than
    // overflowing it: a button that breaks the outline reads as a rendering fault.
    Box(
        modifier = Modifier.size(CaptureSize),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = colors.accent,
            shape = CircleShape,
            shadowElevation = 8.dp,
            modifier = Modifier
                .size(CaptureSize)
                .clickable(onClick = onClick)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = stringResource(R.string.nav_capture),
                    tint = colors.textOnAccent,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
