package com.tuck.app.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tuck.app.domain.repository.AppTheme
import com.tuck.app.domain.repository.TuckThemeFlavor
import com.tuck.app.ui.components.TuckCategoryChip
import com.tuck.app.ui.components.TuckSectionHeader
import com.tuck.app.ui.components.TuckThemePreviewCard
import com.tuck.app.ui.theme.TuckTheme

@Composable
fun SettingsScreen(
    onNavigateToFilingRules: () -> Unit,
    onNavigateToVaultHealth: () -> Unit,
    onNavigateToTrash: () -> Unit,
    onNavigateBack: (() -> Unit)? = null,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val tuckColors = TuckTheme.colors
    val tuckShapes = TuckTheme.shapes
    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(
                context,
                "Weekly Memory is on, but notifications are blocked for Tuck",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    var showPaletteGallery by remember { mutableStateOf(false) }

    androidx.activity.compose.BackHandler(enabled = showPaletteGallery) {
        showPaletteGallery = false
    }

    if (showPaletteGallery) {
        com.tuck.app.ui.debug.PaletteGalleryScreen(
            onNavigateBack = { showPaletteGallery = false }
        )
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(tuckColors.background),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (onNavigateBack != null) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = tuckColors.textPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Column {
                        Text(
                            text = "Settings",
                            style = TuckTheme.typography.displayLarge,
                            color = tuckColors.textPrimary
                        )
                        Text(
                            text = "Personalize your Tuck experience and digital vault.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = tuckColors.textSecondary
                        )
                    }
                }
            }
        }

        // Section: "Make Tuck yours" (Theme Flavor Previews)
        item {
            Card(
                shape = tuckShapes.large,
                colors = CardDefaults.cardColors(containerColor = tuckColors.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, tuckColors.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Palette,
                            contentDescription = null,
                            tint = tuckColors.accent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Make Tuck yours",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = tuckColors.textPrimary
                        )
                    }
                    Text(
                        text = "Choose a visual theme designed with intentional color harmony.",
                        style = MaterialTheme.typography.bodySmall,
                        color = tuckColors.textSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 5 Theme Preview Cards
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        TuckThemeFlavor.values().forEach { flavor ->
                            TuckThemePreviewCard(
                                flavor = flavor,
                                isSelected = uiState.settings.themeFlavor == flavor,
                                onClick = { viewModel.setThemeFlavor(flavor) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Light / Dark / System Mode Selector
                    Text(
                        text = "Appearance Mode",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = tuckColors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "System" to AppTheme.SYSTEM,
                            "Light" to AppTheme.LIGHT,
                            "Dark" to AppTheme.DARK
                        ).forEach { (label, theme) ->
                            TuckCategoryChip(
                                label = label,
                                isSelected = uiState.settings.theme == theme,
                                onClick = { viewModel.setTheme(theme) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Theme Palette Gallery Launcher (Debug / Review)
                    Surface(
                        shape = tuckShapes.medium,
                        color = tuckColors.surfaceCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, tuckColors.borderSubtle),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showPaletteGallery = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Inspect Color Palettes",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = tuckColors.textPrimary
                                )
                                Text(
                                    text = "Review 8-hue slots, contrast ratios & CVD simulations",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = tuckColors.textSecondary
                                )
                            }
                            Text(
                                text = "View →",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = tuckColors.accent
                            )
                        }
                    }
                }
            }
        }

        // Section: On-Device Intelligence
        item {
            Card(
                shape = tuckShapes.large,
                colors = CardDefaults.cardColors(containerColor = tuckColors.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, tuckColors.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Tuck Intelligence",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = tuckColors.textPrimary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SettingsSwitchRow(
                        title = "On-Device OCR",
                        subtitle = "Recognize and index text inside screenshots & images",
                        checked = uiState.settings.ocrEnabled,
                        onCheckedChange = { viewModel.setOcrEnabled(it) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    SettingsSwitchRow(
                        title = "Auto-Categorize",
                        subtitle = "Automatically organize content into smart folders",
                        checked = uiState.settings.autoCategorizeEnabled,
                        onCheckedChange = { viewModel.setAutoCategorizeEnabled(it) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    SettingsSwitchRow(
                        title = "Save Community Comments",
                        subtitle = "Index top discussions from Reddit and social posts for offline search",
                        checked = uiState.settings.saveCommentsEnabled,
                        onCheckedChange = { viewModel.setSaveCommentsEnabled(it) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    SettingsActionRow(
                        icon = Icons.Filled.Rule,
                        title = "Auto-filing rules",
                        subtitle = "Send matching saves straight to a collection",
                        onClick = onNavigateToFilingRules
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    SettingsSwitchRow(
                        title = "Weekly Memory",
                        subtitle = "Resurface one forgotten save each week. Off by default.",
                        checked = uiState.settings.memoryResurfacingEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            viewModel.setMemoryResurfacingEnabled(enabled)
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    SettingsSwitchRow(
                        title = "Wi-Fi Only for Previews",
                        subtitle = "Fetch rich link previews and metadata only on Wi-Fi",
                        checked = uiState.settings.wifiOnlyMetadata,
                        onCheckedChange = { viewModel.setWifiOnlyMetadata(it) }
                    )
                }
            }
        }

        // Section: Storage & Vault Management
        item {
            Card(
                shape = tuckShapes.large,
                colors = CardDefaults.cardColors(containerColor = tuckColors.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, tuckColors.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Vault & Storage",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = tuckColors.textPrimary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SettingsActionRow(
                        icon = Icons.Filled.HealthAndSafety,
                        title = "Vault health",
                        subtitle = "Check that every save is intact and searchable",
                        onClick = onNavigateToVaultHealth
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    SettingsActionRow(
                        icon = Icons.Filled.Download,
                        title = "Export Full Vault Archive (.tuck)",
                        subtitle = "Complete archive with photos, PDFs, and notes",
                        onClick = {
                            viewModel.exportFullArchive { path ->
                                Toast.makeText(context, "Archive exported: $path", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    SettingsActionRow(
                        icon = Icons.Filled.Download,
                        title = "Export Vault JSON",
                        subtitle = "Lightweight text and metadata backup",
                        onClick = {
                            viewModel.exportVault { path ->
                                Toast.makeText(context, "Backup exported: $path", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    SettingsActionRow(
                        icon = Icons.Filled.Storage,
                        title = "Clear Image Cache",
                        subtitle = "Free temporary cached preview thumbnails",
                        onClick = {
                            viewModel.clearCache()
                            Toast.makeText(context, "Cache cleared", Toast.LENGTH_SHORT).show()
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    SettingsActionRow(
                        icon = Icons.Filled.Delete,
                        title = "Trash & Recovery",
                        subtitle = "${uiState.trashedCount} items in trash",
                        onClick = onNavigateToTrash
                    )
                }
            }
        }

        // Section: Privacy & Security
        item {
            Surface(
                shape = tuckShapes.medium,
                color = tuckColors.accentContainer.copy(alpha = 0.5f),
                border = androidx.compose.foundation.BorderStroke(1.dp, tuckColors.accent.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shield,
                        contentDescription = null,
                        tint = tuckColors.accent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "100% Local-First & Private",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = tuckColors.textPrimary
                        )
                        Text(
                            text = "Your data, OCR text, and saved content live entirely on this device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = tuckColors.textSecondary
                        )
                    }
                }
            }
        }

        // Section: About
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Tuck v1.0.0",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = tuckColors.textMuted
                )
                Text(
                    text = "Tuck it away. Find it later.",
                    style = MaterialTheme.typography.bodySmall,
                    color = tuckColors.textMuted
                )
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val tuckColors = TuckTheme.colors

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = tuckColors.textPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = tuckColors.textSecondary
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = tuckColors.textOnAccent,
                checkedTrackColor = tuckColors.accent,
                uncheckedThumbColor = tuckColors.textMuted,
                uncheckedTrackColor = tuckColors.surfaceCard
            )
        )
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val tuckColors = TuckTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(TuckTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(tuckColors.surfaceCard),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tuckColors.accent,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = tuckColors.textPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = tuckColors.textSecondary
                )
            }
        }

        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = tuckColors.textMuted,
            modifier = Modifier.size(18.dp)
        )
    }
}
