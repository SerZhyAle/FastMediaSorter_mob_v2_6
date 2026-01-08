package com.sza.fastmediasorter.ui.settings.media

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sza.fastmediasorter.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Shared ViewModel for all media settings fragments.
 * Contains settings for Images, Video, Audio, Documents, and Other media.
 */
@HiltViewModel
class MediaSettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MediaSettingsUiState())
    val uiState: StateFlow<MediaSettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            try {
                _uiState.update { state ->
                    state.copy(
                        // Image settings
                        supportImages = preferencesRepository.supportImages.first(),
                        thumbnailQuality = preferencesRepository.thumbnailQuality.first(),
                        autoRotateImages = preferencesRepository.autoRotateImages.first(),
                        showExifData = preferencesRepository.showExifData.first(),
                        jpegQuality = preferencesRepository.jpegQuality.first(),
                        loadFullSizeImages = preferencesRepository.loadFullSizeImages.first(),
                        
                        // Video settings
                        supportVideo = preferencesRepository.supportVideo.first(),
                        videoQuality = preferencesRepository.videoQuality.first(),
                        hardwareAcceleration = preferencesRepository.hardwareAcceleration.first(),
                        seekIncrement = preferencesRepository.seekIncrement.first(),
                        previewDuration = preferencesRepository.previewDuration.first(),
                        showVideoThumbnails = preferencesRepository.showVideoThumbnails.first(),
                        
                        // Audio settings
                        supportAudio = preferencesRepository.supportAudio.first(),
                        waveformStyle = preferencesRepository.waveformStyle.first(),
                        waveformColor = preferencesRepository.waveformColor.first(),
                        backgroundPlayback = preferencesRepository.backgroundPlayback.first(),
                        audioFocusHandling = preferencesRepository.audioFocusHandling.first(),
                        searchAlbumCovers = preferencesRepository.searchAlbumCovers.first(),
                        
                        // Document settings
                        supportText = preferencesRepository.supportText.first(),
                        supportPdf = preferencesRepository.supportPdf.first(),
                        supportEpub = preferencesRepository.supportEpub.first(),
                        pdfPageCache = preferencesRepository.pdfPageCache.first(),
                        pdfRenderQuality = preferencesRepository.pdfRenderQuality.first(),
                        textEncoding = preferencesRepository.textEncoding.first(),
                        epubFontFamily = preferencesRepository.epubFontFamily.first(),
                        epubFontSize = preferencesRepository.epubFontSize.first(),
                        
                        // Other settings
                        supportGif = preferencesRepository.supportGif.first(),
                        gifFrameRateLimit = preferencesRepository.gifFrameRateLimit.first()
                    )
                }
                Timber.d("MediaSettingsViewModel: Settings loaded")
            } catch (e: Exception) {
                Timber.e(e, "Error loading media settings")
            }
        }
    }

    // ==================== Image Settings ====================

    fun setSupportImages(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setSupportImages(enabled)
            _uiState.update { it.copy(supportImages = enabled) }
        }
    }

    fun setThumbnailQuality(quality: String) {
        viewModelScope.launch {
            preferencesRepository.setThumbnailQuality(quality)
            _uiState.update { it.copy(thumbnailQuality = quality) }
        }
    }

    fun setAutoRotateImages(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setAutoRotateImages(enabled)
            _uiState.update { it.copy(autoRotateImages = enabled) }
        }
    }

    fun setShowExifData(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setShowExifData(enabled)
            _uiState.update { it.copy(showExifData = enabled) }
        }
    }

    fun setJpegQuality(quality: Int) {
        viewModelScope.launch {
            preferencesRepository.setJpegQuality(quality)
            _uiState.update { it.copy(jpegQuality = quality) }
        }
    }

    fun setLoadFullSizeImages(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setLoadFullSizeImages(enabled)
            _uiState.update { it.copy(loadFullSizeImages = enabled) }
        }
    }

    // ==================== Video Settings ====================

    fun setSupportVideo(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setSupportVideo(enabled)
            _uiState.update { it.copy(supportVideo = enabled) }
        }
    }

    fun setVideoQuality(quality: String) {
        viewModelScope.launch {
            preferencesRepository.setVideoQuality(quality)
            _uiState.update { it.copy(videoQuality = quality) }
        }
    }

    fun setHardwareAcceleration(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setHardwareAcceleration(enabled)
            _uiState.update { it.copy(hardwareAcceleration = enabled) }
        }
    }

    fun setSeekIncrement(seconds: Int) {
        viewModelScope.launch {
            preferencesRepository.setSeekIncrement(seconds)
            _uiState.update { it.copy(seekIncrement = seconds) }
        }
    }

    fun setPreviewDuration(seconds: Int) {
        viewModelScope.launch {
            preferencesRepository.setPreviewDuration(seconds)
            _uiState.update { it.copy(previewDuration = seconds) }
        }
    }

    fun setShowVideoThumbnails(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setShowVideoThumbnails(enabled)
            _uiState.update { it.copy(showVideoThumbnails = enabled) }
        }
    }

    // ==================== Audio Settings ====================

    fun setSupportAudio(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setSupportAudio(enabled)
            _uiState.update { it.copy(supportAudio = enabled) }
        }
    }

    fun setWaveformStyle(style: String) {
        viewModelScope.launch {
            preferencesRepository.setWaveformStyle(style)
            _uiState.update { it.copy(waveformStyle = style) }
        }
    }

    fun setWaveformColor(color: String) {
        viewModelScope.launch {
            preferencesRepository.setWaveformColor(color)
            _uiState.update { it.copy(waveformColor = color) }
        }
    }

    fun setBackgroundPlayback(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setBackgroundPlayback(enabled)
            _uiState.update { it.copy(backgroundPlayback = enabled) }
        }
    }

    fun setAudioFocusHandling(mode: String) {
        viewModelScope.launch {
            preferencesRepository.setAudioFocusHandling(mode)
            _uiState.update { it.copy(audioFocusHandling = mode) }
        }
    }

    fun setSearchAlbumCovers(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setSearchAlbumCovers(enabled)
            _uiState.update { it.copy(searchAlbumCovers = enabled) }
        }
    }

    // ==================== Document Settings ====================

    fun setSupportText(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setSupportText(enabled)
            _uiState.update { it.copy(supportText = enabled) }
        }
    }

    fun setSupportPdf(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setSupportPdf(enabled)
            _uiState.update { it.copy(supportPdf = enabled) }
        }
    }

    fun setSupportEpub(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setSupportEpub(enabled)
            _uiState.update { it.copy(supportEpub = enabled) }
        }
    }

    fun setPdfPageCache(pages: Int) {
        viewModelScope.launch {
            preferencesRepository.setPdfPageCache(pages)
            _uiState.update { it.copy(pdfPageCache = pages) }
        }
    }

    fun setPdfRenderQuality(quality: String) {
        viewModelScope.launch {
            preferencesRepository.setPdfRenderQuality(quality)
            _uiState.update { it.copy(pdfRenderQuality = quality) }
        }
    }

    fun setTextEncoding(encoding: String) {
        viewModelScope.launch {
            preferencesRepository.setTextEncoding(encoding)
            _uiState.update { it.copy(textEncoding = encoding) }
        }
    }

    fun setEpubFontFamily(family: String) {
        viewModelScope.launch {
            preferencesRepository.setEpubFontFamily(family)
            _uiState.update { it.copy(epubFontFamily = family) }
        }
    }

    fun setEpubFontSize(size: Int) {
        viewModelScope.launch {
            preferencesRepository.setEpubFontSize(size)
            _uiState.update { it.copy(epubFontSize = size) }
        }
    }

    // ==================== Other Media Settings ====================

    fun setSupportGif(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setSupportGif(enabled)
            _uiState.update { it.copy(supportGif = enabled) }
        }
    }

    fun setGifFrameRateLimit(fps: Int) {
        viewModelScope.launch {
            preferencesRepository.setGifFrameRateLimit(fps)
            _uiState.update { it.copy(gifFrameRateLimit = fps) }
        }
    }
}

/**
 * UI state for media settings.
 */
data class MediaSettingsUiState(
    // Image settings
    val supportImages: Boolean = true,
    val thumbnailQuality: String = "medium",
    val autoRotateImages: Boolean = true,
    val showExifData: Boolean = true,
    val jpegQuality: Int = 85,
    val loadFullSizeImages: Boolean = false,
    
    // Video settings
    val supportVideo: Boolean = true,
    val videoQuality: String = "auto",
    val hardwareAcceleration: Boolean = true,
    val seekIncrement: Int = 10,
    val previewDuration: Int = 3,
    val showVideoThumbnails: Boolean = true,
    
    // Audio settings
    val supportAudio: Boolean = true,
    val waveformStyle: String = "bars",
    val waveformColor: String = "#03DAC5",
    val backgroundPlayback: Boolean = true,
    val audioFocusHandling: String = "duck",
    val searchAlbumCovers: Boolean = false,
    
    // Document settings
    val supportText: Boolean = true,
    val supportPdf: Boolean = true,
    val supportEpub: Boolean = true,
    val pdfPageCache: Int = 5,
    val pdfRenderQuality: String = "high",
    val textEncoding: String = "auto",
    val epubFontFamily: String = "serif",
    val epubFontSize: Int = 16,
    
    // Other media settings
    val supportGif: Boolean = true,
    val gifFrameRateLimit: Int = 0
)
