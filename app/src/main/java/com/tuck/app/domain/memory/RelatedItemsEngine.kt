package com.tuck.app.domain.memory

import com.tuck.app.domain.model.SavedItem
import kotlinx.coroutines.flow.Flow

interface RelatedItemsEngine {
    fun findRelatedItems(itemId: Long, limit: Int = 5): Flow<List<SavedItem>>
    fun getRediscoverItems(limit: Int = 4): Flow<List<SavedItem>>
    fun getForgottenSaves(limit: Int = 10): Flow<List<SavedItem>>
}
