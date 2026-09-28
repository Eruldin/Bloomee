package com.bloomee.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyLogDao {

    @Query("SELECT * FROM daily_logs WHERE deletedAt IS NULL ORDER BY date ASC")
    fun observeAll(): Flow<List<DailyLogEntity>>

    @Query("SELECT * FROM daily_logs WHERE date = :date AND deletedAt IS NULL LIMIT 1")
    suspend fun findByDate(date: String): DailyLogEntity?

    @Query("SELECT * FROM daily_logs WHERE deletedAt IS NULL ORDER BY date ASC")
    suspend fun getAll(): List<DailyLogEntity>

    @Query("SELECT * FROM daily_logs ORDER BY date ASC")
    suspend fun getAllIncludingDeleted(): List<DailyLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DailyLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<DailyLogEntity>)

    // Tombstones carry no payload: a deleted record keeps only its deletion
    // marker, so the sensitive fields don't linger in the database or backups.
    suspend fun softDelete(date: String, deletedAt: Long) {
        upsert(
            DailyLogEntity(
                date = date,
                flow = "NONE",
                mood = null,
                symptoms = "",
                painLevel = 0,
                sleepHours = null,
                weightKg = null,
                note = "",
                updatedAt = deletedAt,
                deletedAt = deletedAt
            )
        )
    }

    @Query("DELETE FROM daily_logs WHERE deletedAt IS NOT NULL AND deletedAt < :cutoffMs")
    suspend fun pruneTombstones(cutoffMs: Long)

    @Query("DELETE FROM daily_logs")
    suspend fun clear()
}

@Dao
interface HydrationDao {

    @Query("SELECT * FROM hydration_days WHERE deletedAt IS NULL ORDER BY date ASC")
    fun observeAll(): Flow<List<HydrationDayEntity>>

    @Query("SELECT * FROM hydration_days WHERE date = :date AND deletedAt IS NULL LIMIT 1")
    suspend fun findByDate(date: String): HydrationDayEntity?

    @Query("SELECT * FROM hydration_days WHERE deletedAt IS NULL ORDER BY date ASC")
    suspend fun getAll(): List<HydrationDayEntity>

    @Query("SELECT * FROM hydration_days ORDER BY date ASC")
    suspend fun getAllIncludingDeleted(): List<HydrationDayEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: HydrationDayEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<HydrationDayEntity>)

    suspend fun softDelete(date: String, deletedAt: Long) {
        upsert(
            HydrationDayEntity(
                date = date,
                consumedMl = 0,
                goalMl = 0,
                updatedAt = deletedAt,
                deletedAt = deletedAt
            )
        )
    }

    @Query("DELETE FROM hydration_days WHERE deletedAt IS NOT NULL AND deletedAt < :cutoffMs")
    suspend fun pruneTombstones(cutoffMs: Long)

    @Query("DELETE FROM hydration_days")
    suspend fun clear()
}

@Dao
interface NutritionDao {

    @Query("SELECT * FROM nutrition_entries WHERE deletedAt IS NULL ORDER BY date ASC, id ASC")
    fun observeAll(): Flow<List<NutritionEntryEntity>>

    @Query("SELECT * FROM nutrition_entries WHERE id = :id LIMIT 1")
    suspend fun findByIdIncludingDeleted(id: String): NutritionEntryEntity?

    @Query("SELECT * FROM nutrition_entries WHERE deletedAt IS NULL ORDER BY date ASC, id ASC")
    suspend fun getAll(): List<NutritionEntryEntity>

    @Query("SELECT * FROM nutrition_entries ORDER BY date ASC, id ASC")
    suspend fun getAllIncludingDeleted(): List<NutritionEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: NutritionEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<NutritionEntryEntity>)

    suspend fun softDelete(id: String, deletedAt: Long) {
        val existing = findByIdIncludingDeleted(id) ?: return
        upsert(
            existing.copy(
                deletedAt = deletedAt,
                updatedAt = deletedAt,
                name = "",
                kcal = 0
            )
        )
    }

    @Query("DELETE FROM nutrition_entries WHERE deletedAt IS NOT NULL AND deletedAt < :cutoffMs")
    suspend fun pruneTombstones(cutoffMs: Long)

    @Query("DELETE FROM nutrition_entries")
    suspend fun clear()
}
