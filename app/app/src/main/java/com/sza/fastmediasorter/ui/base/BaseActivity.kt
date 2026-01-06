package com.sza.fastmediasorter.ui.base

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding

/**
 * Base Activity with ViewBinding support.
 * All Activities should extend this class for consistent lifecycle management.
 *
 * @param VB The ViewBinding type for the Activity
 */
abstract class BaseActivity<VB : ViewBinding> : AppCompatActivity() {

    private var _binding: VB? = null
    
    /**
     * Access the ViewBinding. Throws if accessed before onCreate or after onDestroy.
     */
    protected val binding: VB
        get() = _binding ?: throw IllegalStateException(
            "ViewBinding accessed before onCreate or after onDestroy"
        )

    /**
     * Override to provide the ViewBinding instance.
     * Typically: ActivityFooBinding.inflate(layoutInflater)
     */
    abstract fun getViewBinding(): VB

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = getViewBinding()
        setContentView(binding.root)
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
