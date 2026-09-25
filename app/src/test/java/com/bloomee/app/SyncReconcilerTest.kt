package com.bloomee.app

import com.bloomee.app.domain.sync.SyncReconciler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncReconcilerTest {

    private data class Item(val id: String, val updatedAt: Long, val value: String = "")

    private fun plan(
        local: List<Item>,
        remote: List<Item>,
        tombstones: Map<String, Long> = emptyMap(),
        now: Long = 10_000L
    ) = SyncReconciler.reconcile(local, remote, tombstones, { it.id }, { it.updatedAt }, now)

    @Test
    fun `local only entries are pushed to remote`() {
        val item = Item("a", 100)
        val p = plan(local = listOf(item), remote = emptyList())
        assertEquals(listOf(item), p.merged)
        assertEquals(listOf(item), p.toPush)
        assertTrue(p.toDeleteRemote.isEmpty())
        assertTrue(p.toDeleteLocal.isEmpty())
    }

    @Test
    fun `remote only entries are imported locally and not pushed`() {
        val item = Item("a", 100)
        val p = plan(local = emptyList(), remote = listOf(item))
        assertEquals(listOf(item), p.merged)
        assertTrue(p.toPush.isEmpty())
    }

    @Test
    fun `local winner is pushed when remote copy is older`() {
        val local = Item("a", 200, "new")
        val remote = Item("a", 100, "old")
        val p = plan(local = listOf(local), remote = listOf(remote))
        assertEquals(listOf(local), p.merged)
        assertEquals(listOf(local), p.toPush)
        assertTrue(p.toDeleteRemote.isEmpty())
    }

    @Test
    fun `remote winner replaces stale local copy without a push`() {
        val local = Item("a", 100, "old")
        val remote = Item("a", 200, "new")
        val p = plan(local = listOf(local), remote = listOf(remote))
        assertEquals(listOf(remote), p.merged)
        assertTrue(p.toPush.isEmpty())
    }

    @Test
    fun `tombstone kills local copy while remote copy was already deleted`() {
        val local = Item("a", 100)
        val p = plan(
            local = listOf(local),
            remote = emptyList(),
            tombstones = mapOf("a" to 500)
        )
        assertTrue(p.merged.isEmpty())
        assertEquals(listOf(local), p.toDeleteLocal)
        assertTrue(p.toDeleteRemote.isEmpty())
        assertEquals(mapOf("a" to 500L), p.liveTombstones)
    }

    @Test
    fun `tombstone kills remote copy and schedules remote delete`() {
        val remote = Item("a", 100)
        val p = plan(
            local = emptyList(),
            remote = listOf(remote),
            tombstones = mapOf("a" to 500)
        )
        assertTrue(p.merged.isEmpty())
        assertEquals(listOf("a"), p.toDeleteRemote)
        assertTrue(p.toPush.isEmpty())
        assertEquals(mapOf("a" to 500L), p.liveTombstones)
    }

    @Test
    fun `remote tombstone propagates deletion to surviving local copy`() {
        // Device A deleted "a" and pushed tombstone; device B still holds the entry.
        val local = Item("a", 100)
        val p = plan(local = listOf(local), remote = emptyList(), tombstones = mapOf("a" to 900))
        assertTrue(p.merged.isEmpty())
        assertEquals(listOf(local), p.toDeleteLocal)
        assertTrue(p.toPush.isEmpty())
    }

    @Test
    fun `recreated entity newer than tombstone survives and prunes it`() {
        val local = Item("a", 1_000)
        val p = plan(
            local = listOf(local),
            remote = emptyList(),
            tombstones = mapOf("a" to 500)
        )
        assertEquals(listOf(local), p.merged)
        assertEquals(listOf(local), p.toPush)
        assertTrue(p.liveTombstones.isEmpty())
    }

    @Test
    fun `remote edit newer than tombstone resurrects the entry`() {
        val remote = Item("a", 2_000)
        val local = Item("a", 100)
        val p = plan(
            local = listOf(local),
            remote = listOf(remote),
            tombstones = mapOf("a" to 500)
        )
        assertEquals(listOf(remote), p.merged)
        assertEquals(listOf(local), p.toDeleteLocal) // stale local copy replaced
        assertTrue(p.toDeleteRemote.isEmpty())
        assertTrue(p.liveTombstones.isEmpty())
    }

    @Test
    fun `expired tombstone is pruned after applying`() {
        val remote = Item("a", 100)
        val expired = 200L // kills a@100 but is far in the past relative to now
        val p = plan(
            local = emptyList(),
            remote = listOf(remote),
            tombstones = mapOf("a" to expired),
            now = expired + SyncReconciler.TOMBSTONE_RETENTION_MS + 1
        )
        assertEquals(listOf("a"), p.toDeleteRemote)
        assertTrue(p.liveTombstones.isEmpty())
    }

    @Test
    fun `mixed collections merge independently`() {
        val localOnly = Item("local", 100)
        val remoteOnly = Item("remote", 200)
        val sharedLocal = Item("shared", 50, "old")
        val sharedRemote = Item("shared", 300, "new")
        val deletedRemote = Item("dead", 10)
        val p = plan(
            local = listOf(localOnly, sharedLocal),
            remote = listOf(remoteOnly, sharedRemote, deletedRemote),
            tombstones = mapOf("dead" to 400)
        )
        assertEquals(setOf(localOnly, remoteOnly, sharedRemote), p.merged.toSet())
        assertEquals(listOf(localOnly), p.toPush)
        assertEquals(listOf("dead"), p.toDeleteRemote)
        assertEquals(mapOf("dead" to 400L), p.liveTombstones)
    }
}
