package com.example.android.partialscreenshot.floatingView

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.*
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowManager.LayoutParams.*
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.android.partialscreenshot.R
import com.example.android.partialscreenshot.utils.flags
import com.example.android.partialscreenshot.utils.layoutFlag


class FloatingImageViewService: Service() {

    private lateinit var floatingImage: ImageView
    private var mImageUri: Uri? = null
    private lateinit var floatingViewContainer: ConstraintLayout
    private lateinit var manager: WindowManager
    private val binder = LocalBinder()
    private var isFloatingWindowOn = false
    private lateinit var doubleTapGesture: GestureDetector

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    inner class LocalBinder : Binder() {
        // Return this instance of LocalService so clients can call public methods
        fun getService(): FloatingImageViewService = this@FloatingImageViewService
    }

    override fun onBind(p0: Intent?): IBinder? {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        setGestureDetector()
        if(!isFloatingWindowOn){
            setUpFloatingWidget()
        } else {
            floatingImage.setImageURI(mImageUri)
        }

        return START_NOT_STICKY
    }

    fun setImageUri(uri: Uri){
        mImageUri = uri
    }

    private fun setGestureDetector() {

        doubleTapGesture = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {

            override fun onDoubleTap(e: MotionEvent?): Boolean {
                manager.removeView(floatingViewContainer)
                return super.onDoubleTap(e)
            }
        })
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setUpFloatingWidget() {

        manager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        floatingViewContainer  = LayoutInflater.from(this).inflate(R.layout.floating_image_view, null) as ConstraintLayout
        floatingImage = floatingViewContainer.findViewById<ImageView>(R.id.floating_view)

        val params = WindowManager.LayoutParams(
            WRAP_CONTENT,
            WRAP_CONTENT,
            layoutFlag,
            flags,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START    //Initially view will be added to top-left corner
        params.x = 0
        params.y = 0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            params.layoutInDisplayCutoutMode = LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            params.layoutInDisplayCutoutMode = LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        floatingViewContainer?.addOnAttachStateChangeListener(object: View.OnAttachStateChangeListener{
            override fun onViewAttachedToWindow(p0: View?) {
                floatingImage.setImageURI(mImageUri)
                isFloatingWindowOn = true
            }

            override fun onViewDetachedFromWindow(p0: View?) {
                isFloatingWindowOn = false
            }

        }
        )



        floatingViewContainer?.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            override fun onTouch(p0: View?, p1: MotionEvent?): Boolean {
                doubleTapGesture.onTouchEvent(p1)
                when (p1?.action) {
                    MotionEvent.ACTION_DOWN -> {

                        //remember the initial position.
                        initialX = params.x
                        initialY = params.y

                        //get the touch location
                        initialTouchX = p1.rawX
                        initialTouchY = p1.rawY
                    }
                    MotionEvent.ACTION_UP -> {

                    }
                    MotionEvent.ACTION_MOVE -> {
                        //Calculate the X and Y coordinates of the view.
                        params.x = initialX + (p1.rawX - initialTouchX).toInt()
                        params.y = initialY + (p1.rawY - initialTouchY).toInt()
                        manager.updateViewLayout(floatingViewContainer, params);


                    }
                }
                return false
            }

        })


        manager.addView(floatingViewContainer, params)


    }
}