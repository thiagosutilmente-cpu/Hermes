package com.example

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.util.Locale

/**
 * Modelo de dados estruturado do JSON de Decisão Tática do Jarvis Neural Cockpit
 */
data class JarvisDecisionJson(
    @SerializedName("status")
    val status: String, // "SUCCESS" ou "INVALID_DATA"

    @SerializedName("error_message")
    val errorMessage: String? = null,

    @SerializedName("data")
    val data: JarvisDecisionData? = null
) {
    data class JarvisDecisionData(
        @SerializedName("platform")
        val platform: String = "iFood",

        @SerializedName("financials")
        val financials: JarvisFinancials? = null,

        @SerializedName("decision")
        val decision: JarvisDecisionDetails? = null,

        @SerializedName("voice_alert")
        val voiceAlert: String = ""
    )

    data class JarvisFinancials(
        @SerializedName("payout_brl")
        val payoutBrl: Double = 0.0,

        @SerializedName("total_distance_km")
        val totalDistanceKm: Double = 0.0,

        @SerializedName("rate_per_km")
        val ratePerKm: Double = 0.0,

        @SerializedName("estimated_time_minutes")
        val estimatedTimeMinutes: Int = 0,

        @SerializedName("estimated_hourly_rate_brl")
        val estimatedHourlyRateBrl: Double = 0.0
    )

    data class JarvisDecisionDetails(
        @SerializedName("action")
        val action: String = "ACCEPT", // "ACCEPT", "REJECT", "BATCH"

        @SerializedName("score_0_to_100")
        val score0To100: Int = 85,

        @SerializedName("reason")
        val reason: String = ""
    )
}

/**
 * JarvisVoiceAlertManager
 *
 * Responsável por:
 * 1. Processar e analisar notificações ou JSONs brutos da IA/Jarvis.
 * 2. Extrair a propriedade 'voice_alert'.
 * 3. Utilizar o motor nativo Android TextToSpeech ([OfferTextToSpeechEngine]) para falar a recomendação
 *    direto no fone Bluetooth ou capacete do motoboy sem que ele precise desviar o olhar do trânsito.
 */
object JarvisVoiceAlertManager {

    private const val TAG = "JarvisVoiceAlertMgr"
    private val gson = Gson()

    private val _lastProcessedAlert = MutableStateFlow<JarvisDecisionJson?>(null)
    val lastProcessedAlert: StateFlow<JarvisDecisionJson?> = _lastProcessedAlert.asStateFlow()

    private val _lastSpokenVoiceAlert = MutableStateFlow("")
    val lastSpokenVoiceAlert: StateFlow<String> = _lastSpokenVoiceAlert.asStateFlow()

    /**
     * Converte a string 'voice_alert' contida no JSON de decisão em áudio falado via Android TTS
     *
     * @param context Contexto do Android para acessar o TTS Engine
     * @param jsonText String JSON bruta retornada pelo modelo ou backend
     * @return true se o voice_alert foi extraído e enviado para reprodução no áudio
     */
    fun speakVoiceAlertFromJson(context: Context, jsonText: String): Boolean {
        if (jsonText.isBlank()) {
            Log.w(TAG, "JSON vazio fornecido para speakVoiceAlertFromJson")
            return false
        }

        try {
            // Tenta decodificar via Gson
            val parsedObj = parseDecisionJson(jsonText)
            _lastProcessedAlert.value = parsedObj

            val voiceAlertText = parsedObj?.data?.voiceAlert?.trim()

            if (!voiceAlertText.isNullOrEmpty()) {
                _lastSpokenVoiceAlert.value = voiceAlertText
                // Reproduz via Android TextToSpeech Engine
                OfferTextToSpeechEngine.getInstance(context).speakVoiceAlert(voiceAlertText)
                Log.i(TAG, "Alerta de voz reproduzido via TTS: \"$voiceAlertText\"")
                return true
            }

            // Fallback: Tenta extração direta via JSONObject caso haja formatação irregular
            val json = JSONObject(jsonText)
            val dataObj = json.optJSONObject("data")
            val fallbackVoiceAlert = dataObj?.optString("voice_alert", "")?.trim()
                ?: json.optString("voice_alert", "").trim()

            if (fallbackVoiceAlert.isNotEmpty()) {
                _lastSpokenVoiceAlert.value = fallbackVoiceAlert
                OfferTextToSpeechEngine.getInstance(context).speakVoiceAlert(fallbackVoiceAlert)
                Log.i(TAG, "Alerta de voz (fallback JSON) reproduzido via TTS: \"$fallbackVoiceAlert\"")
                return true
            }

            Log.w(TAG, "Nenhum campo 'voice_alert' válido encontrado no JSON")
            return false

        } catch (e: Exception) {
            Log.e(TAG, "Falha ao processar JSON para extração de voice_alert: ${e.message}", e)
            return false
        }
    }

    /**
     * Converte e fala diretamente uma string de voice_alert conhecida
     */
    fun speakVoiceAlertDirectly(context: Context, voiceAlert: String) {
        if (voiceAlert.isBlank()) return
        _lastSpokenVoiceAlert.value = voiceAlert
        OfferTextToSpeechEngine.getInstance(context).speakVoiceAlert(voiceAlert)
    }

    /**
     * Gera um JSON estruturado de decisão a partir dos dados de uma corrida real e reproduz o voice_alert
     */
    fun evaluateAndSpeakOffer(
        context: Context,
        platform: String,
        payoutBrl: Double,
        distanceKm: Double,
        isBatched: Boolean = false
    ): JarvisDecisionJson {
        if (payoutBrl <= 0.0 || distanceKm <= 0.0) {
            val invalidJson = JarvisDecisionJson(
                status = "INVALID_DATA",
                errorMessage = "Valor financeiro ou distância zerados ou não detectados na notificação.",
                data = null
            )
            _lastProcessedAlert.value = invalidJson
            return invalidJson
        }

        val ratePerKm = payoutBrl / distanceKm
        val estimatedTimeMin = ((distanceKm / 25.0 * 60.0) + 8.0).toInt().coerceAtLeast(10)
        val estimatedHourlyRate = (payoutBrl / estimatedTimeMin.toDouble()) * 60.0

        val (action, score, reason, voiceAlert) = when {
            isBatched -> {
                val alert = "Rota dupla! Adicional de R$ ${formatMoney(payoutBrl)} por ${formatKm(distanceKm)} km. Renda extra garantida."
                DecisionTuple("BATCH", 90, "Corrida no mesmo eixo com baixo desvio de rota.", alert)
            }
            ratePerKm >= 1.80 && estimatedHourlyRate >= 30.0 -> {
                val scoreCalc = (80 + ((ratePerKm - 1.8) * 10).toInt()).coerceIn(80, 100)
                val alert = "Corrida top! R$ ${formatMoney(payoutBrl)} por ${formatKm(distanceKm)} km. R$ ${formatMoney(ratePerKm)} o km. Pode pegar."
                DecisionTuple("ACCEPT", scoreCalc, "Taxa de R$ ${formatMoney(ratePerKm)}/km e R$ ${formatMoney(estimatedHourlyRate)}/h compensam.", alert)
            }
            ratePerKm < 1.50 -> {
                val scoreCalc = (30 - ((1.5 - ratePerKm) * 20).toInt()).coerceIn(5, 35)
                val alert = "Atenção: prejuízo. Apenas R$ ${formatMoney(ratePerKm)} o km. Recomendo rejeitar."
                DecisionTuple("REJECT", scoreCalc, "Valor por km inferior a R$ 1,50/km gera prejuízo com combustível.", alert)
            }
            else -> {
                val alert = "Alerta: R$ ${formatMoney(payoutBrl)} por ${formatKm(distanceKm)} km. R$ ${formatMoney(ratePerKm)} o km. Avalie o trânsito."
                DecisionTuple("REJECT", 45, "Margem limítrofe entre custos operacionais e tempo estimado.", alert)
            }
        }

        val decisionObj = JarvisDecisionJson(
            status = "SUCCESS",
            errorMessage = null,
            data = JarvisDecisionJson.JarvisDecisionData(
                platform = platform,
                financials = JarvisDecisionJson.JarvisFinancials(
                    payoutBrl = Math.round(payoutBrl * 100.0) / 100.0,
                    totalDistanceKm = Math.round(distanceKm * 100.0) / 100.0,
                    ratePerKm = Math.round(ratePerKm * 100.0) / 100.0,
                    estimatedTimeMinutes = estimatedTimeMin,
                    estimatedHourlyRateBrl = Math.round(estimatedHourlyRate * 100.0) / 100.0
                ),
                decision = JarvisDecisionJson.JarvisDecisionDetails(
                    action = action,
                    score_0_to_100 = score,
                    reason = reason
                ),
                voice_alert = voiceAlert
            )
        )

        _lastProcessedAlert.value = decisionObj
        _lastSpokenVoiceAlert.value = voiceAlert

        // Reproduz imediatamente via TTS nativo
        OfferTextToSpeechEngine.getInstance(context).speakVoiceAlert(voiceAlert)

        return decisionObj
    }

    private fun parseDecisionJson(jsonText: String): JarvisDecisionJson? {
        return try {
            gson.fromJson(jsonText, JarvisDecisionJson::class.java)
        } catch (_: Exception) {
            null
        }
    }

    private fun formatMoney(value: Double): String {
        return String.format(Locale("pt", "BR"), "%.2f", value).replace(".", ",")
    }

    private fun formatKm(value: Double): String {
        return String.format(Locale("pt", "BR"), "%.1f", value).replace(".", ",")
    }

    private data class DecisionTuple(
        val action: String,
        val score: Int,
        val reason: String,
        val voiceAlert: String
    )
}
