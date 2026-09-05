package com.example

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Estado do reconhecedor de voz viva-voz
 */
data class VoiceCommandState(
    val isListening: Boolean = false,
    val lastRecognizedText: String = "",
    val detectedCommand: VoiceActionCommand? = null,
    val isPermissionGranted: Boolean = false,
    val errorMessage: String? = null
)

enum class VoiceActionCommand {
    ACCEPT,
    DECLINE,
    FOCUS_ON,
    FOCUS_OFF,
    RADAR_ON,
    RADAR_OFF
}

/**
 * Gerenciador mãos-livres (Hands-Free) com SpeechRecognizer nativo do Android.
 * Permite ao entregador aceitar ("aceitar", "sim", "pegar", "confirma")
 * ou rejeitar ("recusar", "não", "pular", "cancela") chamadas de voz com capacete/fone Bluetooth.
 */
class HandsFreeSpeechManager(
    private val context: Context,
    private val onCommandRecognized: (VoiceActionCommand, String) -> Unit
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var isShouldBeListening: Boolean = false
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var restartJob: Job? = null

    private val _state = MutableStateFlow(VoiceCommandState())
    val state: StateFlow<VoiceCommandState> = _state.asStateFlow()

    private val recognitionIntent: Intent by lazy {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "pt-BR")
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "pt-BR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _state.value = _state.value.copy(isListening = true, errorMessage = null)
        }

        override fun onBeginningOfSpeech() {}

        override fun onRmsChanged(rmsdB: Float) {}

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            _state.value = _state.value.copy(isListening = false)
        }

        override fun onError(error: Int) {
            _state.value = _state.value.copy(isListening = false)
            Log.d("HandsFreeSpeech", "SpeechRecognizer error: $error")
            // Se ainda deve estar ouvindo, reinicia após uma pausa curta
            scheduleRestartListening()
        }

        override fun onResults(results: Bundle?) {
            _state.value = _state.value.copy(isListening = false)
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                val fullText = matches.first().lowercase(Locale.getDefault())
                processSpokenText(fullText)
            }
            scheduleRestartListening()
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val partialMatches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!partialMatches.isNullOrEmpty()) {
                val partial = partialMatches.first().lowercase(Locale.getDefault())
                _state.value = _state.value.copy(lastRecognizedText = partial)
                checkImmediateKeywords(partial)
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    init {
        scope.launch {
            try {
                if (SpeechRecognizer.isRecognitionAvailable(context)) {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                        setRecognitionListener(listener)
                    }
                } else {
                    _state.value = _state.value.copy(
                        errorMessage = "SpeechRecognizer indisponível no dispositivo"
                    )
                }
            } catch (e: Exception) {
                Log.e("HandsFreeSpeech", "Erro inicializando SpeechRecognizer: ${e.message}")
            }
        }
    }

    /**
     * Inicia a escuta contínua de comandos de voz
     */
    fun startListening() {
        isShouldBeListening = true
        restartJob?.cancel()
        scope.launch {
            try {
                if (speechRecognizer == null && SpeechRecognizer.isRecognitionAvailable(context)) {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                        setRecognitionListener(listener)
                    }
                }
                speechRecognizer?.startListening(recognitionIntent)
                _state.value = _state.value.copy(isListening = true)
            } catch (e: Exception) {
                Log.e("HandsFreeSpeech", "Erro ao iniciar escuta: ${e.message}")
                scheduleRestartListening()
            }
        }
    }

    /**
     * Pausa a escuta de comandos de voz
     */
    fun stopListening() {
        isShouldBeListening = false
        restartJob?.cancel()
        scope.launch {
            try {
                speechRecognizer?.stopListening()
                _state.value = _state.value.copy(isListening = false)
            } catch (_: Exception) {}
        }
    }

    private fun scheduleRestartListening() {
        if (!isShouldBeListening) return
        restartJob?.cancel()
        restartJob = scope.launch {
            delay(1200L) // Pausa de recuperação para não sobrecarregar o microfone
            if (isShouldBeListening) {
                try {
                    speechRecognizer?.startListening(recognitionIntent)
                    _state.value = _state.value.copy(isListening = true)
                } catch (e: Exception) {
                    Log.d("HandsFreeSpeech", "Tentativa de restart: ${e.message}")
                }
            }
        }
    }

    /**
     * Processa a frase final reconhecida
     */
    private fun processSpokenText(spoken: String) {
        _state.value = _state.value.copy(lastRecognizedText = spoken)
        val command = parseCommand(spoken)
        if (command != null) {
            _state.value = _state.value.copy(detectedCommand = command)
            HapticFeedbackHelper.vibrateVoiceCommandRecognized(context)
            onCommandRecognized(command, spoken)
        }
    }

    /**
     * Verificação precoce para respostas instantâneas no partial result
     */
    private fun checkImmediateKeywords(spoken: String) {
        val command = parseCommand(spoken)
        if (command != null) {
            _state.value = _state.value.copy(detectedCommand = command)
            HapticFeedbackHelper.vibrateVoiceCommandRecognized(context)
            onCommandRecognized(command, spoken)
            // Para a sessão atual após comando executado e agenda nova escuta
            try {
                speechRecognizer?.stopListening()
            } catch (_: Exception) {}
        }
    }

    /**
     * Permite simular ou injetar diretamente uma frase falada no motor de reconhecimento de voz.
     * Útil para botões de atalho viva-voz na tela e testes de integração.
     */
    fun simulateVoiceCommand(spokenText: String) {
        processSpokenText(spokenText.lowercase(Locale.getDefault()))
    }

    /**
     * Analisador léxico de intenções de voz para condução de motocicleta.
     * Foco principal em comandos simples como 'aceitar' ou 'cancelar'.
     */
    private fun parseCommand(text: String): VoiceActionCommand? {
        val t = text.lowercase(Locale.getDefault()).trim()

        // 1. Palavras de Aceite (Prioridade: 'aceitar')
        if (t == "aceitar" || t == "aceita" || t == "aceito" || t == "aceite" ||
            t.contains("aceitar") || t.contains("aceito") || t.contains("aceita") ||
            t == "sim" || t == "confirmar" || t == "confirma" ||
            t.contains("confirmar") || t.contains("confirma") ||
            t.contains("pegar") || t.contains("vou aceitar") || t.contains("pode aceitar")
        ) {
            return VoiceActionCommand.ACCEPT
        }

        // 2. Palavras de Recusa / Cancelamento (Prioridade: 'cancelar')
        if (t == "cancelar" || t == "cancela" || t == "cancelado" ||
            t.contains("cancelar") || t.contains("cancela") ||
            t == "recusar" || t == "recusa" || t == "rejeitar" ||
            t.contains("recusar") || t.contains("recusa") || t.contains("rejeitar") ||
            t == "não" || t == "nao" || t.contains("deixa passar") ||
            t.contains("descartar") || t.contains("dispensar") || t.contains("pular")
        ) {
            return VoiceActionCommand.DECLINE
        }

        // Comandos de Modo Foco
        if (t.contains("modo foco ativar") || t.contains("ativar foco") || t.contains("modo trânsito")) {
            return VoiceActionCommand.FOCUS_ON
        }
        if (t.contains("modo foco desativar") || t.contains("desativar foco") || t.contains("fechar foco")) {
            return VoiceActionCommand.FOCUS_OFF
        }

        // Comandos de Radar
        if (t.contains("ativar radar") || t.contains("ligar radar") || t.contains("iniciar radar")) {
            return VoiceActionCommand.RADAR_ON
        }
        if (t.contains("pausar radar") || t.contains("desligar radar") || t.contains("parar radar")) {
            return VoiceActionCommand.RADAR_OFF
        }

        return null
    }

    fun destroy() {
        isShouldBeListening = false
        restartJob?.cancel()
        scope.launch {
            try {
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (_: Exception) {}
        }
    }
}
