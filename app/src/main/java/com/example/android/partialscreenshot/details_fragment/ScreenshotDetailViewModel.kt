package com.example.android.partialscreenshot.details_fragment

import androidx.lifecycle.*
import com.example.android.partialscreenshot.database.ScreenshotItem
import com.example.android.partialscreenshot.database.ScreenshotsDAO

class ScreenshotDetailViewModel  (private val screenshotId: Long = 0L,
                                  private val dataSource: ScreenshotsDAO) : ViewModel() {

    private val screenshot = MediatorLiveData<ScreenshotItem>()
    fun getScreenshot() = screenshot

    init {
        screenshot.addSource(dataSource.get(screenshotId)) { screenshot.setValue(it) }
    }

}