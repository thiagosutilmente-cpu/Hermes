package com.example

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * MotoboyCockpitHeroCard
 *
 * Super Painel de Bordo Unificado do Motoboy (Piloto de Pista 360°).
 * Unifica os 5 pilares essenciais de sobrevivência e lucro na rua:
 * 1. Filtro de Corridas & Auto-Aceite por R$/km no fone de ouvido
 * 2. HUD Flutuante no Guidão & Modo Economia OLED de Bateria
 * 3. Radar de Velocidade, Chuva e Blitz Policial Colaborativa
 * 4. SOS de Queda e Pânico (Shake Gesture)
 * 5. Fechamento de Féria & Cálculo Real de Gasolina
 */
@Composable
fun MotoboyCockpitHeroCard(
    modifier: Modifier = Modifier,
    isNightMode: Boolean = true,
    onToggleNightMode: () -> Unit = {},
    voiceManager: NeuralVoiceManager? = null,
    onOpenFilterSettings: () -> Unit = {},
    onOpenEmergencyDialog: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 1. Gerenciadores Reativos do App
    val autoAcceptManager = remember { UltraProfitAutoAcceptManager.getInstance(context) }
    val autoAcceptCriteria by autoAcceptManager.criteria.collectAsState()

    val blitzManager = remember { CrowdsourcedBlitzRadarManager.getInstance(context) }
    val closestHazard by blitzManager.closestHazard.collectAsState()
    val activeHazards by blitzManager.activeHazards.collectAsState()

    val sosManager = remember { EmergencySosManager.getInstance(context) }
    val sosState by EmergencySosManager.sosState.collectAsState()

    val goalManager = remember { DailyGoalTrackerManager.getInstance(context) }
    val goalStatus by goalManager.goalStatus.collectAsState()

    // Estado da bolha flutuante
    var isOverlayActive by remember {
        mutableStateOf(OverlayWindowManager.getInstance(context).isShowing())
    }

    // Cores táticas de alto contraste
    val neonGreen = Color(0xFF22C55E)
    val neonYellow = Color(0xFFFACC15)
    val neonOrange = Color(0xFFF97316)
    val alertRed = Color(0xFFEF4444)
    val cyanBlue = Color(0xFF38BDF8)
    val cardBackground = if (isNightMode) Color(0xFF0B1120) else Color(0xFF1E293B)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("motoboy_cockpit_hero_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        border = BorderStroke(
            width = 1.5.dp,
            brush = Brush.horizontalGradient(
                colors = listOf(neonGreen, cyanBlue, neonYellow)
            )
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // =================================================================
            // TOPO: STATUS DO PILOTO & VELOCIDADE GPS
            // =================================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(neonGreen, Color(0xFF15803D))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TwoWheeler,
                            contentDescription = "Piloto de Pista",
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "PILOTO DE PISTA 360°",
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            letterSpacing = 0.8.sp,
                            color = Color.White
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(neonGreen)
                            )
                            Text(
                                text = "RADAR INTELIGENTE ATIVO",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = neonGreen
                            )
                        }
                    }
                }

                // Botão de Alternar Modo OLED / Sol
                Surface(
                    onClick = {
                        onToggleNightMode()
                        HapticFeedbackHelper.vibrateTap(context)
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E293B),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.testTag("btn_hero_oled_toggle")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (isNightMode) "🌙 OLED" else "☀️ SOL",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isNightMode) cyanBlue else neonYellow
                        )
                    }
                }
            }

            HorizontalDivider(color = Color(0xFF334155).copy(alpha = 0.6f))

            // =================================================================
            // PILAR 1: FILTRO DE LUCRO & AUTO-ACEITE (R$/KM NO FONE)
            // =================================================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF131D31))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterAlt,
                            contentDescription = null,
                            tint = neonYellow,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Filtro de Lucro no Fone (R$/km)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Switch(
                        checked = autoAcceptCriteria.isEnabled,
                        onCheckedChange = { enabled ->
                            autoAcceptManager.toggleEnabled(enabled)
                            HapticFeedbackHelper.vibrateTap(context)
                            if (enabled) {
                                voiceManager?.speak("Auto-aceite ativado com piso de ${String.format(Locale("pt", "BR"), "R$ %.2f", autoAcceptCriteria.minGainPerKm)} por quilômetro.")
                            } else {
                                voiceManager?.speak("Auto-aceite desativado.")
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = neonGreen
                        ),
                        modifier = Modifier.testTag("switch_hero_auto_accept")
                    )
                }

                Text(
                    text = "Só toca no fone e aceita se pagar no mínimo:",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )

                // Seletores Rápidos de Piso R$/km
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val thresholds = listOf(3.50, 4.50, 6.00)
                    thresholds.forEach { thresh ->
                        val isSelected = (autoAcceptCriteria.minGainPerKm == thresh)
                        Surface(
                            onClick = {
                                autoAcceptManager.updateMinGainPerKm(thresh)
                                HapticFeedbackHelper.vibrateTap(context)
                                voiceManager?.speak("Piso alterado para ${String.format(Locale("pt", "BR"), "R$ %.2f", thresh)} por quilômetro.")
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) neonGreen.copy(alpha = 0.2f) else Color(0xFF1E293B),
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) neonGreen else Color(0xFF475569)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "R$ %.2f/km".format(thresh),
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                    color = if (isSelected) neonGreen else Color(0xFFCBD5E1)
                                )
                            }
                        }
                    }
                }
            }

            // =================================================================
            // PILAR 2: HUD FLUTUANTE & BOTÃO DE PÂNICO SOS (SHAKE GESTURE)
            // =================================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Botão de Ativar Bolha HUD sobre iFood/99/Uber
                Button(
                    onClick = {
                        if (!OverlayWindowManager.canDrawOverlays(context)) {
                            Toast.makeText(context, "Conceda permissão de sobreposição para o HUD", Toast.LENGTH_LONG).show()
                            OverlayWindowManager.requestOverlayPermission(context)
                        } else {
                            val overlay = OverlayWindowManager.getInstance(context)
                            if (overlay.isShowing()) {
                                overlay.hide()
                                isOverlayActive = false
                                Toast.makeText(context, "HUD Flutuante ocultado", Toast.LENGTH_SHORT).show()
                            } else {
                                overlay.show()
                                isOverlayActive = true
                                Toast.makeText(context, "HUD ativo sobre outros apps!", Toast.LENGTH_SHORT).show()
                                voiceManager?.speak("Painel flutuante ativo no guidão.")
                            }
                        }
                        HapticFeedbackHelper.vibrateTap(context)
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isOverlayActive) cyanBlue else Color(0xFF1E293B),
                        contentColor = if (isOverlayActive) Color.Black else Color.White
                    ),
                    border = BorderStroke(1.dp, cyanBlue),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_hero_toggle_hud")
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isOverlayActive) "HUD Ativo" else "Ligar HUD",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                // Botão de SOS Queda / Emergência Silenciosa
                Button(
                    onClick = {
                        onOpenEmergencyDialog()
                        HapticFeedbackHelper.vibrateDecline(context)
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = alertRed,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_hero_sos_emergency")
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SOS Queda",
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp
                    )
                }
            }

            // =================================================================
            // PILAR 3: RADAR DE BLITZ, CHUVA E RADAR DE VELOCIDADE
            // =================================================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF131D31))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = "🚨", fontSize = 16.sp)
                        Text(
                            text = "Radar de Blitz & Clima na Região",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF1E293B)
                    ) {
                        Text(
                            text = "${activeHazards.size} alertas",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Exibição do Alerta Mais Próximo
                if (closestHazard != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFEF4444).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = closestHazard!!.type.icon, fontSize = 20.sp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = closestHazard!!.title,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "${closestHazard!!.address} (${closestHazard!!.distanceMeters}m de você)",
                                    fontSize = 11.sp,
                                    color = Color(0xFFFCA5A5)
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = "Nenhuma blitz ou alagamento detectado num raio de 1.5 km.",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                // Botões Rápidos de Reporte Comunitário (Colaborativo)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            blitzManager.reportHazard(
                                type = HazardType.POLICE_BLITZ,
                                title = "Nova Blitz Reportada",
                                description = "Fiscalização com viaturas parando motocicletas",
                                address = "Ponto Atual GPS",
                                latitude = 0.0,
                                longitude = 0.0
                            )
                            HapticFeedbackHelper.vibrateSuccess(context)
                            Toast.makeText(context, "Blitz registrada! Alerta enviado para os motoboys da região.", Toast.LENGTH_SHORT).show()
                            voiceManager?.speak("Blitz reportada com sucesso. Obrigado parceiro!")
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = "🚔 Reportar Blitz", fontSize = 11.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            blitzManager.reportHazard(
                                type = HazardType.HEAVY_RAIN,
                                title = "Chuva / Alagamento",
                                description = "Pista escorregadia com água acumulada",
                                address = "Ponto Atual GPS",
                                latitude = 0.0,
                                longitude = 0.0
                            )
                            HapticFeedbackHelper.vibrateSuccess(context)
                            Toast.makeText(context, "Alerta de chuva compartilhado!", Toast.LENGTH_SHORT).show()
                            voiceManager?.speak("Ponto de alagamento registrado.")
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFEAB308)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = "🌧️ Reportar Chuva", fontSize = 11.sp, color = Color(0xFFEAB308), fontWeight = FontWeight.Bold)
                    }
                }
            }

            // =================================================================
            // PILAR 5: TERMÔMETRO DA FÉRIA DO DIA & GASOLINA
            // =================================================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF131D31))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Féria do Dia (Lucro Líquido no Bolso)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    val gross = goalStatus.currentEarnings
                    val net = if (gross > 0) gross * 0.78 else 0.0
                    Text(
                        text = "R$ %.2f limpos".format(net),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = neonGreen
                    )
                }

                LinearProgressIndicator(
                    progress = { (goalStatus.progressPercent / 100.0).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = neonGreen,
                    trackColor = Color(0xFF334155)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Meta: R$ %.2f (%.0f%%)".format(goalStatus.targetEarnings, goalStatus.progressPercent),
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = if (goalStatus.remainingEarnings > 0) "Faltam R$ %.2f".format(goalStatus.remainingEarnings) else "Meta Batida! 🏆",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (goalStatus.remainingEarnings > 0) neonYellow else neonGreen
                    )
                }
            }
        }
    }
}
