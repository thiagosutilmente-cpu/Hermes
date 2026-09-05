package com.example

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

// Paleta visual inspirada no tema Cyberpunk / Dark Radar
private val DarkBg = Color(0xFF0A0A0F)
private val CardBg = Color(0xFF13131F)
private val NeonGreen = Color(0xFF00FF88)
private val CyberCyan = Color(0xFF00D2FF)
private val TextMuted = Color(0xFF8E8EA0)
private val DangerRed = Color(0xFFFF4757)

/**
 * Composable que renderiza um Card do Material 3 com os dados de uma [DeliveryOffer].
 *
 * @param offer Objeto com os dados da oferta (id, nomeRestaurante, valor, distancia, tempoEstimado).
 * @param modifier Modificador de layout do Compose.
 * @param onAccept Callback acionado ao clicar em Aceitar.
 * @param onDecline Callback acionado ao clicar em Recusar.
 */
@Composable
fun DeliveryOfferCard(
    offer: DeliveryOffer,
    modifier: Modifier = Modifier,
    onAccept: (DeliveryOffer) -> Unit = {},
    onDecline: (DeliveryOffer) -> Unit = {}
) {
    val formattedPrice = String.format(Locale.GERMANY, "R$ %.2f", offer.valor)
    val formattedDistance = String.format(Locale.GERMANY, "%.1f km", offer.distancia)
    val formattedGainPerKm = String.format(Locale.GERMANY, "R$ %.2f/km", offer.ganhoPorKm)

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "DeliveryOfferCardScale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(cardScale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { /* Permite o feedback tátil de clique no card */ }
            )
            .border(
                width = 1.dp,
                color = if (offer.isAltaRentabilidade) NeonGreen.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(16.dp)
            )
            .testTag("delivery_offer_card_${offer.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Linha Superior: Nome do Restaurante + Tag de Rentabilidade + Valor
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = offer.nomeRestaurante,
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    if (offer.isAltaRentabilidade) {
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
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formattedPrice,
                        color = NeonGreen,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = formattedGainPerKm,
                        color = if (offer.isAltaRentabilidade) NeonGreen else TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Linha de Métricas: Distância e Tempo Estimado
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = 0.3f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "📏", fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Distância: $formattedDistance",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "⏱️", fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Tempo: ~${offer.tempoEstimado} min",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Linha de Ações: Recusar e Aceitar com Material Icons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { onDecline(offer) },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("btn_decline_${offer.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Ícone de recusar oferta",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("RECUSAR", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { onAccept(offer) },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonGreen,
                        contentColor = DarkBg
                    ),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(42.dp)
                        .testTag("btn_accept_${offer.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Ícone de aceitar oferta",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ACEITAR", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Composable que renderiza a lista completa de ofertas em uma LazyColumn.
 */
@Composable
fun OfferList(
    offers: List<DeliveryOffer>,
    modifier: Modifier = Modifier,
    onAccept: (DeliveryOffer) -> Unit = {},
    onDecline: (DeliveryOffer) -> Unit = {}
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        items(
            items = offers,
            key = { it.id }
        ) { offer ->
            DeliveryOfferCard(
                offer = offer,
                onAccept = onAccept,
                onDecline = onDecline
            )
        }
    }
}

// ----------------------------------------------------
// PREVIEW DO CARD NO MATERIAL 3
// ----------------------------------------------------
@Preview(
    name = "Delivery Offer Card Preview",
    showBackground = true,
    backgroundColor = 0xFF0A0A0F,
    widthDp = 380
)
@Composable
fun DeliveryOfferCardPreview() {
    MaterialTheme {
        Surface(color = DarkBg, modifier = Modifier.padding(16.dp)) {
            DeliveryOfferCard(
                offer = DeliveryOffer(
                    id = "stk_01",
                    nomeRestaurante = "Burger King - Av. Paulista",
                    valor = 22.50,
                    distancia = 3.8,
                    tempoEstimado = 16
                )
            )
        }
    }
}
