package com.example

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * TextToSpeechManager
 *
 * Gerenciador dedicado para o motor de síntese de voz (TTS) do Android,
 * configurado nativamente com o Locale em Português Brasileiro (pt-BR).
 *
 * Responsabilidades:
 * 1. Inicializar e monitorar o estado do [TextToSpeech] do Android.
 * 2. Garantir o idioma Português Brasileiro (`Locale("pt", "BR")`).
 * 3. Reproduzir automaticamente o `voice_alert` retornado pelo servidor VPS da Hostinger
 *    ou pela IA do Jarvis Neural Cockpit sem que o motoboy desvie a atenção do trânsito.
 * 4. Controlar fila de reprodução, volume, velocidade da fala (speech rate) e pausas.
 */
class TextToSpeechManager private constructor(private val context: Context) : TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "TextToSpeechManager"
        private const val PREFS_NAME = "tts_manager_prefs"
        private const val KEY_SPEECH_RATE = "pref_tts_speech_rate"
        private const val KEY_AUTO_VOICE_ALERT = "pref_auto_voice_alert_enabled"

        private val PT_BR = Locale("pt", "BR")

        @Volatile
        private var instance: TextToSpeechManager? = null

        fun getInstance(context: Context): TextToSpeechManager {
            return instance ?: synchronized(this) {
                instance ?: TextToSpeechManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private var textToSpeech: TextToSpeech? = null

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _lastSpokenAlert = MutableStateFlow("")
    val lastSpokenAlert: StateFlow<String> = _lastSpokenAlert.asStateFlow()

    private val _isAutoVoiceAlertEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_AUTO_VOICE_ALERT, true)
    )
    val isAutoVoiceAlertEnabled: StateFlow<Boolean> = _isAutoVoiceAlertEnabled.asStateFlow()

    private var speechRate: Float = prefs.getFloat(KEY_SPEECH_RATE, 1.15f)

    private val pendingSpeechQueue = ConcurrentLinkedQueue<String>()

    init {
        initializeTts()
    }

    private fun initializeTts() {
        try {
            textToSpeech = TextToSpeech(context, this)
        } catch (e: Exception) {
            Log.e(TAG, "Falha crítica ao instanciar TextToSpeech: ${e.message}", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val tts = textToSpeech
            if (tts == null) {
                Log.e(TAG, "Instância TTS nula após onInit")
                return
            }

            // Configuração do idioma em Português Brasileiro (pt-BR)
            val langResult = tts.setLanguage(PT_BR)
            if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w(TAG, "Idioma pt-BR não disponível nativamente no motor atual, tentando Locale padrão do sistema...")
                tts.language = Locale.getDefault()
            }

            // Otimização de áudio para fones Bluetooth e capacetes
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                tts.setAudioAttributes(audioAttributes)
            }

            // Calibração de tom e velocidade rápida para motociclistas em movimento
            tts.setPitch(1.02f)
            tts.setSpeechRate(speechRate)

            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                    Log.d(TAG, "Iniciando síntese de voz (utterance: $utteranceId)")
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                    Log.d(TAG, "Síntese concluída (utterance: $utteranceId)")
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                    Log.w(TAG, "Erro na síntese de voz (utterance: $utteranceId)")
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    _isSpeaking.value = false
                    Log.w(TAG, "Erro na síntese de voz código $errorCode (utterance: $utteranceId)")
                }
            })

            _isInitialized.value = true
            Log.i(TAG, "TextToSpeechManager inicializado com sucesso em Português do Brasil.")

            // Despacha frases que foram enfileiradas antes da inicialização do hardware
            while (pendingSpeechQueue.isNotEmpty()) {
                val queued = pendingSpeechQueue.poll() ?: break
                speak(queued, TextToSpeech.QUEUE_ADD)
            }

        } else {
            Log.e(TAG, "Falha na inicialização do TextToSpeech (status: $status)")
            _isInitialized.value = false
        }
    }

    /**
     * Reproduz automaticamente a string 'voice_alert' retornada pelo servidor VPS ou pelo motor Jarvis.
     * Exemplo: "Corrida top! R$ 19 e 50 por 5km. R$ 3 e 75 o km. Pode pegar."
     *
     * @param voiceAlert Texto retornado pelo servidor
     * @param forceSpeak Se true, fala mesmo se o auto-voice alert estiver desligado
     */
    fun speakVoiceAlert(voiceAlert: String?, forceSpeak: Boolean = false) {
        if (voiceAlert.isNullOrBlank()) {
            Log.d(TAG, "voice_alert vazio ignorado.")
            return
        }

        if (!_isAutoVoiceAlertEnabled.value && !forceSpeak) {
            Log.d(TAG, "Auto voice_alert desligado nas configurações do app.")
            return
        }

        val sanitizedText = cleanAlertForSpeech(voiceAlert)
        _lastSpokenAlert.value = sanitizedText

        speak(sanitizedText, TextToSpeech.QUEUE_FLUSH)
    }

    /**
     * Fala qualquer texto arbitrário via TTS
     */
    fun speak(text: String, queueMode: Int = TextToSpeech.QUEUE_FLUSH) {
        if (text.isBlank()) return

        if (!_isInitialized.value || textToSpeech == null) {
            Log.w(TAG, "TTS ainda não inicializado. Enfileirando áudio: \"$text\"")
            pendingSpeechQueue.add(text)
            return
        }

        try {
            val utteranceId = "VOICE_ALERT_${System.currentTimeMillis()}"
            val params = Bundle().apply {
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
            }
            textToSpeech?.speak(text, queueMode, params, utteranceId)
            Log.i(TAG, "Reproduzindo voice_alert: \"$text\"")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao enviar texto para o motor TTS: ${e.message}", e)
        }
    }

    /**
     * Interrompe qualquer áudio em andamento imediatamente
     */
    fun stop() {
        try {
            textToSpeech?.stop()
            _isSpeaking.value = false
        } catch (e: Exception) {
            Log.w(TAG, "Erro ao parar TTS: ${e.message}")
        }
    }

    /**
     * Repete o último alerta falado
     */
    fun repeatLastAlert() {
        val last = _lastSpokenAlert.value
        if (last.isNotBlank()) {
            speak("Repetindo: $last", TextToSpeech.QUEUE_FLUSH)
        }
    }

    /**
     * Habilita ou desabilita a leitura automática de voice_alert recebidos da VPS
     */
    fun setAutoVoiceAlertEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_VOICE_ALERT, enabled).apply()
        _isAutoVoiceAlertEnabled.value = enabled
    }

    /**
     * Ajusta a velocidade da fala (ex: 1.0f = normal, 1.25f = mais rápido)
     */
    fun setSpeechRate(rate: Float) {
        val clamped = rate.coerceIn(0.75f, 2.0f)
        speechRate = clamped
        prefs.edit().putFloat(KEY_SPEECH_RATE, clamped).apply()
        textToSpeech?.setSpeechRate(clamped)
    }

    /**
     * Libera recursos do sintetizador de voz ao encerrar o app
     */
    fun shutdown() {
        try {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
            textToSpeech = null
            _isInitialized.value = false
        } catch (_: Exception) {}
    }

    /**
     * Formata e limpa o texto para garantir pronúncia brasileira natural
     */
    private fun cleanAlertForSpeech(raw: String): String {
        return raw
            .replace("R$", "reais")
            .replace("km", "quilômetros")
            .replace("Km", "quilômetros")
            .replace("/", " por ")
            .trim()
    }
}
