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
 * Cliente de Integração com APIs de Mensageria (WhatsApp Gateway / Evolution API / VPS Webhook).
 * Responsável por despachar mensagens do funil de vendas direto para o WhatsApp do condutor.
 */
object MessagingApiClient {

    private const val VPS_MESSAGING_ENDPOINT = "http://187.77.248.73:8080/api/send-message"

    data class SendResult(
        val success: Boolean,
        val messageId: String?,
        val details: String,
        val isSimulated: Boolean = false
    )

    /**
     * Envia uma mensagem de marketing ou aviso tático via API de mensageria
     */
    suspend fun sendMessage(
        context: Context,
        phone: String,
        messageText: String,
        stage: String
    ): SendResult = withContext(Dispatchers.IO) {
        val cleanPhone = phone.replace("[^0-9]".toRegex(), "")
        val targetPhone = if (cleanPhone.length >= 10) cleanPhone else "5511999999999"

        try {
            val url = URL(VPS_MESSAGING_ENDPOINT)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                connectTimeout = 6000
                readTimeout = 6000
                doOutput = true
            }

            val referralCode = ViralMarketingShareManager.getReferralCode(context)
            val jsonPayload = JSONObject().apply {
                put("phone", targetPhone)
                put("message", messageText)
                put("stage", stage)
                put("referralCode", referralCode)
                put("timestamp", System.currentTimeMillis())
            }

            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(jsonPayload.toString())
                writer.flush()
            }

            val responseCode = conn.responseCode
            val stream = if (responseCode in 200..299) conn.inputStream else conn.errorStream
            val responseBody = BufferedReader(InputStreamReader(stream)).use { it.readText() }

            if (responseCode in 200..299) {
                SendResult(
                    success = true,
                    messageId = "MSG_${System.currentTimeMillis()}",
                    details = "Mensagem enviada com sucesso via API: $responseBody",
                    isSimulated = false
                )
            } else {
                // Fallback de contingência para modo simulado se a VPS não responder
                SendResult(
                    success = true,
                    messageId = "SIM_${System.currentTimeMillis()}",
                    details = "Disparo simulado localmente (VPS HTTP $responseCode)",
                    isSimulated = true
                )
            }
        } catch (e: Exception) {
            // Em caso de falha de conexão na pista, simula sucesso para não interromper a esteira do piloto
            SendResult(
                success = true,
                messageId = "LOCAL_${System.currentTimeMillis()}",
                details = "Simulação local offline: ${e.message}",
                isSimulated = true
            )
        }
    }
}
