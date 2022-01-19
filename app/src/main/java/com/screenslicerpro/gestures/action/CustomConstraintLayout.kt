package com.screenslicerpro.gestures.action

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.*
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import com.screenslicerpro.floatingCropWindow.CropViewFloatingWindowService
import com.screenslicerpro.utils.*

import android.app.usage.UsageStats

import android.app.usage.UsageStatsManager
import java.util.*


class CustomConstraintLayout @JvmOverloads constructor(
    private val ct: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0): ConstraintLayout(ct,attrs, defStyleAttr) {

    private var audioManager: AudioManager
    private lateinit var doubleTapGesture: GestureDetector
    private lateinit var swipeWithTwoFingerDetector: SimpleTwoFingerSwipeDetector
    private var wm: WindowManager? = null


    private lateinit var mHandler: Handler
    private lateinit var mRunnable: Runnable
    private var mTime: Long = ViewConfiguration.getScrollDefaultDelay().toLong()


    var notFocusableFlag =  WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE

    init {
        wm = WindowManagerClass.getMyWindowManager(context)
        setSwipeWithTwoFingerDetector()
        setHandler()
         audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {

        if (printForegroundTask() == "com.screenslicerpro" || printForegroundTask() == "ni.com.lafise" ){
            startHandler()
            changeFlags(notFocusableFlag, MATCH_PARENT)
            event?.let {
                swipeWithTwoFingerDetector.onTouchEvent(event)
            }
        }

        return false

    }


    @SuppressLint("WrongConstant")
    private fun printForegroundTask(): String {
        var currentApp = ""
        val usm = context.getSystemService("usagestats") as UsageStatsManager
        val time = System.currentTimeMillis()
        val appList =
            usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 1000, time)
        if (appList != null && appList.size > 0) {
            val mySortedMap: SortedMap<Long, UsageStats> = TreeMap()
            for (usageStats in appList) {
                mySortedMap[usageStats.lastTimeUsed] = usageStats
            }
            if (!mySortedMap.isEmpty()) {
                currentApp = mySortedMap[mySortedMap.lastKey()]?.packageName ?: "Not detected"
            }
        }

        Log.e("adapter", "Current App in foreground is: $currentApp")
        return currentApp
    }



    private fun setGestureDetector() {

//         doubleTapGesture =
//            object : SimpleTwoFingerDoubleTapDetector() {
//                override fun onTwoFingerDoubleTap(event: MotionEvent) {
//                    Toast.makeText(context,"double tap on ${event.x} and ${event.y}",Toast.LENGTH_SHORT).show()
//                    changeFlags(allFlags)
//                    stopHandler()
//                }
//            }

        doubleTapGesture = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {

            override fun onSingleTapUp(event: MotionEvent?): Boolean {
                Toast.makeText(context,"double tap on ${event?.x} and ${event?.y}",Toast.LENGTH_SHORT).show()
                changeFlags(allFlags, 96)
                stopHandler()
                return super.onSingleTapUp(event)
            }
        })
    }

    private fun setSwipeWithTwoFingerDetector(){
                 swipeWithTwoFingerDetector =
            object : SimpleTwoFingerSwipeDetector() {
                override fun onTwoFingerSwipeDetector(event: MotionEvent?) {
                    Toast.makeText(context,"double swipe on ${event?.x} and ${event?.y}",Toast.LENGTH_SHORT).show()
                    stopHandler()
                    val intent = Intent(context, CropViewFloatingWindowService::class.java)
                    intent.putExtra(NEW_POSITION_X,event?.x)
                    intent.putExtra(NEW_POSITION_Y,event?.y)
                   context.startService(intent)

                    INITIAL_POINT_X = event?.x?.toInt() ?: 0
                    INITIAL_POINT_Y = event?.y?.toInt() ?: 0
                    changeFlags(allFlags, 96)

                }
            }
    }

    private fun setHandler(){
        // Initializing the handler and the runnable
        mHandler = Handler(Looper.getMainLooper())
        mRunnable = Runnable {
            changeFlags(allFlags, 96)
        }
    }

    // start handler function
    private fun startHandler(){
        mHandler.postDelayed(mRunnable, mTime)
    }

    // stop handler function
    private fun stopHandler(){
        mHandler.removeCallbacks(mRunnable)
    }

    fun changeFlags(flag: Int, mode: Int){

        wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager?
        val paramsF = WindowManager.LayoutParams(
            mode,
            mode,
            layoutFlag,
            flag,
            PixelFormat.TRANSLUCENT
        )
        paramsF.gravity = Gravity.TOP or Gravity.START

        try {
            wm?.updateViewLayout(this, paramsF)
        } catch (e: IllegalArgumentException){
            Log.i("MyViewError","error: $e")
        }





    }
}