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

        // Coordenadas dos pontos de coleta e entrega urbanos
        val baseLat = -23.561684
        val baseLng = -46.655981
        val dist1 = LocationService.estimateUrbanRouteKm(baseLat, baseLng, baseLat + 0.015, baseLng + 0.018).coerceIn(2.0, 3.8)
        val dist2 = LocationService.estimateUrbanRouteKm(baseLat + 0.015, baseLng + 0.018, baseLat - 0.010, baseLng - 0.012).coerceIn(1.8, 3.5)

        // Economia real de rota compartilhada de 25% a 35% ao unificar trechos
        val rawTotal = (dist1 + dist2) * 0.70
        val totalDist = (Math.round(rawTotal * 10.0) / 10.0).coerceAtLeast(3.0)
        val savedKm = Math.max(0.6, Math.round(((dist1 + dist2) - totalDist) * 10.0) / 10.0)
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
     * Executa busca e pareamento inteligente a partir de ofertas isoladas existentes.
     * Algoritmo Aprimorado de Busca Multi-App O(N²):
     * 1. Testa todas as combinações de pares não-mesclados entre apps parceiros distintos.
     * 2. Calcula a distância combinada considerando o fator de rota urbana compartilhada e proximidade dos hubs.
     * 3. Prioriza e ordena pelo maior ganho financeiro por quilômetro (R$/km) e maior economia de trajeto.
     */
    fun findPairableSynergy(offers: List<RadarOffer>): List<RadarOffer> {
        val singles = offers.filter { !it.isMultiStack }
        if (singles.size < 2) return emptyList()

        val candidatePairs = mutableListOf<RadarOffer>()

        for (i in 0 until singles.size) {
            for (j in (i + 1) until singles.size) {
                val a = singles[i]
                val b = singles[j]

                // Prioriza mesclagem entre aplicativos diferentes (iFood + Rappi, Uber + 99, etc.)
                if (a.appName != b.appName) {
                    val combinedValue = a.value + b.value

                    // Fator de sobreposição geográfica estimado com base na proximidade de hubs e destinos
                    val overlapFactor = when {
                        a.pickupAddress.contains("Paulista", ignoreCase = true) && b.pickupAddress.contains("Paulista", ignoreCase = true) -> 0.62
                        a.destinationAddress.contains("Jardins", ignoreCase = true) && b.destinationAddress.contains("Jardins", ignoreCase = true) -> 0.65
                        a.destinationAddress.contains("Pinheiros", ignoreCase = true) && b.destinationAddress.contains("Pinheiros", ignoreCase = true) -> 0.68
                        else -> 0.74 // Rota compartilhada padrão no raio urbano
                    }

                    val rawDistance = (a.distanceKm + b.distanceKm) * overlapFactor
                    val combinedDistance = (Math.round(rawDistance * 10.0) / 10.0).coerceAtLeast(1.8)
                    val savedKm = Math.max(0.4, Math.round(((a.distanceKm + b.distanceKm) - combinedDistance) * 10.0) / 10.0)

                    val maxSingleGain = Math.max(a.gainPerKm, b.gainPerKm)
                    val combinedGainPerKm = combinedValue / combinedDistance
                    val bonusPercent = (((combinedGainPerKm / maxSingleGain) - 1.0) * 100).toInt().coerceIn(15, 90)

                    val estimatedTimeMinutes = ((a.timeMinutes + b.timeMinutes) * 0.72).toInt().coerceAtLeast(14)

                    candidatePairs.add(
                        RadarOffer(
                            id = "paired_${a.id}_${b.id}",
                            appName = "${a.appName} + ${b.appName} (Multi-Stack)",
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
                                    destinationAddress = a.destinationAddress
                                ),
                                SubDeliveryOrder(
                                    appName = b.appName,
                                    appColor = b.appColor,
                                    restaurant = b.restaurant,
                                    value = b.value,
                                    distanceKm = b.distanceKm,
                                    pickupAddress = b.pickupAddress,
                                    destinationAddress = b.destinationAddress
                                )
                            ),
                            synergySavingsKm = savedKm,
                            synergyBonusPercent = bonusPercent,
                            waypointRoute = listOf(
                                "● Coleta 1 (${a.appName}): ${a.restaurant}",
                                "● Coleta 2 (${b.appName}): ${b.restaurant}",
                                "🏠 Entrega 1: ${a.destinationAddress}",
                                "🏢 Entrega 2: ${b.destinationAddress}"
                            )
                        )
                    )
                }
            }
        }

        // Ordena pela maior rentabilidade por km gerada
        return candidatePairs.sortedByDescending { it.gainPerKm }
    }

    /**
     * Mapeia um JSON de pedido retornado pela API REST (/api/stacks) para um [RadarOffer] completo,
     * identificando automaticamente se é multi-stack, calculando ganho/km e avaliando com Jarvis Neural.
     */
    fun mapBackendStackToRadarOffer(json: JSONObject): RadarOffer {
        val id = json.optString("id", "stk_${System.currentTimeMillis() % 10000}")
        val apps = json.optString("apps", "iFood")
        val restaurant = json.optString("restaurant", "Restaurante Local")
        val totalValue = json.optDouble("total_value", 20.0)
        val distanceKm = json.optDouble("distance_km", 3.5)
        val timeMin = json.optInt("time_min", 15)
        val isMulti = apps.contains("+")

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
            val halfValue = (totalValue / 2.0 * 100).toInt() / 100.0
            val otherValue = totalValue - halfValue
            val halfDist = (distanceKm / 2.0 * 10).toInt() / 10.0
            val otherDist = (distanceKm - halfDist).coerceAtLeast(1.0)
            listOf(
                SubDeliveryOrder(
                    appName = appParts.getOrNull(0) ?: "App 1",
                    appColor = if (appParts.getOrNull(0)?.contains("iFood", true) == true) RedIFood else OrangeRappi,
                    restaurant = restParts.getOrNull(0) ?: restaurant,
                    value = halfValue,
                    distanceKm = halfDist,
                    pickupAddress = "Hub ${restParts.getOrNull(0) ?: restaurant}",
                    destinationAddress = "Ponto de Entrega A"
                ),
                SubDeliveryOrder(
                    appName = appParts.getOrNull(1) ?: "App 2",
                    appColor = if (appParts.getOrNull(1)?.contains("Rappi", true) == true) OrangeRappi else if (appParts.getOrNull(1)?.contains("99", true) == true) Yellow99 else TextLight,
                    restaurant = restParts.getOrNull(1) ?: "Parceiro Secundário",
                    value = otherValue,
                    distanceKm = otherDist,
                    pickupAddress = "Hub ${restParts.getOrNull(1) ?: "Parceiro"}",
                    destinationAddress = "Ponto de Entrega B"
                )
            )
        } else {
            emptyList()
        }

        return RadarOffer(
            id = id,
            appName = if (isMulti) "$apps (Multi-Stack)" else apps,
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
            synergySavingsKm = if (isMulti) 0.9 else 0.0,
            synergyBonusPercent = if (isMulti) 48 else 0,
            waypointRoute = if (isMulti) {
                listOf(
                    "● Coleta Multi-App: $restaurant",
                    "🏠 Entrega Cliente A",
                    "🏢 Entrega Cliente B"
                )
            } else {
                listOf(
                    "● Coleta: $restaurant",
                    "🏠 Entrega Cliente"
                )
            }
        )
    }
}
