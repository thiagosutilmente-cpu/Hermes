package com.example

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

private val NeonGreen = Color(0xFF00FF88)
private val AmberGold = Color(0xFFFFB800)
private val AlertRed = Color(0xFFFF3366)
private val CyanTech = Color(0xFF00E5FF)
private val DarkCardSurface = Color(0xFF10141D)
private val CardBorderColor = Color(0xFF1F2636)
private val TextLight = Color(0xFFF3F4F8)
private val TextMuted = Color(0xFF8A93A6)

/**
 * Card de Calibração e Monitoramento do Motor de Rentabilidade em Tempo Real (RealTimeProfitEngine)
 * Permite calibrar custo do combustível, autonomia da moto e simular corridas com thresholds coloridos no HUD.
 */
@Composable
fun RealTimeProfitEngineCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val overlayManager = remember { OverlayWindowManager.getInstance(context) }

    var fuelPrice by remember { mutableDoubleStateOf(RealTimeProfitEngine.getFuelPrice()) }
    var kmPerLiter by remember { mutableDoubleStateOf(RealTimeProfitEngine.getKmPerLiter()) }
    var simFare by remember { mutableDoubleStateOf(28.50) }
    var simDistance by remember { mutableDoubleStateOf(3.9) }
    var isCalibrating by remember { mutableStateOf(false) }

    // Cálculo instantâneo pelo motor
    val metrics = remember(simFare, simDistance, fuelPrice, kmPerLiter) {
        RealTimeProfitEngine.calculateTripMetrics(
            fareValue = simFare,
            distanceKm = simDistance,
            customFuelPrice = fuelPrice,
            customKmPerLiter = kmPerLiter
        )
    }

    val thresholdColor = when (metrics.thresholdLevel) {
        ProfitThresholdLevel.HIGH_PROFIT -> NeonGreen
        ProfitThresholdLevel.MEDIUM_PROFIT -> AmberGold
        ProfitThresholdLevel.LOW_PROFIT -> AlertRed
    }

    val thresholdBg = when (metrics.thresholdLevel) {
        ProfitThresholdLevel.HIGH_PROFIT -> Color(0xFF122A1E)
        ProfitThresholdLevel.MEDIUM_PROFIT -> Color(0xFF2A2210)
        ProfitThresholdLevel.LOW_PROFIT -> Color(0xFF2E1418)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("card_realtime_profit_engine"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCardSurface),
        border = androidx.compose.foundation.BorderStroke(1.2.dp, thresholdColor.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header do Motor
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(thresholdColor.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalGasStation,
                            contentDescription = null,
                            tint = thresholdColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "CALCULADORA DE R$/KM",
                                color = TextLight,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = thresholdBg,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = metrics.thresholdLevel.badgeLabel,
                                    color = thresholdColor,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Desconto real de gasolina e desgaste por km",
                            color = TextMuted,
                            fontSize = 10.5.sp
                        )
                    }
                }

                OutlinedButton(
                    onClick = { isCalibrating = !isCalibrating },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanTech),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(text = if (isCalibrating) "Fechar" else "Calibrar", fontSize = 10.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // BANNER PRINCIPAL COM THRESHOLD COLORIDO
            Surface(
                color = thresholdBg,
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.2.dp, thresholdColor)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = "LUCRO LÍQUIDO POR KM",
                                color = TextMuted,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = metrics.formattedNetPerKm,
                                color = thresholdColor,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "TARIFA BRUTA",
                                color = TextMuted,
                                fontSize = 9.5.sp
                            )
                            Text(
                                text = metrics.formattedGrossPerKm,
                                color = TextLight,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Linha com Detalhamento de Custos
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        CostItem(label = "Gasolina da Viagem", value = metrics.formattedFuelCost, color = AlertRed)
                        CostItem(label = "Lucro no Bolso", value = metrics.formattedNetProfit, color = NeonGreen)
                        CostItem(label = "Margem Líquida", value = metrics.formattedMargin, color = CyanTech)
                    }
                }
            }

            // Seção de Calibração Expansível (Preço Gasolina e km/l da moto)
            AnimatedVisibility(visible = isCalibrating) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0A0E16))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "CALIBRAÇÃO DA SUA MOTO:",
                        color = CyanTech,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Slider Preço da Gasolina
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Preço do Litro da Gasolina", color = TextLight, fontSize = 11.sp)
                        Text(
                            text = String.format(Locale.GERMANY, "R$ %.2f/L", fuelPrice),
                            color = AmberGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = fuelPrice.toFloat(),
                        onValueChange = {
                            fuelPrice = it.toDouble()
                            RealTimeProfitEngine.saveConfig(fuelPrice = fuelPrice, kmPerLiter = kmPerLiter)
                        },
                        valueRange = 4.50f..8.50f,
                        colors = SliderDefaults.colors(thumbColor = AmberGold, activeTrackColor = AmberGold)
                    )

                    // Slider Autonomia da Moto (km/L)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Autonomia da Moto (Consumo)", color = TextLight, fontSize = 11.sp)
                        Text(
                            text = String.format(Locale.GERMANY, "%.0f km/L", kmPerLiter),
                            color = NeonGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = kmPerLiter.toFloat(),
                        onValueChange = {
                            kmPerLiter = it.toDouble()
                            RealTimeProfitEngine.saveConfig(fuelPrice = fuelPrice, kmPerLiter = kmPerLiter)
                        },
                        valueRange = 20f..55f,
                        colors = SliderDefaults.colors(thumbColor = NeonGreen, activeTrackColor = NeonGreen)
                    )

                    Text(
                        text = "💡 Custo por KM rodado da sua moto: R$ ${String.format(Locale.GERMANY, "%.2f", fuelPrice / kmPerLiter)}/km de combustível",
                        color = Color.LightGray,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Botão para testar o cálculo ao vivo no HUD Flutuante
            Button(
                onClick = {
                    overlayManager.showTacticalOffer(
                        OverlayWindowManager.FloatingTacticalOffer(
                            appName = "iFood",
                            packageName = "com.ifood.driver",
                            value = simFare,
                            distanceKm = simDistance,
                            pricePerKm = metrics.grossPricePerKm,
                            isGoodDeal = metrics.thresholdLevel == ProfitThresholdLevel.HIGH_PROFIT,
                            dealLabel = metrics.thresholdLabel,
                            destination = "Av. Paulista, 1578 • Bela Vista",
                            pickup = "Burger King Paulista",
                            hasDualRoute = metrics.thresholdLevel == ProfitThresholdLevel.HIGH_PROFIT,
                            dualRouteAppName = "99 Moto",
                            dualRouteExtraGain = 16.50
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .testTag("btn_send_metrics_to_hud"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = thresholdColor,
                    contentColor = Color.Black
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "TESTAR MÉTRICAS NO HUD FLUTUANTE 🫧",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Botão para ouvir a síntese de voz (TTS) do cálculo em tempo real
            OutlinedButton(
                onClick = {
                    val tts = OfferTextToSpeechEngine.getInstance(context)
                    tts.speakOfferSummary(
                        appName = "iFood",
                        restaurant = "Burger King Paulista",
                        value = simFare,
                        distanceKm = simDistance,
                        profitPerKm = metrics.netProfitPerKm,
                        isGoodDeal = metrics.thresholdLevel == ProfitThresholdLevel.HIGH_PROFIT,
                        hasDualRoute = false
                    )
                    Toast.makeText(context, "🔊 Anunciando valor, km e lucro líquido por voz!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .testTag("btn_speak_profit_metrics_tts"),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, thresholdColor.copy(alpha = 0.8f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = thresholdColor)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "OUVIR CÁLCULO POR VOZ (TTS) 🔊",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun CostItem(label: String, value: String, color: Color) {
    Column {
        Text(text = label, color = TextMuted, fontSize = 9.sp)
        Text(text = value, color = color, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
    }
}
