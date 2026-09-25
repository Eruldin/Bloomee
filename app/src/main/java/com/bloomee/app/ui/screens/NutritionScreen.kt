package com.bloomee.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.bloomee.app.domain.model.Meal
import com.bloomee.app.domain.model.NutritionEntry
import com.bloomee.app.ui.BloomeeUiState
import com.bloomee.app.ui.components.BloomeeCard
import com.bloomee.app.ui.components.LabeledValue
import com.bloomee.app.ui.components.SectionTitle
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val shortDate = DateTimeFormatter.ofPattern("d MMM", Locale("tr"))

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NutritionScreen(
    state: BloomeeUiState,
    onAddEntry: (name: String, kcal: Int, meal: Meal) -> Unit,
    onRemoveEntry: (id: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val nutrition = state.nutritionToday
    var name by remember { mutableStateOf("") }
    var kcal by remember { mutableStateOf("") }
    var meal by remember { mutableStateOf(Meal.SNACK) }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            BloomeeCard(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CalorieRing(nutrition.progress)
                    Spacer(Modifier.size(18.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "${nutrition.consumedKcal} kcal",
                            style = MaterialTheme.typography.displaySmall
                        )
                        Text(
                            "Hedef ${nutrition.goalKcal} kcal",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "Hedef; kilon, boyun, yaşın ve hareket düzeyine göre tahmin edildi.",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            BloomeeCard {
                SectionTitle("Öğün ekle")
                Spacer(Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Meal.entries.forEach { option ->
                        FilterChip(
                            selected = meal == option,
                            onClick = { meal = option },
                            label = { Text(option.label) }
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Ne yedin?") },
                        singleLine = true,
                        modifier = Modifier.weight(1.4f)
                    )
                    OutlinedTextField(
                        value = kcal,
                        onValueChange = { value -> kcal = value.filter { it.isDigit() }.take(4) },
                        label = { Text("kcal") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(0.7f)
                    )
                }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        val parsed = kcal.toIntOrNull() ?: return@Button
                        onAddEntry(name, parsed, meal)
                        name = ""
                        kcal = ""
                    },
                    enabled = name.isNotBlank() && (kcal.toIntOrNull() ?: 0) > 0,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Ekle")
                }
            }
        }

        item { SectionTitle("Bugünkü öğünler") }

        if (nutrition.entries.isEmpty()) {
            item {
                BloomeeCard {
                    Text(
                        "Henüz kayıt yok. İlk öğününü yukarıdan ekle.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(nutrition.entries, key = { it.id }) { entry ->
                NutritionEntryRow(entry, onRemoveEntry)
            }
        }

        item { SectionTitle("Son 7 gün") }

        item {
            val today = LocalDate.now()
            val week = (6 downTo 0).map { offset ->
                val date = today.minusDays(offset.toLong())
                date to state.nutritionHistory.filter { it.date == date }.sumOf { it.kcal }
            }
            val goal = nutrition.goalKcal.coerceAtLeast(1)
            BloomeeCard {
                Row(
                    modifier = Modifier.fillMaxWidth().height(140.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    week.forEach { (date, consumed) ->
                        val progress = (consumed.toFloat() / goal).coerceIn(0f, 1.3f)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$consumed", style = MaterialTheme.typography.labelMedium)
                            Spacer(Modifier.height(4.dp))
                            Box(
                                Modifier
                                    .size(width = 22.dp, height = (8 + 90 * progress.coerceAtMost(1f)).dp)
                                    .clip(MaterialTheme.shapes.small)
                                    .background(
                                        when {
                                            consumed <= 0 -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f)
                                            consumed in (goal * 0.8).toInt()..(goal * 1.1).toInt() ->
                                                MaterialTheme.colorScheme.secondary
                                            else -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)
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
                    val last7 = (0..6).map { offset ->
                        val date = LocalDate.now().minusDays(offset.toLong())
                        state.nutritionHistory.filter { it.date == date }.sumOf { it.kcal }
                    }
                    LabeledValue(
                        "7 gün ortalaması",
                        "${last7.sum() / last7.size} kcal"
                    )
                    LabeledValue(
                        "Bugünkü öğün",
                        "${nutrition.entries.size}"
                    )
                }
            }
        }
    }
}

@Composable
private fun NutritionEntryRow(entry: NutritionEntry, onRemove: (String) -> Unit) {
    BloomeeCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(entry.name, style = MaterialTheme.typography.titleMedium)
                Text(entry.meal.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("${entry.kcal} kcal", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = { onRemove(entry.id) }) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Sil",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CalorieRing(progress: Float) {
    val color = MaterialTheme.colorScheme.secondary
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
