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
import androidx.compose.ui.draw.shadow
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

/**
 * Componente: Lista de Sequenciamento de Entregas (Simplified Delivery Sequence List).
 *
 * Visualiza de forma simplificada e ultra legível a ordem otimizada das paradas da rota mesclada.
 * Destaques visuais:
 * - Ícones de alto contraste diferenciando COLETA (🛍️ / Verde Neon) e ENTREGA (🏠 / Ciano Neon).
 * - Linha de metrô contínua indicando a progressão exata do percurso.
 * - Chips de filtro rápido (Todas, Coletas, Entregas).
 * - Botão de navegação tática rápida por parada.
 */
@Composable
fun SimplifiedDeliverySequenceListCard(
    modifier: Modifier = Modifier,
    onStopSelected: ((MergedDeliveryStop) -> Unit)? = null
) {
    val context = LocalContext.current
    val activeRouteState by MergedDeliveryDispatcher.activeRoute.collectAsStateWithLifecycle()

    var filterType by remember { mutableStateOf<StopActionType?>(null) } // null = todas

    val route = activeRouteState ?: return
    val stops = route.stops

    val pickupCount = stops.count { it.actionType == StopActionType.PICKUP }
    val deliveryCount = stops.count { it.actionType == StopActionType.DELIVERY }

    val filteredStops = when (filterType) {
        StopActionType.PICKUP -> stops.filter { it.actionType == StopActionType.PICKUP }
        StopActionType.DELIVERY -> stops.filter { it.actionType == StopActionType.DELIVERY }
        null -> stops
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                1.dp,
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF00FF88).copy(alpha = 0.6f),
                        Color(0xFF00F0FF).copy(alpha = 0.6f)
                    )
                ),
                RoundedCornerShape(16.dp)
            )
            .testTag("simplified_delivery_sequence_list_card"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0F19))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // HEADER DO SEQUENCIAMENTO
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
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF162032)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🗺️", fontSize = 15.sp)
                    }

                    Column {
                        Text(
                            text = "SEQUENCIAMENTO OTIMIZADO",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF00FF88),
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${stops.size} Paradas em Sequência Inteligente",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Resumo rápido de coletas vs entregas
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        color = Color(0x3300FF88),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(0.5.dp, Color(0xFF00FF88))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text("🛍️", fontSize = 9.sp)
                            Text("$pickupCount Coletas", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00FF88))
                        }
                    }

                    Surface(
                        color = Color(0x3300F0FF),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(0.5.dp, Color(0xFF00F0FF))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text("🏠", fontSize = 9.sp)
                            Text("$deliveryCount Entregas", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00F0FF))
                        }
                    }
                }
            }

            // CHIPS DE FILTRO SIMPLES & BOTÃO DE AÇÃO RÁPIDA 'LEVA-ME LÁ'
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterPill(
                        label = "Todas (${stops.size})",
                        isSelected = filterType == null,
                        activeColor = Color.White,
                        onClick = { filterType = null }
                    )
                    FilterPill(
                        label = "🛍️ Coletas ($pickupCount)",
                        isSelected = filterType == StopActionType.PICKUP,
                        activeColor = Color(0xFF00FF88),
                        onClick = { filterType = StopActionType.PICKUP }
                    )
                    FilterPill(
                        label = "🏠 Entregas ($deliveryCount)",
                        isSelected = filterType == StopActionType.DELIVERY,
                        activeColor = Color(0xFF00F0FF),
                        onClick = { filterType = StopActionType.DELIVERY }
                    )
                }
            }

            // BOTÃO PRINCIPAL TÁTICO 'LEVA-ME LÁ' (NAVEGAÇÃO RÁPIDA DE ROTA OTIMIZADA)
            val targetStop = route.currentStop ?: stops.firstOrNull()
            if (targetStop != null) {
                Button(
                    onClick = {
                        val stop = route.currentStop ?: stops.firstOrNull() ?: return@Button
                        if (!route.isRouteActive) {
                            MergedDeliveryDispatcher.activateCurrentRoute(context)
                        } else {
                            MergedDeliveryDispatcher.launchNavigation(
                                context,
                                stop.fullAddress,
                                stop.latitude,
                                stop.longitude
                            )
                        }
                        val actionName = if (stop.actionType == StopActionType.PICKUP) "coleta no" else "entrega para"
                        val voiceMsg = "Leva-me lá ativado! Navegando para $actionName ${stop.establishmentOrCustomer}."
                        com.example.coordinator.RadarCoordinator.voiceManager?.speak(voiceMsg)
                        com.example.util.ToastUtils.showToast(context, "🧭 Iniciando navegação: ${stop.establishmentOrCustomer}", android.widget.Toast.LENGTH_SHORT)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_take_me_there"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFF00FF88),
                                        Color(0xFF00D4FF)
                                    )
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Navigation,
                                contentDescription = "Navegar Rota",
                                tint = Color(0xFF0A0F19),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "LEVA-ME LÁ (1º DESTINO)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0A0F19),
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "• ${targetStop.appName}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0A0F19).copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

            // LISTA DE PARADAS COM TIMELINE VISUAL
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filteredStops.forEachIndexed { index, stop ->
                    val originalIndex = stops.indexOf(stop)
                    val isCurrent = originalIndex == route.currentStopIndex
                    val isCompleted = stop.status == StopExecutionStatus.COMPLETED
                    val isLast = index == filteredStops.size - 1

                    SequenceStopItem(
                        stop = stop,
                        stepNumber = originalIndex + 1,
                        isCurrent = isCurrent,
                        isCompleted = isCompleted,
                        isLast = isLast,
                        onNavigate = {
                            MergedDeliveryDispatcher.launchNavigation(
                                context,
                                stop.fullAddress,
                                stop.latitude,
                                stop.longitude
                            )
                        },
                        onClick = {
                            onStopSelected?.invoke(stop)
                        }
                    )
                }
            }
        }
    }
}

/**
 * Chip de Filtro de Paradas
 */
@Composable
private fun FilterPill(
    label: String,
    isSelected: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) activeColor.copy(alpha = 0.2f) else Color(0xFF131A29),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            1.dp,
            if (isSelected) activeColor else Color.White.copy(alpha = 0.1f)
        ),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            fontSize = 8.5.sp,
            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
            color = if (isSelected) activeColor else Color.LightGray,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/**
 * Item individual da linha do tempo da parada com destaque simplificado
 */
@Composable
private fun SequenceStopItem(
    stop: MergedDeliveryStop,
    stepNumber: Int,
    isCurrent: Boolean,
    isCompleted: Boolean,
    isLast: Boolean,
    onNavigate: () -> Unit,
    onClick: () -> Unit
) {
    val isPickup = stop.actionType == StopActionType.PICKUP

    val actionColor = if (isPickup) Color(0xFF00FF88) else Color(0xFF00F0FF)
    val appColor = when (stop.appName.lowercase()) {
        "ifood" -> Color(0xFFEA1D2C)
        "rappi" -> Color(0xFFFF441F)
        "99" -> Color(0xFFF7C200)
        else -> Color(0xFF00F0FF)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                when {
                    isCurrent -> Color(0xFF182236)
                    isCompleted -> Color(0xFF0C111C)
                    else -> Color(0xFF111724)
                }
            )
            .border(
                width = if (isCurrent) 1.5.dp else 0.5.dp,
                color = when {
                    isCurrent -> actionColor
                    isCompleted -> Color.White.copy(alpha = 0.05f)
                    else -> Color.White.copy(alpha = 0.1f)
                },
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ÍCONE DE AÇÃO & NÚMERO DA ETAPA
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isCompleted -> Color(0xFF1A2A20)
                            isCurrent -> actionColor.copy(alpha = 0.25f)
                            else -> Color(0xFF1B2333)
                        }
                    )
                    .border(
                        1.5.dp,
                        when {
                            isCompleted -> Color(0xFF00FF88)
                            isCurrent -> actionColor
                            else -> Color.DarkGray
                        },
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Concluído",
                        tint = Color(0xFF00FF88),
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Text(
                        text = if (isPickup) "🛍️" else "🏠",
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Passo $stepNumber",
                fontSize = 7.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (isCurrent) actionColor else Color.Gray
            )
        }

        // DETALHES DA PARADA (ESTABELECIMENTO / CLIENTE, ENDEREÇO, CÓDIGO)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Linha com Badges
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                // Badge Tipo de Ação
                Surface(
                    color = actionColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = if (isPickup) "COLETA" else "ENTREGA",
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Black,
                        color = actionColor,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }

                // Badge do App
                Surface(
                    color = appColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(0.5.dp, appColor.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = stop.appName.uppercase(),
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Black,
                        color = if (stop.appName.lowercase() == "99") Color.Yellow else Color.White,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }

                // Badge de Pedido / Código
                if (stop.orderCode.isNotEmpty()) {
                    Text(
                        text = stop.orderCode,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFFFB800)
                    )
                }

                if (isCurrent) {
                    Surface(
                        color = Color(0xFF00FF88),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "ATUAL",
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.Black,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            // Nome do Local
            Text(
                text = stop.establishmentOrCustomer,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Black,
                color = if (isCompleted) Color.Gray else Color.White,
                maxLines = 1
            )

            // Endereço Resumido
            Text(
                text = stop.fullAddress,
                fontSize = 8.5.sp,
                color = if (isCompleted) Color.DarkGray else Color(0xFFA0B0CC),
                maxLines = 1
            )
        }

        // BOTÃO DE NAVEGAÇÃO DIRETO (WAZE / MAPS)
        IconButton(
            onClick = onNavigate,
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(if (isCurrent) Color(0xFF1E2D47) else Color(0xFF141A26))
                .border(
                    0.5.dp,
                    if (isCurrent) Color(0xFF00F0FF) else Color.White.copy(alpha = 0.1f),
                    CircleShape
                )
        ) {
            Icon(
                imageVector = Icons.Default.Directions,
                contentDescription = "Navegar para este ponto",
                tint = if (isCurrent) Color(0xFF00F0FF) else Color.LightGray,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
