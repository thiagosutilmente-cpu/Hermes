package com.example

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import kotlin.math.roundToInt

/**
 * DynamicGeofencingEngine
 *
 * Motor de Geofencing Dinâmico Adaptativo que recalcula o raio de busca e detecção
 * de pedidos em tempo real baseado em 3 variáveis fundamentais:
 *
 * 1. Velocidade atual do entregador (GPS):
 *    - Parado / Semáforo (0-5 km/h): Raio compacto (450m - 600m) focado no polo imediato.
 *    - Trânsito urbano (15-35 km/h): Raio padrão (1.000m - 1.500m).
 *    - Vias expressas / Corredores (45+ km/h): Raio expandido (2.500m - 3.200m) para
 *      capturar o pedido antecipadamente antes de chegar na saída da via.
 *
 * 2. Densidade de restaurantes na região:
 *    - Polos densos (Shopping/Food Courts): Encolhe o raio para forçar coletas no mesmo local.
 *    - Zonas dispersas / Subúrbios: Expande o raio para cobrir maior área.
 *
 * 3. Probabilidade de Mesclagem:
 *    - Prioriza e atrai o centroide da geofence para corredores com alta chance de pedidos duplos.
 */
object DynamicGeofencingEngine {

    private const val TAG = "DynamicGeofencing"

    enum class RestaurantDensityProfile(
        val label: String,
        val spotsPerKm2: Int,
        val baseRadiusMeters: Int,
        val mergeLikelihood: String
    ) {
        HYPER_DENSE_MALL("Polo Gastronômico / Shopping", 45, 500, "92% (Excelente)"),
        HIGH_DENSITY_URBAN("Centro Urbano / Corredor", 28, 1100, "78% (Alta)"),
        MODERATE_SUBURB("Bairro Misto / Residencial", 12, 1800, "45% (Média)"),
        SPARSE_HIGHWAY("Via Expressa / Rodovia", 4, 2800, "20% (Baixa)")
    }

    data class DynamicGeofenceState(
        val currentSpeedKmh: Double = 0.0,
        val densityProfile: RestaurantDensityProfile = RestaurantDensityProfile.HYPER_DENSE_MALL,
        val computedRadiusMeters: Int = 500,
        val mergeProbabilityPercent: Int = 92,
        val isNearKnownHub: Boolean = true,
        val hubName: String = "Shopping Paulista / Cerqueira César",
        val tacticalExplanation: String = "Moto parada em polo de alta densidade: raio reduzido para 500m para focar em pedidos do mesmo shopping."
    )

    private val _geofenceState = MutableStateFlow(DynamicGeofenceState())
    val geofenceState: StateFlow<DynamicGeofenceState> = _geofenceState.asStateFlow()

    /**
     * Recalcula o raio dinâmico com base na velocidade e perfil de densidade.
     */
    fun recalculate(
        speedKmh: Double,
        density: RestaurantDensityProfile = _geofenceState.value.densityProfile,
        hubName: String = _geofenceState.value.hubName
    ) {
        val isNearHub = density == RestaurantDensityProfile.HYPER_DENSE_MALL || hubName.contains("Shopping", ignoreCase = true)

        // 1. Fator de Velocidade
        val speedFactor: Double = when {
            speedKmh <= 6.0 -> 0.70    // Parado no semáforo ou aguardando
            speedKmh <= 25.0 -> 1.00   // Trânsito urbano normal
            speedKmh <= 45.0 -> 1.45   // Velocidade média em avenidas
            else -> 1.90               // Via expressa / Rodovia (23 de Maio, Marginal)
        }

        // 2. Fator de Densidade
        val densityBaseRadius = density.baseRadiusMeters

        // 3. Cálculo do raio dinâmico final
        var finalRadius = (densityBaseRadius * speedFactor).roundToInt()

        // Se estiver em shopping denso e parado, força raio hiper-focado
        if (isNearHub && speedKmh <= 6.0) {
            finalRadius = 450
        } else {
            finalRadius = finalRadius.coerceIn(400, 3500)
        }

        // 4. Probabilidade de Mesclagem
        val mergeProb = calculateMergeProbability(speedKmh, density, isNearHub)

        // 5. Explicação Tática para o Motoboy
        val explanation = when {
            isNearHub && speedKmh <= 6.0 ->
                "Moto parada em polo denso: raio calibrado para 450m focado exclusivamente em pedidos duplos do mesmo shopping."
            speedKmh > 40.0 ->
                "Alta velocidade detectada (${speedKmh.toInt()} km/h): raio expandido para ${finalRadius}m para antecipar coletas antes da saída da via."
            else ->
                "Velocidade urbana (${speedKmh.toInt()} km/h) em ${density.label}: raio otimizado para ${finalRadius}m com ${mergeProb}% de probabilidade de mesclagem."
        }

        _geofenceState.value = DynamicGeofenceState(
            currentSpeedKmh = (speedKmh * 10).roundToInt() / 10.0,
            densityProfile = density,
            computedRadiusMeters = finalRadius,
            mergeProbabilityPercent = mergeProb,
            isNearKnownHub = isNearHub,
            hubName = hubName,
            tacticalExplanation = explanation
        )

        Log.d(TAG, "Dynamic Geofence Recalculada: Raio=${finalRadius}m | Vel=${speedKmh}km/h | Densidade=${density.name} | Prob=${mergeProb}%")
    }

    private fun calculateMergeProbability(
        speedKmh: Double,
        density: RestaurantDensityProfile,
        isNearHub: Boolean
    ): Int {
        var score = when (density) {
            RestaurantDensityProfile.HYPER_DENSE_MALL -> 80
            RestaurantDensityProfile.HIGH_DENSITY_URBAN -> 68
            RestaurantDensityProfile.MODERATE_SUBURB -> 40
            RestaurantDensityProfile.SPARSE_HIGHWAY -> 15
        }

        if (isNearHub) score += 12
        if (speedKmh <= 10.0) score += 8 else if (speedKmh > 50.0) score -= 15

        return score.coerceIn(10, 98)
    }

    /**
     * Avalia se uma oferta recebida está dentro do perímetro geofence dinâmico ativo.
     */
    fun isOfferWithinDynamicPerimeter(offerDistanceMeters: Double): Boolean {
        return offerDistanceMeters <= _geofenceState.value.computedRadiusMeters
    }
}
