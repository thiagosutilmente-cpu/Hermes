package com.example

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

/**
 * Gerenciador profissional de voz neural via Text-to-Speech nativo do Android.
 * Projetado para anunciar corridas, decisões neurais e métricas no fone Bluetooth do entregador,
 * permitindo operação hands-free no trânsito.
 */
class NeuralVoiceManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    var isMuted: Boolean = false

    var onSpeechStarted: (() -> Unit)? = null
    var onSpeechFinished: (() -> Unit)? = null
    var isSpeaking: Boolean = false
        private set

    init {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.e("NeuralVoiceManager", "Erro ao inicializar TTS: ${e.message}")
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val ptBr = Locale("pt", "BR")
            val result = tts?.setLanguage(ptBr) ?: TextToSpeech.LANG_MISSING_DATA
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Fallback para idioma padrão do sistema
                tts?.setLanguage(Locale.getDefault())
            }
            tts?.setSpeechRate(1.15f) // Velocidade ágil e dinâmica para o trânsito
            tts?.setPitch(1.0f)

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    isSpeaking = true
                    onSpeechStarted?.invoke()
                }

                override fun onDone(utteranceId: String?) {
                    isSpeaking = false
                    onSpeechFinished?.invoke()
                }

                override fun onError(utteranceId: String?) {
                    isSpeaking = false
                    onSpeechFinished?.invoke()
                }
            })

            isInitialized = true
            Log.i("NeuralVoiceManager", "Text-to-Speech inicializado com sucesso em pt-BR.")
        } else {
            Log.w("NeuralVoiceManager", "Falha na inicialização do TTS: $status")
        }
    }

    /**
     * Sintetiza uma fala se o áudio não estiver mutado.
     */
    fun speak(text: String, queueMode: Int = TextToSpeech.QUEUE_FLUSH) {
        if (isMuted || !isInitialized || text.isBlank()) return
        try {
            val utteranceId = "RADAR_${System.currentTimeMillis()}"
            val params = Bundle().apply {
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
            }
            tts?.speak(text, queueMode, params, utteranceId)
        } catch (e: Exception) {
            Log.e("NeuralVoiceManager", "Erro ao falar texto: ${e.message}")
        }
    }

    /**
     * Lê uma oferta de entrega detalhadamente em voz alta para o piloto em movimento.
     */
    fun readOfferAloud(
        appName: String,
        restaurant: String,
        value: Double,
        distanceKm: Double,
        gainPerKm: Double,
        pickupAddress: String = "",
        estimatedMinutes: Int = 0,
        neuralDecision: String = "ACCEPT",
        neuralReason: String = ""
    ) {
        val formattedVal = String.format(Locale.GERMANY, "%.2f", value).replace(".", ",")
        val formattedKm = String.format(Locale.GERMANY, "%.1f", distanceKm).replace(".", ",")
        val formattedGain = String.format(Locale.GERMANY, "%.2f", gainPerKm).replace(".", ",")

        val recommendation = if (neuralDecision.equals("accept", ignoreCase = true) || neuralDecision.contains("ACCEPT")) {
            "Recomendação Jarvis: Aceitar corrida vantajosa."
        } else {
            "Recomendação Jarvis: Atenção, rendimento abaixo do ideal."
        }

        val reasonText = if (neuralReason.isNotBlank()) " Motivo: $neuralReason." else ""
        val timeText = if (estimatedMinutes > 0) " Tempo estimado de $estimatedMinutes minutos." else ""

        val speech = "Oferta $appName. Estabelecimento: $restaurant. Valor total: $formattedVal reais para $formattedKm quilômetros, rendendo $formattedGain por quilômetro.$timeText $recommendation$reasonText"
        speak(speech)
    }

    /**
     * Anúncio inteligente de nova corrida interceptada
     */
    fun announceNewOffer(
        appName: String,
        restaurant: String,
        value: Double,
        distanceKm: Double,
        gainPerKm: Double,
        neuralDecision: String
    ) {
        readOfferAloud(
            appName = appName,
            restaurant = restaurant,
            value = value,
            distanceKm = distanceKm,
            gainPerKm = gainPerKm,
            neuralDecision = neuralDecision
        )
    }

    /**
     * Anúncio rápido mãos-livres para o piloto em movimento aceitar ou recusar no viva-voz
     */
    fun announceDrivingHandsFreeOffer(
        appName: String,
        restaurant: String,
        value: Double,
        distanceKm: Double,
        gainPerKm: Double
    ) {
        val formattedVal = String.format(Locale.GERMANY, "%.2f", value).replace(".", ",")
        val formattedKm = String.format(Locale.GERMANY, "%.1f", distanceKm).replace(".", ",")
        val formattedGain = String.format(Locale.GERMANY, "%.2f", gainPerKm).replace(".", ",")
        val speech = "Atenção piloto. Nova oferta $appName no $restaurant: $formattedVal reais para $formattedKm quilômetros, rendendo $formattedGain por quilômetro. Diga ACEITAR ou RECUSAR."
        speak(speech)
    }

    /**
     * Anúncio de aceite
     */
    fun announceAccept(restaurant: String, value: Double) {
        val formattedVal = String.format(Locale.GERMANY, "%.2f", value).replace(".", ",")
        speak("Corrida aceita no $restaurant. Mais $formattedVal reais garantidos. Abrindo rota no mapa.")
    }

    /**
     * Anúncio de recusa
     */
    fun announceDecline() {
        speak("Corrida descartada. Continuando monitoramento.")
    }

    /**
     * Anúncio de estado do radar
     */
    fun announceRadarState(isActive: Boolean) {
        if (isActive) {
            speak("Radar Neural ativado. Monitorando iFood, Rappi, Uber e 99 Food.")
        } else {
            speak("Radar Neural pausado.")
        }
    }

    /**
     * Anúncio de saldo e faturamento do dia
     */
    fun announceEarnings(todayGross: Double, netProfit: Double, totalKm: Double, deliveryCount: Int) {
        val grossFmt = String.format(Locale.GERMANY, "%.2f", todayGross).replace(".", ",")
        val profitFmt = String.format(Locale.GERMANY, "%.2f", netProfit).replace(".", ",")
        val kmFmt = String.format(Locale.GERMANY, "%.1f", totalKm).replace(".", ",")
        val deliveryText = if (deliveryCount == 1) "1 entrega realizada" else "$deliveryCount entregas realizadas"
        speak("Ganhos de hoje: $grossFmt reais brutos com lucro líquido estimado de $profitFmt reais, totalizando $kmFmt quilômetros rodados em $deliveryText.")
    }

    /**
     * Anúncio de rota e navegação para coleta
     */
    fun announceNavigation(restaurant: String, address: String) {
        val addrText = if (address.isNotBlank()) " no endereço $address." else "."
        speak("Iniciando navegação no mapa para coleta no $restaurant$addrText")
    }

    /**
     * Anúncio de status de saúde e conectividade
     */
    fun announceSystemHealth(score: Int, gpsAccuracyMeters: Float, latencyMs: Int) {
        val accuracyFmt = String.format(Locale.GERMANY, "%.1f", gpsAccuracyMeters).replace(".", ",")
        speak("Diagnóstico do sistema: Índice de integridade em $score de 100. GPS com precisão de $accuracyFmt metros e latência de rede em $latencyMs milissegundos.")
    }

    /**
     * Ajuda com lista de comandos de voz disponíveis
     */
    fun announceHelp() {
        speak("Comandos viva-voz disponíveis: Diga Aceitar, Cancelar, Ler oferta, Saldo, Rota, Filtro chuva ou Tiro curto.")
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (_: Exception) {}
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {}
    }
}
