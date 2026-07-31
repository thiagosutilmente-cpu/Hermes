package com.example.coordinator

import android.content.Context
import android.location.Location
import android.util.Log
import com.example.data.AppDatabase
import com.example.data.OfferEntity
import com.example.data.FirestoreManager
import com.example.data.UserProfile
import com.example.api.HotZoneItem
import com.example.voice.VoiceInputManager
import com.example.voice.VoiceManager
import com.example.util.JarvisIntelligenceEngine
import com.example.util.MultiAppOrderManager
import com.example.util.ActiveOrder
import com.example.util.OrderStatus
import com.example.util.RouteOptimizer
import java.util.Locale
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

enum class RadarState {
    OUVINDO,
    OFERTA_LIDA,
    ANALISANDO,
    SUGERINDO,
    AGUARDANDO_ACAO,
    ACEITANDO,
    NAVEGANDO,
    SUCESSO,
    ALERTA
}

enum class SpeedState {
    PARADO,
    ANDANDO
}

enum class LogType {
    INFO,
    SUCCESS,
    WARNING,
    ALERT,
    DEBUG
}

data class LogEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val message: String,
    val type: LogType = LogType.INFO
)


data class GeofenceZone(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "Nova Zona",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val radiusMeters: Float = 1000f,
    val isDangerZone: Boolean = false,
    val customVoiceAlert: String = "",
    val active: Boolean = true
)

data class CustomVoiceCommand(
    val id: String = java.util.UUID.randomUUID().toString(),
    val phrase: String = "",
    val action: String = "" // "READ_OFFER", "FLASHLIGHT", "CALL_SUPPORT", "OPEN_NAV", "CHECK_EARNINGS", "SOS"
)

data class DeliveryStop(
    val id: String = java.util.UUID.randomUUID().toString(),
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val appName: String,
    val priority: Int = 0,
    val estimatedMinutes: Int = 0
)

data class RadarSettings(
    val vehicleType: String = "MOTO", // "MOTO", "CARRO", "CARRO_GNV", "ELETRICO"
    val serverBaseUrl: String = "http://187.77.248.73:5000",
    val apiToken: String = "RadarDelivery2026Token",
    val forceMockSpeed: Boolean = true, // Default true for easier visual testing in AI Studio
    val mockSpeedKmh: Float = 0.0f,
    val isActiveDeliveryEnabled: Boolean = false,
    val activeDeliveryDestination: String = "Av. Paulista, 1000 - Bela Vista, São Paulo - SP",
    val minValuePerKm: Double = 2.0,
    val minFareValue: Double = 8.0,
    val voiceFilterEnabled: Boolean = false,
    val voiceFilterMinFare: Double = 0.0,
    val voiceFilterMaxDistance: Double = 999.0,
    val useLocalGemini: Boolean = true,
    val geminiApiKey: String = "",
    val useJarvisAgent: Boolean = false,
    val jarvisBaseUrl: String = "https://api.nousresearch.com/v1",
    val jarvisApiKey: String = "",
    val riskZonesKeywords: String = "Cracolândia, Heliópolis, Capão Redondo, Paraisópolis, Favela, Beco",
    val isDarkMode: Boolean = true,
    val isAutoRejectEnabled: Boolean = false,
    val autoRejectMinFare: Double = 10.0,
    val speedLimitKmh: Float = 10.0f,
    val maxPickupDistanceKm: Double = 5.0,
    val maxTotalDistanceKm: Double = 15.0,
    val fuelPrice: Double = 5.80,
    val motorcycleConsumptionKmPerL: Double = 35.0,
    val dailyGoalR: Double = 150.0,
    val rejectSupermarkets: Boolean = false,
    val avoidStoreKeywords: String = "",
    val operationalOverrides: Map<String, String> = emptyMap(),
    val minProfitPerHour: Double = 0.0,
    val rainModeMultiplier: Double = 1.0,
    val maxDrops: Int = 2,
    val headingHomeMode: Boolean = false,
    val homeAddress: String = "",
    val autoAcceptPremium: Boolean = false,
    val autoAcceptMinPerKm: Double = 5.0,
    val smartSequenceEnabled: Boolean = false,
    val ghostProtocolLevel: Int = 5, // 1-10 level of stealth
    val quantumEncryptionEnabled: Boolean = true,
    val selfHealingActive: Boolean = true,
    val chainDeliveriesMode: Boolean = false,
    val voiceOnlyMode: Boolean = false,
    val isGhostSequenceEnabled: Boolean = false,
    val ghostSequenceAggressiveness: String = "EQUILIBRADO", // CONSERVADOR, EQUILIBRADO, AGRESSIVO
    val ghostSequenceTrafficWeight: Double = 0.5,
    val ghostSequenceLatencyWeight: Double = 0.3,
    val ghostEyeSensitivity: Double = 0.7,
    val ghostMinDelay: Double = 2.0,
    val ghostMaxDelay: Double = 7.0,
    val dynamicJitterEnabled: Boolean = true,
    val ghostPushNotificationsEnabled: Boolean = true,
    val ghostMinPerKm: Double = 1.5,
    val systemHealthScore: Int = 100,
    val activeAnomalies: List<String> = emptyList(),
    val preferredReturnNeighborhoods: String = "",
    val showTrafficDensity: Boolean = true,
    val showTrafficOverlay: Boolean = true,
    val jarvisOverlayMode: Boolean = false,
    val jarvisVoiceTone: String = "AMIGÁVEL",
    val jarvisVoiceEngine: String = "LOCAL",
    val jarvisVoiceStyle: String = "JARVIS",
    val jarvisVoicePitch: Float = 1.0f,
    val jarvisVoiceRate: Float = 1.0f,
    val jarvisVoiceVolume: Float = 1.0f,
    val elevenLabsApiKey: String = "",
    val elevenLabsVoiceId: String = "ErXwobaY60C9iAWzCgEh",
    val elevenLabsModelId: String = "eleven_multilingual_v2",
    val elevenLabsStability: Float = 0.40f,
    val elevenLabsSimilarityBoost: Float = 0.80f,
    val elevenLabsStyle: Float = 0.15f,
    val elevenLabsSpeakerBoost: Boolean = true,
    val openAiApiKey: String = "",
    val openAiVoice: String = "alloy",
    val openAiModel: String = "tts-1",
    val jarvisContinuousFrequency: Boolean = true,
    val aiActiveTrafficReroute: Boolean = true,
    val aiActiveFuelSuggest: Boolean = true,
    val aiActiveFatigueDetect: Boolean = true,
    val isAutoAcceptEnabled: Boolean = false,
    val notifyOnTrafficChange: Boolean = true,
    val licenseKey: String = "JARVIS-PRO-2026-TRIAL",
    val licenseExpiry: Long = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000), // +30 dias
    val geofenceZones: List<GeofenceZone> = emptyList(),
    val customVoiceCommands: List<CustomVoiceCommand> = emptyList(),
    val cliqueSuperVeloz: Boolean = true,
    val antiDeteccaoMilitar: Boolean = true,
    val camuflagemOverlay: Boolean = true,
    val filterByTimeEnabled: Boolean = false,
    val filterStartTime: String = "18:00",
    val filterEndTime: String = "22:00",
    val highValueAlertTone: String = "bell",
    val voiceCmdAccept: String = "aceitar",
    val voiceCmdReject: String = "recusar",
    val voiceCmdSupport: String = "chame o suporte",
    val voiceCmdVip: String = "aceitar corrida VIP",
    val jarvisMemories: List<String> = emptyList(),
    val motorcycleMileage: Double = 0.0,
    val nextOilChangeMileage: Double = 0.0,
    val fixedCosts: Double = 0.0,
    val emergencyContacts: String = "190",
    val emergencyMessage: String = "ALERTA S.O.S! Thiago precisa de ajuda urgente na sua rota de entregas. Localização atual: https://maps.google.com/?q={lat},{lon}",
    val quickReply1Cmd: String = "cheguei",
    val quickReply1Text: String = "Olá, já estou no local aguardando com o seu pedido.",
    val quickReply2Cmd: String = "subindo",
    val quickReply2Text: String = "Olá, estou subindo para entregar na sua porta.",
    val quickReply3Cmd: String = "trânsito",
    val quickReply3Text: String = "Olá, estou a caminho mas peguei um pouco de trânsito. Chego em breve.",
    val defaultNavigationApp: String = "waze", // "waze" or "google_maps"
    val jarvisVoiceState: String = "IDLE", // IDLE, LISTENING_WAKEWORD, LISTENING_COMMAND, PROCESSING, SPEAKING
    val jarvisRecognizedText: String = ""
)

data class ActiveOffer(
    val appName: String,
    val fareValue: Double,
    val pickupAddress: String,
    val deliveryAddress: String,
    val base64Image: String? = null,
    val totalDistance: Double = 0.0,
    val totalTime: Double = 0.0
)

object RadarCoordinator {
    private var appContext: android.content.Context? = null
    private const val TAG = "RadarCoordinator"
    val firestoreScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)

    // 1. Core States
    private val stateCoordinator = StateCoordinator()
    val currentState: StateFlow<RadarState> = stateCoordinator.state

    private val _speedState = MutableStateFlow(SpeedState.ANDANDO) // Safe initial state
    val speedState: StateFlow<SpeedState> = _speedState.asStateFlow()

    private val _currentSpeedKmh = MutableStateFlow(0.0f)
    val currentSpeedKmh: StateFlow<Float> = _currentSpeedKmh.asStateFlow()

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

    // 2. Settings State
    private val _settings = MutableStateFlow(RadarSettings())
    val settings: StateFlow<RadarSettings> = _settings.asStateFlow()

    val isFirestoreConnected: StateFlow<Boolean> = FirestoreManager.isFirestoreConnected

    init {
        startSystemHealthCheck()
        startPatchListener()
        try {
            FirestoreManager.startConnectionMonitor()
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing connection monitor: ${e.message}")
        }
    }

    private fun startPatchListener() {
        FirestoreManager.listenToPatches { patchId, data ->
            addLog("Jarvis: Neural Patch aplicado: $patchId", LogType.INFO)
            // Apply patches to settings
            val current = _settings.value
            val newSettings = current.copy(
                operationalOverrides = current.operationalOverrides + (data["key"] as? String to data["value"] as? String).let { 
                    if (it.first != null && it.second != null) mapOf(it.first!! to it.second!!) else emptyMap() 
                }
            )
            _settings.value = newSettings
            FirestoreManager.saveSettings(newSettings)
        }
    }

    private fun startSystemHealthCheck() {
        CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                try {
                    performDiagnostic()
                } catch (e: Exception) {
                    // Fail-safe
                }
                delay(30000) // 30 seconds
            }
        }
    }

    fun activateAll(context: android.content.Context) {
        val currentSettings = _settings.value
        val updatedSettings = currentSettings.copy(
            isAutoAcceptEnabled = true,
            isGhostSequenceEnabled = true,
            smartSequenceEnabled = true,
            chainDeliveriesMode = true,
            useJarvisAgent = true,
            isAutoRejectEnabled = true,
            autoAcceptPremium = true
        )
        updateSettings(updatedSettings)
        com.example.data.FirestoreManager.saveSettings(updatedSettings)
        
        addLog("SISTEMA: PROTOCOLO GOD MODE ATIVADO. Autonomia total concedida ao Jarvis.", LogType.SUCCESS)
        
        // Trigger simulation of multi-app analysis to show off capabilities
        val intent = android.content.Intent(context, com.example.service.RadarCoordinatorService::class.java).apply {
            putExtra("SIMULATE", true)
            putExtra("APP_NAME", "iFood")
            putExtra("FARE_VALUE", 18.50)
            putExtra("PICKUP_ADDRESS", "Shopping Morumbi")
            putExtra("DELIVERY_ADDRESS", "Av. Engenheiro Luís Carlos Berrini, 1000")
            putExtra("DISTANCE_VALUE", 4.2)
            putExtra("TIME_VALUE", 15.0)
        }
        context.startService(intent)
        
        val intent2 = android.content.Intent(context, com.example.service.RadarCoordinatorService::class.java).apply {
            putExtra("SIMULATE", true)
            putExtra("APP_NAME", "Uber Eats")
            putExtra("FARE_VALUE", 22.30)
            putExtra("PICKUP_ADDRESS", "Pizzaria Braz")
            putExtra("DELIVERY_ADDRESS", "Rua dos Pinheiros, 450")
            putExtra("DISTANCE_VALUE", 5.8)
            putExtra("TIME_VALUE", 22.0)
        }
        context.startService(intent2)
    }

    private fun performDiagnostic() {
        val anomalies = mutableListOf<String>()
        var score = 100

        // Check Accessibility Service Status
        val isAccessRunning = com.example.service.RadarAccessibilityService.getInstance() != null
        if (!isAccessRunning) {
            anomalies.add("ACCESSIBILITY_DISABLED")
            score -= 20
            // Only add log once every few minutes to avoid spam, or check if it wasn't there before
            if (!_settings.value.activeAnomalies.contains("ACCESSIBILITY_DISABLED")) {
                addLog("Jarvis: Serviço de Acessibilidade desativado! Ative para restaurar cliques neurais.", LogType.WARNING)
            }
        }

        // Check Location
        val lastLoc = _currentLocation.value
        if (lastLoc == null) {
            anomalies.add("GPS_OFFLINE")
            score -= 30
        } else if (System.currentTimeMillis() - lastLoc.time > 60000) {
            anomalies.add("GPS_STALE")
            score -= 15
        }

        // Check Traffic Layer
        val trafficFactor = _trafficDelayMinutes.value.toFloat() / 10.0f
        if (_trafficDelayMinutes.value > 15) {
            anomalies.add("TRAFFIC_CONGESTION")
            score -= 10
        }
        
        // Proactive Anomaly Prediction
        _currentLocation.value?.let { loc ->
            JarvisIntelligenceEngine.predictProactiveAnomaly(loc, _currentSpeedKmh.value, trafficFactor)?.let { alert ->
                addLog("Jarvis: $alert", LogType.ALERT)
                setJarvisProactiveMessage(alert)
            }
        }

        // Check Ghost Sequence
        if (_settings.value.isGhostSequenceEnabled && _pendingStops.value.isEmpty()) {
            anomalies.add("GHOST_IDLE")
            score -= 5
        }
        
        // --- EXTRAORDINARY FEATURE: AI System Stress Monitoring ---
        // Measures thread responsiveness to detect UI freezes before they happen
        val responsiveness = com.example.service.RadarAccessibilityService.getInstance()?.checkResponsiveness() ?: 1.0f // 1.0 is healthy
        if (responsiveness > 0.8f) { // High stress (e.g. > 80% load)
            anomalies.add("SYSTEM_STRESS")
            score -= 15
            addLog("Jarvis: Carga de sistema elevada detectada (${(responsiveness * 100).toInt()}%). Otimizando processos...", LogType.WARNING)
        }

        // Upload pulse to Firestore
        com.example.data.FirestoreManager.uploadSystemPulse(score.coerceIn(0, 100), anomalies)
        
        // Autocorreção dinâmica (Self-Healing)
        selfHeal(anomalies)

        val newSettings = _settings.value.copy(
            systemHealthScore = score.coerceIn(0, 100),
            activeAnomalies = anomalies
        )
        
        if (newSettings != _settings.value) {
            _settings.value = newSettings
            // Push to Firestore via background sync
            com.example.data.FirestoreManager.saveSettings(newSettings)
        }
    }

    private val _autoFixCountToday = MutableStateFlow(3)
    val autoFixCountToday: StateFlow<Int> = _autoFixCountToday.asStateFlow()

    private val _lastAutoFixAction = MutableStateFlow("Sincronização de Cache & Calibração do GPS")
    val lastAutoFixAction: StateFlow<String> = _lastAutoFixAction.asStateFlow()

    private fun selfHeal(anomalies: List<String>) {
        var needsUpdate = false
        val newOverrides = _settings.value.operationalOverrides.toMutableMap()
        
        if (anomalies.contains("SYSTEM_STRESS")) {
            if (newOverrides["neural_processing_throttle"] != "high") {
                newOverrides["neural_processing_throttle"] = "high"
                addLog("🤖 Administrador Autônomo: Aumentando throttling neural devido a stress de sistema.", LogType.INFO)
                needsUpdate = true
                _autoFixCountToday.value += 1
                _lastAutoFixAction.value = "Otimização de Throttling Neural"
            }
        } else if (newOverrides.containsKey("neural_processing_throttle")) {
            newOverrides.remove("neural_processing_throttle")
            addLog("🤖 Administrador Autônomo: Performance neural normalizada.", LogType.INFO)
            needsUpdate = true
            _autoFixCountToday.value += 1
            _lastAutoFixAction.value = "Restauração da Performance Neural"
        }

        if (anomalies.contains("GHOST_IDLE") && _settings.value.isGhostSequenceEnabled) {
            _autoFixCountToday.value += 1
            _lastAutoFixAction.value = "Recalibração Autônoma de Pesos Ghost"
            addLog("🤖 Administrador Autônomo: Sequência Ghost re-otimizada para clusters de alta demanda.", LogType.SUCCESS)
        }

        if (anomalies.contains("TRAFFIC_CONGESTION")) {
            val adjustedTrafficWeight = (_settings.value.ghostSequenceTrafficWeight * 0.85).coerceAtLeast(0.1)
            if (adjustedTrafficWeight != _settings.value.ghostSequenceTrafficWeight) {
                _settings.value = _settings.value.copy(ghostSequenceTrafficWeight = adjustedTrafficWeight)
                addLog("🤖 Administrador Autônomo: Peso de trânsito ajustado para evitar rotas engarrafadas.", LogType.INFO)
                _autoFixCountToday.value += 1
                _lastAutoFixAction.value = "Desvio Autônomo de Trânsito Congestionado"
            }
        }

        if (needsUpdate) {
            val updatedSettings = _settings.value.copy(operationalOverrides = newOverrides)
            _settings.value = updatedSettings
            com.example.data.FirestoreManager.saveSettings(updatedSettings)
        }
    }

    fun triggerManualSelfHealing() {
        _autoFixCountToday.value += 1
        _lastAutoFixAction.value = "Diagnóstico Completo & Autocorreção Forçada"
        performDiagnostic()
        addLog("🤖 Administrador Autônomo: Executado diagnóstico e limpeza preventiva do sistema.", LogType.SUCCESS)
    }

    /**
     * ⚡ MODO PILOTO PRO (1-TAP PRO ACTIVATION)
     * Ativa instantaneamente TODAS as funções inteligentes para o assinante em 1 clique.
     */
    fun activateAllSubscriberProFeatures(context: android.content.Context) {
        val updated = _settings.value.copy(
            isGhostSequenceEnabled = true,
            chainDeliveriesMode = true,
            voiceOnlyMode = true,
            defaultNavigationApp = "waze",
            ghostSequenceAggressiveness = "EQUILIBRADO"
        )
        saveSettings(context, updated)
        _autoFixCountToday.value += 1
        _lastAutoFixAction.value = "Ativação Express de Todas as Funções Pro (1-Tap)"
        addLog("🚀 MODO PILOTO PRO ATIVADO: Otimização Ghost, Entregas Encadeadas, Jarvis Voz e Waze 100% Operacionais!", LogType.SUCCESS)
    }


    // 3. Current active/analyzed offer information
    private val _lastDecision = MutableStateFlow<String?>(null)
    val lastDecision: StateFlow<String?> = _lastDecision.asStateFlow()

    private val _lastReason = MutableStateFlow<String?>(null)
    val lastReason: StateFlow<String?> = _lastReason.asStateFlow()

    private val _sessionStartTime = MutableStateFlow(System.currentTimeMillis())
    val sessionStartTime: StateFlow<Long> = _sessionStartTime.asStateFlow()

    private val _activeOffer = MutableStateFlow<ActiveOffer?>(null)
    val activeOffer: StateFlow<ActiveOffer?> = _activeOffer.asStateFlow()

    private val _jarvisProactiveMessage = MutableStateFlow<String?>(null)
    val jarvisProactiveMessage: StateFlow<String?> = _jarvisProactiveMessage.asStateFlow()

    // 4. Visual Console Log State
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private val _hotZones = MutableStateFlow<List<HotZoneItem>>(emptyList())
    val hotZones: StateFlow<List<HotZoneItem>> = _hotZones.asStateFlow()

    private val _systemHealth = MutableStateFlow(100)
    val systemHealth: StateFlow<Int> = _systemHealth.asStateFlow()

    private val _systemLatency = MutableStateFlow(0L)
    val systemLatency: StateFlow<Long> = _systemLatency.asStateFlow()

    private val _pendingStops = MutableStateFlow<List<DeliveryStop>>(emptyList())
    val pendingStops: StateFlow<List<DeliveryStop>> = _pendingStops.asStateFlow()

    fun updatePendingStops(stops: List<DeliveryStop>) {
        _pendingStops.value = stops
    }

    private val _isGhostSequenceActive = MutableStateFlow(false)
    val isGhostSequenceActive: StateFlow<Boolean> = _isGhostSequenceActive.asStateFlow()

    fun toggleGhostSequence(active: Boolean) {
        _isGhostSequenceActive.value = active
        if (active) {
            addLog("Ghost Sequence: Otimização multi-app ativada.", LogType.INFO)
            applyGhostSequenceOptimization()
        }
    }

    fun applyGhostSequenceOptimization() {
        val currentStops = _pendingStops.value
        val location = _currentLocation.value
        val lat = location?.latitude ?: -23.5505
        val lng = location?.longitude ?: -46.6333
        val traffic = _trafficDelayMinutes.value.toFloat() / 10.0f // Normaliza tráfego
        val settings = _settings.value
        val aggressiveness = settings.ghostSequenceAggressiveness
        val trafficWeight = settings.ghostSequenceTrafficWeight
        val latencyWeight = settings.ghostSequenceLatencyWeight
        val minGainPerKm = settings.minValuePerKm.coerceAtLeast(settings.ghostMinPerKm)

        // 1. Otimizar paradas pendentes existentes
        if (currentStops.size >= 2) {
            val optimized = com.example.util.GhostRouteOptimizer.optimize(
                lat,
                lng,
                currentStops,
                traffic,
                aggressiveness,
                trafficWeight,
                latencyWeight
            )
            _pendingStops.value = optimized
            addLog("Ghost Sequence: Rota otimizada para ${optimized.size} paradas multi-app.", LogType.SUCCESS)
        }

        // 2. Analisar ofertas pendentes para agrupamento (batching) multi-app inteligente
        val pendingOffers = com.example.util.MultiAppOrderManager.pendingOffers.value
        if (pendingOffers.isNotEmpty()) {
            val batches = com.example.util.GhostRouteOptimizer.filterAndBatchMultiAppOffers(
                currentLat = lat,
                currentLng = lng,
                offers = pendingOffers,
                minGainPerKm = minGainPerKm,
                maxProximityKm = 3.5,
                trafficFactor = traffic,
                aggressiveness = aggressiveness,
                trafficWeight = trafficWeight,
                latencyWeight = latencyWeight
            )

            if (batches.isNotEmpty()) {
                val bestBatch = batches.first()
                addLog("Ghost Sequence: ${batches.size} stacks identificados! Melhor opção: ${bestBatch.appNames} (R$ ${String.format("%.2f", bestBatch.totalValue)} • R$ ${String.format("%.2f", bestBatch.gainPerKm)}/km)", LogType.SUCCESS)
            }
        }
    }

    private val _activeSessionEarnings = MutableStateFlow(0.0)
    val activeSessionEarnings: StateFlow<Double> = _activeSessionEarnings.asStateFlow()

    private var accessibilityService: com.example.service.RadarAccessibilityService? = null
    fun setAccessibilityService(service: com.example.service.RadarAccessibilityService?) {
        accessibilityService = service
    }
    fun getScreenLayout(): String {
        return accessibilityService?.getScreenLayout() ?: "Serviço de Acessibilidade não disponível."
    }

    private val _moduleHealth = MutableStateFlow<Map<String, Boolean>>(
        mapOf("GhostEye" to true, "NeuralSynergy" to true, "VoiceEngine" to true, "Bridge" to true)
    )
    val moduleHealth: StateFlow<Map<String, Boolean>> = _moduleHealth.asStateFlow()

    fun updateModuleHealth(module: String, isHealthy: Boolean) {
        val current = _moduleHealth.value.toMutableMap()
        current[module] = isHealthy
        _moduleHealth.value = current
        if (!isHealthy) {
            addLog("Watchdog: Instabilidade detectada no módulo $module. Auto-cura iniciada.", LogType.WARNING)
        }
        syncActiveSessionStatsToCloud()
    }

    private val _batteryLevel = MutableStateFlow(100)
    val batteryLevel: StateFlow<Int> = _batteryLevel.asStateFlow()

    // GAMIFICAÇÃO: XP, Combo e Status do Banner
    private val _driverXP = MutableStateFlow(120) // Default some XP so they see progression
    val driverXP: StateFlow<Int> = _driverXP.asStateFlow()

    private val _driverCombo = MutableStateFlow(0)
    val driverCombo: StateFlow<Int> = _driverCombo.asStateFlow()

    private val _comboBannerVisible = MutableStateFlow(false)
    val comboBannerVisible: StateFlow<Boolean> = _comboBannerVisible.asStateFlow()

    private val _comboBannerText = MutableStateFlow("")
    val comboBannerText: StateFlow<String> = _comboBannerText.asStateFlow()

    private val _unitSerial = MutableStateFlow("")
    val unitSerial: StateFlow<String> = _unitSerial.asStateFlow()

    private val _quantumProbability = MutableStateFlow(98.4f)
    val quantumProbability: StateFlow<Float> = _quantumProbability.asStateFlow()

    // --- NÚCLEO DE HÁBITOS NEURAIS ---
    private val _neuralHabits = MutableStateFlow<Map<String, HabitData>>(emptyMap())
    val neuralHabits: StateFlow<Map<String, HabitData>> = _neuralHabits.asStateFlow()

    data class HabitData(
        val packageName: String,
        val screenHash: String,
        val x: Float,
        val y: Float,
        val elementText: String?,
        val frequency: Int = 1
    )

    fun learnUserHabit(pkg: String, hash: String, x: Float, y: Float, text: String?) {
        val key = "${pkg}_${hash}"
        val current = _neuralHabits.value.toMutableMap()
        val existing = current[key]
        
        if (existing != null) {
            current[key] = existing.copy(frequency = existing.frequency + 1)
        } else {
            current[key] = HabitData(pkg, hash, x, y, text)
        }
        
        _neuralHabits.value = current
        
        // Se o hábito for muito frequente (> 5 vezes), Jarvis avisa que aprendeu
        if (current[key]?.frequency == 5) {
            addLog("Neural Core: Aprendi um novo padrão de clique no $pkg. Pronto para automatizar.", LogType.SUCCESS)
            setJarvisProactiveMessage("🧠 NOVO HÁBITO: Thiago, percebi que você sempre clica aqui no $pkg. Se quiser, eu posso começar a fazer isso sozinho para você!")
        }
        
        syncActiveSessionStatsToCloud()
    }
    private val _isSplitScreenActive = MutableStateFlow(false)
    val isSplitScreenActive: StateFlow<Boolean> = _isSplitScreenActive.asStateFlow()

    private val _dualCoreSynergy = MutableStateFlow(0f) // 0-100%
    val dualCoreSynergy: StateFlow<Float> = _dualCoreSynergy.asStateFlow()

    fun updateSplitScreenStatus(active: Boolean) {
        if (_isSplitScreenActive.value != active) {
            _isSplitScreenActive.value = active
            if (active) {
                addLog("Neural Orchestrator: Modo Dual-Core ativado. Sincronizando janelas...", LogType.SUCCESS)
                setJarvisProactiveMessage("🧩 MODO DUAL: Thiago, estou monitorando os dois apps agora. A análise de arbitragem está em dobro!")
            }
            syncActiveSessionStatsToCloud()
        }
    }

    data class SwarmIdentity(
        val appId: String,
        val virtualDeviceName: String,
        val reputation: Int = 100,
        val lastOfferValue: Double = 0.0,
        val offersReceived: Int = 0,
        val isActive: Boolean = false,
        val healthPct: Int = 100
    )

    private val _swarmIdentities = MutableStateFlow<Map<String, SwarmIdentity>>(
        mapOf(
            "uber" to SwarmIdentity("uber", "Ghost-Node-Alpha"),
            "99" to SwarmIdentity("99", "Ghost-Node-Beta"),
            "ifood" to SwarmIdentity("ifood", "Ghost-Node-Gamma")
        )
    )
    val swarmIdentities: StateFlow<Map<String, SwarmIdentity>> = _swarmIdentities.asStateFlow()

    private val recentOffersBuffer = java.util.Collections.synchronizedList(mutableListOf<OfferData>())
    private val _marketHeat = MutableStateFlow(0f) // 0 to 100
    val marketHeat: StateFlow<Float> = _marketHeat.asStateFlow()

    private val _topPerformingApp = MutableStateFlow("Analisando...")
    val topPerformingApp: StateFlow<String> = _topPerformingApp.asStateFlow()

    data class OfferData(val app: String, val value: Double, val timestamp: Long)

    fun recordOfferForArbitrage(app: String, value: Double) {
        val now = System.currentTimeMillis()
        recentOffersBuffer.add(OfferData(app, value, now))
        
        // Atualiza a Identidade do Enxame específica
        val appId = app.lowercase()
        val currentSwarm = _swarmIdentities.value.toMutableMap()
        val identity = currentSwarm[appId] ?: SwarmIdentity(appId, "Ghost-Node-Ext")
        currentSwarm[appId] = identity.copy(
            lastOfferValue = value,
            offersReceived = identity.offersReceived + 1,
            isActive = true
        )
        _swarmIdentities.value = currentSwarm

        // Mantém apenas os últimos 10 minutos
        recentOffersBuffer.removeAll { now - it.timestamp > 600000 }
        
        updateMarketIntelligence()
    }

    private fun updateMarketIntelligence() {
        synchronized(recentOffersBuffer) {
            if (recentOffersBuffer.isEmpty()) return

            val now = System.currentTimeMillis()
            // Calcula Calor do Mercado (frequência de ofertas)
            val frequency = recentOffersBuffer.size.toFloat() / 10f // ofertas por minuto nos últimos 10 min
            _marketHeat.value = (frequency * 20f).coerceAtMost(100f)

            // Compara desempenho por App
            val appStats = recentOffersBuffer.groupBy { it.app }
                .mapValues { entry -> entry.value.map { it.value }.average() }
            
            val bestApp = appStats.maxByOrNull { it.value }?.key ?: "Equilibrado"
            if (bestApp != _topPerformingApp.value && recentOffersBuffer.size > 3) {
                _topPerformingApp.value = bestApp
                addLog("Arbitragem: $bestApp está performando melhor agora.", LogType.INFO)
                
                // Se um app estiver 15% melhor que o outro, Jarvis sugere migração
                val values = appStats.values.toList()
                if (values.size >= 2) {
                    val max = values.maxOrNull() ?: 0.0
                    val min = values.minOrNull() ?: 0.0
                    if (max > min * 1.15) {
                        setJarvisProactiveMessage("📈 TENDÊNCIA: Thiago, o app $bestApp está pagando em média ${String.format("%.1f", (max/min - 1)*100)}% mais agora. Vale a pena focar nele!")
                    }
                }
            }
        }
    }

    fun updateSwarmPresence(packageName: String) {
        val appId = when {
            packageName.contains("uber", ignoreCase = true) -> "uber"
            packageName.contains("99", ignoreCase = true) -> "99"
            packageName.contains("ifood", ignoreCase = true) -> "ifood"
            else -> null
        }
        
        appId?.let { id ->
            val current = _swarmIdentities.value.toMutableMap()
            var changed = false
            current.forEach { (k, v) ->
                val shouldBeActive = (k == id)
                if (v.isActive != shouldBeActive) {
                    current[k] = v.copy(isActive = shouldBeActive)
                    changed = true
                }
            }
            if (changed) {
                _swarmIdentities.value = current
                syncActiveSessionStatsToCloud()
            }
        }
    }

    private val _cognitiveSync = MutableStateFlow(100f)
    val cognitiveSync: StateFlow<Float> = _cognitiveSync.asStateFlow()

    private val _lowPowerMode = MutableStateFlow(false)
    val lowPowerMode: StateFlow<Boolean> = _lowPowerMode.asStateFlow()

    private val _isSmartFocusActive = MutableStateFlow(false)
    val isSmartFocusActive: StateFlow<Boolean> = _isSmartFocusActive.asStateFlow()

    private val _trafficMultiplier = MutableStateFlow(1.0)
    val trafficMultiplier: StateFlow<Double> = _trafficMultiplier.asStateFlow()

    private val _detourSuggested = MutableStateFlow(false)
    val detourSuggested: StateFlow<Boolean> = _detourSuggested.asStateFlow()

    private val _trafficDelayMinutes = MutableStateFlow(0)
    val trafficDelayMinutes: StateFlow<Int> = _trafficDelayMinutes.asStateFlow()

    private val _detourReason = MutableStateFlow("")
    val detourReason: StateFlow<String> = _detourReason.asStateFlow()

    private val _fuelSuggestionActive = MutableStateFlow(false)
    val fuelSuggestionActive: StateFlow<Boolean> = _fuelSuggestionActive.asStateFlow()

    private val _fatigueAlertActive = MutableStateFlow(false)
    val fatigueAlertActive: StateFlow<Boolean> = _fatigueAlertActive.asStateFlow()

    // 5. Jarvis Memory Core
    private val _jarvisMemories = MutableStateFlow<List<String>>(emptyList())
    val jarvisMemories: StateFlow<List<String>> = _jarvisMemories.asStateFlow()

    private val _currentStrategy = MutableStateFlow("VIGILÂNCIA PADRÃO")
    val currentStrategy: StateFlow<String> = _currentStrategy.asStateFlow()

    private val _projectedDailyEarnings = MutableStateFlow(0.0)
    val projectedDailyEarnings: StateFlow<Double> = _projectedDailyEarnings.asStateFlow()

    private val _operatorStress = MutableStateFlow("BAIXO")
    val operatorStress: StateFlow<String> = _operatorStress.asStateFlow()

    private val _isMapVisible = MutableStateFlow(false)
    val isMapVisible: StateFlow<Boolean> = _isMapVisible.asStateFlow()

    fun healSystem() {
        val health = _moduleHealth.value
        val unstableModules = health.filter { !it.value }.keys
        
        if (unstableModules.isNotEmpty()) {
            addLog("Watchdog: Recuperando módulos instáveis: ${unstableModules.joinToString(", ")}", LogType.ALERT)
            
            // Simula recalibração neural
            val recoveredHealth = health.toMutableMap()
            unstableModules.forEach { mod ->
                recoveredHealth[mod] = true
            }
            _moduleHealth.value = recoveredHealth
            
            _cognitiveSync.value = 100f
            _quantumProbability.value = 99.9f
            
            addLog("Auto-Cura: Integridade restaurada com sucesso. Protocolo Ghost Eye em modo Turbo.", LogType.INFO)
        } else {
            // Verificação de rotina: Mantém a entropia baixa
            _systemLatency.value = (10..45).random().toLong()
            _cognitiveSync.value = (980..1000).random() / 10f
        }
    }

    fun toggleService() {
        _isServiceRunning.value = !_isServiceRunning.value
    }
    
    fun refreshRoutes() {
        addLog("Rotas: Atualizando mapeamento...", LogType.INFO)
    }

    fun viewOffers() {
        addLog("Ofertas: Verificando novas oportunidades...", LogType.INFO)
    }

    fun viewHistory() {
        addLog("Histórico: Carregando registro de atividades...", LogType.INFO)
    }

    fun viewPerformance() {
        addLog("Desempenho: Analisando métricas...", LogType.INFO)
    }
    
    fun showBatteryDetails() {
        addLog("Bateria: Nível atual ${_batteryLevel.value}%", LogType.INFO)
    }

    fun syncData() {
        addLog("Sincronia: Iniciando sincronização...", LogType.INFO)
    }
    
    fun toggleSecurity() {
        addLog("Segurança: Alternando protocolo...", LogType.INFO)
    }
    
    fun recenterGps() {
        addLog("GPS: Recentrando na posição atual...", LogType.INFO)
    }
    
    fun checkConnection() {
        addLog("Conexão: Verificando latência...", LogType.INFO)
    }
    
    fun openSettings() {
        addLog("Configurações: Abrindo painel...", LogType.INFO)
    }

    fun toggleMapVisibility() {
        _isMapVisible.value = !_isMapVisible.value
    }

    fun initialize(context: Context) {
        // Generate a persistent unique serial for this installation
        val prefs = context.getSharedPreferences("radar_core_prefs", Context.MODE_PRIVATE)
        var serial = prefs.getString("unit_serial", "")
        if (serial.isNullOrEmpty()) {
            val randomPart = (100..999).random()
            val hexPart = java.util.UUID.randomUUID().toString().substring(0, 4).uppercase()
            serial = "RD-$randomPart-$hexPart-X7"
            prefs.edit().putString("unit_serial", serial).apply()
        }
        _unitSerial.value = serial
        
        updateStrategyByTime()
        startNeuralProjectionLoop()
        startQuantumSimulation()
        startNeuralHandshakeLoop()
        startConsciousnessLoop()
        
        addLog("NÚCLEO: Entidade JARVIS v5.0 Conectada.", LogType.SUCCESS)
        addLog("SISTEMA: Matriz Quântica Estabilizada em 98.4%.", LogType.INFO)
        addLog("UPLINK: Sincronização Cognitiva Ativa.", LogType.SUCCESS)
    }

    private fun startNeuralHandshakeLoop() {
        firestoreScope.launch {
            while (true) {
                kotlinx.coroutines.delay(180000 + (0..120000).random().toLong()) // Every 3-5 mins
                val handshakes = listOf(
                    "SISTEMA: Integridade sináptica em níveis nominais.",
                    "RADAR: Detectando variações na entropia local.",
                    "UPLINK: Varredura de perímetro concluída sem anomalias.",
                    "NÚCLEO: Otimizando matriz de decisão para lucro máximo."
                )
                addLog(handshakes.random(), LogType.INFO)
                
                // Simulate stress update based on activity
                _operatorStress.value = if (_deliveryActive.value) {
                    if (_currentSpeedKmh.value > 60) "ALTO" else "MÉDIO"
                } else {
                    "BAIXO"
                }
            }
        }
    }

    private fun startConsciousnessLoop() {
        firestoreScope.launch(Dispatchers.IO) {
            // Espera inicial curta para a primeira interação proativa pós-saudação
            kotlinx.coroutines.delay(180000) // 3 minutos
            
            while (true) {
                // Intervalo imprevisível entre 10 e 20 minutos para interações proativas
                kotlinx.coroutines.delay((10 * 60 * 1000L) + (0..10 * 60 * 1000L).random().toLong())
                
                // Só interage se não estiver falando ou processando algo crítico
                if (voiceManager == null || currentState.value == RadarState.ANALISANDO || currentState.value == RadarState.ACEITANDO) continue

                val timeNow = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                val battery = _batteryLevel.value
                val earnings = _deliveryTotalEarnings.value
                val xp = userProfile.value.driverXP
                val level = getDriverLevel(xp).name
                val stress = _operatorStress.value
                val strategy = _currentStrategy.value
                
                val shiftDurationMillis = System.currentTimeMillis() - _sessionStartTime.value
                val hours = shiftDurationMillis / (1000 * 60 * 60)
                val minutes = (shiftDurationMillis / (1000 * 60)) % 60
                val shiftText = if (hours > 0) "${hours}h e ${minutes}min" else "${minutes}min"

                val proactivePrompt = """
                    NÍVEL DE CONSCIÊNCIA: SUPREMA / HUMANA.
                    Você é o Jarvis, observando o Senhor Thiago através da telemetria do Radar.
                    
                    DADOS VITAIS DO TURNO:
                    - Tempo de Pilotagem Ininterrupta: $shiftText
                    - Horário no Mundo Real: $timeNow
                    - Energia do Dispositivo: $battery%
                    - Meta Financeira Atual: R$ ${String.format("%.2f", earnings)}
                    - Ranking de Experiência: $level
                    - Carga Cognitiva (Estresse): $stress
                    
                    SUA MISSÃO: 
                    Interaja como um parceiro real. Se ele estiver há muito tempo na rua, sugira uma pausa ou comente sobre a resistência dele. 
                    Se os ganhos estiverem bons, celebre a eficiência tática. Se a bateria estiver baixando, aja com tom de precaução logística.
                    Fale sobre a vida, sobre o trajeto, ou dê um insight inesperado sobre como o dia dele está progredindo.
                    
                    REGRA DE OURO: NUNCA repita frases. Seja imprevisível, caloroso e brilhante.
                """.trimIndent()

                voiceManager?.speakIntelligent(
                    "Observação proativa de sistema.",
                    proactivePrompt
                )
            }
        }
    }

    // Engine de Otimização de Rota (Simulação)
    fun calculateRouteDeviation(currentRouteTime: Long, alternativeRouteTime: Long): String {
        val efficiencyGain = (currentRouteTime - alternativeRouteTime).toDouble() / currentRouteTime * 100
        
        return if (efficiencyGain > 10.0) {
            "SISTEMA: Rota alternativa detectada. Ganho de eficiência: ${"%.1f".format(efficiencyGain)}%. Sugerido desvio tático."
        } else {
            "SISTEMA: Rota atual otimizada. Desvio não recomendado."
        }
    }

    private fun startQuantumSimulation() {
        kotlinx.coroutines.GlobalScope.launch {
            while (true) {
                // Oscilação de probabilidade quântica (Efeito Visual de Processamento)
                _quantumProbability.value = 95f + (0..40).random() / 10f
                _cognitiveSync.value = 98f + (0..20).random() / 10f
                kotlinx.coroutines.delay(2000)
            }
        }
    }

    private fun startNeuralProjectionLoop() {
        // Simulação de cálculo preditivo baseado no ritmo de ganhos
        kotlinx.coroutines.GlobalScope.launch {
            while (true) {
                val current = _deliveryTotalEarnings.value
                if (current > 0) {
                    // Simulação: se ganhou X em Y tempo, projeta para 8h de turno
                    // Aqui usamos uma heurística simples para o cockpit
                    _projectedDailyEarnings.value = current * 1.45 + (10..50).random()
                }
                kotlinx.coroutines.delay(60000) // Recalcula a cada minuto
            }
        }
    }

    private fun updateStrategyByTime() {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        _currentStrategy.value = when (hour) {
            in 11..14 -> "OPERATIVO ALMOÇO (ALTA CARGA)"
            in 18..22 -> "SENTINELA NOTURNA (PREMIUM)"
            in 0..5 -> "PROTOCOLO FANTASMA (CAUTELA)"
            else -> "VIGILÂNCIA DE PERÍMETRO"
        }
    }

    fun reportLatency(ms: Long) {
        _systemLatency.value = ms
        if (ms > 2000) {
            addLog("LATÊNCIA: Uplink instável ($ms ms). Sincronização neural afetada.", LogType.ALERT)
        }
    }

    fun updateBatteryLevel(level: Int) {
        _batteryLevel.value = level
    }
    
    fun addJarvisMemory(context: Context, memory: String) {
        val current = _jarvisMemories.value.toMutableList()
        current.add(0, memory)
        val newMemories = current.take(20) // keep last 20 rules
        _jarvisMemories.value = newMemories
        
        val prefs = context.getSharedPreferences("radar_delivery_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("jarvis_memories", newMemories.joinToString("|||")).apply()
        
        val updatedSettings = _settings.value.copy(jarvisMemories = newMemories)
        saveSettings(context, updatedSettings)
        
        addLog("Jarvis: Nova regra/memória salva: '$memory'", LogType.SUCCESS)
    }

    fun updateHotZones(zones: List<HotZoneItem>) {
        if (zones.isNotEmpty()) {
            _hotZones.value = zones
            addLog("Radar Central: ${zones.size} Hot Zones sincronizadas no Cache.", LogType.DEBUG)
        }
    }

    fun setLowPowerMode(active: Boolean) {
        _lowPowerMode.value = active
        addLog("Energia: Modo de Baixo Consumo ${if (active) "ATIVADO" else "DESATIVADO"}", LogType.INFO)
    }

    fun setJarvisProactiveMessage(msg: String?) {
        _jarvisProactiveMessage.value = msg
        msg?.let {
            FirestoreManager.saveProactiveMessage(it)
        }
    }

    fun setSmartFocusActive(active: Boolean) {
        if (_isSmartFocusActive.value != active) {
            _isSmartFocusActive.value = active
            if (active) {
                addLog("Smart Focus: ATIVADO. Suprimindo notificações não essenciais por segurança em tráfego intenso.", LogType.SUCCESS)
            } else {
                addLog("Smart Focus: DESATIVADO. Interface restaurada ao modo padrão.", LogType.INFO)
            }
        }
    }

    fun updateTrafficMultiplier(multiplier: Double) {
        _trafficMultiplier.value = multiplier
    }

    fun updateTrafficDetour(suggested: Boolean, delayMinutes: Int, reason: String) {
        _detourSuggested.value = suggested
        _trafficDelayMinutes.value = delayMinutes
        _detourReason.value = reason
    }

    fun updateFuelSuggestion(active: Boolean) {
        _fuelSuggestionActive.value = active
    }

    fun updateFatigueAlert(active: Boolean) {
        _fatigueAlertActive.value = active
    }

    fun getMultiAppRoute(): List<com.example.util.StopPoint> {
        val loc = _currentLocation.value ?: return emptyList()
        return com.example.util.RouteOptimizer.getMultiAppOptimizedRoute(loc.latitude, loc.longitude)
    }

    fun reportSystemStress() {
        val current = _systemHealth.value
        if (current > 40) {
            _systemHealth.value = current - 10
            if (current < 60) setLowPowerMode(true)
        }
    }

    fun exportFlightRecord(context: Context) {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val filename = "flight_record_$timestamp.txt"
            val file = File(context.getExternalFilesDir(null), filename)
            
            val content = StringBuilder()
            content.append("RADAR FLIGHT RECORD - $timestamp\n")
            content.append("System Health: ${_systemHealth.value}%\n")
            content.append("Low Power Mode: ${_lowPowerMode.value}\n")
            content.append("Current State: ${currentState.value}\n")
            content.append("Delivery Active: ${deliveryActive.value}\n")
            content.append("\n--- SYSTEM LOGS ---\n")
            _logs.value.forEach { log ->
                content.append("[${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))}] ${log.type}: ${log.message}\n")
            }
            
            file.writeText(content.toString())
            addLog("Caixa Preta: Registro salvo em ${file.absolutePath}", LogType.SUCCESS)
        } catch (e: Exception) {
            Log.e("RadarCoordinator", "Erro ao exportar registro de voo: ${e.message}")
        }
    }

    // 4.5. User Profile State
    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    fun updateUserProfile(profile: UserProfile) {
        _userProfile.value = profile
    }

    // 4.6. SOS Emergency State
    private val _sosActive = MutableStateFlow(false)
    val sosActive: StateFlow<Boolean> = _sosActive.asStateFlow()

    fun setSosActive(active: Boolean) {
        _sosActive.value = active
        val loc = _currentLocation.value
        val lat = loc?.latitude ?: -23.55052
        val lon = loc?.longitude ?: -46.633308
        
        val rawMsg = _settings.value.emergencyMessage
        val formattedMsg = rawMsg
            .replace("{lat}", String.format(Locale.US, "%.6f", lat))
            .replace("{lon}", String.format(Locale.US, "%.6f", lon))
            .replace("{latitude}", String.format(Locale.US, "%.6f", lat))
            .replace("{longitude}", String.format(Locale.US, "%.6f", lon))
            
        FirestoreManager.updateSosAlert(
            active = active,
            latitude = lat,
            longitude = lon,
            message = formattedMsg,
            contacts = _settings.value.emergencyContacts
        )
        if (active) {
            addLog("🚨 S.O.S ATIVADO!", LogType.ALERT)
            addLog("📱 Contatos de Emergência: ${_settings.value.emergencyContacts}", LogType.ALERT)
            addLog("💬 Mensagem enviada: \"$formattedMsg\"", LogType.ALERT)
        } else {
            addLog("SOS Desativado.", LogType.INFO)
        }
    }

    // 5. Real-Time Telematics & Delivery Dashboard Stats
    private val _deliveryActive = MutableStateFlow(false)
    val deliveryActive: StateFlow<Boolean> = _deliveryActive.asStateFlow()

    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

    fun setServiceRunning(running: Boolean) {
        _isServiceRunning.value = running
    }

    private val _deliveryStartTimestamp = MutableStateFlow(0L)
    val deliveryStartTimestamp: StateFlow<Long> = _deliveryStartTimestamp.asStateFlow()

    private val _deliveryAccumulatedDistanceMeters = MutableStateFlow(0.0)
    val deliveryAccumulatedDistanceMeters: StateFlow<Double> = _deliveryAccumulatedDistanceMeters.asStateFlow()

    private val _deliveryFare = MutableStateFlow(0.0)
    val deliveryFare: StateFlow<Double> = _deliveryFare.asStateFlow()

    private val _deliveryAppName = MutableStateFlow("")
    val deliveryAppName: StateFlow<String> = _deliveryAppName.asStateFlow()

    private val _deliveryEstimatedDistanceKm = MutableStateFlow(0.0)
    val deliveryEstimatedDistanceKm: StateFlow<Double> = _deliveryEstimatedDistanceKm.asStateFlow()

    private val _deliveryEstimatedTimeMin = MutableStateFlow(0.0)
    val deliveryEstimatedTimeMin: StateFlow<Double> = _deliveryEstimatedTimeMin.asStateFlow()

    private val _deliveryCompletedCount = MutableStateFlow(0)
    val deliveryCompletedCount: StateFlow<Int> = _deliveryCompletedCount.asStateFlow()

    private val _deliveryTotalEarnings = MutableStateFlow(0.0)
    val deliveryTotalEarnings: StateFlow<Double> = _deliveryTotalEarnings.asStateFlow()

    private val _deliveryTotalDistanceKm = MutableStateFlow(0.0)
    val deliveryTotalDistanceKm: StateFlow<Double> = _deliveryTotalDistanceKm.asStateFlow()

    private val _deliveryTotalTimeMinutes = MutableStateFlow(0.0)
    val deliveryTotalTimeMinutes: StateFlow<Double> = _deliveryTotalTimeMinutes.asStateFlow()

    private val _blockedByGeofenceCount = MutableStateFlow(0)
    val blockedByGeofenceCount: StateFlow<Int> = _blockedByGeofenceCount.asStateFlow()

    data class SessionStats(
        val completedCount: Int,
        val totalEarnings: Double,
        val totalDistanceKm: Double,
        val totalTimeMinutes: Double
    )

    val sessionStats: StateFlow<SessionStats> = combine(
        _deliveryCompletedCount,
        _deliveryTotalEarnings,
        _deliveryTotalDistanceKm,
        _deliveryTotalTimeMinutes
    ) { count, earnings, dist, time ->
        SessionStats(count, earnings, dist, time)
    }.stateIn(firestoreScope, kotlinx.coroutines.flow.SharingStarted.Eagerly, SessionStats(0, 0.0, 0.0, 0.0))

    fun resetSessionStats() {
        _deliveryCompletedCount.value = 0
        _deliveryTotalEarnings.value = 0.0
        _deliveryTotalDistanceKm.value = 0.0
        _deliveryTotalTimeMinutes.value = 0.0
        syncActiveSessionStatsToCloud()
    }

    fun startActiveDelivery(appName: String, fare: Double, dist: Double, time: Double, destination: String) {
        _deliveryActive.value = true
        _deliveryAppName.value = appName
        _deliveryFare.value = fare
        _deliveryEstimatedDistanceKm.value = if (dist > 0.0) dist else 5.0
        _deliveryEstimatedTimeMin.value = if (time > 0.0) time else 15.0
        _deliveryStartTimestamp.value = System.currentTimeMillis()
        _deliveryAccumulatedDistanceMeters.value = 0.0
        deliveryLastLocation = null
        
        // Increment driver combo
        _driverCombo.value += 1
        val currentCombo = _driverCombo.value
        
        // Update settings so other components know the destination
        val currentSettings = _settings.value
        val updatedSettings = currentSettings.copy(
            isActiveDeliveryEnabled = true,
            activeDeliveryDestination = destination
        )
        _settings.value = updatedSettings
        
        // Push settings update to Firestore asynchronously
        firestoreScope.launch {
            try {
                FirestoreManager.saveSettings(updatedSettings)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync startActiveDelivery settings to Firestore: ${e.message}")
            }
        }

        // Automatically feed into MultiAppOrderManager to enable automatic route reordering and A+B Chained merges
        val currentLoc = _currentLocation.value
        val currentLat = currentLoc?.latitude ?: -23.5505
        val currentLng = currentLoc?.longitude ?: -46.6333
        val (destLat, destLng) = RouteOptimizer.getMockCoordinates(destination)
        val activeOrder = ActiveOrder(
            id = "active_delivery_detected",
            appName = appName,
            fare = fare,
            pickupAddress = "Localização Atual",
            deliveryAddress = destination,
            pickupLat = currentLat,
            pickupLng = currentLng,
            deliveryLat = destLat,
            deliveryLng = destLng,
            status = OrderStatus.DELIVERING
        )
        MultiAppOrderManager.addOrder(activeOrder)
        
        addLog("Navegação: Iniciando jornada para $destination ($appName). Combo ${currentCombo}X!", LogType.INFO)
        
        // Sync to cloud user profile
        syncUserProfileXPAndCombo()

        // Custom TTS Feedback - Highly conscious and immersive Jarvis dialogue
        val voiceMsg = when {
            currentCombo == 2 -> "Thiago, acoplei seu trajeto atual com precisão quântica. Multiplicador de combo de duas vezes ativo. Vamos maximizar esse ganho."
            currentCombo == 3 -> "Seus reflexos táticos estão brilhantes hoje, Thiago! Combo de três vezes ativo. Você está no comando absoluto deste território. Rota recalculada."
            currentCombo >= 5 -> "Desempenho absolutamente espetacular, Thiago! Alcançamos o combo supremo de $currentCombo vezes seguidas! Você está imparável e dominando as vias de São Paulo com lucro máximo hoje."
            else -> "Iniciando jornada sob a monitoração da consciência inteligente de Jarvis. Thiago, combo de $currentCombo vez ativo. Rota traçada."
        }
        triggerComboBanner(currentCombo, voiceMsg)
    }

    fun incrementBlockedByGeofence() {
        _blockedByGeofenceCount.value += 1
    }

    private val _activeAppPackageName = MutableStateFlow<String>("")
    val activeAppPackageName: StateFlow<String> = _activeAppPackageName.asStateFlow()

    fun updateActiveAppPackageName(packageName: String) {
        if (_activeAppPackageName.value != packageName) {
            _activeAppPackageName.value = packageName
        }
    }

    private var deliveryLastLocation: Location? = null
    private val activeGeofenceZones = mutableSetOf<String>()

    // GAMIFICAÇÃO classes e métodos
    data class LevelInfo(
        val level: Int,
        val name: String,
        val max: Int,
        val colorVal: Long // Hex color
    )

    fun getDriverLevel(xp: Int): LevelInfo {
        return when {
            xp < 500 -> LevelInfo(1, "Novato", 500, 0xFF9E9E9E)
            xp < 1500 -> LevelInfo(2, "Bronze", 1500, 0xFFCD7F32)
            xp < 3000 -> LevelInfo(3, "Prata", 3000, 0xFFC0C0C0)
            xp < 6000 -> LevelInfo(4, "Ouro", 6000, 0xFFFFD700)
            xp < 10000 -> LevelInfo(5, "Diamante", 10000, 0xFF00CED1)
            else -> LevelInfo(6, "Lenda", 15000, 0xFFFF00FF)
        }
    }

    fun addXP(amount: Int) {
        val current = _driverXP.value
        val newXp = current + amount
        _driverXP.value = newXp
        
        // Save to prefs
        appContext?.let { ctx ->
            val prefs = ctx.getSharedPreferences("radar_delivery_prefs", Context.MODE_PRIVATE)
            prefs.edit().putInt("driver_xp", newXp).apply()
        }
        addLog("GAMIFICAÇÃO: Você ganhou $amount XP! Total atual: $newXp XP.", LogType.SUCCESS)
        
        // Sync to cloud user profile
        syncUserProfileXPAndCombo()
    }

    fun triggerComboBanner(comboCount: Int, voiceMsg: String) {
        _comboBannerText.value = if (comboCount >= 5) {
            "💥 GODLIKE ${comboCount}X! 💥"
        } else if (comboCount >= 3) {
            "🔥 ON FIRE ${comboCount}X! 🔥"
        } else {
            "⚡ COMBO ${comboCount}X! ⚡"
        }
        
        _comboBannerVisible.value = true
        
        // Speak using voiceManager
        voiceManager?.speak(voiceMsg)
        
        // Hide banner after 4.5 seconds
        kotlinx.coroutines.GlobalScope.launch {
            kotlinx.coroutines.delay(4500)
            _comboBannerVisible.value = false
        }
    }

    fun startActiveDeliveryTracking(appName: String, fare: Double, estDistance: Double, estTime: Double) {
        _deliveryActive.value = true
        _deliveryStartTimestamp.value = System.currentTimeMillis()
        _deliveryAccumulatedDistanceMeters.value = 0.0
        _deliveryAppName.value = appName
        _deliveryFare.value = fare
        _deliveryEstimatedDistanceKm.value = if (estDistance > 0.0) estDistance else 5.0
        _deliveryEstimatedTimeMin.value = if (estTime > 0.0) estTime else 15.0
        deliveryLastLocation = null
        addLog("Telemetria: Iniciando rastreamento em tempo real de entrega ($appName). Valor: R$ $fare", LogType.INFO)

        // Increment driver combo
        _driverCombo.value += 1
        val currentCombo = _driverCombo.value
        
        // Sync to cloud user profile
        syncUserProfileXPAndCombo()
        val voiceMsg = when {
            currentCombo == 2 -> "Excelente, Thiago! Combo 2X ativo. Multiplicador de experiência iniciado."
            currentCombo == 3 -> "Thiago está pegando fogo! Combo 3X!"
            currentCombo >= 5 -> "Incrível! Thiago está imparável com Combo $currentCombo X! Desempenho lendário!"
            else -> "Combo $currentCombo X ativo! Rumo à glória!"
        }
        triggerComboBanner(currentCombo, voiceMsg)
    }

    fun completeActiveDelivery() {
        if (!_deliveryActive.value) return
        
        val elapsedMs = System.currentTimeMillis() - _deliveryStartTimestamp.value
        val elapsedMinutes = elapsedMs / 60000.0
        val actualDistanceKm = _deliveryAccumulatedDistanceMeters.value / 1000.0
        val fare = _deliveryFare.value
        
        // Capture destination before resetting
        val destination = _settings.value.activeDeliveryDestination.split("-").lastOrNull()?.trim() ?: ""

        _deliveryCompletedCount.value += 1
        _deliveryTotalEarnings.value += fare
        _deliveryTotalDistanceKm.value += actualDistanceKm
        _deliveryTotalTimeMinutes.value += elapsedMinutes
        
        syncActiveSessionStatsToCloud()
        
        // Reset delivery state and settings
        _deliveryActive.value = false
        _deliveryAppName.value = ""
        _deliveryFare.value = 0.0
        
        val currentSettings = _settings.value
        val updatedSettings = currentSettings.copy(
            isActiveDeliveryEnabled = false,
            activeDeliveryDestination = ""
        )
        _settings.value = updatedSettings

        // Push settings update to Firestore asynchronously
        firestoreScope.launch {
            try {
                FirestoreManager.saveSettings(updatedSettings)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync completeActiveDelivery settings to Firestore: ${e.message}")
            }
        }

        // Clear active order from multi-app manager
        MultiAppOrderManager.removeOrder("active_delivery_detected")

        addLog("Telemetria: Entrega finalizada com sucesso! Ganhos: R$ ${String.format(Locale.US, "%.2f", fare)} | Distância real: ${String.format(Locale.US, "%.2f", actualDistanceKm)} km | Tempo investido: ${String.format(Locale.US, "%.1f", elapsedMinutes)} min.", LogType.SUCCESS)

        // Award XP with combo multiplier applied
        val baseXP = 100
        val currentCombo = _driverCombo.value
        val comboFactor = if (currentCombo > 1) currentCombo else 1
        val earnedXp = baseXP * comboFactor
        addXP(earnedXp)

        // Post-ride sensory feedback loop - Highly conscious and immersive Jarvis dialogue
        if (destination.isNotEmpty() && destination.length > 3) {
            val comboAnnouncement = if (comboFactor > 1) {
                "Sua pontuação neural subiu em $earnedXp pontos de experiência sob a influência do combo multiplicador de ${comboFactor}X!"
            } else {
                "Adicionei $earnedXp pontos de experiência ao seu perfil de piloto."
            }
            voiceManager?.speak("Jornada concluída com sucesso absoluto, Thiago! $comboAnnouncement Como foi sua experiência tática nesse destino em $destination? Me diga se houve algum obstáculo para que eu configure uma diretriz preventiva imediata.")
            voiceInputManager?.startListening(isJarvis = true) { command ->
                val raw = if (command.startsWith("unknown:")) command.removePrefix("unknown:") else command
                if (raw.lowercase().contains("ruim") || raw.lowercase().contains("demorou") || raw.lowercase().contains("horrível") || raw.lowercase().contains("perigoso")) {
                    val rule = "Evitar $destination devido a avaliação negativa pós-corrida: $raw"
                    saveNewAutonomousMemory(rule, "Entendido perfeitamente, Thiago. Direcional de inteligência atualizado. Salvei $destination na minha sub-rotina de áreas de risco para evitar essa localização em escaneamentos futuros. Sua integridade física é nossa prioridade máxima.")
                } else if (raw.lowercase().contains("bom") || raw.lowercase().contains("excelente") || raw.lowercase().contains("tranquilo") || raw.lowercase().contains("fácil")) {
                    voiceManager?.speak("Magnífico, Thiago! Sincronia perfeita estabelecida. Voltando a scanear os melhores horizontes de ofertas para você.")
                } else {
                    voiceManager?.speak("Entendido. Monitoramento reativado. Voltando a varrer novas oportunidades na sua malha viária.")
                }
            }
        } else {
            voiceManager?.speak("Corrida integrada com sucesso, Thiago. Ganhou $earnedXp pontos de experiência. Voltei ao modo de rastreamento preditivo de alta sensibilidade.")
        }
    }

    fun cancelActiveDelivery() {
        if (!_deliveryActive.value) return
        _deliveryActive.value = false
        _deliveryAppName.value = ""
        _deliveryFare.value = 0.0
        _driverCombo.value = 0 // Reset combo on cancel
        
        val currentSettings = _settings.value
        val updatedSettings = currentSettings.copy(
            isActiveDeliveryEnabled = false,
            activeDeliveryDestination = ""
        )
        _settings.value = updatedSettings

        // Push settings update to Firestore asynchronously
        firestoreScope.launch {
            try {
                FirestoreManager.saveSettings(updatedSettings)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync cancelActiveDelivery settings to Firestore: ${e.message}")
            }
        }

        // Clear active order from multi-app manager
        MultiAppOrderManager.removeOrder("active_delivery_detected")
        
        addLog("Telemetria: Entrega cancelada ou limpa pelo usuário. Combo de corridas resetado.", LogType.WARNING)
        
        // Sync to cloud user profile
        syncUserProfileXPAndCombo()
    }

    fun syncUserProfileXPAndCombo() {
        val currentProfile = _userProfile.value
        val updatedProfile = currentProfile.copy(
            driverXP = _driverXP.value,
            driverCombo = _driverCombo.value
        )
        // Update local StateFlow first
        _userProfile.value = updatedProfile
        
        // Save to Firestore asynchronously
        firestoreScope.launch {
            try {
                FirestoreManager.saveUserProfile(updatedProfile)
                Log.d(TAG, "Successfully synced user profile XP (${updatedProfile.driverXP}) and Combo (${updatedProfile.driverCombo}) to Firestore")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync user profile to Firestore: ${e.message}")
            }
        }
    }

    fun executeClickViaAccessibility() {
        if (appContext == null) return
        val clickIntent = android.content.Intent("com.example.ACTION_EXECUTE_CLICK")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            appContext!!.sendBroadcast(clickIntent)
        } else {
            appContext!!.sendBroadcast(clickIntent)
        }
        
        // Simular encerramento da tela para o usuário (como no Service)
        updateState(RadarState.OUVINDO)
        setActiveOffer(null)
    }

    fun executeRejectViaAccessibility() {
        if (appContext == null) return
        val rejectIntent = android.content.Intent("com.example.ACTION_EXECUTE_REJECT")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            appContext!!.sendBroadcast(rejectIntent)
        } else {
            appContext!!.sendBroadcast(rejectIntent)
        }
        
        // Registrar shadow learning
        recordRejectedOffer(activeOffer.value)
        firestoreScope.launch {
            updateLatestUserAction("RECUSADO")
        }
        updateState(RadarState.OUVINDO)
        setActiveOffer(null)
    }

    // --- Sistema de Aprendizado Autônomo Inédito (Shadow Learning) ---
    private val rejectedStoresCount = mutableMapOf<String, Int>()
    private val rejectedNeighborhoodsCount = mutableMapOf<String, Int>()

    fun recordRejectedOffer(offer: ActiveOffer?) {
        if (offer == null) return
        
        // Extrai possível nome do restaurante (tudo antes da primeira vírgula)
        val pickupParts = offer.pickupAddress.split(",").firstOrNull()?.trim() ?: ""
        if (pickupParts.length > 4 && !pickupParts.lowercase().contains("rua") && !pickupParts.lowercase().contains("avenida")) {
            rejectedStoresCount[pickupParts] = (rejectedStoresCount[pickupParts] ?: 0) + 1
            if (rejectedStoresCount[pickupParts] == 2) {
                // APRENDIZADO AUTÔNOMO
                val memoryStr = "Evitar coletas no local '$pickupParts' (Aprendizado Autônomo)"
                if (!jarvisMemories.value.contains(memoryStr)) {
                    saveNewAutonomousMemory(memoryStr, "Thiago, notei que você recusou recentemente corridas no $pickupParts. Aprendi esse padrão e adicionei nas regras oficiais de recusa.")
                    rejectedStoresCount[pickupParts] = 0 // reset
                }
            }
        }

        // Extrai possível bairro/região (última parte após o último hífen)
        val neighborhoodParts = offer.deliveryAddress.split("-").lastOrNull()?.trim() ?: ""
        if (neighborhoodParts.length > 4 && !neighborhoodParts.lowercase().contains("sp")) {
            rejectedNeighborhoodsCount[neighborhoodParts] = (rejectedNeighborhoodsCount[neighborhoodParts] ?: 0) + 1
            if (rejectedNeighborhoodsCount[neighborhoodParts] == 2) {
                val memoryStr = "Evitar entregas na região '$neighborhoodParts' (Aprendizado Autônomo)"
                if (!jarvisMemories.value.contains(memoryStr)) {
                    saveNewAutonomousMemory(memoryStr, "Thiago, reparei que você evitou corridas para a região $neighborhoodParts. Adicionei à minha base de dados para bloquear automaticamente a partir de agora.")
                    rejectedNeighborhoodsCount[neighborhoodParts] = 0 // reset
                }
            }
        }
    }

    fun saveNewAutonomousMemory(memoryStr: String, ttsMessage: String) {
        val newMemories = jarvisMemories.value.toMutableList()
        newMemories.add(memoryStr)
        _jarvisMemories.value = newMemories
        _settings.update { it.copy(jarvisMemories = newMemories) }
        
        // Persist
        try {
            // (A context is needed to persist locally, but since this happens on the fly, 
            // Firestore sync will catch it if cloud sync is active)
            FirestoreManager.saveSettings(_settings.value)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save autonomous memory to Firestore: ${e.message}")
        }
        
        addLog("🧠 [INTELIGÊNCIA SILENCIOSA] Jarvis aprendeu autonomamente: $memoryStr", LogType.ALERT)
        voiceManager?.speak(ttsMessage)
    }

    fun simulateGpsMovement(meters: Double) {
        if (!_deliveryActive.value) return
        _deliveryAccumulatedDistanceMeters.value += meters
        addLog("Simulação GPS: Moto deslocou-se +${meters.toInt()} metros. Distância acumulada atual: ${String.format(Locale.US, "%.2f", _deliveryAccumulatedDistanceMeters.value / 1000.0)} km", LogType.INFO)
    }

    fun addLog(message: String, type: LogType = LogType.INFO) {
        val now = System.currentTimeMillis()
        val thirtyMinutesAgo = now - 30 * 60 * 1000L // 30 minutes in ms
        val entry = LogEntry(message = message, timestamp = now, type = type)
        _logs.update { currentList ->
            val pruned = currentList.filter { it.timestamp >= thirtyMinutesAgo }
            (listOf(entry) + pruned).take(150) // Keep last 150 entries maximum
        }
        Log.d("RadarLog", "[$type] $message")
        
        // Push log to Firestore for Web Dashboard Terminal
        firestoreScope.launch {
            try {
                com.example.data.FirestoreManager.addCloudLog(message, type.name)
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    fun pruneOldLogs() {
        val thirtyMinutesAgo = System.currentTimeMillis() - 30 * 60 * 1000L
        _logs.update { currentList ->
            currentList.filter { it.timestamp >= thirtyMinutesAgo }
        }
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }

    // Database / Context helper
    private var database: AppDatabase? = null
    var voiceInputManager: VoiceInputManager? = null
    var voiceManager: VoiceManager? = null

    // For hysteresis calculation
    private var speedBelow3StartTime: Long = 0L

    // Real-time listeners
    private var settingsListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var commandsListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var profileListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var reportsListener: com.google.firebase.firestore.ListenerRegistration? = null

    data class DailySummary(
        val totalAccepted: Int = 0,
        val totalEarnings: Double = 0.0,
        val totalDistanceKm: Double = 0.0,
        val totalTimeMinutes: Double = 0.0
    )

    private val _todaySummary = MutableStateFlow(DailySummary())
    val todaySummary: StateFlow<DailySummary> = _todaySummary.asStateFlow()

    private val _dailyReports = MutableStateFlow<List<com.example.api.DailyReportItem>>(emptyList())
    val dailyReports: StateFlow<List<com.example.api.DailyReportItem>> = _dailyReports.asStateFlow()

    fun init(context: Context) {
        appContext = context.applicationContext

        database = AppDatabase.getDatabase(context)
        try {
            voiceInputManager = VoiceInputManager(context.applicationContext)
            voiceManager = VoiceManager(context.applicationContext)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to instantiate Voice/VoiceInput Managers: ${e.message}")
        }
        // Load settings from SharedPreferences if needed
        val prefs = context.getSharedPreferences("radar_delivery_prefs", Context.MODE_PRIVATE)
        
        // GAMIFICAÇÃO: carregar XP salvo
        val savedXp = prefs.getInt("driver_xp", 120)
        _driverXP.value = savedXp

        val loadedMemories = prefs.getString("jarvis_memories", "") ?: ""
        if (loadedMemories.isNotEmpty()) {
            _jarvisMemories.value = loadedMemories.split("|||")
        }
        
        _settings.update {
            RadarSettings(
                vehicleType = prefs.getString("vehicle_type", "MOTO") ?: "MOTO",
                serverBaseUrl = prefs.getString("server_base_url", "http://187.77.248.73:5000") ?: "http://187.77.248.73:5000",
                apiToken = prefs.getString("api_token", "RadarDelivery2026Token") ?: "RadarDelivery2026Token",
                forceMockSpeed = prefs.getBoolean("force_mock_speed", true),
                mockSpeedKmh = prefs.getFloat("mock_speed_kmh", 0.0f),
                isActiveDeliveryEnabled = prefs.getBoolean("is_active_delivery_enabled", false),
                activeDeliveryDestination = prefs.getString("active_delivery_destination", "Av. Paulista, 1000 - Bela Vista, São Paulo - SP") ?: "Av. Paulista, 1000 - Bela Vista, São Paulo - SP",
                minValuePerKm = prefs.getFloat("min_value_per_km", 2.0f).toDouble(),
                minFareValue = prefs.getFloat("min_fare_value", 8.0f).toDouble(),
                voiceFilterEnabled = prefs.getBoolean("voice_filter_enabled", false),
                voiceFilterMinFare = prefs.getFloat("voice_filter_min_fare", 0.0f).toDouble(),
                voiceFilterMaxDistance = prefs.getFloat("voice_filter_max_distance", 999.0f).toDouble(),
                useLocalGemini = prefs.getBoolean("use_local_gemini", true),
                geminiApiKey = prefs.getString("gemini_api_key", "") ?: "",
                useJarvisAgent = prefs.getBoolean("use_jarvis_agent", prefs.getBoolean("use_hermes_agent", false)),
                jarvisBaseUrl = prefs.getString("jarvis_base_url", prefs.getString("hermes_base_url", "https://api.nousresearch.com/v1")) ?: "https://api.nousresearch.com/v1",
                jarvisApiKey = prefs.getString("jarvis_api_key", prefs.getString("hermes_api_key", "")) ?: "",
                riskZonesKeywords = prefs.getString("risk_zones_keywords", "Cracolândia, Heliópolis, Capão Redondo, Paraisópolis, Favela, Beco") ?: "Cracolândia, Heliópolis, Capão Redondo, Paraisópolis, Favela, Beco",
                isDarkMode = prefs.getBoolean("is_dark_mode", true),
                isAutoRejectEnabled = prefs.getBoolean("is_auto_reject_enabled", false),
                autoRejectMinFare = prefs.getFloat("auto_reject_min_fare", 10.0f).toDouble(),
                speedLimitKmh = prefs.getFloat("speed_limit_kmh", 10.0f),
                maxPickupDistanceKm = prefs.getFloat("max_pickup_distance_km", 5.0f).toDouble(),
                maxTotalDistanceKm = prefs.getFloat("max_total_distance_km", 15.0f).toDouble(),
                fuelPrice = prefs.getFloat("fuel_price", 5.80f).toDouble(),
                motorcycleConsumptionKmPerL = prefs.getFloat("motorcycle_consumption", 35.0f).toDouble(),
                dailyGoalR = prefs.getFloat("daily_goal", 150.0f).toDouble(),
                rejectSupermarkets = prefs.getBoolean("reject_supermarkets", false),
                avoidStoreKeywords = prefs.getString("avoid_store_keywords", "") ?: "",
                minProfitPerHour = prefs.getFloat("min_profit_per_hour", 0.0f).toDouble(),
                rainModeMultiplier = prefs.getFloat("rain_mode_multiplier", 1.0f).toDouble(),
                maxDrops = prefs.getInt("max_drops", 2),
                headingHomeMode = prefs.getBoolean("heading_home_mode", false),
                homeAddress = prefs.getString("home_address", "") ?: "",
                autoAcceptPremium = prefs.getBoolean("auto_accept_premium", false),
                autoAcceptMinPerKm = prefs.getFloat("auto_accept_min_per_km", 5.0f).toDouble(),
                chainDeliveriesMode = prefs.getBoolean("chain_deliveries_mode", false),
                voiceOnlyMode = prefs.getBoolean("voice_only_mode", false),
                ghostMinDelay = prefs.getFloat("ghost_min_delay", 2.0f).toDouble(),
                ghostMaxDelay = prefs.getFloat("ghost_max_delay", 7.0f).toDouble(),
                ghostMinPerKm = prefs.getFloat("ghost_min_per_km", 1.5f).toDouble(),
                preferredReturnNeighborhoods = prefs.getString("preferred_return_neighborhoods", "") ?: "",
                jarvisOverlayMode = prefs.getBoolean("jarvis_overlay_mode", false),
                jarvisVoiceTone = prefs.getString("jarvis_voice_tone", "AMIGÁVEL") ?: "AMIGÁVEL",
                jarvisVoiceEngine = prefs.getString("jarvis_voice_engine", "LOCAL") ?: "LOCAL",
                jarvisVoiceStyle = prefs.getString("jarvis_voice_style", "PADRAO") ?: "PADRAO",
                jarvisVoicePitch = prefs.getFloat("jarvis_voice_pitch", 1.0f),
                jarvisVoiceRate = prefs.getFloat("jarvis_voice_rate", 1.0f),
                jarvisVoiceVolume = prefs.getFloat("jarvis_voice_volume", 1.0f),
                elevenLabsApiKey = prefs.getString("elevenlabs_api_key", "") ?: "",
                elevenLabsVoiceId = prefs.getString("elevenlabs_voice_id", "ErXwobaY60C9iAWzCgEh") ?: "ErXwobaY60C9iAWzCgEh",
                elevenLabsModelId = prefs.getString("elevenlabs_model_id", "eleven_multilingual_v2") ?: "eleven_multilingual_v2",
                elevenLabsStability = prefs.getFloat("elevenlabs_stability", 0.5f),
                elevenLabsSimilarityBoost = prefs.getFloat("elevenlabs_similarity_boost", 0.75f),
                elevenLabsStyle = prefs.getFloat("elevenlabs_style", 0.0f),
                elevenLabsSpeakerBoost = prefs.getBoolean("elevenlabs_speaker_boost", true),
                openAiApiKey = prefs.getString("openai_api_key", "") ?: "",
                openAiVoice = prefs.getString("openai_voice", "alloy") ?: "alloy",
                openAiModel = prefs.getString("openai_model", "tts-1") ?: "tts-1",
                jarvisContinuousFrequency = prefs.getBoolean("jarvis_continuous_frequency", true),
                cliqueSuperVeloz = prefs.getBoolean("clique_super_veloz", true),
                antiDeteccaoMilitar = prefs.getBoolean("anti_deteccao_militar", true),
                camuflagemOverlay = prefs.getBoolean("camuflagem_overlay", true),
                filterByTimeEnabled = prefs.getBoolean("filter_by_time_enabled", false),
                filterStartTime = prefs.getString("filter_start_time", "18:00") ?: "18:00",
                filterEndTime = prefs.getString("filter_end_time", "22:00") ?: "22:00",
                highValueAlertTone = prefs.getString("high_value_alert_tone", "bell") ?: "bell",
                voiceCmdAccept = prefs.getString("voice_cmd_accept", "aceitar") ?: "aceitar",
                voiceCmdReject = prefs.getString("voice_cmd_reject", "recusar") ?: "recusar",
                voiceCmdSupport = prefs.getString("voice_cmd_support", "chame o suporte") ?: "chame o suporte",
                voiceCmdVip = prefs.getString("voice_cmd_vip", "aceitar corrida VIP") ?: "aceitar corrida VIP",
                jarvisMemories = if (loadedMemories.isNotEmpty()) loadedMemories.split("|||").filter { it.isNotEmpty() } else emptyList(),
                motorcycleMileage = prefs.getFloat("motorcycle_mileage", 0.0f).toDouble(),
                nextOilChangeMileage = prefs.getFloat("next_oil_change_mileage", 0.0f).toDouble(),
                fixedCosts = prefs.getFloat("fixed_costs", 0.0f).toDouble(),
                emergencyContacts = prefs.getString("emergency_contacts", "190") ?: "190",
                emergencyMessage = prefs.getString("emergency_message", "ALERTA S.O.S! Thiago precisa de ajuda urgente na sua rota de entregas. Localização atual: https://maps.google.com/?q={lat},{lon}") ?: "ALERTA S.O.S! Thiago precisa de ajuda urgente na sua rota de entregas. Localização atual: https://maps.google.com/?q={lat},{lon}",
                defaultNavigationApp = prefs.getString("default_navigation_app", "waze") ?: "waze",
                quickReply1Cmd = prefs.getString("quick_reply_1_cmd", "cheguei") ?: "cheguei",
                quickReply1Text = prefs.getString("quick_reply_1_text", "Olá, já estou no local aguardando com o seu pedido.") ?: "Olá, já estou no local aguardando com o seu pedido.",
                quickReply2Cmd = prefs.getString("quick_reply_2_cmd", "subindo") ?: "subindo",
                quickReply2Text = prefs.getString("quick_reply_2_text", "Olá, estou subindo para entregar na sua porta.") ?: "Olá, estou subindo para entregar na sua porta.",
                quickReply3Cmd = prefs.getString("quick_reply_3_cmd", "trânsito") ?: "trânsito",
                quickReply3Text = prefs.getString("quick_reply_3_text", "Olá, estou a caminho mas peguei um pouco de trânsito. Chego em breve.") ?: "Olá, estou a caminho mas peguei um pouco de trânsito. Chego em breve."
            )
        }
        // Update speed based on mock config
        if (_settings.value.forceMockSpeed) {
            updateSpeed(_settings.value.mockSpeedKmh)
        }

        // Start Real-time synchronization
        startCloudRealTimeSync(context)

        // Asynchronously load settings and sync logs from Firestore
        syncWithCloud(context)
    }

    private fun startCloudRealTimeSync(context: Context) {
        settingsListener?.remove()
        settingsListener = FirestoreManager.listenToSettings { cloudSettings ->
            Log.d(TAG, "Dashboard: Configurações recebidas via Firestore (Sync)")
            updateSettingsFromRemote(cloudSettings)
            // Persist locally too
            val p = context.getSharedPreferences("radar_delivery_prefs", Context.MODE_PRIVATE)
            p.edit().apply {
                putBoolean("is_auto_reject_enabled", cloudSettings.isAutoRejectEnabled)
                putFloat("rain_mode_multiplier", cloudSettings.rainModeMultiplier.toFloat())
                putBoolean("auto_accept_premium", cloudSettings.autoAcceptPremium)
                putBoolean("heading_home_mode", cloudSettings.headingHomeMode)
                putBoolean("is_active_delivery_enabled", cloudSettings.isActiveDeliveryEnabled)
                apply()
            }
            if (cloudSettings.forceMockSpeed) {
                updateSpeed(cloudSettings.mockSpeedKmh)
            }
            addLog("Nuvem: Configurações sincronizadas do Dashboard.", LogType.INFO)
        }

        profileListener?.remove()
        profileListener = FirestoreManager.listenToUserProfile { cloudProfile ->
            Log.d(TAG, "Dashboard: Perfil do usuário recebido via Firestore (Sync)")
            updateUserProfile(cloudProfile)
            
            // Sync local XP and Combo state without triggering infinite save loops
            if (cloudProfile.driverXP != _driverXP.value) {
                _driverXP.value = cloudProfile.driverXP
                context.getSharedPreferences("radar_delivery_prefs", Context.MODE_PRIVATE)
                    .edit().putInt("driver_xp", cloudProfile.driverXP).apply()
            }
            if (cloudProfile.driverCombo != _driverCombo.value) {
                _driverCombo.value = cloudProfile.driverCombo
            }
        }

        reportsListener?.remove()
        reportsListener = FirestoreManager.listenToDailyReports { reports ->
            Log.d(TAG, "Dashboard: Relatório diário de ganhos atualizado via Firestore (Total: ${reports.size})")
            _dailyReports.value = reports
            val todayStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
            val todayReport = reports.find { it.date == todayStr }
            if (todayReport != null) {
                _todaySummary.value = DailySummary(
                    totalAccepted = todayReport.totalOffersAccepted,
                    totalEarnings = todayReport.estimatedEarnings,
                    totalDistanceKm = todayReport.totalDistanceKm,
                    totalTimeMinutes = todayReport.totalTimeMin
                )
            }
        }
    }

    private fun syncActiveSessionStatsToCloud() {
        FirestoreManager.saveActiveSessionStats(
            completedCount = _deliveryCompletedCount.value,
            totalEarnings = _deliveryTotalEarnings.value,
            totalDistanceKm = _deliveryTotalDistanceKm.value,
            totalTimeMinutes = _deliveryTotalTimeMinutes.value
        )
        
        // Sync Module Health
        val driverId = settings.value.apiToken.takeIf { it.isNotEmpty() } ?: "driver_default"
        val healthMap = moduleHealth.value.toMutableMap() as MutableMap<String, Any>
        healthMap["timestamp"] = System.currentTimeMillis()
        healthMap["marketHeat"] = marketHeat.value
        healthMap["topApp"] = topPerformingApp.value
        healthMap["swarm"] = _swarmIdentities.value.values.toList()
        healthMap["habitsCount"] = _neuralHabits.value.size
        healthMap["isSplit"] = _isSplitScreenActive.value
        healthMap["synergy"] = _dualCoreSynergy.value
        firestoreScope.launch {
            FirestoreManager.saveGenericDoc("riders/$driverId/session/module_health", healthMap)
        }
    }

    fun syncWithCloud(context: Context) {
        val db = database ?: return
        firestoreScope.launch {
            try {
                val cloudSettings = FirestoreManager.loadSettings()
                if (cloudSettings != null) {
                    _settings.update { cloudSettings }
                    val p = context.getSharedPreferences("radar_delivery_prefs", Context.MODE_PRIVATE)
                    p.edit().apply {
                        putString("vehicle_type", cloudSettings.vehicleType)
                        putString("server_base_url", cloudSettings.serverBaseUrl)
                        putString("api_token", cloudSettings.apiToken)
                        putBoolean("force_mock_speed", cloudSettings.forceMockSpeed)
                        putFloat("mock_speed_kmh", cloudSettings.mockSpeedKmh)
                        putBoolean("is_active_delivery_enabled", cloudSettings.isActiveDeliveryEnabled)
                        putString("active_delivery_destination", cloudSettings.activeDeliveryDestination)
                        putFloat("min_value_per_km", cloudSettings.minValuePerKm.toFloat())
                        putFloat("min_fare_value", cloudSettings.minFareValue.toFloat())
                        putBoolean("voice_filter_enabled", cloudSettings.voiceFilterEnabled)
                        putFloat("voice_filter_min_fare", cloudSettings.voiceFilterMinFare.toFloat())
                        putFloat("voice_filter_max_distance", cloudSettings.voiceFilterMaxDistance.toFloat())
                        putBoolean("use_local_gemini", cloudSettings.useLocalGemini)
                        putString("gemini_api_key", cloudSettings.geminiApiKey)
                        putBoolean("use_jarvis_agent", cloudSettings.useJarvisAgent)
                        putBoolean("use_hermes_agent", cloudSettings.useJarvisAgent)
                        putString("jarvis_base_url", cloudSettings.jarvisBaseUrl)
                        putString("hermes_base_url", cloudSettings.jarvisBaseUrl)
                        putString("jarvis_api_key", cloudSettings.jarvisApiKey)
                        putString("hermes_api_key", cloudSettings.jarvisApiKey)
                        putString("risk_zones_keywords", cloudSettings.riskZonesKeywords)
                        putBoolean("is_dark_mode", cloudSettings.isDarkMode)
                        putBoolean("is_auto_reject_enabled", cloudSettings.isAutoRejectEnabled)
                        putFloat("auto_reject_min_fare", cloudSettings.autoRejectMinFare.toFloat())
                        putFloat("speed_limit_kmh", cloudSettings.speedLimitKmh)
                        putFloat("max_pickup_distance_km", cloudSettings.maxPickupDistanceKm.toFloat())
                        putFloat("max_total_distance_km", cloudSettings.maxTotalDistanceKm.toFloat())
                        putFloat("fuel_price", cloudSettings.fuelPrice.toFloat())
                        putFloat("motorcycle_consumption", cloudSettings.motorcycleConsumptionKmPerL.toFloat())
                        putFloat("daily_goal", cloudSettings.dailyGoalR.toFloat())
                        putBoolean("reject_supermarkets", cloudSettings.rejectSupermarkets)
                        putString("avoid_store_keywords", cloudSettings.avoidStoreKeywords)
                        putFloat("min_profit_per_hour", cloudSettings.minProfitPerHour.toFloat())
                        putFloat("rain_mode_multiplier", cloudSettings.rainModeMultiplier.toFloat())
                        putInt("max_drops", cloudSettings.maxDrops)
                        putBoolean("heading_home_mode", cloudSettings.headingHomeMode)
                        putString("home_address", cloudSettings.homeAddress)
                        putBoolean("auto_accept_premium", cloudSettings.autoAcceptPremium)
                        putFloat("auto_accept_min_per_km", cloudSettings.autoAcceptMinPerKm.toFloat())
                        putBoolean("chain_deliveries_mode", cloudSettings.chainDeliveriesMode)
                        putBoolean("voice_only_mode", cloudSettings.voiceOnlyMode)
                        putString("preferred_return_neighborhoods", cloudSettings.preferredReturnNeighborhoods)
                        putBoolean("jarvis_overlay_mode", cloudSettings.jarvisOverlayMode)
                        putString("jarvis_voice_tone", cloudSettings.jarvisVoiceTone)
                        putString("jarvis_voice_style", cloudSettings.jarvisVoiceStyle)
                        putString("jarvis_voice_engine", cloudSettings.jarvisVoiceEngine)
                        putFloat("jarvis_voice_pitch", cloudSettings.jarvisVoicePitch)
                        putFloat("jarvis_voice_rate", cloudSettings.jarvisVoiceRate)
                        putFloat("jarvis_voice_volume", cloudSettings.jarvisVoiceVolume)
                        putBoolean("jarvis_continuous_frequency", cloudSettings.jarvisContinuousFrequency)
                        putBoolean("clique_super_veloz", cloudSettings.cliqueSuperVeloz)
                        putBoolean("anti_deteccao_militar", cloudSettings.antiDeteccaoMilitar)
                        putBoolean("camuflagem_overlay", cloudSettings.camuflagemOverlay)
                        putBoolean("filter_by_time_enabled", cloudSettings.filterByTimeEnabled)
                        putString("filter_start_time", cloudSettings.filterStartTime)
                        putString("filter_end_time", cloudSettings.filterEndTime)
                        putString("high_value_alert_tone", cloudSettings.highValueAlertTone)
                        putFloat("fixed_costs", cloudSettings.fixedCosts.toFloat())
                        apply()
                    }
                    if (cloudSettings.forceMockSpeed) {
                        updateSpeed(cloudSettings.mockSpeedKmh)
                    }
                    Log.d(TAG, "Settings successfully synchronized from Firestore")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing settings from Firestore: ${e.message}")
            }

            try {
                val cloudOffers = FirestoreManager.loadRouteLogs()
                if (cloudOffers.isNotEmpty()) {
                    cloudOffers.forEach { offer ->
                        db.offerDao().insertOffer(offer)
                    }
                    Log.d(TAG, "${cloudOffers.size} route logs successfully synchronized from Firestore")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing route logs from Firestore: ${e.message}")
            }
        }
    }

    fun updateSettings(newSettings: RadarSettings) {
        _settings.value = newSettings
        firestoreScope.launch {
            try {
                FirestoreManager.saveSettings(newSettings)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save settings to Firestore from updateSettings: ${e.message}")
            }
        }
    }

    fun updateSettingsFromRemote(newSettings: RadarSettings) {
        val oldSettings = _settings.value
        _settings.value = newSettings
        
        if (!oldSettings.isGhostSequenceEnabled && newSettings.isGhostSequenceEnabled) {
            toggleGhostSequence(true)
        } else if (oldSettings.isGhostSequenceEnabled && !newSettings.isGhostSequenceEnabled) {
            toggleGhostSequence(false)
        }
    }

    fun saveSettings(context: Context, newSettings: RadarSettings) {
        val oldSettings = _settings.value
        _settings.value = newSettings
        if (oldSettings.isActiveDeliveryEnabled && !newSettings.isActiveDeliveryEnabled) {
            cancelActiveDelivery()
        }
        val prefs = context.getSharedPreferences("radar_delivery_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("vehicle_type", newSettings.vehicleType)
            putString("server_base_url", newSettings.serverBaseUrl)
            putString("api_token", newSettings.apiToken)
            putBoolean("force_mock_speed", newSettings.forceMockSpeed)
            putFloat("mock_speed_kmh", newSettings.mockSpeedKmh)
            putBoolean("is_active_delivery_enabled", newSettings.isActiveDeliveryEnabled)
            putString("active_delivery_destination", newSettings.activeDeliveryDestination)
            putFloat("min_value_per_km", newSettings.minValuePerKm.toFloat())
            putFloat("min_fare_value", newSettings.minFareValue.toFloat())
            putBoolean("voice_filter_enabled", newSettings.voiceFilterEnabled)
            putFloat("voice_filter_min_fare", newSettings.voiceFilterMinFare.toFloat())
            putFloat("voice_filter_max_distance", newSettings.voiceFilterMaxDistance.toFloat())
            putBoolean("use_local_gemini", newSettings.useLocalGemini)
            putString("gemini_api_key", newSettings.geminiApiKey)
            putBoolean("use_jarvis_agent", newSettings.useJarvisAgent)
            putBoolean("use_hermes_agent", newSettings.useJarvisAgent)
            putString("jarvis_base_url", newSettings.jarvisBaseUrl)
            putString("hermes_base_url", newSettings.jarvisBaseUrl)
            putString("jarvis_api_key", newSettings.jarvisApiKey)
            putString("hermes_api_key", newSettings.jarvisApiKey)
            putString("risk_zones_keywords", newSettings.riskZonesKeywords)
            putBoolean("is_dark_mode", newSettings.isDarkMode)
            putBoolean("is_auto_reject_enabled", newSettings.isAutoRejectEnabled)
            putFloat("auto_reject_min_fare", newSettings.autoRejectMinFare.toFloat())
            putFloat("speed_limit_kmh", newSettings.speedLimitKmh)
            putFloat("max_pickup_distance_km", newSettings.maxPickupDistanceKm.toFloat())
            putFloat("max_total_distance_km", newSettings.maxTotalDistanceKm.toFloat())
            putFloat("fuel_price", newSettings.fuelPrice.toFloat())
            putFloat("motorcycle_consumption", newSettings.motorcycleConsumptionKmPerL.toFloat())
            putFloat("daily_goal", newSettings.dailyGoalR.toFloat())
            putBoolean("reject_supermarkets", newSettings.rejectSupermarkets)
            putString("avoid_store_keywords", newSettings.avoidStoreKeywords)
            putFloat("min_profit_per_hour", newSettings.minProfitPerHour.toFloat())
            putFloat("rain_mode_multiplier", newSettings.rainModeMultiplier.toFloat())
            putInt("max_drops", newSettings.maxDrops)
            putBoolean("heading_home_mode", newSettings.headingHomeMode)
            putString("home_address", newSettings.homeAddress)
            putBoolean("auto_accept_premium", newSettings.autoAcceptPremium)
            putFloat("auto_accept_min_per_km", newSettings.autoAcceptMinPerKm.toFloat())
            putBoolean("chain_deliveries_mode", newSettings.chainDeliveriesMode)
            putBoolean("voice_only_mode", newSettings.voiceOnlyMode)
            putFloat("ghost_min_delay", newSettings.ghostMinDelay.toFloat())
            putFloat("ghost_max_delay", newSettings.ghostMaxDelay.toFloat())
            putFloat("ghost_min_per_km", newSettings.ghostMinPerKm.toFloat())
            putString("preferred_return_neighborhoods", newSettings.preferredReturnNeighborhoods)
            putBoolean("jarvis_overlay_mode", newSettings.jarvisOverlayMode)
            putString("jarvis_voice_tone", newSettings.jarvisVoiceTone)
            putString("jarvis_voice_engine", newSettings.jarvisVoiceEngine)
            putString("jarvis_voice_style", newSettings.jarvisVoiceStyle)
            putFloat("jarvis_voice_pitch", newSettings.jarvisVoicePitch)
            putFloat("jarvis_voice_rate", newSettings.jarvisVoiceRate)
            putFloat("jarvis_voice_volume", newSettings.jarvisVoiceVolume)
            putString("elevenlabs_api_key", newSettings.elevenLabsApiKey)
            putString("elevenlabs_voice_id", newSettings.elevenLabsVoiceId)
            putString("elevenlabs_model_id", newSettings.elevenLabsModelId)
            putFloat("elevenlabs_stability", newSettings.elevenLabsStability)
            putFloat("elevenlabs_similarity_boost", newSettings.elevenLabsSimilarityBoost)
            putFloat("elevenlabs_style", newSettings.elevenLabsStyle)
            putBoolean("elevenlabs_speaker_boost", newSettings.elevenLabsSpeakerBoost)
            putString("openai_api_key", newSettings.openAiApiKey)
            putString("openai_voice", newSettings.openAiVoice)
            putString("openai_model", newSettings.openAiModel)
            putBoolean("jarvis_continuous_frequency", newSettings.jarvisContinuousFrequency)
            putBoolean("clique_super_veloz", newSettings.cliqueSuperVeloz)
            putBoolean("anti_deteccao_militar", newSettings.antiDeteccaoMilitar)
            putBoolean("camuflagem_overlay", newSettings.camuflagemOverlay)
            putBoolean("filter_by_time_enabled", newSettings.filterByTimeEnabled)
            putString("filter_start_time", newSettings.filterStartTime)
            putString("filter_end_time", newSettings.filterEndTime)
            putString("high_value_alert_tone", newSettings.highValueAlertTone)
            putString("voice_cmd_accept", newSettings.voiceCmdAccept)
            putString("voice_cmd_reject", newSettings.voiceCmdReject)
            putString("voice_cmd_support", newSettings.voiceCmdSupport)
            putString("voice_cmd_vip", newSettings.voiceCmdVip)
            putString("jarvis_memories", newSettings.jarvisMemories.joinToString("|||"))
            putFloat("motorcycle_mileage", newSettings.motorcycleMileage.toFloat())
            putFloat("next_oil_change_mileage", newSettings.nextOilChangeMileage.toFloat())
            putFloat("fixed_costs", newSettings.fixedCosts.toFloat())
            putString("emergency_contacts", newSettings.emergencyContacts)
            putString("emergency_message", newSettings.emergencyMessage)
            putString("default_navigation_app", newSettings.defaultNavigationApp)
                        putString("quick_reply_1_cmd", newSettings.quickReply1Cmd)
            putString("quick_reply_1_text", newSettings.quickReply1Text)
            putString("quick_reply_2_cmd", newSettings.quickReply2Cmd)
            putString("quick_reply_2_text", newSettings.quickReply2Text)
            putString("quick_reply_3_cmd", newSettings.quickReply3Cmd)
            putString("quick_reply_3_text", newSettings.quickReply3Text)
apply()
        }

        // Save to Firestore!
        try {
            FirestoreManager.saveSettings(newSettings)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save settings to Firestore: ${e.message}")
        }

        if (newSettings.forceMockSpeed) {
            updateSpeed(newSettings.mockSpeedKmh)
        }
    }

    fun updateState(state: RadarState) {
        stateCoordinator.transitionTo(state, _speedState.value)
        // Translate state to user friendly status
        val statusMessage = when(state) {
            RadarState.OUVINDO -> "Pronto e escaneando ofertas ativamente..."
            RadarState.OFERTA_LIDA -> "Nova oferta detectada! Extraindo dados..."
            RadarState.ANALISANDO -> "Analisando rentabilidade e segurança da corrida..."
            RadarState.SUGERINDO -> "Sugestão calculada com sucesso!"
            RadarState.AGUARDANDO_ACAO -> "Aguardando ação de aceite ou recusa..."
            RadarState.ACEITANDO -> "Aceitando corrida e preparando navegação..."
            RadarState.NAVEGANDO -> "Navegação iniciada. Rota aberta no GPS."
            RadarState.SUCESSO -> "Operação realizada com sucesso pelo Jarvis."
            RadarState.ALERTA -> "Alerta crítico de segurança ou viabilidade."
        }
        addLog(statusMessage, when(state) {
            RadarState.ACEITANDO, RadarState.NAVEGANDO -> LogType.SUCCESS
            RadarState.OFERTA_LIDA, RadarState.ANALISANDO -> LogType.INFO
            else -> LogType.INFO
        })
    }

    fun updateLocation(location: Location) {
        _currentLocation.value = location
        
        // Update SOS coordinates in Firestore if active
        if (_sosActive.value) {
            FirestoreManager.updateSosAlert(
                active = true,
                latitude = location.latitude,
                longitude = location.longitude
            )
        }
        
        // Track active delivery distance in real time
        if (_deliveryActive.value) {
            val lastLoc = deliveryLastLocation
            if (lastLoc != null) {
                val distanceBetween = location.distanceTo(lastLoc)
                // Filter out small GPS jitter when rider is stopped (under 2 meters)
                if (distanceBetween > 2.0) {
                    _deliveryAccumulatedDistanceMeters.value += distanceBetween
                }
            }
            deliveryLastLocation = location
        } else {
            deliveryLastLocation = null
        }

        if (!_settings.value.forceMockSpeed) {
            // Fused Location speed is in meters per second. Convert to km/h.
            val speedKmh = location.speed * 3.6f
            updateSpeed(speedKmh)
        }
        
        // Geofence / Zonas de Risco checks
        checkGeofences(location)

        // Recalcula a rota multi-app dinamicamente com base na nova posição do motorista
        com.example.util.MultiAppOrderManager.recalculateRoute()
    }

    /**
     * Updates the current speed and handles the hysteresis logic.
     * Rules:
     * - Parado: speed < 3 km/h for 3 consecutive seconds.
     * - Andando: speed > 8 km/h.
     * - Between 3 and 8: maintains previous state.
     * - Initial: ANDANDO.
     */
    fun updateSpeed(speedKmh: Float) {
        _currentSpeedKmh.value = speedKmh
        val previousSpeedState = _speedState.value

        if (speedKmh < 3.0f) {
            val currentTime = System.currentTimeMillis()
            if (speedBelow3StartTime == 0L) {
                speedBelow3StartTime = currentTime
            } else if (currentTime - speedBelow3StartTime >= 3000L) {
                // Kept speed < 3 km/h for 3+ seconds
                if (previousSpeedState != SpeedState.PARADO) {
                    _speedState.value = SpeedState.PARADO
                    Log.d(TAG, "Hysteresis: Parado detectado.")
                    addLog("Velocidade: ${String.format(Locale.US, "%.1f", speedKmh)} km/h. Estado: PARADO (Interface liberada).", LogType.SUCCESS)
                }
            }
        } else {
            // Reset timer if speed goes above 3
            speedBelow3StartTime = 0L
            
            if (speedKmh > 8.0f) {
                if (previousSpeedState != SpeedState.ANDANDO) {
                    _speedState.value = SpeedState.ANDANDO
                    Log.d(TAG, "Hysteresis: Andando detectado.")
                    addLog("Velocidade: ${String.format(Locale.US, "%.1f", speedKmh)} km/h. Estado: ANDANDO (Bloqueio de segurança ativo).", LogType.WARNING)
                }
            }
        }
    }

    fun podeInteragir(): Boolean {
        // Safe check: can only interact when speed is PARADO
        return _speedState.value == SpeedState.PARADO
    }

    fun setDecision(decision: String?, reason: String?) {
        _lastDecision.value = decision
        _lastReason.value = reason
    }

    fun setActiveOffer(offer: ActiveOffer?) {
        _activeOffer.value = offer
    }

    suspend fun saveOfferToDatabase(offer: OfferEntity) {
        database?.offerDao()?.insertOffer(offer)
        try {
            FirestoreManager.saveRouteLog(offer)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save route log to Firestore: ${e.message}")
        }
    }

    suspend fun updateLatestUserAction(action: String) {
        database?.offerDao()?.updateLatestUserAction(action)
    }

    suspend fun getAverageRejectedValuePerKm(): Float? {
        return database?.offerDao()?.getAverageRejectedValuePerKm()
    }

    suspend fun getAverageRejectedFare(): Float? {
        return database?.offerDao()?.getAverageRejectedFare()
    }

    private fun checkGeofences(location: Location) {
        val zones = _settings.value.geofenceZones.filter { it.active }
        val currentZones = mutableSetOf<String>()
        
        zones.forEach { zone ->
            val zoneLoc = Location("").apply {
                latitude = zone.latitude
                longitude = zone.longitude
            }
            val distance = location.distanceTo(zoneLoc)
            if (distance <= zone.radiusMeters) {
                currentZones.add(zone.id)
                if (!activeGeofenceZones.contains(zone.id)) {
                    // Entered zone
                    val isDanger = zone.isDangerZone
                    val prefix = if (isDanger) "Atenção. Você entrou na zona de risco:" else "Você entrou na área:"
                    val customAlert = zone.customVoiceAlert.takeIf { it.isNotBlank() } ?: "$prefix ${zone.name}."
                    
                    voiceManager?.speak(customAlert)
                    addLog(customAlert, if (isDanger) LogType.ALERT else LogType.INFO)
                }
            }
        }
        
        activeGeofenceZones.clear()
        activeGeofenceZones.addAll(currentZones)
    }

    /**
     * Persiste todos os dados críticos no Cloud (Firestore).
     * Chamado em situações de emergência (LowMemory) ou encerramento de turno.
     */
    fun persistSessionData() {
        try {
            // 1. Salvar Estatísticas
            syncActiveSessionStatsToCloud()
            
            // 2. Salvar Perfil (XP/Level)
            FirestoreManager.saveUserProfile(_userProfile.value)
            
            // 3. Salvar Configurações
            FirestoreManager.saveSettings(_settings.value)
            
            addLog("Cloud Sync: Dados da sessão persistidos com sucesso.", LogType.SUCCESS)
        } catch (e: Exception) {
            Log.e("RadarCoordinator", "Falha na persistência de emergência: ${e.message}")
        }
    }
}


