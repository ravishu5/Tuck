package com.tuck.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.tuck.app.domain.model.ContentType
import com.tuck.app.processing.ShareParser
import com.tuck.app.processing.UrlMetadataProcessor
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ShareParserTest {

    private val context: Context = mockk(relaxed = true)
    private val urlMetadataProcessor = UrlMetadataProcessor()
    private lateinit var shareParser: ShareParser

    @Before
    fun setUp() {
        shareParser = ShareParser(
            context = context,
            urlMetadataProcessor = urlMetadataProcessor
        )
    }

    @Test
    fun testParseUrlSendIntent() {
        val mockBundle = mockk<Bundle>(relaxed = true)
        every { mockBundle.keySet() } returns setOf(Intent.EXTRA_TEXT)
        every { mockBundle.get(Intent.EXTRA_TEXT) } returns "Check this out: https://www.reddit.com/r/androiddev/comments/123"

        val intent = mockk<Intent>(relaxed = true)
        every { intent.action } returns Intent.ACTION_SEND
        every { intent.type } returns "text/plain"
        every { intent.getStringExtra(Intent.EXTRA_SUBJECT) } returns null
        every { intent.getStringExtra(Intent.EXTRA_TITLE) } returns null
        every { intent.getStringExtra(Intent.EXTRA_TEXT) } returns "Check this out: https://www.reddit.com/r/androiddev/comments/123"
        every { intent.getParcelableExtra<Uri>(any()) } returns null
        every { intent.getParcelableExtra(any(), any<Class<Uri>>()) } returns null
        every { intent.extras } returns mockBundle

        val result = shareParser.parseIntent(intent, "com.reddit.frontpage")

        assertNotNull(result)
        assertEquals(ContentType.URL, result?.contentType)
        assertEquals("https://www.reddit.com/r/androiddev/comments/123", result?.url)
        assertEquals("com.reddit.frontpage", result?.sourceApp)
        assertNotNull(result?.rawPayloadJson)
    }

    @Test
    fun testParseTextNoteSendIntent() {
        val intent = mockk<Intent>(relaxed = true)
        every { intent.action } returns Intent.ACTION_SEND
        every { intent.type } returns "text/plain"
        every { intent.getStringExtra(Intent.EXTRA_SUBJECT) } returns null
        every { intent.getStringExtra(Intent.EXTRA_TITLE) } returns null
        every { intent.getStringExtra(Intent.EXTRA_TEXT) } returns "Buy milk and eggs from the grocery store tomorrow."
        every { intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM) } returns null

        val result = shareParser.parseIntent(intent, "com.google.android.keep")

        assertNotNull(result)
        assertEquals(ContentType.TEXT, result?.contentType)
        assertEquals("Buy milk and eggs from the grocery store tomorrow.", result?.text)
        assertEquals("com.google.android.keep", result?.sourceApp)
    }

    @Test
    fun testParseImageStreamSendIntent() {
        val mockUri = mockk<Uri>(relaxed = true)
        every { mockUri.lastPathSegment } returns "screenshot_2026.png"

        val intent = mockk<Intent>(relaxed = true)
        every { intent.action } returns Intent.ACTION_SEND
        every { intent.type } returns "image/png"
        every { intent.getStringExtra(Intent.EXTRA_SUBJECT) } returns null
        every { intent.getStringExtra(Intent.EXTRA_TITLE) } returns null
        every { intent.getStringExtra(Intent.EXTRA_TEXT) } returns null
        every { intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM) } returns mockUri

        val result = shareParser.parseIntent(intent, "com.google.android.apps.photos")

        assertNotNull(result)
        assertEquals(ContentType.IMAGE, result?.contentType)
        assertEquals(listOf(mockUri), result?.streamUris)
        assertEquals("screenshot_2026", result?.title)
    }

    @Test
    fun testParsePdfStreamSendIntent() {
        val mockUri = mockk<Uri>(relaxed = true)
        every { mockUri.lastPathSegment } returns "research_paper.pdf"

        val intent = mockk<Intent>(relaxed = true)
        every { intent.action } returns Intent.ACTION_SEND
        every { intent.type } returns "application/pdf"
        every { intent.getStringExtra(Intent.EXTRA_SUBJECT) } returns null
        every { intent.getStringExtra(Intent.EXTRA_TITLE) } returns null
        every { intent.getStringExtra(Intent.EXTRA_TEXT) } returns null
        every { intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM) } returns mockUri

        val result = shareParser.parseIntent(intent, "com.adobe.reader")

        assertNotNull(result)
        assertEquals(ContentType.PDF, result?.contentType)
        assertEquals(listOf(mockUri), result?.streamUris)
        assertEquals("research_paper", result?.title)
    }

    @Test
    fun testParseVCardTextSendIntent() {
        val vCard = """
            BEGIN:VCARD
            VERSION:3.0
            FN:Alice Smith
            TEL:+14155550199
            EMAIL:alice@example.com
            ORG:Acme Corp
            TITLE:Software Engineer
            END:VCARD
        """.trimIndent()

        val intent = mockk<Intent>(relaxed = true)
        every { intent.action } returns Intent.ACTION_SEND
        every { intent.type } returns "text/x-vcard"
        every { intent.getStringExtra(Intent.EXTRA_SUBJECT) } returns null
        every { intent.getStringExtra(Intent.EXTRA_TITLE) } returns null
        every { intent.getStringExtra(Intent.EXTRA_TEXT) } returns vCard
        every { intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM) } returns null

        val result = shareParser.parseIntent(intent, "com.google.android.contacts")

        assertNotNull(result)
        assertEquals(ContentType.CONTACT, result?.contentType)
        assertEquals("Alice Smith", result?.title)
        assertTrue(result?.text?.contains("Alice Smith") == true)
        assertTrue(result?.text?.contains("+14155550199") == true)
        assertEquals("Alice Smith", result?.extraMetadata?.get("name"))
        assertEquals("+14155550199", result?.extraMetadata?.get("phone"))
        assertEquals("alice@example.com", result?.extraMetadata?.get("email"))
    }

    @Test
    fun testParseGeoUriSendIntent() {
        val intent = mockk<Intent>(relaxed = true)
        every { intent.action } returns Intent.ACTION_SEND
        every { intent.type } returns "text/plain"
        every { intent.getStringExtra(Intent.EXTRA_SUBJECT) } returns null
        every { intent.getStringExtra(Intent.EXTRA_TITLE) } returns null
        every { intent.getStringExtra(Intent.EXTRA_TEXT) } returns "geo:37.7749,-122.4194?q=San+Francisco"
        every { intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM) } returns null

        val result = shareParser.parseIntent(intent, "com.google.android.apps.maps")

        assertNotNull(result)
        assertEquals(ContentType.LOCATION, result?.contentType)
        assertEquals("San Francisco", result?.title)
        assertEquals("https://maps.google.com/?q=37.7749,-122.4194", result?.url)
        assertEquals("37.7749", result?.extraMetadata?.get("latitude"))
        assertEquals("-122.4194", result?.extraMetadata?.get("longitude"))
    }

    @Test
    fun testParseMultipleImageSendIntent() {
        val mockUri1 = mockk<Uri>(relaxed = true)
        val mockUri2 = mockk<Uri>(relaxed = true)

        val intent = mockk<Intent>(relaxed = true)
        every { intent.action } returns Intent.ACTION_SEND_MULTIPLE
        every { intent.type } returns "image/*"
        every { intent.getStringExtra(Intent.EXTRA_SUBJECT) } returns null
        every { intent.getStringExtra(Intent.EXTRA_TITLE) } returns null
        every { intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM) } returns arrayListOf(mockUri1, mockUri2)

        val result = shareParser.parseIntent(intent, "com.google.android.apps.photos")

        assertNotNull(result)
        assertEquals(ContentType.MULTI_IMAGE, result?.contentType)
        assertEquals(2, result?.streamUris?.size)
    }

    @Test
    fun testParseProcessTextIntent() {
        val intent = mockk<Intent>(relaxed = true)
        every { intent.action } returns Intent.ACTION_PROCESS_TEXT
        every { intent.type } returns "text/plain"
        every { intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT) } returns "https://kotlinlang.org/docs/home.html"

        val result = shareParser.parseIntent(intent, "com.android.chrome")

        assertNotNull(result)
        assertEquals(ContentType.URL, result?.contentType)
        assertEquals("https://kotlinlang.org/docs/home.html", result?.url)
        assertEquals("com.android.chrome", result?.sourceApp)
    }

    @Test
    fun testParseMalformedIntentReturnsNull() {
        val intent = mockk<Intent>(relaxed = true)
        every { intent.action } returns null

        val result = shareParser.parseIntent(intent, null)
        assertNull(result)
    }
}
