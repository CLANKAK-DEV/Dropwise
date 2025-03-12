package com.example.dropwise

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [User::class, WaterIntake::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dropwise_db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration() // Temporary for testing
                    .build()
                INSTANCE = instance
                instance
            }
        }

        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP TABLE IF EXISTS water_intake")
                database.execSQL("DROP TABLE IF EXISTS users")

                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS users (
                        id TEXT PRIMARY KEY NOT NULL,
                        username TEXT NOT NULL,
                        email TEXT NOT NULL,
                        password TEXT,
                        birthday TEXT,
                        roomId TEXT NOT NULL DEFAULT ''
                    )
                """)

                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS water_intake (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId TEXT NOT NULL,
                        amount REAL NOT NULL,
                        date TEXT NOT NULL,
                        FOREIGN KEY (userId) REFERENCES users(id) ON DELETE CASCADE
                    )
                """)
            }
        }
    }
}