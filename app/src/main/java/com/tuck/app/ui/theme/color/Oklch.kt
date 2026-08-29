package com.tuck.app.ui.theme.color

import androidx.compose.ui.graphics.Color
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Perceptually uniform OKLCH color representation.
 *
 * @param l Perceived lightness in range [0.0, 1.0]
 * @param c Chroma (color intensity / saturation) in range [0.0, ~0.4]
 * @param h Hue angle in degrees in range [0.0, 360.0)
 */
data class Oklch(
    val l: Double,
    val c: Double,
    val h: Double
) {
    /**
     * Converts this OKLCH color to Compose [Color] in sRGB space.
     * If [clamp] is true, chroma is binary-searched to ensure it fits within the sRGB gamut.
     */
    fun toSrgb(clamp: Boolean = false): Color {
        val oklch = if (clamp) clampChromaToGamut() else this
        val (rLin, gLin, bLin) = oklch.toLinearSrgb()
        val r = linearToSrgb(rLin).coerceIn(0.0, 1.0).toFloat()
        val g = linearToSrgb(gLin).coerceIn(0.0, 1.0).toFloat()
        val b = linearToSrgb(bLin).coerceIn(0.0, 1.0).toFloat()
        return Color(r, g, b)
    }

    /**
     * Returns true if this OKLCH color falls cleanly within the standard sRGB gamut.
     */
    fun isInGamut(epsilon: Double = 1e-4): Boolean {
        val (rLin, gLin, bLin) = toLinearSrgb()
        val minBound = -epsilon
        val maxBound = 1.0 + epsilon
        return rLin in minBound..maxBound &&
                gLin in minBound..maxBound &&
                bLin in minBound..maxBound
    }

    /**
     * Binary-searches the maximum in-gamut chroma for the fixed [l] and [h] coordinates.
     */
    fun clampChromaToGamut(iterations: Int = 20): Oklch {
        if (isInGamut()) return this
        var low = 0.0
        var high = c
        var best = 0.0

        for (i in 0 until iterations) {
            val mid = (low + high) / 2.0
            val test = copy(c = mid)
            if (test.isInGamut()) {
                best = mid
                low = mid
            } else {
                high = mid
            }
        }
        return copy(c = best)
    }

    /**
     * Converts this OKLCH color to OKLab (L, a, b) coordinates.
     */
    fun toOklab(): Triple<Double, Double, Double> {
        val hRad = Math.toRadians(h)
        val a = c * cos(hRad)
        val b = c * sin(hRad)
        return Triple(l, a, b)
    }

    fun toLinearSrgb(): Triple<Double, Double, Double> {
        val (labL, labA, labB) = toOklab()

        val l_ = labL + 0.3963377774 * labA + 0.2158037573 * labB
        val m_ = labL - 0.1055613458 * labA - 0.0638541728 * labB
        val s_ = labL - 0.0894841775 * labA - 1.2914855480 * labB

        val lLin = l_ * l_ * l_
        val mLin = m_ * m_ * m_
        val sLin = s_ * s_ * s_

        val rLin = +4.0767416621 * lLin - 3.3077115913 * mLin + 0.2309699292 * sLin
        val gLin = -1.2684380046 * lLin + 2.6097574011 * mLin - 0.3413193965 * sLin
        val bLin = -0.0041960863 * lLin - 0.7034186147 * mLin + 1.7076147010 * sLin

        return Triple(rLin, gLin, bLin)
    }

    companion object {
        fun srgbToLinear(c: Double): Double {
            return if (c <= 0.04045) {
                c / 12.92
            } else {
                ((c + 0.055) / 1.055).pow(2.4)
            }
        }

        fun linearToSrgb(c: Double): Double {
            return if (c <= 0.0031308) {
                12.92 * c
            } else {
                1.055 * c.pow(1.0 / 2.4) - 0.055
            }
        }
    }
}

/**
 * Converts a Compose [Color] to [Oklch].
 */
fun Color.toOklch(): Oklch {
    val rLin = Oklch.srgbToLinear(red.toDouble())
    val gLin = Oklch.srgbToLinear(green.toDouble())
    val bLin = Oklch.srgbToLinear(blue.toDouble())

    val l = 0.4122214708 * rLin + 0.5363325363 * gLin + 0.0514459929 * bLin
    val m = 0.2119034982 * rLin + 0.6806995451 * gLin + 0.1073969566 * bLin
    val s = 0.0883024619 * rLin + 0.2817188376 * gLin + 0.6299787005 * bLin

    val l_ = Math.cbrt(l)
    val m_ = Math.cbrt(m)
    val s_ = Math.cbrt(s)

    val labL = 0.2104542553 * l_ + 0.7936177850 * m_ - 0.0040720468 * s_
    val labA = 1.9779984951 * l_ - 2.4285922050 * m_ + 0.4505937099 * s_
    val labB = 0.0259040371 * l_ + 0.7827717662 * m_ - 0.8086757660 * s_

    val c = sqrt(labA * labA + labB * labB)
    var h = Math.toDegrees(atan2(labB, labA))
    if (h < 0.0) h += 360.0

    return Oklch(l = labL, c = c, h = h)
}

/**
 * Computes the Euclidean distance in OKLab color space: ΔE = √(ΔL² + Δa² + Δb²).
 */
fun oklabDeltaE(c1: Color, c2: Color): Double {
    val ok1 = c1.toOklch().toOklab()
    val ok2 = c2.toOklch().toOklab()

    val dL = ok1.first - ok2.first
    val da = ok1.second - ok2.second
    val db = ok1.third - ok2.third

    return sqrt(dL * dL + da * da + db * db)
}
