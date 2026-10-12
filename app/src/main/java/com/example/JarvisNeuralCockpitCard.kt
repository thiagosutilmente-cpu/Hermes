package com.example

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

/**
 * JarvisNeuralCockpitCard
 *
 * Card interativo para execução, teste e auditoria em tempo real do
 * motor JARVIS NEURAL COCKPIT 2.0 (SINDICATO INVISÍVEL).
 */
@Composable
fun JarvisNeuralCockpitCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var selectedVoiceStyle by remember {
        mutableStateOf(JarvisNeuralCockpitEngine.getActiveVoiceStyle())
    }

    var inputText by remember {
        mutableStateOf("iFood: Nova entrega disponível! R$ 5,50 - Distância total: 8.2 km - Coleta: Mcdonalds")
    }
    var jsonOutput by remember { mutableStateOf<String?>(null) }
    var currentResponse by remember { mutableStateOf<JarvisNeuralCockpitEngine.JarvisResponse?>(null) }
    var executionLatencyMs by remember { mutableLongStateOf(-1L) }
    var isExpandedJson by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCardElevated),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .border(1.2.dp, Color(0xFF6C5CE7).copy(alpha = 0.6f), RoundedCornerShape(18.dp))
            .testTag("jarvis_neural_cockpit_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 1. Cabeçalho do Jarvis Neural Cockpit
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF6C5CE7))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "🧠 JARVIS NEURAL COCKPIT 2.0",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                if (executionLatencyMs >= 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF00B894).copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${executionLatencyMs}ms (Ultra-rápido)",
                            color = Color(0xFF55EFC4),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Motor de gamificação tática (CODENAME: SINDICATO INVISÍVEL). Alertas no capacete e cards para WhatsApp.",
                color = TextGray,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Seletor de Modo de Voz (TTS)
            Text(
                text = "ESTILO DE ALERTA DE VOZ (CAPACETE):",
                color = Color(0xFFA29BFE),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                JarvisNeuralCockpitEngine.VoiceAlertStyle.entries.forEach { style ->
                    val isSelected = (selectedVoiceStyle == style)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Color(0xFF6C5CE7) else Color(0xFF222238))
                            .border(1.dp, if (isSelected) Color(0xFFA29BFE) else Color(0xFF38385A), RoundedCornerShape(8.dp))
                            .clickable {
                                selectedVoiceStyle = style
                                JarvisNeuralCockpitEngine.setVoiceStyle(style, context)
                                HapticFeedbackHelper.vibrateClick(context)
                            }
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (style) {
                                JarvisNeuralCockpitEngine.VoiceAlertStyle.TACTICAL_ANGRY -> "🔥 Revoltado"
                                JarvisNeuralCockpitEngine.VoiceAlertStyle.WAR_GENERAL -> "⚔️ Estrategista"
                                JarvisNeuralCockpitEngine.VoiceAlertStyle.STANDARD -> "🎯 Padrão"
                            },
                            color = if (isSelected) Color.White else TextGray,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Presets de Treinamento Rápido
            Text(
                text = "PRESETS DE TESTE RÁPIDO:",
                color = TextGray,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PresetChip(label = "Lixo (iFood)", modifier = Modifier.weight(1f)) {
                    inputText = "iFood: Nova entrega disponível! R$ 5,50 - Distância total: 8.2 km - Coleta: Mcdonalds"
                }
                PresetChip(label = "Top (99Food)", modifier = Modifier.weight(1f)) {
                    inputText = "99Food: Corrida recebida R$ 22,00 - Distância: 5.0 km"
                }
                PresetChip(label = "Dupla Rota", modifier = Modifier.weight(1f)) {
                    inputText = "Uber Direct: Rota Dupla Agrupada! R$ 38,00 - Distância total: 8.0 km"
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PresetChip(label = "Indicação (4/5)", modifier = Modifier.weight(1f)) {
                    inputText = "[EVENTO_SISTEMA: REFERRAL_UPDATE | User: Thiago | Confirmados: 4 | AmigoPago: Marcos Silva]"
                }
                PresetChip(label = "VIP Anual (5/5)", modifier = Modifier.weight(1f)) {
                    inputText = "[EVENTO_SISTEMA: REFERRAL_UPDATE | User: Thiago | Confirmados: 5 | AmigoPago: Carlos Souza]"
                }
                PresetChip(label = "Sem R$ (Erro)", modifier = Modifier.weight(1f)) {
                    inputText = "Uber Direct: Nova entrega sem valor visível..."
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Campo de entrada do texto da notificação ou OCR
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                label = { Text("Texto da Notificação / OCR / Evento") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF6C5CE7),
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = Color(0xFFA29BFE),
                    unfocusedLabelColor = TextGray
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("jarvis_input_text_field"),
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 4. Botão de Execução
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val start = System.currentTimeMillis()
                        val res = JarvisNeuralCockpitEngine.processAndVoice(
                            context = context,
                            rawText = inputText,
                            styleOverride = selectedVoiceStyle
                        )
                        val elapsed = System.currentTimeMillis() - start
                        executionLatencyMs = elapsed
                        currentResponse = res
                        jsonOutput = JarvisNeuralCockpitEngine.processInputToJson(
                            rawText = inputText,
                            styleOverride = selectedVoiceStyle
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C5CE7)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("execute_jarvis_button")
                ) {
                    Text(
                        text = "⚡ Processar no JARVIS",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (currentResponse?.rideData?.voiceAlert != null) {
                    Button(
                        onClick = {
                            currentResponse?.rideData?.voiceAlert?.text?.let { speech ->
                                OfferTextToSpeechEngine.getInstance(context).speak(
                                    speech,
                                    android.speech.tts.TextToSpeech.QUEUE_FLUSH
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF28283E)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(44.dp)
                            .testTag("jarvis_speak_again_button")
                    ) {
                        Text(text = "🔊 Ouvir Voz", color = Color(0xFFFFB800), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 5. Exibição do Resultado
            val resp = currentResponse
            if (resp != null) {
                Spacer(modifier = Modifier.height(12.dp))

                if (resp.status == "SUCCESS" && resp.rideData != null) {
                    val rd = resp.rideData
                    val actionColor = when (rd.decision.action) {
                        "ACCEPT" -> NeonGreen
                        "BATCH" -> Color(0xFFFFB800)
                        else -> RedDecline
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkCard)
                            .border(1.dp, actionColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "DECISÃO: ${rd.decision.action} (${rd.platform})",
                                    color = actionColor,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "SCORE: ${rd.decision.score_0_to_100}/100",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Grid financeiro
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "R$/KM: R$ ${String.format(Locale.GERMAN, "%.2f", rd.financials.ratePerKm)}",
                                    color = actionColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "TEMPO: ${rd.financials.estimatedTimeMinutes} min",
                                    color = TextLight,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "GANHO/H: R$ ${String.format(Locale.GERMAN, "%.2f", rd.financials.estimatedHourlyRateBrl)}/h",
                                    color = NeonGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Motivo: ${rd.decision.reason}",
                                color = TextLight,
                                fontSize = 10.sp
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Áudio [${rd.voiceAlert.styleUsed}]: \"${rd.voiceAlert.text}\"",
                                color = Color(0xFFFFB800),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            // 6. CARD VIRAL DE COMBATE PARA WHATSAPP (QUANDO DECISÃO FOR REJECT)
                            if (rd.viralGroupCard != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF1B2E1E))
                                        .border(1.dp, Color(0xFF25D366).copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                                        .padding(10.dp)
                                ) {
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = rd.viralGroupCard.headline,
                                                color = Color(0xFF25D366),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                            Text(
                                                text = "CARD DO SINDICATO",
                                                color = Color(0xFFA29BFE),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = "\"${rd.viralGroupCard.shareTextWhatsapp}\"",
                                            color = TextLight,
                                            fontSize = 10.sp,
                                            lineHeight = 14.sp
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Button(
                                            onClick = {
                                                try {
                                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                        type = "text/plain"
                                                        putExtra(Intent.EXTRA_TEXT, rd.viralGroupCard.shareTextWhatsapp)
                                                        `package` = "com.whatsapp"
                                                    }
                                                    context.startActivity(shareIntent)
                                                } catch (_: Exception) {
                                                    val generalIntent = Intent(Intent.ACTION_SEND).apply {
                                                        type = "text/plain"
                                                        putExtra(Intent.EXTRA_TEXT, rd.viralGroupCard.shareTextWhatsapp)
                                                    }
                                                    context.startActivity(Intent.createChooser(generalIntent, "Compartilhar Alerta"))
                                                }
                                                HapticFeedbackHelper.vibrateSuccess(context)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(36.dp)
                                        ) {
                                            Text(
                                                text = "💬 Compartilhar no WhatsApp do Grupo",
                                                color = Color.Black,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (resp.status == "SUCCESS" && resp.referralData != null) {
                    val ref = resp.referralData
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1E2838))
                            .border(1.dp, NeonGreen.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "👑 MISSÃO EMBAIXADOR",
                                    color = Color(0xFFFFB800),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = if (ref.isAnnualVipUnlocked) "VIP DESBLOQUEADO!" else "${ref.confirmedReferrals}/5 AMIGOS",
                                    color = if (ref.isAnnualVipUnlocked) NeonGreen else Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Progresso: ${ref.progressBar} (${ref.confirmedReferrals}/5)",
                                color = NeonGreen,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = ref.messageText,
                                color = TextLight,
                                fontSize = 10.sp
                            )
                        }
                    }
                } else if (resp.status == "INVALID_DATA") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(RedDecline.copy(alpha = 0.15f))
                            .border(1.dp, RedDecline.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "⚠️ SYSTEM_ERROR [INVALID_DATA]: ${resp.errorMessage}",
                            color = RedDecline,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Toggle para visualizar o JSON estrito
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isExpandedJson) "▼ Ocultar JSON Estrito" else "► Ver JSON Estrito da API",
                        color = Color(0xFFA29BFE),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { isExpandedJson = !isExpandedJson }
                    )

                    if (jsonOutput != null) {
                        Text(
                            text = "Copiar JSON",
                            color = TextGray,
                            fontSize = 10.sp,
                            modifier = Modifier.clickable {
                                jsonOutput?.let { clipboardManager.setText(AnnotatedString(it)) }
                            }
                        )
                    }
                }

                AnimatedVisibility(
                    visible = isExpandedJson && jsonOutput != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0F0F1A))
                            .border(1.dp, Color(0xFF2E2E48), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = jsonOutput ?: "",
                            color = Color(0xFF81ECEC),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PresetChip(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF222238))
            .border(1.dp, Color(0xFF38385A), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 5.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color(0xFFA29BFE),
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}
