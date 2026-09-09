package com.tuck.app.domain.model

import java.util.concurrent.TimeUnit

/** Where a trip came from, which decides how much the app is allowed to assume about it. */
enum class TripSource {
    /** Entered by the traveller. Never overwritten by a scan. */
    MANUAL,

    /** Reconstructed from the location data in the photo library, and confirmed on import. */
    PHOTO_IMPORT;

    companion object {
        fun fromString(value: String?): TripSource =
            entries.firstOrNull { it.name.equals(value?.trim(), ignoreCase = true) } ?: MANUAL
    }
}

/**
 * One visit to one place.
 *
 * [startDate] and [endDate] are local-midnight epoch millis and both ends count, so a trip that
 * starts and ends on the same day is one day long rather than none.
 */
data class Trip(
    val id: Long = 0,
    val title: String,
    val countryCode: String,
    val countryName: String,
    val city: String? = null,
    val region: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val startDate: Long,
    val endDate: Long,
    val notes: String? = null,
    val source: TripSource = TripSource.MANUAL,
    val photoCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /** Inclusive of both ends: a single-day trip is one day, not zero. */
    val days: Int
        get() = (TimeUnit.MILLISECONDS.toDays(endDate - startDate).toInt() + 1).coerceAtLeast(1)

    fun isUpcoming(now: Long = System.currentTimeMillis()): Boolean = endDate >= now

    fun isInProgress(now: Long = System.currentTimeMillis()): Boolean =
        startDate <= now && endDate >= now

    val hasCoordinates: Boolean get() = latitude != null && longitude != null

    /** The most specific name this trip has: a city if it knows one, else the country. */
    val placeLabel: String get() = city?.takeIf { it.isNotBlank() } ?: countryName

    /**
     * Where this was, written the way an address is: town, state, country.
     *
     * The state is what makes two identically-named towns tellable apart, and in a large
     * country it is the part that actually locates the place — "Araria, India" could be almost
     * anywhere, "Araria, Bihar" is somewhere. Repeated parts are dropped, because a city that
     * is also its own state (Delhi, Singapore) should not be printed twice.
     */
    val locationLabel: String
        get() = listOfNotNull(
            city?.takeIf { it.isNotBlank() },
            region?.takeIf { it.isNotBlank() },
            countryName.takeIf { it.isNotBlank() }
        ).distinctBy { it.trim().lowercase() }.joinToString(", ")
}

/** A place the traveller means to go. */
data class BucketListPlace(
    val id: Long = 0,
    val place: String,
    val countryCode: String? = null,
    val countryName: String? = null,
    val notes: String? = null,
    val achievedTripId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    val isAchieved: Boolean get() = achievedTripId != null
}

/** One country's row in the travel history. */
data class CountryVisit(
    val countryCode: String,
    val countryName: String,
    val days: Int,
    val tripCount: Int,
    val cities: List<String>,
    /** States or provinces visited in this country, so a big country reads as more than one place. */
    val regions: List<String>,
    val firstVisit: Long,
    val lastVisit: Long
)

/**
 * The figures shown at the top of the travel page.
 *
 * [daysTravelled] counts days spent on trips, and overlapping trips are counted once — two
 * trips filed for the same week are one week away from home, not two.
 */
data class TravelStats(
    val countryCount: Int = 0,
    val cityCount: Int = 0,
    val usStateCount: Int = 0,
    val tripCount: Int = 0,
    val daysTravelled: Int = 0,
    val continentCount: Int = 0,
    val longestTripDays: Int = 0,
    val currentYearCountryCount: Int = 0
)

/**
 * A trip the photo scan believes it found, before the traveller has agreed it happened.
 *
 * Nothing here is written to the vault until it is confirmed on the review screen, which is
 * also where a wrong place or a badly split span gets corrected — a history assembled from
 * guesses is only trustworthy if the guesses were shown before they were kept.
 */
data class DetectedTrip(
    /** Stable within one scan, so the review screen can track edits and merges. */
    val key: String,
    val title: String,
    val countryCode: String,
    val countryName: String,
    val city: String?,
    val region: String?,
    val latitude: Double,
    val longitude: Double,
    val startDate: Long,
    val endDate: Long,
    val photoCount: Int,
    /** True when a trip already in the vault covers the same country and dates. */
    val alreadyInVault: Boolean = false
) {
    val days: Int
        get() = (TimeUnit.MILLISECONDS.toDays(endDate - startDate).toInt() + 1).coerceAtLeast(1)

    /** Town, state, country — the same shape the trip list uses, so the review matches it. */
    val locationLabel: String
        get() = listOfNotNull(
            city?.takeIf { it.isNotBlank() },
            region?.takeIf { it.isNotBlank() },
            countryName.takeIf { it.isNotBlank() }
        ).distinctBy { it.trim().lowercase() }.joinToString(", ")

    fun toTrip(): Trip = Trip(
        title = title,
        countryCode = countryCode,
        countryName = countryName,
        city = city,
        region = region,
        latitude = latitude,
        longitude = longitude,
        startDate = startDate,
        endDate = endDate,
        source = TripSource.PHOTO_IMPORT,
        photoCount = photoCount
    )
}

/** How far a photo scan has got, so the screen can say something truthful while it runs. */
sealed interface PhotoScanState {
    data object Idle : PhotoScanState
    data class Scanning(val photosRead: Int, val totalPhotos: Int) : PhotoScanState
    data class Naming(val placesResolved: Int, val totalPlaces: Int) : PhotoScanState

    /**
     * [newestPhotoTakenAt] is the moment of the most recent photo this scan looked at, which
     * becomes the watermark once the traveller has finished reviewing what it found.
     */
    data class Done(
        val trips: List<DetectedTrip>,
        val newestPhotoTakenAt: Long
    ) : PhotoScanState

    data class Failed(val reason: ScanFailure) : PhotoScanState
}

/** How much of the library a scan should look at. */
enum class ScanScope {
    /** Only photos taken since the last reviewed scan. The everyday case. */
    NEW_PHOTOS_ONLY,

    /** The whole library, ignoring the watermark. For when the history needs rebuilding. */
    ENTIRE_LIBRARY
}

/** What an import actually did, so the traveller is told rather than left guessing. */
data class TripImportResult(
    /** Trips that were not in the history before. */
    val added: Int = 0,
    /** Existing trips whose dates grew to cover newly found photos. */
    val extended: Int = 0
) {
    val total: Int get() = added + extended
}

/** Why a scan could not produce a history, in terms the screen can explain to a person. */
enum class ScanFailure {
    /** The photo library was never opened to the app. */
    PERMISSION_DENIED,

    /** Granted for photos, but not for the location inside them, which is the whole input. */
    MEDIA_LOCATION_DENIED,

    /** The library was read, and not one photo in it carries a location. */
    NO_LOCATED_PHOTOS
}
