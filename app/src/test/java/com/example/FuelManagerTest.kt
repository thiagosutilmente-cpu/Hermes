package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes unitários para a classe [FuelManager].
 * Valida o cálculo de consumo no ciclo urbano e a alternância dinâmica entre modo seco e modo chuva.
 */
class FuelManagerTest {

    @Test
    fun testDryModeUrbanConsumptionNominal() {
        // Honda CG 160 no modo seco deve retornar 41.0 km/l
        val fuelManager = FuelManager(initialModel = FuelManager.HONDA_CG_160, initialMode = WeatherMode.DRY)

        assertEquals(WeatherMode.DRY, fuelManager.weatherMode.value)
        assertFalse(fuelManager.isRainModeActive)
        assertEquals(41.0, fuelManager.calculateUrbanConsumptionKmPerLiter(), 0.01)
    }

    @Test
    fun testRainModeAerodynamicAndTrafficReduction() {
        // Na chuva, Honda CG 160 tem perda por arrasto aerodinâmico e tráfego
        // 41.0 * (1.0 - 0.18) = 33.62 -> ~33.6 km/l
        val fuelManager = FuelManager(initialModel = FuelManager.HONDA_CG_160, initialMode = WeatherMode.RAIN)

        assertTrue(fuelManager.isRainModeActive)
        val rainConsumption = fuelManager.calculateUrbanConsumptionKmPerLiter()
        assertTrue("Consumo na chuva deve ser menor que 41.0 km/l", rainConsumption < 41.0)
        assertTrue("Consumo na chuva para CG 160 deve estar em torno de 33-34 km/l", rainConsumption in 32.0..35.0)
    }

    @Test
    fun testToggleWeatherMode() {
        val fuelManager = FuelManager(initialModel = FuelManager.HONDA_CG_160, initialMode = WeatherMode.DRY)

        assertEquals(41.0, fuelManager.calculateUrbanConsumptionKmPerLiter(), 0.01)

        val newMode = fuelManager.toggleWeatherMode()
        assertEquals(WeatherMode.RAIN, newMode)
        assertTrue(fuelManager.isRainModeActive)

        val rainKmPerL = fuelManager.calculateUrbanConsumptionKmPerLiter()
        assertTrue(rainKmPerL < 41.0)

        // Alterna de volta para modo seco
        val backToDry = fuelManager.toggleWeatherMode()
        assertEquals(WeatherMode.DRY, backToDry)
        assertEquals(41.0, fuelManager.calculateUrbanConsumptionKmPerLiter(), 0.01)
    }

    @Test
    fun testLitersConsumedAndFuelCost() {
        val fuelManager = FuelManager(
            initialModel = FuelManager.HONDA_CG_160,
            initialMode = WeatherMode.DRY,
            initialFuelPrice = 6.00
        )

        // Corrida de 82 km com média 41 km/l = exatamente 2.0 litros
        val liters = fuelManager.calculateConsumptionLiters(82.0)
        assertEquals(2.0, liters, 0.01)

        // Custo: 2 litros * R$ 6,00 = R$ 12,00
        val cost = fuelManager.calculateFuelCost(82.0)
        assertEquals(12.00, cost, 0.01)

        // Custo por km = 6.00 / 41.0 = ~R$ 0,15/km
        val costPerKm = fuelManager.calculateCostPerKm()
        assertEquals(0.15, costPerKm, 0.01)
    }

    @Test
    fun testDifferentMotorcyclePresets() {
        val popManager = FuelManager(initialModel = FuelManager.HONDA_POP_110)
        assertEquals(55.0, popManager.calculateUrbanConsumptionKmPerLiter(), 0.01)

        val bizManager = FuelManager(initialModel = FuelManager.HONDA_BIZ_125)
        assertEquals(45.0, bizManager.calculateUrbanConsumptionKmPerLiter(), 0.01)

        val scooterManager = FuelManager(initialModel = FuelManager.SCOOTER_PCX_NMAX)
        assertEquals(38.0, scooterManager.calculateUrbanConsumptionKmPerLiter(), 0.01)
    }

    @Test
    fun testFindModelByNameOrId() {
        val cg = FuelManager.findModelByNameOrId("cg160")
        assertEquals("cg160", cg.id)
        assertEquals(41.0, cg.baseUrbanConsumptionKmPerL, 0.01)

        val biz = FuelManager.findModelByNameOrId("Biz 125")
        assertEquals("biz125", biz.id)

        val factor = FuelManager.findModelByNameOrId("Factor 150")
        assertEquals("factor150", factor.id)
    }
}
