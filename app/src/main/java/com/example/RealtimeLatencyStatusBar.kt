package com.example

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

private val NeonGreen = Color(0xFF00FF88)
private val CyberCyan = Color(0xFF00D2FF)
private val CyberGold = Color(0xFFFFD700)
private val AlertRed = Color(0xFFFF3366)
private val DarkBg = Color(0xFF0D0F18)
private val TextLight = Color(0xFFF0F0F8)
private val TextMuted = Color(0xFF9898B0)

/**
 * RealtimeLatencyStatusBar
 *
 * Componente visual de monitoramento em tempo real posicionado no topo da tela (Status Bar).
 * Exibe latência em milissegundos, status da rede 4G/5G e dispara alerta visual luminoso
 * e notificação sonora/vocalizada quando o app entra no modo de recuperação offline.
 */
@Composable
fun RealtimeLatencyStatusBar(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val engineState by OfflineSignalSimulationEngine.state.collectAsState()
    val ghostTelemetry by GhostApiVirtualBus.telemetry.collectAsState()

    val isOffline = engineState.networkState == OfflineSignalSimulationEngine.NetworkSimulationState.OFFLINE_TUNNEL

    // Monitora se acabou de ocorrer uma transição de offline para online (Recuperação Offline)
    var wasOfflineBefore by remember { mutableStateOf(false) }
    var showRecoveryAlertBanner by remember { mutableStateOf(false) }

    LaunchedEffect(isOffline) {
        if (wasOfflineBefore && !isOffline) {
            // Acabou de recuperar do modo offline!
            showRecoveryAlertBanner = true
            HapticFeedbackHelper.vibrateSuccess(context)
            try {
                TextToSpeechManager.getInstance(context).speakVoiceAlert(
                    "Modo de recuperação offline concluído com sucesso. Fila e rotas restauradas em ${engineState.recoveryLatencyMs} milissegundos."
                )
            } catch (_: Exception) {}

            kotlinx.coroutines.delay(4500L)
            showRecoveryAlertBanner = false
        }
        wasOfflineBefore = isOffline
    }

    // Animação de pulsação suave para o indicador de sinal
    val infiniteTransition = rememberInfiniteTransition(label = "latency_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val currentLatency = if (isOffline) 0 else (ghostTelemetry.averageLatencyMs.coerceAtLeast(3))

    val statusColor by animateColorAsState(
        targetValue = when {
            showRecoveryAlertBanner -> CyberGold
            isOffline -> AlertRed
            currentLatency < 25 -> NeonGreen
            else -> CyberGold
        },
        label = "statusColor"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("realtime_latency_status_bar")
    ) {
        // Alerta Visual Expansível de Recuperação Offline
        AnimatedVisibility(
            visible = showRecoveryAlertBanner,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(listOf(CyberGold.copy(alpha = 0.95f), NeonGreen.copy(alpha = 0.95f)))
                    )
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "Recuperação",
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "⚡ MODO DE RECUPERAÇÃO OFFLINE ATIVADO: DADOS RESTAURADOS EM ${engineState.recoveryLatencyMs}ms!",
                        color = Color.Black,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.4.sp
                    )
                }
            }
        }

        // Barra Superior Compacta de Latência e Sinal
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkBg)
                .border(
                    width = 0.5.dp,
                    color = statusColor.copy(alpha = 0.35f)
                )
                .clickable {
                    val info = if (isOffline) {
                        "Modo Túnel Ativo: ${engineState.activeDeliveries.size} pedidos mesclados protegidos no SQLite Room"
                    } else {
                        "Rede 4G/5G Excelente: Latência ${currentLatency}ms | GhostAPI Porta 8899"
                    }
                    Toast.makeText(context, info, Toast.LENGTH_SHORT).show()
                }
                .padding(horizontal = 14.dp, vertical = 5.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Lado Esquerdo: Indicador Luminoso e Status da Rede
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(statusColor.copy(alpha = if (isOffline) pulseAlpha else 1f))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isOffline) "SEM SINAL (MODO TÚNEL)" else "REDE 4G/5G ATIVA",
                        color = if (isOffline) AlertRed else TextLight,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                // Lado Central: Alerta ou Telemetria de Cache
                if (isOffline) {
                    Text(
                        text = "🔒 CACHE ROOM ATIVO",
                        color = CyberGold,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black
                    )
                } else if (showRecoveryAlertBanner) {
                    Text(
                        text = "SINCRONIZANDO...",
                        color = CyberGold,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black
                    )
                } else {
                    Text(
                        text = "GHOST-API :8899",
                        color = TextMuted,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Lado Direito: Medidor de Latência em Tempo Real
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isOffline) Icons.Default.WifiOff else Icons.Default.Wifi,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isOffline) "OFFLINE" else "${currentLatency} ms",
                        color = statusColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
