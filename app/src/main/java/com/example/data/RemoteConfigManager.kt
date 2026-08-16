package com.example.data

import android.util.Log
import com.example.coordinator.RadarCoordinator
import com.example.coordinator.RadarSettings
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class AppUpdateInfo(
    val latestVersionCode: Int = 1,
    val downloadUrl: String = "",
    val releaseNotes: String = "",
    val forceUpdate: Boolean = false
)

object RemoteConfigManager {
    private const val TAG = "RemoteConfigManager"

    // Fetch the latest business rules and configuration from the server
    suspend fun fetchAndApplyRemoteConfig() {
        val firestore = FirestoreManager.db ?: return
        try {
            val doc = firestore.collection("app_config").document("public").get().await()
            if (doc.exists()) {
                val data = doc.data ?: return
                
                // Parse remote configuration
                val remoteMinFare = (data["minFareValue"] as? Number)?.toDouble()
                val remoteMinValuePerKm = (data["minValuePerKm"] as? Number)?.toDouble()
                val remoteMaxPickupDist = (data["maxPickupDistanceKm"] as? Number)?.toDouble()
                val remoteMaxTotalDist = (data["maxTotalDistanceKm"] as? Number)?.toDouble()
                val remoteSpeedLimit = (data["speedLimitKmh"] as? Number)?.toFloat()
                val remoteRiskZones = data["riskZonesKeywords"] as? String

                val currentSettings = RadarCoordinator.settings.value
                var updatedSettings = currentSettings

                if (remoteMinFare != null) updatedSettings = updatedSettings.copy(minFareValue = remoteMinFare)
                if (remoteMinValuePerKm != null) updatedSettings = updatedSettings.copy(minValuePerKm = remoteMinValuePerKm)
                if (remoteMaxPickupDist != null) updatedSettings = updatedSettings.copy(maxPickupDistanceKm = remoteMaxPickupDist)
                if (remoteMaxTotalDist != null) updatedSettings = updatedSettings.copy(maxTotalDistanceKm = remoteMaxTotalDist)
                if (remoteSpeedLimit != null) updatedSettings = updatedSettings.copy(speedLimitKmh = remoteSpeedLimit)
                if (remoteRiskZones != null) updatedSettings = updatedSettings.copy(riskZonesKeywords = remoteRiskZones)

                // Only apply if it actually changed to prevent constant overwriting of user local tweaks 
                // Alternatively, could always override if remote config is marked as "forced"
                val forceRemoteConfig = data["forceRemoteConfig"] as? Boolean ?: false
                if (forceRemoteConfig && updatedSettings != currentSettings) {
                    RadarCoordinator.updateSettingsFromRemote(updatedSettings)
                    Log.d(TAG, "Applied forced remote configuration from server.")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Notice fetching remote config: ${e.localizedMessage ?: e.message}")
        }
    }

    suspend fun checkAppUpdate(): AppUpdateInfo? {
        val firestore = FirestoreManager.db ?: return null
        return try {
            val doc = firestore.collection("app_config").document("update_info").get().await()
            if (doc.exists()) {
                val data = doc.data ?: return null
                AppUpdateInfo(
                    latestVersionCode = (data["latestVersionCode"] as? Number)?.toInt() ?: 1,
                    downloadUrl = data["downloadUrl"] as? String ?: "",
                    releaseNotes = data["releaseNotes"] as? String ?: "",
                    forceUpdate = data["forceUpdate"] as? Boolean ?: false
                )
            } else null
        } catch (e: Exception) {
            Log.w(TAG, "Notice checking app update: ${e.localizedMessage ?: e.message}")
            null
        }
    }
}
