package com.example.util

import android.location.Location
import java.util.Calendar
import java.util.Locale

data class StopPoint(
    val id: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val type: StopType,
    val orderId: String,
    var estimatedArrival: Long = 0,
    val urgencyScore: Double = 1.0, // 1.0 (Normal) a 2.0 (Muito Urgente)
    val baseValue: Double = 0.0,
    val appName: String = "iFood"
)

enum class StopType {
    PICKUP, DELIVERY
}

object RouteOptimizer {

    /**
     * Algoritmo de Otimização Multiobjetivo (M.O.A.)
     * Minimiza (Distância * Tráfego) e Maximiza (Lucro / Tempo)
     */
    fun optimizeRoute(
        currentLat: Double,
        currentLon: Double,
        stops: List<StopPoint>,
        rainMultiplier: Double = 1.0
    ): List<StopPoint> {
        if (stops.isEmpty()) return emptyList()
        
        // Para rotas pequenas, busca exaustiva (Brute Force Otimizada)
        if (stops.size > 8) return optimizeRouteGreedy(currentLat, currentLon, stops, rainMultiplier)

        val permutations = mutableListOf<List<StopPoint>>()
        generateValidPermutations(emptyList(), stops, mutableSetOf(), permutations)

        val trafficMultiplier = getTrafficMultiplier() * rainMultiplier
        val currentTime = System.currentTimeMillis()

        val bestPath = permutations.minByOrNull { path ->
            calculateMultiObjectiveCost(currentLat, currentLon, path, trafficMultiplier)
        }

        bestPath?.forEachIndexed { index, stop ->
            val prevLat = if (index == 0) currentLat else bestPath[index - 1].latitude
            val prevLon = if (index == 0) currentLon else bestPath[index - 1].longitude
            val dist = calculateDistance(prevLat, prevLon, stop.latitude, stop.longitude)
            
            val baseSpeedKmh = 28.0
            val speedMps = (baseSpeedKmh / 3.6) / trafficMultiplier
            val travelTime = (dist / speedMps).toLong() + 180L 
            
            val prevEta = if (index == 0) currentTime else bestPath[index - 1].estimatedArrival
            stop.estimatedArrival = prevEta + (travelTime * 1000)
        }

        return bestPath ?: emptyList()
    }

    private fun generateValidPermutations(
        currentPath: List<StopPoint>,
        remaining: List<StopPoint>,
        currentLoad: MutableSet<String>,
        result: MutableList<List<StopPoint>>
    ) {
        if (remaining.isEmpty()) {
            result.add(currentPath)
            return
        }

        for (stop in remaining) {
            val canProcess = when (stop.type) {
                StopType.PICKUP -> true
                StopType.DELIVERY -> currentLoad.contains(stop.orderId)
            }

            if (canProcess) {
                val nextLoad = currentLoad.toMutableSet()
                if (stop.type == StopType.PICKUP) nextLoad.add(stop.orderId) else nextLoad.remove(stop.orderId)
                
                generateValidPermutations(
                    currentPath + stop,
                    remaining.filter { it.id != stop.id },
                    nextLoad,
                    result
                )
            }
        }
    }

    private fun calculateMultiObjectiveCost(
        startLat: Double,
        startLon: Double,
        path: List<StopPoint>,
        multiplier: Double
    ): Double {
        var totalDistance = 0.0
        var totalCustomerWaitTime = 0.0
        var lastLat = startLat
        var lastLon = startLon
        var accumulatedTime = 0.0 // em segundos
        
        val pickupTimes = mutableMapOf<String, Double>()
        
        path.forEach { stop ->
            val distance = calculateDistance(lastLat, lastLon, stop.latitude, stop.longitude)
            totalDistance += distance
            
            val speedMps = (28.0 / 3.6) / multiplier
            val travelTime = distance / speedMps
            accumulatedTime += travelTime + 180.0 // +3min de tempo de servico
            
            if (stop.type == StopType.PICKUP) {
                pickupTimes[stop.orderId] = accumulatedTime
            } else if (stop.type == StopType.DELIVERY) {
                val pTime = pickupTimes[stop.orderId] ?: 0.0
                val waitTime = accumulatedTime - pTime
                // Multiplica pela urgência do stop para penalizar atrasos em clientes prioritários/comida quente
                totalCustomerWaitTime += waitTime * stop.urgencyScore
            }
            
            lastLat = stop.latitude
            lastLon = stop.longitude
        }
        
        // Custo multiobjetivo balanceando distância e tempo de espera dos clientes
        // Peso de 0.25 significa que cada segundo a mais de espera equivale a 0.25 metros de desvio aceitável
        val waitTimeWeight = 0.25
        return totalDistance + (totalCustomerWaitTime * waitTimeWeight)
    }

    private fun calculatePathCost(
        startLat: Double,
        startLon: Double,
        path: List<StopPoint>,
        multiplier: Double
    ): Double {
        var totalCost = 0.0
        var lastLat = startLat
        var lastLon = startLon
        for (stop in path) {
            totalCost += calculateDistance(lastLat, lastLon, stop.latitude, stop.longitude) * multiplier
            lastLat = stop.latitude
            lastLon = stop.longitude
        }
        return totalCost
    }

    /**
     * Gera uma rota otimizada baseada em todas as ordens ativas de múltiplos apps.
     */
    fun getMultiAppOptimizedRoute(currentLat: Double, currentLng: Double): List<StopPoint> {
        val orders = MultiAppOrderManager.activeOrders.value
        if (orders.isEmpty()) return emptyList()

        val stops = mutableListOf<StopPoint>()
        orders.forEach { order ->
            // Se o status for PICKING_UP, a coleta ainda precisa ser realizada
            if (order.status == OrderStatus.PICKING_UP) {
                stops.add(StopPoint(
                    id = "${order.id}_p",
                    address = order.pickupAddress,
                    latitude = order.pickupLat,
                    longitude = order.pickupLng,
                    type = StopType.PICKUP,
                    orderId = order.id,
                    urgencyScore = if (order.appName.lowercase().contains("ifood")) 1.8 else 1.2, // iFood (comida) tem maior urgência padrão
                    baseValue = order.fare
                ))
            }
            // A entrega sempre é incluída se a ordem não estiver concluída ou cancelada
            if (order.status != OrderStatus.COMPLETED && order.status != OrderStatus.CANCELLED) {
                stops.add(StopPoint(
                    id = "${order.id}_d",
                    address = order.deliveryAddress,
                    latitude = order.deliveryLat,
                    longitude = order.deliveryLng,
                    type = StopType.DELIVERY,
                    orderId = order.id,
                    urgencyScore = if (order.appName.lowercase().contains("ifood")) 1.8 else 1.2,
                    baseValue = 0.0
                ))
            }
        }

        return optimizeRoute(currentLat, currentLng, stops)
    }

    private fun optimizeRouteGreedy(
        currentLat: Double,
        currentLon: Double,
        stops: List<StopPoint>,
        rainMultiplier: Double
    ): List<StopPoint> {
        val optimizedList = mutableListOf<StopPoint>()
        val remainingStops = stops.toMutableList()
        val currentLoad = mutableSetOf<String>()
        var lastLat = currentLat
        var lastLon = currentLon
        val traffic = getTrafficMultiplier() * rainMultiplier

        while (remainingStops.isNotEmpty()) {
            val valid = remainingStops.filter { s -> 
                if (s.type == StopType.PICKUP) true else currentLoad.contains(s.orderId) 
            }
            if (valid.isEmpty()) break
            
            // Heurística Gulosa Inteligente: pondera distância e urgência (tempo de espera do cliente)
            val next = valid.minByOrNull { s -> 
                val dist = calculateDistance(lastLat, lastLon, s.latitude, s.longitude)
                // Reduz o custo aparente se for entrega urgente para priorizar o menor tempo de espera
                val urgencyFactor = if (s.type == StopType.DELIVERY) (1.0 / s.urgencyScore) else 0.9
                dist * urgencyFactor
            } ?: break
            
            optimizedList.add(next)
            remainingStops.remove(next)
            if (next.type == StopType.PICKUP) currentLoad.add(next.orderId) else currentLoad.remove(next.orderId)
            lastLat = next.latitude
            lastLon = next.longitude
        }
        return optimizedList
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0].toDouble()
    }

    private fun getTrafficMultiplier(): Double {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 7..9 -> 1.8
            in 11..13 -> 1.4
            in 17..19 -> 2.1
            in 0..5 -> 0.7
            else -> 1.2
        }
    }

    fun calculateRouteMetrics(
        currentLat: Double,
        currentLon: Double,
        stops: List<StopPoint>,
        rainMultiplier: Double = 1.0
    ): RouteSummary {
        val optimized = optimizeRoute(currentLat, currentLon, stops, rainMultiplier)
        
        val trafficMultiplier = getTrafficMultiplier() * rainMultiplier
        val sequentialCost = calculatePathCost(currentLat, currentLon, stops, trafficMultiplier)
        val optimizedCost = calculatePathCost(currentLat, currentLon, optimized, trafficMultiplier)
        
        val totalDistanceKm = optimizedCost / 1000.0
        val totalTimeSeconds = (optimizedCost / ((28.0 / 3.6) / trafficMultiplier)).toLong() + (stops.size * 180L)
        val timeSavedMinutes = maxOf(0L, ((sequentialCost - optimizedCost) / ((28.0 / 3.6) / trafficMultiplier)).toLong() / 60)

        val totalBaseValue = stops.filter { it.type == StopType.DELIVERY }.sumOf { it.baseValue }
        val efficiencyBonus = (timeSavedMinutes * 0.5) 
        val estimatedProfit = totalBaseValue + efficiencyBonus
        val profitPerHour = if (totalTimeSeconds > 0) (estimatedProfit / (totalTimeSeconds / 3600.0)) else 0.0

        return RouteSummary(
            optimizedStops = optimized,
            totalDistanceKm = totalDistanceKm,
            totalTimeMinutes = totalTimeSeconds / 60,
            timeSavedMinutes = timeSavedMinutes,
            efficiencyScore = if (sequentialCost > 0) ((sequentialCost - optimizedCost) / sequentialCost * 100).toInt() else 0,
            estimatedProfit = estimatedProfit,
            profitPerHour = profitPerHour
        )
    }

    data class RouteSummary(
        val optimizedStops: List<StopPoint>,
        val totalDistanceKm: Double,
        val totalTimeMinutes: Long,
        val timeSavedMinutes: Long,
        val efficiencyScore: Int,
        val estimatedProfit: Double,
        val profitPerHour: Double
    )

    fun getMockCoordinates(address: String): Pair<Double, Double> {
        val hash = Math.abs(address.hashCode())
        val latOffset = (hash % 2000 - 1000) / 10000.0
        val lonOffset = (hash % 1600 - 800) / 10000.0
        return Pair(-23.5505 + latOffset, -46.6333 + lonOffset)
    }
    
    fun formatEta(timestamp: Long): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        return String.format(Locale.getDefault(), "%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
    }
}
