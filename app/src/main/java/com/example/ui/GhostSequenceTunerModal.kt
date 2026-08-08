package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.coordinator.RadarCoordinator
import com.example.data.FirestoreManager

@Composable
fun GhostSequenceTunerModal(
    onDismissRequest: () -> Unit
) {
    val currentSettings by RadarCoordinator.settings.collectAsState()

    var aggressiveness by remember { mutableStateOf(currentSettings.ghostSequenceAggressiveness) }
    var trafficWeight by remember { mutableFloatStateOf(currentSettings.ghostSequenceTrafficWeight.toFloat()) }
    var latencyWeight by remember { mutableFloatStateOf(currentSettings.ghostSequenceLatencyWeight.toFloat()) }
    var maxWaitMin by remember { mutableIntStateOf((latencyWeight * 20).toInt().coerceIn(3, 20)) }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF111118),
            border = BorderStroke(1.5.dp, Brush.linearGradient(listOf(Color(0xFF00FF88), Color(0xFF00E5FF))))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Title
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
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00FF88).copy(alpha = 0.15f))
                                .border(1.dp, Color(0xFF00FF88), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Sintonizador IA",
                                tint = Color(0xFF00FF88),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "GHOST SEQUENCE NEURAL TUNER",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Ajuste Fino de Algoritmo de Batelada Multi-App",
                                color = Color.Gray,
                                fontSize = 10.sp
                            )
                        }
                    }

                    IconButton(onClick = onDismissRequest) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fechar",
                            tint = Color.Gray
                        )
                    }
                }

                Divider(color = Color.White.copy(alpha = 0.1f))

                // Real-time D3-style Density & Synergy Prediction Chart
                GhostDensityD3ChartPreview(
                    aggressiveness = aggressiveness,
                    trafficWeight = trafficWeight,
                    latencyWeight = latencyWeight
                )

                // Controls Section
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                    // 1. Aggressiveness Selector
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "1. NÍVEL DE AGRESSIVIDADE DA IA",
                            color = Color(0xFF00E5FF),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("CONSERVADOR", "EQUILIBRADO", "AGRESSIVO").forEach { level ->
                                val isSelected = aggressiveness.equals(level, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) Color(0xFF00FF88).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f))
                                        .border(
                                            1.dp,
                                            if (isSelected) Color(0xFF00FF88) else Color.White.copy(alpha = 0.15f),
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { aggressiveness = level }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = level,
                                        color = if (isSelected) Color(0xFF00FF88) else Color.Gray,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }
                    }

                    // 2. Price vs Distance Weighting Slider
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "2. PESO PREÇO VS DISTÂNCIA (R$/KM)",
                                color = Color(0xFF00E5FF),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${(trafficWeight * 100).toInt()}% Preço",
                                color = Color(0xFF00FF88),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Slider(
                            value = trafficWeight,
                            onValueChange = { trafficWeight = it },
                            valueRange = 0.1f..1.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF00FF88),
                                activeTrackColor = Color(0xFF00FF88),
                                inactiveTrackColor = Color.Gray.copy(alpha = 0.3f)
                            )
                        )
                    }

                    // 3. Max Waiting Time / Latency Tolerance
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "3. TOLERÂNCIA DE ESPERA NA COZINHA",
                                color = Color(0xFF00E5FF),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Até $maxWaitMin min",
                                color = Color(0xFFFFCC00),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Slider(
                            value = latencyWeight,
                            onValueChange = {
                                latencyWeight = it
                                maxWaitMin = (it * 20).toInt().coerceIn(3, 20)
                            },
                            valueRange = 0.1f..1.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFFFCC00),
                                activeTrackColor = Color(0xFFFFCC00),
                                inactiveTrackColor = Color.Gray.copy(alpha = 0.3f)
                            )
                        )
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.5f))
                    ) {
                        Text("CANCELAR", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val newSettings = currentSettings.copy(
                                ghostSequenceAggressiveness = aggressiveness,
                                ghostSequenceTrafficWeight = trafficWeight.toDouble(),
                                ghostSequenceLatencyWeight = latencyWeight.toDouble()
                            )
                            RadarCoordinator.updateSettings(newSettings)
                            FirestoreManager.saveSettings(newSettings)
                            onDismissRequest()
                        },
                        modifier = Modifier.weight(1.2f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF88)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("SALVAR AJUSTE FINO", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

/**
 * Real-time D3-style Canvas preview chart rendering density curves & synergy boosts dynamically
 */
@Composable
fun GhostDensityD3ChartPreview(
    aggressiveness: String,
    trafficWeight: Float,
    latencyWeight: Float
) {
    // Calculated live metrics
    val aggMultiplier = when (aggressiveness.uppercase()) {
        "AGRESSIVO" -> 1.45f
        "CONSERVADOR" -> 0.75f
        else -> 1.0f
    }
    val predictedBatchDensity = ((trafficWeight * 0.5f + latencyWeight * 0.5f) * 100f * aggMultiplier).coerceIn(30f, 98f)
    val estimatedGainBoost = (4.5f + (trafficWeight * 3.8f) + (aggMultiplier * 1.5f))

    val animatedDensity by animateFloatAsState(targetValue = predictedBatchDensity, animationSpec = tween(600), label = "densityAnim")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.6f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📈 SIMULAÇÃO DE DENSIDADE DE BATELADA",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Sinergia: ${animatedDensity.toInt()}%",
                        color = Color(0xFF00FF88),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "+R$ ${String.format("%.2f", estimatedGainBoost)}/km",
                        color = Color(0xFF00E5FF),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // D3 Curve Canvas Drawing
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                val w = size.width
                val h = size.height

                // Draw background grid lines
                for (i in 1..4) {
                    val y = h * (i / 5f)
                    drawLine(
                        color = Color.White.copy(alpha = 0.05f),
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1f
                    )
                }

                // Compute smooth Bezier curve points based on parameters
                val path = Path()
                val steps = 8
                val startY = h * (1f - (0.3f * aggMultiplier))
                path.moveTo(0f, startY)

                for (i in 1..steps) {
                    val x = w * (i.toFloat() / steps)
                    val factor = Math.sin((i.toDouble() / steps) * Math.PI).toFloat()
                    val peakY = h * (1f - (animatedDensity / 100f))
                    val currentY = startY - ((startY - peakY) * factor)

                    val prevX = w * ((i - 1).toFloat() / steps)
                    val cx1 = prevX + (x - prevX) / 2f
                    val cx2 = prevX + (x - prevX) / 2f
                    path.cubicTo(cx1, startY - ((startY - peakY) * factor * 0.8f), cx2, currentY, x, currentY)
                }

                // Draw filled gradient under curve
                val fillPath = Path().apply {
                    addPath(path)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF00FF88).copy(alpha = 0.35f),
                            Color(0xFF00E5FF).copy(alpha = 0.10f),
                            Color.Transparent
                        )
                    )
                )

                // Draw main curve line
                drawPath(
                    path = path,
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFF00FF88), Color(0xFF00E5FF), Color(0xFFFF0055))
                    ),
                    style = Stroke(width = 3.5f)
                )
            }
        }
    }
}
