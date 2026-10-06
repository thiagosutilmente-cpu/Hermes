package com.example

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.VolumeUp
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import java.util.Locale

/**
 * Filtro de exibição para auditoria de decisões
 */
enum class DecisionFilter(val label: String) {
    ALL("Todas"),
    ACCEPTED("Aceitas"),
    DECLINED("Recusadas")
}

/**
 * DecisionAuditHistoryCard
 *
 * Componente Jetpack Compose que exibe o histórico auditável de decisões tomadas (ACCEPT / REJECT),
 * mostrando:
 * - Plataforma de origem (iFood, Uber, 99Food, Rappi)
 * - Valor bruto em R$ e ganho/km calculado
 * - Motivo da decisão (análise de custo/benefício, comando de voz, velocidade etc.)
 * - Canal de ação (Toque na Tela, Comando de Voz Hands-Free, HUD Flutuante)
 * - Hora do registro e botão de reprodução em voz alta (TTS) para cada decisão
 */
@Composable
fun DecisionAuditHistoryCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        OfferDecisionLogManager.initialize(context)
    }

    val logsList = OfferDecisionLogManager.logs
    var selectedFilter by remember { mutableStateOf(DecisionFilter.ALL) }
    var isExpanded by remember { mutableStateOf(true) }

    val filteredLogs = remember(logsList.size, selectedFilter) {
        when (selectedFilter) {
            DecisionFilter.ALL -> logsList
            DecisionFilter.ACCEPTED -> logsList.filter { it.action == DecisionAction.ACCEPTED }
            DecisionFilter.DECLINED -> logsList.filter { it.action == DecisionAction.DECLINED }
        }
    }

    val totalAccepted = remember(logsList.size) {
        logsList.count { it.action == DecisionAction.ACCEPTED }
    }
    val totalDeclined = remember(logsList.size) {
        logsList.count { it.action == DecisionAction.DECLINED }
    }
    val acceptanceRate = remember(totalAccepted, totalDeclined) {
        val sum = totalAccepted + totalDeclined
        if (sum > 0) (totalAccepted * 100) / sum else 0
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(18.dp))
            .testTag("card_decision_audit_history")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // CABEÇALHO DO COMPONENTE
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF38BDF8).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Auditoria de Decisões",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "AUDITORIA DE DECISÕES (ACCEPT/REJECT)",
                                color = Color.White,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Text(
                            text = "Histórico de ofertas analisadas e critérios aplicados",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                }

                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(32.dp)
                ) {
                    Text(
                        text = if (isExpanded) "▲" else "▼",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // MÉTRICAS DE TAXA DE ACEITE
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Card 1: Aceitas
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF064E3B).copy(alpha = 0.35f))
                        .border(1.dp, Color(0xFF059669).copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Text("ACEITAS (ACCEPT)", color = Color(0xFF34D399), fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "$totalAccepted",
                                color = Color(0xFF10B981),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("corridas", color = Color(0xFF94A3B8), fontSize = 10.sp)
                        }
                    }
                }

                // Card 2: Rejeitadas
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF7F1D1D).copy(alpha = 0.35f))
                        .border(1.dp, Color(0xFFDC2626).copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Text("RECUSADAS (REJECT)", color = Color(0xFFF87171), fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "$totalDeclined",
                                color = Color(0xFFEF4444),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("dispensadas", color = Color(0xFF94A3B8), fontSize = 10.sp)
                        }
                    }
                }

                // Card 3: Taxa de Aceite
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF1E293B).copy(alpha = 0.6f))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Text("TAXA DE ACEITE", color = Color(0xFF64748B), fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "$acceptanceRate%",
                                color = Color(0xFF38BDF8),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column {
                    Spacer(modifier = Modifier.height(14.dp))

                    // BARRA DE FILTROS (TODAS / ACEITAS / RECUSADAS)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DecisionFilter.values().forEach { filter ->
                            val isSelected = selectedFilter == filter
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFF2563EB) else Color(0xFF1E293B))
                                    .clickable { selectedFilter = filter }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                    .testTag("filter_${filter.name.lowercase()}")
                            ) {
                                Text(
                                    text = filter.label,
                                    color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Text(
                            text = "${filteredLogs.size} registros",
                            color = Color(0xFF64748B),
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // LISTA DE EVENTOS AUDITÁVEIS
                    if (filteredLogs.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF090D16))
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Nenhuma decisão encontrada para o filtro selecionado.",
                                color = Color(0xFF64748B),
                                fontSize = 12.sp
                            )
                        }
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            filteredLogs.take(8).forEach { logItem ->
                                DecisionAuditItemRow(
                                    log = logItem,
                                    onSpeakItem = {
                                        val speechText = if (logItem.action == DecisionAction.ACCEPTED) {
                                            "Oferta aceita no ${logItem.appName}. ${logItem.restaurant}. Valor de ${formatSpokenMoney(logItem.value)} reais por ${logItem.distanceKm} quilômetros. Motivo: ${logItem.reason}."
                                        } else {
                                            "Oferta recusada no ${logItem.appName}. ${logItem.restaurant}. Valor de ${formatSpokenMoney(logItem.value)} reais. Motivo: ${logItem.reason}."
                                        }
                                        TextToSpeechManager.getInstance(context).speak(speechText)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Linha individual da lista de decisões com detalhes completos
 */
@Composable
private fun DecisionAuditItemRow(
    log: OfferDecisionLog,
    onSpeakItem: () -> Unit
) {
    val isAccepted = log.action == DecisionAction.ACCEPTED
    val accentColor = if (isAccepted) Color(0xFF10B981) else Color(0xFFEF4444)
    val bgColor = Color(0xFF090D16)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .padding(12.dp)
            .testTag("decision_item_${log.offerId}")
    ) {
        Column {
            // LINHA SUPERIOR: STATUS, PLATAFORMA E HORA
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(accentColor.copy(alpha = 0.2f))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isAccepted) "✅ ACCEPT" else "❌ REJECT",
                                color = accentColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Tag da Plataforma
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF1E293B))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = log.appName,
                            color = Color(0xFFE2E8F0),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Origem da decisão
                    Text(
                        text = "via ${log.source}",
                        color = Color(0xFF64748B),
                        fontSize = 9.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = log.timestampFormatted,
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = onSpeakItem,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Ouvir Decisão no Fone",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // LINHA MÉDIA: RESTAURANTE E VALOR
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = log.restaurant,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "R$ ${String.format(Locale.GERMAN, "%.2f", log.value)}",
                        color = Color(0xFF00FF88),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "(${String.format(Locale.GERMAN, "%.1f", log.distanceKm)} km • R$ ${String.format(Locale.GERMAN, "%.2f", log.gainPerKm)}/km)",
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // LINHA INFERIOR: MOTIVO DA DECISÃO
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF111827))
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Motivo: ",
                        color = Color(0xFF64748B),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = log.reason,
                        color = Color(0xFFE2E8F0),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

private fun formatSpokenMoney(value: Double): String {
    return String.format(Locale("pt", "BR"), "%.2f", value).replace(".", ",")
}
