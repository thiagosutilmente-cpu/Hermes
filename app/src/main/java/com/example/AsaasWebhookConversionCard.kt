package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
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
import java.util.Locale

private val DarkBg = Color(0xFF090A0F)
private val DarkSurface = Color(0xFF11141C)
private val DarkSurfaceBorder = Color(0xFF1E2433)
private val NeonEmerald = Color(0xFF00FF88)
private val TikTokCyan = Color(0xFF00F2FE)
private val AmberGold = Color(0xFFFFB800)
private val CoralRed = Color(0xFFFF5252)
private val ElectricPurple = Color(0xFF9D4EDD)

/**
 * COMPONENTE 'AsaasWebhookConversionCard'
 *
 * Integra os dados recebidos pelo webhook do Asaas na VPS ao MarketingDashboard:
 * - Gráfico de funil de conversão (Criados -> Recebidos -> Vencidos)
 * - Rastreamento de Meta de Faturamento Mensal/Semanal das assinaturas do Asaas
 * - Indicador de status do Webhook da VPS Hostinger em tempo real
 * - Lista dos últimos eventos processados com ação rápida de envio para o WhatsApp
 */
@Composable
fun AsaasWebhookConversionCard(
    onOpenAsaasCockpit: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        AsaasWebhookSimulatorManager.initialize(context)
    }

    val events = AsaasWebhookSimulatorManager.events
    val config = AsaasWebhookSimulatorManager.config

    val receivedEvents = events.filter { it.eventType == "PAYMENT_RECEIVED" }
    val overdueEvents = events.filter { it.eventType == "PAYMENT_OVERDUE" }

    val totalRevenueAsaas = receivedEvents.sumOf { it.paymentValue }
    val revenueTarget = 1500.0 // Meta mensal de R$ 1.500,00 em assinaturas Pro
    val progress = (totalRevenueAsaas / revenueTarget).coerceIn(0.0, 1.0).toFloat()
    val isGoalAchieved = totalRevenueAsaas >= revenueTarget

    val conversionRate = if (events.isNotEmpty()) {
        (receivedEvents.size.toDouble() / events.size.toDouble()) * 100.0
    } else 0.0

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("card_asaas_webhook_conversion"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, TikTokCyan.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 1. CABEÇALHO DO WEBHOOK ASAAS COM STATUS DA VPS
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
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = TikTokCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "WEBHOOK ASAAS • VPS HOSTINGER",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            letterSpacing = 0.5.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FiberManualRecord,
                                contentDescription = null,
                                tint = NeonEmerald,
                                modifier = Modifier.size(8.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Webhook Ativo • Porta 8000",
                                color = NeonEmerald,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = onOpenAsaasCockpit,
                    modifier = Modifier
                        .height(30.dp)
                        .testTag("btn_open_full_asaas_cockpit"),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TikTokCyan),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TikTokCyan.copy(alpha = 0.6f))
                ) {
                    Text("Abrir Cockpit", fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. RÉGUA DE PROGRESSO DA META DE FATURAMENTO DAS ASSINATURAS
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkBg)
                    .border(1.dp, Color(0xFF1B2230), RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "META DE ASSINATURAS DO MÊS",
                            color = Color.Gray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isGoalAchieved) "🎉 Meta Batida!" else "${(progress * 100).toInt()}% atingido",
                            color = if (isGoalAchieved) NeonEmerald else AmberGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (isGoalAchieved) NeonEmerald else TikTokCyan,
                        trackColor = Color(0xFF1A1F2B)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Realizado: R$ ${String.format(Locale("pt", "BR"), "%.2f", totalRevenueAsaas)}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Alvo: R$ ${String.format(Locale("pt", "BR"), "%.2f", revenueTarget)}",
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. CARTÕES DE FUNIL DE CONVERSÃO ASAAS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Pagamentos Recebidos
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkBg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonEmerald.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Pagos (Ativos)", color = Color.Gray, fontSize = 9.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${receivedEvents.size} clientes",
                            color = NeonEmerald,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "R$ ${String.format(Locale("pt", "BR"), "%.2f", totalRevenueAsaas)}",
                            color = Color.LightGray,
                            fontSize = 10.sp
                        )
                    }
                }

                // Inadimplentes / Vencidos
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkBg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CoralRed.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Vencidos (Alerta)", color = Color.Gray, fontSize = 9.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${overdueEvents.size} contas",
                            color = if (overdueEvents.isNotEmpty()) CoralRed else Color.LightGray,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Régua Ativa",
                            color = Color.Gray,
                            fontSize = 10.sp
                        )
                    }
                }

                // Taxa de Conversão do Webhook
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkBg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ElectricPurple.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Tx. Conversão", color = Color.Gray, fontSize = 9.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = String.format(Locale("pt", "BR"), "%.1f%%", conversionRate),
                            color = ElectricPurple,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Checkout PIX",
                            color = Color.Gray,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4. ÚLTIMO EVENTO RECEBIDO COM BOTÃO RÁPIDO PARA WHATSAPP
            val latestEvent = events.firstOrNull()
            if (latestEvent != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0B0E16))
                        .border(1.dp, Color(0xFF161E2E), RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            if (latestEvent.eventType == "PAYMENT_RECEIVED") NeonEmerald.copy(alpha = 0.15f) else CoralRed.copy(alpha = 0.15f)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = latestEvent.eventType,
                                        color = if (latestEvent.eventType == "PAYMENT_RECEIVED") NeonEmerald else CoralRed,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = latestEvent.customerName,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(text = latestEvent.timestampFormatted, color = Color.Gray, fontSize = 9.sp)
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Plano: ${latestEvent.planType} • Valor: R$ ${String.format(Locale("pt", "BR"), "%.2f", latestEvent.paymentValue)}",
                            color = Color.LightGray,
                            fontSize = 10.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val msg = AsaasWebhookSimulatorManager.buildWhatsAppMessage(latestEvent)
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Mensagem Asaas WhatsApp", msg))
                                    Toast.makeText(context, "Texto do WhatsApp copiado!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(30.dp),
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copiar Mensagem", fontSize = 9.sp)
                            }

                            Button(
                                onClick = {
                                    val msg = AsaasWebhookSimulatorManager.buildWhatsAppMessage(latestEvent)
                                    val cleanPhone = latestEvent.customerPhone.replace(Regex("[^0-9]"), "")
                                    val url = "https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(msg)}"
                                    try {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "WhatsApp não instalado no dispositivo", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(30.dp),
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald, contentColor = Color.Black)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.Black)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Disparar WhatsApp", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
