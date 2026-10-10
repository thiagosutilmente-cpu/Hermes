package com.example

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

/**
 * StackedCancellationAlertCard
 *
 * Card e banner de alerta tático para resolução de cancelamentos de pedidos em corridas mescladas.
 * Avisa o motoboy imediatamente quando um dos restaurantes ou clientes cancela um item do pacote,
 * recalcula a rota, audita a rentabilidade do trecho restante e permite abortar ou prosseguir.
 */
@Composable
fun StackedCancellationAlertCard(
    offersList: MutableList<RadarOffer>,
    completedDeliveriesList: MutableList<CompletedDeliveryItem>,
    onAdjustTodayEarnings: (Double) -> Unit,
    onAdjustTotalKm: ((Double) -> Unit)? = null,
    onDecrementDeliveries: (() -> Unit)? = null,
    onClearPickupSelection: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeAlert by StackedOrderCancellationManager.activeCancellationAlert.collectAsState()
    val cancellationHistory by StackedOrderCancellationManager.cancellationHistory.collectAsState()
    var isHistoryExpanded by remember { mutableStateOf(false) }

    val pulseTransition = rememberInfiniteTransition(label = "cancelAlertPulse")
    val alertPulse by pulseTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alertPulse"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        // =========================================================================
        // 1. ALERTA ATIVO QUANDO HOUVER CANCELAMENTO EM ANDAMENTO
        // =========================================================================
        AnimatedVisibility(
            visible = activeAlert != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            activeAlert?.let { alert ->
                val borderColor = if (alert.isViable) Color(0xFFFFB800) else Color(0xFFEF4444)
                val headerBg = if (alert.isViable) Color(0xFF1E1400) else Color(0xFF260A0A)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("stacked_cancellation_alert_banner"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0F14)),
                    border = androidx.compose.foundation.BorderStroke(1.8.dp, borderColor)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        // Linha Superior: Cabeçalho com Ícone e Botão Fechar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(borderColor.copy(alpha = 0.2f))
                                        .border(1.dp, borderColor, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = borderColor,
                                        modifier = Modifier
                                            .size(20.dp)
                                            .scale(alertPulse)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = alert.verdictTitle,
                                        color = borderColor,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 0.5.sp
                                    )
                                    Text(
                                        text = "Cancelamento em Corrida Mesclada • ${alert.timestampFormatted}",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 10.5.sp
                                    )
                                }
                            }

                            IconButton(
                                onClick = { StackedOrderCancellationManager.dismissAlert() },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Explicação Clara da Ocorrência
                        Text(
                            text = alert.explanation,
                            color = Color(0xFFF1F5F9),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 17.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Comparativo Lado a Lado: Cancelado vs Restante
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Coluna 1: Cancelado
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF1E1010))
                                    .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "❌ CANCELADO",
                                        color = Color(0xFFEF4444),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = alert.cancelledRestaurant,
                                        color = Color.White,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = String.format(Locale("pt", "BR"), "- R$ %.2f (%s)", alert.cancelledValue, alert.cancelledApp),
                                        color = Color(0xFFEF4444),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Coluna 2: Restante Recalculado
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (alert.isViable) Color(0xFF0A1A12) else Color(0xFF1E1010))
                                    .border(
                                        1.dp,
                                        if (alert.isViable) Color(0xFF00FF88).copy(alpha = 0.4f) else Color(0xFFEF4444).copy(alpha = 0.4f),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Text(
                                        text = if (alert.isViable) "🔄 ROTA RESTANTE" else "⚠️ RESTANTE (DEFICITÁRIO)",
                                        color = if (alert.isViable) Color(0xFF00FF88) else Color(0xFFEF4444),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = alert.remainingRestaurants,
                                        color = Color.White,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = String.format(Locale("pt", "BR"), "R$ %.2f (R$ %.2f/km)", alert.remainingValue, alert.remainingGainPerKm),
                                        color = if (alert.isViable) Color(0xFF00FF88) else Color(0xFFEF4444),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Botões de Ação para o Condutor
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (alert.isViable) {
                                Button(
                                    onClick = {
                                        StackedOrderCancellationManager.applyCancellationToState(
                                            context = context,
                                            event = alert,
                                            offersList = offersList,
                                            completedDeliveriesList = completedDeliveriesList,
                                            onAdjustTodayEarnings = onAdjustTodayEarnings,
                                            onAdjustTotalKm = onAdjustTotalKm,
                                            onDecrementDeliveries = onDecrementDeliveries,
                                            onClearPickupSelection = onClearPickupSelection
                                        )
                                        StackedOrderCancellationManager.dismissAlert()
                                        HapticFeedbackHelper.vibrateAccept(context)
                                        Toast.makeText(context, "✅ Rota restante confirmada com sucesso!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier
                                        .weight(1.3f)
                                        .height(40.dp)
                                        .testTag("btn_accept_remaining_stack_route"),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF88))
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("PROSSEGUIR ROTA", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }

                            Button(
                                onClick = {
                                    StackedOrderCancellationManager.applyCancellationToState(
                                        context = context,
                                        event = alert.copy(remainingOrdersCount = 0), // força cancelamento total
                                        offersList = offersList,
                                        completedDeliveriesList = completedDeliveriesList,
                                        onAdjustTodayEarnings = onAdjustTodayEarnings,
                                        onAdjustTotalKm = onAdjustTotalKm,
                                        onDecrementDeliveries = onDecrementDeliveries,
                                        onClearPickupSelection = onClearPickupSelection
                                    )
                                    StackedOrderCancellationManager.dismissAlert()
                                    HapticFeedbackHelper.vibrateDecline(context)
                                    Toast.makeText(context, "🚫 Corrida cancelada. Retornando ao radar.", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .testTag("btn_abort_remaining_stack_route"),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                            ) {
                                Icon(Icons.Default.Cancel, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (alert.isViable) "REJEITAR RESTANTE" else "ABORTAR CORRIDA", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // 2. BOTÃO TÁTICO PARA TESTAR CANCELAMENTO EM TEMPO REAL NO RADAR
        // =========================================================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = {
                    val event = StackedOrderCancellationManager.simulateStackCancellation(context, offersList)
                    Toast.makeText(context, "⚡ Simulação disparada: Sub-pedido cancelado!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .testTag("btn_simulate_stack_cancellation"),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB800).copy(alpha = 0.5f))
            ) {
                Icon(Icons.Default.Sync, contentDescription = null, tint = Color(0xFFFFB800), modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "SIMULAR CANCELAMENTO EM MESCLADA",
                    color = Color(0xFFFFB800),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
