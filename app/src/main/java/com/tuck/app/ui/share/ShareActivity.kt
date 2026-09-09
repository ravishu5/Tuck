package com.tuck.app.ui.share

import androidx.compose.ui.res.stringResource
import com.tuck.app.R

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tuck.app.ui.MainActivity
import com.tuck.app.ui.theme.TuckTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ShareActivity : ComponentActivity() {

    private val viewModel: ShareViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val caller = callingPackage ?: intent.getStringExtra("android.intent.extra.REFERRER_NAME")
        viewModel.handleIncomingIntent(intent, caller)

        setContent {
            TuckTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                ShareDialogOverlay(
                    uiState = uiState,
                    onToggleCollection = { colId -> viewModel.toggleCollection(colId) },
                    onSetReminder = { at, note -> viewModel.setReminder(at, note) },
                    onCreateCollection = { name -> viewModel.createAndAddCollection(name) },
                    onOpenCustomCategoryDialog = { open -> viewModel.setCustomCategoryDialogOpen(open) },
                    onOpenItem = { itemId ->
                        val mainIntent = Intent(this, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            putExtra("open_item_id", itemId)
                        }
                        startActivity(mainIntent)
                        finish()
                    },
                    onDismiss = { finish() }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val caller = callingPackage ?: intent.getStringExtra("android.intent.extra.REFERRER_NAME")
        viewModel.handleIncomingIntent(intent, caller)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShareDialogOverlay(
    uiState: ShareUiState,
    onToggleCollection: (Long) -> Unit,
    onSetReminder: (Long?, String?) -> Unit,
    onCreateCollection: (String) -> Unit,
    onOpenCustomCategoryDialog: (Boolean) -> Unit,
    onOpenItem: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val tuckColors = TuckTheme.colors
    val tuckShapes = TuckTheme.shapes

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(tuckColors.scrim.copy(alpha = 0.55f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = uiState !is ShareUiState.Idle,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Card(
                // A share sheet belongs to the bottom edge: no side gutters, and corners only
                // where it meets the screen it is covering.
                shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
                colors = CardDefaults.cardColors(containerColor = tuckColors.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // Consume click
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 14.dp, bottom = 20.dp)
                ) {
                    when (uiState) {
                        is ShareUiState.Saving -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.5.dp,
                                    color = tuckColors.accent
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = stringResource(R.string.share_saving_to_tuck),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = tuckColors.textPrimary
                                )
                            }
                        }

                        is ShareUiState.Saved -> {
                            SaveToTuckSheet(
                                state = uiState,
                                onToggleCollection = onToggleCollection,
                                onCreateCollection = { onOpenCustomCategoryDialog(true) },
                                onSetReminder = onSetReminder,
                                onOpenItem = onOpenItem,
                                onDone = onDismiss
                            )
                        }

                        is ShareUiState.Error -> {
                            Text(
                                text = stringResource(R.string.share_could_not_save_item),
                                style = MaterialTheme.typography.titleMedium,
                                color = tuckColors.destructive,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = uiState.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = tuckColors.textSecondary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onDismiss,
                                shape = tuckShapes.small,
                                colors = ButtonDefaults.buttonColors(containerColor = tuckColors.accent)
                            ) {
                                Text(stringResource(R.string.components_dismiss), fontWeight = FontWeight.Bold)
                            }
                        }

                        is ShareUiState.Idle -> {}
                    }
                }
            }
        }
    }

    // New Category Dialog
    if (uiState is ShareUiState.Saved && uiState.isCustomCategoryDialogOpen) {
        var newCatName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { onOpenCustomCategoryDialog(false) },
            title = {
                Text(
                    text = stringResource(R.string.share_new_category),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = tuckColors.textPrimary
                )
            },
            text = {
                OutlinedTextField(
                    value = newCatName,
                    onValueChange = { newCatName = it },
                    placeholder = { Text(stringResource(R.string.share_category_name), color = tuckColors.textMuted) },
                    singleLine = true,
                    shape = tuckShapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = tuckColors.surfaceCard,
                        unfocusedContainerColor = tuckColors.surfaceCard,
                        focusedBorderColor = tuckColors.accent,
                        unfocusedBorderColor = tuckColors.border,
                        focusedTextColor = tuckColors.textPrimary,
                        unfocusedTextColor = tuckColors.textPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCatName.isNotBlank()) {
                            onCreateCollection(newCatName.trim())
                            onOpenCustomCategoryDialog(false)
                        }
                    },
                    shape = tuckShapes.small,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = tuckColors.accent,
                        contentColor = tuckColors.textOnAccent
                    ),
                    enabled = newCatName.isNotBlank()
                ) {
                    Text(stringResource(R.string.share_add), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { onOpenCustomCategoryDialog(false) }) {
                    Text(stringResource(R.string.collections_cancel), color = tuckColors.textMuted)
                }
            },
            containerColor = tuckColors.surface,
            shape = tuckShapes.large
        )
    }
}
