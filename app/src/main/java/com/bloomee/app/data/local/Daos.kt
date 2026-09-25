package com.bloomee.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyLogDao {

    @Query("SELECT * FROM daily_logs ORDER BY date ASC")
    fun observeAll(): Flow<List<DailyLogEntity>>

    @Query("SELECT * FROM daily_logs WHERE date = :date LIMIT 1")
    suspend fun findByDate(date: String): DailyLogEntity?

    @Query("SELECT * FROM daily_logs ORDER BY date ASC")
    suspend fun getAll(): List<DailyLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DailyLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<DailyLogEntity>)

    @Query("DELETE FROM daily_logs WHERE date = :date")
    suspend fun delete(date: String)

    @Query("DELETE FROM daily_logs")
    suspend fun clear()
}

@Dao
interface HydrationDao {

    @Query("SELECT * FROM hydration_days ORDER BY date ASC")
    fun observeAll(): Flow<List<HydrationDayEntity>>

    @Query("SELECT * FROM hydration_days WHERE date = :date LIMIT 1")
    suspend fun findByDate(date: String): HydrationDayEntity?

    @Query("SELECT * FROM hydration_days ORDER BY date ASC")
    suspend fun getAll(): List<HydrationDayEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: HydrationDayEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<HydrationDayEntity>)

    @Query("DELETE FROM hydration_days")
    suspend fun clear()
}

@Dao
interface NutritionDao {

    @Query("SELECT * FROM nutrition_entries ORDER BY date ASC, id ASC")
    fun observeAll(): Flow<List<NutritionEntryEntity>>

    @Query("SELECT * FROM nutrition_entries ORDER BY date ASC, id ASC")
    suspend fun getAll(): List<NutritionEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: NutritionEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<NutritionEntryEntity>)

    @Query("DELETE FROM nutrition_entries WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM nutrition_entries")
    suspend fun clear()
}
