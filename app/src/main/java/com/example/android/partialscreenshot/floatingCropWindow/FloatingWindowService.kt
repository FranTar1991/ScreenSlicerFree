package com.example.android.partialscreenshot.floatingCropWindow

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Resources
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
import android.os.*
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import com.example.android.partialscreenshot.R
import com.example.android.partialscreenshot.floatingCropWindow.cropWindow.CropView
import com.example.android.partialscreenshot.floatingCropWindow.cropWindow.OnRequestTakeScreenShotListener
import com.example.android.partialscreenshot.floatingCropWindow.cropWindow.addMyCropView
import com.example.android.partialscreenshot.getCurrentTimeStamp
import com.example.android.partialscreenshot.utils.NotificationUtils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


class FloatingWindowService: Service() {

    private lateinit var floatingView: CropView
    private var mData: Intent? = null

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
    private var mDensity = 0
    private var mWidth = 0
    private var mHeight = 0
    private var mRotation: Int? = 0
    private var mOrientationChangeCallback: OrientationChangeCallback? = null

    /**
     * From here down we find the variables for the floating button
     */

    private var takeScreenShotServiceCallback: FloatingWindowListener? = null
    private val binder: IBinder = LocalBinder()
    /**Solution for handle layout flag because that devices whom Build version is
     * greater then Oreo that don't support WindowManager.LayoutParams.TYPE_PHONE
     * in that case we use WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY*/




    inner class LocalBinder : Binder() {
        // Return this instance of LocalService so clients can call public methods
        fun getService(): FloatingWindowService = this@FloatingWindowService
    }

    override fun onCreate() {
        super.onCreate()
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

    override fun onBind(intent: Intent?): IBinder? {
        return binder;
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        setUpFloatingWidget()
        mData = takeScreenShotServiceCallback?.getIntentFromMain()
        return START_NOT_STICKY
    }
    override fun onDestroy() {
        super.onDestroy()
        manager.removeView(floatingView)
    }


    private fun setUpFloatingWidget() {

        manager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        floatingView  = LayoutInflater.from(this).inflate(R.layout.crop_view, null) as CropView
        floatingView.setOnRequestTakeScreenShotListener(object: OnRequestTakeScreenShotListener {
            override fun onRequestScreenShot(rect: Rect) {

                getStartIntent(applicationContext, -1, mData)?.let {
                    imageCoordinatesRect = rect
                    setUpScreenCapture(it)
                }
            }

        })
        manager.addMyCropView(floatingView, ViewGroup.LayoutParams.WRAP_CONTENT,0,0)
    }


    private fun setUpScreenCapture(intent: Intent) {
        Log.i("Istoartcommand", "set up screen capture")
        when {
            isStartCommand(intent) -> {
                Log.i("Istoartcommand", "is start command launched")
                // create notification
                val notification = NotificationUtils.getNotification(applicationContext)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        notification.first,
                        notification.second,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                    )
                }

                // start projection
                val resultCode =
                    intent.getIntExtra(RESULT_CODE, Activity.RESULT_CANCELED)
                val data = intent.getParcelableExtra<Intent>(DATA)
                startProjection(resultCode, data!!)
            }
            isStopCommand(intent) -> {
                stopProjection()
                //stopSelf();
            }
            else -> {
                stopSelf()
                Log.i("Istoartcommand", "stopping seld")
            }
        }
    }


    fun setServiceCallBacks(floatingAndTakeScreenShotServiceCallback: FloatingWindowListener?){
        takeScreenShotServiceCallback = floatingAndTakeScreenShotServiceCallback
    }


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
    private fun startProjection(resultCode: Int, data: Intent) {
        val mpManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        if (mMediaProjection == null) {
            Log.i("Istoartcommand", "mediaProjection is null so let´s create one")
            mMediaProjection = mpManager.getMediaProjection(resultCode, data)

        }

        if (mMediaProjection != null) {
            Log.i("Istoartcommand", "we created one now it´s not null")
            // display metrics
            mDensity = Resources.getSystem().displayMetrics.densityDpi
            val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            mDisplay = windowManager.defaultDisplay

            // create virtual display depending on device width / height
            createVirtualDisplay()

            // register orientation change callback
            mOrientationChangeCallback = OrientationChangeCallback(this)
            if (mOrientationChangeCallback!!.canDetectOrientation()) {
                mOrientationChangeCallback!!.enable()
            }

            // register media projection stop callback
            mMediaProjection!!.registerCallback(MediaProjectionStopCallback(), mHandler)
        }
    }
    private fun isStopCommand(intent: Intent): Boolean {
        return intent.hasExtra(ACTION) && intent.getStringExtra(
            ACTION
        ) == STOP
    }
    private fun createDirectory() {
        // create store dir
        val externalFilesDir = getExternalFilesDir(null)
        if (externalFilesDir != null) {
            mStoreDir = externalFilesDir.absolutePath + "/screenshots/"
            val storeDirectory = File(mStoreDir)
            if (!storeDirectory.exists()) {
                val success = storeDirectory.mkdirs()
                if (!success) {
                    Log.e(TAG, "failed to create file storage directory.")
                    stopSelf()
                }
            }
        } else {
            Log.e(
                TAG,
                "failed to create file storage directory, getExternalFilesDir is null."
            )
            stopSelf()
        }
    }
    @SuppressLint("WrongConstant")
    private fun createVirtualDisplay() {


        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
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
    private fun getVirtualDisplayFlags(): Int {
        return DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY or DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC
    }
    private fun stopProjection() {
        Log.i("Istoartcommand","Stopping projection")
        mHandler?.post {
            mMediaProjection?.stop()
        }
    }

    inner class ImageAvailableListener : ImageReader.OnImageAvailableListener {
        override fun onImageAvailable(reader: ImageReader) {
            if (IMAGES_PRODUCED <= 8) {

                Log.i("Istoartcommand", "image ready to use")
                val fos: FileOutputStream? = null
                var bitmap: Bitmap? = null
                try {
                    mImageReader?.acquireLatestImage().let { image ->
                        if (image != null) {
                            val planes: Array<Image.Plane> = image.getPlanes()
                            val buffer = planes[0].buffer
                            val pixelStride = planes[0].pixelStride
                            val rowStride = planes[0].rowStride
                            val rowPadding: Int = rowStride - pixelStride * mWidth

                            // create bitmap
                            bitmap = Bitmap.createBitmap(
                                mWidth + rowPadding / pixelStride,
                                mHeight,
                                Bitmap.Config.ARGB_8888
                            ).apply {
                                this.copyPixelsFromBuffer(buffer)
                                //fos = FileOutputStream(mStoreDir + "/myscreen_" + getCurrentTimeStamp() + ".png")
                                //this.compress(Bitmap.CompressFormat.JPEG, 100, fos)

                                Log.i("ImageCropped","x: ${(height*0.8).toInt()} " +
                                        "and y: 0")

                                val croppedBitmap: Bitmap = Bitmap.createBitmap(
                                    this,
                                    imageCoordinatesRect.left,
                                    imageCoordinatesRect.top,
                                    imageCoordinatesRect.width(),
                                    imageCoordinatesRect.height()
                                ).apply {
                                    val fos2 = FileOutputStream(mStoreDir + "/myscreen_" + getCurrentTimeStamp() + ".png")
                                    this.compress(Bitmap.CompressFormat.JPEG, 100, fos2)
                                    Log.i("ImageCropped", "rect height: ${imageCoordinatesRect.height()}, width: ${imageCoordinatesRect.width()} ")
                                    Log.i("ImageCropped","height: $height and width: $width")
                                }
                            }




                            IMAGES_PRODUCED ++
                            Log.e(
                                TAG,
                                "captured image: " + IMAGES_PRODUCED
                            )
                        }
                    }

                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                } finally {
                    if (fos != null) {
                        try {
                            fos!!.close()
                        } catch (ioe: IOException) {
                            ioe.printStackTrace()
                        }
                    }
                    if (bitmap != null) {
                        bitmap!!.recycle()
                    }
                }
            } else {
                stopProjection()
            }
        }
    }

    inner class OrientationChangeCallback internal constructor(context: Context?) :
        OrientationEventListener(context) {
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

    companion object{

        lateinit var manager: WindowManager
    }

}


interface FloatingWindowListener  {
    fun getPermissionToRecordScreen()
    fun getIntentFromMain():Intent?
}

