package com.screenslicerpro.gestures

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.AttributeSet
import android.util.Log
import android.view.*
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import com.screenslicerpro.floatingCropWindow.CropViewFloatingWindowService
import com.screenslicerpro.utils.WindowManagerClass
import com.screenslicerpro.utils.allFlags
import com.screenslicerpro.utils.layoutFlag


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

        startHandler()
        changeFlags(notFocusableFlag, MATCH_PARENT)
        event?.let {
            swipeWithTwoFingerDetector.onTouchEvent(event)
        }

        return false

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
                   context.startService(intent)

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