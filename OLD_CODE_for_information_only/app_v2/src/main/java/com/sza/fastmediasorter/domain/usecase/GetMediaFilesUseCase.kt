package com.sza.fastmediasorter.domain.usecase

import com.sza.fastmediasorter.domain.model.MediaFile
import com.sza.fastmediasorter.domain.model.MediaResource
import com.sza.fastmediasorter.domain.model.MediaType
import com.sza.fastmediasorter.domain.model.SortMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import com.sza.fastmediasorter.domain.repository.FavoritesRepository

data class SizeFilter(
    val imageSizeMin: Long,
    val imageSizeMax: Long,
    val videoSizeMin: Long,
    val videoSizeMax: Long,
    val audioSizeMin: Long,
    val audioSizeMax: Long
)

data class MediaFilePage(
    val files: List<MediaFile>,
    val hasMore: Boolean
)

interface MediaScanner {
    suspend fun scanFolder(
        path: String,
        supportedTypes: Set<MediaType>,
        sizeFilter: SizeFilter? = null,
        credentialsId: String? = null,
        scanSubdirectories: Boolean = true,
        onProgress: ScanProgressCallback? = null
    ): List<MediaFile>
    
    /**
     * Scan folder with pagination support.
     * @param offset Starting position (0-based)
     * @param limit Maximum number of files to return
     * @param scanSubdirectories Whether to scan subdirectories recursively
     * @return MediaFilePage with files and hasMore flag
     */
    suspend fun scanFolderPaged(
        path: String,
        supportedTypes: Set<MediaType>,
        sizeFilter: SizeFilter? = null,
        offset: Int,
        limit: Int,
        credentialsId: String? = null,
        scanSubdirectories: Boolean = true
    ): MediaFilePage
    
    suspend fun getFileCount(
        path: String,
        supportedTypes: Set<MediaType>,
        sizeFilter: SizeFilter? = null,
        credentialsId: String? = null,
        scanSubdirectories: Boolean = true
    ): Int
    
    suspend fun isWritable(path: String, credentialsId: String? = null): Boolean
}



class GetMediaFilesUseCase @Inject constructor(
    private val mediaScannerFactory: MediaScannerFactory,
    private val favoritesRepository: FavoritesRepository
) {
    companion object {
        private const val LARGE_FOLDER_THRESHOLD = 1000
    }
    
    operator fun invoke(
        resource: MediaResource,
        sortMode: SortMode = SortMode.NAME_ASC,
        sizeFilter: SizeFilter? = null,
        useChunkedLoading: Boolean = false,
        maxFiles: Int = 100,
        onProgress: ScanProgressCallback? = null
    ): Flow<List<MediaFile>> = flow {
        timber.log.Timber.d("GetMediaFilesUseCase: START invoke - resource='${resource.name}' (id=${resource.id}), type=${resource.type}")
        timber.log.Timber.d("GetMediaFilesUseCase: path='${resource.path}', useChunked=$useChunkedLoading, sortMode=$sortMode")
        
        // Handle virtual Favorites resource
        if (resource.id == -100L) {
            timber.log.Timber.d("GetMediaFilesUseCase: Loading Favorites from repository")
            val favorites = favoritesRepository.getAllFavorites().first()
            val favoriteFiles = favorites.map { entity ->
                MediaFile(
                    path = entity.uri,
                    name = entity.displayName,
                    type = MediaType.entries.getOrElse(entity.mediaType) { MediaType.IMAGE },
                    size = entity.size,
                    createdDate = entity.dateModified,
                    resourceId = entity.resourceId,
                    isFavorite = true,
                    // Default values for other fields
                    width = 0, height = 0, duration = 0
                )
            }
            // Apply sorting
            val sortedFavorites = sortFiles(favoriteFiles, sortMode)
            emit(sortedFavorites)
            return@flow
        }
        
        val scanner = mediaScannerFactory.getScanner(resource.type)
        
        timber.log.Timber.d("GetMediaFilesUseCase: Got scanner type=${scanner.javaClass.simpleName}")
        
        timber.log.Timber.d("GetMediaFilesUseCase: Calling scanner.scanFolder with supportedTypes=${resource.supportedMediaTypes.map { it.name }}")
        val files = if (useChunkedLoading && scanner is com.sza.fastmediasorter.data.network.SmbMediaScanner) {
            timber.log.Timber.d("GetMediaFilesUseCase: Using chunked loading, maxFiles=$maxFiles")
            // Use chunked loading for SMB to quickly show first files
            scanner.scanFolderChunked(
                path = resource.path,
                supportedTypes = resource.supportedMediaTypes,
                sizeFilter = sizeFilter,
                maxFiles = maxFiles,
                credentialsId = resource.credentialsId,
                scanSubdirectories = resource.scanSubdirectories
            )
        } else {
            timber.log.Timber.d("GetMediaFilesUseCase: Using standard loading")
            // Standard full scan for other types with progress reporting
            scanner.scanFolder(
                path = resource.path,
                supportedTypes = resource.supportedMediaTypes,
                sizeFilter = sizeFilter,
                credentialsId = resource.credentialsId,
                scanSubdirectories = resource.scanSubdirectories,
                onProgress = onProgress
            )
        }
        
        timber.log.Timber.d("GetMediaFilesUseCase: Scanner returned ${files.size} files")
        
        // Fetch favorites to mark files
        val favoriteUris = try {
            favoritesRepository.getAllFavorites().first().map { it.uri }.toSet()
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Failed to fetch favorites")
            emptySet<String>()
        }
        
        val filesWithFavorites = files.map { file ->
            // Populate resourceId if missing (it should be missing from scanner)
            val fileWithResource = if (file.resourceId == null) {
                file.copy(resourceId = resource.id)
            } else {
                file
            }
            
            if (favoriteUris.contains(fileWithResource.path)) {
                fileWithResource.copy(isFavorite = true)
            } else {
                fileWithResource
            }
        }
        
        // Skip sorting for large folders (> 1000 files) - improves performance
        val sortedFiles = if (filesWithFavorites.size > LARGE_FOLDER_THRESHOLD) {
            timber.log.Timber.d("GetMediaFilesUseCase: Large folder (${filesWithFavorites.size} files), skipping sort for better performance")
            filesWithFavorites  // Return unsorted for large folders
        } else {
            timber.log.Timber.d("GetMediaFilesUseCase: Small folder (${filesWithFavorites.size} files), sorting by $sortMode")
            sortFiles(filesWithFavorites, sortMode)
        }
        
        timber.log.Timber.d("GetMediaFilesUseCase: Emitting ${sortedFiles.size} files to flow")
        emit(sortedFiles)
        
        timber.log.Timber.d("GetMediaFilesUseCase: COMPLETE - flow emission done")
    }.flowOn(Dispatchers.IO) // Execute scanning and sorting on IO thread

    private fun sortFiles(files: List<MediaFile>, mode: SortMode): List<MediaFile> {
        return when (mode) {
            SortMode.MANUAL -> files // Keep original order for manual mode
            SortMode.NAME_ASC -> files.sortedBy { it.name.lowercase() }
            SortMode.NAME_DESC -> files.sortedByDescending { it.name.lowercase() }
            SortMode.DATE_ASC -> files.sortedBy { it.createdDate }
            SortMode.DATE_DESC -> files.sortedByDescending { it.createdDate }
            SortMode.SIZE_ASC -> files.sortedBy { it.size }
            SortMode.SIZE_DESC -> files.sortedByDescending { it.size }
            SortMode.TYPE_ASC -> files.sortedBy { it.type.ordinal }
            SortMode.TYPE_DESC -> files.sortedByDescending { it.type.ordinal }
            SortMode.RANDOM -> files.shuffled() // Random order for slideshows
        }
    }
}
