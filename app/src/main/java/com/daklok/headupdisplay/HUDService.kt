package com.daklok.headupdisplay

import android.Manifest
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import androidx.annotation.RequiresPermission
import java.io.OutputStream
import java.util.UUID

class HUDService : NotificationListenerService() {

    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null


    private val MY_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private val DEVICE_NAME = "HUD-ESP32" // meno esp32

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == "com.google.android.apps.maps") {
            val extras = sbn.notification.extras

            val title = extras.getCharSequence("android.title")?.toString() ?: ""
            val text = extras.getCharSequence("android.text")?.toString() ?: ""
            val fullContext = "$title $text"

            // smer
            val directionKeywords = "(?:left|right|straight|slight left|slight right|sharp left|sharp right)"
            val exitPattern = "(\\d+(?:st|nd|rd|th))"
            val exitMatch = Regex(exitPattern, RegexOption.IGNORE_CASE).find(fullContext)
            val directionMatch = Regex(directionKeywords, RegexOption.IGNORE_CASE).find(fullContext)
            val maneuver = listOf(exitMatch?.value ?: "", directionMatch?.value ?: "").filter { it.isNotEmpty() }.joinToString(" ")

            // vzdialenost
            val distanceRegex = "(\\d+(?:[.,]\\d+)?\\s*(?:km|m))".toRegex()
            val distanceOnly = distanceRegex.find(fullContext)?.value ?: ""

            // Cas
            val subText = extras.getCharSequence("android.subText")?.toString() ?: ""
            val timeRegex = "([0-9]{1,2}:[0-9]{2})".toRegex()
            val arrivalTime = timeRegex.find(subText)?.value ?: ""


            val dataToSend = "$maneuver,$distanceOnly,$arrivalTime\n"

            sendDataToBluetooth(dataToSend)
            Log.d("HUD_DEBUG", "Sent to BT: $dataToSend")
        }
    }

    private fun sendDataToBluetooth(data: String) {
        try {
            if (bluetoothSocket == null || !bluetoothSocket!!.isConnected) {
                connectToEsp32()
            }
            outputStream?.write(data.toByteArray())
        } catch (e: Exception) {
            Log.e("HUD_DEBUG", "Error sending BT data: ${e.message}")
            bluetoothSocket = null // Reset for retry next time
        }
    }


    private fun connectToEsp32() {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter() ?: return

        // On Android 12 (API 31) and above, we must check BLUETOOTH_CONNECT
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                Log.e("HUD_DEBUG", "Missing BLUETOOTH_CONNECT permission")
                return
            }
        }

        try {
            val pairedDevices = bluetoothAdapter.bondedDevices
            val device = pairedDevices.find { it.name == DEVICE_NAME }

            if (device != null) {
                bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID)
                bluetoothSocket?.connect()
                outputStream = bluetoothSocket?.outputStream
                Log.d("HUD_DEBUG", "Bluetooth Connected to ${device.name}")
            } else {
                Log.e("HUD_DEBUG", "ESP32 Device '$DEVICE_NAME' not found in paired devices")
            }
        } catch (e: SecurityException) {
            Log.e("HUD_DEBUG", "SecurityException: Permission denied at runtime: ${e.message}")
        } catch (e: Exception) {
            Log.e("HUD_DEBUG", "Connection failed: ${e.message}")
            bluetoothSocket = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            bluetoothSocket?.close()
        } catch (e: Exception) { }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d("HUD_DEBUG", "INTERNAL: Service is officially CONNECTED")
        connectToEsp32()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {}
}