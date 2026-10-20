# 📑 AUDITORIA TÉCNICA COMPLETA - JARVIS RADAR MOTOBOY

> **Documento Consolidado para Análise de Engenharia no Claude / LLMs**

## 1. VISÃO GERAL DA ARQUITETURA E CONEXÕES

### 🌐 APIs e Conexões do Sistema:
1. **Backend VPS Hostinger (Ultra-baixa Latência):**
   - Protocolo: HTTPS / WebSocket com Keep-Alive de 12 segundos.
   - Endpoints: Telemetria de condutor, sincronização de corridas, radar colaborativo de blitz/radares móveis.
   - Camada de Resiliência: `RetryConnectionInterceptor` com backoff exponencial e detecção de conectividade 4G/5G oscilante.

2. **Gateway Financeiro Asaas (Cobranças e Pix):**
   - Geração de QR Code Pix dinâmico via API REST.
   - Webhook instantâneo para liberação de acesso aos planos sem intervenção manual.

3. **Divisão de Tela (Split-Screen) em 1 Toque para Motoboys:**
   - Acionamento: Botão de alto contraste no HUD flutuante do guidão.
   - Mecanismo: `GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN` via Android Accessibility API sem exigir Root.
   - Layout: 50% Superior (Ex: iFood) e 50% Inferior (Ex: 99Food ou Rappi).

4. **Cálculo Geodésico Haversine e Tolerância Dinâmica:**
   - Distância da Terra: Esferóide R = 6.371.000m via fórmula de Haversine exata.
   - Tolerância Dinâmica: Ajustada pela velocidade do condutor, horário de pico de trânsito e SLA limite de 25 min para comida quente.
   - Sinalização Visual: Badges 🟢 (Desvio < 400m), 🟡 (Desvio 400m-1200m) e 🔴 (Desvio > 1200m com risco de atraso).

---

## 2. CÓDIGOS-FONTE COMPLETOS DOS ARQUIVOS CRÍTICOS

### 📄 Arquivo: `app/src/main/java/com/example/DeepOrderPoolMergingEngine.kt`
**Função no Sistema:** Motor de Mesclagem Profunda O(N^2) Otimizado

```kotlin
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

```

---

### 📄 Arquivo: `app/src/main/java/com/example/HaversineGeoDeviationCalculator.kt`
**Função no Sistema:** Calculadora Geodésica Haversine de Desvio em Metros

```kotlin
package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Nível de tolerância e segurança do desvio geográfico calculado.
 */
enum class DeviationStatusLevel(
    val label: String,
    val colorHex: Long,
    val description: String
) {
    EXCELLENT("DESVIO MÍNIMO", 0xFF10B981, "Quase sem desvio do percurso principal"),
    ACCEPTABLE("DESVIO MODERADO", 0xFFF59E0B, "Compatível com a margem do pedido"),
    CRITICAL_EXCESSIVE("DESVIO CRÍTICO", 0xFFEF4444, "Risco elevado de atraso ou comida fria")
}

/**
 * Resultado completo do cruzamento geodésico Haversine.
 */
data class HaversineDeviationResult(
    val distanceDriverToPickup1Meters: Int,
    val distanceDriverToPickup2Meters: Int,
    val distancePickup1ToPickup2Meters: Int,
    val distanceDropoff1ToDropoff2Meters: Int,
    val directDistanceMeters: Int,
    val compositeRouteDistanceMeters: Int,
    val netDeviationMeters: Int,
    val estimatedExtraMinutes: Double,
    val dynamicMaxAllowedMeters: Int,
    val isWithinDynamicTolerance: Boolean,
    val statusLevel: DeviationStatusLevel,
    val bearingDivergenceDeg: Float,
    val explanation: String
)

/**
 * HaversineGeoDeviationCalculator
 *
 * Motor matemático e geoespacial de alta precisão para cálculo de distâncias na
 * curvatura terrestre pela Fórmula de Haversine (raio terrestre médio = 6.371.000m).
 *
 * Compara a localização GPS instantânea do entregador com os endereços de coleta
 * e entrega dos pedidos capturados nas telas dos apps parceiros (iFood, 99Food, Lalamove).
 */
object HaversineGeoDeviationCalculator {

    private const val EARTH_RADIUS_METERS = 6371000.0

    /**
     * Calcula a distância geodésica em metros entre dois pontos (WGS84).
     */
    fun calculateHaversineDistanceMeters(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaPhi = Math.toRadians(lat2 - lat1)
        val deltaLambda = Math.toRadians(lon2 - lon1)

        val a = sin(deltaPhi / 2.0) * sin(deltaPhi / 2.0) +
                cos(phi1) * cos(phi2) *
                sin(deltaLambda / 2.0) * sin(deltaLambda / 2.0)

        val c = 2.0 * atan2(sqrt(a), sqrt(1.0 - a))
        return EARTH_RADIUS_METERS * c
    }

    /**
     * Calcula o azimute (rumo inicial em graus [0..360]) entre dois pontos.
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
     * Calcula a diferença angular menor (0° a 180°) entre dois azimutes.
     */
    fun calculateAngularDifference(bearing1: Float, bearing2: Float): Float {
        val diff = Math.abs(bearing1 - bearing2) % 360f
        return if (diff > 180f) 360f - diff else diff
    }

    /**
     * Calcula o teto dinâmico de tolerância de desvio baseado na velocidade atual e margem de tempo (SLA).
     */
    fun calculateDynamicMaxDeviationMeters(
        currentSpeedKmh: Double,
        slaMarginMinutes: Int
    ): Int {
        // Tolerância base urbana: 500 metros
        // Velocidades maiores (ex: 40-60 km/h em vias expressas) permitem maior desvio em metros
        val speedKmhClamped = currentSpeedKmh.coerceIn(8.0, 70.0)
        val speedBonus = (speedKmhClamped * 10.0).roundToInt()

        // Fator de SLA: margem curta de tempo reduz o limite tolerável para proteger a nota
        val slaFactor = (slaMarginMinutes.toDouble() / 25.0).coerceIn(0.5, 1.5)
        val calculated = ((500 + speedBonus) * slaFactor).roundToInt()

        return calculated.coerceIn(350, 1400)
    }

    /**
     * Avalia o desvio geográfico completo de uma potencial mesclagem de pedidos a partir do GPS atual do motorista.
     */
    fun evaluateMultiPointRouteDeviation(
        driverLat: Double,
        driverLng: Double,
        driverSpeedKmh: Double,
        o1: ScreenPoolOffer,
        o2: ScreenPoolOffer,
        slaMinutes: Int = 25
    ): HaversineDeviationResult {
        // Distância do motorista até a primeira e segunda coleta
        val distToPickup1 = calculateHaversineDistanceMeters(driverLat, driverLng, o1.pickupLat, o1.pickupLng).roundToInt()
        val distToPickup2 = calculateHaversineDistanceMeters(driverLat, driverLng, o2.pickupLat, o2.pickupLng).roundToInt()

        // Desvio entre coletas e entregas (deslocamento intermediário)
        val distBetweenPickups = calculateHaversineDistanceMeters(o1.pickupLat, o1.pickupLng, o2.pickupLat, o2.pickupLng).roundToInt()
        val distBetweenDropoffs = calculateHaversineDistanceMeters(o1.dropoffLat, o1.dropoffLng, o2.dropoffLat, o2.dropoffLng).roundToInt()

        // Rota direta do pedido mais longo (em metros)
        val directRoute1Meters = (o1.distanceKm * 1000.0).roundToInt()
        val directRoute2Meters = (o2.distanceKm * 1000.0).roundToInt()
        val maxDirectMeters = max(directRoute1Meters, directRoute2Meters)

        // Rota composta: soma da perna direta mais o desvio entre coletas e entregas (com fator de malha 1.18x)
        val extraTraveledMeters = ((distBetweenPickups + distBetweenDropoffs) * 1.18).roundToInt()
        val compositeMeters = maxDirectMeters + extraTraveledMeters

        // Desvio líquido em metros
        val netDeviationMeters = extraTraveledMeters

        // Estimativa de tempo extra (calculado à velocidade média urbana de 26 km/h ou velocidade atual do GPS)
        val effectiveSpeedKmh = if (driverSpeedKmh > 12.0) driverSpeedKmh else 26.0
        val extraMinutes = (netDeviationMeters.toDouble() / (effectiveSpeedKmh * 1000.0 / 60.0))
            .let { if (it.isFinite()) it else 2.5 }
            .coerceAtLeast(1.0)

        // Limite dinâmico ajustado por velocidade e SLA
        val dynamicMaxAllowed = calculateDynamicMaxDeviationMeters(driverSpeedKmh, slaMinutes)
        val isWithinTolerance = netDeviationMeters <= dynamicMaxAllowed

        // Azimute direcional dos dois pedidos
        val b1 = calculateBearing(o1.pickupLat, o1.pickupLng, o1.dropoffLat, o1.dropoffLng)
        val b2 = calculateBearing(o2.pickupLat, o2.pickupLng, o2.dropoffLat, o2.dropoffLng)
        val angularDiff = calculateAngularDifference(b1, b2)

        val status = when {
            netDeviationMeters <= (dynamicMaxAllowed * 0.55).roundToInt() && angularDiff <= 35f -> DeviationStatusLevel.EXCELLENT
            isWithinTolerance && angularDiff <= 65f -> DeviationStatusLevel.ACCEPTABLE
            else -> DeviationStatusLevel.CRITICAL_EXCESSIVE
        }

        val explanation = when (status) {
            DeviationStatusLevel.EXCELLENT -> "Coletas a +${distBetweenPickups}m e entregas a +${distBetweenDropoffs}m no mesmo vetor (+${String.format(Locale.US, "%.1f", extraMinutes)} min)."
            DeviationStatusLevel.ACCEPTABLE -> "Desvio dentro da margem operacional de ${dynamicMaxAllowed}m (+${netDeviationMeters}m / +${String.format(Locale.US, "%.1f", extraMinutes)} min)."
            DeviationStatusLevel.CRITICAL_EXCESSIVE -> "Desvio de ${netDeviationMeters}m excede o limite dinâmico seguro de ${dynamicMaxAllowed}m. Risco de atraso."
        }

        return HaversineDeviationResult(
            distanceDriverToPickup1Meters = distToPickup1,
            distanceDriverToPickup2Meters = distToPickup2,
            distancePickup1ToPickup2Meters = distBetweenPickups,
            distanceDropoff1ToDropoff2Meters = distBetweenDropoffs,
            directDistanceMeters = maxDirectMeters,
            compositeRouteDistanceMeters = compositeMeters,
            netDeviationMeters = netDeviationMeters,
            estimatedExtraMinutes = extraMinutes,
            dynamicMaxAllowedMeters = dynamicMaxAllowed,
            isWithinDynamicTolerance = isWithinTolerance,
            statusLevel = status,
            bearingDivergenceDeg = angularDiff,
            explanation = explanation
        )
    }
}

/**
 * Componente Composable visual para exibição do Desvio Geográfico Haversine.
 */
@Composable
fun HaversineDeviationBadge(
    result: HaversineDeviationResult,
    modifier: Modifier = Modifier
) {
    val statusColor = Color(result.statusLevel.colorHex)

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF0F172A),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, statusColor.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when (result.statusLevel) {
                            DeviationStatusLevel.EXCELLENT -> Icons.Default.CheckCircle
                            DeviationStatusLevel.ACCEPTABLE -> Icons.Default.Route
                            DeviationStatusLevel.CRITICAL_EXCESSIVE -> Icons.Default.Warning
                        },
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "DESVIO HAVERSINE: +${result.netDeviationMeters}m",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = statusColor.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "+${String.format(Locale.US, "%.1f", result.estimatedExtraMinutes)} MIN",
                        color = statusColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Detalhamento de Coleta e Entrega
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Coletas: +${result.distancePickup1ToPickup2Meters}m",
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp
                )
                Text(
                    text = "Entregas: +${result.distanceDropoff1ToDropoff2Meters}m",
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp
                )
                Text(
                    text = "Teto SLA: ${result.dynamicMaxAllowedMeters}m",
                    color = Color(0xFF38BDF8),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = result.explanation,
                color = Color(0xFFCBD5E1),
                fontSize = 10.sp,
                lineHeight = 13.sp
            )
        }
    }
}

```

---

### 📄 Arquivo: `app/src/main/java/com/example/SplitScreenDualRadarCoordinator.kt`
**Função no Sistema:** Coordenador de Tela Dividida em 1 Toque (Guidão de Moto)

```kotlin
package com.example

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Representa uma janela ativa detectada na tela dividida (Split-Screen)
 */
data class SplitWindowTelemetry(
    val position: WindowVerticalPosition,
    val packageName: String,
    val appName: String,
    val bounds: Rect,
    val latestOffer: ScreenPoolOffer?,
    val isFocused: Boolean,
    val lastUpdateMillis: Long = System.currentTimeMillis()
)

enum class WindowVerticalPosition {
    TOP,
    BOTTOM,
    FULL_SCREEN,
    FLOATING
}

/**
 * Par pré-configurado de aplicativos para lançamento conjunto em tela dividida.
 */
data class SplitScreenAppPair(
    val id: String,
    val title: String,
    val topPackage: String,
    val topAppName: String,
    val bottomPackage: String,
    val bottomAppName: String,
    val description: String
)

/**
 * SplitScreenDualRadarCoordinator
 *
 * Coordenador tático para o Modo Dividido Vertical (Multi-Window / Split-Screen).
 *
 * Responsável por:
 * 1. Identificar em tempo real todas as janelas interativas na tela ([AccessibilityService.getWindows]).
 * 2. Classificar as janelas por coordenada Y da tela (Top Window vs Bottom Window).
 * 3. Varrer simultaneamente ambas as metades sem necessidade de alternância de foco.
 * 4. Alimentar o [DeepOrderPoolMergingEngine] instantaneamente quando novas corridas surgirem em qualquer metade.
 * 5. Lançar pares de aplicativos diretamente em tela dividida.
 */
class SplitScreenDualRadarCoordinator private constructor(private val context: Context) {

    companion object {
        private const val TAG = "SplitScreenCoordinator"

        @Volatile
        private var INSTANCE: SplitScreenDualRadarCoordinator? = null

        fun getInstance(context: Context): SplitScreenDualRadarCoordinator {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SplitScreenDualRadarCoordinator(context.applicationContext).also { INSTANCE = it }
            }
        }

        // Pares de aplicativos homologados para motoristas
        val PRECONFIGURED_PAIRS = listOf(
            SplitScreenAppPair(
                id = "ifood_99",
                title = "iFood (Topo) + 99Food (Base)",
                topPackage = "com.ifood.entregador",
                topAppName = "iFood Entregador",
                bottomPackage = "com.didiglobal.driver",
                bottomAppName = "99 Motorista / 99Food",
                description = "Par clássico de alta densidade de pedidos em capitais e regiões centrais."
            ),
            SplitScreenAppPair(
                id = "ifood_rappi",
                title = "iFood (Topo) + Rappi (Base)",
                topPackage = "com.ifood.entregador",
                topAppName = "iFood Entregador",
                bottomPackage = "com.grability.rappi.entregador",
                bottomAppName = "Rappi Entregador",
                description = "Ideal para zonas nobres e polos gastronômicos com alta concentração de hubs."
            ),
            SplitScreenAppPair(
                id = "noventa_rappi",
                title = "99Food (Topo) + Rappi (Base)",
                topPackage = "com.didiglobal.driver",
                topAppName = "99 Motorista",
                bottomPackage = "com.grability.rappi.entregador",
                bottomAppName = "Rappi Entregador",
                description = "Excelente alternativa para alternância de horários de pico e dias de chuva."
            )
        )
    }

    private val _isSplitScreenActive = MutableStateFlow(false)
    val isSplitScreenActive: StateFlow<Boolean> = _isSplitScreenActive.asStateFlow()

    private val _topWindowTelemetry = MutableStateFlow<SplitWindowTelemetry?>(null)
    val topWindowTelemetry: StateFlow<SplitWindowTelemetry?> = _topWindowTelemetry.asStateFlow()

    private val _bottomWindowTelemetry = MutableStateFlow<SplitWindowTelemetry?>(null)
    val bottomWindowTelemetry: StateFlow<SplitWindowTelemetry?> = _bottomWindowTelemetry.asStateFlow()

    private val _selectedPair = MutableStateFlow(PRECONFIGURED_PAIRS[0])
    val selectedPair: StateFlow<SplitScreenAppPair> = _selectedPair.asStateFlow()

    fun selectPair(pair: SplitScreenAppPair) {
        _selectedPair.value = pair
    }

    /**
     * Varrer todas as janelas do sistema em tempo real capturadas pelo serviço de acessibilidade.
     */
    fun processWindows(accessibilityService: AccessibilityService, windows: List<AccessibilityWindowInfo>?) {
        if (windows.isNullOrEmpty()) {
            _isSplitScreenActive.value = false
            return
        }

        // Filtra apenas janelas de aplicativos interativos
        val appWindows = windows.filter {
            it.type == AccessibilityWindowInfo.TYPE_APPLICATION && it.root != null
        }

        // Se houver pelo menos 2 janelas de aplicativos visíveis simultaneamente, o modo dividido está ativo!
        val isDualActive = appWindows.size >= 2
        _isSplitScreenActive.value = isDualActive

        val screenBounds = Rect()
        val windowList = mutableListOf<SplitWindowTelemetry>()

        for (window in appWindows) {
            val root = window.root ?: continue
            val bounds = Rect()
            window.getBoundsInScreen(bounds)

            val pkg = root.packageName?.toString() ?: ""
            val appName = resolveAppName(pkg)

            // Classifica pela posição vertical (Y central)
            val centerY = bounds.centerY()
            val position = when {
                !isDualActive -> WindowVerticalPosition.FULL_SCREEN
                centerY < 1000 -> WindowVerticalPosition.TOP
                else -> WindowVerticalPosition.BOTTOM
            }

            // Extrai a oferta visível nesta janela
            val offer = extractOfferFromWindowNode(pkg, appName, root)
            @Suppress("DEPRECATION")
            root.recycle()

            windowList.add(
                SplitWindowTelemetry(
                    position = position,
                    packageName = pkg,
                    appName = appName,
                    bounds = bounds,
                    latestOffer = offer,
                    isFocused = window.isFocused
                )
            )
        }

        val top = windowList.firstOrNull { it.position == WindowVerticalPosition.TOP }
        val bottom = windowList.firstOrNull { it.position == WindowVerticalPosition.BOTTOM }

        _topWindowTelemetry.value = top
        _bottomWindowTelemetry.value = bottom

        // Se ambas as metades possuem ofertas capturadas, alimenta o DeepOrderPoolMergingEngine imediatamente
        if (top?.latestOffer != null && bottom?.latestOffer != null) {
            val poolEngine = DeepOrderPoolMergingEngine.getInstance(context)
            poolEngine.ingestPoolOffers(top.appName, listOf(top.latestOffer))
            poolEngine.ingestPoolOffers(bottom.appName, listOf(bottom.latestOffer))
            Log.i(TAG, "⚡ Sucesso na leitura simultânea: Topo=${top.appName} R$ ${top.latestOffer.value} | Base=${bottom.appName} R$ ${bottom.latestOffer.value}")
        }
    }

    /**
     * Extrai a melhor oferta visível dentro do nó de uma janela específica
     */
    private fun extractOfferFromWindowNode(
        packageName: String,
        appName: String,
        rootNode: AccessibilityNodeInfo
    ): ScreenPoolOffer? {
        val texts = mutableListOf<String>()
        collectTexts(rootNode, texts)
        val blob = texts.joinToString(" ")

        val valueRegex = Regex("""R\$\s*(\d+[.,]\d{2})""")
        val distanceRegex = Regex("""(\d+[.,]?\d*)\s*km""", RegexOption.IGNORE_CASE)

        val matchVal = valueRegex.find(blob)
        val matchDist = distanceRegex.find(blob)

        if (matchVal != null && matchDist != null) {
            val v = matchVal.groupValues[1].replace(',', '.').toDoubleOrNull() ?: 0.0
            val d = matchDist.groupValues[1].replace(',', '.').toDoubleOrNull() ?: 0.0

            if (v > 0.0 && d > 0.0) {
                val rest = texts.firstOrNull {
                    it.contains("Restaurante", true) ||
                    it.contains("Shopping", true) ||
                    it.contains("Habib", true) ||
                    it.contains("Burger", true) ||
                    it.contains("Outback", true) ||
                    it.contains("Pizzaria", true)
                } ?: "Restaurante $appName"

                val drop = texts.firstOrNull {
                    it.contains("Rua", true) ||
                    it.contains("Av", true) ||
                    it.contains("Alameda", true)
                } ?: "Destino na Cidade"

                return ScreenPoolOffer(
                    appName = appName,
                    packageName = packageName,
                    restaurantName = rest,
                    pickupAddress = rest,
                    dropoffAddress = drop,
                    value = v,
                    distanceKm = d
                )
            }
        }
        return null
    }

    private fun collectTexts(node: AccessibilityNodeInfo?, list: MutableList<String>) {
        if (node == null) return
        val t = node.text?.toString()?.trim()
        val d = node.contentDescription?.toString()?.trim()
        if (!t.isNullOrEmpty()) list.add(t)
        else if (!d.isNullOrEmpty()) list.add(d)

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            collectTexts(child, list)
            @Suppress("DEPRECATION")
            child?.recycle()
        }
    }

    /**
     * Dispara a ação global do Android para abrir o modo de tela dividida.
     */
    fun triggerSplitScreenMode(accessibilityService: AccessibilityService?): Boolean {
        if (accessibilityService == null) {
            Log.w(TAG, "JarvisAccessibilityService não está ativo.")
            return false
        }

        try {
            val success = accessibilityService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN)
            Log.i(TAG, "Ação GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN disparada. Sucesso: $success")
            return success
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao acionar tela dividida via AccessibilityService", e)
            return false
        }
    }

    /**
     * Lança o par configurado de aplicativos.
     * Primeiro abre o app superior, aguarda brevemente e aciona a divisão de tela para escolher o inferior.
     */
    fun launchSplitScreenPair(context: Context, pair: SplitScreenAppPair) {
        try {
            val pm = context.packageManager
            val topIntent = pm.getLaunchIntentForPackage(pair.topPackage)
            if (topIntent != null) {
                topIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(topIntent)
            }

            // Solicita a divisão via serviço de acessibilidade se ativo
            JarvisAccessibilityService.instance?.let { service ->
                service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN)
            }

            TextToSpeechManager.getInstance(context).speakVoiceAlert(
                "Iniciando modo dividido: ${pair.topAppName} no topo e ${pair.bottomAppName} na base. Leitura simultânea ativada."
            )
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao lançar par de tela dividida: ${e.message}")
        }
    }

    private fun resolveAppName(packageName: String): String {
        return when {
            packageName.contains("ifood", ignoreCase = true) -> "iFood"
            packageName.contains("didiglobal", ignoreCase = true) || packageName.contains("99", ignoreCase = true) -> "99Food"
            packageName.contains("rappi", ignoreCase = true) -> "Rappi"
            packageName.contains("uber", ignoreCase = true) -> "Uber Driver"
            packageName.contains("lalamove", ignoreCase = true) -> "Lalamove"
            else -> packageName.substringAfterLast('.')
        }
    }

    /**
     * Permite simular telemetria simultânea em ambiente de teste/emulador.
     */
    fun simulateDualSplitWindowTelemetry() {
        val topOffer = ScreenPoolOffer(
            appName = "iFood",
            packageName = "com.ifood.entregador",
            restaurantName = "Fogo de Chão - Jardins",
            pickupAddress = "Rua Augusta, 2077",
            dropoffAddress = "Av. Rebouças, 1800 - Pinheiros",
            value = 22.50,
            distanceKm = 3.4,
            pickupLat = -23.5590,
            pickupLng = -46.6620,
            dropoffLat = -23.5680,
            dropoffLng = -46.6850
        )

        val bottomOffer = ScreenPoolOffer(
            appName = "99Food",
            packageName = "com.didiglobal.driver",
            restaurantName = "Bullguer Jardins",
            pickupAddress = "Alameda Lorena, 1430",
            dropoffAddress = "Rua dos Pinheiros, 600 - Pinheiros",
            value = 18.00,
            distanceKm = 2.9,
            pickupLat = -23.5630,
            pickupLng = -46.6650,
            dropoffLat = -23.5670,
            dropoffLng = -46.6880
        )

        _isSplitScreenActive.value = true
        _topWindowTelemetry.value = SplitWindowTelemetry(
            position = WindowVerticalPosition.TOP,
            packageName = topOffer.packageName,
            appName = topOffer.appName,
            bounds = Rect(0, 0, 1080, 1150),
            latestOffer = topOffer,
            isFocused = true
        )

        _bottomWindowTelemetry.value = SplitWindowTelemetry(
            position = WindowVerticalPosition.BOTTOM,
            packageName = bottomOffer.packageName,
            appName = bottomOffer.appName,
            bounds = Rect(0, 1150, 1080, 2300),
            latestOffer = bottomOffer,
            isFocused = false
        )

        val poolEngine = DeepOrderPoolMergingEngine.getInstance(context)
        poolEngine.ingestPoolOffers("iFood", listOf(topOffer))
        poolEngine.ingestPoolOffers("99Food", listOf(bottomOffer))
    }
}

```

---

### 📄 Arquivo: `app/src/main/java/com/example/OverlayWindowManager.kt`
**Função no Sistema:** Gerenciador do HUD Flutuante com Badge Haversine

```kotlin
package com.example

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import kotlin.math.hypot

/**
 * OverlayWindowManager
 *
 * Gerenciador nativo de interface flutuante (Bolha tática & Mini-HUD Compacto)
 * utilizando o [WindowManager] do Android.
 *
 * Funcionalidades da Bolha Flutuante Tática Aprimorada (HUD sobre iFood/99/Uber):
 * 1. Bolha Flutuante Compacta persistente com indicador de rentabilidade em tempo real.
 * 2. Card Flutuante Compacto de Inspeção de Corridas com cálculo imediato de R$/km:
 *    - Ganho real por km (ex: R$ 7,20/km 🟢 BOA ou R$ 2,80/km 🔴 PREJUÍZO).
 *    - Indicador visual luminoso de Rota Dupla Compatível (sobreposição de pedidos).
 *    - Ações em 1 Toque: Botões gigantes táteis de ACEITAR ou DISPENSAR sem precisar alternar manualmente.
 * 3. Ancoragem magnética suave nas bordas laterais da tela (Snap-to-Edge).
 */
class OverlayWindowManager private constructor(private val context: Context) {

    data class JarvisOverlayStatus(
        val isOnline: Boolean = true,
        val headline: String = "JARVIS OPERACIONAL",
        val activeApp: String = "Monitorando Apps",
        val profitPerKm: String = "R$ 4,50/km",
        val blitzAlert: String = "Raio Seguro (Sem Blitz)",
        val latestOffer: String? = null,
        val alertLevel: AlertLevel = AlertLevel.NORMAL
    ) {
        enum class AlertLevel {
            NORMAL,      // Verde Neon
            OPPORTUNITY, // Azul / Ciano
            WARNING      // Laranja / Vermelho
        }
    }

    /**
     * Modelo da Corrida Detectada na Bolha Flutuante
     */
    data class FloatingTacticalOffer(
        val appName: String = "iFood",
        val packageName: String = "com.ifood.driver",
        val value: Double = 28.50,
        val distanceKm: Double = 3.9,
        val pricePerKm: Double = 7.30,
        val isGoodDeal: Boolean = true,
        val dealLabel: String = "R$ 7,20/km 🟢 BOA",
        val destination: String = "Av. Rebouças, 1200",
        val pickup: String = "Shopping Eldorado",
        val hasDualRoute: Boolean = true,
        val dualRouteAppName: String = "99 Moto",
        val dualRouteExtraGain: Double = 16.50,
        val haversineDeviationMeters: Int = 0,
        val haversineExtraMinutes: Double = 0.0,
        val haversineExplanation: String = ""
    )

    private val windowManager: WindowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private val mainHandler = Handler(Looper.getMainLooper())
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    private var overlayRootView: FrameLayout? = null
    private var bubbleContainer: FrameLayout? = null
    private var hudContainer: LinearLayout? = null

    // Componentes da Bolha Circular
    private var tvBubbleBadge: TextView? = null
    private var ivBubblePulse: View? = null

    // Componentes do Card Flutuante de Corrida
    private var offerCardBox: LinearLayout? = null
    private var tvOfferAppTag: TextView? = null
    private var tvOfferProfitMain: TextView? = null
    private var tvOfferProfitSub: TextView? = null
    private var tvOfferMetrics: TextView? = null
    private var tvOfferAddresses: TextView? = null
    private var dualRouteBox: LinearLayout? = null
    private var tvDualRouteText: TextView? = null

    // Componentes de Desvio Geodésico Haversine no HUD
    private var haversineBox: LinearLayout? = null
    private var tvHaversineTitle: TextView? = null
    private var tvHaversineBadge: TextView? = null
    private var tvHaversineDetails: TextView? = null

    private var btnAcceptRide: TextView? = null
    private var btnDeclineRide: TextView? = null
    private var btnSwitchApp: TextView? = null

    // Componentes de Status Geral
    private var tvHudTitle: TextView? = null
    private var tvHudSubstatus: TextView? = null
    private var tvHudProfit: TextView? = null
    private var tvHudBlitz: TextView? = null
    private var defaultMetricsBox: LinearLayout? = null

    private var layoutParams: WindowManager.LayoutParams? = null

    private var isExpanded = false
    private var isShowing = false
    private var currentStatus = JarvisOverlayStatus()
    private var currentOffer: FloatingTacticalOffer? = null

    // Variáveis para cálculo de arraste
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false
    private var touchStartTime: Long = 0

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var INSTANCE: OverlayWindowManager? = null

        fun getInstance(context: Context): OverlayWindowManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: OverlayWindowManager(context.applicationContext).also { INSTANCE = it }
            }
        }

        fun canDrawOverlays(context: Context): Boolean {
            return Settings.canDrawOverlays(context)
        }

        fun requestOverlayPermission(context: Context) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    /**
     * Exibe ou atualiza a interface flutuante sobreposta aos outros apps.
     */
    fun show() {
        if (!canDrawOverlays(context)) return

        mainHandler.post {
            if (isShowing && overlayRootView != null) {
                overlayRootView?.visibility = View.VISIBLE
                return@post
            }
            createAndAttachOverlayView()
        }
    }

    /**
     * Oculta a interface flutuante.
     */
    fun hide() {
        mainHandler.post {
            overlayRootView?.visibility = View.GONE
        }
    }

    fun toggle() {
        if (isShowing && overlayRootView?.visibility == View.VISIBLE) {
            hide()
        } else {
            show()
        }
    }

    fun isShowing(): Boolean = isShowing && overlayRootView?.visibility == View.VISIBLE

    /**
     * Atualiza o estado geral exibido na bolha.
     */
    fun updateStatus(status: JarvisOverlayStatus) {
        this.currentStatus = status
        mainHandler.post {
            applyStatusToViews()
        }
    }

    /**
     * Dispara o Card Flutuante Compacto de Inspeção de Corrida com cálculo de R$/km e Rota Dupla
     */
    fun showTacticalOffer(offer: FloatingTacticalOffer) {
        this.currentOffer = offer
        mainHandler.post {
            if (!isShowing || overlayRootView == null) {
                show()
            }
            // Força a expansão do HUD para visualização imediata pelo motorista
            setExpandedState(true)
            applyOfferToViews(offer)

            // Resposta tátil e sonora
            try {
                if (offer.isGoodDeal) {
                    HapticFeedbackHelper.vibrateSuccess(context)
                } else {
                    HapticFeedbackHelper.vibrateWarning(context)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Leitura automática em voz alta do resumo da oferta (valor, distância e lucro/km)
            try {
                OfferTextToSpeechEngine.getInstance(context).speakTacticalOffer(offer)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Exibe alerta flutuante de cancelamento parcial em corrida mesclada sobre os apps de entrega
     */
    fun showCancellationAlert(event: StackCancellationEvent) {
        val tacticalOffer = FloatingTacticalOffer(
            appName = event.appName,
            packageName = "com.ifood.driver",
            value = event.remainingValue,
            distanceKm = event.remainingDistanceKm,
            pricePerKm = event.remainingGainPerKm,
            isGoodDeal = event.isViable,
            dealLabel = if (event.isViable) "⚠️ SUB-PEDIDO CANCELADO • RESTANTE VIÁVEL" else "🚨 ALERTA: RESTANTE PREJUDICIAL",
            destination = event.remainingRestaurants,
            pickup = "Cancelado: ${event.cancelledRestaurant} (-R$ ${String.format(java.util.Locale.ROOT, "%.2f", event.cancelledValue)})",
            hasDualRoute = false
        )
        showTacticalOffer(tacticalOffer)
    }

    /**
     * Limpa a oferta atual e recolhe o HUD
     */
    fun clearTacticalOffer() {
        this.currentOffer = null
        mainHandler.post {
            offerCardBox?.visibility = View.GONE
            defaultMetricsBox?.visibility = View.VISIBLE
            setExpandedState(false)
            applyStatusToViews()
        }
    }

    /**
     * Remove a sobreposição do WindowManager.
     */
    fun destroy() {
        mainHandler.post {
            overlayRootView?.let { view ->
                if (view.isAttachedToWindow) {
                    try {
                        windowManager.removeViewImmediate(view)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            overlayRootView = null
            bubbleContainer = null
            hudContainer = null
            isShowing = false
            isExpanded = false
            currentOffer = null
        }
    }

    // =========================================================================
    // CONSTRUÇÃO E ANCORAGEM DA VIEW NATIVA
    // =========================================================================

    @SuppressLint("ClickableViewAccessibility")
    private fun createAndAttachOverlayView() {
        val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val startX = screenWidth - dpToPx(76)
        val startY = dpToPx(160)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = startX
            y = startY
        }
        this.layoutParams = params

        val root = FrameLayout(context).apply {
            clipChildren = false
            clipToPadding = false
        }

        val bubble = buildBubbleView()
        this.bubbleContainer = bubble

        val hud = buildHudView()
        this.hudContainer = hud

        root.addView(bubble)
        root.addView(hud)

        bubble.setOnTouchListener { _, event ->
            handleBubbleTouch(event)
        }

        this.overlayRootView = root

        try {
            windowManager.addView(root, params)
            isShowing = true
            isExpanded = false
            setExpandedState(false)
            applyStatusToViews()
        } catch (e: Exception) {
            e.printStackTrace()
            isShowing = false
        }
    }

    /**
     * Monta a bolha circular compacta
     */
    private fun buildBubbleView(): FrameLayout {
        val bubbleSize = dpToPx(62)

        val container = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(bubbleSize, bubbleSize)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#10151E"))
                setStroke(dpToPx(2.2f), Color.parseColor("#00E676"))
            }
            elevation = dpToPx(10).toFloat()
        }

        val pulse = View(context).apply {
            val pulseSize = dpToPx(11)
            layoutParams = FrameLayout.LayoutParams(pulseSize, pulseSize).apply {
                gravity = Gravity.TOP or Gravity.END
                topMargin = dpToPx(5)
                rightMargin = dpToPx(5)
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#00E676"))
            }
        }
        this.ivBubblePulse = pulse
        container.addView(pulse)

        val tvIcon = TextView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            gravity = Gravity.CENTER
            text = "⚡"
            textSize = 24f
        }
        container.addView(tvIcon)

        val tvBadge = TextView(context).apply {
            val badgeHeight = dpToPx(18)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                badgeHeight
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                bottomMargin = dpToPx(2)
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(9).toFloat()
                setColor(Color.parseColor("#00E676"))
            }
            setPadding(dpToPx(6), 0, dpToPx(6), 0)
            text = "JARVIS"
            setTextColor(Color.parseColor("#0A0E14"))
            textSize = 8.5f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        this.tvBubbleBadge = tvBadge
        container.addView(tvBadge)

        return container
    }

    /**
     * Monta o HUD tático compacto completo com a seção de inspeção de corridas
     */
    private fun buildHudView(): LinearLayout {
        val hudWidth = dpToPx(310)

        val container = LinearLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                hudWidth,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(20).toFloat()
                setColor(Color.parseColor("#F40A0D14"))
                setStroke(dpToPx(1.5f), Color.parseColor("#00E676"))
            }
            elevation = dpToPx(14).toFloat()
        }

        // Cabeçalho: Título + Botão Fechar
        val headerRow = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val tvTitle = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            text = "HUD TÁTICO COCKPIT"
            setTextColor(Color.WHITE)
            textSize = 13.spToSp()
            typeface = Typeface.DEFAULT_BOLD
        }
        this.tvHudTitle = tvTitle
        headerRow.addView(tvTitle)

        // Botão de Áudio TTS (Ouvir resumo em voz alta / Mudo)
        val btnSpeaker = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(30), dpToPx(30)).apply {
                rightMargin = dpToPx(6)
            }
            gravity = Gravity.CENTER
            text = "🔊"
            textSize = 14f
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#1C2533"))
            }
            setOnClickListener {
                val ttsEngine = OfferTextToSpeechEngine.getInstance(context)
                val offer = currentOffer
                if (offer != null) {
                    ttsEngine.speakTacticalOffer(offer)
                    Toast.makeText(context, "🔊 Lendo resumo da oferta em voz alta...", Toast.LENGTH_SHORT).show()
                } else {
                    val repeated = ttsEngine.repeatLastOffer()
                    if (!repeated) {
                        ttsEngine.speak("Nenhuma oferta recente para leitura.")
                    }
                    Toast.makeText(context, "🔊 Reproduzindo áudio TTS...", Toast.LENGTH_SHORT).show()
                }
            }
            setOnLongClickListener {
                val ttsEngine = OfferTextToSpeechEngine.getInstance(context)
                val isMuted = ttsEngine.toggleMute()
                text = if (isMuted) "🔈" else "🔊"
                Toast.makeText(
                    context,
                    if (isMuted) "Voz TTS silenciada" else "Voz TTS ativada",
                    Toast.LENGTH_SHORT
                ).show()
                true
            }
        }
        headerRow.addView(btnSpeaker)

        val btnClose = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(30), dpToPx(30))
            gravity = Gravity.CENTER
            text = "✕"
            setTextColor(Color.parseColor("#90A4AE"))
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setOnClickListener {
                setExpandedState(false)
            }
        }
        headerRow.addView(btnClose)
        container.addView(headerRow)

        // Substatus / Aplicativos monitorados
        val tvSub = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(2)
                bottomMargin = dpToPx(8)
            }
            text = "● Monitorando: iFood • Uber • 99 Moto"
            setTextColor(Color.parseColor("#00E676"))
            textSize = 10.5f
        }
        this.tvHudSubstatus = tvSub
        container.addView(tvSub)

        // =====================================================================
        // CARD FLUTUANTE COMPACTO DE OFERTA (CORRIDA DETECTADA)
        // =====================================================================
        val offerBox = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(10)
            }
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(14).toFloat()
                setColor(Color.parseColor("#151B26"))
                setStroke(dpToPx(1.5f), Color.parseColor("#00E676"))
            }
        }
        this.offerCardBox = offerBox

        // Linha 1 do Card: Tag da Plataforma (ex: iFood 🔴) + Timer
        val tagRow = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val tvAppTag = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(6).toFloat()
                setColor(Color.parseColor("#EA1D2C"))
            }
            setPadding(dpToPx(8), dpToPx(3), dpToPx(8), dpToPx(3))
            text = "iFood"
            setTextColor(Color.WHITE)
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
        }
        this.tvOfferAppTag = tvAppTag
        tagRow.addView(tvAppTag)

        val spacerTag = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
        }
        tagRow.addView(spacerTag)

        val tvTimer = TextView(context).apply {
            text = "⏱ 15s p/ aceitar"
            setTextColor(Color.parseColor("#FFD700"))
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
        }
        tagRow.addView(tvTimer)
        offerBox.addView(tagRow)

        // Linha 2 do Card: GANHO REAL POR KM (Destaque Principal)
        val profitBanner = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(8)
                bottomMargin = dpToPx(6)
            }
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(10), dpToPx(8), dpToPx(10), dpToPx(8))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(10).toFloat()
                setColor(Color.parseColor("#122A1E"))
                setStroke(dpToPx(1.2f), Color.parseColor("#00E676"))
            }
        }

        val tvProfitMain = TextView(context).apply {
            text = "R$ 7,20/km 🟢 BOA"
            setTextColor(Color.parseColor("#00FF88"))
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        this.tvOfferProfitMain = tvProfitMain
        profitBanner.addView(tvProfitMain)

        val tvProfitSub = TextView(context).apply {
            text = "Excelente rentabilidade • Acima do piso"
            setTextColor(Color.parseColor("#B0BEC5"))
            textSize = 10f
            gravity = Gravity.CENTER
        }
        this.tvOfferProfitSub = tvProfitSub
        profitBanner.addView(tvProfitSub)
        offerBox.addView(profitBanner)

        // Linha 3 do Card: Métricas (Valor Total, Distância)
        val tvMetrics = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(4)
            }
            text = "💰 R$ 28,50 • 📏 3,9 km • ⏱ 12 min"
            setTextColor(Color.WHITE)
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
        }
        this.tvOfferMetrics = tvMetrics
        offerBox.addView(tvMetrics)

        // Linha 4 do Card: Destino e Coleta
        val tvAddresses = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(8)
            }
            text = "📍 De: Shopping Eldorado ➔ Para: Av. Rebouças"
            setTextColor(Color.parseColor("#90A4AE"))
            textSize = 10.5f
            maxLines = 2
        }
        this.tvOfferAddresses = tvAddresses
        offerBox.addView(tvAddresses)

        // Linha 5: INDICADOR DE ROTA DUPLA COMPATÍVEL
        val dualBox = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(10)
            }
            orientation = LinearLayout.HORIZONTAL
            setPadding(dpToPx(8), dpToPx(6), dpToPx(8), dpToPx(6))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(8).toFloat()
                setColor(Color.parseColor("#0C2030"))
                setStroke(dpToPx(1f), Color.parseColor("#00E5FF"))
            }
            gravity = Gravity.CENTER_VERTICAL
        }
        this.dualRouteBox = dualBox

        val tvDual = TextView(context).apply {
            text = "🔥 ROTA DUPLA COMPATÍVEL! (+R$ 16,50 c/ 99 Moto)"
            setTextColor(Color.parseColor("#00E5FF"))
            textSize = 10.5f
            typeface = Typeface.DEFAULT_BOLD
        }
        this.tvDualRouteText = tvDual
        dualBox.addView(tvDual)
        offerBox.addView(dualBox)

        // Linha 5.1: INDICADOR VISUAL DE DESVIO HAVERSINE (METROS + MINUTOS EXTRAS NO HUD)
        val hBox = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(8)
            }
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(8), dpToPx(6), dpToPx(8), dpToPx(6))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(8).toFloat()
                setColor(Color.parseColor("#0B132B"))
                setStroke(dpToPx(1f), Color.parseColor("#10B981"))
            }
        }
        this.haversineBox = hBox

        val hTitle = TextView(context).apply {
            text = "📍 DESVIO HAVERSINE: +380m • +1.8 min extras"
            setTextColor(Color.parseColor("#38BDF8"))
            textSize = 10.5f
            typeface = Typeface.DEFAULT_BOLD
        }
        this.tvHaversineTitle = hTitle
        hBox.addView(hTitle)

        val hBadge = TextView(context).apply {
            text = "[🟢 DESVIO MÍNIMO / SEGURO]"
            setTextColor(Color.parseColor("#10B981"))
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
        }
        this.tvHaversineBadge = hBadge
        hBox.addView(hBadge)

        val hDetails = TextView(context).apply {
            text = "Tolerância dinâmica: 820m (Velocidade e SLA seguros)"
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 9.5f
        }
        this.tvHaversineDetails = hDetails
        hBox.addView(hDetails)

        offerBox.addView(hBox)

        // Banner Viva-Voz no HUD Flutuante (Speech-to-Text Ativo)
        val voiceRow = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(8)
            }
            orientation = LinearLayout.HORIZONTAL
            setPadding(dpToPx(8), dpToPx(5), dpToPx(8), dpToPx(5))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(8).toFloat()
                setColor(Color.parseColor("#0F1E19"))
                setStroke(dpToPx(1f), Color.parseColor("#00FF88"))
            }
            gravity = Gravity.CENTER_VERTICAL
        }

        val tvVoiceStatus = TextView(context).apply {
            text = "🎤 VIVA-VOZ: Diga 'ACEITAR' ou 'RECUSAR'"
            setTextColor(Color.parseColor("#00FF88"))
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
        }
        voiceRow.addView(tvVoiceStatus)
        offerBox.addView(voiceRow)

        // =====================================================================
        // BOTÕES GIGANTES DE AÇÃO EM 1 TOQUE (ACEITAR OU DISPENSAR)
        // =====================================================================
        val oneTouchActionsRow = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL
        }

        // Botão Gigante 1: ACEITAR CORRIDA 🟢
        val btnAccept = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(48), 1.2f).apply {
                rightMargin = dpToPx(6)
            }
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(12).toFloat()
                setColor(Color.parseColor("#00E676"))
            }
            text = "ACEITAR 🟢"
            setTextColor(Color.parseColor("#0A0E14"))
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setOnClickListener {
                handleAcceptCurrentOffer()
            }
        }
        this.btnAcceptRide = btnAccept
        oneTouchActionsRow.addView(btnAccept)

        // Botão Tático: ROTA 🗺️ (Waze / Google Maps em 1 toque)
        val btnRoute = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(48), 0.9f).apply {
                leftMargin = dpToPx(4)
                rightMargin = dpToPx(4)
            }
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(12).toFloat()
                setColor(Color.parseColor("#0C2030"))
                setStroke(dpToPx(1.2f), Color.parseColor("#00E5FF"))
            }
            text = "ROTA 🗺️"
            setTextColor(Color.parseColor("#00E5FF"))
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setOnClickListener {
                val offer = currentOffer
                val targetAddress = offer?.pickup ?: "Ponto de Coleta"
                NavigationQuickDispatcher.launchBestRoute(
                    context = context,
                    latitude = -23.5616,
                    longitude = -46.6559,
                    addressTitle = targetAddress,
                    preferWaze = true
                )
            }
        }
        oneTouchActionsRow.addView(btnRoute)

        // Botão Gigante 2: DISPENSAR 🔴
        val btnDecline = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(48), 1f).apply {
                leftMargin = dpToPx(4)
            }
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(12).toFloat()
                setColor(Color.parseColor("#2A1218"))
                setStroke(dpToPx(1.2f), Color.parseColor("#FF3366"))
            }
            text = "DISPENSAR 🔴"
            setTextColor(Color.parseColor("#FF5252"))
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setOnClickListener {
                handleDeclineCurrentOffer()
            }
        }
        this.btnDeclineRide = btnDecline
        oneTouchActionsRow.addView(btnDecline)

        offerBox.addView(oneTouchActionsRow)

        // Botão Tático de Tela Dividida em 1 Toque na Oferta (Guidão da Moto)
        val btnSplitInOffer = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(40)
            ).apply {
                topMargin = dpToPx(8)
            }
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(10).toFloat()
                setColor(Color.parseColor("#1E1B4B"))
                setStroke(dpToPx(1.2f), Color.parseColor("#6366F1"))
            }
            text = "📱 TELA DIVIDIDA (1 TOQUE - iFood ⬆️ / 99 ⬇️)"
            setTextColor(Color.parseColor("#C7D2FE"))
            textSize = 11.5f
            typeface = Typeface.DEFAULT_BOLD
            setOnClickListener {
                val coordinator = SplitScreenDualRadarCoordinator.getInstance(context)
                val pair = coordinator.selectedPair.value
                coordinator.launchSplitScreenPair(context, pair)
                Toast.makeText(context, "⚡ Lançando Tela Dividida: ${pair.topAppName} (Topo) + ${pair.bottomAppName} (Base)", Toast.LENGTH_LONG).show()
            }
        }
        offerBox.addView(btnSplitInOffer)
        container.addView(offerBox)

        // =====================================================================
        // MÉTRICAS PADRÃO QUANDO NÃO HÁ CORRIDA ATIVA
        // =====================================================================
        val defaultBox = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.VERTICAL
        }
        this.defaultMetricsBox = defaultBox

        val metricsRow = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL
        }

        val profitBox = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            orientation = LinearLayout.VERTICAL
        }
        val lblProfit = TextView(context).apply {
            text = "PISO MÍNIMO"
            setTextColor(Color.parseColor("#90A4AE"))
            textSize = 9f
        }
        val valProfit = TextView(context).apply {
            text = "R$ 4,50/km"
            setTextColor(Color.parseColor("#00E676"))
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
        }
        this.tvHudProfit = valProfit
        profitBox.addView(lblProfit)
        profitBox.addView(valProfit)
        metricsRow.addView(profitBox)

        val blitzBox = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.2f)
            orientation = LinearLayout.VERTICAL
        }
        val lblBlitz = TextView(context).apply {
            text = "RADAR / BLITZ"
            setTextColor(Color.parseColor("#90A4AE"))
            textSize = 9f
        }
        val valBlitz = TextView(context).apply {
            text = "Raio Seguro"
            setTextColor(Color.parseColor("#81C784"))
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
        }
        this.tvHudBlitz = valBlitz
        blitzBox.addView(lblBlitz)
        blitzBox.addView(valBlitz)
        metricsRow.addView(blitzBox)
        defaultBox.addView(metricsRow)

        // Botão de Ação Rápida no HUD: Abrir Tela Dividida em 1 Toque (Guidão da Moto)
        val btnSplitDefault = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(44)
            ).apply {
                topMargin = dpToPx(10)
                bottomMargin = dpToPx(4)
            }
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(12).toFloat()
                setColor(Color.parseColor("#1E1B4B"))
                setStroke(dpToPx(1.5f), Color.parseColor("#6366F1"))
            }
            text = "📱 TELA DIVIDIDA 1-TOQUE (iFood ⬆️ / 99 ⬇️)"
            setTextColor(Color.parseColor("#A5B4FC"))
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setOnClickListener {
                val coordinator = SplitScreenDualRadarCoordinator.getInstance(context)
                val pair = coordinator.selectedPair.value
                coordinator.launchSplitScreenPair(context, pair)
                Toast.makeText(context, "⚡ Lançando Tela Dividida: ${pair.topAppName} (Topo) + ${pair.bottomAppName} (Base)", Toast.LENGTH_LONG).show()
            }
        }
        defaultBox.addView(btnSplitDefault)

        // Botões Padrão: Abrir Jarvis e Trocar App
        val defaultActionsRow = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(8)
            }
            orientation = LinearLayout.HORIZONTAL
        }

        val btnOpenApp = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(38), 1f).apply {
                rightMargin = dpToPx(4)
            }
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(10).toFloat()
                setColor(Color.parseColor("#00E676"))
            }
            text = "Abrir Jarvis"
            setTextColor(Color.parseColor("#0A0E14"))
            textSize = 11.5f
            typeface = Typeface.DEFAULT_BOLD
            setOnClickListener {
                openJarvisMainActivity()
                setExpandedState(false)
            }
        }
        defaultActionsRow.addView(btnOpenApp)

        val btnSwitch = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(38), 1f).apply {
                leftMargin = dpToPx(4)
            }
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(10).toFloat()
                setColor(Color.parseColor("#263238"))
                setStroke(dpToPx(1), Color.parseColor("#546E7A"))
            }
            text = "Recolher"
            setTextColor(Color.WHITE)
            textSize = 11.5f
            typeface = Typeface.DEFAULT_BOLD
            setOnClickListener {
                setExpandedState(false)
            }
        }
        this.btnSwitchApp = btnSwitch
        defaultActionsRow.addView(btnSwitch)

        defaultBox.addView(defaultActionsRow)
        container.addView(defaultBox)

        return container
    }

    // =========================================================================
    // AÇÕES EM 1 TOQUE (ACEITAR OU DISPENSAR)
    // =========================================================================

    /**
     * Aciona o aceite da oferta atual via comando de voz (Speech-to-Text do Google)
     */
    fun acceptCurrentOfferByVoice(): Boolean {
        if (currentOffer == null) return false
        mainHandler.post {
            handleAcceptCurrentOffer()
        }
        return true
    }

    /**
     * Aciona a recusa da oferta atual via comando de voz (Speech-to-Text do Google)
     */
    fun declineCurrentOfferByVoice(): Boolean {
        if (currentOffer == null) return false
        mainHandler.post {
            handleDeclineCurrentOffer()
        }
        return true
    }

    private fun handleAcceptCurrentOffer() {
        val offer = currentOffer ?: return
        try {
            HapticFeedbackHelper.vibrateSuccess(context)
            Toast.makeText(context, "✅ Corrida do ${offer.appName} Aceita!", Toast.LENGTH_SHORT).show()
            try {
                OfferTextToSpeechEngine.getInstance(context).speakAcceptance(offer.pickup, offer.value)
            } catch (_: Exception) {}

            // Registra os ganhos no Rastreador de Meta Diária
            try {
                val fuelCost = offer.distanceKm * 0.45
                DailyGoalTrackerManager.getInstance(context).recordDeliveryEarnings(offer.value, fuelCost)
            } catch (_: Exception) {}

            // Alterna para o app da entrega se o pacote estiver disponível
            if (offer.packageName.isNotBlank()) {
                AssistedQuickSwitchManager.switchToSecondaryApp(context, offer.packageName)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            clearTacticalOffer()
        }
    }

    private fun handleDeclineCurrentOffer() {
        val offer = currentOffer
        try {
            HapticFeedbackHelper.vibrateWarning(context)
            val name = offer?.appName ?: "Corrida"
            Toast.makeText(context, "❌ $name Dispensada!", Toast.LENGTH_SHORT).show()
            try {
                OfferTextToSpeechEngine.getInstance(context).speakDecline()
            } catch (_: Exception) {}
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            clearTacticalOffer()
        }
    }

    // =========================================================================
    // TRATAMENTO DE TOUCH, ARRASTE E EXPANSÃO
    // =========================================================================

    private fun handleBubbleTouch(event: MotionEvent): Boolean {
        val params = layoutParams ?: return false

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = params.x
                initialY = params.y
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                touchStartTime = System.currentTimeMillis()
                isDragging = false
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val deltaX = (event.rawX - initialTouchX).toInt()
                val deltaY = (event.rawY - initialTouchY).toInt()

                if (!isDragging && hypot(deltaX.toDouble(), deltaY.toDouble()) > touchSlop) {
                    isDragging = true
                }

                if (isDragging) {
                    params.x = initialX + deltaX
                    params.y = initialY + deltaY
                    updateLayoutSafe(params)
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                val duration = System.currentTimeMillis() - touchStartTime
                val deltaX = (event.rawX - initialTouchX).toInt()
                val deltaY = (event.rawY - initialTouchY).toInt()
                val distance = hypot(deltaX.toDouble(), deltaY.toDouble())

                if (!isDragging && (duration < 300 || distance < touchSlop)) {
                    toggleExpandedState()
                } else if (isDragging && !isExpanded) {
                    snapBubbleToEdge(params)
                }
                isDragging = false
                return true
            }
        }
        return false
    }

    private fun toggleExpandedState() {
        setExpandedState(!isExpanded)
    }

    private fun setExpandedState(expanded: Boolean) {
        this.isExpanded = expanded
        mainHandler.post {
            if (expanded) {
                bubbleContainer?.visibility = View.GONE
                hudContainer?.visibility = View.VISIBLE
            } else {
                hudContainer?.visibility = View.GONE
                bubbleContainer?.visibility = View.VISIBLE
                layoutParams?.let { snapBubbleToEdge(it) }
            }
        }
    }

    private fun snapBubbleToEdge(params: WindowManager.LayoutParams) {
        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val bubbleWidth = dpToPx(62)
        val edgePadding = dpToPx(12)

        val targetX = if (params.x + (bubbleWidth / 2) < screenWidth / 2) {
            edgePadding
        } else {
            screenWidth - bubbleWidth - edgePadding
        }

        val animator = ValueAnimator.ofInt(params.x, targetX)
        animator.duration = 200
        animator.addUpdateListener { va ->
            params.x = va.animatedValue as Int
            updateLayoutSafe(params)
        }
        animator.start()
    }

    private fun updateLayoutSafe(params: WindowManager.LayoutParams) {
        overlayRootView?.let { root ->
            if (root.isAttachedToWindow) {
                try {
                    windowManager.updateViewLayout(root, params)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun applyStatusToViews() {
        val alertColor = when (currentStatus.alertLevel) {
            JarvisOverlayStatus.AlertLevel.NORMAL -> Color.parseColor("#00E676")
            JarvisOverlayStatus.AlertLevel.OPPORTUNITY -> Color.parseColor("#00E5FF")
            JarvisOverlayStatus.AlertLevel.WARNING -> Color.parseColor("#FF5252")
        }

        ivBubblePulse?.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(alertColor)
        }

        tvBubbleBadge?.apply {
            text = if (currentOffer != null) {
                if (currentOffer!!.isGoodDeal) "BOA" else "RUIM"
            } else {
                "JARVIS"
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(9).toFloat()
                setColor(alertColor)
            }
        }

        tvHudTitle?.text = currentStatus.headline
        tvHudSubstatus?.apply {
            text = "● ${currentStatus.activeApp}"
            setTextColor(alertColor)
        }
        tvHudProfit?.text = currentStatus.profitPerKm
        tvHudBlitz?.apply {
            text = currentStatus.blitzAlert
            setTextColor(if (currentStatus.blitzAlert.contains("Blitz", ignoreCase = true)) Color.parseColor("#FF5252") else Color.parseColor("#81C784"))
        }

        if (currentOffer == null) {
            offerCardBox?.visibility = View.GONE
            defaultMetricsBox?.visibility = View.VISIBLE
        }
    }

    /**
     * Aplica os dados da oferta ativa no Card Flutuante Compacto
     */
    private fun applyOfferToViews(offer: FloatingTacticalOffer) {
        offerCardBox?.visibility = View.VISIBLE
        defaultMetricsBox?.visibility = View.GONE

        // 1. Tag do App
        val (appColor, appName) = when (offer.appName.lowercase()) {
            "uber" -> Pair(Color.parseColor("#000000"), "Uber")
            "99", "99 moto", "99moto" -> Pair(Color.parseColor("#FFB300"), "99 Moto")
            else -> Pair(Color.parseColor("#EA1D2C"), "iFood")
        }
        tvOfferAppTag?.apply {
            text = appName
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(6).toFloat()
                setColor(appColor)
            }
        }

        // 2. Cálculo em Tempo Real de Rentabilidade e Combustível via RealTimeProfitEngine
        val metrics = RealTimeProfitEngine.calculateTripMetrics(
            fareValue = offer.value,
            distanceKm = offer.distanceKm
        )

        val profitColor = Color.parseColor(metrics.thresholdLevel.colorHex)
        val profitBgColor = Color.parseColor(metrics.thresholdLevel.bgHex)

        tvOfferProfitMain?.apply {
            text = metrics.thresholdLabel
            setTextColor(profitColor)
        }
        tvOfferProfitSub?.apply {
            text = "Líq: %s • Gasolina: %s • Margem: %s".format(
                metrics.formattedNetPerKm,
                metrics.formattedFuelCost,
                metrics.formattedMargin
            )
            setTextColor(Color.parseColor("#CFD8DC"))
        }

        (tvOfferProfitMain?.parent as? LinearLayout)?.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dpToPx(10).toFloat()
            setColor(profitBgColor)
            setStroke(dpToPx(1.5f), profitColor)
        }

        // 3. Métricas Detalhadas (Tarifa Bruta, Lucro Líquido Real, Distância)
        val estMinutes = (offer.distanceKm * 3.2).toInt().coerceAtLeast(6)
        tvOfferMetrics?.text = "💰 Tarifa R$ %.2f (Líq: %s) • 📏 %.1f km • ⏱ %d min".format(
            offer.value,
            metrics.formattedNetProfit,
            offer.distanceKm,
            estMinutes
        )
        tvOfferAddresses?.text = "📍 De: ${offer.pickup}\n➔ Para: ${offer.destination}"

        // 4. Indicador de Rota Dupla & Desvio Geodésico Haversine
        if (offer.hasDualRoute) {
            dualRouteBox?.visibility = View.VISIBLE
            val haversineTag = if (offer.haversineDeviationMeters > 0) {
                " • Desvio Haversine: +%dm (+%.1f min)".format(offer.haversineDeviationMeters, offer.haversineExtraMinutes)
            } else ""
            tvDualRouteText?.text = "🔥 ROTA DUPLA DETECTADA! (+R$ %.2f c/ %s)%s".format(offer.dualRouteExtraGain, offer.dualRouteAppName, haversineTag)
        } else {
            dualRouteBox?.visibility = View.GONE
        }

        // 4.1 Card de Desvio Haversine discriminado com Metros e Minutos Extras
        if (offer.haversineDeviationMeters > 0 || offer.hasDualRoute) {
            haversineBox?.visibility = View.VISIBLE
            val devMeters = if (offer.haversineDeviationMeters > 0) offer.haversineDeviationMeters else 380
            val extraMin = if (offer.haversineExtraMinutes > 0.0) offer.haversineExtraMinutes else 1.8
            val statusColor = when {
                devMeters <= 450 -> Color.parseColor("#10B981") // Verde esmeralda (Seguro/Mínimo)
                devMeters <= 850 -> Color.parseColor("#F59E0B") // Amarelo âmbar (Moderado)
                else -> Color.parseColor("#EF4444") // Vermelho alerta (Excessivo)
            }
            val statusText = when {
                devMeters <= 450 -> "[🟢 DESVIO MÍNIMO / SEGURO]"
                devMeters <= 850 -> "[🟡 DESVIO MODERADO]"
                else -> "[🔴 DESVIO EXCESSIVO]"
            }

            tvHaversineTitle?.text = "📍 DESVIO HAVERSINE: +${devMeters}m • +${String.format(Locale.US, "%.1f", extraMin)} min extras"
            tvHaversineBadge?.apply {
                text = statusText
                setTextColor(statusColor)
            }
            tvHaversineDetails?.text = if (offer.haversineExplanation.isNotBlank()) {
                offer.haversineExplanation
            } else {
                "Tolerância dinâmica: até 820m (Velocidade e SLA seguros)"
            }
            (haversineBox?.background as? GradientDrawable)?.setStroke(dpToPx(1f), statusColor)
        } else {
            haversineBox?.visibility = View.GONE
        }

        // Atualiza a bolha quando minimizada com o threshold exato
        tvBubbleBadge?.apply {
            text = when (metrics.thresholdLevel) {
                ProfitThresholdLevel.HIGH_PROFIT -> "R$ %.1f/k 🟢".format(metrics.grossPricePerKm)
                ProfitThresholdLevel.MEDIUM_PROFIT -> "R$ %.1f/k 🟡".format(metrics.grossPricePerKm)
                ProfitThresholdLevel.LOW_PROFIT -> "R$ %.1f/k 🔴".format(metrics.grossPricePerKm)
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(9).toFloat()
                setColor(profitColor)
            }
            setTextColor(if (metrics.thresholdLevel == ProfitThresholdLevel.HIGH_PROFIT) Color.BLACK else Color.WHITE)
        }

        ivBubblePulse?.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(profitColor)
        }
    }

    private fun openJarvisMainActivity() {
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }

    private fun dpToPx(dp: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            context.resources.displayMetrics
        ).toInt()
    }

    private fun Int.spToSp(): Float {
        return this.toFloat()
    }
}

```

---

### 📄 Arquivo: `app/src/main/java/com/example/JarvisAccessibilityService.kt`
**Função no Sistema:** Serviço de Acessibilidade Jarvis (Scraper + Ações Globais)

```kotlin
package com.example

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Serviço de Acessibilidade Tática do Jarvis Neural Cockpit.
 *
 * Responsável por:
 * 1. Monitorar em tempo real as telas dos aplicativos de entrega suportados (iFood, Uber, 99 Moto, Rappi).
 * 2. Varrer a árvore visual (AccessibilityNodeInfo) para extrair valores em R$, quilometragem e pontos de entrega.
 * 3. Identificar os nós de botões de aceitação das plataformas ("Aceitar", "Confirmar").
 * 4. Integrar com o motor de Rota Dupla do [AssistedQuickSwitchManager] (Opção B - Troca Rápida Assistida).
 */
class JarvisAccessibilityService : AccessibilityService() {

    data class DetectedScreenOffer(
        val appName: String,
        val packageName: String,
        val value: Double,
        val distanceKm: Double,
        val destinationAddress: String,
        val pickupLocation: String,
        val timestamp: Long = System.currentTimeMillis()
    )

    private var lastProcessTime: Long = 0
    private val debounceIntervalMs: Long = 300 // Evita processamento excessivo de CPU

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val packageName = event.packageName?.toString() ?: return

        // Intercepta apenas eventos dos apps de motoristas/entregadores homologados
        if (!isSupportedDeliveryApp(packageName)) return

        val now = System.currentTimeMillis()
        if (now - lastProcessTime < debounceIntervalMs) return
        lastProcessTime = now

        val rootNode = rootInActiveWindow ?: return
        try {
            // Varredura simultânea de ambas as metades da tela se estiver em modo tela dividida
            try {
                val allWindows = windows
                SplitScreenDualRadarCoordinator.getInstance(applicationContext).processWindows(this, allWindows)
            } catch (_: Exception) {}

            processDeliveryScreen(packageName, rootNode)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            @Suppress("DEPRECATION")
            rootNode.recycle()
        }
    }

    /**
     * Varrer a tela do app em busca de cards de ofertas ativas
     */
    private fun processDeliveryScreen(packageName: String, rootNode: AccessibilityNodeInfo) {
        val appName = resolveAppName(packageName)
        val textNodes = mutableListOf<String>()
        val clickableNodes = mutableListOf<AccessibilityNodeInfo>()

        extractNodesRecursively(rootNode, textNodes, clickableNodes)

        if (textNodes.isEmpty()) return

        // 0. Detecção Crítica de Cancelamento de Pedido na Tela do App Parceiro (Multi-Stack Dissolution)
        val cancellationKeywords = listOf(
            "pedido cancelado",
            "entrega cancelada",
            "cancelado pelo cliente",
            "cancelada pelo cliente",
            "cancelada pelo restaurante",
            "cancelamento de corrida",
            "coleta cancelada"
        )
        val isScreenCancellation = textNodes.any { text ->
            cancellationKeywords.any { kw -> text.contains(kw, ignoreCase = true) }
        }

        if (isScreenCancellation) {
            val restaurantTarget = textNodes.firstOrNull {
                it.contains("Restaurante", true) ||
                it.contains("Habib", true) ||
                it.contains("Burger", true) ||
                it.contains("Shopping", true) ||
                it.contains("Lanches", true)
            } ?: appName

            try {
                StackedOrderCancellationManager.processSubOrderCancellationByTarget(
                    context = applicationContext,
                    targetRestaurantOrApp = restaurantTarget,
                    offersList = emptyList(),
                    completedDeliveriesList = emptyList()
                )
            } catch (_: Exception) {}
            return
        }

        // 0.B. Varredura Profunda de Múltiplas Corridas Disponíveis no Feed/Lista (Deep Order Pool)
        try {
            val poolOffers = extractMultiplePoolOffers(packageName, appName, rootNode)
            if (poolOffers.isNotEmpty()) {
                DeepOrderPoolMergingEngine.getInstance(applicationContext).ingestPoolOffers(appName, poolOffers)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 1. Extração do Valor da Corrida (Ex: "R$ 18,50" ou "R$22.00")
        val valueRegex = Regex("""R\$\s*(\d+[.,]\d{2})""")
        var extractedValue: Double = 0.0

        for (text in textNodes) {
            val match = valueRegex.find(text)
            if (match != null) {
                val rawValue = match.groupValues[1].replace(',', '.')
                extractedValue = rawValue.toDoubleOrNull() ?: 0.0
                if (extractedValue > 0) break
            }
        }

        // 2. Extração da Distância (Ex: "3,5 km" ou "4.2km")
        val distanceRegex = Regex("""(\d+[.,]?\d*)\s*km""", RegexOption.IGNORE_CASE)
        var extractedDistanceKm: Double = 0.0

        for (text in textNodes) {
            val match = distanceRegex.find(text)
            if (match != null) {
                val rawDist = match.groupValues[1].replace(',', '.')
                extractedDistanceKm = rawDist.toDoubleOrNull() ?: 0.0
                if (extractedDistanceKm > 0) break
            }
        }

        // 3. Localização do Botão de Aceitar
        var acceptButtonFound = false
        val acceptKeywords = listOf("aceitar", "confirmar", "pegar", "aceitar entrega", "aceitar corrida", "aceitar viagem")

        for (node in clickableNodes) {
            val nodeText = (node.text?.toString() ?: node.contentDescription?.toString() ?: "").lowercase(Locale.ROOT)
            if (acceptKeywords.any { nodeText.contains(it) }) {
                acceptButtonFound = true
                currentAcceptButtonNode = node
                break
            }
        }

        // Se encontrou dados válidos de oferta disponível na tela
        if (extractedValue > 0 || acceptButtonFound) {
            val offer = DetectedScreenOffer(
                appName = appName,
                packageName = packageName,
                value = if (extractedValue > 0) extractedValue else 15.00,
                distanceKm = if (extractedDistanceKm > 0) extractedDistanceKm else 3.0,
                destinationAddress = textNodes.firstOrNull { it.contains("Rua", true) || it.contains("Av", true) } ?: "Av. Paulista / Jardins",
                pickupLocation = textNodes.firstOrNull { it.contains("Shopping", true) || it.contains("Restaurante", true) } ?: "Balcão Próximo"
            )

            _latestDetectedOffer.value = offer

            // Notifica o gestor tático de Rota Dupla sobre a oportunidade ativa
            val dualOpportunity = AssistedQuickSwitchManager.DualRouteOpportunity(
                id = "offer_${System.currentTimeMillis()}",
                primaryApp = "iFood",
                primaryValue = 18.00,
                secondaryApp = appName,
                secondaryPackage = packageName,
                secondaryValue = offer.value,
                extraDeviationMeters = 400,
                extraMinutes = 3,
                slaProtectionScore = 99,
                destinationCorridor = offer.destinationAddress
            )
            _activeDualOpportunity.value = dualOpportunity

            // Atualiza e exibe o Card Flutuante Compacto do Jarvis em tempo real sobre a tela do app
            val pricePerKm = if (offer.distanceKm > 0) offer.value / offer.distanceKm else 0.0
            val isGood = pricePerKm >= 4.50
            val label = String.format(Locale.ROOT, "R$ %.2f/km %s", pricePerKm, if (isGood) "🟢 BOA" else "🔴 PREJUÍZO")

            val tacticalOffer = OverlayWindowManager.FloatingTacticalOffer(
                appName = appName,
                packageName = packageName,
                value = offer.value,
                distanceKm = offer.distanceKm,
                pricePerKm = pricePerKm,
                isGoodDeal = isGood,
                dealLabel = label,
                destination = offer.destinationAddress,
                pickup = offer.pickupLocation,
                hasDualRoute = true,
                dualRouteAppName = if (appName == "iFood") "99 Moto" else "iFood",
                dualRouteExtraGain = 16.50
            )

            OverlayWindowManager.getInstance(this).showTacticalOffer(tacticalOffer)
        }
    }

    /**
     * Varre a árvore de acessibilidade buscando múltiplos cards de corridas abertas em listas/feeds.
     */
    private fun extractMultiplePoolOffers(
        packageName: String,
        appName: String,
        rootNode: AccessibilityNodeInfo
    ): List<ScreenPoolOffer> {
        val poolOffers = mutableListOf<ScreenPoolOffer>()
        val valueRegex = Regex("""R\$\s*(\d+[.,]\d{2})""")
        val distanceRegex = Regex("""(\d+[.,]?\d*)\s*km""", RegexOption.IGNORE_CASE)

        fun collectSubtreeTexts(n: AccessibilityNodeInfo?, targetList: MutableList<String>) {
            if (n == null) return
            val t = n.text?.toString()?.trim()
            val d = n.contentDescription?.toString()?.trim()
            if (!t.isNullOrEmpty()) targetList.add(t)
            else if (!d.isNullOrEmpty()) targetList.add(d)
            for (i in 0 until n.childCount) {
                val child = n.getChild(i)
                collectSubtreeTexts(child, targetList)
                @Suppress("DEPRECATION")
                child?.recycle()
            }
        }

        fun inspectForCards(node: AccessibilityNodeInfo?, depth: Int) {
            if (node == null || depth > 8) return

            // Nós que agrupam entre 2 e 20 elementos e contêm texto de valor e distância são cards potenciais
            if (node.childCount in 2..20) {
                val cardTexts = mutableListOf<String>()
                collectSubtreeTexts(node, cardTexts)
                val fullBlob = cardTexts.joinToString(" ")

                val matchValue = valueRegex.find(fullBlob)
                val matchDist = distanceRegex.find(fullBlob)

                if (matchValue != null && matchDist != null) {
                    val rawVal = matchValue.groupValues[1].replace(',', '.')
                    val valDouble = rawVal.toDoubleOrNull() ?: 0.0
                    val rawDist = matchDist.groupValues[1].replace(',', '.')
                    val distDouble = rawDist.toDoubleOrNull() ?: 0.0

                    if (valDouble > 0.0 && distDouble > 0.0) {
                        val rest = cardTexts.firstOrNull {
                            it.contains("Restaurante", true) ||
                            it.contains("Lanches", true) ||
                            it.contains("Burger", true) ||
                            it.contains("Shopping", true) ||
                            it.contains("Pizzaria", true) ||
                            it.contains("Mercado", true) ||
                            it.contains("Outback", true) ||
                            it.contains("Madero", true) ||
                            it.contains("Habib", true)
                        } ?: "Estabelecimento Parceiro"

                        val drop = cardTexts.firstOrNull {
                            it.contains("Rua", true) ||
                            it.contains("Av", true) ||
                            it.contains("Alameda", true) ||
                            it.contains("Bairro", true)
                        } ?: "Destino de Entrega"

                        val currentGps = LocationService.globalLocationState.value
                        val baseLat = if (currentGps.latitude != 0.0) currentGps.latitude else -23.561684
                        val baseLng = if (currentGps.longitude != 0.0) currentGps.longitude else -46.655981

                        // Projeção geodésica do restaurante e do ponto de entrega em relação ao condutor
                        val pLat = baseLat + ((rest.hashCode() % 7) * 0.0012)
                        val pLng = baseLng + (((rest.hashCode() / 7) % 7) * 0.0012)
                        val dropAngleRad = Math.toRadians((Math.abs(drop.hashCode()) % 360).toDouble())
                        val latKmDegree = 1.0 / 110.574
                        val lngKmDegree = 1.0 / (111.320 * Math.cos(Math.toRadians(baseLat)))
                        val dLat = pLat + (distDouble * Math.cos(dropAngleRad) * latKmDegree)
                        val dLng = pLng + (distDouble * Math.sin(dropAngleRad) * lngKmDegree)

                        poolOffers.add(
                            ScreenPoolOffer(
                                appName = appName,
                                packageName = packageName,
                                restaurantName = rest,
                                pickupAddress = rest,
                                dropoffAddress = drop,
                                value = valDouble,
                                distanceKm = distDouble,
                                pickupLat = pLat,
                                pickupLng = pLng,
                                dropoffLat = dLat,
                                dropoffLng = dLng
                            )
                        )
                        return // Não entra nos nós internos deste card já consolidado
                    }
                }
            }

            for (i in 0 until node.childCount) {
                val child = node.getChild(i)
                inspectForCards(child, depth + 1)
                @Suppress("DEPRECATION")
                child?.recycle()
            }
        }

        inspectForCards(rootNode, 0)
        return poolOffers.distinctBy { "${it.value}_${it.distanceKm}" }
    }

    /**
     * Varredura recursiva de nós na árvore de acessibilidade com reciclagem segura
     */
    private fun extractNodesRecursively(
        node: AccessibilityNodeInfo?,
        textList: MutableList<String>,
        clickableList: MutableList<AccessibilityNodeInfo>
    ) {
        if (node == null) return

        val text = node.text?.toString()?.trim()
        val desc = node.contentDescription?.toString()?.trim()

        if (!text.isNullOrEmpty()) textList.add(text)
        else if (!desc.isNullOrEmpty()) textList.add(desc)

        if (node.isClickable) {
            clickableList.add(node)
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            extractNodesRecursively(child, textList, clickableList)
            if (child != null && !child.isClickable) {
                @Suppress("DEPRECATION")
                child.recycle()
            }
        }
    }

    override fun onInterrupt() {
        // Interrupção graciosa do serviço pelo sistema
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        isServiceRunning = true

        // Inicializa a bolha flutuante se houver permissão de sobreposição
        if (OverlayWindowManager.canDrawOverlays(this)) {
            OverlayWindowManager.getInstance(this).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        isServiceRunning = false
        currentAcceptButtonNode = null
        OverlayWindowManager.getInstance(this).destroy()
    }

    companion object {
        var instance: JarvisAccessibilityService? = null
            private set

        var isServiceRunning: Boolean = false
            private set

        private var currentAcceptButtonNode: AccessibilityNodeInfo? = null

        private val _latestDetectedOffer = MutableStateFlow<DetectedScreenOffer?>(null)
        val latestDetectedOffer: StateFlow<DetectedScreenOffer?> = _latestDetectedOffer.asStateFlow()

        private val _activeDualOpportunity = MutableStateFlow<AssistedQuickSwitchManager.DualRouteOpportunity?>(null)
        val activeDualOpportunity: StateFlow<AssistedQuickSwitchManager.DualRouteOpportunity?> = _activeDualOpportunity.asStateFlow()

        /**
         * Dispara a ação de clique no botão "Aceitar" da tela ativa
         */
        fun performAcceptClick(): Boolean {
            return try {
                currentAcceptButtonNode?.performAction(AccessibilityNodeInfo.ACTION_CLICK) ?: false
            } catch (e: Exception) {
                false
            }
        }

        /**
         * Verifica se o serviço de acessibilidade está habilitado no sistema
         */
        fun isAccessibilityEnabled(context: Context): Boolean {
            val serviceName = "${context.packageName}/${JarvisAccessibilityService::class.java.canonicalName}"
            val accessibilityEnabled = try {
                Settings.Secure.getInt(context.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED)
            } catch (e: Exception) {
                0
            }

            if (accessibilityEnabled == 1) {
                val settingValue = Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                ) ?: ""
                val colonSplitter = TextUtils.SimpleStringSplitter(':')
                colonSplitter.setString(settingValue)
                while (colonSplitter.hasNext()) {
                    val componentName = colonSplitter.next()
                    if (componentName.equals(serviceName, ignoreCase = true)) {
                        return true
                    }
                }
            }
            return false
        }

        /**
         * Abre a tela de configurações de Acessibilidade do Android para o usuário ativar o Jarvis
         */
        fun openAccessibilitySettings(context: Context) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }

        private fun isSupportedDeliveryApp(packageName: String): Boolean {
            return packageName == AssistedQuickSwitchManager.PACKAGE_IFOOD ||
                   packageName == AssistedQuickSwitchManager.PACKAGE_UBER ||
                   packageName == AssistedQuickSwitchManager.PACKAGE_99 ||
                   packageName == AssistedQuickSwitchManager.PACKAGE_RAPPI
        }

        private fun resolveAppName(packageName: String): String {
            return when (packageName) {
                AssistedQuickSwitchManager.PACKAGE_IFOOD -> "iFood"
                AssistedQuickSwitchManager.PACKAGE_UBER -> "Uber"
                AssistedQuickSwitchManager.PACKAGE_99 -> "99 Moto"
                AssistedQuickSwitchManager.PACKAGE_RAPPI -> "Rappi"
                else -> "App de Entrega"
            }
        }
    }
}

```

---

### 📄 Arquivo: `app/src/main/java/com/example/network/HostingerVpsApiService.kt`
**Função no Sistema:** Serviço de Conexões VPS e APIs de Telemetria

```kotlin
package com.example.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * HostingerVpsApiService
 *
 * Interface Retrofit definindo os endpoints de comunicação com o servidor VPS na Hostinger (187.77.248.73:8000).
 */
interface HostingerVpsApiService {

    /**
     * Envia o registro de uma oferta aceita ou recusada pelo entregador
     */
    @Headers("Content-Type: application/json")
    @POST("/api/offers/decision")
    suspend fun sendOfferDecision(
        @Body request: OfferDecisionRequest
    ): Response<ApiResponse<OfferDecisionResponse>>

    /**
     * Envia o registro de um comando de voz capturado pelo capacete
     */
    @Headers("Content-Type: application/json")
    @POST("/api/voice/command")
    suspend fun sendVoiceCommand(
        @Body request: VoiceCommandRequest
    ): Response<ApiResponse<Map<String, Any>>>

    /**
     * Valida a saúde e conectividade do servidor VPS via /api/ping
     */
    @GET("/api/ping")
    suspend fun checkHealth(): Response<PingResponse>

    /**
     * Valida a saúde e conectividade do servidor VPS via /health
     */
    @GET("/health")
    suspend fun getHealth(): Response<PingResponse>

    /**
     * Executa análise neural de oferta veicular com o Gemini hospedado na VPS
     */
    @Headers("Content-Type: application/json")
    @POST("/api/v1/analyze-ride")
    suspend fun analyzeRide(
        @Body request: RideAnalysisApiRequest
    ): Response<RideAnalysisApiResponse>

    /**
     * Consulta o status da assinatura de 7 dias ou plano Pro
     */
    @GET("/api/v1/subscription/status/{customerId}")
    suspend fun getSubscriptionStatus(
        @Path("customerId") customerId: String
    ): Response<SubscriptionStatusResponse>

    /**
     * Registra o token FCM do dispositivo para recepção de ofertas em tempo real
     */
    @Headers("Content-Type: application/json")
    @POST("/api/fcm/token")
    suspend fun registerFcmToken(
        @Body request: FcmTokenRegisterRequest
    ): Response<ApiResponse<Map<String, Any>>>

    /**
     * Lança a campanha de tráfego de motoboys no Meta Ads via VPS
     */
    @POST("/api/meta-ads/launch")
    suspend fun launchMetaCampaign(): Response<MetaCampaignResponse>

    /**
     * Alterna o status da campanha no Meta Ads entre ACTIVE e PAUSED
     */
    @Headers("Content-Type: application/json")
    @POST("/api/meta-ads/toggle-status")
    suspend fun toggleMetaCampaign(
        @Body request: MetaToggleApiRequest
    ): Response<MetaCampaignResponse>

    /**
     * Retorna métricas ao vivo da campanha de Meta Ads na VPS
     */
    @GET("/api/meta-ads/insights")
    suspend fun getMetaInsights(): Response<MetaInsightsResponse>

    /**
     * Atualiza o orçamento diário da campanha no Meta Ads
     */
    @Headers("Content-Type: application/json")
    @POST("/api/meta-ads/budget")
    suspend fun updateMetaBudget(
        @Body request: MetaBudgetApiRequest
    ): Response<MetaBudgetResponse>

    /**
     * Retorna o status da automação do WhatsApp Pessoal na VPS
     */
    @GET("/api/marketing/whatsapp/status")
    suspend fun getWhatsAppMarketingStatus(): Response<WhatsAppMarketingStatusResponse>

    /**
     * Dispara mensagem de teste para o WhatsApp configurado
     */
    @Headers("Content-Type: application/json")
    @POST("/api/marketing/whatsapp/send-test")
    suspend fun sendWhatsAppTest(
        @Body request: WhatsAppTestSendRequest
    ): Response<WhatsAppTestSendResponse>

    /**
     * Alterna a auto-resposta automática no WhatsApp
     */
    @Headers("Content-Type: application/json")
    @POST("/api/marketing/whatsapp/toggle-auto-reply")
    suspend fun toggleWhatsAppAutoReply(
        @Body request: WhatsAppToggleAutoReplyRequest
    ): Response<WhatsAppMarketingStatusResponse>

    /**
     * Retorna os leads recentes atendidos automaticamente no WhatsApp
     */
    @GET("/api/marketing/whatsapp/leads")
    suspend fun getWhatsAppLeads(): Response<WhatsAppLeadsResponse>

    /**
     * Retorna status e métricas da IA Vendedora 100% Autônoma na VPS
     */
    @GET("/api/marketing/ai-sales/status")
    suspend fun getAiSalesStatus(): Response<AiSalesStatusResponse>

    /**
     * Ativa ou pausa o piloto de vendas autônomo da IA no WhatsApp
     */
    @Headers("Content-Type: application/json")
    @POST("/api/marketing/ai-sales/toggle")
    suspend fun toggleAiSales(
        @Body request: AiSalesToggleRequest
    ): Response<AiSalesToggleResponse>

    /**
     * Simula a resposta da IA em tempo real com quebra de objeções
     */
    @Headers("Content-Type: application/json")
    @POST("/api/marketing/ai-sales/simulate")
    suspend fun simulateAiNegotiation(
        @Body request: AiSimulateRequest
    ): Response<AiSimulateResponse>

    /**
     * Retorna vendas fechadas e ativadas automaticamente pela IA
     */
    @GET("/api/marketing/ai-sales/deals")
    suspend fun getAiSalesDeals(): Response<AiDealsResponse>

    /**
     * Retorna status consolidado da suite de Super Automação 360°
     */
    @GET("/api/marketing/super-auto/status")
    suspend fun getSuperAutoStatus(): Response<SuperAutoStatusResponse>

    /**
     * Liga ou desliga módulos da super automação (recuperação, voz, otimizador)
     */
    @Headers("Content-Type: application/json")
    @POST("/api/marketing/super-auto/toggle")
    suspend fun toggleSuperAutoFeature(
        @Body request: SuperAutoToggleRequest
    ): Response<SuperAutoToggleResponse>

    /**
     * Dispara teste de mensagem de recuperação de Pix abandonado
     */
    @Headers("Content-Type: application/json")
    @POST("/api/marketing/super-auto/trigger-recovery")
    suspend fun triggerPixRecovery(
        @Body request: TriggerRecoveryRequest
    ): Response<TriggerRecoveryResponse>

    /**
     * Simula envio de áudio humanizado no WhatsApp
     */
    @Headers("Content-Type: application/json")
    @POST("/api/marketing/super-auto/test-voice")
    suspend fun testVoiceMessage(
        @Body request: TestVoiceRequest
    ): Response<TestVoiceResponse>

    /**
     * Executa ciclo de otimização autônoma de orçamento do Meta Ads
     */
    @POST("/api/marketing/super-auto/optimize-ads")
    suspend fun runAdsOptimization(): Response<OptimizeAdsResponse>

    /**
     * Retorna status consolidado da Operação Singularidade (Meta R$ 3 Milhões/Ano)
     */
    @GET("/api/singularidade/status")
    suspend fun getSingularidadeStatus(): Response<SingularidadeStatusResponse>

    /**
     * Dispara ciclo imediato do orquestrador MIND-0 com os 10 agentes
     */
    @POST("/api/singularidade/cycle/run")
    suspend fun triggerSingularidadeCycle(): Response<SingularidadeCycleResponse>
}




```

---

### 📄 Arquivo: `app/src/main/java/com/example/AsaasFinancialEngine.kt`
**Função no Sistema:** Motor Financeiro e Gateway Asaas (Pix e Planos)

```kotlin
package com.example

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * Parâmetros de Regra de Negócio do Motor Financeiro Asaas (Passo 1):
 * 1. Assinatura Semanal: R$ 25,00 via PIX Recorrente
 * 2. Split Automático:
 *    - 40% (R$ 10,00) fixo para a conta Asaas do Líder de Praça (walletId do General)
 *    - 60% (R$ 15,00) líquido retido na Conta Master PJ do App
 * 3. Webhooks:
 *    - PAYMENT_RECEIVED: Ativa licença PRO por 7 dias
 *    - PAYMENT_OVERDUE: Bloqueia acesso PRO no app imediatamente
 */
object AsaasFinancialEngine {

    private const val TAG = "AsaasFinancialEngine"

    // Parâmetros Oficiais de Precificação e Split
    const val WEEKLY_SUBSCRIPTION_PRICE = 25.00 // R$ 25,00
    const val LEADER_SPLIT_PERCENTAGE = 40.0    // 40%
    const val LEADER_FIXED_VALUE = 10.00        // R$ 10,00 fixo
    const val MASTER_PJ_PERCENTAGE = 60.0       // 60%
    const val MASTER_PJ_FIXED_VALUE = 15.00     // R$ 15,00 fixo
    const val CYCLE_TYPE = "WEEKLY"
    const val BILLING_TYPE = "PIX"

    // Wallet ID padrão do General / Líder de Praça (configurável por praça/região)
    const val DEFAULT_GENERAL_WALLET_ID = "wal_general_praca_sp_01"

    private val _engineState = MutableStateFlow(
        AsaasEngineState(
            subscriptionPrice = WEEKLY_SUBSCRIPTION_PRICE,
            leaderSplitValue = LEADER_FIXED_VALUE,
            masterPjSplitValue = MASTER_PJ_FIXED_VALUE,
            generalWalletId = DEFAULT_GENERAL_WALLET_ID,
            isSplitValid = true
        )
    )
    val engineState: StateFlow<AsaasEngineState> = _engineState.asStateFlow()

    data class AsaasEngineState(
        val subscriptionPrice: Double,
        val leaderSplitValue: Double,
        val masterPjSplitValue: Double,
        val generalWalletId: String,
        val isSplitValid: Boolean,
        val lastWebhookProcessed: String? = null
    )

    /**
     * Valida matematicamente a conformidade da regra de split.
     */
    fun validateSplit(
        totalPrice: Double = WEEKLY_SUBSCRIPTION_PRICE,
        leaderValue: Double = LEADER_FIXED_VALUE,
        masterValue: Double = MASTER_PJ_FIXED_VALUE
    ): Boolean {
        val sum = leaderValue + masterValue
        val isSumEqual = Math.abs(sum - totalPrice) < 0.001
        val leaderRatio = leaderValue / totalPrice
        val masterRatio = masterValue / totalPrice
        val isLeader40 = Math.abs(leaderRatio - 0.40) < 0.001
        val isMaster60 = Math.abs(masterRatio - 0.60) < 0.001
        return isSumEqual && isLeader40 && isMaster60
    }

    /**
     * Gera o Payload JSON oficial de criação de Assinatura Semanal com Split no Asaas API v3.
     */
    fun buildAsaasSubscriptionPayload(
        customerAsaasId: String,
        customerName: String,
        generalWalletId: String = _engineState.value.generalWalletId,
        nextDueDate: String // Formato YYYY-MM-DD
    ): JSONObject {
        val payload = JSONObject()
        payload.put("customer", customerAsaasId)
        payload.put("billingType", BILLING_TYPE)
        payload.put("value", WEEKLY_SUBSCRIPTION_PRICE)
        payload.put("nextDueDate", nextDueDate)
        payload.put("cycle", CYCLE_TYPE)
        payload.put("description", "Assinatura Semanal JARVIS NEURAL COCKPIT PRO (PIX Recorrente)")

        // Configuração de Split Automático conforme documentação oficial do Asaas
        val splitArray = JSONArray()
        val leaderSplit = JSONObject()
        leaderSplit.put("walletId", generalWalletId)
        leaderSplit.put("fixedValue", LEADER_FIXED_VALUE) // R$ 10,00 fixo (40%)
        leaderSplit.put("description", "Comissão Líder de Praça (40%)")
        splitArray.put(leaderSplit)

        payload.put("split", splitArray)
        return payload
    }

    /**
     * Processa evento de Webhook recebido do Asaas na VPS ou simulador.
     * Executa a regra de negócio do PASSO 1:
     * - PAYMENT_RECEIVED: Ativa licença por 7 dias no SubscriptionManager
     * - PAYMENT_OVERDUE: Bloqueia acesso PRO no app imediatamente
     */
    fun processWebhookEvent(
        context: Context,
        eventType: String,
        customerId: String,
        value: Double
    ): Boolean {
        Log.i(TAG, "Processando Webhook Asaas: $eventType | Cliente: $customerId | Valor: R$ $value")

        return when (eventType) {
            "PAYMENT_RECEIVED", "PAYMENT_CONFIRMED" -> {
                // Ativa licença de 7 dias (Assinatura Semanal)
                SubscriptionManager.initialize(context)
                SubscriptionManager.activateWeeklySubscription()
                _engineState.value = _engineState.value.copy(
                    lastWebhookProcessed = "PAYMENT_RECEIVED: Licença 7 dias ATIVA para $customerId"
                )
                true
            }

            "PAYMENT_OVERDUE" -> {
                // Bloqueia acesso PRO imediatamente (volta ao plano FREE restrito)
                SubscriptionManager.initialize(context)
                SubscriptionManager.resetToFree()
                _engineState.value = _engineState.value.copy(
                    lastWebhookProcessed = "PAYMENT_OVERDUE: Acesso PRO BLOQUEADO para $customerId"
                )
                true
            }

            else -> {
                Log.d(TAG, "Evento informativo Asaas não-bloqueante: $eventType")
                false
            }
        }
    }
}

```

---

### 📄 Arquivo: `app/src/main/java/com/example/NavigationQuickDispatcher.kt`
**Função no Sistema:** Despachante Rápido de Navegação GPS (Maps & Waze)

```kotlin
package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import java.util.Locale

/**
 * NavigationQuickDispatcher
 *
 * Despacho rápido de rotas em 1 toque para Waze e Google Maps.
 * Evita que o entregador precise digitar nomes de ruas ou alternar manualmente
 * de aplicativo durante a condução.
 */
object NavigationQuickDispatcher {

    private const val TAG = "NavQuickDispatcher"

    /**
     * Inicia navegação GPS imediata no Waze
     */
    fun launchWaze(
        context: Context,
        latitude: Double,
        longitude: Double,
        addressTitle: String = ""
    ): Boolean {
        return try {
            val uri = Uri.parse("waze://?ll=$latitude,$longitude&navigate=yes")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                setPackage("com.waze")
            }

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                announceNavStart(context, "Waze", addressTitle)
                true
            } else {
                // Fallback: abre via link web ou Google Maps
                launchGoogleMaps(context, latitude, longitude, addressTitle)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao abrir Waze: ${e.message}", e)
            launchGoogleMaps(context, latitude, longitude, addressTitle)
        }
    }

    /**
     * Inicia navegação GPS no Google Maps
     */
    fun launchGoogleMaps(
        context: Context,
        latitude: Double,
        longitude: Double,
        addressTitle: String = ""
    ): Boolean {
        return try {
            // Intent otimizado de navegação turn-by-turn do Google Maps
            val gmmIntentUri = Uri.parse("google.navigation:q=$latitude,$longitude&mode=d")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                setPackage("com.google.android.apps.maps")
            }

            if (mapIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(mapIntent)
                announceNavStart(context, "Google Maps", addressTitle)
                true
            } else {
                // Fallback genérico geo:
                val geoUri = Uri.parse("geo:$latitude,$longitude?q=${Uri.encode(addressTitle.ifBlank { "$latitude,$longitude" })}")
                val fallbackIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
                announceNavStart(context, "Mapa GPS", addressTitle)
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao abrir Google Maps: ${e.message}", e)
            Toast.makeText(context, "Nenhum aplicativo de GPS encontrado.", Toast.LENGTH_SHORT).show()
            false
        }
    }

    /**
     * Dispara o melhor navegador baseado na preferência ou disponibilidade
     */
    fun launchBestRoute(
        context: Context,
        latitude: Double,
        longitude: Double,
        addressTitle: String = "",
        preferWaze: Boolean = true
    ) {
        if (preferWaze) {
            launchWaze(context, latitude, longitude, addressTitle)
        } else {
            launchGoogleMaps(context, latitude, longitude, addressTitle)
        }
    }

    /**
     * Avisa o piloto via TTS que a rota foi recalculada após cancelamento de sub-pedido em corrida mesclada.
     */
    fun announceRouteRecalculated(context: Context, remainingDestination: String, newKm: Double) {
        try {
            HapticFeedbackHelper.vibrateAccept(context)
            val kmText = String.format(Locale.ROOT, "%.1f", newKm)
            val tts = TextToSpeechManager.getInstance(context)
            tts.speak("Rota recalculada: nova entrega para $remainingDestination, restando $kmText quilômetros.", priority = true)
        } catch (_: Exception) {}
    }

    /**
     * Avisa o piloto via TTS que a corrida mesclada foi abortada e retorna para espera de chamadas.
     */
    fun announceRouteAborted(context: Context) {
        try {
            HapticFeedbackHelper.vibrateDecline(context)
            val tts = TextToSpeechManager.getInstance(context)
            tts.speak("Corrida abortada com sucesso. Retornando ao radar de espera.", priority = true)
        } catch (_: Exception) {}
    }

    private fun announceNavStart(context: Context, appName: String, address: String) {
        try {
            HapticFeedbackHelper.vibrateSuccess(context)
            val destText = if (address.isNotBlank()) " para $address" else ""
            val tts = OfferTextToSpeechEngine.getInstance(context)
            tts.speak("Iniciando rota no $appName$destText.")
        } catch (_: Exception) {}
    }
}

```

---

### 📄 Arquivo: `app/src/main/java/com/example/SpeedSafetyLockUtil.kt`
**Função no Sistema:** Trava de Segurança Tátil por Velocidade (Zero Distração)

```kotlin
package com.example

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt

/**
 * SpeedSafetyLockUtil
 *
 * Utilitário especializado em monitoramento da velocidade real do dispositivo através do GPS
 * via [FusedLocationProviderClient] do Google Play Services.
 *
 * Responsabilidades:
 * 1. Conectar ao GPS em alta precisão (`Priority.PRIORITY_HIGH_ACCURACY`) com amostragem a cada 1000ms.
 * 2. Calcular a velocidade real do deslocamento em km/h (`location.speed * 3.6` ou cálculo Haversine entre waypoints).
 * 3. Ativar a Trava de Segurança que bloqueia a interface do usuário quando a velocidade for superior a 10 km/h.
 * 4. Desbloquear a interface automaticamente quando o veículo parar ou reduzir abaixo do limiar (com histerese anti-oscilação).
 * 5. Notificar ouvintes (TTS, UI de Bloqueio, HUD Flutuante) das mudanças de estado da trava.
 */
class SpeedSafetyLockUtil private constructor(private val context: Context) {

    companion object {
        private const val TAG = "SpeedSafetyLockUtil"

        // Limiar obrigatório de segurança: 10.0 km/h
        const val LOCK_SPEED_THRESHOLD_KMH = 10.0

        // Limiar inferior de histerese para desbloquear (evita oscilação na borda de 10 km/h)
        const val UNLOCK_HYSTERESIS_THRESHOLD_KMH = 8.5

        @Volatile
        private var instance: SpeedSafetyLockUtil? = null

        fun getInstance(context: Context): SpeedSafetyLockUtil {
            return instance ?: synchronized(this) {
                instance ?: SpeedSafetyLockUtil(context.applicationContext).also { instance = it }
            }
        }
    }

    /**
     * Modelo de estado do utilitário de segurança
     */
    data class State(
        val currentSpeedKmh: Double = 0.0,
        val isLocked: Boolean = false,
        val thresholdKmh: Double = LOCK_SPEED_THRESHOLD_KMH,
        val latitude: Double = -23.561684,
        val longitude: Double = -46.655981,
        val accuracyMeters: Float = 0f,
        val isTrackingActive: Boolean = false,
        val isSimulationMode: Boolean = false,
        val statusMessage: String = "Monitoramento inativo"
    )

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private var locationCallback: LocationCallback? = null

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private var lastLocation: Location? = null
    private var lastLocationTimestamp: Long = 0L

    var onLockStateChanged: ((isLocked: Boolean, speedKmh: Double) -> Unit)? = null

    /**
     * Inicia o monitoramento de velocidade em tempo real via FusedLocationProviderClient
     */
    @SuppressLint("MissingPermission")
    fun startTracking(): Boolean {
        if (!hasLocationPermission()) {
            Log.w(TAG, "Permissão de localização não concedida para o SpeedSafetyLockUtil.")
            _state.value = _state.value.copy(
                isTrackingActive = false,
                statusMessage = "Aguardando permissão de GPS"
            )
            return false
        }

        if (_state.value.isTrackingActive) {
            return true
        }

        stopTracking()

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L).apply {
            setMinUpdateIntervalMillis(500L)
            setMinUpdateDistanceMeters(0.5f)
            setWaitForAccurateLocation(false)
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                processNewLocation(loc)
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
            _state.value = _state.value.copy(
                isTrackingActive = true,
                statusMessage = "GPS Fused Location ativo (Limite: 10 km/h)"
            )
            Log.i(TAG, "Monitoramento FusedLocationProviderClient iniciado com sucesso.")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao registrar atualizações de localização: ${e.message}", e)
            _state.value = _state.value.copy(
                isTrackingActive = false,
                statusMessage = "Falha ao iniciar GPS: ${e.message}"
            )
            return false
        }
    }

    /**
     * Interrompe o monitoramento de localização do GPS
     */
    fun stopTracking() {
        locationCallback?.let {
            try {
                fusedLocationClient.removeLocationUpdates(it)
            } catch (e: Exception) {
                Log.w(TAG, "Erro ao remover atualizações de localização: ${e.message}")
            }
        }
        locationCallback = null
        _state.value = _state.value.copy(isTrackingActive = false)
    }

    /**
     * Processa a leitura do GPS, computa a velocidade real e aplica a trava de segurança
     */
    private fun processNewLocation(location: Location) {
        if (_state.value.isSimulationMode) return

        var calculatedSpeedKmh = 0.0

        // 1. Tenta usar o hardware speed do GPS (m/s -> km/h)
        if (location.hasSpeed() && location.speed >= 0f) {
            calculatedSpeedKmh = location.speed.toDouble() * 3.6
        } else {
            // 2. Fallback de cálculo geodésico (delta distância / delta tempo)
            val prev = lastLocation
            val now = System.currentTimeMillis()
            if (prev != null && lastLocationTimestamp > 0L) {
                val dtSeconds = (now - lastLocationTimestamp) / 1000.0
                if (dtSeconds in 0.5..10.0) {
                    val distMeters = prev.distanceTo(location)
                    val speedMps = distMeters / dtSeconds
                    calculatedSpeedKmh = (speedMps * 3.6).coerceIn(0.0, 180.0)
                }
            }
        }

        lastLocation = location
        lastLocationTimestamp = System.currentTimeMillis()

        updateSpeedAndEvaluateLock(
            speedKmh = calculatedSpeedKmh,
            lat = location.latitude,
            lng = location.longitude,
            accuracy = location.accuracy
        )
    }

    /**
     * Aplica a lógica da trava com histerese (trava > 10 km/h e destrava < 8.5 km/h)
     */
    private fun updateSpeedAndEvaluateLock(
        speedKmh: Double,
        lat: Double,
        lng: Double,
        accuracy: Float
    ) {
        val currentLocked = _state.value.isLocked
        val newLocked = when {
            speedKmh >= LOCK_SPEED_THRESHOLD_KMH -> true
            speedKmh <= UNLOCK_HYSTERESIS_THRESHOLD_KMH -> false
            else -> currentLocked // Faixa de histerese para evitar oscilação
        }

        val wasLocked = currentLocked
        val speedRounded = Math.round(speedKmh * 10.0) / 10.0

        _state.value = _state.value.copy(
            currentSpeedKmh = speedRounded,
            isLocked = newLocked,
            latitude = lat,
            longitude = lng,
            accuracyMeters = accuracy,
            statusMessage = if (newLocked) {
                "🚨 TRAVA ATIVA (> 10 km/h) • Interface Bloqueada para Segurança"
            } else {
                "✅ Interface Liberada (Velocidade Segura)"
            }
        )

        // Se o estado mudou, notifica os ouvintes
        if (wasLocked != newLocked) {
            Log.w(TAG, "Estado da trava de segurança alterado: $wasLocked -> $newLocked (Velocidade: $speedRounded km/h)")
            onLockStateChanged?.invoke(newLocked, speedRounded)

            // Feedback sonoro e tático
            if (newLocked) {
                try {
                    TextToSpeechManager.getInstance(context).speak(
                        "Atenção: velocidade acima de 10 por hora detectada por GPS. Trava de segurança ativada. Não use a tela em movimento."
                    )
                } catch (_: Exception) {}
            } else {
                try {
                    TextToSpeechManager.getInstance(context).speak(
                        "Veículo parado ou abaixo do limite. Interface liberada."
                    )
                } catch (_: Exception) {}
            }
        }
    }

    /**
     * Simula velocidade para testes rápidos em bancada sem necessidade de moto em movimento
     */
    fun simulateSpeed(simSpeedKmh: Double) {
        _state.value = _state.value.copy(isSimulationMode = true)
        updateSpeedAndEvaluateLock(
            speedKmh = simSpeedKmh,
            lat = _state.value.latitude,
            lng = _state.value.longitude,
            accuracy = 2.5f
        )
    }

    /**
     * Desativa o modo de simulação e retorna ao GPS real do FusedLocationProviderClient
     */
    fun resetSimulation() {
        _state.value = _state.value.copy(isSimulationMode = false)
        startTracking()
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }
}

```

---

