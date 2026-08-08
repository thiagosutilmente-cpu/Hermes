package com.example.util

import com.example.data.OfferEntity
import com.example.coordinator.DeliveryStop
import kotlin.math.*

/**
 * Análise Inteligente e Otimização de Desvios para Entregas Secundárias (B, C, D)
 * em Rotas Encadeadas de Multi-Apps no Brasil.
 */
data class DetourAnalysisResult(
    val primaryOffer: OfferEntity,
    val secondaryOffer: OfferEntity,
    val detourType: DetourType,
    val originalDistanceKm: Double,
    val combinedDistanceKm: Double,
    val extraDetourKm: Double,
    val originalTimeMin: Int,
    val combinedTimeMin: Int,
    val extraDetourTimeMin: Int,
    val primaryValue: Double,
    val secondaryValue: Double,
    val totalValue: Double,
    val detourYieldPerKm: Double, // R$/km gerado EXCLUSIVAMENTE pela distância do desvio
    val combinedGainPerKm: Double, // R$/km total da rota combinada
    val efficiencyScore: Int, // 0 a 100%
    val verdict: String, // "DESVIO DE OURO", "DESVIO EXCELENTE", "DESVIO MARGINAL", "DESVIO INVIÁVEL"
    val verdictReason: String,
    val sequencedStops: List<DeliveryStop>
)

enum class DetourType {
    INTERCEPT_EN_ROUTE, // Coleta B e Entrega B ocorrem ANTES da Entrega A
    CORRIDOR_EXTENSION,  // Coleta B no trajeto de A, Entrega B LOGO APÓS Entrega A
    PARALLEL_CLUSTER     // Coletas e Entregas em clusters vizinhos (Prédios/Shoppings próximos)
}

object ChainedDetourEngine {

    /**
     * Calcula o impacto exato do desvio para anexar a Oferta B a uma Oferta Principal A.
     */
    fun analyzeDetourForOfferB(
        userLat: Double,
        userLng: Double,
        offerA: OfferEntity,
        offerB: OfferEntity,
        trafficMultiplier: Double = 1.2
    ): DetourAnalysisResult {
        val (aPLat, aPLng) = RouteOptimizer.getMockCoordinates(offerA.pickupAddress)
        val (aDLat, aDLng) = RouteOptimizer.getMockCoordinates(offerA.deliveryAddress)
        val (bPLat, bPLng) = RouteOptimizer.getMockCoordinates(offerB.pickupAddress)
        val (bDLat, bDLng) = RouteOptimizer.getMockCoordinates(offerB.deliveryAddress)

        // Distância e tempo originais da Rota A isolada
        val distUserToAp = calculateDist(userLat, userLng, aPLat, aPLng)
        val distApToAd = if (offerA.totalDistance > 0) offerA.totalDistance else calculateDist(aPLat, aPLng, aDLat, aDLng)
        val origDist = distUserToAp + distApToAd
        val origTime = (origDist * 3.2 + 5).toInt().coerceAtLeast(10)

        // Paradas da Oferta A e B
        val stopAp = DeliveryStop("${offerA.id}_p", offerA.pickupAddress, aPLat, aPLng, offerA.appName, 1)
        val stopAd = DeliveryStop("${offerA.id}_d", offerA.deliveryAddress, aDLat, aDLng, offerA.appName, 4)
        val stopBp = DeliveryStop("${offerB.id}_p", offerB.pickupAddress, bPLat, bPLng, offerB.appName, 2)
        val stopBd = DeliveryStop("${offerB.id}_d", offerB.deliveryAddress, bDLat, bDLng, offerB.appName, 3)

        // Sequência 1: Intercept en route (Ap -> Bp -> Bd -> Ad)
        val seq1Dist = calculateDist(userLat, userLng, aPLat, aPLng) +
                       calculateDist(aPLat, aPLng, bPLat, bPLng) +
                       calculateDist(bPLat, bPLng, bDLat, bDLng) +
                       calculateDist(bDLat, bDLng, aDLat, aDLng)

        // Sequência 2: Corridor extension (Ap -> Bp -> Ad -> Bd)
        val seq2Dist = calculateDist(userLat, userLng, aPLat, aPLng) +
                       calculateDist(aPLat, aPLng, bPLat, bPLng) +
                       calculateDist(bPLat, bPLng, aDLat, aDLng) +
                       calculateDist(aDLat, aDLng, bDLat, bDLng)

        val (bestDist, bestSeq, detourType) = if (seq1Dist <= seq2Dist) {
            Triple(seq1Dist, listOf(stopAp, stopBp, stopBd, stopAd), DetourType.INTERCEPT_EN_ROUTE)
        } else {
            Triple(seq2Dist, listOf(stopAp, stopBp, stopAd, stopBd), DetourType.CORRIDOR_EXTENSION)
        }

        val extraKm = (bestDist - origDist).coerceAtLeast(0.2)
        val combinedTime = (bestDist * 3.0 + 8).toInt().coerceAtLeast(14)
        val extraTime = (combinedTime - origTime).coerceAtLeast(2)

        val totalValue = offerA.fareValue + offerB.fareValue
        val detourYieldPerKm = offerB.fareValue / extraKm
        val combinedGainPerKm = totalValue / bestDist

        // Cálculo da eficiência (0-100%)
        // Se a oferta B paga muito e o desvio extra em km é pequeno, a eficiência explode!
        val efficiency = ((detourYieldPerKm / 12.0) * 100).toInt().coerceIn(40, 99)

        val (verdict, reason) = when {
            extraKm <= 1.5 && detourYieldPerKm >= 8.0 -> {
                "DESVIO DE OURO" to "Adiciona apenas ${String.format("%.1f", extraKm)} km ao trajeto e gera R$ ${String.format("%.2f", detourYieldPerKm)}/km no desvio"
            }
            detourYieldPerKm >= 5.0 && extraTime <= 8 -> {
                "DESVIO EXCELENTE" to "Retorno financeiro alto (+R$ ${String.format("%.2f", offerB.fareValue)}) com impacto de tempo mínimo (+${extraTime} min)"
            }
            extraKm <= 3.5 -> {
                "DESVIO MARGINAL" to "Desvio aceitável no mesmo corredor urbano de entregas"
            }
            else -> {
                "DESVIO INVIÁVEL" to "Aumento significativo de trajeto (+${String.format("%.1f", extraKm)} km) reduz a eficiência geral da rota"
            }
        }

        return DetourAnalysisResult(
            primaryOffer = offerA,
            secondaryOffer = offerB,
            detourType = detourType,
            originalDistanceKm = Math.round(origDist * 10.0) / 10.0,
            combinedDistanceKm = Math.round(bestDist * 10.0) / 10.0,
            extraDetourKm = Math.round(extraKm * 10.0) / 10.0,
            originalTimeMin = origTime,
            combinedTimeMin = combinedTime,
            extraDetourTimeMin = extraTime,
            primaryValue = offerA.fareValue,
            secondaryValue = offerB.fareValue,
            totalValue = Math.round(totalValue * 100.0) / 100.0,
            detourYieldPerKm = Math.round(detourYieldPerKm * 100.0) / 100.0,
            combinedGainPerKm = Math.round(combinedGainPerKm * 100.0) / 100.0,
            efficiencyScore = efficiency,
            verdict = verdict,
            verdictReason = reason,
            sequencedStops = bestSeq
        )
    }

    private fun calculateDist(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
