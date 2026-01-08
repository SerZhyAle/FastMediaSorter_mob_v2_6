package com.sza.fastmediasorter.ui.dialog

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sza.fastmediasorter.R
import com.sza.fastmediasorter.databinding.DialogColorPickerBinding

/**
 * Color picker dialog with preset colors and custom RGB/Hex input.
 *
 * Features:
 * - Preset material colors
 * - RGB sliders
 * - Hex color input
 * - Real-time preview
 * - Color history (optional)
 */
class ColorPickerDialog : DialogFragment() {

    companion object {
        const val TAG = "ColorPickerDialog"
        private const val ARG_INITIAL_COLOR = "initial_color"
        private const val ARG_TITLE = "title"

        // Material Design preset colors
        val PRESET_COLORS = intArrayOf(
            0xFFF44336.toInt(), // Red
            0xFFE91E63.toInt(), // Pink
            0xFF9C27B0.toInt(), // Purple
            0xFF673AB7.toInt(), // Deep Purple
            0xFF3F51B5.toInt(), // Indigo
            0xFF2196F3.toInt(), // Blue
            0xFF03A9F4.toInt(), // Light Blue
            0xFF00BCD4.toInt(), // Cyan
            0xFF009688.toInt(), // Teal
            0xFF4CAF50.toInt(), // Green
            0xFF8BC34A.toInt(), // Light Green
            0xFFCDDC39.toInt(), // Lime
            0xFFFFEB3B.toInt(), // Yellow
            0xFFFFC107.toInt(), // Amber
            0xFFFF9800.toInt(), // Orange
            0xFFFF5722.toInt(), // Deep Orange
            0xFF795548.toInt(), // Brown
            0xFF9E9E9E.toInt(), // Grey
            0xFF607D8B.toInt(), // Blue Grey
            0xFF000000.toInt(), // Black
            0xFFFFFFFF.toInt(), // White
            0xFF212121.toInt(), // Dark Grey
            0xFFBDBDBD.toInt(), // Light Grey
            0xFFF5F5F5.toInt(), // Almost White
        )

        fun newInstance(
            initialColor: Int = Color.RED,
            title: String? = null
        ): ColorPickerDialog {
            return ColorPickerDialog().apply {
                arguments = Bundle().apply {
                    putInt(ARG_INITIAL_COLOR, initialColor)
                    putString(ARG_TITLE, title)
                }
            }
        }
    }

    private var _binding: DialogColorPickerBinding? = null
    private val binding get() = _binding!!

    var onColorSelected: ((Int) -> Unit)? = null

    private var currentColor: Int = Color.RED
    private var isUpdating = false

    override fun onCreateDialog(savedInstanceState: Bundle?): android.app.Dialog {
        _binding = DialogColorPickerBinding.inflate(LayoutInflater.from(requireContext()))

        val args = arguments ?: Bundle()
        currentColor = args.getInt(ARG_INITIAL_COLOR, Color.RED)
        val title = args.getString(ARG_TITLE) ?: getString(R.string.pick_color)

        setupViews()
        updateFromColor(currentColor)

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setView(binding.root)
            .setPositiveButton(R.string.select) { _, _ ->
                onColorSelected?.invoke(currentColor)
            }
            .setNegativeButton(R.string.cancel, null)
            .create()
    }

    private fun setupViews() {
        setupPresetColors()
        setupRgbSliders()
        setupHexInput()
    }

    private fun setupPresetColors() {
        binding.rvPresetColors.adapter = PresetColorAdapter(PRESET_COLORS) { color ->
            updateFromColor(color)
        }
    }

    private fun setupRgbSliders() {
        binding.sliderRed.addOnChangeListener { _, value, fromUser ->
            if (fromUser && !isUpdating) {
                updateColorFromRgb()
                binding.tvRedValue.text = value.toInt().toString()
            }
        }

        binding.sliderGreen.addOnChangeListener { _, value, fromUser ->
            if (fromUser && !isUpdating) {
                updateColorFromRgb()
                binding.tvGreenValue.text = value.toInt().toString()
            }
        }

        binding.sliderBlue.addOnChangeListener { _, value, fromUser ->
            if (fromUser && !isUpdating) {
                updateColorFromRgb()
                binding.tvBlueValue.text = value.toInt().toString()
            }
        }
    }

    private fun setupHexInput() {
        binding.etHexColor.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isUpdating) return
                val hexString = s?.toString() ?: return
                if (hexString.length == 6) {
                    try {
                        val color = Color.parseColor("#$hexString")
                        updateFromColor(color, updateHex = false)
                    } catch (e: IllegalArgumentException) {
                        // Invalid hex
                    }
                }
            }
        })
    }

    private fun updateFromColor(color: Int, updateHex: Boolean = true) {
        isUpdating = true
        currentColor = color

        // Update preview
        binding.viewColorPreview.setBackgroundColor(color)

        // Update RGB sliders
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)

        binding.sliderRed.value = r.toFloat()
        binding.sliderGreen.value = g.toFloat()
        binding.sliderBlue.value = b.toFloat()

        binding.tvRedValue.text = r.toString()
        binding.tvGreenValue.text = g.toString()
        binding.tvBlueValue.text = b.toString()

        // Update hex input
        if (updateHex) {
            val hexString = String.format("%02X%02X%02X", r, g, b)
            binding.etHexColor.setText(hexString)
        }

        isUpdating = false
    }

    private fun updateColorFromRgb() {
        val r = binding.sliderRed.value.toInt()
        val g = binding.sliderGreen.value.toInt()
        val b = binding.sliderBlue.value.toInt()
        val color = Color.rgb(r, g, b)
        updateFromColor(color)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Adapter for preset color grid
     */
    private inner class PresetColorAdapter(
        private val colors: IntArray,
        private val onColorClick: (Int) -> Unit
    ) : RecyclerView.Adapter<PresetColorAdapter.ColorViewHolder>() {

        inner class ColorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            fun bind(color: Int) {
                val drawable = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(color)
                    setStroke(2, if (color == Color.WHITE) Color.LTGRAY else Color.TRANSPARENT)
                }
                itemView.background = drawable
                itemView.setOnClickListener { onColorClick(color) }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
            val size = (36 * parent.resources.displayMetrics.density).toInt()
            val margin = (4 * parent.resources.displayMetrics.density).toInt()
            val view = View(parent.context).apply {
                layoutParams = ViewGroup.MarginLayoutParams(size, size).apply {
                    setMargins(margin, margin, margin, margin)
                }
            }
            return ColorViewHolder(view)
        }

        override fun onBindViewHolder(holder: ColorViewHolder, position: Int) {
            holder.bind(colors[position])
        }

        override fun getItemCount() = colors.size
    }
}
