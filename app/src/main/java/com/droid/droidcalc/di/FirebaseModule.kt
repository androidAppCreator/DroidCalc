package com.droid.droidcalc.di

import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt Module for providing Firebase-related dependencies.
 * This module is installed in the [SingletonComponent], ensuring that Firebase services
 * like [FirebaseAuth] and [FirebaseFirestore] are provided as singletons
 * throughout the application lifecycle.
 *
 * @author DroidSwap
 */
@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    /**
     * Provides a singleton instance of [FirebaseAuth].
     * This is the entry point to the Firebase Authentication SDK.
     *
     * @return A singleton instance of [FirebaseAuth].
     */
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return Firebase.auth
    }

    /**
     * Provides a singleton instance of [FirebaseFirestore].
     * This is the entry point for accessing a Firebase Firestore database.
     *
     * @return A singleton instance of [FirebaseFirestore].
     */
    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return Firebase.firestore
    }

    // Add other Firebase service providers here if needed (e.g., FirebaseStorage, FirebaseFunctions).
}
