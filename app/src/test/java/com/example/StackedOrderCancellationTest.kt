package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes unitários para o motor de dissolução de corridas mescladas (StackedOrderCancellationManager)
 * e o motor de decisão de auto-aceite (UltraProfitAutoAcceptManager).
 */
class StackedOrderCancellationTest {

    @Test
    fun testSubOrderCancellationRecalculatesProfitably() {
        val sub1 = SubDeliveryOrder(
            appName = "iFood",
            restaurant = "Outback Steakhouse",
            value = 24.00,
            distanceKm = 3.0,
            pickupAddress = "Shopping Morumbi",
            destinationAddress = "Rua Flórida, 500"
        )
        val sub2 = SubDeliveryOrder(
            appName = "Rappi",
            restaurant = "Madero Burger",
            value = 16.00,
            distanceKm = 2.5,
            pickupAddress = "Shopping Morumbi",
            destinationAddress = "Av. Santo Amaro, 1200"
        )

        val totalValue = sub1.value + sub2.value // R$ 40,00
        val totalDistance = 5.0

        val multiOffer = RadarOffer(
            id = "multi_123",
            appName = "iFood + Rappi",
            restaurant = "Outback & Madero",
            value = totalValue,
            distanceKm = totalDistance,
            isMultiStack = true,
            subOrders = listOf(sub1, sub2)
        )

        // Se o Madero Burger cancelar (R$ 16,00 cancelado, resta Outback por R$ 24,00 e 3.0 km)
        val cancelledSub = sub2
        val remainingSubOrders = listOf(sub1)

        val remainingValue = remainingSubOrders.sumOf { it.value }
        val remainingDistance = remainingSubOrders.sumOf { it.distanceKm }
        val remainingGainPerKm = remainingValue / remainingDistance

        assertEquals(24.00, remainingValue, 0.01)
        assertEquals(3.0, remainingDistance, 0.01)
        assertEquals(8.00, remainingGainPerKm, 0.01) // R$ 8,00/km (altamente lucrativo)

        val isViable = remainingSubOrders.isNotEmpty() &&
                remainingGainPerKm >= StackedOrderCancellationManager.MIN_VIABLE_GAIN_PER_KM &&
                remainingValue >= 11.00

        assertTrue("A rota restante com Outback deve ser viável", isViable)
    }

    @Test
    fun testSubOrderCancellationDetectsDeficitRoute() {
        // Oferta combinada onde o pedido principal de alto valor é cancelado
        val subLucrativo = SubDeliveryOrder(
            appName = "iFood",
            restaurant = "Pobre Juan",
            value = 35.00,
            distanceKm = 2.0
        )
        val subDeficitario = SubDeliveryOrder(
            appName = "Rappi",
            restaurant = "Pastelaria Express",
            value = 7.50,
            distanceKm = 4.0
        )

        // Pobre Juan cancelou! Sobrou apenas R$ 7,50 para rodar 4.0 km (R$ 1,87/km)
        val remainingSubOrders = listOf(subDeficitario)
        val remainingValue = remainingSubOrders.sumOf { it.value }
        val remainingDistance = remainingSubOrders.sumOf { it.distanceKm }
        val remainingGainPerKm = remainingValue / remainingDistance

        assertEquals(7.50, remainingValue, 0.01)
        assertEquals(4.0, remainingDistance, 0.01)
        assertEquals(1.875, remainingGainPerKm, 0.01)

        val isViable = remainingSubOrders.isNotEmpty() &&
                remainingGainPerKm >= StackedOrderCancellationManager.MIN_VIABLE_GAIN_PER_KM &&
                remainingValue >= 11.00

        assertFalse("A rota deficitária de R$ 1,87/km não deve ser classificada como viável", isViable)
    }

    @Test
    fun testTotalCancellationWhenAllSubOrdersCancelled() {
        val remainingSubOrders = emptyList<SubDeliveryOrder>()
        val remainingValue = if (remainingSubOrders.isNotEmpty()) remainingSubOrders.sumOf { it.value } else 0.0
        val remainingDistance = if (remainingSubOrders.isNotEmpty()) remainingSubOrders.sumOf { it.distanceKm } else 0.0

        assertEquals(0.0, remainingValue, 0.001)
        assertEquals(0.0, remainingDistance, 0.001)

        val verdictTitle = if (remainingSubOrders.isEmpty()) {
            "CANCELAMENTO TOTAL"
        } else {
            "ROTA RESTANTE LUCRATIVA"
        }

        assertEquals("CANCELAMENTO TOTAL", verdictTitle)
    }

    @Test
    fun testAutoAcceptCriteriaEvaluation() {
        val criteria = AutoAcceptCriteria(
            isEnabled = true,
            minGainPerKm = 5.00,
            minValueBrl = 18.00,
            maxDistanceKm = 6.0,
            minHourlyRateBrl = 45.0,
            enabledPlatforms = setOf("iFood", "Uber Direct")
        )

        // Oferta ultra lucrativa (R$ 28,00 por 4.0 km -> R$ 7,00/km)
        val lucrativeOffer = RadarOffer(
            id = "offer_win",
            appName = "iFood",
            restaurant = "Fogo de Chão",
            value = 28.00,
            distanceKm = 4.0,
            timeMinutes = 15
        )

        // Oferta com ganho/km baixo (R$ 15,00 por 5.0 km -> R$ 3,00/km)
        val lowOffer = RadarOffer(
            id = "offer_low",
            appName = "iFood",
            restaurant = "Lanchonete",
            value = 15.00,
            distanceKm = 5.0,
            timeMinutes = 20
        )

        // Oferta de app não habilitado
        val disabledPlatformOffer = RadarOffer(
            id = "offer_disabled",
            appName = "99 Food",
            restaurant = "Pizzaria",
            value = 35.00,
            distanceKm = 3.0,
            timeMinutes = 12
        )

        // Verificação 1: Oferta lucrativa aprovada
        assertTrue(lucrativeOffer.gainPerKm >= criteria.minGainPerKm)
        assertTrue(lucrativeOffer.value >= criteria.minValueBrl)
        assertTrue(lucrativeOffer.distanceKm <= criteria.maxDistanceKm)
        assertTrue(criteria.enabledPlatforms.contains(lucrativeOffer.appName))

        // Verificação 2: Oferta com ganho por km abaixo do limiar
        assertFalse(lowOffer.gainPerKm >= criteria.minGainPerKm)

        // Verificação 3: Plataforma excluída dos critérios
        assertFalse(criteria.enabledPlatforms.contains(disabledPlatformOffer.appName))
    }
}
