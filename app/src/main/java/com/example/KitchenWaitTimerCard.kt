package com.example

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val NeonGreen = Color(0xFF00FF88)
private val CyberCyan = Color(0xFF00D2FF)
private val CyberGold = Color(0xFFFFD700)
private val WarmOrange = Color(0xFFFF9100)
private val CardSurface = Color(0xFF13131D)
private val CardBorder = Color(0xFF232335)
private val TextLight = Color(0xFFF0F0F8)
private val TextMuted = Color(0xFF8888A2)

@Composable
fun KitchenWaitTimerCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val stackState by KitchenWaitTimerEngine.stackState.collectAsState()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("kitchen_wait_timer_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Brush.horizontalGradient(listOf(WarmOrange.copy(alpha = 0.6f), CyberGold.copy(alpha = 0.6f)))
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header do Cronômetro
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                Brush.linearGradient(listOf(WarmOrange.copy(alpha = 0.2f), CyberGold.copy(alpha = 0.2f)))
                            )
                            .border(1.dp, WarmOrange.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restaurant,
                            contentDescription = "Cozinha",
                            tint = WarmOrange,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "CRONÔMETRO DE COZINHA",
                                color = TextLight,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(WarmOrange.copy(alpha = 0.2f))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "ANTI-ESFRIAMENTO",
                                    color = WarmOrange,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = "Estimativa de preparo para sincronizar a retirada",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }

                // Status do Cronômetro
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (stackState.isTimerRunning) NeonGreen.copy(alpha = 0.15f) else Color(0xFF222230))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (stackState.isTimerRunning) "● AO VIVO" else "PAUSADO",
                        color = if (stackState.isTimerRunning) NeonGreen else TextMuted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Lista de Restaurantes em Preparo
            stackState.orders.forEach { order ->
                KitchenOrderRow(
                    order = order,
                    onMarkPickedUp = {
                        KitchenWaitTimerEngine.markOrderPickedUp(order.orderId)
                        HapticFeedbackHelper.vibrateSuccess(context)
                        Toast.makeText(context, "${order.restaurantName} marcado como coletado!", Toast.LENGTH_SHORT).show()
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Banner Tático com Recomendação da Ordem de Coleta
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1B1910))
                    .border(1.dp, CyberGold.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                    .padding(10.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = CyberGold,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ORDEM RECOMENDADA DE RETIRADA:",
                            color = CyberGold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = stackState.recommendedPickupSequence,
                        color = TextLight,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = stackState.tacticalAdvice,
                        color = TextMuted,
                        fontSize = 9.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Ações do Cronômetro
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        KitchenWaitTimerEngine.startTimerLoop(context)
                        HapticFeedbackHelper.vibrateTap(context)
                        try {
                            TextToSpeechManager.getInstance(context).speakVoiceAlert(
                                "Cronômetro de cozinha ativado. Outback em preparo com 5 minutos restantes. Burger King quase pronto."
                            )
                        } catch (_: Exception) {}
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF282315))
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Iniciar",
                        tint = CyberGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (stackState.isTimerRunning) "Executando" else "Iniciar Contagem",
                        color = CyberGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = {
                        KitchenWaitTimerEngine.resetScenario(fastFoodFirst = false)
                        HapticFeedbackHelper.vibrateTap(context)
                        Toast.makeText(context, "Cenário alternado: Pizza Hut + Zé Delivery", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "Alternar Cenário",
                        color = TextLight,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun KitchenOrderRow(
    order: KitchenWaitTimerEngine.KitchenOrder,
    onMarkPickedUp: () -> Unit
) {
    val isReady = order.status == KitchenWaitTimerEngine.OrderKitchenStatus.READY_AT_COUNTER
    val isPickedUp = order.status == KitchenWaitTimerEngine.OrderKitchenStatus.PICKED_UP
    val progress = if (order.initialSeconds > 0) {
        (1f - (order.remainingSeconds.toFloat() / order.initialSeconds.toFloat())).coerceIn(0f, 1f)
    } else 1f

    val accentColor = when {
        isPickedUp -> TextMuted
        isReady -> NeonGreen
        order.remainingSeconds <= 60 -> CyberGold
        else -> WarmOrange
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF161622))
            .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "[${order.appName}]",
                        color = if (order.appName.contains("iFood")) Color(0xFFEA1D2C) else CyberGold,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = order.restaurantName,
                        color = TextLight,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
                Text(
                    text = order.category.label,
                    color = TextMuted,
                    fontSize = 9.sp
                )
            }

            // Relógio / Tempo Restante
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isPickedUp) "COLETADO" else if (isReady) "PRONTO NO BALCÃO" else KitchenWaitTimerEngine.formatTime(order.remainingSeconds),
                    color = accentColor,
                    fontSize = if (isReady || isPickedUp) 10.sp else 14.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = if (isReady || isPickedUp) FontFamily.Default else FontFamily.Monospace
                )

                if (!isPickedUp) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1D2820))
                            .border(1.dp, NeonGreen.copy(alpha = 0.5f), CircleShape)
                            .clickable { onMarkPickedUp() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Pegar",
                            tint = NeonGreen,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Barra de Progresso do Preparo
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = accentColor,
            trackColor = Color(0xFF262638)
        )
    }
}
