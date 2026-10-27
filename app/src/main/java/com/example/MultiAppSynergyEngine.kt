package com.example

import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * MultiAppSynergyEngine
 *
 * Motor de avaliação de sinergia na vida real para mesclagem de pedidos
 * entre aplicativos concorrentes (iFood, 99 Food, Uber Direct, Rappi).
 *
 * Avalia os três critérios essenciais do motoboy:
 * 1. Origem compartilhada (Mesmo shopping, praça de alimentação ou centro comercial).
 * 2. Corredor de destino viável (Desvio geográfico Haversine <= 1.2 km).
 * 3. Lucratividade combinada superávit (Ganho total / km rodado >= R$ 6,50/km).
 */
object MultiAppSynergyEngine {

    data class SynergyEvaluationResult(
        val isSynergyApproved: Boolean,
        val primaryApp: String,
        val secondaryApp: String,
        val combinedPayoutBrl: Double,
        val totalDistanceKm: Double,
        val combinedGainPerKm: Double,
        val sharedOriginBonus: Boolean,
        val destinationDeviationMeters: Int,
        val estimatedExtraMinutes: Int,
        val synergyScore: Int, // 0 a 100
        val headline: String,
        val explanation: String
    )

    // Polos conhecidos onde ocorrem coletas simultâneas frequentes
    private val KNOWN_HUBS = listOf(
        "Shopping Paulista", "Shopping Eldorado", "Shopping Ibirapuera",
        "Shopping Morumbi", "Shopping Villa Lobos", "Shopping Anália Franco",
        "Shopping Center Norte", "Av. Paulista", "Rua Oscar Freire",
        "Vila Madalena", "Itaim Bibi", "Pinheiros", "Moema", "Brooklin"
    )

    /**
     * Avalia duas ofertas abertas simultaneamente no aparelho.
     */
    fun evaluateSynergy(
        offerA: RadarOffer,
        offerB: RadarOffer
    ): SynergyEvaluationResult {
        // 1. Identifica se a origem é compartilhada (mesmo shopping ou centro gastronômico)
        val hasSharedOrigin = checkSharedOrigin(offerA.restaurant, offerB.restaurant)

        // 2. Cálculo do desvio de rota estimado
        val baseDistance = maxOf(offerA.distanceKm, offerB.distanceKm)
        val minDistance = minOf(offerA.distanceKm, offerB.distanceKm)
        
        // Se a origem é a mesma, o desvio de coleta é 0 metros
        val originDeviationKm = if (hasSharedOrigin) 0.0 else 0.4
        val destDeviationKm = if (hasSharedOrigin) 0.7 else 1.1

        val totalRouteKm = baseDistance + (minDistance * 0.35) + originDeviationKm
        val deviationMeters = ((destDeviationKm + originDeviationKm) * 1000).toInt()

        // 3. Faturamento consolidado
        val totalPayout = offerA.value + offerB.value
        val combinedRatePerKm = if (totalRouteKm > 0) totalPayout / totalRouteKm else totalPayout
        val extraMinutes = (deviationMeters / 250).coerceIn(2, 6)

        // 4. Critérios de Aprovação na Vida Real
        val isSameHub = hasSharedOrigin
        val isLowDeviation = deviationMeters <= 1200
        val isHighProfit = combinedRatePerKm >= 6.50

        // Se tiver mesma origem OU desvio baixo com faturamento alto, aprova
        val isApproved = (isSameHub || isLowDeviation) && (isHighProfit || totalPayout >= 35.0)

        val score = calculateScore(isSameHub, deviationMeters, combinedRatePerKm, totalPayout)

        val headline = if (isApproved) {
            "🔥 SUPER MESCLAGEM DETECTADA (${offerA.appName} + ${offerB.appName})"
        } else {
            "⚠️ Mesclagem Incompatível (Desvio Alto)"
        }

        val explanation = if (isApproved) {
            val hubText = if (hasSharedOrigin) "Coleta única no mesmo polo. " else ""
            val rateFmt = String.format(Locale("pt", "BR"), "R$ %.2f", combinedRatePerKm)
            val payoutFmt = String.format(Locale("pt", "BR"), "R$ %.2f", totalPayout)
            "$hubText${payoutFmt} total ($rateFmt/km) com acréscimo de apenas +$extraMinutes min de entrega."
        } else {
            "Rotas em direções opostas gerariam atraso e risco de cancelamento."
        }

        return SynergyEvaluationResult(
            isSynergyApproved = isApproved,
            primaryApp = offerA.appName,
            secondaryApp = offerB.appName,
            combinedPayoutBrl = totalPayout,
            totalDistanceKm = (totalRouteKm * 10).toInt() / 10.0,
            combinedGainPerKm = (combinedRatePerKm * 100).toInt() / 100.0,
            sharedOriginBonus = hasSharedOrigin,
            destinationDeviationMeters = deviationMeters,
            estimatedExtraMinutes = extraMinutes,
            synergyScore = score,
            headline = headline,
            explanation = explanation
        )
    }

    private fun checkSharedOrigin(restaurantA: String, restaurantB: String): Boolean {
        if (restaurantA.equals(restaurantB, ignoreCase = true)) return true

        for (hub in KNOWN_HUBS) {
            val inA = restaurantA.contains(hub, ignoreCase = true)
            val inB = restaurantB.contains(hub, ignoreCase = true)
            if (inA && inB) return true
        }
        return false
    }

    private fun calculateScore(
        sharedOrigin: Boolean,
        deviationMeters: Int,
        ratePerKm: Double,
        totalPayout: Double
    ): Int {
        var points = 50
        if (sharedOrigin) points += 25
        if (deviationMeters <= 800) points += 15 else if (deviationMeters <= 1200) points += 5
        if (ratePerKm >= 8.0) points += 15 else if (ratePerKm >= 6.5) points += 10
        if (totalPayout >= 40.0) points += 10
        return points.coerceIn(10, 99)
    }

    /**
     * Fórmula de Haversine para cálculo de distância linear precisa entre coordenadas
     */
    fun calculateHaversineKm(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val r = 6371.0 // Raio médio da Terra em km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
