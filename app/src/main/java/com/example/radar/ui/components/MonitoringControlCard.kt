package com.example.radar.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.radar.ui.theme.DarkBackground
import com.example.radar.ui.theme.DarkSurface
import com.example.radar.ui.theme.DarkSurfaceBorder
import com.example.radar.ui.theme.NeonEmerald
import com.example.radar.ui.theme.StatusOffline
import com.example.radar.ui.theme.TextMuted
import com.example.radar.ui.theme.TextPrimary
import com.example.radar.ui.theme.TextSecondary

@Composable
fun MonitoringControlCard(
    isMonitoringActive: Boolean,
    onToggleMonitoring: () -> Unit,
    onActivate: () -> Unit,
    onDeactivate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeColor by animateColorAsState(
        targetValue = if (isMonitoringActive) NeonEmerald else StatusOffline,
        label = "activeColor"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val radarRing1 by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 2.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring1"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        DarkSurface,
                        DarkBackground
                    )
                )
            )
            .border(1.dp, activeColor.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header do Card com Status e Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Radar Visualizer Mini
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(36.dp)
                    ) {
                        if (isMonitoringActive) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .scale(radarRing1)
                                    .clip(CircleShape)
                                    .background(NeonEmerald.copy(alpha = 0.2f))
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(activeColor.copy(alpha = 0.2f))
                                .border(1.5.dp, activeColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isMonitoringActive) "🎯" else "⏸️",
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = if (isMonitoringActive) "MONITORAMENTO ATIVO" else "MONITORAMENTO PAUSADO",
                            color = activeColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = if (isMonitoringActive) "Escaneando iFood, Rappi, Uber e 99" else "Ofertas automáticas em espera",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                // Switch de Ativação Rápida
                Switch(
                    checked = isMonitoringActive,
                    onCheckedChange = { onToggleMonitoring() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = DarkBackground,
                        checkedTrackColor = NeonEmerald,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = DarkSurfaceBorder
                    ),
                    modifier = Modifier.testTag("monitoring_switch")
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Botões de Ação Direta (Ativar / Desativar)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onActivate,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isMonitoringActive) NeonEmerald else DarkSurfaceBorder,
                        contentColor = if (isMonitoringActive) DarkBackground else TextSecondary
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("activate_button")
                ) {
                    Text(
                        text = "▶ ATIVAR RADAR",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                OutlinedButton(
                    onClick = onDeactivate,
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.horizontalGradient(
                            listOf(
                                if (!isMonitoringActive) StatusOffline else DarkSurfaceBorder,
                                if (!isMonitoringActive) StatusOffline.copy(alpha = 0.6f) else DarkSurfaceBorder
                            )
                        )
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (!isMonitoringActive) StatusOffline else TextMuted
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("deactivate_button")
                ) {
                    Text(
                        text = "⏸ PAUSAR",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
