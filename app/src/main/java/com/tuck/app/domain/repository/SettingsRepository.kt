package com.tuck.app.domain.repository

import kotlinx.coroutines.flow.Flow

enum class AppTheme {
    SYSTEM,
    LIGHT,
    DARK
}

enum class TuckThemeFlavor(val displayName: String, val tagline: String) {
    LINEN("Linen", "Warm & personal"),
    NOIR("Noir", "Quiet & luxurious"),
    FOREST("Forest", "Calm & natural"),
    COBALT("Cobalt", "Focused & technical"),
    PLUM("Plum", "Creative & expressive")
}

data class AppSettings(
    val theme: AppTheme = AppTheme.SYSTEM,
    val themeFlavor: TuckThemeFlavor = TuckThemeFlavor.LINEN,
    val ocrEnabled: Boolean = true,
    val autoCategorizeEnabled: Boolean = true,
    val saveCommentsEnabled: Boolean = true,
    val wifiOnlyMetadata: Boolean = false,
    val autoDismissShareSeconds: Int = 3
)

data class StorageUsage(
    val imagesSizeBytes: Long,
    val pdfsSizeBytes: Long,
    val thumbnailsSizeBytes: Long,
    val cacheSizeBytes: Long,
    val totalSizeBytes: Long
)

interface SettingsRepository {
    fun getSettings(): Flow<AppSettings>
    suspend fun updateTheme(theme: AppTheme)
    suspend fun updateThemeFlavor(flavor: TuckThemeFlavor)
    suspend fun setOcrEnabled(enabled: Boolean)
    suspend fun setAutoCategorizeEnabled(enabled: Boolean)
    suspend fun setSaveCommentsEnabled(enabled: Boolean)
    suspend fun setWifiOnlyMetadata(wifiOnly: Boolean)
    suspend fun getStorageUsage(): StorageUsage
    suspend fun clearCache(): Boolean
}
