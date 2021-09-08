package com.example.android.partialscreenshot.floatingCropWindow

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.*
import android.view.*
import com.example.android.partialscreenshot.R
import com.example.android.partialscreenshot.floatingCropWindow.cropWindow.CropView
import com.example.android.partialscreenshot.floatingCropWindow.cropWindow.ScreenShotTaker
import com.example.android.partialscreenshot.utils.FloatingWindowListener
import com.example.android.partialscreenshot.utils.OnRequestTakeScreenShotListener
import com.example.android.partialscreenshot.utils.addMyCropView


class FloatingWindowService: Service() {

     lateinit var floatingView: CropView
    private var mData: Intent? = null
    private lateinit var screenShotTaker: ScreenShotTaker

    private var takeScreenShotServiceCallback: FloatingWindowListener? = null
    private val binder: IBinder = LocalBinder()
    companion object{

        lateinit var manager: WindowManager
    }


    inner class LocalBinder : Binder() {
        // Return this instance of LocalService so clients can call public methods
        fun getService(): FloatingWindowService = this@FloatingWindowService
    }

    override fun onCreate() {
        super.onCreate()
        screenShotTaker = ScreenShotTaker(applicationContext,this)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder;
    }

    fun setServiceCallBacks(floatingAndTakeScreenShotServiceCallback: FloatingWindowListener?){
        takeScreenShotServiceCallback = floatingAndTakeScreenShotServiceCallback
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        setUpFloatingWidget()
        mData = takeScreenShotServiceCallback?.getDataToRecordScreen()
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
                screenShotTaker.getStartIntent(applicationContext, -1, mData)?.let {
                    screenShotTaker.setUpScreenCapture(it, rect)
                }
            }
        })
        manager.addMyCropView(floatingView, ViewGroup.LayoutParams.WRAP_CONTENT,0,0)
    }

}




