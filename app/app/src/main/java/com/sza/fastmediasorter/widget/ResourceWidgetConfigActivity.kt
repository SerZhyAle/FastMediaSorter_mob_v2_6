package com.sza.fastmediasorter.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.sza.fastmediasorter.databinding.ActivityWidgetConfigBinding
import com.sza.fastmediasorter.domain.model.Resource
import com.sza.fastmediasorter.ui.base.BaseActivity
import com.sza.fastmediasorter.ui.main.MainViewModel
import com.sza.fastmediasorter.ui.main.ResourceAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Configuration activity for Resource Launch Widget.
 * Allows user to select which resource the widget should open.
 */
@AndroidEntryPoint
class ResourceWidgetConfigActivity : BaseActivity<ActivityWidgetConfigBinding>() {

    private val viewModel: MainViewModel by viewModels()
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    
    private lateinit var adapter: ResourceAdapter

    override fun getViewBinding() = ActivityWidgetConfigBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set result to CANCELED initially
        setResult(Activity.RESULT_CANCELED)

        // Extract widget ID from intent
        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setupToolbar()
        setupRecyclerView()
        observeResources()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        adapter = ResourceAdapter(
            onItemClick = { resource -> onResourceSelected(resource) },
            onItemLongClick = { _ -> false },
            onMoreClick = { _ -> /* No menu in widget config */ }
        )
        
        binding.resourceList.apply {
            layoutManager = LinearLayoutManager(this@ResourceWidgetConfigActivity)
            adapter = this@ResourceWidgetConfigActivity.adapter
        }
    }

    private fun observeResources() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    adapter.submitList(state.resources)
                    
                    if (state.resources.isEmpty() && !state.isLoading) {
                        binding.emptyText.visibility = View.VISIBLE
                        binding.resourceList.visibility = View.GONE
                    } else {
                        binding.emptyText.visibility = View.GONE
                        binding.resourceList.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private fun onResourceSelected(resource: Resource) {
        // Save the widget configuration
        ResourceLaunchWidget.saveWidgetConfig(
            context = this,
            appWidgetId = appWidgetId,
            resourceId = resource.id,
            resourceName = resource.name,
            resourceType = resource.type
        )

        // Update the widget
        val appWidgetManager = AppWidgetManager.getInstance(this)
        ResourceLaunchWidget.updateAppWidget(this, appWidgetManager, appWidgetId)

        // Return success
        val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(Activity.RESULT_OK, resultValue)
        finish()
    }
}
