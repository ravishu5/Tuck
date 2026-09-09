package com.tuck.app.ui.travel

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tuck.app.R
import com.tuck.app.domain.model.BucketListPlace
import com.tuck.app.domain.model.CountryVisit
import com.tuck.app.domain.model.PhotoScanState
import com.tuck.app.domain.model.ScanFailure
import com.tuck.app.domain.model.ScanScope
import com.tuck.app.domain.model.TravelStats
import com.tuck.app.domain.model.Trip
import com.tuck.app.domain.model.TripSource
import com.tuck.app.ui.components.SectionLabel
import com.tuck.app.ui.components.TuckEmptyState
import com.tuck.app.ui.theme.TuckTheme
import com.tuck.app.ui.theme.pressScale
import androidx.compose.foundation.interaction.MutableInteractionSource
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

/**
 * The travel page: a history of where the traveller has been, and where they are going.
 *
 * It replaces the Inbox tab, which showed the same saves as Home under a different heading.
 */
@Composable
fun TravelScreen(
    viewModel: TravelViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scanState by viewModel.scanState.collectAsStateWithLifecycle()
    val review by viewModel.review.collectAsStateWithLifecycle()
    val colors = TuckTheme.colors
    val context = LocalContext.current

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var tab by remember { mutableStateOf(TravelTab.TRIPS) }
    // The trip the map is looking at. Tapping a row moves the map rather than opening a form:
    // the first thing anyone wants from a trip in a travel history is to see where it was.
    var focusedTrip by remember { mutableStateOf<Trip?>(null) }

    /**
     * Points the map at a trip and brings the map back on screen.
     *
     * Zooming alone is no use from halfway down the list, where the map is scrolled off the
     * top — the map would move where nobody could see it. The map is the first item, so
     * showing it is a scroll to the top of the list.
     */
    fun focusOnMap(trip: Trip) {
        focusedTrip = trip
        scope.launch { listState.animateScrollToItem(0) }
    }
    var editingTrip by remember { mutableStateOf<Trip?>(null) }
    var showTripEditor by remember { mutableStateOf(false) }
    var showBucketEditor by remember { mutableStateOf(false) }

    val photoPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    // Remembered across the permission round trip, so the choice of scope survives the system
    // dialog that sits between pressing the button and the scan actually starting.
    var pendingScope by remember { mutableStateOf(ScanScope.NEW_PHOTOS_ONLY) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        // READ_MEDIA_IMAGES is the one that gates the scan at all; ACCESS_MEDIA_LOCATION only
        // decides whether the photos come back with their coordinates intact, and the scan
        // reports that itself when every photo turns out to be location-free.
        if (granted[photoPermission] == true) viewModel.startPhotoScan(pendingScope)
    }

    fun requestScan(scope: ScanScope) {
        pendingScope = scope
        val needed = buildList {
            add(photoPermission)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(Manifest.permission.ACCESS_MEDIA_LOCATION)
            }
        }
        val missing = needed.filter {
            ContextCompat.checkSelfPermission(context, it) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) {
            viewModel.startPhotoScan(scope)
        } else {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        TravelHeader(stats = uiState.stats)

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (uiState.mappableTrips.isNotEmpty()) {
                item {
                    Surface(
                        shape = TuckTheme.shapes.large,
                        color = colors.surfaceCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth()
                            .height(220.dp)
                    ) {
                        TravelMap(
                            trips = uiState.mappableTrips,
                            focused = focusedTrip,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            if (uiState.upcoming.isNotEmpty()) {
                item {
                    Column {
                        SectionLabel(
                            text = stringResource(R.string.travel_upcoming),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(Modifier.height(10.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(uiState.upcoming, key = { it.id }) { trip ->
                                UpcomingTripCard(
                                    trip = trip,
                                    onClick = { focusOnMap(trip) }
                                )
                            }
                        }
                    }
                }
            }

            item {
                TravelTabRow(
                    selected = tab,
                    onSelect = { tab = it },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            when (tab) {
                TravelTab.TRIPS -> {
                    if (uiState.trips.isEmpty()) {
                        item {
                            TuckEmptyState(
                                title = stringResource(R.string.travel_empty_title),
                                description = stringResource(R.string.travel_empty_body),
                                icon = Icons.Outlined.Flight,
                                actionLabel = stringResource(R.string.travel_add_trip),
                                onAction = { editingTrip = null; showTripEditor = true }
                            )
                        }
                    } else {
                        items(uiState.trips, key = { it.id }) { trip ->
                            TripRow(
                                trip = trip,
                                isFocused = trip.id == focusedTrip?.id,
                                onClick = { focusOnMap(trip) },
                                onEdit = { editingTrip = trip; showTripEditor = true },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }

                TravelTab.COUNTRIES -> {
                    if (uiState.countries.isEmpty()) {
                        item {
                            EmptyNote(stringResource(R.string.travel_countries_empty))
                        }
                    } else {
                        items(uiState.countries, key = { it.countryCode }) { country ->
                            CountryRow(
                                visit = country,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }

                TravelTab.BUCKET_LIST -> {
                    if (uiState.bucketList.isEmpty()) {
                        item { EmptyNote(stringResource(R.string.travel_bucket_empty)) }
                    }
                    items(uiState.bucketList, key = { it.id }) { place ->
                        BucketRow(
                            place = place,
                            onToggleAchieved = {
                                viewModel.markBucketPlaceAchieved(
                                    place,
                                    if (place.isAchieved) null else 0L
                                )
                            },
                            onDelete = { viewModel.deleteBucketPlace(place.id) },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                    item {
                        OutlinedAction(
                            label = stringResource(R.string.travel_bucket_add),
                            icon = Icons.Filled.Add,
                            onClick = { showBucketEditor = true },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // The empty state already offers this as its own call to action, so
                    // repeating it here would put the same button on screen twice.
                    if (uiState.trips.isNotEmpty() || tab != TravelTab.TRIPS) {
                        OutlinedAction(
                            label = stringResource(R.string.travel_add_trip),
                            icon = Icons.Filled.Add,
                            onClick = { editingTrip = null; showTripEditor = true }
                        )
                    }
                    PhotoImportCard(
                        lastScannedPhotoAt = uiState.lastScannedPhotoAt,
                        onScanNew = { requestScan(ScanScope.NEW_PHOTOS_ONLY) },
                        onRescanAll = { requestScan(ScanScope.ENTIRE_LIBRARY) }
                    )
                }
            }
        }
    }

    // --- Scan and review ---------------------------------------------------------------

    when (val state = scanState) {
        is PhotoScanState.Scanning, is PhotoScanState.Naming -> {
            ScanProgressDialog(state = state, onCancel = viewModel::cancelScan)
        }

        is PhotoScanState.Failed -> {
            ScanFailedDialog(
                failure = state.reason,
                onDismiss = viewModel::cancelScan,
                onOpenSettings = {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null)
                        )
                    )
                }
            )
        }

        is PhotoScanState.Done -> {
            TripImportReviewSheet(
                review = review,
                onToggle = viewModel::toggleDetected,
                onSelectAll = { viewModel.setAllDetectedSelected(true) },
                onClearAll = { viewModel.setAllDetectedSelected(false) },
                onRename = viewModel::renameDetected,
                onMerge = { viewModel.mergeDetected(review.selectedKeys) },
                onImport = { viewModel.importSelected() },
                onDismiss = viewModel::cancelScan
            )
        }

        PhotoScanState.Idle -> Unit
    }

    if (showTripEditor) {
        TripEditorDialog(
            trip = editingTrip,
            onSave = { trip ->
                viewModel.saveTrip(trip)
                showTripEditor = false
                editingTrip = null
            },
            onDelete = editingTrip?.let { existing ->
                {
                    viewModel.deleteTrip(existing.id)
                    showTripEditor = false
                    editingTrip = null
                }
            },
            onDismiss = { showTripEditor = false; editingTrip = null }
        )
    }

    if (showBucketEditor) {
        BucketPlaceDialog(
            onSave = { place ->
                viewModel.addBucketPlace(place)
                showBucketEditor = false
            },
            onDismiss = { showBucketEditor = false }
        )
    }
}

// --- Header --------------------------------------------------------------------------

@Composable
private fun TravelHeader(stats: TravelStats) {
    val colors = TuckTheme.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp, bottom = 16.dp)
    ) {
        Text(
            text = stringResource(R.string.travel_title),
            style = TuckTheme.typography.displayLarge,
            color = colors.textPrimary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.travel_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary
        )

        Spacer(Modifier.height(14.dp))

        // Built outside the list builder: `stringResource` is composable and the `items` block
        // is not, so resolving labels in there does not compile.
        val tiles = buildList {
            add(stringResource(R.string.travel_stat_countries) to stats.countryCount)
            add(stringResource(R.string.travel_stat_cities) to stats.cityCount)
            add(stringResource(R.string.travel_stat_days) to stats.daysTravelled)
            add(stringResource(R.string.travel_stat_trips) to stats.tripCount)
            add(stringResource(R.string.travel_stat_continents) to stats.continentCount)
            // Only shown to someone who has actually been to the US, where the figure means
            // something; everyone else gets a tile reading zero forever.
            if (stats.usStateCount > 0) {
                add(stringResource(R.string.travel_stat_states) to stats.usStateCount)
            }
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(tiles) { (label, value) ->
                StatTile(label = label, value = value)
            }
        }
    }
}

@Composable
private fun StatTile(label: String, value: Int) {
    val colors = TuckTheme.colors
    Surface(
        shape = TuckTheme.shapes.medium,
        color = colors.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = value.toString(),
                style = TuckTheme.typography.displayMedium,
                color = colors.textPrimary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary,
                maxLines = 1
            )
        }
    }
}

// --- Tabs ----------------------------------------------------------------------------

@Composable
private fun TravelTabRow(
    selected: TravelTab,
    onSelect: (TravelTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = TuckTheme.colors
    val shapes = TuckTheme.shapes

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shapes.pill)
            .background(colors.surfaceCard)
            .border(1.dp, colors.borderSubtle, shapes.pill)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val labels = mapOf(
            TravelTab.TRIPS to stringResource(R.string.travel_tab_trips),
            TravelTab.COUNTRIES to stringResource(R.string.travel_tab_countries),
            TravelTab.BUCKET_LIST to stringResource(R.string.travel_tab_bucket)
        )
        TravelTab.entries.forEach { entry ->
            val isSelected = entry == selected
            val source = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .pressScale(source, pressedScale = 0.95f)
                    .clip(shapes.pill)
                    .background(if (isSelected) colors.surface else Color.Transparent)
                    .then(
                        if (isSelected) Modifier.border(1.dp, colors.border, shapes.pill)
                        else Modifier
                    )
                    .clickable(interactionSource = source, indication = null) { onSelect(entry) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = labels.getValue(entry),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) colors.textPrimary else colors.textSecondary,
                    maxLines = 1
                )
            }
        }
    }
}

// --- Rows ----------------------------------------------------------------------------

@Composable
private fun TripRow(
    trip: Trip,
    isFocused: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = TuckTheme.colors
    val source = remember { MutableInteractionSource() }

    Surface(
        shape = TuckTheme.shapes.large,
        // The row the map is showing is marked, so it is obvious which pin is which once the
        // map has flown somewhere.
        color = if (isFocused) colors.accentContainer else colors.surfaceCard,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isFocused) colors.accent else colors.borderSubtle
        ),
        modifier = modifier
            .fillMaxWidth()
            .pressScale(source)
            .clickable(interactionSource = source, indication = null, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(colors.accentContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = flagFor(trip.countryCode),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = trip.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = trip.locationLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = dateRange(trip.startDate, trip.endDate),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (trip.source == TripSource.PHOTO_IMPORT) {
                    Text(
                        text = stringResource(R.string.travel_source_photos),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textMuted
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            Text(
                text = dayLabel(trip.days),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = colors.accent
            )

            // Editing needs its own control now that the row itself moves the map.
            IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = stringResource(R.string.travel_edit_trip),
                    tint = colors.textMuted,
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
}

@Composable
private fun UpcomingTripCard(trip: Trip, onClick: () -> Unit) {
    val colors = TuckTheme.colors
    val source = remember { MutableInteractionSource() }

    Surface(
        shape = TuckTheme.shapes.large,
        color = colors.accentContainer,
        modifier = Modifier
            .width(190.dp)
            .pressScale(source)
            .clickable(interactionSource = source, indication = null, onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = flagFor(trip.countryCode), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                text = trip.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = trip.locationLabel,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = dateRange(trip.startDate, trip.endDate),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted,
                maxLines = 1
            )
            if (trip.isInProgress()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.travel_in_progress),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.accent
                )
            }
        }
    }
}

@Composable
private fun CountryRow(visit: CountryVisit, modifier: Modifier = Modifier) {
    val colors = TuckTheme.colors

    Surface(
        shape = TuckTheme.shapes.large,
        color = colors.surfaceCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = flagFor(visit.countryCode), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = visit.countryName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                if (visit.regions.isNotEmpty()) {
                    Text(
                        text = visit.regions.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (visit.cities.isNotEmpty()) {
                    Text(
                        text = visit.cities.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = dayLabel(visit.days),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.accent
                )
                Text(
                    text = tripLabel(visit.tripCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textMuted
                )
            }
        }
    }
}

@Composable
private fun BucketRow(
    place: BucketListPlace,
    onToggleAchieved: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = TuckTheme.colors

    Surface(
        shape = TuckTheme.shapes.large,
        color = colors.surfaceCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onToggleAchieved) {
                Icon(
                    imageVector = if (place.isAchieved) Icons.Filled.Check else Icons.Filled.Flag,
                    contentDescription = stringResource(R.string.travel_bucket_mark_visited),
                    tint = if (place.isAchieved) colors.success else colors.textMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = place.place,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (place.isAchieved) colors.textMuted else colors.textPrimary
                )
                place.countryName?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.travel_delete),
                    tint = colors.textMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// --- Actions -------------------------------------------------------------------------

@Composable
private fun OutlinedAction(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = TuckTheme.colors
    val source = remember { MutableInteractionSource() }

    Surface(
        shape = TuckTheme.shapes.medium,
        color = Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
        modifier = modifier
            .fillMaxWidth()
            .pressScale(source)
            .clickable(interactionSource = source, indication = null, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = colors.accent
            )
        }
    }
}

@Composable
private fun PhotoImportCard(
    lastScannedPhotoAt: Long?,
    onScanNew: () -> Unit,
    onRescanAll: () -> Unit
) {
    val colors = TuckTheme.colors
    val hasScanned = lastScannedPhotoAt != null

    Surface(
        shape = TuckTheme.shapes.large,
        color = colors.accentContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.PhotoLibrary,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(
                        if (hasScanned) R.string.travel_scan_new_photos
                        else R.string.travel_scan_photos
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.textPrimary
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                // Once there is a watermark the card says where the next scan will pick up,
                // so it is clear the whole library is not being read again.
                text = if (hasScanned) {
                    stringResource(R.string.travel_scan_since, formatDate(lastScannedPhotoAt!!))
                } else {
                    stringResource(R.string.travel_scan_photos_hint)
                },
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onScanNew,
                    shape = TuckTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent,
                        contentColor = colors.textOnAccent
                    )
                ) {
                    Text(
                        text = stringResource(
                            if (hasScanned) R.string.travel_scan_new_action
                            else R.string.travel_scan_photos
                        ),
                        fontWeight = FontWeight.Bold
                    )
                }
                if (hasScanned) {
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = onRescanAll) {
                        Text(
                            text = stringResource(R.string.travel_rescan_all),
                            fontWeight = FontWeight.Bold,
                            color = colors.accent
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyNote(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = TuckTheme.colors.textSecondary,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)
    )
}

// --- Formatting ----------------------------------------------------------------------

/**
 * A country's flag, built from its ISO code rather than shipped as 250 images.
 *
 * Regional indicator symbols are the two letters offset into a private block; every platform
 * with flag emoji renders the pair as the flag, and the ones without show the letters, which is
 * still the country. Returns a globe for the code the scanner uses when it could not name a
 * place at all.
 */
internal fun flagFor(countryCode: String?): String {
    val code = countryCode?.trim()?.uppercase().orEmpty()
    if (code.length != 2 || !code.all { it in 'A'..'Z' }) return "🌍"
    val base = 0x1F1E6
    val first = base + (code[0] - 'A')
    val second = base + (code[1] - 'A')
    return String(Character.toChars(first)) + String(Character.toChars(second))
}

@Composable
private fun dayLabel(days: Int): String =
    if (days == 1) stringResource(R.string.travel_day_count_one)
    else stringResource(R.string.travel_days_count, days)

@Composable
private fun tripLabel(trips: Int): String =
    if (trips == 1) stringResource(R.string.travel_trip_count_one)
    else stringResource(R.string.travel_trips_count, trips)

private val dateFormat: DateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM)

internal fun formatDate(millis: Long): String = dateFormat.format(Date(millis))

internal fun dateRange(start: Long, end: Long): String =
    if (start == end) formatDate(start) else "${formatDate(start)} – ${formatDate(end)}"
