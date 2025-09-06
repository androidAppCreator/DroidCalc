package com.droid.droidcalc

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * Custom [Application] class for the DroidCalc application.
 * This class is annotated with [@HiltAndroidApp] to enable Hilt for dependency injection
 * throughout the application. It also initializes Timber for logging in debug builds.
 *
 * @author DroidSwap
 */
@HiltAndroidApp
class CalcApplication : Application() {

    /**
     * Called when the application is starting, before any other application objects have been created.
     * Initializes Timber for logging if the application is running in a debug build configuration.
     */
    override fun onCreate() {
        super.onCreate()

        // Initialize Timber for logging in Debug builds
        // In a real app, you might have different Timber trees for debug and release.
        // For example, a DebugTree that logs to Logcat and a ReleaseTree that logs to a crash reporting service.

            Timber.d("Timber logging NOT initialized for release build (or BuildConfig.DEBUG is false).")
    }
}
