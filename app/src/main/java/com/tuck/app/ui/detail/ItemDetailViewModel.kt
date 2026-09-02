package com.tuck.app.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuck.app.data.local.db.dao.SourceContentDao
import com.tuck.app.data.local.db.entity.SourceCommentEntity
import com.tuck.app.data.local.db.entity.SourcePostEntity
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
import com.tuck.app.processing.ReminderPreset
import com.tuck.app.processing.ReminderScheduler
import javax.inject.Inject
import com.tuck.app.data.local.db.dao.ChecklistDao
import com.tuck.app.data.local.db.entity.ChecklistItemEntity

data class DetailUiState(
    val item: SavedItem? = null,
    val sourcePost: SourcePostEntity? = null,
    val commentsTree: List<SourceCommentEntity> = emptyList(),
    val allCollections: List<Collection> = emptyList(),
    val relatedItems: List<SavedItem> = emptyList(),
    val isEditingTitle: Boolean = false,
    val editedTitle: String = "",
    val isLoading: Boolean = true,
    val isDeleted: Boolean = false,
    val checklist: List<ChecklistItemEntity> = emptyList()
)

private data class TitleEditState(
    val isEditing: Boolean = false,
    val text: String = ""
)

private data class SourceContentState(
    val post: SourcePostEntity? = null,
    val comments: List<SourceCommentEntity> = emptyList()
)

@HiltViewModel
class ItemDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val savedItemRepository: SavedItemRepository,
    private val checklistDao: ChecklistDao,
    private val collectionRepository: CollectionRepository,
    private val sourceContentDao: SourceContentDao,
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

    private val sourceContentFlow = combine(
        sourceContentDao.getPostFlow(itemId),
        sourceContentDao.getCommentsTree(itemId)
    ) { post, comments ->
        SourceContentState(post = post, comments = comments)
    }

    // combine() is typed only up to five flows, so the checklist is layered on top
    // rather than pushed into the vararg overload, which would lose the types.
    private val baseState = combine(
        savedItemRepository.getItemByIdFlow(itemId),
        sourceContentFlow,
        collectionRepository.getAllCollections(),
        relatedItemsEngine.findRelatedItems(itemId, 5),
        titleEditFlow
    ) { item, sourceContent, allCollections, relatedItems, titleEdit ->
        DetailUiState(
            item = item,
            sourcePost = sourceContent.post,
            commentsTree = sourceContent.comments,
            allCollections = allCollections,
            relatedItems = relatedItems,
            isEditingTitle = titleEdit.isEditing,
            editedTitle = if (titleEdit.isEditing) titleEdit.text else item?.title.orEmpty(),
            isLoading = false,
            isDeleted = _isDeleted.value
        )
    }

    val uiState: StateFlow<DetailUiState> = combine(
        baseState,
        checklistDao.getForItem(itemId)
    ) { base, checklist ->
        base.copy(checklist = checklist)
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

    fun addChecklistItem(text: String) {
        val currentItem = uiState.value.item ?: return
        if (text.isBlank()) return
        viewModelScope.launch {
            checklistDao.insert(
                ChecklistItemEntity(
                    itemId = currentItem.id,
                    text = text.trim(),
                    ordinal = uiState.value.checklist.size
                )
            )
        }
    }

    fun setChecklistItemDone(id: Long, isDone: Boolean) {
        viewModelScope.launch { checklistDao.setDone(id, isDone) }
    }

    fun deleteChecklistItem(entry: ChecklistItemEntity) {
        viewModelScope.launch { checklistDao.delete(entry) }
    }

    fun setReminder(preset: ReminderPreset) {
        val currentItem = uiState.value.item ?: return
        viewModelScope.launch {
            savedItemRepository.setReminder(currentItem.id, ReminderScheduler.resolve(preset))
        }
    }

    fun clearReminder() {
        val currentItem = uiState.value.item ?: return
        viewModelScope.launch {
            savedItemRepository.setReminder(currentItem.id, null)
        }
    }

    fun toggleCompleted() {
        val currentItem = uiState.value.item ?: return
        viewModelScope.launch {
            savedItemRepository.setCompleted(currentItem.id, currentItem.completedAt == null)
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
