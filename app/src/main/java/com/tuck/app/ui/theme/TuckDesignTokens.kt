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
import com.tuck.app.ui.theme.color.CobaltDarkPalette
import com.tuck.app.ui.theme.color.CobaltLightPalette
import com.tuck.app.ui.theme.color.ForestDarkPalette
import com.tuck.app.ui.theme.color.ForestLightPalette
import com.tuck.app.ui.theme.color.LinenDarkPalette
import com.tuck.app.ui.theme.color.LinenLightPalette
import com.tuck.app.ui.theme.color.LinenDarkPalette
import com.tuck.app.ui.theme.color.NoirDarkPalette
import com.tuck.app.ui.theme.color.NoirLightPalette
import com.tuck.app.ui.theme.color.ForestLightPalette
import com.tuck.app.ui.theme.color.ForestDarkPalette
import com.tuck.app.ui.theme.color.CobaltLightPalette
import com.tuck.app.ui.theme.color.CobaltDarkPalette
import com.tuck.app.ui.theme.color.PlumLightPalette
import com.tuck.app.ui.theme.color.PlumDarkPalette
import com.tuck.app.ui.theme.color.NoirLightPalette
import com.tuck.app.ui.theme.color.PaletteSlot
import com.tuck.app.ui.theme.color.PlumDarkPalette
import com.tuck.app.ui.theme.color.PlumLightPalette
import com.tuck.app.ui.theme.color.TuckPalette

/**
 * Curated Collection Tile Color with guaranteed WCAG 2.1 AA (>= 4.5:1) foreground contrast.
 */
data class CollectionColorEntry(
    val id: String,
    val name: String,
    val background: Color,
    val foreground: Color
)

/**
 * Maps a legacy color ID or string to a canonical [PaletteSlot].
 */
fun mapLegacyColorToSlot(stored: String?): PaletteSlot? {
    if (stored.isNullOrBlank()) return null
    val direct = PaletteSlot.fromString(stored)
    if (direct != null) return direct

    return when (stored.trim().lowercase()) {
        "coral", "terracotta", "orange", "crimson" -> PaletteSlot.PRIMARY_CORE
        "amber", "yellow" -> PaletteSlot.SECONDARY_CORE
        "mustard", "gold" -> PaletteSlot.TERTIARY_CORE
        "emerald", "mint", "green", "sage" -> PaletteSlot.PRIMARY_SOFT
        "teal", "cyan" -> PaletteSlot.SECONDARY_SOFT
        "sky", "indigo", "slate", "midnight", "blue", "denim" -> PaletteSlot.TERTIARY_DEEP
        "purple", "violet", "plum" -> PaletteSlot.PRIMARY_DEEP
        "berry", "rose", "pink" -> PaletteSlot.SECONDARY_DEEP
        else -> null
    }
}

/**
 * Resolves a [PaletteSlot] deterministically for a collection.
 * Adjacent items in default sort are dispersed across hues using prime multiplication.
 */
fun resolveCollectionSlot(storedColorId: String?, name: String = "", id: Long = 0): PaletteSlot {
    val mapped = mapLegacyColorToSlot(storedColorId)
    if (mapped != null) return mapped

    val rawSeed = if (name.isNotBlank()) name.trim().lowercase().hashCode().toLong() else id
    val positiveSeed = kotlin.math.abs(rawSeed)
    val slotIndex = ((positiveSeed * 5L) % PaletteSlot.entries.size).toInt()
    return PaletteSlot.entries[slotIndex]
}

/**
 * Resolves a collection color entry from a stored ID or name, using the provided or default palette.
 */
fun resolveCollectionColor(
    storedColorId: String?,
    name: String = "",
    id: Long = 0,
    palette: TuckPalette = LinenLightPalette
): CollectionColorEntry {
    val slot = resolveCollectionSlot(storedColorId, name, id)
    val entry = palette[slot]
    return CollectionColorEntry(
        id = slot.name.lowercase(),
        name = slot.displayName,
        background = entry.fill,
        foreground = entry.onFill
    )
}

/**
 * Backward compatibility 8-color saturated collection palette list for LinenLight.
 */
val DefaultCollectionPalette: List<CollectionColorEntry> by lazy {
    PaletteSlot.entries.map { slot ->
        val entry = LinenLightPalette[slot]
        CollectionColorEntry(
            id = slot.name.lowercase(),
            name = slot.displayName,
            background = entry.fill,
            foreground = entry.onFill
        )
    }
}

/**
 * Multi-hue Tuck Design Tokens.
 * Structured into Neutrals, Roles, and Palette while maintaining 100% backward compatibility.
 */
data class TuckColors(
    // Neutrals
    val canvas: Color,
    val surface: Color,
    val surfaceCard: Color,
    val surfaceElevated: Color,
    val surfaceVariant: Color,
    val surfaceSubtle: Color,
    val border: Color,
    val borderSubtle: Color,
    val dividerHairline: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val isDark: Boolean,

    // Primary & Accent
    val accent: Color,
    val accentLight: Color,
    val accentContainer: Color,
    val textOnAccent: Color,

    // Highlight & Badge
    val highlight: Color,
    val highlightText: Color,
    val badgeBackground: Color,
    val tileForeground: Color = textPrimary,

    // Roles
    /** Scrim behind media overlays and the share sheet. */
    val scrim: Color = Color(0xFF000000),
    /** Text and icons drawn on top of [scrim]. */
    val onScrim: Color = Color(0xFFFFFFFF),
    /** The favourite star. Warm in both themes, so it reads as a marked state. */
    val favorite: Color = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706),
    val destructive: Color = if (isDark) Color(0xFFF87171) else Color(0xFFDC2626),
    val warning: Color = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706),
    val success: Color = if (isDark) Color(0xFF4ADE80) else Color(0xFF16A34A),

    // Multi-Hue Coordinated Palette
    val palette: TuckPalette = if (isDark) NoirDarkPalette else LinenLightPalette
) {
    // Backward compatibility alias for background -> canvas
    val background: Color get() = canvas

    // Backward compatibility collectionPalette list
    val collectionPalette: List<CollectionColorEntry>
        get() = PaletteSlot.entries.map { slot ->
            val entry = palette[slot]
            CollectionColorEntry(
                id = slot.name.lowercase(),
                name = slot.displayName,
                background = entry.fill,
                foreground = entry.onFill
            )
        }
}

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

// 1. Linen Palettes (Signature Warm Paper & Terracotta)
val LinenLight = TuckColors(
    canvas = Color(0xFFFAF7F2),
    surface = Color(0xFFFFFFFF),
    surfaceCard = Color(0xFFF4EEE5),
    surfaceElevated = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEDE5D8),
    surfaceSubtle = Color(0xFFF8F3EC),
    border = Color(0xFFE8DFD3),
    borderSubtle = Color(0xFFF0E9DF),
    dividerHairline = Color(0xFFE8DFD3),
    textPrimary = Color(0xFF1C1917),
    textSecondary = Color(0xFF6E665E),
    textMuted = Color(0xFF9C9287),
    accent = LinenLightPalette[PaletteSlot.PRIMARY_CORE].fill,
    accentLight = Color(0xFFF07650),
    accentContainer = Color(0xFFFDEEE9),
    textOnAccent = LinenLightPalette[PaletteSlot.PRIMARY_CORE].onFill,
    highlight = Color(0xFFFEF08A),
    highlightText = Color(0xFF713F12),
    badgeBackground = Color(0xFFEDE5D8),
    isDark = false,
    palette = LinenLightPalette
)

val LinenDark = TuckColors(
    canvas = Color(0xFF181614),
    surface = Color(0xFF221F1C),
    surfaceCard = Color(0xFF2A2622),
    surfaceElevated = Color(0xFF322E29),
    surfaceVariant = Color(0xFF38332E),
    surfaceSubtle = Color(0xFF1F1C19),
    border = Color(0xFF3B352E),
    borderSubtle = Color(0xFF2B2621),
    dividerHairline = Color(0xFF3B352E),
    textPrimary = Color(0xFFF7F4EE),
    textSecondary = Color(0xFFB5ADA4),
    textMuted = Color(0xFF7C746B),
    accent = LinenDarkPalette[PaletteSlot.PRIMARY_CORE].fill,
    accentLight = Color(0xFFFFA080),
    accentContainer = Color(0xFF3D261E),
    textOnAccent = LinenDarkPalette[PaletteSlot.PRIMARY_CORE].onFill,
    highlight = Color(0xFF854D0E),
    highlightText = Color(0xFFFEF08A),
    badgeBackground = Color(0xFF38332E),
    isDark = true,
    palette = LinenDarkPalette
)

// 2. Noir Palettes (Signature Obsidian & Titanium)
val NoirDark = TuckColors(
    canvas = Color(0xFF0D0D0E),
    surface = Color(0xFF161619),
    surfaceCard = Color(0xFF1F1F24),
    surfaceElevated = Color(0xFF28282E),
    surfaceVariant = Color(0xFF2F2F37),
    surfaceSubtle = Color(0xFF121214),
    border = Color(0xFF2E2E38),
    borderSubtle = Color(0xFF22222A),
    dividerHairline = Color(0xFF2E2E38),
    textPrimary = Color(0xFFF4F4F6),
    textSecondary = Color(0xFFA2A2AD),
    textMuted = Color(0xFF6B6B76),
    accent = NoirDarkPalette[PaletteSlot.PRIMARY_CORE].fill,
    accentLight = Color(0xFFFF886E),
    accentContainer = Color(0xFF3A1E17),
    textOnAccent = NoirDarkPalette[PaletteSlot.PRIMARY_CORE].onFill,
    highlight = Color(0xFF854D0E),
    highlightText = Color(0xFFFEF08A),
    badgeBackground = Color(0xFF2F2F37),
    isDark = true,
    palette = NoirDarkPalette
)

val NoirLight = TuckColors(
    canvas = Color(0xFFF5F5F7),
    surface = Color(0xFFFFFFFF),
    surfaceCard = Color(0xFFECECEE),
    surfaceElevated = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE2E2E6),
    surfaceSubtle = Color(0xFFF9F9FA),
    border = Color(0xFFDCDCE2),
    borderSubtle = Color(0xFFE8E8EC),
    dividerHairline = Color(0xFFDCDCE2),
    textPrimary = Color(0xFF111113),
    textSecondary = Color(0xFF5A5A66),
    textMuted = Color(0xFF8C8C9A),
    accent = NoirLightPalette[PaletteSlot.PRIMARY_CORE].fill,
    accentLight = Color(0xFFFF6B4A),
    accentContainer = Color(0xFFFFEBE6),
    textOnAccent = NoirLightPalette[PaletteSlot.PRIMARY_CORE].onFill,
    highlight = Color(0xFFFEF08A),
    highlightText = Color(0xFF713F12),
    badgeBackground = Color(0xFFE2E2E6),
    isDark = false,
    palette = NoirLightPalette
)

// 3. Forest Palettes (Botanical Sage & Moss Pine)
val ForestLight = TuckColors(
    canvas = Color(0xFFF3F7F4),
    surface = Color(0xFFFFFFFF),
    surfaceCard = Color(0xFFE8F0EA),
    surfaceElevated = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFDCE8DF),
    surfaceSubtle = Color(0xFFF0F5F2),
    border = Color(0xFFD1E0D5),
    borderSubtle = Color(0xFFE3EDE6),
    dividerHairline = Color(0xFFD1E0D5),
    textPrimary = Color(0xFF111D14),
    textSecondary = Color(0xFF475E4D),
    textMuted = Color(0xFF789480),
    accent = ForestLightPalette[PaletteSlot.PRIMARY_CORE].fill,
    accentLight = Color(0xFF52B788),
    accentContainer = Color(0xFFE5F6EE),
    textOnAccent = ForestLightPalette[PaletteSlot.PRIMARY_CORE].onFill,
    highlight = Color(0xFFD9F99D),
    highlightText = Color(0xFF365314),
    badgeBackground = Color(0xFFDCE8DF),
    isDark = false,
    palette = ForestLightPalette
)

val ForestDark = TuckColors(
    canvas = Color(0xFF0F1712),
    surface = Color(0xFF17221A),
    surfaceCard = Color(0xFF1E2C22),
    surfaceElevated = Color(0xFF26372B),
    surfaceVariant = Color(0xFF2E4133),
    surfaceSubtle = Color(0xFF131D16),
    border = Color(0xFF2A3D30),
    borderSubtle = Color(0xFF1F2E24),
    dividerHairline = Color(0xFF2A3D30),
    textPrimary = Color(0xFFEEF7F1),
    textSecondary = Color(0xFFA5C2AC),
    textMuted = Color(0xFF6B8773),
    accent = ForestDarkPalette[PaletteSlot.PRIMARY_CORE].fill,
    accentLight = Color(0xFF74C69D),
    accentContainer = Color(0xFF1A3D2A),
    textOnAccent = ForestDarkPalette[PaletteSlot.PRIMARY_CORE].onFill,
    highlight = Color(0xFF4D7C0F),
    highlightText = Color(0xFFECFCCB),
    badgeBackground = Color(0xFF2E4133),
    isDark = true,
    palette = ForestDarkPalette
)

// 4. Cobalt Palettes (Blueprint Ice & Oceanic Navy)
val CobaltLight = TuckColors(
    canvas = Color(0xFFF2F6FB),
    surface = Color(0xFFFFFFFF),
    surfaceCard = Color(0xFFE5EEF8),
    surfaceElevated = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFD7E5F4),
    surfaceSubtle = Color(0xFFEEF4FA),
    border = Color(0xFFCADDF0),
    borderSubtle = Color(0xFFDEEAF7),
    dividerHairline = Color(0xFFCADDF0),
    textPrimary = Color(0xFF0F1B2C),
    textSecondary = Color(0xFF485E7C),
    textMuted = Color(0xFF7B93B2),
    accent = CobaltLightPalette[PaletteSlot.PRIMARY_CORE].fill,
    accentLight = Color(0xFF3B82F6),
    accentContainer = Color(0xFFEFF6FF),
    textOnAccent = CobaltLightPalette[PaletteSlot.PRIMARY_CORE].onFill,
    highlight = Color(0xFFBAE6FD),
    highlightText = Color(0xFF0369A1),
    badgeBackground = Color(0xFFD7E5F4),
    isDark = false,
    palette = CobaltLightPalette
)

val CobaltDark = TuckColors(
    canvas = Color(0xFF0C131D),
    surface = Color(0xFF131D2B),
    surfaceCard = Color(0xFF1B2739),
    surfaceElevated = Color(0xFF223247),
    surfaceVariant = Color(0xFF2A3C54),
    surfaceSubtle = Color(0xFF101925),
    border = Color(0xFF27384E),
    borderSubtle = Color(0xFF1B293A),
    dividerHairline = Color(0xFF27384E),
    textPrimary = Color(0xFFEFF5FC),
    textSecondary = Color(0xFFA1BBD9),
    textMuted = Color(0xFF65809E),
    accent = CobaltDarkPalette[PaletteSlot.PRIMARY_CORE].fill,
    accentLight = Color(0xFF60A5FA),
    accentContainer = Color(0xFF173054),
    textOnAccent = CobaltDarkPalette[PaletteSlot.PRIMARY_CORE].onFill,
    highlight = Color(0xFF0369A1),
    highlightText = Color(0xFFE0F2FE),
    badgeBackground = Color(0xFF2A3C54),
    isDark = true,
    palette = CobaltDarkPalette
)

// 5. Plum Palettes (Velvet Dusk & Twilight Mulberry)
val PlumLight = TuckColors(
    canvas = Color(0xFFF9F5F9),
    surface = Color(0xFFFFFFFF),
    surfaceCard = Color(0xFFF2E7F3),
    surfaceElevated = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEADBEC),
    surfaceSubtle = Color(0xFFF7EFF7),
    border = Color(0xFFE2CCE5),
    borderSubtle = Color(0xFFEEDEEF),
    dividerHairline = Color(0xFFE2CCE5),
    textPrimary = Color(0xFF241026),
    textSecondary = Color(0xFF69486E),
    textMuted = Color(0xFF9E77A4),
    accent = PlumLightPalette[PaletteSlot.PRIMARY_CORE].fill,
    accentLight = Color(0xFFA855F7),
    accentContainer = Color(0xFFFAF5FF),
    textOnAccent = PlumLightPalette[PaletteSlot.PRIMARY_CORE].onFill,
    highlight = Color(0xFFF5D0FE),
    highlightText = Color(0xFF86198F),
    badgeBackground = Color(0xFFEADBEC),
    isDark = false,
    palette = PlumLightPalette
)

val PlumDark = TuckColors(
    canvas = Color(0xFF150E18),
    surface = Color(0xFF1E1522),
    surfaceCard = Color(0xFF281C2E),
    surfaceElevated = Color(0xFF33243A),
    surfaceVariant = Color(0xFF3D2C45),
    surfaceSubtle = Color(0xFF1A111E),
    border = Color(0xFF3B2942),
    borderSubtle = Color(0xFF2A1D30),
    dividerHairline = Color(0xFF3B2942),
    textPrimary = Color(0xFFFAF3FC),
    textSecondary = Color(0xFFD1B4D9),
    textMuted = Color(0xFF8D6F96),
    accent = PlumDarkPalette[PaletteSlot.PRIMARY_CORE].fill,
    accentLight = Color(0xFFC084FC),
    accentContainer = Color(0xFF3D184E),
    textOnAccent = PlumDarkPalette[PaletteSlot.PRIMARY_CORE].onFill,
    highlight = Color(0xFF86198F),
    highlightText = Color(0xFFFAE8FF),
    badgeBackground = Color(0xFF3D2C45),
    isDark = true,
    palette = PlumDarkPalette
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
