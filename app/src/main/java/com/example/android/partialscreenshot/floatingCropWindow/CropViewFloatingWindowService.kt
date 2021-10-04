package com.example.android.partialscreenshot.floatingCropWindow

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.*
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.appcompat.widget.ContentFrameLayout
import androidx.appcompat.widget.ContentFrameLayout.OnAttachListener
import com.example.android.partialscreenshot.R
import com.example.android.partialscreenshot.floatingCropWindow.cropWindow.CropView
import com.example.android.partialscreenshot.floatingCropWindow.cropWindow.ScreenShotTaker
import com.example.android.partialscreenshot.notification_utils.NotificationUtils
import com.example.android.partialscreenshot.utils.*



class CropViewFloatingWindowService: Service() {


    private lateinit var floatingView: CropView
    private var mData: Intent? = null
    lateinit var screenShotTaker: ScreenShotTaker
    private var isCropWindowOn = false

    private var takeScreenShotServiceCallback: FloatingWindowListener? = null
    private val binder: IBinder = LocalBinder()
    companion object{

        lateinit var manager: WindowManager
    }


    inner class LocalBinder : Binder() {
        // Return this instance of LocalService so clients can call public methods
        fun getService(): CropViewFloatingWindowService = this@CropViewFloatingWindowService
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder;
    }

    fun setServiceCallBacks(floatingAndTakeScreenShotServiceCallback: FloatingWindowListener?){
        takeScreenShotServiceCallback = floatingAndTakeScreenShotServiceCallback
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        if (!isCropWindowOn){
            setUpNotification()
            setUpFloatingWidget()
            screenShotTaker = ScreenShotTaker(applicationContext, this, floatingView, takeScreenShotServiceCallback)

            mData = takeScreenShotServiceCallback?.getDataToRecordScreen()

        }



        return START_NOT_STICKY
    }

    fun hideCropView(visibility: Int){
        floatingView.visibility = visibility
    }
    private fun setUpNotification() {

        // create notification
        val notification = NotificationUtils.getNotification(this,
            NotificationUtils.N_ID_F_ScreenShot)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            startForeground(
                notification.first,
                notification.second,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        }else{
            startForeground(
                notification.first,
                notification.second
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if(floatingView.isShown){
            manager.removeView(floatingView)
        }

    }

    private fun setUpFloatingWidget() {

        manager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        floatingView  = LayoutInflater.from(this).inflate(R.layout.crop_view, null) as CropView
        floatingView.setOnRequestTakeScreenShotListener(object: OnRequestTakeScreenShotListener {
            override fun onRequestScreenShot(rect: Rect) {
                screenShotTaker.getStartIntent(applicationContext, -1, mData)?.let {
                    screenShotTaker.setUpScreenCapture(it, rect)
                }
            }
        })

        floatingView.addOnAttachStateChangeListener(object: View.OnAttachStateChangeListener{
            override fun onViewAttachedToWindow(p0: View?) {
                isCropWindowOn = true
            }

            override fun onViewDetachedFromWindow(p0: View?) {
              isCropWindowOn = false
            }

        }
        )

            manager.addMyCropView(floatingView, ViewGroup.LayoutParams.WRAP_CONTENT,0, INITIAL_POINT)


    }

    fun setCroppedImage(croppedBitmap: Bitmap?) {
        floatingView.croppedImage = croppedBitmap
    }

}




