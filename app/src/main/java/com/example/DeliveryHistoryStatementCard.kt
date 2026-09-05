package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

/**
 * Item de entrega concluída para o histórico diário
 */
data class CompletedDeliveryItem(
    val id: String,
    val restaurant: String,
    val appSource: String,
    val grossValue: Double,
    val distanceKm: Double,
    val timeMinutes: Int,
    val netProfit: Double,
    val timestamp: String
)

/**
 * HISTÓRICO DE CORRIDAS & EXTRATO DIÁRIO DETALHADO
 * Mostra o detalhamento financeiro das corridas finalizadas no dia,
 * discriminando o ganho bruto, custo de combustível estimado e lucro líquido real.
 */
@Composable
fun DeliveryHistoryStatementCard(
    deliveries: List<CompletedDeliveryItem>,
    fuelConfig: FuelConfig,
    modifier: Modifier = Modifier
) {
    val totalGross = deliveries.sumOf { it.grossValue }
    val totalKm = deliveries.sumOf { it.distanceKm }
    val totalFuelCost = (totalKm / fuelConfig.kmPerLiter) * fuelConfig.fuelPricePerLiter
    val totalNet = totalGross - totalFuelCost

    val formattedGross = String.format(Locale.GERMANY, "R$ %.2f", totalGross)
    val formattedFuel = String.format(Locale.GERMANY, "R$ %.2f", totalFuelCost)
    val formattedNet = String.format(Locale.GERMANY, "R$ %.2f", totalNet)

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, DarkBorder, RoundedCornerShape(22.dp))
            .testTag("delivery_history_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header do Extrato
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "📑", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "EXTRATO DETALHADO DO DIA",
                            color = TextLight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = "${deliveries.size} corridas finalizadas",
                            color = NeonGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Resumo do Lucro Real
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formattedNet,
                        color = NeonGreen,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "LUCRO LÍQUIDO",
                        color = TextMuted,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Resumo Financeiro Consolidado (Bruto - Combustível = Líquido)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkBg)
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Total Bruto", color = TextMuted, fontSize = 9.sp)
                    Text(text = formattedGross, color = TextLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Text(text = "-", color = TextMuted, fontSize = 14.sp)
                Column {
                    Text(text = "Combustível", color = TextMuted, fontSize = 9.sp)
                    Text(text = formattedFuel, color = Color(0xFFFF8800), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Text(text = "=", color = TextMuted, fontSize = 14.sp)
                Column {
                    Text(text = "Líquido no Bolso", color = TextMuted, fontSize = 9.sp)
                    Text(text = formattedNet, color = NeonGreen, fontSize = 13.sp, fontWeight = FontWeight.Black)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Lista das Últimas Corridas
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                deliveries.take(4).forEach { item ->
                    val appColor = when {
                        item.appSource.contains("iFood", ignoreCase = true) -> RedIFood
                        item.appSource.contains("Rappi", ignoreCase = true) -> OrangeRappi
                        item.appSource.contains("99", ignoreCase = true) -> Yellow99
                        else -> TextLight
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkCardElevated)
                            .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = item.appSource,
                                        color = appColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "• ${item.timestamp}",
                                        color = TextMuted,
                                        fontSize = 9.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = item.restaurant,
                                    color = TextLight,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = String.format(Locale.GERMANY, "+ R$ %.2f", item.grossValue),
                                    color = NeonGreen,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "${item.distanceKm} km • ${item.timeMinutes} min",
                                    color = TextMuted,
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
