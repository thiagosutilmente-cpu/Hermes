package com.example

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.NotificationCompat
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

// Paleta Dark Tática do Jarvis
private val DarkBg = Color(0xFF090A0F)
private val DarkSurface = Color(0xFF11141C)
private val DarkSurfaceBorder = Color(0xFF1E2433)
private val NeonEmerald = Color(0xFF00FF88)
private val TikTokCyan = Color(0xFF00F2FE)
private val AmberGold = Color(0xFFFFB800)
private val ElectricPurple = Color(0xFF8B5CF6)

private const val SIMULATED_CHANNEL_ID = "jarvis_simulated_marketing_channel"
private const val SIMULATED_CHANNEL_NAME = "Simulações de Notificação Jarvis"

/**
 * Modelo de dados para análise de conversão e telemetria de campanhas
 */
data class CampaignDataPoint(
    val label: String,
    val profitBrl: Double,
    val distanceKm: Double,
    val conversionRatePercent: Double,
    val conversionsCount: Int,
    val ratePerKm: Double = if (distanceKm > 0) profitBrl / distanceKm else 0.0
)

/**
 * Interface Javascript para receber interações do gráfico Recharts no WebView
 */
class MarketingDashboardBridge(
    private val onPointSelected: (label: String, profit: Double, km: Double, rate: Double) -> Unit
) {
    @JavascriptInterface
    fun onChartPointClick(label: String, profit: Double, km: Double, rate: Double) {
        onPointSelected(label, profit, km, rate)
    }
}

/**
 * Dispara uma notificação Heads-Up real no Android com os parâmetros do cenário hipotético
 */
fun dispatchSimulatedMarketingNotification(
    context: Context,
    profit: Double,
    km: Double,
    ratePerKm: Double,
    fuelSaved: Double
): Pair<String, String> {
    val rateFormatted = String.format(Locale("pt", "BR"), "R$ %.2f", ratePerKm)
    val profitFormatted = String.format(Locale("pt", "BR"), "R$ %.2f", profit)
    val kmFormatted = String.format(Locale("pt", "BR"), "%.1f km", km)
    val fuelSavedFormatted = String.format(Locale("pt", "BR"), "R$ %.2f", fuelSaved)

    val title = "⛽ Jarvis: $rateFormatted/km batido hoje! ($profitFormatted)"
    val body = "🔥 Telemetria de Hoje: Você rodou $kmFormatted com lucro líquido de $profitFormatted ($rateFormatted/km)! Economia estimada em combustível: $fuelSavedFormatted ao descartar corridas predatórias. Mantenha seu copiloto ativo: garanta o Jarvis Pro com 15% OFF via Pix!"

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            SIMULATED_CHANNEL_ID,
            SIMULATED_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Canal para testes e simulações do funil de marketing inteligente"
            enableVibration(true)
        }
        notificationManager.createNotificationChannel(channel)
    }

    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val launchIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val pendingIntent = PendingIntent.getActivity(
        context,
        88219,
        launchIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(context, SIMULATED_CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle(title)
        .setContentText("Cenário: $kmFormatted • $profitFormatted ($rateFormatted/km)")
        .setStyle(NotificationCompat.BigTextStyle().bigText(body))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setCategory(NotificationCompat.CATEGORY_PROMO)
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)
        .build()

    notificationManager.notify(88219, notification)
    return Pair(title, body)
}

/**
 * COMPONENTE 'MarketingDashboard' EM COMPOSE
 *
 * Exibe gráficos de conversão de campanhas usando a biblioteca Recharts (via WebView embutida)
 * para visualizar em tempo real a correlação entre lucros líquidos (R$) e KM rodado pelos motoboys.
 * Conta com painel tático interativo e botão 'Simular Disparo' para testes de notificações.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketingDashboard(
    onNavigateBack: (() -> Unit)? = null,
    onOpenAsaasCockpit: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showAsaasScreen by remember { mutableStateOf(false) }
    var selectedTimeframe by remember { mutableIntStateOf(0) } // 0: 7 Dias, 1: 30 Dias, 2: Campanhas Pix
    var selectedPointDetails by remember { mutableStateOf<String?>(null) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    if (showAsaasScreen) {
        AsaasAutomationCockpitScreen(
            onNavigateBack = { showAsaasScreen = false }
        )
        return
    }

    // Inicializa o gerenciador de logs persistente
    androidx.compose.runtime.LaunchedEffect(Unit) {
        MarketingDispatchLogManager.initialize(context)
        AsaasWebhookSimulatorManager.initialize(context)
    }

    // Estado do Cenário Hipotético de Lucros por KM
    var selectedScenarioName by remember { mutableStateOf("Personalizado") }
    var hypotheticalProfit by remember { mutableDoubleStateOf(220.0) }
    var hypotheticalKm by remember { mutableDoubleStateOf(38.0) }
    val hypotheticalRate = remember(hypotheticalProfit, hypotheticalKm) {
        if (hypotheticalKm > 0) hypotheticalProfit / hypotheticalKm else 0.0
    }
    val hypotheticalFuelSaved = remember(hypotheticalKm) {
        hypotheticalKm * 0.95 // Média estimada de R$ 0,95 por KM otimizado
    }
    var simulatedNotificationResult by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showNotificationPreview by remember { mutableStateOf(false) }

    // Conjuntos de dados calibrados para as campanhas do Jarvis
    val currentData = remember(selectedTimeframe) {
        when (selectedTimeframe) {
            0 -> listOf(
                CampaignDataPoint("Seg", 145.0, 32.5, 4.2, 3),
                CampaignDataPoint("Ter", 180.0, 38.0, 5.8, 4),
                CampaignDataPoint("Qua", 210.0, 42.1, 7.5, 6),
                CampaignDataPoint("Qui", 195.0, 39.4, 6.1, 5),
                CampaignDataPoint("Sex", 280.0, 54.2, 9.4, 9),
                CampaignDataPoint("Sáb", 340.0, 62.0, 12.8, 14),
                CampaignDataPoint("Dom", 310.0, 56.5, 11.2, 11)
            )
            1 -> listOf(
                CampaignDataPoint("Semana 1", 980.0, 210.0, 5.2, 22),
                CampaignDataPoint("Semana 2", 1250.0, 260.0, 7.4, 34),
                CampaignDataPoint("Semana 3", 1480.0, 295.0, 9.8, 48),
                CampaignDataPoint("Semana 4", 1720.0, 330.0, 13.5, 65)
            )
            else -> listOf(
                CampaignDataPoint("Boas-Vindas", 450.0, 98.0, 8.5, 18),
                CampaignDataPoint("Gasolina R$30", 820.0, 160.0, 14.2, 39),
                CampaignDataPoint("10 Lixos Fora", 1100.0, 220.0, 18.6, 52),
                CampaignDataPoint("Fechamento Pix", 1650.0, 310.0, 24.0, 78)
            )
        }
    }

    val totalProfit = currentData.sumOf { it.profitBrl }
    val totalKm = currentData.sumOf { it.distanceKm }
    val avgRatePerKm = if (totalKm > 0) totalProfit / totalKm else 0.0
    val totalConversions = currentData.sumOf { it.conversionsCount }

    // Ganhos diários e semanais para rastreamento de metas
    val dailyGoalState by DailyGoalTrackerManager.getInstance(context).goalState.collectAsState()
    val currentEarningsDaily = if (dailyGoalState.grossEarned > 0) dailyGoalState.grossEarned else (currentData.lastOrNull()?.profitBrl ?: 310.0)
    val currentEarningsWeekly = if (selectedTimeframe == 0) totalProfit else (totalProfit * 0.25)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "MARKETING DASHBOARD",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Gráficos Recharts • Lucros vs KM Rodado",
                            fontSize = 11.sp,
                            color = NeonEmerald,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag("btn_back_marketing_dashboard")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Voltar",
                                tint = Color.White
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (onOpenAsaasCockpit != null) {
                                onOpenAsaasCockpit()
                            } else {
                                showAsaasScreen = true
                            }
                        },
                        modifier = Modifier.testTag("btn_open_asaas_from_marketing_dashboard")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = "Automação Asaas & Webhook",
                            tint = TikTokCyan
                        )
                    }

                    IconButton(
                        onClick = {
                            val result = MarketingCsvExporter.exportToCsv(
                                context = context,
                                dispatchLogs = MarketingDispatchLogManager.logs.toList(),
                                campaignDataPoints = currentData
                            )
                            if (result.success && result.file != null) {
                                Toast.makeText(
                                    context,
                                    "Relatório CSV exportado com sucesso! (${result.rowCount} linhas)",
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
                        modifier = Modifier.testTag("btn_export_csv_marketing_dashboard")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Exportar Histórico e Métricas para CSV",
                            tint = NeonEmerald
                        )
                    }

                    IconButton(
                        onClick = {
                            webViewRef?.reload()
                            Toast.makeText(context, "Gráficos Recharts atualizados!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("btn_refresh_marketing_dashboard")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Atualizar",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
            )
        },
        containerColor = DarkBg,
        modifier = modifier.testTag("marketing_dashboard_container")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. SELETOR DE PERÍODO / CAMPANHA
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
                    val timeframes = listOf("Últimos 7 Dias", "Mensal", "Por Campanha")
                    timeframes.forEachIndexed { index, label ->
                        val isSelected = selectedTimeframe == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) NeonEmerald.copy(alpha = 0.15f) else Color.Transparent)
                                .clickable { selectedTimeframe = index }
                                .padding(vertical = 8.dp)
                                .testTag("timeframe_tab_$index"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) NeonEmerald else Color.Gray,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // 2. CARDS DE KPI (LUCRO, KM, R$/KM E CONVERSÕES)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Lucro Total
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonEmerald.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = NeonEmerald, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Lucro Total", color = Color.Gray, fontSize = 10.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = String.format(Locale("pt", "BR"), "R$ %.2f", totalProfit),
                                color = NeonEmerald,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    // KM Rodado
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TikTokCyan.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Analytics, contentDescription = null, tint = TikTokCyan, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("KM Rodado", color = Color.Gray, fontSize = 10.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = String.format(Locale("pt", "BR"), "%.1f km", totalKm),
                                color = TikTokCyan,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Eficiência R$/KM
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AmberGold.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocalGasStation, contentDescription = null, tint = AmberGold, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("R$/KM Médio", color = Color.Gray, fontSize = 10.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = String.format(Locale("pt", "BR"), "R$ %.2f/km", avgRatePerKm),
                                color = AmberGold,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    // Conversões
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ElectricPurple.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = ElectricPurple, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Conversões Pix", color = Color.Gray, fontSize = 10.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$totalConversions assinantes",
                                color = ElectricPurple,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }

            // 3. METAS PERSONALIZADAS DE GANHOS (DIÁRIAS OU SEMANAIS) COM DESTAQUE VISUAL
            item {
                MarketingGoalTrackerCard(
                    currentEarningsDaily = currentEarningsDaily,
                    currentEarningsWeekly = currentEarningsWeekly
                )
            }

            // 3.1 DADOS DO WEBHOOK ASAAS NA VPS (CONVERSÕES, METAS & RETENÇÃO)
            item {
                AsaasWebhookConversionCard(
                    onOpenAsaasCockpit = {
                        if (onOpenAsaasCockpit != null) {
                            onOpenAsaasCockpit()
                        } else {
                            showAsaasScreen = true
                        }
                    }
                )
            }

            // 4. CONTAINER DO GRÁFICO RECHARTS EMBUTIDO VIA WEBVIEW
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_recharts_chart"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "RECHARTS: CORRELAÇÃO LUCRO vs KM",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "Área: Lucro (R$) • Barras: KM • Linha: Taxa (%)",
                                    color = Color.Gray,
                                    fontSize = 10.sp
                                )
                            }

                            // Legenda
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(NeonEmerald))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Lucro", color = Color.LightGray, fontSize = 9.sp)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(TikTokCyan))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("KM", color = Color.LightGray, fontSize = 9.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // O componente AndroidView que renderiza a WebView com o Recharts
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(310.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF06070B))
                                .border(1.dp, Color(0xFF151922), RoundedCornerShape(12.dp))
                                .testTag("recharts_webview_container")
                        ) {
                            AndroidView(
                                factory = { ctx ->
                                    WebView(ctx).apply {
                                        layoutParams = ViewGroup.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.MATCH_PARENT
                                        )
                                        settings.apply {
                                            @SuppressLint("SetJavaScriptEnabled")
                                            javaScriptEnabled = true
                                            domStorageEnabled = true
                                            loadWithOverviewMode = true
                                            useWideViewPort = true
                                            cacheMode = WebSettings.LOAD_NO_CACHE
                                        }
                                        setBackgroundColor(android.graphics.Color.TRANSPARENT)

                                        // Bridge de comunicação JS -> Kotlin
                                        addJavascriptInterface(
                                            MarketingDashboardBridge { label, profit, km, rate ->
                                                selectedPointDetails = "Ponto Selecionado: $label • Lucro: R$ ${String.format(Locale("pt", "BR"), "%.2f", profit)} • Distância: ${String.format(Locale("pt", "BR"), "%.1f", km)} km • Eficiência: R$ ${String.format(Locale("pt", "BR"), "%.2f", rate)}/km"
                                            },
                                            "MarketingBridge"
                                        )

                                        webChromeClient = WebChromeClient()
                                        webViewClient = WebViewClient()

                                        val html = buildRechartsHtml(currentData)
                                        loadDataWithBaseURL("https://recharts.org", html, "text/html", "UTF-8", null)
                                        webViewRef = this
                                    }
                                },
                                update = { wv ->
                                    val html = buildRechartsHtml(currentData)
                                    wv.loadDataWithBaseURL("https://recharts.org", html, "text/html", "UTF-8", null)
                                    webViewRef = wv
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Informação do ponto clicado
                        AnimatedVisibility(visible = selectedPointDetails != null) {
                            selectedPointDetails?.let { details ->
                                Spacer(modifier = Modifier.height(10.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(NeonEmerald.copy(alpha = 0.1f))
                                        .border(1.dp, NeonEmerald.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                        .padding(10.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Info, contentDescription = null, tint = NeonEmerald, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(text = details, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. PAINEL DE SIMULAÇÃO DE CENÁRIO HIPOTÉTICO & BOTÃO 'SIMULAR DISPARO'
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_simulate_dispatch_container"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonEmerald.copy(alpha = 0.45f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Cabeçalho da Seção de Simulação
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
                                        .background(NeonEmerald.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = null,
                                        tint = NeonEmerald,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "SIMULADOR DE CENÁRIO HIPOTÉTICO",
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp,
                                        letterSpacing = 0.5.sp
                                    )
                                    Text(
                                        text = "Ajuste os valores e teste o disparo da notificação",
                                        color = Color.Gray,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            // Badge de Eficiência Dinâmica do Cenário
                            val efficiencyColor = when {
                                hypotheticalRate >= 5.0 -> NeonEmerald
                                hypotheticalRate >= 3.5 -> AmberGold
                                else -> Color(0xFFFF5252)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(efficiencyColor.copy(alpha = 0.15f))
                                    .border(1.dp, efficiencyColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = String.format(Locale("pt", "BR"), "R$ %.2f/km", hypotheticalRate),
                                    color = efficiencyColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Cenários Rápidos Pré-configurados (Chips)
                        Text(
                            text = "Cenários Pré-Configurados:",
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val presets = listOf(
                                Triple("Padrão", 180.0, 38.0),
                                Triple("Super Sexta", 310.0, 48.0),
                                Triple("Chuva Alta", 440.0, 58.0),
                                Triple("Curta VIP", 125.0, 18.0)
                            )
                            presets.forEach { (name, p, k) ->
                                val isCurrent = (hypotheticalProfit == p && hypotheticalKm == k)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isCurrent) NeonEmerald.copy(alpha = 0.2f) else DarkBg)
                                        .border(1.dp, if (isCurrent) NeonEmerald else DarkSurfaceBorder, RoundedCornerShape(8.dp))
                                        .clickable {
                                            hypotheticalProfit = p
                                            hypotheticalKm = k
                                            selectedScenarioName = name
                                        }
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = name,
                                        color = if (isCurrent) NeonEmerald else Color.LightGray,
                                        fontSize = 10.sp,
                                        fontWeight = if (isCurrent) FontWeight.Black else FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Slider 1: Lucro Líquido Hipotético (R$)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Lucro Líquido Hipotético:",
                                color = Color.LightGray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = String.format(Locale("pt", "BR"), "R$ %.2f", hypotheticalProfit),
                                color = NeonEmerald,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Slider(
                            value = hypotheticalProfit.toFloat(),
                            onValueChange = {
                                hypotheticalProfit = (Math.round(it * 10) / 10.0)
                                selectedScenarioName = "Personalizado"
                            },
                            valueRange = 50f..600f,
                            colors = SliderDefaults.colors(
                                thumbColor = NeonEmerald,
                                activeTrackColor = NeonEmerald,
                                inactiveTrackColor = DarkSurfaceBorder
                            ),
                            modifier = Modifier.testTag("slider_hypothetical_profit")
                        )

                        // Slider 2: KM Rodado Hipotético (km)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Distância Percorrida Hipotética:",
                                color = Color.LightGray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = String.format(Locale("pt", "BR"), "%.1f km", hypotheticalKm),
                                color = TikTokCyan,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Slider(
                            value = hypotheticalKm.toFloat(),
                            onValueChange = {
                                hypotheticalKm = (Math.round(it * 10) / 10.0)
                                selectedScenarioName = "Personalizado"
                            },
                            valueRange = 10f..100f,
                            colors = SliderDefaults.colors(
                                thumbColor = TikTokCyan,
                                activeTrackColor = TikTokCyan,
                                inactiveTrackColor = DarkSurfaceBorder
                            ),
                            modifier = Modifier.testTag("slider_hypothetical_km")
                        )

                        // Resumo em Régua Tática das Métricas Calculadas
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkBg)
                                .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Taxa R$/KM", color = Color.Gray, fontSize = 10.sp)
                                Text(
                                    text = String.format(Locale("pt", "BR"), "R$ %.2f / km", hypotheticalRate),
                                    color = when {
                                        hypotheticalRate >= 5.0 -> NeonEmerald
                                        hypotheticalRate >= 3.5 -> AmberGold
                                        else -> Color(0xFFFF5252)
                                    },
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Gasolina Poupada", color = Color.Gray, fontSize = 10.sp)
                                Text(
                                    text = String.format(Locale("pt", "BR"), "R$ %.2f", hypotheticalFuelSaved),
                                    color = AmberGold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Classificação", color = Color.Gray, fontSize = 10.sp)
                                Text(
                                    text = if (hypotheticalRate >= 5.0) "🟢 Rota Ouro" else if (hypotheticalRate >= 3.5) "🟡 Rota Média" else "🔴 Alerta",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // BOTÃO SOLICITADO: 'Simular Disparo'
                        Button(
                            onClick = {
                                val result = dispatchSimulatedMarketingNotification(
                                    context = context,
                                    profit = hypotheticalProfit,
                                    km = hypotheticalKm,
                                    ratePerKm = hypotheticalRate,
                                    fuelSaved = hypotheticalFuelSaved
                                )
                                // Registra o disparo no histórico persistente de logs
                                MarketingDispatchLogManager.recordDispatch(
                                    context = context,
                                    scenarioName = selectedScenarioName,
                                    profitBrl = hypotheticalProfit,
                                    distanceKm = hypotheticalKm,
                                    ratePerKm = hypotheticalRate,
                                    fuelSavedBrl = hypotheticalFuelSaved,
                                    notificationTitle = result.first,
                                    notificationBody = result.second,
                                    deliveryStatus = "Entregue (Heads-Up)",
                                    isSuccess = true
                                )
                                simulatedNotificationResult = result
                                showNotificationPreview = true
                                Toast.makeText(
                                    context,
                                    "⚡ Disparo simulado e registrado no histórico persistente!",
                                    Toast.LENGTH_LONG
                                ).show()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("btn_simulate_dispatch"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonEmerald,
                                contentColor = Color.Black
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = "Simular Disparo",
                                    tint = Color.Black,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Simular Disparo",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }

                        // PRÉVIA VISUAL DA NOTIFICAÇÃO (APARÊNCIA HEADS-UP REAL NO APP)
                        AnimatedVisibility(
                            visible = showNotificationPreview && simulatedNotificationResult != null,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            simulatedNotificationResult?.let { (title, body) ->
                                Column(
                                    modifier = Modifier
                                        .padding(top = 16.dp)
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0xFF141923))
                                        .border(1.5.dp, NeonEmerald.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                                        .padding(14.dp)
                                        .testTag("simulated_notification_preview_card")
                                ) {
                                    // Barra superior da Notificação Push simulada
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clip(CircleShape)
                                                    .background(NeonEmerald),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("J", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 11.sp)
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "JARVIS INTELIGÊNCIA • agora",
                                                color = Color.LightGray,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        IconButton(
                                            onClick = { showNotificationPreview = false },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Fechar Prévia",
                                                tint = Color.Gray,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Título da Notificação
                                    Text(
                                        text = title,
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp,
                                        lineHeight = 17.sp
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Corpo da Notificação
                                    Text(
                                        text = body,
                                        color = Color(0xFFD1D5DB),
                                        fontSize = 11.sp,
                                        lineHeight = 16.sp
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Ações Rápidas na Notificação
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                clipboard.setPrimaryClip(ClipData.newPlainText("Notificação Simulada", "$title\n\n$body"))
                                                Toast.makeText(context, "Texto da notificação copiado!", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(36.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                                        ) {
                                            Icon(
                                                Icons.Default.ContentCopy,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = Color.LightGray
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Copiar", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Button(
                                            onClick = {
                                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "text/plain"
                                                    putExtra(Intent.EXTRA_TEXT, "$title\n\n$body")
                                                }
                                                context.startActivity(Intent.createChooser(shareIntent, "Testar Compartilhamento"))
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(36.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = TikTokCyan,
                                                contentColor = Color.Black
                                            )
                                        ) {
                                            Text("Compartilhar", fontSize = 10.sp, fontWeight = FontWeight.Black)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 6. HISTÓRICO PERSISTENTE DE DISPAROS DE MARKETING (LOGS DE DATA, CENÁRIO E STATUS PUSH)
            item {
                MarketingDispatchHistoryCard()
            }

            // 7. INSIGHTS TÁTICOS SOBRE A RELAÇÃO LUCRO vs KM
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TrendingUp, contentDescription = null, tint = AmberGold, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ANÁLISE DE CONVERSÃO & EFICIÊNCIA",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "• Rentabilidade Máxima no Fim de Semana: Sexta e Sábado atingiram o pico de R$ 5,48/KM com 12% de conversão nas campanhas Pix.",
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "• Prova de ROI: Dias com maior KM rodado apresentaram redução no custo de combustível por litro, validando a filtragem de corridas curtas predatórias.",
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Constrói o HTML com Recharts / React responsivo e fallback SVG/Canvas de alta performance
 */
private fun buildRechartsHtml(dataPoints: List<CampaignDataPoint>): String {
    val jsonArray = JSONArray()
    for (dp in dataPoints) {
        val obj = JSONObject()
        obj.put("name", dp.label)
        obj.put("profit", dp.profitBrl)
        obj.put("km", dp.distanceKm)
        obj.put("rate", dp.ratePerKm)
        obj.put("conversions", dp.conversionsCount)
        jsonArray.put(obj)
    }

    return """
<!DOCTYPE html>
<html lang="pt-BR">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
  <title>Recharts Marketing Dashboard</title>
  <script src="https://unpkg.com/react@18/umd/react.production.min.js"></script>
  <script src="https://unpkg.com/react-dom@18/umd/react-dom.production.min.js"></script>
  <script src="https://unpkg.com/recharts@2.12.7/umd/Recharts.min.js"></script>
  <style>
    * { margin: 0; padding: 0; box-sizing: border-box; -webkit-tap-highlight-color: transparent; }
    body {
      background: #06070B;
      color: #E2E8F0;
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
      overflow: hidden;
      width: 100vw;
      height: 100vh;
      display: flex;
      flex-direction: column;
    }
    #root {
      width: 100%;
      height: 100%;
      display: flex;
      flex-direction: column;
    }
    /* SVG Fallback Canvas */
    .svg-chart {
      width: 100%;
      height: 100%;
    }
    .grid-line { stroke: #1E2433; stroke-dasharray: 3 3; }
    .bar-km { fill: #00F2FE; opacity: 0.75; cursor: pointer; transition: opacity 0.2s; }
    .bar-km:hover { opacity: 1; }
    .line-profit { fill: none; stroke: #00FF88; stroke-width: 3; }
    .dot-profit { fill: #00FF88; stroke: #06070B; stroke-width: 2; cursor: pointer; }
    .axis-label { fill: #94A3B8; font-size: 10px; font-weight: 600; text-anchor: middle; }
    .val-label { fill: #FFFFFF; font-size: 9px; font-weight: 700; text-anchor: middle; }
  </style>
</head>
<body>
  <div id="root"></div>

  <script>
    const chartData = $jsonArray;

    function notifyAndroid(label, profit, km, rate) {
      if (window.MarketingBridge && window.MarketingBridge.onChartPointClick) {
        window.MarketingBridge.onChartPointClick(label, profit, km, rate);
      }
    }

    // Tenta renderizar via Recharts nativo (React) se a lib estiver disponível
    try {
      if (window.Recharts && window.React && window.ReactDOM) {
        const { ResponsiveContainer, ComposedChart, Area, Bar, Line, XAxis, YAxis, Tooltip, CartesianGrid } = window.Recharts;
        const e = React.createElement;

        function RechartsView() {
          return e(ResponsiveContainer, { width: '100%', height: '100%' },
            e(ComposedChart, {
              data: chartData,
              margin: { top: 20, right: 15, bottom: 20, left: 5 },
              onClick: (e) => {
                if (e && e.activePayload && e.activePayload[0]) {
                  const d = e.activePayload[0].payload;
                  notifyAndroid(d.name, d.profit, d.km, d.rate);
                }
              }
            },
              e(CartesianGrid, { stroke: '#1E2433', strokeDasharray: '3 3' }),
              e(XAxis, { dataKey: 'name', stroke: '#64748B', tick: { fill: '#94A3B8', fontSize: 11, fontWeight: 'bold' } }),
              e(YAxis, { yAxisId: 'left', stroke: '#00FF88', tick: { fill: '#00FF88', fontSize: 10 }, tickFormatter: (v) => 'R$' + v }),
              e(YAxis, { yAxisId: 'right', orientation: 'right', stroke: '#00F2FE', tick: { fill: '#00F2FE', fontSize: 10 }, tickFormatter: (v) => v + 'k' }),
              e(Tooltip, {
                contentStyle: { background: '#11141C', borderColor: '#1E2433', borderRadius: '8px', color: '#fff', fontSize: '11px' },
                formatter: (val, name) => [name === 'profit' ? 'R$ ' + val.toFixed(2) : val + ' km', name === 'profit' ? 'Lucro' : 'Distância']
              }),
              e(Bar, { yAxisId: 'right', dataKey: 'km', fill: '#00F2FE', radius: [4, 4, 0, 0], opacity: 0.7 }),
              e(Area, { yAxisId: 'left', type: 'monotone', dataKey: 'profit', fill: 'rgba(0, 255, 136, 0.15)', stroke: '#00FF88', strokeWidth: 2 })
            )
          );
        }

        const root = ReactDOM.createRoot(document.getElementById('root'));
        root.render(e(RechartsView));
      } else {
        renderSvgFallback();
      }
    } catch (err) {
      console.warn('Recharts CDN fallback acionado:', err);
      renderSvgFallback();
    }

    // Renderizador SVG de alta fidelidade como fallback imediato
    function renderSvgFallback() {
      const container = document.getElementById('root');
      const w = window.innerWidth || 340;
      const h = window.innerHeight || 310;
      const padL = 36;
      const padR = 36;
      const padT = 25;
      const padB = 30;
      const plotW = w - padL - padR;
      const plotH = h - padT - padB;
      const count = chartData.length;
      const step = plotW / Math.max(1, count);

      let maxP = Math.max(10, ...chartData.map(d => d.profit));
      let maxK = Math.max(10, ...chartData.map(d => d.km));

      let svg = '<svg class="svg-chart" viewBox="0 0 ' + w + ' ' + h + '" xmlns="http://www.w3.org/2000/svg">';
      
      // Linhas de Grade
      for (let i = 0; i <= 4; i++) {
        let y = padT + (plotH / 4) * i;
        svg += '<line x1="' + padL + '" y1="' + y + '" x2="' + (w - padR) + '" y2="' + y + '" class="grid-line" />';
      }

      // Pontos para a linha de lucro
      let linePoints = [];

      chartData.forEach((d, idx) => {
        let cx = padL + step * idx + step / 2;
        let barH = (d.km / maxK) * (plotH * 0.75);
        let barY = padT + plotH - barH;
        let barW = Math.max(8, step * 0.45);

        // Barra KM
        svg += '<rect x="' + (cx - barW/2) + '" y="' + barY + '" width="' + barW + '" height="' + barH + '" rx="3" class="bar-km" onclick="notifyAndroid(\'' + d.name + '\', ' + d.profit + ', ' + d.km + ', ' + d.rate + ')" />';

        // Ponto Lucro
        let py = padT + plotH - (d.profit / maxP) * (plotH * 0.85);
        linePoints.push(cx + ',' + py);

        // Rótulo X
        svg += '<text x="' + cx + '" y="' + (h - 8) + '" class="axis-label">' + d.name + '</text>';
        // Valor no topo da barra
        svg += '<text x="' + cx + '" y="' + (barY - 4) + '" class="val-label">' + Math.round(d.km) + 'k</text>';
      });

      // Linha e Área de Lucro
      if (linePoints.length > 0) {
        svg += '<polyline points="' + linePoints.join(' ') + '" class="line-profit" />';
        chartData.forEach((d, idx) => {
          let cx = padL + step * idx + step / 2;
          let py = padT + plotH - (d.profit / maxP) * (plotH * 0.85);
          svg += '<circle cx="' + cx + '" cy="' + py + '" r="4" class="dot-profit" onclick="notifyAndroid(\'' + d.name + '\', ' + d.profit + ', ' + d.km + ', ' + d.rate + ')" />';
        });
      }

      svg += '</svg>';
      container.innerHTML = svg;
    }
  </script>
</body>
</html>
""".trimIndent()
}
