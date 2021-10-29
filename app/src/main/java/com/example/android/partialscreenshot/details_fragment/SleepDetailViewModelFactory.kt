package com.example.android.partialscreenshot.details_fragment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.android.partialscreenshot.database.ScreenshotsDAO

class SleepDetailViewModelFactory (
    private val itemId: Long,
    private val dataSource: ScreenshotsDAO) : ViewModelProvider.Factory {
    @Suppress("unchecked_cast")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DetailsViewModel::class.java)) {
            return DetailsViewModel(itemId, dataSource) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}