package com.example.util

import android.location.Location
import com.example.coordinator.DeliveryStop
import com.example.coordinator.RadarCoordinator
import com.example.data.OfferEntity
import kotlin.math.*

data class GhostBatchCandidate(
    val id: String,
    val offers: List<OfferEntity>,
    val appNames: String,
    val totalValue: Double,
    val totalDistanceKm: Double,
    val gainPerKm: Double,
    val routeCost: Double,
    val estimatedTimeMin: Int,
    val isMultiApp: Boolean,
    val proximityKm: Double,
    val stops: List<DeliveryStop>
)

object GhostRouteOptimizer {

    /**
     * Obtém o fator de congestionamento histórico por horário do dia e dia da semana (padrões Google Maps Traffic).
     * Horários de pico (07h-09h30 e 17h-20h) aumentam a retenção de trânsito em até 2.1x.
     */
    fun getHistoricalTrafficFactor(hourOfDay: Int, dayOfWeek: Int = 2): Float {
        return when (hourOfDay) {
            in 7..9 -> 1.85f   // Pico da manhã
            in 11..13 -> 1.45f  // Pico do almoço (fluxo intenso de restaurantes)
            in 17..20 -> 2.10f  // Pico da noite (retenção urbana severa)
            in 21..23 -> 1.15f  // Noturno moderado
            in 0..5 -> 1.00f   // Madrugada vias livres
            else -> 1.25f      // Entre picos
        }
    }

    /**
     * Reordena as paradas pendentes para minimizar o tempo total de viagem.
     * Considera a distância euclidiana ajustada pelos fatores de tráfego, padrão histórico do horário e latência.
     */
    fun optimize(
        currentLat: Double, 
        currentLng: Double, 
        stops: List<DeliveryStop>, 
        trafficFactor: Float, 
        aggressiveness: String,
        trafficWeight: Double,
        latencyWeight: Double,
        hourOfDay: Int = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    ): List<DeliveryStop> {
        if (stops.isEmpty()) return emptyList()
        
        val mutableStops = stops.toMutableList()
        val optimized = mutableListOf<DeliveryStop>()
        
        var lastLat = currentLat
        var lastLng = currentLng
        
        val historicalFactor = getHistoricalTrafficFactor(hourOfDay)
        val combinedTraffic = (trafficFactor * historicalFactor).coerceAtMost(3.0f)
        
        val aggFactor = when(aggressiveness.uppercase()) {
            "CONSERVADOR" -> 0.3f
            "AGRESSIVO" -> 0.8f
            else -> 0.5f // EQUILIBRADO
        }
        
        while (mutableStops.isNotEmpty()) {
            val nearest = mutableStops.minByOrNull { stop ->
                calculateCost(lastLat, lastLng, stop.latitude, stop.longitude, combinedTraffic, aggFactor, trafficWeight, latencyWeight)
            }
            
            nearest?.let {
                optimized.add(it)
                mutableStops.remove(it)
                lastLat = it.latitude
                lastLng = it.longitude
            }
        }
        
        return optimized
    }

    /**
     * Filtra e realiza o agrupamento inteligente (batching) de ofertas multi-app
     * baseado nos critérios de ganho financeiro (R$/km) e proximidade espacial.
     * Fformula de custo da rota: distância * (1.0 + (fator_tráfego * fator_agressividade * trafficWeight) + (latencyWeight * 0.2))
     */
    fun filterAndBatchMultiAppOffers(
        currentLat: Double,
        currentLng: Double,
        offers: List<OfferEntity>,
        minGainPerKm: Double = 5.0,
        maxProximityKm: Double = 3.5,
        trafficFactor: Float = 0.5f,
        aggressiveness: String = "EQUILIBRADO",
        trafficWeight: Double = 0.5,
        latencyWeight: Double = 0.3,
        hourOfDay: Int = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    ): List<GhostBatchCandidate> {
        if (offers.isEmpty()) return emptyList()

        val candidates = mutableListOf<GhostBatchCandidate>()
        val historicalFactor = getHistoricalTrafficFactor(hourOfDay)
        val combinedTraffic = (trafficFactor * historicalFactor).coerceAtMost(3.0f)

        val aggFactor = when (aggressiveness.uppercase()) {
            "CONSERVADOR" -> 0.3f
            "AGRESSIVO" -> 0.8f
            else -> 0.5f
        }

        // 1. Filtrar ofertas com base em ganho mínimo por km individual ou viabilidade de batching
        val validOffers = offers.filter { offer ->
            val dist = if (offer.totalDistance > 0) offer.totalDistance else 4.0
            val gain = if (dist > 0) offer.fareValue / dist else offer.fareValue
            gain >= (minGainPerKm * 0.65)
        }

        // 2. Avaliar cada oferta individualmente
        for (offer in validOffers) {
            val (pLat, pLng) = RouteOptimizer.getMockCoordinates(offer.pickupAddress)
            val (dLat, dLng) = RouteOptimizer.getMockCoordinates(offer.deliveryAddress)
            val dist = if (offer.totalDistance > 0) offer.totalDistance else calculateDistance(pLat, pLng, dLat, dLng)
            
            // Re-calcular ganho efetivo por km ajustado pelo fator de retenção do trânsito histórico
            val effectiveDist = dist * (1.0 + (combinedTraffic * 0.25 * trafficWeight))
            val gain = if (dist > 0) offer.fareValue / dist else offer.fareValue
            val effectiveGain = if (effectiveDist > 0) offer.fareValue / effectiveDist else gain

            if (gain >= minGainPerKm) {
                val cost = calculateCost(currentLat, currentLng, pLat, pLng, combinedTraffic, aggFactor, trafficWeight, latencyWeight) +
                           calculateCost(pLat, pLng, dLat, dLng, combinedTraffic, aggFactor, trafficWeight, latencyWeight)

                candidates.add(
                    GhostBatchCandidate(
                        id = "offer_${offer.id}",
                        offers = listOf(offer),
                        appNames = offer.appName,
                        totalValue = offer.fareValue,
                        totalDistanceKm = dist,
                        gainPerKm = gain,
                        routeCost = cost,
                        estimatedTimeMin = (dist * 3.5).toInt().coerceAtLeast(10),
                        isMultiApp = false,
                        proximityKm = calculateDistance(currentLat, currentLng, pLat, pLng),
                        stops = listOf(
                            DeliveryStop(id = "${offer.id}_p", address = offer.pickupAddress, latitude = pLat, longitude = pLng, appName = offer.appName, priority = 1),
                            DeliveryStop(id = "${offer.id}_d", address = offer.deliveryAddress, latitude = dLat, longitude = dLng, appName = offer.appName, priority = 2)
                        )
                    )
                )
            }
        }

        // 3. Agrupar combinações multi-app em par (Stack de 2 ofertas com proximidade espacial de coleta)
        for (i in validOffers.indices) {
            for (j in i + 1 until validOffers.size) {
                val a = validOffers[i]
                val b = validOffers[j]

                val (aPLat, aPLng) = RouteOptimizer.getMockCoordinates(a.pickupAddress)
                val (bPLat, bPLng) = RouteOptimizer.getMockCoordinates(b.pickupAddress)
                val (aDLat, aDLng) = RouteOptimizer.getMockCoordinates(a.deliveryAddress)
                val (bDLat, bDLng) = RouteOptimizer.getMockCoordinates(b.deliveryAddress)

                // Proximidade espacial das coletas
                val pickupDistance = calculateDistance(aPLat, aPLng, bPLat, bPLng)

                if (pickupDistance <= maxProximityKm) {
                    val leg1 = calculateDistance(currentLat, currentLng, aPLat, aPLng)
                    val leg2 = calculateDistance(aPLat, aPLng, bPLat, bPLng)
                    val leg3 = calculateDistance(bPLat, bPLng, aDLat, aDLng)
                    val leg4 = calculateDistance(aDLat, aDLng, bDLat, bDLng)

                    val totalDist = leg1 + leg2 + leg3 + leg4
                    val totalVal = a.fareValue + b.fareValue
                    val gainPerKm = if (totalDist > 0) totalVal / totalDist else totalVal

                    if (gainPerKm >= minGainPerKm) {
                        val routeCost = calculateCost(currentLat, currentLng, aPLat, aPLng, combinedTraffic, aggFactor, trafficWeight, latencyWeight) +
                                         calculateCost(aPLat, aPLng, bPLat, bPLng, combinedTraffic, aggFactor, trafficWeight, latencyWeight) +
                                         calculateCost(bPLat, bPLng, aDLat, aDLng, combinedTraffic, aggFactor, trafficWeight, latencyWeight) +
                                         calculateCost(aDLat, aDLng, bDLat, bDLng, combinedTraffic, aggFactor, trafficWeight, latencyWeight)

                        val appTitle = if (a.appName.equals(b.appName, ignoreCase = true)) {
                            "${a.appName} (Duplo Stack)"
                        } else {
                            "${a.appName} + ${b.appName}"
                        }

                        candidates.add(
                            GhostBatchCandidate(
                                id = "stack_${a.id}_${b.id}",
                                offers = listOf(a, b),
                                appNames = appTitle,
                                totalValue = totalVal,
                                totalDistanceKm = totalDist,
                                gainPerKm = gainPerKm,
                                routeCost = routeCost,
                                estimatedTimeMin = (totalDist * 3.2).toInt().coerceAtLeast(15),
                                isMultiApp = !a.appName.equals(b.appName, ignoreCase = true),
                                proximityKm = pickupDistance,
                                stops = listOf(
                                    DeliveryStop(id = "${a.id}_p", address = a.pickupAddress, latitude = aPLat, longitude = aPLng, appName = a.appName, priority = 1),
                                    DeliveryStop(id = "${b.id}_p", address = b.pickupAddress, latitude = bPLat, longitude = bPLng, appName = b.appName, priority = 2),
                                    DeliveryStop(id = "${a.id}_d", address = a.deliveryAddress, latitude = aDLat, longitude = aDLng, appName = a.appName, priority = 3),
                                    DeliveryStop(id = "${b.id}_d", address = b.deliveryAddress, latitude = bDLat, longitude = bDLng, appName = b.appName, priority = 4)
                                )
                            )
                        )
                    }
                }
            }
        }

        // 4. Retornar candidatos ordenados por ganho R$/km decrescente e custo da rota crescente
        return candidates.sortedWith(
            compareByDescending<GhostBatchCandidate> { it.gainPerKm }
                .thenBy { it.routeCost }
        )
    }

    /**
     * Obtém o tempo médio estimado de espera na cozinha do restaurante (em minutos).
     * Ajuda o motor Ghost Sequence a priorizar coletas mais rápidas e evitar retenções.
     */
    fun getAverageWaitTimeMinutes(restaurantAddress: String): Int {
        val norm = restaurantAddress.lowercase()
        return when {
            norm.contains("burger king") || norm.contains("bk") -> 3
            norm.contains("pizza hut") -> 8
            norm.contains("starbucks") -> 2
            norm.contains("hub 99") || norm.contains("99") -> 4
            else -> 5
        }
    }

    private fun calculateCost(
        lat1: Double, 
        lng1: Double, 
        lat2: Double, 
        lng2: Double, 
        trafficFactor: Float, 
        aggFactor: Float,
        trafficWeight: Double,
        latencyWeight: Double,
        pickupWaitMin: Int = 0
    ): Double {
        val distance = calculateDistance(lat1, lng1, lat2, lng2)
        // O custo aumenta proporcionalmente ao tráfego detectado ajustado pela agressividade e pesos finos
        // TrafficWeight influencia o impacto do tráfego. LatencyWeight influencia a reatividade a mudanças.
        val trafficImpact = trafficFactor * aggFactor * trafficWeight
        val latencyImpact = latencyWeight * 0.2 // Simulação de impacto de latência na decisão
        val kitchenWaitImpact = pickupWaitMin * 0.12 // Penalização proporcional ao tempo de fila no restaurante
        
        return (distance * (1.0 + trafficImpact + latencyImpact)) + kitchenWaitImpact
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
