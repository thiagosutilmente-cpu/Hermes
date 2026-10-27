package com.example

import android.content.Context
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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * DeliveryAudioNotificationService
 *
 * Serviço dedicado de gerenciamento de áudio com Text-to-Speech (TTS)
 * voltado especificamente para entregadores em trânsito com fone de ouvido ou intercomunicador de capacete.
 *
 * Características principais:
 * 1. Roteamento de áudio para fone Bluetooth/P2 (Canal de Navegação & Assistência).
 * 2. Audio Ducking automático: abaixa o volume de músicas/podcasts durante a fala e restaura ao término.
 * 3. Notificações táticas de mudanças na fila de entregas e alterações de rota para manter os "olhos na estrada".
 */
class DeliveryAudioNotificationService private constructor(private val context: Context) : TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "DeliveryAudioService"
        private val PT_BR = Locale("pt", "BR")

        @Volatile
        private var instance: DeliveryAudioNotificationService? = null

        fun getInstance(context: Context): DeliveryAudioNotificationService {
            return instance ?: synchronized(this) {
                instance ?: DeliveryAudioNotificationService(context.applicationContext).also { instance = it }
            }
        }
    }

    enum class QueueChangeType {
        ORDER_ADDED_TO_STACK,
        ORDER_CANCELLED,
        PICKUP_SEQUENCE_CHANGED,
        EXPIRATION_WARNING
    }

    enum class RouteChangeReason {
        TRAFFIC_DETOUR_AVOIDED,
        RECALCULATED_AFTER_CANCELLATION,
        HAZE_CORRIDOR_SHORTCUT,
        APPROACHING_STOP
    }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var textToSpeech: TextToSpeech? = null
    private var audioFocusRequest: AudioFocusRequest? = null

    private val _isTtsReady = MutableStateFlow(false)
    val isTtsReady: StateFlow<Boolean> = _isTtsReady.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _isHeadsetConnected = MutableStateFlow(false)
    val isHeadsetConnected: StateFlow<Boolean> = _isHeadsetConnected.asStateFlow()

    private val _lastVoiceAlert = MutableStateFlow("Sistema de áudio em prontidão nos fones")
    val lastVoiceAlert: StateFlow<String> = _lastVoiceAlert.asStateFlow()

    private val _isEyesOnRoadModeEnabled = MutableStateFlow(true)
    val isEyesOnRoadModeEnabled: StateFlow<Boolean> = _isEyesOnRoadModeEnabled.asStateFlow()

    private val _speechRate = MutableStateFlow(1.20f) // Velocidade calibrada para motoboys
    val speechRate: StateFlow<Float> = _speechRate.asStateFlow()

    private val pendingSpeechQueue = ConcurrentLinkedQueue<String>()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    init {
        initializeTts()
        checkHeadsetStatus()
    }

    private fun initializeTts() {
        try {
            textToSpeech = TextToSpeech(context, this)
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao instanciar TextToSpeech: ${e.message}")
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val tts = textToSpeech ?: return
            val result = tts.setLanguage(PT_BR)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts.setLanguage(Locale.getDefault())
            }

            tts.setSpeechRate(_speechRate.value)
            tts.setPitch(1.05f)

            // Configura áudio com canal de orientação de navegação (ducking em músicas)
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            tts.setAudioAttributes(audioAttributes)

            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                    releaseAudioFocus()
                    processNextInQueue()
                }

                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                    releaseAudioFocus()
                    processNextInQueue()
                }
            })

            _isTtsReady.value = true
            Log.i(TAG, "🟢 DeliveryAudioNotificationService pronto em Português-BR")
            processNextInQueue()
        } else {
            Log.e(TAG, "Falha na inicialização do TextToSpeech: status=$status")
            _isTtsReady.value = false
        }
    }

    fun checkHeadsetStatus() {
        val am = audioManager ?: return
        val isWired = am.isWiredHeadsetOn
        val isBt = am.isBluetoothA2dpOn || am.isBluetoothScoOn
        _isHeadsetConnected.value = (isWired || isBt)
    }

    /**
     * Fala uma mensagem com prioridade imediata e Audio Ducking.
     */
    fun speak(text: String, flushQueue: Boolean = true) {
        if (!_isEyesOnRoadModeEnabled.value || text.isBlank()) return

        _lastVoiceAlert.value = text

        if (!_isTtsReady.value) {
            if (flushQueue) pendingSpeechQueue.clear()
            pendingSpeechQueue.offer(text)
            return
        }

        requestAudioFocus()

        val tts = textToSpeech ?: return
        val queueMode = if (flushQueue) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        val utteranceId = "delivery_alert_${System.currentTimeMillis()}"

        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }
        tts.speak(text, queueMode, params, utteranceId)
    }

    // -----------------------------------------------------------------------------------
    // ANÚNCIOS ESPECÍFICOS DE MUDANÇA NA FILA DE ENTREGAS
    // -----------------------------------------------------------------------------------
    fun announceQueueChange(
        changeType: QueueChangeType,
        appName: String,
        restaurant: String,
        extraDetails: String = ""
    ) {
        val message = when (changeType) {
            QueueChangeType.ORDER_ADDED_TO_STACK ->
                "Atenção piloto: novo pedido da $appName adicionado à sua fila. Coleta no $restaurant. $extraDetails"

            QueueChangeType.ORDER_CANCELLED ->
                "Alerta na fila: a entrega da $appName no $restaurant foi cancelada. Mantenha os olhos na pista."

            QueueChangeType.PICKUP_SEQUENCE_CHANGED ->
                "Mudança na fila de cozinha: prioridade alterada. Retire primeiro no $restaurant para manter a temperatura."

            QueueChangeType.EXPIRATION_WARNING ->
                "Atenção: oferta da $appName expirando em dez segundos."
        }
        speak(message, flushQueue = true)
    }

    // -----------------------------------------------------------------------------------
    // ANÚNCIOS ESPECÍFICOS DE ALTERAÇÃO DE ROTA
    // -----------------------------------------------------------------------------------
    fun announceRouteChange(
        reason: RouteChangeReason,
        targetName: String,
        deviationKmOrMeters: String = ""
    ) {
        val message = when (reason) {
            RouteChangeReason.TRAFFIC_DETOUR_AVOIDED ->
                "Rota recalculada: atalho pelo corredor viário ativado, economizando $deviationKmOrMeters de congestionamento."

            RouteChangeReason.RECALCULATED_AFTER_CANCELLATION ->
                "Rota atualizada: seguindo direto para $targetName com trajeto individual sem desvios."

            RouteChangeReason.HAZE_CORRIDOR_SHORTCUT ->
                "Trajeto otimizado: desvio de apenas $deviationKmOrMeters para a entrega em $targetName."

            RouteChangeReason.APPROACHING_STOP ->
                "Atenção: aproximação da parada em $targetName a $deviationKmOrMeters. Reduza com segurança."
        }
        speak(message, flushQueue = true)
    }

    fun toggleEyesOnRoad(enabled: Boolean) {
        _isEyesOnRoadModeEnabled.value = enabled
        if (!enabled) {
            stopSpeaking()
        } else {
            speak("Modo olhos na estrada ativado nos fones de ouvido.", flushQueue = true)
        }
    }

    fun setSpeechRate(rate: Float) {
        val clamped = rate.coerceIn(0.9f, 1.5f)
        _speechRate.value = clamped
        textToSpeech?.setSpeechRate(clamped)
    }

    fun stopSpeaking() {
        try {
            textToSpeech?.stop()
            _isSpeaking.value = false
            releaseAudioFocus()
        } catch (_: Exception) {}
    }

    private fun processNextInQueue() {
        val next = pendingSpeechQueue.poll() ?: return
        speak(next, flushQueue = false)
    }

    private fun requestAudioFocus() {
        val am = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (audioFocusRequest == null) {
                audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .build()
            }
            audioFocusRequest?.let { am.requestAudioFocus(it) }
        } else {
            @Suppress("DEPRECATION")
            am.requestAudioFocus(null, AudioManager.STREAM_NOTIFICATION, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        }
    }

    private fun releaseAudioFocus() {
        val am = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { am.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            am.abandonAudioFocus(null)
        }
    }

    fun destroy() {
        try {
            stopSpeaking()
            textToSpeech?.shutdown()
            textToSpeech = null
            _isTtsReady.value = false
        } catch (_: Exception) {}
    }
}
