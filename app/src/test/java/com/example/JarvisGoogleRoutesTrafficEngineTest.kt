package com.example

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes unitários para o módulo JarvisGoogleRoutesTrafficEngine.
 * Valida a estimativa de tráfego, o cálculo do fator de peso em tempo real
 * e o ajuste conjunto de tráfego, cozinha e proximidade angular.
 */
class JarvisGoogleRoutesTrafficEngineTest {

    @Test
    fun testTrafficCongestionLevelsPenaltyFactors() {
        // Congestionamento livre deve ter multiplicador neutro 1.0x
        assertEquals(1.0, TrafficCongestionLevel.CLEAR_SPEEDWAY.penaltyRatio, 0.01)

        // Congestionamento moderado
        assertEquals(1.25, TrafficCongestionLevel.MODERATE_BUSY.penaltyRatio, 0.01)

        // Congestionamento severo
        assertEquals(1.85, TrafficCongestionLevel.SEVERE_GRIDLOCK.penaltyRatio, 0.01)
    }

    @Test
    fun testCalculateRouteWithTrafficReturnsValidMetrics() = runBlocking {
        // Coordenadas SP: Av. Paulista (-23.5615, -46.6558) até Consolação (-23.5532, -46.6612)
        val result = JarvisGoogleRoutesTrafficEngine.calculateRouteWithTraffic(
            originLat = -23.5615,
            originLng = -46.6558,
            destLat = -23.5532,
            destLng = -46.6612,
            originName = "Paulista",
            destName = "Consolação"
        )

        assertNotNull(result)
        assertTrue("Distância deve ser positiva", result.distanceKm > 0.0)
        assertTrue("Duração com tráfego deve ser maior ou igual à duração estática",
            result.trafficDurationMinutes >= (result.staticDurationMinutes * 0.9))
        assertTrue("Fator de peso de tráfego deve estar entre 0.20 e 1.0",
            result.trafficWeightFactor in 0.20f..1.0f)
    }

    @Test
    fun testOfferWeightAdjustmentDegradesWithHeavyTrafficAndSlowKitchen() = runBlocking {
        // Cenário 1: Restaurante rápido (Farmácia 2m), rota curta sem tráfego excessivo
        val scoreOptimal = JarvisGoogleRoutesTrafficEngine.evaluateOfferWithTrafficRoutes(
            offerTitle = "iFood: Drogaria Raia Express",
            platform = "iFood",
            payoutBrl = 22.00,
            pickupLat = -23.5615,
            pickupLng = -46.6558,
            dropoffLat = -23.5532,
            dropoffLng = -46.6612,
            restaurantName = "Drogaria Raia",
            pickupAddress = "Av. Paulista, 1000"
        )

        // Cenário 2: Restaurante em Shopping Center lento (12m), mesma distância
        val scoreSlowKitchen = JarvisGoogleRoutesTrafficEngine.evaluateOfferWithTrafficRoutes(
            offerTitle = "iFood: Outback Shopping Pátio Paulista",
            platform = "iFood",
            payoutBrl = 22.00,
            pickupLat = -23.5615,
            pickupLng = -46.6558,
            dropoffLat = -23.5532,
            dropoffLng = -46.6612,
            restaurantName = "Outback Shopping Pátio Paulista",
            pickupAddress = "Rua Treze de Maio, 1947"
        )

        assertTrue(
            "Fator de peso da cozinha rápida deve ser superior ao do shopping center",
            scoreOptimal.kitchenWaitWeight > scoreSlowKitchen.kitchenWaitWeight
        )

        assertTrue(
            "Rentabilidade real por hora da corrida com balcão rápido deve ser maior",
            scoreOptimal.adjustedHourlyRateBrl > scoreSlowKitchen.adjustedHourlyRateBrl
        )

        assertTrue(
            "Score final da corrida com balcão rápido deve superar a com espera de shopping",
            scoreOptimal.finalScore > scoreSlowKitchen.finalScore
        )
    }

    @Test
    fun testJarvisActionRejectsOnDivergentAngularRoutes() = runBlocking {
        // Pedido 1 indo para o Norte/Noroeste (Paulista -> Consolação)
        val pLat1 = -23.5650
        val pLng1 = -46.6520
        val dLat1 = -23.5550
        val dLng1 = -46.6600

        // Pedido 2 indo em direção oposta / Sudeste (Paulista -> Jabaquara/Saúde)
        val o2Opposite = ScreenPoolOffer(
            appName = "99Food",
            packageName = "com.didiglobal.driver",
            restaurantName = "Pizzaria Sul",
            pickupAddress = "Av. Paulista, 1200",
            dropoffAddress = "Rua Domingos de Morais, 3000",
            value = 15.0,
            distanceKm = 5.0,
            pickupLat = -23.5640,
            pickupLng = -46.6530,
            dropoffLat = -23.6100,
            dropoffLng = -46.6380
        )

        val score = JarvisGoogleRoutesTrafficEngine.evaluateOfferWithTrafficRoutes(
            offerTitle = "iFood + 99Food Opostos",
            platform = "iFood",
            payoutBrl = 30.00,
            pickupLat = pLat1,
            pickupLng = pLng1,
            dropoffLat = dLat1,
            dropoffLng = dLng1,
            restaurantName = "Burger King",
            pickupAddress = "Av. Paulista",
            secondaryOffer = o2Opposite
        )

        assertNotNull(score.angularResult)
        assertTrue("Divergência angular deve ser expressiva", score.angularResult!!.corridorAngleDiffDeg > 50f)
        assertFalse("Não deve ser aprovado como ótimo devido ao contra-fluxo",
            score.recommendedAction == JarvisTrafficAction.ACCEPT_OPTIMAL)
    }
}
