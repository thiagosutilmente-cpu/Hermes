package com.example.util

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast

/**
 * Utility object for displaying Android Toasts safely.
 * Cancels any previously queued Toast to prevent the Android NotificationManager/NotificationService
 * error: "Package has already queued 5 toasts. Not showing more."
 */
object ToastUtils {
    private var currentToast: Toast? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    fun showToast(context: Context, message: String, duration: Int = Toast.LENGTH_SHORT) {
        if (message.isBlank()) return
        mainHandler.post {
            try {
                currentToast?.cancel()
                val toast = Toast.makeText(context.applicationContext, message, duration)
                currentToast = toast
                toast.show()
            } catch (e: Exception) {
                // Ignore any Toast rendering exceptions gracefully
            }
        }
    }
}
