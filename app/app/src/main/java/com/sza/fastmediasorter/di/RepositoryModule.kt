package com.sza.fastmediasorter.di

import com.sza.fastmediasorter.data.repository.FileMetadataRepositoryImpl
import com.sza.fastmediasorter.data.repository.MediaRepositoryImpl
import com.sza.fastmediasorter.data.repository.PreferencesRepositoryImpl
import com.sza.fastmediasorter.data.repository.ResourceRepositoryImpl
import com.sza.fastmediasorter.domain.repository.FileMetadataRepository
import com.sza.fastmediasorter.domain.repository.MediaRepository
import com.sza.fastmediasorter.domain.repository.PreferencesRepository
import com.sza.fastmediasorter.domain.repository.ResourceRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for binding repository interfaces to implementations.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindResourceRepository(
        impl: ResourceRepositoryImpl
    ): ResourceRepository

    @Binds
    @Singleton
    abstract fun bindMediaRepository(
        impl: MediaRepositoryImpl
    ): MediaRepository

    @Binds
    @Singleton
    abstract fun bindPreferencesRepository(
        impl: PreferencesRepositoryImpl
    ): PreferencesRepository

    @Binds
    @Singleton
    abstract fun bindFileMetadataRepository(
        impl: FileMetadataRepositoryImpl
    ): FileMetadataRepository
}
