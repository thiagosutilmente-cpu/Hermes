package com.example

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.network.HostingerLatencyMonitor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * HostingerLatencyCockpitCard
 *
 * Card de telemetria em tempo real no Cockpit para monitorar a latência da VPS Hostinger
 * e o acionamento do fallback do JARVIS NEURAL quando o tempo de resposta ultrapassa 500ms.
 */
@Composable
fun HostingerLatencyCockpitCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val latencyState by HostingerLatencyMonitor.latencyState.collectAsState()

    var isTestingPing by remember { mutableStateOf(false) }

    val statusColor by animateColorAsState(
        targetValue = when {
            latencyState.lastLatencyMs == 0L -> Color(0xFF9EABB8)
            latencyState.isBottleneckActive || latencyState.lastLatencyMs > 500L -> RedDecline
            latencyState.lastLatencyMs > 250L -> Color(0xFFFFB800)
            else -> NeonGreen
        },
        label = "statusColor"
    )

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCardElevated),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .border(
                1.2.dp,
                if (latencyState.isBottleneckActive) RedDecline.copy(alpha = 0.8f) else Color(0xFF00D2FF).copy(alpha = 0.4f),
                RoundedCornerShape(18.dp)
            )
            .testTag("hostinger_latency_cockpit_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 1. Cabeçalho com indicador de pulso
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "🌐 LATÊNCIA VPS HOSTINGER",
                        color = Color.White,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                // Badge de Limiar do Interceptor
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (latencyState.isBottleneckActive) RedDecline.copy(alpha = 0.2f)
                            else Color(0xFF00D2FF).copy(alpha = 0.15f)
                        )
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (latencyState.isBottleneckActive) "GARGALO > 500ms" else "LIMIAR: 500ms",
                        color = if (latencyState.isBottleneckActive) RedDecline else Color(0xFF00D2FF),
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Interceptor OkHttp ativo em 187.77.248.73:8080. Se ultrapassar 500ms, o JARVIS prioriza o motor local para decisão instantânea.",
                color = TextGray,
                fontSize = 10.5.sp,
                lineHeight = 14.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Banner de Alerta Crítico quando o gargalo estiver ativo
            AnimatedVisibility(
                visible = latencyState.isBottleneckActive || latencyState.isFallbackToLocalEngineActive,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(RedDecline.copy(alpha = 0.15f))
                        .border(1.dp, RedDecline.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Gargalo",
                            tint = RedDecline,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "ALERTA JARVIS: LENTIDÃO DE SERVIDOR (>500ms)",
                                color = RedDecline,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "Chaveamento automático ativado: corridas sendo analisadas pelo motor neural on-device (< 5ms).",
                                color = TextLight,
                                fontSize = 10.sp,
                                lineHeight = 13.sp
                            )
                        }
                    }
                }
            }

            if (latencyState.isBottleneckActive) {
                Spacer(modifier = Modifier.height(10.dp))
            }

            // 3. Grid de Telemetria (Último Ping, Média Móvel, Total de Chamadas)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Último Ping
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkCard)
                        .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            text = "ÚLTIMO PING",
                            color = TextGray,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (latencyState.lastLatencyMs > 0) "${latencyState.lastLatencyMs} ms" else "---",
                            color = statusColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Média Móvel (Últimas 10)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkCard)
                        .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            text = "MÉDIA MÓVEL",
                            color = TextGray,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (latencyState.averageLatencyMs > 0) "${latencyState.averageLatencyMs} ms" else "---",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Gargalos Prevenidos
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkCard)
                        .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            text = "GARGALOS",
                            color = TextGray,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${latencyState.bottleneckCount} alertas",
                            color = if (latencyState.bottleneckCount > 0) RedDecline else NeonGreen,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4. Detalhes do Endpoint e Histórico Recente
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Endpoint: ${latencyState.lastEndpoint.ifBlank { "/api/ping" }}",
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Status: ${latencyState.statusDescription}",
                    color = statusColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 5. Botões de Ação (Testar Ping e Liberar Fallback)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        isTestingPing = true
                        HapticFeedbackHelper.vibrateClick(context)
                        HostingerLatencyMonitor.testPing(context) { ms, ok ->
                            isTestingPing = false
                            if (ok) {
                                if (ms > 500L) {
                                    HapticFeedbackHelper.vibrateDecline(context)
                                    Toast.makeText(context, "⚠️ Ping Hostinger: ${ms}ms (GARGALO > 500ms)", Toast.LENGTH_SHORT).show()
                                } else {
                                    HapticFeedbackHelper.vibrateSuccess(context)
                                    Toast.makeText(context, "✓ Ping Hostinger: ${ms}ms (Rápido)", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                HapticFeedbackHelper.vibrateDecline(context)
                                Toast.makeText(context, "Falha de conexão com a VPS", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D2FF)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(42.dp)
                        .testTag("btn_test_hostinger_ping"),
                    enabled = !isTestingPing
                ) {
                    Icon(
                        imageVector = if (isTestingPing) Icons.Default.Refresh else Icons.Default.Bolt,
                        contentDescription = "Testar",
                        tint = DarkBg,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isTestingPing) "Testando..." else "⚡ Testar Ping VPS",
                        color = DarkBg,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                if (latencyState.isFallbackToLocalEngineActive) {
                    OutlinedButton(
                        onClick = {
                            HostingerLatencyMonitor.setFallbackActive(false)
                            HapticFeedbackHelper.vibrateClick(context)
                            Toast.makeText(context, "Fallback liberado - Conexão normal", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("btn_reset_latency_fallback")
                    ) {
                        Text(
                            text = "Liberar Fallback",
                            color = NeonGreen,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
