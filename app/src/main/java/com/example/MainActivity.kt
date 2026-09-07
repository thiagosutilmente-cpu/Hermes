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

    // 7. Critérios de Filtragem em Tempo Real (Valor Mínimo, Distância Máxima e Jarvis)
    var filterCriteria by remember { mutableStateOf(OfferFilterCriteria(minValue = 0.0, maxDistanceKm = 8.0)) }

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

    // 9. Monitor de Velocidade e Segurança (LocationManager + Sensores)
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        hasLocationPermission = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                                perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
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
            val serviceIntent = Intent(context, LocationService::class.java).apply {
                action = LocationService.ACTION_START_LOCATION_TRACKING
            }
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            } catch (_: Exception) {}
        }

        val monitor = SpeedSafetyMonitor(context) { isLocked, speed ->
            FirebaseAnalyticsManager.logSpeedSafetyAlert(speed, limit = 10.0)
            if (isLocked) {
                HapticFeedbackHelper.vibrateDecline(context)
                if (isVoiceEnabled) {
                    voiceManager?.speak("Atenção: moto em movimento acima de 10 por hora. Trava de segurança ativada. Use comandos de voz.")
                }
            } else {
                HapticFeedbackHelper.vibrateTap(context)
                if (isVoiceEnabled) {
                    voiceManager?.speak("Velocidade segura. Lista de pedidos liberada.")
                }
            }
        }
        speedMonitor = monitor

        onDispose {
            monitor.destroy()
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
            }
        }
        speechManager = manager

        if (hasMicPermission) {
            manager.startListening()
        }

        onDispose {
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
                    pickupAddress = newOffer.pickupLocation?.address ?: "${newOffer.restaurant}, São Paulo",
                    deliveryAddress = "Destino Cliente, SP",
                    neuralDecision = newOffer.neuralDecision.decision.name,
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
                val isHighPriority = newOffer.gainPerKm >= 5.0 || newOffer.neuralDecision.decision == RadarDecision.ACCEPT
                if (isAppInBackground && isHighPriority && filterCriteria.matches(newOffer)) {
                    localNotificationManager.showHighPriorityOfferNotification(newOffer)
                }
            }
        }
    }

    if (showProfileScreen) {
        DeliveryProfileScreen(
            onNavigateBack = { showProfileScreen = false }
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

                    // Botão Manual de Despacho / Varredura Imediata
                    IconButton(
                        onClick = {
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
            // PAINEL SUPERIOR COM SLIDERS EM TEMPO REAL (Valor Mínimo & Distância Máxima)
            TopFilterSlidersPanel(
                criteria = filterCriteria,
                totalOffersCount = offersList.size,
                filteredOffersCount = displayedOffers.size,
                onCriteriaChange = { filterCriteria = it }
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
                        onSimulateSpeed = { speedMonitor?.setSimulatedSpeed(it) },
                        onResetRealGps = { speedMonitor?.disableSimulation() }
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
                                        // Alterna simulação de velocidade para teste prático do entregador
                                        if (speedState.isSafetyLockActive) {
                                            speedMonitor?.setSimulatedSpeed(0.0)
                                        } else {
                                            speedMonitor?.setSimulatedSpeed(24.0)
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
                // VELOCIDADE > 10 KM/H: OCULTA AUTOMATICAMENTE A LISTA DE OFERTAS PARA SEGURANÇA
                item {
                    SpeedSafetyLockCard(
                        speedKmh = speedState.currentSpeedKmh,
                        isListeningVoice = currentVoiceState.isListening,
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
                    val formattedMin = String.format(Locale.GERMANY, "R$ %.2f", filterCriteria.minValue)
                    RadarEmptyState(
                        icon = "🎚️",
                        title = "Nenhuma oferta nos critérios",
                        subtitle = "Há ${offersList.size} pedidos na área, mas nenhum com valor >= $formattedMin e distância <= ${filterCriteria.maxDistanceKm} km."
                    )
                }
            } else {
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
                                    neuralDecision = offer.neuralDecision.decision.name,
                                    neuralReason = offer.neuralDecision.reason
                                )
                            }
                        }
                    )
                }
            }
        }
    }

    // Tela de Perfil do Entregador (Histórico de Decisões)
    if (showProfileScreen) {
        DeliveryProfileScreen(
            onNavigateBack = { showProfileScreen = false }
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
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(NeonGreen.copy(alpha = 0.2f))
                                .border(1.dp, NeonGreen, RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "✨ MULTI-STACK",
                                color = NeonGreen,
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
