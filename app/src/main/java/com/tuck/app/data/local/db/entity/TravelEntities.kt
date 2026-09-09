package com.tuck.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One visit to one place, between two dates.
 *
 * A trip is stored as a place plus a span rather than as a track: the questions this feeds —
 * which countries, how many days, what is next — are all answered from a place and a span, and
 * a track would be both far larger and far more revealing than anything those questions need.
 *
 * Dates are stored as local-midnight epoch millis. A trip is a run of *days*, and a day is what
 * the calendar on the wall said where the traveller was standing, not an instant on a timeline —
 * so storing a moment would make a trip's length change with the reader's timezone.
 */
@Entity(
    tableName = "trips",
    indices = [
        Index(value = ["startDate"]),
        Index(value = ["countryCode"]),
        Index(value = ["source"])
    ]
)
data class TripEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** What the traveller calls this trip. Defaults to the place, and is theirs to change. */
    val title: String,

    /** ISO 3166-1 alpha-2, uppercase. The join key for every per-country figure. */
    val countryCode: String,
    val countryName: String,
    val city: String? = null,
    /** First-level subdivision — the US state, where there is one. */
    val region: String? = null,

    val latitude: Double? = null,
    val longitude: Double? = null,

    /** Local-midnight epoch millis, inclusive of both ends. */
    val startDate: Long,
    val endDate: Long,

    val notes: String? = null,

    /** [TripSource] name. Kept as text so a new source cannot renumber the stored ones. */
    val source: String = "MANUAL",

    /** How many photos backed a scanned trip; 0 for a trip entered by hand. */
    val photoCount: Int = 0,

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * A place the traveller wants to go, which is not yet a trip.
 *
 * Kept in its own table rather than as a flag on [TripEntity], because a wish has no dates and
 * a trip cannot exist without them. When a bucket-list place is finally visited it is marked
 * with the trip that fulfilled it rather than deleted, so the list keeps its own history.
 */
@Entity(
    tableName = "bucket_list_places",
    indices = [Index(value = ["countryCode"]), Index(value = ["achievedTripId"])]
)
data class BucketListPlaceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val place: String,
    val countryCode: String? = null,
    val countryName: String? = null,
    val notes: String? = null,
    /** Set once a trip covers this place; the entry stays, marked done. */
    val achievedTripId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
