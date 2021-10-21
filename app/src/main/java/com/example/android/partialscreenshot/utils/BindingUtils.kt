package com.example.android.partialscreenshot.utils

import android.net.Uri
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.example.android.partialscreenshot.database.ScreenshotItem

@BindingAdapter("screenshotUri")
fun ImageView.setScreenshotUri(item: ScreenshotItem?) {
    item?.let {
        setImageURI(Uri.parse(item.screenshotURI))
    }
}