package com.example.data

import android.util.Log
import com.example.api.DailyReportItem
import com.example.coordinator.RadarSettings
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.android.gms.tasks.Task
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        if (task.isSuccessful) {
            continuation.resume(task.result)
        } else {
            continuation.resumeWithException(task.exception ?: RuntimeException("Task failed"))
        }
    }
}

object FirestoreManager {
    private const val TAG = "FirestoreManager"
    const val DEFAULT_RIDER_ID = "motoboy_thiago_01"

    private val db: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing FirebaseFirestore: ${e.message}")
            null
        }
    }

    // Save user preferences (RadarSettings)
    fun saveSettings(settings: RadarSettings, riderId: String = FirebaseAuthManager.getCurrentRiderId()) {
        val firestore = db ?: return
        val data = mapOf(
            "serverBaseUrl" to settings.serverBaseUrl,
            "apiToken" to settings.apiToken,
            "forceMockSpeed" to settings.forceMockSpeed,
            "mockSpeedKmh" to settings.mockSpeedKmh,
            "isActiveDeliveryEnabled" to settings.isActiveDeliveryEnabled,
            "activeDeliveryDestination" to settings.activeDeliveryDestination,
            "minValuePerKm" to settings.minValuePerKm,
            "minFareValue" to settings.minFareValue,
            "useLocalGemini" to settings.useLocalGemini,
            "geminiApiKey" to settings.geminiApiKey,
            "useHermesAgent" to settings.useHermesAgent,
            "hermesBaseUrl" to settings.hermesBaseUrl,
            "hermesApiKey" to settings.hermesApiKey,
            "riskZonesKeywords" to settings.riskZonesKeywords,
            "isDarkMode" to settings.isDarkMode,
            "isAutoRejectEnabled" to settings.isAutoRejectEnabled,
            "autoRejectMinFare" to settings.autoRejectMinFare,
            "speedLimitKmh" to settings.speedLimitKmh,
            "maxPickupDistanceKm" to settings.maxPickupDistanceKm,
            "maxTotalDistanceKm" to settings.maxTotalDistanceKm
        )

        firestore.collection("riders")
            .document(riderId)
            .collection("config")
            .document("settings")
            .set(data, SetOptions.merge())
            .addOnSuccessListener {
                Log.d(TAG, "Settings successfully saved to Firestore for $riderId")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error saving settings to Firestore", e)
            }
    }

    // Load user preferences (RadarSettings)
    suspend fun loadSettings(riderId: String = FirebaseAuthManager.getCurrentRiderId()): RadarSettings? = withContext(Dispatchers.IO) {
        val firestore = db ?: return@withContext null
        try {
            val document = firestore.collection("riders")
                .document(riderId)
                .collection("config")
                .document("settings")
                .get()
                .awaitTask()

            if (document.exists()) {
                val data = document.data ?: return@withContext null
                RadarSettings(
                    serverBaseUrl = data["serverBaseUrl"] as? String ?: "http://187.77.248.73:5000",
                    apiToken = data["apiToken"] as? String ?: "RadarDelivery2026Token",
                    forceMockSpeed = data["forceMockSpeed"] as? Boolean ?: true,
                    mockSpeedKmh = (data["mockSpeedKmh"] as? Number)?.toFloat() ?: 0.0f,
                    isActiveDeliveryEnabled = data["isActiveDeliveryEnabled"] as? Boolean ?: false,
                    activeDeliveryDestination = data["activeDeliveryDestination"] as? String ?: "Av. Paulista, 1000 - Bela Vista, São Paulo - SP",
                    minValuePerKm = (data["minValuePerKm"] as? Number)?.toDouble() ?: 2.0,
                    minFareValue = (data["minFareValue"] as? Number)?.toDouble() ?: 8.0,
                    useLocalGemini = data["useLocalGemini"] as? Boolean ?: true,
                    geminiApiKey = data["geminiApiKey"] as? String ?: "",
                    useHermesAgent = data["useHermesAgent"] as? Boolean ?: false,
                    hermesBaseUrl = data["hermesBaseUrl"] as? String ?: "https://api.nousresearch.com/v1",
                    hermesApiKey = data["hermesApiKey"] as? String ?: "",
                    riskZonesKeywords = data["riskZonesKeywords"] as? String ?: "Cracolândia, Heliópolis, Capão Redondo, Paraisópolis, Favela, Beco",
                    isDarkMode = data["isDarkMode"] as? Boolean ?: true,
                    isAutoRejectEnabled = data["isAutoRejectEnabled"] as? Boolean ?: false,
                    autoRejectMinFare = (data["autoRejectMinFare"] as? Number)?.toDouble() ?: 10.0,
                    speedLimitKmh = (data["speedLimitKmh"] as? Number)?.toFloat() ?: 10.0f,
                    maxPickupDistanceKm = (data["maxPickupDistanceKm"] as? Number)?.toDouble() ?: 5.0,
                    maxTotalDistanceKm = (data["maxTotalDistanceKm"] as? Number)?.toDouble() ?: 15.0
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading settings from Firestore: ${e.message}")
            null
        }
    }

    // Save single daily report item (History of earnings)
    fun saveDailyReport(item: DailyReportItem, riderId: String = FirebaseAuthManager.getCurrentRiderId()) {
        val firestore = db ?: return
        val docId = item.date.replace("/", "-")
        val data = mapOf(
            "date" to item.date,
            "totalOffersEvaluated" to item.totalOffersEvaluated,
            "totalOffersAccepted" to item.totalOffersAccepted,
            "totalOffersRejected" to item.totalOffersRejected,
            "totalOffersConsidered" to item.totalOffersConsidered,
            "estimatedEarnings" to item.estimatedEarnings,
            "totalDistanceKm" to item.totalDistanceKm,
            "totalTimeMin" to item.totalTimeMin,
            "averageFareValue" to item.averageFareValue,
            "earningsPerKm" to item.earningsPerKm
        )

        firestore.collection("riders")
            .document(riderId)
            .collection("daily_reports")
            .document(docId)
            .set(data, SetOptions.merge())
            .addOnSuccessListener {
                Log.d(TAG, "Daily report saved to Firestore for $riderId on ${item.date}")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error saving daily report to Firestore", e)
            }
    }

    // Save list of daily report items
    fun saveDailyReports(items: List<DailyReportItem>, riderId: String = FirebaseAuthManager.getCurrentRiderId()) {
        items.forEach { saveDailyReport(it, riderId) }
    }

    // Load daily report items (History of earnings)
    suspend fun loadDailyReports(riderId: String = FirebaseAuthManager.getCurrentRiderId()): List<DailyReportItem> = withContext(Dispatchers.IO) {
        val firestore = db ?: return@withContext emptyList()
        try {
            val querySnapshot = firestore.collection("riders")
                .document(riderId)
                .collection("daily_reports")
                .get()
                .awaitTask()

            querySnapshot.documents.mapNotNull { document ->
                val data = document.data ?: return@mapNotNull null
                DailyReportItem(
                    date = data["date"] as? String ?: document.id.replace("-", "/"),
                    totalOffersEvaluated = (data["totalOffersEvaluated"] as? Number)?.toInt() ?: 0,
                    totalOffersAccepted = (data["totalOffersAccepted"] as? Number)?.toInt() ?: 0,
                    totalOffersRejected = (data["totalOffersRejected"] as? Number)?.toInt() ?: 0,
                    totalOffersConsidered = (data["totalOffersConsidered"] as? Number)?.toInt() ?: 0,
                    estimatedEarnings = (data["estimatedEarnings"] as? Number)?.toDouble() ?: 0.0,
                    totalDistanceKm = (data["totalDistanceKm"] as? Number)?.toDouble() ?: 0.0,
                    totalTimeMin = (data["totalTimeMin"] as? Number)?.toDouble() ?: 0.0,
                    averageFareValue = (data["averageFareValue"] as? Number)?.toDouble() ?: 0.0,
                    earningsPerKm = (data["earningsPerKm"] as? Number)?.toDouble() ?: 0.0,
                    appBreakdown = null
                )
            }.sortedBy { 
                // Sort by date components for display
                val parts = it.date.split("/")
                if (parts.size == 3) {
                    "${parts[2]}-${parts[1]}-${parts[0]}"
                } else {
                    it.date
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading daily reports from Firestore: ${e.message}")
            emptyList()
        }
    }

    // Save an optimized route log (OfferEntity)
    fun saveRouteLog(offer: OfferEntity, riderId: String = FirebaseAuthManager.getCurrentRiderId()) {
        val firestore = db ?: return
        val docId = offer.timestamp.toString()
        val data = mapOf(
            "id" to offer.id,
            "appName" to offer.appName,
            "fareValue" to offer.fareValue,
            "pickupAddress" to offer.pickupAddress,
            "deliveryAddress" to offer.deliveryAddress,
            "totalDistance" to offer.totalDistance,
            "totalTime" to offer.totalTime,
            "detourDistance" to offer.detourDistance,
            "detourTime" to offer.detourTime,
            "suggestion" to offer.suggestion,
            "reason" to offer.reason,
            "timestamp" to offer.timestamp,
            "speedKmhAtDecision" to offer.speedKmhAtDecision,
            "isChained" to offer.isChained,
            "activeDeliveryDestination" to offer.activeDeliveryDestination
        )

        firestore.collection("riders")
            .document(riderId)
            .collection("offers")
            .document(docId)
            .set(data, SetOptions.merge())
            .addOnSuccessListener {
                Log.d(TAG, "Route log saved to Firestore for $riderId: app=${offer.appName} fare=${offer.fareValue}")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error saving route log to Firestore", e)
            }
    }

    // Load route logs from Firestore
    suspend fun loadRouteLogs(riderId: String = FirebaseAuthManager.getCurrentRiderId()): List<OfferEntity> = withContext(Dispatchers.IO) {
        val firestore = db ?: return@withContext emptyList()
        try {
            val querySnapshot = firestore.collection("riders")
                .document(riderId)
                .collection("offers")
                .get()
                .awaitTask()

            querySnapshot.documents.mapNotNull { document ->
                val data = document.data ?: return@mapNotNull null
                OfferEntity(
                    id = (data["id"] as? Number)?.toInt() ?: 0,
                    appName = data["appName"] as? String ?: "",
                    fareValue = (data["fareValue"] as? Number)?.toDouble() ?: 0.0,
                    pickupAddress = data["pickupAddress"] as? String ?: "",
                    deliveryAddress = data["deliveryAddress"] as? String ?: "",
                    totalDistance = (data["totalDistance"] as? Number)?.toDouble() ?: 0.0,
                    totalTime = (data["totalTime"] as? Number)?.toDouble() ?: 0.0,
                    detourDistance = (data["detourDistance"] as? Number)?.toDouble() ?: 0.0,
                    detourTime = (data["detourTime"] as? Number)?.toDouble() ?: 0.0,
                    suggestion = data["suggestion"] as? String ?: "",
                    reason = data["reason"] as? String ?: "",
                    timestamp = (data["timestamp"] as? Number)?.toLong() ?: 0L,
                    speedKmhAtDecision = (data["speedKmhAtDecision"] as? Number)?.toFloat() ?: 0.0f,
                    isChained = data["isChained"] as? Boolean ?: false,
                    activeDeliveryDestination = data["activeDeliveryDestination"] as? String
                )
            }.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading route logs from Firestore: ${e.message}")
            emptyList()
        }
    }

    // Save user profile
    fun saveUserProfile(profile: UserProfile, riderId: String = FirebaseAuthManager.getCurrentRiderId()) {
        val firestore = db ?: return
        val data = mapOf(
            "fullName" to profile.fullName,
            "phoneNumber" to profile.phoneNumber,
            "vehiclePlate" to profile.vehiclePlate,
            "notifyOnAutoReject" to profile.notifyOnAutoReject,
            "audioAlertEnabled" to profile.audioAlertEnabled,
            "voiceCommandsEnabled" to profile.voiceCommandsEnabled,
            "vibrateOnNewOffer" to profile.vibrateOnNewOffer,
            "emergencyContactName" to profile.emergencyContactName,
            "emergencyContactPhone" to profile.emergencyContactPhone
        )

        firestore.collection("riders")
            .document(riderId)
            .collection("profile")
            .document("details")
            .set(data, SetOptions.merge())
            .addOnSuccessListener {
                Log.d(TAG, "User profile saved to Firestore for $riderId")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error saving user profile to Firestore", e)
            }
    }

    // Load user profile
    suspend fun loadUserProfile(riderId: String = FirebaseAuthManager.getCurrentRiderId()): UserProfile? = withContext(Dispatchers.IO) {
        val firestore = db ?: return@withContext null
        try {
            val document = firestore.collection("riders")
                .document(riderId)
                .collection("profile")
                .document("details")
                .get()
                .awaitTask()

            if (document.exists()) {
                val data = document.data ?: return@withContext UserProfile()
                UserProfile(
                    fullName = data["fullName"] as? String ?: "",
                    phoneNumber = data["phoneNumber"] as? String ?: "",
                    vehiclePlate = data["vehiclePlate"] as? String ?: "",
                    notifyOnAutoReject = data["notifyOnAutoReject"] as? Boolean ?: true,
                    audioAlertEnabled = data["audioAlertEnabled"] as? Boolean ?: true,
                    voiceCommandsEnabled = data["voiceCommandsEnabled"] as? Boolean ?: true,
                    vibrateOnNewOffer = data["vibrateOnNewOffer"] as? Boolean ?: true,
                    emergencyContactName = data["emergencyContactName"] as? String ?: "",
                    emergencyContactPhone = data["emergencyContactPhone"] as? String ?: ""
                )
            } else {
                UserProfile() // return default profile if not set in cloud
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading user profile from Firestore: ${e.message}")
            UserProfile()
        }
    }

    // Update SOS Alert in Firestore
    fun updateSosAlert(active: Boolean, latitude: Double? = null, longitude: Double? = null, riderId: String = FirebaseAuthManager.getCurrentRiderId()) {
        val firestore = db ?: return
        val userProfile = com.example.coordinator.RadarCoordinator.userProfile.value
        
        val data = mutableMapOf<String, Any>(
            "active" to active,
            "timestamp" to System.currentTimeMillis(),
            "riderId" to riderId,
            "riderName" to userProfile.fullName,
            "riderPhone" to userProfile.phoneNumber,
            "vehiclePlate" to userProfile.vehiclePlate,
            "emergencyContactName" to userProfile.emergencyContactName,
            "emergencyContactPhone" to userProfile.emergencyContactPhone
        )
        if (latitude != null) {
            data["latitude"] = latitude
        }
        if (longitude != null) {
            data["longitude"] = longitude
        }

        firestore.collection("riders")
            .document(riderId)
            .collection("sos")
            .document("active_alert")
            .set(data, SetOptions.merge())
            .addOnSuccessListener {
                Log.d(TAG, "SOS alert state updated: active=$active to Firestore for $riderId")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error updating SOS alert state", e)
            }
    }

    // Save simple error log to Firebase
    fun logErrorToFirebase(type: String, message: String, riderId: String = FirebaseAuthManager.getCurrentRiderId()) {
        val firestore = db ?: return
        val id = java.util.UUID.randomUUID().toString()
        val data = mapOf(
            "id" to id,
            "timestamp" to System.currentTimeMillis(),
            "type" to type,
            "message" to message,
            "deviceModel" to "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
        )

        firestore.collection("riders")
            .document(riderId)
            .collection("errors")
            .document(id)
            .set(data)
            .addOnSuccessListener {
                Log.d(TAG, "Error log saved to Firebase: type=$type, msg=$message")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to save error log to Firebase", e)
            }
    }

    // Real-time listener for error logs
    fun listenToErrorLogs(riderId: String = FirebaseAuthManager.getCurrentRiderId(), onUpdate: (List<AppErrorLog>) -> Unit): ListenerRegistration? {
        val firestore = db ?: return null
        return firestore.collection("riders")
            .document(riderId)
            .collection("errors")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(10)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Error listening to error logs", e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val logs = snapshot.documents.mapNotNull { doc ->
                        val data = doc.data ?: return@mapNotNull null
                        AppErrorLog(
                            id = data["id"] as? String ?: "",
                            timestamp = data["timestamp"] as? Long ?: 0L,
                            type = data["type"] as? String ?: "",
                            message = data["message"] as? String ?: "",
                            deviceModel = data["deviceModel"] as? String ?: ""
                        )
                    }
                    onUpdate(logs)
                }
            }
    }
}

data class AppErrorLog(
    val id: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val type: String = "",
    val message: String = "",
    val deviceModel: String = ""
)

data class UserProfile(
    val fullName: String = "",
    val phoneNumber: String = "",
    val vehiclePlate: String = "",
    val notifyOnAutoReject: Boolean = true,
    val audioAlertEnabled: Boolean = true,
    val voiceCommandsEnabled: Boolean = true,
    val vibrateOnNewOffer: Boolean = true,
    val emergencyContactName: String = "",
    val emergencyContactPhone: String = ""
)
