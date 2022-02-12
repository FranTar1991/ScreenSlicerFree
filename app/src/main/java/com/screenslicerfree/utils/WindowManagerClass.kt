package com.screenslicerfree.utils

import android.content.Context
import android.view.WindowManager

class WindowManagerClass {

    companion object {
        @Volatile
        private var WM: WindowManager? = null

        fun getMyWindowManager(context: Context): WindowManager {

            synchronized(this) {
                var wm = WM
                if (wm == null){
                    wm =  context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                    WM = wm
                }

                return wm
            }
        }
    }
}