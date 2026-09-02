package com.tuck.app.data.health

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.tuck.app.data.local.db.dao.SavedItemDao
import com.tuck.app.data.local.db.dao.SavedItemFtsDao
import com.tuck.app.data.local.storage.FileStorageService
import com.tuck.app.domain.health.HealthFinding
import com.tuck.app.domain.health.HealthReport
import com.tuck.app.domain.health.RepairResult
import com.tuck.app.processing.ItemProcessingWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Checks the vault for the failures that quietly destroy trust, and repairs them.
 *
 * The competing app's most praised feature was a "Sync Doctor" that explained problems in
 * plain English and fixed them - they turned their worst support burden into their best
 * received feature. Tuck has no sync yet, but the same failures exist locally: an item can
 * fall out of the search index and become invisible, enrichment can stall, a media file can
 * vanish, and orphaned files can quietly eat storage.
 *
 * Every finding says what is wrong in the user's terms, and every repair reports what it did.
 */
@Singleton
class VaultHealthChecker @Inject constructor(
    @ApplicationContext private val context: Context,
    private val savedItemDao: SavedItemDao,
    private val savedItemFtsDao: SavedItemFtsDao,
    private val fileStorageService: FileStorageService
) {

    companion object {
        const val UNSEARCHABLE = "unsearchable"
        const val STALLED = "stalled"
        const val MISSING_MEDIA = "missing_media"
        const val ORPHANED_FILES = "orphaned_files"
    }

    suspend fun check(): HealthReport = withContext(Dispatchers.IO) {
        val unsearchable = unsearchableItemIds()
        val stalled = savedItemDao.getStalledItems()
        val missingMedia = itemsWithMissingFiles()
        val orphans = orphanedFiles()
        val orphanBytes = orphans.sumOf { it.length() }

        HealthReport(
            checkedAt = System.currentTimeMillis(),
            findings = listOf(
                HealthFinding(
                    id = UNSEARCHABLE,
                    title = "Search index",
                    detail = if (unsearchable.isEmpty()) "Every save is searchable."
                    else "${unsearchable.size} ${saves(unsearchable.size)} cannot be found by " +
                        "search. They are safe - only the index is out of date.",
                    affectedCount = unsearchable.size,
                    severity = severity(unsearchable.size, HealthFinding.Severity.PROBLEM),
                    repairLabel = "Rebuild index".takeIf { unsearchable.isNotEmpty() }
                ),
                HealthFinding(
                    id = STALLED,
                    title = "Background processing",
                    detail = if (stalled.isEmpty()) "Nothing is stuck."
                    else "${stalled.size} ${saves(stalled.size)} never finished processing, so " +
                        "titles, previews or recognised text may be missing.",
                    affectedCount = stalled.size,
                    severity = severity(stalled.size, HealthFinding.Severity.ATTENTION),
                    repairLabel = "Try again".takeIf { stalled.isNotEmpty() }
                ),
                HealthFinding(
                    id = MISSING_MEDIA,
                    title = "Missing files",
                    detail = if (missingMedia.isEmpty()) "Every saved file is present."
                    else "${missingMedia.size} ${saves(missingMedia.size)} point to a file that is " +
                        "no longer on this device. The save itself is kept.",
                    affectedCount = missingMedia.size,
                    severity = severity(missingMedia.size, HealthFinding.Severity.PROBLEM),
                    repairLabel = "Clear broken links".takeIf { missingMedia.isNotEmpty() }
                ),
                HealthFinding(
                    id = ORPHANED_FILES,
                    title = "Unused storage",
                    detail = if (orphans.isEmpty()) "No wasted space."
                    else "${orphans.size} ${files(orphans.size)} on this device belong to no save, " +
                        "using ${formatBytes(orphanBytes)}.",
                    affectedCount = orphans.size,
                    severity = severity(orphans.size, HealthFinding.Severity.ATTENTION),
                    repairLabel = "Reclaim space".takeIf { orphans.isNotEmpty() },
                    reclaimableBytes = orphanBytes
                )
            )
        )
    }

    suspend fun repair(findingId: String): RepairResult = withContext(Dispatchers.IO) {
        when (findingId) {
            UNSEARCHABLE -> reindexAll()
            STALLED -> retryStalled()
            MISSING_MEDIA -> clearBrokenLinks()
            ORPHANED_FILES -> deleteOrphans()
            else -> RepairResult(summary = "Nothing to do.")
        }
    }

    suspend fun repairAll(report: HealthReport): RepairResult = withContext(Dispatchers.IO) {
        val results = report.findings.filterNot { it.isHealthy }.map { repair(it.id) }
        if (results.isEmpty()) return@withContext RepairResult(summary = "Nothing needed fixing.")
        RepairResult(
            summary = results.joinToString(" ") { it.summary },
            itemsReindexed = results.sumOf { it.itemsReindexed },
            enrichmentRetried = results.sumOf { it.enrichmentRetried },
            filesDeleted = results.sumOf { it.filesDeleted },
            bytesReclaimed = results.sumOf { it.bytesReclaimed },
            brokenLinksCleared = results.sumOf { it.brokenLinksCleared }
        )
    }

    private suspend fun unsearchableItemIds(): List<Long> {
        val indexed = savedItemFtsDao.allIndexedRowIds().toSet()
        return savedItemDao.getAllActiveItemsList().map { it.id }.filterNot { indexed.contains(it) }
    }

    private suspend fun itemsWithMissingFiles() = savedItemDao.getItemsWithFiles()
        .filter { item -> item.localFilePath?.let { !File(it).exists() } ?: false }

    private suspend fun reindexAll(): RepairResult {
        savedItemFtsDao.rebuildIndex()
        val count = savedItemDao.getAllActiveItemsList().size
        return RepairResult(
            summary = "Rebuilt the search index over $count ${saves(count)}.",
            itemsReindexed = count
        )
    }

    private suspend fun retryStalled(): RepairResult {
        val stalled = savedItemDao.getStalledItems()
        stalled.forEach { item ->
            WorkManager.getInstance(context).enqueueUniqueWork(
                "retry_enrich_${item.id}",
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<ItemProcessingWorker>()
                    .setInputData(Data.Builder().putLong(ItemProcessingWorker.KEY_ITEM_ID, item.id).build())
                    .build()
            )
        }
        return RepairResult(
            summary = "Queued ${stalled.size} ${saves(stalled.size)} to be processed again.",
            enrichmentRetried = stalled.size
        )
    }

    /**
     * Clears the path but keeps the save. The file is gone either way; the title, note, tags
     * and collections are not, and deleting someone's save because its thumbnail went missing
     * would be the worst possible response.
     */
    private suspend fun clearBrokenLinks(): RepairResult {
        val broken = itemsWithMissingFiles()
        broken.forEach { savedItemDao.update(it.copy(localFilePath = null, thumbnailPath = null)) }
        return RepairResult(
            summary = "Cleared ${broken.size} broken ${files(broken.size)} link, keeping the saves.",
            brokenLinksCleared = broken.size
        )
    }

    private suspend fun deleteOrphans(): RepairResult {
        val orphans = orphanedFiles()
        val bytes = orphans.sumOf { it.length() }
        val deleted = orphans.count { it.delete() }
        return RepairResult(
            summary = "Removed $deleted unused ${files(deleted)}, freeing ${formatBytes(bytes)}.",
            filesDeleted = deleted,
            bytesReclaimed = bytes
        )
    }

    /**
     * Files no live save references. Trashed items still count: trash is restorable, so
     * deleting their media would make restore a lie.
     */
    private suspend fun orphanedFiles(): List<File> {
        val referenced = buildSet {
            (savedItemDao.getAllActiveItemsList() + savedItemDao.getTrashedItemsList()).forEach {
                it.localFilePath?.let { path -> add(path) }
                it.thumbnailPath?.let { path -> add(path) }
            }
        }
        return fileStorageService.listStoredFiles().filterNot { referenced.contains(it.absolutePath) }
    }

    private fun severity(count: Int, whenBad: HealthFinding.Severity) =
        if (count == 0) HealthFinding.Severity.OK else whenBad

    private fun saves(count: Int) = if (count == 1) "save" else "saves"
    private fun files(count: Int) = if (count == 1) "file" else "files"

    private fun formatBytes(bytes: Long): String = when {
        bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
        bytes >= 1024 -> "%.0f KB".format(bytes / 1024.0)
        else -> "$bytes bytes"
    }
}
