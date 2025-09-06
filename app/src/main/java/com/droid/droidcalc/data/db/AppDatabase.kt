package com.droid.droidcalc.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * The main Room database for the DroidCalc application.
 * This class is annotated with [@Database] and lists all the entities that belong to this database
 * and the version of the database. It provides abstract access to the Data Access Objects (DAOs).
 *
 * The database instance is typically created as a singleton. While Hilt will be used for providing
 * this database or its DAOs, this file includes a traditional singleton pattern for completeness
 * or for scenarios where direct instantiation might be needed outside Hilt's direct scope (e.g., migrations).
 *
 * @property historyDao Provides access to the [HistoryDao] for calculation history operations.
 * @author DroidSwap
 */
@Database(entities = [HistoryEntity::class], version = 1, exportSchema = false) // exportSchema should be true for production with schema files
abstract class AppDatabase : RoomDatabase() {

    /**
     * Abstract method to get the Data Access Object for [HistoryEntity].
     *
     * @return An instance of [HistoryDao].
     */
    abstract fun historyDao(): HistoryDao

    companion object {
        /**
         * The name of the Room database file.
         */
        private const val DATABASE_NAME = "droidcalc_database"

        /**
         * Volatile instance of the AppDatabase to ensure atomic access to the variable.
         * This is used to implement the singleton pattern.
         */
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Gets the singleton instance of the [AppDatabase].
         * If the instance does not exist, it creates the database.
         * This method is thread-safe.
         *
         * @param context The application context.
         * @return The singleton [AppDatabase] instance.
         */
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                // TODO: Add database migrations if schema changes in the future.
                // .addMigrations(MIGRATION_1_2, MIGRATION_2_3) // Example
                .fallbackToDestructiveMigration() // Use this only during development if you don't want to provide migrations
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
