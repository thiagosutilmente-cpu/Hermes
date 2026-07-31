package com.example

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.coordinator.RadarCoordinator
import com.example.coordinator.RadarState
import kotlinx.coroutines.delay

import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.voice.JarvisPersonaEngine

@Composable
fun JarvisLiveScreen(
    transcription: String = "",
    response: String = "",
    thoughtProcess: String = "",
    stressLevel: String = "LOW",
    strategyLabel: String = "ESTRATÉGIA PADRÃO",
    isAnalyzing: Boolean = false,
    onClose: () -> Unit
) {
    val radarState by RadarCoordinator.currentState.collectAsStateWithLifecycle()
    val isListeningFlow = RadarCoordinator.voiceInputManager?.isListening
    val isListening = isListeningFlow?.collectAsStateWithLifecycle()?.value == true
    val isProcessing by JarvisPersonaEngine.isProcessing.collectAsStateWithLifecycle()
    
    // Animation for scanning effect
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val scanY by infiniteTransition.animateFloat(
        initialValue = -100f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scan"
    )
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF020617), Color(0xFF0F172A), Color.Black)
                )
            )
    ) {
        // Scanline effect
        Canvas(modifier = Modifier.fillMaxSize()) {
            val scanColor = if (isProcessing) Color(0xFF00F5D4) else Color(0xFF4285F4)
            val scanAlpha = if (isProcessing) 0.3f else 0.1f
            drawLine(
                color = scanColor.copy(alpha = scanAlpha),
                start = Offset(0f, scanY),
                end = Offset(size.width, scanY),
                strokeWidth = (if (isProcessing) 3.dp else 2.dp).toPx()
            )
        }

        // Side Telemetry - Stress Level
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "BIOMETRIC",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 8.sp,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(60.dp)
                    .background(Color.White.copy(alpha = 0.1f))
            ) {
                val stressHeight = when(stressLevel) {
                    "HIGH" -> 1.0f
                    "MEDIUM" -> 0.6f
                    else -> 0.2f
                }
                val stressColor = when(stressLevel) {
                    "HIGH" -> Color(0xFFEA4335)
                    "MEDIUM" -> Color(0xFFFBBC05)
                    else -> Color(0xFF34A853)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(stressHeight)
                        .align(Alignment.BottomCenter)
                        .background(stressColor)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stressLevel,
                color = when(stressLevel) {
                    "HIGH" -> Color(0xFFEA4335)
                    "MEDIUM" -> Color(0xFFFBBC05)
                    else -> Color(0xFF34A853)
                },
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Neural Link Telemetry (Right Side)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "NEURAL LINK",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 8.sp,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            val pulseAlpha by infiniteTransition.animateFloat(
                initialValue = 0.2f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulse"
            )
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (isProcessing) Color(0xFF00F5D4).copy(alpha = pulseAlpha) else Color(0xFF4285F4).copy(alpha = pulseAlpha))
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isProcessing) "SYNCING..." else "ENCRYPTED",
                color = if (isProcessing) Color(0xFF00F5D4) else Color(0xFF4285F4),
                fontSize = 7.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Neural Thought Process Overlay
        if (thoughtProcess.isNotBlank() && (isProcessing || response.isNotBlank())) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 100.dp, start = 40.dp, end = 40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .border(1.dp, Color(0xFF4285F4).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Column {
                    val techStrings = listOf(
                        "AES-512 DECRYPTING...",
                        "STARK UPLINK: MAXIMUM",
                        "NEURAL CORE LOAD: 100%",
                        "SATELLITE SYNC: LOCKED",
                        "QUANTUM DATA STREAMING",
                        "HEARTBEAT MONITOR: PEAK"
                    )
                    val techText = techStrings[(System.currentTimeMillis() / 1500 % techStrings.size).toInt()]
                    
                    Text(
                        text = if (isProcessing) techText else "NEURAL THOUGHT PROCESS",
                        color = if (isProcessing) Color(0xFF00F5D4) else Color(0xFF4285F4),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = thoughtProcess,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            }
        }

        // Server Status (Bottom Left)
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 48.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(if (isProcessing) Color(0xFF00F5D4) else Color(0xFF00F5D4), CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "STARK SERVER LINK: ACTIVE | NEURAL LOAD: 100% [MAX]",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 8.sp,
                    letterSpacing = 1.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 56.dp, start = 24.dp, end = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = "Menu",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(24.dp)
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = 2.dp, y = (-2).dp)
                        .background(Color(0xFF4285F4), CircleShape)
                )
            }
            
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = strategyLabel.uppercase(),
                    color = if (isProcessing) Color(0xFF00F5D4) else Color(0xFF4285F4),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    fontFamily = FontFamily.Monospace
                )
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "More",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Center Content - Neural Waveform
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.Center) {
                // Background Glow
                Box(
                    modifier = Modifier
                        .size(280.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF4285F4).copy(alpha = 0.15f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                
                // Animated Waveform
                Canvas(modifier = Modifier.size(300.dp, 150.dp)) {
                    val width = size.width
                    val height = size.height
                    val centerY = height / 2
                    
                    val amplitude = if (isListening) 50f else if (isProcessing || isAnalyzing) 25f else 12f
                    val frequency = 0.05f
                    
                    // Layer 1 - Primary Blue
                    val path1 = Path()
                    path1.moveTo(0f, centerY)
                    for (x in 0..width.toInt() step 5) {
                        val y = centerY + amplitude * Math.sin((x * frequency + wavePhase).toDouble()).toFloat()
                        path1.lineTo(x.toFloat(), y)
                    }
                    drawPath(path1, Color(0xFF4285F4).copy(alpha = 0.8f), style = Stroke(width = 3.dp.toPx()))
                    
                    // Layer 2 - Subtle Purple
                    val path2 = Path()
                    path2.moveTo(0f, centerY)
                    for (x in 0..width.toInt() step 5) {
                        val y = centerY + (amplitude * 0.7f) * Math.sin((x * frequency - wavePhase * 1.5f).toDouble()).toFloat()
                        path2.lineTo(x.toFloat(), y)
                    }
                    drawPath(path2, Color(0xFF8B5CF6).copy(alpha = 0.5f), style = Stroke(width = 2.dp.toPx()))
                }
                
                // Gemini Icon in center
                GeminiStar(modifier = Modifier.size(40.dp).offset(y = (-40).dp))
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Transcription and Response Labels
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 40.dp)
            ) {
                if (transcription.isNotBlank() && transcription != "Ouvindo comando...") {
                    Text(
                        text = transcription,
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Light,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Text(
                    text = when {
                        isAnalyzing -> "Processando..."
                        response.isNotBlank() -> response
                        isListening -> "Estou ouvindo, Senhor..."
                        else -> "E aí, Thiago, qual o plano?"
                    },
                    color = Color.White,
                    fontSize = if (response.isNotBlank()) 22.sp else 28.sp,
                    fontWeight = if (response.isNotBlank()) FontWeight.Medium else FontWeight.Light,
                    textAlign = TextAlign.Center,
                    lineHeight = if (response.isNotBlank()) 28.sp else 36.sp
                )
            }
        }

        // Bottom Controls
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .fillMaxWidth()
                .padding(horizontal = 48.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LiveIconButton(icon = Icons.Filled.CameraAlt) {}
            
            // Primary Voice Pill
            Surface(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .clickable {
                        if (isListening) {
                            RadarCoordinator.voiceInputManager?.stopListening()
                        } else {
                            RadarCoordinator.voiceInputManager?.startListening(isJarvis = true) {}
                        }
                    },
                color = if (isListening) Color(0xFFEA4335) else Color(0xFF1E293B),
                shape = CircleShape,
                shadowElevation = 12.dp,
                border = androidx.compose.foundation.BorderStroke(2.dp, if (isListening) Color.White.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.1f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isListening) Icons.Filled.Stop else Icons.Filled.Mic,
                        contentDescription = "Voice",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            
            LiveIconButton(icon = Icons.Filled.Close) { onClose() }
        }
    }
}

@Composable
fun LiveIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(64.dp)
            .background(Color(0xFF1E293B), CircleShape)
            .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isActive) Color(0xFF4285F4) else Color.White,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
fun GeminiStar(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val path = Path().apply {
            moveTo(width / 2, 0f)
            quadraticTo(width / 2, height / 2, width, height / 2)
            quadraticTo(width / 2, height / 2, width / 2, height)
            quadraticTo(width / 2, height / 2, 0f, height / 2)
            quadraticTo(width / 2, height / 2, width / 2, 0f)
            close()
        }
        
        drawPath(
            path = path,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF4285F4), // Blue
                    Color(0xFFEA4335), // Red
                    Color(0xFFFBBC05), // Yellow
                    Color(0xFF34A853)  // Green
                )
            )
        )
    }
}
