package com.example.android.partialscreenshot.floatingCropWindow.cropWindow

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.Display
import android.view.OrientationEventListener
import android.view.WindowManager
import com.example.android.partialscreenshot.floatingCropWindow.FloatingWindowService
import com.example.android.partialscreenshot.getCurrentTimeStamp
import com.example.android.partialscreenshot.utils.NotificationUtils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class ScreenShotTaker(private val context: Context, private val floatingWindowService: FloatingWindowService) {
    /**
     * Variables for screen capture
     */

    private lateinit var imageCoordinatesRect: Rect
    private val TAG: String? = "ScreenCaptureService"
    private val RESULT_CODE = "RESULT_CODE"
    private val DATA = "DATA"
    private val ACTION = "ACTION"
    private val START = "START"
    private val STOP = "STOP"
    private val SCREENCAP_NAME = "screencap"
    private var IMAGES_PRODUCED = 0
    private var mMediaProjection: MediaProjection? = null
    private var mStoreDir: String = ""
    private var mImageReader: ImageReader? = null
    private var mHandler: Handler? = null
    private var mDisplay: Display? = null
    private var mVirtualDisplay: VirtualDisplay? = null
    private var mWidth = 0
    private var mHeight = 0
    private var mRotation: Int? = 0
    private lateinit var mOrientationChangeCallback: OrientationChangeCallback

    init {
        createDirectory()

        // start capture handling thread
        object : Thread() {
            override fun run() {
                Looper.prepare()
                mHandler = Handler()
                Looper.loop()
            }
        }.start()
    }

    /**
     * if @param intent has the Data info, sets the notification and then calls
     * startProjection() else checks if this was called to stop the projection, if none of that
     * the service is stopped
     */
    fun setUpScreenCapture(intent: Intent, imageCoordinatesRect: Rect) {
       this.imageCoordinatesRect = imageCoordinatesRect
        when {
            isStartCommand(intent) -> {
                // create notification
                val notification = NotificationUtils.getNotification(context)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    floatingWindowService.startForeground(
                        notification.first,
                        notification.second,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                    )
                }
                // start projection
                val resultCode = intent.getIntExtra(RESULT_CODE, Activity.RESULT_CANCELED)
                val data = intent.getParcelableExtra<Intent>(DATA)
                startProjection(resultCode, data!!)
            }
            isStopCommand(intent) -> {
                stopProjection()
                floatingWindowService.stopSelf()
            }
            else -> {
                floatingWindowService.stopSelf()
            }
        }
    }

    /**
     * If media projections is null, create a new one, then set up the rest to start the projection
     */
    private fun startProjection(resultCode: Int, data: Intent) {
        val mpManager = context.getSystemService(Service.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        if (mMediaProjection == null) {
            mMediaProjection = mpManager.getMediaProjection(resultCode, data)
        }

            // create virtual display depending on device width / height
            createVirtualDisplay()

            // register orientation change callback
            mOrientationChangeCallback = OrientationChangeCallback(context)

            if (mOrientationChangeCallback.canDetectOrientation()) {
                mOrientationChangeCallback.enable()
            }

            // register media projection stop callback
            mMediaProjection?.registerCallback(MediaProjectionStopCallback(), mHandler)

    }

    /**
     * Method called to stop taking the screenshots
     */
    private fun stopProjection() {
        Log.i("Istoartcommand","Stopping projection")
        mHandler?.post {
            mMediaProjection?.stop()
        }
    }

    /**
     * Create the directory where the cropped image will be saved when the user says so
     */
    private fun createDirectory() {
        // create store dir
        val externalFilesDir = context.getExternalFilesDir(null)
        if (externalFilesDir != null) {
            mStoreDir = externalFilesDir.absolutePath + "/screenshots/"
            val storeDirectory = File(mStoreDir)
            if (!storeDirectory.exists()) {
                val success = storeDirectory.mkdirs()
                if (!success) {
                    Log.e(TAG, "failed to create file storage directory.")
                    floatingWindowService.stopSelf()
                }
            }
        } else {
            Log.e(
                TAG,
                "failed to create file storage directory, getExternalFilesDir is null."
            )
            floatingWindowService.stopSelf()
        }
    }

    /**
     * The screen dimensions to be used to take the screenshot
     */
    @SuppressLint("WrongConstant")
    private fun createVirtualDisplay() {

        val wm = context.getSystemService(Service.WINDOW_SERVICE) as WindowManager
        val display = wm.defaultDisplay
        val metrics = DisplayMetrics()
        display.getMetrics(metrics)
        val size = Point()
        display.getRealSize(size)
        mWidth = size.x
        mHeight = size.y
        val mDensity = metrics.densityDpi

        // start capture reader
        mImageReader = ImageReader
            .newInstance(mWidth, mHeight,
                PixelFormat.RGBA_8888, 2).apply {
                setOnImageAvailableListener(ImageAvailableListener(), mHandler)
            }
        mVirtualDisplay = mMediaProjection!!.createVirtualDisplay(
            SCREENCAP_NAME,
            mWidth,
            mHeight,
            mDensity,
            getVirtualDisplayFlags(),
            mImageReader!!.getSurface(),
            null,
            mHandler
        )

    }


    /**
     * Starts section of helper methods to set up the Media projection
     */
    fun getStartIntent(context: Context?, resultCode: Int, data: Intent?): Intent? {
        val intent = Intent(context, FloatingWindowService::class.java)
        intent.putExtra(ACTION, START)
        intent.putExtra(RESULT_CODE, resultCode)
        intent.putExtra(DATA, data)
        return intent
    }
    fun getStopIntent(context: Context?): Intent? {
        val intent = Intent(context, FloatingWindowService::class.java)
        intent.putExtra(ACTION, STOP)
        return intent
    }

    private fun isStartCommand(intent: Intent): Boolean {
        IMAGES_PRODUCED = 0
        return (intent.hasExtra(RESULT_CODE)
                && intent.hasExtra(DATA)
                && intent.hasExtra(ACTION)
                && intent.getStringExtra(ACTION) == START)
    }
    private fun isStopCommand(intent: Intent): Boolean {
        return intent.hasExtra(ACTION) && intent.getStringExtra(
            ACTION
        ) == STOP
    }

    private fun getVirtualDisplayFlags(): Int {
        return DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY or DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC
    }

    /**
     * Ends section of helper methods
     */

    private fun setBitmap() {

        var bitmap: Bitmap? = null
        try {
            mImageReader?.acquireLatestImage().let { image ->
                if (image != null) {
                    val planes: Array<Image.Plane> = image.planes
                    val buffer = planes[0].buffer
                    val pixelStride = planes[0].pixelStride
                    val rowStride = planes[0].rowStride
                    val rowPadding: Int = rowStride - pixelStride * mWidth

                    // create bitmap
                    bitmap = Bitmap.createBitmap(mWidth + rowPadding / pixelStride, mHeight, Bitmap.Config.ARGB_8888
                    ).apply {
                        this.copyPixelsFromBuffer(buffer)
                       cropBaseBitmap(this)
                    }
                }
            }

        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        } finally {
            bitmap?.recycle()
        }
    }
    private fun cropBaseBitmap(bitmap: Bitmap) {
        val croppedBitmap: Bitmap = Bitmap.createBitmap(
            bitmap,
            imageCoordinatesRect.left,
            imageCoordinatesRect.top,
            imageCoordinatesRect.width(),
            imageCoordinatesRect.height()
        )
        saveBitmap(croppedBitmap)
    }
    private fun saveBitmap(bitmapToSave: Bitmap){
       val fos = FileOutputStream(mStoreDir + "/myscreen_" + getCurrentTimeStamp() + ".png")

       fos.use {
           bitmapToSave.compress(Bitmap.CompressFormat.JPEG, 100, it)
       }
    }

    inner class OrientationChangeCallback internal constructor(context: Context?) : OrientationEventListener(context) {
        override fun onOrientationChanged(orientation: Int) {
            val rotation: Int? = mDisplay?.rotation
            if (rotation != mRotation) {
                mRotation = rotation
                try {
                    // clean up
                    mVirtualDisplay?.release()
                    mImageReader?.setOnImageAvailableListener(null, null)

                    // re-create virtual display depending on device width / height
                    createVirtualDisplay()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    inner class MediaProjectionStopCallback : MediaProjection.Callback() {
        override fun onStop() {
            Log.e(TAG, "stopping projection.")
            mHandler?.post(Runnable {
                mVirtualDisplay?.release()
                mImageReader?.setOnImageAvailableListener(null, null)
                mOrientationChangeCallback?.disable()
                mMediaProjection?.unregisterCallback(this@MediaProjectionStopCallback)
                mMediaProjection = null
            })
        }
    }
    inner class ImageAvailableListener : ImageReader.OnImageAvailableListener {
        override fun onImageAvailable(reader: ImageReader) {
            if (IMAGES_PRODUCED <= 8) {

                setBitmap()

                IMAGES_PRODUCED++

            } else {
                stopProjection()
            }
        }
    }
}