package com.droid.droidcalc.di

import android.content.Context
import com.droid.droidcalc.data.db.AppDatabase
import com.droid.droidcalc.data.db.HistoryDao
import com.droid.droidcalc.data.repository.HistoryRepositoryImpl
import com.droid.droidcalc.domain.repository.HistoryRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

/**
 * Hilt Module for providing application-level dependencies.
 * This module is installed in the [SingletonComponent], meaning that all bindings
 * provided here will have a singleton scope throughout the application lifecycle.
 * It provides instances of the Room database, DAOs, repositories, and coroutine dispatchers.
 *
 * @author DroidSwap
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Provides a singleton instance of the Room [AppDatabase].
     *
     * @param context The application context provided by Hilt.
     * @return A singleton instance of [AppDatabase].
     */
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getInstance(context)
    }

    /**
     * Provides a singleton instance of the [HistoryDao].
     * This DAO is obtained from the [AppDatabase] instance.
     *
     * @param appDatabase The singleton [AppDatabase] instance provided by Hilt.
     * @return A singleton instance of [HistoryDao].
     */
    @Provides
    @Singleton
    fun provideHistoryDao(appDatabase: AppDatabase): HistoryDao {
        return appDatabase.historyDao()
    }

    /**
     * Provides a singleton instance of the [HistoryRepository].
     * This binds the [HistoryRepositoryImpl] implementation to the [HistoryRepository] interface.
     * It depends on [HistoryDao], [FirebaseFirestore], [FirebaseAuth], and an IO [CoroutineDispatcher].
     *
     * @param historyDao The singleton [HistoryDao] instance.
     * @param firestore The singleton [FirebaseFirestore] instance (provided by [FirebaseModule]).
     * @param firebaseAuth The singleton [FirebaseAuth] instance (provided by [FirebaseModule]).
     * @param ioDispatcher The IO [CoroutineDispatcher] for background tasks.
     * @return A singleton instance of [HistoryRepository] implemented by [HistoryRepositoryImpl].
     */
    @Provides
    @Singleton
    fun provideHistoryRepository(
        historyDao: HistoryDao,
        firestore: FirebaseFirestore, // Provided by FirebaseModule
        firebaseAuth: FirebaseAuth,   // Provided by FirebaseModule
        ioDispatcher: CoroutineDispatcher
    ): HistoryRepository {
        return HistoryRepositoryImpl(historyDao, firestore, firebaseAuth, ioDispatcher)
    }

    /**
     * Provides the IO [CoroutineDispatcher] for background tasks.
     * Using Dispatchers.IO is suitable for disk and network I/O operations.
     *
     * @return An instance of [CoroutineDispatcher] specifically for IO operations.
     */
    @Provides
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    // Add other application-wide singleton providers here as needed.
    // For example, SharedPreferences, DataStore, etc.
}
