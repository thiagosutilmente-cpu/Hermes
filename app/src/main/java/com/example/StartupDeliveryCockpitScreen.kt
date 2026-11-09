package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

// -----------------------------------------------------------------------------
// PALETA DE CORES MODERNA ESTILO START-UP (iFood, Uber, 99, Nubank)
// -----------------------------------------------------------------------------
private val StartupBackground = Color(0xFF090D16)       // Obsidian ultra profundo e limpo
private val StartupSurface = Color(0xFF111827)          // Cinza ardósia moderno
private val StartupCardSurface = Color(0xFF162032)      // Superfície de card premium
private val StartupCardBorder = Color(0xFF26334D)       // Borda sutil de 1dp sem neon
private val StartupTextPrimary = Color(0xFFF9FAFB)      // Branco níveo
private val StartupTextSecondary = Color(0xFF94A3B8)    // Cinza suave de alta legibilidade
private val StartupTextMuted = Color(0xFF64748B)        // Cinza discreto

// Cores Oficiais das Startups de Mobilidade e Delivery
private val IFoodRed = Color(0xFFEA1D2C)
private val NinetyNineYellow = Color(0xFFF59E0B)
private val UberBlack = Color(0xFF000000)
private val EmeraldSuccess = Color(0xFF10B981)
private val AlertDanger = Color(0xFFEF4444)
private val PixTeal = Color(0xFF00B494)

enum class StartupTab(val label: String, val icon: ImageVector) {
    COCKPIT("Pista", Icons.Default.Navigation),
    EARNINGS("Ganhos", Icons.AutoMirrored.Filled.TrendingUp),
    FILTERS("Filtros", Icons.Default.Tune),
    PRO("Plano PRO", Icons.Default.WorkspacePremium)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartupDeliveryCockpitScreen(
    voiceManager: NeuralVoiceManager? = null,
    onOpenEmergencyDialog: () -> Unit = {},
    isNightMode: Boolean = true,
    onToggleNightMode: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf(StartupTab.COCKPIT) }
    var isDriverOnline by remember { mutableStateOf(true) }
    var isVoiceActive by remember { mutableStateOf(voiceManager?.isMuted != true) }

    // Métricas financeiras em tempo real
    var todayEarnings by remember { mutableDoubleStateOf(284.50) }
    var completedTrips by remember { mutableIntStateOf(18) }
    var onlineHours by remember { mutableStateOf("6h 40m") }
    var currentRatePerKm by remember { mutableDoubleStateOf(5.85) }

    // Critérios de filtro de R$/km
    var minGainPerKm by remember { mutableFloatStateOf(4.50f) }
    var minOfferValue by remember { mutableFloatStateOf(15.0f) }
    var autoAcceptEnabled by remember { mutableStateOf(true) }

    // Lista de ofertas ativas
    val activeOffers = remember {
        mutableStateListOf(
            StartupOffer(
                id = "off_1",
                appName = "iFood",
                appColor = IFoodRed,
                restaurant = "Madero Container - Faria Lima",
                pickupAddress = "Av. Brig. Faria Lima, 2000",
                destinationAddress = "Rua Haddock Lobo, 850 (Jardins)",
                payout = 33.50,
                distanceKm = 5.2,
                estimatedMinutes = 16,
                ratePerKm = 6.44,
                isChained = true,
                chainedBonus = 18.00,
                surgeActive = true
            ),
            StartupOffer(
                id = "off_2",
                appName = "99 Moto",
                appColor = NinetyNineYellow,
                restaurant = "Burger King - Av. Paulista",
                pickupAddress = "Av. Paulista, 1000",
                destinationAddress = "Rua Augusta, 1400 (Consolação)",
                payout = 21.00,
                distanceKm = 3.1,
                estimatedMinutes = 11,
                ratePerKm = 6.77,
                isChained = false,
                surgeActive = false
            ),
            StartupOffer(
                id = "off_3",
                appName = "Uber Flash",
                appColor = Color(0xFF0F172A),
                restaurant = "Drogasil Farmácia - Moema",
                pickupAddress = "Av. Ibirapuera, 3103",
                destinationAddress = "Alameda dos Arapanés, 420",
                payout = 19.50,
                distanceKm = 2.4,
                estimatedMinutes = 9,
                ratePerKm = 8.12,
                isChained = false,
                surgeActive = true
            )
        )
    }

    val backgroundOffers by BackgroundOfferDispatcher.latestOffers.collectAsState()
    LaunchedEffect(backgroundOffers) {
        backgroundOffers.forEach { radarOffer ->
            if (activeOffers.none { it.id == radarOffer.id }) {
                val appColor = when {
                    radarOffer.appName.contains("iFood", true) -> IFoodRed
                    radarOffer.appName.contains("99", true) -> NinetyNineYellow
                    radarOffer.appName.contains("Uber", true) -> Color(0xFF0F172A)
                    else -> Color(0xFFFF5722)
                }
                activeOffers.add(
                    0,
                    StartupOffer(
                        id = radarOffer.id,
                        appName = radarOffer.appName,
                        appColor = appColor,
                        restaurant = radarOffer.restaurant,
                        pickupAddress = radarOffer.pickupLocation?.address ?: "${radarOffer.restaurant}, São Paulo",
                        destinationAddress = "Destino do Cliente",
                        payout = radarOffer.value,
                        distanceKm = radarOffer.distanceKm,
                        estimatedMinutes = radarOffer.estimatedTimeMin,
                        ratePerKm = radarOffer.gainPerKm,
                        isChained = radarOffer.gainPerKm > 6.0,
                        chainedBonus = if (radarOffer.gainPerKm > 6.0) 16.00 else 0.0,
                        surgeActive = radarOffer.gainPerKm > 5.0
                    )
                )
            }
        }
    }

    Scaffold(
        containerColor = StartupBackground,
        topBar = {
            StartupTopHeader(
                isOnline = isDriverOnline,
                onToggleOnline = {
                    isDriverOnline = !isDriverOnline
                    HapticFeedbackHelper.vibrateSuccess(context)
                    if (isDriverOnline) {
                        Toast.makeText(context, "Radar Ativo: Você está online nos 4 apps!", Toast.LENGTH_SHORT).show()
                        if (isVoiceActive) voiceManager?.speak("Você está online. Radar ativado buscando as melhores corridas.")
                    } else {
                        Toast.makeText(context, "Radar em pausa: Você está offline.", Toast.LENGTH_SHORT).show()
                        if (isVoiceActive) voiceManager?.speak("Radar em pausa.")
                    }
                },
                isVoiceActive = isVoiceActive,
                onToggleVoice = {
                    isVoiceActive = !isVoiceActive
                    voiceManager?.isMuted = !isVoiceActive
                    HapticFeedbackHelper.vibrateTap(context)
                },
                onOpenEmergency = onOpenEmergencyDialog
            )
        },
        bottomBar = {
            StartupBottomNavigationBar(
                currentTab = selectedTab,
                onSelectTab = { selectedTab = it }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                StartupTab.COCKPIT -> {
                    StartupCockpitTab(
                        isOnline = isDriverOnline,
                        todayEarnings = todayEarnings,
                        completedTrips = completedTrips,
                        onlineHours = onlineHours,
                        ratePerKm = currentRatePerKm,
                        offers = activeOffers,
                        onAcceptOffer = { offer ->
                            val currentSpeed = LocationService.globalLocationState.value.currentSpeedKmh
                            val isLocked = LocationService.globalLocationState.value.isSafetyLockActive || currentSpeed > 10.0
                            if (isLocked) {
                                HapticFeedbackHelper.vibrateDecline(context)
                                Toast.makeText(context, "🚨 Toque manual bloqueado por segurança: moto em movimento (${String.format(Locale("pt", "BR"), "%.1f", currentSpeed)} km/h)", Toast.LENGTH_SHORT).show()
                                if (isVoiceActive) voiceManager?.speak("Toque bloqueado por segurança. Moto em movimento.")
                                return@StartupCockpitTab
                            }
                            HapticFeedbackHelper.vibrateAccept(context)
                            activeOffers.remove(offer)
                            todayEarnings += offer.payout
                            completedTrips += 1
                            try {
                                DailyGoalTrackerManager.getInstance(context).recordDeliveryEarnings(offer.payout, offer.distanceKm * 0.17)
                            } catch (_: Exception) {}
                            Toast.makeText(context, "Corrida aceita no ${offer.appName} (+R$ ${String.format(Locale("pt", "BR"), "%.2f", offer.payout)})", Toast.LENGTH_LONG).show()
                            if (isVoiceActive) {
                                voiceManager?.speak("Corrida aceita. Navegando para ${offer.restaurant}.")
                            }
                        },
                        onDeclineOffer = { offer ->
                            HapticFeedbackHelper.vibrateDecline(context)
                            activeOffers.remove(offer)
                            Toast.makeText(context, "Oferta recusada.", Toast.LENGTH_SHORT).show()
                        },
                        onSimulateNewOffer = {
                            val newOffer = StartupOffer(
                                id = "sim_${System.currentTimeMillis() % 10000}",
                                appName = listOf("iFood", "99 Moto", "Uber Direct", "Rappi").random(),
                                appColor = listOf(IFoodRed, NinetyNineYellow, Color(0xFF0F172A), Color(0xFFFF5722)).random(),
                                restaurant = listOf("Outback Steakhouse", "McDonald's Berrini", "Coco Bambu", "Habib's Rebouças").random(),
                                pickupAddress = "Av. Eng. Luís Carlos Berrini, 1200",
                                destinationAddress = "Rua Gomes de Carvalho, 1500 (Vila Olímpia)",
                                payout = (18..45).random().toDouble(),
                                distanceKm = (20..65).random() / 10.0,
                                estimatedMinutes = (10..25).random(),
                                ratePerKm = (55..85).random() / 10.0,
                                isChained = (1..2).random() == 1,
                                chainedBonus = 16.00,
                                surgeActive = true
                            )
                            activeOffers.add(0, newOffer)
                            HapticFeedbackHelper.vibrateSuccess(context)
                            if (isVoiceActive) {
                                voiceManager?.speak("Nova corrida de R$ ${newOffer.payout.toInt()} no ${newOffer.appName}.")
                            }
                        },
                        onUpgradePro = { selectedTab = StartupTab.PRO }
                    )
                }

                StartupTab.EARNINGS -> {
                    StartupEarningsTab(
                        todayEarnings = todayEarnings,
                        completedTrips = completedTrips,
                        onlineHours = onlineHours,
                        ratePerKm = currentRatePerKm
                    )
                }

                StartupTab.FILTERS -> {
                    StartupFiltersTab(
                        minGainPerKm = minGainPerKm,
                        onMinGainChange = { minGainPerKm = it },
                        minOfferValue = minOfferValue,
                        onMinOfferValueChange = { minOfferValue = it },
                        autoAcceptEnabled = autoAcceptEnabled,
                        onToggleAutoAccept = { autoAcceptEnabled = it }
                    )
                }

                StartupTab.PRO -> {
                    StartupFintechCheckoutScreen(
                        onPaymentSuccess = {
                            Toast.makeText(context, "Parabéns! Assinatura PRO Ativada com Sucesso!", Toast.LENGTH_LONG).show()
                            selectedTab = StartupTab.COCKPIT
                        }
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// TOP HEADER ESTILO STARTUP (UBER / 99 / iFOOD)
// -----------------------------------------------------------------------------
@Composable
private fun StartupTopHeader(
    isOnline: Boolean,
    onToggleOnline: () -> Unit,
    isVoiceActive: Boolean,
    onToggleVoice: () -> Unit,
    onOpenEmergency: () -> Unit
) {
    Surface(
        color = StartupSurface,
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Perfil do Motorista com status ao vivo
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(StartupCardSurface)
                                .border(1.5.dp, if (isOnline) EmeraldSuccess else StartupCardBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🛵", fontSize = 22.sp)
                        }
                        Box(
                            modifier = Modifier
                                .size(13.dp)
                                .clip(CircleShape)
                                .background(if (isOnline) EmeraldSuccess else AlertDanger)
                                .border(2.dp, StartupSurface, CircleShape)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Radar Piloto",
                                color = StartupTextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = EmeraldSuccess.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "PRO",
                                    color = EmeraldSuccess,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        val locState by LocationService.globalLocationState.collectAsState()
                        val gpsSubtitle = if (!isOnline) {
                            "Em Espera • Modo Pausado"
                        } else if (locState.isSafetyLockActive) {
                            "🚨 Trava de Velocidade (${String.format(Locale("pt", "BR"), "%.0f", locState.currentSpeedKmh)} km/h)"
                        } else if (locState.isTracking) {
                            "GPS ±${String.format(Locale("pt", "BR"), "%.1f", locState.accuracyMeters.coerceAtLeast(1.0f))}m • 4 Apps Integrados"
                        } else {
                            "GPS Pronto • 4 Apps Integrados"
                        }
                        Text(
                            text = gpsSubtitle,
                            color = if (locState.isSafetyLockActive && isOnline) AlertDanger else StartupTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (locState.isSafetyLockActive && isOnline) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                // Ações do Topo: Som e SOS
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onToggleVoice,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(StartupCardSurface)
                    ) {
                        Icon(
                            imageVector = if (isVoiceActive) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = "Voz",
                            tint = if (isVoiceActive) EmeraldSuccess else StartupTextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onOpenEmergency,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(AlertDanger.copy(alpha = 0.18f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "SOS Emergência",
                            tint = AlertDanger,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Barra de Status Online/Offline estilo Uber Driver / 99
            val toggleBgColor by animateColorAsState(
                targetValue = if (isOnline) EmeraldSuccess else Color(0xFF334155),
                animationSpec = tween(300),
                label = "onlineToggleColor"
            )

            Surface(
                onClick = onToggleOnline,
                color = toggleBgColor,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("driver_online_toggle_button")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isOnline) "VOCÊ ESTÁ ONLINE" else "VOCÊ ESTÁ OFFLINE",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    }

                    Text(
                        text = if (isOnline) "Toque para Pausar" else "Toque para Conectar",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// ABA 1: COCKPIT DE PISTA (OFERTAS E MONITORAMENTO EM TEMPO REAL)
// -----------------------------------------------------------------------------
@Composable
private fun StartupCockpitTab(
    isOnline: Boolean,
    todayEarnings: Double,
    completedTrips: Int,
    onlineHours: String,
    ratePerKm: Double,
    offers: List<StartupOffer>,
    onAcceptOffer: (StartupOffer) -> Unit,
    onDeclineOffer: (StartupOffer) -> Unit,
    onSimulateNewOffer: () -> Unit,
    onUpgradePro: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Resumo Financeiro Compacto no Topo
        item {
            StartupDailySummaryCard(
                todayEarnings = todayEarnings,
                completedTrips = completedTrips,
                onlineHours = onlineHours,
                ratePerKm = ratePerKm
            )
        }

        // 2. Barramento de Conexão com os 4 Apps (Multi-App Synergy)
        item {
            StartupMultiAppStatusBar()
        }

        // 3. Título da Seção com Botão de Teste de Corrida
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Chamadas Disponíveis",
                        color = StartupTextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = StartupCardSurface,
                        shape = CircleShape
                    ) {
                        Text(
                            text = "${offers.size}",
                            color = EmeraldSuccess,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                TextButton(onClick = onSimulateNewOffer) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = EmeraldSuccess,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Nova Oferta",
                        color = EmeraldSuccess,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // 4. Lista de Ofertas Interceptadas
        if (offers.isEmpty()) {
            item {
                StartupEmptyStateCard(isOnline = isOnline, onSimulate = onSimulateNewOffer)
            }
        } else {
            items(offers, key = { it.id }) { offer ->
                StartupDeliveryOfferCard(
                    offer = offer,
                    onAccept = { onAcceptOffer(offer) },
                    onDecline = { onDeclineOffer(offer) }
                )
            }
        }

        // 5. Banner de Assinatura PRO
        item {
            StartupProFeatureBanner(onUpgrade = onUpgradePro)
        }
    }
}

// -----------------------------------------------------------------------------
// CARD DE RESUMO FINANCEIRO (GANHOS DE HOJE)
// -----------------------------------------------------------------------------
@Composable
private fun StartupDailySummaryCard(
    todayEarnings: Double,
    completedTrips: Int,
    onlineHours: String,
    ratePerKm: Double
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("earnings_summary_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = StartupSurface),
        border = BorderStroke(1.dp, StartupCardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "GANHO DE HOJE",
                    color = StartupTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Surface(
                    color = EmeraldSuccess.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = null,
                            tint = EmeraldSuccess,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "+18% que ontem",
                            color = EmeraldSuccess,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = String.format(Locale("pt", "BR"), "R$ %.2f", todayEarnings),
                color = StartupTextPrimary,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = StartupCardBorder, thickness = 1.dp)
            Spacer(modifier = Modifier.height(14.dp))

            // Linha com as 3 métricas de pista
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StartupMetricItem(
                    label = "Viagens",
                    value = "$completedTrips",
                    icon = Icons.Default.CheckCircle
                )
                StartupMetricItem(
                    label = "Tempo Online",
                    value = onlineHours,
                    icon = Icons.Default.Schedule
                )
                StartupMetricItem(
                    label = "Média / km",
                    value = String.format(Locale("pt", "BR"), "R$ %.2f", ratePerKm),
                    icon = Icons.Default.Speed
                )
            }
        }
    }
}

@Composable
private fun StartupMetricItem(
    label: String,
    value: String,
    icon: ImageVector
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(StartupCardSurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = StartupTextSecondary,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = value,
                color = StartupTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                color = StartupTextSecondary,
                fontSize = 11.sp
            )
        }
    }
}

// -----------------------------------------------------------------------------
// STATUS DOS 4 APPS CONECTADOS (MULTI-APP SYNERGY BAR)
// -----------------------------------------------------------------------------
@Composable
private fun StartupMultiAppStatusBar() {
    Surface(
        color = StartupSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, StartupCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "RADAR MULTI-APPS INTEGRADO",
                color = StartupTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StartupAppBadge(name = "iFood", color = IFoodRed, active = true)
                StartupAppBadge(name = "99 Moto", color = NinetyNineYellow, active = true)
                StartupAppBadge(name = "Uber", color = Color.White, active = true)
                StartupAppBadge(name = "Rappi", color = Color(0xFFFF5722), active = true)
            }
        }
    }
}

@Composable
private fun StartupAppBadge(name: String, color: Color, active: Boolean) {
    Surface(
        color = StartupCardSurface,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (active) color.copy(alpha = 0.4f) else StartupCardBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (active) EmeraldSuccess else StartupTextMuted)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = name,
                color = StartupTextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// -----------------------------------------------------------------------------
// CARD DE CORRIDA 100% ESTILO iFOOD / UBER DRIVER
// -----------------------------------------------------------------------------
data class StartupOffer(
    val id: String,
    val appName: String,
    val appColor: Color,
    val restaurant: String,
    val pickupAddress: String,
    val destinationAddress: String,
    val payout: Double,
    val distanceKm: Double,
    val estimatedMinutes: Int,
    val ratePerKm: Double,
    val isChained: Boolean = false,
    val chainedBonus: Double = 0.0,
    val surgeActive: Boolean = false
)

@Composable
private fun StartupDeliveryOfferCard(
    offer: StartupOffer,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("delivery_offer_card_${offer.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = StartupSurface),
        border = BorderStroke(1.5.dp, if (offer.isChained) EmeraldSuccess.copy(alpha = 0.5f) else StartupCardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Cabeçalho da Oferta: App de Origem + Payout Total
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Badge do App de Origem
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = offer.appColor,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = offer.appName,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    if (offer.surgeActive) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = NinetyNineYellow.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "🔥 Tarifa Alta",
                                color = NinetyNineYellow,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                // Valor total a receber
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = String.format(Locale("pt", "BR"), "R$ %.2f", offer.payout),
                        color = EmeraldSuccess,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = String.format(Locale("pt", "BR"), "R$ %.2f/km", offer.ratePerKm),
                        color = StartupTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Destaque de Rota Encadeada Dupla (Exclusivo Radar)
            if (offer.isChained) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = EmeraldSuccess.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⚡", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Entrega Encadeada: +R$ ${String.format(Locale("pt", "BR"), "%.2f", offer.chainedBonus)} no mesmo trajeto!",
                            color = EmeraldSuccess,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Timeline de Coleta e Entrega (Estilo iFood / Uber)
            Row(modifier = Modifier.fillMaxWidth()) {
                // Coluna com indicadores visuais da rota
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(EmeraldSuccess)
                    )
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(34.dp)
                            .background(StartupCardBorder)
                    )
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(AlertDanger)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Endereços e Restaurante
                Column(modifier = Modifier.weight(1f)) {
                    // Ponto de Coleta
                    Column {
                        Text(
                            text = offer.restaurant,
                            color = StartupTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = offer.pickupAddress,
                            color = StartupTextSecondary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Ponto de Entrega
                    Column {
                        Text(
                            text = "Destino do Cliente",
                            color = StartupTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = offer.destinationAddress,
                            color = StartupTextSecondary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Barra com Distância e Tempo estimado
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(StartupCardSurface, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Distância total: ${offer.distanceKm} km",
                    color = StartupTextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "~${offer.estimatedMinutes} minutos",
                    color = StartupTextSecondary,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Botões de Ação (Recusar e Aceitar)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDecline,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertDanger),
                    border = BorderStroke(1.dp, AlertDanger.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                ) {
                    Text(
                        text = "Recusar",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onAccept,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                    modifier = Modifier
                        .weight(2f)
                        .height(46.dp)
                ) {
                    Text(
                        text = "ACEITAR CORRIDA",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// ESTADO VAZIO COM BOTÃO DE SIMULAÇÃO
// -----------------------------------------------------------------------------
@Composable
private fun StartupEmptyStateCard(isOnline: Boolean, onSimulate: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = StartupSurface),
        border = BorderStroke(1.dp, StartupCardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("📡", fontSize = 42.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (isOnline) "Monitorando Chamadas em Tempo Real" else "Radar em Pausa",
                color = StartupTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (isOnline)
                    "Aguardando notificações ou ofertas na tela dos apps iFood, 99, Uber e Rappi."
                else
                    "Ative o botão online acima para começar a interceptar as melhores chamadas.",
                color = StartupTextSecondary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onSimulate,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = StartupCardSurface)
            ) {
                Text("Simular Chamada Agora", color = EmeraldSuccess, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// -----------------------------------------------------------------------------
// BANNER DE RECURSOS PRO
// -----------------------------------------------------------------------------
@Composable
private fun StartupProFeatureBanner(onUpgrade: () -> Unit) {
    Surface(
        onClick = onUpgrade,
        color = StartupCardSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, PixTeal.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(PixTeal.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text("👑", fontSize = 22.sp)
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Acesso Ilimitado ao Plano PRO",
                    color = StartupTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Rotas duplas, cálculo automático no fone e auto-aceite.",
                    color = StartupTextSecondary,
                    fontSize = 11.sp
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = PixTeal
            )
        }
    }
}

// -----------------------------------------------------------------------------
// ABA 2: EXTRATO & GANHOS (ESTILO NUBANK / UBER DRIVER)
// -----------------------------------------------------------------------------
@Composable
private fun StartupEarningsTab(
    todayEarnings: Double,
    completedTrips: Int,
    onlineHours: String,
    ratePerKm: Double
) {
    val context = LocalContext.current

    val historyDeliveries = remember {
        listOf(
            HistoryItem("iFood", IFoodRed, "Madero Prime Faria Lima", 38.50, "13:42", "5.1 km"),
            HistoryItem("99 Moto", NinetyNineYellow, "McDonald's Rebouças", 22.00, "13:05", "3.2 km"),
            HistoryItem("iFood", IFoodRed, "Habib's Teodoro", 17.50, "12:28", "2.4 km"),
            HistoryItem("Uber Flash", Color.White, "Droga Raia Jardins", 26.00, "11:50", "3.8 km"),
            HistoryItem("Rappi", Color(0xFFFF5722), "Starbucks Frei Caneca", 29.50, "11:15", "4.0 km"),
            HistoryItem("iFood", IFoodRed, "BK Paulista", 33.00, "10:30", "4.5 km")
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = StartupSurface),
                border = BorderStroke(1.dp, StartupCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "RESUMO DA SEMANA",
                        color = StartupTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "R$ 1.642,80",
                        color = StartupTextPrimary,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // Barra de Meta Diária
                    Text(
                        text = "Meta Diária: R$ 284,50 de R$ 350,00 (81%)",
                        color = StartupTextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { 0.81f },
                        color = EmeraldSuccess,
                        trackColor = StartupCardSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                }
            }
        }

        item {
            Text(
                text = "Histórico de Corridas Hoje",
                color = StartupTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        items(historyDeliveries) { item ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = StartupSurface),
                border = BorderStroke(1.dp, StartupCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = item.color,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = item.app.take(1),
                                    color = if (item.color == Color.White) Color.Black else Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = item.restaurant,
                                color = StartupTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${item.time} • ${item.distance}",
                                color = StartupTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Text(
                        text = String.format(Locale("pt", "BR"), "+R$ %.2f", item.amount),
                        color = EmeraldSuccess,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

data class HistoryItem(
    val app: String,
    val color: Color,
    val restaurant: String,
    val amount: Double,
    val time: String,
    val distance: String
)

// -----------------------------------------------------------------------------
// ABA 3: FILTROS DE R$/KM E PREFERÊNCIAS DE DESPACHO
// -----------------------------------------------------------------------------
@Composable
private fun StartupFiltersTab(
    minGainPerKm: Float,
    onMinGainChange: (Float) -> Unit,
    minOfferValue: Float,
    onMinOfferValueChange: (Float) -> Unit,
    autoAcceptEnabled: Boolean,
    onToggleAutoAccept: (Boolean) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Filtros de Rentabilidade",
                color = StartupTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Recuse automaticamente corridas com taxas desvantajosas que dão prejuízo de combustível.",
                color = StartupTextSecondary,
                fontSize = 12.sp
            )
        }

        // Filtro de Piso R$/km
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = StartupSurface),
                border = BorderStroke(1.dp, StartupCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Ganho Mínimo por Km",
                            color = StartupTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = String.format(Locale("pt", "BR"), "R$ %.2f / km", minGainPerKm),
                            color = EmeraldSuccess,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = minGainPerKm,
                        onValueChange = onMinGainChange,
                        valueRange = 2.0f..10.0f,
                        steps = 16,
                        colors = SliderDefaults.colors(
                            thumbColor = EmeraldSuccess,
                            activeTrackColor = EmeraldSuccess,
                            inactiveTrackColor = StartupCardSurface
                        )
                    )
                    Text(
                        text = "Corridas abaixo desse piso serão silenciadas.",
                        color = StartupTextMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Filtro de Valor Mínimo Absoluto
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = StartupSurface),
                border = BorderStroke(1.dp, StartupCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Valor Mínimo da Corrida",
                            color = StartupTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = String.format(Locale("pt", "BR"), "R$ %.2f", minOfferValue),
                            color = EmeraldSuccess,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = minOfferValue,
                        onValueChange = onMinOfferValueChange,
                        valueRange = 8.0f..40.0f,
                        steps = 16,
                        colors = SliderDefaults.colors(
                            thumbColor = EmeraldSuccess,
                            activeTrackColor = EmeraldSuccess,
                            inactiveTrackColor = StartupCardSurface
                        )
                    )
                }
            }
        }

        // Auto-Aceite Inteligente
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = StartupSurface),
                border = BorderStroke(1.dp, StartupCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-Aceite de Corridas VIP",
                            color = StartupTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Aceita automaticamente ofertas com taxa acima de R$ 7,00/km sem precisar tirar a mão do guidão.",
                            color = StartupTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = autoAcceptEnabled,
                        onCheckedChange = onToggleAutoAccept,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = EmeraldSuccess,
                            uncheckedThumbColor = StartupTextMuted,
                            uncheckedTrackColor = StartupCardSurface
                        )
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// ABA 4: CHECKOUT FINTECH PROFISSIONAL (ASAAS / PIX / CARTÃO)
// -----------------------------------------------------------------------------
enum class ProPlanTier(
    val id: String,
    val title: String,
    val priceDisplay: String,
    val period: String,
    val savingsBadge: String?,
    val isPopular: Boolean
) {
    SEMANAL("weekly", "Semanal", "R$ 25,00", "/ semana", "MAIS ESCOLHIDO", true),
    MENSAL("monthly", "Mensal", "R$ 89,90", "/ mês", "ECONOMIZE 10%", false),
    TRIMESTRAL("annual", "Trimestral VIP", "R$ 229,00", "/ 3 meses", "ECONOMIZE 25%", false)
}

@Composable
fun StartupFintechCheckoutScreen(
    onPaymentSuccess: () -> Unit = {}
) {
    val context = LocalContext.current
    var selectedPlan by remember { mutableStateOf(ProPlanTier.SEMANAL) }
    var isCopied by remember { mutableStateOf(false) }

    val pixKey = when (selectedPlan) {
        ProPlanTier.SEMANAL -> "00020126580014br.gov.bcb.pix0136d8924b17-742a-43d9-9521-radarpro2026520400005303986540525.005802BR5916RADAR DELIVERY6009SAO PAULO62070503***6304E8A2"
        ProPlanTier.MENSAL -> "00020126580014br.gov.bcb.pix0136d8924b17-742a-43d9-9521-radarpro2026520400005303986540589.905802BR5916RADAR DELIVERY6009SAO PAULO62070503***6304C4B1"
        ProPlanTier.TRIMESTRAL -> "00020126580014br.gov.bcb.pix0136d8924b17-742a-43d9-9521-radarpro20265204000053039865406229.005802BR5916RADAR DELIVERY6009SAO PAULO62070503***6304F91A"
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Cabeçalho Fintech Seguro
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    color = PixTeal.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = PixTeal,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "CHECKOUT FINTECH CRIPTOGRAFADO 256-BIT",
                            color = PixTeal,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Turbine Seus Ganhos com o PRO",
                    color = StartupTextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Acesso completo a rotas duplas, cálculo automático no fone e auto-aceite nos 4 apps.",
                    color = StartupTextSecondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        // 2. Seletor de Planos
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ProPlanTier.values().forEach { plan ->
                    val isSelected = selectedPlan == plan
                    Surface(
                        onClick = { selectedPlan = plan },
                        color = if (isSelected) StartupCardSurface else StartupSurface,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(
                            1.5.dp,
                            if (isSelected) PixTeal else StartupCardBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedPlan = plan },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = PixTeal,
                                        unselectedColor = StartupTextMuted
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = plan.title,
                                            color = StartupTextPrimary,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (plan.savingsBadge != null) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                color = if (plan.isPopular) EmeraldSuccess else PixTeal,
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = plan.savingsBadge,
                                                    color = Color.White,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Black,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = "Liberação imediata via PIX",
                                        color = StartupTextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = plan.priceDisplay,
                                    color = StartupTextPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = plan.period,
                                    color = StartupTextSecondary,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Card do QR Code PIX Nativo e Chave Copia e Cola
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = StartupSurface),
                border = BorderStroke(1.dp, StartupCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Pague com PIX Instantâneo",
                        color = StartupTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Reconhecimento automático pelo gateway Asaas em menos de 10 segundos.",
                        color = StartupTextSecondary,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // QR Code Desenhado Nativamente via Canvas
                    Box(
                        modifier = Modifier
                            .size(190.dp)
                            .background(Color.White, RoundedCornerShape(14.dp))
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        NativePixQrCanvas(modifier = Modifier.fillMaxSize())
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Botão Copiar Chave PIX
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Chave PIX Radar PRO", pixKey)
                            clipboard.setPrimaryClip(clip)
                            isCopied = true
                            HapticFeedbackHelper.vibrateSuccess(context)
                            Toast.makeText(context, "Código PIX Copiado com Sucesso!", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCopied) EmeraldSuccess else PixTeal
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("copy_pix_button")
                    ) {
                        Icon(
                            imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isCopied) "CÓDIGO PIX COPIADO!" else "COPIAR CÓDIGO PIX (1 TOQUE)",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Opção de Pagar via Link Asaas
                    OutlinedButton(
                        onClick = {
                            val opened = AsaasCheckoutManager.openCheckout(context, selectedPlan.id, selectedPlan.title)
                            if (!opened) {
                                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(AsaasCheckoutManager.getCheckoutUrl(selectedPlan.id)))
                                context.startActivity(browserIntent)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, StartupCardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Text(
                            text = "Pagar com Cartão de Crédito ou Boleto",
                            color = StartupTextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // 4. Selos de Confiança e Garantia
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🛡️ Cancelamento fácil sem carência • Processado por Asaas Fintech",
                    color = StartupTextMuted,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// CANVAS DO QR CODE PIX NATIVO
// -----------------------------------------------------------------------------
@Composable
private fun NativePixQrCanvas(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cellCount = 21
        val cellSize = w / cellCount

        // Padrões dos cantos (Finder Patterns)
        fun drawFinderPattern(startX: Float, startY: Float) {
            drawRoundRect(
                color = Color.Black,
                topLeft = Offset(startX, startY),
                size = Size(cellSize * 7, cellSize * 7),
                cornerRadius = CornerRadius(6f, 6f)
            )
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(startX + cellSize, startY + cellSize),
                size = Size(cellSize * 5, cellSize * 5),
                cornerRadius = CornerRadius(4f, 4f)
            )
            drawRoundRect(
                color = Color.Black,
                topLeft = Offset(startX + cellSize * 2, startY + cellSize * 2),
                size = Size(cellSize * 3, cellSize * 3),
                cornerRadius = CornerRadius(2f, 2f)
            )
        }

        drawFinderPattern(0f, 0f)
        drawFinderPattern(w - cellSize * 7, 0f)
        drawFinderPattern(0f, h - cellSize * 7)

        // Matriz de dados com semente visual estável
        for (i in 0 until cellCount) {
            for (j in 0 until cellCount) {
                // Não desenha sobre os cantos
                val inTopLeft = (i < 8 && j < 8)
                val inTopRight = (i > 13 && j < 8)
                val inBottomLeft = (i < 8 && j > 13)
                val inCenter = (i in 9..11 && j in 9..11)

                if (!inTopLeft && !inTopRight && !inBottomLeft && !inCenter) {
                    val pseudoRandomBit = ((i * 37 + j * 17 + (i xor j)) % 3) == 0
                    if (pseudoRandomBit) {
                        drawRect(
                            color = Color.Black,
                            topLeft = Offset(i * cellSize, j * cellSize),
                            size = Size(cellSize - 1f, cellSize - 1f)
                        )
                    }
                }
            }
        }

        // Diamante central do PIX
        val center = Offset(w / 2, h / 2)
        val diamondSize = cellSize * 2.8f
        val pixPath = Path().apply {
            moveTo(center.x, center.y - diamondSize)
            lineTo(center.x + diamondSize, center.y)
            lineTo(center.x, center.y + diamondSize)
            lineTo(center.x - diamondSize, center.y)
            close()
        }
        drawPath(path = pixPath, color = PixTeal)
    }
}

// -----------------------------------------------------------------------------
// BARRA DE NAVEGAÇÃO INFERIOR ESTILO STARTUP (UBER / iFOOD)
// -----------------------------------------------------------------------------
@Composable
private fun StartupBottomNavigationBar(
    currentTab: StartupTab,
    onSelectTab: (StartupTab) -> Unit
) {
    NavigationBar(
        containerColor = StartupSurface,
        tonalElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        StartupTab.values().forEach { tab ->
            val isSelected = currentTab == tab
            NavigationBarItem(
                selected = isSelected,
                onClick = { onSelectTab(tab) },
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = {
                    Text(
                        text = tab.label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = EmeraldSuccess,
                    selectedTextColor = EmeraldSuccess,
                    unselectedIconColor = StartupTextSecondary,
                    unselectedTextColor = StartupTextSecondary,
                    indicatorColor = EmeraldSuccess.copy(alpha = 0.12f)
                )
            )
        }
    }
}
