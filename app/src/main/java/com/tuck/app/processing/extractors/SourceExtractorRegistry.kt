package com.tuck.app.processing.extractors

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SourceExtractorRegistry @Inject constructor(
    private val redditExtractor: RedditSourceExtractor,
    private val youtubeExtractor: YouTubeSourceExtractor,
    private val twitterExtractor: TwitterSourceExtractor,
    private val genericWebExtractor: GenericWebSourceExtractor
) {

    private val extractors: List<SourceExtractor> = listOf(
        redditExtractor,
        youtubeExtractor,
        twitterExtractor,
        genericWebExtractor
    )

    fun getExtractor(url: String): SourceExtractor {
        return extractors.firstOrNull { it.canHandle(url) && it != genericWebExtractor }
            ?: genericWebExtractor
    }
}
