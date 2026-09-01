package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

// ----------------------------------------------------
// CORES & TEMA (Material Design 3 - Radar Dark Cockpit)
// ----------------------------------------------------
val NeonGreen = Color(0xFF00FF88)
val NeonGreenDark = Color(0xFF00B35F)
val DarkBg = Color(0xFF0A0A0F)
val DarkCard = Color(0xFF111118)
val DarkCardElevated = Color(0xFF181824)
val DarkBorder = Color(0xFF222233)
val TextLight = Color(0xFFF0F0F5)
val TextMuted = Color(0xFF8888A0)
val RedDecline = Color(0xFFFF4444)
val OrangeRappi = Color(0xFFFF441F)
val RedIFood = Color(0xFFEA1D2C)
val Yellow99 = Color(0xFFF7C200)

private val RadarColorScheme = darkColorScheme(
    primary = NeonGreen,
    onPrimary = DarkBg,
    primaryContainer = NeonGreenDark,
    onPrimaryContainer = TextLight,
    background = DarkBg,
    onBackground = TextLight,
    surface = DarkCard,
    onSurface = TextLight,
    surfaceVariant = DarkCardElevated,
    onSurfaceVariant = TextMuted,
    outline = DarkBorder
)

// ----------------------------------------------------
// MODELO DE DADOS DE OFERTA DE ENTREGA
// ----------------------------------------------------
data class DeliveryOffer(
    val id: String,
    val appName: String,
    val appColor: Color,
    val restaurant: String,
    val value: Double,
    val distanceKm: Double,
    val timeMinutes: Int,
    val pickupAddress: String,
    val destinationAddress: String,
    val isMultiStack: Boolean = false
) {
    val gainPerKm: Double
        get() = if (distanceKm > 0) value / distanceKm else value
}

// ----------------------------------------------------
// ACTIVITY PRINCIPAL
// ----------------------------------------------------
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = RadarColorScheme) {
                RadarDeliveryDashboard()
            }
        }
    }
}

// ----------------------------------------------------
// TELA PRINCIPAL: RADAR DELIVERY DASHBOARD
// ----------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadarDeliveryDashboard() {
    // Estado do Rastreamento / Radar
    var isTrackingActive by remember { mutableStateOf(true) }
    var todayEarnings by remember { mutableDoubleStateOf(284.50) }
    var completedDeliveries by remember { mutableIntStateOf(18) }
    var scannedOffersCount by remember { mutableIntStateOf(52) }

    // Lista de Ofertas Simuladas
    val offersList = remember {
        mutableStateListOf(
            DeliveryOffer(
                id = "offer_101",
                appName = "iFood + Rappi (Multi-Stack)",
                appColor = NeonGreen,
                restaurant = "Burger King & Pizza Hut",
                value = 33.00,
                distanceKm = 4.2,
                timeMinutes = 18,
                pickupAddress = "Av. Paulista, 1578",
                destinationAddress = "R. Bela Cintra, 904",
                isMultiStack = true
            ),
            DeliveryOffer(
                id = "offer_102",
                appName = "iFood",
                appColor = RedIFood,
                restaurant = "Madero Container",
                value = 22.50,
                distanceKm = 3.1,
                timeMinutes = 12,
                pickupAddress = "Shopping Ibirapuera",
                destinationAddress = "Av. Moema, 450"
            ),
            DeliveryOffer(
                id = "offer_103",
                appName = "Rappi",
                appColor = OrangeRappi,
                restaurant = "Starbucks Coffee",
                value = 18.00,
                distanceKm = 2.4,
                timeMinutes = 9,
                pickupAddress = "R. Augusta, 2100",
                destinationAddress = "Al. Santos, 120"
            ),
            DeliveryOffer(
                id = "offer_104",
                appName = "99 Food",
                appColor = Yellow99,
                restaurant = "Outback Steakhouse",
                value = 29.80,
                distanceKm = 5.0,
                timeMinutes = 20,
                pickupAddress = "Shopping Morumbi",
                destinationAddress = "Av. Chucri Zaidan, 110"
            )
        )
    }

    Scaffold(
        containerColor = DarkBg,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(text = "🎯", fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "RADAR DELIVERY",
                                color = TextLight,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.2.sp
                            )
                            Text(
                                text = "Jarvis Neural Cockpit",
                                color = NeonGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            offersList.add(
                                DeliveryOffer(
                                    id = "offer_${System.currentTimeMillis() % 10000}",
                                    appName = "iFood",
                                    appColor = RedIFood,
                                    restaurant = "Bullguer Jardins",
                                    value = (20..42).random().toDouble(),
                                    distanceKm = (2..5).random().toDouble(),
                                    timeMinutes = (11..21).random(),
                                    pickupAddress = "R. Oscar Freire, 800",
                                    destinationAddress = "Al. Lorena, 450"
                                )
                            )
                            scannedOffersCount++
                        },
                        modifier = Modifier.testTag("action_refresh")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Simular Nova Chamada",
                            tint = NeonGreen
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = DarkCard
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. ESTADO ATUAL DO RADAR E BOTÃO GRANDE DE ATIVAR/DESATIVAR RASTREAMENTO
            item {
                BigRadarTrackingControl(
                    isTrackingActive = isTrackingActive,
                    onToggleTracking = { isTrackingActive = !isTrackingActive }
                )
            }

            // 2. RESUMO DE GANHOS E MÉTRICAS DO DIA
            item {
                RadarMetricsRow(
                    todayEarnings = todayEarnings,
                    completed = completedDeliveries,
                    scanned = scannedOffersCount
                )
            }

            // 3. APPS PARCEIROS CONECTADOS
            item {
                PartnersStatusBar(isTrackingActive = isTrackingActive)
            }

            // 4. CABEÇALHO DA LISTA DE OFERTAS
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "OFERTAS INTERCEPTADAS (${if (isTrackingActive) offersList.size else 0})",
                        color = TextLight,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )

                    if (isTrackingActive) {
                        Text(
                            text = "⚡ FILTRANDO R$ > 5,00/km",
                            color = NeonGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // 5. LISTA DE OFERTAS OU MENSAGEM DE STATUS
            if (!isTrackingActive) {
                item {
                    RadarEmptyState(
                        icon = "⏸️",
                        title = "Rastreamento Desativado",
                        subtitle = "Toque no botão principal acima para ativar o radar e rastrear pedidos."
                    )
                }
            } else if (offersList.isEmpty()) {
                item {
                    RadarEmptyState(
                        icon = "🛰️",
                        title = "Varrendo Área em Tempo Real...",
                        subtitle = "Aguardando pedidos de alta rentabilidade nas proximidades."
                    )
                }
            } else {
                items(offersList, key = { it.id }) { offer ->
                    OfferCard(
                        offer = offer,
                        onAccept = {
                            todayEarnings += offer.value
                            completedDeliveries++
                            offersList.remove(offer)
                        },
                        onDecline = {
                            offersList.remove(offer)
                        }
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------
// COMPONENTE: BOTÃO GRANDE DE RASTREAMENTO DO RADAR
// ----------------------------------------------------
@Composable
fun BigRadarTrackingControl(
    isTrackingActive: Boolean,
    onToggleTracking: () -> Unit
) {
    val statusColor by animateColorAsState(
        targetValue = if (isTrackingActive) NeonGreen else RedDecline,
        label = "statusColor"
    )

    // Animação de pulso e rotação do radar quando ativo
    val infiniteTransition = rememberInfiniteTransition(label = "radar_anim")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val radarRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_rotation"
    )

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, statusColor.copy(alpha = 0.45f), RoundedCornerShape(24.dp))
            .testTag("radar_tracking_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header do Estado Atual do Radar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ESTADO DO RADAR",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                }

                Text(
                    text = if (isTrackingActive) "RASTREAMENTO ATIVO" else "RASTREAMENTO PAUSADO",
                    color = statusColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Ícone Central do Radar com Feedback Visual
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(DarkCardElevated)
                    .border(2.dp, statusColor.copy(alpha = 0.5f), CircleShape)
            ) {
                if (isTrackingActive) {
                    // Círculo de pulso
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(NeonGreen.copy(alpha = 0.15f))
                    )
                    // Linha de varredura giratória
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .rotate(radarRotation)
                            .border(1.dp, Brush.sweepGradient(listOf(Color.Transparent, NeonGreen)), CircleShape)
                    )
                }

                Text(
                    text = if (isTrackingActive) "🎯" else "⏸️",
                    fontSize = 38.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Descrição do Status
            Text(
                text = if (isTrackingActive) "Monitorando GPS (3.8m) e interceptando chamadas" else "Nenhum pedido está sendo interceptado no momento",
                color = TextMuted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            // BOTÃO GRANDE DE ATIVAR / DESATIVAR RASTREAMENTO
            Button(
                onClick = onToggleTracking,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isTrackingActive) RedDecline else NeonGreen,
                    contentColor = if (isTrackingActive) TextLight else DarkBg
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("btn_toggle_tracking_large")
            ) {
                Text(
                    text = if (isTrackingActive) "⏹ DESATIVAR RASTREAMENTO" else "▶ ATIVAR RASTREAMENTO",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.8.sp
                )
            }
        }
    }
}

// ----------------------------------------------------
// COMPONENTE: MÉTRICAS DE GANHO E ATIVIDADE
// ----------------------------------------------------
@Composable
fun RadarMetricsRow(
    todayEarnings: Double,
    completed: Int,
    scanned: Int
) {
    val formattedEarnings = String.format(Locale.GERMANY, "R$ %.2f", todayEarnings)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MetricTile(
            title = "GANHO HOJE",
            value = formattedEarnings,
            valueColor = NeonGreen,
            modifier = Modifier.weight(1.3f)
        )
        MetricTile(
            title = "ENTREGAS",
            value = "$completed",
            modifier = Modifier.weight(0.85f)
        )
        MetricTile(
            title = "SCANNER",
            value = "$scanned",
            modifier = Modifier.weight(0.85f)
        )
    }
}

@Composable
fun MetricTile(
    title: String,
    value: String,
    valueColor: Color = TextLight,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(DarkCardElevated)
            .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
            .padding(vertical = 10.dp, horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            color = TextMuted,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            color = valueColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

// ----------------------------------------------------
// COMPONENTE: STATUS DOS APPS PARCEIROS
// ----------------------------------------------------
@Composable
fun PartnersStatusBar(isTrackingActive: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkCard)
            .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppTag(name = "iFood", color = RedIFood, active = isTrackingActive)
        AppTag(name = "Rappi", color = OrangeRappi, active = isTrackingActive)
        AppTag(name = "Uber", color = TextLight, active = isTrackingActive)
        AppTag(name = "99 Food", color = Yellow99, active = isTrackingActive)
    }
}

@Composable
fun AppTag(name: String, color: Color, active: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(if (active) color else TextMuted.copy(alpha = 0.35f))
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = name,
            color = if (active) TextLight else TextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ----------------------------------------------------
// COMPONENTE: CARD DA OFERTA DE ENTREGA
// ----------------------------------------------------
@Composable
fun OfferCard(
    offer: DeliveryOffer,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    val formattedPrice = String.format(Locale.GERMANY, "R$ %.2f", offer.value)
    val formattedPerKm = String.format(Locale.GERMANY, "R$ %.2f/km", offer.gainPerKm)

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCardElevated),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (offer.isMultiStack) NeonGreen.copy(alpha = 0.6f) else DarkBorder,
                RoundedCornerShape(18.dp)
            )
            .testTag("offer_card_${offer.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header do Card
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(offer.appColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = offer.appName,
                        color = if (offer.isMultiStack) NeonGreen else TextLight,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = formattedPrice,
                    color = NeonGreen,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Estabelecimento
            Text(
                text = "🍔 ${offer.restaurant}",
                color = TextLight,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Trajeto
            Text(
                text = "📍 ${offer.pickupAddress} ➔ ${offer.destinationAddress}",
                color = TextMuted,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Estatísticas da Corrida (Distância, R$/km, Tempo)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "🛵 ${offer.distanceKm} km",
                    color = TextMuted,
                    fontSize = 12.sp
                )
                Text(
                    text = "⚡ $formattedPerKm",
                    color = NeonGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Text(
                    text = "⏱️ ${offer.timeMinutes} min",
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Botões de Ação Rápida
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDecline,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RedDecline),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("btn_decline_offer_${offer.id}")
                ) {
                    Text("❌ RECUSAR", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }

                Button(
                    onClick = onAccept,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonGreen,
                        contentColor = DarkBg
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(42.dp)
                        .testTag("btn_accept_offer_${offer.id}")
                ) {
                    Text("✅ ACEITAR", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }
    }
}

// ----------------------------------------------------
// COMPONENTE: ESTADO VAZIO / INFORMATIVO
// ----------------------------------------------------
@Composable
fun RadarEmptyState(
    icon: String,
    title: String,
    subtitle: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(DarkCard)
            .border(1.dp, DarkBorder, RoundedCornerShape(18.dp))
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = icon, fontSize = 32.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                color = TextLight,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                color = TextMuted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
