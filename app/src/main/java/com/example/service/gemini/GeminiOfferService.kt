package com.example.service.gemini

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.coordinator.ActiveOffer
import com.example.coordinator.RadarCoordinator
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Serviço de Inteligência Artificial para consultar a API do Gemini.
 * Envia o JSON detalhado das ofertas multi-app recebidas e telemetria do piloto
 * para que o modelo avalie a melhor oferta ou sugira rotas e sequências ótimas.
 */
object GeminiOfferService {
    private const val TAG = "GeminiOfferService"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }

    private val apiService: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    /**
     * Avalia uma lista de ofertas concorrentes (ou oferta única) enviando os dados em JSON para o Gemini.
     * Retorna um objeto [GeminiOfferEvaluation] estruturado com a melhor decisão e motivos.
     */
    suspend fun evaluateOffersWithGemini(
        offers: List<ActiveOffer>,
        context: Context? = null
    ): GeminiOfferEvaluation? = withContext(Dispatchers.IO) {
        if (offers.isEmpty()) {
            Log.w(TAG, "Nenhuma oferta para avaliar no Gemini.")
            return@withContext null
        }

        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "sua_chave_gemini_aqui") {
            Log.w(TAG, "GEMINI_API_KEY não configurada no .env / BuildConfig.")
            return@withContext fallbackLocalEvaluation(offers)
        }

        val settings = RadarCoordinator.settings.value
        val speed = RadarCoordinator.currentSpeedKmh.value
        val location = RadarCoordinator.currentLocation.value
        val lat = location?.latitude ?: -23.5505
        val lng = location?.longitude ?: -46.6333
        val currentEarnings = RadarCoordinator.activeSessionEarnings.value

        // Constrói o JSON com o payload das ofertas e o contexto operacional do piloto
        val offersPayload = offers.mapIndexed { index, offer ->
            val gainPerKm = if (offer.totalDistance > 0) offer.fareValue / offer.totalDistance else 0.0
            mapOf(
                "id" to "offer_${index + 1}",
                "app" to offer.appName,
                "fare_value_brl" to offer.fareValue,
                "distance_km" to offer.totalDistance,
                "estimated_time_min" to offer.totalTime,
                "pickup_address" to offer.pickupAddress,
                "delivery_address" to offer.deliveryAddress,
                "gain_per_km" to String.format(java.util.Locale.US, "%.2f", gainPerKm)
            )
        }

        val contextPayload = mapOf(
            "driver_name" to "Thiago",
            "current_speed_kmh" to speed,
            "current_location" to mapOf("lat" to lat, "lng" to lng),
            "today_earnings_brl" to currentEarnings,
            "min_fare_setting" to settings.minFareValue,
            "min_gain_per_km_setting" to settings.minValuePerKm,
            "ghost_sequence_active" to settings.isGhostSequenceEnabled,
            "ghost_aggressiveness" to settings.ghostSequenceAggressiveness
        )

        val offersJsonString = moshi.adapter(List::class.java).toJson(offersPayload)
        val contextJsonString = moshi.adapter(Map::class.java).toJson(contextPayload)

        val systemPrompt = """
            Você é o cérebro Jarvis AI do Radar Coordinator, assistente de elite para entregadores no Brasil (iFood, Uber, 99, Rappi).
            Sua missão é analisar as ofertas recebidas em tempo real e decidir a melhor estratégia para maximizar o ganho financeiro por quilômetro rodado (R$/km) e segurança do piloto.
            
            DIRETRIZES DE DECISÃO:
            1. R$/km excelente: >= R$ 5,00/km (Decisão: ACCEPT, Alta confiança).
            2. R$/km aceitável: R$ 3,50 a R$ 4,99/km com distância curta (<= 4 km) (Decisão: ACCEPT).
            3. Rota excessivamente longa (> 8 km) ou valor baixo (< R$ 8,00): (Decisão: DECLINE).
            4. Se houver múltiplas ofertas próximas, sugira se é viável empilhar/encadear (Decisão: CHAIN).
            
            Você DEVE responder EXCLUSIVAMENTE em formato JSON com o seguinte schema:
            {
                "selected_offer_id": "string",
                "selected_app": "string",
                "decision": "ACCEPT" | "DECLINE" | "CHAIN",
                "confidence": 0.0 a 1.0,
                "gain_per_km": number,
                "estimated_hourly_rate": number,
                "reason": "Explicação técnica concisa em português",
                "suggested_voice_announcement": "Frase curta e natural em português para falar ao piloto Thiago no fone de ouvido",
                "optimal_stop_sequence": ["lista de paradas ordenadas"],
                "risk_factors": ["fatores de risco se houver"]
            }
        """.trimIndent()

        val userPrompt = """
            DADOS DAS OFERTAS RECEBIDAS (JSON):
            $offersJsonString
            
            TELEMETRIA E CONTEXTO DO PILOTO (JSON):
            $contextJsonString
            
            Analise e retorne a avaliação estratégica em JSON.
        """.trimIndent()

        val request = GeminiGenerateRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(GeminiPart(text = userPrompt))
                )
            ),
            systemInstruction = GeminiContent(
                parts = listOf(GeminiPart(text = systemPrompt))
            ),
            generationConfig = GeminiGenerationConfig(
                temperature = 0.15f,
                responseMimeType = "application/json"
            )
        )

        try {
            Log.d(TAG, "Enviando ${offers.size} ofertas para avaliação no Gemini 3.5 Flash...")
            val response = apiService.generateContent(apiKey, request)
            val candidateText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

            if (!candidateText.isNullOrBlank()) {
                Log.d(TAG, "Resposta do Gemini recebida com sucesso: $candidateText")
                val evaluationAdapter = moshi.adapter(GeminiOfferEvaluation::class.java)
                val evaluation = evaluationAdapter.fromJson(candidateText)
                
                if (evaluation != null) {
                    RadarCoordinator.addLog(
                        "Jarvis Gemini: Decisão ${evaluation.decision} para ${evaluation.selectedApp ?: "Oferta"} (R$ ${String.format("%.2f", evaluation.gainPerKm)}/km). ${evaluation.reason}",
                        com.example.coordinator.LogType.SUCCESS
                    )
                    return@withContext evaluation
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao consultar API do Gemini: ${e.message}", e)
            RadarCoordinator.addLog("Jarvis Gemini: Falha na conexão de rede (${e.message}). Usando cálculo heurístico local.", com.example.coordinator.LogType.WARNING)
        }

        return@withContext fallbackLocalEvaluation(offers)
    }

    /**
     * Avaliação heurística de fallback local quando a API do Gemini estiver inacessível
     */
    private fun fallbackLocalEvaluation(offers: List<ActiveOffer>): GeminiOfferEvaluation {
        val bestOffer = offers.maxByOrNull { offer ->
            if (offer.totalDistance > 0) offer.fareValue / offer.totalDistance else offer.fareValue / 3.0
        } ?: offers.first()

        val distance = if (bestOffer.totalDistance > 0) bestOffer.totalDistance else 3.5
        val gainPerKm = bestOffer.fareValue / distance
        val decision = if (gainPerKm >= 4.0 || (gainPerKm >= 3.0 && distance <= 4.0)) "ACCEPT" else "DECLINE"
        val confidence = if (decision == "ACCEPT") 0.88 else 0.75

        return GeminiOfferEvaluation(
            selectedOfferId = "offer_1",
            selectedApp = bestOffer.appName,
            decision = decision,
            confidence = confidence,
            gainPerKm = gainPerKm,
            estimatedHourlyRate = (bestOffer.fareValue / 20.0) * 60.0,
            reason = if (decision == "ACCEPT") "Ganho de R$ ${String.format("%.2f", gainPerKm)}/km atende aos critérios do piloto." else "Ganho por km abaixo do piso configurado.",
            suggestedVoiceAnnouncement = if (decision == "ACCEPT") "Thiago, corrida excelente no ${bestOffer.appName}, pagando R$ ${String.format("%.2f", bestOffer.fareValue)} por $distance km." else "Corrida no ${bestOffer.appName} descartada por baixa rentabilidade.",
            optimalStopSequence = listOf(bestOffer.pickupAddress, bestOffer.deliveryAddress),
            riskFactors = emptyList()
        )
    }
}
