package com.example.android.partialscreenshot.details_fragment

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.android.partialscreenshot.database.ScreenshotsDAO

class SleepDetailViewModelFactory (
    private val sleepNightKey: Long,
    private val dataSource: ScreenshotsDAO) : ViewModelProvider.Factory {
    @Suppress("unchecked_cast")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ScreenshotDetailViewModel::class.java)) {
            return ScreenshotDetailViewModel(sleepNightKey, dataSource) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}