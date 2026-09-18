package com.example

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

/**
 * TELA DEDICADA DE CONFIGURAÇÃO DE FILTROS DE RENTABILIDADE
 *
 * Permite ao entregador definir e calibrar com precisão cirúrgica:
 * 1. Valor mínimo por corrida (R$)
 * 2. Distância máxima de trajeto (km)
 * 3. Bônus por entrega / gorjeta estimada (R$)
 * 4. Multiplicador de ganho por quilômetro (R$/km)
 * 5. Somente recomendações positivas do Jarvis Neural
 * 6. Priorização de entregas mescladas (Multi-Stack)
 * 7. Presets rápidos de estratégia operacional (Chuva, Noturno, Tiro Curto, Centro Expandido)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun FilterSettingsScreen(
    currentCriteria: OfferFilterCriteria = OfferFilterCriteria(),
    onSaveCriteria: (OfferFilterCriteria) -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current

    // Estados locais para edição dos sliders e controles
    var minValue by remember { mutableDoubleStateOf(currentCriteria.minValue) }
    var maxDistanceKm by remember { mutableDoubleStateOf(currentCriteria.maxDistanceKm) }
    var minDeliveryBonus by remember { mutableDoubleStateOf(currentCriteria.minDeliveryBonus) }
    var minGainPerKm by remember { mutableDoubleStateOf(currentCriteria.minGainPerKm) }
    var minHourlyMultiplier by remember { mutableDoubleStateOf(currentCriteria.minHourlyMultiplier) }
    var onlyAcceptedNeural by remember { mutableStateOf(currentCriteria.onlyAcceptedNeural) }
    var onlyMultiStack by remember { mutableStateOf(currentCriteria.onlyMultiStack) }
    var safetySpeedThresholdKm by remember { mutableDoubleStateOf(currentCriteria.safetySpeedThresholdKm) }
    var isAutoAcceptEnabled by remember { mutableStateOf(currentCriteria.isAutoAcceptEnabled) }
    var autoAcceptMinGainPerKm by remember { mutableDoubleStateOf(currentCriteria.autoAcceptMinGainPerKm) }
    var isNotificationFilterEnabled by remember { mutableStateOf(currentCriteria.isNotificationFilterEnabled) }
    var notificationMinGainPerKm by remember { mutableDoubleStateOf(currentCriteria.notificationMinGainPerKm) }
    var notificationAllowMultiStackBypass by remember { mutableStateOf(currentCriteria.notificationAllowMultiStackBypass) }
    var notificationOnlyJarvisApproved by remember { mutableStateOf(currentCriteria.notificationOnlyJarvisApproved) }

    val formattedMinVal = if (minValue > 0.0) String.format(Locale("pt", "BR"), "R$ %.2f", minValue) else "Sem Mínimo"
    val formattedMaxDist = if (maxDistanceKm < 15.0) String.format(Locale("pt", "BR"), "%.1f km", maxDistanceKm) else "Sem Limite"
    val formattedBonus = if (minDeliveryBonus > 0.0) String.format(Locale("pt", "BR"), "R$ %.2f", minDeliveryBonus) else "Sem Mínimo"
    val formattedGainKm = if (minGainPerKm > 0.0) String.format(Locale("pt", "BR"), "R$ %.2f/km", minGainPerKm) else "Sem Mínimo"
    val formattedHourlyMult = if (minHourlyMultiplier > 1.0) String.format(Locale("pt", "BR"), "%.1fx (~R$ %.0f/h)", minHourlyMultiplier, 35.0 * minHourlyMultiplier) else "1.0x (Padrão)"
    val formattedSpeedLimit = "${safetySpeedThresholdKm.toInt()} km/h"
    val formattedAutoAcceptGain = String.format(Locale("pt", "BR"), "R$ %.2f/km", autoAcceptMinGainPerKm)

    Scaffold(
        containerColor = DarkBg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "🎯 FILTROS DE CORRIDA",
                                color = TextLight,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Text(
                            text = "Cockpit de Rentabilidade & Automação",
                            color = NeonGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("btn_back_filter_settings")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar ao Radar",
                            tint = TextLight
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            minValue = 0.0
                            maxDistanceKm = 8.0
                            minDeliveryBonus = 0.0
                            minGainPerKm = 0.0
                            minHourlyMultiplier = 1.0
                            onlyAcceptedNeural = false
                            onlyMultiStack = false
                            safetySpeedThresholdKm = 10.0
                            isAutoAcceptEnabled = false
                            autoAcceptMinGainPerKm = 5.0
                            HapticFeedbackHelper.vibrateClick(context)
                            Toast.makeText(context, "Filtros redefinidos para padrão!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("btn_reset_all_filters")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Redefinir Filtros",
                            tint = TextMuted
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkCardElevated
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ==========================================
            // 1. CARD HERO INFORMATIVO DO RADAR NEURAL
            // ==========================================
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFF0F1A15),
                                    Color(0xFF13221C)
                                )
                            )
                        )
                        .border(1.dp, NeonGreen.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(NeonGreen.copy(alpha = 0.15f))
                                .border(1.dp, NeonGreen.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "🛡️", fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Proteção de Custo Operacional",
                                color = TextLight,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Corridas fora dos seus parâmetros são silenciadas no HUD para evitar distrações de pilotagem.",
                                color = TextMuted,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }

            // ==========================================
            // 2. PRESETS RÁPIDOS DE ESTRATÉGIA
            // ==========================================
            item {
                Column {
                    Text(
                        text = "PRESETS RÁPIDOS DE OPERAÇÃO",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.6.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PresetStrategyCard(
                            icon = "🌧️",
                            title = "Chuva Forte",
                            details = "Min: R$ 25 • Máx: 4.5km • Mult: 1.5x (~R$52/h)",
                            badge = "TARIFA DINÂMICA",
                            badgeColor = Color(0xFF00D2FF),
                            isSelected = minValue == 25.0 && maxDistanceKm == 4.5 && minHourlyMultiplier == 1.5,
                            onClick = {
                                minValue = 25.0
                                maxDistanceKm = 4.5
                                minHourlyMultiplier = 1.5
                                minDeliveryBonus = 5.0
                                minGainPerKm = 5.5
                                onlyAcceptedNeural = true
                                HapticFeedbackHelper.vibrateSuccess(context)
                            }
                        )

                        PresetStrategyCard(
                            icon = "⚡",
                            title = "Tiro Curto",
                            details = "Min: R$ 14 • Máx: 3.0km • Mult: 1.25x (~R$44/h)",
                            badge = "ALTO GIRO",
                            badgeColor = Color(0xFFFFD700),
                            isSelected = minValue == 14.0 && maxDistanceKm == 3.0 && minHourlyMultiplier == 1.25,
                            onClick = {
                                minValue = 14.0
                                maxDistanceKm = 3.0
                                minHourlyMultiplier = 1.25
                                minDeliveryBonus = 0.0
                                minGainPerKm = 4.5
                                onlyAcceptedNeural = false
                                HapticFeedbackHelper.vibrateSuccess(context)
                            }
                        )

                        PresetStrategyCard(
                            icon = "💎",
                            title = "Máximo Lucro",
                            details = "Min: R$ 30 • Máx: 5.0km • Mult: 2.0x (~R$70/h)",
                            badge = "ELITE PRO",
                            badgeColor = NeonGreen,
                            isSelected = minValue == 30.0 && maxDistanceKm == 5.0 && minHourlyMultiplier == 2.0,
                            onClick = {
                                minValue = 30.0
                                maxDistanceKm = 5.0
                                minHourlyMultiplier = 2.0
                                minDeliveryBonus = 4.0
                                minGainPerKm = 6.0
                                onlyAcceptedNeural = true
                                HapticFeedbackHelper.vibrateSuccess(context)
                            }
                        )

                        PresetStrategyCard(
                            icon = "🌙",
                            title = "Noturno / Madrugada",
                            details = "Min: R$ 20 • Máx: 6.0km • Mult: 1.5x (~R$52/h)",
                            badge = "ALTA MARGEM",
                            badgeColor = Color(0xFFB388FF),
                            isSelected = minValue == 20.0 && maxDistanceKm == 6.0 && minHourlyMultiplier == 1.5,
                            onClick = {
                                minValue = 20.0
                                maxDistanceKm = 6.0
                                minHourlyMultiplier = 1.5
                                minDeliveryBonus = 3.0
                                minGainPerKm = 4.0
                                onlyAcceptedNeural = true
                                HapticFeedbackHelper.vibrateSuccess(context)
                            }
                        )
                    }
                }
            }

            // ==========================================
            // 3. CONTROLE: VALOR MÍNIMO DE CORRIDA (R$)
            // ==========================================
            item {
                FilterSettingCard(
                    icon = "💵",
                    title = "Valor Mínimo por Corrida",
                    description = "Oculta automaticamente qualquer chamado com remuneração bruta inferior",
                    valueDisplay = formattedMinVal,
                    valueColor = NeonGreen
                ) {
                    Slider(
                        value = minValue.toFloat(),
                        onValueChange = {
                            val rounded = (it.toDouble() * 2).toInt() / 2.0
                            minValue = rounded
                        },
                        valueRange = 0f..60f,
                        steps = 29,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonGreen,
                            activeTrackColor = NeonGreen,
                            inactiveTrackColor = DarkBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("slider_screen_min_value")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        QuickChip(label = "Sem Mínimo", isSelected = minValue == 0.0) { minValue = 0.0 }
                        QuickChip(label = "R$ 15,00", isSelected = minValue == 15.0) { minValue = 15.0 }
                        QuickChip(label = "R$ 20,00", isSelected = minValue == 20.0) { minValue = 20.0 }
                        QuickChip(label = "R$ 25,00", isSelected = minValue == 25.0) { minValue = 25.0 }
                        QuickChip(label = "R$ 35,00", isSelected = minValue == 35.0) { minValue = 35.0 }
                    }
                }
            }

            // ==========================================
            // 4. CONTROLE: RAIO DE DISTÂNCIA MÁXIMO (KM)
            // ==========================================
            item {
                FilterSettingCard(
                    icon = "📍",
                    title = "Raio de Distância Máximo (km)",
                    description = "Raio viário acumulado a partir da sua localização para aceitar chamados",
                    valueDisplay = formattedMaxDist,
                    valueColor = Color(0xFF00D2FF)
                ) {
                    Slider(
                        value = maxDistanceKm.toFloat(),
                        onValueChange = {
                            val rounded = (it.toDouble() * 10).toInt() / 10.0
                            maxDistanceKm = rounded
                        },
                        valueRange = 1f..15f,
                        steps = 27,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF00D2FF),
                            activeTrackColor = Color(0xFF00D2FF),
                            inactiveTrackColor = DarkBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("slider_screen_max_distance")
                    )

                    // Indicador Visual do Raio de Cobertura
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkCardElevated)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🎯 Cobertura: ${if (maxDistanceKm < 15.0) "Raio de $formattedMaxDist" else "Sem Restrição de Raio"}",
                                color = Color(0xFF00D2FF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (maxDistanceKm < 15.0) {
                                Text(
                                    text = "~${String.format(Locale("pt", "BR"), "%.0f", Math.PI * maxDistanceKm * maxDistanceKm)} km² viários",
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        QuickChip(label = "Até 3 km", isSelected = maxDistanceKm == 3.0) { maxDistanceKm = 3.0 }
                        QuickChip(label = "Até 5 km", isSelected = maxDistanceKm == 5.0) { maxDistanceKm = 5.0 }
                        QuickChip(label = "Até 8 km", isSelected = maxDistanceKm == 8.0) { maxDistanceKm = 8.0 }
                        QuickChip(label = "Até 12 km", isSelected = maxDistanceKm == 12.0) { maxDistanceKm = 12.0 }
                        QuickChip(label = "Sem Limite", isSelected = maxDistanceKm >= 15.0) { maxDistanceKm = 15.0 }
                    }
                }
            }

            // ==========================================
            // 5. CONTROLE: MULTIPLICADOR DE GANHO POR HORA (R$/H)
            // ==========================================
            item {
                FilterSettingCard(
                    icon = "⏱️",
                    title = "Multiplicador de Ganho por Hora",
                    description = "Garante remuneração proporcional ao tempo: (Valor ÷ Minutos) × 60 min",
                    valueDisplay = formattedHourlyMult,
                    valueColor = Color(0xFF00E5FF)
                ) {
                    Slider(
                        value = minHourlyMultiplier.toFloat(),
                        onValueChange = {
                            val rounded = (it.toDouble() * 10).toInt() / 10.0
                            minHourlyMultiplier = rounded
                        },
                        valueRange = 1.0f..3.0f,
                        steps = 19,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF00E5FF),
                            activeTrackColor = Color(0xFF00E5FF),
                            inactiveTrackColor = DarkBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("slider_screen_hourly_multiplier")
                    )

                    // Demonstrativo Dinâmico da Meta Horária
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkCardElevated)
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "⚡ Meta Horária Alvo:",
                                    color = TextLight,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = String.format(Locale("pt", "BR"), "R$ %.2f / hora", 35.0 * minHourlyMultiplier),
                                    color = Color(0xFF00E5FF),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Base padrão: R$ 35,00/h. Bloqueia chamados demorados cujo ritmo de remuneração fique abaixo deste patamar.",
                                color = TextMuted,
                                fontSize = 10.sp,
                                lineHeight = 13.5.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        QuickChip(label = "1.0x (Padrão)", isSelected = minHourlyMultiplier == 1.0) { minHourlyMultiplier = 1.0 }
                        QuickChip(label = "1.25x (~R$44/h)", isSelected = minHourlyMultiplier == 1.25 || minHourlyMultiplier == 1.3) { minHourlyMultiplier = 1.25 }
                        QuickChip(label = "1.5x (~R$52/h)", isSelected = minHourlyMultiplier == 1.5) { minHourlyMultiplier = 1.5 }
                        QuickChip(label = "2.0x (~R$70/h)", isSelected = minHourlyMultiplier == 2.0) { minHourlyMultiplier = 2.0 }
                    }
                }
            }

            // ==========================================
            // 5. CONTROLE: BÔNUS POR ENTREGA (R$)
            // ==========================================
            item {
                FilterSettingCard(
                    icon = "🎁",
                    title = "Bônus por Entrega / Gorjeta Mínima",
                    description = "Prioriza corridas com incentivo de app, gorjeta declarada ou sinergia de Multi-Stack",
                    valueDisplay = formattedBonus,
                    valueColor = Color(0xFFFF80AB)
                ) {
                    Slider(
                        value = minDeliveryBonus.toFloat(),
                        onValueChange = {
                            val rounded = (it.toDouble() * 2).toInt() / 2.0
                            minDeliveryBonus = rounded
                        },
                        valueRange = 0f..20f,
                        steps = 19,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFFF80AB),
                            activeTrackColor = Color(0xFFFF80AB),
                            inactiveTrackColor = DarkBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("slider_screen_delivery_bonus")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        QuickChip(label = "Qualquer Bônus", isSelected = minDeliveryBonus == 0.0) { minDeliveryBonus = 0.0 }
                        QuickChip(label = "+ R$ 3,00", isSelected = minDeliveryBonus == 3.0) { minDeliveryBonus = 3.0 }
                        QuickChip(label = "+ R$ 5,00", isSelected = minDeliveryBonus == 5.0) { minDeliveryBonus = 5.0 }
                        QuickChip(label = "+ R$ 10,00", isSelected = minDeliveryBonus == 10.0) { minDeliveryBonus = 10.0 }
                    }
                }
            }

            // ==========================================
            // 6. CONTROLE: GANHO POR QUILÔMETRO (R$/KM)
            // ==========================================
            item {
                FilterSettingCard(
                    icon = "⚡",
                    title = "Multiplicador Ganho por Km (R$/km)",
                    description = "Regra de ouro: garante que o valor bruto cubra gasolina, desgaste e tempo",
                    valueDisplay = formattedGainKm,
                    valueColor = Color(0xFFFFD700)
                ) {
                    Slider(
                        value = minGainPerKm.toFloat(),
                        onValueChange = {
                            val rounded = (it.toDouble() * 2).toInt() / 2.0
                            minGainPerKm = rounded
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
                            .testTag("slider_screen_gain_per_km")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        QuickChip(label = "Sem Regra", isSelected = minGainPerKm == 0.0) { minGainPerKm = 0.0 }
                        QuickChip(label = "R$ 4,00/km", isSelected = minGainPerKm == 4.0) { minGainPerKm = 4.0 }
                        QuickChip(label = "R$ 5,00/km", isSelected = minGainPerKm == 5.0) { minGainPerKm = 5.0 }
                        QuickChip(label = "R$ 6,00/km", isSelected = minGainPerKm == 6.0) { minGainPerKm = 6.0 }
                    }
                }
            }

            // =========================================================================
            // 6.5. FILTRO AUTOMÁTICO DE NOTIFICAÇÕES POR PISO DE PREÇO POR KM (R$/KM)
            // =========================================================================
            item {
                PricePerKmNotificationFilterLayout(
                    criteria = OfferFilterCriteria(
                        minValue = minValue,
                        maxDistanceKm = maxDistanceKm,
                        minDeliveryBonus = minDeliveryBonus,
                        minGainPerKm = minGainPerKm,
                        minHourlyMultiplier = minHourlyMultiplier,
                        onlyAcceptedNeural = onlyAcceptedNeural,
                        onlyMultiStack = onlyMultiStack,
                        safetySpeedThresholdKm = safetySpeedThresholdKm,
                        searchQuery = currentCriteria.searchQuery,
                        isAutoAcceptEnabled = isAutoAcceptEnabled,
                        autoAcceptMinGainPerKm = autoAcceptMinGainPerKm,
                        isNotificationFilterEnabled = isNotificationFilterEnabled,
                        notificationMinGainPerKm = notificationMinGainPerKm,
                        notificationAllowMultiStackBypass = notificationAllowMultiStackBypass,
                        notificationOnlyJarvisApproved = notificationOnlyJarvisApproved
                    ),
                    onCriteriaChange = { updated ->
                        isNotificationFilterEnabled = updated.isNotificationFilterEnabled
                        notificationMinGainPerKm = updated.notificationMinGainPerKm
                        notificationAllowMultiStackBypass = updated.notificationAllowMultiStackBypass
                        notificationOnlyJarvisApproved = updated.notificationOnlyJarvisApproved
                    }
                )
            }

            // ==========================================
            // 7. SWITCHES DE AUTOMAÇÃO INTELIGENTE
            // ==========================================
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    border = BorderStroke(1.dp, DarkBorder),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "AUTOMAÇÃO DO COPILOTO",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.6.sp
                        )

                        // Switch 1: Somente Jarvis Neural Aceitar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "🧠 Somente Jarvis Aceitar",
                                    color = TextLight,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Descarta chamadas classificadas como inviáveis pela IA",
                                    color = TextMuted,
                                    fontSize = 10.5.sp
                                )
                            }
                            Switch(
                                checked = onlyAcceptedNeural,
                                onCheckedChange = { onlyAcceptedNeural = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = NeonGreen,
                                    checkedTrackColor = NeonGreen.copy(alpha = 0.35f),
                                    uncheckedThumbColor = TextMuted,
                                    uncheckedTrackColor = DarkBorder
                                ),
                                modifier = Modifier.testTag("switch_screen_jarvis_filter")
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(0.8.dp)
                                .background(DarkBorder)
                        )

                        // Switch 2: Somente Entregas Mescladas
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "✨ Somente Entregas Mescladas (Multi-Stack)",
                                    color = TextLight,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Mostra apenas trajetos combinados de 2 ou mais apps com sinergia",
                                    color = TextMuted,
                                    fontSize = 10.5.sp
                                )
                            }
                            Switch(
                                checked = onlyMultiStack,
                                onCheckedChange = { onlyMultiStack = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = NeonGreen,
                                    checkedTrackColor = NeonGreen.copy(alpha = 0.35f),
                                    uncheckedThumbColor = TextMuted,
                                    uncheckedTrackColor = DarkBorder
                                ),
                                modifier = Modifier.testTag("switch_screen_multistack_filter")
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(0.8.dp)
                                .background(DarkBorder)
                        )

                        // Switch 3: Auto-Aceite Inteligente com Filtros Pré-definidos
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isAutoAcceptEnabled) NeonGreen.copy(alpha = 0.08f) else Color.Transparent)
                                .padding(if (isAutoAcceptEnabled) 8.dp else 0.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "⚡ Auto-Aceite Inteligente",
                                            color = if (isAutoAcceptEnabled) NeonGreen else TextLight,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(if (isAutoAcceptEnabled) NeonGreen else DarkBorder)
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = if (isAutoAcceptEnabled) "ATIVO" else "OFF",
                                                color = if (isAutoAcceptEnabled) DarkBg else TextMuted,
                                                fontSize = 8.5.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Aceita automaticamente ofertas sem intervenção manual quando atingirem o ganho mínimo/km",
                                        color = TextMuted,
                                        fontSize = 10.5.sp
                                    )
                                }
                                Switch(
                                    checked = isAutoAcceptEnabled,
                                    onCheckedChange = { isAutoAcceptEnabled = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = NeonGreen,
                                        checkedTrackColor = NeonGreen.copy(alpha = 0.4f),
                                        uncheckedThumbColor = TextMuted,
                                        uncheckedTrackColor = DarkBorder
                                    ),
                                    modifier = Modifier.testTag("switch_screen_auto_accept")
                                )
                            }

                            if (isAutoAcceptEnabled) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Filtro pré-definido: Mínimo por Km",
                                        color = TextLight,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = formattedAutoAcceptGain,
                                        color = NeonGreen,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }

                                Slider(
                                    value = autoAcceptMinGainPerKm.toFloat(),
                                    onValueChange = {
                                        val rounded = (it.toDouble() * 2).toInt() / 2.0
                                        autoAcceptMinGainPerKm = rounded
                                    },
                                    valueRange = 3f..10f,
                                    steps = 13,
                                    colors = SliderDefaults.colors(
                                        thumbColor = NeonGreen,
                                        activeTrackColor = NeonGreen,
                                        inactiveTrackColor = DarkBorder
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("slider_screen_auto_accept_min_gain")
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    QuickChip(label = "R$ 4,00/km", isSelected = autoAcceptMinGainPerKm == 4.0) { autoAcceptMinGainPerKm = 4.0 }
                                    QuickChip(label = "R$ 5,00/km (Padrão)", isSelected = autoAcceptMinGainPerKm == 5.0) { autoAcceptMinGainPerKm = 5.0 }
                                    QuickChip(label = "R$ 6,50/km", isSelected = autoAcceptMinGainPerKm == 6.5) { autoAcceptMinGainPerKm = 6.5 }
                                    QuickChip(label = "R$ 8,00/km", isSelected = autoAcceptMinGainPerKm == 8.0) { autoAcceptMinGainPerKm = 8.0 }
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // 8. CONTROLE: LIMITE DE VELOCIDADE PARA TRAVA DE SEGURANÇA (KM/H)
            // ==========================================
            item {
                FilterSettingCard(
                    icon = "🛡️",
                    title = "Trava de Segurança por Velocidade",
                    description = "Bloqueia toques e alertas durante pilotagem acima do limite configurado para evitar acidentes",
                    valueDisplay = formattedSpeedLimit,
                    valueColor = RedDecline
                ) {
                    Slider(
                        value = safetySpeedThresholdKm.toFloat(),
                        onValueChange = {
                            val rounded = it.toInt().toDouble()
                            safetySpeedThresholdKm = rounded
                        },
                        valueRange = 5f..35f,
                        steps = 29,
                        colors = SliderDefaults.colors(
                            thumbColor = RedDecline,
                            activeTrackColor = RedDecline,
                            inactiveTrackColor = DarkBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("slider_screen_safety_speed_limit")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        QuickChip(label = "10 km/h (Padrão)", isSelected = safetySpeedThresholdKm == 10.0) { safetySpeedThresholdKm = 10.0 }
                        QuickChip(label = "15 km/h", isSelected = safetySpeedThresholdKm == 15.0) { safetySpeedThresholdKm = 15.0 }
                        QuickChip(label = "20 km/h", isSelected = safetySpeedThresholdKm == 20.0) { safetySpeedThresholdKm = 20.0 }
                        QuickChip(label = "25 km/h", isSelected = safetySpeedThresholdKm == 25.0) { safetySpeedThresholdKm = 25.0 }
                    }
                }
            }

            // ==========================================
            // 8.5. SIMULADOR DE CALIBRAÇÃO EM TEMPO REAL
            // ==========================================
            item {
                val sampleValue = 24.00
                val sampleDistKm = 4.2
                val sampleTimeMin = 18
                val sampleGainKm = sampleValue / sampleDistKm // ~5.71
                val sampleHourlyRate = (sampleValue / sampleTimeMin) * 60.0 // 80.00/h
                val targetHourlyRate = 35.0 * minHourlyMultiplier

                val passesValue = sampleValue >= minValue
                val passesDist = sampleDistKm <= maxDistanceKm
                val passesGainKm = minGainPerKm == 0.0 || sampleGainKm >= minGainPerKm
                val passesHourly = sampleHourlyRate >= targetHourlyRate
                val allPasses = passesValue && passesDist && passesGainKm && passesHourly

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    border = BorderStroke(1.2.dp, if (allPasses) NeonGreen.copy(alpha = 0.6f) else RedDecline.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "🧪", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "SIMULAÇÃO EM TEMPO REAL",
                                    color = TextLight,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (allPasses) NeonGreen.copy(alpha = 0.15f) else RedDecline.copy(alpha = 0.15f))
                                    .border(0.8.dp, if (allPasses) NeonGreen else RedDecline, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (allPasses) "✅ PASSARIA NO FILTRO" else "❌ BLOQUEADA",
                                    color = if (allPasses) NeonGreen else RedDecline,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Dados da Corrida de Teste
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(DarkBg)
                                .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "🍔 Burger King • iFood + Rappi",
                                        color = TextLight,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "R$ 24,00",
                                        color = NeonGreen,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "📍 4.2 km • ⏱️ 18 min",
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = "⚡ R$ 5,71/km • ⏱️ R$ 80,00/h",
                                        color = Color(0xFF00E5FF),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Status individual dos 3 critérios principais
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = if (passesValue) "✔" else "✖", color = if (passesValue) NeonGreen else RedDecline, fontSize = 11.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Valor: R$ 24,00 ${if (passesValue) "≥" else "<"} Mínimo $formattedMinVal",
                                    color = if (passesValue) TextLight else RedDecline,
                                    fontSize = 11.sp
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = if (passesDist) "✔" else "✖", color = if (passesDist) NeonGreen else RedDecline, fontSize = 11.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Distância: 4.2 km ${if (passesDist) "≤" else ">"} Raio Máximo $formattedMaxDist",
                                    color = if (passesDist) TextLight else RedDecline,
                                    fontSize = 11.sp
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = if (passesHourly) "✔" else "✖", color = if (passesHourly) NeonGreen else RedDecline, fontSize = 11.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Ganho/Hora: R$ 80,00/h ${if (passesHourly) "≥" else "<"} Meta ${String.format(Locale("pt", "BR"), "R$ %.2f/h", targetHourlyRate)}",
                                    color = if (passesHourly) TextLight else RedDecline,
                                    fontSize = 11.sp
                                )
                            }
                            if (isAutoAcceptEnabled) {
                                val passesAuto = sampleGainKm >= autoAcceptMinGainPerKm
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = if (passesAuto) "⚡" else "⚠️", color = if (passesAuto) NeonGreen else RedDecline, fontSize = 11.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Auto-Aceite: R$ 5,71/km ${if (passesAuto) "≥" else "<"} Gatilho $formattedAutoAcceptGain (${if (passesAuto) "Aceite Automático Dispararia!" else "Requer toque manual"})",
                                        color = if (passesAuto) NeonGreen else RedDecline,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // 9. BOTÕES DE SALVAMENTO E PERSISTÊNCIA
            // ==========================================
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            minValue = 0.0
                            maxDistanceKm = 8.0
                            minDeliveryBonus = 0.0
                            minGainPerKm = 0.0
                            minHourlyMultiplier = 1.0
                            onlyAcceptedNeural = false
                            onlyMultiStack = false
                            safetySpeedThresholdKm = 10.0
                            isAutoAcceptEnabled = false
                            autoAcceptMinGainPerKm = 5.0
                            isNotificationFilterEnabled = true
                            notificationMinGainPerKm = 5.0
                            notificationAllowMultiStackBypass = true
                            notificationOnlyJarvisApproved = false
                            HapticFeedbackHelper.vibrateClick(context)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RedDecline),
                        border = BorderStroke(1.dp, RedDecline.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = "LIMPAR",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Button(
                        onClick = {
                            val newCriteria = OfferFilterCriteria(
                                minValue = minValue,
                                maxDistanceKm = maxDistanceKm,
                                minDeliveryBonus = minDeliveryBonus,
                                minGainPerKm = minGainPerKm,
                                minHourlyMultiplier = minHourlyMultiplier,
                                onlyAcceptedNeural = onlyAcceptedNeural,
                                onlyMultiStack = onlyMultiStack,
                                safetySpeedThresholdKm = safetySpeedThresholdKm,
                                searchQuery = currentCriteria.searchQuery,
                                isAutoAcceptEnabled = isAutoAcceptEnabled,
                                autoAcceptMinGainPerKm = autoAcceptMinGainPerKm,
                                isNotificationFilterEnabled = isNotificationFilterEnabled,
                                notificationMinGainPerKm = notificationMinGainPerKm,
                                notificationAllowMultiStackBypass = notificationAllowMultiStackBypass,
                                notificationOnlyJarvisApproved = notificationOnlyJarvisApproved
                            )
                            // Salva no SharedPreferences
                            FilterPreferencesManager.saveCriteria(context, newCriteria)
                            onSaveCriteria(newCriteria)
                            HapticFeedbackHelper.vibrateSuccess(context)
                            Toast.makeText(context, "Filtros aplicados com sucesso!", Toast.LENGTH_SHORT).show()
                            onNavigateBack()
                        },
                        modifier = Modifier
                            .weight(1.6f)
                            .height(52.dp)
                            .testTag("btn_save_filter_settings"),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = "💾 SALVAR E APLICAR",
                            color = DarkBg,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// COMPONENTES AUXILIARES DE DESIGN
// ----------------------------------------------------

@Composable
private fun FilterSettingCard(
    icon: String,
    title: String,
    description: String,
    valueDisplay: String,
    valueColor: Color,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        border = BorderStroke(1.dp, DarkBorder),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = icon, fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = title,
                            color = TextLight,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = description,
                            color = TextMuted,
                            fontSize = 10.5.sp,
                            lineHeight = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = valueDisplay,
                    color = valueColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            content()
        }
    }
}

@Composable
private fun PresetStrategyCard(
    icon: String,
    title: String,
    details: String,
    badge: String,
    badgeColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(220.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) DarkCardElevated else DarkCard)
            .border(
                1.2.dp,
                if (isSelected) NeonGreen else DarkBorder,
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = icon, fontSize = 20.sp)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeColor.copy(alpha = 0.15f))
                        .border(0.8.dp, badgeColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badge,
                        color = badgeColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                color = TextLight,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = details,
                color = TextMuted,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun QuickChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) NeonGreen.copy(alpha = 0.2f) else DarkCardElevated)
            .border(
                1.dp,
                if (isSelected) NeonGreen else DarkBorder,
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) NeonGreen else TextMuted,
            fontSize = 10.5.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
