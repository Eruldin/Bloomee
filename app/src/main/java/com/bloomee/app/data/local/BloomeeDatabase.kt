package com.bloomee.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [DailyLogEntity::class, HydrationDayEntity::class],
    version = 1,
    exportSchema = true
)
abstract class BloomeeDatabase : RoomDatabase() {

    abstract fun dailyLogDao(): DailyLogDao

    abstract fun hydrationDao(): HydrationDao

    companion object {
        @Volatile
        private var instance: BloomeeDatabase? = null

        fun get(context: Context): BloomeeDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                BloomeeDatabase::class.java,
                "bloomee.db"
            ).build().also { instance = it }
        }
    }
}
