package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes unitários cobrindo o motor de filtragem de ofertas por Geolocalização e Valor Mínimo
 * para o Radar Delivery AI.
 */
class GeoPriceFilterTest {

    // Coordenadas de referência do piloto: Av. Paulista, SP
    private val pilotLat = -23.561684
    private val pilotLng = -46.655981

    @Test
    fun testHaversineDistanceCalculation() {
        val paulistaOffer = DeliveryOffer(
            id = "t1",
            nomeRestaurante = "Burger King Paulista",
            valor = 30.0,
            distancia = 3.0,
            tempoEstimado = 15,
            latitude = -23.561684,
            longitude = -46.655981
        )
        // Mesmas coordenadas devem resultar em distância 0.0
        assertEquals(0.0, paulistaOffer.distanceTo(pilotLat, pilotLng), 0.01)

        val freiCanecaOffer = DeliveryOffer(
            id = "t2",
            nomeRestaurante = "Starbucks Frei Caneca",
            valor = 22.0,
            distancia = 2.5,
            tempoEstimado = 12,
            latitude = -23.553200,
            longitude = -46.652800
        )
        // Distância calculada entre Paulista e Frei Caneca ~0.99 km
        val distanceKm = freiCanecaOffer.distanceTo(pilotLat, pilotLng)
        assertTrue("Distância deve estar entre 0.8 e 1.2 km", distanceKm in 0.8..1.2)
    }

    @Test
    fun testMinimumPriceFilterExcludesLowValueOffers() {
        val criteria = GeoPriceFilterCriteria(
            minOrderValue = 25.0,
            driverLatitude = pilotLat,
            driverLongitude = pilotLng,
            isGeoRadiusFilterActive = false
        )

        val lowOffer = DeliveryOffer(
            id = "low",
            nomeRestaurante = "Lanchonete",
            valor = 18.0,
            distancia = 2.0,
            tempoEstimado = 10
        )
        val highOffer = DeliveryOffer(
            id = "high",
            nomeRestaurante = "Pizzaria Gourmet",
            valor = 35.0,
            distancia = 3.5,
            tempoEstimado = 18
        )

        val lowResult = GeoPriceFilterEngine.evaluateOffer(lowOffer, criteria)
        assertFalse("Oferta de R$ 18,00 deve ser rejeitada com piso de R$ 25,00", lowResult.isAccepted)
        assertTrue(lowResult.rejectionReason?.contains("R$ 25,00") == true)

        val highResult = GeoPriceFilterEngine.evaluateOffer(highOffer, criteria)
        assertTrue("Oferta de R$ 35,00 deve ser aceita com piso de R$ 25,00", highResult.isAccepted)
    }

    @Test
    fun testGeoRadiusFilterExcludesDistantPickups() {
        val criteria = GeoPriceFilterCriteria(
            minOrderValue = 0.0,
            driverLatitude = pilotLat,
            driverLongitude = pilotLng,
            isGeoRadiusFilterActive = true,
            maxPickupRadiusKm = 4.0
        )

        val nearbyOffer = DeliveryOffer(
            id = "near",
            nomeRestaurante = "Pizzaria Jardins",
            valor = 20.0,
            distancia = 2.0,
            tempoEstimado = 12,
            latitude = -23.568210,
            longitude = -46.662150 // ~0.97 km de distância
        )

        val distantOffer = DeliveryOffer(
            id = "far",
            nomeRestaurante = "Restaurante Anália Franco",
            valor = 40.0,
            distancia = 12.0,
            tempoEstimado = 40,
            latitude = -23.548200,
            longitude = -46.562100 // ~9.8 km de distância
        )

        val nearResult = GeoPriceFilterEngine.evaluateOffer(nearbyOffer, criteria)
        assertTrue("Oferta a ~1 km deve ser aceita com raio de 4 km", nearResult.isAccepted)

        val distantResult = GeoPriceFilterEngine.evaluateOffer(distantOffer, criteria)
        assertFalse("Oferta a ~9.8 km deve ser rejeitada com raio de 4 km", distantResult.isAccepted)
        assertTrue(distantResult.rejectionReason?.contains("km") == true)
    }

    @Test
    fun testCombinedGeolocationAndMinimumPriceFilter() {
        val criteria = GeoPriceFilterCriteria(
            minOrderValue = 25.0,
            driverLatitude = pilotLat,
            driverLongitude = pilotLng,
            isGeoRadiusFilterActive = true,
            maxPickupRadiusKm = 5.0
        )

        val offerPassesBoth = DeliveryOffer(
            id = "pass",
            nomeRestaurante = "Burger King Paulista",
            valor = 32.0, // >= 25.0
            distancia = 3.0,
            tempoEstimado = 15,
            latitude = -23.561684,
            longitude = -46.655981 // 0.0 km <= 5.0 km
        )

        val offerFailsPriceOnly = DeliveryOffer(
            id = "fail_price",
            nomeRestaurante = "Café Paulista",
            valor = 14.0, // < 25.0
            distancia = 1.0,
            tempoEstimado = 5,
            latitude = -23.561684,
            longitude = -46.655981
        )

        val offerFailsDistanceOnly = DeliveryOffer(
            id = "fail_dist",
            nomeRestaurante = "Churrascaria Distante",
            valor = 65.0, // >= 25.0
            distancia = 15.0,
            tempoEstimado = 45,
            latitude = -23.500000,
            longitude = -46.600000 // > 5.0 km
        )

        assertTrue(GeoPriceFilterEngine.evaluateOffer(offerPassesBoth, criteria).isAccepted)
        assertFalse(GeoPriceFilterEngine.evaluateOffer(offerFailsPriceOnly, criteria).isAccepted)
        assertFalse(GeoPriceFilterEngine.evaluateOffer(offerFailsDistanceOnly, criteria).isAccepted)
    }

    @Test
    fun testOfferFilterCriteriaWithRadarOfferGeolocation() {
        val criteria = OfferFilterCriteria(
            minValue = 20.0,
            isGeoRadiusFilterActive = true,
            maxPickupRadiusKm = 3.0,
            driverCurrentLat = pilotLat,
            driverCurrentLng = pilotLng
        )

        val closeHighOffer = RadarOffer(
            id = "ro_close_high",
            appName = "iFood",
            restaurant = "Outback",
            value = 35.0,
            distanceKm = 4.0,
            estimatedTimeMin = 18,
            pickupLat = -23.563000,
            pickupLng = -46.654000 // ~0.25 km
        )

        val farHighOffer = RadarOffer(
            id = "ro_far_high",
            appName = "Rappi",
            restaurant = "Coco Bambu",
            value = 50.0,
            distanceKm = 10.0,
            estimatedTimeMin = 30,
            pickupLat = -23.510000,
            pickupLng = -46.620000 // ~6.8 km
        )

        val closeLowOffer = RadarOffer(
            id = "ro_close_low",
            appName = "99Food",
            restaurant = "Lanche Rápido",
            value = 12.0, // < 20.0
            distanceKm = 2.0,
            estimatedTimeMin = 10,
            pickupLat = -23.563000,
            pickupLng = -46.654000
        )

        assertTrue("Oferta próxima com valor alto deve ser aceita", criteria.matches(closeHighOffer))
        assertFalse("Oferta distante deve ser rejeitada pelo filtro de raio", criteria.matches(farHighOffer))
        assertFalse("Oferta de baixo valor deve ser rejeitada pelo piso de valor", criteria.matches(closeLowOffer))
    }
}
