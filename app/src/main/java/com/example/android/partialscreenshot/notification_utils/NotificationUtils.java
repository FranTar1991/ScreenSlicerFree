package com.example.android.partialscreenshot.notification_utils;


import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.util.Pair;

import com.example.android.partialscreenshot.MainActivity;
import com.example.android.partialscreenshot.R;
import com.example.android.partialscreenshot.floatingCropWindow.CropViewFloatingWindowService;


public class NotificationUtils {

    public static final int N_ID_F_ScreenShot = 1337;
    public static final int N_ID_F_Floating = 1338;
    private static final String NOTIFICATION_CHANNEL_ID = "com.partialscreenshot.app";
    private static final String NOTIFICATION_CHANNEL_NAME ="com.partialscreenshot.app";

    public static Pair<Integer, Notification> getNotification(@NonNull Context context, int id) {
        createNotificationChannel(context);
        Notification notification = createNotification(context);
        NotificationManager notificationManager
                = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(id, notification);
        return new Pair<>(id, notification);
    }

    @TargetApi(Build.VERSION_CODES.O)
    private static void createNotificationChannel(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    NOTIFICATION_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);
        }
    }

    private static Notification createNotification(@NonNull Context context) {

        Intent cropWindowIntent = new Intent(context, CropViewFloatingWindowService.class);
        cropWindowIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent cropWindowPendingIntent = PendingIntent.getService(context, 0 , cropWindowIntent, 0);


        Intent closeIntent = new Intent(context, NotificationBroadcastReceiver.class);
        PendingIntent closePendingIntent = PendingIntent.getBroadcast(context, 0 ,
                closeIntent, 0);

        Intent homeIntent = new Intent(context, MainActivity.class);
        PendingIntent homePendingIntent = PendingIntent.getActivity(context, 0 ,
                homeIntent, 0);


        RemoteViews notificationLayout = new RemoteViews(context.getPackageName(), R.layout.custom_layout_notification);
        notificationLayout.setOnClickPendingIntent(R.id.crop_window_btn, cropWindowPendingIntent);
        notificationLayout.setOnClickPendingIntent(R.id.close_btn,closePendingIntent);
        notificationLayout.setOnClickPendingIntent(R.id.home_btn,homePendingIntent);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID);
        builder.setSmallIcon(R.drawable.ic_camera)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(context.getString(R.string.recording))
                .setPriority(Notification.PRIORITY_HIGH)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setCustomContentView(notificationLayout)
                .setShowWhen(false);
        return builder.build();
    }


}
