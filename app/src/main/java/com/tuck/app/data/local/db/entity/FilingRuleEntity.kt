package com.tuck.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A user-defined auto-filing rule: "anything matching this query goes in that collection".
 *
 * The condition is a query-DSL string - the same syntax the search box already accepts -
 * so there is no separate rule language to learn or maintain.
 */
@Entity(
    tableName = "filing_rules",
    foreignKeys = [
        ForeignKey(
            entity = CollectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["collectionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["collectionId"]),
        Index(value = ["isEnabled"]),
        Index(value = ["sortOrdinal"])
    ]
)
data class FilingRuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** Query-DSL expression, e.g. `source:reddit type:pdf`. */
    val query: String,
    val collectionId: Long,
    val isEnabled: Boolean = true,
    val sortOrdinal: Int = 0,
    /** How many items this rule has filed, so a rule that never fires is visible as such. */
    val matchCount: Int = 0,
    val lastMatchedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
