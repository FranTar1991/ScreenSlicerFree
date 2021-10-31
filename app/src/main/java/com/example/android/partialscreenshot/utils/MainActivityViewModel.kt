package com.example.android.partialscreenshot.utils

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainActivityViewModel : ViewModel() {
    private val mutableOverlayCall = MutableLiveData<Boolean>()
    val overlayCall: LiveData<Boolean> get() = mutableOverlayCall

    private val _showImageInFloatingWindow = MutableLiveData<Boolean>()
    val showImageInFloatingWindow
            get() = _showImageInFloatingWindow


    fun callFloatingWindowWithImageCallback(call: Boolean){
        _showImageInFloatingWindow.value = call
    }

    fun callFloatingWindowWithImageCallbackDone(){
        _showImageInFloatingWindow.value = null
    }

    fun checkIfHasOverlayPermission(call: Boolean) {
        mutableOverlayCall.value = call
    }

}