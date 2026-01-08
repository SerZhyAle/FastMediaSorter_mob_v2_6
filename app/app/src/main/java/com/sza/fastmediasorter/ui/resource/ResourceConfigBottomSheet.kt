package com.sza.fastmediasorter.ui.resource

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.sza.fastmediasorter.R
import com.sza.fastmediasorter.databinding.FragmentResourceConfigBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ResourceConfigBottomSheet : BottomSheetDialogFragment() {

    private var _binding: FragmentResourceConfigBinding? = null
    private val binding get() = _binding!!

    private var onConfirmListener: ((Boolean, Boolean, Boolean) -> Unit)? = null
    private var resourcePath: String = ""
    private var resourceName: String = ""

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
        
        binding.tvTitle.text = getString(R.string.configure_resource) // Or use name if desired
        binding.tvPath.text = path
        
        binding.btnSave.setOnClickListener {
            val isReadOnly = binding.switchReadOnly.isChecked
            val isDestination = binding.switchDestination.isChecked
            val scanAll = binding.switchScanAll.isChecked
            
            onConfirmListener?.invoke(isReadOnly, isDestination, scanAll)
            dismiss()
        }
    }

    fun setOnConfirmListener(listener: (Boolean, Boolean, Boolean) -> Unit) {
        this.onConfirmListener = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_NAME = "arg_name"
        private const val ARG_PATH = "arg_path"

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
}
