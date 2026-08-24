package com.tuck.app.ui.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuck.app.domain.model.Collection
import com.tuck.app.domain.model.SavedItem
import com.tuck.app.domain.repository.CollectionRepository
import com.tuck.app.domain.repository.SavedItemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InboxUiState(
    val inboxItems: List<SavedItem> = emptyList(),
    val allCollections: List<Collection> = emptyList(),
    val isLoading: Boolean = false,
    val selectedItemForCategorize: SavedItem? = null
)

@HiltViewModel
class InboxViewModel @Inject constructor(
    private val savedItemRepository: SavedItemRepository,
    private val collectionRepository: CollectionRepository
) : ViewModel() {

    private val _selectedItemForCategorize = MutableStateFlow<SavedItem?>(null)

    val uiState: StateFlow<InboxUiState> = combine(
        savedItemRepository.getAllActiveItems(),
        collectionRepository.getAllCollections(),
        _selectedItemForCategorize
    ) { items, collections, selectedItem ->
        // Inbox shows unpinned & non-archived items that need triage
        val inbox = items.filter { !it.isPinned && !it.isArchived }
        InboxUiState(
            inboxItems = inbox,
            allCollections = collections,
            isLoading = false,
            selectedItemForCategorize = selectedItem
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = InboxUiState(isLoading = true)
    )

    fun keepItem(item: SavedItem) {
        viewModelScope.launch {
            savedItemRepository.updateItem(item.copy(isPinned = true))
        }
    }

    fun archiveItem(item: SavedItem) {
        viewModelScope.launch {
            savedItemRepository.setArchived(item.id, true)
        }
    }

    fun deleteItem(item: SavedItem) {
        viewModelScope.launch {
            savedItemRepository.moveToTrash(item.id)
        }
    }

    fun toggleFavorite(item: SavedItem) {
        viewModelScope.launch {
            savedItemRepository.setFavorite(item.id, !item.isFavorite)
        }
    }

    fun openCategorizeDialog(item: SavedItem) {
        _selectedItemForCategorize.value = item
    }

    fun closeCategorizeDialog() {
        _selectedItemForCategorize.value = null
    }

    fun addToCollection(itemId: Long, collectionId: Long) {
        viewModelScope.launch {
            savedItemRepository.addItemToCollection(itemId, collectionId)
            _selectedItemForCategorize.value = null
        }
    }
}
