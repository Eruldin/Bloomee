package com.bloomee.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Text
import com.bloomee.app.domain.hydration.HydrationCalculator
import com.bloomee.app.domain.model.FlowLevel
import com.bloomee.app.domain.model.HydrationDay
import com.bloomee.app.domain.nutrition.CalorieCalculator
import com.bloomee.app.domain.prediction.CyclePredictor
import com.bloomee.app.shared.theme.UiPalette
import java.time.LocalDate

class MainActivity : ComponentActivity() {

    private lateinit var storage: WearStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        storage = WearStorage(this)
        setContent {
            WearHome(storage)
        }
    }
}

@Composable
private fun WearHome(storage: WearStorage) {
    var version by remember { mutableIntStateOf(0) }
    val today = LocalDate.now()
    val stats = CyclePredictor.calculate(storage.logs(), today)
    val waterGoal = HydrationCalculator.dailyGoalMl(null, phase = stats.phase)
    val waterMl = storage.waterMl(today)
    val water = HydrationDay(today, waterMl, waterGoal)
    val kcal = storage.calories(today)
    val kcalGoal = CalorieCalculator.dailyGoalKcal(null, null, null)
    version.let { } // read state so edits recompose

    BloomeeWearTheme(storage.themeName) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colors.background),
            state = rememberScalingLazyListState(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                Text(
                    "Bloomee",
                    color = MaterialTheme.colors.primary,
                    style = MaterialTheme.typography.title2,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                if (stats.cycleDay > 0) {
                    Text(
                        "${stats.cycleDay}. gün · ${stats.phase.label}",
                        style = MaterialTheme.typography.body1,
                        textAlign = TextAlign.Center
                    )
                    stats.daysToNextPeriod?.let {
                        Text(
                            if (it >= 0) "Regle $it gün" else "Regl ${-it} gün gecikti",
                            style = MaterialTheme.typography.caption1,
                            color = MaterialTheme.colors.secondary,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Text(
                        "Regl takibi için\naşağıdan kayıt gir",
                        style = MaterialTheme.typography.body2,
                        textAlign = TextAlign.Center
                    )
                }
            }

            item {
                val bleeding = storage.flowOn(today).isBleeding
                Chip(
                    onClick = {
                        storage.setFlow(today, if (bleeding) FlowLevel.NONE else FlowLevel.MEDIUM)
                        version++
                    },
                    label = {
                        Text(if (bleeding) "Regl: açık" else "Regl başladı")
                    },
                    colors = if (bleeding) ChipDefaults.secondaryChipColors() else ChipDefaults.primaryChipColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item { SectionLabel("Su · ${water.consumedMl}/${water.goalMl} ml") }
            item {
                ProgressRing(water.progress, MaterialTheme.colors.secondary) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        CompactChip(onClick = {
                            storage.addWater(today, 250)
                            version++
                        }, label = { Text("+250") })
                        CompactChip(
                            onClick = {
                                storage.resetWater(today)
                                version++
                            },
                            label = { Text("Sıfırla") },
                            colors = ChipDefaults.secondaryChipColors()
                        )
                    }
                }
            }

            item { SectionLabel("Kalori · $kcal/$kcalGoal kcal") }
            item {
                ProgressRing(
                    (kcal.toFloat() / kcalGoal).coerceIn(0f, 1f),
                    MaterialTheme.colors.primary
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        CompactChip(onClick = {
                            storage.addCalories(today, 150)
                            version++
                        }, label = { Text("+150") })
                        CompactChip(onClick = {
                            storage.addCalories(today, 300)
                            version++
                        }, label = { Text("+300") })
                    }
                }
            }

            item { SectionLabel("Tema") }
            item {
                val current = UiPalette.fromKey(storage.themeName)
                Chip(
                    onClick = {
                        val next = UiPalette.entries[(current.ordinal + 1) % UiPalette.entries.size]
                        storage.themeName = next.key
                        version++
                    },
                    label = { Text("Tema: ${current.label}") },
                    icon = {
                        Box(
                            Modifier.size(14.dp).clip(CircleShape)
                                .background(Color(current.primary.light))
                        )
                    },
                    colors = ChipDefaults.secondaryChipColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.caption1,
        color = MaterialTheme.colors.onBackground,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ProgressRing(
    progress: Float,
    color: Color,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = progress,
                modifier = Modifier.size(56.dp),
                indicatorColor = color,
                trackColor = MaterialTheme.colors.surface,
                strokeWidth = 5.dp
            )
        }
        content()
    }
}
