package com.sza.fastmediasorter.ui.welcome

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.sza.fastmediasorter.R
import com.sza.fastmediasorter.databinding.ItemWelcomePageBinding

class WelcomePagerAdapter : RecyclerView.Adapter<WelcomePagerAdapter.WelcomeViewHolder>() {

    private val pages = listOf(
        WelcomePage(
            iconRes = R.mipmap.ic_launcher,
            titleRes = R.string.welcome_title_1,
            descriptionRes = R.string.welcome_description_1
        ),
        WelcomePage(
            iconRes = R.drawable.resource_types,
            titleRes = R.string.welcome_title_2,
            descriptionRes = R.string.welcome_description_2
        ),
        WelcomePage(
            iconRes = R.mipmap.ic_launcher, // Reuse icon for now
            titleRes = R.string.welcome_title_3,
            descriptionRes = R.string.welcome_description_3
        ),
        WelcomePage(
            iconRes = R.drawable.destinations,
            titleRes = R.string.welcome_title_4,
            descriptionRes = R.string.welcome_description_4
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
        }
    }

    data class WelcomePage(
        val iconRes: Int,
        val titleRes: Int,
        val descriptionRes: Int
    )
}
