package com.tuck.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tuck.app.processing.extractors.ExtractedComment
import com.tuck.app.processing.legacyCommentsJson
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs on device because `org.json` is stubbed on the JVM unit-test classpath -
 * `JSONArray.toString()` returns null there, so this cannot be asserted off-device.
 */
@RunWith(AndroidJUnit4::class)
class LegacyCommentsJsonTest {

    @Test
    fun legacyJsonKeepsTopLevelCommentsForPreV3Fallback() {
        val comments = listOf(
            ExtractedComment(
                id = "c1", author = "u/alpha", bodyText = "Battery life is the biggest complaint",
                score = 42, depth = 0, path = "0001",
                replies = listOf(
                    ExtractedComment(
                        id = "c1a", parentId = "c1", author = "u/beta",
                        bodyText = "Agreed", score = 7, depth = 1, path = "0001.0001"
                    )
                )
            ),
            ExtractedComment(
                id = "c2", author = "u/delta", bodyText = "Model A is the pick",
                score = 19, depth = 0, path = "0002"
            )
        )

        val json = JSONArray(legacyCommentsJson(comments))

        assertEquals("only roots go into the legacy blob", 2, json.length())
        assertEquals("u/alpha", json.getJSONObject(0).getString("author"))
        assertEquals("Battery life is the biggest complaint", json.getJSONObject(0).getString("text"))
        assertEquals(42, json.getJSONObject(0).getInt("score"))
        assertEquals("u/delta", json.getJSONObject(1).getString("author"))
    }
}
