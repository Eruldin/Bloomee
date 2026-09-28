package com.bloomee.app.data.backup

import android.content.Context
import android.net.Uri
import com.bloomee.app.data.local.DailyLogEntity
import com.bloomee.app.data.local.HydrationDayEntity
import com.bloomee.app.data.local.NutritionEntryEntity
import com.bloomee.app.data.repository.CycleRepository
import com.bloomee.app.data.repository.HydrationRepository
import com.bloomee.app.data.repository.NutritionRepository
import com.bloomee.app.domain.sync.SyncMerge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/** Human-readable JSON backup, usable as an offline transfer between devices. */
class BackupRepository(
    private val context: Context,
    private val cycleRepository: CycleRepository,
    private val hydrationRepository: HydrationRepository,
    private val nutritionRepository: NutritionRepository
) {

    enum class ImportMode { MERGE, REPLACE }

    suspend fun exportToCacheFile(): File = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("version", BACKUP_VERSION)
        root.put("exportedAt", System.currentTimeMillis())

        val logs = JSONArray()
        cycleRepository.exportAll().forEach { entity ->
            logs.put(
                JSONObject().apply {
                    put("date", entity.date)
                    put("flow", entity.flow)
                    put("mood", entity.mood ?: JSONObject.NULL)
                    put("symptoms", entity.symptoms)
                    put("painLevel", entity.painLevel)
                    put("sleepHours", entity.sleepHours ?: JSONObject.NULL)
                    put("weightKg", entity.weightKg ?: JSONObject.NULL)
                    put("note", entity.note)
                    put("updatedAt", entity.updatedAt)
                }
            )
        }
        root.put("dailyLogs", logs)

        val hydration = JSONArray()
        hydrationRepository.exportAll().forEach { entity ->
            hydration.put(
                JSONObject().apply {
                    put("date", entity.date)
                    put("consumedMl", entity.consumedMl)
                    put("goalMl", entity.goalMl)
                    put("updatedAt", entity.updatedAt)
                }
            )
        }
        root.put("hydration", hydration)

        val nutrition = JSONArray()
        nutritionRepository.exportAll().forEach { entity ->
            nutrition.put(
                JSONObject().apply {
                    put("id", entity.id)
                    put("date", entity.date)
                    put("meal", entity.meal)
                    put("name", entity.name)
                    put("kcal", entity.kcal)
                    put("updatedAt", entity.updatedAt)
                }
            )
        }
        root.put("nutrition", nutrition)

        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        File(dir, "bloomee-yedek.json").apply {
            writeText(root.toString(2))
        }
    }

    /**
     * Reads and parses the file without touching the database so the UI can show
     * what an import would do before the user commits to it.
     */
    suspend fun previewImport(uri: Uri): ImportPreview = withContext(Dispatchers.IO) {
        when (val parsed = parseUri(uri)) {
            is ParseOutcome.Failed -> ImportPreview(error = parsed.message)
            is ParseOutcome.Ok -> {
                val decision = decide(parsed.backup)
                ImportPreview(
                    backup = parsed.backup,
                    applyCount = decision.totalApply,
                    skippedNewerLocalCount = decision.totalSkipped,
                    invalidCount = parsed.backup.invalidCount
                )
            }
        }
    }

    suspend fun importBackup(backup: ParsedBackup, mode: ImportMode): ImportResult =
        withContext(Dispatchers.IO) {
            if (mode == ImportMode.REPLACE) {
                cycleRepository.importAll(backup.logs, replace = true)
                hydrationRepository.importAll(backup.hydration, replace = true)
                nutritionRepository.importAll(backup.nutrition, replace = true)
                return@withContext ImportResult(
                    appliedCount = backup.logs.size + backup.hydration.size + backup.nutrition.size,
                    skippedNewerLocalCount = 0,
                    invalidCount = backup.invalidCount,
                    error = null
                )
            }
            val decision = decide(backup)
            cycleRepository.importAll(decision.logs.apply, replace = false)
            hydrationRepository.importAll(decision.hydration.apply, replace = false)
            nutritionRepository.importAll(decision.nutrition.apply, replace = false)
            ImportResult(
                appliedCount = decision.totalApply,
                skippedNewerLocalCount = decision.totalSkipped,
                invalidCount = backup.invalidCount,
                error = null
            )
        }

    /** Convenience wrapper for callers that don't need a preview step. */
    suspend fun importFrom(uri: Uri, mode: ImportMode): ImportResult =
        when (val parsed = parseUri(uri)) {
            is ParseOutcome.Failed -> ImportResult(0, 0, parsed.backup?.invalidCount ?: 0, parsed.message)
            is ParseOutcome.Ok -> importBackup(parsed.backup, mode)
        }

    private sealed interface ParseOutcome {
        data class Ok(val backup: ParsedBackup) : ParseOutcome
        data class Failed(val message: String, val backup: ParsedBackup? = null) : ParseOutcome
    }

    private fun parseUri(uri: Uri): ParseOutcome {
        return runCatching {
            val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: return ParseOutcome.Failed("Dosya okunamadı.")
            val root = JSONObject(content)
            val version = root.optInt("version", 0)
            if (version > BACKUP_VERSION) {
                return ParseOutcome.Failed(
                    "Bu yedek daha yeni bir Bloomee sürümüyle (v$version) oluşturulmuş; uygulamayı güncelle."
                )
            }
            ParseOutcome.Ok(parseRoot(root))
        }.getOrElse {
            val detail = when (it) {
                is org.json.JSONException -> "Dosya biçimi tanınamadı (geçerli bir Bloomee yedeği değil)."
                else -> it.message ?: "Yedek dosyası okunamadı."
            }
            ParseOutcome.Failed(detail)
        }
    }

    // Per-item parsing is tolerant: a malformed row is skipped and counted instead
    // of failing the whole file.
    private fun parseRoot(root: JSONObject): ParsedBackup {
        var invalid = 0

        fun JSONArray?.objects(): List<JSONObject> {
            this ?: return emptyList()
            val items = mutableListOf<JSONObject>()
            for (index in 0 until length()) {
                val item = runCatching { getJSONObject(index) }.getOrNull()
                if (item == null) invalid++ else items += item
            }
            return items
        }

        val now = System.currentTimeMillis()
        val logs = root.optJSONArray("dailyLogs").objects().mapNotNull { item ->
            runCatching {
                DailyLogEntity(
                    date = item.getString("date").also { java.time.LocalDate.parse(it) },
                    flow = item.optString("flow", "NONE"),
                    mood = item.optString("mood").takeIf { it.isNotBlank() && it != "null" },
                    symptoms = item.optString("symptoms"),
                    painLevel = item.optInt("painLevel"),
                    sleepHours = item.optDouble("sleepHours").takeIf { !it.isNaN() },
                    weightKg = item.optDouble("weightKg").takeIf { !it.isNaN() },
                    note = item.optString("note"),
                    updatedAt = item.optLong("updatedAt", now),
                    deletedAt = item.optLong("deletedAt").takeIf { it > 0 }
                )
            }.getOrElse {
                invalid++
                null
            }
        }
        val hydration = root.optJSONArray("hydration").objects().mapNotNull { item ->
            runCatching {
                HydrationDayEntity(
                    date = item.getString("date").also { java.time.LocalDate.parse(it) },
                    consumedMl = item.optInt("consumedMl"),
                    goalMl = item.optInt("goalMl", 2000),
                    updatedAt = item.optLong("updatedAt", now),
                    deletedAt = item.optLong("deletedAt").takeIf { it > 0 }
                )
            }.getOrElse {
                invalid++
                null
            }
        }
        val nutrition = root.optJSONArray("nutrition").objects().mapNotNull { item ->
            runCatching {
                NutritionEntryEntity(
                    id = item.optString("id").ifBlank { UUID.randomUUID().toString() },
                    date = item.getString("date").also { java.time.LocalDate.parse(it) },
                    meal = item.optString("meal", "SNACK"),
                    name = item.optString("name"),
                    kcal = item.optInt("kcal"),
                    updatedAt = item.optLong("updatedAt", now),
                    deletedAt = item.optLong("deletedAt").takeIf { it > 0 }
                )
            }.getOrElse {
                invalid++
                null
            }
        }
        val tombstones = logs.count { it.deletedAt != null } +
            hydration.count { it.deletedAt != null } +
            nutrition.count { it.deletedAt != null }
        return ParsedBackup(logs, hydration, nutrition, invalid, tombstones)
    }

    private suspend fun decide(backup: ParsedBackup): MergeDecision {
        val logs = SyncMerge.applySet(
            local = cycleRepository.exportAllIncludingDeleted(),
            incoming = backup.logs,
            key = { it.date },
            timestamp = { SyncMerge.effectiveTimestamp(it.updatedAt, it.deletedAt) }
        )
        val hydration = SyncMerge.applySet(
            local = hydrationRepository.exportAllIncludingDeleted(),
            incoming = backup.hydration,
            key = { it.date },
            timestamp = { SyncMerge.effectiveTimestamp(it.updatedAt, it.deletedAt) }
        )
        val nutrition = SyncMerge.applySet(
            local = nutritionRepository.exportAllIncludingDeleted(),
            incoming = backup.nutrition,
            key = { it.id },
            timestamp = { SyncMerge.effectiveTimestamp(it.updatedAt, it.deletedAt) }
        )
        return MergeDecision(logs, hydration, nutrition)
    }

    private data class MergeDecision(
        val logs: SyncMerge.ApplyDecision<DailyLogEntity>,
        val hydration: SyncMerge.ApplyDecision<HydrationDayEntity>,
        val nutrition: SyncMerge.ApplyDecision<NutritionEntryEntity>
    ) {
        val totalApply: Int get() = logs.apply.size + hydration.apply.size + nutrition.apply.size
        val totalSkipped: Int get() = logs.skipped + hydration.skipped + nutrition.skipped
    }

    data class ParsedBackup(
        val logs: List<DailyLogEntity>,
        val hydration: List<HydrationDayEntity>,
        val nutrition: List<NutritionEntryEntity>,
        val invalidCount: Int,
        val tombstoneCount: Int
    ) {
        val totalRecords: Int get() = logs.size + hydration.size + nutrition.size
    }

    data class ImportPreview(
        val backup: ParsedBackup? = null,
        val applyCount: Int = 0,
        val skippedNewerLocalCount: Int = 0,
        val invalidCount: Int = 0,
        val error: String? = null
    )

    data class ImportResult(
        val appliedCount: Int,
        val skippedNewerLocalCount: Int,
        val invalidCount: Int,
        val error: String?
    )

    private companion object {
        const val BACKUP_VERSION = 1
    }
}
