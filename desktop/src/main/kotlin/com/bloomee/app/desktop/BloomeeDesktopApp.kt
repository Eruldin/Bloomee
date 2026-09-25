package com.bloomee.app.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bloomee.app.domain.advice.AdviceEngine
import com.bloomee.app.domain.hydration.HydrationCalculator
import com.bloomee.app.domain.model.ActivityLevel
import com.bloomee.app.domain.model.DailyLog
import com.bloomee.app.domain.model.FlowLevel
import com.bloomee.app.domain.model.HydrationDay
import com.bloomee.app.domain.model.Meal
import com.bloomee.app.domain.model.CycleStats
import com.bloomee.app.domain.model.NutritionEntry
import com.bloomee.app.domain.model.NutritionDay
import com.bloomee.app.domain.nutrition.CalorieCalculator
import com.bloomee.app.domain.prediction.CyclePredictor
import com.bloomee.app.shared.theme.UiPalette
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.time.LocalDate
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale
import java.util.UUID

private enum class DesktopSection(val label: String, val icon: ImageVector) {
    TODAY("Bugün", Icons.Filled.FavoriteBorder),
    DATA("Profil ve veri", Icons.Filled.Settings)
}

private class DesktopAppState(private val store: BloomeeStore = BloomeeStore()) {
    val data: DesktopData = store.load()
    var section by mutableStateOf(DesktopSection.TODAY)
    var version by mutableIntStateOf(0)
    var statusMessage by mutableStateOf<String?>(null)

    fun touch() {
        store.save(data)
        version++
    }

    fun importBackup(file: File) {
        statusMessage = store.importBackup(file, data)
        version++
    }

    fun exportBackup(file: File) {
        store.exportBackup(file, data)
        statusMessage = "Yedek ${file.name} olarak kaydedildi."
    }
}

@Composable
fun BloomeeDesktopApp() {
    val state = remember { DesktopAppState() }
    val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val darkTheme = state.data.profile.darkMode ?: systemDark

    BloomeeDesktopTheme(paletteName = state.data.profile.themeName, darkTheme = darkTheme) {
        Row(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
                Spacer(Modifier.height(16.dp))
                Text(
                    "Bloomee",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(24.dp))
                DesktopSection.entries.forEach { item ->
                    NavigationRailItem(
                        selected = state.section == item,
                        onClick = { state.section = item },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) }
                    )
                }
            }
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
                when (state.section) {
                    DesktopSection.TODAY -> TodayDashboard(state)
                    DesktopSection.DATA -> DataScreen(state)
                }
            }
        }
    }
}

@Composable
private fun TodayDashboard(state: DesktopAppState) {
    state.version // recompose on data changes
    val today = LocalDate.now()
    val profile = state.data.profile
    val stats = CyclePredictor.calculate(
        logs = state.data.logs.values.toList(),
        today = today,
        defaultCycleLength = 28,
        defaultPeriodLength = 5
    )
    val waterGoal = HydrationCalculator.dailyGoalMl(profile.weightKg, profile.activityLevel, stats.phase)
    val waterToday = HydrationDay(today, state.data.hydrationMl[today] ?: 0, waterGoal)
    val kcalGoal = CalorieCalculator.dailyGoalKcal(
        profile.weightKg, profile.heightCm, profile.birthYear, profile.activityLevel, today
    )
    val nutritionToday = NutritionDay(today, state.data.nutrition.filter { it.date == today }, kcalGoal)
    val todayLog = state.data.logs[today]

    Text("Merhaba ${profile.displayName.ifBlank { "" }}", style = MaterialTheme.typography.headlineMedium)
    Text(
        today.dayOfWeek.getDisplayName(JavaTextStyle.FULL, Locale("tr")) + ", " + today,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(16.dp))

    ThemePicker(state)

    Spacer(Modifier.height(16.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CycleCard(stats, todayLog, state, today)
            WaterCard(waterToday, state, today)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CalorieCard(nutritionToday, state, today)
            AdviceColumn(stats, todayLog, waterToday)
        }
    }
}

@Composable
private fun ThemePicker(state: DesktopAppState) {
    Card {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                UiPalette.entries.forEach { palette ->
                    FilterChip(
                        selected = state.data.profile.themeName == palette.key,
                        onClick = {
                            state.data.profile = state.data.profile.copy(themeName = palette.key)
                            state.touch()
                        },
                        label = { Text(palette.label) },
                        leadingIcon = {
                            Box(
                                Modifier.size(14.dp).clip(CircleShape)
                                    .background(Color(palette.primary.main))
                            )
                        }
                    )
                }
            }
            IconButton(onClick = {
                val next = !(state.data.profile.darkMode ?: false)
                state.data.profile = state.data.profile.copy(darkMode = next)
                state.touch()
            }) {
                Icon(
                    if (state.data.profile.darkMode == true) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                    contentDescription = "Tema"
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CycleCard(
    stats: CycleStats,
    todayLog: DailyLog?,
    state: DesktopAppState,
    today: LocalDate
) {
    Card {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CalendarMonth, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Döngü", style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.height(12.dp))
            if (stats.hasEnoughData || stats.cycleDay > 0) {
                Text("${stats.cycleDay}. gün — ${stats.phase.label}", style = MaterialTheme.typography.titleMedium)
                stats.daysToNextPeriod?.let {
                    Text(
                        if (it >= 0) "Bir sonraki regle $it gün" else "Regl ${-it} gün gecikti",
                        color = if (stats.isLate) WarningAmber else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                stats.fertileWindow?.let {
                    Text("Doğurgan pencere: ${it.start} → ${it.endInclusive}", style = MaterialTheme.typography.bodyMedium)
                }
                Text("Doğurganlık: ${stats.fertility.label}", style = MaterialTheme.typography.bodyMedium)
            } else {
                Text("Bugünkü kanama durumunu seçerek takibi başlat.")
            }
            Spacer(Modifier.height(12.dp))
            Text("Bugünkü akıntı:", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FlowLevel.entries.forEach { flow ->
                    FilterChip(
                        selected = (todayLog?.flow ?: FlowLevel.NONE) == flow,
                        onClick = {
                            state.data.logs[today] = (todayLog ?: DailyLog(today)).copy(flow = flow)
                            state.touch()
                        },
                        label = { Text(flow.label) }
                    )
                }
            }
        }
    }
}

@Composable
private fun WaterCard(day: HydrationDay, state: DesktopAppState, today: LocalDate) {
    Card {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.WaterDrop, null, tint = MaterialTheme.colorScheme.tertiary)
                Spacer(Modifier.width(8.dp))
                Text("Su", style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.height(8.dp))
            Text("${day.consumedMl} / ${day.goalMl} ml", style = MaterialTheme.typography.titleMedium)
            LinearProgressIndicator(
                progress = { day.progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = MaterialTheme.colorScheme.tertiary
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HydrationCalculator.quickAddOptionsMl.forEach { ml ->
                    OutlinedButton(onClick = {
                        state.data.hydrationMl[today] = day.consumedMl + ml
                        state.data.hydrationGoalMl[today] = day.goalMl
                        state.touch()
                    }) { Text("+$ml ml") }
                }
                TextButton(onClick = {
                    state.data.hydrationMl[today] = 0
                    state.touch()
                }) { Text("Sıfırla") }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CalorieCard(day: NutritionDay, state: DesktopAppState, today: LocalDate) {
    var newName by remember { mutableStateOf("") }
    var newKcal by remember { mutableStateOf("") }
    var newMeal by remember { mutableStateOf(Meal.SNACK) }

    Card {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Restaurant, null, tint = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.width(8.dp))
                Text("Kalori", style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.height(8.dp))
            Text("${day.consumedKcal} / ${day.goalKcal} kcal", style = MaterialTheme.typography.titleMedium)
            LinearProgressIndicator(
                progress = { day.progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(Modifier.height(12.dp))
            day.entries.forEach { entry ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${entry.meal.label} · ${entry.name}", style = MaterialTheme.typography.bodyMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${entry.kcal} kcal", style = MaterialTheme.typography.bodyMedium)
                        IconButton(onClick = {
                            state.data.nutrition.removeAll { it.id == entry.id }
                            state.touch()
                        }) { Icon(Icons.Filled.Delete, "Sil", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Ne yedin?") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = newKcal,
                    onValueChange = { newKcal = it.filter(Char::isDigit) },
                    label = { Text("kcal") },
                    singleLine = true,
                    modifier = Modifier.width(90.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                    Meal.entries.forEach { meal ->
                        FilterChip(
                            selected = newMeal == meal,
                            onClick = { newMeal = meal },
                            label = { Text(meal.label) }
                        )
                    }
                }
                Button(
                    onClick = {
                        val kcal = newKcal.toIntOrNull() ?: return@Button
                        if (newName.isBlank() || kcal <= 0) return@Button
                        state.data.nutrition += NutritionEntry(
                            id = UUID.randomUUID().toString(),
                            date = today,
                            meal = newMeal,
                            name = newName.trim(),
                            kcal = kcal
                        )
                        newName = ""
                        newKcal = ""
                        state.touch()
                    },
                    enabled = newName.isNotBlank() && (newKcal.toIntOrNull() ?: 0) > 0
                ) {
                    Icon(Icons.Filled.Add, null)
                    Text("Ekle")
                }
            }
        }
    }
}

@Composable
private fun AdviceColumn(
    stats: CycleStats,
    todayLog: DailyLog?,
    waterToday: HydrationDay
) {
    val cards = remember(stats, todayLog, waterToday) {
        AdviceEngine.cardsFor(stats, todayLog, waterToday)
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Bugün için öneriler", style = MaterialTheme.typography.titleLarge)
        cards.forEach { card ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(12.dp)) {
                    Text(card.title, style = MaterialTheme.typography.titleMedium)
                    Text(card.body, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun DataScreen(state: DesktopAppState) {
    state.version
    val profile = state.data.profile

    var name by remember(profile.displayName, state.version) { mutableStateOf(profile.displayName) }
    var weight by remember(profile.weightKg, state.version) {
        mutableStateOf(profile.weightKg?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "")
    }
    var height by remember(profile.heightCm, state.version) { mutableStateOf(profile.heightCm?.toString() ?: "") }
    var birthYear by remember(profile.birthYear, state.version) { mutableStateOf(profile.birthYear?.toString() ?: "") }

    Text("Profil ve veri", style = MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.height(16.dp))

    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Profil", style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Adın") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(
                    birthYear, { birthYear = it.filter(Char::isDigit) },
                    label = { Text("Doğum yılı") }, singleLine = true, modifier = Modifier.width(140.dp)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    weight, { weight = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Kilo (kg)") }, singleLine = true, modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    height, { height = it.filter(Char::isDigit) },
                    label = { Text("Boy (cm)") }, singleLine = true, modifier = Modifier.weight(1f)
                )
            }
            Text("Aktivite düzeyi", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActivityLevel.entries.forEach { level ->
                    FilterChip(
                        selected = profile.activityLevel == level,
                        onClick = {
                            state.data.profile = profile.copy(activityLevel = level)
                            state.touch()
                        },
                        label = { Text(level.label) }
                    )
                }
            }
            Button(onClick = {
                state.data.profile = profile.copy(
                    displayName = name.trim(),
                    birthYear = birthYear.toIntOrNull(),
                    weightKg = weight.toDoubleOrNull(),
                    heightCm = height.toIntOrNull()
                )
                state.touch()
                state.statusMessage = "Profil kaydedildi."
            }) { Text("Kaydet") }
        }
    }

    Spacer(Modifier.height(16.dp))

    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Cihazlar arası aktarım", style = MaterialTheme.typography.titleLarge)
            Text(
                "Telefon uygulamasındaki \"Yedekle\" dosyasını içe aktar; burada da dışa aktarıp telefona geri yükleyebilirsin.",
                style = MaterialTheme.typography.bodyMedium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    pickFile(load = true)?.let { state.importBackup(it) }
                }) {
                    Icon(Icons.Filled.Download, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Telefon yedeğini içe aktar")
                }
                OutlinedButton(onClick = {
                    pickFile(load = false)?.let { state.exportBackup(it) }
                }) {
                    Icon(Icons.Filled.Upload, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Yedeği dışa aktar")
                }
            }
            state.statusMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                "Veriler ${BloomeeStore.defaultFile().absolutePath} dosyasında saklanır.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun pickFile(load: Boolean): File? {
    val dialog = FileDialog(null as Frame?, if (load) "Yedek dosyasını seç" else "Yedek nereye kaydedilsin",
        if (load) FileDialog.LOAD else FileDialog.SAVE)
    if (!load) dialog.file = "bloomee-yedek.json"
    dialog.isVisible = true
    val dir = dialog.directory ?: return null
    val name = dialog.file ?: return null
    return File(dir, name)
}
