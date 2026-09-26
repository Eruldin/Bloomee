package com.bloomee.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.bloomee.app.domain.model.DailyLog
import com.bloomee.app.domain.model.FlowLevel
import com.bloomee.app.domain.model.Mood
import com.bloomee.app.domain.model.Symptom
import com.bloomee.app.ui.components.SectionTitle
import java.time.format.DateTimeFormatter
import java.util.Locale

private val sheetDateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy, EEEE", Locale("tr"))

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DailyLogSheet(
    initial: DailyLog,
    onSave: (DailyLog) -> Unit,
    onMarkPeriod: (log: DailyLog, totalDays: Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var flow by remember(initial) { mutableStateOf(initial.flow) }
    var mood by remember(initial) { mutableStateOf(initial.mood) }
    var symptoms by remember(initial) { mutableStateOf(initial.symptoms) }
    var pain by remember(initial) { mutableStateOf(initial.painLevel.toFloat()) }
    var sleep by remember(initial) { mutableStateOf(initial.sleepHours?.toString().orEmpty()) }
    var weight by remember(initial) { mutableStateOf(initial.weightKg?.toString().orEmpty()) }
    var note by remember(initial) { mutableStateOf(initial.note) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            initial.date.format(sheetDateFormatter),
            style = MaterialTheme.typography.titleLarge
        )

        SectionTitle("Kanama")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FlowLevel.entries.forEach { level ->
                FilterChip(
                    selected = flow == level,
                    onClick = { flow = level },
                    label = { Text(level.label) }
                )
            }
        }

        if (flow.isBleeding) {
            Text(
                "Regl bu günden itibaren kaç gün sürüyor? (tek seferde işaretle)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(2, 3, 5, 7).forEach { days ->
                    AssistChip(
                        onClick = {
                            onMarkPeriod(
                                initial.copy(
                                    flow = flow,
                                    mood = mood,
                                    symptoms = symptoms,
                                    painLevel = pain.toInt(),
                                    sleepHours = sleep.toDoubleOrNull(),
                                    weightKg = weight.toDoubleOrNull(),
                                    note = note.trim()
                                ),
                                days
                            )
                        },
                        label = { Text("$days gün") }
                    )
                }
            }
        }

        SectionTitle("Ruh hali")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Mood.entries.forEach { option ->
                FilterChip(
                    selected = mood == option,
                    onClick = { mood = if (mood == option) null else option },
                    label = { Text(option.label) }
                )
            }
        }

        SectionTitle("Belirtiler")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Symptom.entries.forEach { symptom ->
                FilterChip(
                    selected = symptom in symptoms,
                    onClick = {
                        symptoms = if (symptom in symptoms) symptoms - symptom else symptoms + symptom
                    },
                    label = { Text(symptom.label) }
                )
            }
        }

        SectionTitle("Ağrı düzeyi: ${pain.toInt()}/5")
        Slider(value = pain, onValueChange = { pain = it }, valueRange = 0f..5f, steps = 4)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = sleep,
                onValueChange = { sleep = it.filter { char -> char.isDigit() || char == '.' } },
                label = { Text("Uyku (saat)") },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Decimal
                ),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = weight,
                onValueChange = { weight = it.filter { char -> char.isDigit() || char == '.' } },
                label = { Text("Kilo (kg)") },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Decimal
                ),
                modifier = Modifier.weight(1f)
            )
        }

        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("Not") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(4.dp))

        Button(
            onClick = {
                onSave(
                    initial.copy(
                        flow = flow,
                        mood = mood,
                        symptoms = symptoms,
                        painLevel = pain.toInt(),
                        sleepHours = sleep.toDoubleOrNull(),
                        weightKg = weight.toDoubleOrNull(),
                        note = note.trim()
                    )
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Kaydet")
        }
        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Vazgeç") }
    }
}
