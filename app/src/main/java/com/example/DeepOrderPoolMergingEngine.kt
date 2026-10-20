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
    val haversineResult: HaversineDeviationResult? = null
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

    // Estatísticas de varredura
    private val _lastScanTimestamp = MutableStateFlow(0L)
    val lastScanTimestamp: StateFlow<Long> = _lastScanTimestamp.asStateFlow()

    private val _scannedAppsCount = MutableStateFlow(0)
    val scannedAppsCount: StateFlow<Int> = _scannedAppsCount.asStateFlow()

    /**
     * Atualiza a lista de ofertas escaneadas de um app específico e recalcula a matriz combinatória.
     */
    @Synchronized
    fun ingestPoolOffers(appName: String, offers: List<ScreenPoolOffer>) {
        if (offers.isEmpty()) return

        val currentList = _scannedPoolOffers.value.toMutableList()
        // Remove ofertas anteriores deste mesmo app que tenham expirado (mais de 4 minutos)
        val cutoff = System.currentTimeMillis() - (4 * 60 * 1000L)
        currentList.removeAll { it.appName.equals(appName, ignoreCase = true) || it.timestamp < cutoff }

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
            _rankedCombos.value = emptyList()
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

                    val combo = evaluatePair(o1, o2, costPerKm, currentLoc)
                    if (combo != null && combo.slaSafetyScore >= 70 && combo.extraDeviationMeters <= 2200) {
                        combos.add(combo)
                    }
                }
            }

            // Ordena priorizando Menor Desvio no mesmo corredor e Maior Lucro Líquido por Hora
            val sortedCombos = combos.sortedWith(
                compareByDescending<MergedDeliveryCombo> { it.isDiamondDeal }
                    .thenBy { it.extraDeviationMeters }
                    .thenBy { it.corridorAlignmentDeg }
                    .thenByDescending { it.netProfitPerHour }
                    .thenByDescending { it.slaSafetyScore }
            ).take(8)

            _rankedCombos.value = sortedCombos
            Log.i(TAG, "⚡ Combos calculados: ${combos.size} válidos. Top ${sortedCombos.size} exibidos no mesmo corredor.")

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

        // Distância entre coletas
        val distBetweenPickups = calculateGeodeticKm(o1.pickupLat, o1.pickupLng, o2.pickupLat, o2.pickupLng)
        // Distância entre entregas
        val distBetweenDropoffs = calculateGeodeticKm(o1.dropoffLat, o1.dropoffLng, o2.dropoffLat, o2.dropoffLng)

        // Se coletas ou entregas forem excessivamente distantes (acima de 2.8 km), descarta
        if (distBetweenPickups > 2.8 || distBetweenDropoffs > 3.2) {
            return null
        }

        // Análise de Vetor Direcional (Azimute de Deslocamento das duas corridas)
        val bearing1 = calculateBearing(o1.pickupLat, o1.pickupLng, o1.dropoffLat, o1.dropoffLng)
        val bearing2 = calculateBearing(o2.pickupLat, o2.pickupLng, o2.dropoffLat, o2.dropoffLng)
        val angularDiff = calculateAngularDifference(bearing1, bearing2)

        // Se as duas rotas seguem em direções opostas (divergência angular > 70 graus) e a entrega não for hiper-próxima, descarta para evitar contra-fluxo
        if (angularDiff > 70f && distBetweenDropoffs > 1.2) {
            return null
        }

        // Rota combinada estimada: Coleta 1 -> Coleta 2 -> Entrega 1 -> Entrega 2
        // Distância combinada: maior perna + o desvio entre coletas e entregas com fator de malha urbana (1.18x)
        val maxDirect = max(o1.distanceKm, o2.distanceKm)
        val extraKm = (distBetweenPickups + distBetweenDropoffs) * 1.18
        val totalKm = maxDirect + extraKm
        val deviationMeters = (extraKm * 1000).roundToInt()
        val pickupDevMeters = (distBetweenPickups * 1000).roundToInt()
        val dropoffDevMeters = (distBetweenDropoffs * 1000).roundToInt()

        // Estimativa de tempo total e extra
        val extraMinutes = ((extraKm / 28.0) * 60.0).roundToInt().coerceAtLeast(3)
        val totalMinutes = max(o1.estimatedMinutes, o2.estimatedMinutes) + extraMinutes

        // Lucro líquido: Receita bruta menos o custo de combustível
        val fuelCost = totalKm * costPerKm
        val netProfit = (grossRev - fuelCost).let { if (it.isFinite()) it else 0.0 }
        val rawNetPerHour = if (totalMinutes > 0) (netProfit / totalMinutes) * 60.0 else 0.0
        val netProfitPerHour = if (rawNetPerHour.isFinite()) rawNetPerHour.coerceAtLeast(0.0) else 0.0

        // Score de Segurança SLA (Evita atraso na entrega de comida quente)
        val anglePenalty = (angularDiff / 10).roundToInt()
        val slaScore = (100 - (extraMinutes * 3) - (deviationMeters / 90) - anglePenalty).coerceIn(10, 100)

        // Combo Diamante: mais de R$ 26,00 bruto, mais de R$ 45,00/h líquida e desvio < 1.000m com ângulo <= 45°
        val isDiamond = grossRev >= 26.0 && netProfitPerHour >= 45.0 && deviationMeters <= 1000 && angularDiff <= 45f

        val corridorLabel = when {
            angularDiff <= 25f -> "Corredor Direcional Alinhado (${angularDiff.roundToInt()}°)"
            angularDiff <= 45f -> "Mesmo Corredor Viário (${angularDiff.roundToInt()}°)"
            else -> "Corredor Transversal Compatível (${angularDiff.roundToInt()}°)"
        }

        val steps = listOf(
            "1. Coletar em ${o1.restaurantName} (${o1.appName})",
            "2. Coletar em ${o2.restaurantName} (${o2.appName}) - Desvio: +${pickupDevMeters}m",
            "3. Entregar em ${o1.dropoffAddress} (${o1.appName})",
            "4. Entregar em ${o2.dropoffAddress} (${o2.appName}) - Desvio: +${dropoffDevMeters}m"
        )

        val reason = if (isDiamond) {
            "💎 Combo Diamante de Corredor: $corridorLabel com ganho líquido de R$ ${String.format(Locale.US, "%.2f", netProfitPerHour)}/h e apenas +${deviationMeters}m de desvio."
        } else {
            "⚡ Mesclagem de Corredor: $corridorLabel com rendimento líquido de R$ ${String.format(Locale.US, "%.2f", netProfitPerHour)}/h (+${extraMinutes} min)."
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
            haversineResult = haversineEval
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
     * Limpa ofertas antigas ou reinicia o scanner.
     */
    fun clearPool() {
        _scannedPoolOffers.value = emptyList()
        _rankedCombos.value = emptyList()
    }
}
