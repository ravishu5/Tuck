package com.tuck.app

import androidx.compose.ui.graphics.Color
import com.tuck.app.ui.theme.CobaltDark
import com.tuck.app.ui.theme.CobaltLight
import com.tuck.app.ui.theme.ForestDark
import com.tuck.app.ui.theme.ForestLight
import com.tuck.app.ui.theme.LinenDark
import com.tuck.app.ui.theme.LinenLight
import com.tuck.app.ui.theme.NoirDark
import com.tuck.app.ui.theme.NoirLight
import com.tuck.app.ui.theme.PlumDark
import com.tuck.app.ui.theme.PlumLight
import com.tuck.app.ui.theme.color.CobaltDarkPalette
import com.tuck.app.ui.theme.color.CobaltLightPalette
import com.tuck.app.ui.theme.color.ForestDarkPalette
import com.tuck.app.ui.theme.color.ForestLightPalette
import com.tuck.app.ui.theme.color.LinenDarkPalette
import com.tuck.app.ui.theme.color.LinenLightPalette
import com.tuck.app.ui.theme.color.NoirDarkPalette
import com.tuck.app.ui.theme.color.NoirLightPalette
import com.tuck.app.ui.theme.color.Oklch
import com.tuck.app.ui.theme.color.PaletteEntry
import com.tuck.app.ui.theme.color.PaletteSlot
import com.tuck.app.ui.theme.color.PlumDarkPalette
import com.tuck.app.ui.theme.color.PlumLightPalette
import com.tuck.app.ui.theme.color.TuckPalette
import com.tuck.app.ui.theme.color.TuckPaletteBuilder
import com.tuck.app.ui.theme.color.oklabDeltaE
import com.tuck.app.ui.theme.color.toOklch
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class PaletteValidationTest {

    private val shippedPalettes = listOf(
        "Linen Light" to (LinenLightPalette to LinenLight.canvas),
        "Linen Dark" to (LinenDarkPalette to LinenDark.canvas),
        "Noir Light" to (NoirLightPalette to NoirLight.canvas),
        "Noir Dark" to (NoirDarkPalette to NoirDark.canvas),
        "Forest Light" to (ForestLightPalette to ForestLight.canvas),
        "Forest Dark" to (ForestDarkPalette to ForestDark.canvas),
        "Cobalt Light" to (CobaltLightPalette to CobaltLight.canvas),
        "Cobalt Dark" to (CobaltDarkPalette to CobaltDark.canvas),
        "Plum Light" to (PlumLightPalette to PlumLight.canvas),
        "Plum Dark" to (PlumDarkPalette to PlumDark.canvas)
    )

    // Viénot 1999 Color Vision Deficiency Simulation Matrices in Linear sRGB
    enum class CvdType {
        DEUTERANOPIA,
        PROTANOPIA,
        TRITANOPIA
    }

    companion object {
        fun simulateCvd(color: Color, cvdType: CvdType): Color {
            val rLin = Oklch.srgbToLinear(color.red.toDouble())
            val gLin = Oklch.srgbToLinear(color.green.toDouble())
            val bLin = Oklch.srgbToLinear(color.blue.toDouble())

            val (rSim, gSim, bSim) = when (cvdType) {
                CvdType.DEUTERANOPIA -> Triple(
                    0.625 * rLin + 0.375 * gLin + 0.0 * bLin,
                    0.700 * rLin + 0.300 * gLin + 0.0 * bLin,
                    0.0 * rLin + 0.300 * gLin + 0.700 * bLin
                )
                CvdType.PROTANOPIA -> Triple(
                    0.56667 * rLin + 0.43333 * gLin + 0.0 * bLin,
                    0.55833 * rLin + 0.44167 * gLin + 0.0 * bLin,
                    0.0 * rLin + 0.24167 * gLin + 0.75833 * bLin
                )
                CvdType.TRITANOPIA -> Triple(
                    0.950 * rLin + 0.050 * gLin + 0.0 * bLin,
                    0.0 * rLin + 0.433 * gLin + 0.567 * bLin,
                    0.0 * rLin + 0.475 * gLin + 0.525 * bLin
                )
            }

            val r = Oklch.linearToSrgb(rSim).coerceIn(0.0, 1.0).toFloat()
            val g = Oklch.linearToSrgb(gSim).coerceIn(0.0, 1.0).toFloat()
            val b = Oklch.linearToSrgb(bSim).coerceIn(0.0, 1.0).toFloat()

            return Color(r, g, b)
        }
    }

    @Test
    fun test1_ForegroundContrastScoresAtLeast4_5To1() {
        for ((name, pair) in shippedPalettes) {
            val (palette, _) = pair
            for (entry in palette.entries) {
                val ratio = TuckPaletteBuilder.contrastRatio(entry.fill, entry.onFill)
                assertTrue(
                    "[$name - ${entry.slot.displayName}] Foreground contrast $ratio is below 4.5:1 WCAG AA minimum",
                    ratio >= 4.5
                )
            }
        }
    }

    @Test
    fun test2_TileSeparationScoresAtLeast3To1AgainstCanvas() {
        for ((name, pair) in shippedPalettes) {
            val (palette, canvas) = pair
            for (entry in palette.entries) {
                val ratio = TuckPaletteBuilder.contrastRatio(entry.fill, canvas)
                assertTrue(
                    "[$name - ${entry.slot.displayName}] Tile separation $ratio against canvas is below 3:1 minimum",
                    ratio >= 3.0
                )
            }
        }
    }

    @Test
    fun test3_PairwiseDistinctnessPassesPerceptualDistanceFloor() {
        val distinctnessFloor = 0.06 // OKLab ΔE minimum separation floor

        for ((name, pair) in shippedPalettes) {
            val (palette, _) = pair
            val entries = palette.entries
            for (i in 0 until entries.size) {
                for (j in i + 1 until entries.size) {
                    val e1 = entries[i]
                    val e2 = entries[j]
                    val deltaE = oklabDeltaE(e1.fill, e2.fill)
                    assertTrue(
                        "[$name] Pairwise distinctness between ${e1.slot} and ${e2.slot} ($deltaE) is below floor $distinctnessFloor",
                        deltaE >= distinctnessFloor
                    )
                }
            }
        }
    }

    @Test
    fun test3b_DeliberatelyFailingBadPaletteFailsDistinctnessFloor() {
        val badPalette = TuckPalette(
            listOf(
                PaletteEntry(PaletteSlot.TERRACOTTA, Color(0xFFC04030), Color.White, Oklch(0.5, 0.1, 25.0)),
                PaletteEntry(PaletteSlot.AMBER, Color(0xFFC04131), Color.White, Oklch(0.5, 0.1, 25.5))
            )
        )
        val deltaE = oklabDeltaE(badPalette.entries[0].fill, badPalette.entries[1].fill)
        val floor = 0.06
        val passes = deltaE >= floor
        assertFalse("A bad palette with nearly identical colors MUST fail the distinctness floor", passes)
    }

    @Test
    fun test4_ColorblindSafetyHoldsUnderDeuteranopiaProtanopiaTritanopia() {
        val cvdDistinctnessFloor = 0.020 // OKLab ΔE under full dichromacy CVD simulation

        for (cvdType in CvdType.entries) {
            for ((name, pair) in shippedPalettes) {
                val (palette, _) = pair
                val entries = palette.entries
                for (i in 0 until entries.size) {
                    for (j in i + 1 until entries.size) {
                        val e1 = entries[i]
                        val e2 = entries[j]
                        val sim1 = simulateCvd(e1.fill, cvdType)
                        val sim2 = simulateCvd(e2.fill, cvdType)
                        val deltaE = oklabDeltaE(sim1, sim2)
                        assertTrue(
                            "[$name - $cvdType] Pairwise distinctness between ${e1.slot} and ${e2.slot} under $cvdType ($deltaE) is below CVD floor $cvdDistinctnessFloor",
                            deltaE >= cvdDistinctnessFloor
                        )
                    }
                }
            }
        }
    }

    @Test
    fun test4b_DeliberatelyFailingBadPaletteFailsCvdFloor() {
        val badPalette = TuckPalette(
            listOf(
                PaletteEntry(PaletteSlot.SAGE, Color(0xFF558855), Color.White, Oklch(0.5, 0.1, 140.0)),
                PaletteEntry(PaletteSlot.ROSE, Color(0xFF558855), Color.White, Oklch(0.5, 0.1, 140.0))
            )
        )
        val sim1 = simulateCvd(badPalette.entries[0].fill, CvdType.DEUTERANOPIA)
        val sim2 = simulateCvd(badPalette.entries[1].fill, CvdType.DEUTERANOPIA)
        val deltaE = oklabDeltaE(sim1, sim2)
        val cvdFloor = 0.020
        val passes = deltaE >= cvdFloor
        assertFalse("A bad palette with identical colors under CVD simulation MUST fail the CVD floor", passes)
    }

    @Test
    fun test5_GamutIntegrityRoundTripsWithoutClipping() {
        for ((name, pair) in shippedPalettes) {
            val (palette, _) = pair
            for (entry in palette.entries) {
                assertTrue(
                    "[$name - ${entry.slot.displayName}] OKLCH definition must be within sRGB gamut",
                    entry.oklch.isInGamut()
                )

                val reconstructed = entry.fill.toOklch()
                assertEquals(
                    "[$name - ${entry.slot.displayName}] Lightness round-trip mismatch",
                    entry.oklch.l,
                    reconstructed.l,
                    0.02
                )
                assertEquals(
                    "[$name - ${entry.slot.displayName}] Chroma round-trip mismatch",
                    entry.oklch.c,
                    reconstructed.c,
                    0.02
                )
            }
        }
    }

    private fun assertEquals(msg: String, expected: Double, actual: Double, delta: Double) {
        assertTrue("$msg: expected $expected but was $actual", abs(expected - actual) <= delta)
    }
}
