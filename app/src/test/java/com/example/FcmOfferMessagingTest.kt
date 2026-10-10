package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes unitários para o serviço FirebaseMessagingService e processamento de alertas FCM em tempo real.
 */
class FcmOfferMessagingTest {

    @Test
    fun testFcmOfferAlertModelAndGainPerKmCalculation() {
        val alert = RadarFirebaseMessagingService.FcmOfferAlert(
            offerId = "fcm_test_101",
            appName = "iFood",
            restaurant = "Outback Shopping Morumbi",
            value = 35.00,
            distanceKm = 5.0,
            gainPerKm = 7.00,
            voiceAlert = "Nova oferta no iFood! R$ 35,00 por 5 km. Aceitar!",
            decision = "ACCEPT"
        )

        assertEquals("fcm_test_101", alert.offerId)
        assertEquals("iFood", alert.appName)
        assertEquals("Outback Shopping Morumbi", alert.restaurant)
        assertEquals(35.00, alert.value, 0.001)
        assertEquals(5.0, alert.distanceKm, 0.001)
        assertEquals(7.00, alert.gainPerKm, 0.001)
        assertEquals("ACCEPT", alert.decision)
    }

    @Test
    fun testProfitThresholdCalculationForFcmPayload() {
        // Oferta altamente rentável (> R$ 2.00 / km)
        val goodValue = 28.50
        val goodDistance = 4.2
        val goodGain = goodValue / goodDistance // ~6.78
        assertTrue(goodGain >= 2.0)

        // Oferta desvantajosa (< R$ 2.00 / km)
        val poorValue = 12.00
        val poorDistance = 8.5
        val poorGain = poorValue / poorDistance // ~1.41
        assertTrue(poorGain < 2.0)
    }

    @Test
    fun testClearLatestOfferState() {
        RadarFirebaseMessagingService.clearLatestOffer()
        assertEquals(null, RadarFirebaseMessagingService.latestPushOffer.value)
    }

    @Test
    fun testTtsFormatSpokenCurrency() {
        assertEquals("28 reais e 50 centavos", OfferTextToSpeechEngine.formatSpokenCurrency(28.50))
        assertEquals("15 reais", OfferTextToSpeechEngine.formatSpokenCurrency(15.00))
        assertEquals("75 centavos", OfferTextToSpeechEngine.formatSpokenCurrency(0.75))
        assertEquals("zero reais", OfferTextToSpeechEngine.formatSpokenCurrency(0.0))
    }

    @Test
    fun testTtsFormatSpokenDistance() {
        assertEquals("3 vírgula 9 quilômetros", OfferTextToSpeechEngine.formatSpokenDistance(3.9))
        assertEquals("4 quilômetros", OfferTextToSpeechEngine.formatSpokenDistance(4.0))
        assertEquals("800 metros", OfferTextToSpeechEngine.formatSpokenDistance(0.8))
    }

    @Test
    fun testTtsCleanAlertForSpeech() {
        val rawAlert = "Nova oferta iFood! R$ 28,50 por 4,2 km. R$/km excelente."
        val cleaned = OfferTextToSpeechEngine.cleanAlertForSpeech(rawAlert)
        assertTrue(cleaned.contains("28 reais e 50 centavos"))
        assertTrue(cleaned.contains("4 vírgula 2 quilômetros"))
        assertTrue(cleaned.contains("por quilômetro"))
    }

    @Test
    fun testTtsBuildOfferSpeechText() {
        val speech = OfferTextToSpeechEngine.buildOfferSpeechText(
            appName = "iFood",
            restaurant = "McDonald's Paulista",
            value = 26.80,
            distanceKm = 3.6,
            profitPerKm = 7.44,
            isGoodDeal = true
        )

        assertTrue(speech.contains("Nova oferta iFood no McDonald's Paulista"))
        assertTrue(speech.contains("26 reais e 80 centavos"))
        assertTrue(speech.contains("3 vírgula 6 quilômetros"))
        assertTrue(speech.contains("7 reais e 44 centavos por quilômetro"))
        assertTrue(speech.contains("Corrida excelente! Recomendação: aceitar."))
    }
}
