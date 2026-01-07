package com.sza.fastmediasorter.domain.usecase

import com.sza.fastmediasorter.domain.model.MediaFile
import com.sza.fastmediasorter.domain.model.Resource
import com.sza.fastmediasorter.domain.model.Result
import com.sza.fastmediasorter.domain.model.SearchFilter
import com.sza.fastmediasorter.domain.model.SearchResult
import com.sza.fastmediasorter.domain.repository.MediaRepository
import com.sza.fastmediasorter.domain.repository.ResourceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import javax.inject.Inject

/**
 * Use case for searching across all resources.
 * Performs parallel search across all resources and aggregates results.
 */
class GlobalSearchUseCase @Inject constructor(
    private val resourceRepository: ResourceRepository,
    private val mediaRepository: MediaRepository
) {
    
    /**
     * Executes a global search with the given filter.
     * Returns a flow of search results as they are found.
     */
    operator fun invoke(filter: SearchFilter): Flow<Result<List<SearchResult>>> = flow {
        try {
            emit(Result.Loading)
            
            // Get all resources
            val resources = resourceRepository.getAllResources()
            if (resources.isEmpty()) {
                emit(Result.Success(emptyList()))
                return@flow
            }
            
            Timber.d("Searching across ${resources.size} resources with filter: $filter")
            
            // Search each resource and collect results
            val allResults = mutableListOf<SearchResult>()
            
            for (resource in resources) {
                val files = mediaRepository.getFilesForResource(resource.id)
                
                val matchingFiles = files.filter { file ->
                    matchesFilter(file, filter)
                }
                
                matchingFiles.forEach { file ->
                    allResults.add(SearchResult(file, resource))
                }
                
                Timber.d("Found ${matchingFiles.size} matches in ${resource.name}")
            }
            
            Timber.d("Global search completed: ${allResults.size} total results")
            emit(Result.Success(allResults.sortedByDescending { it.file.date.time }))
            
        } catch (e: Exception) {
            Timber.e(e, "Global search failed")
            emit(Result.Error(e.message ?: "Search failed"))
        }
    }
    
    /**
     * Checks if a file matches the search filter criteria.
     */
    private fun matchesFilter(file: MediaFile, filter: SearchFilter): Boolean {
        // Query filter (case-insensitive substring match)
        if (filter.query.isNotBlank()) {
            if (!file.name.contains(filter.query, ignoreCase = true)) {
                return false
            }
        }
        
        // File type filter
        if (filter.fileTypes.isNotEmpty()) {
            if (file.type !in filter.fileTypes) {
                return false
            }
        }
        
        // Size filter
        filter.minSize?.let { minSize ->
            if (file.size < minSize) {
                return false
            }
        }
        
        filter.maxSize?.let { maxSize ->
            if (file.size > maxSize) {
                return false
            }
        }
        
        // Date filter
        filter.dateFrom?.let { dateFrom ->
            if (file.date.time < dateFrom) {
                return false
            }
        }
        
        filter.dateTo?.let { dateTo ->
            if (file.date.time > dateTo) {
                return false
            }
        }
        
        return true
    }
}
