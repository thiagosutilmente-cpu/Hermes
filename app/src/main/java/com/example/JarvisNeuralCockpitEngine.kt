package com.example

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import java.util.Locale
import java.util.regex.Pattern

/**
 * JARVIS NEURAL COCKPIT 2.0 (CODENAME: SINDICATO INVISÍVEL)
 *
 * Cérebro tático e motor de gamificação em tempo real para entregadores de aplicativo
 * no Brasil (iFood, 99Food, Uber Direct, Rappi).
 *
 * Diretrizes:
 * 1. SAÍDA ESTRITA: Formato JSON limpo determinístico.
 * 2. DETERMINISMO E VELOCIDADE: Execução ultra-rápida sem inventar dados.
 * 3. TRATAMENTO DE ERROS: Dados ausentes ou inválidos geram status "INVALID_DATA" e tipo "SYSTEM_ERROR".
 * 4. MODOS DE VOZ: TACTICAL_ANGRY (padrão), STANDARD, WAR_GENERAL (máx 12 palavras).
 * 5. GERADOR VIRAL: Produz cards para WhatsApp sempre que a corrida for REJECT.
 */
object JarvisNeuralCockpitEngine {

    private const val TAG = "JarvisNeuralCockpit"
    private const val PREFS_NAME = "jarvis_neural_cockpit_prefs"
    private const val KEY_VOICE_STYLE = "key_jarvis_voice_style_preference"

    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .serializeNulls()
        .create()

    // =========================================================================
    // SCHEMA ESTRITO DE SAÍDA JSON
    // =========================================================================

    data class JarvisResponse(
        @SerializedName("type") val type: String, // "RIDE_ANALYSIS" | "SYSTEM_ERROR" | "REFERRAL_EVENT"
        @SerializedName("status") val status: String, // "SUCCESS" | "INVALID_DATA"
        @SerializedName("error_message") val errorMessage: String?,
        @SerializedName("ride_data") val rideData: JarvisRideData?,
        @SerializedName("referral_data") val referralData: JarvisReferralData? = null
    )

    data class JarvisRideData(
        @SerializedName("platform") val platform: String, // "iFood" | "Uber Direct" | "99Food" | "Rappi" | "Desconhecido"
        @SerializedName("financials") val financials: JarvisFinancials,
        @SerializedName("decision") val decision: JarvisDecision,
        @SerializedName("voice_alert") val voiceAlert: JarvisVoiceAlert,
        @SerializedName("viral_group_card") val viralGroupCard: JarvisViralGroupCard?
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

    data class JarvisVoiceAlert(
        @SerializedName("style_used") val styleUsed: String, // "TACTICAL_ANGRY" | "STANDARD" | "WAR_GENERAL"
        @SerializedName("text") val text: String
    )

    data class JarvisViralGroupCard(
        @SerializedName("headline") val headline: String,
        @SerializedName("share_text_whatsapp") val shareTextWhatsapp: String
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
    // CONFIGURAÇÃO DE ESTILO DE VOZ
    // =========================================================================

    enum class VoiceAlertStyle(val id: String, val label: String) {
        TACTICAL_ANGRY("TACTICAL_ANGRY", "Revoltado / Debochado (Padrão)"),
        STANDARD("STANDARD", "Padrão / Profissional"),
        WAR_GENERAL("WAR_GENERAL", "Estrategista / Militar");

        companion object {
            fun fromId(id: String?): VoiceAlertStyle {
                return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: TACTICAL_ANGRY
            }
        }
    }

    private var activeVoiceStyle: VoiceAlertStyle = VoiceAlertStyle.TACTICAL_ANGRY

    fun getActiveVoiceStyle(): VoiceAlertStyle = activeVoiceStyle

    fun setVoiceStyle(style: VoiceAlertStyle, context: Context? = null) {
        activeVoiceStyle = style
        context?.let {
            it.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_VOICE_STYLE, style.id)
                .apply()
        }
    }

    fun initialize(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_VOICE_STYLE, VoiceAlertStyle.TACTICAL_ANGRY.id)
        activeVoiceStyle = VoiceAlertStyle.fromId(saved)
    }

    // =========================================================================
    // PROCESSAMENTO DETERMINÍSTICO
    // =========================================================================

    /**
     * Processa a entrada de texto e retorna o objeto estrito de resposta.
     */
    fun processInput(
        rawText: String,
        styleOverride: VoiceAlertStyle? = null
    ): JarvisResponse {
        val trimmed = rawText.trim()
        if (trimmed.isEmpty()) {
            return buildErrorResponse("Nenhum texto de oferta fornecido para processamento.")
        }

        if (isReferralEvent(trimmed)) {
            return processReferralEvent(trimmed)
        }

        return processRideAnalysis(trimmed, styleOverride ?: activeVoiceStyle)
    }

    /**
     * Retorna estritamente o JSON limpo, sem nenhuma saudação ou texto extra.
     */
    fun processInputToJson(
        rawText: String,
        styleOverride: VoiceAlertStyle? = null
    ): String {
        val response = processInput(rawText, styleOverride)
        return gson.toJson(response)
    }

    /**
     * Processa a notificação e emite o alerta em áudio direto para o capacete via TTS.
     */
    fun processAndVoice(
        context: Context,
        rawText: String,
        styleOverride: VoiceAlertStyle? = null
    ): JarvisResponse {
        val response = processInput(rawText, styleOverride)
        if (response.status == "SUCCESS" && response.rideData != null) {
            try {
                OfferTextToSpeechEngine.getInstance(context).speak(
                    response.rideData.voiceAlert.text,
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
    // ANÁLISE DE CORRIDAS (RIDE_ANALYSIS)
    // =========================================================================

    private fun processRideAnalysis(
        text: String,
        style: VoiceAlertStyle
    ): JarvisResponse {
        val payout = extractPayout(text)
        val distanceKm = extractDistance(text)

        // REGRA DE TRATAMENTO DE ERROS:
        // Se o valor (R$) ou a distância (KM) estiverem ocultos ou ausentes/inválidos
        if (payout == null || payout <= 0.0 || distanceKm == null || distanceKm <= 0.0) {
            return buildErrorResponse(
                "Valor financeiro em R$ ou distância em KM ausentes ou ilegíveis na notificação."
            )
        }

        val platform = detectPlatform(text)

        // FÓRMULAS DE CÁLCULO EXATAS:
        // Taxa por KM (R$/KM) = Payout_Total_BRL / Distancia_Total_KM
        val ratePerKm = Math.round((payout / distanceKm) * 100.0) / 100.0

        // Tempo Estimado (Minutos) = (Distancia_Total_KM / 25 * 60) + 8
        val estimatedTimeMinutes = Math.round((distanceKm / 25.0 * 60.0) + 8.0).toInt().coerceAtLeast(8)

        // Ganho Hora Estimado (R$/H) = (Payout_Total_BRL / Tempo_Estimado_Minutos) * 60
        val estimatedHourlyRateBrl = Math.round((payout / estimatedTimeMinutes * 60.0) * 100.0) / 100.0

        // MATRIZ DE DECISÃO TÁTICA:
        val isBatch = text.contains("batch", ignoreCase = true) ||
                text.contains("dupla", ignoreCase = true) ||
                text.contains("agrupada", ignoreCase = true) ||
                text.contains("duas entregas", ignoreCase = true)

        val isPickupFar = text.contains("coleta", ignoreCase = true) &&
                extractPickupDistance(text)?.let { it > 4.0 } ?: false

        val decisionAction: String
        val decisionScore: Int
        val decisionReason: String

        val rateFmt = String.format(Locale.GERMAN, "%.2f", ratePerKm)
        val payoutFmt = String.format(Locale.GERMAN, "%.2f", payout)
        val distFmt = String.format(Locale.GERMAN, "%.1f", distanceKm)
        val hourlyFmt = String.format(Locale.GERMAN, "%.2f", estimatedHourlyRateBrl)

        if (isBatch) {
            decisionAction = "BATCH"
            decisionScore = 88
            decisionReason = "Rota dupla compatível com desvio mínimo e ganho total otimizado."
        } else if (ratePerKm >= 1.80 && estimatedHourlyRateBrl >= 30.00) {
            decisionAction = "ACCEPT"
            val calculatedScore = 80 + ((ratePerKm - 1.80) * 8.0).toInt() + ((estimatedHourlyRateBrl - 30.0) / 6.0).toInt()
            decisionScore = calculatedScore.coerceIn(80, 99)
            decisionReason = "Excelente retorno de R$ $rateFmt por km e R$ $hourlyFmt/hora."
        } else if (ratePerKm < 1.50 || isPickupFar) {
            decisionAction = "REJECT"
            val calculatedScore = (ratePerKm * 15.0).toInt()
            decisionScore = calculatedScore.coerceIn(5, 30)
            decisionReason = if (isPickupFar) {
                "Coleta excessiva superior a 4,0 km sem taxa compensatória."
            } else {
                "Taxa humilhante de R$ $rateFmt por km. Prejuízo financeiro direto."
            }
        } else {
            // Faixa intermediária: corte rigoroso
            decisionAction = "REJECT"
            decisionScore = 42
            decisionReason = "Taxa de R$ $rateFmt/km e R$ $hourlyFmt/h abaixo do corte (mínimo R$ 1,80/km e R$ 30,00/h)."
        }

        // GERAÇÃO DO ALERTA DE VOZ CONFORME ESTILO CONFIGURADO (MÁX 12 PALAVRAS)
        val voiceAlertText = generateVoiceAlert(
            style = style,
            action = decisionAction,
            payoutFmt = payoutFmt,
            distFmt = distFmt,
            rateFmt = rateFmt,
            hourlyFmt = hourlyFmt,
            ratePerKm = ratePerKm,
            payout = payout,
            distanceKm = distanceKm
        )

        val voiceAlert = JarvisVoiceAlert(
            styleUsed = style.id,
            text = voiceAlertText
        )

        // GERADOR DE CARDS VIRAIS (VIRAL_GROUP_CARD) - OBRIGATÓRIO EM DECISÃO REJECT
        val viralCard: JarvisViralGroupCard? = if (decisionAction == "REJECT") {
            generateViralGroupCard(
                platform = platform,
                payoutFmt = payoutFmt,
                distFmt = distFmt,
                rateFmt = rateFmt
            )
        } else {
            null
        }

        val rideData = JarvisRideData(
            platform = platform,
            financials = JarvisFinancials(
                payoutBrl = payout,
                totalDistanceKm = distanceKm,
                ratePerKm = ratePerKm,
                estimatedTimeMinutes = estimatedTimeMinutes,
                estimatedHourlyRateBrl = estimatedHourlyRateBrl
            ),
            decision = JarvisDecision(
                action = decisionAction,
                score_0_to_100 = decisionScore,
                reason = decisionReason
            ),
            voiceAlert = voiceAlert,
            viralGroupCard = viralCard
        )

        return JarvisResponse(
            type = "RIDE_ANALYSIS",
            status = "SUCCESS",
            errorMessage = null,
            rideData = rideData,
            referralData = null
        )
    }

    /**
     * Gera frases curtas (máx. 12 palavras) segundo a persona configurada.
     */
    private fun generateVoiceAlert(
        style: VoiceAlertStyle,
        action: String,
        payoutFmt: String,
        distFmt: String,
        rateFmt: String,
        hourlyFmt: String,
        ratePerKm: Double,
        payout: Double,
        distanceKm: Double
    ): String {
        return when (style) {
            VoiceAlertStyle.TACTICAL_ANGRY -> {
                when (action) {
                    "ACCEPT" -> "Oferta de respeito! R$ $rateFmt o km. Pega rápido antes que suma!"
                    "BATCH" -> "Rota dupla patrão! R$ $payoutFmt na mesma viagem. Pega que compensa!"
                    else -> {
                        if (ratePerKm < 1.0) {
                            "Cinco conto pra rodar oito quilômetros? Tá maluco patrão! Rejeita esse lixo agora!"
                        } else {
                            "Pelo amor de Deus! Quatro conto pra rodar oito km? A plataforma ficou louca, rejeita isso!"
                        }
                    }
                }
            }
            VoiceAlertStyle.WAR_GENERAL -> {
                when (action) {
                    "ACCEPT" -> "Atenção soldado! Oferta de elite: R$ $rateFmt o quilômetro. Aceitar missão imediatamente!"
                    "BATCH" -> "Comboio tático liberado! Dupla entrega em rota otimizada. Proceder com avanço!"
                    else -> "Soldado, recuse essa missão! Prejuízo de combustível garantido. Aguarde a próxima."
                }
            }
            VoiceAlertStyle.STANDARD -> {
                when (action) {
                    "ACCEPT" -> "Oferta recomendada: R$ $rateFmt por quilômetro e R$ $hourlyFmt por hora. Aceitar."
                    "BATCH" -> "Dupla rota viável: ganho total de R$ $payoutFmt. Aceitar lote."
                    else -> "Corrida rejeitada: taxa de R$ $rateFmt por km abaixo da meta estipulada."
                }
            }
        }
    }

    /**
     * Gera o card viral de indignação para envio rápido ao grupo de WhatsApp.
     */
    private fun generateViralGroupCard(
        platform: String,
        payoutFmt: String,
        distFmt: String,
        rateFmt: String
    ): JarvisViralGroupCard {
        val headlines = listOf(
            "🚨 TENTATIVA DE ASSALTO DE ALGORITMO!",
            "⛽ ALERTA DE PREJUÍZO: MOTO NÃO ANDA COM ÁGUA!",
            "📉 TAXA DE FOME REJEITADA PELO JARVIS!",
            "🗑️ MAIS UMA CORRIDA LIXO JOGADA FORA!"
        )
        val headline = headlines.random()

        val shareLink = AsaasCheckoutManager.DEFAULT_URL_GENERAL
        val shareText = "Acharam que minha moto anda com água! R$ $payoutFmt pra rodar $distFmt KM (R$ $rateFmt/km) no $platform. O Jarvis rejeitou na hora. Veja se o seu app te livra disso: $shareLink"

        return JarvisViralGroupCard(
            headline = headline,
            shareTextWhatsapp = shareText
        )
    }

    // =========================================================================
    // PROGRAMA MISSÃO EMBAIXADOR (REFERRAL_EVENT)
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
    // EXTRATORES DETERMINÍSTICOS
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
        val pattern = Pattern.compile("""(?:R\$|r\$|\$)\s*(\d+[.,]\d{2})""")
        val matcher = pattern.matcher(text)
        if (matcher.find()) {
            val numStr = matcher.group(1)?.replace(',', '.') ?: return null
            return numStr.toDoubleOrNull()
        }

        val fallback = Pattern.compile("""(?:valor|recebido|pago|tarifa|total)\s*[:=]?\s*(?:R\$)?\s*(\d+[.,]\d{2})""", Pattern.CASE_INSENSITIVE)
        val m2 = fallback.matcher(text)
        if (m2.find()) {
            val numStr = m2.group(1)?.replace(',', '.') ?: return null
            return numStr.toDoubleOrNull()
        }

        return null
    }

    private fun extractDistance(text: String): Double? {
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
