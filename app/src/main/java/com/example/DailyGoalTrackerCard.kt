package com.example

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

private val NeonEmerald = Color(0xFF00FF88)
private val CyberCyan = Color(0xFF00E5FF)
private val GoldCup = Color(0xFFFFD700)
private val DarkCardSurface = Color(0xFF10141D)
private val CardBorderColor = Color(0xFF1F2636)
private val TextLight = Color(0xFFF3F4F8)
private val TextMuted = Color(0xFF8A93A6)

@Composable
fun DailyGoalTrackerCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val goalManager = remember { DailyGoalTrackerManager.getInstance(context) }
    val goalState by goalManager.goalState.collectAsState()

    val progressPercent = (goalState.progressFraction * 100).toInt()
    val isAchieved = goalState.isGoalAchieved

    val progressColor by animateColorAsState(
        targetValue = if (isAchieved) GoldCup else NeonEmerald,
        label = "GoalProgressColor"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("card_daily_goal_tracker"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCardSurface),
        border = androidx.compose.foundation.BorderStroke(
            1.2.dp,
            if (isAchieved) GoldCup.copy(alpha = 0.8f) else CardBorderColor
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Topo: Ícone, Título e Meta Atual
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isAchieved) GoldCup.copy(alpha = 0.2f) else NeonEmerald.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isAchieved) Icons.Default.EmojiEvents else Icons.Default.Flag,
                            contentDescription = null,
                            tint = if (isAchieved) GoldCup else NeonEmerald,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "META DIÁRIA DE GANHOS",
                                color = TextLight,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            if (isAchieved) {
                                Surface(
                                    color = GoldCup.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "BATIDA! 🏆",
                                        color = GoldCup,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = "Rastreador inteligente com alarme sonoro e por voz",
                            color = TextMuted,
                            fontSize = 10.5.sp
                        )
                    }
                }

                IconButton(
                    onClick = {
                        goalManager.resetTodayProgress()
                        Toast.makeText(context, "Progresso do dia reiniciado.", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reiniciar",
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Display de Valores: Faturamento Líquido vs Meta
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "FATURAMENTO LÍQUIDO DO TURNO",
                        color = TextMuted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = String.format(Locale.GERMANY, "R$ %.2f", goalState.netEarned),
                        color = progressColor,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "META ESTABELECIDA",
                        color = TextMuted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = String.format(Locale.GERMANY, "R$ %.2f", goalState.targetAmount),
                        color = TextLight,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Barra de Progresso
            LinearProgressIndicator(
                progress = { goalState.progressFraction.coerceIn(0f, 1f) },
                color = progressColor,
                trackColor = Color(0xFF1A2230),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$progressPercent% concluído",
                    color = progressColor,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = if (isAchieved) {
                        "Excedente: +R$ ${String.format(Locale.GERMANY, "%.2f", goalState.netEarned - goalState.targetAmount)}"
                    } else {
                        "Falta: R$ ${String.format(Locale.GERMANY, "%.2f", goalState.remainingAmount)} (~${goalState.estimatedRidesRemaining} corridas)"
                    },
                    color = if (isAchieved) GoldCup else CyberCyan,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Seletores Rápidos de Meta (R$ 150, R$ 200, R$ 250, R$ 350)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ajustar Meta:",
                    color = TextMuted,
                    fontSize = 10.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(150.0, 200.0, 250.0, 350.0).forEach { targetPreset ->
                        val isSelected = Math.abs(goalState.targetAmount - targetPreset) < 1.0
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) NeonEmerald.copy(alpha = 0.25f) else Color(0xFF151D2A),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) NeonEmerald else Color(0xFF222E42)
                            ),
                            modifier = Modifier
                                .clickable {
                                    goalManager.setTargetAmount(targetPreset)
                                    HapticFeedbackHelper.performClick(context)
                                }
                        ) {
                            Text(
                                text = "R$ ${targetPreset.toInt()}",
                                color = if (isSelected) NeonEmerald else TextMuted,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Botões de Ação: Simular Corrida e Testar Alarme de Conquista
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Simula faturamento de uma corrida (+R$ 28,50)
                Button(
                    onClick = {
                        goalManager.recordDeliveryEarnings(grossFare = 28.50, fuelCost = 4.20)
                        HapticFeedbackHelper.vibrateSuccess(context)
                        Toast.makeText(context, "+R$ 28,50 bruto adicionado ao turno!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .testTag("btn_record_sample_earning"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonEmerald,
                        contentColor = Color.Black
                    )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "+R$ 28,50 CORRIDA", fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                }

                // Testa a celebração com Alarme por Voz
                OutlinedButton(
                    onClick = {
                        goalManager.recordDeliveryEarnings(grossFare = goalState.remainingAmount + 10.0, fuelCost = 2.0)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .testTag("btn_trigger_goal_celebration"),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldCup),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldCup)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.EmojiEvents, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "BATER META 🏆", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
