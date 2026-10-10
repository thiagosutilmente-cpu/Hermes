package com.example

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Gerenciador de Notificações Push de Alta Prioridade do Radar Coordinator.
 * Emite notificações Heads-Up flutuantes com alertas sonoros distintos e
 * sobreposição de áudio (AudioFocus ducking) sobre músicas e navegação GPS.
 */
class LocalNotificationManager(private val context: Context) {

    companion object {
        const val PREFS_NAME = "radar_notification_prefs"
        const val KEY_SOUND_SELECTION = "key_notification_sound_selection"

        // Canais v3 com sons personalizados distintos incorporados
        const val CHANNEL_ID_HIGH_PRIORITY = "radar_offers_high_priority_v3"
        const val CHANNEL_ID_URGENT = "radar_offers_urgent_v3"
        const val CHANNEL_ID_VIP = "radar_offers_vip_v3"
        const val CHANNEL_ID_TURBO = "radar_offers_turbo_v3"
        const val CHANNEL_ID_TACTICAL = "radar_offers_tactical_v3"

        // Compatibilidade legada
        const val CHANNEL_ID = CHANNEL_ID_HIGH_PRIORITY
        const val CHANNEL_NAME = "Ofertas de Alta Prioridade"
        const val CHANNEL_DESC = "Alertas sonoros imediatos de pedidos com alta rentabilidade (R$/km)"

        const val EXTRA_ACTION = "com.example.EXTRA_NOTIFICATION_ACTION"
        const val ACTION_ACCEPT = "ACTION_ACCEPT_OFFER"
        const val ACTION_DECLINE = "ACTION_DECLINE_OFFER"
        const val ACTION_OPEN_RADAR = "ACTION_OPEN_RADAR"

        const val EXTRA_OFFER_ID = "com.example.EXTRA_OFFER_ID"
        const val EXTRA_OFFER_APP = "com.example.EXTRA_OFFER_APP"
        const val EXTRA_OFFER_VALUE = "com.example.EXTRA_OFFER_VALUE"
        const val EXTRA_OFFER_DISTANCE = "com.example.EXTRA_OFFER_DISTANCE"
        const val EXTRA_OFFER_GAIN_KM = "com.example.EXTRA_OFFER_GAIN_KM"

        const val NOTIFICATION_ID_BASE = 7000

        /**
         * Flag global de silenciamento por velocidade (> 10 km/h).
         */
        @Volatile
        var isSpeedMuteActive: Boolean = false

        /**
         * Cancela todas as notificações de ofertas pendentes quando o piloto acelera acima de 10 km/h.
         */
        fun cancelAllActiveOfferNotifications(context: Context) {
            try {
                val manager = NotificationManagerCompat.from(context)
                manager.cancelAll()
                Log.d("LocalNotificationManager", "Notificações ativas canceladas por segurança (> 10 km/h).")
            } catch (e: Exception) {
                Log.e("LocalNotificationManager", "Erro ao cancelar notificações: ${e.message}")
            }
        }

        fun getSelectedSoundType(context: Context): NotificationSoundType {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val id = prefs.getString(KEY_SOUND_SELECTION, NotificationSoundType.AUTO_DISTINCT.id)
            return NotificationSoundType.fromId(id)
        }

        fun setSelectedSoundType(context: Context, type: NotificationSoundType) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_SOUND_SELECTION, type.id).apply()
        }
    }

    private val notificationManager = NotificationManagerCompat.from(context)
    private val scope = CoroutineScope(Dispatchers.Default)

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundOfferUri = Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/${R.raw.radar_offer_alert}")
            val soundUrgentUri = Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/${R.raw.radar_urgent_alert}")
            val soundVipUri = Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/${R.raw.radar_vip_alert}")
            val soundTurboUri = Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/${R.raw.radar_turbo_alert}")
            val soundTacticalUri = Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/${R.raw.radar_tactical_horn}")

            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                .build()

            // Canal 1: Alta Prioridade Padrão (Chime Harmônico E5-E6)
            val highPriorityChannel = NotificationChannel(
                CHANNEL_ID_HIGH_PRIORITY,
                "Ofertas Aprovadas (Sonar Melódico)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alertas sonoros imediatos para pedidos com rentabilidade validada"
                enableLights(true)
                lightColor = Color.parseColor("#00FF88") // Verde Neon Jarvis
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 150, 100, 150, 100, 250)
                setSound(soundOfferUri, audioAttributes)
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            // Canal 2: Ofertas Ultra Lucrativas (Sonar Duplo C6-G6)
            val urgentChannel = NotificationChannel(
                CHANNEL_ID_URGENT,
                "Ofertas Ultra Lucrativas (Sonar Duplo)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alertas de alta penetração para corridas com rentabilidade extrema (> R$ 3,80/km)"
                enableLights(true)
                lightColor = Color.parseColor("#FF9900") // Âmbar Dourado
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 80, 250, 80, 350)
                setSound(soundUrgentUri, audioAttributes)
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            // Canal 3: Ofertas VIP & Ticket Alto (Fanfarra Ouro D5-F#6)
            val vipChannel = NotificationChannel(
                CHANNEL_ID_VIP,
                "Ofertas VIP (Ticket Alto > R$ 32)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Arpeggio brilhante para pedidos com valor bruto alto (> R$ 32,00)"
                enableLights(true)
                lightColor = Color.parseColor("#FFD700") // Dourado VIP
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 150, 80, 150, 80, 150, 80, 300)
                setSound(soundVipUri, audioAttributes)
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            // Canal 4: Corridas Curtas Sprint Turbo (Chirp Veloz)
            val turboChannel = NotificationChannel(
                CHANNEL_ID_TURBO,
                "Ofertas Sprint Turbo (Curta Rota < 3.5km)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Chirp duplo de alta cadência para rotas curtas de giro rápido"
                enableLights(true)
                lightColor = Color.parseColor("#00E5FF") // Ciano Neon
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 100, 50, 100, 50, 200)
                setSound(soundTurboUri, audioAttributes)
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            // Canal 5: Sirene Tática Anti-Ruído (Penetração 2.2 kHz)
            val tacticalChannel = NotificationChannel(
                CHANNEL_ID_TACTICAL,
                "Alertas Táticos Anti-Ruído (Vento & Trânsito)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Pulsos penetrantes para cortar ruído de escapamento, vento e capacete"
                enableLights(true)
                lightColor = Color.parseColor("#FF0055") // Magenta Tático
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 100, 300, 100, 400)
                setSound(soundTacticalUri, audioAttributes)
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            val systemManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            systemManager?.createNotificationChannel(highPriorityChannel)
            systemManager?.createNotificationChannel(urgentChannel)
            systemManager?.createNotificationChannel(vipChannel)
            systemManager?.createNotificationChannel(turboChannel)
            systemManager?.createNotificationChannel(tacticalChannel)
        }
    }

    /**
     * Verifica se o app possui permissão para emitir notificações (Android 13+).
     */
    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Emite uma notificação Heads-Up flutuante de alta prioridade com alertas sonoros distintos
     * e sobreposição acústica (AudioFocus ducking) sobre músicas e navegação GPS.
     */
    @SuppressLint("MissingPermission")
    fun showHighPriorityOfferNotification(
        offer: RadarOffer,
        filterCriteria: OfferFilterCriteria? = null
    ): Boolean {
        if (!hasNotificationPermission()) return false

        // 0. TRAVA DE SEGURANÇA POR VELOCIDADE: Silencia e suprime notificações se > 10 km/h
        val currentSpeed = LocationService.globalLocationState.value.currentSpeedKmh
        val isSafetyLock = LocationService.globalLocationState.value.isSafetyLockActive
        if (isSpeedMuteActive || isSafetyLock || currentSpeed > 10.0) {
            Log.d(
                "LocalNotificationManager",
                "Notificação de oferta suprimida por segurança em alta velocidade ($currentSpeed km/h > 10 km/h)."
            )
            return false
        }

        // 1. FILTRAGEM AUTOMÁTICA DE NOTIFICAÇÃO POR LIMITE DE PREÇO/KM
        if (filterCriteria != null) {
            val passes = NotificationFilterManager.evaluateAndRecord(offer, filterCriteria)
            if (!passes) {
                Log.d(
                    "LocalNotificationManager",
                    "Notificação bloqueada pelo filtro: R$ ${offer.gainPerKm}/km < piso R$ ${filterCriteria.notificationMinGainPerKm}/km."
                )
                return false
            }
        }

        val isUrgent = offer.gainPerKm >= 3.8 || offer.value >= 32.0
        val (resolvedSound, soundResId) = CustomSoundPlayer.resolveSoundForOffer(context, offer, isUrgent)

        val targetChannelId = when (resolvedSound) {
            NotificationSoundType.VIP_FANFARE -> CHANNEL_ID_VIP
            NotificationSoundType.URGENT_SONAR -> CHANNEL_ID_URGENT
            NotificationSoundType.TURBO_SPRINT -> CHANNEL_ID_TURBO
            NotificationSoundType.TACTICAL_HORN -> CHANNEL_ID_TACTICAL
            else -> CHANNEL_ID_HIGH_PRIORITY
        }

        val soundUri = if (soundResId != 0 && resolvedSound != NotificationSoundType.SILENT) {
            Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/$soundResId")
        } else null

        val formattedValue = String.format(Locale.GERMANY, "R$ %.2f", offer.value)
        val formattedPerKm = String.format(Locale.GERMANY, "R$ %.2f/km", offer.gainPerKm)
        val notificationId = NOTIFICATION_ID_BASE + (offer.id.hashCode() % 1000)

        // 1. Intent para abrir o Radar Coordinator ao tocar no corpo da notificação
        val openIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_OPEN_RADAR
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_ACTION, ACTION_OPEN_RADAR)
            putExtra(EXTRA_OFFER_ID, offer.id)
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 2. Ação direta: ACEITAR PEDIDO
        val acceptIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_ACCEPT
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_ACTION, ACTION_ACCEPT)
            putExtra(EXTRA_OFFER_ID, offer.id)
            putExtra(EXTRA_OFFER_VALUE, offer.value)
            putExtra(EXTRA_OFFER_APP, offer.appName)
        }
        val acceptPendingIntent = PendingIntent.getActivity(
            context,
            notificationId + 1,
            acceptIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 3. Ação direta: DESCARTAR
        val declineIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_DECLINE
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_ACTION, ACTION_DECLINE)
            putExtra(EXTRA_OFFER_ID, offer.id)
        }
        val declinePendingIntent = PendingIntent.getActivity(
            context,
            notificationId + 2,
            declineIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val appIconBadge = when (offer.appName) {
            "iFood" -> "🔴 iFood"
            "Rappi" -> "🟠 Rappi"
            "Uber Direct", "Uber Flash" -> "⚫ Uber"
            "99Food", "99 Entrega" -> "🟡 99"
            else -> "📦 ${offer.appName}"
        }

        val bigText = StringBuilder().apply {
            append("🍔 ${offer.restaurant}\n")
            append("🛵 ${offer.distanceKm} km • Tempo estimado: ${offer.estimatedTimeMin} min\n")
            append("⚡ Rentabilidade: $formattedPerKm • Lucro líq.: R$ ${String.format(Locale.GERMANY, "%.2f", offer.netProfit)}\n")
            append("🔔 Som do Alerta: ${resolvedSound.title} (${resolvedSound.badge})\n")
            append("🧠 IA Jarvis: ${offer.neuralDecision.reason}")
        }.toString()

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
            .build()

        val vibrationPattern = when (resolvedSound) {
            NotificationSoundType.VIP_FANFARE -> longArrayOf(0, 150, 80, 150, 80, 150, 80, 300)
            NotificationSoundType.TURBO_SPRINT -> longArrayOf(0, 100, 50, 100, 50, 200)
            NotificationSoundType.TACTICAL_HORN -> longArrayOf(0, 300, 100, 300, 100, 400)
            NotificationSoundType.URGENT_SONAR -> longArrayOf(0, 250, 80, 250, 80, 350)
            else -> longArrayOf(0, 150, 100, 150, 100, 250)
        }

        val bannerColor = when (resolvedSound) {
            NotificationSoundType.VIP_FANFARE -> Color.parseColor("#FFD700")
            NotificationSoundType.URGENT_SONAR -> Color.parseColor("#FF9900")
            NotificationSoundType.TURBO_SPRINT -> Color.parseColor("#00E5FF")
            NotificationSoundType.TACTICAL_HORN -> Color.parseColor("#FF0055")
            else -> Color.parseColor("#00FF88")
        }

        val builder = NotificationCompat.Builder(context, targetChannelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("🎯 ${resolvedSound.badge}: $formattedValue ($appIconBadge)")
            .setContentText("🍔 ${offer.restaurant} • 🛵 ${offer.distanceKm} km • $formattedPerKm")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(bigText)
                    .setSummaryText("${resolvedSound.shortName} • ${CustomSoundPlayer.getAudioOverlayMode(context).subtitle}")
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setColor(bannerColor)
            .setAutoCancel(true)
            .setOngoing(false)
            .setVibrate(vibrationPattern)
            .setContentIntent(openPendingIntent)
            .addAction(
                android.R.drawable.ic_input_add,
                "✅ ACEITAR ($formattedValue)",
                acceptPendingIntent
            )
            .addAction(
                android.R.drawable.ic_delete,
                "❌ RECUSAR",
                declinePendingIntent
            )

        if (soundUri != null) {
            builder.setSound(soundUri, audioAttributes)
        }

        notificationManager.notify(notificationId, builder.build())

        // Executa áudio imediato com AudioManager AudioFocus Ducking para garantir que o som se sobreponha
        // a músicas (Spotify, etc.) ou navegação GPS (Waze, Google Maps)
        CustomSoundPlayer.playOfferSound(context, offer = offer, isUrgent = isUrgent)
        HapticFeedbackHelper.vibrateHighPriorityOffer(context)

        return true
    }

    /**
     * Agenda uma notificação de alta prioridade com som para disparar após um delay em segundos.
     * Útil para o piloto testar a notificação enquanto minimiza o app para o segundo plano.
     */
    fun scheduleDelayedBackgroundNotification(
        delaySeconds: Int = 3,
        customOffer: RadarOffer? = null,
        filterCriteria: OfferFilterCriteria? = null,
        onScheduled: () -> Unit = {}
    ) {
        val testOffer = customOffer ?: RadarOffer(
            id = "test_bg_${System.currentTimeMillis()}",
            appName = "iFood",
            restaurant = "Outback Steakhouse Jardins",
            value = 34.50,
            distanceKm = 3.8,
            timeMinutes = 14,
            pickupAddress = "Av. Paulista, 1200",
            destinationAddress = "R. Bela Cintra, 850",
            neuralDecision = NeuralDecision(
                decision = "ACEITAR_IMEDIATAMENTE",
                reason = "Excelente rentabilidade de R$ 9.07/km em rota rápida",
                confidence = 0.98
            ),
            gainPerKm = 9.07,
            netProfit = 33.72
        )

        onScheduled()

        scope.launch {
            delay(delaySeconds * 1000L)
            showHighPriorityOfferNotification(testOffer, filterCriteria)
        }
    }

    /**
     * Cancela a notificação de um pedido específico.
     */
    fun cancelOfferNotification(offerId: String) {
        val notificationId = NOTIFICATION_ID_BASE + (offerId.hashCode() % 1000)
        notificationManager.cancel(notificationId)
    }

    /**
     * Cancela todas as notificações de ofertas ativas.
     */
    fun clearAllNotifications() {
        notificationManager.cancelAll()
    }
}
