package com.example

import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

private val NeonGreen = Color(0xFF00FF88)
private val CyberCyan = Color(0xFF00D2FF)
private val CyberGold = Color(0xFFFFD700)
private val CardSurface = Color(0xFF13131D)
private val CardBorder = Color(0xFF232335)
private val TextLight = Color(0xFFF0F0F8)
private val TextMuted = Color(0xFF8888A2)

@Composable
fun DynamicGeofencingControlCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val geoState by DynamicGeofencingEngine.geofenceState.collectAsState()

    val infiniteTransition = rememberInfiniteTransition(label = "geofence_pulse")
    val pulseRadiusFraction by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_radius"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("dynamic_geofencing_control_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Brush.horizontalGradient(listOf(NeonGreen.copy(alpha = 0.6f), CyberCyan.copy(alpha = 0.6f)))
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header do Card
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
                                Brush.linearGradient(listOf(NeonGreen.copy(alpha = 0.2f), CyberCyan.copy(alpha = 0.2f)))
                            )
                            .border(1.dp, NeonGreen.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrackChanges,
                            contentDescription = "Geofencing Dinâmico",
                            tint = NeonGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "GEOFENCING DINÂMICO ADAPTATIVO",
                                color = TextLight,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Text(
                            text = "Ajuste de raio por velocidade e densidade de polos",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }

                // Badge de Probabilidade de Mesclagem
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(NeonGreen.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "MESCLAGEM ${geoState.mergeProbabilityPercent}%",
                        color = NeonGreen,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Radar Visual com Escopo de Geofence Dinâmico
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF090C12))
                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                    val w = size.width
                    val h = size.height
                    val center = Offset(w * 0.35f, h * 0.50f)
                    val maxRadius = (h * 0.45f).coerceAtLeast(30f)

                    // Escala do raio relativo (450m a 2800m)
                    val scaleFactor = (geoState.computedRadiusMeters - 400).toFloat() / (3000f - 400f)
                    val currentRadiusPx = maxRadius * (0.35f + (scaleFactor * 0.65f))

                    // Círculos concêntricos do radar
                    drawCircle(color = Color(0xFF151C2C), radius = maxRadius * 0.35f, center = center, style = Stroke(1.dp.toPx()))
                    drawCircle(color = Color(0xFF151C2C), radius = maxRadius * 0.70f, center = center, style = Stroke(1.dp.toPx()))
                    drawCircle(color = Color(0xFF1E283E), radius = maxRadius, center = center, style = Stroke(1.dp.toPx()))

                    // Onda de pulsação do radar
                    drawCircle(
                        color = NeonGreen.copy(alpha = (1f - pulseRadiusFraction) * 0.4f),
                        radius = currentRadiusPx * pulseRadiusFraction,
                        center = center
                    )

                    // Círculo ativo da Geofence Atual
                    drawCircle(
                        color = NeonGreen.copy(alpha = 0.15f),
                        radius = currentRadiusPx,
                        center = center
                    )
                    drawCircle(
                        color = NeonGreen,
                        radius = currentRadiusPx,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )

                    // Centroide: Motoboy no centro
                    drawCircle(color = CyberGold, radius = 5.dp.toPx(), center = center)
                    drawCircle(color = CyberGold.copy(alpha = 0.3f), radius = 10.dp.toPx(), center = center)
                }

                // Legendas de telemetria dentro do Radar Box
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 16.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "RAIO DE BUSCA ATIVO",
                        color = TextMuted,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${geoState.computedRadiusMeters} metros",
                        color = NeonGreen,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "VELOCIDADE GPS: ${geoState.currentSpeedKmh.toInt()} km/h",
                        color = CyberCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "DENSIDADE: ${geoState.densityProfile.spotsPerKm2} polos/km²",
                        color = CyberGold,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Banner Tático de Orientação
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F1420))
                    .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Adjust,
                        contentDescription = null,
                        tint = NeonGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = geoState.tacticalExplanation,
                        color = TextLight,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Seletor de Perfis de Densidade Gastronômica
            Text(
                text = "PERFIL DE DENSIDADE DA REGIÃO:",
                color = TextMuted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DynamicGeofencingEngine.RestaurantDensityProfile.values().forEach { profile ->
                    val isSelected = profile == geoState.densityProfile
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) CyberCyan.copy(alpha = 0.2f) else Color(0xFF181824))
                            .border(
                                1.dp,
                                if (isSelected) CyberCyan else CardBorder,
                                RoundedCornerShape(6.dp)
                            )
                            .clickable {
                                DynamicGeofencingEngine.recalculate(
                                    speedKmh = geoState.currentSpeedKmh,
                                    density = profile
                                )
                                HapticFeedbackHelper.vibrateTap(context)
                            }
                            .padding(vertical = 5.dp, horizontal = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = profile.name.split("_").first(),
                            color = if (isSelected) CyberCyan else TextMuted,
                            fontSize = 8.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Botões de Simulação de Velocidade
            Text(
                text = "SIMULAR CENÁRIOS DE VELOCIDADE REAL:",
                color = TextMuted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = {
                        DynamicGeofencingEngine.recalculate(
                            speedKmh = 0.0,
                            density = DynamicGeofencingEngine.RestaurantDensityProfile.HYPER_DENSE_MALL
                        )
                        HapticFeedbackHelper.vibrateAccept(context)
                        try {
                            DeliveryAudioNotificationService.getInstance(context).speak(
                                "Parado no shopping: raio de geofence calibrado para 450 metros com foco em pedidos do mesmo balcão."
                            )
                        } catch (_: Exception) {}
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2822)),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "0 km/h (Polo)",
                        color = NeonGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = {
                        DynamicGeofencingEngine.recalculate(
                            speedKmh = 25.0,
                            density = DynamicGeofencingEngine.RestaurantDensityProfile.HIGH_DENSITY_URBAN
                        )
                        HapticFeedbackHelper.vibrateTap(context)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A2434)),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "25 km/h (Urbano)",
                        color = CyberCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = {
                        DynamicGeofencingEngine.recalculate(
                            speedKmh = 55.0,
                            density = DynamicGeofencingEngine.RestaurantDensityProfile.SPARSE_HIGHWAY
                        )
                        HapticFeedbackHelper.vibrateSuccess(context)
                        try {
                            DeliveryAudioNotificationService.getInstance(context).speak(
                                "Avenida rápida detectada: raio expandido para 2800 metros para aceitação antecipada antes da saída da via."
                            )
                        } catch (_: Exception) {}
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E2218)),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "55 km/h (Avenida)",
                        color = CyberGold,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
