package com.tuck.app.ui.theme.color

import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * 8 Perceptual Hue Slots for Multi-Hue Theme Families.
 */
enum class PaletteSlot(val defaultHueAngle: Double, val displayName: String) {
    TERRACOTTA(25.0, "Terracotta"),
    AMBER(65.0, "Amber"),
    MUSTARD(108.0, "Mustard"),
    SAGE(148.0, "Sage"),
    TEAL(190.0, "Teal"),
    DENIM(245.0, "Denim"),
    PLUM(290.0, "Plum"),
    ROSE(335.0, "Rose");

    companion object {
        fun fromString(value: String?): PaletteSlot? {
            if (value.isNullOrBlank()) return null
            return entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) }
        }
    }
}

/**
 * Entry representing a single slot's resolved fill color and accessible onFill foreground color.
 */
data class PaletteEntry(
    val slot: PaletteSlot,
    val fill: Color,
    val onFill: Color,
    val oklch: Oklch
)

/**
 * Coordinated 8-hue palette family for a theme.
 */
data class TuckPalette(
    val entries: List<PaletteEntry>
) {
    operator fun get(slot: PaletteSlot): PaletteEntry {
        return entries.firstOrNull { it.slot == slot } ?: entries.first()
    }

    fun getOrNull(slot: PaletteSlot?): PaletteEntry? {
        return entries.firstOrNull { it.slot == slot }
    }
}

object TuckPaletteBuilder {

    private fun channelToLinear(channel: Float): Double {
        return if (channel <= 0.04045) {
            channel / 12.92
        } else {
            ((channel + 0.055) / 1.055).pow(2.4)
        }
    }

    fun relativeLuminance(color: Color): Double {
        val r = channelToLinear(color.red)
        val g = channelToLinear(color.green)
        val b = channelToLinear(color.blue)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    fun contrastRatio(c1: Color, c2: Color): Double {
        val l1 = relativeLuminance(c1)
        val l2 = relativeLuminance(c2)
        val lighter = max(l1, l2)
        val darker = min(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }

    /**
     * Determines optimal onFill color (ink or paper) ensuring highest contrast ratio >= 4.5:1.
     */
    fun pickOnFill(fill: Color, ink: Color, paper: Color): Color {
        val inkRatio = contrastRatio(fill, ink)
        val paperRatio = contrastRatio(fill, paper)
        return if (paperRatio >= inkRatio) paper else ink
    }

    /**
     * Finds the maximum in-gamut chroma for a hue at given lightness.
     */
    fun findMaxInGamutChroma(l: Double, h: Double): Double {
        val probe = Oklch(l = l, c = 0.40, h = h)
        return probe.clampChromaToGamut().c
    }

    /**
     * Builds a coordinated TuckPalette holding lightness constant (or per-slot) and using
     * the minimum in-gamut chroma across all 8 slots.
     */
    fun buildPalette(
        targetLightness: (PaletteSlot) -> Double,
        ink: Color,
        paper: Color,
        maxChromaCap: Double = 0.15
    ): TuckPalette {
        // Step 1: determine in-gamut chroma for each slot
        val maxChromas = PaletteSlot.entries.map { slot ->
            val l = targetLightness(slot)
            findMaxInGamutChroma(l, slot.defaultHueAngle)
        }

        // Step 2: shared chroma is the minimum across the family, capped
        val sharedChroma = min(maxChromas.minOrNull() ?: maxChromaCap, maxChromaCap)

        // Step 3: construct entries
        val entries = PaletteSlot.entries.map { slot ->
            val l = targetLightness(slot)
            val oklch = Oklch(l = l, c = sharedChroma, h = slot.defaultHueAngle)
            val fill = oklch.toSrgb(clamp = true)
            val onFill = pickOnFill(fill, ink, paper)
            PaletteEntry(slot = slot, fill = fill, onFill = onFill, oklch = oklch)
        }

        return TuckPalette(entries)
    }
}

// Standard Light & Dark Optimal Lightness Distribution Maps
private val standardLightLightness: (PaletteSlot) -> Double = { slot ->
    when (slot) {
        PaletteSlot.TERRACOTTA -> 0.48
        PaletteSlot.AMBER -> 0.55
        PaletteSlot.MUSTARD -> 0.47
        PaletteSlot.SAGE -> 0.55
        PaletteSlot.TEAL -> 0.54
        PaletteSlot.DENIM -> 0.42
        PaletteSlot.PLUM -> 0.50
        PaletteSlot.ROSE -> 0.44
    }
}

private val standardDarkLightness: (PaletteSlot) -> Double = { slot ->
    when (slot) {
        PaletteSlot.TERRACOTTA -> 0.61
        PaletteSlot.AMBER -> 0.68
        PaletteSlot.MUSTARD -> 0.62
        PaletteSlot.SAGE -> 0.68
        PaletteSlot.TEAL -> 0.68
        PaletteSlot.DENIM -> 0.53
        PaletteSlot.PLUM -> 0.64
        PaletteSlot.ROSE -> 0.61
    }
}

/**
 * 1. Curated Linen Light Palette (Warm Paper Base).
 */
val LinenLightPalette: TuckPalette by lazy {
    TuckPaletteBuilder.buildPalette(
        targetLightness = standardLightLightness,
        ink = Color(0xFF1C1917),
        paper = Color(0xFFFFFFFF),
        maxChromaCap = 0.14
    )
}

/**
 * 1b. Curated Linen Dark Palette (Warm Hearth / Charcoal Base).
 */
val LinenDarkPalette: TuckPalette by lazy {
    TuckPaletteBuilder.buildPalette(
        targetLightness = standardDarkLightness,
        ink = Color(0xFF0F0E0C),
        paper = Color(0xFFFFFFFF),
        maxChromaCap = 0.13
    )
}

/**
 * 2. Curated Noir Dark Palette (Obsidian / Slate Base).
 */
val NoirDarkPalette: TuckPalette by lazy {
    TuckPaletteBuilder.buildPalette(
        targetLightness = standardDarkLightness,
        ink = Color(0xFF0D0D0E),
        paper = Color(0xFFFFFFFF),
        maxChromaCap = 0.13
    )
}

/**
 * 2b. Curated Noir Light Palette (Titanium / Crisp Monochrome Base).
 */
val NoirLightPalette: TuckPalette by lazy {
    TuckPaletteBuilder.buildPalette(
        targetLightness = standardLightLightness,
        ink = Color(0xFF111113),
        paper = Color(0xFFFFFFFF),
        maxChromaCap = 0.13
    )
}

/**
 * 3. Curated Forest Light Palette (Botanical Sage / Birch Base).
 */
val ForestLightPalette: TuckPalette by lazy {
    TuckPaletteBuilder.buildPalette(
        targetLightness = standardLightLightness,
        ink = Color(0xFF131A15),
        paper = Color(0xFFFFFFFF),
        maxChromaCap = 0.135
    )
}

/**
 * 3b. Curated Forest Dark Palette (Deep Pine / Moss Base).
 */
val ForestDarkPalette: TuckPalette by lazy {
    TuckPaletteBuilder.buildPalette(
        targetLightness = standardDarkLightness,
        ink = Color(0xFF0D140F),
        paper = Color(0xFFFFFFFF),
        maxChromaCap = 0.13
    )
}

/**
 * 4. Curated Cobalt Light Palette (Ice Blueprint / Paper Base).
 */
val CobaltLightPalette: TuckPalette by lazy {
    TuckPaletteBuilder.buildPalette(
        targetLightness = standardLightLightness,
        ink = Color(0xFF0F172A),
        paper = Color(0xFFFFFFFF),
        maxChromaCap = 0.14
    )
}

/**
 * 4b. Curated Cobalt Dark Palette (Abyssal Ocean / Deep Navy Base).
 */
val CobaltDarkPalette: TuckPalette by lazy {
    TuckPaletteBuilder.buildPalette(
        targetLightness = standardDarkLightness,
        ink = Color(0xFF090E17),
        paper = Color(0xFFFFFFFF),
        maxChromaCap = 0.135
    )
}

/**
 * 5. Curated Plum Light Palette (Velvet Dusk / Rose Quartz Base).
 */
val PlumLightPalette: TuckPalette by lazy {
    TuckPaletteBuilder.buildPalette(
        targetLightness = standardLightLightness,
        ink = Color(0xFF1E1320),
        paper = Color(0xFFFFFFFF),
        maxChromaCap = 0.14
    )
}

/**
 * 5b. Curated Plum Dark Palette (Twilight Mulberry / Charcoal Base).
 */
val PlumDarkPalette: TuckPalette by lazy {
    TuckPaletteBuilder.buildPalette(
        targetLightness = standardDarkLightness,
        ink = Color(0xFF100A13),
        paper = Color(0xFFFFFFFF),
        maxChromaCap = 0.13
    )
}
