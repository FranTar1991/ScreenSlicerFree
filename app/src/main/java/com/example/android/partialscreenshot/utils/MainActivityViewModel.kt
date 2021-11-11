package com.example.android.partialscreenshot.utils

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainActivityViewModel : ViewModel() {
    private val mutableOverlayCall = MutableLiveData<Boolean>()
    val overlayCall: LiveData<Boolean> get() = mutableOverlayCall

    private val _imageInFloatingWindowUri = MutableLiveData<String>()
    val imageInFloatingWindow
            get() = _imageInFloatingWindowUri


    fun setFloatingImageViewUri(uri: String){
        _imageInFloatingWindowUri.value = uri
    }

    fun setFloatingImageViewUriDone(){
        _imageInFloatingWindowUri.value = null
    }

    fun checkIfHasOverlayPermission(call: Boolean) {
        mutableOverlayCall.value = call
    }

    fun checkIfOverlayPermissionDone(){
        mutableOverlayCall.value = false
    }

}