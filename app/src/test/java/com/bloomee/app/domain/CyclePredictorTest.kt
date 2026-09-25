package com.bloomee.app.domain

import com.bloomee.app.domain.model.CyclePhase
import com.bloomee.app.domain.model.DailyLog
import com.bloomee.app.domain.model.FlowLevel
import com.bloomee.app.domain.prediction.CyclePredictor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CyclePredictorTest {

    private fun period(start: LocalDate, days: Int = 5): List<DailyLog> =
        (0 until days).map { DailyLog(date = start.plusDays(it.toLong()), flow = FlowLevel.MEDIUM) }

    @Test
    fun `groups consecutive bleeding days into a single period`() {
        val logs = period(LocalDate.of(2025, 1, 1), days = 5)

        val periods = CyclePredictor.detectPeriods(logs)

        assertEquals(1, periods.size)
        assertEquals(5, periods.first().size)
    }

    @Test
    fun `tolerates a one day gap inside the same period`() {
        val logs = listOf(
            DailyLog(LocalDate.of(2025, 1, 1), FlowLevel.MEDIUM),
            DailyLog(LocalDate.of(2025, 1, 2), FlowLevel.MEDIUM),
            DailyLog(LocalDate.of(2025, 1, 4), FlowLevel.SPOTTING)
        )

        assertEquals(1, CyclePredictor.detectPeriods(logs).size)
    }

    @Test
    fun `splits separate cycles`() {
        val logs = period(LocalDate.of(2025, 1, 1)) + period(LocalDate.of(2025, 1, 29))

        assertEquals(2, CyclePredictor.detectPeriods(logs).size)
    }

    @Test
    fun `predicts next period from the average of recorded cycles`() {
        val logs = period(LocalDate.of(2025, 1, 1)) +
            period(LocalDate.of(2025, 1, 29)) +
            period(LocalDate.of(2025, 2, 26))

        val stats = CyclePredictor.calculate(logs, today = LocalDate.of(2025, 3, 10))

        assertEquals(28, stats.averageCycleLength)
        assertEquals(LocalDate.of(2025, 3, 26), stats.predictedNextPeriodStart)
        assertEquals(16, stats.daysToNextPeriod)
        assertTrue(stats.hasEnoughData)
    }

    @Test
    fun `reports menstrual phase while bleeding`() {
        val logs = period(LocalDate.of(2025, 3, 1)) + period(LocalDate.of(2025, 3, 29))

        val stats = CyclePredictor.calculate(logs, today = LocalDate.of(2025, 3, 30))

        assertEquals(CyclePhase.MENSTRUAL, stats.phase)
        assertEquals(2, stats.cycleDay)
    }

    @Test
    fun `flags irregular cycles when variation is high`() {
        val logs = period(LocalDate.of(2025, 1, 1)) +
            period(LocalDate.of(2025, 1, 22)) +
            period(LocalDate.of(2025, 3, 5)) +
            period(LocalDate.of(2025, 3, 30))

        val stats = CyclePredictor.calculate(logs, today = LocalDate.of(2025, 4, 10))

        assertTrue(stats.isIrregular)
        assertTrue(stats.cycleLengthVariation > 7.0)
    }

    @Test
    fun `marks a period as late once the prediction is overdue`() {
        val logs = period(LocalDate.of(2025, 1, 1)) + period(LocalDate.of(2025, 1, 29))

        val stats = CyclePredictor.calculate(logs, today = LocalDate.of(2025, 3, 5))

        assertTrue(stats.isLate)
        assertTrue((stats.daysToNextPeriod ?: 0) < 0)
    }

    @Test
    fun `falls back to defaults without any data`() {
        val stats = CyclePredictor.calculate(emptyList(), today = LocalDate.of(2025, 3, 5))

        assertFalse(stats.hasEnoughData)
        assertEquals(CyclePhase.UNKNOWN, stats.phase)
        assertEquals(28, stats.averageCycleLength)
    }

    @Test
    fun `derives a fertile window around the luteal anchor`() {
        val logs = period(LocalDate.of(2025, 1, 1)) +
            period(LocalDate.of(2025, 1, 29)) +
            period(LocalDate.of(2025, 2, 26))

        val stats = CyclePredictor.calculate(logs, today = LocalDate.of(2025, 3, 10))
        val window = stats.fertileWindow

        assertNotNull(window)
        assertTrue(window!!.contains(LocalDate.of(2025, 3, 12)))
    }

    @Test
    fun `predicted period days cover upcoming cycles`() {
        val logs = period(LocalDate.of(2025, 1, 1)) + period(LocalDate.of(2025, 1, 29))
        val stats = CyclePredictor.calculate(logs, today = LocalDate.of(2025, 2, 10))

        val predicted = CyclePredictor.predictedPeriodDays(stats, cyclesAhead = 2)

        assertTrue(predicted.contains(LocalDate.of(2025, 2, 26)))
        assertTrue(predicted.isNotEmpty())
    }
}
