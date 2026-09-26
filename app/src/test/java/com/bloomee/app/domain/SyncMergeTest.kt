package com.bloomee.app.domain

import com.bloomee.app.domain.sync.SyncMerge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncMergeTest {

    private data class Rec(
        val key: String,
        val updatedAt: Long,
        val deletedAt: Long? = null
    )

    private fun merge(local: List<Rec>, remote: List<Rec>) = SyncMerge.winners(
        local = local,
        remote = remote,
        key = { it.key },
        timestamp = { SyncMerge.effectiveTimestamp(it.updatedAt, it.deletedAt) }
    )

    @Test
    fun `newer local record wins`() {
        val winners = merge(
            local = listOf(Rec("a", updatedAt = 200)),
            remote = listOf(Rec("a", updatedAt = 100))
        )
        assertEquals(200, winners.single().updatedAt)
    }

    @Test
    fun `newer remote record wins`() {
        val winners = merge(
            local = listOf(Rec("a", updatedAt = 100)),
            remote = listOf(Rec("a", updatedAt = 300))
        )
        assertEquals(300, winners.single().updatedAt)
    }

    @Test
    fun `local tombstone beats older remote edit`() {
        val winners = merge(
            local = listOf(Rec("a", updatedAt = 500, deletedAt = 500)),
            remote = listOf(Rec("a", updatedAt = 100))
        )
        assertEquals(500L, winners.single().deletedAt)
    }

    @Test
    fun `remote tombstone beats older local edit`() {
        val winners = merge(
            local = listOf(Rec("a", updatedAt = 100)),
            remote = listOf(Rec("a", updatedAt = 400, deletedAt = 400))
        )
        assertEquals(400L, winners.single().deletedAt)
    }

    @Test
    fun `edit after delete revives the record`() {
        val winners = merge(
            local = listOf(Rec("a", updatedAt = 900)),
            remote = listOf(Rec("a", updatedAt = 400, deletedAt = 400))
        )
        assertEquals(null, winners.single().deletedAt)
    }

    @Test
    fun `records only on one side are kept`() {
        val winners = merge(
            local = listOf(Rec("a", updatedAt = 1)),
            remote = listOf(Rec("b", updatedAt = 2))
        )
        assertEquals(setOf("a", "b"), winners.map { it.key }.toSet())
    }

    @Test
    fun `pushback detection uses effective timestamp`() {
        // Mirrors CloudSync's filter: a winner pushes only when its effective
        // timestamp differs from the remote copy's (or remote is missing it).
        fun shouldPush(winner: Rec, remote: Rec?): Boolean =
            remote == null ||
                SyncMerge.effectiveTimestamp(remote.updatedAt, remote.deletedAt) !=
                SyncMerge.effectiveTimestamp(winner.updatedAt, winner.deletedAt)

        assertTrue(shouldPush(Rec("a", 500, 500), Rec("a", 100)))
        assertTrue(shouldPush(Rec("a", 500), null))
        assertTrue(!shouldPush(Rec("a", 300), Rec("a", 300)))
        // Remote tombstone already covers a local row with the same timestamp.
        assertTrue(!shouldPush(Rec("a", 100), Rec("a", 100, 100)))
    }
}
