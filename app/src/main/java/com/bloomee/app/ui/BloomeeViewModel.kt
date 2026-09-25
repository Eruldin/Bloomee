package com.bloomee.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bloomee.app.BloomeeApplication
import com.bloomee.app.data.sync.SyncState
import com.bloomee.app.domain.advice.AdviceCard
import com.bloomee.app.domain.advice.AdviceEngine
import com.bloomee.app.domain.hydration.HydrationCalculator
import com.bloomee.app.domain.model.CyclePhase
import com.bloomee.app.domain.model.CycleStats
import com.bloomee.app.domain.model.DailyLog
import com.bloomee.app.domain.model.FertilityLevel
import com.bloomee.app.domain.model.HydrationDay
import com.bloomee.app.domain.model.Meal
import com.bloomee.app.domain.model.NutritionDay
import com.bloomee.app.domain.model.NutritionEntry
import com.bloomee.app.domain.model.UserProfile
import com.bloomee.app.domain.nutrition.CalorieCalculator
import com.bloomee.app.domain.prediction.CyclePredictor
import com.bloomee.app.notification.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate

data class BloomeeUiState(
    val profile: UserProfile = UserProfile(),
    val logs: List<DailyLog> = emptyList(),
    val stats: CycleStats = EMPTY_STATS,
    val hydrationToday: HydrationDay = HydrationDay(LocalDate.now(), 0, 2000),
    val hydrationHistory: List<HydrationDay> = emptyList(),
    val nutritionToday: NutritionDay = NutritionDay(LocalDate.now(), emptyList(), 2000),
    val nutritionHistory: List<NutritionEntry> = emptyList(),
    val advice: List<AdviceCard> = emptyList(),
    val predictedPeriodDays: Set<LocalDate> = emptySet(),
    val syncState: SyncState = SyncState.UNCONFIGURED,
    val loading: Boolean = true
) {
    val todayLog: DailyLog? get() = logs.firstOrNull { it.date == LocalDate.now() }

    companion object {
        val EMPTY_STATS = CycleStats(
            cycleDay = 0,
            phase = CyclePhase.UNKNOWN,
            phaseProgress = 0f,
            daysToNextPeriod = null,
            predictedNextPeriodStart = null,
            fertility = FertilityLevel.LOW,
            fertileWindow = null,
            averageCycleLength = 28,
            averagePeriodLength = 5,
            cycleLengthVariation = 0.0,
            isIrregular = false,
            isLate = false,
            recordedCycles = 0,
            hasEnoughData = false
        )
    }
}

data class AssistantMessage(val fromUser: Boolean, val text: String)

class BloomeeViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as BloomeeApplication).container

    private val _messages = MutableStateFlow<List<AssistantMessage>>(emptyList())
    val messages: StateFlow<List<AssistantMessage>> = _messages

    private val _assistantBusy = MutableStateFlow(false)
    val assistantBusy: StateFlow<Boolean> = _assistantBusy

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast

    val uiState: StateFlow<BloomeeUiState> = combine(
        container.userPreferencesRepository.profile,
        container.cycleRepository.logs,
        container.hydrationRepository.days,
        container.nutritionRepository.entries,
        container.cloudSync.state
    ) { profile, logs, hydrationDays, nutritionEntries, syncState ->
        val today = LocalDate.now()
        val stats = CyclePredictor.calculate(
            logs = logs,
            today = today,
            defaultCycleLength = profile.defaultCycleLength,
            defaultPeriodLength = profile.defaultPeriodLength
        )
        val goal = HydrationCalculator.dailyGoalMl(
            weightKg = profile.weightKg,
            activityLevel = profile.activityLevel,
            phase = stats.phase
        )
        val hydrationToday = hydrationDays.firstOrNull { it.date == today }
            ?: HydrationDay(today, 0, goal)
        val todayLog = logs.firstOrNull { it.date == today }
        val calorieGoal = CalorieCalculator.dailyGoalKcal(
            weightKg = profile.weightKg,
            heightCm = profile.heightCm,
            birthYear = profile.birthYear,
            activityLevel = profile.activityLevel,
            today = today
        )

        BloomeeUiState(
            profile = profile,
            logs = logs,
            stats = stats,
            hydrationToday = hydrationToday.copy(goalMl = if (hydrationToday.goalMl > 0) hydrationToday.goalMl else goal),
            hydrationHistory = hydrationDays,
            nutritionToday = NutritionDay(
                date = today,
                entries = nutritionEntries.filter { it.date == today },
                goalKcal = calorieGoal
            ),
            nutritionHistory = nutritionEntries,
            advice = AdviceEngine.cardsFor(stats, todayLog, hydrationToday),
            predictedPeriodDays = CyclePredictor.predictedPeriodDays(stats),
            syncState = syncState,
            loading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BloomeeUiState()
    )

    fun logFor(date: LocalDate): DailyLog =
        uiState.value.logs.firstOrNull { it.date == date } ?: DailyLog(date = date)

    fun saveLog(log: DailyLog) {
        viewModelScope.launch { container.cycleRepository.save(log) }
    }

    fun addWater(amountMl: Int) {
        viewModelScope.launch {
            val state = uiState.value
            container.hydrationRepository.addWater(
                date = LocalDate.now(),
                amountMl = amountMl,
                goalMl = state.hydrationToday.goalMl
            )
        }
    }

    fun resetWater() {
        viewModelScope.launch {
            container.hydrationRepository.reset(LocalDate.now(), uiState.value.hydrationToday.goalMl)
        }
    }

    fun addNutritionEntry(name: String, kcal: Int, meal: Meal) {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || kcal <= 0) return
        viewModelScope.launch {
            container.nutritionRepository.save(
                NutritionEntry(date = LocalDate.now(), meal = meal, name = trimmed, kcal = kcal)
            )
        }
    }

    fun removeNutritionEntry(id: String) {
        viewModelScope.launch { container.nutritionRepository.delete(id) }
    }

    fun updateProfile(transform: (UserProfile) -> UserProfile) {
        viewModelScope.launch {
            container.userPreferencesRepository.update(transform)
            val profile = container.userPreferencesRepository.profile.first()
            ReminderScheduler.schedule(getApplication(), profile)
            container.cloudSync.setEnabled(profile.cloudSyncEnabled)
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            container.cloudSync.syncNow(
                container.cycleRepository,
                container.hydrationRepository,
                container.nutritionRepository
            )
        }
    }

    fun exportBackup(onReady: (File) -> Unit) {
        viewModelScope.launch { onReady(container.backupRepository.exportToCacheFile()) }
    }

    fun importBackup(uri: Uri, replace: Boolean) {
        viewModelScope.launch {
            val result = container.backupRepository.importFrom(uri, replace)
            _toast.value = result.error
                ?: "${result.logCount} günlük, ${result.hydrationCount} su, ${result.nutritionCount} beslenme kaydı geri yüklendi."
        }
    }

    fun askAssistant(question: String) {
        val trimmed = question.trim()
        if (trimmed.isEmpty() || _assistantBusy.value) return
        _messages.value = _messages.value + AssistantMessage(fromUser = true, text = trimmed)
        _assistantBusy.value = true

        viewModelScope.launch {
            val state = uiState.value
            val contextBlock = buildString {
                appendLine("Kullanıcı bağlamı:")
                appendLine("- Döngü günü: ${state.stats.cycleDay}")
                appendLine("- Faz: ${state.stats.phase.label}")
                appendLine("- Ortalama döngü: ${state.stats.averageCycleLength} gün")
                state.stats.daysToNextPeriod?.let { appendLine("- Tahmini regle kalan: $it gün") }
                appendLine("- Bugünkü su: ${state.hydrationToday.consumedMl}/${state.hydrationToday.goalMl} ml")
                appendLine("- Bugünkü kalori: ${state.nutritionToday.consumedKcal}/${state.nutritionToday.goalKcal} kcal")
                state.todayLog?.symptoms?.takeIf { it.isNotEmpty() }?.let { symptoms ->
                    appendLine("- Bugünkü belirtiler: ${symptoms.joinToString { it.label }}")
                }
            }

            val result = container.assistantClient.ask(
                apiKey = state.profile.assistantApiKey,
                contextBlock = contextBlock,
                question = trimmed
            )
            _messages.value = _messages.value + AssistantMessage(
                fromUser = false,
                text = result.getOrElse { it.message ?: "Asistana şu anda ulaşılamıyor." }
            )
            _assistantBusy.value = false
        }
    }

    fun consumeToast() {
        _toast.value = null
    }

    companion object {
        val Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: androidx.lifecycle.viewmodel.CreationExtras
            ): T {
                val application = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!
                return BloomeeViewModel(application) as T
            }
        }
    }
}
