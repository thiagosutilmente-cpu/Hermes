package com.example.util

import com.example.data.OfferEntity
import kotlin.math.max

/**
 * Calculadora Tática de Lucro Líquido Real e Custo Operacional por KM Rodado.
 * Desenvolvida para a realidade dos entregadores brasileiros de moto e bike elétrica.
 */

data class MotorcycleExpenseConfig(
    val fuelPricePerLiter: Double = 5.89,       // Gasolina Média Brasil (R$/L)
    val fuelAutonomyKmPerLiter: Double = 35.0,  // Autonomia Média da Moto (150cc/160cc - ex: Fan/Titan/FZ15)
    val maintenanceCostPerKm: Double = 0.12,    // Troca de Óleo (1000km), Pneu, Relação, Transmissão, Pastilhas
    val meiInssMonthlyReserve: Double = 75.0,   // Reserva mensal MEI Caminhoneiro/Entregador
    val dailyTargetHours: Double = 8.0          // Meta de Horas Trabalhadas por Dia
)

data class NetProfitCalculation(
    val grossFare: Double,                      // Faturamento Bruto (R$)
    val totalDistanceKm: Double,                // Distância Total (Coleta + Entrega + Retorno Estimado em km)
    val fuelCost: Double,                       // Custo do Combustível (R$)
    val maintenanceCost: Double,                // Custo de Desgaste da Moto (R$)
    val totalOperationalCost: Double,           // Custo Operacional Total (R$)
    val netProfit: Double,                      // Lucro Líquido Real (R$)
    val grossPerKm: Double,                     // R$/KM Bruto
    val netPerKm: Double,                       // R$/KM Líquido Real
    val profitMarginPercent: Double,            // Margem de Lucro (%)
    val efficiencyTier: NetProfitTier,          // Classificação de Rentabilidade
    val voiceAlertText: String                  // Texto sintético formatado para o Jarvis sintetizar por Voz no Headset
)

enum class NetProfitTier(val label: String, val colorHex: Long) {
    EXCELENTE("EXCELENTE RENTABILIDADE", 0xFF00FF88),
    BOA("BOA RENTABILIDADE", 0xFF00E5FF),
    ALERTA_MARGEM_BAIXA("MARGEM BAIXA (PAGA APLINHAS)", 0xFFFF8800),
    PREJUIZO_OPERACIONAL("PREJUÍZO OPERACIONAL (PAGANDO PRA TRABALHAR)", 0xFFFF0055)
}

object CourierNetProfitEngine {

    /**
     * Calcula o Lucro Líquido Real descontando o combustível e a manutenção por km.
     */
    fun calculateNetProfit(
        offer: OfferEntity,
        config: MotorcycleExpenseConfig = MotorcycleExpenseConfig()
    ): NetProfitCalculation {
        val distKm = max(0.5, offer.totalDistance)
        val fare = offer.fareValue

        // Custo do Combustível: (Distância / Autonomia) * Preço do Litro
        val fuelCost = (distKm / config.fuelAutonomyKmPerLiter) * config.fuelPricePerLiter

        // Custo de Manutenção (R$ 0.12 por km)
        val maintCost = distKm * config.maintenanceCostPerKm

        // Custo Operacional Total
        val totalCost = fuelCost + maintCost

        // Lucro Líquido Real
        val netProfit = fare - totalCost

        val grossPerKm = if (distKm > 0) fare / distKm else 0.0
        val netPerKm = if (distKm > 0) netProfit / distKm else 0.0

        val margin = if (fare > 0) (netProfit / fare) * 100.0 else 0.0

        val tier = when {
            netPerKm >= 3.80 -> NetProfitTier.EXCELENTE
            netPerKm >= 2.60 -> NetProfitTier.BOA
            netPerKm >= 1.50 -> NetProfitTier.ALERTA_MARGEM_BAIXA
            else -> NetProfitTier.PREJUIZO_OPERACIONAL
        }

        // Sintetiza uma mensagem direta para áudio no capacete/fone Bluetooth
        val voiceAlert = when (tier) {
            NetProfitTier.EXCELENTE -> "Atenção piloto. Oferta ${offer.appName} excelente. Valor R$ ${String.format("%.2f", fare)}. Lucro líquido de R$ ${String.format("%.2f", netProfit)}, resultando em R$ ${String.format("%.1f", netPerKm)} por quilômetro limpo. Aceite recomendado!"
            NetProfitTier.BOA -> "Nova oferta ${offer.appName}. R$ ${String.format("%.2f", fare)} por ${String.format("%.1f", distKm)} km. Lucro líquido de R$ ${String.format("%.2f", netProfit)}. Dentro do padrão aceitável."
            NetProfitTier.ALERTA_MARGEM_BAIXA -> "Aviso de baixa margem na oferta ${offer.appName}. Lucro líquido de apenas R$ ${String.format("%.2f", netProfit)}. Restam apenas R$ ${String.format("%.2f", netPerKm)} por quilômetro após cobrir combustível e pneu."
            NetProfitTier.PREJUIZO_OPERACIONAL -> "Alerta crítico! Oferta ${offer.appName} em prejuízo operacional. Custo da corrida é de R$ ${String.format("%.2f", totalCost)}, deixando apenas R$ ${String.format("%.2f", netProfit)} de lucro. Recusa altamente recomendada!"
        }

        return NetProfitCalculation(
            grossFare = fare,
            totalDistanceKm = distKm,
            fuelCost = fuelCost,
            maintenanceCost = maintCost,
            totalOperationalCost = totalCost,
            netProfit = netProfit,
            grossPerKm = grossPerKm,
            netPerKm = netPerKm,
            profitMarginPercent = margin,
            efficiencyTier = tier,
            voiceAlertText = voiceAlert
        )
    }

    /**
     * Calcula o resumo de lucro diário consolidado da sessão ativa.
     */
    fun calculateDailySessionNetProfit(
        grossEarningsToday: Double,
        totalKmDrivenToday: Double,
        config: MotorcycleExpenseConfig = MotorcycleExpenseConfig()
    ): Double {
        val totalFuel = (totalKmDrivenToday / config.fuelAutonomyKmPerLiter) * config.fuelPricePerLiter
        val totalMaint = totalKmDrivenToday * config.maintenanceCostPerKm
        return max(0.0, grossEarningsToday - (totalFuel + totalMaint))
    }
}
