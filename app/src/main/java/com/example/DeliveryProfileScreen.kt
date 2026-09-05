package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

/**
 * TELA DE PERFIL DO ENTREGADOR COM HISTÓRICO SIMPLES DE DECISÕES
 * Exibe as informações do motorista/motoboy e o histórico de logs internos
 * registrando detalhadamente cada evento de aceitação e rejeição de ofertas.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DeliveryProfileScreen(
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val logsList = OfferDecisionLogManager.logs

    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, ACCEPTED, DECLINED
    var showClearDialog by remember { mutableStateOf(false) }

    val filteredLogs = remember(logsList.size, selectedFilter) {
        when (selectedFilter) {
            "ACCEPTED" -> logsList.filter { it.action == DecisionAction.ACCEPTED }
            "DECLINED" -> logsList.filter { it.action == DecisionAction.DECLINED }
            else -> logsList.toList()
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = {
                Text(
                    text = "Limpar Histórico de Decisões?",
                    color = TextLight,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = "Todos os registros de aceites e recusas locais serão apagados.",
                    color = TextMuted,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        OfferDecisionLogManager.clearLogs(context)
                        showClearDialog = false
                    },
                    modifier = Modifier.testTag("confirm_clear_logs")
                ) {
                    Text("Limpar", color = RedDecline, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancelar", color = TextMuted)
                }
            },
            containerColor = DarkCard
        )
    }

    Scaffold(
        containerColor = DarkBg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "PERFIL DO ENTREGADOR",
                            color = TextLight,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Histórico Interno de Decisões",
                            color = NeonGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("action_back_to_dashboard")
                    ) {
                        Text(text = "⬅️", fontSize = 20.sp)
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showClearDialog = true },
                        modifier = Modifier.testTag("action_clear_logs")
                    ) {
                        Text(text = "🗑️", fontSize = 18.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkCard
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 14.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. CARD PRINCIPAL DO PERFIL DO ENTREGADOR
            item {
                UserProfileHeaderCard()
            }

            // 2. MÉTRICAS AGREGADAS DO HISTÓRICO DE DECISÕES
            item {
                DecisionStatsDashboardCard(
                    totalAccepted = OfferDecisionLogManager.getTotalAccepted(),
                    totalDeclined = OfferDecisionLogManager.getTotalDeclined(),
                    acceptanceRate = OfferDecisionLogManager.getAcceptanceRate(),
                    totalValueAccepted = OfferDecisionLogManager.getTotalAcceptedValue()
                )
            }

            // 3. BARRA DE FILTROS DO HISTÓRICO
            item {
                DecisionFilterChipsRow(
                    selectedFilter = selectedFilter,
                    onFilterChange = { selectedFilter = it },
                    totalAll = logsList.size,
                    totalAccepted = logsList.count { it.action == DecisionAction.ACCEPTED },
                    totalDeclined = logsList.count { it.action == DecisionAction.DECLINED }
                )
            }

            // 4. LISTA DE LOGS DE OFERTAS
            if (filteredLogs.isEmpty()) {
                item {
                    EmptyDecisionLogsState(filter = selectedFilter)
                }
            } else {
                items(filteredLogs, key = { it.id }) { logItem ->
                    DecisionLogItemCard(log = logItem)
                }
            }
        }
    }
}

/**
 * Card de Informações Pessoais do Entregador
 */
@Composable
fun UserProfileHeaderCard() {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DarkBorder, RoundedCornerShape(20.dp))
            .testTag("profile_header_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Avatar com Indicador Ativo
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E2230))
                        .border(2.dp, NeonGreen, CircleShape)
                ) {
                    Text(text = "🏍️", fontSize = 26.sp)
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Thiago Silva",
                            color = TextLight,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(NeonGreen.copy(alpha = 0.2f))
                                .border(1.dp, NeonGreen, RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "PRO ⚡",
                                color = NeonGreen,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = "⭐ 4.98 (1.420 entregas) • Nível Jarvis",
                        color = Color(0xFFFFD166),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Text(
                        text = "Honda CG 160 Titan (Flex) • São Paulo, SP",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Divider(color = DarkBorder, thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // Aplicativos Vinculados
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Apps Vinculados:",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PartnerAppPill("iFood", RedIFood)
                    PartnerAppPill("Rappi", OrangeRappi)
                    PartnerAppPill("Uber", Color(0xFFFFFFFF))
                    PartnerAppPill("99", Yellow99)
                }
            }
        }
    }
}

@Composable
fun PartnerAppPill(name: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(
            text = name,
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Resumo Estatístico do Sistema de Logs
 */
@Composable
fun DecisionStatsDashboardCard(
    totalAccepted: Int,
    totalDeclined: Int,
    acceptanceRate: Double,
    totalValueAccepted: Double
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DarkBorder, RoundedCornerShape(18.dp))
            .testTag("decision_stats_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "📊", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "RESUMO DAS DECISÕES DO COCKPIT",
                        color = TextLight,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                }

                Text(
                    text = "${totalAccepted + totalDeclined} eventos",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatMiniBox(
                    title = "ACEITAS",
                    value = "$totalAccepted",
                    valueColor = NeonGreen,
                    modifier = Modifier.weight(1f)
                )
                StatMiniBox(
                    title = "RECUSADAS",
                    value = "$totalDeclined",
                    valueColor = RedDecline,
                    modifier = Modifier.weight(1f)
                )
                StatMiniBox(
                    title = "TAXA ACEITE",
                    value = String.format(Locale.GERMANY, "%.0f%%", acceptanceRate),
                    valueColor = if (acceptanceRate >= 50.0) NeonGreen else Color(0xFFFFD166),
                    modifier = Modifier.weight(1f)
                )
                StatMiniBox(
                    title = "TOTAL GANHO",
                    value = String.format(Locale.GERMANY, "R$ %.0f", totalValueAccepted),
                    valueColor = NeonGreen,
                    modifier = Modifier.weight(1.2f)
                )
            }
        }
    }
}

@Composable
fun StatMiniBox(
    title: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF141724))
            .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
            .padding(vertical = 8.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                color = TextMuted,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                color = valueColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

/**
 * Filtros de visualização do Histórico
 */
@Composable
fun DecisionFilterChipsRow(
    selectedFilter: String,
    onFilterChange: (String) -> Unit,
    totalAll: Int,
    totalAccepted: Int,
    totalDeclined: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChipTab(
            label = "Todos ($totalAll)",
            isSelected = selectedFilter == "ALL",
            activeColor = TextLight,
            onClick = { onFilterChange("ALL") },
            modifier = Modifier.weight(1f),
            tag = "filter_all"
        )
        FilterChipTab(
            label = "Aceitas ($totalAccepted)",
            isSelected = selectedFilter == "ACCEPTED",
            activeColor = NeonGreen,
            onClick = { onFilterChange("ACCEPTED") },
            modifier = Modifier.weight(1f),
            tag = "filter_accepted"
        )
        FilterChipTab(
            label = "Recusadas ($totalDeclined)",
            isSelected = selectedFilter == "DECLINED",
            activeColor = RedDecline,
            onClick = { onFilterChange("DECLINED") },
            modifier = Modifier.weight(1f),
            tag = "filter_declined"
        )
    }
}

@Composable
fun FilterChipTab(
    label: String,
    isSelected: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tag: String = ""
) {
    val bgColor = if (isSelected) activeColor.copy(alpha = 0.2f) else Color(0xFF141724)
    val borderColor = if (isSelected) activeColor else DarkBorder

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp)
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) activeColor else TextMuted,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal
        )
    }
}

/**
 * Card Individual de Registro no Log Interno
 */
@Composable
fun DecisionLogItemCard(log: OfferDecisionLog) {
    val isAccepted = log.action == DecisionAction.ACCEPTED
    val accentColor = if (isAccepted) NeonGreen else RedDecline
    val formattedValue = String.format(Locale.GERMANY, "R$ %.2f", log.value)
    val formattedGain = String.format(Locale.GERMANY, "R$ %.2f/km", log.gainPerKm)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .testTag("log_item_${log.offerId}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Linha Superior: Ação (Aceita/Recusada), App e Horário
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(accentColor.copy(alpha = 0.18f))
                            .border(1.dp, accentColor.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "${log.action.icon} ${log.action.label.uppercase()}",
                            color = accentColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = log.appName,
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🕒", fontSize = 10.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = log.timestampFormatted,
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Restaurante
            Text(
                text = log.restaurant,
                color = TextLight,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Linha de Valores: Bruto, Distância, R$/km
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F121C))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formattedValue,
                    color = TextLight,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black
                )

                Text(
                    text = "${log.distanceKm} km",
                    color = TextMuted,
                    fontSize = 11.sp
                )

                Text(
                    text = formattedGain,
                    color = if (log.gainPerKm >= 5.0) NeonGreen else Color(0xFFFFD166),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Motivo Registrado e Origem do Comando
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "💡 ${log.reason}",
                    color = if (isAccepted) NeonGreen.copy(alpha = 0.9f) else Color(0xFFFF8B94),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(6.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF1B2030))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = log.source,
                        color = TextMuted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/**
 * Estado Vazio para quando não houver logs no filtro
 */
@Composable
fun EmptyDecisionLogsState(filter: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 36.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "📋", fontSize = 38.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Nenhum evento registrado",
                color = TextLight,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (filter == "ALL") "Novas aceitações e rejeições aparecerão aqui."
                else "Nenhuma oferta encontrada para o filtro selecionado.",
                color = TextMuted,
                fontSize = 12.sp
            )
        }
    }
}
