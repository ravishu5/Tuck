package com.tuck.app.domain.repository

interface MaintenanceRepository {
    /**
     * Backfills PERSON entities from structured source metadata (source_posts and source_comments).
     *
     * Idempotent: Deletes existing "source-metadata" producer rows before re-inserting
     * so running multiple times produces identical state without duplicates.
     *
     * @return Number of items processed.
     */
    suspend fun backfillSourcePersonEntities(): Int
}
