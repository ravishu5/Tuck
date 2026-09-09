package com.tuck.app.processing

import com.tuck.app.data.local.db.dao.CollectionDao
import com.tuck.app.data.local.db.dao.FilingRuleDao
import com.tuck.app.data.local.db.entity.SavedItemCollectionCrossRef
import com.tuck.app.domain.model.RuleMatcher
import com.tuck.app.domain.model.SavedItem
import com.tuck.app.domain.model.SearchQueryParser
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Applies the user's auto-filing rules to a freshly enriched item.
 *
 * Runs after enrichment rather than at save time: a rule like `source:reddit` needs the
 * domain, and `tag:android` needs the tags, neither of which exist when the share sheet
 * closes. Saving is never blocked on this.
 */
@Singleton
class FilingRuleEngine @Inject constructor(
    private val filingRuleDao: FilingRuleDao,
    private val collectionDao: CollectionDao
) {

    /**
     * Files [item] into every collection whose rule it matches.
     *
     * Rules are additive and independent - an item can satisfy several and land in all of
     * them, which is the point of collections being many-to-many. Returns the collection
     * ids it was filed into.
     */
    suspend fun apply(item: SavedItem): List<Long> {
        val rules = filingRuleDao.getEnabledRules()
        if (rules.isEmpty()) return emptyList()

        val filedInto = mutableListOf<Long>()

        for (rule in rules) {
            val parsed = try {
                SearchQueryParser.parse(rule.query)
            } catch (e: Exception) {
                // A malformed rule must never stop the others from running, and must
                // never fail the item's enrichment.
                continue
            }

            if (!RuleMatcher.matches(parsed, item)) continue

            collectionDao.insertItemCollectionCrossRef(
                SavedItemCollectionCrossRef(savedItemId = item.id, collectionId = rule.collectionId)
            )
            filingRuleDao.recordMatch(rule.id)
            filedInto.add(rule.collectionId)
        }

        return filedInto
    }
}
