package com.example

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.PI
import kotlin.math.roundToInt

// Paleta visual consistente com o Cockpit Radar / Modo Noturno de Alto Contraste
private val PanelDarkBg = Color(0xFF0D0D14)
private val PanelCardBg = Color(0xFF13131F)
private val PanelCardElevated = Color(0xFF1A1A2B)
private val PanelNeonGreen = Color(0xFF00FF88)
private val PanelCyberCyan = Color(0xFF00D2FF)
private val PanelAccentGold = Color(0xFFFFD700)
private val PanelBorderColor = Color(0xFF26263D)
private val PanelTextLight = Color(0xFFF0F0F8)
private val PanelTextMuted = Color(0xFF8E8EA8)

/**
 * PAINEL DE CONFIGURAÇÕES DE OFERTAS (SETTINGS PANEL)
 *
 * Componente Jetpack Compose dedicado para que o entregador configure
 * os dois pilares operacionais fundamentais para entrada de novos chamados:
 * 1. Piso de Valor Mínimo por Corrida (R$)
 * 2. Raio Máximo de Distância de Coleta/Entrega (km)
 *
 * Recursos inclusos:
 * - Sliders de alta precisão com indicador numérico formatado
 * - Botões grandes de incremento/decremento com toque facilitado (Modo Luva / Guidão)
 * - Chips de seleção rápida pré-calibrados (R$ 0, 15, 20, 25, 30, 40 e 3km, 5km, 8km, 12km, 15km)
 * - Cálculo geométrico em tempo real da área de cobertura viária (π × r²)
 * - Alternância para ativar/desativar restrição de raio geográfico
 * - Feedback háptico tático e persistência em SharedPreferences via [FilterPreferencesManager]
 * - Indicador de compatibilidade com pedidos em tempo real
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DeliveryOfferSettingsPanel(
    currentMinValue: Double,
    currentMaxRadiusKm: Double,
    onMinValueChange: (Double) -> Unit,
    onMaxRadiusKmChange: (Double) -> Unit,
    modifier: Modifier = Modifier,
    isRadiusLimitEnabled: Boolean = true,
    onToggleRadiusLimit: ((Boolean) -> Unit)? = null,
    onSavePreferences: (() -> Unit)? = null,
    onResetDefaults: (() -> Unit)? = null,
    isCollapsible: Boolean = true,
    initiallyExpanded: Boolean = true,
    matchingOffersCount: Int? = null,
    totalOffersCount: Int? = null
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(initiallyExpanded) }

    val quickValuePresets = listOf(0.0, 15.0, 20.0, 25.0, 30.0, 40.0)
    val quickRadiusPresets = listOf(3.0, 5.0, 8.0, 10.0, 15.0, 20.0)

    val formattedMinValue = if (currentMinValue > 0.0) {
        String.format(Locale("pt", "BR"), "R$ %.2f", currentMinValue)
    } else {
        "Sem Mínimo (Todas)"
    }

    val formattedRadius = if (isRadiusLimitEnabled) {
        String.format(Locale("pt", "BR"), "%.1f km", currentMaxRadiusKm)
    } else {
        "Sem Limite de Raio"
    }

    val coverageAreaKm2 = if (isRadiusLimitEnabled) {
        PI * currentMaxRadiusKm * currentMaxRadiusKm
    } else 0.0

    val isCustomized = currentMinValue > 0.0 || (isRadiusLimitEnabled && currentMaxRadiusKm != 8.0) || !isRadiusLimitEnabled

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .testTag("settings_panel_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = PanelCardBg),
        border = BorderStroke(
            width = 1.dp,
            color = if (isCustomized) PanelNeonGreen.copy(alpha = 0.45f) else PanelBorderColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // =========================================================================
            // CABEÇALHO DO PAINEL DE CONFIGURAÇÃO
            // =========================================================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (isCollapsible) {
                            Modifier.clickable {
                                isExpanded = !isExpanded
                                HapticFeedbackHelper.vibrateTap(context)
                            }
                        } else Modifier
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(PanelNeonGreen.copy(alpha = 0.2f), PanelCyberCyan.copy(alpha = 0.2f))
                                )
                            )
                            .border(1.dp, PanelNeonGreen.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configuração de Filtros",
                            tint = PanelNeonGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "PARÂMETROS DE OFERTAS",
                                color = PanelTextLight,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.6.sp
                            )
                            if (isCustomized) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(PanelNeonGreen)
                                )
                            }
                        }
                        Text(
                            text = "Piso financeiro & Raio de distância",
                            color = PanelTextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (matchingOffersCount != null && totalOffersCount != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(PanelCardElevated)
                                .border(0.8.dp, PanelNeonGreen.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "$matchingOffersCount/$totalOffersCount ativas",
                                color = PanelNeonGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    if (isCollapsible) {
                        IconButton(
                            onClick = {
                                isExpanded = !isExpanded
                                HapticFeedbackHelper.vibrateTap(context)
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("btn_toggle_expand_settings_panel")
                        ) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isExpanded) "Recolher" else "Expandir",
                                tint = PanelTextLight
                            )
                        }
                    }
                }
            }

            // Resumo quando recolhido
            if (!isExpanded && isCollapsible) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(PanelCardElevated)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mínimo: $formattedMinValue",
                        color = if (currentMinValue > 0.0) PanelNeonGreen else PanelTextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Raio: $formattedRadius",
                        color = if (isRadiusLimitEnabled) PanelCyberCyan else PanelTextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // =========================================================================
            // CORPO EXPANDIDO COM OS CONTROLES COMPLETOS
            // =========================================================================
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                ) {
                    // =====================================================================
                    // 1. SEÇÃO: VALOR MÍNIMO PREFERIDO POR CORRIDA (R$)
                    // =====================================================================
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(PanelCardElevated)
                            .border(0.8.dp, PanelBorderColor, RoundedCornerShape(14.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            // Cabeçalho da Seção de Valor
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = "💵", fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = "Valor Mínimo Aceitável",
                                            color = PanelTextLight,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Piso para aceitar novos chamados",
                                            color = PanelTextMuted,
                                            fontSize = 10.sp
                                        )
                                    }
                                }

                                // Badge com valor atual em destaque
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(PanelNeonGreen.copy(alpha = 0.15f))
                                        .border(1.dp, PanelNeonGreen.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = formattedMinValue,
                                        color = PanelNeonGreen,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Linha de Slider + Botões de Toque Rápido para Luva
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Botão de Diminuir (Glove-friendly)
                                OutlinedIconButton(
                                    onClick = {
                                        val nextVal = (currentMinValue - 2.5).coerceAtLeast(0.0)
                                        onMinValueChange(nextVal)
                                        HapticFeedbackHelper.vibrateTap(context)
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("btn_decrement_min_value"),
                                    border = BorderStroke(1.dp, PanelBorderColor),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(text = "−", color = PanelTextLight, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Slider de Precisão de R$ 0 a R$ 60
                                Slider(
                                    value = currentMinValue.toFloat(),
                                    onValueChange = { newValue ->
                                        val rounded = (newValue * 2).roundToInt() / 2.0
                                        onMinValueChange(rounded)
                                    },
                                    valueRange = 0f..60f,
                                    steps = 23,
                                    colors = SliderDefaults.colors(
                                        thumbColor = PanelNeonGreen,
                                        activeTrackColor = PanelNeonGreen,
                                        inactiveTrackColor = Color.DarkGray
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("slider_min_delivery_value")
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                // Botão de Aumentar (Glove-friendly)
                                OutlinedIconButton(
                                    onClick = {
                                        val nextVal = (currentMinValue + 2.5).coerceAtMost(60.0)
                                        onMinValueChange(nextVal)
                                        HapticFeedbackHelper.vibrateTap(context)
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("btn_increment_min_value"),
                                    border = BorderStroke(1.dp, PanelBorderColor),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(text = "+", color = PanelNeonGreen, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Chips Pré-calibrados de Valor Mínimo
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                quickValuePresets.forEach { presetVal ->
                                    val isSelected = currentMinValue == presetVal
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            onMinValueChange(presetVal)
                                            HapticFeedbackHelper.vibrateTap(context)
                                        },
                                        label = {
                                            Text(
                                                text = if (presetVal == 0.0) "Sem Mínimo" else "≥ R$ ${presetVal.toInt()}",
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            containerColor = PanelDarkBg,
                                            labelColor = PanelTextMuted,
                                            selectedContainerColor = PanelNeonGreen.copy(alpha = 0.2f),
                                            selectedLabelColor = PanelNeonGreen
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = isSelected,
                                            borderColor = if (isSelected) PanelNeonGreen else PanelBorderColor
                                        ),
                                        modifier = Modifier.testTag("chip_min_val_${presetVal.toInt()}")
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // =====================================================================
                    // 2. SEÇÃO: RAIO MÁXIMO DE DISTÂNCIA DE ALCANCE (KM)
                    // =====================================================================
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(PanelCardElevated)
                            .border(0.8.dp, PanelBorderColor, RoundedCornerShape(14.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            // Cabeçalho da Seção de Raio
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = "📍", fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = "Raio Máximo de Distância",
                                            color = PanelTextLight,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Limite radial viário da sua posição",
                                            color = PanelTextMuted,
                                            fontSize = 10.sp
                                        )
                                    }
                                }

                                // Alternância de ativação do filtro de raio
                                if (onToggleRadiusLimit != null) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Switch(
                                            checked = isRadiusLimitEnabled,
                                            onCheckedChange = {
                                                onToggleRadiusLimit(it)
                                                HapticFeedbackHelper.vibrateTap(context)
                                            },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = PanelCyberCyan,
                                                checkedTrackColor = PanelCyberCyan.copy(alpha = 0.35f),
                                                uncheckedThumbColor = PanelTextMuted,
                                                uncheckedTrackColor = PanelDarkBg
                                            ),
                                            modifier = Modifier.testTag("switch_radius_filter")
                                        )
                                    }
                                } else {
                                    // Badge com raio atual
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(PanelCyberCyan.copy(alpha = 0.15f))
                                            .border(1.dp, PanelCyberCyan.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = formattedRadius,
                                            color = PanelCyberCyan,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }
                            }

                            if (onToggleRadiusLimit != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (isRadiusLimitEnabled) "Filtro Ativo" else "Filtro Desativado (Sem limite)",
                                        color = if (isRadiusLimitEnabled) PanelCyberCyan else PanelTextMuted,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = formattedRadius,
                                        color = if (isRadiusLimitEnabled) PanelCyberCyan else PanelTextMuted,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Controles do Slider de Raio (ativo somente se isRadiusLimitEnabled = true)
                            AnimatedVisibility(visible = isRadiusLimitEnabled) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Botão Decrementar Raio
                                        OutlinedIconButton(
                                            onClick = {
                                                val nextVal = (currentMaxRadiusKm - 1.0).coerceAtLeast(1.0)
                                                onMaxRadiusKmChange(nextVal)
                                                HapticFeedbackHelper.vibrateTap(context)
                                            },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .testTag("btn_decrement_max_radius"),
                                            border = BorderStroke(1.dp, PanelBorderColor),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(text = "−", color = PanelTextLight, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        // Slider de 1.0 km a 20.0 km
                                        Slider(
                                            value = currentMaxRadiusKm.toFloat(),
                                            onValueChange = { newValue ->
                                                val rounded = (newValue * 10).roundToInt() / 10.0
                                                onMaxRadiusKmChange(rounded)
                                            },
                                            valueRange = 1f..20f,
                                            steps = 18,
                                            colors = SliderDefaults.colors(
                                                thumbColor = PanelCyberCyan,
                                                activeTrackColor = PanelCyberCyan,
                                                inactiveTrackColor = Color.DarkGray
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("slider_max_distance_radius")
                                        )

                                        Spacer(modifier = Modifier.width(8.dp))

                                        // Botão Incrementar Raio
                                        OutlinedIconButton(
                                            onClick = {
                                                val nextVal = (currentMaxRadiusKm + 1.0).coerceAtMost(20.0)
                                                onMaxRadiusKmChange(nextVal)
                                                HapticFeedbackHelper.vibrateTap(context)
                                            },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .testTag("btn_increment_max_radius"),
                                            border = BorderStroke(1.dp, PanelBorderColor),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(text = "+", color = PanelCyberCyan, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Indicador de Cobertura Geográfica
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(PanelDarkBg)
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "🎯 Área Coberta: ~${coverageAreaKm2.toInt()} km²",
                                            color = PanelCyberCyan,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "~${(currentMaxRadiusKm * 2.8).toInt()} a ${(currentMaxRadiusKm * 3.5).toInt()} min viagem",
                                            color = PanelTextMuted,
                                            fontSize = 10.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Chips Pré-calibrados de Raio Máximo
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        quickRadiusPresets.forEach { presetRadius ->
                                            val isSelected = currentMaxRadiusKm == presetRadius
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = {
                                                    onMaxRadiusKmChange(presetRadius)
                                                    HapticFeedbackHelper.vibrateTap(context)
                                                },
                                                label = {
                                                    Text(
                                                        text = "≤ ${presetRadius.toInt()} km",
                                                        fontSize = 11.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    containerColor = PanelDarkBg,
                                                    labelColor = PanelTextMuted,
                                                    selectedContainerColor = PanelCyberCyan.copy(alpha = 0.2f),
                                                    selectedLabelColor = PanelCyberCyan
                                                ),
                                                border = FilterChipDefaults.filterChipBorder(
                                                    enabled = true,
                                                    selected = isSelected,
                                                    borderColor = if (isSelected) PanelCyberCyan else PanelBorderColor
                                                ),
                                                modifier = Modifier.testTag("chip_max_radius_${presetRadius.toInt()}")
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // =====================================================================
                    // 3. BARRA DE AÇÕES: SALVAR PREFERÊNCIAS & RESTAURAR PADRÃO
                    // =====================================================================
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                onResetDefaults?.invoke() ?: run {
                                    onMinValueChange(0.0)
                                    onMaxRadiusKmChange(8.0)
                                    onToggleRadiusLimit?.invoke(true)
                                }
                                HapticFeedbackHelper.vibrateTap(context)
                                Toast.makeText(context, "Valores restaurados para o padrão!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("btn_reset_preferences"),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, PanelBorderColor),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PanelTextMuted)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Restaurar",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "PADRÃO", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                onSavePreferences?.invoke()
                                HapticFeedbackHelper.vibrateSuccess(context)
                                Toast.makeText(
                                    context,
                                    "Preferências salvas: Mínimo $formattedMinValue • Raio $formattedRadius",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier
                                .weight(1.3f)
                                .height(44.dp)
                                .testTag("btn_save_preferences"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PanelNeonGreen,
                                contentColor = Color.Black
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Salvar",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "APLICAR FILTROS", fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Versão do painel conectada diretamente com [OfferFilterCriteria] e [FilterPreferencesManager].
 */
@Composable
fun ConnectedOfferSettingsPanel(
    criteria: OfferFilterCriteria,
    onCriteriaChange: (OfferFilterCriteria) -> Unit,
    modifier: Modifier = Modifier,
    matchingOffersCount: Int? = null,
    totalOffersCount: Int? = null,
    isCollapsible: Boolean = true,
    initiallyExpanded: Boolean = true
) {
    val context = LocalContext.current

    DeliveryOfferSettingsPanel(
        currentMinValue = criteria.minValue,
        currentMaxRadiusKm = if (criteria.isGeoRadiusFilterActive) criteria.maxPickupRadiusKm else criteria.maxDistanceKm,
        onMinValueChange = { newMinVal ->
            val updated = criteria.copy(minValue = newMinVal)
            onCriteriaChange(updated)
            FilterPreferencesManager.saveCriteria(context, updated)
        },
        onMaxRadiusKmChange = { newRadius ->
            val updated = criteria.copy(
                maxDistanceKm = newRadius,
                maxPickupRadiusKm = newRadius
            )
            onCriteriaChange(updated)
            FilterPreferencesManager.saveCriteria(context, updated)
        },
        isRadiusLimitEnabled = criteria.maxDistanceKm < 20.0 || criteria.isGeoRadiusFilterActive,
        onToggleRadiusLimit = { isEnabled ->
            val updated = criteria.copy(
                isGeoRadiusFilterActive = isEnabled,
                maxDistanceKm = if (isEnabled) criteria.maxDistanceKm else 25.0
            )
            onCriteriaChange(updated)
            FilterPreferencesManager.saveCriteria(context, updated)
        },
        onSavePreferences = {
            FilterPreferencesManager.saveCriteria(context, criteria)
        },
        onResetDefaults = {
            val defaults = criteria.copy(
                minValue = 0.0,
                maxDistanceKm = 8.0,
                maxPickupRadiusKm = 6.0,
                isGeoRadiusFilterActive = true
            )
            onCriteriaChange(defaults)
            FilterPreferencesManager.saveCriteria(context, defaults)
        },
        modifier = modifier,
        isCollapsible = isCollapsible,
        initiallyExpanded = initiallyExpanded,
        matchingOffersCount = matchingOffersCount,
        totalOffersCount = totalOffersCount
    )
}

// -----------------------------------------------------------------------------
// PREVIEW DO COMPONENTE NO JETPACK COMPOSE
// -----------------------------------------------------------------------------
@Preview(
    name = "Delivery Offer Settings Panel - Cockpit Theme",
    showBackground = true,
    backgroundColor = 0xFF0A0A0F,
    widthDp = 380
)
@Composable
fun DeliveryOfferSettingsPanelPreview() {
    var minVal by remember { mutableDoubleStateOf(20.0) }
    var maxRadius by remember { mutableDoubleStateOf(6.0) }
    var isRadiusEnabled by remember { mutableStateOf(true) }

    MaterialTheme {
        Surface(color = PanelDarkBg, modifier = Modifier.padding(16.dp)) {
            DeliveryOfferSettingsPanel(
                currentMinValue = minVal,
                currentMaxRadiusKm = maxRadius,
                onMinValueChange = { minVal = it },
                onMaxRadiusKmChange = { maxRadius = it },
                isRadiusLimitEnabled = isRadiusEnabled,
                onToggleRadiusLimit = { isRadiusEnabled = it },
                matchingOffersCount = 14,
                totalOffersCount = 20
            )
        }
    }
}
