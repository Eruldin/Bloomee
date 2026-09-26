package com.bloomee.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [DailyLogEntity::class, HydrationDayEntity::class, NutritionEntryEntity::class],
    version = 3,
    exportSchema = true
)
abstract class BloomeeDatabase : RoomDatabase() {

    abstract fun dailyLogDao(): DailyLogDao

    abstract fun hydrationDao(): HydrationDao

    abstract fun nutritionDao(): NutritionDao

    companion object {
        @Volatile
        private var instance: BloomeeDatabase? = null

        fun get(context: Context): BloomeeDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                BloomeeDatabase::class.java,
                "bloomee.db"
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { instance = it }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `nutrition_entries` (" +
                        "`id` TEXT NOT NULL, `date` TEXT NOT NULL, `meal` TEXT NOT NULL, " +
                        "`name` TEXT NOT NULL, `kcal` INTEGER NOT NULL, " +
                        "`updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE daily_logs ADD COLUMN deletedAt INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE hydration_days ADD COLUMN deletedAt INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE nutrition_entries ADD COLUMN deletedAt INTEGER DEFAULT NULL")
            }
        }
    }
}
