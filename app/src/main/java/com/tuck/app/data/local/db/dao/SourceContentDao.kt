package com.tuck.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tuck.app.data.local.db.entity.SourceCommentEntity
import com.tuck.app.data.local.db.entity.SourcePostEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SourceContentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: SourcePostEntity)

    @Query("SELECT * FROM source_posts WHERE itemId = :itemId")
    suspend fun getPost(itemId: Long): SourcePostEntity?

    @Query("SELECT * FROM source_posts WHERE itemId = :itemId")
    fun getPostFlow(itemId: Long): Flow<SourcePostEntity?>

    @Query("SELECT * FROM source_posts")
    suspend fun getAllPosts(): List<SourcePostEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComments(comments: List<SourceCommentEntity>): List<Long>

    @Query("SELECT * FROM source_comments WHERE itemId = :itemId ORDER BY path ASC, ordinal ASC")
    fun getCommentsTree(itemId: Long): Flow<List<SourceCommentEntity>>

    @Query("SELECT * FROM source_comments WHERE itemId = :itemId ORDER BY path ASC, ordinal ASC")
    suspend fun getCommentsTreeSync(itemId: Long): List<SourceCommentEntity>

    @Query("DELETE FROM source_comments WHERE itemId = :itemId")
    suspend fun deleteCommentsForItem(itemId: Long)

    @Query("DELETE FROM source_posts WHERE itemId = :itemId")
    suspend fun deletePostForItem(itemId: Long)
}
