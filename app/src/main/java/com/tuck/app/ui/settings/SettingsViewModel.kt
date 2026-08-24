package com.tuck.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuck.app.domain.repository.AppSettings
import com.tuck.app.domain.repository.AppTheme
import com.tuck.app.domain.repository.SavedItemRepository
import com.tuck.app.domain.repository.SettingsRepository
import com.tuck.app.domain.repository.StorageUsage
import com.tuck.app.data.local.storage.VaultBackupService
import com.tuck.app.processing.MemoryNotificationScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val storageUsage: StorageUsage = StorageUsage(0, 0, 0, 0, 0),
    val trashedCount: Int = 0
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val savedItemRepository: SavedItemRepository,
    private val vaultBackupService: VaultBackupService,
    private val memoryNotificationScheduler: MemoryNotificationScheduler
) : ViewModel() {

    private val _storageUsage = MutableStateFlow(StorageUsage(0, 0, 0, 0, 0))

    init {
        refreshStorageUsage()
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.getSettings(),
        _storageUsage,
        savedItemRepository.getTrashedItems()
    ) { settings, storage, trashed ->
        SettingsUiState(
            settings = settings,
            storageUsage = storage,
            trashedCount = trashed.size
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch {
            settingsRepository.updateTheme(theme)
        }
    }

    fun setThemeFlavor(flavor: com.tuck.app.domain.repository.TuckThemeFlavor) {
        viewModelScope.launch {
            settingsRepository.updateThemeFlavor(flavor)
        }
    }

    fun setOcrEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setOcrEnabled(enabled)
        }
    }

    fun setAutoCategorizeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutoCategorizeEnabled(enabled)
        }
    }

    fun setSaveCommentsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setSaveCommentsEnabled(enabled)
        }
    }

    fun setMemoryResurfacingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setMemoryResurfacingEnabled(enabled)
            memoryNotificationScheduler.apply(enabled)
        }
    }

    fun setWifiOnlyMetadata(wifiOnly: Boolean) {
        viewModelScope.launch {
            settingsRepository.setWifiOnlyMetadata(wifiOnly)
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            settingsRepository.clearCache()
            refreshStorageUsage()
        }
    }

    fun exportVault(onExported: (String) -> Unit) {
        viewModelScope.launch {
            val path = vaultBackupService.exportVaultJson()
            onExported(path)
        }
    }

    fun exportFullArchive(destFile: java.io.File? = null, onExported: (String) -> Unit) {
        viewModelScope.launch {
            val file = vaultBackupService.exportFullVaultZip(destFile)
            onExported(file.absolutePath)
        }
    }

    fun restoreFullArchive(zipFile: java.io.File, onRestored: (Int) -> Unit) {
        viewModelScope.launch {
            val count = vaultBackupService.restoreFullVaultZip(zipFile)
            refreshStorageUsage()
            onRestored(count)
        }
    }

    fun restoreVault(json: String, onRestored: (Int) -> Unit) {
        viewModelScope.launch {
            val count = vaultBackupService.restoreVaultJson(json)
            refreshStorageUsage()
            onRestored(count)
        }
    }

    private fun refreshStorageUsage() {
        viewModelScope.launch {
            _storageUsage.value = settingsRepository.getStorageUsage()
        }
    }
}
