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

/**
 * Estado da telemetria de localização e velocidade do condutor.
 */
data class LocationSpeedState(
    val currentSpeedKmh: Double = 0.0,
    val isSafetyLockActive: Boolean = false, // true quando > 10.0 km/h
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
 * 1. Rastrear em tempo real a posição e velocidade atual do usuário (m/s convertidos para km/h).
 * 2. Operar tanto como serviço em primeiro plano (Foreground Service) quanto como Singleton / Bound Service.
 * 3. Notificar o estado da trava de segurança (Safety Lock) quando a velocidade ultrapassar o limiar de 10 km/h.
 */
class LocationService : Service() {

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private var lastValidLocation: Location? = null

    companion object {
        private const val TAG = "LocationService"
        const val NOTIFICATION_CHANNEL_ID = "channel_radar_speed_safety"
        const val NOTIFICATION_ID = 4001

        const val ACTION_START_LOCATION_TRACKING = "com.example.action.START_LOCATION_TRACKING"
        const val ACTION_STOP_LOCATION_TRACKING = "com.example.action.STOP_LOCATION_TRACKING"

        // Limiar crítico de segurança em km/h
        const val SAFETY_SPEED_LOCK_THRESHOLD_KMH = 10.0

        // Fluxo de estado global acessível para a UI e Composables
        private val _globalLocationState = MutableStateFlow(LocationSpeedState())
        val globalLocationState: StateFlow<LocationSpeedState> = _globalLocationState.asStateFlow()

        /**
         * Permite simular velocidade para fins de teste no emulador / modo de desenvolvimento.
         */
        fun updateSimulatedSpeed(speedKmh: Double) {
            val isLock = speedKmh > SAFETY_SPEED_LOCK_THRESHOLD_KMH
            _globalLocationState.value = _globalLocationState.value.copy(
                currentSpeedKmh = speedKmh,
                isSafetyLockActive = isLock,
                speedSource = "Simulação Manual",
                lastUpdateTimeMillis = System.currentTimeMillis()
            )
        }
    }

    inner class LocalBinder : Binder() {
        fun getService(): LocationService = this@LocationService
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "LocationService onCreate: Inicializando FusedLocationProviderClient")
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
        setupLocationCallback()
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
        val speedKmh = calculateSpeedKmh(location)
        val isSafetyLockActive = speedKmh > SAFETY_SPEED_LOCK_THRESHOLD_KMH

        val newState = LocationSpeedState(
            currentSpeedKmh = speedKmh,
            isSafetyLockActive = isSafetyLockActive,
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

        val lockStatus = if (state.isSafetyLockActive) {
            "🚨 TRAVA ATIVA (> 10 km/h) • ${String.format(java.util.Locale.US, "%.0f", state.currentSpeedKmh)} km/h"
        } else {
            "🟢 MODO SEGURO • ${String.format(java.util.Locale.US, "%.0f", state.currentSpeedKmh)} km/h"
        }

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Radar Speed Safety Monitor")
            .setContentText(lockStatus)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
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
        serviceScope.cancel()
        Log.d(TAG, "LocationService destruído.")
    }
}
