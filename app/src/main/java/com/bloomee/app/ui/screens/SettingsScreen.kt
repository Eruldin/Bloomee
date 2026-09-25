package com.bloomee.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.bloomee.app.data.sync.SyncState
import com.bloomee.app.domain.model.ActivityLevel
import com.bloomee.app.domain.model.UserProfile
import com.bloomee.app.ui.BloomeeUiState
import com.bloomee.app.ui.components.BloomeeCard
import com.bloomee.app.ui.components.SectionTitle
import com.bloomee.app.ui.theme.ThemePalette

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    state: BloomeeUiState,
    onUpdateProfile: ((UserProfile) -> UserProfile) -> Unit,
    onSyncNow: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val profile = state.profile
    var name by remember(profile.displayName) { mutableStateOf(profile.displayName) }
    var weight by remember(profile.weightKg) { mutableStateOf(profile.weightKg?.toString().orEmpty()) }
    var height by remember(profile.heightCm) { mutableStateOf(profile.heightCm?.toString().orEmpty()) }
    var birthYear by remember(profile.birthYear) { mutableStateOf(profile.birthYear?.toString().orEmpty()) }
    var apiKey by remember(profile.assistantApiKey) { mutableStateOf(profile.assistantApiKey) }
    var partnerName by remember(profile.partnerName) { mutableStateOf(profile.partnerName) }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            BloomeeCard {
                SectionTitle("Profil")
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        onUpdateProfile { current -> current.copy(displayName = it) }
                    },
                    label = { Text("Adın") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = weight,
                    onValueChange = { value ->
                        weight = value.filter { it.isDigit() || it == '.' }
                        weight.toDoubleOrNull()?.let { parsed ->
                            onUpdateProfile { current -> current.copy(weightKg = parsed) }
                        }
                    },
                    label = { Text("Kilo (kg) — su ve kalori hedefi için") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = height,
                    onValueChange = { value ->
                        height = value.filter { it.isDigit() }.take(3)
                        height.toIntOrNull()?.let { parsed ->
                            onUpdateProfile { current -> current.copy(heightCm = parsed) }
                        }
                    },
                    label = { Text("Boy (cm) — kalori hedefi için") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = birthYear,
                    onValueChange = { value ->
                        birthYear = value.filter { it.isDigit() }.take(4)
                        birthYear.toIntOrNull()?.let { parsed ->
                            onUpdateProfile { current -> current.copy(birthYear = parsed) }
                        }
                    },
                    label = { Text("Doğum yılı — kalori hedefi için") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                Text("Hareket düzeyi", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActivityLevel.entries.forEach { level ->
                        FilterChip(
                            selected = profile.activityLevel == level,
                            onClick = {
                                onUpdateProfile { current -> current.copy(activityLevel = level) }
                            },
                            label = { Text(level.label) }
                        )
                    }
                }
            }
        }

        item {
            BloomeeCard {
                SectionTitle("Tema")
                Spacer(Modifier.height(4.dp))
                Text(
                    "Uygulama renklerini seç",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemePalette.entries.forEach { palette ->
                        FilterChip(
                            selected = profile.themeName == palette.key,
                            onClick = {
                                onUpdateProfile { it.copy(themeName = palette.key) }
                            },
                            label = { Text(palette.label) },
                            leadingIcon = {
                                Box(
                                    Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(palette.primary.main)
                                )
                            }
                        )
                    }
                }
            }
        }

        item {
            BloomeeCard {
                SectionTitle("Döngü varsayılanları")
                Spacer(Modifier.height(8.dp))
                StepperRow(
                    label = "Döngü uzunluğu",
                    value = "${profile.defaultCycleLength} gün",
                    onDecrease = {
                        onUpdateProfile { it.copy(defaultCycleLength = (it.defaultCycleLength - 1).coerceAtLeast(21)) }
                    },
                    onIncrease = {
                        onUpdateProfile { it.copy(defaultCycleLength = (it.defaultCycleLength + 1).coerceAtMost(45)) }
                    }
                )
                StepperRow(
                    label = "Regl süresi",
                    value = "${profile.defaultPeriodLength} gün",
                    onDecrease = {
                        onUpdateProfile { it.copy(defaultPeriodLength = (it.defaultPeriodLength - 1).coerceAtLeast(2)) }
                    },
                    onIncrease = {
                        onUpdateProfile { it.copy(defaultPeriodLength = (it.defaultPeriodLength + 1).coerceAtMost(10)) }
                    }
                )
            }
        }

        item {
            BloomeeCard {
                SectionTitle("Hatırlatmalar")
                Spacer(Modifier.height(4.dp))
                SwitchRow("Su hatırlatıcısı", profile.reminderHydrationEnabled) { enabled ->
                    onUpdateProfile { it.copy(reminderHydrationEnabled = enabled) }
                }
                SwitchRow("Regl yaklaşma bildirimi", profile.reminderPeriodEnabled) { enabled ->
                    onUpdateProfile { it.copy(reminderPeriodEnabled = enabled) }
                }
                SwitchRow("İlaç hatırlatıcısı", profile.reminderMedicationEnabled) { enabled ->
                    onUpdateProfile { it.copy(reminderMedicationEnabled = enabled) }
                }
                if (profile.reminderMedicationEnabled) {
                    StepperRow(
                        label = "İlaç saati",
                        value = "${profile.medicationReminderHour}:00",
                        onDecrease = {
                            onUpdateProfile { it.copy(medicationReminderHour = (it.medicationReminderHour - 1).coerceAtLeast(0)) }
                        },
                        onIncrease = {
                            onUpdateProfile { it.copy(medicationReminderHour = (it.medicationReminderHour + 1).coerceAtMost(23)) }
                        }
                    )
                }
            }
        }

        item {
            BloomeeCard {
                SectionTitle("Partner modu")
                Spacer(Modifier.height(4.dp))
                SwitchRow("Partner özetini göster", profile.partnerModeEnabled) { enabled ->
                    onUpdateProfile { it.copy(partnerModeEnabled = enabled) }
                }
                if (profile.partnerModeEnabled) {
                    OutlinedTextField(
                        value = partnerName,
                        onValueChange = {
                            partnerName = it
                            onUpdateProfile { current -> current.copy(partnerName = it) }
                        },
                        label = { Text("Partner adı") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Özet yalnızca bu cihazda gösterilir; verin kimseyle otomatik paylaşılmaz.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            BloomeeCard {
                SectionTitle("Bulut yedekleme")
                Spacer(Modifier.height(4.dp))
                val syncLabel = when (state.syncState) {
                    SyncState.UNCONFIGURED -> "Firebase yapılandırması (google-services.json) eklenmedi."
                    SyncState.DISABLED -> "Kapalı. Verin yalnızca bu cihazda."
                    SyncState.IDLE -> "Açık. Kayıtların hesabına şifreli bağlantıyla yedekleniyor."
                    SyncState.SYNCING -> "Eşitleniyor..."
                    SyncState.ERROR -> "Son eşitleme başarısız oldu."
                }
                SwitchRow(
                    label = "Bulut eşitlemesi",
                    checked = profile.cloudSyncEnabled,
                    enabled = state.syncState != SyncState.UNCONFIGURED
                ) { enabled ->
                    onUpdateProfile { it.copy(cloudSyncEnabled = enabled) }
                }
                Text(
                    syncLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (profile.cloudSyncEnabled) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = onSyncNow) { Text("Şimdi eşitle") }
                }
            }
        }

        item {
            BloomeeCard {
                SectionTitle("Yedek dosyası")
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onExportBackup) { Text("Dışa aktar") }
                    OutlinedButton(onClick = onImportBackup) { Text("İçe aktar") }
                }
            }
        }

        item {
            BloomeeCard {
                SectionTitle("Asistan")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = {
                        apiKey = it
                        onUpdateProfile { current -> current.copy(assistantApiKey = it.trim()) }
                    },
                    label = { Text("Gemini API anahtarı") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Anahtar yalnızca bu cihazda saklanır ve uygulamayla birlikte dağıtılmaz. " +
                        "Girmezsen uygulama çevrimdışı öneri motoruyla çalışmaya devam eder.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
private fun StepperRow(
    label: String,
    value: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                value,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onDecrease) { Text("−") }
            OutlinedButton(onClick = onIncrease) { Text("+") }
        }
    }
}
