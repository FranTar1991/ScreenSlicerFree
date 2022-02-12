package com.screenslicerfree.notification_utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.screenslicerfree.utils.STOP_INTENT

class NotificationBroadcastReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, p1: Intent?) {

        val stopIntent = Intent()
        stopIntent.action = STOP_INTENT
        context?.sendBroadcast(stopIntent)

    }
}