package com.tuck.app.ui.rules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuck.app.data.local.db.dao.FilingRuleDao
import com.tuck.app.data.local.db.entity.FilingRuleEntity
import com.tuck.app.domain.model.Collection
import com.tuck.app.domain.model.RuleMatcher
import com.tuck.app.domain.model.SearchQueryParser
import com.tuck.app.domain.repository.CollectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FilingRulesUiState(
    val rules: List<FilingRuleEntity> = emptyList(),
    val collections: List<Collection> = emptyList()
) {
    fun collectionNameFor(id: Long): String =
        collections.firstOrNull { it.id == id }?.name ?: "Unknown collection"
}

@HiltViewModel
class FilingRulesViewModel @Inject constructor(
    private val filingRuleDao: FilingRuleDao,
    collectionRepository: CollectionRepository
) : ViewModel() {

    val uiState: StateFlow<FilingRulesUiState> = combine(
        filingRuleDao.getAllRules(),
        collectionRepository.getAllCollections()
    ) { rules, collections ->
        FilingRulesUiState(rules = rules, collections = collections)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FilingRulesUiState())

    /**
     * A rule stating no condition would file everything, so it is rejected before it can
     * be saved rather than quietly doing damage.
     */
    fun isValid(query: String): Boolean =
        query.isNotBlank() && !RuleMatcher.isEmpty(SearchQueryParser.parse(query))

    fun addRule(query: String, collectionId: Long) {
        if (!isValid(query) || collectionId <= 0L) return
        viewModelScope.launch {
            filingRuleDao.insert(
                FilingRuleEntity(
                    query = query.trim(),
                    collectionId = collectionId,
                    sortOrdinal = uiState.value.rules.size
                )
            )
        }
    }

    fun setEnabled(rule: FilingRuleEntity, enabled: Boolean) {
        viewModelScope.launch { filingRuleDao.setEnabled(rule.id, enabled) }
    }

    fun delete(rule: FilingRuleEntity) {
        viewModelScope.launch { filingRuleDao.delete(rule) }
    }
}
