package com.tuck.app.ui.travel

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tuck.app.R
import com.tuck.app.domain.model.BucketListPlace
import com.tuck.app.domain.model.Trip
import com.tuck.app.domain.model.TripSource
import com.tuck.app.ui.theme.TuckTheme
import java.util.Calendar
import java.util.Locale

/**
 * Adds or edits a trip by hand.
 *
 * The country is typed as a name or a code and resolved against the platform's own ISO table,
 * so the app carries no country list of its own and the codes every per-country figure joins on
 * are always real ISO ones — a free-text country would make "France" and "france" two places.
 */
@Composable
fun TripEditorDialog(
    trip: Trip?,
    onSave: (Trip) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    val colors = TuckTheme.colors
    val context = LocalContext.current

    var title by remember { mutableStateOf(trip?.title.orEmpty()) }
    var country by remember { mutableStateOf(trip?.countryName.orEmpty()) }
    var city by remember { mutableStateOf(trip?.city.orEmpty()) }
    var region by remember { mutableStateOf(trip?.region.orEmpty()) }
    var notes by remember { mutableStateOf(trip?.notes.orEmpty()) }
    var start by remember { mutableStateOf(trip?.startDate ?: startOfDayNow()) }
    var end by remember { mutableStateOf(trip?.endDate ?: startOfDayNow()) }
    var error by remember { mutableStateOf<String?>(null) }

    val countryRequired = stringResource(R.string.travel_country_required)
    val datesInvalid = stringResource(R.string.travel_dates_invalid)

    fun pickDate(current: Long, onPicked: (Long) -> Unit) {
        val calendar = Calendar.getInstance().apply { timeInMillis = current }
        DatePickerDialog(
            context,
            { _, year, month, day ->
                onPicked(
                    Calendar.getInstance().apply {
                        set(year, month, day, 0, 0, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                )
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = {
            Text(
                text = stringResource(
                    if (trip == null) R.string.travel_add_trip else R.string.travel_edit_trip
                ),
                fontWeight = FontWeight.ExtraBold,
                color = colors.textPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.travel_field_title)) },
                    singleLine = true,
                    colors = tuckFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = country,
                    onValueChange = { country = it },
                    label = { Text(stringResource(R.string.travel_field_country)) },
                    singleLine = true,
                    colors = tuckFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text(stringResource(R.string.travel_field_city)) },
                    singleLine = true,
                    colors = tuckFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = region,
                    onValueChange = { region = it },
                    label = { Text(stringResource(R.string.travel_field_region)) },
                    singleLine = true,
                    colors = tuckFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DateField(
                        label = stringResource(R.string.travel_field_start),
                        value = start,
                        onClick = { pickDate(start) { picked -> start = picked } },
                        modifier = Modifier.weight(1f)
                    )
                    DateField(
                        label = stringResource(R.string.travel_field_end),
                        value = end,
                        onClick = { pickDate(end) { picked -> end = picked } },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.travel_field_notes)) },
                    colors = tuckFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                error?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.destructive
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val resolved = resolveCountry(country)
                    when {
                        resolved == null -> error = countryRequired
                        end < start -> error = datesInvalid
                        else -> onSave(
                            (trip ?: Trip(
                                title = "",
                                countryCode = "",
                                countryName = "",
                                startDate = start,
                                endDate = end
                            )).copy(
                                title = title.trim().ifBlank {
                                    city.trim().ifBlank { resolved.second }
                                },
                                countryCode = resolved.first,
                                countryName = resolved.second,
                                city = city.trim().takeIf { it.isNotBlank() },
                                region = region.trim().takeIf { it.isNotBlank() },
                                notes = notes.trim().takeIf { it.isNotBlank() },
                                startDate = start,
                                endDate = end,
                                // An edited photo-import keeps its provenance: the trip really
                                // did come from the library, the traveller only corrected it.
                                source = trip?.source ?: TripSource.MANUAL
                            )
                        )
                    }
                }
            ) {
                Text(stringResource(R.string.travel_save), color = colors.accent)
            }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text(stringResource(R.string.travel_delete), color = colors.destructive)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.travel_cancel), color = colors.textSecondary)
                }
            }
        }
    )
}

@Composable
private fun DateField(
    label: String,
    value: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = formatDate(value),
        onValueChange = {},
        label = { Text(label) },
        readOnly = true,
        enabled = false,
        singleLine = true,
        colors = tuckFieldColors().let { it },
        modifier = modifier.clickable(onClick = onClick)
    )
}

/** Adds a place to the bucket list. */
@Composable
fun BucketPlaceDialog(
    onSave: (BucketListPlace) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = TuckTheme.colors
    var place by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = {
            Text(
                text = stringResource(R.string.travel_bucket_add),
                fontWeight = FontWeight.ExtraBold,
                color = colors.textPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = place,
                    onValueChange = { place = it },
                    label = { Text(stringResource(R.string.travel_field_place)) },
                    singleLine = true,
                    colors = tuckFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = country,
                    onValueChange = { country = it },
                    label = { Text(stringResource(R.string.travel_field_country)) },
                    singleLine = true,
                    colors = tuckFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.travel_field_notes)) },
                    colors = tuckFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = place.isNotBlank(),
                onClick = {
                    val resolved = resolveCountry(country)
                    onSave(
                        BucketListPlace(
                            place = place.trim(),
                            countryCode = resolved?.first,
                            countryName = resolved?.second,
                            notes = notes.trim().takeIf { it.isNotBlank() }
                        )
                    )
                }
            ) {
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

/**
 * Turns whatever was typed into an ISO code and a display name, or null if it is not a country.
 *
 * Matched against the platform's own ISO list, in the reader's language and in English, so
 * "Deutschland", "Germany" and "DE" all reach the same row. Without a canonical code, per-
 * country figures would split across spellings.
 */
internal fun resolveCountry(input: String): Pair<String, String>? {
    val query = input.trim()
    if (query.isEmpty()) return null

    if (query.length == 2) {
        val code = query.uppercase()
        if (code in Locale.getISOCountries()) {
            return code to Locale("", code).getDisplayCountry(Locale.getDefault())
        }
    }

    return Locale.getISOCountries().firstNotNullOfOrNull { code ->
        val locale = Locale("", code)
        val localName = locale.getDisplayCountry(Locale.getDefault())
        val englishName = locale.getDisplayCountry(Locale.ENGLISH)
        if (localName.equals(query, ignoreCase = true) ||
            englishName.equals(query, ignoreCase = true)
        ) {
            code to localName
        } else {
            null
        }
    }
}

private fun startOfDayNow(): Long = Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis
