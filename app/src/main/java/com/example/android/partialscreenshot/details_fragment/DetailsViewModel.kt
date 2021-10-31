package com.example.android.partialscreenshot.details_fragment

import android.content.ContentResolver
import android.view.View
import androidx.lifecycle.*
import com.example.android.partialscreenshot.R
import com.example.android.partialscreenshot.database.ScreenshotItem
import com.example.android.partialscreenshot.database.ScreenshotsDAO

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DetailsViewModel  (private val screenshotUri: String,
                         private val dataSource: ScreenshotsDAO) : ViewModel() {

    private val _navigateToMainFragment = MediatorLiveData<Boolean>()
    val navigateToMainFragment
        get() = _navigateToMainFragment

    private val _screenshot = MediatorLiveData<ScreenshotItem>()
    val screenshot
            get() = _screenshot

    init {
        _screenshot.addSource(dataSource.getByUri(screenshotUri)) { _screenshot.setValue(it) }
    }


    fun onDeleteListWithUri(listToDelete: List<String>){
        viewModelScope.launch {
            deleteItem(listToDelete)
        }
    }

    private suspend fun deleteItem(list: List<String>){
        withContext(Dispatchers.IO){
            dataSource.clearAllByUri(list)
        }
    }

    fun onNavigateToMainFragment() {
        _navigateToMainFragment.value = true
    }

    fun onNavigateToMainFragmentDone(){
        _navigateToMainFragment.value = null
    }

}