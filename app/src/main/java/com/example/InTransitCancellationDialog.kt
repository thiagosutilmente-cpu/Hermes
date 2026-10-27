package com.example

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.util.Locale

private val NeonGreen = Color(0xFF00FF88)
private val CyberGold = Color(0xFFFFD700)
private val AlertRed = Color(0xFFFF4D4D)
private val CardSurface = Color(0xFF151522)
private val CardBorder = Color(0xFF28283C)
private val TextLight = Color(0xFFF0F0F8)
private val TextMuted = Color(0xFF9898B0)

@Composable
fun InTransitCancellationDialog(
    incident: MultiAppConflictResolutionEngine.ActiveCancellationIncident,
    onConfirmRecalculation: () -> Unit,
    onDismissWithoutRecalculation: () -> Unit
) {
    Dialog(onDismissRequest = onDismissWithoutRecalculation) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = CardSurface,
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, AlertRed.copy(alpha = 0.8f), RoundedCornerShape(20.dp))
                .testTag("in_transit_cancellation_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Ícone de Alerta
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(AlertRed.copy(alpha = 0.15f))
                        .border(1.dp, AlertRed.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Cancelamento",
                        tint = AlertRed,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "PEDIDO CANCELADO EM TRÂNSITO",
                    color = AlertRed,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    letterSpacing = 0.5.sp,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "O cliente cancelou a entrega da ${incident.cancelledAppName}",
                    color = TextLight,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Card com Compensação Financeira (Taxa de Deslocamento)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF221A12))
                        .border(1.dp, CyberGold.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "TAXA DE DESLOCAMENTO",
                                color = CyberGold,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "Valor a solicitar ao suporte",
                                color = TextMuted,
                                fontSize = 9.sp
                            )
                        }
                        Text(
                            text = "+ R$ ${String.format(Locale("pt", "BR"), "%.2f", incident.estimatedDisplacementFeeBrl)}",
                            color = CyberGold,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Card com a Corrida que Continua ATIVA
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF101B14))
                        .border(1.dp, NeonGreen.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "● CORRIDA ATIVA REMANESCENTE:",
                                color = NeonGreen,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${incident.remainingAppName} (${incident.remainingRestaurant})",
                                color = TextLight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "R$ ${String.format(Locale("pt", "BR"), "%.2f", incident.remainingPayoutBrl)}",
                                color = NeonGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Text(
                            text = "Nova rota direta: ${incident.recalculatedKm} km (desvio cancelado)",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Botão de Confirmação Manual para Recalcular o GPS
                Button(
                    onClick = onConfirmRecalculation,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_confirm_recalculate_gps"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
                ) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = "GPS",
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "CONFIRMAR E RECALCULAR GPS",
                        color = Color.Black,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Botão de Manter Rota Atual
                OutlinedButton(
                    onClick = onDismissWithoutRecalculation,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "MANTER ROTA ATUAL",
                        color = TextLight,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
