package com.example

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

// Paleta de Cores para o Modo de Emergência SOS
private val SosRedPrimary = Color(0xFFFF2238)
private val SosRedDark = Color(0xFF8B0010)
private val SosRedContainer = Color(0xFF2E080C)
private val SosRedBorder = Color(0xFFFF334B)
private val SosSafeGreen = Color(0xFF00E676)
private val SosWarningOrange = Color(0xFFFF9100)

/**
 * EmergencySosCard
 *
 * Componente principal da Tela Principal que oferece:
 * 1. Botão de SOS de Alta Visibilidade e Contraste ("sos_emergency_button")
 * 2. Transmissão imediata de coordenadas GPS em tempo real aos contatos de confiança
 * 3. Disparo do Alerta Silencioso via WorkManager para máxima resiliência mesmo com o app em segundo plano
 * 4. Contagem regressiva tátil de 5 segundos contra toques acidentais no guidão
 * 5. Atalhos rápidos para WhatsApp e SMS direto aos contatos
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EmergencySosCard(
    onOpenManageContacts: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sosState by EmergencySosManager.sosState.collectAsState()
    val contacts by EmergencySosManager.emergencyContacts.collectAsState()
    val locationState by LocationService.globalLocationState.collectAsState()

    // Animação de pulso para indicar monitoramento e urgência
    val infiniteTransition = rememberInfiniteTransition(label = "SosPulseTransition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SosPulseScale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("sos_emergency_card")
            .border(
                width = if (sosState is EmergencySosState.Active) 2.dp else 1.dp,
                color = if (sosState is EmergencySosState.Active) SosRedBorder else Color(0xFF441218),
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = SosRedContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Cabeçalho com Ícone de Alerta e Tag de Proteção Ativa
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SosRedDark)
                            .border(1.dp, SosRedBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Proteção de Emergência SOS",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "SISTEMA DE EMERGÊNCIA SOS",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(SosRedDark)
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "SILENCIOSO",
                                    color = Color(0xFFFF8896),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = "WorkManager Resiliente • GPS em Tempo Real",
                            color = Color(0xFFE0A0A8),
                            fontSize = 11.sp
                        )
                    }
                }

                // Botão de Gestão de Contatos
                Surface(
                    onClick = onOpenManageContacts,
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF4A1016),
                    modifier = Modifier.testTag("sos_manage_contacts_button")
                ) {
                    Text(
                        text = "⚙️ Contatos",
                        color = Color(0xFFFFD5DA),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Badge de Telemetria GPS em Tempo Real
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF1F0508)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "GPS",
                            tint = SosSafeGreen,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = String.format(Locale.US, "GPS: %.4f, %.4f", locationState.latitude, locationState.longitude),
                            color = Color(0xFFF0E0E2),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Velocidade",
                            tint = Color(0xFFFFD54F),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = String.format(Locale.US, "%.1f km/h", locationState.currentSpeedKmh),
                            color = Color(0xFFFFD54F),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tratamento Dinâmico por Estado da Máquina de SOS
            when (val state = sosState) {
                is EmergencySosState.Idle -> {
                    // Botão Principal de Disparo Instantâneo de SOS
                    Button(
                        onClick = {
                            EmergencySosManager.startSosCountdown(context)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .scale(pulseScale)
                            .testTag("sos_emergency_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SosRedPrimary,
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(text = "🚨", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ACIONAR SOS SILENCIOSO",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.8.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Dispara beacon silencioso via WorkManager e envia localização GPS aos contatos com contagem de 5s para evitar erros.",
                        color = Color(0xFFDCA8B0),
                        fontSize = 10.5.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                is EmergencySosState.Countdown -> {
                    // Visual de Contagem Regressiva (Cancelável)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF4A0A12))
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "DISPARANDO SOS EM",
                            color = Color(0xFFFFCDD2),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${state.secondsRemaining}s",
                            color = Color.White,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Black
                        )

                        LinearProgressIndicator(
                            progress = { (state.secondsRemaining / 5.0f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = SosRedPrimary,
                            trackColor = Color(0xFF2E080C)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    EmergencySosManager.cancelSosCountdown(context)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("sos_cancel_countdown_button"),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFFFFCDD2)
                                )
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Cancelar", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Cancelar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    EmergencySosManager.triggerSosImmediately(context)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("sos_trigger_now_button"),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SosRedPrimary,
                                    contentColor = Color.White
                                )
                            ) {
                                Text("Enviar Já ⚡", fontSize = 12.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }

                is EmergencySosState.Active -> {
                    // Estado de Emergência Ativa / Beacon Silencioso Disparado
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF38080E))
                            .border(1.dp, SosRedBorder, RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(SosRedPrimary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ALERTA SILENCIOSO DISPARADO VIA WORKMANAGER",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Aviso emitido silenciosamente. ID da Tarefa: ${state.workManagerId.take(8)}...",
                            color = Color(0xFFFFB3BC),
                            fontSize = 11.sp
                        )
                        Text(
                            text = "Link gerado: ${state.mapsUrl}",
                            color = Color(0xFF80D8FF),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Ações Rápidas de Disparo aos Contatos
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    EmergencySosManager.shareDistressLocation(context)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("sos_share_active_link_button"),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1E88E5),
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Compartilhar", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Compartilhar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    EmergencySosManager.resolveSos(context)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("sos_resolve_button"),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SosSafeGreen,
                                    contentColor = Color(0xFF003314)
                                )
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Estou Seguro", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Estou Seguro ✅", fontSize = 11.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }

                is EmergencySosState.Resolved -> {
                    // Estado de Conclusão / Seguro
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF002910))
                            .border(1.dp, SosSafeGreen, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "✅ Ocorrência Finalizada - Entregador em Segurança",
                            color = SosSafeGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = {
                                EmergencySosManager.resetToIdle()
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Text("Retornar ao Modo Normal", fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Lista Compacta de Contatos de Emergência Cadastrados
            if (contacts.isNotEmpty()) {
                Text(
                    text = "CONTATOS VINCULADOS (${contacts.size}):",
                    color = Color(0xFFFFCDD2),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    contacts.forEach { contact ->
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF38080E),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF5A141D)),
                            modifier = Modifier.clickable {
                                // Abre WhatsApp diretamente ao tocar no contato
                                EmergencySosManager.sendSosViaWhatsApp(context, contact)
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (contact.isWhatsAppEnabled) "💬 " else "📞 ",
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = contact.name,
                                    color = Color(0xFFFBE4E7),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "(${contact.relationship})",
                                    color = Color(0xFFB87882),
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
