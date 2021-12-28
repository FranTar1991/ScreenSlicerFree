package com.screenslicerpro.utils

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import android.view.MotionEvent

interface FloatingWindowListener  {
    fun checkIfPermissionToSave(): Boolean
    fun getDataToRecordScreen(): Intent?
}


interface OnMoveCropWindowListener{
    fun onMove(event: MotionEvent?)
    fun onClose()
}

interface OnRequestTakeScreenShotListener{
    fun onRequestScreenShot(rect: Rect)
    fun cleanUpMyTourGuide()
}

interface OnOptionsWindowSelectedListener{
    fun onSaveScreenshotSelected()
    fun onDeleteScreenshotSelected()
    fun onShareScreenshotSelected()
    fun onExtractTextSelected()
    fun onEditScreenshotSelected()
    fun onMinimizeCropView()
}