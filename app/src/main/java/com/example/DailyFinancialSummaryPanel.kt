package com.example

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

/**
 * PAINEL DE RESUMO FINANCEIRO DIÁRIO (RADAR AI COCKPIT)
 *
 * Calcula automaticamente em tempo real:
 * - Ganho Bruto Total consolidado das entregas aceitas
 * - Custo estimado de combustível gasto no deslocamento
 * - Lucro Líquido Real em mãos
 * - Rentabilidade média por quilômetro (R$/km)
 * - Ganho extra estimado gerado pela triagem neural da IA Radar
 * - Distribuição financeira automática por aplicativo parceiro (iFood, Rappi, Uber, 99)
 * - Progresso dinâmico em relação à meta diária de ganhos
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
    onResetTurn: () -> Unit = {},
    onExportReport: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isBreakdownExpanded by remember { mutableStateOf(false) }

    // Cálculos Automáticos derivados da lista de entregas aceitas pelo Radar AI
    val totalGross = deliveries.sumOf { it.grossValue }
    val totalKm = deliveries.sumOf { it.distanceKm }
    val totalMinutes = deliveries.sumOf { it.timeMinutes }
    val deliveriesCount = deliveries.size

    val fuelLitersConsumed = if (fuelConfig.kmPerLiter > 0) totalKm / fuelConfig.kmPerLiter else 0.0
    val totalFuelCost = fuelLitersConsumed * fuelConfig.fuelPricePerLiter
    val totalNetProfit = (totalGross - totalFuelCost).coerceAtLeast(0.0)

    val averageGainPerKm = if (totalKm > 0) totalGross / totalKm else 0.0
    val averagePerDelivery = if (deliveriesCount > 0) totalGross / deliveriesCount else 0.0
    val profitMarginPercent = if (totalGross > 0) ((totalNetProfit / totalGross) * 100.0).coerceIn(0.0, 100.0) else 100.0

    // Estimativa de Ganho Extra gerado pela triagem da IA:
    // A média típica do mercado sem filtragem inteligente é de aproximadamente R$ 3,20/km
    val baselineMarketRate = 3.20
    val baselineGross = totalKm * baselineMarketRate
    val radarAiSurplus = (totalGross - baselineGross).coerceAtLeast(0.0)

    // Progresso em relação à meta diária
    val goalProgress = if (dailyGoal > 0) (totalGross / dailyGoal).toFloat().coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = goalProgress,
        animationSpec = tween(durationMillis = 800),
        label = "goalProgressAnim"
    )
    val remainingToGoal = (dailyGoal - totalGross).coerceAtLeast(0.0)

    // Agrupamento automático por Aplicativo
    val appTotals = deliveries.groupBy { item ->
        when {
            item.appSource.contains("iFood", ignoreCase = true) -> "iFood"
            item.appSource.contains("Rappi", ignoreCase = true) -> "Rappi"
            item.appSource.contains("Uber", ignoreCase = true) -> "Uber"
            item.appSource.contains("99", ignoreCase = true) -> "99 Food"
            else -> "Outros"
        }
    }.mapValues { entry -> entry.value.sumOf { it.grossValue } }

    val formattedGross = String.format(Locale.GERMANY, "R$ %.2f", totalGross)
    val formattedFuelCost = String.format(Locale.GERMANY, "- R$ %.2f", totalFuelCost)
    val formattedNetProfit = String.format(Locale.GERMANY, "R$ %.2f", totalNetProfit)
    val formattedAvgKm = String.format(Locale.GERMANY, "R$ %.2f/km", averageGainPerKm)
    val formattedAvgDelivery = String.format(Locale.GERMANY, "R$ %.2f", averagePerDelivery)
    val formattedSurplus = String.format(Locale.GERMANY, "+ R$ %.2f", radarAiSurplus)
    val formattedGoal = String.format(Locale.GERMANY, "R$ %.2f", dailyGoal)
    val formattedRemaining = String.format(Locale.GERMANY, "R$ %.2f", remainingToGoal)

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, NeonGreen.copy(alpha = 0.45f), RoundedCornerShape(22.dp))
            .testTag("daily_financial_summary_panel")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 1. Cabeçalho Executivo do Painel
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
                            text = "RESUMO FINANCEIRO DO DIA",
                            color = TextLight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = "Calculado automaticamente via Radar AI",
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
                        text = "🧠 IA INTELIGENTE",
                        color = NeonGreen,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Bloco Central de Ganhos: Ganho Bruto e Lucro Líquido no Bolso
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
                            text = "LUCRO LÍQUIDO (NO BOLSO)",
                            color = TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = formattedNetProfit,
                            color = NeonGreen,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = "Margem líquida de ${String.format(Locale.GERMANY, "%.1f%%", profitMarginPercent)} do faturamento",
                            color = TextLight.copy(alpha = 0.8f),
                            fontSize = 10.sp
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "GANHO BRUTO",
                            color = TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = formattedGross,
                            color = TextLight,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Combustível: $formattedFuelCost",
                            color = RedDecline,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Grid de Eficiência do Piloto & Radar AI (4 Métricas)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Rentabilidade média R$/km
                FinancialMetricBox(
                    label = "MÉDIA R$/KM",
                    value = formattedAvgKm,
                    subtext = if (averageGainPerKm >= 5.0) "🌟 Nível Ouro" else "Dentro da Meta",
                    valueColor = if (averageGainPerKm >= 5.0) NeonGreen else TextLight,
                    modifier = Modifier.weight(1f)
                )

                // Ganho Adicional gerado pelo Radar AI
                FinancialMetricBox(
                    label = "SURPLUS RADAR AI",
                    value = formattedSurplus,
                    subtext = "vs R$ 3,20/km rua",
                    valueColor = Color(0xFFF7C200),
                    modifier = Modifier.weight(1f)
                )

                // Média por Corrida
                FinancialMetricBox(
                    label = "TICKET MÉDIO",
                    value = formattedAvgDelivery,
                    subtext = "$deliveriesCount corridas",
                    valueColor = TextLight,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Barra de Progresso da Meta Diária
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
                        text = "META DIÁRIA ($formattedGoal)",
                        color = TextLight,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = String.format(Locale.GERMANY, "%.1f%%", goalProgress * 100f),
                        color = if (goalProgress >= 1f) NeonGreen else Color(0xFFF7C200),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = { animatedProgress },
                    color = if (goalProgress >= 1f) NeonGreen else Color(0xFF00D1FF),
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
                        text = if (goalProgress >= 1f) "🎯 Meta batida! Parabéns pelo turno." else "Faltam $formattedRemaining para bater a meta",
                        color = if (goalProgress >= 1f) NeonGreen else TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Quilômetros: ${String.format(Locale.GERMANY, "%.1f km", totalKm)}",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 5. Botão Expansível: Distribuição por Aplicativo Parceiro
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
                    text = if (isBreakdownExpanded) "Ocultar divisão por aplicativo" else "Ver faturamento por aplicativo (${appTotals.size} apps)",
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    appTotals.forEach { (appName, appValue) ->
                        val sharePercent = if (totalGross > 0) (appValue / totalGross).toFloat() else 0f
                        val appColor = when (appName) {
                            "iFood" -> RedIFood
                            "Rappi" -> OrangeRappi
                            "Uber" -> Color(0xFFE0E0E0)
                            "99 Food" -> Yellow99
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

            Spacer(modifier = Modifier.height(12.dp))

            // 6. Botões de Ação do Turno (Exportar Relatório / Iniciar Novo Turno)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onExportReport,
                    shape = RoundedCornerShape(10.dp),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                        contentColor = NeonGreen
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("btn_export_financial_report")
                ) {
                    Text(
                        text = "📤 Relatório",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onResetTurn,
                    shape = RoundedCornerShape(10.dp),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
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
