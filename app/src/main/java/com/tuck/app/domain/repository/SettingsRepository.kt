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
    /**
     * Tier 2 capture: render pages in a headless WebView when a plain fetch returns a shell.
     * Off by default — it costs memory, seconds and battery, and it loads pages as the reader.
     */
    val deepCaptureEnabled: Boolean = false,
    /** Transcribe voice notes on device so their words reach search. */
    val transcribeVoiceNotes: Boolean = true,
    /** Weekly resurfacing notification. Opt-in: nothing is posted unless the user turns it on. */
    val memoryResurfacingEnabled: Boolean = false,
    val autoDismissShareSeconds: Int = 3
)

data class StorageUsage(
    val imagesSizeBytes: Long,
    val pdfsSizeBytes: Long,
    val documentsSizeBytes: Long = 0L,
    val thumbnailsSizeBytes: Long,
    val cacheSizeBytes: Long,
    val totalSizeBytes: Long
) {
    /**
     * What can be freed without losing a save.
     *
     * Thumbnails and cache are both regenerable from the originals; images, PDFs and
     * documents are the saves themselves and are never counted here.
     */
    val reclaimableBytes: Long get() = thumbnailsSizeBytes + cacheSizeBytes
}

interface SettingsRepository {
    fun getSettings(): Flow<AppSettings>
    suspend fun updateTheme(theme: AppTheme)
    suspend fun updateThemeFlavor(flavor: TuckThemeFlavor)
    suspend fun setOcrEnabled(enabled: Boolean)
    suspend fun setAutoCategorizeEnabled(enabled: Boolean)
    suspend fun setSaveCommentsEnabled(enabled: Boolean)
    suspend fun setWifiOnlyMetadata(wifiOnly: Boolean)
    suspend fun setDeepCaptureEnabled(enabled: Boolean)
    suspend fun setTranscribeVoiceNotes(enabled: Boolean)
    suspend fun setMemoryResurfacingEnabled(enabled: Boolean)
    suspend fun getStorageUsage(): StorageUsage
    suspend fun clearCache(): Boolean
    /** Frees regenerable data only; returns the bytes actually recovered. */
    suspend fun reclaimSpace(): Long
}
