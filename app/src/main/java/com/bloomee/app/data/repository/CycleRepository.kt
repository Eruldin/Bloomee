package com.bloomee.app.data.repository

import com.bloomee.app.data.local.DailyLogDao
import com.bloomee.app.data.local.DailyLogEntity
import com.bloomee.app.data.sync.CloudSync
import com.bloomee.app.domain.model.DailyLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class CycleRepository(
    private val dao: DailyLogDao,
    private val cloudSync: CloudSync
) {

    val logs: Flow<List<DailyLog>> = dao.observeAll().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun logFor(date: LocalDate): DailyLog? = dao.findByDate(date.toString())?.toDomain()

    suspend fun save(log: DailyLog) {
        if (log.isEmpty) {
            dao.delete(log.date.toString())
            cloudSync.deleteDailyLog(log.date.toString())
            return
        }
        val entity = DailyLogEntity.fromDomain(log)
        dao.upsert(entity)
        cloudSync.pushDailyLog(entity)
    }

    suspend fun delete(date: LocalDate) {
        dao.delete(date.toString())
        cloudSync.deleteDailyLog(date.toString())
    }

    /** Removes entries locally only — used by sync to apply remote tombstones. */
    suspend fun removeLocal(dates: List<String>) {
        dates.forEach { dao.delete(it) }
    }

    suspend fun markPeriodRange(start: LocalDate, endInclusive: LocalDate, flowLevelName: String) {
        var cursor = start
        while (!cursor.isAfter(endInclusive)) {
            val existing = logFor(cursor) ?: DailyLog(date = cursor)
            save(existing.copy(flow = com.bloomee.app.domain.model.FlowLevel.fromName(flowLevelName)))
            cursor = cursor.plusDays(1)
        }
    }

    suspend fun exportAll(): List<DailyLogEntity> = dao.getAll()

    suspend fun importAll(entities: List<DailyLogEntity>, replace: Boolean) {
        if (replace) dao.clear()
        dao.upsertAll(entities)
    }
}
