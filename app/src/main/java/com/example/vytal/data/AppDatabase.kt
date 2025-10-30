package com.example.vytal.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// ✅ Include both entities: Profile + HealthRecord
@Database(entities = [Profile::class, HealthRecord::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    // ✅ DAOs
    abstract fun profileDao(): ProfileDao
    abstract fun healthRecordDao(): HealthRecordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "vytal_database"
                )
                    // ⚠️ Migration fallback for dev testing
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
