package com.example.ui.components

import android.content.Context
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.OfferEntity
import com.example.util.CourierNetProfitEngine
import com.example.util.MotorcycleExpenseConfig
import com.example.util.NetProfitTier
import java.util.Locale

@Composable
fun CourierNetProfitWidget() {
    val context = LocalContext.current
    var showConfigPanel by remember { mutableStateOf(false) }

    // Motorcycle Config State
    var fuelPrice by remember { mutableStateOf(5.89) }
    var autonomyKmL by remember { mutableStateOf(35.0) }
    var maintCostPerKm by remember { mutableStateOf(0.12) }

    val config = remember(fuelPrice, autonomyKmL, maintCostPerKm) {
        MotorcycleExpenseConfig(
            fuelPricePerLiter = fuelPrice,
            fuelAutonomyKmPerLiter = autonomyKmL,
            maintenanceCostPerKm = maintCostPerKm
        )
    }

    // Sample offer for live breakdown demonstration
    val sampleOffer = remember {
        OfferEntity(
            appName = "iFood",
            pickupAddress = "McDonald's Paulista",
            deliveryAddress = "Rua Consolação, 1200",
            fareValue = 28.50,
            totalDistance = 6.2,
            totalTime = 18.0,
            suggestion = "ACEITAR",
            reason = "Lucro alto R$ 4.60/km"
        )
    }

    val profitCalc = remember(sampleOffer, config) {
        CourierNetProfitEngine.calculateNetProfit(sampleOffer, config)
    }

    // Text To Speech Manager
    var ttsEngine by remember { mutableStateOf<TextToSpeech?>(null) }
    var isTtsReady by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        var tts: TextToSpeech? = null
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("pt", "BR")
                isTtsReady = true
            }
        }
        ttsEngine = tts

        onDispose {
            tts?.stop()
            tts?.shutdown()
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0D14)),
        border = BorderStroke(1.dp, Brush.horizontalGradient(listOf(Color(0xFF00FF88), Color(0xFF00E5FF))))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00FF88).copy(alpha = 0.2f))
                            .border(1.dp, Color(0xFF00FF88), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MonetizationOn,
                            contentDescription = "Lucro Líquido",
                            tint = Color(0xFF00FF88),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "LUCRO LÍQUIDO REAL & CUSTO OPERACIONAL",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Gasolina R$ ${String.format("%.2f", fuelPrice)}/L • Autonomia $autonomyKmL km/L",
                            color = Color.Gray,
                            fontSize = 9.sp
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White.copy(alpha = 0.08f),
                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f)),
                    modifier = Modifier.clickable { showConfigPanel = !showConfigPanel }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Build, contentDescription = "Ajustar", tint = Color(0xFF00E5FF), modifier = Modifier.size(12.dp))
                        Text(
                            text = if (showConfigPanel) "FECHAR" else "MOTO",
                            color = Color(0xFF00E5FF),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Expandable Configurator Panel
            AnimatedVisibility(visible = showConfigPanel) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "⚙️ AJUSTE DE CUSTOS DA SUA MOTO / BIKE",
                        color = Color(0xFF00E5FF),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )

                    // Gas Price Adjuster
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.LocalGasStation, contentDescription = null, tint = Color(0xFFFFCC00), modifier = Modifier.size(14.dp))
                            Text("Preço Gasolina: R$ ${String.format("%.2f", fuelPrice)}", color = Color.White, fontSize = 9.sp)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Button(
                                onClick = { if (fuelPrice > 3.0) fuelPrice -= 0.10 },
                                modifier = Modifier.size(24.dp),
                                contentPadding = PaddingValues(0.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f))
                            ) { Text("-", color = Color.White, fontSize = 10.sp) }

                            Button(
                                onClick = { fuelPrice += 0.10 },
                                modifier = Modifier.size(24.dp),
                                contentPadding = PaddingValues(0.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f))
                            ) { Text("+", color = Color.White, fontSize = 10.sp) }
                        }
                    }

                    // Autonomy Adjuster
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.TwoWheeler, contentDescription = null, tint = Color(0xFF00FF88), modifier = Modifier.size(14.dp))
                            Text("Autonomia: ${autonomyKmL.toInt()} km/L", color = Color.White, fontSize = 9.sp)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Button(
                                onClick = { if (autonomyKmL > 15) autonomyKmL -= 1.0 },
                                modifier = Modifier.size(24.dp),
                                contentPadding = PaddingValues(0.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f))
                            ) { Text("-", color = Color.White, fontSize = 10.sp) }

                            Button(
                                onClick = { autonomyKmL += 1.0 },
                                modifier = Modifier.size(24.dp),
                                contentPadding = PaddingValues(0.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f))
                            ) { Text("+", color = Color.White, fontSize = 10.sp) }
                        }
                    }
                }
            }

            // Real Earnings Breakdown Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.4f))
                    .border(1.dp, Color(profitCalc.efficiencyTier.colorHex).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(10.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("OFERTA ANALISADA: ${sampleOffer.appName}", color = Color.Gray, fontSize = 8.sp)
                            Text(
                                text = "Bruto: R$ ${String.format("%.2f", sampleOffer.fareValue)} (${sampleOffer.totalDistance} km)",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("Lucro Líquido Limpo", color = Color.Gray, fontSize = 8.sp)
                            Text(
                                text = "R$ ${String.format("%.2f", profitCalc.netProfit)}",
                                color = Color(profitCalc.efficiencyTier.colorHex),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Cost Split Visual Bar
                    val total = sampleOffer.fareValue.coerceAtLeast(0.01)
                    val fuelPct = (profitCalc.fuelCost / total).toFloat().coerceIn(0f, 1f)
                    val maintPct = (profitCalc.maintenanceCost / total).toFloat().coerceIn(0f, 1f)
                    val netPct = (profitCalc.netProfit / total).toFloat().coerceIn(0f, 1f)

                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White.copy(alpha = 0.1f))
                        ) {
                            Box(modifier = Modifier.fillMaxHeight().weight(netPct.coerceAtLeast(0.01f)).background(Color(0xFF00FF88)))
                            Box(modifier = Modifier.fillMaxHeight().weight(fuelPct.coerceAtLeast(0.01f)).background(Color(0xFFFFCC00)))
                            Box(modifier = Modifier.fillMaxHeight().weight(maintPct.coerceAtLeast(0.01f)).background(Color(0xFFFF0055)))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("🟢 Líquido: R$ ${String.format("%.2f", profitCalc.netProfit)} (${String.format("%.0f", profitCalc.profitMarginPercent)}%)", color = Color(0xFF00FF88), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            Text("🟡 Gasolina: R$ ${String.format("%.2f", profitCalc.fuelCost)}", color = Color(0xFFFFCC00), fontSize = 8.sp)
                            Text("🔴 Peças/Óleo: R$ ${String.format("%.2f", profitCalc.maintenanceCost)}", color = Color(0xFFFF0055), fontSize = 8.sp)
                        }
                    }

                    // Tactical Audio HUD Button for Bluetooth Helmet/Headset
                    Button(
                        onClick = {
                            if (isTtsReady && ttsEngine != null) {
                                ttsEngine?.speak(profitCalc.voiceAlertText, TextToSpeech.QUEUE_FLUSH, null, "tts_jarvis_alert")
                                Toast.makeText(context, "🎙️ Transmitindo Alerta para o Capacete Bluetooth...", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, profitCalc.voiceAlertText, Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(34.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF).copy(alpha = 0.2f)),
                        border = BorderStroke(1.dp, Color(0xFF00E5FF))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Headset, contentDescription = "Bluetooth Voice Alert", tint = Color(0xFF00E5FF), modifier = Modifier.size(14.dp))
                            Text(
                                text = "🎙️ OUVIR ALERTA DE VOZ JARVIS NO FONE / CAPACETE",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }
    }
}
