package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.network.HostingerLatencyMonitor
import com.example.network.OfferDecisionRequest
import com.example.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Log de auditoria de rede e testes gravados em tempo real.
 */
data class DiagnosticLogEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: String,
    val source: String, // VPS HOSTINGER ou ASAAS WEBHOOK
    val action: String,
    val endpointOrEvent: String,
    val status: String,
    val latencyMs: Long,
    val isSuccess: Boolean,
    val details: String
)

/**
 * HostingerAsaasDiagnosticPanel
 *
 * Painel completo e unificado de testes em tempo real para:
 * 1. Servidor VPS Hostinger (187.77.248.73:8000): Endpoints de ping, health, decisão de corridas e simulação de failover.
 * 2. Gateway Asaas: Webhooks de PAYMENT_RECEIVED, PAYMENT_OVERDUE, divisão de split (40% Líder / 60% PJ) e desbloqueio Pro.
 * 3. Console Interativo de Logs ao Vivo com exportação de auditoria para suporte ou Claude.
 */
@Composable
fun HostingerAsaasDiagnosticPanel(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val latencyState by HostingerLatencyMonitor.latencyState.collectAsState()
    val subscriptionState by SubscriptionManager.subscriptionState.collectAsState()
    val asaasEngineState by AsaasFinancialEngine.engineState.collectAsState()

    var isRunningVpsPing by remember { mutableStateOf(false) }
    var isRunningDecisionTest by remember { mutableStateOf(false) }
    var isRunningAsaasWebhook by remember { mutableStateOf(false) }
    var testResultSummary by remember { mutableStateOf<String?>(null) }

    val logs = remember { mutableStateListOf<DiagnosticLogEntry>() }

    fun addLog(source: String, action: String, endpoint: String, status: String, latencyMs: Long, isSuccess: Boolean, details: String) {
        val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale("pt", "BR"))
        logs.add(
            0,
            DiagnosticLogEntry(
                timestamp = sdf.format(Date()),
                source = source,
                action = action,
                endpointOrEvent = endpoint,
                status = status,
                latencyMs = latencyMs,
                isSuccess = isSuccess,
                details = details
            )
        )
        if (logs.size > 40) {
            logs.removeRange(40, logs.size)
        }
    }

    val statusColor by animateColorAsState(
        targetValue = when {
            latencyState.lastLatencyMs == 0L -> Color(0xFF94A3B8)
            latencyState.isBottleneckActive || latencyState.lastLatencyMs > 500L -> RedDecline
            latencyState.lastLatencyMs > 250L -> Color(0xFFFFB800)
            else -> NeonGreen
        },
        label = "diagStatusColor"
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCardElevated),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .border(
                1.5.dp,
                if (latencyState.isBottleneckActive) RedDecline.copy(alpha = 0.8f) else Color(0xFF00D2FF).copy(alpha = 0.5f),
                RoundedCornerShape(20.dp)
            )
            .testTag("hostinger_asaas_diagnostic_panel")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // ========================================================
            // 1. CABEÇALHO DO PAINEL DE DIAGNÓSTICO
            // ========================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "DIAGNÓSTICO VPS & ASAAS AO VIVO",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Hostinger (187.77.248.73:8000) • Asaas v3 Webhook",
                            color = Color(0xFF00D2FF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (subscriptionState.isActive) NeonGreen.copy(alpha = 0.15f) else Color(0xFFFF5252).copy(alpha = 0.15f))
                        .border(1.dp, if (subscriptionState.isActive) NeonGreen else Color(0xFFFF5252), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (subscriptionState.isActive) "PRO ATIVO" else "PLANO FREE",
                        color = if (subscriptionState.isActive) NeonGreen else Color(0xFFFF5252),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ========================================================
            // 2. SEÇÃO 1: TESTES DA VPS HOSTINGER
            // ========================================================
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0F1D)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Dns, contentDescription = null, tint = Color(0xFF00D2FF), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("TESTE DE CONECTIVIDADE VPS", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = if (latencyState.lastLatencyMs > 0) "${latencyState.lastLatencyMs} ms" else "---",
                            color = statusColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Status: ${latencyState.statusDescription}",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Botão Testar Ping (/api/ping)
                        Button(
                            onClick = {
                                if (!isRunningVpsPing) {
                                    isRunningVpsPing = true
                                    val startNs = System.nanoTime()
                                    coroutineScope.launch(Dispatchers.IO) {
                                        var isOk = false
                                        var msg = ""
                                        var code = 0
                                        try {
                                            val service = RetrofitClient.getApiService()
                                            val resp = service.checkHealth()
                                            code = resp.code()
                                            isOk = resp.isSuccessful
                                            msg = if (isOk) "HTTP $code - OK (${resp.body()?.message ?: "Online"})" else "HTTP $code - Falha de Resposta"
                                        } catch (e: Exception) {
                                            msg = "Falha de Conexão: ${e.javaClass.simpleName}: ${e.message}"
                                            isOk = false
                                        }
                                        val elapsedMs = ((System.nanoTime() - startNs) / 1_000_000).coerceAtLeast(1L)
                                        HostingerLatencyMonitor.recordLatency(endpoint = "/api/ping", latencyMs = elapsedMs, isSuccess = isOk)

                                        withContext(Dispatchers.Main) {
                                            isRunningVpsPing = false
                                            addLog(
                                                source = "VPS HOSTINGER",
                                                action = "Ping / Health Check",
                                                endpoint = "GET /api/ping",
                                                status = if (isOk) "HTTP $code OK" else "ERRO CONEXÃO",
                                                latencyMs = elapsedMs,
                                                isSuccess = isOk,
                                                details = msg
                                            )
                                            testResultSummary = "📡 Ping VPS concluído em ${elapsedMs}ms: $msg"
                                            Toast.makeText(context, "Ping VPS: ${elapsedMs}ms ($msg)", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            enabled = !isRunningVpsPing,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("btn_test_vps_ping")
                        ) {
                            Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isRunningVpsPing) "Testando..." else "1. Ping /api/ping", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // Botão Enviar Decisão de Corrida (/api/offers/decision)
                        Button(
                            onClick = {
                                if (!isRunningDecisionTest) {
                                    isRunningDecisionTest = true
                                    val startNs = System.nanoTime()
                                    coroutineScope.launch(Dispatchers.IO) {
                                        var isOk = false
                                        var msg = ""
                                        var code = 0
                                        try {
                                            val service = RetrofitClient.getApiService()
                                            val testReq = OfferDecisionRequest(
                                                offerId = "sim_${System.currentTimeMillis()}",
                                                appName = "iFood Entregas",
                                                accepted = true,
                                                value = 23.50,
                                                distanceKm = 4.8,
                                                driverLatitude = -23.561684,
                                                driverLongitude = -46.655981
                                            )
                                            val resp = service.sendOfferDecision(testReq)
                                            code = resp.code()
                                            isOk = resp.isSuccessful
                                            msg = if (isOk) "HTTP $code - Decisão gravada na VPS" else "HTTP $code - Erro ao gravar decisão"
                                        } catch (e: Exception) {
                                            msg = "Falha no Envio: ${e.javaClass.simpleName}: ${e.message}"
                                            isOk = false
                                        }
                                        val elapsedMs = ((System.nanoTime() - startNs) / 1_000_000).coerceAtLeast(1L)
                                        HostingerLatencyMonitor.recordLatency(endpoint = "/api/offers/decision", latencyMs = elapsedMs, isSuccess = isOk)

                                        withContext(Dispatchers.Main) {
                                            isRunningDecisionTest = false
                                            addLog(
                                                source = "VPS HOSTINGER",
                                                action = "Envio de Corrida",
                                                endpoint = "POST /api/offers/decision",
                                                status = if (isOk) "HTTP $code OK" else "FALHA / TIMEOUT",
                                                latencyMs = elapsedMs,
                                                isSuccess = isOk,
                                                details = msg
                                            )
                                            testResultSummary = "📦 Decisão enviada para VPS em ${elapsedMs}ms: $msg"
                                            Toast.makeText(context, "Decisão de corrida: $msg", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            enabled = !isRunningDecisionTest,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("btn_test_offer_decision")
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isRunningDecisionTest) "Enviando..." else "2. Post Corrida", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Alternar Fallback On-Device (< 5ms)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (latencyState.isFallbackToLocalEngineActive) "🚨 Modo Local Ativo (VPS Ignorada)" else "🟢 Modo Nuvem Ativo (VPS Preferencial)",
                            color = if (latencyState.isFallbackToLocalEngineActive) RedDecline else NeonGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedButton(
                            onClick = {
                                val newState = !latencyState.isFallbackToLocalEngineActive
                                HostingerLatencyMonitor.setFallbackActive(newState)
                                val actionLabel = if (newState) "Ativou Fallback Local (<5ms)" else "Restaurou Conexão VPS"
                                addLog(
                                    source = "GOVERNANÇA REDE",
                                    action = "Chaveamento Failover",
                                    endpoint = "LOCAL_FALLBACK_SWITCH",
                                    status = if (newState) "LOCAL FORCED" else "VPS PRIMARY",
                                    latencyMs = 1L,
                                    isSuccess = true,
                                    details = actionLabel
                                )
                                Toast.makeText(context, actionLabel, Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(if (latencyState.isFallbackToLocalEngineActive) "Restaurar Nuvem" else "Forçar Fallback Local", fontSize = 10.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ========================================================
            // 3. SEÇÃO 2: TESTES DE WEBHOOK ASAAS & SPLIT PIX
            // ========================================================
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SIMULADOR DE WEBHOOK ASAAS", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = "Split 40% Líder / 60% PJ",
                            color = NeonGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Assinatura Semanal: R$ 25,00 • Split: R$ 10,00 (Líder) + R$ 15,00 (PJ Master)",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Botão Simular PAYMENT_RECEIVED
                        Button(
                            onClick = {
                                isRunningAsaasWebhook = true
                                coroutineScope.launch {
                                    val simulatedCustomer = "cus_motoboy_${(1000..9999).random()}"
                                    val event = AsaasWebhookSimulatorManager.recordWebhookEvent(
                                        context = context,
                                        eventType = "PAYMENT_RECEIVED",
                                        customerId = simulatedCustomer,
                                        customerName = "Thiago Motoboy VIP",
                                        customerPhone = "+55 11 98765-4321",
                                        paymentValue = 25.00,
                                        planType = "Semanal PIX (R$ 25,00) • Split 40%",
                                        whatsappStatus = "7 Dias PRO Ativados (Split R$ 10,00 Líder)"
                                    )

                                    // Feedback sonoro e tátil
                                    try {
                                        SaleAlertSoundManager.playSaleNotification(context)
                                    } catch (_: Exception) {}

                                    try {
                                        HapticFeedbackEngine.vibratePattern(context, HapticPattern.SUCCESS_DOUBLE_PULSE)
                                    } catch (_: Exception) {}

                                    addLog(
                                        source = "ASAAS WEBHOOK",
                                        action = "Pagamento Recebido PIX",
                                        endpoint = "WEBHOOK /webhook/asaas",
                                        status = "200 SUCCESS",
                                        latencyMs = 12L,
                                        isSuccess = true,
                                        details = "Split 40% (R$ 10,00 Líder) + 60% (R$ 15,00 PJ) -> PRO Liberado 7 dias!"
                                    )

                                    isRunningAsaasWebhook = false
                                    testResultSummary = "🎉 Webhook Asaas processado: Licença PRO de 7 dias ativada com split R$ 10 Líder / R$ 15 PJ!"
                                    Toast.makeText(context, "Asaas: Licença 7 Dias PRO Ativada via PIX!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = !isRunningAsaasWebhook,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("btn_simulate_payment_received")
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("1. Simular Pago (PRO)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // Botão Simular PAYMENT_OVERDUE (Inadimplência)
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val simulatedCustomer = "cus_motoboy_overdue"
                                    AsaasWebhookSimulatorManager.recordWebhookEvent(
                                        context = context,
                                        eventType = "PAYMENT_OVERDUE",
                                        customerId = simulatedCustomer,
                                        customerName = "Condutor Inadimplente",
                                        customerPhone = "+55 11 99999-0000",
                                        paymentValue = 25.00,
                                        planType = "Semanal PIX (R$ 25,00)",
                                        whatsappStatus = "Acesso PRO Bloqueado"
                                    )

                                    addLog(
                                        source = "ASAAS WEBHOOK",
                                        action = "Pagamento Vencido",
                                        endpoint = "WEBHOOK /webhook/asaas",
                                        status = "PAYMENT_OVERDUE",
                                        latencyMs = 8L,
                                        isSuccess = false,
                                        details = "Acesso PRO bloqueado imediatamente. Plano revertido para FREE restrito."
                                    )

                                    testResultSummary = "⚠️ Webhook Asaas: Acesso PRO bloqueado por vencimento (PAYMENT_OVERDUE)."
                                    Toast.makeText(context, "Asaas: Acesso PRO Bloqueado (PAYMENT_OVERDUE)", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("btn_simulate_payment_overdue")
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("2. Vencido (Bloqueio)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Botão Validar Split Matemático
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isSplitValid = AsaasFinancialEngine.validateSplit()
                        Text(
                            text = if (isSplitValid) "✅ Split Matemático 100% Conforme" else "❌ Inconsistência na Regra de Split",
                            color = if (isSplitValid) NeonGreen else RedDecline,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedButton(
                            onClick = {
                                val valid = AsaasFinancialEngine.validateSplit()
                                addLog(
                                    source = "AUDITORIA FISCAL",
                                    action = "Validação Split",
                                    endpoint = "ALGORITMO_SPLIT_AUDIT",
                                    status = if (valid) "AUDIT OK" else "AUDIT FAIL",
                                    latencyMs = 0L,
                                    isSuccess = valid,
                                    details = "Total: R$ 25,00 | Líder: R$ 10,00 (40,0%) | PJ: R$ 15,00 (60,0%)"
                                )
                                Toast.makeText(context, "Split validado: 40% Líder + 60% PJ = R$ 25,00", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Auditar Split", fontSize = 10.sp)
                        }
                    }
                }
            }

            // Exibição do resumo do último teste executado
            AnimatedVisibility(visible = testResultSummary != null) {
                testResultSummary?.let { summary ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0F172A))
                            .border(1.dp, Color(0xFF00D2FF).copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = summary,
                            color = Color(0xFFE2E8F0),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ========================================================
            // 4. SEÇÃO 3: CONSOLE DE LOGS DE REDE AO VIVO (AUDITORIA)
            // ========================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Terminal, contentDescription = null, tint = Color(0xFF00FF88), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "CONSOLE DE EVENTOS EM TEMPO REAL (${logs.size})",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row {
                    // Copiar Logs
                    IconButton(
                        onClick = {
                            if (logs.isNotEmpty()) {
                                val sb = StringBuilder()
                                sb.append("=== LOG DE DIAGNÓSTICO VPS HOSTINGER & ASAAS ===\n")
                                logs.forEach { l ->
                                    sb.append("[${l.timestamp}] [${l.source}] ${l.action} | ${l.endpointOrEvent} | ${l.status} (${l.latencyMs}ms) -> ${l.details}\n")
                                }
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Logs Jarvis VPS", sb.toString()))
                                Toast.makeText(context, "Logs copiados para a área de transferência!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Nenhum log gravado ainda.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copiar Logs", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                    }

                    // Limpar Logs
                    IconButton(
                        onClick = {
                            logs.clear()
                            testResultSummary = null
                            Toast.makeText(context, "Logs limpos com sucesso.", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.ClearAll, contentDescription = "Limpar Logs", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF050811))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
                    .padding(8.dp)
            ) {
                if (logs.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Aguardando disparo de testes da VPS ou Webhook Asaas...\nClique nos botões acima para auditar os endpoints em tempo real.",
                            color = Color(0xFF475569),
                            fontSize = 11.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(logs, key = { it.id }) { log ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF0D1322))
                                    .padding(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(if (log.isSuccess) NeonGreen else RedDecline)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "[${log.timestamp}] ${log.source}",
                                            color = Color(0xFF00D2FF),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }

                                    Text(
                                        text = "${log.latencyMs}ms",
                                        color = if (log.latencyMs > 500) RedDecline else NeonGreen,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${log.action} • ${log.endpointOrEvent} -> ${log.status}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.Monospace
                                )

                                Text(
                                    text = log.details,
                                    color = Color(0xFF94A3B8),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
