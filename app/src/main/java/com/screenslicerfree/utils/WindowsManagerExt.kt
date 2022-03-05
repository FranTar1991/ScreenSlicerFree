package com.screenslicerfree.utils

import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.*
import com.screenslicerfree.adds.ConstraintLayoutForAdBanner
import com.screenslicerfree.floatingCropWindow.cropWindow.CropView

fun WindowManager.addMovableView(myFloatingView: ViewMobil?, mode: Int, initX: Int, initY: Int){

    val params = WindowManager.LayoutParams(
        mode,
        mode,
        layoutFlag,
        flags,
        PixelFormat.TRANSLUCENT
    )

    //Specify the view position
    if (myFloatingView is ConstraintLayoutForAdBanner){
        params.gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
    } else {
        params.gravity = Gravity.START or Gravity.TOP
    }
 //Initially view will be added to top-left corner
    params.x = initX
    params.y = initY



        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            params.layoutInDisplayCutoutMode = LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        params.layoutInDisplayCutoutMode = LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
         }


    myFloatingView?.setWindowManagerCallback(object : OnMoveCropWindowListener {
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
                    updateViewLayout(myFloatingView as View, params)

                    if (myFloatingView is CropView){
                        myFloatingView.setNewPositionOfSecondRect(params.x,params.y)
                    }


                }
            }

        }
        override fun onClose() {
             INITIAL_POINT_Y = 0
             INITIAL_POINT_X = 0
            removeView(myFloatingView as View)
        }

    })


        addView(myFloatingView as View, params)


}
fun WindowManager.removeMyView(myFloatingView: ViewMobil, mode: Int, newX: Int, newY: Int){
    removeView(myFloatingView as View)
    addMovableView(myFloatingView, mode, newX, newY)
}