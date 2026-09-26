package com.bloomee.app.data.repository

import com.bloomee.app.data.local.NutritionDao
import com.bloomee.app.data.local.NutritionEntryEntity
import com.bloomee.app.data.sync.CloudSync
import com.bloomee.app.domain.model.NutritionEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NutritionRepository(
    private val dao: NutritionDao,
    private val cloudSync: CloudSync
) {

    val entries: Flow<List<NutritionEntry>> = dao.observeAll().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun save(entry: NutritionEntry) {
        val entity = NutritionEntryEntity.fromDomain(entry)
        dao.upsert(entity)
        cloudSync.pushNutritionEntry(entity)
    }

    suspend fun delete(id: String) {
        val deletedAt = System.currentTimeMillis()
        dao.softDelete(id, deletedAt)
        cloudSync.deleteNutritionEntry(id, deletedAt)
    }

    suspend fun exportAll(): List<NutritionEntryEntity> = dao.getAll()

    suspend fun exportAllIncludingDeleted(): List<NutritionEntryEntity> = dao.getAllIncludingDeleted()

    suspend fun importAll(entities: List<NutritionEntryEntity>, replace: Boolean) {
        if (replace) dao.clear()
        dao.upsertAll(entities)
    }
}
