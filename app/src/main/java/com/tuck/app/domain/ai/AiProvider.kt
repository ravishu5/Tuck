package com.tuck.app.domain.ai

interface AiProvider {
    val providerId: String
    val isAvailable: Boolean
    suspend fun summarize(text: String): String?
    suspend fun extractKeyPoints(text: String): List<String>
    suspend fun generateTags(text: String): List<String>
}
