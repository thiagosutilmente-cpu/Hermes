package com.example.network

import com.google.gson.annotations.SerializedName

/**
 * Modelos de dados para envio e recebimento de informações com o servidor VPS na Hostinger.
 */

/**
 * Payload de envio de decisão de oferta (Aceita ou Recusada)
 */
data class OfferDecisionRequest(
    @SerializedName("eventType")
    val eventType: String = "OFFER_DECISION",

    @SerializedName("offerId")
    val offerId: String,

    @SerializedName("action")
    val action: String, // "ACCEPTED" ou "DECLINED"

    @SerializedName("appName")
    val appName: String,

    @SerializedName("restaurant")
    val restaurant: String,

    @SerializedName("value")
    val value: Double,

    @SerializedName("distanceKm")
    val distanceKm: Double,

    @SerializedName("gainPerKm")
    val gainPerKm: Double,

    @SerializedName("source")
    val source: String, // "Toque na Tela", "Comando de Voz", "HUD Flutuante", etc.

    @SerializedName("reason")
    val reason: String = "",

    @SerializedName("deliveryAddress")
    val deliveryAddress: String = "",

    @SerializedName("deviceId")
    val deviceId: String = android.os.Build.MODEL,

    @SerializedName("androidVersion")
    val androidVersion: String = android.os.Build.VERSION.RELEASE,

    @SerializedName("timestamp")
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Resposta padrão do servidor VPS para operações de decisão de oferta
 */
data class ApiResponse<T>(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String,

    @SerializedName("data")
    val data: T? = null,

    @SerializedName("timestamp")
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Resposta detalhada do registro da oferta no banco de dados da VPS
 */
data class OfferDecisionResponse(
    @SerializedName("receiptId")
    val receiptId: String,

    @SerializedName("status")
    val status: String,

    @SerializedName("savedAt")
    val savedAt: Long,

    @SerializedName("voice_alert")
    val voiceAlert: String? = null,

    @SerializedName("decision")
    val decision: String? = null
)

/**
 * Payload para registro de comandos de voz (STT)
 */
data class VoiceCommandRequest(
    @SerializedName("eventType")
    val eventType: String = "VOICE_COMMAND",

    @SerializedName("command")
    val command: String,

    @SerializedName("spokenText")
    val spokenText: String,

    @SerializedName("actionTaken")
    val actionTaken: String,

    @SerializedName("confidence")
    val confidence: Float = 0.95f,

    @SerializedName("micSource")
    val micSource: String = "BLUETOOTH_HELMET_OR_PHONE",

    @SerializedName("timestamp")
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Resposta de status e teste de conexão da VPS
 */
data class PingResponse(
    @SerializedName("status")
    val status: String,

    @SerializedName("serverTime")
    val serverTime: Long,

    @SerializedName("version")
    val version: String? = null
)
