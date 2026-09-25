package com.bloomee.app.domain

import com.bloomee.app.domain.model.ActivityLevel
import com.bloomee.app.domain.nutrition.CalorieCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CalorieCalculatorTest {

    private val today = LocalDate.of(2026, 9, 25)

    @Test
    fun `uses the reference intake when weight is unknown`() {
        assertEquals(2000, CalorieCalculator.dailyGoalKcal(null, null, null, today = today))
    }

    @Test
    fun `scales with body weight`() {
        val light = CalorieCalculator.dailyGoalKcal(50.0, 165, 1995, ActivityLevel.MODERATE, today)
        val heavy = CalorieCalculator.dailyGoalKcal(80.0, 165, 1995, ActivityLevel.MODERATE, today)

        assertTrue(heavy > light)
    }

    @Test
    fun `activity increases the target`() {
        val moderate = CalorieCalculator.dailyGoalKcal(60.0, 165, 1995, ActivityLevel.MODERATE, today)
        val high = CalorieCalculator.dailyGoalKcal(60.0, 165, 1995, ActivityLevel.HIGH, today)

        assertTrue(high > moderate)
    }

    @Test
    fun `matches mifflin st jeor for a known profile`() {
        // BMR = 10*60 + 6.25*165 - 5*30 - 161 = 1320.25; TDEE = 1320.25 * 1.4 = 1848 -> 1850
        val goal = CalorieCalculator.dailyGoalKcal(60.0, 165, 1996, ActivityLevel.MODERATE, today)

        assertEquals(1850, goal)
    }

    @Test
    fun `missing height and age fall back to averages but keep weight`() {
        val withAverages = CalorieCalculator.dailyGoalKcal(60.0, null, null, ActivityLevel.MODERATE, today)
        val explicit = CalorieCalculator.dailyGoalKcal(60.0, 165, 1996, ActivityLevel.MODERATE, today)

        assertEquals(explicit, withAverages)
    }

    @Test
    fun `stays inside safe bounds and rounds to fifty`() {
        val tiny = CalorieCalculator.dailyGoalKcal(30.0, 120, 2020, ActivityLevel.LOW, today)
        val huge = CalorieCalculator.dailyGoalKcal(200.0, 230, 1940, ActivityLevel.HIGH, today)

        assertTrue(tiny >= 1200)
        assertTrue(huge <= 3500)
        assertEquals(0, tiny % 50)
        assertEquals(0, huge % 50)
    }
}
