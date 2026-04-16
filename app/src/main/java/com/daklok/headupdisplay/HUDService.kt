package com.daklok.headupdisplay

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import android.app.Notification

class HUDService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == "com.google.android.apps.maps") {
            val extras = sbn.notification.extras


            for (key in extras.keySet()) {
                val value = extras.get(key)
                Log.d("HUD_DEBUG", "Key: $key | Value: $value")
            }


            val title = extras.getString("android.title")
            val text = extras.getString("android.text")
            val subText = extras.getString("android.subText")

            Log.d("HUD_DEBUG", "FOUND DATA -> $title | $text | $subText")
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d("HUD_DEBUG", "INTERNAL: Service is officially CONNECTED to the System")
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        //Tu logika ked sa navigacia zastavi
    }
}