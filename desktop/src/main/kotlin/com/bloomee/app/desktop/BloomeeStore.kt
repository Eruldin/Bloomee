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
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission
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

    /**
     * Per-record last-write stamps, keyed via [logKey]/[hydrationKey]/[nutritionKey].
     * `save` writes these back verbatim so an unchanged record keeps its real
     * `updatedAt` — required for last-write-wins merges against phone backups.
     * A missing stamp (e.g. a pre-migration store file) is stamped once on save.
     */
    val updatedAt = mutableMapOf<String, Long>()

    /** Keys whose record was tombstoned in the source file (deletedAt > 0).
     *  The record itself is not loaded; only its deletion stamp survives so a
     *  merge can carry the delete instead of resurrecting stale data. */
    val deletedKeys = mutableSetOf<String>()

    fun stampFor(key: String): Long = updatedAt.getOrPut(key) { System.currentTimeMillis() }

    companion object {
        fun logKey(date: LocalDate) = "log:$date"
        fun hydrationKey(date: LocalDate) = "hyd:$date"
        fun nutritionKey(id: String) = "nut:$id"
    }
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
                val key = DesktopData.logKey(date)
                val deletedAt = item.optLong("deletedAt").takeIf { it > 0 }
                if (deletedAt != null) {
                    data.deletedKeys += key
                    data.updatedAt[key] = maxOf(item.optLong("updatedAt"), deletedAt)
                    return@forEachObject
                }
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
                item.optLong("updatedAt").takeIf { it > 0 }
                    ?.let { data.updatedAt[key] = it }
            }
            root.optJSONArray("hydration")?.forEachObject { item ->
                val date = runCatching { LocalDate.parse(item.getString("date")) }.getOrNull()
                    ?: return@forEachObject
                val key = DesktopData.hydrationKey(date)
                val deletedAt = item.optLong("deletedAt").takeIf { it > 0 }
                if (deletedAt != null) {
                    data.deletedKeys += key
                    data.updatedAt[key] = maxOf(item.optLong("updatedAt"), deletedAt)
                    return@forEachObject
                }
                data.hydrationMl[date] = item.optInt("consumedMl")
                data.hydrationGoalMl[date] = item.optInt("goalMl", 2000)
                item.optLong("updatedAt").takeIf { it > 0 }
                    ?.let { data.updatedAt[key] = it }
            }
            root.optJSONArray("nutrition")?.forEachObject { item ->
                val date = runCatching { LocalDate.parse(item.getString("date")) }.getOrNull()
                    ?: return@forEachObject
                val id = item.optString("id").ifBlank { UUID.randomUUID().toString() }
                val key = DesktopData.nutritionKey(id)
                val deletedAt = item.optLong("deletedAt").takeIf { it > 0 }
                if (deletedAt != null) {
                    data.deletedKeys += key
                    data.updatedAt[key] = maxOf(item.optLong("updatedAt"), deletedAt)
                    return@forEachObject
                }
                data.nutrition += NutritionEntry(
                    id = id,
                    date = date,
                    meal = Meal.fromName(item.optString("meal", "SNACK")),
                    name = item.optString("name"),
                    kcal = item.optInt("kcal")
                )
                item.optLong("updatedAt").takeIf { it > 0 }
                    ?.let { data.updatedAt[key] = it }
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
                    put("updatedAt", data.stampFor(DesktopData.logKey(log.date)))
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
                    put("updatedAt", data.stampFor(DesktopData.hydrationKey(date)))
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
                    put("updatedAt", data.stampFor(DesktopData.nutritionKey(entry.id)))
                }
            )
        }
        root.put("nutrition", nutrition)

        file.parentFile?.mkdirs()
        file.writeText(root.toString(2))
        restrictToOwner(file)
    }

    /**
     * Health data at rest should not be world-readable. On POSIX systems the
     * store (and any exported backup) is limited to the owner; other platforms
     * keep their default permissions.
     */
    private fun restrictToOwner(target: File) {
        runCatching {
            if (!FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
                return
            }
            val ownerOnly = setOf(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE
            )
            Files.setPosixFilePermissions(target.toPath(), ownerOnly)
            target.parentFile?.toPath()?.let { dir ->
                Files.setPosixFilePermissions(
                    dir,
                    setOf(
                        PosixFilePermission.OWNER_READ,
                        PosixFilePermission.OWNER_WRITE,
                        PosixFilePermission.OWNER_EXECUTE
                    )
                )
            }
        }
    }

    /**
     * Merges a phone-exported `bloomee-yedek.json` per record: an incoming
     * version is applied only when its `updatedAt` stamp is newer than the
     * local one. Records without a stamp (legacy backups) only fill gaps and
     * never overwrite — an old export can no longer clobber newer edits.
     * Tombstones apply as deletes by the same rule.
     */
    fun importBackup(source: File, data: DesktopData): String {
        val imported = BloomeeStore(source).load()
        var applied = 0
        var skipped = 0
        var deleted = 0

        fun wins(key: String): Boolean {
            val incoming = imported.updatedAt[key] ?: 0L
            return incoming > (data.updatedAt[key] ?: -1L)
        }

        fun adopt(key: String) {
            imported.updatedAt[key]?.let { data.updatedAt[key] = it }
        }

        imported.logs.forEach { (date, log) ->
            val key = DesktopData.logKey(date)
            if (wins(key)) {
                data.logs[date] = log
                adopt(key)
                applied++
            } else skipped++
        }
        imported.hydrationMl.keys.forEach { date ->
            val key = DesktopData.hydrationKey(date)
            if (wins(key)) {
                data.hydrationMl[date] = imported.hydrationMl.getValue(date)
                imported.hydrationGoalMl[date]?.let { data.hydrationGoalMl[date] = it }
                adopt(key)
                applied++
            } else skipped++
        }
        imported.nutrition.forEach { entry ->
            val key = DesktopData.nutritionKey(entry.id)
            if (wins(key)) {
                data.nutrition.removeAll { it.id == entry.id }
                data.nutrition += entry
                adopt(key)
                applied++
            } else skipped++
        }
        imported.deletedKeys.forEach { key ->
            if (!wins(key)) return@forEach
            val target = key.substringAfter(':')
            when (key.substringBefore(':')) {
                "log" -> data.logs.remove(runCatching { LocalDate.parse(target) }.getOrNull())
                "hyd" -> runCatching { LocalDate.parse(target) }.getOrNull()?.let { date ->
                    data.hydrationMl.remove(date)
                    data.hydrationGoalMl.remove(date)
                }
                "nut" -> data.nutrition.removeAll { it.id == target }
            }
            adopt(key)
            deleted++
        }

        save(data)
        return "$applied kayıt eklendi/güncellendi" +
            (if (skipped > 0) ", $skipped atlandı (yereli daha yeni)" else "") +
            (if (deleted > 0) ", $deleted silme işlendi" else "") + "."
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
