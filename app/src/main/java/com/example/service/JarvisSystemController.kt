package com.example.service

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.net.wifi.WifiManager
import android.provider.Settings
import android.util.Log
import android.net.Uri

/**
 * Advanced Neural System Controller.
 * Allows Jarvis to directly manipulate the device state and execute complex actions.
 */
object JarvisSystemController {
    private const val TAG = "JarvisSystemController"

    fun executeSystemCommand(context: Context, command: String, params: Map<String, Any>) {
        Log.d(TAG, "Executing neural command: $command with params: $params")
        
        when (command) {
            "TOGGLE_WIFI" -> toggleWifi(context, params["enabled"] as? Boolean ?: false)
            "TOGGLE_FLASHLIGHT" -> toggleFlashlight(context, params["enabled"] as? Boolean ?: false)
            "SEND_WHATSAPP" -> sendWhatsAppMessage(context, params["contact"] as? String ?: "", params["message"] as? String ?: "")
            "LAUNCH_APP" -> launchApp(context, params["packageName"] as? String ?: "")
            "SET_VOLUME" -> setVolume(context, params["level"] as? Int ?: 5)
            "SHOW_TOAST" -> showToast(context, params["message"] as? String ?: "")
            "SET_BRIGHTNESS" -> setBrightness(context, params["level"] as? Int ?: 100)
            "TOGGLE_DND" -> toggleDnd(context, params["enabled"] as? Boolean ?: false)
            else -> Log.w(TAG, "Unknown neural command: $command")
        }
    }

    private fun setBrightness(context: Context, level: Int) {
        val brightness = (level.coerceIn(0, 100) * 255 / 100)
        try {
            android.provider.Settings.System.putInt(
                context.contentResolver,
                android.provider.Settings.System.SCREEN_BRIGHTNESS,
                brightness
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error setting brightness", e)
        }
    }

    private fun toggleDnd(context: Context, enabled: Boolean) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        try {
            if (enabled) {
                notificationManager.setInterruptionFilter(android.app.NotificationManager.INTERRUPTION_FILTER_NONE)
            } else {
                notificationManager.setInterruptionFilter(android.app.NotificationManager.INTERRUPTION_FILTER_ALL)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling DND", e)
        }
    }

    private fun setVolume(context: Context, level: Int) {
        val audioManager = context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        val notificationManager = context.applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        
        try {
            val maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
            val volume = (level.coerceIn(0, 10) * maxVolume / 10)
            
            // On newer Android versions, background volume changes might trigger AppOps CONTROL_AUDIO warnings 
            // if we don't have notification policy access or aren't a privileged app.
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M || notificationManager.isNotificationPolicyAccessGranted) {
                 audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, volume, 0)
            } else {
                 // Try anyway, ignoring AppOps warning if it happens, but at least we checked.
                 // We could also just suppress this entirely in background to avoid log spam.
                 audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, volume, 0)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting volume", e)
        }
    }

    private fun showToast(context: Context, message: String) {
        com.example.util.ToastUtils.showToast(context, message, android.widget.Toast.LENGTH_LONG)
    }

    private fun toggleWifi(context: Context, enabled: Boolean) {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        @Suppress("DEPRECATION")
        wifiManager.isWifiEnabled = enabled
    }

    private fun toggleFlashlight(context: Context, enabled: Boolean) {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            val cameraId = cameraManager.cameraIdList[0]
            cameraManager.setTorchMode(cameraId, enabled)
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling flashlight", e)
        }
    }

    private fun sendWhatsAppMessage(context: Context, contact: String, message: String) {
        val intent = Intent(Intent.ACTION_VIEW)
        val url = "https://api.whatsapp.com/send?phone=$contact&text=${Uri.encode(message)}"
        intent.data = Uri.parse(url)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun launchApp(context: Context, packageName: String) {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}
