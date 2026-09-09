package com.tuck.app

import com.tuck.app.data.repository.KeywordSearchEngine
import com.tuck.app.processing.DuplicateDetector
import com.tuck.app.processing.UrlMetadataProcessor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test

class DuplicateDetectorTest {

    private lateinit var duplicateDetector: DuplicateDetector

    @Before
    fun setUp() {
        duplicateDetector = DuplicateDetector(UrlMetadataProcessor())
    }

    @Test
    fun testNormalizeUrlMatchesCanonicalForm() {
        val url1 = "https://example.com/item?id=100&utm_source=newsletter"
        val url2 = "https://example.com/item?id=100"

        val can1 = duplicateDetector.getCanonicalUrl(url1)
        val can2 = duplicateDetector.getCanonicalUrl(url2)

        assertEquals(can1, can2)
    }

    @Test
    fun testTextHashIgnoresWhitespaceDifferences() {
        val text1 = "React   performance   guide\nfor developers."
        val text2 = "React performance guide for developers."

        val hash1 = duplicateDetector.hashText(text1)
        val hash2 = duplicateDetector.hashText(text2)

        assertEquals(hash1, hash2)
    }

    @Test
    fun testTextHashDifferentiatesDifferentText() {
        val text1 = "React performance guide"
        val text2 = "Flutter performance guide"

        val hash1 = duplicateDetector.hashText(text1)
        val hash2 = duplicateDetector.hashText(text2)

        assertNotEquals(hash1, hash2)
    }
}
