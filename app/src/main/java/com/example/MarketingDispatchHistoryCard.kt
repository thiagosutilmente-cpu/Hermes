package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import java.util.Locale

private val DarkBg = Color(0xFF090A0F)
private val DarkSurface = Color(0xFF11141C)
private val DarkSurfaceBorder = Color(0xFF1E2433)
private val NeonEmerald = Color(0xFF00FF88)
private val TikTokCyan = Color(0xFF00F2FE)
private val AmberGold = Color(0xFFFFB800)

/**
 * COMPONENTE 'MarketingDispatchHistoryCard'
 *
 * Exibe o histórico persistente de disparos de marketing realizados no app,
 * detalhando data/hora, cenário de ganhos utilizado (Lucro R$, KM e R$/KM)
 * e o status de entrega do push para a barra de notificações.
 */
@Composable
fun MarketingDispatchHistoryCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val logs = MarketingDispatchLogManager.logs
    var selectedFilterIndex by remember { mutableIntStateOf(0) } // 0: Todos, 1: Alta Taxa (>= R$ 5/km), 2: Média

    // Filtragem em tempo real
    val filteredLogs = remember(logs.size, selectedFilterIndex) {
        when (selectedFilterIndex) {
            1 -> logs.filter { it.ratePerKm >= 5.0 }
            2 -> logs.filter { it.ratePerKm < 5.0 }
            else -> logs.toList()
        }
    }

    val avgRateOverall = if (logs.isNotEmpty()) logs.map { it.ratePerKm }.average() else 0.0

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("card_marketing_dispatch_history"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 1. CABEÇALHO DO HISTÓRICO
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
                            .background(TikTokCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = TikTokCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "HISTÓRICO DE DISPAROS (LOG)",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Registro persistente de cenários e status push",
                            color = Color.Gray,
                            fontSize = 10.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Badge de contagem
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF1E2433))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "${logs.size} disparos",
                            color = Color.LightGray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Botão Exportar CSV
                    IconButton(
                        onClick = {
                            val result = MarketingCsvExporter.exportToCsv(
                                context = context,
                                dispatchLogs = logs.toList()
                            )
                            if (result.success && result.file != null) {
                                Toast.makeText(
                                    context,
                                    "CSV exportado com sucesso! (${result.rowCount} registros salvos)",
                                    Toast.LENGTH_LONG
                                ).show()
                                val shareIntent = MarketingCsvExporter.createShareIntent(context, result.file)
                                context.startActivity(Intent.createChooser(shareIntent, "Abrir / Compartilhar Relatório CSV"))
                            } else {
                                Toast.makeText(
                                    context,
                                    "Falha ao exportar CSV: ${result.errorMessage ?: "Erro desconhecido"}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("btn_export_csv_history_header")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Exportar Histórico para CSV",
                            tint = NeonEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Botão Limpar Histórico
                    if (logs.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                MarketingDispatchLogManager.clearLogs(context)
                                Toast.makeText(context, "Histórico de disparos limpo com sucesso!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("btn_clear_dispatch_logs")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Limpar Histórico",
                                tint = Color.Gray,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. RÉGUA DE RESUMO ESTATÍSTICO DOS DISPAROS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkBg)
                    .border(1.dp, Color(0xFF181D28), RoundedCornerShape(10.dp))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Total Disparado", color = Color.Gray, fontSize = 9.sp)
                    Text(
                        text = "${logs.size} eventos",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Média R$/KM dos Disparos", color = Color.Gray, fontSize = 9.sp)
                    Text(
                        text = String.format(Locale("pt", "BR"), "R$ %.2f / km", avgRateOverall),
                        color = NeonEmerald,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Status de Entrega", color = Color.Gray, fontSize = 9.sp)
                    Text(
                        text = "100% Sucesso",
                        color = TikTokCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3.1 BOTÃO DEDICADO: EXPORTAR HISTÓRICO PARA CSV
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF131722))
                    .border(1.dp, Color(0xFF1F293D), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = null,
                        tint = TikTokCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Exportação de Dados (.CSV)",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Gere uma planilha com histórico e métricas de lucros/KM",
                            color = Color.Gray,
                            fontSize = 9.sp
                        )
                    }
                }

                Button(
                    onClick = {
                        val result = MarketingCsvExporter.exportToCsv(
                            context = context,
                            dispatchLogs = logs.toList()
                        )
                        if (result.success && result.file != null) {
                            Toast.makeText(
                                context,
                                "CSV gerado com sucesso! Salvo em: ${result.file.name}",
                                Toast.LENGTH_LONG
                            ).show()
                            val shareIntent = MarketingCsvExporter.createShareIntent(context, result.file)
                            context.startActivity(Intent.createChooser(shareIntent, "Exportar / Compartilhar Relatório CSV"))
                        } else {
                            Toast.makeText(
                                context,
                                "Erro na exportação: ${result.errorMessage ?: "Erro desconhecido"}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("btn_export_dispatch_history_csv"),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TikTokCyan,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = Color.Black
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Exportar CSV",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4. LISTA DE ITENS DO HISTÓRICO
            if (filteredLogs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkBg)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nenhum disparo encontrado neste filtro.\nUtilize o botão 'Simular Disparo' acima para registrar novos eventos.",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    filteredLogs.forEach { logItem ->
                        DispatchLogItemCard(logItem = logItem)
                    }
                }
            }
        }
    }
}

/**
 * Card individual representando um item do log de disparo
 */
@Composable
private fun DispatchLogItemCard(
    logItem: MarketingDispatchLog
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }

    val rateColor = when {
        logItem.ratePerKm >= 5.0 -> NeonEmerald
        logItem.ratePerKm >= 3.5 -> AmberGold
        else -> Color(0xFFFF5252)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0C0E15))
            .border(1.dp, Color(0xFF1A202C), RoundedCornerShape(10.dp))
            .clickable { isExpanded = !isExpanded }
            .padding(10.dp)
            .testTag("log_item_${logItem.id}")
    ) {
        // Linha 1: Data/Hora e Status de Entrega do Push
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "📅 ${logItem.timestampFormatted}",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Status de Entrega do Push
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (logItem.isSuccess) NeonEmerald.copy(alpha = 0.12f) else Color(0xFFFF5252).copy(alpha = 0.15f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (logItem.isSuccess) NeonEmerald else Color(0xFFFF5252),
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = logItem.deliveryStatus,
                    color = if (logItem.isSuccess) NeonEmerald else Color(0xFFFF5252),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Linha 2: Cenário de Ganhos Utilizado & Taxa R$/KM
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF1E2433))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Cenário: ${logItem.scenarioName}",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Taxa R$/KM em Destaque
            Text(
                text = String.format(Locale("pt", "BR"), "R$ %.2f / km", logItem.ratePerKm),
                color = rateColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Linha 3: Detalhamento de Lucro, KM e Combustível Poupado
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = String.format(
                    Locale("pt", "BR"),
                    "Lucro: R$ %.2f • Distância: %.1f km",
                    logItem.profitBrl,
                    logItem.distanceKm
                ),
                color = Color(0xFFCBD5E1),
                fontSize = 10.sp
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = String.format(Locale("pt", "BR"), "⛽ R$ %.2f poupados", logItem.fuelSavedBrl),
                    color = AmberGold,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // DETALHES EXPANDIDOS DO PUSH DISPARADO
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkBg)
                    .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Text(
                    text = "CONTEÚDO DO PUSH ENVIADO:",
                    color = TikTokCyan,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = logItem.notificationTitle,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = logItem.notificationBody,
                    color = Color.LightGray,
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(
                                ClipData.newPlainText(
                                    "Log Notificação",
                                    "${logItem.notificationTitle}\n\n${logItem.notificationBody}"
                                )
                            )
                            Toast.makeText(context, "Conteúdo do push copiado!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp),
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copiar Texto", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            dispatchSimulatedMarketingNotification(
                                context = context,
                                profit = logItem.profitBrl,
                                km = logItem.distanceKm,
                                ratePerKm = logItem.ratePerKm,
                                fuelSaved = logItem.fuelSavedBrl
                            )
                            // Registra novo disparo repetido
                            MarketingDispatchLogManager.recordDispatch(
                                context = context,
                                scenarioName = "${logItem.scenarioName} (Re-teste)",
                                profitBrl = logItem.profitBrl,
                                distanceKm = logItem.distanceKm,
                                ratePerKm = logItem.ratePerKm,
                                fuelSavedBrl = logItem.fuelSavedBrl,
                                notificationTitle = logItem.notificationTitle,
                                notificationBody = logItem.notificationBody,
                                deliveryStatus = "Entregue (Re-disparo)",
                                isSuccess = true
                            )
                            Toast.makeText(context, "⚡ Notificação re-disparada com sucesso!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp),
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonEmerald,
                            contentColor = Color.Black
                        )
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.Black)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Re-disparar", fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}
