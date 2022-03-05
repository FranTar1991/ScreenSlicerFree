package com.screenslicerfree.utils

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.ads.interstitial.InterstitialAd

class MainActivityViewModel() : ViewModel() {
    private val mutableOverlayCall = MutableLiveData<Boolean>()
    val overlayCall: LiveData<Boolean> get() = mutableOverlayCall

    private val _imageInFloatingWindowUri = MutableLiveData<String?>()
    val imageInFloatingWindow
            get() = _imageInFloatingWindowUri


    private val _permissionToSaveCalled = MutableLiveData<Boolean>()
    val permissionToSaveCalled
        get() = _permissionToSaveCalled


    private val _destroyService = MutableLiveData<Boolean>()
    val destroyService
        get() = _destroyService



    private val _cleanTourGuide = MutableLiveData<String>()
    val cleanTourGuide
        get() = _cleanTourGuide

    fun setDestroyService(value: Boolean){
        _destroyService.value = value
    }


    fun setPermissionToSaveCalled(){
        _permissionToSaveCalled.value = true
    }


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

    fun setCleanTourGuide(key: String) {
        _cleanTourGuide.value = key
    }



}