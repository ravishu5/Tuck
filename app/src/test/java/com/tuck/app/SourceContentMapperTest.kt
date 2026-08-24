package com.tuck.app

import com.tuck.app.processing.extractors.ExtractedComment
import com.tuck.app.processing.flattenComments
import com.tuck.app.processing.legacyCommentsJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceContentMapperTest {

    private fun tree() = listOf(
        ExtractedComment(
            id = "c1",
            author = "u/alpha",
            bodyText = "Battery life is the biggest complaint",
            score = 42,
            depth = 0,
            path = "0001",
            replies = listOf(
                ExtractedComment(
                    id = "c1a", parentId = "c1", author = "u/beta",
                    bodyText = "Agreed", score = 7, depth = 1, path = "0001.0001"
                ),
                ExtractedComment(
                    id = "c1b", parentId = "c1", author = "u/gamma",
                    bodyText = "Not on the newer revision", score = 3, depth = 1, path = "0001.0002"
                )
            )
        ),
        ExtractedComment(
            id = "c2", author = "u/delta", bodyText = "Model A is the pick",
            score = 19, depth = 0, path = "0002"
        )
    )

    @Test
    fun flattensDepthFirstAndPreservesMaterializedPaths() {
        val rows = flattenComments(tree(), itemId = 7L)

        assertEquals(4, rows.size)
        assertEquals(listOf("0001", "0001.0001", "0001.0002", "0002"), rows.map { it.path })
        assertEquals(listOf(0, 1, 1, 0), rows.map { it.depth })
        assertTrue("every row belongs to the item", rows.all { it.itemId == 7L })
    }

    @Test
    fun carriesIdentityScoreAndChildCount() {
        val rows = flattenComments(tree(), itemId = 7L)

        val root = rows.first()
        assertEquals("c1", root.platformCommentId)
        assertEquals("u/alpha", root.authorHandle)
        assertEquals(42, root.score)
        assertEquals("child count is recorded for collapse affordances", 2, root.childCount)

        val reply = rows[1]
        assertEquals("c1", reply.parentCommentId)
        assertEquals(0, reply.childCount)
    }

    @Test
    fun ordinalsAreSequentialAcrossTheFlattenedTree() {
        val rows = flattenComments(tree(), itemId = 7L)
        assertEquals(listOf(1, 2, 3, 4), rows.map { it.ordinal })
    }

    @Test
    fun capStopsRunawayThreadsFromBloatingTheDatabase() {
        val rows = flattenComments(tree(), itemId = 7L, cap = 2)
        assertEquals(2, rows.size)
        assertEquals(listOf("0001", "0001.0001"), rows.map { it.path })
    }
}
