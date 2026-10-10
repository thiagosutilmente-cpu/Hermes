package com.example

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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val DialogDarkBg = Color(0xFF140306)
private val DialogCardBg = Color(0xFF24060A)
private val SosRed = Color(0xFFFF2238)
private val SosGreen = Color(0xFF00E676)

/**
 * EmergencySosDialog
 *
 * Diálogo modal para configuração e monitoramento de emergência SOS:
 * - Aba 1: Gestão de Contatos de Confiança (Adicionar, Excluir, Testar envio)
 * - Aba 2: Histórico e Auditoria de Disparos Silenciosos via WorkManager
 * - Aba 3: Telemetria GPS e Link em Tempo Real
 */
@Composable
fun EmergencySosDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val contacts by EmergencySosManager.emergencyContacts.collectAsState()
    val logs by EmergencySosManager.historyLogs.collectAsState()
    val locationState by LocationService.globalLocationState.collectAsState()
    val sosState by EmergencySosManager.sosState.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showAddContactForm by remember { mutableStateOf(false) }

    // Formulário de novo contato
    var newContactName by remember { mutableStateOf("") }
    var newContactPhone by remember { mutableStateOf("") }
    var newContactRelationship by remember { mutableStateOf("Família") }
    var newContactIsWhatsApp by remember { mutableStateOf(true) }
    var newContactIsSms by remember { mutableStateOf(true) }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, Color(0xFF5A141D), RoundedCornerShape(20.dp))
                .testTag("emergency_sos_dialog"),
            color = DialogDarkBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header com Botão Fechar
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
                                .background(Color(0xFF50000A)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "SOS",
                                tint = SosRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Central de Segurança SOS",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "Alerta Silencioso • WorkManager",
                                color = Color(0xFFFF8896),
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.testTag("dialog_sos_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fechar",
                            tint = Color(0xFFE0A0A8)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Abas de Navegação
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color(0xFF1E0407),
                    contentColor = SosRed,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = SosRed
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = {
                            Text(
                                text = "Contatos (${contacts.size})",
                                fontSize = 12.sp,
                                fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTabIndex == 0) Color.White else Color(0xFFA07078)
                            )
                        }
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = {
                            Text(
                                text = "Auditoria Logs (${logs.size})",
                                fontSize = 12.sp,
                                fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTabIndex == 1) Color.White else Color(0xFFA07078)
                            )
                        }
                    )
                    Tab(
                        selected = selectedTabIndex == 2,
                        onClick = { selectedTabIndex = 2 },
                        text = {
                            Text(
                                text = "GPS & Disparo",
                                fontSize = 12.sp,
                                fontWeight = if (selectedTabIndex == 2) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTabIndex == 2) Color.White else Color(0xFFA07078)
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Conteúdo da Aba
                when (selectedTabIndex) {
                    0 -> {
                        // ABA CONTATOS
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Pessoas que receberão sua localização:",
                                    color = Color(0xFFFFCDD2),
                                    fontSize = 11.5.sp
                                )

                                Button(
                                    onClick = { showAddContactForm = !showAddContactForm },
                                    modifier = Modifier.testTag("sos_toggle_add_contact_button"),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (showAddContactForm) Color(0xFF4A1016) else SosRed,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(
                                        imageVector = if (showAddContactForm) Icons.Default.Close else Icons.Default.Add,
                                        contentDescription = "Adicionar",
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (showAddContactForm) "Fechar" else "Novo Contato",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Formulário Expansível de Adicionar Contato
                            AnimatedVisibility(visible = showAddContactForm) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = DialogCardBg)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text(
                                            text = "Cadastrar Novo Contato de Emergência",
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))

                                        OutlinedTextField(
                                            value = newContactName,
                                            onValueChange = { newContactName = it },
                                            label = { Text("Nome do Contato", color = Color(0xFFB87882)) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("sos_input_contact_name"),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White,
                                                focusedBorderColor = SosRed,
                                                unfocusedBorderColor = Color(0xFF4A1016)
                                            ),
                                            singleLine = true
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        OutlinedTextField(
                                            value = newContactPhone,
                                            onValueChange = { newContactPhone = it },
                                            label = { Text("Telefone / WhatsApp (ex: 11988887777)", color = Color(0xFFB87882)) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("sos_input_contact_phone"),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White,
                                                focusedBorderColor = SosRed,
                                                unfocusedBorderColor = Color(0xFF4A1016)
                                            ),
                                            singleLine = true
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        OutlinedTextField(
                                            value = newContactRelationship,
                                            onValueChange = { newContactRelationship = it },
                                            label = { Text("Vínculo (ex: Esposa, Central, Amigo)", color = Color(0xFFB87882)) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("sos_input_contact_rel"),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White,
                                                focusedBorderColor = SosRed,
                                                unfocusedBorderColor = Color(0xFF4A1016)
                                            ),
                                            singleLine = true
                                        )

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Button(
                                            onClick = {
                                                if (newContactName.isNotBlank() && newContactPhone.isNotBlank()) {
                                                    EmergencySosManager.addContact(
                                                        context,
                                                        EmergencyContact(
                                                            name = newContactName.trim(),
                                                            phone = newContactPhone.trim(),
                                                            relationship = newContactRelationship.trim(),
                                                            isWhatsAppEnabled = newContactIsWhatsApp,
                                                            isSmsEnabled = newContactIsSms
                                                        )
                                                    )
                                                    newContactName = ""
                                                    newContactPhone = ""
                                                    showAddContactForm = false
                                                    Toast.makeText(context, "Contato de emergência salvo!", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, "Preencha nome e telefone", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("sos_save_contact_button"),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = SosRed)
                                        ) {
                                            Text("Salvar Contato de Emergência", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            // Lista de Contatos
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(contacts, key = { it.id }) { contact ->
                                    Card(
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = DialogCardBg),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = contact.name,
                                                        color = Color.White,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "(${contact.relationship})",
                                                        color = Color(0xFFFF8896),
                                                        fontSize = 10.sp
                                                    )
                                                }
                                                Text(
                                                    text = contact.phone,
                                                    color = Color(0xFFDCA8B0),
                                                    fontSize = 11.sp
                                                )
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                // Botão Testar WhatsApp
                                                Surface(
                                                    onClick = {
                                                        EmergencySosManager.sendSosViaWhatsApp(context, contact)
                                                    },
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = Color(0xFF1B5E20),
                                                    modifier = Modifier.padding(end = 6.dp)
                                                ) {
                                                    Text(
                                                        text = "WhatsApp 💬",
                                                        color = Color.White,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)
                                                    )
                                                }

                                                // Botão Testar SMS
                                                Surface(
                                                    onClick = {
                                                        EmergencySosManager.sendSosViaSms(context, contact)
                                                    },
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = Color(0xFF0D47A1),
                                                    modifier = Modifier.padding(end = 6.dp)
                                                ) {
                                                    Text(
                                                        text = "SMS ✉️",
                                                        color = Color.White,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)
                                                    )
                                                }

                                                // Remover Contato
                                                IconButton(
                                                    onClick = {
                                                        EmergencySosManager.removeContact(context, contact.id)
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Excluir",
                                                        tint = Color(0xFFFF5252),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        // ABA AUDITORIA DE LOGS DO WORKMANAGER
                        Column(modifier = Modifier.fillMaxSize()) {
                            Text(
                                text = "Histórico de Disparos Silenciosos & Beacons:",
                                color = Color(0xFFFFCDD2),
                                fontSize = 11.5.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            if (logs.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Nenhum disparo de SOS registrado até o momento.",
                                        color = Color(0xFFA07078),
                                        fontSize = 12.sp
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(logs, key = { it.id }) { logEntry ->
                                        val dateStr = SimpleDateFormat("HH:mm:ss - dd/MM/yyyy", Locale.getDefault())
                                            .format(Date(logEntry.timestampMillis))

                                        Card(
                                            shape = RoundedCornerShape(10.dp),
                                            colors = CardDefaults.cardColors(containerColor = DialogCardBg),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = "🚨 $dateStr",
                                                        color = Color.White,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = if (logEntry.resolvedAtMillis != null) "RESOLVIDO ✅" else "ATIVO",
                                                        color = if (logEntry.resolvedAtMillis != null) SosGreen else SosRed,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }

                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "Status: ${logEntry.status}",
                                                    color = Color(0xFFFFD5DA),
                                                    fontSize = 11.sp
                                                )
                                                Text(
                                                    text = "WorkManager ID: ${logEntry.workManagerId}",
                                                    color = Color(0xFFA07078),
                                                    fontSize = 9.sp
                                                )
                                                Text(
                                                    text = "GPS: ${logEntry.latitude}, ${logEntry.longitude} (${String.format(Locale.US, "%.1f", logEntry.speedKmh)} km/h)",
                                                    color = Color(0xFF80D8FF),
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        // ABA GPS E TESTE DO WORKMANAGER
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = DialogCardBg),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.LocationOn, contentDescription = "GPS", tint = SosGreen)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Telemetria de Localização Atual",
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Latitude: ${locationState.latitude}",
                                        color = Color(0xFFFFCDD2),
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = "Longitude: ${locationState.longitude}",
                                        color = Color(0xFFFFCDD2),
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = "Precisão do Sensor: ±${locationState.accuracyMeters.toInt()} metros",
                                        color = Color(0xFFFFCDD2),
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = "Velocidade Instantânea: ${String.format(Locale.US, "%.1f", locationState.currentSpeedKmh)} km/h",
                                        color = Color(0xFFFFD54F),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Botão de Disparo do Alerta Silencioso via WorkManager
                            Button(
                                onClick = {
                                    EmergencySosManager.triggerSosImmediately(context)
                                    Toast.makeText(context, "Alerta Silencioso Disparado via WorkManager!", Toast.LENGTH_LONG).show()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("sos_dialog_trigger_button"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SosRed)
                            ) {
                                Text("DISPARAR ALERTA SILENCIOSO VIA WORKMANAGER ⚡", fontWeight = FontWeight.Black, fontSize = 12.sp)
                            }

                            // Botão de Compartilhar Link Geral
                            OutlinedButton(
                                onClick = {
                                    EmergencySosManager.shareDistressLocation(context)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Compartilhar", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Compartilhar Link GPS via Outros Apps", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
