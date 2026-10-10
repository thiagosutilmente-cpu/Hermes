package com.example

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Modos de sobreposição de áudio e ducking nas APIs de áudio do Android.
 * Garante que o alerta sonoro de novas ofertas se sobreponha a músicas (Spotify, etc.)
 * ou instruções de navegação GPS (Waze, Google Maps).
 */
enum class AudioOverlayPriorityMode(
    val id: String,
    val title: String,
    val subtitle: String,
    val description: String
) {
    DUCKING(
        id = "ducking",
        title = "Ducking Inteligente (80%)",
        subtitle = "Atenuação de Fundo",
        description = "Atenua o volume de músicas (Spotify, Deezer) e navegação GPS durante o toque da oferta, restaurando o volume normal em seguida."
    ),
    EXCLUSIVE_ALARM(
        id = "exclusive_alarm",
        title = "Alarme Prioritário (Corte Total)",
        subtitle = "Canal STREAM_ALARM",
        description = "Pausa temporariamente músicas e anúncios de rota, emitindo o som no canal de Alarme do Android com penetração acústica máxima."
    ),
    STANDARD_MIX(
        id = "standard_mix",
        title = "Mixagem Normal",
        subtitle = "Sem Atenuação",
        description = "Reproduz o áudio junto com os outros aplicativos sem intervir no volume do sistema."
    );

    companion object {
        fun fromId(id: String?): AudioOverlayPriorityMode {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: DUCKING
        }
    }
}

/**
 * Tipos de sons personalizados e distintos disponíveis para o Radar Coordinator.
 * Cada tipo possui uma assinatura acústica distinta adaptada à rentabilidade e rota.
 */
enum class NotificationSoundType(
    val id: String,
    val title: String,
    val shortName: String,
    val description: String,
    val rawResId: Int,
    val badge: String
) {
    AUTO_DISTINCT(
        id = "auto_distinct",
        title = "Inteligência Sonora Automática",
        shortName = "Auto Inteligente",
        description = "Detecta em tempo real se a corrida é VIP, Turbo, Urgente ou Padrão e dispara o alerta correspondente.",
        rawResId = 0,
        badge = "🧠 RECOMENDADO"
    ),
    RADAR_CHIME(
        id = "radar_chime",
        title = "Sonar Radar Melódico",
        shortName = "Melódico Padrão",
        description = "Chime harmônico quádruplo nítido (E5-G#5-B5-E6) para corridas regulares aprovadas.",
        rawResId = R.raw.radar_offer_alert,
        badge = "🔊 PADRÃO"
    ),
    URGENT_SONAR(
        id = "urgent_sonar",
        title = "Alerta Tático Sonar Duplo",
        shortName = "Sonar Alta Margem",
        description = "Pulso duplo de alta frequência (C6-G6) para corridas de rentabilidade extrema (> R$ 3,80/km).",
        rawResId = R.raw.radar_urgent_alert,
        badge = "⚡ R$/KM ALTO"
    ),
    VIP_FANFARE(
        id = "vip_fanfare",
        title = "Fanfarra Ouro VIP (Ticket Alto)",
        shortName = "Fanfarra VIP",
        description = "Arpeggio cristalino brilhante (D5-F#6) para corridas com valor bruto elevado (> R$ 32,00).",
        rawResId = R.raw.radar_vip_alert,
        badge = "👑 TICKET ALTO"
    ),
    TURBO_SPRINT(
        id = "turbo_sprint",
        title = "Alerta Turbo Sprint (Curta Distância)",
        shortName = "Turbo Sprint",
        description = "Chirp duplo ascendente veloz para corridas expressas de giro rápido (< 3,5 km).",
        rawResId = R.raw.radar_turbo_alert,
        badge = "🚀 ROTA RÁPIDA"
    ),
    TACTICAL_HORN(
        id = "tactical_horn",
        title = "Sirene Tática Anti-Ruído",
        shortName = "Sirene Anti-Vento",
        description = "Pulsos de 2.2 kHz ultra-penetrantes para cortar vento, moto e trânsito pesado.",
        rawResId = R.raw.radar_tactical_horn,
        badge = "🛡️ ANTI-RUÍDO"
    ),
    SYSTEM_DEFAULT(
        id = "system_default",
        title = "Som Padrão do Sistema",
        shortName = "Toque do Android",
        description = "Utiliza o toque de notificação padrão configurado no Android.",
        rawResId = 0,
        badge = "📱 SISTEMA"
    ),
    SILENT(
        id = "silent",
        title = "Apenas Vibração (Silencioso)",
        shortName = "Silencioso",
        description = "Notificação visual e feedback háptico sem emissão sonora.",
        rawResId = 0,
        badge = "🔕 MUDO"
    );

    companion object {
        // Alias de compatibilidade com versões anteriores
        val CHIME_MELODIC: NotificationSoundType get() = RADAR_CHIME
        val TACTICAL_URGENT: NotificationSoundType get() = URGENT_SONAR

        fun fromId(id: String?): NotificationSoundType {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: AUTO_DISTINCT
        }
    }
}

/**
 * Player de som dedicado para alertas de notificação prioritários com suporte a
 * AudioFocus e sobreposição tática sobre músicas (Spotify/YouTube) e navegação GPS (Waze/Maps).
 */
object CustomSoundPlayer {
    private const val TAG = "CustomSoundPlayer"

    const val KEY_AUDIO_OVERLAY_MODE = "key_audio_overlay_priority_mode"
    const val KEY_DISTINCT_OFFER_SOUNDS_ENABLED = "key_distinct_offer_sounds_enabled"
    const val KEY_AUDIO_VOLUME_BOOST = "key_audio_volume_boost"

    private var activeFocusRequest: AudioFocusRequest? = null
    private var activeMediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    private var focusWatchdogJob: Job? = null

    fun getAudioOverlayMode(context: Context): AudioOverlayPriorityMode {
        val prefs = context.getSharedPreferences(LocalNotificationManager.PREFS_NAME, Context.MODE_PRIVATE)
        val modeId = prefs.getString(KEY_AUDIO_OVERLAY_MODE, AudioOverlayPriorityMode.DUCKING.id)
        return AudioOverlayPriorityMode.fromId(modeId)
    }

    fun setAudioOverlayMode(context: Context, mode: AudioOverlayPriorityMode) {
        val prefs = context.getSharedPreferences(LocalNotificationManager.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_AUDIO_OVERLAY_MODE, mode.id).apply()
    }

    fun isDistinctSoundsEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(LocalNotificationManager.PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_DISTINCT_OFFER_SOUNDS_ENABLED, true)
    }

    fun setDistinctSoundsEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(LocalNotificationManager.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_DISTINCT_OFFER_SOUNDS_ENABLED, enabled).apply()
    }

    fun isVolumeBoostEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(LocalNotificationManager.PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_AUDIO_VOLUME_BOOST, true)
    }

    fun setVolumeBoostEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(LocalNotificationManager.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_AUDIO_VOLUME_BOOST, enabled).apply()
    }

    /**
     * Resolve o tipo de som exato para uma oferta específica.
     * Considera os parâmetros da corrida (valor, R$/km, distância e velocidade) quando o modo
     * automático estiver ativo.
     */
    fun resolveSoundForOffer(
        context: Context,
        offer: RadarOffer?,
        isUrgent: Boolean = false,
        forcedType: NotificationSoundType? = null
    ): Pair<NotificationSoundType, Int> {
        val prefs = context.getSharedPreferences(LocalNotificationManager.PREFS_NAME, Context.MODE_PRIVATE)
        val savedType = forcedType ?: NotificationSoundType.fromId(
            prefs.getString(LocalNotificationManager.KEY_SOUND_SELECTION, NotificationSoundType.AUTO_DISTINCT.id)
        )

        val resolvedType: NotificationSoundType = if (savedType == NotificationSoundType.AUTO_DISTINCT) {
            when {
                offer == null && isUrgent -> NotificationSoundType.URGENT_SONAR
                offer == null -> NotificationSoundType.RADAR_CHIME
                offer.value >= 32.0 -> NotificationSoundType.VIP_FANFARE
                offer.gainPerKm >= 3.8 || isUrgent -> NotificationSoundType.URGENT_SONAR
                offer.distanceKm <= 3.2 -> NotificationSoundType.TURBO_SPRINT
                LocationService.globalLocationState.value.currentSpeedKmh > 7.0 -> NotificationSoundType.TACTICAL_HORN
                else -> NotificationSoundType.RADAR_CHIME
            }
        } else if (isUrgent && savedType == NotificationSoundType.RADAR_CHIME) {
            NotificationSoundType.URGENT_SONAR
        } else {
            savedType
        }

        val resId = when (resolvedType) {
            NotificationSoundType.SILENT -> 0
            NotificationSoundType.SYSTEM_DEFAULT -> if (isUrgent) R.raw.radar_urgent_alert else R.raw.radar_offer_alert
            else -> if (resolvedType.rawResId != 0) resolvedType.rawResId else R.raw.radar_offer_alert
        }

        return Pair(resolvedType, resId)
    }

    /**
     * Solicita AudioFocus ao sistema Android para sobrepor o som a músicas ou navegação GPS.
     */
    private fun requestAudioFocus(context: Context, mode: AudioOverlayPriorityMode): Boolean {
        if (mode == AudioOverlayPriorityMode.STANDARD_MIX) return true
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false

        val focusGain = when (mode) {
            AudioOverlayPriorityMode.DUCKING -> AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            AudioOverlayPriorityMode.EXCLUSIVE_ALARM -> AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
            AudioOverlayPriorityMode.STANDARD_MIX -> AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
        }

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(
                if (mode == AudioOverlayPriorityMode.EXCLUSIVE_ALARM)
                    AudioAttributes.USAGE_ALARM
                else
                    AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_REQUEST
            )
            .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
            .build()

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val request = AudioFocusRequest.Builder(focusGain)
                    .setAudioAttributes(audioAttributes)
                    .setAcceptsDelayedFocusGain(false)
                    .setWillPauseWhenDucked(false)
                    .setOnAudioFocusChangeListener { focusChange ->
                        Log.d(TAG, "Audio focus change event: $focusChange")
                    }
                    .build()
                activeFocusRequest = request
                val res = audioManager.requestAudioFocus(request)
                Log.d(TAG, "AudioFocus O+ solicitado ($mode): $res")
                res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            } else {
                @Suppress("DEPRECATION")
                val streamType = if (mode == AudioOverlayPriorityMode.EXCLUSIVE_ALARM)
                    AudioManager.STREAM_ALARM
                else
                    AudioManager.STREAM_NOTIFICATION

                @Suppress("DEPRECATION")
                val res = audioManager.requestAudioFocus(null, streamType, focusGain)
                Log.d(TAG, "AudioFocus legado solicitado ($mode): $res")
                res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            }
        } catch (e: Exception) {
            Log.w(TAG, "Erro ao solicitar foco de áudio: ${e.message}")
            false
        }
    }

    /**
     * Libera o foco de áudio, restaurando o volume padrão de músicas e GPS.
     */
    fun releaseAudioFocus(context: Context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                activeFocusRequest?.let {
                    audioManager.abandonAudioFocusRequest(it)
                    Log.d(TAG, "AudioFocus liberado com sucesso (Android O+).")
                }
                activeFocusRequest = null
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(null)
                Log.d(TAG, "AudioFocus legado liberado.")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Falha ao liberar AudioFocus: ${e.message}")
        }
    }

    /**
     * Toca o som personalizado correspondente à oferta garantindo sobreposição sonora
     * via AudioFocus e atributos de alta prioridade.
     */
    fun playOfferSound(
        context: Context,
        offer: RadarOffer? = null,
        isUrgent: Boolean = false,
        forcedType: NotificationSoundType? = null
    ) {
        val (resolvedType, soundRes) = resolveSoundForOffer(context, offer, isUrgent, forcedType)
        if (resolvedType == NotificationSoundType.SILENT || soundRes == 0) {
            HapticFeedbackHelper.vibrateHighPriorityOffer(context)
            return
        }

        val overlayMode = getAudioOverlayMode(context)

        // 1. Solicita foco de áudio para ducking ou corte de música e GPS
        requestAudioFocus(context, overlayMode)

        // 2. Cancela qualquer watchdog anterior e libera player prévio
        focusWatchdogJob?.cancel()
        try {
            activeMediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
        } catch (_: Exception) {}

        try {
            val mediaPlayer = MediaPlayer()
            activeMediaPlayer = mediaPlayer

            val audioUsage = if (overlayMode == AudioOverlayPriorityMode.EXCLUSIVE_ALARM) {
                AudioAttributes.USAGE_ALARM
            } else {
                AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_REQUEST
            }

            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(audioUsage)
                .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                .build()

            mediaPlayer.setAudioAttributes(audioAttributes)
            val uri = Uri.parse("android.resource://${context.packageName}/$soundRes")
            mediaPlayer.setDataSource(context, uri)
            mediaPlayer.prepare()

            val boost = isVolumeBoostEnabled(context)
            val volume = if (boost) 1.0f else 0.85f
            mediaPlayer.setVolume(volume, volume)

            fun cleanup() {
                try {
                    mediaPlayer.release()
                } catch (_: Exception) {}
                if (activeMediaPlayer == mediaPlayer) {
                    activeMediaPlayer = null
                }
                releaseAudioFocus(context)
            }

            mediaPlayer.setOnCompletionListener {
                Log.d(TAG, "Som concluído. Restaurando áudio de fundo...")
                cleanup()
            }

            mediaPlayer.setOnErrorListener { _, what, extra ->
                Log.e(TAG, "Erro no MediaPlayer ($what, $extra). Liberando foco...")
                cleanup()
                true
            }

            mediaPlayer.start()
            Log.d(TAG, "Som de alerta reproduzido com sucesso: ${resolvedType.title} (Res: $soundRes, Modo: ${overlayMode.title})")

            // Watchdog de segurança: garante que o foco seja liberado em no máximo 3.5s
            focusWatchdogJob = scope.launch {
                delay(3500L)
                if (activeMediaPlayer == mediaPlayer) {
                    cleanup()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao reproduzir som de oferta: ${e.message}")
            releaseAudioFocus(context)
        }
    }

    /**
     * Pré-escuta para a tela de configurações com simulação de ducking e vibração tática.
     */
    fun previewSound(
        context: Context,
        soundType: NotificationSoundType,
        simulateDucking: Boolean = true
    ) {
        if (soundType == NotificationSoundType.SILENT) {
            HapticFeedbackHelper.vibrateHighPriorityOffer(context)
            return
        }

        val overlayMode = if (simulateDucking) getAudioOverlayMode(context) else AudioOverlayPriorityMode.STANDARD_MIX
        val resId = if (soundType == NotificationSoundType.AUTO_DISTINCT) {
            R.raw.radar_urgent_alert
        } else if (soundType.rawResId != 0) {
            soundType.rawResId
        } else {
            R.raw.radar_offer_alert
        }

        // Solicita foco para pré-escuta para demonstrar o ducking
        requestAudioFocus(context, overlayMode)

        try {
            focusWatchdogJob?.cancel()
            try {
                activeMediaPlayer?.release()
            } catch (_: Exception) {}

            val mediaPlayer = MediaPlayer()
            activeMediaPlayer = mediaPlayer

            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(
                    if (overlayMode == AudioOverlayPriorityMode.EXCLUSIVE_ALARM)
                        AudioAttributes.USAGE_ALARM
                    else
                        AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_REQUEST
                )
                .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                .build()

            mediaPlayer.setAudioAttributes(audioAttributes)
            val uri = Uri.parse("android.resource://${context.packageName}/$resId")
            mediaPlayer.setDataSource(context, uri)
            mediaPlayer.prepare()
            mediaPlayer.setVolume(1.0f, 1.0f)

            fun cleanup() {
                try {
                    mediaPlayer.release()
                } catch (_: Exception) {}
                if (activeMediaPlayer == mediaPlayer) {
                    activeMediaPlayer = null
                }
                releaseAudioFocus(context)
            }

            mediaPlayer.setOnCompletionListener { cleanup() }
            mediaPlayer.setOnErrorListener { _, _, _ ->
                cleanup()
                true
            }

            mediaPlayer.start()
            HapticFeedbackHelper.vibrateHighPriorityOffer(context)

            focusWatchdogJob = scope.launch {
                delay(3500L)
                if (activeMediaPlayer == mediaPlayer) {
                    cleanup()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro na pré-escuta: ${e.message}")
            releaseAudioFocus(context)
        }
    }
}
