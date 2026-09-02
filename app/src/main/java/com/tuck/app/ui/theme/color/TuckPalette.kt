package com.tuck.app.ui.theme.color

import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/** Which of a theme's three signature hues a slot is built from. */
enum class HueRole { PRIMARY, SECONDARY, TERTIARY }

/**
 * A palette slot: one of the theme's three hues at one of three tonal depths.
 *
 * Themes are tricolor by design. Eight arbitrary hues make every theme look like the
 * same rainbow - which is exactly what happened before, since the hue angles were global
 * constants shared by all five themes. Three hues, varied by lightness, read as one
 * deliberate family, and restraint is most of what makes a palette look premium.
 *
 * Tiles stay distinguishable because tones are separated by lightness, not only hue -
 * which also survives colour-vision deficiency far better than hue alone.
 */
enum class PaletteSlot(val role: HueRole, val tone: Int, val displayName: String) {
    PRIMARY_SOFT(HueRole.PRIMARY, 0, "Primary soft"),
    PRIMARY_CORE(HueRole.PRIMARY, 1, "Primary"),
    PRIMARY_DEEP(HueRole.PRIMARY, 2, "Primary deep"),
    SECONDARY_SOFT(HueRole.SECONDARY, 0, "Secondary soft"),
    SECONDARY_CORE(HueRole.SECONDARY, 1, "Secondary"),
    SECONDARY_DEEP(HueRole.SECONDARY, 2, "Secondary deep"),
    TERTIARY_CORE(HueRole.TERTIARY, 1, "Tertiary"),
    TERTIARY_DEEP(HueRole.TERTIARY, 2, "Tertiary deep");

    companion object {
        fun fromString(value: String?): PaletteSlot? {
            if (value.isNullOrBlank()) return null
            return entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) }
        }
    }
}

/** The three signature hues of a theme, as OKLCH hue angles in degrees. */
data class ThemeTriad(
    val primaryHue: Double,
    val secondaryHue: Double,
    val tertiaryHue: Double
) {
    fun hueFor(role: HueRole): Double = when (role) {
        HueRole.PRIMARY -> primaryHue
        HueRole.SECONDARY -> secondaryHue
        HueRole.TERTIARY -> tertiaryHue
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
     * Builds a theme's palette from its three hues.
     *
     * Chroma is shared and capped: maximum in-gamut chroma varies a lot by hue at a fixed
     * lightness, so per-hue maxima would make the family uneven - a glaring yellow beside
     * a muted blue. Taking the family minimum keeps it even, and a low cap keeps it calm.
     */
    fun buildTriadPalette(
        triad: ThemeTriad,
        toneLightness: (Int) -> Double,
        ink: Color,
        paper: Color,
        maxChromaCap: Double = 0.15
    ): TuckPalette {
        val maxChromas = PaletteSlot.entries.map { slot ->
            findMaxInGamutChroma(toneLightness(slot.tone), triad.hueFor(slot.role))
        }
        val sharedChroma = min(maxChromas.minOrNull() ?: maxChromaCap, maxChromaCap)

        val entries = PaletteSlot.entries.map { slot ->
            val oklch = Oklch(
                l = toneLightness(slot.tone),
                c = sharedChroma,
                h = triad.hueFor(slot.role)
            )
            val fill = oklch.toSrgb(clamp = true)
            PaletteEntry(
                slot = slot,
                fill = fill,
                onFill = pickOnFill(fill, ink, paper),
                oklch = oklch
            )
        }
        return TuckPalette(entries)
    }
}

/**
 * Light themes.
 *
 * Kept below ~0.60: a tile must clear 3:1 against a near-white canvas or it dissolves
 * into it, and lightness in the 0.55-0.65 band is the dead zone where neither ink nor
 * paper reaches 4.5:1 on top of the fill.
 */
val lightToneLightness: (Int) -> Double = { tone ->
    when (tone) {
        0 -> 0.52
        1 -> 0.44
        else -> 0.36
    }
}

/** Dark themes: lifted, so saturated tiles never glare against a near-black canvas. */
val darkToneLightness: (Int) -> Double = { tone ->
    when (tone) {
        0 -> 0.80
        1 -> 0.71
        else -> 0.62
    }
}

// --- Tile foreground candidates -------------------------------------------------
// Only two, deliberately: a tile's label is either ink or paper, whichever contrasts
// better. Anything in between reads as muddy.
private val TileInk = Color(0xFF15110E)
private val TilePaper = Color(0xFFFDFBF8)

/**
 * The six theme triads.
 *
 * Each is three hues chosen to sit together: a lead, a companion within reach of it,
 * and one further away for contrast. Angles are OKLCH degrees, so "close" and "far"
 * mean perceptually close and far rather than close on a colour wheel.
 */
object TuckTriads {
    // Every triad pairs two related hues with one that sits far away, and each spans the
    // blue-yellow axis somewhere. Red-green deficiencies leave blue-yellow largely intact,
    // so a triad drawn from one side of the wheel - three blues, say - collapses into a
    // single colour for roughly one man in twelve. Cobalt was exactly that, and the
    // colourblind test caught it.

    /** Warm paper: terracotta, ochre, denim. */
    val Linen = ThemeTriad(primaryHue = 32.0, secondaryHue = 85.0, tertiaryHue = 235.0)

    /** Luxe dark: champagne, oxblood, steel. */
    val Noir = ThemeTriad(primaryHue = 88.0, secondaryHue = 25.0, tertiaryHue = 245.0)

    /** Calm and natural: moss, teal, ochre. */
    val Forest = ThemeTriad(primaryHue = 150.0, secondaryHue = 212.0, tertiaryHue = 70.0)

    /** Focused and technical: cobalt, cyan, amber. */
    val Cobalt = ThemeTriad(primaryHue = 255.0, secondaryHue = 195.0, tertiaryHue = 65.0)

    /** Creative and expressive: plum, violet, citron. */
    val Plum = ThemeTriad(primaryHue = 325.0, secondaryHue = 268.0, tertiaryHue = 90.0)

    /** Cool and composed: slate, seafoam, sand. */
    val Harbor = ThemeTriad(primaryHue = 235.0, secondaryHue = 175.0, tertiaryHue = 75.0)
}

private fun lightPalette(triad: ThemeTriad): TuckPalette =
    TuckPaletteBuilder.buildTriadPalette(triad, lightToneLightness, TileInk, TilePaper)

private fun darkPalette(triad: ThemeTriad): TuckPalette =
    TuckPaletteBuilder.buildTriadPalette(triad, darkToneLightness, TileInk, TilePaper)

val LinenLightPalette: TuckPalette by lazy { lightPalette(TuckTriads.Linen) }
val LinenDarkPalette: TuckPalette by lazy { darkPalette(TuckTriads.Linen) }
val NoirLightPalette: TuckPalette by lazy { lightPalette(TuckTriads.Noir) }
val NoirDarkPalette: TuckPalette by lazy { darkPalette(TuckTriads.Noir) }
val ForestLightPalette: TuckPalette by lazy { lightPalette(TuckTriads.Forest) }
val ForestDarkPalette: TuckPalette by lazy { darkPalette(TuckTriads.Forest) }
val CobaltLightPalette: TuckPalette by lazy { lightPalette(TuckTriads.Cobalt) }
val CobaltDarkPalette: TuckPalette by lazy { darkPalette(TuckTriads.Cobalt) }
val PlumLightPalette: TuckPalette by lazy { lightPalette(TuckTriads.Plum) }
val PlumDarkPalette: TuckPalette by lazy { darkPalette(TuckTriads.Plum) }
val HarborLightPalette: TuckPalette by lazy { lightPalette(TuckTriads.Harbor) }
val HarborDarkPalette: TuckPalette by lazy { darkPalette(TuckTriads.Harbor) }
