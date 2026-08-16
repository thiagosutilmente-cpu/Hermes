package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.coordinator.ActiveOffer
import com.example.coordinator.RadarCoordinator
import com.example.service.gemini.GeminiOfferEvaluation
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.RoundCap
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import kotlin.math.*

/**
 * Item individual para representar uma oferta no mapa de constelações
 */
data class OfferClusterItem(
    val id: String,
    val appName: String,
    val fareValue: Double,
    val pickupAddress: String,
    val deliveryAddress: String,
    val pickupLatLng: LatLng,
    val deliveryLatLng: LatLng,
    val gainPerKm: Double,
    val totalDistance: Double,
    val isRecommendedByGemini: Boolean = false
)

/**
 * Cluster geográfico de pedidos próximos (Constelação de Ofertas)
 */
data class OfferClusterGroup(
    val clusterId: String,
    val centerLatLng: LatLng,
    val items: List<OfferClusterItem>,
    val totalEarnings: Double,
    val averageGainPerKm: Double,
    val appsInvolved: List<String>,
    val radiusMeters: Double
)

/**
 * Componente Visual com Google Maps Compose que renderiza "Constelações" de ofertas
 * próximas com clustering dinâmico e suporte integrado à solicitação de permissão
 * de localização em tempo real (ACCESS_FINE_LOCATION).
 */
@Composable
fun OfferConstellationMap(
    offers: List<ActiveOffer>,
    geminiEvaluation: GeminiOfferEvaluation? = null,
    modifier: Modifier = Modifier,
    onOfferSelected: ((ActiveOffer) -> Unit)? = null,
    onClusterSelected: ((OfferClusterGroup) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Hook dinâmico de Permissão de Localização
    val locationPermissionState = rememberLocationPermissionState(
        onPermissionGranted = {
            RadarCoordinator.addLog("GPS de Alta Precisão (ACCESS_FINE_LOCATION) concedido.", com.example.coordinator.LogType.SUCCESS)
        }
    )

    val currentLocation by RadarCoordinator.currentLocation.collectAsStateWithLifecycle()
    val speedKmh by RadarCoordinator.currentSpeedKmh.collectAsStateWithLifecycle()

    val riderLatLng = LatLng(
        currentLocation?.latitude ?: -23.550520,
        currentLocation?.longitude ?: -46.633308
    )

    // Configura posição inicial da câmera
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(riderLatLng, 14.5f)
    }

    // Segue a posição do piloto suavemente quando o GPS atualizar
    LaunchedEffect(currentLocation?.latitude, currentLocation?.longitude) {
        if (currentLocation != null) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLng(riderLatLng),
                durationMs = 600
            )
        }
    }

    var selectedCluster by remember { mutableStateOf<OfferClusterGroup?>(null) }
    var selectedItem by remember { mutableStateOf<OfferClusterItem?>(null) }
    var showConstellationRays by remember { mutableStateOf(true) }

    // Animação suave de pulso para rotas e ofertas avaliadas pelo Gemini
    val infiniteTransition = rememberInfiniteTransition(label = "geminiMarkerPulse")
    val geminiPulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.22f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "geminiPulseScale"
    )
    val geminiPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.50f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "geminiPulseAlpha"
    )

    // Converte e calcula as coordenadas geográficas dos pedidos em torno do piloto
    val clusterItems = remember(offers, geminiEvaluation, riderLatLng) {
        offers.mapIndexed { index, offer ->
            val angle = ((index * 68) % 360) * (Math.PI / 180.0)
            val dist = 0.007 * (1 + (index % 3) * 0.4)

            val pLat = riderLatLng.latitude + (sin(angle) * dist)
            val pLng = riderLatLng.longitude + (cos(angle) * dist)
            val dLat = pLat + (sin(angle + 0.5) * 0.006)
            val dLng = pLng + (cos(angle + 0.5) * 0.006)

            val gainPerKm = if (offer.totalDistance > 0) offer.fareValue / offer.totalDistance else 0.0
            val isGeminiChoice = geminiEvaluation?.selectedApp.equals(offer.appName, ignoreCase = true)

            OfferClusterItem(
                id = "offer_${index + 1}",
                appName = offer.appName,
                fareValue = offer.fareValue,
                pickupAddress = offer.pickupAddress,
                deliveryAddress = offer.deliveryAddress,
                pickupLatLng = LatLng(pLat, pLng),
                deliveryLatLng = LatLng(dLat, dLng),
                gainPerKm = gainPerKm,
                totalDistance = offer.totalDistance,
                isRecommendedByGemini = isGeminiChoice
            )
        }
    }

    // Algoritmo de Clustering Geográfico: agrupa ofertas que estejam em um raio próximo (~850m)
    val clusters = remember(clusterItems) {
        val groups = mutableListOf<OfferClusterGroup>()
        val visited = mutableSetOf<String>()

        clusterItems.forEachIndexed { i, item ->
            if (item.id !in visited) {
                val groupItems = mutableListOf(item)
                visited.add(item.id)

                clusterItems.forEachIndexed { j, other ->
                    if (i != j && other.id !in visited) {
                        val dist = calculateDistanceMeters(item.pickupLatLng, other.pickupLatLng)
                        if (dist <= 850.0) { // Raio de agrupamento de ~850 metros
                            groupItems.add(other)
                            visited.add(other.id)
                        }
                    }
                }

                val avgLat = groupItems.map { it.pickupLatLng.latitude }.average()
                val avgLng = groupItems.map { it.pickupLatLng.longitude }.average()
                val totalValue = groupItems.sumOf { it.fareValue }
                val avgGain = groupItems.map { it.gainPerKm }.average()
                val apps = groupItems.map { it.appName }.distinct()

                groups.add(
                    OfferClusterGroup(
                        clusterId = "cluster_${groups.size + 1}",
                        centerLatLng = LatLng(avgLat, avgLng),
                        items = groupItems,
                        totalEarnings = totalValue,
                        averageGainPerKm = avgGain,
                        appsInvolved = apps,
                        radiusMeters = if (groupItems.size > 1) 450.0 else 200.0
                    )
                )
            }
        }
        groups
    }

    val mapProperties = remember(locationPermissionState.hasPermission) {
        MapProperties(
            mapType = MapType.NORMAL,
            isMyLocationEnabled = false,
            isTrafficEnabled = true,
            mapStyleOptions = MapStyleOptions(CYBER_NEURAL_MAP_STYLE)
        )
    }

    val mapUiSettings = remember {
        MapUiSettings(
            zoomControlsEnabled = false,
            compassEnabled = true,
            myLocationButtonEnabled = false,
            rotationGesturesEnabled = true,
            scrollGesturesEnabled = true,
            tiltGesturesEnabled = true,
            zoomGesturesEnabled = true
        )
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Se a permissão de GPS não estiver concedida, exibe o Card de solicitação
        if (!locationPermissionState.hasPermission) {
            LocationPermissionCard(
                onRequestPermission = { locationPermissionState.requestPermission() }
            )
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, Color(0xFF00FF88).copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                .testTag("offer_constellation_map"),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1117))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header do Mapa de Constelações
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF161B22))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("✨", fontSize = 14.sp)
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "CONSTELAÇÃO DE OFERTAS",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF00FF88),
                                    letterSpacing = 1.sp
                                )
                                Surface(
                                    color = Color(0x3300F0FF),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "${clusters.size} CLUSTERS",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF00F0FF),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = if (locationPermissionState.hasPermission) 
                                    "GPS Ativo • ${offers.size} pedidos mapeados em órbita"
                                else 
                                    "GPS Aguardando Permissão • Modo Estimado",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 10.sp,
                                color = if (locationPermissionState.hasPermission) Color.LightGray else Color(0xFFFFB800)
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = { showConstellationRays = !showConstellationRays },
                            modifier = Modifier
                                .size(30.dp)
                                .background(if (showConstellationRays) Color(0x3300FF88) else Color.Transparent, CircleShape)
                        ) {
                            Text(if (showConstellationRays) "🌌" else "🛰️", fontSize = 12.sp)
                        }

                        IconButton(
                            onClick = {
                                if (!locationPermissionState.hasPermission) {
                                    locationPermissionState.requestPermission()
                                } else {
                                    coroutineScope.launch {
                                        cameraPositionState.animate(
                                            CameraUpdateFactory.newLatLngZoom(riderLatLng, 15f),
                                            800
                                        )
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(30.dp)
                                .background(Color(0xFF21262D), CircleShape)
                        ) {
                            Icon(
                                imageVector = if (locationPermissionState.hasPermission) Icons.Default.MyLocation else Icons.Default.LocationSearching,
                                contentDescription = "Centralizar",
                                tint = if (locationPermissionState.hasPermission) Color(0xFF00FF88) else Color(0xFFFFB800),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }

                // Área de Renderização do Google Maps
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        properties = mapProperties,
                        uiSettings = mapUiSettings,
                        onMapClick = {
                            selectedCluster = null
                            selectedItem = null
                        }
                    ) {
                        // 1. Marcador Central do Piloto (Moto)
                        Marker(
                            state = rememberMarkerState(position = riderLatLng),
                            title = "Você (Piloto)",
                            snippet = if (locationPermissionState.hasPermission) "GPS Alta Precisão" else "GPS Simulado",
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
                            zIndex = 10f
                        )

                        // Círculo de Proximidade Imediata (800m)
                        Circle(
                            center = riderLatLng,
                            radius = 800.0,
                            strokeColor = Color(0x6400FF88),
                            strokeWidth = 2f,
                            fillColor = Color(0x0F00FF88)
                        )

                        // 2. Renderização de Clusters Geográficos (Constelações)
                        clusters.forEach { cluster ->
                            val isMulti = cluster.items.size > 1
                            val hasGemini = cluster.items.any { it.isRecommendedByGemini }

                            // Círculo gravitacional do cluster com pulsação suave em rotas do Gemini
                            val effectiveRadius = if (hasGemini) cluster.radiusMeters * geminiPulseScale else cluster.radiusMeters
                            Circle(
                                center = cluster.centerLatLng,
                                radius = effectiveRadius,
                                strokeColor = if (hasGemini) Color(0xFF00FF88).copy(alpha = geminiPulseAlpha * 1.5f) else if (isMulti) Color(0xAA00F0FF) else Color(0x55FFB800),
                                strokeWidth = if (hasGemini) 3.5f else 2f,
                                fillColor = if (hasGemini) Color(0xFF00FF88).copy(alpha = geminiPulseAlpha * 0.45f) else if (isMulti) Color(0x1800F0FF) else Color(0x10FFB800)
                            )

                            // Linhas de conexão constelar ligando o centro aos nós do cluster
                            if (showConstellationRays && isMulti) {
                                cluster.items.forEach { item ->
                                    Polyline(
                                        points = listOf(cluster.centerLatLng, item.pickupLatLng, item.deliveryLatLng),
                                        color = if (item.isRecommendedByGemini) Color(0xE600FF88) else Color(0x9900F0FF),
                                        width = if (item.isRecommendedByGemini) 5f else 3f,
                                        jointType = JointType.ROUND,
                                        startCap = RoundCap(),
                                        endCap = RoundCap()
                                    )
                                }
                            }

                            // Marcador do Cluster
                            val clusterHue = when {
                                hasGemini -> BitmapDescriptorFactory.HUE_GREEN
                                isMulti -> BitmapDescriptorFactory.HUE_CYAN
                                cluster.items.first().appName.equals("iFood", ignoreCase = true) -> BitmapDescriptorFactory.HUE_RED
                                cluster.items.first().appName.equals("Rappi", ignoreCase = true) -> BitmapDescriptorFactory.HUE_ORANGE
                                else -> BitmapDescriptorFactory.HUE_YELLOW
                            }

                            Marker(
                                state = rememberMarkerState(position = cluster.centerLatLng),
                                title = if (isMulti) "Constelação (${cluster.items.size} pedidos)" else "${cluster.items.first().appName} R$ ${String.format("%.2f", cluster.totalEarnings)}",
                                snippet = "Média: R$ ${String.format("%.2f", cluster.averageGainPerKm)}/km • Apps: ${cluster.appsInvolved.joinToString("+")}",
                                icon = BitmapDescriptorFactory.defaultMarker(clusterHue),
                                onClick = {
                                    selectedCluster = cluster
                                    selectedItem = cluster.items.firstOrNull()
                                    onClusterSelected?.invoke(cluster)
                                    true
                                }
                            )
                        }
                    }

                    // Card Flutuante de Detalhes da Constelação/Cluster Selecionado
                    selectedCluster?.let { cluster ->
                        ClusterDetailBadge(
                            cluster = cluster,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(10.dp),
                            onAccept = {
                                selectedItem?.let { item ->
                                    val orig = offers.find { it.appName.equals(item.appName, ignoreCase = true) }
                                    if (orig != null) onOfferSelected?.invoke(orig)
                                }
                            },
                            onClose = {
                                selectedCluster = null
                                selectedItem = null
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ClusterDetailBadge(
    cluster: OfferClusterGroup,
    modifier: Modifier = Modifier,
    onAccept: () -> Unit,
    onClose: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF00FF88), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xF20D1117)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = if (cluster.items.size > 1) "🌌 Constelação Multi-App" else "📍 Oferta Regional",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00FF88)
                    )
                    Text(
                        text = "(${cluster.appsInvolved.joinToString("+")})",
                        fontSize = 10.sp,
                        color = Color.LightGray
                    )
                }

                IconButton(onClick = onClose, modifier = Modifier.size(20.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color.Gray, modifier = Modifier.size(14.dp))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "R$ ${String.format("%.2f", cluster.totalEarnings)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = "Média: R$ ${String.format("%.2f", cluster.averageGainPerKm)}/km",
                        fontSize = 10.sp,
                        color = Color(0xFF00F0FF)
                    )
                }

                Button(
                    onClick = onAccept,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF88)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("Aceitar", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun calculateDistanceMeters(p1: LatLng, p2: LatLng): Double {
    val r = 6371000.0 // Raio da Terra em metros
    val dLat = Math.toRadians(p2.latitude - p1.latitude)
    val dLng = Math.toRadians(p2.longitude - p1.longitude)
    val a = sin(dLat / 2).pow(2.0) + cos(Math.toRadians(p1.latitude)) * cos(Math.toRadians(p2.latitude)) * sin(dLng / 2).pow(2.0)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return r * c
}

private const val CYBER_NEURAL_MAP_STYLE = """
[
  { "elementType": "geometry", "stylers": [{ "color": "#0d1117" }] },
  { "elementType": "labels.text.stroke", "stylers": [{ "color": "#0d1117" }] },
  { "elementType": "labels.text.fill", "stylers": [{ "color": "#8b949e" }] },
  { "featureType": "road", "elementType": "geometry", "stylers": [{ "color": "#21262d" }] },
  { "featureType": "road.highway", "elementType": "geometry", "stylers": [{ "color": "#30363d" }] },
  { "featureType": "road.highway", "elementType": "geometry.stroke", "stylers": [{ "color": "#00ff88" }, { "weight": 0.2 }] },
  { "featureType": "water", "elementType": "geometry", "stylers": [{ "color": "#030d1a" }] }
]
"""
