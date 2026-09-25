package com.bloomee.app.domain.report

import com.bloomee.app.domain.model.DailyLog
import com.bloomee.app.domain.model.HydrationDay
import com.bloomee.app.domain.model.NutritionEntry
import com.bloomee.app.domain.prediction.CyclePredictor
import java.time.LocalDate
import java.util.Locale
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/**
 * Builds the human-readable doctor summary shared from the Insights tab. Pure Kotlin so the
 * same report can be produced on Android and desktop; unit-tested on the JVM.
 *
 * The report intentionally contains only the sections the user selected and never includes
 * notes, the assistant API key or profile fields — those stay on the device.
 */
object DoctorReport {

    enum class Section(val label: String) {
        CYCLES("Döngü özeti"),
        SYMPTOMS("Belirtiler"),
        MOODS("Ruh halleri"),
        WELLBEING("Ağrı, uyku ve kilo"),
        LIFESTYLE("Su ve kalori")
    }

    data class Options(
        val from: LocalDate? = null,
        val to: LocalDate = LocalDate.now(),
        val sections: Set<Section> = Section.entries.toSet()
    )

    private val tr = Locale.forLanguageTag("tr")
    private fun dec(value: Double) = String.format(tr, "%.1f", value)
    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    fun build(
        logs: List<DailyLog>,
        hydrationDays: List<HydrationDay>,
        nutrition: List<NutritionEntry>,
        options: Options,
        generatedAt: LocalDate = LocalDate.now()
    ): String {
        val from = options.from
        val to = options.to
        fun LocalDate.inRange() = (from == null || !isBefore(from)) && !isAfter(to)

        val rangeLogs = logs.filter { it.date.inRange() }.sortedBy { it.date }
        val text = StringBuilder()
        text.appendLine("Bloomee hekim özeti")
        text.appendLine(
            "Oluşturma: ${generatedAt.format(dateFormatter)} · Aralık: " +
                (from?.format(dateFormatter) ?: "tümü") + " – ${to.format(dateFormatter)}"
        )
        text.appendLine("Kişisel takip kaydının özetidir; tanı aracı değildir.")
        text.appendLine()

        if (Section.CYCLES in options.sections) {
            text.appendLine("== Döngü özeti ==")
            val periods = CyclePredictor.detectPeriods(rangeLogs)
            if (periods.isEmpty()) {
                text.appendLine("Bu aralıkta regl kaydı yok.")
            } else {
                text.appendLine("Kayıtlı regl dönemleri: ${periods.size}")
                periods.forEach { period ->
                    val length = ChronoUnit.DAYS.between(period.first(), period.last()) + 1
                    text.appendLine(
                        "- ${period.first().format(dateFormatter)} – " +
                            "${period.last().format(dateFormatter)} ($length gün)"
                    )
                }
                val cycleLengths = periods.zipWithNext { current, next ->
                    ChronoUnit.DAYS.between(current.first(), next.first()).toInt()
                }.filter { it in 15..60 }
                if (cycleLengths.isNotEmpty()) {
                    text.appendLine(
                        "Döngü uzunlukları: ${cycleLengths.joinToString(", ") { "$it gün" }}"
                    )
                    val mean = cycleLengths.average()
                    val variance = cycleLengths.sumOf { (it - mean) * (it - mean) } / cycleLengths.size
                    text.appendLine(
                        "Ortalama döngü: ${mean.roundToInt()} gün " +
                            "(±${dec(kotlin.math.sqrt(variance))} gün)"
                    )
                }
                val avgPeriod = periods
                    .map { ChronoUnit.DAYS.between(it.first(), it.last()) + 1 }
                    .average()
                text.appendLine("Ortalama regl süresi: ${avgPeriod.roundToInt()} gün")
            }
            text.appendLine()
        }

        if (Section.SYMPTOMS in options.sections) {
            text.appendLine("== Belirtiler ==")
            val counts = rangeLogs.flatMap { it.symptoms }
                .groupingBy { it }.eachCount()
                .entries.sortedByDescending { it.value }
            if (counts.isEmpty()) {
                text.appendLine("Belirti kaydı yok.")
            } else {
                counts.forEach { (symptom, count) ->
                    text.appendLine("- ${symptom.label}: $count gün")
                }
            }
            text.appendLine()
        }

        if (Section.MOODS in options.sections) {
            text.appendLine("== Ruh halleri ==")
            val counts = rangeLogs.mapNotNull { it.mood }
                .groupingBy { it }.eachCount()
                .entries.sortedByDescending { it.value }
            if (counts.isEmpty()) {
                text.appendLine("Ruh hali kaydı yok.")
            } else {
                counts.forEach { (mood, count) ->
                    text.appendLine("- ${mood.label}: $count gün")
                }
            }
            text.appendLine()
        }

        if (Section.WELLBEING in options.sections) {
            text.appendLine("== Ağrı, uyku ve kilo ==")
            val painDays = rangeLogs.filter { it.painLevel > 0 }
            val sleepDays = rangeLogs.mapNotNull { it.sleepHours }
            val weightDays = rangeLogs.mapNotNull { it.weightKg }
            var wroteAny = false
            if (painDays.isNotEmpty()) {
                text.appendLine(
                    "Ağrı: ${painDays.size} gün kaydedildi, " +
                        "ortalama ${dec(painDays.map { it.painLevel }.average())}/10"
                )
                wroteAny = true
            }
            if (sleepDays.isNotEmpty()) {
                text.appendLine(
                    "Ortalama uyku: ${dec(sleepDays.average())} saat " +
                        "(${sleepDays.size} kayıt)"
                )
                wroteAny = true
            }
            if (weightDays.isNotEmpty()) {
                text.appendLine(
                    "Kilo: son ${dec(weightDays.last())} kg " +
                        "(aralık ${dec(weightDays.min())}–${dec(weightDays.max())} kg)"
                )
                wroteAny = true
            }
            if (!wroteAny) text.appendLine("Bu aralıkta kayıt yok.")
            text.appendLine()
        }

        if (Section.LIFESTYLE in options.sections) {
            text.appendLine("== Su ve kalori ==")
            val rangeHydration = hydrationDays.filter { it.date.inRange() }
            val rangeNutrition = nutrition.filter { it.date.inRange() }
            if (rangeHydration.isEmpty() && rangeNutrition.isEmpty()) {
                text.appendLine("Bu aralıkta kayıt yok.")
            } else {
                if (rangeHydration.isNotEmpty()) {
                    val avgMl = rangeHydration.map { it.consumedMl }.average().roundToInt()
                    val goalDays = rangeHydration.count { it.consumedMl >= it.goalMl && it.goalMl > 0 }
                    text.appendLine(
                        "Ortalama su: $avgMl ml/gün · hedefe ulaşılan gün: " +
                            "$goalDays/${rangeHydration.size}"
                    )
                }
                if (rangeNutrition.isNotEmpty()) {
                    val byDay = rangeNutrition.groupBy { it.date }
                    val avgKcal = byDay.values.map { day -> day.sumOf { it.kcal } }
                        .average().roundToInt()
                    text.appendLine("Ortalama kalori: $avgKcal kcal/gün (${byDay.size} kayıtlı gün)")
                }
            }
        }

        return text.toString().trimEnd() + "\n"
    }
}
