package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes unitários para as estruturas de dados do MarketingDashboard
 */
class MarketingDashboardTest {

    @Test
    fun testCampaignDataPointCalculations() {
        val point = CampaignDataPoint(
            label = "Sábado",
            profitBrl = 340.00,
            distanceKm = 62.0,
            conversionRatePercent = 12.8,
            conversionsCount = 14
        )

        assertEquals("Sábado", point.label)
        assertEquals(340.00, point.profitBrl, 0.001)
        assertEquals(62.0, point.distanceKm, 0.001)
        assertEquals(12.8, point.conversionRatePercent, 0.001)
        assertEquals(14, point.conversionsCount)

        // Taxa calculada de R$/KM
        val expectedRate = 340.00 / 62.0
        assertEquals(expectedRate, point.ratePerKm, 0.01)
        assertTrue("Deve ser uma corrida altamente rentável (> R$ 5,00/km)", point.ratePerKm > 5.0)
    }

    @Test
    fun testCampaignDataPointZeroKmSafety() {
        val zeroKmPoint = CampaignDataPoint(
            label = "Zero",
            profitBrl = 10.0,
            distanceKm = 0.0,
            conversionRatePercent = 0.0,
            conversionsCount = 0
        )

        assertEquals(0.0, zeroKmPoint.ratePerKm, 0.001)
    }
}
