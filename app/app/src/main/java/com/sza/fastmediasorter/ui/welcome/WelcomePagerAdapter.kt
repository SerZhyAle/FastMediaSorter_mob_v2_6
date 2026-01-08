package com.sza.fastmediasorter.ui.welcome

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.sza.fastmediasorter.BuildConfig
import com.sza.fastmediasorter.R
import com.sza.fastmediasorter.databinding.ItemWelcomePageBinding

class WelcomePagerAdapter : RecyclerView.Adapter<WelcomePagerAdapter.WelcomeViewHolder>() {

    private val pages = listOf(
        WelcomePage(
            iconRes = R.mipmap.ic_launcher,
            titleRes = R.string.welcome_title_1,
            descriptionRes = R.string.welcome_description_1,
            showVersion = true  // Show version on first page
        ),
        WelcomePage(
            iconRes = R.drawable.resource_types,
            titleRes = R.string.welcome_title_2,
            descriptionRes = R.string.welcome_description_2,
            showVersion = false
        ),
        WelcomePage(
            iconRes = R.drawable.ic_touch_zones_guide,
            titleRes = R.string.welcome_title_3_touch_zones,
            descriptionRes = R.string.welcome_description_3_touch_zones,
            showVersion = false
        ),
        WelcomePage(
            iconRes = R.mipmap.ic_launcher,
            titleRes = R.string.welcome_title_3,
            descriptionRes = R.string.welcome_description_3,
            showVersion = false
        ),
        WelcomePage(
            iconRes = R.drawable.destinations,
            titleRes = R.string.welcome_title_4,
            descriptionRes = R.string.welcome_description_4,
            showVersion = false
        )
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WelcomeViewHolder {
        val binding = ItemWelcomePageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return WelcomeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WelcomeViewHolder, position: Int) {
        holder.bind(pages[position])
    }

    override fun getItemCount(): Int = pages.size

    inner class WelcomeViewHolder(private val binding: ItemWelcomePageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(page: WelcomePage) {
            binding.ivIcon.setImageResource(page.iconRes)
            binding.tvTitle.setText(page.titleRes)
            binding.tvDescription.setText(page.descriptionRes)
            
            // Show version info if requested
            if (page.showVersion) {
                binding.tvVersion.visibility = View.VISIBLE
                val versionText = "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
                binding.tvVersion.text = versionText
            } else {
                binding.tvVersion.visibility = View.GONE
            }
        }
    }

    data class WelcomePage(
        val iconRes: Int,
        val titleRes: Int,
        val descriptionRes: Int,
        val showVersion: Boolean = false
    )
}
