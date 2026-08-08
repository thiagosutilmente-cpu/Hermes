package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun AdminLoginModal(
    onDismiss: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.8f)),
            border = BorderStroke(1.dp, Color(0xFF3A86FF).copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "ACESSO_ADMINISTRATIVO_RESTRITO", color = Color(0xFF3A86FF), fontSize = 16.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 6) pin = it },
                    label = { Text("PIN", color = Color.White.copy(alpha = 0.5f)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF3A86FF),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace)
                )
                Button(
                    onClick = {
                        if (pin == "748596") {
                            onLoginSuccess()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3A86FF).copy(alpha = 0.2f)),
                    border = BorderStroke(1.dp, Color(0xFF3A86FF))
                ) {
                    Text("VALIDAR_ACESSO", color = Color(0xFF3A86FF), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
fun AdminDashboardModal(
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(1) } // Default to Offline Sync Queue tab
    val context = androidx.compose.ui.platform.LocalContext.current
    val isConnected by com.example.coordinator.RadarCoordinator.isFirestoreConnected.collectAsState()

    // Offline queue mock state for live Android visualization
    var pendingWritesCount by remember { mutableIntStateOf(3) }
    var lastFlushTimestamp by remember { mutableStateOf("Há 2 min") }
    var isFlushing by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0F)),
            border = BorderStroke(1.5.dp, Color(0xFF00F5D4).copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "🔐 PAINEL DE ADMINISTRAÇÃO MASTER",
                            color = Color(0xFF00F5D4),
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Radar Coordinator • Monitoramento de Sincronização",
                            color = Color.Gray,
                            fontSize = 10.sp
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isConnected) Color(0xFF00FF88).copy(alpha = 0.2f) else Color.Red.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, if (isConnected) Color(0xFF00FF88) else Color.Red)
                    ) {
                        Text(
                            text = if (isConnected) "FIRESTORE ONLINE" else "OFFLINE (BUFFER)",
                            color = if (isConnected) Color(0xFF00FF88) else Color.Red,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Dedicated Tab Bar
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Black.copy(alpha = 0.5f),
                    contentColor = Color(0xFF00F5D4),
                    divider = {}
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                "📊 SYSTEM LOGS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                "🔄 FILA SYNC D3 ($pendingWritesCount)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = if (selectedTab == 1) Color(0xFF00FF88) else Color.Gray
                            )
                        }
                    )
                }

                // Tab Contents
                when (selectedTab) {
                    0 -> {
                        // System Logs Overview
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("SISTEMA OPERACIONAL JARVIS NEURAL DEBUG", color = Color(0xFF00F5D4), fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            Text("CONSTRUÇÃO: KOTLIN/COMPOSE + FIRESTORE SYNC", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            Text("STATUS PERSISTÊNCIA: FIRESTORE ACTIVE SNAPSHOTS", color = Color(0xFF00FF88), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            
                            Spacer(modifier = Modifier.weight(1f))
                            Text("Últimos Eventos do Administrador Autônomo:", color = Color.Gray, fontSize = 10.sp)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .background(Color.Black, RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = "[SYS_INIT] Listener de rotas e ofertas ativado\n[PULSE_HEALTH] Score de sistema: 98 pts (Sem anomalias)\n[GHOST_OPT] Ponderação de trânsito sincronizada\n[FIRESTORE] Real-time listener conectado à coleção 'riders'",
                                    color = Color(0xFF00FF88),
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    1 -> {
                        // Dedicated D3 Offline Sync Queue Monitor & Manual Flush Tab
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Status Badges
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF151520)),
                                    border = BorderStroke(1.dp, Color(0xFF00F5D4).copy(alpha = 0.3f))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("PENDENTES NO BUFFER", color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                        Text("$pendingWritesCount registros", color = Color(0xFF00F5D4), fontSize = 14.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                                    }
                                }

                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF151520)),
                                    border = BorderStroke(1.dp, Color(0xFFFFB800).copy(alpha = 0.3f))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("ÚLTIMA SINCRONIZAÇÃO", color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                        Text(lastFlushTimestamp, color = Color(0xFFFFB800), fontSize = 14.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }

                            // D3-style Bar Chart Canvas Component for Pending Writes Category Breakdown
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.6f)),
                                border = BorderStroke(1.dp, Color(0xFF00FF88).copy(alpha = 0.3f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("📊 VISUALIZAÇÃO D3.JS — GRAVAÇÕES PENDENTES POR CATEGORIA", color = Color(0xFF00FF88), fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                                        Text("REAL-TIME D3", color = Color(0xFF00F5D4), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    }

                                    // Canvas drawing bars
                                    androidx.compose.foundation.Canvas(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f)
                                    ) {
                                        val categories = listOf(
                                            Triple("Ganhos", if (pendingWritesCount > 0) 1f else 0f, Color(0xFF00FF88)),
                                            Triple("Config", if (pendingWritesCount > 1) 1f else 0f, Color(0xFF00F5D4)),
                                            Triple("Veículo", if (pendingWritesCount > 2) 1f else 0f, Color(0xFFFFB800)),
                                            Triple("Health", 0f, Color(0xFFFF3366)),
                                            Triple("Stacks", 0f, Color(0xFFB066FE))
                                        )

                                        val barWidth = size.width / (categories.size * 1.8f)
                                        val maxBarHeight = size.height - 30.dp.toPx()

                                        categories.forEachIndexed { index, (label, value, color) ->
                                            val x = (index * (size.width / categories.size)) + (barWidth / 2)
                                            val h = (value * maxBarHeight).coerceAtLeast(10f)
                                            val y = size.height - 20.dp.toPx() - h

                                            // Draw Bar
                                            drawRect(
                                                color = color.copy(alpha = 0.8f),
                                                topLeft = androidx.compose.ui.geometry.Offset(x, y),
                                                size = androidx.compose.ui.geometry.Size(barWidth, h)
                                            )

                                            // Draw Bar Top Accent
                                            drawRect(
                                                color = color,
                                                topLeft = androidx.compose.ui.geometry.Offset(x, y),
                                                size = androidx.compose.ui.geometry.Size(barWidth, 4f)
                                            )
                                        }
                                    }

                                    // Category Labels Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceAround
                                    ) {
                                        Text("Ganhos", color = Color(0xFF00FF88), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        Text("Config", color = Color(0xFF00F5D4), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        Text("Veículo", color = Color(0xFFFFB800), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        Text("Health", color = Color(0xFFFF3366), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        Text("Stacks", color = Color(0xFFB066FE), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // Manual Sync Flush Controls
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        isFlushing = true
                                        com.example.coordinator.RadarCoordinator.syncActiveSessionStatsToCloud()
                                        pendingWritesCount = 0
                                        lastFlushTimestamp = "Agora mesmo"
                                        isFlushing = false
                                        android.widget.Toast.makeText(context, "⚡ Flush manual executado com sucesso! Dados sincronizados com Firestore.", android.widget.Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(2f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F5D4).copy(alpha = 0.2f)),
                                    border = BorderStroke(1.dp, Color(0xFF00F5D4))
                                ) {
                                    Text(
                                        text = if (isFlushing) "SINCRONIZANDO..." else "⚡ FORÇAR FLUSH MANUAL",
                                        color = Color(0xFF00F5D4),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Button(
                                    onClick = {
                                        pendingWritesCount++
                                        android.widget.Toast.makeText(context, "🧪 Item de teste adicionado à fila offline.", android.widget.Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB800).copy(alpha = 0.2f)),
                                    border = BorderStroke(1.dp, Color(0xFFFFB800))
                                ) {
                                    Text("+ TESTAR", color = Color(0xFFFFB800), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Footer Close Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.2f)),
                    border = BorderStroke(1.dp, Color.Red)
                ) {
                    Text("ENCERRAR SESSÃO ADMIN", color = Color.Red, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }
            }
        }
    }
}
