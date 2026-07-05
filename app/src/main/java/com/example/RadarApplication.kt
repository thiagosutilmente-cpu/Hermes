package com.example

import android.app.Application
import android.util.Log
import com.example.data.FirebaseInitializer

class RadarApplication : Application() {
    companion object {
        private const val TAG = "RadarApplication"
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "RadarApplication onCreate - Initializing core services")
        
        // Explicitly initialize Firebase (Firestore and Auth) in application context
        FirebaseInitializer.initialize(this)
    }
}
