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

/**
 * The rediscover rule, over a list the caller already has.
 *
 * A screen showing the vault has already loaded and mapped every item; asking the engine for
 * its own flow made the same query and the same mapping run a second time on every change.
 *
 * [seed] fixes the shuffle. Re-shuffling on each emission meant the section dealt itself new
 * cards every time anything in the vault was touched, which reads as the screen redrawing
 * itself for no reason.
 */
fun List<SavedItem>.rediscoverPicks(
    limit: Int,
    now: Long = System.currentTimeMillis(),
    seed: Long
): List<SavedItem> {
    val sevenDaysAgo = now - SEVEN_DAYS_MS
    return asSequence()
        .filter { it.createdAt < sevenDaysAgo && !it.isArchived }
        .sortedBy { (it.id * PRIME).xorSeed(seed) }
        .take(limit)
        .toList()
}

/** Deterministic per (item, seed), so the same vault and seed always deal the same hand. */
private fun Long.xorSeed(seed: Long): Long = this xor seed

private const val PRIME = 2_654_435_761L
private const val SEVEN_DAYS_MS = 7L * 24 * 60 * 60 * 1000
