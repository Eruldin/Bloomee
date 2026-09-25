package com.bloomee.app.data.repository

import com.bloomee.app.data.local.HydrationDao
import com.bloomee.app.data.local.HydrationDayEntity
import com.bloomee.app.data.sync.CloudSync
import com.bloomee.app.domain.model.HydrationDay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class HydrationRepository(
    private val dao: HydrationDao,
    private val cloudSync: CloudSync
) {

    val days: Flow<List<HydrationDay>> = dao.observeAll().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun addWater(date: LocalDate, amountMl: Int, goalMl: Int) {
        val existing = dao.findByDate(date.toString())
        val entity = HydrationDayEntity(
            date = date.toString(),
            consumedMl = ((existing?.consumedMl ?: 0) + amountMl).coerceAtLeast(0),
            goalMl = goalMl
        )
        dao.upsert(entity)
        cloudSync.pushHydrationDay(entity)
    }

    suspend fun setGoal(date: LocalDate, goalMl: Int) {
        val existing = dao.findByDate(date.toString())
        val entity = HydrationDayEntity(
            date = date.toString(),
            consumedMl = existing?.consumedMl ?: 0,
            goalMl = goalMl
        )
        dao.upsert(entity)
        cloudSync.pushHydrationDay(entity)
    }

    suspend fun reset(date: LocalDate, goalMl: Int) {
        val entity = HydrationDayEntity(date = date.toString(), consumedMl = 0, goalMl = goalMl)
        dao.upsert(entity)
        cloudSync.pushHydrationDay(entity)
    }

    suspend fun exportAll(): List<HydrationDayEntity> = dao.getAll()

    suspend fun importAll(entities: List<HydrationDayEntity>, replace: Boolean) {
        if (replace) dao.clear()
        dao.upsertAll(entities)
    }
}
