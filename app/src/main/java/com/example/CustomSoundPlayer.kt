package com.example

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log

/**
 * Tipos de sons personalizados disponíveis para notificações do Radar Coordinator.
 */
enum class NotificationSoundType(val id: String, val title: String, val description: String, val rawResId: Int) {
    RADAR_CHIME(
        id = "radar_chime",
        title = "Sonar Radar Melódico (Padrão)",
        description = "Chime harmônico quádruplo nítido (E5-G#5-B5-E6) para cortar ruído de trânsito",
        rawResId = R.raw.radar_offer_alert
    ),
    URGENT_SONAR(
        id = "urgent_sonar",
        title = "Alerta Tático Sonar Duplo",
        description = "Pulso duplo de alta frequência (C6-G6) para corridas de alta rentabilidade (> R$ 4,00/km)",
        rawResId = R.raw.radar_urgent_alert
    ),
    SYSTEM_DEFAULT(
        id = "system_default",
        title = "Som Padrão do Sistema",
        description = "Utiliza o toque de notificação padrão configurado no Android",
        rawResId = 0
    ),
    SILENT(
        id = "silent",
        title = "Apenas Vibração (Silencioso)",
        description = "Notificação visual e feedback háptico sem emissão sonora",
        rawResId = 0
    );

    companion object {
        fun fromId(id: String?): NotificationSoundType {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: RADAR_CHIME
        }
    }
}

/**
 * Player de som dedicado para alertas de notificação e pré-escuta de áudio.
 */
object CustomSoundPlayer {
    private const val TAG = "CustomSoundPlayer"

    /**
     * Toca o som personalizado correspondente à oferta.
     */
    fun playOfferSound(context: Context, isUrgent: Boolean = false, forcedType: NotificationSoundType? = null) {
        val prefs = context.getSharedPreferences(LocalNotificationManager.PREFS_NAME, Context.MODE_PRIVATE)
        val savedType = forcedType ?: NotificationSoundType.fromId(
            prefs.getString(LocalNotificationManager.KEY_SOUND_SELECTION, NotificationSoundType.RADAR_CHIME.id)
        )

        if (savedType == NotificationSoundType.SILENT) {
            return
        }

        val soundRes = if (savedType == NotificationSoundType.SYSTEM_DEFAULT) {
            // Usa som do chime caso sistema padrão não tenha recurso raw
            if (isUrgent) R.raw.radar_urgent_alert else R.raw.radar_offer_alert
        } else if (isUrgent && savedType == NotificationSoundType.RADAR_CHIME) {
            // Se a oferta for ultra-lucrativa, prioriza o som urgente
            R.raw.radar_urgent_alert
        } else {
            savedType.rawResId
        }

        if (soundRes == 0) return

        try {
            val mediaPlayer = MediaPlayer()
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_REQUEST)
                .build()

            mediaPlayer.setAudioAttributes(audioAttributes)
            val uri = Uri.parse("android.resource://${context.packageName}/$soundRes")
            mediaPlayer.setDataSource(context, uri)
            mediaPlayer.prepare()
            mediaPlayer.setVolume(1.0f, 1.0f)
            mediaPlayer.setOnCompletionListener { mp ->
                try {
                    mp.release()
                } catch (e: Exception) {
                    Log.d(TAG, "Erro ao liberar MediaPlayer: ${e.message}")
                }
            }
            mediaPlayer.start()
            Log.d(TAG, "Som personalizado reproduzido com sucesso: $savedType (Res: $soundRes)")
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao reproduzir som personalizado: ${e.message}")
        }
    }

    /**
     * Pré-escuta para a tela de configurações de notificação.
     */
    fun previewSound(context: Context, soundType: NotificationSoundType) {
        if (soundType == NotificationSoundType.SILENT) {
            HapticFeedbackHelper.vibrateHighPriorityOffer(context)
            return
        }

        val resId = if (soundType.rawResId != 0) soundType.rawResId else R.raw.radar_offer_alert
        try {
            val mediaPlayer = MediaPlayer()
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .build()

            mediaPlayer.setAudioAttributes(audioAttributes)
            val uri = Uri.parse("android.resource://${context.packageName}/$resId")
            mediaPlayer.setDataSource(context, uri)
            mediaPlayer.prepare()
            mediaPlayer.setVolume(1.0f, 1.0f)
            mediaPlayer.setOnCompletionListener { mp ->
                try {
                    mp.release()
                } catch (_: Exception) {}
            }
            mediaPlayer.start()
            HapticFeedbackHelper.vibrateHighPriorityOffer(context)
        } catch (e: Exception) {
            Log.e(TAG, "Erro na pré-escuta: ${e.message}")
        }
    }
}
