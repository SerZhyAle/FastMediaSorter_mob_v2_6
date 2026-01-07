package com.sza.fastmediasorter.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sza.fastmediasorter.data.db.entity.FileMetadataEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for FileMetadataEntity.
 */
@Dao
interface FileMetadataDao {

    @Query("SELECT * FROM file_metadata WHERE path = :path")
    suspend fun getByPath(path: String): FileMetadataEntity?

    @Query("SELECT * FROM file_metadata WHERE resourceId = :resourceId")
    suspend fun getByResourceId(resourceId: Long): List<FileMetadataEntity>

    @Query("SELECT * FROM file_metadata WHERE isFavorite = 1 ORDER BY lastViewedDate DESC")
    fun getFavoritesFlow(): Flow<List<FileMetadataEntity>>

    @Query("SELECT * FROM file_metadata WHERE isFavorite = 1")
    suspend fun getFavorites(): List<FileMetadataEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(metadata: FileMetadataEntity): Long

    @Update
    suspend fun update(metadata: FileMetadataEntity)

    @Delete
    suspend fun delete(metadata: FileMetadataEntity)

    @Query("DELETE FROM file_metadata WHERE path = :path")
    suspend fun deleteByPath(path: String)

    @Query("UPDATE file_metadata SET isFavorite = :isFavorite WHERE path = :path")
    suspend fun updateFavorite(path: String, isFavorite: Boolean)

    @Query("UPDATE file_metadata SET lastPlaybackPosition = :position WHERE path = :path")
    suspend fun updatePlaybackPosition(path: String, position: Long)

    @Query("UPDATE file_metadata SET viewCount = viewCount + 1, lastViewedDate = :timestamp WHERE path = :path")
    suspend fun incrementViewCount(path: String, timestamp: Long = System.currentTimeMillis())
    
    @Query("DELETE FROM file_metadata WHERE path LIKE :pathPrefix || '%'")
    suspend fun deleteByPathPrefix(pathPrefix: String)
}
