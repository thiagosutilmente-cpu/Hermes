package com.example

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Card de Controle e Detecção Automática de Tema Escuro para Entregadores.
 * Reduz a fadiga ocular em corridas noturnas com transição automática baseada em horário
 * ou preferência do sistema operacional Android.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AutoNightThemeControlCard(
    isNightMode: Boolean,
    onThemeModeChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()

    var themeMode by remember { mutableStateOf(ThemePreferencesManager.getThemeMode(context)) }
    var nightStartHour by remember { mutableIntStateOf(ThemePreferencesManager.getNightStartHour(context)) }
    var nightEndHour by remember { mutableIntStateOf(ThemePreferencesManager.getNightEndHour(context)) }
    var isEyeFatigueEnabled by remember { mutableStateOf(ThemePreferencesManager.isEyeFatigueProtectionEnabled(context)) }
    var showScheduleDetails by remember { mutableStateOf(false) }

    // Atualiza status e sincroniza
    fun applyThemeChange(newMode: ThemeModePreference) {
        themeMode = newMode
        ThemePreferencesManager.setThemeMode(context, newMode)
        val effectiveNight = ThemePreferencesManager.resolveEffectiveNightMode(context, systemDark)
        onThemeModeChanged(effectiveNight)
        HapticFeedbackHelper.vibrateTap(context)
        Toast.makeText(context, "Modo: ${newMode.title}", Toast.LENGTH_SHORT).show()
    }

    val cardBg = if (isNightMode) Color(0xFF0C1017) else Color(0xFFF1F8E9)
    val cardBorder = if (isNightMode) Color(0xFF00FF88).copy(alpha = 0.4f) else Color(0xFF007A3D).copy(alpha = 0.5f)
    val accentColor = if (isNightMode) Color(0xFF00FF88) else Color(0xFF007A3D)
    val textPrimary = if (isNightMode) Color(0xFFFFFFFF) else Color(0xFF111827)
    val textMuted = if (isNightMode) Color(0xFF9EABB8) else Color(0xFF374151)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("banner_theme_visibility_control"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Linha do Cabeçalho
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
                            .background(if (isNightMode) Color(0xFF00FF88) else Color(0xFFE5A000))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isNightMode) "COCKPIT NOTURNO (ANTI-FADIGA OCULAR)" else "MODO SOL (ALTO CONTRASTE WCAG AAA)",
                        color = accentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.6.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(accentColor.copy(alpha = 0.15f))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = when (themeMode) {
                            ThemeModePreference.AUTO_INTELLIGENT -> "⚡ AUTO INTELIGENTE"
                            ThemeModePreference.AUTO_SCHEDULE -> "⏰ POR HORÁRIO"
                            ThemeModePreference.AUTO_SYSTEM -> "📱 SISTEMA ANDROID"
                            ThemeModePreference.MANUAL_DARK -> "🌙 FIXO NOTURNO"
                            ThemeModePreference.MANUAL_LIGHT -> "☀️ FIXO SOL"
                        },
                        color = accentColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Descrição explicativa do status atual
            Text(
                text = ThemePreferencesManager.getThemeStatusDescription(context, systemDark),
                color = textMuted,
                fontSize = 10.5.sp,
                lineHeight = 14.5.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Seletor de Modo de Tema (Chips)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ThemeModePreference.entries.forEach { mode ->
                    val isSelected = mode == themeMode
                    Surface(
                        color = if (isSelected) accentColor.copy(alpha = 0.2f) else if (isNightMode) Color(0xFF161F2B) else Color(0xFFE5E7EB),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) accentColor else if (isNightMode) Color(0xFF243242) else Color(0xFFD1D5DB)
                        ),
                        modifier = Modifier
                            .clickable { applyThemeChange(mode) }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                            .testTag("theme_chip_${mode.id}")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = mode.icon,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = mode.title,
                                color = if (isSelected) accentColor else textPrimary,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Seção de Ajuste de Turno Noturno e Proteção Anti-Fadiga
            Surface(
                color = if (isNightMode) Color(0xFF070B10) else Color(0xFFFFFFFF),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isNightMode) Color(0xFF1E293B) else Color(0xFFE2E8F0)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    // Switch de Redução de Fadiga Ocular
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.BrightnessAuto,
                                contentDescription = "Fadiga Ocular",
                                tint = accentColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "Redução Automática de Fadiga Ocular",
                                    color = textPrimary,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Ativa o tema escuro sem intervenção para evitar ofuscamento na viseira",
                                    color = textMuted,
                                    fontSize = 9.sp
                                )
                            }
                        }

                        Switch(
                            checked = isEyeFatigueEnabled,
                            onCheckedChange = { checked ->
                                isEyeFatigueEnabled = checked
                                ThemePreferencesManager.setEyeFatigueProtectionEnabled(context, checked)
                                Toast.makeText(
                                    context,
                                    if (checked) "Proteção anti-fadiga ocular ativada" else "Proteção desativada",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = if (isNightMode) Color(0xFF000000) else Color(0xFFFFFFFF),
                                checkedTrackColor = accentColor
                            ),
                            modifier = Modifier.testTag("switch_eye_fatigue")
                        )
                    }

                    // Seletor de Horário do Turno Noturno (Expandível)
                    if (themeMode == ThemeModePreference.AUTO_INTELLIGENT || themeMode == ThemeModePreference.AUTO_SCHEDULE) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = "Turno",
                                    tint = accentColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Turno Noturno: $nightStartHour:00 às 0$nightEndHour:00",
                                    color = textPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Text(
                                text = if (showScheduleDetails) "Ocultar ▲" else "Ajustar ▾",
                                color = accentColor,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { showScheduleDetails = !showScheduleDetails }
                                    .testTag("btn_toggle_schedule_hours")
                            )
                        }

                        AnimatedVisibility(
                            visible = showScheduleDetails,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Column(modifier = Modifier.padding(top = 6.dp)) {
                                Text(
                                    text = "Início do Turno da Noite (Anoitecer):",
                                    color = textMuted,
                                    fontSize = 9.sp
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    listOf(17, 18, 19, 20).forEach { hour ->
                                        val isSel = hour == nightStartHour
                                        Surface(
                                            color = if (isSel) accentColor.copy(alpha = 0.25f) else Color.Transparent,
                                            shape = RoundedCornerShape(6.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) accentColor else Color.Gray.copy(alpha = 0.4f)),
                                            modifier = Modifier
                                                .clickable {
                                                    nightStartHour = hour
                                                    ThemePreferencesManager.setNightStartHour(context, hour)
                                                    val eff = ThemePreferencesManager.resolveEffectiveNightMode(context, systemDark)
                                                    onThemeModeChanged(eff)
                                                }
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = "${hour}:00",
                                                color = if (isSel) accentColor else textPrimary,
                                                fontSize = 9.5.sp,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "Fim do Turno da Noite (Amanhecer):",
                                    color = textMuted,
                                    fontSize = 9.sp
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    listOf(5, 6, 7, 8).forEach { hour ->
                                        val isSel = hour == nightEndHour
                                        Surface(
                                            color = if (isSel) accentColor.copy(alpha = 0.25f) else Color.Transparent,
                                            shape = RoundedCornerShape(6.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) accentColor else Color.Gray.copy(alpha = 0.4f)),
                                            modifier = Modifier
                                                .clickable {
                                                    nightEndHour = hour
                                                    ThemePreferencesManager.setNightEndHour(context, hour)
                                                    val eff = ThemePreferencesManager.resolveEffectiveNightMode(context, systemDark)
                                                    onThemeModeChanged(eff)
                                                }
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = "0${hour}:00",
                                                color = if (isSel) accentColor else textPrimary,
                                                fontSize = 9.5.sp,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Botão de Alternância Rápida Manual
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = {
                        val next = !isNightMode
                        if (next) {
                            applyThemeChange(ThemeModePreference.MANUAL_DARK)
                        } else {
                            applyThemeChange(ThemeModePreference.MANUAL_LIGHT)
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_toggle_theme_banner")
                ) {
                    Text(
                        text = if (isNightMode) "☀️ Forçar Modo Sol" else "🌙 Forçar Modo Noturno",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }
            }
        }
    }
}
