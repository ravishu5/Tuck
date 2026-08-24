package com.tuck.app.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tuck.app.domain.repository.TuckThemeFlavor

/**
 * Tuck design tokens for colors, spacing, and shapes.
 */
data class TuckColors(
    val background: Color,
    val surface: Color,
    val surfaceCard: Color,
    val surfaceElevated: Color,
    val surfaceVariant: Color,
    val surfaceSubtle: Color,
    val accent: Color,
    val accentLight: Color,
    val accentContainer: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val textOnAccent: Color,
    val border: Color,
    val borderSubtle: Color,
    val isDark: Boolean
)

data class TuckSpacing(
    val none: Dp = 0.dp,
    val xxs: Dp = 4.dp,
    val xs: Dp = 8.dp,
    val s: Dp = 12.dp,
    val m: Dp = 16.dp,
    val l: Dp = 20.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp
)

data class TuckShapes(
    val small: Shape = RoundedCornerShape(12.dp),
    val medium: Shape = RoundedCornerShape(16.dp),
    val large: Shape = RoundedCornerShape(20.dp),
    val extraLarge: Shape = RoundedCornerShape(24.dp),
    val pill: Shape = RoundedCornerShape(28.dp),
    val circle: Shape = CircleShape
)

// 1. Linen Palettes (Default)
val LinenLight = TuckColors(
    background = Color(0xFFFAF7F2),
    surface = Color(0xFFFFFFFF),
    surfaceCard = Color(0xFFF4EEE5),
    surfaceElevated = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEDE5D8),
    surfaceSubtle = Color(0xFFF8F3EC),
    accent = Color(0xFFE25C34),
    accentLight = Color(0xFFF07650),
    accentContainer = Color(0xFFFDEEE9),
    textPrimary = Color(0xFF1C1917),
    textSecondary = Color(0xFF6E665E),
    textMuted = Color(0xFF9C9287),
    textOnAccent = Color(0xFFFFFFFF),
    border = Color(0xFFE8DFD3),
    borderSubtle = Color(0xFFF0E9DF),
    isDark = false
)

val LinenDark = TuckColors(
    background = Color(0xFF181614),
    surface = Color(0xFF221F1C),
    surfaceCard = Color(0xFF2A2622),
    surfaceElevated = Color(0xFF322E29),
    surfaceVariant = Color(0xFF38332E),
    surfaceSubtle = Color(0xFF1F1C19),
    accent = Color(0xFFFF7A50),
    accentLight = Color(0xFFFFA080),
    accentContainer = Color(0xFF3D261E),
    textPrimary = Color(0xFFF7F4EE),
    textSecondary = Color(0xFFB5ADA4),
    textMuted = Color(0xFF7C746B),
    textOnAccent = Color(0xFF181614),
    border = Color(0xFF3B352E),
    borderSubtle = Color(0xFF2B2621),
    isDark = true
)

// 2. Noir Palettes
val NoirDark = TuckColors(
    background = Color(0xFF0D0D0E),
    surface = Color(0xFF161619),
    surfaceCard = Color(0xFF1F1F24),
    surfaceElevated = Color(0xFF28282E),
    surfaceVariant = Color(0xFF2F2F37),
    surfaceSubtle = Color(0xFF121214),
    accent = Color(0xFFFF6B4A),
    accentLight = Color(0xFFFF886E),
    accentContainer = Color(0xFF3A1E17),
    textPrimary = Color(0xFFF4F4F6),
    textSecondary = Color(0xFFA2A2AD),
    textMuted = Color(0xFF6B6B76),
    textOnAccent = Color(0xFF0D0D0E),
    border = Color(0xFF2E2E38),
    borderSubtle = Color(0xFF22222A),
    isDark = true
)

val NoirLight = TuckColors(
    background = Color(0xFFF5F5F7),
    surface = Color(0xFFFFFFFF),
    surfaceCard = Color(0xFFECECEE),
    surfaceElevated = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE2E2E6),
    surfaceSubtle = Color(0xFFF9F9FA),
    accent = Color(0xFFE64A19),
    accentLight = Color(0xFFFF6B4A),
    accentContainer = Color(0xFFFFEBE6),
    textPrimary = Color(0xFF111113),
    textSecondary = Color(0xFF5A5A66),
    textMuted = Color(0xFF8C8C9A),
    textOnAccent = Color(0xFFFFFFFF),
    border = Color(0xFFDCDCE2),
    borderSubtle = Color(0xFFE8E8EC),
    isDark = false
)

// 3. Forest Palettes
val ForestLight = TuckColors(
    background = Color(0xFFF3F7F4),
    surface = Color(0xFFFFFFFF),
    surfaceCard = Color(0xFFE8F0EA),
    surfaceElevated = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFDDE9E0),
    surfaceSubtle = Color(0xFFF0F5F2),
    accent = Color(0xFF2D6A4F),
    accentLight = Color(0xFF40916C),
    accentContainer = Color(0xFFE2EFE7),
    textPrimary = Color(0xFF122017),
    textSecondary = Color(0xFF4A6052),
    textMuted = Color(0xFF7D9585),
    textOnAccent = Color(0xFFFFFFFF),
    border = Color(0xFFD5E4D8),
    borderSubtle = Color(0xFFE2EEE5),
    isDark = false
)

val ForestDark = TuckColors(
    background = Color(0xFF101813),
    surface = Color(0xFF17231C),
    surfaceCard = Color(0xFF1E2E25),
    surfaceElevated = Color(0xFF26392E),
    surfaceVariant = Color(0xFF2E4537),
    surfaceSubtle = Color(0xFF131D17),
    accent = Color(0xFF52B788),
    accentLight = Color(0xFF74C69D),
    accentContainer = Color(0xFF1D3B2B),
    textPrimary = Color(0xFFECF5EE),
    textSecondary = Color(0xFFA3BCA9),
    textMuted = Color(0xFF6B8572),
    textOnAccent = Color(0xFF101813),
    border = Color(0xFF2A4033),
    borderSubtle = Color(0xFF203227),
    isDark = true
)

// 4. Cobalt Palettes
val CobaltLight = TuckColors(
    background = Color(0xFFF0F4F8),
    surface = Color(0xFFFFFFFF),
    surfaceCard = Color(0xFFE3ECF5),
    surfaceElevated = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFD6E3F0),
    surfaceSubtle = Color(0xFFF4F7FB),
    accent = Color(0xFF2563EB),
    accentLight = Color(0xFF3B82F6),
    accentContainer = Color(0xFFDBEAFE),
    textPrimary = Color(0xFF0E1726),
    textSecondary = Color(0xFF475569),
    textMuted = Color(0xFF8294AA),
    textOnAccent = Color(0xFFFFFFFF),
    border = Color(0xFFCFDDEB),
    borderSubtle = Color(0xFFDFE8F2),
    isDark = false
)

val CobaltDark = TuckColors(
    background = Color(0xFF0B132B),
    surface = Color(0xFF121E42),
    surfaceCard = Color(0xFF1A2B5E),
    surfaceElevated = Color(0xFF24397B),
    surfaceVariant = Color(0xFF2C448F),
    surfaceSubtle = Color(0xFF0E1836),
    accent = Color(0xFF38BDF8),
    accentLight = Color(0xFF60A5FA),
    accentContainer = Color(0xFF1E3A8A),
    textPrimary = Color(0xFFF0F6FF),
    textSecondary = Color(0xFF94A3B8),
    textMuted = Color(0xFF64748B),
    textOnAccent = Color(0xFF0B132B),
    border = Color(0xFF243A6B),
    borderSubtle = Color(0xFF1B2D54),
    isDark = true
)

// 5. Plum Palettes
val PlumLight = TuckColors(
    background = Color(0xFFFAF4F8),
    surface = Color(0xFFFFFFFF),
    surfaceCard = Color(0xFFF3E6EF),
    surfaceElevated = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEAD4E4),
    surfaceSubtle = Color(0xFFFDF7FB),
    accent = Color(0xFF8E3B68),
    accentLight = Color(0xFFA84B79),
    accentContainer = Color(0xFFFBEAF3),
    textPrimary = Color(0xFF230F21),
    textSecondary = Color(0xFF5E4057),
    textMuted = Color(0xFF8F6E87),
    textOnAccent = Color(0xFFFFFFFF),
    border = Color(0xFFEAD4E4),
    borderSubtle = Color(0xFFF2E2ED),
    isDark = false
)

val PlumDark = TuckColors(
    background = Color(0xFF1A0F1A),
    surface = Color(0xFF261626),
    surfaceCard = Color(0xFF331E33),
    surfaceElevated = Color(0xFF402640),
    surfaceVariant = Color(0xFF4D2D4D),
    surfaceSubtle = Color(0xFF201320),
    accent = Color(0xFFC26795),
    accentLight = Color(0xFFD982B0),
    accentContainer = Color(0xFF4A203E),
    textPrimary = Color(0xFFFAEFF8),
    textSecondary = Color(0xFFB89EB3),
    textMuted = Color(0xFF806B7C),
    textOnAccent = Color(0xFF1A0F1A),
    border = Color(0xFF422540),
    borderSubtle = Color(0xFF331B31),
    isDark = true
)

val LocalTuckColors = staticCompositionLocalOf { LinenLight }
val LocalTuckSpacing = staticCompositionLocalOf { TuckSpacing() }
val LocalTuckShapes = staticCompositionLocalOf { TuckShapes() }

fun getTuckColors(flavor: TuckThemeFlavor, isDark: Boolean): TuckColors {
    return when (flavor) {
        TuckThemeFlavor.LINEN -> if (isDark) LinenDark else LinenLight
        TuckThemeFlavor.NOIR -> if (isDark) NoirDark else NoirLight
        TuckThemeFlavor.FOREST -> if (isDark) ForestDark else ForestLight
        TuckThemeFlavor.COBALT -> if (isDark) CobaltDark else CobaltLight
        TuckThemeFlavor.PLUM -> if (isDark) PlumDark else PlumLight
    }
}
