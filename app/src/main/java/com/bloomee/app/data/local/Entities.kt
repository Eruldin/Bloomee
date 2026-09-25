package com.bloomee.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.bloomee.app.domain.model.DailyLog
import com.bloomee.app.domain.model.FlowLevel
import com.bloomee.app.domain.model.HydrationDay
import com.bloomee.app.domain.model.Mood
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
    val updatedAt: Long = System.currentTimeMillis()
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
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): HydrationDay = HydrationDay(
        date = LocalDate.parse(date),
        consumedMl = consumedMl,
        goalMl = goalMl
    )
}
