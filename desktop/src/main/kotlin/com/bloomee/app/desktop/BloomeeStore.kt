package com.bloomee.app.desktop

import com.bloomee.app.domain.model.ActivityLevel
import com.bloomee.app.domain.model.DailyLog
import com.bloomee.app.domain.model.FlowLevel
import com.bloomee.app.domain.model.Meal
import com.bloomee.app.domain.model.Mood
import com.bloomee.app.domain.model.NutritionEntry
import com.bloomee.app.domain.model.Symptom
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
import java.util.UUID

/**
 * Desktop data store. The on-disk schema intentionally matches the phone app's
 * `bloomee-yedek.json` backup so a file exported on one device imports on the other.
 * A `profile` section carries desktop-only settings; the phone importer ignores it.
 */
data class DesktopProfile(
    val displayName: String = "",
    val birthYear: Int? = null,
    val weightKg: Double? = null,
    val heightCm: Int? = null,
    val activityLevel: ActivityLevel = ActivityLevel.MODERATE,
    val themeName: String = "rose",
    val darkMode: Boolean? = null
)

class DesktopData {
    val logs = sortedMapOf<LocalDate, DailyLog>()
    val hydrationMl = sortedMapOf<LocalDate, Int>()
    val hydrationGoalMl = sortedMapOf<LocalDate, Int>()
    val nutrition = mutableListOf<NutritionEntry>()
    var profile = DesktopProfile()
}

class BloomeeStore(private val file: File = defaultFile()) {

    fun load(): DesktopData {
        val data = DesktopData()
        if (!file.exists()) return data
        runCatching {
            val root = JSONObject(file.readText())
            data.profile = parseProfile(root.optJSONObject("profile"))
            root.optJSONArray("dailyLogs")?.forEachObject { item ->
                val date = runCatching { LocalDate.parse(item.getString("date")) }.getOrNull()
                    ?: return@forEachObject
                data.logs[date] = DailyLog(
                    date = date,
                    flow = FlowLevel.fromName(item.optString("flow", "NONE")),
                    mood = Mood.fromName(item.optString("mood").takeIf { it.isNotBlank() && it != "null" }),
                    symptoms = item.optString("symptoms")
                        .split(",")
                        .mapNotNull { Symptom.fromName(it.trim().takeIf { t -> t.isNotEmpty() }) }
                        .toSet(),
                    painLevel = item.optInt("painLevel"),
                    sleepHours = item.optDouble("sleepHours").takeIf { !it.isNaN() },
                    weightKg = item.optDouble("weightKg").takeIf { !it.isNaN() },
                    note = item.optString("note")
                )
            }
            root.optJSONArray("hydration")?.forEachObject { item ->
                val date = runCatching { LocalDate.parse(item.getString("date")) }.getOrNull()
                    ?: return@forEachObject
                data.hydrationMl[date] = item.optInt("consumedMl")
                data.hydrationGoalMl[date] = item.optInt("goalMl", 2000)
            }
            root.optJSONArray("nutrition")?.forEachObject { item ->
                val date = runCatching { LocalDate.parse(item.getString("date")) }.getOrNull()
                    ?: return@forEachObject
                data.nutrition += NutritionEntry(
                    id = item.optString("id").ifBlank { UUID.randomUUID().toString() },
                    date = date,
                    meal = Meal.fromName(item.optString("meal", "SNACK")),
                    name = item.optString("name"),
                    kcal = item.optInt("kcal")
                )
            }
        }
        return data
    }

    fun save(data: DesktopData) {
        val root = JSONObject()
        root.put("version", BACKUP_VERSION)
        root.put("exportedAt", System.currentTimeMillis())

        val profile = JSONObject()
        profile.put("displayName", data.profile.displayName)
        profile.put("birthYear", data.profile.birthYear ?: JSONObject.NULL)
        profile.put("weightKg", data.profile.weightKg ?: JSONObject.NULL)
        profile.put("heightCm", data.profile.heightCm ?: JSONObject.NULL)
        profile.put("activityLevel", data.profile.activityLevel.name)
        profile.put("themeName", data.profile.themeName)
        profile.put("darkMode", data.profile.darkMode ?: JSONObject.NULL)
        root.put("profile", profile)

        val logs = JSONArray()
        data.logs.values.forEach { log ->
            logs.put(
                JSONObject().apply {
                    put("date", log.date.toString())
                    put("flow", log.flow.name)
                    put("mood", log.mood?.name ?: JSONObject.NULL)
                    put("symptoms", log.symptoms.joinToString(",") { it.name })
                    put("painLevel", log.painLevel)
                    put("sleepHours", log.sleepHours ?: JSONObject.NULL)
                    put("weightKg", log.weightKg ?: JSONObject.NULL)
                    put("note", log.note)
                    put("updatedAt", System.currentTimeMillis())
                }
            )
        }
        root.put("dailyLogs", logs)

        val hydration = JSONArray()
        (data.hydrationMl.keys + data.hydrationGoalMl.keys).forEach { date ->
            hydration.put(
                JSONObject().apply {
                    put("date", date.toString())
                    put("consumedMl", data.hydrationMl[date] ?: 0)
                    put("goalMl", data.hydrationGoalMl[date] ?: 2000)
                    put("updatedAt", System.currentTimeMillis())
                }
            )
        }
        root.put("hydration", hydration)

        val nutrition = JSONArray()
        data.nutrition.sortedWith(compareBy({ it.date }, { it.meal }, { it.name })).forEach { entry ->
            nutrition.put(
                JSONObject().apply {
                    put("id", entry.id)
                    put("date", entry.date.toString())
                    put("meal", entry.meal.name)
                    put("name", entry.name)
                    put("kcal", entry.kcal)
                    put("updatedAt", System.currentTimeMillis())
                }
            )
        }
        root.put("nutrition", nutrition)

        file.parentFile?.mkdirs()
        file.writeText(root.toString(2))
    }

    /** Merges a phone-exported `bloomee-yedek.json` into the current data. */
    fun importBackup(source: File, data: DesktopData): String {
        val imported = BloomeeStore(source).load()
        data.logs.putAll(imported.logs)
        data.hydrationMl.putAll(imported.hydrationMl)
        data.hydrationGoalMl.putAll(imported.hydrationGoalMl)
        val existingIds = data.nutrition.mapTo(mutableSetOf()) { it.id }
        data.nutrition += imported.nutrition.filter { it.id !in existingIds }
        save(data)
        return "${imported.logs.size} günlük kayıt, ${imported.hydrationMl.size} su günü, " +
            "${imported.nutrition.size} beslenme kaydı içe aktarıldı."
    }

    fun exportBackup(target: File, data: DesktopData) {
        BloomeeStore(target).save(data.copyForExport())
    }

    private fun DesktopData.copyForExport() = this

    private fun parseProfile(json: JSONObject?): DesktopProfile {
        json ?: return DesktopProfile()
        return DesktopProfile(
            displayName = json.optString("displayName"),
            birthYear = json.optInt("birthYear").takeIf { it > 0 },
            weightKg = json.optDouble("weightKg").takeIf { !it.isNaN() && it > 0 },
            heightCm = json.optInt("heightCm").takeIf { it > 0 },
            activityLevel = ActivityLevel.entries
                .firstOrNull { it.name == json.optString("activityLevel") } ?: ActivityLevel.MODERATE,
            themeName = json.optString("themeName", "rose"),
            darkMode = when {
                json.isNull("darkMode") -> null
                else -> json.optBoolean("darkMode")
            }
        )
    }

    private inline fun JSONArray.forEachObject(block: (JSONObject) -> Unit) {
        for (index in 0 until length()) {
            runCatching { getJSONObject(index) }.getOrNull()?.let(block)
        }
    }

    companion object {
        private const val BACKUP_VERSION = 1

        fun defaultFile(): File =
            File(System.getProperty("user.home"), ".bloomee/bloomee-store.json")
    }
}
