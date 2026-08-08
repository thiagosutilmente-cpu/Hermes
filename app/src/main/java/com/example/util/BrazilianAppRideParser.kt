package com.example.util

import android.util.Log
import java.util.regex.Pattern

/**
 * Motor de Inteligência para Busca e Análise de Corridas em Apps de Entrega Brasileiros.
 * Suporta: iFood Para Entregadores, Rappi, Uber Driver (Moto/Flash), 99 Moto, Lalamove, Loggi e inDrive.
 */
data class ParsedRideOffer(
    val appName: String,
    val serviceType: String, // "Moto", "Flash", "Batelada", "Turbo", "Favor", etc.
    val fareValue: Double,
    val totalDistanceKm: Double,
    val totalTimeMinutes: Double,
    val pickupAddress: String,
    val deliveryAddress: String,
    val storeOrMerchantName: String? = null,
    val hasTip: Boolean = false,
    val tipValue: Double = 0.0,
    val isMultiOrder: Boolean = false,
    val orderCount: Int = 1,
    val paymentType: String = "Online", // "Online", "Maquininha / Cobrar", "Dinheiro"
    val surgeMultiplier: Double = 1.0,
    val surgeBonus: Double = 0.0,
    val gainPerKm: Double = 0.0,
    val gainPerHour: Double = 0.0,
    val estimatedFuelCost: Double = 0.0,
    val netProfit: Double = 0.0,
    val recommendation: String = "CONSIDERAR", // "ACEITAR", "CONSIDERAR", "RECUSAR"
    val recommendationReason: String = ""
)

object BrazilianAppRideParser {
    private const val TAG = "BrazilianAppRideParser"

    private val FARE_REGEX = Pattern.compile("R\\$\\s*(\\d+[,.]\\d{2})")
    private val DISTANCE_REGEX = Pattern.compile("(\\d+([.,]\\d+)?)\\s*(km|m)", Pattern.CASE_INSENSITIVE)
    private val TIME_REGEX = Pattern.compile("(\\d+)\\s*(min|minutos)", Pattern.CASE_INSENSITIVE)
    private val TIP_REGEX = Pattern.compile("gorjeta\\s*(:+)?\\s*R\\$\\s*(\\d+[,.]\\d{2})", Pattern.CASE_INSENSITIVE)
    private val SURGE_REGEX = Pattern.compile("\\+R\\$\\s*(\\d+[,.]\\d{2})", Pattern.CASE_INSENSITIVE)

    /**
     * Analisa o conjunto de textos extraídos da tela do app e gera um objeto rico de oferta.
     */
    fun parseRideOffer(
        packageName: String,
        extractedTexts: List<String>,
        fuelConsumptionKmPerL: Double = 35.0,
        fuelPricePerL: Double = 5.80,
        minGainPerKm: Double = 3.50
    ): ParsedRideOffer? {
        val joinedText = extractedTexts.joinToString(" | ")

        // 1. Extração de Tarifa (Preço)
        val fareMatcher = FARE_REGEX.matcher(joinedText)
        var fareValue = 0.0
        if (fareMatcher.find()) {
            fareValue = fareMatcher.group(1)?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
        }

        if (fareValue <= 0.0) return null // Nenhuma tarifa válida encontrada

        // 2. Identificação do Aplicativo e Categoria
        val appNameBase = when {
            packageName.contains("ifood", ignoreCase = true) || joinedText.contains("ifood", ignoreCase = true) -> "iFood"
            packageName.contains("uber", ignoreCase = true) || joinedText.contains("uber", ignoreCase = true) -> "Uber"
            packageName.contains("taxis99", ignoreCase = true) || packageName.contains("99", ignoreCase = true) -> "99"
            packageName.contains("rappi", ignoreCase = true) -> "Rappi"
            packageName.contains("lalamove", ignoreCase = true) -> "Lalamove"
            packageName.contains("loggi", ignoreCase = true) -> "Loggi"
            packageName.contains("indriver", ignoreCase = true) -> "inDrive"
            packageName.contains("borzo", ignoreCase = true) -> "Borzo"
            else -> "App Parceiro"
        }

        // 3. Detecção de Tipo de Serviço e Multi-pedidos (Batelada)
        val isMultiOrder = joinedText.contains("batelada", ignoreCase = true) ||
                joinedText.contains("2 entregas", ignoreCase = true) ||
                joinedText.contains("3 entregas", ignoreCase = true) ||
                joinedText.contains("rota com", ignoreCase = true) ||
                joinedText.contains("2 paradas", ignoreCase = true)

        val orderCount = when {
            joinedText.contains("3 entregas", ignoreCase = true) || joinedText.contains("3 paradas", ignoreCase = true) -> 3
            isMultiOrder -> 2
            else -> 1
        }

        val serviceType = when {
            joinedText.contains("turbo", ignoreCase = true) -> "Rappi Turbo"
            joinedText.contains("favor", ignoreCase = true) -> "Rappi Favor"
            joinedText.contains("flash", ignoreCase = true) -> "Uber Flash"
            joinedText.contains("moto", ignoreCase = true) -> if (appNameBase == "Uber") "Uber Moto" else "99Moto"
            isMultiOrder -> "Batelada (${orderCount}x)"
            else -> "Entrega Padrão"
        }

        // 4. Distância e Tempo
        val distMatcher = DISTANCE_REGEX.matcher(joinedText)
        val distances = mutableListOf<Double>()
        while (distMatcher.find()) {
            val valStr = distMatcher.group(1)?.replace(",", ".")?.toDoubleOrNull() ?: continue
            val unit = distMatcher.group(3)?.lowercase() ?: "km"
            distances.add(if (unit == "m") valStr / 1000.0 else valStr)
        }
        val totalDistance = distances.maxOrNull() ?: 2.5 // Fallback padrão seguro

        val timeMatcher = TIME_REGEX.matcher(joinedText)
        val times = mutableListOf<Double>()
        while (timeMatcher.find()) {
            val t = timeMatcher.group(1)?.toDoubleOrNull() ?: continue
            times.add(t)
        }
        val totalTime = times.maxOrNull() ?: 12.0 // Fallback padrão em minutos

        // 5. Gorjetas e Preço Dinâmico (Surge)
        var hasTip = false
        var tipValue = 0.0
        val tipMatcher = TIP_REGEX.matcher(joinedText)
        if (tipMatcher.find()) {
            hasTip = true
            tipValue = tipMatcher.group(2)?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
        }

        var surgeBonus = 0.0
        val surgeMatcher = SURGE_REGEX.matcher(joinedText)
        if (surgeMatcher.find()) {
            surgeBonus = surgeMatcher.group(1)?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
        }

        // 6. Estabelecimento / Restaurante / Loja
        val storeKeywords = listOf("mcdonald", "burger king", "bk", "habib", "outback", "pizza", "subway", "spoleto", "starbucks", "carrefour", "extra", "cacau show")
        val foundStore = extractedTexts.firstOrNull { text ->
            storeKeywords.any { kw -> text.contains(kw, ignoreCase = true) }
        }

        // 7. Tipo de Pagamento
        val paymentType = when {
            joinedText.contains("maquininha", ignoreCase = true) || joinedText.contains("cobrar do cliente", ignoreCase = true) -> "Maquininha no Destino"
            joinedText.contains("dinheiro", ignoreCase = true) -> "Dinheiro"
            else -> "Pagamento Online"
        }

        // 8. Endereços de Coleta e Entrega
        val addressKeywords = listOf("rua", "av.", "avenida", "alameda", "travessa", "praça", "rodovia", "r.", "av", "estrada", "alameda")
        val potentialAddresses = extractedTexts.filter { t ->
            addressKeywords.any { kw -> t.contains(kw, ignoreCase = true) }
        }

        val pickupAddress = when {
            foundStore != null -> foundStore
            potentialAddresses.isNotEmpty() -> potentialAddresses[0]
            else -> "Ponto de Coleta $appNameBase"
        }

        val deliveryAddress = when {
            potentialAddresses.size >= 2 -> potentialAddresses[1]
            potentialAddresses.size == 1 && foundStore != null -> potentialAddresses[0]
            else -> "Destino final da corrida"
        }

        // 9. Cálculo de Indicadores Econômicos (ROI do Entregador)
        val gainPerKm = if (totalDistance > 0) fareValue / totalDistance else fareValue
        val gainPerHour = if (totalTime > 0) (fareValue / totalTime) * 60.0 else fareValue * 4.0

        // Combustível: (Distância / Consumo) * Preço_Gasolina
        val fuelLiters = totalDistance / fuelConsumptionKmPerL.coerceAtLeast(10.0)
        val estimatedFuelCost = fuelLiters * fuelPricePerL
        val netProfit = (fareValue - estimatedFuelCost).coerceAtLeast(0.0)

        // 10. Lógica de Recomendação com Padrões Brasileiros
        val (recommendation, recommendationReason) = evaluateRideDecision(
            appName = appNameBase,
            fareValue = fareValue,
            gainPerKm = gainPerKm,
            gainPerHour = gainPerHour,
            totalDistance = totalDistance,
            totalTime = totalTime,
            isMultiOrder = isMultiOrder,
            hasTip = hasTip,
            surgeBonus = surgeBonus,
            minGainPerKm = minGainPerKm
        )

        val parsedOffer = ParsedRideOffer(
            appName = appNameBase,
            serviceType = serviceType,
            fareValue = fareValue,
            totalDistanceKm = totalDistance,
            totalTimeMinutes = totalTime,
            pickupAddress = pickupAddress,
            deliveryAddress = deliveryAddress,
            storeOrMerchantName = foundStore,
            hasTip = hasTip,
            tipValue = tipValue,
            isMultiOrder = isMultiOrder,
            orderCount = orderCount,
            paymentType = paymentType,
            surgeMultiplier = if (surgeBonus > 0) 1.2 else 1.0,
            surgeBonus = surgeBonus,
            gainPerKm = Math.round(gainPerKm * 100.0) / 100.0,
            gainPerHour = Math.round(gainPerHour * 100.0) / 100.0,
            estimatedFuelCost = Math.round(estimatedFuelCost * 100.0) / 100.0,
            netProfit = Math.round(netProfit * 100.0) / 100.0,
            recommendation = recommendation,
            recommendationReason = recommendationReason
        )

        Log.i(TAG, "Corrida Mapeada [$appNameBase]: R$$fareValue | $totalDistance km | R$${parsedOffer.gainPerKm}/km | Rec: $recommendation ($recommendationReason)")
        return parsedOffer
    }

    private fun evaluateRideDecision(
        appName: String,
        fareValue: Double,
        gainPerKm: Double,
        gainPerHour: Double,
        totalDistance: Double,
        totalTime: Double,
        isMultiOrder: Boolean,
        hasTip: Boolean,
        surgeBonus: Double,
        minGainPerKm: Double
    ): Pair<String, String> {
        return when {
            // Oferta de Ouro
            gainPerKm >= (minGainPerKm * 1.4) && totalDistance <= 6.0 -> {
                "ACEITAR" to "Lucro excelente (R$ ${String.format("%.2f", gainPerKm)}/km) em trajeto curto de $totalDistance km"
            }
            // Batelada com alto ganho por hora
            isMultiOrder && gainPerHour >= 35.0 -> {
                "ACEITAR" to "Batelada otimizada com projeção de R$ ${String.format("%.2f", gainPerHour)}/hora"
            }
            // Dinâmico / Gorjeta expressiva
            surgeBonus >= 3.0 || hasTip -> {
                "ACEITAR" to "Bônus dinâmico (+R$ ${String.format("%.2f", surgeBonus)}) ou gorjeta inclusa compensa a rota"
            }
            // Aceitável
            gainPerKm >= minGainPerKm -> {
                "CONSIDERAR" to "Ganho/km (R$ ${String.format("%.2f", gainPerKm)}) dentro da meta estabelecida"
            }
            // Distância excessiva
            totalDistance > 12.0 -> {
                "RECUSAR" to "Distância excessiva ($totalDistance km) reduz retorno líquido de volta"
            }
            // Ganho baixo por km
            else -> {
                "RECUSAR" to "Ganho/km de R$ ${String.format("%.2f", gainPerKm)} abaixo da meta mínima de R$ ${String.format("%.2f", minGainPerKm)}"
            }
        }
    }
}
