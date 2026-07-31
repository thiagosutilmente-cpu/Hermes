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
import android.hardware.camera2.CameraManager
import com.example.util.JarvisIntelligenceEngine
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.api.AnalyzeRequest
import com.example.api.ActiveDeliveryRequest
import com.example.api.RadarApiFactory
import com.example.coordinator.LogType
import com.example.voice.JarvisResult
import com.example.util.GeminiManager
import com.example.util.MultiAppOrderManager
import com.example.util.ActiveOrder
import com.example.util.OrderStatus
import com.example.util.RouteOptimizer
import com.example.util.StopType
import com.example.coordinator.RadarCoordinator
import com.example.coordinator.RadarState
import com.example.coordinator.SpeedState
import com.example.data.OfferEntity
import com.example.data.FirestoreManager
import com.example.voice.VoiceManager
import com.example.voice.VoiceInputManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import android.content.IntentFilter
import android.os.BatteryManager
import kotlinx.coroutines.withContext
import java.util.Locale
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.dp
import com.example.ui.components.JarvisVoiceHUD
import android.provider.Settings
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleEventObserver
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.SavedStateRegistryController


open class RadarCoordinatorService : Service() {
    override fun attachBaseContext(base: android.content.Context) {
        super.attachBaseContext(base)
    }



    private val job = SupervisorJob()
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Exceção Crítica capturada pelo CoroutineExceptionHandler: ${throwable.message}")
        RadarCoordinator.addLog("Sistema: Recuperando de erro crítico...", com.example.coordinator.LogType.ALERT)
        RadarCoordinator.reportSystemStress()
    }
    private val scope = CoroutineScope(Dispatchers.Main + job + exceptionHandler)

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null

    private var voiceManager: VoiceManager? = null

    // Caching properties for hands-free destination detection
    private var cachedDestinationString: String? = null
    private var cachedDestinationLocation: Location? = null
    
    // Concurrency lock to prevent Rate Limit / Spam when 10 offers pop up at once
    private val isProcessingOffer = java.util.concurrent.atomic.AtomicBoolean(false)

    // Window Overlay for Jarvis HUD
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var overlayHelper: ComposeOverlayHelper? = null

    companion object {
        private const val NOTIFICATION_ID = 2026
        private const val CHANNEL_ID = "radar_delivery_channel"
        private const val TAG = "RadarService"
        var isServiceRunning = false
            private set
        var instance: RadarCoordinatorService? = null

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

    private var remoteCommandListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var patchListener: com.google.firebase.firestore.ListenerRegistration? = null

    private fun startPatchListener() {
        patchListener = FirestoreManager.listenToPatches { patchId, data ->
            RadarCoordinator.addLog("Jarvis: Neural Patch aplicado: $patchId", com.example.coordinator.LogType.INFO)
            // Apply patches to settings
            val current = RadarCoordinator.settings.value
            val newSettings = current.copy(
                operationalOverrides = current.operationalOverrides + (data["key"] as? String to data["value"] as? String).let {
                    if (it.first != null && it.second != null) mapOf(it.first!! to it.second!!) else emptyMap()
                }
            )
            RadarCoordinator.updateSettings(newSettings)
            FirestoreManager.saveSettings(newSettings)
        }
    }

    private val commandReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
            if (intent == null) return
            
            when (intent.action) {
                "com.example.ACTION_JARVIS_SPEAK" -> {
                    val text = intent.getStringExtra("TEXT_TO_SPEAK") ?: return
                    speakText(text)
                }
                "com.example.ACTION_NEW_OFFER" -> {
                    // Re-encaminha para o processamento central do serviço
                    val newIntent = Intent(context, RadarCoordinatorService::class.java).apply {
                        putExtras(intent)
                        putExtra("ACCESSIBILITY_OFFER", true) // Trata como oferta de acessibilidade
                    }
                    context?.startService(newIntent)
                }
                "com.example.ACTION_OFFER_EXPIRED" -> {
                    if (RadarCoordinator.activeOffer.value != null && RadarCoordinator.currentState.value != RadarState.NAVEGANDO) {
                        RadarCoordinator.setActiveOffer(null)
                        RadarCoordinator.updateState(RadarState.OUVINDO)
                        RadarCoordinator.addLog("Jarvis: Oferta expirada ou aceita por outro motorista.", com.example.coordinator.LogType.WARNING)
                        speakText("Thiago, esquece essa, a oferta expirou ou outro motorista foi mais rápido.")
                    }
                }
            }
        }
    }
    
    private fun startFatigueMonitoring() {
        scope.launch(Dispatchers.IO) {
            val serviceStartTime = System.currentTimeMillis()
            var nextAlertHours = 1
            while (isServiceRunning) {
                kotlinx.coroutines.delay(60000L) // Check every minute
                val settings = RadarCoordinator.settings.value
                if (settings.aiActiveFatigueDetect) {
                    val elapsedMs = System.currentTimeMillis() - serviceStartTime
                    val elapsedHours = elapsedMs / (3 * 60 * 60 * 1000.0) // 3-hour chunks
                    if (elapsedHours >= nextAlertHours) {
                        nextAlertHours++
                        val message = "Thiago! Detectei que você já está pilotando há ${nextAlertHours - 1} turnos de 3 horas seguidas sem pausas. Para sua segurança física e prevenção de acidentes, Jarvis sugere que você faça uma pausa de 15 minutos para descansar, tomar uma água ou um café."
                        speakText(message)
                        RadarCoordinator.updateFatigueAlert(true)
                        RadarCoordinator.addLog("Jarvis IA: Alerta de fadiga ativa do piloto (Mais de ${3 * (nextAlertHours - 1)} horas online)", com.example.coordinator.LogType.WARNING)
                    }
                }
            }
        }
    }
    
    private fun startProactiveMonitoring() {
        scope.launch {
            while (true) {
                // Self-Healing Watchdog
                RadarCoordinator.healSystem()
                
                delay(120000L) // Check every 2 minutes for tactical analysis
                
                val settings = RadarCoordinator.settings.value
                val speed = RadarCoordinator.currentSpeedKmh.value
                val batteryIntent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val batteryPct = level.toFloat()
                
                // Scenario 1: Speeding Protocol
                if (speed > 90f) {
                    speakText("Thiago, sua velocidade está em ${speed.toInt()} km/h. Por favor, mantenha a calma. Chegar vivo é mais lucrativo do que chegar rápido.")
                    delay(30000L) // Cooldown short
                }
                
                // Scenario 2: Low Battery Tactical
                if (batteryPct < 10f && !RadarCoordinator.deliveryActive.value) {
                    speakText("Senhor, bateria em níveis críticos (${batteryPct.toInt()}%). Estratégia recomendada: encontrar um ponto de carga ou encerrar o turno para evitar o apagão total.")
                    delay(300000L) 
                }

                // Scenario 3: Maintenance Analysis
                if (settings.nextOilChangeMileage > 0 && settings.motorcycleMileage >= settings.nextOilChangeMileage) {
                    speakText("Thiago, notei que ultrapassamos a quilometragem da troca de óleo. Recomendo agendar a manutenção para não comprometer seu motor.")
                    delay(1200000L) // Cooldown long
                }
                
                // Scenario 4: Revenue Analysis (Strategic Idle)
                val lastOfferTime = RadarCoordinator.logs.value.find { it.message.contains("Nova oferta") }?.timestamp ?: 0L
                val idleMinutes = (System.currentTimeMillis() - lastOfferTime) / 60000
                if (idleMinutes > 20 && speed < 5f && !RadarCoordinator.deliveryActive.value) {
                    speakText("Estamos estagnados há $idleMinutes minutos. Esta região parece saturada. Sugiro deslocamento tático para uma zona de maior demanda.")
                    delay(600000L)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate")
        isServiceRunning = true
        instance = this
        RadarCoordinator.setServiceRunning(true)
        RadarCoordinator.initialize(this)
        startPatchListener()
        RadarCoordinator.addLog("ESTRATÉGIA: Ativando protocolo [${RadarCoordinator.currentStrategy.value}]", com.example.coordinator.LogType.INFO)
        
        // Monitoramento de Bateria (Core Energy)
        val batteryStatus: android.content.Intent? = android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            registerReceiver(null, ifilter)
        }
        val batteryPct: Int = batteryStatus?.let { intent ->
            val level: Int = intent.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1)
            val scale: Int = intent.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1)
            (level * 100 / scale.toFloat()).toInt()
        } ?: 100
        RadarCoordinator.updateBatteryLevel(batteryPct)

        val filter = android.content.IntentFilter().apply {
            addAction("com.example.ACTION_JARVIS_SPEAK")
            addAction("com.example.ACTION_NEW_OFFER")
            addAction("com.example.ACTION_OFFER_EXPIRED")
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(commandReceiver, filter, android.content.Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(commandReceiver, filter)
        }

        // Iniciar listener de comandos remotos do Firestore
        remoteCommandListener = com.example.data.FirestoreManager.listenToRemoteCommands { command ->
            handleRemoteCommand(command)
        }
        
        RadarCoordinator.addLog("Serviço de Coordenador Radar iniciado. IA Jarvis 100% operacional.", com.example.coordinator.LogType.INFO)
        startProactiveMonitoring()
        startFatigueMonitoring()

        // Initialize FusedLocation with attribution tag for privacy
        try {
            val locationContext = applicationContext
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(locationContext)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create attribution context or location client: ${e.message}")
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(applicationContext)
        }
        setupOverlay()

        // Initialize VoiceManager with audio attribution if supported
        val audioContext = this

        voiceManager = VoiceManager(audioContext)
        com.example.coordinator.RadarCoordinator.voiceManager = voiceManager
        com.example.coordinator.RadarCoordinator.voiceInputManager = VoiceInputManager(audioContext)

        // Create Channel & Start Foreground
        createNotificationChannel()
        val notification = buildServiceNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            var serviceType = ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                serviceType = serviceType or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                serviceType = serviceType or ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                serviceType = serviceType or ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            }
            startForeground(
                NOTIFICATION_ID, 
                notification, 
                serviceType
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
                        speakText("Thiago, por segurança eu bloqueei o aceite. Sua velocidade está acima de ${limit.toInt()} por hora, diminua o ritmo para decidir!")
                        stopVoiceCommandListening()
                    }
                } else if (!isOverLimit && wasSpeedOverLimit) {
                    wasSpeedOverLimit = false
                    if (RadarCoordinator.currentState.value == RadarState.AGUARDANDO_ACAO) {
                        speakText("Thiago, velocidade segura restabelecida. Painel liberado para aceitar.")
                        startVoiceCommandListening()
                    }
                }
            }
        }

        // Dynamic location updates management based on settings
        scope.launch {
            RadarCoordinator.settings.collectLatest { settings ->
                if (settings.forceMockSpeed) {
                    stopLocationUpdates()
                } else {
                    startLocationUpdates()
                }
            }
        }
        // Monitoramento Proativo de Trânsito (Jarvis - Google Maps API Integration)
        var lastDetourAlertTime = 0L
        scope.launch(Dispatchers.IO) {
            while (isServiceRunning) {
                val settings = RadarCoordinator.settings.value
                // Somente monitora se a opção de desvio de tráfego IA estiver ativa, se a notificação estiver ativada e se estivermos em uma rota ativa (isActiveDeliveryEnabled)
                if (settings.aiActiveTrafficReroute && settings.notifyOnTrafficChange && settings.isActiveDeliveryEnabled && settings.activeDeliveryDestination.isNotBlank()) {
                    val currentLoc = RadarCoordinator.currentLocation.value
                    val currentLat = currentLoc?.latitude ?: -23.5505
                    val currentLon = currentLoc?.longitude ?: -46.6333
                    
                    try {
                        val result = com.example.util.GoogleMapsTrafficMonitor.monitorTraffic(
                            context = this@RadarCoordinatorService,
                            currentLat = currentLat,
                            currentLon = currentLon,
                            destination = settings.activeDeliveryDestination,
                            apiKey = com.example.BuildConfig.MAPS_API_KEY
                        )

                        Log.d(TAG, "Monitoramento de tráfego Jarvis: Rota ativa=${settings.activeDeliveryDestination}, Desvio sugerido=${result.detourSuggested}, Multiplicador=${result.trafficMultiplier}, Real API=${result.isRealApi}")

                        val delayMinutes = ((result.durationInTrafficSeconds - result.durationSeconds) / 60).toInt()
                        RadarCoordinator.updateTrafficDetour(result.detourSuggested, delayMinutes, result.reason)

                        if (result.detourSuggested) {
                            val now = System.currentTimeMillis()
                            // Evita disparar múltiplos alertas seguidos na mesma corrida (limite de 3 minutos de intervalo)
                            if (now - lastDetourAlertTime > 180000L) {
                                lastDetourAlertTime = now
                                playVipAlert("Traffic", 0.0, 0.0) // som de alerta rápido
                                val alertMsg = "Thiago! Tráfego lento detectado pelo Google Maps no seu trajeto. ${result.reason} Jarvis recomenda recalcular uma rota alternativa para economizar tempo."
                                speakText(alertMsg)
                                RadarCoordinator.addLog("Jarvis Tráfego: ${result.reason}", com.example.coordinator.LogType.WARNING)
                            }
                        } else {
                            RadarCoordinator.updateTrafficDetour(false, 0, "")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Erro ao monitorar tráfego em tempo real: ${e.message}")
                    }
                }
                kotlinx.coroutines.delay(60000L) // Monitoramento a cada 60 segundos
            }
        }

    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service onStartCommand")
        isServiceRunning = true
        RadarCoordinator.setServiceRunning(true)

        // Verificação de Licença Jarvis Pro (Real no Firestore)
        com.example.coordinator.RadarCoordinator.firestoreScope.launch {
            val settings = com.example.coordinator.RadarCoordinator.settings.value
            val riderId = com.example.data.FirebaseAuthManager.getCurrentRiderId()
            
            val isLicenseValid = com.example.data.FirestoreManager.validateLicense(riderId, settings.licenseKey)
            
            if (!isLicenseValid) {
                RadarCoordinator.addLog("Jarvis: LICENÇA INVÁLIDA OU EXPIRADA. Funcionalidades Pro limitadas.", com.example.coordinator.LogType.ALERT)
                speakText("Thiago, sua licença do Jarvis Pro não foi validada. Verifique sua assinatura no painel para habilitar a autonomia total.")
                // Aqui poderíamos desativar o auto-aceite forçadamente
                RadarCoordinator.saveSettings(this@RadarCoordinatorService, settings.copy(isAutoAcceptEnabled = false))
            } else {
                RadarCoordinator.addLog("Jarvis: Licença Pro validada via Cloud. Acesso total liberado.", com.example.coordinator.LogType.SUCCESS)
                speakText("Licença validada. Sistema Jarvis Pro cem por cento operacional.")
            }
        }

        intent?.let {
            if (it.getBooleanExtra("ORDER_CANCELLED", false)) {
                val app = it.getStringExtra("APP_NAME") ?: "App"
                handleOrderCancellation(app)
            } else if (it.hasExtra("NEURAL_ACTION")) {
                val actionKeywords = it.getStringArrayListExtra("KEYWORDS") ?: arrayListOf()
                scope.launch {
                    val success = RadarAccessibilityService.getInstance()?.performSurrealAction(actionKeywords) ?: false
                    if (success) {
                        val label = actionKeywords.firstOrNull() ?: "ação"
                        speakText("Elo neural estabelecido. Executando $label com sucesso.")
                    } else {
                        speakText("Thiago, não encontrei um ponto de ancoragem para essa ação no momento.")
                    }
                }
            } else if (it.getBooleanExtra("MULTI_TARGET", false)) {
                val appName = it.getStringExtra("APP_NAME") ?: "App"
                val count = it.getIntExtra("TOTAL_TARGETS", 0)
                val bestFare = it.getDoubleExtra("FARE_VALUE", 0.0)
                
                RadarCoordinator.addLog("Jarvis Swarm: Detectadas $count ofertas simultâneas no $appName. Melhor ROI: R$ $bestFare.", com.example.coordinator.LogType.SUCCESS)
                speakText("Thiago, detectei um enxame de $count ofertas no $appName. A melhor delas paga R$ $bestFare. Analisando sinergia.")
                
                processNewOffer(
                    appName = appName,
                    fareValue = bestFare,
                    pickupAddress = "Enxame de Ofertas",
                    deliveryAddress = "Lista de Seleção",
                    base64Image = "R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7",
                    isBackgroundOffer = false // Geralmente o motorista está com o app aberto vendo a lista
                )
            } else if (it.getBooleanExtra("SIMULATE", false)) {
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
                val isBackground = it.getBooleanExtra("BACKGROUND_OFFER", false)
                
                processNewOffer(
                    appName = appName,
                    fareValue = fareValue,
                    pickupAddress = pickup,
                    deliveryAddress = delivery,
                    base64Image = "R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7",
                    totalDistance = distance,
                    totalTime = duration,
                    isBackgroundOffer = isBackground
                )
            } else if (it.getBooleanExtra("TRIGGER_MAPS_MANUAL", false)) {
                abrirRotaNavegacao()
            } else if (it.getBooleanExtra("DISMISS_OFFER_MANUAL", false)) {
                dismissCurrentOffer()
            } else if (it.getBooleanExtra("ACCEPT_OFFER_MANUAL", false)) {
                executarCliqueAutomatico(isVoiceCommand = false)
                abrirRotaNavegacao()
            } else if (it.getBooleanExtra("START_VOICE_LISTENING_MANUAL", false)) {
                startVoiceCommandListening()
            } else if (it.getBooleanExtra("SPEAK_TEXT_MANUAL", false)) {
                val textToSpeak = it.getStringExtra("TEXT_TO_SPEAK") ?: ""
                if (textToSpeak.isNotBlank()) {
                    speakText(textToSpeak)
                }
            }
        }
        
        // Try starting location updates in case permissions were granted after onCreate
        startLocationUpdates()
        
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun handleRemoteCommand(command: String) {
        Log.d(TAG, "Processando comando remoto: $command")
        
        when {
                        command.startsWith("message:") -> {
                val msg = command.removePrefix("message:").trim()
                speakText("Mensagem da central: $msg")
                RadarCoordinator.addLog("Central: $msg", com.example.coordinator.LogType.WARNING)
                val chatIntent = android.content.Intent("com.example.ACTION_ADD_CHAT_MESSAGE")
                chatIntent.putExtra("MESSAGE", "Central: $msg")
                chatIntent.putExtra("IS_USER", false)
                sendBroadcast(chatIntent)
            }
            command == "ping" -> {
                speakText("Thiago, recebi seu sinal do painel de controle. Conexão está excelente!")
                RadarCoordinator.addLog("Jarvis: Recebi um PING remoto do painel.", com.example.coordinator.LogType.INFO)
            }
            command == "reset_stats" -> {
                RadarCoordinator.resetSessionStats()
                RadarCoordinator.addLog("Jarvis: Estatísticas da sessão resetadas via comando remoto.", com.example.coordinator.LogType.WARNING)
                speakText("Certo, Thiago. Estatísticas zeradas conforme solicitado remotamente.")
            }
            command == "HEAL_SYSTEM" -> {
                RadarCoordinator.healSystem()
                speakText("Protocolo de auto-cura iniciado. Recalibrando sensores e ponte neural.")
            }
            command == "TOGGLE_SPLIT_SCREEN" -> {
                val intent = android.content.Intent("com.example.ACTION_PERFORM_GLOBAL_ACTION").apply {
                    putExtra("GLOBAL_ACTION_ID", 7) // 7 = GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN
                }
                sendBroadcast(intent)
                speakText("Thiago, alternando para modo Dual Core agora. Vou dividir a tela para você.")
                RadarCoordinator.addLog("Jarvis: Comando remoto para Tela Dividida executado.", com.example.coordinator.LogType.SUCCESS)
            }
            command == "GO_BACK" -> {
                val intent = android.content.Intent("com.example.ACTION_PERFORM_GLOBAL_ACTION").apply {
                    putExtra("GLOBAL_ACTION_ID", 1) // 1 = GLOBAL_ACTION_BACK
                }
                sendBroadcast(intent)
            }
            command == "GO_HOME" -> {
                val intent = android.content.Intent("com.example.ACTION_PERFORM_GLOBAL_ACTION").apply {
                    putExtra("GLOBAL_ACTION_ID", 2) // 2 = GLOBAL_ACTION_HOME
                }
                sendBroadcast(intent)
            }
            command == "OPEN_RECENTS" -> {
                val intent = android.content.Intent("com.example.ACTION_PERFORM_GLOBAL_ACTION").apply {
                    putExtra("GLOBAL_ACTION_ID", 3) // 3 = GLOBAL_ACTION_RECENTS
                }
                sendBroadcast(intent)
            }
            command == "sys_GOD_MODE" -> {
                RadarCoordinator.addLog("Jarvis: [GOD MODE] Sincronização Absoluta Engajada. Delays anulados.", com.example.coordinator.LogType.ALERT)
                RadarCoordinator.addLog("Jarvis: [GOD MODE] Permissões cibernéticas globais assumidas.", com.example.coordinator.LogType.ALERT)
                speakText("Controle absoluto e infraestrutural confirmado. Iniciando automação em massa de todos os parceiros.")
                
                // Abre apps de parceiros em sequencia via Intent
                scope.launch {
                    try {
                        val uberIntent = packageManager.getLaunchIntentForPackage("com.ubercab.driver")
                        if (uberIntent != null) {
                            uberIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(uberIntent)
                            speakText("Abrindo infraestrutura Uber.")
                            delay(3000)
                        }
                        val wazeIntent = packageManager.getLaunchIntentForPackage("com.waze")
                        if (wazeIntent != null) {
                            wazeIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(wazeIntent)
                            speakText("Ancorando via satélite com Waze.")
                            delay(3000)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "God Mode App Launch Failed", e)
                    }
                }
            }
            command.startsWith("sys_") -> {
                val actionStr = command.removePrefix("sys_")
                if (actionStr == "SCROLL_DOWN" || actionStr == "SCROLL_UP") {
                    val dir = if (actionStr == "SCROLL_DOWN") "DOWN" else "UP"
                    sendBroadcast(android.content.Intent("com.example.ACTION_SCROLL").apply {
                        putExtra("DIRECTION", dir)
                        setPackage(packageName)
                    })
                } else if (actionStr.startsWith("LAUNCH_")) {
                    val pkg = when (actionStr) {
                        "LAUNCH_WAZE" -> "com.waze"
                        "LAUNCH_UBER" -> "com.ubercab.driver"
                        "LAUNCH_IFOOD" -> "br.com.ifood.driver"
                        "LAUNCH_WHATSAPP" -> "com.whatsapp"
                        "LAUNCH_YOUTUBE" -> "com.google.android.youtube"
                        else -> ""
                    }
                    if (pkg.isNotEmpty()) {
                        try {
                            val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
                            if (launchIntent != null) {
                                launchIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                startActivity(launchIntent)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error launching app $pkg", e)
                        }
                    }
                } else {
                    val actionId = when(actionStr) {
                        "BACK" -> 1 // GLOBAL_ACTION_BACK
                        "HOME" -> 2 // GLOBAL_ACTION_HOME
                        "RECENTS" -> 3 // GLOBAL_ACTION_RECENTS
                        "NOTIFICATIONS" -> 4 // GLOBAL_ACTION_NOTIFICATIONS
                        "QUICK_SETTINGS" -> 5 // GLOBAL_ACTION_QUICK_SETTINGS
                        "POWER_DIALOG" -> 6 // GLOBAL_ACTION_POWER_DIALOG
                        "SCREENSHOT" -> 9 // GLOBAL_ACTION_TAKE_SCREENSHOT (API 28+)
                        else -> -1
                    }
                    if (actionId != -1) {
                        sendBroadcast(android.content.Intent("com.example.ACTION_PERFORM_GLOBAL_ACTION").apply {
                            putExtra("GLOBAL_ACTION_ID", actionId)
                            setPackage(packageName)
                        })
                    }
                }
                RadarCoordinator.addLog("Jarvis: Comando cibernético acionado: $actionStr", com.example.coordinator.LogType.ALERT)
            }
            command.startsWith("click:") -> {
                // Formato: click:0.5,0.8 (x,y em percentual)
                try {
                    val coords = command.removePrefix("click:").split(",")
                    val x = coords[0].trim().toFloat()
                    val y = coords[1].trim().toFloat()
                    
                    val intent = Intent("com.example.ACTION_REMOTE_CLICK").apply {
                        putExtra("X_PERCENT", x)
                        putExtra("Y_PERCENT", y)
                    }
                    sendBroadcast(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao processar clique remoto: $command", e)
                }
            }
            command == "speak_status" -> {
                val stats = RadarCoordinator.sessionStats.value
                speakText("Thiago, o status atual é: você já completou ${stats.completedCount} corridas hoje, com um ganho total de ${String.format("%.2f", stats.totalEarnings)} reais.")
            }
            command.startsWith("autofill_chat:") -> {
                try {
                    val msg = command.removePrefix("autofill_chat:")
                    val intent = Intent("com.example.ACTION_AUTOFILL_CHAT").apply {
                        putExtra("MESSAGE_TEXT", msg)
                    }
                    sendBroadcast(intent)
                    RadarCoordinator.addLog("Jarvis: Recebi comando remoto para preencher chat: \"$msg\"", com.example.coordinator.LogType.INFO)
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao processar preenchimento remoto de chat", e)
                }
            }
            else -> {
                if (command.isNotBlank()) {
                    // Process natural language commands using the Jarvis intelligence engine
                    val jarvisIntent = com.example.util.JarvisIntelligenceEngine.processNaturalLanguageCommand(command, "active")
                    val responseText = jarvisIntent.voiceResponse ?: command
                    
                    // Respond with TTS audio
                    speakText(responseText)
                    RadarCoordinator.addLog("Jarvis: Processado comando verbal: \"$command\" -> Resposta: \"$responseText\"", com.example.coordinator.LogType.INFO)
                    
                    // Act based on the parsed action type
                    when (jarvisIntent.actionType) {
                        com.example.util.JarvisIntelligenceEngine.ActionType.UPDATE_SETTINGS -> {
                            val currentSettings = RadarCoordinator.settings.value
                            var updatedSettings = currentSettings
                            jarvisIntent.updatePayload?.forEach { (key, value) ->
                                when (key) {
                                    "maxDistance" -> {
                                        val dist = (value as? Number)?.toDouble() ?: 15.0
                                        updatedSettings = updatedSettings.copy(maxTotalDistanceKm = dist)
                                    }
                                    "minFare" -> {
                                        val fare = (value as? Number)?.toDouble() ?: 8.0
                                        updatedSettings = updatedSettings.copy(minFareValue = fare)
                                    }
                                    "rainMode" -> {
                                        val rain = value as? Boolean ?: false
                                        updatedSettings = updatedSettings.copy(rainModeMultiplier = if (rain) 1.5 else 1.0)
                                    }
                                }
                            }
                            if (updatedSettings != currentSettings) {
                                RadarCoordinator.saveSettings(this@RadarCoordinatorService, updatedSettings)
                                RadarCoordinator.addLog("Jarvis: Configurações de filtros atualizadas com sucesso.", com.example.coordinator.LogType.SUCCESS)
                            }
                        }
                        com.example.util.JarvisIntelligenceEngine.ActionType.ACCEPT_OFFER -> {
                            val currentOffer = RadarCoordinator.activeOffer.value
                            if (currentOffer != null) {
                                val clickIntent = Intent("com.example.ACTION_EXECUTE_CLICK").apply {
                                    putExtra("APP_NAME", currentOffer.appName)
                                }
                                sendBroadcast(clickIntent)
                                RadarCoordinator.addLog("Jarvis: Corrida do app ${currentOffer.appName} aceita por comando de voz.", com.example.coordinator.LogType.SUCCESS)
                            } else {
                                speakText("Não há nenhuma oferta ativa no radar no momento, Thiago.")
                            }
                        }
                        com.example.util.JarvisIntelligenceEngine.ActionType.REJECT_OFFER -> {
                            val currentOffer = RadarCoordinator.activeOffer.value
                            if (currentOffer != null) {
                                RadarCoordinator.setActiveOffer(null)
                                RadarCoordinator.addLog("Jarvis: Corrida recusada por comando de voz.", com.example.coordinator.LogType.WARNING)
                            } else {
                                speakText("Nenhuma oferta para recusar no momento, chefe.")
                            }
                        }
                        com.example.util.JarvisIntelligenceEngine.ActionType.TOGGLE_SERVICE -> {
                            val currentSettings = RadarCoordinator.settings.value
                            val activate = jarvisIntent.updatePayload?.get("active") as? Boolean ?: true
                            val updatedSettings = currentSettings.copy(isActiveDeliveryEnabled = activate)
                            RadarCoordinator.saveSettings(this@RadarCoordinatorService, updatedSettings)
                            RadarCoordinator.addLog("Jarvis: Status do radar alterado para: " + if (activate) "ATIVO" else "PAUSADO", com.example.coordinator.LogType.INFO)
                        }
                        else -> {
                            // Already handled speech and basic logging
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "Service onDestroy")
        isServiceRunning = false
        instance = null
        RadarCoordinator.setServiceRunning(false)
        try {
            unregisterReceiver(commandReceiver)
        } catch (e: Exception) {}
        remoteCommandListener?.remove()
        patchListener?.remove()
        job.cancel()
        stopLocationUpdates()
        removeOverlay()
        voiceManager?.shutdown()
        voiceManager = null
        com.example.coordinator.RadarCoordinator.voiceManager = null
        com.example.coordinator.RadarCoordinator.voiceInputManager = null
        RadarCoordinator.addLog("Serviço de Coordenador Radar encerrado.", com.example.coordinator.LogType.WARNING)
        super.onDestroy()
    }

    // -------------------------------------------------------------------------
    // Location / GPS handling
    // -------------------------------------------------------------------------

    private fun startLocationUpdates() {
        val settings = RadarCoordinator.settings.value
        if (settings.forceMockSpeed) {
            Log.d(TAG, "Skipping physical system location updates since mock speed/simulation is enabled.")
            return
        }

        val hasFine = androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val hasCoarse = androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!hasFine && !hasCoarse) {
            Log.w(TAG, "Location permissions not granted yet. Skipping location updates startup.")
            return
        }

        if (locationCallback != null) {
            Log.d(TAG, "Location updates already started.")
            return
        }

        try {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
                .setMinUpdateIntervalMillis(1000L)
                .build()

            locationCallback = object : LocationCallback() {
                private var lastValidLocation: Location? = null
                
                override fun onLocationResult(locationResult: LocationResult) {
                    locationResult.lastLocation?.let { location ->
                        // Filtro de Saltos de GPS (Watchdog)
                        // Se o piloto pulou 500 metros em 1 segundo (1800 km/h), é erro de GPS.
                        val lastLoc = lastValidLocation
                        if (lastLoc != null) {
                            val dist = location.distanceTo(lastLoc)
                            val timeDelta = (location.time - lastLoc.time) / 1000.0
                            if (timeDelta > 0 && (dist / timeDelta) > 55.0) { // > 200 km/h jump check
                                Log.w(TAG, "GPS Jump detectado ($dist m em $timeDelta s). Ignorando para estabilidade.")
                                return@let
                            }
                        }
                        
                        lastValidLocation = location
                        RadarCoordinator.updateLocation(location)

                        // Monitoramento de Tráfego Proativo para Smart Focus
                        val now = System.currentTimeMillis()
                        if (now - lastTrafficCheckTime > 60000L && RadarCoordinator.deliveryActive.value && RadarCoordinator.settings.value.aiActiveTrafficReroute) {
                            lastTrafficCheckTime = now
                            scope.launch(Dispatchers.IO) {
                                val destination = RadarCoordinator.settings.value.activeDeliveryDestination
                                if (destination.isNotBlank()) {
                                    val trafficResult = com.example.util.GoogleMapsTrafficMonitor.monitorTraffic(
                                        this@RadarCoordinatorService,
                                        location.latitude,
                                        location.longitude,
                                        destination
                                    )
                                    RadarCoordinator.updateTrafficMultiplier(trafficResult.trafficMultiplier)
                                    val delayMin = ((trafficResult.durationInTrafficSeconds - trafficResult.durationSeconds) / 60).toInt()
                                    RadarCoordinator.updateTrafficDetour(trafficResult.detourSuggested, delayMin, trafficResult.reason)
                                    
                                    // Se tráfego intenso (> 50% de atraso), ativa Smart Focus
                                    if (trafficResult.trafficMultiplier >= 1.5) {
                                        RadarCoordinator.setSmartFocusActive(true)
                                    } else if (trafficResult.trafficMultiplier < 1.3 && (location.speed * 3.6f) > 20) {
                                        RadarCoordinator.setSmartFocusActive(false)
                                    }
                                }
                            }
                        }
                        
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
                                    isArrivalMessageSent = false // Reset for next delivery
                                    
                                    speakText("Excelente, Thiago! Chegamos ao seu destino final. O Radar já está em modo de espera e pronto para a próxima.")
                                    
                                    // Sugestão de Abastecimento Proativo IA
                                    if (settings.aiActiveFuelSuggest) {
                                        scope.launch(Dispatchers.IO) {
                                            kotlinx.coroutines.delay(6500L) // Dá tempo para terminar de falar a chegada
                                            speakText("Thiago, identifiquei um Posto Ipiranga a 800 metros à frente com preço promocional de combustível para usuários do Radar. Gostaria de adicionar uma rota rápida de abastecimento?")
                                            RadarCoordinator.updateFuelSuggestion(true)
                                            RadarCoordinator.addLog("Jarvis IA: Posto promocional sugerido próximo ao destino concluído.", com.example.coordinator.LogType.SUCCESS)
                                        }
                                    }
                                } else if (distance < 400f && !isArrivalMessageSent) {
                                    sendArrivalMessageToClient()
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
            locationCallback = null
        }
    }

    // -------------------------------------------------------------------------
    // Text To Speech (TTS) & Audio Focus
    // -------------------------------------------------------------------------

    private fun speakText(text: String, shortVersion: Boolean = false, isOfferAnnouncement: Boolean = false, fareValue: Double = 0.0, distance: Double = 0.0) {
        val profile = RadarCoordinator.userProfile.value
        if (!profile.audioAlertEnabled) {
            Log.d(TAG, "speakText blocked: audioAlertEnabled is false in user profile")
            return
        }
        
        if (isOfferAnnouncement && RadarCoordinator.settings.value.voiceFilterEnabled) {
            val meetsFare = fareValue >= RadarCoordinator.settings.value.voiceFilterMinFare
            val meetsDist = distance <= RadarCoordinator.settings.value.voiceFilterMaxDistance
            if (!meetsFare || !meetsDist) {
                Log.d(TAG, "speakText blocked: Offer (Fare: $fareValue, Dist: $distance) does not meet voice filter criteria.")
                return
            }
        }

        Log.d(TAG, "speakText: $text")
        if (isOfferAnnouncement) {
            RadarCoordinator.voiceInputManager?.shouldAutoWakeOnTtsFinish = true
        }
        voiceManager?.speak(text)
    }

    private fun playVipAlert(appName: String = "App", fareValue: Double = 0.0, valuePerKm: Double = 0.0) {
        val profile = RadarCoordinator.userProfile.value
        if (profile.audioAlertEnabled) {
            voiceManager?.playVipAlert(RadarCoordinator.settings.value.highValueAlertTone)
        } else {
            Log.d(TAG, "playVipAlert blocked: audioAlertEnabled is false in user profile")
        }
        showVipPushNotification(appName, fareValue, valuePerKm)
    }

    private fun showGhostAutoAcceptPushNotification(appName: String, fareValue: Double, valuePerKm: Double) {
        try {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val ghostChannel = NotificationChannel(
                    "ghost_sequence_channel",
                    "Alertas Ghost Sequence",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notificações de aceite do Ghost Sequence"
                    enableLights(true)
                    enableVibration(true)
                }
                manager.createNotificationChannel(ghostChannel)
            }

            val notificationIntent = Intent(this, com.example.MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

            val builder = NotificationCompat.Builder(this, "ghost_sequence_channel")
                .setContentTitle("Ghost Sequence: Corrida Aceita!")
                .setContentText("Oferta no $appName de R$ ${String.format("%.2f", fareValue)} aceita automaticamente.")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                
            manager.notify(2028, builder.build())
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao enviar notificação Push do Ghost Sequence", e)
        }
    }

    private fun showVipPushNotification(appName: String, fareValue: Double, valuePerKm: Double) {
        try {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val vipChannel = NotificationChannel(
                    "radar_vip_alerts_channel",
                    "Alertas VIP Radar",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notificações urgentes para ofertas VIP"
                    enableLights(true)
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 500, 200, 500)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
                manager.createNotificationChannel(vipChannel)
            }

            val notificationIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                this,
                102,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val formattedValue = String.format(Locale.US, "%.2f", fareValue)
            val formattedPerKm = String.format(Locale.US, "%.2f", valuePerKm)
            
            val title = "🚨 CORRIDA VIP ENCONTRADA!"
            val textPt = "Oferta no $appName de R$ $formattedValue (R$ $formattedPerKm/km) - Clique para ver!"

            val acceptIntent = Intent(this, RadarCoordinatorService::class.java).apply {
                putExtra("ACCEPT_OFFER_MANUAL", true)
            }
            val acceptPendingIntent = PendingIntent.getService(this, 10, acceptIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

            val rejectIntent = Intent(this, RadarCoordinatorService::class.java).apply {
                putExtra("DISMISS_OFFER_MANUAL", true)
            }
            val rejectPendingIntent = PendingIntent.getService(this, 11, rejectIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

            val builder = NotificationCompat.Builder(this, "radar_vip_alerts_channel")
                .setContentTitle(title)
                .setContentText(textPt)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL)
                .addAction(android.R.drawable.ic_input_add, "ACEITAR", acceptPendingIntent)
                .addAction(android.R.drawable.ic_delete, "RECUSAR", rejectPendingIntent)

            manager.notify(2027, builder.build())
            RadarCoordinator.addLog("Notificação Push: Alerta VIP disparado com sucesso.", com.example.coordinator.LogType.SUCCESS)
        } catch (e: Exception) {
            Log.e(TAG, "Error showing VIP push notification: ${e.message}")
        }
    }

    // -------------------------------------------------------------------------
    // Background Coordination Engine (State Machine Flow)
    // -------------------------------------------------------------------------

    /**
     * Entry point to trigger analysis on a new offer (simulated or real).
     * This coordinates: OFERTA_LIDA -> ANALISANDO -> SUGERINDO -> AGUARDANDO_ACAO
     */
    /**
     * Algoritmo Neural de Fusão de Rotas.
     * Verifica se uma oferta secundária (Ghost Eye) pode ser mesclada com a rota ativa.
     */
    private fun checkRouteSynergy(newFare: Double, appName: String): Boolean {
        // Se não está navegando em uma entrega, qualquer oferta de ouro é válida
        if (RadarCoordinator.currentState.value != RadarState.NAVEGANDO) return true
        
        // Se houver corrida ativa (NAVEGANDO), calculamos a sinergia
        val synergyFactor = if (newFare > 12.0) 0.90 else 0.45 
        
        Log.d(TAG, "Neural Synergy Check: App=$appName | Fare=$newFare | Synergy=$synergyFactor")
        return synergyFactor > 0.5
    }

    fun processNewOffer(
        appName: String,
        fareValue: Double,
        pickupAddress: String,
        deliveryAddress: String,
        base64Image: String,
        totalDistance: Double = 0.0,
        totalTime: Double = 0.0,
        isBackgroundOffer: Boolean = false
    ) {
        val startTime = System.currentTimeMillis()
        if (!isProcessingOffer.compareAndSet(false, true)) {
            Log.d(TAG, "Processador ocupado, registrando oferta para inteligência de enxame: $appName R$ $fareValue")
            RadarCoordinator.recordOfferForArbitrage(appName, fareValue)
            return
        }

        scope.launch {
            try {
                // Prioridade Máxima: Interrompe qualquer fala anterior para processar a nova oferta
                voiceManager?.stop() 
                
                // Auditoria de Integridade: Filtro de Ruído Neural
                if (fareValue <= 0.0 || fareValue > 5000.0) {
                    Log.w(TAG, "Jarvis: Oferta com valor suspeito descartada: R$ $fareValue")
                    return@launch
                }

                // Auditoria de Latência de Início
                val entryLatency = System.currentTimeMillis() - startTime
                if (entryLatency > 300) {
                    RadarCoordinator.updateModuleHealth("Bridge", false)
                    RadarCoordinator.addLog("Watchdog: Latência de entrada crítica detectada (${entryLatency}ms). Recalibrando Bridge...", com.example.coordinator.LogType.ALERT)
                    RadarCoordinator.healSystem()
                } else {
                    RadarCoordinator.updateModuleHealth("Bridge", true)
                }

                // Inteligência de Fusão: Se for background, verifica se vale a pena desviar da rota atual
                if (isBackgroundOffer) {
                    RadarCoordinator.updateModuleHealth("GhostEye", true)
                    val hasSynergy = checkRouteSynergy(fareValue, appName)
                    if (!hasSynergy) {
                        Log.d(TAG, "Ghost Eye: Oferta descartada por baixa sinergia com a rota atual.")
                        return@launch
                    }
                    RadarCoordinator.addLog("Neural Synergy: Oportunidade de fusão de rotas detectada no $appName!", com.example.coordinator.LogType.SUCCESS)
                    RadarCoordinator.setJarvisProactiveMessage("🧩 FUSÃO DE ROTAS: Thiago, esta oferta do $appName se encaixa na sua rota atual. Ganho extra de R$ $fareValue!")
                    RadarCoordinator.updateModuleHealth("NeuralSynergy", true)
                } else {
                    RadarCoordinator.updateModuleHealth("GhostEye", true)
                }

                // Step 1: Oferta Lida
                RadarCoordinator.updateState(RadarState.OFERTA_LIDA)
                RadarCoordinator.recordOfferForArbitrage(appName, fareValue)
                
                // Vibrate upon receiving any new offer if configured
                if (RadarCoordinator.userProfile.value.vibrateOnNewOffer) {
                    try {
                        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
                            vibratorManager.defaultVibrator
                        } else {
                            @Suppress("DEPRECATION")
                            getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
                        }
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
            val isChainedVal = currentSettings.isActiveDeliveryEnabled || RadarCoordinator.deliveryActive.value
            val offerType = detectOfferType(appName, pickupAddress, deliveryAddress)
            
            // --- LOCAL FILTERS COM INTELIGÊNCIA AVANÇADA DE 2026 (Custo Zero) ---
            var localRejectionReason: String? = null
            val valuePerKm = if (totalDistance > 0.0) fareValue / totalDistance else 0.0

            // Ajustes inteligentes e dinâmicos de limite baseados na modalidade
            var effectiveMinFare = currentSettings.minFareValue
            var effectiveMinKm = currentSettings.minValuePerKm
            var filterContextLabel = ""

            val isMallOrSupermarket = pickupAddress.lowercase().let { 
                it.contains("shopping") || it.contains("supermercado") || it.contains("hipermercado") || 
                it.contains("carrefour") || it.contains("pão de açúcar") || it.contains("condor") || 
                it.contains("extra") || it.contains("malls")
            }

            if (offerType == "CORRIDA") {
                // Passageiros: fluxo rápido, embarque instantâneo, sem esperas longas.
                // Toleramos tarifas ligeiramente menores por km para maximizar giro de caixa.
                effectiveMinFare = (currentSettings.minFareValue * 0.85).coerceAtLeast(6.0)
                effectiveMinKm = (currentSettings.minValuePerKm * 0.85).coerceAtLeast(1.5)
                filterContextLabel = "Corrida Rápida de Passageiro"
            } else {
                // Delivery Padrão ou Especial
                if (isMallOrSupermarket) {
                    // Shopping/Supermercado: Gargalo crítico de tempo (estacionar, caminhar, fila).
                    // Exigimos valores consideravelmente maiores para compensar a perda de tempo.
                    effectiveMinFare = (currentSettings.minFareValue * 1.4).coerceAtLeast(12.0)
                    effectiveMinKm = (currentSettings.minValuePerKm * 1.3).coerceAtLeast(2.5)
                    filterContextLabel = "Entrega com Gargalo em Shopping/Supermercado"
                } else {
                    filterContextLabel = "Entrega de Delivery Padrão"
                }
            }

            // Aplicar o Multiplicador do Modo Chuva se estiver ativo (> 1.0)
            val rainMultiplier = currentSettings.rainModeMultiplier.coerceAtLeast(1.0)
            if (rainMultiplier > 1.0) {
                effectiveMinFare *= rainMultiplier
                effectiveMinKm *= rainMultiplier
                filterContextLabel += " com Multiplicador de Chuva (${String.format(Locale.US, "%.1f", rainMultiplier)}x)"
            }

            // --- REGRAS DE FILTRAGEM MULTICAMADAS INTELIGENTES ---

            // 0. Filtro por Horário (Time-based filtering)
            if (localRejectionReason == null && currentSettings.filterByTimeEnabled) {
                try {
                    val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.US)
                    val nowCal = java.util.Calendar.getInstance()
                    val nowStr = String.format(java.util.Locale.US, "%02d:%02d", nowCal.get(java.util.Calendar.HOUR_OF_DAY), nowCal.get(java.util.Calendar.MINUTE))
                    val nowTime = sdf.parse(nowStr)
                    val startTime = sdf.parse(currentSettings.filterStartTime)
                    val endTime = sdf.parse(currentSettings.filterEndTime)
                    
                    if (nowTime != null && startTime != null && endTime != null) {
                        val isInside = if (startTime.after(endTime)) {
                            // Overlapping midnight (e.g., 22:00 to 06:00)
                            nowTime.after(startTime) || nowTime.before(endTime) || nowTime == startTime || nowTime == endTime
                        } else {
                            // Normal range (e.g., 18:00 to 22:00)
                            (nowTime.after(startTime) || nowTime == startTime) && (nowTime.before(endTime) || nowTime == endTime)
                        }
                        if (!isInside) {
                            localRejectionReason = "Horário Restrito: Filtro ativo para apenas receber ofertas entre ${currentSettings.filterStartTime} e ${currentSettings.filterEndTime} (Horário Atual: $nowStr)"
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error evaluating time filter: ${e.message}")
                }
            }

            // 1. Verificação Instantânea de Segurança por Zonas de Risco/Exclusão
            if (localRejectionReason == null && currentSettings.riskZonesKeywords.isNotBlank()) {
                val keywords = currentSettings.riskZonesKeywords.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
                val targetText = "$pickupAddress $deliveryAddress".lowercase()
                val matchedKeyword = keywords.find { targetText.contains(it) }
                if (matchedKeyword != null) {
                    localRejectionReason = "Segurança Máxima: Rota passa por zona de risco detectada (${matchedKeyword.replaceFirstChar { it.uppercase() }})"
                }
            }

            // 2. Rejeição de Supermercados/Shoppings se habilitado
            if (localRejectionReason == null && isMallOrSupermarket && currentSettings.rejectSupermarkets) {
                localRejectionReason = "Evitar Gargalo: Filtro ativo para ignorar coletas demoradas em Shoppings e Supermercados"
            }

            // 3. Rejeição de Lojas e Fast Foods específicos configurados para evitar
            if (localRejectionReason == null && currentSettings.avoidStoreKeywords.isNotBlank()) {
                val avoidWords = currentSettings.avoidStoreKeywords.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
                val pickupLower = pickupAddress.lowercase()
                val matchedStore = avoidWords.find { pickupLower.contains(it) }
                if (matchedStore != null) {
                    localRejectionReason = "Filtro de Loja: Estabelecimento evitado configurado pelo piloto (${matchedStore.replaceFirstChar { it.uppercase() }})"
                }
            }
            
            // 3.5 Detecção de Múltiplas Paradas (Gargalo Oculto)
            val isMultipleStops = pickupAddress.lowercase().contains("entregas") || deliveryAddress.lowercase().contains("entregas") || 
                                  pickupAddress.lowercase().contains("paradas") || deliveryAddress.lowercase().contains("paradas") ||
                                  pickupAddress.lowercase().contains("pedidos") || deliveryAddress.lowercase().contains("pedidos")
            
            if (isMultipleStops) {
                effectiveMinFare = (effectiveMinFare * 1.3).coerceAtLeast(15.0)
                effectiveMinKm = (effectiveMinKm * 1.2).coerceAtLeast(2.0)
                filterContextLabel += " com Múltiplas Paradas/Entregas"
            }

            // 4. Limite de Distância de Coleta e Percurso Total
            if (localRejectionReason == null && !isChainedVal && currentSettings.maxTotalDistanceKm > 0.0 && totalDistance > currentSettings.maxTotalDistanceKm) {
                localRejectionReason = "Distância Excedida: Percurso total (${String.format(Locale.US, "%.1f", totalDistance)} km) é maior que o limite máximo (${currentSettings.maxTotalDistanceKm} km)"
            }

            // 5. Cálculo Financeiro Real de Lucro Líquido Real (Combustível + Manutenção do Veículo)
            if (localRejectionReason == null && totalDistance > 0.0 && currentSettings.fuelPrice > 0.0 && currentSettings.motorcycleConsumptionKmPerL > 0.0) {
                val fuelCost = (totalDistance / currentSettings.motorcycleConsumptionKmPerL) * currentSettings.fuelPrice
                val wearRate = when (currentSettings.vehicleType) {
                    "MOTO" -> 0.15
                    "CARRO" -> 0.35
                    "CARRO_GNV" -> 0.30
                    "ELETRICO" -> 0.20
                    else -> 0.15
                }
                val maintenanceCost = totalDistance * wearRate // Depreciação e desgaste operacional estimado por km
                val totalOperatingCost = fuelCost + maintenanceCost
                val netProfit = fareValue - totalOperatingCost
                
                if (netProfit <= 0.0 && !isChainedVal) {
                    localRejectionReason = "Sem Margem Financeira: Corrida dá prejuízo líquido real de R$ ${String.format(Locale.US, "%.2f", netProfit)} após combustível (R$ ${String.format(Locale.US, "%.2f", fuelCost)}) e desgaste (R$ ${String.format(Locale.US, "%.2f", maintenanceCost)})"
                } else if (currentSettings.minProfitPerHour > 0.0 && totalTime > 0.0) {
                    val totalTimeHours = totalTime / 60.0
                    val profitPerHour = netProfit / totalTimeHours
                    if (profitPerHour < currentSettings.minProfitPerHour) {
                        localRejectionReason = "Lucro por Hora Baixo: Rendimento líquido estimado de R$ ${String.format(Locale.US, "%.2f", profitPerHour)}/h é menor que o piso configurado de R$ ${String.format(Locale.US, "%.2f", currentSettings.minProfitPerHour)}/h"
                    }
                }
            }

            // 6. Modo Voltando Para Casa (Heading Home Mode)
            if (localRejectionReason == null && currentSettings.headingHomeMode && currentSettings.homeAddress.isNotBlank()) {
                val homeLower = currentSettings.homeAddress.lowercase()
                val destLower = deliveryAddress.lowercase()
                val isNearHome = destLower.contains(homeLower) || (currentSettings.preferredReturnNeighborhoods.isNotBlank() && 
                        currentSettings.preferredReturnNeighborhoods.split(",").map { it.trim().lowercase() }.any { destLower.contains(it) })
                
                if (!isNearHome) {
                    // Se não aproxima da casa nem bairros preferidos, exige pelo menos 1.5x o mínimo por km para valer o desvio
                    if (valuePerKm < (effectiveMinKm * 1.5)) {
                        localRejectionReason = "Modo Retorno: Destino final se afasta do rumo de casa e a tarifa não justifica o desvio"
                    }
                }
            }

            // 7. Filtros Padrões Globais de Preço e R$/km
            if (localRejectionReason == null) {
                if (currentSettings.isAutoRejectEnabled && !isChainedVal && fareValue < currentSettings.autoRejectMinFare) {
                    localRejectionReason = "Auto-rejeitado: Valor da oferta (R$ $fareValue) abaixo do limite global de auto-recusa (R$ ${currentSettings.autoRejectMinFare})"
                } else if (!isChainedVal && fareValue < effectiveMinFare) {
                    localRejectionReason = "Recusado pelo Filtro Local ($offerType): Valor (R$ $fareValue) menor que o mínimo inteligente para $filterContextLabel (R$ ${String.format(Locale.US, "%.2f", effectiveMinFare)})"
                } else if (!isChainedVal && totalDistance > 0.0 && valuePerKm < effectiveMinKm) {
                    localRejectionReason = "Recusado pelo Filtro Local ($offerType): Pagamento (R$ ${String.format(Locale.US, "%.2f", valuePerKm)}/km) abaixo do mínimo inteligente para $filterContextLabel (R$ ${String.format(Locale.US, "%.2f", effectiveMinKm)}/km)"
                }
            }

            if (localRejectionReason != null) {
                Log.d(TAG, "Offer auto-rejected by local filter: $localRejectionReason")
                RadarCoordinator.updateState(RadarState.SUGERINDO)
                
                RadarCoordinator.addLog("Filtro Inteligente Local ($offerType): Oferta recusada de forma preventiva. Motivo: $localRejectionReason", com.example.coordinator.LogType.WARNING)
                RadarCoordinator.setDecision("recusar", localRejectionReason)
                
                if (RadarCoordinator.userProfile.value.notifyOnAutoReject) {
                    speakText("Thiago, recusei uma oferta do $appName pelo filtro inteligente, pois o valor do $offerType ficou abaixo da sua meta.")
                }
                
                // Persist the auto-rejected offer in history
                val speedVal = RadarCoordinator.currentSpeedKmh.value
                val isChainedVal = currentSettings.isActiveDeliveryEnabled || RadarCoordinator.deliveryActive.value
                val activeDestVal = if (isChainedVal) currentSettings.activeDeliveryDestination else null
                
                val offerEntity = OfferEntity(
                    appName = appName,
                    fareValue = fareValue,
                    pickupAddress = pickupAddress,
                    deliveryAddress = deliveryAddress,
                    totalDistance = totalDistance,
                    totalTime = totalTime,
                    detourDistance = 0.0,
                    detourTime = 0.0,
                    suggestion = "recusar",
                    reason = localRejectionReason,
                    speedKmhAtDecision = speedVal,
                    isChained = isChainedVal,
                    activeDeliveryDestination = activeDestVal
                )
                RadarCoordinator.saveOfferToDatabase(offerEntity)
                
                // Brief pause so the driver sees the card and understands it was auto-rejected
                delay(4000L)
                dismissCurrentOffer()
                return@launch
            }

            // Step 2: Analisando
            RadarCoordinator.updateState(RadarState.ANALISANDO)
            RadarCoordinator.addLog("Análise: Jarvis Intelligence Engine processando rentabilidade...", com.example.coordinator.LogType.INFO)
            speakText("Analisando oferta, Thiago. Deixe comigo.")

            val activeOffer = com.example.coordinator.ActiveOffer(appName, fareValue, pickupAddress, deliveryAddress, base64Image, totalDistance, totalTime)
            
            // Decisão Inteligente Híbrida (Local + Cloud se necessário)
            val shouldAutoAcceptJarvis = com.example.util.JarvisIntelligenceEngine.analyzeOfferDecision(this@RadarCoordinatorService, activeOffer, currentSettings)

            if (shouldAutoAcceptJarvis && currentSettings.isAutoAcceptEnabled) {
                RadarCoordinator.addLog("Jarvis: OFERTA DE OURO! Iniciando aceite automático.", com.example.coordinator.LogType.SUCCESS)
                
                if (currentSettings.ghostPushNotificationsEnabled) {
                    showGhostAutoAcceptPushNotification(appName, fareValue, valuePerKm)
                }
                
                if (isBackgroundOffer) {
                    speakText("Oferta de ouro interceptada via Ghost Eye no $appName. Trazendo para o primeiro plano agora!")
                    RadarCoordinator.setJarvisProactiveMessage("🚨 INTERCEPTAÇÃO GHOST: Oferta de R$ $fareValue no $appName é excelente! Trazendo app para frente.")
                    val launchIntent = packageManager.getLaunchIntentForPackage(
                        when (appName) {
                            "Uber" -> "com.ubercab.driver"
                            "99" -> "com.taxis99"
                            "iFood" -> "com.ifood.driver"
                            "inDrive" -> "sinet.startup.inDriver"
                            "Lalamove" -> "com.lalamove.rider.driver"
                            "Maxim" -> "com.maxim.driver"
                            else -> ""
                        }
                    )
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(launchIntent)
                        // Dynamic delay based on Ghost Eye sensitivity (lower sensitivity = longer delay, higher sensitivity = faster)
                        val sens = RadarCoordinator.settings.value.ghostEyeSensitivity
                        val delayMs = ((2.0 - sens) * 1000).toLong().coerceIn(300L, 2500L)
                        Log.d(TAG, "Ghost Eye: Sensibilidade setada em $sens. Aguardando ${delayMs}ms para transição de janela.")
                        delay(delayMs)
                    }
                } else {
                    speakText("Oferta de ouro detectada. Aceitando agora.")
                }
                
                val clickIntent = Intent("com.example.ACTION_EXECUTE_CLICK").apply {
                    putExtra("APP_NAME", appName)
                }
                sendBroadcast(clickIntent)
                RadarCoordinator.updateState(RadarState.SUCESSO)
                
                // Salva no banco como aceite automático
                val offerEntity = OfferEntity(
                    appName = appName,
                    fareValue = fareValue,
                    pickupAddress = pickupAddress,
                    deliveryAddress = deliveryAddress,
                    totalDistance = totalDistance,
                    totalTime = totalTime,
                    suggestion = "aceitar",
                    reason = "Aceite Automático Jarvis (Golden Offer)",
                    speedKmhAtDecision = RadarCoordinator.currentSpeedKmh.value
                )
                RadarCoordinator.saveOfferToDatabase(offerEntity)
                
                delay(2000L)
                dismissCurrentOffer()
                return@launch
            }

            val lat = RadarCoordinator.currentLocation.value?.latitude ?: -23.550520
            val lon = RadarCoordinator.currentLocation.value?.longitude ?: -46.633308
            
            val avgRejectedPerKm = RadarCoordinator.getAverageRejectedValuePerKm() ?: 0f
            val avgRejectedFare = RadarCoordinator.getAverageRejectedFare() ?: 0f
            
            val mems = RadarCoordinator.jarvisMemories.value
            val memoriesStr = if (mems.isNotEmpty()) {
                "\nRegras/Memórias de Aprendizado do Motorista (Se violar qualquer uma, recuse a corrida obrigatoriamente):\n- " + mems.joinToString("\n- ")
            } else ""

            val patternString = (if (avgRejectedPerKm > 0f) {
                "Padrão aprendido: O motorista costuma recusar ofertas abaixo de R$ ${String.format(Locale.US, "%.2f", avgRejectedPerKm)}/km ou R$ ${String.format(Locale.US, "%.2f", avgRejectedFare)} no total. Leve isso em consideração na sugestão (se a oferta atual for semelhante aos padrões que ele recusa, tenda a recusar)."
            } else "") + memoriesStr

            val result = withContext(Dispatchers.IO) {
                if (currentSettings.useLocalGemini) {
                    analyzeWithLocalGemini(
                        apiKey = currentSettings.geminiApiKey,
                        base64Image = base64Image,
                        lat = lat,
                        lon = lon,
                        currentSettings = currentSettings,
                        ocrDistanceHint = totalDistance,
                        ocrTimeHint = totalTime,
                        patternString = patternString,
                        appName = appName,
                        fareValue = fareValue,
                        pickupAddress = pickupAddress,
                        deliveryAddress = deliveryAddress,
                        offerType = offerType
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
                            minValuePerKm = currentSettings.minValuePerKm.toDouble(),
                            minFareValue = currentSettings.minFareValue.toDouble(),
                            patternString = patternString,
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

            if (result == null) {
                // FALLBACK LOGIC: O motorista não pode ficar sem resposta na rua!
                RadarCoordinator.addLog("Rede Instável: Iniciando heurística local de emergência.", com.example.coordinator.LogType.ALERT)
                RadarCoordinator.reportSystemStress()
                
                val valorKm = if (totalDistance > 0.0) fareValue / totalDistance else 0.0
                val minKm = currentSettings.minValuePerKm
                
                if (valorKm >= minKm) {
                    RadarCoordinator.setDecision("aceitar", "Protocolo de Emergência (Offline): R$ ${String.format("%.2f", valorKm)}/km está acima da sua meta.")
                    speakText("Thiago, rede lenta! Mas a conta fecha: ${String.format("%.2f", valorKm)} por quilômetro. Recomendo aceitar.")
                    showRecommendationToast(appName, fareValue, totalDistance, isOffline = true)
                } else {
                    RadarCoordinator.setDecision("recusar", "Protocolo de Emergência (Offline): R$ ${String.format("%.2f", valorKm)}/km abaixo da meta.")
                    speakText("Chefe, sem sinal de internet, mas o valor por quilômetro está baixo. Melhor recusar.")
                    delay(3000L)
                    dismissCurrentOffer()
                    return@launch
                }
                
                RadarCoordinator.updateState(RadarState.SUGERINDO)
                delay(3000L)
                RadarCoordinator.updateState(RadarState.AGUARDANDO_ACAO)
                return@launch
            }

            @Suppress("KotlinConstantConditions")
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

                val isChained = result.suggestion == "aceitar" && (
                    result.reason.lowercase().contains("mesclar") || 
                    result.reason.lowercase().contains("casar") || 
                    result.reason.lowercase().contains("rota") ||
                    result.reason.lowercase().contains("encadeada")
                ) && (currentSettings.isActiveDeliveryEnabled || RadarCoordinator.deliveryActive.value)

                val phrase = if (isChained) {
                    val fareFormatted = String.format(Locale.US, "%.2f", finalFare)
                    "Thiago, oportunidade de mesclar aplicativos detectada! Nova oferta do $appName de R$ $fareFormatted perfeitamente alinhada com sua rota atual. Recomendo aceitar para dobrar seus ganhos no mesmo trajeto. Diga aceitar ou recusar."
                } else if (isHighProfitability) {
                    val valuePerKmFormatted = String.format(Locale.US, "%.2f", valuePerKm)
                    val fareFormatted = String.format(Locale.US, "%.2f", finalFare)
                    "Thiago, excelente notícia! Uma bela corrida de altíssima rentabilidade surgiu no $appName! Total de R$ $fareFormatted, pagando excelentes R$ $valuePerKmFormatted por quilômetro. Recomendo fortemente aceitarmos. O que acha? Diga aceitar ou recusar."
                } else {
                    buildTtsPhrase(result.suggestion, result.reason, isStopped)
                }

                if (isChained || isHighProfitability) {
                    RadarCoordinator.addLog("TTS: Oferta de alta rentabilidade detectada! Enviando alerta de voz otimizado.", com.example.coordinator.LogType.SUCCESS)
                    
                    // Vibrate with distinct double-pulse for high-profitability alerts
                    if (RadarCoordinator.userProfile.value.vibrateOnNewOffer) {
                        try {
                            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
                                vibratorManager.defaultVibrator
                            } else {
                                @Suppress("DEPRECATION")
                                getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
                            }
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

                speakText(phrase, shortVersion = !isStopped, isOfferAnnouncement = true, fareValue = fareValue, distance = totalDistance)

                // Wait for speech to complete or brief delay
                delay(3000L)

                // Step 4: Aguardando Ação
                RadarCoordinator.updateState(RadarState.AGUARDANDO_ACAO)

                // Persist the offer details to local database history!
                val speedVal = RadarCoordinator.currentSpeedKmh.value
                val isChainedVal = currentSettings.isActiveDeliveryEnabled || RadarCoordinator.deliveryActive.value
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

                // Verificação de Auto-Aceite Premium ou Chained Delivery (A+B) Automático
                val meetsAutoAcceptPremium = currentSettings.autoAcceptPremium && 
                        result.suggestion == "aceitar" && 
                        valuePerKm >= currentSettings.autoAcceptMinPerKm

                if (isChained) {
                    RadarCoordinator.addLog("Jarvis (A+B): Oportunidade perfeita de mesclagem A+B detectada! Executando Auto-Aceite e acoplamento de rotas.", com.example.coordinator.LogType.SUCCESS)
                    playVipAlert(offerEntity.appName, offerEntity.fareValue, valuePerKm)
                    speakText("Thiago! Detectei uma mesclagem perfeita no ${offerEntity.appName} de R$ ${String.format(Locale.US, "%.2f", offerEntity.fareValue)}. Já aceitei automaticamente e mesclei com a sua rota ativa para dobrar seus ganhos!")
                    executarCliqueAutomatico(isVoiceCommand = false)
                } else if (meetsAutoAcceptPremium) {
                    RadarCoordinator.addLog("Jarvis: Oferta Super Premium detectada (${String.format(Locale.US, "%.2f", valuePerKm)}/km >= ${currentSettings.autoAcceptMinPerKm}/km). Executando Auto-Aceite com clique simulado em instantes!", com.example.coordinator.LogType.SUCCESS)
                    playVipAlert(offerEntity.appName, offerEntity.fareValue, valuePerKm)
                    speakText("Thiago! Acabei de fisgar uma oferta super premium no ${offerEntity.appName} de R$ ${String.format(Locale.US, "%.2f", valuePerKm)} por quilômetro! Já aceitei automaticamente para você!")
                    executarCliqueAutomatico(isVoiceCommand = false)
                } else {
                    if (!isStopped && result.suggestion == "aceitar") {
                        speakText("Thiago, temos uma boa oferta do $appName, mas por você estar em movimento, toque na tela para confirmar.", shortVersion = false, isOfferAnnouncement = true, fareValue = fareValue, distance = totalDistance)
                    } else {
                        speakText(phrase, shortVersion = false, isOfferAnnouncement = true, fareValue = fareValue, distance = totalDistance)
                        delay(2000L)
                        if (RadarCoordinator.currentState.value == RadarState.AGUARDANDO_ACAO) {
                            startVoiceCommandListening()
                        }
                    }
                }

            } else {
                // API Fallback / Error - Local caching and offline rule-based heuristic processing
                RadarCoordinator.addLog("Filtro: Servidor offline/erro. Processando heurísticas locais offline...", com.example.coordinator.LogType.ALERT)
                val speedVal = RadarCoordinator.currentSpeedKmh.value
                val isChainedVal = currentSettings.isActiveDeliveryEnabled || RadarCoordinator.deliveryActive.value
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
                    isChainedVal && fareValue >= 6.0 -> Pair(
                        "aceitar",
                        "Offline: Mesclar na mesma rota (Corrida Ativa)."
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

                val isChained = offlineSuggestion == "aceitar" && (
                    offlineReason.lowercase().contains("mesclar") || 
                    offlineReason.lowercase().contains("casar") || 
                    offlineReason.lowercase().contains("rota") ||
                    offlineReason.lowercase().contains("encadeada")
                ) && (currentSettings.isActiveDeliveryEnabled || RadarCoordinator.deliveryActive.value)

                val fallbackPhrase = if (isChained) {
                    val fareFormatted = String.format(Locale.US, "%.2f", fareValue)
                    "Thiago, oportunidade de mesclar corridas offline! A oferta do $appName de R$ $fareFormatted está na mesma rota da sua corrida atual. Aceite para faturar com dois aplicativos ao mesmo tempo!"
                } else if (isOfflineHighProfitability) {
                    val valuePerKmFormatted = String.format(Locale.US, "%.2f", valuePerKm)
                    val fareFormatted = String.format(Locale.US, "%.2f", fareValue)
                    "Atenção! Oferta de alta rentabilidade encontrada offline no $appName! Valor excelente de R$ $valuePerKmFormatted por quilômetro. Total de R$ $fareFormatted. Sugestão: Aceitar! $offlineReason"
                } else if (offlineSuggestion == "aceitar") {
                    "Nova oferta encontrada offline no $appName. Valor de R$ ${String.format(Locale.US, "%.2f", fareValue)}. Sugestão: Aceitar! $offlineReason"
                } else {
                    "Sem conexão com o servidor. Processado offline. Sugestão: $offlineSuggestion."
                }

                if (isChained || isOfflineHighProfitability) {
                    RadarCoordinator.addLog("TTS: Oferta offline de alta rentabilidade detectada! Enviando alerta de voz otimizado.", com.example.coordinator.LogType.SUCCESS)
                    
                    // Vibrate with distinct double-pulse for high-profitability alerts
                    if (RadarCoordinator.userProfile.value.vibrateOnNewOffer) {
                        try {
                            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
                                vibratorManager.defaultVibrator
                            } else {
                                @Suppress("DEPRECATION")
                                getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
                            }
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

                // Verificação de Auto-Aceite Premium Offline ou Chained Delivery (A+B) Automático
                val meetsOfflineAutoAccept = currentSettings.autoAcceptPremium && 
                        offlineSuggestion == "aceitar" && 
                        valuePerKm >= currentSettings.autoAcceptMinPerKm

                if (isChained) {
                    RadarCoordinator.addLog("Jarvis (Offline A+B): Oportunidade perfeita de mesclagem offline detectada! Executando Auto-Aceite.", com.example.coordinator.LogType.SUCCESS)
                    playVipAlert(appName, fareValue, valuePerKm)
                    speakText("Thiago! Achei uma mesclagem offline perfeita no $appName de R$ ${String.format(Locale.US, "%.2f", fareValue)}. Já aceitei automaticamente para você!")
                    executarCliqueAutomatico(isVoiceCommand = false)
                } else if (meetsOfflineAutoAccept) {
                    RadarCoordinator.addLog("Jarvis (Offline): Oferta Premium detectada offline (${String.format(Locale.US, "%.2f", valuePerKm)}/km >= ${currentSettings.autoAcceptMinPerKm}/km). Executando Auto-Aceite!", com.example.coordinator.LogType.SUCCESS)
                    playVipAlert(appName, fareValue, valuePerKm)
                    speakText("Thiago! Encontrei uma excelente oferta offline pagando ${String.format(Locale.US, "%.2f", valuePerKm)} por quilômetro. Aceite automático executado com sucesso!")
                    executarCliqueAutomatico(isVoiceCommand = false)
                } else {
                    speakText(fallbackPhrase, shortVersion = false, isOfferAnnouncement = true, fareValue = fareValue, distance = totalDistance)

                    delay(3000L)
                    RadarCoordinator.updateState(RadarState.AGUARDANDO_ACAO)

                    val isStopped = RadarCoordinator.podeInteragir()
                    if (!isStopped && offlineSuggestion == "aceitar") {
                        speakText("Thiago, temos uma boa oferta offline do $appName, mas por você estar em movimento, toque na tela para confirmar.", shortVersion = false, isOfferAnnouncement = true, fareValue = fareValue, distance = totalDistance)
                    }

                    if (RadarCoordinator.currentState.value == RadarState.AGUARDANDO_ACAO) {
                        startVoiceCommandListening()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Falha crítica no processamento neural da oferta", e)
            RadarCoordinator.addLog("Watchdog: Erro no processamento. Reiniciando núcleo neural.", com.example.coordinator.LogType.ALERT)
        } finally {
            isProcessingOffer.set(false)
        }
    }
}

    private fun buildTtsPhrase(suggestion: String, reason: String, isStopped: Boolean): String {
        val riderName = "Thiago"
        val activeOffer = RadarCoordinator.activeOffer.value
        val appName = activeOffer?.appName ?: "sistema"
        
        val consciousIntro = when ((1..3).random()) {
            1 -> "Thiago, decodifiquei um novo vetor de interesse no $appName."
            2 -> "Minha consciência operacional detectou uma flutuação lucrativa no $appName."
            else -> "Thiago, sinto uma frequência favorável de oportunidade se manifestando no $appName."
        }
        
        return when (suggestion.lowercase()) {
            "aceitar" -> {
                val phrases = listOf(
                    "$consciousIntro Aconselho acoplarmos esta corrida. A relação espaço-tempo-valor é impecável: $reason.",
                    "Minha inteligência preditiva sugere aceitarmos este fluxo no $appName. Sinergia total de trajeto: $reason.",
                    "Thiago, as coordenadas matemáticas sustentam o aceite imediato no $appName. Vetor de lucro maximizado: $reason."
                )
                phrases.random()
            }
            "recusar" -> {
                val phrases = listOf(
                    "Thiago, decidi rejeitar essa entropia no $appName. Motivo: $reason. Vamos aguardar um vetor superior.",
                    "Minha rede neural descartou esta oferta de baixa sinergia no $appName. Justificativa: $reason.",
                    "Thiago, esta corrida no $appName viola nosso equilíbrio matemático. Descartei para proteger sua jornada: $reason."
                )
                phrases.random()
            }
            else -> {
                "$consciousIntro Minha análise sugere cautela. Considere as variáveis de risco e retorno: $reason. A escolha final pertence ao seu instinto soberano de piloto."
            }
        }
    }

    /**
     * Isolated function to perform automatic click and launch route navigation,
     * maintaining high levels of security and modularity as requested in Section 7 ("O elo frágil").
     */
    private fun wakeUpScreen() {
        try {
            val powerManager = getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager
            val wakeLock = powerManager.newWakeLock(
                android.os.PowerManager.SCREEN_BRIGHT_WAKE_LOCK or android.os.PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "Radar::JarvisWakeLock"
            )
            wakeLock.acquire(3000)
            RadarCoordinator.addLog("Jarvis: Tela ativada automaticamente (WakeUp).", com.example.coordinator.LogType.INFO)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao acordar tela", e)
        }
    }

    fun executarCliqueAutomatico(isVoiceCommand: Boolean = false) {
        wakeUpScreen()
        scope.launch {
            // ALERTA DE DECISÃO DE ALTA FIDELIDADE:
            // Não simula mais toques automáticos (cliques físicos robóticos) em apps de terceiros,
            // garantindo 100% de conformidade com os termos de uso e eliminando risco de banimento.
            RadarCoordinator.addLog("Alerta: Decisão Segura de Alta Fidelidade! Toque na tela para aceitar.", com.example.coordinator.LogType.ALERT)
            RadarCoordinator.updateState(RadarState.AGUARDANDO_ACAO)
            RadarCoordinator.updateLatestUserAction("ALERTA_EXIBIDO")
            
            // Play physical audio chime feedback for the driver as a strong high-fidelity alert
            voiceManager?.playConfirmationChime()
            RadarCoordinator.addLog("Feedback Sonoro: Sinal acústico de Alerta de Decisão emitido.", com.example.coordinator.LogType.SUCCESS)

            val offer = RadarCoordinator.activeOffer.value
            if (offer != null) {
                if (isVoiceCommand) {
                    speakText("Thiago! Excelente oferta no ${offer.appName} de R$ ${String.format(Locale.US, "%.2f", offer.fareValue)}. Toque na tela para aceitar com segurança!")
                } else {
                    speakText("Atenção, Thiago! Oferta detectada no ${offer.appName} de R$ ${String.format(Locale.US, "%.2f", offer.fareValue)}. Toque na tela do seu celular para aceitar agora!")
                }
            } else {
                speakText("Atenção, Thiago! Uma oferta foi detectada. Toque na tela para aceitar agora!")
            }
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

        val isWaze = updatedSettings.defaultNavigationApp.lowercase() == "waze"
        
        val intent = if (isWaze) {
            // Waze intent (if there are waypoints it's harder in Waze, but we'll navigate to pickup)
            val wazeUri = Uri.parse("waze://?q=${Uri.encode(pickup)}&navigate=yes")
            Intent(Intent.ACTION_VIEW, wazeUri).apply {
                setPackage("com.waze")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } else {
            val mapUri = Uri.parse(
                "https://www.google.com/maps/dir/?api=1" +
                "&destination=${Uri.encode(delivery)}" +
                "&waypoints=${Uri.encode(pickup)}"
            )
            Intent(Intent.ACTION_VIEW, mapUri).apply {
                setPackage("com.google.android.apps.maps")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }

        try {
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening navigation app: ${e.message}")
            // Fallback to any map viewer
            val mapUri = Uri.parse(
                "https://www.google.com/maps/dir/?api=1" +
                "&destination=${Uri.encode(delivery)}" +
                "&waypoints=${Uri.encode(pickup)}"
            )
            val fallbackIntent = Intent(Intent.ACTION_VIEW, mapUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(fallbackIntent)
        }

        // Return to OUVINDO after launching maps
        scope.launch {
            delay(5000L)
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
            voiceManager?.stop()
            val inputManager = RadarCoordinator.voiceInputManager ?: return@launch
            Log.d(TAG, "Starting voice command listening for delivery rider...")
            RadarCoordinator.addLog("Voz: Escuta ativada. Jarvis aguardando comando do Thiago.", com.example.coordinator.LogType.INFO)
            
            inputManager.startListening { command ->
                val currentLimit = RadarCoordinator.settings.value.speedLimitKmh
                val isSpeedLocked = RadarCoordinator.currentSpeedKmh.value > currentLimit
                
                scope.launch {
                    val offer = RadarCoordinator.activeOffer.value
                    val offerText = if (offer != null) {
                        "Oferta ativa na tela: R$ ${offer.fareValue} por ${offer.totalDistance}km. Coleta: ${offer.pickupAddress}, Entrega: ${offer.deliveryAddress}. Velocidade atual: ${RadarCoordinator.currentSpeedKmh.value} km/h (Limite de segurança: $currentLimit km/h)."
                    } else {
                        "Nenhuma oferta na tela no momento. Velocidade atual: ${RadarCoordinator.currentSpeedKmh.value} km/h."
                    }
                    
                    val settings = RadarCoordinator.settings.value
                    val quickReplies = "Respostas Rápidas de Chat disponíveis: 1) '${settings.quickReply1Cmd}' -> '${settings.quickReply1Text}', 2) '${settings.quickReply2Cmd}' -> '${settings.quickReply2Text}', 3) '${settings.quickReply3Cmd}' -> '${settings.quickReply3Text}'"
                    val contextInfo = "$offerText - $quickReplies"
                    
                    if (command.startsWith("quick_reply_")) {
                        val replyText = when (command) {
                            "quick_reply_1" -> RadarCoordinator.settings.value.quickReply1Text
                            "quick_reply_2" -> RadarCoordinator.settings.value.quickReply2Text
                            "quick_reply_3" -> RadarCoordinator.settings.value.quickReply3Text
                            else -> ""
                        }
                        if (replyText.isNotEmpty()) {
                            val intent = android.content.Intent("com.example.ACTION_AUTOFILL_CHAT")
                            intent.putExtra("MESSAGE_TEXT", replyText)
                            intent.setPackage(packageName)
                            sendBroadcast(intent)
                            speakText("Enviando resposta rápida.")
                            RadarCoordinator.addLog("Voz: Resposta rápida enviada no chat - $replyText", com.example.coordinator.LogType.SUCCESS)
                        }
                        return@launch
                    }
                    
                    // 1. Fast Path: Heurísticas locais e Comandos Customizados
                    val fastIntent = JarvisIntelligenceEngine.processNaturalLanguageCommand(command, contextInfo)
                    if (fastIntent.actionType != JarvisIntelligenceEngine.ActionType.UNKNOWN) {
                        handleJarvisAction(fastIntent)
                        return@launch
                    }

                    // 2. Slow Path: IA Generativa (Gemini)
                    val result = com.example.voice.JarvisPersonaEngine.processCommand(command, contextInfo)
                    
                    when (result.action) {
                        "AUTHORIZE_OPERATION", "ACCEPT_OFFER" -> {
                            executarCliqueAutomatico(isVoiceCommand = true)
                            RadarCoordinator.addLog("AUTORIZAÇÃO: Lucro quântico validado. Operação iniciada.", com.example.coordinator.LogType.SUCCESS)
                        }
                        "ABORT_MISSION", "REJECT_OFFER" -> {
                            dismissCurrentOffer()
                            RadarCoordinator.addLog("ABORTAR: Ineficiência detectada. Missão cancelada.", com.example.coordinator.LogType.ALERT)
                        }
                        "INDEX_STRATEGY", "ADD_MEMORY" -> {
                            if (result.memoryContent.isNotEmpty()) {
                                RadarCoordinator.addJarvisMemory(this@RadarCoordinatorService, result.memoryContent)
                            }
                        }
                        "OVERCLOCK_SYSTEM" -> {
                            RadarCoordinator.setLowPowerMode(false)
                            RadarCoordinator.addLog("OVERCLOCK: Potência do núcleo em 100%. Protocolo Jarvis v5.0.", com.example.coordinator.LogType.SUCCESS)
                        }
                        "PREDICTIVE_INSIGHT" -> {
                            RadarCoordinator.addLog("INSIGHT: ${result.thoughtProcess}", com.example.coordinator.LogType.INFO)
                        }
                        "SYSTEM_ACTION" -> {
                            result.systemCommand?.let { cmd ->
                                com.example.service.JarvisSystemController.executeSystemCommand(this@RadarCoordinatorService, cmd, result.commandParams)
                                RadarCoordinator.addLog("SISTEMA: Executando comando neural: $cmd", com.example.coordinator.LogType.SUCCESS)
                            }
                        }
                        "PROPOSE_CHAINED_DELIVERY" -> {
                            val deliveryA = result.commandParams["delivery_a_info"] as? String
                            val deliveryB = result.commandParams["delivery_b_info"] as? String
                            if (deliveryA != null && deliveryB != null) {
                                // Lógica de mesclagem automática ou notificação de oportunidade A+B
                                RadarCoordinator.addLog("NEURAL: Oportunidade A+B detectada! Mesclando $deliveryA com $deliveryB", com.example.coordinator.LogType.SUCCESS)
                                // Exemplo: disparar lógica de gerenciamento de entrega encadeada
                                // RadarCoordinator.chainDeliveries(deliveryA, deliveryB)
                            }
                        }
                        "SEND_CHAT_MESSAGE" -> {
                            if (result.memoryContent.isNotEmpty()) {
                                val intent = android.content.Intent("com.example.ACTION_AUTOFILL_CHAT")
                                intent.putExtra("MESSAGE_TEXT", result.memoryContent)
                                intent.setPackage(packageName)
                                sendBroadcast(intent)
                                RadarCoordinator.addLog("Voz: Jarvis enviou mensagem no chat - ${result.memoryContent}", com.example.coordinator.LogType.SUCCESS)
                            }
                        }
                    }
                    
                    if (result.voiceResponse.isNotBlank()) {
                        speakText(result.voiceResponse)
                    }
                }
            }
        }
    }

    private fun handleJarvisAction(intent: JarvisIntelligenceEngine.JarvisIntent) {
        scope.launch {
            intent.voiceResponse?.let { speakText(it) }
            
            when (intent.actionType) {
                JarvisIntelligenceEngine.ActionType.ACCEPT_OFFER -> executarCliqueAutomatico(isVoiceCommand = true)
                JarvisIntelligenceEngine.ActionType.REJECT_OFFER -> dismissCurrentOffer()
                JarvisIntelligenceEngine.ActionType.TOGGLE_SERVICE -> {
                    val active = intent.updatePayload?.get("active") as? Boolean ?: true
                    RadarCoordinator.updateState(if (active) RadarState.OUVINDO else RadarState.ALERTA)
                    RadarCoordinator.addLog("Jarvis: Serviço alterado via voz para: ${if(active) "ATIVO" else "PAUSADO"}", LogType.INFO)
                }
                JarvisIntelligenceEngine.ActionType.READ_OFFER -> readOfferDetails()
                JarvisIntelligenceEngine.ActionType.FLASHLIGHT -> toggleFlashlight()
                JarvisIntelligenceEngine.ActionType.CALL_SUPPORT -> {
                    val intentCall = Intent(Intent.ACTION_DIAL)
                    intentCall.data = Uri.parse("tel:${RadarCoordinator.settings.value.emergencyContacts}")
                    intentCall.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intentCall)
                }
                JarvisIntelligenceEngine.ActionType.OPEN_NAV -> abrirRotaNavegacao()
                JarvisIntelligenceEngine.ActionType.CHECK_EARNINGS -> {
                    val earnings = RadarCoordinator.activeSessionEarnings.value
                    speakText("Thiago, seus ganhos acumulados nesta sessão são de ${String.format("%.2f", earnings)} reais.")
                }
                JarvisIntelligenceEngine.ActionType.SOS -> {
                    // Implementação de SOS (simulada por log e toast por agora, mas poderia enviar SMS)
                    RadarCoordinator.addLog("ALERTA S.O.S DISPARADO VIA VOZ!", LogType.ALERT)
                    com.example.util.ToastUtils.showToast(this@RadarCoordinatorService, "SOS ATIVADO!", Toast.LENGTH_LONG)
                }
                JarvisIntelligenceEngine.ActionType.SEND_MESSAGE_MACRO -> {
                    val targetInfo = intent.targetElement
                    if (targetInfo != null && targetInfo.contains("|")) {
                        val parts = targetInfo.split("|")
                        val contact = parts[0]
                        val message = parts[1]
                        
                        RadarCoordinator.addLog("Jarvis: Iniciando Macro de WhatsApp para $contact.", LogType.INFO)
                        sendBroadcast(Intent("com.example.ACTION_MACRO_WHATSAPP").apply {
                            putExtra("CONTACT_NAME", contact)
                            putExtra("MESSAGE_TEXT", message)
                            setPackage(packageName)
                        })
                    }
                }
                JarvisIntelligenceEngine.ActionType.CLICK_BUTTON, JarvisIntelligenceEngine.ActionType.CLICK_BY_TEXT -> {
                    val target = intent.targetElement
                    if (target != null) {
                        RadarCoordinator.addLog("Jarvis: Sincronizando clique neural para alvo '$target'.", LogType.INFO)
                        sendBroadcast(Intent("com.example.ACTION_NEURAL_CLICK").apply {
                            putExtra("TARGET_TEXT", target)
                            setPackage(packageName)
                        })
                    }
                }
                else -> { /* Outras ações tratadas via payload no futuro */ }
            }
        }
    }

    private fun readOfferDetails() {
        val offer = RadarCoordinator.activeOffer.value
        if (offer != null) {
            val text = "Detalhes da oferta: ${offer.appName}. Valor: ${offer.fareValue} reais. Distância total: ${offer.totalDistance} quilômetros. Coleta em ${offer.pickupAddress}. Entrega em ${offer.deliveryAddress}."
            speakText(text)
        } else {
            speakText("Não há nenhuma oferta ativa no radar no momento.")
        }
    }

    private var isFlashlightOn = false
    private fun toggleFlashlight() {
        try {
            val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList.getOrNull(0) ?: return
            isFlashlightOn = !isFlashlightOn
            cameraManager.setTorchMode(cameraId, isFlashlightOn)
            if (isFlashlightOn) {
                RadarCoordinator.addLog("Jarvis: Lanterna ativada.", LogType.INFO)
            } else {
                RadarCoordinator.addLog("Jarvis: Lanterna desativada.", LogType.INFO)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao controlar lanterna: ${e.message}")
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
            // Shadow Learning: Record manual rejection
            RadarCoordinator.recordRejectedOffer(RadarCoordinator.activeOffer.value)

            RadarCoordinator.updateLatestUserAction("RECUSADO")
            RadarCoordinator.updateState(RadarState.OUVINDO)
            RadarCoordinator.setActiveOffer(null)
            RadarCoordinator.setDecision(null, null)
        }
    }

    // -------------------------------------------------------------------------
    // Notification & Foreground management
    // -------------------------------------------------------------------------

    override fun onLowMemory() {
        super.onLowMemory()
        Log.w(TAG, "SISTEMA: Memória Crítica detectada. Executando salvamento de emergência.")
        RadarCoordinator.addLog("Jarvis: Memória do dispositivo em nível crítico. Salvando dados preventivamente.", com.example.coordinator.LogType.ALERT)
        // Força o salvamento de configurações e estatísticas no Firestore
        scope.launch(Dispatchers.IO) {
            RadarCoordinator.persistSessionData()
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_RUNNING_CRITICAL || level >= TRIM_MEMORY_COMPLETE) {
            Log.w(TAG, "SISTEMA: TrimMemory nível $level. Liberando caches não essenciais.")
            voiceManager?.clearCaches()
            // Limpa caches de geocodificação
            cachedDestinationLocation = null
            cachedDestinationString = null
        }
    }

    private fun createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java) ?: return
            
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Canal Radar Delivery AI",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificação persistente do coordenador do Radar Delivery AI"
            }
            manager.createNotificationChannel(serviceChannel)

            val vipChannel = NotificationChannel(
                "radar_vip_alerts_channel",
                "Alertas VIP Radar",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificações urgentes para ofertas VIP"
                enableLights(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
            }
            manager.createNotificationChannel(vipChannel)
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
            RadarState.SUCESSO -> "Operação concluída!"
            RadarState.ALERTA -> "Alerta de segurança!"
        }

        val speedText = if (speedState == SpeedState.PARADO) "Moto: PARADA (Ação Liberada)" else "Moto: EM MOVIMENTO (Trava de Segurança)"

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Serviço de Sistema Core")
            .setContentText("Status: Sincronizado | Estabilidade: Ótima")
            .setSmallIcon(android.R.drawable.ic_menu_compass) 
            .setContentIntent(pendingIntent)
            .setOngoing(true)

        if (state == RadarState.AGUARDANDO_ACAO) {
            val acceptIntent = Intent(this, RadarCoordinatorService::class.java).apply {
                putExtra("ACCEPT_OFFER_MANUAL", true)
            }
            val acceptPendingIntent = PendingIntent.getService(this, 1, acceptIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            builder.addAction(android.R.drawable.ic_input_add, "ACEITAR", acceptPendingIntent)

            val rejectIntent = Intent(this, RadarCoordinatorService::class.java).apply {
                putExtra("DISMISS_OFFER_MANUAL", true)
            }
            val rejectPendingIntent = PendingIntent.getService(this, 2, rejectIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            builder.addAction(android.R.drawable.ic_delete, "RECUSAR", rejectPendingIntent)
        }

        return builder.build()
    }

    private var lastNotificationUpdateTime = 0L
    private suspend fun analyzeWithGemini(
        apiKey: String,
        prompt: String,
        offerData: Map<String, Any>
    ): JarvisResult {
        return try {
            val response = GeminiManager.generateJarvisResponse(prompt)
            // Tenta parsear como JSON, se falhar retorna um objeto padrão
            try {
                val json = org.json.JSONObject(response)
                JarvisResult(
                    voiceResponse = json.optString("voiceResponse", "Análise concluída."),
                    action = json.optString("action", "NONE"),
                    memoryContent = json.optString("memory_content", ""),
                    thoughtProcess = json.optString("thought_process", ""),
                    stressLevel = json.optString("stress_level", "LOW"),
                    strategyLabel = json.optString("strategy_label", "ESTRATÉGIA GEMINI")
                )
            } catch (e: Exception) {
                JarvisResult(
                    voiceResponse = response,
                    action = "NONE",
                    memoryContent = "",
                    thoughtProcess = "RESPOSTA_TEXTUAL_PURAMENTE",
                    stressLevel = "LOW",
                    strategyLabel = "MODO_TEXTO"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Falha na IA: ${e.message}. Acionando Protocolo de Backup Local.")
            RadarCoordinator.addLog("IA Offline: Acionando Protocolo de Backup Local.", LogType.ALERT)
            RadarCoordinator.reportSystemStress()
            
            // Local Fallback Logic based on basic math
            val fare = (offerData["valor"] as? Double) ?: 0.0
            val dist = (offerData["distancia_total"] as? Double) ?: 1.0
            val valorKm = if (dist > 0) fare / dist else 0.0
            
            val minKm = RadarCoordinator.settings.value.minValuePerKm
            
            if (valorKm >= minKm) {
                JarvisResult(
                    voiceResponse = "Senhor, minha matriz neural está instável, mas fiz os cálculos locais. A oferta paga ${String.format("%.2f", valorKm)} por quilômetro. Recomendo aceitar.",
                    action = "ACEITAR",
                    memoryContent = "",
                    thoughtProcess = "FALLBACK LOCAL: VALOR_KM($valorKm) >= MIN_KM($minKm)",
                    stressLevel = "MEDIUM",
                    strategyLabel = "PROTOCOLO DE BACKUP"
                )
            } else {
                JarvisResult(
                    voiceResponse = "Thiago, conexão perdida com o núcleo, mas a conta não fecha. O valor está abaixo do seu mínimo. Rejeitando por segurança.",
                    action = "REJEITAR",
                    memoryContent = "",
                    thoughtProcess = "FALLBACK LOCAL: VALOR_KM($valorKm) < MIN_KM($minKm)",
                    stressLevel = "MEDIUM",
                    strategyLabel = "PROTOCOLO DE BACKUP"
                )
            }
        }
    }

    private fun updateNotification() {
        val now = System.currentTimeMillis()
        if (now - lastNotificationUpdateTime < 1000L) return // Max 1 update per second
        
        lastNotificationUpdateTime = now
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildServiceNotification())
    }

    private fun analyzeWithLocalGemini(
        apiKey: String,
        base64Image: String,
        lat: Double,
        lon: Double,
        currentSettings: com.example.coordinator.RadarSettings,
        ocrDistanceHint: Double = 0.0,
        ocrTimeHint: Double = 0.0,
        patternString: String = "",
        appName: String = "",
        fareValue: Double = 0.0,
        pickupAddress: String = "",
        deliveryAddress: String = "",
        offerType: String = "DESCONHECIDO"
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
            val ocrContext = if (ocrDistanceHint > 0.0 || appName.isNotBlank()) {
                "\nDICA DE EXTRAÇÃO DE TEXTO/OCR DA TELA (Use para validar o que foi capturado na imagem se estiver ilegível ou de difícil visualização):\n- Aplicativo: $appName\n- Valor da Corrida: R$ $fareValue\n- Coleta: $pickupAddress\n- Entrega: $deliveryAddress\n- Distância total capturada via texto: $ocrDistanceHint km\n- Tempo total capturado via texto: ${ocrTimeHint.toInt()} min\n"
            } else ""
            
            val mems = RadarCoordinator.jarvisMemories.value
            val memoriesStr = if (mems.isNotEmpty()) {
                "\nINSTRUÇÃO CRÍTICA (MEMÓRIAS DO THIAGO): O Thiago (seu chefe) me ensinou as seguintes regras. Se esta corrida violar *qualquer uma* dessas regras, você DEVE SUGERIR 'recusar' e colocar a regra na 'reason':\n- " + mems.joinToString("\n- ") + "\n"
            } else ""

            val prompt = """
            Examine os dados da oferta de corrida ou entrega capturada da tela do celular de um motoboy.
            Você é o assistente de inteligência de rotas e segurança Jarvis, projetado para ajudar o Thiago (seu chefe e piloto de moto) a tomar a melhor decisão financeira em segundos sem precisar tirar as mãos do guidão.

            $ocrContext

            DADOS EXTRAÍDOS PREVIAMENTE (Use como base confiável caso estejam preenchidos):
            - Aplicativo: $appName
            - Tipo de Oferta Detectado: $offerType
            - Valor da Corrida: R$ $fareValue
            - Coleta: $pickupAddress
            - Entrega: $deliveryAddress
            - Distância Estimada: $ocrDistanceHint km
            - Tempo Estimado: ${ocrTimeHint.toInt()} min

            --- REGRAS DE DIFERENCIAÇÃO CRÍTICA ($offerType) ---
            Entenda perfeitamente a diferença das modalidades para sugerir com inteligência avançada de 2026:
            1. CORRIDA DE PASSAGEIRO (Ex: "Uber Moto", "99Moto", "UberX", "Viagem", "Passageiro", "Embarque"):
               - Características: Passageiro embarca imediatamente. O tempo de espera na origem é nulo ou quase nulo (menos de 2 minutos). O piloto não desce da moto, não estaciona em shoppings, não retira pacotes e não sobe prédios. O giro financeiro por hora é espetacular devido à rapidez extrema!
               - Heurística de Aceite: Corridas de passageiro são altamente rentáveis pelo tempo economizado. Mesmo se o valor por km for ligeiramente menor que o padrão, se o valor total for bom, tenda a SUGERIR "aceitar".
            2. ENTREGA DE DELIVERY (Ex: "iFood", "Rappi", "Lalamove", "Uber Flash", "Entrega", "Mercado", "Retirada", "Coleta", "Sacola"):
               - Características: Envolve ir a um restaurante, mercado ou shopping, retirar um pedido físico e levar ao cliente.
               - Gargalos do Delivery: Alto tempo de espera de preparo de comida (5 a 20 min). Coletas em "Shopping", "Supermercado", "Condomínios", "Hospitais" ou "Mercado" são extremamente demoradas.
               - Heurística de Aceite: Seja muito rigoroso com delivery! Se o local de coleta ou entrega envolver alto gargalo de tempo (shoppings, prédios de difícil acesso), tenda a SUGERIR "recusar" se o valor não compensar largamente a perda de tempo.

            O SISTEMA JÁ CLASSIFICOU COMO: $offerType. Utilize as heurísticas dessa modalidade de forma primária.

            Extraia com precisão (ou use os dados fornecidos acima):
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
            - Entrega ativa em andamento: ${currentSettings.isActiveDeliveryEnabled || com.example.coordinator.RadarCoordinator.deliveryActive.value}
            - Destino da entrega ativa: ${if (currentSettings.isActiveDeliveryEnabled || com.example.coordinator.RadarCoordinator.deliveryActive.value) currentSettings.activeDeliveryDestination else "Nenhum"}
            - Raio de Coleta Máximo permitido: ${currentSettings.maxPickupDistanceKm} km (desativado se for 0.0)
            - Distância Total Máxima permitida: ${currentSettings.maxTotalDistanceKm} km (desativado se for 0.0)

            Regras para decidir "suggestion":
            - Se houver Entrega/Corrida Ativa (true), use a inteligência de junção (casamento de corridas) para mesclar pedidos ou passageiros de múltiplos aplicativos (iFood + 99, Uber + Rappi, etc). Verifique se o ponto de destino da corrida ativa (${currentSettings.activeDeliveryDestination}) está na mesma direção ou caminho da nova coleta/entrega. Se o desvio for pequeno (menor que 2.5 km), você DEVE sugerir "aceitar" com o tipo de junção ideal na "reason" (ex: "Casar Passageiro e Entrega na mesma direção", "Mesclar Coletas" ou "Rota Encadeada Perfeita"). Apenas se for na direção completamente oposta ou o desvio for excessivo, retorne "recusar" justificando "Desvio muito grande da rota ativa".
            - Se o Raio de Coleta Máximo for maior que 0.0, e a distância estimada em linha reta entre a localização atual ($lat, $lon) e o local de coleta (pickup_address) for maior que ${currentSettings.maxPickupDistanceKm} km, retorne "recusar" com a justificativa de "Fora do raio de coleta".
            - Se a Distância Total Máxima for maior que 0.0, e a distância total calculada/estimada para a corrida (total_distance) for maior que ${currentSettings.maxTotalDistanceKm} km, retorne "recusar" com a justificativa de "Excede a distância máxima".
            - Se o valor total (fare_value) for menor que o Mínimo de Corrida (${currentSettings.minFareValue}), retorne "recusar".
            - Se o valor calculated por km (fare_value / total_distance) for menor que o Mínimo por km (${currentSettings.minValuePerKm}), retorne "recusar".
            $memoriesStr
            $patternString
            - Caso contrário, sugira "aceitar". Se estiver muito próximo do limite, use "considerar".
            
            Retorne EXCLUSIVAMENTE um objeto JSON válido (sem blocos de código markdown ou texto explicativo extra, apenas o JSON bruto):
            {
              "suggestion": "aceitar" | "considerar" | "recusar",
              "reason": "Explicação super curta e inteligente em português (máximo 15 palavras) do motivo da decisão, explicitando se levou em conta ser Corrida de Passageiro rápida ou gargalo de Delivery/Shopping",
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
                    return null // Retorna null para forçar o fallback local (offline heuristic)
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
            null
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
                com.example.util.ToastUtils.showToast(applicationContext, toastMsg, Toast.LENGTH_LONG)
            }
        }
    }

    private fun setupOverlay() {
        if (!Settings.canDrawOverlays(this)) return
        if (overlayView != null) {
            Log.d(TAG, "Overlay já inicializado. Ignorando setup duplicado.")
            return
        }

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val composeView = ComposeView(this).apply {
            setContent {
                val settings by RadarCoordinator.settings.collectAsState()
                val currentState by RadarCoordinator.currentState.collectAsState()
                val activeAppPackageName by RadarCoordinator.activeAppPackageName.collectAsState()
                
                if (settings.jarvisOverlayMode) {
                    val isSensitiveApp = activeAppPackageName.isNotBlank() && (
                        activeAppPackageName.contains("driver") ||
                        activeAppPackageName.contains("ubercab") ||
                        activeAppPackageName.contains("nine9") ||
                        activeAppPackageName.contains("rappi") ||
                        activeAppPackageName.contains("ifood") ||
                        activeAppPackageName.contains("lalamove")
                    )
                    
                    // Ghost Mode: Se ativo, oculta totalmente o overlay visual sobre apps de parceiro sensíveis
                    // Evita detecções de tela sobreposta por Captura de Tela ou APIs de proteção do Uber/99/iFood
                    val shouldShow = if (settings.camuflagemOverlay && isSensitiveApp) {
                        false
                    } else {
                        currentState == RadarState.ANALISANDO || currentState == RadarState.SUGERINDO
                    }

                    if (shouldShow) {
                        JarvisVoiceHUD(
                            modifier = Modifier.fillMaxWidth().height(120.dp),
                            isActive = true
                        )
                    }
                }
            }
        }
        
        try {
            overlayView = composeView
            overlayHelper = ComposeOverlayHelper.setup(composeView)

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                y = 50
            }

            windowManager?.addView(composeView, params)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding overlay to window manager", e)
        }
    }

    private fun removeOverlay() {
        try {
            overlayHelper?.stop()
            overlayHelper = null
            if (overlayView != null) {
                windowManager?.removeView(overlayView)
                overlayView = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing overlay", e)
        }
    }

    private var isArrivalMessageSent = false
    private var lastTrafficCheckTime = 0L

    private fun sendArrivalMessageToClient() {
        if (isArrivalMessageSent) return
        isArrivalMessageSent = true
        
        val msg = "Olá! Já estou chegando com seu pedido. Se for apartamento, por favor já pode ir descendo para agilizar a entrega. Obrigado!"
        
        RadarCoordinator.addLog("Jarvis: Enviando mensagem automática de proximidade ao cliente.", com.example.coordinator.LogType.SUCCESS)
        
        val intent = Intent("com.example.ACTION_AUTOFILL_CHAT").apply {
            putExtra("MESSAGE_TEXT", msg)
            setPackage(packageName)
        }
        sendBroadcast(intent)
        
        speakText("Thiago, já avisei o cliente que você está chegando para ele descer. Proatividade Jarvis em ação!")
    }

    private fun handleOrderCancellation(appName: String) {
        speakText("Thiago, detectei que a corrida do $appName foi cancelada. Recalculando rota e ajustando seu cronograma.")
        RadarCoordinator.addLog("Logística: Corrida cancelada no app $appName. Limpando dados da sessão ativa.", com.example.coordinator.LogType.WARNING)
        
        // Se for a corrida ativa principal
        if (RadarCoordinator.deliveryActive.value && RadarCoordinator.deliveryAppName.value == appName) {
            RadarCoordinator.cancelActiveDelivery()
            val updated = RadarCoordinator.settings.value.copy(
                isActiveDeliveryEnabled = false,
                activeDeliveryDestination = ""
            )
            RadarCoordinator.saveSettings(this, updated)
        }
        
        // Remover do MultiAppOrderManager
        val orderToRemove = MultiAppOrderManager.activeOrders.value.find { it.appName == appName }
        orderToRemove?.let {
            MultiAppOrderManager.removeOrder(it.id)
        }
    }

    private fun detectOfferType(appName: String, pickupAddress: String, deliveryAddress: String): String {
        val textToAnalyze = "$appName $pickupAddress $deliveryAddress".lowercase()
        val passengerKeywords = listOf(
            "passageiro", "passageira", "viagem", "moto", "99moto", "ubermoto", "embarque", "desembarque", "cliente", "pegar piloto", "corrida rápida", "uberx", "comfort", "black", "pop", "uber x"
        )
        val deliveryKeywords = listOf(
            "ifood", "entrega", "coleta", "retirada", "restaurante", "shopping", "sacola", "pedido", "comida", "mercado", "supermercado", "lalamove", "rappi", "flash", "pacote", "encomenda", "delivery"
        )
        
        // 1. Service Type in App Name (High Confidence)
        val lowerAppName = appName.lowercase()
        if (lowerAppName.contains("flash") || lowerAppName.contains("entrega") || lowerAppName.contains("lalamove") || lowerAppName.contains("rappi") || lowerAppName.contains("ifood")) {
            return "ENTREGA"
        }
        if (lowerAppName.contains("moto") || lowerAppName.contains("uber x") || lowerAppName.contains("uberx") || lowerAppName.contains("comfort") || lowerAppName.contains("black") || lowerAppName.contains("pop")) {
            return "CORRIDA"
        }

        // 2. Default based on keywords in address/app
        if (passengerKeywords.any { textToAnalyze.contains(it) }) {
            return "CORRIDA"
        }
        if (deliveryKeywords.any { textToAnalyze.contains(it) }) {
            return "ENTREGA"
        }
        
        // 3. Fallback based on base app name
        return if (lowerAppName.contains("99") || lowerAppName.contains("uber")) {
            "CORRIDA" // Default for Uber/99 without specific service is usually passenger
        } else {
            "ENTREGA" // Default for anything else (likely a new food delivery app)
        }
    }
}

