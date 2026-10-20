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
