package com.tuck.app.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuck.app.domain.model.SavedItem
import com.tuck.app.domain.repository.SavedItemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FavoritesUiState(
    val favoriteItems: List<SavedItem> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val savedItemRepository: SavedItemRepository
) : ViewModel() {

    val uiState: StateFlow<FavoritesUiState> = savedItemRepository.getFavoriteItems()
        .map { items ->
            FavoritesUiState(
                favoriteItems = items,
                isLoading = false
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = FavoritesUiState(isLoading = true)
        )

    fun toggleFavorite(itemId: Long, currentFavorite: Boolean) {
        viewModelScope.launch {
            savedItemRepository.setFavorite(itemId, !currentFavorite)
        }
    }
}
