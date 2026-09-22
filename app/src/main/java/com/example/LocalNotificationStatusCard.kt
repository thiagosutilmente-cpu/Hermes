package com.example

import android.content.Context
import android.widget.Toast
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import kotlinx.coroutines.delay

/**
 * Card de Gestão de Notificações em Segundo Plano com Sons Personalizados.
 * Permite ao piloto:
 * 1. Escolher o som personalizado do alerta (Sonar Radar, Alerta Urgente, Som do Sistema, Silencioso).
 * 2. Ouvir pré-escuta do áudio com volume máximo e resposta háptica.
 * 3. Testar a notificação imediatamente.
 * 4. Testar a notificação em segundo plano com contagem regressiva de 3 segundos para minimizar o app.
 */
@Composable
fun LocalNotificationStatusCard(
    hasNotificationPermission: Boolean = true,
    onRequestPermission: () -> Unit = {},
    onTestNotification: () -> Unit = {},
    onScheduleBackgroundTest: ((delaySeconds: Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedSoundType by remember {
        mutableStateOf(LocalNotificationManager.getSelectedSoundType(context))
    }
    var countdownRemaining by remember { mutableIntStateOf(0) }
    var showSoundSelector by remember { mutableStateOf(false) }

    // Efeito de contagem regressiva para o teste em segundo plano
    LaunchedEffect(countdownRemaining) {
        if (countdownRemaining > 0) {
            delay(1000L)
            countdownRemaining--
        }
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (hasNotificationPermission) NeonGreen.copy(alpha = 0.35f) else Color(0xFFE5A000).copy(alpha = 0.5f),
                shape = RoundedCornerShape(18.dp)
            )
            .testTag("local_notification_status_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Cabeçalho
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
                            .background(if (hasNotificationPermission) NeonGreen else Color(0xFFE5A000))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "NOTIFICAÇÕES PUSH EM 2º PLANO",
                            color = TextLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = if (hasNotificationPermission) "Alertas Heads-Up Ativos no Bolso/Waze" else "Permissão de Notificação Pendente",
                            color = if (hasNotificationPermission) NeonGreen else Color(0xFFE5A000),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (hasNotificationPermission) NeonGreen.copy(alpha = 0.15f) else Color(0xFFE5A000).copy(alpha = 0.2f))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (hasNotificationPermission) "SOM PERSONALIZADO" else "PENDENTE",
                        color = if (hasNotificationPermission) NeonGreen else Color(0xFFE5A000),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Quando você estiver usando o Waze, Google Maps ou com a tela bloqueada, o Radar Coordinator emitirá um banner flutuante de alta prioridade com o som personalizado escolhido e botões de Aceitar/Recusar sem precisar abrir o app.",
                color = TextMuted,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Seção de Seleção do Som Personalizado
            Surface(
                color = DarkCardElevated,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "🔊",
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "Toque de Alerta:",
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = selectedSoundType.title,
                                    color = NeonGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Botão Pré-escuta (Preview)
                            OutlinedButton(
                                onClick = {
                                    CustomSoundPlayer.previewSound(context, selectedSoundType)
                                    Toast.makeText(context, "Tocando: ${selectedSoundType.title}", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp).testTag("btn_preview_sound")
                            ) {
                                Text(
                                    text = "▶ Ouvir",
                                    fontSize = 10.sp,
                                    color = NeonGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Botão Alternar Som
                            OutlinedButton(
                                onClick = { showSoundSelector = !showSoundSelector },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp).testTag("btn_toggle_sound_list")
                            ) {
                                Text(
                                    text = if (showSoundSelector) "Fechar" else "Trocar ▾",
                                    fontSize = 10.sp,
                                    color = TextLight,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Lista Expansível de Sons
                    AnimatedVisibility(
                        visible = showSoundSelector,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            NotificationSoundType.entries.forEach { soundOption ->
                                val isSelected = soundOption == selectedSoundType
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) NeonGreen.copy(alpha = 0.12f) else Color.Transparent)
                                        .clickable {
                                            selectedSoundType = soundOption
                                            LocalNotificationManager.setSelectedSoundType(context, soundOption)
                                            CustomSoundPlayer.previewSound(context, soundOption)
                                            showSoundSelector = false
                                        }
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = soundOption.title,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) NeonGreen else TextLight
                                        )
                                        Text(
                                            text = soundOption.description,
                                            fontSize = 9.sp,
                                            color = TextMuted,
                                            lineHeight = 12.sp
                                        )
                                    }
                                    if (isSelected) {
                                        Text(
                                            text = "✓",
                                            color = NeonGreen,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Banner de contagem regressiva se ativada
            if (countdownRemaining > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF2A1C0E))
                        .border(1.dp, Color(0xFFFF9900), RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "⏳ MINIMIZE O APP AGORA!",
                            color = Color(0xFFFF9900),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Notificação com som em $countdownRemaining segundo(s)... Pressione o botão Home!",
                            color = TextLight,
                            fontSize = 10.5.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Linha de Ações de Teste
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!hasNotificationPermission) {
                    Button(
                        onClick = onRequestPermission,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE5A000),
                            contentColor = DarkBg
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("btn_request_notification_permission")
                    ) {
                        Text(
                            text = "🔔 Ativar Alertas",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    // Botão 1: Testar Imediato
                    Button(
                        onClick = onTestNotification,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DarkCardElevated,
                            contentColor = NeonGreen
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .border(1.dp, NeonGreen.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .testTag("btn_test_notification")
                    ) {
                        Text(
                            text = "⚡ Teste Imediato",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Botão 2: Testar em Segundo Plano (com delay de 3s para o piloto minimizar)
                    Button(
                        onClick = {
                            countdownRemaining = 3
                            Toast.makeText(
                                context,
                                "Pressione HOME! Notificação com som chegará em 3 segundos.",
                                Toast.LENGTH_LONG
                            ).show()
                            if (onScheduleBackgroundTest != null) {
                                onScheduleBackgroundTest(3)
                            } else {
                                val manager = LocalNotificationManager(context)
                                manager.scheduleDelayedBackgroundNotification(3)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1B3B2B),
                            contentColor = NeonGreen
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1.2f)
                            .height(42.dp)
                            .border(1.dp, NeonGreen, RoundedCornerShape(10.dp))
                            .testTag("btn_test_background_notification")
                    ) {
                        Text(
                            text = "📱 Testar no 2º Plano (3s)",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
