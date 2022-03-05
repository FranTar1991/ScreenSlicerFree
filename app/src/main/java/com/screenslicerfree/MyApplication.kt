package com.screenslicerfree

import android.app.Application
import android.content.Context
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener


class MyApplication: Application() {

    init {
        instance = this
    }

    companion object {
        private var instance: MyApplication? = null

        fun applicationContext() : Context {
            return instance!!.applicationContext
        }
    }

    override fun onCreate() {
        super.onCreate()

        MobileAds.initialize(applicationContext,
            OnInitializationCompleteListener { })
    }
}