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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * RadarBackendSyncCard
 *
 * Card visual interativo que expõe o envio e sincronização em tempo real com o servidor:
 * - Status de conexão (Online / Offline / Sincronizando)
 * - URL configurável do servidor (VPS / Webhook / API REST)
 * - Switch de envio automático imediato a cada oferta / comando de voz
 * - Botão de Ping / Testar Conexão
 * - Botão para disparar evento de teste manual (simulação de aceite ou voz para validar o backend)
 * - Painel de logs de pacotes despachados (HTTP 200, Fila Offline, JSON do payload)
 * - Contador de pacotes enviados com sucesso vs pendentes na fila
 */
@Composable
fun RadarBackendSyncCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    val syncState by RadarBackendSyncManager.syncState.collectAsState()
    val recentDispatches by RadarBackendSyncManager.recentDispatches.collectAsState()

    var showEditUrlDialog by remember { mutableStateOf(false) }
    var tempUrlText by remember { mutableStateOf(syncState.serverUrl) }
    var showPayloadDialog by remember { mutableStateOf<RadarBackendSyncManager.DispatchedEventLog?>(null) }
    var isTestingPing by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(18.dp))
            .testTag("card_backend_sync")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // CABEÇALHO DO CARD
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
                            .background(
                                if (syncState.isSyncing) Color(0xFF38BDF8).copy(alpha = 0.2f)
                                else if (syncState.isConnected) Color(0xFF10B981).copy(alpha = 0.2f)
                                else Color(0xFFEF4444).copy(alpha = 0.2f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                syncState.isSyncing -> Icons.Default.CloudSync
                                syncState.isConnected -> Icons.Default.CloudDone
                                else -> Icons.Default.CloudOff
                            },
                            contentDescription = "Status do Servidor",
                            tint = when {
                                syncState.isSyncing -> Color(0xFF38BDF8)
                                syncState.isConnected -> Color(0xFF10B981)
                                else -> Color(0xFFEF4444)
                            },
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "SINCRONIZAÇÃO COM O SERVIDOR",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (syncState.isSyncing) Color(0xFF0284C7)
                                        else if (syncState.isConnected) Color(0xFF059669)
                                        else Color(0xFFDC2626)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (syncState.isSyncing) "ENVIANDO" else if (syncState.isConnected) "ONLINE" else "OFFLINE",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                        Text(
                            text = "Envio de decisões, voz e telemetria para a VPS/API",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ENDPOINT ATUAL + BOTÃO DE EDITAR
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1E293B))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ENDPOINT ATIVO (API / WEBHOOK):",
                            color = Color(0xFF64748B),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = syncState.serverUrl,
                            color = Color(0xFF38BDF8),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row {
                        IconButton(
                            onClick = {
                                tempUrlText = syncState.serverUrl
                                showEditUrlDialog = true
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Editar URL",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // MÉTRICAS EM GRADE (ENVIADOS / FILA OFFLINE / AUTO-SYNC)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Card 1: Enviados
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF1E293B).copy(alpha = 0.6f))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Text("ENVIADOS COM SUCESSO", color = Color(0xFF64748B), fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "${syncState.totalSentSuccess}",
                                color = Color(0xFF10B981),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("pacotes", color = Color(0xFF94A3B8), fontSize = 10.sp)
                        }
                    }
                }

                // Card 2: Fila Offline
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF1E293B).copy(alpha = 0.6f))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Text("FILA OFFLINE (RE-TENTAR)", color = Color(0xFF64748B), fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "${syncState.pendingCount}",
                                color = if (syncState.pendingCount > 0) Color(0xFFF59E0B) else Color(0xFF94A3B8),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("retidos", color = Color(0xFF94A3B8), fontSize = 10.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // CHAVE DE ENVIO AUTOMÁTICO EM SEGUNDO PLANO
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1E293B).copy(alpha = 0.4f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Envio Automático Imediato",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Dispara HTTP POST assim que a oferta for aceita/recusada ou ouvida",
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp
                    )
                }
                Switch(
                    checked = syncState.isAutoSyncEnabled,
                    onCheckedChange = { isEnabled ->
                        RadarBackendSyncManager.setAutoSyncEnabled(context, isEnabled)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF10B981),
                        checkedTrackColor = Color(0xFF064E3B),
                        uncheckedThumbColor = Color(0xFF94A3B8),
                        uncheckedTrackColor = Color(0xFF334155)
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // BOTÕES DE AÇÃO: TESTAR CONEXÃO (PING) & DISPARAR PACOTE TESTE
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        isTestingPing = true
                        RadarBackendSyncManager.testServerConnection(context) { success, msg ->
                            isTestingPing = false
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedColors(contentColor = Color(0xFF38BDF8)),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF38BDF8).copy(alpha = 0.5f))
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isTestingPing) {
                        CircularProgressIndicator(
                            color = Color(0xFF38BDF8),
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Testando...", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Testar Ping", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = {
                        // Dispara evento de teste completo para a API
                        RadarBackendSyncManager.sendOfferDecision(
                            context = context,
                            offerId = "teste_${System.currentTimeMillis() % 10000}",
                            appName = "iFood",
                            restaurant = "Madero Burger Express",
                            value = 34.50,
                            distanceKm = 4.2,
                            gainPerKm = 8.21,
                            action = "ACCEPTED",
                            source = "Comando de Voz",
                            reason = "Teste Manual do Piloto",
                            deliveryAddress = "Av. Paulista, 1000"
                        )
                        Toast.makeText(context, "Pacote de teste despachado para a VPS!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Disparar Teste", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // SEÇÃO: ÚLTIMOS EVENTOS DESPACHADOS PARA O SERVIDOR
            if (recentDispatches.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "HISTÓRICO DE TRANSMISSÃO EM TEMPO REAL",
                        color = Color(0xFF64748B),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${recentDispatches.size} registros",
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF090D16))
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    recentDispatches.take(4).forEach { logItem ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF111827))
                                .clickable { showPayloadDialog = logItem }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = when (logItem.type) {
                                        "OFFER_DECISION" -> "📦"
                                        "VOICE_COMMAND" -> "🎙️"
                                        "TELEMETRY" -> "⚡"
                                        else -> "🌐"
                                    },
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = logItem.summary,
                                        color = Color.White,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "${logItem.timestampFormatted} • Toque para ver JSON",
                                        color = Color(0xFF64748B),
                                        fontSize = 8.5.sp
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (logItem.status.startsWith("ENVIADO")) Color(0xFF065F46)
                                        else Color(0xFF78350F)
                                    )
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (logItem.status.startsWith("ENVIADO")) "200 OK" else "FILA",
                                    color = if (logItem.status.startsWith("ENVIADO")) Color(0xFF34D399) else Color(0xFFFBBF24),
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // DIÁLOGO PARA EDITAR A URL DO SERVIDOR
    if (showEditUrlDialog) {
        AlertDialog(
            onDismissRequest = { showEditUrlDialog = false },
            title = {
                Text(
                    text = "Configurar Servidor Central",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "Informe a URL da VPS ou API REST que receberá os despachos de ofertas e comandos por voz do aplicativo:",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = tempUrlText,
                        onValueChange = { tempUrlText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("URL do Servidor") },
                        placeholder = { Text("http://187.77.248.73:8080") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF475569),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TextButton(
                            onClick = { tempUrlText = "http://187.77.248.73:8080" },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("VPS Jarvis Padrão", fontSize = 10.sp, color = Color(0xFF38BDF8))
                        }
                        TextButton(
                            onClick = { tempUrlText = "https://api.radar-coordinator.local" },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Localhost / Mock", fontSize = 10.sp, color = Color(0xFF94A3B8))
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        RadarBackendSyncManager.updateServerUrl(context, tempUrlText)
                        showEditUrlDialog = false
                        Toast.makeText(context, "URL do servidor atualizada!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditUrlDialog = false }) {
                    Text("Cancelar", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }

    // DIÁLOGO PARA VISUALIZAR O PAYLOAD JSON DO EVENTO
    showPayloadDialog?.let { logItem ->
        AlertDialog(
            onDismissRequest = { showPayloadDialog = null },
            title = {
                Text(
                    text = "Pacote de Dados Despachado",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "Tipo: ${logItem.type} • Status: ${logItem.status}",
                        color = Color(0xFF38BDF8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF090D16))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = logItem.payloadSnippet,
                            color = Color(0xFFE2E8F0),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(logItem.payloadSnippet))
                        Toast.makeText(context, "JSON copiado para a área de transferência!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copiar JSON")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPayloadDialog = null }) {
                    Text("Fechar", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }
}
