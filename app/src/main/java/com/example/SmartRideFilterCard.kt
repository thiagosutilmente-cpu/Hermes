package com.example

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.roundToInt

// Cores táticas com alto contraste (Padrão Big Tech / OLED Night Mode)
private val DarkCardSurface = Color(0xFF11141C)
private val DarkCardBorder = Color(0xFF1E2433)
private val NeonGreenAccent = Color(0xFF00FF88)
private val CyberCyanAccent = Color(0xFF00E5FF)
private val MutedLabel = Color(0xFF8C9BAE)
private val DangerRedAccent = Color(0xFFFF4757)
private val DarkSubSurface = Color(0xFF0B0D13)

/**
 * Estado imutável do Filtro Inteligente de Corridas.
 */
data class SmartRideFilterState(
    val isEnabled: Boolean = true,
    val minPricePerKm: Double = 4.50,
    val maxDistanceKm: Double = 6.0
) {
    /**
     * Avalia se uma corrida atende aos critérios do filtro inteligente.
     */
    fun matches(gainPerKm: Double, distanceKm: Double): Boolean {
        if (!isEnabled) return true
        return gainPerKm >= minPricePerKm && distanceKm <= maxDistanceKm
    }
}

/**
 * Interface do Filtro Inteligente de Corridas.
 *
 * Permite ao motoboy:
 * 1. Ativar / Desativar o filtro instantaneamente via [Switch] Material 3.
 * 2. Regular o Piso Mínimo de Rentabilidade (R$/km) com controle deslizante e chips rápidos.
 * 3. Limitar a Distância Máxima de Deslocamento (km) para evitar desvios excessivos.
 * 4. Visualizar a projeção estimada de economia de gasolina e taxa de aprovação.
 */
@Composable
fun SmartRideFilterCard(
    modifier: Modifier = Modifier,
    initialState: SmartRideFilterState = SmartRideFilterState(),
    onFilterChange: (SmartRideFilterState) -> Unit = {}
) {
    val context = LocalContext.current

    var isEnabled by remember { mutableStateOf(initialState.isEnabled) }
    var minPricePerKm by remember { mutableDoubleStateOf(initialState.minPricePerKm) }
    var maxDistanceKm by remember { mutableDoubleStateOf(initialState.maxDistanceKm) }

    fun notifyChange(newEnabled: Boolean, newMinPrice: Double, newMaxDist: Double) {
        isEnabled = newEnabled
        minPricePerKm = newMinPrice
        maxDistanceKm = newMaxDist
        onFilterChange(SmartRideFilterState(newEnabled, newMinPrice, newMaxDist))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (isEnabled) NeonGreenAccent.copy(alpha = 0.5f) else DarkCardBorder,
                shape = RoundedCornerShape(20.dp)
            )
            .testTag("smart_ride_filter_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // -----------------------------------------------------------------
            // CABEÇALHO: Título, Ícone e Switch de Ativação
            // -----------------------------------------------------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (isEnabled) NeonGreenAccent.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f)
                            )
                            .border(
                                1.dp,
                                if (isEnabled) NeonGreenAccent.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.1f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Filtro Inteligente",
                            tint = if (isEnabled) NeonGreenAccent else MutedLabel,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "FILTRO INTELIGENTE",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = if (isEnabled) "🟢 Ativo • Descartando corridas lixo" else "⚪ Desativado • Exibindo todas",
                            color = if (isEnabled) NeonGreenAccent else MutedLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Switch Material 3 de Ativação / Desativação
                Switch(
                    checked = isEnabled,
                    onCheckedChange = { checked ->
                        if (checked) {
                            HapticFeedbackHelper.vibrateSuccess(context)
                        } else {
                            HapticFeedbackHelper.vibrateTap(context)
                        }
                        notifyChange(checked, minPricePerKm, maxDistanceKm)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF0A0E14),
                        checkedTrackColor = NeonGreenAccent,
                        uncheckedThumbColor = MutedLabel,
                        uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier.testTag("switch_smart_filter")
                )
            }

            // -----------------------------------------------------------------
            // CORPO EXPANSÍVEL: Sliders e Presets (Visível quando ativado)
            // -----------------------------------------------------------------
            AnimatedVisibility(
                visible = isEnabled,
                enter = fadeIn() + expandVertically(animationSpec = spring(stiffness = Spring.StiffnessLow)),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    // Linha divisória sutil
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(DarkCardBorder)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // =========================================================
                    // 1. CONTROLE DE VALOR MÍNIMO POR KM (R$/km)
                    // =========================================================
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = NeonGreenAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "PISO MÍNIMO POR KM",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Badge com o valor formatado
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(NeonGreenAccent.copy(alpha = 0.15f))
                                .border(1.dp, NeonGreenAccent.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = String.format(Locale.GERMANY, "R$ %.2f / km", minPricePerKm),
                                color = NeonGreenAccent,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Slider de Ganho/km
                    Slider(
                        value = minPricePerKm.toFloat(),
                        onValueChange = { rawVal ->
                            val rounded = (rawVal * 2).roundToInt() / 2.0 // Passos de 0.50
                            notifyChange(isEnabled, rounded, maxDistanceKm)
                        },
                        valueRange = 2.0f..8.0f,
                        steps = 11,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonGreenAccent,
                            activeTrackColor = NeonGreenAccent,
                            inactiveTrackColor = Color.White.copy(alpha = 0.12f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("slider_min_price_km")
                    )

                    // Presets rápidos de R$/km
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(3.00, 4.00, 4.50, 5.50, 6.50).forEach { presetVal ->
                            val isSelected = (minPricePerKm == presetVal)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    HapticFeedbackHelper.vibrateTap(context)
                                    notifyChange(isEnabled, presetVal, maxDistanceKm)
                                },
                                label = {
                                    Text(
                                        text = String.format(Locale.GERMANY, "R$ %.2f", presetVal),
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonGreenAccent.copy(alpha = 0.25f),
                                    selectedLabelColor = NeonGreenAccent,
                                    containerColor = Color.White.copy(alpha = 0.05f),
                                    labelColor = Color.LightGray
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = Color.White.copy(alpha = 0.1f),
                                    selectedBorderColor = NeonGreenAccent
                                ),
                                modifier = Modifier.testTag("preset_price_${presetVal}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // =========================================================
                    // 2. CONTROLE DE DISTÂNCIA MÁXIMA (km)
                    // =========================================================
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Navigation,
                                contentDescription = null,
                                tint = CyberCyanAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "DISTÂNCIA MÁXIMA",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Badge com a distância formatada
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(CyberCyanAccent.copy(alpha = 0.15f))
                                .border(1.dp, CyberCyanAccent.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = String.format(Locale.GERMANY, "%.1f km", maxDistanceKm),
                                color = CyberCyanAccent,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Slider de Distância
                    Slider(
                        value = maxDistanceKm.toFloat(),
                        onValueChange = { rawVal ->
                            val rounded = (rawVal * 2).roundToInt() / 2.0 // Passos de 0.5 km
                            notifyChange(isEnabled, minPricePerKm, rounded)
                        },
                        valueRange = 2.0f..15.0f,
                        steps = 25,
                        colors = SliderDefaults.colors(
                            thumbColor = CyberCyanAccent,
                            activeTrackColor = CyberCyanAccent,
                            inactiveTrackColor = Color.White.copy(alpha = 0.12f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("slider_max_distance")
                    )

                    // Presets rápidos de Distância
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            3.0 to "Tiro Curto (3 km)",
                            5.0 to "5 km",
                            7.0 to "7 km",
                            10.0 to "10 km"
                        ).forEach { (presetDist, label) ->
                            val isSelected = (maxDistanceKm == presetDist)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    HapticFeedbackHelper.vibrateTap(context)
                                    notifyChange(isEnabled, minPricePerKm, presetDist)
                                },
                                label = {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyberCyanAccent.copy(alpha = 0.25f),
                                    selectedLabelColor = CyberCyanAccent,
                                    containerColor = Color.White.copy(alpha = 0.05f),
                                    labelColor = Color.LightGray
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = Color.White.copy(alpha = 0.1f),
                                    selectedBorderColor = CyberCyanAccent
                                ),
                                modifier = Modifier.testTag("preset_dist_${presetDist}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // =========================================================
                    // 3. PAINEL DE INTELIGÊNCIA E PROJEÇÃO DE IMPACTO
                    // =========================================================
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSubSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.LocalGasStation,
                                        contentDescription = null,
                                        tint = NeonGreenAccent,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "PROJEÇÃO DE EFICIÊNCIA",
                                        color = TextLight(0.9f),
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.5.sp
                                    )
                                }

                                Text(
                                    text = "IA JARVIS",
                                    color = NeonGreenAccent,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                MetricImpactBox(
                                    label = "Aproveitamento",
                                    value = "${(100 - (minPricePerKm * 8.5)).coerceIn(25.0, 75.0).roundToInt()}%",
                                    color = Color.White
                                )
                                MetricImpactBox(
                                    label = "Econ. Gasolina",
                                    value = "+${(minPricePerKm * 5.2).coerceIn(15.0, 42.0).roundToInt()}%",
                                    color = NeonGreenAccent
                                )
                                MetricImpactBox(
                                    label = "Lucro/Hora Est.",
                                    value = "+R$ ${(minPricePerKm * 4.2).roundToInt()}/h",
                                    color = CyberCyanAccent
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricImpactBox(
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            color = color,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black
        )
        Text(
            text = label,
            color = MutedLabel,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun TextLight(alpha: Float): Color = Color(0xFFF0F0F8).copy(alpha = alpha)

// -----------------------------------------------------------------------------
// PREVIEW DO COMPONENTE
// -----------------------------------------------------------------------------
@Preview(
    name = "Smart Ride Filter Card Preview",
    showBackground = true,
    backgroundColor = 0xFF0A0A0F,
    widthDp = 380
)
@Composable
fun SmartRideFilterCardPreview() {
    Surface(
        color = Color(0xFF0A0A0F),
        modifier = Modifier.padding(16.dp)
    ) {
        SmartRideFilterCard(
            initialState = SmartRideFilterState(
                isEnabled = true,
                minPricePerKm = 4.50,
                maxDistanceKm = 6.0
            )
        )
    }
}
