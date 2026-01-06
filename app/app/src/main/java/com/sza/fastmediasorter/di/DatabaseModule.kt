package com.sza.fastmediasorter.di

import android.content.Context
import androidx.room.Room
import com.sza.fastmediasorter.data.db.AppDatabase
import com.sza.fastmediasorter.data.db.dao.FileMetadataDao
import com.sza.fastmediasorter.data.db.dao.FileOperationHistoryDao
import com.sza.fastmediasorter.data.db.dao.NetworkCredentialsDao
import com.sza.fastmediasorter.data.db.dao.ResourceDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing database-related dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private const val DATABASE_NAME = "fastmediasorter_v2.db"

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            DATABASE_NAME
        )
            .fallbackToDestructiveMigration() // TODO: Add proper migrations for production
            .build()
    }

    @Provides
    fun provideResourceDao(database: AppDatabase): ResourceDao = database.resourceDao()

    @Provides
    fun provideNetworkCredentialsDao(database: AppDatabase): NetworkCredentialsDao = 
        database.networkCredentialsDao()

    @Provides
    fun provideFileMetadataDao(database: AppDatabase): FileMetadataDao = 
        database.fileMetadataDao()

    @Provides
    fun provideFileOperationHistoryDao(database: AppDatabase): FileOperationHistoryDao = 
        database.fileOperationHistoryDao()
}
