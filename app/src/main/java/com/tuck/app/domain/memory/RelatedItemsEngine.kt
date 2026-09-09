package com.tuck.app.domain.memory

import com.tuck.app.domain.model.SavedItem
import kotlinx.coroutines.flow.Flow

/**
 * Surfaces saves the reader has not looked at lately.
 *
 * Item-to-item similarity used to live here too, behind a "Related saves in your vault" section
 * on the detail screen. It was removed rather than kept unused: scoring every save against every
 * other one is not free, and nothing was reading the answer.
 */
interface RelatedItemsEngine {
    fun getRediscoverItems(limit: Int = 4): Flow<List<SavedItem>>
    fun getForgottenSaves(limit: Int = 10): Flow<List<SavedItem>>
}
