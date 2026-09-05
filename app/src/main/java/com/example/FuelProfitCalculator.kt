package com.example

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

/**
 * Modelo de Configuração de Combustível da Motocicleta
 */
data class FuelConfig(
    val kmPerLiter: Double = 35.0,
    val fuelPricePerLiter: Double = 5.89
)

/**
 * Cartão Executivo de Lucro Líquido e Gestão de Combustível
 */
@Composable
fun FuelProfitCard(
    grossEarnings: Double,
    totalKmDriven: Double,
    fuelConfig: FuelConfig,
    onUpdateConfig: (FuelConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    // Cálculos em tempo real
    val litersConsumed = if (fuelConfig.kmPerLiter > 0) totalKmDriven / fuelConfig.kmPerLiter else 0.0
    val fuelCost = litersConsumed * fuelConfig.fuelPricePerLiter
    val netProfit = (grossEarnings - fuelCost).coerceAtLeast(0.0)
    val netGainPerKm = if (totalKmDriven > 0) netProfit / totalKmDriven else 0.0
    val profitMarginPercent = if (grossEarnings > 0) ((netProfit / grossEarnings) * 100.0).coerceIn(0.0, 100.0) else 100.0

    val formattedGross = String.format(Locale.GERMANY, "R$ %.2f", grossEarnings)
    val formattedCost = String.format(Locale.GERMANY, "- R$ %.2f", fuelCost)
    val formattedNet = String.format(Locale.GERMANY, "R$ %.2f", netProfit)
    val formattedNetPerKm = String.format(Locale.GERMANY, "R$ %.2f/km líq.", netGainPerKm)
    val formattedKm = String.format(Locale.GERMANY, "%.1f km", totalKmDriven)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, NeonGreen.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .testTag("fuel_profit_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Lucro Líquido & Toggle de Configuração
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
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
                    Text(
                        text = "LUCRO LÍQUIDO REAL NO BOLSO",
                        color = TextLight,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isExpanded) "Ocultar" else "Calibrar",
                        color = NeonGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expandir configurações de combustível",
                        tint = NeonGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Destaque Principal: Lucro Líquido
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = formattedNet,
                        color = NeonGreen,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Margem líquida de ${String.format(Locale.GERMANY, "%.1f", profitMarginPercent)}% hoje",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formattedNetPerKm,
                        color = Color(0xFF00E5FF),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Baseado em $formattedKm rodados",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Barra de Margem Líquida
            LinearProgressIndicator(
                progress = { (profitMarginPercent / 100.0).toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = NeonGreen,
                trackColor = RedDecline.copy(alpha = 0.35f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Linha com Quebra: Bruto vs Custo Gasolina
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "💰 Bruto: ", color = TextMuted, fontSize = 12.sp)
                    Text(text = formattedGross, color = TextLight, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "⛽ Gasolina: ", color = TextMuted, fontSize = 12.sp)
                    Text(text = formattedCost, color = RedDecline, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            // Painel Expansível de Calibração da Moto
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkCardElevated)
                        .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        text = "⚙️ CALIBRAÇÃO DE CONSUMO DA MOTO",
                        color = TextLight,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Consumo da Moto (km/L)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Rendimento médio:", color = TextMuted, fontSize = 11.sp)
                        Text(
                            text = "${fuelConfig.kmPerLiter.toInt()} km/L",
                            color = NeonGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Slider(
                        value = fuelConfig.kmPerLiter.toFloat(),
                        onValueChange = { onUpdateConfig(fuelConfig.copy(kmPerLiter = it.toDouble())) },
                        valueRange = 20f..55f,
                        steps = 7,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonGreen,
                            activeTrackColor = NeonGreen,
                            inactiveTrackColor = DarkBorder
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Preço do Litro da Gasolina (R$/L)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Preço do Combustível:", color = TextMuted, fontSize = 11.sp)
                        Text(
                            text = String.format(Locale.GERMANY, "R$ %.2f / L", fuelConfig.fuelPricePerLiter),
                            color = Color(0xFFFFB300),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Slider(
                        value = fuelConfig.fuelPricePerLiter.toFloat(),
                        onValueChange = { onUpdateConfig(fuelConfig.copy(fuelPricePerLiter = it.toDouble())) },
                        valueRange = 4.50f..7.50f,
                        steps = 12,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFFFB300),
                            activeTrackColor = Color(0xFFFFB300),
                            inactiveTrackColor = DarkBorder
                        )
                    )
                }
            }
        }
    }
}
