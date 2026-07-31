package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.coordinator.RadarCoordinator
import com.example.coordinator.RadarSettings
import com.example.coordinator.ActiveOffer
import com.example.util.RouteOptimizer
import com.example.util.StopPoint
import com.example.util.StopType
import com.example.AccentAmber
import com.example.AccentBlue
import com.example.AccentGreen
import com.example.AccentRed
import com.example.CardSlateBg
import com.example.TextDim
import com.example.TextLight
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

data class ChartDayData(
    val dateLabel: String,
    val earnings: Double,
    val savings: Double,
    val acceptedCount: Int,
    val rejectedCount: Int,
    val totalKm: Double
)

data class ForecastDayData(
    val dateLabel: String,
    val baselineEarnings: Double,
    val potentialEarnings: Double,
    val percentageGain: Double,
    val recommendedRoutesCount: Int
)

@Composable
fun D3InteractiveEfficiencyChart(
    activeOffer: ActiveOffer,
    settings: RadarSettings,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    val offerDistance = activeOffer.totalDistance
    val offerFare = activeOffer.fareValue

    val rawMaxX = maxOf(offerDistance * 1.4, 15.0)
    val maxX = if (rawMaxX.isNaN() || rawMaxX.isInfinite()) 15.0 else rawMaxX
    
    val rawMaxY = maxOf(offerFare * 1.4, 40.0)
    val maxY = if (rawMaxY.isNaN() || rawMaxY.isInfinite()) 40.0 else rawMaxY

    var touchPoint by remember { mutableStateOf<Offset?>(null) }
    var isInteracting by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 6f,
        targetValue = 14f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "radius"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    val gridColor = Color.Gray.copy(alpha = 0.15f)
    val fontColor = Color.Gray
    val thresholdLineColor = AccentAmber
    val minFareLineColor = Color.Magenta.copy(alpha = 0.5f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .background(CardSlateBg, RoundedCornerShape(20.dp))
            .border(1.dp, Color.Gray.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
            .padding(8.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(activeOffer) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        touchPoint = down.position
                        isInteracting = true
                        down.consume()

                        while (true) {
                            val event = awaitPointerEvent()
                            val anyPressed = event.changes.any { it.pressed }
                            if (anyPressed) {
                                val pointer = event.changes.firstOrNull { it.pressed } ?: event.changes.first()
                                touchPoint = pointer.position
                                pointer.consume()
                            } else {
                                touchPoint = null
                                isInteracting = false
                                break
                            }
                        }
                    }
                }
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            val marginLeft = with(density) { 48.dp.toPx() }
            val marginBottom = with(density) { 36.dp.toPx() }
            val marginRight = with(density) { 16.dp.toPx() }
            val marginTop = with(density) { 16.dp.toPx() }

            val plotWidth = canvasWidth - marginLeft - marginRight
            val plotHeight = canvasHeight - marginTop - marginBottom

            if (plotWidth > 0f && plotHeight > 0f) {
                fun getPixelX(x: Double): Float = marginLeft + ((x / maxX) * plotWidth).toFloat()
                fun getPixelY(y: Double): Float = marginTop + plotHeight - ((y / maxY) * plotHeight).toFloat()

                val acceptPath = Path().apply {
                    val intersectX = if (settings.minValuePerKm > 0.0) settings.minFareValue / settings.minValuePerKm else 0.0
                    moveTo(getPixelX(intersectX), getPixelY(settings.minFareValue))
                    lineTo(getPixelX(maxX), getPixelY(settings.minFareValue))
                    lineTo(getPixelX(maxX), getPixelY(maxY))

                    val topEfficiencyY = maxX * settings.minValuePerKm
                    if (topEfficiencyY > maxY) {
                        val boundaryX = maxY / settings.minValuePerKm
                        lineTo(getPixelX(boundaryX), getPixelY(maxY))
                    } else {
                        lineTo(getPixelX(maxX), getPixelY(topEfficiencyY))
                    }
                    close()
                }

                drawPath(
                    path = acceptPath,
                    color = AccentGreen.copy(alpha = 0.06f)
                )

                val baseYStep = if (maxY > 50) 15.0 else 10.0
                val yStep = maxOf(baseYStep, maxY / 20.0)
                var yVal = 0.0
                while (yVal <= maxY) {
                    val py = getPixelY(yVal)
                    drawLine(
                        color = gridColor,
                        start = Offset(marginLeft, py),
                        end = Offset(canvasWidth - marginRight, py),
                        strokeWidth = 1f
                    )
                    drawText(
                        textMeasurer = textMeasurer,
                        text = "R$ ${yVal.toInt()}",
                        style = TextStyle(color = fontColor, fontSize = 9.sp),
                        topLeft = Offset(8f, py - 12f)
                    )
                    yVal += yStep
                }

                val baseXStep = if (maxX > 20) 5.0 else 2.0
                val xStep = maxOf(baseXStep, maxX / 20.0)
                var xVal = 0.0
                while (xVal <= maxX) {
                    val px = getPixelX(xVal)
                    drawLine(
                        color = gridColor,
                        start = Offset(px, marginTop),
                        end = Offset(px, marginTop + plotHeight),
                        strokeWidth = 1f
                    )
                    drawText(
                        textMeasurer = textMeasurer,
                        text = "${xVal.toInt()}km",
                        style = TextStyle(color = fontColor, fontSize = 9.sp),
                        topLeft = Offset(px - 20f, marginTop + plotHeight + 4f)
                    )
                    xVal += xStep
                }

                val slopeEndX = minOf(maxX, if (settings.minValuePerKm > 0.0) maxY / settings.minValuePerKm else maxX)
                val slopeEndY = slopeEndX * settings.minValuePerKm
                drawLine(
                    color = thresholdLineColor.copy(alpha = 0.8f),
                    start = Offset(getPixelX(0.0), getPixelY(0.0)),
                    end = Offset(getPixelX(slopeEndX), getPixelY(slopeEndY)),
                    strokeWidth = 3f
                )

                if (settings.minFareValue > 0.0 && settings.minFareValue <= maxY) {
                    val pyMinFare = getPixelY(settings.minFareValue)
                    drawLine(
                        color = minFareLineColor,
                        start = Offset(marginLeft, pyMinFare),
                        end = Offset(canvasWidth - marginRight, pyMinFare),
                        strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                }

                val currentOfferPx = getPixelX(offerDistance)
                val currentOfferPy = getPixelY(offerFare)

                drawCircle(
                    color = AccentBlue.copy(alpha = pulseAlpha),
                    radius = pulseRadius,
                    center = Offset(currentOfferPx, currentOfferPy)
                )
                drawCircle(
                    color = AccentBlue,
                    radius = 5f,
                    center = Offset(currentOfferPx, currentOfferPy)
                )

                if (isInteracting) {
                    touchPoint?.let { pos ->
                        if (pos.x >= marginLeft && pos.x <= canvasWidth - marginRight &&
                            pos.y >= marginTop && pos.y <= marginTop + plotHeight) {

                            drawLine(
                                color = AccentBlue.copy(alpha = 0.4f),
                                start = Offset(pos.x, marginTop),
                                end = Offset(pos.x, marginTop + plotHeight),
                                strokeWidth = 1.5f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                            )
                            drawLine(
                                color = AccentBlue.copy(alpha = 0.4f),
                                start = Offset(marginLeft, pos.y),
                                end = Offset(canvasWidth - marginRight, pos.y),
                                strokeWidth = 1.5f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                            )

                            val touchX = ((pos.x - marginLeft) / plotWidth) * maxX
                            val touchY = ((marginTop + plotHeight - pos.y) / plotHeight) * maxY

                            val touchEfficiency = if (touchX > 0.0) touchY / touchX else 0.0
                            val isTouchPointAcceptable = touchY >= settings.minFareValue && touchEfficiency >= settings.minValuePerKm

                            val tooltipText = String.format(Locale.US, "%.1f km | R$ %.2f\n%.2f R$/km\n%s",
                                touchX, touchY, touchEfficiency, if (isTouchPointAcceptable) "ACEITÁVEL" else "RECUSADO")

                            val textLayoutResult = textMeasurer.measure(
                                text = tooltipText,
                                style = TextStyle(color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            )

                            val tooltipWidth = textLayoutResult.size.width + 16f
                            val tooltipHeight = textLayoutResult.size.height + 12f

                            var tooltipX = pos.x - tooltipWidth / 2f
                            var tooltipY = pos.y - tooltipHeight - 16f

                            tooltipX = tooltipX.coerceIn(marginLeft, canvasWidth - marginRight - tooltipWidth)
                            tooltipY = tooltipY.coerceIn(marginTop, marginTop + plotHeight - tooltipHeight)

                            drawRoundRect(
                                color = CardSlateBg.copy(alpha = 0.9f),
                                topLeft = Offset(tooltipX, tooltipY),
                                size = Size(tooltipWidth, tooltipHeight),
                                cornerRadius = CornerRadius(6f, 6f)
                            )
                            drawRoundRect(
                                color = if (isTouchPointAcceptable) AccentGreen else AccentRed,
                                topLeft = Offset(tooltipX, tooltipY),
                                size = Size(tooltipWidth, tooltipHeight),
                                cornerRadius = CornerRadius(6f, 6f),
                                style = Stroke(width = 2f)
                            )

                            drawText(
                                textLayoutResult = textLayoutResult,
                                topLeft = Offset(tooltipX + 8f, tooltipY + 6f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EarningsAndSavingsOptimizationChart(
    dailyReport: List<com.example.api.DailyReportItem>,
    settings: RadarSettings,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    var selectedViewTab by remember { mutableStateOf(0) } // 0 = Histórico, 1 = Previsão IA

    val chartItems = remember(dailyReport) {
        if (dailyReport.isNotEmpty()) {
            dailyReport.map { item ->
                val dateParts = item.date.split("/")
                val shortDate = if (dateParts.size >= 2) "${dateParts[0]}/${dateParts[1]}" else item.date
                val calculatedSavings = (item.totalOffersRejected * 2.50) + (item.totalOffersAccepted * 4.50)
                ChartDayData(
                    dateLabel = shortDate,
                    earnings = item.estimatedEarnings,
                    savings = calculatedSavings,
                    acceptedCount = item.totalOffersAccepted,
                    rejectedCount = item.totalOffersRejected,
                    totalKm = item.totalDistanceKm
                )
            }.reversed()
        } else {
            listOf(
                ChartDayData("Seg", 120.0, 32.50, 8, 12, 48.0),
                ChartDayData("Ter", 145.5, 41.20, 10, 15, 54.0),
                ChartDayData("Qua", 110.0, 28.00, 7, 10, 42.0),
                ChartDayData("Qui", 168.0, 49.80, 11, 18, 62.0),
                ChartDayData("Sex", 195.0, 58.60, 13, 22, 75.0),
                ChartDayData("Sáb", 240.0, 74.30, 16, 25, 92.0),
                ChartDayData("Dom", 185.0, 52.00, 12, 17, 70.0)
            )
        }
    }

    val routeOptimizationEfficiency = remember(settings) {
        val baseEff = 0.12
        val filterBonus = ((settings.minValuePerKm - 1.5) * 0.08).coerceIn(0.0, 0.20)
        val autoRejectBonus = if (settings.isAutoRejectEnabled) 0.10 else 0.02
        val distanceBonus = if (settings.maxPickupDistanceKm <= 4.0) 0.04 else 0.01
        baseEff + filterBonus + autoRejectBonus + distanceBonus
    }

    val forecastItems = remember(chartItems, routeOptimizationEfficiency) {
        val avgHistoricalEarnings = if (chartItems.isNotEmpty()) {
            chartItems.map { it.earnings }.average()
        } else {
            160.0
        }
        val baseMean = if (avgHistoricalEarnings.isNaN() || avgHistoricalEarnings < 20.0) 160.0 else avgHistoricalEarnings

        val futureLabels = listOf("Seg+", "Ter+", "Qua+", "Qui+", "Sex+", "Sáb+", "Dom+")
        futureLabels.mapIndexed { idx, label ->
            val dayOfWeekFactor = when (idx) {
                4 -> 1.15
                5 -> 1.35
                6 -> 1.20
                else -> 0.90
            }
            val dailyBaseline = baseMean * dayOfWeekFactor
            val dailyPotential = dailyBaseline * (1.0 + routeOptimizationEfficiency)
            
            ForecastDayData(
                dateLabel = label,
                baselineEarnings = dailyBaseline,
                potentialEarnings = dailyPotential,
                percentageGain = routeOptimizationEfficiency * 100.0,
                recommendedRoutesCount = (8 + (dayOfWeekFactor * 6).toInt()).coerceAtLeast(5)
            )
        }
    }

    val maxEarnings = chartItems.maxOfOrNull { it.earnings } ?: 100.0
    val maxSavings = chartItems.maxOfOrNull { it.savings } ?: 50.0
    val maxValHist = maxOf(maxEarnings, maxSavings)
    val maxYHist = maxOf(maxValHist * 1.15, 100.0)

    val maxForecastPot = forecastItems.maxOfOrNull { it.potentialEarnings } ?: 100.0
    val maxForecastBase = forecastItems.maxOfOrNull { it.baselineEarnings } ?: 100.0
    val maxValForecast = maxOf(maxForecastPot, maxForecastBase)
    val maxYForecast = maxOf(maxValForecast * 1.15, 100.0)

    val maxY = if (selectedViewTab == 0) maxYHist else maxYForecast

    var touchPoint by remember { mutableStateOf<Offset?>(null) }
    var isInteracting by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(CardSlateBg, RoundedCornerShape(20.dp))
            .border(1.dp, Color.Gray.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "PILOTO DE INTELIGÊNCIA ARTIFICIAL",
                        color = Color.Gray,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = if (selectedViewTab == 0) "Histórico Otimizado de Operação" else "Previsão & Prospecção Futura",
                        color = TextLight,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("Histórico", "Previsão").forEachIndexed { idx, title ->
                        val isSelected = selectedViewTab == idx
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) AccentBlue.copy(alpha = 0.25f) else Color.Transparent)
                                .border(1.dp, if (isSelected) AccentBlue.copy(alpha = 0.6f) else Color.Transparent, RoundedCornerShape(16.dp))
                                .clickable { selectedViewTab = idx }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = title,
                                color = if (isSelected) TextLight else Color.Gray,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (selectedViewTab == 0) {
                    val totalEarnings = chartItems.sumOf { it.earnings }
                    val totalSavings = chartItems.sumOf { it.savings }
                    val acceptedTotal = chartItems.sumOf { it.acceptedCount }
                    val rejectedTotal = chartItems.sumOf { it.rejectedCount }
                    val totalOffers = acceptedTotal + rejectedTotal
                    val efficiencyPercent = if (totalOffers > 0) (rejectedTotal.toDouble() / totalOffers * 100.0).coerceAtMost(85.0) else 45.0

                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("Faturamento", color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Text(String.format(Locale.US, "R$ %.2f", totalEarnings), color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider(modifier = Modifier.height(20.dp).width(1.dp).align(Alignment.CenterVertically), color = Color.Gray.copy(alpha = 0.15f))
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("Poupado (IA)", color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Text(String.format(Locale.US, "R$ %.2f", totalSavings), color = AccentBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider(modifier = Modifier.height(20.dp).width(1.dp).align(Alignment.CenterVertically), color = Color.Gray.copy(alpha = 0.15f))
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("Filtro Eficaz", color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Text(String.format(Locale.US, "%.1f%%", efficiencyPercent), color = AccentAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    val totalForecasted = forecastItems.sumOf { it.potentialEarnings }
                    val totalBaseline = forecastItems.sumOf { it.baselineEarnings }
                    val totalIncremental = totalForecasted - totalBaseline
                    val efficiencyPercent = routeOptimizationEfficiency * 100.0

                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("Otimização de Rota", color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Text(String.format(Locale.US, "+%.1f%%", efficiencyPercent), color = AccentAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider(modifier = Modifier.height(20.dp).width(1.dp).align(Alignment.CenterVertically), color = Color.Gray.copy(alpha = 0.15f))
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("Projeção (7 Dias)", color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Text(String.format(Locale.US, "R$ %.2f", totalForecasted), color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider(modifier = Modifier.height(20.dp).width(1.dp).align(Alignment.CenterVertically), color = Color.Gray.copy(alpha = 0.15f))
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("Lucro Extra IA", color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Text(String.format(Locale.US, "+R$ %.2f", totalIncremental), color = AccentBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectedViewTab == 0) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Box(modifier = Modifier.size(8.dp).background(AccentGreen, RoundedCornerShape(1.5.dp)))
                        Text("Ganhos Reais", color = Color.LightGray, fontSize = 8.5.sp)
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(8.dp).background(AccentBlue, RoundedCornerShape(4.dp)))
                        Text("Economia IA", color = Color.LightGray, fontSize = 8.5.sp)
                    }
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Box(modifier = Modifier.size(8.dp).background(AccentAmber, RoundedCornerShape(4.dp)))
                        Text("Potencial Otimizado", color = Color.LightGray, fontSize = 8.5.sp)
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(8.dp).background(Color.Gray, RoundedCornerShape(4.dp)))
                        Text("Baseline (Sem IA)", color = Color.LightGray, fontSize = 8.5.sp)
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(selectedViewTab, chartItems, forecastItems) {
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                touchPoint = down.position
                                isInteracting = true
                                down.consume()

                                while (true) {
                                    val event = awaitPointerEvent()
                                    val anyPressed = event.changes.any { it.pressed }
                                    if (anyPressed) {
                                        val pointer = event.changes.firstOrNull { it.pressed } ?: event.changes.first()
                                        touchPoint = pointer.position
                                        pointer.consume()
                                    } else {
                                        touchPoint = null
                                        isInteracting = false
                                        break
                                    }
                                }
                            }
                        }
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height

                    val marginLeft = with(density) { 42.dp.toPx() }
                    val marginBottom = with(density) { 20.dp.toPx() }
                    val marginRight = with(density) { 8.dp.toPx() }
                    val marginTop = with(density) { 8.dp.toPx() }

                    val plotWidth = canvasWidth - marginLeft - marginRight
                    val plotHeight = canvasHeight - marginTop - marginBottom

                    if (plotWidth > 0f && plotHeight > 0f) {
                        fun getPixelY(value: Double): Float =
                            marginTop + plotHeight - ((value / maxY) * plotHeight).toFloat()

                        val gridLineCount = 4
                        for (i in 0..gridLineCount) {
                            val ratio = i.toFloat() / gridLineCount
                            val yVal = ratio * maxY
                            val py = getPixelY(yVal)

                            drawLine(
                                color = Color.Gray.copy(alpha = 0.1f),
                                start = Offset(marginLeft, py),
                                end = Offset(canvasWidth - marginRight, py),
                                strokeWidth = 1f
                            )

                            drawText(
                                textMeasurer = textMeasurer,
                                text = "R$ ${yVal.toInt()}",
                                style = TextStyle(color = Color.Gray, fontSize = 8.sp),
                                topLeft = Offset(4f, py - 6.dp.toPx())
                            )
                        }

                        if (selectedViewTab == 0) {
                            val colWidth = plotWidth / chartItems.size
                            val savingsPoints = mutableListOf<Offset>()

                            chartItems.forEachIndexed { idx, day ->
                                val centerX = marginLeft + (idx * colWidth) + (colWidth / 2f)

                                val barWidth = colWidth * 0.45f
                                val barHeight = ((day.earnings / maxY) * plotHeight).toFloat()
                                val barTop = marginTop + plotHeight - barHeight

                                drawRoundRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            AccentGreen,
                                            AccentGreen.copy(alpha = 0.2f)
                                        )
                                    ),
                                    topLeft = Offset(centerX - barWidth / 2f, barTop),
                                    size = Size(barWidth, barHeight),
                                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                                )

                                val pySavings = getPixelY(day.savings)
                                savingsPoints.add(Offset(centerX, pySavings))

                                drawText(
                                    textMeasurer = textMeasurer,
                                    text = day.dateLabel,
                                    style = TextStyle(color = Color.LightGray, fontSize = 8.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                                    topLeft = Offset(centerX - 12.dp.toPx(), marginTop + plotHeight + 4.dp.toPx())
                                )
                            }

                            if (savingsPoints.isNotEmpty()) {
                                val linePath = Path().apply {
                                    moveTo(savingsPoints[0].x, savingsPoints[0].y)
                                    for (i in 1 until savingsPoints.size) {
                                        val prev = savingsPoints[i - 1]
                                        val curr = savingsPoints[i]
                                        val controlX = (prev.x + curr.x) / 2f
                                        cubicTo(controlX, prev.y, controlX, curr.y, curr.x, curr.y)
                                    }
                                }

                                drawPath(
                                    path = linePath,
                                    color = AccentBlue.copy(alpha = 0.7f),
                                    style = Stroke(width = 2.5.dp.toPx())
                                )

                                savingsPoints.forEach { point ->
                                    drawCircle(
                                        color = CardSlateBg,
                                        radius = 4.dp.toPx(),
                                        center = point
                                    )
                                    drawCircle(
                                        color = AccentBlue,
                                        radius = 2.5.dp.toPx(),
                                        center = point
                                    )
                                }
                            }

                            if (isInteracting) {
                                touchPoint?.let { pos ->
                                    if (pos.x >= marginLeft && pos.x <= canvasWidth - marginRight) {
                                        val hoveredIdx = (((pos.x - marginLeft) / plotWidth) * chartItems.size)
                                            .toInt()
                                            .coerceIn(0, chartItems.size - 1)

                                        val dayData = chartItems[hoveredIdx]
                                        val cursorX = marginLeft + (hoveredIdx * colWidth) + (colWidth / 2f)

                                        drawLine(
                                            color = AccentBlue.copy(alpha = 0.4f),
                                            start = Offset(cursorX, marginTop),
                                            end = Offset(cursorX, marginTop + plotHeight),
                                            strokeWidth = 1.dp.toPx(),
                                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                                        )

                                        val tooltipText = String.format(
                                            Locale.US,
                                            "%s\nGanhos: R$ %.2f\nSalvo IA: R$ %.2f\nRotas: %da / %dr\nDist.: %.1f km",
                                            dayData.dateLabel,
                                            dayData.earnings,
                                            dayData.savings,
                                            dayData.acceptedCount,
                                            dayData.rejectedCount,
                                            dayData.totalKm
                                        )

                                        val textLayoutResult = textMeasurer.measure(
                                            text = tooltipText,
                                            style = TextStyle(color = Color.White, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, lineHeight = 11.sp)
                                        )

                                        val tooltipWidth = textLayoutResult.size.width + 12f
                                        val tooltipHeight = textLayoutResult.size.height + 10f

                                        var tooltipX = cursorX - tooltipWidth / 2f
                                        val tooltipY = marginTop + 6f

                                        tooltipX = tooltipX.coerceIn(marginLeft, canvasWidth - marginRight - tooltipWidth)

                                        drawRoundRect(
                                            color = Color(0xFF0F172A).copy(alpha = 0.95f),
                                            topLeft = Offset(tooltipX, tooltipY),
                                            size = Size(tooltipWidth, tooltipHeight),
                                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                                            style = androidx.compose.ui.graphics.drawscope.Fill
                                        )
                                        drawRoundRect(
                                            color = AccentBlue,
                                            topLeft = Offset(tooltipX, tooltipY),
                                            size = Size(tooltipWidth, tooltipHeight),
                                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                                            style = Stroke(width = 1.dp.toPx())
                                        )

                                        drawText(
                                            textLayoutResult = textLayoutResult,
                                            topLeft = Offset(tooltipX + 6f, tooltipY + 5f)
                                        )
                                    }
                                }
                            }
                        } else {
                            val colWidth = plotWidth / forecastItems.size
                            val baselinePoints = mutableListOf<Offset>()
                            val potentialPoints = mutableListOf<Offset>()

                            forecastItems.forEachIndexed { idx, day ->
                                val centerX = marginLeft + (idx * colWidth) + (colWidth / 2f)

                                val pyBase = getPixelY(day.baselineEarnings)
                                val pyPot = getPixelY(day.potentialEarnings)

                                baselinePoints.add(Offset(centerX, pyBase))
                                potentialPoints.add(Offset(centerX, pyPot))

                                drawText(
                                    textMeasurer = textMeasurer,
                                    text = day.dateLabel,
                                    style = TextStyle(color = Color.LightGray, fontSize = 8.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                                    topLeft = Offset(centerX - 12.dp.toPx(), marginTop + plotHeight + 4.dp.toPx())
                                )
                            }

                            if (potentialPoints.isNotEmpty()) {
                                val areaPath = Path().apply {
                                    moveTo(potentialPoints[0].x, potentialPoints[0].y)
                                    for (i in 1 until potentialPoints.size) {
                                        val prev = potentialPoints[i - 1]
                                        val curr = potentialPoints[i]
                                        val controlX = (prev.x + curr.x) / 2f
                                        cubicTo(controlX, prev.y, controlX, curr.y, curr.x, curr.y)
                                    }
                                    lineTo(potentialPoints.last().x, marginTop + plotHeight)
                                    lineTo(potentialPoints.first().x, marginTop + plotHeight)
                                    close()
                                }

                                drawPath(
                                    path = areaPath,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            AccentAmber.copy(alpha = 0.25f),
                                            Color.Transparent
                                        )
                                    ),
                                    style = androidx.compose.ui.graphics.drawscope.Fill
                                )

                                val linePathPot = Path().apply {
                                    moveTo(potentialPoints[0].x, potentialPoints[0].y)
                                    for (i in 1 until potentialPoints.size) {
                                        val prev = potentialPoints[i - 1]
                                        val curr = potentialPoints[i]
                                        val controlX = (prev.x + curr.x) / 2f
                                        cubicTo(controlX, prev.y, controlX, curr.y, curr.x, curr.y)
                                    }
                                }

                                drawPath(
                                    path = linePathPot,
                                    color = AccentAmber,
                                    style = Stroke(width = 2.5.dp.toPx())
                                )

                                val linePathBase = Path().apply {
                                    moveTo(baselinePoints[0].x, baselinePoints[0].y)
                                    for (i in 1 until baselinePoints.size) {
                                        val prev = baselinePoints[i - 1]
                                        val curr = baselinePoints[i]
                                        val controlX = (prev.x + curr.x) / 2f
                                        cubicTo(controlX, prev.y, controlX, curr.y, curr.x, curr.y)
                                    }
                                }

                                drawPath(
                                    path = linePathBase,
                                    color = Color.Gray.copy(alpha = 0.7f),
                                    style = Stroke(
                                        width = 1.5.dp.toPx(),
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                                    )
                                )

                                potentialPoints.forEach { point ->
                                    drawCircle(
                                        color = CardSlateBg,
                                        radius = 4.dp.toPx(),
                                        center = point
                                    )
                                    drawCircle(
                                        color = AccentAmber,
                                        radius = 2.5.dp.toPx(),
                                        center = point
                                    )
                                }
                            }

                            if (isInteracting) {
                                touchPoint?.let { pos ->
                                    if (pos.x >= marginLeft && pos.x <= canvasWidth - marginRight) {
                                        val hoveredIdx = (((pos.x - marginLeft) / plotWidth) * forecastItems.size)
                                            .toInt()
                                            .coerceIn(0, forecastItems.size - 1)

                                        val dayData = forecastItems[hoveredIdx]
                                        val cursorX = marginLeft + (hoveredIdx * colWidth) + (colWidth / 2f)

                                        drawLine(
                                            color = AccentAmber.copy(alpha = 0.4f),
                                            start = Offset(cursorX, marginTop),
                                            end = Offset(cursorX, marginTop + plotHeight),
                                            strokeWidth = 1.dp.toPx(),
                                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                                        )

                                        val tooltipText = String.format(
                                            Locale.US,
                                            "Previsão: %s\nPotencial: R$ %.2f\nSem IA (Base): R$ %.2f\nLucro Extra: +R$ %.2f\nEficiência: +%.1f%%\nRotas IA: %d",
                                            dayData.dateLabel,
                                            dayData.potentialEarnings,
                                            dayData.baselineEarnings,
                                            dayData.potentialEarnings - dayData.baselineEarnings,
                                            dayData.percentageGain,
                                            dayData.recommendedRoutesCount
                                        )

                                        val textLayoutResult = textMeasurer.measure(
                                            text = tooltipText,
                                            style = TextStyle(color = Color.White, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, lineHeight = 11.sp)
                                        )

                                        val tooltipWidth = textLayoutResult.size.width + 12f
                                        val tooltipHeight = textLayoutResult.size.height + 10f

                                        var tooltipX = cursorX - tooltipWidth / 2f
                                        val tooltipY = marginTop + 6f

                                        tooltipX = tooltipX.coerceIn(marginLeft, canvasWidth - marginRight - tooltipWidth)

                                        drawRoundRect(
                                            color = Color(0xFF0F172A).copy(alpha = 0.95f),
                                            topLeft = Offset(tooltipX, tooltipY),
                                            size = Size(tooltipWidth, tooltipHeight),
                                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                                            style = androidx.compose.ui.graphics.drawscope.Fill
                                        )
                                        drawRoundRect(
                                            color = AccentAmber,
                                            topLeft = Offset(tooltipX, tooltipY),
                                            size = Size(tooltipWidth, tooltipHeight),
                                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                                            style = Stroke(width = 1.dp.toPx())
                                        )

                                        drawText(
                                            textLayoutResult = textLayoutResult,
                                            topLeft = Offset(tooltipX + 6f, tooltipY + 5f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Text(
                text = if (selectedViewTab == 0) "💡 Arraste no gráfico para ver detalhes do dia" else "🔮 Prospecção baseada nas suas regras de km, filtros e auto-rejeição ativos",
                color = Color.Gray,
                fontSize = 8.5.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun OptimizedRoutePanel(
    currentLocation: Location?,
    activeDeliveryDestination: String,
    targetActiveOffer: ActiveOffer,
    rainMultiplier: Double = 1.0
) {
    val context = LocalContext.current
    
    val routeMetrics = remember(activeDeliveryDestination, targetActiveOffer, currentLocation, rainMultiplier) {
        val startLat = currentLocation?.latitude ?: -23.5505
        val startLon = currentLocation?.longitude ?: -46.6333
        
        val activeDestCoords = RouteOptimizer.getMockCoordinates(activeDeliveryDestination)
        val newPickupCoords = RouteOptimizer.getMockCoordinates(targetActiveOffer.pickupAddress)
        val newDeliveryCoords = RouteOptimizer.getMockCoordinates(targetActiveOffer.deliveryAddress)
        
        val stops = listOf(
            StopPoint("active_d", activeDeliveryDestination, activeDestCoords.first, activeDestCoords.second, StopType.DELIVERY, "active", baseValue = 12.50, urgencyScore = 1.2, appName = "iFood"),
            StopPoint("new_p", targetActiveOffer.pickupAddress, newPickupCoords.first, newPickupCoords.second, StopType.PICKUP, "new", appName = targetActiveOffer.appName.ifBlank { "Rappi" }),
            StopPoint("new_d", targetActiveOffer.deliveryAddress, newDeliveryCoords.first, newDeliveryCoords.second, StopType.DELIVERY, "new", baseValue = 15.80, urgencyScore = 1.5, appName = targetActiveOffer.appName.ifBlank { "Rappi" })
        )
        
        RouteOptimizer.calculateRouteMetrics(startLat, startLon, stops, rainMultiplier)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0F172A), RoundedCornerShape(20.dp))
            .border(1.dp, Color(0xFF00F5D4).copy(alpha = 0.2f), RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.AutoAwesome, 
                    contentDescription = null, 
                    tint = Color(0xFF00F5D4),
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Inteligência de Rota Ativa",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp
                )
            }
            
            val badgeColor = when {
                routeMetrics.efficiencyScore >= 40 -> Color(0xFFFFD700)
                routeMetrics.efficiencyScore >= 20 -> Color(0xFFC0C0C0)
                else -> Color(0xFFCD7F32)
            }

            Surface(
                color = badgeColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.3f))
            ) {
                Text(
                    text = if(routeMetrics.efficiencyScore >= 40) "EFICIÊNCIA MÁXIMA" else "+${routeMetrics.efficiencyScore}% Eficiente",
                    color = badgeColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            InfoMetricCard(
                label = "Lucro/Hora", 
                value = String.format("R$%.0f", routeMetrics.profitPerHour), 
                icon = Icons.Default.FlashOn,
                color = Color(0xFF00F5D4),
                modifier = Modifier.weight(1f)
            )
            InfoMetricCard(
                label = "Total Rota", 
                value = String.format("%.1fkm", routeMetrics.totalDistanceKm), 
                icon = Icons.Default.Route,
                color = Color(0xFF3A86FF),
                modifier = Modifier.weight(1f)
            )
            InfoMetricCard(
                label = "Tempo Est.", 
                value = "${routeMetrics.totalTimeMinutes}m", 
                icon = Icons.Default.Speed,
                color = Color(0xFFFFB703),
                modifier = Modifier.weight(1f)
            )
            InfoMetricCard(
                label = "Lucro Est.", 
                value = String.format("R$%.1f", routeMetrics.estimatedProfit), 
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                color = Color(0xFF00F5D4),
                modifier = Modifier.weight(1f)
            )
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.05f), thickness = 1.dp)

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            routeMetrics.optimizedStops.forEachIndexed { index, stop ->
                StopItem(index, stop, index == routeMetrics.optimizedStops.size - 1)
            }
        }
        
        Button(
            onClick = {
                com.example.util.ToastUtils.showToast(context, "Inteligência de Rota Aplicada!", Toast.LENGTH_SHORT)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3A86FF)),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.AltRoute, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Assumir Controle da Rota Otimizada", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun InfoMetricCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = color.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
        Text(text = value, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text(text = label, color = Color.Gray, fontSize = 8.sp)
    }
}

@Composable
fun StopItem(index: Int, stop: StopPoint, isLast: Boolean) {
    val context = LocalContext.current
    val appColor = when (stop.appName.lowercase()) {
        "ifood" -> Color(0xFFEA1D2C)
        "rappi" -> Color(0xFFFF441F)
        "99", "99food" -> Color(0xFFF7C200)
        "uber", "ubereats" -> Color(0xFF10B981)
        else -> Color(0xFF00F5D4)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(
                        if (stop.type == StopType.PICKUP) Color(0xFFFFB703).copy(alpha = 0.2f) 
                        else Color(0xFF00F5D4).copy(alpha = 0.2f), 
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = (index + 1).toString(),
                    color = if (stop.type == StopType.PICKUP) Color(0xFFFFB703) else Color(0xFF00F5D4),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(32.dp)
                        .background(Color.Gray.copy(alpha = 0.15f))
                )
            }
        }
        
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    color = appColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(0.5.dp, appColor)
                ) {
                    Text(
                        text = stop.appName.uppercase(),
                        color = appColor,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
                Text(
                    text = stop.address,
                    color = Color.White,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    color = (if (stop.type == StopType.PICKUP) Color(0xFFFFB703) else Color(0xFF00F5D4)).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = if (stop.type == StopType.PICKUP) "COLETA" else "ENTREGA",
                        color = if (stop.type == StopType.PICKUP) Color(0xFFFFB703) else Color(0xFF00F5D4),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
                Text(
                    text = "Chegada: ${RouteOptimizer.formatEta(stop.estimatedArrival)}",
                    color = Color.Gray,
                    fontSize = 10.sp
                )
            }
        }

        // Action button to launch specific partner app
        Button(
            onClick = {
                val pkgName = when (stop.appName.lowercase()) {
                    "uber", "ubereats" -> "com.ubercab.driver"
                    "99", "99food" -> "com.taxis99"
                    "ifood" -> "com.ifood.driver"
                    "rappi" -> "com.rappidriver"
                    "indrive" -> "sinet.startup.inDriver"
                    "lalamove" -> "com.lalamove.rider.driver"
                    else -> "com.ifood.driver"
                }
                try {
                    val intent = context.packageManager.getLaunchIntentForPackage(pkgName)
                    if (intent != null) {
                        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                        com.example.util.ToastUtils.showToast(context, "Abrindo ${stop.appName}...", Toast.LENGTH_SHORT)
                    } else {
                        com.example.util.ToastUtils.showToast(context, "Aplicativo ${stop.appName} não instalado neste celular", Toast.LENGTH_SHORT)
                    }
                } catch (e: Exception) {
                    com.example.util.ToastUtils.showToast(context, "Erro ao abrir ${stop.appName}")
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = appColor.copy(alpha = 0.2f)),
            border = BorderStroke(1.dp, appColor),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            modifier = Modifier.height(28.dp)
        ) {
            Text(
                text = "📍 ABRIR ${stop.appName.uppercase()}",
                color = appColor,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

fun generateRealisticRoutePoints(start: LatLng, end: LatLng): List<LatLng> {
    val points = mutableListOf<LatLng>()
    points.add(start)
    
    val steps = 15
    var currentLat = start.latitude
    var currentLon = start.longitude
    
    val latStep = (end.latitude - start.latitude) / steps
    val lonStep = (end.longitude - start.longitude) / steps
    
    for (i in 1 until steps) {
         if (i % 2 == 0) {
              currentLat += latStep * 2
         } else {
              currentLon += lonStep * 2
         }
         
         val jitterLat = (Math.sin(i.toDouble()) * 0.0001)
         val jitterLon = (Math.cos(i.toDouble()) * 0.0001)
         
         val pLat = currentLat + jitterLat
         val pLon = currentLon + jitterLon
         
         points.add(LatLng(pLat, pLon))
    }
    
    points.add(end)
    return points
}

@Composable
fun GoogleMapsNavigationCard(
    currentLocation: Location?,
    currentSpeedKmh: Float,
    rainMultiplier: Double = 1.0
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val settings by RadarCoordinator.settings.collectAsState()
    
    var searchAddress by remember { mutableStateOf("Avenida Paulista, 1000, São Paulo") }
    var destinationLatLng by remember { mutableStateOf<LatLng?>(null) }
    var isSimulationActive by remember { mutableStateOf(false) }
    var simPosition by remember { mutableStateOf<LatLng?>(null) }
    var simulationStep by remember { mutableStateOf(0) }
    var isSatelliteView by remember { mutableStateOf(false) }
    
    var isDetourApplied by remember { mutableStateOf(false) }
    var isFuelStopApplied by remember { mutableStateOf(false) }
    var isBreakActive by remember { mutableStateOf(false) }
    val detourSuggested by RadarCoordinator.detourSuggested.collectAsState()
    val trafficDelayMinutes by RadarCoordinator.trafficDelayMinutes.collectAsState()
    val detourReason by RadarCoordinator.detourReason.collectAsState()
    
    val fuelSuggestionActive by RadarCoordinator.fuelSuggestionActive.collectAsState()
    val fatigueAlertActive by RadarCoordinator.fatigueAlertActive.collectAsState()
    
    val startLat = currentLocation?.latitude ?: -23.5505
    val startLon = currentLocation?.longitude ?: -46.6333
    val startLatLng = LatLng(startLat, startLon)
    
    LaunchedEffect(Unit) {
        if (destinationLatLng == null) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = withContext(Dispatchers.IO) {
                    geocoder.getFromLocationName(searchAddress, 1)
                }
                if (!addresses.isNullOrEmpty()) {
                    destinationLatLng = LatLng(addresses[0].latitude, addresses[0].longitude)
                } else {
                    val fallback = RouteOptimizer.getMockCoordinates(searchAddress)
                    destinationLatLng = LatLng(fallback.first, fallback.second)
                }
            } catch (e: Exception) {
                val fallback = RouteOptimizer.getMockCoordinates(searchAddress)
                destinationLatLng = LatLng(fallback.first, fallback.second)
            }
        }
    }

    val currentNavigationAddress by com.example.util.MultiAppOrderManager.currentNavigationAddress.collectAsState()
    
    LaunchedEffect(currentNavigationAddress) {
        if (currentNavigationAddress.isNotEmpty()) {
            searchAddress = currentNavigationAddress
            isSimulationActive = false
            simulationStep = 0
            simPosition = null
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = withContext(Dispatchers.IO) {
                    geocoder.getFromLocationName(currentNavigationAddress, 1)
                }
                if (!addresses.isNullOrEmpty()) {
                    destinationLatLng = LatLng(addresses[0].latitude, addresses[0].longitude)
                } else {
                    val fallback = RouteOptimizer.getMockCoordinates(currentNavigationAddress)
                    destinationLatLng = LatLng(fallback.first, fallback.second)
                }
            } catch (e: Exception) {
                val fallback = RouteOptimizer.getMockCoordinates(currentNavigationAddress)
                destinationLatLng = LatLng(fallback.first, fallback.second)
            }
        }
    }
    
    val activeRiderPos = if (isSimulationActive && simPosition != null) simPosition!! else startLatLng
    
    val routePoints = remember(activeRiderPos, destinationLatLng, isDetourApplied) {
        if (destinationLatLng != null) {
            val basePoints = generateRealisticRoutePoints(activeRiderPos, destinationLatLng!!)
            if (isDetourApplied) {
                basePoints.mapIndexed { idx, pt ->
                    if (idx > 0 && idx < basePoints.size - 1) {
                        val factor = Math.sin((idx.toDouble() / basePoints.size.toDouble()) * Math.PI)
                        LatLng(
                            pt.latitude + (0.003 * factor),
                            pt.longitude - (0.003 * factor)
                        )
                    } else pt
                }
            } else {
                basePoints
            }
        } else {
            emptyList()
        }
    }
    
    val metrics = remember(activeRiderPos, destinationLatLng, rainMultiplier) {
        if (destinationLatLng != null) {
            val distResults = FloatArray(1)
            Location.distanceBetween(
                activeRiderPos.latitude, activeRiderPos.longitude,
                destinationLatLng!!.latitude, destinationLatLng!!.longitude,
                distResults
            )
            val distanceKm = distResults[0] / 1000.0
            val speedKmh = if (rainMultiplier > 1.0) 25.0 else 35.0
            val timeMinutes = (distanceKm / speedKmh * 60).toLong() + 2
            Pair(distanceKm, timeMinutes)
        } else {
            Pair(0.0, 0L)
        }
    }
    
    val totalDistanceKm = metrics.first
    val totalTimeMinutes = metrics.second
    
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(activeRiderPos, 15f)
    }
    
    LaunchedEffect(activeRiderPos) {
        cameraPositionState.animate(
            com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(activeRiderPos, 15f),
            1000
        )
    }
    
    val mapProperties = remember(isSatelliteView) {
        val darkMapStyleJson = """
        [
          {
            "elementType": "geometry",
            "stylers": [{"color": "#1e293b"}]
          },
          {
            "elementType": "labels.text.stroke",
            "stylers": [{"color": "#0f172a"}]
          },
          {
            "elementType": "labels.text.fill",
            "stylers": [{"color": "#94a3b8"}]
          },
          {
            "featureType": "administrative",
            "elementType": "geometry",
            "stylers": [{"color": "#334155"}]
          },
          {
            "featureType": "road",
            "elementType": "geometry",
            "stylers": [{"color": "#334155"}]
          },
          {
            "featureType": "road",
            "elementType": "geometry.stroke",
            "stylers": [{"color": "#1e293b"}]
          },
          {
            "featureType": "water",
            "elementType": "geometry",
            "stylers": [{"color": "#0f172a"}]
          }
        ]
        """.trimIndent()
        
        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        
        MapProperties(
            mapType = if (isSatelliteView) MapType.SATELLITE else MapType.NORMAL,
            isMyLocationEnabled = hasPermission && !settings.forceMockSpeed,
            isTrafficEnabled = true,
            mapStyleOptions = if (isSatelliteView) null else com.google.android.gms.maps.model.MapStyleOptions(darkMapStyleJson)
        )
    }
    
    val mapUiSettings = remember {
        MapUiSettings(
            zoomControlsEnabled = false,
            compassEnabled = true,
            myLocationButtonEnabled = true
        )
    }
    
    LaunchedEffect(isSimulationActive) {
        if (isSimulationActive && routePoints.isNotEmpty()) {
            val totalSteps = routePoints.size
            for (step in simulationStep until totalSteps) {
                if (!isSimulationActive) break
                simulationStep = step
                simPosition = routePoints[step]
                
                val simulatedLocation = Location("gps").apply {
                    latitude = routePoints[step].latitude
                    longitude = routePoints[step].longitude
                    speed = 9.7f // ~35 km/h
                    time = System.currentTimeMillis()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                        elapsedRealtimeNanos = android.os.SystemClock.elapsedRealtimeNanos()
                    }
                }
                
                RadarCoordinator.updateLocation(simulatedLocation)
                kotlinx.coroutines.delay(2000)
            }
            isSimulationActive = false
            simulationStep = 0
            simPosition = null
            RadarCoordinator.voiceManager?.speak("Destino alcançado. Simulação concluída.")
        }
    }
    
    Card(
        colors = CardDefaults.cardColors(containerColor = CardSlateBg),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("google_maps_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = null,
                        tint = AccentBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Co-piloto Ativo & Tráfego",
                        color = TextLight,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { isSatelliteView = !isSatelliteView },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = "Tipo de Mapa",
                            tint = if (isSatelliteView) AccentGreen else Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, Color.Gray.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = mapProperties,
                    uiSettings = mapUiSettings
                ) {
                    destinationLatLng?.let {
                        Marker(
                            state = MarkerState(position = it),
                            title = "Destino",
                            snippet = searchAddress,
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                        )
                    }
                    
                    Marker(
                        state = MarkerState(position = activeRiderPos),
                        title = "Thiago (Rider)",
                        snippet = if (isSimulationActive) "Simulação ativa (Velocidade: 35 km/h)" else "Sua Localização Atual",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                    )
                    
                    if (routePoints.isNotEmpty()) {
                        Polyline(
                            points = routePoints,
                            color = if (isDetourApplied) AccentAmber else AccentBlue,
                            width = 10f
                        )
                    }
                }
                
                val currentSpd = if (isSimulationActive) 35 else currentSpeedKmh.toInt()
                Box(
                    modifier = Modifier
                        .padding(12.dp)
                        .align(Alignment.BottomStart)
                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                        .border(1.dp, AccentBlue.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$currentSpd", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                        Text("km/h", color = AccentBlue, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                if (isSimulationActive) {
                    Box(
                        modifier = Modifier
                            .padding(12.dp)
                            .align(Alignment.TopEnd)
                            .background(AccentGreen.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .border(1.dp, AccentGreen.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(modifier = Modifier.size(6.dp).background(AccentGreen, CircleShape))
                            Text("SIMULANDO GPS", color = AccentGreen, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InfoMetricCard(
                    label = "Distância",
                    value = String.format("%.2f km", totalDistanceKm),
                    icon = Icons.Default.Route,
                    color = AccentBlue,
                    modifier = Modifier.weight(1f)
                )
                InfoMetricCard(
                    label = "Tempo Estimado (ETA)",
                    value = if (totalTimeMinutes > 0) "$totalTimeMinutes min" else "-- min",
                    icon = Icons.Default.Speed,
                    color = AccentAmber,
                    modifier = Modifier.weight(1f)
                )
                InfoMetricCard(
                    label = "Status da Viagem",
                    value = if (isSimulationActive) "Em rota..." else "Aguardando",
                    icon = Icons.Default.CompassCalibration,
                    color = if (isSimulationActive) AccentGreen else Color.Gray,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        isSimulationActive = !isSimulationActive
                        if (!isSimulationActive) {
                            simulationStep = 0
                            simPosition = null
                            RadarCoordinator.voiceManager?.speak("Simulação pausada por Thiago.")
                        }
                    },
                    modifier = Modifier.weight(1.5f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSimulationActive) AccentRed else AccentGreen
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = if (isSimulationActive) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isSimulationActive) "Parar Viagem" else "Iniciar Simulação",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Button(
                    onClick = {
                        isSimulationActive = false
                        simulationStep = 0
                        simPosition = null
                        destinationLatLng = null
                        searchAddress = ""
                        RadarCoordinator.voiceManager?.speak("Navegação reiniciada.")
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = CardSlateBg),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Limpar", color = Color.White, fontSize = 11.sp)
                }
            }
        }
    }
}
