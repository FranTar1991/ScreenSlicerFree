package com.example.android.partialscreenshot.utils

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.MediaStore
import android.util.Log
import android.view.WindowManager.LayoutParams.*
import com.example.android.partialscreenshot.main_fragment.MainFragmentViewModel
import java.io.*
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
const val STOP_INTENT = "com.partialscreenshot.stop"
const val PERMISSION_TO_OVERLAY ="overlay"
const val PERMISSION_TO_SAVE ="save"

const val flags = FLAG_NOT_FOCUSABLE or FLAG_LAYOUT_IN_SCREEN
var INITIAL_POINT = 120

val Float.dp: Int
    get() = (this * Resources.getSystem().displayMetrics.density + 0.5f).toInt()

val Int.dp: Int
    get() = (this * Resources.getSystem().displayMetrics.density + 0.5f).toInt()

//the function I already explained, it is used to save the Bitmap to external storage
 fun saveMediaToStorage(contentResolver: ContentResolver, source: Bitmap?,
                        title: String, context: Context): Uri? {

    var fos: OutputStream? = null
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        contentResolver.also { resolver ->
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.TITLE, title)
                put(MediaStore.Images.Media.DISPLAY_NAME, title)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                // Add the date meta data to ensure the image is added at the front of the gallery
                put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis())
                put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
            }
            val imageUri: Uri? =
                resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            fos = imageUri?.let { resolver.openOutputStream(it) }
        }
    } else {
        val imagesDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val image = File(imagesDir, title)
        fos = FileOutputStream(image)
    }
    fos?.use {
        source?.compress(Bitmap.CompressFormat.JPEG, 100, it)
    }

    return Uri.fromFile(context.getFileStreamPath(title))
}
 fun saveImageToPhotoGallery(cr: ContentResolver, source: Bitmap,
                             title: String?): Uri? {
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.TITLE, title)
        put(MediaStore.Images.Media.DISPLAY_NAME, title)
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        // Add the date meta data to ensure the image is added at the front of the gallery
        put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis())
        put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
    }

    var url: Uri? = null

    try {
        val allImages = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        url = cr.insert(allImages, values)

        Log.i("MyUri","$url")
        var imageOut: OutputStream? = null
        try {
            imageOut = url?.let {
                cr.openOutputStream(it)
            }
            source.compress(Bitmap.CompressFormat.JPEG, 100, imageOut)
        }
        finally {
            imageOut?.close()
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Log.i("MyUri","calling thumbnail")
            val id = ContentUris.parseId(url!!)
            val thumbBitmap: Bitmap? = MediaStore.Images.Thumbnails.getThumbnail(cr, id, MediaStore.Images.Thumbnails.MINI_KIND, null)
            //This is for backward compatibility.
            thumbBitmap?.let {
                storeThumbnail(
                    cr, it, id, 50f, 50f,
                    MediaStore.Images.Thumbnails.MICRO_KIND
                )
            }
        }

    } catch (e: java.lang.Exception) {
        Log.i("MyUri","${e.message}")
        if (url != null) {
            cr.delete(url, null, null)
            url = null
        }
    }

    return url
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

 fun shakeItBaby(context: Context) {
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator?

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator?.vibrate(VibrationEffect.createOneShot(125, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        vibrator?.vibrate(125)
    }
}

 fun deleteTheList(listToDelete: List<String>, mainFragmentViewModel: MainFragmentViewModel) {

    mainFragmentViewModel.onDeleteListWithUri(listToDelete).also {
        for(fileUri in listToDelete){
            deleteFile(Uri.parse(fileUri))
        }
    }

}

 fun deleteFile(uriToDelete: Uri){

    val file = File(uriToDelete.path)
    if (file.exists()) {
        if (file.delete()) {
            Log.i("MyFile","file Deleted :" + uriToDelete.path)
        } else {
            Log.i("MyFile","file not Deleted :" + uriToDelete.path)
        }
    }
}
