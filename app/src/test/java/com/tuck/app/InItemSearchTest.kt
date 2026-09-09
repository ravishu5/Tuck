package com.tuck.app

import com.tuck.app.domain.model.InItemSearch
import com.tuck.app.domain.model.SearchableBlock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InItemSearchTest {

    private fun blocks(vararg pairs: Pair<String, String>) =
        pairs.mapIndexed { i, (label, text) ->
            SearchableBlock(id = "b$i", label = label, listIndex = i, text = text)
        }

    @Test
    fun findsEveryOccurrenceInReadingOrder() {
        val found = InItemSearch.occurrences("kotlin and more kotlin, then kotlin", "kotlin")

        assertEquals(3, found.size)
        assertEquals(0, found[0].first)
        assertTrue("matches come back in order", found[0].first < found[1].first)
    }

    @Test
    fun matchingIsCaseInsensitive() {
        assertEquals(2, InItemSearch.occurrences("Kotlin and KOTLIN", "kotlin").size)
    }

    @Test
    fun overlappingMatchesAreNotDoubleCounted() {
        // "aaaa" contains "aa" twice without overlap, not three times.
        assertEquals(2, InItemSearch.occurrences("aaaa", "aa").size)
    }

    @Test
    fun singleCharacterQueriesAreIgnored() {
        val found = InItemSearch.find(blocks("Body" to "a lot of text here"), "a")

        assertTrue("one letter would match nearly everything", found.isEmpty())
    }

    @Test
    fun blankAndWhitespaceQueriesFindNothing() {
        val body = blocks("Body" to "some text")
        assertTrue(InItemSearch.find(body, "").isEmpty())
        assertTrue(InItemSearch.find(body, "   ").isEmpty())
    }

    @Test
    fun reportsWhichBlockEachHitIsIn() {
        val found = InItemSearch.find(
            blocks(
                "Original post" to "how should I learn GNNs",
                "Comment from u/beta" to "start with GNNs then message passing"
            ),
            "gnns"
        )

        assertEquals(2, found.size)
        assertEquals("Original post", found[0].blockLabel)
        assertEquals("Comment from u/beta", found[1].blockLabel)
        assertEquals("the second hit points at the second list row", 1, found[1].listIndex)
    }

    @Test
    fun findsTextInsideRecognisedImageContent() {
        val found = InItemSearch.find(
            blocks("Recognised text" to "Nike Air Max 8999 Amazon"),
            "8999"
        )

        assertEquals(1, found.size)
        assertEquals("Recognised text", found.single().blockLabel)
    }

    @Test
    fun steppingCyclesInBothDirections() {
        assertEquals(1, InItemSearch.step(currentIndex = 0, total = 3, forward = true))
        assertEquals("forward from the last match wraps to the first",
            0, InItemSearch.step(currentIndex = 2, total = 3, forward = true))
        assertEquals("back from the first wraps to the last",
            2, InItemSearch.step(currentIndex = 0, total = 3, forward = false))
    }

    @Test
    fun steppingWithNoMatchesIsSafe() {
        assertEquals(0, InItemSearch.step(currentIndex = 0, total = 0, forward = true))
    }
}
