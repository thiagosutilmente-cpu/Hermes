package com.example

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

/**
 * Componente de Dashboard de Velocidade em Tempo Real com Trava de Segurança Automática.
 *
 * Características principais:
 * 1. Exibe a velocidade atual do piloto em tempo real com indicador digital de tacômetro.
 * 2. Transiciona a cor de fundo dinamicamente para VERMELHO INTENSO de emergência
 *    quando a velocidade ultrapassa o limite configurado (default: 10 km/h).
 * 3. Bloqueia integralmente as interações táteis com a tela quando o limite for excedido,
 *    interceptando todos os toques via pointerInput para prevenir acidentes no trânsito.
 * 4. Permite ao condutor configurar o limite (presets ou slider contínuo) quando parado.
 * 5. Disponibiliza painel de simulação rápida para verificação imediata das cores e da trava.
 */
@Composable
fun RealTimeSpeedSafetyDashboardCard(
    modifier: Modifier = Modifier,
    initialSpeedThresholdKmh: Double = LocationService.dynamicSafetySpeedThresholdKmh,
    onSpeedThresholdChanged: ((Double) -> Unit)? = null,
    onSimulateSpeed: ((Double) -> Unit)? = null,
    enableFullscreenLock: Boolean = false
) {
    val context = LocalContext.current
    val speedState by LocationService.globalLocationState.collectAsState()

    var configuredLimitKmh by remember {
        mutableDoubleStateOf(initialSpeedThresholdKmh.coerceIn(5.0, 60.0))
    }
    var showConfigPanel by remember { mutableStateOf(false) }

    val currentSpeedKmh = speedState.currentSpeedKmh
    val isSpeedExceeded = currentSpeedKmh > configuredLimitKmh

    // Emite feedback háptico quando a trava for acionada por excesso de velocidade
    LaunchedEffect(isSpeedExceeded) {
        if (isSpeedExceeded) {
            HapticFeedbackHelper.vibrateWarning(context)
        }
    }

    // Animação de pulso visual de alerta no estado de perigo (vermelho)
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_warning")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    // Cores dinâmicas que transicionam conforme a velocidade
    val targetBgColor = if (isSpeedExceeded) {
        Color(0xFFB71C1C) // Vermelho de Alerta Crítico
    } else {
        Color(0xFF0C1017) // HUD escuro noturno com alto contraste
    }

    val animatedBgColor by animateColorAsState(
        targetValue = targetBgColor,
        animationSpec = tween(durationMillis = 400),
        label = "animatedBgColor"
    )

    val targetBorderColor = if (isSpeedExceeded) {
        Color(0xFFFF1744).copy(alpha = pulseAlpha)
    } else {
        Color(0xFF00FF88).copy(alpha = 0.6f)
    }

    // Progresso relativo da velocidade até o limite (0.0 a 1.0+)
    val speedProgress = (currentSpeedKmh / configuredLimitKmh).toFloat().coerceIn(0f, 1.5f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .testTag("speed_dashboard_container")
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("speed_dashboard_card")
                .border(
                    width = if (isSpeedExceeded) 2.5.dp else 1.dp,
                    color = targetBorderColor,
                    shape = RoundedCornerShape(20.dp)
                ),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = animatedBgColor
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = if (isSpeedExceeded) 10.dp else 4.dp
            )
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                // Conteúdo principal do tacômetro
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = if (isSpeedExceeded) {
                                    listOf(
                                        Color(0xFFD32F2F),
                                        Color(0xFF8B0000)
                                    )
                                } else {
                                    listOf(
                                        Color(0xFF131922),
                                        Color(0xFF0A0D12)
                                    )
                                }
                            )
                        )
                        .padding(18.dp)
                ) {
                    // 1. Cabeçalho com indicador de status e limite configurado
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSpeedExceeded) Color(0xFFFF5252) else Color(0xFF00FF88)
                                    )
                            )
                            Text(
                                text = if (isSpeedExceeded) "VELOCIDADE EXCEDIDA" else "TELEMETRIA EM TEMPO REAL",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = if (isSpeedExceeded) Color.White else Color(0xFF00FF88)
                            )
                        }

                        // Botão para abrir configuração de limite (desabilitado se em alta velocidade)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSpeedExceeded) Color(0x33000000) else Color(0x22FFFFFF),
                                modifier = Modifier.clip(RoundedCornerShape(8.dp))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Speed,
                                        contentDescription = null,
                                        tint = if (isSpeedExceeded) Color.White else Color(0xFF90CAF9),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Limite: ${configuredLimitKmh.toInt()} km/h",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }
                            }

                            if (!isSpeedExceeded) {
                                IconButton(
                                    onClick = { showConfigPanel = !showConfigPanel },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = "Configurar Limite",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 2. Tacômetro Principal: Número da Velocidade em Destaque
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = String.format(Locale("pt", "BR"), "%.1f", currentSpeedKmh),
                                    fontSize = 54.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.White,
                                    lineHeight = 54.sp,
                                    modifier = Modifier.testTag("speed_display_text")
                                )
                                Text(
                                    text = "km/h",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSpeedExceeded) Color(0xFFFFCDD2) else Color(0xFF80CBC4),
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }

                            Text(
                                text = when {
                                    currentSpeedKmh == 0.0 -> "Parado • Aguardando corrida"
                                    currentSpeedKmh <= 5.0 -> "Manobra em baixa velocidade"
                                    currentSpeedKmh <= configuredLimitKmh -> "Deslocamento seguro"
                                    else -> "⚠️ PILOTAGEM EM CURSO — TOQUES BLOQUEADOS"
                                },
                                fontSize = 12.sp,
                                fontWeight = if (isSpeedExceeded) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (isSpeedExceeded) Color.White else Color(0xFFB0BEC5)
                            )
                        }

                        // Ícone Dinâmico de Cadeado / Status de Proteção
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSpeedExceeded) Color(0x33000000) else Color(0x1A00FF88)
                                )
                                .border(
                                    width = 1.5.dp,
                                    color = if (isSpeedExceeded) Color.White else Color(0xFF00FF88),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isSpeedExceeded) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = if (isSpeedExceeded) "Tela Bloqueada" else "Tela Liberada",
                                tint = if (isSpeedExceeded) Color.White else Color(0xFF00FF88),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3. Barra de Progresso Analógica da Velocidade
                    Column(modifier = Modifier.fillMaxWidth()) {
                        LinearProgressIndicator(
                            progress = { (currentSpeedKmh / (configuredLimitKmh * 1.5)).toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if (isSpeedExceeded) Color.White else Color(0xFF00FF88),
                            trackColor = if (isSpeedExceeded) Color(0x44000000) else Color(0x33FFFFFF),
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "0 km/h",
                                fontSize = 10.sp,
                                color = if (isSpeedExceeded) Color(0xFFFFCDD2) else Color.Gray
                            )
                            Text(
                                text = "Limite: ${configuredLimitKmh.toInt()} km/h",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSpeedExceeded) Color.White else Color(0xFF00FF88)
                            )
                            Text(
                                text = "${(configuredLimitKmh * 1.5).toInt()} km/h",
                                fontSize = 10.sp,
                                color = if (isSpeedExceeded) Color(0xFFFFCDD2) else Color.Gray
                            )
                        }
                    }

                    // 4. Banner de Bloqueio Ativo (Quando velocidade > limite)
                    AnimatedVisibility(
                        visible = isSpeedExceeded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 14.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x33000000))
                                .border(1.dp, Color(0x66FFFFFF), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                                .testTag("speed_lock_banner")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "TRAVA DE SEGURANÇA ATIVADA",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Toques na tela bloqueados para sua proteção no trânsito. Use comandos de voz no capacete (\"Jarvis, aceitar\") ou reduza para menos de ${configuredLimitKmh.toInt()} km/h.",
                                fontSize = 11.sp,
                                color = Color.White,
                                lineHeight = 15.sp
                            )
                        }
                    }

                    // 5. Painel de Configuração do Limite (Visível apenas quando abaixo da velocidade limite)
                    AnimatedVisibility(
                        visible = showConfigPanel && !isSpeedExceeded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 14.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x22FFFFFF))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "Ajustar Limite de Trava de Segurança",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // Slider de ajuste
                            Slider(
                                value = configuredLimitKmh.toFloat(),
                                onValueChange = { newVal ->
                                    val rounded = newVal.toDouble()
                                    configuredLimitKmh = rounded
                                    LocationService.updateSafetySpeedThreshold(rounded)
                                    onSpeedThresholdChanged?.invoke(rounded)
                                },
                                valueRange = 5f..50f,
                                steps = 8,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("speed_limit_slider"),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF00FF88),
                                    activeTrackColor = Color(0xFF00FF88),
                                    inactiveTrackColor = Color(0x44FFFFFF)
                                )
                            )

                            // Atalhos Rápidos
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(10.0, 15.0, 20.0, 30.0).forEach { preset ->
                                    FilterChip(
                                        selected = configuredLimitKmh.toInt() == preset.toInt(),
                                        onClick = {
                                            configuredLimitKmh = preset
                                            LocationService.updateSafetySpeedThreshold(preset)
                                            onSpeedThresholdChanged?.invoke(preset)
                                        },
                                        label = {
                                            Text(
                                                text = "${preset.toInt()} km/h",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF00FF88),
                                            selectedLabelColor = Color.Black,
                                            containerColor = Color(0x1AFFFFFF),
                                            labelColor = Color.White
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 6. Barra de Simulação Rápida (Para testes no emulador ou parado)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Testar Velocidade:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSpeedExceeded) Color(0xFFFFCDD2) else Color.Gray
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Parar (0 km/h)
                            Button(
                                onClick = {
                                    LocationService.updateSimulatedSpeed(0.0, context)
                                    onSimulateSpeed?.invoke(0.0)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSpeedExceeded) Color.White else Color(0x22FFFFFF),
                                    contentColor = if (isSpeedExceeded) Color.Black else Color.White
                                ),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                    horizontal = 10.dp,
                                    vertical = 4.dp
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("0 km/h", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            // Limite Seguro (7 km/h)
                            Button(
                                onClick = {
                                    val safe = (configuredLimitKmh - 3.0).coerceAtLeast(2.0)
                                    LocationService.updateSimulatedSpeed(safe, context)
                                    onSimulateSpeed?.invoke(safe)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0x22FFFFFF),
                                    contentColor = Color.White
                                ),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                    horizontal = 10.dp,
                                    vertical = 4.dp
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("Seguro", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            // Exceder Limite (> Limite, ex: +15 km/h)
                            Button(
                                onClick = {
                                    val exceeded = configuredLimitKmh + 15.0
                                    LocationService.updateSimulatedSpeed(exceeded, context)
                                    onSimulateSpeed?.invoke(exceeded)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSpeedExceeded) Color(0x44000000) else Color(0xFFFF5252),
                                    contentColor = Color.White
                                ),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                    horizontal = 10.dp,
                                    vertical = 4.dp
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("Bloquear", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // 7. ESCUDO PROTETOR DE BLOQUEIO INTEGRAL DE TOQUE (Quando a velocidade exceder o limite)
                // Intercepta e consome todo e qualquer toque quando em velocidade excessiva
                if (isSpeedExceeded) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .testTag("speed_lock_interaction_shield")
                            .pointerInput(isSpeedExceeded) {
                                // Consome 100% dos eventos de toque para bloquear interações com os botões internos
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        event.changes.forEach { it.consume() }
                                    }
                                }
                            }
                    )
                }
            }
        }
    }

    // 8. BLOQUEIO DE TELA CHEIA (Opcional: quando configurado para blindar o app inteiro)
    if (enableFullscreenLock && isSpeedExceeded) {
        FullscreenSpeedLockBarrier(
            currentSpeedKmh = currentSpeedKmh,
            thresholdKmh = configuredLimitKmh,
            onEmergencyStop = {
                LocationService.updateSimulatedSpeed(0.0, context)
                onSimulateSpeed?.invoke(0.0)
            }
        )
    }
}

/**
 * Cortina de Bloqueio Tátil Global da Tela em Condução Excessiva.
 * Intercepta e consome todos os toques da janela inteira para garantir conformidade
 * com segurança viária e prevenção de distrações de moto.
 */
@Composable
fun FullscreenSpeedLockBarrier(
    currentSpeedKmh: Double,
    thresholdKmh: Double,
    onEmergencyStop: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "fullscreen_pulse")
    val pulseBorder by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseBorder"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE67F0000)) // Vermelho escuro translúcido protetor
            .pointerInput(Unit) {
                // Interceptador e consumidor incondicional de toques na tela
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        event.changes.forEach { it.consume() }
                    }
                }
            }
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, Color.White.copy(alpha = pulseBorder), RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFB71C1C))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Bloqueio Ativo",
                        tint = Color(0xFFB71C1C),
                        modifier = Modifier.size(40.dp)
                    )
                }

                Text(
                    text = "TRAVA DE SEGURANÇA EM CURSO",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "${String.format(Locale("pt", "BR"), "%.1f", currentSpeedKmh)} km/h",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                )

                Text(
                    text = "A tela está bloqueada porque sua velocidade ultrapassou o limite configurado (${thresholdKmh.toInt()} km/h).\n\nPara sua segurança, opere usando a voz com o Jarvis ou pare a moto para voltar a tocar na tela.",
                    fontSize = 13.sp,
                    color = Color(0xFFFFCDD2),
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                // Botão de simulação/parada de emergência para testes
                Button(
                    onClick = onEmergencyStop,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFFB71C1C)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Simular Parada (0 km/h)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
