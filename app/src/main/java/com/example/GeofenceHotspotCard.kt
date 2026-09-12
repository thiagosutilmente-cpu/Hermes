package com.example

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

/**
 * Card Jetpack Compose exibindo o status do Geofencing e detecção de Zonas de Alta Demanda.
 *
 * Exibe:
 * - Indicador ativo com pulso de radar quando o entregador entra em um polo gastronômico/comercial.
 * - Taxa de chamadas/hora e estimativa de tarifa dinâmica (+30% a +50%).
 * - Lista expansível de todas as zonas monitoradas com distância em tempo real.
 * - Ferramenta de simulação para testar o comportamento de entrada/saída em ambientes de desenvolvimento.
 */
@Composable
fun GeofenceHotspotCard(
    activeZone: HighDemandZone?,
    registeredZones: List<HighDemandZone>,
    userLatitude: Double,
    userLongitude: Double,
    isGeofencingActive: Boolean,
    onSimulateEnterZone: (String) -> Unit,
    onSimulateExitZone: () -> Unit,
    onNavigateToZone: (HighDemandZone) -> Unit
) {
    var isListExpanded by remember { mutableStateOf(false) }

    val pulseTransition = rememberInfiniteTransition(label = "hotspotPulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val borderColor by animateColorAsState(
        targetValue = if (activeZone != null) Color(0xFFFF9F1C) else NeonGreen.copy(alpha = 0.4f),
        label = "borderColor"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("geofence_hotspot_card")
            .clip(RoundedCornerShape(16.dp))
            .border(1.5.dp, borderColor, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = DarkCard
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Cabeçalho do Card
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .scale(if (activeZone != null) pulseScale else 1f)
                            .clip(CircleShape)
                            .background(
                                if (activeZone != null) Color(0x33FF9F1C) else Color(0x2200FF88)
                            )
                            .border(
                                1.5.dp,
                                if (activeZone != null) Color(0xFFFF9F1C) else NeonGreen,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (activeZone != null) "🔥" else "📡",
                            fontSize = 18.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "GEOFENCING • POLOS DE DEMANDA",
                                color = TextLight,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Text(
                            text = if (activeZone != null) "Você está dentro de um hotspot!" else "Monitorando 5 polos gastronômicos em SP",
                            color = if (activeZone != null) Color(0xFFFF9F1C) else TextMuted,
                            fontSize = 11.sp,
                            fontWeight = if (activeZone != null) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                // Badge de Status do Geofence
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (activeZone != null) Color(0x33FF9F1C) else Color(0x2200FF88))
                        .border(1.dp, if (activeZone != null) Color(0xFFFF9F1C) else NeonGreen.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (activeZone != null) "EM ZONA QUENTE" else "GEOFENCE ATIVO",
                        color = if (activeZone != null) Color(0xFFFF9F1C) else NeonGreen,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // DESTAQUE DA ZONA ATIVA (SE DENTRO DO GEOFENCE)
            if (activeZone != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF261905), Color(0xFF1B130E))
                            )
                        )
                        .border(1.5.dp, Color(0xFFFF9F1C).copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = activeZone.name,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = activeZone.category + " • " + activeZone.primaryPartnerApp,
                                    color = Color(0xFFFFB347),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // Multiplicador de Ganho
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFFF9F1C))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "+${String.format(Locale.US, "%.0f", (activeZone.estimatedBonusMultiplier - 1.0) * 100)}% Tarifa",
                                    color = Color.Black,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Métricas do Hotspot
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "🔥 ~${activeZone.averageOrdersPerHour} chamadas/hora",
                                color = TextLight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "📍 Raio: ${activeZone.radiusMeters.toInt()}m",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = activeZone.description,
                            color = TextLight.copy(alpha = 0.8f),
                            fontSize = 10.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Barra de Alternância para Listar Todos os Polos
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { isListExpanded = !isListExpanded }
                    .background(Color(0xFF14141E))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isListExpanded) "Ocultar polos gastronômicos" else "Ver 5 polos monitorados por Geofence",
                    color = NeonGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isListExpanded) "▲" else "▼",
                    color = NeonGreen,
                    fontSize = 12.sp
                )
            }

            // Lista de Hotspots Expansível
            AnimatedVisibility(
                visible = isListExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    registeredZones.forEach { zone ->
                        val isCurrent = activeZone?.id == zone.id
                        val distanceKm = LocationService.calculateDistanceKm(
                            userLatitude,
                            userLongitude,
                            zone.latitude,
                            zone.longitude
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isCurrent) Color(0xFF2A1C0A) else Color(0xFF0F0F17)
                                )
                                .border(
                                    1.dp,
                                    if (isCurrent) Color(0xFFFF9F1C) else DarkBorder,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = zone.name,
                                    color = if (isCurrent) Color(0xFFFFB347) else TextLight,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${zone.category} • ~${zone.averageOrdersPerHour} ped/h",
                                    color = TextMuted,
                                    fontSize = 9.sp
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (isCurrent) "DENTRO" else "${String.format(Locale.GERMANY, "%.1f", distanceKm)} km",
                                    color = if (isCurrent) Color(0xFFFF9F1C) else NeonGreen,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    // Botão de simular entrada
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFF222233))
                                            .clickable { onSimulateEnterZone(zone.id) }
                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Text("Testar", fontSize = 8.sp, color = TextLight)
                                    }
                                    // Botão de navegar no Maps
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0x3300FF88))
                                            .clickable { onNavigateToZone(zone) }
                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Text("Ir", fontSize = 8.sp, color = NeonGreen, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Botões de Teste e Simulação Rápida no Cockpit
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val firstZone = registeredZones.firstOrNull()
                        if (firstZone != null) {
                            onSimulateEnterZone(firstZone.id)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .testTag("btn_simulate_enter_hotspot"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF261905),
                        contentColor = Color(0xFFFF9F1C)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp)
                ) {
                    Text(
                        text = "⚡ Entrar em Hotspot",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onSimulateExitZone,
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .testTag("btn_simulate_exit_hotspot"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF181824),
                        contentColor = TextMuted
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp)
                ) {
                    Text(
                        text = "Sair do Hotspot",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
