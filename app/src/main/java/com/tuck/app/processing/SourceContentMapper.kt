package com.tuck.app.processing

import com.tuck.app.data.local.db.entity.SourceCommentEntity
import com.tuck.app.processing.extractors.ExtractedComment
import org.json.JSONArray
import org.json.JSONObject

/** Hard cap so a very large thread cannot bloat the database on a single save. */
const val MAX_PERSISTED_COMMENTS = 500

/**
 * Flattens a nested comment tree depth-first into rows, preserving the
 * materialized [ExtractedComment.path] so the tree can be rebuilt with a single
 * `ORDER BY path` query.
 */
fun flattenComments(
    comments: List<ExtractedComment>,
    itemId: Long,
    cap: Int = MAX_PERSISTED_COMMENTS
): List<SourceCommentEntity> {
    val rows = mutableListOf<SourceCommentEntity>()

    fun walk(comment: ExtractedComment) {
        if (rows.size >= cap) return
        rows.add(
            SourceCommentEntity(
                itemId = itemId,
                platformCommentId = comment.id,
                parentCommentId = comment.parentId,
                depth = comment.depth,
                path = comment.path,
                authorHandle = comment.author,
                bodyText = comment.bodyText,
                score = comment.score,
                postedAt = comment.postedAt,
                childCount = comment.replies.size,
                ordinal = rows.size + 1
            )
        )
        comment.replies.forEach { walk(it) }
    }

    comments.forEach { walk(it) }
    return rows
}

/**
 * Renders top-level comments into the legacy `commentsJson` shape, which the
 * detail screen still reads as a fallback for items saved before schema v3.
 */
fun legacyCommentsJson(comments: List<ExtractedComment>): String {
    val array = JSONArray()
    comments.forEach { comment ->
        array.put(
            JSONObject().apply {
                put("author", comment.author)
                put("text", comment.bodyText)
                put("score", comment.score)
                comment.postedAt?.let { put("timestamp", it) }
            }
        )
    }
    return array.toString()
}
