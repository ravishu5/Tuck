package com.tuck.app.ui.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuck.app.domain.model.SavedItem
import com.tuck.app.domain.repository.SavedItemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrashViewModel @Inject constructor(
    private val savedItemRepository: SavedItemRepository
) : ViewModel() {

    val trashedItems: StateFlow<List<SavedItem>> = savedItemRepository.getTrashedItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun restoreItem(itemId: Long) {
        viewModelScope.launch {
            savedItemRepository.restoreFromTrash(itemId)
        }
    }

    fun permanentlyDelete(itemId: Long) {
        viewModelScope.launch {
            savedItemRepository.permanentlyDelete(itemId)
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            savedItemRepository.emptyTrash()
        }
    }
}
