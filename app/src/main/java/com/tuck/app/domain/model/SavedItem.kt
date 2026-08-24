package com.tuck.app.domain.model

data class SavedComment(
    val author: String,
    val text: String,
    val score: Int? = null,
    val timestamp: Long? = null
)

data class SavedItem(
    val id: Long = 0,
    val contentType: ContentType,
    val title: String,
    val description: String? = null,
    val originalUrl: String? = null,
    val canonicalUrl: String? = null,
    val sourceDomain: String? = null,
    val sourceApp: String? = null,
    val mimeType: String? = null,
    val localFilePath: String? = null,
    val thumbnailPath: String? = null,
    val originalText: String? = null,
    val extractedText: String? = null,
    val ocrText: String? = null,
    val comments: List<SavedComment> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastOpenedAt: Long? = null,
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val isDeleted: Boolean = false,
    val processingStatus: ProcessingStatus = ProcessingStatus.PENDING,
    val entities: List<ExtractedEntity> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val collections: List<Collection> = emptyList()
) {
    val displayTitle: String
        get() = when {
            title.isNotBlank() -> title
            !originalUrl.isNullOrBlank() -> sourceDomain ?: originalUrl
            !originalText.isNullOrBlank() -> originalText.take(80)
            !extractedText.isNullOrBlank() -> extractedText.take(80)
            !ocrText.isNullOrBlank() -> ocrText.take(80)
            else -> "${contentType.displayName} item"
        }

    val displaySnippet: String
        get() = when {
            !description.isNullOrBlank() -> description
            !ocrText.isNullOrBlank() -> ocrText
            !extractedText.isNullOrBlank() -> extractedText
            !originalText.isNullOrBlank() -> originalText
            else -> ""
        }

    val hasExtractedContent: Boolean
        get() = !ocrText.isNullOrBlank() || !extractedText.isNullOrBlank() || !originalText.isNullOrBlank()
}
