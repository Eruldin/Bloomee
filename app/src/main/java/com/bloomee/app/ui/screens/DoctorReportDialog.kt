package com.bloomee.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.bloomee.app.domain.report.DoctorReport
import com.bloomee.app.ui.BloomeeUiState
import java.time.LocalDate

/**
 * Lets the user pick a date range and which sections go into the doctor report,
 * and shows the exact text before it leaves the device.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DoctorReportDialog(
    state: BloomeeUiState,
    onShare: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var rangeMonths by remember { mutableStateOf(6) }
    var sections by remember { mutableStateOf(DoctorReport.Section.entries.toSet()) }

    val report = remember(state.logs, state.hydrationHistory, state.nutritionHistory, rangeMonths, sections) {
        DoctorReport.build(
            logs = state.logs,
            hydrationDays = state.hydrationHistory,
            nutrition = state.nutritionHistory,
            options = DoctorReport.Options(
                from = rangeMonths.takeIf { it > 0 }?.let { LocalDate.now().minusMonths(it.toLong()) },
                sections = sections
            )
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Hekim raporu") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "Aralık",
                    style = MaterialTheme.typography.labelLarge
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(3 to "Son 3 ay", 6 to "Son 6 ay", 12 to "Son 12 ay", 0 to "Tümü")
                        .forEach { (months, label) ->
                            FilterChip(
                                selected = rangeMonths == months,
                                onClick = { rangeMonths = months },
                                label = { Text(label) }
                            )
                        }
                }
                Spacer(Modifier.height(10.dp))
                Text("Dahil edilecek alanlar", style = MaterialTheme.typography.labelLarge)
                DoctorReport.Section.entries.forEach { section ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = section in sections,
                            onCheckedChange = { checked ->
                                sections = if (checked) sections + section else sections - section
                            }
                        )
                        Text(section.label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text("Önizleme", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(4.dp))
                Column(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.shapes.small
                        )
                        .verticalScroll(rememberScrollState())
                        .padding(10.dp)
                ) {
                    Text(
                        report,
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Yalnızca seçtiğin alanlar gönderilir; notların ve profil bilgilerin rapora girmez.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onShare(report) },
                enabled = sections.isNotEmpty()
            ) {
                Text("Paylaş")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Vazgeç") }
        }
    )
}
