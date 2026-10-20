package com.example

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.BatteryManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.min

/**
 * Estados da Conexão WebSocket em Tempo Real com a VPS Hostinger
 */
sealed class SocketConnectionState {
    data object Disconnected : SocketConnectionState()
    data object Connecting : SocketConnectionState()
    data class Connected(val connectedAtMillis: Long = System.currentTimeMillis()) : SocketConnectionState()
    data class Reconnecting(val attempt: Int, val nextRetrySec: Long) : SocketConnectionState()
    data class Error(val message: String) : SocketConnectionState()
}

/**
 * WebSocketKeepAliveEngine
 *
 * Motor de comunicação contínua via WebSocket nativo ([OkHttpClient]) para manter
 * a sessão do motoboy permanentemente conectada à VPS sem ser derrubada pelo Android.
 *
 * Recursos:
 * 1. Conexão bidirecional persistente em segundo plano.
 * 2. Algoritmo de Auto-Reconexão com Backoff Exponencial (1s, 2s, 4s, 8s, até 30s max).
 * 3. Heartbeat (Ping/Pong) de Telemetria com coordenadas GPS e nível de bateria.
 * 4. Recepção em tempo real de ofertas relâmpago e alertas de trânsito da VPS.
 */
class WebSocketKeepAliveEngine(private val context: Context) {

    companion object {
        private const val TAG = "WebSocketKeepAlive"
        private const val DEFAULT_WS_ENDPOINT = "ws://187.77.248.73:8000/ws/telemetry"
        private const val HEARTBEAT_INTERVAL_MS = 20_000L
        private const val MAX_RECONNECT_DELAY_SEC = 30L

        @Volatile
        private var INSTANCE: WebSocketKeepAliveEngine? = null

        fun getInstance(context: Context): WebSocketKeepAliveEngine {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: WebSocketKeepAliveEngine(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var heartbeatJob: Job? = null
    private var reconnectJob: Job? = null

    private var client: OkHttpClient = OkHttpClient.Builder()
        .pingInterval(15, TimeUnit.SECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // 0 para conexões WebSocket persistentes
        .retryOnConnectionFailure(true)
        .build()

    private var webSocket: WebSocket? = null
    private var reconnectAttempt = 0
    private var isUserExplicitDisconnect = false

    private val _connectionState = MutableStateFlow<SocketConnectionState>(SocketConnectionState.Disconnected)
    val connectionState: StateFlow<SocketConnectionState> = _connectionState.asStateFlow()

    private val _lastHeartbeatTimestamp = MutableStateFlow(0L)
    val lastHeartbeatTimestamp: StateFlow<Long> = _lastHeartbeatTimestamp.asStateFlow()

    private val _receivedMessagesCount = MutableStateFlow(0)
    val receivedMessagesCount: StateFlow<Int> = _receivedMessagesCount.asStateFlow()

    private val _lastReceivedMessage = MutableStateFlow("")
    val lastReceivedMessage: StateFlow<String> = _lastReceivedMessage.asStateFlow()

    /**
     * Inicia a conexão persistente com o WebSocket da VPS.
     */
    fun connect() {
        if (_connectionState.value is SocketConnectionState.Connected ||
            _connectionState.value is SocketConnectionState.Connecting) {
            return
        }

        isUserExplicitDisconnect = false
        reconnectJob?.cancel()

        val wsUrl = resolveWebSocketUrl()
        Log.i(TAG, "🔌 Conectando ao WebSocket da VPS: $wsUrl")
        _connectionState.value = SocketConnectionState.Connecting

        val request = Request.Builder()
            .url(wsUrl)
            .addHeader("User-Agent", "RadarDeliveryCoordinator-AndroidKeepAlive/1.0")
            .build()

        webSocket = client.newWebSocket(request, createSocketListener())
    }

    private fun resolveWebSocketUrl(): String {
        val prefs = context.getSharedPreferences("radar_backend_sync_prefs", Context.MODE_PRIVATE)
        val httpUrl = prefs.getString("pref_server_url", RadarBackendSyncManager.DEFAULT_SERVER_URL)
            ?: RadarBackendSyncManager.DEFAULT_SERVER_URL

        val clean = httpUrl.trimEnd('/')
        return clean
            .replace("https://", "wss://")
            .replace("http://", "ws://") + "/ws/telemetry"
    }

    private fun createSocketListener() = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.i(TAG, "🟢 WebSocket conectado com sucesso na VPS Hostinger! (Status: ${response.code})")
            reconnectAttempt = 0
            _connectionState.value = SocketConnectionState.Connected()
            startHeartbeatLoop()
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            Log.d(TAG, "📩 Mensagem recebida via WebSocket: $text")
            _receivedMessagesCount.value = _receivedMessagesCount.value + 1
            _lastReceivedMessage.value = text
            processServerMessage(text)
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            Log.w(TAG, "⚠️ WebSocket fechando pelo servidor (code $code): $reason")
            webSocket.close(1000, null)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.i(TAG, "🔴 WebSocket encerrado (code $code): $reason")
            stopHeartbeatLoop()
            _connectionState.value = SocketConnectionState.Disconnected
            if (!isUserExplicitDisconnect) {
                scheduleAutoReconnect()
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.w(TAG, "❌ Falha na conexão WebSocket: ${t.message} (Resp: ${response?.code})")
            stopHeartbeatLoop()
            _connectionState.value = SocketConnectionState.Error(t.message ?: "Falha de rede")
            if (!isUserExplicitDisconnect) {
                scheduleAutoReconnect()
            }
        }
    }

    private fun startHeartbeatLoop() {
        stopHeartbeatLoop()
        heartbeatJob = scope.launch {
            while (isActive) {
                sendTelemetryHeartbeat()
                delay(HEARTBEAT_INTERVAL_MS)
            }
        }
    }

    private fun stopHeartbeatLoop() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    /**
     * Transmite pacote de telemetria e heartbeat com bateria e localização.
     */
    fun sendTelemetryHeartbeat() {
        val socket = webSocket ?: return
        if (_connectionState.value !is SocketConnectionState.Connected) return

        try {
            val locationState = LocationService.globalLocationState.value
            val batteryLevel = getBatteryPercentage()
            val payload = JSONObject().apply {
                put("type", "PING_TELEMETRY")
                put("timestamp", System.currentTimeMillis())
                put("latitude", locationState.latitude)
                put("longitude", locationState.longitude)
                put("speedKmh", locationState.currentSpeedKmh)
                put("accuracy", locationState.accuracyMeters)
                put("batteryPercent", batteryLevel)
                put("isSafetyLockActive", locationState.isSafetyLockActive)
                put("isLowPowerProtected", true)
            }
            val sent = socket.send(payload.toString())
            if (sent) {
                _lastHeartbeatTimestamp.value = System.currentTimeMillis()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Erro ao enviar telemetria no WebSocket: ${e.message}")
        }
    }

    private fun getBatteryPercentage(): Int {
        return try {
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 100
        } catch (_: Exception) {
            100
        }
    }

    private fun scheduleAutoReconnect() {
        reconnectJob?.cancel()
        reconnectAttempt++
        // Backoff exponencial: 1s, 2s, 4s, 8s, 16s, 30s
        val delaySec = min(MAX_RECONNECT_DELAY_SEC, (1L shl min(reconnectAttempt, 5)))
        Log.i(TAG, "🔄 Agendando reconexão do WebSocket (tentativa $reconnectAttempt) em $delaySec segundos...")
        _connectionState.value = SocketConnectionState.Reconnecting(reconnectAttempt, delaySec)

        reconnectJob = scope.launch {
            delay(delaySec * 1000L)
            if (isActive && !isUserExplicitDisconnect) {
                connect()
            }
        }
    }

    private fun processServerMessage(jsonText: String) {
        try {
            val json = JSONObject(jsonText)
            val type = json.optString("type")
            if (type == "OFFER_ALERT") {
                // Alerta push instantâneo recebido da VPS
                val restaurant = json.optString("restaurant", "Nova Oferta")
                val value = json.optDouble("value", 0.0)
                val distance = json.optDouble("distanceKm", 0.0)
                TextToSpeechManager.getInstance(context).speakVoiceAlert(
                    "Alerta relâmpago via WebSocket: $restaurant, R$ ${String.format(java.util.Locale.US, "%.2f", value)}"
                )
            }
        } catch (_: Exception) {}
    }

    init {
        registerNetworkCallback()
    }

    private fun registerNetworkCallback() {
        try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val networkRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            connectivityManager?.registerNetworkCallback(networkRequest, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    Log.i(TAG, "🌐 Conectividade com Internet restabelecida. Checando WebSocket...")
                    if (!isUserExplicitDisconnect && _connectionState.value !is SocketConnectionState.Connected) {
                        connect()
                    }
                }

                override fun onLost(network: Network) {
                    Log.w(TAG, "⚠️ Conexão de dados perdida momentaneamente.")
                }
            })
        } catch (e: Exception) {
            Log.w(TAG, "Não foi possível registrar NetworkCallback: ${e.message}")
        }
    }

    /**
     * Força reconexão imediata descartando o backoff exponencial.
     */
    fun forceReconnect() {
        isUserExplicitDisconnect = false
        reconnectJob?.cancel()
        webSocket?.close(1000, "Forçando reconexão manual")
        webSocket = null
        connect()
    }

    /**
     * Encerra a conexão voluntariamente.
     */
    fun disconnect() {
        isUserExplicitDisconnect = true
        reconnectJob?.cancel()
        stopHeartbeatLoop()
        webSocket?.close(1000, "Desconexão solicitada pelo usuário")
        webSocket = null
        _connectionState.value = SocketConnectionState.Disconnected
        Log.i(TAG, "WebSocket desconectado voluntariamente.")
    }
}
