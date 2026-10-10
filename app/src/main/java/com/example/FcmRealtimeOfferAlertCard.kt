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
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
 * FcmRealtimeOfferAlertCard
 *
 * Card e Banner interativo para exibição em tempo real de novas ofertas despachadas
 * via Firebase Cloud Messaging (FCM) diretamente do backend VPS da Hostinger.
 */
@Composable
fun FcmRealtimeOfferAlertCard(
    onAcceptOffer: (RadarOffer) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val fcmAlert by RadarFirebaseMessagingService.latestPushOffer.collectAsState()
    val fcmToken by RadarFirebaseMessagingService.fcmTokenState.collectAsState()
    var isDismissed by remember { mutableStateOf(false) }
    var currentOfferId by remember { mutableStateOf<String?>(null) }

    // Quando chega uma nova oferta via FCM, reseta o estado de dispensado
    LaunchedEffect(fcmAlert?.offerId) {
        if (fcmAlert != null && fcmAlert?.offerId != currentOfferId) {
            currentOfferId = fcmAlert?.offerId
            isDismissed = false
        }
    }

    // Auto-dismiss após 25 segundos se o usuário não interagir
    LaunchedEffect(fcmAlert?.offerId, isDismissed) {
        if (fcmAlert != null && !isDismissed) {
            delay(25000L)
            isDismissed = true
        }
    }

    val alert = fcmAlert
    val ttsEngine = remember { OfferTextToSpeechEngine.getInstance(context) }
    val ttsState by ttsEngine.engineState.collectAsState()

    AnimatedVisibility(
        visible = alert != null && !isDismissed,
        enter = fadeIn(tween(250)) + slideInVertically(tween(300)) { -it },
        exit = fadeOut(tween(200)) + slideOutVertically(tween(250)) { -it },
        modifier = modifier
    ) {
        if (alert != null) {
            val infiniteTransition = rememberInfiniteTransition(label = "pulse_fcm")
            val pulseAlpha by infiniteTransition.animateFloat(
                initialValue = 0.5f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(900, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulse_fcm_border"
            )

            val isGoodDeal = alert.decision == "ACCEPT" || alert.gainPerKm >= 2.0
            val accentColor = if (isGoodDeal) NeonGreen else RedDecline

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .border(
                        width = 1.8.dp,
                        brush = Brush.horizontalGradient(
                            listOf(accentColor.copy(alpha = pulseAlpha), Color(0xFFFF9900).copy(alpha = 0.7f))
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .testTag("fcm_realtime_offer_alert_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // 1. Cabeçalho FCM com badge de tempo real
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
                                    .background(accentColor)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "⚡ NOVA OFERTA FCM EM TEMPO REAL",
                                color = Color(0xFFFFB800),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.8.sp
                            )
                        }

                        // Badge da plataforma
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when (alert.appName.lowercase()) {
                                        "ifood" -> RedDecline.copy(alpha = 0.25f)
                                        "uber" -> DarkCardElevated
                                        "99", "99food" -> Color(0xFFFFB800).copy(alpha = 0.25f)
                                        else -> NeonGreen.copy(alpha = 0.2f)
                                    }
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = alert.appName.uppercase(),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 2. Restaurante / Origem da corrida
                    Text(
                        text = alert.restaurant,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 3. Grid de Telemetria Financeira (Valor, Distância, R$/km)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Valor bruto
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkCardElevated)
                                .padding(vertical = 8.dp, horizontal = 10.dp)
                        ) {
                            Column {
                                Text(text = "VALOR", color = TextGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = String.format(Locale.GERMAN, "R$ %.2f", alert.value),
                                    color = NeonGreen,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        // Distância
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkCardElevated)
                                .padding(vertical = 8.dp, horizontal = 10.dp)
                        ) {
                            Column {
                                Text(text = "DISTÂNCIA", color = TextGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = String.format(Locale.GERMAN, "%.1f km", alert.distanceKm),
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        // R$/km
                        Box(
                            modifier = Modifier
                                .weight(1.1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isGoodDeal) NeonGreen.copy(alpha = 0.15f) else RedDecline.copy(alpha = 0.15f))
                                .padding(vertical = 8.dp, horizontal = 10.dp)
                        ) {
                            Column {
                                Text(text = "RENTABILIDADE", color = TextGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = String.format(Locale.GERMAN, "R$ %.2f/km", alert.gainPerKm),
                                    color = if (isGoodDeal) NeonGreen else RedDecline,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 4. Recomendação da IA / Voice Alert do Backend
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkCardElevated.copy(alpha = 0.8f))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = if (isGoodDeal) "✅" else "⚠️",
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = alert.voiceAlert,
                            color = TextLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 4.1 Barra de Áudio Text-to-Speech (TTS) nativo do Android
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (ttsState.isSpeaking) Color(0xFF1E2D40) else Color(0xFF1B1B28))
                            .border(1.dp, if (ttsState.isSpeaking) NeonGreen.copy(alpha = 0.6f) else DarkBorder, RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (ttsState.isSpeaking) "🔊" else "🎧",
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (ttsState.isSpeaking) "Vocalizando no capacete..." else "Áudio TTS Android (pt-BR)",
                                color = if (ttsState.isSpeaking) NeonGreen else TextGray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Botão de Repetir Áudio TTS sem tocar no celular
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF28283E))
                                .clickable {
                                    ttsEngine.speakFcmOffer(alert)
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("fcm_replay_tts_button")
                        ) {
                            Text(
                                text = "🔄 Repetir Voz",
                                color = Color(0xFFFFB800),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 5. Botões de ação rápida
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                isDismissed = true
                                RadarFirebaseMessagingService.clearLatestOffer()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextGray),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Text(text = "Dispensar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val radarOffer = RadarOffer(
                                    id = alert.offerId,
                                    appName = alert.appName,
                                    restaurant = alert.restaurant,
                                    value = alert.value,
                                    distanceKm = alert.distanceKm,
                                    timeMinutes = (alert.distanceKm * 3.0).toInt().coerceAtLeast(8),
                                    neuralDecision = NeuralDecision(
                                        if (alert.decision == "ACCEPT") "accept" else "decline",
                                        alert.voiceAlert,
                                        0.96
                                    )
                                )
                                onAcceptOffer(radarOffer)
                                isDismissed = true
                                RadarFirebaseMessagingService.clearLatestOffer()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1.5f)
                                .height(44.dp)
                                .testTag("fcm_accept_offer_button")
                        ) {
                            Text(
                                text = "Aceitar Corrida",
                                color = Color.Black,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * FcmStatusDiagnosticsCard
 *
 * Card informativo e ferramenta de teste para validar a conectividade do FCM com a VPS Hostinger.
 */
@Composable
fun FcmStatusDiagnosticsCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val fcmToken by RadarFirebaseMessagingService.fcmTokenState.collectAsState()
    val syncState by RadarBackendSyncManager.syncState.collectAsState()
    val ttsEngine = remember { OfferTextToSpeechEngine.getInstance(context) }
    val ttsState by ttsEngine.engineState.collectAsState()
    var isTesting by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCardElevated),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
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
                    Text(text = "🔥", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "FCM Cloud Messaging & TTS Audio",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (fcmToken.isNotEmpty()) NeonGreen.copy(alpha = 0.2f) else Color(0xFFFFB800).copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (fcmToken.isNotEmpty()) "TOKEN ATIVO" else "OBTENDO...",
                        color = if (fcmToken.isNotEmpty()) NeonGreen else Color(0xFFFFB800),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            val displayToken = if (fcmToken.isNotEmpty()) {
                "${fcmToken.take(12)}...${fcmToken.takeLast(8)}"
            } else {
                "Registrando token com os servidores do Firebase..."
            }

            Text(
                text = "Token: $displayToken",
                color = TextGray,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Linha de status do TTS nativo do Android
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1D1D2C))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🎧", fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "TTS Android: ${if (ttsState.isReady) "Pronto (pt-BR)" else "Inicializando..."}",
                        color = if (ttsState.isReady) NeonGreen else Color(0xFFFFB800),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Text(
                    text = "Velocidade: ${String.format(Locale.US, "%.2fx", ttsState.speechRate)}",
                    color = TextLight,
                    fontSize = 10.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Servidor: ${syncState.serverUrl}",
                    color = TextLight,
                    fontSize = 10.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Botão para testar áudio TTS
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF2C2436))
                            .border(1.dp, Color(0xFF4A3B54), RoundedCornerShape(8.dp))
                            .clickable {
                                ttsEngine.speak(
                                    "Radar ativo. Motor Text to Speech do Android pronto para converter ofertas recebidas via FCM em áudio no capacete.",
                                    android.speech.tts.TextToSpeech.QUEUE_FLUSH
                                )
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .testTag("test_tts_audio_button")
                    ) {
                        Text(
                            text = "🔊 Testar Voz",
                            color = Color(0xFFE289F2),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Botão de simulação de push para validação imediata do condutor
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF242436))
                            .border(1.dp, Color(0xFF3B3B54), RoundedCornerShape(8.dp))
                            .clickable {
                                isTesting = true
                                RadarFirebaseMessagingService.simulateIncomingOffer(
                                    context = context,
                                    appName = "iFood",
                                    restaurant = "McDonald's Paulista",
                                    value = 26.80,
                                    distanceKm = 3.6
                                )
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("simulate_fcm_push_button")
                    ) {
                        Text(
                            text = "⚡ Simular Push FCM",
                            color = Color(0xFFFFB800),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
