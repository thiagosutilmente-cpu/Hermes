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
 * 1. Receber pushes e mensagens de dados emitidas diretamente pelo servidor Hostinger VPS (187.77.248.73:8000).
 * 2. Processar payloads de ofertas em alta velocidade (`priority = high`).
 * 3. Disparar o [TextToSpeechManager] para converter a recomendação ou o 'voice_alert' recebido do push em fala imediata.
 * 4. Exibir notificação heads-up prioritária com botões de ação e som de alerta.
 * 5. Notificar o [OverlayWindowManager] (HUD Flutuante) e registrar o evento de telemetria.
 */
class RadarFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "RadarFCMService"
        const val CHANNEL_ID = "jarvis_fcm_offers_channel_v1"
        const val BLITZ_CHANNEL_ID = "jarvis_fcm_blitz_channel_v1"
        const val COMBO_CHANNEL_ID = "jarvis_fcm_combos_channel_v1"
        const val PREFS_NAME = "radar_fcm_prefs"
        const val KEY_FCM_TOKEN = "key_current_fcm_token"

        private val _latestPushOffer = MutableStateFlow<FcmOfferAlert?>(null)
        val latestPushOffer: StateFlow<FcmOfferAlert?> = _latestPushOffer.asStateFlow()

        private val _latestBlitzAlert = MutableStateFlow<FcmBlitzAlert?>(null)
        val latestBlitzAlert: StateFlow<FcmBlitzAlert?> = _latestBlitzAlert.asStateFlow()

        private val _latestMergedComboPush = MutableStateFlow<FcmMergedComboAlert?>(null)
        val latestMergedComboPush: StateFlow<FcmMergedComboAlert?> = _latestMergedComboPush.asStateFlow()

        private val _blitzHistory = MutableStateFlow<List<FcmBlitzAlert>>(emptyList())
        val blitzHistory: StateFlow<List<FcmBlitzAlert>> = _blitzHistory.asStateFlow()

        private val _fcmTokenState = MutableStateFlow<String>("")
        val fcmTokenState: StateFlow<String> = _fcmTokenState.asStateFlow()

        fun getStoredToken(context: Context): String {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getString(KEY_FCM_TOKEN, "") ?: ""
        }

        fun clearLatestOffer() {
            _latestPushOffer.value = null
        }

        fun clearLatestBlitz() {
            _latestBlitzAlert.value = null
        }

        fun clearLatestCombo() {
            _latestMergedComboPush.value = null
        }

        /**
         * Inicializa o Firebase Cloud Messaging, obtém o token de registro atual,
         * sincroniza com o backend VPS Hostinger e assina os tópicos de broadcast
         * de ofertas, alertas de blitz e mesclagens de corrida.
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

                // Assina tópicos padrão para recebimento de chamadas de entrega, blitz e mesclagens
                val messaging = com.google.firebase.messaging.FirebaseMessaging.getInstance()
                messaging.subscribeToTopic("offers")
                messaging.subscribeToTopic("delivery_alerts")
                messaging.subscribeToTopic("blitz_alerts")
                messaging.subscribeToTopic("police_radar")
                messaging.subscribeToTopic("merge_offers")
                messaging.subscribeToTopic("combo_alerts")
                messaging.subscribeToTopic("critical_safety")
                Log.i(TAG, "🛡️ FCM: Tópicos de Blitz e Mesclagens assinados com sucesso.")
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

        /**
         * Simula a recepção de um alerta crítico de Blitz Policial via push FCM.
         */
        fun simulateIncomingBlitzAlert(
            context: Context,
            location: String = "Av. Radial Leste x Rua Bresser",
            direction: String = "Sentido Bairro",
            details: String = "Comando da PM com guincho e fiscalização de documentos",
            severity: String = "CRITICAL"
        ) {
            val voiceAlert = "Atenção piloto: Alerta crítico de Blitz policial confirmado na $location, $direction. Reduza a velocidade e verifique documentação!"
            val alert = FcmBlitzAlert(
                id = "blitz_sim_${System.currentTimeMillis() % 10000}",
                location = location,
                direction = direction,
                details = details,
                severity = severity,
                voiceAlert = voiceAlert
            )
            _latestBlitzAlert.value = alert
            _blitzHistory.value = listOf(alert) + _blitzHistory.value.take(20)

            try {
                OfferTextToSpeechEngine.getInstance(context).speak(
                    voiceAlert,
                    android.speech.tts.TextToSpeech.QUEUE_FLUSH
                )
                HapticFeedbackHelper.vibrateBlitzUrgent(context)
            } catch (_: Exception) {}
        }

        /**
         * Simula a recepção de uma nova oferta de mesclagem multi-app via push FCM.
         */
        fun simulateIncomingMergedCombo(
            context: Context,
            app1: String = "iFood",
            restaurant1: String = "Madero Container - Paulista",
            payout1: Double = 19.50,
            app2: String = "99Food",
            restaurant2: String = "Drogaria São Paulo",
            payout2: Double = 18.00
        ) {
            val totalPayout = payout1 + payout2
            val totalDistKm = 3.6
            val extraDevMeters = 650
            val netProfitPerHour = 52.00
            val corridorLabel = "Mesmo Eixo Viário (< 15°)"
            val voiceAlert = "Jarvis: Nova mesclagem de pedidos detectada! $app1 mais $app2. Ganho total de R$ ${String.format(Locale.US, "%.2f", totalPayout)} com desvio de apenas ${extraDevMeters} metros no mesmo corredor!"

            val alert = FcmMergedComboAlert(
                comboId = "combo_sim_${System.currentTimeMillis() % 10000}",
                app1 = app1,
                restaurant1 = restaurant1,
                payout1 = payout1,
                distance1Km = 3.0,
                app2 = app2,
                restaurant2 = restaurant2,
                payout2 = payout2,
                distance2Km = 2.8,
                totalPayout = totalPayout,
                combinedDistanceKm = totalDistKm,
                extraDeviationMeters = extraDevMeters,
                netProfitPerHour = netProfitPerHour,
                corridorLabel = corridorLabel,
                voiceAlert = voiceAlert,
                isDiamond = true
            )
            _latestMergedComboPush.value = alert

            // Injeta imediatamente no DeepOrderPoolMergingEngine para alimentar o Cockpit
            try {
                val poolEngine = DeepOrderPoolMergingEngine.getInstance(context)
                poolEngine.ingestPoolOffers(app1, listOf(
                    ScreenPoolOffer(
                        appName = app1,
                        packageName = "com.ifood.entregador",
                        restaurantName = restaurant1,
                        pickupAddress = "Av. Paulista, 1200",
                        dropoffAddress = "Rua Augusta, 1800",
                        value = payout1,
                        distanceKm = 3.0
                    )
                ))
                poolEngine.ingestPoolOffers(app2, listOf(
                    ScreenPoolOffer(
                        appName = app2,
                        packageName = "com.didiglobal.driver",
                        restaurantName = restaurant2,
                        pickupAddress = "Av. Paulista, 800",
                        dropoffAddress = "Alameda Lorena, 1400",
                        value = payout2,
                        distanceKm = 2.8
                    )
                ))
            } catch (_: Exception) {}

            try {
                OfferTextToSpeechEngine.getInstance(context).speak(
                    voiceAlert,
                    android.speech.tts.TextToSpeech.QUEUE_FLUSH
                )
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

    data class FcmBlitzAlert(
        val id: String = "blitz_${System.currentTimeMillis() % 10000}",
        val location: String,
        val direction: String = "Sentido Bairro",
        val details: String = "Comando policial de fiscalização de trânsito",
        val severity: String = "CRITICAL", // "CRITICAL", "HIGH", "MODERATE"
        val voiceAlert: String,
        val timestamp: Long = System.currentTimeMillis()
    )

    data class FcmMergedComboAlert(
        val comboId: String = "combo_${System.currentTimeMillis() % 10000}",
        val app1: String,
        val restaurant1: String,
        val payout1: Double,
        val distance1Km: Double,
        val app2: String,
        val restaurant2: String,
        val payout2: Double,
        val distance2Km: Double,
        val totalPayout: Double,
        val combinedDistanceKm: Double,
        val extraDeviationMeters: Int,
        val netProfitPerHour: Double,
        val corridorLabel: String,
        val voiceAlert: String,
        val isDiamond: Boolean = true,
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

        // 1. ALERTA CRÍTICO DE BLITZ POLICIAL (SEGURANÇA DO ENTREGADOR)
        if (type.contains("BLITZ", ignoreCase = true) ||
            type.contains("POLICE", ignoreCase = true) ||
            data.containsKey("blitz_location") ||
            data.containsKey("radar_blitz")
        ) {
            processBlitzAlert(data)
            return
        }

        // 2. NOVA OFERTA DE MESCLAGEM DE ENTREGAS (COMBO MULTI-APP)
        if (type.contains("COMBO", ignoreCase = true) ||
            type.contains("MERGE", ignoreCase = true) ||
            type.contains("MESCLAGEM", ignoreCase = true) ||
            data.containsKey("app2") ||
            data.containsKey("restaurant2")
        ) {
            processMergedComboAlert(data)
            return
        }

        // 3. CANCELAMENTO PARCIAL EM CORRIDA MESCLADA
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

        // 4. OFERTA INDIVIDUAL EM TEMPO REAL
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

        // 1. Áudio falado no capacete imediatamente via TTS nativo
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
     * Processa o alerta crítico de Blitz Policial transmitido via FCM.
     */
    private fun processBlitzAlert(data: Map<String, String>) {
        val location = data["location"] ?: data["blitz_location"] ?: data["street"] ?: "Via Expressa"
        val direction = data["direction"] ?: data["sentido"] ?: "Sentido Bairro"
        val details = data["details"] ?: data["description"] ?: "Comando da PM com guincho e fiscalização de documentação"
        val severity = data["severity"] ?: "CRITICAL"
        val alertId = data["blitz_id"] ?: data["id"] ?: "blitz_${System.currentTimeMillis() % 10000}"
        val voiceAlert = data["voice_alert"]?.takeIf { it.isNotBlank() }
            ?: "Atenção piloto: Alerta crítico de Blitz policial confirmado na $location, $direction. Reduza a velocidade e verifique documentação!"

        val alertObj = FcmBlitzAlert(
            id = alertId,
            location = location,
            direction = direction,
            details = details,
            severity = severity,
            voiceAlert = voiceAlert
        )

        _latestBlitzAlert.value = alertObj
        _blitzHistory.value = (listOf(alertObj) + _blitzHistory.value).distinctBy { it.id }.take(25)

        // 1. Áudio imediato no capacete via TTS
        try {
            OfferTextToSpeechEngine.getInstance(applicationContext).speak(
                voiceAlert,
                android.speech.tts.TextToSpeech.QUEUE_FLUSH
            )
        } catch (e: Exception) {
            Log.w(TAG, "Falha ao reproduzir áudio de blitz: ${e.message}")
        }

        // 2. Feedback tátil urgente no guidão/luvas
        try {
            HapticFeedbackHelper.vibrateBlitzUrgent(applicationContext)
        } catch (_: Exception) {}

        // 3. Notificação Heads-Up de Emergência no Canal Crítico de Blitz
        showBlitzNotification(alertObj)

        // 4. Alerta visual no HUD Flutuante
        try {
            val overlayManager = OverlayWindowManager.getInstance(applicationContext)
            if (overlayManager.hasPermission()) {
                overlayManager.showTacticalOffer(
                    OverlayWindowManager.FloatingTacticalOffer(
                        appName = "🚨 RADAR BLITZ",
                        value = 0.0,
                        distanceKm = 0.0,
                        pricePerKm = 0.0,
                        isGoodDeal = false,
                        dealLabel = "🚨 BLITZ POLICIAL CONFIRMADA ($direction)",
                        destination = details,
                        pickup = location,
                        hasDualRoute = false
                    )
                )
            }
        } catch (_: Exception) {}
    }

    /**
     * Processa uma nova oferta de mesclagem multi-app transmitida via FCM.
     */
    private fun processMergedComboAlert(data: Map<String, String>) {
        val comboId = data["combo_id"] ?: data["id"] ?: "combo_${System.currentTimeMillis() % 10000}"
        val app1 = data["app1"] ?: data["app_name_1"] ?: data["app_name"] ?: "iFood"
        val restaurant1 = data["restaurant1"] ?: data["restaurant_1"] ?: data["restaurant"] ?: "Madero Container - Paulista"
        val payout1 = data["payout1"]?.toDoubleOrNull() ?: data["value1"]?.toDoubleOrNull() ?: 19.50
        val dist1 = data["distance1"]?.toDoubleOrNull() ?: 3.0

        val app2 = data["app2"] ?: data["app_name_2"] ?: "99Food"
        val restaurant2 = data["restaurant2"] ?: data["restaurant_2"] ?: "Drogaria São Paulo"
        val payout2 = data["payout2"]?.toDoubleOrNull() ?: data["value2"]?.toDoubleOrNull() ?: 18.00
        val dist2 = data["distance2"]?.toDoubleOrNull() ?: 2.8

        val totalPayout = data["total_payout"]?.toDoubleOrNull() ?: (payout1 + payout2)
        val combinedDistKm = data["combined_distance_km"]?.toDoubleOrNull() ?: (dist1 + 0.8)
        val extraDeviationMeters = data["extra_deviation_meters"]?.toIntOrNull() ?: 650
        val netPerHour = data["net_profit_per_hour"]?.toDoubleOrNull() ?: 54.00
        val corridorLabel = data["corridor_label"] ?: "Mesmo Eixo Viário (< 15°)"

        val voiceAlert = data["voice_alert"]?.takeIf { it.isNotBlank() }
            ?: "Jarvis: Nova mesclagem de pedidos detectada! $app1 mais $app2. Ganho total de R$ ${String.format(Locale.US, "%.2f", totalPayout)} com desvio de apenas ${extraDeviationMeters} metros no mesmo corredor!"

        val comboObj = FcmMergedComboAlert(
            comboId = comboId,
            app1 = app1,
            restaurant1 = restaurant1,
            payout1 = payout1,
            distance1Km = dist1,
            app2 = app2,
            restaurant2 = restaurant2,
            payout2 = payout2,
            distance2Km = dist2,
            totalPayout = totalPayout,
            combinedDistanceKm = combinedDistKm,
            extraDeviationMeters = extraDeviationMeters,
            netProfitPerHour = netPerHour,
            corridorLabel = corridorLabel,
            voiceAlert = voiceAlert,
            isDiamond = true
        )

        _latestMergedComboPush.value = comboObj

        // Injeta os dois pedidos no DeepOrderPoolMergingEngine imediatamente para alimentar o Cockpit
        try {
            val poolEngine = DeepOrderPoolMergingEngine.getInstance(applicationContext)
            val offer1 = ScreenPoolOffer(
                appName = app1,
                packageName = if (app1.contains("ifood", ignoreCase = true)) "com.ifood.entregador" else "com.didiglobal.driver",
                restaurantName = restaurant1,
                pickupAddress = data["pickup1"] ?: "Av. Paulista, 1200",
                dropoffAddress = data["dropoff1"] ?: "Rua Augusta, 1800",
                value = payout1,
                distanceKm = dist1
            )
            val offer2 = ScreenPoolOffer(
                appName = app2,
                packageName = if (app2.contains("ifood", ignoreCase = true)) "com.ifood.entregador" else "com.didiglobal.driver",
                restaurantName = restaurant2,
                pickupAddress = data["pickup2"] ?: "Av. Paulista, 800",
                dropoffAddress = data["dropoff2"] ?: "Alameda Lorena, 1400",
                value = payout2,
                distanceKm = dist2
            )
            poolEngine.ingestPoolOffers(app1, listOf(offer1))
            poolEngine.ingestPoolOffers(app2, listOf(offer2))
        } catch (e: Exception) {
            Log.w(TAG, "Erro ao injetar pedidos no DeepOrderPoolMergingEngine: ${e.message}")
        }

        // 1. Áudio imediato via TTS
        try {
            OfferTextToSpeechEngine.getInstance(applicationContext).speak(
                voiceAlert,
                android.speech.tts.TextToSpeech.QUEUE_FLUSH
            )
        } catch (_: Exception) {}

        // 2. Feedback tátil afirmativo
        try {
            HapticFeedbackHelper.vibrateSuccess(applicationContext)
        } catch (_: Exception) {}

        // 3. Notificação Heads-Up com ação direta de aceitar combo e dividir tela
        showMergedComboNotification(comboObj)

        // 4. Overlay HUD
        try {
            val overlayManager = OverlayWindowManager.getInstance(applicationContext)
            if (overlayManager.hasPermission()) {
                overlayManager.showTacticalOffer(
                    OverlayWindowManager.FloatingTacticalOffer(
                        appName = "$app1 + $app2",
                        value = totalPayout,
                        distanceKm = combinedDistKm,
                        pricePerKm = totalPayout / combinedDistKm,
                        dealLabel = "💎 MESCLAGEM MULTI-APP (+${extraDeviationMeters}m)",
                        pickup = "$restaurant1 & $restaurant2",
                        destination = "Mesmo Corredor Viário",
                        isGoodDeal = true,
                        hasDualRoute = true,
                        dualRouteAppName = app2,
                        dualRouteExtraGain = payout2
                    )
                )
            }
        } catch (_: Exception) {}
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

    /**
     * Exibe notificação de emergência no canal crítico de Blitz Policial
     */
    private fun showBlitzNotification(alert: FcmBlitzAlert) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        createNotificationChannel(notificationManager)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_FCM_BLITZ_ID", alert.id)
            putExtra("EXTRA_FCM_BLITZ_LOCATION", alert.location)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            alert.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val ackIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            action = "ACTION_ACKNOWLEDGE_BLITZ_FROM_NOTIFICATION"
            putExtra("EXTRA_FCM_BLITZ_ID", alert.id)
        }
        val ackPendingIntent = PendingIntent.getActivity(
            this,
            alert.id.hashCode() + 1,
            ackIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(this, BLITZ_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("🚨 ALERTA CRÍTICO DE BLITZ POLICIAL")
            .setContentText("${alert.location} • ${alert.direction}")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Local: ${alert.location}\nSentido: ${alert.direction}\nDetalhes: ${alert.details}\n\n${alert.voiceAlert}"))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_directions, "Desviar Rota", pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Entendido", ackPendingIntent)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 400, 200, 400, 200, 600))
            .build()

        notificationManager.notify(alert.id.hashCode(), notification)
    }

    /**
     * Exibe notificação de nova mesclagem de pedidos no canal de combos
     */
    private fun showMergedComboNotification(combo: FcmMergedComboAlert) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        createNotificationChannel(notificationManager)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_FCM_COMBO_ID", combo.comboId)
            putExtra("EXTRA_FCM_COMBO_APP1", combo.app1)
            putExtra("EXTRA_FCM_COMBO_APP2", combo.app2)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            combo.comboId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val acceptComboIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            action = "ACTION_ACCEPT_COMBO_FROM_NOTIFICATION"
            putExtra("EXTRA_FCM_COMBO_ID", combo.comboId)
            putExtra("EXTRA_AUTO_ACCEPT", true)
        }
        val acceptComboPendingIntent = PendingIntent.getActivity(
            this,
            combo.comboId.hashCode() + 1,
            acceptComboIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(this, COMBO_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_input_add)
            .setContentTitle("💎 NOVA MESCLAGEM • ${combo.app1} + ${combo.app2} (R$ ${String.format(Locale.GERMAN, "%.2f", combo.totalPayout)})")
            .setContentText("${combo.restaurant1} & ${combo.restaurant2} • ${combo.corridorLabel} (+${combo.extraDeviationMeters}m)")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Total: R$ ${String.format(Locale.GERMAN, "%.2f", combo.totalPayout)} • ${combo.combinedDistanceKm} km\n${combo.corridorLabel} (+${combo.extraDeviationMeters}m desvio)\nLucro Est.: R$ ${String.format(Locale.GERMAN, "%.2f", combo.netProfitPerHour)}/h\n\n${combo.voiceAlert}"))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_input_add, "Aceitar Combo e Dividir Tela", acceptComboPendingIntent)
            .addAction(android.R.drawable.ic_delete, "Dispensar", pendingIntent)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 200, 100, 200, 100, 300))
            .build()

        notificationManager.notify(combo.comboId.hashCode(), notification)
    }

    private fun createNotificationChannel(manager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Canal 1: Ofertas Individuais
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
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

            // Canal 2: Alertas Críticos de Blitz Policial (Segurança)
            if (manager.getNotificationChannel(BLITZ_CHANNEL_ID) == null) {
                val blitzChannel = NotificationChannel(
                    BLITZ_CHANNEL_ID,
                    "Alertas Críticos de Blitz Policial (Segurança)",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Avisos urgentes de comandos de blitz e fiscalização de trânsito em tempo real"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 400, 200, 400, 200, 600)
                    val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    setSound(
                        soundUri,
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                }
                manager.createNotificationChannel(blitzChannel)
            }

            // Canal 3: Novas Ofertas de Mesclagem (Combos Multi-App)
            if (manager.getNotificationChannel(COMBO_CHANNEL_ID) == null) {
                val comboChannel = NotificationChannel(
                    COMBO_CHANNEL_ID,
                    "Novas Mesclagens de Pedidos (Combos Multi-App)",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notificações prioritárias de rotas combinadas de alto lucro líquido"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 200, 100, 200, 100, 300)
                    val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    setSound(
                        soundUri,
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                }
                manager.createNotificationChannel(comboChannel)
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
