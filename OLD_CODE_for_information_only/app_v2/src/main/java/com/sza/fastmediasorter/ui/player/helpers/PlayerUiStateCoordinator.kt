package com.sza.fastmediasorter.ui.player.helpers

import androidx.core.view.isVisible
import com.sza.fastmediasorter.R
import com.sza.fastmediasorter.domain.model.MediaFile
import com.sza.fastmediasorter.domain.model.MediaType
import com.sza.fastmediasorter.domain.repository.SettingsRepository
import com.sza.fastmediasorter.domain.model.AppSettings
import com.sza.fastmediasorter.databinding.ActivityPlayerUnifiedBinding
import com.sza.fastmediasorter.ui.player.PlayerViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Coordinates UI state rendering for PlayerActivity.
 * Keeps PlayerActivity slimmer by centralizing updateUI(state) logic.
 */
class PlayerUiStateCoordinator(
    private val binding: ActivityPlayerUnifiedBinding,
    private val settingsRepository: SettingsRepository,
    private val coroutineScope: CoroutineScope,
    private val callback: Callback
) {

    private val mediaDisplayCoordinator = MediaDisplayCoordinator(
        callback = object : MediaDisplayCoordinator.Callback {
            override fun displayImage(path: String) = callback.displayImage(path)
            override fun playVideo(path: String) = callback.playVideo(path)
            override fun displayText(file: MediaFile) = callback.displayText(file)
            override fun displayPdf(file: MediaFile) = callback.displayPdf(file)
            override fun displayEpub(file: MediaFile) = callback.displayEpub(file)
        }
    )

    interface Callback {
        fun isActivityAlive(): Boolean

        fun getCurrentSettings(): AppSettings?
        fun setCurrentSettings(settings: AppSettings?)

        fun getCurrentFilePath(): String?
        fun setCurrentFilePath(path: String?)

        fun isSlideshowModeRequested(): Boolean
        fun clearSlideshowModeRequested()

        fun hasShownFirstRunHint(): Boolean
        fun markFirstRunHintShown()

        fun getUseTouchZones(): Boolean

        fun displayImage(path: String)
        fun playVideo(path: String)
        fun displayText(file: MediaFile)
        fun displayPdf(file: MediaFile)
        fun displayEpub(file: MediaFile)

        fun adjustTouchZonesForVideo(isVideo: Boolean)
        fun updatePanelVisibility(showCommandPanel: Boolean)
        fun updateCommandAvailability(state: PlayerViewModel.PlayerState)

        fun updatePlayPauseButton()
        fun updateSlideShowButton()
        fun updateVolumeButtonsVisibility()

        fun showFirstRunHintOverlay()
        fun showSlideshowEnabledMessage()
        fun toggleSlideShow()
        fun startSlideshow(intervalSeconds: Int)
        fun getLatestState(): PlayerViewModel.PlayerState
    }

    fun updateUI(state: PlayerViewModel.PlayerState) {
        Timber.d(
            "PlayerUiStateCoordinator.updateUI: START - currentFile=${state.currentFile?.name}, type=${state.currentFile?.type}"
        )

        if (!callback.isActivityAlive()) {
            Timber.d("PlayerUiStateCoordinator.updateUI: Activity not alive, skipping")
            return
        }


        if (state.files.isEmpty()) {
            Timber.d("PlayerUiStateCoordinator.updateUI: Files not loaded yet, skipping")
            return
        }

        // Auto-start slideshow if requested via intent (only once after files are loaded)
        Timber.d("PlayerUiStateCoordinator.updateUI: Checking slideshow auto-start - isSlideshowModeRequested=${callback.isSlideshowModeRequested()}, currentFile=${state.currentFile?.name}")
        if (callback.isSlideshowModeRequested() && state.currentFile != null) {
            callback.clearSlideshowModeRequested()
            Timber.d("PlayerUiStateCoordinator.updateUI: Auto-starting slideshow (requested via intent)")

            coroutineScope.launch {
                delay(300)
                if (!callback.getLatestState().isSlideShowActive) {
                    val intervalSeconds = (callback.getLatestState().slideShowInterval / 1000).toInt()
                    Timber.d("PlayerUiStateCoordinator: Calling toggleSlideShow() and startSlideshow($intervalSeconds)")
                    callback.toggleSlideShow()
                    callback.startSlideshow(intervalSeconds)
                    callback.showSlideshowEnabledMessage()
                    callback.updateSlideShowButton()
                    Timber.d("PlayerUiStateCoordinator: Slideshow auto-start COMPLETE")
                } else {
                    Timber.w("PlayerUiStateCoordinator: Slideshow already active, skipping auto-start")
                }
            }
        } else {
            Timber.d("PlayerUiStateCoordinator.updateUI: Slideshow auto-start NOT triggered")
        }


        // Show first-run hint if enabled and not shown yet (only in fullscreen mode without command panel)
        if (!callback.hasShownFirstRunHint() && state.currentFile != null && !state.showCommandPanel) {
            coroutineScope.launch {
                val settings = settingsRepository.getSettings().first()
                val isFirstRun = settingsRepository.isPlayerFirstRun()

                if (settings.showPlayerHintOnFirstRun && isFirstRun) {
                    Timber.d("PlayerUiStateCoordinator.updateUI: Showing first-run hint overlay (fullscreen mode)")
                    delay(500)
                    callback.showFirstRunHintOverlay()
                    settingsRepository.setPlayerFirstRun(false)
                    callback.markFirstRunHintShown()
                }
            }
        }

        state.currentFile?.let { file ->
            binding.toolbar.title = "${state.currentIndex + 1}/${state.files.size} - ${file.name}"
            binding.btnPrevious.isEnabled = state.hasPrevious
            binding.btnNext.isEnabled = state.hasNext
            binding.btnPreviousCmd.isEnabled = state.hasPrevious
            binding.btnPreviousCmd.isEnabled = state.hasPrevious
            binding.btnNextCmd.isEnabled = state.hasNext

            binding.btnFavorite.setImageResource(
                if (file.isFavorite) R.drawable.ic_star_filled else R.drawable.ic_star_outline
            )

            binding.tvFileNameOverlay.text = file.name

            val currentFilePath = callback.getCurrentFilePath()
            if (currentFilePath != file.path) {
                Timber.d(
                    "PlayerUiStateCoordinator.updateUI: File changed from '$currentFilePath' to '${file.path}' - reloading media"
                )
                callback.setCurrentFilePath(file.path)

                // Hide translation overlays when changing files
                binding.translationOverlay.isVisible = false
                binding.translationLensOverlay.isVisible = false

                mediaDisplayCoordinator.display(file)
            } else {
                Timber.d("PlayerUiStateCoordinator.updateUI: Same file path - skipping media reload (metadata update only)")
            }

            // Determine if file should disable touch zones (VIDEO/AUDIO use ExoPlayer controls)
            // - AUDIO: disable touch zones (has ExoPlayer controls for Previous/Next)
            // - VIDEO: disable touch zones (has ExoPlayer controls for Previous/Next)
            // - GIF: treat as image - ENABLE touch zones for Previous/Next navigation
            // - IMAGE: ENABLE touch zones for Previous/Next navigation
            val isGif = file.type == MediaType.GIF
            val isVideo = file.type == MediaType.AUDIO || (file.type == MediaType.VIDEO && !isGif)
            callback.adjustTouchZonesForVideo(isVideo)
        }

        callback.updatePanelVisibility(state.showCommandPanel)
        callback.updateCommandAvailability(state)

        val shouldShowControls = !state.showCommandPanel && state.showControls && !callback.getUseTouchZones()
        binding.controlsOverlay.isVisible = shouldShowControls

        val currentFile = state.currentFile
        val settings = callback.getCurrentSettings()
        binding.touchZonesOverlayNew.isVisible =
            !state.showCommandPanel &&
                settings?.alwaysShowTouchZonesOverlay == true &&
                currentFile != null &&
                (currentFile.type == MediaType.IMAGE || currentFile.type == MediaType.GIF)

        callback.updatePlayPauseButton()
        callback.updateSlideShowButton()
        callback.updateVolumeButtonsVisibility()

        // Note: updateAudioTouchZonesVisibility() is invoked inside updatePanelVisibility()
    }
}
