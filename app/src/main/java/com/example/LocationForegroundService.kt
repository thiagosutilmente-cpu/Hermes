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
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Service em Primeiro Plano para Leitura Contínua de GPS em Alta Precisão.
 * Utiliza o algoritmo de Haversine para cálculo geodésico exato de velocidade e deslocamento.
 * Transmite telemetria contínua para a WebView nativa e para a tela multimídia do Android Auto.
 */
class LocationForegroundService : Service() {

    companion object {
        private const val TAG = "LocationFgService"
        const val CHANNEL_ID = "radar_location_channel_v1"
        const val NOTIFICATION_ID = 9001

        const val ACTION_START_SERVICE = "com.example.ACTION_START_LOCATION_FG"
        const val ACTION_STOP_SERVICE = "com.example.ACTION_STOP_LOCATION_FG"
        const val ACTION_LOCATION_BROADCAST = "com.example.RADAR_LOCATION_BROADCAST"

        const val EXTRA_LATITUDE = "extra_latitude"
        const val EXTRA_LONGITUDE = "extra_longitude"
        const val EXTRA_SPEED_KMH = "extra_speed_kmh"
        const val EXTRA_ACCURACY = "extra_accuracy"
        const val EXTRA_SAFETY_LOCK = "extra_safety_lock"

        const val SPEED_SAFETY_LOCK_KMH = 10.0

        // Estado global de telemetria compartilhado para consumo direto (Android Auto e WebView)
        private val _telemetryState = MutableStateFlow(TelemetryData())
        val telemetryState: StateFlow<TelemetryData> = _telemetryState.asStateFlow()

        fun start(context: Context) {
            val intent = Intent(context, LocationForegroundService::class.java).apply {
                action = ACTION_START_SERVICE
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, LocationForegroundService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.startService(intent)
        }

        /**
         * Cálculo de Distância Geodésica entre duas coordenadas usando a fórmula de Haversine.
         * Retorna a distância exata em metros.
         */
        fun calculateHaversineMeters(
            lat1: Double, lon1: Double,
            lat2: Double, lon2: Double
        ): Double {
            val earthRadiusM = 6371000.0 // Raio médio da Terra em metros
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)

            val a = sin(dLat / 2) * sin(dLat / 2) +
                    cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                    sin(dLon / 2) * sin(dLon / 2)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))

            return earthRadiusM * c
        }
    }

    data class TelemetryData(
        val latitude: Double = -23.561684,
        val longitude: Double = -46.655981,
        val speedKmh: Double = 0.0,
        val accuracyMeters: Float = 0.0f,
        val isSafetyLockActive: Boolean = false,
        val isRunning: Boolean = false,
        val timestamp: Long = System.currentTimeMillis()
    )

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null

    private var lastLocation: Location? = null
    private var lastLocationTimestamp: Long = 0L

    inner class LocalBinder : Binder() {
        fun getService(): LocationForegroundService = this@LocationForegroundService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SERVICE -> startForegroundTracking()
            ACTION_STOP_SERVICE -> stopForegroundTracking()
            else -> startForegroundTracking()
        }
        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun startForegroundTracking() {
        startForeground(NOTIFICATION_ID, buildForegroundNotification(0.0, false))

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L).apply {
            setMinUpdateIntervalMillis(500L)
            setMinUpdateDistanceMeters(0.5f)
            setWaitForAccurateLocation(false)
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                processLocationUpdate(loc)
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
            Log.d(TAG, "Monitoramento GPS de alta precisão iniciado com sucesso.")
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao requisitar updates de localização: ${e.message}")
        }
    }

    private fun processLocationUpdate(loc: Location) {
        val now = System.currentTimeMillis()
        var calculatedSpeedKmh = if (loc.hasSpeed() && loc.speed >= 0f) {
            loc.speed * 3.6
        } else {
            0.0
        }

        // Refinamento via Haversine se disponível
        val prev = lastLocation
        if (prev != null && lastLocationTimestamp > 0) {
            val deltaSeconds = (now - lastLocationTimestamp) / 1000.0
            if (deltaSeconds in 0.4..5.0) {
                val haversineDistance = calculateHaversineMeters(
                    prev.latitude, prev.longitude,
                    loc.latitude, loc.longitude
                )
                val haversineSpeedKmh = (haversineDistance / deltaSeconds) * 3.6
                if (!loc.hasSpeed() || calculatedSpeedKmh <= 0.5) {
                    calculatedSpeedKmh = haversineSpeedKmh
                }
            }
        }

        lastLocation = loc
        lastLocationTimestamp = now

        val speedClamped = String.format(Locale.US, "%.1f", calculatedSpeedKmh).toDoubleOrNull() ?: 0.0
        val isLocked = speedClamped > SPEED_SAFETY_LOCK_KMH

        // 1. Atualiza StateFlow Global para observadores diretos
        val newTelemetry = TelemetryData(
            latitude = loc.latitude,
            longitude = loc.longitude,
            speedKmh = speedClamped,
            accuracyMeters = loc.accuracy,
            isSafetyLockActive = isLocked,
            isRunning = true,
            timestamp = now
        )
        _telemetryState.value = newTelemetry

        // 2. Transmite via Broadcast para a WebView e componentes do sistema
        val broadcastIntent = Intent(ACTION_LOCATION_BROADCAST).apply {
            putExtra(EXTRA_LATITUDE, loc.latitude)
            putExtra(EXTRA_LONGITUDE, loc.longitude)
            putExtra(EXTRA_SPEED_KMH, speedClamped)
            putExtra(EXTRA_ACCURACY, loc.accuracy)
            putExtra(EXTRA_SAFETY_LOCK, isLocked)
        }
        sendBroadcast(broadcastIntent)

        // 3. Atualiza notificação persistente em primeiro plano
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(NOTIFICATION_ID, buildForegroundNotification(speedClamped, isLocked))
    }

    private fun stopForegroundTracking() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
        _telemetryState.value = _telemetryState.value.copy(isRunning = false, speedKmh = 0.0)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Radar Telemetria Contínua (GPS/HUD)",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitoramento ininterrupto de rota e velocímetro com trava de segurança"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(speedKmh: Double, isLocked: Boolean): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val statusText = if (isLocked) {
            "🔒 MODO PILOTAGEM ATIVO (> 10 km/h) • ${String.format(Locale.GERMANY, "%.1f", speedKmh)} km/h"
        } else {
            "⚡ Cockpit Ativo • ${String.format(Locale.GERMANY, "%.1f", speedKmh)} km/h • GPS Alta Precisão"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle("Radar Coordinator — Jarvis Cockpit")
            .setContentText(statusText)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
        serviceScope.cancel()
    }
}
