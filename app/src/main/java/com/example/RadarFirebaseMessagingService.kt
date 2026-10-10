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

        fun clearLatestOffer() {
            _latestPushOffer.value = null
        }

        /**
         * Inicializa o Firebase Cloud Messaging, obtém o token de registro atual,
         * sincroniza com o backend VPS Hostinger e assina os tópicos de broadcast de ofertas.
         */
        fun initializeFcm(context: Context) {
            try {
                com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val token = task.result ?: return@addOnCompleteListener
                        Log.i(TAG, "Token FCM obtido na inicialização: $token")
                        _fcmTokenState.value = token
                        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        prefs.edit().putString(KEY_FCM_TOKEN, token).apply()
                        RadarBackendSyncManager.registerDeviceFcmToken(context, token)
                    } else {
                        Log.w(TAG, "Falha ao obter token FCM na inicialização: ${task.exception?.message}")
                    }
                }

                // Assina tópicos padrão para recebimento de chamadas de entrega
                com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic("offers")
                com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic("delivery_alerts")
            } catch (e: Exception) {
                Log.w(TAG, "Erro ao configurar FirebaseMessaging: ${e.message}")
            }
        }

        /**
         * Simula a recepção de uma oferta de entrega via FCM diretamente no app para testes práticos.
         */
        fun simulateIncomingOffer(
            context: Context,
            appName: String = "iFood",
            restaurant: String = "Habib's Radial Leste",
            value: Double = 28.50,
            distanceKm: Double = 4.2
        ) {
            val gainPerKm = if (distanceKm > 0) value / distanceKm else value
            val decision = if (gainPerKm >= 2.0) "ACCEPT" else "REJECT"
            val offerId = "fcm_test_${System.currentTimeMillis() % 10000}"
            val ttsEngine = OfferTextToSpeechEngine.getInstance(context)
            val voiceAlert = ttsEngine.buildOfferSpeechText(
                appName = appName,
                restaurant = restaurant,
                value = value,
                distanceKm = distanceKm,
                profitPerKm = gainPerKm,
                isGoodDeal = decision == "ACCEPT"
            )

            val alert = FcmOfferAlert(
                offerId = offerId,
                appName = appName,
                restaurant = restaurant,
                value = value,
                distanceKm = distanceKm,
                gainPerKm = gainPerKm,
                voiceAlert = voiceAlert,
                decision = decision
            )

            _latestPushOffer.value = alert

            // Áudio via OfferTextToSpeechEngine (Android TTS) e vibração
            try {
                ttsEngine.speakFcmOffer(alert)
                HapticFeedbackHelper.vibrateSuccess(context)
            } catch (_: Exception) {}
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

    override fun onCreate() {
        super.onCreate()
        try {
            // Pré-aquece e inicializa o motor Text-to-Speech nativo do Android
            OfferTextToSpeechEngine.getInstance(applicationContext)
            Log.i(TAG, "OfferTextToSpeechEngine pré-aquecido no RadarFirebaseMessagingService")
        } catch (e: Exception) {
            Log.w(TAG, "Falha ao pré-aquecer Text-to-Speech: ${e.message}")
        }
    }

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

        // Subscreve nos tópicos de entregas em tempo real
        try {
            com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic("offers")
            com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic("delivery_alerts")
        } catch (_: Exception) {}

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
            try {
                OfferTextToSpeechEngine.getInstance(applicationContext).speak(
                    "$title. $body",
                    android.speech.tts.TextToSpeech.QUEUE_FLUSH
                )
            } catch (_: Exception) {}
        }
    }

    /**
     * Processa os dados estruturados enviados pela API da Hostinger
     */
    private fun processOfferDataPayload(data: Map<String, String>) {
        val type = data["type"] ?: data["action"] ?: ""
        if (type.contains("CANCEL", ignoreCase = true) || data.containsKey("cancelled_restaurant")) {
            val cancelledRestaurant = data["cancelled_restaurant"] ?: data["restaurant"] ?: "Restaurante Parceiro"
            Log.w(TAG, "Push FCM de cancelamento recebido para: $cancelledRestaurant")
            try {
                StackedOrderCancellationManager.processSubOrderCancellationByTarget(
                    context = applicationContext,
                    targetRestaurantOrApp = cancelledRestaurant,
                    offersList = emptyList(),
                    completedDeliveriesList = emptyList()
                )
                HapticFeedbackHelper.vibrateDecline(applicationContext)
                OfferTextToSpeechEngine.getInstance(applicationContext).speak(
                    "Atenção: pedido cancelado no $cancelledRestaurant. Rota recalculada.",
                    android.speech.tts.TextToSpeech.QUEUE_FLUSH
                )
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao processar push de cancelamento: ${e.message}")
            }
            return
        }

        val offerId = data["offer_id"] ?: data["id"] ?: "fcm_${System.currentTimeMillis() % 10000}"
        val appName = data["app_name"] ?: data["platform"] ?: "iFood"
        val restaurant = data["restaurant"] ?: data["store"] ?: "Restaurante Parceiro"
        val value = data["value"]?.toDoubleOrNull() ?: data["payout"]?.toDoubleOrNull() ?: 24.50
        val distanceKm = data["distance_km"]?.toDoubleOrNull() ?: data["distance"]?.toDoubleOrNull() ?: 3.8
        val gainPerKm = if (distanceKm > 0) value / distanceKm else value
        val isGoodDeal = gainPerKm >= 2.0
        val decision = data["decision"] ?: if (isGoodDeal) "ACCEPT" else "REJECT"

        // Constrói áudio humanizado com pronúncia perfeita em português brasileiro (R$, km, centavos)
        val voiceAlert = data["voice_alert"]?.takeIf { it.isNotBlank() } ?: OfferTextToSpeechEngine.getInstance(applicationContext).buildOfferSpeechText(
            appName = appName,
            restaurant = restaurant,
            value = value,
            distanceKm = distanceKm,
            profitPerKm = gainPerKm,
            isGoodDeal = decision == "ACCEPT" || isGoodDeal
        )

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

        // 1. Converte a recomendação FCM em áudio falado no capacete imediatamente via TTS nativo do Android
        try {
            OfferTextToSpeechEngine.getInstance(applicationContext).speakFcmOffer(alertObj)
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

        // Ação rápida para aceitar corrida direto da notificação Heads-Up
        val acceptIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            action = "ACTION_ACCEPT_OFFER_FROM_NOTIFICATION"
            putExtra("EXTRA_FCM_OFFER_ID", alert?.offerId)
            putExtra("EXTRA_AUTO_ACCEPT", true)
        }
        val acceptPendingIntent = PendingIntent.getActivity(
            this,
            (alert?.offerId?.hashCode() ?: 1002) + 1,
            acceptIntent,
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
            .addAction(android.R.drawable.ic_input_add, "Aceitar Corrida", acceptPendingIntent)
            .addAction(android.R.drawable.ic_delete, "Dispensar", pendingIntent)
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
                RadarBackendSyncManager.registerDeviceFcmToken(applicationContext, token)
                Log.i(TAG, "Token FCM sincronizado com a Hostinger.")
            } catch (e: Exception) {
                Log.w(TAG, "Falha ao registrar token na Hostinger: ${e.message}")
            }
        }
    }
}
