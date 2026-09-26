package com.bloomee.app.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bloomee.app.domain.model.DailyLog
import com.bloomee.app.domain.model.FlowLevel
import com.bloomee.app.ui.screens.AssistantSheet
import com.bloomee.app.ui.screens.CalendarScreen
import com.bloomee.app.ui.screens.DailyLogSheet
import com.bloomee.app.ui.screens.HomeScreen
import com.bloomee.app.ui.screens.HydrationScreen
import com.bloomee.app.ui.screens.InsightsScreen
import com.bloomee.app.ui.screens.NutritionScreen
import com.bloomee.app.ui.screens.OnboardingScreen
import com.bloomee.app.ui.screens.SettingsScreen
import java.time.LocalDate

private enum class Destination(val label: String, val icon: ImageVector) {
    HOME("Bugün", Icons.Default.Favorite),
    CALENDAR("Takvim", Icons.Default.CalendarMonth),
    HYDRATION("Su", Icons.Default.WaterDrop),
    NUTRITION("Kalori", Icons.Default.Restaurant),
    INSIGHTS("İçgörüler", Icons.Default.Insights),
    SETTINGS("Ayarlar", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BloomeeApp(viewModel: BloomeeViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val assistantBusy by viewModel.assistantBusy.collectAsStateWithLifecycle()
    val toast by viewModel.toast.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var destination by remember { mutableStateOf(Destination.HOME) }
    var editingLog by remember { mutableStateOf<DailyLog?>(null) }
    var assistantVisible by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.importBackup(it, replace = false) } }

    LaunchedEffect(toast) {
        toast?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeToast()
        }
    }

    if (!state.loading && !state.profile.onboardingCompleted) {
        OnboardingScreen(
            themeKey = state.profile.themeName,
            onSelectTheme = { key ->
                viewModel.updateProfile { it.copy(themeName = key) }
            },
            remindersEnabled = state.profile.reminderHydrationEnabled ||
                state.profile.reminderPeriodEnabled,
            onRemindersChange = { enabled ->
                viewModel.updateProfile {
                    it.copy(
                        reminderHydrationEnabled = enabled,
                        reminderPeriodEnabled = enabled
                    )
                }
            },
            onFinish = { transform, lastPeriodStart, periodLength ->
                viewModel.updateProfile(transform)
                lastPeriodStart?.let { start ->
                    repeat(periodLength) { offset ->
                        val date = start.plusDays(offset.toLong())
                        if (!date.isAfter(LocalDate.now())) {
                            viewModel.saveLog(DailyLog(date = date, flow = FlowLevel.MEDIUM))
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (destination == Destination.HOME && state.profile.displayName.isNotBlank()) {
                            "Merhaba ${state.profile.displayName}"
                        } else {
                            destination.label
                        }
                    )
                }
            )
        },
        bottomBar = {
            NavigationBar {
                Destination.entries.forEach { item ->
                    NavigationBarItem(
                        selected = destination == item,
                        onClick = { destination = item },
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(item.label) }
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        val contentModifier = Modifier.fillMaxSize().padding(innerPadding)
        when (destination) {
            Destination.HOME -> HomeScreen(
                state = state,
                onLogToday = { editingLog = viewModel.logFor(LocalDate.now()) },
                onAddWater = viewModel::addWater,
                onOpenAssistant = { assistantVisible = true },
                onOpenNutrition = { destination = Destination.NUTRITION },
                modifier = contentModifier
            )

            Destination.CALENDAR -> CalendarScreen(
                state = state,
                onSelectDate = { date -> editingLog = viewModel.logFor(date) },
                modifier = contentModifier
            )

            Destination.HYDRATION -> HydrationScreen(
                state = state,
                onAddWater = viewModel::addWater,
                onReset = viewModel::resetWater,
                modifier = contentModifier
            )

            Destination.NUTRITION -> NutritionScreen(
                state = state,
                onAddEntry = viewModel::addNutritionEntry,
                onRemoveEntry = viewModel::removeNutritionEntry,
                modifier = contentModifier
            )

            Destination.INSIGHTS -> InsightsScreen(
                state = state,
                onShareReport = { text ->
                    val share = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, text)
                        putExtra(Intent.EXTRA_SUBJECT, "Bloomee hekim özeti")
                    }
                    context.startActivity(Intent.createChooser(share, "Raporu paylaş"))
                },
                modifier = contentModifier
            )

            Destination.SETTINGS -> SettingsScreen(
                state = state,
                onUpdateProfile = viewModel::updateProfile,
                onSyncNow = viewModel::syncNow,
                onExportBackup = {
                    viewModel.exportBackup { file ->
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )
                        val share = Intent(Intent.ACTION_SEND).apply {
                            type = "application/json"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(share, "Yedeği paylaş"))
                    }
                },
                onImportBackup = { importLauncher.launch(arrayOf("application/json", "text/plain")) },
                modifier = contentModifier
            )
        }
    }

    editingLog?.let { log ->
        ModalBottomSheet(
            onDismissRequest = { editingLog = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            DailyLogSheet(
                initial = log,
                onSave = {
                    viewModel.saveLog(it)
                    editingLog = null
                },
                onMarkPeriod = { saved, totalDays ->
                    viewModel.saveLog(saved)
                    viewModel.markPeriodRange(
                        saved.date.plusDays(1),
                        saved.date.plusDays(totalDays - 1L),
                        saved.flow
                    )
                    editingLog = null
                },
                onDismiss = { editingLog = null }
            )
        }
    }

    if (assistantVisible) {
        ModalBottomSheet(onDismissRequest = { assistantVisible = false }) {
            AssistantSheet(
                messages = messages,
                busy = assistantBusy,
                hasApiKey = state.profile.assistantApiKey.isNotBlank(),
                onSend = viewModel::askAssistant
            )
        }
    }
}
