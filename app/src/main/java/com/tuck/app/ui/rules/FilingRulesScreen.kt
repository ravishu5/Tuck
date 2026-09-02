package com.tuck.app.ui.rules

import androidx.compose.ui.res.stringResource
import com.tuck.app.R

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tuck.app.ui.theme.TuckTheme

/**
 * Auto-filing rules. Seven users in the research asked for automatic categorisation and
 * the incumbent refuses on the grounds that guessing misfiles things - which is true.
 * Rules sidestep that: the user states the intent, so nothing is guessed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilingRulesScreen(
    onNavigateBack: () -> Unit,
    viewModel: FilingRulesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val tuckColors = TuckTheme.colors

    var draftQuery by remember { mutableStateOf("") }
    var draftCollectionId by remember { mutableLongStateOf(0L) }

    val isValid = viewModel.isValid(draftQuery)

    Scaffold(
        containerColor = tuckColors.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.rules_auto_filing_rules)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.collections_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = tuckColors.background)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.rules_anything_you_save_that_matches_a) +
                        "Rules use the same syntax as search, for example  source:reddit  or  type:pdf tag:work",
                    style = MaterialTheme.typography.bodyMedium,
                    color = tuckColors.textSecondary
                )
            }

            item {
                Card(colors = CardDefaults.cardColors(containerColor = tuckColors.surfaceCard)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.rules_new_rule), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, color = tuckColors.textMuted)
                        Spacer(Modifier.height(10.dp))

                        OutlinedTextField(
                            value = draftQuery,
                            onValueChange = { draftQuery = it },
                            label = { Text(stringResource(R.string.rules_when_a_save_matches)) },
                            placeholder = { Text(stringResource(R.string.rules_source_reddit)) },
                            singleLine = true,
                            isError = draftQuery.isNotBlank() && !isValid,
                            supportingText = {
                                if (draftQuery.isNotBlank() && !isValid) {
                                    Text(stringResource(R.string.rules_this_would_match_everything_add_a))
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(12.dp))
                        Text(stringResource(R.string.rules_file_it_into), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, color = tuckColors.textMuted)
                        Spacer(Modifier.height(8.dp))

                        androidx.compose.foundation.lazy.LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(uiState.collections, key = { it.id }) { collection ->
                                FilterChip(
                                    selected = draftCollectionId == collection.id,
                                    onClick = { draftCollectionId = collection.id },
                                    label = { Text(collection.name) }
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        TextButton(
                            enabled = isValid && draftCollectionId > 0L,
                            onClick = {
                                viewModel.addRule(draftQuery, draftCollectionId)
                                draftQuery = ""
                                draftCollectionId = 0L
                            }
                        ) { Text(stringResource(R.string.rules_add_rule)) }
                    }
                }
            }

            if (uiState.rules.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.rules_no_rules_yet_everything_you_save),
                        style = MaterialTheme.typography.bodyMedium,
                        color = tuckColors.textMuted
                    )
                }
            }

            items(uiState.rules, key = { it.id }) { rule ->
                Card(colors = CardDefaults.cardColors(containerColor = tuckColors.surfaceCard)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(rule.query, fontWeight = FontWeight.Bold, color = tuckColors.textPrimary)
                            Text(
                                "→ ${uiState.collectionNameFor(rule.collectionId)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = tuckColors.textSecondary
                            )
                            Text(
                                if (rule.matchCount == 0) stringResource(R.string.rules_never_matched)
                                else "Filed ${rule.matchCount} ${if (rule.matchCount == 1) "item" else "items"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = tuckColors.textMuted
                            )
                        }
                        Switch(
                            checked = rule.isEnabled,
                            onCheckedChange = { viewModel.setEnabled(rule, it) }
                        )
                        Spacer(Modifier.width(4.dp))
                        IconButton(onClick = { viewModel.delete(rule) }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.rules_delete_rule))
                        }
                    }
                }
            }
        }
    }
}
