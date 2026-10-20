package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes unitários para validação matemática da Fórmula de Haversine e do cálculo de desvio de rotas.
 */
class HaversineGeoDeviationCalculatorTest {

    @Test
    fun testHaversineDistanceBetweenKnownCoordinates() {
        // Ponto 1: Masp (Av. Paulista, SP)
        val latMasp = -23.5615
        val lonMasp = -46.6558

        // Ponto 2: Shopping Frei Caneca (SP) - ~1.2 km em linha reta
        val latFreiCaneca = -23.5532
        val lonFreiCaneca = -46.6528

        val distanceMeters = HaversineGeoDeviationCalculator.calculateHaversineDistanceMeters(
            latMasp, lonMasp, latFreiCaneca, lonFreiCaneca
        )

        // Distância deve estar entre 800m e 1200m
        assertTrue("Distância deve ser maior que 800m", distanceMeters > 800.0)
        assertTrue("Distância deve ser menor que 1300m", distanceMeters < 1300.0)
    }

    @Test
    fun testBearingAndAngularDifference() {
        // Trajetória Norte (0°) e Trajetória Nordeste (~45°)
        val b1 = 10f
        val b2 = 40f
        val diff = HaversineGeoDeviationCalculator.calculateAngularDifference(b1, b2)
        assertEquals(30f, diff, 0.01f)

        // Trajetória quase oposta (10° e 190°)
        val diffOpposite = HaversineGeoDeviationCalculator.calculateAngularDifference(10f, 190f)
        assertEquals(180f, diffOpposite, 0.01f)
    }

    @Test
    fun testDynamicMaxDeviationIncreasesWithSpeed() {
        val lowSpeedTolerance = HaversineGeoDeviationCalculator.calculateDynamicMaxDeviationMeters(
            currentSpeedKmh = 10.0,
            slaMarginMinutes = 20
        )
        val highSpeedTolerance = HaversineGeoDeviationCalculator.calculateDynamicMaxDeviationMeters(
            currentSpeedKmh = 50.0,
            slaMarginMinutes = 20
        )

        assertTrue(
            "Velocidade maior deve conceder tolerância dinâmica maior de desvio em metros",
            highSpeedTolerance > lowSpeedTolerance
        )
    }

    @Test
    fun testMultiPointRouteDeviationEvaluation() {
        val driverLat = -23.5615
        val driverLng = -46.6558

        val o1 = ScreenPoolOffer(
            appName = "iFood",
            packageName = "com.ifood.driver",
            restaurantName = "Madero Container",
            pickupAddress = "Av. Paulista, 1200",
            dropoffAddress = "Rua Augusta, 1800",
            value = 19.50,
            distanceKm = 3.2,
            pickupLat = -23.5618,
            pickupLng = -46.6555,
            dropoffLat = -23.5530,
            dropoffLng = -46.6610
        )

        val o2 = ScreenPoolOffer(
            appName = "99Food",
            packageName = "com.didiglobal.driver",
            restaurantName = "Outback Steakhouse",
            pickupAddress = "Av. Paulista, 1000",
            dropoffAddress = "Rua Bela Cintra, 1100",
            value = 21.00,
            distanceKm = 3.4,
            pickupLat = -23.5622,
            pickupLng = -46.6550,
            dropoffLat = -23.5540,
            dropoffLng = -46.6620
        )

        val result = HaversineGeoDeviationCalculator.evaluateMultiPointRouteDeviation(
            driverLat = driverLat,
            driverLng = driverLng,
            driverSpeedKmh = 35.0,
            o1 = o1,
            o2 = o2,
            slaMinutes = 25
        )

        assertNotNull(result)
        assertTrue("Desvio líquido deve ser positivo", result.netDeviationMeters > 0)
        assertTrue("Tempo extra deve ser positivo", result.estimatedExtraMinutes > 0.0)
        // Ambas coletas e entregas são muito próximas, deve ser classificado como aceitável ou excelente
        assertTrue(
            "Combinação próxima no mesmo quadrante deve ser aceitável ou excelente",
            result.statusLevel != DeviationStatusLevel.CRITICAL_EXCESSIVE
        )
        assertTrue(result.isWithinDynamicTolerance)
    }
}
