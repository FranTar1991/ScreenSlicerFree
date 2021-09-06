package com.example.android.partialscreenshot

import java.text.SimpleDateFormat
import java.util.*

    fun getCurrentTimeStamp(): String? {
        // write bitmap to a file
        val dateFormat = SimpleDateFormat("yyyyMMdd_HH_mm_ss")
        return dateFormat.format(Date())
    }
