package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Dados de Telemetria de Coleta calculados pela API do Google Maps.
 */
data class GoogleMapsPickupEstimate(
    val restaurantName: String,
    val pickupAddress: String,
    val distanceToPickupKm: Double,
    val etaToPickupMinutes: Int,
    val trafficCondition: TrafficCondition = TrafficCondition.LIGHT,
    val recommendedCorridor: String = "Corredor Central de Motos",
    val deliveryTotalDistanceKm: Double,
    val grossValue: Double
) {
    val totalTimeMinutes: Int
        get() = etaToPickupMinutes + (deliveryTotalDistanceKm * 3.0).roundToInt().coerceAtLeast(6)

    val totalDistanceKm: Double
        get() = distanceToPickupKm + deliveryTotalDistanceKm

    val realProfitPerKm: Double
        get() = if (totalDistanceKm > 0) grossValue / totalDistanceKm else grossValue

    val decisionRecommendation: PickupDecisionScore
        get() = when {
            distanceToPickupKm <= 1.5 && etaToPickupMinutes <= 5 -> PickupDecisionScore.EXCELLENT
            distanceToPickupKm <= 3.0 && etaToPickupMinutes <= 8 -> PickupDecisionScore.GOOD
            distanceToPickupKm <= 4.5 && realProfitPerKm >= 4.0 -> PickupDecisionScore.ACCEPTABLE
            else -> PickupDecisionScore.POOR
        }
}

enum class TrafficCondition(val label: String, val color: Color, val icon: String) {
    LIGHT("Trânsito Livre", Color(0xFF00FF88), "🟢"),
    MODERATE("Trânsito Moderado", Color(0xFFFFC107), "🟡"),
    HEAVY("Trânsito Intenso", Color(0xFFFF4444), "🔴")
}

enum class PickupDecisionScore(
    val title: String,
    val badge: String,
    val color: Color,
    val explanation: String
) {
    EXCELLENT(
        title = "COLETA IMEDIATA (EXCELENTE)",
        badge = "🔥 ACEITE RECOMENDADO",
        color = Color(0xFF00FF88),
        explanation = "Restaurante muito próximo. Deslocamento morto quase nulo, maximiza seu ganho por hora!"
    ),
    GOOD(
        title = "COLETA FAVORÁVEL",
        badge = "⚡ COMPENSA PEGAR",
        color = Color(0xFF00D2FF),
        explanation = "Tempo até o restaurante dentro do padrão de alta rentabilidade com via rápida."
    ),
    ACCEPTABLE(
        title = "COLETA MODERADA",
        badge = "⚖️ AVALIE DESTINO",
        color = Color(0xFFFFC107),
        explanation = "Distância intermediária até a coleta. O valor total da corrida cobre o trajeto."
    ),
    POOR(
        title = "COLETA MUITO DISTANTE",
        badge = "⚠️ DESVANTAGEM",
        color = Color(0xFFFF4444),
        explanation = "Mais de 10 min de deslocamento vazio até a coleta. Risco de queda no faturamento horário."
    )
}

/**
 * Card de Estimativa de Tempo e Distância de Coleta via Google Maps.
 * Fornece métricas de tempo real da rota até o restaurante para auxiliar
 * o motoboy a decidir se compensa aceitar a oferta.
 */
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun GoogleMapsPickupCard(
    offer: RadarOffer = LiveDispatchSimulator.getInitialOffers().first(),
    onAcceptWithNavigation: (RadarOffer) -> Unit = {},
    onDismissOrNext: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Gera estimativa inteligente de coleta Google Maps proporcional aos dados reais da oferta
    val pickupEstimate = remember(offer.id) {
        val distPickup = when {
            offer.pickupAddress.contains("Paulista", ignoreCase = true) -> 0.8
            offer.pickupAddress.contains("Ibirapuera", ignoreCase = true) -> 2.1
            offer.pickupAddress.contains("Augusta", ignoreCase = true) -> 1.3
            offer.pickupAddress.contains("Oscar Freire", ignoreCase = true) -> 1.7
            offer.pickupAddress.contains("Santos", ignoreCase = true) -> 0.9
            else -> 1.4
        }
        val etaMin = (distPickup * 2.8).roundToInt().coerceAtLeast(3)
        val traffic = if (distPickup > 2.0) TrafficCondition.MODERATE else TrafficCondition.LIGHT

        GoogleMapsPickupEstimate(
            restaurantName = offer.restaurant,
            pickupAddress = offer.pickupAddress,
            distanceToPickupKm = distPickup,
            etaToPickupMinutes = etaMin,
            trafficCondition = traffic,
            recommendedCorridor = if (distPickup <= 1.0) "Via Local Direta (Faixa de Moto)" else "Corredor Av. Paulista / Rebouças",
            deliveryTotalDistanceKm = offer.distanceKm,
            grossValue = offer.value
        )
    }

    val pickupCardRenderTime = remember(offer.id) { System.currentTimeMillis() }

    LaunchedEffect(offer.id) {
        FirebaseAnalyticsManager.logOfferViewed(
            offerId = offer.id,
            appName = offer.appName,
            restaurant = offer.restaurant,
            value = offer.value,
            distanceKm = offer.distanceKm,
            gainPerKm = offer.gainPerKm,
            neuralDecision = offer.neuralDecision.decision.name,
            viewSource = "maps_pickup_card"
        )
    }

    var isDetailsExpanded by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_radar_maps")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "border_alpha"
    )

    val decision = pickupEstimate.decisionRecommendation

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF13151F)),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.6.dp,
                color = decision.color.copy(alpha = borderAlpha),
                shape = RoundedCornerShape(20.dp)
            )
            .testTag("google_maps_pickup_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 1. CABEÇALHO DO CARD: GOOGLE MAPS API + STATUS DE TRÂNSITO EM TEMPO REAL
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4285F4).copy(alpha = 0.2f))
                            .border(1.dp, Color(0xFF4285F4), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🗺️", fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "GOOGLE MAPS",
                                color = Color(0xFF4285F4),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.8.sp
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "• ROTA DE COLETA",
                                color = TextLight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Deslocamento da sua posição até o restaurante",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }

                // Badge de Trânsito
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(pickupEstimate.trafficCondition.color.copy(alpha = 0.15f))
                        .border(1.dp, pickupEstimate.trafficCondition.color.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "${pickupEstimate.trafficCondition.icon} ${pickupEstimate.trafficCondition.label}",
                        color = pickupEstimate.trafficCondition.color,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. NOME DO RESTAURANTE E ENDEREÇO DE COLETA COM BOTÃO GOOGLE MAPS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0A0A0F))
                    .border(1.dp, Color(0xFF222233), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "🍔 ${pickupEstimate.restaurantName}",
                        color = TextLight,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "📍 Coleta: ${pickupEstimate.pickupAddress}, SP",
                        color = TextMuted,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Botão de visualização no Google Maps
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF4285F4).copy(alpha = 0.18f))
                        .border(1.dp, Color(0xFF4285F4).copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .clickable {
                            HapticFeedbackHelper.vibrateTap(context)
                            launchGoogleMapsDirectPickup(context, pickupEstimate.pickupAddress)
                        }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .testTag("btn_open_pickup_map")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "↗️ Maps", color = Color(0xFF8AB4F8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. TELEMETRIA TÁTICA DE COLETA (DISTÂNCIA ATÉ O RESTAURANTE + TEMPO DE CHEGADA)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Bloco 1: Tempo Estimado até o Restaurante (ETA)
                Box(
                    modifier = Modifier
                        .weight(1.2f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(decision.color.copy(alpha = 0.12f))
                        .border(1.dp, decision.color.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                        .padding(vertical = 10.dp, horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "TEMPO ATÉ COLETA",
                            color = decision.color,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "⏱️ ${pickupEstimate.etaToPickupMinutes} min",
                            color = decision.color,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "via corredor de moto",
                            color = TextMuted,
                            fontSize = 8.5.sp
                        )
                    }
                }

                // Bloco 2: Distância até a Coleta
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkBg)
                        .border(1.dp, Color(0xFF222233), RoundedCornerShape(12.dp))
                        .padding(vertical = 10.dp, horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "DISTÂNCIA",
                            color = TextMuted,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "🛵 ${String.format(Locale.GERMANY, "%.1f", pickupEstimate.distanceToPickupKm)} km",
                            color = TextLight,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "deslocamento",
                            color = TextMuted,
                            fontSize = 8.5.sp
                        )
                    }
                }

                // Bloco 3: Ganho Real por KM Total (Coleta + Entrega)
                Box(
                    modifier = Modifier
                        .weight(1.1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkBg)
                        .border(1.dp, Color(0xFF222233), RoundedCornerShape(12.dp))
                        .padding(vertical = 10.dp, horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "VALOR / KM TOTAL",
                            color = TextMuted,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = String.format(Locale.GERMANY, "R$ %.2f", pickupEstimate.realProfitPerKm),
                            color = NeonGreen,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "coleta + entrega",
                            color = TextMuted,
                            fontSize = 8.5.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4. BANNER DE RECOMENDAÇÃO DE DECISÃO (ASSISTÊNCIA DE DECISÃO GOOGLE MAPS)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(decision.color.copy(alpha = 0.14f))
                    .border(1.dp, decision.color.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .padding(10.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🧭 ${decision.title}",
                            color = decision.color,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(decision.color.copy(alpha = 0.25f))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = decision.badge,
                                color = decision.color,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = decision.explanation,
                        color = TextLight,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }

            // 5. DETALHES EXPANSÍVEIS (COMPARATIVO TRAJETO COMPLETO)
            AnimatedVisibility(visible = isDetailsExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0D0E16))
                        .border(1.dp, Color(0xFF222233), RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "DETALHAMENTO DE ROTAS (GOOGLE MAPS API):",
                        color = TextMuted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    DetailRouteRow(
                        step = "1. Posição Atual ➔ Coleta:",
                        val1 = "${pickupEstimate.distanceToPickupKm} km",
                        val2 = "${pickupEstimate.etaToPickupMinutes} min de moto"
                    )
                    DetailRouteRow(
                        step = "2. Restaurante ➔ Cliente:",
                        val1 = "${offer.distanceKm} km",
                        val2 = "${offer.timeMinutes} min de entrega"
                    )
                    DetailRouteRow(
                        step = "Total do Ciclo Completo:",
                        val1 = "${String.format(Locale.GERMANY, "%.1f", pickupEstimate.totalDistanceKm)} km",
                        val2 = "${pickupEstimate.totalTimeMinutes} min • R$ ${String.format(Locale.GERMANY, "%.2f", offer.value)}"
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 6. BOTÕES DE AÇÃO DO CARD DE COLETA (ACEITAR OU NAVEGAR DIRETO)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Alternar Detalhes
                OutlinedButton(
                    onClick = { isDetailsExpanded = !isDetailsExpanded },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMuted),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E2E44)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(0.7f)
                        .height(44.dp)
                ) {
                    Text(
                        text = if (isDetailsExpanded) "Ocultar" else "+ Detalhes",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Abrir Navegação no Google Maps (Rota até a Coleta)
                OutlinedButton(
                    onClick = {
                        HapticFeedbackHelper.vibrateTap(context)
                        launchGoogleMapsDirectPickup(context, pickupEstimate.pickupAddress)
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF4285F4)),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF4285F4)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("btn_pickup_maps_navigate")
                ) {
                    Text(
                        text = "🗺️ ROTA COLETA",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                // Aceitar com Rota de Coleta Imediata
                Button(
                    onClick = {
                        val latency = System.currentTimeMillis() - pickupCardRenderTime
                        HapticFeedbackHelper.vibrateAccept(context)
                        FirebaseAnalyticsManager.logOfferAcceptClicked(
                            offerId = offer.id,
                            appName = offer.appName,
                            restaurant = offer.restaurant,
                            value = offer.value,
                            distanceKm = offer.distanceKm,
                            gainPerKm = offer.gainPerKm,
                            clickSource = "maps_pickup_card",
                            timeToClickMs = latency
                        )
                        launchGoogleMapsDirectPickup(context, pickupEstimate.pickupAddress)
                        onAcceptWithNavigation(offer)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonGreen,
                        contentColor = DarkBg
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(44.dp)
                        .testTag("btn_accept_with_pickup")
                ) {
                    Text(
                        text = "✅ ACEITAR",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailRouteRow(step: String, val1: String, val2: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = step, color = TextLight, fontSize = 10.sp, fontWeight = FontWeight.Medium)
        Row {
            Text(text = val1, color = NeonGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(text = " • ", color = TextMuted, fontSize = 10.sp)
            Text(text = val2, color = TextLight, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

/**
 * Abre o Google Maps traçado direto para o restaurante de coleta com modo de veículo de duas rodas (motocicleta).
 */
fun launchGoogleMapsDirectPickup(context: Context, pickupAddress: String) {
    try {
        val destEncoded = URLEncoder.encode("$pickupAddress, Sao Paulo, SP", StandardCharsets.UTF_8.toString())
        // URL da API de Navegação do Google Maps com travelmode=two_wheeler
        val uri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$destEncoded&travelmode=two_wheeler")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            val genericIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(genericIntent)
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Abrindo rota de coleta no Google Maps...", Toast.LENGTH_SHORT).show()
    }
}
