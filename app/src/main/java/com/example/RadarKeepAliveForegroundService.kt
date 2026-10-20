package com.example

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
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
 * RadarKeepAliveForegroundService
 *
 * Foreground Service com proteção total contra encerramento pelo sistema operacional
 * durante estados de economia de bateria (Doze Mode, App Standby).
 *
 * Pilares de Funcionamento:
 * 1. Notificação Permanente com Botões de Ação Rápida (SOS Silencioso, Alternar Radar, Abrir Cockpit).
 * 2. WakeLock Parcial de CPU ([PowerManager.PARTIAL_WAKE_LOCK]) com liberação segura no ciclo de vida.
 * 3. Listener Contínuo de Alta Precisão GPS ([FusedLocationProviderClient]) com sincronização de velocidade.
 * 4. Conexão WebSocket Persistente e Auto-Reconhecível com a VPS Hostinger ([WebSocketKeepAliveEngine]).
 * 5. Declaração dupla de tipo de foreground service: `location` e `dataSync`.
 */
class RadarKeepAliveForegroundService : Service() {

    companion object {
        private const val TAG = "RadarKeepAliveService"
        const val NOTIFICATION_CHANNEL_ID = "channel_radar_keepalive_v2"
        const val NOTIFICATION_ID = 8801

        const val ACTION_START = "com.example.radar.START_KEEPALIVE_SERVICE"
        const val ACTION_STOP = "com.example.radar.STOP_KEEPALIVE_SERVICE"
        const val ACTION_UPDATE_NOTIFICATION = "com.example.radar.UPDATE_NOTIFICATION"

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

        private val _isRadarMonitoringActive = MutableStateFlow(true)
        val isRadarMonitoringActive: StateFlow<Boolean> = _isRadarMonitoringActive.asStateFlow()

        /**
         * Inicia o serviço persistente em primeiro plano
         */
        fun start(context: Context) {
            val intent = Intent(context, RadarKeepAliveForegroundService::class.java).apply {
                action = ACTION_START
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Falha ao disparar RadarKeepAliveForegroundService", e)
            }
        }

        /**
         * Encerra o serviço
         */
        fun stop(context: Context) {
            val intent = Intent(context, RadarKeepAliveForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Falha ao encerrar RadarKeepAliveForegroundService", e)
            }
        }

        /**
         * Alterna o estado do radar diretamente da notificação ou do cockpit
         */
        fun toggleRadarState(context: Context) {
            val nextState = !_isRadarMonitoringActive.value
            _isRadarMonitoringActive.value = nextState
            val msg = if (nextState) "Radar de ofertas ativado!" else "Radar pausado temporariamente."
            Log.i(TAG, msg)
            try {
                HapticFeedbackHelper.vibrateTap(context)
                TextToSpeechManager.getInstance(context).speakVoiceAlert(msg)
            } catch (_: Exception) {}

            val updateIntent = Intent(context, RadarKeepAliveForegroundService::class.java).apply {
                action = ACTION_UPDATE_NOTIFICATION
            }
            context.startService(updateIntent)
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var webSocketEngine: WebSocketKeepAliveEngine

    private var wakeLock: PowerManager.WakeLock? = null
    private var lastLocation: Location? = null

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "⚡ Criando RadarKeepAliveForegroundService...")

        createNotificationChannel()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        webSocketEngine = WebSocketKeepAliveEngine.getInstance(this)

        acquireWakeLock()
        setupLocationCallback()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        Log.d(TAG, "onStartCommand com ação: $action")

        when (action) {
            ACTION_STOP -> {
                stopForegroundAndSelf()
                return START_NOT_STICKY
            }

            ACTION_UPDATE_NOTIFICATION -> {
                updateNotification()
                return START_STICKY
            }

            ACTION_START -> {
                startForegroundWithNotification()
                startLocationUpdates()
                webSocketEngine.connect()
                _isServiceRunning.value = true

                // Observa o estado da conexão para atualizar a notificação dinamicamente
                serviceScope.launch {
                    webSocketEngine.connectionState.collect {
                        updateNotification()
                    }
                }
            }
        }

        // START_STICKY garante reinício automático pelo Android caso haja baixa memória severa
        return START_STICKY
    }

    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "RadarDelivery:KeepAliveWakeLock"
            )?.apply {
                setReferenceCounted(false)
                // Timeout máximo de segurança de 12 horas (duração média do turno)
                acquire(12 * 60 * 60 * 1000L)
            }
            Log.i(TAG, "🔋 WakeLock parcial de CPU adquirido com sucesso contra Doze Mode.")
        } catch (e: Exception) {
            Log.w(TAG, "Falha ao adquirir WakeLock parcial: ${e.message}")
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                Log.d(TAG, "WakeLock liberado.")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Erro ao liberar WakeLock: ${e.message}")
        }
    }

    private fun startForegroundWithNotification() {
        val notification = buildKeepAliveNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val serviceTypes = ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION or
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                    } else 0

            startForeground(NOTIFICATION_ID, notification, serviceTypes)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(NOTIFICATION_ID, buildKeepAliveNotification())
    }

    /**
     * Constrói a notificação permanente com telemetria e os botões de ação rápida
     */
    private fun buildKeepAliveNotification(): Notification {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName) ?: Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val mainPendingIntent = PendingIntent.getActivity(
            this,
            100,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 1. Ação Rápida: SOS Silencioso
        val sosIntent = Intent(this, RadarNotificationActionReceiver::class.java).apply {
            action = RadarNotificationActionReceiver.ACTION_TRIGGER_SOS
        }
        val sosPendingIntent = PendingIntent.getBroadcast(
            this,
            101,
            sosIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 2. Ação Rápida: Pausar / Retomar Radar
        val toggleIntent = Intent(this, RadarNotificationActionReceiver::class.java).apply {
            action = RadarNotificationActionReceiver.ACTION_TOGGLE_RADAR
        }
        val togglePendingIntent = PendingIntent.getBroadcast(
            this,
            102,
            toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val locState = LocationService.globalLocationState.value
        val speedStr = String.format(Locale.GERMANY, "%.1f", locState.currentSpeedKmh)
        val isMonitoring = _isRadarMonitoringActive.value
        val socketState = webSocketEngine.connectionState.value

        val socketLabel = when (socketState) {
            is SocketConnectionState.Connected -> "🟢 WebSocket Conectado"
            is SocketConnectionState.Connecting -> "🟡 Conectando..."
            is SocketConnectionState.Reconnecting -> "🔄 Reconectando (T${socketState.attempt})"
            is SocketConnectionState.Error -> "🔴 Socket Offline"
            is SocketConnectionState.Disconnected -> "⚪ Desconectado"
        }

        val title = if (isMonitoring) {
            "🟢 RADAR PILOTO ATIVO • $speedStr km/h"
        } else {
            "⏸️ RADAR PAUSADO • $speedStr km/h"
        }

        val contentText = "$socketLabel • GPS Fused Contínuo"
        val bigText = """
            Velocidade atual: $speedStr km/h
            Monitoramento de Corridas: ${if (isMonitoring) "Ativo e Vigilante" else "Pausado"}
            Status VPS: $socketLabel
            Proteção Anti-Morte: Ativa (CPU WakeLock + Foreground Service)
        """.trimIndent()

        val toggleLabel = if (isMonitoring) "⏸️ Pausar Radar" else "▶️ Retomar Radar"

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(mainPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            // AÇÃO 1: 🚨 SOS Silencioso
            .addAction(
                android.R.drawable.ic_dialog_alert,
                "🚨 SOS Silencioso",
                sosPendingIntent
            )
            // AÇÃO 2: ⚡ Pausar / Retomar
            .addAction(
                android.R.drawable.ic_media_pause,
                toggleLabel,
                togglePendingIntent
            )
            // AÇÃO 3: 🏍️ Abrir Cockpit
            .addAction(
                android.R.drawable.ic_menu_view,
                "🏍️ Cockpit",
                mainPendingIntent
            )
            .build()
    }

    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                processLocation(loc)
            }
        }
    }

    private fun processLocation(location: Location) {
        val speedKmh = if (location.hasSpeed()) {
            (location.speed * 3.6).toDouble()
        } else {
            lastLocation?.let { prev ->
                val dist = location.distanceTo(prev)
                val timeSec = (location.time - prev.time) / 1000.0
                if (timeSec > 0) (dist / timeSec) * 3.6 else 0.0
            } ?: 0.0
        }
        lastLocation = location

        // Sincroniza a localização e velocidade no estado global do app e checa proximidade de blitz
        try {
            LocationService.updateLocationFromKeepAlive(location, speedKmh, applicationContext)
        } catch (e: Exception) {
            Log.w(TAG, "Falha ao sincronizar com LocationService: ${e.message}")
        }

        // Atualiza a notificação de tempos em tempos com a velocidade atual
        updateNotification()

        // Envia telemetria no WebSocket caso esteja conectado
        webSocketEngine.sendTelemetryHeartbeat()
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val hasFine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasFine && !hasCoarse) return

        try {
            val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
                .setMinUpdateIntervalMillis(500L)
                .setMinUpdateDistanceMeters(0.5f)
                .setWaitForAccurateLocation(false)
                .build()

            fusedLocationClient.requestLocationUpdates(req, locationCallback, Looper.getMainLooper())
            Log.d(TAG, "FusedLocationProvider: GPS de alta precisão ativo no KeepAlive.")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao iniciar FusedLocationProvider", e)
        }
    }

    private fun stopLocationUpdates() {
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        } catch (_: Exception) {}
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Radar KeepAlive & Anti-Morte",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Mantém o GPS e o WebSocket permanentemente ativos mesmo em modo de economia de bateria."
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun stopForegroundAndSelf() {
        _isServiceRunning.value = false
        stopLocationUpdates()
        webSocketEngine.disconnect()
        releaseWakeLock()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
        Log.i(TAG, "RadarKeepAliveForegroundService encerrado.")
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForegroundAndSelf()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
