package com.bloomee.app.domain.sync

/**
 * Last-write-wins merge helpers for cloud sync. A record carries an `updatedAt`
 * edit timestamp and an optional `deletedAt` tombstone; the effective timestamp
 * is whichever is newer, so a delete beats an older edit.
 */
object SyncMerge {

    fun effectiveTimestamp(updatedAt: Long, deletedAt: Long?): Long =
        maxOf(updatedAt, deletedAt ?: 0L)

    /** Winners of a key-wise merge between local and remote copies of a collection. */
    fun <T> winners(
        local: List<T>,
        remote: List<T>,
        key: (T) -> String,
        timestamp: (T) -> Long
    ): List<T> = (local + remote).groupBy(key).map { (_, versions) -> versions.maxBy(timestamp) }

    /**
     * Records from [incoming] that should be applied over [local]: an incoming
     * record applies only when no local version exists or its timestamp is
     * strictly newer. Ties keep the local version; [skipped] counts losers.
     */
    data class ApplyDecision<T>(val apply: List<T>, val skipped: Int)

    fun <T> applySet(
        local: List<T>,
        incoming: List<T>,
        key: (T) -> String,
        timestamp: (T) -> Long
    ): ApplyDecision<T> {
        val localStamp = local.associate { key(it) to timestamp(it) }
        val apply = incoming.filter { item -> (localStamp[key(item)] ?: -1L) < timestamp(item) }
        return ApplyDecision(apply, incoming.size - apply.size)
    }
}
