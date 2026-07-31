package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.random.Random

private data class Particle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val color: Color,
    val size: Float,
    val rotation: Float,
    val rotSpeed: Float
)

@Composable
fun ConfettiAnimation(
    modifier: Modifier = Modifier,
    durationMillis: Int = 3000,
    onAnimationEnd: () -> Unit = {}
) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = durationMillis, easing = LinearEasing)
        )
        onAnimationEnd()
    }

    val particles = remember {
        val colors = listOf(
            Color(0xFF3A86FF), // AccentBlue
            Color(0xFF00F5D4), // AccentGreen
            Color(0xFFFF006E), // AccentPink
            Color(0xFFFFBE0B), // Yellow
            Color(0xFF8338EC)  // Purple
        )
        List(150) {
            Particle(
                x = Random.nextFloat(), // relative 0 to 1
                y = Random.nextFloat() * -0.2f, // start slightly above
                vx = (Random.nextFloat() - 0.5f) * 1200f,
                vy = Random.nextFloat() * 800f + 200f,
                color = colors.random(),
                size = Random.nextFloat() * 20f + 10f,
                rotation = Random.nextFloat() * 360f,
                rotSpeed = (Random.nextFloat() - 0.5f) * 720f
            )
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val t = progress.value

        particles.forEach { p ->
            val currentX = p.x * width + p.vx * t
            // Add gravity (quadratic)
            val currentY = p.y * height + p.vy * t + 1000f * t * t
            
            val alpha = (1f - (t * 1.2f)).coerceIn(0f, 1f)
            if (alpha > 0f) {
                withTransform({
                    translate(left = currentX, top = currentY)
                    rotate(degrees = p.rotation + p.rotSpeed * t)
                }) {
                    drawRect(
                        color = p.color.copy(alpha = alpha),
                        size = Size(p.size, p.size)
                    )
                }
            }
        }
    }
}
