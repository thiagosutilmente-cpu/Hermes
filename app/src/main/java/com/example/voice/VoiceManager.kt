package com.example.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class VoiceManager(private val context: Context) : TextToSpeech.OnInitListener {

    private val TAG = "VoiceManager"
    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var focusRequest: AudioFocusRequest? = null

    private var toneGenerator: ToneGenerator? = null

    init {
        tts = TextToSpeech(context, this)
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 85) // 85% volume
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize ToneGenerator: ${e.message}")
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.forLanguageTag("pt-BR"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "Brazilian Portuguese language is not supported or missing data. Falling back to default locale.")
                tts?.language = Locale.getDefault()
            }
            isTtsInitialized = true
            Log.d(TAG, "TTS Engine initialized successfully with PT-BR.")
        } else {
            Log.e(TAG, "TTS Engine initialization failed with status: $status")
        }
    }

    /**
     * Fala uma frase, solicitando Audio Focus temporário e forçando a saída pelo canal de mídia/música.
     * O foco 'Transient May Duck' permite abaixar temporariamente o volume de outras mídias (músicas, GPS)
     * e restaurar o volume normal logo em seguida.
     */
    fun speak(text: String) {
        if (!isTtsInitialized || tts == null) {
            Log.w(TAG, "TTS ainda não está inicializado ou está indisponível.")
            return
        }

        requestAudioFocusAndSpeak(text)
    }

    private fun requestAudioFocusAndSpeak(text: String) {
        // Usamos USAGE_MEDIA ou USAGE_ASSISTANCE_NAVIGATION_GUIDANCE direcionando para o canal de mídia/fone
        val playbackAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val listener = AudioManager.OnAudioFocusChangeListener { focusChange ->
                when (focusChange) {
                    AudioManager.AUDIOFOCUS_LOSS,
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                        // Se perder o foco, interrompe a fala para não atrapalhar
                        tts?.stop()
                    }
                }
            }

            focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(playbackAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(listener)
                .build()

            val result = audioManager.requestAudioFocus(focusRequest!!)
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                speakWithTtsParams(text)
            } else {
                Log.w(TAG, "Audio Focus negado para fala.")
            }
        } else {
            @Suppress("DEPRECATION")
            val result = audioManager.requestAudioFocus(
                { focusChange ->
                    if (focusChange == AudioManager.AUDIOFOCUS_LOSS || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                        tts?.stop()
                    }
                },
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            )
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                speakWithTtsParams(text)
            }
        }
    }

    private fun speakWithTtsParams(text: String) {
        // Força áudio pelo STREAM_MUSIC (Canal de Mídia) usando os parâmetros do TTS
        val params = android.os.Bundle().apply {
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "radar_voice_id")
    }

    /**
     * Interrompe qualquer fala em andamento.
     */
    fun stop() {
        tts?.stop()
        abandonAudioFocus()
    }

    /**
     * Toca um tom de confirmação sonora (bipes rápidos).
     */
    fun playConfirmationChime() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 350)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play confirmation chime: ${e.message}")
        }
    }

    /**
     * Libera recursos ao encerrar.
     */
    fun shutdown() {
        tts?.apply {
            stop()
            shutdown()
        }
        tts = null
        try {
            toneGenerator?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to release ToneGenerator: ${e.message}")
        }
        toneGenerator = null
        abandonAudioFocus()
        Log.d(TAG, "VoiceManager finalizado com sucesso.")
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus { }
        }
    }
}
