package com.tuck.app.ui.travel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuck.app.data.travel.PhotoTripScanner
import com.tuck.app.data.travel.TravelScanStore
import com.tuck.app.domain.model.BucketListPlace
import com.tuck.app.domain.model.CountryVisit
import com.tuck.app.domain.model.DetectedTrip
import com.tuck.app.domain.model.PhotoScanState
import com.tuck.app.domain.model.ScanScope
import com.tuck.app.domain.model.TravelStats
import com.tuck.app.domain.model.TripImportResult
import com.tuck.app.domain.model.Trip
import com.tuck.app.domain.repository.TravelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Which slice of the history the page is showing. */
enum class TravelTab { TRIPS, COUNTRIES, BUCKET_LIST }

data class TravelUiState(
    val trips: List<Trip> = emptyList(),
    val upcoming: List<Trip> = emptyList(),
    val countries: List<CountryVisit> = emptyList(),
    val bucketList: List<BucketListPlace> = emptyList(),
    val stats: TravelStats = TravelStats(),
    /** When the newest already-reviewed photo was taken; null before any scan. */
    val lastScannedPhotoAt: Long? = null,
    val isLoading: Boolean = true
) {
    val isEmpty: Boolean get() = !isLoading && trips.isEmpty() && bucketList.isEmpty()

    /** Trips with coordinates, which are the only ones the map can place. */
    val mappableTrips: List<Trip> get() = trips.filter { it.hasCoordinates }
}

/**
 * The review sheet's state: what the scan found, and what the traveller has decided to keep.
 *
 * Selections and edits live here rather than in the scanner because they are answers to the
 * scan, not part of it — the scan can be rerun without losing them, and nothing reaches the
 * vault until [importSelected].
 */
data class ImportReviewState(
    val detected: List<DetectedTrip> = emptyList(),
    val selectedKeys: Set<String> = emptySet(),
    /** Titles the traveller has corrected, by candidate key. */
    val renamed: Map<String, String> = emptyMap(),
    /** Carried through the review so finishing it can move the scan watermark forward. */
    val newestPhotoTakenAt: Long = 0
) {
    val selectedCount: Int get() = selectedKeys.size

    fun titleFor(trip: DetectedTrip): String = renamed[trip.key] ?: trip.title
}

@HiltViewModel
class TravelViewModel @Inject constructor(
    private val repository: TravelRepository,
    private val scanner: PhotoTripScanner,
    private val scanStore: TravelScanStore
) : ViewModel() {

    val uiState: StateFlow<TravelUiState> = combine(
        repository.getTrips(),
        repository.getUpcomingTrips(),
        repository.getCountryVisits(),
        repository.getBucketList(),
        repository.getStats(),
        scanStore.lastScannedPhotoTakenAt
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        TravelUiState(
            trips = values[0] as List<Trip>,
            upcoming = values[1] as List<Trip>,
            countries = values[2] as List<CountryVisit>,
            bucketList = values[3] as List<BucketListPlace>,
            stats = values[4] as TravelStats,
            lastScannedPhotoAt = values[5] as Long?,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TravelUiState()
    )

    private val _scanState = MutableStateFlow<PhotoScanState>(PhotoScanState.Idle)
    val scanState: StateFlow<PhotoScanState> = _scanState.asStateFlow()

    private val _review = MutableStateFlow(ImportReviewState())
    val review: StateFlow<ImportReviewState> = _review.asStateFlow()

    private var scanJob: Job? = null

    // --- Photo import ------------------------------------------------------------------

    /**
     * Looks for trips in the photo library.
     *
     * Defaults to photos taken since the last reviewed scan. Re-reading a whole library costs an
     * EXIF open per photo, which on a real camera roll is minutes of work to rederive a history
     * the app already has — so the everyday case only looks at what is new, and rebuilding from
     * scratch is a deliberate choice.
     */
    fun startPhotoScan(scope: ScanScope = ScanScope.NEW_PHOTOS_ONLY) {
        if (scanJob?.isActive == true) return
        _scanState.value = PhotoScanState.Scanning(0, 0)
        scanJob = viewModelScope.launch {
            val since = when (scope) {
                ScanScope.NEW_PHOTOS_ONLY -> scanStore.watermark()
                ScanScope.ENTIRE_LIBRARY -> null
            }
            scanner.scan(since, repository::hasTripCovering).collect { state ->
                _scanState.value = state
                if (state is PhotoScanState.Done) {
                    _review.value = ImportReviewState(
                        detected = state.trips,
                        // Everything the vault does not already hold starts ticked: the common
                        // case is keeping the lot, and a duplicate is the one thing a scan
                        // should never add without being asked.
                        selectedKeys = state.trips
                            .filterNot { it.alreadyInVault }
                            .map { it.key }
                            .toSet(),
                        newestPhotoTakenAt = state.newestPhotoTakenAt
                    )
                }
            }
        }
    }

    fun cancelScan() {
        scanJob?.cancel()
        scanJob = null
        _scanState.value = PhotoScanState.Idle
        _review.value = ImportReviewState()
    }

    fun toggleDetected(key: String) {
        _review.update { state ->
            state.copy(
                selectedKeys = if (key in state.selectedKeys) {
                    state.selectedKeys - key
                } else {
                    state.selectedKeys + key
                }
            )
        }
    }

    fun setAllDetectedSelected(selected: Boolean) {
        _review.update { state ->
            state.copy(
                selectedKeys = if (selected) state.detected.map { it.key }.toSet() else emptySet()
            )
        }
    }

    fun renameDetected(key: String, title: String) {
        _review.update { it.copy(renamed = it.renamed + (key to title)) }
    }

    /**
     * Merges a run of candidates into one trip.
     *
     * The scan splits on distance, which turns a single tour into a trip per city. Only the
     * traveller knows whether that was three trips or one, so merging is theirs to do — the
     * merged trip keeps the earliest start, the latest end and the largest cluster's place.
     */
    fun mergeDetected(keys: Set<String>) {
        if (keys.size < 2) return
        _review.update { state ->
            val group = state.detected.filter { it.key in keys }
            if (group.size < 2) return@update state
            val anchor = group.maxBy { it.photoCount }
            val merged = anchor.copy(
                key = anchor.key,
                startDate = group.minOf { it.startDate },
                endDate = group.maxOf { it.endDate },
                photoCount = group.sumOf { it.photoCount }
            )
            val firstIndex = state.detected.indexOfFirst { it.key in keys }
            val remaining = state.detected.filterNot { it.key in keys }.toMutableList()
            remaining.add(firstIndex.coerceIn(0, remaining.size), merged)
            state.copy(
                detected = remaining,
                selectedKeys = state.selectedKeys - keys + merged.key
            )
        }
    }

    /**
     * Keeps the selected candidates and closes the review.
     *
     * The watermark moves here rather than when the scan finishes, because a candidate is only
     * dealt with once the traveller has seen it and decided — abandoning a review has to leave
     * those photos to be found again next time.
     */
    fun importSelected(onDone: (TripImportResult) -> Unit = {}) {
        viewModelScope.launch {
            val state = _review.value
            val toSave = state.detected
                .filter { it.key in state.selectedKeys }
                .map { detected -> detected.toTrip().copy(title = state.titleFor(detected)) }
            val result = repository.importTrips(toSave)
            scanStore.advanceWatermark(state.newestPhotoTakenAt)
            _scanState.value = PhotoScanState.Idle
            _review.value = ImportReviewState()
            onDone(result)
        }
    }

    // --- Trips and wishes --------------------------------------------------------------

    fun saveTrip(trip: Trip) {
        viewModelScope.launch { repository.saveTrip(trip) }
    }

    fun deleteTrip(id: Long) {
        viewModelScope.launch { repository.deleteTrip(id) }
    }

    fun addBucketPlace(place: BucketListPlace) {
        viewModelScope.launch { repository.saveBucketPlace(place) }
    }

    fun deleteBucketPlace(id: Long) {
        viewModelScope.launch { repository.deleteBucketPlace(id) }
    }

    /** Marks a wish as reached, tied to the trip that reached it. */
    fun markBucketPlaceAchieved(place: BucketListPlace, tripId: Long?) {
        viewModelScope.launch {
            repository.saveBucketPlace(place.copy(achievedTripId = tripId))
        }
    }
}

/** [MutableStateFlow.update] without pulling in the whole atomicfu-flavoured extension set. */
private inline fun <T> MutableStateFlow<T>.update(transform: (T) -> T) {
    while (true) {
        val current = value
        val next = transform(current)
        if (compareAndSet(current, next)) return
    }
}
