package com.screenslicerpro.detailsView.viewpager

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.screenslicerpro.database.ScreenshotItem
import com.screenslicerpro.databinding.FragmentDetailsBinding
import com.screenslicerpro.utils.ScreenshotListener


class ViewPagerDetailsAdapter(
    private val clickListener: ScreenshotListener) : ListAdapter<ScreenshotItem,
        ViewPagerDetailsAdapter.ViewHolder>(ScreenshotAdapterDiffCallback()) {


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(clickListener, item)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        return ViewHolder.from(parent)
    }




     class ViewHolder private constructor(private val binding: FragmentDetailsBinding)
        : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            clickListener: ScreenshotListener,
            item: ScreenshotItem
        ) {
            binding.clickListener = clickListener
            binding.screenshot = item
            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = FragmentDetailsBinding.inflate(layoutInflater, parent, false)

                return ViewHolder(binding)
            }
        }
    }
}

/**
 * Callback for calculating the diff between two non-null items in a list.
 *
 * Used by ListAdapter to calculate the minumum number of changes between and old list and a new
 * list that's been passed to `submitList`.
 */
class ScreenshotAdapterDiffCallback : DiffUtil.ItemCallback<ScreenshotItem>() {
    override fun areItemsTheSame(oldItem: ScreenshotItem, newItem: ScreenshotItem): Boolean {
        return oldItem.uri == newItem.uri
    }

    override fun areContentsTheSame(oldItem: ScreenshotItem, newItem: ScreenshotItem): Boolean {
        return oldItem == newItem
    }
}



