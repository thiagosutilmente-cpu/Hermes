package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.coordinator.ActiveOffer
import com.example.coordinator.DeliveryStop
import com.example.coordinator.RadarCoordinator
import com.example.coordinator.RadarSettings
import com.example.util.GhostRouteOptimizer
import com.example.util.JarvisIntelligenceEngine
import com.example.util.MultiAppOrderManager
import com.example.util.ActiveOrder
import com.example.util.OrderStatus
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.regex.Pattern

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RadarPipelineRigorTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    // =========================================================================
    // SITUAÇÃO 1: 👁️ LEITURA NATIVA DE TELA & PARSING REGEX (ACCESSIBILITY SERVICE)
    // =========================================================================

    @Test
    fun `testScreenReadingRegex_FareExtractionFromMultipleFormats`() {
        val fareRegex = Pattern.compile("R\\$\\s*(\\d+[,.]\\d{2})")

        val sampleScreens = listOf(
            "iFood: Nova Corrida! R$ 15,00 - Restaurante Burger King" to 15.00,
            "Uber Eats: Pedido Pronto. R$28.50 - Starbucks Paulista" to 28.50,
            "Rappi: Corrida Especial R$  45,90 - Pizza Hut" to 45.90,
            "99: Corrida Flash R$ 9,20 - Padaria Real" to 9.20
        )

        for ((text, expectedFare) in sampleScreens) {
            val matcher = fareRegex.matcher(text)
            assertTrue("Deveria encontrar padrão de tarifa em: '$text'", matcher.find())
            val extractedStr = matcher.group(1)?.replace(",", ".") ?: "0"
            val fare = extractedStr.toDouble()
            assertEquals("Tarifa extraída incorreta para: '$text'", expectedFare, fare, 0.001)
        }
    }

    @Test
    fun `testScreenReadingRegex_DistanceExtractionFromMultipleFormats`() {
        val distRegex = Pattern.compile("(\\d+([.,]\\d+)?)\\s*(km|m)", Pattern.CASE_INSENSITIVE)

        val sampleDistances = listOf(
            "Distância total: 5.2 km" to 5.2,
            "Coleta a 800 m de você" to 0.8,
            "Trajeto longo: 12,4km no total" to 12.4,
            "Distância: 0.5 KM" to 0.5
        )

        for ((text, expectedKm) in sampleDistances) {
            val matcher = distRegex.matcher(text)
            assertTrue("Deveria encontrar padrão de distância em: '$text'", matcher.find())
            val number = matcher.group(1)?.replace(",", ".")?.toDouble() ?: 0.0
            val unit = matcher.group(3)?.lowercase() ?: "km"
            val distanceKm = if (unit == "m") number / 1000.0 else number
            assertEquals("Distância em km extraída incorreta para: '$text'", expectedKm, distanceKm, 0.001)
        }
    }

    @Test
    fun `testAppNameIdentification()`() {
        val packagesToTest = mapOf(
            "com.ubercab.driver" to "Uber",
            "com.99taxis.driver" to "99",
            "com.ifood.driver" to "iFood",
            "sinet.startup.inDriver" to "inDrive",
            "com.lalamove.rider.driver" to "Lalamove"
        )

        for ((pkg, expectedName) in packagesToTest) {
            val pkgLower = pkg.lowercase()
            val appName = when {
                pkgLower.contains("uber") -> "Uber"
                pkgLower.contains("99") -> "99"
                pkgLower.contains("ifood") -> "iFood"
                pkgLower.contains("indriver") -> "inDrive"
                pkgLower.contains("lalamove") -> "Lalamove"
                else -> "Outro"
            }
            assertEquals(expectedName, appName)
        }
    }

    // =========================================================================
    // SITUAÇÃO 2: 🔔 LEITOR INTELIGENTE DE NOTIFICAÇÕES (NOTIFICATION LISTENER)
    // =========================================================================

    @Test
    fun `testNotificationParsing_BackgroundPushExtractionLatencyAndAccuracy`() {
        val notificationPayload = "iFood Entregador: Nova oferta disponível! Valor R$ 18,50 | Distância: 3,2 km | Coleta: Mcdonalds Paulista"

        val startTime = System.nanoTime()

        // 1. Extract Fare
        val fareRegex = Pattern.compile("R\\$\\s*(\\d+[,.]\\d{2})")
        val fareMatcher = fareRegex.matcher(notificationPayload)
        var fareValue = 0.0
        if (fareMatcher.find()) {
            fareValue = fareMatcher.group(1)?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
        }

        // 2. Extract Distance
        val distRegex = Pattern.compile("(\\d+([.,]\\d+)?)\\s*(km|m)", Pattern.CASE_INSENSITIVE)
        val distMatcher = distRegex.matcher(notificationPayload)
        var distanceKm = 0.0
        if (distMatcher.find()) {
            val number = distMatcher.group(1)?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
            val unit = distMatcher.group(3)?.lowercase() ?: "km"
            distanceKm = if (unit == "m") number / 1000.0 else number
        }

        val elapsedMs = (System.nanoTime() - startTime) / 1_000_000.0

        assertEquals(18.50, fareValue, 0.001)
        assertEquals(3.2, distanceKm, 0.001)
        assertTrue("Parsing da notificação deve levar menos de 10ms (Levou: ${elapsedMs}ms)", elapsedMs < 10.0)
    }

    // =========================================================================
    // SITUAÇÃO 3: 🧠 ALGORITMO DE JUNÇÃO ESPACIAL (GHOST ROUTE OPTIMIZER & STACKING)
    // =========================================================================

    @Test
    fun `testGhostRouteOptimizer_RouteReorderingByTrafficAndAggressiveness`() {
        // Driver position: São Paulo Center (-23.5505, -46.6333)
        val driverLat = -23.5505
        val driverLng = -46.6333

        // 3 Stops at different distances
        val stopFar = DeliveryStop("1", "Far Stop", -23.5900, -46.6800, "iFood")
        val stopClose = DeliveryStop("2", "Close Stop", -23.5520, -46.6350, "Rappi")
        val stopMedium = DeliveryStop("3", "Medium Stop", -23.5700, -46.6500, "Uber")

        val unoptimized = listOf(stopFar, stopClose, stopMedium)

        val optimized = GhostRouteOptimizer.optimize(
            currentLat = driverLat,
            currentLng = driverLng,
            stops = unoptimized,
            trafficFactor = 0.5f,
            aggressiveness = "EQUILIBRADO",
            trafficWeight = 0.5,
            latencyWeight = 0.3
        )

        assertEquals("Próxima parada deve ser a mais próxima", "2", optimized[0].id)
        assertEquals("Segunda parada deve ser a de média distância", "3", optimized[1].id)
        assertEquals("Última parada deve ser a mais distante", "1", optimized[2].id)
    }

    @Test
    fun `testSpatialStacking_MultiAppRouteConvergenceAndCombinedGain`() {
        // Active Order: iFood (R$ 15.00)
        val activeOrder = ActiveOrder(
            id = "order_ifood_1",
            appName = "iFood",
            fare = 15.00,
            pickupAddress = "Burger King, Av. Paulista",
            deliveryAddress = "Rua Consolação, 1000",
            pickupLat = -23.5550,
            pickupLng = -46.6600,
            deliveryLat = -23.5500,
            deliveryLng = -46.6500,
            status = OrderStatus.PICKING_UP
        )

        // Incoming Secondary Offer: Rappi (R$ 18.00) in same direction (< 2.5km detour)
        val candidateOffer = ActiveOffer(
            appName = "Rappi",
            fareValue = 18.00,
            pickupAddress = "Pizza Hut, Av. Paulista",
            deliveryAddress = "Rua Consolação, 1200",
            totalDistance = 1.2, // Detour distance = +1.2 km
            totalTime = 8.0
        )

        // Set active delivery in settings
        val settings = RadarSettings(
            smartSequenceEnabled = true,
            isActiveDeliveryEnabled = true,
            activeDeliveryDestination = activeOrder.deliveryAddress
        )

        val isAcceptable = JarvisIntelligenceEngine.analyzeOfferDecision(context, candidateOffer, settings)

        assertTrue("Oferta convergente da Rappi deve ser aceita para Stack Multi-App", isAcceptable)

        // Combined Gain Calculation Test
        val totalCombinedFare = activeOrder.fare + candidateOffer.fareValue
        val combinedDistanceKm = 3.0 + candidateOffer.totalDistance // 4.2 km total
        val combinedGainPerKm = totalCombinedFare / combinedDistanceKm

        assertEquals(33.00, totalCombinedFare, 0.001)
        assertEquals(7.857, combinedGainPerKm, 0.01)
    }

    @Test
    fun `testSpatialStacking_RejectionWhenDetourCausesExcessiveDelay`() {
        // Active Order
        val activeOrderAddress = "Rua Consolação, 1000"

        // Incoming Candidate with excessive detour distance (> 4.5 km detour)
        val candidateOfferFar = ActiveOffer(
            appName = "Uber Eats",
            fareValue = 12.00,
            pickupAddress = "Far Mall",
            deliveryAddress = "Rua Consolação, 1000",
            totalDistance = 6.5, // 6.5 km detour
            totalTime = 25.0
        )

        val settings = RadarSettings(
            smartSequenceEnabled = true,
            isActiveDeliveryEnabled = true,
            activeDeliveryDestination = activeOrderAddress,
            minFareValue = 5.00,
            maxTotalDistanceKm = 10.0
        )

        val isAcceptable = JarvisIntelligenceEngine.analyzeOfferDecision(context, candidateOfferFar, settings)

        assertFalse("Oferta com desvio excessivo deve ser REJEITADA para evitar atraso", isAcceptable)
    }

    // =========================================================================
    // SITUAÇÃO 4: ⚡ EXECUÇÃO AUTOMÁTICA NO HUD, VOZ (JARVIS) & DECISÃO SUB-300MS
    // =========================================================================

    @Test
    fun `testJarvisAutoAcceptDecisionPerformance_Under300ms()`() {
        val settings = RadarSettings(
            minFareValue = 8.00,
            maxTotalDistanceKm = 10.0,
            minValuePerKm = 2.0,
            preferredReturnNeighborhoods = "Rebouças"
        )

        val validOffer = ActiveOffer(
            appName = "iFood",
            fareValue = 20.00,
            pickupAddress = "Mcdonalds",
            deliveryAddress = "Av. Rebouças, 500",
            totalDistance = 3.0,
            totalTime = 12.0
        )

        val startTime = System.nanoTime()
        val decision = JarvisIntelligenceEngine.analyzeOfferDecision(context, validOffer, settings)
        val elapsedMs = (System.nanoTime() - startTime) / 1_000_000.0

        assertTrue("Oferta de R$15/3km deve ser aceita pelo Jarvis", decision)
        assertTrue("Decisão do Jarvis deve rodar em menos de 300ms (Executou em ${elapsedMs}ms)", elapsedMs < 300.0)
    }

    @Test
    fun `testJarvisAutoReject_MinFareAndDistanceFilters()`() {
        val settings = RadarSettings(
            minFareValue = 10.00,
            maxTotalDistanceKm = 5.0,
            minValuePerKm = 2.5
        )

        // Offer below min fare (R$ 7.00 < R$ 10.00)
        val lowFareOffer = ActiveOffer(
            appName = "99",
            fareValue = 7.00,
            pickupAddress = "Padaria",
            deliveryAddress = "Rua A",
            totalDistance = 2.0,
            totalTime = 8.0
        )
        assertFalse("Oferta abaixo da tarifa mínima deve ser recusada",
            JarvisIntelligenceEngine.analyzeOfferDecision(context, lowFareOffer, settings))

        // Offer above max distance (8.0 km > 5.0 km)
        val longDistanceOffer = ActiveOffer(
            appName = "iFood",
            fareValue = 25.00,
            pickupAddress = "Restaurante",
            deliveryAddress = "Rua B",
            totalDistance = 8.0,
            totalTime = 30.0
        )
        assertFalse("Oferta acima da distância máxima deve ser recusada",
            JarvisIntelligenceEngine.analyzeOfferDecision(context, longDistanceOffer, settings))
    }

    @Test
    fun `testJarvisNaturalLanguageVoiceCommands()`() {
        // Test Accept Voice Intent
        val acceptCmd = JarvisIntelligenceEngine.processNaturalLanguageCommand("Jarvis aceita essa corrida pra mim", "IDLE")
        assertEquals(JarvisIntelligenceEngine.ActionType.ACCEPT_OFFER, acceptCmd.actionType)

        // Test Reject Voice Intent
        val rejectCmd = JarvisIntelligenceEngine.processNaturalLanguageCommand("Recusa essa oferta agora", "IDLE")
        assertEquals(JarvisIntelligenceEngine.ActionType.REJECT_OFFER, rejectCmd.actionType)

        // Test Negative Guardrail ("não aceita essa")
        val negativeCmd = JarvisIntelligenceEngine.processNaturalLanguageCommand("Não aceita essa corrida jamais", "IDLE")
        assertEquals(JarvisIntelligenceEngine.ActionType.REJECT_OFFER, negativeCmd.actionType)

        // Test Settings Update Command
        val settingsCmd = JarvisIntelligenceEngine.processNaturalLanguageCommand("Altera o valor mínimo para 15 reais", "IDLE")
        assertEquals(JarvisIntelligenceEngine.ActionType.UPDATE_SETTINGS, settingsCmd.actionType)
        assertEquals(15.0, settingsCmd.updatePayload?.get("minFare"))
    }
}
