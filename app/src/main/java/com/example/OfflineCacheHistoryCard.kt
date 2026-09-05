package com.example

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.radar.data.CachedOfferEntity
import com.example.radar.data.CachedRouteEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * PAINEL DE CACHE LOCAL ROOM & HISTÓRICO OFFLINE
 *
 * Permite ao entregador consultar rotas e ofertas recentes salvas no banco Room SQLite,
 * assegurando operação contínua em túneis, subsolos, elevadores e áreas de sombra 4G/5G.
 */
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun OfflineCacheHistoryCard(
    cachedOffers: List<CachedOfferEntity> = emptyList(),
    cachedRoutes: List<CachedRouteEntity> = emptyList(),
    isOfflineModeSimulated: Boolean = false,
    onToggleOfflineMode: () -> Unit = {},
    onClearOldCache: () -> Unit = {},
    onSelectRouteForNavigation: (CachedRouteEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (isOfflineModeSimulated) Color(0xFFF7C200).copy(alpha = 0.6f) else NeonGreen.copy(alpha = 0.4f),
                RoundedCornerShape(22.dp)
            )
            .testTag("offline_cache_history_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 1. Cabeçalho com Status do Room Cache
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isOfflineModeSimulated) Color(0xFFF7C200) else NeonGreen)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "CACHE LOCAL ROOM",
                                color = TextLight,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.8.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (isOfflineModeSimulated) Color(0xFFF7C200).copy(alpha = 0.2f)
                                        else NeonGreen.copy(alpha = 0.15f)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isOfflineModeSimulated) "MODO OFFLINE ATIVO" else "SQLITE 100% ONLINE",
                                    color = if (isOfflineModeSimulated) Color(0xFFF7C200) else NeonGreen,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = "Histórico e rotas persistidas localmente sem sinal",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }

                // Botão de expandir / recolher
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(DarkCardElevated)
                        .clickable { isExpanded = !isExpanded },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Recolher" else "Expandir",
                        tint = NeonGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Barra de Métricas de Armazenamento Local Room
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkCardElevated)
                        .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Column {
                        Text("OFERTAS SALVAS", color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "${cachedOffers.size} itens",
                            color = NeonGreen,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkCardElevated)
                        .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Column {
                        Text("ROTAS RECENTES", color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "${cachedRoutes.size} trajetos",
                            color = Color(0xFF00D1FF),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkCardElevated)
                        .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Column {
                        Text("ESTADO DE REDE", color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Text(
                            if (isOfflineModeSimulated) "Sem Sinal" else "Sincronizado",
                            color = if (isOfflineModeSimulated) Color(0xFFF7C200) else TextLight,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Botão para Simular / Testar Perda de Sinal
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onToggleOfflineMode,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isOfflineModeSimulated) Color(0xFFF7C200) else TextLight
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isOfflineModeSimulated) Color(0xFFF7C200) else DarkBorder
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .testTag("btn_toggle_offline_mode")
                ) {
                    Text(
                        text = if (isOfflineModeSimulated) "📶 Restaurar Conexão" else "📡 Testar Modo Offline",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = { isExpanded = !isExpanded },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = NeonGreen
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .testTag("btn_view_room_cache")
                ) {
                    Text(
                        text = if (isExpanded) "Ocultar Dados" else "Ver Cache (${cachedOffers.size + cachedRoutes.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 4. Seção Expansível com as Abas de Ofertas e Rotas
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    TabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = DarkCardElevated,
                        contentColor = NeonGreen,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                color = NeonGreen
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                    ) {
                        Tab(
                            selected = selectedTabIndex == 0,
                            onClick = { selectedTabIndex = 0 },
                            text = {
                                Text(
                                    "📦 OFERTAS (${cachedOffers.size})",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        )
                        Tab(
                            selected = selectedTabIndex == 1,
                            onClick = { selectedTabIndex = 1 },
                            text = {
                                Text(
                                    "🗺️ ROTAS (${cachedRoutes.size})",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (selectedTabIndex == 0) {
                        // Lista de Ofertas Cacheadas no Room
                        if (cachedOffers.isEmpty()) {
                            Text(
                                text = "Nenhuma oferta armazenada no cache local.",
                                color = TextMuted,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                cachedOffers.take(6).forEach { offer ->
                                    CachedOfferItemRow(offer = offer, timeStr = timeFormatter.format(Date(offer.timestamp)))
                                }
                            }
                        }
                    } else {
                        // Lista de Rotas Cacheadas no Room
                        if (cachedRoutes.isEmpty()) {
                            Text(
                                text = "Nenhuma rota salva no cache offline.",
                                color = TextMuted,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                cachedRoutes.take(6).forEach { route ->
                                    CachedRouteItemRow(
                                        route = route,
                                        timeStr = timeFormatter.format(Date(route.completedAt)),
                                        onSelect = { onSelectRouteForNavigation(route) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Linha que exibe uma oferta persistida no Room SQLite
 */
@Composable
fun CachedOfferItemRow(
    offer: CachedOfferEntity,
    timeStr: String
) {
    val appColor = when {
        offer.appName.contains("iFood", ignoreCase = true) -> RedIFood
        offer.appName.contains("Rappi", ignoreCase = true) -> OrangeRappi
        offer.appName.contains("Uber", ignoreCase = true) -> Color(0xFFE0E0E0)
        offer.appName.contains("99", ignoreCase = true) -> Yellow99
        else -> NeonGreen
    }

    val statusColor = when (offer.status) {
        "ACCEPTED" -> NeonGreen
        "COMPLETED" -> Color(0xFF00D1FF)
        "DECLINED" -> RedDecline
        else -> Color(0xFFF7C200)
    }

    val statusText = when (offer.status) {
        "ACCEPTED" -> "ACEITA"
        "COMPLETED" -> "CONCLUÍDA"
        "DECLINED" -> "RECUSADA"
        else -> "PENDENTE"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DarkCardElevated)
            .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(appColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = offer.appName,
                        color = appColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "• $timeStr",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = offer.restaurant,
                        color = TextLight,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = offer.deliveryAddress.ifEmpty { "Destino salvo no cache" },
                        color = TextMuted,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = String.format(Locale.GERMANY, "R$ %.2f", offer.value),
                        color = NeonGreen,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "${String.format(Locale.GERMANY, "%.1f km", offer.distanceKm)} • ${String.format(Locale.GERMANY, "R$ %.2f/km", offer.gainPerKm)}",
                        color = TextMuted,
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}

/**
 * Linha que exibe uma rota recente persistida no Room SQLite
 */
@Composable
fun CachedRouteItemRow(
    route: CachedRouteEntity,
    timeStr: String,
    onSelect: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DarkCardElevated)
            .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
            .clickable { onSelect() }
            .padding(10.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "📍 ${route.appName}",
                        color = TextLight,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "• $timeStr",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }

                Text(
                    text = "${String.format(Locale.GERMANY, "%.1f km", route.totalDistanceKm)} • ${route.estimatedMinutes} min",
                    color = Color(0xFF00D1FF),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = route.waypointsSummary,
                color = TextLight.copy(alpha = 0.9f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "➔ Destino: ${route.destinationAddress}",
                color = TextMuted,
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
