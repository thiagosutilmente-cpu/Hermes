package com.example

import android.content.Context
import android.location.Location
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Oferta individual capturada de listas/feeds abertos de aplicativos de entrega parceiros.
 */
data class ScreenPoolOffer(
    val id: String = UUID.randomUUID().toString(),
    val appName: String,
    val packageName: String,
    val restaurantName: String,
    val pickupAddress: String,
    val dropoffAddress: String,
    val value: Double,
    val distanceKm: Double,
    val estimatedMinutes: Int = (distanceKm * 3.5).roundToInt().coerceAtLeast(8),
    val pickupLat: Double = -23.561684,
    val pickupLng: Double = -46.655981,
    val dropoffLat: Double = -23.561684 + (distanceKm * 0.007),
    val dropoffLng: Double = -46.655981 + (distanceKm * 0.007),
    val actionText: String = "Aceitar",
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Combo de entrega resultante da mesclagem profunda entre pedidos compatíveis no mesmo corredor.
 */
data class MergedDeliveryCombo(
    val id: String = UUID.randomUUID().toString(),
    val primaryOffer: ScreenPoolOffer,
    val secondaryOffer: ScreenPoolOffer,
    val totalGrossRevenue: Double,
    val combinedDistanceKm: Double,
    val extraDeviationMeters: Int,
    val estimatedExtraMinutes: Int,
    val fuelCostEstimated: Double,
    val netProfit: Double,
    val netProfitPerHour: Double,
    val slaSafetyScore: Int,
    val executionSteps: List<String>,
    val recommendationReason: String,
    val isDiamondDeal: Boolean,
    val corridorAlignmentDeg: Float = 0f,
    val corridorLabel: String = "Mesmo Corredor Viário",
    val pickupDeviationMeters: Int = 0,
    val dropoffDeviationMeters: Int = 0,
    val haversineResult: HaversineDeviationResult? = null,
    val angularResult: AngularProximityResult? = null,
    val kitchenResult: KitchenWaitAnalysisResult? = null,
    val optimizationScore: Float = 0f
)

/**
 * DeepOrderPoolMergingEngine
 *
 * Motor de agregação combinatória e mesclagem profunda de corridas disponíveis nos apps de entrega.
 *
 * Em vez de depender apenas de popups isolados, este motor ingere listas inteiras
 * de corridas abertas capturadas na tela (iFood Sob Demanda, 99Food Radar, Rappi Hub, Lalamove),
 * cruzando com a localização GPS do condutor e quaisquer entregas já em transporte.
 */
class DeepOrderPoolMergingEngine private constructor(private val context: Context) {

    companion object {
        private const val TAG = "DeepOrderPoolEngine"

        @Volatile
        private var INSTANCE: DeepOrderPoolMergingEngine? = null

        fun getInstance(context: Context): DeepOrderPoolMergingEngine {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DeepOrderPoolMergingEngine(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    // Lista de todas as ofertas ativas escaneadas dos feeds
    private val _scannedPoolOffers = MutableStateFlow<List<ScreenPoolOffer>>(emptyList())
    val scannedPoolOffers: StateFlow<List<ScreenPoolOffer>> = _scannedPoolOffers.asStateFlow()

    // Melhores combos rankeados de mesclagem profunda
    private val _rankedCombos = MutableStateFlow<List<MergedDeliveryCombo>>(emptyList())
    val rankedCombos: StateFlow<List<MergedDeliveryCombo>> = _rankedCombos.asStateFlow()

    // Histórico Permanente de Combos do Turno (Nunca somem sozinhos)
    private val _comboHistory = MutableStateFlow<List<MergedDeliveryCombo>>(emptyList())
    val comboHistory: StateFlow<List<MergedDeliveryCombo>> = _comboHistory.asStateFlow()

    // Trava de Retenção Tática: Mantém as mesclagens fixas na tela até decisão manual
    private val _isHoldModeEnabled = MutableStateFlow(true)
    val isHoldModeEnabled: StateFlow<Boolean> = _isHoldModeEnabled.asStateFlow()

    // Alerta de Imprevisto Ativo (Race condition, atraso em restaurante ou cancelamento)
    private val _activeIncidentNotice = MutableStateFlow<String?>(null)
    val activeIncidentNotice: StateFlow<String?> = _activeIncidentNotice.asStateFlow()

    // Estatísticas de varredura
    private val _lastScanTimestamp = MutableStateFlow(0L)
    val lastScanTimestamp: StateFlow<Long> = _lastScanTimestamp.asStateFlow()

    private val _scannedAppsCount = MutableStateFlow(0)
    val scannedAppsCount: StateFlow<Int> = _scannedAppsCount.asStateFlow()

    // Conjunto de IDs de combos dispensados manualmente pelo motoboy (evita reaparecimento cíclico)
    private val dismissedComboIds = mutableSetOf<String>()

    fun toggleHoldMode(enabled: Boolean) {
        _isHoldModeEnabled.value = enabled
        Log.i(TAG, "🛡️ Trava de Retenção Tática: ${if (enabled) "ATIVADA (Combos Fixos)" else "DESATIVADA"}")
    }

    fun dismissCombo(comboId: String) {
        dismissedComboIds.add(comboId)
        if (dismissedComboIds.size > 200) {
            val toRemove = dismissedComboIds.take(50).toSet()
            dismissedComboIds.removeAll(toRemove)
        }
        _rankedCombos.value = _rankedCombos.value.filterNot { it.id == comboId }
    }

    fun dismissIncidentNotice() {
        _activeIncidentNotice.value = null
    }

    fun generateComboId(o1: ScreenPoolOffer, o2: ScreenPoolOffer): String {
        val k1 = "${o1.appName}_${o1.restaurantName}_${(o1.value * 100).toInt()}"
        val k2 = "${o2.appName}_${o2.restaurantName}_${(o2.value * 100).toInt()}"
        return if (k1 <= k2) "combo_${k1}__${k2}" else "combo_${k2}__${k1}"
    }

    /**
     * Resolução de Imprevisto 1: Outro motoboy aceitou uma das pontas do combo (Race Condition).
     * Desmonta suavemente sem pânico e preserva a corrida principal.
     */
    fun handleRaceConditionLost(lostAppName: String, remainingRestaurant: String, remainingGainPerKm: Double) {
        val notice = "⚠️ Oferta do $lostAppName foi aceita por outro motoboy. Mantendo rota solo para $remainingRestaurant (R$ ${String.format(Locale.US, "%.2f", remainingGainPerKm)}/km)."
        _activeIncidentNotice.value = notice
        try {
            TextToSpeechManager.getInstance(context).speakVoiceAlert(
                "Atenção: A corrida do $lostAppName expirou. Rota solo recalculada mantendo apenas $remainingRestaurant."
            )
            HapticFeedbackHelper.vibrateWarning(context)
        } catch (_: Exception) {}
    }

    /**
     * Resolução de Imprevisto 2: Restaurante com atraso excessivo (>8 min).
     */
    fun reportRestaurantDelay(restaurantName: String, minutesDelayed: Int) {
        val notice = "⏱️ Alerta de Atraso: $restaurantName demorando ${minutesDelayed} min. Risco de esfriar o 2º pedido do combo."
        _activeIncidentNotice.value = notice
        try {
            TextToSpeechManager.getInstance(context).speakVoiceAlert(
                "Alerta: Restaurante $restaurantName com atraso excessivo. Avalie cancelar a segunda coleta."
            )
        } catch (_: Exception) {}
    }

    /**
     * Atualiza a lista de ofertas escaneadas de um app específico e recalcula a matriz combinatória.
     */
    @Synchronized
    fun ingestPoolOffers(appName: String, offers: List<ScreenPoolOffer>) {
        if (offers.isEmpty()) return

        val currentList = _scannedPoolOffers.value.toMutableList()
        // Se a Trava de Retenção estiver ativa, mantém por até 45 minutos. Senão, 8 minutos.
        val retentionMillis = if (_isHoldModeEnabled.value) (45 * 60 * 1000L) else (8 * 60 * 1000L)
        val cutoff = System.currentTimeMillis() - retentionMillis
        currentList.removeAll { it.timestamp < cutoff }

        // Adiciona as novas filtrando dados corrompidos ou zerados
        val validOffers = offers.filter { it.value > 0.0 && it.distanceKm > 0.05 && it.distanceKm.isFinite() }
        currentList.addAll(validOffers)
        _scannedPoolOffers.value = currentList.distinctBy { "${it.appName}_${it.restaurantName}_${it.value}" }
        _lastScanTimestamp.value = System.currentTimeMillis()
        _scannedAppsCount.value = _scannedPoolOffers.value.map { it.appName }.distinct().size

        Log.i(TAG, "📦 Ingestão de pool: ${validOffers.size} ofertas válidas de $appName. Total no banco: ${_scannedPoolOffers.value.size}")
        recalculateOptimalCombos()
    }

    /**
     * Recalcula os melhores combos combinando pares de ofertas e calculando lucro líquido real.
     */
    fun recalculateOptimalCombos() {
        val pool = _scannedPoolOffers.value.toList()
        if (pool.size < 2) {
            // Se a Trava de Retenção estiver ativa, NÃO ZERA a tela bruscamente
            if (!_isHoldModeEnabled.value) {
                _rankedCombos.value = emptyList()
            }
            return
        }

        CoroutineScope(Dispatchers.Default).launch {
            val fuelPrice = FuelManager.currentFuelPrice.coerceAtLeast(1.0)
            val kmPerLiter = max(FuelManager.getEffectiveKmPerLiter(), 10.0)
            val costPerKm = (fuelPrice / kmPerLiter).coerceAtLeast(0.01)

            val currentLoc = LocationService.globalLocationState.value
            val combos = mutableListOf<MergedDeliveryCombo>()

            // Combina pares de ofertas (de apps diferentes ou do mesmo app se rota compatível)
            for (i in 0 until pool.size) {
                for (j in (i + 1) until pool.size) {
                    val o1 = pool[i]
                    val o2 = pool[j]
                    val expectedId = generateComboId(o1, o2)
                    if (dismissedComboIds.contains(expectedId)) {
                        continue
                    }

                    val combo = evaluatePair(o1, o2, costPerKm, currentLoc)
                    if (combo != null && combo.slaSafetyScore >= 70 && combo.extraDeviationMeters <= 2200) {
                        combos.add(combo)
                    }
                }
            }

            // Ordena priorizando Combo Diamante, Maior Score de Otimização (Angular + Cozinha), Menor Desvio e Lucro/Hora
            val sortedCombos = combos.sortedWith(
                compareByDescending<MergedDeliveryCombo> { it.isDiamondDeal }
                    .thenByDescending { it.optimizationScore }
                    .thenBy { it.extraDeviationMeters }
                    .thenBy { it.corridorAlignmentDeg }
                    .thenByDescending { it.netProfitPerHour }
                    .thenByDescending { it.slaSafetyScore }
            ).take(8)

            // Se encontrou novos combos, atualiza o ranking e alimenta o histórico permanente
            if (sortedCombos.isNotEmpty()) {
                _rankedCombos.value = sortedCombos
                val updatedHistory = (sortedCombos + _comboHistory.value).distinctBy { it.id }.take(50)
                _comboHistory.value = updatedHistory
                Log.i(TAG, "⚡ Combos calculados: ${combos.size} válidos. Histórico: ${updatedHistory.size}")
            } else if (!_isHoldModeEnabled.value) {
                _rankedCombos.value = emptyList()
            }

        // Se houver um combo diamante espetacular, avisa via áudio/voz se não estiver em alta velocidade
        val top = sortedCombos.firstOrNull()
        if (top != null && top.isDiamondDeal && !currentLoc.isSafetyLockActive) {
            val text = "Combo Diamante de Corredor detectado: ${top.primaryOffer.appName} mais ${top.secondaryOffer.appName}. Ganho total de R$ ${String.format(Locale.US, "%.2f", top.totalGrossRevenue)} com lucro de R$ ${String.format(Locale.US, "%.2f", top.netProfit)} e desvio de apenas ${top.extraDeviationMeters} metros."
            try {
                TextToSpeechManager.getInstance(context).speakVoiceAlert(text)
            } catch (_: Exception) {}

            try {
                if (OverlayWindowManager.canDrawOverlays(context)) {
                    val pricePerKm = if (top.combinedDistanceKm > 0) top.totalGrossRevenue / top.combinedDistanceKm else 0.0
                    OverlayWindowManager.getInstance(context).showTacticalOffer(
                        OverlayWindowManager.FloatingTacticalOffer(
                            appName = "${top.primaryOffer.appName} + ${top.secondaryOffer.appName}",
                            packageName = top.primaryOffer.packageName,
                            value = top.totalGrossRevenue,
                            distanceKm = top.combinedDistanceKm,
                            pricePerKm = pricePerKm,
                            isGoodDeal = true,
                            dealLabel = String.format(Locale.US, "💎 CORREDOR +R$ %.2f (+%dm)", top.netProfit, top.extraDeviationMeters),
                            destination = "${top.primaryOffer.dropoffAddress} • +${top.extraDeviationMeters}m",
                            pickup = "${top.primaryOffer.restaurantName} & ${top.secondaryOffer.restaurantName}",
                            hasDualRoute = true,
                            dualRouteAppName = top.secondaryOffer.appName,
                            dualRouteExtraGain = top.secondaryOffer.value,
                            haversineDeviationMeters = top.extraDeviationMeters,
                            haversineExtraMinutes = top.estimatedExtraMinutes.toDouble()
                        )
                    )
                }
            } catch (_: Exception) {}
        }
    }
}

    /**
     * Avalia a compatibilidade física e financeira de duas corridas simultâneas com foco no corredor geográfico.
     */
    private fun evaluatePair(
        o1: ScreenPoolOffer,
        o2: ScreenPoolOffer,
        costPerKm: Double,
        currentLoc: LocationSpeedState
    ): MergedDeliveryCombo? {
        val grossRev = o1.value + o2.value

        // 1. Distância geodésica preliminar entre coletas e entregas
        val distBetweenPickups = calculateGeodeticKm(o1.pickupLat, o1.pickupLng, o2.pickupLat, o2.pickupLng)
        val distBetweenDropoffs = calculateGeodeticKm(o1.dropoffLat, o1.dropoffLng, o2.dropoffLat, o2.dropoffLng)

        // Se coletas ou entregas forem excessivamente distantes (acima de 2.8 km), descarta
        if (distBetweenPickups > 2.8 || distBetweenDropoffs > 3.2) {
            return null
        }

        // 2. Executa o algoritmo de otimização especializada:
        // Prioriza proximidade angular entre os pontos de entrega de diferentes plataformas
        // e calcula o tempo estimado de espera no restaurante como fator de peso (kitchen wait weight factor)
        val optScore = AngularProximityKitchenMergingOptimizer.optimizeDeliveryMerge(o1, o2, costPerKm)
        if (!optScore.isApproved) {
            return null
        }

        val angularResult = optScore.angularResult
        val kitchenResult = optScore.kitchenResult
        val angularDiff = angularResult.corridorAngleDiffDeg
        val kitchenDelay1 = kitchenResult.waitMinutesPickup1
        val kitchenDelay2 = kitchenResult.waitMinutesPickup2
        val totalKitchenWaitMinutes = kitchenResult.totalWaitMinutes

        // 3. Rota combinada estimada com Otimização TSP de 4 Paradas (Traveling Salesperson):
        val maxDirect = max(o1.distanceKm, o2.distanceKm)

        // Sequência A (FIFO): Coleta 1 -> Coleta 2 -> Entrega 1 -> Entrega 2
        val distP2toD1 = calculateGeodeticKm(o2.pickupLat, o2.pickupLng, o1.dropoffLat, o1.dropoffLng)
        val routeDistA = distBetweenPickups + distP2toD1 + distBetweenDropoffs

        // Sequência B (LIFO / Dropoff Intermediário): Coleta 1 -> Coleta 2 -> Entrega 2 -> Entrega 1
        val distP2toD2 = o2.distanceKm
        val routeDistB = distBetweenPickups + distP2toD2 + distBetweenDropoffs

        // Seleciona a permutação com menor quilometragem e menor tempo de comida na bag
        val isSequenceBFaster = routeDistB < (routeDistA - 0.35)
        val chosenSeqDesc = if (isSequenceBFaster) "LIFO (Entrega ${o2.appName} no caminho)" else "FIFO (Entrega ${o1.appName} prioritária)"

        // Distância combinada calculada a partir da sequência ótima
        val baseTourKm = if (isSequenceBFaster) routeDistB else routeDistA
        val extraKm = baseTourKm * 1.18
        val totalKm = maxDirect + extraKm
        val deviationMeters = (extraKm * 1000).roundToInt()
        val pickupDevMeters = (distBetweenPickups * 1000).roundToInt()
        val dropoffDevMeters = (distBetweenDropoffs * 1000).roundToInt()

        // Estimativa de tempo total somando deslocamento em trânsito e espera ponderada de balcão
        val transitMinutes = ((totalKm / 28.0) * 60.0).roundToInt().coerceAtLeast(4)
        val extraMinutes = ((extraKm / 28.0) * 60.0).roundToInt().coerceAtLeast(3)
        val totalMinutes = transitMinutes + totalKitchenWaitMinutes

        // Lucro líquido: Receita bruta menos o custo de combustível
        val fuelCost = totalKm * costPerKm
        val netProfit = (grossRev - fuelCost).let { if (it.isFinite()) it else 0.0 }
        val rawNetPerHour = if (totalMinutes > 0) (netProfit / totalMinutes) * 60.0 else 0.0
        val netProfitPerHour = if (rawNetPerHour.isFinite()) rawNetPerHour.coerceAtLeast(0.0) else 0.0

        // Score de Segurança SLA: Combina desvio, segurança térmica da bag e peso de proximidade angular
        val baseSla = 100 - (extraMinutes * 3) - (deviationMeters / 90)
        val slaScore = (
            baseSla * 0.40f +
            kitchenResult.thermalBagSafetyScore * 0.35f +
            angularResult.angularProximityScore * 25f
        ).roundToInt().coerceIn(10, 100)

        // Combo Diamante: rentabilidade líquida alta, alinhamento angular forte e balcão rápido
        val isDiamond = grossRev >= 26.0 &&
                netProfitPerHour >= 45.0 &&
                deviationMeters <= 1000 &&
                angularDiff <= 32f &&
                kitchenResult.riskLevel == KitchenRiskLevel.MINIMAL_QUICK_GRAB

        val corridorLabel = "${angularResult.corridorClassification.label} • Balcão ~${kitchenDelay2}m (Peso ${String.format(Locale.US, "%.2f", kitchenResult.kitchenWaitWeightFactor)})"

        val platformLabel = if (optScore.isCrossPlatform) "${o1.appName} + ${o2.appName} (Multi-App)" else o1.appName
        val steps = if (isSequenceBFaster) {
            listOf(
                "1. Coletar em ${o1.restaurantName} (${o1.appName}) • Balcão: ~${kitchenDelay1}m",
                "2. Coletar em ${o2.restaurantName} (${o2.appName}) • Desvio: +${pickupDevMeters}m (Balcão: ~${kitchenDelay2}m | Peso: ${String.format(Locale.US, "%.2f", kitchenResult.kitchenWaitWeightFactor)})",
                "3. Entregar em ${o2.dropoffAddress} (${o2.appName}) • Entrega intermediária no caminho",
                "4. Entregar em ${o1.dropoffAddress} (${o1.appName}) • Destino final do corredor (+${dropoffDevMeters}m | Bag: ~${kitchenResult.thermalBagTrapMinutes}m)"
            )
        } else {
            listOf(
                "1. Coletar em ${o1.restaurantName} (${o1.appName}) • Balcão: ~${kitchenDelay1}m",
                "2. Coletar em ${o2.restaurantName} (${o2.appName}) • Desvio: +${pickupDevMeters}m (Balcão: ~${kitchenDelay2}m | Peso: ${String.format(Locale.US, "%.2f", kitchenResult.kitchenWaitWeightFactor)})",
                "3. Entregar em ${o1.dropoffAddress} (${o1.appName}) • 1ª Entrega prioritária (Bag: ~${kitchenResult.thermalBagTrapMinutes}m)",
                "4. Entregar em ${o2.dropoffAddress} (${o2.appName}) • Desvio final: +${dropoffDevMeters}m"
            )
        }

        val reason = if (isDiamond) {
            "💎 Combo Diamante Multi-App [$platformLabel]: $corridorLabel com ganho líquido de R$ ${String.format(Locale.US, "%.2f", netProfitPerHour)}/h e retenção na bag de apenas ~${kitchenResult.thermalBagTrapMinutes}m."
        } else {
            "⚡ Mesclagem Otimizada [$platformLabel]: $corridorLabel com rendimento líquido de R$ ${String.format(Locale.US, "%.2f", netProfitPerHour)}/h (Score: ${optScore.overallScore.roundToInt()}/100)."
        }

        val driverLat = if (currentLoc.latitude != 0.0) currentLoc.latitude else -23.561684
        val driverLng = if (currentLoc.longitude != 0.0) currentLoc.longitude else -46.655981
        val driverSpeed = currentLoc.calculatedSpeedKmh

        // Avaliação de desvio geodésico Haversine e limite dinâmico de SLA
        val haversineEval = HaversineGeoDeviationCalculator.evaluateMultiPointRouteDeviation(
            driverLat = driverLat,
            driverLng = driverLng,
            driverSpeedKmh = driverSpeed,
            o1 = o1,
            o2 = o2,
            slaMinutes = 25
        )

        // Se o desvio Haversine violar o limite dinâmico com risco de atraso (CRÍTICO), descarta
        if (haversineEval.statusLevel == DeviationStatusLevel.CRITICAL_EXCESSIVE && !haversineEval.isWithinDynamicTolerance) {
            return null
        }

        return MergedDeliveryCombo(
            id = generateComboId(o1, o2),
            primaryOffer = o1,
            secondaryOffer = o2,
            totalGrossRevenue = grossRev,
            combinedDistanceKm = Math.round(totalKm * 10.0) / 10.0,
            extraDeviationMeters = haversineEval.netDeviationMeters,
            estimatedExtraMinutes = haversineEval.estimatedExtraMinutes.roundToInt(),
            fuelCostEstimated = Math.round(fuelCost * 100.0) / 100.0,
            netProfit = Math.round(netProfit * 100.0) / 100.0,
            netProfitPerHour = Math.round(netProfitPerHour * 100.0) / 100.0,
            slaSafetyScore = slaScore,
            executionSteps = steps,
            recommendationReason = reason,
            isDiamondDeal = isDiamond,
            corridorAlignmentDeg = angularDiff,
            corridorLabel = corridorLabel,
            pickupDeviationMeters = haversineEval.distancePickup1ToPickup2Meters,
            dropoffDeviationMeters = haversineEval.distanceDropoff1ToDropoff2Meters,
            haversineResult = haversineEval,
            angularResult = angularResult,
            kitchenResult = kitchenResult,
            optimizationScore = optScore.overallScore
        )
    }

    private fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaLambda = Math.toRadians(lon2 - lon1)

        val y = Math.sin(deltaLambda) * Math.cos(phi2)
        val x = Math.cos(phi1) * Math.sin(phi2) - Math.sin(phi1) * Math.cos(phi2) * Math.cos(deltaLambda)
        val theta = Math.atan2(y, x)
        val bearing = (Math.toDegrees(theta) + 360.0) % 360.0
        return bearing.toFloat()
    }

    private fun calculateAngularDifference(bearing1: Float, bearing2: Float): Float {
        val diff = Math.abs(bearing1 - bearing2) % 360f
        return if (diff > 180f) 360f - diff else diff
    }

    private fun calculateGeodeticKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return (results[0] / 1000.0)
    }

    /**
     * Estima o tempo de espera no balcão/cozinha com base no tipo de estabelecimento
     * e localização (Shoppings / Praças de Alimentação / Fast-Food).
     */
    fun estimateKitchenDelayMinutes(restaurantName: String, address: String): Int {
        val text = (restaurantName + " " + address).lowercase(Locale.ROOT)
        return when {
            // Shoppings e galerias: tempo de estacionar moto, subir até praça de alimentação
            text.contains("shopping") || text.contains("center") || text.contains("bourbon") ||
            text.contains("iguatemi") || text.contains("mall") || text.contains("eldorado") ||
            text.contains("morumbi") || text.contains("piazza") -> 10

            // Restaurantes lentos ou à la carte em horário de pico
            text.contains("pizzaria") || text.contains("churrascaria") || text.contains("barbecue") ||
            text.contains("restaurante") || text.contains("cantina") -> 8

            // Fast foods e conveniências rápidas
            text.contains("mcdonald") || text.contains("burger king") || text.contains("habib") ||
            text.contains("subway") || text.contains("drogasil") || text.contains("droga raia") ||
            text.contains("farmacia") || text.contains("conveniencia") -> 3

            // Padrão de rua / dark kitchen
            else -> 5
        }
    }

    /**
     * Limpa ofertas antigas ou reinicia o scanner.
     */
    fun clearPool() {
        _scannedPoolOffers.value = emptyList()
        _rankedCombos.value = emptyList()
    }
}
