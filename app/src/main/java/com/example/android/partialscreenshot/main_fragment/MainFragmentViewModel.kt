package com.example.android.partialscreenshot.main_fragment

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.android.partialscreenshot.database.ScreenshotItem
import com.example.android.partialscreenshot.database.ScreenshotsDAO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainFragmentViewModel  (val database: ScreenshotsDAO, application: Application) : AndroidViewModel(application) {
    val screenshots = database.getAllScreenshots()

    private val _navigateToScreenshot = MutableLiveData<Long>()
    val navigateToScreenshot
        get() = _navigateToScreenshot


    fun onSaveScreenshot(newScreenshotItem: ScreenshotItem){
        viewModelScope.launch {
            insert(newScreenshotItem)
        }
    }


    private suspend fun insert(screenshot: ScreenshotItem) {
        withContext(Dispatchers.IO) {
                database.insertScreenshot(screenshot)
        }
    }
    fun onScreenshotClicked(id: Long) {
        _navigateToScreenshot.value = id
    }

    fun onScreenshotNavigated() {
        _navigateToScreenshot.value = null
    }

}