package com.bloomee.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bloomee.app.domain.advice.AdviceCard
import com.bloomee.app.domain.advice.AdviceEngine
import com.bloomee.app.domain.model.CyclePhase
import com.bloomee.app.ui.BloomeeUiState
import com.bloomee.app.ui.components.BloomeeCard
import com.bloomee.app.ui.components.LabeledValue
import com.bloomee.app.ui.components.SectionTitle
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val dayFormatter = DateTimeFormatter.ofPattern("d MMMM", Locale("tr"))

@Composable
fun HomeScreen(
    state: BloomeeUiState,
    onLogToday: () -> Unit,
    onAddWater: (Int) -> Unit,
    onOpenAssistant: () -> Unit,
    onOpenNutrition: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { CycleHeroCard(state, onLogToday) }
        item { HydrationQuickCard(state, onAddWater) }
        item { NutritionQuickCard(state, onOpenNutrition) }

        if (state.profile.partnerModeEnabled) {
            item {
                BloomeeCard(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                    Text("Partner özeti", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        AdviceEngine.partnerSummary(
                            stats = state.stats,
                            partnerName = state.profile.partnerName,
                            symptoms = state.todayLog?.symptoms.orEmpty()
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        item {
            SectionTitle("Bugün senin için", trailing = {
                FilledTonalButton(onClick = onOpenAssistant) { Text("Asistan") }
            })
        }

        items(state.advice, key = { it.id }) { card -> AdviceCardView(card) }

        item {
            Text(
                text = AdviceEngine.MEDICAL_DISCLAIMER,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun CycleHeroCard(state: BloomeeUiState, onLogToday: () -> Unit) {
    val stats = state.stats
    val headline = when {
        stats.phase == CyclePhase.UNKNOWN -> "Takibe başlayalım"
        stats.isLate -> "Regl ${-(stats.daysToNextPeriod ?: 0)} gün gecikti"
        stats.daysToNextPeriod != null && stats.daysToNextPeriod!! < 0 ->
            "Yeni reglini kaydedince tahmin netleşir"
        stats.daysToNextPeriod == 0 -> "Bugün regl beklentisi"
        stats.daysToNextPeriod != null -> "Regle ${stats.daysToNextPeriod} gün"
        else -> stats.phase.label
    }

    BloomeeCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CycleRing(
                progress = stats.phaseProgress,
                centerTop = if (stats.cycleDay > 0) "${stats.cycleDay}." else "–",
                centerBottom = if (stats.cycleDay > 0) "gün" else "kayıt yok"
            )
            Spacer(Modifier.size(18.dp))
            Column(Modifier.weight(1f)) {
                Text(headline, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(4.dp))
                Text(
                    stats.phase.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                stats.predictedNextPeriodStart?.let {
                    val spread = stats.cycleLengthVariation.roundToInt()
                    val uncertainty = when {
                        !stats.hasEnoughData -> " · tek kayda dayalı"
                        spread >= 1 -> " ±$spread gün"
                        else -> ""
                    }
                    Text(
                        "Tahmini başlangıç: ${it.format(dayFormatter)}$uncertainty",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            LabeledValue("Ortalama döngü", "${stats.averageCycleLength} gün")
            LabeledValue("Regl süresi", "${stats.averagePeriodLength} gün")
            LabeledValue("Doğurganlık", stats.fertility.label)
        }

        val notes = buildList {
            if (stats.phase != CyclePhase.UNKNOWN && !stats.hasEnoughData) {
                add("Tahmin tek dönem kaydına dayanıyor; düzenli kayıtla hassaslaşır")
            }
            if (stats.isIrregular) {
                add("Döngün değişken, tahmin aralığı geniş")
            }
        }
        notes.forEach { note ->
            Spacer(Modifier.height(10.dp))
            AssistChip(onClick = {}, label = { Text(note) })
        }

        Spacer(Modifier.height(14.dp))
        Button(onClick = onLogToday, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text("Bugünü kaydet")
        }
    }
}

@Composable
private fun CycleRing(progress: Float, centerTop: String, centerBottom: String) {
    val ringColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surface

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(116.dp)) {
        androidx.compose.foundation.Canvas(Modifier.size(116.dp)) {
            val stroke = 12.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                brush = Brush.sweepGradient(listOf(ringColor, ringColor.copy(alpha = 0.45f), ringColor)),
                startAngle = -90f,
                sweepAngle = 360f * progress.coerceIn(0f, 1f),
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(centerTop, style = MaterialTheme.typography.headlineMedium)
            Text(centerBottom, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun HydrationQuickCard(state: BloomeeUiState, onAddWater: (Int) -> Unit) {
    val hydration = state.hydrationToday
    BloomeeCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.WaterDrop,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary
            )
            Spacer(Modifier.size(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Su takibi", style = MaterialTheme.typography.titleMedium)
                Text(
                    "${hydration.consumedMl} / ${hydration.goalMl} ml",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text("%${(hydration.progress * 100).toInt()}", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = { hydration.progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(6.dp))
                .semantics {
                    contentDescription =
                        "Su hedefinin yüzde ${(hydration.progress * 100).toInt()} kadarı tamam"
                },
            color = MaterialTheme.colorScheme.tertiary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(200, 330, 500).forEach { amount ->
                FilledTonalButton(onClick = { onAddWater(amount) }) { Text("+$amount ml") }
            }
        }
    }
}

@Composable
private fun NutritionQuickCard(state: BloomeeUiState, onOpenNutrition: () -> Unit) {
    val nutrition = state.nutritionToday
    BloomeeCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Restaurant,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
            )
            Spacer(Modifier.size(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Kalori takibi", style = MaterialTheme.typography.titleMedium)
                Text(
                    "${nutrition.consumedKcal} / ${nutrition.goalKcal} kcal · ${nutrition.entries.size} öğün",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text("%${(nutrition.progress * 100).toInt()}", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = { nutrition.progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(6.dp)),
            color = MaterialTheme.colorScheme.secondary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        FilledTonalButton(onClick = onOpenNutrition) { Text("Öğün ekle") }
    }
}

@Composable
private fun AdviceCardView(card: AdviceCard) {
    BloomeeCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
            Spacer(Modifier.size(8.dp))
            Text(
                card.category.label.uppercase(Locale("tr")),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(card.title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(card.body, style = MaterialTheme.typography.bodyMedium)
    }
}
