package com.bloomee.app.domain.hydration

import com.bloomee.app.domain.model.ActivityLevel
import com.bloomee.app.domain.model.CyclePhase
import kotlin.math.roundToInt

/**
 * Daily water target in millilitres.
 *
 * Baseline is 33 ml per kilogram, adjusted for activity level and for phases where fluid
 * needs rise (bleeding days and the luteal phase, where water retention and cramps are common).
 * Without a body weight the general adult target is used and activity is ignored, since there
 * is nothing to scale it against.
 */
object HydrationCalculator {

    private const val ML_PER_KG = 33.0
    private const val FALLBACK_GOAL_ML = 2000
    private const val MIN_GOAL_ML = 1500
    private const val MAX_GOAL_ML = 4000

    fun dailyGoalMl(
        weightKg: Double?,
        activityLevel: ActivityLevel = ActivityLevel.MODERATE,
        phase: CyclePhase = CyclePhase.UNKNOWN
    ): Int {
        val weight = weightKg?.takeIf { it > 25 }
        val base = weight?.times(ML_PER_KG) ?: FALLBACK_GOAL_ML.toDouble()
        val activityFactor = if (weight == null) 1.0 else activityLevel.hydrationFactor
        val phaseFactor = when (phase) {
            CyclePhase.MENSTRUAL -> 1.10
            CyclePhase.LUTEAL -> 1.07
            else -> 1.0
        }
        val goal = base * activityFactor * phaseFactor
        return (goal / 50).roundToInt().times(50).coerceIn(MIN_GOAL_ML, MAX_GOAL_ML)
    }

    val quickAddOptionsMl = listOf(100, 200, 330, 500)
}
