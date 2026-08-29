package com.tuck.app.data.local.db

import android.content.ContentValues
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.tuck.app.data.local.db.dao.CollectionDao
import com.tuck.app.data.local.db.dao.DerivedContentDao
import com.tuck.app.data.local.db.dao.EntityDao
import com.tuck.app.data.local.db.dao.ItemRawPayloadDao
import com.tuck.app.data.local.db.dao.MediaAssetDao
import com.tuck.app.data.local.db.dao.SavedItemDao
import com.tuck.app.data.local.db.dao.FilingRuleDao
import com.tuck.app.data.local.db.dao.SearchHistoryDao
import com.tuck.app.data.local.db.dao.SourceContentDao
import com.tuck.app.data.local.db.dao.TagDao
import com.tuck.app.data.local.db.entity.CollectionEntity
import com.tuck.app.data.local.db.entity.DerivedPointEntity
import com.tuck.app.data.local.db.entity.DerivedSummaryEntity
import com.tuck.app.data.local.db.entity.EntityEntity
import com.tuck.app.data.local.db.entity.ItemRawPayloadEntity
import com.tuck.app.data.local.db.entity.MediaAssetEntity
import com.tuck.app.data.local.db.entity.OcrBlockEntity
import com.tuck.app.data.local.db.entity.SavedItemCollectionCrossRef
import com.tuck.app.data.local.db.entity.SavedItemEntity
import com.tuck.app.data.local.db.entity.SavedItemTagCrossRef
import com.tuck.app.data.local.db.entity.FilingRuleEntity
import com.tuck.app.data.local.db.entity.SearchHistoryEntity
import com.tuck.app.data.local.db.entity.SourceCommentEntity
import com.tuck.app.data.local.db.entity.SourcePostEntity
import com.tuck.app.data.local.db.entity.TagEntity
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

@Database(
    entities = [
        SavedItemEntity::class,
        EntityEntity::class,
        TagEntity::class,
        SavedItemTagCrossRef::class,
        CollectionEntity::class,
        SavedItemCollectionCrossRef::class,
        SearchHistoryEntity::class,
        FilingRuleEntity::class,
        ItemRawPayloadEntity::class,
        MediaAssetEntity::class,
        SourcePostEntity::class,
        SourceCommentEntity::class,
        DerivedSummaryEntity::class,
        DerivedPointEntity::class,
        OcrBlockEntity::class
    ],
    version = 7,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class TuckDatabase : RoomDatabase() {
    abstract fun savedItemDao(): SavedItemDao
    abstract fun entityDao(): EntityDao
    abstract fun tagDao(): TagDao
    abstract fun collectionDao(): CollectionDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun filingRuleDao(): FilingRuleDao
    abstract fun mediaAssetDao(): MediaAssetDao
    abstract fun sourceContentDao(): SourceContentDao
    abstract fun derivedContentDao(): DerivedContentDao
    abstract fun itemRawPayloadDao(): ItemRawPayloadDao

    companion object {
        const val DATABASE_NAME = "tuck_database.db"

        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE saved_items ADD COLUMN commentsJson TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Alter saved_items table with Schema v3 columns
                db.execSQL("ALTER TABLE saved_items ADD COLUMN titleIsUserEdited INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE saved_items ADD COLUMN openCount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE saved_items ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE saved_items ADD COLUMN deletedAt INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE saved_items ADD COLUMN captureNote TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE saved_items ADD COLUMN userNote TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE saved_items ADD COLUMN dedupeGroupId TEXT DEFAULT NULL")

                db.execSQL("CREATE INDEX IF NOT EXISTS index_saved_items_isPinned ON saved_items(isPinned)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_saved_items_dedupeGroupId ON saved_items(dedupeGroupId)")

                // 2. Alter collections table
                db.execSQL("ALTER TABLE collections ADD COLUMN color TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE collections ADD COLUMN parentId INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE collections ADD COLUMN isSmart INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE collections ADD COLUMN smartQuery TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE collections ADD COLUMN sortOrdinal INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE collections ADD COLUMN isLocked INTEGER NOT NULL DEFAULT 0")

                db.execSQL("CREATE INDEX IF NOT EXISTS index_collections_parentId ON collections(parentId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_collections_sortOrdinal ON collections(sortOrdinal)")

                // 3. Alter saved_item_collections table
                db.execSQL("ALTER TABLE saved_item_collections ADD COLUMN addedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE saved_item_collections ADD COLUMN ordinal INTEGER NOT NULL DEFAULT 0")

                // 4. Alter entities table
                db.execSQL("ALTER TABLE entities ADD COLUMN charStart INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE entities ADD COLUMN charEnd INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE entities ADD COLUMN producer TEXT NOT NULL DEFAULT 'rule-based'")

                // 5. Create item_raw_payload table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `item_raw_payload` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `itemId` INTEGER NOT NULL,
                        `action` TEXT,
                        `mimeType` TEXT,
                        `text` TEXT,
                        `uris` TEXT,
                        `referrerPackage` TEXT,
                        `receivedAt` INTEGER NOT NULL,
                        FOREIGN KEY(`itemId`) REFERENCES `saved_items`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_item_raw_payload_itemId` ON `item_raw_payload`(`itemId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_item_raw_payload_receivedAt` ON `item_raw_payload`(`receivedAt`)")

                // 6. Create media_assets table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `media_assets` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `itemId` INTEGER NOT NULL,
                        `role` TEXT NOT NULL,
                        `localPath` TEXT NOT NULL,
                        `thumbnailPath` TEXT,
                        `mimeType` TEXT,
                        `bytes` INTEGER NOT NULL,
                        `width` INTEGER NOT NULL,
                        `height` INTEGER NOT NULL,
                        `durationMs` INTEGER NOT NULL,
                        `sha256` TEXT,
                        `ordinal` INTEGER NOT NULL,
                        `downloadState` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        FOREIGN KEY(`itemId`) REFERENCES `saved_items`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_media_assets_itemId` ON `media_assets`(`itemId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_media_assets_role` ON `media_assets`(`role`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_media_assets_sha256` ON `media_assets`(`sha256`)")

                // 7. Migrate legacy localFilePath / thumbnailPath to media_assets table
                db.execSQL(
                    """
                    INSERT INTO media_assets (itemId, role, localPath, thumbnailPath, mimeType, bytes, width, height, durationMs, sha256, ordinal, downloadState, createdAt)
                    SELECT id, 'PRIMARY', localFilePath, thumbnailPath, mimeType, 0, 0, 0, 0, imageSha256, 0, 'COMPLETE', createdAt
                    FROM saved_items
                    WHERE localFilePath IS NOT NULL AND localFilePath != ''
                    """.trimIndent()
                )

                // 8. Create source_posts table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `source_posts` (
                        `itemId` INTEGER PRIMARY KEY NOT NULL,
                        `platform` TEXT NOT NULL,
                        `platformPostId` TEXT,
                        `community` TEXT,
                        `authorHandle` TEXT,
                        `authorDisplay` TEXT,
                        `title` TEXT,
                        `bodyText` TEXT,
                        `bodyHtml` TEXT,
                        `score` INTEGER NOT NULL,
                        `commentCount` INTEGER NOT NULL,
                        `postedAt` INTEGER,
                        `permalink` TEXT,
                        `isNsfw` INTEGER NOT NULL,
                        `rawJson` TEXT,
                        `extractorVersion` TEXT,
                        `fetchedAt` INTEGER NOT NULL,
                        FOREIGN KEY(`itemId`) REFERENCES `saved_items`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_source_posts_platform` ON `source_posts`(`platform`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_source_posts_community` ON `source_posts`(`community`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_source_posts_platformPostId` ON `source_posts`(`platformPostId`)")

                // 9. Create source_comments table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `source_comments` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `itemId` INTEGER NOT NULL,
                        `platformCommentId` TEXT,
                        `parentCommentId` TEXT,
                        `depth` INTEGER NOT NULL,
                        `path` TEXT NOT NULL,
                        `authorHandle` TEXT,
                        `bodyText` TEXT NOT NULL,
                        `bodyHtml` TEXT,
                        `score` INTEGER NOT NULL,
                        `postedAt` INTEGER,
                        `isOp` INTEGER NOT NULL,
                        `isStickied` INTEGER NOT NULL,
                        `childCount` INTEGER NOT NULL,
                        `ordinal` INTEGER NOT NULL,
                        FOREIGN KEY(`itemId`) REFERENCES `saved_items`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_source_comments_itemId` ON `source_comments`(`itemId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_source_comments_parentCommentId` ON `source_comments`(`parentCommentId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_source_comments_path` ON `source_comments`(`path`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_source_comments_score` ON `source_comments`(`score`)")

                // 10. Migrate legacy commentsJson blobs into source_posts and source_comments
                migrateCommentsData(db)

                // 11. Create derived_summaries table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `derived_summaries` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `itemId` INTEGER NOT NULL,
                        `kind` TEXT NOT NULL,
                        `text` TEXT NOT NULL,
                        `producer` TEXT NOT NULL,
                        `modelVersion` TEXT,
                        `producedAt` INTEGER NOT NULL,
                        `confidence` REAL NOT NULL,
                        FOREIGN KEY(`itemId`) REFERENCES `saved_items`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_derived_summaries_itemId` ON `derived_summaries`(`itemId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_derived_summaries_kind` ON `derived_summaries`(`kind`)")

                // 12. Create derived_points table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `derived_points` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `itemId` INTEGER NOT NULL,
                        `kind` TEXT NOT NULL,
                        `text` TEXT NOT NULL,
                        `ordinal` INTEGER NOT NULL,
                        `evidenceRef` TEXT,
                        `producer` TEXT NOT NULL,
                        `producedAt` INTEGER NOT NULL,
                        FOREIGN KEY(`itemId`) REFERENCES `saved_items`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_derived_points_itemId` ON `derived_points`(`itemId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_derived_points_kind` ON `derived_points`(`kind`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_derived_points_ordinal` ON `derived_points`(`ordinal`)")

                // 13. Create ocr_blocks table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `ocr_blocks` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `assetId` INTEGER,
                        `itemId` INTEGER NOT NULL,
                        `text` TEXT NOT NULL,
                        `confidence` REAL NOT NULL,
                        `bboxX` REAL NOT NULL,
                        `bboxY` REAL NOT NULL,
                        `bboxW` REAL NOT NULL,
                        `bboxH` REAL NOT NULL,
                        `blockIndex` INTEGER NOT NULL,
                        `producer` TEXT NOT NULL,
                        FOREIGN KEY(`itemId`) REFERENCES `saved_items`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_ocr_blocks_itemId` ON `ocr_blocks`(`itemId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_ocr_blocks_assetId` ON `ocr_blocks`(`assetId`)")
            }
        }

        /**
         * Rebuilds the full-text index as a directly-owned FTS4 table.
         *
         * Android's SQLite has no fts5 module, so BM25 is not available; ranking is
         * computed from `matchinfo` instead, which needs the table outside Room's
         * `@Fts4` management. Also adds the porter tokenizer and prefix indexes.
         *
         * The index is derived data rebuilt from source, so dropping it loses nothing.
         */
        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TRIGGER IF EXISTS saved_items_fts_delete")
                db.execSQL("DROP TABLE IF EXISTS saved_items_fts")
                db.execSQL(com.tuck.app.data.local.db.dao.SavedItemFtsDaoImpl.CREATE_TABLE)
                db.execSQL(com.tuck.app.data.local.db.dao.SavedItemFtsDaoImpl.CREATE_DELETE_TRIGGER)
                db.execSQL(com.tuck.app.data.local.db.dao.backfillSql())
            }
        }

        /**
         * Adds `capturedAt`: when the content was originally created, distinct from when
         * Tuck saved it. A photo taken in 2019 and shared today previously read as if it
         * were from today, which is the reason competitors' users refused to delete their
         * originals. Nullable, and backfilled from EXIF by "regenerate derived data".
         */
        val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // No DEFAULT clause: a nullable column is implicitly NULL, and an explicit
                // "DEFAULT NULL" records a default of 'NULL' where Room expects 'undefined',
                // which fails schema validation on the next open.
                db.execSQL("ALTER TABLE saved_items ADD COLUMN capturedAt INTEGER")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_saved_items_capturedAt ON saved_items(capturedAt)")
            }
        }

        /**
         * Adds the item lifecycle: `remindAt` and `completedAt`.
         *
         * Until now a saved item had no notion of intent or completion, so everything
         * became an undifferentiated pile - the single most repeated complaint about
         * apps in this category. Both columns are nullable and carry no DEFAULT clause,
         * since an explicit "DEFAULT NULL" records a default Room does not expect.
         */
        val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE saved_items ADD COLUMN remindAt INTEGER")
                db.execSQL("ALTER TABLE saved_items ADD COLUMN completedAt INTEGER")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_saved_items_remindAt ON saved_items(remindAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_saved_items_completedAt ON saved_items(completedAt)")
            }
        }

        /**
         * Adds user-defined auto-filing rules.
         *
         * No DEFAULT clauses: the table is created empty, so NOT NULL columns need no
         * backfill, and an explicit default would record one Room does not expect.
         */
        val MIGRATION_6_7 = object : androidx.room.migration.Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `filing_rules` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `query` TEXT NOT NULL,
                        `collectionId` INTEGER NOT NULL,
                        `isEnabled` INTEGER NOT NULL,
                        `sortOrdinal` INTEGER NOT NULL,
                        `matchCount` INTEGER NOT NULL,
                        `lastMatchedAt` INTEGER,
                        `createdAt` INTEGER NOT NULL,
                        FOREIGN KEY(`collectionId`) REFERENCES `collections`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_filing_rules_collectionId` ON `filing_rules`(`collectionId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_filing_rules_isEnabled` ON `filing_rules`(`isEnabled`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_filing_rules_sortOrdinal` ON `filing_rules`(`sortOrdinal`)")
            }
        }

        private fun migrateCommentsData(db: SupportSQLiteDatabase) {
            try {
                val cursor = db.query(
                    "SELECT id, title, sourceDomain, commentsJson, createdAt FROM saved_items WHERE commentsJson IS NOT NULL AND commentsJson != ''"
                )
                cursor.use { c ->
                    val idIdx = c.getColumnIndexOrThrow("id")
                    val titleIdx = c.getColumnIndexOrThrow("title")
                    val domainIdx = c.getColumnIndexOrThrow("sourceDomain")
                    val jsonIdx = c.getColumnIndexOrThrow("commentsJson")
                    val createdIdx = c.getColumnIndexOrThrow("createdAt")

                    while (c.moveToNext()) {
                        val itemId = c.getLong(idIdx)
                        val title = c.getString(titleIdx)
                        val domain = c.getString(domainIdx) ?: ""
                        val rawJson = c.getString(jsonIdx)
                        val createdAt = c.getLong(createdIdx)

                        val platform = when {
                            domain.contains("reddit", ignoreCase = true) -> "REDDIT"
                            domain.contains("youtube", ignoreCase = true) || domain.contains("youtu.be", ignoreCase = true) -> "YOUTUBE"
                            domain.contains("twitter", ignoreCase = true) || domain.contains("x.com", ignoreCase = true) -> "TWITTER"
                            domain.contains("instagram", ignoreCase = true) -> "INSTAGRAM"
                            else -> "WEB"
                        }

                        val postArgs = arrayOf<Any?>(itemId, platform, title, rawJson, createdAt)
                        db.execSQL(
                            "INSERT OR REPLACE INTO source_posts (itemId, platform, title, rawJson, score, commentCount, isNsfw, fetchedAt) VALUES (?, ?, ?, ?, 0, 0, 0, ?)",
                            postArgs
                        )

                        parseAndInsertComments(db, itemId, rawJson, createdAt)
                    }
                }
            } catch (ignored: Exception) {
                // Defensive safeguard to guarantee migration never crashes on edge cases
            }
        }

        private fun parseAndInsertComments(
            db: SupportSQLiteDatabase,
            itemId: Long,
            jsonStr: String,
            createdAt: Long
        ) {
            try {
                val jsonElement = Json.parseToJsonElement(jsonStr)
                if (jsonElement is JsonArray) {
                    var ordinal = 0
                    for (elem in jsonElement) {
                        if (elem is JsonObject) {
                            insertCommentRecursive(
                                db = db,
                                itemId = itemId,
                                obj = elem,
                                parentPath = null,
                                depth = 0,
                                index = ++ordinal,
                                createdAt = createdAt
                            )
                        }
                    }
                } else if (jsonElement is JsonObject) {
                    insertCommentRecursive(
                        db = db,
                        itemId = itemId,
                        obj = jsonElement,
                        parentPath = null,
                        depth = 0,
                        index = 1,
                        createdAt = createdAt
                    )
                }

                // Update total comment count on source_post
                val countCursor = db.query("SELECT COUNT(*) FROM source_comments WHERE itemId = $itemId")
                var totalCount = 0
                countCursor.use { cc ->
                    if (cc.moveToNext()) {
                        totalCount = cc.getInt(0)
                    }
                }
                db.execSQL("UPDATE source_posts SET commentCount = ? WHERE itemId = ?", arrayOf<Any?>(totalCount, itemId))
            } catch (ignored: Exception) {
                // Fallback: rawJson is still preserved in source_posts table
            }
        }

        private fun insertCommentRecursive(
            db: SupportSQLiteDatabase,
            itemId: Long,
            obj: JsonObject,
            parentPath: String?,
            depth: Int,
            index: Int,
            createdAt: Long
        ) {
            val pathSegment = "%04d".format(index)
            val fullPath = if (parentPath == null) pathSegment else "$parentPath.$pathSegment"

            val author = obj["author"]?.toString()?.trim('"')
                ?: obj["authorHandle"]?.toString()?.trim('"')
                ?: "anonymous"

            val body = obj["body"]?.toString()?.trim('"')
                ?: obj["text"]?.toString()?.trim('"')
                ?: obj["bodyText"]?.toString()?.trim('"')
                ?: ""

            val score = obj["score"]?.toString()?.toIntOrNull()
                ?: obj["ups"]?.toString()?.toIntOrNull()
                ?: 0

            val platformCommentId = obj["id"]?.toString()?.trim('"')
                ?: obj["platformCommentId"]?.toString()?.trim('"')

            val commentArgs = arrayOf<Any?>(
                itemId,
                platformCommentId,
                depth,
                fullPath,
                author,
                body,
                score,
                createdAt,
                index
            )
            db.execSQL(
                "INSERT OR REPLACE INTO source_comments (itemId, platformCommentId, depth, path, authorHandle, bodyText, score, postedAt, isOp, isStickied, childCount, ordinal) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0, 0, 0, ?)",
                commentArgs
            )

            val replies = obj["replies"] ?: obj["children"]
            if (replies is JsonArray) {
                var childIndex = 0
                for (reply in replies) {
                    if (reply is JsonObject) {
                        insertCommentRecursive(
                            db = db,
                            itemId = itemId,
                            obj = reply,
                            parentPath = fullPath,
                            depth = depth + 1,
                            index = ++childIndex,
                            createdAt = createdAt
                        )
                    }
                }
            }
        }

        val DEFAULT_SMART_COLLECTIONS = listOf(
            "LinkedIn" to "work",
            "Instagram" to "photo_camera",
            "Reddit" to "forum",
            "YouTube" to "smart_display",
            "Twitter / X" to "tag",
            "GitHub" to "code",
            "Articles" to "article",
            "Programming" to "code",
            "Research" to "school",
            "Shopping" to "shopping_cart",
            "Travel" to "flight",
            "Food & Dining" to "restaurant",
            "Finance" to "attach_money",
            "Work" to "work",
            "Education" to "menu_book",
            "Personal" to "person",
            "Videos" to "videocam",
            "Images" to "image",
            "PDFs" to "picture_as_pdf"
        )
    }
}
