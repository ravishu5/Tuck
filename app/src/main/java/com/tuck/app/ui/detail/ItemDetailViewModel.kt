package com.tuck.app.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuck.app.domain.memory.RelatedItemsEngine
import com.tuck.app.domain.model.Collection
import com.tuck.app.domain.model.SavedItem
import com.tuck.app.domain.repository.CollectionRepository
import com.tuck.app.domain.repository.SavedItemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailUiState(
    val item: SavedItem? = null,
    val allCollections: List<Collection> = emptyList(),
    val relatedItems: List<SavedItem> = emptyList(),
    val isEditingTitle: Boolean = false,
    val editedTitle: String = "",
    val isLoading: Boolean = true,
    val isDeleted: Boolean = false
)

private data class TitleEditState(
    val isEditing: Boolean = false,
    val text: String = ""
)

@HiltViewModel
class ItemDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val savedItemRepository: SavedItemRepository,
    private val collectionRepository: CollectionRepository,
    private val relatedItemsEngine: RelatedItemsEngine
) : ViewModel() {

    private val itemId: Long = checkNotNull(savedStateHandle["itemId"])

    private val _isEditingTitle = MutableStateFlow(false)
    private val _editedTitle = MutableStateFlow("")
    private val _isDeleted = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            savedItemRepository.markOpened(itemId)
        }
    }

    private val titleEditFlow = combine(_isEditingTitle, _editedTitle) { isEditing, text ->
        TitleEditState(isEditing = isEditing, text = text)
    }

    val uiState: StateFlow<DetailUiState> = combine(
        savedItemRepository.getItemByIdFlow(itemId),
        collectionRepository.getAllCollections(),
        relatedItemsEngine.findRelatedItems(itemId, 5),
        titleEditFlow,
        _isDeleted
    ) { item, allCollections, relatedItems, titleEdit, isDeleted ->
        DetailUiState(
            item = item,
            allCollections = allCollections,
            relatedItems = relatedItems,
            isEditingTitle = titleEdit.isEditing,
            editedTitle = if (titleEdit.isEditing) titleEdit.text else item?.title.orEmpty(),
            isLoading = false,
            isDeleted = isDeleted
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DetailUiState(isLoading = true)
    )

    fun startEditTitle(currentTitle: String) {
        _editedTitle.value = currentTitle
        _isEditingTitle.value = true
    }

    fun onTitleChange(newTitle: String) {
        _editedTitle.value = newTitle
    }

    fun saveTitle() {
        val currentItem = uiState.value.item ?: return
        val newTitle = _editedTitle.value.trim()
        if (newTitle.isNotBlank()) {
            viewModelScope.launch {
                savedItemRepository.updateItem(currentItem.copy(title = newTitle))
                _isEditingTitle.value = false
            }
        } else {
            _isEditingTitle.value = false
        }
    }

    fun cancelEditTitle() {
        _isEditingTitle.value = false
    }

    fun saveUserNote(note: String) {
        val currentItem = uiState.value.item ?: return
        viewModelScope.launch {
            savedItemRepository.updateItem(currentItem.copy(userNote = note.trim().ifBlank { null }))
        }
    }

    fun toggleFavorite() {
        val currentItem = uiState.value.item ?: return
        viewModelScope.launch {
            savedItemRepository.setFavorite(currentItem.id, !currentItem.isFavorite)
        }
    }

    fun toggleArchived() {
        val currentItem = uiState.value.item ?: return
        viewModelScope.launch {
            savedItemRepository.setArchived(currentItem.id, !currentItem.isArchived)
        }
    }

    fun moveToTrash() {
        val currentItem = uiState.value.item ?: return
        viewModelScope.launch {
            savedItemRepository.moveToTrash(currentItem.id)
            _isDeleted.value = true
        }
    }

    fun toggleCollectionMembership(collectionId: Long, isCurrentlyMember: Boolean) {
        val currentItem = uiState.value.item ?: return
        viewModelScope.launch {
            if (isCurrentlyMember) {
                savedItemRepository.removeItemFromCollection(currentItem.id, collectionId)
            } else {
                savedItemRepository.addItemToCollection(currentItem.id, collectionId)
            }
        }
    }
}
