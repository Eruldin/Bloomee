package com.bloomee.app.domain.nutrition

import com.bloomee.app.domain.model.ActivityLevel
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * Daily calorie target in kcal, estimated with the Mifflin-St Jeor equation for women
 * (BMR = 10·kg + 6.25·cm − 5·age − 161) scaled by activity level.
 *
 * Missing height or age fall back to population averages so the goal still reflects
 * body weight rather than a fixed constant. Without a body weight the general adult
 * reference intake is used and activity is ignored, since there is nothing to scale
 * it against. The result is an estimate, not a diet plan.
 */
object CalorieCalculator {

    private const val DEFAULT_HEIGHT_CM = 165.0
    private const val DEFAULT_AGE = 30
    private const val FALLBACK_GOAL_KCAL = 2000
    private const val MIN_GOAL_KCAL = 1200
    private const val MAX_GOAL_KCAL = 3500

    fun dailyGoalKcal(
        weightKg: Double?,
        heightCm: Int?,
        birthYear: Int?,
        activityLevel: ActivityLevel = ActivityLevel.MODERATE,
        today: LocalDate = LocalDate.now()
    ): Int {
        val weight = weightKg?.takeIf { it > 25 } ?: return FALLBACK_GOAL_KCAL
        val height = heightCm?.takeIf { it in 120..230 }?.toDouble() ?: DEFAULT_HEIGHT_CM
        val age = birthYear
            ?.let { today.year - it }
            ?.takeIf { it in 10..100 }
            ?: DEFAULT_AGE
        val bmr = 10 * weight + 6.25 * height - 5 * age - 161
        val goal = bmr * activityLevel.calorieFactor
        return (goal / 50).roundToInt().times(50).coerceIn(MIN_GOAL_KCAL, MAX_GOAL_KCAL)
    }

    val quickAddOptionsKcal = listOf(150, 300, 500, 700)
}
