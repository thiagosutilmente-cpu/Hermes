package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

val NeonCyan = Color(0xFF00E5FF)
val NeonMagenta = Color(0xFFFF00FF)
val VoidBlack = Color(0xFF020205)

@Composable
fun JarvisVoiceHUD(modifier: Modifier = Modifier, isActive: Boolean = false, driverName: String = "OPERATOR") {
    val infiniteTransition = rememberInfiniteTransition(label = "hologram")
    
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val chaos by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isActive) 120 else 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "chaos"
    )

    val targetAmplitude = if (isActive) 1f else 0.1f
    val amplitudeAnim by animateFloatAsState(
        targetValue = targetAmplitude,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "amplitude"
    )

    var randomBands by remember { mutableStateOf(List(32) { 0f }) }
    LaunchedEffect(isActive) {
        while (true) {
            randomBands = List(32) { if (isActive) Random.nextFloat() else 0.1f + Random.nextFloat() * 0.1f }
            delay(if (isActive) 50 else 200)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(VoidBlack)
            .border(
                width = 1.dp,
                color = if (isActive) NeonMagenta.copy(alpha = 0.5f) else NeonCyan.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cy = h / 2
            
            val glitchActive = isActive && Random.nextFloat() > 0.8f
            val gShift = if (glitchActive) chaos * 10f else 0f
            
            // Draw chaotic spectral bands
            val bandWidth = w / 32f
            for (i in 0 until 32) {
                val bandH = randomBands[i] * h * amplitudeAnim
                val bandX = i * bandWidth
                val color = if (i % 3 == 0) NeonMagenta else NeonCyan
                
                translate(left = gShift * (if(i%2==0) 1 else -1)) {
                    drawRect(
                        color = color.copy(alpha = 0.3f * (1f + chaos*0.2f)),
                        topLeft = Offset(bandX + 2f, cy - bandH/2),
                        size = Size(bandWidth - 4f, bandH)
                    )
                }
            }
            
            // Draw hyper-waveform
            val wavePath1 = Path()
            val wavePath2 = Path()
            val points = 100
            for (i in 0..points) {
                val x = (i.toFloat() / points) * w
                // Complex interference pattern
                val y1 = cy + sin(phase + i*0.1f) * cos(phase*2f - i*0.05f) * h * 0.4f * amplitudeAnim
                val y2 = cy + cos(-phase + i*0.2f) * sin(phase*3f + i*0.08f) * h * 0.3f * amplitudeAnim
                
                if (i == 0) {
                    wavePath1.moveTo(x, y1)
                    wavePath2.moveTo(x, y2)
                } else {
                    wavePath1.lineTo(x, y1)
                    wavePath2.lineTo(x, y2)
                }
            }
            
            translate(left = -gShift) {
                drawPath(wavePath1, color = NeonCyan.copy(alpha = 0.8f), style = Stroke(width = 3f, cap = StrokeCap.Round))
            }
            translate(left = gShift * 1.5f) {
                drawPath(wavePath2, color = NeonMagenta.copy(alpha = 0.8f), style = Stroke(width = 3f, cap = StrokeCap.Round))
            }
            
            // UI Overlay details
            drawLine(
                color = NeonCyan.copy(alpha = 0.5f),
                start = Offset(0f, cy),
                end = Offset(w, cy),
                strokeWidth = 1f,
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(5f, 5f))
            )
        }

        // HUD Text Overlay
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Text(
                    text = "NEURAL LINK: " + if (isActive) "ACTIVE [UNSTABLE]" else "STANDBY",
                    color = if (isActive) NeonMagenta else NeonCyan.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
                Text(
                    text = "ID: $driverName // COGNITIVE BURST",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 8.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
            
            Text(
                text = if (isActive) "FREQ: ${Random.nextInt(100, 999)} THz" else "FREQ: ---",
                color = if (isActive) NeonMagenta else NeonCyan.copy(alpha = 0.7f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        }
    }
}
