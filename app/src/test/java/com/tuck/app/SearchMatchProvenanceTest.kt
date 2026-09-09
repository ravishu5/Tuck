package com.tuck.app

import com.tuck.app.data.repository.matchFieldsIn
import com.tuck.app.data.repository.toMatchFieldList
import com.tuck.app.domain.model.MatchField
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A search result claims *why* it matched. These tests hold that claim honest: the
 * chip has to name a field the query genuinely appears in, or it is worse than no
 * chip at all.
 */
class SearchMatchProvenanceTest {

    // --- Indexed path: FTS column indices ------------------------------------------------

    /**
     * The mapping is only correct relative to the column order declared in
     * `SavedItemFtsDaoImpl.CREATE_TABLE`. If that CREATE statement is ever reordered,
     * this test is the thing that should fail.
     */
    @Test
    fun `fts columns map to the field each one holds`() {
        assertEquals(listOf(MatchField.TITLE), setOf(0).toMatchFieldList())
        assertEquals(listOf(MatchField.DESCRIPTION), setOf(1).toMatchFieldList())
        assertEquals(listOf(MatchField.URL), setOf(2).toMatchFieldList())
        assertEquals(listOf(MatchField.DOMAIN), setOf(3).toMatchFieldList())
        assertEquals(listOf(MatchField.OCR), setOf(6).toMatchFieldList())
        assertEquals(listOf(MatchField.TAG), setOf(7).toMatchFieldList())
        assertEquals(listOf(MatchField.ENTITY), setOf(8).toMatchFieldList())
    }

    /** `originalText` and `extractedText` are one idea to a reader: the item's text. */
    @Test
    fun `both body text columns collapse to a single text field`() {
        assertEquals(listOf(MatchField.TEXT), setOf(4, 5).toMatchFieldList())
    }

    @Test
    fun `fields come back strongest first, not in column order`() {
        // A hit in the URL and a hit in the title: the title is the one worth showing.
        assertEquals(
            listOf(MatchField.TITLE, MatchField.TAG, MatchField.URL),
            setOf(2, 7, 0).toMatchFieldList()
        )
    }

    @Test
    fun `an unknown column is dropped rather than guessed at`() {
        assertEquals(listOf(MatchField.TITLE), setOf(0, 99).toMatchFieldList())
    }

    // --- Fallback path: LIKE over saved_items ---------------------------------------------

    private fun fieldsFor(
        query: String,
        title: String? = null,
        description: String? = null,
        originalText: String? = null,
        extractedText: String? = null,
        commentsJson: String? = null,
        ocrText: String? = null,
        originalUrl: String? = null,
        sourceDomain: String? = null
    ) = matchFieldsIn(
        query = query,
        title = title,
        description = description,
        originalText = originalText,
        extractedText = extractedText,
        commentsJson = commentsJson,
        ocrText = ocrText,
        originalUrl = originalUrl,
        sourceDomain = sourceDomain
    )

    @Test
    fun `text found only inside a screenshot is reported as recognised text`() {
        val fields = fieldsFor(
            query = "ramen",
            title = "Screenshot",
            ocrText = "Best tonkotsu ramen in Bengaluru"
        )
        assertEquals(listOf(MatchField.OCR), fields)
    }

    @Test
    fun `a match is reported for every field it really appears in`() {
        val fields = fieldsFor(
            query = "kotlin",
            title = "Kotlin coroutines guide",
            originalUrl = "https://kotlinlang.org/docs/coroutines.html",
            sourceDomain = "kotlinlang.org"
        )
        assertEquals(listOf(MatchField.TITLE, MatchField.URL, MatchField.DOMAIN), fields)
    }

    @Test
    fun `a hit in an archived comment is named as a comment`() {
        val fields = fieldsFor(
            query = "sourdough",
            title = "Bread thread",
            commentsJson = """[{"body":"my sourdough starter died"}]"""
        )
        assertEquals(listOf(MatchField.COMMENT), fields)
    }

    @Test
    fun `matching is case insensitive, as the LIKE query is`() {
        assertEquals(listOf(MatchField.TITLE), fieldsFor(query = "REACT", title = "React hooks"))
    }

    @Test
    fun `nothing is claimed when the query appears nowhere`() {
        assertTrue(fieldsFor(query = "zebra", title = "React hooks").isEmpty())
    }

    /** A blank query matches everything under `LIKE '%%'`; claiming a reason would be a lie. */
    @Test
    fun `a blank query claims no reason at all`() {
        assertTrue(fieldsFor(query = "   ", title = "React hooks").isEmpty())
    }

    @Test
    fun `null columns are not treated as matches`() {
        assertTrue(fieldsFor(query = "react").isEmpty())
    }
}
