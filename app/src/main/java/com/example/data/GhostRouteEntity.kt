package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.model.ActiveMergedRouteState
import com.example.model.MergedDeliveryStop
import com.example.model.StopActionType
import com.example.model.StopExecutionStatus

/**
 * Entidade Room para persistência local e cache offline de Rotas da Ghost Sequence.
 * Garante que o motoboy mantenha acesso aos itinerários, dados dos clientes e navegação mesmo sem sinal 4G/5G.
 */
@Entity(
    tableName = "ghost_routes",
    indices = [Index(value = ["isRouteActive"]), Index(value = ["updatedAt"])]
)
data class GhostRouteEntity(
    @PrimaryKey val batchId: String,
    val title: String,
    val appsInvolved: String, // CSV ex: "iFood,Rappi"
    val totalEarnings: Double,
    val totalDistanceKm: Double,
    val totalEstimatedMinutes: Int,
    val waitTimeSavedMinutes: Int,
    val currentStopIndex: Int = 0,
    val isRouteActive: Boolean = true,
    val startedTimestamp: Long = System.currentTimeMillis(),
    val completedStopsCount: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Entidade Room para as paradas individuais de cada Rota Mesclada salva.
 */
@Entity(
    tableName = "ghost_stops",
    indices = [Index(value = ["batchId"]), Index(value = ["sequenceNumber"])]
)
data class GhostStopEntity(
    @PrimaryKey val id: String,
    val batchId: String,
    val appName: String,
    val actionType: String, // PICKUP, DELIVERY
    val status: String, // PENDING, NEXT, ARRIVED, COMPLETED, SKIPPED
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

// Métodos de conversão entre entidades Room e modelos de domínio
fun ActiveMergedRouteState.toEntity(): GhostRouteEntity {
    return GhostRouteEntity(
        batchId = this.batchId,
        title = this.title,
        appsInvolved = this.appsInvolved.joinToString(","),
        totalEarnings = this.totalEarnings,
        totalDistanceKm = this.totalDistanceKm,
        totalEstimatedMinutes = this.totalEstimatedMinutes,
        waitTimeSavedMinutes = this.waitTimeSavedMinutes,
        currentStopIndex = this.currentStopIndex,
        isRouteActive = this.isRouteActive,
        startedTimestamp = this.startedTimestamp,
        completedStopsCount = this.completedStopsCount,
        updatedAt = System.currentTimeMillis()
    )
}

fun MergedDeliveryStop.toEntity(batchId: String): GhostStopEntity {
    return GhostStopEntity(
        id = this.id,
        batchId = batchId,
        appName = this.appName,
        actionType = this.actionType.name,
        status = this.status.name,
        establishmentOrCustomer = this.establishmentOrCustomer,
        fullAddress = this.fullAddress,
        latitude = this.latitude,
        longitude = this.longitude,
        orderCode = this.orderCode,
        customerNotes = this.customerNotes,
        itemsSummary = this.itemsSummary,
        fareShare = this.fareShare,
        estimatedWaitOrPrepMinutes = this.estimatedWaitOrPrepMinutes,
        sequenceNumber = this.sequenceNumber,
        tacticalGuidance = this.tacticalGuidance
    )
}

fun GhostStopEntity.toDomainModel(): MergedDeliveryStop {
    return MergedDeliveryStop(
        id = this.id,
        offerBatchId = this.batchId,
        appName = this.appName,
        actionType = try { StopActionType.valueOf(this.actionType) } catch (e: Exception) { StopActionType.PICKUP },
        status = try { StopExecutionStatus.valueOf(this.status) } catch (e: Exception) { StopExecutionStatus.PENDING },
        establishmentOrCustomer = this.establishmentOrCustomer,
        fullAddress = this.fullAddress,
        latitude = this.latitude,
        longitude = this.longitude,
        orderCode = this.orderCode,
        customerNotes = this.customerNotes,
        itemsSummary = this.itemsSummary,
        fareShare = this.fareShare,
        estimatedWaitOrPrepMinutes = this.estimatedWaitOrPrepMinutes,
        sequenceNumber = this.sequenceNumber,
        tacticalGuidance = this.tacticalGuidance
    )
}

fun GhostRouteEntity.toDomainModel(stops: List<MergedDeliveryStop>): ActiveMergedRouteState {
    return ActiveMergedRouteState(
        batchId = this.batchId,
        title = this.title,
        appsInvolved = this.appsInvolved.split(",").filter { it.isNotBlank() },
        totalEarnings = this.totalEarnings,
        totalDistanceKm = this.totalDistanceKm,
        totalEstimatedMinutes = this.totalEstimatedMinutes,
        waitTimeSavedMinutes = this.waitTimeSavedMinutes,
        stops = stops,
        currentStopIndex = this.currentStopIndex,
        isRouteActive = this.isRouteActive,
        startedTimestamp = this.startedTimestamp,
        completedStopsCount = this.completedStopsCount
    )
}
