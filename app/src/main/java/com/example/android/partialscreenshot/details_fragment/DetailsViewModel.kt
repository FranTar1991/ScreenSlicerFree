package com.example.android.partialscreenshot.details_fragment

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import android.view.View
import androidx.lifecycle.*
import com.example.android.partialscreenshot.R
import com.example.android.partialscreenshot.database.ScreenshotItem
import com.example.android.partialscreenshot.database.ScreenshotsDAO
import com.example.android.partialscreenshot.utils.deleteItemFromGallery

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.Exception

class DetailsViewModel  (private val screenshotId: Long = 0L,
                         private val dataSource: ScreenshotsDAO) : ViewModel() {

    private val _navigateToMainFragment = MediatorLiveData<Boolean>()
    val navigateToMainFragment
        get() = _navigateToMainFragment

    private val _screenshot = MediatorLiveData<ScreenshotItem>()
    val screenshot
            get() = _screenshot

    private val _authorizationToMakeChanges = MediatorLiveData<Int>()
    val authorizationToMakeChanges
        get() = _authorizationToMakeChanges

    private val _editScreenshot = MediatorLiveData<ScreenshotItem>()
    val editScreenshot
        get() = _editScreenshot

    private val _shareScreenshot = MediatorLiveData<ScreenshotItem>()
    val shareScreenshot
        get() = _shareScreenshot


    init {
        _screenshot.addSource(dataSource.getById(screenshotId)) { _screenshot.setValue(it) }
    }


    fun onConfirmToMakeAction(authorization: Boolean, id: Int, resolver: ContentResolver){

        when(id){
            R.id.edit_options -> if (authorization) onEdiThisItem()
            R.id.delete_options -> if (authorization) onDeleteThisItem(resolver)
            R.id.share_options -> if (authorization) onShareThisItem()
        }


    }

    private fun onEdiThisItem() {
      _editScreenshot.value = screenshot.value
    }

    private fun onShareThisItem(){
        _shareScreenshot.value = screenshot.value
    }
    fun onAskAuthorization(view: View?){
        authorizationToMakeChanges.value = view?.id
    }

    fun onDeleteThisItem(resolver: ContentResolver){
        viewModelScope.launch {
            deleteItem(resolver)
        }
    }

    private suspend fun deleteItem(resolver: ContentResolver){
        withContext(Dispatchers.IO){
            dataSource.clearById(screenshotId)
            val list = listOf(screenshot.value?.uri)
            deleteItemFromGallery(list,resolver)

            try {
                resolver.delete(Uri.parse(screenshot.value?.uri),null, null)
            } catch (e: Exception) {
                Log.e("MyDeleteRequest", "Exception $e")
            }

        }
    }

    fun onNavigateToMainFragment() {
        _navigateToMainFragment.value = true
    }

    fun onNavigateToMainFragmentDone(){
        _navigateToMainFragment.value = null
    }

    fun onActionFlagReceived() {
        _editScreenshot.value = null
        _shareScreenshot.value = null
    }

}