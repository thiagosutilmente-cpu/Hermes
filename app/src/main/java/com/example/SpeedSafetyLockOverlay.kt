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
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

/**
 * Overlay de Bloqueio Integral da Interface do Aplicativo por Velocidade.
 *
 * Acionado automaticamente pelo [LocationService] / [SpeedSafetyMonitor] quando o
 * Fused Location Provider detecta velocidade de deslocamento superior a 10 km/h.
 *
 * Intercepta e consome todos os eventos de toque na tela para evitar distrações e
 * acidentes de moto, exibindo o velocímetro digital em tempo real e habilitando
 * a operação 100% viva-voz via Speech-to-Text do Google.
 */
@Composable
fun SpeedSafetyLockOverlay(
    isLocked: Boolean,
    currentSpeedKmh: Double,
    speedThresholdKmh: Double = 10.0,
    isListeningVoice: Boolean = true,
    lastVoiceCommand: String = "",
    rmsDb: Float = 0f,
    pendingOffer: DeliveryOffer? = null,
    onSimulateVoiceCommand: ((String) -> Unit)? = null,
    onSetSimulatedSpeed: ((Double) -> Unit)? = null,
    onAcceptCurrentOffer: (() -> Unit)? = null,
    onDeclineCurrentOffer: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isLocked,
        enter = fadeIn(animationSpec = tween(280)) + scaleIn(initialScale = 0.94f),
        exit = fadeOut(animationSpec = tween(240)) + scaleOut(targetScale = 0.96f),
        modifier = modifier
    ) {
        // Interceptador global de toques: impede qualquer interação acidental com a interface subjacente
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xF005050A)) // Fundo escuro OLED com 94% de opacidade para foco total
                .pointerInput(Unit) {
                    // Consome todos os toques sem repassar para a tela abaixo
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            event.changes.forEach { it.consume() }
                        }
                    }
                }
                .testTag("speed_safety_lock_overlay"),
            contentAlignment = Alignment.Center
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "pulse_lock")
            val pulseAlpha by infiniteTransition.animateFloat(
                initialValue = 0.4f,
                targetValue = 0.95f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1100, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulse_lock_alpha"
            )

            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 24.dp)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. CABEÇALHO DE ALERTA DE SEGURANÇA
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Badge pulsante de bloqueio de segurança
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(30.dp))
                            .background(Color(0xFF2A0D10))
                            .border(
                                width = 1.5.dp,
                                color = Color(0xFFFF2A4B).copy(alpha = pulseAlpha),
                                shape = RoundedCornerShape(30.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF2A4B).copy(alpha = pulseAlpha))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SENSOR DE VELOCIDADE ATIVO (> ${speedThresholdKmh.toInt()} KM/H)",
                                color = Color(0xFFFF4D6D),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.8.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "INTERFACE BLOQUEADA",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Toques na tela desativados por segurança enquanto em movimento. Mantenha as mãos no guidão e os olhos na pista.",
                        color = Color(0xFFA0A0B2),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. VELOCÍMETRO DIGITAL HUD (FUSED LOCATION PROVIDER)
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF101018)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 2.dp,
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFFFF2A4B).copy(alpha = 0.6f), Color(0xFF1E1E2C))
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "VELOCIDADE EM TEMPO REAL (GPS)",
                            color = Color(0xFF8E8EA8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Display gigante da velocidade
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = String.format(Locale.US, "%.0f", currentSpeedKmh),
                                color = Color(0xFFFF3355),
                                fontSize = 64.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 64.sp,
                                modifier = Modifier.testTag("speed_lock_velocity_display")
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "km/h",
                                color = Color(0xFFFF8599),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 10.dp)
                            )
                        }

                        // Barra de progresso da velocidade em relação ao limiar de segurança
                        val speedProgress = (currentSpeedKmh / 50.0).toFloat().coerceIn(0f, 1f)
                        LinearProgressIndicator(
                            progress = { speedProgress },
                            color = Color(0xFFFF2A4B),
                            trackColor = Color(0xFF2B161B),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "0 km/h (Parado)",
                                color = Color(0xFF6B6B82),
                                fontSize = 10.sp
                            )
                            Text(
                                text = "Limite de bloqueio: ${speedThresholdKmh.toInt()} km/h",
                                color = Color(0xFFFFB800),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "50+ km/h",
                                color = Color(0xFF6B6B82),
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 3. OFERTA EM DESTAQUE (SE HOUVER UMA OFERTA PENDENTE DURANTE O DESLOCAMENTO)
                if (pendingOffer != null) {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF141422)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.2.dp, Color(0xFF00FF88).copy(alpha = 0.4f), RoundedCornerShape(18.dp))
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
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                when (pendingOffer.appOrigem.lowercase()) {
                                                    "ifood" -> Color(0xFFEA1D2C)
                                                    "rappi" -> Color(0xFFFF441F)
                                                    "uber", "uber eats" -> Color.Black
                                                    else -> Color(0xFFFFB800)
                                                }
                                            )
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = pendingOffer.appOrigem.uppercase(),
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = pendingOffer.nomeRestaurante,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Text(
                                    text = String.format(Locale("pt", "BR"), "R$ %.2f", pendingOffer.valor),
                                    color = Color(0xFF00FF88),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "📍 ${pendingOffer.distancia} km total",
                                    color = Color(0xFFA5A5BC),
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = String.format(Locale("pt", "BR"), "R$ %.2f / km", pendingOffer.ganhoPorKm),
                                    color = Color(0xFF00E5FF),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // 4. PAINEL VIVA-VOZ GOOGLE SPEECH-TO-TEXT (MÃOS NO GUIDÃO)
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0C1914)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.2.dp, Color(0xFF00FF88).copy(alpha = 0.6f), RoundedCornerShape(18.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF00FF88).copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = if (isListeningVoice) "🎙️" else "🔇", fontSize = 18.sp)
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "COMANDOS DE VOZ ATIVOS (GOOGLE STT)",
                                    color = Color(0xFF00FF88),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = if (lastVoiceCommand.isNotBlank())
                                        "Detectado: \"$lastVoiceCommand\""
                                    else
                                        "Fale para decidir a corrida com as duas mãos no guidão.",
                                    color = if (lastVoiceCommand.isNotBlank()) Color(0xFF00D2FF) else Color(0xFF90B5A2),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Chips informativos de comandos suportados
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            VoiceHintChip(
                                label = "🗣️ \"Aceitar\"",
                                hint = "Pega a corrida",
                                color = Color(0xFF00FF88),
                                onClick = { onSimulateVoiceCommand?.invoke("aceitar") }
                            )
                            VoiceHintChip(
                                label = "🗣️ \"Recusar\"",
                                hint = "Descarta oferta",
                                color = Color(0xFFFF3355),
                                onClick = { onSimulateVoiceCommand?.invoke("recusar") }
                            )
                            VoiceHintChip(
                                label = "🗣️ \"Ler Oferta\"",
                                hint = "Ouve detalhes",
                                color = Color(0xFF00D2FF),
                                onClick = { onSimulateVoiceCommand?.invoke("ler oferta") }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 5. BARRA DE TESTES / SIMULAÇÃO DE VELOCIDADE (PARA TESTAR O BLOQUEIO E DESBLOQUEIO)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF14141E))
                        .border(1.dp, Color(0xFF2E2E42), RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "🛠️ TESTAR COMPORTAMENTO DO SENSOR DE VELOCIDADE:",
                        color = Color(0xFFA0A0B8),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Botão de parada: Desbloqueia instantaneamente a tela
                        SpeedSimulatorTestButton(
                            label = "🛑 0 km/h (Desbloquear)",
                            color = Color(0xFF00FF88),
                            isSelected = currentSpeedKmh <= 2.0,
                            modifier = Modifier.weight(1.3f)
                        ) {
                            onSetSimulatedSpeed?.invoke(0.0)
                        }

                        SpeedSimulatorTestButton(
                            label = "🚶 6 km/h",
                            color = Color(0xFF00E5FF),
                            isSelected = currentSpeedKmh in 2.1..9.9,
                            modifier = Modifier.weight(0.9f)
                        ) {
                            onSetSimulatedSpeed?.invoke(6.0)
                        }

                        SpeedSimulatorTestButton(
                            label = "🏍️ 15 km/h",
                            color = Color(0xFFFF9900),
                            isSelected = currentSpeedKmh in 10.0..22.0,
                            modifier = Modifier.weight(0.9f)
                        ) {
                            onSetSimulatedSpeed?.invoke(15.0)
                        }

                        SpeedSimulatorTestButton(
                            label = "🚀 35 km/h",
                            color = Color(0xFFFF2A4B),
                            isSelected = currentSpeedKmh > 22.0,
                            modifier = Modifier.weight(0.9f)
                        ) {
                            onSetSimulatedSpeed?.invoke(35.0)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceHintChip(
    label: String,
    hint: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                color = color,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = hint,
                color = color.copy(alpha = 0.75f),
                fontSize = 8.sp
            )
        }
    }
}

@Composable
private fun SpeedSimulatorTestButton(
    label: String,
    color: Color,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) color.copy(alpha = 0.25f) else Color(0xFF1D1D2C))
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) color else Color(0xFF383850),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) color else Color(0xFFB0B0C4),
            fontSize = 9.sp,
            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}
