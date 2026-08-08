package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBike
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.OfferEntity
import com.example.util.BrazilianCourierRealityEngine
import com.example.util.BonusGoalImpact
import com.example.util.IFoodRiderLevel
import com.example.util.MultiAppOrderManager

@Composable
fun CourierAppRealitiesWidget() {
    var selectedAppTab by remember { mutableStateOf("iFood") }

    // State for iFood Castigo Calculator
    var totalOffersToday by remember { mutableStateOf(16) }
    var rejectionsToday by remember { mutableStateOf(3) }
    var userLevel by remember { mutableStateOf(IFoodRiderLevel.OURO) }

    val iFoodRisk = remember(totalOffersToday, rejectionsToday, userLevel) {
        BrazilianCourierRealityEngine.calculateIFoodLockoutRisk(
            totalOffersToday = totalOffersToday,
            rejectionsToday = rejectionsToday,
            userLevel = userLevel
        )
    }

    // State for 99Moto Bonus Goal
    var currentTrips99 by remember { mutableStateOf(11) }
    var targetTrips99 by remember { mutableStateOf(12) }
    var bonusAmount99 by remember { mutableStateOf(50.0) }

    val bonusGoal99 = remember(currentTrips99, targetTrips99, bonusAmount99) {
        val remaining = (targetTrips99 - currentTrips99).coerceAtLeast(1)
        BonusGoalImpact(
            appName = "99Moto",
            currentTrips = currentTrips99,
            targetTrips = targetTrips99,
            bonusAmount = bonusAmount99,
            effectiveBonusPerRemainingTrip = bonusAmount99 / remaining,
            isNearGoal = remaining <= 2,
            recommendation = "FOQUE EM CORRIDAS CURTAS DE R$ 6 A R$ 8 PARA LIBERAR O BÔNUS DE R$ ${bonusAmount99.toInt()}!"
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111118)),
        border = BorderStroke(1.dp, Brush.horizontalGradient(listOf(Color(0xFFEA1D2C), Color(0xFFF7C200))))
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
                            .background(Color(0xFFEA1D2C).copy(alpha = 0.2f))
                            .border(1.dp, Color(0xFFEA1D2C), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = "Realidade dos Apps",
                            tint = Color(0xFFEA1D2C),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "REALIDADE PRÁTICA DOS APPS NO BRASIL",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Proteção Anti-Castigo, Bônus de Meta e Análise de Batelada",
                            color = Color.Gray,
                            fontSize = 9.sp
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF00FF88).copy(alpha = 0.15f),
                    border = BorderStroke(0.5.dp, Color(0xFF00FF88))
                ) {
                    Text(
                        text = "MODO MOTOGIRL/MOTOBOY",
                        color = Color(0xFF00FF88),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // App Selector Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("iFood", "Rappi", "99Moto", "Uber Flash").forEach { app ->
                    val isSel = selectedAppTab == app
                    val brandColor = when (app) {
                        "iFood" -> Color(0xFFEA1D2C)
                        "Rappi" -> Color(0xFFFF441F)
                        "99Moto" -> Color(0xFFF7C200)
                        else -> Color(0xFF00E5FF)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) brandColor.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f))
                            .border(1.dp, if (isSel) brandColor else Color.Transparent, RoundedCornerShape(8.dp))
                            .clickable { selectedAppTab = app }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = app,
                            color = if (isSel) Color.White else Color.Gray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Tab Content 1: iFood Castigo & Score Calculator
            if (selectedAppTab == "iFood") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .border(1.dp, Color(0xFFEA1D2C).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🛡️ SIMULADOR ANTI-CASTIGO IFOOD (PAUSA FORÇADA)",
                            color = Color(0xFFEA1D2C),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Nível: ${userLevel.label}",
                            color = Color(0xFFFFCC00),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Interactive controls for recusas and total offers
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Ofertas Hoje: $totalOffersToday", color = Color.Gray, fontSize = 9.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Button(
                                    onClick = { if (totalOffersToday > 1) totalOffersToday-- },
                                    modifier = Modifier.size(26.dp),
                                    contentPadding = PaddingValues(0.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f))
                                ) { Text("-", color = Color.White, fontSize = 10.sp) }

                                Button(
                                    onClick = { totalOffersToday++ },
                                    modifier = Modifier.size(26.dp),
                                    contentPadding = PaddingValues(0.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f))
                                ) { Text("+", color = Color.White, fontSize = 10.sp) }
                            }
                        }

                        Column {
                            Text("Recusas Hoje: $rejectionsToday", color = Color.Gray, fontSize = 9.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Button(
                                    onClick = { if (rejectionsToday > 0) rejectionsToday-- },
                                    modifier = Modifier.size(26.dp),
                                    contentPadding = PaddingValues(0.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f))
                                ) { Text("-", color = Color.White, fontSize = 10.sp) }

                                Button(
                                    onClick = { rejectionsToday++ },
                                    modifier = Modifier.size(26.dp),
                                    contentPadding = PaddingValues(0.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f))
                                ) { Text("+", color = Color.White, fontSize = 10.sp) }
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("Taxa Atual", color = Color.Gray, fontSize = 9.sp)
                            Text(
                                text = "${String.format("%.1f", iFoodRisk.currentAcceptanceRate * 100)}%",
                                color = if (iFoodRisk.isRiskOfLockout) Color(0xFFFF0055) else Color(0xFF00FF88),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Advice Message Banner
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (iFoodRisk.isRiskOfLockout) Color(0xFFFF0055).copy(alpha = 0.2f) else Color(0xFF00FF88).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, if (iFoodRisk.isRiskOfLockout) Color(0xFFFF0055) else Color(0xFF00FF88))
                    ) {
                        Text(
                            text = iFoodRisk.adviceMessage,
                            color = if (iFoodRisk.isRiskOfLockout) Color.White else Color(0xFF00FF88),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    Text(
                        text = "💡 DICA PRÁTICA: Em Bateladas iFood (2 pedidos), o deslocamento de coleta é pago apenas uma vez. Recuse se a 2ª entrega for para o lado oposto do seu bairro.",
                        color = Color.LightGray,
                        fontSize = 8.sp
                    )
                }
            }

            // Tab Content 2: Rappi Auto-Aceite Shield & Turbo
            if (selectedAppTab == "Rappi") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .border(1.dp, Color(0xFFFF441F).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Speed, contentDescription = null, tint = Color(0xFFFF441F), modifier = Modifier.size(16.dp))
                        Text(
                            text = "RAPPI TURBO vs. AUTO-ACEITE INVOLUNTÁRIO",
                            color = Color(0xFFFF441F),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Text(
                        text = "O Auto-Aceite do Rappi força o aceite de corridas longas (R$ 5 para 7km). O Radar Coordinator monitora em tempo real e emite um som vibratório para você desativar a função em horários de pico.",
                        color = Color.LightGray,
                        fontSize = 9.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF00FF88).copy(alpha = 0.15f),
                            border = BorderStroke(0.5.dp, Color(0xFF00FF88))
                        ) {
                            Text("⚡ Dark Store Turbo: +R$ 4.00 Fixos", color = Color(0xFF00FF88), fontSize = 8.sp, modifier = Modifier.padding(6.dp))
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFFF0055).copy(alpha = 0.15f),
                            border = BorderStroke(0.5.dp, Color(0xFFFF0055))
                        ) {
                            Text("🛒 Supermercados: Fila Média 35 min", color = Color(0xFFFF0055), fontSize = 8.sp, modifier = Modifier.padding(6.dp))
                        }
                    }
                }
            }

            // Tab Content 3: 99Moto Bonus Goal Calculator
            if (selectedAppTab == "99Moto") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .border(1.dp, Color(0xFFF7C200).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🎯 SIMULADOR DE METAS DIÁRIAS (99MOTO)",
                            color = Color(0xFFF7C200),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Bônus: R$ ${bonusAmount99.toInt()}",
                            color = Color(0xFF00FF88),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Corridas Concluídas: $currentTrips99 / $targetTrips99", color = Color.White, fontSize = 9.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Button(
                                onClick = { if (currentTrips99 > 0) currentTrips99-- },
                                modifier = Modifier.size(24.dp),
                                contentPadding = PaddingValues(0.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f))
                            ) { Text("-", color = Color.White, fontSize = 9.sp) }

                            Button(
                                onClick = { if (currentTrips99 < targetTrips99) currentTrips99++ },
                                modifier = Modifier.size(24.dp),
                                contentPadding = PaddingValues(0.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f))
                            ) { Text("+", color = Color.White, fontSize = 9.sp) }
                        }
                    }

                    val remaining = targetTrips99 - currentTrips99
                    val addedValue = if (remaining > 0) bonusAmount99 / remaining else 0.0

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF7C200).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color(0xFFF7C200))
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = "FALTA APENAS $remaining CORRIDA(S)! O valor real de uma corrida de R$ 7.00 passa a valer R$ ${String.format("%.2f", 7.0 + addedValue)} no seu bolso!",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Tab Content 4: Uber Flash & Sniping
            if (selectedAppTab == "Uber Flash") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "⚡ UBER TRIP RADAR (SNIPING AUTOMÁTICO EM 0.8S)",
                        color = Color(0xFF00E5FF),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )

                    Text(
                        text = "No Radar de Viagens da Uber, várias ofertas piscam na tela por 3 segundos. O Radar Accessibility Service lê a corrida em background e exibe um balão verde se a taxa for acima de R$ 4.50/km.",
                        color = Color.LightGray,
                        fontSize = 9.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("🔒 Exigência de PIN de 4 Dígitos no Flash", color = Color.White, fontSize = 8.sp)
                        Text("📍 Leitura por OCR de Bairro Oculto", color = Color(0xFF00FF88), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
