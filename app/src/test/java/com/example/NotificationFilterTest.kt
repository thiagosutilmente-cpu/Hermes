package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NotificationFilterTest {

    @Before
    fun setup() {
        NotificationFilterManager.resetStats()
    }

    @Test
    fun testLowPricePerKmIsBlocked() {
        val criteria = OfferFilterCriteria(
            isNotificationFilterEnabled = true,
            notificationMinGainPerKm = 5.0,
            notificationAllowMultiStackBypass = false,
            notificationOnlyJarvisApproved = false
        )

        val lowOffer = RadarOffer(
            id = "offer_1",
            appName = "iFood",
            restaurant = "Burgers",
            value = 14.0,
            distanceKm = 4.0, // 14 / 4 = 3.50 R$/km
            estimatedTimeMin = 15,
            gainPerKm = 3.50,
            neuralDecision = NeuralDecision(RadarDecision.DECLINE, "Baixo", 0.7)
        )

        assertFalse("Oferta de R$ 3,50/km deve ser bloqueada quando piso for R$ 5,00/km", criteria.matchesNotification(lowOffer))
        val evaluated = NotificationFilterManager.evaluateAndRecord(lowOffer, criteria)
        assertFalse(evaluated)
        assertEquals(1, NotificationFilterManager.blockedCount.value)
        assertEquals(0, NotificationFilterManager.allowedCount.value)
    }

    @Test
    fun testHighPricePerKmIsAllowed() {
        val criteria = OfferFilterCriteria(
            isNotificationFilterEnabled = true,
            notificationMinGainPerKm = 5.0,
            notificationAllowMultiStackBypass = false,
            notificationOnlyJarvisApproved = false
        )

        val highOffer = RadarOffer(
            id = "offer_2",
            appName = "Rappi",
            restaurant = "Churrascaria",
            value = 35.0,
            distanceKm = 5.0, // 35 / 5 = 7.00 R$/km
            estimatedTimeMin = 18,
            gainPerKm = 7.00,
            neuralDecision = NeuralDecision(RadarDecision.ACCEPT, "Excelente", 0.95)
        )

        assertTrue("Oferta de R$ 7,00/km deve ser permitida quando piso for R$ 5,00/km", criteria.matchesNotification(highOffer))
        val evaluated = NotificationFilterManager.evaluateAndRecord(highOffer, criteria)
        assertTrue(evaluated)
        assertEquals(0, NotificationFilterManager.blockedCount.value)
        assertEquals(1, NotificationFilterManager.allowedCount.value)
    }

    @Test
    fun testFilterDisabledAllowsAll() {
        val criteria = OfferFilterCriteria(
            isNotificationFilterEnabled = false,
            notificationMinGainPerKm = 8.0
        )

        val lowOffer = RadarOffer(
            id = "offer_3",
            appName = "Uber",
            restaurant = "Pizzaria",
            value = 10.0,
            distanceKm = 5.0, // 2.00 R$/km
            estimatedTimeMin = 12,
            gainPerKm = 2.00,
            neuralDecision = NeuralDecision(RadarDecision.DECLINE, "Baixo", 0.8)
        )

        assertTrue("Quando filtro estiver desativado, deve permitir todas as notificações", criteria.matchesNotification(lowOffer))
    }

    @Test
    fun testMultiStackBypassAllowsCombinedOrders() {
        val criteria = OfferFilterCriteria(
            isNotificationFilterEnabled = true,
            notificationMinGainPerKm = 6.0,
            notificationAllowMultiStackBypass = true
        )

        val multiStackOffer = RadarOffer(
            id = "offer_multi",
            appName = "iFood+Rappi",
            restaurant = "Combo Duplo",
            value = 25.0,
            distanceKm = 5.0, // 5.00 R$/km < 6.00 piso
            estimatedTimeMin = 20,
            isMultiStack = true,
            gainPerKm = 5.00,
            neuralDecision = NeuralDecision(RadarDecision.ACCEPT, "MultiStack", 0.88)
        )

        assertTrue("Multi-stack deve passar quando bypass de corridas mescladas estiver ativo", criteria.matchesNotification(multiStackOffer))
    }
}
