package com.example

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

// -----------------------------------------------------------------------------
// SISTEMA DE CORES ADAPTATIVO: NOITE OLED vs MODO SOL ALTO CONTRASTE (WCAG AAA)
// -----------------------------------------------------------------------------
@Immutable
data class CockpitPalette(
    val isNight: Boolean,
    val background: Color,
    val surface: Color,
    val cardSurface: Color,
    val border: Color,
    val borderWidth: Dp,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val emerald: Color,
    val emeraldTint: Color,
    val amber: Color,
    val amberTint: Color,
    val danger: Color,
    val dangerTint: Color,
    val pixTeal: Color,
    val uberBadgeBg: Color,
    val uberBadgeFg: Color,
    val toggleOfflineBg: Color
)

private val NightCockpitPalette = CockpitPalette(
    isNight = true,
    background = Color(0xFF070B12),
    surface = Color(0xFF101826),
    cardSurface = Color(0xFF172235),
    border = Color(0xFF283652),
    borderWidth = 1.dp,
    textPrimary = Color(0xFFF9FAFB),
    textSecondary = Color(0xFFCBD5E1),
    textMuted = Color(0xFF94A3B8),
    emerald = Color(0xFF10B981),
    emeraldTint = Color(0xFF10B981).copy(alpha = 0.16f),
    amber = Color(0xFFF59E0B),
    amberTint = Color(0xFFF59E0B).copy(alpha = 0.18f),
    danger = Color(0xFFEF4444),
    dangerTint = Color(0xFFEF4444).copy(alpha = 0.18f),
    pixTeal = Color(0xFF00B494),
    uberBadgeBg = Color(0xFF0F172A),
    uberBadgeFg = Color.White,
    toggleOfflineBg = Color(0xFF334155)
)

private val SunlightHighContrastPalette = CockpitPalette(
    isNight = false,
    background = SunlightOffWhite,
    surface = SunlightPureWhite,
    cardSurface = Color(0xFFEFF3F8),
    border = SunlightBorderHeavy,
    borderWidth = 1.5.dp,
    textPrimary = SunlightTextPrimary,
    textSecondary = SunlightTextSecondary,
    textMuted = SunlightTextMuted,
    emerald = SunlightEmeraldGreen,
    emeraldTint = Color(0xFFD1FADF),
    amber = SunlightAmberWarning,
    amberTint = Color(0xFFFEF3C7),
    danger = SunlightRedAlert,
    dangerTint = Color(0xFFFEE2E2),
    pixTeal = Color(0xFF00796B),
    uberBadgeBg = Color(0xFF000000),
    uberBadgeFg = Color.White,
    toggleOfflineBg = Color(0xFF374151)
)

val LocalCockpitPalette = staticCompositionLocalOf { NightCockpitPalette }

private val IFoodRed = Color(0xFFEA1D2C)
private val NinetyNineYellow = Color(0xFFF59E0B)
private val RappiOrange = Color(0xFFFF5722)

enum class StartupTab(val label: String, val icon: ImageVector) {
    COCKPIT("Radar", Icons.Default.Navigation),
    COMBOS("Mesclagens", Icons.Default.AltRoute),
    FINANCE("Finanças", Icons.AutoMirrored.Filled.TrendingUp)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartupDeliveryCockpitScreen(
    voiceManager: NeuralVoiceManager? = null,
    onOpenEmergencyDialog: () -> Unit = {},
    isNightMode: Boolean = true,
    onToggleNightMode: () -> Unit = {},
    cloudViewModel: RiderCloudViewModel? = null,
    userEmail: String? = null,
    onSignOut: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val activePalette = if (isNightMode) NightCockpitPalette else SunlightHighContrastPalette

    CompositionLocalProvider(LocalCockpitPalette provides activePalette) {
        val palette = LocalCockpitPalette.current

        var selectedTab by remember { mutableStateOf(StartupTab.COCKPIT) }
        var isDriverOnline by remember { mutableStateOf(true) }
        var isVoiceActive by remember { mutableStateOf(voiceManager?.isMuted != true) }

        // Modais ergonômicos
        var showFilterSheet by remember { mutableStateOf(false) }
        var showSosConfirmDialog by remember { mutableStateOf(false) }

        // Métricas financeiras em tempo real
        var todayEarnings by remember { mutableDoubleStateOf(284.50) }
        var completedTrips by remember { mutableIntStateOf(18) }
        val onlineHours by remember { mutableStateOf("6h 40m") }
        val currentRatePerKm by remember { mutableDoubleStateOf(5.85) }

        // Critérios ativos de filtro de R$/km (sincronizados com Firestore quando autenticado)
        var minGainPerKm by remember { mutableFloatStateOf(4.50f) }
        var minOfferValue by remember { mutableFloatStateOf(15.0f) }
        var hideBelowThreshold by remember { mutableStateOf(false) }
        var autoAcceptEnabled by remember { mutableStateOf(false) }

        val cloudProfileState = cloudViewModel?.profileState?.collectAsStateWithLifecycle()?.value
        LaunchedEffect(cloudProfileState) {
            if (cloudProfileState is CloudUiState.Success) {
                cloudProfileState.data?.let { profile ->
                    minGainPerKm = profile.minGainPerKm.toFloat()
                    minOfferValue = profile.minOfferValue.toFloat()
                }
            }
        }

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
                        else -> RappiOrange
                    }
                    activeOffers.add(
                        0,
                        StartupOffer(
                            id = radarOffer.id,
                            appName = radarOffer.appName,
                            appColor = appColor,
                            restaurant = radarOffer.restaurant,
                            pickupAddress = radarOffer.pickupAddress.ifEmpty {
                                "${radarOffer.restaurant}, São Paulo"
                            },
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

        val displayedOffers by remember(hideBelowThreshold, minGainPerKm, minOfferValue) {
            derivedStateOf {
                if (!hideBelowThreshold) {
                    activeOffers.toList()
                } else {
                    activeOffers.filter {
                        it.ratePerKm >= minGainPerKm && it.payout >= minOfferValue
                    }
                }
            }
        }

        val acceptOfferAction: (StartupOffer) -> Unit = { offer ->
            val currentSpeed = LocationService.globalLocationState.value.currentSpeedKmh
            val isLocked =
                LocationService.globalLocationState.value.isSafetyLockActive || currentSpeed > 10.0
            if (isLocked) {
                HapticFeedbackHelper.vibrateDecline(context)
                Toast.makeText(
                    context,
                    "🚨 Toque bloqueado por segurança: moto em movimento (${String.format(Locale("pt", "BR"), "%.1f", currentSpeed)} km/h)",
                    Toast.LENGTH_SHORT
                ).show()
                if (isVoiceActive) voiceManager?.speak("Toque bloqueado por segurança. Moto em movimento.")
            } else {
                HapticFeedbackHelper.vibrateAccept(context)
                activeOffers.remove(offer)
                todayEarnings += offer.payout
                completedTrips += 1
                try {
                    DailyGoalTrackerManager.getInstance(context)
                        .recordDeliveryEarnings(offer.payout, offer.distanceKm * 0.17)
                } catch (_: Exception) {
                }
                cloudViewModel?.syncAcceptedDelivery(
                    appSource = offer.appName,
                    restaurant = offer.restaurant,
                    payoutBrl = offer.payout,
                    distanceKm = offer.distanceKm,
                    merged = offer.isChained
                )
                Toast.makeText(
                    context,
                    "Corrida aceita no ${offer.appName} (+R$ ${String.format(Locale("pt", "BR"), "%.2f", offer.payout)})",
                    Toast.LENGTH_LONG
                ).show()
                if (isVoiceActive) {
                    voiceManager?.speak("Corrida aceita. Navegando para ${offer.restaurant}.")
                }
            }
        }

        // Diálogo de confirmação SOS (evita disparo acidental com luva/trepidação)
        if (showSosConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showSosConfirmDialog = false },
                containerColor = palette.surface,
                title = {
                    Text(
                        text = "🚨 Confirmar SOS Emergência?",
                        color = palette.danger,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp
                    )
                },
                text = {
                    Text(
                        text = "Ao confirmar, o alerta de emergência será acionado com sua localização atual.",
                        color = palette.textSecondary,
                        fontSize = 13.sp
                    )
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { showSosConfirmDialog = false },
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("sos_cancel_button")
                    ) {
                        Text("Cancelar", color = palette.textPrimary)
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showSosConfirmDialog = false
                            HapticFeedbackHelper.vibrateBlitzUrgent(context)
                            onOpenEmergencyDialog()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = palette.danger),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("sos_confirm_button")
                    ) {
                        Text("Acionar SOS", color = Color.White, fontWeight = FontWeight.Black)
                    }
                }
            )
        }

        // Bottom Sheet de Filtros de Rentabilidade (reintegrado na aba Radar sem ocupar aba fixa)
        if (showFilterSheet) {
            ModalBottomSheet(
                onDismissRequest = { showFilterSheet = false },
                containerColor = palette.surface
            ) {
                ProfitabilityFiltersSheetContent(
                    minGainPerKm = minGainPerKm,
                    onMinGainChange = { minGainPerKm = it },
                    minOfferValue = minOfferValue,
                    onMinOfferValueChange = { minOfferValue = it },
                    hideBelowThreshold = hideBelowThreshold,
                    onToggleHideBelow = { hideBelowThreshold = it },
                    autoAcceptEnabled = autoAcceptEnabled,
                    onToggleAutoAccept = { autoAcceptEnabled = it },
                    onClose = {
                        showFilterSheet = false
                        cloudViewModel?.saveFiltersToCloud(
                            dailyGoalBrl = 300.0,
                            minGainPerKm = minGainPerKm.toDouble(),
                            minOfferValue = minOfferValue.toDouble()
                        )
                    }
                )
            }
        }

        Scaffold(
            containerColor = palette.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                StartupTopHeader(
                    isOnline = isDriverOnline,
                    onToggleOnline = {
                        isDriverOnline = !isDriverOnline
                        HapticFeedbackHelper.vibrateSuccess(context)
                        if (isDriverOnline) {
                            Toast.makeText(
                                context,
                                "Radar Ativo: Você está online nos 4 apps!",
                                Toast.LENGTH_SHORT
                            ).show()
                            if (isVoiceActive) {
                                voiceManager?.speak("Você está online. Radar ativado buscando as melhores corridas.")
                            }
                        } else {
                            Toast.makeText(
                                context,
                                "Radar em pausa: Você está offline.",
                                Toast.LENGTH_SHORT
                            ).show()
                            if (isVoiceActive) voiceManager?.speak("Radar em pausa.")
                        }
                    },
                    isVoiceActive = isVoiceActive,
                    onToggleVoice = {
                        isVoiceActive = !isVoiceActive
                        voiceManager?.isMuted = !isVoiceActive
                        HapticFeedbackHelper.vibrateTap(context)
                    },
                    isNightMode = isNightMode,
                    onToggleNightMode = {
                        HapticFeedbackHelper.vibrateTap(context)
                        onToggleNightMode()
                    },
                    onOpenEmergency = { showSosConfirmDialog = true },
                    userEmail = userEmail,
                    onSignOut = onSignOut
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
                            minGainPerKm = minGainPerKm,
                            hideBelowThreshold = hideBelowThreshold,
                            offers = displayedOffers,
                            totalUnfilteredCount = activeOffers.size,
                            onOpenFilters = { showFilterSheet = true },
                            onAcceptFcmOffer = { radarOffer ->
                                val offer = StartupOffer(
                                    id = radarOffer.id,
                                    appName = radarOffer.appName,
                                    appColor = when {
                                        radarOffer.appName.contains("iFood", true) -> IFoodRed
                                        radarOffer.appName.contains("99", true) -> NinetyNineYellow
                                        radarOffer.appName.contains("Uber", true) -> Color(0xFF0F172A)
                                        else -> RappiOrange
                                    },
                                    restaurant = radarOffer.restaurant,
                                    pickupAddress = radarOffer.restaurant,
                                    destinationAddress = "Destino do Cliente",
                                    payout = radarOffer.value,
                                    distanceKm = radarOffer.distanceKm,
                                    estimatedMinutes = radarOffer.estimatedTimeMin,
                                    ratePerKm = radarOffer.gainPerKm
                                )
                                acceptOfferAction(offer)
                            },
                            onAcceptOffer = acceptOfferAction,
                            onDeclineOffer = { offer ->
                                HapticFeedbackHelper.vibrateDecline(context)
                                activeOffers.remove(offer)
                                Toast.makeText(context, "Oferta recusada.", Toast.LENGTH_SHORT).show()
                            },
                            onSimulateNewOffer = {
                                val newOffer = StartupOffer(
                                    id = "sim_${System.currentTimeMillis() % 10000}",
                                    appName = listOf("iFood", "99 Moto", "Uber Direct", "Rappi").random(),
                                    appColor = listOf(
                                        IFoodRed,
                                        NinetyNineYellow,
                                        Color(0xFF0F172A),
                                        RappiOrange
                                    ).random(),
                                    restaurant = listOf(
                                        "Outback Steakhouse",
                                        "McDonald's Berrini",
                                        "Coco Bambu",
                                        "Habib's Rebouças"
                                    ).random(),
                                    pickupAddress = "Av. Eng. Luís Carlos Berrini, 1200",
                                    destinationAddress = "Rua Gomes de Carvalho, 1500 (Vila Olímpia)",
                                    payout = (18..45).random().toDouble(),
                                    distanceKm = (20..65).random() / 10.0,
                                    estimatedMinutes = (10..25).random(),
                                    ratePerKm = (42..85).random() / 10.0,
                                    isChained = (1..2).random() == 1,
                                    chainedBonus = 16.00,
                                    surgeActive = true
                                )
                                activeOffers.add(0, newOffer)
                                HapticFeedbackHelper.vibrateSuccess(context)
                                if (isVoiceActive) {
                                    voiceManager?.speak("Nova corrida de R$ ${newOffer.payout.toInt()} no ${newOffer.appName}.")
                                }
                            }
                        )
                    }

                    StartupTab.COMBOS -> {
                        StartupCombosTab()
                    }

                    StartupTab.FINANCE -> {
                        StartupFinanceAndProTab(
                            todayEarnings = todayEarnings,
                            completedTrips = completedTrips
                        )
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// TOP HEADER ESTILO COCKPIT TÁTICO (48dp+ TOUCH TARGETS & TOGGLE MODO SOL)
// -----------------------------------------------------------------------------
@Composable
private fun StartupTopHeader(
    isOnline: Boolean,
    onToggleOnline: () -> Unit,
    isVoiceActive: Boolean,
    onToggleVoice: () -> Unit,
    isNightMode: Boolean,
    onToggleNightMode: () -> Unit,
    onOpenEmergency: () -> Unit,
    userEmail: String? = null,
    onSignOut: (() -> Unit)? = null
) {
    val palette = LocalCockpitPalette.current

    Surface(
        color = palette.surface,
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = if (palette.isNight) 6.dp else 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Perfil + Status GPS
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(palette.cardSurface)
                                .border(
                                    2.dp,
                                    if (isOnline) palette.emerald else palette.border,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🛵", fontSize = 20.sp)
                        }
                        Box(
                            modifier = Modifier
                                .size(13.dp)
                                .clip(CircleShape)
                                .background(if (isOnline) palette.emerald else palette.danger)
                                .border(2.dp, palette.surface, CircleShape)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Radar Piloto",
                                color = palette.textPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = palette.emeraldTint,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "PRO",
                                    color = palette.emerald,
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
                            "🚨 Trava Velocidade (${String.format(Locale("pt", "BR"), "%.0f", locState.currentSpeedKmh)} km/h)"
                        } else if (locState.isTracking) {
                            "GPS ±${String.format(Locale("pt", "BR"), "%.1f", locState.accuracyMeters.coerceAtLeast(1.0f))}m • 4 Apps"
                        } else {
                            "GPS Pronto • 4 Apps Conectados"
                        }
                        Text(
                            text = gpsSubtitle,
                            color = if (locState.isSafetyLockActive && isOnline) palette.danger else palette.textSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (locState.isSafetyLockActive && isOnline) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Ações do Topo (48dp mínimos; SOS separado visualmente com borda de aviso)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = onToggleNightMode,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (isNightMode) palette.cardSurface else palette.amberTint)
                            .border(palette.borderWidth, palette.border, CircleShape)
                            .testTag("sunlight_mode_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isNightMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = if (isNightMode) "Ativar Modo Sol Alto Contraste" else "Ativar Modo Noturno OLED",
                            tint = if (isNightMode) palette.amber else palette.textPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    IconButton(
                        onClick = onToggleVoice,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(palette.cardSurface)
                            .border(palette.borderWidth, palette.border, CircleShape)
                            .testTag("voice_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isVoiceActive) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = if (isVoiceActive) "Silenciar Voz" else "Ativar Voz",
                            tint = if (isVoiceActive) palette.emerald else palette.textMuted,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(2.dp))

                    IconButton(
                        onClick = onOpenEmergency,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(palette.dangerTint)
                            .border(1.5.dp, palette.danger.copy(alpha = 0.7f), CircleShape)
                            .testTag("emergency_sos_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "SOS Emergência",
                            tint = palette.danger,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    if (onSignOut != null) {
                        IconButton(
                            onClick = onSignOut,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(palette.cardSurface)
                                .border(palette.borderWidth, palette.border, CircleShape)
                                .testTag("sign_out_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = if (!userEmail.isNullOrBlank()) "Sair de $userEmail" else "Sair da conta Google",
                                tint = palette.textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val toggleBgColor by animateColorAsState(
                targetValue = if (isOnline) palette.emerald else palette.toggleOfflineBg,
                animationSpec = tween(250),
                label = "onlineToggleColor"
            )

            Surface(
                onClick = onToggleOnline,
                color = toggleBgColor,
                shape = RoundedCornerShape(14.dp),
                border = if (!palette.isNight) BorderStroke(1.5.dp, palette.border) else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
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
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp
                        )
                    }

                    Text(
                        text = if (isOnline) "Toque para Pausar" else "Toque para Conectar",
                        color = Color.White.copy(alpha = 0.92f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// ABA 1: RADAR COCKPIT (SEM BANNERS DUPLICADOS + FCM VIVO + FILTROS R$/KM)
// -----------------------------------------------------------------------------
@Composable
private fun StartupCockpitTab(
    isOnline: Boolean,
    todayEarnings: Double,
    completedTrips: Int,
    onlineHours: String,
    ratePerKm: Double,
    minGainPerKm: Float,
    hideBelowThreshold: Boolean,
    offers: List<StartupOffer>,
    totalUnfilteredCount: Int,
    onOpenFilters: () -> Unit,
    onAcceptFcmOffer: (RadarOffer) -> Unit,
    onAcceptOffer: (StartupOffer) -> Unit,
    onDeclineOffer: (StartupOffer) -> Unit,
    onSimulateNewOffer: () -> Unit
) {
    val palette = LocalCockpitPalette.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 14.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 0. Alertas em Tempo Real do FCM (Blitz Policial, Combos Multi-App e Push)
        item {
            FcmRealtimeOfferAlertCard(onAcceptOffer = onAcceptFcmOffer)
        }

        // 1. Resumo Financeiro Compacto no Topo
        item {
            StartupDailySummaryCard(
                todayEarnings = todayEarnings,
                completedTrips = completedTrips,
                onlineHours = onlineHours,
                ratePerKm = ratePerKm
            )
        }

        // 2. Barramento dos 4 Apps Integrados
        item {
            StartupMultiAppStatusBar()
        }

        // 3. Cabeçalho de Chamadas + Filtro Ativo + Simulador Discreto
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Chamadas",
                        color = palette.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = palette.cardSurface,
                        shape = CircleShape,
                        border = BorderStroke(palette.borderWidth, palette.border)
                    ) {
                        Text(
                            text = "${offers.size}",
                            color = palette.emerald,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Chip de Filtro R$/km (48dp touch target)
                    Surface(
                        onClick = onOpenFilters,
                        color = if (hideBelowThreshold) palette.emeraldTint else palette.surface,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            palette.borderWidth,
                            if (hideBelowThreshold) palette.emerald else palette.border
                        ),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("filter_cockpit_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Configurar Filtros de Rentabilidade",
                                tint = palette.emerald,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = String.format(Locale("pt", "BR"), "≥ R$ %.1f/km", minGainPerKm),
                                color = palette.textPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Botão de Simulação Rápida (48dp)
                    OutlinedButton(
                        onClick = onSimulateNewOffer,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(palette.borderWidth, palette.border),
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("simulate_offer_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Simular Nova Oferta",
                            tint = palette.emerald,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // 4. Lista de Ofertas Interceptadas
        if (offers.isEmpty()) {
            item {
                StartupEmptyStateCard(
                    isOnline = isOnline,
                    hiddenByFilterCount = if (hideBelowThreshold) totalUnfilteredCount else 0,
                    onOpenFilters = onOpenFilters,
                    onSimulate = onSimulateNewOffer
                )
            }
        } else {
            items(offers, key = { it.id }) { offer ->
                StartupDeliveryOfferCard(
                    offer = offer,
                    minGainPerKm = minGainPerKm,
                    onAccept = { onAcceptOffer(offer) },
                    onDecline = { onDeclineOffer(offer) }
                )
            }
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
    val palette = LocalCockpitPalette.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("earnings_summary_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = palette.surface),
        border = BorderStroke(palette.borderWidth, palette.border),
        elevation = CardDefaults.cardElevation(defaultElevation = if (palette.isNight) 2.dp else 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "GANHO DE HOJE",
                    color = palette.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Surface(
                    color = palette.emeraldTint,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = null,
                            tint = palette.emerald,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "+18% vs ontem",
                            color = palette.emerald,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = String.format(Locale("pt", "BR"), "R$ %.2f", todayEarnings),
                color = palette.textPrimary,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = palette.border.copy(alpha = 0.4f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

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
    val palette = LocalCockpitPalette.current

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(palette.cardSurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = palette.textSecondary,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(
                text = value,
                color = palette.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = label,
                color = palette.textSecondary,
                fontSize = 11.sp
            )
        }
    }
}

// -----------------------------------------------------------------------------
// STATUS DOS 4 APPS CONECTADOS
// -----------------------------------------------------------------------------
@Composable
private fun StartupMultiAppStatusBar() {
    val palette = LocalCockpitPalette.current

    Surface(
        color = palette.surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(palette.borderWidth, palette.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "RADAR MULTI-APPS INTEGRADO",
                color = palette.textSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StartupAppBadge(name = "iFood", accentColor = IFoodRed)
                StartupAppBadge(name = "99 Moto", accentColor = NinetyNineYellow)
                StartupAppBadge(name = "Uber", accentColor = palette.textPrimary)
                StartupAppBadge(name = "Rappi", accentColor = RappiOrange)
            }
        }
    }
}

@Composable
private fun StartupAppBadge(name: String, accentColor: Color) {
    val palette = LocalCockpitPalette.current

    Surface(
        color = palette.cardSurface,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(palette.emerald)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = name,
                color = palette.textPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// -----------------------------------------------------------------------------
// CARD DE CORRIDA (BOTÕES LUVA 56dp + BADGE R$/KM DE LEITURA PERIFÉRICA)
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
    minGainPerKm: Float,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    val palette = LocalCockpitPalette.current
    val meetsTarget = offer.ratePerKm >= minGainPerKm
    val rateColor = if (meetsTarget) palette.emerald else palette.amber
    val rateTint = if (meetsTarget) palette.emeraldTint else palette.amberTint

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("delivery_offer_card_${offer.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = palette.surface),
        border = BorderStroke(
            if (offer.isChained || !palette.isNight) 2.dp else 1.dp,
            if (offer.isChained) palette.emerald else palette.border
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Cabeçalho: App de Origem + Valor & R$/km em destaque absoluto
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val badgeBg =
                        if (offer.appName.contains("Uber", true)) palette.uberBadgeBg else offer.appColor
                    val badgeFg =
                        if (offer.appName.contains("99", true)) Color.Black else Color.White

                    Surface(
                        color = badgeBg,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = offer.appName,
                            color = badgeFg,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }

                    if (offer.surgeActive) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = palette.amberTint,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "🔥 Tarifa Alta",
                                color = palette.amber,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = String.format(Locale("pt", "BR"), "R$ %.2f", offer.payout),
                        color = palette.emerald,
                        fontSize = 27.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Surface(
                        color = rateTint,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, rateColor.copy(alpha = 0.8f))
                    ) {
                        Text(
                            text = String.format(Locale("pt", "BR"), "R$ %.2f/km", offer.ratePerKm),
                            color = rateColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            if (offer.isChained) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = palette.emeraldTint,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, palette.emerald.copy(alpha = 0.5f))
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
                            color = palette.emerald,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Timeline Coleta -> Entrega
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(palette.emerald)
                    )
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(34.dp)
                            .background(palette.border.copy(alpha = 0.6f))
                    )
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(palette.danger)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = offer.restaurant,
                        color = palette.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = offer.pickupAddress,
                        color = palette.textSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Destino do Cliente",
                        color = palette.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = offer.destinationAddress,
                        color = palette.textSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Distância & Tempo
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.cardSurface, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Distância total: ${offer.distanceKm} km",
                    color = palette.textPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "~${offer.estimatedMinutes} minutos",
                    color = palette.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Botões Padrão Luva (56.dp): Recusar à esquerda, Aceitar gigante na zona do polegar direito
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDecline,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.danger),
                    border = BorderStroke(1.5.dp, palette.danger.copy(alpha = 0.7f)),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .testTag("decline_offer_button_${offer.id}")
                ) {
                    Text(
                        text = "Recusar",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onAccept,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = palette.emerald),
                    modifier = Modifier
                        .weight(2.2f)
                        .height(56.dp)
                        .testTag("accept_offer_button_${offer.id}")
                ) {
                    Text(
                        text = "ACEITAR CORRIDA",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// ESTADO VAZIO (INCLUI AVISO QUANDO OFERTAS ESTÃO OCULTAS PELO FILTRO)
// -----------------------------------------------------------------------------
@Composable
private fun StartupEmptyStateCard(
    isOnline: Boolean,
    hiddenByFilterCount: Int,
    onOpenFilters: () -> Unit,
    onSimulate: () -> Unit
) {
    val palette = LocalCockpitPalette.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = palette.surface),
        border = BorderStroke(palette.borderWidth, palette.border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(if (hiddenByFilterCount > 0) "🛡️" else "📡", fontSize = 40.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = when {
                    hiddenByFilterCount > 0 -> "$hiddenByFilterCount oferta(s) abaixo do seu piso"
                    isOnline -> "Monitorando Chamadas em Tempo Real"
                    else -> "Radar em Pausa"
                },
                color = palette.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = when {
                    hiddenByFilterCount > 0 -> "O filtro automático está protegendo seu R$/km. Toque em Filtros se quiser ajustar."
                    isOnline -> "Aguardando notificações dos apps iFood, 99, Uber e Rappi."
                    else -> "Ative o botão online acima para começar a interceptar as melhores chamadas."
                },
                color = palette.textSecondary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (hiddenByFilterCount > 0) {
                    OutlinedButton(
                        onClick = onOpenFilters,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("Ajustar Filtros", color = palette.emerald, fontWeight = FontWeight.Bold)
                    }
                }
                Button(
                    onClick = onSimulate,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = palette.cardSurface),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Simular Chamada", color = palette.emerald, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// MODAL SHEET: FILTROS DE RENTABILIDADE R$/KM
// -----------------------------------------------------------------------------
@Composable
private fun ProfitabilityFiltersSheetContent(
    minGainPerKm: Float,
    onMinGainChange: (Float) -> Unit,
    minOfferValue: Float,
    onMinOfferValueChange: (Float) -> Unit,
    hideBelowThreshold: Boolean,
    onToggleHideBelow: (Boolean) -> Unit,
    autoAcceptEnabled: Boolean,
    onToggleAutoAccept: (Boolean) -> Unit,
    onClose: () -> Unit
) {
    val palette = LocalCockpitPalette.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Filtros de Rentabilidade",
            color = palette.textPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black
        )
        Text(
            text = "Defina seu piso mínimo de R$/km para nunca rodar no prejuízo.",
            color = palette.textSecondary,
            fontSize = 12.sp
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = palette.cardSurface),
            border = BorderStroke(palette.borderWidth, palette.border)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Ganho Mínimo por Km", color = palette.textPrimary, fontWeight = FontWeight.Bold)
                    Text(
                        text = String.format(Locale("pt", "BR"), "R$ %.2f / km", minGainPerKm),
                        color = palette.emerald,
                        fontWeight = FontWeight.Black
                    )
                }
                Slider(
                    value = minGainPerKm,
                    onValueChange = onMinGainChange,
                    valueRange = 2.0f..10.0f,
                    steps = 15,
                    colors = SliderDefaults.colors(
                        thumbColor = palette.emerald,
                        activeTrackColor = palette.emerald
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Valor Mínimo da Corrida", color = palette.textPrimary, fontWeight = FontWeight.Bold)
                    Text(
                        text = String.format(Locale("pt", "BR"), "R$ %.2f", minOfferValue),
                        color = palette.emerald,
                        fontWeight = FontWeight.Black
                    )
                }
                Slider(
                    value = minOfferValue,
                    onValueChange = onMinOfferValueChange,
                    valueRange = 8.0f..40.0f,
                    steps = 15,
                    colors = SliderDefaults.colors(
                        thumbColor = palette.emerald,
                        activeTrackColor = palette.emerald
                    )
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(palette.cardSurface)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Ocultar corridas abaixo do piso", color = palette.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("Mostra apenas chamadas que batem seu R$/km.", color = palette.textSecondary, fontSize = 11.sp)
            }
            Switch(
                checked = hideBelowThreshold,
                onCheckedChange = onToggleHideBelow,
                colors = SwitchDefaults.colors(checkedTrackColor = palette.emerald)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(palette.cardSurface)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Prioridade de Voz para Corridas VIP", color = palette.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("Anuncia imediatamente no fone ofertas acima de R$ 7,00/km.", color = palette.textSecondary, fontSize = 11.sp)
            }
            Switch(
                checked = autoAcceptEnabled,
                onCheckedChange = onToggleAutoAccept,
                colors = SwitchDefaults.colors(checkedTrackColor = palette.emerald)
            )
        }

        Button(
            onClick = onClose,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = palette.emerald),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text("Aplicar Filtros", color = Color.White, fontWeight = FontWeight.Black)
        }
    }
}

// -----------------------------------------------------------------------------
// ABA 2: MESCLAGENS & ROTAS TÁTICAS
// -----------------------------------------------------------------------------
@Composable
private fun StartupCombosTab() {
    val palette = LocalCockpitPalette.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            DeepOrderPoolCockpitCard()
        }

        item {
            Surface(
                color = palette.surface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(palette.borderWidth, palette.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("💡", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Dica do Copiloto Neural",
                            color = palette.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Combos com desvio angular menor que 15° garantem mais de R$ 50/hora sem atrasar pedidos.",
                            color = palette.textSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// ABA 3: FINANÇAS & PLANO PRO (LUCRO LÍQUIDO, METAS E CHECKOUT PIX)
// -----------------------------------------------------------------------------
@Composable
private fun StartupFinanceAndProTab(
    todayEarnings: Double,
    completedTrips: Int
) {
    val palette = LocalCockpitPalette.current
    var showCheckoutModal by remember { mutableStateOf(false) }

    if (showCheckoutModal) {
        AlertDialog(
            onDismissRequest = { showCheckoutModal = false },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { showCheckoutModal = false },
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Fechar", color = palette.textSecondary, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = palette.surface,
            title = {
                Text(
                    text = "👑 Assinatura PRO",
                    color = palette.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Box(modifier = Modifier.heightIn(max = 460.dp)) {
                    StartupFintechCheckoutScreen(
                        onPaymentSuccess = { showCheckoutModal = false }
                    )
                }
            }
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
            StartupProFeatureBanner(onUpgrade = { showCheckoutModal = true })
        }

        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = palette.surface),
                border = BorderStroke(palette.borderWidth, palette.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "FATURAMENTO & LUCRO REAL",
                        color = palette.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = String.format(Locale("pt", "BR"), "R$ %.2f", todayEarnings),
                        color = palette.textPrimary,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    val fuelCostEstimate = completedTrips * 3.8 * 0.17
                    val netProfitReal = (todayEarnings - fuelCostEstimate).coerceAtLeast(0.0)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Lucro Líquido (Bolso)", color = palette.textSecondary, fontSize = 11.sp)
                            Text(
                                String.format(Locale("pt", "BR"), "R$ %.2f", netProfitReal),
                                color = palette.emerald,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Gasto Gasolina (Est.)", color = palette.textSecondary, fontSize = 11.sp)
                            Text(
                                String.format(Locale("pt", "BR"), "-R$ %.2f", fuelCostEstimate),
                                color = palette.danger,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = palette.border.copy(alpha = 0.4f), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Meta Diária: R$ ${String.format(Locale("pt", "BR"), "%.2f", todayEarnings)} de R$ 350,00",
                        color = palette.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { (todayEarnings / 350.0).toFloat().coerceIn(0f, 1f) },
                        color = palette.emerald,
                        trackColor = palette.cardSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                    )
                }
            }
        }

        item {
            Text(
                text = "Histórico de Corridas Recentes",
                color = palette.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        items(
            listOf(
                HistoryItem("iFood", IFoodRed, "Madero Prime Faria Lima", 38.50, "13:42", "5.1 km"),
                HistoryItem("99 Moto", NinetyNineYellow, "McDonald's Rebouças", 22.00, "13:05", "3.2 km"),
                HistoryItem("iFood", IFoodRed, "Habib's Teodoro", 17.50, "12:28", "2.4 km"),
                HistoryItem("Uber Flash", Color(0xFF0F172A), "Droga Raia Jardins", 26.00, "11:50", "3.8 km"),
                HistoryItem("Rappi", RappiOrange, "Starbucks Frei Caneca", 29.50, "11:15", "4.0 km")
            )
        ) { item ->
            StartupHistoryDeliveryRow(item)
        }
    }
}

@Composable
private fun StartupProFeatureBanner(onUpgrade: () -> Unit) {
    val palette = LocalCockpitPalette.current

    Surface(
        onClick = onUpgrade,
        color = palette.cardSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, palette.pixTeal.copy(alpha = 0.7f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("upgrade_pro_banner")
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(palette.pixTeal.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text("👑", fontSize = 22.sp)
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Acesso Ilimitado ao Plano PRO",
                    color = palette.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Rotas duplas, cálculo automático no fone e filtros de R$/km.",
                    color = palette.textSecondary,
                    fontSize = 11.sp
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Abrir Planos PRO",
                tint = palette.pixTeal
            )
        }
    }
}

@Composable
private fun StartupHistoryDeliveryRow(item: HistoryItem) {
    val palette = LocalCockpitPalette.current

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = palette.surface),
        border = BorderStroke(palette.borderWidth, palette.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    color = item.color,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = item.app.take(1),
                            color = if (item.color == NinetyNineYellow) Color.Black else Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = item.restaurant,
                        color = palette.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${item.time} • ${item.distance}",
                        color = palette.textSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            Text(
                text = String.format(Locale("pt", "BR"), "+R$ %.2f", item.amount),
                color = palette.emerald,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
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
// BARRA DE NAVEGAÇÃO INFERIOR (3 ABAS COM RESPEITO A WINDOW INSETS)
// -----------------------------------------------------------------------------
@Composable
private fun StartupBottomNavigationBar(
    currentTab: StartupTab,
    onSelectTab: (StartupTab) -> Unit
) {
    val palette = LocalCockpitPalette.current

    NavigationBar(
        containerColor = palette.surface,
        tonalElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        StartupTab.values().forEach { tab ->
            val isSelected = currentTab == tab
            NavigationBarItem(
                selected = isSelected,
                onClick = { onSelectTab(tab) },
                modifier = Modifier.testTag("bottom_tab_${tab.name.lowercase()}"),
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
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = palette.emerald,
                    selectedTextColor = palette.emerald,
                    unselectedIconColor = palette.textSecondary,
                    unselectedTextColor = palette.textSecondary,
                    indicatorColor = palette.emeraldTint
                )
            )
        }
    }
}
