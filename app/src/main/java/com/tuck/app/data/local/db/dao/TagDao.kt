package com.tuck.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tuck.app.data.local.db.entity.SavedItemTagCrossRef
import com.tuck.app.data.local.db.entity.TagEntity

@Dao
interface TagDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: TagEntity): Long

    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    suspend fun getTagByName(name: String): TagEntity?

    @Query("""
        SELECT t.* FROM tags t
        INNER JOIN saved_item_tags st ON t.id = st.tagId
        WHERE st.savedItemId = :savedItemId
    """)
    suspend fun getTagsForSavedItem(savedItemId: Long): List<TagEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertItemTagCrossRef(crossRef: SavedItemTagCrossRef)

    @Query("DELETE FROM saved_item_tags WHERE savedItemId = :savedItemId")
    suspend fun clearTagsForSavedItem(savedItemId: Long)
}
