package com.tuck.app.domain.classifier

import com.tuck.app.domain.model.SavedItem

data class ClassificationResult(
    val primaryCategory: String,
    val matchedCategories: List<String>,
    val suggestedTags: List<String>,
    val confidence: Float
)

interface ContentClassifier {
    suspend fun classify(item: SavedItem): ClassificationResult
}
