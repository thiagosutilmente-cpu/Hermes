package com.example

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
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
 * Estado do reconhecedor de voz viva-voz com a API Google Speech-to-Text
 */
data class VoiceCommandState(
    val isListening: Boolean = false,
    val rmsDb: Float = 0f,
    val audioLevelFraction: Float = 0f,
    val lastRecognizedText: String = "",
    val detectedCommand: VoiceActionCommand? = null,
    val isPermissionGranted: Boolean = false,
    val errorMessage: String? = null,
    val isGoogleSpeechActive: Boolean = true,
    val engineName: String = "Google Speech-to-Text"
)

enum class VoiceActionCommand {
    ACCEPT,
    DECLINE,
    ACCEPT_IFOOD,
    DECLINE_IFOOD,
    ACCEPT_RAPPI,
    DECLINE_RAPPI,
    ACCEPT_UBER,
    DECLINE_UBER,
    ACCEPT_99,
    DECLINE_99,
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
    FILTER_MIN_30,
    // Comandos de Auto-Aceite Inteligente por Voz
    AUTO_ACCEPT_ON,
    AUTO_ACCEPT_OFF,
    AUTO_ACCEPT_TOGGLE,
    // Comandos de Navegação de Telas Hands-Free
    OPEN_OFFERS_LIST,
    CLOSE_SCREEN
}

/**
 * Gerenciador mãos-livres (Hands-Free) com SpeechRecognizer nativo do Android.
 * Permite ao entregador aceitar ("aceitar", "sim", "pegar", "confirma", "bora")
 * ou rejeitar ("recusar", "não", "pular", "cancela", "deixa passar") chamadas de voz com capacete/fone Bluetooth.
 */
class HandsFreeSpeechManager(
    private val context: Context,
    private val onCommandRecognized: (VoiceActionCommand, String) -> Unit
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var isShouldBeListening: Boolean = false
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var restartJob: Job? = null
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

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
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Diga 'Aceitar' ou 'Recusar'")
            // Prioriza modelo offline no aparelho (Google Speech) para baixa latência em trânsito no guidão
            putExtra("android.speech.extra.PREFER_OFFLINE", true)
            putExtra("android.speech.extra.DICTATION_MODE", true)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1200L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 800L)
        }
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _state.value = _state.value.copy(isListening = true, errorMessage = null)
        }

        override fun onBeginningOfSpeech() {
            _state.value = _state.value.copy(isListening = true)
        }

        override fun onRmsChanged(rmsdB: Float) {
            val level = if (rmsdB > -2f) {
                ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
            } else 0f
            _state.value = _state.value.copy(
                rmsDb = if (rmsdB > 0f) rmsdB else 0f,
                audioLevelFraction = level
            )
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            _state.value = _state.value.copy(isListening = false, rmsDb = 0f, audioLevelFraction = 0f)
        }

        override fun onError(error: Int) {
            _state.value = _state.value.copy(isListening = false, rmsDb = 0f, audioLevelFraction = 0f)
            Log.d("HandsFreeSpeech", "SpeechRecognizer error: $error")
            when (error) {
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                    try {
                        speechRecognizer?.cancel()
                    } catch (_: Exception) {}
                    scheduleRestartListening(800L)
                }
                SpeechRecognizer.ERROR_CLIENT -> {
                    recreateRecognizer()
                    scheduleRestartListening(1000L)
                }
                SpeechRecognizer.ERROR_NO_MATCH,
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                    // Sem voz detectada no intervalo (situação comum em condução de moto com barulho de trânsito)
                    scheduleRestartListening(500L)
                }
                SpeechRecognizer.ERROR_NETWORK,
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
                SpeechRecognizer.ERROR_SERVER -> {
                    // Instabilidade de rede no trânsito: aguarda breve reconexão
                    scheduleRestartListening(1200L)
                }
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                    _state.value = _state.value.copy(
                        errorMessage = "Permissão de microfone necessária para comandos por voz"
                    )
                }
                else -> {
                    scheduleRestartListening(1200L)
                }
            }
        }

        override fun onResults(results: Bundle?) {
            _state.value = _state.value.copy(isListening = false, rmsDb = 0f, audioLevelFraction = 0f)
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                val fullText = matches.first().lowercase(Locale.getDefault())
                processSpokenText(fullText)
            }
            scheduleRestartListening(600L)
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
            initRecognizerInternal()
        }
    }

    private fun initRecognizerInternal() {
        try {
            var recognizer: SpeechRecognizer? = null
            var engine = "Google Speech-to-Text"

            // 1. No Android 13+ (API 33+), tenta o motor on-device do Google para latência zero no guidão
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                try {
                    if (SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
                        recognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
                        engine = "Google Speech-to-Text (On-Device)"
                    }
                } catch (e: Exception) {
                    Log.d("HandsFreeSpeech", "Tentando fallback do Google Speech: ${e.message}")
                }
            }

            // 2. Tenta vincular diretamente ao serviço oficial do Google Speech-to-Text
            if (recognizer == null) {
                val googleSearchComponent = ComponentName(
                    "com.google.android.googlequicksearchbox",
                    "com.google.android.voicesearch.serviceapi.GoogleRecognitionService"
                )
                try {
                    recognizer = SpeechRecognizer.createSpeechRecognizer(context, googleSearchComponent)
                    engine = "Google Speech-to-Text Service"
                } catch (e: Exception) {
                    Log.d("HandsFreeSpeech", "Fallback ComponentName do Google: ${e.message}")
                }
            }

            // 3. Fallback para o reconhecedor padrão do sistema
            if (recognizer == null && SpeechRecognizer.isRecognitionAvailable(context)) {
                recognizer = SpeechRecognizer.createSpeechRecognizer(context)
                engine = "Google Speech-to-Text"
            }

            if (recognizer != null) {
                speechRecognizer = recognizer.apply {
                    setRecognitionListener(listener)
                }
                _state.value = _state.value.copy(
                    isGoogleSpeechActive = true,
                    engineName = engine,
                    errorMessage = null
                )
            } else {
                _state.value = _state.value.copy(
                    isGoogleSpeechActive = false,
                    engineName = "Indisponível",
                    errorMessage = "API de Speech-to-Text do Google indisponível no dispositivo"
                )
            }
        } catch (e: Exception) {
            Log.e("HandsFreeSpeech", "Erro inicializando SpeechRecognizer: ${e.message}")
            _state.value = _state.value.copy(errorMessage = e.message)
        }
    }

    private fun recreateRecognizer() {
        try {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
        initRecognizerInternal()
    }

    /**
     * Inicia a escuta contínua de comandos de voz
     */
    fun startListening() {
        isShouldBeListening = true
        restartJob?.cancel()
        enableBluetoothScoIfAvailable()
        scope.launch {
            try {
                if (speechRecognizer == null && SpeechRecognizer.isRecognitionAvailable(context)) {
                    initRecognizerInternal()
                }
                // Cancela qualquer sessão residual pendente antes de disparar nova escuta
                try {
                    speechRecognizer?.cancel()
                } catch (_: Exception) {}

                speechRecognizer?.startListening(recognitionIntent)
                _state.value = _state.value.copy(isListening = true, errorMessage = null)
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
                speechRecognizer?.cancel()
                _state.value = _state.value.copy(isListening = false, rmsDb = 0f)
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
                speechRecognizer?.cancel()
                _state.value = _state.value.copy(isListening = false, rmsDb = 0f)
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

    private fun scheduleRestartListening(delayMs: Long = 1000L) {
        if (!isShouldBeListening || isMutedForTts) return
        restartJob?.cancel()
        restartJob = scope.launch {
            delay(delayMs)
            if (isShouldBeListening && !isMutedForTts) {
                try {
                    speechRecognizer?.cancel()
                    speechRecognizer?.startListening(recognitionIntent)
                    _state.value = _state.value.copy(isListening = true)
                } catch (e: Exception) {
                    Log.d("HandsFreeSpeech", "Tentativa de restart: ${e.message}")
                }
            }
        }
    }

    /**
     * Habilita áudio de capacete / fone Bluetooth SCO se conectado
     */
    private fun enableBluetoothScoIfAvailable() {
        try {
            audioManager?.let { am ->
                if (am.isBluetoothScoAvailableOffCall && !am.isBluetoothScoOn) {
                    am.startBluetoothSco()
                    am.isBluetoothScoOn = true
                }
            }
        } catch (e: Exception) {
            Log.d("HandsFreeSpeech", "Bluetooth SCO setup: ${e.message}")
        }
    }

    /**
     * Desliga áudio Bluetooth SCO
     */
    private fun disableBluetoothSco() {
        try {
            audioManager?.let { am ->
                if (am.isBluetoothScoOn) {
                    am.isBluetoothScoOn = false
                    am.stopBluetoothSco()
                }
            }
        } catch (_: Exception) {}
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

    companion object {
        /**
         * Analisador léxico de intenções de voz para condução de motocicleta.
         * Foco principal em comandos simples como 'aceitar' ou 'cancelar'.
         */
        fun parseCommand(text: String): VoiceActionCommand? {
        val t = text.lowercase(Locale.getDefault()).trim()

        // 1. Aceite Específico de Aplicativo (Multi-app)
        if (t.contains("ifood") && (t.contains("aceit") || t.contains("peg") || t.contains("confirm") || t.contains("sim") || t.contains("bora"))) {
            return VoiceActionCommand.ACCEPT_IFOOD
        }
        if (t.contains("rappi") && (t.contains("aceit") || t.contains("peg") || t.contains("confirm") || t.contains("sim") || t.contains("bora"))) {
            return VoiceActionCommand.ACCEPT_RAPPI
        }
        if (t.contains("uber") && (t.contains("aceit") || t.contains("peg") || t.contains("confirm") || t.contains("sim") || t.contains("bora"))) {
            return VoiceActionCommand.ACCEPT_UBER
        }
        if ((t.contains("99") || t.contains("noventa e nove")) && (t.contains("aceit") || t.contains("peg") || t.contains("confirm") || t.contains("sim") || t.contains("bora"))) {
            return VoiceActionCommand.ACCEPT_99
        }

        // 2. Recusa Específica de Aplicativo
        if (t.contains("ifood") && (t.contains("recus") || t.contains("cancel") || t.contains("rejeit") || t.contains("não") || t.contains("nao") || t.contains("pul") || t.contains("descart"))) {
            return VoiceActionCommand.DECLINE_IFOOD
        }
        if (t.contains("rappi") && (t.contains("recus") || t.contains("cancel") || t.contains("rejeit") || t.contains("não") || t.contains("nao") || t.contains("pul") || t.contains("descart"))) {
            return VoiceActionCommand.DECLINE_RAPPI
        }
        if (t.contains("uber") && (t.contains("recus") || t.contains("cancel") || t.contains("rejeit") || t.contains("não") || t.contains("nao") || t.contains("pul") || t.contains("descart"))) {
            return VoiceActionCommand.DECLINE_UBER
        }
        if ((t.contains("99") || t.contains("noventa e nove")) && (t.contains("recus") || t.contains("cancel") || t.contains("rejeit") || t.contains("não") || t.contains("nao") || t.contains("pul") || t.contains("descart"))) {
            return VoiceActionCommand.DECLINE_99
        }

        // 3. Palavras de Aceite Geral (Prioridade Máxima do Entregador)
        if (t == "aceitar" || t == "aceita" || t == "aceito" || t == "aceite" ||
            t.contains("aceitar") || t.contains("aceito") || t.contains("aceita") ||
            t == "sim" || t == "confirmar" || t == "confirma" || t == "confirmado" ||
            t.contains("confirmar") || t.contains("confirma") ||
            t.contains("pegar") || t.contains("pega") || t.contains("pego") || t.contains("vou aceitar") ||
            t.contains("pode aceitar") || t.contains("pode pegar") || t.contains("bora") ||
            t.contains("topo") || t.contains("aceita ai") || t.contains("aceitar corrida") ||
            t.contains("aceitar pedido") || t.contains("pegar corrida") || t.contains("fechou") ||
            t.contains("manda") || t.contains("positivo") || t.contains("partiu") ||
            t.contains("vamos") || t.contains("vamo") || t.contains("vou") || t.contains("pega essa")
        ) {
            return VoiceActionCommand.ACCEPT
        }

        // 4. Palavras de Recusa / Cancelamento Geral
        if (t == "cancelar" || t == "cancela" || t == "cancelado" ||
            t.contains("cancelar") || t.contains("cancela") ||
            t == "recusar" || t == "recusa" || t == "recuso" || t == "rejeitar" || t == "rejeita" ||
            t.contains("recusar") || t.contains("recusa") || t.contains("rejeitar") ||
            t == "não" || t == "nao" || t.contains("deixa passar") || t.contains("passo") ||
            t.contains("descartar") || t.contains("descarta") || t.contains("dispensar") ||
            t.contains("dispensa") || t.contains("pular") || t.contains("pula") ||
            t.contains("negar") || t.contains("nega") || t.contains("recusar corrida") ||
            t.contains("recusar pedido") || t.contains("ruim") || t.contains("fora") ||
            t.contains("muito longe") || t.contains("longe demais") || t.contains("cancela essa")
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

        // Comandos de Auto-Aceite Inteligente
        if (t.contains("ativar auto aceite") || t.contains("ligar auto aceite") ||
            t.contains("ativar auto-aceite") || t.contains("ligar auto-aceite") ||
            t.contains("ligar aceite automático") || t.contains("ativar aceite automático")
        ) {
            return VoiceActionCommand.AUTO_ACCEPT_ON
        }
        if (t.contains("desativar auto aceite") || t.contains("desligar auto aceite") ||
            t.contains("desativar auto-aceite") || t.contains("desligar auto-aceite") ||
            t.contains("parar auto aceite") || t.contains("parar auto-aceite") ||
            t.contains("desligar aceite automático")
        ) {
            return VoiceActionCommand.AUTO_ACCEPT_OFF
        }
        if (t.contains("auto aceite") || t.contains("auto-aceite") ||
            t.contains("aceite automático") || t.contains("aceite automatico")
        ) {
            return VoiceActionCommand.AUTO_ACCEPT_TOGGLE
        }

        // Comandos de Navegação de Telas
        if (t.contains("abrir oferta") || t.contains("ver oferta") || t.contains("lista de oferta") ||
            t.contains("radar de oferta") || t.contains("mostrar oferta") || t.contains("tela de oferta")
        ) {
            return VoiceActionCommand.OPEN_OFFERS_LIST
        }
        if (t.contains("voltar") || t.contains("fechar") || t.contains("voltar ao painel") ||
            t.contains("voltar ao cockpit") || t.contains("voltar tela") || t.contains("painel principal")
        ) {
            return VoiceActionCommand.CLOSE_SCREEN
        }

        return null
    }
    }

    fun destroy() {
        isShouldBeListening = false
        restartJob?.cancel()
        disableBluetoothSco()
        scope.launch {
            try {
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (_: Exception) {}
        }
    }
}
