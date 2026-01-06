package com.sza.fastmediasorter.di

import com.sza.fastmediasorter.data.operation.LocalOperationStrategy
import com.sza.fastmediasorter.domain.operation.FileOperationStrategy
import dagger.Binds
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
     * Provide the local file operation strategy.
     */
    @Provides
    @Singleton
    @Named("local")
    fun provideLocalOperationStrategy(): FileOperationStrategy = LocalOperationStrategy()

    // TODO: Add other protocol strategies as they are implemented
    // @Provides
    // @Named("smb")
    // fun provideSmbOperationStrategy(client: SmbClient): FileOperationStrategy = SmbOperationStrategy(client)
    
    // @Provides
    // @Named("sftp")
    // fun provideSftpOperationStrategy(client: SftpClient): FileOperationStrategy = SftpOperationStrategy(client)
}
