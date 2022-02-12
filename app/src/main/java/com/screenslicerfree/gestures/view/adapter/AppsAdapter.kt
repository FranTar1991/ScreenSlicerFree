package com.screenslicerfree.gestures.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.screenslicerfree.gestures.action.database.AppItem
import com.screenslicerfree.gestures.view.viewmodel.GestureSettingsViewModel
import com.screenslicerfree.main_fragment.MainFragmentViewModel
import com.screenslicerfree.main_fragment.adapter.ScreenshotsAdapter

import com.screenslicerfree.utils.AppClickListener
import com.screenslicerfree.databinding.AppItemBinding

class AppsAdapter(private val viewModel: GestureSettingsViewModel,
    private val clickListener: AppClickListener) : ListAdapter<AppItem,
        AppsAdapter.ViewHolder>(AppAdapterDiffCallback()) {


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(clickListener, viewModel, item)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent)
    }


    class ViewHolder private constructor(private val binding: AppItemBinding)
        : RecyclerView.ViewHolder(binding.root) {


        fun bind(
            clickListener: AppClickListener,
            viewModel: GestureSettingsViewModel,
            item: AppItem) {
            binding.app = item
            binding.clickListener = clickListener
            binding.viewModel = viewModel
            binding.executePendingBindings()

        }

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = AppItemBinding.inflate(layoutInflater, parent, false)

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
class AppAdapterDiffCallback : DiffUtil.ItemCallback<AppItem>() {
    override fun areItemsTheSame(oldItem: AppItem, newItem: AppItem): Boolean {
        return oldItem.appId == newItem.appId
    }

    override fun areContentsTheSame(oldItem: AppItem, newItem: AppItem): Boolean {
        return oldItem == newItem
    }
}