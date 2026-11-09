package com.example

import android.location.Location
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Classificação do alinhamento geométrico e direcional entre dois pedidos de entrega.
 */
enum class AngularCorridorType(
    val label: String,
    val maxAngleDeg: Float,
    val badgeColorHex: Long
) {
    COLINEAR_HIGHWAY("Mesmo Eixo Viário (< 15°)", 15f, 0xFF10B981),
    PARALLEL_CORRIDOR("Corredor Paralelo (< 32°)", 32f, 0xFF06B6D4),
    ADJACENT_SECTOR("Setor Urbano Adjacente (< 50°)", 50f, 0xFFF59E0B),
    TRANSVERSAL_DIVERGENT("Transversal Divergente (< 70°)", 70f, 0xFFF97316),
    OPPOSITE_BACKTRACKING("Sentidos Opostos / Contra-Fluxo (> 70°)", 180f, 0xFFEF4444)
}

/**
 * Nível de risco de espera no restaurante e esfriamento da comida na bag.
 */
enum class KitchenRiskLevel(
    val label: String,
    val badgeColorHex: Long
) {
    MINIMAL_QUICK_GRAB("Balcão Imediato / Rápido", 0xFF10B981),
    ACCEPTABLE_NORMAL("Preparo Médio Normal", 0xFF38BDF8),
    HIGH_DELAY_RISK("Risco de Espera Prolongada", 0xFFF59E0B),
    CRITICAL_COLD_FOOD("Crítico: Risco de Comida Fria", 0xFFEF4444)
}

/**
 * Resultado detalhado da análise de proximidade angular entre os pontos de entrega de diferentes plataformas.
 */
data class AngularProximityResult(
    val bearingP1toD1: Float,
    val bearingP2toD2: Float,
    val corridorAngleDiffDeg: Float,
    val destinationApertureDeg: Float,
    val angularCosineSimilarity: Float,
    val angularProximityScore: Float, // 0.0 a 1.0 (peso de proximidade angular)
    val corridorClassification: AngularCorridorType,
    val isCompatibleAngularly: Boolean,
    val explanation: String
)

/**
 * Resultado da análise e cálculo do peso de tempo de espera no restaurante.
 */
data class KitchenWaitAnalysisResult(
    val waitMinutesPickup1: Int,
    val waitMinutesPickup2: Int,
    val totalWaitMinutes: Int,
    val peakHourSurgeApplied: Boolean,
    val peakMultiplier: Float,
    val thermalBagTrapMinutes: Int, // Tempo que o Pedido 1 fica na mochila enquanto o motoboy aguarda o Pedido 2
    val thermalBagSafetyScore: Int, // 0 a 100
    val kitchenWaitWeightFactor: Float, // 0.05 a 1.0 (fator de peso atenuante)
    val isKitchenAcceptable: Boolean,
    val riskLevel: KitchenRiskLevel,
    val explanation: String
)

/**
 * Pontuação composta final do algoritmo de otimização de mesclagem.
 */
data class OptimizationCompositeScore(
    val overallScore: Float, // 0.0 a 100.0
    val angularResult: AngularProximityResult,
    val kitchenResult: KitchenWaitAnalysisResult,
    val isCrossPlatform: Boolean,
    val crossPlatformBonus: Float,
    val transitEfficiencyFactor: Float,
    val isApproved: Boolean,
    val recommendationSummary: String
)

/**
 * AngularProximityKitchenMergingOptimizer
 *
 * Algoritmo especializado de otimização de mesclagens de entregas multi-plataforma
 * (iFood, 99Food, Uber Driver, Rappi).
 *
 * Princípios Matemáticos:
 * 1. Proximidade Angular (Vetor Direcional & Ângulo de Abertura):
 *    Calcula os azimutes geodésicos das rotas individuais (P1->D1 e P2->D2) e a abertura angular
 *    das entregas a partir do ponto comum. Aplica penalidade exponencial a divergências > 35°
 *    e veto estrito a rotas em contra-fluxo (> 70°).
 *
 * 2. Peso da Espera de Cozinha (Kitchen Wait Weight Factor & Thermal Degradation):
 *    Calcula a estimativa de balcão de cada restaurante considerando categoria (Shopping, Pizzeria, Fast-Food,
 *    Farmácia) e horário de pico. Modela a curva de decaimento térmico da comida do Pedido 1 confinada na mochila
 *    enquanto o entregador aguarda o preparo do Pedido 2.
 */
object AngularProximityKitchenMergingOptimizer {

    // Expoente de priorização angular (quanto maior, mais severamente penaliza desvios de rumo)
    private const val ANGULAR_PRIORITY_EXPONENT = 1.45

    // Expoente de decaimento da espera de cozinha (penalização não-linear quando espera ultrapassa 6 min)
    private const val KITCHEN_DECAY_EXPONENT = 1.80

    /**
     * Calcula a proximidade angular entre duas corridas de plataformas distintas ou mesma plataforma.
     */
    fun evaluateAngularProximity(
        o1: ScreenPoolOffer,
        o2: ScreenPoolOffer
    ): AngularProximityResult {
        // 1. Azimute geodésico de cada entrega (de coleta até entrega)
        val bearing1 = calculateBearing(o1.pickupLat, o1.pickupLng, o1.dropoffLat, o1.dropoffLng)
        val bearing2 = calculateBearing(o2.pickupLat, o2.pickupLng, o2.dropoffLat, o2.dropoffLng)

        // Diferença angular entre as trajetórias (0° a 180°)
        val corridorAngleDiff = calculateAngularDifference(bearing1, bearing2)

        // 2. Ângulo de abertura das duas entregas a partir da segunda coleta (P2 -> D1 vs P2 -> D2)
        val bearingP2toD1 = calculateBearing(o2.pickupLat, o2.pickupLng, o1.dropoffLat, o1.dropoffLng)
        val destinationAperture = calculateAngularDifference(bearingP2toD1, bearing2)

        // 3. Similaridade Cosseno das direções
        val cosCorridor = cos(Math.toRadians(corridorAngleDiff.toDouble())).toFloat().coerceIn(-1f, 1f)
        val cosAperture = cos(Math.toRadians(destinationAperture.toDouble())).toFloat().coerceIn(-1f, 1f)

        // Similaridade combinada orientada para frente
        val combinedCos = if (cosCorridor > 0f && cosAperture > 0f) {
            (cosCorridor * 0.65f + cosAperture * 0.35f)
        } else {
            max(0f, cosCorridor * cosAperture)
        }

        // 4. Score de Proximidade Angular normalizado com curva exponencial de prioridade
        val rawAngularScore = max(0f, (180f - corridorAngleDiff) / 180f)
        val angularProximityScore = (rawAngularScore.toDouble().pow(ANGULAR_PRIORITY_EXPONENT)).toFloat()
            .coerceIn(0f, 1f)

        // 5. Classificação do corredor viário
        val corridorType = when {
            corridorAngleDiff <= 15f && destinationAperture <= 20f -> AngularCorridorType.COLINEAR_HIGHWAY
            corridorAngleDiff <= 32f -> AngularCorridorType.PARALLEL_CORRIDOR
            corridorAngleDiff <= 50f -> AngularCorridorType.ADJACENT_SECTOR
            corridorAngleDiff <= 70f -> AngularCorridorType.TRANSVERSAL_DIVERGENT
            else -> AngularCorridorType.OPPOSITE_BACKTRACKING
        }

        // Compatibilidade angular: aprovada se ângulo for menor que 65° (ou até 70° se entregas muito próximas)
        val distBetweenDropoffsKm = calculateGeodeticKm(o1.dropoffLat, o1.dropoffLng, o2.dropoffLat, o2.dropoffLng)
        val isCompatible = corridorAngleDiff <= 65f || (corridorAngleDiff <= 75f && distBetweenDropoffsKm <= 1.0)

        val explanation = when (corridorType) {
            AngularCorridorType.COLINEAR_HIGHWAY ->
                "Alinhamento linear perfeito (${corridorAngleDiff.roundToInt()}°): ambos os destinos seguem na mesma via expressa/avenida radial."
            AngularCorridorType.PARALLEL_CORRIDOR ->
                "Corredor paralelo forte (${corridorAngleDiff.roundToInt()}°): divergência mínima de percurso viário."
            AngularCorridorType.ADJACENT_SECTOR ->
                "Bairros adjacentes (${corridorAngleDiff.roundToInt()}°): entrega complementar com desvio transversal contido."
            AngularCorridorType.TRANSVERSAL_DIVERGENT ->
                "Ângulo transversal (${corridorAngleDiff.roundToInt()}°): desvio considerável; viável apenas com alta tarifa."
            AngularCorridorType.OPPOSITE_BACKTRACKING ->
                "Veto angular (${corridorAngleDiff.roundToInt()}°): rotas em direções opostas (risco crítico de contra-fluxo)."
        }

        return AngularProximityResult(
            bearingP1toD1 = bearing1,
            bearingP2toD2 = bearing2,
            corridorAngleDiffDeg = corridorAngleDiff,
            destinationApertureDeg = destinationAperture,
            angularCosineSimilarity = combinedCos,
            angularProximityScore = angularProximityScore,
            corridorClassification = corridorType,
            isCompatibleAngularly = isCompatible,
            explanation = explanation
        )
    }

    /**
     * Calcula o peso do tempo de espera nos restaurantes como fator atenuante na mesclagem.
     */
    fun evaluateKitchenWaitWeight(
        o1: ScreenPoolOffer,
        o2: ScreenPoolOffer,
        transitP1toP2Minutes: Double
    ): KitchenWaitAnalysisResult {
        // 1. Horário de pico (Almoço 11:45-14:00, Jantar 19:15-22:00)
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val timeDecimal = hour + (minute / 60.0)

        val isLunchPeak = timeDecimal in 11.75..14.0
        val isDinnerPeak = timeDecimal in 19.25..22.0
        val isPeak = isLunchPeak || isDinnerPeak
        val peakMultiplier = if (isPeak) 1.35f else 1.0f

        // 2. Estimativa individual de balcão
        val baseWait1 = estimateSingleRestaurantWait(o1.restaurantName, o1.pickupAddress)
        val baseWait2 = estimateSingleRestaurantWait(o2.restaurantName, o2.pickupAddress)

        val wait1 = (baseWait1 * peakMultiplier).roundToInt().coerceAtLeast(2)
        val wait2 = (baseWait2 * peakMultiplier).roundToInt().coerceAtLeast(2)
        val totalWait = wait1 + wait2

        // 3. Risco de Confinamento Térmico do Pedido 1 na Mochila:
        // O Pedido 1 fica na bag durante: Deslocamento P1->P2 + Tempo de Espera em P2
        val thermalBagTrap = (transitP1toP2Minutes + wait2).roundToInt()

        // Score de segurança térmica (100 = comida impecável e quente; < 50 = perigo de esfriar)
        val thermalBagScore = (100 - (thermalBagTrap * 4)).coerceIn(5, 100)

        // 4. Fator de Peso Matemático da Cozinha (Kitchen Wait Weight Factor: W_wait)
        // Função sigmoide invertida: para esperas curtas (< 5 min), W_wait ~ 0.95 - 1.0;
        // para esperas longas (> 12 min), cai drasticamente para < 0.40.
        val waitPenaltyBase = (wait2.toDouble() / 6.0).pow(KITCHEN_DECAY_EXPONENT)
        val rawWeightFactor = (1.0 / (1.0 + (waitPenaltyBase * 0.45))).toFloat()
        val thermalModifier = (thermalBagScore / 100f).coerceIn(0.2f, 1f)
        val kitchenWaitWeight = (rawWeightFactor * thermalModifier).coerceIn(0.08f, 1.0f)

        // 5. Nível de risco
        val riskLevel = when {
            wait2 <= 4 && thermalBagTrap <= 7 -> KitchenRiskLevel.MINIMAL_QUICK_GRAB
            wait2 <= 8 && thermalBagTrap <= 13 -> KitchenRiskLevel.ACCEPTABLE_NORMAL
            wait2 <= 13 || thermalBagTrap <= 18 -> KitchenRiskLevel.HIGH_DELAY_RISK
            else -> KitchenRiskLevel.CRITICAL_COLD_FOOD
        }

        val isAcceptable = riskLevel != KitchenRiskLevel.CRITICAL_COLD_FOOD && wait2 <= 15

        val explanation = when (riskLevel) {
            KitchenRiskLevel.MINIMAL_QUICK_GRAB ->
                "Espera rápida (~${wait2}m no 2º ponto). Comida #1 fica apenas ~${thermalBagTrap}m na bag. Fator peso: ${String.format(Locale.US, "%.2f", kitchenWaitWeight)}."
            KitchenRiskLevel.ACCEPTABLE_NORMAL ->
                "Espera padrão (~${wait2}m). Retenção térmica de ~${thermalBagTrap}m compatível com embalagem padrão. Fator peso: ${String.format(Locale.US, "%.2f", kitchenWaitWeight)}."
            KitchenRiskLevel.HIGH_DELAY_RISK ->
                "Atenção: ~${wait2}m de espera em ${o2.restaurantName}. Risco de esfriar pedido #1 (${thermalBagTrap}m na bag). Fator peso reduzido para ${String.format(Locale.US, "%.2f", kitchenWaitWeight)}."
            KitchenRiskLevel.CRITICAL_COLD_FOOD ->
                "Veto por espera excessiva: ~${wait2}m no balcão e ~${thermalBagTrap}m de comida presa na mochila. Risco extremo de reclamação."
        }

        return KitchenWaitAnalysisResult(
            waitMinutesPickup1 = wait1,
            waitMinutesPickup2 = wait2,
            totalWaitMinutes = totalWait,
            peakHourSurgeApplied = isPeak,
            peakMultiplier = peakMultiplier,
            thermalBagTrapMinutes = thermalBagTrap,
            thermalBagSafetyScore = thermalBagScore,
            kitchenWaitWeightFactor = kitchenWaitWeight,
            isKitchenAcceptable = isAcceptable,
            riskLevel = riskLevel,
            explanation = explanation
        )
    }

    /**
     * Motor completo de otimização combinada:
     * Calcula proximidade angular + peso de espera de cozinha + bônus de plataformas distintas.
     */
    fun optimizeDeliveryMerge(
        o1: ScreenPoolOffer,
        o2: ScreenPoolOffer,
        costPerKm: Double
    ): OptimizationCompositeScore {
        // Distância entre coletas para estimar trânsito P1 -> P2
        val distBetweenPickupsKm = calculateGeodeticKm(o1.pickupLat, o1.pickupLng, o2.pickupLat, o2.pickupLng)
        val transitP1toP2Min = (distBetweenPickupsKm / 28.0) * 60.0

        // 1. Proximidade Angular entre destinos de diferentes plataformas
        val angularResult = evaluateAngularProximity(o1, o2)

        // 2. Fator de peso da espera de cozinha
        val kitchenResult = evaluateKitchenWaitWeight(o1, o2, transitP1toP2Min)

        // 3. Diferenciação de plataformas (Cross-Platform Multiplier)
        val isCrossPlatform = !o1.appName.equals(o2.appName, ignoreCase = true)
        val crossPlatformBonus = if (isCrossPlatform) 1.15f else 1.0f

        // 4. Eficiência de deslocamento viário (desvio vs rota solo)
        val maxDirectKm = max(o1.distanceKm, o2.distanceKm)
        val distBetweenDropoffsKm = calculateGeodeticKm(o1.dropoffLat, o1.dropoffLng, o2.dropoffLat, o2.dropoffLng)
        val compositeExtraKm = (distBetweenPickupsKm + distBetweenDropoffsKm) * 1.18
        val totalKm = maxDirectKm + compositeExtraKm
        val transitEfficiency = (maxDirectKm / totalKm).toFloat().coerceIn(0.4f, 1.0f)

        // 5. Lucro líquido financeiro da combinação
        val grossRev = o1.value + o2.value
        val fuelCost = totalKm * costPerKm
        val netProfit = max(0.0, grossRev - fuelCost)
        val totalMinutes = ((totalKm / 28.0) * 60.0) + kitchenResult.totalWaitMinutes
        val netPerHour = if (totalMinutes > 0) (netProfit / totalMinutes) * 60.0 else 0.0

        // 6. Cálculo da Função Objetivo de Otimização:
        // Score = (Rentabilidade Normalizada) * (Proximidade Angular)^1.45 * (Peso de Cozinha)^1.80 * BônusMultiplataforma * Eficiência
        val financialBaseScore = (netPerHour / 40.0).toFloat().coerceIn(0.2f, 2.5f)
        val combinedScore = (
            financialBaseScore * 40f *
            angularResult.angularProximityScore *
            kitchenResult.kitchenWaitWeightFactor *
            crossPlatformBonus *
            transitEfficiency
        ).coerceIn(0f, 100f)

        // Critérios de aprovação estrita
        val isApproved = angularResult.isCompatibleAngularly &&
                kitchenResult.isKitchenAcceptable &&
                combinedScore >= 45f &&
                compositeExtraKm <= 2.8

        val platformTag = if (isCrossPlatform) "${o1.appName} ⨁ ${o2.appName} (Multi-App)" else "${o1.appName} (Mesmo App)"
        val summary = if (isApproved) {
            "✅ Otimização Aprovada [$platformTag]: ${angularResult.corridorClassification.label} com peso de balcão de ${String.format(Locale.US, "%.2f", kitchenResult.kitchenWaitWeightFactor)} e retorno de R$ ${String.format(Locale.US, "%.2f", netPerHour)}/h."
        } else {
            "❌ Rejeitada: Desalinhamento angular (${angularResult.corridorAngleDiffDeg.roundToInt()}°) ou retenção na cozinha (${kitchenResult.waitMinutesPickup2}m)."
        }

        return OptimizationCompositeScore(
            overallScore = combinedScore,
            angularResult = angularResult,
            kitchenResult = kitchenResult,
            isCrossPlatform = isCrossPlatform,
            crossPlatformBonus = crossPlatformBonus,
            transitEfficiencyFactor = transitEfficiency,
            isApproved = isApproved,
            recommendationSummary = summary
        )
    }

    /**
     * Estima o tempo base de espera por tipo de estabelecimento e localização.
     */
    fun estimateSingleRestaurantWait(restaurantName: String, address: String): Int {
        val text = (restaurantName + " " + address).lowercase(Locale.ROOT)
        return when {
            // Shoppings e centros comerciais (maior gargalo logístico urbano)
            text.contains("shopping") || text.contains("center") || text.contains("bourbon") ||
            text.contains("iguatemi") || text.contains("mall") || text.contains("eldorado") ||
            text.contains("morumbi") || text.contains("piazza") || text.contains("plaza") -> 12

            // Restaurantes lentos ou à la carte
            text.contains("pizzaria") || text.contains("churrascaria") || text.contains("barbecue") ||
            text.contains("restaurante") || text.contains("cantina") || text.contains("parrilla") -> 9

            // Dark Kitchens com alta demanda
            text.contains("dark kitchen") || text.contains("kitchen hub") || text.contains("cozinha virtual") -> 7

            // Padarias e lanchonetes de balcão rápido
            text.contains("padaria") || text.contains("panificadora") || text.contains("cafe") ||
            text.contains("lanchonete") || text.contains("pastelaria") -> 5

            // Fast foods conhecidos
            text.contains("mcdonald") || text.contains("burger king") || text.contains("habib") ||
            text.contains("subway") || text.contains("bobs") || text.contains("kfc") -> 3

            // Farmácias e conveniências (retirada expressa na prateleira)
            text.contains("drogasil") || text.contains("droga raia") || text.contains("farmacia") ||
            text.contains("drogaria") || text.contains("oxxo") || text.contains("conveniencia") ||
            text.contains("pague menos") || text.contains("sao paulo") -> 2

            // Estabelecimento de rua médio
            else -> 5
        }
    }

    /**
     * Calcula o azimute geodésico inicial (rumo em graus [0..360]) entre dois pontos.
     */
    fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaLambda = Math.toRadians(lon2 - lon1)

        val y = sin(deltaLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)
        val theta = atan2(y, x)
        val bearing = (Math.toDegrees(theta) + 360.0) % 360.0
        return bearing.toFloat()
    }

    /**
     * Retorna a menor diferença angular entre dois azimutes (entre 0° e 180°).
     */
    fun calculateAngularDifference(bearing1: Float, bearing2: Float): Float {
        val diff = abs(bearing1 - bearing2) % 360f
        return if (diff > 180f) 360f - diff else diff
    }

    /**
     * Distância geodésica em quilômetros.
     */
    fun calculateGeodeticKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0] / 1000.0
    }
}
