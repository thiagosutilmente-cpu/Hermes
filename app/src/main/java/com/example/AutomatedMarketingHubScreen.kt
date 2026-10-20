package com.example

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Link
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

import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.runtime.collectAsState

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
 * Contém o motor de Métricas e Funil Inteligente orientado a telemetria,
 * simulador do Robô de WhatsApp na VPS, mensagens de grupos e roteiros para TikTok.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutomatedMarketingHubScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showRechartsDashboard by remember { mutableStateOf(false) }
    var showAsaasCockpit by remember { mutableStateOf(false) }
    val tabs = listOf(
        "📊 Métricas & Funil",
        "🎯 Tráfego Pago & Orgânico",
        "🤖 Robô do Zap (VPS)",
        "💬 Grupos de WhatsApp",
        "🎬 TikTok & Reels"
    )

    if (showAsaasCockpit) {
        AsaasAutomationCockpitScreen(
            onNavigateBack = { showAsaasCockpit = false }
        )
        return
    }

    if (showRechartsDashboard) {
        MarketingDashboard(
            onNavigateBack = { showRechartsDashboard = false }
        )
        return
    }

    BackHandler { onNavigateBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "CENTRAL DE MARKETING INTELIGENTE",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Telemetria de Lucros • Disparos FCM & WhatsApp",
                            fontSize = 11.sp,
                            color = NeonEmerald,
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
                actions = {
                    IconButton(
                        onClick = {
                            AsaasCheckoutManager.openCheckout(context, "general", "Jarvis Pro Campanha Geral")
                        },
                        modifier = Modifier.testTag("btn_open_asaas_checkout_topbar")
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = "Abrir Checkout Oficial Asaas no Navegador",
                            tint = AmberGold
                        )
                    }

                    IconButton(
                        onClick = { showAsaasCockpit = true },
                        modifier = Modifier.testTag("btn_open_asaas_cockpit_topbar")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = "Cockpit Asaas & WhatsApp",
                            tint = NeonEmerald
                        )
                    }

                    IconButton(
                        onClick = { showRechartsDashboard = true },
                        modifier = Modifier.testTag("btn_open_recharts_dashboard_topbar")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = "Gráficos Recharts",
                            tint = TikTokCyan
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
                            0 -> NeonEmerald
                            1 -> TikTokCyan
                            2 -> AmberGold
                            3 -> WhatsAppGreen
                            else -> TikTokCyan
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
                0 -> SmartMilestoneFunnelTabContent(onOpenRechartsDashboard = { showRechartsDashboard = true })
                1 -> TrafficEngineTabContent()
                2 -> WhatsAppBotAutomationTabContent()
                3 -> WhatsAppGroupsStrategyTabContent()
                4 -> TikTokReelsScriptsTabContent()
            }
        }
    }
}

// =============================================================================
// ABA 1: TRÁFEGO PAGO & ORGÂNICO (ANÚNCIOS META/TIKTOK ADS & ESTRATÉGIAS VIRAIS)
// =============================================================================
@Composable
private fun TrafficEngineTabContent() {
    val context = LocalContext.current
    androidx.compose.runtime.LaunchedEffect(Unit) {
        TrafficGrowthEngineManager.initialize(context)
        AsaasCheckoutManager.initialize(context)
    }

    val paidCampaigns = remember { TrafficGrowthEngineManager.getPaidTrafficCampaigns() }
    val organicStrategies = remember { TrafficGrowthEngineManager.getOrganicTrafficStrategies() }
    var selectedTrafficMode by remember { mutableIntStateOf(0) } // 0: Tráfego Pago, 1: Tráfego Orgânico

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("traffic_engine_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // CARD PRINCIPAL: CHECKOUT OFICIAL ASAAS NO NAVEGADOR
        item {
            AsaasCheckoutLiveTestingCard()
        }

        // Seletor de Modalidade (Pago vs Orgânico)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurface)
                    .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val modes = listOf("🎯 Tráfego Pago (Ads)", "🌱 Tráfego Orgânico (Viral)")
                modes.forEachIndexed { index, title ->
                    val isSelected = selectedTrafficMode == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) TikTokCyan.copy(alpha = 0.15f) else Color.Transparent)
                            .clickable { selectedTrafficMode = index }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            color = if (isSelected) TikTokCyan else Color.Gray,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        if (selectedTrafficMode == 0) {
            // SEÇÃO TRÁFEGO PAGO
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TikTokCyan.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Campaign, contentDescription = null, tint = TikTokCyan, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("CAMPANHAS VALIDADAS PARA META & TIKTOK ADS", color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Copie o texto, o gancho visual e o link com UTM trackeada para subir direto no Gerenciador de Anúncios com CPA médio de R$ 3,80 a R$ 5,00.",
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            items(paidCampaigns.size) { index ->
                val camp = paidCampaigns[index]
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_paid_campaign_${camp.id}"),
                    shape = RoundedCornerShape(14.dp),
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
                                    .background(Color(0xFF161C2A))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(text = camp.platform, color = TikTokCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(text = "CPA Est: R$ ${String.format(java.util.Locale("pt", "BR"), "%.2f", camp.estimatedCpaBrl)}", color = NeonEmerald, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(text = camp.title, color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Headline: \"${camp.headline}\"", color = AmberGold, fontWeight = FontWeight.Bold, fontSize = 11.sp)

                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF090B10))
                                .padding(10.dp)
                        ) {
                            Column {
                                Text("Texto Principal (Copy):", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(text = camp.primaryText, color = Color.LightGray, fontSize = 11.sp, lineHeight = 15.sp)

                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Gancho Visual Recomendado:", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(text = camp.visualHookDescription, color = TikTokCyan, fontSize = 10.5.sp, lineHeight = 14.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Público Alvo: ${camp.targetAudience}", color = Color.Gray, fontSize = 10.sp)
                        Text(text = "Orçamento Sugerido: ${camp.suggestedDailyBudget}", color = Color.Gray, fontSize = 10.sp)

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val clip = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    clip.setPrimaryClip(android.content.ClipData.newPlainText("Link UTM Anúncio", camp.utmLink))
                                    android.widget.Toast.makeText(context, "Link UTM copiado para o Gerenciador de Anúncios!", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f).height(36.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                            ) {
                                Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copiar Link UTM", fontSize = 10.sp)
                            }

                            Button(
                                onClick = {
                                    val fullAd = "${camp.headline}\n\n${camp.primaryText}\n\nLink: ${camp.utmLink}"
                                    val clip = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    clip.setPrimaryClip(android.content.ClipData.newPlainText("Copy Anúncio", fullAd))
                                    android.widget.Toast.makeText(context, "Copy completa do anúncio copiada!", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f).height(36.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = TikTokCyan, contentColor = Color.Black)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(13.dp), tint = Color.Black)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copiar Copy", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        } else {
            // SEÇÃO TRÁFEGO ORGÂNICO
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonEmerald.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.RocketLaunch, contentDescription = null, tint = NeonEmerald, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("MÁQUINA DE TRÁFEGO ORGÂNICO VIRAL", color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Estratégias de custo zero para infiltração em grupos de motoboys, postagens no horário nobre e vídeos no guidão que viralizam com prova de telemetria.",
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            items(organicStrategies.size) { index ->
                val org = organicStrategies[index]
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_organic_strat_${org.id}"),
                    shape = RoundedCornerShape(14.dp),
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
                                    .background(Color(0xFF11221A))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(text = org.channel, color = NeonEmerald, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(text = org.format, color = Color.LightGray, fontSize = 10.sp)
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(text = org.title, color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp)

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "Gancho: \"${org.hookText}\"", color = AmberGold, fontWeight = FontWeight.Bold, fontSize = 11.sp)

                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF090B10))
                                .padding(10.dp)
                        ) {
                            Text(text = org.scriptOrCopy, color = Color.LightGray, fontSize = 11.sp, lineHeight = 15.sp)
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "⏰ Melhor Horário: ${org.bestTimeToPost}", color = Color.Gray, fontSize = 10.sp)
                        Text(text = "Hashtags: ${org.hashtags.joinToString(" ")}", color = TikTokCyan, fontSize = 10.sp)

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                val fullCopy = "${org.hookText}\n\n${org.scriptOrCopy}\n\n${org.callToAction}\n\n${org.hashtags.joinToString(" ")}"
                                val clip = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                clip.setPrimaryClip(android.content.ClipData.newPlainText("Estratégia Orgânica", fullCopy))
                                android.widget.Toast.makeText(context, "Roteiro e copy orgânica copiados!", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth().height(36.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald, contentColor = Color.Black)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(13.dp), tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copiar Roteiro e Estratégia", fontSize = 10.5.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Card para teste interativo e cópia de URLs do Checkout Oficial do Asaas no navegador.
 */
@Composable
fun AsaasCheckoutLiveTestingCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedPlanKey by remember { mutableStateOf("general") }

    val currentUrl = remember(selectedPlanKey) {
        AsaasCheckoutManager.getCheckoutUrl(selectedPlanKey)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("card_asaas_checkout_live_testing"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.2.dp, AmberGold.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(AmberGold.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = null,
                            tint = AmberGold,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CHECKOUT OFICIAL ASAAS (WEB)",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.5.sp,
                        letterSpacing = 0.5.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(NeonEmerald.copy(alpha = 0.15f))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text("ONLINE 🌐", color = NeonEmerald, fontSize = 9.sp, fontWeight = FontWeight.Black)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Abra a página de checkout seguro do Asaas no navegador para validar os fluxos de pagamento (Cartão de Crédito, Boleto ou Pix) ou copie a URL para seus anúncios pagos/orgânicos.",
                color = Color.LightGray,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Seletor de Planos
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val planOptions = listOf(
                    "general" to "Geral",
                    "weekly" to "Semanal",
                    "monthly" to "Mensal",
                    "annual" to "Anual VIP"
                )
                planOptions.forEach { (key, label) ->
                    val isSelected = selectedPlanKey == key
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) AmberGold.copy(alpha = 0.2f) else Color(0xFF141824))
                            .border(1.dp, if (isSelected) AmberGold else DarkSurfaceBorder, RoundedCornerShape(8.dp))
                            .clickable { selectedPlanKey = key }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) AmberGold else Color.Gray,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Prévia da URL
            Surface(
                color = Color(0xFF0A0C12),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1B2232)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        tint = TikTokCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = currentUrl,
                        color = TikTokCyan,
                        fontSize = 10.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Botões de Ação
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Botão Abrir no Navegador
                Button(
                    onClick = {
                        val planTitle = when (selectedPlanKey) {
                            "weekly" -> "Jarvis Pro Semanal"
                            "monthly" -> "Jarvis Pro Mensal"
                            "annual" -> "Jarvis Pro Anual VIP"
                            else -> "Jarvis Pro Geral"
                        }
                        AsaasCheckoutManager.openCheckout(context, selectedPlanKey, planTitle)
                    },
                    modifier = Modifier
                        .weight(1.3f)
                        .height(38.dp)
                        .testTag("btn_open_asaas_browser_from_hub"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AmberGold, contentColor = Color.Black)
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = Color.Black
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Abrir no Navegador 🌐",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                // Botão Copiar Link
                OutlinedButton(
                    onClick = {
                        val clip = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        clip.setPrimaryClip(android.content.ClipData.newPlainText("Link Asaas Checkout", currentUrl))
                        android.widget.Toast.makeText(context, "Link do Asaas copiado com sucesso!", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("btn_copy_asaas_url_from_hub"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copiar Link", fontSize = 10.sp)
                }
            }
        }
    }
}

// =============================================================================
// ABA 0: MÉTRICAS & FUNIL INTELIGENTE (DISPAROS POR MARCOS & TELEMETRIA)
// =============================================================================
@Composable
private fun SmartMilestoneFunnelTabContent(
    onOpenRechartsDashboard: () -> Unit = {}
) {
    val context = LocalContext.current
    androidx.compose.runtime.LaunchedEffect(Unit) {
        SmartMarketingMilestoneManager.initialize(context)
    }

    val metrics by SmartMarketingMilestoneManager.metricsState.collectAsState()
    var funnelItems by remember { mutableStateOf(SmartMarketingMilestoneManager.getMilestoneFunnelList(context)) }

    fun refreshFunnel() {
        funnelItems = SmartMarketingMilestoneManager.getMilestoneFunnelList(context)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("smart_marketing_funnel_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. BANNER PRINCIPAL DE ROI DO MOTORISTA
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonEmerald.copy(alpha = 0.5f))
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
                                    .background(NeonEmerald.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = "ROI",
                                    tint = NeonEmerald,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "PAINEL DE CONVERSÃO & ROI",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Telemetria Real • Argumentos de Venda no Pix",
                                    color = NeonEmerald,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(NeonEmerald.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "FCM + Zap Ativos 🟢",
                                color = NeonEmerald,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // GRID 2x2 DE MÉTRICAS EM TEMPO REAL
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Card Gasolina
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0C1318)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E2D38))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.LocalGasStation,
                                        contentDescription = null,
                                        tint = AmberGold,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Gasolina Poupada", color = Color.Gray, fontSize = 10.sp)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = String.format(java.util.Locale("pt", "BR"), "R$ %.2f", metrics.fuelSavedBrl),
                                    color = AmberGold,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        // Card Corridas Recusadas
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF150C10)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF331822))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = Color(0xFFFF3366),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Prejuízos Barrados", color = Color.Gray, fontSize = 10.sp)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${metrics.badRidesRejected} viagens",
                                    color = Color(0xFFFF3366),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Card KM Otimizados
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF081418)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF132A33))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Analytics,
                                        contentDescription = null,
                                        tint = TikTokCyan,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("KM Sem Desgaste", color = Color.Gray, fontSize = 10.sp)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = String.format(java.util.Locale("pt", "BR"), "%.1f km", metrics.kmOptimized),
                                    color = TikTokCyan,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        // Card Lucro Líquido Real
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1810)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF133622))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.TrendingUp,
                                        contentDescription = null,
                                        tint = NeonEmerald,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Lucro Líquido", color = Color.Gray, fontSize = 10.sp)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = String.format(java.util.Locale("pt", "BR"), "R$ %.2f", metrics.netEarningsBrl),
                                    color = NeonEmerald,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Estatísticas de Disparo e Conversão
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF07090D))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Total de Disparos: ${metrics.totalDispatches}", color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("Conversões Pix: ${metrics.conversionsCount}", color = NeonEmerald, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = "Último: ${metrics.lastDispatchFormatted}",
                            color = Color.Gray,
                            fontSize = 10.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Botão Acesso aos Gráficos Recharts
                    Button(
                        onClick = onOpenRechartsDashboard,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_open_recharts_dashboard_card"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TikTokCyan.copy(alpha = 0.15f),
                            contentColor = TikTokCyan
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TikTokCyan.copy(alpha = 0.4f))
                    ) {
                        Icon(imageVector = Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ver Gráficos Recharts (Lucros vs KM)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 2. PAINEL DE SIMULAÇÃO E TESTES
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "SIMULADOR DE TELEMETRIA & TESTES",
                        color = AmberGold,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Adicione métricas de corrida em 1 clique para testar o destravamento e os disparos do funil inteligente.",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                SmartMarketingMilestoneManager.simulateRideMetricsBonus(context)
                                refreshFunnel()
                                Toast.makeText(context, "+R$ 12,50 gas / +35 lucro adicionados à telemetria!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .weight(1.3f)
                                .testTag("btn_simulate_bonus_metrics"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald, contentColor = Color.Black)
                        ) {
                            Icon(imageVector = Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Simular Corrida (+R$ 12,50)", fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }

                        OutlinedButton(
                            onClick = {
                                SmartMarketingMilestoneManager.resetDemoMetrics(context)
                                refreshFunnel()
                                Toast.makeText(context, "Métricas restauradas para padrão demo!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .weight(0.9f)
                                .testTag("btn_reset_demo_metrics"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.LightGray)
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Resetar", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // 3. CABEÇALHO DO FUNIL DE MARCOS
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ETAPAS DO FUNIL INTELIGENTE (ROI)",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "${funnelItems.count { it.isUnlocked }} de ${funnelItems.size} liberadas",
                    color = NeonEmerald,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 4. ITENS DO FUNIL BASEADOS EM MARCOS
        items(funnelItems.size) { index ->
            val item = funnelItems[index]
            val borderColor = when {
                item.isDispatched -> NeonEmerald.copy(alpha = 0.6f)
                item.isUnlocked -> AmberGold.copy(alpha = 0.5f)
                else -> DarkSurfaceBorder
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_milestone_${item.stageKey}"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.title,
                            color = if (item.isUnlocked) Color.White else Color.Gray,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (item.isDispatched) NeonEmerald.copy(alpha = 0.2f)
                                    else if (item.isUnlocked) AmberGold.copy(alpha = 0.2f)
                                    else Color.White.copy(alpha = 0.05f)
                                )
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (item.isDispatched) "DISPARADO 🟢" else if (item.isUnlocked) "DESTRAVADO 🟡" else "BLOQUEADO 🔒",
                                color = if (item.isDispatched) NeonEmerald else if (item.isUnlocked) AmberGold else Color.Gray,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Gatilho: ${item.triggerCondition}",
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // PREVIEW DA CÓPIA PERSONALIZADA
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF07090E))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = item.previewCopy,
                            color = Color(0xFFC0CAD5),
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // AÇÕES DE DISPARO
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Disparar Push FCM
                        Button(
                            onClick = {
                                SmartMarketingMilestoneManager.dispatchMilestone(context, item.stageKey)
                                refreshFunnel()
                                Toast.makeText(context, "Notificação Push Heads-up emitida!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .weight(1.1f)
                                .testTag("btn_dispatch_push_${item.stageKey}"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald, contentColor = Color.Black)
                        ) {
                            Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Push FCM", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // Abrir no WhatsApp
                        Button(
                            onClick = {
                                ViralMarketingShareManager.shareToWhatsApp(context, item.previewCopy)
                            },
                            modifier = Modifier
                                .weight(1.1f)
                                .testTag("btn_share_whatsapp_${item.stageKey}"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen, contentColor = Color.White)
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("WhatsApp", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // Copiar Texto
                        OutlinedButton(
                            onClick = {
                                ViralMarketingShareManager.copyToClipboard(context, item.previewCopy, item.title)
                            },
                            modifier = Modifier
                                .weight(0.9f)
                                .testTag("btn_copy_text_${item.stageKey}"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.LightGray)
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Copiar", fontSize = 11.sp)
                        }
                    }
                }
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
