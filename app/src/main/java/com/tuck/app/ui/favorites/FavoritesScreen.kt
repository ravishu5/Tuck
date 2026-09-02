package com.tuck.app.ui.favorites

import androidx.compose.ui.res.stringResource
import com.tuck.app.R

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tuck.app.ui.components.TuckContentCard
import com.tuck.app.ui.components.TuckEmptyState
import com.tuck.app.ui.theme.TuckTheme

@Composable
fun FavoritesScreen(
    onNavigateToDetail: (Long) -> Unit,
    viewModel: FavoritesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val tuckColors = TuckTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(tuckColors.background)
            .statusBarsPadding()
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Text(
                text = stringResource(R.string.favorites_favorites),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = tuckColors.textPrimary
            )
            Text(
                text = stringResource(R.string.favorites_things_you_definitely_want_to_keep),
                style = MaterialTheme.typography.bodyMedium,
                color = tuckColors.textSecondary
            )
        }

        if (uiState.favoriteItems.isEmpty()) {
            TuckEmptyState(
                title = stringResource(R.string.empty_no_favorites),
                description = stringResource(R.string.empty_star_any_item),
                icon = Icons.Filled.Star,
                modifier = Modifier.padding(top = 40.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(uiState.favoriteItems, key = { it.id }) { item ->
                    TuckContentCard(
                        item = item,
                        onClick = { onNavigateToDetail(item.id) },
                        onToggleFavorite = {
                            viewModel.toggleFavorite(item.id, item.isFavorite)
                        }
                    )
                }
            }
        }
    }
}
