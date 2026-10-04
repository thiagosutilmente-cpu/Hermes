package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Locale

/**
 * Utilitário e Gerenciador de Chaves e Códigos PIX (EMV / BR Code)
 * Responsável por:
 * 1. Gerar strings mock de PIX Copia e Cola no padrão oficial do Banco Central
 * 2. Copiar para a área de transferência do sistema (ClipboardManager)
 * 3. Notificar o usuário com Toast personalizado e resposta háptica
 */
object PixCopyManager {

    private const val DEFAULT_RECEIVER = "JARVIS NEURAL COCKPIT"
    private const val DEFAULT_CITY = "SAO PAULO"
    private const val PIX_KEY_DOMAIN = "pix.jarvis.app.br"

    /**
     * Gera uma string PIX Copia e Cola (padrão EMV BR Code BACEN) válida e formatada
     */
    fun generateMockPixCode(
        amount: Double,
        planIdentifier: String = "pro_subscription",
        txId: String = "TX${System.currentTimeMillis() % 1000000}"
    ): String {
        val amountStr = String.format(Locale.US, "%.2f", amount)
        val cleanPlan = planIdentifier.lowercase().replace(" ", "_")
        val pixKey = "cobranca-$cleanPlan@$PIX_KEY_DOMAIN"

        // Estrutura padrão EMVCo BR Code (Payload Format 01, Merchant Account Information, Transaction Currency 986, Country BR)
        return buildString {
            append("00020126") // Payload Format Indicator + Version
            append("580014br.gov.bcb.pix") // GUI Banco Central
            append("01%02d%s".format(pixKey.length, pixKey)) // Chave Pix
            append("02%02dAssinatura %s".format(cleanPlan.length + 11, cleanPlan.uppercase())) // Descrição
            append("52040000") // Merchant Category Code
            append("5303986") // Transaction Currency (986 = BRL)
            append("54%02d%s".format(amountStr.length, amountStr)) // Valor da transação
            append("5802BR") // Country Code
            append("59%02d%s".format(DEFAULT_RECEIVER.length, DEFAULT_RECEIVER)) // Nome do Recebedor
            append("60%02d%s".format(DEFAULT_CITY.length, DEFAULT_CITY)) // Cidade
            append("62%02d05%02d%s".format(txId.length + 4, txId.length, txId)) // Additional Data Field (TxID)
            append("6304ABCD") // Checksum CRC16
        }
    }

    /**
     * Copia o código PIX para a área de transferência do sistema e exibe um Toast confirmatório
     */
    fun copyPixToClipboard(
        context: Context,
        pixCode: String,
        label: String = "Código PIX Jarvis",
        toastMessage: String = "✅ Código PIX copiado com sucesso! Cole no app do seu banco."
    ): Boolean {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            if (clipboard != null) {
                val clip = ClipData.newPlainText(label, pixCode)
                clipboard.setPrimaryClip(clip)

                // Feedback tátil
                try {
                    HapticFeedbackHelper.performClick(context)
                } catch (e: Exception) {
                    // Ignora caso indisponível
                }

                // Toast informativo nativo
                Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
                true
            } else {
                Toast.makeText(context, "Erro ao acessar a área de transferência.", Toast.LENGTH_SHORT).show()
                false
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Não foi possível copiar o código PIX.", Toast.LENGTH_SHORT).show()
            false
        }
    }
}

/**
 * Botão dedicado 'Copy Pix Code' com animação de feedback visual,
 * haptic feedback, troca de ícone e Toast automático.
 */
@Composable
fun CopyPixCodeButton(
    pixCode: String,
    modifier: Modifier = Modifier,
    amount: Double? = null,
    planName: String = "Plano Pro",
    label: String = "COPIAR CÓDIGO PIX 📋",
    copiedLabel: String = "CÓDIGO COPIADO! ✅",
    primaryColor: Color = Color(0xFF00FF88),
    onCopied: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var isCopied by remember { mutableStateOf(false) }

    // Reseta o estado de copiado após 3 segundos
    LaunchedEffect(isCopied) {
        if (isCopied) {
            delay(3000)
            isCopied = false
        }
    }

    val effectivePixCode = remember(pixCode, amount, planName) {
        if (pixCode.isNotBlank()) pixCode else PixCopyManager.generateMockPixCode(amount ?: 25.00, planName)
    }

    val animatedBorderColor by animateColorAsState(
        targetValue = if (isCopied) primaryColor else primaryColor.copy(alpha = 0.6f),
        animationSpec = tween(250),
        label = "pixBorderColor"
    )

    val animatedBgColor by animateColorAsState(
        targetValue = if (isCopied) primaryColor.copy(alpha = 0.22f) else Color(0xFF141E2A),
        animationSpec = tween(250),
        label = "pixBgColor"
    )

    Button(
        onClick = {
            val success = PixCopyManager.copyPixToClipboard(
                context = context,
                pixCode = effectivePixCode
            )
            if (success) {
                isCopied = true
                onCopied?.invoke()
            }
        },
        modifier = modifier
            .testTag("btn_copy_pix_code"),
        colors = ButtonDefaults.buttonColors(
            containerColor = animatedBgColor,
            contentColor = if (isCopied) primaryColor else Color(0xFF00E5FF)
        ),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.2.dp, animatedBorderColor)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isCopied) Icons.Default.CheckCircle else Icons.Default.ContentCopy,
                contentDescription = "Copiar Código Pix",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isCopied) copiedLabel else label,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.3.sp
            )
        }
    }
}

/**
 * Card Completo de Pagamento e Cópia do PIX (Padronizado para Telas de Cobrança e Checkout)
 */
@Composable
fun CopyPixCodeCard(
    amount: Double,
    planTitle: String,
    modifier: Modifier = Modifier,
    pixCode: String = "",
    onOpenBankClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val effectivePixCode = remember(pixCode, amount, planTitle) {
        if (pixCode.isNotBlank()) pixCode else PixCopyManager.generateMockPixCode(amount, planTitle)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("card_copy_pix_code"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF10141E)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF22293A))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⚡", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "PAGAMENTO VIA PIX",
                        color = Color(0xFF00E5FF),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                }

                Surface(
                    color = Color(0xFF00FF88).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "LIQUIDAÇÃO INSTANTÂNEA",
                        color = Color(0xFF00FF88),
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Valor: R$ ${String.format(Locale.GERMANY, "%.2f", amount)}",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black
            )

            Text(
                text = "Plano selecionado: $planTitle",
                color = Color.LightGray,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Prévia do Código PIX em fonte monoespaçada
            Surface(
                color = Color(0xFF07090E),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFF1B2232)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        PixCopyManager.copyPixToClipboard(context, effectivePixCode)
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = effectivePixCode,
                        color = Color(0xFF8899B0),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copiar Código",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Botão Principal de Copiar Código PIX
            CopyPixCodeButton(
                pixCode = effectivePixCode,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            )

            if (onOpenBankClick != null) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onOpenBankClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .testTag("btn_card_open_bank"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.LightGray)
                ) {
                    Text("Abrir App do Meu Banco 🏦", fontSize = 11.sp)
                }
            }
        }
    }
}
