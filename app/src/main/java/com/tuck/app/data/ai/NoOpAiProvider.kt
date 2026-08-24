package com.tuck.app.data.ai

import com.tuck.app.domain.ai.AiProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoOpAiProvider @Inject constructor() : AiProvider {
    override val providerId: String = "noop"
    override val isAvailable: Boolean = true

    override suspend fun summarize(text: String): String? = null

    override suspend fun extractKeyPoints(text: String): List<String> = emptyList()

    override suspend fun generateTags(text: String): List<String> = emptyList()
}
