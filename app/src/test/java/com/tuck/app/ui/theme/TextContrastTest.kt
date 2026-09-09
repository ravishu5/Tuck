package com.tuck.app.ui.theme

import androidx.compose.ui.graphics.Color
import com.tuck.app.domain.repository.TuckThemeFlavor
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Every theme's text must be readable on every surface that theme draws it on.
 *
 * Harbor was reported as unreadable, but the measurement showed the whole family was:
 * `textMuted` sat between 2.4:1 and 4.4:1 against the surfaces it was actually painted on
 * in all twelve palettes, which is below the 4.5:1 WCAG AA needs for body-sized text. A
 * per-theme eyeball fix would have left the other eleven wrong, so the ramp is defined by
 * contrast against the tightest surface in the theme and checked here.
 *
 * `surfaceVariant` is the reference background because it is the darkest surface in a light
 * theme and the lightest in a dark one — clearing it clears every other surface too.
 */
class TextContrastTest {

    private fun channelToLinear(channel: Float): Double =
        if (channel <= 0.04045) channel / 12.92 else ((channel + 0.055) / 1.055).pow(2.4)

    private fun relativeLuminance(color: Color): Double =
        0.2126 * channelToLinear(color.red) +
            0.7152 * channelToLinear(color.green) +
            0.0722 * channelToLinear(color.blue)

    private fun contrastRatio(a: Color, b: Color): Double {
        val l1 = relativeLuminance(a)
        val l2 = relativeLuminance(b)
        return (max(l1, l2) + 0.05) / (min(l1, l2) + 0.05)
    }

    private val allThemes: List<Pair<String, TuckColors>> =
        TuckThemeFlavor.entries.flatMap { flavor ->
            listOf(
                "${flavor.name} light" to getTuckColors(flavor, isDark = false),
                "${flavor.name} dark" to getTuckColors(flavor, isDark = true)
            )
        }

    /** Every surface a theme paints text on. */
    private fun surfacesOf(c: TuckColors) = listOf(
        "canvas" to c.canvas,
        "surface" to c.surface,
        "surfaceCard" to c.surfaceCard,
        "surfaceElevated" to c.surfaceElevated,
        "surfaceVariant" to c.surfaceVariant,
        "surfaceSubtle" to c.surfaceSubtle,
        "badgeBackground" to c.badgeBackground
    )

    @Test
    fun bodyTextClearsAaOnEverySurface() {
        for ((name, colors) in allThemes) {
            for ((surfaceName, surface) in surfacesOf(colors)) {
                for ((role, ink) in listOf(
                    "textPrimary" to colors.textPrimary,
                    "textSecondary" to colors.textSecondary,
                    "textMuted" to colors.textMuted
                )) {
                    val ratio = contrastRatio(ink, surface)
                    assertTrue(
                        "$name: $role on $surfaceName is ${"%.2f".format(ratio)}:1, below WCAG AA 4.5:1",
                        ratio >= 4.5
                    )
                }
            }
        }
    }

    /**
     * Three tones that measure the same are one tone. The ramp only earns its three names if
     * the eye can tell them apart, so each step has to be a real contrast step.
     */
    @Test
    fun theThreeTextTonesStayDistinct() {
        for ((name, colors) in allThemes) {
            val primary = contrastRatio(colors.textPrimary, colors.surfaceVariant)
            val secondary = contrastRatio(colors.textSecondary, colors.surfaceVariant)
            val muted = contrastRatio(colors.textMuted, colors.surfaceVariant)

            assertTrue(
                "$name: textPrimary (${"%.2f".format(primary)}) is not clearly stronger than " +
                    "textSecondary (${"%.2f".format(secondary)})",
                primary > secondary * 1.15
            )
            assertTrue(
                "$name: textSecondary (${"%.2f".format(secondary)}) is not clearly stronger than " +
                    "textMuted (${"%.2f".format(muted)})",
                secondary > muted * 1.15
            )
        }
    }

    /** Highlighted text is still text. */
    @Test
    fun highlightedTextClearsAa() {
        for ((name, colors) in allThemes) {
            val ratio = contrastRatio(colors.highlightText, colors.highlight)
            assertTrue(
                "$name: highlightText on highlight is ${"%.2f".format(ratio)}:1, below 4.5:1",
                ratio >= 4.5
            )
        }
    }

    /** A filled accent button's label has to survive its own fill. */
    @Test
    fun accentLabelClearsAa() {
        for ((name, colors) in allThemes) {
            val ratio = contrastRatio(colors.textOnAccent, colors.accent)
            assertTrue(
                "$name: textOnAccent on accent is ${"%.2f".format(ratio)}:1, below 4.5:1",
                ratio >= 4.5
            )
        }
    }

    /**
     * Status colours are read as words ("2 failed", "up to date"), not just as dots, so they
     * carry the same burden as any other text on the canvas.
     */
    @Test
    fun statusColoursClearLargeTextContrastOnCanvas() {
        for ((name, colors) in allThemes) {
            for ((role, ink) in listOf(
                "destructive" to colors.destructive,
                "warning" to colors.warning,
                "success" to colors.success,
                "favorite" to colors.favorite
            )) {
                val ratio = contrastRatio(ink, colors.canvas)
                assertTrue(
                    "$name: $role on canvas is ${"%.2f".format(ratio)}:1, below 3:1",
                    ratio >= 3.0
                )
            }
        }
    }
}
