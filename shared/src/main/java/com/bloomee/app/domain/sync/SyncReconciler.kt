package com.bloomee.app.domain.sync

/**
 * Deterministic last-write-wins reconciliation between local and remote copies of the same
 * collection, plus deletion tombstones. Pure Kotlin so the merge semantics are unit-testable
 * on the JVM without Firebase.
 *
 * Rules:
 *  - A tombstone with timestamp `ts` kills every entity version with `updatedAt <= ts`.
 *  - Among surviving versions of a key, the one with the newest `updatedAt` wins.
 *  - The winner is pushed remotely when the remote copy is missing or older; a remote winner
 *  is imported locally; a tombstoned local copy is deleted locally.
 *  - Tombstones are kept while they may still be needed (an entity killed, or retention not
 *  yet elapsed) and are pruned once a newer entity version supersedes them.
 */
object SyncReconciler {

    const val TOMBSTONE_RETENTION_MS: Long = 90L * 24 * 60 * 60 * 1000 // 90 days

    data class Plan<E>(
        /** Winners to upsert into the local database. */
        val merged: List<E>,
        /** Local entries killed by a tombstone — must be removed from the local database. */
        val toDeleteLocal: List<E>,
        /** Winners the remote side lacks or holds an older copy of — push these. */
        val toPush: List<E>,
        /** Remote document ids to delete because a tombstone killed them. */
        val toDeleteRemote: List<String>,
        /** Tombstones that must be kept after this sync. */
        val liveTombstones: Map<String, Long>
    )

    fun <E> reconcile(
        local: List<E>,
        remote: List<E>,
        tombstones: Map<String, Long>,
        key: (E) -> String,
        updatedAt: (E) -> Long,
        now: Long = System.currentTimeMillis()
    ): Plan<E> {
        val localByKey = local.associateBy(key)
        val remoteByKey = remote.associateBy(key)
        val merged = mutableListOf<E>()
        val toDeleteLocal = mutableListOf<E>()
        val toPush = mutableListOf<E>()
        val toDeleteRemote = mutableListOf<String>()

        for (k in localByKey.keys + remoteByKey.keys) {
            val tombTs = tombstones[k]
            val localEntity = localByKey[k]
            val remoteEntity = remoteByKey[k]
            val localAlive = localEntity != null && (tombTs == null || updatedAt(localEntity) > tombTs)
            val remoteAlive = remoteEntity != null && (tombTs == null || updatedAt(remoteEntity) > tombTs)

            if (localEntity != null && !localAlive) toDeleteLocal += localEntity
            if (!localAlive && !remoteAlive) {
                if (remoteEntity != null) toDeleteRemote += k
                continue
            }

            val winner = listOfNotNull(
                localEntity.takeIf { localAlive },
                remoteEntity.takeIf { remoteAlive }
            ).maxBy { updatedAt(it) }
            merged += winner

            // Remote must be brought to the winner: either missing, dead, or older.
            if (remoteEntity == null || !remoteAlive || updatedAt(remoteEntity) != updatedAt(winner)) {
                toPush += winner
            }
        }

        val liveTombstones = tombstones.filter { (k, ts) ->
            now - ts < TOMBSTONE_RETENTION_MS &&
                localByKey[k]?.let { updatedAt(it) <= ts } != false &&
                remoteByKey[k]?.let { updatedAt(it) <= ts } != false
        }

        return Plan(merged, toDeleteLocal, toPush, toDeleteRemote, liveTombstones)
    }
}
