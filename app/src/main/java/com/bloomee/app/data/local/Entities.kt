package com.bloomee.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.bloomee.app.domain.model.DailyLog
import com.bloomee.app.domain.model.FlowLevel
import com.bloomee.app.domain.model.HydrationDay
import com.bloomee.app.domain.model.Meal
import com.bloomee.app.domain.model.Mood
import com.bloomee.app.domain.model.NutritionEntry
import com.bloomee.app.domain.model.Symptom
import java.time.LocalDate

@Entity(tableName = "daily_logs")
data class DailyLogEntity(
    @PrimaryKey val date: String,
    val flow: String,
    val mood: String?,
    val symptoms: String,
    val painLevel: Int,
    val sleepHours: Double?,
    val weightKg: Double?,
    val note: String,
    val updatedAt: Long = System.currentTimeMillis(),
    // Soft-delete marker: null = live row, timestamp = tombstone. Tombstones let
    // cloud sync propagate deletions instead of resurrecting records on merge.
    val deletedAt: Long? = null
) {
    fun toDomain(): DailyLog = DailyLog(
        date = LocalDate.parse(date),
        flow = FlowLevel.fromName(flow),
        mood = Mood.fromName(mood),
        symptoms = symptoms.split(",")
            .mapNotNull { Symptom.fromName(it.trim().takeIf(String::isNotEmpty)) }
            .toSet(),
        painLevel = painLevel,
        sleepHours = sleepHours,
        weightKg = weightKg,
        note = note
    )

    companion object {
        fun fromDomain(log: DailyLog) = DailyLogEntity(
            date = log.date.toString(),
            flow = log.flow.name,
            mood = log.mood?.name,
            symptoms = log.symptoms.joinToString(",") { it.name },
            painLevel = log.painLevel,
            sleepHours = log.sleepHours,
            weightKg = log.weightKg,
            note = log.note
        )
    }
}

@Entity(tableName = "hydration_days")
data class HydrationDayEntity(
    @PrimaryKey val date: String,
    val consumedMl: Int,
    val goalMl: Int,
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
) {
    fun toDomain(): HydrationDay = HydrationDay(
        date = LocalDate.parse(date),
        consumedMl = consumedMl,
        goalMl = goalMl
    )
}

// A single logged food item. The id is a client-generated UUID rather than an
// autoincrement so entries stay mergeable when cloud sync is enabled.
@Entity(tableName = "nutrition_entries")
data class NutritionEntryEntity(
    @PrimaryKey val id: String,
    val date: String,
    val meal: String,
    val name: String,
    val kcal: Int,
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
) {
    fun toDomain(): NutritionEntry = NutritionEntry(
        id = id,
        date = LocalDate.parse(date),
        meal = Meal.fromName(meal),
        name = name,
        kcal = kcal
    )

    companion object {
        fun fromDomain(entry: NutritionEntry) = NutritionEntryEntity(
            id = entry.id,
            date = entry.date.toString(),
            meal = entry.meal.name,
            name = entry.name,
            kcal = entry.kcal
        )
    }
}
