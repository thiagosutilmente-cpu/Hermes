package com.example

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
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
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * AdminDeveloperCockpitScreen
 *
 * Tela exclusiva do proprietário protegida por PIN.
 * Centraliza o diagnóstico da VPS Hostinger, inspeção dos Webhooks Asaas,
 * telemetria FCM e ponte para as ferramentas de gestão.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDeveloperCockpitScreen(
    onNavigateBack: () -> Unit,
    onOpenAsaasCockpit: () -> Unit,
    onOpenMarketingHub: () -> Unit,
    onOpenMarketingDashboard: () -> Unit,
    onOpenWebViewCockpit: () -> Unit
) {
    val context = LocalContext.current
    BackHandler { onNavigateBack() }

    var showChangePinSection by remember { mutableStateOf(false) }
    var currentPinInput by remember { mutableStateOf("") }
    var newPinInput by remember { mutableStateOf("") }
    var pinMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "COCKPIT DO PROPRIETÁRIO",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF00FF88).copy(alpha = 0.2f))
                                    .border(0.8.dp, Color(0xFF00FF88), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "ADMIN",
                                    color = Color(0xFF00FF88),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = "VPS Hostinger • Asaas Pix • Telemetria Interna",
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("admin_cockpit_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar ao Modo Motoboy",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    // Botão para Bloquear o Modo Admin Imediatamente
                    OutlinedButton(
                        onClick = {
                            AdminSecurityManager.lockAdmin()
                            Toast.makeText(context, "🔒 Modo Administrador Bloqueado", Toast.LENGTH_SHORT).show()
                            onNavigateBack()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF4444)),
                        border = BorderStroke(1.dp, Color(0xFFFF4444).copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("admin_cockpit_lock_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Bloquear",
                            tint = Color(0xFFFF4444),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Bloquear", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0A0A0F)
                )
            )
        },
        containerColor = Color(0xFF0A0A0F)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Banner de Status da Arquitetura Segura
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    border = BorderStroke(1.2.dp, Color(0xFF00FF88).copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00FF88).copy(alpha = 0.15f))
                                .border(1.dp, Color(0xFF00FF88).copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = Color(0xFF00FF88),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "MODO DESENVOLVEDOR ATIVADO",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "Este painel está 100% oculto dos motoboys. O motorista visualiza apenas a interface operacional de corridas.",
                                color = Color(0xFF94A3B8),
                                fontSize = 10.5.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }

            // 2. Hub de Ações Rápidas de Gestão
            item {
                Text(
                    text = "AÇÕES DE GESTÃO DO PROPRIETÁRIO",
                    color = Color(0xFF64748B),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onOpenAsaasCockpit,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_open_asaas_cockpit"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E293B),
                            contentColor = Color(0xFF38BDF8)
                        ),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f))
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("💳", fontSize = 18.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Webhooks Asaas", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = onOpenMarketingHub,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_open_marketing_hub"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E293B),
                            contentColor = Color(0xFFF59E0B)
                        ),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f))
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🚀", fontSize = 18.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Central Automação", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = onOpenMarketingDashboard,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_open_marketing_dashboard"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E293B),
                            contentColor = Color(0xFF10B981)
                        ),
                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f))
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📊", fontSize = 18.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Gráficos Recharts", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 2.0. ARQUITETURA DESACOPLADA: MARKETING & ENXAME 100% EXTERNO (VPS / N8N / CLAUDE)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_decoupled_marketing_status"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    border = BorderStroke(1.2.dp, Color(0xFF38BDF8).copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("☁️", fontSize = 20.sp)
                            Column {
                                Text(
                                    text = "MARKETING & ENXAME 100% EXTERNOS",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "Automações rodando na VPS Hostinger (Claude + N8N)",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Text(
                            text = "O APK do motoboy foi 100% desacoplado de robôs de vendas, campanhas de Meta Ads e disparadores de WhatsApp. Toda a máquina de aquisição opera na nuvem sem consumir a bateria ou memória do aparelho do piloto.",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1E293B)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Papel do App:",
                                    color = Color(0xFF64748B),
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = "Apenas consulta /api/check-subscription",
                                    color = Color(0xFF10B981),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // 3. Card de Manuais e Playbooks Externos (Fora do APK)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "PLAYBOOKS E GUIAS DE MARKETING EXTERNOS",
                                color = Color(0xFFF59E0B),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Para não sobrecarregar o APK do motoboy, todos os roteiros, campanhas do Meta Ads e guias do Asaas estão salvos no repositório em arquivos dedicados:",
                            color = Color(0xFFD1D5DB),
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        val docItems = listOf(
                            "docs/marketing/01_META_ADS_CAMPANHA_MOTOBOYS.md" to "Campanhas Meta Ads, orçamentos e criativos",
                            "docs/marketing/02_ROTEIROS_ORGANICO_WHATSAPP_TELEGRAM.md" to "Scripts de abordagem orgânica e indicação",
                            "docs/marketing/03_GUIA_ASAAS_PIX_CELULAR.md" to "Acompanhamento de Pix no app oficial Asaas",
                            "docs/marketing/04_COMO_FUNCIONA_AUTOMACAO_META_E_WHATSAPP.md" to "Como a automação funciona por APIs (sem bots de tela)",
                            "docs/marketing/05_SETUP_META_ADS_API_TOKEN.md" to "Passo a passo para gerar o Token Meta e colar na VPS",
                            "docs/marketing/06_IA_VENDEDORA_E_ATIVACAO_PIX_AUTOMATICA.md" to "Piloto de IA: Negociação WhatsApp + Ativação Asaas Pix 100% autônoma",
                            "docs/marketing/07_SUPER_AUTOMACAO_360_GUIA_COMPLETO.md" to "Super Automação: Anti-abandono Pix, Áudio de Voz, Otimizador Ads e Som Cha-Ching",
                            "docs/marketing/08_OPERACAO_SINGULARIDADE_SWARM_CLAUDE.md" to "Operação Singularidade: 10 Agentes Autônomos (MIND-0) + Execução no PC com Claude",
                            "docs/vps/01_OPERACAO_HOSTINGER_VPS.md" to "Comandos de monitoramento da VPS 187.77.248.73"
                        )

                        docItems.forEach { (path, desc) ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .background(Color(0xFF1F2937), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = path,
                                    color = Color(0xFF38BDF8),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = desc,
                                    color = Color(0xFF9CA3AF),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }

            // 4. Diagnóstico de Rede & VPS Hostinger
            item {
                Text(
                    text = "TELEMETRIA DA VPS HOSTINGER (187.77.248.73:8000)",
                    color = Color(0xFF64748B),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
            }

            item {
                VpsHealthHudComponent()
            }

            item {
                HostingerAsaasDiagnosticPanel()
            }

            item {
                HostingerLatencyCockpitCard()
            }

            item {
                RadarKeepAliveCard()
            }

            item {
                DeepOrderPoolCockpitCard()
            }

            item {
                DualSplitWindowSimulatorCard()
            }

            item {
                SplitScreenManagerCard()
            }

            item {
                SystemHealthCheckupCard()
            }

            item {
                RadarBackendSyncCard()
            }

            item {
                FcmStatusDiagnosticsCard()
            }

            // 5. Gestão do PIN do Administrador
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    border = BorderStroke(1.dp, Color(0xFF334155))
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
                                Icon(
                                    imageVector = Icons.Default.Build,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Alterar PIN do Administrador",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            OutlinedButton(
                                onClick = { showChangePinSection = !showChangePinSection },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00FF88))
                            ) {
                                Text(if (showChangePinSection) "Fechar" else "Configurar", fontSize = 11.sp)
                            }
                        }

                        if (showChangePinSection) {
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = currentPinInput,
                                onValueChange = { currentPinInput = it.filter { c -> c.isDigit() } },
                                label = { Text("PIN Atual", color = Color(0xFF94A3B8), fontSize = 11.sp) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = newPinInput,
                                onValueChange = { newPinInput = it.filter { c -> c.isDigit() } },
                                label = { Text("Novo PIN (mínimo 4 dígitos)", color = Color(0xFF94A3B8), fontSize = 11.sp) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )

                            if (pinMessage != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = pinMessage ?: "",
                                    color = if (pinMessage?.contains("sucesso", ignoreCase = true) == true) Color(0xFF00FF88) else Color(0xFFFF4444),
                                    fontSize = 11.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = {
                                    val ok = AdminSecurityManager.changePin(context, currentPinInput, newPinInput)
                                    if (ok) {
                                        pinMessage = "PIN alterado com sucesso!"
                                        currentPinInput = ""
                                        newPinInput = ""
                                    } else {
                                        pinMessage = "PIN atual incorreto ou novo PIN menor que 4 dígitos."
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF00FF88),
                                    contentColor = Color(0xFF0A0A0F)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("SALVAR NOVO PIN", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
