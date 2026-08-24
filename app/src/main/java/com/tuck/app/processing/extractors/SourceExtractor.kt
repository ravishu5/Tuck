package com.tuck.app.processing.extractors

data class ExtractedComment(
    val id: String,
    val parentId: String? = null,
    val author: String,
    val bodyText: String,
    val score: Int = 0,
    val depth: Int = 0,
    val path: String = "0001",
    val postedAt: Long? = null,
    val replies: List<ExtractedComment> = emptyList()
)

data class ExtractedSourceData(
    val platform: String,
    val title: String? = null,
    val description: String? = null,
    val bodyText: String? = null,
    val authorHandle: String? = null,
    val authorDisplay: String? = null,
    val community: String? = null,
    val score: Int = 0,
    val commentCount: Int = 0,
    val postedAt: Long? = null,
    val leadImageUrl: String? = null,
    val mediaUrls: List<String> = emptyList(),
    val comments: List<ExtractedComment> = emptyList(),
    val rawJson: String? = null
)

interface SourceExtractor {
    val platformName: String
    fun canHandle(url: String): Boolean
    suspend fun extract(url: String, content: String? = null): ExtractedSourceData
}
