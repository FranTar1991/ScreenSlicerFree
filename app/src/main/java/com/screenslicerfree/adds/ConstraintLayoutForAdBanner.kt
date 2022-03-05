package com.screenslicerfree.adds

import android.content.Context
import android.media.Image
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_MOVE
import android.view.View
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import com.screenslicerfree.R
import com.screenslicerfree.utils.OnMoveCropWindowListener
import com.screenslicerfree.utils.ViewMobil

class ConstraintLayoutForAdBanner@JvmOverloads constructor(context: Context,
                                                           attrs: AttributeSet? = null,
                                                           defStyleAttr: Int = 0): ConstraintLayout(context, attrs, defStyleAttr), ViewMobil{

    private lateinit var callBackForWindowManager: OnMoveCropWindowListener

    override fun setWindowManagerCallback(onMoveCropWindowListener: OnMoveCropWindowListener) {
        this.callBackForWindowManager = onMoveCropWindowListener
    }


   fun getWindowCallback(): OnMoveCropWindowListener{
       return  callBackForWindowManager
   }

}