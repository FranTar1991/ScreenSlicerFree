package com.example.android.partialscreenshot.main_fragment.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.ItemKeyProvider
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.android.partialscreenshot.database.ScreenshotItem

import com.example.android.partialscreenshot.databinding.ListItemPictureBinding

import android.content.Context

import android.graphics.drawable.Drawable
import com.example.android.partialscreenshot.R


class ScreenshotsAdapter (private val clickListener: ScreenshotListener) : ListAdapter<ScreenshotItem,
        ScreenshotsAdapter.ViewHolder>(ScreenshotAdapterDiffCallback()) {


    init {
        setHasStableIds(true)
    }
    override fun getItemId(position: Int): Long = position.toLong()

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
            holder.bind(clickListener,item)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent, this)
    }


     class ViewHolder private constructor(private val binding: ListItemPictureBinding, private val adapter: ScreenshotsAdapter)
        : RecyclerView.ViewHolder(binding.root) {

        fun bind(clickListener: ScreenshotListener, item: ScreenshotItem) {

            binding.screenshot = item
            binding.clickListener = clickListener
            binding.executePendingBindings()

        }

        companion object {
            fun from(parent: ViewGroup, screenshotsAdapter: ScreenshotsAdapter): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListItemPictureBinding.inflate(layoutInflater, parent, false)

                return ViewHolder(binding,screenshotsAdapter)
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
        return oldItem.screenshotID == newItem.screenshotID
    }

    override fun areContentsTheSame(oldItem: ScreenshotItem, newItem: ScreenshotItem): Boolean {
        return oldItem == newItem
    }
}

class ScreenshotListener(val clickListener: (sleepId: Long) -> Unit){
    fun onClick(screenshot: ScreenshotItem) = clickListener(screenshot.screenshotID)
}




