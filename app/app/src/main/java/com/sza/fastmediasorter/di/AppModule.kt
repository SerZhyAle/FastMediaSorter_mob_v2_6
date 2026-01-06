package com.sza.fastmediasorter.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt module providing application-wide dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    // Application-wide dependencies will be added here
    // Examples: SharedPreferences, NetworkClient, EncryptedPreferences
}
