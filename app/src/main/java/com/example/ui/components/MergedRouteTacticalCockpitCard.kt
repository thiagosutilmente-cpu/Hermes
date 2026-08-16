package com.example.ui.components

import android.content.Context
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.manager.MergedDeliveryDispatcher
import com.example.model.ActiveMergedRouteState
import com.example.model.MergedDeliveryStop
import com.example.model.StopActionType
import com.example.model.StopExecutionStatus
import java.util.Locale

/**
 * 🚀 Central Tática de Entregas Mescladas & Guia Passo a Passo (Merged Route Action Cockpit).
 *
 * Oferece extrema clareza e praticidade para o entregador:
 * - Exibe claramente a ETAPA ATUAL: Qual restaurante/endereço ir agora, qual aplicativo pertence, o código do pedido e o que retirar.
 * - Botão de 1 Toque "ABRIR NO WAZE/MAPS" para navegação instantânea.
 * - Botão de Ação Tática: "CONFIRMAR COLETA" ou "CONFIRMAR ENTREGA" que avança a rota e já traça o próximo ponto.
 * - Dicas e notas do cliente em destaque com alta legibilidade ao pilotar a moto.
 * - Visão geral das próximas paradas ordenadas estrategicamente.
 */
@Composable
fun MergedRouteTacticalCockpitCard(
    modifier: Modifier = Modifier,
    onNavigateNext: ((MergedDeliveryStop) -> Unit)? = null
) {
    val context = LocalContext.current
    val activeRouteState by MergedDeliveryDispatcher.activeRoute.collectAsStateWithLifecycle()
    var isExpandedList by remember { mutableStateOf(false) }

    val route = activeRouteState

    if (route == null) return

    val currentStop = route.currentStop
    val isCompleted = route.completedStopsCount >= route.stops.size

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(
                1.5.dp,
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF00FF88),
                        Color(0xFF00F0FF),
                        Color(0xFFFFB800)
                    )
                ),
                RoundedCornerShape(18.dp)
            )
            .testTag("merged_route_tactical_cockpit_card"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B101B))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // --- HEADER DA ROTA MESCLADA ---
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
                            .background(
                                Brush.linearGradient(listOf(Color(0xFF00FF88), Color(0xFF00B4D8)))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🏍️", fontSize = 16.sp)
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "ROTA MESCLADA ATIVA",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF00FF88),
                                letterSpacing = 1.sp
                            )
                            Surface(
                                color = if (route.isRouteActive) Color(0x3300FF88) else Color(0x33FFB800),
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(0.5.dp, if (route.isRouteActive) Color(0xFF00FF88) else Color(0xFFFFB800))
                            ) {
                                Text(
                                    text = if (route.isRouteActive) "EM ANDAMENTO" else "PRONTA P/ INICIAR",
                                    fontSize = 7.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (route.isRouteActive) Color(0xFF00FF88) else Color(0xFFFFB800),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = route.title,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Ganhos totais e progresso
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "R$ ${String.format(Locale.US, "%.2f", route.totalEarnings)}",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF00FF88)
                    )
                    Text(
                        text = "Etapa ${route.currentStopIndex + 1} de ${route.stops.size}",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.LightGray
                    )
                }
            }

            // Barra de Progresso do Lote & Indicador de Cache Offline Room
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LinearProgressIndicator(
                    progress = { route.progressPct },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = Color(0xFF00FF88),
                    trackColor = Color(0xFF1E273A)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            color = Color(0x2200FF88),
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(0.5.dp, Color(0xFF00FF88).copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text("💾", fontSize = 8.sp)
                                Text(
                                    text = "OFFLINE READY (CACHE ROOM SQLite)",
                                    fontSize = 7.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00FF88)
                                )
                            }
                        }
                    }

                    Text(
                        text = "Economia: ${route.waitTimeSavedMinutes} min",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF00F0FF)
                    )
                }
            }

            // --- DESTAQUE DA ETAPA ATUAL (O QUE O ENTREGADOR DEVE FAZER AGORA) ---
            if (!isCompleted && currentStop != null) {
                val isPickup = currentStop.actionType == StopActionType.PICKUP
                val appColor = when (currentStop.appName.lowercase()) {
                    "ifood" -> Color(0xFFEA1D2C)
                    "rappi" -> Color(0xFFFF441F)
                    "99" -> Color(0xFFF7C200)
                    else -> Color(0xFF00F0FF)
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF141C2E)),
                    border = BorderStroke(1.dp, appColor.copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Badge da Ação Imediata & App de Origem
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(
                                    color = if (isPickup) Color(0x3300FF88) else Color(0x3300F0FF),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(if (isPickup) "📍" else "🏁", fontSize = 10.sp)
                                        Text(
                                            text = if (isPickup) "PASSO 1: COLETAR" else "PASSO 2: ENTREGAR",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (isPickup) Color(0xFF00FF88) else Color(0xFF00F0FF)
                                        )
                                    }
                                }

                                Surface(
                                    color = appColor.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(0.5.dp, appColor)
                                ) {
                                    Text(
                                        text = currentStop.appName.uppercase(),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (currentStop.appName.lowercase() == "99") Color.Yellow else Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            if (currentStop.orderCode.isNotEmpty()) {
                                Surface(
                                    color = Color(0xFF222C42),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "PEDIDO: ${currentStop.orderCode}",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFFFFB800),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        // Nome do Estabelecimento / Cliente e Endereço
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = currentStop.establishmentOrCustomer,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                text = currentStop.fullAddress,
                                fontSize = 10.5.sp,
                                color = Color(0xFFB0C0E0),
                                lineHeight = 14.sp
                            )
                        }

                        // Resumo dos Itens e Instruções do Cliente / Coleta
                        if (currentStop.itemsSummary.isNotEmpty() || currentStop.customerNotes.isNotEmpty()) {
                            Surface(
                                color = Color(0xFF0D1322),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    if (currentStop.itemsSummary.isNotEmpty()) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text("📦", fontSize = 10.sp)
                                            Text(
                                                text = currentStop.itemsSummary,
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                    if (currentStop.customerNotes.isNotEmpty()) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text("💬", fontSize = 10.sp)
                                            Text(
                                                text = "Nota: ${currentStop.customerNotes}",
                                                fontSize = 8.5.sp,
                                                color = Color(0xFFFFCC00)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Orientação Tática do Jarvis para o Piloto
                        if (currentStop.tacticalGuidance.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("💡", fontSize = 11.sp)
                                Text(
                                    text = currentStop.tacticalGuidance,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF00FF88)
                                )
                            }
                        }

                        // --- BOTÕES DE AÇÃO RÁPIDA (PILOTAGEM) ---
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Botão 1: Abrir no Waze / Maps
                            Button(
                                onClick = {
                                    MergedDeliveryDispatcher.launchNavigation(
                                        context,
                                        currentStop.fullAddress,
                                        currentStop.latitude,
                                        currentStop.longitude
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Navigation, contentDescription = null, tint = Color(0xFF00F0FF), modifier = Modifier.size(16.dp))
                                    Text(
                                        text = "ABRIR WAZE",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                }
                            }

                            // Botão 2: Confirmar Ação (Avançar Próxima Parada)
                            Button(
                                onClick = {
                                    MergedDeliveryDispatcher.completeCurrentStopAndAdvance(context)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isPickup) Color(0xFF00FF88) else Color(0xFF00D4FF)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(38.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isPickup) Icons.Default.CheckCircle else Icons.Default.DoneAll,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (isPickup) "CONFIRMAR COLETA" else "CONFIRMAR ENTREGA",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.Black
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Todas as paradas concluídas
                Surface(
                    color = Color(0x3300FF88),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF00FF88)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🏆", fontSize = 20.sp)
                        Column {
                            Text(
                                text = "LOTE 100% FINALIZADO COM SUCESSO!",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF00FF88)
                            )
                            Text(
                                text = "Faturamento consolidado: R$ ${String.format(Locale.US, "%.2f", route.totalEarnings)}. Rastreamento preditivo pronto para o próximo lote.",
                                fontSize = 8.5.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // --- ITINERÁRIO COMPLETO DAS PARADAS (EXPANSÍVEL) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpandedList = !isExpandedList }
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        imageVector = if (isExpandedList) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = Color.LightGray,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (isExpandedList) "Ocultar sequência de paradas" else "Ver roteiro ordenado (${route.stops.size} paradas)",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.LightGray
                    )
                }

                Text(
                    text = "Economia: ${route.waitTimeSavedMinutes} min",
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00FF88)
                )
            }

            AnimatedVisibility(visible = isExpandedList) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                    route.stops.forEachIndexed { index, stop ->
                        val isCurrent = index == route.currentStopIndex
                        val isDone = stop.status == StopExecutionStatus.COMPLETED
                        val isPickup = stop.actionType == StopActionType.PICKUP

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when {
                                        isCurrent -> Color(0xFF1E2A42)
                                        isDone -> Color(0xFF0F1522)
                                        else -> Color(0xFF121724)
                                    }
                                )
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Indicador numérico
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isDone -> Color(0xFF00FF88)
                                            isCurrent -> Color(0xFF00F0FF)
                                            else -> Color.DarkGray
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isDone) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(12.dp))
                                } else {
                                    Text(
                                        text = "${index + 1}",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCurrent) Color.Black else Color.White
                                    )
                                }
                            }

                            // Conteúdo da parada
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = if (isPickup) "📍 COLETA" else "🏁 ENTREGA",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (isPickup) Color(0xFF00FF88) else Color(0xFF00F0FF)
                                    )
                                    Text(
                                        text = "• ${stop.appName}",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.LightGray
                                    )
                                }
                                Text(
                                    text = stop.establishmentOrCustomer,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDone) Color.Gray else Color.White,
                                    maxLines = 1
                                )
                                Text(
                                    text = stop.fullAddress,
                                    fontSize = 7.5.sp,
                                    color = Color.Gray,
                                    maxLines = 1
                                )
                            }

                            // Botão de navegação individual
                            IconButton(
                                onClick = {
                                    MergedDeliveryDispatcher.launchNavigation(
                                        context,
                                        stop.fullAddress,
                                        stop.latitude,
                                        stop.longitude
                                    )
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Directions,
                                    contentDescription = "Traçar Rota",
                                    tint = if (isCurrent) Color(0xFF00FF88) else Color.LightGray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
