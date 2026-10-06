package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes unitários para o motor de cálculo em tempo real de Lucro por KM e Combustível (RealTimeProfitEngine).
 */
class RealTimeProfitEngineTest {

    @Test
    fun testHighProfitThresholdClassification() {
        // Corrida de 4 km por R$ 30,00 com gasolina a R$ 6,00/l e 30 km/l
        // Custo de gasolina por km = 6.00 / 30.0 = R$ 0,20/km
        // Custo total de gasolina = 4 * 0.20 = R$ 0,80
        // Tarifa bruta por km = 30.00 / 4.0 = R$ 7,50/km
        // Lucro líquido = 30.00 - 0.80 - (4 * 0.12) = R$ 28,72
        // Lucro líq/km = 28.72 / 4 = R$ 7,18/km
        val metrics = RealTimeProfitEngine.calculateTripMetrics(
            fareValue = 30.00,
            distanceKm = 4.0,
            customFuelPrice = 6.00,
            customKmPerLiter = 30.0
        )

        assertEquals(7.50, metrics.grossPricePerKm, 0.01)
        assertEquals(0.80, metrics.fuelCostTotal, 0.01)
        assertEquals(0.20, metrics.fuelCostPerKm, 0.01)
        assertTrue("Lucro líquido deve ser superior a R$ 6,00/km", metrics.netProfitPerKm > 6.00)
        assertEquals(ProfitThresholdLevel.HIGH_PROFIT, metrics.thresholdLevel)
        assertEquals("#00FF88", metrics.thresholdLevel.colorHex)
        assertTrue(metrics.thresholdLabel.contains("🟢"))
    }

    @Test
    fun testLowProfitDeficitClassification() {
        // Corrida de 5 km por R$ 8,00 com gasolina a R$ 6,00/l e 30 km/l
        // Tarifa bruta = 8.00 / 5.0 = R$ 1,60/km (Abaixo do piso)
        val metrics = RealTimeProfitEngine.calculateTripMetrics(
            fareValue = 8.00,
            distanceKm = 5.0,
            customFuelPrice = 6.00,
            customKmPerLiter = 30.0
        )

        assertEquals(1.60, metrics.grossPricePerKm, 0.01)
        assertEquals(ProfitThresholdLevel.LOW_PROFIT, metrics.thresholdLevel)
        assertEquals("#FF3366", metrics.thresholdLevel.colorHex)
        assertTrue(metrics.thresholdLabel.contains("🔴 PREJUÍZO"))
    }

    @Test
    fun testMediumProfitClassification() {
        // Corrida de 4 km por R$ 17,00 (R$ 4,25/km bruto)
        val metrics = RealTimeProfitEngine.calculateTripMetrics(
            fareValue = 17.00,
            distanceKm = 4.0,
            customFuelPrice = 5.89,
            customKmPerLiter = 35.0
        )

        assertEquals(4.25, metrics.grossPricePerKm, 0.01)
        assertEquals(ProfitThresholdLevel.MEDIUM_PROFIT, metrics.thresholdLevel)
        assertEquals("#FFB800", metrics.thresholdLevel.colorHex)
        assertTrue(metrics.thresholdLabel.contains("🟡 MÉDIA"))
    }
}
