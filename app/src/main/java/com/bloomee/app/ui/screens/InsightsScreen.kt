package com.bloomee.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.bloomee.app.domain.model.Symptom
import com.bloomee.app.domain.prediction.CyclePredictor
import com.bloomee.app.ui.BloomeeUiState
import com.bloomee.app.ui.components.BloomeeCard
import com.bloomee.app.ui.components.LabeledValue
import com.bloomee.app.ui.components.SectionTitle
import java.time.temporal.ChronoUnit

private const val MHRS_URL = "https://www.mhrs.gov.tr"

@Composable
fun InsightsScreen(
    state: BloomeeUiState,
    onShareReport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val periods = remember(state.logs) { CyclePredictor.detectPeriods(state.logs) }
    val cycleLengths = periods.zipWithNext { current, next ->
        ChronoUnit.DAYS.between(current.first(), next.first()).toInt()
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            BloomeeCard {
                SectionTitle("Özet")
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                    LabeledValue("Kayıtlı döngü", "${state.stats.recordedCycles}")
                    LabeledValue("Ortalama", "${state.stats.averageCycleLength} gün")
                    LabeledValue("Sapma", "±${state.stats.cycleLengthVariation.toInt()} gün")
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    if (state.stats.hasEnoughData) {
                        "Tahminler son ${state.stats.recordedCycles} döngüne dayanıyor; yeni döngüler daha ağırlıklı sayılır."
                    } else {
                        "En az iki tamamlanmış döngü kaydettiğinde tahmin doğruluğu belirgin şekilde artar."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item { SectionTitle("Döngü uzunlukları") }
        item {
            BloomeeCard {
                if (cycleLengths.isEmpty()) {
                    Text("Henüz karşılaştırılacak döngü yok.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    val maxLength = cycleLengths.max().coerceAtLeast(1)
                    Row(
                        modifier = Modifier.fillMaxWidth().height(140.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        cycleLengths.takeLast(8).forEach { length ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$length", style = MaterialTheme.typography.labelMedium)
                                Spacer(Modifier.height(4.dp))
                                Box(
                                    Modifier
                                        .size(
                                            width = 24.dp,
                                            height = (16 + 90 * (length.toFloat() / maxLength)).dp
                                        )
                                        .clip(MaterialTheme.shapes.small)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            }
                        }
                    }
                }
            }
        }

        item { SectionTitle("En sık belirtiler") }
        item {
            val counts = state.logs
                .flatMap { it.symptoms }
                .groupingBy { it }
                .eachCount()
                .entries
                .sortedByDescending { it.value }
                .take(5)

            BloomeeCard {
                if (counts.isEmpty()) {
                    Text("Belirti kaydı yok.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    counts.forEach { (symptom: Symptom, count: Int) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(symptom.label, style = MaterialTheme.typography.bodyMedium)
                            Text("$count gün", style = MaterialTheme.typography.bodyMedium)
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onShareReport, modifier = Modifier.weight(1f)) {
                    Text("Hekim için rapor paylaş")
                }
                Button(
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(MHRS_URL))
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Randevu al")
                }
            }
        }
    }
}
