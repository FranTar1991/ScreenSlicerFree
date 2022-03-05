package com.screenslicerfree.main_fragment

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.screenslicerfree.database.ScreenshotItem
import com.screenslicerfree.database.DAOScrenshots

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainFragmentViewModel  (val database: DAOScrenshots)
    : ViewModel() {


    private val _screenshots = database.getAllScreenshots()
    val screenshots
        get() = _screenshots

    private val _screenshotCount = MutableLiveData<Int>()
    val screenshotCount
        get() = _screenshotCount


    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading
        get() = _isLoading

    private val _navigateToScreenshot = MutableLiveData<String?>()
    val navigateToScreenshot
        get() = _navigateToScreenshot

    private val _navigateToGSettings= MutableLiveData<Boolean?>()
    val navigateToGSettings
        get() = _navigateToGSettings

    private val _navigateToProFragment= MutableLiveData<Boolean?>()
    val navigateToProFragment
        get() = _navigateToProFragment


    fun setScreenShotCount(count: Int) {
        _screenshotCount.value = count
    }


    fun setIsLoading(isLoading: Boolean){
        _isLoading.value = isLoading
    }

    fun onSaveScreenshot(newScreenshotItem: ScreenshotItem){
        viewModelScope.launch {
            insert(newScreenshotItem)
        }
    }

    fun onDeleteListWithUri(listToDelete: List<String>){
        viewModelScope.launch {
            deleteList(listToDelete)
        }
    }


    private suspend fun insert(screenshot: ScreenshotItem) {
        withContext(Dispatchers.IO) {
                database.insertScreenshot(screenshot)
        }
    }

    private suspend fun deleteList(uriToDeleteList: List<String>){
        withContext(Dispatchers.IO){
            database.clearAllByUri(uriToDeleteList)
        }
    }

    fun onScreenshotClicked(uri: String) {
        _navigateToScreenshot.value = uri
    }

    fun onScreenshotNavigated() {
        _navigateToScreenshot.value = null
    }

    fun onNavigateToGestureSettingsClicked() {
        _navigateToGSettings.value = true
    }

    fun onNavigateToGestureSettingsNavigated() {
        _navigateToGSettings.value = null
    }

    fun onNavigateToProClicked() {
        _navigateToProFragment.value = true
    }

    fun onNavigateToProNavigated() {
        _navigateToProFragment.value = null
    }

}