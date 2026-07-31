package com.example

import android.app.Application
import android.content.Context
import android.os.Build
import android.util.Log
import com.example.data.FirebaseInitializer

class RadarApplication : Application() {
    override fun attachBaseContext(base: android.content.Context) {
        unattributedContextInstance = base
        super.attachBaseContext(base)
    }


    companion object {
        private const val TAG = "RadarApplication"
        var unattributedContextInstance: Context? = null
            private set
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "RadarApplication onCreate - Initializing core services")
        
        // Explicitly initialize Firebase (Firestore and Auth) in application context
        FirebaseInitializer.initialize(this)
    }
}
