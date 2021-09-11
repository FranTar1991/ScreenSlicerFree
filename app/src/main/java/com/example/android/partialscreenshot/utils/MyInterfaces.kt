package com.example.android.partialscreenshot.utils

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.view.MotionEvent

interface FloatingWindowListener  {
    fun getPermissionToRecordScreen()
    fun getDataToRecordScreen(): Intent?
}

interface OnMoveCropWindowListener{
    fun onMove(event: MotionEvent?)
    fun onClose()
}

interface OnRequestTakeScreenShotListener{
    fun onRequestScreenShot(rect: Rect)
}

interface OnOptionsWindowSelectedListener{
    fun onSaveScreenshot()
    fun onDeleteScreenshot()
    fun onShareScreenshot()
    fun onAddNoteToScreenshot()
    fun onEditScreenshot()
}