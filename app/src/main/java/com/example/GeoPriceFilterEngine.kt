package com.example

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

// ==============================================================================
// 1. MODELOS DE DADOS E CRITÉRIOS DE FILTRAGEM (GEOLOCALIZAÇÃO & VALOR MÍNIMO)
// ==============================================================================

/**
 * Representação de um polo gastronômico ou zona de alta concentração de pedidos.
 */
data class GeoHotspot(
    val id: String,
    val name: String,
    val shortName: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float = 650f,
    val surgeBonus: String = "+35%"
)

object GeoHotspotsRegistry {
    val ALL = listOf(
        GeoHotspot("all", "Todos os Polos", "Todos", -23.561684, -46.655981, 15000f, "100%"),
        GeoHotspot("paulista", "Polo Paulista", "Paulista", -23.561684, -46.655981, 650f, "+45%"),
        GeoHotspot("jardins", "Jardins / Oscar Freire", "Jardins", -23.564210, -46.652150, 550f, "+35%"),
        GeoHotspot("pinheiros", "Hub Pinheiros / Fradique", "Pinheiros", -23.567890, -46.684120, 700f, "+40%"),
        GeoHotspot("faria_lima", "Faria Lima / Itaim Bibi", "Faria Lima", -23.585120, -46.681530, 800f, "+50%"),
        GeoHotspot("moema", "Moema / Ibirapuera", "Moema", -23.604512, -46.666491, 650f, "+30%"),
        GeoHotspot("morumbi", "Complexo Morumbi / Berrini", "Morumbi", -23.623100, -46.698900, 900f, "+40%")
    )
}

/**
 * Critérios unificados de filtragem por Geolocalização e Valor Mínimo.
 */
data class GeoPriceFilterCriteria(
    val minValue: Double = 15.0,
    val minGainPerKm: Double = 0.0,
    val isGeoFilterEnabled: Boolean = true,
    val maxRadiusKm: Double = 5.0,
    val selectedHotspotId: String? = null,
    val driverLatitude: Double = -23.561684,
    val driverLongitude: Double = -46.655981
) {
    val isFilterActive: Boolean
        get() = minValue > 0.0 || (isGeoFilterEnabled && maxRadiusKm < 15.0) || minGainPerKm > 0.0 || selectedHotspotId != null
}

/**
 * Resultado detalhado da avaliação de uma oferta.
 */
data class GeoPriceEvaluation(
    val isApproved: Boolean,
    val distanceToPickupKm: Double,
    val gainPerKm: Double,
    val passesMinValue: Boolean,
    val passesGeoRadius: Boolean,
    val passesMinGainPerKm: Boolean,
    val passesHotspot: Boolean,
    val rejectionReason: String? = null
)

// ==============================================================================
// 2. MOTOR DE DECISÃO GEODÉSICO E MATEMÁTICO
// ==============================================================================

object GeoPriceFilterEngine {

    /**
     * Calcula a distância geodésica pela fórmula de Haversine em quilômetros.
     */
    fun calculateHaversineDistanceKm(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return Math.round(earthRadiusKm * c * 100.0) / 100.0
    }

    /**
     * Avalia uma [DeliveryOffer] perante os critérios de geolocalização e valor mínimo.
     */
    fun evaluate(offer: DeliveryOffer, criteria: GeoPriceFilterCriteria): GeoPriceEvaluation {
        val distKm = calculateHaversineDistanceKm(
            criteria.driverLatitude, criteria.driverLongitude,
            offer.latitude, offer.longitude
        )
        val gainKm = offer.ganhoPorKm

        val passesValue = offer.valor >= criteria.minValue
        val passesRadius = !criteria.isGeoFilterEnabled || distKm <= criteria.maxRadiusKm
        val passesGain = criteria.minGainPerKm <= 0.0 || gainKm >= criteria.minGainPerKm

        val passesHotspot = if (criteria.selectedHotspotId.isNullOrEmpty() || criteria.selectedHotspotId == "all") {
            true
        } else {
            val hotspot = GeoHotspotsRegistry.ALL.firstOrNull { it.id == criteria.selectedHotspotId }
            if (hotspot != null) {
                val distToHotspot = calculateHaversineDistanceKm(
                    hotspot.latitude, hotspot.longitude,
                    offer.latitude, offer.longitude
                )
                distToHotspot <= (hotspot.radiusMeters / 1000.0 * 1.5)
            } else true
        }

        val approved = passesValue && passesRadius && passesGain && passesHotspot

        val reason = when {
            !passesValue -> "Valor R$ ${String.format(Locale("pt", "BR"), "%.2f", offer.valor)} abaixo do piso R$ ${String.format(Locale("pt", "BR"), "%.2f", criteria.minValue)}"
            !passesRadius -> "Distância ${distKm}km excede o raio máximo de ${criteria.maxRadiusKm}km"
            !passesGain -> "R$ ${String.format(Locale("pt", "BR"), "%.2f", gainKm)}/km abaixo do mínimo R$ ${String.format(Locale("pt", "BR"), "%.2f", criteria.minGainPerKm)}/km"
            !passesHotspot -> "Fora do polo selecionado"
            else -> null
        }

        return GeoPriceEvaluation(
            isApproved = approved,
            distanceToPickupKm = distKm,
            gainPerKm = gainKm,
            passesMinValue = passesValue,
            passesGeoRadius = passesRadius,
            passesMinGainPerKm = passesGain,
            passesHotspot = passesHotspot,
            rejectionReason = reason
        )
    }

    /**
     * Avalia uma [RadarOffer] perante os critérios de geolocalização e valor mínimo.
     */
    fun evaluateRadar(offer: RadarOffer, criteria: GeoPriceFilterCriteria): GeoPriceEvaluation {
        val distKm = calculateHaversineDistanceKm(
            criteria.driverLatitude, criteria.driverLongitude,
            offer.pickupLat, offer.pickupLng
        )
        val gainKm = offer.gainPerKm

        val passesValue = offer.value >= criteria.minValue
        val passesRadius = !criteria.isGeoFilterEnabled || distKm <= criteria.maxRadiusKm
        val passesGain = criteria.minGainPerKm <= 0.0 || gainKm >= criteria.minGainPerKm

        val passesHotspot = if (criteria.selectedHotspotId.isNullOrEmpty() || criteria.selectedHotspotId == "all") {
            true
        } else {
            val hotspot = GeoHotspotsRegistry.ALL.firstOrNull { it.id == criteria.selectedHotspotId }
            if (hotspot != null) {
                val distToHotspot = calculateHaversineDistanceKm(
                    hotspot.latitude, hotspot.longitude,
                    offer.pickupLat, offer.pickupLng
                )
                distToHotspot <= (hotspot.radiusMeters / 1000.0 * 1.5)
            } else true
        }

        val approved = passesValue && passesRadius && passesGain && passesHotspot

        val reason = when {
            !passesValue -> "Valor R$ ${String.format(Locale("pt", "BR"), "%.2f", offer.value)} abaixo do piso de R$ ${String.format(Locale("pt", "BR"), "%.2f", criteria.minValue)}"
            !passesRadius -> "Coleta a ${distKm}km ultrapassa o raio GPS de ${criteria.maxRadiusKm}km"
            !passesGain -> "R$ ${String.format(Locale("pt", "BR"), "%.2f", gainKm)}/km abaixo de R$ ${String.format(Locale("pt", "BR"), "%.2f", criteria.minGainPerKm)}/km"
            !passesHotspot -> "Fora do polo selecionado"
            else -> null
        }

        return GeoPriceEvaluation(
            isApproved = approved,
            distanceToPickupKm = distKm,
            gainPerKm = gainKm,
            passesMinValue = passesValue,
            passesGeoRadius = passesRadius,
            passesMinGainPerKm = passesGain,
            passesHotspot = passesHotspot,
            rejectionReason = reason
        )
    }
}

// ==============================================================================
// 3. COMPONENTES VISUAIS EM JETPACK COMPOSE
// ==============================================================================

private val NeonGreen = Color(0xFF00FF88)
private val CyberCyan = Color(0xFF00D2FF)
private val DarkBg = Color(0xFF0A0A0F)
private val CardBg = Color(0xFF13131F)
private val CardBorder = Color(0xFF262638)
private val DangerRed = Color(0xFFFF4757)
private val WarningAmber = Color(0xFFFFB300)
private val TextMuted = Color(0xFF8E8EA0)

/**
 * Card de Controle e Calibração dos Filtros de Geolocalização e Valor Mínimo.
 */
@Composable
fun GeoPriceFilterControlCard(
    criteria: GeoPriceFilterCriteria,
    onCriteriaChange: (GeoPriceFilterCriteria) -> Unit,
    totalOffers: Int,
    acceptedOffers: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("geo_price_filter_control_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, if (criteria.isFilterActive) NeonGreen.copy(alpha = 0.5f) else CardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Cabeçalho com Status e Badges de Contagem
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(NeonGreen.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Geolocalização e Filtro",
                            tint = NeonGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Geolocalização & Piso Mínimo",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Filtragem tática GPS por raio e remuneração",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                // Contador de Ofertas Aprovadas
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (acceptedOffers > 0) NeonGreen.copy(alpha = 0.15f) else DangerRed.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, if (acceptedOffers > 0) NeonGreen.copy(alpha = 0.4f) else DangerRed.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "$acceptedOffers / $totalOffers Ativas",
                        color = if (acceptedOffers > 0) NeonGreen else DangerRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SEÇÃO 1: VALOR MÍNIMO DA CORRIDA (R$)
            Text(
                text = "1. PISO DE VALOR MÍNIMO (R$)",
                color = CyberCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (criteria.minValue <= 0.0) "Sem valor mínimo (Todas)" else "R$ ${String.format(Locale("pt", "BR"), "%.2f", criteria.minValue)} por corrida",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                if (criteria.minValue > 0.0) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = NeonGreen.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "Piso Ativo",
                            color = NeonGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Slider(
                value = criteria.minValue.toFloat(),
                onValueChange = { onCriteriaChange(criteria.copy(minValue = Math.round(it * 2) / 2.0)) },
                valueRange = 0f..40f,
                steps = 19,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("slider_min_value"),
                colors = SliderDefaults.colors(
                    thumbColor = NeonGreen,
                    activeTrackColor = NeonGreen,
                    inactiveTrackColor = CardBorder
                )
            )

            // Chips Rápidos de Valor
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val valuePresets = listOf(0.0 to "Todos", 15.0 to "R$ 15+", 20.0 to "R$ 20+", 25.0 to "R$ 25+", 30.0 to "R$ 30+")
                valuePresets.forEach { (valPres, label) ->
                    val isSelected = criteria.minValue == valPres
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) NeonGreen.copy(alpha = 0.2f) else CardBg,
                        border = BorderStroke(1.dp, if (isSelected) NeonGreen else CardBorder),
                        modifier = Modifier
                            .clickable { onCriteriaChange(criteria.copy(minValue = valPres)) }
                            .testTag("chip_value_${valPres.toInt()}")
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) NeonGreen else TextMuted,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SEÇÃO 2: GEOLOCALIZAÇÃO E RAIO MÁXIMO (KM)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "2. RAIO GPS DE COLETA",
                    color = CyberCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (criteria.isGeoFilterEnabled) "Ativo" else "Desativado",
                        color = if (criteria.isGeoFilterEnabled) NeonGreen else TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Switch(
                        checked = criteria.isGeoFilterEnabled,
                        onCheckedChange = { onCriteriaChange(criteria.copy(isGeoFilterEnabled = it)) },
                        modifier = Modifier.testTag("switch_geo_filter"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = NeonGreen,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = CardBorder
                        )
                    )
                }
            }

            if (criteria.isGeoFilterEnabled) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Até ${String.format(Locale("pt", "BR"), "%.1f", criteria.maxRadiusKm)} km do seu GPS atual",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "GPS",
                            tint = NeonGreen,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Paulista ±4.2m",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }

                Slider(
                    value = criteria.maxRadiusKm.toFloat(),
                    onValueChange = { onCriteriaChange(criteria.copy(maxRadiusKm = Math.round(it * 2) / 2.0)) },
                    valueRange = 1f..15f,
                    steps = 27,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("slider_max_radius"),
                    colors = SliderDefaults.colors(
                        thumbColor = CyberCyan,
                        activeTrackColor = CyberCyan,
                        inactiveTrackColor = CardBorder
                    )
                )

                // Chips Rápidos de Raio
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val radiusPresets = listOf(1.5 to "1.5km (Curto)", 3.0 to "3km (Bairro)", 5.0 to "5km (Médio)", 8.0 to "8km (Expandido)", 12.0 to "12km (Macro)")
                    radiusPresets.forEach { (radPres, label) ->
                        val isSelected = criteria.maxRadiusKm == radPres
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) CyberCyan.copy(alpha = 0.2f) else CardBg,
                            border = BorderStroke(1.dp, if (isSelected) CyberCyan else CardBorder),
                            modifier = Modifier
                                .clickable { onCriteriaChange(criteria.copy(maxRadiusKm = radPres)) }
                                .testTag("chip_radius_${radPres.toInt()}")
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) CyberCyan else TextMuted,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SEÇÃO 3: FILTRAGEM POR POLO GASTRONÔMICO
            Text(
                text = "3. POLO GASTRONÔMICO ESPECÍFICO",
                color = CyberCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GeoHotspotsRegistry.ALL.forEach { hotspot ->
                    val isSelected = (criteria.selectedHotspotId == hotspot.id) || (criteria.selectedHotspotId == null && hotspot.id == "all")
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) NeonGreen.copy(alpha = 0.2f) else CardBg,
                        border = BorderStroke(1.dp, if (isSelected) NeonGreen else CardBorder),
                        modifier = Modifier
                            .clickable {
                                onCriteriaChange(
                                    criteria.copy(
                                        selectedHotspotId = if (hotspot.id == "all") null else hotspot.id
                                    )
                                )
                            }
                            .testTag("hotspot_chip_${hotspot.id}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = hotspot.shortName,
                                color = if (isSelected) NeonGreen else Color.White,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            if (hotspot.surgeBonus.isNotEmpty() && hotspot.id != "all") {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = hotspot.surgeBonus,
                                    color = CyberCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Botão de Redefinir se filtros ativos
            if (criteria.isFilterActive) {
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedButton(
                    onClick = { onCriteriaChange(GeoPriceFilterCriteria(minValue = 0.0, maxRadiusKm = 15.0, isGeoFilterEnabled = false, selectedHotspotId = null)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .testTag("btn_reset_geo_price_filter"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMuted),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Limpar", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Limpar Filtros e Mostrar Todas", fontSize = 12.sp)
                }
            }
        }
    }
}

/**
 * Item de Oferta individual com selo de Geolocalização e Valor Mínimo.
 */
@Composable
fun FilteredDeliveryOfferCard(
    offer: DeliveryOffer,
    evaluation: GeoPriceEvaluation,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("offer_card_${offer.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(
            1.dp,
            if (evaluation.isApproved) NeonGreen.copy(alpha = 0.4f) else DangerRed.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Cabeçalho da Oferta
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = offer.nomeRestaurante,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = CyberCyan.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = offer.poloGastronomico,
                                color = CyberCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "📍 A ${String.format(Locale("pt", "BR"), "%.1f", evaluation.distanceToPickupKm)} km de você",
                            color = if (evaluation.passesGeoRadius) NeonGreen else DangerRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Valor Bruto em Destaque
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "R$ ${String.format(Locale("pt", "BR"), "%.2f", offer.valor)}",
                        color = NeonGreen,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "R$ ${String.format(Locale("pt", "BR"), "%.2f", offer.ganhoPorKm)}/km",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Status da Avaliação do Filtro
            if (!evaluation.isApproved && evaluation.rejectionReason != null) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = DangerRed.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Alerta",
                            tint = DangerRed,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = evaluation.rejectionReason,
                            color = DangerRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Linha de Ações (Aceitar / Recusar)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDecline,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("decline_offer_${offer.id}"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMuted),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Recusar", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Recusar", fontSize = 13.sp)
                }

                Button(
                    onClick = onAccept,
                    modifier = Modifier
                        .weight(1.5f)
                        .height(44.dp)
                        .testTag("accept_offer_${offer.id}"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (evaluation.isApproved) NeonGreen else TextMuted
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Aceitar",
                        tint = DarkBg,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Aceitar Corrida",
                        color = DarkBg,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Seção Completa e Interativa integrando o Controlador de Filtro e a Lista Filtrada.
 */
@Composable
fun GeoPriceFilteredOffersSection(
    offers: List<DeliveryOffer>,
    criteria: GeoPriceFilterCriteria,
    onCriteriaChange: (GeoPriceFilterCriteria) -> Unit,
    onAcceptOffer: (DeliveryOffer) -> Unit,
    onDeclineOffer: (DeliveryOffer) -> Unit,
    modifier: Modifier = Modifier
) {
    // Avaliação de cada oferta
    val evaluatedList = remember(offers, criteria) {
        offers.map { offer ->
            Pair(offer, GeoPriceFilterEngine.evaluate(offer, criteria))
        }
    }

    val approvedOffers = evaluatedList.filter { it.second.isApproved }
    val totalCount = offers.size
    val approvedCount = approvedOffers.size

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("geo_price_filtered_offers_section")
    ) {
        // Painel de Controle de Filtros
        GeoPriceFilterControlCard(
            criteria = criteria,
            onCriteriaChange = onCriteriaChange,
            totalOffers = totalCount,
            acceptedOffers = approvedCount
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Lista de Ofertas Aprovadas
        if (approvedOffers.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .testTag("empty_filtered_offers_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Nenhuma oferta",
                        tint = TextMuted,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Nenhuma oferta dentro dos critérios",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Experimente reduzir o piso de valor (R$ ${String.format(Locale("pt", "BR"), "%.2f", criteria.minValue)}) ou expandir o raio GPS (${criteria.maxRadiusKm} km).",
                        color = TextMuted,
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            onCriteriaChange(
                                criteria.copy(
                                    minValue = 0.0,
                                    maxRadiusKm = 15.0,
                                    isGeoFilterEnabled = false,
                                    selectedHotspotId = null
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("btn_relax_filters")
                    ) {
                        Text("Ver Todas as Ofertas Disponíveis", color = DarkBg, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                approvedOffers.forEach { (offer, eval) ->
                    FilteredDeliveryOfferCard(
                        offer = offer,
                        evaluation = eval,
                        onAccept = { onAcceptOffer(offer) },
                        onDecline = { onDeclineOffer(offer) }
                    )
                }
            }
        }
    }
}
