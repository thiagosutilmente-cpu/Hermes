package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes unitários para validação determinística do JARVIS NEURAL COCKPIT
 */
class JarvisNeuralCockpitEngineTest {

    @Test
    fun testExample1RideAnalysisAccept() {
        val input = "iFood: Nova entrega disponível! R$ 19,50 - Distância total: 4.5 km - Coleta: Burger King Centro"
        val response = JarvisNeuralCockpitEngine.processInput(input)

        assertEquals("RIDE_ANALYSIS", response.type)
        assertEquals("SUCCESS", response.status)
        assertNull(response.errorMessage)
        assertNotNull(response.rideData)

        val ride = response.rideData!!
        assertEquals("iFood", ride.platform)
        assertEquals(19.50, ride.financials.payoutBrl, 0.001)
        assertEquals(4.5, ride.financials.totalDistanceKm, 0.001)
        assertEquals(4.33, ride.financials.ratePerKm, 0.05)
        assertEquals(19, ride.financials.estimatedTimeMinutes)
        assertTrue(ride.financials.estimatedHourlyRateBrl >= 60.0)

        assertEquals("ACCEPT", ride.decision.action)
        assertTrue(ride.decision.score in 80..100)
        assertTrue(ride.decision.reason.contains("Excelente taxa"))
        assertTrue(ride.voiceAlert.contains("Corrida top parceiro!"))
    }

    @Test
    fun testExample2RideAnalysisReject() {
        val input = "99Food: Corrida recebida R$ 5,20 - Distância: 7.8 km"
        val response = JarvisNeuralCockpitEngine.processInput(input)

        assertEquals("RIDE_ANALYSIS", response.type)
        assertEquals("SUCCESS", response.status)
        assertNull(response.errorMessage)
        assertNotNull(response.rideData)

        val ride = response.rideData!!
        assertEquals("99Food", ride.platform)
        assertEquals(5.20, ride.financials.payoutBrl, 0.001)
        assertEquals(7.8, ride.financials.totalDistanceKm, 0.001)
        assertEquals(0.67, ride.financials.ratePerKm, 0.05)

        assertEquals("REJECT", ride.decision.action)
        assertTrue(ride.decision.score <= 35)
        assertTrue(ride.decision.reason.contains("Corrida lixo!"))
        assertTrue(ride.voiceAlert.contains("Fora de cogitação irmão!"))
    }

    @Test
    fun testExample3SystemErrorOnMissingData() {
        val input = "Uber Direct: Nova entrega para o endereço Rua das Flores..."
        val response = JarvisNeuralCockpitEngine.processInput(input)

        assertEquals("SYSTEM_ERROR", response.type)
        assertEquals("INVALID_DATA", response.status)
        assertNotNull(response.errorMessage)
        assertNull(response.rideData)
        assertNull(response.referralData)
    }

    @Test
    fun testExample4ReferralEventProgress() {
        val input = "[EVENTO_SISTEMA: REFERRAL_UPDATE | User: Thiago | Confirmados: 4 | AmigoPago: Marcos Silva]"
        val response = JarvisNeuralCockpitEngine.processInput(input)

        assertEquals("REFERRAL_EVENT", response.type)
        assertEquals("SUCCESS", response.status)
        assertNull(response.errorMessage)
        assertNull(response.rideData)
        assertNotNull(response.referralData)

        val ref = response.referralData!!
        assertEquals("Thiago", ref.userName)
        assertEquals(4, ref.confirmedReferrals)
        assertEquals(5, ref.targetReferrals)
        assertEquals("[▓▓▓▓░]", ref.progressBar)
        assertFalse(ref.isAnnualVipUnlocked)
        assertTrue(ref.messageText.contains("Falta só 1 motoboy"))
    }

    @Test
    fun testReferralEventGoalReached5Of5() {
        val input = "[EVENTO_SISTEMA: REFERRAL_UPDATE | User: Thiago | Confirmados: 5 | AmigoPago: Carlos Souza]"
        val response = JarvisNeuralCockpitEngine.processInput(input)

        assertEquals("REFERRAL_EVENT", response.type)
        assertEquals("SUCCESS", response.status)
        assertNotNull(response.referralData)

        val ref = response.referralData!!
        assertEquals(5, ref.confirmedReferrals)
        assertEquals("[▓▓▓▓▓]", ref.progressBar)
        assertTrue(ref.isAnnualVipUnlocked)
        assertTrue(ref.messageText.contains("PLANO ANUAL VIP de R$ 650,00 foi DESBLOQUEADO GRATUITAMENTE"))
    }

    @Test
    fun testBatchRideDetection() {
        val input = "iFood: Rota Dupla Agrupada! R$ 38,00 por 8.0 km"
        val response = JarvisNeuralCockpitEngine.processInput(input)

        assertEquals("RIDE_ANALYSIS", response.type)
        assertEquals("SUCCESS", response.status)
        assertNotNull(response.rideData)
        assertEquals("BATCH", response.rideData!!.decision.action)
    }

    @Test
    fun testJsonOutputStructureIsValid() {
        val input = "iFood: Nova entrega disponível! R$ 22,00 - Distância total: 4.0 km"
        val json = JarvisNeuralCockpitEngine.processInputToJson(input)

        assertTrue(json.contains("\"type\": \"RIDE_ANALYSIS\""))
        assertTrue(json.contains("\"status\": \"SUCCESS\""))
        assertTrue(json.contains("\"platform\": \"iFood\""))
        assertTrue(json.contains("\"payout_brl\": 22.0"))
        assertTrue(json.contains("\"rate_per_km\": 5.5"))
        assertTrue(json.contains("\"action\": \"ACCEPT\""))
    }

    @Test
    fun testRideAnalysisDataMapping() {
        val input = "iFood: Nova entrega disponível! R$ 25,00 - Distância total: 5.0 km"
        val response = JarvisNeuralCockpitEngine.processInput(input)
        assertNotNull(response.rideData)

        val rideData = response.rideData!!
        val model = RideAnalysisData.fromJarvisRide(rideData)

        assertEquals("iFood", model.platform)
        assertEquals(25.00, model.payoutBrl, 0.001)
        assertEquals(5.0, model.totalDistanceKm, 0.001)
        assertEquals(5.00, model.ratePerKm, 0.001)
        assertEquals("ACCEPT", model.decisionAction)
        assertTrue(model.decisionScore > 0)
        assertNotNull(model.voiceAlert)
        assertTrue(model.netProfitBrl > 0.0)
    }

    @Test
    fun testRideAnalysisDataFinancialCalculation() {
        val analysis = RideAnalysisData.processFinancials(
            platform = "Uber Direct",
            payout = 30.00,
            distanceKm = 6.0,
            pickupDistanceKm = 1.0
        )

        assertEquals("Uber Direct", analysis.platform)
        assertEquals(30.00, analysis.payoutBrl, 0.001)
        assertEquals(6.0, analysis.totalDistanceKm, 0.001)
        assertEquals(5.00, analysis.ratePerKm, 0.01)
        assertEquals("ACCEPT", analysis.decisionAction)
        assertTrue(analysis.isFinanciallyViable(minRatePerKm = 1.80, minHourlyRate = 30.00))

        // Custo operacional estimado: 6 km * (0.35 combustível + 0.15 desgaste) = R$ 3.00
        val netProfit = analysis.calculateNetProfit(0.35, 0.15)
        assertEquals(27.00, netProfit, 0.05)

        // Taxa líquida por km: 5.00 - 0.35 = R$ 4.65/km
        val netRate = analysis.calculateNetRatePerKm(0.35)
        assertEquals(4.65, netRate, 0.05)
    }
}
