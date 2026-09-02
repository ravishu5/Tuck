package com.tuck.app.domain.health

/** One thing the vault check looked at. */
data class HealthFinding(
    val id: String,
    val title: String,
    /** What was found, in plain language. Empty count means nothing to report. */
    val detail: String,
    val affectedCount: Int,
    val severity: Severity,
    /** Null when there is nothing to repair - some findings are informational. */
    val repairLabel: String? = null,
    /** Bytes recoverable, where the finding is about wasted space. */
    val reclaimableBytes: Long = 0L
) {
    val isHealthy: Boolean get() = affectedCount == 0

    enum class Severity { OK, ATTENTION, PROBLEM }
}

data class HealthReport(
    val findings: List<HealthFinding>,
    val checkedAt: Long
) {
    val isAllClear: Boolean get() = findings.all { it.isHealthy }
    val problemCount: Int get() = findings.count { !it.isHealthy }
    val totalReclaimableBytes: Long get() = findings.sumOf { it.reclaimableBytes }
}

/** What a repair actually did, so the user is told rather than left guessing. */
data class RepairResult(
    val summary: String,
    val itemsReindexed: Int = 0,
    val enrichmentRetried: Int = 0,
    val filesDeleted: Int = 0,
    val bytesReclaimed: Long = 0L,
    val brokenLinksCleared: Int = 0
)
