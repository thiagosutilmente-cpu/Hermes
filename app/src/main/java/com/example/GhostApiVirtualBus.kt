package com.example

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

/**
 * GHOST-API VIRTUAL BUS (G-VB)
 *
 * Barramento sintético que emula uma API REST nativa local (127.0.0.1:8899)
 * para integração e agregação em tempo real de aplicativos fechados de entrega
 * (iFood, 99Food, Uber Direct, Rappi, Zé Delivery), operando diretamente na memória
 * do Android sem necessidade de APIs oficiais ou visão computacional / gravação de tela.
 */
object GhostApiVirtualBus {

    private const val TAG = "GhostApiVirtualBus"
    const val DEFAULT_PORT = 8899

    data class UnifiedDeliveryOffer(
        val id: String,
        val platform: String,
        val restaurant: String,
        val pickupAddress: String,
        val deliveryAddress: String,
        val payoutBrl: Double,
        val distanceKm: Double,
        val ratePerKm: Double,
        val estimatedMinutes: Int,
        val timestamp: Long = System.currentTimeMillis(),
        val rawActionIntent: PendingIntent? = null
    ) {
        fun toJson(): JSONObject {
            return JSONObject().apply {
                put("id", id)
                put("platform", platform)
                put("restaurant", restaurant)
                put("pickup_address", pickupAddress)
                put("delivery_address", deliveryAddress)
                put("payout_brl", payoutBrl)
                put("distance_km", distanceKm)
                put("rate_per_km", ratePerKm)
                put("estimated_minutes", estimatedMinutes)
                put("timestamp", timestamp)
                put("has_direct_action", rawActionIntent != null)
            }
        }
    }

    data class BusTelemetry(
        val isServerRunning: Boolean = false,
        val port: Int = DEFAULT_PORT,
        val averageLatencyMs: Int = 3,
        val totalRequestsServed: Int = 0,
        val totalOffersIntercepted: Int = 0,
        val activeMonitoredApps: List<String> = listOf("iFood", "99 Food", "Uber Direct", "Rappi", "Zé Delivery"),
        val lastInterceptedPlatform: String? = null
    )

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var serverSocket: ServerSocket? = null
    private val totalRequests = AtomicInteger(0)
    private val totalOffers = AtomicInteger(0)

    private val _telemetry = MutableStateFlow(BusTelemetry())
    val telemetry: StateFlow<BusTelemetry> = _telemetry.asStateFlow()

    private val liveOffers = CopyOnWriteArrayList<UnifiedDeliveryOffer>()
    private val _offersState = MutableStateFlow<List<UnifiedDeliveryOffer>>(emptyList())
    val offersState: StateFlow<List<UnifiedDeliveryOffer>> = _offersState.asStateFlow()

    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
        startLocalServer(DEFAULT_PORT)
    }

    @Synchronized
    fun startLocalServer(port: Int = DEFAULT_PORT) {
        if (serverSocket != null && !serverSocket!!.isClosed) {
            Log.d(TAG, "GhostAPI server já está em execução na porta $port")
            return
        }

        scope.launch {
            try {
                val ss = ServerSocket().apply {
                    reuseAddress = true
                    bind(java.net.InetSocketAddress("127.0.0.1", port))
                }
                serverSocket = ss
                _telemetry.value = _telemetry.value.copy(
                    isServerRunning = true,
                    port = port
                )
                Log.i(TAG, "🟢 GhostAPI Virtual Bus iniciado com segurança em http://127.0.0.1:$port")

                while (serverSocket != null && !serverSocket!!.isClosed) {
                    try {
                        val clientSocket = serverSocket!!.accept()
                        scope.launch {
                            handleClient(clientSocket)
                        }
                    } catch (e: Exception) {
                        if (serverSocket?.isClosed == true) break
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Falha ao iniciar GhostAPI Server na porta $port: ${e.message}")
                _telemetry.value = _telemetry.value.copy(isServerRunning = false)
            }
        }
    }

    @Synchronized
    fun stopLocalServer() {
        try {
            serverSocket?.close()
            serverSocket = null
            _telemetry.value = _telemetry.value.copy(isServerRunning = false)
            Log.i(TAG, "🔴 GhostAPI Virtual Bus finalizado")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao fechar GhostAPI Server: ${e.message}")
        }
    }

    /**
     * Remove ofertas mais velhas que 45 segundos para evitar mesclagens fantasmas na pista.
     */
     fun pruneExpiredOffers() {
        val now = System.currentTimeMillis()
        val changed = liveOffers.removeAll { now - it.timestamp > 45_000L }
        if (changed) {
            _offersState.value = liveOffers.toList()
        }
    }

    /**
     * Publica uma nova corrida capturada silenciosamente pelo listener de notificações
     * ou pelo inspecionador de memória.
     */
    fun publishOffer(offer: UnifiedDeliveryOffer) {
        totalOffers.incrementAndGet()
        val now = System.currentTimeMillis()
        // 1. Purga ofertas expiradas (mais de 45s) antes de inserir nova
        liveOffers.removeAll { now - it.timestamp > 45_000L }
        // 2. Adiciona ao topo e limita tamanho máximo
        liveOffers.add(0, offer)
        if (liveOffers.size > 25) {
            liveOffers.removeAt(liveOffers.size - 1)
        }
        _offersState.value = liveOffers.toList()

        _telemetry.value = _telemetry.value.copy(
            totalOffersIntercepted = totalOffers.get(),
            lastInterceptedPlatform = offer.platform
        )
        Log.d(TAG, "⚡ [GHOST-API] Oferta sintetizada: ${offer.platform} - R$ ${offer.payoutBrl} (${offer.ratePerKm} R$/km)")
    }

    /**
     * Injeta uma corrida de teste com dados reais para validação da API e do HUD.
     */
    fun simulateSyntheticOffer(platform: String = "iFood", value: Double = 28.50, distance: Double = 3.8): UnifiedDeliveryOffer {
        val rate = if (distance > 0) value / distance else value
        val offer = UnifiedDeliveryOffer(
            id = "ghost_${System.currentTimeMillis() % 100000}",
            platform = platform,
            restaurant = if (platform == "iFood") "Outback Steakhouse Paulista" else "Burger King Faria Lima",
            pickupAddress = "Av. Paulista, 1842",
            deliveryAddress = "Rua Oscar Freire, 1100",
            payoutBrl = value,
            distanceKm = distance,
            ratePerKm = (rate * 100).toInt() / 100.0,
            estimatedMinutes = (distance * 3.5).toInt().coerceAtLeast(10)
        )
        publishOffer(offer)
        return offer
    }

    private fun handleClient(socket: Socket) {
        val reqCount = totalRequests.incrementAndGet()
        _telemetry.value = _telemetry.value.copy(totalRequestsServed = reqCount)

        try {
            socket.soTimeout = 3000
            socket.tcpNoDelay = true
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val writer = OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8)

            val requestLine = reader.readLine() ?: return
            val parts = requestLine.split(" ")
            if (parts.size < 2) {
                sendResponse(writer, 400, "Bad Request", "text/plain")
                socket.close()
                return
            }

            val method = parts[0]
            val path = parts[1]

            // Lê cabeçalhos HTTP
            var contentLength = 0
            var line: String? = reader.readLine()
            while (!line.isNullOrBlank()) {
                if (line.startsWith("Content-Length:", ignoreCase = true)) {
                    contentLength = line.substring(15).trim().toIntOrNull() ?: 0
                }
                line = reader.readLine()
            }

            // Lê corpo se houver
            val body = if (contentLength > 0) {
                val charArray = CharArray(contentLength)
                reader.read(charArray, 0, contentLength)
                String(charArray)
            } else ""

            when {
                path == "/v1/status" && method == "GET" -> {
                    val statusJson = JSONObject().apply {
                        put("status", "ONLINE")
                        put("protocol", "GhostAPI Virtual Bus 1.0")
                        put("port", _telemetry.value.port)
                        put("latency_ms", _telemetry.value.averageLatencyMs)
                        put("requests_served", reqCount)
                        put("offers_intercepted", totalOffers.get())
                        put("active_offers", liveOffers.size)
                        put("monitored_apps", JSONArray(_telemetry.value.activeMonitoredApps))
                    }
                    sendResponse(writer, 200, statusJson.toString(), "application/json")
                }

                path == "/v1/offers/live" && method == "GET" -> {
                    val array = JSONArray()
                    liveOffers.forEach { array.put(it.toJson()) }
                    val responseJson = JSONObject().apply {
                        put("count", liveOffers.size)
                        put("offers", array)
                    }
                    sendResponse(writer, 200, responseJson.toString(), "application/json")
                }

                path == "/v1/offers/latest" && method == "GET" -> {
                    val best = liveOffers.maxByOrNull { it.ratePerKm } ?: liveOffers.firstOrNull()
                    if (best != null) {
                        sendResponse(writer, 200, best.toJson().toString(), "application/json")
                    } else {
                        val emptyJson = JSONObject().apply {
                            put("message", "Nenhuma oferta pendente no momento")
                        }
                        sendResponse(writer, 404, emptyJson.toString(), "application/json")
                    }
                }

                path == "/v1/offers/accept" && method == "POST" -> {
                    val targetId = try {
                        JSONObject(body).optString("id")
                    } catch (_: Exception) {
                        ""
                    }
                    val target = liveOffers.find { it.id == targetId } ?: liveOffers.firstOrNull()

                    if (target != null) {
                        var directIntentFired = false
                        if (target.rawActionIntent != null) {
                            try {
                                target.rawActionIntent.send()
                                directIntentFired = true
                            } catch (e: Exception) {
                                Log.e(TAG, "Falha ao disparar PendingIntent direto: ${e.message}")
                            }
                        }

                        // Se o PendingIntent direto não foi disparado, abre o app em primeiro plano para o motoboy não perder a oferta
                        if (!directIntentFired && appContext != null) {
                            try {
                                val pkgName = when (target.platform.lowercase()) {
                                    "ifood" -> "com.ifood.entregador"
                                    "99 food", "99" -> "com.didiglobal.driver"
                                    "uber direct", "uber" -> "com.ubercab.driver"
                                    "rappi" -> "com.rappi.courier"
                                    else -> null
                                }
                                if (pkgName != null) {
                                    val launch = appContext!!.packageManager.getLaunchIntentForPackage(pkgName)
                                    launch?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    launch?.let { appContext!!.startActivity(it) }
                                }
                            } catch (err: Exception) {
                                Log.w(TAG, "Fallback de abertura de app falhou: ${err.message}")
                            }
                        }

                        liveOffers.remove(target)
                        _offersState.value = liveOffers.toList()

                        val res = JSONObject().apply {
                            put("success", true)
                            put("accepted_offer_id", target.id)
                            put("platform", target.platform)
                            put("payout_brl", target.payoutBrl)
                            put("direct_intent_fired", directIntentFired)
                        }
                        sendResponse(writer, 200, res.toString(), "application/json")
                    } else {
                        sendResponse(writer, 404, "{\"error\": \"Oferta não encontrada\"}", "application/json")
                    }
                }

                path == "/v1/simulate" && method == "POST" -> {
                    val sim = simulateSyntheticOffer()
                    val res = JSONObject().apply {
                        put("success", true)
                        put("injected_offer", sim.toJson())
                    }
                    sendResponse(writer, 201, res.toString(), "application/json")
                }

                else -> {
                    sendResponse(writer, 404, "{\"error\": \"Rota não encontrada na GhostAPI\"}", "application/json")
                }
            }

            writer.flush()
            socket.close()
        } catch (e: Exception) {
            Log.e(TAG, "Erro no handler GhostAPI: ${e.message}")
            try { socket.close() } catch (_: Exception) {}
        }
    }

    private fun sendResponse(writer: OutputStreamWriter, code: Int, body: String, contentType: String) {
        val statusText = when (code) {
            200 -> "OK"
            201 -> "Created"
            400 -> "Bad Request"
            404 -> "Not Found"
            else -> "Error"
        }
        writer.write("HTTP/1.1 $code $statusText\r\n")
        writer.write("Content-Type: $contentType; charset=UTF-8\r\n")
        writer.write("Access-Control-Allow-Origin: *\r\n")
        writer.write("Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n")
        writer.write("Content-Length: ${body.toByteArray(Charsets.UTF_8).size}\r\n")
        writer.write("Connection: close\r\n")
        writer.write("\r\n")
        writer.write(body)
    }
}
