package com.example

import android.widget.Toast
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
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
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
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

// Cores táticas Uber / 99 Dark
private val DarkBg = Color(0xFF090A0F)
private val DarkSurface = Color(0xFF11141C)
private val DarkSurfaceBorder = Color(0xFF1E2433)
private val WhatsAppGreen = Color(0xFF25D366)
private val TikTokCyan = Color(0xFF00F2FE)
private val AmberGold = Color(0xFFFFB800)
private val NeonEmerald = Color(0xFF00FF88)

/**
 * TELA CENTRAL DE MARKETING AUTOMATIZADO & ROTEIROS VIRAIS
 * Contém os roteiros oficiais para TikTok/Reels, mensagens para Grupos de WhatsApp
 * e simulador do Robô de WhatsApp na VPS.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutomatedMarketingHubScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("🎬 TikTok & Reels", "💬 Grupos de WhatsApp", "🤖 Robô do Zap (VPS)")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "CENTRAL DE MARKETING VIRAL",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Meta R$ 50 mil • Escala Automática",
                            fontSize = 11.sp,
                            color = AmberGold,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("btn_back_marketing_hub")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
            )
        },
        containerColor = DarkBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Seletor de Abas
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = DarkSurface,
                contentColor = Color.White,
                edgePadding = 12.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = when (selectedTab) {
                            0 -> TikTokCyan
                            1 -> WhatsAppGreen
                            else -> AmberGold
                        }
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Black else FontWeight.Medium,
                                fontSize = 13.sp,
                                color = if (selectedTab == index) Color.White else Color.Gray
                            )
                        }
                    )
                }
            }

            // Conteúdo da Aba Selecionada
            when (selectedTab) {
                0 -> TikTokReelsScriptsTabContent()
                1 -> WhatsAppGroupsStrategyTabContent()
                2 -> WhatsAppBotAutomationTabContent()
            }
        }
    }
}

// =============================================================================
// ABA 1: ROTEIROS TIKTOK & REELS (GRAVAÇÃO COM CELULAR NO GUIDÃO)
// =============================================================================
@Composable
private fun TikTokReelsScriptsTabContent() {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, TikTokCyan.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(TikTokCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Videocam, contentDescription = null, tint = TikTokCyan)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = "Dica de Ouro de Gravação", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(
                            text = "Grave o celular no suporte da moto na rua. Vídeos com a moto ligada batem 50k a 100k views fácil!",
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Roteiro 1
        item {
            VideoScriptCard(
                title = "ROTEIRO 1: 'Cansado de Corrida de R$ 6?'",
                category = "Mais Viral • 100k Views Potencial",
                accentColor = TikTokCyan,
                hook = "Mano, você ainda tá aceitando corrida de R$ 6,00 no iFood que não paga nem o pneu da moto?",
                actionScene = "Mostra o celular no guidão tocando corrida ruim. O Jarvis recusa na hora e fala no fone: 'Corrida descartada: prejuízo de R$ 1,80/km'. 5 segundos depois toca uma de R$ 24,00 e o Jarvis avisa: 'Aprovada: R$ 5,20/km'.",
                callToAction = "Comenta 'EU QUERO' ou clica no link da bio que liberaram 7 dias de graça desse copiloto!",
                fullCopyText = """
ROTEIRO 1 - TIKTOK / REELS (Cansado de corrida de R$ 6):
[GANCHO]: "Mano, você ainda tá aceitando corrida de R$ 6,00 no iFood que não paga nem a gasolina?"
[CENA 1]: Filma o painel da moto com a notificação do app tocando. Mostra a bolha do Jarvis piscando em vermelho.
[VOZ DO JARVIS NO FONE]: "Corrida lixo detectada: apenas R$ 1,90 por km. Recusando automaticamente."
[CENA 2]: Segundos depois, toca uma corrida de R$ 22,00. O Jarvis pisca verde: "Corrida lucrativa aprovada: R$ 5,10 por km!".
[FINAL/CTA]: "Pare de trabalhar de graça. Clica no link da minha bio e testa o Jarvis grátis por 7 dias!"
                """.trimIndent()
            )
        }

        // Roteiro 2
        item {
            VideoScriptCard(
                title = "ROTEIRO 2: 'A Rota Dupla Secreta'",
                category = "Foco em Alto Ganho • R$ 300/dia",
                accentColor = NeonEmerald,
                hook = "Como fazer R$ 300 em 5 horas combinando iFood e 99Entrega na mesma rota.",
                actionScene = "Mostra o Jarvis identificando que a entrega do iFood e da 99 vão para a mesma avenida. O piloto pega os 2 pedidos e recebe pelas 2 entregas no mesmo trajeto.",
                callToAction = "O link pra testar de graça tá na bio antes que fechem as vagas!",
                fullCopyText = """
ROTEIRO 2 - TIKTOK / REELS (A Rota Dupla Secreta):
[GANCHO]: "O que os motoboys veteranos não contam: como fazer R$ 300 no dia rodando menos quilômetros."
[CENA]: Mostra a tela do Jarvis com o badge 'ROTA DUPLA IDENTIFICADA'.
[EXPLICAÇÃO]: "O Jarvis cruza as chamadas dos apps. Se você tá com pedido do iFood indo pra Vila Mariana e toca um 99 pro mesmo caminho, ele te avisa pra encaixar as duas. Ganho duplicado na mesma viagem de moto."
[CTA]: "Testa aí na faixa por 7 dias no link da bio!"
                """.trimIndent()
            )
        }

        // Roteiro 3
        item {
            VideoScriptCard(
                title = "ROTEIRO 3: 'Alerta de Blitz no Fone'",
                category = "Segurança • Ponto Crítico do Motoboy",
                accentColor = AmberGold,
                hook = "Quase perdi minha moto hoje pra blitz... se não fosse o Jarvis me avisar no fone!",
                actionScene = "Piloto acelerando e a voz do Jarvis avisa: 'Atenção piloto: Blitz reportada a 300 metros à direita'. O piloto desvia tranquilo sem estresse.",
                callToAction = "Não arrisque seu ganha-pão. Baixa o app grátis no link da bio.",
                fullCopyText = """
ROTEIRO 3 - TIKTOK / REELS (Alerta de Blitz no Fone):
[GANCHO]: "Quase perdi minha moto hoje... se não fosse esse app me avisar 300 metros antes!"
[CENA]: Mostra a moto andando e o fone Bluetooth conectado.
[ÁUDIO JARVIS]: "Alerta tático: fiscalização e blitz detectada na Avenida dos Bandeirantes."
[CTA]: "Salva a sua ferramenta de trabalho. O app avisa blitz e radar em tempo real. 7 dias grátis na bio!"
                """.trimIndent()
            )
        }
    }
}

@Composable
private fun VideoScriptCard(
    title: String,
    category: String,
    accentColor: Color,
    hook: String,
    actionScene: String,
    callToAction: String,
    fullCopyText: String
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.5.sp)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(accentColor.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(text = category, color = accentColor, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Gancho
            Text(text = "🔥 GANCHO (Primeiros 3 segundos):", color = AmberGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(text = "\"$hook\"", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)

            Spacer(modifier = Modifier.height(8.dp))

            // Cena
            Text(text = "🎬 CENA NO SUPORTE DA MOTO:", color = TikTokCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(text = actionScene, color = Color.LightGray, fontSize = 12.sp)

            Spacer(modifier = Modifier.height(8.dp))

            // CTA
            Text(text = "🎯 CHAMADA PRA AÇÃO (CTA):", color = NeonEmerald, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(text = callToAction, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = {
                    ViralMarketingShareManager.copyToClipboard(context, fullCopyText, title)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.Black)
            ) {
                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Copiar Roteiro Completo", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

// =============================================================================
// ABA 2: ESTRATÉGIA DE GRUPOS DE WHATSAPP (DIVULGAÇÃO LOCAL)
// =============================================================================
@Composable
private fun WhatsAppGroupsStrategyTabContent() {
    val context = LocalContext.current
    val link = remember { ViralMarketingShareManager.getReferralLink(context) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, WhatsAppGreen.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "🚀 Como Infiltrar nos Grupos da Cidade",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Entre em grupos de motoboy da sua região (grupos de blitz, praças de alimentação de shopping, pontos de apoio). Mande a mensagem pronta sem parecer spam. A galera instala na hora para ver o filtro de R$/km!",
                        color = Color.LightGray,
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Mensagem Oficial para Grupos
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "MENSAGEM CAMPEÃ PARA GRUPOS DE WHATSAPP",
                        color = WhatsAppGreen,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val groupMsg = ViralMarketingShareManager.getGroupWhatsAppMessage(context)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF07090E))
                            .padding(12.dp)
                    ) {
                        Text(text = groupMsg, color = Color.White, fontSize = 11.sp, lineHeight = 16.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { ViralMarketingShareManager.shareToWhatsApp(context, groupMsg) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen, contentColor = Color.Black)
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Enviar no Zap", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { ViralMarketingShareManager.copyToClipboard(context, groupMsg, "Mensagem de Grupos") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Copiar Texto", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// ABA 3: ROBÔ DO WHATSAPP & WORKMANAGER (CONVERSÃO AUTOMÁTICA DOS 7 DIAS)
// =============================================================================
@Composable
private fun WhatsAppBotAutomationTabContent() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val referralCode = remember { ViralMarketingShareManager.getReferralCode(context) }

    var testPhone by remember {
        mutableStateOf(MarketingFunnelScheduler.getLastScheduledPhone(context))
    }
    var funnelStages by remember {
        mutableStateOf(MarketingFunnelScheduler.getFunnelStages(context))
    }
    var isFunnelActive by remember {
        mutableStateOf(MarketingFunnelScheduler.isFunnelScheduled(context))
    }

    val botFunnel = remember {
        WhatsAppMarketingBotManager.getOfficialBotFunnel(testPhone, referralCode)
    }

    fun refreshStages() {
        funnelStages = MarketingFunnelScheduler.getFunnelStages(context)
        isFunnelActive = MarketingFunnelScheduler.isFunnelScheduled(context)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Card do Painel de Controle do WorkManager
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.2.dp, AmberGold.copy(alpha = 0.6f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(AmberGold.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = AmberGold
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "WORKMANAGER AUTOMATIZADO",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = if (isFunnelActive) "🟢 Agendamento em Segundo Plano Ativo" else "⚪ Nenhum agendamento pendente",
                                    color = if (isFunnelActive) NeonEmerald else Color.LightGray,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        IconButton(onClick = { refreshStages() }) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Atualizar Status",
                                tint = AmberGold,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Telefone WhatsApp para Disparos:",
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = testPhone,
                        onValueChange = { testPhone = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_funnel_phone"),
                        placeholder = { Text("Ex: 5511999999999", color = Color.Gray, fontSize = 12.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AmberGold,
                            unfocusedBorderColor = DarkSurfaceBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF07090E),
                            unfocusedContainerColor = Color(0xFF07090E)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Botão 1: Agendar Funil Oficial (7 Dias)
                    Button(
                        onClick = {
                            MarketingFunnelScheduler.scheduleFull7DayFunnel(
                                context = context,
                                phone = testPhone,
                                userName = "Piloto",
                                isAccelerated = false
                            )
                            refreshStages()
                            Toast.makeText(context, "✅ Funil de 7 Dias agendado no WorkManager!", Toast.LENGTH_LONG).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_schedule_official_funnel"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AmberGold, contentColor = Color.Black)
                    ) {
                        Icon(imageVector = Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Agendar Funil Oficial (1h, 3d, 7d)", fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Botão 2: Simular Acelerado (5s, 20s, 40s)
                    Button(
                        onClick = {
                            MarketingFunnelScheduler.scheduleFull7DayFunnel(
                                context = context,
                                phone = testPhone,
                                userName = "Piloto",
                                isAccelerated = true
                            )
                            refreshStages()
                            Toast.makeText(context, "⚡ Modo Simulação: 3 etapas disparando em 5s, 20s e 40s!", Toast.LENGTH_LONG).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_simulate_accelerated_funnel"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TikTokCyan, contentColor = Color.Black)
                    ) {
                        Icon(imageVector = Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "⚡ Simular Funil Acelerado (5s, 20s, 40s)", fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Botão 3: Fechamento Imediato Dia 7 (Plano Anual)
                        Button(
                            onClick = {
                                MarketingFunnelScheduler.triggerStageImmediately(
                                    context = context,
                                    stage = MarketingFunnelWorker.STAGE_DAY_7_ANNUAL_CLOSE,
                                    phone = testPhone
                                )
                                refreshStages()
                                Toast.makeText(context, "🎯 Oferta do 7º Dia (Plano Anual R$ 650) disparada!", Toast.LENGTH_LONG).show()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_trigger_day_7_now"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald, contentColor = Color.Black)
                        ) {
                            Text(text = "Disparar 7º Dia Agora", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        // Cancelar Agendamento
                        OutlinedButton(
                            onClick = {
                                MarketingFunnelScheduler.cancelAllFunnelWork(context)
                                refreshStages()
                                Toast.makeText(context, "Agendamentos cancelados", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .weight(0.7f)
                                .testTag("btn_cancel_funnel_work"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.LightGray)
                        ) {
                            Text(text = "Cancelar", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Título da Lista de Estágios
        item {
            Text(
                text = "STATUS DOS DISPAROS PROGRAMADOS",
                color = Color.LightGray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }

        // Lista de Estágios com Detalhes e Status
        items(funnelStages.size) { index ->
            val stageInfo = funnelStages[index]
            val stageCopy = botFunnel.getOrNull(index)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (stageInfo.stageKey == MarketingFunnelWorker.STAGE_DAY_7_ANNUAL_CLOSE) AmberGold else DarkSurfaceBorder
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stageInfo.title,
                            color = if (stageInfo.stageKey == MarketingFunnelWorker.STAGE_DAY_7_ANNUAL_CLOSE) AmberGold else Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.5.sp
                        )

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (stageInfo.isDispatched) NeonEmerald.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.08f)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (stageInfo.isDispatched) "Disparado (${stageInfo.dispatchedAtFormatted})" else stageInfo.targetDelayDescription,
                                color = if (stageInfo.isDispatched) NeonEmerald else Color.LightGray,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (stageCopy != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF07090E))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = stageCopy.messageText,
                                color = Color.LightGray,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    ViralMarketingShareManager.copyToClipboard(context, stageCopy.messageText, stageCopy.stageTitle)
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                            ) {
                                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "Copiar Texto", fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    MarketingFunnelScheduler.triggerStageImmediately(
                                        context = context,
                                        stage = stageInfo.stageKey,
                                        phone = testPhone
                                    )
                                    refreshStages()
                                    Toast.makeText(context, "Etapa disparada via WorkManager!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen, contentColor = Color.Black)
                            ) {
                                Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "Testar Disparo", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
