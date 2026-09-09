package com.tuck.app.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.tuck.app.ui.theme.color.toOklch

/**
 * Gradients derived from the active theme rather than written by hand.
 *
 * A gradient is only ever the same colour at two lightnesses, computed in OKLCH so the
 * shift is perceptually even. Mixing toward white or black in sRGB - the usual shortcut -
 * desaturates and skews hue, which is what makes hand-rolled gradients look muddy.
 */
object TuckGradients {

    /** Shifts a colour's lightness while holding its hue and chroma. */
    fun shift(color: Color, delta: Double): Color {
        val oklch = color.toOklch()
        return oklch.copy(l = (oklch.l + delta).coerceIn(0.0, 1.0)).toSrgb(clamp = true)
    }

    /**
     * Tile fill: a quiet diagonal, brighter at the top-left as though lit from there.
     * Kept shallow on purpose - a strong gradient reads as decoration, a shallow one
     * reads as material.
     */
    fun tile(fill: Color, isDark: Boolean): Brush {
        val lift = if (isDark) 0.05 else 0.06
        return Brush.linearGradient(
            colors = listOf(shift(fill, lift), fill, shift(fill, -lift * 0.8)),
            start = Offset.Zero,
            end = Offset.Infinite
        )
    }

    /** Primary actions - the FAB and filled buttons. */
    fun accent(accent: Color, isDark: Boolean): Brush {
        val lift = if (isDark) 0.06 else 0.07
        return Brush.linearGradient(listOf(shift(accent, lift), shift(accent, -lift * 0.6)))
    }

    /** A soft wash for headers and empty states, fading into the canvas. */
    fun canvasWash(accent: Color, canvas: Color): Brush = Brush.verticalGradient(
        listOf(accent.copy(alpha = 0.07f), canvas.copy(alpha = 0f))
    )

    /** Bottom-up scrim so caption text stays readable over any image. */
    fun mediaScrim(scrim: Color): Brush = Brush.verticalGradient(
        0f to scrim.copy(alpha = 0f),
        0.55f to scrim.copy(alpha = 0.35f),
        1f to scrim.copy(alpha = 0.78f)
    )

    /**
     * A surface-weight version of a palette colour: same hue, lifted to near-canvas
     * lightness with most of the chroma removed.
     *
     * Computed in OKLCH rather than by lowering alpha over the canvas - an alpha wash
     * inherits whatever is behind it and shifts as the background does, where this stays
     * a fixed, predictable colour in the same family.
     */
    fun tint(color: Color, isDark: Boolean): Color {
        val oklch = color.toOklch()
        return oklch.copy(
            l = if (isDark) 0.235 else 0.945,
            c = minOf(oklch.c, if (isDark) 0.045 else 0.032)
        ).toSrgb(clamp = true)
    }

    /** A half-step stronger than [tint], for borders and separators on a tinted surface. */
    fun tintEdge(color: Color, isDark: Boolean): Color {
        val oklch = color.toOklch()
        return oklch.copy(
            l = if (isDark) 0.30 else 0.895,
            c = minOf(oklch.c, if (isDark) 0.055 else 0.045)
        ).toSrgb(clamp = true)
    }

    /** The faint highlight along a tile's top edge that suggests a lit surface. */
    fun sheen(foreground: Color): Brush = Brush.verticalGradient(
        0f to foreground.copy(alpha = 0.14f),
        0.45f to foreground.copy(alpha = 0.02f),
        1f to foreground.copy(alpha = 0f)
    )
}
