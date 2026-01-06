package com.sza.fastmediasorter.di

import com.sza.fastmediasorter.data.repository.ResourceRepositoryImpl
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

    // TODO: Add other repository bindings as they are implemented
    // @Binds
    // abstract fun bindFileMetadataRepository(impl: FileMetadataRepositoryImpl): FileMetadataRepository
    
    // @Binds
    // abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}
