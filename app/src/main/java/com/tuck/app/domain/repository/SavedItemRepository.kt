package com.tuck.app.domain.repository

import com.tuck.app.domain.model.ContentType
import com.tuck.app.domain.model.ProcessingStatus
import com.tuck.app.domain.model.SavedItem
import kotlinx.coroutines.flow.Flow

interface SavedItemRepository {
    fun getAllActiveItems(): Flow<List<SavedItem>>
    fun getRecentItems(limit: Int = 20): Flow<List<SavedItem>>
    fun getFavoriteItems(): Flow<List<SavedItem>>
    fun getArchivedItems(): Flow<List<SavedItem>>
    fun getTrashedItems(): Flow<List<SavedItem>>
    fun getItemsByType(contentType: ContentType): Flow<List<SavedItem>>
    fun getItemsByCollection(collectionId: Long): Flow<List<SavedItem>>
    fun getItemByIdFlow(id: Long): Flow<SavedItem?>

    suspend fun getItemById(id: Long): SavedItem?
    suspend fun insertItem(item: SavedItem): Long
    suspend fun updateItem(item: SavedItem)
    suspend fun updateProcessingStatus(id: Long, status: ProcessingStatus, ocrText: String? = null, extractedText: String? = null, title: String? = null, description: String? = null)
    suspend fun setFavorite(id: Long, isFavorite: Boolean)
    suspend fun setArchived(id: Long, isArchived: Boolean)
    suspend fun moveToTrash(id: Long)
    suspend fun restoreFromTrash(id: Long)
    suspend fun permanentlyDelete(id: Long)
    suspend fun emptyTrash()
    suspend fun markOpened(id: Long)

    /** Sets or clears a reminder, keeping the scheduled work in step with the stored value. */
    suspend fun setReminder(id: Long, remindAt: Long?)
    /** Marks an item acted-on, which also retires any pending reminder. */
    suspend fun setCompleted(id: Long, completed: Boolean)

    suspend fun findByCanonicalUrl(canonicalUrl: String): SavedItem?
    suspend fun findByTextHash(hash: String): SavedItem?
    suspend fun findByImageHash(sha256: String): SavedItem?

    suspend fun addItemToCollection(itemId: Long, collectionId: Long)
    suspend fun removeItemFromCollection(itemId: Long, collectionId: Long)
    suspend fun setItemCollections(itemId: Long, collectionIds: List<Long>)
}
