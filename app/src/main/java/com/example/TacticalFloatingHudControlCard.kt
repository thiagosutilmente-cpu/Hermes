package com.example

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.runtime.collectAsState
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Cores táticas
private val NeonEmerald = Color(0xFF00FF88)
private val AlertRed = Color(0xFFFF3366)
private val CyberCyan = Color(0xFF00E5FF)
private val DarkCardSurface = Color(0xFF10141D)
private val CardBorderColor = Color(0xFF1F2636)
private val TextLight = Color(0xFFF3F4F8)
private val TextMuted = Color(0xFF8A93A6)

/**
 * Card de Controle e Simulação da Bolha Flutuante Tática Aprimorada (HUD sobre iFood/99/Uber)
 * Exibido no Painel Principal do Cockpit.
 */
@Composable
fun TacticalFloatingHudControlCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val overlayManager = remember { OverlayWindowManager.getInstance(context) }
    val ttsEngine = remember { OfferTextToSpeechEngine.getInstance(context) }
    val ttsState by ttsEngine.engineState.collectAsState()

    var isOverlayActive by remember { mutableStateOf(overlayManager.isShowing()) }
    var hasOverlayPermission by remember { mutableStateOf(OverlayWindowManager.canDrawOverlays(context)) }

    // Sincroniza estado de permissão
    LaunchedEffect(Unit) {
        hasOverlayPermission = OverlayWindowManager.canDrawOverlays(context)
        isOverlayActive = overlayManager.isShowing()
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("card_tactical_floating_hud"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCardSurface),
        border = androidx.compose.foundation.BorderStroke(
            1.2.dp,
            if (isOverlayActive) NeonEmerald.copy(alpha = 0.6f) else CardBorderColor
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Topo do Card: Ícone, Título e Switch On/Off
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isOverlayActive) NeonEmerald.copy(alpha = 0.18f) else Color(0xFF1C2433)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🫧", fontSize = 20.sp)
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "BOLHA FLUTUANTE HUD",
                                color = TextLight,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = if (isOverlayActive) NeonEmerald.copy(alpha = 0.2f) else Color(0xFF1F2838),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = if (isOverlayActive) "SOBRE OUTROS APPS" else "RECOLHIDA",
                                    color = if (isOverlayActive) NeonEmerald else TextMuted,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = "Card tático com R$/km e Rota Dupla sobre iFood, 99 e Uber",
                            color = TextMuted,
                            fontSize = 10.5.sp,
                            lineHeight = 14.sp
                        )
                    }
                }

                // Switch On/Off
                Switch(
                    checked = isOverlayActive,
                    onCheckedChange = { enable ->
                        if (enable) {
                            if (!OverlayWindowManager.canDrawOverlays(context)) {
                                Toast.makeText(context, "Conceda permissão de sobreposição para ativar a bolha.", Toast.LENGTH_LONG).show()
                                OverlayWindowManager.requestOverlayPermission(context)
                                return@Switch
                            }
                            overlayManager.show()
                            isOverlayActive = true
                            HapticFeedbackHelper.performClick(context)
                            Toast.makeText(context, "🫧 Bolha flutuante ativada sobre os apps!", Toast.LENGTH_SHORT).show()
                        } else {
                            overlayManager.hide()
                            isOverlayActive = false
                            HapticFeedbackHelper.performClick(context)
                            Toast.makeText(context, "Bolha flutuante recolhida.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.testTag("switch_floating_bubble_overlay"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = NeonEmerald,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = Color(0xFF1A2230)
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3 Pilares do HUD Compacto
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0A0E16))
                    .padding(12.dp)
            ) {
                FeatureRow(
                    icon = "🟢",
                    title = "Ganho Real por KM",
                    desc = "Calcula no milissegundo: exibe R$ 7,20/km 🟢 BOA ou R$ 2,80/km 🔴 PREJUÍZO."
                )
                Spacer(modifier = Modifier.height(8.dp))
                FeatureRow(
                    icon = "🔥",
                    title = "Indicador de Rota Dupla",
                    desc = "Avisa instantaneamente se há pedido compatível em outro app no mesmo trajeto."
                )
                Spacer(modifier = Modifier.height(8.dp))
                FeatureRow(
                    icon = "👆",
                    title = "Ações Gigantes em 1 Toque",
                    desc = "Botões de 48dp+ para aceitar ou dispensar sem tirar a mão do guidão da moto."
                )
                Spacer(modifier = Modifier.height(8.dp))
                FeatureRow(
                    icon = "🗣️",
                    title = "Leitura em Voz Alta (TTS Nativo)",
                    desc = "Anuncia valor, distância e lucro por km no fone Bluetooth para você nunca tirar os olhos da via."
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // =====================================================================
            // PAINEL DE CONTROLE DO MOTOR DE TEXT-TO-SPEECH (TTS ANDROID)
            // =====================================================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF0F1722))
                    .border(1.dp, if (!ttsState.isMuted) CyberCyan.copy(alpha = 0.5f) else Color(0xFF1E2838), RoundedCornerShape(14.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (!ttsState.isMuted) CyberCyan.copy(alpha = 0.2f) else Color(0xFF1F2838)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (!ttsState.isMuted) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                contentDescription = "TTS Status",
                                tint = if (!ttsState.isMuted) CyberCyan else TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "LEITURA POR VOZ (TTS)",
                                    color = TextLight,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                if (ttsState.isSpeaking) {
                                    Surface(
                                        color = NeonEmerald.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "FALANDO...",
                                            color = NeonEmerald,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = if (!ttsState.isMuted) "Ativo • Anuncia valor, km e R$/km" else "Silenciado pelo piloto",
                                color = if (!ttsState.isMuted) CyberCyan else TextMuted,
                                fontSize = 9.5.sp
                            )
                        }
                    }

                    Switch(
                        checked = !ttsState.isMuted,
                        onCheckedChange = { enable ->
                            ttsEngine.setMuted(!enable)
                            HapticFeedbackHelper.performClick(context)
                        },
                        modifier = Modifier.testTag("switch_tts_offer_reading"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = CyberCyan,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = Color(0xFF1E2838)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Seletor de Velocidade da Voz (Normal 1.0x, Ágil 1.15x, Turbo 1.3x)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Velocidade da Voz:",
                        color = TextMuted,
                        fontSize = 10.sp
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            1.0f to "1.0x Normal",
                            1.15f to "1.15x Ágil",
                            1.3f to "1.3x Turbo"
                        ).forEach { (rate, label) ->
                            val isSelected = Math.abs(ttsState.speechRate - rate) < 0.05f
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSelected) CyberCyan.copy(alpha = 0.25f) else Color(0xFF151D2A),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) CyberCyan else Color(0xFF222E42)
                                ),
                                modifier = Modifier
                                    .clickable {
                                        ttsEngine.setSpeechRate(rate)
                                        HapticFeedbackHelper.performClick(context)
                                    }
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) CyberCyan else TextMuted,
                                    fontSize = 9.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Botão de Teste Direto do TTS (Lê Valor, Distância e Lucro/KM)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            ttsEngine.speakOfferSummary(
                                appName = "iFood",
                                restaurant = "Shopping Eldorado",
                                value = 28.50,
                                distanceKm = 3.9,
                                profitPerKm = 7.30,
                                isGoodDeal = true,
                                hasDualRoute = true,
                                dualRouteAppName = "99 Moto",
                                dualRouteExtraGain = 16.50
                            )
                            Toast.makeText(context, "🔊 Reproduzindo áudio no alto-falante/fone...", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .testTag("btn_test_tts_speech_summary"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberCyan,
                            contentColor = Color.Black
                        )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.RecordVoiceOver,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "OUVIR RESUMO POR VOZ 🔊",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    if (ttsState.isSpeaking) {
                        OutlinedButton(
                            onClick = { ttsEngine.stop() },
                            modifier = Modifier
                                .height(40.dp)
                                .testTag("btn_stop_tts_speech"),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AlertRed),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertRed)
                        ) {
                            Text(text = "PARAR ⏹", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Exibe a transcrição do último áudio reproduzido
                if (ttsState.lastSpokenText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF090D14))
                            .padding(8.dp)
                    ) {
                        Column {
                            Text(
                                text = "Transcrição do áudio falado:",
                                color = TextMuted,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "\"${ttsState.lastSpokenText}\"",
                                color = Color(0xFFD6E2F0),
                                fontSize = 9.5.sp,
                                lineHeight = 13.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Botões de Teste Tático em Tempo Real
            Text(
                text = "SIMULAR CORRIDAS NA BOLHA (TESTE IMEDIATO):",
                color = CyberCyan,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Botão 1: Simular Corrida Boa (R$ 7,20/km 🟢)
                Button(
                    onClick = {
                        if (!OverlayWindowManager.canDrawOverlays(context)) {
                            OverlayWindowManager.requestOverlayPermission(context)
                            return@Button
                        }
                        isOverlayActive = true
                        overlayManager.showTacticalOffer(
                            OverlayWindowManager.FloatingTacticalOffer(
                                appName = "iFood",
                                packageName = "com.ifood.driver",
                                value = 28.50,
                                distanceKm = 3.9,
                                pricePerKm = 7.30,
                                isGoodDeal = true,
                                dealLabel = "R$ 7,20/km 🟢 BOA",
                                destination = "Av. Rebouças, 1200 • Pinheiros",
                                pickup = "Shopping Eldorado",
                                hasDualRoute = true,
                                dualRouteAppName = "99 Moto",
                                dualRouteExtraGain = 16.50
                            )
                        )
                        Toast.makeText(context, "🟢 Corrida Boa (R$ 7,20/km) enviada para a Bolha!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("btn_simulate_good_offer"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonEmerald,
                        contentColor = Color.Black
                    )
                ) {
                    Text(text = "R$ 7,20/km 🟢 BOA", fontSize = 11.sp, fontWeight = FontWeight.Black)
                }

                // Botão 2: Simular Corrida Prejuízo (R$ 2,80/km 🔴)
                Button(
                    onClick = {
                        if (!OverlayWindowManager.canDrawOverlays(context)) {
                            OverlayWindowManager.requestOverlayPermission(context)
                            return@Button
                        }
                        isOverlayActive = true
                        overlayManager.showTacticalOffer(
                            OverlayWindowManager.FloatingTacticalOffer(
                                appName = "99 Moto",
                                packageName = "com.taxis99",
                                value = 8.40,
                                distanceKm = 3.0,
                                pricePerKm = 2.80,
                                isGoodDeal = false,
                                dealLabel = "R$ 2,80/km 🔴 PREJUÍZO",
                                destination = "Pq. Ibirapuera",
                                pickup = "Rua Augusta",
                                hasDualRoute = false,
                                dualRouteAppName = "",
                                dualRouteExtraGain = 0.0
                            )
                        )
                        Toast.makeText(context, "🔴 Corrida Ruim (R$ 2,80/km) enviada para a Bolha!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("btn_simulate_bad_offer"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2C1318),
                        contentColor = AlertRed
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AlertRed)
                ) {
                    Text(text = "R$ 2,80/km 🔴 RUIM", fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun FeatureRow(icon: String, title: String, desc: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(text = icon, fontSize = 13.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextLight,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = desc,
                color = TextMuted,
                fontSize = 10.sp,
                lineHeight = 13.5.sp
            )
        }
    }
}
