package com.example

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
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val NeonGreen = Color(0xFF00FF88)
private val CyberCyan = Color(0xFF00D2FF)
private val CyberGold = Color(0xFFFFD700)
private val RedDetour = Color(0xFFFF5252)
private val CardSurface = Color(0xFF13131D)
private val CardBorder = Color(0xFF232335)
private val TextLight = Color(0xFFF0F0F8)
private val TextMuted = Color(0xFF8888A2)

data class RouteScenario(
    val hubName: String,
    val appAName: String,
    val destAName: String,
    val payoutA: Double,
    val appBName: String,
    val destBName: String,
    val payoutB: Double,
    val combinedPayout: Double,
    val kmSaved: Double,
    val deviationMeters: Int,
    val fuelSavedBrl: Double
)

@Composable
fun TacticalMergedRouteMapCard(
    modifier: Modifier = Modifier
) {
    var selectedScenarioIndex by remember { mutableIntStateOf(0) }

    val scenarios = remember {
        listOf(
            RouteScenario(
                hubName = "Shopping Paulista",
                appAName = "iFood",
                destAName = "Bela Vista (3.5 km)",
                payoutA = 24.50,
                appBName = "99 Food",
                destBName = "Jardins (4.1 km)",
                payoutB = 19.00,
                combinedPayout = 43.50,
                kmSaved = 2.7,
                deviationMeters = 800,
                fuelSavedBrl = 4.80
            ),
            RouteScenario(
                hubName = "Shopping Eldorado",
                appAName = "iFood",
                destAName = "Pinheiros (2.8 km)",
                payoutA = 22.00,
                appBName = "Rappi",
                destBName = "Vila Madalena (3.4 km)",
                payoutB = 18.50,
                combinedPayout = 40.50,
                kmSaved = 2.2,
                deviationMeters = 650,
                fuelSavedBrl = 3.90
            ),
            RouteScenario(
                hubName = "Av. Faria Lima",
                appAName = "Uber Flash",
                destAName = "Itaim Bibi (2.1 km)",
                payoutA = 17.50,
                appBName = "Zé Delivery",
                destBName = "Vila Olímpia (2.6 km)",
                payoutB = 16.00,
                combinedPayout = 33.50,
                kmSaved = 1.9,
                deviationMeters = 500,
                fuelSavedBrl = 3.40
            )
        )
    }

    val currentScenario = scenarios[selectedScenarioIndex]

    // Animação contínua da pulsação do desvio
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("tactical_merged_route_map_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Brush.horizontalGradient(listOf(CyberCyan.copy(alpha = 0.6f), NeonGreen.copy(alpha = 0.6f)))
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header do Mapa
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
                                Brush.linearGradient(listOf(CyberCyan.copy(alpha = 0.2f), NeonGreen.copy(alpha = 0.2f)))
                            )
                            .border(1.dp, CyberCyan.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = "Mapa Tático",
                            tint = CyberCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "MAPA TÁTICO DE ROTAS MESCLADAS",
                                color = TextLight,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Text(
                            text = "Visualização da sobreposição e economia de trajeto",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(NeonGreen.copy(alpha = 0.15f))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "-43% KM",
                        color = NeonGreen,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Seletor de Cenários de Teste em Campo
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                scenarios.forEachIndexed { index, scn ->
                    val isSelected = index == selectedScenarioIndex
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Color(0xFF1E2638) else Color(0xFF151522))
                            .border(
                                1.dp,
                                if (isSelected) CyberCyan else CardBorder,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedScenarioIndex = index }
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = scn.hubName.replace("Shopping ", "Shop. "),
                            color = if (isSelected) CyberCyan else TextMuted,
                            fontSize = 9.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // CANVAS VETORIAL: DESENHO DAS ROTAS E SOBREPOSIÇÃO
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF090B10))
                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                    val w = size.width
                    val h = size.height

                    // Grid tático de fundo
                    val gridSpacing = 28f
                    var gx = 0f
                    while (gx < w) {
                        drawLine(
                            color = Color(0xFF141824),
                            start = Offset(gx, 0f),
                            end = Offset(gx, h),
                            strokeWidth = 1f
                        )
                        gx += gridSpacing
                    }
                    var gy = 0f
                    while (gy < h) {
                        drawLine(
                            color = Color(0xFF141824),
                            start = Offset(0f, gy),
                            end = Offset(w, gy),
                            strokeWidth = 1f
                        )
                        gy += gridSpacing
                    }

                    // Coordenadas normalizadas dos nós
                    val hubPos = Offset(w * 0.20f, h * 0.50f)
                    val destAPos = Offset(w * 0.65f, h * 0.28f)
                    val destBPos = Offset(w * 0.82f, h * 0.72f)

                    // 1. ROTA A (iFood): Hub -> Destino A (Verde Neon contínuo)
                    val pathA = Path().apply {
                        moveTo(hubPos.x, hubPos.y)
                        quadraticBezierTo(w * 0.40f, h * 0.32f, destAPos.x, destAPos.y)
                    }
                    drawPath(
                        path = pathA,
                        color = NeonGreen,
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // 2. DESVIO ENTRE DESTINO A E DESTINO B (Cyan tracejado pulsante)
                    val pathDeviation = Path().apply {
                        moveTo(destAPos.x, destAPos.y)
                        quadraticBezierTo(w * 0.75f, h * 0.45f, destBPos.x, destBPos.y)
                    }
                    drawPath(
                        path = pathDeviation,
                        color = CyberCyan.copy(alpha = pulseAlpha),
                        style = Stroke(
                            width = 3.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f),
                            cap = StrokeCap.Round
                        )
                    )

                    // 3. ROTA ALTERNATIVA DE DESPERDÍCIO (Sem mesclagem: teria que voltar ao ponto inicial)
                    val pathWaste = Path().apply {
                        moveTo(destAPos.x, destAPos.y)
                        quadraticBezierTo(w * 0.50f, h * 0.80f, hubPos.x, hubPos.y)
                    }
                    drawPath(
                        path = pathWaste,
                        color = Color(0xFF33334A),
                        style = Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 12f), 0f)
                        )
                    )

                    // NÓ DE ORIGEM (SHOPPING / POLO DE COLETA)
                    drawCircle(
                        color = CyberGold.copy(alpha = 0.25f),
                        radius = 16.dp.toPx(),
                        center = hubPos
                    )
                    drawCircle(
                        color = CyberGold,
                        radius = 8.dp.toPx(),
                        center = hubPos
                    )

                    // NÓ DE ENTREGA A
                    drawCircle(
                        color = NeonGreen.copy(alpha = 0.25f),
                        radius = 14.dp.toPx(),
                        center = destAPos
                    )
                    drawCircle(
                        color = NeonGreen,
                        radius = 6.dp.toPx(),
                        center = destAPos
                    )

                    // NÓ DE ENTREGA B
                    drawCircle(
                        color = CyberCyan.copy(alpha = 0.25f),
                        radius = 14.dp.toPx(),
                        center = destBPos
                    )
                    drawCircle(
                        color = CyberCyan,
                        radius = 6.dp.toPx(),
                        center = destBPos
                    )
                }

                // Legendas de texto sobrepostas no Canvas
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                ) {
                    Text(
                        text = "● COLETA ÚNICA: ${currentScenario.hubName}",
                        color = CyberGold,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "● 1ª ENTREGA (${currentScenario.appAName}): R$ ${String.format(java.util.Locale("pt", "BR"), "%.2f", currentScenario.payoutA)}",
                        color = NeonGreen,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "● 2ª ENTREGA (${currentScenario.appBName}): R$ ${String.format(java.util.Locale("pt", "BR"), "%.2f", currentScenario.payoutB)} (+${currentScenario.deviationMeters}m)",
                        color = CyberCyan,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Badge de Economia de Trajeto no rodapé do Canvas
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF0F2016))
                        .border(1.dp, NeonGreen.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "⚡ Economia: ${currentScenario.kmSaved} km poupados",
                        color = NeonGreen,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Resumo Financeiro da Rota Otimizada
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "TOTAL FATURADO", color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = "R$ ${String.format(java.util.Locale("pt", "BR"), "%.2f", currentScenario.combinedPayout)}",
                        color = CyberGold,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Column {
                    Text(text = "DESVIO DE ROTA", color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = "+${currentScenario.deviationMeters} metros",
                        color = CyberCyan,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Column {
                    Text(text = "GASOLINA POUPADA", color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = "R$ ${String.format(java.util.Locale("pt", "BR"), "%.2f", currentScenario.fuelSavedBrl)}",
                        color = NeonGreen,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}
