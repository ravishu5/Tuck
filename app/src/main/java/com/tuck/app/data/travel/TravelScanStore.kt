package com.tuck.app.data.travel

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.travelScanStore: DataStore<Preferences> by
    preferencesDataStore(name = "tuck_travel_scan")

/**
 * Remembers how far through the photo library the last scan got.
 *
 * A library is tens of thousands of photos and every one of them costs an EXIF read, so a second
 * scan that starts from the beginning spends minutes re-deriving a history it already has. The
 * watermark is the moment of the newest photo the traveller has already been shown, so the next
 * scan only has to look at pictures taken after it.
 *
 * Stored as the photo's own timestamp rather than the clock time of the scan: a photo imported
 * today from a camera's SD card was taken months ago, and a clock-time watermark would skip it.
 */
@Singleton
class TravelScanStore @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private object Keys {
        val LAST_PHOTO_TAKEN_AT = longPreferencesKey("last_scanned_photo_taken_at")
    }

    /** Null until a scan has been reviewed; the whole library is fair game until then. */
    val lastScannedPhotoTakenAt: Flow<Long?> = context.travelScanStore.data.map {
        it[Keys.LAST_PHOTO_TAKEN_AT]?.takeIf { value -> value > 0 }
    }

    suspend fun watermark(): Long? = lastScannedPhotoTakenAt.first()

    /**
     * Moves the watermark forward, never back.
     *
     * Only called once a review has been completed, because a candidate the traveller never saw
     * a decision screen for has not been dealt with — cancelling a scan has to leave those
     * photos to be found again.
     */
    suspend fun advanceWatermark(photoTakenAt: Long) {
        if (photoTakenAt <= 0) return
        context.travelScanStore.edit { prefs ->
            val current = prefs[Keys.LAST_PHOTO_TAKEN_AT] ?: 0L
            if (photoTakenAt > current) prefs[Keys.LAST_PHOTO_TAKEN_AT] = photoTakenAt
        }
    }

    /** Forgets the watermark, so the next scan reads the library from the beginning. */
    suspend fun reset() {
        context.travelScanStore.edit { it.remove(Keys.LAST_PHOTO_TAKEN_AT) }
    }
}
