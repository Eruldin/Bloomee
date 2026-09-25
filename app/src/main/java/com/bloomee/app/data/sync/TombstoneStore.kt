package com.bloomee.app.data.sync

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

// Deletion markers for cloud sync. Entries are "key:deletedAtEpochMs"; keys are ISO dates
// or UUIDs and never contain ':'.
private val Context.tombstoneDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "bloomee_tombstones"
)

class TombstoneStore(private val context: Context) {

    private object Keys {
        val logs = stringSetPreferencesKey("deleted_logs")
        val nutrition = stringSetPreferencesKey("deleted_nutrition")
    }

    suspend fun markLog(date: String) = add(Keys.logs, date)

    suspend fun markNutrition(id: String) = add(Keys.nutrition, id)

    private suspend fun add(key: Preferences.Key<Set<String>>, id: String) {
        val ts = System.currentTimeMillis()
        context.tombstoneDataStore.edit { prefs ->
            prefs[key] = (prefs[key] ?: emptySet()) + "$id:$ts"
        }
    }

    suspend fun snapshot(): Pair<Map<String, Long>, Map<String, Long>> {
        val prefs = context.tombstoneDataStore.data.first()
        return decode(prefs[Keys.logs]) to decode(prefs[Keys.nutrition])
    }

    /** Replaces the local tombstone set with the canonical merged set after a sync. */
    suspend fun replaceAll(logs: Map<String, Long>, nutrition: Map<String, Long>) {
        context.tombstoneDataStore.edit { prefs ->
            prefs[Keys.logs] = logs.map { "${it.key}:${it.value}" }.toSet()
            prefs[Keys.nutrition] = nutrition.map { "${it.key}:${it.value}" }.toSet()
        }
    }

    private fun decode(entries: Set<String>?): Map<String, Long> =
        entries.orEmpty()
            .mapNotNull { entry ->
                val sep = entry.lastIndexOf(':')
                if (sep <= 0) return@mapNotNull null
                val ts = entry.substring(sep + 1).toLongOrNull() ?: return@mapNotNull null
                entry.substring(0, sep) to ts
            }
            .toMap()
}
