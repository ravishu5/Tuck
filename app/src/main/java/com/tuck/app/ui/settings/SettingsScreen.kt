package com.tuck.app.ui.settings

import androidx.compose.ui.res.stringResource
import com.tuck.app.R

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
import androidx.compose.foundation.layout.fillMaxHeight
import com.tuck.app.ui.theme.color.PaletteSlot
import com.tuck.app.domain.repository.StorageUsage
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.TextButton

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
                context.getString(R.string.setting_notifications_blocked),
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
                                contentDescription = stringResource(R.string.collections_back),
                                tint = tuckColors.textPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.home_settings),
                            style = TuckTheme.typography.displayLarge,
                            color = tuckColors.textPrimary
                        )
                        Text(
                            text = stringResource(R.string.settings_personalize_your_tuck_experience_and_digital),
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
                            text = stringResource(R.string.settings_make_tuck_yours),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = tuckColors.textPrimary
                        )
                    }
                    Text(
                        text = stringResource(R.string.settings_choose_a_visual_theme_designed_with),
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
                        text = stringResource(R.string.home_appearance_mode),
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
                            stringResource(R.string.theme_system) to AppTheme.SYSTEM,
                            stringResource(R.string.theme_light) to AppTheme.LIGHT,
                            stringResource(R.string.theme_dark) to AppTheme.DARK
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
                                    text = stringResource(R.string.settings_inspect_color_palettes),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = tuckColors.textPrimary
                                )
                                Text(
                                    text = stringResource(R.string.settings_review_8_hue_slots_contrast_ratios),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = tuckColors.textSecondary
                                )
                            }
                            Text(
                                text = stringResource(R.string.settings_view),
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
                        text = stringResource(R.string.settings_tuck_intelligence),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = tuckColors.textPrimary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SettingsSwitchRow(
                        title = stringResource(R.string.setting_ocr),
                        subtitle = stringResource(R.string.setting_ocr_sub),
                        checked = uiState.settings.ocrEnabled,
                        onCheckedChange = { viewModel.setOcrEnabled(it) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    SettingsSwitchRow(
                        title = stringResource(R.string.setting_auto_categorize),
                        subtitle = stringResource(R.string.setting_auto_categorize_sub),
                        checked = uiState.settings.autoCategorizeEnabled,
                        onCheckedChange = { viewModel.setAutoCategorizeEnabled(it) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    SettingsSwitchRow(
                        title = stringResource(R.string.setting_save_comments),
                        subtitle = stringResource(R.string.setting_save_comments_sub),
                        checked = uiState.settings.saveCommentsEnabled,
                        onCheckedChange = { viewModel.setSaveCommentsEnabled(it) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    SettingsActionRow(
                        icon = Icons.Filled.Rule,
                        title = stringResource(R.string.rules_auto_filing_rules),
                        subtitle = stringResource(R.string.setting_rules_sub),
                        onClick = onNavigateToFilingRules
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    SettingsSwitchRow(
                        title = stringResource(R.string.setting_weekly_memory),
                        subtitle = stringResource(R.string.setting_weekly_memory_sub),
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
                        title = stringResource(R.string.setting_wifi_only),
                        subtitle = stringResource(R.string.setting_wifi_only_sub),
                        checked = uiState.settings.wifiOnlyMetadata,
                        onCheckedChange = { viewModel.setWifiOnlyMetadata(it) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    SettingsSwitchRow(
                        title = stringResource(R.string.setting_transcribe),
                        subtitle = stringResource(R.string.setting_transcribe_sub),
                        checked = uiState.settings.transcribeVoiceNotes,
                        onCheckedChange = { viewModel.setTranscribeVoiceNotes(it) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    SettingsSwitchRow(
                        title = stringResource(R.string.setting_deep_capture),
                        subtitle = stringResource(R.string.setting_deep_capture_sub),
                        checked = uiState.settings.deepCaptureEnabled,
                        onCheckedChange = { viewModel.setDeepCaptureEnabled(it) }
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
                        text = stringResource(R.string.settings_vault_storage),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = tuckColors.textPrimary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SettingsActionRow(
                        icon = Icons.Filled.HealthAndSafety,
                        title = stringResource(R.string.health_vault_health),
                        subtitle = stringResource(R.string.setting_vault_health_sub),
                        onClick = onNavigateToVaultHealth
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    SettingsActionRow(
                        icon = Icons.Filled.Download,
                        title = stringResource(R.string.setting_export_archive),
                        subtitle = stringResource(R.string.setting_export_archive_sub),
                        onClick = {
                            viewModel.exportFullArchive { path ->
                                Toast.makeText(context, "Archive exported: $path", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    SettingsActionRow(
                        icon = Icons.Filled.Download,
                        title = stringResource(R.string.setting_export_json),
                        subtitle = stringResource(R.string.setting_export_json_sub),
                        onClick = {
                            viewModel.exportVault { path ->
                                Toast.makeText(context, "Backup exported: $path", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    StorageBreakdown(
                        usage = uiState.storageUsage,
                        onReclaim = {
                            viewModel.reclaimSpace { freed ->
                                Toast.makeText(
                                    context,
                                    if (freed > 0) {
                                    context.getString(R.string.setting_freed, formatBytes(freed))
                                } else {
                                    context.getString(R.string.setting_nothing_to_reclaim)
                                },
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    SettingsActionRow(
                        icon = Icons.Filled.Delete,
                        title = stringResource(R.string.setting_trash_recovery),
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
                            text = stringResource(R.string.settings_100_local_first_private),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = tuckColors.textPrimary
                        )
                        Text(
                            text = stringResource(R.string.settings_your_data_ocr_text_and_saved),
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
                    text = stringResource(R.string.settings_tuck_v1_0_0),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = tuckColors.textMuted
                )
                Text(
                    text = stringResource(R.string.settings_tuck_it_away_find_it_later),
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


/**
 * Where the space actually went, and what can be freed.
 *
 * A competitor's user watched a folder quietly fill their disk and called it money lost.
 * A single total does not answer "what is taking the room" - a breakdown does, and it
 * makes clear that only the regenerable half is ever offered for deletion.
 */
@Composable
private fun StorageBreakdown(
    usage: StorageUsage,
    onReclaim: () -> Unit
) {
    val tuckColors = TuckTheme.colors
    val palette = tuckColors.palette

    val segments = listOf(
        Triple(stringResource(R.string.storage_images), usage.imagesSizeBytes, palette[PaletteSlot.PRIMARY_CORE].fill),
        Triple(stringResource(R.string.storage_pdfs), usage.pdfsSizeBytes, palette[PaletteSlot.SECONDARY_CORE].fill),
        Triple(stringResource(R.string.storage_documents), usage.documentsSizeBytes, palette[PaletteSlot.TERTIARY_CORE].fill),
        Triple(stringResource(R.string.storage_previews), usage.thumbnailsSizeBytes, palette[PaletteSlot.PRIMARY_SOFT].fill),
        Triple(stringResource(R.string.storage_cache), usage.cacheSizeBytes, palette[PaletteSlot.SECONDARY_SOFT].fill)
    ).filter { it.second > 0 }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = formatBytes(usage.totalSizeBytes),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = tuckColors.textPrimary
        )
        Text(
            text = stringResource(R.string.settings_used_on_this_device),
            style = MaterialTheme.typography.bodySmall,
            color = tuckColors.textMuted
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (segments.isEmpty()) {
            Text(
                text = stringResource(R.string.settings_nothing_stored_yet),
                style = MaterialTheme.typography.bodySmall,
                color = tuckColors.textMuted
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(tuckColors.surfaceVariant)
            ) {
                segments.forEach { (_, bytes, color) ->
                    Box(
                        modifier = Modifier
                            .weight(bytes.toFloat())
                            .fillMaxHeight()
                            .background(color)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            segments.forEach { (label, bytes, color) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    Spacer(modifier = Modifier.width(9.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = tuckColors.textSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = formatBytes(bytes),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = tuckColors.textPrimary
                    )
                }
            }
        }

        if (usage.reclaimableBytes > 0) {
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(onClick = onReclaim, contentPadding = PaddingValues(0.dp)) {
                Text(
                    text = stringResource(R.string.reclaim_bytes, formatBytes(usage.reclaimableBytes)),
                    fontWeight = FontWeight.Bold,
                    color = tuckColors.accent
                )
            }
            Text(
                text = stringResource(R.string.settings_removes_previews_and_cache_only_your),
                style = MaterialTheme.typography.labelSmall,
                color = tuckColors.textMuted
            )
        }
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_073_741_824 -> "%.2f GB".format(bytes / 1_073_741_824.0)
    bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
    bytes >= 1024 -> "%.0f KB".format(bytes / 1024.0)
    else -> "$bytes B"
}
