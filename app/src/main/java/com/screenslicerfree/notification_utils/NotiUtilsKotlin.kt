package com.screenslicerfree.notification_utils

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import org.chromium.base.ContextUtils

fun setUpNotification(context: Context, drawable: Int, service: Service) {

    // create notification
    val notification = NotificationUtils.getNotification(context,
        NotificationUtils.N_ID_F_ScreenShot, drawable)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

        service.startForeground(
            notification.first,
            notification.second,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
        )
    }else{
        service.startForeground(
            notification.first,
            notification.second
        )
    }
}

fun cancelNotification(context: Context?) {
    val notificationManager =context?.
    getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.cancel(NotificationUtils.N_ID_F_ScreenShot)
}