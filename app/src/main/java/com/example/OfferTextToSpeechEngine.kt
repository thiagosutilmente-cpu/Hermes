package com.example

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
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
import java.util.concurrent.ConcurrentLinkedQueue
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

        /**
         * Limpa e padroniza a pontuação e termos abreviados para pronúncia natural em pt-BR.
         * Converte "R$ 28,50" para "28 reais e 50 centavos" e "4,2 km" para "4 vírgula 2 quilômetros",
         * garantindo que o motor de síntese de voz (TTS) do Android soe cristalino no capacete sem distrair o condutor.
         */
        fun cleanAlertForSpeech(raw: String): String {
            var cleaned = raw

            // Converte valores monetários como "R$ 28,50" ou "R$ 28.50" para "28 reais e 50 centavos"
            val currencyRegex = Regex("""(?:R\$|r\$)\s*(\d+)(?:[.,](\d{1,2}))?""")
            cleaned = currencyRegex.replace(cleaned) { matchResult ->
                val reais = matchResult.groupValues[1]
                val centavosRaw = matchResult.groupValues.getOrNull(2) ?: ""
                val centavosFormatted = if (centavosRaw.length == 1) "${centavosRaw}0" else centavosRaw
                val centavosInt = centavosFormatted.toIntOrNull() ?: 0
                if (centavosInt > 0) {
                    "$reais reais e $centavosInt centavos"
                } else {
                    "$reais reais"
                }
            }

            // Converte distâncias fracionadas como "3.8 km" ou "3,8 km" para "3 vírgula 8 quilômetros"
            val kmDecimalRegex = Regex("""(\d+)[.,](\d+)\s*(?:km|KM|Km)""")
            cleaned = kmDecimalRegex.replace(cleaned) { match ->
                "${match.groupValues[1]} vírgula ${match.groupValues[2]} quilômetros"
            }

            // Converte distâncias inteiras como "5 km" para "5 quilômetros"
            val kmIntRegex = Regex("""(\d+)\s*(?:km|KM|Km)""")
            cleaned = kmIntRegex.replace(cleaned) { match ->
                "${match.groupValues[1]} quilômetros"
            }

            return cleaned
                .replace("km/h", "quilômetros por hora", ignoreCase = true)
                .replace("/km", " por quilômetro", ignoreCase = true)
                .replace("/", " por ")
                .trim()
        }

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
                val meters = (km * 1000).toInt()
                "$meters metros"
            } else {
                val roundedOneDecimal = (Math.round(km * 10.0)) / 10.0
                val intPart = roundedOneDecimal.toInt()
                val decPart = ((roundedOneDecimal - intPart) * 10).toInt()
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

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var onSpeechStarted: (() -> Unit)? = null
    var onSpeechFinished: (() -> Unit)? = null

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private val pendingSpeechQueue = ConcurrentLinkedQueue<String>()

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

            // Monitor de progresso da fala com gerenciamento de foco de áudio (Audio Ducking)
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    requestAudioFocus()
                    mainScope.launch {
                        _engineState.value = _engineState.value.copy(isSpeaking = true)
                    }
                    onSpeechStarted?.invoke()
                }

                override fun onDone(utteranceId: String?) {
                    releaseAudioFocus()
                    mainScope.launch {
                        _engineState.value = _engineState.value.copy(isSpeaking = false)
                    }
                    onSpeechFinished?.invoke()
                }

                override fun onError(utteranceId: String?) {
                    releaseAudioFocus()
                    mainScope.launch {
                        _engineState.value = _engineState.value.copy(isSpeaking = false)
                    }
                    onSpeechFinished?.invoke()
                }

                override fun onStop(utteranceId: String?, interrupted: Boolean) {
                    releaseAudioFocus()
                    mainScope.launch {
                        _engineState.value = _engineState.value.copy(isSpeaking = false)
                    }
                    onSpeechFinished?.invoke()
                }
            })

            isInitialized = true
            _engineState.value = _engineState.value.copy(isReady = true)
            Log.i(TAG, "Motor TTS de Ofertas pronto em pt-BR com taxa ${currentRate}x.")

            // Despacha frases que foram enfileiradas antes da inicialização do hardware (ex: FCM em background)
            while (pendingSpeechQueue.isNotEmpty()) {
                val queued = pendingSpeechQueue.poll() ?: break
                speak(queued, TextToSpeech.QUEUE_ADD)
            }
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
        if (text.isBlank() || _engineState.value.isMuted) return

        if (!isInitialized || tts == null) {
            Log.w(TAG, "TTS ainda não inicializado. Enfileirando áudio de oferta: \"$text\"")
            pendingSpeechQueue.add(text)
            return
        }

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
     * Converte e vocaliza uma oferta recebida via Firebase Cloud Messaging (FCM) em tempo real,
     * permitindo que o condutor ouça instantaneamente sem desviar os olhos da pista.
     */
    fun speakFcmOffer(alert: RadarFirebaseMessagingService.FcmOfferAlert) {
        if (_engineState.value.isMuted) {
            Log.d(TAG, "Leitura de oferta FCM ignorada pois TTS está pausado/mutado.")
            return
        }

        val speechText = if (alert.voiceAlert.isNotBlank()) {
            cleanAlertForSpeech(alert.voiceAlert)
        } else {
            buildOfferSpeechText(
                appName = alert.appName,
                restaurant = alert.restaurant,
                value = alert.value,
                distanceKm = alert.distanceKm,
                profitPerKm = alert.gainPerKm,
                isGoodDeal = alert.decision == "ACCEPT" || alert.gainPerKm >= 2.0
            )
        }

        val summary = SpokenOfferSummary(
            appName = alert.appName,
            restaurant = alert.restaurant,
            value = alert.value,
            distanceKm = alert.distanceKm,
            profitPerKm = alert.gainPerKm,
            isGoodDeal = alert.decision == "ACCEPT" || alert.gainPerKm >= 2.0,
            fullSpeechText = speechText
        )

        _engineState.value = _engineState.value.copy(
            lastOfferSummary = summary,
            lastSpokenText = speechText
        )

        speak(speechText, TextToSpeech.QUEUE_FLUSH)
    }

    /**
     * Solicita foco de áudio com ducking para atenuar Waze, Spotify e rádio durante a fala
     */
    private fun requestAudioFocus(): Boolean {
        val am = audioManager ?: return false
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val playbackAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(playbackAttributes)
                    .setAcceptsDelayedFocusGain(false)
                    .setOnAudioFocusChangeListener { /* no-op listener */ }
                    .build()
                audioFocusRequest = request
                am.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            } else {
                @Suppress("DEPRECATION")
                am.requestAudioFocus(
                    null,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            }
        } catch (e: Exception) {
            Log.w(TAG, "Falha ao solicitar AudioFocus no OfferTtsEngine: ${e.message}")
            false
        }
    }

    /**
     * Libera o foco de áudio restaurando o volume normal dos outros aplicativos
     */
    private fun releaseAudioFocus() {
        val am = audioManager ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { am.abandonAudioFocusRequest(it) }
                audioFocusRequest = null
            } else {
                @Suppress("DEPRECATION")
                am.abandonAudioFocus(null)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Falha ao liberar AudioFocus: ${e.message}")
        }
    }

    /**
     * Limpa e padroniza a pontuação e termos abreviados para pronúncia natural em pt-BR.
     * Converte "R$ 28,50" para "28 reais e 50 centavos" e "4,2 km" para "4 vírgula 2 quilômetros",
     * garantindo que o motor de síntese de voz (TTS) do Android soe cristalino no capacete sem distrair o condutor.
     */
    fun cleanAlertForSpeech(raw: String): String = Companion.cleanAlertForSpeech(raw)

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
    ): String = Companion.buildOfferSpeechText(
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

    fun formatSpokenCurrency(amount: Double): String = Companion.formatSpokenCurrency(amount)

    fun formatSpokenDistance(km: Double): String = Companion.formatSpokenDistance(km)

    fun formatSpokenProfitPerKm(profitPerKm: Double): String = Companion.formatSpokenProfitPerKm(profitPerKm)
}
