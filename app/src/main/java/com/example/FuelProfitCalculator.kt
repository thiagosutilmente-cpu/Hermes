package com.example

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

/**
 * Modelos de Motocicletas mais Populares no Brasil
 */
data class MotorcyclePreset(
    val id: String,
    val name: String,
    val shortName: String,
    val averageKmPerLiter: Double
)

val POPULAR_MOTORCYCLE_PRESETS = listOf(
    MotorcyclePreset("cg160", "Honda CG 160 Fan/Titan", "CG 160", 41.0),
    MotorcyclePreset("biz125", "Honda Biz 125", "Biz 125", 45.0),
    MotorcyclePreset("factor150", "Yamaha Factor 150", "Factor 150", 42.0),
    MotorcyclePreset("pop110", "Honda Pop 110i", "Pop 110i", 55.0),
    MotorcyclePreset("scooter", "PCX / NMAX Scooter", "Scooter", 38.0)
)

/**
 * Modelo de Configuração de Combustível da Motocicleta e Condições Climáticas
 */
data class FuelConfig(
    val kmPerLiter: Double = 41.0,
    val fuelPricePerLiter: Double = 5.89,
    val selectedModelId: String = "cg160",
    val isRainModeActive: Boolean = false,
    val rainSurgePercentage: Double = 35.0
)

/**
 * Cartão Executivo de Lucro Líquido, Gestão de Combustível e Modo Chuva
 */
@Composable
fun FuelProfitCard(
    grossEarnings: Double,
    totalKmDriven: Double,
    fuelConfig: FuelConfig,
    onUpdateConfig: (FuelConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }

    // Cálculos em tempo real
    val effectiveKmPerLiter = fuelConfig.kmPerLiter.coerceAtLeast(10.0)
    val litersConsumed = if (effectiveKmPerLiter > 0) totalKmDriven / effectiveKmPerLiter else 0.0
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
            .border(
                1.2.dp,
                if (fuelConfig.isRainModeActive) Color(0xFF00D2FF).copy(alpha = 0.6f) else NeonGreen.copy(alpha = 0.35f),
                RoundedCornerShape(20.dp)
            )
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
                            .background(if (fuelConfig.isRainModeActive) Color(0xFF00D2FF) else NeonGreen)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (fuelConfig.isRainModeActive) "LUCRO REAL • MODO CHUVA ATIVO 🌧️" else "LUCRO LÍQUIDO REAL NO BOLSO",
                        color = if (fuelConfig.isRainModeActive) Color(0xFF00D2FF) else TextLight,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isExpanded) "Ocultar" else "Calibrar Moto",
                        color = if (fuelConfig.isRainModeActive) Color(0xFF00D2FF) else NeonGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expandir configurações de combustível",
                        tint = if (fuelConfig.isRainModeActive) Color(0xFF00D2FF) else NeonGreen,
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

            // Painel Expansível de Calibração da Moto & Clima
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
                        text = "🏍️ SELEÇÃO DE MODELO DE MOTO:",
                        color = TextLight,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Chips de Modelos Populares
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        POPULAR_MOTORCYCLE_PRESETS.forEach { preset ->
                            val isSelected = fuelConfig.selectedModelId == preset.id
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) NeonGreen.copy(alpha = 0.2f) else Color(0xFF1E293B))
                                    .border(1.dp, if (isSelected) NeonGreen else DarkBorder, RoundedCornerShape(8.dp))
                                    .clickable {
                                        val modelSpec = FuelManager.findModelByNameOrId(preset.id)
                                        val manager = FuelManager(
                                            initialModel = modelSpec,
                                            initialMode = if (fuelConfig.isRainModeActive) WeatherMode.RAIN else WeatherMode.DRY,
                                            initialFuelPrice = fuelConfig.fuelPricePerLiter
                                        )
                                        onUpdateConfig(
                                            fuelConfig.copy(
                                                selectedModelId = preset.id,
                                                kmPerLiter = manager.calculateUrbanConsumptionKmPerLiter()
                                            )
                                        )
                                        HapticFeedbackHelper.vibrateClick(context)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = preset.shortName,
                                        color = if (isSelected) NeonGreen else Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${preset.averageKmPerLiter.toInt()} km/L",
                                        color = TextMuted,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Consumo da Moto Ajustável (km/L)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Rendimento médio fino:", color = TextMuted, fontSize = 11.sp)
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
                        valueRange = 20f..60f,
                        steps = 8,
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
                        Text(text = "Preço da Gasolina:", color = TextMuted, fontSize = 11.sp)
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

                    Spacer(modifier = Modifier.height(10.dp))

                    // MODO CHUVA / PISTA MOLHADA (+35% SOBRETAXA NO PISO MÍNIMO)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0A1826))
                            .border(1.dp, Color(0xFF00D2FF).copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.WaterDrop,
                                contentDescription = "Modo Chuva",
                                tint = Color(0xFF00D2FF),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "MODO CHUVA / PISTA MOLHADA",
                                    color = Color.White,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "+35% de sobretaxa no corte mínimo do JARVIS",
                                    color = Color(0xFF00D2FF),
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Switch(
                            checked = fuelConfig.isRainModeActive,
                            onCheckedChange = { active ->
                                val modelSpec = FuelManager.findModelByNameOrId(fuelConfig.selectedModelId)
                                val manager = FuelManager(
                                    initialModel = modelSpec,
                                    initialMode = if (active) WeatherMode.RAIN else WeatherMode.DRY,
                                    initialFuelPrice = fuelConfig.fuelPricePerLiter
                                )
                                val adjustedKmPerLiter = manager.calculateUrbanConsumptionKmPerLiter()
                                onUpdateConfig(
                                    fuelConfig.copy(
                                        isRainModeActive = active,
                                        kmPerLiter = adjustedKmPerLiter
                                    )
                                )
                                if (active) {
                                    HapticFeedbackHelper.vibrateRainModeSurge(context)
                                } else {
                                    HapticFeedbackHelper.vibrateClick(context)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF00D2FF),
                                checkedTrackColor = Color(0xFF00557A),
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = DarkBg
                            )
                        )
                    }

                    if (fuelConfig.isRainModeActive) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val currentSpec = FuelManager.findModelByNameOrId(fuelConfig.selectedModelId)
                        Text(
                            text = "🌧️ Ajuste Físico: Arrasto de capa de chuva e tráfego urbano aplicados (-${currentSpec.rainTrafficPenaltyPercent.toInt()}% no rendimento).",
                            color = Color(0xFF00D2FF).copy(alpha = 0.85f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
