package com.example

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

private val DarkBg = Color(0xFF090A0F)
private val DarkSurface = Color(0xFF11141C)
private val DarkSurfaceBorder = Color(0xFF1E2433)
private val NeonEmerald = Color(0xFF00FF88)
private val TikTokCyan = Color(0xFF00F2FE)
private val AmberGold = Color(0xFFFFB800)
private val CoralRed = Color(0xFFFF5252)

private const val PREFS_GOALS = "jarvis_marketing_goals_prefs"
private const val KEY_DAILY_GOAL = "key_custom_daily_goal"
private const val KEY_WEEKLY_GOAL = "key_custom_weekly_goal"
private const val KEY_GOAL_PERIOD = "key_goal_period_type"

/**
 * COMPONENTE 'MarketingGoalTrackerCard'
 *
 * Permite ao entregador definir metas personalizadas de faturamento diário ou semanal,
 * com campos de entrada interativos e destaque visual claro se o progresso está acima
 * ou abaixo do alvo estabelecido.
 */
@Composable
fun MarketingGoalTrackerCard(
    currentEarningsDaily: Double,
    currentEarningsWeekly: Double,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_GOALS, Context.MODE_PRIVATE) }

    // 0: Meta Diária, 1: Meta Semanal
    var selectedPeriod by remember { mutableIntStateOf(prefs.getInt(KEY_GOAL_PERIOD, 0)) }
    var dailyGoalInput by remember {
        mutableStateOf(String.format(Locale.US, "%.0f", prefs.getFloat(KEY_DAILY_GOAL, 250f)))
    }
    var weeklyGoalInput by remember {
        mutableStateOf(String.format(Locale.US, "%.0f", prefs.getFloat(KEY_WEEKLY_GOAL, 1500f)))
    }
    var isEditingGoal by remember { mutableStateOf(false) }

    // Sincroniza meta diária com o DailyGoalTrackerManager se disponível
    LaunchedEffect(dailyGoalInput) {
        val parsed = dailyGoalInput.toDoubleOrNull()
        if (parsed != null && parsed > 0) {
            DailyGoalTrackerManager.getInstance(context).setTargetAmount(parsed)
        }
    }

    val targetAmount = remember(selectedPeriod, dailyGoalInput, weeklyGoalInput) {
        if (selectedPeriod == 0) {
            dailyGoalInput.toDoubleOrNull() ?: 250.0
        } else {
            weeklyGoalInput.toDoubleOrNull() ?: 1500.0
        }
    }

    val currentEarned = if (selectedPeriod == 0) currentEarningsDaily else currentEarningsWeekly
    val progressFraction = if (targetAmount > 0) {
        (currentEarned / targetAmount).toFloat().coerceIn(0f, 1f)
    } else 0f
    val progressPercentage = if (targetAmount > 0) {
        (currentEarned / targetAmount) * 100.0
    } else 0.0

    val isAboveTarget = currentEarned >= targetAmount
    val difference = currentEarned - targetAmount

    // Cores de status visual
    val statusColor = when {
        isAboveTarget -> NeonEmerald
        progressPercentage >= 70.0 -> AmberGold
        else -> CoralRed
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("card_marketing_goal_tracker"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            statusColor.copy(alpha = if (isAboveTarget) 0.8f else 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 1. CABEÇALHO DO PAINEL DE METAS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(statusColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isAboveTarget) Icons.Default.EmojiEvents else Icons.Default.Flag,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "METAS PERSONALIZADAS DE GANHOS",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = if (selectedPeriod == 0) "Acompanhamento Diário do Turno" else "Acompanhamento Semanal Consolidado",
                            color = Color.Gray,
                            fontSize = 10.sp
                        )
                    }
                }

                // DESTAQUE VISUAL: BADGE DE STATUS ACIMA / ABAIXO DO ALVO
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .border(1.dp, statusColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("goal_status_badge")
                ) {
                    Text(
                        text = if (isAboveTarget) {
                            String.format(Locale("pt", "BR"), "🚀 ACIMA (+R$ %.2f)", difference)
                        } else {
                            String.format(Locale("pt", "BR"), "⚠️ ABAIXO (-R$ %.2f)", -difference)
                        },
                        color = statusColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. SELETOR DE PERÍODO: DIÁRIO vs SEMANAL
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkBg)
                    .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(10.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val periods = listOf("Meta Diária (Turno)", "Meta Semanal (7 Dias)")
                periods.forEachIndexed { idx, label ->
                    val isSelected = selectedPeriod == idx
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(7.dp))
                            .background(if (isSelected) TikTokCyan.copy(alpha = 0.18f) else Color.Transparent)
                            .clickable {
                                selectedPeriod = idx
                                prefs.edit().putInt(KEY_GOAL_PERIOD, idx).apply()
                            }
                            .padding(vertical = 7.dp)
                            .testTag(if (idx == 0) "tab_goal_daily" else "tab_goal_weekly"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) TikTokCyan else Color.Gray,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. BARRA DE PROGRESSO & COMPARATIVO VISUAL
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = String.format(
                        Locale("pt", "BR"),
                        "Realizado: R$ %.2f",
                        currentEarned
                    ),
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black
                )

                Text(
                    text = String.format(
                        Locale("pt", "BR"),
                        "Alvo: R$ %.2f (%.1f%%)",
                        targetAmount,
                        progressPercentage
                    ),
                    color = statusColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .testTag("goal_progress_indicator"),
                color = statusColor,
                trackColor = Color(0xFF1E2433),
            )

            Spacer(modifier = Modifier.height(10.dp))

            // CARD INFORMATIVO DE STATUS TÁTICO
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(statusColor.copy(alpha = 0.08f))
                    .border(1.dp, statusColor.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                    .padding(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isAboveTarget) Icons.Default.CheckCircle else Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isAboveTarget) {
                            String.format(
                                Locale("pt", "BR"),
                                "Meta batida! Você faturou R$ %.2f a mais que o alvo estabelecido.",
                                difference
                            )
                        } else {
                            String.format(
                                Locale("pt", "BR"),
                                "Faltam R$ %.2f (%.1f%%) para atingir o objetivo %s.",
                                -difference,
                                100.0 - progressPercentage,
                                if (selectedPeriod == 0) "do dia" else "da semana"
                            )
                        },
                        color = Color.White,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. CAMPO DE ENTRADA PARA DEFINIR / ALTERAR A META PERSONALIZADA
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selectedPeriod == 0) "Ajustar Meta Diária (R$):" else "Ajustar Meta Semanal (R$):",
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = if (isEditingGoal) "Ocultar Edição" else "Alterar Valor ✏️",
                    color = TikTokCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { isEditingGoal = !isEditingGoal }
                )
            }

            AnimatedVisibility(visible = isEditingGoal) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = if (selectedPeriod == 0) dailyGoalInput else weeklyGoalInput,
                            onValueChange = { newVal ->
                                val filtered = newVal.filter { it.isDigit() }
                                if (selectedPeriod == 0) {
                                    dailyGoalInput = filtered
                                } else {
                                    weeklyGoalInput = filtered
                                }
                            },
                            placeholder = { Text("Ex: 300", color = Color.Gray, fontSize = 12.sp) },
                            prefix = { Text("R$ ", color = NeonEmerald, fontWeight = FontWeight.Black, fontSize = 13.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonEmerald,
                                unfocusedBorderColor = DarkSurfaceBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = DarkBg,
                                unfocusedContainerColor = DarkBg
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("input_custom_goal_amount")
                        )

                        Button(
                            onClick = {
                                val currentVal = if (selectedPeriod == 0) dailyGoalInput else weeklyGoalInput
                                val parsed = currentVal.toDoubleOrNull() ?: if (selectedPeriod == 0) 250.0 else 1500.0
                                if (selectedPeriod == 0) {
                                    prefs.edit().putFloat(KEY_DAILY_GOAL, parsed.toFloat()).apply()
                                    DailyGoalTrackerManager.getInstance(context).setTargetAmount(parsed)
                                } else {
                                    prefs.edit().putFloat(KEY_WEEKLY_GOAL, parsed.toFloat()).apply()
                                }
                                isEditingGoal = false
                                Toast.makeText(
                                    context,
                                    "Meta ${if (selectedPeriod == 0) "diária" else "semanal"} de R$ ${String.format(Locale("pt", "BR"), "%.2f", parsed)} salva com sucesso!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier
                                .height(52.dp)
                                .testTag("btn_save_custom_goal"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonEmerald,
                                contentColor = Color.Black
                            )
                        ) {
                            Text("Salvar", fontWeight = FontWeight.Black, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // CHIPS DE METAS PRÉ-CALIBRADAS RÁPIDAS
                    Text("Valores Rápidos:", color = Color.Gray, fontSize = 10.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val presets = if (selectedPeriod == 0) {
                            listOf("150", "200", "250", "350")
                        } else {
                            listOf("1000", "1500", "2000", "2500")
                        }

                        presets.forEach { presetVal ->
                            val isPresetCurrent = if (selectedPeriod == 0) dailyGoalInput == presetVal else weeklyGoalInput == presetVal
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isPresetCurrent) NeonEmerald.copy(alpha = 0.2f) else DarkBg)
                                    .border(1.dp, if (isPresetCurrent) NeonEmerald else DarkSurfaceBorder, RoundedCornerShape(6.dp))
                                    .clickable {
                                        if (selectedPeriod == 0) {
                                            dailyGoalInput = presetVal
                                            prefs.edit().putFloat(KEY_DAILY_GOAL, presetVal.toFloat()).apply()
                                            DailyGoalTrackerManager.getInstance(context).setTargetAmount(presetVal.toDouble())
                                        } else {
                                            weeklyGoalInput = presetVal
                                            prefs.edit().putFloat(KEY_WEEKLY_GOAL, presetVal.toFloat()).apply()
                                        }
                                        Toast.makeText(context, "Meta definida para R$ $presetVal,00", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "R$ $presetVal",
                                    color = if (isPresetCurrent) NeonEmerald else Color.LightGray,
                                    fontSize = 10.sp,
                                    fontWeight = if (isPresetCurrent) FontWeight.Black else FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
