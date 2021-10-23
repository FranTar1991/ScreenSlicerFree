package com.example.android.partialscreenshot.floatingCropWindow.cropWindow

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.*
import android.util.DisplayMetrics
import android.util.Log
import android.widget.Toast
import com.example.android.partialscreenshot.floatingCropWindow.CropViewFloatingWindowService
import com.example.android.partialscreenshot.floatingCropWindow.optionsWindow.OptionsWindowView

import android.graphics.*
import java.io.*
import android.os.Build

import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.view.*
import androidx.core.content.ContextCompat.startActivity
import com.example.android.partialscreenshot.MainActivity
import com.example.android.partialscreenshot.R
import com.example.android.partialscreenshot.utils.*


class ScreenShotTaker(
    private val context: Context,
    private val cropViewFloatingWindowService: CropViewFloatingWindowService,
    private val cropView: CropView?,
    private val mainActivityReference: FloatingWindowListener?
): OnOptionsWindowSelectedListener {

    private var isToEdit: Boolean = false
    private var mainActivity: MainActivity? = null
    private var uriToEdit: Uri? = null
    private lateinit var path: String
    private lateinit var name: String
    private var uriToImage: Uri? = null

    //This is the cropped bitmap that´s going to be saved when the user saves
    private lateinit var croppedBitmap: Bitmap
    lateinit var optionsWindowView: OptionsWindowView
    private lateinit var fileOutputStream: FileOutputStream

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

    private var isToShare: Boolean = false

    init {

        mainActivity = (mainActivityReference as MainActivity)
        createDirectory()

        // start capture handling thread
        object : Thread() {
            override fun run() {

                Looper.prepare()
                mHandler = Handler(Looper.getMainLooper())
                Looper.loop()
            }
        }.start()
    }

    /**
     * Method used to call the window where the user will see all the options they have
     * for the screenshot just taken. This method is called when the base bitmap has been
     * cropped
     */

    private fun callOptionsFloatingWindowService(){
        optionsWindowView = OptionsWindowView(context, cropView).apply {
            this.setOnOnOptionsWindowSelected(this@ScreenShotTaker)
        }
        optionsWindowView.createView()
    }

    /**
     * Start region of methods used to process the bitmap
     */

    private fun getBaseBitmap() {

        try {
            mImageReader?.acquireLatestImage().let { image ->
                if (image != null) {
                    val planes: Array<Image.Plane> = image.planes
                    val buffer = planes[0].buffer
                    val pixelStride = planes[0].pixelStride
                    val rowStride = planes[0].rowStride
                    val rowPadding: Int = rowStride - pixelStride * mWidth

                    // create bitmap
                    Bitmap.createBitmap(mWidth + rowPadding / pixelStride, mHeight, Bitmap.Config.ARGB_8888
                    ).let { baseBitmap ->

                        baseBitmap?.apply {
                            baseBitmap.copyPixelsFromBuffer(buffer)
                            croppedBitmap = cropBaseBitmap(baseBitmap).copy(baseBitmap.config,true)
                            cropViewFloatingWindowService.setCroppedImage(croppedBitmap)
                            callOptionsFloatingWindowService()
                            baseBitmap.recycle()

                        }

                    }
                }
            }

        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }
    private fun cropBaseBitmap(bitmap: Bitmap): Bitmap {

        return Bitmap.createBitmap(
            bitmap,
            imageCoordinatesRect.left,
            imageCoordinatesRect.top,
            imageCoordinatesRect.width(),
            imageCoordinatesRect.height()
        )

    }
    private fun saveCroppedBitmap(bitmapToSave: Bitmap?){

        name = "${getCurrentTimeStamp()}"
        path = "$mStoreDir/$name"
        fileOutputStream = FileOutputStream(path)
        uriToImage = Uri.parse(path)

        fileOutputStream.use {
           bitmapToSave?.compress(Bitmap.CompressFormat.JPEG, 100, it)
       }


    }
    fun saveScreenshot(){
        saveCroppedBitmap(croppedBitmap)
        cropView?.showDrawable = true
        cropView?.resetView()
        uriToEdit = saveImageToPhotoGallery(context.contentResolver, croppedBitmap, name)
        optionsWindowView.destroyView()

        mainActivity?.saveScreenshotWIthPermission(uriToImage.toString(), uriToEdit.toString())

        if (isToShare){
            shareScreenShot(uriToImage)
        } else if(isToEdit){
            editScreenShot()
        }

    }
    private fun shareScreenShot(uriToScan: Uri?) {
        Log.i("Myuri","$uriToScan")
        uriToScan?.let {
            MediaScannerConnection.scanFile(context,
                arrayOf(uriToImage.toString()),
                arrayOf("image/jpeg")){ path, uri ->
                val shareIntent: Intent

                if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q){
                     shareIntent = Intent().apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_STREAM, uri)
                        type = "image/jpeg"
                    }
                }else {
                    shareIntent = Intent().apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_STREAM, uriToEdit)
                        type = "image/jpeg"
                    }
                }

                mainActivity?.let {
                    startActivity(it,
                        Intent.createChooser(shareIntent,
                            context.resources.getText(R.string.share)),
                        null)
                }

            }
        }
        isToShare = false
    }
    private fun editScreenShot() {

        val intent = Intent(Intent.ACTION_EDIT).apply {
            setDataAndType(uriToEdit, "image/*")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        mainActivity?.let {
            startActivity(it,intent,null)
        }
        isToEdit = false
    }

    /**
     * ends region of methods used to process the bitmap
     */


    override fun onSaveScreenshotSelected() {
        saveScreenshot()
//        if (mainActivity?.checkIfPermissionToSave() == true){
//            saveScreenshot()
//        } else {
//           mainActivity?. callPermissionToSaveDialog()
//        }
    }

    override fun onDeleteScreenshotSelected() {
        cropView?.showDrawable = true
        optionsWindowView.destroyView()
        Toast.makeText(context,"Screenshot Deleted",Toast.LENGTH_SHORT).show()
    }

    override fun onShareScreenshotSelected() {
        isToShare = true
        if (mainActivity?.checkIfPermissionToSave() == true){
            saveScreenshot()
        } else {
            mainActivity?. callPermissionToSaveDialog()
        }

    }

    override fun onAddNoteToScreenshotSelected() {
        Toast.makeText(context,"onAddNoteToScreenshot",Toast.LENGTH_SHORT).show()
    }

    override fun onEditScreenshotSelected() {
        isToEdit = true
        if (mainActivity?.checkIfPermissionToSave() == true){
            saveScreenshot()
        } else {
            mainActivity?.callPermissionToSaveDialog()
        }


    }



    /**
     * Starts section of helper methods to set up the Media projection
     */

    /**
     * if @param intent has the Data info, sets the notification and then calls
     * startProjection() else checks if this was called to stop the projection, if none of that
     * the service is stopped
     */
    fun setUpScreenCapture(intent: Intent, imageCoordinatesRect: Rect) {
        this.imageCoordinatesRect = imageCoordinatesRect
        when {
            isStartCommand(intent) -> {

                // start projection
                val resultCode = intent.getIntExtra(RESULT_CODE, Activity.RESULT_CANCELED)
                val data = intent.getParcelableExtra<Intent>(DATA)
                data?.let {
                    startProjection(resultCode, data)
                }

            }
            isStopCommand(intent) -> {
                stopProjection()
                cropViewFloatingWindowService.stopSelf()
            }
            else -> {
                cropViewFloatingWindowService.stopSelf()
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
                    cropViewFloatingWindowService.stopSelf()
                }
            }
        } else {
            Log.e(
                TAG,
                "failed to create file storage directory, getExternalFilesDir is null."
            )
            cropViewFloatingWindowService.stopSelf()
        }
    }

    /**
     * The screen dimensions to be used to take the screenshot
     */
    @SuppressLint("WrongConstant")
    private fun createVirtualDisplay() {

        val wm = context.getSystemService(Service.WINDOW_SERVICE) as WindowManager

        var  mDensity = 0

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val metrics: WindowMetrics =   wm.currentWindowMetrics
            // Gets all excluding insets
            // Gets all excluding insets
            val windowInsets = metrics.windowInsets
            val insets: Insets = windowInsets.getInsetsIgnoringVisibility(
                WindowInsets.Type.navigationBars()
                        or WindowInsets.Type.displayCutout()
            )

            val insetsWidth = insets.right + insets.left
            val insetsHeight = insets.top + insets.bottom

            // Legacy size that Display#getSize reports

            // Legacy size that Display#getSize reports
            val bounds = metrics.bounds
            mDensity =context.resources.displayMetrics.density.toInt()

            mWidth = bounds.width() - 0
            mHeight = bounds.height() - 0
        } else {
            val display = wm.defaultDisplay
            val metrics = DisplayMetrics()
            display.getMetrics(metrics)
            val size = Point()
            display.getRealSize(size)
            mWidth = size.x
            mHeight = size.y
            mDensity = metrics.densityDpi
        }


        // sta

        // start capture reader
        mImageReader = ImageReader
            .newInstance(mWidth, mHeight,
                PixelFormat.RGBA_8888, 2).apply {
                setOnImageAvailableListener(ImageAvailableListener(), mHandler)
            }
        mVirtualDisplay = mMediaProjection?.createVirtualDisplay(
            SCREENCAP_NAME,
            mWidth,
            mHeight,
            mDensity,
            getVirtualDisplayFlags(),
            mImageReader?.surface,
            null,
            mHandler
        )

    }

    fun getStartIntent(context: Context?, resultCode: Int, data: Intent?): Intent? {
        val intent = Intent(context, CropViewFloatingWindowService::class.java)
        intent.putExtra(ACTION, START)
        intent.putExtra(RESULT_CODE, resultCode)
        intent.putExtra(DATA, data)
        return intent
    }
    fun getStopIntent(context: Context?): Intent? {
        val intent = Intent(context, CropViewFloatingWindowService::class.java)
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
                mOrientationChangeCallback.disable()
                mMediaProjection?.unregisterCallback(this@MediaProjectionStopCallback)
                mMediaProjection = null
            })
        }
    }
    inner class ImageAvailableListener : ImageReader.OnImageAvailableListener {
        override fun onImageAvailable(reader: ImageReader) {
            if (IMAGES_PRODUCED == 0) {

                getBaseBitmap()

                IMAGES_PRODUCED++

            } else {
                stopProjection()
            }
        }
    }

    /**
     * Ends section of helper for media projection methods
     */
}