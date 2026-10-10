package com.example

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Evento estruturado de cancelamento de sub-pedido em corrida mesclada (Multi-Stack Dissolution).
 */
data class StackCancellationEvent(
    val id: String = UUID.randomUUID().toString(),
    val originalOfferId: String,
    val appName: String,
    val cancelledRestaurant: String,
    val cancelledApp: String,
    val cancelledValue: Double,
    val remainingRestaurants: String,
    val remainingValue: Double,
    val remainingDistanceKm: Double,
    val remainingGainPerKm: Double,
    val remainingOrdersCount: Int,
    val isViable: Boolean,
    val verdictTitle: String, // "ROTA RESTANTE LUCRATIVA" ou "ALERTA CRÍTICO DE PREJUÍZO"
    val explanation: String,
    val voiceSpeechText: String,
    val timestampMillis: Long = System.currentTimeMillis(),
    val timestampFormatted: String = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()),
    val updatedRadarOffer: RadarOffer? = null
)

/**
 * StackedOrderCancellationManager
 *
 * Gerenciador tático para cancelamentos parciais em entregas mescladas (Multi-Stack / Corridas Agrupadas).
 *
 * Resolve o problema crítico em que o entregador aceitou uma corrida combinada (ex: iFood + Rappi por R$ 38),
 * mas um dos restaurantes ou clientes cancela um dos pedidos:
 *
 * 1. Recalcula instantaneamente o valor real restante e a rota de entrega sem o nó cancelado.
 * 2. Faz auditoria neural de viabilidade: detecta se a corrida que restou virou prejuízo (ex: R$ 8 por 6km)
 *    ou se ainda compensa financeiramente.
 * 3. Notifica o piloto via aviso de voz prioritário (TTS no fone) para não ir até o restaurante à toa.
 * 4. Ajusta o saldo diário (`todayEarnings`), o extrato e remove o pedido fantasma do GPS.
 * 5. Registra o evento auditável no log de decisões com o motivo do reajuste.
 */
object StackedOrderCancellationManager {

    private const val TAG = "StackCancelManager"

    // Limiar mínimo aceitável de ganho/km para a corrida restante não virar prejuízo
    const val MIN_VIABLE_GAIN_PER_KM = 3.50

    // Alerta ativo no topo da tela
    private val _activeCancellationAlert = MutableStateFlow<StackCancellationEvent?>(null)
    val activeCancellationAlert: StateFlow<StackCancellationEvent?> = _activeCancellationAlert.asStateFlow()

    // Histórico de cancelamentos ocorridos no turno
    private val _cancellationHistory = MutableStateFlow<List<StackCancellationEvent>>(emptyList())
    val cancellationHistory: StateFlow<List<StackCancellationEvent>> = _cancellationHistory.asStateFlow()

    /**
     * Processa o cancelamento de um sub-pedido dentro de uma oferta mesclada.
     */
    fun processSubOrderCancellation(
        context: Context,
        originalOffer: RadarOffer,
        cancelledRestaurantTarget: String? = null
    ): StackCancellationEvent {
        val subOrders = originalOffer.subOrders

        // Se não houver subOrders explícitas, cria uma divisão estimada para análise
        val effectiveSubOrders = if (subOrders.isNotEmpty()) {
            subOrders
        } else {
            listOf(
                SubDeliveryOrder(
                    appName = originalOffer.appName.split("+").firstOrNull()?.trim() ?: "iFood",
                    appColor = RedIFood,
                    restaurant = originalOffer.restaurant.split("&").firstOrNull()?.trim() ?: originalOffer.restaurant,
                    value = originalOffer.value * 0.55,
                    distanceKm = originalOffer.distanceKm * 0.6,
                    pickupAddress = originalOffer.pickupAddress,
                    destinationAddress = originalOffer.destinationAddress
                ),
                SubDeliveryOrder(
                    appName = originalOffer.appName.split("+").getOrNull(1)?.trim() ?: "Rappi",
                    appColor = OrangeRappi,
                    restaurant = originalOffer.restaurant.split("&").getOrNull(1)?.trim() ?: "Segundo Restaurante",
                    value = originalOffer.value * 0.45,
                    distanceKm = originalOffer.distanceKm * 0.7,
                    pickupAddress = "Coleta Secundária",
                    destinationAddress = "Entrega Secundária"
                )
            )
        }

        // Seleciona o sub-pedido cancelado
        val cancelledSub = if (!cancelledRestaurantTarget.isNullOrEmpty()) {
            effectiveSubOrders.find {
                it.restaurant.contains(cancelledRestaurantTarget, ignoreCase = true) ||
                it.appName.contains(cancelledRestaurantTarget, ignoreCase = true)
            } ?: effectiveSubOrders.first()
        } else {
            // Cancela preferencialmente o segundo ou primeiro da lista
            if (effectiveSubOrders.size > 1) effectiveSubOrders[1] else effectiveSubOrders[0]
        }

        val remainingSubOrders = effectiveSubOrders.filterNot { it == cancelledSub }

        // Recálculo financeiro e de distância
        val cancelledValue = cancelledSub.value
        val remainingValue = if (remainingSubOrders.isNotEmpty()) {
            val sumSub = remainingSubOrders.sumOf { it.value }
            if (sumSub > 0.0) sumSub else (originalOffer.value - cancelledValue).coerceAtLeast(0.0)
        } else {
            0.0
        }

        // Em corridas mescladas, a distância total restante é calculada a partir dos sub-pedidos ou otimizada
        val remainingDistanceKm = if (remainingSubOrders.isNotEmpty()) {
            val sumDist = remainingSubOrders.sumOf { it.distanceKm }
            if (sumDist > 0.0) sumDist else (originalOffer.distanceKm * 0.70).coerceAtLeast(1.5)
        } else {
            0.0
        }

        val remainingGainPerKm = if (remainingDistanceKm > 0.0) {
            remainingValue / remainingDistanceKm
        } else {
            0.0
        }

        val remainingRestaurants = if (remainingSubOrders.isNotEmpty()) {
            remainingSubOrders.joinToString(" & ") { it.restaurant }
        } else {
            "Nenhum pedido restante (Cancelamento Total)"
        }

        val remainingApps = if (remainingSubOrders.isNotEmpty()) {
            remainingSubOrders.map { it.appName }.distinct().joinToString(" + ")
        } else {
            originalOffer.appName
        }

        // Análise Neural de Viabilidade do Trecho Restante
        val isViable = remainingSubOrders.isNotEmpty() &&
                remainingGainPerKm >= MIN_VIABLE_GAIN_PER_KM &&
                remainingValue >= 11.00

        val verdictTitle = if (remainingSubOrders.isEmpty()) {
            "CANCELAMENTO TOTAL"
        } else if (isViable) {
            "ROTA RESTANTE LUCRATIVA"
        } else {
            "ALERTA DE PREJUÍZO"
        }

        val explanation = if (remainingSubOrders.isEmpty()) {
            "Todos os pedidos foram cancelados pela plataforma. Retorne à espera de chamadas."
        } else if (isViable) {
            "O cancelamento reduziu ${formatBrl(cancelledValue)}, mas a rota restante ainda compensa com ${formatBrl(remainingGainPerKm)}/km (${formatBrl(remainingValue)} por ${formatKm(remainingDistanceKm)})."
        } else {
            "PREJUÍZO DETECTADO: O cancelamento de ${formatBrl(cancelledValue)} deixou uma rota deficitária de apenas ${formatBrl(remainingGainPerKm)}/km (${formatBrl(remainingValue)} para ${formatKm(remainingDistanceKm)}). Recomendado rejeitar no app."
        }

        val voiceSpeechText = if (remainingSubOrders.isEmpty()) {
            "Atenção piloto: O pedido da corrida mesclada foi cancelado. Rota finalizada sem entregas pendentes."
        } else if (isViable) {
            "Atenção: O pedido de ${cancelledSub.restaurant} foi cancelado. Porém a rota restante ainda compensa! ${formatBrl(remainingValue)} por ${formatKm(remainingDistanceKm)}, dando ${formatBrl(remainingGainPerKm)} por quilômetro. Prossiga com a entrega de ${remainingRestaurants}."
        } else {
            "Alerta de prejuízo! O pedido de ${cancelledSub.restaurant} foi cancelado. A entrega que sobrou paga apenas ${formatBrl(remainingValue)} para ${formatKm(remainingDistanceKm)}. Ganho de apenas ${formatBrl(remainingGainPerKm)} por quilômetro. Recomendamos rejeitar ou cancelar no aplicativo."
        }

        // Filtra waypoints eliminando as menções ao restaurante cancelado
        val updatedWaypoints = originalOffer.waypointRoute.filterNot {
            it.contains(cancelledSub.restaurant, ignoreCase = true) ||
            it.contains(cancelledSub.pickupAddress, ignoreCase = true)
        }

        val updatedOffer = originalOffer.copy(
            id = "${originalOffer.id}_recal",
            appName = remainingApps,
            restaurant = remainingRestaurants,
            value = remainingValue,
            distanceKm = remainingDistanceKm,
            isMultiStack = remainingSubOrders.size > 1,
            subOrders = remainingSubOrders,
            waypointRoute = updatedWaypoints,
            neuralDecision = NeuralDecision(
                decision = if (isViable) "accept" else "decline",
                confidence = if (isViable) 0.92 else 0.40,
                reason = explanation
            )
        )

        val event = StackCancellationEvent(
            originalOfferId = originalOffer.id,
            appName = originalOffer.appName,
            cancelledRestaurant = cancelledSub.restaurant,
            cancelledApp = cancelledSub.appName,
            cancelledValue = cancelledValue,
            remainingRestaurants = remainingRestaurants,
            remainingValue = remainingValue,
            remainingDistanceKm = remainingDistanceKm,
            remainingGainPerKm = remainingGainPerKm,
            remainingOrdersCount = remainingSubOrders.size,
            isViable = isViable,
            verdictTitle = verdictTitle,
            explanation = explanation,
            voiceSpeechText = voiceSpeechText,
            updatedRadarOffer = updatedOffer
        )

        _activeCancellationAlert.value = event
        _cancellationHistory.value = listOf(event) + _cancellationHistory.value.take(19)

        // Emite voz e feedback háptico
        try {
            TextToSpeechManager.getInstance(context).speak(voiceSpeechText, priority = true)
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao sintetizar áudio TTS de cancelamento: ${e.message}")
        }

        if (isViable) {
            HapticFeedbackHelper.vibrateTap(context)
        } else {
            HapticFeedbackHelper.vibrateDecline(context)
        }

        // Atualiza a sobreposição flutuante tática (HUD) se ativa
        try {
            if (OverlayWindowManager.canDrawOverlays(context)) {
                OverlayWindowManager.getInstance(context).showCancellationAlert(event)
            }
        } catch (_: Exception) {}

        // Registra evento de auditoria
        OfferDecisionLogManager.logPartialCancellation(
            context = context,
            offerId = originalOffer.id,
            appName = originalOffer.appName,
            cancelledRestaurant = cancelledSub.restaurant,
            cancelledValue = cancelledValue,
            remainingValue = remainingValue,
            remainingGainPerKm = remainingGainPerKm,
            reason = explanation
        )

        return event
    }

    /**
     * Aplica o resultado do cancelamento nas listas ativas do aplicativo, metas e telemetria.
     */
    fun applyCancellationToState(
        context: Context,
        event: StackCancellationEvent,
        offersList: MutableList<RadarOffer>,
        completedDeliveriesList: MutableList<CompletedDeliveryItem>,
        onAdjustTodayEarnings: (Double) -> Unit,
        onAdjustTotalKm: ((Double) -> Unit)? = null,
        onDecrementDeliveries: (() -> Unit)? = null,
        onClearPickupSelection: (() -> Unit)? = null
    ) {
        // 1. Se a oferta cancelada ainda estava no radar (pendente de aceite)
        val radarIndex = offersList.indexOfFirst { it.id == event.originalOfferId || it.restaurant.contains(event.cancelledRestaurant) }
        if (radarIndex != -1) {
            if (event.remainingOrdersCount > 0 && event.updatedRadarOffer != null) {
                // Atualiza em tempo real a oferta no radar para os novos valores
                offersList[radarIndex] = event.updatedRadarOffer
            } else {
                // Remove completamente do radar
                offersList.removeAt(radarIndex)
            }
        }

        // 2. Se a oferta já havia sido aceita e estava registrada nas entregas do turno
        val completedIndex = completedDeliveriesList.indexOfFirst {
            it.id == event.originalOfferId || it.restaurant.contains(event.cancelledRestaurant)
        }
        if (completedIndex != -1) {
            val oldItem = completedDeliveriesList[completedIndex]
            if (event.remainingOrdersCount > 0) {
                // Reajusta o valor abatendo o pedido cancelado
                val updatedItem = oldItem.copy(
                    grossValue = event.remainingValue,
                    distanceKm = event.remainingDistanceKm,
                    restaurant = event.remainingRestaurants,
                    appSource = event.updatedRadarOffer?.appName ?: oldItem.appSource
                )
                completedDeliveriesList[completedIndex] = updatedItem
                onAdjustTodayEarnings(-event.cancelledValue)
                onAdjustTotalKm?.invoke(-(oldItem.distanceKm - event.remainingDistanceKm))

                // Estorna faturamento nas metas diárias
                DailyGoalTrackerManager.getInstance(context).rollbackDeliveryEarnings(
                    cancelledGross = event.cancelledValue,
                    decrementRides = false
                )
            } else {
                // Cancelamento total: remove do extrato e abate o valor e distância integrais
                completedDeliveriesList.removeAt(completedIndex)
                onAdjustTodayEarnings(-oldItem.grossValue)
                onAdjustTotalKm?.invoke(-oldItem.distanceKm)
                onDecrementDeliveries?.invoke()
                onClearPickupSelection?.invoke()

                // Estorna faturamento integral e decrementa corrida nas metas
                DailyGoalTrackerManager.getInstance(context).rollbackDeliveryEarnings(
                    cancelledGross = oldItem.grossValue,
                    decrementRides = true
                )
            }
        }

        // 3. Atualiza persistência local no Cache Room (se disponível)
        try {
            val cacheRepo = RadarCacheRepository.getInstance(context)
            val newStatus = if (event.remainingOrdersCount > 0) "CANCELLED_PARTIAL" else "CANCELLED"
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                cacheRepo.updateOfferStatus(event.originalOfferId, newStatus)
            }
        } catch (_: Exception) {}

        // 4. Despacha telemetria de dissolução de stack para o backend VPS
        try {
            RadarBackendSyncManager.sendOfferDecision(
                context = context,
                offerId = event.originalOfferId,
                appName = event.appName,
                restaurant = "Cancelamento Parcial: ${event.cancelledRestaurant}",
                value = event.remainingValue,
                distanceKm = event.remainingDistanceKm,
                gainPerKm = event.remainingGainPerKm,
                action = if (event.remainingOrdersCount > 0) "CANCELLED_PARTIAL" else "CANCELLED_TOTAL",
                source = "Stack Dissolution Engine",
                reason = event.explanation,
                deliveryAddress = event.remainingRestaurants
            )
        } catch (_: Exception) {}
    }

    /**
     * Processa cancelamento buscando automaticamente nas ofertas ativas pelo nome do app ou restaurante.
     * Útil quando o evento é disparado via Notificação (NotificationListener) ou Acessibilidade.
     */
    fun processSubOrderCancellationByTarget(
        context: Context,
        targetRestaurantOrApp: String,
        offersList: List<RadarOffer>,
        completedDeliveriesList: List<CompletedDeliveryItem>
    ): StackCancellationEvent {
        // Procura primeiro em ofertas no radar
        val matchingOffer = offersList.find {
            it.restaurant.contains(targetRestaurantOrApp, ignoreCase = true) ||
            it.appName.contains(targetRestaurantOrApp, ignoreCase = true)
        } ?: offersList.find { it.isMultiStack }

        if (matchingOffer != null) {
            return processSubOrderCancellation(context, matchingOffer, targetRestaurantOrApp)
        }

        // Caso a oferta já tenha sido aceita e esteja nas entregas concluídas
        val matchingCompleted = completedDeliveriesList.find {
            it.restaurant.contains(targetRestaurantOrApp, ignoreCase = true) ||
            it.appSource.contains(targetRestaurantOrApp, ignoreCase = true)
        }

        val offerToCancel = if (matchingCompleted != null) {
            RadarOffer(
                id = matchingCompleted.id,
                appName = matchingCompleted.appSource,
                restaurant = matchingCompleted.restaurant,
                value = matchingCompleted.grossValue,
                distanceKm = matchingCompleted.distanceKm,
                estimatedTimeMin = matchingCompleted.timeMinutes,
                isMultiStack = matchingCompleted.restaurant.contains("&") || matchingCompleted.appSource.contains("+"),
                neuralDecision = NeuralDecision("accept", "Ativa em rota", 0.90)
            )
        } else {
            // Fallback para stack demonstrativo se nenhum encontrado
            MergedDeliverySearchEngine.getCuratedMergedStacks().first()
        }

        return processSubOrderCancellation(context, offerToCancel, targetRestaurantOrApp)
    }

    /**
     * Fecha o alerta visual ativo
     */
    fun dismissAlert() {
        _activeCancellationAlert.value = null
    }

    /**
     * Gera uma simulação tática de cancelamento de sub-pedido em corrida mesclada.
     */
    fun simulateStackCancellation(context: Context, offersList: List<RadarOffer>): StackCancellationEvent {
        // Procura primeiro uma oferta mesclada no radar
        val multiOffer = offersList.find { it.isMultiStack || it.appName.contains("+") }
            ?: MergedDeliverySearchEngine.getCuratedMergedStacks().first()

        return processSubOrderCancellation(
            context = context,
            originalOffer = multiOffer
        )
    }

    private fun formatBrl(value: Double): String {
        return String.format(Locale("pt", "BR"), "R$ %.2f", value)
    }

    private fun formatKm(distanceKm: Double): String {
        return String.format(Locale.ROOT, "%.1f km", distanceKm)
    }
}
