package com.example.radar.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.radar.data.DeliveryOffer
import java.util.Locale

private val DarkBackground = Color(0xFF0A0A0F)
private val CardSurface = Color(0xFF13131F)
private val NeonGreen = Color(0xFF00FF88)
private val CyberCyan = Color(0xFF00D2FF)
private val TextMuted = Color(0xFF8E8EA0)
private val IFoodColor = Color(0xFFEA1D2C)
private val RappiColor = Color(0xFFFF441F)
private val UberColor = Color(0xFF276EF1)
private val App99Color = Color(0xFFF7C200)

@Composable
fun OfferListScreen(
    offers: List<DeliveryOffer> = sampleDeliveryOffers(),
    onAcceptOffer: (DeliveryOffer) -> Unit = {},
    onDeclineOffer: (DeliveryOffer) -> Unit = {},
    onOpenMapRoute: (DeliveryOffer) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var offerList by remember { mutableStateOf(offers) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp)
    ) {
        // Cabeçalho da Lista de Ofertas
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "OFERTAS DISPONÍVEIS",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "${offerList.size} oportunidades no radar",
                    color = NeonGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            IconButton(
                onClick = { offerList = sampleDeliveryOffers() },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(CardSurface)
                    .testTag("btn_refresh_offers")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Atualizar ofertas",
                    tint = NeonGreen
                )
            }
        }

        if (offerList.isEmpty()) {
            // Empty State
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "🎯",
                        fontSize = 48.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Nenhuma oferta no momento",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "O radar está monitorando os apps parceiros em tempo real...",
                        color = TextMuted,
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("delivery_offers_list"),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(
                    items = offerList,
                    key = { it.id }
                ) { offer ->
                    DeliveryOfferItemCard(
                        offer = offer,
                        onAccept = {
                            onAcceptOffer(offer)
                            offerList = offerList.filter { it.id != offer.id }
                        },
                        onDecline = {
                            onDeclineOffer(offer)
                            offerList = offerList.filter { it.id != offer.id }
                        },
                        onOpenMap = { onOpenMapRoute(offer) }
                    )
                }
            }
        }
    }
}

@Composable
fun DeliveryOfferItemCard(
    offer: DeliveryOffer,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onOpenMap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appBadgeColor = when (offer.appName.uppercase()) {
        "IFOOD" -> IFoodColor
        "RAPPI" -> RappiColor
        "UBER", "UBER EATS" -> UberColor
        "99", "99 FOOD" -> App99Color
        else -> NeonGreen
    }

    val isGoodRate = offer.gainPerKm >= 5.0

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (isGoodRate) NeonGreen.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(16.dp)
            )
            .testTag("offer_card_${offer.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Linha do Topo: App + Restaurante + Valor Total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(appBadgeColor.copy(alpha = 0.2f))
                                .border(1.dp, appBadgeColor.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = offer.appName.uppercase(),
                                color = appBadgeColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        if (isGoodRate) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(NeonGreen.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "ALTA RENTABILIDADE",
                                    color = NeonGreen,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = offer.restaurant,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = String.format(Locale.GERMANY, "R$ %.2f", offer.value),
                        color = NeonGreen,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = String.format(Locale.GERMANY, "R$ %.2f/km", offer.gainPerKm),
                        color = if (isGoodRate) NeonGreen else TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Rota: Coleta e Entrega
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = 0.35f))
                    .padding(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = appBadgeColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = offer.pickupAddress,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = offer.deliveryAddress,
                        color = TextMuted,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "📏 ${String.format(Locale.GERMANY, "%.1f", offer.distanceKm)} km total",
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "⏱️ ~${offer.timeMinutes} min estimados",
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Linha de Ações: Recusar, Maps, Aceitar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDecline,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF4757)),
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("btn_decline_${offer.id}")
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("RECUSAR", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onOpenMap,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .border(1.dp, CyberCyan.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .testTag("btn_maps_${offer.id}")
                ) {
                    Icon(imageVector = Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("MAPS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onAccept,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonGreen,
                        contentColor = DarkBackground
                    ),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(42.dp)
                        .testTag("btn_accept_${offer.id}")
                ) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ACEITAR", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Mock de Ofertas para Inicialização e Previews
fun sampleDeliveryOffers(): List<DeliveryOffer> = listOf(
    DeliveryOffer(
        id = "stk_01",
        appName = "iFood + Rappi",
        restaurant = "Burger King Paulista & Pizza Hut",
        value = 33.00,
        distanceKm = 4.2,
        timeMinutes = 18,
        pickupAddress = "Av. Paulista, 1200",
        deliveryAddress = "Edifício Paulista Corporate",
        neuralDecision = "ACCEPT",
        confidence = 0.98
    ),
    DeliveryOffer(
        id = "stk_02",
        appName = "iFood",
        restaurant = "McDonald's Henrique Schaumann",
        value = 15.00,
        distanceKm = 2.8,
        timeMinutes = 12,
        pickupAddress = "Av. Henrique Schaumann, 800",
        deliveryAddress = "Rua Augusta, 1500",
        neuralDecision = "ACCEPT",
        confidence = 0.85
    ),
    DeliveryOffer(
        id = "stk_03",
        appName = "Rappi",
        restaurant = "Starbucks Shopping Frei Caneca",
        value = 18.00,
        distanceKm = 3.1,
        timeMinutes = 14,
        pickupAddress = "Rua Frei Caneca, 569",
        deliveryAddress = "Av. Consolação, 2000",
        neuralDecision = "ACCEPT",
        confidence = 0.92
    ),
    DeliveryOffer(
        id = "stk_04",
        appName = "99 Food",
        restaurant = "Madero Container Jardins",
        value = 24.50,
        distanceKm = 4.5,
        timeMinutes = 20,
        pickupAddress = "Alameda Santos, 980",
        deliveryAddress = "Alameda Lorena, 1400",
        neuralDecision = "ACCEPT",
        confidence = 0.89
    ),
    DeliveryOffer(
        id = "stk_05",
        appName = "Uber Eats",
        restaurant = "Habib's Rebouças",
        value = 11.00,
        distanceKm = 3.9,
        timeMinutes = 15,
        pickupAddress = "Av. Rebouças, 2200",
        deliveryAddress = "Rua dos Pinheiros, 750",
        neuralDecision = "DECLINE",
        confidence = 0.65
    )
)

// ----------------------------------------------------
// PREVIEW DO JETPACK COMPOSE
// ----------------------------------------------------
@Preview(
    name = "Lista de Ofertas - Radar Delivery",
    showBackground = true,
    backgroundColor = 0xFF0A0A0F,
    widthDp = 390,
    heightDp = 844
)
@Composable
fun OfferListScreenPreview() {
    MaterialTheme {
        Surface(color = DarkBackground) {
            OfferListScreen()
        }
    }
}
