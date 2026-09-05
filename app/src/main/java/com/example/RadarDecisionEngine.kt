package com.example

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/**
 * Resultado da análise neural do Jarvis Decision Core
 */
data class NeuralDecision(
    val decision: String, // "accept" ou "decline"
    val confidence: Double, // Ex: 0.95
    val reason: String // Ex: "Ganho/km acima da média"
) {
    val isAccept: Boolean
        get() = decision.equals("accept", ignoreCase = true)

    val confidencePercent: Int
        get() = (confidence * 100).toInt()
}

/**
 * Status de saúde do sistema retornado por /api/health
 */
data class SystemHealthData(
    val score: Int = 94,
    val gpsAccuracy: Double = 4.2,
    val latencyMs: Int = 12,
    val temperature: Double = 28.0,
    val isSyncActive: Boolean = true
)

/**
 * Motor Neural de Avaliação e Sincronização com a API REST do Radar Coordinator
 */
object RadarDecisionEngine {

    private const val API_BASE_URL_EMULATOR = "http://10.0.2.2:5000"
    private const val API_BASE_URL_LOCAL = "http://localhost:5000"

    /**
     * Avalia uma oferta instantaneamente usando as regras de negócio do Radar
     */
    fun evaluate(value: Double, distanceKm: Double, appName: String): NeuralDecision {
        val gainPerKm = if (distanceKm > 0) value / distanceKm else value
        val formattedGain = String.format(Locale.GERMANY, "R$ %.2f", gainPerKm)

        return when {
            gainPerKm >= 5.0 -> {
                NeuralDecision(
                    decision = "accept",
                    confidence = 0.95,
                    reason = "Ganho/km de $formattedGain acima da média"
                )
            }
            gainPerKm >= 3.5 && distanceKm <= 4.0 -> {
                NeuralDecision(
                    decision = "accept",
                    confidence = 0.78,
                    reason = "Distância curta ($distanceKm km) compensa rápido"
                )
            }
            distanceKm > 6.0 -> {
                NeuralDecision(
                    decision = "decline",
                    confidence = 0.88,
                    reason = "Distância excessiva ($distanceKm km) para o retorno"
                )
            }
            else -> {
                NeuralDecision(
                    decision = "decline",
                    confidence = 0.65,
                    reason = "Ganho/km de $formattedGain abaixo do ideal"
                )
            }
        }
    }

    /**
     * Sincroniza a decisão assincronamente com o endpoint REST /api/decision
     */
    suspend fun queryApiDecision(
        value: Double,
        distanceKm: Double,
        appName: String,
        userId: Int = 1
    ): NeuralDecision = withContext(Dispatchers.IO) {
        val fallback = evaluate(value, distanceKm, appName)
        val endpoints = listOf(
            "$API_BASE_URL_LOCAL/api/decision",
            "$API_BASE_URL_EMULATOR/api/decision"
        )

        for (endpoint in endpoints) {
            try {
                val url = URL(endpoint)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 1200
                    readTimeout = 1200
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                }

                val payload = JSONObject().apply {
                    put("value", value)
                    put("distance", distanceKm)
                    put("app", appName)
                    put("user_id", userId)
                }

                OutputStreamWriter(conn.outputStream).use { writer ->
                    writer.write(payload.toString())
                    writer.flush()
                }

                if (conn.responseCode == 200) {
                    val responseStr = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                    val json = JSONObject(responseStr)
                    return@withContext NeuralDecision(
                        decision = json.optString("decision", fallback.decision),
                        confidence = json.optDouble("confidence", fallback.confidence),
                        reason = json.optString("reason", fallback.reason)
                    )
                }
            } catch (_: Exception) {
                // Tenta o próximo endpoint
            }
        }
        return@withContext fallback
    }

    /**
     * Consulta a telemetria do backend em /api/health
     */
    suspend fun fetchSystemHealth(): SystemHealthData = withContext(Dispatchers.IO) {
        val fallback = SystemHealthData()
        val endpoints = listOf(
            "$API_BASE_URL_LOCAL/api/health",
            "$API_BASE_URL_EMULATOR/api/health"
        )
        for (endpoint in endpoints) {
            try {
                val url = URL(endpoint)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 1200
                    readTimeout = 1200
                }
                if (conn.responseCode == 200) {
                    val responseStr = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                    val json = JSONObject(responseStr)
                    return@withContext SystemHealthData(
                        score = json.optInt("score", fallback.score),
                        gpsAccuracy = json.optDouble("gps_accuracy", fallback.gpsAccuracy),
                        latencyMs = json.optInt("latency_ms", fallback.latencyMs),
                        temperature = json.optDouble("temperature", fallback.temperature),
                        isSyncActive = true
                    )
                }
            } catch (_: Exception) {}
        }
        return@withContext fallback
    }

    /**
     * Notifica o aceite do stack ao backend (/api/stacks/accept)
     */
    suspend fun notifyStackAccepted(stackId: String) = withContext(Dispatchers.IO) {
        notifyStackAction("/api/stacks/accept", stackId)
    }

    /**
     * Notifica a recusa do stack ao backend (/api/stacks/decline)
     */
    suspend fun notifyStackDeclined(stackId: String) = withContext(Dispatchers.IO) {
        notifyStackAction("/api/stacks/decline", stackId)
    }

    private fun notifyStackAction(path: String, stackId: String) {
        val endpoints = listOf(
            "$API_BASE_URL_LOCAL$path",
            "$API_BASE_URL_EMULATOR$path"
        )
        for (endpoint in endpoints) {
            try {
                val url = URL(endpoint)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 1000
                    readTimeout = 1000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                }
                val payload = JSONObject().apply {
                    put("stack_id", stackId)
                }
                OutputStreamWriter(conn.outputStream).use { writer ->
                    writer.write(payload.toString())
                    writer.flush()
                }
                if (conn.responseCode in 200..299) {
                    break
                }
            } catch (_: Exception) {}
        }
    }
}
