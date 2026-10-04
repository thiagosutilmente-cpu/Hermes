package com.example

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

// Paleta visual inspirada no tema Cyberpunk / Dark Radar
private val DarkBg = Color(0xFF0A0A0F)
private val CardBg = Color(0xFF13131F)
private val CardBgElevated = Color(0xFF1B1B2C)
private val NeonGreen = Color(0xFF00FF88)
private val CyberCyan = Color(0xFF00D2FF)
private val TextMuted = Color(0xFF8E8EA0)
private val DangerRed = Color(0xFFFF4757)
private val AccentGold = Color(0xFFFFD166)

/**
 * Filtros rápidos por faixa de valor monetário da corrida (R$)
 */
enum class QuickValueFilter(val label: String, val minVal: Double) {
    ALL("Todos os Valores", 0.0),
    MIN_15("R$ 15+", 15.0),
    MIN_20("R$ 20+", 20.0),
    MIN_25("R$ 25+", 25.0),
    MIN_30("R$ 30+", 30.0)
}

/**
 * Filtros rápidos por distância máxima de entrega (km)
 */
enum class QuickDistanceFilter(val label: String, val maxKm: Double) {
    ALL("Qualquer Distância", Double.MAX_VALUE),
    SHORT_3KM("Tiro Curto (≤ 3 km)", 3.0),
    MEDIUM_5KM("Até 5 km", 5.0),
    LONG_8KM("Até 8 km", 8.0)
}

/**
 * Ordenação rápida de prioridade na esteira
 */
enum class OfferSortOrder(val label: String) {
    RECOMMENDED("Sinergia IA"),
    HIGHEST_VALUE("Maior Valor"),
    SHORTEST_DISTANCE("Menor Distância"),
    BEST_PROFIT_KM("Maior R$/km")
}

/**
 * Interface completa de Lista de Ofertas com LazyColumn e Filtros Rápidos por Valor ou Distância.
 *
 * @param offers Lista de ofertas brutas recebidas das plataformas de entrega.
 * @param modifier Modificador Compose para layout.
 * @param onAccept Callback disparado ao aceitar uma oferta.
 * @param onDecline Callback disparado ao rejeitar uma oferta.
 */
@Composable
fun OfferList(
    offers: List<DeliveryOffer>,
    modifier: Modifier = Modifier,
    onAccept: (DeliveryOffer) -> Unit = {},
    onDecline: (DeliveryOffer) -> Unit = {}
) {
    var selectedValueFilter by remember { mutableStateOf(QuickValueFilter.ALL) }
    var selectedDistanceFilter by remember { mutableStateOf(QuickDistanceFilter.ALL) }
    var selectedSortOrder by remember { mutableStateOf(OfferSortOrder.RECOMMENDED) }
    var onlyHighProfit by remember { mutableStateOf(false) }

    // Aplicação dos filtros em tempo real
    val filteredOffers by remember(
        offers,
        selectedValueFilter,
        selectedDistanceFilter,
        selectedSortOrder,
        onlyHighProfit
    ) {
        derivedStateOf {
            offers.filter { offer ->
                val matchesValue = offer.valor >= selectedValueFilter.minVal
                val matchesDistance = offer.distancia <= selectedDistanceFilter.maxKm
                val matchesProfit = !onlyHighProfit || offer.isAltaRentabilidade
                matchesValue && matchesDistance && matchesProfit
            }.let { list ->
                when (selectedSortOrder) {
                    OfferSortOrder.RECOMMENDED -> list // Mantém a pontuação neural do simulador
                    OfferSortOrder.HIGHEST_VALUE -> list.sortedByDescending { it.valor }
                    OfferSortOrder.SHORTEST_DISTANCE -> list.sortedBy { it.distancia }
                    OfferSortOrder.BEST_PROFIT_KM -> list.sortedByDescending { it.ganhoPorKm }
                }
            }
        }
    }

    val isFilterActive = selectedValueFilter != QuickValueFilter.ALL ||
            selectedDistanceFilter != QuickDistanceFilter.ALL ||
            selectedSortOrder != OfferSortOrder.RECOMMENDED ||
            onlyHighProfit

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        // ----------------------------------------------------
        // SEÇÃO DE FILTROS RÁPIDOS (Valor & Distância)
        // ----------------------------------------------------
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBg)
                .padding(vertical = 12.dp)
        ) {
            // Linha 1: Filtros Rápidos por Valor
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "VALOR:",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickValueFilter.entries.forEach { filter ->
                        val isSelected = selectedValueFilter == filter
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedValueFilter = filter },
                            label = {
                                Text(
                                    text = filter.label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonGreen.copy(alpha = 0.2f),
                                selectedLabelColor = NeonGreen,
                                containerColor = Color.White.copy(alpha = 0.05f),
                                labelColor = Color.LightGray
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = Color.White.copy(alpha = 0.1f),
                                selectedBorderColor = NeonGreen
                            ),
                            modifier = Modifier.testTag("filter_value_${filter.name}")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Linha 2: Filtros Rápidos por Distância
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RAIO:",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickDistanceFilter.entries.forEach { filter ->
                        val isSelected = selectedDistanceFilter == filter
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedDistanceFilter = filter },
                            label = {
                                Text(
                                    text = filter.label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyberCyan.copy(alpha = 0.2f),
                                selectedLabelColor = CyberCyan,
                                containerColor = Color.White.copy(alpha = 0.05f),
                                labelColor = Color.LightGray
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = Color.White.copy(alpha = 0.1f),
                                selectedBorderColor = CyberCyan
                            ),
                            modifier = Modifier.testTag("filter_dist_${filter.name}")
                        )
                    }

                    // Chip de Alta Rentabilidade
                    FilterChip(
                        selected = onlyHighProfit,
                        onClick = { onlyHighProfit = !onlyHighProfit },
                        label = {
                            Text(
                                text = "⭐ R$ 5+/km",
                                fontSize = 12.sp,
                                fontWeight = if (onlyHighProfit) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentGold.copy(alpha = 0.25f),
                            selectedLabelColor = AccentGold,
                            containerColor = Color.White.copy(alpha = 0.05f),
                            labelColor = Color.LightGray
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = onlyHighProfit,
                            borderColor = Color.White.copy(alpha = 0.1f),
                            selectedBorderColor = AccentGold
                        ),
                        modifier = Modifier.testTag("filter_high_profit")
                    )
                }
            }

            // Barra de Resumo & Limpar Filtros
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val avgGain = if (filteredOffers.isNotEmpty()) {
                    filteredOffers.map { it.ganhoPorKm }.average()
                } else 0.0

                Text(
                    text = "${filteredOffers.size} ofertas ativas • Média R$ ${String.format(Locale.GERMANY, "%.2f", avgGain)}/km",
                    color = if (filteredOffers.isNotEmpty()) NeonGreen else DangerRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )

                if (isFilterActive) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                selectedValueFilter = QuickValueFilter.ALL
                                selectedDistanceFilter = QuickDistanceFilter.ALL
                                selectedSortOrder = OfferSortOrder.RECOMMENDED
                                onlyHighProfit = false
                            }
                            .background(Color.White.copy(alpha = 0.08f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "Limpar Filtros",
                            tint = Color.LightGray,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Limpar Filtros",
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // ----------------------------------------------------
        // LISTA DE OFERTAS COM LAZYCOLUMN OU EMPTY STATE
        // ----------------------------------------------------
        if (filteredOffers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.04f))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "📡", fontSize = 32.sp)
                    }

                    Text(
                        text = "Nenhuma oferta com esses critérios",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Ajuste o filtro de valor mínimo ou aumente o raio de distância para receber novas chamadas do radar.",
                        color = TextMuted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Button(
                        onClick = {
                            selectedValueFilter = QuickValueFilter.ALL
                            selectedDistanceFilter = QuickDistanceFilter.ALL
                            onlyHighProfit = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonGreen,
                            contentColor = DarkBg
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Ver Todas as Ofertas", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
            ) {
                items(
                    items = filteredOffers,
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
    }
}

/**
 * Composable que renderiza um Card do Material 3 com os dados de uma [DeliveryOffer].
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
        targetValue = if (isPressed) 0.97f else 1.0f,
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
            .border(
                width = 1.dp,
                color = if (offer.isAltaRentabilidade) NeonGreen.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.08f),
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Badge da Plataforma (iFood, Uber, 99, Rappi)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    when {
                                        offer.appOrigem.contains("iFood", true) -> Color(0xFFEA1D2C).copy(alpha = 0.2f)
                                        offer.appOrigem.contains("Uber", true) -> Color.White.copy(alpha = 0.15f)
                                        offer.appOrigem.contains("99", true) -> Color(0xFFFFB300).copy(alpha = 0.25f)
                                        else -> CyberCyan.copy(alpha = 0.2f)
                                    }
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = offer.appOrigem,
                                color = when {
                                    offer.appOrigem.contains("iFood", true) -> Color(0xFFFF5252)
                                    offer.appOrigem.contains("Uber", true) -> Color.White
                                    offer.appOrigem.contains("99", true) -> Color(0xFFFFD54F)
                                    else -> CyberCyan
                                },
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = offer.nomeRestaurante,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (offer.isAltaRentabilidade) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(NeonGreen.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "⚡ ALTA RENTABILIDADE",
                                    color = NeonGreen,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(CyberCyan.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "📍 ${offer.poloGastronomico}",
                                color = CyberCyan,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formattedPrice,
                        color = NeonGreen,
                        fontSize = 21.sp,
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
                    .background(Color.Black.copy(alpha = 0.35f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = "Distância",
                        tint = CyberCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Distância: $formattedDistance",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "⏱️", fontSize = 12.sp)
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

            // Linha de Ações: Recusar e Aceitar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { onDecline(offer) },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DangerRed.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("btn_decline_${offer.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Recusar",
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
                        .weight(1.3f)
                        .height(44.dp)
                        .testTag("btn_accept_${offer.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Aceitar",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ACEITAR", fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

// ----------------------------------------------------
// PREVIEW DA LISTA DE OFERTAS
// ----------------------------------------------------
@Preview(
    name = "Offer List Preview",
    showBackground = true,
    backgroundColor = 0xFF0A0A0F,
    widthDp = 380,
    heightDp = 700
)
@Composable
fun OfferListPreview() {
    val sampleOffers = listOf(
        DeliveryOffer(
            id = "stk_01",
            nomeRestaurante = "Madero Container - Paulista",
            valor = 26.50,
            distancia = 3.2,
            tempoEstimado = 14,
            appOrigem = "iFood"
        ),
        DeliveryOffer(
            id = "stk_02",
            nomeRestaurante = "Outback Steakhouse - Jardins",
            valor = 34.00,
            distancia = 4.8,
            tempoEstimado = 22,
            appOrigem = "Rappi"
        ),
        DeliveryOffer(
            id = "stk_03",
            nomeRestaurante = "McDonald's - Augusta",
            valor = 14.50,
            distancia = 2.1,
            tempoEstimado = 9,
            appOrigem = "Uber"
        ),
        DeliveryOffer(
            id = "stk_04",
            nomeRestaurante = "Habib's - Consolação",
            valor = 18.00,
            distancia = 5.6,
            tempoEstimado = 19,
            appOrigem = "99"
        )
    )

    MaterialTheme {
        OfferList(offers = sampleOffers)
    }
}
