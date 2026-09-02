package com.tuck.app.ui.search

import androidx.compose.ui.res.stringResource
import com.tuck.app.R

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.AssistChip
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tuck.app.domain.model.ContentType
import com.tuck.app.domain.model.SortOrder
import com.tuck.app.ui.components.TuckCategoryChip
import com.tuck.app.ui.components.TuckContentCard
import com.tuck.app.ui.components.TuckEmptyState
import com.tuck.app.ui.components.TuckSearchBar
import com.tuck.app.ui.components.TuckSectionHeader
import com.tuck.app.ui.theme.TuckTheme

@Composable
fun SearchScreen(
    onNavigateToDetail: (Long) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val tuckColors = TuckTheme.colors

    LaunchedEffect(Unit) {
        try {
            focusRequester.requestFocus()
        } catch (e: Exception) {
            // Ignore if not ready
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(tuckColors.background)
    ) {
        // 1. Hero Search Input Bar
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            TuckSearchBar(
                query = uiState.query,
                onQueryChange = { viewModel.onQueryChange(it) },
                onSearch = { focusManager.clearFocus() },
                placeholder = stringResource(R.string.components_search_anything),
                focusRequester = focusRequester,
                onClear = { viewModel.onQueryChange("") }
            )
        }

        // 2. Filters & Sort Row
        SearchFiltersRow(
            selectedType = uiState.selectedContentType,
            isFavoriteOnly = uiState.isFavoriteOnly,
            selectedDays = uiState.selectedDateDays,
            sortOrder = uiState.sortOrder,
            onSelectType = { viewModel.selectContentType(it) },
            onToggleFavorite = { viewModel.toggleFavoriteFilter() },
            onSelectDays = { viewModel.selectDateRange(it) },
            onSelectSort = { viewModel.selectSortOrder(it) }
        )

        // 2b. Operators recognised in the query box, e.g. source:reddit or after:last-month
        if (uiState.activeTokens.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                items(uiState.activeTokens, key = { it.raw }) { token ->
                    AssistChip(
                        onClick = { viewModel.removeToken(token) },
                        label = { Text(token.label) },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.remove_filter, token.label),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 3. Main Content: Empty State / Natural Query Suggestions OR Results
        if (uiState.query.isBlank()) {
            SearchSuggestionsContent(
                recentQueries = uiState.recentQueries,
                onQuerySelected = { viewModel.onQueryChange(it) },
                onClearHistory = { viewModel.clearSearchHistory() }
            )
        } else {
            if (uiState.results.isEmpty()) {
                TuckEmptyState(
                    title = stringResource(R.string.empty_no_search_match),
                    description = "Try searching:\n\"React\"\n\"Travel\"\n\"things I saved last week\"",
                    icon = Icons.Filled.Search,
                    modifier = Modifier.padding(top = 40.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.results, key = { it.item.id }) { result ->
                        TuckContentCard(
                            item = result.item,
                            highlightSnippet = result.matchSnippet,
                            onClick = { onNavigateToDetail(result.item.id) },
                            onToggleFavorite = {
                                viewModel.toggleFavorite(result.item.id, result.item.isFavorite)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchFiltersRow(
    selectedType: ContentType?,
    isFavoriteOnly: Boolean,
    selectedDays: Int?,
    sortOrder: SortOrder,
    onSelectType: (ContentType?) -> Unit,
    onToggleFavorite: () -> Unit,
    onSelectDays: (Int?) -> Unit,
    onSelectSort: (SortOrder) -> Unit
) {
    var sortMenuExpanded by remember { mutableStateOf(false) }
    val tuckColors = TuckTheme.colors

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Favorites Filter
        item {
            TuckCategoryChip(
                label = stringResource(R.string.favorites_favorites),
                isSelected = isFavoriteOnly,
                onClick = onToggleFavorite,
                icon = if (isFavoriteOnly) "⭐" else null
            )
        }

        // Date Range: 7 Days
        item {
            TuckCategoryChip(
                label = stringResource(R.string.search_past_7_days),
                isSelected = selectedDays == 7,
                onClick = { onSelectDays(if (selectedDays == 7) null else 7) }
            )
        }

        // Date Range: 30 Days
        item {
            TuckCategoryChip(
                label = stringResource(R.string.search_past_30_days),
                isSelected = selectedDays == 30,
                onClick = { onSelectDays(if (selectedDays == 30) null else 30) }
            )
        }

        // Content Types
        item {
            TuckCategoryChip(
                label = stringResource(R.string.search_images_ocr),
                isSelected = selectedType == ContentType.IMAGE,
                onClick = { onSelectType(if (selectedType == ContentType.IMAGE) null else ContentType.IMAGE) }
            )
        }

        item {
            TuckCategoryChip(
                label = stringResource(R.string.search_videos_reels),
                isSelected = selectedType == ContentType.VIDEO,
                onClick = { onSelectType(if (selectedType == ContentType.VIDEO) null else ContentType.VIDEO) }
            )
        }

        item {
            TuckCategoryChip(
                label = stringResource(R.string.search_notes),
                isSelected = selectedType == ContentType.TEXT,
                onClick = { onSelectType(if (selectedType == ContentType.TEXT) null else ContentType.TEXT) }
            )
        }

        // Sort Order Dropdown
        item {
            Box {
                TuckCategoryChip(
                    label = when (sortOrder) {
                        SortOrder.RELEVANCE -> stringResource(R.string.sort_prefix, stringResource(R.string.search_relevance))
                        SortOrder.NEWEST -> stringResource(R.string.sort_prefix, stringResource(R.string.sort_newest))
                        SortOrder.OLDEST -> stringResource(R.string.sort_prefix, stringResource(R.string.sort_oldest))
                        SortOrder.RECENTLY_OPENED -> stringResource(R.string.sort_prefix, stringResource(R.string.sort_opened))
                    },
                    isSelected = false,
                    onClick = { sortMenuExpanded = true }
                )

                DropdownMenu(
                    expanded = sortMenuExpanded,
                    onDismissRequest = { sortMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.search_relevance)) },
                        onClick = {
                            onSelectSort(SortOrder.RELEVANCE)
                            sortMenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.search_newest_first)) },
                        onClick = {
                            onSelectSort(SortOrder.NEWEST)
                            sortMenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.search_oldest_first)) },
                        onClick = {
                            onSelectSort(SortOrder.OLDEST)
                            sortMenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.search_recently_opened)) },
                        onClick = {
                            onSelectSort(SortOrder.RECENTLY_OPENED)
                            sortMenuExpanded = false
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchSuggestionsContent(
    recentQueries: List<String>,
    onQuerySelected: (String) -> Unit,
    onClearHistory: () -> Unit
) {
    val tuckColors = TuckTheme.colors
    val tuckShapes = TuckTheme.shapes

    val naturalSuggestions = listOf(
        "that React post",
        "restaurants in Kolkata",
        "AI papers I saved last month",
        "headphones around ₹3000",
        "travel places in Meghalaya"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Recent Searches
        if (recentQueries.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.History,
                            contentDescription = null,
                            tint = tuckColors.accent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.search_recent_searches),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = tuckColors.textPrimary
                        )
                    }

                    TextButton(onClick = onClearHistory) {
                        Text(
                            text = stringResource(R.string.components_clear),
                            style = MaterialTheme.typography.labelSmall,
                            color = tuckColors.textMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    recentQueries.forEach { query ->
                        Surface(
                            shape = tuckShapes.pill,
                            color = tuckColors.surfaceCard,
                            border = androidx.compose.foundation.BorderStroke(1.dp, tuckColors.border),
                            modifier = Modifier
                                .clickable { onQuerySelected(query) }
                        ) {
                            Text(
                                text = query,
                                style = MaterialTheme.typography.bodySmall,
                                color = tuckColors.textPrimary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Natural Query Examples
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Lightbulb,
                    contentDescription = null,
                    tint = tuckColors.accent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.search_try_searching_naturally),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = tuckColors.textPrimary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                naturalSuggestions.forEach { suggestion ->
                    Surface(
                        shape = tuckShapes.medium,
                        color = tuckColors.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, tuckColors.borderSubtle),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onQuerySelected(suggestion) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = null,
                                tint = tuckColors.textMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "\"$suggestion\"",
                                style = MaterialTheme.typography.bodyMedium,
                                color = tuckColors.textSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}
