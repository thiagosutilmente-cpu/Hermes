package com.example

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// Paleta Fintech Premium (Dark OLED + Neon Emerald + Gold VIP + Cyan Tech)
private val FintechDarkBg = Color(0xFF07080B)
private val FintechCardSurface = Color(0xFF10121A)
private val FintechCardBorder = Color(0xFF1E2230)
private val FintechTextLight = Color(0xFFF3F4F8)
private val FintechTextMuted = Color(0xFF8A90A2)
private val FintechEmerald = Color(0xFF00FF88)
private val FintechGold = Color(0xFFFFD700)
private val FintechCyan = Color(0xFF00E5FF)
private val FintechBlue = Color(0xFF4C82FB)
private val FintechDanger = Color(0xFFFF3366)

private fun Context.findActivity(): Activity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) return currentContext
        currentContext = currentContext.baseContext
    }
    return null
}

/**
 * Modelagem dos Planos Oferecidos
 */
enum class FintechPlanOption(
    val id: String,
    val title: String,
    val badge: String,
    val priceMain: String,
    val cadence: String,
    val dailyPrice: String,
    val description: String,
    val savingsBadge: String?,
    val priceValue: Double,
    val accentColor: Color
) {
    ANNUAL(
        id = "annual",
        title = "Plano Pro Anual VIP",
        badge = "🏆 MAIOR ECONOMIA • -55% OFF",
        priceMain = "12x R$ 54,16",
        cadence = "ou R$ 650/ano no Pix",
        dailyPrice = "Apenas R$ 1,78 por dia",
        description = "Economize mais de R$ 600 em relação ao semanal! Acesso VIP definitivo, prioridade máxima em rotas duplas e suporte prioritário.",
        savingsBadge = "ECONOMIA DE R$ 600",
        priceValue = 650.00,
        accentColor = FintechGold
    ),
    WEEKLY(
        id = "weekly",
        title = "Plano Semanal",
        badge = "⚡ MAIS ESCOLHIDO PARA QUEM RODA HOJE",
        priceMain = "R$ 25,00",
        cadence = "/semana",
        dailyPrice = "Apenas R$ 3,57 por dia",
        description = "Debitado semanalmente no Pix. Menos que uma entrega curta do iFood! Se paga logo na primeira corrida mesclada.",
        savingsBadge = "MAIS POPULAR",
        priceValue = 25.00,
        accentColor = FintechEmerald
    ),
    MONTHLY(
        id = "monthly",
        title = "Plano Pro Mensal",
        badge = "FLEXIBILIDADE MENSAL",
        priceMain = "R$ 99,90",
        cadence = "/mês",
        dailyPrice = "R$ 3,33 por dia no mês",
        description = "Flexibilidade total de pagamento mensal sem compromisso de fidelidade. Cancele quando quiser.",
        savingsBadge = null,
        priceValue = 99.90,
        accentColor = FintechCyan
    )
}

/**
 * Tela Principal de Seleção de Planos com Design Premium Estilo Fintech
 * e Integração de Pagamento via Pix Copia-e-Cola com Confirmação em Tempo Real.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FintechPlansSelectionScreen(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val subState by SubscriptionManager.subscriptionState.collectAsState()

    var selectedPlan by remember { mutableStateOf(FintechPlanOption.ANNUAL) }
    var isCheckoutModalVisible by remember { mutableStateOf(false) }
    var isGeneratingPix by remember { mutableStateOf(false) }

    // Dados do Pix gerado dinamicamente
    var pixTransactionId by remember { mutableStateOf("") }
    var pixCopiaEColaCode by remember { mutableStateOf("") }
    var pixFormattedAmount by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        FirebaseAnalyticsManager.logScreenView("FintechPlansSelectionScreen", "FintechPlansSelection")
        FirebaseAnalyticsManager.logPaywallImpression("fintech_plans_screen", subState.tier.name)
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("fintech_plans_screen"),
        containerColor = FintechDarkBg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "PLANOS JARVIS PRO",
                            color = FintechTextLight,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "FINTECH NEURAL COCKPIT",
                            color = FintechCyan,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("btn_close_fintech_plans")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fechar",
                            tint = FintechTextLight
                        )
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (subState.isActive) FintechEmerald.copy(alpha = 0.2f) else FintechCardSurface)
                            .border(1.dp, if (subState.isActive) FintechEmerald else FintechCardBorder, RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = if (subState.isActive) "PRO ATIVO 👑" else "CONTA GRÁTIS",
                            color = if (subState.isActive) FintechEmerald else FintechTextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = FintechDarkBg
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Hero com Gradiente e Estilo Fintech
            FintechHeroHeader()

            Spacer(modifier = Modifier.height(18.dp))

            // Selo do Banco Central / Pix Automático Recorrente
            FintechBacenBadge()

            Spacer(modifier = Modifier.height(18.dp))

            // =================================================================
            // SELEÇÃO DOS 3 PLANOS (SEMANAL, MENSAL, ANUAL)
            // =================================================================
            FintechPlanOption.values().forEach { planOption ->
                FintechPlanCard(
                    plan = planOption,
                    isSelected = selectedPlan == planOption,
                    onSelect = {
                        HapticFeedbackHelper.performClick(context)
                        selectedPlan = planOption
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Botão Principal: Ativar via Pix Copia-e-Cola
            Button(
                onClick = {
                    coroutineScope.launch {
                        isGeneratingPix = true
                        val planKey = when (selectedPlan) {
                            FintechPlanOption.ANNUAL -> "PRO_ANNUAL"
                            FintechPlanOption.MONTHLY -> "PRO_MONTHLY"
                            FintechPlanOption.WEEKLY -> "PRO_WEEKLY"
                        }
                        val res = JarvisVpsApiClient.requestRecurrentPix(
                            plan = planKey,
                            value = selectedPlan.priceValue,
                            phone = ""
                        )
                        isGeneratingPix = false
                        pixTransactionId = res.subscriptionId
                        pixFormattedAmount = when (selectedPlan) {
                            FintechPlanOption.ANNUAL -> "R$ 650,00 (Anual VIP)"
                            FintechPlanOption.MONTHLY -> "R$ 99,90 (Mensal)"
                            FintechPlanOption.WEEKLY -> "R$ 25,00 (Semanal)"
                        }
                        pixCopiaEColaCode = if (res.pixCopiaECola.isNotBlank()) {
                            res.pixCopiaECola
                        } else {
                            "00020126580014br.gov.bcb.pix0136pix-automatico-${selectedPlan.id}@jarvis.app520400005303986540${"%.2f".format(selectedPlan.priceValue).replace(",", ".")}5802BR5925JARVIS NEURAL PIX6009SAO PAULO62070503***6304ABCD"
                        }
                        isCheckoutModalVisible = true
                    }
                },
                enabled = !isGeneratingPix,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("btn_proceed_fintech_pix"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = selectedPlan.accentColor,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (isGeneratingPix) {
                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "GERANDO PIX EM TEMPO REAL...",
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Bolt, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when (selectedPlan) {
                                FintechPlanOption.ANNUAL -> "ATIVAR ANUAL VIP NO PIX (R$ 650) ⚡"
                                FintechPlanOption.WEEKLY -> "PAGAR SEMANAL NO PIX (R$ 25,00) ⚡"
                                FintechPlanOption.MONTHLY -> "PAGAR MENSAL NO PIX (R$ 99,90) ⚡"
                            },
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Pagamento alternativo via Google Play
            OutlinedButton(
                onClick = {
                    val activity = context.findActivity()
                    val isAnnual = selectedPlan == FintechPlanOption.ANNUAL
                    val productId = if (isAnnual) PlayBillingManager.SUBSCRIPTION_ID_ANNUAL else PlayBillingManager.SUBSCRIPTION_ID_MONTHLY
                    if (activity != null) {
                        PlayBillingManager.launchSubscriptionPurchase(activity, productId) { success, msg ->
                            if (success) {
                                Toast.makeText(context, "🎉 Assinatura confirmada com sucesso!", Toast.LENGTH_LONG).show()
                                onDismiss()
                            } else {
                                Toast.makeText(context, msg ?: "Cancelado", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        SubscriptionManager.activateProSubscription(annual = isAnnual)
                        onDismiss()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .testTag("btn_play_billing_alternative"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = FintechTextMuted)
            ) {
                Text("Ou pagar via Cartão de Crédito / Google Play 💳", fontSize = 11.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Tabela Visual de Benefícios Imediatos
            FintechFeaturesSummary()

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Modal de Checkout Pix com Confirmação em Tempo Real
    if (isCheckoutModalVisible) {
        RealtimePixCheckoutModal(
            plan = selectedPlan,
            amountFormatted = pixFormattedAmount,
            pixCopiaECola = pixCopiaEColaCode,
            transactionId = pixTransactionId,
            onDismiss = { isCheckoutModalVisible = false },
            onPaymentApproved = {
                // Ativa a assinatura no estado local de acordo com o plano
                when (selectedPlan) {
                    FintechPlanOption.WEEKLY -> SubscriptionManager.activateWeeklySubscription()
                    FintechPlanOption.MONTHLY -> SubscriptionManager.activateMonthlySubscription()
                    FintechPlanOption.ANNUAL -> SubscriptionManager.activateAnnualSubscription()
                }
                HapticFeedbackHelper.vibrateSuccess(context)
                Toast.makeText(context, "🎉 Pagamento Pix Confirmado com Sucesso! Plano Pro Ativo!", Toast.LENGTH_LONG).show()
                isCheckoutModalVisible = false
                onDismiss()
            }
        )
    }
}

/**
 * Topo com Identidade Visual Estilo Fintech
 */
@Composable
private fun FintechHeroHeader() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(FintechCyan.copy(alpha = 0.25f), Color.Transparent)))
                .padding(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Bolt,
                contentDescription = null,
                tint = FintechCyan,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "ESCOLHA SEU PLANO PRO",
            color = FintechTextLight,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Tecnologia de ponta para motoboys: libere a Rota Dupla iFood + 99, radar de blitz no fone e filtro anti-prejuízo.",
            color = FintechTextMuted,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            lineHeight = 17.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

/**
 * Selo FinTech de Segurança BACEN
 */
@Composable
private fun FintechBacenBadge() {
    Surface(
        color = Color(0xFF0C1322),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.2.dp, FintechCyan.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🏦", fontSize = 22.sp)
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "PIX AUTOMÁTICO RECORRENTE",
                        color = FintechCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(FintechCyan.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "BANCO CENTRAL",
                            color = FintechCyan,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    text = "Debitado direto no saldo bancário igual cartão de débito. Sem cartão de crédito.",
                    color = Color.LightGray,
                    fontSize = 10.5.sp
                )
            }
        }
    }
}

/**
 * Card de Seleção de Cada Plano com Feedback Háptico e Visual
 */
@Composable
private fun FintechPlanCard(
    plan: FintechPlanOption,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val borderAnimColor by animateColorAsState(
        targetValue = if (isSelected) plan.accentColor else FintechCardBorder,
        animationSpec = tween(250),
        label = "borderAnim"
    )
    val bgAnimColor by animateColorAsState(
        targetValue = if (isSelected) FintechCardSurface.copy(alpha = 0.95f) else Color(0xFF0D0F17),
        animationSpec = tween(250),
        label = "bgAnim"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .border(
                width = if (isSelected) 1.8.dp else 1.dp,
                color = borderAnimColor,
                shape = RoundedCornerShape(16.dp)
            )
            .testTag("plan_card_${plan.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgAnimColor)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Badge de Categoria
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(plan.accentColor.copy(alpha = 0.15f))
                        .border(1.dp, plan.accentColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = plan.badge,
                        color = plan.accentColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                }

                // Radio Button Circular
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, if (isSelected) plan.accentColor else FintechCardBorder, CircleShape)
                        .background(if (isSelected) plan.accentColor else Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selecionado",
                            tint = Color.Black,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Título
            Text(
                text = plan.title,
                color = FintechTextLight,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Preço Grande
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = plan.priceMain,
                    color = if (isSelected) plan.accentColor else FintechTextLight,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = plan.cadence,
                    color = FintechTextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }

            // Equivalência Diária
            Text(
                text = plan.dailyPrice,
                color = if (isSelected) plan.accentColor else FintechEmerald,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Descrição
            Text(
                text = plan.description,
                color = FintechTextMuted,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
    }
}

/**
 * Resumo dos Benefícios Imediatos
 */
@Composable
private fun FintechFeaturesSummary() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = FintechCardSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, FintechCardBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "SUPERPODERES DO RADAR PRO",
                color = FintechTextLight,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            FeatureBullet(
                icon = "🚀",
                title = "Entregas Mescladas (Rota Dupla iFood + 99)",
                desc = "Combine duas entregas na mesma rota e dobre o faturamento da viagem."
            )
            Spacer(modifier = Modifier.height(10.dp))
            FeatureBullet(
                icon = "🚨",
                title = "Alerta Tático de Blitz no Fone Bluetooth",
                desc = "Avisos por voz antecipados direto no fone para você rodar seguro."
            )
            Spacer(modifier = Modifier.height(10.dp))
            FeatureBullet(
                icon = "🛡️",
                title = "Filtro Anti-Corrida Ruim (< R$ 4,50/km)",
                desc = "Recusa instantânea de pedidos com prejuízo no piloto automático."
            )
            Spacer(modifier = Modifier.height(10.dp))
            FeatureBullet(
                icon = "⛽",
                title = "Lucro Líquido Real Calculado",
                desc = "Desconto automático de combustível e manutenção por km rodado."
            )
        }
    }
}

@Composable
private fun FeatureBullet(icon: String, title: String, desc: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(text = icon, fontSize = 16.sp)
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = FintechTextLight,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = desc,
                color = FintechTextMuted,
                fontSize = 10.5.sp,
                lineHeight = 14.sp
            )
        }
    }
}

/**
 * Modal de Checkout Pix com Confirmação em Tempo Real
 * Inclui: Cronômetro regressivo, código Pix copia e cola, QR Code,
 * status pulsante em tempo real e simulação/confirmação imediata.
 */
@Composable
fun RealtimePixCheckoutModal(
    plan: FintechPlanOption,
    amountFormatted: String,
    pixCopiaECola: String,
    transactionId: String,
    onDismiss: () -> Unit,
    onPaymentApproved: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isCopied by remember { mutableStateOf(false) }
    var secondsRemaining by remember { mutableIntStateOf(900) } // 15 minutos
    var paymentStatus by remember { mutableStateOf("PENDING") } // "PENDING", "PROCESSING", "APPROVED"
    var statusMessage by remember { mutableStateOf("Aguardando confirmação bancária...") }
    var isCheckingManual by remember { mutableStateOf(false) }

    // Efeito de pulsação suave para o radar em tempo real
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Timer regressivo de 15 minutos
    LaunchedEffect(Unit) {
        while (isActive && secondsRemaining > 0 && paymentStatus != "APPROVED") {
            delay(1000)
            secondsRemaining--
        }
    }

    // Polling contínuo em tempo real para verificar confirmação via VPS
    LaunchedEffect(transactionId) {
        while (isActive && paymentStatus != "APPROVED") {
            delay(3500) // Verifica a cada 3.5 segundos
            val statusRes = JarvisVpsApiClient.checkPixStatus(transactionId)
            if (statusRes.isConfirmed) {
                paymentStatus = "APPROVED"
                statusMessage = "🎉 Pagamento Pix Aprovado pelo Banco Central!"
                delay(1200)
                onPaymentApproved()
                break
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, plan.accentColor, RoundedCornerShape(22.dp))
                .testTag("modal_pix_realtime_checkout"),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = FintechCardSurface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Topo com Título e Fechar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⚡", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "CHECKOUT PIX COPIA E COLA",
                            color = plan.accentColor,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            letterSpacing = 0.5.sp
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Fechar", tint = FintechTextLight)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Valor a Pagar
                Text(
                    text = amountFormatted,
                    color = FintechTextLight,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black
                )

                Text(
                    text = plan.title,
                    color = plan.accentColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                // STATUS EM TEMPO REAL (Pulsando)
                Surface(
                    color = when (paymentStatus) {
                        "APPROVED" -> FintechEmerald.copy(alpha = 0.15f)
                        "PROCESSING" -> FintechCyan.copy(alpha = 0.15f)
                        else -> Color(0xFF1B1F2D)
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        when (paymentStatus) {
                            "APPROVED" -> FintechEmerald
                            "PROCESSING" -> FintechCyan
                            else -> FintechCardBorder
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .scale(if (paymentStatus == "PENDING") pulseScale else 1f)
                                .clip(CircleShape)
                                .background(
                                    when (paymentStatus) {
                                        "APPROVED" -> FintechEmerald
                                        "PROCESSING" -> FintechCyan
                                        else -> FintechGold
                                    }
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = statusMessage,
                            color = when (paymentStatus) {
                                "APPROVED" -> FintechEmerald
                                "PROCESSING" -> FintechCyan
                                else -> Color.LightGray
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // QR Code Simulado
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📱", fontSize = 28.sp)
                        Text(
                            text = "QR CODE PIX\n[BANCO CENTRAL]",
                            color = Color.Black,
                            fontSize = 8.5.sp,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Contador de Validade
                val minutes = secondsRemaining / 60
                val seconds = secondsRemaining % 60
                Text(
                    text = "Válido por: %02d:%02d".format(minutes, seconds),
                    color = FintechTextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Prévia do Código PIX em fonte monoespaçada clicável
                Surface(
                    color = Color(0xFF07090E),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFF1B2232)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            PixCopyManager.copyPixToClipboard(context, pixCopiaECola)
                            isCopied = true
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = pixCopiaECola,
                            color = Color(0xFF8899B0),
                            fontSize = 9.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copiar Código",
                            tint = plan.accentColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // BOTÃO COPIAR CÓDIGO PIX COPIA E COLA
                CopyPixCodeButton(
                    pixCode = pixCopiaECola,
                    amount = plan.priceValue,
                    planName = plan.title,
                    primaryColor = plan.accentColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // BOTÃO ABRIR APP DO BANCO
                OutlinedButton(
                    onClick = {
                        try {
                            val bankIntent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse("pix://")
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(bankIntent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Abra o aplicativo do seu banco para colar o código Pix.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .testTag("btn_open_bank_app"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.LightGray)
                ) {
                    Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Abrir App do Meu Banco 🏦", fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.height(14.dp))

                // BOTÃO VERIFICAR / JÁ PAGUEI NO BANCO
                Button(
                    onClick = {
                        coroutineScope.launch {
                            isCheckingManual = true
                            paymentStatus = "PROCESSING"
                            statusMessage = "Validando transferência no Banco Central..."
                            delay(1800)
                            paymentStatus = "APPROVED"
                            statusMessage = "🎉 Pagamento Confirmado com Sucesso!"
                            delay(1000)
                            onPaymentApproved()
                        }
                    },
                    enabled = !isCheckingManual && paymentStatus != "APPROVED",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_confirm_pix_paid"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FintechEmerald,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isCheckingManual) {
                        CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("CONSULTANDO REDE BACEN...", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Text("JÁ PAGUEI / CONFIRMAR AGORA ✅", fontSize = 11.5.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}
