package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun JarvisFace(modifier: Modifier = Modifier, isProcessing: Boolean = false) {
    val infiniteTransition = rememberInfiniteTransition(label = "face_anim")
    
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(100000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )
    
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isProcessing) 150 else 3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val erraticGlitch by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isProcessing) 50 else 500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glitch"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w / 2
        val cy = h / 2
        
        val cyan = Color(0xFF00E5FF)
        val magenta = Color(0xFFFF00FF)
        val deepBlue = Color(0xFF0A2463)
        val pureWhite = Color(0xFFFFFFFF)

        val baseRadius = w * 0.45f
        
        // Background Sacred Geometry (Metatron-ish)
        rotate(time * 15f) {
            for (i in 0 until 6) {
                val angle = i * (PI / 3)
                val nx = cx + cos(angle).toFloat() * baseRadius * 0.8f
                val ny = cy + sin(angle).toFloat() * baseRadius * 0.8f
                
                drawLine(
                    color = cyan.copy(alpha = 0.15f * pulseAlpha),
                    start = Offset(cx, cy),
                    end = Offset(nx, ny),
                    strokeWidth = 1f
                )
                drawCircle(
                    color = magenta.copy(alpha = 0.1f),
                    radius = baseRadius * 0.2f,
                    center = Offset(nx, ny),
                    style = Stroke(width = 2f)
                )
            }
        }

        // Glitch Layer
        val glitchOffset = if (isProcessing && Random.nextFloat() > 0.8f) erraticGlitch * 20f else 0f
        val colorPhase = if (isProcessing && Random.nextFloat() > 0.9f) magenta else cyan

        translate(left = glitchOffset, top = -glitchOffset * 0.5f) {
            // Orbital Rings
            rotate(-time * 25f) {
                drawCircle(
                    color = colorPhase.copy(alpha = 0.3f),
                    radius = baseRadius,
                    center = Offset(cx, cy),
                    style = Stroke(width = 4f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 20f, 50f, 10f), time))
                )
                drawArc(
                    color = pureWhite.copy(alpha = 0.4f * pulseAlpha),
                    startAngle = 0f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(cx - baseRadius, cy - baseRadius),
                    size = Size(baseRadius * 2, baseRadius * 2),
                    style = Stroke(width = 8f)
                )
                drawArc(
                    color = magenta.copy(alpha = 0.4f * pulseAlpha),
                    startAngle = 180f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(cx - baseRadius, cy - baseRadius),
                    size = Size(baseRadius * 2, baseRadius * 2),
                    style = Stroke(width = 8f)
                )
            }

            // Quantum Core (Center)
            val coreSize = if (isProcessing) baseRadius * 0.4f + (erraticGlitch * 10f) else baseRadius * 0.3f
            
            drawCircle(
                color = colorPhase.copy(alpha = 0.2f * pulseAlpha),
                radius = coreSize * 1.5f,
                center = Offset(cx, cy)
            )

            // Inner eye structure
            val path = Path().apply {
                val eyeW = coreSize * 1.2f
                val eyeH = coreSize * 0.6f
                moveTo(cx - eyeW, cy)
                quadraticBezierTo(cx, cy - eyeH, cx + eyeW, cy)
                quadraticBezierTo(cx, cy + eyeH, cx - eyeW, cy)
                close()
            }
            drawPath(
                path = path,
                color = if (isProcessing) magenta.copy(alpha = 0.5f) else cyan.copy(alpha = 0.4f),
                style = Stroke(width = 3f)
            )

            // Dynamic Iris
            rotate(time * 50f) {
                val irisRadius = coreSize * 0.5f
                for (i in 0..12) {
                    val angle = (i * 30) * (PI / 180)
                    val rMod = if (isProcessing) Random.nextFloat() * 10f else 0f
                    val ix = cx + cos(angle).toFloat() * (irisRadius + rMod)
                    val iy = cy + sin(angle).toFloat() * (irisRadius + rMod)
                    drawLine(
                        color = pureWhite.copy(alpha = 0.8f * pulseAlpha),
                        start = Offset(cx, cy),
                        end = Offset(ix, iy),
                        strokeWidth = 2f
                    )
                }
            }

            // The pupil / singularity
            drawCircle(
                color = pureWhite,
                radius = if (isProcessing) coreSize * 0.2f * Math.abs(erraticGlitch) else coreSize * 0.15f,
                center = Offset(cx, cy)
            )
        }

        // Tesseract / Hypercube Projection (Illusion of 3D)
        val hyperCubeScale = if (isProcessing) 1f + Math.abs(erraticGlitch) * 0.1f else 1f
        scale(hyperCubeScale) {
            val hSize = baseRadius * 0.6f
            val hSizeInner = baseRadius * 0.3f
            val offset3D = baseRadius * 0.15f * sin(time)
            
            val outerCorners = listOf(
                Offset(cx - hSize, cy - hSize),
                Offset(cx + hSize, cy - hSize),
                Offset(cx + hSize, cy + hSize),
                Offset(cx - hSize, cy + hSize)
            )
            
            val innerCorners = listOf(
                Offset(cx - hSizeInner + offset3D, cy - hSizeInner + offset3D),
                Offset(cx + hSizeInner + offset3D, cy - hSizeInner + offset3D),
                Offset(cx + hSizeInner + offset3D, cy + hSizeInner + offset3D),
                Offset(cx - hSizeInner + offset3D, cy + hSizeInner + offset3D)
            )
            
            for (i in 0..3) {
                val next = (i + 1) % 4
                // Outer box
                drawLine(cyan.copy(alpha = 0.2f), outerCorners[i], outerCorners[next], 2f)
                // Inner box
                drawLine(cyan.copy(alpha = 0.4f), innerCorners[i], innerCorners[next], 2f)
                // Connectors
                drawLine(magenta.copy(alpha = 0.3f * pulseAlpha), outerCorners[i], innerCorners[i], 1.5f)
            }
        }

        // Digital Rain / Data streams
        if (isProcessing) {
            for (i in 0..20) {
                val streamX = cx - baseRadius + Random.nextFloat() * (baseRadius * 2)
                val streamY = cy - baseRadius + Random.nextFloat() * (baseRadius * 2)
                drawLine(
                    color = cyan.copy(alpha = Random.nextFloat() * pulseAlpha),
                    start = Offset(streamX, streamY),
                    end = Offset(streamX, streamY + Random.nextFloat() * 40f),
                    strokeWidth = Random.nextFloat() * 4f + 1f
                )
            }
        }
    }
}
