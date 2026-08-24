package com.tuck.app

import android.database.Cursor
import androidx.sqlite.db.SupportSQLiteDatabase
import com.tuck.app.data.local.db.TuckDatabase
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomMigrationTest {

    @Test
    fun testMigration2To3ExecutesAllTableCreationsAndAlters() {
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)
        val executedSql = mutableListOf<String>()

        every { db.execSQL(any<String>()) } answers {
            val sql = firstArg<String>()
            executedSql.add(sql.trim())
            Unit
        }

        val emptyCursor = mockk<Cursor>(relaxed = true)
        every { emptyCursor.moveToNext() } returns false
        every { db.query(any<String>()) } returns emptyCursor

        // Run migration
        TuckDatabase.MIGRATION_2_3.migrate(db)

        // Verify key Schema v3 operations
        assertTrue(executedSql.any { it.contains("ALTER TABLE saved_items ADD COLUMN isPinned") })
        assertTrue(executedSql.any { it.contains("ALTER TABLE saved_items ADD COLUMN dedupeGroupId") })
        assertTrue(executedSql.any { it.contains("ALTER TABLE collections ADD COLUMN parentId") })
        assertTrue(executedSql.any { it.contains("ALTER TABLE collections ADD COLUMN isSmart") })
        assertTrue(executedSql.any { it.contains("ALTER TABLE entities ADD COLUMN producer") })

        assertTrue(executedSql.any { it.contains("CREATE TABLE IF NOT EXISTS `item_raw_payload`") })
        assertTrue(executedSql.any { it.contains("CREATE TABLE IF NOT EXISTS `media_assets`") })
        assertTrue(executedSql.any { it.contains("CREATE TABLE IF NOT EXISTS `source_posts`") })
        assertTrue(executedSql.any { it.contains("CREATE TABLE IF NOT EXISTS `source_comments`") })
        assertTrue(executedSql.any { it.contains("CREATE TABLE IF NOT EXISTS `derived_summaries`") })
        assertTrue(executedSql.any { it.contains("CREATE TABLE IF NOT EXISTS `derived_points`") })
        assertTrue(executedSql.any { it.contains("CREATE TABLE IF NOT EXISTS `ocr_blocks`") })

        // Verify media_assets populated from legacy localFilePath
        assertTrue(executedSql.any { it.contains("INSERT INTO media_assets") && it.contains("SELECT id, 'PRIMARY', localFilePath") })
    }

    @Test
    fun testMigration2To3ParsesCommentsJsonIntoSourcePostsAndMaterializedPathComments() {
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)
        val insertedPosts = mutableListOf<Array<out Any?>>()
        val insertedComments = mutableListOf<Array<out Any?>>()

        val cursor = mockk<Cursor>(relaxed = true)
        every { cursor.getColumnIndexOrThrow("id") } returns 0
        every { cursor.getColumnIndexOrThrow("title") } returns 1
        every { cursor.getColumnIndexOrThrow("sourceDomain") } returns 2
        every { cursor.getColumnIndexOrThrow("commentsJson") } returns 3
        every { cursor.getColumnIndexOrThrow("createdAt") } returns 4

        val jsonPayload = """
            [
                {
                    "id": "c1",
                    "author": "alice",
                    "body": "First top comment",
                    "score": 42,
                    "replies": [
                        {
                            "id": "c1_1",
                            "author": "bob",
                            "body": "Nested reply to first comment",
                            "score": 10
                        }
                    ]
                },
                {
                    "id": "c2",
                    "author": "charlie",
                    "body": "Second top comment",
                    "score": 15
                }
            ]
        """.trimIndent()

        var cursorRead = false
        every { cursor.moveToNext() } answers {
            if (!cursorRead) {
                cursorRead = true
                true
            } else {
                false
            }
        }
        every { cursor.getLong(0) } returns 101L
        every { cursor.getString(1) } returns "Reddit Discussion about GNNs"
        every { cursor.getString(2) } returns "reddit.com"
        every { cursor.getString(3) } returns jsonPayload
        every { cursor.getLong(4) } returns 1620000000000L

        every { db.query(match<String> { it.contains("FROM saved_items WHERE commentsJson IS NOT NULL") }) } returns cursor

        val countCursor = mockk<Cursor>(relaxed = true)
        every { countCursor.moveToNext() } returns true
        every { countCursor.getInt(0) } returns 3
        every { db.query(match<String> { it.contains("SELECT COUNT(*) FROM source_comments") }) } returns countCursor

        every { db.execSQL(match<String> { it.contains("INSERT OR REPLACE INTO source_posts") }, any()) } answers {
            val args = secondArg<Array<out Any?>>()
            insertedPosts.add(args)
            Unit
        }

        every { db.execSQL(match<String> { it.contains("INSERT OR REPLACE INTO source_comments") }, any()) } answers {
            val args = secondArg<Array<out Any?>>()
            insertedComments.add(args)
            Unit
        }

        // Run migration
        TuckDatabase.MIGRATION_2_3.migrate(db)

        // Verify source_post was created
        assertEquals(1, insertedPosts.size)
        assertEquals(101L, insertedPosts[0][0])
        assertEquals("REDDIT", insertedPosts[0][1])
        assertEquals("Reddit Discussion about GNNs", insertedPosts[0][2])

        // Verify comments were converted with materialized paths
        assertEquals(3, insertedComments.size)

        // 1. First top level comment -> path = "0001", depth = 0
        assertEquals(101L, insertedComments[0][0]) // itemId
        assertEquals(0, insertedComments[0][2])    // depth
        assertEquals("0001", insertedComments[0][3]) // path
        assertEquals("alice", insertedComments[0][4]) // authorHandle
        assertEquals("First top comment", insertedComments[0][5]) // bodyText

        // 2. Child reply -> path = "0001.0001", depth = 1
        assertEquals(101L, insertedComments[1][0])
        assertEquals(1, insertedComments[1][2]) // depth
        assertEquals("0001.0001", insertedComments[1][3]) // path
        assertEquals("bob", insertedComments[1][4])
        assertEquals("Nested reply to first comment", insertedComments[1][5])

        // 3. Second top level comment -> path = "0002", depth = 0
        assertEquals(101L, insertedComments[2][0])
        assertEquals(0, insertedComments[2][2]) // depth
        assertEquals("0002", insertedComments[2][3]) // path
        assertEquals("charlie", insertedComments[2][4])
        assertEquals("Second top comment", insertedComments[2][5])
    }
}
