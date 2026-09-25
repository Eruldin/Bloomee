package com.bloomee.app.data.sync

import android.content.Context
import android.util.Log
import com.bloomee.app.BuildConfig
import com.bloomee.app.data.local.DailyLogEntity
import com.bloomee.app.data.local.HydrationDayEntity
import com.bloomee.app.data.repository.CycleRepository
import com.bloomee.app.data.repository.HydrationRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
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
    suspend fun deleteDailyLog(date: String)
    suspend fun syncNow(cycleRepository: CycleRepository, hydrationRepository: HydrationRepository)
}

/**
 * Firestore-backed sync. Stays inert until a google-services.json is added to the app module and
 * the user turns sync on, so the app is fully usable offline and without a Firebase project.
 */
class FirebaseCloudSync(private val context: Context) : CloudSync {

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
                mapOf(
                    "date" to entity.date,
                    "flow" to entity.flow,
                    "mood" to entity.mood,
                    "symptoms" to entity.symptoms,
                    "painLevel" to entity.painLevel,
                    "sleepHours" to entity.sleepHours,
                    "weightKg" to entity.weightKg,
                    "note" to entity.note,
                    "updatedAt" to entity.updatedAt
                )
            )?.await()
        }.onFailure { report(it) }
    }

    override suspend fun pushHydrationDay(entity: HydrationDayEntity) {
        if (!enabled) return
        runCatching {
            collection("hydration")?.document(entity.date)?.set(
                mapOf(
                    "date" to entity.date,
                    "consumedMl" to entity.consumedMl,
                    "goalMl" to entity.goalMl,
                    "updatedAt" to entity.updatedAt
                )
            )?.await()
        }.onFailure { report(it) }
    }

    override suspend fun deleteDailyLog(date: String) {
        if (!enabled) return
        runCatching { collection("dailyLogs")?.document(date)?.delete()?.await() }
            .onFailure { report(it) }
    }

    override suspend fun syncNow(
        cycleRepository: CycleRepository,
        hydrationRepository: HydrationRepository
    ) {
        if (!enabled) return
        _state.value = SyncState.SYNCING
        runCatching {
            val remoteLogs = collection("dailyLogs")?.get()?.await()?.documents.orEmpty().mapNotNull { doc ->
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
                    updatedAt = doc.getLong("updatedAt") ?: 0L
                )
            }
            val remoteHydration = collection("hydration")?.get()?.await()?.documents.orEmpty().mapNotNull { doc ->
                val date = doc.getString("date") ?: return@mapNotNull null
                HydrationDayEntity(
                    date = date,
                    consumedMl = doc.getLong("consumedMl")?.toInt() ?: 0,
                    goalMl = doc.getLong("goalMl")?.toInt() ?: 2000,
                    updatedAt = doc.getLong("updatedAt") ?: 0L
                )
            }

            // Last write wins per day, comparing local and remote timestamps.
            val localLogs = cycleRepository.exportAll().associateBy { it.date }
            val mergedLogs = (remoteLogs + localLogs.values)
                .groupBy { it.date }
                .map { (_, versions) -> versions.maxBy { it.updatedAt } }
            cycleRepository.importAll(mergedLogs, replace = false)
            mergedLogs.filter { localLogs[it.date]?.updatedAt != it.updatedAt }.forEach { pushDailyLog(it) }

            val localHydration = hydrationRepository.exportAll().associateBy { it.date }
            val mergedHydration = (remoteHydration + localHydration.values)
                .groupBy { it.date }
                .map { (_, versions) -> versions.maxBy { it.updatedAt } }
            hydrationRepository.importAll(mergedHydration, replace = false)
            mergedHydration.filter { localHydration[it.date]?.updatedAt != it.updatedAt }
                .forEach { pushHydrationDay(it) }

            _state.value = SyncState.IDLE
        }.onFailure {
            report(it)
            _state.value = SyncState.ERROR
        }
    }

    private fun report(throwable: Throwable) {
        Log.w("BloomeeSync", "Cloud sync failed", throwable)
    }
}
