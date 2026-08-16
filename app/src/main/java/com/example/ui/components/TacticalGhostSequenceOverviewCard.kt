package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.coordinator.RadarCoordinator
import com.example.data.GhostSequenceBatchResult
import java.util.Locale

/**
 * Widget Tático para Visualização da Ghost Sequence em Ação e Análise Preditiva de Lotes Multi-App.
 * Fornece visão detalhada da economia gerada (tempo de espera em cozinhas, R$/km consolidado e paradas ordenadas).
 */
@Composable
fun TacticalGhostSequenceOverviewCard(
    modifier: Modifier = Modifier,
    onActivateGhostBatch: ((GhostSequenceBatchResult) -> Unit)? = null
) {
    val settings by RadarCoordinator.settings.collectAsStateWithLifecycle()
    val activeGhostBatch by com.example.data.GeminiOfferRepository.getInstance().activeGhostBatch.collectAsStateWithLifecycle()
    val geminiEvaluation by RadarCoordinator.latestGeminiEvaluation.collectAsStateWithLifecycle()

    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                1.dp,
                if (settings.isGhostSequenceEnabled) Color(0xFF00FF88).copy(alpha = 0.5f)
                else Color.White.copy(alpha = 0.15f),
                RoundedCornerShape(16.dp)
            )
            .testTag("tactical_ghost_sequence_overview_card"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D111A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Header: Status do Motor Ghost Sequence
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("👻", fontSize = 16.sp)
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "GHOST SEQUENCE BATCHING",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF00FF88),
                                letterSpacing = 1.sp
                            )
                            Surface(
                                color = Color(0x3300FF88),
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(0.5.dp, Color(0x8800FF88))
                            ) {
                                Text(
                                    text = settings.ghostSequenceAggressiveness,
                                    fontSize = 7.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00FF88),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = "Otimizador Topológico & Redução de Espera",
                            fontSize = 8.5.sp,
                            color = Color.LightGray
                        )
                    }
                }

                // Switch Rápido Ligar/Desligar Motor Ghost
                Switch(
                    checked = settings.isGhostSequenceEnabled,
                    onCheckedChange = { isEnabled ->
                        RadarCoordinator.updateSettings(settings.copy(isGhostSequenceEnabled = isEnabled))
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = Color(0xFF00FF88),
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = Color(0xFF1E2638)
                    ),
                    modifier = Modifier.scale(0.8f)
                )
            }

            if (!settings.isGhostSequenceEnabled) {
                // Estado Inativo
                Surface(
                    color = Color(0xFF161C2C),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                        Text(
                            text = "Ative a Ghost Sequence para agrupar automaticamente pedidos iFood + Rappi + 99 no mesmo trajeto.",
                            fontSize = 8.5.sp,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                // Estado Ativo: Exibe Detalhes do Lote Calculado
                val batch = activeGhostBatch

                if (batch != null) {
                    // Cartão Dourado/Verde do Lote Inteligente
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF131B2A))
                            .border(1.dp, Color(0xFF00F0FF).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("⚡", fontSize = 12.sp)
                                    Text(
                                        text = batch.appNamesFormatted,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF00F0FF)
                                    )
                                }

                                Surface(
                                    color = Color(0x3300FF88),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "${batch.synergyConfidencePct}% Sinergia",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF00FF88),
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            // Métricas Chave do Lote (Ganhos, KM, R$/km, Espera economizada)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("VALOR COMBINADO", fontSize = 7.5.sp, color = Color.Gray)
                                    Text(
                                        "R$ ${String.format(Locale.US, "%.2f", batch.totalEarnings)}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFF00FF88)
                                    )
                                }

                                Column {
                                    Text("TAXA / KM", fontSize = 7.5.sp, color = Color.Gray)
                                    Text(
                                        "R$ ${String.format(Locale.US, "%.2f", batch.gainPerKm)}/km",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }

                                Column {
                                    Text("TEMPO TOTAL", fontSize = 7.5.sp, color = Color.Gray)
                                    Text(
                                        "${batch.estimatedTimeMinutes} min",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFFB800)
                                    )
                                }

                                Column {
                                    Text("ESPERA REDUZIDA", fontSize = 7.5.sp, color = Color(0xFF00F0FF))
                                    Text(
                                        "-${batch.waitTimeSavedMinutes} min",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF00F0FF)
                                    )
                                }
                            }

                            // Botão para ver a sequência das paradas
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = { isExpanded = !isExpanded },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(
                                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = null,
                                            tint = Color.LightGray,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = if (isExpanded) "Ocultar paradas otimizadas" else "Ver ${batch.stopsInOptimizedOrder.size} paradas sequenciadas",
                                            fontSize = 8.5.sp,
                                            color = Color.LightGray
                                        )
                                    }
                                }

                                Button(
                                    onClick = { onActivateGhostBatch?.invoke(batch) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF88)),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text(
                                        text = "ACEITAR LOTE",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.Black
                                    )
                                }
                            }

                            // Detalhamento Expandido das Paradas
                            AnimatedVisibility(visible = isExpanded) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                                    batch.stopsInOptimizedOrder.forEachIndexed { index, stop ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Surface(
                                                color = if (stop.priority <= 2) Color(0xFF00FF88).copy(alpha = 0.2f) else Color(0xFF00F0FF).copy(alpha = 0.2f),
                                                shape = CircleShape,
                                                modifier = Modifier.size(16.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = "${index + 1}",
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White
                                                    )
                                                }
                                            }
                                            Text(
                                                text = if (stop.priority <= 2) "📍 [COLETA]" else "🏁 [ENTREGA]",
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (stop.priority <= 2) Color(0xFF00FF88) else Color(0xFF00F0FF)
                                            )
                                            Text(
                                                text = stop.address,
                                                fontSize = 8.sp,
                                                color = Color.LightGray,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Sem lote no momento (Motor varrendo pedidos em segundo plano)
                    Surface(
                        color = Color(0xFF161C2C),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                color = Color(0xFF00FF88),
                                modifier = Modifier.size(16.dp)
                            )
                            Column {
                                Text(
                                    text = "Motor Ghost em Varredura de Corredor Viário",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Monitorando coletas adjacentes no iFood, Rappi e 99 num raio de 3.8 km...",
                                    fontSize = 7.5.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
