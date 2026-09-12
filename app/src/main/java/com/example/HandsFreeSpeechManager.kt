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
    RADAR_OFF,
    SEARCH_MERGED,
    // Comandos de Interação Natural com o Radar
    READ_OFFER,
    READ_EARNINGS,
    OPEN_NAVIGATION,
    READ_HEALTH,
    HELP,
    // Comandos de Alteração de Filtros por Voz
    FILTER_RAIN_PRESET,
    FILTER_SHORT_PRESET,
    FILTER_MAX_PROFIT_PRESET,
    FILTER_RESET,
    FILTER_ONLY_MERGED,
    FILTER_ONLY_JARVIS,
    FILTER_MIN_15,
    FILTER_MIN_20,
    FILTER_MIN_30
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

    private var isMutedForTts: Boolean = false

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

    /**
     * Pausa temporariamente a escuta enquanto o TTS estiver anunciando algo
     * para evitar que o microfone capture o próprio áudio gerado pelo aparelho (Acoustic Echo).
     */
    fun pauseForTts() {
        isMutedForTts = true
        restartJob?.cancel()
        scope.launch {
            try {
                speechRecognizer?.stopListening()
                _state.value = _state.value.copy(isListening = false)
            } catch (_: Exception) {}
        }
    }

    /**
     * Retoma a escuta contínua logo após o término da fala do TTS.
     */
    fun resumeAfterTts() {
        isMutedForTts = false
        if (isShouldBeListening) {
            scheduleRestartListening(delayMs = 400L)
        }
    }

    private fun scheduleRestartListening(delayMs: Long = 1200L) {
        if (!isShouldBeListening || isMutedForTts) return
        restartJob?.cancel()
        restartJob = scope.launch {
            delay(delayMs) // Pausa de recuperação para não sobrecarregar o microfone
            if (isShouldBeListening && !isMutedForTts) {
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

        // 3. Comandos de Leitura de Oferta em Voz Alta
        if (t.contains("ler oferta") || t.contains("ler corrida") || t.contains("ler pedido") ||
            t.contains("ouvir oferta") || t.contains("ouvir corrida") || t.contains("detalhes") ||
            t.contains("falar oferta") || t.contains("falar pedido") || t.contains("qual é a oferta") ||
            t.contains("qual a oferta") || t.contains("o que tem") || t.contains("anunciar")
        ) {
            return VoiceActionCommand.READ_OFFER
        }

        // 4. Comandos de Ganhos / Saldo do Dia
        if (t.contains("saldo") || t.contains("ganhos") || t.contains("quanto ganhei") ||
            t.contains("faturamento") || t.contains("extrato") || t.contains("meu saldo") ||
            t.contains("total hoje") || t.contains("lucro de hoje")
        ) {
            return VoiceActionCommand.READ_EARNINGS
        }

        // 5. Comandos de Rota / Navegação GPS para Coleta
        if (t.contains("navegar") || t.contains("abrir rota") || t.contains("rota") ||
            t.contains("abrir mapa") || t.contains("como chegar") || t.contains("iniciar rota") ||
            t.contains("abrir gps")
        ) {
            return VoiceActionCommand.OPEN_NAVIGATION
        }

        // 6. Comandos de Diagnóstico / Saúde do Sistema
        if (t.contains("saúde") || t.contains("saude") || t.contains("diagnóstico") ||
            t.contains("bateria") || t.contains("sinal gps") || t.contains("precisão gps")
        ) {
            return VoiceActionCommand.READ_HEALTH
        }

        // 7. Ajuda / Lista de Comandos Disponíveis
        if (t.contains("ajuda") || t.contains("comandos") || t.contains("o que posso falar") ||
            t.contains("quais comandos") || t.contains("socorro")
        ) {
            return VoiceActionCommand.HELP
        }

        // 3. Comandos de Alteração de Filtro por Voz (Direção Segura)
        if (t.contains("chuva") || t.contains("tarifa dinâmica") || t.contains("temporal")) {
            return VoiceActionCommand.FILTER_RAIN_PRESET
        }
        if (t.contains("tiro curto") || t.contains("filtro curto") || t.contains("corridas curtas") || t.contains("curta distância") || t.contains("curtas")) {
            return VoiceActionCommand.FILTER_SHORT_PRESET
        }
        if (t.contains("máximo lucro") || t.contains("maximo lucro") || t.contains("filtro lucro") || t.contains("alta rentabilidade") || t.contains("lucro alto")) {
            return VoiceActionCommand.FILTER_MAX_PROFIT_PRESET
        }
        if (t.contains("limpar filtro") || t.contains("limpar filtros") || t.contains("resetar filtro") || t.contains("resetar filtros") ||
            t.contains("redefinir filtro") || t.contains("redefinir filtros") || t.contains("sem filtro") || t.contains("tirar filtro") || t.contains("padrão livre")
        ) {
            return VoiceActionCommand.FILTER_RESET
        }
        if (t.contains("somente mesclada") || t.contains("somente mescladas") || t.contains("só mesclada") || t.contains("só mescladas") ||
            t.contains("filtro mesclada") || t.contains("filtro mescladas") || t.contains("apenas mescladas")
        ) {
            return VoiceActionCommand.FILTER_ONLY_MERGED
        }
        if (t.contains("somente jarvis") || t.contains("só jarvis") || t.contains("filtro jarvis") || t.contains("modo inteligente") || t.contains("recomendações jarvis")) {
            return VoiceActionCommand.FILTER_ONLY_JARVIS
        }
        if (t.contains("mínimo 15") || t.contains("minimo 15") || t.contains("quinze reais") || t.contains("15 reais")) {
            return VoiceActionCommand.FILTER_MIN_15
        }
        if (t.contains("mínimo 20") || t.contains("minimo 20") || t.contains("vinte reais") || t.contains("20 reais")) {
            return VoiceActionCommand.FILTER_MIN_20
        }
        if (t.contains("mínimo 30") || t.contains("minimo 30") || t.contains("trinta reais") || t.contains("30 reais")) {
            return VoiceActionCommand.FILTER_MIN_30
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

        // Comandos de Busca de Entregas Mescladas (Multi-Stack)
        if (t.contains("mesclada") || t.contains("mescladas") || t.contains("multi stack") ||
            t.contains("multistack") || t.contains("varrer") || t.contains("buscar mesclada") ||
            t.contains("entregas combinadas") || t.contains("combinar pedidos")
        ) {
            return VoiceActionCommand.SEARCH_MERGED
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
