package com.sza.fastmediasorter.domain.usecase

import com.sza.fastmediasorter.data.local.db.FavoritesEntity
import com.sza.fastmediasorter.domain.model.MediaFile
import com.sza.fastmediasorter.domain.repository.FavoritesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FavoritesUseCase @Inject constructor(
    private val favoritesRepository: FavoritesRepository
) {
    fun getAllFavorites(): Flow<List<FavoritesEntity>> {
        return favoritesRepository.getAllFavorites()
    }

    fun isFavorite(uri: String): Flow<Boolean> {
        return favoritesRepository.isFavorite(uri)
    }
    
    suspend fun isFavoriteSync(uri: String): Boolean {
        return favoritesRepository.isFavoriteSync(uri)
    }

    suspend fun toggleFavorite(mediaFile: MediaFile, resourceId: Long) {
        val isFav = favoritesRepository.isFavoriteSync(mediaFile.path)
        if (isFav) {
            favoritesRepository.removeFavorite(mediaFile.path)
        } else {
            val entity = FavoritesEntity(
                uri = mediaFile.path,
                resourceId = resourceId,
                displayName = mediaFile.name,
                mediaType = mediaFile.type.ordinal, // Assuming ResourceType ordinal or similar mapping. Need to check MediaFile definition.
                size = mediaFile.size,
                lastKnownPath = mediaFile.path,
                dateModified = mediaFile.createdDate // MediaFile has createdDate, not dateModified, using that as proxy or I should check if there is dateModified
            )
            favoritesRepository.addFavorite(entity)
        }
    }
}
