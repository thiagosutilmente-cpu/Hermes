package com.example.service.gemini

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Modelos de dados para comunicação com a API REST do Gemini
 */
@JsonClass(generateAdapter = true)
data class GeminiGenerateRequest(
    @Json(name = "contents") val contents: List<GeminiContent>,
    @Json(name = "generationConfig") val generationConfig: GeminiGenerationConfig? = null,
    @Json(name = "systemInstruction") val systemInstruction: GeminiContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    @Json(name = "parts") val parts: List<GeminiPart>,
    @Json(name = "role") val role: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    @Json(name = "text") val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    @Json(name = "temperature") val temperature: Float? = 0.2f,
    @Json(name = "topP") val topP: Float? = 0.95f,
    @Json(name = "topK") val topK: Int? = 40,
    @Json(name = "responseMimeType") val responseMimeType: String? = "application/json"
)

@JsonClass(generateAdapter = true)
data class GeminiGenerateResponse(
    @Json(name = "candidates") val candidates: List<GeminiCandidate>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    @Json(name = "content") val content: GeminiContent? = null
)

/**
 * Modelo estruturado do JSON de decisão retornado pelo Gemini para as ofertas
 */
@JsonClass(generateAdapter = true)
data class GeminiOfferEvaluation(
    @Json(name = "selected_offer_id") val selectedOfferId: String? = null,
    @Json(name = "selected_app") val selectedApp: String? = null,
    @Json(name = "decision") val decision: String = "DECLINE", // "ACCEPT", "DECLINE", "CHAIN"
    @Json(name = "confidence") val confidence: Double = 0.0,
    @Json(name = "gain_per_km") val gainPerKm: Double = 0.0,
    @Json(name = "estimated_hourly_rate") val estimatedHourlyRate: Double = 0.0,
    @Json(name = "reason") val reason: String = "",
    @Json(name = "suggested_voice_announcement") val suggestedVoiceAnnouncement: String = "",
    @Json(name = "optimal_stop_sequence") val optimalStopSequence: List<String> = emptyList(),
    @Json(name = "risk_factors") val riskFactors: List<String> = emptyList()
)
