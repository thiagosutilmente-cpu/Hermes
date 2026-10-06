package com.example

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

/**
 * UltraProfitAutoAcceptCard
 *
 * Interface gráfica moderna e tática em Jetpack Compose que gerencia a resposta automática
 * para aceitação de corridas ultra-lucrativas no piloto automático.
 */
@Composable
fun UltraProfitAutoAcceptCard(
    autoAcceptManager: UltraProfitAutoAcceptManager,
    onAcceptOfferCallback: (RadarOffer, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val criteria by autoAcceptManager.criteria.collectAsState()
    val totalAccepted by autoAcceptManager.totalAcceptedCount.collectAsState()
    val totalProfit by autoAcceptManager.totalProfitGeneratedBrl.collectAsState()
    val recentRides by autoAcceptManager.recentAutoAcceptedRides.collectAsState()
    val pendingOffer by autoAcceptManager.pendingOffer.collectAsState()
    val countdownRemainingMs by autoAcceptManager.countdownRemainingMs.collectAsState()

    var isConfigExpanded by remember { mutableStateOf(false) }
    var isHistoryExpanded by remember { mutableStateOf(false) }

    // Pulso luminescente para o status do robô quando ativo
    val pulseTransition = rememberInfiniteTransition(label = "autoAcceptPulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("ultra_profit_auto_accept_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF090C10)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (criteria.isEnabled) Color(0xFF00FF88).copy(alpha = 0.7f) else Color(0xFF263238)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // =========================================================================
            // 1. CABEÇALHO DO CARD COM STATUS E SWITCH MESTRE
            // =========================================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (criteria.isEnabled) Color(0xFF00FF88).copy(alpha = 0.2f)
                                else Color(0xFF374151).copy(alpha = 0.3f)
                            )
                            .border(
                                1.dp,
                                if (criteria.isEnabled) Color(0xFF00FF88) else Color(0xFF4B5563),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlashOn,
                            contentDescription = "Auto-Accept",
                            tint = if (criteria.isEnabled) Color(0xFF00FF88) else Color(0xFF9CA3AF),
                            modifier = Modifier
                                .size(22.dp)
                                .then(if (criteria.isEnabled) Modifier.scale(pulseScale) else Modifier)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "AUTO-ACEITE ULTRA-LUCRO",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (criteria.isEnabled) Color(0xFF00FF88).copy(alpha = 0.15f)
                                        else Color(0xFF374151).copy(alpha = 0.4f)
                                    )
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (criteria.isEnabled) "ATIVO" else "PAUSADO",
                                    color = if (criteria.isEnabled) Color(0xFF00FF88) else Color(0xFF9CA3AF),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = if (criteria.isEnabled)
                                "Gatilho: ≥ R$ ${String.format(Locale("pt", "BR"), "%.2f", criteria.minGainPerKm)}/km • Máx ${String.format(Locale.ROOT, "%.1f", criteria.maxDistanceKm)}km"
                            else "Resposta automática desligada",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                }

                Switch(
                    checked = criteria.isEnabled,
                    onCheckedChange = { newState ->
                        autoAcceptManager.updateCriteria(criteria.copy(isEnabled = newState))
                        if (newState) {
                            HapticFeedbackHelper.vibrateAccept(context)
                            Toast.makeText(context, "⚡ Auto-Aceite ativado! Monitorando corridas ultra-lucrativas.", Toast.LENGTH_SHORT).show()
                        } else {
                            HapticFeedbackHelper.vibrateDecline(context)
                            autoAcceptManager.cancelPendingCountdown()
                            Toast.makeText(context, "Auto-Aceite pausado.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF000000),
                        checkedTrackColor = Color(0xFF00FF88),
                        uncheckedThumbColor = Color(0xFF9CA3AF),
                        uncheckedTrackColor = Color(0xFF1E293B)
                    ),
                    modifier = Modifier.testTag("switch_master_auto_accept")
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // =========================================================================
            // 2. PAINEL DE KPIS E LUCRO DO PILOTO AUTOMÁTICO
            // =========================================================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF111827))
                    .padding(vertical = 10.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "ACEITAS P/ IA",
                        color = Color(0xFF94A3B8),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$totalAccepted",
                        color = Color(0xFF00F0FF),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(30.dp)
                        .background(Color(0xFF374151))
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "LUCRO ACUMULADO",
                        color = Color(0xFF94A3B8),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = String.format(Locale("pt", "BR"), "R$ %.2f", totalProfit),
                        color = Color(0xFF00FF88),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(30.dp)
                        .background(Color(0xFF374151))
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "VELOCIDADE GPS",
                        color = Color(0xFF94A3B8),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = if (criteria.allowWhileMoving) Color(0xFF00FF88) else Color(0xFFFFB800),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (criteria.allowWhileMoving) "100% MÃOS-LIVRES" else "PARADO",
                            color = if (criteria.allowWhileMoving) Color(0xFF00FF88) else Color(0xFFFFB800),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // =========================================================================
            // 3. BANNER DE CONTAGEM REGRESSIVA (QUANDO OFERTA ULTRA-LUCRATIVA É DETECTADA)
            // =========================================================================
            AnimatedVisibility(
                visible = pendingOffer != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                pendingOffer?.let { offer ->
                    val totalDelayMs = criteria.reactionDelayMs.coerceAtLeast(100L)
                    val progress = ((totalDelayMs - countdownRemainingMs).toFloat() / totalDelayMs.toFloat()).coerceIn(0f, 1f)
                    val secondsLeft = String.format(Locale.ROOT, "%.1f", countdownRemainingMs / 1000f)

                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1400)),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFFB800))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.FlashOn,
                                        contentDescription = null,
                                        tint = Color(0xFFFFB800),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "ACEITANDO CORRIDA TOP EM ${secondsLeft}s",
                                        color = Color(0xFFFFB800),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Button(
                                    onClick = {
                                        autoAcceptManager.cancelPendingCountdown()
                                        HapticFeedbackHelper.vibrateDecline(context)
                                        Toast.makeText(context, "Aceite automático cancelado", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("CANCELAR", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${offer.restaurant} • ${offer.appName} • ${String.format(Locale("pt", "BR"), "R$ %.2f", offer.value)} (${String.format(Locale("pt", "BR"), "R$ %.2f", offer.gainPerKm)}/km)",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = progress,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = Color(0xFFFFB800),
                                trackColor = Color(0xFF374151)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // =========================================================================
            // 4. BOTÕES DE AÇÃO: CONFIGURAR CRITÉRIOS, TESTAR SIMULAÇÃO E HISTÓRICO
            // =========================================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val testOffer = autoAcceptManager.generateUltraProfitableTestOffer()
                        val success = autoAcceptManager.processIncomingOffer(
                            offer = testOffer,
                            isCurrentlyMoving = false,
                            onAcceptedCallback = onAcceptOfferCallback
                        )
                        if (success) {
                            Toast.makeText(
                                context,
                                "🚀 Corrida ultra-lucrativa simulada! Verifique o áudio e a aceitação.",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                context,
                                "Oferta gerada não passou nos filtros estritos atuais.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    modifier = Modifier
                        .weight(1.3f)
                        .height(38.dp)
                        .testTag("btn_test_ultra_profit_auto_accept"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00FF88)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "TESTAR DISPARO",
                        color = Color.Black,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Button(
                    onClick = { isConfigExpanded = !isConfigExpanded },
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("btn_toggle_auto_accept_config"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1F2937)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = Color(0xFF00F0FF),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isConfigExpanded) "FECHAR" else "CRITÉRIOS",
                        color = Color(0xFF00F0FF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = { isHistoryExpanded = !isHistoryExpanded },
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("btn_toggle_auto_accept_history"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1F2937)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = Color(0xFFE2E8F0),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "LOGS (${recentRides.size})",
                        color = Color(0xFFE2E8F0),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // =========================================================================
            // 5. PAINEL EXPANSÍVEL DE AJUSTE FINO DE CRITÉRIOS DE LUCRATIVIDADE
            // =========================================================================
            AnimatedVisibility(
                visible = isConfigExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF05070A))
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "PARÂMETROS DE ULTRA-LUCRO",
                            color = Color(0xFF00FF88),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Calibração Jarvis",
                            color = Color(0xFF64748B),
                            fontSize = 10.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 1. Ganho Mínimo por KM
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Ganho Mínimo por KM:", color = Color(0xFFCBD5E1), fontSize = 11.sp)
                        Text(
                            String.format(Locale("pt", "BR"), "R$ %.2f / km", criteria.minGainPerKm),
                            color = Color(0xFF00FF88),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                    Slider(
                        value = criteria.minGainPerKm.toFloat(),
                        onValueChange = { newValue ->
                            val rounded = (Math.round(newValue * 2) / 2.0)
                            autoAcceptManager.updateCriteria(criteria.copy(minGainPerKm = rounded))
                        },
                        valueRange = 3.0f..8.0f,
                        steps = 9,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF00FF88),
                            activeTrackColor = Color(0xFF00FF88),
                            inactiveTrackColor = Color(0xFF1E293B)
                        ),
                        modifier = Modifier.height(28.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 2. Valor Mínimo da Corrida
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Valor Mínimo Bruto:", color = Color(0xFFCBD5E1), fontSize = 11.sp)
                        Text(
                            String.format(Locale("pt", "BR"), "R$ %.2f", criteria.minValueBrl),
                            color = Color(0xFF00F0FF),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                    Slider(
                        value = criteria.minValueBrl.toFloat(),
                        onValueChange = { newValue ->
                            val rounded = (Math.round(newValue)).toDouble()
                            autoAcceptManager.updateCriteria(criteria.copy(minValueBrl = rounded))
                        },
                        valueRange = 10.0f..35.0f,
                        steps = 24,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF00F0FF),
                            activeTrackColor = Color(0xFF00F0FF),
                            inactiveTrackColor = Color(0xFF1E293B)
                        ),
                        modifier = Modifier.height(28.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 3. Distância Máxima
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Distância Máxima de Rota:", color = Color(0xFFCBD5E1), fontSize = 11.sp)
                        Text(
                            String.format(Locale.ROOT, "%.1f km", criteria.maxDistanceKm),
                            color = Color(0xFFFFB800),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                    Slider(
                        value = criteria.maxDistanceKm.toFloat(),
                        onValueChange = { newValue ->
                            val rounded = (Math.round(newValue * 2) / 2.0)
                            autoAcceptManager.updateCriteria(criteria.copy(maxDistanceKm = rounded))
                        },
                        valueRange = 2.0f..10.0f,
                        steps = 15,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFFFB800),
                            activeTrackColor = Color(0xFFFFB800),
                            inactiveTrackColor = Color(0xFF1E293B)
                        ),
                        modifier = Modifier.height(28.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 4. Aceite Seguro em Movimento (Zero Toque)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF111827))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Aceite Seguro com Moto em Movimento",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Permite aceitar via IA acima de 10km/h sem tocar na tela",
                                color = Color(0xFF94A3B8),
                                fontSize = 10.sp
                            )
                        }
                        Switch(
                            checked = criteria.allowWhileMoving,
                            onCheckedChange = { newState ->
                                autoAcceptManager.updateCriteria(criteria.copy(allowWhileMoving = newState))
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = Color(0xFF00FF88)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 5. Filtro de Plataformas Homologadas
                    Text(
                        text = "Plataformas Autorizadas:",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val availableApps = listOf("iFood", "Uber", "99 Moto", "Rappi")
                        availableApps.forEach { app ->
                            val isSelected = criteria.enabledPlatforms.contains(app)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) Color(0xFF00FF88).copy(alpha = 0.2f) else Color(0xFF1F2937))
                                    .border(
                                        1.dp,
                                        if (isSelected) Color(0xFF00FF88) else Color(0xFF374151),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable {
                                        val newSet = if (isSelected) {
                                            if (criteria.enabledPlatforms.size > 1) criteria.enabledPlatforms - app
                                            else criteria.enabledPlatforms
                                        } else {
                                            criteria.enabledPlatforms + app
                                        }
                                        autoAcceptManager.updateCriteria(criteria.copy(enabledPlatforms = newSet))
                                    }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = app,
                                    color = if (isSelected) Color(0xFF00FF88) else Color(0xFF9CA3AF),
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // 6. PAINEL EXPANSÍVEL DE HISTÓRICO DE CORRIDAS ACEITAS PELO ROBÔ
            // =========================================================================
            AnimatedVisibility(
                visible = isHistoryExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF05070A))
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ÚLTIMAS CORRIDAS AUTO-ACEITAS",
                            color = Color(0xFF00F0FF),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (recentRides.isNotEmpty()) {
                            TextButton(
                                onClick = {
                                    autoAcceptManager.resetStatistics()
                                    Toast.makeText(context, "Histórico de auto-aceite limpo", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Text("LIMPAR", color = Color(0xFFEF4444), fontSize = 10.sp)
                            }
                        }
                    }

                    if (recentRides.isEmpty()) {
                        Text(
                            text = "Nenhuma corrida aceita pelo robô ainda hoje. Toque em 'TESTAR DISPARO' para validar.",
                            color = Color(0xFF64748B),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        recentRides.take(5).forEachIndexed { index, record ->
                            if (index > 0) Divider(color = Color(0xFF1E293B), thickness = 0.8.dp, modifier = Modifier.padding(vertical = 8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = record.appName,
                                            color = Color(0xFF00FF88),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = record.timestampFormatted,
                                            color = Color(0xFF64748B),
                                            fontSize = 10.sp
                                        )
                                    }
                                    Text(
                                        text = record.restaurant,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${String.format(Locale.ROOT, "%.1f", record.distanceKm)}km • ${record.reason}",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 10.sp
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = String.format(Locale("pt", "BR"), "R$ %.2f", record.value),
                                        color = Color(0xFF00FF88),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = String.format(Locale("pt", "BR"), "R$ %.2f/km", record.gainPerKm),
                                        color = Color(0xFF00F0FF),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
