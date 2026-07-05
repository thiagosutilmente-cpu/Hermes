package com.example.service

import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * CoordinatorService serves as the central Foreground Service "brain" of our state machine,
 * inheriting from RadarCoordinatorService to maintain integration with voice analysis, 
 * safe speed monitoring, and user action automation.
 */
class CoordinatorService : RadarCoordinatorService() {

    companion object {
        val isServiceRunning: Boolean
            get() = RadarCoordinatorService.isServiceRunning

        fun startService(context: Context) {
            val intent = Intent(context, CoordinatorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, CoordinatorService::class.java)
            context.stopService(intent)
        }
    }
}
