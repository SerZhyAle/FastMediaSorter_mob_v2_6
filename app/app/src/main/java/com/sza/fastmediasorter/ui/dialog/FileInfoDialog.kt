package com.sza.fastmediasorter.ui.dialog

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.text.format.DateFormat
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import com.sza.fastmediasorter.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.text.DecimalFormat
import java.util.Date

/**
 * Dialog to display comprehensive file information including media-specific metadata.
 * Supports images (with EXIF), videos, audio files, and documents.
 */
class FileInfoDialog(
    context: Context,
    private val filePath: String
) : Dialog(context) {

    private val file = File(filePath)
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var metadataContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val contentView = createContentView()
        setContentView(contentView)
        
        setTitle(R.string.file_info)
        
        window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.9).toInt(),
            android.view.WindowManager.LayoutParams.WRAP_CONTENT
        )
        
        // Load media-specific metadata asynchronously
        scope.launch {
            loadMediaMetadata()
        }
    }

    override fun onStop() {
        super.onStop()
        scope.cancel()
    }

    private fun createContentView(): View {
        val padding = context.resources.getDimensionPixelSize(R.dimen.spacing_medium)
        
        val scrollView = ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
        }

        // === Basic File Information Section ===
        container.addView(createSectionHeader(context.getString(R.string.info_section_basic)))
        
        container.addView(createInfoRow(
            context.getString(R.string.info_name), 
            file.name
        ))

        container.addView(createInfoRow(
            context.getString(R.string.info_path), 
            file.parent ?: ""
        ))

        container.addView(createInfoRow(
            context.getString(R.string.info_size), 
            formatFileSize(file.length())
        ))

        container.addView(createInfoRow(
            context.getString(R.string.info_type), 
            getFileType()
        ))

        container.addView(createInfoRow(
            context.getString(R.string.info_modified), 
            formatDate(file.lastModified())
        ))

        // === Media Metadata Section (populated asynchronously) ===
        metadataContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        container.addView(metadataContainer)

        // === Buttons ===
        val buttonLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = padding
            }
        }

        val openButton = com.google.android.material.button.MaterialButton(context).apply {
            text = context.getString(R.string.open_with)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { openWithExternalApp() }
        }
        buttonLayout.addView(openButton)

        val closeButton = com.google.android.material.button.MaterialButton(
            context, 
            null, 
            com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {
            text = context.getString(R.string.action_close)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = padding / 2
            }
            setOnClickListener { dismiss() }
        }
        buttonLayout.addView(closeButton)

        container.addView(buttonLayout)
        scrollView.addView(container)

        return scrollView
    }

    private fun createSectionHeader(title: String): View {
        val padding = context.resources.getDimensionPixelSize(R.dimen.spacing_small)
        return TextView(context).apply {
            text = title
            setTypeface(null, Typeface.BOLD)
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleSmall)
            setTextColor(context.getColor(R.color.colorPrimary))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = padding
                bottomMargin = padding / 2
            }
        }
    }

    private fun createInfoRow(label: String, value: String): View {
        val rowLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = context.resources.getDimensionPixelSize(R.dimen.spacing_small)
            }
        }

        val labelView = TextView(context).apply {
            text = label
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_LabelSmall)
            setTextColor(context.getColor(android.R.color.darker_gray))
        }
        rowLayout.addView(labelView)

        val valueView = TextView(context).apply {
            text = value
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
            setTextIsSelectable(true)
        }
        rowLayout.addView(valueView)

        return rowLayout
    }

    private suspend fun loadMediaMetadata() = withContext(Dispatchers.IO) {
        try {
            val extension = file.extension.lowercase()
            val mediaType = getMediaCategory(extension)
            
            when (mediaType) {
                MediaCategory.IMAGE -> loadImageMetadata()
                MediaCategory.VIDEO -> loadVideoMetadata()
                MediaCategory.AUDIO -> loadAudioMetadata()
                MediaCategory.DOCUMENT -> loadDocumentMetadata()
                MediaCategory.OTHER -> { /* No additional metadata */ }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error loading media metadata")
        }
    }

    private suspend fun loadImageMetadata() {
        try {
            val extension = file.extension.lowercase()
            val isGif = extension == "gif"
            
            // Get image dimensions
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, options)
            val width = options.outWidth
            val height = options.outHeight
            
            withContext(Dispatchers.Main) {
                metadataContainer.addView(createSectionHeader(
                    if (isGif) context.getString(R.string.info_section_gif) 
                    else context.getString(R.string.info_section_image)
                ))
                
                if (width > 0 && height > 0) {
                    metadataContainer.addView(createInfoRow(
                        context.getString(R.string.info_resolution),
                        "${width} × ${height} px"
                    ))
                    
                    // Megapixels
                    val megapixels = (width.toLong() * height) / 1_000_000.0
                    if (megapixels >= 0.1) {
                        metadataContainer.addView(createInfoRow(
                            context.getString(R.string.info_megapixels),
                            String.format("%.1f MP", megapixels)
                        ))
                    }
                }
            }
            
            // Load EXIF data for supported formats
            if (extension in listOf("jpg", "jpeg", "webp", "png", "heic", "heif")) {
                loadExifData()
            }
            
        } catch (e: Exception) {
            Timber.e(e, "Error loading image metadata")
        }
    }

    private suspend fun loadExifData() {
        try {
            val exif = ExifInterface(file.absolutePath)
            
            withContext(Dispatchers.Main) {
                // Camera Model
                val make = exif.getAttribute(ExifInterface.TAG_MAKE)
                val model = exif.getAttribute(ExifInterface.TAG_MODEL)
                if (make != null || model != null) {
                    val camera = listOfNotNull(make, model).joinToString(" ")
                    metadataContainer.addView(createInfoRow(
                        context.getString(R.string.info_camera),
                        camera
                    ))
                }
                
                // Date Taken
                val dateTime = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                    ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
                if (dateTime != null) {
                    metadataContainer.addView(createInfoRow(
                        context.getString(R.string.info_date_taken),
                        dateTime
                    ))
                }
                
                // Aperture
                val aperture = exif.getAttribute(ExifInterface.TAG_F_NUMBER)
                if (aperture != null) {
                    metadataContainer.addView(createInfoRow(
                        context.getString(R.string.info_aperture),
                        "f/$aperture"
                    ))
                }
                
                // Exposure Time
                val exposure = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)
                if (exposure != null) {
                    val exposureFormatted = formatExposureTime(exposure)
                    metadataContainer.addView(createInfoRow(
                        context.getString(R.string.info_exposure),
                        exposureFormatted
                    ))
                }
                
                // ISO
                val iso = exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY)
                    ?: exif.getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS)
                if (iso != null) {
                    metadataContainer.addView(createInfoRow(
                        context.getString(R.string.info_iso),
                        "ISO $iso"
                    ))
                }
                
                // Focal Length
                val focalLength = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)
                if (focalLength != null) {
                    val fl = parseFocalLength(focalLength)
                    if (fl != null) {
                        metadataContainer.addView(createInfoRow(
                            context.getString(R.string.info_focal_length),
                            "${fl}mm"
                        ))
                    }
                }
                
                // GPS Location
                val latLong = FloatArray(2)
                if (exif.getLatLong(latLong)) {
                    val lat = latLong[0]
                    val lon = latLong[1]
                    metadataContainer.addView(createClickableInfoRow(
                        context.getString(R.string.info_location),
                        String.format("%.6f, %.6f", lat, lon)
                    ) {
                        openGoogleMaps(lat, lon)
                    })
                }
                
                // Flash
                val flash = exif.getAttributeInt(ExifInterface.TAG_FLASH, -1)
                if (flash >= 0) {
                    val flashFired = (flash and 1) == 1
                    metadataContainer.addView(createInfoRow(
                        context.getString(R.string.info_flash),
                        if (flashFired) context.getString(R.string.info_flash_fired) 
                        else context.getString(R.string.info_flash_not_fired)
                    ))
                }
                
                // White Balance
                val whiteBalance = exif.getAttributeInt(ExifInterface.TAG_WHITE_BALANCE, -1)
                if (whiteBalance >= 0) {
                    metadataContainer.addView(createInfoRow(
                        context.getString(R.string.info_white_balance),
                        if (whiteBalance == ExifInterface.WHITE_BALANCE_AUTO.toInt()) "Auto" else "Manual"
                    ))
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error loading EXIF data")
        }
    }

    private suspend fun loadVideoMetadata() {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(file.absolutePath)
            
            withContext(Dispatchers.Main) {
                metadataContainer.addView(createSectionHeader(context.getString(R.string.info_section_video)))
            }
            
            // Duration
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            
            // Resolution
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()
            
            // Bitrate
            val bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toLongOrNull()
            
            // Rotation
            val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull()
            
            // Frame rate (Android 23+)
            val frameRate = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)?.toFloatOrNull()
            } else null
            
            // Video codec (Android 24+)
            val mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
            
            // Date
            val date = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE)
            
            withContext(Dispatchers.Main) {
                if (durationMs != null) {
                    metadataContainer.addView(createInfoRow(
                        context.getString(R.string.info_duration),
                        formatDuration(durationMs)
                    ))
                }
                
                if (width != null && height != null) {
                    metadataContainer.addView(createInfoRow(
                        context.getString(R.string.info_resolution),
                        "${width} × ${height} px"
                    ))
                    
                    // Quality label
                    val qualityLabel = getVideoQualityLabel(width, height)
                    if (qualityLabel != null) {
                        metadataContainer.addView(createInfoRow(
                            context.getString(R.string.info_quality),
                            qualityLabel
                        ))
                    }
                }
                
                if (bitrate != null) {
                    metadataContainer.addView(createInfoRow(
                        context.getString(R.string.info_bitrate),
                        formatBitrate(bitrate)
                    ))
                }
                
                if (frameRate != null && frameRate > 0) {
                    metadataContainer.addView(createInfoRow(
                        context.getString(R.string.info_framerate),
                        String.format("%.2f fps", frameRate)
                    ))
                }
                
                if (rotation != null && rotation != 0) {
                    metadataContainer.addView(createInfoRow(
                        context.getString(R.string.info_rotation),
                        "$rotation°"
                    ))
                }
                
                if (mimeType != null) {
                    metadataContainer.addView(createInfoRow(
                        context.getString(R.string.info_codec),
                        mimeType.replace("video/", "").uppercase()
                    ))
                }
                
                if (date != null) {
                    metadataContainer.addView(createInfoRow(
                        context.getString(R.string.info_date_recorded),
                        date
                    ))
                }
            }
            
            // Also load audio track info
            loadAudioTrackInfo(retriever)
            
        } catch (e: Exception) {
            Timber.e(e, "Error loading video metadata")
        } finally {
            try { retriever.release() } catch (e: Exception) { }
        }
    }

    private suspend fun loadAudioTrackInfo(retriever: MediaMetadataRetriever) {
        try {
            val hasAudio = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO)
            
            if (hasAudio == "yes") {
                val sampleRate = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)?.toIntOrNull()
                } else null
                
                if (sampleRate != null) {
                    withContext(Dispatchers.Main) {
                        metadataContainer.addView(createSectionHeader(context.getString(R.string.info_section_audio_track)))
                        
                        metadataContainer.addView(createInfoRow(
                            context.getString(R.string.info_sample_rate),
                            "${sampleRate / 1000.0} kHz"
                        ))
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error loading audio track info")
        }
    }

    private suspend fun loadAudioMetadata() {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(file.absolutePath)
            
            withContext(Dispatchers.Main) {
                metadataContainer.addView(createSectionHeader(context.getString(R.string.info_section_audio)))
            }
            
            // Duration
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            
            // Bitrate
            val bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toLongOrNull()
            
            // Title
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            
            // Artist
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            
            // Album
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
            
            // Album Artist
            val albumArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
            
            // Year
            val year = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)
            
            // Track Number
            val trackNumber = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
            
            // Genre
            val genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)
            
            // Composer
            val composer = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COMPOSER)
            
            // MIME type
            val mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
            
            // Sample rate (Android Q+)
            val sampleRate = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)?.toIntOrNull()
            } else null
            
            // Bits per sample (Android Q+)
            val bitsPerSample = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITS_PER_SAMPLE)?.toIntOrNull()
            } else null
            
            withContext(Dispatchers.Main) {
                if (durationMs != null) {
                    metadataContainer.addView(createInfoRow(
                        context.getString(R.string.info_duration),
                        formatDuration(durationMs)
                    ))
                }
                
                if (title != null) {
                    metadataContainer.addView(createInfoRow(
                        context.getString(R.string.info_title),
                        title
                    ))
                }
                
                if (artist != null) {
                    metadataContainer.addView(createInfoRow(
                        context.getString(R.string.info_artist),
                        artist
                    ))
                }
                
                if (album != null) {
                    metadataContainer.addView(createInfoRow(
                        context.getString(R.string.info_album),
                        album
                    ))
                }
                
                if (albumArtist != null && albumArtist != artist) {
                    metadataContainer.addView(createInfoRow(
                        context.getString(R.string.info_album_artist),
                        albumArtist
                    ))
                }
                
                if (year != null) {
                    metadataContainer.addView(createInfoRow(
                        context.getString(R.string.info_year),
                        year
                    ))
                }
                
                if (trackNumber != null) {
                    metadataContainer.addView(createInfoRow(
                        context.getString(R.string.info_track),
                        trackNumber
                    ))
                }
                
                if (genre != null) {
                    metadataContainer.addView(createInfoRow(
                        context.getString(R.string.info_genre),
                        genre
                    ))
                }
                
                if (composer != null) {
                    metadataContainer.addView(createInfoRow(
                        context.getString(R.string.info_composer),
                        composer
                    ))
                }
                
                if (bitrate != null) {
                    metadataContainer.addView(createInfoRow(
                        context.getString(R.string.info_bitrate),
                        formatBitrate(bitrate)
                    ))
                }
                
                if (sampleRate != null) {
                    metadataContainer.addView(createInfoRow(
                        context.getString(R.string.info_sample_rate),
                        "${sampleRate / 1000.0} kHz"
                    ))
                }
                
                if (bitsPerSample != null) {
                    metadataContainer.addView(createInfoRow(
                        context.getString(R.string.info_bit_depth),
                        "$bitsPerSample bit"
                    ))
                }
                
                if (mimeType != null) {
                    metadataContainer.addView(createInfoRow(
                        context.getString(R.string.info_format),
                        mimeType.replace("audio/", "").uppercase()
                    ))
                }
            }
            
        } catch (e: Exception) {
            Timber.e(e, "Error loading audio metadata")
        } finally {
            try { retriever.release() } catch (e: Exception) { }
        }
    }

    private suspend fun loadDocumentMetadata() {
        val extension = file.extension.lowercase()
        
        withContext(Dispatchers.Main) {
            metadataContainer.addView(createSectionHeader(context.getString(R.string.info_section_document)))
        }
        
        when (extension) {
            "txt" -> loadTextFileMetadata()
            "pdf" -> loadPdfMetadata()
        }
    }

    private suspend fun loadTextFileMetadata() {
        try {
            val content = file.readText()
            val lineCount = content.lines().size
            val wordCount = content.split(Regex("\\s+")).filter { it.isNotEmpty() }.size
            val charCount = content.length
            
            withContext(Dispatchers.Main) {
                metadataContainer.addView(createInfoRow(
                    context.getString(R.string.info_lines),
                    lineCount.toString()
                ))
                
                metadataContainer.addView(createInfoRow(
                    context.getString(R.string.info_words),
                    wordCount.toString()
                ))
                
                metadataContainer.addView(createInfoRow(
                    context.getString(R.string.info_characters),
                    charCount.toString()
                ))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error loading text file metadata")
        }
    }

    private suspend fun loadPdfMetadata() {
        try {
            val pdfRenderer = android.graphics.pdf.PdfRenderer(
                android.os.ParcelFileDescriptor.open(file, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
            )
            
            val pageCount = pdfRenderer.pageCount
            pdfRenderer.close()
            
            withContext(Dispatchers.Main) {
                metadataContainer.addView(createInfoRow(
                    context.getString(R.string.info_pages),
                    pageCount.toString()
                ))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error loading PDF metadata")
        }
    }

    private fun createClickableInfoRow(label: String, value: String, onClick: () -> Unit): View {
        val row = createInfoRow(label, value)
        val valueView = (row as LinearLayout).getChildAt(1) as TextView
        valueView.setTextColor(context.getColor(R.color.colorPrimary))
        valueView.setOnClickListener { onClick() }
        return row
    }

    private fun openGoogleMaps(lat: Float, lon: Float) {
        try {
            val uri = android.net.Uri.parse("https://www.google.com/maps?q=$lat,$lon")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            context.startActivity(intent)
        } catch (e: Exception) {
            Timber.e(e, "Failed to open Google Maps")
        }
    }

    // === Formatting Helpers ===

    private fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val df = DecimalFormat("#,##0.#")
        return "${df.format(bytes / Math.pow(1024.0, digitGroups.toDouble()))} ${units[digitGroups]}"
    }

    private fun formatDate(timestamp: Long): String {
        val dateFormat = DateFormat.getMediumDateFormat(context)
        val timeFormat = DateFormat.getTimeFormat(context)
        val date = Date(timestamp)
        return "${dateFormat.format(date)} ${timeFormat.format(date)}"
    }

    private fun formatDuration(milliseconds: Long): String {
        val totalSeconds = milliseconds / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%d:%02d", minutes, seconds)
        }
    }

    private fun formatBitrate(bitsPerSecond: Long): String {
        return when {
            bitsPerSecond >= 1_000_000 -> String.format("%.1f Mbps", bitsPerSecond / 1_000_000.0)
            bitsPerSecond >= 1_000 -> String.format("%.0f kbps", bitsPerSecond / 1_000.0)
            else -> "$bitsPerSecond bps"
        }
    }

    private fun formatExposureTime(exposure: String): String {
        return try {
            val value = exposure.toDouble()
            if (value < 1) {
                "1/${(1 / value).toInt()} sec"
            } else {
                "$value sec"
            }
        } catch (e: Exception) {
            exposure
        }
    }

    private fun parseFocalLength(focalLength: String): Float? {
        return try {
            if (focalLength.contains("/")) {
                val parts = focalLength.split("/")
                parts[0].toFloat() / parts[1].toFloat()
            } else {
                focalLength.toFloat()
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun getVideoQualityLabel(width: Int, height: Int): String? {
        val maxDim = maxOf(width, height)
        return when {
            maxDim >= 7680 -> "8K UHD"
            maxDim >= 3840 -> "4K UHD"
            maxDim >= 2560 -> "1440p QHD"
            maxDim >= 1920 -> "1080p Full HD"
            maxDim >= 1280 -> "720p HD"
            maxDim >= 854 -> "480p SD"
            maxDim >= 640 -> "360p"
            else -> null
        }
    }

    private fun getFileType(): String {
        val extension = file.extension.lowercase()
        return when (extension) {
            "jpg", "jpeg" -> "JPEG Image"
            "png" -> "PNG Image"
            "gif" -> "GIF Image"
            "webp" -> "WebP Image"
            "bmp" -> "Bitmap Image"
            "heic", "heif" -> "HEIC Image"
            "mp4" -> "MP4 Video"
            "mkv" -> "MKV Video"
            "mov" -> "QuickTime Video"
            "avi" -> "AVI Video"
            "webm" -> "WebM Video"
            "3gp" -> "3GP Video"
            "mp3" -> "MP3 Audio"
            "wav" -> "WAV Audio"
            "flac" -> "FLAC Audio"
            "m4a" -> "M4A Audio"
            "aac" -> "AAC Audio"
            "ogg" -> "Ogg Audio"
            "opus" -> "Opus Audio"
            "pdf" -> "PDF Document"
            "txt" -> "Text File"
            "epub" -> "EPUB E-Book"
            else -> extension.uppercase().ifEmpty { "Unknown" }
        }
    }

    private enum class MediaCategory {
        IMAGE, VIDEO, AUDIO, DOCUMENT, OTHER
    }

    private fun getMediaCategory(extension: String): MediaCategory {
        return when (extension) {
            "jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif" -> MediaCategory.IMAGE
            "mp4", "mkv", "mov", "avi", "webm", "3gp", "ts" -> MediaCategory.VIDEO
            "mp3", "wav", "flac", "m4a", "aac", "ogg", "opus" -> MediaCategory.AUDIO
            "pdf", "txt", "epub" -> MediaCategory.DOCUMENT
            else -> MediaCategory.OTHER
        }
    }

    private fun openWithExternalApp() {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, getMimeType())
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            val chooser = Intent.createChooser(intent, context.getString(R.string.open_with))
            context.startActivity(chooser)
            dismiss()
        } catch (e: Exception) {
            Timber.e(e, "Failed to open file with external app")
        }
    }

    private fun getMimeType(): String {
        val extension = file.extension.lowercase()
        return when (extension) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "bmp" -> "image/bmp"
            "heic", "heif" -> "image/heif"
            "mp4" -> "video/mp4"
            "mkv" -> "video/x-matroska"
            "mov" -> "video/quicktime"
            "avi" -> "video/x-msvideo"
            "webm" -> "video/webm"
            "3gp" -> "video/3gpp"
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "flac" -> "audio/flac"
            "m4a" -> "audio/mp4"
            "aac" -> "audio/aac"
            "ogg" -> "audio/ogg"
            "opus" -> "audio/opus"
            "pdf" -> "application/pdf"
            "txt" -> "text/plain"
            "epub" -> "application/epub+zip"
            else -> "*/*"
        }
    }
}
