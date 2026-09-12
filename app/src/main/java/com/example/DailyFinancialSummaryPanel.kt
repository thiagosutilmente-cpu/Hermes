package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

/**
 * Registro individual de ganhos para cada dia da semana
 */
data class DailyEarningRecord(
    val dayOfWeek: String,       // "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom"
    val dateFormatted: String,   // "03/09", "04/09", etc.
    val grossAmount: Double,
    val netProfit: Double,
    val kmDriven: Double,
    val deliveryCount: Int,
    val isToday: Boolean = false,
    val isBestDay: Boolean = false
)

/**
 * PAINEL DE VISUALIZAÇÃO DE GANHOS DIÁRIOS E SEMANAIS (RADAR AI COCKPIT)
 *
 * Permite ao entregador alternar entre a visão diária (turno atual de hoje)
 * e o acumulado semanal (últimos 7 dias), com:
 * - Alternador dinâmico de período: "📅 Diário (Hoje)" vs "📆 Semanal (7 Dias)"
 * - Faturamento Bruto Consolidado, Custo de Combustível e Lucro Líquido Real em mãos
 * - Gráfico interativo de barras verticais dos 7 dias com seleção e inspeção de dia
 * - Comparativo de desempenho: Ritmo de hoje vs média diária da semana
 * - Progresso dinâmico de metas financeiras (Meta Diária e Meta Semanal)
 * - Distribuição financeira automática por aplicativo parceiro (iFood, Rappi, Uber, 99)
 * - Exportação de relatório financeiro completo formatado para WhatsApp/Área de Transferência
 */
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DailyFinancialSummaryPanel(
    deliveries: List<CompletedDeliveryItem> = listOf(
        CompletedDeliveryItem("c1", "Burger King Paulista", "iFood", 33.0, 4.2, 14, 28.5, "12:15"),
        CompletedDeliveryItem("c2", "Pizza Hut Jardins", "Rappi", 18.0, 2.4, 11, 15.6, "11:40"),
        CompletedDeliveryItem("c3", "Starbucks Frei Caneca", "iFood + Rappi", 26.5, 3.1, 12, 22.8, "11:05"),
        CompletedDeliveryItem("c4", "McDonald's Rebouças", "99 Food", 19.0, 2.8, 10, 16.2, "10:20")
    ),
    fuelConfig: FuelConfig = FuelConfig(kmPerLiter = 35.0, fuelPricePerLiter = 5.89),
    dailyGoal: Double = 350.0,
    weeklyGoal: Double = 2200.0,
    onResetTurn: () -> Unit = {},
    onExportReport: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    // 0: Visão Diária (Hoje), 1: Visão Semanal (Últimos 7 dias)
    var selectedPeriodTab by remember { mutableIntStateOf(0) }
    var isBreakdownExpanded by remember { mutableStateOf(false) }

    // ─────────────────────────────────────────────────────────────────────────────
    // 1. CÁLCULOS DO DIA ATUAL (HOJE)
    // ─────────────────────────────────────────────────────────────────────────────
    val todayGross = deliveries.sumOf { it.grossValue }
    val todayKm = deliveries.sumOf { it.distanceKm }
    val todayMinutes = deliveries.sumOf { it.timeMinutes }
    val todayDeliveriesCount = deliveries.size

    val todayFuelLiters = if (fuelConfig.kmPerLiter > 0) todayKm / fuelConfig.kmPerLiter else 0.0
    val todayFuelCost = todayFuelLiters * fuelConfig.fuelPricePerLiter
    val todayNetProfit = (todayGross - todayFuelCost).coerceAtLeast(0.0)

    val todayAvgGainPerKm = if (todayKm > 0) todayGross / todayKm else 0.0
    val todayAvgPerDelivery = if (todayDeliveriesCount > 0) todayGross / todayDeliveriesCount else 0.0
    val todayProfitMargin = if (todayGross > 0) ((todayNetProfit / todayGross) * 100.0).coerceIn(0.0, 100.0) else 100.0

    // Surplus gerado pela triagem neural (vs média da rua R$ 3,20/km)
    val baselineRate = 3.20
    val todaySurplus = (todayGross - (todayKm * baselineRate)).coerceAtLeast(0.0)

    // Meta diária
    val dailyProgress = if (dailyGoal > 0) (todayGross / dailyGoal).toFloat().coerceIn(0f, 1f) else 0f
    val animatedDailyProgress by animateFloatAsState(
        targetValue = dailyProgress,
        animationSpec = tween(durationMillis = 800),
        label = "dailyGoalAnim"
    )
    val remainingDaily = (dailyGoal - todayGross).coerceAtLeast(0.0)

    // ─────────────────────────────────────────────────────────────────────────────
    // 2. DADOS E CÁLCULOS DA SEMANA (7 DIAS COM HOJE DINÂMICO)
    // ─────────────────────────────────────────────────────────────────────────────
    // Histórico dos 6 dias anteriores da semana + o dia de hoje dinamicamente calculado
    val weeklyDays = remember(todayGross, todayKm, todayDeliveriesCount, todayNetProfit) {
        listOf(
            DailyEarningRecord("Seg", "03/09", 215.00, 174.20, 46.5, 9),
            DailyEarningRecord("Ter", "04/09", 198.50, 160.80, 43.0, 8),
            DailyEarningRecord("Qua", "05/09", 254.00, 208.50, 56.2, 11),
            DailyEarningRecord("Qui", "06/09", 288.00, 236.10, 62.0, 12),
            DailyEarningRecord("Sex", "07/09", 362.50, 297.20, 81.5, 15),
            DailyEarningRecord("Sáb", "08/09", 410.00, 336.50, 89.0, 17, isBestDay = true),
            DailyEarningRecord(
                dayOfWeek = "Dom",
                dateFormatted = "Hoje",
                grossAmount = if (todayGross > 0) todayGross else 284.50,
                netProfit = if (todayNetProfit > 0) todayNetProfit else 228.40,
                kmDriven = if (todayKm > 0) todayKm else 38.2,
                deliveryCount = if (todayDeliveriesCount > 0) todayDeliveriesCount else 9,
                isToday = true
            )
        )
    }

    var selectedDayIndex by remember { mutableIntStateOf(6) } // Inicialmente o dia de hoje (Dom)
    val inspectedDay = weeklyDays.getOrElse(selectedDayIndex) { weeklyDays.last() }

    val weeklyTotalGross = weeklyDays.sumOf { it.grossAmount }
    val weeklyTotalNet = weeklyDays.sumOf { it.netProfit }
    val weeklyTotalKm = weeklyDays.sumOf { it.kmDriven }
    val weeklyTotalDeliveries = weeklyDays.sumOf { it.deliveryCount }
    val weeklyFuelCost = ((weeklyTotalKm / fuelConfig.kmPerLiter) * fuelConfig.fuelPricePerLiter).coerceAtLeast(0.0)

    val weeklyDailyAverage = if (weeklyDays.isNotEmpty()) weeklyTotalGross / weeklyDays.size else 0.0
    val weeklyAvgKm = if (weeklyTotalKm > 0) weeklyTotalGross / weeklyTotalKm else 0.0
    val weeklyMarginPercent = if (weeklyTotalGross > 0) ((weeklyTotalNet / weeklyTotalGross) * 100.0).coerceIn(0.0, 100.0) else 80.0

    // Meta semanal
    val weeklyProgress = if (weeklyGoal > 0) (weeklyTotalGross / weeklyGoal).toFloat().coerceIn(0f, 1f) else 0f
    val animatedWeeklyProgress by animateFloatAsState(
        targetValue = weeklyProgress,
        animationSpec = tween(durationMillis = 800),
        label = "weeklyGoalAnim"
    )
    val remainingWeekly = (weeklyGoal - weeklyTotalGross).coerceAtLeast(0.0)

    // Comparativo Hoje vs Média Diária da Semana
    val todayVsWeeklyAvgDiffPercent = if (weeklyDailyAverage > 0) {
        (((inspectedDay.grossAmount - weeklyDailyAverage) / weeklyDailyAverage) * 100.0)
    } else 0.0

    // ─────────────────────────────────────────────────────────────────────────────
    // 3. DISTRIBUIÇÃO POR APLICATIVO
    // ─────────────────────────────────────────────────────────────────────────────
    val appTotalsDaily = deliveries.groupBy { item ->
        when {
            item.appSource.contains("iFood", ignoreCase = true) -> "iFood"
            item.appSource.contains("Rappi", ignoreCase = true) -> "Rappi"
            item.appSource.contains("Uber", ignoreCase = true) -> "Uber Direct"
            item.appSource.contains("99", ignoreCase = true) -> "99 Food"
            else -> "Outros Apps"
        }
    }.mapValues { entry -> entry.value.sumOf { it.grossValue } }

    val appTotalsWeekly = mapOf(
        "iFood" to weeklyTotalGross * 0.52,
        "Rappi" to weeklyTotalGross * 0.24,
        "Uber Direct" to weeklyTotalGross * 0.16,
        "99 Food" to weeklyTotalGross * 0.08
    )

    val displayedAppTotals = if (selectedPeriodTab == 0) {
        if (appTotalsDaily.isNotEmpty()) appTotalsDaily else mapOf("iFood" to 142.50, "Rappi" to 78.00, "Uber Direct" to 44.00, "99 Food" to 20.00)
    } else {
        appTotalsWeekly
    }

    val maxDayAmount = weeklyDays.maxOfOrNull { it.grossAmount } ?: 400.0

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, NeonGreen.copy(alpha = 0.5f), RoundedCornerShape(22.dp))
            .testTag("daily_financial_summary_panel")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // ─────────────────────────────────────────────────────────────────────────
            // CABEÇALHO DO PAINEL FINANCEIRO
            // ─────────────────────────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(NeonGreen)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "PAINEL DE DESEMPENHO FINANCEIRO",
                            color = TextLight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = "Acompanhamento em tempo real • Radar AI",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(NeonGreen.copy(alpha = 0.15f))
                        .border(1.dp, NeonGreen.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "⚡ CONSOLIDADO",
                        color = NeonGreen,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ─────────────────────────────────────────────────────────────────────────
            // SELETOR DE PERÍODO: DIÁRIO (HOJE) vs SEMANAL (7 DIAS)
            // ─────────────────────────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkBg)
                    .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Aba Diário
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (selectedPeriodTab == 0) NeonGreen else Color.Transparent)
                        .clickable { selectedPeriodTab = 0 }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "📅 Ganhos Diários",
                            color = if (selectedPeriodTab == 0) Color(0xFF0A0A0F) else TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                // Aba Semanal
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (selectedPeriodTab == 1) NeonGreen else Color.Transparent)
                        .clickable { selectedPeriodTab = 1 }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "📆 Ganhos Semanais",
                            color = if (selectedPeriodTab == 1) Color(0xFF0A0A0F) else TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ─────────────────────────────────────────────────────────────────────────
            // CONTEÚDO DINÂMICO CONFORME O PERÍODO SELECIONADO
            // ─────────────────────────────────────────────────────────────────────────
            if (selectedPeriodTab == 0) {
                // =====================================================================
                // VISÃO DIÁRIA (TURNO ATUAL DE HOJE)
                // =====================================================================
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkCardElevated)
                        .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "LUCRO LÍQUIDO DE HOJE (NO BOLSO)",
                                color = TextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = String.format(Locale.GERMANY, "R$ %.2f", todayNetProfit),
                                color = NeonGreen,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-0.5).sp
                            )
                            Text(
                                text = "Margem líquida de ${String.format(Locale.GERMANY, "%.1f%%", todayProfitMargin)} das entregas",
                                color = TextLight.copy(alpha = 0.8f),
                                fontSize = 10.sp
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "FATURAMENTO BRUTO",
                                color = TextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = String.format(Locale.GERMANY, "R$ %.2f", todayGross),
                                color = TextLight,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Combustível: - R$ ${String.format(Locale.GERMANY, "%.2f", todayFuelCost)}",
                                color = RedDecline,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Grid de Eficiência do Dia
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FinancialMetricBox(
                        label = "MÉDIA R$/KM",
                        value = String.format(Locale.GERMANY, "R$ %.2f", todayAvgGainPerKm),
                        subtext = if (todayAvgGainPerKm >= 5.0) "🌟 Nível Ouro" else "Dentro da Meta",
                        valueColor = if (todayAvgGainPerKm >= 5.0) NeonGreen else TextLight,
                        modifier = Modifier.weight(1f)
                    )

                    FinancialMetricBox(
                        label = "SURPLUS RADAR",
                        value = "+ R$ ${String.format(Locale.GERMANY, "%.2f", todaySurplus)}",
                        subtext = "vs R$ 3,20/km rua",
                        valueColor = Color(0xFFF7C200),
                        modifier = Modifier.weight(1f)
                    )

                    FinancialMetricBox(
                        label = "TICKET MÉDIO",
                        value = String.format(Locale.GERMANY, "R$ %.2f", todayAvgPerDelivery),
                        subtext = "$todayDeliveriesCount corridas",
                        valueColor = TextLight,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Progresso da Meta Diária
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkBg.copy(alpha = 0.5f))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "META DIÁRIA (R$ ${String.format(Locale.GERMANY, "%.2f", dailyGoal)})",
                            color = TextLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = String.format(Locale.GERMANY, "%.1f%%", dailyProgress * 100f),
                            color = if (dailyProgress >= 1f) NeonGreen else Color(0xFFF7C200),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { animatedDailyProgress },
                        color = if (dailyProgress >= 1f) NeonGreen else Color(0xFF00D1FF),
                        trackColor = DarkCardElevated,
                        strokeCap = StrokeCap.Round,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (dailyProgress >= 1f) "🎯 Meta diária batida! Parabéns." else "Faltam R$ ${String.format(Locale.GERMANY, "%.2f", remainingDaily)} para bater o dia",
                            color = if (dailyProgress >= 1f) NeonGreen else TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Rodagem: ${String.format(Locale.GERMANY, "%.1f km", todayKm)}",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }
            } else {
                // =====================================================================
                // VISÃO SEMANAL (ÚLTIMOS 7 DIAS COM ANÁLISE COMPLETA)
                // =====================================================================
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkCardElevated)
                        .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "LUCRO LÍQUIDO DA SEMANA (7 DIAS)",
                                color = TextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = String.format(Locale.GERMANY, "R$ %.2f", weeklyTotalNet),
                                color = NeonGreen,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-0.5).sp
                            )
                            Text(
                                text = "Margem acumulada de ${String.format(Locale.GERMANY, "%.1f%%", weeklyMarginPercent)} no período",
                                color = TextLight.copy(alpha = 0.8f),
                                fontSize = 10.sp
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "TOTAL BRUTO SEMANAL",
                                color = TextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = String.format(Locale.GERMANY, "R$ %.2f", weeklyTotalGross),
                                color = TextLight,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Combustível: - R$ ${String.format(Locale.GERMANY, "%.2f", weeklyFuelCost)}",
                                color = RedDecline,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Métricas Globais da Semana
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FinancialMetricBox(
                        label = "MÉDIA DIÁRIA",
                        value = String.format(Locale.GERMANY, "R$ %.2f", weeklyDailyAverage),
                        subtext = "Por dia trabalhado",
                        valueColor = TextLight,
                        modifier = Modifier.weight(1f)
                    )

                    FinancialMetricBox(
                        label = "KM SEMANAL",
                        value = String.format(Locale.GERMANY, "%.0f km", weeklyTotalKm),
                        subtext = "Média ${String.format(Locale.GERMANY, "R$ %.2f/km", weeklyAvgKm)}",
                        valueColor = Color(0xFF38BDF8),
                        modifier = Modifier.weight(1f)
                    )

                    FinancialMetricBox(
                        label = "TOTAL CORRIDAS",
                        value = "$weeklyTotalDeliveries",
                        subtext = "Concluídas na semana",
                        valueColor = Color(0xFFF7C200),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Progresso da Meta Semanal
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkBg.copy(alpha = 0.5f))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "META SEMANAL (R$ ${String.format(Locale.GERMANY, "%.2f", weeklyGoal)})",
                            color = TextLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = String.format(Locale.GERMANY, "%.1f%%", weeklyProgress * 100f),
                            color = if (weeklyProgress >= 1f) NeonGreen else Color(0xFFF7C200),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { animatedWeeklyProgress },
                        color = if (weeklyProgress >= 1f) NeonGreen else Color(0xFFF7C200),
                        trackColor = DarkCardElevated,
                        strokeCap = StrokeCap.Round,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (weeklyProgress >= 1f) "🏆 Meta semanal atingida! Parabéns pelo lucro." else "Faltam R$ ${String.format(Locale.GERMANY, "%.2f", remainingWeekly)} para a meta semanal",
                            color = if (weeklyProgress >= 1f) NeonGreen else TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Ritmo: ${if (todayVsWeeklyAvgDiffPercent >= 0) "+${String.format(Locale.GERMANY, "%.0f%%", todayVsWeeklyAvgDiffPercent)}" else String.format(Locale.GERMANY, "%.0f%%", todayVsWeeklyAvgDiffPercent)} vs média",
                            color = if (todayVsWeeklyAvgDiffPercent >= 0) NeonGreen else Color(0xFFFF9F43),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ─────────────────────────────────────────────────────────────────────
                // GRÁFICO INTERATIVO DE BARRAS DOS 7 DIAS DA SEMANA
                // ─────────────────────────────────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(DarkCardElevated)
                        .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "FATURAMENTO DOS 7 DIAS DA SEMANA",
                            color = TextLight,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Toque na barra para inspecionar",
                            color = TextMuted,
                            fontSize = 9.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Linha das 7 Colunas com Gráfico de Barras
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(115.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        weeklyDays.forEachIndexed { index, day ->
                            val heightPercent = if (maxDayAmount > 0) (day.grossAmount / maxDayAmount).toFloat().coerceIn(0.12f, 1f) else 0.2f
                            val isSelected = index == selectedDayIndex

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedDayIndex = index }
                                    .padding(horizontal = 2.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom
                            ) {
                                // Valor no topo da barra
                                Text(
                                    text = if (day.isBestDay) "🏆" else "${day.grossAmount.toInt()}",
                                    color = if (isSelected || day.isToday) NeonGreen else TextMuted,
                                    fontSize = 8.sp,
                                    fontWeight = if (isSelected || day.isToday) FontWeight.Black else FontWeight.Normal
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                // Barra Vertical
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height((75 * heightPercent).dp)
                                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                        .background(
                                            when {
                                                isSelected -> NeonGreen
                                                day.isToday -> NeonGreen.copy(alpha = 0.75f)
                                                day.isBestDay -> Color(0xFFFFD700)
                                                else -> Color(0xFF262638)
                                            }
                                        )
                                        .border(
                                            width = if (isSelected) 1.5.dp else 0.dp,
                                            color = if (isSelected) Color.White else Color.Transparent,
                                            shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                                        )
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                // Rótulo do Dia
                                Text(
                                    text = day.dayOfWeek,
                                    color = if (isSelected || day.isToday) NeonGreen else TextLight,
                                    fontSize = 9.sp,
                                    fontWeight = if (isSelected || day.isToday) FontWeight.Black else FontWeight.Medium
                                )
                                Text(
                                    text = day.dateFormatted,
                                    color = if (day.isToday) NeonGreen else TextMuted,
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Card de Detalhes do Dia Selecionado na Barra
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkBg)
                            .border(1.dp, if (inspectedDay.isToday) NeonGreen.copy(alpha = 0.5f) else DarkBorder, RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${inspectedDay.dayOfWeek} (${inspectedDay.dateFormatted})",
                                        color = TextLight,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    if (inspectedDay.isToday) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(NeonGreen.copy(alpha = 0.2f))
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text("HOJE", color = NeonGreen, fontSize = 8.sp, fontWeight = FontWeight.Black)
                                        }
                                    }
                                    if (inspectedDay.isBestDay) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFFFFD700).copy(alpha = 0.2f))
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text("RECORDE DA SEMANA 🏆", color = Color(0xFFFFD700), fontSize = 8.sp, fontWeight = FontWeight.Black)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${inspectedDay.deliveryCount} entregas • ${inspectedDay.kmDriven} km rodados",
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = String.format(Locale.GERMANY, "R$ %.2f Bruto", inspectedDay.grossAmount),
                                    color = TextLight,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = String.format(Locale.GERMANY, "R$ %.2f Líquido", inspectedDay.netProfit),
                                    color = NeonGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ─────────────────────────────────────────────────────────────────────────
            // DIVISÃO POR APLICATIVO PARCEIRO (ACCORDION EXPANSÍVEL)
            // ─────────────────────────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { isBreakdownExpanded = !isBreakdownExpanded }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isBreakdownExpanded) {
                        "Ocultar divisão por aplicativo"
                    } else {
                        "Ver faturamento por app (${if (selectedPeriodTab == 0) "Hoje" else "Na Semana"})"
                    },
                    color = NeonGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = if (isBreakdownExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = NeonGreen,
                    modifier = Modifier.size(18.dp)
                )
            }

            AnimatedVisibility(visible = isBreakdownExpanded) {
                val currentPeriodGross = if (selectedPeriodTab == 0) todayGross else weeklyTotalGross
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    displayedAppTotals.forEach { (appName, appValue) ->
                        val sharePercent = if (currentPeriodGross > 0) (appValue / currentPeriodGross).toFloat().coerceIn(0f, 1f) else 0f
                        val appColor = when {
                            appName.contains("iFood", ignoreCase = true) -> RedIFood
                            appName.contains("Rappi", ignoreCase = true) -> OrangeRappi
                            appName.contains("Uber", ignoreCase = true) -> Color(0xFFE0E0E0)
                            appName.contains("99", ignoreCase = true) -> Yellow99
                            else -> NeonGreen
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkCardElevated)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(appColor)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = appName,
                                        color = TextLight,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "${String.format(Locale.GERMANY, "R$ %.2f", appValue)} (${String.format(Locale.GERMANY, "%.0f%%", sharePercent * 100f)})",
                                    color = TextLight,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            LinearProgressIndicator(
                                progress = { sharePercent },
                                color = appColor,
                                trackColor = DarkBg,
                                strokeCap = StrokeCap.Round,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ─────────────────────────────────────────────────────────────────────────
            // BOTÕES DE AÇÃO: EXPORTAR RELATÓRIO / NOVO TURNO
            // ─────────────────────────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        if (selectedPeriodTab == 0) {
                            onExportReport()
                        } else {
                            val weeklyReportText = buildString {
                                appendLine("📊 FECHAMENTO SEMANAL RADAR AI (ÚLTIMOS 7 DIAS)")
                                appendLine("💰 Total Bruto: R$ ${String.format(Locale.GERMANY, "%.2f", weeklyTotalGross)}")
                                appendLine("🟢 Lucro Líquido: R$ ${String.format(Locale.GERMANY, "%.2f", weeklyTotalNet)} (${String.format(Locale.GERMANY, "%.1f%%", weeklyMarginPercent)})")
                                appendLine("⛽ Combustível: - R$ ${String.format(Locale.GERMANY, "%.2f", weeklyFuelCost)}")
                                appendLine("🏍️ Rodagem Total: ${String.format(Locale.GERMANY, "%.1f km", weeklyTotalKm)} | $weeklyTotalDeliveries entregas")
                                appendLine("📈 Média Diária: R$ ${String.format(Locale.GERMANY, "%.2f", weeklyDailyAverage)}/dia | R$ ${String.format(Locale.GERMANY, "%.2f/km", weeklyAvgKm)}")
                                appendLine("🎯 Meta Semanal: ${String.format(Locale.GERMANY, "%.1f%%", weeklyProgress * 100f)} atingida (Meta R$ ${String.format(Locale.GERMANY, "%.2f", weeklyGoal)})")
                                appendLine("🚀 Triagem inteligente com Radar Coordinator")
                            }
                            try {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                val clip = ClipData.newPlainText("Relatório Semanal Radar AI", weeklyReportText)
                                clipboard?.setPrimaryClip(clip)
                                Toast.makeText(context, "Relatório Semanal copiado com sucesso!", Toast.LENGTH_LONG).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, weeklyReportText, Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = NeonGreen
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("btn_export_financial_report")
                ) {
                    Text(
                        text = if (selectedPeriodTab == 0) "📤 Relatório Hoje" else "📤 Relatório Semanal",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onResetTurn,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TextMuted
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("btn_reset_turn")
                ) {
                    Text(
                        text = "🔄 Novo Turno",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Caixa compacta para exibir métricas individuais do painel financeiro
 */
@Composable
fun FinancialMetricBox(
    label: String,
    value: String,
    subtext: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(DarkCardElevated)
            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                color = TextMuted,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                color = valueColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = subtext,
                color = TextMuted,
                fontSize = 8.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
