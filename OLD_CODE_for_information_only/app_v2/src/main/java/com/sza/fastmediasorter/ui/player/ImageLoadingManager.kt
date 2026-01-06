package com.sza.fastmediasorter.ui.player

import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Handler
import androidx.core.view.isVisible
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.LifecycleCoroutineScope
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.signature.ObjectKey
import com.sza.fastmediasorter.R
import com.sza.fastmediasorter.databinding.ActivityPlayerUnifiedBinding
import com.sza.fastmediasorter.data.cloud.glide.GoogleDriveThumbnailData
import com.sza.fastmediasorter.data.network.glide.NetworkFileData
import com.sza.fastmediasorter.domain.model.MediaFile
import com.sza.fastmediasorter.domain.model.ResourceType
import com.sza.fastmediasorter.domain.repository.SettingsRepository
import com.sza.fastmediasorter.domain.usecase.SearchAudioCoverUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

/**
 * Manages image loading in PlayerActivity:
 * - Display images (Cloud/Network/Local) with Glide
 * - Preload adjacent images for faster navigation
 * - Show audio file info overlay
 * - Update audio format info from ExoPlayer
 */
class ImageLoadingManager(
    private val binding: ActivityPlayerUnifiedBinding,
    private val settingsRepository: SettingsRepository,
    private val searchAudioCoverUseCase: SearchAudioCoverUseCase,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val loadingIndicatorHandler: Handler,
    private val showLoadingIndicatorRunnable: Runnable,
    private val preloadJobs: MutableList<Job>,
    private val callback: ImageLoadingCallback
) {
    
    interface ImageLoadingCallback {
        fun isFinishing(): Boolean
        fun isDestroyed(): Boolean
        fun releasePlayer()
        fun showError(message: String, exception: Throwable? = null)
        fun showToast(message: String)
        fun getWindowManager(): android.view.WindowManager
        fun updateSlideShow()
        fun getAdjacentFiles(): List<MediaFile>
        fun getCurrentFile(): MediaFile?
        fun getCurrentResource(): com.sza.fastmediasorter.domain.model.MediaResource?
        fun getExoPlayer(): androidx.media3.exoplayer.ExoPlayer?
        fun getString(resId: Int): String
        fun isShowingCommandPanel(): Boolean
    }
    
    // Safety timeout to hide spinner if Glide hangs or cancels silently
    private val hideLoadingSafetyRunnable = Runnable {
        Timber.w("ImageLoadingManager.safetyTimeout: Loading took too long, hiding spinner")
        if (!callback.isDestroyed()) {
            binding.progressBar.isVisible = false
            callback.showToast(binding.root.context.getString(R.string.msg_loading_timeout))
        }
    }
    
    /**
     * Display image in ImageView or PhotoView based on settings
     */
    fun displayImage(path: String) {
        Timber.i("ImageLoadingManager.displayImage: START - path=$path")
        
        // Ensure any pending loading indicator from previous request is cancelled immediately
        loadingIndicatorHandler.removeCallbacks(showLoadingIndicatorRunnable)
        loadingIndicatorHandler.removeCallbacks(hideLoadingSafetyRunnable)
        if (!callback.isDestroyed()) {
            binding.progressBar.isVisible = false
        }
        
        // Skip if activity is being destroyed
        if (callback.isFinishing() || callback.isDestroyed()) {
            Timber.d("ImageLoadingManager.displayImage: Activity is finishing/destroyed, skipping image display")
            return
        }
        
        callback.releasePlayer()
        binding.playerView.isVisible = false
        
        // Hide audio-related views
        binding.audioCoverArtView.isVisible = false
        binding.audioTouchZonesOverlay.isVisible = false
        binding.audioInfoOverlay.isVisible = false
        binding.pdfControlsLayout.isVisible = false
        binding.textViewerContainer.isVisible = false
        
        // Hide text action buttons (they are for TXT files only)
        binding.btnCopyTextCmd.isVisible = false
        binding.btnEditTextCmd.isVisible = false
        binding.btnTranslateTextCmd.isVisible = false
        binding.btnSearchTextCmd.isVisible = false
        
        // Hide PDF action buttons (they are for PDF files only)
        binding.btnGoogleLensPdfCmd.isVisible = false
        binding.btnOcrPdfCmd.isVisible = false
        binding.btnTranslatePdfCmd.isVisible = false
        binding.btnSearchPdfCmd.isVisible = false
        
        // Hide EPUB action buttons (they are for EPUB files only)
        binding.btnSearchEpubCmd.isVisible = false
        binding.btnTranslateEpubCmd.isVisible = false
        
        // Hide EPUB WebView and controls (they are for EPUB files only)
        binding.epubWebView.isVisible = false
        binding.epubControlsLayout.isVisible = false
        binding.btnExitEpubFullscreen.isVisible = false

        // Schedule loading indicator to show after 1 second
        loadingIndicatorHandler.postDelayed(showLoadingIndicatorRunnable, 1000)
        // Schedule safety timeout (30 seconds)
        loadingIndicatorHandler.postDelayed(hideLoadingSafetyRunnable, 30000)

        val currentFile = callback.getCurrentFile()
        val resource = callback.getCurrentResource()
        
        // Get settings to determine which view to use
        lifecycleScope.launch {
            val settings = settingsRepository.getSettings().first()
            val usePhotoView = settings.loadFullSizeImages && currentFile != null
            
            Timber.d("ImageLoadingManager.displayImage: enableTranslation=${settings.enableTranslation}")
            
            // Switch visibility between ImageView and PhotoView
            binding.imageView.isVisible = !usePhotoView
            binding.photoView.isVisible = usePhotoView
            
            // Show action buttons in command panel only if enabled in settings
            binding.btnTranslateImageCmd.isVisible = settings.enableTranslation
            binding.btnGoogleLensImageCmd.isVisible = settings.enableGoogleLens
            binding.btnOcrImageCmd.isVisible = settings.enableOcr
            
            // Hide deprecated overlay buttons (moved to command panel)
            binding.btnTranslateImage.isVisible = false
            binding.btnGoogleLensImage.isVisible = false
            binding.btnOcrImage.isVisible = false
            
            Timber.d("ImageLoadingManager.displayImage: btnTranslateImage.isVisible=${binding.btnTranslateImage.isVisible}, btnTranslateImage.visibility=${binding.btnTranslateImage.visibility}")
            
            // Determine which touch zone overlay to show (only in command panel mode, NOT fullscreen)
            // In fullscreen mode, 9-zone gesture detection is handled by TouchZoneGestureManager
            val showCommandPanel = callback.isShowingCommandPanel()
            if (showCommandPanel) {
                // Command panel mode: show 2-zone or 3-zone overlay based on PhotoView usage
                binding.touchZonesOverlay.isVisible = !usePhotoView
                binding.touchZones3Overlay.isVisible = usePhotoView
            } else {
                // Fullscreen mode: hide all overlays, let imageTouchGestureDetector handle 9 zones
                binding.touchZonesOverlay.isVisible = false
                binding.touchZones3Overlay.isVisible = false
            }
            
            // Determine target view for image loading
            val targetView = if (usePhotoView) binding.photoView else binding.imageView
            
            // NOTE: Touch listeners are NOT set here anymore - they are configured once
            // in PlayerActivity.setupTouchZones() and must NOT be overwritten.
            // The imageTouchGestureDetector in PlayerActivity handles 9-zone touch detection.
            
            // Determine actual resource type from path prefix (for Favorites with mixed sources)
            val actualResourceType = when {
                path.startsWith("cloud://") -> ResourceType.CLOUD
                path.startsWith("smb://") -> ResourceType.SMB
                path.startsWith("sftp://") -> ResourceType.SFTP
                path.startsWith("ftp://") -> ResourceType.FTP
                else -> resource?.type ?: ResourceType.LOCAL
            }
            
            // Check if this is a cloud resource
            if (currentFile != null && actualResourceType == ResourceType.CLOUD) {
                loadCloudImage(path, currentFile, targetView, settings.loadFullSizeImages)
            } else if (currentFile != null && 
                (actualResourceType == ResourceType.SMB || actualResourceType == ResourceType.SFTP || actualResourceType == ResourceType.FTP)) {
                loadNetworkImage(path, currentFile, resource, targetView, settings.loadFullSizeImages)
            } else {
                loadLocalImage(path, currentFile, targetView, settings.loadFullSizeImages)
            }

            callback.updateSlideShow()
        }
    }
    
    private suspend fun loadCloudImage(
        path: String,
        currentFile: MediaFile,
        targetView: android.widget.ImageView,
        loadFullSize: Boolean
    ) {
        // Cloud image loading via GoogleDriveThumbnailModelLoader
        // Extract fileId from cloud path: cloud://googledrive/{fileId}
        val fileId = path.substringAfterLast("/")
        
        Timber.d("ImageLoadingManager: Loading cloud image - fileId = $fileId, path = $path")
        Timber.d("ImageLoadingManager: loadFullSizeImages = $loadFullSize")
        
        // Always load full image in player (never thumbnail)
        // Resolution limiting is done via Glide's override() if needed
        val thumbnailData = GoogleDriveThumbnailData(
            fileId = fileId,
            thumbnailUrl = currentFile.thumbnailUrl ?: "",
            loadFullImage = true  // Always load full image in player
        )
        
        Timber.d("ImageLoadingManager: Created GoogleDriveThumbnailData with loadFullImage = true")
        
        val glideRequest = Glide.with(binding.root.context)
            .load(thumbnailData)
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)  // Cache decoded image, not source stream
            .priority(Priority.IMMEDIATE)
        
        // Apply size limit if loadFullSizeImages is false (limit to 1920px max dimension)
        val finalRequest = if (!loadFullSize) {
            Timber.d("ImageLoadingManager: Loading cloud image with size limit: 1920px max dimension")
            glideRequest.override(1920, 1920)
        } else {
            // Load original size for zooming
            Timber.d("ImageLoadingManager: Loading cloud image at original size (no limit)")
            glideRequest
        }
        
        finalRequest
            .listener(createGlideListener())
            .into(targetView)
    }
    
    private suspend fun loadNetworkImage(
        path: String,
        currentFile: MediaFile,
        resource: com.sza.fastmediasorter.domain.model.MediaResource?,
        targetView: android.widget.ImageView,
        loadFullSize: Boolean
    ) {
        // Network image loading
        
        // Use NetworkFileData for Glide to load via NetworkFileModelLoader
        val networkData = NetworkFileData(
            path = path, 
            credentialsId = resource?.credentialsId, 
            loadFullImage = true,
            highPriority = true,
            size = currentFile.size,
            createdDate = currentFile.createdDate
        )
        val cacheKey = networkData.getCacheKey()
        
        val glideRequest = Glide.with(binding.root.context)
            .load(networkData)
            .signature(ObjectKey(cacheKey))
            .diskCacheStrategy(DiskCacheStrategy.ALL) // Cache both source and decoded for persistence
        
        // Apply size limit if loadFullSizeImages is false
        val finalRequest = if (!loadFullSize) {
            // Limit to screen size to save memory
            val bounds = callback.getWindowManager().currentWindowMetrics.bounds
            val screenWidth = bounds.width()
            val screenHeight = bounds.height()
            Timber.d("ImageLoadingManager: Loading image with screen size limit: ${screenWidth}x${screenHeight}")
            glideRequest.override(screenWidth, screenHeight)
        } else {
            // Load original size for zooming
            Timber.d("ImageLoadingManager: Loading image at original size (no limit)")
            glideRequest
        }
        
        finalRequest
            .listener(createGlideListener())
            .into(targetView)
    }
    
    private suspend fun loadLocalImage(
        path: String,
        currentFile: MediaFile?,
        targetView: android.widget.ImageView,
        loadFullSize: Boolean
    ) {
        // Local file - support both file:// paths and content:// URIs
        
        // Check file existence before loading
        val fileExists = if (path.startsWith("content://")) {
            try {
                val uri = Uri.parse(path)
                val docFile = DocumentFile.fromSingleUri(binding.root.context, uri)
                docFile?.exists() == true
            } catch (e: Exception) {
                Timber.e(e, "ImageLoadingManager: Error checking SAF URI existence: $path")
                false
            }
        } else {
            File(path).exists()
        }
        
        if (!fileExists) {
            Timber.w("ImageLoadingManager: File does not exist, showing error: $path")
            loadingIndicatorHandler.removeCallbacks(showLoadingIndicatorRunnable)
            if (!callback.isDestroyed()) {
                binding.progressBar.isVisible = false
                callback.showError(binding.root.context.getString(R.string.file_not_found_name, currentFile?.name ?: path))
            }
            return
        }
        
        val data = if (path.startsWith("content://")) {
            Uri.parse(path)
        } else {
            File(path)
        }
        val cacheKey = "${path}_${currentFile?.size}"
        
        val glideRequest = Glide.with(binding.root.context)
            .load(data)
            .signature(ObjectKey(cacheKey))
            .diskCacheStrategy(DiskCacheStrategy.DATA)
        
        // Apply size limit if loadFullSizeImages is false
        val finalRequest = if (!loadFullSize) {
            // Limit to screen size to save memory
            val bounds = callback.getWindowManager().currentWindowMetrics.bounds
            val screenWidth = bounds.width()
            val screenHeight = bounds.height()
            Timber.d("ImageLoadingManager: Loading local image with screen size limit: ${screenWidth}x${screenHeight}")
            glideRequest.override(screenWidth, screenHeight)
        } else {
            // Load original size for zooming
            Timber.d("ImageLoadingManager: Loading local image at original size (no limit)")
            glideRequest
        }
        
        finalRequest
            .listener(createGlideListener())
            .into(targetView)
    }
    
    private fun createGlideListener(): RequestListener<Drawable> {
        return object : RequestListener<Drawable> {
            override fun onLoadFailed(
                e: GlideException?,
                model: Any?,
                target: Target<Drawable>,
                isFirstResource: Boolean
            ): Boolean {
                Timber.e(e, "ImageLoadingManager.GlideListener: onLoadFailed triggered")
                loadingIndicatorHandler.removeCallbacks(showLoadingIndicatorRunnable)
                loadingIndicatorHandler.removeCallbacks(hideLoadingSafetyRunnable)
                if (!callback.isDestroyed()) {
                    binding.progressBar.isVisible = false
                }
                
                // Check if this is a race condition error from fast scrolling
                val isRaceConditionError = e?.rootCauses?.any { cause ->
                    val msg = cause.message ?: ""
                    msg.contains("memory mapping") ||
                    msg.contains("setDataSource failed") ||
                    msg.contains("cancelled")
                } == true
                
                if (isRaceConditionError) {
                    Timber.w("ImageLoadingManager: Race condition error during fast scrolling")
                    if (!callback.isDestroyed()) {
                        callback.showToast("Slow down! 🐢")
                    }
                } else {
                    Timber.e(e, "ImageLoadingManager: Failed to load image")
                    if (!callback.isDestroyed()) {
                        callback.showError("Failed to load image: ${e?.message}", e)
                    }
                }
                return false
            }
            
            override fun onResourceReady(
                resource: Drawable,
                model: Any,
                target: Target<Drawable>?,
                dataSource: DataSource,
                isFirstResource: Boolean
            ): Boolean {
                Timber.d("ImageLoadingManager.GlideListener: onResourceReady triggered")
                loadingIndicatorHandler.removeCallbacks(showLoadingIndicatorRunnable)
                loadingIndicatorHandler.removeCallbacks(hideLoadingSafetyRunnable)
                if (!callback.isDestroyed()) {
                    binding.progressBar.isVisible = false
                    preloadNextImageIfNeeded()
                }
                return false
            }
        }
    }

    /**
     * Preload adjacent images (previous + next) in background for faster navigation.
     * Only preloads IMAGE and GIF files.
     * Supports circular navigation.
     */
    fun preloadNextImageIfNeeded() {
        val adjacentFiles = callback.getAdjacentFiles()
        if (adjacentFiles.isEmpty()) {
            Timber.d("ImageLoadingManager: Preload skipped - no adjacent files")
            return
        }
        
        val resource = callback.getCurrentResource() ?: run {
            Timber.d("ImageLoadingManager: Preload skipped - no current resource")
            return
        }
        
        Timber.d("ImageLoadingManager: Starting preload for ${adjacentFiles.size} adjacent files")
        
        // Preload each adjacent file
        adjacentFiles.forEach { file ->
            Timber.d("ImageLoadingManager: Preloading ${file.name} (${file.type})")
            val job = lifecycleScope.launch {
                // Determine actual resource type from path prefix (for Favorites with mixed sources)
                val actualResourceType = when {
                    file.path.startsWith("cloud://") -> ResourceType.CLOUD
                    file.path.startsWith("smb://") -> ResourceType.SMB
                    file.path.startsWith("sftp://") -> ResourceType.SFTP
                    file.path.startsWith("ftp://") -> ResourceType.FTP
                    else -> resource.type
                }
                
                // Check if this is a network resource
                if (actualResourceType == ResourceType.SMB || actualResourceType == ResourceType.SFTP || actualResourceType == ResourceType.FTP) {
                    val networkData = NetworkFileData(
                        path = file.path,
                        credentialsId = resource.credentialsId,
                        loadFullImage = true,
                        size = file.size,
                        createdDate = file.createdDate
                    )
                    
                    // Preload with Glide (will cache to disk)
                    Glide.with(binding.root.context)
                        .load(networkData)
                        .diskCacheStrategy(DiskCacheStrategy.RESOURCE) // Cache decoded data only (InputStream can't be cached as source)
                        .preload()
                } else if (actualResourceType == ResourceType.CLOUD) {
                    // Preload cloud file
                    val fileId = file.path.substringAfterLast('/')
                    val cloudData = GoogleDriveThumbnailData(
                        thumbnailUrl = file.thumbnailUrl ?: "",
                        fileId = fileId,
                        loadFullImage = true
                    )
                    
                    Glide.with(binding.root.context)
                        .load(cloudData)
                        .diskCacheStrategy(DiskCacheStrategy.RESOURCE) // Cache decoded data only
                        .preload()
                } else {
                    // Preload local file
                    Glide.with(binding.root.context)
                        .load(File(file.path))
                        .diskCacheStrategy(DiskCacheStrategy.DATA)
                        .preload()
                }
                Timber.d("ImageLoadingManager: Preload completed for ${file.name}")
            }
            preloadJobs.add(job)
        }
        Timber.d("ImageLoadingManager: Preload initiated for ${adjacentFiles.size} files")
    }
    
    /**
     * Show audio file info overlay with size, duration, format
     */
    fun showAudioFileInfo(file: MediaFile?) {
        if (file == null) return
        
        binding.audioInfoOverlay.isVisible = true
        
        // Display file name without extension
        binding.audioFileName.text = file.name.substringBeforeLast('.', file.name)
        
        // Get file info asynchronously (size, duration, format)
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Use file.size from MediaFile (already populated during scan)
                val fileSize = file.size
                
                val fileSizeStr = if (fileSize > 0) {
                    com.sza.fastmediasorter.core.util.formatFileSize(fileSize)
                } else "N/A"
                
                withContext(Dispatchers.Main) {
                    binding.audioFileInfo.text = buildString {
                        append("Size: $fileSizeStr")
                        file.duration?.let { if (it > 0) append("\nDuration: ${formatDuration(it)}") }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to get audio file info")
                withContext(Dispatchers.Main) {
                    binding.audioFileInfo.text = callback.getString(R.string.file_info_unavailable)
                }
            }
        }
    }
    
    private fun formatDuration(millis: Long?): String {
        if (millis == null || millis <= 0) return "N/A"
        val seconds = millis / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        return if (hours > 0) {
            "%d:%02d:%02d".format(hours, minutes % 60, seconds % 60)
        } else {
            "%d:%02d".format(minutes, seconds % 60)
        }
    }
    
    /**
     * Update audio format info from ExoPlayer tracks
     */
    fun updateAudioFormatInfo() {
        val formatInfo = callback.getExoPlayer()?.currentTracks?.groups?.firstOrNull { group ->
            group.type == androidx.media3.common.C.TRACK_TYPE_AUDIO
        }?.let { audioGroup ->
            val format = audioGroup.getTrackFormat(0)
            buildString {
                format.sampleMimeType?.let { 
                    append(it.substringAfter("audio/").uppercase())
                }
                format.sampleRate.let { 
                    if (isNotEmpty()) append(" • ")
                    append("${it / 1000} kHz")
                }
                format.channelCount.let {
                    if (isNotEmpty()) append(" • ")
                    append(when (it) {
                        1 -> "Mono"
                        2 -> "Stereo"
                        else -> "$it channels"
                    })
                }
                format.bitrate.let {
                    if (it > 0) {
                        if (isNotEmpty()) append(" • ")
                        append("${it / 1000} kbps")
                    }
                }
            }
        }
        
        if (!formatInfo.isNullOrEmpty()) {
            // Update only the format line, preserve size and duration
            val currentText = binding.audioFileInfo.text.toString()
            val lines = currentText.split("\n").toMutableList()
            
            // Replace or add format info line
            if (lines.size >= 3) {
                lines[2] = formatInfo
            } else {
                lines.add(formatInfo)
            }
            
            binding.audioFileInfo.text = lines.joinToString("\n")
        }
    }
    
    /**
     * Load audio cover art with fallback to iTunes Search API
     * Called when ExoPlayer is ready for audio files
     */
    fun loadAudioCoverArt(file: MediaFile) {
        Timber.d("loadAudioCoverArt() called for file: ${file.name}")
        
        val isNetworkFile = file.path.startsWith("smb://") || file.path.startsWith("sftp://") || file.path.startsWith("ftp://")
        
        // For network files, check if ExoPlayer has embedded artwork first
        // ExoPlayer can extract artwork from network streams, but MediaMetadataRetriever cannot
        if (isNetworkFile) {
            // Delay to let ExoPlayer load metadata and artwork
            lifecycleScope.launch {
                delay(1500) // Wait for ExoPlayer to extract artwork
                
                // Check if ExoPlayer's PlayerView is showing artwork
                val player = binding.playerView.player
                val hasExoPlayerArtwork = player?.mediaMetadata?.artworkData != null ||
                        player?.mediaMetadata?.artworkUri != null
                
                Timber.d("Network file artwork check: hasExoPlayerArtwork=$hasExoPlayerArtwork")
                
                if (hasExoPlayerArtwork) {
                    // ExoPlayer has artwork, hide our overlay to show ExoPlayer's artwork
                    Timber.d("ExoPlayer has embedded artwork, hiding audioCoverArtView")
                    binding.audioCoverArtView.isVisible = false
                } else {
                    // ExoPlayer doesn't have artwork, search online
                    Timber.d("ExoPlayer has no artwork, searching online for ${file.name}")
                    binding.audioCoverArtView.isVisible = true
                    searchOnlineAndDisplayCover(file)
                }
            }
            return
        }
        
        // For local files, use MediaMetadataRetriever
        binding.audioCoverArtView.isVisible = true
        
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Try to extract embedded cover art using MediaMetadataRetriever
                Timber.d("Extracting embedded cover art for ${file.name}")
                val coverBitmap = withContext(Dispatchers.IO) {
                    try {
                        val retriever = android.media.MediaMetadataRetriever()
                        retriever.setDataSource(file.path)
                        
                        // Try to get embedded picture
                        val embeddedPicture = retriever.embeddedPicture
                        
                        // Debug: check if file has any metadata
                        val hasAudio = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO)
                        val mimeType = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
                        Timber.d("MediaMetadataRetriever: hasAudio=$hasAudio, mimeType=$mimeType, embeddedPicture=${embeddedPicture?.size} bytes")
                        
                        retriever.release()
                        
                        if (embeddedPicture != null) {
                            Timber.d("Found embedded cover art, decoding bitmap (${embeddedPicture.size} bytes)")
                            android.graphics.BitmapFactory.decodeByteArray(embeddedPicture, 0, embeddedPicture.size)
                        } else {
                            Timber.d("No embedded cover art found")
                            null
                        }
                    } catch (e: Exception) {
                        Timber.w(e, "Failed to extract embedded cover art")
                        null
                    }
                }
                
                withContext(Dispatchers.Main) {
                    if (coverBitmap != null) {
                        // Show embedded cover
                        Timber.d("Displaying embedded cover art for ${file.name}")
                        binding.audioCoverArtView.setImageBitmap(coverBitmap)
                    } else {
                        // No embedded cover, try online search
                        Timber.d("No embedded cover art, searching online for ${file.name}")
                        
                        val coverUrl = withContext(Dispatchers.IO) {
                            Timber.d("Calling searchAudioCoverUseCase for: ${file.name}")
                            searchAudioCoverUseCase(file.name)
                        }
                        
                        if (coverUrl != null) {
                            Timber.d("Found cover art online: $coverUrl")
                            Timber.d("Loading cover art into audioCoverArtView via Glide")
                            Glide.with(binding.audioCoverArtView.context)
                                .load(coverUrl)
                                .placeholder(R.drawable.ic_music_note)
                                .error(R.drawable.ic_music_note)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .into(binding.audioCoverArtView)
                            Timber.d("Cover art loaded successfully")
                        } else {
                            Timber.d("No cover art found online, using placeholder for ${file.name}")
                            binding.audioCoverArtView.setImageResource(R.drawable.ic_music_note)
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load audio cover art for ${file.name}")
                withContext(Dispatchers.Main) {
                    binding.audioCoverArtView.setImageResource(R.drawable.ic_music_note)
                }
            }
        }
    }
    
    /**
     * Search for cover art online and display it
     */
    private fun searchOnlineAndDisplayCover(file: MediaFile) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val coverUrl = searchAudioCoverUseCase(file.name)
                
                withContext(Dispatchers.Main) {
                    if (coverUrl != null) {
                        Timber.d("Found cover art online: $coverUrl")
                        Glide.with(binding.audioCoverArtView.context)
                            .load(coverUrl)
                            .placeholder(R.drawable.ic_music_note)
                            .error(R.drawable.ic_music_note)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(binding.audioCoverArtView)
                    } else {
                        Timber.d("No cover art found online, using placeholder for ${file.name}")
                        binding.audioCoverArtView.setImageResource(R.drawable.ic_music_note)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to search cover art online for ${file.name}")
                withContext(Dispatchers.Main) {
                    binding.audioCoverArtView.setImageResource(R.drawable.ic_music_note)
                }
            }
        }
    }

    fun updateButtonVisibility() {
        lifecycleScope.launch(Dispatchers.IO) {
            val settings = settingsRepository.getSettings().first()
            withContext(Dispatchers.Main) {
                // Only update if currently showing an image (not PDF/Video)
                if (binding.imageView.isVisible || binding.photoView.isVisible) {
                    // Only HIDE buttons if feature is disabled in settings
                    // CommandPanelController controls showing buttons based on orientation
                    if (!settings.enableTranslation) binding.btnTranslateImageCmd.isVisible = false
                    if (!settings.enableGoogleLens) binding.btnGoogleLensImageCmd.isVisible = false
                    if (!settings.enableOcr) binding.btnOcrImageCmd.isVisible = false
                    
                    // Hide deprecated overlay buttons
                    binding.btnTranslateImage.isVisible = false
                    binding.btnGoogleLensImage.isVisible = false
                    binding.btnOcrImage.isVisible = false
                    
                    Timber.d("ImageLoadingManager: Force updated button visibility. Lens=${settings.enableGoogleLens}, OCR=${settings.enableOcr}")
                }
            }
        }
    }
}
