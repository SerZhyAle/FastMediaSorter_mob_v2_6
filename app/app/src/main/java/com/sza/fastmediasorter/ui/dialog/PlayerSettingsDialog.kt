package com.sza.fastmediasorter.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.sza.fastmediasorter.R
import com.sza.fastmediasorter.databinding.DialogPlayerSettingsBinding
import com.sza.fastmediasorter.domain.model.MediaType

/**
 * Bottom sheet dialog for quick player settings.
 *
 * Features:
 * - Slideshow interval slider
 * - Random order toggle
 * - Touch zones toggle
 * - Video playback speed (for videos)
 * - Video repeat mode (for videos)
 * - Immediate apply (no OK button)
 */
class PlayerSettingsDialog : BottomSheetDialogFragment() {

    companion object {
        const val TAG = "PlayerSettingsDialog"
        private const val ARG_MEDIA_TYPE = "media_type"
        private const val ARG_SLIDESHOW_INTERVAL = "slideshow_interval"
        private const val ARG_RANDOM_ORDER = "random_order"
        private const val ARG_TOUCH_ZONES_ENABLED = "touch_zones_enabled"
        private const val ARG_SHOW_ZONE_LABELS = "show_zone_labels"
        private const val ARG_VIDEO_SPEED = "video_speed"
        private const val ARG_REPEAT_MODE = "repeat_mode"

        fun newInstance(
            mediaType: MediaType?,
            slideshowInterval: Int = 3,
            randomOrder: Boolean = false,
            touchZonesEnabled: Boolean = true,
            showZoneLabels: Boolean = false,
            videoSpeed: Float = 1.0f,
            repeatMode: RepeatMode = RepeatMode.OFF
        ): PlayerSettingsDialog {
            return PlayerSettingsDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_MEDIA_TYPE, mediaType?.name)
                    putInt(ARG_SLIDESHOW_INTERVAL, slideshowInterval)
                    putBoolean(ARG_RANDOM_ORDER, randomOrder)
                    putBoolean(ARG_TOUCH_ZONES_ENABLED, touchZonesEnabled)
                    putBoolean(ARG_SHOW_ZONE_LABELS, showZoneLabels)
                    putFloat(ARG_VIDEO_SPEED, videoSpeed)
                    putString(ARG_REPEAT_MODE, repeatMode.name)
                }
            }
        }
    }

    enum class RepeatMode {
        OFF, ONE, ALL
    }

    private var _binding: DialogPlayerSettingsBinding? = null
    private val binding get() = _binding!!

    // Settings listeners for immediate apply
    var onSlideshowIntervalChanged: ((Int) -> Unit)? = null
    var onRandomOrderChanged: ((Boolean) -> Unit)? = null
    var onTouchZonesEnabledChanged: ((Boolean) -> Unit)? = null
    var onShowZoneLabelsChanged: ((Boolean) -> Unit)? = null
    var onVideoSpeedChanged: ((Float) -> Unit)? = null
    var onRepeatModeChanged: ((RepeatMode) -> Unit)? = null

    private var mediaType: MediaType? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mediaType = arguments?.getString(ARG_MEDIA_TYPE)?.let { MediaType.valueOf(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogPlayerSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupInitialValues()
        setupListeners()

        // Show video settings only for video media type
        binding.layoutVideoSettings.visibility =
            if (mediaType == MediaType.VIDEO) View.VISIBLE else View.GONE
    }

    private fun setupInitialValues() {
        val args = arguments ?: return

        // Slideshow interval
        val interval = args.getInt(ARG_SLIDESHOW_INTERVAL, 3)
        binding.sliderInterval.value = interval.toFloat()
        binding.tvIntervalValue.text = "${interval}s"

        // Random order
        binding.switchRandomOrder.isChecked = args.getBoolean(ARG_RANDOM_ORDER, false)

        // Touch zones
        binding.switchTouchZones.isChecked = args.getBoolean(ARG_TOUCH_ZONES_ENABLED, true)
        binding.switchShowZoneLabels.isChecked = args.getBoolean(ARG_SHOW_ZONE_LABELS, false)

        // Video speed
        val speed = args.getFloat(ARG_VIDEO_SPEED, 1.0f)
        binding.sliderSpeed.value = speed
        binding.tvSpeedValue.text = "${speed}x"

        // Repeat mode
        val repeatMode = args.getString(ARG_REPEAT_MODE)?.let { RepeatMode.valueOf(it) } ?: RepeatMode.OFF
        when (repeatMode) {
            RepeatMode.OFF -> binding.toggleRepeatMode.check(R.id.btnRepeatOff)
            RepeatMode.ONE -> binding.toggleRepeatMode.check(R.id.btnRepeatOne)
            RepeatMode.ALL -> binding.toggleRepeatMode.check(R.id.btnRepeatAll)
        }
    }

    private fun setupListeners() {
        // Slideshow interval slider
        binding.sliderInterval.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val interval = value.toInt()
                binding.tvIntervalValue.text = "${interval}s"
                onSlideshowIntervalChanged?.invoke(interval)
            }
        }

        // Random order switch
        binding.switchRandomOrder.setOnCheckedChangeListener { _, isChecked ->
            onRandomOrderChanged?.invoke(isChecked)
        }

        // Touch zones switch
        binding.switchTouchZones.setOnCheckedChangeListener { _, isChecked ->
            onTouchZonesEnabledChanged?.invoke(isChecked)
            binding.switchShowZoneLabels.isEnabled = isChecked
        }

        // Show zone labels switch
        binding.switchShowZoneLabels.setOnCheckedChangeListener { _, isChecked ->
            onShowZoneLabelsChanged?.invoke(isChecked)
        }

        // Video speed slider
        binding.sliderSpeed.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                binding.tvSpeedValue.text = "${value}x"
                onVideoSpeedChanged?.invoke(value)
            }
        }

        // Repeat mode toggle
        binding.toggleRepeatMode.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val mode = when (checkedId) {
                    R.id.btnRepeatOff -> RepeatMode.OFF
                    R.id.btnRepeatOne -> RepeatMode.ONE
                    R.id.btnRepeatAll -> RepeatMode.ALL
                    else -> RepeatMode.OFF
                }
                onRepeatModeChanged?.invoke(mode)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
