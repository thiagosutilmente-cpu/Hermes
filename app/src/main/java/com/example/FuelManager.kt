package com.example

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Modo Climático / Condição da Pista e Tráfego
 */
enum class WeatherMode(val label: String, val description: String) {
    DRY(
        label = "Modo Seco",
        description = "Pista seca, tráfego urbano fluido/padrão e arrasto aerodinâmico nominal."
    ),
    RAIN(
        label = "Modo Chuva",
        description = "Pista molhada, tráfego pesado de chuva, frenagens frequentes e arrasto aerodinâmico elevado por capa de chuva."
    )
}

/**
 * Especificação técnica da motocicleta e parâmetros de consumo no ciclo urbano.
 */
data class MotorcycleModelSpec(
    val id: String,
    val brand: String,
    val modelName: String,
    val engineDisplacementCc: Int,
    val baseUrbanConsumptionKmPerL: Double, // km/l nominal em ciclo urbano padrão
    val aeroDragCoefficient: Double = 1.0,  // Fator de arrasto aerodinâmico relativo da moto
    val rainTrafficPenaltyPercent: Double = 18.5 // % de perda de rendimento na chuva (tráfego + aerodinâmica)
) {
    val displayName: String
        get() = "$brand $modelName (${engineDisplacementCc}cc)"
}

/**
 * Classe [FuelManager]
 *
 * Responsável por receber o modelo da moto do entregador, gerenciar o consumo médio
 * baseado no ciclo urbano das vias brasileiras (stop-and-go de semáforos, corredor, arrancadas)
 * e alternar dinamicamente entre 'Modo Seco' e 'Modo Chuva'.
 *
 * Efeitos físicos e de tráfego calculados no Modo Chuva:
 * 1. Resistência Aerodinâmica Adicional:
 *    O uso obrigatório de capa de chuva e conjunto impermeável aumenta a área frontal efetiva (CdA)
 *    do motociclista em ~10% a 15%, aumentando a resistência do ar mesmo em velocidades urbanas.
 * 2. Condições de Tráfego e Frenagem:
 *    Em dias chuvosos, a velocidade média do tráfego cai, o tempo de motor ligado em marcha lenta (idle)
 *    aumenta, e o acúmulo de lâmina d'água no asfalto gera maior resistência ao rolamento dos pneus,
 *    resultando em uma redução calculada de 15% a 22% no rendimento quilômetro/litro.
 */
class FuelManager(
    initialModel: MotorcycleModelSpec = DEFAULT_MOTORCYCLE,
    initialMode: WeatherMode = WeatherMode.DRY,
    initialFuelPrice: Double = DEFAULT_FUEL_PRICE_BRL
) {

    companion object {
        private const val TAG = "FuelManager"

        const val DEFAULT_FUEL_PRICE_BRL = 5.89

        // Catálogo de Motos Mais Utilizadas em Entregas no Brasil
        val HONDA_CG_160 = MotorcycleModelSpec(
            id = "cg160",
            brand = "Honda",
            modelName = "CG 160 Fan / Titan / Cargo",
            engineDisplacementCc = 162,
            baseUrbanConsumptionKmPerL = 41.0,
            aeroDragCoefficient = 1.0,
            rainTrafficPenaltyPercent = 18.0
        )

        val HONDA_BIZ_125 = MotorcycleModelSpec(
            id = "biz125",
            brand = "Honda",
            modelName = "Biz 125",
            engineDisplacementCc = 124,
            baseUrbanConsumptionKmPerL = 45.0,
            aeroDragCoefficient = 0.95,
            rainTrafficPenaltyPercent = 17.0
        )

        val YAMAHA_FACTOR_150 = MotorcycleModelSpec(
            id = "factor150",
            brand = "Yamaha",
            modelName = "Factor 150 ED / UBS",
            engineDisplacementCc = 149,
            baseUrbanConsumptionKmPerL = 42.0,
            aeroDragCoefficient = 1.0,
            rainTrafficPenaltyPercent = 18.0
        )

        val HONDA_POP_110 = MotorcycleModelSpec(
            id = "pop110",
            brand = "Honda",
            modelName = "Pop 110i",
            engineDisplacementCc = 109,
            baseUrbanConsumptionKmPerL = 55.0,
            aeroDragCoefficient = 0.90,
            rainTrafficPenaltyPercent = 16.0
        )

        val SCOOTER_PCX_NMAX = MotorcycleModelSpec(
            id = "scooter",
            brand = "Honda / Yamaha",
            modelName = "PCX 160 / NMax 160",
            engineDisplacementCc = 156,
            baseUrbanConsumptionKmPerL = 38.0,
            aeroDragCoefficient = 1.10, // Escudo frontal e transmissão CVT têm consumo urbano característico
            rainTrafficPenaltyPercent = 20.0
        )

        val YAMAHA_FAZER_250 = MotorcycleModelSpec(
            id = "fazer250",
            brand = "Yamaha",
            modelName = "Fazer FZ25",
            engineDisplacementCc = 249,
            baseUrbanConsumptionKmPerL = 31.0,
            aeroDragCoefficient = 1.05,
            rainTrafficPenaltyPercent = 19.0
        )

        val HONDA_TWISTER_300 = MotorcycleModelSpec(
            id = "twister300",
            brand = "Honda",
            modelName = "CB 300F Twister",
            engineDisplacementCc = 293,
            baseUrbanConsumptionKmPerL = 30.0,
            aeroDragCoefficient = 1.05,
            rainTrafficPenaltyPercent = 19.0
        )

        val KNOWN_MODELS = listOf(
            HONDA_CG_160,
            HONDA_BIZ_125,
            YAMAHA_FACTOR_150,
            HONDA_POP_110,
            SCOOTER_PCX_NMAX,
            YAMAHA_FAZER_250,
            HONDA_TWISTER_300
        )

        val DEFAULT_MOTORCYCLE = HONDA_CG_160

        /**
         * Localiza uma especificação pelo nome ou identificador
         */
        fun findModelByNameOrId(query: String): MotorcycleModelSpec {
            val clean = query.trim().lowercase(Locale.ROOT)
            return KNOWN_MODELS.firstOrNull { spec ->
                spec.id.equals(clean, ignoreCase = true) ||
                        spec.modelName.lowercase(Locale.ROOT).contains(clean) ||
                        clean.contains(spec.id) ||
                        clean.contains(spec.modelName.lowercase(Locale.ROOT))
            } ?: MotorcycleModelSpec(
                id = "custom",
                brand = "Modelo",
                modelName = query.ifBlank { "Personalizado" },
                engineDisplacementCc = 150,
                baseUrbanConsumptionKmPerL = 38.0,
                aeroDragCoefficient = 1.0,
                rainTrafficPenaltyPercent = 18.0
            )
        }

        /**
         * Instância Singleton compartilhada no ciclo de vida do aplicativo
         */
        @Volatile
        private var INSTANCE: FuelManager? = null

        fun getInstance(): FuelManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FuelManager().also { INSTANCE = it }
            }
        }
    }

    // Estado observável do modelo da moto
    private val _motorcycleModel = MutableStateFlow(initialModel)
    val motorcycleModel: StateFlow<MotorcycleModelSpec> = _motorcycleModel.asStateFlow()

    // Estado observável do modo climático (Seco vs Chuva)
    private val _weatherMode = MutableStateFlow(initialMode)
    val weatherMode: StateFlow<WeatherMode> = _weatherMode.asStateFlow()

    // Preço do combustível por litro em R$
    private val _fuelPricePerLiter = MutableStateFlow(initialFuelPrice)
    val fuelPricePerLiter: StateFlow<Double> = _fuelPricePerLiter.asStateFlow()

    /**
     * Construtor de conveniência que recebe diretamente a String com o modelo da moto.
     */
    constructor(modelName: String, isRainMode: Boolean = false, fuelPrice: Double = DEFAULT_FUEL_PRICE_BRL) : this(
        initialModel = findModelByNameOrId(modelName),
        initialMode = if (isRainMode) WeatherMode.RAIN else WeatherMode.DRY,
        initialFuelPrice = fuelPrice
    )

    // =========================================================================
    // CONFIGURAÇÃO DO MODELO DA MOTO
    // =========================================================================

    /**
     * Atualiza o modelo da moto do entregador usando especificação técnica.
     */
    fun setMotorcycleModel(model: MotorcycleModelSpec) {
        _motorcycleModel.value = model
        Log.i(TAG, "Modelo atualizado para: ${model.displayName} (Urbano nominal: ${model.baseUrbanConsumptionKmPerL} km/l)")
    }

    /**
     * Atualiza o modelo da moto a partir de uma String (ex: "CG 160", "Biz 125", "Factor").
     */
    fun setMotorcycleModel(modelName: String) {
        val found = findModelByNameOrId(modelName)
        setMotorcycleModel(found)
    }

    /**
     * Atualiza o preço do litro de combustível em R$.
     */
    fun setFuelPricePerLiter(price: Double) {
        _fuelPricePerLiter.value = price.coerceAtLeast(1.0)
    }

    // =========================================================================
    // ALTERNÂNCIA ENTRE MODO SECO E MODO CHUVA
    // =========================================================================

    /**
     * Define explicitamente o modo climático.
     */
    fun setWeatherMode(mode: WeatherMode) {
        _weatherMode.value = mode
        Log.i(TAG, "Modo climático alterado para: ${mode.label} (${mode.description})")
    }

    /**
     * Ativa ou desativa o Modo Chuva.
     */
    fun setRainMode(isRain: Boolean) {
        setWeatherMode(if (isRain) WeatherMode.RAIN else WeatherMode.DRY)
    }

    /**
     * Alterna rapidamente entre Modo Seco e Modo Chuva (toggle).
     * @return O novo [WeatherMode] ativado.
     */
    fun toggleWeatherMode(): WeatherMode {
        val newMode = if (_weatherMode.value == WeatherMode.DRY) WeatherMode.RAIN else WeatherMode.DRY
        setWeatherMode(newMode)
        return newMode
    }

    /**
     * Retorna se o Modo Chuva está ativo atualmente.
     */
    val isRainModeActive: Boolean
        get() = _weatherMode.value == WeatherMode.RAIN

    // =========================================================================
    // CÁLCULOS MATEMÁTICOS DE CONSUMO E RESISTÊNCIA
    // =========================================================================

    /**
     * Calcula o consumo médio urbano efetivo em km/l.
     *
     * No 'Modo Seco':
     * - Retorna a média urbana nominal calibrada para o modelo da moto.
     *
     * No 'Modo Chuva':
     * - Aplica a perda de eficiência decorrente da maior resistência aerodinâmica
     *   (capa de chuva/arrasto ampliado) somada ao tráfego lento com retenções e
     *   maior atrito por lâmina d'água nas pistas urbanas.
     */
    fun calculateUrbanConsumptionKmPerLiter(): Double {
        val model = _motorcycleModel.value
        val baseConsumption = model.baseUrbanConsumptionKmPerL

        return if (_weatherMode.value == WeatherMode.RAIN) {
            // Fator de perda combinado: Arrasto aerodinâmico extra (CdA) + tráfego/rolamento
            val penaltyRatio = (model.rainTrafficPenaltyPercent / 100.0) * model.aeroDragCoefficient
            val adjusted = baseConsumption * (1.0 - penaltyRatio.coerceIn(0.10, 0.35))
            Math.round(adjusted * 10.0) / 10.0
        } else {
            baseConsumption
        }
    }

    /**
     * Calcula a quantidade de litros consumida para percorrer determinada distância em km.
     */
    fun calculateConsumptionLiters(distanceKm: Double): Double {
        if (distanceKm <= 0.0) return 0.0
        val kmPerLiter = calculateUrbanConsumptionKmPerLiter()
        return if (kmPerLiter > 0.0) distanceKm / kmPerLiter else 0.0
    }

    /**
     * Calcula o custo financeiro total em R$ do combustível para uma distância específica.
     */
    fun calculateFuelCost(distanceKm: Double, pricePerLiter: Double = _fuelPricePerLiter.value): Double {
        val liters = calculateConsumptionLiters(distanceKm)
        val cost = liters * pricePerLiter
        return Math.round(cost * 100.0) / 100.0
    }

    /**
     * Calcula o custo por quilômetro rodado (R$/km) apenas com combustível.
     */
    fun calculateCostPerKm(pricePerLiter: Double = _fuelPricePerLiter.value): Double {
        val kmPerLiter = calculateUrbanConsumptionKmPerLiter()
        return if (kmPerLiter > 0.0) {
            Math.round((pricePerLiter / kmPerLiter) * 100.0) / 100.0
        } else 0.0
    }

    /**
     * Calcula o lucro líquido de uma corrida subtraindo o custo de combustível.
     */
    fun calculateNetProfit(grossEarnings: Double, totalDistanceKm: Double): Double {
        val cost = calculateFuelCost(totalDistanceKm)
        val net = grossEarnings - cost
        return Math.round(net * 100.0) / 100.0
    }

    /**
     * Gera um relatório estruturado em texto da estimativa de consumo atual.
     */
    fun getTelemetrySummary(): String {
        val model = _motorcycleModel.value
        val kmL = calculateUrbanConsumptionKmPerLiter()
        val costKm = calculateCostPerKm()
        val mode = _weatherMode.value

        return """
            🏍️ Modelo: ${model.displayName}
            🌦️ Condição: ${mode.label}
            ⛽ Consumo Urbano Efetivo: $kmL km/l
            💰 Custo Médio por KM: R$ ${String.format(Locale.GERMANY, "%.2f", costKm)}/km
            📊 Impacto Climático: ${if (isRainModeActive) "-${model.rainTrafficPenaltyPercent}% (Arrasto + Tráfego)" else "Nominal"}
        """.trimIndent()
    }

    /**
     * Converte o estado atual do FuelManager para o objeto [FuelConfig]
     * utilizado nas interfaces visuais existentes do app.
     */
    fun toFuelConfig(): FuelConfig {
        return FuelConfig(
            kmPerLiter = calculateUrbanConsumptionKmPerLiter(),
            fuelPricePerLiter = _fuelPricePerLiter.value,
            selectedModelId = _motorcycleModel.value.id,
            isRainModeActive = isRainModeActive,
            rainSurgePercentage = 35.0
        )
    }
}
