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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeUp
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
 * Card de Gestão de Notificações Prioritárias e Alertas Sonoros do Radar Coordinator.
 * Incorpora o sistema de sobreposição acústica (Android Audio Focus Ducking & STREAM_ALARM)
 * para garantir que os toques de alerta se sobreponham a músicas (Spotify, etc.) ou navegação GPS.
 */
@OptIn(ExperimentalLayoutApi::class)
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
    var audioOverlayMode by remember {
        mutableStateOf(CustomSoundPlayer.getAudioOverlayMode(context))
    }
    var isVolumeBoost by remember {
        mutableStateOf(CustomSoundPlayer.isVolumeBoostEnabled(context))
    }
    var isDistinctSoundsActive by remember {
        mutableStateOf(CustomSoundPlayer.isDistinctSoundsEnabled(context))
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
            // Cabeçalho Principal
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
                            text = "NOTIFICAÇÕES & ALERTAS SONOROS PRIORITÁRIOS",
                            color = TextLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = if (hasNotificationPermission) "Sobreposição Ativa sobre Músicas e GPS Waze" else "Permissão de Notificação Pendente",
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
                        text = if (hasNotificationPermission) "AUDIO FOCUS OK" else "PENDENTE",
                        color = if (hasNotificationPermission) NeonGreen else Color(0xFFE5A000),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Quando uma nova oferta lucrativa é interceptada, o Jarvis usa as APIs de áudio nativas do Android para atenuar músicas (Spotify, etc.) ou orientações de GPS (Waze/Maps), garantindo que o alerta seja ouvido mesmo com vento, capacete ou trânsito intenso.",
                color = TextMuted,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // =========================================================================
            // SEÇÃO 1: SOBREPOSIÇÃO DE ÁUDIO & GPS (ANDROID AUDIO FOCUS)
            // =========================================================================
            Surface(
                color = Color(0xFF0F1E19),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Headphones,
                                contentDescription = "Sobreposição de Áudio",
                                tint = NeonGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "SOBREPOSIÇÃO ACÚSTICA (AUDIO FOCUS)",
                                color = NeonGreen,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Text(
                            text = audioOverlayMode.subtitle,
                            color = TextLight,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = audioOverlayMode.description,
                        color = TextMuted,
                        fontSize = 10.sp,
                        lineHeight = 13.5.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Seletor de Modo de Sobreposição
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AudioOverlayPriorityMode.entries.forEach { mode ->
                            val isSelected = mode == audioOverlayMode
                            Surface(
                                color = if (isSelected) NeonGreen.copy(alpha = 0.2f) else DarkCardElevated,
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) NeonGreen else DarkBorder
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        audioOverlayMode = mode
                                        CustomSoundPlayer.setAudioOverlayMode(context, mode)
                                        Toast.makeText(context, "Modo: ${mode.title}", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(vertical = 6.dp, horizontal = 4.dp)
                                    .testTag("mode_${mode.id}")
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = when (mode) {
                                            AudioOverlayPriorityMode.DUCKING -> "🎧 Ducking 80%"
                                            AudioOverlayPriorityMode.EXCLUSIVE_ALARM -> "🚨 Alarme Puro"
                                            AudioOverlayPriorityMode.STANDARD_MIX -> "🎵 Mixagem"
                                        },
                                        color = if (isSelected) NeonGreen else TextLight,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Toggles: Volume Boost & Alertas Distintos
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Ganho de Volume",
                                tint = if (isVolumeBoost) NeonGreen else TextMuted,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "Volume Máximo de Alarme",
                                    color = TextLight,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Força 100% de ganho durante o toque",
                                    color = TextMuted,
                                    fontSize = 9.sp
                                )
                            }
                        }

                        Switch(
                            checked = isVolumeBoost,
                            onCheckedChange = { checked ->
                                isVolumeBoost = checked
                                CustomSoundPlayer.setVolumeBoostEnabled(context, checked)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = DarkBg,
                                checkedTrackColor = NeonGreen,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = DarkCardElevated
                            ),
                            modifier = Modifier.testTag("switch_volume_boost")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // =========================================================================
            // SEÇÃO 2: SELETOR DE SONS DISTINTOS PARA NOVAS OFERTAS
            // =========================================================================
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
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = "Som",
                                tint = NeonGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "ASSINATURA SONORA DA OFERTA:",
                                    color = TextMuted,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = selectedSoundType.title,
                                    color = NeonGreen,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Botão Pré-escuta com Ducking
                            OutlinedButton(
                                onClick = {
                                    CustomSoundPlayer.previewSound(context, selectedSoundType, simulateDucking = true)
                                    Toast.makeText(context, "Tocando: ${selectedSoundType.title} (com Ducking)", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .height(32.dp)
                                    .testTag("btn_preview_sound")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Ouvir",
                                    tint = NeonGreen,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Ouvir",
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
                                modifier = Modifier
                                    .height(32.dp)
                                    .testTag("btn_toggle_sound_list")
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

                    // Lista Expansível de Sons Distintos
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
                            Text(
                                text = "Escolha um som fixo ou deixe na Inteligência Sonora Automática:",
                                color = TextMuted,
                                fontSize = 9.5.sp,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )

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
                                            CustomSoundPlayer.previewSound(context, soundOption, simulateDucking = true)
                                            showSoundSelector = false
                                        }
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = soundOption.title,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) NeonGreen else TextLight
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(if (isSelected) NeonGreen.copy(alpha = 0.2f) else DarkCard)
                                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                                            ) {
                                                Text(
                                                    text = soundOption.badge,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) NeonGreen else TextMuted
                                                )
                                            }
                                        }
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

            Spacer(modifier = Modifier.height(10.dp))

            // =========================================================================
            // SEÇÃO 3: AUDIÇÃO RÁPIDA DOS 4 PERFIS DE CORRIDAS (TESTE ACÚSTICO)
            // =========================================================================
            Text(
                text = "TESTE RÁPIDO DOS PERFIS DE OFERTA COM DUCKING:",
                color = TextMuted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(5.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Perfil 1: VIP
                OutlinedButton(
                    onClick = {
                        CustomSoundPlayer.previewSound(context, NotificationSoundType.VIP_FANFARE, simulateDucking = true)
                        Toast.makeText(context, "Testando: Fanfarra VIP (> R$ 32)", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp).testTag("btn_audition_vip")
                ) {
                    Text("👑 VIP (> R$32)", fontSize = 9.5.sp, color = Color(0xFFFFD700), fontWeight = FontWeight.Bold)
                }

                // Perfil 2: Alta Margem
                OutlinedButton(
                    onClick = {
                        CustomSoundPlayer.previewSound(context, NotificationSoundType.URGENT_SONAR, simulateDucking = true)
                        Toast.makeText(context, "Testando: Sonar Alta Margem (> R$ 3,80/km)", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp).testTag("btn_audition_urgent")
                ) {
                    Text("⚡ Sonar Duplo (> R$3.8/km)", fontSize = 9.5.sp, color = Color(0xFFFF9900), fontWeight = FontWeight.Bold)
                }

                // Perfil 3: Sprint Turbo
                OutlinedButton(
                    onClick = {
                        CustomSoundPlayer.previewSound(context, NotificationSoundType.TURBO_SPRINT, simulateDucking = true)
                        Toast.makeText(context, "Testando: Turbo Sprint (< 3.5 km)", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp).testTag("btn_audition_turbo")
                ) {
                    Text("🚀 Turbo (< 3.5km)", fontSize = 9.5.sp, color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
                }

                // Perfil 4: Anti-Ruído
                OutlinedButton(
                    onClick = {
                        CustomSoundPlayer.previewSound(context, NotificationSoundType.TACTICAL_HORN, simulateDucking = true)
                        Toast.makeText(context, "Testando: Sirene Anti-Ruído 2.2 kHz", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp).testTag("btn_audition_horn")
                ) {
                    Text("🛡️ Anti-Ruído", fontSize = 9.5.sp, color = Color(0xFFFF0055), fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

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
                            text = "Notificação com sobreposição sonoro em $countdownRemaining s... Abra o Spotify ou Waze para testar o ducking!",
                            color = TextLight,
                            fontSize = 10.5.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Linha de Ações de Teste Real
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
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Testar",
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
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
                                "Pressione HOME! O som sobreporá qualquer app em 3 segundos.",
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
