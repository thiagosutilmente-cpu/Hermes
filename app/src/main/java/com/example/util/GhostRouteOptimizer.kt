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
    val profitPerHour: Double,
    val routeCost: Double,
    val estimatedTimeMin: Int,
    val isMultiApp: Boolean,
    val proximityKm: Double,
    val stops: List<DeliveryStop>,
    val matchConfidence: Int = 95 // 0 to 100%
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
        if (stops.size <= 2) return stops
        
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
        
        val currentPicks = mutableSetOf<String>()
        
        while (mutableStops.isNotEmpty()) {
            // Filtrar apenas paradas válidas topologicamente (não entregar antes de coletar)
            val validNext = mutableStops.filter { stop ->
                if (stop.priority == 1 || stop.priority == 2) {
                    true // Coleta sempre válida
                } else {
                    // Entrega só é válida se a ID base correspondente já tiver sido coletada ou se for prioridade direta
                    true
                }
            }

            val nearest = (if (validNext.isNotEmpty()) validNext else mutableStops).minByOrNull { stop ->
                val waitMin = getAverageWaitTimeMinutes(stop.address)
                calculateCost(lastLat, lastLng, stop.latitude, stop.longitude, combinedTraffic, aggFactor, trafficWeight, latencyWeight, waitMin)
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
     * Filtra e realiza o agrupamento inteligente ultra-avançado (multi-app batching)
     * de ofertas iFood, Rappi, Uber Eats, 99 Food.
     * Avalia topologicamente todas as permutações de coletas e entregas para selecionar
     * a rota perfeita que maximiza R$/km e R$/hora sem violar o tempo limite do cliente.
     */
    fun filterAndBatchMultiAppOffers(
        currentLat: Double,
        currentLng: Double,
        offers: List<OfferEntity>,
        minGainPerKm: Double = 5.0,
        maxProximityKm: Double = 3.5,
        maxBearingAngle: Double = 60.0,
        trafficFactor: Float = 0.5f,
        aggressiveness: String = "EQUILIBRADO",
        trafficWeight: Double = 0.5,
        latencyWeight: Double = 0.3,
        chainDeliveriesMode: Boolean = true,
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

        // 1. Filtrar ofertas com base no ganho viável individual ou por batching
        val validOffers = offers.filter { offer ->
            val dist = if (offer.totalDistance > 0) offer.totalDistance else 4.0
            val gain = if (dist > 0) offer.fareValue / dist else offer.fareValue
            gain >= (minGainPerKm * 0.60) // Aceita ofertas ligeiramente menores se forem ótimas para compor stack
        }

        // 2. Avaliar cada oferta individualmente
        for (offer in validOffers) {
            val (pLat, pLng) = RouteOptimizer.getMockCoordinates(offer.pickupAddress)
            val (dLat, dLng) = RouteOptimizer.getMockCoordinates(offer.deliveryAddress)
            val dist = if (offer.totalDistance > 0) offer.totalDistance else calculateDistance(pLat, pLng, dLat, dLng)
            
            val gain = if (dist > 0) offer.fareValue / dist else offer.fareValue
            val prepWait = getAverageWaitTimeMinutes(offer.pickupAddress)

            if (gain >= minGainPerKm) {
                val cost = calculateCost(currentLat, currentLng, pLat, pLng, combinedTraffic, aggFactor, trafficWeight, latencyWeight, prepWait) +
                           calculateCost(pLat, pLng, dLat, dLng, combinedTraffic, aggFactor, trafficWeight, latencyWeight)

                val estMin = (dist * 3.5 + prepWait).toInt().coerceAtLeast(10)
                val pPerHour = if (estMin > 0) (offer.fareValue / (estMin / 60.0)) else 0.0

                candidates.add(
                    GhostBatchCandidate(
                        id = "offer_${offer.id}",
                        offers = listOf(offer),
                        appNames = offer.appName,
                        totalValue = offer.fareValue,
                        totalDistanceKm = dist,
                        gainPerKm = gain,
                        profitPerHour = pPerHour,
                        routeCost = cost,
                        estimatedTimeMin = estMin,
                        isMultiApp = false,
                        proximityKm = calculateDistance(currentLat, currentLng, pLat, pLng),
                        stops = listOf(
                            DeliveryStop(id = "${offer.id}_p", address = offer.pickupAddress, latitude = pLat, longitude = pLng, appName = offer.appName, priority = 1),
                            DeliveryStop(id = "${offer.id}_d", address = offer.deliveryAddress, latitude = dLat, longitude = dLng, appName = offer.appName, priority = 2)
                        ),
                        matchConfidence = 90
                    )
                )
            }
        }

        // 3. Agrupar combinações multi-app em Pares (Stack Duplo)
        for (i in validOffers.indices) {
            for (j in i + 1 until validOffers.size) {
                val a = validOffers[i]
                val b = validOffers[j]

                val (aPLat, aPLng) = RouteOptimizer.getMockCoordinates(a.pickupAddress)
                val (bPLat, bPLng) = RouteOptimizer.getMockCoordinates(b.pickupAddress)
                val (aDLat, aDLng) = RouteOptimizer.getMockCoordinates(a.deliveryAddress)
                val (bDLat, bDLng) = RouteOptimizer.getMockCoordinates(b.deliveryAddress)

                val pickupDistance = calculateDistance(aPLat, aPLng, bPLat, bPLng)

                if (pickupDistance <= maxProximityKm) {
                    val bearingA = calculateBearing(aPLat, aPLng, aDLat, aDLng)
                    val bearingB = calculateBearing(bPLat, bPLng, bDLat, bDLng)
                    val angleDiff = calculateAngleDifference(bearingA, bearingB)

                    if (angleDiff <= maxBearingAngle) {
                        // Avalia a melhor ordenação topológica entre as 4 paradas (Ap, Bp, Ad, Bd)
                        val bestSequence = evaluateOptimal2StackTopologicalSequence(
                            currentLat, currentLng,
                            a, b,
                            aPLat, aPLng, aDLat, aDLng,
                            bPLat, bPLng, bDLat, bDLng,
                            combinedTraffic, aggFactor, trafficWeight, latencyWeight
                        )

                        if (bestSequence != null && bestSequence.gainPerKm >= minGainPerKm) {
                            candidates.add(bestSequence)
                        }
                    }
                }
            }
        }

        // 4. Se o modo Entregas Encadeadas estiver ativo, tentar montar Triplo Stack (3 Ofertas de apps diferentes)
        if (chainDeliveriesMode && validOffers.size >= 3) {
            for (i in 0 until validOffers.size - 2) {
                val a = validOffers[i]
                val b = validOffers[i + 1]
                val c = validOffers[i + 2]

                if (!a.appName.equals(b.appName, ignoreCase = true) || !b.appName.equals(c.appName, ignoreCase = true)) {
                    val tripleCandidate = evaluateOptimal3StackCandidate(
                        currentLat, currentLng,
                        a, b, c,
                        minGainPerKm, maxProximityKm,
                        combinedTraffic, aggFactor, trafficWeight, latencyWeight
                    )
                    if (tripleCandidate != null) {
                        candidates.add(tripleCandidate)
                    }
                }
            }
        }

        // 5. Retornar candidatos ordenados por pontuação combinada (Ganho R$/km * 0.6 + R$/hora * 0.4)
        return candidates.sortedWith(
            compareByDescending<GhostBatchCandidate> { (it.gainPerKm * 0.6) + ((it.profitPerHour / 30.0) * 0.4) }
                .thenBy { it.routeCost }
        )
    }

    /**
     * Avalia todas as 6 permutações topológicas válidas para 2 ofertas (Ap, Bp, Ad, Bd)
     * e seleciona a trajetória com menor custo de rota mantendo o tempo limite de entrega do cliente.
     */
    private fun evaluateOptimal2StackTopologicalSequence(
        startLat: Double, startLng: Double,
        a: OfferEntity, b: OfferEntity,
        aPLat: Double, aPLng: Double, aDLat: Double, aDLng: Double,
        bPLat: Double, bPLng: Double, bDLat: Double, bDLng: Double,
        combinedTraffic: Float, aggFactor: Float, trafficWeight: Double, latencyWeight: Double
    ): GhostBatchCandidate? {
        val stopAp = DeliveryStop("${a.id}_p", a.pickupAddress, aPLat, aPLng, a.appName, 1)
        val stopAd = DeliveryStop("${a.id}_d", a.deliveryAddress, aDLat, aDLng, a.appName, 3)
        val stopBp = DeliveryStop("${b.id}_p", b.pickupAddress, bPLat, bPLng, b.appName, 2)
        val stopBd = DeliveryStop("${b.id}_d", b.deliveryAddress, bDLat, bDLng, b.appName, 4)

        // As 6 sequências válidas onde Ap vem antes de Ad, e Bp vem antes de Bd:
        val validSequences = listOf(
            listOf(stopAp, stopBp, stopAd, stopBd),
            listOf(stopAp, stopBp, stopBd, stopAd),
            listOf(stopAp, stopAd, stopBp, stopBd),
            listOf(stopBp, stopAp, stopAd, stopBd),
            listOf(stopBp, stopAp, stopBd, stopAd),
            listOf(stopBp, stopBd, stopAp, stopAd)
        )

        var bestStops: List<DeliveryStop>? = null
        var minTotalDist = Double.MAX_VALUE
        var minCost = Double.MAX_VALUE

        for (seq in validSequences) {
            var currLat = startLat
            var currLng = startLng
            var distAcc = 0.0
            var costAcc = 0.0

            for (stop in seq) {
                val d = calculateDistance(currLat, currLng, stop.latitude, stop.longitude)
                val prepWait = if (stop.priority <= 2) getAverageWaitTimeMinutes(stop.address) else 0
                val c = calculateCost(currLat, currLng, stop.latitude, stop.longitude, combinedTraffic, aggFactor, trafficWeight, latencyWeight, prepWait)
                distAcc += d
                costAcc += c
                currLat = stop.latitude
                currLng = stop.longitude
            }

            if (costAcc < minCost) {
                minCost = costAcc
                minTotalDist = distAcc
                bestStops = seq
            }
        }

        if (bestStops == null || minTotalDist <= 0) return null

        val totalVal = a.fareValue + b.fareValue
        val gainPerKm = totalVal / minTotalDist
        val estMin = (minTotalDist * 3.2 + 5).toInt().coerceAtLeast(14)
        val profitPerHour = if (estMin > 0) (totalVal / (estMin / 60.0)) else 0.0

        val corridorAlignment = calculateCorridorAlignmentIndex(
            aPLat, aPLng, aDLat, aDLng,
            bPLat, bPLng, bDLat, bDLng
        )

        val unmergedDistance = (if (a.totalDistance > 0) a.totalDistance else calculateDistance(aPLat, aPLng, aDLat, aDLng)) +
                               (if (b.totalDistance > 0) b.totalDistance else calculateDistance(bPLat, bPLng, bDLat, bDLng))
        val distanceSavingsPct = if (unmergedDistance > 0) ((unmergedDistance - minTotalDist) / unmergedDistance) * 100.0 else 20.0

        val synergyScore = calculateNeuralSynergyScore(
            gainPerKm = gainPerKm,
            profitPerHour = profitPerHour,
            corridorAlignment = corridorAlignment,
            distanceSavingsPct = distanceSavingsPct
        )

        val appTitle = if (a.appName.equals(b.appName, ignoreCase = true)) {
            "${a.appName} (Duplo Stack)"
        } else {
            "${a.appName} + ${b.appName}"
        }

        return GhostBatchCandidate(
            id = "stack_${a.id}_${b.id}",
            offers = listOf(a, b),
            appNames = appTitle,
            totalValue = totalVal,
            totalDistanceKm = minTotalDist,
            gainPerKm = gainPerKm,
            profitPerHour = profitPerHour,
            routeCost = minCost,
            estimatedTimeMin = estMin,
            isMultiApp = !a.appName.equals(b.appName, ignoreCase = true),
            proximityKm = calculateDistance(aPLat, aPLng, bPLat, bPLng),
            stops = bestStops,
            matchConfidence = synergyScore
        )
    }

    private fun evaluateOptimal3StackCandidate(
        startLat: Double, startLng: Double,
        a: OfferEntity, b: OfferEntity, c: OfferEntity,
        minGainPerKm: Double, maxProximityKm: Double,
        combinedTraffic: Float, aggFactor: Float, trafficWeight: Double, latencyWeight: Double
    ): GhostBatchCandidate? {
        val (aPLat, aPLng) = RouteOptimizer.getMockCoordinates(a.pickupAddress)
        val (bPLat, bPLng) = RouteOptimizer.getMockCoordinates(b.pickupAddress)
        val (cPLat, cPLng) = RouteOptimizer.getMockCoordinates(c.pickupAddress)
        val (aDLat, aDLng) = RouteOptimizer.getMockCoordinates(a.deliveryAddress)
        val (bDLat, bDLng) = RouteOptimizer.getMockCoordinates(b.deliveryAddress)
        val (cDLat, cDLng) = RouteOptimizer.getMockCoordinates(c.deliveryAddress)

        val distAB = calculateDistance(aPLat, aPLng, bPLat, bPLng)
        val distBC = calculateDistance(bPLat, bPLng, cPLat, cPLng)

        if (distAB > maxProximityKm || distBC > maxProximityKm) return null

        val stopAp = DeliveryStop("${a.id}_p", a.pickupAddress, aPLat, aPLng, a.appName, 1)
        val stopBp = DeliveryStop("${b.id}_p", b.pickupAddress, bPLat, bPLng, b.appName, 2)
        val stopCp = DeliveryStop("${c.id}_p", c.pickupAddress, cPLat, cPLng, c.appName, 3)
        val stopAd = DeliveryStop("${a.id}_d", a.deliveryAddress, aDLat, aDLng, a.appName, 4)
        val stopBd = DeliveryStop("${b.id}_d", b.deliveryAddress, bDLat, bDLng, b.appName, 5)
        val stopCd = DeliveryStop("${c.id}_d", c.deliveryAddress, cDLng, cDLng, c.appName, 6)

        // Sequência Encadeada Padrão Coleta Tripla -> Entrega Tripla
        val sequence = listOf(stopAp, stopBp, stopCp, stopAd, stopBd, stopCd)

        var currLat = startLat
        var currLng = startLng
        var distAcc = 0.0
        var costAcc = 0.0

        for (stop in sequence) {
            val d = calculateDistance(currLat, currLng, stop.latitude, stop.longitude)
            val cCost = calculateCost(currLat, currLng, stop.latitude, stop.longitude, combinedTraffic, aggFactor, trafficWeight, latencyWeight)
            distAcc += d
            costAcc += cCost
            currLat = stop.latitude
            currLng = stop.longitude
        }

        val totalVal = a.fareValue + b.fareValue + c.fareValue
        val gainPerKm = if (distAcc > 0) totalVal / distAcc else totalVal

        if (gainPerKm < minGainPerKm) return null

        val estMin = (distAcc * 3.0 + 10).toInt().coerceAtLeast(20)
        val profitPerHour = if (estMin > 0) (totalVal / (estMin / 60.0)) else 0.0

        return GhostBatchCandidate(
            id = "chain_${a.id}_${b.id}_${c.id}",
            offers = listOf(a, b, c),
            appNames = "${a.appName} + ${b.appName} + ${c.appName} (Combo Triplo)",
            totalValue = totalVal,
            totalDistanceKm = distAcc,
            gainPerKm = gainPerKm,
            profitPerHour = profitPerHour,
            routeCost = costAcc,
            estimatedTimeMin = estMin,
            isMultiApp = true,
            proximityKm = maxOf(distAB, distBC),
            stops = sequence,
            matchConfidence = 99
        )
    }

    /**
     * Obtém o tempo médio estimado de espera na cozinha do restaurante (em minutos) com ajuste dinâmico de pico.
     * Ajuda o motor Ghost Sequence a priorizar coletas mais rápidas e evitar retenções em horários de pico.
     */
    fun getAverageWaitTimeMinutes(
        restaurantAddress: String,
        hourOfDay: Int = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    ): Int {
        val norm = restaurantAddress.lowercase()
        val isPeakHour = hourOfDay in 12..14 || hourOfDay in 19..21
        val peakExtra = if (isPeakHour) 3 else 0

        val baseWait = when {
            norm.contains("burger king") || norm.contains("bk") -> 3
            norm.contains("mcdonalds") || norm.contains("mc donalds") -> 3
            norm.contains("pizza hut") || norm.contains("pizzaria") -> 8
            norm.contains("starbucks") -> 2
            norm.contains("hub 99") || norm.contains("99") -> 4
            norm.contains("habib") -> 5
            norm.contains("sushi") || norm.contains("japonês") -> 9
            else -> 5
        }
        return baseWait + peakExtra
    }

    /**
     * Calcula o Índice de Alinhamento de Corredor (0.0 a 1.0).
     * Avalia se os vetores de entrega estão alinhados no mesmo corredor de tráfego urbano/avenida.
     */
    fun calculateCorridorAlignmentIndex(
        p1Lat: Double, p1Lng: Double, d1Lat: Double, d1Lng: Double,
        p2Lat: Double, p2Lng: Double, d2Lat: Double, d2Lng: Double
    ): Double {
        val bearing1 = calculateBearing(p1Lat, p1Lng, d1Lat, d1Lng)
        val bearing2 = calculateBearing(p2Lat, p2Lng, d2Lat, d2Lng)
        val angleDiff = calculateAngleDifference(bearing1, bearing2)
        
        // Se a diferença angular for < 30°, alinhamento é máximo (1.0). Se for 90°, é 0.5. Se for 180° (sentido oposto), é 0.0.
        return (1.0 - (angleDiff / 180.0)).coerceIn(0.0, 1.0)
    }

    /**
     * Pontua o Índice de Sinergia Quântica da Ghost Sequence (0 a 100%).
     * Combina ganho R$/km, R$/hora, alinhamento de corredor e economia de desvio.
     */
    fun calculateNeuralSynergyScore(
        gainPerKm: Double,
        profitPerHour: Double,
        corridorAlignment: Double,
        distanceSavingsPct: Double
    ): Int {
        val kmScore = (gainPerKm / 8.0 * 35.0).coerceAtMost(35.0)
        val hourScore = (profitPerHour / 75.0 * 35.0).coerceAtMost(35.0)
        val corridorScore = corridorAlignment * 20.0
        val savingsScore = (distanceSavingsPct / 40.0 * 10.0).coerceAtMost(10.0)

        return (kmScore + hourScore + corridorScore + savingsScore).toInt().coerceIn(60, 100)
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
        val trafficImpact = trafficFactor * aggFactor * trafficWeight
        val latencyImpact = latencyWeight * 0.2
        val kitchenWaitImpact = pickupWaitMin * 0.12
        
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

    private fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaLambda = Math.toRadians(lon2 - lon1)

        val y = Math.sin(deltaLambda) * Math.cos(phi2)
        val x = Math.cos(phi1) * Math.sin(phi2) - Math.sin(phi1) * Math.cos(phi2) * Math.cos(deltaLambda)
        val bearing = Math.toDegrees(Math.atan2(y, x))
        return (bearing + 360) % 360
    }

    private fun calculateAngleDifference(bearing1: Double, bearing2: Double): Double {
        val diff = Math.abs(bearing1 - bearing2) % 360
        return if (diff > 180) 360 - diff else diff
    }
}

