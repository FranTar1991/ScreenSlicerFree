package com.example.android.partialscreenshot.utils

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainActivityViewModel : ViewModel() {
    private val mutableOverlayCall = MutableLiveData<Boolean>()
    val overlayCall: LiveData<Boolean> get() = mutableOverlayCall

    fun checkIfHasOverlayPermission(call: Boolean) {
        mutableOverlayCall.value = call
    }

}