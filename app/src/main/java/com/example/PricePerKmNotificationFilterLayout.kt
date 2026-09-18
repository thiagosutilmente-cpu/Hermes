package com.example

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Evento registrado na auditoria do filtro automático de notificações.
 */
data class NotificationFilterAuditLog(
    val id: String,
    val timestampMs: Long,
    val appName: String,
    val restaurant: String,
    val value: Double,
    val distanceKm: Double,
    val pricePerKm: Double,
    val thresholdPricePerKm: Double,
    val isAllowed: Boolean,
    val reason: String
)

/**
 * Gerenciador singleton reativo para controle estatístico e auditoria
 * das notificações filtradas por piso de R$/km.
 */
object NotificationFilterManager {
    private val _blockedCount = MutableStateFlow(0)
    val blockedCount: StateFlow<Int> = _blockedCount.asStateFlow()

    private val _allowedCount = MutableStateFlow(0)
    val allowedCount: StateFlow<Int> = _allowedCount.asStateFlow()

    private val _recentAuditLogs = MutableStateFlow<List<NotificationFilterAuditLog>>(emptyList())
    val recentAuditLogs: StateFlow<List<NotificationFilterAuditLog>> = _recentAuditLogs.asStateFlow()

    private val _lastSimulatedFeedback = MutableStateFlow<String?>(null)
    val lastSimulatedFeedback: StateFlow<String?> = _lastSimulatedFeedback.asStateFlow()

    fun evaluateAndRecord(
        offer: RadarOffer,
        criteria: OfferFilterCriteria
    ): Boolean {
        val passes = criteria.matchesNotification(offer)
        val reason = if (passes) {
            if (offer.isMultiStack && criteria.notificationAllowMultiStackBypass) {
                "Multi-Stack Liberado (R$ %.2f/km)".format(Locale.GERMANY, offer.gainPerKm)
            } else {
                "Piso atingido: R$ %.2f/km ≥ R$ %.2f/km".format(
                    Locale.GERMANY,
                    offer.gainPerKm,
                    criteria.notificationMinGainPerKm
                )
            }
        } else {
            if (criteria.notificationOnlyJarvisApproved && !offer.neuralDecision.isAccept) {
                "Recusado pela IA Jarvis"
            } else {
                "Abaixo do Piso: R$ %.2f/km < R$ %.2f/km".format(
                    Locale.GERMANY,
                    offer.gainPerKm,
                    criteria.notificationMinGainPerKm
                )
            }
        }

        val log = NotificationFilterAuditLog(
            id = "audit_${System.currentTimeMillis()}_${offer.id}",
            timestampMs = System.currentTimeMillis(),
            appName = offer.appName,
            restaurant = offer.restaurant,
            value = offer.value,
            distanceKm = offer.distanceKm,
            pricePerKm = offer.gainPerKm,
            thresholdPricePerKm = criteria.notificationMinGainPerKm,
            isAllowed = passes,
            reason = reason
        )

        if (passes) {
            _allowedCount.value += 1
        } else {
            _blockedCount.value += 1
        }

        val currentList = _recentAuditLogs.value.toMutableList()
        currentList.add(0, log)
        if (currentList.size > 20) {
            _recentAuditLogs.value = currentList.take(20)
        } else {
            _recentAuditLogs.value = currentList
        }

        return passes
    }

    fun recordBlocked(offer: RadarOffer, thresholdPricePerKm: Double) {
        _blockedCount.value += 1
        val log = NotificationFilterAuditLog(
            id = "audit_block_${System.currentTimeMillis()}_${offer.id}",
            timestampMs = System.currentTimeMillis(),
            appName = offer.appName,
            restaurant = offer.restaurant,
            value = offer.value,
            distanceKm = offer.distanceKm,
            pricePerKm = offer.gainPerKm,
            thresholdPricePerKm = thresholdPricePerKm,
            isAllowed = false,
            reason = "Piso R$ %.2f/km < R$ %.2f/km".format(
                Locale.GERMANY,
                offer.gainPerKm,
                thresholdPricePerKm
            )
        )
        val currentList = _recentAuditLogs.value.toMutableList()
        currentList.add(0, log)
        _recentAuditLogs.value = currentList.take(20)
    }

    fun setSimulatedFeedback(msg: String?) {
        _lastSimulatedFeedback.value = msg
    }

    fun resetStats() {
        _blockedCount.value = 0
        _allowedCount.value = 0
        _recentAuditLogs.value = emptyList()
        _lastSimulatedFeedback.value = null
    }
}

/**
 * Componente de Layout Jetpack Compose para Configuração do Limite Mínimo de Preço por Quilômetro (R$/km)
 * que filtra notificações de chamadas de entrega automaticamente.
 */
@Composable
fun PricePerKmNotificationFilterLayout(
    criteria: OfferFilterCriteria,
    onCriteriaChange: (OfferFilterCriteria) -> Unit,
    modifier: Modifier = Modifier,
    isGloveMode: Boolean = false,
    onTestNotificationAllowed: ((RadarOffer) -> Unit)? = null,
    onTestNotificationBlocked: ((RadarOffer) -> Unit)? = null,
    onDismiss: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val blockedCount by NotificationFilterManager.blockedCount.collectAsState()
    val allowedCount by NotificationFilterManager.allowedCount.collectAsState()
    val auditLogs by NotificationFilterManager.recentAuditLogs.collectAsState()
    val simulatedFeedback by NotificationFilterManager.lastSimulatedFeedback.collectAsState()

    var showAuditHistory by remember { mutableStateOf(false) }
    var showAdvancedRules by remember { mutableStateOf(false) }

    val currentThreshold = criteria.notificationMinGainPerKm
    val isFilterActive = criteria.isNotificationFilterEnabled

    // Classificação visual de rentabilidade do piso selecionado
    val (tierLabel, tierColor, tierIcon, tierDescription) = when {
        !isFilterActive -> Quadruple(
            "FILTRO DESATIVADO",
            TextMuted,
            "⚪",
            "Todas as notificações serão recebidas normalmente sem restrição."
        )
        currentThreshold < 4.0 -> Quadruple(
            "MARGEM DE RISCO",
            Color(0xFFFFB74D),
            "⚠️",
            "Piso baixo: após desconto de combustível e manutenção, a margem líquida é reduzida."
        )
        currentThreshold <= 5.5 -> Quadruple(
            "PADRÃO RECOMENDADO",
            Color(0xFF00D2FF),
            "⚡",
            "Equilíbrio ideal entre volume de pedidos e ganho justo por quilômetro rodado."
        )
        currentThreshold <= 7.0 -> Quadruple(
            "ALTA LUCRATIVIDADE",
            NeonGreen,
            "🚀",
            "Excelente rentabilidade: filtra corridas medianas e prioriza pedidos lucrativos."
        )
        else -> Quadruple(
            "ULTRA SELETIVO",
            Color(0xFFFFD700),
            "💎",
            "Filtro agressivo: ideal para chuva, horários de pico e dias de alta demanda."
        )
    }

    val cardBorderColor by animateColorAsState(
        targetValue = if (isFilterActive) NeonGreen.copy(alpha = 0.65f) else DarkBorder,
        animationSpec = tween(300),
        label = "filterCardBorder"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        border = BorderStroke(1.2.dp, cardBorderColor),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("layout_price_per_km_notification_filter")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isGloveMode) 18.dp else 16.dp)
        ) {
            // Faixa de destaque no topo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                if (isFilterActive) NeonGreen else TextMuted,
                                Color(0xFF00D2FF),
                                Color(0xFFFFD700)
                            )
                        )
                    )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 1. CABEÇALHO DO FILTRO: Título, Ícone e Master Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                            .background(if (isFilterActive) NeonGreen.copy(alpha = 0.15f) else DarkBorder)
                            .border(1.dp, if (isFilterActive) NeonGreen else TextMuted, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = if (isFilterActive) "🛡️" else "🔔", fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "FILTRO DE PREÇO/KM",
                                color = if (isFilterActive) NeonGreen else TextLight,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                            if (isFilterActive) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(NeonGreen.copy(alpha = 0.2f))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "ATIVO",
                                        color = NeonGreen,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }
                        Text(
                            text = "Filtra notificações abaixo do seu piso por km",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                // Switch Principal de Ativação do Filtro de Notificação
                Switch(
                    checked = isFilterActive,
                    onCheckedChange = { active ->
                        HapticFeedbackHelper.vibrateTap(context)
                        val updated = criteria.copy(isNotificationFilterEnabled = active)
                        onCriteriaChange(updated)
                        FilterPreferencesManager.saveCriteria(context, updated)
                        val msg = if (active) {
                            "Filtro de notificações ativado: Mínimo R$ %.2f/km".format(
                                Locale.GERMANY,
                                updated.notificationMinGainPerKm
                            )
                        } else {
                            "Filtro de notificações desativado. Todas chamadas serão notificadas."
                        }
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = DarkBg,
                        checkedTrackColor = NeonGreen,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = DarkBorder
                    ),
                    modifier = Modifier.testTag("switch_notification_filter")
                )
            }

            // 2. CORPO EXPANSÍVEL: CONFIGURAÇÃO DE PISO E GATILHOS (QUANDO ATIVO)
            AnimatedVisibility(
                visible = isFilterActive,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(14.dp))

                    // HERO: VALOR DO PISO MÍNIMO EM GRANDE DESTAQUE
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkCardElevated)
                            .border(1.dp, tierColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "LIMITE MÍNIMO DE NOTIFICAÇÃO",
                                color = TextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "R$ ",
                                    color = tierColor,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                Text(
                                    text = String.format(Locale.GERMANY, "%.2f", currentThreshold),
                                    color = tierColor,
                                    fontSize = if (isGloveMode) 40.sp else 36.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = (-0.5).sp,
                                    modifier = Modifier.testTag("text_current_threshold_value")
                                )
                                Text(
                                    text = " / km",
                                    color = TextMuted,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Status Pill
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(tierColor.copy(alpha = 0.15f))
                                    .border(1.dp, tierColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                    .padding(horizontal = 10.dp, vertical = 3.dp)
                            ) {
                                Text(text = tierIcon, fontSize = 11.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = tierLabel,
                                    color = tierColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = tierDescription,
                                color = TextMuted,
                                fontSize = 10.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3. SLIDER INTERATIVO COM AJUSTE CONTÍNUO (R$ 3,00/km até R$ 10,00/km)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "R$ 3,00/km",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Ajuste Fino de Notificação",
                            color = TextLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "R$ 10,00/km",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Slider(
                        value = currentThreshold.toFloat(),
                        onValueChange = { raw ->
                            val rounded = (raw * 4).roundToInt() / 4.0 // Incrementos de R$ 0,25
                            val updated = criteria.copy(notificationMinGainPerKm = rounded)
                            onCriteriaChange(updated)
                            FilterPreferencesManager.saveCriteria(context, updated)
                        },
                        valueRange = 3.0f..10.0f,
                        steps = 27, // (10.0 - 3.0) / 0.25 - 1 = 27 passos
                        colors = SliderDefaults.colors(
                            thumbColor = NeonGreen,
                            activeTrackColor = NeonGreen,
                            inactiveTrackColor = DarkBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("slider_price_per_km_threshold")
                    )

                    // 4. BOTOES DE AJUSTE PASSO A PASSO (STEPPERS - PERFEITO PARA PILOTAGEM COM LUVA)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Botão Diminuir 0,25
                        OutlinedButton(
                            onClick = {
                                HapticFeedbackHelper.vibrateTap(context)
                                val next = (currentThreshold - 0.25).coerceAtLeast(3.0)
                                val updated = criteria.copy(notificationMinGainPerKm = next)
                                onCriteriaChange(updated)
                                FilterPreferencesManager.saveCriteria(context, updated)
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFB74D)),
                            border = BorderStroke(1.dp, Color(0xFFFFB74D).copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(if (isGloveMode) 52.dp else 42.dp)
                                .testTag("btn_threshold_minus")
                        ) {
                            Text(
                                text = "➖ R$ 0,25",
                                fontWeight = FontWeight.Bold,
                                fontSize = if (isGloveMode) 12.sp else 11.sp
                            )
                        }

                        // Botão Aumentar 0,25
                        OutlinedButton(
                            onClick = {
                                HapticFeedbackHelper.vibrateTap(context)
                                val next = (currentThreshold + 0.25).coerceAtMost(10.0)
                                val updated = criteria.copy(notificationMinGainPerKm = next)
                                onCriteriaChange(updated)
                                FilterPreferencesManager.saveCriteria(context, updated)
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonGreen),
                            border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(if (isGloveMode) 52.dp else 42.dp)
                                .testTag("btn_threshold_plus")
                        ) {
                            Text(
                                text = "➕ R$ 0,25",
                                fontWeight = FontWeight.Bold,
                                fontSize = if (isGloveMode) 12.sp else 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 5. CHIPS DE ATALHOS RÁPIDOS (PRESETS DE MERCADO)
                    Text(
                        text = "PRESETS RÁPIDOS DE MERCADO",
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        QuickThresholdChip(
                            label = "R$ 4,00/km (Urbano)",
                            isSelected = currentThreshold == 4.0,
                            tag = "chip_preset_400"
                        ) {
                            HapticFeedbackHelper.vibrateTap(context)
                            val updated = criteria.copy(notificationMinGainPerKm = 4.0)
                            onCriteriaChange(updated)
                            FilterPreferencesManager.saveCriteria(context, updated)
                        }
                        QuickThresholdChip(
                            label = "R$ 5,00/km (Recomendado)",
                            isSelected = currentThreshold == 5.0,
                            tag = "chip_preset_500"
                        ) {
                            HapticFeedbackHelper.vibrateTap(context)
                            val updated = criteria.copy(notificationMinGainPerKm = 5.0)
                            onCriteriaChange(updated)
                            FilterPreferencesManager.saveCriteria(context, updated)
                        }
                        QuickThresholdChip(
                            label = "R$ 6,50/km (Chuva)",
                            isSelected = currentThreshold == 6.5,
                            tag = "chip_preset_650"
                        ) {
                            HapticFeedbackHelper.vibrateTap(context)
                            val updated = criteria.copy(notificationMinGainPerKm = 6.5)
                            onCriteriaChange(updated)
                            FilterPreferencesManager.saveCriteria(context, updated)
                        }
                        QuickThresholdChip(
                            label = "R$ 8,00/km (Super Lucro)",
                            isSelected = currentThreshold == 8.0,
                            tag = "chip_preset_800"
                        ) {
                            HapticFeedbackHelper.vibrateTap(context)
                            val updated = criteria.copy(notificationMinGainPerKm = 8.0)
                            onCriteriaChange(updated)
                            FilterPreferencesManager.saveCriteria(context, updated)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 6. REGRAS AVANÇADAS DO FILTRO (TOGGLES DE BYPASS MULTI-STACK E JARVIS)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkBg)
                            .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
                            .clickable { showAdvancedRules = !showAdvancedRules }
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "⚙️", fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Regras Avançadas de Exceção",
                                    color = TextLight,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = if (showAdvancedRules) "Ocultar ▲" else "Configurar ▼",
                                color = Color(0xFF00D2FF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    AnimatedVisibility(visible = showAdvancedRules) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(DarkBg.copy(alpha = 0.7f))
                                .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            // Regra 1: Permitir Multi-Stack mesmo abaixo do piso isolado
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Bypass de Corridas Mescladas",
                                        color = TextLight,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Permite notificações de Multi-Stack se o valor acumulado for compensatório.",
                                        color = TextMuted,
                                        fontSize = 9.5.sp
                                    )
                                }
                                Switch(
                                    checked = criteria.notificationAllowMultiStackBypass,
                                    onCheckedChange = { active ->
                                        HapticFeedbackHelper.vibrateTap(context)
                                        val updated = criteria.copy(notificationAllowMultiStackBypass = active)
                                        onCriteriaChange(updated)
                                        FilterPreferencesManager.saveCriteria(context, updated)
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = DarkBg,
                                        checkedTrackColor = NeonGreen
                                    ),
                                    modifier = Modifier.testTag("switch_multistack_bypass")
                                )
                            }

                            HorizontalDivider(
                                color = DarkBorder,
                                thickness = 0.8.dp,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )

                            // Regra 2: Somente com recomendação de Aceitar da IA Jarvis
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Exigir Selo Aceite Jarvis Neural",
                                        color = TextLight,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Bloqueia alertas de pedidos que a IA classificou como Recusar.",
                                        color = TextMuted,
                                        fontSize = 9.5.sp
                                    )
                                }
                                Switch(
                                    checked = criteria.notificationOnlyJarvisApproved,
                                    onCheckedChange = { active ->
                                        HapticFeedbackHelper.vibrateTap(context)
                                        val updated = criteria.copy(notificationOnlyJarvisApproved = active)
                                        onCriteriaChange(updated)
                                        FilterPreferencesManager.saveCriteria(context, updated)
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = DarkBg,
                                        checkedTrackColor = NeonGreen
                                    ),
                                    modifier = Modifier.testTag("switch_jarvis_only_notif")
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 7. SIMULADOR E TESTE INTERATIVO DO FILTRO EM TEMPO REAL
                    Text(
                        text = "TESTAR FILTRO DE NOTIFICAÇÃO (SIMULAÇÃO)",
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Teste 1: Oferta Baixa (ex: R$ 3,50/km) -> DEVE SER BARRADA
                        Button(
                            onClick = {
                                val lowOffer = RadarOffer(
                                    id = "sim_low_${System.currentTimeMillis() % 1000}",
                                    appName = "iFood",
                                    restaurant = "Pastelaria do Bairro",
                                    value = 14.00,
                                    distanceKm = 4.0,
                                    estimatedTimeMin = 14,
                                    neuralDecision = NeuralDecision(RadarDecision.DECLINE, "Ganho baixo por km", 0.85),
                                    gainPerKm = 3.50
                                )
                                val passed = NotificationFilterManager.evaluateAndRecord(lowOffer, criteria)
                                if (!passed) {
                                    HapticFeedbackHelper.vibrateDecline(context)
                                    NotificationFilterManager.setSimulatedFeedback(
                                        "🚫 NOTIFICAÇÃO BARRADA! R$ 3,50/km está abaixo do piso de R$ %.2f/km. Zero distração!".format(
                                            Locale.GERMANY,
                                            currentThreshold
                                        )
                                    )
                                    onTestNotificationBlocked?.invoke(lowOffer)
                                } else {
                                    NotificationFilterManager.setSimulatedFeedback(
                                        "⚠️ Notificação permitida pelas regras de exceção."
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2B161B),
                                contentColor = Color(0xFFFF5252)
                            ),
                            border = BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(if (isGloveMode) 52.dp else 44.dp)
                                .testTag("btn_test_low_offer_notif")
                        ) {
                            Text(
                                text = "🚫 Testar R$ 3,50/km\n(Deve Barrar)",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                lineHeight = 13.sp
                            )
                        }

                        // Teste 2: Oferta Top (ex: R$ 8,20/km) -> DEVE DISPARAR NOTIFICAÇÃO
                        Button(
                            onClick = {
                                val highOffer = RadarOffer(
                                    id = "sim_high_${System.currentTimeMillis() % 1000}",
                                    appName = "Rappi",
                                    restaurant = "Restaurante Paris 6",
                                    value = 41.00,
                                    distanceKm = 5.0,
                                    estimatedTimeMin = 18,
                                    neuralDecision = NeuralDecision(RadarDecision.ACCEPT, "Ganho alto R$ 8.20/km", 0.98),
                                    gainPerKm = 8.20
                                )
                                val passed = NotificationFilterManager.evaluateAndRecord(highOffer, criteria)
                                if (passed) {
                                    HapticFeedbackHelper.vibrateSuccess(context)
                                    NotificationFilterManager.setSimulatedFeedback(
                                        "✅ NOTIFICAÇÃO DISPARADA! R$ 8,20/km atende ao piso de R$ %.2f/km!".format(
                                            Locale.GERMANY,
                                            currentThreshold
                                        )
                                    )
                                    onTestNotificationAllowed?.invoke(highOffer)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0E2A1C),
                                contentColor = NeonGreen
                            ),
                            border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(if (isGloveMode) 52.dp else 44.dp)
                                .testTag("btn_test_high_offer_notif")
                        ) {
                            Text(
                                text = "🔔 Testar R$ 8,20/km\n(Deve Notificar)",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                lineHeight = 13.sp
                            )
                        }
                    }

                    // Banner de Feedback do Teste Simulado
                    if (simulatedFeedback != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkBg)
                                .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = simulatedFeedback.orEmpty(),
                                    color = TextLight,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "✕",
                                    color = TextMuted,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clickable { NotificationFilterManager.setSimulatedFeedback(null) }
                                        .padding(start = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 8. BARRA DE MÉTRICAS E AUDITORIA EM TEMPO REAL
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkBg)
                            .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "BARRADAS", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = "🛡️ $blockedCount",
                                color = Color(0xFFFF5252),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(24.dp)
                                .background(DarkBorder)
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "NOTIFICADAS", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = "🔔 $allowedCount",
                                color = NeonGreen,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(24.dp)
                                .background(DarkBorder)
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "PROTEÇÃO", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            val total = blockedCount + allowedCount
                            val pct = if (total > 0) ((blockedCount.toDouble() / total) * 100).toInt() else 0
                            Text(
                                text = "$pct%",
                                color = Color(0xFF00D2FF),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Alternar visualização do histórico de auditoria
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAuditHistory = !showAuditHistory }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (showAuditHistory) "▲ Ocultar Histórico de Decisões" else "▼ Ver Histórico de Decisões (${auditLogs.size})",
                            color = Color(0xFF00D2FF),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (blockedCount > 0 || allowedCount > 0) {
                            Text(
                                text = "Limpar Estatísticas",
                                color = TextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Normal,
                                modifier = Modifier
                                    .clickable {
                                        NotificationFilterManager.resetStats()
                                        Toast.makeText(context, "Estatísticas do filtro zeradas.", Toast.LENGTH_SHORT).show()
                                    }
                                    .testTag("btn_reset_filter_stats")
                            )
                        }
                    }

                    // Feed de auditoria das últimas decisões
                    AnimatedVisibility(visible = showAuditHistory) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (auditLogs.isEmpty()) {
                                Text(
                                    text = "Nenhuma notificação processada ainda nesta sessão.",
                                    color = TextMuted,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                )
                            } else {
                                val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
                                auditLogs.take(5).forEach { log ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(DarkBg)
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = if (log.isAllowed) "✅" else "🚫",
                                                fontSize = 11.sp
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Column {
                                                Text(
                                                    text = "${log.appName} • ${log.restaurant}",
                                                    color = TextLight,
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = log.reason,
                                                    color = if (log.isAllowed) NeonGreen else Color(0xFFFF5252),
                                                    fontSize = 9.5.sp
                                                )
                                            }
                                        }
                                        Text(
                                            text = timeFormat.format(Date(log.timestampMs)),
                                            color = TextMuted,
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Ação de fechar quando for invocado como sheet ou card
            if (onDismiss != null) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMuted),
                    border = BorderStroke(1.dp, DarkBorder),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                ) {
                    Text(text = "Fechar Painel", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Chip rápido de seleção de limites pré-definidos de R$/km.
 */
@Composable
private fun QuickThresholdChip(
    label: String,
    isSelected: Boolean,
    tag: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) NeonGreen.copy(alpha = 0.25f) else DarkBg)
            .border(
                1.dp,
                if (isSelected) NeonGreen else DarkBorder,
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag(tag)
    ) {
        Text(
            text = label,
            color = if (isSelected) NeonGreen else TextLight,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium
        )
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
