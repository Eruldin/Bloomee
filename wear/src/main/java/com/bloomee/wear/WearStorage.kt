package com.bloomee.wear

import android.content.Context
import android.content.SharedPreferences
import com.bloomee.app.domain.model.DailyLog
import com.bloomee.app.domain.model.FlowLevel
import java.time.LocalDate

/** Tiny preference store for the watch. Entries are glanceable, so only totals persist. */
class WearStorage(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("bloomee_wear", Context.MODE_PRIVATE)

    var themeName: String
        get() = prefs.getString(KEY_THEME, "rose") ?: "rose"
        set(value) = prefs.edit().putString(KEY_THEME, value).apply()

    fun waterMl(date: LocalDate): Int = prefs.getInt("$KEY_WATER$date", 0)

    fun addWater(date: LocalDate, ml: Int) {
        prefs.edit().putInt("$KEY_WATER$date", waterMl(date) + ml).apply()
    }

    fun resetWater(date: LocalDate) {
        prefs.edit().remove("$KEY_WATER$date").apply()
    }

    fun calories(date: LocalDate): Int = prefs.getInt("$KEY_KCAL$date", 0)

    fun addCalories(date: LocalDate, kcal: Int) {
        prefs.edit().putInt("$KEY_KCAL$date", calories(date) + kcal).apply()
    }

    fun logs(): List<DailyLog> =
        prefs.getStringSet(KEY_LOGS, emptySet()).orEmpty().mapNotNull { entry ->
            val parts = entry.split("|")
            val date = runCatching { LocalDate.parse(parts[0]) }.getOrNull() ?: return@mapNotNull null
            DailyLog(date = date, flow = FlowLevel.fromName(parts.getOrNull(1)))
        }.sortedBy { it.date }

    fun setFlow(date: LocalDate, flow: FlowLevel) {
        val entries = prefs.getStringSet(KEY_LOGS, emptySet()).orEmpty()
            .filterNot { it.startsWith("$date|") }
            .toMutableSet()
        if (flow != FlowLevel.NONE) entries += "$date|${flow.name}"
        prefs.edit().putStringSet(KEY_LOGS, entries).apply()
    }

    fun flowOn(date: LocalDate): FlowLevel =
        logs().firstOrNull { it.date == date }?.flow ?: FlowLevel.NONE

    private companion object {
        const val KEY_THEME = "theme_name"
        const val KEY_LOGS = "flow_logs"
        const val KEY_WATER = "water_ml_"
        const val KEY_KCAL = "kcal_"
    }
}
