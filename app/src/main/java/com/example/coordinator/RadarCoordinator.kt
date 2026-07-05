package com.example.coordinator

import android.content.Context
import android.location.Location
import android.util.Log
import com.example.data.AppDatabase
import com.example.data.OfferEntity
import com.example.data.FirestoreManager
import com.example.data.UserProfile
import com.example.voice.VoiceInputManager
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
    NAVEGANDO
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

data class RadarSettings(
    val serverBaseUrl: String = "http://187.77.248.73:5000",
    val apiToken: String = "RadarDelivery2026Token",
    val forceMockSpeed: Boolean = true, // Default true for easier visual testing in AI Studio
    val mockSpeedKmh: Float = 0.0f,
    val isActiveDeliveryEnabled: Boolean = false,
    val activeDeliveryDestination: String = "Av. Paulista, 1000 - Bela Vista, São Paulo - SP",
    val minValuePerKm: Double = 2.0,
    val minFareValue: Double = 8.0,
    val useLocalGemini: Boolean = true,
    val geminiApiKey: String = "",
    val useHermesAgent: Boolean = false,
    val hermesBaseUrl: String = "https://api.nousresearch.com/v1",
    val hermesApiKey: String = "",
    val riskZonesKeywords: String = "Cracolândia, Heliópolis, Capão Redondo, Paraisópolis, Favela, Beco",
    val isDarkMode: Boolean = true,
    val isAutoRejectEnabled: Boolean = false,
    val autoRejectMinFare: Double = 10.0,
    val speedLimitKmh: Float = 10.0f,
    val maxPickupDistanceKm: Double = 5.0,
    val maxTotalDistanceKm: Double = 15.0
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
    private const val TAG = "RadarCoordinator"

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

    // 3. Current active/analyzed offer information
    private val _lastDecision = MutableStateFlow<String?>(null)
    val lastDecision: StateFlow<String?> = _lastDecision.asStateFlow()

    private val _lastReason = MutableStateFlow<String?>(null)
    val lastReason: StateFlow<String?> = _lastReason.asStateFlow()

    private val _activeOffer = MutableStateFlow<ActiveOffer?>(null)
    val activeOffer: StateFlow<ActiveOffer?> = _activeOffer.asStateFlow()

    // 4. Visual Console Log State
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

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
        FirestoreManager.updateSosAlert(
            active = active,
            latitude = loc?.latitude,
            longitude = loc?.longitude
        )
        if (active) {
            addLog("SOS ATIVADO: Localização em tempo real enviada para o contato de emergência!", LogType.ALERT)
        } else {
            addLog("SOS Desativado.", LogType.INFO)
        }
    }

    // 5. Real-Time Telematics & Delivery Dashboard Stats
    private val _deliveryActive = MutableStateFlow(false)
    val deliveryActive: StateFlow<Boolean> = _deliveryActive.asStateFlow()

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

    fun incrementBlockedByGeofence() {
        _blockedByGeofenceCount.value += 1
    }

    private var deliveryLastLocation: Location? = null

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
    }

    fun completeActiveDelivery() {
        if (!_deliveryActive.value) return
        
        val elapsedMs = System.currentTimeMillis() - _deliveryStartTimestamp.value
        val elapsedMinutes = elapsedMs / 60000.0
        val actualDistanceKm = _deliveryAccumulatedDistanceMeters.value / 1000.0
        val fare = _deliveryFare.value
        
        _deliveryCompletedCount.value += 1
        _deliveryTotalEarnings.value += fare
        _deliveryTotalDistanceKm.value += actualDistanceKm
        _deliveryTotalTimeMinutes.value += elapsedMinutes
        
        _deliveryActive.value = false
        addLog("Telemetria: Entrega finalizada com sucesso! Ganhos: R$ ${String.format(Locale.US, "%.2f", fare)} | Distância real: ${String.format(Locale.US, "%.2f", actualDistanceKm)} km | Tempo investido: ${String.format(Locale.US, "%.1f", elapsedMinutes)} min.", LogType.SUCCESS)
    }

    fun cancelActiveDelivery() {
        if (!_deliveryActive.value) return
        _deliveryActive.value = false
        addLog("Telemetria: Entrega cancelada ou limpa pelo usuário.", LogType.WARNING)
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
    private val firestoreScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)

    // For hysteresis calculation
    private var speedBelow3StartTime: Long = 0L

    fun init(context: Context) {
        database = AppDatabase.getDatabase(context)
        try {
            voiceInputManager = VoiceInputManager(context.applicationContext)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to instantiate VoiceInputManager: ${e.message}")
        }
        // Load settings from SharedPreferences if needed
        val prefs = context.getSharedPreferences("radar_delivery_prefs", Context.MODE_PRIVATE)
        _settings.update {
            RadarSettings(
                serverBaseUrl = prefs.getString("server_base_url", "http://187.77.248.73:5000") ?: "http://187.77.248.73:5000",
                apiToken = prefs.getString("api_token", "RadarDelivery2026Token") ?: "RadarDelivery2026Token",
                forceMockSpeed = prefs.getBoolean("force_mock_speed", true),
                mockSpeedKmh = prefs.getFloat("mock_speed_kmh", 0.0f),
                isActiveDeliveryEnabled = prefs.getBoolean("is_active_delivery_enabled", false),
                activeDeliveryDestination = prefs.getString("active_delivery_destination", "Av. Paulista, 1000 - Bela Vista, São Paulo - SP") ?: "Av. Paulista, 1000 - Bela Vista, São Paulo - SP",
                minValuePerKm = prefs.getFloat("min_value_per_km", 2.0f).toDouble(),
                minFareValue = prefs.getFloat("min_fare_value", 8.0f).toDouble(),
                useLocalGemini = prefs.getBoolean("use_local_gemini", true),
                geminiApiKey = prefs.getString("gemini_api_key", "") ?: "",
                useHermesAgent = prefs.getBoolean("use_hermes_agent", false),
                hermesBaseUrl = prefs.getString("hermes_base_url", "https://api.nousresearch.com/v1") ?: "https://api.nousresearch.com/v1",
                hermesApiKey = prefs.getString("hermes_api_key", "") ?: "",
                riskZonesKeywords = prefs.getString("risk_zones_keywords", "Cracolândia, Heliópolis, Capão Redondo, Paraisópolis, Favela, Beco") ?: "Cracolândia, Heliópolis, Capão Redondo, Paraisópolis, Favela, Beco",
                isDarkMode = prefs.getBoolean("is_dark_mode", true),
                isAutoRejectEnabled = prefs.getBoolean("is_auto_reject_enabled", false),
                autoRejectMinFare = prefs.getFloat("auto_reject_min_fare", 10.0f).toDouble(),
                speedLimitKmh = prefs.getFloat("speed_limit_kmh", 10.0f),
                maxPickupDistanceKm = prefs.getFloat("max_pickup_distance_km", 5.0f).toDouble(),
                maxTotalDistanceKm = prefs.getFloat("max_total_distance_km", 15.0f).toDouble()
            )
        }
        // Update speed based on mock config
        if (_settings.value.forceMockSpeed) {
            updateSpeed(_settings.value.mockSpeedKmh)
        }

        // Asynchronously load settings and sync logs from Firestore
        syncWithCloud(context)
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
                        putString("server_base_url", cloudSettings.serverBaseUrl)
                        putString("api_token", cloudSettings.apiToken)
                        putBoolean("force_mock_speed", cloudSettings.forceMockSpeed)
                        putFloat("mock_speed_kmh", cloudSettings.mockSpeedKmh)
                        putBoolean("is_active_delivery_enabled", cloudSettings.isActiveDeliveryEnabled)
                        putString("active_delivery_destination", cloudSettings.activeDeliveryDestination)
                        putFloat("min_value_per_km", cloudSettings.minValuePerKm.toFloat())
                        putFloat("min_fare_value", cloudSettings.minFareValue.toFloat())
                        putBoolean("use_local_gemini", cloudSettings.useLocalGemini)
                        putString("gemini_api_key", cloudSettings.geminiApiKey)
                        putBoolean("use_hermes_agent", cloudSettings.useHermesAgent)
                        putString("hermes_base_url", cloudSettings.hermesBaseUrl)
                        putString("hermes_api_key", cloudSettings.hermesApiKey)
                        putString("risk_zones_keywords", cloudSettings.riskZonesKeywords)
                        putBoolean("is_dark_mode", cloudSettings.isDarkMode)
                        putBoolean("is_auto_reject_enabled", cloudSettings.isAutoRejectEnabled)
                        putFloat("auto_reject_min_fare", cloudSettings.autoRejectMinFare.toFloat())
                        putFloat("speed_limit_kmh", cloudSettings.speedLimitKmh)
                        putFloat("max_pickup_distance_km", cloudSettings.maxPickupDistanceKm.toFloat())
                        putFloat("max_total_distance_km", cloudSettings.maxTotalDistanceKm.toFloat())
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

    fun saveSettings(context: Context, newSettings: RadarSettings) {
        val oldSettings = _settings.value
        _settings.value = newSettings
        if (oldSettings.isActiveDeliveryEnabled && !newSettings.isActiveDeliveryEnabled) {
            cancelActiveDelivery()
        }
        val prefs = context.getSharedPreferences("radar_delivery_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("server_base_url", newSettings.serverBaseUrl)
            putString("api_token", newSettings.apiToken)
            putBoolean("force_mock_speed", newSettings.forceMockSpeed)
            putFloat("mock_speed_kmh", newSettings.mockSpeedKmh)
            putBoolean("is_active_delivery_enabled", newSettings.isActiveDeliveryEnabled)
            putString("active_delivery_destination", newSettings.activeDeliveryDestination)
            putFloat("min_value_per_km", newSettings.minValuePerKm.toFloat())
            putFloat("min_fare_value", newSettings.minFareValue.toFloat())
            putBoolean("use_local_gemini", newSettings.useLocalGemini)
            putString("gemini_api_key", newSettings.geminiApiKey)
            putBoolean("use_hermes_agent", newSettings.useHermesAgent)
            putString("hermes_base_url", newSettings.hermesBaseUrl)
            putString("hermes_api_key", newSettings.hermesApiKey)
            putString("risk_zones_keywords", newSettings.riskZonesKeywords)
            putBoolean("is_dark_mode", newSettings.isDarkMode)
            putBoolean("is_auto_reject_enabled", newSettings.isAutoRejectEnabled)
            putFloat("auto_reject_min_fare", newSettings.autoRejectMinFare.toFloat())
            putFloat("speed_limit_kmh", newSettings.speedLimitKmh)
            putFloat("max_pickup_distance_km", newSettings.maxPickupDistanceKm.toFloat())
            putFloat("max_total_distance_km", newSettings.maxTotalDistanceKm.toFloat())
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
}
