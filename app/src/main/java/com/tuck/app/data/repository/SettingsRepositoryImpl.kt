package com.tuck.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.tuck.app.data.local.storage.FileStorageService
import com.tuck.app.domain.repository.AppSettings
import com.tuck.app.domain.repository.AppTheme
import com.tuck.app.domain.repository.SettingsRepository
import com.tuck.app.domain.repository.StorageUsage
import com.tuck.app.domain.repository.TuckThemeFlavor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tuck_settings")

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fileStorageService: FileStorageService
) : SettingsRepository {

    private object PreferencesKeys {
        val THEME = stringPreferencesKey("theme")
        val THEME_FLAVOR = stringPreferencesKey("theme_flavor")
        val OCR_ENABLED = booleanPreferencesKey("ocr_enabled")
        val AUTO_CATEGORIZE_ENABLED = booleanPreferencesKey("auto_categorize_enabled")
        val SAVE_COMMENTS_ENABLED = booleanPreferencesKey("save_comments_enabled")
        val WIFI_ONLY_METADATA = booleanPreferencesKey("wifi_only_metadata")
        val MEMORY_RESURFACING_ENABLED = booleanPreferencesKey("memory_resurfacing_enabled")
    }

    override fun getSettings(): Flow<AppSettings> {
        return context.dataStore.data.map { preferences ->
            val themeString = preferences[PreferencesKeys.THEME] ?: AppTheme.SYSTEM.name
            val theme = try {
                AppTheme.valueOf(themeString)
            } catch (e: Exception) {
                AppTheme.SYSTEM
            }
            val flavorString = preferences[PreferencesKeys.THEME_FLAVOR] ?: TuckThemeFlavor.LINEN.name
            val flavor = try {
                TuckThemeFlavor.valueOf(flavorString)
            } catch (e: Exception) {
                TuckThemeFlavor.LINEN
            }
            val ocrEnabled = preferences[PreferencesKeys.OCR_ENABLED] ?: true
            val autoCategorize = preferences[PreferencesKeys.AUTO_CATEGORIZE_ENABLED] ?: true
            val saveComments = preferences[PreferencesKeys.SAVE_COMMENTS_ENABLED] ?: true
            val wifiOnly = preferences[PreferencesKeys.WIFI_ONLY_METADATA] ?: false
            val memoryResurfacing = preferences[PreferencesKeys.MEMORY_RESURFACING_ENABLED] ?: false

            AppSettings(
                theme = theme,
                themeFlavor = flavor,
                ocrEnabled = ocrEnabled,
                autoCategorizeEnabled = autoCategorize,
                saveCommentsEnabled = saveComments,
                wifiOnlyMetadata = wifiOnly,
                memoryResurfacingEnabled = memoryResurfacing
            )
        }
    }

    override suspend fun setMemoryResurfacingEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.MEMORY_RESURFACING_ENABLED] = enabled
        }
    }

    override suspend fun updateTheme(theme: AppTheme) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME] = theme.name
        }
    }

    override suspend fun updateThemeFlavor(flavor: TuckThemeFlavor) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_FLAVOR] = flavor.name
        }
    }

    override suspend fun setOcrEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.OCR_ENABLED] = enabled
        }
    }

    override suspend fun setAutoCategorizeEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_CATEGORIZE_ENABLED] = enabled
        }
    }

    override suspend fun setSaveCommentsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SAVE_COMMENTS_ENABLED] = enabled
        }
    }

    override suspend fun setWifiOnlyMetadata(wifiOnly: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.WIFI_ONLY_METADATA] = wifiOnly
        }
    }

    override suspend fun getStorageUsage(): StorageUsage {
        return fileStorageService.getStorageUsage()
    }

    override suspend fun reclaimSpace(): Long = fileStorageService.reclaimSpace()

    override suspend fun clearCache(): Boolean {
        return fileStorageService.clearCache()
    }
}
