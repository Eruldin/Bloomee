package com.bloomee.app.domain.model

import java.time.LocalDate

enum class FlowLevel(val label: String, val weight: Int) {
    NONE("Yok", 0),
    SPOTTING("Lekelenme", 1),
    LIGHT("Hafif", 2),
    MEDIUM("Orta", 3),
    HEAVY("Yoğun", 4);

    val isBleeding: Boolean get() = this != NONE

    companion object {
        fun fromName(value: String?): FlowLevel =
            entries.firstOrNull { it.name == value } ?: NONE
    }
}

enum class Mood(val label: String) {
    HAPPY("Mutlu"),
    CALM("Sakin"),
    SENSITIVE("Hassas"),
    IRRITABLE("Sinirli"),
    SAD("Üzgün"),
    ANXIOUS("Kaygılı"),
    ENERGETIC("Enerjik"),
    TIRED("Yorgun");

    companion object {
        fun fromName(value: String?): Mood? = entries.firstOrNull { it.name == value }
    }
}

enum class Symptom(val label: String) {
    CRAMPS("Kramp"),
    HEADACHE("Baş ağrısı"),
    BLOATING("Şişkinlik"),
    BACK_PAIN("Bel ağrısı"),
    BREAST_TENDERNESS("Göğüs hassasiyeti"),
    ACNE("Akne"),
    NAUSEA("Bulantı"),
    CRAVINGS("Tatlı krizi"),
    INSOMNIA("Uykusuzluk"),
    DIZZINESS("Baş dönmesi");

    companion object {
        fun fromName(value: String?): Symptom? = entries.firstOrNull { it.name == value }
    }
}

enum class CyclePhase(val label: String) {
    MENSTRUAL("Regl dönemi"),
    FOLLICULAR("Foliküler faz"),
    OVULATION("Yumurtlama"),
    LUTEAL("Luteal faz (PMS)"),
    UNKNOWN("Henüz veri yok")
}

enum class FertilityLevel(val label: String) {
    LOW("Düşük"),
    MEDIUM("Orta"),
    HIGH("Yüksek"),
    PEAK("En yüksek")
}

enum class ActivityLevel(val label: String, val hydrationFactor: Double) {
    LOW("Hareketsiz", 1.0),
    MODERATE("Orta", 1.12),
    HIGH("Aktif", 1.25)
}

data class DailyLog(
    val date: LocalDate,
    val flow: FlowLevel = FlowLevel.NONE,
    val mood: Mood? = null,
    val symptoms: Set<Symptom> = emptySet(),
    val painLevel: Int = 0,
    val sleepHours: Double? = null,
    val weightKg: Double? = null,
    val note: String = ""
) {
    val isEmpty: Boolean
        get() = flow == FlowLevel.NONE && mood == null && symptoms.isEmpty() &&
            painLevel == 0 && sleepHours == null && weightKg == null && note.isBlank()
}

data class CycleStats(
    val cycleDay: Int,
    val phase: CyclePhase,
    val phaseProgress: Float,
    val daysToNextPeriod: Int?,
    val predictedNextPeriodStart: LocalDate?,
    val fertility: FertilityLevel,
    val fertileWindow: ClosedRange<LocalDate>?,
    val averageCycleLength: Int,
    val averagePeriodLength: Int,
    val cycleLengthVariation: Double,
    val isIrregular: Boolean,
    val isLate: Boolean,
    val recordedCycles: Int,
    val hasEnoughData: Boolean
)

data class HydrationDay(
    val date: LocalDate,
    val consumedMl: Int,
    val goalMl: Int
) {
    val progress: Float
        get() = if (goalMl <= 0) 0f else (consumedMl.toFloat() / goalMl).coerceIn(0f, 1f)
}

data class UserProfile(
    val displayName: String = "",
    val birthYear: Int? = null,
    val weightKg: Double? = null,
    val activityLevel: ActivityLevel = ActivityLevel.MODERATE,
    val defaultCycleLength: Int = 28,
    val defaultPeriodLength: Int = 5,
    val hydrationGoalMl: Int = 2000,
    val partnerModeEnabled: Boolean = false,
    val partnerName: String = "",
    val reminderHydrationEnabled: Boolean = true,
    val reminderPeriodEnabled: Boolean = true,
    val reminderMedicationEnabled: Boolean = false,
    val medicationReminderHour: Int = 21,
    val cloudSyncEnabled: Boolean = false,
    val assistantApiKey: String = "",
    val onboardingCompleted: Boolean = false
)
