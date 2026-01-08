package com.sza.fastmediasorter.ui.dialog

import android.graphics.Bitmap
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.sza.fastmediasorter.R
import com.sza.fastmediasorter.databinding.DialogImageEditBinding

/**
 * Full-screen dialog for image editing.
 *
 * Features:
 * - Rotate (90° left/right + fine rotation slider)
 * - Flip (horizontal/vertical)
 * - Crop (with aspect ratio presets)
 * - Filters (preset filters with preview thumbnails)
 * - Adjustments (brightness, contrast, saturation)
 * - Real-time preview
 * - Undo/reset functionality
 * - Save options (save copy or overwrite)
 */
class ImageEditDialog : DialogFragment() {

    companion object {
        const val TAG = "ImageEditDialog"
        private const val ARG_IMAGE_URI = "image_uri"

        fun newInstance(imageUri: Uri): ImageEditDialog {
            return ImageEditDialog().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_IMAGE_URI, imageUri)
                }
            }
        }
    }

    enum class EditTool {
        NONE, ROTATE, FLIP, CROP, FILTER, ADJUST
    }

    enum class AspectRatio(val widthRatio: Float, val heightRatio: Float) {
        FREE(0f, 0f),
        SQUARE(1f, 1f),
        RATIO_4_3(4f, 3f),
        RATIO_16_9(16f, 9f),
        ORIGINAL(-1f, -1f)
    }

    private var _binding: DialogImageEditBinding? = null
    private val binding get() = _binding!!

    var onSave: ((editedBitmap: Bitmap, saveAsCopy: Boolean) -> Unit)? = null

    private var imageUri: Uri? = null
    private var currentTool: EditTool = EditTool.NONE

    // Edit state
    private var rotationAngle = 0f
    private var fineRotation = 0f
    private var isFlippedHorizontal = false
    private var isFlippedVertical = false
    private var brightness = 0f
    private var contrast = 0f
    private var saturation = 0f
    private var selectedAspectRatio = AspectRatio.FREE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Theme_FastMediaSorter_FullScreenDialog)

        @Suppress("DEPRECATION")
        imageUri = arguments?.getParcelable(ARG_IMAGE_URI)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogImageEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupToolButtons()
        setupRotateOptions()
        setupFlipOptions()
        setupCropOptions()
        setupAdjustOptions()

        loadImage()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            showDiscardConfirmation()
        }

        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_save -> {
                    saveImage(saveAsCopy = false)
                    true
                }
                R.id.action_save_copy -> {
                    saveImage(saveAsCopy = true)
                    true
                }
                R.id.action_reset -> {
                    resetAllEdits()
                    true
                }
                R.id.action_undo -> {
                    undoLastEdit()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupToolButtons() {
        binding.btnRotate.setOnClickListener { selectTool(EditTool.ROTATE) }
        binding.btnFlip.setOnClickListener { selectTool(EditTool.FLIP) }
        binding.btnCrop.setOnClickListener { selectTool(EditTool.CROP) }
        binding.btnFilter.setOnClickListener { selectTool(EditTool.FILTER) }
        binding.btnAdjust.setOnClickListener { selectTool(EditTool.ADJUST) }
    }

    private fun selectTool(tool: EditTool) {
        if (currentTool == tool) {
            // Toggle off
            currentTool = EditTool.NONE
            hideAllToolOptions()
        } else {
            currentTool = tool
            showToolOptions(tool)
        }
        updateToolButtonStates()
    }

    private fun showToolOptions(tool: EditTool) {
        binding.layoutToolOptions.visibility = View.VISIBLE
        hideAllToolOptions()

        when (tool) {
            EditTool.ROTATE -> binding.layoutRotateOptions.visibility = View.VISIBLE
            EditTool.FLIP -> binding.layoutFlipOptions.visibility = View.VISIBLE
            EditTool.CROP -> {
                binding.layoutCropOptions.visibility = View.VISIBLE
                binding.cropOverlay.visibility = View.VISIBLE
            }
            EditTool.FILTER -> binding.layoutFilterOptions.visibility = View.VISIBLE
            EditTool.ADJUST -> binding.layoutAdjustOptions.visibility = View.VISIBLE
            EditTool.NONE -> binding.layoutToolOptions.visibility = View.GONE
        }
    }

    private fun hideAllToolOptions() {
        binding.layoutRotateOptions.visibility = View.GONE
        binding.layoutFlipOptions.visibility = View.GONE
        binding.layoutCropOptions.visibility = View.GONE
        binding.layoutFilterOptions.visibility = View.GONE
        binding.layoutAdjustOptions.visibility = View.GONE
        binding.cropOverlay.visibility = View.GONE
    }

    private fun updateToolButtonStates() {
        // Highlight selected tool
        binding.btnRotate.isSelected = currentTool == EditTool.ROTATE
        binding.btnFlip.isSelected = currentTool == EditTool.FLIP
        binding.btnCrop.isSelected = currentTool == EditTool.CROP
        binding.btnFilter.isSelected = currentTool == EditTool.FILTER
        binding.btnAdjust.isSelected = currentTool == EditTool.ADJUST
    }

    private fun setupRotateOptions() {
        binding.btnRotateLeft.setOnClickListener {
            rotationAngle = (rotationAngle - 90f) % 360f
            updatePreview()
        }

        binding.btnRotateRight.setOnClickListener {
            rotationAngle = (rotationAngle + 90f) % 360f
            updatePreview()
        }

        binding.sliderRotation.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                fineRotation = value
                binding.tvRotationValue.text = "${value.toInt()}°"
                updatePreview()
            }
        }
    }

    private fun setupFlipOptions() {
        binding.btnFlipHorizontal.setOnClickListener {
            isFlippedHorizontal = !isFlippedHorizontal
            updatePreview()
        }

        binding.btnFlipVertical.setOnClickListener {
            isFlippedVertical = !isFlippedVertical
            updatePreview()
        }
    }

    private fun setupCropOptions() {
        binding.chipGroupAspectRatio.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                selectedAspectRatio = when (checkedIds.first()) {
                    R.id.chipFree -> AspectRatio.FREE
                    R.id.chip1_1 -> AspectRatio.SQUARE
                    R.id.chip4_3 -> AspectRatio.RATIO_4_3
                    R.id.chip16_9 -> AspectRatio.RATIO_16_9
                    R.id.chipOriginal -> AspectRatio.ORIGINAL
                    else -> AspectRatio.FREE
                }
                updateCropOverlay()
            }
        }
    }

    private fun setupAdjustOptions() {
        binding.sliderBrightness.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                brightness = value
                binding.tvBrightnessValue.text = value.toInt().toString()
                updatePreview()
            }
        }

        binding.sliderContrast.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                contrast = value
                binding.tvContrastValue.text = value.toInt().toString()
                updatePreview()
            }
        }

        binding.sliderSaturation.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                saturation = value
                binding.tvSaturationValue.text = value.toInt().toString()
                updatePreview()
            }
        }
    }

    private fun loadImage() {
        imageUri?.let { uri ->
            binding.progressBar.visibility = View.VISIBLE
            Glide.with(this)
                .load(uri)
                .into(binding.ivPreview)
            binding.progressBar.visibility = View.GONE
        }
    }

    private fun updatePreview() {
        // Apply transformations to preview
        binding.ivPreview.rotation = rotationAngle + fineRotation
        binding.ivPreview.scaleX = if (isFlippedHorizontal) -1f else 1f
        binding.ivPreview.scaleY = if (isFlippedVertical) -1f else 1f

        // Apply color adjustments
        applyColorAdjustments()
    }

    private fun applyColorAdjustments() {
        if (brightness == 0f && contrast == 0f && saturation == 0f) {
            binding.ivPreview.colorFilter = null
            return
        }

        val colorMatrix = ColorMatrix()

        // Brightness adjustment
        val brightnessMatrix = ColorMatrix()
        val brightValue = brightness / 100f
        brightnessMatrix.set(floatArrayOf(
            1f, 0f, 0f, 0f, brightValue * 255,
            0f, 1f, 0f, 0f, brightValue * 255,
            0f, 0f, 1f, 0f, brightValue * 255,
            0f, 0f, 0f, 1f, 0f
        ))
        colorMatrix.postConcat(brightnessMatrix)

        // Contrast adjustment
        val contrastValue = (contrast + 100) / 100f
        val contrastMatrix = ColorMatrix()
        val scale = contrastValue
        val translate = (-0.5f * scale + 0.5f) * 255
        contrastMatrix.set(floatArrayOf(
            scale, 0f, 0f, 0f, translate,
            0f, scale, 0f, 0f, translate,
            0f, 0f, scale, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        ))
        colorMatrix.postConcat(contrastMatrix)

        // Saturation adjustment
        val saturationMatrix = ColorMatrix()
        saturationMatrix.setSaturation((saturation + 100) / 100f)
        colorMatrix.postConcat(saturationMatrix)

        binding.ivPreview.colorFilter = ColorMatrixColorFilter(colorMatrix)
    }

    private fun updateCropOverlay() {
        // TODO: Update crop overlay based on selected aspect ratio
    }

    private fun resetAllEdits() {
        rotationAngle = 0f
        fineRotation = 0f
        isFlippedHorizontal = false
        isFlippedVertical = false
        brightness = 0f
        contrast = 0f
        saturation = 0f
        selectedAspectRatio = AspectRatio.FREE

        // Reset UI
        binding.sliderRotation.value = 0f
        binding.tvRotationValue.text = "0°"
        binding.sliderBrightness.value = 0f
        binding.sliderContrast.value = 0f
        binding.sliderSaturation.value = 0f
        binding.tvBrightnessValue.text = "0"
        binding.tvContrastValue.text = "0"
        binding.tvSaturationValue.text = "0"
        binding.chipFree.isChecked = true

        updatePreview()
    }

    private fun undoLastEdit() {
        // TODO: Implement undo stack
    }

    private fun saveImage(saveAsCopy: Boolean) {
        // TODO: Generate edited bitmap and invoke callback
        binding.progressBar.visibility = View.VISIBLE

        // For now, just dismiss
        dismiss()
    }

    private fun showDiscardConfirmation() {
        if (hasChanges()) {
            com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.discard_changes)
                .setMessage(R.string.discard_changes_message)
                .setPositiveButton(R.string.discard) { _, _ ->
                    dismiss()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        } else {
            dismiss()
        }
    }

    private fun hasChanges(): Boolean {
        return rotationAngle != 0f ||
                fineRotation != 0f ||
                isFlippedHorizontal ||
                isFlippedVertical ||
                brightness != 0f ||
                contrast != 0f ||
                saturation != 0f
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
