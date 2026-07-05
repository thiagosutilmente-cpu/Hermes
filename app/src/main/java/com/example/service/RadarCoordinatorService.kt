package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.location.Geocoder
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.api.AnalyzeRequest
import com.example.api.ActiveDeliveryRequest
import com.example.api.RadarApiFactory
import com.example.coordinator.RadarCoordinator
import com.example.coordinator.RadarState
import com.example.coordinator.SpeedState
import com.example.data.OfferEntity
import com.example.voice.VoiceManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

open class RadarCoordinatorService : Service() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null

    private var voiceManager: VoiceManager? = null

    // Caching properties for hands-free destination detection
    private var cachedDestinationString: String? = null
    private var cachedDestinationLocation: Location? = null

    companion object {
        private const val NOTIFICATION_ID = 2026
        private const val CHANNEL_ID = "radar_delivery_channel"
        private const val TAG = "RadarService"
        var isServiceRunning = false
            private set

        fun startService(context: Context) {
            val intent = Intent(context, RadarCoordinatorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, RadarCoordinatorService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate")
        isServiceRunning = true
        RadarCoordinator.addLog("Serviço de Coordenador Radar iniciado. Agente Hermes ativado.", com.example.coordinator.LogType.INFO)

        // Initialize FusedLocation
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Initialize VoiceManager
        voiceManager = VoiceManager(this)
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ speakText("Agente Hermes inicializado. Pronto para ajudar na sua rota.") }, 2000)

        // Create Channel & Start Foreground
        createNotificationChannel()
        val notification = buildServiceNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID, 
                notification, 
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // Start observing Coordinator changes to update Notification
        scope.launch {
            RadarCoordinator.currentState.collectLatest { state ->
                updateNotification()
                if (state != RadarState.AGUARDANDO_ACAO) {
                    stopVoiceCommandListening()
                }
            }
        }
        scope.launch {
            RadarCoordinator.speedState.collectLatest {
                updateNotification()
            }
        }

        var wasSpeedOverLimit = false
        scope.launch {
            RadarCoordinator.currentSpeedKmh.collectLatest { speedKmh ->
                updateNotification()
                val limit = RadarCoordinator.settings.value.speedLimitKmh
                val isOverLimit = speedKmh > limit
                if (isOverLimit && !wasSpeedOverLimit) {
                    wasSpeedOverLimit = true
                    if (RadarCoordinator.currentState.value == RadarState.AGUARDANDO_ACAO) {
                        speakText("Atenção: Aceite bloqueado por segurança. Velocidade acima de ${limit.toInt()} quilômetros por hora. Reduza a velocidade para decidir.")
                        stopVoiceCommandListening()
                    }
                } else if (!isOverLimit && wasSpeedOverLimit) {
                    wasSpeedOverLimit = false
                    if (RadarCoordinator.currentState.value == RadarState.AGUARDANDO_ACAO) {
                        speakText("Interface de aceite liberada. Você já pode decidir.")
                        startVoiceCommandListening()
                    }
                }
            }
        }

        // Request active location updates (if GPS mock is disabled)
        startLocationUpdates()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service onStartCommand")
        intent?.let {
            if (it.getBooleanExtra("SIMULATE", false)) {
                val appName = it.getStringExtra("APP_NAME") ?: "iFood"
                val fareValue = it.getDoubleExtra("FARE_VALUE", 15.0)
                val pickup = it.getStringExtra("PICKUP_ADDRESS") ?: "McDonalds"
                val delivery = it.getStringExtra("DELIVERY_ADDRESS") ?: "Rua das Flores, 123"
                val distance = it.getDoubleExtra("DISTANCE_VALUE", 0.0)
                val duration = it.getDoubleExtra("TIME_VALUE", 0.0)
                
                processNewOffer(
                    appName = appName,
                    fareValue = fareValue,
                    pickupAddress = pickup,
                    deliveryAddress = delivery,
                    base64Image = "R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7",
                    totalDistance = distance,
                    totalTime = duration
                )
            } else if (it.getBooleanExtra("ACCESSIBILITY_OFFER", false)) {
                val appName = it.getStringExtra("APP_NAME") ?: "App de Entrega"
                val fareValue = it.getDoubleExtra("FARE_VALUE", 0.0)
                val pickup = it.getStringExtra("PICKUP_ADDRESS") ?: "Coleta"
                val delivery = it.getStringExtra("DELIVERY_ADDRESS") ?: "Entrega"
                val distance = it.getDoubleExtra("DISTANCE_VALUE", 0.0)
                val duration = it.getDoubleExtra("TIME_VALUE", 0.0)
                
                processNewOffer(
                    appName = appName,
                    fareValue = fareValue,
                    pickupAddress = pickup,
                    deliveryAddress = delivery,
                    base64Image = "R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7",
                    totalDistance = distance,
                    totalTime = duration
                )
            } else if (it.getBooleanExtra("TRIGGER_MAPS_MANUAL", false)) {
                abrirRotaNavegacao()
            } else if (it.getBooleanExtra("DISMISS_OFFER_MANUAL", false)) {
                dismissCurrentOffer()
            } else if (it.getBooleanExtra("ACCEPT_OFFER_MANUAL", false)) {
                executarCliqueAutomatico(isVoiceCommand = false)
            } else if (it.getBooleanExtra("START_VOICE_LISTENING_MANUAL", false)) {
                startVoiceCommandListening()
            } else if (it.getBooleanExtra("SPEAK_TEXT_MANUAL", false)) {
                val textToSpeak = it.getStringExtra("TEXT_TO_SPEAK") ?: ""
                if (textToSpeak.isNotBlank()) {
                    speakText(textToSpeak)
                }
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.d(TAG, "Service onDestroy")
        isServiceRunning = false
        job.cancel()
        stopLocationUpdates()
        voiceManager?.shutdown()
        voiceManager = null
        RadarCoordinator.addLog("Serviço de Coordenador Radar encerrado.", com.example.coordinator.LogType.WARNING)
        super.onDestroy()
    }

    // -------------------------------------------------------------------------
    // Location / GPS handling
    // -------------------------------------------------------------------------

    private fun startLocationUpdates() {
        try {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
                .setMinUpdateIntervalMillis(1000L)
                .build()

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    locationResult.lastLocation?.let { location ->
                        RadarCoordinator.updateLocation(location)
                        
                        // Automatic arrival detection
                        val settings = RadarCoordinator.settings.value
                        if (settings.isActiveDeliveryEnabled && settings.activeDeliveryDestination.isNotBlank()) {
                            if (cachedDestinationString != settings.activeDeliveryDestination) {
                                cachedDestinationString = settings.activeDeliveryDestination
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        val geocoder = Geocoder(this@RadarCoordinatorService, Locale.getDefault())
                                        @Suppress("DEPRECATION")
                                        val addresses = geocoder.getFromLocationName(settings.activeDeliveryDestination, 1)
                                        if (!addresses.isNullOrEmpty()) {
                                            val address = addresses[0]
                                            val loc = Location("geocoder").apply {
                                                latitude = address.latitude
                                                longitude = address.longitude
                                            }
                                            cachedDestinationLocation = loc
                                            Log.d(TAG, "Geocoded destination: ${loc.latitude}, ${loc.longitude}")
                                        } else {
                                            cachedDestinationLocation = null
                                        }
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Failed to geocode address: ${e.message}")
                                        com.example.data.FirestoreManager.logErrorToFirebase("GEOLOCATION_ERROR", "Falha ao geocodificar o endereço de destino '${settings.activeDeliveryDestination}': ${e.message}")
                                        cachedDestinationLocation = null
                                    }
                                }
                            }
                            
                            val dest = cachedDestinationLocation
                            if (dest != null) {
                                val distance = location.distanceTo(dest)
                                Log.d(TAG, "Distance to destination: ${distance}m")
                                
                                val isNear = distance < 150f
                                val isStopped = RadarCoordinator.podeInteragir() || (location.speed * 3.6f) < 5.0f
                                
                                if (isNear && isStopped) {
                                    Log.d(TAG, "AUTOMATIC ARRIVAL DETECTED")
                                    val updated = settings.copy(
                                        isActiveDeliveryEnabled = false,
                                        activeDeliveryDestination = ""
                                    )
                                    RadarCoordinator.saveSettings(this@RadarCoordinatorService, updated)
                                    cachedDestinationString = null
                                    cachedDestinationLocation = null
                                    
                                    // Complete the active delivery tracking
                                    RadarCoordinator.completeActiveDelivery()
                                    
                                    speakText("Você chegou ao seu destino! O Radar foi liberado automaticamente e está ouvindo novas ofertas.")
                                }
                            }
                        } else {
                            cachedDestinationString = null
                            cachedDestinationLocation = null
                        }
                    }
                }
            }

            // Check permissions
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission error requesting location updates: ${e.message}")
            com.example.data.FirestoreManager.logErrorToFirebase("PERMISSION_ERROR", "Permissão negada ao iniciar atualizações de geolocalização: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting location updates: ${e.message}")
            com.example.data.FirestoreManager.logErrorToFirebase("GEOLOCATION_ERROR", "Falha ao iniciar atualizações de geolocalização: ${e.message}")
        }
    }

    private fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
    }

    // -------------------------------------------------------------------------
    // Text To Speech (TTS) & Audio Focus
    // -------------------------------------------------------------------------

    private fun speakText(text: String, shortVersion: Boolean = false) {
        val profile = RadarCoordinator.userProfile.value
        if (!profile.audioAlertEnabled) {
            Log.d(TAG, "speakText blocked: audioAlertEnabled is false in user profile")
            return
        }
        Log.d(TAG, "speakText: $text")
        voiceManager?.speak(text)
    }

    // -------------------------------------------------------------------------
    // Background Coordination Engine (State Machine Flow)
    // -------------------------------------------------------------------------

    /**
     * Entry point to trigger analysis on a new offer (simulated or real).
     * This coordinates: OFERTA_LIDA -> ANALISANDO -> SUGERINDO -> AGUARDANDO_ACAO
     */
    fun processNewOffer(
        appName: String,
        fareValue: Double,
        pickupAddress: String,
        deliveryAddress: String,
        base64Image: String,
        totalDistance: Double = 0.0,
        totalTime: Double = 0.0
    ) {
        scope.launch {
            // Step 1: Oferta Lida
            RadarCoordinator.updateState(RadarState.OFERTA_LIDA)

            // Vibrate upon receiving any new offer if configured
            if (RadarCoordinator.userProfile.value.vibrateOnNewOffer) {
                try {
                    val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(android.os.VibrationEffect.createOneShot(200L, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(200L)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error triggering general vibration: ${e.message}")
                }
            }

            val logMsg = if (totalDistance > 0.0) {
                "Nova oferta capturada do app $appName. Valor: R$ $fareValue | Distância: $totalDistance km | Tempo: ${totalTime.toInt()} min."
            } else {
                "Nova oferta capturada do app $appName. Valor: R$ $fareValue."
            }
            RadarCoordinator.addLog(logMsg, com.example.coordinator.LogType.SUCCESS)
            RadarCoordinator.setActiveOffer(
                com.example.coordinator.ActiveOffer(appName, fareValue, pickupAddress, deliveryAddress, base64Image, totalDistance, totalTime)
            )
            RadarCoordinator.setDecision(null, null)

            val currentSettings = RadarCoordinator.settings.value
            if (currentSettings.isAutoRejectEnabled && fareValue < currentSettings.autoRejectMinFare) {
                Log.d(TAG, "Offer auto-rejected: fare R$$fareValue is below threshold R$${currentSettings.autoRejectMinFare}")
                RadarCoordinator.updateState(RadarState.SUGERINDO)
                
                val rejectReason = "Recusado Automaticamente: Valor R$ ${String.format(Locale.US, "%.2f", fareValue)} abaixo do limite de R$ ${String.format(Locale.US, "%.2f", currentSettings.autoRejectMinFare)}."
                RadarCoordinator.addLog("Filtro: Oferta rejeitada automaticamente. R$ $fareValue abaixo do mínimo R$ ${currentSettings.autoRejectMinFare}.", com.example.coordinator.LogType.WARNING)
                RadarCoordinator.setDecision("recusar", rejectReason)
                
                if (RadarCoordinator.userProfile.value.notifyOnAutoReject) {
                    speakText("Corrida do ${appName} recusada automaticamente por valor baixo.")
                }
                
                // Persist the auto-rejected offer in history
                val speedVal = RadarCoordinator.currentSpeedKmh.value
                val isChainedVal = currentSettings.isActiveDeliveryEnabled
                val activeDestVal = if (isChainedVal) currentSettings.activeDeliveryDestination else null
                
                val offerEntity = OfferEntity(
                    appName = appName,
                    fareValue = fareValue,
                    pickupAddress = pickupAddress,
                    deliveryAddress = deliveryAddress,
                    totalDistance = 0.0,
                    totalTime = 0.0,
                    detourDistance = 0.0,
                    detourTime = 0.0,
                    suggestion = "recusar",
                    reason = "Auto-rejeitado: R$ ${String.format(Locale.US, "%.2f", fareValue)} abaixo de R$ ${String.format(Locale.US, "%.2f", currentSettings.autoRejectMinFare)}",
                    speedKmhAtDecision = speedVal,
                    isChained = isChainedVal,
                    activeDeliveryDestination = activeDestVal
                )
                RadarCoordinator.saveOfferToDatabase(offerEntity)
                
                // Brief pause so the driver sees the card and understands it was auto-rejected
                withContext(Dispatchers.IO) { Thread.sleep(4000L) }
                dismissCurrentOffer()
                return@launch
            }

            // Step 2: Analisando
            RadarCoordinator.updateState(RadarState.ANALISANDO)
            RadarCoordinator.addLog("Análise: Enviando dados da oferta para cálculo inteligente de rota e rentabilidade...", com.example.coordinator.LogType.INFO)

            val lat = RadarCoordinator.currentLocation.value?.latitude ?: -23.550520
            val lon = RadarCoordinator.currentLocation.value?.longitude ?: -46.633308

            val result = withContext(Dispatchers.IO) {
                if (currentSettings.useHermesAgent) {
                    analyzeWithHermesAgent(
                        baseUrl = currentSettings.hermesBaseUrl,
                        apiKey = currentSettings.hermesApiKey,
                        base64Image = base64Image,
                        lat = lat,
                        lon = lon,
                        currentSettings = currentSettings,
                        ocrDistanceHint = totalDistance,
                        ocrTimeHint = totalTime
                    )
                } else if (currentSettings.useLocalGemini) {
                    analyzeWithLocalGemini(
                        apiKey = currentSettings.geminiApiKey,
                        base64Image = base64Image,
                        lat = lat,
                        lon = lon,
                        currentSettings = currentSettings,
                        ocrDistanceHint = totalDistance,
                        ocrTimeHint = totalTime
                    )
                } else {
                    try {
                        val api = RadarApiFactory.create(currentSettings.serverBaseUrl)
                        val activeDeliveryReq = if (currentSettings.isActiveDeliveryEnabled) {
                            ActiveDeliveryRequest(destinationAddress = currentSettings.activeDeliveryDestination)
                        } else null

                        val req = AnalyzeRequest(
                            image = base64Image,
                            latitude = lat,
                            longitude = lon,
                            activeDelivery = activeDeliveryReq,
                            riskZonesKeywords = currentSettings.riskZonesKeywords,
                            minValuePerKm = currentSettings.minValuePerKm,
                            minFareValue = currentSettings.minFareValue,
                            riderId = "moto_rider_android"
                        )

                        api.analyzeOffer(currentSettings.apiToken, req)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error calling /analyze API: ${e.message}", e)
                        com.example.data.FirestoreManager.logErrorToFirebase("CONNECTION_ERROR", "Erro de conexão com a API de análise: ${e.message}")
                        null
                    }
                }
            }

            if (result != null) {
                // Update Coordinator
                RadarCoordinator.setDecision(result.suggestion, result.reason)
                
                if (result.suggestion == "aceitar") {
                    val finalFare = result.details?.metrics?.fareValue ?: fareValue
                    val ocrDist = result.details?.routeData?.totalDistance ?: (result.details?.routeData?.detourDistance ?: 0.0)
                    val finalDist = if (ocrDist > 0.0) ocrDist else totalDistance
                    showRecommendationToast(appName, finalFare, finalDist, isOffline = false)
                }
                
                val isGeofenceBlocked = result.suggestion == "recusar" && (
                    result.reason.contains("raio", ignoreCase = true) ||
                    result.reason.contains("coleta", ignoreCase = true) ||
                    result.reason.contains("distância", ignoreCase = true) ||
                    result.reason.contains("limite", ignoreCase = true) ||
                    result.reason.contains("geofence", ignoreCase = true)
                )
                if (isGeofenceBlocked) {
                    RadarCoordinator.incrementBlockedByGeofence()
                }

                RadarCoordinator.addLog("Análise: IA sugeriu ${result.suggestion.uppercase()}. Motivo: ${result.reason}", if (result.suggestion == "aceitar") com.example.coordinator.LogType.SUCCESS else com.example.coordinator.LogType.WARNING)
                
                // Step 3: Sugerindo
                RadarCoordinator.updateState(RadarState.SUGERINDO)

                // Build TTS phrase based on decision and speed
                val isStopped = RadarCoordinator.podeInteragir()
                val finalFare = result.details?.metrics?.fareValue ?: fareValue
                val ocrDist = result.details?.routeData?.totalDistance ?: (result.details?.routeData?.detourDistance ?: 0.0)
                val finalDist = if (ocrDist > 0.0) ocrDist else totalDistance
                val valuePerKm = result.details?.metrics?.valuePerKm ?: if (finalDist > 0.1) (finalFare / finalDist) else 0.0

                // Define high profitability offer criteria
                val isHighProfitability = result.suggestion == "aceitar" && (
                    valuePerKm >= 2.50 || 
                    valuePerKm >= (currentSettings.minValuePerKm * 1.25) ||
                    finalFare >= 30.0
                )

                val phrase = if (isHighProfitability) {
                    val valuePerKmFormatted = String.format(Locale.US, "%.2f", valuePerKm)
                    val fareFormatted = String.format(Locale.US, "%.2f", finalFare)
                    "Atenção! Oferta de alta rentabilidade encontrada no $appName! Valor excelente de R$ $valuePerKmFormatted por quilômetro. Total de R$ $fareFormatted. Sugestão: Aceitar! ${result.reason}"
                } else {
                    buildTtsPhrase(result.suggestion, result.reason, isStopped)
                }

                if (isHighProfitability) {
                    RadarCoordinator.addLog("TTS: Oferta de alta rentabilidade detectada! Enviando alerta de voz otimizado.", com.example.coordinator.LogType.SUCCESS)
                    
                    // Vibrate with distinct double-pulse for high-profitability alerts
                    if (RadarCoordinator.userProfile.value.vibrateOnNewOffer) {
                        try {
                            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                val timings = longArrayOf(0, 300, 150, 300)
                                val amplitudes = intArrayOf(0, android.os.VibrationEffect.DEFAULT_AMPLITUDE, 0, android.os.VibrationEffect.DEFAULT_AMPLITUDE)
                                vibrator.vibrate(android.os.VibrationEffect.createWaveform(timings, amplitudes, -1))
                            } else {
                                @Suppress("DEPRECATION")
                                vibrator.vibrate(longArrayOf(0, 300, 150, 300), -1)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error triggering high profitability vibration: ${e.message}")
                        }
                    }
                }

                speakText(phrase, shortVersion = !isStopped)

                // Wait for speech to complete or brief delay
                withContext(Dispatchers.IO) { Thread.sleep(3000L) }

                // Step 4: Aguardando Ação
                RadarCoordinator.updateState(RadarState.AGUARDANDO_ACAO)

                // Persist the offer details to local database history!
                val speedVal = RadarCoordinator.currentSpeedKmh.value
                val isChainedVal = currentSettings.isActiveDeliveryEnabled
                val activeDestVal = if (isChainedVal) currentSettings.activeDeliveryDestination else null

                val totalDist = result.details?.routeData?.totalDistance ?: (result.details?.routeData?.detourDistance ?: 0.0)
                val totalTime = result.details?.routeData?.totalTime ?: (result.details?.routeData?.detourTime ?: 0.0)

                val offerEntity = OfferEntity(
                    appName = result.details?.extractedData?.deliveryApp ?: appName,
                    fareValue = result.details?.metrics?.fareValue ?: fareValue,
                    pickupAddress = result.details?.extractedData?.pickupAddress ?: pickupAddress,
                    deliveryAddress = result.details?.extractedData?.deliveryAddress ?: deliveryAddress,
                    totalDistance = totalDist,
                    totalTime = totalTime,
                    detourDistance = result.details?.routeData?.detourDistance ?: 0.0,
                    detourTime = result.details?.routeData?.detourTime ?: 0.0,
                    suggestion = result.suggestion,
                    reason = result.reason,
                    speedKmhAtDecision = speedVal,
                    isChained = isChainedVal,
                    activeDeliveryDestination = activeDestVal
                )
                RadarCoordinator.saveOfferToDatabase(offerEntity)

                // Perform automated actions if parado (convenience)
                if (isStopped && result.suggestion == "aceitar") {
                    // Isolated Automations
                    executarCliqueAutomatico()
                } else if (!isStopped && result.suggestion == "aceitar") {
                    speakText("Sugestão de aceite, mas você está em movimento. Toque para ver a rota.")
                }

                if (RadarCoordinator.currentState.value == RadarState.AGUARDANDO_ACAO) {
                    startVoiceCommandListening()
                }

            } else {
                // API Fallback / Error - Local caching and offline rule-based heuristic processing
                RadarCoordinator.addLog("Filtro: Servidor offline/erro. Processando heurísticas locais offline...", com.example.coordinator.LogType.ALERT)
                val speedVal = RadarCoordinator.currentSpeedKmh.value
                val isChainedVal = currentSettings.isActiveDeliveryEnabled
                val activeDestVal = if (isChainedVal) currentSettings.activeDeliveryDestination else null

                // Fallback heuristic: use extracted screen distance and time if offline
                val estimatedDistance = if (totalDistance > 0.0) totalDistance else 4.5
                val estimatedTime = if (totalTime > 0.0) totalTime else 15.0
                val valuePerKm = fareValue / estimatedDistance
                val isFareGood = fareValue >= currentSettings.minFareValue
                val isKmGood = valuePerKm >= currentSettings.minValuePerKm

                // Geofence checks
                val isPickupDistanceOk = currentSettings.maxPickupDistanceKm <= 0.0 || 1.8 <= currentSettings.maxPickupDistanceKm
                val isTotalDistanceOk = currentSettings.maxTotalDistanceKm <= 0.0 || estimatedDistance <= currentSettings.maxTotalDistanceKm

                val (offlineSuggestion, offlineReason) = when {
                    !isPickupDistanceOk -> Pair(
                        "recusar",
                        "Offline: Local de coleta (simulado 1.8km) fora do raio máximo de ${currentSettings.maxPickupDistanceKm} km."
                    )
                    !isTotalDistanceOk -> Pair(
                        "recusar",
                        "Offline: Distância total de ${String.format(Locale.US, "%.1f", estimatedDistance)} km excede o limite máximo de ${currentSettings.maxTotalDistanceKm} km."
                    )
                    isFareGood && isKmGood -> Pair(
                        "aceitar",
                        if (totalDistance > 0.0) {
                            "Offline: Excelente taxa de R$ ${String.format(Locale.US, "%.2f", valuePerKm)}/km por $totalDistance km."
                        } else {
                            "Offline: Bom valor (R$ ${String.format(Locale.US, "%.2f", fareValue)}) e boa taxa km estimada."
                        }
                    )
                    !isFareGood -> Pair(
                        "recusar",
                        "Offline: Valor R$ ${String.format(Locale.US, "%.2f", fareValue)} abaixo do mínimo de R$ ${String.format(Locale.US, "%.2f", currentSettings.minFareValue)}."
                    )
                    else -> Pair(
                        "recusar",
                        "Offline: Taxa de R$ ${String.format(Locale.US, "%.2f", valuePerKm)}/km abaixo do mínimo de R$ ${String.format(Locale.US, "%.2f", currentSettings.minValuePerKm)}/km."
                    )
                }

                RadarCoordinator.addLog("Análise Offline: Sugestão calculada offline: ${offlineSuggestion.uppercase()}. Motivo: $offlineReason", if (offlineSuggestion == "aceitar") com.example.coordinator.LogType.SUCCESS else com.example.coordinator.LogType.WARNING)

                val fallbackOfferEntity = OfferEntity(
                    appName = appName,
                    fareValue = fareValue,
                    pickupAddress = pickupAddress,
                    deliveryAddress = deliveryAddress,
                    totalDistance = estimatedDistance,
                    totalTime = estimatedTime,
                    detourDistance = 0.0,
                    detourTime = 0.0,
                    suggestion = offlineSuggestion,
                    reason = offlineReason,
                    speedKmhAtDecision = speedVal,
                    isChained = isChainedVal,
                    activeDeliveryDestination = activeDestVal
                )

                // Cache locally in the database
                RadarCoordinator.saveOfferToDatabase(fallbackOfferEntity)

                // Update coordinator for local state and UI
                RadarCoordinator.setDecision(offlineSuggestion, offlineReason)
                
                if (offlineSuggestion == "aceitar") {
                    showRecommendationToast(appName, fareValue, estimatedDistance, isOffline = true)
                }
                
                val isOfflineGeofenceBlocked = offlineSuggestion == "recusar" && (
                    offlineReason.contains("raio", ignoreCase = true) ||
                    offlineReason.contains("coleta", ignoreCase = true) ||
                    offlineReason.contains("distância", ignoreCase = true) ||
                    offlineReason.contains("limite", ignoreCase = true) ||
                    offlineReason.contains("geofence", ignoreCase = true)
                )
                if (isOfflineGeofenceBlocked) {
                    RadarCoordinator.incrementBlockedByGeofence()
                }

                RadarCoordinator.updateState(RadarState.SUGERINDO)

                val isOfflineHighProfitability = offlineSuggestion == "aceitar" && (
                    valuePerKm >= 2.50 || 
                    valuePerKm >= (currentSettings.minValuePerKm * 1.25) ||
                    fareValue >= 30.0
                )

                val fallbackPhrase = if (isOfflineHighProfitability) {
                    val valuePerKmFormatted = String.format(Locale.US, "%.2f", valuePerKm)
                    val fareFormatted = String.format(Locale.US, "%.2f", fareValue)
                    "Atenção! Oferta de alta rentabilidade encontrada offline no $appName! Valor excelente de R$ $valuePerKmFormatted por quilômetro. Total de R$ $fareFormatted. Sugestão: Aceitar! $offlineReason"
                } else if (offlineSuggestion == "aceitar") {
                    "Nova oferta encontrada offline no $appName. Valor de R$ ${String.format(Locale.US, "%.2f", fareValue)}. Sugestão: Aceitar! $offlineReason"
                } else {
                    "Sem conexão com o servidor. Processado offline. Sugestão: $offlineSuggestion."
                }

                if (isOfflineHighProfitability) {
                    RadarCoordinator.addLog("TTS: Oferta offline de alta rentabilidade detectada! Enviando alerta de voz otimizado.", com.example.coordinator.LogType.SUCCESS)
                    
                    // Vibrate with distinct double-pulse for high-profitability alerts
                    if (RadarCoordinator.userProfile.value.vibrateOnNewOffer) {
                        try {
                            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                val timings = longArrayOf(0, 300, 150, 300)
                                val amplitudes = intArrayOf(0, android.os.VibrationEffect.DEFAULT_AMPLITUDE, 0, android.os.VibrationEffect.DEFAULT_AMPLITUDE)
                                vibrator.vibrate(android.os.VibrationEffect.createWaveform(timings, amplitudes, -1))
                            } else {
                                @Suppress("DEPRECATION")
                                vibrator.vibrate(longArrayOf(0, 300, 150, 300), -1)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error triggering offline high profitability vibration: ${e.message}")
                        }
                    }
                }

                speakText(fallbackPhrase)

                withContext(Dispatchers.IO) { Thread.sleep(3000L) }
                RadarCoordinator.updateState(RadarState.AGUARDANDO_ACAO)

                // Automatically accept if stopped and suggestion is to accept
                val isStopped = RadarCoordinator.podeInteragir()
                if (isStopped && offlineSuggestion == "aceitar") {
                    executarCliqueAutomatico()
                } else if (!isStopped && offlineSuggestion == "aceitar") {
                    speakText("Sugestão de aceite offline, mas você está em movimento. Toque para ver a rota.")
                }

                if (RadarCoordinator.currentState.value == RadarState.AGUARDANDO_ACAO) {
                    startVoiceCommandListening()
                }
            }
        }
    }

    private fun buildTtsPhrase(suggestion: String, reason: String, isStopped: Boolean): String {
        val suggestionPortuguese = when (suggestion.lowercase()) {
            "aceitar" -> "Aceitar!"
            "recusar" -> "Recusar."
            else -> "Considerar."
        }

        // If riding, speak a very short safety-focused sentence
        return if (!isStopped) {
            "$suggestionPortuguese $reason."
        } else {
            "Radar Delivery sugere: $suggestionPortuguese O motivo é: $reason."
        }
    }

    /**
     * Isolated function to perform automatic click and launch route navigation,
     * maintaining high levels of security and modularity as requested in Section 7 ("O elo frágil").
     */
    fun executarCliqueAutomatico(isVoiceCommand: Boolean = false) {
        val limit = RadarCoordinator.settings.value.speedLimitKmh
        if (RadarCoordinator.currentSpeedKmh.value > limit) {
            Log.w(TAG, "Automatic acceptance blocked: current speed exceeds $limit km/h")
            speakText("Erro: Aceite automático bloqueado por excesso de velocidade.")
            return
        }
        scope.launch {
            // Anti-bot humanization: Randomize response time to mimic realistic human action
            val randomDelayMs = if (isVoiceCommand) {
                // Voice commands already have intrinsic natural delays, but add a slight extra variance (400ms to 900ms)
                (400..900).random().toLong()
            } else {
                // Auto-clicks get a highly human-like randomized delay between 1.5 and 3.8 seconds
                (1500..3800).random().toLong()
            }
            RadarCoordinator.addLog("Anti-Detecção: Aguardando atraso randômico humanizado de ${randomDelayMs}ms para evitar bloqueios e detecção robotizada.", com.example.coordinator.LogType.ALERT)
            Log.d(TAG, "Anti-detection delay: waiting ${randomDelayMs}ms before simulating click")
            withContext(Dispatchers.IO) { Thread.sleep(randomDelayMs) }

            RadarCoordinator.addLog("Ação: Executando simulação de toque na tela para ACEITAR oferta.", com.example.coordinator.LogType.SUCCESS)
            RadarCoordinator.updateState(RadarState.ACEITANDO)
            
            // Play physical audio chime feedback for the driver
            voiceManager?.playConfirmationChime()
            RadarCoordinator.addLog("Feedback Sonoro: Sinal acústico de confirmação de aceite emitido.", com.example.coordinator.LogType.SUCCESS)

            if (isVoiceCommand) {
                speakText("Corrida aceita por comando de voz. Carregando rota no Google Maps.")
            } else {
                speakText("Corrida aceita automaticamente. Carregando rota no Google Maps.")
            }
            
            withContext(Dispatchers.IO) { Thread.sleep(2000L) }
            
            // Transition to Navigating and open Maps
            abrirRotaNavegacao()
        }
    }

    fun abrirRotaNavegacao() {
        val active = RadarCoordinator.activeOffer.value ?: return
        
        RadarCoordinator.updateState(RadarState.NAVEGANDO)

        // Construct a Multi-stop Google Maps URL to go first to Coleta, then Entrega
        val pickup = active.pickupAddress
        val delivery = active.deliveryAddress

        // AUTOMATICALLY activate active delivery when navigating!
        val currentSettings = RadarCoordinator.settings.value
        val updatedSettings = currentSettings.copy(
            isActiveDeliveryEnabled = true,
            activeDeliveryDestination = delivery
        )
        RadarCoordinator.saveSettings(this, updatedSettings)
        Log.d(TAG, "AUTOMATIC: Enabled Active Delivery to: $delivery")

        // Start active delivery telemetry tracking in real time!
        RadarCoordinator.startActiveDeliveryTracking(
            appName = active.appName,
            fare = active.fareValue,
            estDistance = active.totalDistance,
            estTime = active.totalTime
        )

        val mapUri = Uri.parse(
            "https://www.google.com/maps/dir/?api=1" +
            "&destination=${Uri.encode(delivery)}" +
            "&waypoints=${Uri.encode(pickup)}"
        )

        val mapIntent = Intent(Intent.ACTION_VIEW, mapUri).apply {
            setPackage("com.google.android.apps.maps")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        try {
            startActivity(mapIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening Google Maps: ${e.message}")
            // Fallback to any map viewer
            val fallbackIntent = Intent(Intent.ACTION_VIEW, mapUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(fallbackIntent)
        }

        // Return to OUVINDO after launching maps
        scope.launch {
            withContext(Dispatchers.IO) { Thread.sleep(5000L) }
            RadarCoordinator.updateState(RadarState.OUVINDO)
            RadarCoordinator.setActiveOffer(null)
        }
    }

    // -------------------------------------------------------------------------
    // Voice Command Management
    // -------------------------------------------------------------------------

    private fun startVoiceCommandListening() {
        val limit = RadarCoordinator.settings.value.speedLimitKmh
        if (RadarCoordinator.currentSpeedKmh.value > limit) {
            Log.d(TAG, "Speed exceeds $limit km/h, voice listening blocked for safety.")
            return
        }
        scope.launch(Dispatchers.Main) {
            val inputManager = RadarCoordinator.voiceInputManager ?: return@launch
            Log.d(TAG, "Starting voice command listening for delivery rider...")
            RadarCoordinator.addLog("Voz: Escuta ativada. Fale 'ACEITAR' ou 'RECUSAR' para responder com as mãos livres.", com.example.coordinator.LogType.INFO)
            inputManager.startListening { command ->
                val currentLimit = RadarCoordinator.settings.value.speedLimitKmh
                if (RadarCoordinator.currentSpeedKmh.value > currentLimit) {
                    speakText("Interface bloqueada por excesso de velocidade. Reduza para decidir.")
                    return@startListening
                }
                when (command) {
                    "aceitar" -> {
                        RadarCoordinator.addLog("Voz: Comando 'ACEITAR' identificado por voz.", com.example.coordinator.LogType.SUCCESS)
                        executarCliqueAutomatico(isVoiceCommand = true)
                    }
                    "recusar" -> {
                        RadarCoordinator.addLog("Voz: Comando 'RECUSAR' identificado por voz.", com.example.coordinator.LogType.WARNING)
                        speakText("Comando por voz recebido. Corrida recusada.")
                        dismissCurrentOffer()
                    }
                }
            }
        }
    }

    private fun stopVoiceCommandListening() {
        scope.launch(Dispatchers.Main) {
            val wasListening = RadarCoordinator.voiceInputManager?.isListening?.value ?: false
            RadarCoordinator.voiceInputManager?.stopListening()
            if (wasListening) {
                RadarCoordinator.addLog("Voz: Escuta de voz desativada.", com.example.coordinator.LogType.INFO)
            }
        }
    }

    private fun dismissCurrentOffer() {
        scope.launch {
            RadarCoordinator.updateState(RadarState.OUVINDO)
            RadarCoordinator.setActiveOffer(null)
            RadarCoordinator.setDecision(null, null)
        }
    }

    // -------------------------------------------------------------------------
    // Notification & Foreground management
    // -------------------------------------------------------------------------

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Canal Radar Delivery AI",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificação persistente do coordenador do Radar Delivery AI"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun buildServiceNotification(): Notification {
        val state = RadarCoordinator.currentState.value
        val speedState = RadarCoordinator.speedState.value
        val speedKmh = RadarCoordinator.currentSpeedKmh.value

        val stateText = when (state) {
            RadarState.OUVINDO -> "Ouvindo ofertas..."
            RadarState.OFERTA_LIDA -> "Oferta detectada!"
            RadarState.ANALISANDO -> "Analisando com AI..."
            RadarState.SUGERINDO -> "Sugerindo por voz..."
            RadarState.AGUARDANDO_ACAO -> "Aguardando ação..."
            RadarState.ACEITANDO -> "Aceitando corrida..."
            RadarState.NAVEGANDO -> "Navegando no GPS..."
        }

        val speedText = if (speedState == SpeedState.PARADO) "Moto: PARADA (Ação Liberada)" else "Moto: EM MOVIMENTO (Trava de Segurança)"

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Radar Delivery AI - Ativo")
            .setContentText("$stateText | $speedText (${String.format(Locale.US, "%.1f", speedKmh)} km/h)")
            .setSmallIcon(android.R.drawable.ic_menu_compass) // Standard system compass icon
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildServiceNotification())
    }

    private fun analyzeWithHermesAgent(
        baseUrl: String,
        apiKey: String,
        base64Image: String,
        lat: Double,
        lon: Double,
        currentSettings: com.example.coordinator.RadarSettings,
        ocrDistanceHint: Double = 0.0,
        ocrTimeHint: Double = 0.0
    ): com.example.api.AnalyzeResponse? {
        if (apiKey.isBlank()) {
            return com.example.api.AnalyzeResponse(
                suggestion = "considerar",
                reason = "Configure sua Chave API do Hermes Agent nas configurações do app",
                confidence = 0.0,
                details = null
            )
        }

        return try {
            val ocrContext = if (ocrDistanceHint > 0.0) {
                "\nDICA DE EXTRAÇÃO DE TEXTO/OCR DA TELA:\n- Distância total: $ocrDistanceHint km\n- Tempo total: ${ocrTimeHint.toInt()} min\n"
            } else ""

            val promptText = "Você é o Hermes Agent da Nous Research, um assistente especializado em entregas (motoboy). Analise a imagem da oferta e os dados:\n" +
                "Valor mínimo configurado por KM: R$ ${currentSettings.minValuePerKm}\n" +
                "Valor mínimo aceitável total: R$ ${currentSettings.minFareValue}\n" +
                "Keywords de Área de Risco: ${currentSettings.riskZonesKeywords}\n" +
                (if (currentSettings.isActiveDeliveryEnabled) "ATENÇÃO: Entregador já em rota para: ${currentSettings.activeDeliveryDestination}. Aceite APENAS ofertas cujo destino da entrega se aproxime deste endereço, caracterizando uma Rota Casada (Chaining).\n" else "") +
                ocrContext +
                "\n" +
                "Retorne EXATAMENTE UM JSON, e APENAS o JSON, no seguinte formato:\n" +
                """
                {
                  "suggestion": "aceitar",
                  "reason": "Explicação curta",
                  "confidence": 0.95,
                  "details": {
                     "appName": "Nome do app",
                     "fareValue": 15.5,
                     "pickupAddress": "Endereço Coleta",
                     "deliveryAddress": "Endereço Entrega",
                     "totalDistanceKm": 5.2,
                     "totalTimeMinutes": 15
                  }
                }
                """

            val requestBodyJson = org.json.JSONObject().apply {
                put("model", "hermes-3-llama-3.1-8b") // Or a generic model name if not known
                put("messages", org.json.JSONArray().apply {
                    put(org.json.JSONObject().apply {
                        put("role", "user")
                        put("content", org.json.JSONArray().apply {
                            put(org.json.JSONObject().apply {
                                put("type", "text")
                                put("text", promptText)
                            })
                            put(org.json.JSONObject().apply {
                                put("type", "image_url")
                                put("image_url", org.json.JSONObject().apply {
                                    put("url", "data:image/jpeg;base64,$base64Image")
                                })
                            })
                        })
                    })
                })
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = requestBodyJson.toString().toRequestBody(mediaType)
            
            val formattedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            val url = "${formattedBaseUrl}chat/completions"

            val request = okhttp3.Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            val client = okhttp3.OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                val errBody = response.body?.string()
                Log.e("RadarCoordinator", "Hermes API failed: $errBody")
                return com.example.api.AnalyzeResponse(
                    suggestion = "considerar",
                    reason = "Erro na API do Hermes: código ${response.code}",
                    confidence = 0.0,
                    details = null
                )
            }

            val respStr = response.body?.string() ?: return null
            val responseObj = org.json.JSONObject(respStr)
            val choices = responseObj.getJSONArray("choices")
            val firstChoice = choices.getJSONObject(0)
            val message = firstChoice.getJSONObject("message")
            var innerJsonText = message.getString("content").trim()

            if (innerJsonText.startsWith("```json")) {
                innerJsonText = innerJsonText.removePrefix("```json")
            } else if (innerJsonText.startsWith("```")) {
                innerJsonText = innerJsonText.removePrefix("```")
            }
            if (innerJsonText.endsWith("```")) {
                innerJsonText = innerJsonText.removeSuffix("```")
            }
            innerJsonText = innerJsonText.trim()

            val moshi = com.squareup.moshi.Moshi.Builder()
                .add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
                .build()
            val adapter = moshi.adapter(com.example.api.AnalyzeResponse::class.java)
            adapter.fromJson(innerJsonText)
        } catch (e: Exception) {
            Log.e("RadarCoordinator", "Exception in analyzeWithHermesAgent: ${e.message}", e)
            com.example.data.FirestoreManager.logErrorToFirebase("CONNECTION_ERROR", "Falha na IA Hermes Agent: ${e.message}")
            com.example.api.AnalyzeResponse(
                suggestion = "considerar",
                reason = "Erro na IA Hermes Agent: ${e.message}",
                confidence = 0.0,
                details = null
            )
        }
    }

    private fun analyzeWithLocalGemini(
        apiKey: String,
        base64Image: String,
        lat: Double,
        lon: Double,
        currentSettings: com.example.coordinator.RadarSettings,
        ocrDistanceHint: Double = 0.0,
        ocrTimeHint: Double = 0.0
    ): com.example.api.AnalyzeResponse? {
        if (apiKey.isBlank()) {
            return com.example.api.AnalyzeResponse(
                suggestion = "considerar",
                reason = "Configure sua Chave API do Gemini nas configurações do app",
                confidence = 0.0,
                details = null
            )
        }

        return try {
            val ocrContext = if (ocrDistanceHint > 0.0) {
                "\nDICA DE EXTRAÇÃO DE TEXTO/OCR DA TELA (Use para validar o que foi capturado na imagem se estiver ilegível ou de difícil visualização):\n- Distância total capturada via texto: $ocrDistanceHint km\n- Tempo total capturado via texto: ${ocrTimeHint.toInt()} min\n"
            } else ""

            val prompt = """
            Examine o print de tela de uma oferta de corrida de aplicativo de entrega (como iFood, Uber Moto, Lalamove, Uber Flash, Rappi, etc.).
            Você é o assistente de inteligência de um entregador de moto. Seu objetivo é ajudar o entregador a decidir se a corrida vale a pena financeiramente sem que ele precise olhar para o celular enquanto pilota.
            $ocrContext
            Extraia com precisão:
            1. O nome do aplicativo de entrega (delivery_app)
            2. O valor total da corrida em Reais (fare_value, como número double ou string)
            3. O endereço ou local de Coleta (pickup_address)
            4. O endereço ou local de Entrega (delivery_address)
            
            Calcule ou estime também os seguintes dados com base na sua interpretação visual do mapa, texto ou dados da imagem:
            - total_distance (distância total da corrida em km, ex: 5.2)
            - total_time (tempo total estimado da corrida em minutos, ex: 15.0)

            Use as seguintes regras de negócio configuradas pelo motorista para determinar a "suggestion":
            - Localização atual do entregador (para cálculo de raio): Latitude: $lat, Longitude: $lon
            - Mínimo aceitável por km: ${currentSettings.minValuePerKm} R$/km
            - Valor mínimo por corrida: ${currentSettings.minFareValue} R$
            - Entrega ativa em andamento: ${currentSettings.isActiveDeliveryEnabled}
            - Destino da entrega ativa: ${currentSettings.activeDeliveryDestination}
            - Raio de Coleta Máximo permitido: ${currentSettings.maxPickupDistanceKm} km (desativado se for 0.0)
            - Distância Total Máxima permitida: ${currentSettings.maxTotalDistanceKm} km (desativado se for 0.0)

            Regras para decidir "suggestion":
            - Se o Raio de Coleta Máximo for maior que 0.0, e a distância estimada em linha reta entre a localização atual ($lat, $lon) e o local de coleta (pickup_address) for maior que ${currentSettings.maxPickupDistanceKm} km, retorne "recusar" com a justificativa de "Fora do raio de coleta".
            - Se a Distância Total Máxima for maior que 0.0, e a distância total calculada/estimada para a corrida (total_distance) for maior que ${currentSettings.maxTotalDistanceKm} km, retorne "recusar" com a justificativa de "Excede a distância máxima".
            - Se o valor total (fare_value) for menor que o Mínimo de Corrida (${currentSettings.minFareValue}), retorne "recusar".
            - Se o valor calculado por km (fare_value / total_distance) for menor que o Mínimo por km (${currentSettings.minValuePerKm}), retorne "recusar".
            - Caso contrário, sugira "aceitar". Se estiver muito próximo do limite, use "considerar".
            
            Retorne EXCLUSIVAMENTE um objeto JSON válido (sem blocos de código markdown ou texto explicativo extra, apenas o JSON bruto):
            {
              "suggestion": "aceitar" | "considerar" | "recusar",
              "reason": "Explicação curta em português (máximo 15 palavras) do motivo da decisão",
              "confidence": 0.95,
              "details": {
                "extracted_data": {
                  "pickup_address": "Endereço ou local de coleta",
                  "delivery_address": "Endereço ou local de entrega",
                  "fare_value": "Valor da corrida extraído (ex: 12.50)",
                  "delivery_app": "Nome do app"
                },
                "route_data": {
                  "total_distance": 5.2,
                  "total_time": 15.0,
                  "detour_distance": 0.0,
                  "detour_time": 0.0,
                  "chained_distance": 0.0,
                  "chained_time": 0.0
                },
                "metrics": {
                  "fare_value": 12.50,
                  "value_per_km": 2.40,
                  "value_per_minute": 0.83
                }
              }
            }
            """.trimIndent()

            val partsArray = org.json.JSONArray().apply {
                put(org.json.JSONObject().put("text", prompt))
                put(org.json.JSONObject().put("inlineData", org.json.JSONObject().apply {
                    put("mimeType", "image/jpeg")
                    put("data", base64Image)
                }))
            }

            val contentsArray = org.json.JSONArray().apply {
                put(org.json.JSONObject().put("parts", partsArray))
            }

            val generationConfig = org.json.JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.2)
            }

            val payload = org.json.JSONObject().apply {
                put("contents", contentsArray)
                put("generationConfig", generationConfig)
            }

            val client = okhttp3.OkHttpClient.Builder()
                .connectTimeout(45, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(45, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(45, java.util.concurrent.TimeUnit.SECONDS)
                .build()

            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = payload.toString().toRequestBody(mediaType)

            val request = okhttp3.Request.Builder()
                .url(url)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    Log.e("RadarCoordinator", "Gemini API failed: $errBody")
                    return com.example.api.AnalyzeResponse(
                        suggestion = "considerar",
                        reason = "Erro na API do Gemini: código ${response.code}",
                        confidence = 0.0,
                        details = null
                    )
                }

                val respStr = response.body?.string() ?: return null
                val responseObj = org.json.JSONObject(respStr)
                val candidates = responseObj.getJSONArray("candidates")
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.getJSONObject("content")
                val parts = content.getJSONArray("parts")
                val firstPart = parts.getJSONObject(0)
                val innerJsonText = firstPart.getString("text")

                val moshi = com.squareup.moshi.Moshi.Builder()
                    .add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
                    .build()
                val adapter = moshi.adapter(com.example.api.AnalyzeResponse::class.java)
                adapter.fromJson(innerJsonText)
            }
        } catch (e: Exception) {
            Log.e("RadarCoordinator", "Exception in analyzeWithLocalGemini: ${e.message}", e)
            com.example.data.FirestoreManager.logErrorToFirebase("CONNECTION_ERROR", "Falha na IA Local Gemini: ${e.message}")
            com.example.api.AnalyzeResponse(
                suggestion = "considerar",
                reason = "Erro na IA Local: ${e.message}",
                confidence = 0.0,
                details = null
            )
        }
    }

    private fun showRecommendationToast(appName: String, fare: Double, distance: Double, isOffline: Boolean = false) {
        scope.launch {
            val kmRate = if (distance > 0.0) fare / distance else 0.0
            val suffix = if (isOffline) " (Offline)" else ""
            val toastMsg = if (distance > 0.0) {
                "🚀 Oferta recomendada$suffix!\nR$ ${String.format(Locale.US, "%.2f", fare)} no $appName\nDistância: ${String.format(Locale.US, "%.1f", distance)} km | Taxa: R$ ${String.format(Locale.US, "%.2f", kmRate)}/km"
            } else {
                "🚀 Oferta recomendada$suffix!\nR$ ${String.format(Locale.US, "%.2f", fare)} no $appName"
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(applicationContext, toastMsg, Toast.LENGTH_LONG).show()
            }
        }
    }
}
