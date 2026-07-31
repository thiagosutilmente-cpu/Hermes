package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.BuildConfig
import com.example.api.AsaasPaymentManager
import com.example.coordinator.RadarCoordinator
import com.example.util.ToastUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AsaasPaymentModal(
    onDismiss: () -> Unit,
    onPaymentSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("Piloto Radar") }
    var cpf by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var isCheckingStatus by remember { mutableStateOf(false) }

    var paymentResult by remember { mutableStateOf<AsaasPaymentManager.PixCheckoutResult?>(null) }
    var isPaidConfirmed by remember { mutableStateOf(false) }

    // Auto-poll status when PIX is generated
    LaunchedEffect(paymentResult) {
        val pid = paymentResult?.paymentId
        if (pid != null && paymentResult?.success == true && !isPaidConfirmed) {
            while (!isPaidConfirmed) {
                delay(4000)
                isCheckingStatus = true
                val confirmed = AsaasPaymentManager.checkPaymentStatus(pid)
                isCheckingStatus = false
                if (confirmed) {
                    isPaidConfirmed = true
                    AsaasPaymentManager.activateRiderProLicense()
                    ToastUtils.showToast(context, "✅ PAGAMENTO PIX CONFIRMADO! Licença ativada por 30 dias.")
                    RadarCoordinator.voiceManager?.speak("Pagamento PIX verificado com sucesso. Sua licença do Jarvis Pro está ativa por 30 dias.")
                    delay(2000)
                    onPaymentSuccess()
                    onDismiss()
                    break
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF111118),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00FF88).copy(alpha = 0.4f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Color(0xFF00FF88).copy(alpha = 0.2f),
                            shape = CircleShape,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFF00FF88),
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "JARVIS PRO",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF00FF88),
                                letterSpacing = 1.sp
                            )
                            Text(
                                "Renovação via PIX Asaas",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Fechar",
                            tint = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Price Badge
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1A1A24))
                        .border(1.dp, Color(0xFF2A2A3A), RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text(
                                "Plano Mensal Autônomo",
                                fontSize = 12.sp,
                                color = Color(0xFF8A8A9A)
                            )
                            Text(
                                "R$ ${BuildConfig.SUBSCRIPTION_PRICE} / mês",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF00FF88)
                            )
                        }
                        Surface(
                            color = Color(0xFF00FF88).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "PIX INSTANTÂNEO",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00FF88),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (paymentResult == null) {
                    // Form fields
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nome Completo") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00FF88),
                            unfocusedBorderColor = Color(0xFF333344),
                            focusedLabelColor = Color(0xFF00FF88),
                            unfocusedLabelColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = cpf,
                        onValueChange = { cpf = it },
                        label = { Text("CPF / CNPJ") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00FF88),
                            unfocusedBorderColor = Color(0xFF333344),
                            focusedLabelColor = Color(0xFF00FF88),
                            unfocusedLabelColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("E-mail (Para comprovante)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00FF88),
                            unfocusedBorderColor = Color(0xFF333344),
                            focusedLabelColor = Color(0xFF00FF88),
                            unfocusedLabelColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (cpf.isBlank() && name.isBlank()) {
                                ToastUtils.showToast(context, "Por favor, preencha o CPF e Nome para emitir o PIX.")
                                return@Button
                            }
                            isLoading = true
                            scope.launch {
                                val priceVal = BuildConfig.SUBSCRIPTION_PRICE.replace(",", ".").toDoubleOrNull() ?: 49.90
                                val result = AsaasPaymentManager.generatePixPayment(
                                    name = name,
                                    cpfCnpj = cpf,
                                    email = email,
                                    amount = priceVal
                                )
                                isLoading = false
                                paymentResult = result
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF88)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.Black,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.QrCode,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "GERAR PIX R$ ${BuildConfig.SUBSCRIPTION_PRICE}",
                                color = Color.Black,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    // Display PIX Copia e Cola & QR Code
                    val payload = paymentResult?.pixPayload ?: ""
                    val qrBase64 = paymentResult?.qrCodeBase64

                    if (isPaidConfirmed) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF00FF88),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "PAGAMENTO CONFIRMADO!",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF00FF88)
                            )
                            Text(
                                "Sua licença do Jarvis Pro foi estendida por 30 dias.",
                                fontSize = 12.sp,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (!qrBase64.isNull_or_empty_custom()) {
                                val bitmap = remember(qrBase64) { decodeBase64ToBitmap(qrBase64) }
                                if (bitmap != null) {
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = "QR Code PIX",
                                        modifier = Modifier
                                            .size(180.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.White)
                                            .padding(8.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                            }

                            Text(
                                "Copie o código abaixo e pague no seu app do banco:",
                                fontSize = 12.sp,
                                color = Color(0xFF8A8A9A),
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF0A0A0F))
                                    .border(1.dp, Color(0xFF2A2A3A), RoundedCornerShape(10.dp))
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = payload,
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    maxLines = 3,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Código PIX", payload)
                                    clipboard.setPrimaryClip(clip)
                                    ToastUtils.showToast(context, "📋 Código PIX copiado para a área de transferência!")
                                    RadarCoordinator.voiceManager?.speak("Código PIX copiado.")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3A86FF)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "COPIAR CÓDIGO PIX",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (isCheckingStatus) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = Color(0xFF00FF88),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(
                                    "Aguardando confirmação em tempo real...",
                                    fontSize = 11.sp,
                                    color = Color(0xFF00FF88)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    scope.launch {
                                        isCheckingStatus = true
                                        val pid = paymentResult?.paymentId ?: ""
                                        val confirmed = AsaasPaymentManager.checkPaymentStatus(pid)
                                        isCheckingStatus = false
                                        if (confirmed) {
                                            isPaidConfirmed = true
                                            AsaasPaymentManager.activateRiderProLicense()
                                            ToastUtils.showToast(context, "✅ PAGAMENTO CONFIRMADO!")
                                            onPaymentSuccess()
                                            onDismiss()
                                        } else {
                                            ToastUtils.showToast(context, "Pagamento ainda não identificado. Tentando novamente...")
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00FF88).copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = Color(0xFF00FF88),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                 Text(
                                    "JÁ PAGUEI / VERIFICAR AGORA",
                                    color = Color(0xFF00FF88),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = {
                                    try {
                                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(
                                                android.content.Intent.EXTRA_TEXT,
                                                "🎯 *RADAR COORDINATOR — JARVIS PRO*\n\n" +
                                                "Aumente seus ganhos no iFood, Rappi e 99 com aceite automático inteligente, cálculo de ganho/km e otimizador de rotas Ghost Sequence!\n\n" +
                                                "📲 Baixe o aplicativo e ative sua licença:\n" +
                                                "https://ais-pre-pg6paszmsamfrpcpczq7n7-321882160298.us-west2.run.app\n\n" +
                                                "🚀 *Ativação Jarvis Pro mensal*: R$ 29,90"
                                            )
                                        }
                                        context.startActivity(android.content.Intent.createChooser(shareIntent, "Compartilhar com outros Motoboys"))
                                    } catch (e: Exception) {
                                        ToastUtils.showToast(context, "Erro ao abrir compartilhamento")
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E2C)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB800)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = null,
                                    tint = Color(0xFFFFB800),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "COMPARTILHAR COM MOTOBOYS",
                                    color = Color(0xFFFFB800),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun String?.isNull_or_empty_custom(): Boolean {
    return this == null || this.trim().isEmpty()
}

private fun decodeBase64ToBitmap(base64Str: String?): Bitmap? {
    if (base64Str.isNullOrBlank()) return null
    return try {
        val cleanBase64 = if (base64Str.contains(",")) {
            base64Str.substringAfter(",")
        } else base64Str
        val decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    } catch (e: Exception) {
        null
    }
}
