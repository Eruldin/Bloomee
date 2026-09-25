package com.bloomee.app.data.backup

import android.content.Context
import android.net.Uri
import com.bloomee.app.data.local.DailyLogEntity
import com.bloomee.app.data.local.HydrationDayEntity
import com.bloomee.app.data.local.NutritionEntryEntity
import com.bloomee.app.data.repository.CycleRepository
import com.bloomee.app.data.repository.HydrationRepository
import com.bloomee.app.data.repository.NutritionRepository
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

        File(context.cacheDir, "bloomee-yedek.json").apply {
            writeText(root.toString(2))
        }
    }

    suspend fun importFrom(uri: Uri, replace: Boolean): ImportResult = withContext(Dispatchers.IO) {
        runCatching {
            val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: return@runCatching ImportResult(0, 0, 0, "Dosya okunamadı.")
            val root = JSONObject(content)
            val version = root.optInt("version", 0)
            if (version > BACKUP_VERSION) {
                return@runCatching ImportResult(
                    0, 0, 0,
                    "Bu yedek daha yeni bir Bloomee sürümüyle (v$version) oluşturulmuş; uygulamayı güncelle."
                )
            }

            val logs = root.optJSONArray("dailyLogs") ?: JSONArray()
            val logEntities = (0 until logs.length()).map { index ->
                val item = logs.getJSONObject(index)
                DailyLogEntity(
                    date = item.getString("date"),
                    flow = item.optString("flow", "NONE"),
                    mood = item.optString("mood").takeIf { it.isNotBlank() && it != "null" },
                    symptoms = item.optString("symptoms"),
                    painLevel = item.optInt("painLevel"),
                    sleepHours = item.optDouble("sleepHours").takeIf { !it.isNaN() },
                    weightKg = item.optDouble("weightKg").takeIf { !it.isNaN() },
                    note = item.optString("note"),
                    updatedAt = item.optLong("updatedAt", System.currentTimeMillis())
                )
            }

            val hydration = root.optJSONArray("hydration") ?: JSONArray()
            val hydrationEntities = (0 until hydration.length()).map { index ->
                val item = hydration.getJSONObject(index)
                HydrationDayEntity(
                    date = item.getString("date"),
                    consumedMl = item.optInt("consumedMl"),
                    goalMl = item.optInt("goalMl", 2000),
                    updatedAt = item.optLong("updatedAt", System.currentTimeMillis())
                )
            }

            val nutrition = root.optJSONArray("nutrition") ?: JSONArray()
            val nutritionEntities = (0 until nutrition.length()).map { index ->
                val item = nutrition.getJSONObject(index)
                NutritionEntryEntity(
                    id = item.optString("id").ifBlank { UUID.randomUUID().toString() },
                    date = item.getString("date"),
                    meal = item.optString("meal", "SNACK"),
                    name = item.optString("name"),
                    kcal = item.optInt("kcal"),
                    updatedAt = item.optLong("updatedAt", System.currentTimeMillis())
                )
            }

            cycleRepository.importAll(logEntities, replace)
            hydrationRepository.importAll(hydrationEntities, replace)
            nutritionRepository.importAll(nutritionEntities, replace)
            ImportResult(logEntities.size, hydrationEntities.size, nutritionEntities.size, null)
        }.getOrElse {
            val detail = when (it) {
                is org.json.JSONException -> "Dosya biçimi tanınamadı (geçerli bir Bloomee yedeği değil)."
                else -> it.message ?: "Yedek dosyası okunamadı."
            }
            ImportResult(0, 0, 0, detail)
        }
    }

    data class ImportResult(
        val logCount: Int,
        val hydrationCount: Int,
        val nutritionCount: Int,
        val error: String?
    )

    private companion object {
        const val BACKUP_VERSION = 1
    }
}
