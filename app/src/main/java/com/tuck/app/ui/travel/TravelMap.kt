package com.tuck.app.ui.travel

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tuck.app.domain.model.Trip
import com.tuck.app.ui.map.MapPin
import com.tuck.app.ui.map.TuckMap

/**
 * Everywhere the traveller has been, on one map.
 *
 * A thin translation from trips to pins over [TuckMap], which is also what a single saved
 * location uses — so a place Tuck knows about looks the same wherever it is drawn.
 */
@Composable
fun TravelMap(
    trips: List<Trip>,
    /**
     * The trip the map should be looking at. Changing it flies the map there; null frames every
     * trip at once, which is the view the page opens on.
     */
    focused: Trip? = null,
    modifier: Modifier = Modifier
) {
    TuckMap(
        pins = trips.mapNotNull { trip ->
            val lat = trip.latitude ?: return@mapNotNull null
            val lon = trip.longitude ?: return@mapNotNull null
            MapPin(
                id = trip.id,
                latitude = lat,
                longitude = lon,
                title = trip.title,
                subtitle = trip.countryName
            )
        },
        focusedId = focused?.id,
        modifier = modifier
    )
}
