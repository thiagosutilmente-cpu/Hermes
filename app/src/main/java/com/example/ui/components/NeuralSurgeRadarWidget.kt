package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.NeuralSurgePredictor
import com.example.util.PredictiveHotspot
import com.example.coordinator.RadarCoordinator

@Composable
fun NeuralSurgeRadarWidget(
    onNavigateToHotspot: (PredictiveHotspot) -> Unit = {}
) {
    val currentLocation by RadarCoordinator.currentLocation.collectAsState()
    val settings by RadarCoordinator.settings.collectAsState()

    val lat = currentLocation?.latitude ?: -23.5505
    val lng = currentLocation?.longitude ?: -46.6333

    val hotspots = remember(lat, lng) {
        NeuralSurgePredictor.predictSurgeHotspots(
            userLat = lat,
            userLng = lng,
            rainModeActive = settings.rainModeMultiplier > 1.0,
            trafficDensityFactor = if (settings.showTrafficDensity) 1.3 else 1.0
        )
    }

    var selectedHotspot by remember { mutableStateOf(hotspots.firstOrNull()) }

    // Pulsing radar animation sweep
    val infiniteTransition = rememberInfiniteTransition(label = "radarSweep")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweepAngle"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111118)),
        border = BorderStroke(1.dp, Brush.horizontalGradient(listOf(Color(0xFF00FF88), Color(0xFF00E5FF))))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00FF88).copy(alpha = 0.15f))
                            .border(1.dp, Color(0xFF00FF88), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Radar,
                            contentDescription = "Radar Neural",
                            tint = Color(0xFF00FF88),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "PREDITOR NEURAL DE PONTOS DE PICO (SURGE)",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Posicionamento Estratégico em Clusters com R$ 7.50+/km",
                            color = Color.Gray,
                            fontSize = 9.sp
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF00E5FF).copy(alpha = 0.15f),
                    border = BorderStroke(0.5.dp, Color(0xFF00E5FF))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ElectricBolt,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "IA ATIVA",
                            color = Color(0xFF00E5FF),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            // Interactive Radar Canvas & Hotspot Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Animated Radar Scanner Circle
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.7f))
                        .border(1.dp, Color(0xFF00FF88).copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val radius = size.width / 2f

                        // Radar concentric rings
                        drawCircle(color = Color(0xFF00FF88).copy(alpha = 0.15f), radius = radius * 0.75f, style = Stroke(1f))
                        drawCircle(color = Color(0xFF00FF88).copy(alpha = 0.25f), radius = radius * 0.45f, style = Stroke(1f))

                        // Radar Sweep line
                        val sweepRad = Math.toRadians(sweepAngle.toDouble())
                        val endX = center.x + (radius * Math.cos(sweepRad)).toFloat()
                        val endY = center.y + (radius * Math.sin(sweepRad)).toFloat()

                        drawLine(
                            color = Color(0xFF00FF88),
                            start = center,
                            end = Offset(endX, endY),
                            strokeWidth = 2f
                        )

                        // Hotspot dots on radar
                        hotspots.forEachIndexed { i, h ->
                            val dotAngle = (i * 90) + 25
                            val dotRad = Math.toRadians(dotAngle.toDouble())
                            val distFactor = (h.distanceKm / 5.0).coerceIn(0.2, 0.8)
                            val dotX = center.x + (radius * distFactor * Math.cos(dotRad)).toFloat()
                            val dotY = center.y + (radius * distFactor * Math.sin(dotRad)).toFloat()

                            drawCircle(
                                color = if (selectedHotspot?.id == h.id) Color(0xFFFF0055) else Color(0xFF00FF88),
                                radius = if (selectedHotspot?.id == h.id) 5f else 3f,
                                center = Offset(dotX, dotY)
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Selected Hotspot Stats Box
                selectedHotspot?.let { spot ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = 0.5f))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = spot.name,
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                text = "${spot.demandScore}% Pico",
                                color = Color(0xFF00FF88),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Projeção: R$ ${spot.estimatedFarePerKm}/km",
                                color = Color(0xFF00E5FF),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "${spot.distanceKm} km",
                                color = Color.Gray,
                                fontSize = 9.sp
                            )
                        }

                        Text(
                            text = "Apps: ${spot.dominantApps.joinToString(", ")}",
                            color = Color.LightGray,
                            fontSize = 8.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = {
                                    com.example.util.MultiAppOrderManager.setNavigationAddress(spot.name)
                                    onNavigateToHotspot(spot)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF88)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "Navegar",
                                    tint = Color.Black,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "ROTEAR PONTO CHAVE",
                                    color = Color.Black,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }

            // Horizontal Selector Chips for Hotspots
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(hotspots) { spot ->
                    val isSelected = selectedHotspot?.id == spot.id
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Color(0xFF00FF88).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f))
                            .border(
                                1.dp,
                                if (isSelected) Color(0xFF00FF88) else Color.White.copy(alpha = 0.15f),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedHotspot = spot }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = spot.name.take(18) + "...",
                                color = if (isSelected) Color(0xFF00FF88) else Color.Gray,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${spot.predictedSurgeMultiplier}x",
                                color = Color(0xFF00E5FF),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }
    }
}
