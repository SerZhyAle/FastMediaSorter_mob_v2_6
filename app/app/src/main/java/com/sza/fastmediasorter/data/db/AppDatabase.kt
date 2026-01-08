package com.sza.fastmediasorter.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.sza.fastmediasorter.data.db.dao.FileMetadataDao
import com.sza.fastmediasorter.data.db.dao.FileOperationHistoryDao
import com.sza.fastmediasorter.data.db.dao.NetworkCredentialsDao
import com.sza.fastmediasorter.data.db.dao.ResourceDao
import com.sza.fastmediasorter.data.db.entity.FileMetadataEntity
import com.sza.fastmediasorter.data.db.entity.FileOperationHistoryEntity
import com.sza.fastmediasorter.data.db.entity.NetworkCredentialsEntity
import com.sza.fastmediasorter.data.db.entity.ResourceEntity

/**
 * Room Database for FastMediaSorter.
 * 
 * Version History:
 * - v1: Initial schema with Resources, Credentials, FileMetadata, OperationHistory
 * - v2: Added isReadOnly to ResourceEntity
 * - v3: Added pinCode and supportedMediaTypes to ResourceEntity
 */
@Database(
    entities = [
        ResourceEntity::class,
        NetworkCredentialsEntity::class,
        FileMetadataEntity::class,
        FileOperationHistoryEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun resourceDao(): ResourceDao
    
    abstract fun networkCredentialsDao(): NetworkCredentialsDao
    
    abstract fun fileMetadataDao(): FileMetadataDao
    
    abstract fun fileOperationHistoryDao(): FileOperationHistoryDao

    companion object {
        const val DATABASE_NAME = "fastmediasorter_v2.db"
    }
}
