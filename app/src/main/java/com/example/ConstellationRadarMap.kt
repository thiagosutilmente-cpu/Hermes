package com.example

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

/**
 * Representação de um nó no mapa neural de constelação
 */
data class ConstellationNode(
    val id: String,
    val label: String,
    val icon: String,
    val relX: Float, // 0.0f a 1.0f relativo à largura do canvas
    val relY: Float, // 0.0f a 1.0f relativo à altura do canvas
    val appColor: Color,
    val isUser: Boolean = false,
    val isGhostPredicted: Boolean = false,
    val estimatedValue: Double? = null,
    val distanceKm: Double? = null
)

/**
 * MAPA DE CONSTELAÇÃO INTERATIVO (RADAR NEURAL 360°)
 * Exibe os nós dos estabelecimentos parceiros, o ponto atual do entregador (🏍️),
 * traçado de rotas otimizadas e a sequência de previsão Ghost (83% chance em 3 min).
 */
@Composable
fun ConstellationRadarMap(
    offers: List<RadarOffer>,
    modifier: Modifier = Modifier,
    onNodeSelected: ((ConstellationNode) -> Unit)? = null
) {
    var selectedNode by remember { mutableStateOf<ConstellationNode?>(null) }

    val infiniteTransition = rememberInfiniteTransition(label = "RadarSweepTransition")

    // Animação de rotação da linha de varredura do radar
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SweepAngle"
    )

    // Animação de pulso do anel do usuário e nós
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    // Nós estáticos combinados com as ofertas ativas
    val nodes = remember(offers) {
        listOf(
            ConstellationNode("user", "Você", "🏍️", 0.50f, 0.52f, NeonGreen, isUser = true),
            ConstellationNode("bk", "BK Paulista", "🍔", 0.28f, 0.30f, RedIFood, estimatedValue = 33.0, distanceKm = 1.2),
            ConstellationNode("ph", "Pizza Hut Jardins", "🍕", 0.72f, 0.32f, OrangeRappi, estimatedValue = 18.0, distanceKm = 2.4),
            ConstellationNode("stb", "Starbucks Frei Caneca", "☕", 0.32f, 0.76f, Color(0xFF00704A), estimatedValue = 16.5, distanceKm = 1.8),
            ConstellationNode("dest_res", "Residencial Jardins", "🏠", 0.80f, 0.68f, Yellow99, estimatedValue = 22.0, distanceKm = 3.5),
            ConstellationNode("ghost", "Ghost: 83% Stack", "👻", 0.62f, 0.18f, Color(0xFF00D2FF), isGhostPredicted = true)
        )
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, DarkBorder, RoundedCornerShape(22.dp))
            .testTag("constellation_radar_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Top Header do Mapa
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🛰️", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "MAPA DE CONSTELAÇÃO NEURAL",
                            color = TextLight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = "Varredura 360° • Alcance 5.0 km",
                            color = NeonGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Badge Ghost Sequence
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF00D2FF).copy(alpha = 0.15f))
                        .border(1.dp, Color(0xFF00D2FF).copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "👻 83% GHOST",
                        color = Color(0xFF00D2FF),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Canvas Gráfico do Radar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF06060A))
                    .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxWidth().height(230.dp)) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val maxRadius = minOf(size.width, size.height) * 0.44f

                    // 1. Círculos concêntricos de alcance (1km, 2.5km, 5km)
                    val radii = listOf(maxRadius * 0.33f, maxRadius * 0.66f, maxRadius)
                    val dashedEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)

                    radii.forEachIndexed { index, r ->
                        drawCircle(
                            color = DarkBorder,
                            radius = r,
                            center = center,
                            style = Stroke(width = 1.2f, pathEffect = dashedEffect)
                        )
                    }

                    // 2. Linhas de cruzamento cartesiano
                    drawLine(
                        color = DarkBorder.copy(alpha = 0.7f),
                        start = Offset(center.x, center.y - maxRadius),
                        end = Offset(center.x, center.y + maxRadius),
                        strokeWidth = 1f
                    )
                    drawLine(
                        color = DarkBorder.copy(alpha = 0.7f),
                        start = Offset(center.x - maxRadius, center.y),
                        end = Offset(center.x + maxRadius, center.y),
                        strokeWidth = 1f
                    )

                    // 3. Eixo de Varredura Radar em Rotação
                    val rad = Math.toRadians(sweepAngle.toDouble())
                    val sweepEnd = Offset(
                        (center.x + maxRadius * Math.cos(rad)).toFloat(),
                        (center.y + maxRadius * Math.sin(rad)).toFloat()
                    )

                    drawLine(
                        brush = Brush.linearGradient(
                            colors = listOf(NeonGreen.copy(alpha = 0.8f), Color.Transparent),
                            start = center,
                            end = sweepEnd
                        ),
                        start = center,
                        end = sweepEnd,
                        strokeWidth = 2f
                    )

                    // 4. Linhas de Rota Neural Conectando Nós (BK -> PH -> Destino)
                    val bkPos = Offset(size.width * 0.28f, size.height * 0.30f)
                    val phPos = Offset(size.width * 0.72f, size.height * 0.32f)
                    val destPos = Offset(size.width * 0.80f, size.height * 0.68f)

                    // Rota Multi-Stack Recomendada (BK -> PH -> Destino)
                    drawLine(
                        color = NeonGreen.copy(alpha = 0.5f),
                        start = center,
                        end = bkPos,
                        strokeWidth = 2.5f,
                        pathEffect = dashedEffect
                    )
                    drawLine(
                        color = NeonGreen.copy(alpha = 0.7f),
                        start = bkPos,
                        end = phPos,
                        strokeWidth = 3f
                    )
                    drawLine(
                        color = NeonGreen.copy(alpha = 0.7f),
                        start = phPos,
                        end = destPos,
                        strokeWidth = 3f
                    )
                }

                // Renderização dos Nós Interativos Sobrepostos
                nodes.forEach { node ->
                    val isSelected = selectedNode?.id == node.id
                    val nodeBorder = if (node.isUser) NeonGreen else if (node.isGhostPredicted) Color(0xFF00D2FF) else node.appColor

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(
                                start = (node.relX * 280).dp,
                                top = (node.relY * 180).dp
                            )
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkCardElevated.copy(alpha = 0.85f))
                            .border(
                                width = if (isSelected || node.isUser) 1.5.dp else 1.dp,
                                color = nodeBorder,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                selectedNode = node
                                onNodeSelected?.invoke(node)
                            }
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = node.icon, fontSize = if (node.isUser) 13.sp else 11.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = node.label.split(" ").firstOrNull() ?: "",
                                color = if (node.isUser) NeonGreen else TextLight,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Legenda Inferior de Nós e Status da Sequência Fantasma
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Rota Multi-Stack: ● BK ➔ ● PH ➔ 🏠",
                    color = NeonGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Próx. Chamada: ~3 min",
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }
        }
    }
}
