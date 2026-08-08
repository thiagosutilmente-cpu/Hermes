package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddRoad
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.coordinator.RadarCoordinator
import com.example.data.OfferEntity
import com.example.util.ChainedDetourEngine
import com.example.util.DetourAnalysisResult
import com.example.util.DetourType
import com.example.util.MultiAppOrderManager
import com.example.util.ActiveOrder
import com.example.util.OrderStatus
import com.example.util.RouteOptimizer

@Composable
fun ChainedDetourWidget(
    onAttachDetourOrder: (DetourAnalysisResult) -> Unit = {}
) {
    val currentLocation by RadarCoordinator.currentLocation.collectAsState()
    val pendingOffers by MultiAppOrderManager.pendingOffers.collectAsState()

    val lat = currentLocation?.latitude ?: -23.5505
    val lng = currentLocation?.longitude ?: -46.6333

    // Exemplo de Ofertas para Simulação de Desvio Encadeado
    val primaryOffer = remember(pendingOffers) {
        pendingOffers.firstOrNull() ?: OfferEntity(
            id = 101,
            appName = "iFood",
            fareValue = 18.50,
            pickupAddress = "Burger King - Av. Paulista, 1000",
            deliveryAddress = "Rua Bela Cintra, 450",
            totalDistance = 2.8,
            totalTime = 12.0,
            suggestion = "aceitar",
            reason = "Apenas 2.8km no trajeto original"
        )
    }

    val secondaryCandidates = remember(pendingOffers) {
        if (pendingOffers.size >= 2) {
            pendingOffers.drop(1)
        } else {
            listOf(
                OfferEntity(
                    id = 102,
                    appName = "Rappi",
                    fareValue = 16.00,
                    pickupAddress = "Pizza Hut - Av. Paulista, 1200",
                    deliveryAddress = "Rua Augusta, 890",
                    totalDistance = 3.2,
                    totalTime = 15.0,
                    suggestion = "aceitar",
                    reason = "Mesmo corredor de entrega na Paulista"
                ),
                OfferEntity(
                    id = 103,
                    appName = "Uber Flash",
                    fareValue = 14.50,
                    pickupAddress = "Starbucks - Alameda Santos, 500",
                    deliveryAddress = "Rua Haddock Lobo, 300",
                    totalDistance = 2.1,
                    totalTime = 10.0,
                    suggestion = "aceitar",
                    reason = "Desvio mínimo de +1.2km"
                )
            )
        }
    }

    val detourAnalyses = remember(primaryOffer, secondaryCandidates, lat, lng) {
        secondaryCandidates.map { secondary ->
            ChainedDetourEngine.analyzeDetourForOfferB(
                userLat = lat,
                userLng = lng,
                offerA = primaryOffer,
                offerB = secondary
            )
        }.sortedByDescending { it.efficiencyScore }
    }

    var selectedDetour by remember { mutableStateOf(detourAnalyses.firstOrNull()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111118)),
        border = BorderStroke(1.dp, Brush.horizontalGradient(listOf(Color(0xFFFF0055), Color(0xFF00FF88))))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header
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
                            .background(Color(0xFFFF0055).copy(alpha = 0.15f))
                            .border(1.dp, Color(0xFFFF0055), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AltRoute,
                            contentDescription = "Otimizador de Desvio",
                            tint = Color(0xFFFF0055),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "OTIMIZADOR DE DESVIOS DE ENTREGA B",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Análise de Custo Delta de Rota e Anexação Encadeada",
                            color = Color.Gray,
                            fontSize = 9.sp
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF00FF88).copy(alpha = 0.15f),
                    border = BorderStroke(0.5.dp, Color(0xFF00FF88))
                ) {
                    Text(
                        text = "94% ALINHAMENTO",
                        color = Color(0xFF00FF88),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Selectable Detour Option Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                detourAnalyses.forEach { analysis ->
                    val isSelected = selectedDetour?.secondaryOffer?.id == analysis.secondaryOffer.id
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) Color(0xFFFF0055).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f))
                            .border(
                                1.dp,
                                if (isSelected) Color(0xFFFF0055) else Color.White.copy(alpha = 0.15f),
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { selectedDetour = analysis }
                            .padding(vertical = 8.dp, horizontal = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "+ ${analysis.secondaryOffer.appName}",
                                color = if (isSelected) Color.White else Color.Gray,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "R$ ${String.format("%.2f", analysis.secondaryValue)} (+${analysis.extraDetourKm}km)",
                                color = if (isSelected) Color(0xFF00FF88) else Color.Gray,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Detailed Selected Detour Visualization Box
            selectedDetour?.let { detour ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Verdict Badge & Primary Stats
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFFFCC00).copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, Color(0xFFFFCC00))
                        ) {
                            Text(
                                text = "🏆 ${detour.verdict}",
                                color = Color(0xFFFFCC00),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }

                        Text(
                            text = "R$ ${detour.detourYieldPerKm}/km no Desvio!",
                            color = Color(0xFF00FF88),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Text(
                        text = detour.verdictReason,
                        color = Color.LightGray,
                        fontSize = 9.sp
                    )

                    Divider(color = Color.White.copy(alpha = 0.1f))

                    // Trajectory Sequence Flow Diagram
                    Text(
                        text = "📐 SEQUÊNCIA TÁTICA DA ROTA COMBINADA:",
                        color = Color(0xFF00E5FF),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        detour.sequencedStops.forEachIndexed { idx, stop ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(if (stop.appName.contains("iFood")) Color(0xFFEA1D2C) else Color(0xFFFF0055)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${idx + 1}",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }

                                Text(
                                    text = if (stop.priority <= 2) "COLETA [${stop.appName}]:" else "ENTREGA [${stop.appName}]:",
                                    color = if (stop.priority <= 2) Color(0xFFFFCC00) else Color(0xFF00FF88),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = stop.address,
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Divider(color = Color.White.copy(alpha = 0.1f))

                    // Metrics Comparison Table
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("DISTÂNCIA TOTAL", color = Color.Gray, fontSize = 8.sp)
                            Text("${detour.combinedDistanceKm} km (+${detour.extraDetourKm} km)", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("TEMPO ESTIMADO", color = Color.Gray, fontSize = 8.sp)
                            Text("${detour.combinedTimeMin} min (+${detour.extraDetourTimeMin} min)", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("VALOR TOTAL", color = Color.Gray, fontSize = 8.sp)
                            Text("R$ ${String.format("%.2f", detour.totalValue)}", color = Color(0xFF00FF88), fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    // Attach Detour Action Button
                    Button(
                        onClick = {
                            val (pLat, pLng) = RouteOptimizer.getMockCoordinates(detour.secondaryOffer.pickupAddress)
                            val (dLat, dLng) = RouteOptimizer.getMockCoordinates(detour.secondaryOffer.deliveryAddress)

                            val newOrder = ActiveOrder(
                                id = "detour_${detour.secondaryOffer.id}",
                                appName = detour.secondaryOffer.appName,
                                fare = detour.secondaryOffer.fareValue,
                                pickupAddress = detour.secondaryOffer.pickupAddress,
                                deliveryAddress = detour.secondaryOffer.deliveryAddress,
                                pickupLat = pLat,
                                pickupLng = pLng,
                                deliveryLat = dLat,
                                deliveryLng = dLng,
                                status = OrderStatus.PICKING_UP
                            )
                            MultiAppOrderManager.addOrder(newOrder)
                            onAttachDetourOrder(detour)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF0055)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddRoad,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ANEXAR DESVIO DA ENTREGA B À ROTA ATUAL",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}
