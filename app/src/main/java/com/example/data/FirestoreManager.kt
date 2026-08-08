package com.example.data

import android.util.Log
import com.example.api.DailyReportItem
import com.example.coordinator.RadarSettings
import com.example.util.ActiveOrder
import com.example.util.OrderStatus
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.FieldValue
import com.google.android.gms.tasks.Task
import kotlin.coroutines.resume
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

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

    private val _isFirestoreConnected = MutableStateFlow<Boolean>(true)
    val isFirestoreConnected: StateFlow<Boolean> = _isFirestoreConnected.asStateFlow()

    fun updateConnectionStatus(connected: Boolean) {
        if (_isFirestoreConnected.value != connected) {
            _isFirestoreConnected.value = connected
            Log.d(TAG, "Firestore connection status updated: $connected")
        }
    }

    fun startConnectionMonitor(riderId: String = FirebaseAuthManager.getCurrentRiderId()): ListenerRegistration? {
        val firestore = db ?: run {
            updateConnectionStatus(false)
            return null
        }
        return try {
            firestore.collection("riders")
                .document(riderId)
                .collection("config")
                .document("settings")
                .addSnapshotListener(com.google.firebase.firestore.MetadataChanges.INCLUDE) { snapshot, e ->
                    if (e != null) {
                        Log.e(TAG, "Firestore connection monitor error: ${e.message}")
                        updateConnectionStatus(false)
                    } else if (snapshot != null) {
                        updateConnectionStatus(!snapshot.metadata.isFromCache)
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start Firestore connection monitor", e)
            updateConnectionStatus(false)
            null
        }
    }

    val db: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing FirebaseFirestore: ${e.message}")
            null
        }
    }

    // Envia o texto de voz do usuário para o Firebase para o servidor VPS processar via OpenJarvis
    fun sendJarvisRequest(text: String, riderId: String = FirebaseAuthManager.getCurrentRiderId(), onComplete: (String) -> Unit) {
        val firestore = db
        if (firestore == null) {
            onComplete("Erro de conexão com o banco de dados.")
            return
        }

        val data = hashMapOf(
            "text" to text,
            "status" to "pending",
            "riderId" to riderId,
            "timestamp" to System.currentTimeMillis()
        )

        // Adiciona à fila root "jarvis_requests"
        val docRef = firestore.collection("jarvis_requests").document()
        docRef.set(data).addOnSuccessListener {
            // Fica escutando por atualizações (VPS vai marcar como completed e preencher response)
            var listener: ListenerRegistration? = null
            listener = docRef.addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val status = snapshot.getString("status")
                if (status == "completed" || status == "error") {
                    val response = snapshot.getString("response") ?: "Nenhuma resposta do Jarvis."
                    onComplete(response)
                    listener?.remove() // Remove listener após obter a resposta
                }
            }
        }.addOnFailureListener {
            onComplete("Falha ao enviar requisição para o servidor VPS.")
        }
    }

    fun listenToSettings(riderId: String = FirebaseAuthManager.getCurrentRiderId(), onUpdate: (RadarSettings) -> Unit): ListenerRegistration? {
        val firestore = db ?: return null
        return firestore.collection("riders")
            .document(riderId)
            .collection("config")
            .document("settings")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Error listening to settings", e)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val data = snapshot.data ?: return@addSnapshotListener
                    val settings = RadarSettings(
                        serverBaseUrl = data["serverBaseUrl"] as? String ?: "http://187.77.248.73:5000",
                        apiToken = data["apiToken"] as? String ?: "RadarDelivery2026Token",
                        forceMockSpeed = data["forceMockSpeed"] as? Boolean ?: true,
                        mockSpeedKmh = (data["mockSpeedKmh"] as? Number)?.toFloat() ?: 0.0f,
                        isActiveDeliveryEnabled = data["isActiveDeliveryEnabled"] as? Boolean ?: false,
                        activeDeliveryDestination = data["activeDeliveryDestination"] as? String ?: "",
                        minValuePerKm = (data["minValuePerKm"] as? Number)?.toDouble() ?: 2.0,
                        minFareValue = (data["minFareValue"] as? Number)?.toDouble() ?: 8.0,
                        voiceFilterEnabled = data["voiceFilterEnabled"] as? Boolean ?: false,
                        voiceFilterMinFare = (data["voiceFilterMinFare"] as? Number)?.toDouble() ?: 0.0,
                        voiceFilterMaxDistance = (data["voiceFilterMaxDistance"] as? Number)?.toDouble() ?: 999.0,
                        useLocalGemini = data["useLocalGemini"] as? Boolean ?: true,
                        geminiApiKey = data["geminiApiKey"] as? String ?: "",
                        useJarvisAgent = data["useJarvisAgent"] as? Boolean ?: (data["useHermesAgent"] as? Boolean ?: false),
                        jarvisBaseUrl = data["jarvisBaseUrl"] as? String ?: (data["hermesBaseUrl"] as? String ?: "https://api.nousresearch.com/v1"),
                        jarvisApiKey = data["jarvisApiKey"] as? String ?: (data["hermesApiKey"] as? String ?: ""),
                        riskZonesKeywords = data["riskZonesKeywords"] as? String ?: "Cracolândia, Heliópolis, Capão Redondo, Paraisópolis, Favela, Beco",
                        isDarkMode = data["isDarkMode"] as? Boolean ?: true,
                        isAutoRejectEnabled = data["isAutoRejectEnabled"] as? Boolean ?: false,
                        autoRejectMinFare = (data["autoRejectMinFare"] as? Number)?.toDouble() ?: 10.0,
                        speedLimitKmh = (data["speedLimitKmh"] as? Number)?.toFloat() ?: 10.0f,
                        maxPickupDistanceKm = (data["maxPickupDistanceKm"] as? Number)?.toDouble() ?: 5.0,
                        maxTotalDistanceKm = (data["maxTotalDistanceKm"] as? Number)?.toDouble() ?: 15.0,
                     vehicleType = data["vehicleType"] as? String ?: "MOTO",
                     fuelPrice = (data["fuelPrice"] as? Number)?.toDouble() ?: 5.80,
                     motorcycleConsumptionKmPerL = (data["motorcycleConsumptionKmPerL"] as? Number)?.toDouble() ?: 35.0,
                        rejectSupermarkets = data["rejectSupermarkets"] as? Boolean ?: false,
                        avoidStoreKeywords = data["avoidStoreKeywords"] as? String ?: "",
                        operationalOverrides = (data["operationalOverrides"] as? Map<String, String>) ?: emptyMap(),
                        minProfitPerHour = (data["minProfitPerHour"] as? Number)?.toDouble() ?: 0.0,
                        rainModeMultiplier = (data["rainModeMultiplier"] as? Number)?.toDouble() ?: 1.0,
                        maxDrops = (data["maxDrops"] as? Number)?.toInt() ?: 2,
                        headingHomeMode = data["headingHomeMode"] as? Boolean ?: false,
                        homeAddress = data["homeAddress"] as? String ?: "",
                        autoAcceptPremium = data["autoAcceptPremium"] as? Boolean ?: false,
                        autoAcceptMinPerKm = (data["autoAcceptMinPerKm"] as? Number)?.toDouble() ?: 5.0,
                        chainDeliveriesMode = data["chainDeliveriesMode"] as? Boolean ?: false,
                        voiceOnlyMode = data["voiceOnlyMode"] as? Boolean ?: false,
                        isGhostSequenceEnabled = data["isGhostSequenceEnabled"] as? Boolean ?: false,
                        ghostSequenceAggressiveness = data["ghostSequenceAggressiveness"] as? String ?: "EQUILIBRADO",
                        ghostSequenceTrafficWeight = (data["ghostSequenceTrafficWeight"] as? Number)?.toDouble() ?: 0.5,
                        ghostSequenceLatencyWeight = (data["ghostSequenceLatencyWeight"] as? Number)?.toDouble() ?: 0.3,
                        ghostEyeSensitivity = (data["ghostEyeSensitivity"] as? Number)?.toDouble() ?: 0.7,
                        ghostMinDelay = (data["ghostMinDelay"] as? Number)?.toDouble() ?: 2.0,
                        ghostMaxDelay = (data["ghostMaxDelay"] as? Number)?.toDouble() ?: 7.0,
                        dynamicJitterEnabled = data["dynamicJitterEnabled"] as? Boolean ?: true,
                        ghostPushNotificationsEnabled = data["ghostPushNotificationsEnabled"] as? Boolean ?: true,
                                                ghostMinPerKm = (data["ghostMinPerKm"] as? Number)?.toDouble() ?: 1.5,
                        systemHealthScore = (data["systemHealthScore"] as? Number)?.toInt() ?: 100,
                        activeAnomalies = (data["activeAnomalies"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                        preferredReturnNeighborhoods = data["preferredReturnNeighborhoods"] as? String ?: "",
                        showTrafficDensity = data["showTrafficDensity"] as? Boolean ?: true,
                        showTrafficOverlay = data["showTrafficOverlay"] as? Boolean ?: true,
                        jarvisOverlayMode = data["jarvisOverlayMode"] as? Boolean ?: false,
                        jarvisVoiceEngine = data["jarvisVoiceEngine"] as? String ?: "LOCAL",
                        jarvisVoiceTone = data["jarvisVoiceTone"] as? String ?: "AMIGÁVEL",
                        jarvisVoiceStyle = data["jarvisVoiceStyle"] as? String ?: "PADRAO",
                        jarvisVoicePitch = (data["jarvisVoicePitch"] as? Number)?.toFloat() ?: 1.0f,
                        jarvisVoiceRate = (data["jarvisVoiceRate"] as? Number)?.toFloat() ?: 1.0f,
                        jarvisVoiceVolume = (data["jarvisVoiceVolume"] as? Number)?.toFloat() ?: 1.0f,
                        elevenLabsApiKey = data["elevenLabsApiKey"] as? String ?: "",
                        elevenLabsVoiceId = data["elevenLabsVoiceId"] as? String ?: "ErXwobaY60C9iAWzCgEh",
                        elevenLabsModelId = data["elevenLabsModelId"] as? String ?: "eleven_multilingual_v2",
                        elevenLabsStability = (data["elevenLabsStability"] as? Number)?.toFloat() ?: 0.5f,
                        elevenLabsSimilarityBoost = (data["elevenLabsSimilarityBoost"] as? Number)?.toFloat() ?: 0.75f,
                        elevenLabsStyle = (data["elevenLabsStyle"] as? Number)?.toFloat() ?: 0.0f,
                        elevenLabsSpeakerBoost = data["elevenLabsSpeakerBoost"] as? Boolean ?: true,
                        jarvisContinuousFrequency = data["jarvisContinuousFrequency"] as? Boolean ?: true,
                        aiActiveTrafficReroute = data["aiActiveTrafficReroute"] as? Boolean ?: true,
                        aiActiveFuelSuggest = data["aiActiveFuelSuggest"] as? Boolean ?: true,
                        aiActiveFatigueDetect = data["aiActiveFatigueDetect"] as? Boolean ?: true,
                        cliqueSuperVeloz = data["cliqueSuperVeloz"] as? Boolean ?: true,
                        antiDeteccaoMilitar = data["antiDeteccaoMilitar"] as? Boolean ?: true,
                        camuflagemOverlay = data["camuflagemOverlay"] as? Boolean ?: true,
                        filterByTimeEnabled = data["filterByTimeEnabled"] as? Boolean ?: false,
                        filterStartTime = data["filterStartTime"] as? String ?: "18:00",
                        filterEndTime = data["filterEndTime"] as? String ?: "22:00",
                        highValueAlertTone = data["highValueAlertTone"] as? String ?: "bell",
                        voiceCmdAccept = data["voiceCmdAccept"] as? String ?: "aceitar",
                        voiceCmdReject = data["voiceCmdReject"] as? String ?: "recusar",
                        voiceCmdSupport = data["voiceCmdSupport"] as? String ?: "chame o suporte",
                        voiceCmdVip = data["voiceCmdVip"] as? String ?: "aceitar corrida VIP",
                        jarvisMemories = (data["jarvisMemories"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                        motorcycleMileage = (data["motorcycleMileage"] as? Number)?.toDouble() ?: 0.0,
                        nextOilChangeMileage = (data["nextOilChangeMileage"] as? Number)?.toDouble() ?: 0.0,
                        fixedCosts = (data["fixedCosts"] as? Number)?.toDouble() ?: 0.0,
                        emergencyContacts = data["emergencyContacts"] as? String ?: "190",
                        emergencyMessage = data["emergencyMessage"] as? String ?: "ALERTA S.O.S! Thiago precisa de ajuda urgente na sua rota de entregas. Localização atual: https://maps.google.com/?q={lat},{lon}",
                        defaultNavigationApp = data["defaultNavigationApp"] as? String ?: "waze",
                        jarvisVoiceState = data["jarvisVoiceState"] as? String ?: "IDLE",
                        jarvisRecognizedText = data["jarvisRecognizedText"] as? String ?: "",
                    quickReply1Cmd = data["quickReply1Cmd"] as? String ?: "cheguei",
                    quickReply1Text = data["quickReply1Text"] as? String ?: "Olá, já estou no local aguardando com o seu pedido.",
                    quickReply2Cmd = data["quickReply2Cmd"] as? String ?: "subindo",
                    quickReply2Text = data["quickReply2Text"] as? String ?: "Olá, estou subindo para entregar na sua porta.",
                    quickReply3Cmd = data["quickReply3Cmd"] as? String ?: "trânsito",
                    quickReply3Text = data["quickReply3Text"] as? String ?: "Olá, estou a caminho mas peguei um pouco de trânsito. Chego em breve." ,
                        geofenceZones = (data["geofenceZones"] as? List<Map<String, Any>>)?.map {
                            com.example.coordinator.GeofenceZone(
                                id = it["id"] as? String ?: java.util.UUID.randomUUID().toString(),
                                name = it["name"] as? String ?: "Nova Zona",
                                latitude = (it["latitude"] as? Number)?.toDouble() ?: 0.0,
                                longitude = (it["longitude"] as? Number)?.toDouble() ?: 0.0,
                                radiusMeters = (it["radiusMeters"] as? Number)?.toFloat() ?: 1000f,
                                isDangerZone = it["isDangerZone"] as? Boolean ?: false,
                                customVoiceAlert = it["customVoiceAlert"] as? String ?: "",
                                active = it["active"] as? Boolean ?: true
                            )
                        } ?: emptyList()
                    )
                    onUpdate(settings)
                }
            }
    }

    // Save active session statistics to Firestore for cross-device sync
    fun saveActiveSessionStats(
        completedCount: Int,
        totalEarnings: Double,
        totalDistanceKm: Double,
        totalTimeMinutes: Double,
        riderId: String = FirebaseAuthManager.getCurrentRiderId()
    ) {
        val firestore = db ?: return
        val data = mapOf(
            "completedCount" to completedCount,
            "totalEarnings" to totalEarnings,
            "totalDistanceKm" to totalDistanceKm,
            "totalTimeMinutes" to totalTimeMinutes,
            "lastUpdate" to System.currentTimeMillis()
        )

        firestore.collection("riders")
            .document(riderId)
            .collection("session")
            .document("active_stats")
            .set(data, SetOptions.merge())
            .addOnSuccessListener {
                Log.d(TAG, "Active session stats synced to Firestore")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error syncing active session stats", e)
            }
    }

    data class DriverSessionStats(
        val completedCount: Int = 0,
        val totalEarnings: Double = 0.0,
        val totalDistanceKm: Double = 0.0,
        val totalTimeMinutes: Double = 0.0,
        val lastUpdate: Long = 0L
    )

    // Real-time listener for active session driver analytics across devices/sessions
    fun listenToActiveSessionStats(
        riderId: String = FirebaseAuthManager.getCurrentRiderId(),
        onUpdate: (DriverSessionStats) -> Unit
    ): ListenerRegistration? {
        val firestore = db ?: return null
        return firestore.collection("riders")
            .document(riderId)
            .collection("session")
            .document("active_stats")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Error listening to active session stats", e)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val data = snapshot.data ?: return@addSnapshotListener
                    val stats = DriverSessionStats(
                        completedCount = (data["completedCount"] as? Number)?.toInt() ?: 0,
                        totalEarnings = (data["totalEarnings"] as? Number)?.toDouble() ?: 0.0,
                        totalDistanceKm = (data["totalDistanceKm"] as? Number)?.toDouble() ?: 0.0,
                        totalTimeMinutes = (data["totalTimeMinutes"] as? Number)?.toDouble() ?: 0.0,
                        lastUpdate = (data["lastUpdate"] as? Number)?.toLong() ?: System.currentTimeMillis()
                    )
                    onUpdate(stats)
                }
            }
    }

    data class SystemPulseData(
        val systemHealthScore: Int = 100,
        val activeAnomalies: List<String> = emptyList(),
        val lastPulse: Long = 0L
    )

    // Real-time listener for system pulse and anomalies
    fun listenToSystemPulse(
        riderId: String = FirebaseAuthManager.getCurrentRiderId(),
        onUpdate: (SystemPulseData) -> Unit
    ): ListenerRegistration? {
        val firestore = db ?: return null
        return firestore.collection("riders")
            .document(riderId)
            .collection("pulse")
            .document("current")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Error listening to system pulse", e)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val data = snapshot.data ?: return@addSnapshotListener
                    val pulse = SystemPulseData(
                        systemHealthScore = (data["systemHealthScore"] as? Number)?.toInt() ?: 100,
                        activeAnomalies = (data["activeAnomalies"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                        lastPulse = (data["lastPulse"] as? Number)?.toLong() ?: System.currentTimeMillis()
                    )
                    onUpdate(pulse)
                }
            }
    }

    // Send real-time system health pulse
    fun uploadSystemPulse(score: Int, anomalies: List<String>, riderId: String = FirebaseAuthManager.getCurrentRiderId()) {
        val firestore = db ?: return
        val data = mapOf(
            "systemHealthScore" to score,
            "activeAnomalies" to anomalies,
            "lastPulse" to System.currentTimeMillis()
        )

        firestore.collection("riders")
            .document(riderId)
            .collection("pulse")
            .document("current")
            .set(data, SetOptions.merge())
            .addOnFailureListener { Log.e(TAG, "Error saving system pulse", it) }
    }

    fun uploadJarvisVoiceState(state: String, recognizedText: String, riderId: String = FirebaseAuthManager.getCurrentRiderId()) {
        val firestore = db ?: return
        val data = mapOf(
            "jarvisVoiceState" to state,
            "jarvisRecognizedText" to recognizedText,
            "lastVoiceUpdate" to System.currentTimeMillis()
        )
        firestore.collection("riders")
            .document(riderId)
            .collection("pulse")
            .document("current")
            .set(data, SetOptions.merge())
            .addOnFailureListener { Log.e(TAG, "Error saving jarvis voice state", it) }
    }

    fun saveGenericDoc(path: String, data: Map<String, Any>) {
        val firestore = db ?: return
        firestore.document(path).set(data, SetOptions.merge())
            .addOnFailureListener { Log.e(TAG, "Error saving generic doc to $path", it) }
    }

    // Listen for remote commands to trigger specific app actions (like pinging or refreshing)
    fun listenToRemoteCommands(riderId: String = FirebaseAuthManager.getCurrentRiderId(), onCommand: (String) -> Unit): ListenerRegistration? {
        val firestore = db ?: return null
        return firestore.collection("riders")
            .document(riderId)
            .collection("commands")
            .document("latest")
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                val command = snapshot.getString("action") ?: ""
                val status = snapshot.getString("status") ?: ""
                if (status == "pending" && command.isNotEmpty()) {
                    firestore.collection("riders").document(riderId).collection("commands").document("latest")
                        .update("status", "received")
                    onCommand(command)
                }
            }
    }

    // Listen for real-time neural patches to update app logic dynamically
    fun listenToPatches(onPatchReceived: (String, Map<String, Any>) -> Unit): ListenerRegistration? {
        val firestore = db ?: return null
        return firestore.collection("patches")
            .whereEqualTo("applied", false)
            .addSnapshotListener { snapshots, e ->
                if (e != null || snapshots == null) {
                    Log.e(TAG, "Listen failed.", e)
                    return@addSnapshotListener
                }

                for (doc in snapshots) {
                    val patchId = doc.id
                    val data = doc.data
                    onPatchReceived(patchId, data)
                    // Mark as applied
                    firestore.collection("patches").document(patchId).update("applied", true)
                }
            }
    }

    fun savePatch(key: String, value: Any) {
        val firestore = db ?: return
        val data = mapOf(
            "key" to key,
            "value" to value,
            "applied" to false,
            "timestamp" to com.google.firebase.Timestamp.now()
        )
        firestore.collection("patches").add(data)
    }

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
            "voiceFilterEnabled" to settings.voiceFilterEnabled,
            "voiceFilterMinFare" to settings.voiceFilterMinFare,
            "voiceFilterMaxDistance" to settings.voiceFilterMaxDistance,
            "useLocalGemini" to settings.useLocalGemini,
            "geminiApiKey" to settings.geminiApiKey,
            "useJarvisAgent" to settings.useJarvisAgent,
            "useHermesAgent" to settings.useJarvisAgent,
            "jarvisBaseUrl" to settings.jarvisBaseUrl,
            "hermesBaseUrl" to settings.jarvisBaseUrl,
            "jarvisApiKey" to settings.jarvisApiKey,
            "hermesApiKey" to settings.jarvisApiKey,
            "riskZonesKeywords" to settings.riskZonesKeywords,
            "isDarkMode" to settings.isDarkMode,
            "isAutoRejectEnabled" to settings.isAutoRejectEnabled,
            "autoRejectMinFare" to settings.autoRejectMinFare,
            "speedLimitKmh" to settings.speedLimitKmh,
            "maxPickupDistanceKm" to settings.maxPickupDistanceKm,
            "maxTotalDistanceKm" to settings.maxTotalDistanceKm,
            "vehicleType" to settings.vehicleType,
            "fuelPrice" to settings.fuelPrice,
            "motorcycleConsumptionKmPerL" to settings.motorcycleConsumptionKmPerL,
            "rejectSupermarkets" to settings.rejectSupermarkets,
            "avoidStoreKeywords" to settings.avoidStoreKeywords,
            "operationalOverrides" to settings.operationalOverrides,
            "minProfitPerHour" to settings.minProfitPerHour,
            "rainModeMultiplier" to settings.rainModeMultiplier,
            "maxDrops" to settings.maxDrops,
            "headingHomeMode" to settings.headingHomeMode,
            "homeAddress" to settings.homeAddress,
            "autoAcceptPremium" to settings.autoAcceptPremium,
            "autoAcceptMinPerKm" to settings.autoAcceptMinPerKm,
            "chainDeliveriesMode" to settings.chainDeliveriesMode,
            "voiceOnlyMode" to settings.voiceOnlyMode,
            "isGhostSequenceEnabled" to settings.isGhostSequenceEnabled,
            "ghostSequenceAggressiveness" to settings.ghostSequenceAggressiveness,
            "ghostSequenceTrafficWeight" to settings.ghostSequenceTrafficWeight,
            "ghostSequenceLatencyWeight" to settings.ghostSequenceLatencyWeight,
            "ghostEyeSensitivity" to settings.ghostEyeSensitivity,
            "ghostMinDelay" to settings.ghostMinDelay,
            "ghostMaxDelay" to settings.ghostMaxDelay,
            "dynamicJitterEnabled" to settings.dynamicJitterEnabled,
            "ghostPushNotificationsEnabled" to settings.ghostPushNotificationsEnabled,
                        "ghostMinPerKm" to settings.ghostMinPerKm,
            "systemHealthScore" to settings.systemHealthScore,
            "activeAnomalies" to settings.activeAnomalies,
            "preferredReturnNeighborhoods" to settings.preferredReturnNeighborhoods,
            "showTrafficDensity" to settings.showTrafficDensity,
            "showTrafficOverlay" to settings.showTrafficOverlay,
            "jarvisOverlayMode" to settings.jarvisOverlayMode,
            "jarvisVoiceEngine" to settings.jarvisVoiceEngine,
            "jarvisVoiceTone" to settings.jarvisVoiceTone,
            "jarvisVoiceStyle" to settings.jarvisVoiceStyle,
            "jarvisVoicePitch" to settings.jarvisVoicePitch,
            "jarvisVoiceRate" to settings.jarvisVoiceRate,
            "jarvisVoiceVolume" to settings.jarvisVoiceVolume,
            "elevenLabsApiKey" to settings.elevenLabsApiKey,
            "elevenLabsVoiceId" to settings.elevenLabsVoiceId,
            "elevenLabsModelId" to settings.elevenLabsModelId,
            "elevenLabsStability" to settings.elevenLabsStability,
            "elevenLabsSimilarityBoost" to settings.elevenLabsSimilarityBoost,
            "elevenLabsStyle" to settings.elevenLabsStyle,
            "elevenLabsSpeakerBoost" to settings.elevenLabsSpeakerBoost,
            "jarvisContinuousFrequency" to settings.jarvisContinuousFrequency,
            "aiActiveTrafficReroute" to settings.aiActiveTrafficReroute,
            "aiActiveFuelSuggest" to settings.aiActiveFuelSuggest,
            "aiActiveFatigueDetect" to settings.aiActiveFatigueDetect,
            "cliqueSuperVeloz" to settings.cliqueSuperVeloz,
            "antiDeteccaoMilitar" to settings.antiDeteccaoMilitar,
            "camuflagemOverlay" to settings.camuflagemOverlay,
            "filterByTimeEnabled" to settings.filterByTimeEnabled,
            "filterStartTime" to settings.filterStartTime,
            "filterEndTime" to settings.filterEndTime,
            "highValueAlertTone" to settings.highValueAlertTone,
            "voiceCmdAccept" to settings.voiceCmdAccept,
            "voiceCmdReject" to settings.voiceCmdReject,
            "voiceCmdSupport" to settings.voiceCmdSupport,
            "voiceCmdVip" to settings.voiceCmdVip,
            "jarvisMemories" to settings.jarvisMemories,
            "motorcycleMileage" to settings.motorcycleMileage,
            "nextOilChangeMileage" to settings.nextOilChangeMileage,
            "fixedCosts" to settings.fixedCosts,
            "emergencyContacts" to settings.emergencyContacts,
            "emergencyMessage" to settings.emergencyMessage,
            "defaultNavigationApp" to settings.defaultNavigationApp,
            "jarvisVoiceState" to settings.jarvisVoiceState,
            "jarvisRecognizedText" to settings.jarvisRecognizedText,
            "quickReply1Cmd" to settings.quickReply1Cmd,
            "quickReply1Text" to settings.quickReply1Text,
            "quickReply2Cmd" to settings.quickReply2Cmd,
            "quickReply2Text" to settings.quickReply2Text,
            "quickReply3Cmd" to settings.quickReply3Cmd,
            "quickReply3Text" to settings.quickReply3Text,
            "geofenceZones" to settings.geofenceZones.map {
                mapOf(
                    "id" to it.id,
                    "name" to it.name,
                    "latitude" to it.latitude,
                    "longitude" to it.longitude,
                    "radiusMeters" to it.radiusMeters,
                    "isDangerZone" to it.isDangerZone,
                    "customVoiceAlert" to it.customVoiceAlert,
                    "active" to it.active
                )
            }
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
                    voiceFilterEnabled = data["voiceFilterEnabled"] as? Boolean ?: false,
                    voiceFilterMinFare = (data["voiceFilterMinFare"] as? Number)?.toDouble() ?: 0.0,
                    voiceFilterMaxDistance = (data["voiceFilterMaxDistance"] as? Number)?.toDouble() ?: 999.0,
                    useLocalGemini = data["useLocalGemini"] as? Boolean ?: true,
                    geminiApiKey = data["geminiApiKey"] as? String ?: "",
                    useJarvisAgent = data["useJarvisAgent"] as? Boolean ?: (data["useHermesAgent"] as? Boolean ?: false),
                    jarvisBaseUrl = data["jarvisBaseUrl"] as? String ?: (data["hermesBaseUrl"] as? String ?: "https://api.nousresearch.com/v1"),
                    jarvisApiKey = data["jarvisApiKey"] as? String ?: (data["hermesApiKey"] as? String ?: ""),
                    riskZonesKeywords = data["riskZonesKeywords"] as? String ?: "Cracolândia, Heliópolis, Capão Redondo, Paraisópolis, Favela, Beco",
                    isDarkMode = data["isDarkMode"] as? Boolean ?: true,
                    isAutoRejectEnabled = data["isAutoRejectEnabled"] as? Boolean ?: false,
                    autoRejectMinFare = (data["autoRejectMinFare"] as? Number)?.toDouble() ?: 10.0,
                    speedLimitKmh = (data["speedLimitKmh"] as? Number)?.toFloat() ?: 10.0f,
                    maxPickupDistanceKm = (data["maxPickupDistanceKm"] as? Number)?.toDouble() ?: 5.0,
                    maxTotalDistanceKm = (data["maxTotalDistanceKm"] as? Number)?.toDouble() ?: 15.0,
                    rejectSupermarkets = data["rejectSupermarkets"] as? Boolean ?: false,
                    avoidStoreKeywords = data["avoidStoreKeywords"] as? String ?: "",
                    minProfitPerHour = (data["minProfitPerHour"] as? Number)?.toDouble() ?: 0.0,
                    rainModeMultiplier = (data["rainModeMultiplier"] as? Number)?.toDouble() ?: 1.0,
                    maxDrops = (data["maxDrops"] as? Number)?.toInt() ?: 2,
                    headingHomeMode = data["headingHomeMode"] as? Boolean ?: false,
                    homeAddress = data["homeAddress"] as? String ?: "",
                    autoAcceptPremium = data["autoAcceptPremium"] as? Boolean ?: false,
                    autoAcceptMinPerKm = (data["autoAcceptMinPerKm"] as? Number)?.toDouble() ?: 5.0,
                    chainDeliveriesMode = data["chainDeliveriesMode"] as? Boolean ?: false,
                    voiceOnlyMode = data["voiceOnlyMode"] as? Boolean ?: false,
                    isGhostSequenceEnabled = data["isGhostSequenceEnabled"] as? Boolean ?: false,
                    ghostSequenceAggressiveness = data["ghostSequenceAggressiveness"] as? String ?: "EQUILIBRADO",
                    ghostSequenceTrafficWeight = (data["ghostSequenceTrafficWeight"] as? Number)?.toDouble() ?: 0.5,
                    ghostSequenceLatencyWeight = (data["ghostSequenceLatencyWeight"] as? Number)?.toDouble() ?: 0.3,
                    ghostEyeSensitivity = (data["ghostEyeSensitivity"] as? Number)?.toDouble() ?: 0.7,
                    ghostMinDelay = (data["ghostMinDelay"] as? Number)?.toDouble() ?: 2.0,
                    ghostMaxDelay = (data["ghostMaxDelay"] as? Number)?.toDouble() ?: 7.0,
                        dynamicJitterEnabled = data["dynamicJitterEnabled"] as? Boolean ?: true,
                        ghostPushNotificationsEnabled = data["ghostPushNotificationsEnabled"] as? Boolean ?: true,
                                            ghostMinPerKm = (data["ghostMinPerKm"] as? Number)?.toDouble() ?: 1.5,
                    systemHealthScore = (data["systemHealthScore"] as? Number)?.toInt() ?: 100,
                    activeAnomalies = (data["activeAnomalies"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                    preferredReturnNeighborhoods = data["preferredReturnNeighborhoods"] as? String ?: "",
                    jarvisOverlayMode = data["jarvisOverlayMode"] as? Boolean ?: false,
                    jarvisVoiceEngine = data["jarvisVoiceEngine"] as? String ?: "LOCAL",
                    jarvisVoiceTone = data["jarvisVoiceTone"] as? String ?: "AMIGÁVEL",
                    jarvisVoiceStyle = data["jarvisVoiceStyle"] as? String ?: "PADRAO",
                    jarvisVoicePitch = (data["jarvisVoicePitch"] as? Number)?.toFloat() ?: 1.0f,
                    jarvisVoiceRate = (data["jarvisVoiceRate"] as? Number)?.toFloat() ?: 1.0f,
                    jarvisVoiceVolume = (data["jarvisVoiceVolume"] as? Number)?.toFloat() ?: 1.0f,
                    elevenLabsApiKey = data["elevenLabsApiKey"] as? String ?: "",
                    elevenLabsVoiceId = data["elevenLabsVoiceId"] as? String ?: "ErXwobaY60C9iAWzCgEh",
                    elevenLabsModelId = data["elevenLabsModelId"] as? String ?: "eleven_multilingual_v2",
                    elevenLabsStability = (data["elevenLabsStability"] as? Number)?.toFloat() ?: 0.5f,
                    elevenLabsSimilarityBoost = (data["elevenLabsSimilarityBoost"] as? Number)?.toFloat() ?: 0.75f,
                    elevenLabsStyle = (data["elevenLabsStyle"] as? Number)?.toFloat() ?: 0.0f,
                    elevenLabsSpeakerBoost = data["elevenLabsSpeakerBoost"] as? Boolean ?: true,
                    jarvisContinuousFrequency = data["jarvisContinuousFrequency"] as? Boolean ?: true,
                    aiActiveTrafficReroute = data["aiActiveTrafficReroute"] as? Boolean ?: true,
                    aiActiveFuelSuggest = data["aiActiveFuelSuggest"] as? Boolean ?: true,
                    aiActiveFatigueDetect = data["aiActiveFatigueDetect"] as? Boolean ?: true,
                    cliqueSuperVeloz = data["cliqueSuperVeloz"] as? Boolean ?: true,
                    antiDeteccaoMilitar = data["antiDeteccaoMilitar"] as? Boolean ?: true,
                    camuflagemOverlay = data["camuflagemOverlay"] as? Boolean ?: true,
                    filterByTimeEnabled = data["filterByTimeEnabled"] as? Boolean ?: false,
                    filterStartTime = data["filterStartTime"] as? String ?: "18:00",
                    filterEndTime = data["filterEndTime"] as? String ?: "22:00",
                    highValueAlertTone = data["highValueAlertTone"] as? String ?: "bell",
                    voiceCmdAccept = data["voiceCmdAccept"] as? String ?: "aceitar",
                    voiceCmdReject = data["voiceCmdReject"] as? String ?: "recusar",
                    voiceCmdSupport = data["voiceCmdSupport"] as? String ?: "chame o suporte",
                    voiceCmdVip = data["voiceCmdVip"] as? String ?: "aceitar corrida VIP",
                    jarvisMemories = (data["jarvisMemories"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                    motorcycleMileage = (data["motorcycleMileage"] as? Number)?.toDouble() ?: 0.0,
                    nextOilChangeMileage = (data["nextOilChangeMileage"] as? Number)?.toDouble() ?: 0.0,
                    fixedCosts = (data["fixedCosts"] as? Number)?.toDouble() ?: 0.0,
                    emergencyContacts = data["emergencyContacts"] as? String ?: "190",
                    emergencyMessage = data["emergencyMessage"] as? String ?: "ALERTA S.O.S! Thiago precisa de ajuda urgente na sua rota de entregas. Localização atual: https://maps.google.com/?q={lat},{lon}",
                    defaultNavigationApp = data["defaultNavigationApp"] as? String ?: "waze",
                    jarvisVoiceState = data["jarvisVoiceState"] as? String ?: "IDLE",
                    jarvisRecognizedText = data["jarvisRecognizedText"] as? String ?: "",
                    quickReply1Cmd = data["quickReply1Cmd"] as? String ?: "cheguei",
                    quickReply1Text = data["quickReply1Text"] as? String ?: "Olá, já estou no local aguardando com o seu pedido.",
                    quickReply2Cmd = data["quickReply2Cmd"] as? String ?: "subindo",
                    quickReply2Text = data["quickReply2Text"] as? String ?: "Olá, estou subindo para entregar na sua porta.",
                    quickReply3Cmd = data["quickReply3Cmd"] as? String ?: "trânsito",
                    quickReply3Text = data["quickReply3Text"] as? String ?: "Olá, estou a caminho mas peguei um pouco de trânsito. Chego em breve." 
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

    // Real-time listener for earnings history (daily reports)
    fun listenToDailyReports(
        riderId: String = FirebaseAuthManager.getCurrentRiderId(),
        onUpdate: (List<DailyReportItem>) -> Unit
    ): ListenerRegistration? {
        val firestore = db ?: return null
        return firestore.collection("riders")
            .document(riderId)
            .collection("daily_reports")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Error listening to daily reports", e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val reports = snapshot.documents.mapNotNull { document ->
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
                        val parts = it.date.split("/")
                        if (parts.size == 3) "${parts[2]}-${parts[1]}-${parts[0]}" else it.date
                    }
                    onUpdate(reports)
                }
            }
    }

    // Save active order status to Firestore in real-time
    fun saveActiveOrder(
        order: ActiveOrder,
        riderId: String = FirebaseAuthManager.getCurrentRiderId()
    ) {
        val firestore = db ?: return
        val data = mapOf(
            "id" to order.id,
            "appName" to order.appName,
            "fare" to order.fare,
            "pickupAddress" to order.pickupAddress,
            "deliveryAddress" to order.deliveryAddress,
            "pickupLat" to order.pickupLat,
            "pickupLng" to order.pickupLng,
            "deliveryLat" to order.deliveryLat,
            "deliveryLng" to order.deliveryLng,
            "status" to order.status.name,
            "timestamp" to order.timestamp,
            "lastUpdate" to System.currentTimeMillis()
        )
        firestore.collection("riders")
            .document(riderId)
            .collection("active_orders")
            .document(order.id)
            .set(data, SetOptions.merge())
            .addOnSuccessListener {
                Log.d(TAG, "Active order ${order.id} saved to Firestore (status=${order.status})")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error saving active order to Firestore", e)
            }
    }

    // Update real-time status of an active order in Firestore
    fun updateActiveOrderStatus(
        orderId: String,
        status: OrderStatus,
        riderId: String = FirebaseAuthManager.getCurrentRiderId()
    ) {
        val firestore = db ?: return
        val data = mapOf(
            "status" to status.name,
            "lastUpdate" to System.currentTimeMillis()
        )
        firestore.collection("riders")
            .document(riderId)
            .collection("active_orders")
            .document(orderId)
            .set(data, SetOptions.merge())
            .addOnSuccessListener {
                Log.d(TAG, "Order $orderId status updated to ${status.name} in Firestore")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error updating order status in Firestore", e)
            }
    }

    // Remove active order from Firestore upon completion/cancellation
    fun removeActiveOrder(
        orderId: String,
        riderId: String = FirebaseAuthManager.getCurrentRiderId()
    ) {
        val firestore = db ?: return
        firestore.collection("riders")
            .document(riderId)
            .collection("active_orders")
            .document(orderId)
            .delete()
            .addOnSuccessListener {
                Log.d(TAG, "Active order $orderId deleted from Firestore")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error removing active order from Firestore", e)
            }
    }

    // Real-time listener for active orders and statuses
    fun listenToActiveOrders(
        riderId: String = FirebaseAuthManager.getCurrentRiderId(),
        onUpdate: (List<ActiveOrder>) -> Unit
    ): ListenerRegistration? {
        val firestore = db ?: return null
        return firestore.collection("riders")
            .document(riderId)
            .collection("active_orders")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Error listening to active orders", e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val orders = snapshot.documents.mapNotNull { doc ->
                        val data = doc.data ?: return@mapNotNull null
                        val statusStr = data["status"] as? String ?: "PICKING_UP"
                        val statusEnum = try {
                            OrderStatus.valueOf(statusStr)
                        } catch (ex: Exception) {
                            OrderStatus.PICKING_UP
                        }
                        ActiveOrder(
                            id = data["id"] as? String ?: doc.id,
                            appName = data["appName"] as? String ?: "App",
                            fare = (data["fare"] as? Number)?.toDouble() ?: 0.0,
                            pickupAddress = data["pickupAddress"] as? String ?: "",
                            deliveryAddress = data["deliveryAddress"] as? String ?: "",
                            pickupLat = (data["pickupLat"] as? Number)?.toDouble() ?: 0.0,
                            pickupLng = (data["pickupLng"] as? Number)?.toDouble() ?: 0.0,
                            deliveryLat = (data["deliveryLat"] as? Number)?.toDouble() ?: 0.0,
                            deliveryLng = (data["deliveryLng"] as? Number)?.toDouble() ?: 0.0,
                            status = statusEnum,
                            timestamp = (data["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
                        )
                    }
                    onUpdate(orders)
                }
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

    // Real-time service listener for the 'pedidos' collection in Firestore
    fun listenToPedidosCollection(
        riderId: String = FirebaseAuthManager.getCurrentRiderId(),
        onPedidosUpdate: (List<OfferEntity>) -> Unit
    ): ListenerRegistration? {
        val firestore = db ?: return null
        return firestore.collection("pedidos")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Error listening to 'pedidos' collection in Firestore", e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val pendingOrders = snapshot.documents.mapNotNull { doc ->
                        val data = doc.data ?: return@mapNotNull null
                        val status = data["status"] as? String ?: "PENDING"
                        if (status.equals("ACCEPTED", ignoreCase = true) ||
                            status.equals("CANCELLED", ignoreCase = true) ||
                            status.equals("COMPLETED", ignoreCase = true)
                        ) {
                            return@mapNotNull null
                        }
                        OfferEntity(
                            id = (data["id"] as? Number)?.toInt() ?: Math.abs(doc.id.hashCode()),
                            appName = data["appName"] as? String ?: data["app"] as? String ?: "iFood",
                            fareValue = (data["valor"] as? Number)?.toDouble() ?: (data["fareValue"] as? Number)?.toDouble() ?: 15.0,
                            pickupAddress = data["origem"] as? String ?: data["pickupAddress"] as? String ?: "Coleta",
                            deliveryAddress = data["destino"] as? String ?: data["deliveryAddress"] as? String ?: "Entrega",
                            totalDistance = (data["distanciaKm"] as? Number)?.toDouble() ?: (data["totalDistance"] as? Number)?.toDouble() ?: 3.5,
                            totalTime = (data["tempoMin"] as? Number)?.toDouble() ?: (data["totalTime"] as? Number)?.toDouble() ?: 15.0,
                            detourDistance = (data["desvioKm"] as? Number)?.toDouble() ?: 0.0,
                            detourTime = (data["desvioMin"] as? Number)?.toDouble() ?: 0.0,
                            suggestion = data["sugestao"] as? String ?: "aceitar",
                            reason = data["motivo"] as? String ?: "Oferta em tempo real",
                            timestamp = (data["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                            speedKmhAtDecision = 0.0f,
                            isChained = (data["isChained"] as? Boolean) ?: false,
                            activeDeliveryDestination = doc.id // Store document ID in activeDeliveryDestination for status updates
                        )
                    }
                    Log.d(TAG, "'pedidos' collection updated in real-time. Received ${pendingOrders.size} active offers")
                    onPedidosUpdate(pendingOrders)
                }
            }
    }

    // Save a new offer/order to the 'pedidos' collection in Firestore
    fun savePedidoToFirestore(offer: OfferEntity) {
        val firestore = db ?: return
        val docId = if (offer.id != 0) offer.id.toString() else "pedido_${System.currentTimeMillis()}"
        val pedidoMap = hashMapOf<String, Any>(
            "id" to offer.id,
            "appName" to offer.appName,
            "app" to offer.appName,
            "valor" to offer.fareValue,
            "fareValue" to offer.fareValue,
            "origem" to offer.pickupAddress,
            "pickupAddress" to offer.pickupAddress,
            "destino" to offer.deliveryAddress,
            "deliveryAddress" to offer.deliveryAddress,
            "distanciaKm" to offer.totalDistance,
            "totalDistance" to offer.totalDistance,
            "tempoMin" to offer.totalTime,
            "totalTime" to offer.totalTime,
            "desvioKm" to offer.detourDistance,
            "desvioMin" to offer.detourTime,
            "sugestao" to offer.suggestion,
            "motivo" to offer.reason,
            "status" to "PENDING",
            "isChained" to offer.isChained,
            "timestamp" to offer.timestamp
        )
        firestore.collection("pedidos")
            .document(docId)
            .set(pedidoMap, SetOptions.merge())
            .addOnSuccessListener {
                Log.d(TAG, "Pedido $docId saved successfully to Firestore 'pedidos' collection")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error saving pedido $docId to Firestore", e)
            }
    }

    // Record a security audit log in the 'audit_logs' collection in Firestore
    fun recordAuditLog(
        orderId: String,
        action: String,
        previousStatus: String,
        newStatus: String,
        actorId: String = "driver_system",
        details: String = ""
    ) {
        val firestore = db ?: return
        val timestamp = System.currentTimeMillis()
        val formattedTime = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(timestamp))
        val auditMap = mapOf(
            "orderId" to orderId,
            "action" to action,
            "previousStatus" to previousStatus,
            "newStatus" to newStatus,
            "actorId" to actorId,
            "details" to details,
            "timestamp" to timestamp,
            "formattedTime" to formattedTime,
            "securityLevel" to "CRITICAL_STATUS_CHANGE"
        )
        firestore.collection("audit_logs")
            .add(auditMap)
            .addOnSuccessListener { ref ->
                Log.d(TAG, "🔒 Audit log recorded for order $orderId (Doc ID: ${ref.id}) in 'audit_logs'")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error recording audit log to 'audit_logs' collection in Firestore", e)
            }
    }

    // Update status of a document in the 'pedidos' collection with automatic audit log recording
    fun updatePedidoStatusInFirestore(docId: String, status: String, previousStatus: String = "PENDING") {
        val firestore = db ?: return
        firestore.collection("pedidos")
            .document(docId)
            .update("status", status)
            .addOnSuccessListener {
                Log.d(TAG, "Pedido $docId status updated to $status in Firestore")
                recordAuditLog(
                    orderId = docId,
                    action = "ORDER_STATUS_CHANGED",
                    previousStatus = previousStatus,
                    newStatus = status,
                    details = "Status do pedido $docId alterado para $status"
                )
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error updating status for pedido $docId in Firestore", e)
            }
    }

    // Seed initial test pedidos into Firestore if the 'pedidos' collection is empty
    fun seedPedidosIfEmpty() {
        val firestore = db ?: return
        firestore.collection("pedidos")
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot == null || snapshot.isEmpty) {
                    Log.d(TAG, "'pedidos' collection is empty in Firestore. Seeding initial test orders.")
                    val seed1 = OfferEntity(
                        id = 101,
                        appName = "iFood + Rappi",
                        fareValue = 33.0,
                        pickupAddress = "Burger King → Pizza Hut",
                        deliveryAddress = "Av. Paulista → Consolação",
                        totalDistance = 4.2,
                        totalTime = 18.0,
                        detourDistance = 0.5,
                        detourTime = 2.0,
                        suggestion = "aceitar",
                        reason = "Stack Multi-App de Alta Rentabilidade (R$ 7.86/km)",
                        timestamp = System.currentTimeMillis(),
                        isChained = true
                    )
                    val seed2 = OfferEntity(
                        id = 102,
                        appName = "iFood",
                        fareValue = 15.0,
                        pickupAddress = "McDonald's Pinheiros",
                        deliveryAddress = "Rua Oscar Freire, 1200",
                        totalDistance = 2.8,
                        totalTime = 12.0,
                        detourDistance = 0.0,
                        detourTime = 0.0,
                        suggestion = "aceitar",
                        reason = "Ganho R$ 5,35/km - Rota Curta e Segura",
                        timestamp = System.currentTimeMillis(),
                        isChained = false
                    )
                    val seed3 = OfferEntity(
                        id = 103,
                        appName = "Rappi",
                        fareValue = 18.0,
                        pickupAddress = "Habib's Rebouças",
                        deliveryAddress = "Av. Rebouças, 2500",
                        totalDistance = 3.5,
                        totalTime = 15.0,
                        detourDistance = 0.0,
                        detourTime = 0.0,
                        suggestion = "aceitar",
                        reason = "Valor dentro da meta operacional",
                        timestamp = System.currentTimeMillis(),
                        isChained = false
                    )
                    savePedidoToFirestore(seed1)
                    savePedidoToFirestore(seed2)
                    savePedidoToFirestore(seed3)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error checking 'pedidos' collection for seeding: ${e.message}")
            }
    }

    // Save user profile
    fun addCloudLog(message: String, type: String) {
        val riderId = FirebaseAuthManager.getCurrentRiderId()
        if (riderId.isEmpty()) return
        val firestore = db ?: return

        val logData = hashMapOf(
            "message" to message,
            "type" to type,
            "timestamp" to FieldValue.serverTimestamp()
        )
        
        firestore.collection("riders")
            .document(riderId)
            .collection("logs")
            .add(logData)
            .addOnFailureListener { Log.e(TAG, "Error pushing cloud log", it) }
    }

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
            "emergencyContactPhone" to profile.emergencyContactPhone,
            "driverXP" to profile.driverXP,
            "driverCombo" to profile.driverCombo
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
                    emergencyContactPhone = data["emergencyContactPhone"] as? String ?: "",
                    driverXP = (data["driverXP"] as? Number)?.toInt() ?: 120,
                    driverCombo = (data["driverCombo"] as? Number)?.toInt() ?: 0
                )
            } else {
                UserProfile() // return default profile if not set in cloud
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading user profile from Firestore: ${e.message}")
            UserProfile()
        }
    }

    fun listenToUserProfile(riderId: String = FirebaseAuthManager.getCurrentRiderId(), onUpdate: (UserProfile) -> Unit): ListenerRegistration? {
        val firestore = db ?: return null
        return firestore.collection("riders")
            .document(riderId)
            .collection("profile")
            .document("details")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Error listening to user profile", e)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val data = snapshot.data ?: return@addSnapshotListener
                    val profile = UserProfile(
                        fullName = data["fullName"] as? String ?: "",
                        phoneNumber = data["phoneNumber"] as? String ?: "",
                        vehiclePlate = data["vehiclePlate"] as? String ?: "",
                        notifyOnAutoReject = data["notifyOnAutoReject"] as? Boolean ?: true,
                        audioAlertEnabled = data["audioAlertEnabled"] as? Boolean ?: true,
                        voiceCommandsEnabled = data["voiceCommandsEnabled"] as? Boolean ?: true,
                        vibrateOnNewOffer = data["vibrateOnNewOffer"] as? Boolean ?: true,
                        emergencyContactName = data["emergencyContactName"] as? String ?: "",
                        emergencyContactPhone = data["emergencyContactPhone"] as? String ?: "",
                        driverXP = (data["driverXP"] as? Number)?.toInt() ?: 120,
                        driverCombo = (data["driverCombo"] as? Number)?.toInt() ?: 0
                    )
                    onUpdate(profile)
                }
            }
    }

    // Update SOS Alert in Firestore
    fun updateSosAlert(active: Boolean, latitude: Double? = null, longitude: Double? = null, message: String? = null, contacts: String? = null, riderId: String = FirebaseAuthManager.getCurrentRiderId()) {
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
        if (message != null) {
            data["message"] = message
        }
        if (contacts != null) {
            data["contacts"] = contacts
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

    // Save user voice feedback to Firestore
    fun saveUserFeedback(feedback: UserFeedback, riderId: String = FirebaseAuthManager.getCurrentRiderId()) {
        val firestore = db ?: return
        val docId = feedback.id.ifEmpty { java.util.UUID.randomUUID().toString() }
        val data = mapOf(
            "id" to docId,
            "timestamp" to feedback.timestamp,
            "transcription" to feedback.transcription,
            "category" to feedback.category,
            "aiResponse" to feedback.aiResponse,
            "status" to feedback.status,
            "deviceModel" to "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
        )
        firestore.collection("riders")
            .document(riderId)
            .collection("feedbacks")
            .document(docId)
            .set(data)
            .addOnSuccessListener {
                Log.d(TAG, "User voice feedback saved to Firestore: $docId")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error saving feedback to Firestore: ${e.message}")
            }
    }

    // Save WhatsApp notification to Firestore for real-time dashboard updates
    fun saveWhatsAppNotification(sender: String, text: String, riderId: String = FirebaseAuthManager.getCurrentRiderId()) {
        val firestore = db ?: return
        val data = mapOf(
            "sender" to sender,
            "text" to text,
            "timestamp" to System.currentTimeMillis(),
            "isRead" to false
        )
        firestore.collection("riders")
            .document(riderId)
            .collection("whatsapp")
            .document("last_received")
            .set(data, SetOptions.merge())
            .addOnSuccessListener {
                Log.d(TAG, "WhatsApp notification saved to Firestore for $riderId: sender=$sender")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error saving WhatsApp notification to Firestore", e)
            }
    }

    // Listen to WhatsApp replies from the dashboard and execute the notification response flow
    fun listenToWhatsAppReply(riderId: String = FirebaseAuthManager.getCurrentRiderId(), onReply: (String) -> Unit): ListenerRegistration? {
        val firestore = db ?: return null
        return firestore.collection("riders")
            .document(riderId)
            .collection("whatsapp")
            .document("reply_command")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Error listening to WhatsApp reply commands", e)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val status = snapshot.getString("status") ?: ""
                    val text = snapshot.getString("text") ?: ""
                    if (status == "pending" && text.isNotEmpty()) {
                        // Mark as processing or completed
                        val update = mapOf("status" to "completed")
                        snapshot.reference.set(update, SetOptions.merge())
                        onReply(text)
                    }
                }
            }
    }

    suspend fun validateLicense(driverId: String, licenseKey: String): Boolean {
        val firestore = db ?: return false
        return try {
            val doc = firestore.collection("licenses").document(licenseKey).get().awaitTask()
            if (doc.exists()) {
                val ownerId = doc.getString("ownerId")
                val expiry = doc.getLong("expiryDate") ?: 0L
                val isActive = doc.getBoolean("isActive") ?: false
                
                ownerId == driverId && isActive && expiry > System.currentTimeMillis()
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao validar licença: ${e.message}")
            false
        }
    }

    fun listenToRemoteCommands(onCommand: (String) -> Unit): ListenerRegistration? {
        val riderId = FirebaseAuthManager.getCurrentRiderId()
        if (riderId.isEmpty()) return null
        val firestore = db ?: return null
        
        return firestore.collection("riders")
            .document(riderId)
            .collection("commands")
            .document("last_command")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Error listening to remote commands", e)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val command = snapshot.getString("command") ?: ""
                    val timestamp = snapshot.getLong("timestamp") ?: 0L
                    
                    if (command.isNotEmpty() && (System.currentTimeMillis() - timestamp < 30000)) {
                        onCommand(command)
                        snapshot.reference.update("command", "")
                    }
                }
            }
    }

    fun saveProactiveMessage(message: String, riderId: String = FirebaseAuthManager.getCurrentRiderId()) {
        val firestore = db ?: return
        if (riderId.isEmpty()) return
        
        val data = mapOf(
            "message" to message,
            "timestamp" to System.currentTimeMillis()
        )
        
        firestore.collection("riders")
            .document(riderId)
            .collection("jarvis")
            .document("proactive_message")
            .set(data)
            .addOnFailureListener { e ->
                Log.e(TAG, "Error saving proactive message", e)
            }
    }

    fun listenToProactiveMessages(riderId: String = FirebaseAuthManager.getCurrentRiderId(), onMessage: (String) -> Unit): ListenerRegistration? {
        val firestore = db ?: return null
        if (riderId.isEmpty()) return null
        
        return firestore.collection("riders")
            .document(riderId)
            .collection("jarvis")
            .document("proactive_message")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Error listening to proactive messages", e)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val message = snapshot.getString("message") ?: ""
                    val timestamp = snapshot.getLong("timestamp") ?: 0L
                    
                    // Only process messages from the last 60 seconds
                    if (message.isNotEmpty() && (System.currentTimeMillis() - timestamp < 60000)) {
                        onMessage(message)
                    }
                }
            }
    }

    fun uploadNeuralLearning(learning: NeuralClickEntity) {
        val firestore = db ?: return
        val docId = "${learning.packageName}_${learning.keyword}".replace("/", "_").replace(".", "_")
        val data = mapOf(
            "packageName" to learning.packageName,
            "keyword" to learning.keyword,
            "semanticKey" to learning.semanticKey,
            "lastX" to learning.lastX,
            "lastY" to learning.lastY,
            "usageCount" to learning.usageCount,
            "timestamp" to System.currentTimeMillis()
        )
        firestore.collection("shared_neural_learnings")
            .document(docId)
            .set(data, SetOptions.merge())
            .addOnSuccessListener {
                Log.d(TAG, "Shared neural learning uploaded for ${learning.packageName}: ${learning.keyword}")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error uploading shared neural learning", e)
            }
    }

    fun syncSharedNeuralLearnings(context: android.content.Context) {
        val firestore = db ?: return
        firestore.collection("shared_neural_learnings")
            .limit(100)
            .get()
            .addOnSuccessListener { documents ->
                val dao = AppDatabase.getDatabase(context).neuralClickDao()
                val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)
                scope.launch {
                    for (doc in documents) {
                        try {
                            val pkg = doc.getString("packageName") ?: continue
                            val keyword = doc.getString("keyword") ?: continue
                            val semanticKey = doc.getString("semanticKey") ?: ""
                            val lastX = (doc.get("lastX") as? Number)?.toFloat() ?: 0.0f
                            val lastY = (doc.get("lastY") as? Number)?.toFloat() ?: 0.0f
                            val usageCount = (doc.get("usageCount") as? Number)?.toInt() ?: 1
                            
                            val existing = dao.getLearning(pkg, keyword)
                            if (existing == null) {
                                dao.insert(NeuralClickEntity(
                                    packageName = pkg,
                                    keyword = keyword,
                                    semanticKey = semanticKey,
                                    lastX = lastX,
                                    lastY = lastY,
                                    usageCount = usageCount,
                                    timestamp = System.currentTimeMillis()
                                ))
                                Log.d(TAG, "Sync: Imported shared learning from another driver for '$keyword' in $pkg")
                            } else if (usageCount > existing.usageCount) {
                                dao.update(existing.copy(
                                    lastX = lastX,
                                    lastY = lastY,
                                    usageCount = usageCount,
                                    semanticKey = semanticKey,
                                    timestamp = System.currentTimeMillis()
                                ))
                                Log.d(TAG, "Sync: Refined local learning for '$keyword' using shared driver intelligence.")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing shared learning document: ${doc.id}", e)
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error fetching shared neural learnings", e)
            }
    }
}

data class UserFeedback(
    val id: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val transcription: String = "",
    val category: String = "SUGGESTION",
    val aiResponse: String = "",
    val status: String = "PENDING_IA_UPGRADE"
)

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
    val emergencyContactPhone: String = "",
    val driverXP: Int = 120,
    val driverCombo: Int = 0
)
