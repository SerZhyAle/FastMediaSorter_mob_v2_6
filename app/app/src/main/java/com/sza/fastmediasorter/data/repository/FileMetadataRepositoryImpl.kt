package com.sza.fastmediasorter.data.repository

import com.sza.fastmediasorter.data.db.dao.FileMetadataDao
import com.sza.fastmediasorter.data.db.entity.FileMetadataEntity
import com.sza.fastmediasorter.domain.repository.FileMetadataRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of FileMetadataRepository using Room database.
 */
@Singleton
class FileMetadataRepositoryImpl @Inject constructor(
    private val fileMetadataDao: FileMetadataDao
) : FileMetadataRepository {

    override fun getFavoritesFlow(): Flow<List<String>> {
        return fileMetadataDao.getFavoritesFlow().map { entities ->
            entities.map { it.path }
        }
    }

    override suspend fun getFavorites(): List<String> {
        return fileMetadataDao.getFavorites().map { it.path }
    }

    override suspend fun isFavorite(path: String): Boolean {
        return fileMetadataDao.getByPath(path)?.isFavorite == true
    }

    override suspend fun toggleFavorite(path: String, resourceId: Long): Boolean {
        val existing = fileMetadataDao.getByPath(path)
        
        return if (existing != null) {
            val newFavoriteStatus = !existing.isFavorite
            fileMetadataDao.updateFavorite(path, newFavoriteStatus)
            newFavoriteStatus
        } else {
            // Create new entry with favorite = true
            val newEntity = FileMetadataEntity(
                resourceId = resourceId,
                path = path,
                name = path.substringAfterLast("/"),
                isFavorite = true
            )
            fileMetadataDao.insertOrUpdate(newEntity)
            true
        }
    }

    override suspend fun getPlaybackPosition(path: String): Long {
        return fileMetadataDao.getByPath(path)?.lastPlaybackPosition ?: 0L
    }

    override suspend fun savePlaybackPosition(path: String, position: Long) {
        val existing = fileMetadataDao.getByPath(path)
        
        if (existing != null) {
            fileMetadataDao.updatePlaybackPosition(path, position)
        } else {
            // Create new entry - we don't know the resourceId here, use 0
            val newEntity = FileMetadataEntity(
                resourceId = 0,
                path = path,
                name = path.substringAfterLast("/"),
                lastPlaybackPosition = position
            )
            fileMetadataDao.insertOrUpdate(newEntity)
        }
    }

    override suspend fun recordView(path: String) {
        fileMetadataDao.incrementViewCount(path)
    }
    
    override suspend fun deleteMetadataByResourcePath(resourcePath: String) {
        // Delete all metadata entries where the path starts with the resource path
        fileMetadataDao.deleteByPathPrefix(resourcePath)
    }
}
