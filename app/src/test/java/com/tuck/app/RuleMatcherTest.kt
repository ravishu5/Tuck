package com.tuck.app

import com.tuck.app.domain.model.Collection
import com.tuck.app.domain.model.ContentType
import com.tuck.app.domain.model.RuleMatcher
import com.tuck.app.domain.model.SavedItem
import com.tuck.app.domain.model.SearchQueryParser
import com.tuck.app.domain.model.Tag
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class RuleMatcherTest {

    private fun item(
        title: String = "Untitled",
        type: ContentType = ContentType.URL,
        domain: String? = null,
        url: String? = null,
        text: String? = null,
        ocr: String? = null,
        tags: List<String> = emptyList(),
        collections: List<String> = emptyList(),
        favorite: Boolean = false,
        archived: Boolean = false,
        createdAt: Long = System.currentTimeMillis()
    ) = SavedItem(
        contentType = type,
        title = title,
        sourceDomain = domain,
        originalUrl = url,
        originalText = text,
        ocrText = ocr,
        isFavorite = favorite,
        isArchived = archived,
        createdAt = createdAt,
        tags = tags.mapIndexed { i, n -> Tag(id = i.toLong(), name = n) },
        collections = collections.mapIndexed { i, n -> Collection(id = i.toLong(), name = n) }
    )

    private fun matches(rule: String, item: SavedItem) =
        RuleMatcher.matches(SearchQueryParser.parse(rule), item)

    // --- the headline case from the research ---

    @Test
    fun redditLinksMatchARedditRule() {
        val redditPost = item(domain = "reddit.com", url = "https://reddit.com/r/androidapps/comments/x")

        assertTrue(matches("source:reddit", redditPost))
        assertFalse(matches("source:reddit", item(domain = "youtube.com")))
    }

    // --- safety: the failure that would be worst ---

    @Test
    fun anEmptyRuleMatchesNothing() {
        assertFalse("a blank rule must never file everything", matches("", item(title = "anything")))
        assertFalse(matches("   ", item(title = "anything")))
    }

    @Test
    fun aRuleOfOnlyUnknownOperatorsFallsBackToTextAndStillRequiresAMatch() {
        // `note:` is not an operator, so it stays free text rather than matching everything.
        assertFalse(matches("note:something", item(title = "unrelated")))
        assertTrue(matches("note:something", item(title = "a note:something here")))
    }

    // --- conditions combine with AND ---

    @Test
    fun everyStatedConditionMustHold() {
        val pdfFromArxiv = item(type = ContentType.PDF, domain = "arxiv.org", title = "Attention paper")

        assertTrue(matches("type:pdf source:arxiv", pdfFromArxiv))
        assertFalse("wrong type", matches("type:image source:arxiv", pdfFromArxiv))
        assertFalse("wrong source", matches("type:pdf source:nature", pdfFromArxiv))
    }

    @Test
    fun freeTextMustMatchEveryTerm() {
        val shoes = item(title = "Nike Air Max running shoes")

        assertTrue(matches("nike shoes", shoes))
        assertFalse("one missing term fails the whole rule", matches("nike sandals", shoes))
    }

    // --- where the text is found ---

    @Test
    fun freeTextSearchesOcrAndNotJustTheTitle() {
        val screenshot = item(title = "Screenshot", type = ContentType.IMAGE, ocr = "Nike Air Max 8999 Amazon")

        assertTrue("a rule should see text recognised inside an image", matches("nike", screenshot))
        assertTrue(matches("amazon", screenshot))
    }

    @Test
    fun freeTextMatchingIsCaseInsensitive() {
        assertTrue(matches("NIKE", item(title = "nike shoes")))
        assertTrue(matches("nike", item(title = "NIKE SHOES")))
    }

    // --- individual operators ---

    @Test
    fun tagOperatorMatchesOnTagsNotOnBodyText() {
        assertTrue(matches("tag:shopping", item(tags = listOf("shopping"))))
        assertFalse(
            "a body mention must not satisfy a tag condition",
            matches("tag:shopping", item(text = "shopping came up in conversation"))
        )
    }

    @Test
    fun collectionOperatorMatchesCurrentMembership() {
        assertTrue(matches("in:Research", item(collections = listOf("Research"))))
        assertTrue("collection names are case-insensitive", matches("in:research", item(collections = listOf("Research"))))
        assertFalse(matches("in:Research", item(collections = listOf("Shopping"))))
    }

    @Test
    fun favoriteAndArchivedFlagsAreHonoured() {
        assertTrue(matches("is:favorite", item(favorite = true)))
        assertFalse(matches("is:favorite", item(favorite = false)))
        assertTrue(matches("is:archived", item(archived = true)))
    }

    @Test
    fun dateBoundsAreApplied() {
        val cal = Calendar.getInstance().apply { clear(); set(2026, Calendar.MARCH, 15) }
        val march = item(createdAt = cal.timeInMillis)

        assertTrue(matches("after:2026-01", march))
        assertTrue(matches("before:2026-06", march))
        assertFalse(matches("after:2026-06", march))
        assertFalse(matches("before:2026-01", march))
    }

    @Test
    fun sourceMatchesTheUrlWhenTheDomainFieldIsNotSetYet() {
        // Enrichment may not have filled sourceDomain when a rule first runs.
        val bare = item(domain = null, url = "https://www.reddit.com/r/kotlin")

        assertTrue(matches("source:reddit", bare))
    }

    @Test
    fun aRealisticMultiConditionRule() {
        val target = item(
            title = "Best backend for Android",
            type = ContentType.URL,
            domain = "reddit.com",
            tags = listOf("android"),
            createdAt = Calendar.getInstance().apply { clear(); set(2026, Calendar.MAY, 1) }.timeInMillis
        )

        assertTrue(matches("source:reddit tag:android after:2026-01 backend", target))
        assertFalse(matches("source:reddit tag:android after:2026-01 frontend", target))
    }
}
