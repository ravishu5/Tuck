package com.tuck.app.ui.travel

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Merge
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tuck.app.R
import com.tuck.app.domain.model.DetectedTrip
import com.tuck.app.domain.model.PhotoScanState
import com.tuck.app.domain.model.ScanFailure
import com.tuck.app.ui.theme.TuckTheme
import com.tuck.app.ui.theme.pressScale

/**
 * What the scan believes it found, before any of it is kept.
 *
 * Every candidate is shown with the evidence behind it — the place, the dates and how many
 * photos backed the guess — and nothing is written until the traveller presses the button. A
 * history rebuilt by inference is only trustworthy if the inferences were reviewable, so this
 * screen is part of the feature rather than a confirmation step bolted onto it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripImportReviewSheet(
    review: ImportReviewState,
    onToggle: (String) -> Unit,
    onSelectAll: () -> Unit,
    onClearAll: () -> Unit,
    onRename: (String, String) -> Unit,
    onMerge: () -> Unit,
    onImport: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = TuckTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var renaming by remember { mutableStateOf<DetectedTrip?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(
                text = stringResource(R.string.travel_review_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = colors.textPrimary
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.travel_review_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )

            Spacer(Modifier.height(14.dp))

            if (review.detected.isEmpty()) {
                Text(
                    text = stringResource(R.string.travel_review_empty_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(onClick = onSelectAll) {
                        Text(
                            text = stringResource(R.string.travel_review_select_all),
                            color = colors.accent
                        )
                    }
                    TextButton(onClick = onClearAll) {
                        Text(
                            text = stringResource(R.string.travel_review_clear_all),
                            color = colors.textSecondary
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    // Meaningless on fewer than two, and destructive-looking if offered anyway.
                    if (review.selectedCount >= 2) {
                        TextButton(onClick = onMerge) {
                            Icon(
                                Icons.Filled.Merge,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.travel_review_merge),
                                color = colors.accent
                            )
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.heightIn(max = 420.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(review.detected, key = { it.key }) { trip ->
                        DetectedTripRow(
                            trip = trip,
                            title = review.titleFor(trip),
                            isSelected = trip.key in review.selectedKeys,
                            onToggle = { onToggle(trip.key) },
                            onRename = { renaming = trip }
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            Button(
                onClick = onImport,
                enabled = review.selectedCount > 0,
                shape = TuckTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accent,
                    contentColor = colors.textOnAccent,
                    disabledContainerColor = colors.surfaceVariant,
                    disabledContentColor = colors.textMuted
                )
            ) {
                Text(
                    text = when (review.selectedCount) {
                        0 -> stringResource(R.string.travel_review_none_selected)
                        1 -> stringResource(R.string.travel_review_import_one)
                        else -> stringResource(R.string.travel_review_import, review.selectedCount)
                    },
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(28.dp))
        }
    }

    renaming?.let { trip ->
        RenameDialog(
            initial = review.titleFor(trip),
            onSave = { onRename(trip.key, it); renaming = null },
            onDismiss = { renaming = null }
        )
    }
}

@Composable
private fun DetectedTripRow(
    trip: DetectedTrip,
    title: String,
    isSelected: Boolean,
    onToggle: () -> Unit,
    onRename: () -> Unit
) {
    val colors = TuckTheme.colors
    val source = remember { MutableInteractionSource() }

    Surface(
        shape = TuckTheme.shapes.medium,
        color = if (isSelected) colors.accentContainer else colors.surfaceCard,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isSelected) colors.accent else colors.borderSubtle
        ),
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(source)
            .clickable(interactionSource = source, indication = null, onClick = onToggle)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSelected) colors.accent else colors.surface)
                    .border(
                        1.dp,
                        if (isSelected) colors.accent else colors.border,
                        RoundedCornerShape(6.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = colors.textOnAccent,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))
            Text(text = flagFor(trip.countryCode), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable(onClick = onRename)
                )
                Text(
                    text = dateRange(trip.startDate, trip.endDate),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    maxLines = 1
                )
                Text(
                    text = if (trip.alreadyInVault) {
                        stringResource(R.string.travel_review_already_saved)
                    } else {
                        stringResource(R.string.travel_photo_count, trip.photoCount)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (trip.alreadyInVault) colors.warning else colors.textMuted
                )
            }
        }
    }
}

@Composable
private fun RenameDialog(
    initial: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = TuckTheme.colors
    var value by remember { mutableStateOf(initial) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        titleContentColor = colors.textPrimary,
        textContentColor = colors.textSecondary,
        title = {
            Text(
                text = stringResource(R.string.travel_field_title),
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
        },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                colors = tuckFieldColors()
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(value.trim().ifBlank { initial }) }) {
                Text(stringResource(R.string.travel_save), color = colors.accent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.travel_cancel), color = colors.textSecondary)
            }
        }
    )
}

/** Progress while the library is being read, with a way out of it. */
@Composable
fun ScanProgressDialog(state: PhotoScanState, onCancel: () -> Unit) {
    val colors = TuckTheme.colors

    val (heading, detail, fraction) = when (state) {
        is PhotoScanState.Scanning -> Triple(
            stringResource(R.string.travel_scanning),
            if (state.totalPhotos > 0) {
                stringResource(
                    R.string.travel_scanning_progress,
                    state.photosRead,
                    state.totalPhotos
                )
            } else {
                null
            },
            if (state.totalPhotos > 0) {
                state.photosRead.toFloat() / state.totalPhotos
            } else {
                null
            }
        )

        is PhotoScanState.Naming -> Triple(
            stringResource(R.string.travel_naming),
            stringResource(
                R.string.travel_naming_progress,
                state.placesResolved,
                state.totalPlaces
            ),
            if (state.totalPlaces > 0) {
                state.placesResolved.toFloat() / state.totalPlaces
            } else {
                null
            }
        )

        else -> Triple(stringResource(R.string.travel_scanning), null, null)
    }

    AlertDialog(
        onDismissRequest = onCancel,
        containerColor = colors.surface,
        title = {
            Text(text = heading, fontWeight = FontWeight.Bold, color = colors.textPrimary)
        },
        text = {
            Column {
                if (fraction != null) {
                    LinearProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier.fillMaxWidth(),
                        color = colors.accent,
                        trackColor = colors.surfaceVariant
                    )
                } else {
                    CircularProgressIndicator(color = colors.accent)
                }
                detail?.let {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.travel_scan_cancel), color = colors.accent)
            }
        }
    )
}

/** Why the scan produced nothing, and what the traveller can do about it. */
@Composable
fun ScanFailedDialog(
    failure: ScanFailure,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val colors = TuckTheme.colors
    val message = when (failure) {
        ScanFailure.PERMISSION_DENIED -> stringResource(R.string.travel_error_permission)
        ScanFailure.MEDIA_LOCATION_DENIED -> stringResource(R.string.travel_error_media_location)
        ScanFailure.NO_LOCATED_PHOTOS -> stringResource(R.string.travel_error_no_located_photos)
    }
    // Only a trip to system settings fixes a denied permission; nothing fixes a library with
    // no coordinates in it, so that case is told plainly and offered no button that would lie.
    val offerSettings = failure != ScanFailure.NO_LOCATED_PHOTOS

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = {
            Text(
                text = stringResource(R.string.travel_review_empty),
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
        },
        text = {
            Text(text = message, color = colors.textSecondary)
        },
        confirmButton = {
            if (offerSettings) {
                TextButton(onClick = { onOpenSettings(); onDismiss() }) {
                    Text(stringResource(R.string.travel_grant_access), color = colors.accent)
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.travel_cancel), color = colors.accent)
                }
            }
        },
        dismissButton = if (offerSettings) {
            {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.travel_cancel), color = colors.textSecondary)
                }
            }
        } else {
            null
        }
    )
}

/** Text fields carry the theme's own colours; Material's defaults ignore them entirely. */
@Composable
internal fun tuckFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TuckTheme.colors.textPrimary,
    unfocusedTextColor = TuckTheme.colors.textPrimary,
    focusedBorderColor = TuckTheme.colors.accent,
    unfocusedBorderColor = TuckTheme.colors.border,
    focusedLabelColor = TuckTheme.colors.accent,
    unfocusedLabelColor = TuckTheme.colors.textSecondary,
    cursorColor = TuckTheme.colors.accent,
    focusedContainerColor = TuckTheme.colors.surface,
    unfocusedContainerColor = TuckTheme.colors.surface
)
