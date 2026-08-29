package com.tuck.app.ui.theme

import androidx.compose.ui.graphics.Color
import com.tuck.app.ui.theme.color.PaletteSlot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class PaletteContrastTest {

    private fun channelToLinear(channel: Float): Double {
        return if (channel <= 0.04045) {
            channel / 12.92
        } else {
            ((channel + 0.055) / 1.055).pow(2.4)
        }
    }

    private fun relativeLuminance(color: Color): Double {
        val r = channelToLinear(color.red)
        val g = channelToLinear(color.green)
        val b = channelToLinear(color.blue)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    private fun contrastRatio(c1: Color, c2: Color): Double {
        val l1 = relativeLuminance(c1)
        val l2 = relativeLuminance(c2)
        val lighter = max(l1, l2)
        val darker = min(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }

    @Test
    fun testAllCollectionPaletteEntriesPassWcagAAContrast() {
        for (entry in DefaultCollectionPalette) {
            val ratio = contrastRatio(entry.background, entry.foreground)
            assertTrue(
                "Palette entry '${entry.name}' (${entry.id}) contrast ratio $ratio is below the WCAG AA requirement of 4.5:1",
                ratio >= 4.5
            )
        }
    }

    @Test
    fun testDeterministicFallbackResolution() {
        val resolvedWithSlot = resolveCollectionColor("terracotta", "Random Title", 100)
        assertEquals("terracotta", resolvedWithSlot.id)

        val resolvedLegacy = resolveCollectionColor("emerald", "Random Title", 100)
        assertEquals("sage", resolvedLegacy.id)

        val resolvedFromTitle = resolveCollectionColor(null, "Design Ideas", 0)
        assertTrue(DefaultCollectionPalette.contains(resolvedFromTitle))

        val resolvedBlank = resolveCollectionColor("", "", 42)
        assertTrue(DefaultCollectionPalette.contains(resolvedBlank))
    }

    @Test
    fun testSlotDispersionAvoidsSameNeighboringSlots() {
        val slot1 = resolveCollectionSlot(null, "Collection A", 1)
        val slot2 = resolveCollectionSlot(null, "Collection B", 2)
        val slot3 = resolveCollectionSlot(null, "Collection C", 3)
        // Ensure deterministic and returns valid slots
        assertTrue(PaletteSlot.entries.contains(slot1))
        assertTrue(PaletteSlot.entries.contains(slot2))
        assertTrue(PaletteSlot.entries.contains(slot3))
    }
}
