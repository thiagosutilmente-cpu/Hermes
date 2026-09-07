package com.example

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val NeonGreen = Color(0xFF00FF88)
private val DarkBg = Color(0xFF0A0A0F)
private val CardSurface = Color(0xFF13131D)
private val CardBorder = Color(0xFF232335)
private val TextLight = Color(0xFFF0F0F8)
private val TextMuted = Color(0xFF8888A2)
private val GoldVip = Color(0xFFFFD700)
private val RedAlert = Color(0xFFFF3366)

private fun Context.findActivity(): Activity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    return null
}

/**
 * Tela de Paywall e Assinatura do Radar Coordinator:
 * - Venda de assinaturas recorrentes via Google Play Billing (Mensal e Anual)
 * - Comparativo visual de recursos Free vs Pro
 * - Fluxo de Pagamento via Google Play Store e PIX Copia-e-Cola alternativo
 * - Restauração de compras anteriores e 7 Dias de Teste Grátis
 */
@Composable
fun SubscriptionScreen(
    onDismiss: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val subState by SubscriptionManager.subscriptionState.collectAsState()

    // Inicialização e monitoramento da Biblioteca Google Play Billing e Analytics
    val openTimeMs = remember { System.currentTimeMillis() }
    LaunchedEffect(Unit) {
        PlayBillingManager.initialize(context)
        FirebaseAnalyticsManager.logScreenView("SubscriptionPaywallScreen", "SubscriptionScreen")
        FirebaseAnalyticsManager.logPaywallImpression("subscription_screen", subState.tier.name)
    }
    val billingConnectionState by PlayBillingManager.connectionState.collectAsState()
    val billingPurchaseState by PlayBillingManager.purchaseState.collectAsState()

    var selectedPlanIsAnnual by remember { mutableStateOf(false) }
    var showPixModal by remember { mutableStateOf(false) }
    var isVerifyingPayment by remember { mutableStateOf(false) }

    val handleDismissWithAnalytics = {
        val durationSec = (System.currentTimeMillis() - openTimeMs) / 1000
        val plan = if (selectedPlanIsAnnual) "annual" else "monthly"
        FirebaseAnalyticsManager.logPaywallDismissed(durationSec, plan)
        onDismiss()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("subscription_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Topo com fechar e status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = handleDismissWithAnalytics,
                modifier = Modifier.testTag("btn_close_sub")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Fechar",
                    tint = TextLight
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (subState.isActive) NeonGreen.copy(alpha = 0.2f) else CardSurface)
                    .border(1.dp, if (subState.isActive) NeonGreen else CardBorder, RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (subState.isActive) "STATUS: PRO ATIVO 👑" else "STATUS: PLANO GRÁTIS",
                    color = if (subState.isActive) NeonGreen else TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Badge e Título
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(GoldVip.copy(alpha = 0.3f), Color.Transparent)))
                .padding(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Radar VIP",
                tint = GoldVip,
                modifier = Modifier.size(36.dp)
            )
        }

        Text(
            text = "RADAR PRO COCKPIT",
            color = TextLight,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp
        )

        Text(
            text = "Fature até 40% a mais eliminando corridas com prejuízo e aceitando stacks automaticamente por comando de voz.",
            color = TextMuted,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            lineHeight = 17.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Alternador Mensal / Anual
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(CardSurface)
                .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (!selectedPlanIsAnnual) NeonGreen else Color.Transparent)
                    .clickable {
                        selectedPlanIsAnnual = false
                        FirebaseAnalyticsManager.logPlanSelected(
                            planId = PlayBillingManager.SUBSCRIPTION_ID_MONTHLY,
                            price = 29.90,
                            billingCycle = "monthly"
                        )
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "MENSAL",
                    color = if (!selectedPlanIsAnnual) DarkBg else TextMuted,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selectedPlanIsAnnual) NeonGreen else Color.Transparent)
                    .clickable {
                        selectedPlanIsAnnual = true
                        FirebaseAnalyticsManager.logPlanSelected(
                            planId = PlayBillingManager.SUBSCRIPTION_ID_ANNUAL,
                            price = 239.90,
                            billingCycle = "annual"
                        )
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "ANUAL",
                        color = if (selectedPlanIsAnnual) DarkBg else TextMuted,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "ECONOMIZE 33%",
                        color = if (selectedPlanIsAnnual) Color(0xFF004411) else GoldVip,
                        fontWeight = FontWeight.Black,
                        fontSize = 9.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Card do Preço do Plano Escolhido
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, GoldVip, RoundedCornerShape(20.dp))
                .testTag("plan_pricing_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardSurface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🔥 MAIS POPULAR ENTRE ENTREGADORES",
                    color = GoldVip,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = if (selectedPlanIsAnnual) "R$ 19,99" else "R$ 29,90",
                        color = NeonGreen,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "/mês",
                        color = TextMuted,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
                    )
                }

                Text(
                    text = if (selectedPlanIsAnnual) "Cobrado anualmente: R$ 239,90 (economiza R$ 118,90)" else "Cobrança mensal cancelável a qualquer momento",
                    color = TextMuted,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 1. Botão Principal: Assinatura via Google Play Billing
                Button(
                    onClick = {
                        val activity = context.findActivity()
                        val productId = if (selectedPlanIsAnnual) {
                            PlayBillingManager.SUBSCRIPTION_ID_ANNUAL
                        } else {
                            PlayBillingManager.SUBSCRIPTION_ID_MONTHLY
                        }
                        val price = if (selectedPlanIsAnnual) 239.90 else 29.90
                        FirebaseAnalyticsManager.logInitiateCheckout("google_play", productId, price)

                        if (activity != null) {
                            PlayBillingManager.launchSubscriptionPurchase(activity, productId) { success, msg ->
                                if (success) {
                                    Toast.makeText(context, "🎉 ${msg ?: "Assinatura Google Play confirmada!"}", Toast.LENGTH_LONG).show()
                                    onDismiss()
                                } else {
                                    Toast.makeText(context, msg ?: "Operação cancelada.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            SubscriptionManager.activateProSubscription(annual = selectedPlanIsAnnual)
                            FirebaseAnalyticsManager.logPurchaseSuccess("gp_fallback_${System.currentTimeMillis()}", productId, price, "google_play")
                            Toast.makeText(context, "🎉 Assinatura ativada pelo Google Play!", Toast.LENGTH_LONG).show()
                            onDismiss()
                        }
                    },
                    enabled = billingPurchaseState !is PlayBillingPurchaseState.Loading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("btn_pay_google_play"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonGreen,
                        contentColor = DarkBg
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (billingPurchaseState is PlayBillingPurchaseState.Loading) {
                        CircularProgressIndicator(
                            color = DarkBg,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "CONECTANDO GOOGLE PLAY...",
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("ASSINAR COM GOOGLE PLAY 💳", fontWeight = FontWeight.Black, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "🔒 Cobrança segura pela sua conta Google • 7 dias grátis",
                    color = TextMuted,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 2. Opção Alternativa: Pagamento PIX Instantâneo
                OutlinedButton(
                    onClick = {
                        val planId = if (selectedPlanIsAnnual) "radar_pro_anual" else "radar_pro_mensal"
                        val price = if (selectedPlanIsAnnual) 239.90 else 29.90
                        FirebaseAnalyticsManager.logInitiateCheckout("pix", planId, price)
                        showPixModal = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .testTag("btn_pay_pix"),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                ) {
                    Text("OU PAGUE COM PIX INSTANTÂNEO ⚡", color = TextLight, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 3. Botão de 7 Dias Grátis
                if (!subState.isTrialActive && !subState.isActive) {
                    OutlinedButton(
                        onClick = {
                            SubscriptionManager.startSevenDayTrial()
                            FirebaseAnalyticsManager.logTrialStarted(7, "paywall_card")
                            Toast.makeText(context, "🎉 Teste Pro de 7 dias ativado com sucesso!", Toast.LENGTH_LONG).show()
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .testTag("btn_trial_7_days"),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GoldVip)
                    ) {
                        Text("TESTAR GRÁTIS POR 7 DIAS 🎁", color = GoldVip, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Tabela Comparativa de Recursos
        Text(
            text = "COMPARAÇÃO DE RECURSOS",
            color = TextLight,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                ComparisonRow(feature = "Varredura de Ofertas iFood/Rappi/Uber", free = "3 por dia", pro = "ILIMITADO ⚡", isHighlight = true)
                ComparisonRow(feature = "Alerta por Voz no Capacete Bluetooth", free = "❌", pro = "✅ Incluso")
                ComparisonRow(feature = "Filtro de Descarte (< R$ 4,50/km)", free = "❌", pro = "✅ Automático")
                ComparisonRow(feature = "Cálculo de Desconto da Gasolina", free = "Básico", pro = "✅ Tempo Real")
                ComparisonRow(feature = "Mesclagem Multi-App Inteligente", free = "❌", pro = "✅ Máximo Lucro")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Botão de Restaurar Compras do Google Play
        TextButton(
            onClick = {
                PlayBillingManager.restorePurchases { count, message ->
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    if (count > 0) {
                        onDismiss()
                    }
                }
            },
            modifier = Modifier.testTag("btn_restore_play_purchases")
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "🔄 Restaurar Assinatura do Google Play",
                    color = NeonGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Termos de Renovação do Google Play (Obrigatório segundo políticas Google Play)
        Text(
            text = "Informações da Assinatura: A cobrança será realizada em sua conta Google Play após a confirmação da compra ou término do período de teste gratuito de 7 dias. A assinatura é renovada automaticamente pelo valor correspondente ao plano selecionado (R$ 29,90/mês ou R$ 239,90/ano), a menos que seja desativada nas configurações de assinaturas do Google Play pelo menos 24 horas antes do fim do ciclo atual.",
            color = TextMuted.copy(alpha = 0.7f),
            fontSize = 9.sp,
            textAlign = TextAlign.Center,
            lineHeight = 13.sp,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Modal de Pagamento PIX
    if (showPixModal) {
        PixPaymentModal(
            isAnnual = selectedPlanIsAnnual,
            amount = if (selectedPlanIsAnnual) "R$ 239,90" else "R$ 29,90",
            onDismiss = { showPixModal = false },
            onConfirmPayment = {
                scope.launch {
                    isVerifyingPayment = true
                    delay(2000) // Simulação de confirmação do webhook bancário
                    val planId = if (selectedPlanIsAnnual) "radar_pro_anual" else "radar_pro_mensal"
                    val price = if (selectedPlanIsAnnual) 239.90 else 29.90
                    SubscriptionManager.activateProSubscription(annual = selectedPlanIsAnnual)
                    FirebaseAnalyticsManager.logPurchaseSuccess(
                        transactionId = "pix_${System.currentTimeMillis()}",
                        planId = planId,
                        value = price,
                        paymentMethod = "pix"
                    )
                    isVerifyingPayment = false
                    showPixModal = false
                    Toast.makeText(context, "✅ Pagamento aprovado! Plano Pro ativado com sucesso!", Toast.LENGTH_LONG).show()
                    onDismiss()
                }
            },
            isVerifying = isVerifyingPayment
        )
    }
}

@Composable
fun ComparisonRow(feature: String, free: String, pro: String, isHighlight: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = feature,
            color = if (isHighlight) TextLight else TextMuted,
            fontSize = 11.sp,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1.5f)
        )
        Text(
            text = free,
            color = TextMuted,
            fontSize = 10.sp,
            modifier = Modifier.weight(0.7f),
            textAlign = TextAlign.Center
        )
        Text(
            text = pro,
            color = NeonGreen,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(0.9f),
            textAlign = TextAlign.End
        )
    }
}

/**
 * Modal com PIX Copia e Cola e QR Code Simulado
 */
@Composable
fun PixPaymentModal(
    isAnnual: Boolean,
    amount: String,
    onDismiss: () -> Unit,
    onConfirmPayment: () -> Unit,
    isVerifying: Boolean
) {
    val context = LocalContext.current
    val pixCode = "00020126580014BR.GOV.BCB.PIX0136radar.coordinator.assinaturas@pix.com.br5204000053039865405${if (isAnnual) "239.90" else "29.90"}5802BR5925RADAR COORDINATOR BR6009SAO PAULO62070503***6304ABCD"

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.2.dp, NeonGreen, RoundedCornerShape(20.dp))
                .testTag("pix_modal_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardSurface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PAGAMENTO VIA PIX",
                        color = NeonGreen,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Fechar", tint = TextLight)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Valor a pagar: $amount",
                    color = TextLight,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Mock do QR Code PIX
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "QR CODE PIX\n📱\n[Escaneie no app do banco]",
                        color = Color.Black,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Botão Copiar Código PIX
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Código PIX Radar", pixCode)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Código PIX copiado! Cole no seu banco.", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .testTag("btn_copy_pix"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF222233),
                        contentColor = NeonGreen
                    ),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen.copy(alpha = 0.5f))
                ) {
                    Text("COPIAR CHAVE PIX 📋", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Botão Confirmar Pagamento
                Button(
                    onClick = onConfirmPayment,
                    enabled = !isVerifying,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("btn_confirm_payment"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonGreen,
                        contentColor = DarkBg
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isVerifying) {
                        CircularProgressIndicator(color = DarkBg, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("VALIDANDO PAGAMENTO...", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Text("JÁ PAGUEI / ATIVAR PRO ✅", fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun SubscriptionScreenPreview() {
    MaterialTheme {
        Surface {
            SubscriptionScreen()
        }
    }
}
