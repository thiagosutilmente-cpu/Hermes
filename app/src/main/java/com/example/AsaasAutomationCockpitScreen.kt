package com.example

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
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

private val DarkBg = Color(0xFF07090E)
private val DarkSurface = Color(0xFF10141E)
private val DarkSurfaceBorder = Color(0xFF1C2436)
private val NeonEmerald = Color(0xFF00FF88)
private val TikTokCyan = Color(0xFF00F2FE)
private val AmberGold = Color(0xFFFFB800)
private val CoralRed = Color(0xFFFF5252)

/**
 * TELA COCKPIT DE AUTOMAÇÃO ASAAS & WHATSAPP
 *
 * Permite monitorar eventos de pagamento do Asaas, simular webhooks de produção
 * e testar a automação de envio de mensagens no WhatsApp via Evolution API ou Intent direto.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AsaasAutomationCockpitScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onNavigateBack() }

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        AsaasWebhookSimulatorManager.initialize(context)
    }

    val events = AsaasWebhookSimulatorManager.events
    val currentConfig = AsaasWebhookSimulatorManager.config

    var showConfigSection by remember { mutableStateOf(false) }
    var vpsUrlInput by remember { mutableStateOf(currentConfig.vpsWebhookUrl) }
    var evolutionApiUrlInput by remember { mutableStateOf(currentConfig.evolutionApiUrl) }
    var evolutionApiKeyInput by remember { mutableStateOf(currentConfig.evolutionApiKey) }

    // Campos de simulação
    var simCustomerName by remember { mutableStateOf("Marcos Entregador (SP)") }
    var simCustomerPhone by remember { mutableStateOf("5511999998888") }
    var simPlanPrice by remember { mutableDoubleStateOf(29.90) }
    var simPlanType by remember { mutableStateOf("Mensal (R$ 29,90)") }

    val totalRevenue = events.filter { it.eventType == "PAYMENT_RECEIVED" }.sumOf { it.paymentValue }
    val activeSubscribers = events.count { it.eventType == "PAYMENT_RECEIVED" }
    val overdueSubscribers = events.count { it.eventType == "PAYMENT_OVERDUE" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "AUTOMAÇÃO ASAAS & WHATSAPP",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Webhook VPS • Evolution API • Régua de Retenção",
                            fontSize = 11.sp,
                            color = TikTokCyan,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("btn_back_asaas_cockpit")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showConfigSection = !showConfigSection },
                        modifier = Modifier.testTag("btn_toggle_asaas_config")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configurar VPS/API",
                            tint = if (showConfigSection) NeonEmerald else Color.LightGray
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
            )
        },
        containerColor = DarkBg,
        modifier = modifier.testTag("asaas_cockpit_screen")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. CARTOES DE MÉTRICAS CONSOLIDADAS
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Faturamento Asaas", color = Color.Gray, fontSize = 10.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = String.format(Locale("pt", "BR"), "R$ %.2f", totalRevenue),
                                color = NeonEmerald,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Assinantes Ativos", color = Color.Gray, fontSize = 10.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$activeSubscribers motoboys",
                                color = TikTokCyan,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Inadimplentes", color = Color.Gray, fontSize = 10.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$overdueSubscribers contas",
                                color = if (overdueSubscribers > 0) CoralRed else Color.LightGray,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }

            // 2. CONFIGURAÇÕES DA VPS & EVOLUTION API (EXPANSÍVEL)
            item {
                AnimatedVisibility(visible = showConfigSection) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TikTokCyan.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Dns, contentDescription = null, tint = TikTokCyan, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("CONFIGURAÇÃO DA VPS & EVOLUTION API", color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp)
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = vpsUrlInput,
                                onValueChange = { vpsUrlInput = it },
                                label = { Text("URL Webhook VPS (FastAPI)", fontSize = 11.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TikTokCyan,
                                    unfocusedBorderColor = DarkSurfaceBorder,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.LightGray
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = evolutionApiUrlInput,
                                onValueChange = { evolutionApiUrlInput = it },
                                label = { Text("URL Evolution API (WhatsApp)", fontSize = 11.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TikTokCyan,
                                    unfocusedBorderColor = DarkSurfaceBorder,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.LightGray
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    AsaasWebhookSimulatorManager.updateConfig(
                                        context,
                                        currentConfig.copy(
                                            vpsWebhookUrl = vpsUrlInput,
                                            evolutionApiUrl = evolutionApiUrlInput
                                        )
                                    )
                                    showConfigSection = false
                                    Toast.makeText(context, "Configurações de infraestrutura salvas!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = TikTokCyan, contentColor = Color.Black)
                            ) {
                                Text("Salvar Configurações", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // 3. PAINEL DE DISPARO E SIMULAÇÃO DE WEBHOOK
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = NeonEmerald, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("SIMULADOR DE EVENTOS DO ASAAS", color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp)
                        }
                        Text(
                            text = "Teste em tempo real a recepção do webhook e a geração da mensagem de WhatsApp",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Nome do Cliente
                        OutlinedTextField(
                            value = simCustomerName,
                            onValueChange = { simCustomerName = it },
                            label = { Text("Nome do Entregador", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonEmerald,
                                unfocusedBorderColor = DarkSurfaceBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.LightGray
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Telefone do Cliente
                        OutlinedTextField(
                            value = simCustomerPhone,
                            onValueChange = { simCustomerPhone = it },
                            label = { Text("WhatsApp (com DDD)", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonEmerald,
                                unfocusedBorderColor = DarkSurfaceBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.LightGray
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Seletor de Planos
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val plans = listOf("Semanal (R$ 9,90)" to 9.90, "Mensal (R$ 29,90)" to 29.90)
                            plans.forEach { (label, value) ->
                                val isSelected = simPlanPrice == value
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) NeonEmerald.copy(alpha = 0.15f) else DarkBg)
                                        .border(1.dp, if (isSelected) NeonEmerald else DarkSurfaceBorder, RoundedCornerShape(8.dp))
                                        .clickable {
                                            simPlanPrice = value
                                            simPlanType = label
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) NeonEmerald else Color.Gray,
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Botões de Ação de Disparo
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val newEvent = AsaasWebhookSimulatorManager.recordWebhookEvent(
                                        context = context,
                                        eventType = "PAYMENT_RECEIVED",
                                        customerId = "cus_${System.currentTimeMillis() % 1000000}",
                                        customerName = simCustomerName,
                                        customerPhone = simCustomerPhone,
                                        paymentValue = simPlanPrice,
                                        planType = simPlanType,
                                        whatsappStatus = "Disparo WhatsApp Pronto"
                                    )
                                    val msg = AsaasWebhookSimulatorManager.buildWhatsAppMessage(newEvent)
                                    openWhatsAppWithMessage(context, simCustomerPhone, msg)
                                    Toast.makeText(context, "Pagamento simulado! Abrindo mensagem de boas-vindas.", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("btn_simulate_payment_received"),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald, contentColor = Color.Black)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Simular Pgto Recebido", fontSize = 11.sp, fontWeight = FontWeight.Black)
                            }

                            Button(
                                onClick = {
                                    val newEvent = AsaasWebhookSimulatorManager.recordWebhookEvent(
                                        context = context,
                                        eventType = "PAYMENT_OVERDUE",
                                        customerId = "cus_${System.currentTimeMillis() % 1000000}",
                                        customerName = simCustomerName,
                                        customerPhone = simCustomerPhone,
                                        paymentValue = simPlanPrice,
                                        planType = simPlanType,
                                        whatsappStatus = "Alerta Anti-Inadimplência Gerado"
                                    )
                                    val msg = AsaasWebhookSimulatorManager.buildWhatsAppMessage(newEvent)
                                    openWhatsAppWithMessage(context, simCustomerPhone, msg)
                                    Toast.makeText(context, "Simulação de inadimplência! Disparando régua de retenção.", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("btn_simulate_payment_overdue"),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CoralRed, contentColor = Color.White)
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Simular Pgto Vencido", fontSize = 11.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }

            // 4. HISTÓRICO DE EVENTOS E STATUS DO DISPARO
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "EVENTOS PROCESSADOS (${events.size})",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black
                    )

                    if (events.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                AsaasWebhookSimulatorManager.clearEvents(context)
                                Toast.makeText(context, "Histórico limpo com sucesso!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Limpar", tint = Color.Gray, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            items(events.size) { index ->
                val ev = events[index]
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_asaas_event_${ev.id}"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (ev.eventType == "PAYMENT_RECEIVED") NeonEmerald.copy(alpha = 0.15f) else CoralRed.copy(alpha = 0.15f)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = ev.eventType,
                                    color = if (ev.eventType == "PAYMENT_RECEIVED") NeonEmerald else CoralRed,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }

                            Text(text = ev.timestampFormatted, color = Color.Gray, fontSize = 10.sp)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = ev.customerName,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Plano: ${ev.planType} • Chave: ${ev.activationKey}",
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val msg = AsaasWebhookSimulatorManager.buildWhatsAppMessage(ev)
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Mensagem WhatsApp", msg))
                                    Toast.makeText(context, "Texto de WhatsApp copiado!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp),
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copiar Texto", fontSize = 10.sp)
                            }

                            Button(
                                onClick = {
                                    val msg = AsaasWebhookSimulatorManager.buildWhatsAppMessage(ev)
                                    openWhatsAppWithMessage(context, ev.customerPhone, msg)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp),
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = TikTokCyan, contentColor = Color.Black)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.Black)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Enviar WhatsApp", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun openWhatsAppWithMessage(context: Context, phone: String, message: String) {
    val cleanPhone = phone.replace(Regex("[^0-9]"), "")
    val url = if (cleanPhone.isNotEmpty()) {
        "https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(message)}"
    } else {
        "https://api.whatsapp.com/send?text=${Uri.encode(message)}"
    }

    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Enviar via WhatsApp"))
    }
}
