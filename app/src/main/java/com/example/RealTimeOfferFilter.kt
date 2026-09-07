package com.example

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.util.Locale

/**
 * Critérios de filtragem dinâmica para ofertas em tempo real.
 * Permite ao piloto definir:
 * 1. Valor Mínimo por corrida (R$)
 * 2. Distância Máxima total (km)
 * 3. Multiplicador de Ganho por Quilômetro (R$/km)
 * 4. Somente recomendações aceitas pelo Jarvis Neural
 * 5. Somente Entregas Mescladas (Multi-Stack)
 * 6. Busca textual por restaurante, app ou corredor de rota
 */
data class OfferFilterCriteria(
    val minValue: Double = 0.0,
    val maxDistanceKm: Double = 8.0,
    val minGainPerKm: Double = 0.0,
    val onlyAcceptedNeural: Boolean = false,
    val onlyMultiStack: Boolean = false,
    val searchQuery: String = ""
) {
    val isActive: Boolean
        get() = minValue > 0.0 || maxDistanceKm < 8.0 || minGainPerKm > 0.0 || onlyAcceptedNeural || onlyMultiStack || searchQuery.isNotBlank()

    fun matches(offer: RadarOffer): Boolean {
        if (onlyMultiStack && !offer.isMultiStack) return false
        if (offer.value < minValue) return false
        if (offer.distanceKm > maxDistanceKm) return false
        if (minGainPerKm > 0.0 && offer.gainPerKm < minGainPerKm) return false
        if (onlyAcceptedNeural && !offer.neuralDecision.isAccept) return false
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim().lowercase(Locale.getDefault())
            val matchApp = offer.appName.lowercase(Locale.getDefault()).contains(q)
            val matchRest = offer.restaurant.lowercase(Locale.getDefault()).contains(q)
            val matchPickup = offer.pickupAddress.lowercase(Locale.getDefault()).contains(q)
            val matchDest = offer.destinationAddress.lowercase(Locale.getDefault()).contains(q)
            val matchSub = offer.subOrders.any {
                it.appName.lowercase(Locale.getDefault()).contains(q) ||
                it.restaurant.lowercase(Locale.getDefault()).contains(q) ||
                it.pickupAddress.lowercase(Locale.getDefault()).contains(q)
            }
            val matchWaypoints = offer.waypointRoute.any { it.lowercase(Locale.getDefault()).contains(q) }
            val matchKeyword = if (q.contains("mescl") || q.contains("multi") || q.contains("stack")) offer.isMultiStack else false
            if (!matchApp && !matchRest && !matchPickup && !matchDest && !matchSub && !matchWaypoints && !matchKeyword) return false
        }
        return true
    }
}

/**
 * PAINEL SUPERIOR COM SLIDERS EM TEMPO REAL
 * Posicionado no topo da tela principal, permitindo ajuste instantâneo
 * dos critérios principais com reflexo imediato na fila de pedidos:
 * - Valor mínimo (R$)
 * - Distância máxima (km)
 * - Multiplicador de ganho por quilômetro (R$/km)
 * - Busca textual e varredura de entregas mescladas (Multi-Stack)
 */
@Composable
fun TopFilterSlidersPanel(
    criteria: OfferFilterCriteria,
    totalOffersCount: Int,
    filteredOffersCount: Int,
    mergedOffersCount: Int = 0,
    onCriteriaChange: (OfferFilterCriteria) -> Unit,
    modifier: Modifier = Modifier,
    onTriggerMergedSearch: (() -> Unit)? = null,
    onOpenAdvancedSettings: (() -> Unit)? = null
) {
    var isCollapsed by remember { mutableStateOf(false) }

    val formattedMinVal = if (criteria.minValue > 0.0) {
        String.format(Locale.GERMANY, "R$ %.2f", criteria.minValue)
    } else {
        "Sem Mínimo"
    }

    val formattedMaxDist = if (criteria.maxDistanceKm < 10.0) {
        String.format(Locale.GERMANY, "%.1f km", criteria.maxDistanceKm)
    } else {
        "Sem Limite"
    }

    val formattedMinGain = if (criteria.minGainPerKm > 0.0) {
        String.format(Locale.GERMANY, "R$ %.2f/km", criteria.minGainPerKm)
    } else {
        "Sem Mínimo"
    }

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
                .padding(horizontal = 16.dp, vertical = 12.dp)
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
                        Spacer(modifier = Modifier.width(6.dp))
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

                    if (onOpenAdvancedSettings != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(DarkCardElevated)
                                .clickable { onOpenAdvancedSettings() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "⚙️", fontSize = 13.sp)
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (criteria.onlyMultiStack) {
                        FilterSummaryChip(label = "✨ Só Mescladas", isActive = true)
                    }
                    if (criteria.searchQuery.isNotBlank()) {
                        FilterSummaryChip(label = "🔍 \"${criteria.searchQuery}\"", isActive = true)
                    }
                    FilterSummaryChip(label = "Mín: $formattedMinVal", isActive = criteria.minValue > 0)
                    FilterSummaryChip(label = "Máx: $formattedMaxDist", isActive = criteria.maxDistanceKm < 8.0)
                    FilterSummaryChip(label = "Mult: $formattedMinGain", isActive = criteria.minGainPerKm > 0)
                    if (criteria.onlyAcceptedNeural) {
                        FilterSummaryChip(label = "🧠 Só Jarvis", isActive = true)
                    }
                }
            }

            // SE EXPANDIDO: Exibe os 3 sliders e controles de presets
            AnimatedVisibility(
                visible = !isCollapsed,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 10.dp)) {

                    // ========================================================
                    // BUSCA DE ENTREGAS MESCLADAS & FILTRO MULTI-STACK
                    // ========================================================
                    OutlinedTextField(
                        value = criteria.searchQuery,
                        onValueChange = { onCriteriaChange(criteria.copy(searchQuery = it)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("input_search_offers"),
                        placeholder = {
                            Text("Buscar restaurante, app ou mesclada...", color = TextMuted, fontSize = 12.sp)
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Buscar",
                                tint = if (criteria.searchQuery.isNotBlank()) NeonGreen else TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (criteria.searchQuery.isNotBlank()) {
                                IconButton(onClick = { onCriteriaChange(criteria.copy(searchQuery = "")) }) {
                                    Icon(Icons.Default.Close, contentDescription = "Limpar busca", tint = TextMuted, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonGreen,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight,
                            cursorColor = NeonGreen,
                            focusedContainerColor = DarkBg.copy(alpha = 0.8f),
                            unfocusedContainerColor = DarkBg.copy(alpha = 0.8f)
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // BOTOES DE CONTROLE MULTI-STACK
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Botão Toggle: "✨ SÓ MESCLADAS"
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (criteria.onlyMultiStack) NeonGreen.copy(alpha = 0.22f) else DarkCardElevated)
                                .border(
                                    width = if (criteria.onlyMultiStack) 1.5.dp else 1.dp,
                                    color = if (criteria.onlyMultiStack) NeonGreen else DarkBorder,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    onCriteriaChange(criteria.copy(onlyMultiStack = !criteria.onlyMultiStack))
                                }
                                .padding(horizontal = 8.dp, vertical = 8.dp)
                                .testTag("btn_toggle_multi_stack"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "✨", fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "SÓ MESCLADAS",
                                    color = if (criteria.onlyMultiStack) NeonGreen else TextLight,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Black
                                )
                                if (mergedOffersCount > 0) {
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(if (criteria.onlyMultiStack) NeonGreen else DarkBorder)
                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "$mergedOffersCount",
                                            color = if (criteria.onlyMultiStack) DarkBg else TextLight,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }
                            }
                        }

                        // Botão Ação Rápida: Varredura de Mesclagens
                        if (onTriggerMergedSearch != null) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF00D2FF).copy(alpha = 0.15f))
                                    .border(1.dp, Color(0xFF00D2FF).copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                    .clickable { onTriggerMergedSearch() }
                                    .padding(horizontal = 8.dp, vertical = 8.dp)
                                    .testTag("btn_trigger_merged_search"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = "🔍", fontSize = 12.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "VARRER MULTI-STACK",
                                        color = Color(0xFF00D2FF),
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

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
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Slider(
                        value = criteria.minValue.toFloat(),
                        onValueChange = { newValue ->
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
                            .height(32.dp)
                            .testTag("slider_min_value")
                    )

                    // Chips Rápidos de Valor
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

                    Spacer(modifier = Modifier.height(10.dp))

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
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Slider(
                        value = criteria.maxDistanceKm.toFloat(),
                        onValueChange = { newDist ->
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
                            .height(32.dp)
                            .testTag("slider_max_distance")
                    )

                    // Chips Rápidos de Distância
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

                    // ========================================================
                    // 3. SLIDER: MULTIPLICADOR DE GANHO POR QUILÔMETRO (R$/KM)
                    // ========================================================
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "⚡", fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Multiplicador Ganho/Km:",
                                color = TextLight,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = formattedMinGain,
                            color = Color(0xFFFFD700),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Slider(
                        value = criteria.minGainPerKm.toFloat(),
                        onValueChange = { newGain ->
                            val rounded = (newGain.toDouble() * 2).toInt() / 2.0
                            onCriteriaChange(criteria.copy(minGainPerKm = rounded))
                        },
                        valueRange = 0f..10f,
                        steps = 19,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFFFD700),
                            activeTrackColor = Color(0xFFFFD700),
                            inactiveTrackColor = DarkBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp)
                            .testTag("slider_min_gain_per_km")
                    )

                    // Chips Rápidos de Multiplicador Ganho/Km
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        QuickValueChip(label = "Sem Mínimo", isSelected = criteria.minGainPerKm == 0.0) {
                            onCriteriaChange(criteria.copy(minGainPerKm = 0.0))
                        }
                        QuickValueChip(label = "R$ 4,00/km", isSelected = criteria.minGainPerKm == 4.0) {
                            onCriteriaChange(criteria.copy(minGainPerKm = 4.0))
                        }
                        QuickValueChip(label = "R$ 5,00/km", isSelected = criteria.minGainPerKm == 5.0) {
                            onCriteriaChange(criteria.copy(minGainPerKm = 5.0))
                        }
                        QuickValueChip(label = "R$ 6,50/km", isSelected = criteria.minGainPerKm == 6.5) {
                            onCriteriaChange(criteria.copy(minGainPerKm = 6.5))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // ==========================================
                    // 4. FILTRO ADICIONAL: SOMENTE JARVIS RECOMENDADO
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

                    Spacer(modifier = Modifier.height(8.dp))

                    // ==========================================
                    // 5. PRESETS DE ESTRATÉGIA DO PILOTO
                    // ==========================================
                    Text(
                        text = "PRESETS RÁPIDOS DE ESTRATÉGIA",
                        color = TextMuted,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        StrategyPresetChip(
                            icon = "🌧️",
                            title = "Chuva / Tarifa",
                            subtitle = "R$22+ • 5km • R$6/km",
                            onClick = {
                                onCriteriaChange(
                                    OfferFilterCriteria(
                                        minValue = 22.0,
                                        maxDistanceKm = 5.0,
                                        minGainPerKm = 6.0,
                                        onlyAcceptedNeural = true
                                    )
                                )
                            }
                        )

                        StrategyPresetChip(
                            icon = "⚡",
                            title = "Tiro Curto",
                            subtitle = "R$12+ • 3.5km • R$5/km",
                            onClick = {
                                onCriteriaChange(
                                    OfferFilterCriteria(
                                        minValue = 12.0,
                                        maxDistanceKm = 3.5,
                                        minGainPerKm = 5.0,
                                        onlyAcceptedNeural = false
                                    )
                                )
                            }
                        )

                        StrategyPresetChip(
                            icon = "💎",
                            title = "Máximo Lucro",
                            subtitle = "R$30+ • 7km • R$7/km",
                            onClick = {
                                onCriteriaChange(
                                    OfferFilterCriteria(
                                        minValue = 30.0,
                                        maxDistanceKm = 7.0,
                                        minGainPerKm = 7.0,
                                        onlyAcceptedNeural = true
                                    )
                                )
                            }
                        )

                        StrategyPresetChip(
                            icon = "✨",
                            title = "Multi-Stack Top",
                            subtitle = "Mescladas • R$6,50/km",
                            onClick = {
                                onCriteriaChange(
                                    OfferFilterCriteria(
                                        minValue = 25.0,
                                        maxDistanceKm = 6.0,
                                        minGainPerKm = 6.0,
                                        onlyAcceptedNeural = true,
                                        onlyMultiStack = true
                                    )
                                )
                            }
                        )

                        StrategyPresetChip(
                            icon = "🎯",
                            title = "Restaurar Padrão",
                            subtitle = "Sem restrições",
                            onClick = {
                                onCriteriaChange(OfferFilterCriteria())
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Chip para exibir resumo quando os sliders estão recolhidos
 */
@Composable
private fun FilterSummaryChip(label: String, isActive: Boolean) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isActive) NeonGreen.copy(alpha = 0.15f) else DarkCardElevated)
            .border(
                0.8.dp,
                if (isActive) NeonGreen.copy(alpha = 0.5f) else DarkBorder,
                RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            color = if (isActive) NeonGreen else TextLight,
            fontSize = 10.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
        )
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
            .padding(horizontal = 8.dp, vertical = 4.dp)
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
 * Chip de preset de estratégia para o piloto
 */
@Composable
private fun StrategyPresetChip(
    icon: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(DarkCardElevated)
            .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = icon, fontSize = 14.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = title,
                    color = TextLight,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    color = TextMuted,
                    fontSize = 8.5.sp
                )
            }
        }
    }
}

/**
 * MODAL DE DIÁLOGO DE CONFIGURAÇÃO AVANÇADA DE FILTROS
 * Janela dedicada que permite ao entregador definir e refinar
 * todos os parâmetros de filtragem com explicações didáticas de impacto financeiro.
 */
@Composable
fun FilterSettingsDialog(
    criteria: OfferFilterCriteria,
    onCriteriaChange: (OfferFilterCriteria) -> Unit,
    onDismiss: () -> Unit
) {
    var tempCriteria by remember { mutableStateOf(criteria) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, NeonGreen.copy(alpha = 0.5f), RoundedCornerShape(24.dp)),
            color = DarkCard
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header do Modal
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🎯", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Configuração de Filtros",
                                color = TextLight,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "Defina os limites de rentabilidade do cockpit",
                                color = TextMuted,
                                fontSize = 10.5.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fechar",
                            tint = TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Card Informativo de Rendimento
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(NeonGreen.copy(alpha = 0.08f))
                        .border(1.dp, NeonGreen.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "💡", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Filtros inteligentes economizam combustível e impedem que corridas abaixo do custo por km poluam sua atenção no trânsito.",
                            color = TextLight,
                            fontSize = 10.5.sp,
                            lineHeight = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 1. VALOR MÍNIMO (R$)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Valor Mínimo por Corrida",
                            color = TextLight,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Oculta entregas com valor bruto menor",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                    Text(
                        text = if (tempCriteria.minValue > 0) String.format(Locale.GERMANY, "R$ %.2f", tempCriteria.minValue) else "Sem Mínimo",
                        color = NeonGreen,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Slider(
                    value = tempCriteria.minValue.toFloat(),
                    onValueChange = {
                        val rounded = (it.toDouble() * 2).toInt() / 2.0
                        tempCriteria = tempCriteria.copy(minValue = rounded)
                    },
                    valueRange = 0f..50f,
                    steps = 24,
                    colors = SliderDefaults.colors(
                        thumbColor = NeonGreen,
                        activeTrackColor = NeonGreen,
                        inactiveTrackColor = DarkBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 2. DISTÂNCIA MÁXIMA (KM)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Distância Máxima de Deslocamento",
                            color = TextLight,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Limita o raio de coleta e entrega",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                    Text(
                        text = if (tempCriteria.maxDistanceKm < 10.0) String.format(Locale.GERMANY, "%.1f km", tempCriteria.maxDistanceKm) else "Sem Limite",
                        color = Color(0xFF00D2FF),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Slider(
                    value = tempCriteria.maxDistanceKm.toFloat(),
                    onValueChange = {
                        val rounded = (it.toDouble() * 10).toInt() / 10.0
                        tempCriteria = tempCriteria.copy(maxDistanceKm = rounded)
                    },
                    valueRange = 1f..10f,
                    steps = 17,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF00D2FF),
                        activeTrackColor = Color(0xFF00D2FF),
                        inactiveTrackColor = DarkBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 3. MULTIPLICADOR DE GANHO POR QUILÔMETRO (R$/KM)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Multiplicador Ganho por Km",
                            color = TextLight,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Garante rentabilidade mínima por km rodado",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                    Text(
                        text = if (tempCriteria.minGainPerKm > 0) String.format(Locale.GERMANY, "R$ %.2f/km", tempCriteria.minGainPerKm) else "Sem Mínimo",
                        color = Color(0xFFFFD700),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Slider(
                    value = tempCriteria.minGainPerKm.toFloat(),
                    onValueChange = {
                        val rounded = (it.toDouble() * 2).toInt() / 2.0
                        tempCriteria = tempCriteria.copy(minGainPerKm = rounded)
                    },
                    valueRange = 0f..10f,
                    steps = 19,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFFFD700),
                        activeTrackColor = Color(0xFFFFD700),
                        inactiveTrackColor = DarkBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 4. SOMENTE RECOMENDAÇÃO JARVIS
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkCardElevated)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "🧠 Somente Jarvis Aceitar",
                            color = TextLight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Descarta automaticamente chamadas inviáveis",
                            color = TextMuted,
                            fontSize = 9.5.sp
                        )
                    }

                    Switch(
                        checked = tempCriteria.onlyAcceptedNeural,
                        onCheckedChange = { tempCriteria = tempCriteria.copy(onlyAcceptedNeural = it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NeonGreen,
                            checkedTrackColor = NeonGreen.copy(alpha = 0.35f),
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = DarkBorder
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 5. SOMENTE ENTREGAS MESCLADAS (MULTI-STACK)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkCardElevated)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "✨ Somente Mescladas (Multi-App)",
                            color = TextLight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Filtra apenas pedidos agrupados com alta sinergia",
                            color = TextMuted,
                            fontSize = 9.5.sp
                        )
                    }

                    Switch(
                        checked = tempCriteria.onlyMultiStack,
                        onCheckedChange = { tempCriteria = tempCriteria.copy(onlyMultiStack = it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NeonGreen,
                            checkedTrackColor = NeonGreen.copy(alpha = 0.35f),
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = DarkBorder
                        )
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Botões de Ação do Diálogo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            tempCriteria = OfferFilterCriteria()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RedDecline),
                        border = androidx.compose.foundation.BorderStroke(1.dp, RedDecline.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("REDEFINIR", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            onCriteriaChange(tempCriteria)
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1.3f)
                            .height(48.dp)
                            .testTag("btn_apply_filter_settings"),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("APLICAR FILTROS", color = DarkBg, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                }
            }
        }
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
