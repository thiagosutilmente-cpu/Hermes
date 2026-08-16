package com.example.data

import android.content.Context
import android.util.Log
import com.example.coordinator.ActiveOffer
import com.example.coordinator.DeliveryStop
import com.example.coordinator.RadarCoordinator
import com.example.coordinator.RadarSettings
import com.example.service.gemini.GeminiOfferEvaluation
import com.example.service.gemini.GeminiOfferService
import com.example.util.GhostBatchCandidate
import com.example.util.GhostRouteOptimizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Resultado do processamento de ofertas pelo GeminiOfferRepository
 */
data class ProcessedOfferResult(
    val acceptedOffers: List<ActiveOffer>,
    val filteredOutOffers: List<ActiveOffer>,
    val evaluation: GeminiOfferEvaluation?,
    val recommendedOffer: ActiveOffer?,
    val filterSummary: String,
    val ghostBatch: GhostSequenceBatchResult? = null
)

/**
 * Repository responsável por:
 * 1. Filtrar ofertas com base nos limites operacionais do piloto (tarifa mínima, R$/km, distância).
 * 2. Executar o 'Ghost Sequence Batching' para agrupar ofertas geograficamente compatíveis
 *    em uma única rota otimizada, reduzindo o tempo de espera nas cozinhas e paradas intermediárias.
 * 3. Integrar com a IA Gemini para validar a viabilidade neural do lote e priorizar paradas.
 */
class GeminiOfferRepository(
    private val offerDao: OfferDao? = null,
    private val geminiService: GeminiOfferService = GeminiOfferService
) {
    companion object {
        private const val TAG = "GeminiOfferRepository"

        @Volatile
        private var INSTANCE: GeminiOfferRepository? = null

        fun getInstance(context: Context? = null, offerDao: OfferDao? = null): GeminiOfferRepository {
            return INSTANCE ?: synchronized(this) {
                val db = if (context != null && offerDao == null) {
                    try {
                        AppDatabase.getDatabase(context).offerDao()
                    } catch (e: Throwable) {
                        null
                    }
                } else {
                    offerDao
                }
                val instance = GeminiOfferRepository(db)
                INSTANCE = instance
                instance
            }
        }
    }

    private val _lastProcessedResult = MutableStateFlow<ProcessedOfferResult?>(null)
    val lastProcessedResult: StateFlow<ProcessedOfferResult?> = _lastProcessedResult.asStateFlow()

    private val _activeGhostBatch = MutableStateFlow<GhostSequenceBatchResult?>(null)
    val activeGhostBatch: StateFlow<GhostSequenceBatchResult?> = _activeGhostBatch.asStateFlow()

    /**
     * Filtra as ofertas recebidas, executa a formação de lotes inteligentes via Ghost Sequence Batching
     * e encaminha as ofertas/lotes para a recomendação do Gemini.
     */
    suspend fun processAndFilterOffers(
        incomingOffers: List<ActiveOffer>,
        settings: RadarSettings = RadarCoordinator.settings.value,
        context: Context? = null
    ): ProcessedOfferResult = withContext(Dispatchers.IO) {
        if (incomingOffers.isEmpty()) {
            val emptyResult = ProcessedOfferResult(
                acceptedOffers = emptyList(),
                filteredOutOffers = emptyList(),
                evaluation = null,
                recommendedOffer = null,
                filterSummary = "Nenhuma oferta recebida para processamento.",
                ghostBatch = null
            )
            _lastProcessedResult.value = emptyResult
            _activeGhostBatch.value = null
            return@withContext emptyResult
        }

        val minFare = settings.minFareValue
        val minValuePerKm = settings.minValuePerKm
        val maxTotalDistance = if (settings.maxTotalDistanceKm > 0) settings.maxTotalDistanceKm else 15.0

        val qualifiedOffers = mutableListOf<ActiveOffer>()
        val filteredOutOffers = mutableListOf<ActiveOffer>()

        // 1. Filtragem rigorosa baseada nos parâmetros do piloto
        for (offer in incomingOffers) {
            val distance = if (offer.totalDistance > 0) offer.totalDistance else 0.1
            val gainPerKm = offer.fareValue / distance
            val meetsMinFare = offer.fareValue >= minFare
            val meetsMinGainPerKm = gainPerKm >= minValuePerKm
            val withinMaxDistance = offer.totalDistance <= maxTotalDistance

            if (meetsMinFare && meetsMinGainPerKm && withinMaxDistance) {
                qualifiedOffers.add(offer)
            } else {
                filteredOutOffers.add(offer)
                Log.d(TAG, "Oferta descartada: ${offer.appName} R$ ${offer.fareValue} (${distance}km, R$ ${String.format(Locale.US, "%.2f", gainPerKm)}/km). MinFare=$minFare, MinPerKm=$minValuePerKm")
            }
        }

        val filterSummary = "Recebidas: ${incomingOffers.size} | Qualificadas: ${qualifiedOffers.size} | Descartadas no Piso: ${filteredOutOffers.size}"
        Log.i(TAG, filterSummary)

        // 2. Se nenhuma oferta individual atendeu aos requisitos mínimos
        if (qualifiedOffers.isEmpty()) {
            val noPassEvaluation = GeminiOfferEvaluation(
                selectedOfferId = null,
                selectedApp = null,
                decision = "DECLINE",
                confidence = 0.99,
                gainPerKm = 0.0,
                estimatedHourlyRate = 0.0,
                reason = "Todas as ${incomingOffers.size} ofertas recebidas estão abaixo do piso configurado (Tarifa mínima: R$ ${String.format(Locale.US, "%.2f", minFare)}, Piso: R$ ${String.format(Locale.US, "%.2f", minValuePerKm)}/km).",
                suggestedVoiceAnnouncement = "Thiago, ofertas recebidas abaixo do seu piso mínimo de ganho. Descartadas pelo Radar.",
                optimalStopSequence = emptyList(),
                riskFactors = listOf("LOW_FARE_OR_GAIN_PER_KM")
            )
            RadarCoordinator.updateGeminiEvaluation(noPassEvaluation)
            
            val result = ProcessedOfferResult(
                acceptedOffers = emptyList(),
                filteredOutOffers = filteredOutOffers,
                evaluation = noPassEvaluation,
                recommendedOffer = null,
                filterSummary = filterSummary,
                ghostBatch = null
            )
            _lastProcessedResult.value = result
            _activeGhostBatch.value = null
            return@withContext result
        }

        // 3. Executa a lógica de 'Ghost Sequence Batching' no Repositório
        val location = RadarCoordinator.currentLocation.value
        val pilotLat = location?.latitude ?: -23.550520
        val pilotLng = location?.longitude ?: -46.633308

        val ghostBatchResult = if (settings.isGhostSequenceEnabled && qualifiedOffers.size >= 2) {
            computeGhostSequenceBatch(
                pilotLat = pilotLat,
                pilotLng = pilotLng,
                offers = qualifiedOffers,
                settings = settings
            )
        } else {
            null
        }

        _activeGhostBatch.value = ghostBatchResult

        // 4. Se um lote otimizado (Ghost Batch) foi construído, cria a oferta combinada para a IA Gemini
        val offersForGemini = if (ghostBatchResult != null) {
            val combinedOffer = ActiveOffer(
                appName = ghostBatchResult.appNamesFormatted,
                fareValue = ghostBatchResult.totalEarnings,
                pickupAddress = ghostBatchResult.stopsInOptimizedOrder.firstOrNull()?.address ?: "Múltiplas Coletas",
                deliveryAddress = ghostBatchResult.stopsInOptimizedOrder.lastOrNull()?.address ?: "Múltiplas Entregas",
                totalDistance = ghostBatchResult.totalDistanceKm,
                totalTime = ghostBatchResult.estimatedTimeMinutes.toDouble()
            )
            listOf(combinedOffer) + qualifiedOffers
        } else {
            qualifiedOffers
        }

        // 5. Envia as ofertas qualificadas e/ou lote para análise neural no Gemini
        val geminiEvaluation = try {
            geminiService.evaluateOffersWithGemini(offersForGemini, context)
        } catch (e: Throwable) {
            Log.e(TAG, "Erro ao consultar GeminiOfferService: ${e.message}")
            null
        }

        // 6. Identifica a oferta recomendada pela IA
        val recommended = if (geminiEvaluation != null) {
            RadarCoordinator.updateGeminiEvaluation(geminiEvaluation)
            
            offersForGemini.find { it.appName.equals(geminiEvaluation.selectedApp, ignoreCase = true) }
                ?: offersForGemini.maxByOrNull { if (it.totalDistance > 0) it.fareValue / it.totalDistance else 0.0 }
        } else {
            offersForGemini.maxByOrNull { if (it.totalDistance > 0) it.fareValue / it.totalDistance else 0.0 }
        }

        // 7. Persiste a decisão no Room Database (se disponível)
        if (offerDao != null && recommended != null) {
            try {
                val isAccept = geminiEvaluation?.decision == "ACCEPT" || geminiEvaluation?.decision == "CHAIN"
                val entity = OfferEntity(
                    appName = recommended.appName,
                    fareValue = recommended.fareValue,
                    pickupAddress = recommended.pickupAddress,
                    deliveryAddress = recommended.deliveryAddress,
                    totalDistance = recommended.totalDistance,
                    totalTime = recommended.totalTime,
                    suggestion = if (isAccept) "aceitar" else "recusar",
                    reason = geminiEvaluation?.reason ?: "Avaliação Gemini",
                    timestamp = System.currentTimeMillis()
                )
                offerDao.insertOffer(entity)
            } catch (e: Throwable) {
                Log.w(TAG, "Não foi possível persistir oferta no Room: ${e.message}")
            }
        }

        val finalResult = ProcessedOfferResult(
            acceptedOffers = qualifiedOffers,
            filteredOutOffers = filteredOutOffers,
            evaluation = geminiEvaluation,
            recommendedOffer = recommended,
            filterSummary = filterSummary,
            ghostBatch = ghostBatchResult
        )

        _lastProcessedResult.value = finalResult
        return@withContext finalResult
    }

    /**
     * Motor de 'Ghost Sequence Batching':
     * Agrupa ofertas em um único itinerário topo-otimizado, reduzindo o tempo de espera nas coletas,
     * calculando economia de percurso e alinhamento de corredores viários.
     */
    fun computeGhostSequenceBatch(
        pilotLat: Double,
        pilotLng: Double,
        offers: List<ActiveOffer>,
        settings: RadarSettings
    ): GhostSequenceBatchResult? {
        try {
            // Converte ActiveOffer para OfferEntity para passar pelo motor de otimização
            val offerEntities = offers.mapIndexed { idx, offer ->
                OfferEntity(
                    id = idx + 1,
                    appName = offer.appName,
                    fareValue = offer.fareValue,
                    pickupAddress = offer.pickupAddress,
                    deliveryAddress = offer.deliveryAddress,
                    totalDistance = offer.totalDistance,
                    totalTime = offer.totalTime,
                    suggestion = "analisar",
                    reason = "Batch Candidate",
                    timestamp = System.currentTimeMillis()
                )
            }

            // Executa o algoritmo preditivo de agrupamento e ordenação
            val batchCandidates = GhostRouteOptimizer.filterAndBatchMultiAppOffers(
                currentLat = pilotLat,
                currentLng = pilotLng,
                offers = offerEntities,
                minGainPerKm = settings.minValuePerKm,
                maxProximityKm = 3.8,
                trafficFactor = 0.5f,
                aggressiveness = settings.ghostSequenceAggressiveness,
                trafficWeight = settings.ghostSequenceTrafficWeight,
                latencyWeight = settings.ghostSequenceLatencyWeight,
                chainDeliveriesMode = settings.chainDeliveriesMode
            )

            // Pega o melhor candidato a lote múltiplo (isMultiApp ou com mais de 1 oferta)
            val bestCandidate = batchCandidates.firstOrNull { it.offers.size >= 2 }
                ?: batchCandidates.firstOrNull()

            if (bestCandidate != null && bestCandidate.offers.size >= 2) {
                // Mapeia de volta as ofertas originais contidas no lote
                val combinedOffers = bestCandidate.offers.map { entity ->
                    offers.find { it.appName.equals(entity.appName, ignoreCase = true) && it.fareValue == entity.fareValue }
                        ?: ActiveOffer(
                            appName = entity.appName,
                            fareValue = entity.fareValue,
                            pickupAddress = entity.pickupAddress,
                            deliveryAddress = entity.deliveryAddress,
                            totalDistance = entity.totalDistance,
                            totalTime = entity.totalTime
                        )
                }

                // Calcula o tempo de espera economizado com as coletas coordenadas em cadeia
                val totalWaitTime = bestCandidate.stops.filter { it.priority <= 2 }.sumOf { 
                    GhostRouteOptimizer.getAverageWaitTimeMinutes(it.address) 
                }
                val savedWaitTime = (totalWaitTime * 0.40).toInt().coerceAtLeast(4)

                // Reordena as paradas de entrega usando o motor Ghost Route Optimizer
                val optimizedStops = GhostRouteOptimizer.optimize(
                    currentLat = pilotLat,
                    currentLng = pilotLng,
                    stops = bestCandidate.stops,
                    trafficFactor = 0.5f,
                    aggressiveness = settings.ghostSequenceAggressiveness,
                    trafficWeight = settings.ghostSequenceTrafficWeight,
                    latencyWeight = settings.ghostSequenceLatencyWeight
                )

                Log.d(TAG, "Ghost Sequence Batch gerado: ${bestCandidate.appNames} | R$ ${bestCandidate.totalValue} | ${bestCandidate.totalDistanceKm}km | Espera economizada: ${savedWaitTime}min")

                return GhostSequenceBatchResult(
                    batchId = bestCandidate.id,
                    combinedOffers = combinedOffers,
                    appNamesFormatted = "👻 GHOST STACK: ${bestCandidate.appNames}",
                    totalEarnings = bestCandidate.totalValue,
                    totalDistanceKm = bestCandidate.totalDistanceKm,
                    gainPerKm = bestCandidate.gainPerKm,
                    estimatedTimeMinutes = bestCandidate.estimatedTimeMin,
                    waitTimeSavedMinutes = savedWaitTime,
                    stopsInOptimizedOrder = optimizedStops,
                    isMultiApp = bestCandidate.isMultiApp,
                    synergyConfidencePct = bestCandidate.matchConfidence,
                    candidate = bestCandidate,
                    evaluation = null
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao processar Ghost Sequence Batching: ${e.message}")
        }
        return null
    }
}
