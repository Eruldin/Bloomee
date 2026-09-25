package com.bloomee.app.domain

import com.bloomee.app.domain.model.DailyLog
import com.bloomee.app.domain.model.FlowLevel
import com.bloomee.app.domain.model.HydrationDay
import com.bloomee.app.domain.model.Meal
import com.bloomee.app.domain.model.Mood
import com.bloomee.app.domain.model.NutritionEntry
import com.bloomee.app.domain.model.Symptom
import com.bloomee.app.domain.report.DoctorReport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DoctorReportTest {

    private val today = LocalDate.of(2026, 9, 25)

    private fun sampleLogs(): List<DailyLog> = buildList {
        // Two periods 30 days apart, then one 28 days later.
        for (d in 0..4) {
            add(DailyLog(LocalDate.of(2026, 6, 1).plusDays(d.toLong()), flow = FlowLevel.MEDIUM))
        }
        for (d in 0..4) {
            add(DailyLog(LocalDate.of(2026, 7, 1).plusDays(d.toLong()), flow = FlowLevel.LIGHT))
        }
        for (d in 0..3) {
            add(DailyLog(LocalDate.of(2026, 7, 29).plusDays(d.toLong()), flow = FlowLevel.HEAVY))
        }
        add(
            DailyLog(
                date = LocalDate.of(2026, 7, 10),
                mood = Mood.TIRED,
                symptoms = setOf(Symptom.CRAMPS, Symptom.HEADACHE),
                painLevel = 6,
                sleepHours = 7.5,
                weightKg = 62.0,
                note = "özel not — rapora girmemeli"
            )
        )
        add(
            DailyLog(
                date = LocalDate.of(2026, 7, 11),
                mood = Mood.TIRED,
                symptoms = setOf(Symptom.CRAMPS),
                painLevel = 4,
                sleepHours = 6.5
            )
        )
    }

    private fun sampleHydration() = listOf(
        HydrationDay(LocalDate.of(2026, 7, 10), 2000, 2000),
        HydrationDay(LocalDate.of(2026, 7, 11), 1500, 2000)
    )

    private fun sampleNutrition() = listOf(
        NutritionEntry(id = "1", date = LocalDate.of(2026, 7, 10), meal = Meal.LUNCH, name = "a", kcal = 800),
        NutritionEntry(id = "2", date = LocalDate.of(2026, 7, 10), meal = Meal.DINNER, name = "b", kcal = 1000),
        NutritionEntry(id = "3", date = LocalDate.of(2026, 7, 11), meal = Meal.LUNCH, name = "c", kcal = 2000)
    )

    @Test
    fun `report contains cycle stats and per-section data for all sections`() {
        val report = DoctorReport.build(
            logs = sampleLogs(),
            hydrationDays = sampleHydration(),
            nutrition = sampleNutrition(),
            options = DoctorReport.Options(),
            generatedAt = today
        )

        assertTrue(report.contains("Kayıtlı regl dönemleri: 3"))
        assertTrue(report.contains("30 gün") && report.contains("28 gün"))
        assertTrue(report.contains("Ortalama döngü: 29 gün"))
        assertTrue(report.contains("Kramp: 2 gün"))
        assertTrue(report.contains("Baş ağrısı: 1 gün"))
        assertTrue(report.contains("Yorgun: 2 gün"))
        assertTrue(report.contains("2 gün kaydedildi, ortalama 5,0/10"))
        assertTrue(report.contains("Ortalama uyku: 7,0 saat"))
        assertTrue(report.contains("Ortalama su: 1750 ml/gün"))
        assertTrue(report.contains("hedefe ulaşılan gün: 1/2"))
        assertTrue(report.contains("Ortalama kalori: 1900 kcal/gün"))
        assertFalse(report.contains("özel not"))
    }

    @Test
    fun `range filter excludes older logs`() {
        val report = DoctorReport.build(
            logs = sampleLogs(),
            hydrationDays = emptyList(),
            nutrition = emptyList(),
            options = DoctorReport.Options(from = LocalDate.of(2026, 7, 15), to = today),
            generatedAt = today
        )
        assertTrue(report.contains("Kayıtlı regl dönemleri: 1"))
        assertFalse(report.contains("Kramp"))
    }

    @Test
    fun `unselected sections are omitted`() {
        val report = DoctorReport.build(
            logs = sampleLogs(),
            hydrationDays = sampleHydration(),
            nutrition = sampleNutrition(),
            options = DoctorReport.Options(
                sections = setOf(DoctorReport.Section.CYCLES)
            ),
            generatedAt = today
        )
        assertTrue(report.contains("== Döngü özeti =="))
        assertFalse(report.contains("== Belirtiler =="))
        assertFalse(report.contains("== Su ve kalori =="))
    }

    @Test
    fun `empty data produces an explicit no-data report`() {
        val report = DoctorReport.build(
            logs = emptyList(),
            hydrationDays = emptyList(),
            nutrition = emptyList(),
            options = DoctorReport.Options(),
            generatedAt = today
        )
        assertTrue(report.contains("Bu aralıkta regl kaydı yok."))
        assertEquals(5, report.lines().count { it.startsWith("==") })
    }
}
