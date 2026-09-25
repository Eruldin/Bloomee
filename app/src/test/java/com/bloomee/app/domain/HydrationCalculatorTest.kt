package com.bloomee.app.domain

import com.bloomee.app.domain.hydration.HydrationCalculator
import com.bloomee.app.domain.model.ActivityLevel
import com.bloomee.app.domain.model.CyclePhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HydrationCalculatorTest {

    @Test
    fun `uses the default target when weight is unknown`() {
        assertEquals(2000, HydrationCalculator.dailyGoalMl(null))
    }

    @Test
    fun `scales with body weight`() {
        val light = HydrationCalculator.dailyGoalMl(50.0, ActivityLevel.LOW, CyclePhase.FOLLICULAR)
        val heavy = HydrationCalculator.dailyGoalMl(80.0, ActivityLevel.LOW, CyclePhase.FOLLICULAR)

        assertTrue(heavy > light)
    }

    @Test
    fun `activity increases the target`() {
        val moderate = HydrationCalculator.dailyGoalMl(60.0, ActivityLevel.MODERATE, CyclePhase.FOLLICULAR)
        val high = HydrationCalculator.dailyGoalMl(60.0, ActivityLevel.HIGH, CyclePhase.FOLLICULAR)

        assertTrue(high > moderate)
    }

    @Test
    fun `menstrual phase adds a small amount`() {
        val follicular = HydrationCalculator.dailyGoalMl(60.0, ActivityLevel.MODERATE, CyclePhase.FOLLICULAR)
        val menstrual = HydrationCalculator.dailyGoalMl(60.0, ActivityLevel.MODERATE, CyclePhase.MENSTRUAL)

        assertTrue(menstrual > follicular)
    }

    @Test
    fun `stays inside safe bounds and rounds to fifty`() {
        val tiny = HydrationCalculator.dailyGoalMl(30.0, ActivityLevel.LOW, CyclePhase.FOLLICULAR)
        val huge = HydrationCalculator.dailyGoalMl(200.0, ActivityLevel.HIGH, CyclePhase.MENSTRUAL)

        assertTrue(tiny >= 1500)
        assertTrue(huge <= 4000)
        assertEquals(0, tiny % 50)
        assertEquals(0, huge % 50)
    }
}
