package com.example

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import java.util.Locale
import java.util.regex.Pattern

/**
 * JARVIS NEURAL COCKPIT
 *
 * Motor de inteligência tática e copiloto em tempo real para entregadores no Brasil
 * (iFood, 99Food, Uber Direct, Rappi).
 *
 * Processa notificações brutas, OCR e eventos de sistema, gerando estritamente um
 * JSON determinístico de altíssima velocidade (< 5ms) com matriz de decisão matemática,
 * alertas vocais táticos (TTS) para o capacete e controle da Missão Embaixador VIP.
 */
object JarvisNeuralCockpitEngine {

    private const val TAG = "JarvisNeuralCockpit"

    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .serializeNulls()
        .create()

    // =========================================================================
    // ESTRUTURA UNIFICADA DO SCHEMA JSON
    // =========================================================================

    data class JarvisResponse(
        @SerializedName("type") val type: String, // "RIDE_ANALYSIS" | "REFERRAL_EVENT" | "SYSTEM_ERROR"
        @SerializedName("status") val status: String, // "SUCCESS" | "INVALID_DATA"
        @SerializedName("error_message") val errorMessage: String?,
        @SerializedName("ride_data") val rideData: JarvisRideData?,
        @SerializedName("referral_data") val referralData: JarvisReferralData?
    )

    data class JarvisRideData(
        @SerializedName("platform") val platform: String, // "iFood" | "Uber Direct" | "99Food" | "Rappi" | "Desconhecido"
        @SerializedName("financials") val financials: JarvisFinancials,
        @SerializedName("decision") val decision: JarvisDecision,
        @SerializedName("voice_alert") val voiceAlert: String
    )

    data class JarvisFinancials(
        @SerializedName("payout_brl") val payoutBrl: Double,
        @SerializedName("total_distance_km") val totalDistanceKm: Double,
        @SerializedName("rate_per_km") val ratePerKm: Double,
        @SerializedName("estimated_time_minutes") val estimatedTimeMinutes: Int,
        @SerializedName("estimated_hourly_rate_brl") val estimatedHourlyRateBrl: Double
    )

    data class JarvisDecision(
        @SerializedName("action") val action: String, // "ACCEPT" | "REJECT" | "BATCH"
        @SerializedName("score_0_to_100") val score: Int,
        @SerializedName("reason") val reason: String
    )

    data class JarvisReferralData(
        @SerializedName("user_name") val userName: String,
        @SerializedName("confirmed_referrals") val confirmedReferrals: Int,
        @SerializedName("target_referrals") val targetReferrals: Int = 5,
        @SerializedName("progress_bar") val progressBar: String,
        @SerializedName("is_annual_vip_unlocked") val isAnnualVipUnlocked: Boolean,
        @SerializedName("message_text") val messageText: String
    )

    // =========================================================================
    // MOTOR DE PROCESSAMENTO DETERMINÍSTICO
    // =========================================================================

    /**
     * Processa a entrada e retorna a estrutura do JARVIS.
     */
    fun processInput(rawText: String): JarvisResponse {
        val trimmed = rawText.trim()
        if (trimmed.isEmpty()) {
            return buildErrorResponse("Nenhum texto de oferta de corrida ou evento de sistema fornecido para processamento.")
        }

        // 1. Checa se é um evento do Programa Missão Embaixador (REFERRAL_EVENT)
        if (isReferralEvent(trimmed)) {
            return processReferralEvent(trimmed)
        }

        // 2. Processa como Análise de Corrida (RIDE_ANALYSIS)
        return processRideAnalysis(trimmed)
    }

    /**
     * Processa e retorna estritamente a string JSON formatada.
     */
    fun processInputToJson(rawText: String): String {
        val response = processInput(rawText)
        return gson.toJson(response)
    }

    /**
     * Processa a notificação, atualiza o HUD e vocaliza no capacete via TTS do Android.
     */
    fun processAndVoice(context: Context, rawText: String): JarvisResponse {
        val response = processInput(rawText)
        if (response.status == "SUCCESS" && response.rideData != null) {
            try {
                OfferTextToSpeechEngine.getInstance(context).speak(
                    response.rideData.voiceAlert,
                    android.speech.tts.TextToSpeech.QUEUE_FLUSH
                )
            } catch (e: Exception) {
                Log.w(TAG, "Falha ao emitir voice_alert do Jarvis via TTS: ${e.message}")
            }
        } else if (response.status == "SUCCESS" && response.referralData != null) {
            try {
                OfferTextToSpeechEngine.getInstance(context).speak(
                    response.referralData.messageText,
                    android.speech.tts.TextToSpeech.QUEUE_FLUSH
                )
            } catch (_: Exception) {}
        }
        return response
    }

    // =========================================================================
    // MODO 1: ANÁLISE DE CORRIDAS (RIDE_ANALYSIS)
    // =========================================================================

    private fun processRideAnalysis(text: String): JarvisResponse {
        val payout = extractPayout(text)
        val distanceKm = extractDistance(text)

        // REGRA DE TRATAMENTO DE ERROS E DADOS INCOMPLETOS:
        // Se o valor pago (R$) ou a distância total (KM) estiverem ausentes ou <= 0.0
        if (payout == null || payout <= 0.0 || distanceKm == null || distanceKm <= 0.0) {
            return buildErrorResponse(
                "Não foi possível identificar o valor em R$ ou a distância total em KM na notificação enviada."
            )
        }

        val platform = detectPlatform(text)

        // FÓRMULAS DE CÁLCULO EXATAS:
        // Taxa por KM = Payout / KM
        val ratePerKm = Math.round((payout / distanceKm) * 100.0) / 100.0

        // Tempo Estimado (Minutos) = (KM / 25 * 60) + 8 (assumindo 25 km/h média urbana + 8 min coleta/entrega)
        val estimatedTimeMinutes = Math.round((distanceKm / 25.0 * 60.0) + 8.0).toInt().coerceAtLeast(8)

        // Ganho Hora Estimado (R$/H) = (Payout / Minutos) * 60
        val estimatedHourlyRate = Math.round((payout / estimatedTimeMinutes * 60.0) * 100.0) / 100.0

        // MATRIZ DE DECISÃO TÁTICA:
        val isBatch = text.contains("batch", ignoreCase = true) ||
                text.contains("dupla", ignoreCase = true) ||
                text.contains("agrupada", ignoreCase = true) ||
                text.contains("duas entregas", ignoreCase = true)

        val isPickupFar = text.contains("coleta", ignoreCase = true) && extractPickupDistance(text)?.let { it > 4.0 } ?: false

        val decisionAction: String
        val decisionScore: Int
        val decisionReason: String
        val voiceAlert: String

        val rateFmt = String.format(Locale.GERMAN, "%.2f", ratePerKm)
        val payoutFmt = String.format(Locale.GERMAN, "%.2f", payout)
        val distFmt = String.format(Locale.GERMAN, "%.1f", distanceKm)
        val hourlyFmt = String.format(Locale.GERMAN, "%.2f", estimatedHourlyRate)

        if (isBatch) {
            decisionAction = "BATCH"
            decisionScore = 88
            decisionReason = "Rota dupla compatível! Duas entregas na mesma direção com ganho otimizado."
            voiceAlert = "Rota dupla parceiro! R$ $payoutFmt na mesma viagem. Pega que compensa!"
        } else if (ratePerKm >= 1.80 && estimatedHourlyRate >= 30.00) {
            decisionAction = "ACCEPT"
            val calculatedScore = 80 + ((ratePerKm - 1.80) * 10.0).toInt() + ((estimatedHourlyRate - 30.0) / 5.0).toInt()
            decisionScore = calculatedScore.coerceIn(80, 99)
            decisionReason = "Excelente taxa de R$ $rateFmt por km e ganho estimado de R$ $hourlyFmt por hora."
            voiceAlert = if (distanceKm <= 5.0) {
                "Corrida top parceiro! R$ $payoutFmt por $distFmt km. Pega rápido!"
            } else {
                "Corrida boa! R$ $payoutFmt rendendo R$ $rateFmt o km. Pode aceitar."
            }
        } else if (ratePerKm < 1.50 || isPickupFar) {
            decisionAction = "REJECT"
            val calculatedScore = (ratePerKm * 15.0).toInt()
            decisionScore = calculatedScore.coerceIn(5, 35)
            decisionReason = if (isPickupFar) {
                "Coleta excessiva superior a 4,0 km sem taxa compensatória."
            } else {
                "Corrida lixo! R$ $rateFmt por km gera prejuízo direto de combustível."
            }
            voiceAlert = "Fora de cogitação irmão! Apenas R$ $rateFmt o quilômetro. Rejeita isso."
        } else {
            // Faixa intermediária (R$ 1,50 a R$ 1,79/km ou R$/h < 30) -> Regra de corte rigorosa
            decisionAction = "REJECT"
            decisionScore = 45
            decisionReason = "Taxa de R$ $rateFmt por km e R$ $hourlyFmt/h abaixo da meta de corte (mínimo R$ 1,80/km e R$ 30/h)."
            voiceAlert = "Abaixo da meta! R$ $rateFmt o km. Melhor esperar outra oportunidade."
        }

        val rideData = JarvisRideData(
            platform = platform,
            financials = JarvisFinancials(
                payoutBrl = payout,
                totalDistanceKm = distanceKm,
                ratePerKm = ratePerKm,
                estimatedTimeMinutes = estimatedTimeMinutes,
                estimatedHourlyRateBrl = estimatedHourlyRate
            ),
            decision = JarvisDecision(
                action = decisionAction,
                score_0_to_100 = decisionScore,
                reason = decisionReason
            ),
            voiceAlert = voiceAlert
        )

        return JarvisResponse(
            type = "RIDE_ANALYSIS",
            status = "SUCCESS",
            errorMessage = null,
            rideData = rideData,
            referralData = null
        )
    }

    // =========================================================================
    // MODO 2: PROGRAMA MISSÃO EMBAIXADOR (REFERRAL_EVENT)
    // =========================================================================

    private fun isReferralEvent(text: String): Boolean {
        return text.contains("REFERRAL", ignoreCase = true) ||
                text.contains("EMBAIXADOR", ignoreCase = true) ||
                text.contains("META_5", ignoreCase = true) ||
                text.contains("Confirmados:", ignoreCase = true) ||
                text.contains("AmigoPago", ignoreCase = true)
    }

    private fun processReferralEvent(text: String): JarvisResponse {
        val userName = extractRegex(text, """(?:User|Piloto|Usuario|Nome):\s*([A-Za-zÀ-ÿ]+)""") ?: "Thiago"
        val confirmedRaw = extractRegex(text, """(?:Confirmados|Indicados|Ativos):\s*(\d+)""")?.toIntOrNull() ?: 1
        val friendName = extractRegex(text, """(?:AmigoPago|Amigo|Indicado):\s*([A-Za-zÀ-ÿ\s]+?)(?:[|\]\n]|$)""")?.trim() ?: "Novo Parceiro"

        val confirmed = confirmedRaw.coerceIn(0, 5)
        val progressBar = when (confirmed) {
            0 -> "[░░░░░]"
            1 -> "[▓░░░░]"
            2 -> "[▓▓░░░]"
            3 -> "[▓▓▓░░]"
            4 -> "[▓▓▓▓░]"
            else -> "[▓▓▓▓▓]"
        }

        val isUnlocked = (confirmed >= 5)
        val remaining = (5 - confirmed).coerceAtLeast(0)

        val messageText = if (isUnlocked) {
            "PARABÉNS, $userName! $friendName confirmou. Placar: [▓▓▓▓▓] 5/5! Meta batida! Seu PLANO ANUAL VIP de R$ 650,00 foi DESBLOQUEADO GRATUITAMENTE por 365 dias!"
        } else {
            "Aí sim, $userName! $friendName pagou a 1ª semana. Placar: $progressBar $confirmed/5. Falta só $remaining motoboy pra você liberar o Plano Anual VIP grátis por 1 ano!"
        }

        val referralData = JarvisReferralData(
            userName = userName,
            confirmedReferrals = confirmed,
            targetReferrals = 5,
            progressBar = progressBar,
            isAnnualVipUnlocked = isUnlocked,
            messageText = messageText
        )

        return JarvisResponse(
            type = "REFERRAL_EVENT",
            status = "SUCCESS",
            errorMessage = null,
            rideData = null,
            referralData = referralData
        )
    }

    // =========================================================================
    // EXTRATORES DETERMINÍSTICOS (REGEX ROBUSTO EM TEMPO REAL)
    // =========================================================================

    private fun detectPlatform(text: String): String {
        val lower = text.lowercase()
        return when {
            lower.contains("ifood") -> "iFood"
            lower.contains("uber") -> "Uber Direct"
            lower.contains("99") || lower.contains("99food") -> "99Food"
            lower.contains("rappi") -> "Rappi"
            else -> "Desconhecido"
        }
    }

    private fun extractPayout(text: String): Double? {
        // Padrões como: "R$ 19,50", "R$ 19.50", "R$19,50", "19,50" precedido por cifrão ou ganho
        val pattern = Pattern.compile("""(?:R\$|r\$|\$)\s*(\d+[.,]\d{2})""")
        val matcher = pattern.matcher(text)
        if (matcher.find()) {
            val numStr = matcher.group(1)?.replace(',', '.') ?: return null
            return numStr.toDoubleOrNull()
        }

        // Fallback: "recebido 19,50" ou "valor: 19.50"
        val fallback = Pattern.compile("""(?:valor|recebido|pago|tarifa|total)\s*[:=]?\s*(?:R\$)?\s*(\d+[.,]\d{2})""", Pattern.CASE_INSENSITIVE)
        val m2 = fallback.matcher(text)
        if (m2.find()) {
            val numStr = m2.group(1)?.replace(',', '.') ?: return null
            return numStr.toDoubleOrNull()
        }

        return null
    }

    private fun extractDistance(text: String): Double? {
        // Padrões como: "4.5 km", "4,5km", "Distância: 7.8 km"
        val pattern = Pattern.compile("""(\d+[.,]?\d*)\s*(?:km|KM|Km)\b""")
        val matcher = pattern.matcher(text)
        var lastFound: Double? = null
        while (matcher.find()) {
            val str = matcher.group(1)?.replace(',', '.') ?: continue
            val dist = str.toDoubleOrNull()
            if (dist != null && dist > 0.0) {
                lastFound = dist
            }
        }
        return lastFound
    }

    private fun extractPickupDistance(text: String): Double? {
        val pattern = Pattern.compile("""(?:coleta|ate o restaurante|buscar)\s*[:=]?\s*(\d+[.,]?\d*)\s*(?:km|KM|Km)""", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(text)
        if (matcher.find()) {
            val str = matcher.group(1)?.replace(',', '.') ?: return null
            return str.toDoubleOrNull()
        }
        return null
    }

    private fun extractRegex(text: String, regexPattern: String): String? {
        val pattern = Pattern.compile(regexPattern, Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(text)
        return if (matcher.find()) {
            matcher.group(1)?.trim()
        } else null
    }

    private fun buildErrorResponse(message: String): JarvisResponse {
        return JarvisResponse(
            type = "SYSTEM_ERROR",
            status = "INVALID_DATA",
            errorMessage = message,
            rideData = null,
            referralData = null
        )
    }
}
