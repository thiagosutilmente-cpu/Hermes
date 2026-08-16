package com.example.model

import java.util.UUID

/**
 * Tipo de parada no itinerário do motoboy
 */
enum class StopActionType {
    PICKUP,   // Coleta de pedido no restaurante/loja
    DELIVERY  // Entrega na casa/escritório do cliente
}

/**
 * Status operacional da parada
 */
enum class StopExecutionStatus {
    PENDING,     // Aguardando piloto chegar
    NEXT,        // Próxima parada imediata (ativa no GPS/Waze)
    ARRIVED,     // No local (aguardando pedido ou cliente)
    COMPLETED,   // Concluído (coletado ou entregue)
    SKIPPED      // Pulado ou cancelado
}

/**
 * Representa um ponto de parada detalhado e mesclado para a rota multi-app.
 * Contém orientações práticas e táticas para o motoboy (código de retirada, dicas, nome do app).
 */
data class MergedDeliveryStop(
    val id: String = UUID.randomUUID().toString(),
    val offerBatchId: String = "",
    val appName: String,
    val actionType: StopActionType,
    val status: StopExecutionStatus = StopExecutionStatus.PENDING,
    val establishmentOrCustomer: String,
    val fullAddress: String,
    val latitude: Double,
    val longitude: Double,
    val orderCode: String = "",
    val customerNotes: String = "",
    val itemsSummary: String = "",
    val fareShare: Double = 0.0,
    val estimatedWaitOrPrepMinutes: Int = 0,
    val sequenceNumber: Int = 1,
    val tacticalGuidance: String = ""
)

/**
 * Estado completo do itinerário mesclado ativo (Active Merged Route)
 */
data class ActiveMergedRouteState(
    val batchId: String,
    val title: String,
    val appsInvolved: List<String>,
    val totalEarnings: Double,
    val totalDistanceKm: Double,
    val totalEstimatedMinutes: Int,
    val waitTimeSavedMinutes: Int,
    val stops: List<MergedDeliveryStop>,
    val currentStopIndex: Int = 0,
    val isRouteActive: Boolean = false,
    val startedTimestamp: Long = 0L,
    val completedStopsCount: Int = 0
) {
    val currentStop: MergedDeliveryStop?
        get() = stops.getOrNull(currentStopIndex)

    val nextStop: MergedDeliveryStop?
        get() = stops.getOrNull(currentStopIndex + 1)

    val remainingStops: List<MergedDeliveryStop>
        get() = if (currentStopIndex < stops.size) stops.subList(currentStopIndex, stops.size) else emptyList()

    val progressPct: Float
        get() = if (stops.isNotEmpty()) (completedStopsCount.toFloat() / stops.size.toFloat()) else 0f
}
