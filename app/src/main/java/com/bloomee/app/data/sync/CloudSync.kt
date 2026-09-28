package com.bloomee.app.data.sync

import android.content.Context
import android.util.Log
import com.bloomee.app.BuildConfig
import com.bloomee.app.data.local.DailyLogEntity
import com.bloomee.app.data.local.HydrationDayEntity
import com.bloomee.app.data.local.NutritionEntryEntity
import com.bloomee.app.data.prefs.UserPreferencesRepository
import com.bloomee.app.data.repository.CycleRepository
import com.bloomee.app.data.repository.HydrationRepository
import com.bloomee.app.data.repository.NutritionRepository
import com.bloomee.app.domain.sync.SyncMerge
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await

enum class SyncState { DISABLED, UNCONFIGURED, IDLE, SYNCING, ERROR }

interface CloudSync {
    val state: StateFlow<SyncState>
    val enabled: Boolean
    suspend fun setEnabled(enabled: Boolean)
    suspend fun pushDailyLog(entity: DailyLogEntity)
    suspend fun pushHydrationDay(entity: HydrationDayEntity)
    suspend fun pushNutritionEntry(entity: NutritionEntryEntity)
    suspend fun deleteDailyLog(date: String, deletedAt: Long)
    suspend fun deleteNutritionEntry(id: String, deletedAt: Long)
    suspend fun syncNow(
        cycleRepository: CycleRepository,
        hydrationRepository: HydrationRepository,
        nutritionRepository: NutritionRepository
    )
}

/**
 * Firestore-backed sync. Stays inert until a google-services.json is added to the app module and
 * the user turns sync on, so the app is fully usable offline and without a Firebase project.
 *
 * Deletes are tombstones, not removals: a remote doc is kept with `deletedAt` set so stale
 * copies on other devices can't resurrect the record on merge. Local rows behave the same
 * way (soft delete), so deletes made while sync is off are applied on the next syncNow.
 *
 * Incremental reads: every written doc carries `changedAt = max(updatedAt, deletedAt)` and
 * syncNow only fetches docs changed since the stored watermark. Docs written before this
 * field existed lack it, so a first run (watermark 0) always does a full read.
 * Tombstones older than [TOMBSTONE_RETENTION_MS] are pruned locally and remotely.
 */
class FirebaseCloudSync(
    private val context: Context,
    private val preferences: UserPreferencesRepository? = null
) : CloudSync {

    private val _state = MutableStateFlow(
        if (BuildConfig.CLOUD_SYNC_CONFIGURED) SyncState.DISABLED else SyncState.UNCONFIGURED
    )
    override val state: StateFlow<SyncState> = _state

    private var userEnabled = false

    override val enabled: Boolean
        get() = userEnabled && BuildConfig.CLOUD_SYNC_CONFIGURED && FirebaseApp.getApps(context).isNotEmpty()

    override suspend fun setEnabled(enabled: Boolean) {
        userEnabled = enabled
        _state.value = when {
            !BuildConfig.CLOUD_SYNC_CONFIGURED -> SyncState.UNCONFIGURED
            !enabled -> SyncState.DISABLED
            else -> SyncState.IDLE
        }
    }

    private suspend fun collection(name: String) = runCatching {
        val auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid ?: auth.signInAnonymously().await().user?.uid
        uid?.let { FirebaseFirestore.getInstance().collection("users").document(it).collection(name) }
    }.getOrNull()

    override suspend fun pushDailyLog(entity: DailyLogEntity) {
        if (!enabled) return
        runCatching {
            collection("dailyLogs")?.document(entity.date)?.set(
                mutableMapOf<String, Any?>(
                    "date" to entity.date,
                    "flow" to entity.flow,
                    "mood" to entity.mood,
                    "symptoms" to entity.symptoms,
                    "painLevel" to entity.painLevel,
                    "sleepHours" to entity.sleepHours,
                    "weightKg" to entity.weightKg,
                    "note" to entity.note,
                    "updatedAt" to entity.updatedAt,
                    "changedAt" to SyncMerge.effectiveTimestamp(entity.updatedAt, entity.deletedAt)
                ).apply { entity.deletedAt?.let { put("deletedAt", it) } }
            )?.await()
        }.onFailure { report(it) }
    }

    override suspend fun pushHydrationDay(entity: HydrationDayEntity) {
        if (!enabled) return
        runCatching {
            collection("hydration")?.document(entity.date)?.set(
                mutableMapOf<String, Any?>(
                    "date" to entity.date,
                    "consumedMl" to entity.consumedMl,
                    "goalMl" to entity.goalMl,
                    "updatedAt" to entity.updatedAt,
                    "changedAt" to SyncMerge.effectiveTimestamp(entity.updatedAt, entity.deletedAt)
                ).apply { entity.deletedAt?.let { put("deletedAt", it) } }
            )?.await()
        }.onFailure { report(it) }
    }

    override suspend fun pushNutritionEntry(entity: NutritionEntryEntity) {
        if (!enabled) return
        runCatching {
            collection("nutrition")?.document(entity.id)?.set(
                mutableMapOf<String, Any?>(
                    "id" to entity.id,
                    "date" to entity.date,
                    "meal" to entity.meal,
                    "name" to entity.name,
                    "kcal" to entity.kcal,
                    "updatedAt" to entity.updatedAt,
                    "changedAt" to SyncMerge.effectiveTimestamp(entity.updatedAt, entity.deletedAt)
                ).apply { entity.deletedAt?.let { put("deletedAt", it) } }
            )?.await()
        }.onFailure { report(it) }
    }

    override suspend fun deleteDailyLog(date: String, deletedAt: Long) {
        if (!enabled) return
        runCatching {
            collection("dailyLogs")?.document(date)
                ?.set(
                    mapOf(
                        "date" to date,
                        "updatedAt" to deletedAt,
                        "deletedAt" to deletedAt,
                        "changedAt" to deletedAt
                    )
                )
                ?.await()
        }.onFailure { report(it) }
    }

    override suspend fun deleteNutritionEntry(id: String, deletedAt: Long) {
        if (!enabled) return
        runCatching {
            collection("nutrition")?.document(id)
                ?.set(
                    mapOf(
                        "id" to id,
                        "updatedAt" to deletedAt,
                        "deletedAt" to deletedAt,
                        "changedAt" to deletedAt
                    )
                )
                ?.await()
        }.onFailure { report(it) }
    }

    private suspend fun fetchChanged(name: String, watermark: Long): List<DocumentSnapshot> {
        val col = collection(name) ?: return emptyList()
        val query = if (watermark > 0) col.whereGreaterThan("changedAt", watermark) else col
        return query.get().await().documents
    }

    /**
     * Which merged winners to write back. With a delta fetch, a key missing from
     * [remote] means "unchanged on the server", not "absent" — so we only push
     * local winners that changed since the watermark (they were queued offline or
     * the earlier push failed). A full read (watermark 0) treats a missing remote
     * doc as genuinely absent and pushes it. When the remote version was fetched,
     * push only on a timestamp difference.
     */
    private inline fun <T> List<T>.toPush(
        watermark: Long,
        remote: Map<String, T>,
        key: (T) -> String,
        timestamp: (T) -> Long
    ): List<T> = filter { winner ->
        val remoteVersion = remote[key(winner)]
        if (remoteVersion != null) {
            timestamp(remoteVersion) != timestamp(winner)
        } else {
            watermark == 0L || timestamp(winner) > watermark
        }
    }

    override suspend fun syncNow(
        cycleRepository: CycleRepository,
        hydrationRepository: HydrationRepository,
        nutritionRepository: NutritionRepository
    ) {
        if (!enabled) return
        _state.value = SyncState.SYNCING
        runCatching {
            val watermark = preferences?.syncWatermark() ?: 0L
            val syncStart = System.currentTimeMillis()

            val remoteLogs = fetchChanged("dailyLogs", watermark).mapNotNull { doc ->
                val date = doc.getString("date") ?: return@mapNotNull null
                DailyLogEntity(
                    date = date,
                    flow = doc.getString("flow") ?: "NONE",
                    mood = doc.getString("mood"),
                    symptoms = doc.getString("symptoms").orEmpty(),
                    painLevel = doc.getLong("painLevel")?.toInt() ?: 0,
                    sleepHours = doc.getDouble("sleepHours"),
                    weightKg = doc.getDouble("weightKg"),
                    note = doc.getString("note").orEmpty(),
                    updatedAt = doc.getLong("updatedAt") ?: 0L,
                    deletedAt = doc.getLong("deletedAt")
                )
            }
            val remoteHydration = fetchChanged("hydration", watermark).mapNotNull { doc ->
                val date = doc.getString("date") ?: return@mapNotNull null
                HydrationDayEntity(
                    date = date,
                    consumedMl = doc.getLong("consumedMl")?.toInt() ?: 0,
                    goalMl = doc.getLong("goalMl")?.toInt() ?: 2000,
                    updatedAt = doc.getLong("updatedAt") ?: 0L,
                    deletedAt = doc.getLong("deletedAt")
                )
            }
            val remoteNutrition = fetchChanged("nutrition", watermark).mapNotNull { doc ->
                val id = doc.getString("id") ?: doc.id
                NutritionEntryEntity(
                    id = id,
                    date = doc.getString("date") ?: return@mapNotNull null,
                    meal = doc.getString("meal") ?: "SNACK",
                    name = doc.getString("name").orEmpty(),
                    kcal = doc.getLong("kcal")?.toInt() ?: 0,
                    updatedAt = doc.getLong("updatedAt") ?: 0L,
                    deletedAt = doc.getLong("deletedAt")
                )
            }

            // Last write wins per record; tombstones participate via their deletedAt stamp.
            val localLogs = cycleRepository.exportAllIncludingDeleted().associateBy { it.date }
            val mergedLogs = SyncMerge.winners(
                local = localLogs.values.toList().filter { local ->
                    // With a delta fetch the remote set is incomplete — never let an
                    // absent remote doc delete local rows; only fetched docs can win.
                    watermark == 0L ||
                        remoteLogs.any { it.date == local.date } ||
                        SyncMerge.effectiveTimestamp(local.updatedAt, local.deletedAt) > watermark
                },
                remote = remoteLogs,
                key = { it.date },
                timestamp = { SyncMerge.effectiveTimestamp(it.updatedAt, it.deletedAt) }
            )
            cycleRepository.importAll(mergedLogs, replace = false)
            val remoteLogMap = remoteLogs.associateBy { it.date }
            mergedLogs
                .toPush(watermark, remoteLogMap, { it.date }) { e ->
                    SyncMerge.effectiveTimestamp(e.updatedAt, e.deletedAt)
                }
                .forEach { pushDailyLog(it) }

            val localHydration = hydrationRepository.exportAllIncludingDeleted().associateBy { it.date }
            val mergedHydration = SyncMerge.winners(
                local = localHydration.values.toList().filter { local ->
                    watermark == 0L ||
                        remoteHydration.any { it.date == local.date } ||
                        SyncMerge.effectiveTimestamp(local.updatedAt, local.deletedAt) > watermark
                },
                remote = remoteHydration,
                key = { it.date },
                timestamp = { SyncMerge.effectiveTimestamp(it.updatedAt, it.deletedAt) }
            )
            hydrationRepository.importAll(mergedHydration, replace = false)
            val remoteHydrationMap = remoteHydration.associateBy { it.date }
            mergedHydration
                .toPush(watermark, remoteHydrationMap, { it.date }) { e ->
                    SyncMerge.effectiveTimestamp(e.updatedAt, e.deletedAt)
                }
                .forEach { pushHydrationDay(it) }

            val localNutrition = nutritionRepository.exportAllIncludingDeleted().associateBy { it.id }
            val mergedNutrition = SyncMerge.winners(
                local = localNutrition.values.toList().filter { local ->
                    watermark == 0L ||
                        remoteNutrition.any { it.id == local.id } ||
                        SyncMerge.effectiveTimestamp(local.updatedAt, local.deletedAt) > watermark
                },
                remote = remoteNutrition,
                key = { it.id },
                timestamp = { SyncMerge.effectiveTimestamp(it.updatedAt, it.deletedAt) }
            )
            nutritionRepository.importAll(mergedNutrition, replace = false)
            val remoteNutritionMap = remoteNutrition.associateBy { it.id }
            mergedNutrition
                .toPush(watermark, remoteNutritionMap, { it.id }) { e ->
                    SyncMerge.effectiveTimestamp(e.updatedAt, e.deletedAt)
                }
                .forEach { pushNutritionEntry(it) }

            val cutoff = syncStart - TOMBSTONE_RETENTION_MS
            cycleRepository.pruneTombstones(cutoff)
            hydrationRepository.pruneTombstones(cutoff)
            nutritionRepository.pruneTombstones(cutoff)
            pruneRemoteTombstones(cutoff)

            preferences?.setSyncWatermark(syncStart)
            _state.value = SyncState.IDLE
        }.onFailure {
            report(it)
            _state.value = SyncState.ERROR
        }
    }

    private suspend fun pruneRemoteTombstones(cutoff: Long) {
        listOf("dailyLogs", "hydration", "nutrition").forEach { name ->
            runCatching {
                collection(name)?.whereLessThan("deletedAt", cutoff)?.get()?.await()
                    ?.documents?.forEach { doc ->
                        runCatching { doc.reference.delete().await() }
                    }
            }
        }
    }

    private fun report(throwable: Throwable) {
        Log.w("BloomeeSync", "Cloud sync failed", throwable)
    }

    private companion object {
        // Deleted records keep their tombstone for a month — long enough for every
        // device that could still hold a stale copy to sync — then it's dropped.
        const val TOMBSTONE_RETENTION_MS = 30L * 24 * 60 * 60 * 1000
    }
}
