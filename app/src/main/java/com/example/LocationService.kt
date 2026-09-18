package com.example

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.core.app.NotificationCompat
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Estado da telemetria de localização e velocidade do condutor.
 */
data class LocationSpeedState(
    val currentSpeedKmh: Double = 0.0,
    val isSafetyLockActive: Boolean = false, // true quando > 15.0 km/h (bloqueio tátil da UI)
    val isNotificationsMuted: Boolean = false, // true quando > 15.0 km/h (silenciamento de notificações Heads-Up)
    val latitude: Double = -23.561684,
    val longitude: Double = -46.655981,
    val accuracyMeters: Float = 0f,
    val altitudeMeters: Double = 0.0,
    val bearingDegrees: Float = 0f,
    val isTracking: Boolean = false,
    val speedSource: String = "GPS Fused",
    val lastUpdateTimeMillis: Long = System.currentTimeMillis()
)

/**
 * [LocationService] utilizando [FusedLocationProviderClient] da Google Play Services.
 *
 * Responsável por:
 * 1. Rastrear em tempo real e em segundo plano a posição e velocidade do entregador.
 * 2. Operar como Foreground Service com notificação contínua e wake lock parcial de proteção.
 * 3. Ativar automaticamente a trava de segurança quando a velocidade ultrapassar o limiar configurado.
 * 4. Emitir avisos sonoros (TTS em português) e vibrações hápticas mesmo com o app minimizado no Waze ou com a tela apagada.
 */
class LocationService : Service(), TextToSpeech.OnInitListener {

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private var lastValidLocation: Location? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var vibrator: Vibrator? = null
    private var tts: TextToSpeech? = null
    private var isTtsReady: Boolean = false

    companion object {
        private const val TAG = "LocationService"
        const val NOTIFICATION_CHANNEL_ID = "channel_radar_speed_safety"
        const val NOTIFICATION_ID = 4001

        const val ACTION_START_LOCATION_TRACKING = "com.example.action.START_LOCATION_TRACKING"
        const val ACTION_STOP_LOCATION_TRACKING = "com.example.action.STOP_LOCATION_TRACKING"

        // Limiar crítico de segurança em km/h padrão (10 km/h: desativa automaticamente o aceite de ofertas)
        const val SAFETY_SPEED_LOCK_THRESHOLD_KMH = 10.0

        // Limiar configurado dinamicamente pelo entregador
        @Volatile
        var dynamicSafetySpeedThresholdKmh: Double = SAFETY_SPEED_LOCK_THRESHOLD_KMH

        // Fluxo de estado global acessível para a UI e Composables
        private val _globalLocationState = MutableStateFlow(LocationSpeedState())
        val globalLocationState: StateFlow<LocationSpeedState> = _globalLocationState.asStateFlow()

        /**
         * Inicia o serviço de localização em segundo plano (Foreground Service).
         */
        fun start(context: Context) {
            val intent = Intent(context, LocationService::class.java).apply {
                action = ACTION_START_LOCATION_TRACKING
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Falha ao iniciar LocationService em foreground", e)
            }
        }

        /**
         * Interrompe o serviço de localização em segundo plano.
         */
        fun stop(context: Context) {
            val intent = Intent(context, LocationService::class.java).apply {
                action = ACTION_STOP_LOCATION_TRACKING
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Falha ao parar LocationService", e)
            }
        }

        /**
         * Atualiza o limiar de velocidade para a trava de segurança e silenciamento de notificações.
         */
        fun updateSafetySpeedThreshold(thresholdKmh: Double) {
            dynamicSafetySpeedThresholdKmh = thresholdKmh
            val currentSpeed = _globalLocationState.value.currentSpeedKmh
            val isLock = currentSpeed > thresholdKmh
            LocalNotificationManager.isSpeedMuteActive = isLock
            if (_globalLocationState.value.isSafetyLockActive != isLock) {
                _globalLocationState.value = _globalLocationState.value.copy(
                    isSafetyLockActive = isLock,
                    isNotificationsMuted = isLock
                )
            }
        }

        /**
         * Permite simular velocidade para fins de teste no emulador / modo de desenvolvimento.
         */
        fun updateSimulatedSpeed(speedKmh: Double, context: Context? = null) {
            val isLock = speedKmh > dynamicSafetySpeedThresholdKmh
            LocalNotificationManager.isSpeedMuteActive = isLock
            if (isLock && context != null) {
                LocalNotificationManager.cancelAllActiveOfferNotifications(context)
            }
            _globalLocationState.value = _globalLocationState.value.copy(
                currentSpeedKmh = speedKmh,
                isSafetyLockActive = isLock,
                isNotificationsMuted = isLock,
                speedSource = "Simulação Manual",
                lastUpdateTimeMillis = System.currentTimeMillis()
            )
        }

        /**
         * Calcula a distância geodésica em linha reta (em metros) entre dois pontos de coordenadas (WGS84).
         */
        fun calculateDistanceMeters(
            startLat: Double,
            startLng: Double,
            endLat: Double,
            endLng: Double
        ): Float {
            val results = FloatArray(1)
            Location.distanceBetween(startLat, startLng, endLat, endLng, results)
            return results[0]
        }

        /**
         * Calcula a distância geodésica convertida em quilômetros (com arredondamento a 2 casas decimais).
         */
        fun calculateDistanceKm(
            startLat: Double,
            startLng: Double,
            endLat: Double,
            endLng: Double
        ): Double {
            val meters = calculateDistanceMeters(startLat, startLng, endLat, endLng)
            return Math.round((meters / 1000.0) * 100.0) / 100.0
        }

        /**
         * Estima a distância real de condução veicular urbana (São Paulo / Corredores de Moto),
         * aplicando o fator Manhattan/Circuidade de malha viária (fator 1.25x a 1.35x sobre a geodésica).
         */
        fun estimateUrbanRouteKm(
            startLat: Double,
            startLng: Double,
            endLat: Double,
            endLng: Double,
            circuityFactor: Double = 1.28
        ): Double {
            val directKm = calculateDistanceKm(startLat, startLng, endLat, endLng)
            val roadDistance = directKm * circuityFactor
            return Math.round(roadDistance * 10.0) / 10.0
        }

        /**
         * Calcula a distância direta da posição GPS atual do entregador até o ponto de coleta.
         */
        fun calculateDistanceToPickupFromCurrent(
            pickupLat: Double,
            pickupLng: Double
        ): Double {
            val current = _globalLocationState.value
            return estimateUrbanRouteKm(current.latitude, current.longitude, pickupLat, pickupLng)
        }
    }

    inner class LocalBinder : Binder() {
        fun getService(): LocationService = this@LocationService
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "LocationService onCreate: Inicializando FusedLocationProviderClient")
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // WakeLock para garantir execução de telemetria contínua com tela desligada
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "RadarCoordinator:LocationWakeLock")
            wakeLock?.acquire(24 * 60 * 60 * 1000L) // 24 horas max
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao adquirir WakeLock", e)
        }

        // Inicialização do Vibrator para feedback tático de segurança
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

        // Inicialização do TextToSpeech para avisos sonoros no capacete mesmo em segundo plano
        try {
            tts = TextToSpeech(applicationContext, this)
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao inicializar TTS no LocationService", e)
        }

        createNotificationChannel()
        setupLocationCallback()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("pt", "BR"))
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                isTtsReady = true
                tts?.setSpeechRate(1.05f)
            }
        }
    }

    private fun speakAlert(text: String) {
        if (isTtsReady && tts != null) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "loc_safety_alert_${System.currentTimeMillis()}")
        }
    }

    private fun triggerVibrationAlert(isLock: Boolean) {
        try {
            if (isLock) {
                // Alerta tático de bloqueio em movimento: 2 pulsos fortes
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 150, 100, 250), -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(longArrayOf(0, 150, 100, 250), -1)
                }
            } else {
                // Desbloqueio e segurança: 1 pulso curto
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(80)
                }
            }
        } catch (_: Exception) {}
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_LOCATION_TRACKING -> {
                startForeground(NOTIFICATION_ID, buildForegroundNotification(_globalLocationState.value))
                startLocationUpdates()
            }
            ACTION_STOP_LOCATION_TRACKING -> {
                stopLocationUpdates()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            else -> {
                startForeground(NOTIFICATION_ID, buildForegroundNotification(_globalLocationState.value))
                startLocationUpdates()
            }
        }
        return START_STICKY
    }

    /**
     * Configura o listener de retorno de posições do Fused Location.
     */
    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation ?: return
                processNewLocation(location)
            }
        }
    }

    /**
     * Processa a nova localização e calcula a velocidade precisa em km/h.
     */
    private fun processNewLocation(location: Location) {
        val previousLock = _globalLocationState.value.isSafetyLockActive
        val speedKmh = calculateSpeedKmh(location)
        val isSafetyLockActive = speedKmh > dynamicSafetySpeedThresholdKmh
        val isNotificationsMuted = isSafetyLockActive

        val newState = LocationSpeedState(
            currentSpeedKmh = speedKmh,
            isSafetyLockActive = isSafetyLockActive,
            isNotificationsMuted = isNotificationsMuted,
            latitude = location.latitude,
            longitude = location.longitude,
            accuracyMeters = location.accuracy,
            altitudeMeters = location.altitude,
            bearingDegrees = location.bearing,
            isTracking = true,
            speedSource = if (location.hasSpeed()) "GPS Fused (Hardware)" else "Calculado Delta/Tempo",
            lastUpdateTimeMillis = System.currentTimeMillis()
        )

        _globalLocationState.value = newState
        lastValidLocation = location

        // Atualiza silenciador de notificações globais e cancela flutuantes pendentes em movimento
        LocalNotificationManager.isSpeedMuteActive = isNotificationsMuted
        if (isNotificationsMuted) {
            LocalNotificationManager.cancelAllActiveOfferNotifications(applicationContext)
        }

        // Disparo de alertas sensoriais (Áudio TTS + Vibração Tática) quando a trava é ativada ou liberada
        if (previousLock != isSafetyLockActive) {
            val limit = dynamicSafetySpeedThresholdKmh.toInt()
            triggerVibrationAlert(isSafetyLockActive)
            if (isSafetyLockActive) {
                speakAlert("Atenção: veículo acima de $limit por hora detectado por GPS. Aceite de ofertas desativado por segurança.")
            } else {
                speakAlert("Velocidade abaixo de $limit por hora. Aceite de ofertas e tela liberados.")
            }
        }

        // Avalia zonas de geofence de alta demanda
        try {
            GeofencingDemandManager.getInstance(applicationContext)
                .evaluateCurrentLocation(location.latitude, location.longitude)
        } catch (_: Exception) {}

        // Atualiza a notificação foreground quando houver alteração significativa
        updateNotification(newState)
    }

    /**
     * Extrai a velocidade do hardware de localização ou calcula delta distância / tempo.
     */
    private fun calculateSpeedKmh(currentLocation: Location): Double {
        // Se o GPS já reporta a velocidade nativa com precisão
        if (currentLocation.hasSpeed() && currentLocation.speed >= 0) {
            val speedMps = currentLocation.speed
            return (speedMps * 3.6).coerceAtLeast(0.0)
        }

        // Caso o sensor não forneça speed direto, computamos via deslocamento e delta t
        val previous = lastValidLocation ?: return 0.0
        val distanceMeters = currentLocation.distanceTo(previous)
        val timeDeltaSeconds = (currentLocation.time - previous.time) / 1000.0

        return if (timeDeltaSeconds in 0.5..10.0) {
            val speedMps = distanceMeters / timeDeltaSeconds
            (speedMps * 3.6).coerceIn(0.0, 180.0)
        } else {
            0.0
        }
    }

    /**
     * Inicia a requisição periódica de localização via FusedLocationProviderClient.
     */
    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        try {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1500L)
                .setMinUpdateIntervalMillis(1000L)
                .setMinUpdateDistanceMeters(1.0f)
                .setWaitForAccurateLocation(false)
                .build()

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )

            _globalLocationState.value = _globalLocationState.value.copy(isTracking = true)
            Log.d(TAG, "FusedLocationProvider: Atualizações de velocidade iniciadas com sucesso.")
        } catch (e: SecurityException) {
            Log.e(TAG, "Permissão de localização ausente ao iniciar FusedLocationProvider", e)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao solicitar atualizações no FusedLocationProviderClient", e)
        }
    }

    /**
     * Interrompe a requisição periódica de localização.
     */
    fun stopLocationUpdates() {
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            _globalLocationState.value = _globalLocationState.value.copy(isTracking = false)
            Log.d(TAG, "FusedLocationProvider: Atualizações de velocidade paralisadas.")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao pausar FusedLocationProviderClient", e)
        }
    }

    /**
     * Constrói a notificação persistente de telemetria em segundo plano.
     */
    private fun buildForegroundNotification(state: LocationSpeedState): Notification {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val threshold = dynamicSafetySpeedThresholdKmh.toInt()
        val speedStr = String.format(java.util.Locale.US, "%.0f", state.currentSpeedKmh)
        val lockStatus = if (state.isSafetyLockActive) {
            "🚨 TRAVA DE SEGURANÇA ATIVA (> $threshold km/h) • $speedStr km/h"
        } else {
            "🟢 VELOCIDADE SEGURA (<= $threshold km/h) • $speedStr km/h"
        }

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Radar Coordinator • Proteção Ativa")
            .setContentText(lockStatus)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun updateNotification(state: LocationSpeedState) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(NOTIFICATION_ID, buildForegroundNotification(state))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Radar Telemetria e Velocidade",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitora a velocidade do entregador para ativar a trava de segurança em condução."
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {}
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {}
        serviceScope.cancel()
        Log.d(TAG, "LocationService destruído.")
    }
}
