package com.tuck.app.data.ai

import com.tuck.app.domain.ai.AiProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudAiProvider @Inject constructor() : AiProvider {
    override val providerId: String = "cloud-gemini"
    
    var apiKey: String? = null
    var userConsented: Boolean = false

    override val isAvailable: Boolean
        get() = !apiKey.isNullOrBlank() && userConsented

    override suspend fun summarize(text: String): String? {
        if (!isAvailable || text.isBlank()) return null
        // Cloud summarization logic with user consented BYO API Key
        return null
    }

    override suspend fun extractKeyPoints(text: String): List<String> {
        if (!isAvailable || text.isBlank()) return emptyList()
        return emptyList()
    }

    override suspend fun generateTags(text: String): List<String> {
        if (!isAvailable || text.isBlank()) return emptyList()
        return emptyList()
    }
}
