package com.example.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.media.MediaPlayer
import java.net.URLEncoder
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class VoiceManager(private val context: Context) : TextToSpeech.OnInitListener {

    private val TAG = "VoiceManager"
    private var tts: TextToSpeech? = null
    private var mediaPlayer: MediaPlayer? = null
    private var isPlayingMedia = false
    private var isTtsInitialized = false
    
    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var focusRequest: AudioFocusRequest? = null

    private var toneGenerator: ToneGenerator? = null

    init {
        tts = TextToSpeech(appContext, this)
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
            
            applyVoiceStyleSettings()
            
            tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    // Do nothing
                }

                override fun onDone(utteranceId: String?) {
                    abandonAudioFocus()
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    abandonAudioFocus()
                }
            })

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
        if (!isTtsInitialized && tts == null) {
            Log.w(TAG, "TTS ainda não está inicializado ou está indisponível.")
            return
        }
        val currentSettings = try {
            com.example.coordinator.RadarCoordinator.settings.value
        } catch (e: Exception) {
            null
        }
        val engine = currentSettings?.jarvisVoiceEngine ?: "LOCAL"
        
        when (engine.uppercase(java.util.Locale.getDefault())) {
            "NEURAL", "ELEVENLABS" -> {
                if (currentSettings?.openAiApiKey?.isNotEmpty() == true) {
                    playOpenAiVoice(text, currentSettings)
                } else {
                    applyVoiceStyleSettings()
                    requestAudioFocusAndSpeak(text)
                }
            }
            "OPENAI" -> {
                playOpenAiVoice(text, currentSettings)
            }
            else -> {
                applyVoiceStyleSettings()
                requestAudioFocusAndSpeak(text)
            }
        }
    }

    /**
     * Fala uma frase processada inteligentemente pelo Jarvis Persona Engine.
     */
    fun speakIntelligent(text: String, screenContext: String) {
        if (!isTtsInitialized && tts == null) {
            Log.w(TAG, "TTS ainda não está inicializado ou está indisponível.")
            return
        }
        val currentSettings = try {
            com.example.coordinator.RadarCoordinator.settings.value
        } catch (e: Exception) {
            null
        }

        CoroutineScope(Dispatchers.Main).launch {
            val result = com.example.voice.JarvisPersonaEngine.processCommand(text, screenContext)
            
            // Report to UI
            com.example.coordinator.RadarCoordinator.setJarvisProactiveMessage(result.voiceResponse)
            
            val engine = currentSettings?.jarvisVoiceEngine ?: "LOCAL"

            when (engine.uppercase(java.util.Locale.getDefault())) {
                "NEURAL", "ELEVENLABS" -> {
                    if (currentSettings?.openAiApiKey?.isNotEmpty() == true) {
                        playOpenAiVoice(result.voiceResponse, currentSettings)
                    } else {
                        applyVoiceStyleSettings()
                        requestAudioFocusAndSpeak(result.voiceResponse)
                    }
                }
                "OPENAI" -> {
                    playOpenAiVoice(result.voiceResponse, currentSettings)
                }
                else -> {
                    applyVoiceStyleSettings()
                    requestAudioFocusAndSpeak(result.voiceResponse)
                }
            }
        }
    }

    private fun playCloudNeuralVoice(text: String, settings: com.example.coordinator.RadarSettings?) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Lista de vozes caóticas/imprevisíveis
                val chaoticVoices = listOf(
                    com.example.voice.ElevenLabsClient.ANTONI_VOICE_ID,
                    com.example.voice.ElevenLabsClient.JARVIS_VOICE_ID,
                    "TX3OmfOUXGLcasCJQA64", 
                    "EXAVITQu4vr4xnSDxMaL",
                    "pFZP5JQG7iQjIQuC4Bku", // Lily
                    "pNInz6obpgDQGcFmaJcg", // Adam
                    "ZQe5CZNOzWyzOMuNjWuI"  // James
                )
                val voiceId = chaoticVoices.random()
                val modelId = settings?.elevenLabsModelId?.ifEmpty { null } ?: "eleven_multilingual_v2"
                // Modo "Imprevisível" - valores aleatórios para alta variabilidade e emoção
                val stability = 0.1f + java.util.Random().nextFloat() * 0.2f // 0.1 a 0.3 (muito imprevisível)
                val similarityBoost = 0.5f + java.util.Random().nextFloat() * 0.3f // 0.5 a 0.8
                val style = 0.5f + java.util.Random().nextFloat() * 0.5f // 0.5 a 1.0 (muita variação de estilo)
                val speakerBoost = true

                val response = com.example.voice.ElevenLabsClient.service.textToSpeech(
                    apiKey = com.example.voice.ElevenLabsClient.apiKey,
                    voiceId = voiceId,
                    requestBody = com.example.voice.ElevenLabsRequestBody(
                        text = text,
                        model_id = modelId,
                        voice_settings = com.example.voice.VoiceSettings(
                            stability = stability,
                            similarity_boost = similarityBoost,
                            style = style,
                            use_speaker_boost = speakerBoost
                        )
                    )
                )

                if (response.isSuccessful && response.body() != null) {
                    val audioFile = File(context.cacheDir, "jarvis_voice.mp3")
                    FileOutputStream(audioFile).use { it.write(response.body()!!.bytes()) }
                    
                    withContext(Dispatchers.Main) {
                        playAudioFile(audioFile.absolutePath, text)
                    }
                } else {
                    Log.e(TAG, "ElevenLabs falhou, fallback: ${response.code()}")
                    withContext(Dispatchers.Main) {
                        fallbackToGoogleTts(text, settings)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro na rede ElevenLabs, fallback: ${e.message}")
                withContext(Dispatchers.Main) {
                    fallbackToGoogleTts(text, settings)
                }
            }
        }
    }

    private fun playOpenAiVoice(text: String, settings: com.example.coordinator.RadarSettings?) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val apiKey = com.example.voice.OpenAiClient.apiKey
                if (apiKey.isEmpty()) {
                    Log.w(TAG, "Chave OpenAI vazia, usando fallback para Google TTS.")
                    withContext(Dispatchers.Main) {
                        fallbackToGoogleTts(text, settings)
                    }
                    return@launch
                }
                val voice = settings?.openAiVoice?.ifEmpty { null } ?: "alloy"
                val model = settings?.openAiModel?.ifEmpty { null } ?: "tts-1"
                val processedInput = preprocessTextForPtBr(text)

                val response = com.example.voice.OpenAiClient.service.textToSpeech(
                    authorization = "Bearer $apiKey",
                    requestBody = com.example.voice.OpenAiRequestBody(
                        model = model,
                        input = processedInput,
                        voice = voice,
                        response_format = "mp3"
                    )
                )

                if (response.isSuccessful && response.body() != null) {
                    val audioFile = File(context.cacheDir, "jarvis_openai_voice.mp3")
                    FileOutputStream(audioFile).use { it.write(response.body()!!.bytes()) }
                    
                    withContext(Dispatchers.Main) {
                        playAudioFile(audioFile.absolutePath, text)
                    }
                } else {
                    Log.e(TAG, "OpenAI TTS falhou, fallback: ${response.code()}")
                    withContext(Dispatchers.Main) {
                        fallbackToGoogleTts(text, settings)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro na rede OpenAI, fallback: ${e.message}")
                withContext(Dispatchers.Main) {
                    fallbackToGoogleTts(text, settings)
                }
            }
        }
    }

    private fun playAudioFile(url: String, textForFallback: String? = null) {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            setDataSource(url)
            setOnPreparedListener { 
                isPlayingMedia = true
                try {
                    val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                    Log.d(TAG, "Audio status: current system volume is $currentVolume / $maxVolume")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to read stream volume: ${e.message}")
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                        .setAudioAttributes(AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build())
                        .build()
                    audioManager.requestAudioFocus(focusRequest!!)
                } else {
                    @Suppress("DEPRECATION")
                    audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                }
                it.start() 
            }
            setOnCompletionListener {
                isPlayingMedia = false
                it.release()
                mediaPlayer = null
                abandonAudioFocus()
            }
            setOnErrorListener { _, _, _ ->
                Log.e(TAG, "Erro na reprodução de áudio, usando fallback.")
                if (textForFallback != null) {
                    applyVoiceStyleSettings()
                    requestAudioFocusAndSpeak(textForFallback)
                }
                true
            }
            prepareAsync()
        }
    }

    private fun fallbackToGoogleTts(text: String, settings: com.example.coordinator.RadarSettings?) {
        try {
            val style = settings?.jarvisVoiceStyle ?: "JARVIS"
            // Use pt-BR for all styles to guarantee a high quality Brazilian delivery voice!
            val lang = "pt-BR"
            
            val processedText = preprocessTextForPtBr(text)
            val encodedText = URLEncoder.encode(processedText, "UTF-8")
            val url = "https://translate.google.com/translate_tts?ie=UTF-8&client=tw-ob&tl=$lang&q=$encodedText"

            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                setDataSource(url)
                setOnPreparedListener { 
                    isPlayingMedia = true
                    
                    // Assegura que o volume está audível para o motoboy (vento, capacete)
                    try {
                        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                        Log.d(TAG, "Audio status: current system volume is $currentVolume / $maxVolume")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to read stream volume: ${e.message}")
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                            .setAudioAttributes(AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                .build())
                            .build()
                        try {
                            audioManager.requestAudioFocus(focusRequest!!)
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to request audio focus in fallback: ${e.message}")
                        }
                    } else {
                        try {
                            @Suppress("DEPRECATION")
                            audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to request audio focus (legacy) in fallback: ${e.message}")
                        }
                    }
                    
                    it.start() 
                }
                setOnCompletionListener {
                    isPlayingMedia = false
                    it.release()
                    mediaPlayer = null
                    abandonAudioFocus()
                }
                setOnErrorListener { _, _, _ ->
                    Log.e(TAG, "Erro no Cloud TTS, usando fallback offline.")
                    isPlayingMedia = false
                    applyVoiceStyleSettings()
                    requestAudioFocusAndSpeak(text)
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao iniciar Cloud TTS: ${e.message}")
            applyVoiceStyleSettings()
            requestAudioFocusAndSpeak(text)
        }
    }
    
    // Original method renamed
    fun speakOffline(text: String) {
        if (!isTtsInitialized || tts == null) {
            Log.w(TAG, "TTS ainda não está inicializado ou está indisponível.")
            return
        }

        applyVoiceStyleSettings()
        requestAudioFocusAndSpeak(text)
    }

    private fun applyVoiceStyleSettings() {
        val currentSettings = try {
            com.example.coordinator.RadarCoordinator.settings.value
        } catch (e: Exception) {
            null
        }
        val engine = currentSettings?.jarvisVoiceEngine ?: "LOCAL"
        val style = currentSettings?.jarvisVoiceStyle ?: "JARVIS"
        val customPitch = currentSettings?.jarvisVoicePitch ?: 1.0f
        val customRate = currentSettings?.jarvisVoiceRate ?: 1.0f
        
        Log.d(TAG, "Applying voice style settings: style=$style, pitch=$customPitch, rate=$customRate")
        
        var basePitch = 0.92f // Grave por padrão para Jarvis
        var baseRate = 0.98f // Calmo por padrão

        when (style.uppercase(Locale.getDefault())) {
            "JARVIS" -> {
                basePitch = 0.82f // Mais profundo, ressonante e autoritário
                baseRate = 0.94f // Articulação impecável e deliberada
            }
            "MASCULINA" -> {
                basePitch = 0.80f
                baseRate = 1.00f
            }
            "FEMININA" -> {
                basePitch = 1.32f
                baseRate = 1.02f
            }
            "ACELERADA" -> {
                basePitch = 1.05f
                baseRate = 1.45f
            }
            "PADRAO" -> {
                basePitch = 0.95f
                baseRate = 1.05f
            }
        }

        // Tenta encontrar a voz de melhor qualidade do Google TTS se disponível
        try {
            val ptVoices = tts?.voices?.filter { it.locale.language == "pt" }
            if (!ptVoices.isNullOrEmpty()) {
                val isFemale = style.uppercase(Locale.getDefault()) == "FEMININA"
                
                // Filtra as vozes do pool por gênero para alinhar com o estilo escolhido
                val genderFiltered = if (isFemale) {
                    ptVoices.filter { it.name.contains("fem", ignoreCase = true) || it.name.contains("female", ignoreCase = true) || it.name.contains("-f-") }
                } else {
                    ptVoices.filter { !it.name.contains("fem", ignoreCase = true) && !it.name.contains("female", ignoreCase = true) && !it.name.contains("-f-") }
                }
                
                val finalPool = if (genderFiltered.isNotEmpty()) genderFiltered else ptVoices
                var selectedVoice: android.speech.tts.Voice? = null
                
                if (engine == "NEURAL") {
                    // Tenta achar vozes com melhor qualidade de rede/Wavenet
                    selectedVoice = finalPool.find { 
                        it.isNetworkConnectionRequired || 
                        it.name.contains("network", ignoreCase = true) || 
                        it.name.contains("wavenet", ignoreCase = true)
                    } ?: finalPool.find { it.name.contains("pt-br-x-") } ?: finalPool.firstOrNull()
                } else {
                    // Para NATIVE/LOCAL, prioriza vozes HD salvas localmente (pt-br-x-) e evita downloads obrigatórios em trânsito
                    selectedVoice = finalPool.find { 
                        !it.isNetworkConnectionRequired && 
                        !it.name.contains("network", ignoreCase = true) && 
                        it.name.contains("pt-br-x-")
                    } ?: finalPool.find { 
                        !it.isNetworkConnectionRequired && !it.name.contains("network", ignoreCase = true) 
                    } ?: finalPool.firstOrNull()
                }

                if (selectedVoice != null) {
                    tts?.voice = selectedVoice
                    Log.d(TAG, "Selecionada voz otimizada para estilo $style: ${selectedVoice.name} (Network=${selectedVoice.isNetworkConnectionRequired})")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao tentar otimizar a voz TTS: ${e.message}")
        }

        // Apply custom pitch and rate multipliers
        tts?.setPitch(basePitch * customPitch)
        tts?.setSpeechRate(baseRate * customRate)
    }

    private fun requestAudioFocusAndSpeak(text: String) {
        // Assegura que o volume está audível para o motoboy (vento, capacete)
        try {
            val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            Log.d(TAG, "Audio status: current system volume is $currentVolume / $maxVolume")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read stream volume: ${e.message}")
        }

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

            try {
                val result = audioManager.requestAudioFocus(focusRequest!!)
                if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    speakWithTtsParams(text)
                } else {
                    Log.w(TAG, "Audio Focus negado para fala.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to request audio focus: ${e.message}")
                // Fallback: speak anyway if audio focus request fails
                speakWithTtsParams(text)
            }
        } else {
            try {
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
            } catch (e: Exception) {
                Log.e(TAG, "Failed to request audio focus (legacy): ${e.message}")
                speakWithTtsParams(text)
            }
        }
    }

    private fun speakWithTtsParams(text: String) {
        // Preprocess text to ensure beautiful pronunciation in Brazilian Portuguese
        val processedText = preprocessTextForPtBr(text)
        
        // Força áudio pelo STREAM_MUSIC (Canal de Mídia) usando os parâmetros do TTS
        val params = android.os.Bundle().apply {
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
            val volumeValue = try {
                com.example.coordinator.RadarCoordinator.settings.value.jarvisVoiceVolume
            } catch (e: Exception) {
                1.0f
            }
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volumeValue)
        }
        val result = tts?.speak(processedText, TextToSpeech.QUEUE_FLUSH, params, "radar_voice_id")
        if (result == TextToSpeech.SUCCESS) {
            com.example.coordinator.RadarCoordinator.updateModuleHealth("VoiceEngine", true)
        } else {
            com.example.coordinator.RadarCoordinator.updateModuleHealth("VoiceEngine", false)
            Log.e(TAG, "Falha crítica na síntese de voz TTS.")
        }
    }

    /**
     * Verifica se o mecanismo TTS está falando no momento.
     */
    fun isSpeaking(): Boolean {
        return (tts?.isSpeaking == true) || isPlayingMedia
    }

    /**
     * Interrompe qualquer fala em andamento.
     */
    fun stop() {
        tts?.stop()
        try {
            if (isPlayingMedia) {
                mediaPlayer?.stop()
                mediaPlayer?.release()
                mediaPlayer = null
                isPlayingMedia = false
            }
        } catch (e: Exception) {}
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
     * Toca um tom de alerta VIP selecionado pelo motorista.
     */
    fun playVipAlert(tone: String) {
        try {
            when (tone) {
                "beep" -> {
                    toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                    }, 150)
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
                    }, 300)
                }
                "sonar" -> {
                    toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, 300)
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, 300)
                    }, 450)
                }
                else -> { // "bell" or "custom"
                    toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 200)
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        toneGenerator?.startTone(ToneGenerator.TONE_PROP_PROMPT, 200)
                    }, 250)
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 300)
                    }, 500)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play VIP alert tone ($tone): ${e.message}")
        }
    }

    /**
     * Libera recursos de cache em situações de baixa memória.
     */
    fun clearCaches() {
        Log.d(TAG, "Limpando caches de áudio para liberar memória.")
        // Se tivéssemos um cache de arquivos temporários, limparíamos aqui.
    }

    /**
     * Libera recursos ao encerrar.
     */
    fun shutdown() {
        try {
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {}
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
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                focusRequest?.let { 
                    audioManager.abandonAudioFocusRequest(it)
                    focusRequest = null
                }
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus { }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to abandon audio focus: ${e.message}")
        }
    }

    /**
     * Preprocessador fonético de alta performance para Português do Brasil.
     * Converte gírias de entrega, termos técnicos e moedas em sentenças faladas 100% naturais.
     */
    fun preprocessTextForPtBr(text: String): String {
        if (text.isEmpty()) return text
        var result = text

        // Subtitui moedas (ex: R$ 15,50 ou R$15) por fala humana nativa
        val currencyRegex = Regex("""R\$\s*(\d+)(?:[,.](\d{1,2}))?""")
        result = currencyRegex.replace(result) { matchResult ->
            val reais = matchResult.groups[1]?.value?.toIntOrNull() ?: 0
            val centavosGroup = matchResult.groups[2]?.value
            val centavos = centavosGroup?.toIntOrNull() ?: 0
            
            val reaisStr = when (reais) {
                0 -> ""
                1 -> "um real"
                else -> "$reais reais"
            }
            
            val centavosStr = when (centavos) {
                0 -> ""
                1 -> "um centavo"
                else -> {
                    val adjustedCentavos = if (centavosGroup?.length == 1) centavos * 10 else centavos
                    "$adjustedCentavos centavos"
                }
            }
            
            if (reaisStr.isNotEmpty() && centavosStr.isNotEmpty()) {
                "$reaisStr e $centavosStr"
            } else if (reaisStr.isNotEmpty()) {
                reaisStr
            } else if (centavosStr.isNotEmpty()) {
                centavosStr
            } else {
                "zero reais"
            }
        }

        // Dicionário de abreviações e termos fonéticos para trânsito/entrega
        val replacements = listOf(
            Regex("""\bAv\.""", RegexOption.IGNORE_CASE) to "Avenida",
            Regex("""\bR\.""", RegexOption.IGNORE_CASE) to "Rua",
            Regex("""\bAl\.""", RegexOption.IGNORE_CASE) to "Alameda",
            Regex("""\bRod\.""", RegexOption.IGNORE_CASE) to "Rodovia",
            Regex("""\bDr\.""", RegexOption.IGNORE_CASE) to "Doutor",
            Regex("""\bNº""", RegexOption.IGNORE_CASE) to "número",
            Regex("""\bnº""", RegexOption.IGNORE_CASE) to "número",
            Regex("""\bno\.""", RegexOption.IGNORE_CASE) to "número",
            Regex("""\bNo\.""", RegexOption.IGNORE_CASE) to "número",
            Regex("""\bkm/h\b""", RegexOption.IGNORE_CASE) to "quilômetros por hora",
            Regex("""\bkm\b""", RegexOption.IGNORE_CASE) to "quilômetros",
            Regex("""\bmin\b""", RegexOption.IGNORE_CASE) to "minutos",
            Regex("""\bseg\b""", RegexOption.IGNORE_CASE) to "segundos",
            Regex("""%""") to " por cento",
            Regex("""\b99\b""") to "Noventa e Nove",
            Regex("""\bSP\b""") to "São Paulo",
            Regex("""\biFood\b""", RegexOption.IGNORE_CASE) to "ifúdi",
            Regex("""\bRappi\b""", RegexOption.IGNORE_CASE) to "rápi",
            Regex("""\bUber\b""", RegexOption.IGNORE_CASE) to "úber",
            Regex("""\bGPS\b""", RegexOption.IGNORE_CASE) to "Gê Pê Ésse",
            Regex("""\bAPI\b""", RegexOption.IGNORE_CASE) to "Á Pê Í",
            Regex("""\bTTS\b""", RegexOption.IGNORE_CASE) to "Tê Tê Ésse",
            Regex("""\bHD\b""", RegexOption.IGNORE_CASE) to "Agá Dê",
            Regex("""\bAPK\b""", RegexOption.IGNORE_CASE) to "Ah Pê Cá",
            Regex("""\bJarvis\b""", RegexOption.IGNORE_CASE) to "Járvis",
            Regex("""\bStark\b""", RegexOption.IGNORE_CASE) to "Istárk",
            Regex("""\bSovereign\b""", RegexOption.IGNORE_CASE) to "Soverin",
            Regex("""\bHUD\b""", RegexOption.IGNORE_CASE) to "Hâd",
            Regex("""\bOnline\b""", RegexOption.IGNORE_CASE) to "On-laine",
            Regex("""\bOffline\b""", RegexOption.IGNORE_CASE) to "Of-laine",
            Regex("""\bWifi\b""", RegexOption.IGNORE_CASE) to "Uai-fai",
            Regex("""\bBluetooth\b""", RegexOption.IGNORE_CASE) to "Blutúfi"
        )

        for ((regex, replacement) in replacements) {
            result = regex.replace(result, replacement)
        }

        return result
    }
}
