package com.example

import com.google.gson.annotations.SerializedName

/**
 * RideAnalysisData
 *
 * Modelo centralizado para encapsulamento das métricas financeiras, operacionais
 * e pontuações táticas de uma corrida analisada pelo motor de decisão do Jarvis.
 * Contém a blindagem de ofuscação com preservação de anotações @SerializedName.
 */
data class RideAnalysisData(
    @SerializedName("platform") val platform: String = "Desconhecido",
    @SerializedName("payout_brl") val payoutBrl: Double = 0.0,
    @SerializedName("total_distance_km") val totalDistanceKm: Double = 0.0,
    @SerializedName("rate_per_km") val ratePerKm: Double = 0.0,
    @SerializedName("estimated_time_minutes") val estimatedTimeMinutes: Int = 0,
    @SerializedName("estimated_hourly_rate_brl") val estimatedHourlyRateBrl: Double = 0.0,
    @SerializedName("pickup_distance_km") val pickupDistanceKm: Double = 0.0,
    @SerializedName("delivery_distance_km") val deliveryDistanceKm: Double = 0.0,
    @SerializedName("decision_action") val decisionAction: String = "REJECT", // "ACCEPT" | "REJECT" | "BATCH"
    @SerializedName("decision_score") val decisionScore: Int = 0,
    @SerializedName("decision_reason") val decisionReason: String = "",
    @SerializedName("voice_alert") val voiceAlert: String = "",
    @SerializedName("is_batch_route") val isBatchRoute: Boolean = false,
    @SerializedName("fuel_cost_estimate_brl") val fuelCostEstimateBrl: Double = 0.0,
    @SerializedName("net_profit_brl") val netProfitBrl: Double = 0.0,
    @SerializedName("timestamp") val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Lógica proprietária de processamento financeiro em tempo real:
     * Calcula o lucro líquido descontando consumo de combustível e depreciação da moto.
     */
    fun calculateNetProfit(fuelCostPerKm: Double = 0.35, wearPerKm: Double = 0.15): Double {
        val operationalCost = totalDistanceKm * (fuelCostPerKm + wearPerKm)
        return Math.round((payoutBrl - operationalCost) * 100.0) / 100.0
    }

    /**
     * Calcula o ganho real líquido por quilômetro rodado.
     */
    fun calculateNetRatePerKm(fuelCostPerKm: Double = 0.35): Double {
        if (totalDistanceKm <= 0.0) return 0.0
        val grossRate = payoutBrl / totalDistanceKm
        return Math.round((grossRate - fuelCostPerKm) * 100.0) / 100.0
    }

    /**
     * Avalia a viabilidade financeira da corrida baseada na margem mínima de corte.
     */
    fun isFinanciallyViable(minRatePerKm: Double = 1.80, minHourlyRate: Double = 30.00): Boolean {
        return ratePerKm >= minRatePerKm && estimatedHourlyRateBrl >= minHourlyRate
    }

    companion object {
        /**
         * Processa os dados brutos financeiros e gera a instância calibrada do RideAnalysisData.
         */
        fun processFinancials(
            platform: String,
            payout: Double,
            distanceKm: Double,
            pickupDistanceKm: Double = 0.0,
            isBatch: Boolean = false
        ): RideAnalysisData {
            val safeDistance = distanceKm.coerceAtLeast(0.1)
            val rate = Math.round((payout / safeDistance) * 100.0) / 100.0
            val estMinutes = Math.round((safeDistance / 25.0 * 60.0) + 8.0).toInt().coerceAtLeast(8)
            val estHourly = Math.round((payout / estMinutes * 60.0) * 100.0) / 100.0
            val fuelCost = Math.round((safeDistance * 0.35) * 100.0) / 100.0
            val netProfit = Math.round((payout - fuelCost - (safeDistance * 0.15)) * 100.0) / 100.0

            val action: String
            val score: Int
            val reason: String

            if (isBatch) {
                action = "BATCH"
                score = 88
                reason = "Rota dupla compatível com ganho maximizado."
            } else if (rate >= 1.80 && estHourly >= 30.00) {
                action = "ACCEPT"
                score = (80 + ((rate - 1.80) * 10.0).toInt()).coerceIn(80, 99)
                reason = "Excelente taxa de R$ %.2f/km e R$ %.2f/h.".format(java.util.Locale.US, rate, estHourly)
            } else {
                action = "REJECT"
                score = (rate * 15.0).toInt().coerceIn(5, 35)
                reason = "Margem abaixo do limite operacional sustentável."
            }

            return RideAnalysisData(
                platform = platform,
                payoutBrl = payout,
                totalDistanceKm = distanceKm,
                ratePerKm = rate,
                estimatedTimeMinutes = estMinutes,
                estimatedHourlyRateBrl = estHourly,
                pickupDistanceKm = pickupDistanceKm,
                deliveryDistanceKm = (distanceKm - pickupDistanceKm).coerceAtLeast(0.0),
                decisionAction = action,
                decisionScore = score,
                decisionReason = reason,
                voiceAlert = "R$ %.2f por %.1f km. %s".format(java.util.Locale.US, payout, distanceKm, action),
                isBatchRoute = isBatch,
                fuelCostEstimateBrl = fuelCost,
                netProfitBrl = netProfit
            )
        }

        fun fromJarvisRide(ride: JarvisNeuralCockpitEngine.JarvisRideData): RideAnalysisData {
            val safeDistance = ride.financials.totalDistanceKm.coerceAtLeast(0.1)
            val fuelCost = Math.round((safeDistance * 0.35) * 100.0) / 100.0
            val netProfit = Math.round((ride.financials.payoutBrl - fuelCost - (safeDistance * 0.15)) * 100.0) / 100.0
            return RideAnalysisData(
                platform = ride.platform,
                payoutBrl = ride.financials.payoutBrl,
                totalDistanceKm = ride.financials.totalDistanceKm,
                ratePerKm = ride.financials.ratePerKm,
                estimatedTimeMinutes = ride.financials.estimatedTimeMinutes,
                estimatedHourlyRateBrl = ride.financials.estimatedHourlyRateBrl,
                decisionAction = ride.decision.action,
                decisionScore = ride.decision.score,
                decisionReason = ride.decision.reason,
                voiceAlert = ride.voiceAlert,
                isBatchRoute = ride.decision.action == "BATCH",
                fuelCostEstimateBrl = fuelCost,
                netProfitBrl = netProfit
            )
        }
    }
}
