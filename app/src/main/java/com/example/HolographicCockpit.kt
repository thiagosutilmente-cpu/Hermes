package com.example

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.coordinator.RadarCoordinator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import com.example.ui.components.NeuralSurgeRadarWidget
import com.example.ui.components.ChainedDetourWidget
import com.example.ui.components.CourierAppRealitiesWidget
import com.example.ui.components.CourierNetProfitWidget
import com.example.util.NeuralSurgePredictor
import com.example.util.MultiAppOrderManager

// Color Palette for Jarvis Neural Cockpit v2.4 (Cockpit namespace to avoid collisions)
val CockpitDarkBg = Color(0xFF0A0A0F)
val CockpitPanelBg = Color(0xFF111118)
val CockpitGreen = Color(0xFF00FF88)
val CockpitCyan = Color(0xFF00F0FF)
val CockpitIFood = Color(0xFFEA1D2C)
val CockpitRappi = Color(0xFFFF441F)
val Cockpit99 = Color(0xFFF7C200)
val CockpitUber = Color(0xFF222226)
val CockpitTextPrimary = Color(0xFFFFFFFF)
val CockpitTextSecondary = Color(0xFF8A8A9A)
val CockpitDangerRed = Color(0xFFFF3366)
val CockpitWarningAmber = Color(0xFFFFAA00)

@Composable
fun BottomNavItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, isActive: Boolean = false, onClick: () -> Unit = {}) {
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.1f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "navScale"
    )
    val alpha by animateFloatAsState(targetValue = if (isActive) 1.0f else 0.4f, label = "navAlpha")
    
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.scale(scale)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) CockpitGreen else Color.White.copy(alpha = alpha),
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                color = if (isActive) CockpitGreen else Color.White.copy(alpha = alpha),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
fun HolographicCockpit(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    currentTab: Int = 0,
    onTabChange: (Int) -> Unit = {},
    content: @Composable () -> Unit = {}
) {
    val context = LocalContext.current
    val systemHealth by RadarCoordinator.systemHealth.collectAsStateWithLifecycle()
    val batteryLevel by RadarCoordinator.batteryLevel.collectAsStateWithLifecycle()
    val isFirestoreConnected by RadarCoordinator.isFirestoreConnected.collectAsStateWithLifecycle()

    var previousConnectionState by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(isFirestoreConnected) {
        if (previousConnectionState != null && previousConnectionState != isFirestoreConnected) {
            if (isFirestoreConnected) {
                com.example.util.ToastUtils.showToast(context, "✅ Conexão Firestore reestabelecida!", Toast.LENGTH_SHORT)
            } else {
                com.example.util.ToastUtils.showToast(context, "⚠️ Conexão Firestore perdida! Modo offline ativado.", Toast.LENGTH_LONG)
            }
        }
        previousConnectionState = isFirestoreConnected
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CockpitDarkBg)
            .padding(top = 28.dp, bottom = 4.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // --- FEATURE 4: TOP STATUS BAR ---
            TopStatusBar(batteryLevel = batteryLevel, isFirestoreConnected = isFirestoreConnected)
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // --- MAIN BODY AREA ---
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (currentTab == 0) {
                    // MAIN COCKPIT DASHBOARD (Constellation Map + Stack Panel + Health Pulse)
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // FEATURE WIDGET TELEMETRIA, CONEXÃO, VELOCIDADE & TRAVA DE SEGURANÇA
                        item {
                            com.example.ui.components.TelemetrySafetyStatusWidget(
                                onToggleSafetyLock = {
                                    val currentSettings = RadarCoordinator.settings.value
                                    val newFocusMode = !currentSettings.focusModeAuto
                                    RadarCoordinator.updateSettings(currentSettings.copy(focusModeAuto = newFocusMode))
                                    val msg = if (newFocusMode) "🛡️ Trava de Segurança em Movimento ATIVADA" else "⚠️ Trava de Segurança DESATIVADA (Modo Livre)"
                                    com.example.util.ToastUtils.showToast(context, msg, Toast.LENGTH_SHORT)
                                    viewModel.updateJarvisResponse(msg)
                                }
                            )
                        }

                        // GRÁFICO RECHARTS / D3 DE GANHOS POR HORA (ÚLTIMAS 8 HORAS)
                        item {
                            com.example.ui.components.RechartsHourlyEarningsCard()
                        }

                        // PAINEL TÁTICO DA GHOST SEQUENCE (BATCHING & REDUÇÃO DE TEMPO DE ESPERA)
                        item {
                            com.example.ui.components.TacticalGhostSequenceOverviewCard(
                                onActivateGhostBatch = { batch ->
                                    com.example.manager.MergedDeliveryDispatcher.startGhostBatchRoute(context, batch)
                                    val msg = "👻 LOTE GHOST ACEITO: ${batch.appNamesFormatted} (R$ ${String.format(java.util.Locale.US, "%.2f", batch.totalEarnings)} - Economia de ${batch.waitTimeSavedMinutes} min)"
                                    com.example.util.ToastUtils.showToast(context, msg, Toast.LENGTH_LONG)
                                    viewModel.updateJarvisResponse(msg)
                                }
                            )
                        }

                        // GUIA OPERACIONAL PASSO A PASSO DA ROTA MESCLADA (CENTRAL TÁTICA DE ENTREGAS)
                        item {
                            com.example.ui.components.MergedRouteTacticalCockpitCard()
                        }

                        // LISTA DE SEQUENCIAMENTO DE ENTREGAS (COLETA 🛍️ VS ENTREGA 🏠)
                        item {
                            com.example.ui.components.SimplifiedDeliverySequenceListCard(
                                onStopSelected = { stop ->
                                    val action = if (stop.actionType == com.example.model.StopActionType.PICKUP) "Coleta" else "Entrega"
                                    val msg = "📍 $action selecionada: ${stop.establishmentOrCustomer} (${stop.appName})"
                                    com.example.util.ToastUtils.showToast(context, msg, Toast.LENGTH_SHORT)
                                    viewModel.updateJarvisResponse(msg)
                                }
                            )
                        }

                        // FEATURE 1 & 2: CONSTELLATION MAP & GHOST OVERLAY
                        item {
                            ConstellationMapCard(
                                onNodeClick = { msg ->
                                    com.example.util.ToastUtils.showToast(context, msg, Toast.LENGTH_SHORT)
                                    viewModel.updateJarvisResponse(msg)
                                }
                            )
                        }

                        // FEATURE NEURAL SURGE RADAR (PONTOS CHAVE E DENSIDADE DE PICOS)
                        item {
                            NeuralSurgeRadarWidget(
                                onNavigateToHotspot = { spot ->
                                    com.example.util.ToastUtils.showToast(context, "🎯 NAVEGANDO PARA CLUSTER: ${spot.name}", Toast.LENGTH_LONG)
                                    viewModel.updateJarvisResponse("Navegando para o ponto estratégico de pico (${spot.name}). Projeção: R$ ${spot.estimatedFarePerKm}/km.")
                                }
                            )
                        }

                        // FEATURE OTIMIZADOR DE DESVIOS (ENTREGA B E ENCADEADAS)
                        item {
                            ChainedDetourWidget(
                                onAttachDetourOrder = { result ->
                                    com.example.util.ToastUtils.showToast(context, "✅ DESVIO B ANEXADO (+R$ ${result.secondaryValue} | +${result.extraDetourKm}km)", Toast.LENGTH_LONG)
                                    viewModel.updateJarvisResponse("Desvio da entrega B (${result.secondaryOffer.appName}) anexado com sucesso. Ganho no desvio: R$ ${result.detourYieldPerKm}/km.")
                                }
                            )
                        }

                        // FEATURE REALIDADE PRÁTICA DOS APPS NO BRASIL (iFood Score, Rappi Auto-Aceite, 99 Bônus)
                        item {
                            CourierAppRealitiesWidget()
                        }

                        // FEATURE CALCULO DE LUCRO LÍQUIDO REAL & CUSTO MOTO + ALERTA DE VOZ BLUETOOTH
                        item {
                            CourierNetProfitWidget()
                        }

                        // FEATURE 3: STACK PANEL (BENTO GRID)
                        item {
                            StackPanelBentoGrid(
                                onAccept = { title ->
                                    com.example.util.ToastUtils.showToast(context, "✅ $title ACEITO! Rota sincronizada.", Toast.LENGTH_LONG)
                                    viewModel.updateJarvisResponse("Stack aceito. Rota otimizada enviada ao GPS.")
                                },
                                onDecline = { title ->
                                    com.example.util.ToastUtils.showToast(context, "❌ $title recusado.", Toast.LENGTH_SHORT)
                                }
                            )
                        }

                        // FEATURE 5: BOTTOM HEALTH PULSE BAR
                        item {
                            BottomHealthPulseBar(
                                systemHealth = systemHealth,
                                onVoiceClick = {
                                    viewModel.startJarvisSession()
                                    com.example.util.ToastUtils.showToast(context, "🎙️ Jarvis em escuta...", Toast.LENGTH_SHORT)
                                },
                                onStartRouteClick = {
                                    com.example.util.ToastUtils.showToast(context, "🚀 NAVEGAÇÃO OTIMIZADA INICIADA!", Toast.LENGTH_LONG)
                                    viewModel.updateJarvisResponse("Iniciando navegação de rota otimizada multi-app.")
                                }
                            )
                        }
                    }
                } else {
                    // OTHER TABS (Simulador, Análises, Config)
                    content()
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))

            // --- BOTTOM NAVIGATION BAR ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .border(1.dp, Color.White.copy(alpha = 0.1f))
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomNavItem(icon = Icons.Default.Home, label = "INÍCIO", isActive = currentTab == 0, onClick = { onTabChange(0) })
                BottomNavItem(icon = Icons.Default.AutoAwesome, label = "SIMULADOR", isActive = currentTab == 1, onClick = { onTabChange(1) })
                BottomNavItem(icon = Icons.Default.PieChart, label = "ANÁLISES", isActive = currentTab == 2, onClick = { onTabChange(2) })
                BottomNavItem(icon = Icons.Default.Settings, label = "CONFIG.", isActive = currentTab == 3, onClick = { onTabChange(3) })
                
                // --- FIRESTORE CONNECTION STATUS INDICATOR ---
                FirestoreConnectionIndicator(
                    isConnected = isFirestoreConnected,
                    onClick = {
                        val newStatus = !isFirestoreConnected
                        com.example.data.FirestoreManager.updateConnectionStatus(newStatus)
                    }
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// FIRESTORE CONNECTION INDICATOR COMPOSABLE FOR BOTTOM BAR
// -----------------------------------------------------------------------------
@Composable
fun FirestoreConnectionIndicator(
    isConnected: Boolean,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "firestorePulse")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotAlpha"
    )

    val statusColor = if (isConnected) CockpitGreen else CockpitDangerRed
    val statusText = if (isConnected) "ONLINE" else "OFFLINE"
    val statusBg = if (isConnected) CockpitGreen.copy(alpha = 0.15f) else CockpitDangerRed.copy(alpha = 0.2f)
    val statusBorder = if (isConnected) CockpitGreen.copy(alpha = 0.4f) else CockpitDangerRed.copy(alpha = 0.6f)

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(statusBg)
            .border(1.dp, statusBorder, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = if (isConnected) 1f else dotAlpha))
            )
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = "FIRESTORE",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = statusText,
                    color = statusColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// TOP STATUS BAR COMPOSABLE
// -----------------------------------------------------------------------------
@Composable
fun TopStatusBar(batteryLevel: Int, isFirestoreConnected: Boolean = true) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulseDot")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(animation = tween(1000, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "dotAlpha"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = CockpitPanelBg.copy(alpha = 0.9f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Brand Title & Badge
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "🎯 RADAR COORDINATOR",
                        color = CockpitTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                    Box(
                        modifier = Modifier
                            .background(
                                brush = Brush.horizontalGradient(listOf(CockpitGreen, CockpitCyan)),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "JARVIS v2.4",
                            color = Color.Black,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
                
                // Status Pills
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    StatusPillItem(dotColor = CockpitGreen, alpha = dotAlpha, text = "GPS 4.2m")
                    StatusPillItem(
                        dotColor = if (isFirestoreConnected) CockpitGreen else CockpitDangerRed,
                        alpha = dotAlpha,
                        text = if (isFirestoreConnected) "Sync OK" else "Sync Off"
                    )
                    StatusPillItem(dotColor = Cockpit99, alpha = 1f, text = "4 Apps")
                }
            }

            // Live Earnings Badge
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Ganho Hoje",
                    color = CockpitTextSecondary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "R$ 284,50",
                    color = CockpitGreen,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "▲ 12% vs ontem",
                    color = CockpitGreen,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun StatusPillItem(dotColor: Color, alpha: Float, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(dotColor.copy(alpha = alpha))
        )
        Text(
            text = text,
            color = CockpitTextPrimary,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private data class DynamicCityZone(
    val id: String,
    val name: String,
    val relX: Float,
    val relY: Float,
    val baseOrdersPerHour: Int,
    val surgeMultiplier: Double,
    val dominantApps: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val emoji: String,
    val tag: String
)

// -----------------------------------------------------------------------------
// FEATURE 1 & 2: CONSTELLATION MAP CARD & GHOST OVERLAY
// -----------------------------------------------------------------------------
@Composable
fun ConstellationMapCard(onNodeClick: (String) -> Unit) {
    val context = LocalContext.current
    val settings by RadarCoordinator.settings.collectAsStateWithLifecycle()
    val currentLocation by RadarCoordinator.currentLocation.collectAsStateWithLifecycle()
    val pendingOffers by MultiAppOrderManager.pendingOffers.collectAsStateWithLifecycle()
    val latestGeminiEval by RadarCoordinator.latestGeminiEvaluation.collectAsStateWithLifecycle()

    val hasGeminiNewRoute = latestGeminiEval != null || pendingOffers.isNotEmpty()

    val showTraffic = settings.showTrafficDensity || settings.showTrafficOverlay
    var showDemandHeatmap by remember { mutableStateOf(true) }
    var selectedFilter by remember { mutableStateOf("TODAS") }
    var activeZoneTelemetry by remember { mutableStateOf<DynamicCityZone?>(null) }

    val rainMultiplier = settings.rainModeMultiplier

    val infiniteTransition = rememberInfiniteTransition(label = "routeFlow")
    val dashOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 40f,
        animationSpec = infiniteRepeatable(animation = tween(2000, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "dashOffset"
    )
    val trafficPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(animation = tween(1500, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "trafficPulseAlpha"
    )
    val heatmapPulseRadius by infiniteTransition.animateFloat(
        initialValue = 70f,
        targetValue = 115f,
        animationSpec = infiniteRepeatable(animation = tween(1800, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "heatmapPulseRadius"
    )

    // Dynamic City Zones calculations shifting based on real-time order density
    val cityZones = remember(pendingOffers, rainMultiplier, settings.showTrafficDensity, selectedFilter) {
        val iFoodCount = pendingOffers.count { it.appName.equals("iFood", ignoreCase = true) }
        val rappiCount = pendingOffers.count { it.appName.equals("Rappi", ignoreCase = true) }
        val uberCount = pendingOffers.count { it.appName.contains("Uber", ignoreCase = true) }

        val all = listOf(
            DynamicCityZone(
                id = "z_paulista",
                name = "Paulista / Bela Cintra",
                relX = 0.38f,
                relY = 0.32f,
                baseOrdersPerHour = 48 + (iFoodCount * 8),
                surgeMultiplier = Math.round((1.45 * (if (rainMultiplier > 1.0) 1.25 else 1.0)) * 100.0) / 100.0,
                dominantApps = "iFood 65% + Rappi 25%",
                primaryColor = Color(0xFFFF0055),
                secondaryColor = Color(0xFFFF3300),
                emoji = "🔥",
                tag = "CLUSTER CRÍTICO"
            ),
            DynamicCityZone(
                id = "z_pinheiros",
                name = "Pinheiros / Faria Lima",
                relX = 0.65f,
                relY = 0.28f,
                baseOrdersPerHour = 36 + (rappiCount * 6 + uberCount * 4),
                surgeMultiplier = Math.round((1.30 * (if (rainMultiplier > 1.0) 1.15 else 1.0)) * 100.0) / 100.0,
                dominantApps = "Rappi 45% + Uber 35%",
                primaryColor = Color(0xFFFF8800),
                secondaryColor = Color(0xFFFFAA00),
                emoji = "⚡",
                tag = "ALTA DEMANDA"
            ),
            DynamicCityZone(
                id = "z_moema",
                name = "Moema / Ibirapuera",
                relX = 0.80f,
                relY = 0.60f,
                baseOrdersPerHour = 28,
                surgeMultiplier = 1.18,
                dominantApps = "99Moto 40% + iFood 35%",
                primaryColor = Color(0xFF00E5FF),
                secondaryColor = Color(0xFF0088FF),
                emoji = "🟡",
                tag = "DENSIDADE MÉDIA"
            ),
            DynamicCityZone(
                id = "z_consolacao",
                name = "Consolação / Jardins",
                relX = 0.22f,
                relY = 0.72f,
                baseOrdersPerHour = 19,
                surgeMultiplier = 1.05,
                dominantApps = "Lalamove 50% + Loggi 30%",
                primaryColor = Color(0xFF00FF88),
                secondaryColor = Color(0xFF00BFA5),
                emoji = "🟢",
                tag = "ESTÁVEL"
            )
        )

        all.filter { zone ->
            when (selectedFilter) {
                "🔥 SURGE" -> zone.surgeMultiplier >= 1.25
                "🍔 iFOOD" -> zone.dominantApps.contains("iFood", ignoreCase = true)
                "🌧️ CHUVA" -> rainMultiplier > 1.0 || zone.surgeMultiplier >= 1.30
                else -> true
            }
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = CockpitDarkBg),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(340.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Tactical Canvas Background Grid + Traffic Density Overlay + Global Demand Heatmap + Animated Flowing Route
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Draw Grid Lines (40px equivalent)
                val gridSize = 40.dp.toPx()
                var x = 0f
                while (x < w) {
                    drawLine(Color.White.copy(alpha = 0.03f), start = Offset(x, 0f), end = Offset(x, h), strokeWidth = 1f)
                    x += gridSize
                }
                var y = 0f
                while (y < h) {
                    drawLine(Color.White.copy(alpha = 0.03f), start = Offset(0f, y), end = Offset(w, y), strokeWidth = 1f)
                    y += gridSize
                }

                // Road Faint Lines
                val p1 = Offset(w * 0.20f, h * 0.65f) // YOU
                val p2 = Offset(w * 0.35f, h * 0.30f) // iFood BK
                val p3 = Offset(w * 0.62f, h * 0.25f) // Rappi PH
                val p4 = Offset(w * 0.55f, h * 0.60f) // Dest A
                val p5 = Offset(w * 0.85f, h * 0.55f) // Dest B

                drawLine(Color.White.copy(alpha = 0.08f), start = p1, end = p2, strokeWidth = 3f)
                drawLine(Color.White.copy(alpha = 0.08f), start = p2, end = p3, strokeWidth = 3f)
                drawLine(Color.White.copy(alpha = 0.08f), start = p3, end = p4, strokeWidth = 3f)
                drawLine(Color.White.copy(alpha = 0.08f), start = p4, end = p5, strokeWidth = 3f)

                // --- GLOBAL DYNAMIC DEMAND HEATMAP OVERLAYS ---
                if (showDemandHeatmap) {
                    cityZones.forEach { zone ->
                        val center = Offset(w * zone.relX, h * zone.relY)
                        val scaleFactor = (zone.baseOrdersPerHour / 35.0f).coerceIn(0.7f, 1.4f)
                        val zoneRadius = heatmapPulseRadius * scaleFactor

                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    zone.primaryColor.copy(alpha = 0.58f),
                                    zone.secondaryColor.copy(alpha = 0.32f),
                                    zone.primaryColor.copy(alpha = 0.10f),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = zoneRadius
                            ),
                            center = center,
                            radius = zoneRadius
                        )

                        // Dynamic contour ring for surge clusters
                        if (zone.surgeMultiplier >= 1.25) {
                            drawCircle(
                                color = zone.primaryColor.copy(alpha = 0.45f),
                                radius = zoneRadius * 1.15f,
                                center = center,
                                style = Stroke(
                                    width = 2.5f,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), dashOffset)
                                )
                            )
                        }
                    }
                }

                // --- REAL-TIME TRAFFIC DENSITY OVERLAY ---
                if (showTraffic) {
                    // Localized GPS Congestion Heat Rings around current location p1
                    drawCircle(color = CockpitDangerRed.copy(alpha = trafficPulseAlpha * 0.35f), radius = 65f, center = p1)
                    drawCircle(color = CockpitWarningAmber.copy(alpha = trafficPulseAlpha * 0.25f), radius = 100f, center = p1)

                    // Congestion Corridors
                    drawLine(color = CockpitDangerRed.copy(alpha = 0.65f), start = p1, end = p2, strokeWidth = 14f)
                    drawLine(color = CockpitWarningAmber.copy(alpha = 0.55f), start = p2, end = p3, strokeWidth = 10f)
                    drawLine(color = CockpitGreen.copy(alpha = 0.45f), start = p3, end = p4, strokeWidth = 8f)
                    drawLine(color = CockpitGreen.copy(alpha = 0.45f), start = p4, end = p5, strokeWidth = 8f)
                }

                // Active Animated Flow Route
                val routePath = Path().apply {
                    moveTo(p1.x, p1.y)
                    lineTo(p2.x, p2.y)
                    lineTo(p3.x, p3.y)
                    lineTo(p4.x, p4.y)
                    lineTo(p5.x, p5.y)
                }

                drawPath(
                    path = routePath,
                    color = CockpitGreen,
                    style = Stroke(
                        width = 6f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f), dashOffset)
                    )
                )
            }

            // FILTER CHIPS OVERLAY (Top-Start)
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 8.dp, start = 8.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(CockpitDarkBg.copy(alpha = 0.88f))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("TODAS", "🔥 SURGE", "🍔 iFOOD", "🌧️ CHUVA").forEach { filterTag ->
                    val isSel = selectedFilter == filterTag
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSel) Color(0xFFFF0055).copy(alpha = 0.25f) else Color.Transparent)
                            .border(0.5.dp, if (isSel) Color(0xFFFF0055) else Color.Transparent, RoundedCornerShape(6.dp))
                            .clickable { selectedFilter = filterTag }
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = filterTag,
                            color = if (isSel) Color.White else CockpitTextSecondary,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // STAR NODES OVER MAP
            // Node 1: YOU 🏍️
            StarNodeComposable(
                modifier = Modifier.align(Alignment.TopStart).offset(x = 30.dp, y = 160.dp),
                emoji = "🏍️",
                label = "VOCÊ (Piloto)",
                value = "Em movimento",
                iconBg = Brush.horizontalGradient(listOf(CockpitGreen, CockpitCyan)),
                valueColor = CockpitGreen,
                isPulsing = false,
                onClick = { onNodeClick("Sua posição GPS: Precisão de 1.2m em movimento.") }
            )

            // Node 2: iFood Burger King 🍔
            StarNodeComposable(
                modifier = Modifier.align(Alignment.TopStart).offset(x = 100.dp, y = 50.dp),
                emoji = "🍔",
                label = "iFood (Burger King)",
                value = "R$ 15,00",
                iconBg = Brush.linearGradient(listOf(CockpitIFood, CockpitIFood)),
                valueColor = CockpitGreen,
                isPulsing = hasGeminiNewRoute,
                pulseColor = CockpitGreen,
                onClick = {
                    val zone = cityZones.firstOrNull { it.id == "z_paulista" }
                    activeZoneTelemetry = zone
                    onNodeClick("Restaurante Burger King: Coleta iFood R$ 15,00 otimizada pelo Gemini AI. 🔥 Cluster de Alta Demanda (${zone?.baseOrdersPerHour ?: 48} ped/h).")
                }
            )

            // Node 3: Rappi Pizza Hut 🍕
            StarNodeComposable(
                modifier = Modifier.align(Alignment.TopStart).offset(x = 200.dp, y = 30.dp),
                emoji = "🍕",
                label = "Rappi (Pizza Hut)",
                value = "R$ 18,00",
                iconBg = Brush.linearGradient(listOf(CockpitRappi, CockpitRappi)),
                valueColor = CockpitGreen,
                isPulsing = hasGeminiNewRoute,
                pulseColor = CockpitCyan,
                onClick = {
                    val zone = cityZones.firstOrNull { it.id == "z_pinheiros" }
                    activeZoneTelemetry = zone
                    onNodeClick("Restaurante Pizza Hut: Coleta Rappi R$ 18,00 encadeada pelo Gemini AI. ⚡ Cluster de Média Demanda (${zone?.baseOrdersPerHour ?: 36} ped/h).")
                }
            )

            // Node 4: Delivery A 🏠
            StarNodeComposable(
                modifier = Modifier.align(Alignment.TopStart).offset(x = 170.dp, y = 140.dp),
                emoji = "🏠",
                label = "Entrega A (Paulista)",
                value = "Ponto 1",
                iconBg = Brush.linearGradient(listOf(CockpitGreen.copy(alpha = 0.3f), CockpitGreen.copy(alpha = 0.3f))),
                valueColor = CockpitGreen,
                isPulsing = hasGeminiNewRoute,
                pulseColor = CockpitGreen,
                onClick = { onNodeClick("Entrega A: Av. Paulista. Rota calculada pelo Gemini AI.") }
            )

            // Node 5: Delivery B 🏢
            StarNodeComposable(
                modifier = Modifier.align(Alignment.TopStart).offset(x = 270.dp, y = 120.dp),
                emoji = "🏢",
                label = "Entrega B (Consolação)",
                value = "Ponto 2",
                iconBg = Brush.linearGradient(listOf(CockpitCyan.copy(alpha = 0.3f), CockpitCyan.copy(alpha = 0.3f))),
                valueColor = CockpitCyan,
                isPulsing = hasGeminiNewRoute,
                pulseColor = CockpitCyan,
                onClick = { onNodeClick("Entrega B: Rua Consolação. Ponto final do stack neural.") }
            )

            // Node 6: Uber Solo ☕ (Bottom Right)
            StarNodeComposable(
                modifier = Modifier.align(Alignment.TopStart).offset(x = 260.dp, y = 210.dp),
                emoji = "☕",
                label = "Uber (Solo)",
                value = "R$ 9,00",
                iconBg = Brush.linearGradient(listOf(CockpitUber, CockpitUber)),
                valueColor = CockpitTextSecondary,
                isPulsing = false,
                onClick = { onNodeClick("Corrida Uber Solo descartada em prol do Stack Multi-App.") }
            )

            // HEATMAP TELEMETRY ACTIVE BANNER
            activeZoneTelemetry?.let { zone ->
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(start = 10.dp, end = 10.dp, bottom = 44.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CockpitDarkBg.copy(alpha = 0.95f))
                        .border(1.dp, zone.primaryColor, RoundedCornerShape(10.dp))
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(zone.emoji, fontSize = 12.sp)
                                Text(
                                    text = "${zone.name} [${zone.tag}]",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Text(
                                text = "${zone.baseOrdersPerHour} ped/h • Surge ${zone.surgeMultiplier}x • ${zone.dominantApps}",
                                color = zone.primaryColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(
                            onClick = { activeZoneTelemetry = null },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color.Gray, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            } ?: run {
                // DEFAULT HEATMAP DEMAND BADGE OVERLAY
                if (showDemandHeatmap) {
                    val topZone = cityZones.firstOrNull()
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 12.dp, bottom = 44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CockpitDarkBg.copy(alpha = 0.85f))
                            .border(1.dp, Color(0xFFFF0055).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .clickable { activeZoneTelemetry = topZone }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(topZone?.emoji ?: "🔥", fontSize = 11.sp)
                            Text(
                                text = "DENSIDADE REAL: ${topZone?.name ?: "Paulista"}: ${topZone?.baseOrdersPerHour ?: 48} ped/h (${topZone?.dominantApps ?: "iFood+Rappi"})",
                                color = topZone?.primaryColor ?: Color(0xFFFF5588),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // TRAFFIC DENSITY & GLOBAL HEATMAP CONTROL TOGGLES
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 8.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(CockpitDarkBg.copy(alpha = 0.88f))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Heatmap Global Toggle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.clickable {
                            showDemandHeatmap = !showDemandHeatmap
                            com.example.util.ToastUtils.showToast(context, if (showDemandHeatmap) "🔥 Heatmap Global de Demanda: ATIVADO" else "🔥 Heatmap Global: DESATIVADO", Toast.LENGTH_SHORT)
                        }
                    ) {
                        Text(
                            text = "🔥 HEATMAP GLOBAL",
                            color = if (showDemandHeatmap) Color(0xFFFF0055) else CockpitTextSecondary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (showDemandHeatmap) Color(0xFFFF0055) else CockpitTextSecondary)
                        )
                    }

                    // Traffic GPS Toggle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.clickable {
                            val newStatus = !showTraffic
                            RadarCoordinator.saveSettings(context, settings.copy(showTrafficDensity = newStatus, showTrafficOverlay = newStatus))
                            com.example.util.ToastUtils.showToast(context, if (newStatus) "🚦 Tráfego em Tempo Real: ATIVADO" else "🚦 Tráfego em Tempo Real: DESATIVADO", Toast.LENGTH_SHORT)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Route,
                            contentDescription = "Tráfego",
                            tint = if (showTraffic) CockpitDangerRed else CockpitTextSecondary,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "TRÁFEGO GPS",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (showTraffic) CockpitGreen else CockpitTextSecondary)
                        )
                    }

                    AnimatedVisibility(visible = showDemandHeatmap || showTraffic) {
                        Column(
                            horizontalAlignment = Alignment.Start,
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            if (showDemandHeatmap) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFFFF0055)))
                                    Text("Alta Demanda (>40/h)", color = CockpitTextSecondary, fontSize = 8.sp)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFFFF8800)))
                                    Text("Média Demanda (20-40/h)", color = CockpitTextSecondary, fontSize = 8.sp)
                                }
                            }
                            if (showTraffic) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(CockpitDangerRed))
                                    Text("Trânsito Pesado", color = CockpitTextSecondary, fontSize = 8.sp)
                                }
                            }
                        }
                    }
                }
            }

            // FEATURE 2: GHOST SEQUENCE OVERLAY (Bottom of Map)
            Card(
                colors = CardDefaults.cardColors(containerColor = CockpitPanelBg.copy(alpha = 0.92f)),
                border = BorderStroke(1.dp, CockpitGreen.copy(alpha = 0.35f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "👻 GHOST SEQUENCE ATIVO",
                            color = CockpitGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "83% Confiança",
                            color = CockpitGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Analisando padrões de demanda em 2km ao redor...",
                        color = CockpitTextSecondary,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                    
                    // Progress Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(0.83f)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Brush.horizontalGradient(listOf(CockpitGreen, CockpitCyan)))
                        )
                    }
                    
                    Text(
                        text = "⚡ 83% chance de stack multi-app em 3 min",
                        color = CockpitCyan,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun StarNodeComposable(
    modifier: Modifier = Modifier,
    emoji: String,
    label: String,
    value: String,
    iconBg: Brush,
    valueColor: Color,
    isPulsing: Boolean = false,
    pulseColor: Color = CockpitGreen,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "starNodePulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isPulsing) 1.12f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = if (isPulsing) 0.55f else 0f,
        targetValue = if (isPulsing) 0.15f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )
    val ringBorderWidthFloat by infiniteTransition.animateFloat(
        initialValue = if (isPulsing) 1.5f else 1.5f,
        targetValue = if (isPulsing) 3.5f else 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ringBorderWidth"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clickable(onClick = onClick)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(44.dp)
        ) {
            // Suave halo/anel de pulsação (Aura de processamento neural Gemini)
            if (isPulsing) {
                Box(
                    modifier = Modifier
                        .size(44.dp * pulseScale)
                        .clip(CircleShape)
                        .background(pulseColor.copy(alpha = glowAlpha * 0.35f))
                        .border(ringBorderWidthFloat.dp, pulseColor.copy(alpha = glowAlpha), CircleShape)
                )
            }

            // Ícone do marcador central
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconBg)
                    .border(
                        if (isPulsing) 1.8.dp else 1.5.dp,
                        if (isPulsing) pulseColor else Color.White.copy(alpha = 0.4f),
                        CircleShape
                    )
            ) {
                Text(text = emoji, fontSize = 16.sp)
            }
        }
        Text(
            text = label,
            color = Color.White,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(top = 2.dp)
                .background(Color.Black.copy(alpha = 0.85f), RoundedCornerShape(4.dp))
                .border(
                    0.5.dp,
                    if (isPulsing) pulseColor.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.2f),
                    RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 4.dp, vertical = 1.dp)
        )
        Text(
            text = value,
            color = if (isPulsing) pulseColor else valueColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace
        )
    }
}

// -----------------------------------------------------------------------------
// FEATURE 3: STACK PANEL (BENTO GRID STYLE)
// -----------------------------------------------------------------------------
@Composable
fun StackPanelBentoGrid(onAccept: (String) -> Unit, onDecline: (String) -> Unit) {
    val pendingOffers by com.example.util.MultiAppOrderManager.pendingOffers.collectAsStateWithLifecycle()
    val latestGeminiEval by RadarCoordinator.latestGeminiEvaluation.collectAsStateWithLifecycle()
    var showTunerModal by remember { mutableStateOf(false) }

    if (showTunerModal) {
        com.example.ui.GhostSequenceTunerModal(onDismissRequest = { showTunerModal = false })
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = CockpitPanelBg),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(text = "🧬 Stacks & Pedidos (Firestore)", color = CockpitTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Black)
                        Box(
                            modifier = Modifier
                                .background(CockpitGreen, CircleShape)
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Text(text = "${pendingOffers.size}", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    Text(text = "Sincronização em tempo real via Coleção 'pedidos'", color = CockpitTextSecondary, fontSize = 9.sp)
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = { showTunerModal = true },
                        colors = ButtonDefaults.buttonColors(containerColor = CockpitGreen.copy(alpha = 0.15f)),
                        border = BorderStroke(1.dp, CockpitGreen.copy(alpha = 0.5f)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = "🎛️ TUNER GHOST", color = CockpitGreen, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }

                    Button(
                        onClick = {
                            com.example.data.FirestoreManager.seedPedidosIfEmpty()
                            com.example.util.MultiAppOrderManager.startFirestoreSync()
                            onAccept("Sincronização Firestore")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.06f)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = "🔄 Sincronizar", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Gemini Neural Reasoning Card
            if (latestGeminiEval != null) {
                val eval = latestGeminiEval!!
                val isAccept = eval.decision.equals("ACCEPT", ignoreCase = true) || eval.decision.equals("CHAIN", ignoreCase = true)
                Card(
                    colors = CardDefaults.cardColors(containerColor = if (isAccept) CockpitGreen.copy(alpha = 0.10f) else CockpitDangerRed.copy(alpha = 0.10f)),
                    border = BorderStroke(1.dp, if (isAccept) CockpitGreen.copy(alpha = 0.6f) else CockpitDangerRed.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(text = "✨ IA GEMINI RECOMENDA:", color = if (isAccept) CockpitGreen else CockpitDangerRed, fontSize = 10.sp, fontWeight = FontWeight.Black)
                                Text(text = eval.decision, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
                            }
                            Text(
                                text = "Confiança: ${(eval.confidence * 100).toInt()}%",
                                color = CockpitTextSecondary,
                                fontSize = 9.sp
                            )
                        }
                        Text(
                            text = eval.reason,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (!eval.suggestedVoiceAnnouncement.isNullOrBlank()) {
                            Text(
                                text = "🎙️ \"${eval.suggestedVoiceAnnouncement}\"",
                                color = CockpitCyan,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // Constellation Map Visual Component com Clustering Google Maps
            if (pendingOffers.isNotEmpty()) {
                val convertedOffers = pendingOffers.map { p ->
                    com.example.coordinator.ActiveOffer(
                        appName = p.appName,
                        fareValue = p.fareValue,
                        pickupAddress = p.pickupAddress,
                        deliveryAddress = p.deliveryAddress,
                        totalDistance = p.totalDistance,
                        totalTime = p.totalTime
                    )
                }
                com.example.ui.components.OfferConstellationMap(
                    offers = convertedOffers,
                    geminiEvaluation = latestGeminiEval,
                    onOfferSelected = { selectedOffer ->
                        onAccept("${selectedOffer.appName} (R$ ${String.format("%.2f", selectedOffer.fareValue)})")
                    }
                )
            }

            if (pendingOffers.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("📡 Nenhum pedido pendente em 'pedidos'", color = CockpitTextSecondary, fontSize = 12.sp)
                        Button(
                            onClick = {
                                com.example.data.FirestoreManager.seedPedidosIfEmpty()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CockpitGreen),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Gerar Pedidos de Teste no Firestore", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                pendingOffers.forEach { offer ->
                    val isStack = offer.isChained || offer.appName.contains("+") || offer.appName.lowercase().contains("stack")
                    val gainPerKm = if (offer.totalDistance > 0) offer.fareValue / offer.totalDistance else offer.fareValue
                    val isAppIFood = offer.appName.lowercase().contains("ifood")
                    val isAppRappi = offer.appName.lowercase().contains("rappi")

                    Card(
                        colors = CardDefaults.cardColors(containerColor = if (isStack) CockpitGreen.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.03f)),
                        border = BorderStroke(if (isStack) 1.5.dp else 1.dp, if (isStack) CockpitGreen else Color.White.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isStack) "⚡ STACK MULTI-APP — ${offer.appName}" else "🍔 ${offer.appName.uppercase()}",
                                    color = if (isStack) CockpitGreen else if (isAppIFood) CockpitIFood else if (isAppRappi) CockpitRappi else CockpitCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy((-4).dp)) {
                                    if (isAppIFood || isStack) {
                                        Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(CockpitIFood), contentAlignment = Alignment.Center) {
                                            Text("iF", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black)
                                        }
                                    }
                                    if (isAppRappi || isStack) {
                                        Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(CockpitRappi), contentAlignment = Alignment.Center) {
                                            Text("Ra", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black)
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "R$ ${String.format("%.2f", offer.fareValue)}",
                                    color = if (isStack) CockpitGreen else Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = if (isStack) "Rota Combinada" else "Corrida Direta",
                                    color = CockpitTextSecondary,
                                    fontSize = 9.sp
                                )
                            }

                            // Meta Grid
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                    .padding(vertical = 4.dp, horizontal = 6.dp),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                MetaGridItem(label = "Distância", value = "${String.format("%.1f", offer.totalDistance)} km")
                                MetaGridItem(label = "Ganho/km", value = "R$ ${String.format("%.2f", gainPerKm)}", valueColor = if (gainPerKm >= 5.0) CockpitGreen else Color.White)
                                MetaGridItem(label = "Tempo", value = "${offer.totalTime.toInt()} min")
                            }

                            Text(
                                text = "${offer.pickupAddress} → ${offer.deliveryAddress}",
                                color = Color.LightGray,
                                fontSize = 9.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (offer.reason.isNotEmpty()) {
                                Text(
                                    text = "IA: ${offer.reason}",
                                    color = CockpitCyan,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Action Buttons
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    onClick = {
                                        val docId = offer.activeDeliveryDestination?.ifEmpty { offer.id.toString() } ?: offer.id.toString()
                                        com.example.data.FirestoreManager.updatePedidoStatusInFirestore(docId, "ACCEPTED")
                                        com.example.util.MultiAppOrderManager.addOrder(
                                            com.example.util.ActiveOrder(
                                                id = "order_${offer.id}",
                                                appName = offer.appName,
                                                fare = offer.fareValue,
                                                pickupAddress = offer.pickupAddress,
                                                deliveryAddress = offer.deliveryAddress,
                                                pickupLat = -23.5505,
                                                pickupLng = -46.6333,
                                                deliveryLat = -23.5615,
                                                deliveryLng = -46.6559,
                                                status = com.example.util.OrderStatus.PICKING_UP
                                            )
                                        )
                                        onAccept("Pedido ${offer.appName} (R$ ${offer.fareValue})")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (isStack) CockpitGreen else Color.White.copy(alpha = 0.12f)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(vertical = 6.dp)
                                ) {
                                    Text(
                                        text = if (isStack) "✅ ACEITAR STACK" else "✅ ACEITAR",
                                        color = if (isStack) Color.Black else Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                                OutlinedButton(
                                    onClick = {
                                        val docId = offer.activeDeliveryDestination?.ifEmpty { offer.id.toString() } ?: offer.id.toString()
                                        com.example.data.FirestoreManager.updatePedidoStatusInFirestore(docId, "CANCELLED")
                                        onDecline("Pedido ${offer.appName}")
                                    },
                                    border = BorderStroke(1.dp, CockpitDangerRed.copy(alpha = 0.5f)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CockpitDangerRed),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text("❌ RECUSAR", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetaGridItem(label: String, value: String, valueColor: Color = Color.White) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = CockpitTextSecondary, fontSize = 8.sp)
        Text(text = value, color = valueColor, fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
    }
}

// -----------------------------------------------------------------------------
// FEATURE 5: BOTTOM HEALTH PULSE BAR
// -----------------------------------------------------------------------------
@Composable
fun BottomHealthPulseBar(systemHealth: Int, onVoiceClick: () -> Unit, onStartRouteClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CockpitPanelBg.copy(alpha = 0.95f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pulse Heart Icon & Metrics
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(CockpitGreen.copy(alpha = 0.15f))
                        .border(1.5.dp, CockpitGreen, CircleShape)
                ) {
                    Icon(imageVector = Icons.Default.Favorite, contentDescription = "Saúde", tint = CockpitGreen, modifier = Modifier.size(16.dp))
                }
                
                Column {
                    Text(text = "Saúde do Sistema: $systemHealth/100", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(text = "GPS: 4.2m  •  Latência: 12ms  •  28°C", color = CockpitTextSecondary, fontSize = 9.sp)
                }
            }

            // Controls
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onVoiceClick,
                    modifier = Modifier
                        .size(34.dp)
                        .background(Color.White.copy(alpha = 0.08f), CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                ) {
                    Icon(imageVector = Icons.Default.Mic, contentDescription = "Jarvis Voz", tint = Color.White, modifier = Modifier.size(18.dp))
                }

                Button(
                    onClick = onStartRouteClick,
                    colors = ButtonDefaults.buttonColors(containerColor = CockpitCyan),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("▶ INICIAR ROTA", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}
