package com.example.util

import com.example.data.OfferEntity
import kotlin.math.*

/**
 * Motor de Inteligência de Realidade dos Aplicativos de Entrega no Brasil (2026).
 * Analisa as regras específicas e ocultas de cada plataforma:
 * - iFood: Score de Alocação, Trava de Castigo (Pausa Forçada), Bateladas e Níveis (Nuvem / OL)
 * - Rappi: Auto-Aceite, Rappi Turbo (Dark Stores), Filas de Supermercado e Cartão Rappi
 * - Uber Moto/Flash: Radar de Viagens (Sniping) e Ocultamento de Endereço
 * - 99Moto: Metas Diárias com Bônus Cumulativo (Atingimento de Metas)
 * - Lalamove / Loggi: Cubagem de Carga, Volume de Baú e Saldo Negativo na Carteira
 */

enum class IFoodRiderLevel(val label: String, val minAcceptanceRate: Double) {
    DIAMANTE("Diamante", 0.90),
    OURO("Ouro", 0.82),
    PRATA("Prata", 0.70),
    NUVEM_PADRAO("Nuvem Sub-25", 0.50)
}

data class IFoodPunishmentRisk(
    val currentAcceptanceRate: Double,
    val totalOffersToday: Int,
    val rejectionsToday: Int,
    val isRiskOfLockout: Boolean, // Risco de entrar no "Castigo" do iFood (15-30 min sem rotas)
    val remainingSafeRejections: Int,
    val adviceMessage: String
)

data class BonusGoalImpact(
    val appName: String,
    val currentTrips: Int,
    val targetTrips: Int,
    val bonusAmount: Double,
    val effectiveBonusPerRemainingTrip: Double,
    val isNearGoal: Boolean,
    val recommendation: String
)

data class AppSpecificRealityInsight(
    val appName: String,
    val primaryAlert: String,
    val hiddenFeeOrBonus: String,
    val operationalTip: String,
    val riskFactorScore: Int, // 0 (Tranquilo) a 100 (Perigo Alto)
    val actionSuggestion: String
)

object BrazilianCourierRealityEngine {

    /**
     * Calcula o Risco de Castigo (Pausa Forçada de 15-30 min) no iFood ao recusar uma oferta ruim.
     */
    fun calculateIFoodLockoutRisk(
        totalOffersToday: Int = 18,
        rejectionsToday: Int = 3,
        userLevel: IFoodRiderLevel = IFoodRiderLevel.OURO
    ): IFoodPunishmentRisk {
        val total = totalOffersToday.coerceAtLeast(1)
        val accepted = (total - rejectionsToday).coerceAtLeast(0)
        val currentRate = accepted.toDouble() / total.toDouble()

        // Se recusar +1 oferta agora:
        val newTotal = total + 1
        val newAccepted = accepted
        val newRate = newAccepted.toDouble() / newTotal.toDouble()

        val isRisk = newRate < userLevel.minAcceptanceRate || rejectionsToday >= 3

        val safeRejectionsLeft = max(0, ((total * (1.0 - userLevel.minAcceptanceRate)) - rejectionsToday).toInt())

        val message = when {
            rejectionsToday >= 3 -> "⚠️ ALERTA DE CASTIGO: Você já recusou $rejectionsToday ofertas seguidas! A próxima recusa pode pausar seu app iFood por 30 minutos."
            newRate < userLevel.minAcceptanceRate -> "⚠️ RISCO DE QUEDA DE NÍVEL: Recusar esta oferta reduz sua taxa para ${String.format("%.1f", newRate * 100)}% (Mínimo ${userLevel.label}: ${String.format("%.0f", userLevel.minAcceptanceRate * 100)}%)."
            else -> "SEGURO PARA RECUSAR: Você ainda pode recusar $safeRejectionsLeft ofertas hoje sem perder o nível ${userLevel.label}."
        }

        return IFoodPunishmentRisk(
            currentAcceptanceRate = currentRate,
            totalOffersToday = total,
            rejectionsToday = rejectionsToday,
            isRiskOfLockout = isRisk,
            remainingSafeRejections = safeRejectionsLeft,
            adviceMessage = message
        )
    }

    /**
     * Calcula o Valor Real Ajustado de uma corrida da 99Moto ou Uber considerando metas diárias com bônus.
     */
    fun calculateEffectiveOfferValueWithBonus(
        offer: OfferEntity,
        bonusGoal: BonusGoalImpact?
    ): Double {
        if (bonusGoal == null || !bonusGoal.isNearGoal) return offer.fareValue

        // Se falta apenas 1 corrida para ganhar R$ 50 de bônus, essa corrida vale Tarifa + R$ 50!
        val remainingTrips = (bonusGoal.targetTrips - bonusGoal.currentTrips).coerceAtLeast(1)
        val bonusAdd = bonusGoal.bonusAmount / remainingTrips
        return offer.fareValue + bonusAdd
    }

    /**
     * Analisa as particularidades operacionais da oferta segundo a realidade de cada app no Brasil.
     */
    fun analyzeAppReality(offer: OfferEntity): AppSpecificRealityInsight {
        val app = offer.appName.lowercase()

        return when {
            app.contains("ifood") -> {
                val isBatelada = offer.pickupAddress.contains("batelada", ignoreCase = true) ||
                                 offer.deliveryAddress.contains("2 entregas", ignoreCase = true)

                AppSpecificRealityInsight(
                    appName = "iFood Para Entregadores",
                    primaryAlert = if (isBatelada) "BATELADA DETECTADA: Paga R$ 6.50 no 1º pedido e apenas R$ 3.00 na 2ª entrega." else "SISTEMA DE SCORE NUVEM ATIVO",
                    hiddenFeeOrBonus = "Deslocamento de Coleta pago a partir do momento do aceite.",
                    operationalTip = "Aguarde dentro do restaurante se a cozinha estiver atrasada; acione a tolerância de 15 min para liberar cancelamento sem punição.",
                    riskFactorScore = if (isBatelada) 45 else 20,
                    actionSuggestion = if (isBatelada) "Verifique se a 2ª entrega é no mesmo bairro; se for contramão, recuse." else "Aceite para manter a Taxa de Alocação do Nível Diamante/Ouro."
                )
            }

            app.contains("rappi") -> {
                val isTurbo = offer.pickupAddress.contains("turbo", ignoreCase = true) || offer.appName.contains("turbo", ignoreCase = true)
                val isMarket = offer.pickupAddress.contains("carrefour", ignoreCase = true) || offer.pickupAddress.contains("pão de açúcar", ignoreCase = true)

                AppSpecificRealityInsight(
                    appName = "Rappi Entregador",
                    primaryAlert = if (isMarket) "COMPRA EM SUPERMERCADO: Risco de fila e conferência de itens (30-45 min)." else "ATENÇÃO AO AUTO-ACEITE",
                    hiddenFeeOrBonus = if (isTurbo) "Bônus Rappi Turbo por entrega no prazo (+R$ 4.00)" else "Gorjeta do cliente incluída no valor facial.",
                    operationalTip = if (isMarket) "Certifique-se de ter saldo liberado no Cartão Rappi antes de entrar no caixa." else "Mantenha o Auto-Aceite DESATIVADO em dias de pico para escolher melhores taxas.",
                    riskFactorScore = if (isMarket) 75 else 30,
                    actionSuggestion = if (isMarket) "Só aceite se o valor compensar o tempo de espera no caixa (+R$ 25.00)." else "Verifique a distância exata da entrega antes que o tempo de aceite expire."
                )
            }

            app.contains("uber") -> {
                val isFlash = offer.appName.contains("flash", ignoreCase = true) ||
                              offer.pickupAddress.contains("flash", ignoreCase = true) ||
                              offer.deliveryAddress.contains("flash", ignoreCase = true)

                AppSpecificRealityInsight(
                    appName = "Uber Driver (Moto / Flash)",
                    primaryAlert = if (isFlash) "UBER FLASH ENCOMENDA: Exige entrega em mãos ou código de confirmação." else "RADAR DE VIAGENS ATIVO",
                    hiddenFeeOrBonus = "Preço Dinâmico (Surge Hexagonal) atualizado em tempo real.",
                    operationalTip = "No Radar de Viagens, o Radar Coordinator analisa a rentabilidade R$/km em 0.8s antes de você clicar no Snipe.",
                    riskFactorScore = if (isFlash) 35 else 25,
                    actionSuggestion = "Exija o código PIN de 4 dígitos do cliente ao entregar encomendas do Uber Flash."
                )
            }

            app.contains("99") -> {
                AppSpecificRealityInsight(
                    appName = "99Moto",
                    primaryAlert = "CAMPANHA DE METAS DIÁRIAS ATIVA (BÔNUS CUMULATIVO)",
                    hiddenFeeOrBonus = "Garantia de Ganhos Diários (Ex: R$ 150 por 15 corridas).",
                    operationalTip = "Corridas curtas de R$ 6.00 a R$ 8.00 são ideais para bater a meta de bônus mais rápido.",
                    riskFactorScore = 15,
                    actionSuggestion = "Foque na quantidade de corridas para atingir a Meta Ouro do dia."
                )
            }

            app.contains("lalamove") || app.contains("loggi") -> {
                AppSpecificRealityInsight(
                    appName = if (app.contains("lalamove")) "Lalamove" else "Loggi",
                    primaryAlert = "ATENÇÃO À CUBAGEM DA CARGA E PESO NA BAG/BAÚ",
                    hiddenFeeOrBonus = "Comissão deduzida diretamente do saldo da carteira do app.",
                    operationalTip = "Verifique a dimensão das caixas antes de retirar no galpão para não violar a capacidade da moto.",
                    riskFactorScore = 50,
                    actionSuggestion = "Confira se o saldo na carteira está positivo para receber a corrida sem bloqueios."
                )
            }

            else -> {
                AppSpecificRealityInsight(
                    appName = offer.appName,
                    primaryAlert = "APP PARCEIRO MULTI-ENTREGA",
                    hiddenFeeOrBonus = "Pagamento conforme tabela padrão.",
                    operationalTip = "Mantenha a localização ativada para sincronização exata da rota.",
                    riskFactorScore = 20,
                    actionSuggestion = "Avalie o ganho mínimo por km (Meta R$ 3.50/km)."
                )
            }
        }
    }
}
