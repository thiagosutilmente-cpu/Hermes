package com.example

import androidx.compose.ui.graphics.Color
import java.util.UUID

/**
 * Modelo completo de Oferta Interceptada pelo Radar
 */
data class RadarOffer(
    val id: String,
    val appName: String,
    val appColor: Color,
    val restaurant: String,
    val value: Double,
    val distanceKm: Double,
    val timeMinutes: Int,
    val pickupAddress: String,
    val destinationAddress: String,
    val isMultiStack: Boolean = false,
    val neuralDecision: NeuralDecision = RadarDecisionEngine.evaluate(value, distanceKm, appName)
) {
    val gainPerKm: Double
        get() = if (distanceKm > 0) value / distanceKm else value

    val estimatedTimeMin: Int
        get() = timeMinutes

    val netProfit: Double
        get() = (value - (distanceKm * 0.17)).coerceAtLeast(0.0)
}

/**
 * Simulador Ativo de Despacho em Tempo Real (Dynamic Dispatch Loop)
 */
object LiveDispatchSimulator {

    private val restaurants = listOf(
        Pair("Burger King Jardins", "Av. Paulista, 1578"),
        Pair("Madero Container", "Av. Moema, 450"),
        Pair("Starbucks Coffee", "R. Augusta, 2100"),
        Pair("Outback Steakhouse", "Shopping Morumbi"),
        Pair("Bullguer Oscar Freire", "R. Oscar Freire, 800"),
        Pair("Fogo de Chão Itaim", "R. Moreira Guimarães, 340"),
        Pair("Pizza Hut Paulista", "Al. Santos, 120"),
        Pair("Coco Bambu Anália Franco", "R. Itapura, 990"),
        Pair("Bacio di Latte Pinheiros", "R. dos Pinheiros, 230"),
        Pair("Wendy's Faria Lima", "Av. Brig. Faria Lima, 1800")
    )

    private val destinations = listOf(
        "R. Bela Cintra, 904 - Apto 82",
        "Av. Chucri Zaidan, 110 - Bloco B",
        "Al. Lorena, 450 - Condomínio Jardim",
        "R. Haddock Lobo, 1307",
        "Av. Rebouças, 2200 - Torre Norte",
        "R. Pamplona, 880 - Apto 141",
        "Al. Campinas, 620",
        "R. Teodoro Sampaio, 1420",
        "Av. Santo Amaro, 3200",
        "R. Pedroso Alvarenga, 700"
    )

    /**
     * Retorna a lista inicial com as principais ofertas ativas
     */
    fun getInitialOffers(): List<RadarOffer> {
        return listOf(
            RadarOffer(
                id = "offer_101",
                appName = "iFood + Rappi (Multi-Stack)",
                appColor = NeonGreen,
                restaurant = "Burger King & Pizza Hut",
                value = 33.00,
                distanceKm = 4.2,
                timeMinutes = 18,
                pickupAddress = "Av. Paulista, 1578",
                destinationAddress = "R. Bela Cintra, 904",
                isMultiStack = true
            ),
            RadarOffer(
                id = "offer_102",
                appName = "iFood",
                appColor = RedIFood,
                restaurant = "Madero Container",
                value = 22.50,
                distanceKm = 3.1,
                timeMinutes = 12,
                pickupAddress = "Shopping Ibirapuera",
                destinationAddress = "Av. Moema, 450"
            ),
            RadarOffer(
                id = "offer_103",
                appName = "Rappi",
                appColor = OrangeRappi,
                restaurant = "Starbucks Coffee",
                value = 18.00,
                distanceKm = 2.4,
                timeMinutes = 9,
                pickupAddress = "R. Augusta, 2100",
                destinationAddress = "Al. Santos, 120"
            ),
            RadarOffer(
                id = "offer_104",
                appName = "99 Food",
                appColor = Yellow99,
                restaurant = "Outback Steakhouse",
                value = 29.80,
                distanceKm = 5.0,
                timeMinutes = 20,
                pickupAddress = "Shopping Morumbi",
                destinationAddress = "Av. Chucri Zaidan, 110"
            )
        )
    }

    /**
     * Gera uma nova oferta aleatória realista em tempo real
     */
    fun generateNextOffer(): RadarOffer {
        val (restName, pickup) = restaurants.random()
        val dest = destinations.random()

        val isMulti = (1..5).random() == 1
        val appConfig = if (isMulti) {
            Triple("iFood + Rappi (Multi-Stack)", NeonGreen, true)
        } else {
            when ((1..4).random()) {
                1 -> Triple("iFood", RedIFood, false)
                2 -> Triple("Rappi", OrangeRappi, false)
                3 -> Triple("Uber Eats", TextLight, false)
                else -> Triple("99 Food", Yellow99, false)
            }
        }

        val distance = if (isMulti) {
            (35..65).random() / 10.0
        } else {
            (18..58).random() / 10.0
        }

        // Gera valores proporcionais com alguns casos excelentes (>= R$ 5/km) e outros médios
        val valuePerKmFactor = when ((1..4).random()) {
            1 -> (52..78).random() / 10.0 // Excelente (R$ 5,20 a 7,80 / km)
            2 -> (40..50).random() / 10.0 // Médio-Bom
            3 -> (32..39).random() / 10.0 // Razoável
            else -> (22..30).random() / 10.0 // Desvantajoso
        }

        val computedValue = (distance * valuePerKmFactor).let { Math.round(it * 2) / 2.0 }.coerceAtLeast(14.0)
        val timeMins = (distance * 3.5).toInt().coerceIn(8, 30)

        val id = "offer_${System.currentTimeMillis() % 100000}"

        return RadarOffer(
            id = id,
            appName = appConfig.first,
            appColor = appConfig.second,
            restaurant = if (isMulti) "$restName & Pizza Hut" else restName,
            value = computedValue,
            distanceKm = distance,
            timeMinutes = timeMins,
            pickupAddress = pickup,
            destinationAddress = dest,
            isMultiStack = appConfig.third,
            neuralDecision = RadarDecisionEngine.evaluate(computedValue, distance, appConfig.first)
        )
    }
}
