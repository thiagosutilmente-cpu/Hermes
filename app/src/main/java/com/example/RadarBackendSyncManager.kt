package com.example

import android.content.Context
import android.os.Build
import android.util.Log
import com.example.network.HostingerVpsApiService
import com.example.network.OfferDecisionRequest
import com.example.network.RetrofitClient
import com.example.network.VoiceCommandRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * RadarBackendSyncManager
 *
 * Motor de sincronização de telemetria, ofertas e comandos de voz com o servidor central (VPS Hostinger / Cloud API).
 * Utiliza o cliente Retrofit (`HostingerVpsApiService`) com OkHttp e serialização Gson para comunicação estruturada.
 */
object RadarBackendSyncManager {

    private const val TAG = "RadarBackendSyncManager"
    private const val PREFS_NAME = "radar_backend_sync_prefs"
    private const val KEY_SERVER_URL = "pref_server_url"
    private const val KEY_AUTO_SYNC_ENABLED = "pref_auto_sync_enabled"

    // Endpoint padrão da VPS na Hostinger
    const val DEFAULT_SERVER_URL = "http://187.77.248.73:8080"

    // Fila em memória de eventos pendentes de envio
    private val pendingQueue = ConcurrentLinkedQueue<SyncPayload>()

    // Estados observáveis
    private val _syncState = MutableStateFlow(SyncStatusState())
    val syncState: StateFlow<SyncStatusState> = _syncState.asStateFlow()

    private val _recentDispatches = MutableStateFlow<List<DispatchedEventLog>>(emptyList())
    val recentDispatches: StateFlow<List<DispatchedEventLog>> = _recentDispatches.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)
    private var isInitialized = false

    data class SyncStatusState(
        val isSyncing: Boolean = false,
        val serverUrl: String = DEFAULT_SERVER_URL,
        val isConnected: Boolean = true,
        val pendingCount: Int = 0,
        val lastSyncTimestamp: Long = 0L,
        val lastSyncSummary: String = "Retrofit Pronto para envio",
        val isAutoSyncEnabled: Boolean = true,
        val totalSentSuccess: Int = 0,
        val totalFailed: Int = 0,
        val engineType: String = "Retrofit 2.9 (Hostinger VPS)"
    )

    data class DispatchedEventLog(
        val id: String,
        val type: String, // "OFFER_DECISION", "VOICE_COMMAND", "TELEMETRY", "PING"
        val summary: String,
        val timestampFormatted: String,
        val status: String, // "ENVIADO (HTTP 200)", "FILA OFFLINE", "SIMULADO"
        val payloadSnippet: String
    )

    data class SyncPayload(
        val id: String = "evt_${System.currentTimeMillis()}_${(1000..9999).random()}",
        val endpoint: String,
        val jsonBody: JSONObject,
        val eventType: String,
        val summary: String,
        val offerDecisionRequest: OfferDecisionRequest? = null,
        val voiceCommandRequest: VoiceCommandRequest? = null,
        val createdAt: Long = System.currentTimeMillis()
    )

    fun initialize(context: Context) {
        if (isInitialized) return
        isInitialized = true

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedUrl = prefs.getString(KEY_SERVER_URL, DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL
        val isAutoSync = prefs.getBoolean(KEY_AUTO_SYNC_ENABLED, true)

        RetrofitClient.updateBaseUrl(savedUrl)

        _syncState.value = _syncState.value.copy(
            serverUrl = savedUrl,
            isAutoSyncEnabled = isAutoSync
        )
    }

    fun updateServerUrl(context: Context, newUrl: String) {
        val sanitized = if (newUrl.startsWith("http://") || newUrl.startsWith("https://")) newUrl else "http://$newUrl"
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SERVER_URL, sanitized).apply()

        RetrofitClient.updateBaseUrl(sanitized)
        _syncState.value = _syncState.value.copy(serverUrl = sanitized)
    }

    fun setAutoSyncEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_AUTO_SYNC_ENABLED, enabled).apply()
        _syncState.value = _syncState.value.copy(isAutoSyncEnabled = enabled)
    }

    /**
     * 1. Despacha evento de Decisão de Oferta (Aceite / Recusa) via Retrofit para a VPS Hostinger
     */
    fun sendOfferDecision(
        context: Context,
        offerId: String,
        appName: String,
        restaurant: String,
        value: Double,
        distanceKm: Double,
        gainPerKm: Double,
        action: String, // "ACCEPTED" ou "DECLINED"
        source: String, // "Toque na Tela", "Comando de Voz", "HUD Flutuante"
        reason: String = "",
        deliveryAddress: String = ""
    ) {
        val request = OfferDecisionRequest(
            eventType = "OFFER_DECISION",
            offerId = offerId,
            action = action,
            appName = appName,
            restaurant = restaurant,
            value = value,
            distanceKm = distanceKm,
            gainPerKm = gainPerKm,
            source = source,
            reason = reason,
            deliveryAddress = deliveryAddress,
            deviceId = Build.MODEL,
            androidVersion = Build.VERSION.RELEASE,
            timestamp = System.currentTimeMillis()
        )

        val json = JSONObject().apply {
            put("eventType", "OFFER_DECISION")
            put("offerId", offerId)
            put("action", action)
            put("appName", appName)
            put("restaurant", restaurant)
            put("value", value)
            put("distanceKm", distanceKm)
            put("gainPerKm", gainPerKm)
            put("source", source)
            put("reason", reason)
            put("deliveryAddress", deliveryAddress)
            put("deviceId", Build.MODEL)
            put("androidVersion", Build.VERSION.RELEASE)
            put("timestamp", System.currentTimeMillis())
        }

        val summary = "$action: $restaurant (R$ ${String.format(Locale.GERMAN, "%.2f", value)}) via $source"
        val payload = SyncPayload(
            endpoint = "/api/offers/decision",
            jsonBody = json,
            eventType = "OFFER_DECISION",
            summary = summary,
            offerDecisionRequest = request
        )

        enqueueAndDispatch(context, payload)
    }

    /**
     * 2. Despacha evento de Comando de Voz (STT) reconhecido no capacete via Retrofit
     */
    fun sendVoiceCommandEvent(
        context: Context,
        commandName: String,
        spokenText: String,
        actionTaken: String,
        confidence: Float = 0.95f
    ) {
        val request = VoiceCommandRequest(
            eventType = "VOICE_COMMAND",
            command = commandName,
            spokenText = spokenText,
            actionTaken = actionTaken,
            confidence = confidence,
            micSource = "BLUETOOTH_HELMET_OR_PHONE",
            timestamp = System.currentTimeMillis()
        )

        val json = JSONObject().apply {
            put("eventType", "VOICE_COMMAND")
            put("command", commandName)
            put("spokenText", spokenText)
            put("actionTaken", actionTaken)
            put("confidence", confidence)
            put("micSource", "BLUETOOTH_HELMET_OR_PHONE")
            put("timestamp", System.currentTimeMillis())
        }

        val summary = "Voz: \"$spokenText\" ➔ $commandName"
        val payload = SyncPayload(
            endpoint = "/api/voice/command",
            jsonBody = json,
            eventType = "VOICE_COMMAND",
            summary = summary,
            voiceCommandRequest = request
        )

        enqueueAndDispatch(context, payload)
    }

    /**
     * 3. Despacha telemetria de velocidade e segurança
     */
    fun sendSpeedTelemetry(
        context: Context,
        speedKmh: Double,
        speedThresholdKmh: Double,
        isLockActive: Boolean,
        latitude: Double = 0.0,
        longitude: Double = 0.0
    ) {
        val json = JSONObject().apply {
            put("eventType", "SPEED_TELEMETRY")
            put("speedKmh", speedKmh)
            put("thresholdKmh", speedThresholdKmh)
            put("isLocked", isLockActive)
            put("latitude", latitude)
            put("longitude", longitude)
            put("timestamp", System.currentTimeMillis())
        }

        val summary = "Velocidade: ${String.format(Locale.GERMAN, "%.1f", speedKmh)} km/h (Trava: ${if (isLockActive) "LIGADA" else "LIBERADA"})"
        val payload = SyncPayload(
            endpoint = "/api/telemetry/speed",
            jsonBody = json,
            eventType = "TELEMETRY",
            summary = summary
        )

        enqueueAndDispatch(context, payload)
    }

    /**
     * Realiza um teste de Ping / Conexão imediato com a VPS usando Retrofit
     */
    fun testServerConnection(context: Context, onResult: (Boolean, String) -> Unit) {
        scope.launch {
            try {
                val apiService = RetrofitClient.getApiService(_syncState.value.serverUrl)
                val response = apiService.checkHealth()

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        _syncState.value = _syncState.value.copy(
                            isConnected = true,
                            lastSyncSummary = "VPS Hostinger Online: ${body?.status ?: "HTTP 200 OK"}"
                        )
                        onResult(true, "VPS Conectada via Retrofit (${response.code()} OK)")
                    } else {
                        // Fallback HTTP padrão caso endpoint não exista ainda na VPS
                        _syncState.value = _syncState.value.copy(
                            isConnected = true,
                            lastSyncSummary = "Servidor respondeu HTTP ${response.code()}"
                        )
                        onResult(true, "Servidor Hostinger acessível (${response.code()})")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Falha no ping Retrofit: ${e.message}")
                withContext(Dispatchers.Main) {
                    _syncState.value = _syncState.value.copy(
                        isConnected = false,
                        lastSyncSummary = "Falha: ${e.message}"
                    )
                    onResult(false, e.localizedMessage ?: "Sem resposta da VPS Hostinger")
                }
            }
        }
    }

    /**
     * Força a sincronização de todos os itens pendentes na fila
     */
    fun flushPendingQueue(context: Context) {
        scope.launch {
            processPendingQueue(context)
        }
    }

    private fun enqueueAndDispatch(context: Context, payload: SyncPayload) {
        pendingQueue.add(payload)
        updateStateQueueCount()

        if (_syncState.value.isAutoSyncEnabled) {
            scope.launch {
                processPendingQueue(context)
            }
        }
    }

    private fun updateStateQueueCount() {
        _syncState.value = _syncState.value.copy(pendingCount = pendingQueue.size)
    }

    private suspend fun processPendingQueue(context: Context) {
        if (pendingQueue.isEmpty()) return

        _syncState.value = _syncState.value.copy(isSyncing = true)
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val apiService = RetrofitClient.getApiService(_syncState.value.serverUrl)

        while (pendingQueue.isNotEmpty()) {
            val item = pendingQueue.poll() ?: break

            val result: HttpResult = try {
                when {
                    item.offerDecisionRequest != null -> {
                        val response = apiService.sendOfferDecision(item.offerDecisionRequest)
                        if (response.isSuccessful) {
                            val serverData = response.body()?.data
                            val serverVoiceAlert = serverData?.voiceAlert
                            // Reproduz automaticamente o voice_alert retornado pelo servidor VPS
                            if (!serverVoiceAlert.isNullOrBlank()) {
                                try {
                                    TextToSpeechManager.getInstance(context).speakVoiceAlert(serverVoiceAlert)
                                } catch (_: Exception) {}
                            }
                            HttpResult(true, response.code(), "Sucesso Retrofit: ${response.body()?.message ?: "Gravado"}", false)
                        } else {
                            HttpResult(false, response.code(), "HTTP ${response.code()}", false)
                        }
                    }
                    item.voiceCommandRequest != null -> {
                        val response = apiService.sendVoiceCommand(item.voiceCommandRequest)
                        if (response.isSuccessful) {
                            HttpResult(true, response.code(), "Voz registrada: ${response.code()}", false)
                        } else {
                            HttpResult(false, response.code(), "HTTP ${response.code()}", false)
                        }
                    }
                    else -> {
                        // Fallback genérico para telemetria ou outros endpoints
                        val baseUrl = _syncState.value.serverUrl.trimEnd('/')
                        executeRawHttpRequest("$baseUrl${item.endpoint}", "POST", item.jsonBody)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Exceção no envio via Retrofit: ${e.message}")
                HttpResult(false, 0, e.localizedMessage ?: "Falha de rede", true)
            }

            val statusLabel = if (result.success) {
                "ENVIADO (HTTP ${result.statusCode})"
            } else {
                "FILA OFFLINE (Tentará novamente)"
            }

            // Registra no histórico recente de despachos
            val logItem = DispatchedEventLog(
                id = item.id,
                type = item.eventType,
                summary = item.summary,
                timestampFormatted = timeFormat.format(Date()),
                status = statusLabel,
                payloadSnippet = item.jsonBody.toString().take(180) + if (item.jsonBody.toString().length > 180) "..." else ""
            )

            val currentLogs = _recentDispatches.value.toMutableList()
            currentLogs.add(0, logItem)
            if (currentLogs.size > 50) {
                currentLogs.removeAt(currentLogs.size - 1)
            }
            _recentDispatches.value = currentLogs

            if (result.success) {
                _syncState.value = _syncState.value.copy(
                    totalSentSuccess = _syncState.value.totalSentSuccess + 1,
                    lastSyncTimestamp = System.currentTimeMillis(),
                    lastSyncSummary = "Último envio Retrofit: ${item.summary}"
                )
            } else {
                _syncState.value = _syncState.value.copy(
                    totalFailed = _syncState.value.totalFailed + 1,
                    lastSyncSummary = "Erro ao enviar: ${result.message}"
                )
                // Se falhou por queda de sinal de operadora, mantém na fila
                if (!result.success && result.isNetworkError) {
                    pendingQueue.add(item)
                    break
                }
            }
            updateStateQueueCount()
        }

        _syncState.value = _syncState.value.copy(isSyncing = false)
        updateStateQueueCount()
    }

    data class HttpResult(
        val success: Boolean,
        val statusCode: Int,
        val message: String,
        val isNetworkError: Boolean
    )

    private fun executeRawHttpRequest(
        fullUrl: String,
        method: String,
        jsonBody: JSONObject?
    ): HttpResult {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL(fullUrl)
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = method
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "RadarCoordinator-Retrofit/${Build.VERSION.SDK_INT}")
                connectTimeout = 5000
                readTimeout = 5000
                if (jsonBody != null) {
                    doOutput = true
                }
            }

            if (jsonBody != null) {
                OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
                    writer.write(jsonBody.toString())
                    writer.flush()
                }
            }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val responseText = stream?.let {
                BufferedReader(InputStreamReader(it)).use { reader -> reader.readText() }
            } ?: ""

            if (code in 200..299) {
                HttpResult(
                    success = true,
                    statusCode = code,
                    message = if (responseText.isNotBlank()) responseText else "OK",
                    isNetworkError = false
                )
            } else {
                HttpResult(
                    success = false,
                    statusCode = code,
                    message = "HTTP $code: $responseText",
                    isNetworkError = false
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Falha de conexão com servidor ($fullUrl): ${e.message}")
            HttpResult(
                success = false,
                statusCode = 0,
                message = e.localizedMessage ?: "Sem conexão com servidor",
                isNetworkError = true
            )
        } finally {
            conn?.disconnect()
        }
    }
}
