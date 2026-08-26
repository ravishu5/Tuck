package com.tuck.app.ui.share

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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tuck.app.ui.MainActivity
import com.tuck.app.ui.components.TuckCategoryChip
import com.tuck.app.ui.theme.TuckTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay

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
            .background(Color.Black.copy(alpha = 0.55f))
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
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            Card(
                shape = tuckShapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = tuckColors.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, tuckColors.border),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // Consume click
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
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
                                    text = "Saving to Tuck…",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = tuckColors.textPrimary
                                )
                            }
                        }

                        is ShareUiState.Saved -> {
                            // Header: Lightweight Confirmation: "✓ Tucked"
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF10B981).copy(alpha = 0.15f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = "Saved",
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "✓ Tucked into Vault",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = tuckColors.textPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = uiState.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = tuckColors.textPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (uiState.subtitle.isNotBlank()) {
                                        Text(
                                            text = uiState.subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = tuckColors.textSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Category Selector Section
                            Text(
                                text = "Add to collection:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = tuckColors.textSecondary
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // "+ New Category" Chip
                                Surface(
                                    shape = tuckShapes.pill,
                                    color = tuckColors.surfaceCard,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, tuckColors.accent.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .clip(tuckShapes.pill)
                                        .clickable { onOpenCustomCategoryDialog(true) }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Add,
                                            contentDescription = null,
                                            tint = tuckColors.accent,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "New",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = tuckColors.accent
                                        )
                                    }
                                }

                                uiState.collections.forEach { col ->
                                    val isSelected = uiState.selectedCollectionIds.contains(col.id)
                                    TuckCategoryChip(
                                        label = col.name,
                                        isSelected = isSelected,
                                        onClick = { onToggleCollection(col.id) },
                                        icon = if (isSelected) "✓" else null
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Action Buttons: Done / Open in Tuck
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = onDismiss,
                                    shape = tuckShapes.medium,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = tuckColors.accent,
                                        contentColor = tuckColors.textOnAccent
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(46.dp)
                                ) {
                                    Text("Done", fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { onOpenItem(uiState.savedItemId) },
                                    shape = tuckShapes.medium,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = tuckColors.surfaceCard,
                                        contentColor = tuckColors.textPrimary
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, tuckColors.border),
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .height(46.dp)
                                ) {
                                    Text("Open in Tuck", fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        tint = tuckColors.textSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        is ShareUiState.Error -> {
                            Text(
                                text = "Could not save item",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFFEF4444),
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
                                Text("Dismiss", fontWeight = FontWeight.Bold)
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
                    text = "New Category",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = tuckColors.textPrimary
                )
            },
            text = {
                OutlinedTextField(
                    value = newCatName,
                    onValueChange = { newCatName = it },
                    placeholder = { Text("Category name…", color = tuckColors.textMuted) },
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
                    Text("Add", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { onOpenCustomCategoryDialog(false) }) {
                    Text("Cancel", color = tuckColors.textMuted)
                }
            },
            containerColor = tuckColors.surface,
            shape = tuckShapes.large
        )
    }
}
