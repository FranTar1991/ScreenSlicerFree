package com.screenslicerpro.floatingCropWindow

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.*
import android.util.Log
import android.view.*
import android.widget.Toast
import com.screenslicerpro.MainActivity
import com.screenslicerpro.R
import com.screenslicerpro.floatingCropWindow.cropWindow.CropView
import com.screenslicerpro.floatingCropWindow.cropWindow.ScreenShotTaker
import com.screenslicerpro.notification_utils.NotificationUtils
import com.screenslicerpro.utils.*
import tourguide.tourguide.TourGuide


class CropViewFloatingWindowService: Service() {


    private val SHOW_SECOND_TOUR: String ="show_second_tour"
    private var showTourGuide: Boolean = true

    private var mTourGuideHandler: TourGuide? = null
    private var floatingView: CropView? = null
    private var mData: Intent? = null
    var screenShotTaker: ScreenShotTaker? = null
    private var isCropWindowOn = false
    private var sharedPreferences: SharedPreferences? = null



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

    fun setServiceCallBacks(floatingAndTakeScreenShotServiceCallback: FloatingWindowListener?, from: String){

        takeScreenShotServiceCallback = floatingAndTakeScreenShotServiceCallback
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        if (!isCropWindowOn){
            startSharedPreferences()
            setUpNotification()
            setUpFloatingWidget()

           screenShotTaker = ScreenShotTaker(applicationContext, this, floatingView, takeScreenShotServiceCallback as MainActivity)

            mData = takeScreenShotServiceCallback?.getDataToRecordScreen()

        } else {
            floatingView?.setDrawMyWaitDrawable()
            shakeItBaby(applicationContext)
        }



        return START_NOT_STICKY
    }

    private fun startSharedPreferences() {
        sharedPreferences = getSharedPreferences("MyPref", MODE_PRIVATE)
        showTourGuide =  sharedPreferences?.getBoolean(SHOW_SECOND_TOUR, true) ?: true
    }

    fun hideCropView(visibility: Int){
        floatingView?.visibility = visibility
        screenShotTaker?.optionsWindowView?.hideOptionsView(visibility)
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
        if(floatingView?.isShown == true){
            manager.removeView(floatingView)
            screenShotTaker?.optionsWindowView?.destroyView()
        }

    }

     private fun setUpFloatingWidget() {

        manager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        floatingView  = LayoutInflater.from(this).inflate(R.layout.crop_view, null) as CropView
        floatingView?.setOnRequestTakeScreenShotListener(object: OnRequestTakeScreenShotListener {
            override fun onRequestScreenShot(rect: Rect) {
                screenShotTaker?.getStartIntent(applicationContext, -1, mData)?.let {
                    screenShotTaker?.setUpScreenCapture(it, rect)
                }
            }

            override fun cleanUpMyTourGuide() {
                mTourGuideHandler?.cleanUp()
                if(showTourGuide){
                    val editor: SharedPreferences.Editor? = sharedPreferences?.edit()
                    editor?.putBoolean(SHOW_SECOND_TOUR, false)
                    editor?.apply()
                }

            }
        })

        floatingView?.addOnAttachStateChangeListener(object: View.OnAttachStateChangeListener{
            override fun onViewAttachedToWindow(p0: View?) {
                isCropWindowOn = true
            }

            override fun onViewDetachedFromWindow(p0: View?) {
              isCropWindowOn = false
            }

        }
        )



        manager.addMyCropView(floatingView, ViewGroup.LayoutParams.WRAP_CONTENT,0, INITIAL_POINT)

         if(showTourGuide){
             mTourGuideHandler=  setMyTourGuide(takeScreenShotServiceCallback as MainActivity, getString(R.string.title_crop_view_tut),
                 getString(R.string.description_cropt_view_tut),
                 Gravity.END or Gravity.BOTTOM,
                 floatingView as View )
         }



    }



    fun setCroppedImage(croppedBitmap: Bitmap?) {
        floatingView?.croppedImage = croppedBitmap
    }



}




