import re

app_path = 'app/src/main/java/com/example/RadarApplication.kt'
with open(app_path, 'r') as f:
    content = f.read()

replacement = """package com.example

import android.app.Application
import android.content.Context
import android.os.Build
import android.util.Log
import com.example.data.FirebaseInitializer

class RadarApplication : Application() {

    companion object {
        private const val TAG = "RadarApplication"
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                base.createAttributionContext("Radar")
            } else {
                base
            }
        )
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "RadarApplication onCreate - Initializing core services")
        
        // Explicitly initialize Firebase (Firestore and Auth) in application context
        FirebaseInitializer.initialize(this)
    }
}
"""

with open(app_path, 'w') as f:
    f.write(replacement)
