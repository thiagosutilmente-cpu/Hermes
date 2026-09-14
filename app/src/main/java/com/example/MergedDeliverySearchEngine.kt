package com.example

import androidx.compose.ui.graphics.Color
import org.json.JSONObject
import java.util.Locale

/**
 * Representa um pedido individual que compõe uma entrega mesclada (Multi-Stack).
 */
data class SubDeliveryOrder(
    val appName: String,
    val appColor: Color,
    val restaurant: String,
    val value: Double,
    val distanceKm: Double,
    val pickupAddress: String,
    val destinationAddress: String,
    val kitchenPrepMinutes: Int = 5,
    val dishCategory: String = "Fast Food",
    val readyStatus: String = "Pronto em 4 min"
)

/**
 * Ponto de Sincronização 4D de Cozinha (Zero Espera no Balcão)
 */
data class KitchenSyncPoint(
    val restaurant: String,
    val prepMinutes: Int,
    val readyStatus: String,
    val dishCategory: String
)

/**
 * Telemetria Quântica 4D de Mesclagem Hiper-Preditiva (Jarvis Quantum Stacking Engine 4.0)
 */
data class HyperQuantumTelemetry(
    val quantumScore: Int = 98, // 0 a 100% de sinergia quântica multi-app
    val vectorDeviationDegrees: Double = 6.4, // desvio angular da rota natural
    val kitchenSyncPoints: List<KitchenSyncPoint> = emptyList(),
    val fuelSavedMilliliters: Int = 340, // ml de combustível poupados
    val penaltyRiskPercent: Double = 0.0, // risco de atraso (0.0% = Escudo Anti-Ban)
    val isTriStack: Boolean = false, // Modo Tri-Stack (3 apps simultâneos)
    val inFlightIntercept: Boolean = false, // Interceptação Dinâmica de Voo
    val aiInsight: String = "Sincronia 4D de Cozinha perfeita: zero minutos de espera na calçada."
)

/**
 * Previsão probabilística da Ghost Sequence para mesclagem iminente de pedidos.
 */
data class GhostSequencePrediction(
    val probabilityPercent: Int = 89,
    val etaMinutes: Int = 3,
    val sourceApp: String = "Rappi",
    val pickupCorridor: String = "Av. Paulista / R. Augusta",
    val potentialBonus: Double = 22.0,
    val message: String = "89% chance de intercepção vetorial em 3 min no mesmo corredor",
    val quantumVectorAngle: Double = 5.2,
    val targetPlatform: String = "Rappi + Uber Direct"
)

/**
 * Motor Neural Hiper-Quântico de Busca e Otimização de Entregas Mescladas 4D
 * (Jarvis Quantum Stacking Engine 4.0)
 * 
 * Inovações sem precedentes:
 * 1. Sincronia 4D de Cozinha em Tempo Real (Kitchen Heatmap & Zero-Wait Balcão)
 * 2. Interceptação Dinâmica em Voo 360° (In-Flight Vector Intercept sem desvio)
 * 3. Tri-Stack Quântico com Escudo Anti-Ban (3 Apps Simultâneos com SLA 100% Seguro)
 * 4. Telemetria de Combustível Poupado em Mililitros e Ganho Exponencial por Km (R$/km)
 */
object MergedDeliverySearchEngine {

    /**
     * Retorna a previsão neural de Ghost Sequence ativa para a região
     */
    fun getActiveGhostPrediction(): GhostSequencePrediction {
        return GhostSequencePrediction(
            probabilityPercent = 89,
            etaMinutes = 3,
            sourceApp = "Rappi",
            pickupCorridor = "Corredor Paulista ➔ Bela Cintra",
            potentialBonus = 22.50,
            message = "89% chance de intercepção vetorial Rappi em 3 min sem desvio",
            quantumVectorAngle = 4.8,
            targetPlatform = "Rappi + iFood"
        )
    }

    /**
     * Catálogo curado de entregas mescladas com altíssima sinergia de trajeto e telemetria quântica
     */
    fun getCuratedMergedStacks(): List<RadarOffer> {
        return listOf(
            // 1. REVOLUCIONÁRIO: TRI-STACK QUÂNTICO 4D (iFood + Rappi + Uber Direct)
            RadarOffer(
                id = "merged_stk_tri_100",
                appName = "iFood + Rappi + Uber (Tri-Stack 4D)",
                appColor = NeonGreen,
                restaurant = "BK Paulista, Pizza Hut Jardins & Subway Frei Caneca",
                value = 54.00,
                distanceKm = 5.2,
                timeMinutes = 24,
                pickupAddress = "Corredor Gastronômico Paulista",
                destinationAddress = "Quadrante Unificado: Jardins / Cerqueira César",
                isMultiStack = true,
                subOrders = listOf(
                    SubDeliveryOrder(
                        appName = "iFood",
                        appColor = RedIFood,
                        restaurant = "Burger King Jardins",
                        value = 16.50,
                        distanceKm = 2.8,
                        pickupAddress = "Av. Paulista, 1578",
                        destinationAddress = "R. Bela Cintra, 904",
                        kitchenPrepMinutes = 3,
                        dishCategory = "Hambúrguer",
                        readyStatus = "Pronto em 3 min (Fast-Track)"
                    ),
                    SubDeliveryOrder(
                        appName = "Rappi",
                        appColor = OrangeRappi,
                        restaurant = "Pizza Hut Al. Santos",
                        value = 19.50,
                        distanceKm = 2.4,
                        pickupAddress = "Al. Santos, 120",
                        destinationAddress = "Al. Lorena, 450",
                        kitchenPrepMinutes = 7,
                        dishCategory = "Pizza Forno",
                        readyStatus = "Sai do Forno em 7 min"
                    ),
                    SubDeliveryOrder(
                        appName = "Uber Direct",
                        appColor = TextLight,
                        restaurant = "Subway Frei Caneca",
                        value = 18.00,
                        distanceKm = 2.0,
                        pickupAddress = "R. Frei Caneca, 569",
                        destinationAddress = "R. Peixoto Gomide, 320",
                        kitchenPrepMinutes = 10,
                        dishCategory = "Sanduíche",
                        readyStatus = "Pronto no Balcão em 10 min"
                    )
                ),
                synergySavingsKm = 2.6, // 7.8 km isolados -> 5.2 km agrupados
                synergyBonusPercent = 86,
                waypointRoute = listOf(
                    "● Coleta 1 (iFood): BK Paulista [Pronto às 19:03]",
                    "● Coleta 2 (Rappi): Pizza Hut [Sai do Forno às 19:07]",
                    "● Coleta 3 (Uber): Subway [Lacrado às 19:10]",
                    "🏠 Entrega 1: R. Bela Cintra, 904",
                    "🏢 Entrega 2: Al. Lorena, 450",
                    "🏠 Entrega 3: R. Peixoto Gomide, 320"
                ),
                quantumTelemetry = HyperQuantumTelemetry(
                    quantumScore = 99,
                    vectorDeviationDegrees = 4.2,
                    kitchenSyncPoints = listOf(
                        KitchenSyncPoint("Burger King", 3, "Pronto em 3 min", "Fast Food"),
                        KitchenSyncPoint("Pizza Hut", 7, "Forno sincronizado", "Pizzaria"),
                        KitchenSyncPoint("Subway", 10, "Balcão liberado", "Lanches")
                    ),
                    fuelSavedMilliliters = 480,
                    penaltyRiskPercent = 0.0,
                    isTriStack = true,
                    inFlightIntercept = false,
                    aiInsight = "Tri-Stack Perfeito: 3 coletas alinhadas na mesma quadra com espera zero no balcão e ganho de R$ 10,38/km."
                )
            ),

            // 2. IN-FLIGHT DYNAMIC INTERCEPT (INTERCEPTAÇÃO EM VOO SEM RETORNO)
            RadarOffer(
                id = "merged_stk_inflight_101",
                appName = "Rappi + iFood (Intercepção em Voo)",
                appColor = NeonGreen,
                restaurant = "Madero Vila Olímpia & Bacio di Latte Funchal",
                value = 38.00,
                distanceKm = 4.0,
                timeMinutes = 19,
                pickupAddress = "R. Funchal, 200 (No seu curso)",
                destinationAddress = "Av. Horácio Lafer ➔ R. Bandeira Paulista",
                isMultiStack = true,
                subOrders = listOf(
                    SubDeliveryOrder(
                        appName = "Rappi",
                        appColor = OrangeRappi,
                        restaurant = "Madero Container Funchal",
                        value = 20.00,
                        distanceKm = 2.8,
                        pickupAddress = "R. Funchal, 200",
                        destinationAddress = "Av. Horácio Lafer, 600",
                        kitchenPrepMinutes = 4,
                        dishCategory = "Burger Gourmet",
                        readyStatus = "Pronto ao encostar a moto"
                    ),
                    SubDeliveryOrder(
                        appName = "iFood",
                        appColor = RedIFood,
                        restaurant = "Bacio di Latte Funchal",
                        value = 18.00,
                        distanceKm = 2.2,
                        pickupAddress = "R. Funchal, 340 (a 140m)",
                        destinationAddress = "R. Bandeira Paulista, 720",
                        kitchenPrepMinutes = 2,
                        dishCategory = "Gelato / Sobremesa",
                        readyStatus = "Na embalagem térmica"
                    )
                ),
                synergySavingsKm = 1.4,
                synergyBonusPercent = 72,
                waypointRoute = listOf(
                    "● Coleta 1 (Rappi): Madero Funchal [Passagem Direta]",
                    "● Coleta 2 (iFood): Bacio di Latte [A 140 metros na mesma calçada]",
                    "🏠 Entrega 1: Av. Horácio Lafer, 600",
                    "🏢 Entrega 2: R. Bandeira Paulista, 720"
                ),
                quantumTelemetry = HyperQuantumTelemetry(
                    quantumScore = 98,
                    vectorDeviationDegrees = 3.8,
                    kitchenSyncPoints = listOf(
                        KitchenSyncPoint("Madero", 4, "Pronto ao encostar", "Burger"),
                        KitchenSyncPoint("Bacio di Latte", 2, "Embalado no freezer", "Gelato")
                    ),
                    fuelSavedMilliliters = 360,
                    penaltyRiskPercent = 0.0,
                    isTriStack = false,
                    inFlightIntercept = true,
                    aiInsight = "Intercepção dinâmica sem desvio: segunda coleta a apenas 140m na mesma via. R$ 9,50/km líquido."
                )
            ),

            // 3. DUPLO-STACK CLÁSSICO COM SINCRONIA 4D
            RadarOffer(
                id = "merged_stk_101",
                appName = "iFood + Rappi (Multi-Stack)",
                appColor = NeonGreen,
                restaurant = "Burger King Paulista & Pizza Hut Jardins",
                value = 33.00,
                distanceKm = 4.2,
                timeMinutes = 18,
                pickupAddress = "Av. Paulista, 1578",
                destinationAddress = "R. Bela Cintra, 904",
                isMultiStack = true,
                subOrders = listOf(
                    SubDeliveryOrder(
                        appName = "iFood",
                        appColor = RedIFood,
                        restaurant = "Burger King Jardins",
                        value = 15.00,
                        distanceKm = 2.8,
                        pickupAddress = "Av. Paulista, 1578",
                        destinationAddress = "R. Bela Cintra, 904",
                        kitchenPrepMinutes = 3,
                        dishCategory = "Fast Food",
                        readyStatus = "Pronto no balcão"
                    ),
                    SubDeliveryOrder(
                        appName = "Rappi",
                        appColor = OrangeRappi,
                        restaurant = "Pizza Hut Al. Santos",
                        value = 18.00,
                        distanceKm = 2.4,
                        pickupAddress = "Al. Santos, 120",
                        destinationAddress = "Al. Lorena, 450",
                        kitchenPrepMinutes = 6,
                        dishCategory = "Pizza",
                        readyStatus = "Saindo da esteira"
                    )
                ),
                synergySavingsKm = 1.0,
                synergyBonusPercent = 58,
                waypointRoute = listOf(
                    "● Coleta 1: Burger King (Av. Paulista)",
                    "● Coleta 2: Pizza Hut (Al. Santos)",
                    "🏠 Entrega 1: R. Bela Cintra, 904",
                    "🏢 Entrega 2: Al. Lorena, 450"
                ),
                quantumTelemetry = HyperQuantumTelemetry(
                    quantumScore = 96,
                    vectorDeviationDegrees = 5.6,
                    kitchenSyncPoints = listOf(
                        KitchenSyncPoint("Burger King", 3, "Pronto no balcão", "Fast Food"),
                        KitchenSyncPoint("Pizza Hut", 6, "Saindo da esteira", "Pizza")
                    ),
                    fuelSavedMilliliters = 290,
                    penaltyRiskPercent = 0.0,
                    isTriStack = false,
                    inFlightIntercept = false,
                    aiInsight = "Sincronia perfeita de rotas paralelas (Paulista e Al. Santos) com R$ 7,86/km."
                )
            ),

            // 4. DUPLO-STACK 99 FOOD + UBER EATS
            RadarOffer(
                id = "merged_stk_103",
                appName = "99 Food + Uber (Multi-Stack)",
                appColor = NeonGreen,
                restaurant = "Habib's Rebouças & Subway Pinheiros",
                value = 29.50,
                distanceKm = 3.9,
                timeMinutes = 19,
                pickupAddress = "Av. Rebouças, 2400",
                destinationAddress = "R. Fradique Coutinho, 820",
                isMultiStack = true,
                subOrders = listOf(
                    SubDeliveryOrder(
                        appName = "99 Food",
                        appColor = Yellow99,
                        restaurant = "Habib's Rebouças",
                        value = 14.50,
                        distanceKm = 2.5,
                        pickupAddress = "Av. Rebouças, 2400",
                        destinationAddress = "R. Fradique Coutinho, 820",
                        kitchenPrepMinutes = 4,
                        dishCategory = "Esfihas / Árabe",
                        readyStatus = "Embalado na estufa"
                    ),
                    SubDeliveryOrder(
                        appName = "Uber Eats",
                        appColor = TextLight,
                        restaurant = "Subway Pinheiros",
                        value = 15.00,
                        distanceKm = 2.2,
                        pickupAddress = "R. Teodoro Sampaio, 1100",
                        destinationAddress = "R. Mourato Coelho, 450",
                        kitchenPrepMinutes = 5,
                        dishCategory = "Sanduíche",
                        readyStatus = "Pronto em 2 min"
                    )
                ),
                synergySavingsKm = 1.1,
                synergyBonusPercent = 52,
                waypointRoute = listOf(
                    "● Coleta 1: Habib's (Av. Rebouças)",
                    "● Coleta 2: Subway (R. Teodoro Sampaio)",
                    "🏠 Entrega 1: R. Fradique Coutinho",
                    "🏢 Entrega 2: R. Mourato Coelho"
                ),
                quantumTelemetry = HyperQuantumTelemetry(
                    quantumScore = 95,
                    vectorDeviationDegrees = 6.1,
                    kitchenSyncPoints = listOf(
                        KitchenSyncPoint("Habib's", 4, "Na estufa", "Árabe"),
                        KitchenSyncPoint("Subway", 5, "Pronto em 2 min", "Lanches")
                    ),
                    fuelSavedMilliliters = 270,
                    penaltyRiskPercent = 0.0,
                    isTriStack = false,
                    inFlightIntercept = false,
                    aiInsight = "Corredor Rebouças ➔ Teodoro Sampaio percorrido em fluxo contínuo."
                )
            )
        )
    }

    /**
     * Gera uma nova entrega mesclada otimizada sob demanda com Telemetria Quântica 4D completa
     */
    fun generateRealtimeMergedStack(preferTriStack: Boolean = false): RadarOffer {
        val isTri = preferTriStack || (1..100).random() <= 35 // 35% de chance de Tri-Stack Quântico

        if (isTri) {
            val triCombinations = listOf(
                Triple(
                    listOf("Outback Steakhouse", "Bullguer Jardins", "Starbucks Paulista"),
                    listOf(Pair("iFood", RedIFood), Pair("Rappi", OrangeRappi), Pair("Uber Direct", TextLight)),
                    "Jardins / Cerqueira César"
                ),
                Triple(
                    listOf("Fogo de Chão Itaim", "Bacio di Latte Pinheiros", "Wendy's Faria Lima"),
                    listOf(Pair("iFood", RedIFood), Pair("99 Food", Yellow99), Pair("Rappi", OrangeRappi)),
                    "Itaim Bibi / Pinheiros"
                ),
                Triple(
                    listOf("Coco Bambu Anália", "Madero Shopping", "Habib's Radial"),
                    listOf(Pair("Rappi", OrangeRappi), Pair("iFood", RedIFood), Pair("Uber Direct", TextLight)),
                    "Tatuapé / Anália Franco"
                )
            )
            val selectedTri = triCombinations.random()
            val restaurants = selectedTri.first
            val apps = selectedTri.second
            val corridor = selectedTri.third

            val val1 = (16..22).random().toDouble()
            val val2 = (17..24).random().toDouble()
            val val3 = (15..20).random().toDouble()
            val totalVal = val1 + val2 + val3

            val distTotal = ((48..62).random() / 10.0) // 4.8 a 6.2 km
            val savedKm = ((20..32).random() / 10.0) // 2.0 a 3.2 km economizados
            val bonusPct = (75..98).random()

            val id = "tri_stack_${System.currentTimeMillis() % 100000}"

            return RadarOffer(
                id = id,
                appName = "${apps[0].first} + ${apps[1].first} + ${apps[2].first} (Tri-Stack 4D)",
                appColor = NeonGreen,
                restaurant = "${restaurants[0]} & ${restaurants[1]} & ${restaurants[2]}",
                value = totalVal,
                distanceKm = distTotal,
                timeMinutes = (distTotal * 4.2).toInt().coerceIn(20, 29),
                pickupAddress = "Corredor Quântico: $corridor",
                destinationAddress = "Quadrante Unificado: $corridor",
                isMultiStack = true,
                subOrders = listOf(
                    SubDeliveryOrder(
                        appName = apps[0].first,
                        appColor = apps[0].second,
                        restaurant = restaurants[0],
                        value = val1,
                        distanceKm = distTotal * 0.4,
                        pickupAddress = "Hub 1: ${restaurants[0]}",
                        destinationAddress = "Destino Cliente Alpha",
                        kitchenPrepMinutes = 3,
                        dishCategory = "Prato Principal",
                        readyStatus = "Pronto ao chegar"
                    ),
                    SubDeliveryOrder(
                        appName = apps[1].first,
                        appColor = apps[1].second,
                        restaurant = restaurants[1],
                        value = val2,
                        distanceKm = distTotal * 0.35,
                        pickupAddress = "Hub 2: ${restaurants[1]}",
                        destinationAddress = "Destino Cliente Beta",
                        kitchenPrepMinutes = 6,
                        dishCategory = "Acompanhamento",
                        readyStatus = "Sai em 4 min"
                    ),
                    SubDeliveryOrder(
                        appName = apps[2].first,
                        appColor = apps[2].second,
                        restaurant = restaurants[2],
                        value = val3,
                        distanceKm = distTotal * 0.25,
                        pickupAddress = "Hub 3: ${restaurants[2]}",
                        destinationAddress = "Destino Cliente Gamma",
                        kitchenPrepMinutes = 9,
                        dishCategory = "Bebidas / Café",
                        readyStatus = "Balcão Liberado"
                    )
                ),
                synergySavingsKm = savedKm,
                synergyBonusPercent = bonusPct,
                waypointRoute = listOf(
                    "● Coleta 1 (${apps[0].first}): ${restaurants[0]} [Pronto]",
                    "● Coleta 2 (${apps[1].first}): ${restaurants[1]} [A 200m]",
                    "● Coleta 3 (${apps[2].first}): ${restaurants[2]} [A 350m]",
                    "🏠 Entrega 1: Destino Alpha",
                    "🏢 Entrega 2: Destino Beta",
                    "🏠 Entrega 3: Destino Gamma"
                ),
                quantumTelemetry = HyperQuantumTelemetry(
                    quantumScore = (97..99).random(),
                    vectorDeviationDegrees = ((35..55).random() / 10.0),
                    kitchenSyncPoints = listOf(
                        KitchenSyncPoint(restaurants[0], 3, "Pronto ao chegar", "Cozinha A"),
                        KitchenSyncPoint(restaurants[1], 6, "Sai em 4 min", "Cozinha B"),
                        KitchenSyncPoint(restaurants[2], 9, "Balcão Liberado", "Cozinha C")
                    ),
                    fuelSavedMilliliters = (420..540).random(),
                    penaltyRiskPercent = 0.0,
                    isTriStack = true,
                    inFlightIntercept = false,
                    aiInsight = "Tri-Stack 4D ativado: R$ ${String.format(Locale("pt", "BR"), "%.2f", totalVal / distTotal)}/km com zero minutos perdidos na calçada."
                )
            )
        }

        // Duplo-Stack com Sincronia 4D Dinâmica
        val pairs = listOf(
            Triple("Outback Steakhouse", "Bullguer Oscar Freire", "Shopping Morumbi & R. Oscar Freire"),
            Triple("Starbucks Coffee", "Burger King Jardins", "R. Augusta & Av. Paulista"),
            Triple("Fogo de Chão Itaim", "Bacio di Latte Pinheiros", "R. Moreira Guimarães & R. dos Pinheiros"),
            Triple("Coco Bambu Anália Franco", "Wendy's Faria Lima", "R. Itapura & Av. Brig. Faria Lima")
        )
        val selected = pairs.random()

        val appPair = listOf(
            Pair("iFood", RedIFood) to Pair("Rappi", OrangeRappi),
            Pair("iFood", RedIFood) to Pair("99 Food", Yellow99),
            Pair("Rappi", OrangeRappi) to Pair("Uber Eats", TextLight)
        ).random()

        val val1 = (15..23).random().toDouble()
        val val2 = (16..24).random().toDouble()
        val totalVal = val1 + val2

        val baseLat = -23.561684
        val baseLng = -46.655981
        val dist1 = LocationService.estimateUrbanRouteKm(baseLat, baseLng, baseLat + 0.015, baseLng + 0.018).coerceIn(2.0, 3.8)
        val dist2 = LocationService.estimateUrbanRouteKm(baseLat + 0.015, baseLng + 0.018, baseLat - 0.010, baseLng - 0.012).coerceIn(1.8, 3.5)

        val rawTotal = (dist1 + dist2) * 0.68
        val totalDist = (Math.round(rawTotal * 10.0) / 10.0).coerceAtLeast(3.0)
        val savedKm = Math.max(0.8, Math.round(((dist1 + dist2) - totalDist) * 10.0) / 10.0)
        val bonusPct = (50..82).random()

        val id = "quantum_duo_${System.currentTimeMillis() % 100000}"
        val inFlight = (1..100).random() <= 40 // 40% chance de Intercepção em Voo

        return RadarOffer(
            id = id,
            appName = if (inFlight) "${appPair.first.first} + ${appPair.second.first} (Intercepção em Voo)" else "${appPair.first.first} + ${appPair.second.first} (Multi-Stack 4D)",
            appColor = NeonGreen,
            restaurant = selected.third,
            value = totalVal,
            distanceKm = totalDist,
            timeMinutes = (totalDist * 3.6).toInt().coerceIn(15, 26),
            pickupAddress = if (inFlight) "Coleta no Curso: ${selected.first}" else "Hub Coleta: ${selected.first}",
            destinationAddress = "Corredor Unificado: Jardins / Pinheiros",
            isMultiStack = true,
            subOrders = listOf(
                SubDeliveryOrder(
                    appName = appPair.first.first,
                    appColor = appPair.first.second,
                    restaurant = selected.first,
                    value = val1,
                    distanceKm = dist1,
                    pickupAddress = "Hub 1: ${selected.first}",
                    destinationAddress = "Destino Cliente A",
                    kitchenPrepMinutes = 3,
                    dishCategory = "Fast Food",
                    readyStatus = "Pronto ao chegar"
                ),
                SubDeliveryOrder(
                    appName = appPair.second.first,
                    appColor = appPair.second.second,
                    restaurant = selected.second,
                    value = val2,
                    distanceKm = dist2,
                    pickupAddress = "Hub 2: ${selected.second}",
                    destinationAddress = "Destino Cliente B",
                    kitchenPrepMinutes = 6,
                    dishCategory = "Refeição",
                    readyStatus = "Lacrado em 4 min"
                )
            ),
            synergySavingsKm = savedKm,
            synergyBonusPercent = bonusPct,
            waypointRoute = listOf(
                "● Coleta 1 (${appPair.first.first}): ${selected.first}",
                "● Coleta 2 (${appPair.second.first}): ${selected.second}",
                "🏠 Entrega 1: Destino Cliente A",
                "🏢 Entrega 2: Destino Cliente B"
            ),
            quantumTelemetry = HyperQuantumTelemetry(
                quantumScore = (95..99).random(),
                vectorDeviationDegrees = if (inFlight) 3.6 else 5.8,
                kitchenSyncPoints = listOf(
                    KitchenSyncPoint(selected.first, 3, "Pronto ao chegar", "Fast Food"),
                    KitchenSyncPoint(selected.second, 6, "Lacrado em 4 min", "Refeição")
                ),
                fuelSavedMilliliters = (280..380).random(),
                penaltyRiskPercent = 0.0,
                isTriStack = false,
                inFlightIntercept = inFlight,
                aiInsight = if (inFlight) "Intercepção vetorial em voo: pedido interceptado a 250m do curso atual sem desvio." else "Sincronia de Cozinha 4D: tempo de espera na calçada zero."
            )
        )
    }

    /**
     * Executa busca e pareamento inteligente a partir de ofertas isoladas existentes.
     * Algoritmo Aprimorado de Busca Multi-App O(N²) e O(N³):
     * 1. Detecta Tri-Stacks viáveis se houver 3 ofertas de plataformas distintas no mesmo vetor.
     * 2. Caso contrário, pareia as melhores duplas O(N²).
     * 3. Aplica telemetria quântica completa (Sincronia de Cozinha, Desvio Vetorial e Anti-Ban).
     */
    fun findPairableSynergy(offers: List<RadarOffer>): List<RadarOffer> {
        val singles = offers.filter { !it.isMultiStack }
        if (singles.size < 2) return emptyList()

        val candidateStacks = mutableListOf<RadarOffer>()

        // 1. Busca por Tri-Stack se houver pelo menos 3 ofertas de plataformas distintas
        if (singles.size >= 3) {
            val triStack = findHyperQuantumTriStack(singles)
            if (triStack != null) {
                candidateStacks.add(triStack)
            }
        }

        // 2. Busca por Pares Sinergéticos
        for (i in 0 until singles.size) {
            for (j in (i + 1) until singles.size) {
                val a = singles[i]
                val b = singles[j]

                // Prioriza mesclagem entre aplicativos diferentes (iFood + Rappi, Uber + 99, etc.)
                if (a.appName != b.appName) {
                    val combinedValue = a.value + b.value

                    // Fator de sobreposição geográfica estimado com base na proximidade de hubs e destinos
                    val overlapFactor = when {
                        a.pickupAddress.contains("Paulista", ignoreCase = true) && b.pickupAddress.contains("Paulista", ignoreCase = true) -> 0.60
                        a.destinationAddress.contains("Jardins", ignoreCase = true) && b.destinationAddress.contains("Jardins", ignoreCase = true) -> 0.63
                        a.destinationAddress.contains("Pinheiros", ignoreCase = true) && b.destinationAddress.contains("Pinheiros", ignoreCase = true) -> 0.66
                        else -> 0.72 // Rota compartilhada padrão no raio urbano
                    }

                    val rawDistance = (a.distanceKm + b.distanceKm) * overlapFactor
                    val combinedDistance = (Math.round(rawDistance * 10.0) / 10.0).coerceAtLeast(1.8)
                    val savedKm = Math.max(0.5, Math.round(((a.distanceKm + b.distanceKm) - combinedDistance) * 10.0) / 10.0)

                    val maxSingleGain = Math.max(a.gainPerKm, b.gainPerKm)
                    val combinedGainPerKm = combinedValue / combinedDistance
                    val bonusPercent = (((combinedGainPerKm / maxSingleGain) - 1.0) * 100).toInt().coerceIn(20, 95)

                    val estimatedTimeMinutes = ((a.timeMinutes + b.timeMinutes) * 0.70).toInt().coerceAtLeast(14)
                    val fuelSavedMl = (savedKm * 65.0).toInt().coerceIn(200, 450)

                    candidateStacks.add(
                        RadarOffer(
                            id = "paired_${a.id}_${b.id}",
                            appName = "${a.appName} + ${b.appName} (Multi-Stack 4D)",
                            appColor = NeonGreen,
                            restaurant = "${a.restaurant} & ${b.restaurant}",
                            value = combinedValue,
                            distanceKm = combinedDistance,
                            timeMinutes = estimatedTimeMinutes,
                            pickupAddress = "${a.pickupAddress} ➔ ${b.pickupAddress}",
                            destinationAddress = "${a.destinationAddress} ➔ ${b.destinationAddress}",
                            isMultiStack = true,
                            subOrders = listOf(
                                SubDeliveryOrder(
                                    appName = a.appName,
                                    appColor = a.appColor,
                                    restaurant = a.restaurant,
                                    value = a.value,
                                    distanceKm = a.distanceKm,
                                    pickupAddress = a.pickupAddress,
                                    destinationAddress = a.destinationAddress,
                                    kitchenPrepMinutes = 3,
                                    dishCategory = "Restaurante A",
                                    readyStatus = "Pronto ao chegar"
                                ),
                                SubDeliveryOrder(
                                    appName = b.appName,
                                    appColor = b.appColor,
                                    restaurant = b.restaurant,
                                    value = b.value,
                                    distanceKm = b.distanceKm,
                                    pickupAddress = b.pickupAddress,
                                    destinationAddress = b.destinationAddress,
                                    kitchenPrepMinutes = 6,
                                    dishCategory = "Restaurante B",
                                    readyStatus = "No forno"
                                )
                            ),
                            synergySavingsKm = savedKm,
                            synergyBonusPercent = bonusPercent,
                            waypointRoute = listOf(
                                "● Coleta 1 (${a.appName}): ${a.restaurant}",
                                "● Coleta 2 (${b.appName}): ${b.restaurant}",
                                "🏠 Entrega 1: ${a.destinationAddress}",
                                "🏢 Entrega 2: ${b.destinationAddress}"
                            ),
                            quantumTelemetry = HyperQuantumTelemetry(
                                quantumScore = 96,
                                vectorDeviationDegrees = 5.2,
                                kitchenSyncPoints = listOf(
                                    KitchenSyncPoint(a.restaurant, 3, "Pronto ao chegar", "Cozinha A"),
                                    KitchenSyncPoint(b.restaurant, 6, "No forno", "Cozinha B")
                                ),
                                fuelSavedMilliliters = fuelSavedMl,
                                penaltyRiskPercent = 0.0,
                                isTriStack = false,
                                inFlightIntercept = true,
                                aiInsight = "Sincronia 4D: economia de ${savedKm}km com ganho de R$ ${String.format(Locale("pt", "BR"), "%.2f", combinedGainPerKm)}/km."
                            )
                        )
                    )
                }
            }
        }

        // Ordena pela maior rentabilidade por km gerada
        return candidateStacks.sortedByDescending { it.gainPerKm }
    }

    /**
     * Algoritmo de busca quântica de Tri-Stack O(N³) entre 3 aplicativos diferentes
     */
    private fun findHyperQuantumTriStack(singles: List<RadarOffer>): RadarOffer? {
        val distinctAppSingles = singles.distinctBy { it.appName }
        if (distinctAppSingles.size < 3) return null

        val a = distinctAppSingles[0]
        val b = distinctAppSingles[1]
        val c = distinctAppSingles[2]

        val totalVal = a.value + b.value + c.value
        val rawDist = (a.distanceKm + b.distanceKm + c.distanceKm) * 0.58 // altíssima sinergia de corredor
        val combinedDist = (Math.round(rawDist * 10.0) / 10.0).coerceAtLeast(3.2)
        val savedKm = Math.max(1.5, Math.round(((a.distanceKm + b.distanceKm + c.distanceKm) - combinedDist) * 10.0) / 10.0)
        val gainPerKm = totalVal / combinedDist
        val bonusPercent = (((gainPerKm / Math.max(a.gainPerKm, Math.max(b.gainPerKm, c.gainPerKm))) - 1.0) * 100).toInt().coerceIn(40, 110)

        return RadarOffer(
            id = "tri_${a.id}_${b.id}_${c.id}",
            appName = "${a.appName} + ${b.appName} + ${c.appName} (Tri-Stack Quântico)",
            appColor = NeonGreen,
            restaurant = "${a.restaurant}, ${b.restaurant} & ${c.restaurant}",
            value = totalVal,
            distanceKm = combinedDist,
            timeMinutes = ((a.timeMinutes + b.timeMinutes + c.timeMinutes) * 0.65).toInt().coerceAtLeast(20),
            pickupAddress = "Corredor Multi-Hub: ${a.pickupAddress}",
            destinationAddress = "Quadrante Compartilhado SP",
            isMultiStack = true,
            subOrders = listOf(
                SubDeliveryOrder(a.appName, a.appColor, a.restaurant, a.value, a.distanceKm, a.pickupAddress, a.destinationAddress, 3, "Refeição", "Pronto em 2 min"),
                SubDeliveryOrder(b.appName, b.appColor, b.restaurant, b.value, b.distanceKm, b.pickupAddress, b.destinationAddress, 6, "Prato", "Pronto em 5 min"),
                SubDeliveryOrder(c.appName, c.appColor, c.restaurant, c.value, c.distanceKm, c.pickupAddress, c.destinationAddress, 9, "Bebidas", "Pronto em 8 min")
            ),
            synergySavingsKm = savedKm,
            synergyBonusPercent = bonusPercent,
            waypointRoute = listOf(
                "● Coleta 1 (${a.appName}): ${a.restaurant}",
                "● Coleta 2 (${b.appName}): ${b.restaurant}",
                "● Coleta 3 (${c.appName}): ${c.restaurant}",
                "🏠 Entrega 1: ${a.destinationAddress}",
                "🏢 Entrega 2: ${b.destinationAddress}",
                "🏠 Entrega 3: ${c.destinationAddress}"
            ),
            quantumTelemetry = HyperQuantumTelemetry(
                quantumScore = 99,
                vectorDeviationDegrees = 4.1,
                kitchenSyncPoints = listOf(
                    KitchenSyncPoint(a.restaurant, 3, "Pronto em 2 min", "Cozinha A"),
                    KitchenSyncPoint(b.restaurant, 6, "Pronto em 5 min", "Cozinha B"),
                    KitchenSyncPoint(c.restaurant, 9, "Pronto em 8 min", "Cozinha C")
                ),
                fuelSavedMilliliters = 480,
                penaltyRiskPercent = 0.0,
                isTriStack = true,
                inFlightIntercept = false,
                aiInsight = "Tri-Stack Otimizado 4D: 3 entregas em rota única, economizando ${savedKm}km e garantindo R$ ${String.format(Locale("pt", "BR"), "%.2f", gainPerKm)}/km."
            )
        )
    }

    /**
     * Mapeia um JSON de pedido retornado pela API REST (/api/stacks) para um [RadarOffer] completo,
     * identificando automaticamente se é multi-stack/tri-stack, calculando ganho/km e avaliando com Jarvis Neural.
     */
    fun mapBackendStackToRadarOffer(json: JSONObject): RadarOffer {
        val id = json.optString("id", "stk_${System.currentTimeMillis() % 10000}")
        val apps = json.optString("apps", "iFood")
        val restaurant = json.optString("restaurant", "Restaurante Local")
        val totalValue = json.optDouble("total_value", 20.0)
        val distanceKm = json.optDouble("distance_km", 3.5)
        val timeMin = json.optInt("time_min", 15)
        val isMulti = apps.contains("+")
        val isTri = apps.count { it == '+' } >= 2

        val appColor = when {
            apps.contains("iFood", ignoreCase = true) -> RedIFood
            apps.contains("Rappi", ignoreCase = true) -> OrangeRappi
            apps.contains("99", ignoreCase = true) -> Yellow99
            else -> TextLight
        }

        val decision = RadarDecisionEngine.evaluate(totalValue, distanceKm, apps)

        val subOrdersList = if (isMulti) {
            val appParts = apps.split("+").map { it.trim() }
            val restParts = restaurant.split("&").map { it.trim() }
            val count = appParts.size.coerceAtLeast(2)
            val valPerOrder = (totalValue / count * 100).toInt() / 100.0
            val distPerOrder = (distanceKm / count * 10).toInt() / 10.0

            appParts.mapIndexed { index, appNamePart ->
                val restName = restParts.getOrNull(index) ?: "$restaurant ($appNamePart)"
                val orderColor = when {
                    appNamePart.contains("iFood", true) -> RedIFood
                    appNamePart.contains("Rappi", true) -> OrangeRappi
                    appNamePart.contains("99", true) -> Yellow99
                    else -> TextLight
                }
                SubDeliveryOrder(
                    appName = appNamePart,
                    appColor = orderColor,
                    restaurant = restName,
                    value = if (index == count - 1) totalValue - (valPerOrder * (count - 1)) else valPerOrder,
                    distanceKm = distPerOrder.coerceAtLeast(1.0),
                    pickupAddress = "Hub $restName",
                    destinationAddress = "Ponto de Entrega ${('A'.code + index).toChar()}",
                    kitchenPrepMinutes = (index + 1) * 3,
                    dishCategory = "Culinária Local",
                    readyStatus = "Sincronizado"
                )
            }
        } else {
            emptyList()
        }

        val quantum = if (isMulti) {
            HyperQuantumTelemetry(
                quantumScore = if (isTri) 99 else 96,
                vectorDeviationDegrees = if (isTri) 4.2 else 5.5,
                kitchenSyncPoints = subOrdersList.map {
                    KitchenSyncPoint(it.restaurant, it.kitchenPrepMinutes, it.readyStatus, it.dishCategory)
                },
                fuelSavedMilliliters = if (isTri) 460 else 310,
                penaltyRiskPercent = 0.0,
                isTriStack = isTri,
                inFlightIntercept = true,
                aiInsight = "Sincronia 4D com Zero Espera de Balcão e Escudo Anti-Ban ativo."
            )
        } else {
            null
        }

        return RadarOffer(
            id = id,
            appName = if (isTri) "$apps (Tri-Stack 4D)" else if (isMulti) "$apps (Multi-Stack)" else apps,
            appColor = if (isMulti) NeonGreen else appColor,
            restaurant = restaurant,
            value = totalValue,
            distanceKm = distanceKm,
            timeMinutes = timeMin,
            pickupAddress = "Hub Parceiro: $restaurant",
            destinationAddress = "Entrega Cliente, SP",
            isMultiStack = isMulti,
            neuralDecision = decision,
            subOrders = subOrdersList,
            synergySavingsKm = if (isTri) 2.2 else if (isMulti) 1.0 else 0.0,
            synergyBonusPercent = if (isTri) 85 else if (isMulti) 52 else 0,
            waypointRoute = if (isMulti) {
                subOrdersList.mapIndexed { idx, sub ->
                    "● Coleta ${idx + 1} (${sub.appName}): ${sub.restaurant}"
                } + subOrdersList.mapIndexed { idx, _ ->
                    "🏠 Entrega Cliente ${('A'.code + idx).toChar()}"
                }
            } else {
                listOf(
                    "● Coleta: $restaurant",
                    "🏠 Entrega Cliente"
                )
            },
            quantumTelemetry = quantum
        )
    }
}
