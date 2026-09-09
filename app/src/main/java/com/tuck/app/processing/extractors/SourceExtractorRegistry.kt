package com.tuck.app.processing.extractors

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Picks the extractor for a URL.
 *
 * This is the single entry point for turning a link into structured data. Platform knowledge
 * lives in the extractors and nowhere else, so adding a site means adding one class here rather
 * than another branch in a shared processor.
 */
@Singleton
class SourceExtractorRegistry @Inject constructor(
    private val redditExtractor: RedditSourceExtractor,
    private val youtubeExtractor: YouTubeSourceExtractor,
    private val twitterExtractor: TwitterSourceExtractor,
    private val instagramExtractor: InstagramSourceExtractor,
    private val tiktokExtractor: TikTokSourceExtractor,
    private val linkedInExtractor: LinkedInSourceExtractor,
    private val mapsExtractor: GoogleMapsSourceExtractor,
    private val genericWebExtractor: GenericWebSourceExtractor
) {

    /** Order matters only in that the generic extractor, which handles everything, comes last. */
    private val extractors: List<SourceExtractor> = listOf(
        redditExtractor,
        youtubeExtractor,
        twitterExtractor,
        instagramExtractor,
        tiktokExtractor,
        linkedInExtractor,
        mapsExtractor
    )

    fun getExtractor(url: String): SourceExtractor =
        extractors.firstOrNull { it.canHandle(url) } ?: genericWebExtractor
}
