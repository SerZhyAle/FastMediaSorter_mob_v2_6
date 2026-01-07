package com.sza.fastmediasorter.di

import com.sza.fastmediasorter.data.network.ftp.FtpClient
import com.sza.fastmediasorter.data.network.sftp.SftpClient
import com.sza.fastmediasorter.data.network.smb.SmbClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for network protocol clients.
 * Provides SMB, SFTP, and FTP clients for network file operations.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * Provide the SMB/CIFS client.
     * Uses SMBJ library for SMB2/SMB3 protocol support.
     */
    @Provides
    @Singleton
    fun provideSmbClient(): SmbClient = SmbClient()

    /**
     * Provide the SFTP client.
     * Uses SSHJ library for secure file transfer over SSH.
     */
    @Provides
    @Singleton
    fun provideSftpClient(): SftpClient = SftpClient()

    /**
     * Provide the FTP client.
     * Uses Apache Commons Net for FTP protocol support.
     */
    @Provides
    @Singleton
    fun provideFtpClient(): FtpClient = FtpClient()
}
