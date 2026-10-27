package com.example

import android.widget.Toast
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
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Queue
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val NeonGreen = Color(0xFF00FF88)
private val CyberCyan = Color(0xFF00D2FF)
private val CyberGold = Color(0xFFFFD700)
private val CardSurface = Color(0xFF13131D)
private val CardBorder = Color(0xFF232335)
private val TextLight = Color(0xFFF0F0F8)
private val TextMuted = Color(0xFF8888A2)

@Composable
fun AudioRouteAlertControlCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val audioService = rememberAudioService(context)

    val isReady by audioService.isTtsReady.collectAsState()
    val isSpeaking by audioService.isSpeaking.collectAsState()
    val isHeadsetConnected by audioService.isHeadsetConnected.collectAsState()
    val isEyesOnRoadEnabled by audioService.isEyesOnRoadModeEnabled.collectAsState()
    val speechRate by audioService.speechRate.collectAsState()
    val lastAlert by audioService.lastVoiceAlert.collectAsState()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("audio_route_alert_control_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Brush.horizontalGradient(listOf(CyberCyan.copy(alpha = 0.6f), CyberGold.copy(alpha = 0.6f)))
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header do Card
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                Brush.linearGradient(listOf(CyberCyan.copy(alpha = 0.2f), CyberGold.copy(alpha = 0.2f)))
                            )
                            .border(1.dp, CyberCyan.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Headphones,
                            contentDescription = "Áudio Fone",
                            tint = CyberCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "ÁUDIO TÁTICO: OLHOS NA ESTRADA",
                                color = TextLight,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Text(
                            text = "Text-to-Speech com Audio Ducking nos fones",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }

                // Switch Principal Liga/Desliga
                Switch(
                    checked = isEyesOnRoadEnabled,
                    onCheckedChange = { audioService.toggleEyesOnRoad(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = NeonGreen,
                        checkedTrackColor = Color(0xFF143020)
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Badges de Status do Fone e Ducking
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF181824))
                        .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                        .padding(vertical = 8.dp, horizontal = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "SAÍDA DE ÁUDIO", color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isHeadsetConnected) "🎧 FONE / BLUETOOTH" else "📱 ALTO-FALANTE",
                            color = if (isHeadsetConnected) CyberCyan else CyberGold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF181824))
                        .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                        .padding(vertical = 8.dp, horizontal = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "AUDIO DUCKING", color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "🟢 ATIVO (-80% MÚSICA)",
                            color = NeonGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Seletor de Velocidade da Fala
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "VELOCIDADE DA FALA:",
                    color = TextMuted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(1.0f, 1.20f, 1.40f).forEach { rate ->
                        val isSelected = (speechRate == rate)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) CyberCyan.copy(alpha = 0.2f) else Color(0xFF181824))
                                .border(
                                    1.dp,
                                    if (isSelected) CyberCyan else CardBorder,
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable { audioService.setSpeechRate(rate) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${rate}x",
                                color = if (isSelected) CyberCyan else TextMuted,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Último Anúncio Falado
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF090A10))
                    .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = null,
                        tint = if (isSpeaking) NeonGreen else TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = lastAlert,
                        color = if (isSpeaking) NeonGreen else TextLight,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 2
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Botões de Teste de Alertas no Fone
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        audioService.announceQueueChange(
                            changeType = DeliveryAudioNotificationService.QueueChangeType.ORDER_ADDED_TO_STACK,
                            appName = "99 Food",
                            restaurant = "Burger King Shopping Paulista",
                            extraDetails = "Faturamento extra de dezenove reais."
                        )
                        HapticFeedbackHelper.vibrateAccept(context)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B2A24))
                ) {
                    Icon(
                        imageVector = Icons.Default.Queue,
                        contentDescription = "Fila",
                        tint = NeonGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Mudança Fila",
                        color = NeonGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = {
                        audioService.announceRouteChange(
                            reason = DeliveryAudioNotificationService.RouteChangeReason.TRAFFIC_DETOUR_AVOIDED,
                            targetName = "Jardins",
                            deviationKmOrMeters = "800 metros"
                        )
                        HapticFeedbackHelper.vibrateSuccess(context)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF192535))
                ) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = "Rota",
                        tint = CyberCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Alteração Rota",
                        color = CyberCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberAudioService(context: android.content.Context): DeliveryAudioNotificationService {
    return androidx.compose.runtime.remember {
        DeliveryAudioNotificationService.getInstance(context)
    }
}
