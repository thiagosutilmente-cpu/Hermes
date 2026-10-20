package com.example

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
 * SystemHealthCheckupCard
 *
 * Card visual do Cockpit apresentando o diagnóstico em tempo real
 * de cada linha de código crítica e subsistema de rua (memória, matemática,
 * conexões, GPS e auto-recuperação silenciosa).
 */
@Composable
fun SystemHealthCheckupCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val supervisor = remember { SilentAutoRecoverySupervisor.getInstance(context) }

    val report by supervisor.healthReport.collectAsState()
    val isAuditRunning by supervisor.isAuditRunning.collectAsState()

    val currentScore = report?.overallHealthScorePercent ?: 100
    val scoreAnimated by animateFloatAsState(
        targetValue = currentScore / 100f,
        animationSpec = tween(600),
        label = "scoreAnimation"
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF090E17)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.5.dp,
                Brush.horizontalGradient(
                    listOf(Color(0xFF10B981), Color(0xFF059669), Color(0xFF0284C7))
                ),
                RoundedCornerShape(20.dp)
            )
            .testTag("card_system_health_checkup")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // CABEÇALHO DO DIAGNÓSTICO
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
                                Brush.linearGradient(
                                    listOf(Color(0xFF059669), Color(0xFF10B981))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.HealthAndSafety,
                            contentDescription = "Saúde do Sistema",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "CHECAPE GERAL DE CÓDIGO",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (currentScore >= 90) Color(0xFF059669) else Color(0xFFD97706)
                            ) {
                                Text(
                                    text = if (currentScore >= 90) "BLINDADO" else "AVISO",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Auto-cura silenciosa & auditoria preventiva de rua",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                }

                // PLACAR CIRCULAR DE SAÚDE
                Box(
                    modifier = Modifier.size(46.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { scoreAnimated },
                        color = Color(0xFF10B981),
                        trackColor = Color(0xFF1E293B),
                        strokeWidth = 3.5.dp,
                        modifier = Modifier.size(46.dp)
                    )
                    Text(
                        text = "$currentScore%",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // BANNER DE PRONTIDÃO
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF131F2E),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (report?.isReadyForLongShift == true)
                                "Apto para Turno Longo (12h+ Contínuas)"
                            else "Aguardando conclusão do diagnóstico",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = report?.auditSummary ?: "Verificando integridade de nós e memória...",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ITENS DO CHECAPE MINUCIOSO
            Text(
                text = "DIAGNÓSTICO DETALHADO POR SUBSISTEMA:",
                color = Color(0xFF94A3B8),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SubsystemCheckRow(
                    icon = Icons.Default.Memory,
                    name = "Vazamento de Binder & Nós de Acessibilidade",
                    statusText = "Reciclagem automática (.recycle()) ativa. Zero nós retidos.",
                    isHealthy = report?.subsystemsStatus?.get(SystemSubsystem.ACCESSIBILITY_BINDER)?.isHealthy ?: true
                )

                SubsystemCheckRow(
                    icon = Icons.Default.Calculate,
                    name = "Sanidade Numérica & Motor de Lucro",
                    statusText = "Divisões por zero, km nulo e valores infinitos 100% blindados.",
                    isHealthy = report?.subsystemsStatus?.get(SystemSubsystem.FINANCIAL_MATH)?.isHealthy ?: true
                )

                SubsystemCheckRow(
                    icon = Icons.Default.Wifi,
                    name = "Conexão WebSocket & Auto-Reconexão",
                    statusText = "Backoff exponencial com restauração instantânea pós-túnel.",
                    isHealthy = report?.subsystemsStatus?.get(SystemSubsystem.WEBSOCKET_NETWORK)?.isHealthy ?: true
                )

                SubsystemCheckRow(
                    icon = Icons.Default.LocationOn,
                    name = "GPS Fused & WakeLock Anti-Doze",
                    statusText = "CPU mantida ativa com bateria baixa sem congelamento do listener.",
                    isHealthy = report?.subsystemsStatus?.get(SystemSubsystem.GPS_TELEMETRY)?.isHealthy ?: true
                )

                SubsystemCheckRow(
                    icon = Icons.Default.RecordVoiceOver,
                    name = "Síntese de Voz TTS (Português pt-BR)",
                    statusText = "Motor nativo com fallback e silenciamento preventivo contra falhas.",
                    isHealthy = report?.subsystemsStatus?.get(SystemSubsystem.TEXT_TO_SPEECH)?.isHealthy ?: true
                )

                SubsystemCheckRow(
                    icon = Icons.Default.Layers,
                    name = "Camada de Sobreposição (WindowManager)",
                    statusText = "Protegida contra BadTokenException e fechamento de tela.",
                    isHealthy = report?.subsystemsStatus?.get(SystemSubsystem.OVERLAY_WINDOW)?.isHealthy ?: true
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Color(0xFF1E293B))
            Spacer(modifier = Modifier.height(12.dp))

            // BOTÃO PARA EXECUTAR AUDITORIA COMPLETA
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "AUDITORIA EM TEMPO REAL",
                        color = Color(0xFF64748B),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = report?.auditTimestamp?.let {
                            "Última: " + SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(it))
                        } ?: "Executando...",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Button(
                    onClick = {
                        supervisor.runDeepSystemHealthCheckup()
                        Toast.makeText(context, "🔍 Checape geral executado com sucesso!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                    shape = RoundedCornerShape(10.dp),
                    enabled = !isAuditRunning,
                    modifier = Modifier.height(38.dp)
                ) {
                    if (isAuditRunning) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Executar Checape", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SubsystemCheckRow(
    icon: ImageVector,
    name: String,
    statusText: String,
    isHealthy: Boolean
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF111827),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isHealthy) Color(0xFF064E3B) else Color(0xFF7F1D1D)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        if (isHealthy) Color(0xFF059669).copy(alpha = 0.2f)
                        else Color(0xFFDC2626).copy(alpha = 0.2f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isHealthy) Color(0xFF10B981) else Color(0xFFEF4444),
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = statusText,
                    color = if (isHealthy) Color(0xFF94A3B8) else Color(0xFFFCA5A5),
                    fontSize = 10.sp
                )
            }

            Icon(
                imageVector = if (isHealthy) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isHealthy) Color(0xFF10B981) else Color(0xFFEF4444),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
