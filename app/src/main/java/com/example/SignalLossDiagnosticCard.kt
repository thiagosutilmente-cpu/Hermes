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
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.SignalCellularConnectedNoInternet0Bar
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

private val NeonGreen = Color(0xFF00FF88)
private val CyberCyan = Color(0xFF00D2FF)
private val CyberGold = Color(0xFFFFD700)
private val AlertRed = Color(0xFFFF4D4D)
private val CardSurface = Color(0xFF13131D)
private val CardBorder = Color(0xFF232335)
private val TextLight = Color(0xFFF0F0F8)
private val TextMuted = Color(0xFF8888A2)

@Composable
fun SignalLossDiagnosticCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val engineState by OfflineSignalSimulationEngine.state.collectAsState()
    val isOffline = engineState.networkState == OfflineSignalSimulationEngine.NetworkSimulationState.OFFLINE_TUNNEL

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("signal_loss_diagnostic_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Brush.horizontalGradient(
                if (isOffline) listOf(AlertRed.copy(alpha = 0.8f), Color(0xFFFF8800).copy(alpha = 0.8f))
                else listOf(CyberCyan.copy(alpha = 0.6f), NeonGreen.copy(alpha = 0.6f))
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header do Card
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isOffline) AlertRed.copy(alpha = 0.2f) else CyberCyan.copy(alpha = 0.2f)
                            )
                            .border(
                                1.dp,
                                if (isOffline) AlertRed.copy(alpha = 0.5f) else CyberCyan.copy(alpha = 0.4f),
                                RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isOffline) Icons.Default.WifiOff else Icons.Default.Wifi,
                            contentDescription = "Sinal",
                            tint = if (isOffline) AlertRed else CyberCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "SIMULADOR DE PERDA DE SINAL",
                                color = TextLight,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isOffline) AlertRed.copy(alpha = 0.2f) else NeonGreen.copy(alpha = 0.2f))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isOffline) "TÚNEL" else "4G/5G",
                                    color = if (isOffline) AlertRed else NeonGreen,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = "Teste de cache local Room em túneis e subsolos",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }

                // Status Badge da Conexão
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isOffline) AlertRed.copy(alpha = 0.2f) else NeonGreen.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isOffline) AlertRed else NeonGreen)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (isOffline) "SEM SINAL" else "ONLINE",
                            color = if (isOffline) AlertRed else NeonGreen,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Grid de Métricas de Resiliência Offline
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricBox(
                    label = "ENTREGAS NO CACHE",
                    value = "${engineState.activeDeliveries.size} ativas",
                    accentColor = CyberGold,
                    modifier = Modifier.weight(1f)
                )
                MetricBox(
                    label = "ROTA PRESERVADA",
                    value = "100%",
                    accentColor = NeonGreen,
                    modifier = Modifier.weight(1f)
                )
                MetricBox(
                    label = "RECUPERAÇÃO",
                    value = "${engineState.recoveryLatencyMs} ms",
                    accentColor = CyberCyan,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Cartão de Rota Salva no SQLite Room Local
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF141724))
                    .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
                    .padding(10.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "STATUS DA ROTA MESCLADA (LOCAL ROOM):",
                            color = CyberCyan,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isOffline) "🔒 PROTEGIDO OFFLINE" else "☁️ SINCRONIZADO",
                            color = if (isOffline) CyberGold else NeonGreen,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Coleta: ${engineState.routeStatus.hubName} -> 1ª: ${engineState.routeStatus.primaryDestination} -> 2ª: ${engineState.routeStatus.secondaryDestination}",
                        color = TextLight,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Total combinado: R$ ${String.format(Locale("pt", "BR"), "%.2f", engineState.routeStatus.combinedPayoutBrl)} (${engineState.routeStatus.totalDistanceKm} km)",
                        color = TextMuted,
                        fontSize = 9.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Log de Eventos de Rede
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0A0C12))
                    .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Text(
                    text = engineState.lastEventMessage,
                    color = if (isOffline) AlertRed else TextLight,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Ações do Simulador
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (isOffline) {
                            OfflineSignalSimulationEngine.simulateSignalRestoration(context)
                        } else {
                            OfflineSignalSimulationEngine.simulateSignalLoss(context)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isOffline) Color(0xFF133220) else Color(0xFF381515)
                    )
                ) {
                    Icon(
                        imageVector = if (isOffline) Icons.Default.CloudDone else Icons.Default.SignalCellularConnectedNoInternet0Bar,
                        contentDescription = "Simular",
                        tint = if (isOffline) NeonGreen else AlertRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isOffline) "Restabelecer Conexão" else "Simular Perda de Sinal",
                        color = if (isOffline) NeonGreen else Color(0xFFFF7070),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (isOffline) {
                    OutlinedButton(
                        onClick = {
                            OfflineSignalSimulationEngine.recordActionWhileOffline("Chegada no balcão de coleta (offline)")
                            HapticFeedbackHelper.vibrateTap(context)
                            Toast.makeText(context, "Ação gravada no buffer offline!", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "Ação Sem Sinal",
                            color = CyberGold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricBox(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF181824))
            .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
            .padding(vertical = 8.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                color = TextMuted,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                color = accentColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}
