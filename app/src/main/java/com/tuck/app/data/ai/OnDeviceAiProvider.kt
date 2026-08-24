package com.tuck.app.data.ai

import com.tuck.app.domain.ai.AiProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnDeviceAiProvider @Inject constructor() : AiProvider {
    override val providerId: String = "on-device-nano"
    override val isAvailable: Boolean = false // Enabled when on-device model is downloaded/supported

    override suspend fun summarize(text: String): String? {
        if (!isAvailable || text.isBlank()) return null
        // On-device text summarizer implementation
        return text.lines().firstOrNull { it.isNotBlank() }?.take(150)
    }

    override suspend fun extractKeyPoints(text: String): List<String> {
        if (!isAvailable || text.isBlank()) return emptyList()
        return text.split(". ")
            .filter { it.length > 20 }
            .take(3)
    }

    override suspend fun generateTags(text: String): List<String> {
        return emptyList()
    }
}
