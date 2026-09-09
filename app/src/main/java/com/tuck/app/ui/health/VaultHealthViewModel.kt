package com.tuck.app.ui.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuck.app.data.health.VaultHealthChecker
import com.tuck.app.domain.health.HealthReport
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VaultHealthUiState(
    val report: HealthReport? = null,
    val isChecking: Boolean = false,
    val isRepairing: Boolean = false,
    /** What the last repair actually did, shown back to the user verbatim. */
    val lastRepairSummary: String? = null
)

@HiltViewModel
class VaultHealthViewModel @Inject constructor(
    private val healthChecker: VaultHealthChecker
) : ViewModel() {

    private val _uiState = MutableStateFlow(VaultHealthUiState())
    val uiState: StateFlow<VaultHealthUiState> = _uiState.asStateFlow()

    init {
        runCheck()
    }

    fun runCheck() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isChecking = true, lastRepairSummary = null)
            val report = healthChecker.check()
            _uiState.value = _uiState.value.copy(report = report, isChecking = false)
        }
    }

    fun repair(findingId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRepairing = true)
            val result = healthChecker.repair(findingId)
            // Re-check rather than trusting the repair: the report the user sees should
            // always reflect the vault as it is now, not as we expect it to be.
            val report = healthChecker.check()
            _uiState.value = _uiState.value.copy(
                report = report,
                isRepairing = false,
                lastRepairSummary = result.summary
            )
        }
    }

    fun repairAll() {
        val current = _uiState.value.report ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRepairing = true)
            val result = healthChecker.repairAll(current)
            val report = healthChecker.check()
            _uiState.value = _uiState.value.copy(
                report = report,
                isRepairing = false,
                lastRepairSummary = result.summary
            )
        }
    }
}
