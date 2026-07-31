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

    // OK Google / Ok Jarvis Wake-word variables
    private var isWakeActive = false
    private var lastTtsFinishedTime = 0L
    var shouldAutoWakeOnTtsFinish = false

    private val _jarvisVoiceState = MutableStateFlow("IDLE")
    val jarvisVoiceState: StateFlow<String> = _jarvisVoiceState.asStateFlow()

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
                Log.d(TAG, "[WebSpeech::onstart] Speech recognition service has started listening.")
                _isListening.value = true
                val currentState = if (isWakeActive) "LISTENING_COMMAND" else "LISTENING_WAKEWORD"
                updateExternalVoiceState(currentState, "")
            }

            override fun onBeginningOfSpeech() {
                Log.d(TAG, "[WebSpeech::onspeechstart] Active speech/voice input detected.")
                val currentState = if (isWakeActive) "LISTENING_COMMAND" else "LISTENING_WAKEWORD"
                updateExternalVoiceState(currentState, "Ouvindo...")
            }

            override fun onRmsChanged(rmsdB: Float) {
                // Real-time sound level tracking
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                Log.d(TAG, "[WebSpeech::onspeechend] User finished speaking.")
                _isListening.value = false
            }

            override fun onError(error: Int) {
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

                val currentState = if (isWakeActive) "LISTENING_COMMAND" else "LISTENING_WAKEWORD"
                updateExternalVoiceState(currentState, "")

                // Auto-recover if client is busy or broken
                if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY || error == SpeechRecognizer.ERROR_CLIENT) {
                    Log.w(TAG, "Speech recognizer is busy or errored. Recreating session to recover.")
                    recreateSpeechRecognizer()
                }

                // Auto-restart if continuous frequency is enabled and active
                checkAndScheduleContinuousRestart()
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val resultText = matches[0]
                    Log.d(TAG, "[WebSpeech::onresult] Speech transcription matched matches: $matches")
                    _recognizedText.value = resultText
                    processText(resultText)
                } else {
                    Log.d(TAG, "[WebSpeech::onresult] No matches transcripted.")
                    _recognizedText.value = "Não entendi"
                    val currentState = if (isWakeActive) "LISTENING_COMMAND" else "LISTENING_WAKEWORD"
                    updateExternalVoiceState(currentState, "Não entendi")
                }
                _isListening.value = false

                // Auto-restart if continuous frequency is enabled and active
                checkAndScheduleContinuousRestart()
            }

            override fun onPartialResults(partialResults: Bundle?) {}

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private var isJarvisMode = false
    private var isContinuousListeningActive = false

    private fun checkAndScheduleContinuousRestart() {
        val settings = try {
            com.example.coordinator.RadarCoordinator.settings.value
        } catch (e: Exception) {
            null
        }
        if (settings?.jarvisContinuousFrequency == true && isContinuousListeningActive) {
            scheduleRestart()
        }
    }

    private fun scheduleRestart() {
        mainHandler.removeCallbacks(restartRunnable)
        mainHandler.postDelayed(restartRunnable, 1000) // Snapier 1000ms delay for natural conversations
    }

    private val restartRunnable = Runnable {
        if (isContinuousListeningActive) {
            val settings = try {
                com.example.coordinator.RadarCoordinator.settings.value
            } catch (e: Exception) {
                null
            }
            if (settings?.jarvisContinuousFrequency == true) {
                val isSpeaking = try {
                    com.example.coordinator.RadarCoordinator.voiceManager?.isSpeaking() ?: false
                } catch (e: Exception) {
                    false
                }
                if (isSpeaking) {
                    Log.d(TAG, "Jarvis TTS está falando. Postergando a reinicialização da sintonia.")
                    scheduleRestart()
                } else {
                    if (shouldAutoWakeOnTtsFinish) {
                        shouldAutoWakeOnTtsFinish = false
                        isWakeActive = true
                        updateExternalVoiceState("LISTENING_COMMAND", "Ouvindo...")
                        Log.d(TAG, "Auto-Wake triggered: TTS finished speaking offer announcement.")
                    }
                    Log.d(TAG, "Frequência Contínua: Reiniciando escuta ativa do Jarvis...")
                    startListeningInternal()
                }
            }
        }
    }

    fun startListening(isJarvis: Boolean = false, onCommand: (String) -> Unit) {
        runOnMainThread {
            this.isContinuousListeningActive = true
            this.isJarvisMode = isJarvis
            this.onCommandReceived = onCommand
            this.isWakeActive = false
            updateExternalVoiceState("LISTENING_WAKEWORD", "")
            startListeningInternal()
        }
    }

    private fun startListeningInternal() {
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.RECORD_AUDIO
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "RECORD_AUDIO permission is not granted. Cannot start listening.")
            _recognizedText.value = "Sem permissão de áudio"
            _isListening.value = false
            return
        }

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
            _recognizedText.value = "Ouvindo..."
            Log.d(TAG, "Speech recognition session initiated.")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting speech listener: ${e.message}", e)
            _recognizedText.value = "Erro ao iniciar escuta"
            _isListening.value = false
            // Attempt to heal the session by recreating
            recreateSpeechRecognizer()
        }
    }

    fun stopListening() {
        runOnMainThread {
            this.isContinuousListeningActive = false
            mainHandler.removeCallbacks(restartRunnable)
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping speech recognizer: ${e.message}", e)
            }
            _isListening.value = false
            isWakeActive = false
            updateExternalVoiceState("IDLE", "")
        }
    }

    fun destroy() {
        runOnMainThread {
            this.isContinuousListeningActive = false
            mainHandler.removeCallbacks(restartRunnable)
            try {
                speechRecognizer?.destroy()
            } catch (e: Exception) {
                Log.e(TAG, "Error destroying speech recognizer: ${e.message}", e)
            }
            speechRecognizer = null
            _isListening.value = false
            isWakeActive = false
            updateExternalVoiceState("IDLE", "")
        }
    }

    private fun updateExternalVoiceState(state: String, text: String) {
        _jarvisVoiceState.value = state
        try {
            val updated = com.example.coordinator.RadarCoordinator.settings.value.copy(
                jarvisVoiceState = state,
                jarvisRecognizedText = text
            )
            com.example.coordinator.RadarCoordinator.updateSettings(updated)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update settings with voice state: ${e.message}")
        }
        com.example.data.FirestoreManager.uploadJarvisVoiceState(state, text)
    }

    private fun playWakeUpSound() {
        try {
            val toneG = android.media.ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 100)
            toneG.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 150)
            Handler(Looper.getMainLooper()).postDelayed({
                toneG.release()
            }, 500)
        } catch (e: Exception) {
            Log.e(TAG, "Error playing wake-up tone: ${e.message}")
        }
    }

    private fun speakConfirmation() {
        val confirmations = listOf(
            "Sim, Thiago?",
            "Estou ouvindo.",
            "Diga, chefe.",
            "Radar ativo.",
            "Pois não, Thiago?"
        )
        val reply = confirmations.random()
        com.example.coordinator.RadarCoordinator.voiceManager?.speak(reply)
    }

    fun activateWakeState() {
        runOnMainThread {
            this.isWakeActive = true
            this.lastTtsFinishedTime = System.currentTimeMillis()
            updateExternalVoiceState("LISTENING_COMMAND", "Ouvindo...")
            Log.d(TAG, "Smart Wake: Listening active command state triggered.")
        }
    }

    private fun processText(text: String) {
        val cleanText = text.lowercase(Locale.getDefault()).trim()
        Log.d(TAG, "Speech matched: '$text' (WakeActive: $isWakeActive)")

        // Check if wake-word is mentioned
        val wakeWords = listOf(
            "jarvis", "ok jarvis", "hey jarvis", "ei jarvis", "oi jarvis", 
            "ok google", "ei google", "ok radar", "ei radar", "copiloto"
        )
        val containsWake = wakeWords.any { cleanText.contains(it) }

        if (!isWakeActive && containsWake) {
            Log.d(TAG, "Wake word detected in: '$text'")
            
            var commandAfterWake = ""
            for (wake in wakeWords) {
                if (cleanText.contains(wake)) {
                    val index = cleanText.indexOf(wake) + wake.length
                    if (index < cleanText.length) {
                        commandAfterWake = cleanText.substring(index).trim()
                    }
                    break
                }
            }

            playWakeUpSound()
            
            if (commandAfterWake.isNotBlank() && commandAfterWake.length >= 3) {
                Log.d(TAG, "Woke up and found command in the same sentence: '$commandAfterWake'")
                isWakeActive = true
                updateExternalVoiceState("PROCESSING", text)
                executeParsedCommand(commandAfterWake)
                isWakeActive = false
            } else {
                Log.d(TAG, "Woke up. Entering active command listening window.")
                isWakeActive = true
                speakConfirmation()
                updateExternalVoiceState("LISTENING_COMMAND", "Diga, Thiago...")
            }
            return
        }

        if (isWakeActive) {
            updateExternalVoiceState("PROCESSING", text)
            executeParsedCommand(cleanText)
            isWakeActive = false
            return
        }

        // Direct Shortcuts allowed without Wake Word
        val instantShortcuts = listOf("aceitar", "recusar", "rejeitar", "fechar", "cancelar")
        val isInstantShortcut = instantShortcuts.any { cleanText == it }
        if (isInstantShortcut) {
            Log.d(TAG, "Instant shortcut matched: '$cleanText'")
            updateExternalVoiceState("PROCESSING", text)
            executeParsedCommand(cleanText)
            return
        }

        Log.d(TAG, "Ambient speech ignored: '$text'")
        updateExternalVoiceState("LISTENING_WAKEWORD", "")
    }

    private fun executeParsedCommand(cleanText: String) {
        val settings = com.example.coordinator.RadarCoordinator.settings.value
        val customAccept = settings.voiceCmdAccept.lowercase(Locale.getDefault()).trim()
        val customReject = settings.voiceCmdReject.lowercase(Locale.getDefault()).trim()
        val customSupport = settings.voiceCmdSupport.lowercase(Locale.getDefault()).trim()
        val customVip = settings.voiceCmdVip.lowercase(Locale.getDefault()).trim()

        val acceptKeywords = listOf("aceitar", "quero", "sim", "aceito", "pegar", "confirmar", "ok", "bora", "fechar", "fechado")
        val rejectKeywords = listOf("rejeitar", "recusar", "não", "nao", "recuso", "rejeito", "cancelar", "pular", "esquece", "ignorar", "próximo", "proximo")

        val matchAccept = acceptKeywords.any { cleanText.contains(it) } || (customAccept.isNotEmpty() && cleanText.contains(customAccept))
        val matchReject = rejectKeywords.any { cleanText.contains(it) } || (customReject.isNotEmpty() && cleanText.contains(customReject))
        val matchSupport = (customSupport.isNotEmpty() && cleanText.contains(customSupport)) || cleanText.contains("suporte")
        val matchVip = (customVip.isNotEmpty() && cleanText.contains(customVip)) || cleanText.contains("vip")

        val matchQuickReply1 = settings.quickReply1Cmd.isNotEmpty() && cleanText.contains(settings.quickReply1Cmd.lowercase(Locale.getDefault()).trim())
        val matchQuickReply2 = settings.quickReply2Cmd.isNotEmpty() && cleanText.contains(settings.quickReply2Cmd.lowercase(Locale.getDefault()).trim())
        val matchQuickReply3 = settings.quickReply3Cmd.isNotEmpty() && cleanText.contains(settings.quickReply3Cmd.lowercase(Locale.getDefault()).trim())

        if (matchQuickReply1) {
            onCommandReceived?.invoke("quick_reply_1")
        } else if (matchQuickReply2) {
            onCommandReceived?.invoke("quick_reply_2")
        } else if (matchQuickReply3) {
            onCommandReceived?.invoke("quick_reply_3")
        } else if (matchSupport) {
            onCommandReceived?.invoke("suporte")
        } else if (matchVip) {
            onCommandReceived?.invoke("vip")
        } else if (cleanText.contains("status") || cleanText.contains("painel") || cleanText.contains("velocidade")) {
            onCommandReceived?.invoke("status")
        } else if (cleanText.contains("clima") || cleanText.contains("chuva") || cleanText.contains("previsão") || cleanText.contains("previsao")) {
            onCommandReceived?.invoke("clima")
        } else if (cleanText.contains("meta") || cleanText.contains("ganho") || cleanText.contains("dinheiro") || cleanText.contains("faturamento") || cleanText.contains("acumulado") || cleanText.contains("resumo do desempenho") || cleanText.contains("desempenho da semana") || cleanText.contains("resumo da semana")) {
            onCommandReceived?.invoke("metas")
        } else if (cleanText.contains("encerrar turno") || cleanText.contains("fechar turno") || cleanText.contains("finalizar turno") || cleanText.contains("terminar turno") || cleanText.contains("fim de turno")) {
            onCommandReceived?.invoke("encerrar_turno")
        } else if (cleanText.contains("regra") || cleanText.contains("memória") || cleanText.contains("memoria") || cleanText.contains("aprendido") || cleanText.contains("preferências")) {
            onCommandReceived?.invoke("regras")
        } else if (cleanText.contains("ajuda") || cleanText.contains("comandos") || cleanText.contains("o que você faz") || cleanText.contains("o que voce faz")) {
            onCommandReceived?.invoke("ajuda")
        } else if (matchAccept && !matchReject) {
            onCommandReceived?.invoke("aceitar")
        } else if (matchReject && !matchAccept) {
            onCommandReceived?.invoke("recusar")
        } else {
            Log.d(TAG, "Passed full text to slow-path processor: '$cleanText'")
            onCommandReceived?.invoke(cleanText)
        }
    }
}
