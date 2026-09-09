package com.tuck.app

import com.tuck.app.domain.model.ContentType
import com.tuck.app.domain.model.SearchQueryParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class SearchQueryParserTest {

    private fun millis(year: Int, month: Int, day: Int): Long =
        Calendar.getInstance().apply { clear(); set(year, month - 1, day) }.timeInMillis

    @Test
    fun plainTextIsUntouched() {
        val parsed = SearchQueryParser.parse("nike running shoes")

        assertEquals("nike running shoes", parsed.freeText)
        assertTrue(parsed.tokens.isEmpty())
        assertNull(parsed.contentType)
    }

    @Test
    fun extractsOperatorsAndLeavesTheRestAsFreeText() {
        val parsed = SearchQueryParser.parse("source:reddit type:pdf machine learning")

        assertEquals("machine learning", parsed.freeText)
        assertEquals("reddit", parsed.sourceDomain)
        assertEquals(ContentType.PDF, parsed.contentType)
        assertEquals(2, parsed.tokens.size)
    }

    @Test
    fun unknownOperatorsFallThroughAsFreeTextRatherThanBeingSwallowed() {
        val parsed = SearchQueryParser.parse("note:something todo")

        assertEquals("note:something todo", parsed.freeText)
        assertTrue(parsed.tokens.isEmpty())
    }

    @Test
    fun aBareUrlIsNotMistakenForAnOperator() {
        val parsed = SearchQueryParser.parse("https://reddit.com/r/androidapps")

        assertEquals("https://reddit.com/r/androidapps", parsed.freeText)
        assertTrue(parsed.tokens.isEmpty())
    }

    @Test
    fun quotedValuesKeepTheirSpaces() {
        val parsed = SearchQueryParser.parse("""in:"Machine Learning" gnn""")

        assertEquals("gnn", parsed.freeText)
        assertEquals("Machine Learning", parsed.collectionName)
    }

    @Test
    fun isFavoriteAndArchivedSetFlags() {
        val favorite = SearchQueryParser.parse("is:favorite kotlin")
        assertTrue(favorite.isFavoriteOnly)
        assertEquals("kotlin", favorite.freeText)

        assertTrue(SearchQueryParser.parse("is:archived").isArchivedOnly)
        assertTrue("unknown is: value stays free text", SearchQueryParser.parse("is:banana").tokens.isEmpty())
    }

    @Test
    fun absoluteDatesParseAtDayMonthAndYearPrecision() {
        assertEquals(millis(2026, 3, 14), SearchQueryParser.parse("after:2026-03-14").createdAfter)
        assertEquals(millis(2026, 3, 1), SearchQueryParser.parse("after:2026-03").createdAfter)
        assertEquals(millis(2026, 1, 1), SearchQueryParser.parse("before:2026").createdBefore)
    }

    @Test
    fun relativeDatesResolveAgainstTheSuppliedClock() {
        val now = millis(2026, 8, 25)
        val lastMonth = SearchQueryParser.parse("after:last-month", now).createdAfter!!

        val expected = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.MONTH, -1)
        }
        assertEquals(Calendar.JULY, Calendar.getInstance().apply { timeInMillis = lastMonth }.get(Calendar.MONTH))
        assertTrue(lastMonth < now)
        assertEquals(expected.get(Calendar.YEAR), Calendar.getInstance().apply { timeInMillis = lastMonth }.get(Calendar.YEAR))
    }

    @Test
    fun nonsenseDatesAreIgnoredRatherThanCrashing() {
        val parsed = SearchQueryParser.parse("after:banana kotlin")

        assertNull(parsed.createdAfter)
        assertEquals("after:banana kotlin", parsed.freeText)
    }

    @Test
    fun tokensCarryTheirRawTextSoAChipCanRemoveThem() {
        val parsed = SearchQueryParser.parse("tag:nike shoes")

        assertEquals("tag:nike", parsed.tokens.single().raw)
        assertEquals("Tag: nike", parsed.tokens.single().label)
        assertEquals("nike", parsed.tag)
    }
}
