package com.example

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

/**
 * COMPONENTE MODO FOCO EM TRÂNSITO (HEAD-UP DISPLAY / HUD OVERLAY)
 * Exibição minimalista de alta prioridade quando a moto está em movimento:
 * - Velocímetro & Precisão do GPS em destaque
 * - Card prioritário da melhor oferta ativa
 * - Botões gigantes de 54dp para toques seguros mesmo com luvas grossas
 */
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun FocusModeHudCard(
    isFocusModeActive: Boolean = true,
    onToggleFocusMode: (Boolean) -> Unit = {},
    bestOffer: RadarOffer? = RadarOffer(
        id = "preview_1",
        appName = "iFood",
        restaurant = "Burger King Paulista",
        value = 28.50,
        distanceKm = 3.2,
        estimatedTimeMin = 14,
        neuralDecision = NeuralDecision(RadarDecision.ACCEPT, "Ganho/km vantajoso", 0.94),
        itemsCount = 2,
        gainPerKm = 8.90,
        fuelCost = 0.54,
        netProfit = 27.96
    ),
    onAcceptBestOffer: (() -> Unit)? = null,
    onDeclineBestOffer: (() -> Unit)? = null,
    isListeningVoice: Boolean = true,
    lastVoiceCommand: String = "aceitar",
    currentSpeedKmh: Double = 0.0,
    isSpeedSafetyLockActive: Boolean = false,
    modifier: Modifier = Modifier
) {
    val speedKmh = currentSpeedKmh
    val gpsAccuracyMeters by remember { mutableDoubleStateOf(4.2) }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isFocusModeActive) Color(0xFF0F141C) else DarkCard
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = if (isFocusModeActive) 1.8.dp else 1.dp,
                color = if (isFocusModeActive) Color(0xFF00D2FF) else DarkBorder,
                shape = RoundedCornerShape(22.dp)
            )
            .testTag("focus_mode_hud_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header do Modo Foco
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
                            .background(if (isFocusModeActive) Color(0xFF00D2FF) else TextMuted)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "MODO FOCO EM TRÂNSITO (HUD)",
                            color = TextLight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = if (isFocusModeActive) "Modo Guidão Ativo • Comandos por Voz" else "Interface Padrão",
                            color = if (isFocusModeActive) Color(0xFF00D2FF) else TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Switch(
                    checked = isFocusModeActive,
                    onCheckedChange = onToggleFocusMode,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF00D2FF),
                        checkedTrackColor = Color(0xFF00D2FF).copy(alpha = 0.35f),
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = DarkBorder
                    ),
                    modifier = Modifier.testTag("switch_focus_mode")
                )
            }

            AnimatedVisibility(
                visible = isFocusModeActive,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    // Velocímetro e Telemetria de Satélite
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Velocidade Instantânea
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkBg)
                                .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "VELOCIDADE", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "${speedKmh.toInt()} km/h",
                                    color = Color(0xFF00D2FF),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        // Precisão GPS
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkBg)
                                .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "GPS RTK", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "±${gpsAccuracyMeters}m",
                                    color = NeonGreen,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        // Latência Jarvis
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkBg)
                                .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "LATÊNCIA", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "12 ms",
                                    color = TextLight,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Oferta Ativa Prioritária para Decisão Rápida
                    if (bestOffer != null) {
                        val formattedVal = String.format(Locale.GERMANY, "R$ %.2f", bestOffer.value)
                        val formattedPerKm = String.format(Locale.GERMANY, "R$ %.2f/km", bestOffer.gainPerKm)

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(DarkBg)
                                .border(1.5.dp, NeonGreen, RoundedCornerShape(14.dp))
                                .padding(14.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "🚨 CORRIDA EM DESTAQUE",
                                        color = NeonGreen,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text(
                                        text = formattedVal,
                                        color = NeonGreen,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "🍔 ${bestOffer.restaurant} • 🛵 ${bestOffer.distanceKm} km • ⚡ $formattedPerKm",
                                    color = TextLight,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Botões Gigantes para Toque de Luva (54dp)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { onDeclineBestOffer?.invoke() },
                                        enabled = !isSpeedSafetyLockActive,
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = RedDecline,
                                            disabledContentColor = RedDecline.copy(alpha = 0.4f)
                                        ),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(54.dp)
                                            .testTag("btn_focus_decline")
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("❌ CANCELAR", fontWeight = FontWeight.Black, fontSize = 12.sp)
                                            if (isListeningVoice) {
                                                Text("Fale \"Cancelar\"", fontSize = 9.sp, color = RedDecline.copy(alpha = 0.8f))
                                            }
                                        }
                                    }

                                    Button(
                                        onClick = { onAcceptBestOffer?.invoke() },
                                        enabled = !isSpeedSafetyLockActive,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = NeonGreen,
                                            contentColor = DarkBg,
                                            disabledContainerColor = NeonGreen.copy(alpha = 0.3f),
                                            disabledContentColor = DarkBg.copy(alpha = 0.5f)
                                        ),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .weight(1.3f)
                                            .height(54.dp)
                                            .testTag("btn_focus_accept")
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("✅ ACEITAR", fontWeight = FontWeight.Black, fontSize = 13.sp)
                                            if (isListeningVoice) {
                                                Text("Fale \"Aceitar\"", fontSize = 9.sp, color = DarkBg.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }

                                if (isSpeedSafetyLockActive) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(RedDecline.copy(alpha = 0.18f))
                                            .border(1.dp, RedDecline.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "🚨", fontSize = 13.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "VELOCIDADE > 10 KM/H: Toques bloqueados por segurança. Use o viva-voz.",
                                            color = RedDecline,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                if (isListeningVoice) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF0D1F18))
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "🎙️ ", fontSize = 11.sp)
                                        Text(
                                            text = if (lastVoiceCommand.isNotEmpty()) "Comando: \"$lastVoiceCommand\"" else "Mãos livres: Fale 'Aceitar' ou 'Cancelar'",
                                            color = NeonGreen,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "Aguardando próxima chamada prioritária...",
                            color = TextMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
