package com.tuck.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.tuck.app.data.local.db.dao.CollectionDao
import com.tuck.app.data.local.db.dao.EntityDao
import com.tuck.app.data.local.db.dao.SavedItemDao
import com.tuck.app.data.local.db.dao.SavedItemFtsDao
import com.tuck.app.data.local.db.dao.SearchHistoryDao
import com.tuck.app.data.local.db.dao.TagDao
import com.tuck.app.data.local.db.entity.CollectionEntity
import com.tuck.app.data.local.db.entity.EntityEntity
import com.tuck.app.data.local.db.entity.SavedItemCollectionCrossRef
import com.tuck.app.data.local.db.entity.SavedItemEntity
import com.tuck.app.data.local.db.entity.SavedItemFtsEntity
import com.tuck.app.data.local.db.entity.SavedItemTagCrossRef
import com.tuck.app.data.local.db.entity.SearchHistoryEntity
import com.tuck.app.data.local.db.entity.TagEntity

@Database(
    entities = [
        SavedItemEntity::class,
        SavedItemFtsEntity::class,
        EntityEntity::class,
        TagEntity::class,
        SavedItemTagCrossRef::class,
        CollectionEntity::class,
        SavedItemCollectionCrossRef::class,
        SearchHistoryEntity::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class TuckDatabase : RoomDatabase() {
    abstract fun savedItemDao(): SavedItemDao
    abstract fun savedItemFtsDao(): SavedItemFtsDao
    abstract fun entityDao(): EntityDao
    abstract fun tagDao(): TagDao
    abstract fun collectionDao(): CollectionDao
    abstract fun searchHistoryDao(): SearchHistoryDao

    companion object {
        const val DATABASE_NAME = "tuck_database.db"

        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE saved_items ADD COLUMN commentsJson TEXT DEFAULT NULL")
            }
        }

        val DEFAULT_SMART_COLLECTIONS = listOf(
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
