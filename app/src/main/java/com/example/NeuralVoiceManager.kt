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
