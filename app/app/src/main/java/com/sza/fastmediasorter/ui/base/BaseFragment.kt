package com.sza.fastmediasorter.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding

/**
 * Base Fragment with ViewBinding support.
 * All Fragments should extend this class for consistent lifecycle management.
 *
 * @param VB The ViewBinding type for the Fragment
 */
abstract class BaseFragment<VB : ViewBinding> : Fragment() {

    private var _binding: VB? = null

    /**
     * Access the ViewBinding. Throws if accessed outside the view lifecycle.
     */
    protected val binding: VB
        get() = _binding ?: throw IllegalStateException(
            "ViewBinding accessed outside view lifecycle (before onCreateView or after onDestroyView)"
        )

    /**
     * Override to provide the ViewBinding instance.
     * Typically: FragmentFooBinding.inflate(inflater, container, false)
     */
    abstract fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): VB

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = getViewBinding(inflater, container)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
