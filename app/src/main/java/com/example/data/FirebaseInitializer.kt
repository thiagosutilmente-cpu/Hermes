package com.example.data

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

object FirebaseInitializer {
    private const val TAG = "FirebaseInitializer"

    fun initialize(context: Context) {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                // Try initializing using the standard auto-merged google-services.json details first
                try {
                    FirebaseApp.initializeApp(context)
                    Log.i(TAG, "Firebase initialized successfully with default provider options.")
                } catch (e: Exception) {
                    Log.w(TAG, "Default Firebase initialization failed: ${e.message}. Attempting custom programmatic initialization.")
                    
                    // Retrieve configuration options from BuildConfig (injected via .env by secrets plugin)
                    val apiKey = try { BuildConfig.FIREBASE_API_KEY } catch (ex: Exception) { "AIzaSyFallbackKeyForRadarDelivery2026" }
                    val projectId = try { BuildConfig.FIREBASE_PROJECT_ID } catch (ex: Exception) { "radar-delivery-2026" }
                    val appId = try { BuildConfig.FIREBASE_APPLICATION_ID } catch (ex: Exception) { "1:1234567890:android:abc123xyz" }

                    val options = FirebaseOptions.Builder()
                        .setApiKey(apiKey.ifBlank { "AIzaSyFallbackKeyForRadarDelivery2026" })
                        .setProjectId(projectId.ifBlank { "radar-delivery-2026" })
                        .setApplicationId(appId.ifBlank { "1:1234567890:android:abc123xyz" })
                        .setDatabaseUrl("https://$projectId.firebaseio.com")
                        .build()
                    
                    FirebaseApp.initializeApp(context, options)
                    Log.i(TAG, "Firebase successfully initialized programmatically with projectId=$projectId")
                }
            } else {
                Log.i(TAG, "Firebase is already initialized.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Critical error during Firebase initialization: ${e.message}", e)
        }
    }
}
