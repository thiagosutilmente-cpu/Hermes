package com.example

import android.app.Notification
import android.content.Intent
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

        // Padrões Regex para extração de valores em R$ e distâncias em KM
        private val VALUE_REGEX = Pattern.compile("(?:R\\$|BRL)\\s*([0-9]+[\\.,]?[0-9]*)", Pattern.CASE_INSENSITIVE)
        private val DISTANCE_REGEX = Pattern.compile("([0-9]+[\\.,]?[0-9]*)\\s*(?:km|quil[oô]metros)", Pattern.CASE_INSENSITIVE)
        private val TIME_REGEX = Pattern.compile("([0-9]+)\\s*(?:min|minutos)", Pattern.CASE_INSENSITIVE)
    }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "NotificationListenerService conectado ao Jarvis Neural Engine.")
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

        if (!isDeliveryApp && !fullPayload.contains("R$")) {
            return
        }

        scope.launch {
            parseDeliveryOffer(packageName, title, text, fullPayload)
        }
    }

    private fun parseDeliveryOffer(pkg: String, title: String, text: String, fullText: String) {
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
        }

        // 2. Extração da Distância Total (km)
        var distanceKm = 1.0
        val distMatcher = DISTANCE_REGEX.matcher(fullText)
        if (distMatcher.find()) {
            val rawDist = distMatcher.group(1)?.replace(",", ".") ?: "1.0"
            distanceKm = rawDist.toDoubleOrNull() ?: 1.0
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

        val gainPerKm = if (distanceKm > 0) value / distanceKm else value
        val fuelCost = distanceKm * 0.17
        val netProfit = value - fuelCost

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
