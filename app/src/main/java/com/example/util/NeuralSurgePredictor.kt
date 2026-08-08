package com.example.util

import android.util.Log
import java.util.Calendar
import kotlin.math.*

/**
 * Preditor Neural de Picos de Demanda (Surge) e Ponto Estratégico de Espera (Smart Staging Hotspot)
 * para entregadores em plataformas brasileiras (iFood, Rappi, Uber, 99, Lalamove, Loggi).
 */
data class PredictiveHotspot(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val distanceKm: Double,
    val predictedSurgeMultiplier: Double, // Ex: 1.4x (R$ +4.50)
    val estimatedFarePerKm: Double, // Ex: R$ 7.80/km
    val demandScore: Int, // 0 - 100%
    val dominantApps: List<String>,
    val bestTimeWindow: String, // Ex: "18:30 - 20:00"
    val recommendedVehiclePosition: String, // Ex: "Estacionar na Av. Paulista, próx. ao Shopping"
    val returnCorridorActive: Boolean = false
)

data class ReturnCorridorStrategy(
    val destinationNeighborhood: String,
    val returnRiskScore: Int, // 0 (Seguro) - 100 (Alto risco de voltar vazio)
    val estimatedEmptyKmPenalty: Double, // Perda estimada em combustivel/tempo
    val recommendedReturnWaypoint: String,
    val suggestedAcceptanceThreshold: Double // Novo R$/km mínimo para compensar a ida
)

object NeuralSurgePredictor {
    private const val TAG = "NeuralSurgePredictor"

    /**
     * Calcula os Hotspots Preditivos mais lucrativos nos arredores do motorista.
     */
    fun predictSurgeHotspots(
        userLat: Double,
        userLng: Double,
        rainModeActive: Boolean = false,
        trafficDensityFactor: Double = 1.2
    ): List<PredictiveHotspot> {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val isPeakHour = (hour in 11..14) || (hour in 18..22)
        val baseRainMultiplier = if (rainModeActive) 1.35 else 1.0

        // Coordenadas simuladas em torno da localização atual (Clusters Paulistas / Centrais)
        val hotspots = listOf(
            Triple("Corredor Gastronômico Paulista", 0.008, -0.005),
            Triple("Pólo de Restaurantes Pinheiros / Faria Lima", -0.012, 0.010),
            Triple("Cluster de Fast-Food Moema / Ibirapuera", -0.018, -0.008),
            Triple("Centro Logístico Lalamove / Mercado Livre", 0.015, 0.012)
        )

        return hotspots.mapIndexed { index, (name, latOffset, lngOffset) ->
            val lat = userLat + latOffset
            val lng = userLng + lngOffset
            val dist = calculateDistanceKm(userLat, userLng, lat, lng)

            val baseDemand = if (isPeakHour) 75 + (index * 5) else 45 + (index * 8)
            val finalDemand = (baseDemand * baseRainMultiplier * trafficDensityFactor).toInt().coerceIn(30, 99)

            val surgeMultiplier = 1.0 + ((finalDemand - 40) / 100.0)
            val estimatedFarePerKm = (4.50 * surgeMultiplier).coerceIn(3.80, 12.50)

            val apps = when (index % 3) {
                0 -> listOf("iFood", "Rappi", "Uber Flash")
                1 -> listOf("Uber Moto", "99Moto", "iFood")
                else -> listOf("Lalamove", "Loggi", "Rappi Turbo")
            }

            PredictiveHotspot(
                id = "hotspot_$index",
                name = name,
                latitude = lat,
                longitude = lng,
                distanceKm = Math.round(dist * 10.0) / 10.0,
                predictedSurgeMultiplier = Math.round(surgeMultiplier * 100.0) / 100.0,
                estimatedFarePerKm = Math.round(estimatedFarePerKm * 100.0) / 100.0,
                demandScore = finalDemand,
                dominantApps = apps,
                bestTimeWindow = if (isPeakHour) "Agora - Próximos 45 min" else "Pico esperado às ${(hour + 1) % 24}:00h",
                recommendedVehiclePosition = "Posição recomendada: A 150m da concentração de lojas de $name"
            )
        }.sortedByDescending { it.demandScore }
    }

    /**
     * Avalia o risco do bairro de destino e calcula a taxa de compensação do "Corredor Antivazio".
     */
    fun analyzeReturnCorridor(
        destinationAddress: String,
        tripDistanceKm: Double,
        tripFare: Double,
        fuelPrice: Double,
        kmPerLiter: Double
    ): ReturnCorridorStrategy {
        val lowDemandKeywords = listOf("industrial", "estrada", "rodovia", "periferia", "interior", "zona rural", "sítio", "residencial fechado")
        val isLowDemandZone = lowDemandKeywords.any { destinationAddress.contains(it, ignoreCase = true) } || tripDistanceKm > 8.0

        val returnRiskScore = if (isLowDemandZone) 82 else 25
        val fuelCostPerKm = fuelPrice / kmPerLiter.coerceAtLeast(10.0)
        val estimatedEmptyKmPenalty = if (isLowDemandZone) (tripDistanceKm * 0.7) * fuelCostPerKm else (tripDistanceKm * 0.2) * fuelCostPerKm

        val suggestedThreshold = if (isLowDemandZone) {
            // Se o local for distante ou isolado, o R$/km mínimo aceitável sobe para cobrir o retorno
            (tripFare / tripDistanceKm) * 1.35
        } else {
            tripFare / tripDistanceKm
        }

        return ReturnCorridorStrategy(
            destinationNeighborhood = destinationAddress.take(25),
            returnRiskScore = returnRiskScore,
            estimatedEmptyKmPenalty = Math.round(estimatedEmptyKmPenalty * 100.0) / 100.0,
            recommendedReturnWaypoint = if (isLowDemandZone) "Retornar via corredor comercial principal em no máximo 3 km" else "Permanecer na zona atual - alta densidade",
            suggestedAcceptanceThreshold = Math.round(suggestedThreshold * 100.0) / 100.0
        )
    }

    private fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Raio da Terra em km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
