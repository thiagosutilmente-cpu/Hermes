package com.example

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

/**
 * Critérios de filtragem dinâmica para ofertas em tempo real
 */
data class OfferFilterCriteria(
    val minValue: Double = 0.0,
    val maxDistanceKm: Double = 8.0,
    val onlyAcceptedNeural: Boolean = false
) {
    val isActive: Boolean
        get() = minValue > 0.0 || maxDistanceKm < 8.0 || onlyAcceptedNeural

    fun matches(offer: RadarOffer): Boolean {
        if (offer.value < minValue) return false
        if (offer.distanceKm > maxDistanceKm) return false
        if (onlyAcceptedNeural && !offer.neuralDecision.isAccept) return false
        return true
    }
}

/**
 * PAINEL SUPERIOR COM SLIDERS EM TEMPO REAL
 * Posicionado no topo da tela principal, permitindo ajuste instantâneo
 * do valor mínimo da entrega e da distância máxima com reflexo imediato no LazyColumn.
 */
@Composable
fun TopFilterSlidersPanel(
    criteria: OfferFilterCriteria,
    totalOffersCount: Int,
    filteredOffersCount: Int,
    onCriteriaChange: (OfferFilterCriteria) -> Unit,
    modifier: Modifier = Modifier
) {
    var isCollapsed by remember { mutableStateOf(false) }

    val formattedMinVal = String.format(Locale.GERMANY, "R$ %.2f", criteria.minValue)
    val formattedMaxDist = String.format(Locale.GERMANY, "%.1f km", criteria.maxDistanceKm)

    Card(
        shape = RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (criteria.isActive) NeonGreen.copy(alpha = 0.5f) else DarkBorder,
                shape = RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp)
            )
            .testTag("top_filter_sliders_panel")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            // HEADER DO PAINEL SUPERIOR
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Título & Indicador Ativo
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { isCollapsed = !isCollapsed }
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (criteria.isActive) NeonGreen else TextMuted)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "FILTRAGEM EM TEMPO REAL",
                        color = TextLight,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp
                    )
                }

                // Status Badge & Ações Rápidas
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Contador de Ofertas que passam no filtro
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (criteria.isActive) NeonGreen.copy(alpha = 0.18f) else DarkCardElevated)
                            .border(
                                0.8.dp,
                                if (criteria.isActive) NeonGreen.copy(alpha = 0.6f) else DarkBorder,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$filteredOffersCount de $totalOffersCount ativas",
                            color = if (criteria.isActive) NeonGreen else TextLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    if (criteria.isActive) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(RedDecline.copy(alpha = 0.15f))
                                .border(0.8.dp, RedDecline.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .clickable { onCriteriaChange(OfferFilterCriteria()) }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                .testTag("btn_reset_top_filters")
                        ) {
                            Text(
                                text = "LIMPAR",
                                color = RedDecline,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Botão Recolher/Expandir
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(DarkCardElevated)
                            .clickable { isCollapsed = !isCollapsed },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isCollapsed) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                            contentDescription = if (isCollapsed) "Expandir sliders" else "Recolher sliders",
                            tint = if (criteria.isActive) NeonGreen else TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // SE RECOLHIDO: Exibe barra resumo compacta
            if (isCollapsed) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Mín: $formattedMinVal  •  Máx: $formattedMaxDist" + if (criteria.onlyAcceptedNeural) "  •  🧠 Só Jarvis" else "",
                    color = if (criteria.isActive) NeonGreen else TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // SE EXPANDIDO: Exibe os dois sliders para ajuste imediato
            AnimatedVisibility(
                visible = !isCollapsed,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    // ==========================================
                    // 1. SLIDER: VALOR MÍNIMO DA ENTREGA (R$)
                    // ==========================================
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "💰", fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Valor Mínimo:",
                                color = TextLight,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = formattedMinVal,
                            color = NeonGreen,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Slider(
                        value = criteria.minValue.toFloat(),
                        onValueChange = { newValue ->
                            // Arredonda para múltiplos de 0.50 para ajuste fluido
                            val rounded = (newValue.toDouble() * 2).toInt() / 2.0
                            onCriteriaChange(criteria.copy(minValue = rounded))
                        },
                        valueRange = 0f..40f,
                        steps = 19,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonGreen,
                            activeTrackColor = NeonGreen,
                            inactiveTrackColor = DarkBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(34.dp)
                            .testTag("slider_min_value")
                    )

                    // Chips de Acesso Rápido para Valor
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        QuickValueChip(label = "Todos", isSelected = criteria.minValue == 0.0) {
                            onCriteriaChange(criteria.copy(minValue = 0.0))
                        }
                        QuickValueChip(label = "R$ 15+", isSelected = criteria.minValue == 15.0) {
                            onCriteriaChange(criteria.copy(minValue = 15.0))
                        }
                        QuickValueChip(label = "R$ 22+", isSelected = criteria.minValue == 22.0) {
                            onCriteriaChange(criteria.copy(minValue = 22.0))
                        }
                        QuickValueChip(label = "R$ 30+", isSelected = criteria.minValue == 30.0) {
                            onCriteriaChange(criteria.copy(minValue = 30.0))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // ==========================================
                    // 2. SLIDER: DISTÂNCIA MÁXIMA (KM)
                    // ==========================================
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🛵", fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Distância Máxima:",
                                color = TextLight,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = formattedMaxDist,
                            color = Color(0xFF00D2FF),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Slider(
                        value = criteria.maxDistanceKm.toFloat(),
                        onValueChange = { newDist ->
                            // Arredonda para 1 casa decimal
                            val rounded = (newDist.toDouble() * 10).toInt() / 10.0
                            onCriteriaChange(criteria.copy(maxDistanceKm = rounded))
                        },
                        valueRange = 1f..10f,
                        steps = 17,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF00D2FF),
                            activeTrackColor = Color(0xFF00D2FF),
                            inactiveTrackColor = DarkBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(34.dp)
                            .testTag("slider_max_distance")
                    )

                    // Chips de Acesso Rápido para Distância
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        QuickValueChip(label = "Até 3 km", isSelected = criteria.maxDistanceKm == 3.0) {
                            onCriteriaChange(criteria.copy(maxDistanceKm = 3.0))
                        }
                        QuickValueChip(label = "Até 5 km", isSelected = criteria.maxDistanceKm == 5.0) {
                            onCriteriaChange(criteria.copy(maxDistanceKm = 5.0))
                        }
                        QuickValueChip(label = "Até 7 km", isSelected = criteria.maxDistanceKm == 7.0) {
                            onCriteriaChange(criteria.copy(maxDistanceKm = 7.0))
                        }
                        QuickValueChip(label = "Sem Limite", isSelected = criteria.maxDistanceKm >= 8.0) {
                            onCriteriaChange(criteria.copy(maxDistanceKm = 10.0))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // ==========================================
                    // 3. FILTRO ADICIONAL: SOMENTE JARVIS RECOMENDADO
                    // ==========================================
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkCardElevated)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🧠", fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "Somente Jarvis Aceitar",
                                    color = TextLight,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Oculta chamadas com alerta de desvantagem",
                                    color = TextMuted,
                                    fontSize = 9.sp
                                )
                            }
                        }

                        Switch(
                            checked = criteria.onlyAcceptedNeural,
                            onCheckedChange = { onCriteriaChange(criteria.copy(onlyAcceptedNeural = it)) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NeonGreen,
                                checkedTrackColor = NeonGreen.copy(alpha = 0.35f),
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = DarkBorder
                            ),
                            modifier = Modifier.testTag("switch_jarvis_filter")
                        )
                    }
                }
            }
        }
    }
}

/**
 * Chip de toque rápido para valores e distâncias
 */
@Composable
private fun QuickValueChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) NeonGreen.copy(alpha = 0.2f) else DarkCardElevated)
            .border(
                width = 1.dp,
                color = if (isSelected) NeonGreen else DarkBorder,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) NeonGreen else TextMuted,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

/**
 * Compatibilidade com o componente anterior, caso referenciado
 */
@Composable
fun RealTimeOfferFilterCard(
    criteria: OfferFilterCriteria,
    totalOffersCount: Int,
    filteredOffersCount: Int,
    onCriteriaChange: (OfferFilterCriteria) -> Unit,
    modifier: Modifier = Modifier
) {
    TopFilterSlidersPanel(
        criteria = criteria,
        totalOffersCount = totalOffersCount,
        filteredOffersCount = filteredOffersCount,
        onCriteriaChange = onCriteriaChange,
        modifier = modifier
    )
}
