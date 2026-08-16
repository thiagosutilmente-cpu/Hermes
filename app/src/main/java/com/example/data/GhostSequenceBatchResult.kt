package com.example.data

import com.example.coordinator.ActiveOffer
import com.example.coordinator.DeliveryStop
import com.example.service.gemini.GeminiOfferEvaluation
import com.example.util.GhostBatchCandidate

/**
 * Representa o resultado consolidado do lote de pedidos inteligentes (Ghost Batch)
 * gerado pelo GhostSequenceBatching no Repositório.
 */
data class GhostSequenceBatchResult(
    val batchId: String,
    val combinedOffers: List<ActiveOffer>,
    val appNamesFormatted: String,
    val totalEarnings: Double,
    val totalDistanceKm: Double,
    val gainPerKm: Double,
    val estimatedTimeMinutes: Int,
    val waitTimeSavedMinutes: Int,
    val stopsInOptimizedOrder: List<DeliveryStop>,
    val isMultiApp: Boolean,
    val synergyConfidencePct: Int,
    val candidate: GhostBatchCandidate?,
    val evaluation: GeminiOfferEvaluation?
)
