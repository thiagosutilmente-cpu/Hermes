package com.example

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Cliente de Conexão com o Servidor VPS do Jarvis Neural Cockpit (187.77.248.73:8080)
 * Permite geração em tempo real do Pix de R$ 4,90 via Asaas e validação dos 7 dias grátis.
 */
object JarvisVpsApiClient {

    // Endereço público da VPS
    private const val DEFAULT_SERVER_URL = "http://187.77.248.73:8080"

    data class PixResponse(
        val success: Boolean,
        val valor: String,
        val pixCopiaECola: String,
        val subscriptionId: String,
        val errorMessage: String? = null
    )

    data class SubscriptionCheckResponse(
        val isPaid: Boolean,
        val isTrialActive: Boolean,
        val daysRemaining: Int,
        val message: String
    )

    /**
     * Solicita à VPS a criação de uma cobrança de Assinatura Semanal via Pix Asaas (R$ 25,00)
     */
    suspend fun requestWeeklyPix(phone: String, value: Double = 25.00): PixResponse = withContext(Dispatchers.IO) {
        try {
            val url = URL("$DEFAULT_SERVER_URL/api/create-pix")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                connectTimeout = 8000
                readTimeout = 8000
                doOutput = true
            }

            val payload = JSONObject().apply {
                put("phone", phone.ifBlank { "5511999999999" })
                put("plan", "PRO_WEEKLY")
                put("value", value)
            }

            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            val responseCode = conn.responseCode
            val stream = if (responseCode in 200..299) conn.inputStream else conn.errorStream
            val responseText = BufferedReader(InputStreamReader(stream)).use { it.readText() }

            if (responseCode in 200..299) {
                val json = JSONObject(responseText)
                PixResponse(
                    success = true,
                    valor = json.optString("valor", "4,90"),
                    pixCopiaECola = json.optString("pixCopiaECola", ""),
                    subscriptionId = json.optString("subscriptionId", "")
                )
            } else {
                // Fallback de segurança caso a VPS esteja com timeout
                PixResponse(
                    success = false,
                    valor = "4,90",
                    pixCopiaECola = "",
                    subscriptionId = "",
                    errorMessage = "Falha no servidor (HTTP $responseCode)"
                )
            }
        } catch (e: Exception) {
            PixResponse(
                success = false,
                valor = "4,90",
                pixCopiaECola = "",
                subscriptionId = "",
                errorMessage = e.localizedMessage ?: "Erro de conexão com o servidor"
            )
        }
    }

    /**
     * Verifica na VPS o status atual do assinante / trial de 7 dias
     */
    suspend fun checkSubscriptionStatus(phone: String): SubscriptionCheckResponse = withContext(Dispatchers.IO) {
        try {
            val url = URL("$DEFAULT_SERVER_URL/api/check-subscription?phone=$phone")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
                connectTimeout = 6000
                readTimeout = 6000
            }

            if (conn.responseCode in 200..299) {
                val responseText = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                val json = JSONObject(responseText)
                SubscriptionCheckResponse(
                    isPaid = json.optBoolean("isPaid", false),
                    isTrialActive = json.optBoolean("isTrialActive", true),
                    daysRemaining = json.optInt("daysRemaining", 7),
                    message = json.optString("message", "Status verificado")
                )
            } else {
                SubscriptionCheckResponse(
                    isPaid = false,
                    isTrialActive = true,
                    daysRemaining = 7,
                    message = "Modo offline ativo"
                )
            }
        } catch (e: Exception) {
            SubscriptionCheckResponse(
                isPaid = false,
                isTrialActive = true,
                daysRemaining = 7,
                message = "Conexão offline"
            )
        }
    }
}
