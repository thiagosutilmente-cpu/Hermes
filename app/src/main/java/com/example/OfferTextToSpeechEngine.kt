package com.example

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Resumo estruturado da oferta para reprodução por voz e feedback visual
 */
data class SpokenOfferSummary(
    val appName: String,
    val restaurant: String,
    val value: Double,
    val distanceKm: Double,
    val profitPerKm: Double,
    val isGoodDeal: Boolean,
    val fullSpeechText: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Estado reativo do motor de Text-to-Speech nativo do Android
 */
data class TtsEngineState(
    val isReady: Boolean = false,
    val isSpeaking: Boolean = false,
    val isMuted: Boolean = false,
    val speechRate: Float = 1.15f,
    val lastSpokenText: String = "",
    val lastOfferSummary: SpokenOfferSummary? = null
)

/**
 * OfferTextToSpeechEngine
 *
 * Motor de Text-to-Speech (TTS) nativo do Android ([TextToSpeech]) desenvolvido especificamente
 * para segurança viária de motoboys e motoristas de aplicativo.
 *
 * Funcionalidade Principal:
 * Lê em voz alta o resumo essencial da oferta em português brasileiro claro e natural:
 * - VALOR DA CORRIDA (ex: "28 reais e 50 centavos")
 * - DISTÂNCIA TOTAL (ex: "para 3 vírgula 9 quilômetros")
 * - LUCRO LÍQUIDO POR KM (ex: "rendendo 7 reais e 30 centavos por quilômetro. Corrida excelente!")
 *
 * Garante que o entregador tome decisões táticas em milissegundos sem precisar tirar
 * os olhos da via nem soltar as mãos do guidão da moto.
 */
class OfferTextToSpeechEngine private constructor(private val context: Context) : TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "OfferTtsEngine"
        private const val PREFS_NAME = "radar_tts_engine_prefs"
        private const val KEY_IS_MUTED = "key_tts_is_muted"
        private const val KEY_SPEECH_RATE = "key_tts_speech_rate"

        @Volatile
        private var INSTANCE: OfferTextToSpeechEngine? = null

        fun getInstance(context: Context): OfferTextToSpeechEngine {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: OfferTextToSpeechEngine(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var onSpeechStarted: (() -> Unit)? = null
    var onSpeechFinished: (() -> Unit)? = null

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _engineState = MutableStateFlow(
        TtsEngineState(
            isMuted = prefs.getBoolean(KEY_IS_MUTED, false),
            speechRate = prefs.getFloat(KEY_SPEECH_RATE, 1.15f)
        )
    )
    val engineState: StateFlow<TtsEngineState> = _engineState.asStateFlow()

    private val mainScope = CoroutineScope(Dispatchers.Main)

    init {
        initializeTts()
    }

    private fun initializeTts() {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.e(TAG, "Falha crítica ao instanciar TextToSpeech nativo do Android", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val ptBr = Locale("pt", "BR")
            val langResult = tts?.setLanguage(ptBr) ?: TextToSpeech.LANG_MISSING_DATA

            if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w(TAG, "pt-BR não disponível no TTS local; utilizando idioma padrão do sistema.")
                tts?.setLanguage(Locale.getDefault())
            }

            // Otimização acústica para capacete e trânsito urbano:
            // 1.15x é a velocidade ideal testada com entregadores (ágil sem perder a clareza)
            val currentRate = _engineState.value.speechRate
            tts?.setSpeechRate(currentRate)
            tts?.setPitch(1.0f)

            // Configuração do canal de áudio para orientação de navegação no fone/intercomunicador
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                tts?.setAudioAttributes(audioAttributes)
            }

            // Monitor de progresso da fala
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    mainScope.launch {
                        _engineState.value = _engineState.value.copy(isSpeaking = true)
                    }
                    onSpeechStarted?.invoke()
                }

                override fun onDone(utteranceId: String?) {
                    mainScope.launch {
                        _engineState.value = _engineState.value.copy(isSpeaking = false)
                    }
                    onSpeechFinished?.invoke()
                }

                override fun onError(utteranceId: String?) {
                    mainScope.launch {
                        _engineState.value = _engineState.value.copy(isSpeaking = false)
                    }
                    onSpeechFinished?.invoke()
                }
            })

            isInitialized = true
            _engineState.value = _engineState.value.copy(isReady = true)
            Log.i(TAG, "Motor TTS de Ofertas pronto em pt-BR com taxa ${currentRate}x.")
        } else {
            Log.e(TAG, "Erro na inicialização do TextToSpeech Android: status=$status")
            _engineState.value = _engineState.value.copy(isReady = false)
        }
    }

    // =========================================================================
    // LEITURA INTELIGENTE DE RESUMO DA OFERTA (VALOR, DISTÂNCIA E LUCRO/KM)
    // =========================================================================

    /**
     * Constrói e vocaliza o resumo estruturado e humanizado da oferta de entrega.
     *
     * @param appName Nome do aplicativo (ex: "iFood", "99 Moto", "Uber")
     * @param restaurant Nome do local de coleta (ex: "Shopping Eldorado")
     * @param value Valor bruto da corrida em Reais (ex: 28.50)
     * @param distanceKm Distância viária estimada em km (ex: 3.9)
     * @param profitPerKm Lucro ou ganho líquido calculado por km (ex: 7.30)
     * @param isGoodDeal Sinalização algorítmica de rentabilidade (Verde/Alta vs Vermelho/Baixa)
     * @param hasDualRoute Indica se há pedido sobreposto na mesma rota
     * @param dualRouteAppName Nome do app compatível para rota dupla
     * @param dualRouteExtraGain Valor adicional garantido na rota dupla
     */
    fun speakOfferSummary(
        appName: String,
        restaurant: String,
        value: Double,
        distanceKm: Double,
        profitPerKm: Double,
        isGoodDeal: Boolean,
        hasDualRoute: Boolean = false,
        dualRouteAppName: String = "",
        dualRouteExtraGain: Double = 0.0
    ) {
        if (_engineState.value.isMuted) {
            Log.d(TAG, "Leitura de oferta ignorada pois TTS está pausado/mutado pelo piloto.")
            return
        }

        val speechText = buildOfferSpeechText(
            appName = appName,
            restaurant = restaurant,
            value = value,
            distanceKm = distanceKm,
            profitPerKm = profitPerKm,
            isGoodDeal = isGoodDeal,
            hasDualRoute = hasDualRoute,
            dualRouteAppName = dualRouteAppName,
            dualRouteExtraGain = dualRouteExtraGain
        )

        val summary = SpokenOfferSummary(
            appName = appName,
            restaurant = restaurant,
            value = value,
            distanceKm = distanceKm,
            profitPerKm = profitPerKm,
            isGoodDeal = isGoodDeal,
            fullSpeechText = speechText
        )

        _engineState.value = _engineState.value.copy(
            lastOfferSummary = summary,
            lastSpokenText = speechText
        )

        speak(speechText, TextToSpeech.QUEUE_FLUSH)
    }

    /**
     * Lê uma oferta tática da bolha flutuante HUD ([OverlayWindowManager.FloatingTacticalOffer])
     */
    fun speakTacticalOffer(offer: OverlayWindowManager.FloatingTacticalOffer) {
        speakOfferSummary(
            appName = offer.appName,
            restaurant = offer.pickup.ifBlank { "Coleta Próxima" },
            value = offer.value,
            distanceKm = offer.distanceKm,
            profitPerKm = offer.pricePerKm,
            isGoodDeal = offer.isGoodDeal,
            hasDualRoute = offer.hasDualRoute,
            dualRouteAppName = offer.dualRouteAppName,
            dualRouteExtraGain = offer.dualRouteExtraGain
        )
    }

    /**
     * Lê uma [DeliveryOffer] do banco de dados ou da lista principal de pedidos
     */
    fun speakDeliveryOffer(offer: DeliveryOffer) {
        speakOfferSummary(
            appName = offer.appOrigem,
            restaurant = offer.nomeRestaurante,
            value = offer.valor,
            distanceKm = offer.distancia,
            profitPerKm = offer.ganhoPorKm,
            isGoodDeal = offer.isAltaRentabilidade,
            hasDualRoute = false
        )
    }

    /**
     * Repete a última oferta anunciada (útil caso o piloto estivesse em trecho barulhento)
     */
    fun repeatLastOffer(): Boolean {
        val last = _engineState.value.lastOfferSummary ?: return false
        speak("Repetindo: ${last.fullSpeechText}", TextToSpeech.QUEUE_FLUSH)
        return true
    }

    /**
     * Fala a mensagem ultra-curta 'voice_alert' gerada pelo motor tático Jarvis Neural Cockpit
     * Exemplo: "Corrida top! R$ 18 por 5km. R$ 3 e 60 o km. Pode pegar."
     */
    fun speakVoiceAlert(voiceAlert: String) {
        if (voiceAlert.isBlank() || _engineState.value.isMuted) return
        _engineState.value = _engineState.value.copy(lastSpokenText = voiceAlert)
        speak(voiceAlert, TextToSpeech.QUEUE_FLUSH)
    }

    /**
     * Fala um texto genérico no motor TTS
     */
    fun speak(text: String, queueMode: Int = TextToSpeech.QUEUE_FLUSH) {
        if (!isInitialized || text.isBlank() || _engineState.value.isMuted) return

        try {
            val utteranceId = "OFFER_TTS_${System.currentTimeMillis()}"
            val params = Bundle().apply {
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
            }
            tts?.speak(text, queueMode, params, utteranceId)
            Log.d(TAG, "TTS reproduzindo: \"$text\"")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao enviar texto para síntese de voz: ${e.message}", e)
        }
    }

    /**
     * Anuncia a decisão de aceite
     */
    fun speakAcceptance(restaurant: String, value: Double) {
        val valFormatted = formatSpokenCurrency(value)
        speak("Corrida aceita no $restaurant. Mais $valFormatted garantidos! Boa viagem.", TextToSpeech.QUEUE_FLUSH)
    }

    /**
     * Anuncia a decisão de recusa
     */
    fun speakDecline() {
        speak("Corrida dispensada. Monitorando novas oportunidades.", TextToSpeech.QUEUE_FLUSH)
    }

    /**
     * Interrompe qualquer reprodução de áudio imediatamente
     */
    fun stop() {
        try {
            tts?.stop()
            _engineState.value = _engineState.value.copy(isSpeaking = false)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao pausar TTS: ${e.message}")
        }
    }

    /**
     * Alterna o estado de mudo da leitura por voz
     */
    fun toggleMute(): Boolean {
        val newMuted = !_engineState.value.isMuted
        setMuted(newMuted)
        return newMuted
    }

    fun setMuted(muted: Boolean) {
        prefs.edit().putBoolean(KEY_IS_MUTED, muted).apply()
        _engineState.value = _engineState.value.copy(isMuted = muted)
        if (muted) {
            stop()
        } else {
            speak("Leitura por voz de ofertas ativada.", TextToSpeech.QUEUE_FLUSH)
        }
    }

    fun setSpeechRate(rate: Float) {
        val safeRate = rate.coerceIn(0.8f, 2.0f)
        prefs.edit().putFloat(KEY_SPEECH_RATE, safeRate).apply()
        tts?.setSpeechRate(safeRate)
        _engineState.value = _engineState.value.copy(speechRate = safeRate)
    }

    fun shutdown() {
        try {
            stop()
            tts?.shutdown()
            tts = null
            isInitialized = false
        } catch (_: Exception) {}
    }

    // =========================================================================
    // FORMATADORES HUMANIZADOS EM PORTUGUÊS BRASILEIRO (PT-BR)
    // =========================================================================

    /**
     * Gera a frase natural e fluida do resumo da oferta para o piloto.
     */
    fun buildOfferSpeechText(
        appName: String,
        restaurant: String,
        value: Double,
        distanceKm: Double,
        profitPerKm: Double,
        isGoodDeal: Boolean,
        hasDualRoute: Boolean = false,
        dualRouteAppName: String = "",
        dualRouteExtraGain: Double = 0.0
    ): String {
        val spokenValue = formatSpokenCurrency(value)
        val spokenDistance = formatSpokenDistance(distanceKm)
        val spokenProfit = formatSpokenProfitPerKm(profitPerKm)

        val verdict = if (isGoodDeal) {
            "Corrida excelente! Recomendação: aceitar."
        } else {
            "Atenção: margem muito baixa, corrida em prejuízo."
        }

        val dualRouteClause = if (hasDualRoute && dualRouteExtraGain > 0.0) {
            val extraFmt = formatSpokenCurrency(dualRouteExtraGain)
            " Alerta: rota dupla compatível com $dualRouteAppName, mais $extraFmt na mesma viagem!"
        } else ""

        val placeClause = if (restaurant.isNotBlank() && restaurant != "Coleta Próxima") {
            " no $restaurant"
        } else ""

        return "Nova oferta $appName$placeClause. Valor: $spokenValue para $spokenDistance. Lucro de $spokenProfit. $verdict$dualRouteClause"
    }

    /**
     * Formata valores monetários em linguagem natural brasileira:
     * ex: 28.50 -> "28 reais e 50 centavos"
     * ex: 15.00 -> "15 reais"
     * ex: 0.75 -> "75 centavos"
     */
    fun formatSpokenCurrency(amount: Double): String {
        val totalCents = (Math.round(amount * 100)).toInt()
        val reais = totalCents / 100
        val centavos = totalCents % 100

        return when {
            reais > 0 && centavos > 0 -> "$reais reais e $centavos centavos"
            reais > 0 && centavos == 0 -> "$reais reais"
            reais == 0 && centavos > 0 -> "$centavos centavos"
            else -> "zero reais"
        }
    }

    /**
     * Formata distâncias para leitura natural do copiloto:
     * ex: 3.9 -> "3 vírgula 9 quilômetros"
     * ex: 4.0 -> "4 quilômetros"
     * ex: 0.8 -> "800 metros"
     */
    fun formatSpokenDistance(km: Double): String {
        return if (km < 1.0 && km > 0.0) {
            val meters = (km * 1000).roundToInt()
            "$meters metros"
        } else {
            val roundedOneDecimal = (Math.round(km * 10.0)) / 10.0
            val intPart = roundedOneDecimal.toInt()
            val decPart = ((roundedOneDecimal - intPart) * 10).roundToInt()
            if (decPart > 0) {
                "$intPart vírgula $decPart quilômetros"
            } else {
                "$intPart quilômetros"
            }
        }
    }

    /**
     * Formata o lucro por km para a voz:
     * ex: 7.30 -> "7 reais e 30 centavos por quilômetro"
     * ex: 5.00 -> "5 reais por quilômetro"
     */
    fun formatSpokenProfitPerKm(profitPerKm: Double): String {
        val currencyPart = formatSpokenCurrency(profitPerKm)
        return "$currencyPart por quilômetro"
    }
}
