package com.tuck.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuck.app.domain.model.ContentType
import com.tuck.app.domain.model.QueryToken
import com.tuck.app.domain.model.SearchFilter
import com.tuck.app.domain.model.SearchQueryParser
import com.tuck.app.domain.model.SearchResult
import com.tuck.app.domain.model.SortOrder
import com.tuck.app.domain.repository.SavedItemRepository
import com.tuck.app.domain.repository.SearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val results: List<SearchResult> = emptyList(),
    val recentQueries: List<String> = emptyList(),
    val selectedContentType: ContentType? = null,
    val isFavoriteOnly: Boolean = false,
    val selectedDateDays: Int? = null, // null, 7, 30
    val sortOrder: SortOrder = SortOrder.RELEVANCE,
    val isSearching: Boolean = false,
    /** Operators recognised in the query box, shown as removable chips. */
    val activeTokens: List<QueryToken> = emptyList()
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository,
    private val savedItemRepository: SavedItemRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _contentType = MutableStateFlow<ContentType?>(null)
    private val _isFavoriteOnly = MutableStateFlow(false)
    private val _dateDays = MutableStateFlow<Int?>(null)
    private val _sortOrder = MutableStateFlow(SortOrder.RELEVANCE)

    val recentQueries: StateFlow<List<String>> = searchRepository.getRecentSearchQueries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val filterFlow = combine(
        _query.debounce(150).distinctUntilChanged(),
        _contentType,
        _isFavoriteOnly,
        _dateDays,
        _sortOrder
    ) { query, type, fav, days, sort ->
        // Operators typed into the box take precedence over the chip selections.
        val parsed = SearchQueryParser.parse(query)
        SearchFilter(
            query = parsed.freeText,
            contentType = parsed.contentType ?: type,
            sourceDomain = parsed.sourceDomain,
            collectionName = parsed.collectionName,
            tag = parsed.tag,
            isFavoriteOnly = parsed.isFavoriteOnly || fav,
            isArchivedOnly = parsed.isArchivedOnly,
            dateRangeDays = days,
            createdAfter = parsed.createdAfter,
            createdBefore = parsed.createdBefore,
            sortOrder = sort
        )
    }

    val searchResults: StateFlow<List<SearchResult>> = filterFlow.flatMapLatest { filter ->
        val hasOnlyOperators = filter.query.isBlank() && (
            filter.contentType != null || filter.sourceDomain != null ||
                filter.collectionName != null || filter.tag != null ||
                filter.isFavoriteOnly || filter.isArchivedOnly ||
                filter.createdAfter != null || filter.createdBefore != null
            )
        if (filter.query.isBlank() && !hasOnlyOperators) {
            flowOf(emptyList())
        } else {
            searchRepository.search(filter)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private data class FilterState(
        val query: String,
        val type: ContentType?,
        val fav: Boolean,
        val days: Int?,
        val sort: SortOrder
    )

    private val currentFilterState = combine(
        _query,
        _contentType,
        _isFavoriteOnly,
        _dateDays,
        _sortOrder
    ) { query, type, fav, days, sort ->
        FilterState(query, type, fav, days, sort)
    }

    val uiState: StateFlow<SearchUiState> = combine(
        currentFilterState,
        searchResults,
        recentQueries
    ) { state, results, recents ->
        SearchUiState(
            query = state.query,
            results = results,
            activeTokens = SearchQueryParser.parse(state.query).tokens,
            recentQueries = recents,
            selectedContentType = state.type,
            isFavoriteOnly = state.fav,
            selectedDateDays = state.days,
            sortOrder = state.sort,
            isSearching = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchUiState())

    /** Removes one operator chip by deleting its text from the query. */
    fun removeToken(token: QueryToken) {
        _query.value = _query.value.replace(token.raw, "").replace(Regex("\\s+"), " ").trim()
    }

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
    }

    fun selectContentType(type: ContentType?) {
        _contentType.value = if (_contentType.value == type) null else type
    }

    fun toggleFavoriteFilter() {
        _isFavoriteOnly.value = !_isFavoriteOnly.value
    }

    fun selectDateRange(days: Int?) {
        _dateDays.value = if (_dateDays.value == days) null else days
    }

    fun selectSortOrder(sort: SortOrder) {
        _sortOrder.value = sort
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            searchRepository.clearSearchHistory()
        }
    }

    fun toggleFavorite(itemId: Long, current: Boolean) {
        viewModelScope.launch {
            savedItemRepository.setFavorite(itemId, !current)
        }
    }
}
