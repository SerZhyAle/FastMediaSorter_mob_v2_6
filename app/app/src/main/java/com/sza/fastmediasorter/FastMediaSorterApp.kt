package com.sza.fastmediasorter

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * Application class for FastMediaSorter.
 * Initializes Hilt dependency injection and Timber logging.
 */
@HiltAndroidApp
class FastMediaSorterApp : Application() {

    override fun onCreate() {
        super.onCreate()
        initializeLogging()
        Timber.d("FastMediaSorter initialized - Version ${BuildConfig.VERSION_NAME}")
    }

    private fun initializeLogging() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        // TODO: Add crash reporting tree for release builds (Firebase Crashlytics)
    }
}
