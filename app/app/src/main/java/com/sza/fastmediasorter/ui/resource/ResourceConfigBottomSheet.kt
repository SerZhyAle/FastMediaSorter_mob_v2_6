package com.sza.fastmediasorter.ui.resource

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.sza.fastmediasorter.R
import com.sza.fastmediasorter.databinding.FragmentResourceConfigBinding
import com.sza.fastmediasorter.ui.dialogs.PinCodeDialog
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ResourceConfigBottomSheet : BottomSheetDialogFragment() {

    private var _binding: FragmentResourceConfigBinding? = null
    private val binding get() = _binding!!

    private var onConfirmListener: ((Boolean, Boolean, Boolean, String?, Int) -> Unit)? = null
    private var resourcePath: String = ""
    private var resourceName: String = ""
    private var pinCode: String? = null

    companion object {
        private const val ARG_NAME = "arg_name"
        private const val ARG_PATH = "arg_path"

        // Media type bitmask constants
        const val MEDIA_TYPE_IMAGE = 1
        const val MEDIA_TYPE_VIDEO = 2
        const val MEDIA_TYPE_AUDIO = 4
        const val MEDIA_TYPE_GIF = 8
        const val MEDIA_TYPE_TEXT = 16
        const val MEDIA_TYPE_PDF = 32
        const val MEDIA_TYPE_EPUB = 64
        const val MEDIA_TYPE_ALL = 0

        fun newInstance(name: String, path: String): ResourceConfigBottomSheet {
            val fragment = ResourceConfigBottomSheet()
            val args = Bundle().apply {
                putString(ARG_NAME, name)
                putString(ARG_PATH, path)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResourceConfigBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val name = arguments?.getString(ARG_NAME) ?: ""
        val path = arguments?.getString(ARG_PATH) ?: ""
        
        binding.tvTitle.text = getString(R.string.configure_resource)
        binding.tvPath.text = path
        
        setupPinProtection()
        setupSaveButton()
    }

    private fun setupPinProtection() {
        binding.switchPinProtection.setOnCheckedChangeListener { _, isChecked ->
            binding.btnSetPin.visibility = if (isChecked) View.VISIBLE else View.GONE
            if (!isChecked) {
                pinCode = null
            }
        }

        binding.btnSetPin.setOnClickListener {
            showPinDialog()
        }
    }

    private fun showPinDialog() {
        val dialog = PinCodeDialog.newInstanceForSetting()
        dialog.setListener(object : PinCodeDialog.PinCodeListener {
            override fun onPinSet(pin: String?) {
                pinCode = pin
                updatePinButtonText()
            }

            override fun onPinVerified(success: Boolean) {
                // Not used in setting mode
            }
        })
        dialog.show(parentFragmentManager, "PinCodeDialog")
    }

    private fun updatePinButtonText() {
        binding.btnSetPin.text = if (pinCode != null) {
            getString(R.string.change_pin_code)
        } else {
            getString(R.string.set_pin_code)
        }
    }

    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener {
            val isReadOnly = binding.switchReadOnly.isChecked
            val isDestination = binding.switchDestination.isChecked
            val scanAll = binding.switchScanAll.isChecked
            
            val isPinProtected = binding.switchPinProtection.isChecked
            val finalPinCode = if (isPinProtected) pinCode else null
            
            val supportedMediaTypes = calculateMediaTypesBitmask()
            
            onConfirmListener?.invoke(isReadOnly, isDestination, scanAll, finalPinCode, supportedMediaTypes)
            dismiss()
        }
    }

    private fun calculateMediaTypesBitmask(): Int {
        var bitmask = 0
        
        if (binding.chipImage.isChecked) bitmask = bitmask or MEDIA_TYPE_IMAGE
        if (binding.chipVideo.isChecked) bitmask = bitmask or MEDIA_TYPE_VIDEO
        if (binding.chipAudio.isChecked) bitmask = bitmask or MEDIA_TYPE_AUDIO
        if (binding.chipGif.isChecked) bitmask = bitmask or MEDIA_TYPE_GIF
        if (binding.chipText.isChecked) bitmask = bitmask or MEDIA_TYPE_TEXT
        if (binding.chipPdf.isChecked) bitmask = bitmask or MEDIA_TYPE_PDF
        if (binding.chipEpub.isChecked) bitmask = bitmask or MEDIA_TYPE_EPUB
        
        // If no types selected, default to all types (0)
        return if (bitmask == 0) MEDIA_TYPE_ALL else bitmask
    }

    fun setOnConfirmListener(listener: (Boolean, Boolean, Boolean, String?, Int) -> Unit) {
        this.onConfirmListener = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null    }
}