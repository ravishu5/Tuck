package com.tuck.app

import androidx.compose.ui.graphics.Color
import com.tuck.app.ui.theme.color.Oklch
import com.tuck.app.ui.theme.color.toOklch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class OklchTest {

    @Test
    fun testWhiteSanityVector() {
        val white = Color(0xFFFFFFFF)
        val oklch = white.toOklch()
        assertEquals("Lightness of white should be 1.0", 1.0, oklch.l, 0.005)
        assertEquals("Chroma of white should be 0.0", 0.0, oklch.c, 0.005)
    }

    @Test
    fun testBlackSanityVector() {
        val black = Color(0xFF000000)
        val oklch = black.toOklch()
        assertEquals("Lightness of black should be 0.0", 0.0, oklch.l, 0.005)
        assertEquals("Chroma of black should be 0.0", 0.0, oklch.c, 0.005)
    }

    @Test
    fun testRedSanityVector() {
        val red = Color(0xFFFF0000)
        val oklch = red.toOklch()
        assertEquals("Lightness of red ≈ 0.628", 0.628, oklch.l, 0.015)
        assertEquals("Chroma of red ≈ 0.258", 0.258, oklch.c, 0.015)
        assertEquals("Hue of red ≈ 29°", 29.2, oklch.h, 2.0)
    }

    @Test
    fun testBlueSanityVector() {
        val blue = Color(0xFF0000FF)
        val oklch = blue.toOklch()
        assertEquals("Lightness of blue ≈ 0.452", 0.452, oklch.l, 0.015)
        assertEquals("Chroma of blue ≈ 0.313", 0.313, oklch.c, 0.015)
        assertEquals("Hue of blue ≈ 264°", 264.0, oklch.h, 2.0)
    }

    @Test
    fun testRoundTripConversionWithinTolerance() {
        val testColors = listOf(
            Color(0xFFFFFFFF),
            Color(0xFF000000),
            Color(0xFFFF0000),
            Color(0xFF00FF00),
            Color(0xFF0000FF),
            Color(0xFFF59E0B),
            Color(0xFF0D9488),
            Color(0xFF6366F1),
            Color(0xFFE11D48),
            Color(0xFF64748B),
            Color(0xFFFAF7F2),
            Color(0xFF181614)
        )

        val tolerance = 1.5f / 255f

        for (original in testColors) {
            val oklch = original.toOklch()
            val reconstructed = oklch.toSrgb()

            assertTrue(
                "Red channel round-trip error exceeds tolerance for $original: orig=${original.red}, rec=${reconstructed.red}",
                abs(original.red - reconstructed.red) <= tolerance
            )
            assertTrue(
                "Green channel round-trip error exceeds tolerance for $original: orig=${original.green}, rec=${reconstructed.green}",
                abs(original.green - reconstructed.green) <= tolerance
            )
            assertTrue(
                "Blue channel round-trip error exceeds tolerance for $original: orig=${original.blue}, rec=${reconstructed.blue}",
                abs(original.blue - reconstructed.blue) <= tolerance
            )
        }
    }

    @Test
    fun testGamutClampingProducesInGamutColors() {
        // High chroma out-of-gamut target
        val outOfGamut = Oklch(l = 0.70, c = 0.35, h = 145.0)
        val clamped = outOfGamut.clampChromaToGamut()

        assertTrue("Clamped color must be in gamut", clamped.isInGamut())
        assertTrue("Clamped chroma must be <= original chroma", clamped.c <= outOfGamut.c)
        assertEquals("Lightness must be preserved", outOfGamut.l, clamped.l, 1e-6)
        assertEquals("Hue must be preserved", outOfGamut.h, clamped.h, 1e-6)
    }
}
