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
 * Cliente de Conexão com o Servidor VPS do Jarvis Neural Cockpit (187.77.248.73:8000)
 * Permite geração em tempo real do Pix de R$ 4,90 via Asaas e validação dos 7 dias grátis.
 */
object JarvisVpsApiClient {

    // Endereço público da VPS
    private const val DEFAULT_SERVER_URL = "http://187.77.248.73:8000"

    data class PixResponse(
        val success: Boolean,
        val valor: String,
        val pixCopiaECola: String,
        val subscriptionId: String,
        val paymentId: String = "",
        val encodedImage: String = "",
        val invoiceUrl: String = "",
        val isRecurrentPix: Boolean = true,
        val recurrenceInterval: String = "WEEKLY",
        val errorMessage: String? = null
    )

    data class SubscriptionCheckResponse(
        val isPaid: Boolean,
        val isTrialActive: Boolean,
        val daysRemaining: Int,
        val message: String
    )

    /**
     * Solicita à VPS a criação de cobrança/assinatura PIX real no Asaas (sem geração de código fictício offline)
     */
    suspend fun requestRecurrentPix(
        plan: String,
        value: Double,
        phone: String,
        cpfCnpj: String = "",
        name: String = "Cliente Radar Coordinator",
        email: String = ""
    ): PixResponse = withContext(Dispatchers.IO) {
        val valueFormatted = String.format(java.util.Locale.GERMANY, "R$ %.2f", value)
        val recurrenceType = when (plan) {
            "PRO_ANNUAL" -> "ANNUALLY"
            "PRO_MONTHLY" -> "MONTHLY"
            else -> "WEEKLY"
        }
        try {
            val url = URL("$DEFAULT_SERVER_URL/api/create-pix")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                connectTimeout = 10000
                readTimeout = 12000
                doOutput = true
            }

            val payload = JSONObject().apply {
                put("name", name.ifBlank { "Cliente Radar Coordinator" })
                put("email", email)
                put("phone", phone)
                put("cpfCnpj", cpfCnpj)
                put("plan", plan)
                put("recurrent", false)
            }

            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            val responseCode = conn.responseCode
            val stream = if (responseCode in 200..299) conn.inputStream else conn.errorStream
            val responseText = if (stream != null) BufferedReader(InputStreamReader(stream)).use { it.readText() } else "{}"

            if (responseCode in 200..299) {
                val json = JSONObject(responseText)
                val pixCode = json.optString("pixCopiaECola", "")
                PixResponse(
                    success = pixCode.isNotBlank(),
                    valor = json.optString("valor", valueFormatted),
                    pixCopiaECola = pixCode,
                    subscriptionId = json.optString("subscriptionId", json.optString("paymentId", "")),
                    paymentId = json.optString("paymentId", ""),
                    encodedImage = json.optString("encodedImage", ""),
                    invoiceUrl = json.optString("invoiceUrl", ""),
                    isRecurrentPix = true,
                    recurrenceInterval = recurrenceType,
                    errorMessage = if (pixCode.isBlank()) "Código PIX não retornado pelo servidor." else null
                )
            } else {
                val detail = try {
                    JSONObject(responseText).optString("detail", "Erro HTTP $responseCode ao gerar PIX.")
                } catch (_: Exception) {
                    "Erro HTTP $responseCode ao gerar PIX."
                }
                PixResponse(
                    success = false,
                    valor = valueFormatted,
                    pixCopiaECola = "",
                    subscriptionId = "",
                    isRecurrentPix = true,
                    recurrenceInterval = recurrenceType,
                    errorMessage = detail
                )
            }
        } catch (e: Exception) {
            PixResponse(
                success = false,
                valor = valueFormatted,
                pixCopiaECola = "",
                subscriptionId = "",
                isRecurrentPix = true,
                recurrenceInterval = recurrenceType,
                errorMessage = "Servidor indisponível para gerar PIX real: ${e.localizedMessage ?: "verifique sua conexão"}"
            )
        }
    }

    /**
     * Solicita à VPS a criação de uma cobrança de Assinatura Semanal via Pix Asaas (R$ 25,00)
     */
    suspend fun requestWeeklyPix(phone: String, value: Double = 25.00): PixResponse =
        requestRecurrentPix("PRO_WEEKLY", value, phone)

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

    data class PixStatusResponse(
        val isConfirmed: Boolean,
        val status: String,
        val message: String
    )

    /**
     * Consulta o status em tempo real de liquidação do Pix no gateway / VPS
     */
    suspend fun checkPixStatus(subscriptionId: String): PixStatusResponse = withContext(Dispatchers.IO) {
        try {
            val url = URL("$DEFAULT_SERVER_URL/api/check-pix-status?id=$subscriptionId")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
                connectTimeout = 3000
                readTimeout = 3000
            }
            if (conn.responseCode in 200..299) {
                val text = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                val json = JSONObject(text)
                PixStatusResponse(
                    isConfirmed = json.optBoolean("confirmed", false),
                    status = json.optString("status", "PENDING"),
                    message = json.optString("message", "Aguardando pagamento no banco...")
                )
            } else {
                PixStatusResponse(false, "PENDING", "Aguardando autorização bancária...")
            }
        } catch (e: Exception) {
            PixStatusResponse(false, "PENDING", "Aguardando autorização na rede Pix...")
        }
    }
}
