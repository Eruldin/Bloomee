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
}
