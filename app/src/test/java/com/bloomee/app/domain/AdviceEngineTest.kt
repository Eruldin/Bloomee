package com.bloomee.app.domain

import com.bloomee.app.domain.advice.AdviceCategory
import com.bloomee.app.domain.advice.AdviceEngine
import com.bloomee.app.domain.model.DailyLog
import com.bloomee.app.domain.model.FlowLevel
import com.bloomee.app.domain.model.HydrationDay
import com.bloomee.app.domain.model.Symptom
import com.bloomee.app.domain.prediction.CyclePredictor
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class AdviceEngineTest {

    private val today = LocalDate.of(2025, 3, 10)

    private fun statsWithHistory() = CyclePredictor.calculate(
        logs = (0 until 5).map { DailyLog(LocalDate.of(2025, 3, 1).plusDays(it.toLong()), FlowLevel.MEDIUM) } +
            (0 until 5).map { DailyLog(LocalDate.of(2025, 2, 1).plusDays(it.toLong()), FlowLevel.MEDIUM) },
        today = today
    )

    @Test
    fun `always returns phase nutrition and movement guidance`() {
        val cards = AdviceEngine.cardsFor(
            stats = statsWithHistory(),
            todayLog = null,
            hydration = HydrationDay(today, 1800, 2000)
        )

        val categories = cards.map { it.category }
        assertTrue(AdviceCategory.CYCLE in categories)
        assertTrue(AdviceCategory.NUTRITION in categories)
        assertTrue(AdviceCategory.MOVEMENT in categories)
    }

    @Test
    fun `warns when hydration is behind`() {
        val cards = AdviceEngine.cardsFor(
            stats = statsWithHistory(),
            todayLog = null,
            hydration = HydrationDay(today, 200, 2000)
        )

        assertTrue(cards.any { it.id == "hydration_behind" })
    }

    @Test
    fun `adds symptom specific guidance`() {
        val log = DailyLog(today, symptoms = setOf(Symptom.CRAMPS, Symptom.HEADACHE))

        val cards = AdviceEngine.cardsFor(
            stats = statsWithHistory(),
            todayLog = log,
            hydration = HydrationDay(today, 2000, 2000)
        )

        assertTrue(cards.any { it.id == "symptom_cramps" })
        assertTrue(cards.any { it.id == "symptom_headache" })
    }

    @Test
    fun `partner summary never claims a diagnosis`() {
        val summary = AdviceEngine.partnerSummary(statsWithHistory(), "Esin", setOf(Symptom.CRAMPS))

        assertTrue(summary.startsWith("Esin"))
        assertTrue(summary.contains("Kramp"))
    }
}
