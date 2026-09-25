package com.bloomee.app.data.sync

import android.content.Context
import android.util.Log
import com.bloomee.app.BuildConfig
import com.bloomee.app.data.local.DailyLogEntity
import com.bloomee.app.data.local.HydrationDayEntity
import com.bloomee.app.data.local.NutritionEntryEntity
import com.bloomee.app.data.repository.CycleRepository
import com.bloomee.app.data.repository.HydrationRepository
import com.bloomee.app.data.repository.NutritionRepository
import com.bloomee.app.domain.sync.SyncReconciler
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await

enum class SyncState { DISABLED, UNCONFIGURED, IDLE, SYNCING, ERROR }

class SyncUnavailableException : Exception("Bulut hesabına erişilemedi")

interface CloudSync {
    val state: StateFlow<SyncState>
    val enabled: Boolean
    suspend fun setEnabled(enabled: Boolean)
    /** @return true when the write reached the remote side (or sync is off and nothing was owed). */
    suspend fun pushDailyLog(entity: DailyLogEntity): Boolean
    suspend fun pushHydrationDay(entity: HydrationDayEntity): Boolean
    suspend fun pushNutritionEntry(entity: NutritionEntryEntity): Boolean
    suspend fun deleteDailyLog(date: String): Boolean
    suspend fun deleteNutritionEntry(id: String): Boolean
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
 * Reconciliation is last-write-wins on `updatedAt` plus deletion tombstones (see
 * [SyncReconciler]). Tombstones live in a local DataStore for pending deletions and in
 * `users/{uid}/meta/tombstones` so deletions propagate to other devices instead of
 * resurrecting entries on their next sync.
 *
 * Note: sync uses an anonymous Firebase account tied to this install — data cannot be
 * transferred to a new device via the account; the JSON backup covers device transfer.
 */
class FirebaseCloudSync(
    private val context: Context,
    private val tombstones: TombstoneStore
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

    private suspend fun userDoc(): DocumentReference? = runCatching {
        val auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid ?: auth.signInAnonymously().await().user?.uid
        uid?.let { FirebaseFirestore.getInstance().collection("users").document(it) }
    }.getOrNull()

    private fun fail(t: Throwable): Boolean {
        report(t)
        _state.value = SyncState.ERROR
        return false
    }

    override suspend fun pushDailyLog(entity: DailyLogEntity): Boolean {
        if (!enabled) return true
        val user = userDoc() ?: return fail(SyncUnavailableException())
        return try {
            user.collection("dailyLogs").document(entity.date).set(
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
            ).await()
            true
        } catch (e: Exception) {
            fail(e)
        }
    }

    override suspend fun pushHydrationDay(entity: HydrationDayEntity): Boolean {
        if (!enabled) return true
        val user = userDoc() ?: return fail(SyncUnavailableException())
        return try {
            user.collection("hydration").document(entity.date).set(
                mapOf(
                    "date" to entity.date,
                    "consumedMl" to entity.consumedMl,
                    "goalMl" to entity.goalMl,
                    "updatedAt" to entity.updatedAt
                )
            ).await()
            true
        } catch (e: Exception) {
            fail(e)
        }
    }

    override suspend fun pushNutritionEntry(entity: NutritionEntryEntity): Boolean {
        if (!enabled) return true
        val user = userDoc() ?: return fail(SyncUnavailableException())
        return try {
            user.collection("nutrition").document(entity.id).set(
                mapOf(
                    "id" to entity.id,
                    "date" to entity.date,
                    "meal" to entity.meal,
                    "name" to entity.name,
                    "kcal" to entity.kcal,
                    "updatedAt" to entity.updatedAt
                )
            ).await()
            true
        } catch (e: Exception) {
            fail(e)
        }
    }

    override suspend fun deleteDailyLog(date: String): Boolean {
        // Tombstone even while disabled: if sync is turned on later, the remote copy must die.
        tombstones.markLog(date)
        return deleteRemoteAndTombstone("dailyLogs", "deletedLogs", date)
    }

    override suspend fun deleteNutritionEntry(id: String): Boolean {
        tombstones.markNutrition(id)
        return deleteRemoteAndTombstone("nutrition", "deletedNutrition", id)
    }

    private suspend fun deleteRemoteAndTombstone(
        collection: String,
        tombstoneField: String,
        key: String
    ): Boolean {
        if (!enabled) return true
        val user = userDoc() ?: return fail(SyncUnavailableException())
        var ok = true
        ok = deleteRemote(user, collection, key) && ok
        ok = upsertRemoteTombstone(user, tombstoneField, key) && ok
        if (!ok) _state.value = SyncState.ERROR
        return ok
    }

    private suspend fun deleteRemote(user: DocumentReference, collection: String, key: String): Boolean =
        try {
            user.collection(collection).document(key).delete().await()
            true
        } catch (e: Exception) {
            report(e)
            false
        }

    private suspend fun upsertRemoteTombstone(
        user: DocumentReference,
        field: String,
        key: String
    ): Boolean = try {
        val doc = user.collection("meta").document("tombstones")
        val current = readTombstoneMap(doc.get().await().get(field))
        val merged = current + (key to maxOf(current[key] ?: 0L, System.currentTimeMillis()))
        doc.set(mapOf(field to merged), SetOptions.merge()).await()
        true
    } catch (e: Exception) {
        report(e)
        false
    }

    override suspend fun syncNow(
        cycleRepository: CycleRepository,
        hydrationRepository: HydrationRepository,
        nutritionRepository: NutritionRepository
    ) {
        if (!enabled) return
        _state.value = SyncState.SYNCING
        runCatching {
            val user = userDoc() ?: throw SyncUnavailableException()
            val now = System.currentTimeMillis()

            // Effective tombstones = local pending deletes ∪ remote doc, newest wins per key.
            val (localLogTomb, localNutritionTomb) = tombstones.snapshot()
            val remoteTombDoc = user.collection("meta").document("tombstones").get().await()
            val logTomb = mergeTombstones(localLogTomb, readTombstoneMap(remoteTombDoc.get("deletedLogs")))
            val nutritionTomb =
                mergeTombstones(localNutritionTomb, readTombstoneMap(remoteTombDoc.get("deletedNutrition")))

            var failures = 0

            val remoteLogs = user.collection("dailyLogs").get().await().documents.mapNotNull { doc ->
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
            val logPlan = SyncReconciler.reconcile(
                cycleRepository.exportAll(), remoteLogs, logTomb, { it.date }, { it.updatedAt }, now
            )
            cycleRepository.removeLocal(logPlan.toDeleteLocal.map { it.date })
            cycleRepository.importAll(logPlan.merged, replace = false)
            logPlan.toPush.forEach { if (!pushDailyLog(it)) failures++ }
            logPlan.toDeleteRemote.forEach { if (!deleteRemote(user, "dailyLogs", it)) failures++ }

            val remoteHydration = user.collection("hydration").get().await().documents.mapNotNull { doc ->
                val date = doc.getString("date") ?: return@mapNotNull null
                HydrationDayEntity(
                    date = date,
                    consumedMl = doc.getLong("consumedMl")?.toInt() ?: 0,
                    goalMl = doc.getLong("goalMl")?.toInt() ?: 2000,
                    updatedAt = doc.getLong("updatedAt") ?: 0L
                )
            }
            // Hydration has no delete path — days are zeroed, never removed.
            val hydrationPlan = SyncReconciler.reconcile(
                hydrationRepository.exportAll(), remoteHydration, emptyMap(),
                { it.date }, { it.updatedAt }, now
            )
            hydrationRepository.importAll(hydrationPlan.merged, replace = false)
            hydrationPlan.toPush.forEach { if (!pushHydrationDay(it)) failures++ }

            val remoteNutrition = user.collection("nutrition").get().await().documents.mapNotNull { doc ->
                val id = doc.getString("id") ?: doc.id
                NutritionEntryEntity(
                    id = id,
                    date = doc.getString("date") ?: return@mapNotNull null,
                    meal = doc.getString("meal") ?: "SNACK",
                    name = doc.getString("name").orEmpty(),
                    kcal = doc.getLong("kcal")?.toInt() ?: 0,
                    updatedAt = doc.getLong("updatedAt") ?: 0L
                )
            }
            val nutritionPlan = SyncReconciler.reconcile(
                nutritionRepository.exportAll(), remoteNutrition, nutritionTomb, { it.id }, { it.updatedAt }, now
            )
            nutritionRepository.removeLocal(nutritionPlan.toDeleteLocal.map { it.id })
            nutritionRepository.importAll(nutritionPlan.merged, replace = false)
            nutritionPlan.toPush.forEach { if (!pushNutritionEntry(it)) failures++ }
            nutritionPlan.toDeleteRemote.forEach { if (!deleteRemote(user, "nutrition", it)) failures++ }

            // Persist the reconciled tombstone set on both sides so other devices learn deletions.
            user.collection("meta").document("tombstones").set(
                mapOf(
                    "deletedLogs" to logPlan.liveTombstones,
                    "deletedNutrition" to nutritionPlan.liveTombstones
                )
            ).await()
            tombstones.replaceAll(logPlan.liveTombstones, nutritionPlan.liveTombstones)

            _state.value = if (failures == 0) SyncState.IDLE else SyncState.ERROR
        }.onFailure {
            report(it)
            _state.value = SyncState.ERROR
        }
    }

    private fun readTombstoneMap(raw: Any?): Map<String, Long> =
        (raw as? Map<*, *>)?.mapNotNull { (k, v) ->
            val key = k as? String ?: return@mapNotNull null
            val ts = (v as? Number)?.toLong() ?: return@mapNotNull null
            key to ts
        }?.toMap().orEmpty()

    private fun mergeTombstones(a: Map<String, Long>, b: Map<String, Long>): Map<String, Long> =
        (a.keys + b.keys).associateWith { k -> maxOf(a[k] ?: 0L, b[k] ?: 0L) }

    private fun report(throwable: Throwable) {
        Log.w("BloomeeSync", "Cloud sync failed", throwable)
    }
}
