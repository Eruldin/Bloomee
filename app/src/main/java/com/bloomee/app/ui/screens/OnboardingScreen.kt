package com.bloomee.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.bloomee.app.domain.advice.AdviceEngine
import com.bloomee.app.domain.model.ActivityLevel
import com.bloomee.app.domain.model.UserProfile
import com.bloomee.app.ui.components.BloomeeCard
import java.time.LocalDate

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    onFinish: (profile: (UserProfile) -> UserProfile, lastPeriodStart: LocalDate?, periodLength: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var activity by remember { mutableStateOf(ActivityLevel.MODERATE) }
    var cycleLength by remember { mutableStateOf(28) }
    var periodLength by remember { mutableStateOf(5) }
    var daysAgo by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        Text("Bloomee'ye hoş geldin", style = MaterialTheme.typography.displaySmall)
        Text(
            "Birkaç soruyla döngü tahminini ve su hedefini sana göre ayarlayalım. " +
                "Tüm veriler cihazında kalır; bulut yedeklemeyi sonradan açabilirsin.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        BloomeeCard {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Adın (isteğe bağlı)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = weight,
                onValueChange = { value -> weight = value.filter { it.isDigit() || it == '.' } },
                label = { Text("Kilo (kg)") },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Decimal
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            Text("Hareket düzeyin", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActivityLevel.entries.forEach { level ->
                    FilterChip(
                        selected = activity == level,
                        onClick = { activity = level },
                        label = { Text(level.label) }
                    )
                }
            }
        }

        BloomeeCard {
            Text("Son reglin kaç gün önce başladı?", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = daysAgo,
                onValueChange = { value -> daysAgo = value.filter { it.isDigit() }.take(2) },
                label = { Text("Gün") },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            Text("Ortalama döngü uzunluğu: $cycleLength gün", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(25, 26, 27, 28, 29, 30, 31, 32).forEach { value ->
                    FilterChip(
                        selected = cycleLength == value,
                        onClick = { cycleLength = value },
                        label = { Text("$value") }
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text("Regl süresi: $periodLength gün", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (3..8).forEach { value ->
                    FilterChip(
                        selected = periodLength == value,
                        onClick = { periodLength = value },
                        label = { Text("$value") }
                    )
                }
            }
        }

        Text(
            AdviceEngine.MEDICAL_DISCLAIMER,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Button(
            onClick = {
                val start = daysAgo.toIntOrNull()?.let { LocalDate.now().minusDays(it.toLong()) }
                onFinish(
                    { current ->
                        current.copy(
                            displayName = name.trim(),
                            weightKg = weight.toDoubleOrNull(),
                            activityLevel = activity,
                            defaultCycleLength = cycleLength,
                            defaultPeriodLength = periodLength,
                            onboardingCompleted = true
                        )
                    },
                    start,
                    periodLength
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Başla")
        }

        TextButton(
            onClick = { onFinish({ it.copy(onboardingCompleted = true) }, null, periodLength) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Şimdilik geç")
        }
    }
}
