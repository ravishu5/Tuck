package com.tuck.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tuck.app.data.local.db.entity.SavedItemEntity
import com.tuck.app.domain.model.ContentType
import com.tuck.app.domain.model.ProcessingStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SavedItemEntity): Long

    @Update
    suspend fun update(item: SavedItemEntity)

    @Query("SELECT * FROM saved_items WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: Long): SavedItemEntity?

    @Query("SELECT * FROM saved_items WHERE id = :id LIMIT 1")
    fun getItemByIdFlow(id: Long): Flow<SavedItemEntity?>

    @Query("SELECT * FROM saved_items WHERE isDeleted = 0 AND isArchived = 0 ORDER BY createdAt DESC")
    fun getAllActiveItems(): Flow<List<SavedItemEntity>>

    @Query("SELECT * FROM saved_items WHERE isDeleted = 0 ORDER BY createdAt DESC")
    suspend fun getAllActiveItemsList(): List<SavedItemEntity>

    @Query("SELECT * FROM saved_items WHERE isDeleted = 0 AND isArchived = 0 ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentItems(limit: Int): Flow<List<SavedItemEntity>>

    @Query("SELECT * FROM saved_items WHERE isDeleted = 0 AND isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavoriteItems(): Flow<List<SavedItemEntity>>

    @Query("SELECT * FROM saved_items WHERE isDeleted = 0 AND isArchived = 1 ORDER BY updatedAt DESC")
    fun getArchivedItems(): Flow<List<SavedItemEntity>>

    @Query("SELECT * FROM saved_items WHERE isDeleted = 1 ORDER BY updatedAt DESC")
    fun getTrashedItems(): Flow<List<SavedItemEntity>>

    @Query("SELECT * FROM saved_items WHERE isDeleted = 0 AND isArchived = 0 AND contentType = :contentType ORDER BY createdAt DESC")
    fun getItemsByType(contentType: ContentType): Flow<List<SavedItemEntity>>

    @Query("""
        SELECT s.* FROM saved_items s
        INNER JOIN saved_item_collections c ON s.id = c.savedItemId
        WHERE c.collectionId = :collectionId AND s.isDeleted = 0 AND s.isArchived = 0
        ORDER BY s.createdAt DESC
    """)
    fun getItemsByCollection(collectionId: Long): Flow<List<SavedItemEntity>>

    @Query("""
        SELECT s.* FROM saved_items s
        INNER JOIN saved_item_collections c ON s.id = c.savedItemId
        WHERE c.collectionId = :collectionId AND s.isDeleted = 0
        ORDER BY s.createdAt DESC
    """)
    suspend fun getItemsByCollectionList(collectionId: Long): List<SavedItemEntity>

    @Query("SELECT * FROM saved_items WHERE isDeleted = 0 AND processingStatus IN ('FAILED', 'PROCESSING')")
    suspend fun getStalledItems(): List<SavedItemEntity>

    @Query("SELECT * FROM saved_items WHERE isDeleted = 0 AND localFilePath IS NOT NULL AND localFilePath != ''")
    suspend fun getItemsWithFiles(): List<SavedItemEntity>

    @Query("UPDATE saved_items SET remindAt = :remindAt, updatedAt = :now WHERE id = :id")
    suspend fun setRemindAt(id: Long, remindAt: Long?, now: Long = System.currentTimeMillis())

    @Query("UPDATE saved_items SET completedAt = :completedAt, updatedAt = :now WHERE id = :id")
    suspend fun setCompletedAt(id: Long, completedAt: Long?, now: Long = System.currentTimeMillis())

    @Query("SELECT * FROM saved_items WHERE remindAt IS NOT NULL AND completedAt IS NULL AND isDeleted = 0 ORDER BY remindAt ASC")
    fun getItemsWithReminders(): kotlinx.coroutines.flow.Flow<List<com.tuck.app.data.local.db.entity.SavedItemEntity>>

    @Query("UPDATE saved_items SET isFavorite = :isFavorite, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE saved_items SET isArchived = :isArchived, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setArchived(id: Long, isArchived: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE saved_items SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun moveToTrash(id: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE saved_items SET isDeleted = 0, updatedAt = :updatedAt WHERE id = :id")
    suspend fun restoreFromTrash(id: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM saved_items WHERE id = :id")
    suspend fun permanentlyDelete(id: Long)

    @Query("DELETE FROM saved_items WHERE isDeleted = 1")
    suspend fun emptyTrash(): Int

    @Query("SELECT * FROM saved_items WHERE isDeleted = 1")
    suspend fun getTrashedItemsList(): List<SavedItemEntity>

    @Query("UPDATE saved_items SET lastOpenedAt = :openedAt WHERE id = :id")
    suspend fun updateLastOpened(id: Long, openedAt: Long = System.currentTimeMillis())

    @Query("UPDATE saved_items SET processingStatus = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: Long, status: ProcessingStatus, updatedAt: Long = System.currentTimeMillis())

    @Query("""
        UPDATE saved_items 
        SET processingStatus = :status,
            ocrText = COALESCE(:ocrText, ocrText),
            extractedText = COALESCE(:extractedText, extractedText),
            commentsJson = COALESCE(:commentsJson, commentsJson),
            title = CASE WHEN :title IS NOT NULL AND :title != '' THEN :title ELSE title END,
            description = COALESCE(:description, description),
            thumbnailPath = CASE WHEN :thumbnailPath IS NOT NULL AND :thumbnailPath != '' THEN :thumbnailPath ELSE thumbnailPath END,
            sourceDomain = CASE WHEN :sourceDomain IS NOT NULL AND :sourceDomain != '' THEN :sourceDomain ELSE sourceDomain END,
            canonicalUrl = CASE WHEN :canonicalUrl IS NOT NULL AND :canonicalUrl != '' THEN :canonicalUrl ELSE canonicalUrl END,
            contentType = CASE WHEN :contentType IS NOT NULL THEN :contentType ELSE contentType END,
            updatedAt = :updatedAt
        WHERE id = :id
    """)
    suspend fun updateProcessingResult(
        id: Long,
        status: ProcessingStatus,
        ocrText: String?,
        extractedText: String?,
        title: String?,
        description: String?,
        thumbnailPath: String? = null,
        sourceDomain: String? = null,
        canonicalUrl: String? = null,
        contentType: ContentType? = null,
        commentsJson: String? = null,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("SELECT * FROM saved_items WHERE canonicalUrl = :canonicalUrl AND isDeleted = 0 LIMIT 1")
    suspend fun findByCanonicalUrl(canonicalUrl: String): SavedItemEntity?

    @Query("SELECT * FROM saved_items WHERE textHash = :hash AND isDeleted = 0 LIMIT 1")
    suspend fun findByTextHash(hash: String): SavedItemEntity?

    @Query("SELECT * FROM saved_items WHERE imageSha256 = :sha256 AND isDeleted = 0 LIMIT 1")
    suspend fun findByImageHash(sha256: String): SavedItemEntity?

    @Query("SELECT * FROM saved_items WHERE id IN (:ids)")
    suspend fun getItemsByIds(ids: List<Long>): List<SavedItemEntity>

    @Query("SELECT * FROM saved_items WHERE processingStatus IN ('PENDING', 'PROCESSING') AND isDeleted = 0")
    suspend fun getPendingProcessingItems(): List<SavedItemEntity>

    @Query("""
        SELECT * FROM saved_items 
        WHERE isDeleted = 0 
          AND (
            title LIKE '%' || :query || '%' 
            OR description LIKE '%' || :query || '%' 
            OR originalText LIKE '%' || :query || '%' 
            OR extractedText LIKE '%' || :query || '%' 
            OR ocrText LIKE '%' || :query || '%' 
            OR sourceDomain LIKE '%' || :query || '%' 
            OR originalUrl LIKE '%' || :query || '%'
            OR commentsJson LIKE '%' || :query || '%'
          )
        ORDER BY createdAt DESC
    """)
    suspend fun searchItemsLike(query: String): List<SavedItemEntity>
}
