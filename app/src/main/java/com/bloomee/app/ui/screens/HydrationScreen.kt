package com.bloomee.app.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.bloomee.app.domain.hydration.HydrationCalculator
import com.bloomee.app.ui.BloomeeUiState
import com.bloomee.app.ui.components.BloomeeCard
import com.bloomee.app.ui.components.LabeledValue
import com.bloomee.app.ui.components.SectionTitle
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val shortDate = DateTimeFormatter.ofPattern("d MMM", Locale("tr"))

@Composable
fun HydrationScreen(
    state: BloomeeUiState,
    onAddWater: (Int) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hydration = state.hydrationToday

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            BloomeeCard(containerColor = MaterialTheme.colorScheme.tertiaryContainer) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    WaterRing(hydration.progress)
                    Spacer(Modifier.size(18.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "${hydration.consumedMl} ml",
                            style = MaterialTheme.typography.displaySmall
                        )
                        Text(
                            "Hedef ${hydration.goalMl} ml",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "Hedef; kilon, hareket düzeyin ve döngü fazına göre hesaplandı.",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HydrationCalculator.quickAddOptionsMl.forEach { amount ->
                        FilledTonalButton(onClick = { onAddWater(amount) }) { Text("+$amount") }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onAddWater(-200) }) { Text("-200 ml") }
                    OutlinedButton(onClick = onReset) { Text("Sıfırla") }
                }
            }
        }

        item { SectionTitle("Son 7 gün") }

        item {
            val today = LocalDate.now()
            val week = (6 downTo 0).map { offset ->
                val date = today.minusDays(offset.toLong())
                date to state.hydrationHistory.firstOrNull { it.date == date }
            }
            BloomeeCard {
                Row(
                    modifier = Modifier.fillMaxWidth().height(140.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    week.forEach { (date, day) ->
                        val progress = day?.progress ?: 0f
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "${day?.consumedMl ?: 0}",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Spacer(Modifier.height(4.dp))
                            Box(
                                Modifier
                                    .size(width = 22.dp, height = (8 + 90 * progress).dp)
                                    .clip(MaterialTheme.shapes.small)
                                    .background(
                                        if (progress >= 1f) {
                                            MaterialTheme.colorScheme.tertiary
                                        } else {
                                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.45f)
                                        }
                                    )
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(date.format(shortDate), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        item {
            BloomeeCard {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    val last7 = state.hydrationHistory.filter {
                        it.date.isAfter(LocalDate.now().minusDays(7))
                    }
                    LabeledValue(
                        "7 gün ortalaması",
                        "${if (last7.isEmpty()) 0 else last7.sumOf { it.consumedMl } / last7.size} ml"
                    )
                    LabeledValue(
                        "Hedefi tutturduğun gün",
                        "${last7.count { it.progress >= 1f }}"
                    )
                }
            }
        }

        item {
            Button(onClick = { onAddWater(250) }, modifier = Modifier.fillMaxWidth()) {
                Text("Bir bardak ekle (250 ml)")
            }
        }
    }
}

@Composable
private fun WaterRing(progress: Float) {
    val color = MaterialTheme.colorScheme.tertiary
    val track = MaterialTheme.colorScheme.surface
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(104.dp)) {
        Canvas(Modifier.size(104.dp)) {
            val stroke = 12.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = track,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * progress.coerceIn(0f, 1f),
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        Text("%${(progress * 100).toInt()}", style = MaterialTheme.typography.headlineSmall)
    }
}
