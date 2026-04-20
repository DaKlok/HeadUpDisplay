package com.daklok.headupdisplay

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import java.io.OutputStream
import java.util.UUID
import android.app.Notification

class HUDService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == "com.google.android.apps.maps") {
            val extras = sbn.notification.extras

            val title = extras.getCharSequence("android.title")?.toString() ?: ""
            val text = extras.getCharSequence("android.text")?.toString() ?: ""
            val fullContext = "$title $text"

            // 1. Extract Maneuver (Exit number AND/OR Direction)
            // This captures "2nd", "left", "right", "slight left", etc.
            val directionKeywords = "(?:left|right|straight|slight left|slight right|sharp left|sharp right)"
            val exitPattern = "(\\d+(?:st|nd|rd|th))"

            // Look for exit number
            val exitMatch = Regex(exitPattern, RegexOption.IGNORE_CASE).find(fullContext)
            // Look for direction keywords
            val directionMatch = Regex(directionKeywords, RegexOption.IGNORE_CASE).find(fullContext)

            val exitOnly = exitMatch?.value ?: ""
            val moveDirection = directionMatch?.value ?: ""

            // Combine them (e.g., "2nd right" or just "left")
            val maneuver = listOf(exitOnly, moveDirection).filter { it.isNotEmpty() }.joinToString(" ")

            // 2. Extract Distance
            val distanceRegex = "(\\d+(?:[.,]\\d+)?\\s*(?:km|m))".toRegex()
            val distanceMatch = distanceRegex.find(fullContext)
            val distanceOnly = distanceMatch?.value ?: ""

            // 3. Extract Arrival Time
            val subText = extras.getCharSequence("android.subText")?.toString() ?: ""
            val timeRegex = "([0-9]{1,2}:[0-9]{2})".toRegex()
            val timeMatch = timeRegex.find(subText)
            val arrivalTime = timeMatch?.value ?: ""

            Log.d("HUD_DEBUG", "Maneuver: $maneuver | Dist: $distanceOnly | Arr: $arrivalTime")
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