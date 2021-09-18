package com.example.android.partialscreenshot.utils

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.*
import java.io.FileNotFoundException
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

    fun getCurrentTimeStamp(): String? {
        // write bitmap to a file
        val dateFormat = SimpleDateFormat("yyyyMMdd_HH_mm_ss")
        return dateFormat.format(Date())
    }
val layoutFlag: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    TYPE_APPLICATION_OVERLAY
} else {
    TYPE_PHONE
}
const val flags = FLAG_NOT_FOCUSABLE or FLAG_LAYOUT_IN_SCREEN
var INITIAL_POINT = 120

val Float.dp: Int
    get() = (this * Resources.getSystem().displayMetrics.density + 0.5f).toInt()

val Int.dp: Int
    get() = (this * Resources.getSystem().displayMetrics.density + 0.5f).toInt()

 fun saveImageToPhotoGallery(cr: ContentResolver, source: Bitmap?, title: String?, description: String?): String? {
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.TITLE, title)
        put(MediaStore.Images.Media.DISPLAY_NAME, title)
        put(MediaStore.Images.Media.DESCRIPTION, description)
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        // Add the date meta data to ensure the image is added at the front of the gallery
        put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis())
        put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        with(values) {
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        }
    }

    var url: Uri? = null
    var stringUrl: String? = null /* value to be returned */
    try {
        val allImages =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(
                    MediaStore.VOLUME_EXTERNAL_PRIMARY
                )

            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }
        url = cr.insert(allImages, values)

        if (source != null) {

            var imageOut: OutputStream? = null

            if(url != null){
                imageOut = cr.openOutputStream(url)
            }


            try {
                source.compress(Bitmap.CompressFormat.JPEG, 100, imageOut)
            } finally {
                imageOut?.close()

            }
            val id = ContentUris.parseId(url!!)
            // Wait until MINI_KIND thumbnail is generated.

            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {

                val thumbBitmap: Bitmap? = MediaStore.Images.Thumbnails.getThumbnail(cr, id, MediaStore.Images.Thumbnails.MINI_KIND, null)
                //This is for backward compatibility.
                thumbBitmap?.let {
                    storeThumbnail(
                        cr, it, id, 50f, 50f,
                        MediaStore.Images.Thumbnails.MICRO_KIND
                    )
                }
            }

        }
        else {
            cr.delete(url!!, null, null)
            url = null
        }
    } catch (e: java.lang.Exception) {
        if (url != null) {
            cr.delete(url, null, null)
            url = null
        }
    }
    if (url != null) {
        stringUrl = url.toString()
    }
    return stringUrl
}
private fun storeThumbnail(cr: ContentResolver, source: Bitmap, id: Long, width: Float, height: Float, kind: Int): Bitmap? {

    // create the matrix to scale it
    val matrix = Matrix()
    val scaleX = width / source.width
    val scaleY = height / source.height
    matrix.setScale(scaleX, scaleY)
    val thumb = Bitmap.createBitmap(
        source, 0, 0,
        source.width,
        source.height, matrix,
        true
    )
    val values = ContentValues(4).apply {

        put(MediaStore.Images.Thumbnails.KIND, kind)
        put(MediaStore.Images.Thumbnails.IMAGE_ID, id.toInt())
        put(MediaStore.Images.Thumbnails.HEIGHT, thumb.height)
        put(MediaStore.Images.Thumbnails.WIDTH, thumb.width)
    }
    val url = cr.insert(MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI, values)

    return try {
        val thumbOut: OutputStream? = cr.openOutputStream(url!!)
        thumb.compress(Bitmap.CompressFormat.JPEG, 100, thumbOut)
        thumbOut?.close()
        thumb
    } catch (ex: FileNotFoundException) {
        null
    } catch (ex: IOException) {
        null
    }
}
