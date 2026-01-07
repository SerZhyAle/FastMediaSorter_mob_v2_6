package com.sza.fastmediasorter.di

import com.sza.fastmediasorter.data.operation.LocalOperationStrategy
import com.sza.fastmediasorter.domain.operation.FileOperationStrategy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

/**
 * Hilt module for file operation strategies.
 * Provides different strategy implementations for different protocols.
 */
@Module
@InstallIn(SingletonComponent::class)
object OperationModule {

    /**
     * Provide the default file operation strategy (local).
     * This is used when no specific protocol is specified.
     */
    @Provides
    @Singleton
    fun provideDefaultOperationStrategy(): FileOperationStrategy = LocalOperationStrategy()

    /**
     * Provide the local file operation strategy.
     */
    @Provides
    @Singleton
    @Named("local")
    fun provideLocalOperationStrategy(): FileOperationStrategy = LocalOperationStrategy()

    // TODO: Network operation strategies (SMB, SFTP, FTP) will be implemented in future iterations
}
