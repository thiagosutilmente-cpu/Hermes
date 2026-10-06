package com.example

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * ShakeEmergencyControlCard
 *
 * Card visual interativo para configurar e testar o detector de gestos de 'Shake' (chacoalhar o celular):
 * - Interruptor para ativar/desativar o monitoramento no acelerômetro
 * - Seletor da ação executada (Silenciar Áudio de Emergência / Re-processar Oferta)
 * - Botão de teste rápido para simular o gesto sem precisar balançar a moto
 * - Status em tempo real da última ação acionada
 */
@Composable
fun ShakeEmergencyControlCard(
    onTriggerSimulateShake: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isEnabled by ShakeEmergencyManager.isShakeEnabled.collectAsState()
    val selectedAction by ShakeEmergencyManager.selectedAction.collectAsState()
    val lastAction by ShakeEmergencyManager.lastActionExecuted.collectAsState()

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(18.dp))
            .testTag("card_shake_emergency_control")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // CABEÇALHO DO CARD
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (isEnabled) Color(0xFFF59E0B).copy(alpha = 0.2f)
                                else Color(0xFF64748B).copy(alpha = 0.2f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Vibration,
                            contentDescription = "Gesto Shake",
                            tint = if (isEnabled) Color(0xFFF59E0B) else Color(0xFF64748B),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "GESTO DE SHAKE (CHACOALHAR)",
                                color = Color.White,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Text(
                            text = "Silenciamento de emergência ou re-análise tática",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                }

                Switch(
                    checked = isEnabled,
                    onCheckedChange = { ShakeEmergencyManager.setShakeEnabled(context, it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFFF59E0B),
                        checkedTrackColor = Color(0xFF78350F),
                        uncheckedThumbColor = Color(0xFF94A3B8),
                        uncheckedTrackColor = Color(0xFF334155)
                    ),
                    modifier = Modifier.testTag("switch_shake_enabled")
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // SELEÇÃO DE AÇÃO PREFERENCIAL
            Text(
                text = "AÇÃO EXECUTADA AO CHACOALHAR:",
                color = Color(0xFF64748B),
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ShakeActionPreference.values().forEach { actionPref ->
                    val isSelected = selectedAction == actionPref
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) Color(0xFF1E293B) else Color(0xFF090D16))
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) Color(0xFFF59E0B) else Color(0xFF1E293B),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                ShakeEmergencyManager.setActionPreference(context, actionPref)
                            }
                            .padding(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = when (actionPref) {
                                    ShakeActionPreference.SILENCE_AUDIO_EMERGENCY -> "🔇"
                                    ShakeActionPreference.REPROCESS_LAST_OFFER -> "🔄"
                                    ShakeActionPreference.DUAL_ACTION -> "⚡"
                                },
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = actionPref.label,
                                    color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                                Text(
                                    text = actionPref.description,
                                    color = Color(0xFF64748B),
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                }
            }

            // ÚLTIMA AÇÃO EXECUTADA
            if (lastAction.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF064E3B).copy(alpha = 0.35f))
                        .border(1.dp, Color(0xFF059669).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Último disparo: $lastAction",
                        color = Color(0xFF34D399),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // BOTÃO DE SIMULAÇÃO RÁPIDA EM BANCADA
            Button(
                onClick = onTriggerSimulateShake,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD97706),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .testTag("btn_simulate_shake_gesture")
            ) {
                Icon(Icons.Default.Vibration, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("TESTAR DISPARO DE SHAKE AGORA", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
