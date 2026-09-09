package com.tuck.app

import com.tuck.app.processing.extractors.GoogleMapsSourceExtractor
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Saved Google Maps locations.
 *
 * A Maps place page serves an empty `<title>` and empty OpenGraph tags, so nothing about a saved
 * location can come from the page — it all comes out of the URL. These cover the URL shapes the
 * Android share sheet actually produces.
 */
class GoogleMapsCaptureTest {

    private val maps = GoogleMapsSourceExtractor()

    @Test
    fun testRecognisesEveryMapsUrlShape() {
        listOf(
            "https://www.google.com/maps/place/Eiffel+Tower/@48.8584,2.2945,17z/",
            "https://maps.google.com/maps?q=48.8584,2.2945",
            "https://maps.app.goo.gl/abc123",
            "https://goo.gl/maps/abc123"
        ).forEach { assertTrue(it, maps.canHandle(it)) }

        assertTrue(!maps.canHandle("https://www.google.com/search?q=maps"))
    }

    @Test
    fun testParsesPlaceNameAndCoordinatesFromAShareUrl() {
        val place = GoogleMapsSourceExtractor.parse(
            "https://www.google.com/maps/place/Eiffel+Tower/@48.8584,2.2945,17z/"
        )
        assertEquals("Eiffel Tower", place.name)
        assertEquals("48.8584" to "2.2945", place.coordinates)
        assertEquals("17", place.zoom)
    }

    @Test
    fun testPinCoordinatesBeatTheViewportCentre() {
        // A share URL can be centred anywhere; !3d/!4d is the pin the reader actually saved.
        val place = GoogleMapsSourceExtractor.parse(
            "https://www.google.com/maps/place/Somewhere/@40.0000,-70.0000,12z/data=!4m5!3m4!3d48.8584!4d2.2945"
        )
        assertEquals("48.8584" to "2.2945", place.coordinates)
    }

    @Test
    fun testHandlesNegativeCoordinatesAndEncodedNames() {
        val place = GoogleMapsSourceExtractor.parse(
            "https://www.google.com/maps/place/Caf%C3%A9+Tortoni/@-34.6087,-58.3781,17z/"
        )
        assertEquals("Café Tortoni", place.name)
        assertEquals("-34.6087" to "-58.3781", place.coordinates)
    }

    @Test
    fun testPlainQueryCoordinatesForm() {
        val place = GoogleMapsSourceExtractor.parse("https://maps.google.com/maps?q=48.8584,2.2945")
        assertEquals("48.8584" to "2.2945", place.coordinates)
        assertNull(place.name)
    }

    @Test
    fun testShortLinkIsResolvedFromThePathGoogleLeavesInThePage() {
        // maps.app.goo.gl encodes nothing, but the page it redirects to carries the real path.
        val html = """
            <html><body><script>window.ES5DGURL='/maps/place/Eiffel+Tower/@48.8584,2.2945,17z/?dg\x3des5';</script></body></html>
        """.trimIndent()

        val place = GoogleMapsSourceExtractor.parse("https://maps.app.goo.gl/abc123", html)
        assertEquals("Eiffel Tower", place.name)
        assertEquals("48.8584" to "2.2945", place.coordinates)
        assertEquals("https://www.google.com/maps/place/Eiffel+Tower/@48.8584,2.2945,17z/", place.canonicalUrl)
    }

    @Test
    fun testShortLinkWithNoPayloadDegradesInsteadOfInventing() {
        val place = GoogleMapsSourceExtractor.parse("https://maps.app.goo.gl/abc123", "<html></html>")
        assertNull(place.name)
        assertNull(place.coordinates)
        assertNull(GoogleMapsSourceExtractor.embedUrl(place))
    }

    @Test
    fun testEmbedUrlPrefersCoordinatesAndKeepsZoom() {
        val place = GoogleMapsSourceExtractor.parse(
            "https://www.google.com/maps/place/Eiffel+Tower/@48.8584,2.2945,17z/"
        )
        assertEquals(
            "https://maps.google.com/maps?q=48.8584%2C2.2945&z=17&output=embed",
            GoogleMapsSourceExtractor.embedUrl(place)
        )
    }

    @Test
    fun testEmbedUrlFallsBackToThePlaceNameWhenThereAreNoCoordinates() {
        val place = GoogleMapsSourceExtractor.Place(name = "Eiffel Tower")
        val url = GoogleMapsSourceExtractor.embedUrl(place)!!
        assertTrue(url.contains("q=Eiffel%20Tower"))
        // No zoom in the URL means the default, not a missing parameter.
        assertTrue(url.contains("z=16"))
        assertTrue(url.endsWith("output=embed"))
    }

    @Test
    fun testExtractionTitlesAndIndexesTheLocation() = runBlocking {
        val result = maps.extract(
            "https://www.google.com/maps/place/Eiffel+Tower/@48.8584,2.2945,17z/",
            null
        )

        assertEquals("MAPS", result.platform)
        assertEquals("Eiffel Tower", result.title)
        assertEquals("Google Maps", result.community)
        // Searchable by name and by raw coordinates.
        assertTrue(result.bodyText!!.contains("Eiffel Tower"))
        assertTrue(result.bodyText!!.contains("48.8584, 2.2945"))
    }

    @Test
    fun testCoordinateOnlyLinkStillGetsAUsableTitle() = runBlocking {
        val result = maps.extract("https://maps.google.com/maps?q=48.8584,2.2945", null)
        assertEquals("Location at 48.8584, 2.2945", result.title)
    }

    @Test
    fun testUnparseableMapsLinkNeverInventsAPlace() = runBlocking {
        val result = maps.extract("https://maps.app.goo.gl/abc123", null)
        assertEquals("Google Maps location", result.title)
        assertNull(result.bodyText)
    }
}
