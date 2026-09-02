package com.tuck.app.ui.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tuck.app.ui.theme.LinenDark
import com.tuck.app.ui.theme.LinenLight
import com.tuck.app.ui.theme.NoirDark
import com.tuck.app.ui.theme.NoirLight
import com.tuck.app.ui.theme.TuckColors
import com.tuck.app.ui.theme.TuckTheme
import com.tuck.app.ui.theme.color.LinenLightPalette
import com.tuck.app.ui.theme.color.NoirDarkPalette
import com.tuck.app.ui.theme.color.Oklch
import com.tuck.app.ui.theme.color.PaletteEntry
import com.tuck.app.ui.theme.color.PaletteSlot
import com.tuck.app.ui.theme.color.TuckPalette
import com.tuck.app.ui.theme.color.TuckPaletteBuilder
import com.tuck.app.ui.theme.color.toOklch

/**
 * Viénot 1999 Color Vision Deficiency Simulation helper for Compose [Color].
 */
enum class CvdSimulationType(val label: String, val subtitle: String) {
    NORMAL("Normal Vision", "Standard full-spectrum human trichromatic vision"),
    DEUTERANOPIA("Deuteranopia", "Green-cone deficiency (~8% of males)"),
    PROTANOPIA("Protanopia", "Red-cone deficiency (~1.5% of males)"),
    TRITANOPIA("Tritanopia", "Blue-cone deficiency (~0.01% of population)")
}

fun simulateCvd(color: Color, type: CvdSimulationType): Color {
    if (type == CvdSimulationType.NORMAL) return color

    val rLin = Oklch.srgbToLinear(color.red.toDouble())
    val gLin = Oklch.srgbToLinear(color.green.toDouble())
    val bLin = Oklch.srgbToLinear(color.blue.toDouble())

    val (rSim, gSim, bSim) = when (type) {
        CvdSimulationType.NORMAL -> Triple(rLin, gLin, bLin)
        CvdSimulationType.DEUTERANOPIA -> Triple(
            0.625 * rLin + 0.375 * gLin + 0.0 * bLin,
            0.700 * rLin + 0.300 * gLin + 0.0 * bLin,
            0.0 * rLin + 0.300 * gLin + 0.700 * bLin
        )
        CvdSimulationType.PROTANOPIA -> Triple(
            0.56667 * rLin + 0.43333 * gLin + 0.0 * bLin,
            0.55833 * rLin + 0.44167 * gLin + 0.0 * bLin,
            0.0 * rLin + 0.24167 * gLin + 0.75833 * bLin
        )
        CvdSimulationType.TRITANOPIA -> Triple(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaletteGalleryScreen(
    onNavigateBack: () -> Unit
) {
    var selectedThemeIndex by remember { mutableStateOf(0) }

    val themes = listOf(
        "Linen Light" to (com.tuck.app.ui.theme.LinenLight to com.tuck.app.ui.theme.color.LinenLightPalette),
        "Linen Dark" to (com.tuck.app.ui.theme.LinenDark to com.tuck.app.ui.theme.color.LinenDarkPalette),
        "Noir Dark" to (com.tuck.app.ui.theme.NoirDark to com.tuck.app.ui.theme.color.NoirDarkPalette),
        "Noir Light" to (com.tuck.app.ui.theme.NoirLight to com.tuck.app.ui.theme.color.NoirLightPalette),
        "Forest Light" to (com.tuck.app.ui.theme.ForestLight to com.tuck.app.ui.theme.color.ForestLightPalette),
        "Forest Dark" to (com.tuck.app.ui.theme.ForestDark to com.tuck.app.ui.theme.color.ForestDarkPalette),
        "Cobalt Light" to (com.tuck.app.ui.theme.CobaltLight to com.tuck.app.ui.theme.color.CobaltLightPalette),
        "Cobalt Dark" to (com.tuck.app.ui.theme.CobaltDark to com.tuck.app.ui.theme.color.CobaltDarkPalette),
        "Plum Light" to (com.tuck.app.ui.theme.PlumLight to com.tuck.app.ui.theme.color.PlumLightPalette),
        "Plum Dark" to (com.tuck.app.ui.theme.PlumDark to com.tuck.app.ui.theme.color.PlumDarkPalette)
    )

    val currentThemePair = themes[selectedThemeIndex].second
    val currentColors = currentThemePair.first
    val currentPalette = currentThemePair.second

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Palette & Theme Gallery",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = currentColors.textPrimary
                        )
                        Text(
                            text = "5 Multi-Hue Families · Light & Dark Verified",
                            style = MaterialTheme.typography.labelSmall,
                            color = currentColors.textSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = currentColors.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = currentColors.canvas
                )
            )
        },
        containerColor = currentColors.canvas
    ) { innerPadding ->
        LazyColumn(
            contentPadding = innerPadding,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Theme Mode Selector Horizontal Scrollable Chips
            item {
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(themes.size) { index ->
                        val (label, _) = themes[index]
                        val isSelected = selectedThemeIndex == index
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) currentColors.accent else currentColors.surfaceCard,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) currentColors.accent else currentColors.border
                            ),
                            modifier = Modifier.clickable { selectedThemeIndex = index }
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) currentColors.textOnAccent else currentColors.textPrimary,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 14.dp)
                            )
                        }
                    }
                }
            }

            // Section 1: 8 Multi-Hue Swatch Tiles
            item {
                Text(
                    text = "8-HUE COORDINATED PALETTE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = currentColors.textSecondary,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    currentPalette.entries.forEach { entry ->
                        PaletteSwatchTile(
                            entry = entry,
                            canvas = currentColors.canvas,
                            shapes = TuckTheme.shapes
                        )
                    }
                }
            }

            // Section 2: Colorblind Simulation Rows
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "COLOR VISION DEFICIENCY SIMULATION",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = currentColors.textSecondary,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                CvdSimulationType.entries.forEach { cvdType ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = currentColors.surfaceCard),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = cvdType.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = currentColors.textPrimary
                                )
                                Text(
                                    text = cvdType.subtitle.substringBefore(" ("),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = currentColors.textSecondary
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            // 8 Simulated Color Swatches
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                currentPalette.entries.forEach { entry ->
                                    val simFill = simulateCvd(entry.fill, cvdType)
                                    val simOnFill = simulateCvd(entry.onFill, cvdType)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(simFill),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = entry.slot.displayName.take(1),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Black,
                                            color = simOnFill
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 3: Neutral Ramp
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "NEUTRAL RAMP",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = currentColors.textSecondary,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = currentColors.surfaceCard),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        NeutralRow(label = "Canvas (Base)", color = currentColors.canvas, textColor = currentColors.textPrimary)
                        NeutralRow(label = "Surface", color = currentColors.surface, textColor = currentColors.textPrimary)
                        NeutralRow(label = "Surface Card", color = currentColors.surfaceCard, textColor = currentColors.textPrimary)
                        NeutralRow(label = "Surface Elevated", color = currentColors.surfaceElevated, textColor = currentColors.textPrimary)
                        NeutralRow(label = "Surface Variant", color = currentColors.surfaceVariant, textColor = currentColors.textPrimary)
                        NeutralRow(label = "Border", color = currentColors.border, textColor = currentColors.textPrimary)
                        NeutralRow(label = "Text Primary", color = currentColors.textPrimary, textColor = currentColors.canvas)
                        NeutralRow(label = "Text Secondary", color = currentColors.textSecondary, textColor = currentColors.canvas)
                        NeutralRow(label = "Text Muted", color = currentColors.textMuted, textColor = currentColors.canvas)
                    }
                }
            }

            // Section 4: Role Colors
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "FUNCTIONAL ROLE COLORS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = currentColors.textSecondary,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RoleCard(label = "Destructive", color = currentColors.destructive, modifier = Modifier.weight(1f))
                    RoleCard(label = "Warning", color = currentColors.warning, modifier = Modifier.weight(1f))
                    RoleCard(label = "Success", color = currentColors.success, modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun PaletteSwatchTile(
    entry: PaletteEntry,
    canvas: Color,
    shapes: com.tuck.app.ui.theme.TuckShapes
) {
    val fgRatio = TuckPaletteBuilder.contrastRatio(entry.fill, entry.onFill)
    val canvasRatio = TuckPaletteBuilder.contrastRatio(entry.fill, canvas)
    val oklch = entry.oklch
    val hex = String.format("#%06X", 0xFFFFFF and entry.fill.toArgb())

    Card(
        shape = shapes.medium,
        colors = CardDefaults.cardColors(containerColor = entry.fill),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = entry.slot.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = entry.onFill
                )
                Text(
                    text = "$hex · L=${String.format("%.2f", oklch.l)} C=${String.format("%.2f", oklch.c)} H=${String.format("%.0f", oklch.h)}°",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = entry.onFill.copy(alpha = 0.85f)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Text: ${String.format("%.1f", fgRatio)}:1",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = entry.onFill
                )
                Text(
                    text = "Tile: ${String.format("%.1f", canvasRatio)}:1",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 10.sp,
                    color = entry.onFill.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun NeutralRow(
    label: String,
    color: Color,
    textColor: Color
) {
    val hex = String.format("#%06X", 0xFFFFFF and color.toArgb())
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(1.dp, TuckTheme.colors.border, CircleShape)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
        Text(
            text = hex,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun RoleCard(
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = TuckTheme.colors.onScrim
        )
    }
}
