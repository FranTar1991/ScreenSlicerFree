package com.example.android.partialscreenshot.utils

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
}

interface OnOptionsWindowSelectedListener{
    fun onSaveScreenshot(isToShare: Boolean = false)
    fun onDeleteScreenshot()
    fun onShareScreenshot()
    fun onAddNoteToScreenshot()
    fun onEditScreenshot()
}