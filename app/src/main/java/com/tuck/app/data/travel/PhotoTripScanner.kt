package com.tuck.app.data.travel

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import com.tuck.app.domain.model.DetectedTrip
import com.tuck.app.domain.model.PhotoScanState
import com.tuck.app.domain.model.ScanFailure
import com.tuck.app.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.CoroutineDispatcher
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Reconstructs a travel history from the location data already in the photo library.
 *
 * The tedious part of a travel tracker is typing in years of trips nobody remembers the dates
 * of. The camera roll already knows: every photo carries where and when. Reading it is entirely
 * local — the library is read through MediaStore, coordinates come out of each file's own EXIF,
 * and nothing is uploaded. The one thing that may leave the device is turning a coordinate into
 * a place name, which is [Geocoder]'s business and is done once per detected trip rather than
 * once per photo.
 *
 * Nothing this produces is saved. It returns candidates for the review screen, because a
 * history assembled from inference is only worth having if the inferences were shown first.
 */
@Singleton
class PhotoTripScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    /**
     * A photo's location and moment — all this needs from a picture, and all it ever reads.
     */
    private data class LocatedPhoto(
        val takenAt: Long,
        val latitude: Double,
        val longitude: Double
    )

    /** A run of photos taken near each other, in space and in time. */
    private data class Cluster(
        val photos: MutableList<LocatedPhoto> = mutableListOf()
    ) {
        val start: Long get() = photos.minOf { it.takenAt }
        val end: Long get() = photos.maxOf { it.takenAt }
        val centroidLat: Double get() = photos.sumOf { it.latitude } / photos.size
        val centroidLon: Double get() = photos.sumOf { it.longitude } / photos.size
        val days: Int
            get() = (TimeUnit.MILLISECONDS.toDays(end - start).toInt() + 1).coerceAtLeast(1)
    }

    /**
     * Scans the library and emits progress, ending in [PhotoScanState.Done] or
     * [PhotoScanState.Failed].
     *
     * @param isAlreadySaved asked once per candidate, so trips the vault already holds arrive
     *   at the review screen pre-marked instead of becoming duplicates.
     */
    fun scan(
        since: Long?,
        isAlreadySaved: suspend (countryCode: String, start: Long, end: Long) -> Boolean
    ): Flow<PhotoScanState> = flow {
        val photos = try {
            readLocatedPhotos(since) { read, total ->
                emit(PhotoScanState.Scanning(read, total))
            }
        } catch (e: SecurityException) {
            emit(PhotoScanState.Failed(ScanFailure.PERMISSION_DENIED))
            return@flow
        }

        if (photos.isEmpty()) {
            emit(PhotoScanState.Failed(ScanFailure.NO_LOCATED_PHOTOS))
            return@flow
        }

        val clusters = cluster(photos)
        val home = homeCluster(clusters)
        val candidates = clusters.filter { it.isWorthKeeping(home) }

        val trips = mutableListOf<DetectedTrip>()
        candidates.forEachIndexed { index, cluster ->
            emit(PhotoScanState.Naming(index, candidates.size))
            val place = resolvePlace(cluster.centroidLat, cluster.centroidLon)
            val countryCode = place?.countryCode?.uppercase() ?: UNKNOWN_COUNTRY_CODE
            val start = cluster.start.atStartOfDay()
            val end = cluster.end.atStartOfDay()
            trips += DetectedTrip(
                key = "${cluster.start}-${cluster.centroidLat}-${cluster.centroidLon}",
                title = place?.city ?: place?.countryName ?: UNKNOWN_PLACE,
                countryCode = countryCode,
                countryName = place?.countryName ?: UNKNOWN_PLACE,
                city = place?.city,
                region = place?.region,
                latitude = cluster.centroidLat,
                longitude = cluster.centroidLon,
                startDate = start,
                endDate = end,
                photoCount = cluster.photos.size,
                alreadyInVault = countryCode != UNKNOWN_COUNTRY_CODE &&
                    isAlreadySaved(countryCode, start, end)
            )
        }

        emit(
            PhotoScanState.Done(
                trips = trips.sortedByDescending { it.startDate },
                newestPhotoTakenAt = photos.maxOf { it.takenAt }
            )
        )
    }.flowOn(ioDispatcher)

    // --- Reading -----------------------------------------------------------------------

    private suspend fun readLocatedPhotos(
        since: Long?,
        onProgress: suspend (read: Int, total: Int) -> Unit
    ): List<LocatedPhoto> {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_ADDED
        )
        val out = mutableListOf<LocatedPhoto>()

        // Narrowed in the query rather than by skipping rows in the loop: the expensive part is
        // opening each file to read its EXIF, and a row that never comes back is never opened.
        //
        // DATE_TAKEN is null for anything that did not come from a camera, and a null fails the
        // comparison, so those rows are also matched on DATE_ADDED — otherwise a photo saved
        // from a messaging app would be invisible to every scan after the first.
        val selection = since?.let {
            "(${MediaStore.Images.Media.DATE_TAKEN} > ? OR " +
                "(${MediaStore.Images.Media.DATE_TAKEN} IS NULL AND " +
                "${MediaStore.Images.Media.DATE_ADDED} > ?))"
        }
        val args = since?.let {
            arrayOf(it.toString(), TimeUnit.MILLISECONDS.toSeconds(it).toString())
        }

        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            args,
            "${MediaStore.Images.Media.DATE_TAKEN} ASC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val takenColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val addedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val total = cursor.count

            var index = 0
            while (cursor.moveToNext()) {
                index++
                if (index % PROGRESS_EVERY == 0) onProgress(index, total)

                // DATE_TAKEN is null for anything that did not come from a camera; DATE_ADDED
                // is seconds, and is only a fallback because when the file landed on the phone
                // is not when the picture was taken.
                val takenAt = cursor.getLong(takenColumn)
                    .takeIf { it > 0 }
                    ?: TimeUnit.SECONDS.toMillis(cursor.getLong(addedColumn))
                if (takenAt <= 0) continue

                val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    .buildUpon()
                    .appendPath(cursor.getLong(idColumn).toString())
                    .build()

                readLatLon(uri)?.let { (lat, lon) ->
                    out += LocatedPhoto(takenAt, lat, lon)
                }
            }
            onProgress(total, total)
        }
        return out
    }

    /**
     * Pulls a photo's coordinates out of its own EXIF.
     *
     * MediaStore redacts location from the file it hands back unless ACCESS_MEDIA_LOCATION is
     * granted *and* the URI is unredacted with `setRequireOriginal`, so a scan without that
     * permission reads a library of photos that all appear to have no location at all.
     */
    private fun readLatLon(uri: Uri): Pair<Double, Double>? = runCatching {
        val original = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.setRequireOriginal(uri)
        } else {
            uri
        }
        context.contentResolver.openInputStream(original)?.use { stream ->
            val latLong = ExifInterface(stream).latLong ?: return null
            val lat = latLong[0]
            val lon = latLong[1]
            // Null Island is what a camera writes when its GPS never got a fix.
            if (abs(lat) < 0.0001 && abs(lon) < 0.0001) null else lat to lon
        }
    }.getOrNull()

    // --- Clustering --------------------------------------------------------------------

    /**
     * Groups photos into trips.
     *
     * A new trip starts when the traveller was somewhere far away, or when enough time passed
     * that this is plainly a separate journey. Both tests are needed: distance alone splits a
     * two-week tour into a trip per city, and time alone merges a weekend in Paris into the
     * fortnight in Rome that followed it.
     */
    private fun cluster(photos: List<LocatedPhoto>): List<Cluster> {
        val sorted = photos.sortedBy { it.takenAt }
        val clusters = mutableListOf<Cluster>()
        var current = Cluster()

        for (photo in sorted) {
            if (current.photos.isEmpty()) {
                current.photos += photo
                continue
            }
            val movedFar = distanceKm(
                current.centroidLat, current.centroidLon, photo.latitude, photo.longitude
            ) > SPLIT_DISTANCE_KM

            // Counted in calendar days rather than hours. A stay is a run of days, and the gap
            // between the last photo of one day and the first of the next can be anything from
            // minutes to nearly two days depending on when the camera came out — so an hours
            // threshold either splits real trips or joins separate ones, depending on the hour.
            // A whole day with no photograph at all is the honest end of a stay.
            val daysApart = TimeUnit.MILLISECONDS.toDays(
                photo.takenAt.atStartOfDay() - current.end.atStartOfDay()
            )
            val longGap = daysApart >= SPLIT_GAP_DAYS

            if (movedFar || longGap) {
                clusters += current
                current = Cluster()
            }
            current.photos += photo
        }
        if (current.photos.isNotEmpty()) clusters += current
        return clusters
    }

    /**
     * Where the traveller lives, taken to be wherever they have taken photos on the most
     * separate occasions.
     *
     * Not the most photos: a fortnight's holiday easily out-shoots a year of home, but home is
     * the place you keep coming back to. Without this the history fills up with "trips" to the
     * end of your own street.
     */
    private fun homeCluster(clusters: List<Cluster>): Cluster? {
        if (clusters.size < MIN_CLUSTERS_TO_INFER_HOME) return null
        return clusters
            .groupBy { roundedCell(it.centroidLat, it.centroidLon) }
            .maxByOrNull { it.value.size }
            ?.value
            ?.maxByOrNull { it.photos.size }
    }

    /**
     * Whether a cluster is a trip rather than an afternoon out or a change of planes.
     *
     * A layover leaves a handful of photos over a few hours in a place nobody stayed; a day
     * out leaves photos close to home. Both look exactly like a trip to a clustering algorithm
     * and like nothing at all to a traveller, so both are dropped here.
     */
    private fun Cluster.isWorthKeeping(home: Cluster?): Boolean {
        if (photos.size < MIN_PHOTOS_PER_TRIP) return false
        if (end - start < MIN_TRIP_DURATION_MS && photos.size < LAYOVER_PHOTO_THRESHOLD) {
            return false
        }
        if (home != null) {
            val fromHome = distanceKm(centroidLat, centroidLon, home.centroidLat, home.centroidLon)
            if (fromHome < HOME_RADIUS_KM) return false
        }
        return true
    }

    // --- Naming ------------------------------------------------------------------------

    private data class Place(
        val city: String?,
        val region: String?,
        val countryName: String?,
        val countryCode: String?
    )

    /**
     * Turns a coordinate into a place name, or null when the device cannot.
     *
     * A coordinate with no name is still a real trip, so a failure here leaves the candidate in
     * place for the traveller to name on the review screen rather than discarding it.
     */
    @Suppress("DEPRECATION")
    private fun resolvePlace(lat: Double, lon: Double): Place? {
        if (!Geocoder.isPresent()) return null
        return runCatching {
            Geocoder(context, Locale.getDefault())
                .getFromLocation(lat, lon, 1)
                ?.firstOrNull()
                ?.toPlace()
        }.getOrNull()
    }

    private fun Address.toPlace() = Place(
        city = locality ?: subAdminArea ?: adminArea,
        region = adminArea,
        countryName = countryName,
        countryCode = countryCode
    )

    private companion object {
        /**
         * Two points more than this apart are not the same stop.
         *
         * A stop is somewhere you stayed, so the radius is about the width of a large city.
         * It was 200 km, which is wide enough to swallow a neighbouring country: a trip 52 km
         * from home clustered together with the photos taken at home either side of it, and
         * the recorded trip ran from the day before the traveller left to the day after they
         * got back.
         */
        const val SPLIT_DISTANCE_KM = 50.0

        /**
         * A gap of this many calendar days ends the stay.
         *
         * Two, so that a full day passing with no photograph anywhere is what separates one
         * visit from the next. One would split a weekend where nobody photographed the Sunday.
         */
        const val SPLIT_GAP_DAYS = 2L

        /** Anything shorter is a layover unless it was very heavily photographed. */
        val MIN_TRIP_DURATION_MS: Long = TimeUnit.HOURS.toMillis(6)
        const val LAYOVER_PHOTO_THRESHOLD = 12

        const val MIN_PHOTOS_PER_TRIP = 3

        /**
         * Inside this of home it is an afternoon out, not a trip.
         *
         * Has to stay below [SPLIT_DISTANCE_KM], or somewhere near enough to cluster separately
         * from home would still be thrown away as home. At 100 km it discarded a trip across a
         * national border 52 km away.
         */
        const val HOME_RADIUS_KM = 25.0

        /** Below this there is not enough of a pattern to call anywhere home. */
        const val MIN_CLUSTERS_TO_INFER_HOME = 4

        /** Roughly a degree, so nearby clusters land in the same bucket when finding home. */
        const val HOME_CELL_DEGREES = 1.0

        const val PROGRESS_EVERY = 50

        const val UNKNOWN_COUNTRY_CODE = "??"
        const val UNKNOWN_PLACE = "Unknown place"
    }

    private fun roundedCell(lat: Double, lon: Double): Pair<Long, Long> =
        Math.round(lat / HOME_CELL_DEGREES) to Math.round(lon / HOME_CELL_DEGREES)
}

/** Great-circle distance in kilometres. */
internal fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val earthRadiusKm = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2) * sin(dLat / 2) +
        cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2) * sin(dLon / 2)
    return earthRadiusKm * 2 * atan2(sqrt(a), sqrt(1 - a))
}

/**
 * Local midnight on the day this instant falls on.
 *
 * A trip is a run of days on the wall calendar, so both ends are pinned to the start of their
 * day. Without it a trip that began at 23:00 and ended at 01:00 the next night would measure as
 * two hours rather than two days.
 */
internal fun Long.atStartOfDay(): Long = Calendar.getInstance().apply {
    timeInMillis = this@atStartOfDay
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis
