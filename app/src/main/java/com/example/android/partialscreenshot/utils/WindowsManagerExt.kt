package com.example.android.partialscreenshot.utils

import android.graphics.PixelFormat
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import com.example.android.partialscreenshot.floatingCropWindow.cropWindow.CropView

fun WindowManager.addMyCropView(myFloatingView: CropView, mode: Int, initX: Int, initY: Int){
    val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    } else {
        WindowManager.LayoutParams.TYPE_PHONE
    }

    val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
    val params = WindowManager.LayoutParams(
        mode,
        mode,
        layoutFlag,
        flags,
        PixelFormat.TRANSLUCENT
    )

    //Specify the view position
    params.gravity = Gravity.TOP or Gravity.START    //Initially view will be added to top-left corner
    params.x = initX
    params.y = initY


    myFloatingView.setWindowManagerCallback(object : OnMoveCropWindowListener {
        private var initialX = 0
        private var initialY = 0
        private var initialTouchX = 0f
        private var initialTouchY = 0f
        override fun onMove(event: MotionEvent?) {
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> {

                    //remember the initial position.
                    initialX = params.x
                    initialY = params.y

                    //get the touch location
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                }
                MotionEvent.ACTION_UP -> {

                }
                MotionEvent.ACTION_MOVE -> {
                    //Calculate the X and Y coordinates of the view.
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()


                    //Update the layout with new X & Y coordinate
                    updateViewLayout(myFloatingView, params)
                    myFloatingView.setNewPositionOfSecondRect(params.x,params.y)

                }
            }

        }

        override fun onClose() {
            Log.i("MyWindows","Closing the window")
            removeView(myFloatingView)
        }

    })

    addView(myFloatingView, params)
}
fun WindowManager.removeMyView(myFloatingView: CropView, mode: Int, newX: Int, newY: Int){
    removeView(myFloatingView)
    addMyCropView(myFloatingView, mode, newX, newY)
}