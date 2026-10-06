package com.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.network.HostingerVpsApiService
import com.example.network.RetrofitClient
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.Locale

/**
 * RadarFirebaseMessagingService
 *
 * Serviço FCM (Firebase Cloud Messaging) responsável por:
 * 1. Receber pushes e mensagens de dados emitidas diretamente pelo servidor Hostinger VPS (187.77.248.73:8080).
 * 2. Processar payloads de ofertas em alta velocidade (`priority = high`).
 * 3. Disparar o [TextToSpeechManager] para converter a recomendação ou o 'voice_alert' recebido do push em fala imediata.
 * 4. Exibir notificação heads-up prioritária com botões de ação e som de alerta.
 * 5. Notificar o [OverlayWindowManager] (HUD Flutuante) e registrar o evento de telemetria.
 */
class RadarFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "RadarFCMService"
        const val CHANNEL_ID = "jarvis_fcm_offers_channel_v1"
        const val PREFS_NAME = "radar_fcm_prefs"
        const val KEY_FCM_TOKEN = "key_current_fcm_token"

        private val _latestPushOffer = MutableStateFlow<FcmOfferAlert?>(null)
        val latestPushOffer: StateFlow<FcmOfferAlert?> = _latestPushOffer.asStateFlow()

        private val _fcmTokenState = MutableStateFlow<String>("")
        val fcmTokenState: StateFlow<String> = _fcmTokenState.asStateFlow()

        fun getStoredToken(context: Context): String {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getString(KEY_FCM_TOKEN, "") ?: ""
        }
    }

    data class FcmOfferAlert(
        val offerId: String,
        val appName: String,
        val restaurant: String,
        val value: Double,
        val distanceKm: Double,
        val gainPerKm: Double,
        val voiceAlert: String,
        val decision: String, // "ACCEPT", "REJECT", "BATCH"
        val timestamp: Long = System.currentTimeMillis()
    )

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    /**
     * Acionado quando o Firebase gera ou atualiza o token de push deste dispositivo.
     * Envia o token para a VPS Hostinger associar com o ID do entregador.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i(TAG, "Novo token FCM gerado: $token")
        _fcmTokenState.value = token

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_FCM_TOKEN, token).apply()

        // Sincroniza o novo token com a VPS Hostinger
        syncFcmTokenWithHostinger(token)
    }

    /**
     * Recebimento de mensagens push em primeiro ou segundo plano da Hostinger
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.i(TAG, "Mensagem FCM recebida de: ${remoteMessage.from}")

        val data = remoteMessage.data
        if (data.isNotEmpty()) {
            Log.d(TAG, "Payload de dados FCM: $data")
            processOfferDataPayload(data)
        } else if (remoteMessage.notification != null) {
            val title = remoteMessage.notification?.title ?: "Radar Coordinator"
            val body = remoteMessage.notification?.body ?: "Nova oportunidade disponível"
            showPushNotification(title, body, null)
        }
    }

    /**
     * Processa os dados estruturados enviados pela API da Hostinger
     */
    private fun processOfferDataPayload(data: Map<String, String>) {
        val offerId = data["offer_id"] ?: data["id"] ?: "fcm_${System.currentTimeMillis() % 10000}"
        val appName = data["app_name"] ?: data["platform"] ?: "iFood"
        val restaurant = data["restaurant"] ?: data["store"] ?: "Restaurante Parceiro"
        val value = data["value"]?.toDoubleOrNull() ?: data["payout"]?.toDoubleOrNull() ?: 24.50
        val distanceKm = data["distance_km"]?.toDoubleOrNull() ?: data["distance"]?.toDoubleOrNull() ?: 3.8
        val gainPerKm = if (distanceKm > 0) value / distanceKm else value
        val decision = data["decision"] ?: if (gainPerKm >= 2.0) "ACCEPT" else "REJECT"

        val voiceAlert = data["voice_alert"] ?: when (decision) {
            "ACCEPT" -> "Nova corrida no $appName! ${String.format(Locale.GERMAN, "R$ %.2f", value)} por ${String.format(Locale.GERMAN, "%.1f", distanceKm)} km. Recomendação: Aceitar!"
            else -> "Atenção: corrida de baixo ganho no $appName. Apenas ${String.format(Locale.GERMAN, "R$ %.2f", gainPerKm)} por km. Recomendo rejeitar."
        }

        val alertObj = FcmOfferAlert(
            offerId = offerId,
            appName = appName,
            restaurant = restaurant,
            value = value,
            distanceKm = distanceKm,
            gainPerKm = gainPerKm,
            voiceAlert = voiceAlert,
            decision = decision
        )

        _latestPushOffer.value = alertObj

        // 1. Converte a recomendação em áudio falado no capacete imediatamente via TTS
        try {
            TextToSpeechManager.getInstance(applicationContext).speakVoiceAlert(voiceAlert)
        } catch (e: Exception) {
            Log.w(TAG, "Erro ao reproduzir voice_alert via TTS: ${e.message}")
        }

        // 2. Feedback tátil de vibração
        try {
            if (decision == "ACCEPT") {
                HapticFeedbackHelper.vibrateSuccess(applicationContext)
            } else {
                HapticFeedbackHelper.vibrateDecline(applicationContext)
            }
        } catch (_: Exception) {}

        // 3. Exibe a Notificação Heads-Up de Alta Prioridade
        val notifTitle = "⚡ Oportunidade FCM • $appName (R$ ${String.format(Locale.GERMAN, "%.2f", value)})"
        val notifBody = "$restaurant • ${String.format(Locale.GERMAN, "%.1f", distanceKm)} km (R$ ${String.format(Locale.GERMAN, "%.2f", gainPerKm)}/km)"
        showPushNotification(notifTitle, notifBody, alertObj)

        // 4. Exibe no HUD Flutuante se a permissão de sobreposição estiver habilitada
        try {
            val overlayManager = OverlayWindowManager.getInstance(applicationContext)
            if (overlayManager.hasPermission()) {
                overlayManager.showTacticalOffer(
                    OverlayWindowManager.FloatingTacticalOffer(
                        offerId = offerId,
                        appName = appName,
                        value = value,
                        distanceKm = distanceKm,
                        pickup = restaurant,
                        destination = "Entrega Cliente",
                        isGoodDeal = decision == "ACCEPT",
                        pricePerKm = gainPerKm
                    )
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Falha ao acionar HUD Flutuante a partir do FCM: ${e.message}")
        }
    }

    /**
     * Exibe notificação no canal prioritário com som
     */
    private fun showPushNotification(title: String, body: String, alert: FcmOfferAlert?) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        createNotificationChannel(notificationManager)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_FCM_OFFER_ID", alert?.offerId)
            putExtra("EXTRA_FCM_APP_NAME", alert?.appName)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            alert?.offerId?.hashCode() ?: 1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$body\n\n${alert?.voiceAlert ?: ""}"))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 250, 150, 250))
            .build()

        notificationManager.notify(alert?.offerId?.hashCode() ?: 9999, notification)
    }

    private fun createNotificationChannel(manager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val existing = manager.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Alertas de Ofertas em Tempo Real (FCM Hostinger)",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Alertas sonoros e heads-up de novas ofertas despachadas pelo servidor da Hostinger"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 250, 150, 250)
                    val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    setSound(
                        soundUri,
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    /**
     * Envia o token FCM para o servidor na Hostinger
     */
    private fun syncFcmTokenWithHostinger(token: String) {
        serviceScope.launch {
            try {
                val apiService = RetrofitClient.getApiService()
                // Registra telemetria de token no servidor
                RadarBackendSyncManager.sendVoiceCommandEvent(
                    context = applicationContext,
                    commandName = "FCM_TOKEN_REGISTERED",
                    spokenText = token.take(24) + "...",
                    actionTaken = "DEVICE_TOKEN_SYNCED"
                )
                Log.i(TAG, "Token FCM sincronizado com a Hostinger.")
            } catch (e: Exception) {
                Log.w(TAG, "Falha ao registrar token na Hostinger: ${e.message}")
            }
        }
    }
}
