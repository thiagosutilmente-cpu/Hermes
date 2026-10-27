package com.example

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.regex.Pattern

/**
 * Service que estende NotificationListenerService para leitura automática
 * de notificações recebidas de apps de entrega (iFood, Uber Driver, Rappi, 99 Entregas).
 * Extrai texto bruto de ganhos (R$) e distância (km), calcula ganho/km e
 * repassa para o motor neural de decisão do Jarvis.
 */
class AppNotificationListenerService : NotificationListenerService() {

    companion object {
        private const val TAG = "AppNotificationListener"

        // Pacotes conhecidos de apps de entrega parceiros
        val KNOWN_DELIVERY_PACKAGES = setOf(
            "com.ifood.entregador",
            "br.com.ifood.driver",
            "com.ubercab.driver",
            "com.rappi.courier",
            "com.tapps.driver",
            "com.didiglobal.driver"
        )

        // Última oferta interceptada das notificações
        private val _lastInterceptedOffer = MutableStateFlow<RadarOffer?>(null)
        val lastInterceptedOffer: StateFlow<RadarOffer?> = _lastInterceptedOffer.asStateFlow()

        // Total de ofertas interceptadas nesta sessão
        private val _interceptedOffersCount = MutableStateFlow(0)
        val interceptedOffersCount: StateFlow<Int> = _interceptedOffersCount.asStateFlow()

        // Total de notificações repetidas descartadas pelo filtro inteligente
        private val _blockedDuplicatesCount = MutableStateFlow(0)
        val blockedDuplicatesCount: StateFlow<Int> = _blockedDuplicatesCount.asStateFlow()

        // Estado de conexão do Listener de Notificações do Android
        private val _isServiceConnected = MutableStateFlow(false)
        val isServiceConnected: StateFlow<Boolean> = _isServiceConnected.asStateFlow()

        // Padrões Regex para extração de valores em R$, distâncias em KM e Metros
        private val VALUE_REGEX = Pattern.compile("(?:R\\$|BRL)\\s*([0-9]+[\\.,]?[0-9]*)", Pattern.CASE_INSENSITIVE)
        private val FALLBACK_VALUE_REGEX = Pattern.compile("([0-9]+[\\.,][0-9]{2})\\s*(?:reais|R\\$)", Pattern.CASE_INSENSITIVE)
        private val DISTANCE_REGEX = Pattern.compile("([0-9]+[\\.,]?[0-9]*)\\s*(?:km|quil[oô]metros)", Pattern.CASE_INSENSITIVE)
        private val METERS_REGEX = Pattern.compile("([0-9]+)\\s*(?:m|metros)(?!\\w)", Pattern.CASE_INSENSITIVE)
        private val TIME_REGEX = Pattern.compile("([0-9]+)\\s*(?:min|minutos)", Pattern.CASE_INSENSITIVE)

        /**
         * Solicita reconexão automática ao sistema Android caso o serviço tenha sido desconectado.
         */
        fun requestServiceRebind(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    requestRebind(ComponentName(context, AppNotificationListenerService::class.java))
                    Log.i(TAG, "Rebind de NotificationListenerService solicitado com sucesso.")
                } catch (e: Exception) {
                    Log.w(TAG, "Falha ao solicitar rebind de notificação: ${e.message}")
                }
            }
        }

        // Janela de resfriamento contra duplicatas (12 segundos)
        const val DUPLICATE_COOLDOWN_MS = 12_000L
        private val recentFingerprints = mutableMapOf<String, Long>()

        @Synchronized
        fun isDuplicateOffer(fingerprint: String): Boolean {
            val now = System.currentTimeMillis()
            // Limpa entradas mais velhas que 30 segundos
            recentFingerprints.entries.removeAll { now - it.value > 30_000L }

            val lastSeen = recentFingerprints[fingerprint]
            if (lastSeen != null && (now - lastSeen) < DUPLICATE_COOLDOWN_MS) {
                return true
            }
            recentFingerprints[fingerprint] = now
            return false
        }

        /**
         * Permite testar o comportamento do interceptador e o descarte inteligente de duplicatas no cockpit.
         */
        @Synchronized
        fun simulateTestNotification(context: Context, isDuplicate: Boolean): Boolean {
            val pkg = "com.ifood.entregador"
            val fingerprint = "${pkg}_2450_35_Outback Shoppin"

            if (isDuplicate) {
                recentFingerprints[fingerprint] = System.currentTimeMillis()
                _blockedDuplicatesCount.value += 1
                return false
            } else {
                recentFingerprints.remove(fingerprint)
                val decision = NeuralDecision(RadarDecision.ACCEPT, "Ganho de R$ 7,00/km excelente", 0.95)
                val offer = RadarOffer(
                    id = "test_${System.currentTimeMillis() % 100000}",
                    appName = "iFood",
                    restaurant = "Outback Shopping Paulista",
                    value = 24.50,
                    distanceKm = 3.5,
                    estimatedTimeMin = 14,
                    neuralDecision = decision,
                    itemsCount = 1,
                    gainPerKm = 7.00,
                    fuelCost = 3.5 * 0.17,
                    netProfit = 24.50 - (3.5 * 0.17)
                )
                _lastInterceptedOffer.value = offer
                _interceptedOffersCount.value += 1
                recentFingerprints[fingerprint] = System.currentTimeMillis()

                try {
                    TextToSpeechManager.getInstance(context).speakVoiceAlert(
                        "Nova oferta interceptada do iFood: Outback Paulista, R$ 24,50 por 3,5 km."
                    )
                } catch (_: Exception) {}
                return true
            }
        }
    }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override fun onListenerConnected() {
        super.onListenerConnected()
        _isServiceConnected.value = true
        Log.d(TAG, "NotificationListenerService conectado ao Jarvis Neural Engine.")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        _isServiceConnected.value = false
        Log.d(TAG, "NotificationListenerService desconectado.")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        val packageName = sbn.packageName ?: return
        val extras = sbn.notification.extras ?: return

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: ""

        val fullPayload = "$title | $text | $bigText | $subText"

        // Verifica se a notificação é de um app de entregas ou contém termos de chamada de corrida
        val isDeliveryApp = KNOWN_DELIVERY_PACKAGES.any { packageName.contains(it, ignoreCase = true) } ||
                fullPayload.contains("corrida", ignoreCase = true) ||
                fullPayload.contains("entrega", ignoreCase = true) ||
                fullPayload.contains("pedido", ignoreCase = true)

        // DETECÇÃO CRÍTICA DE CANCELAMENTOS DE PEDIDOS (MULTI-STACK / PEDIDOS MESCLADOS)
        val isCancellationNotice = fullPayload.contains("cancelad", ignoreCase = true) ||
                fullPayload.contains("cancelou", ignoreCase = true) ||
                fullPayload.contains("cancelamento", ignoreCase = true) ||
                fullPayload.contains("desistência", ignoreCase = true)

        if (isCancellationNotice && (isDeliveryApp || fullPayload.contains("pedido", ignoreCase = true))) {
            scope.launch {
                handleCancellationNotification(packageName, title, text, fullPayload)
            }
            return
        }

        if (!isDeliveryApp && !fullPayload.contains("R$")) {
            return
        }

        val directIntent: PendingIntent? = sbn.notification.actions?.firstOrNull()?.actionIntent ?: sbn.notification.contentIntent

        scope.launch {
            parseDeliveryOffer(packageName, title, text, fullPayload, directIntent)
        }
    }

    private fun handleCancellationNotification(pkg: String, title: String, text: String, fullText: String) {
        val appName = when {
            pkg.contains("ifood", ignoreCase = true) -> "iFood"
            pkg.contains("uber", ignoreCase = true) -> "Uber Direct"
            pkg.contains("rappi", ignoreCase = true) -> "Rappi"
            pkg.contains("didi", ignoreCase = true) || pkg.contains("99", ignoreCase = true) -> "99 Food"
            else -> "App Parceiro"
        }

        val restaurantTarget = if (title.isNotBlank() && !title.contains("Cancel", ignoreCase = true)) {
            title
        } else {
            text.take(35).ifBlank { appName }
        }

        Log.w(TAG, "Notificação de cancelamento interceptada de $appName: $fullText")

        try {
            StackedOrderCancellationManager.processSubOrderCancellationByTarget(
                context = applicationContext,
                targetRestaurantOrApp = restaurantTarget,
                offersList = emptyList(),
                completedDeliveriesList = emptyList()
            )
            HapticFeedbackHelper.vibrateDecline(applicationContext)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao repassar cancelamento de notificação: ${e.message}")
        }
    }

    private fun parseDeliveryOffer(pkg: String, title: String, text: String, fullText: String, directIntent: PendingIntent? = null) {
        val appName = when {
            pkg.contains("ifood", ignoreCase = true) -> "iFood"
            pkg.contains("uber", ignoreCase = true) -> "Uber Direct"
            pkg.contains("rappi", ignoreCase = true) -> "Rappi"
            pkg.contains("didi", ignoreCase = true) || pkg.contains("99", ignoreCase = true) -> "99 Food"
            else -> "App Parceiro"
        }

        // 1. Extração do Valor Financeiro (R$)
        var value = 0.0
        val valueMatcher = VALUE_REGEX.matcher(fullText)
        if (valueMatcher.find()) {
            val raw = valueMatcher.group(1)?.replace(",", ".") ?: "0.0"
            value = raw.toDoubleOrNull() ?: 0.0
        } else {
            val fallbackMatcher = FALLBACK_VALUE_REGEX.matcher(fullText)
            if (fallbackMatcher.find()) {
                val raw = fallbackMatcher.group(1)?.replace(",", ".") ?: "0.0"
                value = raw.toDoubleOrNull() ?: 0.0
            }
        }

        // 2. Extração da Distância Total (km ou metros)
        var distanceKm = 1.0
        val distMatcher = DISTANCE_REGEX.matcher(fullText)
        if (distMatcher.find()) {
            val rawDist = distMatcher.group(1)?.replace(",", ".") ?: "1.0"
            distanceKm = rawDist.toDoubleOrNull() ?: 1.0
        } else {
            // Suporte para coletas e entregas curtas expressas em metros (ex: 800m ou 500 metros)
            val metersMatcher = METERS_REGEX.matcher(fullText)
            if (metersMatcher.find()) {
                val meters = metersMatcher.group(1)?.toDoubleOrNull() ?: 1000.0
                distanceKm = (meters / 1000.0).coerceAtLeast(0.1)
            }
        }

        // 3. Tempo Estimado (min)
        var timeMin = 15
        val timeMatcher = TIME_REGEX.matcher(fullText)
        if (timeMatcher.find()) {
            timeMin = timeMatcher.group(1)?.toIntOrNull() ?: 15
        }

        if (value <= 0.0) {
            // Notificação sem valor financeiro explícito
            return
        }

        val restaurantName = if (title.isNotBlank() && !title.contains("Nova Entrega", ignoreCase = true)) {
            title
        } else {
            text.take(30).ifBlank { "Coleta Delivery" }
        }

        // 3.B. Filtro Anti-Duplicata Inteligente com Janela de Resfriamento (12s)
        val fingerprint = "${pkg}_${(value * 100).toInt()}_${(distanceKm * 10).toInt()}_${restaurantName.take(15).trim()}"
        if (isDuplicateOffer(fingerprint)) {
            _blockedDuplicatesCount.value += 1
            Log.d(TAG, "🛡️ [FILTRO ANTI-DUPLICATA] Notificação repetida descartada silenciosamente: $appName - R$ $value (${_blockedDuplicatesCount.value} duplicatas evitadas)")
            return
        }

        // Processamento tático via JARVIS NEURAL COCKPIT
        val jarvisResponse = JarvisNeuralCockpitEngine.processInput(fullText)
        if (jarvisResponse.status == "SUCCESS" && jarvisResponse.rideData != null) {
            val rd = jarvisResponse.rideData
            val isAccept = (rd.decision.action == "ACCEPT" || rd.decision.action == "BATCH")
            val decision = NeuralDecision(
                if (isAccept) RadarDecision.ACCEPT else RadarDecision.DECLINE,
                rd.decision.reason,
                (rd.decision.score / 100.0).coerceIn(0.1, 0.99)
            )

            val offer = RadarOffer(
                id = "notif_${System.currentTimeMillis() % 100000}",
                appName = rd.platform,
                restaurant = restaurantName,
                value = rd.financials.payoutBrl,
                distanceKm = rd.financials.totalDistanceKm,
                estimatedTimeMin = rd.financials.estimatedTimeMinutes,
                neuralDecision = decision,
                itemsCount = 1,
                gainPerKm = rd.financials.ratePerKm,
                fuelCost = rd.financials.totalDistanceKm * 0.17,
                netProfit = rd.financials.payoutBrl - (rd.financials.totalDistanceKm * 0.17)
            )

            // Alimenta o motor de Mesclagem Profunda para cruzamento imediato com outras ofertas abertas
            try {
                val poolOffer = ScreenPoolOffer(
                    appName = rd.platform,
                    packageName = pkg,
                    restaurantName = restaurantName,
                    pickupAddress = restaurantName,
                    dropoffAddress = "Destino Push",
                    value = rd.financials.payoutBrl,
                    distanceKm = rd.financials.totalDistanceKm
                )
                DeepOrderPoolMergingEngine.getInstance(applicationContext).ingestPoolOffers(rd.platform, listOf(poolOffer))
            } catch (_: Exception) {}

            _lastInterceptedOffer.value = offer
            _interceptedOffersCount.value += 1

            // Publica instantaneamente no barramento sintético GhostAPI (disponível via REST na porta 8899)
            try {
                GhostApiVirtualBus.publishOffer(
                    GhostApiVirtualBus.UnifiedDeliveryOffer(
                        id = offer.id,
                        platform = rd.platform,
                        restaurant = restaurantName,
                        pickupAddress = restaurantName,
                        deliveryAddress = "Destino Push",
                        payoutBrl = rd.financials.payoutBrl,
                        distanceKm = rd.financials.totalDistanceKm,
                        ratePerKm = rd.financials.ratePerKm,
                        estimatedMinutes = rd.financials.estimatedTimeMinutes,
                        rawActionIntent = directIntent
                    )
                )
            } catch (_: Exception) {}

            // Validação rigorosa dos filtros definidos pelo entregador (raio de alcance e piso de ganho)
            val savedCriteria = FilterPreferencesManager.getSavedCriteria(applicationContext)
            val locState = LocationService.globalLocationState.value
            val gpsCriteria = if (locState.latitude != 0.0 && locState.longitude != 0.0) {
                savedCriteria.copy(driverCurrentLat = locState.latitude, driverCurrentLng = locState.longitude)
            } else {
                savedCriteria
            }

            if (!gpsCriteria.matchesNotification(offer)) {
                Log.d(TAG, "Notificação de ${rd.platform} filtrada silenciosamente: fora do raio GPS de restaurante ou abaixo do piso de R$/km.")
                return
            }

            // Vocaliza imediatamente no capacete via TTS nativo do Android
            try {
                OfferTextToSpeechEngine.getInstance(applicationContext).speak(
                    rd.voiceAlert.text,
                    android.speech.tts.TextToSpeech.QUEUE_FLUSH
                )
            } catch (e: Exception) {
                Log.w(TAG, "Falha ao emitir áudio TTS da corrida Jarvis: ${e.message}")
            }

            // Dispara assinatura tátil calibrada para guidão de moto (identificação sem olhar a tela)
            try {
                val isRejected = (rd.decision.action == "DECLINE" || rd.decision.action == "REJECT")
                HapticFeedbackEngine.getInstance(applicationContext).classifyAndVibrate(
                    payout = rd.financials.payoutBrl,
                    distanceKm = rd.financials.totalDistanceKm,
                    isAutoRejected = isRejected,
                    platform = rd.platform
                )
            } catch (e: Exception) {
                Log.w(TAG, "Falha ao acionar feedback háptico: ${e.message}")
            }

            Log.i(TAG, "Jarvis Cockpit processou oferta de ${rd.platform}: R$ ${rd.financials.payoutBrl} (${rd.financials.ratePerKm} R$/km) -> Decisão: ${rd.decision.action}")
            return
        }

        // Fallback legado se a notificação não tiver o formato completo
        val gainPerKm = if (distanceKm > 0) value / distanceKm else value
        val fuelCost = distanceKm * 0.17
        val netProfit = value - fuelCost

        // Dispara assinatura tátil para a notificação de fallback
        try {
            HapticFeedbackEngine.getInstance(applicationContext).classifyAndVibrate(
                payout = value,
                distanceKm = distanceKm,
                isAutoRejected = !isAccept,
                platform = appName
            )
        } catch (e: Exception) {
            Log.w(TAG, "Falha no feedback tátil legado: ${e.message}")
        }

        // Decisão Neural preliminar com base nas metas do condutor
        val isAccept = gainPerKm >= 3.0
        val decision = NeuralDecision(
            if (isAccept) RadarDecision.ACCEPT else RadarDecision.DECLINE,
            if (isAccept) "Ganho excelente: R$ ${String.format(Locale.US, "%.2f", gainPerKm)}/km" else "Abaixo do piso",
            if (isAccept) 0.96 else 0.45
        )

        val restaurantName = if (title.isNotBlank() && !title.contains("Nova Entrega", ignoreCase = true)) {
            title
        } else {
            text.take(30).ifBlank { "Coleta Delivery" }
        }

        val offer = RadarOffer(
            id = "notif_${System.currentTimeMillis() % 100000}",
            appName = appName,
            restaurant = restaurantName,
            value = value,
            distanceKm = distanceKm,
            estimatedTimeMin = timeMin,
            neuralDecision = decision,
            itemsCount = 1,
            gainPerKm = gainPerKm,
            fuelCost = fuelCost,
            netProfit = netProfit
        )

        _lastInterceptedOffer.value = offer
        _interceptedOffersCount.value += 1

        Log.d(TAG, "Oferta interceptada de $appName: R$ $value | $distanceKm km | R$ $gainPerKm/km -> Decisão: ${decision.decision}")
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }
}
