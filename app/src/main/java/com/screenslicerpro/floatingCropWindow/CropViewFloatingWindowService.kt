package com.screenslicerpro.floatingCropWindow

import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.*
import android.util.Log
import android.view.*
import com.screenslicerpro.MainActivity
import com.screenslicerpro.R
import com.screenslicerpro.floatingCropWindow.cropWindow.CropView
import com.screenslicerpro.floatingCropWindow.cropWindow.ScreenShotTaker
import com.screenslicerpro.gestures.CustomConstraintLayout
import com.screenslicerpro.notification_utils.NotificationUtils
import com.screenslicerpro.utils.*
import tourguide.tourguide.TourGuide


class CropViewFloatingWindowService: Service() {


    private lateinit var floatingGestureView: CustomConstraintLayout
    private var drawableForSwitch: Int = R.drawable.ic_toggle_off
    private val SHOW_SECOND_TOUR: String ="show_second_tour"
    private var showTourGuide: Boolean = true

    private var mTourGuideHandler: TourGuide? = null
    private var floatingView: CropView? = null
    private var mData: Intent? = null
    var screenShotTaker: ScreenShotTaker? = null
    private var isCropWindowOn = false
    private var isGestureWindowOn = false
    private var sharedPreferences: SharedPreferences? = null
    private var editor: SharedPreferences.Editor? = null

    private var manager: WindowManager? = null



    private var takeScreenShotServiceCallback: FloatingWindowListener? = null
    private val binder: IBinder = LocalBinder()




    inner class LocalBinder : Binder() {
        // Return this instance of LocalService so clients can call public methods
        fun getService(): CropViewFloatingWindowService = this@CropViewFloatingWindowService
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder;
    }

    override fun onCreate() {
        super.onCreate()
        startSharedPreferences()
        manager = WindowManagerClass.getMyWindowManager(applicationContext)
    }

    fun setServiceCallBacks(floatingAndTakeScreenShotServiceCallback: FloatingWindowListener?, from: String){

        takeScreenShotServiceCallback = floatingAndTakeScreenShotServiceCallback
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val currentDrawable = intent?.getIntExtra(MY_VIEW_ID,-1) ?: -1

        if(currentDrawable == -1 || currentDrawable == -2)
       {
           if (!isGestureWindowOn && drawableForSwitch == R.drawable.ic_toggle_on){
               setUpGestureWidget()
           }

           if (!isCropWindowOn){

               setUpNotification()
               setUpFloatingWidget()

               screenShotTaker = ScreenShotTaker(applicationContext, this, floatingView, takeScreenShotServiceCallback as MainActivity)

               mData = takeScreenShotServiceCallback?.getDataToRecordScreen()


           } else {
               floatingView?.setDrawMyWaitDrawable()
               shakeItBaby(applicationContext)
           }

       }else {

            drawableForSwitch = getNextDrawable(currentDrawable)
            saveToSharedPreferences(drawableForSwitch)
            setUpNotification()

            isToAddOrRemove(drawableForSwitch)

        }


        return START_NOT_STICKY
    }

    private fun isToAddOrRemove(currentDrawable: Int) {

        when(currentDrawable){
            R.drawable.ic_toggle_on ->{ setUpGestureWidget()}
            R.drawable.ic_toggle_off ->{ manager?.removeView(floatingGestureView)}
        }
    }

    private fun getNextDrawable(currentDrawable: Int): Int {
        return if (currentDrawable == R.drawable.ic_toggle_on){
            R.drawable.ic_toggle_off
        } else{
            R.drawable.ic_toggle_on
        }
    }

    private fun saveToSharedPreferences(currentDrawable: Int) {
        editor?.putInt(MY_VIEW_ID,currentDrawable)
        editor?.apply()
    }

    private fun setUpGestureWidget() {
        floatingGestureView = LayoutInflater.from(this).inflate(R.layout.surface_for_gestures_layout, null) as CustomConstraintLayout
        val paramsF = WindowManager.LayoutParams(
            96,
            96,
            layoutFlag,
            allFlags,
            PixelFormat.TRANSLUCENT
        )

        paramsF.gravity = Gravity.TOP or Gravity.START

        manager?.addView(floatingGestureView, paramsF)
        floatingGestureView.addOnAttachStateChangeListener(object: View.OnAttachStateChangeListener{
            override fun onViewAttachedToWindow(p0: View?) {
                isGestureWindowOn = true
            }

            override fun onViewDetachedFromWindow(p0: View?) {
                isGestureWindowOn = false
            }

        }
        )

    }

    private fun startSharedPreferences() {
        sharedPreferences = getSharedPreferences("MyPref", MODE_PRIVATE)
        showTourGuide =  sharedPreferences?.getBoolean(SHOW_SECOND_TOUR, true) ?: true
        editor = sharedPreferences?.edit()
        drawableForSwitch = sharedPreferences?.getInt(MY_VIEW_ID,R.drawable.ic_toggle_off) ?: R.drawable.ic_toggle_off
    }

    fun hideCropView(visibility: Int){
        floatingView?.visibility = visibility
        screenShotTaker?.optionsWindowView?.hideOptionsView(visibility)
    }
    private fun setUpNotification() {

        // create notification
        val notification = NotificationUtils.getNotification(this,
            NotificationUtils.N_ID_F_ScreenShot, drawableForSwitch)
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
            manager?.removeView(floatingView)
            screenShotTaker?.optionsWindowView?.destroyView()
        }

        if (floatingGestureView.isShown){
            manager?.removeView(floatingGestureView)
        }

    }

     private fun setUpFloatingWidget() {

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



         manager?.addMyCropView(floatingView, ViewGroup.LayoutParams.WRAP_CONTENT,0, INITIAL_POINT)



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




