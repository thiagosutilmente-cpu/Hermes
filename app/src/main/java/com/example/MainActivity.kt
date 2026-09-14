package com.example

import com.example.radar.data.RadarCacheRepository
import com.example.radar.data.CachedOfferEntity
import com.example.radar.data.CachedRouteEntity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
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
// ACTIVITY PRINCIPAL
// ----------------------------------------------------
class MainActivity : ComponentActivity() {
    private var voiceManager: NeuralVoiceManager? = null
    private var currentIntentState = mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        currentIntentState.value = intent
        val vm = NeuralVoiceManager(this)
        voiceManager = vm

        setContent {
            DeliveryHighContrastTheme {
                RadarDeliveryDashboard(
                    voiceManager = vm,
                    notificationIntent = currentIntentState.value,
                    onIntentConsumed = { currentIntentState.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        currentIntentState.value = intent
    }

    override fun onDestroy() {
        super.onDestroy()
        voiceManager?.shutdown()
    }
}

// ----------------------------------------------------
// TELA PRINCIPAL: RADAR DELIVERY COCKPIT
// ----------------------------------------------------
@Preview(showBackground = true, showSystemUi = true)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadarDeliveryDashboard(
    voiceManager: NeuralVoiceManager? = null,
    notificationIntent: Intent? = null,
    onIntentConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Gerenciador de Notificações Locais em Segundo Plano
    val localNotificationManager = remember { LocalNotificationManager(context) }

    // Rastreamento do Ciclo de Vida (App em Segundo Plano vs Primeiro Plano)
    var isAppInBackground by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> isAppInBackground = true
                Lifecycle.Event.ON_START -> isAppInBackground = false
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Permissão de Notificações (Android 13+ / POST_NOTIFICATIONS)
    var hasNotificationPermission by remember {
        mutableStateOf(localNotificationManager.hasNotificationPermission())
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
        if (granted) {
            Toast.makeText(context, "Notificações em segundo plano ativadas!", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // 1. Estado do Radar e Despacho Ativo
    var isTrackingActive by remember { mutableStateOf(true) }
    var isAutoDispatchActive by remember { mutableStateOf(true) }
    var isVoiceEnabled by remember { mutableStateOf(voiceManager?.isMuted != true) }

    // Estado da Tela de Perfil do Entregador
    var showProfileScreen by remember { mutableStateOf(false) }

    // Estado do Tour Guiado (Onboarding) na primeira execução do app
    var showOnboardingTour by remember {
        mutableStateOf(!OnboardingPreferencesManager.hasCompletedOnboarding(context))
    }

    // Inicialização do Gerenciador de Assinaturas Pro e Google Play Billing
    LaunchedEffect(Unit) {
        SubscriptionManager.initialize(context)
        PlayBillingManager.initialize(context)
        FirebaseAnalyticsManager.initialize(context)
        FirebaseAnalyticsManager.logScreenView("RadarDeliveryDashboard", "MainActivity")
    }
    val subscriptionState by SubscriptionManager.subscriptionState.collectAsState()
    var showSubscriptionPaywall by remember { mutableStateOf(false) }
    var showAnalyticsDashboard by remember { mutableStateOf(false) }

    // Inicialização do Gerenciador de Logs de Decisões do Entregador
    LaunchedEffect(Unit) {
        OfferDecisionLogManager.initialize(context)
    }

    // 2. Métricas Financeiras e de Quilometragem
    var todayEarnings by remember { mutableDoubleStateOf(284.50) }
    var completedDeliveries by remember { mutableIntStateOf(18) }
    var scannedOffersCount by remember { mutableIntStateOf(52) }
    var totalKmDriven by remember { mutableDoubleStateOf(48.6) }

    // 3. Configuração de Combustível da Motocicleta
    var fuelConfig by remember { mutableStateOf(FuelConfig(kmPerLiter = 35.0, fuelPricePerLiter = 5.89)) }

    // 4. Modo Foco em Trânsito (HUD)
    var isFocusModeActive by remember { mutableStateOf(false) }

    // 5. Telemetria e Saúde Neural do Backend
    var systemHealth by remember { mutableStateOf(SystemHealthData()) }

    // 6. Histórico de Entregas Concluídas Aceitas pelo Radar AI
    val completedDeliveriesList = remember {
        mutableStateListOf(
            CompletedDeliveryItem("c1", "BK Paulista", "iFood", 33.0, 4.2, 14, 28.5, "12:15"),
            CompletedDeliveryItem("c2", "Pizza Hut Jardins", "Rappi", 18.0, 2.4, 11, 15.6, "11:40"),
            CompletedDeliveryItem("c3", "Starbucks Frei Caneca", "iFood + Rappi", 26.5, 3.1, 12, 22.8, "11:05"),
            CompletedDeliveryItem("c4", "McDonald's Rebouças", "99 Food", 19.0, 2.8, 10, 16.2, "10:20"),
            CompletedDeliveryItem("c5", "Outback Morumbi", "iFood", 42.0, 6.2, 19, 36.5, "09:45"),
            CompletedDeliveryItem("c6", "Madero Prime Faria Lima", "Uber Direct", 38.0, 5.5, 17, 33.1, "09:10"),
            CompletedDeliveryItem("c7", "Habib's Teodoro", "iFood", 16.5, 2.1, 9, 14.2, "08:40"),
            CompletedDeliveryItem("c8", "Coco Bambu Anália Franco", "iFood", 45.0, 7.0, 22, 39.0, "08:05"),
            CompletedDeliveryItem("c9", "Bullguer Pinheiros", "Rappi", 23.5, 3.5, 13, 20.4, "07:30"),
            CompletedDeliveryItem("c10", "Dona Deôla Pompéia", "Uber Flash", 23.0, 3.2, 12, 19.9, "07:00")
        )
    }

    // 6.1. Cache Local Room para Histórico e Rotas Offline
    val cacheRepository = remember { RadarCacheRepository.getInstance(context) }
    val cachedOffersList by cacheRepository.recentOffers.collectAsState(initial = emptyList())
    val cachedRoutesList by cacheRepository.recentRoutes.collectAsState(initial = emptyList())
    var isOfflineModeSimulated by remember { mutableStateOf(false) }

    // Inicialização do Cache Local Room
    LaunchedEffect(Unit) {
        cacheRepository.seedInitialDataIfEmpty()
    }

    // Atualização periódica da telemetria de saúde com o backend
    LaunchedEffect(Unit) {
        while (isActive) {
            val health = RadarDecisionEngine.fetchSystemHealth()
            systemHealth = health
            delay(15000L)
        }
    }

    // 7. Critérios de Filtragem em Tempo Real (Valor Mínimo, Distância Máxima, Bônus por Entrega, Ganho por Km e Jarvis)
    var filterCriteria by remember {
        mutableStateOf(FilterPreferencesManager.loadCriteria(context))
    }
    var showFilterSettingsModal by remember { mutableStateOf(false) }
    var showFilterSettingsScreen by remember { mutableStateOf(false) }

    // 8. Reconhecedor de Fala Nativo (SpeechRecognizer) - Mãos Livres no Capacete
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        if (granted) {
            speechManager?.startListening()
            Toast.makeText(context, "Microfone liberado! Diga 'Aceitar' ou 'Cancelar'.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Permissão necessária para comando por voz.", Toast.LENGTH_SHORT).show()
        }
    }

    var lastVoiceCommandText by remember { mutableStateOf("") }
    var speechManager: HandsFreeSpeechManager? by remember { mutableStateOf(null) }

    // 9. Monitor de Velocidade e Segurança (LocationService em Background + FusedLocation)
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    var hasBackgroundLocationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val backgroundLocationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasBackgroundLocationPermission = granted
        if (granted) {
            Toast.makeText(context, "Localização contínua em segundo plano autorizada!", Toast.LENGTH_SHORT).show()
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                      perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasLocationPermission = granted
        if (granted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !hasBackgroundLocationPermission) {
            backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    var speedMonitor: SpeedSafetyMonitor? by remember { mutableStateOf(null) }

    DisposableEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            LocationService.start(context)
        }

        val monitor = SpeedSafetyMonitor(context) { isLocked, speed ->
            val limit = FilterPreferencesManager.loadCriteria(context).safetySpeedThresholdKm
            FirebaseAnalyticsManager.logSpeedSafetyAlert(speed, limit = limit)
            if (isLocked) {
                HapticFeedbackHelper.vibrateDecline(context)
                if (isVoiceEnabled) {
                    voiceManager?.speak("Atenção: veículo em movimento acima de ${limit.toInt()} por hora. Trava de segurança ativada usando GPS. Interface desativada.")
                }
            } else {
                HapticFeedbackHelper.vibrateTap(context)
                if (isVoiceEnabled) {
                    voiceManager?.speak("Velocidade abaixo de ${limit.toInt()} por hora. Interface de pedidos liberada.")
                }
            }
        }
        speedMonitor = monitor

        // Inicializa o Geofencing para zonas de alta demanda se permissão estiver concedida
        if (hasLocationPermission) {
            try {
                GeofencingDemandManager.getInstance(context).registerHotspotGeofences()
            } catch (_: Exception) {}
        }

        onDispose {
            monitor.destroy()
        }
    }

    // 4.1. Monitoramento reativo de Geofencing para Zonas de Alta Demanda (Hotspots)
    val geofenceManager = remember { GeofencingDemandManager.getInstance(context) }
    val activeGeofenceZone by GeofencingDemandManager.currentActiveZone.collectAsState()
    val registeredGeofenceZones by GeofencingDemandManager.registeredZones.collectAsState()
    val isGeofencingActive by GeofencingDemandManager.isGeofencingActive.collectAsState()

    // Ouve eventos de transição de Geofence para falar ao entregador e dar feedback tátil
    LaunchedEffect(Unit) {
        GeofencingDemandManager.zoneTransitions.collect { event ->
            if (event.transitionType == com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_ENTER ||
                event.transitionType == com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_DWELL) {
                HapticFeedbackHelper.vibrateAccept(context)
                if (isVoiceEnabled) {
                    voiceManager?.speak("Atenção: você entrou na zona de alta demanda ${event.zone.name}. Tarifa dinâmica ativa com potencial de pedidos acumulados.")
                }
            } else if (event.transitionType == com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_EXIT) {
                HapticFeedbackHelper.vibrateTap(context)
                if (isVoiceEnabled) {
                    voiceManager?.speak("Você saiu da zona de alta demanda ${event.zone.name}.")
                }
            }
        }
    }

    // 5. Lista de Ofertas Interceptadas
    val offersList = remember {
        mutableStateListOf<RadarOffer>().apply {
            addAll(LiveDispatchSimulator.getInitialOffers())
        }
    }

    // 8.1. Oferta em Destaque no Card de Coleta Google Maps
    var selectedPickupOfferId by remember { mutableStateOf<String?>(null) }

    // Ações de Aceitar e Rejeitar reutilizáveis pelo Toque, Card de Coleta e Comando de Voz
    val onAcceptOffer: (RadarOffer, String) -> Unit = { targetOffer, source ->
        // Feedback Háptico Tático de Aceite (Pulso duplo de alta energia)
        HapticFeedbackHelper.vibrateAccept(context)

        // Registro no Sistema Interno de Logs
        OfferDecisionLogManager.logAccept(
            context = context,
            offer = targetOffer,
            reason = targetOffer.neuralDecision.reason.ifEmpty { "Ganho/km vantajoso" },
            source = source
        )

        // Rastreamento de Telemetria no Firebase Analytics
        FirebaseAnalyticsManager.logOfferAccepted(
            offerId = targetOffer.id,
            appName = targetOffer.appName,
            value = targetOffer.value,
            distanceKm = targetOffer.distanceKm,
            gainPerKm = targetOffer.gainPerKm,
            decisionSource = source
        )

        todayEarnings += targetOffer.value
        totalKmDriven += targetOffer.distanceKm
        completedDeliveries++
        completedDeliveriesList.add(
            0,
            CompletedDeliveryItem(
                id = targetOffer.id,
                restaurant = targetOffer.restaurant,
                appSource = targetOffer.appName,
                grossValue = targetOffer.value,
                distanceKm = targetOffer.distanceKm,
                timeMinutes = targetOffer.estimatedTimeMin,
                netProfit = targetOffer.netProfit,
                timestamp = "Agora"
            )
        )
        offersList.remove(targetOffer)
        if (selectedPickupOfferId == targetOffer.id) {
            selectedPickupOfferId = null
        }
        if (isVoiceEnabled && voiceManager != null) {
            voiceManager.announceAccept(targetOffer.restaurant, targetOffer.value)
        }
        coroutineScope.launch {
            RadarDecisionEngine.notifyStackAccepted(targetOffer.id)
            // Persistência Room: Atualiza status da oferta e armazena a rota de entrega
            cacheRepository.updateOfferStatus(targetOffer.id, "ACCEPTED")
            val routeEntity = CachedRouteEntity(
                routeId = "route_${targetOffer.id}_${System.currentTimeMillis() % 10000}",
                offerId = targetOffer.id,
                appName = targetOffer.appName,
                originName = targetOffer.restaurant,
                originAddress = targetOffer.pickupLocation?.address ?: "${targetOffer.restaurant}, São Paulo",
                destinationName = "Cliente Final",
                destinationAddress = "Endereço de Entrega do Cliente, SP",
                totalDistanceKm = targetOffer.distanceKm,
                estimatedMinutes = targetOffer.estimatedTimeMin,
                waypointsSummary = "● Coleta: ${targetOffer.restaurant} ➔ ● Entrega: Cliente",
                completedAt = System.currentTimeMillis(),
                status = "COMPLETED"
            )
            cacheRepository.cacheRoute(routeEntity)
        }
    }

    val onDeclineOffer: (RadarOffer, String) -> Unit = { targetOffer, source ->
        // Feedback Háptico Tático de Recusa (Pulso curto)
        HapticFeedbackHelper.vibrateDecline(context)

        // Registro no Sistema Interno de Logs
        OfferDecisionLogManager.logDecline(
            context = context,
            offer = targetOffer,
            reason = targetOffer.neuralDecision.reason.ifEmpty { "Recusado pelo entregador" },
            source = source
        )

        // Rastreamento de Telemetria no Firebase Analytics
        FirebaseAnalyticsManager.logOfferDeclined(
            offerId = targetOffer.id,
            appName = targetOffer.appName,
            value = targetOffer.value,
            distanceKm = targetOffer.distanceKm,
            reason = targetOffer.neuralDecision.reason.ifEmpty { "Recusado pelo entregador" },
            decisionSource = source
        )

        offersList.remove(targetOffer)
        if (selectedPickupOfferId == targetOffer.id) {
            selectedPickupOfferId = null
        }
        if (isVoiceEnabled && voiceManager != null) {
            voiceManager.announceDecline()
        }
        coroutineScope.launch {
            RadarDecisionEngine.notifyStackDeclined(targetOffer.id)
            // Persistência Room: Marca como recusada no histórico local
            cacheRepository.updateOfferStatus(targetOffer.id, "DECLINED")
        }
    }

    val onAcceptCurrentBestOffer: () -> Unit = {
        val targetOffer = offersList.firstOrNull { filterCriteria.matches(it) } ?: offersList.firstOrNull()
        if (targetOffer != null) {
            onAcceptOffer(targetOffer, "Comando de Voz")
        } else {
            if (isVoiceEnabled && voiceManager != null) {
                voiceManager.speak("Nenhuma oferta pendente no radar no momento.")
            }
        }
    }

    val onDeclineCurrentBestOffer: () -> Unit = {
        val targetOffer = offersList.firstOrNull { filterCriteria.matches(it) } ?: offersList.firstOrNull()
        if (targetOffer != null) {
            onDeclineOffer(targetOffer, "Comando de Voz")
        } else {
            if (isVoiceEnabled && voiceManager != null) {
                voiceManager.speak("Nenhuma oferta pendente para cancelar.")
            }
        }
    }

    // Tratamento de Ações Recebidas de Notificações Locais (Aceitar / Recusar / Abrir)
    LaunchedEffect(notificationIntent) {
        val intent = notificationIntent ?: return@LaunchedEffect
        val action = intent.getStringExtra(LocalNotificationManager.EXTRA_ACTION)
        val offerId = intent.getStringExtra(LocalNotificationManager.EXTRA_OFFER_ID)
        if (action == LocalNotificationManager.ACTION_ACCEPT && offerId != null) {
            val targetOffer = offersList.find { it.id == offerId } ?: offersList.firstOrNull()
            if (targetOffer != null) {
                onAcceptOffer(targetOffer, "Notificação Push")
            }
            localNotificationManager.cancelOfferNotification(offerId)
        } else if (action == LocalNotificationManager.ACTION_DECLINE && offerId != null) {
            val targetOffer = offersList.find { it.id == offerId }
            if (targetOffer != null) {
                onDeclineOffer(targetOffer, "Notificação Push")
            }
            localNotificationManager.cancelOfferNotification(offerId)
        }
        onIntentConsumed()
    }

    // Inicialização do HandsFreeSpeechManager
    DisposableEffect(context) {
        val manager = HandsFreeSpeechManager(context) { command, spokenText ->
            lastVoiceCommandText = spokenText
            FirebaseAnalyticsManager.logVoiceCommandRecognized(command.name, spokenText)
            FirebaseAnalyticsManager.logFeatureUsed("voice_hands_free")
            when (command) {
                VoiceActionCommand.ACCEPT -> {
                    onAcceptCurrentBestOffer()
                }
                VoiceActionCommand.DECLINE -> {
                    onDeclineCurrentBestOffer()
                }
                VoiceActionCommand.READ_OFFER -> {
                    HapticFeedbackHelper.vibrateTap(context)
                    val targetOffer = offersList.firstOrNull { filterCriteria.matches(it) } ?: offersList.firstOrNull()
                    if (targetOffer != null) {
                        voiceManager?.readOfferAloud(
                            appName = targetOffer.appName,
                            restaurant = targetOffer.restaurant,
                            value = targetOffer.value,
                            distanceKm = targetOffer.distanceKm,
                            gainPerKm = targetOffer.gainPerKm,
                            pickupAddress = targetOffer.pickupAddress,
                            estimatedMinutes = targetOffer.estimatedTimeMin,
                            neuralDecision = targetOffer.neuralDecision.decision,
                            neuralReason = targetOffer.neuralDecision.reason
                        )
                    } else {
                        voiceManager?.speak("Nenhuma oferta disponível no radar no momento.")
                    }
                }
                VoiceActionCommand.READ_EARNINGS -> {
                    HapticFeedbackHelper.vibrateTap(context)
                    val netProfit = completedDeliveriesList.sumOf { it.netProfit }
                    voiceManager?.announceEarnings(
                        todayGross = todayEarnings,
                        netProfit = if (netProfit > 0) netProfit else (todayEarnings * 0.78),
                        totalKm = totalKmDriven,
                        deliveryCount = completedDeliveries
                    )
                }
                VoiceActionCommand.OPEN_NAVIGATION -> {
                    HapticFeedbackHelper.vibrateSuccess(context)
                    val targetOffer = offersList.firstOrNull { filterCriteria.matches(it) } ?: offersList.firstOrNull()
                    if (targetOffer != null) {
                        voiceManager?.announceNavigation(targetOffer.restaurant, targetOffer.pickupAddress)
                        launchGoogleMapsNavigation(
                            context = context,
                            origin = null,
                            destination = "${targetOffer.restaurant}, ${targetOffer.pickupAddress}"
                        )
                    } else {
                        voiceManager?.speak("Nenhuma corrida pendente para abrir no mapa.")
                    }
                }
                VoiceActionCommand.READ_HEALTH -> {
                    HapticFeedbackHelper.vibrateTap(context)
                    val acc = speedState.currentAccuracyMeters.let { if (it > 0) it else 4.2f }
                    voiceManager?.announceSystemHealth(
                        score = systemHealth.score,
                        gpsAccuracyMeters = acc,
                        latencyMs = systemHealth.latencyMs
                    )
                }
                VoiceActionCommand.HELP -> {
                    HapticFeedbackHelper.vibrateTap(context)
                    voiceManager?.announceHelp()
                    showOnboardingTour = true
                }
                VoiceActionCommand.FOCUS_ON -> {
                    HapticFeedbackHelper.vibrateTap(context)
                    isFocusModeActive = true
                    voiceManager?.speak("Modo foco no guidão ativado.")
                }
                VoiceActionCommand.FOCUS_OFF -> {
                    HapticFeedbackHelper.vibrateTap(context)
                    isFocusModeActive = false
                    voiceManager?.speak("Modo foco desativado.")
                }
                VoiceActionCommand.RADAR_ON -> {
                    HapticFeedbackHelper.vibrateTap(context)
                    isTrackingActive = true
                    voiceManager?.announceRadarState(true)
                }
                VoiceActionCommand.RADAR_OFF -> {
                    HapticFeedbackHelper.vibrateTap(context)
                    isTrackingActive = false
                    voiceManager?.announceRadarState(false)
                }
                VoiceActionCommand.SEARCH_MERGED -> {
                    HapticFeedbackHelper.vibrateSuccess(context)
                    val newMerged = MergedDeliverySearchEngine.findPairableSynergy(offersList).firstOrNull()
                        ?: MergedDeliverySearchEngine.generateRealtimeMergedStack()
                    if (!offersList.any { it.id == newMerged.id }) {
                        offersList.add(0, newMerged)
                    }
                    filterCriteria = filterCriteria.copy(onlyMultiStack = true)
                    FilterPreferencesManager.saveCriteria(context, filterCriteria)
                    voiceManager?.speak("Varredura neural concluída. Entrega mesclada com alta sinergia identificada.")
                }
                VoiceActionCommand.FILTER_RAIN_PRESET -> {
                    HapticFeedbackHelper.vibrateSuccess(context)
                    filterCriteria = OfferFilterCriteria(
                        minValue = 22.0,
                        maxDistanceKm = 5.0,
                        minGainPerKm = 6.0,
                        minDeliveryBonus = 5.0,
                        onlyAcceptedNeural = true
                    )
                    FilterPreferencesManager.saveCriteria(context, filterCriteria)
                    voiceManager?.speak("Filtro chuva e alta demanda ativado. Mínimo vinte e dois reais.")
                }
                VoiceActionCommand.FILTER_SHORT_PRESET -> {
                    HapticFeedbackHelper.vibrateSuccess(context)
                    filterCriteria = OfferFilterCriteria(
                        minValue = 12.0,
                        maxDistanceKm = 3.5,
                        minGainPerKm = 5.0,
                        minDeliveryBonus = 0.0,
                        onlyAcceptedNeural = false
                    )
                    FilterPreferencesManager.saveCriteria(context, filterCriteria)
                    voiceManager?.speak("Filtro tiro curto ativado. Raio máximo de três quilômetros e meio.")
                }
                VoiceActionCommand.FILTER_MAX_PROFIT_PRESET -> {
                    HapticFeedbackHelper.vibrateSuccess(context)
                    filterCriteria = OfferFilterCriteria(
                        minValue = 30.0,
                        maxDistanceKm = 7.0,
                        minGainPerKm = 7.0,
                        minDeliveryBonus = 8.0,
                        onlyAcceptedNeural = true
                    )
                    FilterPreferencesManager.saveCriteria(context, filterCriteria)
                    voiceManager?.speak("Filtro máximo lucro ativado. Mínimo trinta reais e sete por quilômetro.")
                }
                VoiceActionCommand.FILTER_RESET -> {
                    HapticFeedbackHelper.vibrateTap(context)
                    filterCriteria = OfferFilterCriteria()
                    FilterPreferencesManager.saveCriteria(context, filterCriteria)
                    voiceManager?.speak("Filtros redefinidos. Exibindo todas as entregas disponíveis.")
                }
                VoiceActionCommand.FILTER_ONLY_MERGED -> {
                    HapticFeedbackHelper.vibrateTap(context)
                    filterCriteria = filterCriteria.copy(onlyMultiStack = true)
                    FilterPreferencesManager.saveCriteria(context, filterCriteria)
                    voiceManager?.speak("Filtro ativado: exibindo apenas entregas mescladas.")
                }
                VoiceActionCommand.FILTER_ONLY_JARVIS -> {
                    HapticFeedbackHelper.vibrateTap(context)
                    filterCriteria = filterCriteria.copy(onlyAcceptedNeural = true)
                    FilterPreferencesManager.saveCriteria(context, filterCriteria)
                    voiceManager?.speak("Filtro ativado: somente recomendações aprovadas pelo Jarvis.")
                }
                VoiceActionCommand.FILTER_MIN_15 -> {
                    HapticFeedbackHelper.vibrateTap(context)
                    filterCriteria = filterCriteria.copy(minValue = 15.0)
                    FilterPreferencesManager.saveCriteria(context, filterCriteria)
                    voiceManager?.speak("Filtro alterado: valor mínimo quinze reais.")
                }
                VoiceActionCommand.FILTER_MIN_20 -> {
                    HapticFeedbackHelper.vibrateTap(context)
                    filterCriteria = filterCriteria.copy(minValue = 20.0)
                    FilterPreferencesManager.saveCriteria(context, filterCriteria)
                    voiceManager?.speak("Filtro alterado: valor mínimo vinte reais.")
                }
                VoiceActionCommand.FILTER_MIN_30 -> {
                    HapticFeedbackHelper.vibrateTap(context)
                    filterCriteria = filterCriteria.copy(minValue = 30.0)
                    FilterPreferencesManager.saveCriteria(context, filterCriteria)
                    voiceManager?.speak("Filtro alterado: valor mínimo trinta reais.")
                }
            }
        }
        speechManager = manager

        voiceManager?.onSpeechStarted = {
            manager.pauseForTts()
        }
        voiceManager?.onSpeechFinished = {
            manager.resumeAfterTts()
        }

        if (hasMicPermission) {
            manager.startListening()
        }

        onDispose {
            voiceManager?.onSpeechStarted = null
            voiceManager?.onSpeechFinished = null
            manager.destroy()
        }
    }

    val rawVoiceState = speechManager?.state?.collectAsState()?.value 
        ?: VoiceCommandState(isPermissionGranted = hasMicPermission)
    val currentVoiceState = remember(rawVoiceState, hasMicPermission) {
        rawVoiceState.copy(isPermissionGranted = hasMicPermission)
    }

    val speedState = speedMonitor?.state?.collectAsState()?.value ?: SpeedSafetyState()

    // Lista filtrada derivada das ofertas ativas e critérios dinâmicos
    val displayedOffers = remember(offersList.toList(), filterCriteria) {
        offersList.filter { filterCriteria.matches(it) }
    }

    // 6. Dynamic Dispatch Loop (Simulador Ativo de Despacho em Tempo Real)
    LaunchedEffect(isTrackingActive, isAutoDispatchActive, filterCriteria) {
        if (!isTrackingActive || !isAutoDispatchActive) return@LaunchedEffect

        while (isActive) {
            delay(14000L) // Aguarda 14s entre despachos automáticos
            if (isTrackingActive && isAutoDispatchActive && offersList.size < 6) {
                val newOffer = LiveDispatchSimulator.generateNextOffer()
                offersList.add(0, newOffer)
                scannedOffersCount++

                // Persistência Imediata no Cache Local Room (Consulta Offline)
                val cachedOfferEntity = CachedOfferEntity(
                    id = newOffer.id,
                    appName = newOffer.appName,
                    restaurant = newOffer.restaurant,
                    value = newOffer.value,
                    distanceKm = newOffer.distanceKm,
                    timeMinutes = newOffer.estimatedTimeMin,
                    gainPerKm = newOffer.gainPerKm,
                    pickupAddress = newOffer.pickupAddress.ifEmpty { "${newOffer.restaurant}, São Paulo" },
                    deliveryAddress = newOffer.destinationAddress.ifEmpty { "Destino Cliente, SP" },
                    neuralDecision = newOffer.neuralDecision.decision,
                    neuralReason = newOffer.neuralDecision.reason,
                    confidence = newOffer.neuralDecision.confidence,
                    status = "PENDING",
                    fuelCost = newOffer.fuelCost,
                    netProfit = newOffer.netProfit,
                    timestamp = System.currentTimeMillis()
                )
                coroutineScope.launch {
                    cacheRepository.cacheOffer(cachedOfferEntity)
                }

                // Anúncio Neural por Voz no Fone Bluetooth (respeita os filtros ativos do entregador)
                if (isVoiceEnabled && voiceManager != null && filterCriteria.matches(newOffer)) {
                    voiceManager.announceNewOffer(
                        appName = newOffer.appName,
                        restaurant = newOffer.restaurant,
                        value = newOffer.value,
                        distanceKm = newOffer.distanceKm,
                        gainPerKm = newOffer.gainPerKm,
                        neuralDecision = newOffer.neuralDecision.decision
                    )
                }

                // Disparo de Notificação Local em Segundo Plano para Ofertas de Alta Prioridade
                val isHighPriority = newOffer.gainPerKm >= 5.0 || newOffer.neuralDecision.decisionEnum == RadarDecision.ACCEPT
                if (isAppInBackground && isHighPriority && filterCriteria.matches(newOffer)) {
                    localNotificationManager.showHighPriorityOfferNotification(newOffer)
                }
            }
        }
    }

    if (showOnboardingTour) {
        GuidedOnboardingTour(
            onFinishTour = { showOnboardingTour = false },
            onOpenFilterSettings = {
                showOnboardingTour = false
                showFilterSettingsScreen = true
            }
        )
        return
    }

    if (showProfileScreen) {
        DeliveryProfileScreen(
            onNavigateBack = { showProfileScreen = false },
            onOpenOnboardingTour = {
                showProfileScreen = false
                showOnboardingTour = true
            }
        )
        return
    }

    if (showFilterSettingsScreen) {
        FilterSettingsScreen(
            currentCriteria = filterCriteria,
            onSaveCriteria = { newCriteria ->
                filterCriteria = newCriteria
                speedMonitor?.updateSpeedThreshold(newCriteria.safetySpeedThresholdKm)
                LocationService.updateSafetySpeedThreshold(newCriteria.safetySpeedThresholdKm)
            },
            onNavigateBack = { showFilterSettingsScreen = false }
        )
        return
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
                navigationIcon = {
                    // Botão de Áudio / Voz Neural
                    IconButton(
                        onClick = {
                            isVoiceEnabled = !isVoiceEnabled
                            voiceManager?.isMuted = !isVoiceEnabled
                        },
                        modifier = Modifier.testTag("action_voice_toggle")
                    ) {
                        Text(
                            text = if (isVoiceEnabled) "🔊" else "🔇",
                            fontSize = 18.sp
                        )
                    }
                },
                actions = {
                    // Botão do Tour Guiado / Ajuda de Operação (Filtros e Comandos de Voz)
                    IconButton(
                        onClick = { showOnboardingTour = true },
                        modifier = Modifier.testTag("action_onboarding_tour")
                    ) {
                        Text(
                            text = "💡",
                            fontSize = 18.sp
                        )
                    }

                    // Botão de Perfil do Entregador (Histórico Interno de Decisões)
                    IconButton(
                        onClick = { showProfileScreen = true },
                        modifier = Modifier.testTag("action_profile")
                    ) {
                        Text(
                            text = "👤",
                            fontSize = 18.sp
                        )
                    }

                    // Botão de Microfone Mãos-Livres (SpeechRecognizer)
                    IconButton(
                        onClick = {
                            if (!hasMicPermission) {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            } else {
                                if (currentVoiceState.isListening) {
                                    speechManager?.stopListening()
                                } else {
                                    speechManager?.startListening()
                                }
                            }
                        },
                        modifier = Modifier.testTag("action_mic_toggle")
                    ) {
                        Text(
                            text = if (currentVoiceState.isListening) "🎙️" else "🎙",
                            fontSize = 18.sp
                        )
                    }

                    // Botão VIP Pro de Assinatura / Upgrade
                    IconButton(
                        onClick = { showSubscriptionPaywall = true },
                        modifier = Modifier.testTag("action_subscription_vip")
                    ) {
                        Text(
                            text = if (subscriptionState.isActive) "👑" else "⭐",
                            fontSize = 18.sp
                        )
                    }

                    // Botão de Configuração de Filtros de Despacho (Abre tela completa de filtros)
                    IconButton(
                        onClick = {
                            FirebaseAnalyticsManager.logScreenView("FilterSettingsScreen", "Dashboard")
                            showFilterSettingsScreen = true
                        },
                        modifier = Modifier.testTag("action_filter_settings")
                    ) {
                        Box(contentAlignment = Alignment.TopEnd) {
                            Text(
                                text = "🎚️",
                                fontSize = 18.sp
                            )
                            if (filterCriteria.isActive) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(NeonGreen)
                                )
                            }
                        }
                    }

                    // Botão de Telemetria e Métricas do Firebase Analytics
                    IconButton(
                        onClick = {
                            FirebaseAnalyticsManager.logScreenView("AnalyticsDashboardSheet", "Dashboard")
                            showAnalyticsDashboard = true
                        },
                        modifier = Modifier.testTag("action_analytics_dashboard")
                    ) {
                        Text(
                            text = "🔥",
                            fontSize = 18.sp
                        )
                    }

                    // Botão Manual de Despacho / Varredura Imediata nos Apps e API REST
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                HapticFeedbackHelper.vibrateSuccess(context)
                                Toast.makeText(context, "Varrendo entregas nos apps parceiros...", Toast.LENGTH_SHORT).show()
                                
                                // 1. Tenta buscar entregas reais interceptadas na API REST (/api/stacks)
                                val apiStacks = RadarDecisionEngine.fetchPendingStacks()
                                if (apiStacks.isNotEmpty()) {
                                    var addedCount = 0
                                    apiStacks.forEach { jsonStack ->
                                        val offer = MergedDeliverySearchEngine.mapBackendStackToRadarOffer(jsonStack)
                                        if (!offersList.any { it.id == offer.id }) {
                                            offersList.add(0, offer)
                                            scannedOffersCount++
                                            addedCount++
                                        }
                                    }
                                    if (addedCount > 0) {
                                        Toast.makeText(context, "$addedCount novas entregas sincronizadas dos apps!", Toast.LENGTH_SHORT).show()
                                        val first = offersList.firstOrNull()
                                        if (first != null && isVoiceEnabled && voiceManager != null) {
                                            voiceManager.announceNewOffer(
                                                appName = first.appName,
                                                restaurant = first.restaurant,
                                                value = first.value,
                                                distanceKm = first.distanceKm,
                                                gainPerKm = first.gainPerKm,
                                                neuralDecision = first.neuralDecision.decision
                                            )
                                        }
                                        return@launch
                                    }
                                }

                                // 2. Fallback: gera nova oferta com cálculo de distância ajustado à malha viária
                                val newOffer = LiveDispatchSimulator.generateNextOffer()
                                offersList.add(0, newOffer)
                                scannedOffersCount++
                                if (isVoiceEnabled && voiceManager != null) {
                                    voiceManager.announceNewOffer(
                                        appName = newOffer.appName,
                                        restaurant = newOffer.restaurant,
                                        value = newOffer.value,
                                        distanceKm = newOffer.distanceKm,
                                        gainPerKm = newOffer.gainPerKm,
                                        neuralDecision = newOffer.neuralDecision.decision
                                    )
                                }
                            }
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val mergedOffersCount = remember(offersList.toList()) {
                offersList.count { it.isMultiStack }
            }

            // PAINEL SUPERIOR COM SLIDERS EM TEMPO REAL (Valor Mínimo, Distância Máxima, Ganho/Km e Busca Multi-Stack)
            TopFilterSlidersPanel(
                criteria = filterCriteria,
                totalOffersCount = offersList.size,
                filteredOffersCount = displayedOffers.size,
                mergedOffersCount = mergedOffersCount,
                onCriteriaChange = { 
                    filterCriteria = it 
                    FilterPreferencesManager.saveCriteria(context, it)
                    speedMonitor?.updateSpeedThreshold(it.safetySpeedThresholdKm)
                    LocationService.updateSafetySpeedThreshold(it.safetySpeedThresholdKm)
                },
                onTriggerMergedSearch = {
                    HapticFeedbackHelper.vibrateSuccess(context)
                    val newMerged = MergedDeliverySearchEngine.findPairableSynergy(offersList).firstOrNull()
                        ?: MergedDeliverySearchEngine.generateRealtimeMergedStack()
                    if (!offersList.any { it.id == newMerged.id }) {
                        offersList.add(0, newMerged)
                    }
                    filterCriteria = filterCriteria.copy(onlyMultiStack = true)
                    if (isVoiceEnabled && voiceManager != null) {
                        voiceManager.speak("Varredura de entregas mescladas concluída! Nova combinação de alta sinergia identificada.")
                    }
                },
                onOpenAdvancedSettings = { showFilterSettingsScreen = true }
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 14.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Banner Promocional Plano Pro / Upgrade
                if (!subscriptionState.isActive) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showSubscriptionPaywall = true }
                                .testTag("banner_pro_upgrade_main"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF14141E)),
                            border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFFFD700).copy(alpha = 0.65f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("👑", fontSize = 24.sp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "DESBLOQUEIE O RADAR PRO",
                                            color = Color(0xFFFFD700),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 0.5.sp
                                        )
                                        Text(
                                            text = "Filtro anti-corrida ruim, radar ilimitado e voz mãos-livres.",
                                            color = TextLight,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFFFD700))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "VER PLANO",
                                        color = Color(0xFF0A0A0F),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }
                    }
                }

                // 1. ESTADO ATUAL DO RADAR E BOTÃO GRANDE DE RASTREAMENTO
                item {
                    BigRadarTrackingControl(
                        isTrackingActive = isTrackingActive,
                        onToggleTracking = {
                            isTrackingActive = !isTrackingActive
                            if (isVoiceEnabled && voiceManager != null) {
                                voiceManager.announceRadarState(isTrackingActive)
                            }
                        }
                    )
                }

                // 2. RESUMO DE GANHOS E MÉTRICAS DO DIA (AUTOMÁTICO)
                item {
                    RadarMetricsRow(
                        todayEarnings = completedDeliveriesList.sumOf { it.grossValue },
                        completed = completedDeliveriesList.size,
                        scanned = scannedOffersCount
                    )
                }

                // 3. BARRA DE SAÚDE NEURAL & TELEMETRIA BACKEND
                item {
                    SystemHealthBar(health = systemHealth)
                }

                // 4. MAPA DE CONSTELAÇÃO INTERATIVO (RADAR NEURAL 360°)
                item {
                    ConstellationRadarMap(
                        offers = displayedOffers,
                        onNodeSelected = { node ->
                            if (!node.isUser && node.label.isNotEmpty()) {
                                launchGoogleMapsNavigation(
                                    context = context,
                                    origin = null,
                                    destination = node.label
                                )
                            }
                        }
                    )
                }

                // 5. BANNER DE COMANDO POR VOZ (MÃOS LIVRES / SPEECH RECOGNIZER)
                item {
                    VoiceCommandLiveBanner(
                        voiceState = currentVoiceState,
                        isSpeaking = voiceManager?.isSpeaking == true,
                        onToggleListening = {
                            if (currentVoiceState.isListening) {
                                speechManager?.stopListening()
                            } else {
                                speechManager?.startListening()
                            }
                        },
                        onRequestMicPermission = {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        onSimulateCommand = { spokenText ->
                            speechManager?.simulateVoiceCommand(spokenText)
                        }
                    )
                }

                // 6. TELEMETRIA DE VELOCIDADE EM TEMPO REAL (GPS) E BLOQUEIO DE SEGURANÇA
                item {
                    RealtimeSpeedTelemetryCard(
                        speedState = speedState,
                        isBackgroundLocationGranted = hasBackgroundLocationPermission,
                        onRequestBackgroundLocation = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                            }
                        },
                        onSimulateSpeed = { speedMonitor?.setSimulatedSpeed(it) },
                        onResetRealGps = { speedMonitor?.disableSimulation() }
                    )
                }

                // 6.1. GEOFENCING • ZONAS DE ALTA DEMANDA (HOTSPOTS GASTRONÔMICOS)
                item {
                    GeofenceHotspotCard(
                        activeZone = activeGeofenceZone,
                        registeredZones = registeredGeofenceZones,
                        userLatitude = speedState.latitude,
                        userLongitude = speedState.longitude,
                        isGeofencingActive = isGeofencingActive,
                        onSimulateEnterZone = { zoneId ->
                            geofenceManager.simulateEnterZone(zoneId)
                            HapticFeedbackHelper.vibrateAccept(context)
                            Toast.makeText(context, "Entrou no raio da Geofence!", Toast.LENGTH_SHORT).show()
                        },
                        onSimulateExitZone = {
                            geofenceManager.simulateExitZone()
                            HapticFeedbackHelper.vibrateTap(context)
                            Toast.makeText(context, "Saiu do raio da Geofence.", Toast.LENGTH_SHORT).show()
                        },
                        onNavigateToZone = { zone ->
                            launchGoogleMapsNavigation(
                                context = context,
                                origin = null,
                                destination = "${zone.name} (${zone.latitude},${zone.longitude})"
                            )
                        }
                    )
                }

                // 7. MODO FOCO EM TRÂNSITO (HEAD-UP DISPLAY / HUD)
                item {
                    val bestOffer = displayedOffers.firstOrNull()
                    FocusModeHudCard(
                        isFocusModeActive = isFocusModeActive,
                        onToggleFocusMode = { isFocusModeActive = it },
                        bestOffer = bestOffer,
                        onAcceptBestOffer = if (bestOffer != null) onAcceptCurrentBestOffer else null,
                        onDeclineBestOffer = if (bestOffer != null) onDeclineCurrentBestOffer else null,
                        isListeningVoice = currentVoiceState.isListening,
                        lastVoiceCommand = currentVoiceState.lastRecognizedText,
                        currentSpeedKmh = speedState.currentSpeedKmh,
                        isSpeedSafetyLockActive = speedState.isSafetyLockActive
                    )
                }

                // 6. PAINEL DE RESUMO FINANCEIRO DIÁRIO AUTOMÁTICO (RADAR AI)
                item {
                    DailyFinancialSummaryPanel(
                        deliveries = completedDeliveriesList.toList(),
                        fuelConfig = fuelConfig,
                        dailyGoal = 350.0,
                        onExportReport = {
                            val totalGross = completedDeliveriesList.sumOf { it.grossValue }
                            val totalNet = totalGross - ((completedDeliveriesList.sumOf { it.distanceKm } / fuelConfig.kmPerLiter) * fuelConfig.fuelPricePerLiter)
                            val summaryText = "📊 FECHAMENTO RADAR AI: R$ ${String.format(Locale.GERMANY, "%.2f", totalGross)} Bruto | R$ ${String.format(Locale.GERMANY, "%.2f", totalNet)} Líquido | ${completedDeliveriesList.size} entregas finalizadas."
                            try {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("Resumo Financeiro Radar AI", summaryText)
                                clipboard?.setPrimaryClip(clip)
                                Toast.makeText(context, "Resumo financeiro copiado para a área de transferência!", Toast.LENGTH_LONG).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, summaryText, Toast.LENGTH_LONG).show()
                            }
                        },
                        onResetTurn = {
                            completedDeliveriesList.clear()
                            todayEarnings = 0.0
                            totalKmDriven = 0.0
                            completedDeliveries = 0
                            Toast.makeText(context, "Turno reiniciado! Boas entregas no novo ciclo.", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                // 7. PAINEL DE LUCRO LÍQUIDO REAL & GESTÃO DE COMBUSTÍVEL
                item {
                    FuelProfitCard(
                        grossEarnings = completedDeliveriesList.sumOf { it.grossValue },
                        totalKmDriven = completedDeliveriesList.sumOf { it.distanceKm },
                        fuelConfig = fuelConfig,
                        onUpdateConfig = { fuelConfig = it }
                    )
                }

                // 8. STATUS DOS APPS PARCEIROS CONECTADOS
                item {
                    PartnersStatusBar(isTrackingActive = isTrackingActive)
                }

                // 8. HISTÓRICO & EXTRATO DIÁRIO DETALHADO
                item {
                    DeliveryHistoryStatementCard(
                        deliveries = completedDeliveriesList.toList(),
                        fuelConfig = fuelConfig
                    )
                }

                // 9. GESTÃO DE NOTIFICAÇÕES EM SEGUNDO PLANO (HEADS-UP)
                item {
                    LocalNotificationStatusCard(
                        hasNotificationPermission = hasNotificationPermission,
                        onRequestPermission = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                Toast.makeText(context, "Notificações já habilitadas pelo sistema operacional.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onTestNotification = {
                            val testOffer = RadarOffer(
                                id = "test_notif_${System.currentTimeMillis() % 10000}",
                                appName = "iFood",
                                restaurant = "Fogo de Chão - Jardins",
                                value = 38.50,
                                distanceKm = 4.2,
                                estimatedTimeMin = 16,
                                neuralDecision = NeuralDecision(RadarDecision.ACCEPT, "Ganho excepcional de R$ 9.17/km", 0.98),
                                itemsCount = 3,
                                gainPerKm = 9.17,
                                fuelCost = 0.71,
                                netProfit = 37.79
                            )
                            localNotificationManager.showHighPriorityOfferNotification(testOffer)
                            Toast.makeText(context, "Notificação de alta prioridade enviada! Verifique o banner no topo da tela.", Toast.LENGTH_LONG).show()
                        }
                    )
                }

                // 10. CACHE LOCAL ROOM & HISTÓRICO OFFLINE (SEM SINAL)
                item {
                    OfflineCacheHistoryCard(
                        cachedOffers = cachedOffersList,
                        cachedRoutes = cachedRoutesList,
                        isOfflineModeSimulated = isOfflineModeSimulated,
                        onToggleOfflineMode = {
                            isOfflineModeSimulated = !isOfflineModeSimulated
                            val msg = if (isOfflineModeSimulated)
                                "📡 Modo Offline ATIVADO: Acessando dados locais do Room SQLite."
                            else
                                "📶 Modo Online RESTAURADO: Sincronizando dados em tempo real."
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        },
                        onClearOldCache = {
                            coroutineScope.launch {
                                cacheRepository.clearAllCache()
                                Toast.makeText(context, "Cache local Room limpo!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onSelectRouteForNavigation = { route ->
                            try {
                                val uri = Uri.parse("geo:0,0?q=${Uri.encode(route.destinationAddress)}")
                                val mapIntent = Intent(Intent.ACTION_VIEW, uri)
                                context.startActivity(mapIntent)
                                Toast.makeText(context, "Navegando rota offline: ${route.destinationAddress}", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Destino offline: ${route.destinationAddress}", Toast.LENGTH_LONG).show()
                            }
                        }
                    )
                }

                // 5. CABEÇALHO DA LISTA DE OFERTAS COM CONTROLE DE DESPACHO AO VIVO
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "OFERTAS FILTRADAS (${if (isTrackingActive) displayedOffers.size else 0})",
                                color = TextLight,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                            if (isTrackingActive && filterCriteria.isActive && displayedOffers.size < offersList.size) {
                                Text(
                                    text = "${offersList.size - displayedOffers.size} fora dos critérios",
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            // Badge de Velocidade e Trava de Segurança (> 10 km/h)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (speedState.isSafetyLockActive) RedDecline.copy(alpha = 0.2f) else DarkCardElevated)
                                    .border(1.dp, if (speedState.isSafetyLockActive) RedDecline else DarkBorder, RoundedCornerShape(8.dp))
                                    .clickable {
                                        // Alterna simulação de velocidade para teste prático do entregador (> 10 km/h)
                                        if (speedState.isSafetyLockActive) {
                                            speedMonitor?.setSimulatedSpeed(0.0)
                                        } else {
                                            speedMonitor?.setSimulatedSpeed(15.0)
                                        }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .testTag("speed_indicator_badge")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(if (speedState.isSafetyLockActive) RedDecline else NeonGreen)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "${speedState.currentSpeedKmh.toInt()} km/h",
                                    color = if (speedState.isSafetyLockActive) RedDecline else TextLight,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }

                            if (isTrackingActive) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(DarkCardElevated)
                                        .border(1.dp, if (isAutoDispatchActive) NeonGreen.copy(alpha = 0.5f) else DarkBorder, RoundedCornerShape(8.dp))
                                        .clickable { isAutoDispatchActive = !isAutoDispatchActive }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                        .testTag("toggle_auto_dispatch")
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(if (isAutoDispatchActive) NeonGreen else TextMuted)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isAutoDispatchActive) "RADAR AO VIVO" else "PAUSADO",
                                        color = if (isAutoDispatchActive) NeonGreen else TextMuted,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // 6. LISTA DE OFERTAS OU MENSAGEM DE STATUS
            if (!isTrackingActive) {
                item {
                    RadarEmptyState(
                        icon = "⏸️",
                        title = "Rastreamento Desativado",
                        subtitle = "Toque no botão principal acima para ativar o radar e rastrear pedidos."
                    )
                }
            } else if (speedState.isSafetyLockActive) {
                // VELOCIDADE > LIMITE CONFIGURADO (GPS): DESATIVA E BLOQUEIA AUTOMATICAMENTE A INTERFACE PARA SEGURANÇA
                item {
                    SpeedSafetyLockCard(
                        speedKmh = speedState.currentSpeedKmh,
                        thresholdKmh = speedState.safetySpeedThresholdKmh,
                        isListeningVoice = currentVoiceState.isListening,
                        lastVoiceCommand = currentVoiceState.lastRecognizedText,
                        onSimulateVoiceCommand = { speechManager?.simulateVoiceCommand(it) },
                        onTestSpeedChanged = { speedMonitor?.setSimulatedSpeed(it) }
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
            } else if (displayedOffers.isEmpty()) {
                item {
                    val formattedMin = String.format(Locale("pt", "BR"), "R$ %.2f", filterCriteria.minValue)
                    val formattedGain = String.format(Locale("pt", "BR"), "R$ %.2f/km", filterCriteria.minGainPerKm)
                    val formattedBonus = String.format(Locale("pt", "BR"), "+R$ %.2f", filterCriteria.minDeliveryBonus)
                    val filterDetails = buildList {
                        if (filterCriteria.onlyMultiStack) add("Somente Mescladas (Multi-Stack)")
                        if (filterCriteria.searchQuery.isNotBlank()) add("Busca \"${filterCriteria.searchQuery}\"")
                        if (filterCriteria.minValue > 0.0) add("Valor >= $formattedMin")
                        if (filterCriteria.maxDistanceKm < 8.0) add("Distância <= ${filterCriteria.maxDistanceKm} km")
                        if (filterCriteria.minGainPerKm > 0.0) add("Ganho >= $formattedGain")
                        if (filterCriteria.minDeliveryBonus > 0.0) add("Bônus >= $formattedBonus")
                        if (filterCriteria.onlyAcceptedNeural) add("Somente Jarvis")
                    }.joinToString(" • ")

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        RadarEmptyState(
                            icon = if (filterCriteria.onlyMultiStack) "✨" else "🎚️",
                            title = if (filterCriteria.onlyMultiStack) "Nenhuma entrega mesclada ativa" else "Nenhuma oferta nos critérios",
                            subtitle = if (filterCriteria.onlyMultiStack) "Nenhum pedido agrupado no momento. Execute a varredura neural para combinar rotas isoladas." else "Há ${offersList.size} pedidos na área, mas nenhum atende: $filterDetails."
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                HapticFeedbackHelper.vibrateSuccess(context)
                                val newMerged = MergedDeliverySearchEngine.findPairableSynergy(offersList).firstOrNull()
                                    ?: MergedDeliverySearchEngine.generateRealtimeMergedStack()
                                if (!offersList.any { it.id == newMerged.id }) {
                                    offersList.add(0, newMerged)
                                }
                                filterCriteria = filterCriteria.copy(onlyMultiStack = true, searchQuery = "")
                                if (isVoiceEnabled && voiceManager != null) {
                                    voiceManager.speak("Varredura de entregas mescladas concluída! Nova combinação de alta sinergia identificada.")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("btn_trigger_sweep_empty_state")
                        ) {
                            Text("⚡ VARRER MULTI-STACK AGORA", color = DarkBg, fontWeight = FontWeight.Black, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                // Banner da Ghost Sequence exibido quando há interesse ou presença de entregas mescladas
                if (filterCriteria.onlyMultiStack || displayedOffers.any { it.isMultiStack }) {
                    item(key = "ghost_sequence_predictor_banner") {
                        GhostSequencePredictorBanner(
                            onTriggerSearch = {
                                HapticFeedbackHelper.vibrateSuccess(context)
                                val newMerged = MergedDeliverySearchEngine.findPairableSynergy(offersList).firstOrNull()
                                    ?: MergedDeliverySearchEngine.generateRealtimeMergedStack()
                                if (!offersList.any { it.id == newMerged.id }) {
                                    offersList.add(0, newMerged)
                                }
                                filterCriteria = filterCriteria.copy(onlyMultiStack = true)
                                if (isVoiceEnabled && voiceManager != null) {
                                    voiceManager.speak("Nova entrega mesclada adicionada com sucesso ao radar.")
                                }
                            }
                        )
                    }
                }

                // Card de Estimativa e Telemetria de Coleta Google Maps para a oferta em análise
                val activePickupOffer = displayedOffers.find { it.id == selectedPickupOfferId }
                    ?: displayedOffers.firstOrNull()

                if (activePickupOffer != null) {
                    item(key = "maps_pickup_card_${activePickupOffer.id}") {
                        GoogleMapsPickupCard(
                            offer = activePickupOffer,
                            onAcceptWithNavigation = { offerToAccept ->
                                onAcceptOffer(offerToAccept, "Navegação Maps")
                            },
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                }

                items(displayedOffers, key = { it.id }) { offer ->
                    OfferCard(
                        offer = offer,
                        onAccept = { onAcceptOffer(offer, "Toque na Tela") },
                        onDecline = { onDeclineOffer(offer, "Toque na Tela") },
                        onInspectPickup = {
                            selectedPickupOfferId = offer.id
                        },
                        onSpeakOffer = {
                            if (voiceManager != null) {
                                voiceManager.readOfferAloud(
                                    appName = offer.appName,
                                    restaurant = offer.restaurant,
                                    value = offer.value,
                                    distanceKm = offer.distanceKm,
                                    gainPerKm = offer.gainPerKm,
                                    pickupAddress = offer.pickupAddress,
                                    estimatedMinutes = offer.timeMinutes,
                                    neuralDecision = offer.neuralDecision.decision,
                                    neuralReason = offer.neuralDecision.reason
                                )
                            }
                        }
                    )
                }
            }
        }
    }

    // Tela Dedicada de Configuração de Filtros de Corrida (Valor Mínimo, Distância Máxima, Bônus e Rentabilidade)
    if (showFilterSettingsScreen) {
        FilterSettingsScreen(
            currentCriteria = filterCriteria,
            onSaveCriteria = { newCriteria ->
                filterCriteria = newCriteria
                speedMonitor?.updateSpeedThreshold(newCriteria.safetySpeedThresholdKm)
                LocationService.updateSafetySpeedThreshold(newCriteria.safetySpeedThresholdKm)
            },
            onNavigateBack = { showFilterSettingsScreen = false }
        )
    }

    // Modal de Configuração Rápida de Filtros
    if (showFilterSettingsModal) {
        FilterSettingsDialog(
            criteria = filterCriteria,
            onCriteriaChange = { 
                filterCriteria = it 
                FilterPreferencesManager.saveCriteria(context, it)
                speedMonitor?.updateSpeedThreshold(it.safetySpeedThresholdKm)
                LocationService.updateSafetySpeedThreshold(it.safetySpeedThresholdKm)
            },
            onDismiss = { showFilterSettingsModal = false }
        )
    }

    // Tela de Perfil do Entregador (Histórico de Decisões)
    if (showProfileScreen) {
        DeliveryProfileScreen(
            onNavigateBack = { showProfileScreen = false },
            onOpenOnboardingTour = {
                showProfileScreen = false
                showOnboardingTour = true
            }
        )
    }

    // Modal de Paywall e Planos Pro / PIX
    if (showSubscriptionPaywall) {
        SubscriptionScreen(
            onDismiss = { showSubscriptionPaywall = false }
        )
    }

    // Modal de Métricas do Firebase Analytics (Conversão, Retenção e Feed)
    if (showAnalyticsDashboard) {
        AnalyticsDashboardSheet(
            onDismiss = { showAnalyticsDashboard = false }
        )
    }
}

// ----------------------------------------------------
// COMPONENTE: BOTÃO GRANDE DE RASTREAMENTO DO RADAR
// ----------------------------------------------------
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun BigRadarTrackingControl(
    isTrackingActive: Boolean = true,
    onToggleTracking: () -> Unit = {}
) {
    val statusColor by animateColorAsState(
        targetValue = if (isTrackingActive) NeonGreen else RedDecline,
        label = "statusColor"
    )

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

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(DarkCardElevated)
                    .border(2.dp, statusColor.copy(alpha = 0.5f), CircleShape)
            ) {
                if (isTrackingActive) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(NeonGreen.copy(alpha = 0.15f))
                    )
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

            Text(
                text = if (isTrackingActive) "GPS ativo (3.8m precisão) • Interceptando chamadas multiapp" else "Varredura pausada no momento",
                color = TextMuted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

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
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun RadarMetricsRow(
    todayEarnings: Double = 284.50,
    completed: Int = 18,
    scanned: Int = 52
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

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MetricTile(
    title: String = "GANHO HOJE",
    value: String = "R$ 284,50",
    valueColor: Color = NeonGreen,
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
// COMPONENTE: BARRA DE SAÚDE NEURAL & TELEMETRIA BACKEND
// ----------------------------------------------------
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SystemHealthBar(
    health: SystemHealthData = SystemHealthData(),
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkCardElevated)
            .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(NeonGreen)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "NEURAL HEALTH: ${health.score}/100",
                color = NeonGreen,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "GPS ±${health.gpsAccuracy}m",
                color = TextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(text = "•", color = DarkBorder, fontSize = 10.sp)
            Text(
                text = "${health.latencyMs}ms",
                color = TextLight,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(text = "•", color = DarkBorder, fontSize = 10.sp)
            Text(
                text = "${health.temperature.toInt()}°C",
                color = TextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ----------------------------------------------------
// COMPONENTE: STATUS DOS APPS PARCEIROS
// ----------------------------------------------------
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PartnersStatusBar(isTrackingActive: Boolean = true) {
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

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AppTag(name: String = "iFood", color: Color = RedIFood, active: Boolean = true) {
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
// NAVEGAÇÃO GOOGLE MAPS VIA INTENT
// ----------------------------------------------------
fun launchGoogleMapsNavigation(
    context: Context,
    origin: String?,
    destination: String,
    waypoint: String? = null
) {
    try {
        val destEncoded = URLEncoder.encode(destination, StandardCharsets.UTF_8.toString())
        val uriString = StringBuilder("https://www.google.com/maps/dir/?api=1&destination=$destEncoded&travelmode=two_wheeler")
        
        if (!origin.isNullOrBlank()) {
            val origEncoded = URLEncoder.encode(origin, StandardCharsets.UTF_8.toString())
            uriString.append("&origin=$origEncoded")
        }
        if (!waypoint.isNullOrBlank()) {
            val wayEncoded = URLEncoder.encode(waypoint, StandardCharsets.UTF_8.toString())
            uriString.append("&waypoints=$wayEncoded")
        }

        val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString.toString())).apply {
            setPackage("com.google.android.apps.maps")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        if (mapIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(mapIntent)
        } else {
            val genericIntent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString.toString())).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(genericIntent)
        }
    } catch (_: Exception) {
        Toast.makeText(context, "Iniciando rota no mapa...", Toast.LENGTH_SHORT).show()
    }
}

// ----------------------------------------------------
// COMPONENTE: BANNER DA GHOST SEQUENCE (PREVISÃO NEURAL DE MESCLAGEM MULTI-STACK)
// ----------------------------------------------------
@Composable
fun GhostSequencePredictorBanner(
    prediction: GhostSequencePrediction = MergedDeliverySearchEngine.getActiveGhostPrediction(),
    onTriggerSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onTriggerSearch() }
            .testTag("banner_ghost_sequence"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF12121E)),
        border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF00D2FF).copy(alpha = 0.65f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "👻", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "GHOST SEQUENCE PREDICTOR",
                            color = Color(0xFF00D2FF),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = prediction.pickupCorridor,
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF00D2FF).copy(alpha = 0.2f))
                        .border(1.dp, Color(0xFF00D2FF), RoundedCornerShape(6.dp))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "${prediction.probabilityPercent}% CHANCE",
                        color = Color(0xFF00D2FF),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Barra animada de probabilidade neural
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(DarkBorder)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(prediction.probabilityPercent / 100f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF00D2FF), NeonGreen)
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Previsão de ${prediction.sourceApp} em ~${prediction.etaMinutes} min (+R$ ${String.format(Locale.GERMANY, "%.2f", prediction.potentialBonus)})",
                    color = TextLight,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(NeonGreen.copy(alpha = 0.2f))
                        .border(0.8.dp, NeonGreen, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "VARRER ⚡",
                        color = NeonGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------
// COMPONENTE: CARD DA OFERTA DE ENTREGA COM DESIGN ESCURO DE ALTA LEGIBILIDADE PARA MOTOBOYS
// ----------------------------------------------------
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun OfferCard(
    offer: RadarOffer = LiveDispatchSimulator.getInitialOffers().first(),
    onAccept: () -> Unit = {},
    onDecline: () -> Unit = {},
    onInspectPickup: (() -> Unit)? = null,
    onSpeakOffer: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val formattedPrice = String.format(Locale.GERMANY, "R$ %.2f", offer.value)
    val formattedPerKm = String.format(Locale.GERMANY, "R$ %.2f/km", offer.gainPerKm)

    // Estimativa instantânea de deslocamento Google Maps até a coleta
    val pickupDistKm = when {
        offer.pickupAddress.contains("Paulista", ignoreCase = true) -> 0.8
        offer.pickupAddress.contains("Ibirapuera", ignoreCase = true) -> 2.1
        offer.pickupAddress.contains("Augusta", ignoreCase = true) -> 1.3
        offer.pickupAddress.contains("Oscar Freire", ignoreCase = true) -> 1.7
        offer.pickupAddress.contains("Santos", ignoreCase = true) -> 0.9
        else -> 1.4
    }
    val pickupEtaMin = (pickupDistKm * 2.8).toInt().coerceAtLeast(3)

    val decision = offer.neuralDecision
    val isDecisionAccept = decision.isAccept
    val decisionBorderColor = if (isDecisionAccept) NeonGreen else Color(0xFFFF9900)
    val decisionBgColor = if (isDecisionAccept) NeonGreen.copy(alpha = 0.15f) else Color(0xFFFF9900).copy(alpha = 0.14f)

    val cardRenderTime = remember(offer.id) { System.currentTimeMillis() }

    // Rastreamento automático de visualização de oferta no Firebase Analytics
    LaunchedEffect(offer.id) {
        FirebaseAnalyticsManager.logOfferViewed(
            offerId = offer.id,
            appName = offer.appName,
            restaurant = offer.restaurant,
            value = offer.value,
            distanceKm = offer.distanceKm,
            gainPerKm = offer.gainPerKm,
            neuralDecision = offer.neuralDecision.decision.name,
            viewSource = "radar_feed_card"
        )
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCardElevated),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (offer.isMultiStack) 2.dp else 1.2.dp,
                color = if (offer.isMultiStack) NeonGreen else DarkBorder,
                shape = RoundedCornerShape(20.dp)
            )
            .testTag("offer_card_${offer.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 1. TOP HEADER: APP ORIGEM + BADGE MESCLADA + VALOR PRINCIPAL EM DISPLAY GRANDE
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(offer.appColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = offer.appName.uppercase(Locale.getDefault()),
                        color = if (offer.isMultiStack) NeonGreen else TextLight,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp
                    )

                    if (offer.isMultiStack) {
                        Spacer(modifier = Modifier.width(8.dp))
                        val isTri = offer.quantumTelemetry?.isTriStack == true
                        val isInFlight = offer.quantumTelemetry?.inFlightIntercept == true
                        val badgeText = when {
                            isTri -> "🔥 TRI-STACK 4D"
                            isInFlight -> "🚀 INTERCEPTAÇÃO EM VOO"
                            else -> "✨ MULTI-STACK 4D"
                        }
                        val badgeBg = if (isTri) Color(0xFFFF9800) else NeonGreen
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(badgeBg.copy(alpha = 0.2f))
                                .border(1.dp, badgeBg, RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = badgeText,
                                color = badgeBg,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                // Valor Grande de Destaque Imediato no Guidão
                Text(
                    text = formattedPrice,
                    color = NeonGreen,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. NOME DO RESTAURANTE / ESTABELECIMENTO
            Text(
                text = "🍔 ${offer.restaurant}",
                color = TextLight,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.2.sp
            )

            // Destaque de Zona de Alta Demanda Geofencing (Hotspot ativo)
            if (offer.highDemandZoneTag != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF301B00))
                        .border(1.dp, Color(0xFFFF9F1C), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🔥", fontSize = 11.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "POLO DE ALTA DEMANDA • ${offer.highDemandZoneTag}",
                            color = Color(0xFFFFB347),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.4.sp
                        )
                    }
                    if (offer.surgeBonusPercent > 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFFF9F1C))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "+${offer.surgeBonusPercent}% TARIFA",
                                color = Color.Black,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 3. TRAJETO (COLETA E ENTREGA) EM CONTAINER DE ALTO CONTRASTE
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkBg.copy(alpha = 0.6f))
                    .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🟢", fontSize = 10.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Coleta: ${offer.pickupAddress}",
                            color = TextLight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🏁", fontSize = 10.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Entrega: ${offer.destinationAddress}",
                            color = TextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Estimativa e Rota de Coleta Google Maps
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF4285F4).copy(alpha = 0.12f))
                    .border(1.dp, Color(0xFF4285F4).copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                    .clickable {
                        HapticFeedbackHelper.vibrateTap(context)
                        onInspectPickup?.invoke()
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("btn_inspect_pickup_${offer.id}"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🗺️", fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Maps Coleta: ~$pickupDistKm km • $pickupEtaMin min",
                        color = Color(0xFF8AB4F8),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Ver Rota ↗",
                        color = Color(0xFF8AB4F8),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4. CHIPS DE TELEMETRIA RÁPIDA (DISTÂNCIA, GANHO/KM, TEMPO ESTIMADO)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Distância
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkBg)
                        .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "DISTÂNCIA", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "🛵 ${offer.distanceKm} km",
                            color = TextLight,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                // Rendimento por KM (Destaque Principal)
                Box(
                    modifier = Modifier
                        .weight(1.3f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(NeonGreen.copy(alpha = 0.12f))
                        .border(1.dp, NeonGreen.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "LUCRO / KM", color = NeonGreen, fontSize = 9.sp, fontWeight = FontWeight.Black)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "⚡ $formattedPerKm",
                            color = NeonGreen,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                // Tempo Estimado
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkBg)
                        .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "TEMPO EST.", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "⏱️ ${offer.timeMinutes} min",
                            color = TextLight,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4.5. PAINEL EXCLUSIVO MULTI-STACK: SINERGIA DE PLATAFORMAS, SUB-PEDIDOS E WAYPOINTS
            if (offer.isMultiStack) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(DarkBg)
                        .border(1.2.dp, NeonGreen.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    // Header de Sinergia com Badge de Margem
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "⚡", fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "SINERGIA MULTI-APP DETECTADA",
                                color = NeonGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.6.sp
                            )
                        }
                        if (offer.synergyBonusPercent > 0) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(NeonGreen)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "+${offer.synergyBonusPercent}% MARGEM",
                                    color = DarkBg,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }

                    if (offer.synergySavingsKm > 0.0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Economia em rota compartilhada: -${String.format(Locale.GERMANY, "%.1f", offer.synergySavingsKm)} km vs corridas isoladas",
                            color = TextLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // 4.6. PAINEL EXCLUSIVO TELEMETRIA QUÂNTICA 4D (JARVIS QUANTUM STACKING 4.0)
                    offer.quantumTelemetry?.let { telemetry ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF07121A))
                                .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            // Header do painel quântico
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = "🌌", fontSize = 12.sp)
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "JARVIS QUANTUM 4D • QS ${telemetry.quantumScore}%",
                                        color = Color(0xFF00E5FF),
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF00E5FF).copy(alpha = 0.2f))
                                        .border(0.8.dp, Color(0xFF00E5FF), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "🛡️ ESCUDO ANTI-BAN 100%",
                                        color = Color(0xFF00E5FF),
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Grid de 3 Métricas Quânticas
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // 1. Zero Espera de Balcão
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(DarkCardElevated)
                                        .padding(6.dp)
                                ) {
                                    Column {
                                        Text("⏱️ BALCÃO", color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                        Text("Zero Espera", color = NeonGreen, fontSize = 9.5.sp, fontWeight = FontWeight.Black)
                                    }
                                }
                                // 2. Desvio Vetorial
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(DarkCardElevated)
                                        .padding(6.dp)
                                ) {
                                    Column {
                                        Text("📐 DESVIO", color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                        Text("${telemetry.vectorDeviationDegrees}° curso", color = Color(0xFF00E5FF), fontSize = 9.5.sp, fontWeight = FontWeight.Black)
                                    }
                                }
                                // 3. Gasolina economizada
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(DarkCardElevated)
                                        .padding(6.dp)
                                ) {
                                    Column {
                                        Text("⛽ POUPADO", color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                        Text("-${telemetry.fuelSavedMilliliters} ml", color = Color(0xFFFFD700), fontSize = 9.5.sp, fontWeight = FontWeight.Black)
                                    }
                                }
                            }

                            // Sincronia de Cozinha dos Restaurantes (Kitchen Staging)
                            if (telemetry.kitchenSyncPoints.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "SINCRONIA 4D DE PREPARO (COZINHAS):",
                                    color = TextMuted,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.4.sp
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    telemetry.kitchenSyncPoints.forEach { sync ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "🍳 ${sync.restaurant}",
                                                color = TextLight,
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = sync.readyStatus,
                                                color = NeonGreen,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            // AI Insight
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "💡 \"${telemetry.aiInsight}\"",
                                color = Color(0xFF80DEEA),
                                fontSize = 9.5.sp,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                lineHeight = 13.sp
                            )
                        }
                    }

                    // Detalhamento dos Sub-Pedidos por Plataforma
                    if (offer.subOrders.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            offer.subOrders.forEach { sub ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(DarkCardElevated)
                                        .border(0.8.dp, DarkBorder, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(sub.appColor)
                                                .padding(horizontal = 5.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = sub.appName,
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = sub.restaurant,
                                            color = TextLight,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1
                                        )
                                    }
                                    Text(
                                        text = "${String.format(Locale("pt", "BR"), "R$ %.2f", sub.value)} • ${sub.distanceKm}km",
                                        color = NeonGreen,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Waypoints Sequenciais (Paradas Otimizadas)
                    if (offer.waypointRoute.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "ROTA SEQUENCIAL OTIMIZADA:",
                            color = TextMuted,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            offer.waypointRoute.forEach { waypoint ->
                                Text(
                                    text = waypoint,
                                    color = if (waypoint.contains("Coleta", ignoreCase = true)) Color(0xFFFFD700) else NeonGreen,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Demonstrativo Financeiro: Combustível vs Lucro Líquido
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF0F0F16))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⛽ Gasolina: ~${String.format(Locale("pt", "BR"), "R$ %.2f", offer.fuelCost)}",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "💰 Lucro Líquido: ${String.format(Locale("pt", "BR"), "R$ %.2f", offer.netProfit)}",
                            color = NeonGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // 5. BANNER NEURAL JARVIS (DECISÃO INTELIGENTE & MOTIVO COM CONTRASTE AGUDO)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(decisionBgColor)
                    .border(1.2.dp, decisionBorderColor.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isDecisionAccept) "🧠 JARVIS: ACEITAR CORRIDA" else "⚠️ JARVIS: DESVANTAGEM DETECTADA",
                                color = decisionBorderColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Text(
                            text = "${decision.confidencePercent}% CONFIANÇA",
                            color = decisionBorderColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = decision.reason,
                        color = TextLight,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 6. BOTÕES DE AÇÃO COM ALTURA MÍNIMA DE 48DP (ACID ACCESSIBILITY & TOQUE DE LUVA)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Botão Recusar
                OutlinedButton(
                    onClick = {
                        HapticFeedbackHelper.vibrateDecline(context)
                        onDecline()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RedDecline),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, RedDecline.copy(alpha = 0.7f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(0.85f)
                        .height(48.dp)
                        .testTag("btn_decline_offer_${offer.id}")
                ) {
                    Text("❌ RECUSAR", fontWeight = FontWeight.Black, fontSize = 11.sp)
                }

                // Botão TTS - Ouvir Oferta em Voz Alta (Hands-Free)
                OutlinedButton(
                    onClick = {
                        HapticFeedbackHelper.vibrateTap(context)
                        onSpeakOffer?.invoke()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFD700)),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFFFD700).copy(alpha = 0.65f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(0.75f)
                        .height(48.dp)
                        .testTag("btn_tts_offer_${offer.id}")
                ) {
                    Text("🔊 OUVIR", fontWeight = FontWeight.Black, fontSize = 10.5.sp)
                }
                // Botão Navegação GPS
                OutlinedButton(
                    onClick = {
                        HapticFeedbackHelper.vibrateTap(context)
                        val waypoint = if (offer.isMultiStack) "Pizza Hut Al. Santos, Sao Paulo" else null
                        launchGoogleMapsNavigation(
                            context = context,
                            origin = offer.pickupAddress + ", Sao Paulo",
                            destination = offer.destinationAddress + ", Sao Paulo",
                            waypoint = waypoint
                        )
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00D2FF)),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF00D2FF).copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(0.85f)
                        .height(48.dp)
                        .testTag("btn_maps_offer_${offer.id}")
                ) {
                    Text("🗺️ MAPA", fontWeight = FontWeight.Black, fontSize = 11.sp)
                }

                // Botão Aceitar com Alto Contraste Neon
                Button(
                    onClick = {
                        val latencyMs = System.currentTimeMillis() - cardRenderTime
                        HapticFeedbackHelper.vibrateAccept(context)
                        FirebaseAnalyticsManager.logOfferAcceptClicked(
                            offerId = offer.id,
                            appName = offer.appName,
                            restaurant = offer.restaurant,
                            value = offer.value,
                            distanceKm = offer.distanceKm,
                            gainPerKm = offer.gainPerKm,
                            clickSource = "feed_card_accept_button",
                            timeToClickMs = latencyMs
                        )
                        val waypoint = if (offer.isMultiStack) "Pizza Hut Al. Santos, Sao Paulo" else null
                        launchGoogleMapsNavigation(
                            context = context,
                            origin = offer.pickupAddress + ", Sao Paulo",
                            destination = offer.destinationAddress + ", Sao Paulo",
                            waypoint = waypoint
                        )
                        onAccept()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonGreen,
                        contentColor = DarkBg
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(48.dp)
                        .testTag("btn_accept_offer_${offer.id}")
                ) {
                    Text("✅ ACEITAR", fontWeight = FontWeight.Black, fontSize = 13.sp)
                }
            }
        }
    }
}

// ----------------------------------------------------
// COMPONENTE: ESTADO VAZIO / INFORMATIVO
// ----------------------------------------------------
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun RadarEmptyState(
    icon: String = "🛰️",
    title: String = "Varrendo Área em Tempo Real...",
    subtitle: String = "Aguardando pedidos de alta rentabilidade nas proximidades."
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

// ----------------------------------------------------
// PREVIEW PRINCIPAL PARA O ANDROID STUDIO
// ----------------------------------------------------
@Preview(
    name = "Radar Delivery Dashboard Preview",
    showBackground = true,
    showSystemUi = true,
    backgroundColor = 0xFF0A0A0F,
    widthDp = 390,
    heightDp = 844
)
@Composable
fun RadarDeliveryDashboardPreview() {
    MaterialTheme(colorScheme = RadarColorScheme) {
        RadarDeliveryDashboard()
    }
}
