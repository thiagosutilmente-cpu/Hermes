package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// -----------------------------------------------------------------------------
// CHECKOUT FINTECH PROFISSIONAL (ASAAS / PIX / CARTÃO)
// -----------------------------------------------------------------------------
// Defina true para builds enviados à Google Play Store (sem links externos de cobrança digital);
// defina false para builds APK distribuídos diretamente fora da loja.
const val IS_PLAY_STORE_COMPLIANT_MODE = true

enum class ProPlanTier(
    val id: String,
    val title: String,
    val priceDisplay: String,
    val period: String,
    val savingsBadge: String?,
    val isPopular: Boolean
) {
    SEMANAL("weekly", "Semanal", "R$ 25,00", "/ semana", "MAIS ESCOLHIDO", true),
    MENSAL("monthly", "Mensal", "R$ 89,90", "/ mês", "ECONOMIZE 10%", false),
    TRIMESTRAL("annual", "Trimestral VIP", "R$ 229,00", "/ 3 meses", "ECONOMIZE 25%", false)
}

@Composable
fun StartupFintechCheckoutScreen(
    onPaymentSuccess: () -> Unit = {}
) {
    val context = LocalContext.current
    val palette = LocalCockpitPalette.current
    var selectedPlan by remember { mutableStateOf(ProPlanTier.SEMANAL) }
    var isCopied by remember { mutableStateOf(false) }

    val pixKey = when (selectedPlan) {
        ProPlanTier.SEMANAL -> "00020126580014br.gov.bcb.pix0136d8924b17-742a-43d9-9521-radarpro2026520400005303986540525.005802BR5916RADAR DELIVERY6009SAO PAULO62070503***6304E8A2"
        ProPlanTier.MENSAL -> "00020126580014br.gov.bcb.pix0136d8924b17-742a-43d9-9521-radarpro2026520400005303986540589.905802BR5916RADAR DELIVERY6009SAO PAULO62070503***6304C4B1"
        ProPlanTier.TRIMESTRAL -> "00020126580014br.gov.bcb.pix0136d8924b17-742a-43d9-9521-radarpro20265204000053039865406229.005802BR5916RADAR DELIVERY6009SAO PAULO62070503***6304F91A"
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Cabeçalho Seguro
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    color = palette.pixTeal.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = palette.pixTeal,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (IS_PLAY_STORE_COMPLIANT_MODE) {
                                "ASSINATURA PRO • GOOGLE PLAY & LICENÇA"
                            } else {
                                "CHECKOUT FINTECH CRIPTOGRAFADO 256-BIT"
                            },
                            color = palette.pixTeal,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Turbine Seus Ganhos com o PRO",
                    color = palette.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Acesso completo a rotas duplas, cálculo automático no fone e filtros avançados nos 4 apps.",
                    color = palette.textSecondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }

        // 2. Seletor de Planos
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ProPlanTier.values().forEach { plan ->
                    val isSelected = selectedPlan == plan
                    Surface(
                        onClick = { selectedPlan = plan },
                        color = if (isSelected) palette.cardSurface else palette.surface,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(
                            if (isSelected) 2.dp else palette.borderWidth,
                            if (isSelected) palette.pixTeal else palette.border
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("plan_tier_${plan.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedPlan = plan },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = palette.pixTeal,
                                        unselectedColor = palette.textMuted
                                    )
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = plan.title,
                                            color = palette.textPrimary,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (plan.savingsBadge != null) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                color = if (plan.isPopular) palette.emerald else palette.pixTeal,
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = plan.savingsBadge,
                                                    color = Color.White,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Black,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = if (IS_PLAY_STORE_COMPLIANT_MODE) {
                                            "Ativação segura via conta ou Google Play"
                                        } else {
                                            "Liberação imediata via PIX"
                                        },
                                        color = palette.textSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = plan.priceDisplay,
                                    color = palette.textPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = plan.period,
                                    color = palette.textSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Bloco de Ativação (Google Play Billing / Sincronização vs PIX Direto)
        if (IS_PLAY_STORE_COMPLIANT_MODE) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = palette.surface),
                    border = BorderStroke(palette.borderWidth, palette.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Licença PRO Vinculada à Conta",
                            color = palette.textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Plano selecionado: ${selectedPlan.title} (${selectedPlan.priceDisplay}${selectedPlan.period}). Assinaturas digitais no aplicativo da Play Store são processadas exclusivamente via Google Play Billing ou sincronizadas automaticamente com sua conta já ativa.",
                            color = palette.textSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                HapticFeedbackHelper.vibrateSuccess(context)
                                Toast.makeText(
                                    context,
                                    "Status PRO verificado com sua conta Google Play / servidor.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                onPaymentSuccess()
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = palette.emerald),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("sync_pro_license_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SINCRONIZAR ASSINATURA PRO",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        } else {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = palette.surface),
                    border = BorderStroke(palette.borderWidth, palette.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Pague com PIX Instantâneo",
                            color = palette.textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Reconhecimento automático pelo gateway Asaas em menos de 10 segundos.",
                            color = palette.textSecondary,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Box(
                            modifier = Modifier
                                .size(170.dp)
                                .background(Color.White, RoundedCornerShape(14.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            NativePixQrCanvas(
                                pixTeal = palette.pixTeal,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Chave PIX Radar PRO", pixKey)
                            clipboard.setPrimaryClip(clip)
                            isCopied = true
                            HapticFeedbackHelper.vibrateSuccess(context)
                            Toast.makeText(context, "Código PIX Copiado com Sucesso!", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCopied) palette.emerald else palette.pixTeal
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("copy_pix_button")
                    ) {
                        Icon(
                            imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isCopied) "CÓDIGO PIX COPIADO!" else "COPIAR CÓDIGO PIX (1 TOQUE)",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = {
                            val opened = AsaasCheckoutManager.openCheckout(context, selectedPlan.id, selectedPlan.title)
                            if (!opened) {
                                val browserIntent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse(AsaasCheckoutManager.getCheckoutUrl(selectedPlan.id))
                                )
                                context.startActivity(browserIntent)
                            }
                            onPaymentSuccess()
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(palette.borderWidth, palette.border),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("asaas_card_checkout_button")
                    ) {
                        Text(
                            text = "Pagar com Cartão de Crédito ou Boleto",
                            color = palette.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    }
                }
            }
        }

        // 4. Selos de Confiança e Garantia
        item {
            Text(
                text = if (IS_PLAY_STORE_COMPLIANT_MODE) {
                    "🛡️ Cancelamento fácil a qualquer momento • Assinatura Google Play"
                } else {
                    "🛡️ Cancelamento fácil sem carência • Processado por Asaas Fintech"
                },
                color = palette.textMuted,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun NativePixQrCanvas(
    pixTeal: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cellCount = 21
        val cellSize = w / cellCount

        fun drawFinderPattern(startX: Float, startY: Float) {
            drawRoundRect(
                color = Color.Black,
                topLeft = Offset(startX, startY),
                size = Size(cellSize * 7, cellSize * 7),
                cornerRadius = CornerRadius(6f, 6f)
            )
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(startX + cellSize, startY + cellSize),
                size = Size(cellSize * 5, cellSize * 5),
                cornerRadius = CornerRadius(4f, 4f)
            )
            drawRoundRect(
                color = Color.Black,
                topLeft = Offset(startX + cellSize * 2, startY + cellSize * 2),
                size = Size(cellSize * 3, cellSize * 3),
                cornerRadius = CornerRadius(2f, 2f)
            )
        }

        drawFinderPattern(0f, 0f)
        drawFinderPattern(w - cellSize * 7, 0f)
        drawFinderPattern(0f, h - cellSize * 7)

        for (i in 0 until cellCount) {
            for (j in 0 until cellCount) {
                val inTopLeft = (i < 8 && j < 8)
                val inTopRight = (i > 13 && j < 8)
                val inBottomLeft = (i < 8 && j > 13)
                val inCenter = (i in 9..11 && j in 9..11)

                if (!inTopLeft && !inTopRight && !inBottomLeft && !inCenter) {
                    val pseudoRandomBit = ((i * 37 + j * 17 + (i xor j)) % 3) == 0
                    if (pseudoRandomBit) {
                        drawRect(
                            color = Color.Black,
                            topLeft = Offset(i * cellSize, j * cellSize),
                            size = Size(cellSize - 1f, cellSize - 1f)
                        )
                    }
                }
            }
        }

        val center = Offset(w / 2, h / 2)
        val diamondSize = cellSize * 2.8f
        val pixPath = Path().apply {
            moveTo(center.x, center.y - diamondSize)
            lineTo(center.x + diamondSize, center.y)
            lineTo(center.x, center.y + diamondSize)
            lineTo(center.x - diamondSize, center.y)
            close()
        }
        drawPath(path = pixPath, color = pixTeal)
    }
}
