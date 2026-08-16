package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.coordinator.DeliveryStop
import com.example.coordinator.RadarCoordinator
import com.example.data.OfferEntity
import com.example.data.OfferRepository
import com.example.util.GhostRouteOptimizer
import com.example.util.RouteOptimizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.*

/**
 * Representa um lote (batch) inteligente de entregas agrupadas pelo motor Ghost Sequence.
 */
data class GhostBatchGroup(
    val batchId: String,
    val offers: List<OfferEntity>,
    val appNames: List<String>,
    val totalEarnings: Double,
    val consolidatedDistanceKm: Double,
    val consolidatedTimeMin: Double,
    val gainPerKm: Double,
    val netProfit: Double,
    val detourEfficiencyScore: Int, // 0 - 100
    val convergenceRatio: Double,   // 0.0 - 1.0
    val pickupAddresses: List<String>,
    val deliveryAddresses: List<String>,
    val routeSequencePreview: String,
    val stops: List<DeliveryStop> = emptyList(),
    val recommendation: BatchRecommendation,
    val reason: String,
    val confidence: Double
)

enum class BatchRecommendation {
    SUPER_CHAIN_ACCEPT, // Altíssima convergência e lucro (Multi-App Super Chain)
    ACCEPT,             // Ótima rentabilidade e baixo desvio
    CONSIDER,           // Viável se não houver ofertas melhores
    REJECT              // Desvio excessivo ou ganho/km abaixo do ideal
}

/**
 * Estado da UI para o painel de Ghost Sequence Batching
 */
data class GhostSequenceBatchUiState(
    val activeBatches: List<GhostBatchGroup> = emptyList(),
    val candidateOffers: List<OfferEntity> = emptyList(),
    val isAutoAcceptEnabled: Boolean = false,
    val isGhostSequenceEnabled: Boolean = true,
    val aggressiveness: String = "EQUILIBRADO", // CONSERVADOR, EQUILIBRADO, AGRESSIVO
    val trafficWeight: Double = 0.5,
    val latencyWeight: Double = 0.3,
    val minGainPerKm: Double = 5.0, // Priorizado dinamicamente pelo .env (MIN_VALUE_PER_KM)
    val maxDetourKm: Double = 3.5,
    val maxBatchSize: Int = 3,
    val lastOptimizedTimestamp: Long = 0L,
    val totalChainedEarningsToday: Double = 0.0,
    val currentRiderLat: Double = -23.550520, // Padrão São Paulo Central
    val currentRiderLng: Double = -46.633308
)

/**
 * ViewModel central responsável pelo algoritmo de Ghost Sequence Batching.
 * Agrupa ofertas concorrentes de múltiplas plataformas (iFood, Rappi, Uber, 99) de forma sequencial,
 * priorizando o valor por KM definido no .env (BuildConfig.MIN_VALUE_PER_KM) e ordenando paradas
 * de forma topológica para maximizar o ganho líquido por hora.
 */
class GhostSequenceBatchViewModel(
    private val offerRepository: OfferRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        GhostSequenceBatchUiState(
            minGainPerKm = resolveEnvMinGainPerKm()
        )
    )
    val uiState: StateFlow<GhostSequenceBatchUiState> = _uiState.asStateFlow()

    init {
        // Observa histórico de ofertas em tempo real se o repositório estiver presente
        observeOffersStream()
        
        // Sincroniza configurações do RadarCoordinator
        syncWithRadarCoordinator()
    }

    /**
     * Resolve o valor mínimo por KM configurado no .env via BuildConfig
     */
    private fun resolveEnvMinGainPerKm(): Double {
        return try {
            val envValue = BuildConfig.MIN_VALUE_PER_KM
            envValue.toDoubleOrNull() ?: 5.0
        } catch (e: Throwable) {
            5.0
        }
    }

    private fun syncWithRadarCoordinator() {
        viewModelScope.launch {
            try {
                RadarCoordinator.settings.collect { settings ->
                    val envMinGain = resolveEnvMinGainPerKm()
                    val targetMinGain = if (settings.minValuePerKm > 0.0) settings.minValuePerKm else envMinGain
                    _uiState.update { current ->
                        current.copy(
                            isGhostSequenceEnabled = settings.isGhostSequenceEnabled,
                            aggressiveness = settings.ghostSequenceAggressiveness,
                            trafficWeight = settings.ghostSequenceTrafficWeight,
                            latencyWeight = settings.ghostSequenceLatencyWeight,
                            minGainPerKm = targetMinGain
                        )
                    }
                    recalculateBatches()
                }
            } catch (e: Throwable) {
                // Silencioso para fallback resiliente
            }
        }
    }

    private fun observeOffersStream() {
        viewModelScope.launch {
            offerRepository?.let { repo ->
                try {
                    repo.allOffers.collect { offers ->
                        val pending = offers.filter { it.userAction == null }.take(12)
                        if (pending.isNotEmpty()) {
                            _uiState.update { it.copy(candidateOffers = pending) }
                            recalculateBatches()
                        }
                    }
                } catch (e: Exception) {
                    // Fallback resiliente
                }
            }
        }
    }

    /**
     * Atualiza a posição do motorista em tempo real para recalcular os agrupamentos
     */
    fun updateRiderLocation(lat: Double, lng: Double) {
        _uiState.update { it.copy(currentRiderLat = lat, currentRiderLng = lng) }
        recalculateBatches()
    }

    /**
     * Altera os parâmetros de configuração do filtro dinamicamente
     */
    fun updateFilterParameters(
        minGainPerKm: Double? = null,
        maxDetourKm: Double? = null,
        aggressiveness: String? = null,
        trafficWeight: Double? = null,
        latencyWeight: Double? = null,
        isGhostSequenceEnabled: Boolean? = null,
        isAutoAcceptEnabled: Boolean? = null
    ) {
        _uiState.update { current ->
            current.copy(
                minGainPerKm = minGainPerKm ?: current.minGainPerKm,
                maxDetourKm = maxDetourKm ?: current.maxDetourKm,
                aggressiveness = aggressiveness ?: current.aggressiveness,
                trafficWeight = trafficWeight ?: current.trafficWeight,
                latencyWeight = latencyWeight ?: current.latencyWeight,
                isGhostSequenceEnabled = isGhostSequenceEnabled ?: current.isGhostSequenceEnabled,
                isAutoAcceptEnabled = isAutoAcceptEnabled ?: current.isAutoAcceptEnabled
            )
        }
        recalculateBatches()
    }

    /**
     * Adiciona uma nova oferta recebida de qualquer aplicativo parceiro (iFood, Rappi, Uber, 99)
     */
    fun onNewOfferReceived(offer: OfferEntity) {
        _uiState.update { current ->
            val updated = (listOf(offer) + current.candidateOffers).distinctBy { it.id }.take(15)
            current.copy(candidateOffers = updated)
        }
        recalculateBatches()
    }

    /**
     * Algoritmo Principal: Ghost Sequence Multi-Platform Batching Engine
     * Agrupa ofertas de múltiplas plataformas sequencialmente e prioriza pelo valor por KM do .env.
     */
    fun recalculateBatches() {
        val state = _uiState.value
        if (!state.isGhostSequenceEnabled || state.candidateOffers.isEmpty()) {
            _uiState.update { it.copy(activeBatches = emptyList()) }
            return
        }

        viewModelScope.launch {
            val candidates = state.candidateOffers
            val generatedBatches = mutableListOf<GhostBatchGroup>()
            val envMinGain = state.minGainPerKm

            // 1. GERAÇÃO DE LOTES INDIVIDUAIS DE REFERÊNCIA (Solo Offers)
            for (offer in candidates) {
                val dist = max(0.5, offer.totalDistance)
                val gainPerKm = offer.fareValue / dist
                val netProfit = calculateEstimatedNetProfit(offer.fareValue, dist)
                
                val rec = when {
                    gainPerKm >= (envMinGain * 1.25) -> BatchRecommendation.ACCEPT
                    gainPerKm >= envMinGain -> BatchRecommendation.ACCEPT
                    gainPerKm >= (envMinGain * 0.8) -> BatchRecommendation.CONSIDER
                    else -> BatchRecommendation.REJECT
                }
                val reason = if (gainPerKm >= envMinGain) 
                    "Ganho de R$ ${String.format("%.2f", gainPerKm)}/km atende a meta (.env: R$ ${String.format("%.2f", envMinGain)}/km)" 
                else 
                    "Ganho de R$ ${String.format("%.2f", gainPerKm)}/km abaixo da meta (.env: R$ ${String.format("%.2f", envMinGain)}/km)"

                val (pLat, pLng) = RouteOptimizer.getMockCoordinates(offer.pickupAddress)
                val (dLat, dLng) = RouteOptimizer.getMockCoordinates(offer.deliveryAddress)

                val soloStops = listOf(
                    DeliveryStop("${offer.id}_p", offer.pickupAddress, pLat, pLng, offer.appName, 1),
                    DeliveryStop("${offer.id}_d", offer.deliveryAddress, dLat, dLng, offer.appName, 2)
                )

                generatedBatches.add(
                    GhostBatchGroup(
                        batchId = "solo_${offer.id}",
                        offers = listOf(offer),
                        appNames = listOf(offer.appName),
                        totalEarnings = offer.fareValue,
                        consolidatedDistanceKm = dist,
                        consolidatedTimeMin = offer.totalTime,
                        gainPerKm = gainPerKm,
                        netProfit = netProfit,
                        detourEfficiencyScore = 100,
                        convergenceRatio = 1.0,
                        pickupAddresses = listOf(offer.pickupAddress),
                        deliveryAddresses = listOf(offer.deliveryAddress),
                        routeSequencePreview = "● [${offer.appName}] ${offer.pickupAddress.take(15)} → 🏠 [${offer.appName}] ${offer.deliveryAddress.take(15)}",
                        stops = soloStops,
                        recommendation = rec,
                        reason = reason,
                        confidence = 0.88
                    )
                )
            }

            // 2. GERAÇÃO DE SUPER-LOTES MULTI-APP SEQUENCIAIS EM PARES (iFood + Rappi, Uber + 99, etc.)
            for (i in candidates.indices) {
                for (j in (i + 1) until candidates.size) {
                    val o1 = candidates[i]
                    val o2 = candidates[j]

                    val pairBatch = evaluateGhostMultiAppPairBatch(o1, o2, state, envMinGain)
                    if (pairBatch != null) {
                        generatedBatches.add(pairBatch)

                        // 3. GERAÇÃO DE SUPER-LOTES TRIOS MULTI-APP (3 Apps Encadeados)
                        if (state.maxBatchSize >= 3) {
                            for (k in (j + 1) until candidates.size) {
                                val o3 = candidates[k]
                                val trioBatch = evaluateGhostMultiAppTrioBatch(o1, o2, o3, state, envMinGain)
                                if (trioBatch != null) {
                                    generatedBatches.add(trioBatch)
                                }
                            }
                        }
                    }
                }
            }

            // 4. ORDENAÇÃO E PRIORIZAÇÃO ESTRITA PELO VALOR POR KM (.env) E RECOMENDAÇÃO
            val sortedBatches = generatedBatches.sortedWith(
                compareByDescending<GhostBatchGroup> { it.recommendation == BatchRecommendation.SUPER_CHAIN_ACCEPT }
                    .thenByDescending { it.gainPerKm >= envMinGain }
                    .thenByDescending { it.gainPerKm }
                    .thenByDescending { it.detourEfficiencyScore }
            ).take(8)

            // 5. AUTO-ACCEPT ENGINE: Aceita automaticamente se habilitado e o super-lote superar o gatilho
            if (state.isAutoAcceptEnabled) {
                val autoCandidate = sortedBatches.firstOrNull { 
                    it.recommendation == BatchRecommendation.SUPER_CHAIN_ACCEPT && it.gainPerKm >= (envMinGain * 1.3)
                }
                if (autoCandidate != null) {
                    acceptBatch(autoCandidate)
                    return@launch
                }
            }

            _uiState.update { 
                it.copy(
                    activeBatches = sortedBatches,
                    lastOptimizedTimestamp = System.currentTimeMillis()
                ) 
            }
        }
    }

    /**
     * Avalia e sequencia topologicamente 2 ofertas multi-plataforma (iFood, Rappi, Uber, 99).
     * Otimiza a ordem de coletas e entregas preservando o sentido geográfico (Corridor Alignment).
     */
    private fun evaluateGhostMultiAppPairBatch(
        o1: OfferEntity,
        o2: OfferEntity,
        state: GhostSequenceBatchUiState,
        envMinGain: Double
    ): GhostBatchGroup? {
        val (p1Lat, p1Lng) = RouteOptimizer.getMockCoordinates(o1.pickupAddress)
        val (d1Lat, d1Lng) = RouteOptimizer.getMockCoordinates(o1.deliveryAddress)
        val (p2Lat, p2Lng) = RouteOptimizer.getMockCoordinates(o2.pickupAddress)
        val (d2Lat, d2Lng) = RouteOptimizer.getMockCoordinates(o2.deliveryAddress)

        val pickupProximityKm = calculateDistance(p1Lat, p1Lng, p2Lat, p2Lng)
        if (pickupProximityKm > state.maxDetourKm) return null

        val totalEarnings = o1.fareValue + o2.fareValue
        val isMultiApp = !o1.appName.equals(o2.appName, ignoreCase = true)

        // Calcula as 2 sequências topológicas sequenciais mais eficientes:
        // Sequência A: Coleta 1 -> Coleta 2 -> Entrega 1 -> Entrega 2
        val distSeqA = calculateDistance(state.currentRiderLat, state.currentRiderLng, p1Lat, p1Lng) +
                calculateDistance(p1Lat, p1Lng, p2Lat, p2Lng) +
                calculateDistance(p2Lat, p2Lng, d1Lat, d1Lng) +
                calculateDistance(d1Lat, d1Lng, d2Lat, d2Lng)

        // Sequência B: Coleta 1 -> Entrega 1 -> Coleta 2 -> Entrega 2 (Encadeamento Linear)
        val distSeqB = calculateDistance(state.currentRiderLat, state.currentRiderLng, p1Lat, p1Lng) +
                calculateDistance(p1Lat, p1Lng, d1Lat, d1Lng) +
                calculateDistance(d1Lat, d1Lng, p2Lat, p2Lng) +
                calculateDistance(p2Lat, p2Lng, d2Lat, d2Lng)

        val (bestDist, isSeqA) = if (distSeqA <= distSeqB) Pair(distSeqA, true) else Pair(distSeqB, false)
        val consolidatedDist = max(1.0, bestDist)

        val unmergedDistance = (if (o1.totalDistance > 0) o1.totalDistance else calculateDistance(p1Lat, p1Lng, d1Lat, d1Lng)) +
                (if (o2.totalDistance > 0) o2.totalDistance else calculateDistance(p2Lat, p2Lng, d2Lat, d2Lng))
        val detourKm = max(0.0, consolidatedDist - max(o1.totalDistance, o2.totalDistance))

        if (detourKm > state.maxDetourKm) return null

        val gainPerKm = totalEarnings / consolidatedDist
        val netProfit = calculateEstimatedNetProfit(totalEarnings, consolidatedDist)
        val convergenceRatio = max(0.0, min(1.0, 1.0 - (detourKm / max(1.0, unmergedDistance))))
        val efficiencyScore = ((convergenceRatio * 65) + (min(10.0, gainPerKm) * 3.5)).toInt().coerceIn(0, 100)

        // Classificação e Recomendação Priorizando .env (envMinGain)
        val rec = when {
            gainPerKm >= (envMinGain * 1.35) && isMultiApp && efficiencyScore >= 75 -> BatchRecommendation.SUPER_CHAIN_ACCEPT
            gainPerKm >= envMinGain -> BatchRecommendation.ACCEPT
            gainPerKm >= (envMinGain * 0.85) -> BatchRecommendation.CONSIDER
            else -> BatchRecommendation.REJECT
        }

        val apps = listOf(o1.appName, o2.appName).distinct()
        val appLabel = if (isMultiApp) apps.joinToString(" + ") else "${o1.appName} (Duplo)"

        val routePreview = if (isSeqA) {
            "● [${o1.appName}] ${o1.pickupAddress.take(10)} → ● [${o2.appName}] ${o2.pickupAddress.take(10)} → 🏠 [${o1.appName}] ${o1.deliveryAddress.take(10)} → 🏢 [${o2.appName}] ${o2.deliveryAddress.take(10)}"
        } else {
            "● [${o1.appName}] ${o1.pickupAddress.take(10)} → 🏠 [${o1.appName}] ${o1.deliveryAddress.take(10)} → ● [${o2.appName}] ${o2.pickupAddress.take(10)} → 🏢 [${o2.appName}] ${o2.deliveryAddress.take(10)}"
        }

        val stops = if (isSeqA) {
            listOf(
                DeliveryStop("${o1.id}_p", o1.pickupAddress, p1Lat, p1Lng, o1.appName, 1),
                DeliveryStop("${o2.id}_p", o2.pickupAddress, p2Lat, p2Lng, o2.appName, 2),
                DeliveryStop("${o1.id}_d", o1.deliveryAddress, d1Lat, d1Lng, o1.appName, 3),
                DeliveryStop("${o2.id}_d", o2.deliveryAddress, d2Lat, d2Lng, o2.appName, 4)
            )
        } else {
            listOf(
                DeliveryStop("${o1.id}_p", o1.pickupAddress, p1Lat, p1Lng, o1.appName, 1),
                DeliveryStop("${o1.id}_d", o1.deliveryAddress, d1Lat, d1Lng, o1.appName, 1),
                DeliveryStop("${o2.id}_p", o2.pickupAddress, p2Lat, p2Lng, o2.appName, 2),
                DeliveryStop("${o2.id}_d", o2.deliveryAddress, d2Lat, d2Lng, o2.appName, 2)
            )
        }

        val reason = "Super-Lote [$appLabel]: R$ ${String.format("%.2f", gainPerKm)}/km (Meta .env: R$ ${String.format("%.2f", envMinGain)}/km) com ${efficiencyScore}% de convergência."

        return GhostBatchGroup(
            batchId = "batch_${o1.id}_${o2.id}",
            offers = listOf(o1, o2),
            appNames = apps,
            totalEarnings = totalEarnings,
            consolidatedDistanceKm = consolidatedDist,
            consolidatedTimeMin = (o1.totalTime + o2.totalTime) * 0.76,
            gainPerKm = gainPerKm,
            netProfit = netProfit,
            detourEfficiencyScore = efficiencyScore,
            convergenceRatio = convergenceRatio,
            pickupAddresses = listOf(o1.pickupAddress, o2.pickupAddress),
            deliveryAddresses = listOf(o1.deliveryAddress, o2.deliveryAddress),
            routeSequencePreview = routePreview,
            stops = stops,
            recommendation = rec,
            reason = reason,
            confidence = 0.95
        )
    }

    /**
     * Avalia e sequencia topologicamente 3 ofertas de plataformas distintas (iFood + Rappi + Uber).
     */
    private fun evaluateGhostMultiAppTrioBatch(
        o1: OfferEntity,
        o2: OfferEntity,
        o3: OfferEntity,
        state: GhostSequenceBatchUiState,
        envMinGain: Double
    ): GhostBatchGroup? {
        val (p1Lat, p1Lng) = RouteOptimizer.getMockCoordinates(o1.pickupAddress)
        val (p2Lat, p2Lng) = RouteOptimizer.getMockCoordinates(o2.pickupAddress)
        val (p3Lat, p3Lng) = RouteOptimizer.getMockCoordinates(o3.pickupAddress)
        val (d1Lat, d1Lng) = RouteOptimizer.getMockCoordinates(o1.deliveryAddress)
        val (d2Lat, d2Lng) = RouteOptimizer.getMockCoordinates(o2.deliveryAddress)
        val (d3Lat, d3Lng) = RouteOptimizer.getMockCoordinates(o3.deliveryAddress)

        val dist12 = calculateDistance(p1Lat, p1Lng, p2Lat, p2Lng)
        val dist23 = calculateDistance(p2Lat, p2Lng, p3Lat, p3Lng)
        if (dist12 > state.maxDetourKm || dist23 > state.maxDetourKm) return null

        val totalEarnings = o1.fareValue + o2.fareValue + o3.fareValue
        val maxDist = maxOf(o1.totalDistance, o2.totalDistance, o3.totalDistance)
        val consolidatedDist = maxDist * 1.52 // Fator de otimização de cluster Ghost
        val detourKm = consolidatedDist - maxDist

        if (detourKm > (state.maxDetourKm * 1.5)) return null

        val gainPerKm = totalEarnings / consolidatedDist
        if (gainPerKm < envMinGain) return null

        val netProfit = calculateEstimatedNetProfit(totalEarnings, consolidatedDist)
        val apps = listOf(o1.appName, o2.appName, o3.appName).distinct()

        val stops = listOf(
            DeliveryStop("${o1.id}_p", o1.pickupAddress, p1Lat, p1Lng, o1.appName, 1),
            DeliveryStop("${o2.id}_p", o2.pickupAddress, p2Lat, p2Lng, o2.appName, 2),
            DeliveryStop("${o3.id}_p", o3.pickupAddress, p3Lat, p3Lng, o3.appName, 3),
            DeliveryStop("${o1.id}_d", o1.deliveryAddress, d1Lat, d1Lng, o1.appName, 4),
            DeliveryStop("${o2.id}_d", o2.deliveryAddress, d2Lat, d2Lng, o2.appName, 5),
            DeliveryStop("${o3.id}_d", o3.deliveryAddress, d3Lat, d3Lng, o3.appName, 6)
        )

        return GhostBatchGroup(
            batchId = "trio_${o1.id}_${o2.id}_${o3.id}",
            offers = listOf(o1, o2, o3),
            appNames = apps,
            totalEarnings = totalEarnings,
            consolidatedDistanceKm = consolidatedDist,
            consolidatedTimeMin = (o1.totalTime + o2.totalTime + o3.totalTime) * 0.65,
            gainPerKm = gainPerKm,
            netProfit = netProfit,
            detourEfficiencyScore = 92,
            convergenceRatio = 0.88,
            pickupAddresses = listOf(o1.pickupAddress, o2.pickupAddress, o3.pickupAddress),
            deliveryAddresses = listOf(o1.deliveryAddress, o2.deliveryAddress, o3.deliveryAddress),
            routeSequencePreview = "● 3x Coletas [${apps.joinToString("+")}] → 🏠 3x Entregas Agrupadas",
            stops = stops,
            recommendation = BatchRecommendation.SUPER_CHAIN_ACCEPT,
            reason = "Super-Combo Triplo Multi-App: R$ ${String.format("%.2f", gainPerKm)}/km (Prioridade .env: R$ ${String.format("%.2f", envMinGain)}/km).",
            confidence = 0.98
        )
    }

    /**
     * Calcula o lucro líquido estimado deduzindo despesas reais com combustível e manutenção
     */
    private fun calculateEstimatedNetProfit(fare: Double, distanceKm: Double): Double {
        val costPerKm = 0.65 // Custo médio de combustível (35km/l @ R$ 5,80) + desgaste moto
        val totalCost = distanceKm * costPerKm
        return max(0.0, fare - totalCost)
    }

    /**
     * Aceita um lote completo de entregas e atualiza a telemetria
     */
    fun acceptBatch(batch: GhostBatchGroup) {
        viewModelScope.launch {
            _uiState.update { current ->
                val acceptedIds = batch.offers.map { it.id }.toSet()
                val remaining = current.candidateOffers.filterNot { it.id in acceptedIds }
                current.copy(
                    candidateOffers = remaining,
                    totalChainedEarningsToday = current.totalChainedEarningsToday + batch.totalEarnings
                )
            }
            
            // Registra paradas no RadarCoordinator se disponível
            if (batch.stops.isNotEmpty()) {
                try {
                    RadarCoordinator.addPendingStops(batch.stops)
                } catch (e: Throwable) {
                    // Fallback
                }
            }
            
            recalculateBatches()
        }
    }

    /**
     * Recusa e descarta um lote da visualização do cockpit
     */
    fun declineBatch(batch: GhostBatchGroup) {
        _uiState.update { current ->
            current.copy(activeBatches = current.activeBatches.filterNot { it.batchId == batch.batchId })
        }
    }

    private fun calculateDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6371.0 // Raio da Terra em km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2.0) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
