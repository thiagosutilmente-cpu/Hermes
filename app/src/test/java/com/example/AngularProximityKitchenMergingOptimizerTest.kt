package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes unitários para o algoritmo AngularProximityKitchenMergingOptimizer.
 * Valida a priorização angular entre plataformas e o cálculo do fator de peso de espera de cozinha.
 */
class AngularProximityKitchenMergingOptimizerTest {

    @Test
    fun testAngularDifferenceCalculation() {
        // Azimutes paralelos
        val diff1 = AngularProximityKitchenMergingOptimizer.calculateAngularDifference(15f, 25f)
        assertEquals(10f, diff1, 0.01f)

        // Azimutes passando pelo Norte (350° e 10°)
        val diff2 = AngularProximityKitchenMergingOptimizer.calculateAngularDifference(350f, 10f)
        assertEquals(20f, diff2, 0.01f)

        // Azimutes em direções opostas (0° e 180°)
        val diff3 = AngularProximityKitchenMergingOptimizer.calculateAngularDifference(0f, 180f)
        assertEquals(180f, diff3, 0.01f)
    }

    @Test
    fun testSingleRestaurantWaitEstimator() {
        // Shoppings devem ter tempo alto de espera (12m)
        val shoppingWait = AngularProximityKitchenMergingOptimizer.estimateSingleRestaurantWait(
            "Outback Steakhouse - Shopping Iguatemi", "Av. Brigadeiro Faria Lima, 2232"
        )
        assertEquals(12, shoppingWait)

        // Pizzerias devem ter espera de preparo à la carte (9m)
        val pizzaWait = AngularProximityKitchenMergingOptimizer.estimateSingleRestaurantWait(
            "Pizzaria Speranza", "Rua Treze de Maio, 1004"
        )
        assertEquals(9, pizzaWait)

        // Fast food deve ter tempo rápido (3m)
        val fastFoodWait = AngularProximityKitchenMergingOptimizer.estimateSingleRestaurantWait(
            "McDonald's Paulista", "Av. Paulista, 2026"
        )
        assertEquals(3, fastFoodWait)

        // Farmácias devem ter retirada expressa imediata (2m)
        val pharmacyWait = AngularProximityKitchenMergingOptimizer.estimateSingleRestaurantWait(
            "Drogaria Raia Express", "Alameda Santos, 1200"
        )
        assertEquals(2, pharmacyWait)
    }

    @Test
    fun testAngularProximityEvaluationColinear() {
        // Oferta 1: iFood saindo da Paulista descendo para a Consolação
        val o1 = ScreenPoolOffer(
            appName = "iFood",
            packageName = "com.ifood.entregador",
            restaurantName = "Burger King Paulista",
            pickupAddress = "Av. Paulista, 1000",
            dropoffAddress = "Rua Augusta, 1500",
            value = 18.0,
            distanceKm = 3.0,
            pickupLat = -23.5650,
            pickupLng = -46.6520,
            dropoffLat = -23.5550,
            dropoffLng = -46.6600
        )

        // Oferta 2: 99Food saindo da Paulista descendo também para a Consolação (mesmo corredor viário)
        val o2 = ScreenPoolOffer(
            appName = "99Food",
            packageName = "com.didiglobal.driver",
            restaurantName = "Drogaria São Paulo",
            pickupAddress = "Av. Paulista, 1200",
            dropoffAddress = "Rua Bela Cintra, 1400",
            value = 19.0,
            distanceKm = 3.2,
            pickupLat = -23.5640,
            pickupLng = -46.6530,
            dropoffLat = -23.5540,
            dropoffLng = -46.6610
        )

        val result = AngularProximityKitchenMergingOptimizer.evaluateAngularProximity(o1, o2)

        assertTrue("Divergência angular no mesmo corredor deve ser menor que 25°", result.corridorAngleDiffDeg < 25f)
        assertTrue("Score angular deve ser superior a 0.80", result.angularProximityScore > 0.80f)
        assertTrue("Deve ser compatível angularmente", result.isCompatibleAngularly)
    }

    @Test
    fun testKitchenWaitWeightFactorDecay() {
        // Pedido 1 e Pedido 2 rápidos (Farmácia + Fast Food)
        val o1 = ScreenPoolOffer(
            appName = "iFood",
            packageName = "com.ifood.entregador",
            restaurantName = "Subway Express",
            pickupAddress = "Rua Augusta, 100",
            dropoffAddress = "Rua Frei Caneca, 200",
            value = 15.0,
            distanceKm = 2.0
        )
        val o2Fast = ScreenPoolOffer(
            appName = "99Food",
            packageName = "com.didiglobal.driver",
            restaurantName = "Drogaria Raia",
            pickupAddress = "Rua Augusta, 300",
            dropoffAddress = "Rua Frei Caneca, 400",
            value = 14.0,
            distanceKm = 2.0
        )

        val fastKitchenResult = AngularProximityKitchenMergingOptimizer.evaluateKitchenWaitWeight(
            o1, o2Fast, transitP1toP2Minutes = 3.0
        )

        // Pedido 2 lento (Shopping Center com 12 min de espera)
        val o2Slow = ScreenPoolOffer(
            appName = "99Food",
            packageName = "com.didiglobal.driver",
            restaurantName = "Outback Shopping Center Norte",
            pickupAddress = "Shopping Center Norte",
            dropoffAddress = "Rua Frei Caneca, 400",
            value = 14.0,
            distanceKm = 2.0
        )

        val slowKitchenResult = AngularProximityKitchenMergingOptimizer.evaluateKitchenWaitWeight(
            o1, o2Slow, transitP1toP2Minutes = 3.0
        )

        assertTrue(
            "Fator de peso de restaurante rápido deve ser significativamente maior que o de shopping lento",
            fastKitchenResult.kitchenWaitWeightFactor > slowKitchenResult.kitchenWaitWeightFactor
        )
        assertTrue("Restaurante rápido deve ter fator de peso > 0.80", fastKitchenResult.kitchenWaitWeightFactor > 0.80f)
        assertTrue("Shopping lento deve ter fator de peso < 0.60", slowKitchenResult.kitchenWaitWeightFactor < 0.60f)
    }
}
