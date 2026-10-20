package com.example

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.network.HostingerLatencyMonitor

/**
 * Níveis de Saúde da Conexão com a VPS Hostinger
 */
enum class VpsHealthTier(
    val label: String,
    val subtitle: String,
    val primaryColor: Color,
    val containerBg: Color
) {
    EXCELLENT(
        label = "CONEXÃO EXCELENTE",
        subtitle = "Baixa latência (< 200ms)",
        primaryColor = Color(0xFF00FF88),
        containerBg = Color(0xFF052414)
    ),
    MODERATE(
        label = "LATÊNCIA MODERADA",
        subtitle = "Atenção (200ms a 500ms)",
        primaryColor = Color(0xFFFFB800),
        containerBg = Color(0xFF2B2005)
    ),
    CRITICAL_BOTTLENECK(
        label = "GARGALO CRÍTICO (> 500ms)",
        subtitle = "Motor local JARVIS ativado (< 5ms)",
        primaryColor = Color(0xFFFF334B),
        containerBg = Color(0xFF2D070E)
    ),
    OFFLINE_OR_UNKNOWN(
        label = "AGUARDANDO DIAGNÓSTICO",
        subtitle = "Toque para testar ping",
        primaryColor = Color(0xFF94A3B8),
        containerBg = Color(0xFF1E293B)
    );

    companion object {
        fun resolve(latencyMs: Long, isBottleneck: Boolean): VpsHealthTier {
            return when {
                latencyMs <= 0L -> OFFLINE_OR_UNKNOWN
                isBottleneck || latencyMs > 500L -> CRITICAL_BOTTLENECK
                latencyMs > 200L -> MODERATE
                else -> EXCELLENT
            }
        }
    }
}

/**
 * VpsHealthHudComponent
 *
 * HUD de Saúde em tempo real para o Cockpit do motoboy.
 * Exibe:
 * 1. Latência atual em milissegundos com tipografia monospaçada de alta legibilidade.
 * 2. Status da conexão com a VPS Hostinger (187.77.248.73:8000).
 * 3. Indicador visual colorido com pulso de LED:
 *    - Verde (< 200ms)
 *    - Amarelo (200ms a 500ms)
 *    - Vermelho (> 500ms - Gatilho de fallback)
 * 4. Botão rápido de teste de ping com feedback háptico.
 */
@Composable
fun VpsHealthHudComponent(
    modifier: Modifier = Modifier,
    onTestPingClicked: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val latencyState by HostingerLatencyMonitor.latencyState.collectAsState()

    var isPinging by remember { mutableStateOf(false) }

    val healthTier = VpsHealthTier.resolve(
        latencyMs = latencyState.lastLatencyMs,
        isBottleneck = latencyState.isBottleneckActive || latencyState.isFallbackToLocalEngineActive
    )

    val animatedColor by animateColorAsState(
        targetValue = healthTier.primaryColor,
        label = "vpsHealthColor"
    )

    // Animação de pulso contínuo para o LED de status
    val infiniteTransition = rememberInfiniteTransition(label = "hudPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (healthTier == VpsHealthTier.CRITICAL_BOTTLENECK) 500 else 1100),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0A0F18)
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .border(
                width = 1.2.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        animatedColor.copy(alpha = 0.8f),
                        animatedColor.copy(alpha = 0.3f),
                        Color(0xFF1E293B)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .testTag("vps_health_hud_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Linha Superior: Ícone de Rede, LED Pulsante, Título e Botão de Ping
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // LED Pulsante de Status
                    Box(
                        modifier = Modifier.size(18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(animatedColor.copy(alpha = 0.25f))
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(animatedColor)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "HUD DE SAÚDE VPS",
                                color = Color.White,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "187.77.248.73",
                                color = TextMuted,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Text(
                            text = healthTier.subtitle,
                            color = animatedColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Botão de Ping Rápido
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF162032))
                        .border(1.dp, Color(0xFF283852), RoundedCornerShape(8.dp))
                        .clickable {
                            isPinging = true
                            HapticFeedbackHelper.vibrateClick(context)
                            HostingerLatencyMonitor.testPing(context) { _, _ ->
                                isPinging = false
                                onTestPingClicked?.invoke()
                            }
                        }
                        .padding(horizontal = 9.dp, vertical = 6.dp)
                        .testTag("btn_vps_health_ping")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isPinging) Icons.Default.Refresh else Icons.Default.Bolt,
                            contentDescription = "Testar Ping",
                            tint = if (isPinging) Color(0xFF00D2FF) else animatedColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isPinging) "Ping..." else "TESTAR",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Display Principal de Latência e Status
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(healthTier.containerBg.copy(alpha = 0.6f))
                    .border(1.dp, animatedColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Latência em Destaque
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = if (latencyState.lastLatencyMs > 0L) "${latencyState.lastLatencyMs}" else "--",
                        color = animatedColor,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = " ms",
                        color = animatedColor.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }

                // Badge do Nível de Conexão
                Column(horizontalAlignment = Alignment.End) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(animatedColor.copy(alpha = 0.2f))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = healthTier.label,
                            color = animatedColor,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    if (latencyState.averageLatencyMs > 0L) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Média: ${latencyState.averageLatencyMs}ms (10 chamadas)",
                            color = TextLight,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Barra Visual de Nível de Latência (0ms a 600ms com marca d'água aos 500ms)
            Spacer(modifier = Modifier.height(8.dp))

            val currentMs = latencyState.lastLatencyMs.toFloat().coerceIn(0f, 600f)
            val progressFraction = (currentMs / 600f).coerceIn(0.02f, 1f)

            Column(modifier = Modifier.fillMaxWidth()) {
                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = animatedColor,
                    trackColor = Color(0xFF1E293B)
                )

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "0ms (Ótimo)", color = Color(0xFF00FF88), fontSize = 8.5.sp)
                    Text(text = "200ms", color = Color(0xFFFFB800), fontSize = 8.5.sp)
                    Text(text = "500ms (Gargalo)", color = Color(0xFFFF334B), fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Banner Tático quando Fallback Local Estiver Ativo
            AnimatedVisibility(
                visible = latencyState.isBottleneckActive || latencyState.isFallbackToLocalEngineActive,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF2D070E))
                        .border(1.dp, Color(0xFFFF334B).copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Alerta",
                                tint = Color(0xFFFF334B),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Gargalo detectado: Motor local JARVIS ativo (< 5ms)",
                                color = Color(0xFFFFB4AB),
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "Liberar",
                            color = Color(0xFF00FF88),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable {
                                    HostingerLatencyMonitor.setFallbackActive(false)
                                    HapticFeedbackHelper.vibrateClick(context)
                                }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
