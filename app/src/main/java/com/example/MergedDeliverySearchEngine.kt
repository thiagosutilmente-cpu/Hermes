package com.example

import androidx.compose.ui.graphics.Color
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
    val destinationAddress: String
)

/**
 * Previsão probabilística da Ghost Sequence para mesclagem iminente de pedidos.
 */
data class GhostSequencePrediction(
    val probabilityPercent: Int = 83,
    val etaMinutes: Int = 3,
    val sourceApp: String = "Rappi",
    val pickupCorridor: String = "Av. Paulista / R. Augusta",
    val potentialBonus: Double = 18.0,
    val message: String = "83% chance de nova mesclagem em 3 min no mesmo raio"
)

/**
 * Motor Inteligente de Busca e Otimização de Entregas Mescladas (Multi-App Stacking Engine)
 * Responsável por analisar sinergia geográfica entre pedidos de diferentes plataformas
 * (iFood, Rappi, Uber Eats, 99Food), calcular redução de km em rotas compartilhadas
 * e maximizar a rentabilidade por quilômetro rodado (R$/km).
 */
object MergedDeliverySearchEngine {

    /**
     * Retorna a previsão neural de Ghost Sequence ativa para a região
     */
    fun getActiveGhostPrediction(): GhostSequencePrediction {
        return GhostSequencePrediction(
            probabilityPercent = 83,
            etaMinutes = 3,
            sourceApp = "Rappi",
            pickupCorridor = "Corredor Paulista ➔ Bela Cintra",
            potentialBonus = 18.00,
            message = "83% chance de nova mesclagem Rappi em 3 min no mesmo raio"
        )
    }

    /**
     * Catálogo curado de entregas mescladas com altíssima sinergia de trajeto
     */
    fun getCuratedMergedStacks(): List<RadarOffer> {
        return listOf(
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
                        destinationAddress = "R. Bela Cintra, 904"
                    ),
                    SubDeliveryOrder(
                        appName = "Rappi",
                        appColor = OrangeRappi,
                        restaurant = "Pizza Hut Al. Santos",
                        value = 18.00,
                        distanceKm = 2.4,
                        pickupAddress = "Al. Santos, 120",
                        destinationAddress = "Al. Lorena, 450"
                    )
                ),
                synergySavingsKm = 1.0, // 5.2km individual -> 4.2km mesclado
                synergyBonusPercent = 58,
                waypointRoute = listOf(
                    "● Coleta 1: Burger King (Av. Paulista)",
                    "● Coleta 2: Pizza Hut (Al. Santos)",
                    "🏠 Entrega 1: R. Bela Cintra, 904",
                    "🏢 Entrega 2: Al. Lorena, 450"
                )
            ),
            RadarOffer(
                id = "merged_stk_102",
                appName = "Rappi + iFood (Multi-Stack)",
                appColor = NeonGreen,
                restaurant = "Madero Vila Olímpia & Bacio di Latte",
                value = 36.00,
                distanceKm = 4.8,
                timeMinutes = 21,
                pickupAddress = "R. Funchal, 200",
                destinationAddress = "Av. Horácio Lafer, 600",
                isMultiStack = true,
                subOrders = listOf(
                    SubDeliveryOrder(
                        appName = "Rappi",
                        appColor = OrangeRappi,
                        restaurant = "Madero Container Funchal",
                        value = 19.00,
                        distanceKm = 3.2,
                        pickupAddress = "R. Funchal, 200",
                        destinationAddress = "Av. Horácio Lafer, 600"
                    ),
                    SubDeliveryOrder(
                        appName = "iFood",
                        appColor = RedIFood,
                        restaurant = "Bacio di Latte Itaim",
                        value = 17.00,
                        distanceKm = 2.6,
                        pickupAddress = "R. Joaquim Floriano, 550",
                        destinationAddress = "R. Bandeira Paulista, 720"
                    )
                ),
                synergySavingsKm = 1.0,
                synergyBonusPercent = 65,
                waypointRoute = listOf(
                    "● Coleta 1: Madero (R. Funchal)",
                    "● Coleta 2: Bacio di Latte (R. J. Floriano)",
                    "🏠 Entrega 1: Av. Horácio Lafer",
                    "🏢 Entrega 2: R. Bandeira Paulista"
                )
            ),
            RadarOffer(
                id = "merged_stk_103",
                appName = "99 Food + Uber (Multi-Stack)",
                appColor = NeonGreen,
                restaurant = "Habib's Rebouças & Subway Pinheiros",
                value = 27.50,
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
                        value = 13.50,
                        distanceKm = 2.5,
                        pickupAddress = "Av. Rebouças, 2400",
                        destinationAddress = "R. Fradique Coutinho, 820"
                    ),
                    SubDeliveryOrder(
                        appName = "Uber Eats",
                        appColor = TextLight,
                        restaurant = "Subway Pinheiros",
                        value = 14.00,
                        distanceKm = 2.2,
                        pickupAddress = "R. Teodoro Sampaio, 1100",
                        destinationAddress = "R. Mourato Coelho, 450"
                    )
                ),
                synergySavingsKm = 0.8,
                synergyBonusPercent = 42,
                waypointRoute = listOf(
                    "● Coleta 1: Habib's (Av. Rebouças)",
                    "● Coleta 2: Subway (R. Teodoro Sampaio)",
                    "🏠 Entrega 1: R. Fradique Coutinho",
                    "🏢 Entrega 2: R. Mourato Coelho"
                )
            ),
            RadarOffer(
                id = "merged_stk_104",
                appName = "iFood + 99 Food (Multi-Stack)",
                appColor = NeonGreen,
                restaurant = "Sukiya Liberdade & Ragazzo Aclimação",
                value = 31.00,
                distanceKm = 4.5,
                timeMinutes = 20,
                pickupAddress = "Praça da Liberdade, 120",
                destinationAddress = "R. Vergueiro, 1500",
                isMultiStack = true,
                subOrders = listOf(
                    SubDeliveryOrder(
                        appName = "iFood",
                        appColor = RedIFood,
                        restaurant = "Sukiya Liberdade",
                        value = 16.50,
                        distanceKm = 2.9,
                        pickupAddress = "Praça da Liberdade, 120",
                        destinationAddress = "R. Vergueiro, 1500"
                    ),
                    SubDeliveryOrder(
                        appName = "99 Food",
                        appColor = Yellow99,
                        restaurant = "Ragazzo Aclimação",
                        value = 14.50,
                        distanceKm = 2.4,
                        pickupAddress = "Av. da Aclimação, 420",
                        destinationAddress = "R. Domingos de Morais, 800"
                    )
                ),
                synergySavingsKm = 0.8,
                synergyBonusPercent = 51,
                waypointRoute = listOf(
                    "● Coleta 1: Sukiya (Pça Liberdade)",
                    "● Coleta 2: Ragazzo (Av. Aclimação)",
                    "🏠 Entrega 1: R. Vergueiro",
                    "🏢 Entrega 2: R. Domingos de Morais"
                )
            )
        )
    }

    /**
     * Gera uma nova entrega mesclada otimizada sob demanda (quando o piloto aperta o botão de varredura)
     */
    fun generateRealtimeMergedStack(): RadarOffer {
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

        val val1 = (14..22).random().toDouble()
        val val2 = (15..23).random().toDouble()
        val totalVal = val1 + val2

        val dist1 = (20..35).random() / 10.0
        val dist2 = (18..30).random() / 10.0
        // Economia de rota compartilhada de 20% a 35%
        val totalDist = ((dist1 + dist2) * 0.75).let { Math.round(it * 10) / 10.0 }.coerceAtLeast(3.2)
        val savedKm = Math.max(0.6, Math.round(((dist1 + dist2) - totalDist) * 10) / 10.0)
        val bonusPct = (45..75).random()

        val id = "merged_${System.currentTimeMillis() % 100000}"

        return RadarOffer(
            id = id,
            appName = "${appPair.first.first} + ${appPair.second.first} (Multi-Stack)",
            appColor = NeonGreen,
            restaurant = selected.third,
            value = totalVal,
            distanceKm = totalDist,
            timeMinutes = (totalDist * 3.6).toInt().coerceIn(15, 26),
            pickupAddress = "Hub Coleta: ${selected.first}",
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
                    destinationAddress = "Destino Cliente A"
                ),
                SubDeliveryOrder(
                    appName = appPair.second.first,
                    appColor = appPair.second.second,
                    restaurant = selected.second,
                    value = val2,
                    distanceKm = dist2,
                    pickupAddress = "Hub 2: ${selected.second}",
                    destinationAddress = "Destino Cliente B"
                )
            ),
            synergySavingsKm = savedKm,
            synergyBonusPercent = bonusPct,
            waypointRoute = listOf(
                "● Coleta 1: ${selected.first}",
                "● Coleta 2: ${selected.second}",
                "🏠 Entrega 1: Destino Cliente A",
                "🏢 Entrega 2: Destino Cliente B"
            )
        )
    }

    /**
     * Executa busca e pareamento inteligente a partir de ofertas isoladas existentes
     */
    fun findPairableSynergy(offers: List<RadarOffer>): List<RadarOffer> {
        val singles = offers.filter { !it.isMultiStack }
        if (singles.size < 2) return emptyList()

        val mergedResults = mutableListOf<RadarOffer>()
        for (i in 0 until singles.size - 1) {
            val a = singles[i]
            val b = singles[i + 1]
            if (a.appName != b.appName) {
                // Combina os dois apps
                val combinedValue = a.value + b.value
                val combinedDistance = ((a.distanceKm + b.distanceKm) * 0.72).let { Math.round(it * 10) / 10.0 }
                val savedKm = Math.round(((a.distanceKm + b.distanceKm) - combinedDistance) * 10) / 10.0
                val bonus = (((combinedValue / combinedDistance) / Math.max(a.gainPerKm, b.gainPerKm) - 1.0) * 100).toInt().coerceIn(20, 80)

                mergedResults.add(
                    RadarOffer(
                        id = "paired_${a.id}_${b.id}",
                        appName = "${a.appName} + ${b.appName} (Multi-Stack)",
                        appColor = NeonGreen,
                        restaurant = "${a.restaurant} & ${b.restaurant}",
                        value = combinedValue,
                        distanceKm = combinedDistance,
                        timeMinutes = ((a.timeMinutes + b.timeMinutes) * 0.75).toInt(),
                        pickupAddress = a.pickupAddress,
                        destinationAddress = b.destinationAddress,
                        isMultiStack = true,
                        subOrders = listOf(
                            SubDeliveryOrder(a.appName, a.appColor, a.restaurant, a.value, a.distanceKm, a.pickupAddress, a.destinationAddress),
                            SubDeliveryOrder(b.appName, b.appColor, b.restaurant, b.value, b.distanceKm, b.pickupAddress, b.destinationAddress)
                        ),
                        synergySavingsKm = savedKm,
                        synergyBonusPercent = bonus,
                        waypointRoute = listOf(
                            "● Coleta 1: ${a.restaurant}",
                            "● Coleta 2: ${b.restaurant}",
                            "🏠 Entrega 1: ${a.destinationAddress}",
                            "🏢 Entrega 2: ${b.destinationAddress}"
                        )
                    )
                )
                break
            }
        }
        return mergedResults
    }
}
