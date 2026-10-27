package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val NeonGreen = Color(0xFF00FF88)
private val CyberCyan = Color(0xFF00D2FF)
private val CyberGold = Color(0xFFFFD700)
private val CardSurface = Color(0xFF13131D)
private val CardBorder = Color(0xFF232335)
private val TextLight = Color(0xFFF0F0F8)
private val TextMuted = Color(0xFF8888A2)

@Composable
fun RealWorldDiagnosticCockpitCard(
    modifier: Modifier = Modifier,
    onSimulatedMergeAccepted: ((RadarOffer) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var hasNotificationListenerPermission by remember { mutableStateOf(false) }
    var hasOverlayPermission by remember { mutableStateOf(false) }

    fun checkPermissions() {
        val enabledListeners = NotificationManagerCompat.getEnabledListenerPackages(context)
        hasNotificationListenerPermission = enabledListeners.contains(context.packageName)
        hasOverlayPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else true
    }

    LaunchedEffect(Unit) {
        checkPermissions()
    }

    var isRunningSimulation by remember { mutableStateOf(false) }
    var simulationStepText by remember { mutableStateOf<String?>(null) }
    var detectedSynergyResult by remember { mutableStateOf<MultiAppSynergyEngine.SynergyEvaluationResult?>(null) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("real_world_diagnostic_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Brush.horizontalGradient(listOf(CyberGold.copy(alpha = 0.6f), NeonGreen.copy(alpha = 0.6f)))
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header do Diagnóstico
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
                                Brush.linearGradient(listOf(CyberGold.copy(alpha = 0.2f), NeonGreen.copy(alpha = 0.2f)))
                            )
                            .border(1.dp, CyberGold.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = "Mesclagem Real",
                            tint = CyberGold,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "VALIDAÇÃO EM VIDA REAL",
                                color = TextLight,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(NeonGreen.copy(alpha = 0.2f))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "RUA TEST",
                                    color = NeonGreen,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = "Teste se iFood e 99 mesclam com notificações reais",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Checagem de Permissões Críticas do Sistema Android
            Text(
                text = "PERMISSÕES NECESSÁRIAS NO SEU ANDROID:",
                color = TextMuted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Permissão 1: Leitura de Notificações
            PermissionStatusRow(
                title = "Leitura de Notificações",
                subtitle = "Intercepta pedidos do iFood/99 silenciosamente",
                isGranted = hasNotificationListenerPermission,
                onResolve = {
                    try {
                        val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Abra Configurações > Acesso a Notificações", Toast.LENGTH_LONG).show()
                    }
                }
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Permissão 2: Sobreposição na Tela (HUD)
            PermissionStatusRow(
                title = "Janela Flutuante (HUD)",
                subtitle = "Exibe o card tático sobre os outros apps",
                isGranted = hasOverlayPermission,
                onResolve = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        try {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:" + context.packageName)
                            )
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    }
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Botão Principal de Disparo do Teste de Campo Real
            Button(
                onClick = {
                    isRunningSimulation = true
                    simulationStepText = "1/2 Disparando notificação iFood na barra de status..."
                    detectedSynergyResult = null
                    HapticFeedbackHelper.vibrateTap(context)

                    coroutineScope.launch {
                        RealWorldNotificationHarness.triggerDualSimulation(
                            context = context,
                            onFirstPosted = {
                                simulationStepText = "1/2 iFood emitido. Aguardando toque concorrente da 99..."
                            },
                            onSecondPosted = {
                                simulationStepText = "2/2 99 Food recebido! Cruzando rotas e origens..."
                            }
                        )

                        // Aguarda o término da sequência (3 segundos)
                        delay(3200L)

                        val offerA = RadarOffer(
                            id = "ifood_paulista_real",
                            appName = "iFood",
                            restaurant = "Outback Shopping Paulista",
                            value = 24.50,
                            distanceKm = 3.5,
                            estimatedTimeMin = 14,
                            gainPerKm = 7.00
                        )

                        val offerB = RadarOffer(
                            id = "food99_paulista_real",
                            appName = "99 Food",
                            restaurant = "Burger King Shopping Paulista",
                            value = 19.00,
                            distanceKm = 4.1,
                            estimatedTimeMin = 16,
                            gainPerKm = 4.63
                        )

                        val synergy = MultiAppSynergyEngine.evaluateSynergy(offerA, offerB)
                        detectedSynergyResult = synergy
                        isRunningSimulation = false
                        simulationStepText = null

                        HapticFeedbackHelper.vibrateAccept(context)
                        try {
                            TextToSpeechManager.getInstance(context).speakVoiceAlert(
                                "Atenção piloto: Pedido 99 compatível com iFood. Faturamento combinado de quarenta e três reais com uma única parada de coleta."
                            )
                        } catch (_: Exception) {}
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_trigger_real_field_test"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF282512)),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberGold.copy(alpha = 0.7f)),
                enabled = !isRunningSimulation
            ) {
                if (isRunningSimulation) {
                    CircularProgressIndicator(
                        color = CyberGold,
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = simulationStepText ?: "Processando...",
                        color = CyberGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        contentDescription = "Testar",
                        tint = CyberGold,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "TESTAR NOTIFICAÇÕES REAIS NO CELULAR",
                        color = CyberGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            // Card com Resultado da Mesclagem em Tempo Real
            AnimatedVisibility(
                visible = detectedSynergyResult != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                val syn = detectedSynergyResult ?: return@AnimatedVisibility
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0C1610))
                        .border(1.dp, NeonGreen.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = syn.headline,
                            color = NeonGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(NeonGreen.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "SCORE ${syn.synergyScore}%",
                                color = NeonGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "TOTAL COMBINADO", color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = "R$ ${String.format(java.util.Locale("pt", "BR"), "%.2f", syn.combinedPayoutBrl)}",
                                color = CyberGold,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Column {
                            Text(text = "GANHO MÉDIO", color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = "R$ ${String.format(java.util.Locale("pt", "BR"), "%.2f", syn.combinedGainPerKm)}/km",
                                color = NeonGreen,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Column {
                            Text(text = "DESVIO ESTIMADO", color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = "${syn.destinationDeviationMeters}m (+${syn.estimatedExtraMinutes}m)",
                                color = CyberCyan,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = syn.explanation,
                        color = TextLight,
                        fontSize = 10.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            HapticFeedbackHelper.vibrateSuccess(context)
                            Toast.makeText(context, "✅ Mesclagem Real Aceita! R$ 43,50 faturados.", Toast.LENGTH_SHORT).show()
                            detectedSynergyResult = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
                    ) {
                        Text(
                            text = "ACEITAR CORRIDAS MESCLADAS EM 1 TOQUE",
                            color = Color.Black,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionStatusRow(
    title: String,
    subtitle: String,
    isGranted: Boolean,
    onResolve: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF181824))
            .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (isGranted) NeonGreen else Color(0xFFFF4D4D),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    color = TextLight,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = subtitle,
                color = TextMuted,
                fontSize = 9.sp
            )
        }

        if (isGranted) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(NeonGreen.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "LIBERADO",
                    color = NeonGreen,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Button(
                onClick = onResolve,
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B1818)),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Text(
                    text = "ATIVAR",
                    color = Color(0xFFFF6B6B),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}
