package com.sza.fastmediasorter.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.sza.fastmediasorter.domain.model.MediaFile
import com.sza.fastmediasorter.domain.model.MediaType
import com.sza.fastmediasorter.domain.model.SortMode
import timber.log.Timber
import java.io.File
import java.util.Date

/**
 * PagingSource for loading media files in pages.
 * Provides pagination support for large file collections.
 * 
 * @param directoryPath The directory to scan for media files
 * @param sortMode The sort mode to apply
 */
class MediaFilePagingSource(
    private val directoryPath: String,
    private val sortMode: SortMode
) : PagingSource<Int, MediaFile>() {

    companion object {
        private const val PAGE_SIZE = 50 // Load 50 files per page
        private val SUPPORTED_IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "gif", "bmp", "webp")
        private val SUPPORTED_VIDEO_EXTENSIONS = setOf("mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "m4v", "3gp")
        private val SUPPORTED_AUDIO_EXTENSIONS = setOf("mp3", "wav", "flac", "aac", "ogg", "m4a", "wma", "opus")
    }

    /**
     * All files in the directory, sorted according to sortMode.
     * Lazy-initialized and cached.
     */
    private val allFiles: List<MediaFile> by lazy {
        loadAndSortFiles()
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MediaFile> {
        return try {
            val page = params.key ?: 0
            val startIndex = page * PAGE_SIZE
            val endIndex = minOf(startIndex + PAGE_SIZE, allFiles.size)

            if (startIndex >= allFiles.size) {
                // No more data
                return LoadResult.Page(
                    data = emptyList(),
                    prevKey = if (page > 0) page - 1 else null,
                    nextKey = null
                )
            }

            val pageData = allFiles.subList(startIndex, endIndex)

            LoadResult.Page(
                data = pageData,
                prevKey = if (page > 0) page - 1 else null,
                nextKey = if (endIndex < allFiles.size) page + 1 else null
            )
        } catch (e: Exception) {
            Timber.e(e, "Error loading page")
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, MediaFile>): Int? {
        // Return the page number closest to the anchor position
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }

    /**
     * Load all files from directory and apply sorting.
     */
    private fun loadAndSortFiles(): List<MediaFile> {
        val directory = File(directoryPath)
        if (!directory.exists() || !directory.isDirectory) {
            Timber.w("Directory does not exist or is not a directory: $directoryPath")
            return emptyList()
        }

        val files = directory.listFiles()?.filter { file ->
            file.isFile && isMediaFile(file)
        }?.map { file ->
            MediaFile(
                path = file.absolutePath,
                name = file.name,
                size = file.length(),
                date = Date(file.lastModified()),
                type = getMediaType(file.extension.lowercase())
            )
        } ?: emptyList()

        return sortFiles(files, sortMode)
    }

    /**
     * Check if file is a supported media file.
     */
    private fun isMediaFile(file: File): Boolean {
        val extension = file.extension.lowercase()
        return extension in SUPPORTED_IMAGE_EXTENSIONS ||
                extension in SUPPORTED_VIDEO_EXTENSIONS ||
                extension in SUPPORTED_AUDIO_EXTENSIONS
    }

    /**
     * Determine media type from file extension.
     */
    private fun getMediaType(extension: String): MediaType {
        return when (extension) {
            in SUPPORTED_IMAGE_EXTENSIONS -> {
                if (extension == "gif") MediaType.GIF else MediaType.IMAGE
            }
            in SUPPORTED_VIDEO_EXTENSIONS -> MediaType.VIDEO
            in SUPPORTED_AUDIO_EXTENSIONS -> MediaType.AUDIO
            else -> MediaType.OTHER
        }
    }

    /**
     * Sort files according to the given sort mode.
     */
    private fun sortFiles(files: List<MediaFile>, sortMode: SortMode): List<MediaFile> {
        return when (sortMode) {
            SortMode.NAME_ASC -> files.sortedBy { it.name.lowercase() }
            SortMode.NAME_DESC -> files.sortedByDescending { it.name.lowercase() }
            SortMode.DATE_ASC -> files.sortedBy { it.date }
            SortMode.DATE_DESC -> files.sortedByDescending { it.date }
            SortMode.SIZE_ASC -> files.sortedBy { it.size }
            SortMode.SIZE_DESC -> files.sortedByDescending { it.size }
        }
    }
}
