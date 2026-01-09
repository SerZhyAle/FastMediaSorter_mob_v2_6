package com.sza.fastmediasorter.ui.player

import android.os.CountDownTimer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * Controls slideshow functionality for automatic media navigation.
 * Manages timer-based auto-advance, countdown display, and pause/resume state.
 *
 * Features:
 * - Timer-based auto-advance (5-60 second intervals)
 * - Countdown display with last 3 seconds shown
 * - Pause/Resume capability
 * - Random shuffle option
 * - Skip videos option
 * - Lifecycle-aware (pauses on background)
 */
class SlideshowController(
    private val onAdvance: () -> Unit,
    private val onCountdownTick: (Int) -> Unit = {}
) {
    companion object {
        private const val TAG = "SlideshowController"
        
        // Interval options in seconds
        val INTERVAL_OPTIONS = listOf(5, 10, 15, 30, 60)
        const val DEFAULT_INTERVAL = 10
        const val COUNTDOWN_THRESHOLD = 3 // Show countdown for last 3 seconds
    }

    /**
     * State of the slideshow.
     */
    data class SlideshowState(
        val isActive: Boolean = false,
        val isPaused: Boolean = false,
        val intervalSeconds: Int = DEFAULT_INTERVAL,
        val remainingSeconds: Int = DEFAULT_INTERVAL,
        val showCountdown: Boolean = false,
        val isRandom: Boolean = false,
        val skipVideos: Boolean = false
    ) {
        val isRunning: Boolean get() = isActive && !isPaused
    }

    private val _state = MutableStateFlow(SlideshowState())
    val state: StateFlow<SlideshowState> = _state.asStateFlow()

    private var countDownTimer: CountDownTimer? = null

    /**
     * Start the slideshow with the specified interval.
     */
    fun start(intervalSeconds: Int = state.value.intervalSeconds) {
        Timber.d("Starting slideshow with interval: $intervalSeconds seconds")
        
        _state.value = _state.value.copy(
            isActive = true,
            isPaused = false,
            intervalSeconds = intervalSeconds,
            remainingSeconds = intervalSeconds,
            showCountdown = false
        )
        
        startTimer()
    }

    /**
     * Stop the slideshow completely.
     */
    fun stop() {
        Timber.d("Stopping slideshow")
        
        cancelTimer()
        _state.value = SlideshowState(
            intervalSeconds = _state.value.intervalSeconds,
            isRandom = _state.value.isRandom,
            skipVideos = _state.value.skipVideos
        )
    }

    /**
     * Toggle between started/stopped state.
     */
    fun toggle() {
        if (_state.value.isActive) {
            stop()
        } else {
            start()
        }
    }

    /**
     * Pause the slideshow (preserves state).
     */
    fun pause() {
        if (!_state.value.isActive || _state.value.isPaused) return
        
        Timber.d("Pausing slideshow at ${_state.value.remainingSeconds}s remaining")
        cancelTimer()
        _state.value = _state.value.copy(isPaused = true)
    }

    /**
     * Resume a paused slideshow.
     */
    fun resume() {
        if (!_state.value.isActive || !_state.value.isPaused) return
        
        Timber.d("Resuming slideshow with ${_state.value.remainingSeconds}s remaining")
        _state.value = _state.value.copy(isPaused = false)
        startTimer(_state.value.remainingSeconds)
    }

    /**
     * Toggle pause/resume state.
     */
    fun togglePause() {
        if (_state.value.isPaused) {
            resume()
        } else {
            pause()
        }
    }

    /**
     * Set the slideshow interval.
     */
    fun setInterval(intervalSeconds: Int) {
        _state.value = _state.value.copy(
            intervalSeconds = intervalSeconds,
            remainingSeconds = if (_state.value.isActive) _state.value.remainingSeconds else intervalSeconds
        )
        
        // Restart timer if running
        if (_state.value.isRunning) {
            cancelTimer()
            startTimer()
        }
    }

    /**
     * Set random shuffle mode.
     */
    fun setRandom(enabled: Boolean) {
        _state.value = _state.value.copy(isRandom = enabled)
    }

    /**
     * Set skip videos mode.
     */
    fun setSkipVideos(enabled: Boolean) {
        _state.value = _state.value.copy(skipVideos = enabled)
    }

    /**
     * Reset timer after manual navigation (preserves slideshow state).
     */
    fun resetTimer() {
        if (!_state.value.isActive) return
        
        Timber.d("Resetting slideshow timer")
        cancelTimer()
        _state.value = _state.value.copy(
            remainingSeconds = _state.value.intervalSeconds,
            showCountdown = false
        )
        
        if (!_state.value.isPaused) {
            startTimer()
        }
    }

    /**
     * Call when entering background (pauses timer).
     */
    fun onBackground() {
        if (_state.value.isRunning) {
            cancelTimer()
            Timber.d("Slideshow paused for background")
        }
    }

    /**
     * Call when returning to foreground (resumes timer).
     */
    fun onForeground() {
        if (_state.value.isActive && !_state.value.isPaused) {
            startTimer(_state.value.remainingSeconds)
            Timber.d("Slideshow resumed from background")
        }
    }

    private fun startTimer(seconds: Int = _state.value.intervalSeconds) {
        cancelTimer()
        
        val milliseconds = seconds * 1000L
        
        countDownTimer = object : CountDownTimer(milliseconds, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val remaining = (millisUntilFinished / 1000).toInt() + 1
                val showCountdown = remaining <= COUNTDOWN_THRESHOLD
                
                _state.value = _state.value.copy(
                    remainingSeconds = remaining,
                    showCountdown = showCountdown
                )
                
                if (showCountdown) {
                    onCountdownTick(remaining)
                }
            }

            override fun onFinish() {
                _state.value = _state.value.copy(
                    remainingSeconds = 0,
                    showCountdown = false
                )
                
                // Advance to next slide
                onAdvance()
                
                // Restart timer for next slide
                if (_state.value.isActive && !_state.value.isPaused) {
                    _state.value = _state.value.copy(
                        remainingSeconds = _state.value.intervalSeconds
                    )
                    startTimer()
                }
            }
        }.start()
    }

    private fun cancelTimer() {
        countDownTimer?.cancel()
        countDownTimer = null
    }

    /**
     * Clean up resources.
     */
    fun release() {
        cancelTimer()
    }
}
