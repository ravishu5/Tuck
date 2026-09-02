package com.tuck.app.ui.health

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tuck.app.domain.health.HealthFinding
import com.tuck.app.ui.theme.TuckTheme

/**
 * Tells the user, in their own terms, whether their vault is intact - and fixes it.
 *
 * Trust in this category is lost through silent data problems, not missing features. A
 * check that finds nothing is as valuable as one that finds something, because it is the
 * only way an app can say "your things are fine" and mean it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultHealthScreen(
    onNavigateBack: () -> Unit,
    viewModel: VaultHealthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val tuckColors = TuckTheme.colors
    val report = uiState.report

    Scaffold(
        containerColor = tuckColors.canvas,
        topBar = {
            TopAppBar(
                title = { Text("Vault health") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.runCheck() }, enabled = !uiState.isChecking) {
                        Text("Check again")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = tuckColors.canvas)
            )
        }
    ) { padding ->
        if (uiState.isChecking && report == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = tuckColors.accent)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = if (report?.isAllClear == true) {
                        "Everything checks out. Your saves are complete and searchable."
                    } else {
                        "${report?.problemCount ?: 0} things need attention. Nothing here deletes " +
                            "a save - repairs only rebuild, retry or free unused space."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = tuckColors.textSecondary
                )
            }

            uiState.lastRepairSummary?.let { summary ->
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = tuckColors.accentContainer)) {
                        Text(
                            text = summary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = tuckColors.textPrimary,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            items(report?.findings.orEmpty(), key = { it.id }) { finding ->
                FindingCard(
                    finding = finding,
                    isBusy = uiState.isRepairing,
                    onRepair = { viewModel.repair(finding.id) }
                )
            }

            if (report != null && !report.isAllClear) {
                item {
                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick = { viewModel.repairAll() },
                        enabled = !uiState.isRepairing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = tuckColors.accent,
                            contentColor = tuckColors.textOnAccent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (uiState.isRepairing) "Repairing…" else "Repair everything")
                    }
                }
            }
        }
    }
}

@Composable
private fun FindingCard(
    finding: HealthFinding,
    isBusy: Boolean,
    onRepair: () -> Unit
) {
    val tuckColors = TuckTheme.colors
    val statusColor = when (finding.severity) {
        HealthFinding.Severity.OK -> tuckColors.success
        HealthFinding.Severity.ATTENTION -> tuckColors.warning
        HealthFinding.Severity.PROBLEM -> tuckColors.destructive
    }
    val statusIcon = when (finding.severity) {
        HealthFinding.Severity.OK -> Icons.Filled.CheckCircle
        HealthFinding.Severity.ATTENTION -> Icons.Filled.WarningAmber
        HealthFinding.Severity.PROBLEM -> Icons.Filled.ErrorOutline
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = tuckColors.surfaceCard)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(statusIcon, contentDescription = null, tint = statusColor, modifier = Modifier.size(19.dp))
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = finding.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = tuckColors.textPrimary
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = finding.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = tuckColors.textSecondary
                )

                finding.repairLabel?.let { label ->
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onRepair, enabled = !isBusy, contentPadding = PaddingValues(0.dp)) {
                        Text(label, color = tuckColors.accent, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
