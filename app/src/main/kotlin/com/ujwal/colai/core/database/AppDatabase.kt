package com.ujwal.colai.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ujwal.colai.core.model.AIService
import com.ujwal.colai.core.model.Session

/**
 * Room SQLite Database for ColAI.
 * Houses persistence for [AIService] definitions and containerized [Session] metadata.
 */
@Database(
    entities = [
        AIService::class,
        Session::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun serviceDao(): ServiceDao
    abstract fun sessionDao(): SessionDao

    companion object {
        private const val DATABASE_NAME = "colai_database.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
