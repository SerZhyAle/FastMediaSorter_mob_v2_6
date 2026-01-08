package com.sza.fastmediasorter.ui.dialog

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.RecyclerView
import com.sza.fastmediasorter.R
import com.sza.fastmediasorter.databinding.DialogGifEditorBinding

/**
 * Full-screen dialog for editing GIF animations.
 *
 * Features:
 * - Frame-by-frame preview with thumbnails
 * - Playback speed control (0.25x - 4x)
 * - Frame range selection (trim)
 * - Loop mode (forward, bounce, once)
 * - Frame navigation (previous/next/first/last)
 * - Play/pause control
 * - Save options
 */
class GifEditorDialog : DialogFragment() {

    companion object {
        const val TAG = "GifEditorDialog"
        private const val ARG_GIF_URI = "gif_uri"

        fun newInstance(gifUri: Uri): GifEditorDialog {
            return GifEditorDialog().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_GIF_URI, gifUri)
                }
            }
        }
    }

    enum class LoopMode {
        FORWARD,  // Normal loop
        BOUNCE,   // Forward then backward
        ONCE      // Play once then stop
    }

    private var _binding: DialogGifEditorBinding? = null
    private val binding get() = _binding!!

    var onSave: ((settings: GifSettings) -> Unit)? = null

    private var gifUri: Uri? = null
    private var frames: List<Bitmap> = emptyList()
    private var currentFrameIndex = 0
    private var isPlaying = false
    private var speed = 1.0f
    private var startFrame = 0
    private var endFrame = 0
    private var loopMode = LoopMode.FORWARD
    private var isBouncingBack = false

    private val handler = Handler(Looper.getMainLooper())
    private var playbackRunnable: Runnable? = null

    data class GifSettings(
        val speed: Float,
        val startFrame: Int,
        val endFrame: Int,
        val loopMode: LoopMode
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Theme_FastMediaSorter_FullScreenDialog)

        @Suppress("DEPRECATION")
        gifUri = arguments?.getParcelable(ARG_GIF_URI)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogGifEditorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupPlaybackControls()
        setupSpeedControl()
        setupFrameRangeControl()
        setupLoopModeControl()

        loadGif()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            if (hasChanges()) {
                showDiscardConfirmation()
            } else {
                dismiss()
            }
        }

        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_save -> {
                    saveGif()
                    true
                }
                R.id.action_reset -> {
                    resetSettings()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupPlaybackControls() {
        binding.btnFirstFrame.setOnClickListener {
            stopPlayback()
            currentFrameIndex = startFrame
            showFrame(currentFrameIndex)
        }

        binding.btnPreviousFrame.setOnClickListener {
            stopPlayback()
            if (currentFrameIndex > startFrame) {
                currentFrameIndex--
                showFrame(currentFrameIndex)
            }
        }

        binding.btnPlayPause.setOnClickListener {
            if (isPlaying) {
                stopPlayback()
            } else {
                startPlayback()
            }
        }

        binding.btnNextFrame.setOnClickListener {
            stopPlayback()
            if (currentFrameIndex < endFrame) {
                currentFrameIndex++
                showFrame(currentFrameIndex)
            }
        }

        binding.btnLastFrame.setOnClickListener {
            stopPlayback()
            currentFrameIndex = endFrame
            showFrame(currentFrameIndex)
        }
    }

    private fun setupSpeedControl() {
        binding.sliderSpeed.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                speed = value
                binding.tvSpeedValue.text = "${value}x"

                // Update playback if playing
                if (isPlaying) {
                    stopPlayback()
                    startPlayback()
                }
            }
        }
    }

    private fun setupFrameRangeControl() {
        binding.sliderFrameRange.addOnChangeListener { slider, _, fromUser ->
            if (fromUser) {
                val values = slider.values
                startFrame = values[0].toInt()
                endFrame = values[1].toInt()

                binding.tvStartFrame.text = getString(R.string.start_frame_format, startFrame + 1)
                binding.tvEndFrame.text = getString(R.string.end_frame_format, endFrame + 1)

                // Clamp current frame to range
                if (currentFrameIndex < startFrame) {
                    currentFrameIndex = startFrame
                    showFrame(currentFrameIndex)
                } else if (currentFrameIndex > endFrame) {
                    currentFrameIndex = endFrame
                    showFrame(currentFrameIndex)
                }
            }
        }
    }

    private fun setupLoopModeControl() {
        binding.toggleLoopMode.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                loopMode = when (checkedId) {
                    R.id.btnLoopForward -> LoopMode.FORWARD
                    R.id.btnLoopBounce -> LoopMode.BOUNCE
                    R.id.btnLoopOnce -> LoopMode.ONCE
                    else -> LoopMode.FORWARD
                }
            }
        }

        // Default to forward loop
        binding.btnLoopForward.isChecked = true
    }

    private fun loadGif() {
        binding.progressBar.visibility = View.VISIBLE

        // TODO: Load GIF frames using a GIF decoder library
        // For now, simulate loading
        handler.postDelayed({
            binding.progressBar.visibility = View.GONE

            // Simulate 24 frames
            val frameCount = 24
            endFrame = frameCount - 1

            // Setup frame range slider
            binding.sliderFrameRange.valueFrom = 0f
            binding.sliderFrameRange.valueTo = (frameCount - 1).toFloat()
            binding.sliderFrameRange.values = listOf(0f, (frameCount - 1).toFloat())

            binding.tvStartFrame.text = getString(R.string.start_frame_format, 1)
            binding.tvEndFrame.text = getString(R.string.end_frame_format, frameCount)

            updateFrameIndicator()

            // Setup frame adapter
            binding.rvFrames.adapter = FrameAdapter(frameCount) { index ->
                stopPlayback()
                currentFrameIndex = index
                showFrame(currentFrameIndex)
            }
        }, 500)
    }

    private fun showFrame(index: Int) {
        if (index >= 0 && index < frames.size) {
            binding.ivGifPreview.setImageBitmap(frames[index])
        }
        updateFrameIndicator()

        // Highlight current frame in timeline
        (binding.rvFrames.adapter as? FrameAdapter)?.setSelectedFrame(index)
    }

    private fun updateFrameIndicator() {
        val totalFrames = if (frames.isEmpty()) endFrame + 1 else frames.size
        binding.tvFrameIndicator.text = getString(
            R.string.frame_indicator_format,
            currentFrameIndex + 1,
            totalFrames
        )
    }

    private fun startPlayback() {
        isPlaying = true
        isBouncingBack = false
        binding.btnPlayPause.setIconResource(R.drawable.ic_pause)

        val frameDelay = (100 / speed).toLong() // Base delay 100ms at 1x speed

        playbackRunnable = object : Runnable {
            override fun run() {
                if (!isPlaying) return

                when (loopMode) {
                    LoopMode.FORWARD -> {
                        currentFrameIndex++
                        if (currentFrameIndex > endFrame) {
                            currentFrameIndex = startFrame
                        }
                    }
                    LoopMode.BOUNCE -> {
                        if (isBouncingBack) {
                            currentFrameIndex--
                            if (currentFrameIndex <= startFrame) {
                                isBouncingBack = false
                            }
                        } else {
                            currentFrameIndex++
                            if (currentFrameIndex >= endFrame) {
                                isBouncingBack = true
                            }
                        }
                    }
                    LoopMode.ONCE -> {
                        currentFrameIndex++
                        if (currentFrameIndex > endFrame) {
                            stopPlayback()
                            currentFrameIndex = endFrame
                            return
                        }
                    }
                }

                showFrame(currentFrameIndex)
                handler.postDelayed(this, frameDelay)
            }
        }

        handler.post(playbackRunnable!!)
    }

    private fun stopPlayback() {
        isPlaying = false
        binding.btnPlayPause.setIconResource(R.drawable.ic_play)
        playbackRunnable?.let { handler.removeCallbacks(it) }
    }

    private fun resetSettings() {
        speed = 1.0f
        startFrame = 0
        endFrame = frames.size - 1
        loopMode = LoopMode.FORWARD
        currentFrameIndex = 0

        binding.sliderSpeed.value = 1f
        binding.tvSpeedValue.text = "1.0x"
        binding.sliderFrameRange.values = listOf(0f, (frames.size - 1).toFloat())
        binding.btnLoopForward.isChecked = true

        showFrame(0)
    }

    private fun hasChanges(): Boolean {
        return speed != 1.0f ||
                startFrame != 0 ||
                endFrame != frames.size - 1 ||
                loopMode != LoopMode.FORWARD
    }

    private fun saveGif() {
        onSave?.invoke(GifSettings(
            speed = speed,
            startFrame = startFrame,
            endFrame = endFrame,
            loopMode = loopMode
        ))
        dismiss()
    }

    private fun showDiscardConfirmation() {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.discard_changes)
            .setMessage(R.string.discard_changes_message)
            .setPositiveButton(R.string.discard) { _, _ ->
                dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopPlayback()
        _binding = null
    }

    /**
     * Adapter for frame thumbnails
     */
    private inner class FrameAdapter(
        private val frameCount: Int,
        private val onFrameClick: (Int) -> Unit
    ) : RecyclerView.Adapter<FrameAdapter.FrameViewHolder>() {

        private var selectedFrame = 0

        fun setSelectedFrame(index: Int) {
            val oldSelected = selectedFrame
            selectedFrame = index
            notifyItemChanged(oldSelected)
            notifyItemChanged(selectedFrame)
        }

        inner class FrameViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val imageView: ImageView = itemView as ImageView

            fun bind(index: Int) {
                // TODO: Load frame thumbnail
                imageView.setBackgroundColor(
                    if (index == selectedFrame) {
                        requireContext().getColor(R.color.md_theme_primary)
                    } else {
                        requireContext().getColor(R.color.md_theme_surfaceVariant)
                    }
                )

                imageView.setOnClickListener {
                    onFrameClick(index)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FrameViewHolder {
            val size = (60 * parent.resources.displayMetrics.density).toInt()
            val margin = (2 * parent.resources.displayMetrics.density).toInt()
            val imageView = ImageView(parent.context).apply {
                layoutParams = ViewGroup.MarginLayoutParams(size, size).apply {
                    setMargins(margin, margin, margin, margin)
                }
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
            return FrameViewHolder(imageView)
        }

        override fun onBindViewHolder(holder: FrameViewHolder, position: Int) {
            holder.bind(position)
        }

        override fun getItemCount() = frameCount
    }
}
