package com.tuck.app.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.tuck.app.R
import com.tuck.app.processing.ReminderPreset
import com.tuck.app.processing.ReminderScheduler
import com.tuck.app.ui.theme.TuckTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * The one place a reminder is created, wherever the reader is standing.
 *
 * Reminders used to be a strip of preset chips repeated in the share sheet and again on the
 * detail screen, with no way to pick a real time and nowhere to say why. Those are the two things
 * people actually want from a reminder — "not one of your four presets" and "remind me *what*" —
 * so both live here, and every entry point opens the same dialog rather than its own variation.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReminderDialog(
    initialRemindAt: Long?,
    initialNote: String?,
    onDismiss: () -> Unit,
    onSave: (remindAt: Long, note: String?) -> Unit,
    onClear: (() -> Unit)? = null
) {
    val colors = TuckTheme.colors
    val context = LocalContext.current

    var relative by remember { mutableStateOf(initialRemindAt == null) }
    var chosenAt by remember {
        mutableLongStateOf(initialRemindAt ?: ReminderScheduler.resolve(ReminderPreset.TOMORROW_MORNING))
    }
    var note by remember { mutableStateOf(initialNote.orEmpty()) }

    val formatter = remember { SimpleDateFormat("dd/MM/yyyy, HH:mm", Locale.getDefault()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = colors.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = colors.accentContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Alarm,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(Modifier.size(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(
                                if (initialRemindAt == null) R.string.reminder_new else R.string.reminder_edit
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Text(
                            text = stringResource(R.string.reminder_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textMuted
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.collections_cancel),
                            tint = colors.textMuted
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))

                // Two ways to answer "when": a quick one and an exact one.
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ModeTab(
                        label = stringResource(R.string.reminder_mode_relative),
                        selected = relative,
                        modifier = Modifier.weight(1f)
                    ) { relative = true }
                    ModeTab(
                        label = stringResource(R.string.reminder_mode_exact),
                        selected = !relative,
                        modifier = Modifier.weight(1f)
                    ) { relative = false }
                }

                Spacer(Modifier.height(14.dp))

                if (relative) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            ReminderPreset.LATER_TODAY to R.string.reminder_later_today,
                            ReminderPreset.TOMORROW_MORNING to R.string.reminder_tomorrow,
                            ReminderPreset.THIS_WEEKEND to R.string.reminder_this_weekend,
                            ReminderPreset.NEXT_WEEK to R.string.reminder_next_week
                        ).forEach { (preset, labelRes) ->
                            val at = ReminderScheduler.resolve(preset)
                            val isSelected = chosenAt == at
                            Surface(
                                shape = CircleShape,
                                color = if (isSelected) colors.accentContainer else colors.surfaceCard,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) colors.accent.copy(alpha = 0.6f) else colors.border
                                ),
                                onClick = { chosenAt = at }
                            ) {
                                Text(
                                    text = stringResource(labelRes),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) colors.accent else colors.textPrimary,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(if (relative) 14.dp else 0.dp))

                // Always visible, so the chosen time is stated plainly whichever way it was set,
                // and always editable — a preset is a starting point, not a commitment.
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.surfaceCard,
                    border = BorderStroke(1.dp, colors.border),
                    onClick = {
                        val calendar = Calendar.getInstance().apply { timeInMillis = chosenAt }
                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                calendar.set(year, month, day)
                                TimePickerDialog(
                                    context,
                                    { _, hour, minute ->
                                        calendar.set(Calendar.HOUR_OF_DAY, hour)
                                        calendar.set(Calendar.MINUTE, minute)
                                        chosenAt = calendar.timeInMillis
                                        relative = false
                                    },
                                    calendar.get(Calendar.HOUR_OF_DAY),
                                    calendar.get(Calendar.MINUTE),
                                    true
                                ).show()
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatter.format(Date(chosenAt)),
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = stringResource(R.string.reminder_change),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.accent
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = { Text(stringResource(R.string.reminder_note_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.height(18.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (onClear != null && initialRemindAt != null) {
                        Button(
                            onClick = onClear,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.surfaceCard,
                                contentColor = colors.destructive
                            ),
                            border = BorderStroke(1.dp, colors.border),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                        ) {
                            Text(stringResource(R.string.reminder_clear), fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Button(
                        onClick = { onSave(chosenAt, note.trim().takeIf { it.isNotBlank() }) },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.accent,
                            contentColor = colors.textOnAccent
                        ),
                        modifier = Modifier
                            .weight(1.4f)
                            .height(50.dp)
                    ) {
                        Text(stringResource(R.string.detail_save), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeTab(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = TuckTheme.colors
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) colors.accentContainer else colors.surfaceCard,
        border = BorderStroke(1.dp, if (selected) colors.accent else colors.border),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) colors.accent else colors.textSecondary
            )
        }
    }
}
