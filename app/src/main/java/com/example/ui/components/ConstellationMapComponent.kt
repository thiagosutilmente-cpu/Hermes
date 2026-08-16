package com.example.ui.components

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.coordinator.RadarCoordinator
import com.example.coordinator.RadarSettings
import com.example.data.OfferEntity
import com.example.viewmodel.BatchRecommendation
import com.example.viewmodel.GhostBatchGroup
import com.example.viewmodel.GhostSequenceBatchUiState
import com.example.viewmodel.GhostSequenceBatchViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.sin

/**
 * Modos de Tema do Constellation Map (Dark Mode / Auto Sensor / Solar)
 */
enum class MapThemeMode {
    AUTO_AMBIENT, // Alterna automaticamente baseado em sensor de luz ou horário (18h-06h Dark)
    DARK_NEURAL,  // Tons escuros profundos (#0D1117) - Anti-fadiga visual
    SOLAR_LIGHT,  // Alto contraste para luz solar intensa
    NIGHT_VISION  // Modo Vermelho/Âmbar ultra-escuro para pilotagem noturna
}

/**
 * Cores temáticas para constelação de entrega e apps parceiros
 */
private val GhostCyan = Color(0xFF00F0FF)
private val GhostGreen = Color(0xFF00FF88)
private val GhostGold = Color(0xFFFFB800)
private val GhostRed = Color(0xFFEA1D2C)
private val GhostPurple = Color(0xFF9D00FF)
private val GhostDarkBg = Color(0xFF0D1117)

/**
 * Modelo interno de nó estelar (Constellation Node)
 */
data class ConstellationNode(
    val id: String,
    val title: String,
    val subtitle: String,
    val latLng: LatLng,
    val type: NodeType,
    val appName: String,
    val fareValue: Double,
    val waitTimeMin: Int,
    val clusterId: String,
    val associatedBatch: GhostBatchGroup?
) {
    enum class NodeType {
        RIDER, PICKUP, DELIVERY, RESTAURANT_CLUSTER
    }
}

/**
 * Componente Visual: Constellation Map
 * Exibe clusters geográficos de ofertas em tempo real com conexões neurais
 * e rotas super-encadeadas vindas do GhostSequenceBatchViewModel.
 * Inclui Seletor de Tema Automático (Dark Mode / Sensor de Luz / Horário do Dia).
 */
@Composable
fun ConstellationMapComponent(
    viewModel: GhostSequenceBatchViewModel,
    modifier: Modifier = Modifier,
    onBatchSelected: ((GhostBatchGroup) -> Unit)? = null
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val speedKmh by RadarCoordinator.currentSpeedKmh.collectAsStateWithLifecycle()
    val radarSettings by RadarCoordinator.settings.collectAsStateWithLifecycle()
    val isSpeedLocked = speedKmh >= 40.0f || (radarSettings.speedLimitKmh > 0 && speedKmh >= radarSettings.speedLimitKmh)

    val coroutineScope = rememberCoroutineScope()

    // Configuração de Tema do Mapa
    var themeMode by remember { mutableStateOf(MapThemeMode.AUTO_AMBIENT) }
    var ambientLux by remember { mutableStateOf(50f) } // Lux padrão
    var isNightByTime by remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        mutableStateOf(hour >= 18 || hour < 6)
    }

    // Monitoramento do Sensor de Luz Ambiente (Lux)
    DisposableEffect(themeMode) {
        if (themeMode == MapThemeMode.AUTO_AMBIENT) {
            val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            val lightSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_LIGHT)

            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent?) {
                    event?.values?.firstOrNull()?.let { lux ->
                        ambientLux = lux
                    }
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }

            if (lightSensor != null && sensorManager != null) {
                sensorManager.registerListener(listener, lightSensor, SensorManager.SENSOR_DELAY_UI)
            }

            onDispose {
                sensorManager?.unregisterListener(listener)
            }
        } else {
            onDispose { }
        }
    }

    // Determina se o modo escuro anti-fadiga está ativo
    val isEffectiveDarkMode = remember(themeMode, ambientLux, isNightByTime) {
        when (themeMode) {
            MapThemeMode.DARK_NEURAL, MapThemeMode.NIGHT_VISION -> true
            MapThemeMode.SOLAR_LIGHT -> false
            MapThemeMode.AUTO_AMBIENT -> {
                // Se luz ambiente < 100 lux ou for entre 18h e 06h => ativa tons escuros anti-fadiga
                ambientLux < 100f || isNightByTime
            }
        }
    }

    var isSatellite by remember { mutableStateOf(false) }
    var selectedNode by remember { mutableStateOf<ConstellationNode?>(null) }
    var showOnlySuperChains by remember { mutableStateOf(false) }

    // Centro do mapa baseado na posição do piloto
    val riderLatLng = LatLng(uiState.currentRiderLat, uiState.currentRiderLng)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(riderLatLng, 14.2f)
    }

    // Atualiza a câmera suavemente quando a localização mudar significativamente
    LaunchedEffect(uiState.currentRiderLat, uiState.currentRiderLng) {
        cameraPositionState.animate(
            CameraUpdateFactory.newLatLng(riderLatLng),
            durationMs = 800
        )
    }

    // Constrói os nós estelares e clusters a partir dos lotes do ViewModel
    val constellationNodes = remember(uiState.activeBatches, uiState.candidateOffers, riderLatLng) {
        buildConstellationNodes(uiState, riderLatLng)
    }

    // Configurações visuais do mapa baseadas no Dark Mode / Anti-Fadiga
    val mapProperties by remember(isSatellite, isEffectiveDarkMode, themeMode) {
        derivedStateOf {
            val styleJson = if (isSatellite) {
                null
            } else if (themeMode == MapThemeMode.NIGHT_VISION) {
                NIGHT_VISION_MAP_STYLE
            } else if (isEffectiveDarkMode) {
                CYBER_DARK_MAP_STYLE
            } else {
                null // Tema padrão/solar
            }

            MapProperties(
                mapType = if (isSatellite) MapType.HYBRID else MapType.NORMAL,
                isMyLocationEnabled = false,
                isTrafficEnabled = true,
                mapStyleOptions = styleJson?.let { MapStyleOptions(it) }
            )
        }
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

    val containerBg = if (isEffectiveDarkMode) GhostDarkBg else Color(0xFF1E242C)
    val borderCyanColor = if (themeMode == MapThemeMode.NIGHT_VISION) Color(0xFFFF441F) else GhostCyan

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, borderCyanColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .testTag("constellation_map_container"),
        colors = CardDefaults.cardColors(containerColor = containerBg)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header do Mapa Constellation com Seletor de Tema
            ConstellationMapHeader(
                activeBatchCount = uiState.activeBatches.size,
                isSatellite = isSatellite,
                showOnlySuperChains = showOnlySuperChains,
                themeMode = themeMode,
                isEffectiveDarkMode = isEffectiveDarkMode,
                ambientLux = ambientLux,
                onCycleTheme = {
                    themeMode = when (themeMode) {
                        MapThemeMode.AUTO_AMBIENT -> MapThemeMode.DARK_NEURAL
                        MapThemeMode.DARK_NEURAL -> MapThemeMode.NIGHT_VISION
                        MapThemeMode.NIGHT_VISION -> MapThemeMode.SOLAR_LIGHT
                        MapThemeMode.SOLAR_LIGHT -> MapThemeMode.AUTO_AMBIENT
                    }
                },
                onToggleSatellite = { isSatellite = !isSatellite },
                onToggleSuperChains = { showOnlySuperChains = !showOnlySuperChains },
                onRecenter = {
                    coroutineScope.launch {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newCameraPosition(
                                CameraPosition.fromLatLngZoom(riderLatLng, 14.5f)
                            ),
                            1000
                        )
                    }
                }
            )

            val visibleBatches = if (showOnlySuperChains) {
                uiState.activeBatches.filter { it.recommendation == BatchRecommendation.SUPER_CHAIN_ACCEPT }
            } else {
                uiState.activeBatches
            }

            // Animação suave de pulsação não-distrativa para nós de ofertas processadas por IA
            val infiniteTransition = rememberInfiniteTransition(label = "constellationMarkerPulse")
            val markerPulseRadiusFactor by infiniteTransition.animateFloat(
                initialValue = 70.0f,
                targetValue = 130.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1600, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "markerPulseRadiusFactor"
            )
            val markerPulseAlpha by infiniteTransition.animateFloat(
                initialValue = 0.50f,
                targetValue = 0.12f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1600, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "markerPulseAlpha"
            )

            // Viewport do Google Maps
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = mapProperties,
                    uiSettings = mapUiSettings,
                    onMapClick = { selectedNode = null }
                ) {
                    // 1. Marcador do Piloto (Moto)
                    Marker(
                        state = rememberMarkerState(position = riderLatLng),
                        title = "Você (Piloto)",
                        snippet = "Central Neural Radar Ativa",
                        icon = BitmapDescriptorFactory.defaultMarker(
                            if (themeMode == MapThemeMode.NIGHT_VISION) BitmapDescriptorFactory.HUE_RED else BitmapDescriptorFactory.HUE_CYAN
                        ),
                        zIndex = 10f
                    )

                    // Círculo de cobertura/radar do piloto (pulso de 1.2km)
                    Circle(
                        center = riderLatLng,
                        radius = 1200.0,
                        strokeColor = if (themeMode == MapThemeMode.NIGHT_VISION) Color(0x78FF441F) else Color(0x7800F0FF),
                        strokeWidth = 3f,
                        fillColor = if (themeMode == MapThemeMode.NIGHT_VISION) Color(0x14FF441F) else Color(0x1400F0FF)
                    )

                    // 2. Renderiza Polylines de Conexão Constelar (Rotas do Ghost Sequence)
                    visibleBatches.forEachIndexed { _, batch ->
                        val isSuper = batch.recommendation == BatchRecommendation.SUPER_CHAIN_ACCEPT
                        val lineColor = if (themeMode == MapThemeMode.NIGHT_VISION) {
                            if (isSuper) Color(0xFFFF5533) else Color(0xFFFF8844)
                        } else {
                            if (isSuper) Color(0xE600FF88) else Color(0xB400F0FF)
                        }

                        // Constrói caminho geométrico da super-rota encadeada
                        val points = mutableListOf<LatLng>()
                        points.add(riderLatLng)

                        val batchNodes = constellationNodes.filter { it.associatedBatch?.batchId == batch.batchId }
                        batchNodes.forEach { node ->
                            points.add(node.latLng)
                        }

                        if (points.size >= 2) {
                            Polyline(
                                points = points,
                                color = lineColor,
                                width = if (isSuper) 8f else 5f,
                                jointType = JointType.ROUND,
                                startCap = RoundCap(),
                                endCap = RoundCap()
                            )
                        }
                    }

                    // 3. Renderiza Marcadores Estelares (Nós e Clusters)
                    constellationNodes.forEach { node ->
                        val hasActiveBatch = node.associatedBatch != null
                        val isSuperChain = node.associatedBatch?.recommendation == BatchRecommendation.SUPER_CHAIN_ACCEPT

                        // Animação suave de pulsação ao redor do marcador de oferta processada por IA
                        if (hasActiveBatch) {
                            val pulseColor = if (isSuperChain) Color(0xFF00FF88) else Color(0xFF00F0FF)
                            Circle(
                                center = node.latLng,
                                radius = markerPulseRadiusFactor.toDouble(),
                                strokeColor = pulseColor.copy(alpha = markerPulseAlpha * 1.5f),
                                strokeWidth = if (isSuperChain) 3f else 1.8f,
                                fillColor = pulseColor.copy(alpha = markerPulseAlpha * 0.35f)
                            )
                        }

                        val hue = when (node.type) {
                            ConstellationNode.NodeType.PICKUP -> BitmapDescriptorFactory.HUE_ORANGE
                            ConstellationNode.NodeType.DELIVERY -> BitmapDescriptorFactory.HUE_GREEN
                            ConstellationNode.NodeType.RESTAURANT_CLUSTER -> BitmapDescriptorFactory.HUE_VIOLET
                            ConstellationNode.NodeType.RIDER -> BitmapDescriptorFactory.HUE_AZURE
                        }

                        Marker(
                            state = rememberMarkerState(position = node.latLng),
                            title = "${node.title} (${node.appName})",
                            snippet = "R$ ${String.format("%.2f", node.fareValue)} • Espera: ${node.waitTimeMin}m",
                            icon = BitmapDescriptorFactory.defaultMarker(hue),
                            onClick = {
                                selectedNode = node
                                true
                            }
                        )
                    }
                }

                // Overlay de Telemetria Flutuante (Ghost Hub)
                GhostTelemetryOverlay(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                    activeBatches = visibleBatches,
                    aggressiveness = uiState.aggressiveness
                )

                // Modal/Card Flutuante de Detalhes do Nó Selecionado
                selectedNode?.let { node ->
                    NodeDetailPopup(
                        node = node,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(12.dp),
                        onAccept = {
                            node.associatedBatch?.let { batch ->
                                viewModel.acceptBatch(batch)
                                onBatchSelected?.invoke(batch)
                                selectedNode = null
                            }
                        },
                        onClose = { selectedNode = null }
                    )
                }

                // OVERLAY DE SEGURANÇA ATIVA (> 40 km/h)
                if (isSpeedLocked) {
                    SpeedSafetyOverlay(
                        currentSpeedKmh = speedKmh,
                        speedLimitKmh = 40.0f,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Barra inferior com métricas de convergência
            ConstellationBottomBar(
                batchCount = uiState.activeBatches.size,
                minGainPerKm = uiState.minGainPerKm,
                earningsToday = uiState.totalChainedEarningsToday,
                themeMode = themeMode,
                isDarkMode = isEffectiveDarkMode
            )
        }
    }
}

/**
 * Top Header com controles rápidos de visualização e Seletor de Tema Automático
 */
@Composable
private fun ConstellationMapHeader(
    activeBatchCount: Int,
    isSatellite: Boolean,
    showOnlySuperChains: Boolean,
    themeMode: MapThemeMode,
    isEffectiveDarkMode: Boolean,
    ambientLux: Float,
    onCycleTheme: () -> Unit,
    onToggleSatellite: () -> Unit,
    onToggleSuperChains: () -> Unit,
    onRecenter: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF131922))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("🌌", fontSize = 14.sp)
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "CONSTELLATION MAP",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        color = GhostCyan,
                        letterSpacing = 1.sp
                    )
                    // Badge indicadora de Anti-Fadiga / Modo Noturno
                    val (themeIcon, themeLabel, badgeBg, badgeColor) = when (themeMode) {
                        MapThemeMode.AUTO_AMBIENT -> Quad("⚡ AUTO", if (isEffectiveDarkMode) "Dark (Noite/Sensor)" else "Solar", Color(0x3300FF88), GhostGreen)
                        MapThemeMode.DARK_NEURAL -> Quad("🌙 DARK", "Anti-Fadiga", Color(0x3300F0FF), GhostCyan)
                        MapThemeMode.NIGHT_VISION -> Quad("🔴 NIGHT", "Visão Noturna", Color(0x33FF3366), Color(0xFFFF3366))
                        MapThemeMode.SOLAR_LIGHT -> Quad("☀️ SOLAR", "Sol Forte", Color(0x33FFB800), GhostGold)
                    }
                    Surface(
                        color = badgeBg,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(start = 2.dp)
                    ) {
                        Text(
                            text = "$themeIcon",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
                Text(
                    text = "$activeBatchCount lotes neurais rastreados",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 10.sp,
                    color = Color.LightGray
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            // Botão Seletor de Tema Anti-Fadiga
            IconButton(
                onClick = onCycleTheme,
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        when (themeMode) {
                            MapThemeMode.AUTO_AMBIENT -> Color(0x2200FF88)
                            MapThemeMode.DARK_NEURAL -> Color(0x2200F0FF)
                            MapThemeMode.NIGHT_VISION -> Color(0x22FF3366)
                            MapThemeMode.SOLAR_LIGHT -> Color(0x22FFB800)
                        },
                        CircleShape
                    )
            ) {
                Text(
                    text = when (themeMode) {
                        MapThemeMode.AUTO_AMBIENT -> "🌓"
                        MapThemeMode.DARK_NEURAL -> "🌙"
                        MapThemeMode.NIGHT_VISION -> "🔴"
                        MapThemeMode.SOLAR_LIGHT -> "☀️"
                    },
                    fontSize = 12.sp
                )
            }

            IconButton(
                onClick = onToggleSuperChains,
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        if (showOnlySuperChains) GhostGreen.copy(alpha = 0.2f) else Color.Transparent,
                        CircleShape
                    )
            ) {
                Text(if (showOnlySuperChains) "⚡" else "✨", fontSize = 12.sp)
            }

            IconButton(
                onClick = onToggleSatellite,
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        if (isSatellite) GhostCyan.copy(alpha = 0.2f) else Color.Transparent,
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = "Alternar Modo de Mapa",
                    tint = if (isSatellite) GhostCyan else Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
            }

            IconButton(
                onClick = onRecenter,
                modifier = Modifier
                    .size(32.dp)
                    .background(Color(0xFF1F2937), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "Centralizar no Piloto",
                    tint = GhostGreen,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

/**
 * Overlay com dados de telemetria rápida no canto superior do mapa
 */
@Composable
private fun GhostTelemetryOverlay(
    modifier: Modifier = Modifier,
    activeBatches: List<GhostBatchGroup>,
    aggressiveness: String
) {
    val bestBatch = activeBatches.maxByOrNull { it.gainPerKm }

    Column(
        modifier = modifier
            .background(Color(0xCC0D1117), RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFF00F0FF).copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(GhostGreen, CircleShape)
            )
            Text(
                text = "GHOST RADAR: $aggressiveness",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = GhostGreen
            )
        }

        bestBatch?.let { batch ->
            Text(
                text = "Pico: R$ ${String.format("%.2f", batch.gainPerKm)}/km (${batch.appNames.joinToString("+")})",
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
        }
    }
}

/**
 * Card flutuante que detalha o nó ou cluster clicado
 */
@Composable
private fun NodeDetailPopup(
    node: ConstellationNode,
    modifier: Modifier = Modifier,
    onAccept: () -> Unit,
    onClose: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, GhostCyan, RoundedCornerShape(12.dp)),
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
                        text = when (node.appName.lowercase()) {
                            "ifood" -> "🔴 iFood"
                            "rappi" -> "🟠 Rappi"
                            "uber" -> "⚫ Uber"
                            else -> "🟡 99"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "• ${node.title}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GhostCyan,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = onClose, modifier = Modifier.size(20.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color.Gray, modifier = Modifier.size(14.dp))
                }
            }

            Text(
                text = node.subtitle,
                fontSize = 10.sp,
                color = Color.LightGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "R$ ${String.format("%.2f", node.fareValue)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = GhostGreen
                    )
                    Text(
                        text = "⏱️ Cozinha: ${node.waitTimeMin}m",
                        fontSize = 11.sp,
                        color = GhostGold
                    )
                }

                if (node.associatedBatch != null) {
                    Button(
                        onClick = onAccept,
                        colors = ButtonDefaults.buttonColors(containerColor = GhostGreen),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text(
                            text = "Aceitar Lote",
                            color = Color.Black,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Overlay de bloqueio visual e tátil por Segurança Ativa.
 * Interrompe qualquer toque acidental na tela quando a velocidade do GPS for superior a 40 km/h.
 */
@Composable
fun SpeedSafetyOverlay(
    currentSpeedKmh: Float,
    speedLimitKmh: Float = 40.0f,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "safety_pulse")
    val pulseBorderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_border"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            // Consome e bloqueia todos os eventos de toque na tela para proteção do piloto
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        event.changes.forEach { it.consume() }
                    }
                }
            }
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xF00D1117),
                        Color(0xF8000000),
                        Color(0xFF000000)
                    )
                )
            )
            .border(
                width = 3.dp,
                color = Color(0xFFFF2A4D).copy(alpha = pulseBorderAlpha),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
            .testTag("speed_safety_overlay"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            // Ícone Pulsante de Alerta de Segurança
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(Color(0xFFFF2A4D).copy(alpha = 0.18f), CircleShape)
                    .border(2.dp, Color(0xFFFF2A4D).copy(alpha = pulseBorderAlpha), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🛡️",
                    fontSize = 28.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Título de Segurança Ativa
            Text(
                text = "SEGURANÇA ATIVA",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = Color(0xFFFF2A4D),
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Mãos no guidão • Interação bloqueada",
                style = MaterialTheme.typography.bodySmall,
                color = Color.LightGray,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Velocímetro HUD em Destaque
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                border = BorderStroke(1.dp, Color(0xFF30363D)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${currentSpeedKmh.toInt()}",
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFFB800)
                        )
                        Text(
                            text = "KM/H ATUAL",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(32.dp)
                            .background(Color(0xFF30363D))
                    )

                    Column(horizontalAlignment = Alignment.Start) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(6.dp).background(Color(0xFFFF2A4D), CircleShape))
                            Text(
                                text = "LIMITE: ${speedLimitKmh.toInt()} KM/H",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Text(
                            text = "Comandos de voz Jarvis ativos",
                            fontSize = 10.sp,
                            color = Color(0xFF00FF88)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Dica de desbloqueio automático
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .background(Color(0x3300F0FF), RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text("🎙️", fontSize = 11.sp)
                Text(
                    text = "Fale \"Jarvis\" ou desacelere para liberar a tela",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF00F0FF)
                )
            }
        }
    }
}

/**
 * Rodapé com resumo financeiro, estatísticas e status do tema
 */
@Composable
private fun ConstellationBottomBar(
    batchCount: Int,
    minGainPerKm: Double,
    earningsToday: Double,
    themeMode: MapThemeMode = MapThemeMode.AUTO_AMBIENT,
    isDarkMode: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0A0E14))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Filtro Mín: R$ ${String.format("%.2f", minGainPerKm)}/km",
                fontSize = 10.sp,
                color = Color.Gray
            )
            Text(
                text = "• " + if (isDarkMode) "🌙 Anti-Fadiga Ativo" else "☀️ Modo Claro",
                fontSize = 9.sp,
                color = if (isDarkMode) GhostCyan else GhostGold
            )
        }
        Text(
            text = "Ganhos Encadeados Hoje: R$ ${String.format("%.2f", earningsToday)}",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = GhostGreen
        )
    }
}

/**
 * Função utilitária que gera os nós estelares com coordenadas realistas em torno do piloto
 */
private fun buildConstellationNodes(
    uiState: GhostSequenceBatchUiState,
    riderLatLng: LatLng
): List<ConstellationNode> {
    val nodes = mutableListOf<ConstellationNode>()

    // Nós de ofertas candidatas e lotes
    uiState.activeBatches.forEachIndexed { bIdx, batch ->
        batch.offers.forEachIndexed { oIdx, offer ->
            val angle = ((bIdx * 65 + oIdx * 45) % 360) * (Math.PI / 180.0)
            val distOffset = (0.006 * (bIdx + 1)) + (0.003 * (oIdx + 1))

            val pickupLat = riderLatLng.latitude + (sin(angle) * distOffset)
            val pickupLng = riderLatLng.longitude + (cos(angle) * distOffset)

            val deliveryLat = pickupLat + (sin(angle + 0.4) * distOffset * 0.8)
            val deliveryLng = pickupLng + (cos(angle + 0.4) * distOffset * 0.8)

            nodes.add(
                ConstellationNode(
                    id = "pickup_${offer.id}_$bIdx",
                    title = offer.pickupAddress.take(20),
                    subtitle = "Coleta • ${offer.appName}",
                    latLng = LatLng(pickupLat, pickupLng),
                    type = ConstellationNode.NodeType.PICKUP,
                    appName = offer.appName,
                    fareValue = offer.fareValue,
                    waitTimeMin = if (offer.appName == "iFood") 3 else 7,
                    clusterId = "cluster_$bIdx",
                    associatedBatch = batch
                )
            )

            nodes.add(
                ConstellationNode(
                    id = "delivery_${offer.id}_$bIdx",
                    title = offer.deliveryAddress.take(20),
                    subtitle = "Entrega • ${offer.appName}",
                    latLng = LatLng(deliveryLat, deliveryLng),
                    type = ConstellationNode.NodeType.DELIVERY,
                    appName = offer.appName,
                    fareValue = offer.fareValue,
                    waitTimeMin = 0,
                    clusterId = "cluster_$bIdx",
                    associatedBatch = batch
                )
            )
        }
    }

    return nodes
}

/**
 * Estilo JSON Dark Cyberpunk para Google Maps (Anti-Fadiga Visual)
 */
private const val CYBER_DARK_MAP_STYLE = """
[
  { "elementType": "geometry", "stylers": [{ "color": "#0d1117" }] },
  { "elementType": "labels.text.stroke", "stylers": [{ "color": "#0d1117" }] },
  { "elementType": "labels.text.fill", "stylers": [{ "color": "#8b949e" }] },
  { "featureType": "road", "elementType": "geometry", "stylers": [{ "color": "#21262d" }] },
  { "featureType": "road.highway", "elementType": "geometry", "stylers": [{ "color": "#30363d" }] },
  { "featureType": "road.highway", "elementType": "geometry.stroke", "stylers": [{ "color": "#00f0ff" }, { "weight": 0.2 }] },
  { "featureType": "water", "elementType": "geometry", "stylers": [{ "color": "#030d1a" }] }
]
"""

/**
 * Estilo JSON Night Vision Ultra-Dark (Tons Âmbar / Vermelho Escuro)
 */
private const val NIGHT_VISION_MAP_STYLE = """
[
  { "elementType": "geometry", "stylers": [{ "color": "#080608" }] },
  { "elementType": "labels.text.stroke", "stylers": [{ "color": "#080608" }] },
  { "elementType": "labels.text.fill", "stylers": [{ "color": "#c45a4a" }] },
  { "featureType": "road", "elementType": "geometry", "stylers": [{ "color": "#1f1418" }] },
  { "featureType": "road.highway", "elementType": "geometry", "stylers": [{ "color": "#2c1920" }] },
  { "featureType": "road.highway", "elementType": "geometry.stroke", "stylers": [{ "color": "#ff3366" }, { "weight": 0.4 }] },
  { "featureType": "water", "elementType": "geometry", "stylers": [{ "color": "#0a0408" }] }
]
"""
