package com.tuck.app

import com.tuck.app.data.ai.CloudAiProvider
import com.tuck.app.data.ai.NoOpAiProvider
import com.tuck.app.data.ai.OnDeviceAiProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiProviderTest {

    @Test
    fun testNoOpAiProviderIsOfflineAndReturnsEmpty() = runBlocking {
        val provider = NoOpAiProvider()

        assertTrue(provider.isAvailable)
        assertEquals("noop", provider.providerId)
        assertNull(provider.summarize("Some long article text that needs summarization"))
        assertTrue(provider.extractKeyPoints("Some text with points").isEmpty())
        assertTrue(provider.generateTags("Some text").isEmpty())
    }

    @Test
    fun testOnDeviceAiProviderBehavior() = runBlocking {
        val provider = OnDeviceAiProvider()

        assertEquals("on-device-nano", provider.providerId)
        assertFalse(provider.isAvailable)
        assertNull(provider.summarize("Sample text"))
    }

    @Test
    fun testCloudAiProviderRequiresConsentAndApiKey() = runBlocking {
        val provider = CloudAiProvider()

        assertEquals("cloud-gemini", provider.providerId)
        assertFalse(provider.isAvailable)

        // Add API key without consent -> still not available
        provider.apiKey = "AIzaSyTestKey123"
        provider.userConsented = false
        assertFalse(provider.isAvailable)

        // Add consent -> now available
        provider.userConsented = true
        assertTrue(provider.isAvailable)
    }
}
