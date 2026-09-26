package com.bloomee.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bloomee.app.domain.model.DailyLog
import com.bloomee.app.ui.BloomeeUiState
import com.bloomee.app.ui.components.BloomeeCard
import com.bloomee.app.ui.components.SectionTitle
import com.bloomee.app.ui.components.flowColor
import com.bloomee.app.ui.theme.FertilePeak
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("tr"))
private val fullDateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("tr"))

@Composable
fun CalendarScreen(
    state: BloomeeUiState,
    onSelectDate: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    var month by remember { mutableStateOf(YearMonth.now()) }
    val logsByDate = remember(state.logs) { state.logs.associateBy { it.date } }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            BloomeeCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(onClick = { month = month.minusMonths(1) }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Önceki ay")
                    }
                    Text(
                        month.atDay(1).format(monthFormatter)
                            .replaceFirstChar { it.titlecase(Locale("tr")) },
                        style = MaterialTheme.typography.titleMedium
                    )
                    IconButton(onClick = { month = month.plusMonths(1) }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Sonraki ay")
                    }
                }

                Spacer(Modifier.height(8.dp))
                MonthGrid(
                    month = month,
                    logsByDate = logsByDate,
                    predicted = state.predictedPeriodDays,
                    fertileWindow = state.stats.fertileWindow,
                    onSelectDate = onSelectDate
                )
            }
        }

        item { Legend() }

        item { SectionTitle("Son kayıtlar") }

        val recent = state.logs.sortedByDescending { it.date }.take(12)
        if (recent.isEmpty()) {
            item {
                BloomeeCard {
                    Text(
                        "Henüz kayıt yok. Bir güne dokunarak akış, ruh hali ve belirti girebilirsin.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            items(recent)
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.items(logs: List<DailyLog>) {
    logs.forEach { log ->
        item(key = log.date.toString()) {
            BloomeeCard {
                Text(log.date.format(fullDateFormatter), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    buildString {
                        append("Akış: ${log.flow.label}")
                        log.mood?.let { append(" · Ruh hali: ${it.label}") }
                        if (log.symptoms.isNotEmpty()) {
                            append(" · ${log.symptoms.joinToString { it.label }}")
                        }
                        if (log.painLevel > 0) append(" · Ağrı: ${log.painLevel}/5")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (log.note.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(log.note, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    logsByDate: Map<LocalDate, DailyLog>,
    predicted: Set<LocalDate>,
    fertileWindow: ClosedRange<LocalDate>?,
    onSelectDate: (LocalDate) -> Unit
) {
    val firstDay = month.atDay(1)
    val leadingBlanks = (firstDay.dayOfWeek.value + 6) % 7
    val totalCells = leadingBlanks + month.lengthOfMonth()
    val rows = (totalCells + 6) / 7
    val today = LocalDate.now()

    Row(Modifier.fillMaxWidth()) {
        listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz").forEach { label ->
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
    Spacer(Modifier.height(4.dp))

    repeat(rows) { rowIndex ->
        Row(Modifier.fillMaxWidth()) {
            repeat(7) { columnIndex ->
                val cellIndex = rowIndex * 7 + columnIndex
                val dayOfMonth = cellIndex - leadingBlanks + 1
                Box(Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                    if (dayOfMonth in 1..month.lengthOfMonth()) {
                        val date = month.atDay(dayOfMonth)
                        DayCell(
                            date = date,
                            log = logsByDate[date],
                            isPredicted = date in predicted,
                            isFertile = fertileWindow?.contains(date) == true,
                            isToday = date == today,
                            onClick = { onSelectDate(date) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    log: DailyLog?,
    isPredicted: Boolean,
    isFertile: Boolean,
    isToday: Boolean,
    onClick: () -> Unit
) {
    val logged = log?.flow?.isBleeding == true
    // A day can be both predicted-period and fertile: fertile fills, prediction rings.
    val background = when {
        logged -> flowColor(log.flow)
        isFertile -> FertilePeak.copy(alpha = 0.22f)
        isPredicted -> MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
        else -> Color.Transparent
    }
    val description = buildString {
        append(date.format(fullDateFormatter))
        when {
            logged -> append(", kanama: ${log!!.flow.label}")
            isPredicted && isFertile -> append(", tahmini regl günü ve doğurgan pencere")
            isPredicted -> append(", tahmini regl günü")
            isFertile -> append(", doğurgan pencere")
        }
        if (isToday) append(", bugün")
    }

    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(background)
            .then(
                if (isPredicted && !logged) {
                    Modifier.border(
                        2.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        CircleShape
                    )
                } else {
                    Modifier
                }
            )
            .then(
                if (isToday) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.clearAndSetSemantics { }
            )
            if (log != null && !log.isEmpty && !logged) {
                Box(
                    Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary)
                )
            }
        }
    }
}

@Composable
private fun Legend() {
    BloomeeCard {
        Text("Gösterge", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        LegendRow(flowColor(com.bloomee.app.domain.model.FlowLevel.MEDIUM), "Kanama kaydı")
        LegendRow(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f), "Tahmini regl günü")
        LegendRow(FertilePeak.copy(alpha = 0.22f), "Doğurgan pencere (tahmin)")
        LegendRow(MaterialTheme.colorScheme.secondary, "Belirti / not girilmiş gün")
    }
}

@Composable
private fun LegendRow(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 3.dp)
    ) {
        Box(Modifier.size(14.dp).clip(CircleShape).background(color))
        Spacer(Modifier.size(8.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}
