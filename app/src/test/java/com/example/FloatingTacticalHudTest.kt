package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes unitários para o modelo e regras de cálculo do HUD Flutuante Tático.
 */
class FloatingTacticalHudTest {

    @Test
    fun testGoodOfferCalculationAboveThreshold() {
        val value = 28.50
        val distanceKm = 3.9
        val pricePerKm = value / distanceKm
        val isGood = pricePerKm >= 4.50

        val offer = OverlayWindowManager.FloatingTacticalOffer(
            appName = "iFood",
            packageName = "com.ifood.driver",
            value = value,
            distanceKm = distanceKm,
            pricePerKm = pricePerKm,
            isGoodDeal = isGood,
            dealLabel = "R$ 7,20/km 🟢 BOA",
            destination = "Av. Rebouças, 1200",
            pickup = "Shopping Eldorado",
            hasDualRoute = true,
            dualRouteAppName = "99 Moto",
            dualRouteExtraGain = 16.50
        )

        assertTrue("Preço por km deve ser superior a R$ 4,50", offer.pricePerKm > 4.50)
        assertTrue("Oferta deve ser classificada como BOA", offer.isGoodDeal)
        assertTrue("Deve indicar rota dupla compatível", offer.hasDualRoute)
        assertEquals(45.00, offer.value + offer.dualRouteExtraGain, 0.01)
    }

    @Test
    fun testBadOfferCalculationBelowThreshold() {
        val value = 8.40
        val distanceKm = 3.0
        val pricePerKm = value / distanceKm
        val isGood = pricePerKm >= 4.50

        val offer = OverlayWindowManager.FloatingTacticalOffer(
            appName = "99 Moto",
            packageName = "com.taxis99",
            value = value,
            distanceKm = distanceKm,
            pricePerKm = pricePerKm,
            isGoodDeal = isGood,
            dealLabel = "R$ 2,80/km 🔴 PREJUÍZO",
            destination = "Parque Ibirapuera",
            pickup = "Rua Augusta",
            hasDualRoute = false,
            dualRouteAppName = "",
            dualRouteExtraGain = 0.0
        )

        assertEquals(2.80, offer.pricePerKm, 0.01)
        assertFalse("Oferta deve ser classificada como PREJUÍZO", offer.isGoodDeal)
        assertFalse("Não deve indicar rota dupla", offer.hasDualRoute)
    }
}
