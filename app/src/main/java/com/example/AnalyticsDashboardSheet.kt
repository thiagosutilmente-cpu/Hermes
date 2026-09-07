package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

private val DarkBg = Color(0xFF0A0A0F)
private val DarkCard = Color(0xFF13131D)
private val DarkBorder = Color(0xFF262638)
private val NeonGreen = Color(0xFF00FF88)
private val GoldAccent = Color(0xFFFFD700)
private val BlueAccent = Color(0xFF38BDF8)
private val TextLight = Color(0xFFF3F4F6)
private val TextMuted = Color(0xFF9CA3AF)

/**
 * Painel Interativo de Métricas do Firebase Analytics:
 * - Funil de Conversão de Assinaturas (Impressão -> Seleção -> Checkout -> Compra)
 * - Retenção de Entregadores (D1, D3, D7, D14, D30) e Streaks
 * - Utilização de Recursos Premium exclusivos
 * - Feed de Telemetria em Tempo Real com payload de parâmetros
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsDashboardSheet(
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val funnel by FirebaseAnalyticsManager.funnelMetrics.collectAsState()
    val retention by FirebaseAnalyticsManager.retentionMetrics.collectAsState()
    val events by FirebaseAnalyticsManager.recentEvents.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0: Funil de Vendas, 1: Retenção, 2: Feed ao Vivo

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkBg,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(44.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(TextMuted.copy(alpha = 0.4f))
            )
        },
        modifier = Modifier
            .fillMaxHeight(0.92f)
            .testTag("analytics_dashboard_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // CABEÇALHO DO DASHBOARD
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🔥", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "FIREBASE ANALYTICS",
                            color = TextLight,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Telemetria, Retenção & Conversão Pro",
                            color = NeonGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("btn_close_analytics")
                ) {
                    Text(text = "✕", color = TextLight, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ABAS DE NAVEGAÇÃO
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkCard)
                    .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AnalyticsTabItem(
                    label = "Funil Pro",
                    icon = "👑",
                    isSelected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    modifier = Modifier.weight(1f)
                )
                AnalyticsTabItem(
                    label = "Retenção",
                    icon = "📈",
                    isSelected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    modifier = Modifier.weight(1f)
                )
                AnalyticsTabItem(
                    label = "Eventos (${events.size})",
                    icon = "⚡",
                    isSelected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    modifier = Modifier.weight(1.2f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // CONTEÚDO DA ABA SELECIONADA
            when (selectedTab) {
                0 -> ConversionFunnelTabContent(funnel = funnel)
                1 -> RetentionTabContent(retention = retention)
                2 -> LiveEventsTabContent(events = events)
            }
        }
    }
}

@Composable
private fun AnalyticsTabItem(
    label: String,
    icon: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(if (isSelected) NeonGreen.copy(alpha = 0.18f) else Color.Transparent)
            .border(if (isSelected) 1.dp else 0.dp, if (isSelected) NeonGreen else Color.Transparent, RoundedCornerShape(9.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = icon, fontSize = 12.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                color = if (isSelected) NeonGreen else TextMuted,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

// =============================================================================
// 1. ABA DO FUNIL DE CONVERSÃO PRO
// =============================================================================
@Composable
private fun ConversionFunnelTabContent(funnel: ConversionFunnelMetrics) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            // Card Principal de Conversão
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GoldAccent.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TAXA DE CONVERSÃO GERAL",
                            color = GoldAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${funnel.conversionRatePercent}%",
                            color = NeonGreen,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (funnel.conversionRatePercent / 100f).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = NeonGreen,
                        trackColor = DarkBorder,
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Receita Faturada Estimada:",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                        Text(
                            text = String.format(Locale.GERMANY, "R$ %.2f", funnel.totalRevenueEstimated),
                            color = TextLight,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "ETAPAS DO FUNIL DE AQUISIÇÃO",
                color = TextLight,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }

        item {
            FunnelStepCard(
                step = 1,
                title = "Impressões do Paywall",
                count = funnel.paywallImpressions,
                tag = "Visualizações",
                color = BlueAccent
            )
        }

        item {
            FunnelStepCard(
                step = 2,
                title = "Planos Selecionados (Mensal/Anual)",
                count = funnel.planSelections,
                tag = "Interesse",
                color = Color(0xFFA855F7)
            )
        }

        item {
            FunnelStepCard(
                step = 3,
                title = "Checkout Iniciado (Google Play / PIX)",
                count = funnel.checkoutInitiations,
                tag = "Intenção",
                color = GoldAccent
            )
        }

        item {
            FunnelStepCard(
                step = 4,
                title = "Testes Grátis de 7 Dias",
                count = funnel.trialStarts,
                tag = "Trial Ativo",
                color = Color(0xFFF97316)
            )
        }

        item {
            FunnelStepCard(
                step = 5,
                title = "Assinaturas Pagas Concluídas",
                count = funnel.completedPurchases,
                tag = "Conversão Final",
                color = NeonGreen
            )
        }

        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "FUNIL DE OFERTAS & CLIQUES DE ACEITAÇÃO",
                color = TextLight,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }

        item {
            val offerViews = FirebaseAnalyticsManager.getOfferViewsCount()
            val offerClicks = FirebaseAnalyticsManager.getOfferAcceptClicksCount()
            val ctr = FirebaseAnalyticsManager.getOfferAcceptanceRate()

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCardElevated),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NeonGreen.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CTR DE ACEITAÇÃO DE OFERTAS",
                            color = NeonGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = String.format(Locale.US, "%.1f%%", ctr),
                            color = NeonGreen,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(DarkBg)
                                .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
                                .padding(10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("VISUALIZAÇÕES", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(3.dp))
                                Text("👀 $offerViews", color = TextLight, fontSize = 15.sp, fontWeight = FontWeight.Black)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(NeonGreen.copy(alpha = 0.12f))
                                .border(1.dp, NeonGreen.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                .padding(10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("CLIQUES ACEITE", color = NeonGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(3.dp))
                                Text("✅ $offerClicks", color = NeonGreen, fontSize = 15.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FunnelStepCard(
    step: Int,
    title: String,
    count: Int,
    tag: String,
    color: Color
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.2f))
                        .border(1.dp, color, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "$step", color = color, fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(text = title, color = TextLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(text = tag, color = TextMuted, fontSize = 10.sp)
                }
            }

            Text(
                text = "$count",
                color = color,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

// =============================================================================
// 2. ABA DE RETENÇÃO E ENGAJAMENTO
// =============================================================================
@Composable
private fun RetentionTabContent(retention: RetentionMetrics) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NeonGreen.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "STREAK DE DIAS ATIVOS",
                                color = NeonGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Engajamento Diário no Trânsito",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                        Text(
                            text = "🔥 ${retention.activeStreakDays} DIAS",
                            color = GoldAccent,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Data da Instalação:", color = TextMuted, fontSize = 11.sp)
                            Text(retention.installDateFormatted, color = TextLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Total de Sessões:", color = TextMuted, fontSize = 11.sp)
                            Text("${retention.totalSessionsCount}", color = TextLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "COORTES DE RETENÇÃO DO ENTREGADOR",
                color = TextLight,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RetentionMilestoneBadge(label = "D1 (24h)", isAchieved = retention.isDay1Retained, modifier = Modifier.weight(1f))
                RetentionMilestoneBadge(label = "D3 (3 dias)", isAchieved = retention.isDay3Retained, modifier = Modifier.weight(1f))
                RetentionMilestoneBadge(label = "D7 (1 sem)", isAchieved = retention.isDay7Retained, modifier = Modifier.weight(1f))
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RetentionMilestoneBadge(label = "D14 (2 sem)", isAchieved = retention.isDay14Retained, modifier = Modifier.weight(1f))
                RetentionMilestoneBadge(label = "D30 (1 mês)", isAchieved = retention.isDay30Retained, modifier = Modifier.weight(1f))
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "USO DE RECURSOS PREMIUM (PRO)",
                color = TextLight,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PremiumFeatureMetricRow(
                        name = "Comandos de Voz Mãos-Livres 🎙️",
                        usedCount = FirebaseAnalyticsManager.premiumFeatureUsageCount["voice_hands_free"] ?: 0,
                        blockedCount = FirebaseAnalyticsManager.premiumFeatureBlockedCount["voice_hands_free"] ?: 0
                    )
                    PremiumFeatureMetricRow(
                        name = "Filtro Anti-Prejuízo Automático 🛡️",
                        usedCount = FirebaseAnalyticsManager.premiumFeatureUsageCount["auto_filter"] ?: 0,
                        blockedCount = FirebaseAnalyticsManager.premiumFeatureBlockedCount["auto_filter"] ?: 0
                    )
                    PremiumFeatureMetricRow(
                        name = "Varreduras de Ofertas Ilimitadas ⚡",
                        usedCount = FirebaseAnalyticsManager.premiumFeatureUsageCount["unlimited_scans"] ?: 0,
                        blockedCount = FirebaseAnalyticsManager.premiumFeatureBlockedCount["unlimited_scans"] ?: 0
                    )
                }
            }
        }
    }
}

@Composable
private fun RetentionMilestoneBadge(
    label: String,
    isAchieved: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isAchieved) NeonGreen.copy(alpha = 0.12f) else DarkCard
        ),
        modifier = modifier.border(
            1.dp,
            if (isAchieved) NeonGreen else DarkBorder,
            RoundedCornerShape(10.dp)
        )
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 10.dp, horizontal = 6.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isAchieved) "✅" else "⏳",
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                color = if (isAchieved) NeonGreen else TextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun PremiumFeatureMetricRow(
    name: String,
    usedCount: Int,
    blockedCount: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = name, color = TextLight, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "Uso: $usedCount", color = NeonGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            if (blockedCount > 0) {
                Text(text = "Bloq: $blockedCount", color = Color(0xFFFF3366), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// =============================================================================
// 3. ABA DE EVENTOS EM TEMPO REAL
// =============================================================================
@Composable
private fun LiveEventsTabContent(events: List<AnalyticsEvent>) {
    if (events.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "📡", fontSize = 36.sp)
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Aguardando eventos...",
                    color = TextLight,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Interaja com o aplicativo para visualizar o fluxo em tempo real.",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(events, key = { it.id }) { event ->
                AnalyticsEventItemCard(event = event)
            }
        }
    }
}

@Composable
private fun AnalyticsEventItemCard(event: AnalyticsEvent) {
    val categoryColor = when (event.category) {
        AnalyticsCategory.CONVERSION -> GoldAccent
        AnalyticsCategory.PREMIUM_USAGE -> NeonGreen
        AnalyticsCategory.RETENTION -> BlueAccent
        AnalyticsCategory.RADAR_DISPATCH -> Color(0xFFA855F7)
        AnalyticsCategory.SAFETY -> Color(0xFFFF3366)
        AnalyticsCategory.NAVIGATION -> Color(0xFFF97316)
    }

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = event.category.emoji, fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = event.eventName,
                        color = categoryColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Text(
                    text = event.formattedTime,
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }

            if (event.parameters.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = event.parameters.entries.joinToString("  •  ") { "${it.key}: ${it.value}" },
                    color = TextMuted,
                    fontSize = 10.sp,
                    lineHeight = 13.sp
                )
            }
        }
    }
}
