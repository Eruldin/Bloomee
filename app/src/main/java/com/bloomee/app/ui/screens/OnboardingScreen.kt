package com.bloomee.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.bloomee.app.domain.advice.AdviceEngine
import com.bloomee.app.domain.model.ActivityLevel
import com.bloomee.app.domain.model.UserProfile
import com.bloomee.app.notification.rememberNotificationPermissionRequest
import com.bloomee.app.ui.components.BloomeeCard
import com.bloomee.app.ui.theme.ThemePalette
import java.time.LocalDate
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    onFinish: (profile: (UserProfile) -> UserProfile, lastPeriodStart: LocalDate?, periodLength: Int) -> Unit,
    themeKey: String,
    onSelectTheme: (String) -> Unit,
    remindersEnabled: Boolean,
    onRemindersChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var birthYear by remember { mutableStateOf("") }
    var activity by remember { mutableStateOf(ActivityLevel.MODERATE) }
    var cycleLength by remember { mutableStateOf(28) }
    var periodLength by remember { mutableStateOf(5) }
    var daysAgo by remember { mutableStateOf("") }
    val requestNotificationPermission = rememberNotificationPermissionRequest()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        OnboardingHero()

        BloomeeCard {
            Text("Temanı seç", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Renkler uygulamanın her yerine anında uygulanır; sonra Ayarlar'dan değiştirebilirsin.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ThemePalette.entries.forEach { palette ->
                    val selected = themeKey == palette.key
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(palette.primary.main)
                                .then(
                                    if (selected) {
                                        Modifier.border(
                                            3.dp,
                                            MaterialTheme.colorScheme.onSurface,
                                            CircleShape
                                        )
                                    } else {
                                        Modifier
                                    }
                                )
                                .clickable { onSelectTheme(palette.key) }
                                .semantics {
                                    contentDescription = "Tema: ${palette.label}"
                                }
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(palette.label, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

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
            OutlinedTextField(
                value = height,
                onValueChange = { value -> height = value.filter { it.isDigit() }.take(3) },
                label = { Text("Boy (cm) — isteğe bağlı") },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = birthYear,
                onValueChange = { value -> birthYear = value.filter { it.isDigit() }.take(4) },
                label = { Text("Doğum yılı — isteğe bağlı") },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Number
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

        BloomeeCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Hatırlatıcılar", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Su ve yaklaşan regl bildirimleri",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = remindersEnabled,
                    onCheckedChange = { enabled ->
                        onRemindersChange(enabled)
                        if (enabled) requestNotificationPermission()
                    }
                )
            }
        }

        Text(
            AdviceEngine.MEDICAL_DISCLAIMER,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Button(
            onClick = {
                if (remindersEnabled) requestNotificationPermission()
                val start = daysAgo.toIntOrNull()?.let { LocalDate.now().minusDays(it.toLong()) }
                onFinish(
                    { current ->
                        current.copy(
                            displayName = name.trim(),
                            weightKg = weight.toDoubleOrNull(),
                            heightCm = height.toIntOrNull(),
                            birthYear = birthYear.toIntOrNull(),
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

@Composable
private fun OnboardingHero() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
            .padding(horizontal = 20.dp, vertical = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BloomMark(Modifier.size(72.dp))
            Spacer(Modifier.height(12.dp))
            Text("Bloomee'ye hoş geldin", style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(6.dp))
            Text(
                "Regl, su ve kalori takibi tek yerde. Birkaç soruyla sana göre ayarlayalım — " +
                    "tüm veriler cihazında kalır.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BloomMark(modifier: Modifier = Modifier) {
    val petal = MaterialTheme.colorScheme.primary
    val center = MaterialTheme.colorScheme.tertiary
    Canvas(modifier) {
        val radius = size.minDimension
        val cx = size.width / 2f
        val cy = size.height / 2f
        val petalRadius = radius * 0.26f
        val petalDistance = radius * 0.26f
        for (i in 0 until 5) {
            val angle = Math.toRadians((i * 72.0) - 90.0)
            drawCircle(
                color = petal,
                radius = petalRadius,
                center = Offset(
                    cx + (petalDistance * cos(angle)).toFloat(),
                    cy + (petalDistance * sin(angle)).toFloat()
                )
            )
        }
        drawCircle(color = center, radius = radius * 0.16f, center = Offset(cx, cy))
    }
}
