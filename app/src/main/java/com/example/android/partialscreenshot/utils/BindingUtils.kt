package com.example.android.partialscreenshot.utils

import android.graphics.Color
import android.net.Uri
import android.util.Log
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.example.android.partialscreenshot.R
import com.example.android.partialscreenshot.database.ScreenshotItem
import java.io.InputStream
import java.lang.Exception

@BindingAdapter("screenshotUri")
fun ImageView.setScreenshotUri(item: ScreenshotItem?) {
    item?.let {

         val uriExist: Boolean = try {
             context.contentResolver.openInputStream(Uri.parse(item.uri))
             true
        }catch (e: Exception){
            Log.e("MyDeleteRequest", "Exception $e")
             false
        }

        if(uriExist){
            setImageURI(Uri.parse(item.uri))
        }

            if (drawable == null) {
                setBackgroundColor(Color.rgb(0, 0, 0))
                setImageResource(R.drawable.ic_baseline_broken_image_24)
            }
        }


}