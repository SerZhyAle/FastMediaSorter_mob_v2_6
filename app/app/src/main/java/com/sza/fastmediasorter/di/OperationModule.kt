package com.sza.fastmediasorter.di

import android.content.Context
import com.sza.fastmediasorter.data.network.ftp.FtpClient
import com.sza.fastmediasorter.data.network.sftp.SftpClient
import com.sza.fastmediasorter.data.network.smb.SmbClient
import com.sza.fastmediasorter.data.operation.FtpOperationStrategy
import com.sza.fastmediasorter.data.operation.LocalOperationStrategy
import com.sza.fastmediasorter.data.operation.SftpOperationStrategy
import com.sza.fastmediasorter.data.operation.SmbOperationStrategy
import com.sza.fastmediasorter.domain.operation.FileOperationStrategy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
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

    /**
     * Provide the SMB/CIFS operation strategy.
     */
    @Provides
    @Singleton
    @Named("smb")
    fun provideSmbOperationStrategy(
        @ApplicationContext context: Context,
        smbClient: SmbClient
    ): SmbOperationStrategy = SmbOperationStrategy(context, smbClient)

    /**
     * Provide the SFTP operation strategy.
     */
    @Provides
    @Singleton
    @Named("sftp")
    fun provideSftpOperationStrategy(
        @ApplicationContext context: Context,
        sftpClient: SftpClient
    ): SftpOperationStrategy = SftpOperationStrategy(context, sftpClient)

    /**
     * Provide the FTP operation strategy.
     */
    @Provides
    @Singleton
    @Named("ftp")
    fun provideFtpOperationStrategy(
        @ApplicationContext context: Context,
        ftpClient: FtpClient
    ): FtpOperationStrategy = FtpOperationStrategy(context, ftpClient)
}
