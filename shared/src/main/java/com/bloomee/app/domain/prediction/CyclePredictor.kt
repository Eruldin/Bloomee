package com.bloomee.app.domain.prediction

import com.bloomee.app.domain.model.CyclePhase
import com.bloomee.app.domain.model.CycleStats
import com.bloomee.app.domain.model.DailyLog
import com.bloomee.app.domain.model.FertilityLevel
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Luteal-anchored cycle model.
 *
 * Ovulation is estimated a fixed number of days before the next expected period rather than
 * from the start of the current one, which keeps predictions stable when cycle length varies.
 * Recent cycles are weighted more heavily than older ones, and the fertile window widens with
 * the observed standard deviation.
 */
object CyclePredictor {

    private const val LUTEAL_PHASE_LENGTH = 14
    private const val MAX_CYCLES_CONSIDERED = 6
    private const val PERIOD_GAP_TOLERANCE_DAYS = 2L
    private const val IRREGULAR_STD_DEV_THRESHOLD = 7.0
    private const val LATE_TOLERANCE_DAYS = 2

    fun calculate(
        logs: List<DailyLog>,
        today: LocalDate = LocalDate.now(),
        defaultCycleLength: Int = 28,
        defaultPeriodLength: Int = 5
    ): CycleStats {
        val periods = detectPeriods(logs)
        val cycleLengths = periods.zipWithNext { current, next ->
            ChronoUnit.DAYS.between(current.first(), next.first())
        }.filter { it in 15..60 }

        val recentCycles = cycleLengths.takeLast(MAX_CYCLES_CONSIDERED)
        val averageCycleLength = weightedAverage(recentCycles)?.roundToInt() ?: defaultCycleLength
        val averagePeriodLength = periods
            .takeLast(MAX_CYCLES_CONSIDERED)
            .map { ChronoUnit.DAYS.between(it.first(), it.last()) + 1 }
            .takeIf { it.isNotEmpty() }
            ?.average()
            ?.roundToInt()
            ?: defaultPeriodLength

        val stdDev = standardDeviation(recentCycles)
        val lastPeriodStart = periods.lastOrNull()?.first()

        if (lastPeriodStart == null) {
            return CycleStats(
                cycleDay = 0,
                phase = CyclePhase.UNKNOWN,
                phaseProgress = 0f,
                daysToNextPeriod = null,
                predictedNextPeriodStart = null,
                fertility = FertilityLevel.UNKNOWN,
                fertileWindow = null,
                averageCycleLength = averageCycleLength,
                averagePeriodLength = averagePeriodLength,
                cycleLengthVariation = stdDev,
                isIrregular = false,
                isLate = false,
                recordedCycles = cycleLengths.size,
                hasEnoughData = false
            )
        }

        val daysSinceStart = ChronoUnit.DAYS.between(lastPeriodStart, today).toInt()
        val cycleDay = (daysSinceStart + 1).coerceAtLeast(1)
        val predictedNextStart = lastPeriodStart.plusDays(averageCycleLength.toLong())
        val daysToNextPeriod = ChronoUnit.DAYS.between(today, predictedNextStart).toInt()
        val hasCompletedCycle = cycleLengths.isNotEmpty()

        val ovulationDay = predictedNextStart.minusDays(LUTEAL_PHASE_LENGTH.toLong())
        val padding = if (stdDev > 3) stdDev.roundToInt().coerceAtMost(4).toLong() else 0L
        // A fertile window needs at least one completed cycle to be non-speculative.
        val fertileWindow = if (hasCompletedCycle) {
            ovulationDay.minusDays(5 + padding)..ovulationDay.plusDays(1 + padding)
        } else {
            null
        }

        val currentPeriod = periods.last()
        val stillBleeding = currentPeriod.any { !it.isBefore(today.minusDays(1)) }
        val phase = when {
            daysSinceStart < averagePeriodLength || (stillBleeding && daysSinceStart < averagePeriodLength + 2) ->
                CyclePhase.MENSTRUAL
            today == ovulationDay -> CyclePhase.OVULATION
            today.isBefore(ovulationDay) -> CyclePhase.FOLLICULAR
            else -> CyclePhase.LUTEAL
        }

        val fertility = when {
            !hasCompletedCycle -> FertilityLevel.UNKNOWN
            today == ovulationDay -> FertilityLevel.PEAK
            fertileWindow?.contains(today) == true -> FertilityLevel.HIGH
            abs(ChronoUnit.DAYS.between(today, ovulationDay)) <= 8 -> FertilityLevel.MEDIUM
            else -> FertilityLevel.LOW
        }

        return CycleStats(
            cycleDay = cycleDay,
            phase = phase,
            phaseProgress = (cycleDay.toFloat() / averageCycleLength).coerceIn(0f, 1f),
            daysToNextPeriod = daysToNextPeriod,
            predictedNextPeriodStart = predictedNextStart,
            fertility = fertility,
            fertileWindow = fertileWindow,
            averageCycleLength = averageCycleLength,
            averagePeriodLength = averagePeriodLength,
            cycleLengthVariation = stdDev,
            isIrregular = stdDev >= IRREGULAR_STD_DEV_THRESHOLD,
            // "Late" is only meaningful once a real cycle length exists; with a single
            // logged period the difference to the default isn't evidence of lateness.
            isLate = hasCompletedCycle && daysToNextPeriod < -LATE_TOLERANCE_DAYS,
            recordedCycles = cycleLengths.size,
            hasEnoughData = cycleLengths.isNotEmpty()
        )
    }

    /** Groups bleeding days into periods, tolerating short gaps inside one period. */
    fun detectPeriods(logs: List<DailyLog>): List<List<LocalDate>> {
        val bleedingDays = logs.filter { it.flow.isBleeding }.map { it.date }.distinct().sorted()
        val periods = mutableListOf<MutableList<LocalDate>>()
        for (day in bleedingDays) {
            val currentPeriod = periods.lastOrNull()
            if (currentPeriod != null &&
                ChronoUnit.DAYS.between(currentPeriod.last(), day) <= PERIOD_GAP_TOLERANCE_DAYS + 1
            ) {
                currentPeriod.add(day)
            } else {
                periods.add(mutableListOf(day))
            }
        }
        return periods
    }

    fun predictedPeriodDays(stats: CycleStats, cyclesAhead: Int = 3): Set<LocalDate> {
        val firstStart = stats.predictedNextPeriodStart ?: return emptySet()
        val result = mutableSetOf<LocalDate>()
        repeat(cyclesAhead) { cycleIndex ->
            val start = firstStart.plusDays((stats.averageCycleLength.toLong()) * cycleIndex)
            repeat(stats.averagePeriodLength) { dayIndex ->
                result += start.plusDays(dayIndex.toLong())
            }
        }
        return result
    }

    private fun weightedAverage(values: List<Long>): Double? {
        if (values.isEmpty()) return null
        var weightedSum = 0.0
        var weightTotal = 0.0
        values.forEachIndexed { index, value ->
            val weight = (index + 1).toDouble()
            weightedSum += value * weight
            weightTotal += weight
        }
        return weightedSum / weightTotal
    }

    private fun standardDeviation(values: List<Long>): Double {
        if (values.size < 2) return 0.0
        val mean = values.average()
        val variance = values.sumOf { (it - mean) * (it - mean) } / values.size
        return sqrt(variance)
    }
}
