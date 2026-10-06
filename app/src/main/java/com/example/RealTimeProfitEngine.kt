package com.example

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Níveis de Rentabilidade da Corrida com Thresholds Codificados por Cores
 */
enum class ProfitThresholdLevel(
    val title: String,
    val badgeLabel: String,
    val colorHex: String,
    val bgHex: String,
    val recommendation: String
) {
    HIGH_PROFIT(
        title = "ALTA RENTABILIDADE 🟢",
        badgeLabel = "LUCRO ALTO",
        colorHex = "#00FF88",
        bgHex = "#122A1E",
        recommendation = "Excelente corrida! Aceite imediatamente."
    ),
    MEDIUM_PROFIT(
        title = "RENTABILIDADE MÉDIA 🟡",
        badgeLabel = "LUCRO MODERADO",
        colorHex = "#FFB800",
        bgHex = "#2A2210",
        recommendation = "Corrida aceitável. Paga custos e gera margem moderada."
    ),
    LOW_PROFIT(
        title = "ALERTA DE PREJUÍZO 🔴",
        badgeLabel = "BAIXO LUCRO / RECUSAR",
        colorHex = "#FF3366",
        bgHex = "#2E1418",
        recommendation = "Prejuízo detectado! Não queime combustível à toa."
    )
}

/**
 * Resultado completo do cálculo em tempo real de uma corrida
 */
data class RealTimeProfitMetrics(
    val fareValue: Double,
    val distanceKm: Double,
    val fuelPricePerLiter: Double,
    val kmPerLiter: Double,
    val maintenancePerKm: Double,
    
    // Resultados
    val grossPricePerKm: Double,
    val fuelCostTotal: Double,
    val fuelCostPerKm: Double,
    val maintenanceCostTotal: Double,
    val totalTripCost: Double,
    val netProfitTotal: Double,
    val netProfitPerKm: Double,
    val profitMarginPercent: Double,
    
    // Nível e representação visual
    val thresholdLevel: ProfitThresholdLevel,
    
    // Textos formatados prontos para exibição no HUD
    val formattedGrossPerKm: String,
    val formattedNetPerKm: String,
    val formattedNetProfit: String,
    val formattedFuelCost: String,
    val formattedMargin: String,
    val thresholdLabel: String
)

/**
 * Motor de Cálculo em Tempo Real de Rentabilidade e Combustível (RealTimeProfitEngine)
 * Processa distância da viagem, valor da tarifa e custo de combustível para gerar
 * métricas de "Lucro por KM" com thresholds visuais coloridos no HUD do Jarvis.
 */
object RealTimeProfitEngine {

    private const val PREFS_NAME = "jarvis_profit_engine_prefs"
    private const val KEY_FUEL_PRICE = "fuel_price_per_liter"
    private const val KEY_KM_PER_LITER = "km_per_liter"
    private const val KEY_MAINTENANCE_PER_KM = "maintenance_per_km"
    private const val KEY_MIN_ACCEPTABLE_PROFIT = "min_acceptable_profit_per_km"
    private const val KEY_HIGH_PROFIT_THRESHOLD = "high_profit_threshold_per_km"

    // Valores padrão calibrados para motos de entrega no Brasil (ex: Honda CG 160 / Fan / Factor)
    const val DEFAULT_FUEL_PRICE = 5.89 // R$ por litro
    const val DEFAULT_KM_PER_LITER = 35.0 // km por litro em ciclo urbano
    const val DEFAULT_MAINTENANCE_PER_KM = 0.12 // R$ 0,12/km (óleo, pneus, freios, relação)
    const val DEFAULT_MIN_ACCEPTABLE_NET = 3.50 // R$/km líquido mínimo
    const val DEFAULT_HIGH_PROFIT_NET = 5.00 // R$/km líquido para alta rentabilidade

    private lateinit var prefs: SharedPreferences

    private val _currentMetrics = MutableStateFlow<RealTimeProfitMetrics?>(null)
    val currentMetrics: StateFlow<RealTimeProfitMetrics?> = _currentMetrics.asStateFlow()

    fun initialize(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getFuelPrice(): Double = if (::prefs.isInitialized) prefs.getFloat(KEY_FUEL_PRICE, DEFAULT_FUEL_PRICE.toFloat()).toDouble() else DEFAULT_FUEL_PRICE
    fun getKmPerLiter(): Double = if (::prefs.isInitialized) prefs.getFloat(KEY_KM_PER_LITER, DEFAULT_KM_PER_LITER.toFloat()).toDouble() else DEFAULT_KM_PER_LITER
    fun getMaintenancePerKm(): Double = if (::prefs.isInitialized) prefs.getFloat(KEY_MAINTENANCE_PER_KM, DEFAULT_MAINTENANCE_PER_KM.toFloat()).toDouble() else DEFAULT_MAINTENANCE_PER_KM
    fun getMinAcceptableNet(): Double = if (::prefs.isInitialized) prefs.getFloat(KEY_MIN_ACCEPTABLE_PROFIT, DEFAULT_MIN_ACCEPTABLE_NET.toFloat()).toDouble() else DEFAULT_MIN_ACCEPTABLE_NET
    fun getHighProfitThreshold(): Double = if (::prefs.isInitialized) prefs.getFloat(KEY_HIGH_PROFIT_THRESHOLD, DEFAULT_HIGH_PROFIT_NET.toFloat()).toDouble() else DEFAULT_HIGH_PROFIT_NET

    fun saveConfig(
        fuelPrice: Double,
        kmPerLiter: Double,
        maintenancePerKm: Double = DEFAULT_MAINTENANCE_PER_KM,
        minNetProfit: Double = DEFAULT_MIN_ACCEPTABLE_NET,
        highNetProfit: Double = DEFAULT_HIGH_PROFIT_NET
    ) {
        if (!::prefs.isInitialized) return
        prefs.edit()
            .putFloat(KEY_FUEL_PRICE, fuelPrice.toFloat())
            .putFloat(KEY_KM_PER_LITER, kmPerLiter.toFloat())
            .putFloat(KEY_MAINTENANCE_PER_KM, maintenancePerKm.toFloat())
            .putFloat(KEY_MIN_ACCEPTABLE_PROFIT, minNetProfit.toFloat())
            .putFloat(KEY_HIGH_PROFIT_THRESHOLD, highNetProfit.toFloat())
            .apply()
    }

    /**
     * Processa os dados de uma corrida em tempo real e gera as métricas completas de rentabilidade
     */
    fun calculateTripMetrics(
        fareValue: Double,
        distanceKm: Double,
        customFuelPrice: Double? = null,
        customKmPerLiter: Double? = null
    ): RealTimeProfitMetrics {
        val safeDistance = if (distanceKm > 0.05) distanceKm else 1.0
        val safeFare = if (fareValue > 0.0) fareValue else 0.0

        val fuelPrice = customFuelPrice ?: getFuelPrice()
        val kmPerLiter = (customKmPerLiter ?: getKmPerLiter()).coerceAtLeast(5.0)
        val maintenancePerKm = getMaintenancePerKm()
        val minAcceptableNet = getMinAcceptableNet()
        val highProfitThreshold = getHighProfitThreshold()

        // 1. Ganho Bruto por KM
        val grossPricePerKm = safeFare / safeDistance

        // 2. Custo do Combustível
        val fuelLitersConsumed = safeDistance / kmPerLiter
        val fuelCostTotal = fuelLitersConsumed * fuelPrice
        val fuelCostPerKm = fuelPrice / kmPerLiter

        // 3. Custo de Manutenção / Depreciação
        val maintenanceCostTotal = safeDistance * maintenancePerKm

        // 4. Custo Total da Corrida
        val totalTripCost = fuelCostTotal + maintenanceCostTotal

        // 5. Lucro Líquido Real no Bolso
        val netProfitTotal = (safeFare - totalTripCost).coerceAtLeast(-50.0)
        val netProfitPerKm = netProfitTotal / safeDistance

        // 6. Margem Líquida Percentual
        val profitMarginPercent = if (safeFare > 0.0) {
            ((netProfitTotal / safeFare) * 100.0).coerceIn(-100.0, 100.0)
        } else {
            0.0
        }

        // 7. Determinação do Threshold Colorido
        val thresholdLevel = when {
            netProfitPerKm >= highProfitThreshold || grossPricePerKm >= 5.50 -> ProfitThresholdLevel.HIGH_PROFIT
            netProfitPerKm >= minAcceptableNet || grossPricePerKm >= 4.00 -> ProfitThresholdLevel.MEDIUM_PROFIT
            else -> ProfitThresholdLevel.LOW_PROFIT
        }

        val result = RealTimeProfitMetrics(
            fareValue = safeFare,
            distanceKm = safeDistance,
            fuelPricePerLiter = fuelPrice,
            kmPerLiter = kmPerLiter,
            maintenancePerKm = maintenancePerKm,
            grossPricePerKm = grossPricePerKm,
            fuelCostTotal = fuelCostTotal,
            fuelCostPerKm = fuelCostPerKm,
            maintenanceCostTotal = maintenanceCostTotal,
            totalTripCost = totalTripCost,
            netProfitTotal = netProfitTotal,
            netProfitPerKm = netProfitPerKm,
            profitMarginPercent = profitMarginPercent,
            thresholdLevel = thresholdLevel,
            formattedGrossPerKm = String.format(Locale.GERMANY, "R$ %.2f/km", grossPricePerKm),
            formattedNetPerKm = String.format(Locale.GERMANY, "R$ %.2f/km líq.", netProfitPerKm),
            formattedNetProfit = String.format(Locale.GERMANY, "R$ %.2f", netProfitTotal),
            formattedFuelCost = String.format(Locale.GERMANY, "- R$ %.2f", fuelCostTotal),
            formattedMargin = String.format(Locale.GERMANY, "%.1f%%", profitMarginPercent),
            thresholdLabel = when (thresholdLevel) {
                ProfitThresholdLevel.HIGH_PROFIT -> String.format(Locale.GERMANY, "R$ %.2f/km 🟢 BOA", grossPricePerKm)
                ProfitThresholdLevel.MEDIUM_PROFIT -> String.format(Locale.GERMANY, "R$ %.2f/km 🟡 MÉDIA", grossPricePerKm)
                ProfitThresholdLevel.LOW_PROFIT -> String.format(Locale.GERMANY, "R$ %.2f/km 🔴 PREJUÍZO", grossPricePerKm)
            }
        )

        _currentMetrics.value = result
        return result
    }
}
