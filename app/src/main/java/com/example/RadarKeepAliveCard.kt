package com.example

import android.content.Context
import android.os.PowerManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * RadarKeepAliveCard
 *
 * Card visual no Cockpit demonstrando o estado em tempo real do:
 * 1. Foreground Service Anti-Morte (WakeLock ativo + Proteção em Bateria Baixa / Doze Mode).
 * 2. Conexão WebSocket Direta com Auto-Reconexão Inteligente e Heartbeat de Telemetria.
 * 3. Botões de Ação Rápida configurados na barra de notificação persistente do Android.
 */
@Composable
fun RadarKeepAliveCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isRunning by RadarKeepAliveForegroundService.isServiceRunning.collectAsState()
    val isMonitoring by RadarKeepAliveForegroundService.isRadarMonitoringActive.collectAsState()

    val webSocketEngine = remember { WebSocketKeepAliveEngine.getInstance(context) }
    val socketState by webSocketEngine.connectionState.collectAsState()
    val lastHeartbeat by webSocketEngine.lastHeartbeatTimestamp.collectAsState()
    val messagesCount by webSocketEngine.receivedMessagesCount.collectAsState()
    val locState by LocationService.globalLocationState.collectAsState()

    val powerManager = remember { context.getSystemService(Context.POWER_SERVICE) as? PowerManager }
    val isPowerSaveMode = powerManager?.isPowerSaveMode == true

    val statusBorderColor by animateColorAsState(
        targetValue = when (socketState) {
            is SocketConnectionState.Connected -> Color(0xFF10B981) // Verde esmeralda
            is SocketConnectionState.Reconnecting -> Color(0xFFF59E0B) // Âmbar
            is SocketConnectionState.Connecting -> Color(0xFF38BDF8) // Azul
            else -> Color(0xFFEF4444) // Vermelho
        },
        animationSpec = tween(500),
        label = "borderColor"
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.5.dp, statusBorderColor.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .testTag("card_radar_keepalive")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // CABEÇALHO DO SERVIÇO PERSISTENTE
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                if (isRunning) Color(0xFF10B981).copy(alpha = 0.2f)
                                else Color(0xFFEF4444).copy(alpha = 0.2f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = "Radar KeepAlive",
                            tint = if (isRunning) Color(0xFF10B981) else Color(0xFFEF4444),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "RADAR KEEPALIVE & ANTI-MORTE",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (isRunning) Color(0xFF059669) else Color(0xFFDC2626)
                            ) {
                                Text(
                                    text = if (isRunning) "24/7 ATIVO" else "PARADO",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "CPU WakeLock + GPS Fused + Conexão WebSocket",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // STATUS DO WEBSOCKET
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1E293B).copy(alpha = 0.7f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = when (socketState) {
                                    is SocketConnectionState.Connected -> Icons.Default.Wifi
                                    else -> Icons.Default.WifiOff
                                },
                                contentDescription = "WebSocket",
                                tint = when (socketState) {
                                    is SocketConnectionState.Connected -> Color(0xFF10B981)
                                    is SocketConnectionState.Reconnecting -> Color(0xFFF59E0B)
                                    else -> Color(0xFFEF4444)
                                },
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (val s = socketState) {
                                    is SocketConnectionState.Connected -> "WebSocket VPS Conectado"
                                    is SocketConnectionState.Connecting -> "Conectando ao Socket..."
                                    is SocketConnectionState.Reconnecting -> "Reconectando em ${s.nextRetrySec}s (T${s.attempt})"
                                    is SocketConnectionState.Error -> "Offline: ${s.message}"
                                    is SocketConnectionState.Disconnected -> "Desconectado"
                                },
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Botão de forçar reconexão
                        OutlinedButton(
                            onClick = { webSocketEngine.forceReconnect() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                            border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp),
                            modifier = Modifier.height(30.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reconectar", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reconectar", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (lastHeartbeat > 0) {
                                val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                                "Último Heartbeat: ${sdf.format(Date(lastHeartbeat))}"
                            } else {
                                "Nenhum heartbeat transmitido"
                            },
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )

                        Text(
                            text = "Msgs Recebidas: $messagesCount",
                            color = Color(0xFF38BDF8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ALERTA DE ECONOMIA DE BATERIA DO ANDROID / PROTEÇÃO DOZE
            if (isPowerSaveMode) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF78350F).copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.BatteryAlert,
                            contentDescription = "Bateria Baixa",
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Modo Economia do Android Ativo: O WakeLock e Foreground Service mantêm o Socket e GPS sem cortes.",
                            color = Color(0xFFFEF3C7),
                            fontSize = 11.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // BOTÕES DE AÇÃO RÁPIDA DA BARRA DE NOTIFICAÇÃO (PREVIEW INTERATIVO)
            Text(
                text = "AÇÕES RÁPIDAS DA BARRA DE NOTIFICAÇÃO (1 TOQUE):",
                color = Color(0xFF94A3B8),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Ação 1: SOS Silencioso
                Button(
                    onClick = {
                        EmergencySosManager.triggerSosImmediately(
                            context = context,
                            deliveryInfo = "Teste via Cockpit KeepAlive"
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = "SOS", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("🚨 SOS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // Ação 2: Alternar Radar
                Button(
                    onClick = {
                        RadarKeepAliveForegroundService.toggleRadarState(context)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isMonitoring) Color(0xFF475569) else Color(0xFF059669)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp)
                ) {
                    Icon(
                        if (isMonitoring) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Radar",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isMonitoring) "⏸️ Pausar" else "▶️ Ativar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // Ação 3: Testar Ping de Telemetria
                Button(
                    onClick = {
                        webSocketEngine.sendTelemetryHeartbeat()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp)
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Ping", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("⚡ Ping GPS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // RODAPÉ: TELEMETRIA ATUAL
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Notificação persistente ativa no painel do Android",
                        color = Color(0xFF10B981),
                        fontSize = 10.sp
                    )
                }

                Text(
                    text = "${String.format(Locale.US, "%.1f", locState.currentSpeedKmh)} km/h",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
