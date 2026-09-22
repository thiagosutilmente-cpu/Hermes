package com.example

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.compose.ui.tooling.preview.Preview

/**
 * Card de comando de voz viva-voz (Hands-Free Speech Recognition)
 * Mostra o estado de escuta ativa do SpeechRecognizer do Android,
 * a última fala captada ("aceitar", "cancelar", "sim", "não"),
 * e instruções claras para comando durante a condução.
 */
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun VoiceCommandLiveBanner(
    voiceState: VoiceCommandState = VoiceCommandState(isListening = true, isPermissionGranted = true),
    isSpeaking: Boolean = false,
    onToggleListening: () -> Unit = {},
    onRequestMicPermission: () -> Unit = {},
    onSimulateCommand: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val micScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_pulse_scale"
    )

    val speakingPulse by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "speaking_pulse"
    )

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSpeaking -> Color(0xFF0B192C)
                voiceState.isListening -> Color(0xFF0D1F18)
                else -> DarkCard
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = if (isSpeaking || voiceState.isListening) 1.5.dp else 1.dp,
                color = when {
                    isSpeaking -> NeonBlue
                    voiceState.isListening -> NeonGreen
                    else -> DarkBorder
                },
                shape = RoundedCornerShape(18.dp)
            )
            .testTag("voice_command_banner")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Indicador Visual do Microfone / Alto-falante
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isSpeaking -> NeonBlue.copy(alpha = 0.25f)
                                    voiceState.isListening -> NeonGreen.copy(alpha = 0.2f)
                                    else -> DarkBg
                                }
                            )
                            .border(
                                width = 1.dp,
                                color = when {
                                    isSpeaking -> NeonBlue
                                    voiceState.isListening -> NeonGreen
                                    else -> DarkBorder
                                },
                                shape = CircleShape
                            )
                            .scale(if (isSpeaking) speakingPulse else if (voiceState.isListening) micScale else 1f)
                            .clickable {
                                if (voiceState.isPermissionGranted) {
                                    onToggleListening()
                                } else {
                                    onRequestMicPermission()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when {
                                isSpeaking -> "🔊"
                                voiceState.isListening -> "🎙️"
                                else -> "🔇"
                            },
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isSpeaking) "JARVIS FALANDO (TEXT-TO-SPEECH)" else "GOOGLE SPEECH-TO-TEXT (MÃOS NO GUIDÃO)",
                                color = when {
                                    isSpeaking -> NeonBlue
                                    voiceState.isListening -> NeonGreen
                                    else -> TextLight
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.6.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (voiceState.isListening) NeonGreen.copy(alpha = 0.2f) else DarkBg)
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = voiceState.engineName.uppercase(),
                                    color = if (voiceState.isListening) NeonGreen else TextMuted,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = when {
                                isSpeaking -> "Anunciando no capacete/fone Bluetooth..."
                                voiceState.isListening -> "Ouvindo capacete • Fale 'Aceitar' ou 'Recusar' sem soltar o guidão"
                                !voiceState.isPermissionGranted -> "Permissão de microfone necessária • Toque para autorizar"
                                else -> "Reconhecimento pausado • Toque no microfone para ativar"
                            },
                            color = when {
                                isSpeaking -> NeonBlue.copy(alpha = 0.9f)
                                voiceState.isListening -> NeonGreen.copy(alpha = 0.85f)
                                else -> TextMuted
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Botão de Toggle da Escuta
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when {
                                isSpeaking -> NeonBlue
                                voiceState.isListening -> NeonGreen
                                else -> DarkCardElevated
                            }
                        )
                        .clickable {
                            if (voiceState.isPermissionGranted) {
                                onToggleListening()
                            } else {
                                onRequestMicPermission()
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("btn_toggle_voice_recognition")
                ) {
                    Text(
                        text = when {
                            isSpeaking -> "FALANDO"
                            voiceState.isListening -> "AO VIVO"
                            else -> "LIGAR VOZ"
                        },
                        color = DarkBg,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            // Exibição da última fala detectada ou dicas de comando
            AnimatedVisibility(
                visible = isSpeaking || voiceState.isListening || voiceState.lastRecognizedText.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    if (voiceState.lastRecognizedText.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkBg)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "🗣️ Comando ouvido: ", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = "\"${voiceState.lastRecognizedText}\"",
                                color = if (voiceState.detectedCommand != null) NeonGreen else TextLight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    if (voiceState.isListening) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "MICROFONE (CAPACETE):",
                                color = TextMuted,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(DarkBg)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(voiceState.audioLevelFraction.coerceAtLeast(0.06f))
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(NeonGreen.copy(alpha = 0.5f), NeonGreen, NeonBlue)
                                            )
                                        )
                                )
                            }
                            Text(
                                text = "${(voiceState.audioLevelFraction * 100).toInt()}%",
                                color = if (voiceState.audioLevelFraction > 0.15f) NeonGreen else TextMuted,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // Chips táteis com os comandos por voz suportados
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        VoiceCommandBadge(
                            label = "🗣️ \"Aceitar\"",
                            desc = "aceita corrida",
                            color = NeonGreen,
                            onClick = { onSimulateCommand?.invoke("aceitar") },
                            modifier = Modifier.weight(1f)
                        )
                        VoiceCommandBadge(
                            label = "🗣️ \"Cancelar\"",
                            desc = "recusa pedido",
                            color = RedDecline,
                            onClick = { onSimulateCommand?.invoke("cancelar") },
                            modifier = Modifier.weight(1f)
                        )
                        VoiceCommandBadge(
                            label = "🗣️ \"Ler Oferta\"",
                            desc = "detalhes em áudio",
                            color = NeonBlue,
                            onClick = { onSimulateCommand?.invoke("ler oferta") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(5.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        VoiceCommandBadge(
                            label = "🗣️ \"Saldo\"",
                            desc = "faturamento hoje",
                            color = Color(0xFFFFB800),
                            onClick = { onSimulateCommand?.invoke("saldo") },
                            modifier = Modifier.weight(1f)
                        )
                        VoiceCommandBadge(
                            label = "🗣️ \"Rota\"",
                            desc = "inicia no mapa",
                            color = Color(0xFFFF6B6B),
                            onClick = { onSimulateCommand?.invoke("rota") },
                            modifier = Modifier.weight(1f)
                        )
                        VoiceCommandBadge(
                            label = "🗣️ \"Ajuda\"",
                            desc = "listar comandos",
                            color = TextLight,
                            onClick = { onSimulateCommand?.invoke("ajuda") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(DarkBg.copy(alpha = 0.5f))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "📳 ", fontSize = 9.sp)
                        Text(
                            text = "Fale no microfone do capacete ou toque nos botões para testar os comandos viva-voz.",
                            color = TextMuted,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceCommandBadge(
    label: String,
    desc: String,
    color: Color,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .border(0.8.dp, color.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            )
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Column {
            Text(text = label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Black)
            Text(text = desc, color = TextMuted, fontSize = 8.5.sp)
        }
    }
}
