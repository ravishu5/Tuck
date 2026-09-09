package com.tuck.app.processing.extractors

import java.net.URLDecoder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Extracts a saved Google Maps location.
 *
 * Measured 2026-09-02: a Maps place page answers a fetch with 810 KB whose `<title>` is empty and
 * whose `og:title` and `og:description` are empty strings — the only OpenGraph image is the
 * generic Maps app icon. So the generic extractor produced an item with no title and no preview,
 * which is what a saved location looked like.
 *
 * Everything useful is in the URL instead: Maps encodes the place name and the coordinates in the
 * path. That makes this the cheapest extractor in the app — no network is required at all for a
 * full-form link. Short links (`maps.app.goo.gl`) carry nothing, but Google leaves the resolved
 * place path in the page as `window.ES5DGURL`, so following the redirect and reading that
 * recovers the same information.
 */
@Singleton
class GoogleMapsSourceExtractor @Inject constructor() : SourceExtractor {

    override val platformName: String = "MAPS"

    /** What a Maps URL encodes: everything Tuck knows about a saved location. */
    data class Place(
        val name: String? = null,
        val coordinates: Pair<String, String>? = null,
        val zoom: String? = null,
        val canonicalUrl: String? = null
    )

    override fun canHandle(url: String): Boolean = canHandleUrl(url)

    override suspend fun extract(url: String, content: String?): ExtractedSourceData {
        val place = parse(url, content)

        val coordinateLabel = place.coordinates?.let { (lat, lng) -> "$lat, $lng" }
        val title = place.name
            ?: coordinateLabel?.let { "Location at $it" }
            ?: "Google Maps location"

        // Both the name and the raw coordinates go into the body so either finds the item later.
        val body = listOfNotNull(place.name, coordinateLabel)
            .joinToString("\n")
            .takeIf { it.isNotBlank() }

        return ExtractedSourceData(
            platform = platformName,
            title = title,
            description = coordinateLabel,
            bodyText = body,
            community = "Google Maps",
            canonicalUrl = place.canonicalUrl ?: url.substringBefore('?'),
            faviconUrl = FAVICON
        )
    }

    companion object {

        private const val FAVICON = "https://www.google.com/images/branding/product/ico/maps15_bnuw3a_32dp.ico"

        fun canHandleUrl(url: String): Boolean {
            val lower = url.lowercase()
            return HOSTS.any { lower.contains(it) }
        }

        private val HOSTS = listOf(
            "google.com/maps", "maps.google.", "maps.app.goo.gl", "goo.gl/maps"
        )

        /** `/@48.8584,2.2945,17z` — the viewport centre, present on most share URLs. */
        private val AT_COORDS = Regex("""@(-?\d+\.\d+),(-?\d+\.\d+)""")

        /** `!3d48.8584!4d2.2945` — the pin itself, more precise than the viewport when present. */
        private val PIN_COORDS = Regex("""!3d(-?\d+\.\d+)!4d(-?\d+\.\d+)""")

        /** `?q=48.8584,2.2945` — the plain query form. */
        private val QUERY_COORDS = Regex("""[?&]q=(-?\d+\.\d+),\s*(-?\d+\.\d+)""")

        private val PLACE_NAME = Regex("""/maps/place/([^/@?#]+)""")

        private val ZOOM = Regex("""@-?\d+\.\d+,-?\d+\.\d+,(\d+)(?:\.\d+)?z""")

        /** Google leaves the resolved place path in the page, which is how a short link is followed. */
        private val RESOLVED_PATH = Regex("""ES5DGURL\s*=\s*'([^']+)'""")

        private const val DEFAULT_ZOOM = "16"

        /**
         * Reads whatever the URL encodes, falling back to the resolved path Google embeds in the
         * page when the URL is a short link that encodes nothing.
         */
        fun parse(url: String, html: String? = null): Place {
            val fromUrl = parsePath(url)
            if (fromUrl.name != null || fromUrl.coordinates != null) return fromUrl

            val resolved = html?.let { RESOLVED_PATH.find(it)?.groupValues?.get(1) }
                ?: return fromUrl
            val path = resolved.substringBefore('?').replace("\\x3d", "=")
            return parsePath(path).copy(
                canonicalUrl = if (path.startsWith("http")) path else "https://www.google.com$path"
            )
        }

        private fun parsePath(url: String): Place {
            // The pin wins over the viewport centre: a share URL can be centred anywhere.
            val coordinates = PIN_COORDS.find(url)?.pair()
                ?: AT_COORDS.find(url)?.pair()
                ?: QUERY_COORDS.find(url)?.pair()

            val name = PLACE_NAME.find(url)?.groupValues?.get(1)
                ?.replace('+', ' ')
                ?.let { decode(it) }
                ?.trim()
                ?.takeIf { it.isNotBlank() && !it.startsWith("@") }

            return Place(
                name = name,
                coordinates = coordinates,
                zoom = ZOOM.find(url)?.groupValues?.get(1),
                canonicalUrl = url.takeIf { it.startsWith("http") }?.substringBefore('?')
            )
        }

        private fun MatchResult.pair(): Pair<String, String> =
            groupValues[1] to groupValues[2]

        private fun decode(value: String): String = try {
            URLDecoder.decode(value, "UTF-8")
        } catch (e: Exception) {
            value
        }

        /**
         * An interactive map for [place] using the keyless `output=embed` endpoint.
         *
         * The Maps Embed API proper needs an API key, which a local-first app with no server has
         * nowhere to put. `output=embed` is the long-standing keyless form and answers 200 with
         * no consent interstitial — verified 2026-09-02.
         *
         * Returns null when neither coordinates nor a name were recoverable, since a map of
         * nothing is worse than no map.
         */
        fun embedUrl(place: Place): String? {
            val query = place.coordinates?.let { (lat, lng) -> "$lat,$lng" }
                ?: place.name
                ?: return null
            val zoom = place.zoom ?: DEFAULT_ZOOM
            return "https://maps.google.com/maps" +
                "?q=${encode(query)}&z=$zoom&output=embed"
        }

        private fun encode(value: String): String =
            java.net.URLEncoder.encode(value, "UTF-8").replace("+", "%20")
    }
}
