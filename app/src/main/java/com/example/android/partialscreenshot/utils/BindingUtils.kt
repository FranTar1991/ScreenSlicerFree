package com.example.android.partialscreenshot.utils

import android.graphics.Color
import android.net.Uri
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.example.android.partialscreenshot.R
import com.example.android.partialscreenshot.database.ScreenshotItem

@BindingAdapter("screenshotUri")
fun ImageView.setScreenshotUri(item: ScreenshotItem?) {
    item?.let {
        setImageURI(Uri.parse(item.storeUri))
        if(drawable == null) {
            setBackgroundColor(Color.rgb(0, 0, 0));
            setImageResource(R.drawable.ic_baseline_broken_image_24)
        }
    }
}