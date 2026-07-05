package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * VoiceInputManager provides an enterprise-grade voice recognition engine for the delivery rider.
 * It is fully mapped and architected to mirror the standard Web Speech API (SpeechRecognition) events:
 * - Web Speech 'onstart' / 'onsoundstart' -> maps to Android onReadyForSpeech
 * - Web Speech 'onspeechstart' -> maps to Android onBeginningOfSpeech
 * - Web Speech 'onspeechend' -> maps to Android onEndOfSpeech
 * - Web Speech 'onresult' -> maps to Android onResults
 * - Web Speech 'onerror' -> maps to Android onError
 * - Web Speech 'onend' -> maps to Android session completion / destroy
 */
class VoiceInputManager(private val context: Context) {

    private val TAG = "VoiceInputManager"
    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText.asStateFlow()

    private var onCommandReceived: ((String) -> Unit)? = null

    init {
        // SpeechRecognizer must be created and accessed exclusively from the main looper thread.
        runOnMainThread {
            recreateSpeechRecognizer()
        }
    }

    private fun runOnMainThread(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action()
        } else {
            mainHandler.post(action)
        }
    }

    private fun recreateSpeechRecognizer() {
        try {
            // Clean up existing instance first
            speechRecognizer?.destroy()
            speechRecognizer = null

            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                setupListener()
                Log.d(TAG, "SpeechRecognizer (Web Speech API Wrapper) successfully initialized.")
            } else {
                Log.e(TAG, "Speech recognition is not available on this device")
                _recognizedText.value = "Recursos de voz indisponíveis"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize/recreate SpeechRecognizer: ${e.message}", e)
        }
    }

    private fun setupListener() {
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                // Web Speech API: onstart / onsoundstart
                Log.d(TAG, "[WebSpeech::onstart] Speech recognition service has started listening.")
                _isListening.value = true
            }

            override fun onBeginningOfSpeech() {
                // Web Speech API: onspeechstart
                Log.d(TAG, "[WebSpeech::onspeechstart] Active speech/voice input detected.")
            }

            override fun onRmsChanged(rmsdB: Float) {
                // Real-time sound level tracking
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                // Web Speech API: onspeechend
                Log.d(TAG, "[WebSpeech::onspeechend] User finished speaking.")
                _isListening.value = false
            }

            override fun onError(error: Int) {
                // Web Speech API: onerror
                val message = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Erro de áudio"
                    SpeechRecognizer.ERROR_CLIENT -> "Erro do cliente"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Sem permissão de áudio"
                    SpeechRecognizer.ERROR_NETWORK -> "Erro de rede"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Timeout de rede"
                    SpeechRecognizer.ERROR_NO_MATCH -> "Nenhum comando ouvido"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Serviço de voz ocupado"
                    SpeechRecognizer.ERROR_SERVER -> "Erro do servidor"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Timeout de fala"
                    else -> "Erro desconhecido"
                }
                Log.e(TAG, "[WebSpeech::onerror] SpeechRecognizer Error ($error): $message")
                _isListening.value = false
                _recognizedText.value = "Erro: $message"

                // Auto-recover if client is busy or broken
                if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY || error == SpeechRecognizer.ERROR_CLIENT) {
                    Log.w(TAG, "Speech recognizer is busy or errored. Recreating session to recover.")
                    recreateSpeechRecognizer()
                }
            }

            override fun onResults(results: Bundle?) {
                // Web Speech API: onresult
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val resultText = matches[0]
                    Log.d(TAG, "[WebSpeech::onresult] Speech transcription matched matches: $matches")
                    _recognizedText.value = resultText
                    processText(resultText)
                } else {
                    Log.d(TAG, "[WebSpeech::onresult] No matches transcripted.")
                    _recognizedText.value = "Não entendi"
                }
                _isListening.value = false
            }

            override fun onPartialResults(partialResults: Bundle?) {}

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    fun startListening(onCommand: (String) -> Unit) {
        runOnMainThread {
            this.onCommandReceived = onCommand
            
            // To prevent ERROR_RECOGNIZER_BUSY, always cancel/reset previous active sessions
            try {
                speechRecognizer?.cancel()
            } catch (e: Exception) {
                Log.w(TAG, "Error canceling previous speech session: ${e.message}")
            }

            if (speechRecognizer == null) {
                Log.w(TAG, "SpeechRecognizer was null, recreating instance.")
                recreateSpeechRecognizer()
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "pt-BR")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }

            try {
                speechRecognizer?.startListening(intent)
                _isListening.value = true
                _recognizedText.value = "Ouvindo comando..."
                Log.d(TAG, "Speech recognition session initiated.")
            } catch (e: Exception) {
                Log.e(TAG, "Error starting speech listener: ${e.message}", e)
                _recognizedText.value = "Erro ao iniciar escuta"
                _isListening.value = false
                // Attempt to heal the session by recreating
                recreateSpeechRecognizer()
            }
        }
    }

    fun stopListening() {
        runOnMainThread {
            try {
                speechRecognizer?.stopListening()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping speech recognizer: ${e.message}", e)
            }
            _isListening.value = false
        }
    }

    fun destroy() {
        runOnMainThread {
            try {
                speechRecognizer?.destroy()
            } catch (e: Exception) {
                Log.e(TAG, "Error destroying speech recognizer: ${e.message}", e)
            }
            speechRecognizer = null
            _isListening.value = false
        }
    }

    private fun processText(text: String) {
        val cleanText = text.lowercase(Locale.getDefault()).trim()
        Log.d(TAG, "Processing voice command matching: $cleanText")
        
        val acceptKeywords = listOf("aceitar", "quero", "sim", "aceito", "pegar", "confirmar", "ok", "bora", "fechar", "fechado")
        val rejectKeywords = listOf("rejeitar", "recusar", "não", "nao", "recuso", "rejeito", "cancelar", "pular", "esquece", "ignorar", "próximo", "proximo")

        val matchAccept = acceptKeywords.any { cleanText.contains(it) }
        val matchReject = rejectKeywords.any { cleanText.contains(it) }

        if (matchAccept && !matchReject) {
            onCommandReceived?.invoke("aceitar")
        } else if (matchReject && !matchAccept) {
            onCommandReceived?.invoke("recusar")
        } else {
            Log.d(TAG, "Voice command transcript did not match accept/reject action criteria.")
        }
    }
}
