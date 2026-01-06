package com.sza.fastmediasorter.ui.browse

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.Priority
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.signature.ObjectKey
import com.sza.fastmediasorter.BuildConfig
import com.sza.fastmediasorter.R
import timber.log.Timber
import com.sza.fastmediasorter.databinding.ItemMediaFileBinding
import com.sza.fastmediasorter.databinding.ItemMediaFileGridBinding
import com.sza.fastmediasorter.data.cloud.glide.GoogleDriveThumbnailData
import com.sza.fastmediasorter.data.network.glide.NetworkFileData
import com.sza.fastmediasorter.data.network.glide.NetworkFileDataFetcher
import com.sza.fastmediasorter.domain.model.MediaFile
import com.sza.fastmediasorter.domain.model.MediaType
import java.io.File
import java.util.Date

class MediaFileAdapter(
    private val onFileClick: (MediaFile) -> Unit,
    private val onFileLongClick: (MediaFile) -> Unit,
    private val onSelectionChanged: (MediaFile, Boolean) -> Unit,
    private val onSelectionRangeRequested: (MediaFile) -> Unit = {}, // Long click on checkbox
    private val onPlayClick: (MediaFile) -> Unit,
    private val onFavoriteClick: (MediaFile) -> Unit = {},
    private val onCopyClick: (MediaFile) -> Unit = {},
    private val onMoveClick: (MediaFile) -> Unit = {},
    private val onRenameClick: (MediaFile) -> Unit = {},
    private val onDeleteClick: (MediaFile) -> Unit = {},
    private var isGridMode: Boolean = false,
    private var thumbnailSize: Int = 96, // Default size in dp
    private val getShowVideoThumbnails: () -> Boolean = { false }, // Callback to get current setting
    private val getShowPdfThumbnails: () -> Boolean = { false }, // Callback to get PDF thumbnail setting
    private var disableThumbnails: Boolean = false // Skip thumbnail loading, show extension icons only
) : ListAdapter<MediaFile, RecyclerView.ViewHolder>(MediaFileDiffCallback()) {

    private var selectedPaths = setOf<String>()
    private var credentialsId: String? = null // Credentials ID for network files
    private var hasDestinations: Boolean = false
    private var isWritable: Boolean = false
    private var refreshVersion: Int = 0
    private var skipInitialThumbnailLoad = false // Control initial thumbnail loading
    private var showFavoriteButton: Boolean = true // Show/hide favorite button based on settings
    private var hideGridActionButtons: Boolean = false // Hide quick action buttons in grid mode
    
    fun incrementRefreshVersion() {
        refreshVersion++
    }
    
    fun setCredentialsId(id: String?) {
        credentialsId = id
    }
    
    fun setShowFavoriteButton(show: Boolean) {
        if (this.showFavoriteButton != show) {
            this.showFavoriteButton = show
            notifyDataSetChanged() // Update button visibility across all items
        }
    }
    
    fun setHideGridActionButtons(hide: Boolean) {
        if (this.hideGridActionButtons != hide) {
            this.hideGridActionButtons = hide
            notifyDataSetChanged() // Update button visibility across all items
        }
    }
    
    fun setResourcePermissions(hasDestinations: Boolean, isWritable: Boolean) {
        if (this.hasDestinations != hasDestinations || this.isWritable != isWritable) {
            this.hasDestinations = hasDestinations
            this.isWritable = isWritable
            notifyDataSetChanged() // Update button visibility across all items
        }
    }
    
    fun setDisableThumbnails(disabled: Boolean) {
        if (disableThumbnails != disabled) {
            disableThumbnails = disabled
            // Force rebind all items to switch between thumbnail/icon mode
            notifyDataSetChanged()
        }
    }
    
    /**
     * Enable/disable initial thumbnail loading in bind().
     * When true, thumbnails are loaded only via LOAD_THUMBNAILS payload.
     */
    fun setSkipInitialThumbnailLoad(skip: Boolean) {
        Timber.d("MediaFileAdapter: setSkipInitialThumbnailLoad($skip)")
        skipInitialThumbnailLoad = skip
    }
    
    fun getSkipInitialThumbnailLoad(): Boolean = skipInitialThumbnailLoad
    
    companion object {
        private const val VIEW_TYPE_LIST = 0
        private const val VIEW_TYPE_GRID = 1
        private const val PAYLOAD_VIEW_MODE_CHANGE = "view_mode_change"
        private const val CACHED_THUMBNAIL_SIZE = 300 // Fixed size for cache stability across List/Grid modes
        
        // PDF thumbnail size limits for network resources when "Large PDF Thumbnails" is ENABLED (bytes)
        private const val SMB_PDF_LARGE_MAX_SIZE = 50 * 1024 * 1024L // 50 MB for SMB
        private const val NETWORK_PDF_LARGE_MAX_SIZE = 10 * 1024 * 1024L // 10 MB for SFTP/FTP/Cloud
        
        // PDF thumbnail size limits when "Large PDF Thumbnails" is DISABLED (normal behavior)
        private const val SMB_PDF_NORMAL_MAX_SIZE = 3 * 1024 * 1024L // 3 MB for SMB
        private const val NETWORK_PDF_NORMAL_MAX_SIZE = 1 * 1024 * 1024L // 1 MB for SFTP/FTP/Cloud
        
        // EPUB cover size limits for network resources (same as PDF)
        private const val SMB_EPUB_MAX_SIZE = 50 * 1024 * 1024L // 50 MB for SMB
        private const val NETWORK_EPUB_MAX_SIZE = 10 * 1024 * 1024L // 10 MB for SFTP/FTP/Cloud

        /**
         * Check if GlideException is caused by video decoder failure.
         */
        private fun isVideoDecoderException(e: GlideException?): Boolean {
            if (e == null) return false
            
            var current: Throwable? = e
            var depth = 0
            while (current != null && depth < 10) {
                val msg = current.message?.lowercase() ?: ""
                val className = current.javaClass.simpleName.lowercase()
                
                if (className.contains("videodecoder") ||
                    className.contains("videodecoderexception") ||
                    msg.contains("mediametadataretriever") ||
                    msg.contains("failed to retrieve a frame")) {
                    return true
                }
                
                current = current.cause
                depth++
            }
            return false
        }
            }
        
        /**
         * Apply placeholder style (background color + reduced size for list)
         */
        private fun applyPlaceholderStyle(imageView: android.widget.ImageView, type: MediaType, isListMode: Boolean) {
            val context = imageView.context
            val colorRes = when (type) {
                MediaType.VIDEO -> R.color.thumbnail_video_bg
                MediaType.AUDIO -> R.color.thumbnail_audio_bg
                MediaType.TEXT, MediaType.PDF, MediaType.EPUB -> R.color.thumbnail_doc_bg
                else -> R.color.thumbnail_image_bg
            }
            imageView.setBackgroundColor(ContextCompat.getColor(context, colorRes))
            
            if (isListMode) {
                val density = context.resources.displayMetrics.density
                val smallSize = (32 * density).toInt()
                val params = imageView.layoutParams
                if (params.width != smallSize) {
                    params.width = smallSize
                    params.height = smallSize
                    imageView.layoutParams = params
                }
            }
        }
        
        private fun resetThumbnailStyle(imageView: android.widget.ImageView) {
            imageView.background = null
            // Restore normal thumbnail size (64dp for List mode)
            val context = imageView.context
            val density = context.resources.displayMetrics.density
            val normalSize = (64 * density).toInt()
            val params = imageView.layoutParams
            if (params.width != normalSize) {
                params.width = normalSize
                params.height = normalSize
                imageView.layoutParams = params
            }
        }

    
    fun setGridMode(enabled: Boolean, iconSize: Int = 96) {
        if (isGridMode != enabled || thumbnailSize != iconSize) {
            val modeChanged = isGridMode != enabled
            val sizeChanged = thumbnailSize != iconSize
            isGridMode = enabled
            thumbnailSize = iconSize
            
            // When thumbnail size changes, increment refresh version to force Glide reload
            if (sizeChanged) {
                incrementRefreshVersion()
                Timber.d("Thumbnail size changed to ${iconSize}dp, cache will be regenerated at new size")
            }
            
            // When mode changes (List↔Grid), use payload to rebind items efficiently
            // When only size changes, force thumbnail reload with payload
            if (sizeChanged && !modeChanged) {
                // Size changed: notify with LOAD_THUMBNAILS payload to force thumbnail reload
                notifyItemRangeChanged(0, itemCount, "LOAD_THUMBNAILS")
            } else {
                notifyItemRangeChanged(0, itemCount, PAYLOAD_VIEW_MODE_CHANGE)
            }
        }
    }

    fun setSelectedPaths(paths: Set<String>) {
        if (selectedPaths == paths) return
        
        val oldSelected = selectedPaths
        selectedPaths = paths
        
        // Optimize updates: only notify changed items
        // If selection was cleared
        if (paths.isEmpty() && oldSelected.isNotEmpty()) {
            currentList.forEachIndexed { index, file ->
                if (file.path in oldSelected) {
                    notifyItemChanged(index)
                }
            }
            return
        }
        
        // If selection was added/changed
        currentList.forEachIndexed { index, file ->
            val wasSelected = file.path in oldSelected
            val isSelected = file.path in paths
            if (wasSelected != isSelected) {
                notifyItemChanged(index)
            }
        }
    }
    
    override fun getItemViewType(position: Int): Int {
        return if (isGridMode) VIEW_TYPE_GRID else VIEW_TYPE_LIST
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_GRID -> {
                val binding = ItemMediaFileGridBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                GridViewHolder(binding)
            }
            else -> {
                val binding = ItemMediaFileBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                ListViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val file = getItem(position)
        when (holder) {
            is ListViewHolder -> holder.bind(file, selectedPaths)
            is GridViewHolder -> holder.bind(file, selectedPaths)
        }
    }
    
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
        val file = getItem(position)
        Timber.d("onBindViewHolder WITH PAYLOADS: position=$position, file=${file.name}, payloads=$payloads, isEmpty=${payloads.isEmpty()}, skipFlag=$skipInitialThumbnailLoad")
        
        if (payloads.isEmpty()) {
            // Standard full bind
            Timber.d("onBindViewHolder: payloads EMPTY, calling super (full bind) for ${file.name}")
            super.onBindViewHolder(holder, position, payloads)
        } else {
            // Handle multiple payloads - process each one
            if (payloads.contains("LOAD_THUMBNAILS")) {
                // Partial bind: only reload thumbnail without recreating the whole view
                Timber.d("onBindViewHolder: LOAD_THUMBNAILS payload detected for ${file.name}, calling loadThumbnailOnly")
                when (holder) {
                    is ListViewHolder -> {
                        Timber.d(">>> Calling ListViewHolder.loadThumbnailOnly for ${file.name}")
                        holder.loadThumbnailOnly(file)
                    }
                    is GridViewHolder -> {
                        Timber.d(">>> Calling GridViewHolder.loadThumbnailOnly for ${file.name}")
                        holder.loadThumbnailOnly(file)
                    }
                }
            }
            if (payloads.contains("FAVORITE_CHANGED")) {
                // Partial bind: only update favorite icon
                Timber.d("onBindViewHolder: FAVORITE_CHANGED payload detected for ${file.name}, updating icon only")
                when (holder) {
                    is ListViewHolder -> {
                        holder.itemView.findViewById<android.widget.ImageButton>(R.id.btnFavorite)?.setImageResource(
                            if (file.isFavorite) R.drawable.ic_star_filled else R.drawable.ic_star_outline
                        )
                    }
                    is GridViewHolder -> {
                        holder.itemView.findViewById<android.widget.ImageButton>(R.id.btnFavorite)?.setImageResource(
                            if (file.isFavorite) R.drawable.ic_star_filled else R.drawable.ic_star_outline
                        )
                    }
                }
            }
            // If no known payloads were handled, fall back to super
            if (!payloads.contains("LOAD_THUMBNAILS") && !payloads.contains("FAVORITE_CHANGED")) {
                Timber.d("onBindViewHolder: UNKNOWN payloads=$payloads, calling super")
                super.onBindViewHolder(holder, position, payloads)
            }
        }
    }
    
    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        // Explicitly clear Glide requests when view is recycled to free up
        // ConnectionThrottleManager slots immediately. This is critical for
        // network resources where concurrency is limited.
        when (holder) {
            is ListViewHolder -> holder.clearImage()
            is GridViewHolder -> holder.clearImage()
        }
    }

    inner class ListViewHolder(
        private val binding: ItemMediaFileBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        private var lastLoadedKey: String? = null

        fun clearImage() {
            Glide.with(binding.ivThumbnail.context).clear(binding.ivThumbnail)
        }

        fun loadThumbnailOnly(file: MediaFile) {
            // Partial update: only reload thumbnail (called via payload)
            // Check if we need to reload based on the key (includes refreshVersion)
            val newKey = "${file.path}_${file.size}_${credentialsId}_${disableThumbnails}_${getShowVideoThumbnails()}_${getShowPdfThumbnails()}_${refreshVersion}"
            
            Timber.d("ListViewHolder.loadThumbnailOnly: file=${file.name}, newKey=${newKey.take(80)}")
            
            // Only skip reload if the key is exactly the same (meaning thumbnail already loaded for this version)
            if (lastLoadedKey == newKey) {
                Timber.d("ListViewHolder.loadThumbnailOnly: SKIPPED - key matches for ${file.name}")
                return
            }
            
            Timber.d("ListViewHolder.loadThumbnailOnly: CALLING loadThumbnail for ${file.name}")
            loadThumbnail(file)
        }

        fun bind(file: MediaFile, selectedPaths: Set<String>) {
            // Note: Glide automatically cancels previous request when load() is called on same ImageView
            
            binding.apply {
                val isSelected = file.path in selectedPaths
                
                // Adjust thumbnail size for disableThumbnails mode
                val thumbnailSizePx = if (this@MediaFileAdapter.disableThumbnails) {
                    (32 * root.resources.displayMetrics.density).toInt() // 32dp for list when disabled
                } else {
                    (64 * root.resources.displayMetrics.density).toInt() // 64dp standard
                }
                ivThumbnail.layoutParams.width = thumbnailSizePx
                ivThumbnail.layoutParams.height = thumbnailSizePx
                
                cbSelect.setOnCheckedChangeListener(null)
                cbSelect.isChecked = isSelected
                cbSelect.setOnCheckedChangeListener { _, isChecked ->
                    onSelectionChanged(file, isChecked)
                }
                
                // Highlight selected items
                // Highlight selected items using background color
                root.setBackgroundColor(
                    if (isSelected) {
                        root.context.getColor(com.sza.fastmediasorter.R.color.item_selected)
                    } else {
                        root.context.getColor(com.sza.fastmediasorter.R.color.item_normal)
                    }
                )
                
                tvFileName.text = file.name
                tvFileInfo.text = buildFileInfo(file)
                
                // Load thumbnail using Glide (skip if waiting for payload trigger)
                if (!skipInitialThumbnailLoad) {
                    loadThumbnail(file)
                } else {
                    Timber.d("ListViewHolder.bind: SKIPPED initial thumbnail load for ${file.name} (waiting for payload)")
                }
                
                ivThumbnail.setOnClickListener {
                    onFileClick(file)
                }
                
                root.setOnClickListener {
                    onFileClick(file)
                }
                
                root.setOnLongClickListener {
                    onFileLongClick(file)
                    true
                }
                
                // Favorite button (top-right corner)
                btnFavorite.isVisible = showFavoriteButton
                btnFavorite.setImageResource(
                    if (file.isFavorite) R.drawable.ic_star_filled
                    else R.drawable.ic_star_outline
                )
                btnFavorite.setOnClickListener {
                    onFavoriteClick(file)
                }

                // Setup operation buttons with visibility checks
                // HIDE buttons if: (It's Grid Mode AND HideGridActions is ON)
                val shouldHideActions = isGridMode && hideGridActionButtons
                
                btnCopyItem.isVisible = hasDestinations && !shouldHideActions
                btnCopyItem.setOnClickListener {
                    onCopyClick(file)
                }
                
                btnMoveItem.isVisible = hasDestinations && isWritable && !shouldHideActions
                btnMoveItem.setOnClickListener {
                    onMoveClick(file)
                }
                
                btnRenameItem.isVisible = isWritable && !shouldHideActions
                btnRenameItem.setOnClickListener {
                    onRenameClick(file)
                }
                
                btnDeleteItem.isVisible = isWritable && !shouldHideActions
                btnDeleteItem.setOnClickListener {
                    onDeleteClick(file)
                }
            }
        }
        
        private fun loadThumbnail(file: MediaFile) {
            Timber.d("loadThumbnail: START file=${file.name}")
            val newKey = "${file.path}_${file.size}_${credentialsId}_${disableThumbnails}_${getShowVideoThumbnails()}_${getShowPdfThumbnails()}_${refreshVersion}"
            if (lastLoadedKey == newKey) {
                return
            }
            lastLoadedKey = newKey

            val imageView = binding.ivThumbnail
            val context = imageView.context
            
            // If thumbnails disabled, show only extension-based icons (no Glide loading)
            if (this@MediaFileAdapter.disableThumbnails) {
                when (file.type) {
                    MediaType.IMAGE -> {
                        imageView.setImageResource(R.drawable.ic_image_placeholder)
                        applyPlaceholderStyle(imageView, file.type, true)
                    }
                    MediaType.VIDEO -> {
                        imageView.setImageResource(R.drawable.ic_video_placeholder)
                        applyPlaceholderStyle(imageView, file.type, true)
                    }
                    MediaType.AUDIO -> {
                        val extension = file.name.substringAfterLast('.', "").uppercase()
                        imageView.setImageBitmap(createExtensionBitmap(extension))
                        applyPlaceholderStyle(imageView, file.type, true)
                    }
                    MediaType.GIF -> {
                        imageView.setImageResource(R.drawable.ic_image_placeholder)
                        applyPlaceholderStyle(imageView, file.type, true)
                    }
                    MediaType.TEXT -> {
                        val extension = file.name.substringAfterLast('.', "").uppercase()
                        imageView.setImageBitmap(createExtensionBitmap(extension))
                        applyPlaceholderStyle(imageView, file.type, true)
                    }
                    MediaType.PDF -> {
                        imageView.setImageResource(R.drawable.ic_image_placeholder)
                        applyPlaceholderStyle(imageView, file.type, true)
                    }
                    MediaType.EPUB -> {
                        val extension = file.name.substringAfterLast('.', "").uppercase()
                        imageView.setImageBitmap(createExtensionBitmap(extension))
                        applyPlaceholderStyle(imageView, file.type, true)
                    }
                }
                return
            }
            
            // Check if this is a cloud path (cloud://)
            val isCloudPath = file.path.startsWith("cloud://")
            // Check if this is a network path (SMB/SFTP/FTP)
            val isNetworkPath = file.path.startsWith("smb://") || file.path.startsWith("sftp://") || file.path.startsWith("ftp://")
            
            // Check if file exists before loading thumbnail
            if (!isNetworkPath && !isCloudPath) {
                val fileExists = if (file.path.startsWith("content://")) {
                    // For SAF URIs, check using DocumentFile
                    try {
                        val uri = Uri.parse(file.path)
                        val docFile = androidx.documentfile.provider.DocumentFile.fromSingleUri(context, uri)
                        docFile?.exists() == true
                    } catch (e: Exception) {
                        Timber.w(e, "Failed to check SAF URI existence: ${file.path}")
                        false
                    }
                } else {
                    // For regular file paths
                    File(file.path).exists()
                }
                
                if (!fileExists) {
                    Timber.w("File no longer exists: ${file.path}")
                    // Show error placeholder for deleted files
                    when (file.type) {
                        MediaType.IMAGE, MediaType.GIF -> {
                            imageView.setImageResource(R.drawable.ic_image_error)
                            applyPlaceholderStyle(imageView, file.type, true)
                        }
                        MediaType.VIDEO -> {
                            imageView.setImageResource(R.drawable.ic_video_error)
                            applyPlaceholderStyle(imageView, file.type, true)
                        }
                        MediaType.AUDIO -> {
                            val extension = file.name.substringAfterLast('.', "").uppercase()
                            imageView.setImageBitmap(createExtensionBitmap(extension))
                            applyPlaceholderStyle(imageView, file.type, true)
                        }
                        MediaType.TEXT -> {
                            val extension = file.name.substringAfterLast('.', "").uppercase()
                            imageView.setImageBitmap(createExtensionBitmap(extension))
                            applyPlaceholderStyle(imageView, file.type, true)
                        }
                        MediaType.PDF -> {
                            imageView.setImageResource(R.drawable.ic_image_error)
                            applyPlaceholderStyle(imageView, file.type, true)
                        }
                        MediaType.EPUB -> {
                            val extension = file.name.substringAfterLast('.', "").uppercase()
                            imageView.setImageBitmap(createExtensionBitmap(extension))
                            applyPlaceholderStyle(imageView, file.type, true)
                        }
                    }
                    return
                }
            }
            
            // Don't apply placeholder style initially - only for fallbacks
            // This keeps normal 64dp size for successful thumbnail loads
            
            when (file.type) {
                MediaType.TEXT -> {
                    val extension = file.name.substringAfterLast('.', "").uppercase()
                    imageView.setImageBitmap(createExtensionBitmap(extension))
                    applyPlaceholderStyle(imageView, file.type, true)
                }
                MediaType.EPUB -> {
                    // Load EPUB cover using Glide (EpubCoverDecoder registered in GlideAppModule)
                    if (!isCloudPath && !isNetworkPath) {
                        // Local EPUB - Glide will use EpubCoverDecoder automatically for .epub files
                        val epubFile = File(file.path)
                        if (epubFile.exists()) {
                            Glide.with(context)
                                .asBitmap()
                                .load(epubFile)
                                .placeholder(R.drawable.ic_image_placeholder)
                                .error(createExtensionBitmap("EPUB")) // Fallback to extension bitmap if no cover
                                .diskCacheStrategy(DiskCacheStrategy.RESOURCE) // Cache extracted cover
                                .into(imageView)
                        } else {
                            imageView.setImageResource(R.drawable.ic_image_placeholder)
                        }
                    } else if (isNetworkPath) {
                        // Network EPUB (SMB/SFTP/FTP) - check size limits (same as PDF logic)
                        val isSmbPath = file.path.startsWith("smb://")
                        val maxSize = if (isSmbPath) SMB_EPUB_MAX_SIZE else NETWORK_EPUB_MAX_SIZE
                        
                        if (file.size > maxSize) {
                            // File too large - show placeholder without downloading
                            imageView.setImageResource(R.drawable.ic_image_placeholder)
                            applyPlaceholderStyle(imageView, file.type, true)
                        } else {
                            // Check if thumbnail loading previously failed for this file
                            if (NetworkFileDataFetcher.isThumbnailFailed(file.path)) {
                                Timber.d("Skipping EPUB cover load for ${file.name} (cached as failed)")
                                imageView.setImageBitmap(createExtensionBitmap("EPUB"))
                                applyPlaceholderStyle(imageView, file.type, true)
                            } else {
                                // File size OK - use NetworkEpubCoverLoader
                                val networkData = NetworkFileData(
                                    path = file.path,
                                    size = file.size,
                                    credentialsId = credentialsId
                                )
                                val cacheKey = ObjectKey("${file.path}_${file.size}")
                                Glide.with(context)
                                    .asBitmap()
                                    .load(networkData)
                                    .signature(cacheKey)
                                    .listener(object : RequestListener<Bitmap> {
                                        override fun onLoadFailed(
                                            e: GlideException?,
                                            model: Any?,
                                            target: Target<Bitmap>,
                                            isFirstResource: Boolean
                                        ): Boolean {
                                            if (e != null) {
                                                Timber.w("EPUB cover load failed: ${file.name}, ${e.message}")
                                                NetworkFileDataFetcher.markThumbnailAsFailed(file.path)
                                            }
                                            return false
                                        }

                                        override fun onResourceReady(
                                            resource: Bitmap,
                                            model: Any,
                                            target: Target<Bitmap>?,
                                            dataSource: DataSource,
                                            isFirstResource: Boolean
                                        ): Boolean {
                                            resetThumbnailStyle(imageView)
                                            return false
                                        }
                                    })
                                    .placeholder(R.drawable.ic_image_placeholder)
                                    .error(createExtensionBitmap("EPUB"))
                                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                                    .into(imageView)
                            }
                        }
                    } else {
                        // Cloud EPUB - check size limit (same as PDF)
                        if (file.size > NETWORK_EPUB_MAX_SIZE) {
                            imageView.setImageResource(R.drawable.ic_image_placeholder)
                        } else {
                            // Cloud EPUB implementation would go here
                            imageView.setImageResource(R.drawable.ic_image_placeholder)
                        }
                    }
                }
                MediaType.PDF -> {
                    // PDF thumbnails are always shown when PDF support is enabled
                    // getShowPdfThumbnails() controls size limits (Large PDF Thumbnails setting)
                    val largePdfThumbnails = getShowPdfThumbnails()
                    Timber.d("PDF_THUMB_DEBUG: Loading PDF thumbnail for ${file.name}, largePdfMode=$largePdfThumbnails, isNetwork=$isNetworkPath, isCloud=$isCloudPath, size=${file.size}")
                    
                    // Load PDF thumbnail using Glide (PdfPageDecoder registered in GlideAppModule)
                    if (!isCloudPath && !isNetworkPath) {
                        // Local PDF - Glide will use PdfPageDecoder automatically for .pdf files
                        val pdfFile = File(file.path)
                        if (pdfFile.exists()) {
                            Glide.with(context)
                                .asBitmap()
                                .load(pdfFile)
                                .placeholder(R.drawable.ic_image_placeholder)
                                .error(R.drawable.ic_image_error)
                                .diskCacheStrategy(DiskCacheStrategy.RESOURCE) // Cache rendered bitmap
                                .into(imageView)
                        } else {
                            imageView.setImageResource(R.drawable.ic_image_placeholder)
                        }
                    } else if (isNetworkPath) {
                        // Network PDF (SMB/SFTP/FTP) - check size limits based on setting
                        val isSmbPath = file.path.startsWith("smb://")
                        val maxSize = if (largePdfThumbnails) {
                            // "Large PDF Thumbnails" enabled - use large limits
                            if (isSmbPath) SMB_PDF_LARGE_MAX_SIZE else NETWORK_PDF_LARGE_MAX_SIZE
                        } else {
                            // "Large PDF Thumbnails" disabled - use normal limits
                            if (isSmbPath) SMB_PDF_NORMAL_MAX_SIZE else NETWORK_PDF_NORMAL_MAX_SIZE
                        }
                        Timber.d("PDF_THUMB_DEBUG: Network PDF ${file.name}, size=${file.size}, maxSize=$maxSize, isSMB=$isSmbPath, largePdfMode=$largePdfThumbnails")
                        
                        if (file.size > maxSize) {
                            // File too large - show placeholder icon without downloading
                            Timber.d("PDF_THUMB_DEBUG: PDF too large, showing placeholder for ${file.name}")
                            imageView.setImageResource(R.drawable.ic_image_placeholder)
                            applyPlaceholderStyle(imageView, file.type, true)
                        } else {
                            // Check if thumbnail loading previously failed for this file
                            if (NetworkFileDataFetcher.isThumbnailFailed(file.path)) {
                                Timber.d("Skipping PDF thumbnail load for ${file.name} (cached as failed)")
                                imageView.setImageResource(R.drawable.ic_image_placeholder)
                                applyPlaceholderStyle(imageView, file.type, true)
                            } else {
                                // File size OK - use NetworkPdfThumbnailLoader
                                Timber.d("PDF_THUMB_DEBUG: Loading network PDF thumbnail via Glide for ${file.name}")
                                Glide.with(context)
                                    .asBitmap()
                                    .load(NetworkFileData(
                                        path = file.path,
                                        credentialsId = credentialsId,
                                        loadFullImage = false,
                                        size = file.size,
                                        createdDate = file.createdDate
                                    ))
                                    .listener(object : RequestListener<Bitmap> {
                                        override fun onLoadFailed(
                                            e: GlideException?,
                                            model: Any?,
                                            target: Target<Bitmap>,
                                            isFirstResource: Boolean
                                        ): Boolean {
                                            if (e != null) {
                                                Timber.w("PDF thumbnail load failed: ${file.name}, ${e.message}")
                                                NetworkFileDataFetcher.markThumbnailAsFailed(file.path)
                                            }
                                            return false
                                        }

                                        override fun onResourceReady(
                                            resource: Bitmap,
                                            model: Any,
                                            target: Target<Bitmap>?,
                                            dataSource: DataSource,
                                            isFirstResource: Boolean
                                        ): Boolean {
                                            resetThumbnailStyle(imageView)
                                            return false
                                        }
                                    })
                                    .placeholder(R.drawable.ic_image_placeholder)
                                    .error(R.drawable.ic_image_error)
                                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE) // Cache rendered bitmap
                                    .into(imageView)
                            }
                        }
                    } else {
                        // Cloud PDF - check size limit based on setting
                        val maxSize = if (largePdfThumbnails) {
                            NETWORK_PDF_LARGE_MAX_SIZE
                        } else {
                            NETWORK_PDF_NORMAL_MAX_SIZE
                        }
                        
                        if (file.size > maxSize) {
                            imageView.setImageResource(R.drawable.ic_image_placeholder)
                        } else {
                            // Cloud PDF implementation would go here
                            imageView.setImageResource(R.drawable.ic_image_placeholder)
                        }
                    }
                }
                MediaType.IMAGE, MediaType.GIF -> {
                    when {
                        isCloudPath -> {
                            // Load cloud thumbnail using GoogleDriveThumbnailData for authenticated access
                            if (!file.thumbnailUrl.isNullOrEmpty()) {
                                // Extract fileId from cloud path: cloud://googledrive/{fileId}
                                val fileId = file.path.substringAfterLast("/")
                                Glide.with(context)
                                    .load(GoogleDriveThumbnailData(
                                        thumbnailUrl = file.thumbnailUrl,
                                        fileId = fileId
                                    ))
                                    .priority(Priority.HIGH)  // High priority for images
                                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)  // Cache decoded, not source stream
                                    .placeholder(R.drawable.ic_image_placeholder)
                                    .error(R.drawable.ic_image_placeholder)
                                    .into(imageView)
                            } else {
                                Timber.w("No thumbnailUrl for cloud file: ${file.name}")
                                imageView.setImageResource(R.drawable.ic_image_placeholder)
                            }
                        }
                        isNetworkPath -> {
                            // Check if thumbnail loading previously failed for this file
                            if (NetworkFileDataFetcher.isThumbnailFailed(file.path)) {
                                Timber.d("Skipping thumbnail load for ${file.name} (cached as failed)")
                                imageView.setImageResource(R.drawable.ic_image_placeholder)
                                applyPlaceholderStyle(imageView, file.type, true)
                                return
                            }
                            
                            // Load network image using NetworkFileData (implements Key interface for cache)
                            Glide.with(context)
                                .load(NetworkFileData(
                                    path = file.path,
                                    credentialsId = credentialsId,
                                    loadFullImage = false,
                                    size = file.size,
                                    createdDate = file.createdDate
                                ))
                                .listener(object : RequestListener<android.graphics.drawable.Drawable> {
                                    override fun onLoadFailed(
                                        e: GlideException?,
                                        model: Any?,
                                        target: Target<android.graphics.drawable.Drawable>,
                                        isFirstResource: Boolean
                                    ): Boolean {
                                        if (e != null) {
                                            Timber.w("Network image load failed: ${file.name}, ${e.message}")
                                            NetworkFileDataFetcher.markThumbnailAsFailed(file.path)
                                        }
                                        applyPlaceholderStyle(imageView, file.type, true)
                                        return false
                                    }

                                    override fun onResourceReady(
                                        resource: android.graphics.drawable.Drawable,
                                        model: Any,
                                        target: Target<android.graphics.drawable.Drawable>?,
                                        dataSource: DataSource,
                                        isFirstResource: Boolean
                                    ): Boolean {
                                        com.sza.fastmediasorter.utils.GlideCacheStats.recordLoad(dataSource)
                                        resetThumbnailStyle(imageView)
                                        return false
                                    }
                                })
                                .priority(Priority.HIGH)  // High priority for images
                                .diskCacheStrategy(DiskCacheStrategy.ALL) // Cache both source and decoded for persistence
                                .override(CACHED_THUMBNAIL_SIZE, CACHED_THUMBNAIL_SIZE) // Fixed size for cache stability
                                .centerCrop()
                                .transition(com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade(100))
                                .placeholder(R.drawable.ic_image_placeholder)
                                .error(R.drawable.ic_image_placeholder)
                                .into(imageView)
                        }
                        else -> {
                            // Load image/GIF thumbnail using Glide for local files
                            val data = if (file.path.startsWith("content://")) {
                                Uri.parse(file.path)
                            } else {
                                File(file.path)
                            }
                            Glide.with(context)
                                .load(data)
                                .listener(object : RequestListener<android.graphics.drawable.Drawable> {
                                    override fun onLoadFailed(
                                        e: GlideException?,
                                        model: Any?,
                                        target: Target<android.graphics.drawable.Drawable>,
                                        isFirstResource: Boolean
                                    ): Boolean {
                                        if (e != null) {
                                            Timber.w("Local image load failed: ${file.name}, ${e.message}")
                                        }
                                        applyPlaceholderStyle(imageView, file.type, true)
                                        return false
                                    }

                                    override fun onResourceReady(
                                        resource: android.graphics.drawable.Drawable,
                                        model: Any,
                                        target: Target<android.graphics.drawable.Drawable>?,
                                        dataSource: DataSource,
                                        isFirstResource: Boolean
                                    ): Boolean {
                                        com.sza.fastmediasorter.utils.GlideCacheStats.recordLoad(dataSource)
                                        resetThumbnailStyle(imageView)
                                        return false
                                    }
                                })
                                .signature(ObjectKey("${file.path}_${file.size}")) // Stable cache key (path + size)
                                .priority(Priority.HIGH)  // High priority for images
                                .diskCacheStrategy(DiskCacheStrategy.ALL) // Cache both source and decoded (critical for GIF persistence)
                                .override(CACHED_THUMBNAIL_SIZE, CACHED_THUMBNAIL_SIZE) // Fixed size for cache stability
                                .centerCrop()
                                .transition(com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade(100))
                                .placeholder(R.drawable.ic_image_placeholder)
                                .error(R.drawable.ic_image_placeholder)
                                .into(imageView)
                        }
                    }
                }
                MediaType.VIDEO -> {
                    when {
                        isCloudPath -> {
                            // Load cloud video thumbnail using GoogleDriveThumbnailData for authenticated access
                            if (!file.thumbnailUrl.isNullOrEmpty()) {
                                // Extract fileId from cloud path: cloud://googledrive/{fileId}
                                val fileId = file.path.substringAfterLast("/")
                                Glide.with(context)
                                    .load(GoogleDriveThumbnailData(
                                        thumbnailUrl = file.thumbnailUrl,
                                        fileId = fileId
                                    ))
                                    .priority(Priority.NORMAL)  // Normal priority for videos
                                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)  // Cache decoded, not source stream
                                    .placeholder(R.drawable.ic_video_placeholder)
                                    .error(R.drawable.ic_video_placeholder)
                                    .into(imageView)
                            } else {
                                Timber.w("No thumbnailUrl for cloud video: ${file.name}")
                                imageView.setImageResource(R.drawable.ic_video_placeholder)
                                applyPlaceholderStyle(imageView, file.type, true)
                            }
                        }
                        isNetworkPath -> {
                            // Check if video thumbnails are enabled
                            if (!getShowVideoThumbnails()) {
                                imageView.setImageResource(R.drawable.ic_video_placeholder)
                                return
                            }
                            
                            // Load network video thumbnail using NetworkFileData
                            // Use listener to catch decoder failures and cache them
                            Glide.with(context)
                                .load(NetworkFileData(
                                    path = file.path,
                                    credentialsId = credentialsId,
                                    loadFullImage = false,
                                    size = file.size,
                                    createdDate = file.createdDate
                                ))
                                .priority(Priority.NORMAL)  // Normal priority for videos
                                .listener(object : RequestListener<android.graphics.drawable.Drawable> {
                                    override fun onLoadFailed(
                                        e: GlideException?,
                                        model: Any?,
                                        target: Target<android.graphics.drawable.Drawable>,
                                        isFirstResource: Boolean
                                    ): Boolean {
                                        // Check if this is a video decoder failure
                                        if (isVideoDecoderException(e)) {
                                            NetworkFileDataFetcher.markVideoAsFailed(file.path)
                                            Timber.d("Thumbnail load failed: ${file.name} (decoder error, cached)")
                                        } else if (e != null) {
                                            Timber.w("Thumbnail load failed: ${file.name}, ${e.message}")
                                        }
                                        applyPlaceholderStyle(imageView, file.type, true)
                                        return false // Let Glide show error placeholder
                                    }

                                    override fun onResourceReady(
                                        resource: android.graphics.drawable.Drawable,
                                        model: Any,
                                        target: Target<android.graphics.drawable.Drawable>?,
                                        dataSource: DataSource,
                                        isFirstResource: Boolean
                                    ): Boolean {
                                        com.sza.fastmediasorter.utils.GlideCacheStats.recordLoad(dataSource)
                                        resetThumbnailStyle(imageView)
                                        return false
                                    }
                                })
                                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                                .override(CACHED_THUMBNAIL_SIZE, CACHED_THUMBNAIL_SIZE) // Fixed size for cache stability
                                .centerCrop()
                                .transition(com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade(100))
                                .placeholder(R.drawable.ic_video_placeholder)
                                .error(R.drawable.ic_video_placeholder)
                                .into(imageView)
                        }
                        else -> {
                            // Check if video thumbnails are enabled
                            if (!getShowVideoThumbnails()) {
                                imageView.setImageResource(R.drawable.ic_video_placeholder)
                                return
                            }
                            
                            // Load video first frame using Glide for local files
                            val data = if (file.path.startsWith("content://")) {
                                Uri.parse(file.path)
                            } else {
                                File(file.path)
                            }
                            Glide.with(context)
                                .load(data)
                                .signature(ObjectKey("${file.path}_${file.size}")) // Stable cache key (path + size)
                                .priority(Priority.NORMAL)  // Normal priority for videos
                                .diskCacheStrategy(DiskCacheStrategy.DATA)
                                .override(CACHED_THUMBNAIL_SIZE, CACHED_THUMBNAIL_SIZE) // Fixed size for cache stability
                                .centerCrop()
                                .transition(com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade(100))
                                .placeholder(R.drawable.ic_video_placeholder)
                                .error(R.drawable.ic_video_placeholder)
                                .into(imageView)
                        }
                    }
                }
                MediaType.AUDIO -> {
                    val extension = file.name.substringAfterLast('.', "").uppercase()
                    val bitmap = createExtensionBitmap(extension)
                    imageView.setImageBitmap(bitmap)
                    applyPlaceholderStyle(imageView, file.type, true)
                }
            }
        }
        
        private fun createExtensionBitmap(extension: String): Bitmap {
            val size = 200
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            
            // Background
            val bgPaint = Paint().apply {
                color = ContextCompat.getColor(binding.root.context, R.color.audio_icon_bg)
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), bgPaint)
            
            // Text
            val textPaint = Paint().apply {
                color = Color.WHITE
                textSize = 60f
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            val xPos = size / 2f
            val yPos = (size / 2f - (textPaint.descent() + textPaint.ascent()) / 2)
            canvas.drawText(extension, xPos, yPos, textPaint)
            
            return bitmap
        }
        
        private fun buildFileInfo(file: MediaFile): String {
            // Hide invalid FTP metadata (size=0 or date=1970-01-01)
            val size = if (file.size > 0) formatFileSize(file.size) else "—"
            val date = if (file.createdDate > 0) {
                DateFormat.format("yyyy-MM-dd", Date(file.createdDate))
            } else {
                "—"
            }
            return "$size • $date"
        }
        
        private fun formatFileSize(size: Long): String {
            return com.sza.fastmediasorter.core.util.formatFileSize(size)
        }
    }
    
    // Grid ViewHolder for grid mode
    inner class GridViewHolder(
        private val binding: ItemMediaFileGridBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        private var lastLoadedKey: String? = null
        
        fun clearImage() {
            Glide.with(binding.ivThumbnail.context).clear(binding.ivThumbnail)
        }

        fun loadThumbnailOnly(file: MediaFile) {
            // Partial update: only reload thumbnail (called via payload)
            // Check if we need to reload based on the key (includes refreshVersion)
            val newKey = "${file.path}_${file.size}_${credentialsId}_${disableThumbnails}_${getShowVideoThumbnails()}_${getShowPdfThumbnails()}_${refreshVersion}"
            
            // Only skip reload if the key is exactly the same (meaning thumbnail already loaded for this version)
            if (lastLoadedKey == newKey) {
                return
            }
            
            loadThumbnail(file)
        }
        
        fun bind(file: MediaFile, selectedPaths: Set<String>) {
            // Note: Glide automatically cancels previous request when load() is called on same ImageView
            
            binding.apply {
                val isSelected = file.path in selectedPaths
                
                // Setup checkbox
                cbSelect.setOnCheckedChangeListener(null)
                cbSelect.isChecked = isSelected
                cbSelect.setOnCheckedChangeListener { _, isChecked ->
                    onSelectionChanged(file, isChecked)
                }
                
                // Long click on checkbox: select range from last selected to this file
                cbSelect.setOnLongClickListener {
                    if (!isSelected) {
                        // Only handle long click on unchecked checkbox
                        onSelectionRangeRequested(file)
                    }
                    true // Consume the event
                }
                
                // Set dynamic thumbnail size
                val sizeInPx = if (this@MediaFileAdapter.disableThumbnails) {
                    (64 * root.context.resources.displayMetrics.density).toInt() // 64dp when disabled
                } else {
                    (thumbnailSize * root.context.resources.displayMetrics.density).toInt() // User preference
                }
                ivThumbnail.layoutParams.width = sizeInPx
                ivThumbnail.layoutParams.height = sizeInPx
                tvFileName.layoutParams.width = sizeInPx
                
                // Highlight selected items
                cvCard.setCardBackgroundColor(
                    if (isSelected) {
                        root.context.getColor(R.color.item_selected)
                    } else {
                        root.context.getColor(R.color.item_normal)
                    }
                )
                
                tvFileName.text = file.name
                
                // Load thumbnail using Glide (skip if waiting for payload trigger)
                if (!skipInitialThumbnailLoad) {
                    loadThumbnail(file)
                } else {
                    Timber.d("GridViewHolder.bind: SKIPPED initial thumbnail load for ${file.name} (waiting for payload)")
                }
                
                ivThumbnail.setOnClickListener {
                    onFileClick(file)
                }
                
                root.setOnClickListener {
                    onFileClick(file)
                }
                
                root.setOnLongClickListener {
                    onFileLongClick(file)
                    true
                }
                
                // Favorite button
                btnFavorite.isVisible = showFavoriteButton
                btnFavorite.setImageResource(
                    if (file.isFavorite) R.drawable.ic_star_filled 
                    else R.drawable.ic_star_outline
                )
                btnFavorite.setOnClickListener {
                    onFavoriteClick(file)
                }

                // Setup operation buttons with visibility
                btnCopyItem.isVisible = hasDestinations && !hideGridActionButtons
                btnCopyItem.setOnClickListener {
                    onCopyClick(file)
                }
                
                btnMoveItem.isVisible = hasDestinations && isWritable && !hideGridActionButtons
                btnMoveItem.setOnClickListener {
                    onMoveClick(file)
                }
                
                btnRenameItem.isVisible = isWritable && !hideGridActionButtons
                btnRenameItem.setOnClickListener {
                    onRenameClick(file)
                }
                
                btnDeleteItem.isVisible = isWritable && !hideGridActionButtons
                btnDeleteItem.setOnClickListener {
                    onDeleteClick(file)
                }
            }
        }
        
        private fun loadThumbnail(file: MediaFile) {
            Timber.d("loadThumbnail: START file=${file.name}")
            val newKey = "${file.path}_${file.size}_${credentialsId}_${disableThumbnails}_${getShowVideoThumbnails()}_${getShowPdfThumbnails()}_${refreshVersion}"
            if (lastLoadedKey == newKey) {
                return
            }
            lastLoadedKey = newKey

            val imageView = binding.ivThumbnail
            val context = imageView.context
            
            // If thumbnails disabled, show only extension-based icons (no Glide loading)
            if (this@MediaFileAdapter.disableThumbnails) {
                when (file.type) {
                    MediaType.IMAGE -> imageView.setImageResource(R.drawable.ic_image_placeholder)
                    MediaType.VIDEO -> imageView.setImageResource(R.drawable.ic_video_placeholder)
                    MediaType.AUDIO -> {
                        val extension = file.name.substringAfterLast('.', "").uppercase()
                        imageView.setImageBitmap(createExtensionBitmap(extension))
                    }
                    MediaType.GIF -> imageView.setImageResource(R.drawable.ic_image_placeholder)
                    MediaType.TEXT -> {
                        val extension = file.name.substringAfterLast('.', "").uppercase()
                        imageView.setImageBitmap(createExtensionBitmap(extension))
                    }
                    MediaType.PDF -> imageView.setImageResource(R.drawable.ic_image_placeholder)
                    MediaType.EPUB -> {
                        val extension = file.name.substringAfterLast('.', "").uppercase()
                        imageView.setImageBitmap(createExtensionBitmap(extension))
                    }
                }
                return
            }
            
            // Check if this is a cloud path (cloud://)
            val isCloudPath = file.path.startsWith("cloud://")
            // Check if this is a network path (SMB/SFTP/FTP)
            val isNetworkPath = file.path.startsWith("smb://") || file.path.startsWith("sftp://") || file.path.startsWith("ftp://")
            
            // For local files, check if file exists (skip for content:// URIs)
            if (!isNetworkPath && !isCloudPath && !file.path.startsWith("content://")) {
                val localFile = File(file.path)
                if (!localFile.exists()) {
                    Timber.w("File no longer exists: ${file.path}")
                    when (file.type) {
                        MediaType.IMAGE, MediaType.GIF -> imageView.setImageResource(R.drawable.ic_image_error)
                        MediaType.VIDEO -> imageView.setImageResource(R.drawable.ic_video_error)
                        MediaType.AUDIO -> {
                            val extension = file.name.substringAfterLast('.', "").uppercase()
                            imageView.setImageBitmap(createExtensionBitmap(extension))
                        }
                        MediaType.TEXT -> {
                            val extension = file.name.substringAfterLast('.', "").uppercase()
                            imageView.setImageBitmap(createExtensionBitmap(extension))
                        }
                        MediaType.PDF -> imageView.setImageResource(R.drawable.ic_image_error)
                        MediaType.EPUB -> {
                            val extension = file.name.substringAfterLast('.', "").uppercase()
                            imageView.setImageBitmap(createExtensionBitmap(extension))
                        }
                    }
                    return
                }
            }
            
            when (file.type) {
                MediaType.IMAGE, MediaType.GIF -> {
                    when {
                        isCloudPath -> {
                            if (!file.thumbnailUrl.isNullOrEmpty()) {
                                // Extract fileId from cloud path: cloud://googledrive/{fileId}
                                val fileId = file.path.substringAfterLast("/")
                                Glide.with(context)
                                    .load(GoogleDriveThumbnailData(
                                        thumbnailUrl = file.thumbnailUrl,
                                        fileId = fileId
                                    ))
                                    .priority(Priority.HIGH)  // High priority for images
                                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)  // Cache decoded, not source stream
                                    .placeholder(R.drawable.ic_image_placeholder)
                                    .error(R.drawable.ic_image_placeholder)
                                    .into(imageView)
                            } else {
                                imageView.setImageResource(R.drawable.ic_image_placeholder)
                            }
                        }
                        isNetworkPath -> {
                            // Check if thumbnail loading previously failed for this file
                            if (NetworkFileDataFetcher.isThumbnailFailed(file.path)) {
                                Timber.d("Skipping thumbnail load for ${file.name} (cached as failed)")
                                imageView.setImageResource(R.drawable.ic_image_placeholder)
                                return
                            }
                            
                            // Grid mode: use user-defined thumbnailSize (converts dp to px)
                            // val sizePx = (thumbnailSize * context.resources.displayMetrics.density).toInt()
                            Glide.with(context)
                                .load(NetworkFileData(
                                    path = file.path,
                                    credentialsId = credentialsId,
                                    loadFullImage = false,
                                    size = file.size,
                                    createdDate = file.createdDate
                                ))
                                .listener(object : RequestListener<android.graphics.drawable.Drawable> {
                                    override fun onLoadFailed(
                                        e: GlideException?,
                                        model: Any?,
                                        target: Target<android.graphics.drawable.Drawable>,
                                        isFirstResource: Boolean
                                    ): Boolean {
                                        if (e != null) {
                                            Timber.w("Network image load failed (grid): ${file.name}, ${e.message}")
                                            NetworkFileDataFetcher.markThumbnailAsFailed(file.path)
                                        }
                                        return false
                                    }

                                    override fun onResourceReady(
                                        resource: android.graphics.drawable.Drawable,
                                        model: Any,
                                        target: Target<android.graphics.drawable.Drawable>?,
                                        dataSource: DataSource,
                                        isFirstResource: Boolean
                                    ): Boolean {
                                        com.sza.fastmediasorter.utils.GlideCacheStats.recordLoad(dataSource)
                                        resetThumbnailStyle(imageView)
                                        return false
                                    }
                                })
                                .priority(Priority.HIGH)  // High priority for images
                                .diskCacheStrategy(DiskCacheStrategy.RESOURCE) // Cache decoded only - PipedInputStream can't be re-read
                                .override(CACHED_THUMBNAIL_SIZE, CACHED_THUMBNAIL_SIZE) // Fixed size for cache stability
                                .centerCrop()
                                .transition(com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade(100))
                                .placeholder(R.drawable.ic_image_placeholder)
                                .error(R.drawable.ic_image_placeholder)
                                .into(imageView)
                        }
                        else -> {
                            val data = if (file.path.startsWith("content://")) {
                                Uri.parse(file.path)
                            } else {
                                File(file.path)
                            }
                            // val sizePx = (thumbnailSize * context.resources.displayMetrics.density).toInt()
                            Glide.with(context)
                                .load(data)
                                .signature(ObjectKey("${file.path}_${file.size}")) // Stable cache key (path + size)
                                .listener(object : RequestListener<android.graphics.drawable.Drawable> {
                                    override fun onLoadFailed(
                                        e: GlideException?,
                                        model: Any?,
                                        target: Target<android.graphics.drawable.Drawable>,
                                        isFirstResource: Boolean
                                    ): Boolean {
                                        if (e != null) {
                                            Timber.w("Local image load failed (grid): ${file.name}, ${e.message}")
                                        }
                                        return false
                                    }

                                    override fun onResourceReady(
                                        resource: android.graphics.drawable.Drawable,
                                        model: Any,
                                        target: Target<android.graphics.drawable.Drawable>?,
                                        dataSource: DataSource,
                                        isFirstResource: Boolean
                                    ): Boolean {
                                        com.sza.fastmediasorter.utils.GlideCacheStats.recordLoad(dataSource)
                                        resetThumbnailStyle(imageView)
                                        return false
                                    }
                                })
                                .priority(Priority.HIGH)  // High priority for images
                                .diskCacheStrategy(DiskCacheStrategy.DATA)
                                .override(CACHED_THUMBNAIL_SIZE, CACHED_THUMBNAIL_SIZE) // Fixed size for cache stability
                                .centerCrop()
                                .transition(com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade(100))
                                .placeholder(R.drawable.ic_image_placeholder)
                                .error(R.drawable.ic_image_placeholder)
                                .into(imageView)
                        }
                    }
                }
                MediaType.VIDEO -> {
                    when {
                        isCloudPath -> {
                            if (!file.thumbnailUrl.isNullOrEmpty()) {
                                // Extract fileId from cloud path: cloud://googledrive/{fileId}
                                val fileId = file.path.substringAfterLast("/")
                                Glide.with(context)
                                    .load(GoogleDriveThumbnailData(
                                        thumbnailUrl = file.thumbnailUrl,
                                        fileId = fileId
                                    ))
                                    .priority(Priority.NORMAL)  // Normal priority for videos
                                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)  // Cache decoded, not source stream
                                    .placeholder(R.drawable.ic_video_placeholder)
                                    .error(R.drawable.ic_video_placeholder)
                                    .into(imageView)
                            } else {
                                Timber.w("No thumbnailUrl for cloud video: ${file.name}")
                                imageView.setImageResource(R.drawable.ic_video_placeholder)
                            }
                        }
                        isNetworkPath -> {
                            // Load network video thumbnail with error listener
                            // val sizePx = (thumbnailSize * context.resources.displayMetrics.density).toInt()
                            Glide.with(context)
                                .load(NetworkFileData(
                                    path = file.path,
                                    credentialsId = credentialsId,
                                    loadFullImage = false,
                                    size = file.size,
                                    createdDate = file.createdDate
                                ))
                                .priority(Priority.NORMAL)  // Normal priority for videos
                                .listener(object : RequestListener<android.graphics.drawable.Drawable> {
                                    override fun onLoadFailed(
                                        e: GlideException?,
                                        model: Any?,
                                        target: Target<android.graphics.drawable.Drawable>,
                                        isFirstResource: Boolean
                                    ): Boolean {
                                        // Check if this is a video decoder failure
                                        if (isVideoDecoderException(e)) {
                                            NetworkFileDataFetcher.markVideoAsFailed(file.path)
                                            Timber.d("Thumbnail load failed: ${file.name} (decoder error, cached)")
                                        } else if (e != null) {
                                            Timber.w("Thumbnail load failed: ${file.name}, ${e.message}")
                                        }
                                        return false // Let Glide show error placeholder
                                    }

                                    override fun onResourceReady(
                                        resource: android.graphics.drawable.Drawable,
                                        model: Any,
                                        target: Target<android.graphics.drawable.Drawable>?,
                                        dataSource: DataSource,
                                        isFirstResource: Boolean
                                    ): Boolean {
                                        com.sza.fastmediasorter.utils.GlideCacheStats.recordLoad(dataSource)
                                        resetThumbnailStyle(imageView)
                                        return false
                                    }
                                })
                                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                                .override(CACHED_THUMBNAIL_SIZE, CACHED_THUMBNAIL_SIZE) // Fixed size for cache stability
                                .centerCrop()
                                .transition(com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade(100))
                                .placeholder(R.drawable.ic_video_placeholder)
                                .error(R.drawable.ic_video_placeholder)
                                .into(imageView)
                        }
                        else -> {
                            val data = if (file.path.startsWith("content://")) {
                                Uri.parse(file.path)
                            } else {
                                File(file.path)
                            }
                            // val sizePx = (thumbnailSize * context.resources.displayMetrics.density).toInt()
                            Glide.with(context)
                                .load(data)
                                .signature(ObjectKey("${file.path}_${file.size}")) // Stable cache key (path + size)
                                .priority(Priority.NORMAL)  // Normal priority for videos
                                .diskCacheStrategy(DiskCacheStrategy.DATA)
                                .override(CACHED_THUMBNAIL_SIZE, CACHED_THUMBNAIL_SIZE) // Fixed size for cache stability
                                .centerCrop()
                                .transition(com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade(100))
                                .placeholder(R.drawable.ic_video_placeholder)
                                .error(R.drawable.ic_video_placeholder)
                                .into(imageView)
                        }
                    }
                }
                MediaType.AUDIO -> {
                    val extension = file.name.substringAfterLast('.', "").uppercase()
                    val bitmap = createExtensionBitmap(extension)
                    imageView.setImageBitmap(bitmap)
                }
                MediaType.TEXT -> {
                    val extension = file.name.substringAfterLast('.', "").uppercase()
                    imageView.setImageBitmap(createExtensionBitmap(extension))
                }
                MediaType.EPUB -> {
                    // Load EPUB cover using Glide (EpubCoverDecoder registered in GlideAppModule)
                    if (!isCloudPath && !isNetworkPath) {
                        // Local EPUB - Glide will use EpubCoverDecoder automatically for .epub files
                        val epubFile = File(file.path)
                        if (epubFile.exists()) {
                            Glide.with(context)
                                .asBitmap()
                                .load(epubFile)
                                .placeholder(R.drawable.ic_image_placeholder)
                                .error(createExtensionBitmap("EPUB")) // Fallback to extension bitmap if no cover
                                .diskCacheStrategy(DiskCacheStrategy.RESOURCE) // Cache extracted cover
                                .into(imageView)
                        } else {
                            imageView.setImageResource(R.drawable.ic_image_placeholder)
                        }
                    } else if (isNetworkPath) {
                        // Network EPUB (SMB/SFTP/FTP) - check size limits (same as PDF logic)
                        val isSmbPath = file.path.startsWith("smb://")
                        val maxSize = if (isSmbPath) SMB_EPUB_MAX_SIZE else NETWORK_EPUB_MAX_SIZE
                        
                        if (file.size > maxSize) {
                            // File too large - show placeholder without downloading
                            imageView.setImageResource(R.drawable.ic_image_placeholder)
                        } else {
                            // File size OK - use NetworkEpubCoverLoader
                            val networkData = NetworkFileData(
                                path = file.path,
                                size = file.size,
                                credentialsId = credentialsId
                            )
                            val cacheKey = ObjectKey("${file.path}_${file.size}")
                            Glide.with(context)
                                .asBitmap()
                                .load(networkData)
                                .signature(cacheKey)
                                .placeholder(R.drawable.ic_image_placeholder)
                                .error(createExtensionBitmap("EPUB"))
                                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                                .into(imageView)
                        }
                    } else {
                        // Cloud EPUB - check size limit (same as PDF)
                        if (file.size > NETWORK_EPUB_MAX_SIZE) {
                            imageView.setImageResource(R.drawable.ic_image_placeholder)
                        } else {
                            // Cloud EPUB implementation would go here
                            imageView.setImageResource(R.drawable.ic_image_placeholder)
                        }
                    }
                }
                MediaType.PDF -> {
                    // PDF thumbnails are always shown when PDF support is enabled
                    // getShowPdfThumbnails() controls size limits (Large PDF Thumbnails setting)
                    val largePdfThumbnails = getShowPdfThumbnails()
                    
                    // Load PDF thumbnail using Glide (PdfPageDecoder registered in GlideAppModule)
                    if (!isCloudPath && !isNetworkPath) {
                        // Local PDF - Glide will use PdfPageDecoder automatically for .pdf files
                        val pdfFile = File(file.path)
                        if (pdfFile.exists()) {
                            Glide.with(context)
                                .asBitmap()
                                .load(pdfFile)
                                .placeholder(R.drawable.ic_image_placeholder)
                                .error(R.drawable.ic_image_error)
                                .diskCacheStrategy(DiskCacheStrategy.RESOURCE) // Cache rendered bitmap
                                .into(imageView)
                        } else {
                            imageView.setImageBitmap(createExtensionBitmap("PDF"))
                        }
                    } else if (isNetworkPath) {
                        // Network PDF (SMB/SFTP/FTP) - check size limits based on setting
                        val isSmbPath = file.path.startsWith("smb://")
                        val maxSize = if (largePdfThumbnails) {
                            // "Large PDF Thumbnails" enabled - use large limits
                            if (isSmbPath) SMB_PDF_LARGE_MAX_SIZE else NETWORK_PDF_LARGE_MAX_SIZE
                        } else {
                            // "Large PDF Thumbnails" disabled - use normal limits
                            if (isSmbPath) SMB_PDF_NORMAL_MAX_SIZE else NETWORK_PDF_NORMAL_MAX_SIZE
                        }
                        
                        if (file.size > maxSize) {
                            // File too large - show PDF icon without downloading
                            imageView.setImageBitmap(createExtensionBitmap("PDF"))
                        } else {
                            // File size OK - use NetworkPdfThumbnailLoader
                            Glide.with(context)
                                .asBitmap()
                                .load(NetworkFileData(
                                    path = file.path,
                                    credentialsId = credentialsId,
                                    loadFullImage = false,
                                    size = file.size,
                                    createdDate = file.createdDate
                                ))
                                .placeholder(R.drawable.ic_image_placeholder)
                                .error(R.drawable.ic_image_error)
                                .diskCacheStrategy(DiskCacheStrategy.RESOURCE) // Cache rendered bitmap
                                .into(imageView)
                        }
                    } else {
                        // Cloud PDF - check size limit based on setting
                        val maxSize = if (largePdfThumbnails) {
                            NETWORK_PDF_LARGE_MAX_SIZE
                        } else {
                            NETWORK_PDF_NORMAL_MAX_SIZE
                        }
                        
                        if (file.size > maxSize) {
                            imageView.setImageBitmap(createExtensionBitmap("PDF"))
                        } else {
                            // Cloud PDF implementation would go here
                            imageView.setImageBitmap(createExtensionBitmap("PDF"))
                        }
                    }
                }
            }
        }
        
        private fun createExtensionBitmap(extension: String): Bitmap {
            val size = 200
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            
            val bgPaint = Paint().apply {
                color = ContextCompat.getColor(binding.root.context, R.color.audio_icon_bg)
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), bgPaint)
            
            val textPaint = Paint().apply {
                color = Color.WHITE
                textSize = 60f
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            val xPos = size / 2f
            val yPos = (size / 2f - (textPaint.descent() + textPaint.ascent()) / 2)
            canvas.drawText(extension, xPos, yPos, textPaint)
            
            return bitmap
        }
    }

    private class MediaFileDiffCallback : DiffUtil.ItemCallback<MediaFile>() {
        override fun areItemsTheSame(oldItem: MediaFile, newItem: MediaFile): Boolean {
            return oldItem.path == newItem.path
        }

        override fun areContentsTheSame(oldItem: MediaFile, newItem: MediaFile): Boolean {
            return oldItem == newItem
        }
        
        override fun getChangePayload(oldItem: MediaFile, newItem: MediaFile): Any? {
            // If only isFavorite changed, return FAVORITE_CHANGED payload for partial update
            if (oldItem.isFavorite != newItem.isFavorite) {
                // isFavorite changed - check if everything else is the same
                if (oldItem.copy(isFavorite = newItem.isFavorite) == newItem) {
                    return "FAVORITE_CHANGED"
                }
            }
            return null // Full rebind needed for other changes
        }
    }
}





